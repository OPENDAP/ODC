package opendap.clients.odc;

/**
 * Title:        Model_URLTable
 * Description:  Models contents of retrieved datasets panel
 * Copyright:    Copyright (c) 2002-4
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.59
 */

import java.io.*;
import java.util.*;
import java.net.*;
import opendap.dap.*;
import java.awt.event.*;

// **** CLASS CURRENTLY NOT IN USE ****

public class Model_URLTable extends javax.swing.table.AbstractTableModel implements Model_URL_control {

	final private static int MAX_URLs_TO_DISPLAY = 100;

	DodsURL[] madodsurlDisplay = null; // TODO this is wrong
	DodsURL[] madodsurlSelected = null;
	boolean mzShowTypes = false;
	boolean mzAutoSubset = false;
	boolean mzDisplayingSubset = false;

	DodsURL[] getDisplayURLs(){ return null; } // NO LONGER FUNCTIONAL

//***************** table model stuff
	public int getRowCount(){
		if( madodsurlDisplay == null ){
	    	return 0;
		} else {
		    return madodsurlDisplay.length;
		}
	}
	public int getColumnCount(){ return 2; }
	public String getColumnName( int col ){
		switch(col){
			case 0: return "info";
			case 1: return "title";
			default: return "[undefined]";
		}
	}
	public Object getValueAt( int row, int col ){
		if( madodsurlDisplay == null ) return null;
		if( row < 0 || row > madodsurlDisplay.length ) return null;
		if( col < 0 || col > 1 ) return null;
		DodsURL url = madodsurlDisplay[row];
		switch(col){
			case 0:
				if( url == null ) return null;
				return "?";
			case 1:
				if( url == null ) return "[internal error: undefined cell]";
				return url;
			default:
				return null;
		}
	}
	public boolean isCellEditable(int row, int col){
		switch(col){
			case 0: return true;
			case 1: return false;
			case 2: return false; // todo allow name update
			default: return false;
		}
	}
//***********************************

	javax.swing.JTable mjtableControl; // this is the swing control being modeled

    public Model_URLTable(javax.swing.JTable jlistControl, boolean zShowTypes, boolean zAddToRecent, boolean zAutoSubset ) {
		mzShowTypes = zShowTypes;
		mzAutoSubset = zAutoSubset;
		this.setControl(jlistControl);
	}

	public void setControl( javax.swing.JTable jtableControl ){
		mjtableControl = jtableControl;
		if( mjtableControl != null ){
			mjtableControl.addMouseListener(
				new MouseAdapter(){
				    public void mousePressed( MouseEvent me ){
						if( me.getClickCount() == 1 ){
							int iRowSelected = Model_URLTable.this.mjtableControl.getSelectedRow();
							Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
							Model_URL_control urllist = retrieve_model.getURLList(); // xxx wrong
							DodsURL urlSelected = null; // NONE OF THIS WORKS -- old logic urllist.getURL(iRowSelected);
							if( urlSelected == null ){ // can happen because of a bug in the table component
								ApplicationController.getInstance().getRetrieveModel().vClearSelection();
								return;
							}
							StringBuffer sbError = new StringBuffer(100);
							if( me.isControlDown() ){
								if( me.isShiftDown() ){
									retrieve_model.vShowDAS( urlSelected, null );
								} else {
									retrieve_model.vShowDDS( urlSelected, null );
								}
							} else {
								retrieve_model.vShowURL( urlSelected, null );
							}
						}
					}
				}
			);
		}
	}

	boolean zDisplayingSubset(){ return mzDisplayingSubset; }

	public void vDatasets_Add( DodsURL[] aURLsToAdd ){
		vDatasets_Add( aURLsToAdd, true );
	}

