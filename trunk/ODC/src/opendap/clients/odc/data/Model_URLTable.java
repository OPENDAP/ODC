package opendap.clients.odc.data;

/**
 * Title:        Model_URLTable
 * Description:  Models contents of retrieved datasets panel
 * Copyright:    Copyright (c) 2002-12
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      3.08
 */

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

import java.util.*;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.OpendapConnection;
import opendap.clients.odc.data.Model_Dataset.DATASET_TYPE;
import opendap.clients.odc.gui.Panel_Select_Favorites;
import opendap.clients.odc.gui.Panel_Select_Recent;
import opendap.clients.odc.Model;
import opendap.dap.*;
import java.awt.event.*;

// **** CLASS CURRENTLY NOT IN USE ****

public class Model_URLTable extends javax.swing.table.AbstractTableModel implements Model_Datasets {

	final private static int MAX_URLs_TO_DISPLAY = 100;

	Model_Dataset[] madodsurlDisplay = null; // TODO this is wrong
	Model_Dataset[] madodsurlSelected = null;
	boolean mzShowTypes = false;
	boolean mzAutoSubset = false;
	boolean mzDisplayingSubset = false;

	Model_Dataset[] getDisplayURLs(){ return null; } // NO LONGER FUNCTIONAL

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
		Model_Dataset url = madodsurlDisplay[row];
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
							Model_Retrieve retrieve_model = Model.get().getRetrieveModel();
							Model_Datasets urllist = retrieve_model.getURLList(); // xxx wrong
							Model_Dataset urlSelected = null; // NONE OF THIS WORKS -- old logic urllist.getURL(iRowSelected);
							if( urlSelected == null ){ // can happen because of a bug in the table component
								Model.get().getRetrieveModel().vClearSelection();
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

	public void vDatasets_Add( Model_Dataset[] aURLsToAdd ){
		vDatasets_Add( aURLsToAdd, true );
	}

	public void vDatasets_Add( final Model_Dataset[] aURLsToAdd, boolean zAddToRecent ){

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
			Model_Dataset[] adodsurlNew = new Model_Dataset[madodsurlSelected.length + ctURLsToAdd];

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
					Panel_Select_Recent.vAddRecent(aURLsToAdd[xURL]);
					ApplicationController.getInstance().getAppFrame().vUpdateRecent();
				}
			}
		}

		// show the first URL that was added
		Model_Dataset urlFirstAdded = aURLsToAdd[0];
		int xSelection = ctURLsExisting;
		mjtableControl.getSelectionModel().setSelectionInterval( xSelection, xSelection );
		Model.get().getRetrieveModel().vShowURL( urlFirstAdded, null );

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
//							if( !Model.get().getRetrieveModel().zShowURL(urlFirst, sbError) ){
//								ApplicationController.getInstance().vShowError("Subset display for URL " + urlFirst + " failed: " + sbError);
//								return;
//							}
//						}
//					}
//					Model.get().getRetrieveModel().vUpdateSelected();
//				}
//				public void Cancel(){}
//			};
//			AutoNetUpdate theAutoNetUpdate = new AutoNetUpdate(con, urlFirst);
//			theAutoNetUpdate.start();
//		}
	}

	// gets the URL from the list active in the control
	public Model_Dataset getDisplayURL( int xURL_0){
		// TODO
		return null;
	}

	public void vDatasets_Delete( int[] aiIndicesToRemove ){
		// TODO
	}

	// gets the currently user-selected URLs
	public Model_Dataset[] getSelectedURLs( StringBuffer sbError ){
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
				madodsurlDisplay = new Model_Dataset[MAX_URLs_TO_DISPLAY];
				for( int xURL = 0; xURL < MAX_URLs_TO_DISPLAY; xURL++ ){
					madodsurlDisplay[xURL] = madodsurlSelected[xURL];
				}
			} else {
				mzDisplayingSubset = false;
				madodsurlDisplay = madodsurlSelected;
			}
			Model_Dataset[] aDisplayedURLs = getDisplayURLs();
			if( aDisplayedURLs == null ) return;
        } catch(Exception ex) {
		   ApplicationController.vUnexpectedError( ex, "Unexpected error synchronizing display list");
		} finally {
			Model.get().getRetrieveModel().vValidateRetrieval();
		}
	}

	void vAddToFavorites(int[] aiIndicesToAdd){
		for( int xSelection = 0; xSelection < aiIndicesToAdd.length; xSelection++ ){
			Panel_Select_Favorites.vAddFavorite(madodsurlSelected[aiIndicesToAdd[xSelection]]);
		}
		ApplicationController.getInstance().getAppFrame().vUpdateFavorites();
	}

	void vAddToFavorites( Model_Dataset url ){
		Panel_Select_Favorites.vAddFavorite(url);
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
		Model_Retrieve retrieve_model = Model.get().getRetrieveModel();
		Model_Datasets urllist = retrieve_model.getURLList();
		Model_Dataset urlSelected = urllist.getDisplayURL(iRowSelected);
		Model.get().getRetrieveModel().vShowURL(urlSelected, null);
	}

	public void vDatasets_DeleteAll(){
		madodsurlDisplay = new Model_Dataset[0];
		madodsurlSelected = new Model_Dataset[0];
		mzDisplayingSubset = false;
		mjtableControl.invalidate();
		fireTableDataChanged();
		fireTableStructureChanged();
		StringBuffer sb = new StringBuffer(80);
		Model.get().getRetrieveModel().vClearSelection();
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
				madodsurlSelected = new Model_Dataset[0];;
				madodsurlDisplay = new Model_Dataset[0];;
				ctURLsExisting = 0;
			} else {
				int ctRemainingURLs = madodsurlSelected.length - ctURLsToRemove;
				Model_Dataset[] adodsurlNew = new Model_Dataset[ctRemainingURLs];

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
			Model.get().getRetrieveModel().vValidateRetrieval();
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowError("unexpected error removing URLs: " + ex);
		}
	}

	// no longer used - wasn't really reliable because the digests sometimes matched
	void vApplyCE(String sConstraintExpression, int iDigest){
		StringBuffer sbError = new StringBuffer(80);
		Model_Dataset[] urls = getSelectedURLs(sbError);
		if( urls == null ){
			ApplicationController.vShowWarning("nothing selected: " + sbError);
			return;
		}
		int ctWarning = 0;
		for( int xSelectedURL = 0; xSelectedURL < urls.length; xSelectedURL++ ){
			Model_Dataset urlCurrent = urls[xSelectedURL];
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
		Model.get().getRetrieveModel().vValidateRetrieval();
	}

	private ArrayList<Model_Dataset> mlistKnownURLs = new ArrayList<Model_Dataset>();
	void addKnownURL( Model_Dataset url ){
		if( url != null ){
			mlistKnownURLs.add( url );
		}
	}
	Model_Dataset getKnownURL( String sFullURL ){
		for( int xList = 0; xList < mlistKnownURLs.size(); xList++ ){
			Model_Dataset urlKnown = (Model_Dataset)mlistKnownURLs.get(xList);
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
//		    			Model.get().getRetrieveModel().vValidateRetrieval();
//					}
//				}
//				DodsURL urlSelected = Model_URLTable.this.getSelectedURL();
//				if( urlSelected != null ){
//					if( !Model.get().getRetrieveModel().zShowURL(urlSelected, sbError) ){
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
					DATASET_TYPE eURLType = madodsurlSelected[i].getType();
					String sBaseURL = madodsurlSelected[i].getBaseURL();
					if( eURLType  == DATASET_TYPE.Directory ){
						sBaseURL = Model.get().getRetrieveModel().sFetchDirectoryTree_GetFirstFile( sBaseURL, null );
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
					Model.get().getRetrieveModel().vValidateRetrieval();
				}
			} catch( Exception ex ) {
				ApplicationController.vShowError("Unexpected error determing dataset types: " +  ex);
			}
		}
	}
}



