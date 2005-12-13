package opendap.clients.odc;

/**
 * Title:        Model_URLList
 * Description:  Models the list of URLs for displays such as Recent and Favorites
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.59
 */

import opendap.dap.*;

public class Model_URLList extends javax.swing.DefaultListModel implements Model_URL_control {

	private Panel_URLList mpanelList = null;

    public Model_URLList( boolean zShowTypes ) {
		mzShowTypes = zShowTypes;
		mpanelList = null;
	}

	private final static int MAX_URLs_TO_DISPLAY = 100;
	private boolean zDisplayingSubset = false;
	private DodsURL[] madodsurlDisplay = new DodsURL[0]; // URLs displayed in list (may be only a subset of selected if many are selected)
	private DodsURL[] madodsurlSelected = new DodsURL[0]; // all the URLs selected
	private boolean mzShowTypes = true;

	public void setControl( Panel_URLList list_panel ){
		mpanelList = list_panel;
	}
	public Panel_URLList getControl(){
		return mpanelList;
	}

	boolean zDisplayingSubset(){ return zDisplayingSubset; }
	public DodsURL[] getDisplayURLs(){ return madodsurlDisplay; }
	public DodsURL[] getAllURLs(){ return madodsurlSelected; }
	public DodsURL getSelectedURL(){
		if( madodsurlDisplay == null ) return null;
		if( mpanelList == null ){
			ApplicationController.getInstance().vShowWarning( "selection list control was undefined");
			return null;
		}
		int iSelectedIndex = mpanelList.getSelectedIndex();
		if (iSelectedIndex == -1)return null;
		DodsURL urlSelected = madodsurlDisplay[iSelectedIndex];
		if (urlSelected == null) {
			ApplicationController.getInstance().vShowWarning( "internal error: selected item " + iSelectedIndex + " was null");
			return null;
		}
		return urlSelected;
	}
	boolean getShowTypes(){ return mzShowTypes; }
	DodsURL getURL(int index){
		if( madodsurlDisplay == null ) return null;
		if( index < 0 ) return null;
		if( index >= madodsurlDisplay.length ) return null;
		return madodsurlDisplay[index];
	}
	int getSelectedURLsCount() {
		if( mpanelList == null ){
			ApplicationController.getInstance().vShowError("internal error while getting url count: model has no control");
			return 0;
		}
		int[] aiIndices = mpanelList.getSelectedIndices();
		if(aiIndices == null) return 0;
		return aiIndices.length;
	}

	public DodsURL getDisplayURL( int xURL_0 ){
		DodsURL[] aurlDisplay = this.getDisplayURLs();
		if( aurlDisplay == null ) return null;
		if( aurlDisplay.length < 0 || aurlDisplay.length < xURL_0 ) return null;
		return aurlDisplay[xURL_0];
	}

	// gets the currently user-selected URLs
	public DodsURL[] getSelectedURLs( StringBuffer sbError ){ // zero-based
		if( madodsurlDisplay == null ) return null;
		if( mpanelList == null ){
			sbError.append("internal error, model has no control");
			return null;
		}
		int[] aiIndices = mpanelList.getSelectedIndices();
		try {
			if(aiIndices == null){
				return null;
			}
			int ctURLsToGet = aiIndices.length;
			if(ctURLsToGet == 0) {
				return null;
			}
			int ctURLsExisting;
			if( madodsurlSelected == null ){
				ctURLsExisting = 0;
				return null;
			} else {
				ctURLsExisting = madodsurlSelected.length;
			}
			if( madodsurlSelected.length == ctURLsToGet ){
				return madodsurlSelected;
			} else {
				DodsURL[] adodsurl = new DodsURL[ctURLsToGet];
				for( int xIndex = 0; xIndex < ctURLsToGet; xIndex++ ){
					adodsurl[xIndex] = madodsurlSelected[aiIndices[xIndex]]; // risky consider validating
				}
				return adodsurl;
			}
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return null;
		}
	}

	public void vDatasets_Add( DodsURL[] aURLsToAdd ){
		vDatasets_Add( aURLsToAdd, true );
	}

	public void vDatasets_Add( DodsURL[] aURLsToAdd0, boolean zAddToRecent ){
		if(aURLsToAdd0 == null){
			return; // nothing to add
		}
		int ctURLsToAdd = aURLsToAdd0.length;
		if(aURLsToAdd0.length == 0) {
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
			madodsurlSelected = aURLsToAdd0;
		} else {
			DodsURL[] adodsurlNew = new DodsURL[madodsurlSelected.length + ctURLsToAdd];

			// add existing URLs to new array
			for( int xURL = 0; xURL < ctURLsExisting; xURL++ ){
				adodsurlNew[xURL] = madodsurlSelected[xURL];
			}

			// add new URLs to array
			for( int xURL = 0; xURL < ctURLsToAdd; xURL++ ){
				adodsurlNew[ctURLsExisting + xURL] = aURLsToAdd0[xURL];
			}
			madodsurlSelected = adodsurlNew;
		}
		vSynchronizeJList();
		if( mzShowTypes ){
			ListModelTypeChecker theListModelTypeChecker = new ListModelTypeChecker();
			theListModelTypeChecker.start();
		}
		if( zAddToRecent ){
			if( ConfigurationManager.getInstance().getProperty_MODE_ReadOnly() ){
				// do not add recent files
			} else {
				for( int xURL = 0; xURL < aURLsToAdd0.length; xURL++ ){
					Utility.vAddRecent(aURLsToAdd0[xURL]);
					ApplicationController.getInstance().getAppFrame().vUpdateRecent();
				}
			}
		}

	}