	public void vDatasets_Add( final DodsURL[] aURLsToAdd, boolean zAddToRecent ){

		if(aURLsToAdd == null){
			ApplicationController.getInstance().vShowStatus("Warning, null object sent to dataset add in URLTable: ");
			Thread.yield();
			Thread.dumpStack();
			return;
		}
		int ctURLsToAdd = aURLsToAdd.length;
		if(aURLsToAdd.length == 0) {
			ApplicationController.getInstance().vShowStatus("You currently have no URLs selected to download");
			return;
		}
		int ctURLsExisting;
		if( madodsurlSelected == null ){
			ctURLsExisting = 0;
		} else {
			ctURLsExisting = madodsurlSelected.length;
		}
		if( ctURLsExisting == 0 ){
			madodsurlSelected = aURLsToAdd;
			for( int xURL = 0; xURL < ctURLsToAdd; xURL++ ){
				String sTitle = aURLsToAdd[xURL].getTitle();
				ApplicationController.getInstance().vShowStatus("Selected URL " + sTitle + " " + aURLsToAdd[xURL].getFullURL() );
			}
		} else {
			DodsURL[] adodsurlNew = new DodsURL[madodsurlSelected.length + ctURLsToAdd];

			// add existing URLs to new array
			for( int xURL = 0; xURL < ctURLsExisting; xURL++ ){
				adodsurlNew[xURL] = madodsurlSelected[xURL];
			}

			// add new URLs to array
			for( int xURL = 0; xURL < ctURLsToAdd; xURL++ ){
				adodsurlNew[ctURLsExisting + xURL] = aURLsToAdd[xURL];
				String sTitle = aURLsToAdd[xURL].getTitle();
				ApplicationController.getInstance().vShowStatus("Selected URL " + sTitle + " " + aURLsToAdd[xURL].getFullURL() );
			}
			madodsurlSelected = adodsurlNew;
		}
		vSynchronizeDisplayList();
		fireTableDataChanged();
		if( mzShowTypes ){
			ListModelTypeChecker theListModelTypeChecker = new ListModelTypeChecker();
			theListModelTypeChecker.start();
		}
		if( zAddToRecent ){
			if( ConfigurationManager.getInstance().getProperty_MODE_ReadOnly() ){
				// do not add recent files
			} else {
				for( int xURL = 0; xURL < aURLsToAdd.length; xURL++ ){
					Utility.vAddRecent(aURLsToAdd[xURL]);
					ApplicationController.getInstance().getAppFrame().vUpdateRecent();
				}
			}
		}

		// show the first URL that was added
		DodsURL urlFirstAdded = aURLsToAdd[0];
		int xSelection = ctURLsExisting;
		mjtableControl.getSelectionModel().setSelectionInterval( xSelection, xSelection );
		ApplicationController.getInstance().getRetrieveModel().vShowURL( urlFirstAdded, null );

// stop doing auto subset
//		if( mzAutoSubset ){
//			final DodsURL urlFirst = aURLsToAdd[0];
//			Continuation_DoCancel con = new Continuation_DoCancel(){
//				public void Do(){
//					// automatically activate DDS for newly selected URL
//					DodsURL[] aDisplayedURLs = Model_URLTable.this.getDisplayURLs();
//					for( int xURL = 0; xURL < aDisplayedURLs.length; xURL++ ){
//						if( urlFirst == aDisplayedURLs[xURL] ){
//							mjtableControl.getSelectionModel().setSelectionInterval(xURL, xURL); // apparently to select a row you pass the interval should be the desired row and the next row
//							StringBuffer sbError = new StringBuffer(100);
//							if( !ApplicationController.getInstance().getRetrieveModel().zShowURL(urlFirst, sbError) ){
//								ApplicationController.getInstance().vShowError("Subset display for URL " + urlFirst + " failed: " + sbError);
//								return;
//							}
//						}
//					}
//					ApplicationController.getInstance().getRetrieveModel().vUpdateSelected();
//				}
//				public void Cancel(){}
//			};
//			AutoNetUpdate theAutoNetUpdate = new AutoNetUpdate(con, urlFirst);
//			theAutoNetUpdate.start();
//		}
	}

