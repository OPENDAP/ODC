/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.clients.odc.data;

import java.util.regex.*;

import opendap.clients.odc.Activity;
import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ByteCounter;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Continuation_DoCancel;
import opendap.clients.odc.Continuation_SuccessFailure;
import opendap.clients.odc.IO;
import opendap.clients.odc.OpendapConnection;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.data.Model_Dataset.DATASET_TYPE;
import opendap.clients.odc.gui.Panel_Retrieve;
import opendap.clients.odc.gui.Panel_Retrieve_DDX;

public class Model_Retrieve {

	private Model_URLList mmodelURLs;
	public Model_URLList getURLList(){ return mmodelURLs; }
	Model_DirectoryTree mActiveDirectoryTree;

	public Panel_Retrieve retrieve_panel;

	public boolean zInitialize( Panel_Retrieve main_retrieve_panel, StringBuffer sbError ){
		if( main_retrieve_panel == null ){
			sbError.append("no retrieve panel");
			return false;
		}
		retrieve_panel = main_retrieve_panel;
		mmodelURLs = new Model_URLList( false );
		return true;
	}

	public Panel_Retrieve getRetrievePanel(){ return retrieve_panel; }

	public void vShowURL( Model_Dataset url, Activity activity ){
		if( url.getType() == DATASET_TYPE.Data || url.getType() == DATASET_TYPE.Definition ){
			vShowConstraintEditor( url, activity );
			ApplicationController.getInstance().getAppFrame().getPanel_Retrieve().getOutputPanel().vUpdateOutput_Data();
		} else if( url.getType() == DATASET_TYPE.Directory ){
			vShowDirectory( url, activity );
		} else if( url.getType() == DATASET_TYPE.Catalog ){
			vShowMessage( "[catalogs not currently supported]" );
		} else if( url.getType() == DATASET_TYPE.HTML ||
				   url.getType() == DATASET_TYPE.Text
				  ){
			vShowContent(url);
		} else if( url.getType() == DATASET_TYPE.Image ){
			vShowMessage( "URL is image, output to image viewer to see" );
			ApplicationController.getInstance().getAppFrame().getPanel_Retrieve().getOutputPanel().vUpdateOutput_Image();
		} else if( url.getType() == DATASET_TYPE.Binary ){
			vShowMessage( "URL seems to be a binary file of unknown type" );
			ApplicationController.getInstance().getAppFrame().getPanel_Retrieve().getOutputPanel().vUpdateOutput_Blank();
		} else {
			vShowMessage( "URL is of an unknown type" );
			ApplicationController.getInstance().getAppFrame().getPanel_Retrieve().getOutputPanel().vUpdateOutput_Blank();
		}
	}

	public void vShowConstraintEditor( final Model_Dataset url, final Activity activity ){
		final StringBuffer sbError = new StringBuffer(80);
		if( url.getDDS_Full() == null ){
			final Continuation_SuccessFailure con = new Continuation_SuccessFailure(){
				public void Success(){
					if( activity != null ) activity.vUpdateStatus("creating structure interface");
					if( retrieve_panel.zShowStructure( url, sbError ) ){
						// System.out.println("structure for (" + url + ") is displayed");
					} else {
						String sError = "Error showing constraint editor: " + sbError;
						retrieve_panel.vAdditionalCriteria_ShowText( sError );
						ApplicationController.vLogError( sError );
					}
				}
				public void Failure( String sReason ){
					String sError = "Error getting structure information while showing constraint editor: " + sReason;
					retrieve_panel.vAdditionalCriteria_ShowText( sError );
					ApplicationController.vLogError( sError );
				}
			};
			vUpdateStructure( url, con, activity );
		} else {
			if( retrieve_panel.getPanelDDX().setStructure( url, sbError ) ){
				// System.out.println("structure for (" + url + ") is displayed");
			} else {
				String sError = "Error showing constraint editor: " + sbError;
				retrieve_panel.vAdditionalCriteria_ShowText( sError );
				ApplicationController.vLogError( sError );
			}
		}
	}

	public void vShowDirectory( final Model_Dataset url, Activity activity ){
		final StringBuffer sbError = new StringBuffer(80);
		if( url.getDirectoryTree() == null ){
			Continuation_SuccessFailure con = new Continuation_SuccessFailure(){
				public void Success(){
					if( retrieve_panel.getPanelDirectory().setURL( url, sbError ) ){
						// System.out.println("directory for node at (" + url + ") is displayed: " + url.getDirectoryTree().getPrintout() );
						Model_Retrieve.this.mActiveDirectoryTree = url.getDirectoryTree();
					} else {
						retrieve_panel.getPanelDirectory().vShowMessage( "Error showing directory node: " + sbError );
					}
					retrieve_panel.vShowDirectory(true);
				}
				public void Failure( String sReason ){
					if( retrieve_panel == null ){
						ApplicationController.vShowError( "internal error trying to show directory in non-existent retrieve panel ( Error getting structure information: " + sReason + ")" );
					} else {
						retrieve_panel.vShowDirectory(true);
						retrieve_panel.getPanelDirectory().vShowMessage( "Error getting structure information while showing directory: " + sReason );
					}
				}
			};
			vUpdateDirectory( url, con, activity );
		} else {
			if( retrieve_panel.getPanelDirectory().setURL( url, sbError ) ){
				retrieve_panel.vAdditionalCriteria_ShowText( url.toString() );
				// retrieve_panel.vAdditionalCriteria_Clear();
			} else {
				retrieve_panel.vAdditionalCriteria_ShowText( "Error showing constraint editor: " + sbError );
			}
		}
	}

	public void vShowDDS( Model_Dataset url, Activity activity ){
		if( url == null ){ vShowMessage("internal error, URL missing (ShowDDS)"); return; }
		String sBaseURL = url.getBaseURL();
		if( sBaseURL == null ){ vShowMessage("internal error, URL lacks address (ShowDDS)"); return; }
		vShowDDS( sBaseURL, activity );
	}

	public void vShowDDS( String sBaseURL, Activity activity ){
		StringBuffer sbError = new StringBuffer(80);
		try {
			String sCE = null;
			OpendapConnection connection = new OpendapConnection();
			connection.setUserAgent(ApplicationController.getInstance().getVersionString());
			String sDDS = connection.getDDS_Text( sBaseURL, sCE, activity, sbError );
			if( sDDS == null ){
				vShowMessage("[no DDS available for (" + sBaseURL + "): " + sbError + "]");
			} else if( sDDS.length() == 0 ) {
				vShowMessage("[returned DDS was blank for (" + sBaseURL + ")]");
			} else {
				vShowMessage( sDDS );
			}
		} catch( Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			vShowMessage("[unexpected error retrieving DDS (" + sBaseURL + "): " + sbError + "]");
		}
	}

	public void vShowDAS( Model_Dataset url, Activity activity ){
		if( url == null ){ vShowMessage("internal error, URL missing (ShowDAS)"); return; }
		String sBaseURL = url.getBaseURL();
		if( sBaseURL == null ){ vShowMessage("internal error, URL lacks address (ShowDAS)"); return; }
		vShowDAS( sBaseURL, activity );
	}

	public void vShowDAS( String sBaseURL, Activity activity ){
		StringBuffer sbError = new StringBuffer(80);
		try {
			String sCE = null;
			OpendapConnection connection = new OpendapConnection();
			connection.setUserAgent(ApplicationController.getInstance().getVersionString());
			String sDAS = connection.getDAS_Text( sBaseURL, sCE, activity, sbError );
			if( sDAS == null ){
				vShowMessage("[no DAS available for (" + sBaseURL + "): " + sbError + "]");
			} else if( sDAS.length() == 0 ) {
				vShowMessage("[returned DAS was blank for (" + sBaseURL + ")]");
			} else {
				vShowMessage(sDAS);
			}
		} catch( Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			vShowMessage("[unexpected error retrieving DAS (" + sBaseURL + "): " + sbError + "]");
		}
	}

	public void vShowContent( Model_Dataset url ){
		StringBuffer sbError = new StringBuffer(80);
		try {
			String sBaseURL = url.getBaseURL();
			ByteCounter bc = null;
			Activity activity = null; // todo
			String sContent = IO.getStaticContent( sBaseURL, bc, activity, sbError );
			if( sContent == null ){
				vShowMessage("[no content retrieved for (" + url + "): " + sbError + "]");
			} else if( sContent.length() == 0 ) {
				vShowMessage("[returned page was blank for (" + url + ")]");
			} else {
				vShowMessage(sContent);
			}
		} catch( Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			vShowMessage("[unexpected error retrieving content (" + url + "): " + sbError + "]");
		}
	}

