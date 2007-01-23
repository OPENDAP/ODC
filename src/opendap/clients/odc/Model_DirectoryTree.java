package opendap.clients.odc;

/**
 * Title:        Model_DirectoryTree
 * Description:  Used to define directory trees
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.0
 */

import javax.swing.tree.*;

public class Model_DirectoryTree extends DefaultTreeModel {
	public Model_DirectoryTree( DirectoryTreeNode nodeRoot ){
		super( nodeRoot );
	}
	private String msFirstFile;
	private DirectoryTreeNode mnodeSelected = null;
	DirectoryTreeNode getRootNode(){ return (DirectoryTreeNode)this.getRoot(); }
	String getFirstFile(){
		DirectoryTreeNode nodeRoot = getRootNode();
		if( nodeRoot == null ) return null;
		return getFirstFile_ForNode( nodeRoot );
	}
	void setSelectedNode( DirectoryTreeNode nodeSelected ){
		mnodeSelected = nodeSelected;
	}
	DirectoryTreeNode getSelectedNode(){ return mnodeSelected; }
	boolean zHasErrors(){
		return zHasErrors_ForNode(getRootNode());
	}
	private boolean zHasErrors_ForNode( DirectoryTreeNode node ){
		if( node == null ) return false;
		if( node.getError() != null ) return true;
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			DirectoryTreeNode nodeChild = (DirectoryTreeNode)node.getChildAt(xChild);
			if( zHasErrors_ForNode(nodeChild) ) return true;
		}
		return false;
	}
	private String getFirstFile_ForNode( DirectoryTreeNode node ){
		if( node == null ) return null;
		if( node.getFileCount() > 0 ){
			return node.getFileList()[1];
		} else {
			for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
				DirectoryTreeNode nodeChild = (DirectoryTreeNode)node.getChildAt(xChild);
				String sFirstChildFile = getFirstFile_ForNode( nodeChild );
				if( sFirstChildFile == null ) continue;
				return nodeChild.getName() + "/" + sFirstChildFile;
			}
			return null; // no files found in this branch of tree
		}
	}
	String getPathForNode( DirectoryTreeNode node ){
		if( node == null ) return "";
		TreeNode[] aNodes = node.getPath();
		StringBuffer sbPath = new StringBuffer(80);
		for( int xNode = 1; xNode < aNodes.length; xNode++ ){ // skip the root
			DirectoryTreeNode nodeCurrent = (DirectoryTreeNode)aNodes[xNode];
			sbPath.append(nodeCurrent.getName());
			sbPath.append("/");
		}
		return sbPath.toString();
	}
	String getPrintout(){
		DirectoryTreeNode nodeRoot = (DirectoryTreeNode)this.getRoot();
		return getPrintoutForNode( nodeRoot, 0 );
	}
	private String getPrintoutForNode(DirectoryTreeNode node, int iIndent){
		if( node == null ) return Utility.sRepeatChar('\t', iIndent) + "[null]\n";
		StringBuffer sbOut = new StringBuffer();
		sbOut.append(Utility.sRepeatChar('\t', iIndent));
		String sNodeTitle = node.getTitle();
	    if( sNodeTitle == null ) sNodeTitle = "[unnamed node]";
		if( node.getError() == null ){
			sbOut.append(sNodeTitle + (node.isSelected() ? " *" : "") + '\n');
		} else {
			sbOut.append(sNodeTitle + (node.isSelected() ? " *" : "") + " [Error: " + node.getError() + "]\n");
		}
		String[] asFiles = node.getFileList();
		if( asFiles.length > 1 ){
			for( int xFile = 0; xFile < asFiles.length; xFile++ ){
				sbOut.append(Utility.sRepeatChar('\t', iIndent) + "- " + asFiles[xFile] + (node.zFileSelected(xFile) ? " *\n" : "\n"));
			}
		}
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			DirectoryTreeNode nodeChild = (DirectoryTreeNode)node.getChildAt(xChild);
			sbOut.append(getPrintoutForNode(nodeChild, iIndent+1));
		}
		return sbOut.toString();
	}
	String[] getDirectoryFiles( String sPath, StringBuffer sbError ){
		DirectoryTreeNode nodeRoot = (DirectoryTreeNode)this.getRoot();
		if( sPath == null ){ return nodeRoot.getFileList(); }
		if( sPath.length()==0 ){ return nodeRoot.getFileList(); }
		if( sPath.equals("/") ){ return nodeRoot.getFileList(); }
		// 1.4.1 only: String[] asDirs = sPath.split("/"); replaced by:
		String[] asDirs = Utility.split(sPath, '/');
		int xDir = 0;
		DirectoryTreeNode nodeCurrent = nodeRoot;
		String sPathCurrent = "";
		while(true){
			if( xDir == asDirs.length )	return nodeCurrent.getFileList();
			int ctChild = nodeCurrent.getChildCount();
			if( ctChild == 0 ){
				sbError.append("path not found: " + sPathCurrent);
				return null;
			}
			xDir++;
			int xChild = 0;
			while(true){
				if( xChild == ctChild ){
					sbError.append("directory " + asDirs[xDir] + " not found in path: " + sPathCurrent);
				}
				DirectoryTreeNode nodeCurrentChild = (DirectoryTreeNode)nodeCurrent.getChildAt(xChild);
				if( nodeCurrentChild == null ) continue;
				String sDirName = nodeCurrentChild.getName();
				if( sDirName == null ) continue; // todo nulls not supported
				if( sDirName.equalsIgnoreCase(asDirs[xDir]) ){
					nodeCurrent = nodeCurrentChild;
					sPathCurrent += asDirs[xDir] + "/";
					break;
				}
				xChild++;
			}
		}
	}
	void vClearNodeSelection(){
		DirectoryTreeNode nodeRoot = (DirectoryTreeNode)this.getRoot();
		vClearNodeSelection_ForNode( nodeRoot );
	}
	private void vClearNodeSelection_ForNode( DirectoryTreeNode node ){
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			DirectoryTreeNode nodeChild = (DirectoryTreeNode)node.getChildAt(xChild);
			nodeChild.setSelected(false);
		}
	}
}