	/** this is the only method that should modify the JList element collection */
	void vSynchronizeJList() {
		try {

			// determine if the list will only contain a subset of the selected URLs
			if (madodsurlSelected.length > MAX_URLs_TO_DISPLAY) {
				zDisplayingSubset = true;
				madodsurlDisplay = new DodsURL[MAX_URLs_TO_DISPLAY];
				for (int xURL = 0; xURL < MAX_URLs_TO_DISPLAY; xURL++) {
					madodsurlDisplay[xURL] = madodsurlSelected[xURL];
				}
			}
			else {
				zDisplayingSubset = false;
				madodsurlDisplay = madodsurlSelected;
			}
			DodsURL[] aDisplayedURLs = this.getDisplayURLs();
			if (aDisplayedURLs == null) {
				this.removeAllElements();
				return;
			}

			// add any URLs that are in the display list but not in the JTable
			int ctItemsAdded = 0;
			for (int xURL = 0; xURL < aDisplayedURLs.length; xURL++) {
				int ctElements = this.getSize();
				int xElement = 0;
				while (true) {
					if (xElement >= ctElements) { // did not find url in JList
						this.addElement(aDisplayedURLs[xURL]);
						ctItemsAdded++;
						ctElements = this.getSize();
						break;
					}
					Object oElement = this.getElementAt(xElement);
					if (oElement == aDisplayedURLs[xURL]) { // list has the displayed url
						break; // go onto next url
					}
					xElement++;
				}
			}

			// remove any URLs that are not in the display list but are in the JList
			int ctElements = this.getSize();
			int xElement = 0;
			int ctItemsRemoved = 0;
			while( true ){
				if( xElement >= ctElements )
					break; // done
				Object oElement = this.getElementAt(xElement);
				int xURL = 0;
				while( true ){
					if (xURL >= aDisplayedURLs.length) { // not in display list
						this.removeElementAt(xElement);
						ctItemsRemoved++;
						xElement = 0; // start scanning from beginning of JList again just to be sure
						break;
					}
					if (oElement == aDisplayedURLs[xURL]) { // is in display list
						xElement++;
						break;
					}
					xURL++;
				}
			}

			if( this.zDisplayingSubset() ){
				int ctURLs = this.getAllURLs().length;
				this.addElement("[remaining " + (ctURLs - aDisplayedURLs.length) + " URLs not displayed]");
			}

			if( ctItemsAdded > 0 || ctItemsRemoved > 0 ){
				this.fireContentsChanged( this, 0, this.getSize() - 1 );
			}

		} catch (Exception ex) {
			Utility.vUnexpectedError(ex, new StringBuffer("while synchronizing selection list"));
		}
	}

	void vAddToRecent( DodsURL[] aURLsToAdd ){
		// not implemented
	}

	void vAddToFavorites( DodsURL[] aURLsToAdd ){
		if( aURLsToAdd == null ){
			ApplicationController.getInstance().vShowWarning("Internal error, attempt to add null to favorites.");
			return;
		}
		int ctURLs = aURLsToAdd.length;
		for( int xSelection = 0; xSelection < ctURLs; xSelection++ ){
			Utility.vAddFavorite( aURLsToAdd[xSelection] );
		}
		ApplicationController.getInstance().getAppFrame().vUpdateFavorites();
	}

	void vAddToFavorites(int[] aiIndicesToAdd){
		if( aiIndicesToAdd == null ) return;
		int ctURLs = aiIndicesToAdd.length;
		DodsURL[] aSelectedURLs = new DodsURL[ctURLs];
		for( int xSelection = 0; xSelection < ctURLs; xSelection++ ){
			aSelectedURLs[xSelection] = madodsurlSelected[aiIndicesToAdd[xSelection]];
		}
		vAddToFavorites( aSelectedURLs );
	}

	void vAddToFavorites( DodsURL url ){
		Utility.vAddFavorite(url);
		ApplicationController.getInstance().getAppFrame().vUpdateFavorites();
	}

	public void vDatasets_DeleteSelected(){
	    int[] aiSelectedToDelete = mpanelList.getSelectedIndices();
		if( aiSelectedToDelete == null || aiSelectedToDelete.length == 0 ){
			ApplicationController.getInstance().vShowStatus("Nothing selected for delete request.");
			return;
		}
		this.vDatasets_Delete(aiSelectedToDelete);
	}

	public void vDatasets_DeleteAll(){
		madodsurlDisplay = new DodsURL[0]; // URLs displayed in list (may be only a subset of selected if many are selected)
		madodsurlSelected = new DodsURL[0]; // all the URLs selected
		zDisplayingSubset = false;
		this.vSynchronizeJList();
	}

	public void vDatasets_Delete( int[] aiIndicesToRemove ){
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
				vDatasets_DeleteAll();
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
							break; // this URL is getting removed
						}
						xRemoveList++; // continue searching removal list
					}
				}

				// replace the old URL list with the thinned one
				madodsurlSelected = adodsurlNew;
			}
			this.vSynchronizeJList();
			ApplicationController.getInstance().getAppFrame().vActivateRetrievalPanel();
		} catch(Exception ex) {
			ApplicationController.getInstance().vShowError("unexpected error removing URLs: " + ex);
		}
	}

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
					ApplicationController.getInstance().vShowWarning("URL exceeds 1 kilobyte in length; may fail in some environments [" + urlCurrent.getFullURL().substring(0, 48) + "...]" );
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

	class ListModelTypeChecker extends Thread {
		public ListModelTypeChecker(){
			this.setDaemon(true);
		}
		public void run(){
			try {
				StringBuffer sbError = new StringBuffer(80);
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
				Utility.vUnexpectedError( ex, "Unexpected error determing dataset types" );
			}
		}
	}

}



