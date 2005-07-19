package opendap.clients.odc;

import java.util.regex.*;

public class Model_Retrieve {

	private Model_URLList mmodelURLs;
	public Model_URLList getURLList(){ return mmodelURLs; }
	public void setURLList( Model_URLList url_list ){ mmodelURLs = url_list; }
	Model_DirectoryTree mActiveDirectoryTree;

	Panel_Retrieve retrieve_panel;

	boolean zInitialize( Panel_Retrieve main_retrieve_panel, StringBuffer sbError ){
		if( main_retrieve_panel == null ){
			sbError.append("no retrieve panel");
			return false;
		}
		retrieve_panel = main_retrieve_panel;
		return true;
	}

	public void vShowURL( DodsURL url, Activity activity ){
		if( url.getType() == DodsURL.TYPE_Data ){
			vShowConstraintEditor( url, activity );
		} else if( url.getType() == DodsURL.TYPE_Directory ){
			vShowDirectory( url, activity );
		} else if( url.getType() == DodsURL.TYPE_Catalog ){
			vShowMessage( "[catalogs not currently supported]" );
		} else if( url.getType() == DodsURL.TYPE_HTML ||
				   url.getType() == DodsURL.TYPE_Text
				  ){
			vShowContent(url);
		} else if( url.getType() == DodsURL.TYPE_Image ){
			vShowMessage( "URL is image, output to image viewer to see" );
		} else if( url.getType() == DodsURL.TYPE_Binary ){
			vShowMessage( "URL seems to be a binary file of unknown type" );
		} else {
			vShowMessage( "URL is of an unknown type" );
		}
	}

	public void vShowConstraintEditor( final DodsURL url, final Activity activity ){
		final StringBuffer sbError = new StringBuffer(80);
		if( url.getDDS_Full() == null ){
			final Continuation_SuccessFailure con = new Continuation_SuccessFailure(){
				public void Success(){
					if( activity != null ) activity.vUpdateStatus("creating structure interface");
					if( retrieve_panel.zShowStructure(url, sbError) ){
						// System.out.println("structure for (" + url + ") is displayed");
					} else {
						retrieve_panel.vShowMessage("Error showing constraint editor: " + sbError);
					}
				}
				public void Failure( String sReason ){
					retrieve_panel.vShowMessage("Error getting structure information: " + sbError);
				}
			};
			vUpdateStructure( url, con, activity );
		} else {
			if( retrieve_panel.getPanelDDX().setStructure(url, sbError) ){
				// System.out.println("structure for (" + url + ") is displayed");
			} else {
				retrieve_panel.vShowMessage("Error showing constraint editor: " + sbError);
			}
		}
	}

	public void vShowDirectory( final DodsURL url, Activity activity ){
		final StringBuffer sbError = new StringBuffer(80);
		if( url.getDirectoryTree() == null ){
			Continuation_SuccessFailure con = new Continuation_SuccessFailure(){
				public void Success(){
					if( retrieve_panel.getPanelDirectory().setURL(url, sbError) ){
						// System.out.println("directory for node at (" + url + ") is displayed");
					} else {
						retrieve_panel.getPanelDirectory().vShowMessage("Error showing directory node: " + sbError);
					}
					retrieve_panel.vShowDirectory(true);
				}
				public void Failure( String sReason ){
					retrieve_panel.vShowDirectory(true);
					retrieve_panel.getPanelDirectory().vShowMessage("Error getting structure information: " + sbError);
				}
			};
			vUpdateDirectory( url, con, activity );
		} else {
			if( retrieve_panel.getPanelDirectory().setURL(url, sbError) ){
				retrieve_panel.vAdditionalCriteria_Clear();
				// System.out.println("structure for (" + url + ") is displayed");
			} else {
				retrieve_panel.vShowMessage("Error showing constraint editor: " + sbError);
			}
		}
	}

	public void vShowDDS( DodsURL url, Activity activity ){
		if( url == null ){ vShowMessage("internal error, URL missing (ShowDDS)"); return; }
		String sBaseURL = url.getBaseURL();
		if( sBaseURL == null ){ vShowMessage("internal error, URL lacks address (ShowDDS)"); return; }
		vShowDDS( sBaseURL, activity );
	}

