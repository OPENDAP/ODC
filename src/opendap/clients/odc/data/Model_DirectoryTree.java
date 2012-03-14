package opendap.clients.odc.data;

/**
 * Title:        Model_DirectoryTree
 * Description:  Used to define directory trees
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.0
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

import javax.swing.tree.*;

import opendap.clients.odc.Utility_String;

public class Model_DirectoryTree extends DefaultTreeModel {
	public Model_DirectoryTree( DirectoryTreeNode nodeRoot ){
		super( nodeRoot );
	}
	public static Model_DirectoryTree create( String sURL ){
		DirectoryTreeNode root = new DirectoryTreeNode( sURL );
		Model_DirectoryTree tree = new Model_DirectoryTree( root );
		return tree;
	}
	private DirectoryTreeNode mnodeSelected = null;
	public DirectoryTreeNode getRootNode(){ return (DirectoryTreeNode)this.getRoot(); }
	public String getFirstFile(){
		DirectoryTreeNode nodeRoot = getRootNode();
		if( nodeRoot == null ) return null;
		return getFirstFile_ForNode( nodeRoot );
	}
	public void setSelectedNode( DirectoryTreeNode nodeSelected ){
		mnodeSelected = nodeSelected;
	}
	public DirectoryTreeNode getSelectedNode(){ return mnodeSelected; }
	public boolean zHasErrors(){
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
	public String getPathForNode( DirectoryTreeNode node ){
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
	public String getPrintout(){
		DirectoryTreeNode nodeRoot = (DirectoryTreeNode)this.getRoot();
		return getPrintoutForNode( nodeRoot, 0 );
	}
	private String getPrintoutForNode(DirectoryTreeNode node, int iIndent){
		if( node == null ) return Utility_String.sRepeatChar('\t', iIndent) + "[null]\n";
		StringBuffer sbOut = new StringBuffer();
		sbOut.append( Utility_String.sRepeatChar('\t', iIndent));
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
				sbOut.append( Utility_String.sRepeatChar('\t', iIndent) + "- " + asFiles[xFile] + (node.zFileSelected(xFile) ? " *\n" : "\n"));
			}
		}
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			DirectoryTreeNode nodeChild = (DirectoryTreeNode)node.getChildAt(xChild);
			sbOut.append(getPrintoutForNode(nodeChild, iIndent+1));
		}
		return sbOut.toString();
	}
	public String[] getDirectoryFiles( String sPath, StringBuffer sbError ){
		DirectoryTreeNode nodeRoot = (DirectoryTreeNode)this.getRoot();
		if( sPath == null ){ return nodeRoot.getFileList(); }
		if( sPath.length()==0 ){ return nodeRoot.getFileList(); }
		if( sPath.equals("/") ){ return nodeRoot.getFileList(); }
		// 1.4.1 only: String[] asDirs = sPath.split("/"); replaced by:
		String[] asDirs = Utility_String.split( sPath, '/' );
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
					return null;
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
	public void vClearNodeSelection(){
		DirectoryTreeNode nodeRoot = (DirectoryTreeNode)this.getRoot();
		vClearNodeSelection_ForNode( nodeRoot );
	}
	public void vClearNodeSelection_ForNode( DirectoryTreeNode node ){
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			DirectoryTreeNode nodeChild = (DirectoryTreeNode)node.getChildAt(xChild);
			nodeChild.setSelected(false);
		}
	}
	
}


