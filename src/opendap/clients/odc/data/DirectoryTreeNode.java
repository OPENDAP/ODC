package opendap.clients.odc.data;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class DirectoryTreeNode extends DefaultMutableTreeNode {
	DirectoryTreeNode(){
		super();
	}
	DirectoryTreeNode( String sDirectoryName ){
		super( sDirectoryName );
		msDirectoryName = sDirectoryName;
	}
	private String msDirectoryName;
	private String msError;
	private String[] masHREFList;
	private String[] masFileList; // one-based
	private String[] masFileDescriptions; // one-based
	private Model_Dataset[] maURLs; // one-based
	private int[] maiSelectedFileIndices; // zero-based
	private boolean mzSelected = false;
	private boolean mzTerminal = false; // == terminal node / used instead of isLeaf because isLeaf controls default icon
	private boolean mzDiscovered = false;
	public String toString(){
		return this.getTitle();
	}
	public String getPathString(){
		TreeNode[] aNodes = this.getPath();
		StringBuffer sbPath = new StringBuffer(80);
		for( int xNode = 1; xNode < aNodes.length; xNode++ ){ // skip the root
			DirectoryTreeNode nodeCurrent = (DirectoryTreeNode)aNodes[xNode];
			sbPath.append(nodeCurrent.getName());
			sbPath.append("/");
		}
		return sbPath.toString();
	}
	public String[] getHREFList(){ return masHREFList; }
	public void setHREFList( String[] asHREFList ){ masHREFList = asHREFList; }
	public String[] getFileList(){
		if( masFileList == null ) return new String[1];
		return masFileList;
	}
	public String[] getFileList_Selected(){
		int ctSelected = maiSelectedFileIndices.length;
		if( ctSelected == 0 ) return new String[1];
		String[] masSelectedFiles = new String[ctSelected + 1];
		for( int xSelected = 1; xSelected <= maiSelectedFileIndices.length; xSelected++ ){
			masSelectedFiles[xSelected] = masFileList[maiSelectedFileIndices[xSelected-1]];
		}
		return masSelectedFiles;
	}
	public String[] getHREFList_Selected(){
		int ctSelected = maiSelectedFileIndices.length;
		if( ctSelected == 0 ) return new String[1];
		String[] masSelectedHREFs = new String[ctSelected + 1];
		for( int xSelected = 1; xSelected <= maiSelectedFileIndices.length; xSelected++ ){
			masSelectedHREFs[xSelected] = masHREFList[maiSelectedFileIndices[xSelected-1]];
		}
		return masSelectedHREFs;
	}
	public int getFileList_SelectedCount(){
		if( maiSelectedFileIndices == null ) return 0;
		return maiSelectedFileIndices.length;
	}
	public int getFileList_Count(){
		if( masFileList == null ) return 0;
		return masFileList.length - 1;
	}
	public int[] getFileList_SelectedIndices(){
		return maiSelectedFileIndices;
	}
	public String getFileDescription( int xFile_1 ){
		if( masFileDescriptions == null ){
			return null;
		} else if( xFile_1 < 1 || xFile_1 > masFileDescriptions.length - 1 ){
			return "[bad file index " + xFile_1 + " list size " + (masFileDescriptions.length - 1) + "]";
		} else {
			return masFileDescriptions[xFile_1];
		}
	}
	public void setName( String sName ){ msDirectoryName = sName; }
	public String getName(){ return msDirectoryName; }
	public String getTitle(){ return msDirectoryName + (this.isDiscovered() ? "" : " ...") + (this.getError() == null ? "" : "[Error]"); }
	public void setError( String sError ){ msError = sError; }
	public String getError(){ return msError; }
	public boolean zHasError(){ return msError != null; }
	public void setFileList( String[] asFileList ){
		masFileList = asFileList;
		maiSelectedFileIndices = new int[0];
		maURLs = new Model_Dataset[asFileList.length];
	}
	public void setFileDescriptions( String[] asFileDescriptions ){
		masFileDescriptions = asFileDescriptions;
	}
	public int getFileCount(){
		if( masFileList == null ) return 0;
		return masFileList.length - 1;
	}
	public boolean isDiscovered(){ return mzDiscovered; }
	public boolean isSelected(){ return mzSelected; }
	public boolean isTerminal(){ return mzTerminal; }
	public void setTerminal( boolean zTerminal ){ mzTerminal = zTerminal; }

	public DirectoryTreeNode getChild( int iIndex ){
		return (DirectoryTreeNode)super.getChildAt(iIndex);
	}

	// returns an array of selected children, zero-based
	// if no children are selected returns null
	public DirectoryTreeNode[] getSelectedChildren(){
		int ctSelectedChildren = 0;
		for( int xChild = 0; xChild < this.getChildCount(); xChild++ ){
			DirectoryTreeNode nodeChild = (DirectoryTreeNode)this.getChildAt(xChild);
			if( nodeChild.isSelected() ) ctSelectedChildren++;
		}
		if( ctSelectedChildren == 0 ) return null;
		DirectoryTreeNode[] anodeSelected = new DirectoryTreeNode[ctSelectedChildren];
		int xSelectedChild = -1;
		for( int xChild = 0; xChild < this.getChildCount(); xChild++ ){
			DirectoryTreeNode nodeChild = (DirectoryTreeNode)this.getChildAt(xChild);
			if( nodeChild.isSelected() ){
				xSelectedChild++;
				anodeSelected[xSelectedChild] = nodeChild;
			}
		}
		return anodeSelected;
	}
	public void setDiscovered( boolean zIsDiscovered ){ mzDiscovered = zIsDiscovered; }
	public void setSelected( boolean zIsSelected ){ mzSelected = zIsSelected; }
	public void setSelectedFile( int xFile, boolean zIsSelected ){
		int[] buffer = new int[maiSelectedFileIndices.length + 1];
		if( maiSelectedFileIndices.length > 0 ){
			System.arraycopy(maiSelectedFileIndices, 0, buffer, 0, maiSelectedFileIndices.length);
		}
		maiSelectedFileIndices = buffer;
		maiSelectedFileIndices[maiSelectedFileIndices.length-1] = xFile;
	}
	public void setSelectedFileIndices( int[] aiSelected ){
		maiSelectedFileIndices = aiSelected;
	}
	public void setFileURL( int xFile, Model_Dataset url ){
		maURLs[xFile] = url;
	}
	public Model_Dataset getFileURL( int xFile ){
		if( xFile < 1 || xFile >= masFileList.length ) return null;
		return maURLs[xFile];
	}
	public String getFileName( int xFile ){
		if( xFile < 1 || xFile >= masFileList.length ) return "[error " + xFile + "]";
		return masFileList[xFile];
	}
	public Model_Dataset getFirstFileURL( int xFile ){
		for( int xURL = 1; xURL < maURLs.length; xURL++ ){
			if( maURLs[xURL] != null ) return maURLs[xURL];
		}
		return null;
	}
	public boolean zFileSelected( int xFile ){
		if( maiSelectedFileIndices == null ) return false;
		for( int xSelectedFile = 0; xSelectedFile < maiSelectedFileIndices.length; xSelectedFile++ ){
			if( maiSelectedFileIndices[xSelectedFile] == xFile ) return true;
		}
		return false;
	}
}