	public void vShowDDS( String sBaseURL, Activity activity ){
		StringBuffer sbError = new StringBuffer(80);
		Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
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
				vShowMessage(sDDS);
			}
		} catch( Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			vShowMessage("[unexpected error retrieving DDS (" + sBaseURL + "): " + sbError + "]");
		}
	}

	public void vShowDAS( DodsURL url, Activity activity ){
		if( url == null ){ vShowMessage("internal error, URL missing (ShowDAS)"); return; }
		String sBaseURL = url.getBaseURL();
		if( sBaseURL == null ){ vShowMessage("internal error, URL lacks address (ShowDAS)"); return; }
		vShowDAS( sBaseURL, activity );
	}

	public void vShowDAS( String sBaseURL, Activity activity ){
		StringBuffer sbError = new StringBuffer(80);
		Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
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
			Utility.vUnexpectedError(ex, sbError);
			vShowMessage("[unexpected error retrieving DAS (" + sBaseURL + "): " + sbError + "]");
		}
	}

	public void vShowContent( DodsURL url ){
		StringBuffer sbError = new StringBuffer(80);
		Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
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
			Utility.vUnexpectedError(ex, sbError);
			vShowMessage("[unexpected error retrieving content (" + url + "): " + sbError + "]");
		}
	}

	public void vShowMessage( String sText ){
		Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
		retrieve_panel.getPanelDDX().setRetrieveMessage( sText );
	}

	// updates the DDS and DAS for an URL
	// starts a thread
	final private void vUpdateStructure( final DodsURL url, final Continuation_SuccessFailure con, Activity preexisting_activity ){
		if( url == null ){ con.Failure("internal error, URL was missing"); return; }
		if( url.getType() != DodsURL.TYPE_Data ){ con.Failure("internal error, URL was not of the data type"); return; }
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
						if( con != null ) con.Failure("base URL was null");
					} else {

						// initialize connection handler
						connection.setUserAgent(ApplicationController.getInstance().getVersionString());

						final StringBuffer sbError = new StringBuffer(80);

						// get base/full dds
						activity.vUpdateStatus("getting DDS");
						dds = connection.getDDS(sBaseURL, sCE, activity, sbError);
						if( dds == null ){
							ApplicationController.vShowError("Connection returned no DDS for " + sBaseURL + ": " + sbError);
							url.setDDS_Error( true );
							return;
						}
						url.setDDS_Full(dds);

						// get subset DDS
						if( sCE != null && sCE.length() > 0 ){
							activity.vUpdateStatus("getting subset DDS");
							dds = connection.getDDS(sBaseURL, sCE, activity, sbError);
							if( dds == null ){
								ApplicationController.vShowError("Connection returned no subsetted DDS for: " + sSubsettedURL + ": " + sbError);
								url.setDDS_Error( true );
								return;
							}
						}
						url.setDDS_Subset(dds);

						// get DAS
						activity.vUpdateStatus("getting DAS");
						das = connection.getDAS(sBaseURL, sCE, activity, sbError);
						if( das == null && url.getType() == DodsURL.TYPE_Catalog ){
							ApplicationController.vShowError("Connection returned no DAS for catalog " + sBaseURL + ": " + sbError);
							return;
						}
						url.setDAS(das);
						if( das == null ) ApplicationController.vShowStatus("DAS unavailable for " + sBaseURL + "?" + sCE);
					}
					if( con != null ) con.Success();
			   } catch(Exception ex) {
				   StringBuffer sbError = new StringBuffer(80);
				   Utility.vUnexpectedError( ex, sbError);
				   if( con != null ) con.Failure(sbError.toString());
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
	final private void vUpdateDirectory( final DodsURL url, final Continuation_SuccessFailure con, Activity preexisting_activity ){
		if( url == null ){ con.Failure("internal error, URL was missing"); return; }
		if( url.getType() != DodsURL.TYPE_Directory ){ con.Failure("internal error, URL was not of the directory type"); return; }
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
					activity.vUpdateStatus("generating directory tree");
					Model_DirectoryTree dirtree = zGenerateDirectoryTree(activity, sBaseURL, zRecurse, sbError);
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
					url.setDirectoryTree(dirtree);
					if( con != null ) con.Success();
			   } catch(Exception ex) {
				   StringBuffer sbError = new StringBuffer(80);
				   Utility.vUnexpectedError( ex, sbError);
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

	void vUpdateSubset(){
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
		DodsURL[] urls = getURLList().getSelectedURLs( sbError ); // zero-based
		if( urls == null || urls.length == 0 ){
			retrieve_panel.setEstimatedDownloadSize("000");
			return;
		}
		long nTotalSize = 0;
		for( int xURL = 0; xURL < urls.length; xURL++ ){
			DodsURL urlCurrent = urls[xURL];
//			opendap.dap.DDS dds = urlCurrent.getDDS_Full();
//			opendap.dap.Server.CEEvaluator ce = new opendap.dap.Server.CEEvaluator(dds);
//			ce.parseConstraint(rs.getConstraintExpression());
			nTotalSize += urlCurrent.getSizeEstimate();
		}
		retrieve_panel.setEstimatedDownloadSize( Utility.getByteCountString( nTotalSize ) );
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
		DodsURL url = getURLList().getDisplayURL(xURL_0);
		if( url == null ){
			ApplicationController.vShowError("internal error; display info URL " + xURL_0 + " does not exist");
			return;
		}
		StringBuffer sbError = new StringBuffer(80);
		int eURLtype = url.getType();
		switch( eURLtype ){
			case( DodsURL.TYPE_Data ):
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
						sInfo = Utility.sStripTags(sInfo); // get rid of html tags
						url.setInfo_Text(sInfo);
					}
				}
				break;
			default:
				// do nothing special
		}
		sbError.setLength(0);
		String sInfo = url.getInfo();
		retrieve_panel.vShowMessage( sInfo );
		ApplicationController.vClearStatus();
		return;
	}

	void vEnterURLByHand(String sURL){
		if( sURL == null ){
			sURL = javax.swing.JOptionPane.showInputDialog("Enter URL:");
			if( sURL.length() ==  0 ) return;
		}
		String sTitle = javax.swing.JOptionPane.showInputDialog("Enter a title for URL:");
		if( sTitle == null || sTitle.length() == 0 ) return;
		DodsURL[] aURL = new DodsURL[1];
		boolean zIsDirectory = sURL.endsWith("/");
		if( zIsDirectory ){
			aURL[0]	= new DodsURL(sURL, DodsURL.TYPE_Directory);
		} else {
			aURL[0]	= new DodsURL(sURL, DodsURL.TYPE_Data);
		}
/* code for constrained dir not implemented currently
			sURL = sURL.substring(0, posDirectoryEnd - 1);
			String sConstraintExpression = sURL.substring(posDirectoryEnd + 1);
			aURL[0]	= new DodsURL(sURL, DodsURL.TYPE_Directory);
			aURL[0].setConstraintExpression(sConstraintExpression);
*/
		aURL[0].setTitle(sTitle);
		getURLList().vDatasets_Add(aURL, true);
	}

	void setLocationString( String sLocation ){
		retrieve_panel.setLocationString(sLocation);
	}

	void vClearSelection(){
		retrieve_panel.vClearSelection();
	}

	public void vValidateRetrieval(){
		retrieve_panel.validate();
	}

	String getDirectoryTreePrintout(){
		if( mActiveDirectoryTree == null ){
			return "[no directory active]";
		} else {
			return mActiveDirectoryTree.getPrintout();
		}
	}

	Model_DirectoryTree zGenerateDirectoryTree( Activity activity, final String sURL, final boolean zRecurse, StringBuffer sbError ){
		final DirectoryTreeNode nodeRoot = new DirectoryTreeNode(sURL); // the label for the root is it's URL
		final Model_DirectoryTree dt = new Model_DirectoryTree(nodeRoot);
		miMaxDirectoryCount = ConfigurationManager.getInstance().getProperty_DirectoryCount();
		miMaxDirectoryDepth = ConfigurationManager.getInstance().getProperty_DirectoryDepth();
		miMaxDirectoryFiles = ConfigurationManager.getInstance().getProperty_DirectoryFiles();
		miCurrentDirectoryCount = 0;
		miCurrentDirectoryFiles = 0;
		try {
			if( activity != null ) activity.vUpdateStatus("loading root node " + sURL);
			vFetchDirectoryTree_LoadNode( nodeRoot, sURL, 1, zRecurse, activity );
		} catch(Exception ex) {
			Utility.vUnexpectedError( ex, "Unexpected error getting directory tree " + sURL);
			return null;
		}
		mActiveDirectoryTree = dt;
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
	void vFetchDirectoryTree_LoadNode( DirectoryTreeNode node, String sPageURL, int iDepth, boolean zRecurse, Activity activity ){
		try {

//System.out.println("\n**********\nloading node " + sPageURL + ": \n");
			sbNodeError.setLength(0);
			if( activity != null ) activity.vUpdateStatus("getting content for " + sPageURL);
			String sPageHTML = IO.getStaticContent(sPageURL, null, activity, sbNodeError);
			if( activity != null ) activity.vUpdateStatus("building node for " + sPageURL);
//System.out.println("\n**********\nraw page html for " + sPageURL + ": \n" + sPageHTML);
//System.out.println("\n*******  END *******\n");
//System.out.println("\n**********\npage html for " + sPageURL + ": \n" + Utility.sShowUnprintable(sPageHTML));
//System.out.println("\n*******  END *******\n");
			if( sPageHTML == null ){
				node.setError("bad directory: " + sbNodeError);
				return;
			} else {
				ApplicationController.getInstance().set("_dir_html", sPageHTML);
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
			} else {
//System.out.println("dods dir");
				vFetchDirectoryTree_DodsDir( node, sPageHTML, iDepth, zRecurse, activity );
			}

			return;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbNodeError);
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
			Utility.vUnexpectedError(ex, sbNodeError);
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
			Utility.vUnexpectedError(ex, sbNodeError);
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
			Utility.vUnexpectedError(ex, sbNodeError);
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
			Utility.vUnexpectedError(ex, sbNodeError);
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
			int endIndex;
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
			Utility.vUnexpectedError(ex, sbNodeError);
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
			int ctFile = 0;
			int ctDirectory = 0;
			int posHREF_begin = sPageHTML.indexOf("HREF=", posStart);
			if( posHREF_begin == -1 ){ // no more HREFs
				eggURLposition[0] = -1;
				return true;
			}
			eggURLposition[0] = posHREF_begin + 5; // todo if quotes are then then remove them
			int posURL_begin = eggURLposition[0];
			int posURL_end = sPageHTML.indexOf(">", posURL_begin);
			if( posURL_end == -1 ){
				sbError.append("no closing bracket found after HREF beginning " + Utility.sSafeSubstring(sPageHTML, posHREF_begin + 1, 250));
				return false;
			}
			int xLabel_begin = posURL_end + 1;
			int xLabel_end_upper = sPageHTML.indexOf("</A>", posHREF_begin);
			int xLabel_end_lower = sPageHTML.indexOf("</a>", posHREF_begin);
			int xLabel_end = (xLabel_end_upper == -1) ? xLabel_end_lower : ( xLabel_end_lower == -1 ? xLabel_end_upper : ((xLabel_end_upper > xLabel_end_lower) ? xLabel_end_lower : xLabel_end_upper));
			if( xLabel_begin > xLabel_end ){
				sbError.append("unclosed HREF " + Utility.sSafeSubstring(sPageHTML, posHREF_begin + 1, xLabel_begin - posHREF_begin + 1));
				return false;
			}
			String sURL = sPageHTML.substring(posURL_begin, posURL_end).trim();
			if( sURL.charAt(0) == '\"' || sURL.charAt(0) == '\'') sURL = sURL.substring(1); // remove preceding quotation
			if( sURL.charAt(sURL.length()-1) == '\"' || sURL.charAt(sURL.length()-1) == '\'') sURL = sURL.substring(0, sURL.length()-1); // remove trailing quotation
			eggURL[0] = sURL;
			eggLabel[0] = sPageHTML.substring(xLabel_begin, xLabel_end).trim();
			return true;
		} catch(Exception ex) {
			Utility.vUnexpectedError( ex, sbError);
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
				int xFile = 0;
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
			Utility.vUnexpectedError(ex, sbNodeError);
			return null;
		}
    }


}
