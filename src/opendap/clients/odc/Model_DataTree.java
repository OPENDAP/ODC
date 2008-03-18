package opendap.clients.odc;

/**
 * Title:        Model_DataTree
 * Description:  Used to define data trees
 * @author       John Chamberlain
 * @version      2.0
 */

/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2008 OPeNDAP, Inc.
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

public class Model_DataTree extends DefaultTreeModel {
	public static final char SEPARATOR = '.';
	public Model_DataTree( DataTreeNode nodeRoot ){
		super( nodeRoot );
	}
	private DataTreeNode mnodeSelected = null;
	DataTreeNode getRootNode(){ return (DataTreeNode)this.getRoot(); }
	void setSelectedNode( DataTreeNode nodeSelected ){
		mnodeSelected = nodeSelected;
	}
	DataTreeNode getSelectedNode(){ return mnodeSelected; }
	String getPathForNode( DataTreeNode node ){
		if( node == null ) return "";
		TreeNode[] aNodes = node.getPath();
		StringBuffer sbPath = new StringBuffer(80);
		for( int xNode = 1; xNode < aNodes.length; xNode++ ){ // skip the root
			DataTreeNode nodeCurrent = (DataTreeNode)aNodes[xNode];
			sbPath.append(nodeCurrent.getName());
			sbPath.append( SEPARATOR );
		}
		return sbPath.toString();
	}
	String getPrintout(){
		DataTreeNode nodeRoot = (DataTreeNode)this.getRoot();
		return getPrintoutForNode( nodeRoot, 0 );
	}
	private String getPrintoutForNode(DataTreeNode node, int iIndent){
		if( node == null ) return Utility.sRepeatChar('\t', iIndent) + "[null]\n";
		StringBuffer sbOut = new StringBuffer();
		sbOut.append(Utility.sRepeatChar('\t', iIndent));
		String sNodeTitle = node.getTitle();
	    if( sNodeTitle == null ) sNodeTitle = "[unnamed node]";
		sbOut.append(sNodeTitle + (node.isSelected() ? " *" : "") + '\n');
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			DataTreeNode nodeChild = (DataTreeNode)node.getChildAt(xChild);
			sbOut.append(getPrintoutForNode(nodeChild, iIndent+1));
		}
		return sbOut.toString();
	}
	void vClearNodeSelection(){
		DataTreeNode nodeRoot = (DataTreeNode)this.getRoot();
		vClearNodeSelection_ForNode( nodeRoot );
	}
	private void vClearNodeSelection_ForNode( DataTreeNode node ){
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			DataTreeNode nodeChild = (DataTreeNode)node.getChildAt(xChild);
			nodeChild.setSelected(false);
		}
	}
}

class DataTreeNode extends DefaultMutableTreeNode {
	DataTreeNode(){
		super();
	}
	DataTreeNode( opendap.dap.BaseType bt ){
		super();
		setBaseType( bt );
	}
	private String msStructureName;
	private opendap.dap.BaseType mBaseType;
	private boolean mzSelected = false;
	private boolean mzTerminal = false; // == terminal node / used instead of isLeaf because isLeaf controls default icon
	
	String getPathString(){
		TreeNode[] aNodes = this.getPath();
		StringBuffer sbPath = new StringBuffer(80);
		for( int xNode = 1; xNode < aNodes.length; xNode++ ){ // skip the root
			DataTreeNode nodeCurrent = (DataTreeNode)aNodes[xNode];
			sbPath.append(nodeCurrent.getName());
			sbPath.append("/");
		}
		return sbPath.toString();
	}
	opendap.dap.BaseType getBaseType(){ return mBaseType; }
	void setBaseType( opendap.dap.BaseType bt ){
		mBaseType = bt;
		msStructureName = bt.getName();
	}
	void setName( String sName ){ msStructureName = sName; }
	String getName(){ return msStructureName; }
	String getTitle(){ return msStructureName; }
	public boolean isSelected(){ return mzSelected; }
	public boolean isTerminal(){ return mzTerminal; }
	public void setTerminal( boolean zTerminal ){ mzTerminal = zTerminal; }

	public DataTreeNode getChild( int iIndex ){
		return (DataTreeNode)super.getChildAt(iIndex);
	}

	// returns an array of selected children, zero-based
	// if no children are selected returns null
	public DataTreeNode[] getSelectedChildren(){
		int ctSelectedChildren = 0;
		for( int xChild = 0; xChild < this.getChildCount(); xChild++ ){
			DataTreeNode nodeChild = (DataTreeNode)this.getChildAt(xChild);
			if( nodeChild.isSelected() ) ctSelectedChildren++;
		}
		if( ctSelectedChildren == 0 ) return null;
		DataTreeNode[] anodeSelected = new DataTreeNode[ctSelectedChildren];
		int xSelectedChild = -1;
		for( int xChild = 0; xChild < this.getChildCount(); xChild++ ){
			DataTreeNode nodeChild = (DataTreeNode)this.getChildAt(xChild);
			if( nodeChild.isSelected() ){
				xSelectedChild++;
				anodeSelected[xSelectedChild] = nodeChild;
			}
		}
		return anodeSelected;
	}
	public void setSelected( boolean zIsSelected ){ mzSelected = zIsSelected; }
	public String toString(){ return this.getTitle(); }
}