	// gets the URL from the list active in the control
	public DodsURL getDisplayURL( int xURL_0){
		// TODO
		return null;
	}

	public void vDatasets_Delete( int[] aiIndicesToRemove ){
		// TODO
	}

	// gets the currently user-selected URLs
	public DodsURL[] getSelectedURLs( StringBuffer sbError ){
		// TODO
		sbError.append("not implemented");
		return null;
	}

	/** this is the only method that should modify the JList element collection */
	void vSynchronizeDisplayList(){
		try {

			// determine if the list will only contain a subset of the selected URLs
			if( madodsurlSelected.length > MAX_URLs_TO_DISPLAY ){
				mzDisplayingSubset = true;
				madodsurlDisplay = new DodsURL[MAX_URLs_TO_DISPLAY];
				for( int xURL = 0; xURL < MAX_URLs_TO_DISPLAY; xURL++ ){
					madodsurlDisplay[xURL] = madodsurlSelected[xURL];
				}
			} else {
				mzDisplayingSubset = false;
				madodsurlDisplay = madodsurlSelected;
			}
			DodsURL[] aDisplayedURLs = getDisplayURLs();
			if( aDisplayedURLs == null ) return;
        } catch(Exception ex) {
		   Utility.vUnexpectedError( ex, "Unexpected error synchronizing display list");
		} finally {
			ApplicationController.getInstance().getRetrieveModel().vValidateRetrieval();
		}
	}

	void vAddToFavorites(int[] aiIndicesToAdd){
		for( int xSelection = 0; xSelection < aiIndicesToAdd.length; xSelection++ ){
			Utility.vAddFavorite(madodsurlSelected[aiIndicesToAdd[xSelection]]);
		}
		ApplicationController.getInstance().getAppFrame().vUpdateFavorites();
	}

	void vAddToFavorites( DodsURL url ){
		Utility.vAddFavorite(url);
		ApplicationController.getInstance().getAppFrame().vUpdateFavorites();
	}

	private void vUpdateSelectionAfterRemoval(){

		// format columns (table model must exist before column model can be accessed)
		javax.swing.table.TableColumnModel cmDatasets = mjtableControl.getColumnModel();
		cmDatasets.getColumn(0).setPreferredWidth(10);
		cmDatasets.getColumn(0).setMaxWidth(10);
		cmDatasets.getColumn(1).setPreferredWidth(250);

		int iRowSelected = mjtableControl.getSelectedRow();
		if( iRowSelected < 0 ){
			mjtableControl.setRowSelectionInterval(0, 0);
			iRowSelected = 0;
		}
		Model_Retrieve retrieve_model = ApplicationController.getInstance().getRetrieveModel();
		Model_URL_control urllist = retrieve_model.getURLList();
		DodsURL urlSelected = urllist.getDisplayURL(iRowSelected);
		ApplicationController.getInstance().getRetrieveModel().vShowURL(urlSelected, null);
	}

	public void vDatasets_DeleteAll(){
		madodsurlDisplay = new DodsURL[0];
		madodsurlSelected = new DodsURL[0];
		mzDisplayingSubset = false;
		mjtableControl.invalidate();
		fireTableDataChanged();
		fireTableStructureChanged();
		StringBuffer sb = new StringBuffer(80);
		ApplicationController.getInstance().getRetrieveModel().vClearSelection();
	}

