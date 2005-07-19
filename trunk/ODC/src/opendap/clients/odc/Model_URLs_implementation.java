package opendap.clients.odc;

/**
 * <p>Title: OPeNDAP Data Connector</p>
 * <p>Description: joint implementations for Model_URL_control</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: OPeNDAP.org</p>
 * @author John Chamberlain
 * @version 2.59
 */

// CLASS NO LONGER IN USE

public class Model_URLs_implementation {

	private final static int MAX_URLs_TO_DISPLAY = 100;
	private boolean zDisplayingSubset = false;
	private DodsURL[] madodsurlDisplay = new DodsURL[0]; // URLs displayed in list (may be only a subset of selected if many are selected)
	private DodsURL[] madodsurlSelected = new DodsURL[0]; // all the URLs selected
	private boolean mzShowTypes = false;
	private boolean mzAutoSubset = false;

    public Model_URLs_implementation() {}

	DodsURL[] getDisplayURLs(){ return madodsurlDisplay; }
	DodsURL getDisplayURL( int iURL ){ return madodsurlDisplay[iURL]; }
	DodsURL[] getAllURLs(){ return madodsurlSelected; }
	DodsURL getSelectedURL(){
		return null;
/* NO LONGER FUNCTIONAL
            if( madodsurlDisplay == null ) return null;
            if( mjtableControl == null ){
                ApplicationController.getInstance().vShowWarning("selection list control was undefined");
                return null;
            }
            int iSelectedIndex = mjtableControl.getSelectedRow();
            if( iSelectedIndex == -1 ) return null;
            DodsURL urlSelected = madodsurlDisplay[iSelectedIndex];
            if( urlSelected == null ){
                ApplicationController.getInstance().vShowWarning("internal error: selected item " + iSelectedIndex + " was null");
                return null;
            }
            return urlSelected;
*/
        }
	boolean getShowTypes(){ return mzShowTypes; }
	boolean getAutoSubset(){ return mzAutoSubset; }
	DodsURL getURL(int index){
		if( madodsurlDisplay == null ) return null;
		if( index < 0 ) return null;
		if( index >= madodsurlDisplay.length ) return null;
		return madodsurlDisplay[index];
	}
	int getSelectedURLsCount() {
		return 0;
/* NO LONGER FUNCTIONAL
		if( mjtableControl == null ){
			ApplicationController.getInstance().vShowError("internal error while getting url count: model has no control");
			return 0;
		}
		int[] aiIndices = mjtableControl.getSelectedRows();
		if(aiIndices == null) return 0;
		return aiIndices.length;
*/
	}

	// zero-based
	DodsURL[] getSelectedURLs(StringBuffer sbError){
return null;
/* NO LONGER FUNCTIONAL
		if( madodsurlDisplay == null ) return null;
		if( mjtableControl == null ){
			sbError.append("internal error, model has no control");
			return null;
		}
		int[] aiIndices = mjtableControl.getSelectedRows();
		try {
			if(aiIndices == null){
				sbError.append("null indices returned in getSelectedURLS");
				return null;
			}
			int ctURLsToGet = aiIndices.length;
			if(ctURLsToGet == 0) {
				sbError.append("you currently have no URLs selected");
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
*/
	}

	// gets all sub selections in case of a directory
	// zero-based
	DodsURL[] getSubSelectedURLs(int iIndex, StringBuffer sbError){
		DodsURL[] aurlSelected = this.getAllURLs();
		if( aurlSelected == null ) return null;
		DodsURL[] egg = new DodsURL[1];
		if( iIndex < 0 || iIndex >= aurlSelected.length ){
			sbError.append("Index " + iIndex + " out of range (" + 1 + " to " + aurlSelected.length + " )");
			return null;
		}
		egg[0] = aurlSelected[iIndex];
		return getSubSelectedURLs( egg, sbError );
	}

	// zero-based
	DodsURL[] getSubSelectedURLs(StringBuffer sbError){
		DodsURL[] aurlSelected = getSelectedURLs(sbError);
		if( aurlSelected == null ) return null;
		return getSubSelectedURLs( aurlSelected, sbError );
	}