	public void vShowMessage( String sText ){
		retrieve_panel.getPanelDDX().setRetrieveMessage( sText );
	}

	// updates the DDS and DAS for an URL
	// starts a thread
	final private void vUpdateStructure( final Model_Dataset url, final Continuation_SuccessFailure con, Activity preexisting_activity ){
		if( url == null ){ con.Failure("internal error, URL was missing"); return; }
		if( url.getType() != DATASET_TYPE.Data ){ con.Failure("internal error, URL was not of the data type"); return; }
		final Activity activity = preexisting_activity == null ? new Activity() : preexisting_activity;
		final String sMessage = "Updating data URL: " + url.getTitle();
		final OpendapConnection connection = new OpendapConnection();
		Continuation_DoCancel conUpdate = new Continuation_DoCancel(){
			public void Do(){
				try {
					String sBaseURL = url.getBaseURL();
					String sCE      = url.getConstraintExpression();
					String sSubsettedURL = url.getFullURL();
					opendap.dap.DDS dds = null;
					opendap.dap.DAS das = null;
					if( sBaseURL == null ){
						if( con != null ) con.Failure( "base URL was null" );
					} else {

						// initialize connection handler
						connection.setUserAgent( ApplicationController.getInstance().getVersionString() );

						final StringBuffer sbError = new StringBuffer(80);

						// get base/full dds
						activity.vUpdateStatus( "getting DDS" );
						dds = connection.getDDS( sBaseURL, sCE, activity, sbError );
						if( dds == null ){
							url.setDDS_Error( true );
							if( con != null ) con.Failure( "While updating structure, connection returned no DDS for base URL: " + sBaseURL + ": " + sbError );
							return;
						}
						url.setDDS_Full(dds);

						// get subset DDS
						if( sCE != null && sCE.length() > 0 ){
							activity.vUpdateStatus( "getting subset DDS" );
							dds = connection.getDDS( sBaseURL, sCE, activity, sbError );
							if( dds == null ){
								if( con != null ) con.Failure( "Connection returned no subsetted DDS for: " + sSubsettedURL + ": " + sbError );
								url.setDDS_Error( true );
								return;
							}
						}
						url.setDDS_Subset(dds);

						// get DAS
						activity.vUpdateStatus("getting DAS");
						das = connection.getDAS( sBaseURL, sCE, activity, sbError );
						if( das == null && url.getType() == DATASET_TYPE.Catalog ){
							if( con != null ) con.Failure(  "Connection returned no DAS for catalog " + sBaseURL + ": " + sbError );
							return;
						}
						url.setDAS(das);
						if( das == null ) ApplicationController.vShowStatus( "DAS unavailable for " + sBaseURL + "?" + sCE );
					}
					if( con != null ) con.Success();
			   } catch( Exception ex ){
				   StringBuffer sbError = new StringBuffer(80);
				   ApplicationController.vUnexpectedError( ex, sbError);
				   if( con != null ) con.Failure( sbError.toString() );
			   }
		   }
		   public void Cancel(){
			   if( con != null ) con.Success(); // consider user cancel a successful operation
		   }
		};
		if( activity == preexisting_activity ){
			conUpdate.Do(); // do the update in the context of the existing activity
		} else { // launch the new activity
			javax.swing.JButton jbuttonActivator = null;
			java.awt.event.ActionListener action = null;
			activity.vDoActivity( jbuttonActivator, action, conUpdate, sMessage );
		}
	}

	// updates the directory root for an URL
	// starts a thread
	final private void vUpdateDirectory( final Model_Dataset url, final Continuation_SuccessFailure con, Activity preexisting_activity ){
		if( url == null ){ con.Failure("internal error, URL was missing"); return; }
		if( url.getType() != DATASET_TYPE.Directory ){ con.Failure("internal error, URL was not of the directory type"); return; }
		final Activity activity = preexisting_activity == null ? new Activity() : preexisting_activity;
		final String sMessage = "Updating directory root for: " + url.getTitle();
		Continuation_DoCancel conUpdate = new Continuation_DoCancel(){
			public void Do(){
				try {
					final String sBaseURL = url.getBaseURL();
					final StringBuffer sbError = new StringBuffer(80);
					if( sBaseURL == null ){
						if( con != null ) con.Failure("base URL was null");
						return;
					}
					final boolean zRecurse = false; // only do root, not whole tree, because some trees are enormous
					activity.vUpdateStatus( "generating directory tree" );
					Model_DirectoryTree dirtree = zGenerateDirectoryTree( activity, sBaseURL, zRecurse, sbError );
					if( dirtree == null ){
						if( con != null ) con.Failure("Error getting directory " + sBaseURL + " for retrieve panel: " + sbError);
						return;
					}
					// todo add unreachable property to dirtreenode
					if( dirtree.getRoot() == null ){
						url.setUnreachable(true, ((DirectoryTreeNode)dirtree.getRoot()).getError());
					} else {
						if( dirtree.getRootNode().getError() != null ){
							url.setUnreachable(true, dirtree.getRootNode().getError());
						} else {
							// root is ok
						}
					}
					if( dirtree.zHasErrors() ){
						ApplicationController.vShowWarning("Directory tree " + sBaseURL + " has errors; check info");
					}
					url.setDirectoryTree( dirtree );
					if( con != null ) con.Success();
			   } catch(Exception ex) {
				   StringBuffer sbError = new StringBuffer(80);
				   ApplicationController.vUnexpectedError( ex, sbError);
				   if( con != null ) con.Failure(sbError.toString());
			   }
		   }
		   public void Cancel(){
			   if( con != null ) con.Success(); // consider user cancel a successful operation
		   }
		};
		if( activity == preexisting_activity ){
			activity.vAddContinuation( conUpdate, sMessage + "+" );
		} else { // launch the new activity
			javax.swing.JButton jbuttonActivator = null;
			java.awt.event.ActionListener action = null;
			activity.vDoActivity( jbuttonActivator, action, conUpdate, sMessage );
		}
	}

//	void vUpdateSelected(){
//		DodsURL urlSelected = getURLList().getSelectedURL();
//		if( urlSelected == null ){
//			retrieve_panel.vClearSelection();
//		} else {
//			StringBuffer sbError = new StringBuffer(80);
//			if( zUpdateDDX( urlSelected, sbError ) ){
//				// updated DDX with URL
//			} else {
//				ApplicationController.getInstance().vShowError("Error updating retrieval pane: " + sbError);
//			}
//		}
//	}

//    void vUpdateDDX( String sMessage ) {
//		retrieve_panel.getPanelDDX().setRetrieveMessage( sMessage );
//	}

//	boolean zUpdateDDX(DodsURL url, StringBuffer sbError){
//		if( !retrieve_panel.getPanelDDX().setRetrieveURL( url, sbError ) ) return false;
//		return true;
//	}

	public void vUpdateSubset(){
		Panel_Retrieve_DDX panel = retrieve_panel.getPanelDDX();
		if( panel == null ){
			ApplicationController.vShowWarning("internal error, attempt to update null ddx panel");
		} else {
			panel.vApplySubset();
		}
		vUpdateOutputSize();
	}

	void vUpdateOutputSize(){
		StringBuffer sbError = new StringBuffer(80);
		Model_Dataset[] urls = getURLList().getSelectedURLs( sbError ); // zero-based
		if( urls == null || urls.length == 0 ){
			retrieve_panel.setEstimatedDownloadSize("000");
			return;
		}
		long nTotalSize = 0;
		for( int xURL = 0; xURL < urls.length; xURL++ ){
			Model_Dataset urlCurrent = urls[xURL];
//			opendap.dap.DDS dds = urlCurrent.getDDS_Full();
//			opendap.dap.Server.CEEvaluator ce = new opendap.dap.Server.CEEvaluator(dds);
//			ce.parseConstraint(rs.getConstraintExpression());
			nTotalSize += urlCurrent.getSizeEstimate();
		}
		retrieve_panel.setEstimatedDownloadSize( Utility_String.getByteCountString( nTotalSize ) );
	}

// does not work because server methods written around ServerDDS
//	void vConstrainDDS( opendap.dap.DDS dds, String sConstraintExpression ){
//		try {
//			java.io.StringReader srExpr = new java.io.StringReader(sConstraintExpression);
//            opendap.dap.parser.ExprParser ce_parser = new opendap.dap.parser.ExprParser(srExpr);
//		    opendap.dap.Server.ClauseFactory clause_factory = new opendap.dap.Server.ClauseFactory();
//			// Parses constraint expression (duh...) and sets the
//			// projection flag for each member of the CE's ServerDDS
//			// instance. This also builds the list of clauses.
//			ce_parser.constraint_expression(this, dds.getFactory(), clause_factory);
//		} catch(Exception ex) {
//		}
//	}