	public void vSelectedDatasets_Remove( int[] aiIndicesToRemove ){
		try {
			if(aiIndicesToRemove == null){
				ApplicationController.getInstance().vShowStatus("no URLs selected for removal");
				return;
			}
			int ctURLsToRemove = aiIndicesToRemove.length;
			if(aiIndicesToRemove.length == 0) {
				ApplicationController.getInstance().vShowStatus("you currently have no URLs selected for removal");
				return;
			}
			int ctURLsExisting;
			if( madodsurlSelected == null ){
				ctURLsExisting = 0;
				return;
			} else {
				ctURLsExisting = madodsurlSelected.length;
			}

			if( madodsurlSelected.length == ctURLsToRemove ){
				madodsurlSelected = new DodsURL[0];;
				madodsurlDisplay = new DodsURL[0];;
				ctURLsExisting = 0;
			} else {
				int ctRemainingURLs = madodsurlSelected.length - ctURLsToRemove;
				DodsURL[] adodsurlNew = new DodsURL[ctRemainingURLs];

				// copy existing URLs to new array omitting those being deleted
				int xNewURL = 0;
				for( int xURL = 0; xURL < ctURLsExisting; xURL++ ){
					int xRemoveList = 0;
					while(true){
						if( xRemoveList == ctURLsToRemove ){
							if( xNewURL > ctRemainingURLs ){
								ApplicationController.getInstance().vShowError("internal error; post-removal array too small"); // sure hope this doesn't happen
							} else {
								adodsurlNew[xNewURL] = madodsurlSelected[xURL];
								xNewURL++;
							}
							break;
						}
						if( aiIndicesToRemove[xRemoveList] == xURL ){
							ApplicationController.getInstance().vShowStatus("Removed selected URL " + madodsurlSelected[xURL].getTitle() + " " + madodsurlSelected[xURL].getFullURL());
							break; // this URL is getting removed
						}
						xRemoveList++; // continue searching removal list
					}
				}

				// replace the old URL list with the thinned one
				madodsurlSelected = adodsurlNew;
			}
			vSynchronizeDisplayList();
			fireTableDataChanged();
			fireTableStructureChanged();
			vUpdateSelectionAfterRemoval();
			ApplicationController.getInstance().getRetrieveModel().vValidateRetrieval();
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowError("unexpected error removing URLs: " + ex);
		}
	}

	// no longer used - wasn't really reliable because the digests sometimes matched
	void vApplyCE(String sConstraintExpression, int iDigest){
		StringBuffer sbError = new StringBuffer(80);
		DodsURL[] urls = getSelectedURLs(sbError);
		if( urls == null ){
			ApplicationController.vShowWarning("nothing selected: " + sbError);
			return;
		}
		int ctWarning = 0;
		for( int xSelectedURL = 0; xSelectedURL < urls.length; xSelectedURL++ ){
			DodsURL urlCurrent = urls[xSelectedURL];
			if( urlCurrent.getDigest() == iDigest ){
				urlCurrent.setConstraintExpression(sConstraintExpression);
				if( urlCurrent.getFullURL().length() > 1024 ){
					ctWarning++;
					ApplicationController.getInstance().vShowWarning("URL exceeds 1 kilobyte in length; may fail in some environments [" + urlCurrent.getFullURL().substring(0, 1024) + "...]" );
				}
			} else {
				ApplicationController.vShowWarning("attempt to apply constraint expression to non-matching dataset [" + urlCurrent.getTitle() + "]");
			}
		}
		if( ctWarning > 1 ){
			ApplicationController.vShowWarning("There were " + ctWarning + " URLs longer than 1 kilobyte");
		}
		ApplicationController.getInstance().getRetrieveModel().vValidateRetrieval();
	}