	// zero-based
	DodsURL[] getSubSelectedURLs(DodsURL[] aurlSelected, StringBuffer sbError){
		if( aurlSelected == null ) return null;
		int ctDataURLs = 0;
		for( int xURL = 0; xURL < aurlSelected.length; xURL++ ){
			if( aurlSelected[xURL].getType() == DodsURL.TYPE_Data || aurlSelected[xURL].getType() == DodsURL.TYPE_Image )
			    ctDataURLs++;
		}
		int ctDirectoryURLs = 0;
		for( int xURL = 0; xURL < aurlSelected.length; xURL++ ){
			if( aurlSelected[xURL].getType() == DodsURL.TYPE_Directory ){
				Model_DirectoryTree tree = aurlSelected[xURL].getDirectoryTree();
                if( tree == null ) continue;
				DirectoryTreeNode nodeRoot = (DirectoryTreeNode)tree.getRoot();
				ctDirectoryURLs += getSubSelectedURLs_RecursiveCount(nodeRoot);
				String sDirTitle = aurlSelected[xURL].getTitle();
				String sBaseURL = aurlSelected[xURL].getBaseURL();
				String sCE = aurlSelected[xURL].getConstraintExpression_Encoded();
				DodsURL[] aDirectoryURLs = getSubSelectedURLs_Recursive(sDirTitle, sBaseURL, nodeRoot, sCE);
			}
		}
		DodsURL[] aurlCumulative = new DodsURL[ctDataURLs + ctDirectoryURLs];
		int xDataURL = -1;
		for( int xURL = 0; xURL < aurlSelected.length; xURL++ ){
			if( aurlSelected[xURL].getType() == DodsURL.TYPE_Data  || aurlSelected[xURL].getType() == DodsURL.TYPE_Image ){
				xDataURL++;
				aurlCumulative[xDataURL] = aurlSelected[xURL];
			}
		}
		int xDirectoryURL = -1;
		for( int xURL = 0; xURL < aurlSelected.length; xURL++ ){
			if( aurlSelected[xURL].getType() == DodsURL.TYPE_Directory ){
				Model_DirectoryTree tree = aurlSelected[xURL].getDirectoryTree();
                if( tree == null ) continue;
				DirectoryTreeNode nodeRoot = (DirectoryTreeNode)tree.getRoot();
				String sDirTitle = aurlSelected[xURL].getTitle();
				String sBaseURL = aurlSelected[xURL].getBaseURL();
				String sCE = aurlSelected[xURL].getConstraintExpression_Encoded();
				DodsURL[] aDirectoryURLs = getSubSelectedURLs_Recursive(sDirTitle, sBaseURL, nodeRoot, sCE);
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
	private DodsURL[] getSubSelectedURLs_Recursive( String sDirTitle, String sBaseURL, DirectoryTreeNode node, String sCE){
		if( node == null ) return null;
		int ctSelectedFiles = 0;
		if( node.isSelected() ) ctSelectedFiles += node.getFileList_SelectedCount();
		DodsURL[] aURLselected = null;
		if( ctSelectedFiles > 0 ){
			aURLselected = new DodsURL[ctSelectedFiles];
			String[] asSelectedFiles = node.getFileList_Selected(); // one-based
			String[] asSelectedHREFs = node.getHREFList_Selected(); // one-based
			for( int xFile = 1; xFile <= ctSelectedFiles; xFile++ ){
				String sDirectory = Utility.sConnectPaths(sBaseURL, "/", node.getPathString());
				String sURL = asSelectedHREFs[xFile]; // if it is an image we allow for an out-of-path reference
				if( !sURL.toUpperCase().startsWith("HTTP://") ){ // if URL is relative or bad then construct it
					sURL = Utility.sConnectPaths(sDirectory, "/", asSelectedFiles[xFile]);
				}
				if( Utility.isImage( sURL ) ){
					aURLselected[xFile-1] = new DodsURL(sURL, DodsURL.TYPE_Image);
					aURLselected[xFile-1].setTitle(asSelectedFiles[xFile]);
				} else {
					aURLselected[xFile-1] = new DodsURL(sURL, DodsURL.TYPE_Data);
					aURLselected[xFile-1].setTitle(sDirTitle + " " + asSelectedFiles[xFile] + " " + sCE);
					aURLselected[xFile-1].setConstraintExpression(sCE);
				}
			}
		}
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			DirectoryTreeNode nodeChild = node.getChild(xChild);
			DodsURL[] aURLsubselected = getSubSelectedURLs_Recursive(sDirTitle, sBaseURL, nodeChild, sCE);
			if( aURLsubselected != null ){
				if( aURLselected == null ){
					aURLselected = aURLsubselected;
				} else {
					int ctTotalURLs = aURLselected.length + aURLsubselected.length;
					DodsURL[] aurlBuffer = new DodsURL[ctTotalURLs];
					System.arraycopy(aURLselected, 0, aurlBuffer, 0, aURLselected.length);
					System.arraycopy(aURLsubselected, 0, aurlBuffer, aURLselected.length, aURLsubselected.length);
					aURLselected = aurlBuffer;
				}
			}
		}
		return aURLselected;
	}

}