	public void vShowURLInfo( int xURL_0 ){
		Model_Dataset url = getURLList().getDisplayURL(xURL_0);
		if( url == null ){
			ApplicationController.vShowError("internal error; display info URL " + xURL_0 + " does not exist");
			return;
		}
		StringBuffer sbError = new StringBuffer(80);
		DATASET_TYPE eURLtype = url.getType();
		switch( eURLtype ){
			case Data :
				ApplicationController.vShowStatus("Getting data info for " + url.getTitle() + "...");
				String sBaseURL = url.getBaseURL();
				if( url.getDDS_Text() == null ){
					String sDDS = IO.getStaticContent(sBaseURL + ".dds", null, null, sbError);
					if( sDDS == null ){
						ApplicationController.vShowError("DDS not available: " + sbError + "\n");
					} else {
						url.setDDS_Text(sDDS);
					}
				}
				if( url.getDAS_Text() == null ){
					sbError.setLength(0);
					String sDAS = IO.getStaticContent(sBaseURL + ".das", null, null, sbError);
					if( sDAS == null ){
						ApplicationController.vShowError("DAS not available: " + sbError + "\n");
					} else {
						url.setDAS_Text(sDAS);
					}
				}
				if( url.getInfo_Text() == null ){
					sbError.setLength(0);
					String sInfo = IO.getStaticContent(sBaseURL + ".info", null, null, sbError);
					if( sInfo == null ){
						ApplicationController.vShowError("Info for " + sBaseURL + " not available: " + sbError + "\n");
					} else {
						sInfo = Utility_String.sStripTags(sInfo); // get rid of html tags
						url.setInfo_Text(sInfo);
					}
				}
				break;
			default:
				// do nothing special
		}
		sbError.setLength(0);
		String sInfo = url.getInfo();
		retrieve_panel.vAdditionalCriteria_ShowText( sInfo );
		ApplicationController.vClearStatus();
		return;
	}

	public boolean zEnterURLByHand( String sURL, StringBuffer sbError ){
		if( sURL == null ){
			sURL = javax.swing.JOptionPane.showInputDialog("Enter URL:");
			if( sURL.length() ==  0 ) return true; // user cancel
		}
		String sTitle = javax.swing.JOptionPane.showInputDialog( "Enter a title for URL:" );
		if( sTitle == null || sTitle.length() == 0 ) return true; // user cancel
		Model_Dataset[] aURL = new Model_Dataset[1];
		boolean zIsDirectory = sURL.endsWith("/");
		if( zIsDirectory ){
			Model_Dataset model = Model_Dataset.createDirectoryFromURL( sURL, sbError );
			if( model == null ){
				sbError.insert( 0, "unable to create directory model: " );
				return false;
			} else {
				aURL[0]	= model;
			}
		} else {
			Model_Dataset model = Model_Dataset.createDataFromURL( sURL, sbError );
			if( model == null ){
				sbError.insert( 0, "unable to create data model: " );
				return false;
			} else {
				aURL[0]	= model;
			}
		}
/* code for constrained dir not implemented currently
			sURL = sURL.substring(0, posDirectoryEnd - 1);
			String sConstraintExpression = sURL.substring(posDirectoryEnd + 1);
			aURL[0]	= new DodsURL(sURL, DodsURL.TYPE_Directory);
			aURL[0].setConstraintExpression(sConstraintExpression);
*/
		aURL[0].setTitle(sTitle);
		getURLList().vDatasets_Add(aURL, true);
		return true;
	}

	public void setLocationString( String sLocation ){
		retrieve_panel.setLocationString(sLocation);
	}

	public void vClearSelection(){
		retrieve_panel.vClearSelection();
	}

	public void vValidateRetrieval(){
		retrieve_panel.validate();
	}

	public String getDirectoryTreePrintout(){
		if( mActiveDirectoryTree == null ){
			return "[no directory active]";
		} else {
			return mActiveDirectoryTree.getPrintout();
		}
	}

	public Model_DirectoryTree zGenerateDirectoryTree( Activity activity, final String sURL, final boolean zRecurse, StringBuffer sbError ){
		final Model_DirectoryTree dt = Model_DirectoryTree.create( sURL );
		miMaxDirectoryCount = ConfigurationManager.getInstance().getProperty_DirectoryCount();
		miMaxDirectoryDepth = ConfigurationManager.getInstance().getProperty_DirectoryDepth();
		miMaxDirectoryFiles = ConfigurationManager.getInstance().getProperty_DirectoryFiles();
		miCurrentDirectoryCount = 0;
		miCurrentDirectoryFiles = 0;
		try {
			if( activity != null ) activity.vUpdateStatus("loading root node " + sURL);
			vFetchDirectoryTree_LoadNode( dt.getRootNode(), sURL, 1, zRecurse, activity );
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError( ex, "Unexpected error getting directory tree " + sURL);
			return null;
		}
		return dt;
	}