	private ArrayList mlistKnownURLs = new ArrayList();
	void addKnownURL( DodsURL url ){
		if( url != null ){
			mlistKnownURLs.add( url );
		}
	}
	DodsURL getKnownURL( String sFullURL ){
		for( int xList = 0; xList < mlistKnownURLs.size(); xList++ ){
			DodsURL urlKnown = (DodsURL)mlistKnownURLs.get(xList);
			if( urlKnown.getFullURL().equals( sFullURL ) ) return urlKnown;
		}
		return null;
	}

//	class AutoNetUpdate extends Thread {
//		Continuation_DoCancel mContinuation = null;
//		DodsURL murlTriggersCEUpdate = null;
//		public AutoNetUpdate(){
//			this.setDaemon(true);
//		}
//		public AutoNetUpdate(Continuation_DoCancel con, DodsURL urlTriggering){
//			this.setDaemon(true);
//			mContinuation = con;
//			murlTriggersCEUpdate = urlTriggering;
//		}
//		public void run(){
//			try {
//				ApplicationController.getInstance().setAutoUpdating(true);
//				StringBuffer sbError = new StringBuffer(80);
//				for(int i=0;i<madodsurlSelected.length;i++) {
//					sleep(100); // reduces the priority of this thread
//					DodsURL url = madodsurlSelected[i];
//					if( url.getDDS_Full() == null && !url.getDDS_Error() ){ // do not try to update if we already got an error trying to update this URL
//						if( url == murlTriggersCEUpdate ){
//							if( !url.zUpdateFromServer(null, null, mContinuation, sbError) ){
//								ApplicationController.getInstance().vShowError("Subset update for URL " + url.getTitle() + " failed: " + sbError);
//								return;
//							}
//						} else {
//							if( !url.zUpdateFromServer(null, null, null, sbError) ){
//								ApplicationController.getInstance().vShowError("Subset update for URL " + url.getTitle() + " failed: " + sbError);
//								return;
//							}
//						}
//		    			ApplicationController.getInstance().getRetrieveModel().vValidateRetrieval();
//					}
//				}
//				DodsURL urlSelected = Model_URLTable.this.getSelectedURL();
//				if( urlSelected != null ){
//					if( !ApplicationController.getInstance().getRetrieveModel().zShowURL(urlSelected, sbError) ){
//						ApplicationController.getInstance().vShowError("Subset display for selected URL failed: " + sbError);
//						return;
//					}
//				}
//			} catch( Exception ex ) {
//				ApplicationController.vShowError("Unexpected error auto subsetting: " +  ex);
//			} finally {
//				ApplicationController.getInstance().setAutoUpdating(false);
//			}
//		}
//	}
	class ListModelTypeChecker extends Thread {
		public ListModelTypeChecker(){
			this.setDaemon(true);
		}
		public void run(){
			try {
				StringBuffer sbError = new StringBuffer(120);
				for(int i=0;i<madodsurlSelected.length;i++) {
					sleep(500); // reduces the priority of this thread
					if( madodsurlSelected[i].getDigest() != 0 ) continue;
					int iURLType = madodsurlSelected[i].getType();
					String sBaseURL = madodsurlSelected[i].getBaseURL();
					if( iURLType  == DodsURL.TYPE_Directory ){
						sBaseURL = ApplicationController.getInstance().getRetrieveModel().sFetchDirectoryTree_GetFirstFile( sBaseURL, null );
						if( sBaseURL == null ){
							madodsurlSelected[i].setUnreachable(true, "could not find file in directory tree");
							continue;
						}
					}
					OpendapConnection conn = new OpendapConnection();
					conn.setUserAgent(ApplicationController.getInstance().getVersionString());
					sbError.setLength(0);
					DDS dds = conn.getDDS(sBaseURL, null, null, sbError);
					if( dds == null ){
						madodsurlSelected[i].setUnreachable(true, "unable to get DDS: " + sbError);
						continue; // try next url
					}
					madodsurlSelected[i].setDDS_Full(dds);
					String sDDSText = dds.getDDSText();
					if( sDDSText == null ){
						madodsurlSelected[i].setUnreachable(true, "DDS text unavailable");
						continue;
					}
					int iDigest = sDDSText.hashCode();
					madodsurlSelected[i].setDigest(iDigest);
					ApplicationController.getInstance().getRetrieveModel().vValidateRetrieval();
				}
			} catch( Exception ex ) {
				ApplicationController.vShowError("Unexpected error determing dataset types: " +  ex);
			}
		}
	}
}



