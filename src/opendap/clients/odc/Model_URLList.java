package opendap.clients.odc;

/**
 * Title:        Model_URLList
 * Description:  Models the list of URLs for displays such as Recent and Favorites
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.59
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

import opendap.dap.*;

public class Model_URLList extends javax.swing.DefaultListModel implements Model_Datasets {

	private Panel_URLList mpanelList = null;

    public Model_URLList( boolean zShowTypes ) {
		mzShowTypes = zShowTypes;
		mpanelList = null;
	}

	private final static int MAX_URLs_TO_DISPLAY = 100;
	private boolean zDisplayingSubset = false;
	private Model_Dataset[] madodsurlDisplay = new Model_Dataset[0]; // URLs displayed in list (may be only a subset of selected if many are selected)
	private Model_Dataset[] madodsurlSelected = new Model_Dataset[0]; // all the URLs selected
	private boolean mzShowTypes = true;

	public void setControl( Panel_URLList list_panel ){
		mpanelList = list_panel;
	}
	public Panel_URLList getControl(){
		return mpanelList;
	}

	boolean zDisplayingSubset(){ return zDisplayingSubset; }
	public Model_Dataset[] getDisplayURLs(){ return madodsurlDisplay; }
	public Model_Dataset[] getAllURLs(){ return madodsurlSelected; }
	public Model_Dataset getSelectedURL(){
		if( madodsurlDisplay == null ) return null;
		if( mpanelList == null ){
			ApplicationController.getInstance().vShowWarning( "selection list control was undefined");
			return null;
		}
		int iSelectedIndex = mpanelList.getSelectedIndex();
		if (iSelectedIndex == -1)return null;
		Model_Dataset urlSelected = madodsurlDisplay[iSelectedIndex];
		if (urlSelected == null) {
			ApplicationController.getInstance().vShowWarning( "internal error: selected item " + iSelectedIndex + " was null");
			return null;
		}
		return urlSelected;
	}
	boolean getShowTypes(){ return mzShowTypes; }
	Model_Dataset getURL(int index){
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

	public Model_Dataset getDisplayURL( int xURL_0 ){
		Model_Dataset[] aurlDisplay = this.getDisplayURLs();
		if( aurlDisplay == null ) return null;
		if( aurlDisplay.length < 0 || aurlDisplay.length < xURL_0 ) return null;
		return aurlDisplay[xURL_0];
	}

	// gets the currently user-selected URLs
	public Model_Dataset[] getSelectedURLs_NamedList( StringBuffer sbError ){ // zero-based
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
				Model_Dataset[] adodsurl = new Model_Dataset[ctURLsToGet];
				for( int xIndex = 0; xIndex < ctURLsToGet; xIndex++ ){
					adodsurl[xIndex] = madodsurlSelected[aiIndices[xIndex]]; // risky consider validating
				}
				return adodsurl;
			}
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return null;
		}
	}

	public void vDatasets_Add( Model_Dataset[] aURLsToAdd ){
		vDatasets_Add( aURLsToAdd, true );
	}

	public void vDatasets_Add( Model_Dataset[] aURLsToAdd0, boolean zAddToRecent ){
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
			Model_Dataset[] adodsurlNew = new Model_Dataset[madodsurlSelected.length + ctURLsToAdd];

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
					Panel_Select_Recent.vAddRecent(aURLsToAdd0[xURL]);
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
				madodsurlDisplay = new Model_Dataset[MAX_URLs_TO_DISPLAY];
				for (int xURL = 0; xURL < MAX_URLs_TO_DISPLAY; xURL++) {
					madodsurlDisplay[xURL] = madodsurlSelected[xURL];
				}
			}
			else {
				zDisplayingSubset = false;
				madodsurlDisplay = madodsurlSelected;
			}
			Model_Dataset[] aDisplayedURLs = this.getDisplayURLs();
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
			ApplicationController.vUnexpectedError(ex, new StringBuffer("while synchronizing selection list"));
		}
	}

	void vAddToRecent( Model_Dataset[] aURLsToAdd ){
		// not implemented
	}

	void vAddToFavorites( Model_Dataset[] aURLsToAdd ){
		if( aURLsToAdd == null ){
			ApplicationController.getInstance().vShowWarning("Internal error, attempt to add null to favorites.");
			return;
		}
		int ctURLs = aURLsToAdd.length;
		for( int xSelection = 0; xSelection < ctURLs; xSelection++ ){
			Panel_Select_Favorites.vAddFavorite( aURLsToAdd[xSelection] );
		}
		ApplicationController.getInstance().getAppFrame().vUpdateFavorites();
	}

	void vAddToFavorites(int[] aiIndicesToAdd){
		if( aiIndicesToAdd == null ) return;
		int ctURLs = aiIndicesToAdd.length;
		Model_Dataset[] aSelectedURLs = new Model_Dataset[ctURLs];
		for( int xSelection = 0; xSelection < ctURLs; xSelection++ ){
			aSelectedURLs[xSelection] = madodsurlSelected[aiIndicesToAdd[xSelection]];
		}
		vAddToFavorites( aSelectedURLs );
	}

	void vAddToFavorites( Model_Dataset url ){
		Panel_Select_Favorites.vAddFavorite(url);
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
		madodsurlDisplay = new Model_Dataset[0]; // URLs displayed in list (may be only a subset of selected if many are selected)
		madodsurlSelected = new Model_Dataset[0]; // all the URLs selected
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
		Model_Dataset[] urls = getSelectedURLs( sbError );
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
					if( iURLType  == Model_Dataset.TYPE_Directory ){
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
				ApplicationController.vUnexpectedError( ex, "Unexpected error determing dataset types" );
			}
		}
	}

	public Model_Dataset[] getSelectedURLs( StringBuffer sbError ){ // zero-based
		Model_Dataset[] aurlNamedSelections = getSelectedURLs_NamedList( sbError );
		if( aurlNamedSelections == null ) return null;
		return getSubSelectedURLs( aurlNamedSelections, sbError );
	}

	// zero-based
	Model_Dataset[] getSubSelectedURLs( Model_Dataset[] aurlSelected, StringBuffer sbError ){
		if( aurlSelected == null ) return null;
		int ctDataURLs = 0;
		for( int xURL = 0; xURL < aurlSelected.length; xURL++ ){
			if( aurlSelected[xURL].getType() == Model_Dataset.TYPE_Data || aurlSelected[xURL].getType() == Model_Dataset.TYPE_Image )
			    ctDataURLs++;
		}
		int ctDirectoryURLs = 0;
		for( int xURL = 0; xURL < aurlSelected.length; xURL++ ){
			if( aurlSelected[xURL].getType() == Model_Dataset.TYPE_Directory ){
				Model_DirectoryTree tree = aurlSelected[xURL].getDirectoryTree();
                if( tree == null ) continue;
				DirectoryTreeNode nodeRoot = (DirectoryTreeNode)tree.getRoot();
				ctDirectoryURLs += getSubSelectedURLs_RecursiveCount(nodeRoot);
				String sDirTitle = aurlSelected[xURL].getTitle();
				String sBaseURL = aurlSelected[xURL].getBaseURL();
				String sCE = aurlSelected[xURL].getConstraintExpression_Encoded();
				Model_Dataset[] aDirectoryURLs = getSubSelectedURLs_Recursive(sDirTitle, sBaseURL, nodeRoot, sCE);
			}
		}
		Model_Dataset[] aurlCumulative = new Model_Dataset[ctDataURLs + ctDirectoryURLs];
		int xDataURL = -1;
		for( int xURL = 0; xURL < aurlSelected.length; xURL++ ){
			if( aurlSelected[xURL].getType() == Model_Dataset.TYPE_Data  || aurlSelected[xURL].getType() == Model_Dataset.TYPE_Image ){
				xDataURL++;
				aurlCumulative[xDataURL] = aurlSelected[xURL];
			}
		}
		int xDirectoryURL = -1;
		for( int xURL = 0; xURL < aurlSelected.length; xURL++ ){
			if( aurlSelected[xURL].getType() == Model_Dataset.TYPE_Directory ){
				Model_DirectoryTree tree = aurlSelected[xURL].getDirectoryTree();
                if( tree == null ) continue;
				DirectoryTreeNode nodeRoot = (DirectoryTreeNode)tree.getRoot();
				String sDirTitle = aurlSelected[xURL].getTitle();
				String sBaseURL = aurlSelected[xURL].getBaseURL();
				String sCE = aurlSelected[xURL].getConstraintExpression_Encoded();
				Model_Dataset[] aDirectoryURLs = getSubSelectedURLs_Recursive(sDirTitle, sBaseURL, nodeRoot, sCE);
				if( aDirectoryURLs == null ) continue;
				for( int xCurrentDirectory = 0; xCurrentDirectory < aDirectoryURLs.length; xCurrentDirectory++ ){
					xDirectoryURL++;
                    int xCumulative = (xDataURL+1) + xDirectoryURL;
                    if( xCumulative >= aurlCumulative.length ){
                        ApplicationController.vShowError("internal error; inconsistent selection tree count");
                        break;
                    }
					aurlCumulative[xCumulative] = aDirectoryURLs[xDirectoryURL];
				}
			}
		}
		return aurlCumulative;
	}
	private int getSubSelectedURLs_RecursiveCount(DirectoryTreeNode node){
		if( node == null ) return 0;
		int ctSelectedFiles = 0;
		if( node.isSelected() ) ctSelectedFiles += node.getFileList_SelectedCount();
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			DirectoryTreeNode nodeChild = node.getChild(xChild);
			ctSelectedFiles += getSubSelectedURLs_RecursiveCount(nodeChild);
		}
		return ctSelectedFiles;
	}
	private Model_Dataset[] getSubSelectedURLs_Recursive( String sDirTitle, String sBaseURL, DirectoryTreeNode node, String sCE){
		if( node == null ) return null;
		int ctSelectedFiles = 0;
		if( node.isSelected() ) ctSelectedFiles += node.getFileList_SelectedCount();
		Model_Dataset[] aURLselected = null;
		if( ctSelectedFiles > 0 ){
			aURLselected = new Model_Dataset[ctSelectedFiles];
			String[] asSelectedFiles = node.getFileList_Selected(); // one-based
			String[] asSelectedHREFs = node.getHREFList_Selected(); // one-based
			for( int xFile = 1; xFile <= ctSelectedFiles; xFile++ ){
				String sDirectory = Utility.sConnectPaths(sBaseURL, "/", node.getPathString());
				String sURL = asSelectedHREFs[xFile]; // if it is an image we allow for an out-of-path reference
				if( !sURL.toUpperCase().startsWith("HTTP://") ){ // if URL is relative or bad then construct it
					sURL = Utility.sConnectPaths(sDirectory, "/", asSelectedFiles[xFile]);
				}
				if( Utility.isImage( sURL ) ){
					aURLselected[xFile-1] = new Model_Dataset(sURL, Model_Dataset.TYPE_Image);
					aURLselected[xFile-1].setTitle(asSelectedFiles[xFile]);
				} else {
					aURLselected[xFile-1] = new Model_Dataset(sURL, Model_Dataset.TYPE_Data);
					aURLselected[xFile-1].setTitle(sDirTitle + " " + asSelectedFiles[xFile] + " " + sCE);
					aURLselected[xFile-1].setConstraintExpression(sCE);
				}
			}
		}
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			DirectoryTreeNode nodeChild = node.getChild(xChild);
			Model_Dataset[] aURLsubselected = getSubSelectedURLs_Recursive(sDirTitle, sBaseURL, nodeChild, sCE);
			if( aURLsubselected != null ){
				if( aURLselected == null ){
					aURLselected = aURLsubselected;
				} else {
					int ctTotalURLs = aURLselected.length + aURLsubselected.length;
					Model_Dataset[] aurlBuffer = new Model_Dataset[ctTotalURLs];
					System.arraycopy(aURLselected, 0, aurlBuffer, 0, aURLselected.length);
					System.arraycopy(aURLsubselected, 0, aurlBuffer, aURLselected.length, aURLsubselected.length);
					aURLselected = aurlBuffer;
				}
			}
		}
		return aURLselected;
	}

}