	int miCurrentDirectoryCount;
	int miCurrentDirectoryFiles;
	int miMaxDirectoryCount;
	int miMaxDirectoryDepth;
	int miMaxDirectoryFiles;

// kansas pattern
//    <td><div align='right'><b>challenger:</b> </div></td>
//    <td><div align='center'><a href='http://www.kgs.ku.edu:8080/dods/kgs/challenger.dds'> DDS </a></div></td>
//    <td><div align='center'><a href='http://www.kgs.ku.edu:8080/dods/kgs/challenger.das'> DAS </a></div></td>
//    <td><div align='center'><a href='http://www.kgs.ku.edu:8080/dods/kgs/challenger.info'> Information </a></div></td>
//    <td><div align='center'><a href='http://www.kgs.ku.edu:8080/dods/kgs/challenger.html'> HTML Data Request Form </a></div></td>
// unescaped: <td><div align='right'><b>(.*):</b>.*$\s*.*<a href='(.*)'>
	static String msKansasFilePattern = "<td><div align=\'right\'><b>(.*):</b>.*$\\s*.*<a href=\'(.*)\'>"; // $1 = file name $2 = link
	static java.util.regex.Pattern mPattern_KansasFile = Pattern.compile(msKansasFilePattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

// GrADS pattern
//<b>1: CO2c20c/:</b> <a href="http://nsipp.gsfc.nasa.gov:9090/dods/CO2c20c">dir</a><br><br>
//<b>2: DSP/:</b> <a href="http://nsipp.gsfc.nasa.gov:9090/dods/DSP">dir</a><br><br>
//<b>3: amip-exp006/:</b> <a href="http://nsipp.gsfc.nasa.gov:9090/dods/amip-exp006">dir</a><br><br>
	static String msGradsDirPattern = "<b>.*: (.*)/:</b> <a href=\"(.*)\">dir</a><br><br>"; // $1 = dir name $2 = dir href
	static java.util.regex.Pattern mPattern_GradsDir = Pattern.compile(msGradsDirPattern, Pattern.CASE_INSENSITIVE);

// Hyrax pattern
	static String msHyraxDirPattern = "<title>OPeNDAP Hyrax: Contents of";
	static java.util.regex.Pattern mPattern_HyraxDir = Pattern.compile( msHyraxDirPattern, Pattern.CASE_INSENSITIVE );

//	<b>1:
//	amip_htend:</b>&nbsp;AMIP ens04 24-Sigma Level Heating Tendency Output
//	&nbsp;
//	<a href="http://nsipp.gsfc.nasa.gov:9090/dods/amip-exp006/ens04/amip_htend.info">info</a>&nbsp;
//	<a href="http://nsipp.gsfc.nasa.gov:9090/dods/amip-exp006/ens04/amip_htend.dds">dds</a>&nbsp;
//	<a href="http://nsipp.gsfc.nasa.gov:9090/dods/amip-exp006/ens04/amip_htend.das">das</a><br><br>
// unescaped pattern: <b>\d*:\s*(.*):</b>&nbsp;(.*)$\s*&nbsp;$\s*.*$\s*<a href="(.*)\.dds">dds</a>&nbsp;
	static String msGradsFilePattern = "<b>\\d*:\\s*(.*):</b>&nbsp;(.*)$\\s*&nbsp;$\\s*.*$\\s*<a href=\"(.*)\\.dds\">dds</a>"; // $1 = file name $2 file description $3 file href root
	static java.util.regex.Pattern mPattern_GradsFile = Pattern.compile(msGradsFilePattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	static String msTHREDDSFilePattern = "<b>(.*):</b>  <a href=\'(.*)\\.dds\'>DDS</a>"; // $1 = file name $2 relative href
	static java.util.regex.Pattern mPattern_THREDDSFile = Pattern.compile(msTHREDDSFilePattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	// todo add hash table to make double sure we are not doing a recursive search
	// todo add status update that shows x of x for each dir level
	static StringBuffer sbNodeError = new StringBuffer(80);
	public void vFetchDirectoryTree_LoadNode( DirectoryTreeNode node, String sPageURL, int iDepth, boolean zRecurse, Activity activity ){
		try {

System.out.println("\n**********\nloading node " + sPageURL + ": \n");
			sbNodeError.setLength(0);
			if( activity != null ) activity.vUpdateStatus( "getting content for " + sPageURL );
			String sPageHTML = IO.getStaticContent( sPageURL, null, activity, sbNodeError );
			if( activity != null ) activity.vUpdateStatus( "building node for " + sPageURL );
//System.out.println("\n**********\nraw page html for " + sPageURL + ": \n" + sPageHTML);
//System.out.println("\n*******  END *******\n");
//System.out.println("\n**********\npage html for " + sPageURL + ": \n" + Utility.sShowUnprintable(sPageHTML));
//System.out.println("\n*******  END *******\n");
			if( sPageHTML == null ){
				node.setError( "bad directory (" + sPageURL + "): " + sbNodeError );
				return;
			} else {
				String sServerError = Utility.sGetDODSError( sPageHTML );
				if( sServerError == null ){ // no error occurred
					ApplicationController.getInstance().set("_dir_html", sPageHTML);
				} else {
					node.setError( "directory server error: " + sServerError );
					return;
				}
			}

			Matcher matcherGradsDir = mPattern_GradsDir.matcher(sPageHTML);
			Matcher matcherGradsFiles = mPattern_GradsFile.matcher(sPageHTML);
			Matcher matcherTHREDDSFiles = mPattern_THREDDSFile.matcher(sPageHTML);
			Matcher matcherKansasFiles = mPattern_KansasFile.matcher(sPageHTML);

			if( matcherGradsDir.find() ){
//System.out.println("grads dir");
				vFetchDirectoryTree_GradsDir( node, sPageHTML, iDepth, zRecurse, matcherGradsDir, activity );
			} else if( matcherGradsFiles.find() ){
//System.out.println("grads file");
				vFetchDirectoryTree_GradsFile( node, sPageHTML, iDepth, zRecurse, matcherGradsFiles );
			} else if( matcherTHREDDSFiles.find() ){
//System.out.println("thredds file");
				vFetchDirectoryTree_THREDDSFile( node, sPageURL, sPageHTML, iDepth, zRecurse, matcherTHREDDSFiles );
			} else if( matcherKansasFiles.find() ){
//System.out.println("kansas file");
				vFetchDirectoryTree_KansasFile( node, sPageHTML, iDepth, zRecurse, matcherKansasFiles );
			} else if( sPageHTML.indexOf("<catalogRef xlink") > 0 ){
				vFetchDirectoryTree_THREDDS_XML( node, sPageHTML, iDepth, zRecurse, activity );
			} else if( sPageHTML.indexOf( msHyraxDirPattern ) > 0 ){
				vFetchDirectoryTree_Hyrax( node, sPageHTML, sPageURL, iDepth, zRecurse, activity );
			} else {
//System.out.println("dods dir");
				vFetchDirectoryTree_DodsDir( node, sPageHTML, iDepth, zRecurse, activity );
			}

			return;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbNodeError);
			node.setError("page unavailable: " + sbNodeError);
			return;
		}
    }

	void vFetchDirectoryTree_GradsDir( DirectoryTreeNode node, String sPageHTML, int iDepth, boolean zRecurse, Matcher matcher, Activity activity ){
		try {

	    	// determine how many directories there are
			int ctDirectory = 1;  // the matcher has already found one
			while( matcher.find() ) ctDirectory++;

			// load files
			int ctFile = 0; // grads dirs do not mix files with directories
			String[] asFiles = new String[ctFile+1]; // one-based array
			String[] asHREF = new String[ctFile+1]; // one-based array
			node.setFileList(asFiles);
			node.setHREFList(asHREF);

			// load directories
			String[] asDirectoryName = new String[ctDirectory+1]; // one-based array
			String[] asDirectoryLabel = new String[ctDirectory+1]; // one-based array
			String[] asDirectoryPath = new String[ctDirectory+1]; // one-based array
			if( ctDirectory > 0 ){
				int xDirectory = 0;
				matcher.reset();
				while( matcher.find() ){
					xDirectory++;
					String sDirectoryName = matcher.group(1);
					String sDirectoryPath = matcher.group(2);
					asDirectoryName[xDirectory] = sDirectoryName;
					asDirectoryLabel[xDirectory] = sDirectoryName;
					asDirectoryPath[xDirectory] = sDirectoryPath;
				}
			}

			// create new nodes for each directory and recurse them if required
			for( int xDirectory = 1; xDirectory <= ctDirectory; xDirectory++ ){
				if( asDirectoryName[xDirectory] == null ) continue;
				DirectoryTreeNode nodeNew = new DirectoryTreeNode();
				nodeNew.setName(asDirectoryName[xDirectory]);
				nodeNew.setUserObject(asDirectoryLabel[xDirectory]);
				node.add(nodeNew);
				if( zRecurse )
		    		vFetchDirectoryTree_LoadNode(nodeNew, asDirectoryPath[xDirectory], iDepth+1, zRecurse, activity );
			}
			node.setDiscovered(true); // we now know the content of the node
			node.setError(null); // no errors found
			return;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbNodeError);
			node.setError("page unavailable: " + sbNodeError);
			return;
		}
    }

	void vFetchDirectoryTree_GradsFile( DirectoryTreeNode node, String sPageHTML, int iDepth, boolean zRecurse, Matcher matcher ){
		try {

			// indicate that this node is a leaf (grads dirs do not mix files and directories)
			node.setTerminal(true);

	    	// determine how many files there are
			int ctFile = 1;
			while( matcher.find() ) ctFile++;

			// load files
			String[] asFile = new String[ctFile+1]; // one-based array
			String[] asDescription = new String[ctFile+1]; // one-based array
			String[] asHREF = new String[ctFile+1]; // one-based array
			if( ctFile > 0 ){
				int xFile = 0;
				matcher.reset();
				while( matcher.find() ){
					xFile++;
					asFile[xFile] = matcher.group(1);
					asDescription[xFile] = matcher.group(2);
					String sHREF = matcher.group(3);
					asHREF[xFile] = sHREF;
				}
			}

			node.setFileList(asFile);
			node.setFileDescriptions(asDescription);
			node.setHREFList(asHREF);
			node.setDiscovered(true); // we now know the content of the node
			node.setError(null); // no errors found
			return;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbNodeError);
			node.setError("page unavailable: " + sbNodeError);
			return;
		}
    }