class DirectoryTreeNode extends DefaultMutableTreeNode {
	DirectoryTreeNode(){
		super();
	}
	DirectoryTreeNode( String sDirectoryName ){
		super();
		msDirectoryName = sDirectoryName;
	}
	private String msDirectoryName;
	private String msError;
	private String[] masHREFList;
	private String[] masFileList; // one-based
	private String[] masFileDescriptions; // one-based
	private DodsURL[] maURLs; // one-based
	private int[] maiSelectedFileIndices; // zero-based
	private boolean mzSelected = false;
	private boolean mzTerminal = false; // == terminal node / used instead of isLeaf because isLeaf controls default icon
	private boolean mzDiscovered = false;
	String getPathString(){
		TreeNode[] aNodes = this.getPath();
		StringBuffer sbPath = new StringBuffer(80);
		for( int xNode = 1; xNode < aNodes.length; xNode++ ){ // skip the root
			DirectoryTreeNode nodeCurrent = (DirectoryTreeNode)aNodes[xNode];
			sbPath.append(nodeCurrent.getName());
			sbPath.append("/");
		}
		return sbPath.toString();
	}
	String[] getHREFList(){ return masHREFList; }
	void setHREFList( String[] asHREFList ){ masHREFList = asHREFList; }
	String[] getFileList(){
		if( masFileList == null ) return new String[1];
		return masFileList;
	}
	String[] getFileList_Selected(){
		int ctSelected = maiSelectedFileIndices.length;
		if( ctSelected == 0 ) return new String[1];
		String[] masSelectedFiles = new String[ctSelected + 1];
		for( int xSelected = 1; xSelected <= maiSelectedFileIndices.length; xSelected++ ){
			masSelectedFiles[xSelected] = masFileList[maiSelectedFileIndices[xSelected-1]];
		}
		return masSelectedFiles;
	}
	String[] getHREFList_Selected(){
		int ctSelected = maiSelectedFileIndices.length;
		if( ctSelected == 0 ) return new String[1];
		String[] masSelectedHREFs = new String[ctSelected + 1];
		for( int xSelected = 1; xSelected <= maiSelectedFileIndices.length; xSelected++ ){
			masSelectedHREFs[xSelected] = masHREFList[maiSelectedFileIndices[xSelected-1]];
		}
		return masSelectedHREFs;
	}
	int getFileList_SelectedCount(){
		if( maiSelectedFileIndices == null ) return 0;
		return maiSelectedFileIndices.length;
	}
	int getFileList_Count(){
		if( masFileList == null ) return 0;
		return masFileList.length - 1;
	}
	int[] getFileList_SelectedIndices(){
		return maiSelectedFileIndices;
	}
	String getFileDescription( int xFile_1 ){
		if( masFileDescriptions == null ){
			return null;
		} else if( xFile_1 < 1 || xFile_1 > masFileDescriptions.length - 1 ){
			return "[bad file index " + xFile_1 + " list size " + (masFileDescriptions.length - 1) + "]";
		} else {
			return masFileDescriptions[xFile_1];
		}
	}
	void setName( String sName ){ msDirectoryName = sName; }
	String getName(){ return msDirectoryName; }
	String getTitle(){ return msDirectoryName + (this.isDiscovered() ? "" : " ...") + (this.getError() == null ? "" : "[Error]"); }
	void setError( String sError ){ msError = sError; }
	String getError(){ return msError; }
	boolean zHasError(){ return msError != null; }
	void setFileList( String[] asFileList ){
		masFileList = asFileList;
		maiSelectedFileIndices = new int[0];
		maURLs = new DodsURL[asFileList.length];
	}
	void setFileDescriptions( String[] asFileDescriptions ){
		masFileDescriptions = asFileDescriptions;
	}
	int getFileCount(){
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
	public void setFileURL( int xFile, DodsURL url ){
		maURLs[xFile] = url;
	}
	public DodsURL getFileURL( int xFile ){
		if( xFile < 1 || xFile >= masFileList.length ) return null;
		return maURLs[xFile];
	}
	public String getFileName( int xFile ){
		if( xFile < 1 || xFile >= masFileList.length ) return "[error " + xFile + "]";
		return masFileList[xFile];
	}
	public DodsURL getFirstFileURL( int xFile ){
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
	public String toString(){ return this.getTitle(); }
}