	void vFetchDirectoryTree_THREDDSFile( DirectoryTreeNode node, String sPageURL, String sPageHTML, int iDepth, boolean zRecurse, Matcher matcher ){
		try {

			// indicate that this node is a leaf (grads dirs do not mix files and directories)
			node.setTerminal(true);

	    	// determine how many files there are
			int ctFile = 1;
			while( matcher.find() ) ctFile++;

			// load files
			String[] asFile = new String[ctFile+1]; // one-based array
			String[] asDescription = new String[ctFile+1]; // one-based array
			String[] asHREF = new String[ctFile+1]; // one-based array
			if( ctFile > 0 ){
				int xFile = 0;
				matcher.reset();
				while( matcher.find() ){
					xFile++;
					asDescription[xFile] = matcher.group(1);
					String sRelativeHREF = matcher.group(2);
					String sHREF = Utility.sConnectPaths( sPageURL, "/", sRelativeHREF );
					asHREF[xFile] = sHREF;
					String sFilename = Utility.getFilenameFromURL( sHREF );
					asFile[xFile] = sFilename;
				}
			}

			node.setFileList(asFile);
			node.setFileDescriptions(asDescription);
			node.setHREFList(asHREF);
			node.setDiscovered(true); // we now know the content of the node
			node.setError(null); // no errors found
			return;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbNodeError);
			node.setError("page unavailable: " + sbNodeError);
			return;
		}
    }

	// similar to grads (only there is no description)
	void vFetchDirectoryTree_KansasFile( DirectoryTreeNode node, String sPageHTML, int iDepth, boolean zRecurse, Matcher matcher ){
		try {

			// indicate that this node is a leaf (grads dirs do not mix files and directories)
			node.setTerminal(true);

	    	// determine how many files there are
			int ctFile = 1;
			while( matcher.find() ) ctFile++;

			// load files
			String[] asFile = new String[ctFile+1]; // one-based array
			String[] asDescription = new String[ctFile+1]; // one-based array
			String[] asHREF = new String[ctFile+1]; // one-based array
			if( ctFile > 0 ){
				int xFile = 0;
				matcher.reset();
				while( matcher.find() ){
					xFile++;
					asFile[xFile] = matcher.group(1);
					asDescription[xFile] = asFile[xFile];
					asHREF[xFile] = matcher.group(2);
				}
			}

			node.setFileList(asFile);
			node.setFileDescriptions(asDescription);
			node.setHREFList(asHREF);
			node.setDiscovered(true); // we now know the content of the node
			node.setError(null); // no errors found
			return;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbNodeError);
			node.setError("page unavailable: " + sbNodeError);
			return;
		}
    }

	void vFetchDirectoryTree_DodsDir( DirectoryTreeNode node, String sPageHTML, int iDepth, boolean zRecurse, Activity activity ){
		try {

	    	// determine how many files and directories there are
			int ctFile = 0;
			int ctDirectory = 0;
// old method
//			int startIndex = sPageHTML.indexOf("<HR>", 0); // advance past horizontal rule todo validation
			int startIndex = sPageHTML.indexOf("Parent Directory"); // new method
			String[] eggURL = new String[1];
			String[] eggLabel = new String[1];
			int[] eggURLposition = new int[1];
			while(true){
				eggURL[0] = null;
				eggLabel[0] = null;
				if( !zGetNextDirectoryEntry( sPageHTML, startIndex, eggURL, eggLabel, eggURLposition, sbNodeError ) ){
					node.setError("page scan error during count at " + startIndex + ": " + sbNodeError);
					return;
				}
				if( eggURLposition[0] == -1 ) break; // done scanning
				if( eggLabel[0].toUpperCase().indexOf("PARENT DIRECTORY") == -1 ){
					if( eggURL[0].endsWith(".html") || Utility.isImage(eggURL[0]) ) ctFile++;
					else if( eggURL[0].endsWith("/") ) ctDirectory++;
				}
				startIndex = eggURLposition[0] + eggURL[0].length() + 1; // plus one makes sure we keep going if the URL is blank
			}

			if( ctDirectory == 0 ) node.setTerminal(true);

			// apply file restriction
			if( miCurrentDirectoryFiles >= miMaxDirectoryFiles ){
				ctFile = 0; // no more files permitted
			} else {
				if( miCurrentDirectoryFiles + ctFile > miMaxDirectoryFiles ){
					ctFile = miMaxDirectoryFiles - miCurrentDirectoryFiles;
				}
			}

			// load files
			String[] asFiles = new String[ctFile+1]; // one-based array
			String[] asHREF = new String[ctFile+1]; // one-based array
			if( ctFile > 0 ){
				int xFile = 0;
//				startIndex = sPageHTML.indexOf("<HR>", 0); // advance past horizontal rule todo validation
				startIndex = sPageHTML.indexOf("Parent Directory", 0); // new method
				while(true){
					eggURL[0] = null;
					eggLabel[0] = null;
					if( !zGetNextDirectoryEntry( sPageHTML, startIndex, eggURL, eggLabel, eggURLposition, sbNodeError ) ){
						node.setError("page scan error during file load at " + startIndex + ": " + sbNodeError);
						return;
					}
					if( eggURLposition[0] == -1 ) break; // done scanning
					String sURL = eggURL[0];
					if( sURL == null ){
						node.setError("internal error, URL null at " + startIndex + ": " + sbNodeError);
						return;
					}
					if( eggLabel[0].toUpperCase().indexOf("PARENT DIRECTORY") == -1 ){
						if( sURL.endsWith(".html") ){
							xFile++;
							asHREF[xFile] = sURL.substring(0, sURL.length() - 5);
							int offLastSlash = sURL.lastIndexOf("/");
							int lenFileName = (offLastSlash == -1) ? sURL.length() : sURL.length() - offLastSlash - 1;
							asFiles[xFile] = sURL.substring(sURL.length() - lenFileName, sURL.length()-5); // chop off the .html
						} else if( Utility.isImage(sURL) ){
							xFile++;
							asHREF[xFile] = sURL;
							int offLastSlash = sURL.lastIndexOf("/");
							int lenFileName = (offLastSlash == -1) ? sURL.length() : sURL.length() - offLastSlash - 1;
							asFiles[xFile] = eggURL[0].substring(sURL.length() - lenFileName, sURL.length());
						}

					}
					startIndex = eggURLposition[0] + eggURL[0].length() + 1; // plus one makes sure we keep going if the URL is blank
				}
			}
			node.setFileList(asFiles);
			node.setHREFList(asHREF);

			// load directories
			String[] asDirectoryPath = new String[ctDirectory+1]; // one-based array
			String[] asDirectoryName = new String[ctDirectory+1]; // one-based array
			String[] asDirectoryLabel = new String[ctDirectory+1]; // one-based array
			if( ctDirectory > 0 ){
				int xDirectory = 0;
				startIndex = sPageHTML.indexOf("<HR>", 0); // advance past horizontal rule todo validation
				while(true){
					eggURL[0] = null;
					eggLabel[0] = null;
					if( !zGetNextDirectoryEntry( sPageHTML, startIndex, eggURL, eggLabel, eggURLposition, sbNodeError ) ){
						node.setError("page scan error during directory load at " + startIndex + ": " + sbNodeError);
						return;
					}
					if( eggURLposition[0] == -1 ) break; // done scanning
					if( eggLabel[0].toUpperCase().indexOf("PARENT DIRECTORY") == -1 ){
						if( eggURL[0].endsWith("/") ){
							xDirectory++;
							if( xDirectory > ctDirectory ){
								node.setError("internal error, inconsistent directory count " + xDirectory + " of " + ctDirectory);
								return;
							}
							asDirectoryPath[xDirectory] = eggURL[0];
							String sURLNoSlash = eggURL[0].substring(0, eggURL[0].length()-1);
							int lenURLNoSlash = sURLNoSlash.length();
							int offNameStart = sURLNoSlash.lastIndexOf('/', lenURLNoSlash);
							String sDirectoryName;
							if( offNameStart == -1 ){
								sDirectoryName = sURLNoSlash.substring(0, lenURLNoSlash);
							} else {
								sDirectoryName = sURLNoSlash.substring(offNameStart+1, lenURLNoSlash);
							}
							asDirectoryName[xDirectory] = sDirectoryName;
							asDirectoryLabel[xDirectory] = eggLabel[0];
						}
					}
					startIndex = eggURLposition[0] + eggURL[0].length() + 1; // plus one makes sure we keep going if the URL is blank
				}
			}

			// create new nodes for each directory and recurse them if required
			for( int xDirectory = 1; xDirectory <= ctDirectory; xDirectory++ ){
				if( asDirectoryName[xDirectory] == null ) continue;
				DirectoryTreeNode nodeNew = new DirectoryTreeNode();
				nodeNew.setName(asDirectoryName[xDirectory]);
				nodeNew.setUserObject(asDirectoryLabel[xDirectory]);
				node.add(nodeNew);
				if( zRecurse )
		    		vFetchDirectoryTree_LoadNode(nodeNew, asDirectoryPath[xDirectory], iDepth+1, zRecurse, activity);
			}
			node.setDiscovered(true); // we now know the content of the node
			node.setError(null); // no errors found
			return;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbNodeError);
			node.setError("page unavailable: " + sbNodeError);
			return;
		}
    }

	void vFetchDirectoryTree_Hyrax( DirectoryTreeNode node, String sPageHTML, String sRootPath, int iDepth, boolean zRecurse, Activity activity ){
		try {

			String sStartString = msHyraxDirPattern;
			int startIndex_Page = sPageHTML.indexOf( sStartString );
			if( startIndex_Page == -1 ){
				node.setError( "cannot find beginning of page (" + msHyraxDirPattern + ")" );
				return;
			}
			String sHorizontalRule = "<hr size=\"1\" noshade=\"noshade\" />";
			int startIndex_HR = sPageHTML.indexOf( sHorizontalRule );
			if( startIndex_HR == -1 ){
				node.setError( "cannot find starting HR seperator (" + sHorizontalRule + ")" );
				return;
			}
			int end_index_HR = sPageHTML.indexOf( sHorizontalRule, startIndex_HR + sHorizontalRule.length() );
			if( startIndex_HR == -1 ){
				node.setError( "cannot find ending HR seperator (" + sHorizontalRule + ")" );
				return;
			}
			int startIndex_Body = startIndex_HR + sHorizontalRule.length();
			String sBodyHTML = sPageHTML.substring( startIndex_Body, end_index_HR );

	    	// determine how many files and directories there are
			int ctFile = 0;
			int ctDirectory = 0;
			int startIndex = 0;
			String[] eggURL = new String[1];
			String[] eggLabel = new String[1];
			int[] eggURLposition = new int[1];
			while( true ){
				eggURL[0] = null;
				eggLabel[0] = null;
				if( !zGetNextDirectoryEntry_Hyrax( sBodyHTML, startIndex, eggURL, eggLabel, eggURLposition, sbNodeError ) ){
					node.setError("page scan error during count at " + startIndex + ": " + sbNodeError);
					return;
				}
				if( eggURLposition[0] == -1 ) break; // done scanning
				if( eggLabel[0].toUpperCase().indexOf("PARENT DIRECTORY") == -1 ){
					if( eggURL[0].endsWith("contents.html") ) ctDirectory++;
					else ctFile++;
				} else {
					// its the parent directory link, ignore it, don't count it
				}
				startIndex = sBodyHTML.indexOf( "<tr>", eggURLposition[0] ); // go to next line in table
				if( startIndex < 1 ) break; // done
			}

			if( ctDirectory == 0 ) node.setTerminal(true);

			// apply file restriction
			if( miCurrentDirectoryFiles >= miMaxDirectoryFiles ){
				ctFile = 0; // no more files permitted
			} else {
				if( miCurrentDirectoryFiles + ctFile > miMaxDirectoryFiles ){
					ctFile = miMaxDirectoryFiles - miCurrentDirectoryFiles;
				}
			}

			// load files
			String[] asFiles = new String[ctFile+1]; // one-based array
			String[] asHREF = new String[ctFile+1]; // one-based array
			if( ctFile > 0 ){
				int xFile = 0;
				startIndex = 0;
				while(true){
					eggURL[0] = null;
					eggLabel[0] = null;
					if( !zGetNextDirectoryEntry_Hyrax( sBodyHTML, startIndex, eggURL, eggLabel, eggURLposition, sbNodeError ) ){
						node.setError("page scan error during file load at " + startIndex + ": " + sbNodeError);
						return;
					}
					if( eggURLposition[0] == -1 ) break; // done scanning
					String sURL = eggURL[0];
					if( sURL == null ){
						node.setError("internal error, URL null at " + startIndex + ": " + sbNodeError);
						return;
					}
					String sLabel = eggLabel[0];
					if( sLabel.toUpperCase().indexOf("PARENT DIRECTORY") == -1 ){
						if( sLabel.endsWith("/") ){
							// its a directory, ignore it
						} else {
							if( sURL.endsWith(".html") ){
								xFile++;
								asHREF[xFile] = sRootPath + "/" + sURL.substring(0, sURL.length() - 5);
								int offLastSlash = sURL.lastIndexOf("/");
								int lenFileName = (offLastSlash == -1) ? sURL.length() : sURL.length() - offLastSlash - 1;
								asFiles[xFile] = sURL.substring(sURL.length() - lenFileName, sURL.length()-5); // chop off the .html
							} else {
								xFile++;
								asHREF[xFile] = sURL;
								int offLastSlash = sURL.lastIndexOf("/");
								int lenFileName = (offLastSlash == -1) ? sURL.length() : sURL.length() - offLastSlash - 1;
								asFiles[xFile] = eggURL[0].substring(sURL.length() - lenFileName, sURL.length());
							}
						}
					}
					startIndex = sBodyHTML.indexOf( "<tr>", eggURLposition[0] ); // go to next line in table
					if( startIndex < 1 ) break; // done
				}
			}
			node.setFileList(asFiles);
			node.setHREFList(asHREF);

			// load directories
			String[] asDirectoryPath = new String[ctDirectory+1]; // one-based array
			String[] asDirectoryName = new String[ctDirectory+1]; // one-based array
			String[] asDirectoryLabel = new String[ctDirectory+1]; // one-based array
			if( ctDirectory > 0 ){
				int xDirectory = 0;
				startIndex = 0;
				while(true){
					eggURL[0] = null;
					eggLabel[0] = null;
					if( !zGetNextDirectoryEntry_Hyrax( sBodyHTML, startIndex, eggURL, eggLabel, eggURLposition, sbNodeError ) ){
						node.setError("page scan error during directory load at " + startIndex + ": " + sbNodeError);
						return;
					}
					if( eggURLposition[0] == -1 ) break; // done scanning
					if( eggLabel[0].toUpperCase().indexOf("PARENT DIRECTORY") == -1 ){
						if( eggLabel[0].endsWith("/") ){
							xDirectory++;
							if( xDirectory > ctDirectory ){
								node.setError("internal error, inconsistent directory count " + xDirectory + " of " + ctDirectory);
								return;
							}
							String sDirectoryPath = Utility.sConnectPaths( sRootPath, "/", eggLabel[0] );
							asDirectoryPath[xDirectory] = Utility.sConnectPaths( sDirectoryPath, "/", "contents.html" ); // hyrax directories use this document as their index
							String sURLNoSlash = sDirectoryPath.substring(0, sDirectoryPath.length()-1);
							int lenURLNoSlash = sURLNoSlash.length();
							int offNameStart = sURLNoSlash.lastIndexOf('/', lenURLNoSlash);
							String sDirectoryName;
							if( offNameStart == -1 ){
								sDirectoryName = sURLNoSlash.substring(0, lenURLNoSlash);
							} else {
								sDirectoryName = sURLNoSlash.substring(offNameStart+1, lenURLNoSlash);
							}
							asDirectoryName[xDirectory] = sDirectoryName;
							asDirectoryLabel[xDirectory] = eggLabel[0];
						}
					}
					startIndex = sBodyHTML.indexOf( "<tr>", eggURLposition[0] ); // go to next line in table
					if( startIndex < 1 ) break; // done
				}
			}

			// create new nodes for each directory and recurse them if required
			for( int xDirectory = 1; xDirectory <= ctDirectory; xDirectory++ ){
				String sDirectoryName = asDirectoryName[xDirectory];
				Object oUserObject = asDirectoryLabel[xDirectory];
				if( sDirectoryName == null ) continue;
				DirectoryTreeNode nodeNew = new DirectoryTreeNode();
				nodeNew.setName( sDirectoryName );
				nodeNew.setUserObject( oUserObject );
				node.add( nodeNew );
				if( zRecurse )
		    		vFetchDirectoryTree_LoadNode( nodeNew, asDirectoryPath[xDirectory], iDepth+1, zRecurse, activity );
			}
			node.setDiscovered(true); // we now know the content of the node
			node.setError(null); // no errors found
			return;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbNodeError);
			node.setError("page unavailable: " + sbNodeError);
			return;
		}
    }

	private static boolean zGetNextDirectoryEntry_Hyrax(
							    String sPageHTML,
							    int posStart,
								String[] eggURL, String[] eggLabel, int[] eggURLposition,
								StringBuffer sbError ){
		try {
			String sHREF_token = "<a href=\"";
			String sURL_end_token = "\">";
			int lenHREF_token = sHREF_token.length();
			int lenURL_end_token = sURL_end_token.length();
			int posHREF_begin = sPageHTML.indexOf( sHREF_token, posStart );
			if( posHREF_begin == -1 ){ // no more HREFs
				eggURLposition[0] = -1;
				return true;
			}
			eggURLposition[0] = posHREF_begin + lenHREF_token;
			int posURL_begin = eggURLposition[0];
			int posURL_end = sPageHTML.indexOf( sURL_end_token, posURL_begin );
			if( posURL_end == -1 ){
				sbError.append("no closing bracket found after HREF beginning " + Utility_String.sSafeSubstring( sPageHTML, posHREF_begin + 1, 250 ));
				return false;
			}
			int xLabel_begin = posURL_end + lenURL_end_token;
			int xLabel_end_upper = sPageHTML.indexOf("</A>", posHREF_begin);
			int xLabel_end_lower = sPageHTML.indexOf("</a>", posHREF_begin);
			int xLabel_end = (xLabel_end_upper == -1) ? xLabel_end_lower : ( xLabel_end_lower == -1 ? xLabel_end_upper : ((xLabel_end_upper > xLabel_end_lower) ? xLabel_end_lower : xLabel_end_upper));
			if( xLabel_begin > xLabel_end ){
				sbError.append("unclosed HREF " + Utility_String.sSafeSubstring(sPageHTML, posHREF_begin + 1, xLabel_begin - posHREF_begin + 1));
				return false;
			}
			String sURL = sPageHTML.substring(posURL_begin, posURL_end).trim();
			String sLabel = sPageHTML.substring( xLabel_begin, xLabel_end ).trim();
			if( sURL.charAt(0) == '\"' || sURL.charAt(0) == '\'') sURL = sURL.substring(1); // remove preceding quotation
			if( sURL.charAt(sURL.length()-1) == '\"' || sURL.charAt(sURL.length()-1) == '\'') sURL = sURL.substring(0, sURL.length()-1); // remove trailing quotation
			eggURL[0] = sURL;
			eggLabel[0] = sLabel;
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError( ex, sbError);
			return false;
		}
	}

	void vFetchDirectoryTree_THREDDS_XML( DirectoryTreeNode node, String sXML, int iDepth, boolean zRecurse, Activity activity ){
		try {

			// parse xml
			String sPageHTML = null;

	    	// determine how many files and directories there are
			int ctFile = 0;
			int ctDirectory = 0;
// old method
//			int startIndex = sPageHTML.indexOf("<HR>", 0); // advance past horizontal rule todo validation
			int startIndex = 0; // new method
			String[] eggURL = new String[1];
			String[] eggLabel = new String[1];
			int[] eggURLposition = new int[1];
			while(true){
				eggURL[0] = null;
				eggLabel[0] = null;
				if( !zGetNextDirectoryEntry( sPageHTML, startIndex, eggURL, eggLabel, eggURLposition, sbNodeError ) ){
					node.setError("page scan error during count at " + startIndex + ": " + sbNodeError);
					return;
				}
				if( eggURLposition[0] == -1 ) break; // done scanning
				if( eggLabel[0].toUpperCase().indexOf("PARENT DIRECTORY") == -1 ){
					if( eggURL[0].endsWith(".html") || Utility.isImage(eggURL[0]) ) ctFile++;
					else if( eggURL[0].endsWith("/") ) ctDirectory++;
				}
				startIndex = eggURLposition[0] + eggURL[0].length() + 1; // plus one makes sure we keep going if the URL is blank
			}

			if( ctDirectory == 0 ) node.setTerminal(true);

			// apply file restriction
			if( miCurrentDirectoryFiles >= miMaxDirectoryFiles ){
				ctFile = 0; // no more files permitted
			} else {
				if( miCurrentDirectoryFiles + ctFile > miMaxDirectoryFiles ){
					ctFile = miMaxDirectoryFiles - miCurrentDirectoryFiles;
				}
			}

			// load files
			String[] asFiles = new String[ctFile+1]; // one-based array
			String[] asHREF = new String[ctFile+1]; // one-based array
			if( ctFile > 0 ){
				int xFile = 0;
//				startIndex = sPageHTML.indexOf("<HR>", 0); // advance past horizontal rule todo validation
				startIndex = 0; // new method
				while(true){
					eggURL[0] = null;
					eggLabel[0] = null;
					if( !zGetNextDirectoryEntry( sPageHTML, startIndex, eggURL, eggLabel, eggURLposition, sbNodeError ) ){
						node.setError("page scan error during file load at " + startIndex + ": " + sbNodeError);
						return;
					}
					if( eggURLposition[0] == -1 ) break; // done scanning
					String sURL = eggURL[0];
					if( sURL == null ){
						node.setError("internal error, URL null at " + startIndex + ": " + sbNodeError);
						return;
					}
					if( eggLabel[0].toUpperCase().indexOf("PARENT DIRECTORY") == -1 ){
						if( sURL.endsWith(".html") ){
							xFile++;
							asHREF[xFile] = sURL.substring(0, sURL.length() - 5);
							int offLastSlash = sURL.lastIndexOf("/");
							int lenFileName = (offLastSlash == -1) ? sURL.length() : sURL.length() - offLastSlash - 1;
							asFiles[xFile] = sURL.substring(sURL.length() - lenFileName, sURL.length()-5); // chop off the .html
						} else if( Utility.isImage(sURL) ){
							xFile++;
							asHREF[xFile] = sURL;
							int offLastSlash = sURL.lastIndexOf("/");
							int lenFileName = (offLastSlash == -1) ? sURL.length() : sURL.length() - offLastSlash - 1;
							asFiles[xFile] = eggURL[0].substring(sURL.length() - lenFileName, sURL.length());
						}

					}
					startIndex = eggURLposition[0] + eggURL[0].length() + 1; // plus one makes sure we keep going if the URL is blank
				}
			}
			node.setFileList(asFiles);
			node.setHREFList(asHREF);

			// load directories
			String[] asDirectoryPath = new String[ctDirectory+1]; // one-based array
			String[] asDirectoryName = new String[ctDirectory+1]; // one-based array
			String[] asDirectoryLabel = new String[ctDirectory+1]; // one-based array
			if( ctDirectory > 0 ){
				int xDirectory = 0;
				startIndex = 0; // advance past horizontal rule todo validation
				while(true){
					eggURL[0] = null;
					eggLabel[0] = null;
					if( !zGetNextDirectoryEntry( sPageHTML, startIndex, eggURL, eggLabel, eggURLposition, sbNodeError ) ){
						node.setError("page scan error during directory load at " + startIndex + ": " + sbNodeError);
						return;
					}
					if( eggURLposition[0] == -1 ) break; // done scanning
					if( eggLabel[0].toUpperCase().indexOf("PARENT DIRECTORY") == -1 ){
						if( eggURL[0].endsWith("/") ){
							xDirectory++;
							if( xDirectory > ctDirectory ){
								node.setError("internal error, inconsistent directory count " + xDirectory + " of " + ctDirectory);
								return;
							}
							asDirectoryPath[xDirectory] = eggURL[0];
							String sURLNoSlash = eggURL[0].substring(0, eggURL[0].length()-1);
							int lenURLNoSlash = sURLNoSlash.length();
							int offNameStart = sURLNoSlash.lastIndexOf('/', lenURLNoSlash);
							String sDirectoryName;
							if( offNameStart == -1 ){
								sDirectoryName = sURLNoSlash.substring(0, lenURLNoSlash);
							} else {
								sDirectoryName = sURLNoSlash.substring(offNameStart+1, lenURLNoSlash);
							}
							asDirectoryName[xDirectory] = sDirectoryName;
							asDirectoryLabel[xDirectory] = eggLabel[0];
						}
					}
					startIndex = eggURLposition[0] + eggURL[0].length() + 1; // plus one makes sure we keep going if the URL is blank
				}
			}

			// create new nodes for each directory and recurse them if required
			for( int xDirectory = 1; xDirectory <= ctDirectory; xDirectory++ ){
				if( asDirectoryName[xDirectory] == null ) continue;
				DirectoryTreeNode nodeNew = new DirectoryTreeNode();
				nodeNew.setName(asDirectoryName[xDirectory]);
				nodeNew.setUserObject(asDirectoryLabel[xDirectory]);
				node.add(nodeNew);
				if( zRecurse )
		    		vFetchDirectoryTree_LoadNode(nodeNew, asDirectoryPath[xDirectory], iDepth+1, zRecurse, activity);
			}
			node.setDiscovered(true); // we now know the content of the node
			node.setError(null); // no errors found
			return;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbNodeError);
			node.setError("page unavailable: " + sbNodeError);
			return;
		}
    }

	private static boolean zGetNextDirectoryEntry(
							    String sPageHTML,
							    int posStart,
								String[] eggURL, String[] eggLabel, int[] eggURLposition,
								StringBuffer sbError ){
		try {
			int posHREF_begin = sPageHTML.indexOf("HREF=", posStart);
			if( posHREF_begin == -1 ){ // no more HREFs
				eggURLposition[0] = -1;
				return true;
			}
			eggURLposition[0] = posHREF_begin + 5; // todo if quotes are then then remove them
			int posURL_begin = eggURLposition[0];
			int posURL_end = sPageHTML.indexOf(">", posURL_begin);
			if( posURL_end == -1 ){
				sbError.append("no closing bracket found after HREF beginning " + Utility_String.sSafeSubstring(sPageHTML, posHREF_begin + 1, 250));
				return false;
			}
			int xLabel_begin = posURL_end + 1;
			int xLabel_end_upper = sPageHTML.indexOf("</A>", posHREF_begin);
			int xLabel_end_lower = sPageHTML.indexOf("</a>", posHREF_begin);
			int xLabel_end = (xLabel_end_upper == -1) ? xLabel_end_lower : ( xLabel_end_lower == -1 ? xLabel_end_upper : ((xLabel_end_upper > xLabel_end_lower) ? xLabel_end_lower : xLabel_end_upper));
			if( xLabel_begin > xLabel_end ){
				sbError.append("unclosed HREF " + Utility_String.sSafeSubstring(sPageHTML, posHREF_begin + 1, xLabel_begin - posHREF_begin + 1));
				return false;
			}
			String sURL = sPageHTML.substring(posURL_begin, posURL_end).trim();
			if( sURL.charAt(0) == '\"' || sURL.charAt(0) == '\'') sURL = sURL.substring(1); // remove preceding quotation
			if( sURL.charAt(sURL.length()-1) == '\"' || sURL.charAt(sURL.length()-1) == '\'') sURL = sURL.substring(0, sURL.length()-1); // remove trailing quotation
			eggURL[0] = sURL;
			eggLabel[0] = sPageHTML.substring(xLabel_begin, xLabel_end).trim();
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError( ex, sbError);
			return false;
		}
	}

	String sFetchDirectoryTree_GetFirstFile( String sPageURL, Activity activity ){
		try {
			sbNodeError.setLength(0);
			String sPageHTML = IO.getStaticContent(sPageURL, null, activity, sbNodeError);
			if( sPageHTML == null ) return null;
			if( sPageHTML.length() == 0 ){
				sbNodeError.append("request returned blank");
				return null;
			}
			if( sPageHTML.indexOf("403 Forbidden") > 0 ){
				sbNodeError.append("403 Forbidden (no permission)");
				return null;
			}
			if( sPageHTML.indexOf("Error {") > 0 ){
				sbNodeError.append("Server Error: " + sPageHTML);
				return null;
			}

			// determine how many files and directories there are
			int startIndex = 0;
			int endIndex;
			int ctFile = 0;
			int ctDirectory = 0;
			startIndex = sPageHTML.indexOf("<HR>", startIndex); // advance past horizontal rule todo validation
			if( startIndex == -1 ) startIndex = 0;
			while( true ){
				startIndex = sPageHTML.indexOf("HREF=", startIndex);
				if( startIndex == -1 ) break; // all done
				endIndex = sPageHTML.indexOf(">", startIndex);
				int xHREF = startIndex + 5;
				if(endIndex == -1) {
					// closing bracket not found
				} else {
					String sHREF = sPageHTML.substring(xHREF, endIndex);
					int xName = sHREF.lastIndexOf("/");
					if( xName == -1 ) xName = xHREF;
					if( sHREF.indexOf(sPageURL) == -1 ){
						// then this is reference to the parent directory (or some other place), ignore it
					} else {
						if( sHREF.endsWith(".html") ) ctFile++;
						if( sHREF.endsWith("/") ) ctDirectory++;
					}
				}
				startIndex = xHREF;
			}

			// get file if present
			if( ctFile > 0 ){
				startIndex = sPageHTML.indexOf("<HR>"); // advance past horizontal rule todo validation
				if( startIndex == -1 ) startIndex = 0;
				while( true ){
					startIndex = sPageHTML.indexOf("HREF=", startIndex);
					if( startIndex == -1 ) break; // all done
					endIndex = sPageHTML.indexOf(">", startIndex);
					int xHREF = startIndex + 5;
					if(endIndex != -1) {
						String sHREF = sPageHTML.substring(xHREF, endIndex);
						int xFileName = sHREF.lastIndexOf("/");
						if( xFileName == -1 ) xFileName = xHREF;
						String sFileName = sHREF.substring(xFileName + 1);
						if( sFileName.endsWith(".html") ) {
							sFileName = sFileName.substring(0, sFileName.length()-5); // chop off the .html
							return sPageHTML + sFileName;
						}
					}
					startIndex = xHREF;
				}
			}

			// otherwise start traversing directories
			startIndex = sPageHTML.indexOf("<HR>"); // advance past horizontal rule todo validation
			if( startIndex == -1 ) startIndex = 0;
			endIndex = 0;
			int xDirectory = 0;
			String[] asDirectoryPath = new String[ctDirectory+1]; // one-based array
			String[] asDirectoryName = new String[ctDirectory+1]; // one-based array
			String[] asDirectoryLabel = new String[ctDirectory+1]; // one-based array
			while( true ){
				startIndex = sPageHTML.indexOf("HREF=", startIndex);
				if( startIndex == -1 ) break; // all done
				int xHREF_begin = startIndex + 5;
				int xHREF_end = sPageHTML.indexOf(">", startIndex);
				int xLabel_begin = xHREF_end + 1;
				int xLabel_end = sPageHTML.indexOf("</A>", startIndex);
				if(xHREF_end != -1) {
					String sURL = sPageHTML.substring(xHREF_begin, xHREF_end);
					if( sURL.indexOf(sPageURL) == -1 ){
						// then this is reference to the parent directory, ignore it
					} else {
						if( sURL.endsWith("/") ){
							xDirectory++;
							asDirectoryPath[xDirectory] = sURL;
							String sURLNoSlash = sURL.substring(0, sURL.length()-1);
							int lenURLNoSlash = sURLNoSlash.length();
							int offNameStart = sURLNoSlash.lastIndexOf('/', lenURLNoSlash);
							String sDirectoryName;
							if( offNameStart == -1 ){
								sDirectoryName = sURLNoSlash.substring(0, lenURLNoSlash);
							} else {
								sDirectoryName = sURLNoSlash.substring(offNameStart+1, lenURLNoSlash);
							}
							asDirectoryName[xDirectory] = sDirectoryName;
							if( xLabel_end != -1 ){
								asDirectoryLabel[xDirectory] = sPageHTML.substring(xLabel_begin, xLabel_end);
							} else {
								asDirectoryLabel[xDirectory] = sDirectoryName;
							}
						}
					}
				}
				startIndex = xHREF_begin;
			}
			for( xDirectory = 1; xDirectory <= ctDirectory; xDirectory++ ){
				if( asDirectoryName[xDirectory] == null ) continue;
				String sFirstFile = sFetchDirectoryTree_GetFirstFile(asDirectoryPath[xDirectory], activity);
				if( sFirstFile != null ) return sFirstFile;
			}
			return null; // could not find a file
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbNodeError);
			return null;
		}
    }


}
