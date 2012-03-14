package opendap.clients.odc.data;

import java.util.ArrayList;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.DAP.DAP_TYPE;
import opendap.clients.odc.DAP.DAP_VARIABLE;
import opendap.dap.AttributeTable;
import opendap.dap.BaseType;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.DByte;
import opendap.dap.DDS;
import opendap.dap.DataDDS;
import opendap.dap.DFloat32;
import opendap.dap.DFloat64;
import opendap.dap.DGrid;
import opendap.dap.DInt16;
import opendap.dap.DInt32;
import opendap.dap.DSequence;
import opendap.dap.DString;
import opendap.dap.DStructure;
import opendap.dap.DUInt16;
import opendap.dap.DUInt32;
import opendap.dap.PrimitiveVector;

import  org.python.core.PyObject;

public abstract class Node extends DefaultMutableTreeNode {   // functions as both the root node and a structure node
	private Model_Dataset_Local modelParent;
	private opendap.dap.BaseType mBaseType; // null only in the case of the root node
	private boolean mzSelected = false;
	private boolean mzTerminal = false; // == terminal node / used instead of isLeaf because isLeaf controls default icon
	public DAP_VARIABLE eVariableType = null;
	private String msName = "[undefined]";
	AttributeTable attributes;
	Node nodeParent;
	ArrayList<Node> subnodes;
	protected Node( opendap.dap.BaseType bt ){
		super();
		setBaseType( bt );
		subnodes = new ArrayList<Node>();
	}
	void setModel( Model_Dataset_Local parent ){
		modelParent = parent;
	}
	public opendap.dap.BaseType getBaseType(){ return mBaseType; }
	abstract public DAP_VARIABLE getType();
	void setBaseType( opendap.dap.BaseType bt ){
		mBaseType = bt;
		if( bt != null ) msName = bt.getClearName();
	}
	public String getName(){ return msName; }
	public String getTitle(){ return getName(); }
	public boolean isSelected(){ return mzSelected; }
	public boolean isTerminal(){ return mzTerminal; }
	public void setTerminal( boolean zTerminal ){ mzTerminal = zTerminal; }
	public TreePath getTreePath(){
		TreeNode[] aPathNodes = getPath();
		TreePath path = new TreePath( aPathNodes );
		return path;
	}
	public String getPathString(){
		TreeNode[] aNodes = this.getPath();
		StringBuffer sbPath = new StringBuffer(80);
		for( int xNode = 1; xNode < aNodes.length; xNode++ ){ // skip the root
			Node nodeCurrent = (Node)aNodes[xNode];
			sbPath.append(nodeCurrent.getName());
			sbPath.append("/");
		}
		return sbPath.toString();
	}

	public Node getChild( int iIndex ){
		return (Node)super.getChildAt(iIndex);
	}

	public boolean _setName( String sNewName, StringBuffer sbError ){ // sets name normally (will be encoded)
		if( isRoot() ){
			msName = sNewName;
			if( ! this.modelParent.setName( sNewName, sbError ) ){
				sbError.insert( 0, "failed to set name to \"" + sNewName + "\"" );
				return false;
			}
		} else {
			if( mBaseType == null ){
				sbError.append( "node has no corresponding base type" );
				return false;
			}
			mBaseType.setName( sNewName );
		}
		modelParent.nodeChanged( this );
		return true;
	}

	public boolean _setNameEncoded( String sNewName, StringBuffer sbError ){ // sets name with no encoding (should be encoded)
		// TODO validation
		mBaseType.setClearName( sNewName );
		return true;
	}
	
	abstract public boolean _setSelectedValue( String sNewValue, StringBuffer sbError );
	
	// returns an array of selected children, zero-based
	// if no children are selected returns null
	public Node[] getSelectedChildren(){
		int ctSelectedChildren = 0;
		for( int xChild = 0; xChild < this.getChildCount(); xChild++ ){
			Node nodeChild = (Node)this.getChildAt(xChild);
			if( nodeChild.isSelected() ) ctSelectedChildren++;
		}
		if( ctSelectedChildren == 0 ) return null;
		Node[] anodeSelected = new Node[ctSelectedChildren];
		int xSelectedChild = -1;
		for( int xChild = 0; xChild < this.getChildCount(); xChild++ ){
			Node nodeChild = (Node)this.getChildAt(xChild);
			if( nodeChild.isSelected() ){
				xSelectedChild++;
				anodeSelected[xSelectedChild] = nodeChild;
			}
		}
		return anodeSelected;
	}
	public void setSelected( boolean zIsSelected ){ mzSelected = zIsSelected; }
	public String toString(){ return getTitle(); }
		
	public static Node createRoot( DDS dds, StringBuffer sbError ){
		Node nodeRoot = new Node_Structure( null ); // only the root node has a null basetype
		java.util.Enumeration variable_list = dds.getVariables();
		int xVariable = 0;
		while( variable_list.hasMoreElements() ){
			xVariable++;
			Object oVariable = variable_list.nextElement();
			if( ! (oVariable instanceof BaseType) ){
				sbError.append( "variable " + xVariable + " " + oVariable + " is not a BaseType" );
				return null;
			}
			BaseType bt = (BaseType)oVariable;
			Node node = Node.create( nodeRoot, bt, sbError ); 
			if( node == null  ){
				sbError.insert( 0, "error loading root variable " + xVariable + " " + bt.getClearName() + ": " );
				return null;
			}
			nodeRoot.subnodes.add( node );
		}
		nodeRoot.msName = dds.getClearName();
		return nodeRoot;
	}
	public static Node create( Node parent_node, BaseType bt, StringBuffer sbError ){
		if( parent_node.modelParent == null ){
			sbError.append( "model is missing from parent" );
			return null;
		}	
		if( bt == null ){
			sbError.append( "no base type supplied (root nodes can only be created by the _createRoot method)" );
			return null;
		}
		DAP_VARIABLE variable_type = DAP.getVariableType( bt, sbError );
		if( variable_type == null ) return null; // an error occurred
		Node new_node = null;
		switch( variable_type ){
			case Structure:
				new_node = new Node_Structure( bt ); break;
			case Grid:
				new_node = new Node_Grid( (DGrid)bt ); break;
			case Sequence:
				new_node = new Node_Sequence( (DSequence)bt ); break;
			case Array:
				new_node = new Node_Array( (DArray)bt ); break;
			case Byte:
				new_node = new Node_Byte( (DByte)bt ); break;
			case Int16:
				new_node = new Node_Int16( (DInt16)bt ); break;
			case Int32:
				new_node = new Node_Int32( (DInt32)bt ); break;
			case UInt16:
				new_node = new Node_UInt16( (DUInt16)bt ); break;
			case UInt32:
				new_node = new Node_UInt32( (DUInt32)bt ); break;
			case Float32:
				new_node = new Node_Float32( (DFloat32)bt ); break;
			case Float64:
				new_node = new Node_Float64( (DFloat64)bt ); break;
			case String:
				new_node = new Node_String( (DString)bt ); break;
		}
		new_node.modelParent = parent_node.modelParent;
		new_node.setBaseType( bt );
		new_node.eVariableType = variable_type;
		new_node.nodeParent = parent_node;
		new_node.attributes = bt.getAttributeTable();
		switch( variable_type ){
			case Structure:
				DStructure structure = (DStructure)bt;
				java.util.Enumeration member_list = structure.getVariables();
				int xMember = 0;
				while( member_list.hasMoreElements() ){
					xMember++;
					Object oMember = member_list.nextElement();
					if( ! (oMember instanceof BaseType) ){
						sbError.append( "member " + member_list + " " + oMember + " is not a BaseType" );
						return null;
					}
					BaseType btMember = (BaseType)oMember;
					Node nodeMember = Node.create( new_node, btMember, sbError );
					if( nodeMember == null  ){
						sbError.insert( 0, "error loading DStructure " + new_node.getName() + ": " );
						return null;
					}
					new_node.subnodes.add( nodeMember );
				}
				break;
			case Grid:
				DGrid grid = (DGrid)bt;
				Node_Grid nodeGrid = (Node_Grid)new_node;
				try {
					DArray darrayValues = (DArray)grid.getVar( 0 );
					Node_Array nodeArray = (Node_Array)Node.create( nodeGrid, darrayValues, sbError );
					if( nodeArray == null  ){
						sbError.insert( 0, "error loading value array for DGrid " + new_node.getName() + ": " );
						return null;
					}
					nodeGrid.arrayValues = nodeArray;
				} catch( Throwable t ) {}
				int ctMaps = grid.elementCount( false ) - 1;
				for( int xMap = 1; xMap <= ctMaps; xMap++ ){
					try {
						DArray darrayMap = (DArray)grid.getVar( xMap );
						nodeGrid.darrayDimensionalVectors.add( darrayMap ); 
					} catch( Throwable t ) {
						sbError.append( "error extracting map array: " + t );
						return null;
					}
				}
				break;
			case Sequence:

				ArrayList<String> listFieldNames = new ArrayList<String>();
				ArrayList<DAP_VARIABLE> listFieldTypes = new ArrayList<DAP_VARIABLE>();
				ArrayList<ArrayList<Node>> listRows = new ArrayList<ArrayList<Node>>(); // this contains the data, if any 
				
				DSequence sequence = (DSequence)bt;
				Node_Sequence nodeSequence = (Node_Sequence)new_node;
				java.util.Enumeration field_templates = sequence.getVariables();
				int iRowCount = sequence.getRowCount();
				int xField = 0;
				while( field_templates.hasMoreElements() ){
					xField++;
					Object oFieldTemplate = field_templates.nextElement();
					if( ! (oFieldTemplate instanceof BaseType) ){
						sbError.append( "field template " + xField + " " + oFieldTemplate + " is not a BaseType" );
						return null;
					}
					BaseType btFieldTemplate = (BaseType)oFieldTemplate;
					DAP_VARIABLE typeField = DAP.getVariableType( btFieldTemplate, sbError );
					if( typeField == null ){
						sbError.insert( 0, "failed to get variable type for field template " + xField + " " + btFieldTemplate.getName() + ": " );
						return null;
					}
					listFieldNames.add( btFieldTemplate.getClearName() );
					listFieldTypes.add( typeField );
					if( iRowCount == 0 ){ // recursively load template
						Node nodeField = Node.create( nodeSequence, btFieldTemplate, sbError ); 
						if( nodeField == null ){
							sbError.insert( 0, "error loading DSequence field " + xField + " " + btFieldTemplate.getName() + ": " );
							return null;
						}
						nodeSequence.subnodes.add( nodeField );
					} else { // load data
						for( int xRow = 0; xRow < iRowCount; xRow++ ){
							ArrayList<Node> listFieldValues;
							if( listRows.size() <= xRow ){  // create a new row
								listFieldValues = new ArrayList<Node>();
								listRows.add( listFieldValues );
							} else {
								listFieldValues = listRows.get( xRow );
							}
							try {
								BaseType btFieldValue = sequence.getVariable( xRow, btFieldTemplate.getClearName() );
								Node nodeField = Node.create( nodeSequence, btFieldTemplate, sbError ); 
								if( nodeField == null ){
									sbError.insert( 0, "error loading rows for DSequence field " + xField + " " + btFieldTemplate.getName() + ": " );
									return null;
								}
								listFieldValues.add( nodeField );
							} catch( Throwable t ) {
								sbError.insert( 0, "error loading row " + xRow + " for DSequence field " + xField + " " + btFieldTemplate.getName() + ": " + t );
								return null;
							}
						}
					}
				}
				break;
			case Array:
				DArray array = (DArray)bt;
				Node_Array nodeArray = (Node_Array)new_node;
				java.util.Enumeration dimension_list = array.getDimensions();
				int xDimension = 0;
				while( dimension_list.hasMoreElements() ){
					xDimension++;
					DArrayDimension dimension = (DArrayDimension)dimension_list.nextElement();
					nodeArray.listDimensions.add( dimension );
				}
				break;
			case Byte:
				DByte dbyte = (DByte)bt;
				Node_Byte nodeByte = (Node_Byte)new_node;
				nodeByte.value = dbyte.getValue();
				break;
			case Int16:
				DInt16 dint16 = (DInt16)bt;
				Node_Int16 nodeInt16 = (Node_Int16)new_node;
				nodeInt16.value = dint16.getValue();
				break;
			case Int32:
				DInt32 dint32 = (DInt32)bt;
				Node_Int32 nodeInt32 = (Node_Int32)new_node;
				nodeInt32.value = dint32.getValue();
				break;
			case UInt16:
				DUInt16 duint16 = (DUInt16)bt;
				Node_UInt16 nodeUInt16 = (Node_UInt16)new_node;
				nodeUInt16.value = duint16.getValue() & 0xffff;
				break;
			case UInt32:
				DUInt32 duint32 = (DUInt32)bt;
				Node_UInt32 nodeUInt32 = (Node_UInt32)new_node;
				nodeUInt32.value = duint32.getValue() & 0xffffffffL; // convert to unsigned
				break;
			case Float32:
				DFloat32 dfloat32 = (DFloat32)bt;
				Node_Float32 nodeFloat32 = (Node_Float32)new_node;
				nodeFloat32.value = dfloat32.getValue();
				break;
			case Float64:
				DFloat64 dfloat64 = (DFloat64)bt;
				Node_Float64 nodeFloat64 = (Node_Float64)new_node;
				nodeFloat64.value = dfloat64.getValue();
				break;
			case String:
				DString dstring = (DString)bt;
				Node_String nodeString = (Node_String)new_node;
				nodeString.value = dstring.getValue();
				break;
		}
		return new_node;
	}
	String sGetSummaryText(){
		return getName();
	}
	
	static BaseType createDefaultBaseType( int iIndexNumber, StringBuffer sbError ){ // subclass must override
		sbError.append( "createDefaultBaseType not implemented for this data type" );
		return null;
	}
	
	public Node createDefaultMember( DAP_VARIABLE variable_type, StringBuffer sbError ){
		if( getType() != DAP_VARIABLE.Structure ){
			sbError.append( "internal error, attempt to add variable to a " + getType() + ", variables can only be added to structures" );
			return null;
		}
		
		// determine how many elements of this type already exist in the structure to determine index number
		int iIndexNumber = 1;
		for( Node node : subnodes ){
			if( node.getType() == variable_type ) iIndexNumber++; 
		}
		
		BaseType new_bt = null;
		switch( variable_type ){
			case Structure:
				new_bt = Node_Structure.createDefaultBaseType( iIndexNumber, sbError ); break;
			case Grid:
				new_bt = Node_Grid.createDefaultBaseType( iIndexNumber, sbError ); break;
			case Sequence:
				new_bt = Node_Sequence.createDefaultBaseType( iIndexNumber, sbError ); break;
			case Array:
				new_bt = Node_Array.createDefaultBaseType( iIndexNumber, sbError ); break;
			case Byte:
				new_bt = Node_Byte.createDefaultBaseType( iIndexNumber, sbError ); break;
			case Int16:
				new_bt = Node_Int16.createDefaultBaseType( iIndexNumber, sbError ); break;
			case Int32:
				new_bt = Node_Int32.createDefaultBaseType( iIndexNumber, sbError ); break;
			case UInt16:
				new_bt = Node_UInt16.createDefaultBaseType( iIndexNumber, sbError ); break;
			case UInt32:
				new_bt = Node_UInt32.createDefaultBaseType( iIndexNumber, sbError ); break;
			case Float32:
				new_bt = Node_Float32.createDefaultBaseType( iIndexNumber, sbError ); break;
			case Float64:
				new_bt = Node_Float64.createDefaultBaseType( iIndexNumber, sbError ); break;
			case String:
				new_bt = Node_String.createDefaultBaseType( iIndexNumber, sbError ); break;
			default:
				sbError.append( "unsupported variable type " + variable_type );
				return null;
		}
		if( new_bt == null ){
			sbError.insert( 0, "failed to create default base type: " );
			return null;
		}
		// create attribute table TODO
		Node new_node = Node.create( this, new_bt, sbError );
		if( new_node == null ){
			sbError.insert( 0, "failed to create new node: " );
			return null;
		}
		BaseType btParent = getBaseType();
		if( btParent == null ){ // adding to root
			DataDDS data_dds = this.modelParent.getSource().getData();
			data_dds.addVariable( new_bt );
		} else {
			DStructure structureParent = (DStructure)btParent;
			structureParent.addVariable( new_bt );
		}
		new_bt.setParent( btParent );
		subnodes.add( new_node );
		modelParent.insertNodeInto( new_node, this, this.getChildCount() ); // add new member at end
		return new_node;
	}
}

class Node_Structure extends Node {
	DStructure dstructure;
	protected Node_Structure( BaseType bt ){
		super( bt );
		dstructure = (DStructure)bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Structure; }
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "structures have no values" );
		return false;
	}
}

class Node_Grid extends Node {
	DGrid dgrid;
	Node_Array arrayValues;
	ArrayList<DArray> darrayDimensionalVectors = new ArrayList();
	protected Node_Grid( DGrid bt ){
		super( bt );
		dgrid = bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Grid; }
	String sGetSummaryText(){
		StringBuffer sb = new StringBuffer( 250 );
		sb.append( getName() );
		long nTotalSize = 1;
		for( int xDimension = 1; xDimension <= arrayValues.listDimensions.size(); xDimension++ ){
			DArrayDimension dim = arrayValues.listDimensions.get( xDimension - 1);
			int size = dim.getSize();
			sb.append( ' ' );
			sb.append( dim.getClearName() );
			sb.append( '[' );
			sb.append( size );
			sb.append( ']' );
			if( size > 0 ) nTotalSize *= size;
		}
		sb.append( ' ' );
		sb.append( nTotalSize );
		return sb.toString();
	}
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}

class Node_Sequence extends Node {
	DSequence dsequence;
	protected Node_Sequence( DSequence bt ){
		super( bt );
		dsequence = bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Sequence; }
	ArrayList<String> listFieldNames = new ArrayList<String>();
	ArrayList<DAP_VARIABLE> listFieldTypes = new ArrayList<DAP_VARIABLE>();
	ArrayList<ArrayList<BaseType>> listRows = new ArrayList<ArrayList<BaseType>>(); // this contains the data, if any 
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}

class Node_Byte extends Node {
	DByte dbyte;
	protected Node_Byte( DByte bt ){
		super( bt );
		dbyte = bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Byte; }
	int value;
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}
class Node_Int16 extends Node {
	DInt16 dint16;
	protected Node_Int16( DInt16 bt ){
		super( bt );
		dint16 = bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Int16; }
	int value;
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}
class Node_Int32 extends Node {
	DInt32 dint32;
	protected Node_Int32( DInt32 bt ){
		super( bt );
		dint32 = bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Int32; }
	int value;
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}
class Node_UInt16 extends Node {
	DUInt16 duint16;
	protected Node_UInt16( DUInt16 bt ){
		super( bt );
		duint16 = bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.UInt16; }
	int value;
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}
class Node_UInt32 extends Node {
	DUInt32 duint32;
	protected Node_UInt32( DUInt32 bt ){
		super( bt );
		duint32 = bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.UInt32; }
	long value;
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}
class Node_Float32 extends Node {
	DFloat32 dfloat32;
	protected Node_Float32( DFloat32 bt ){
		super( bt );
		dfloat32 = bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Float32; }
	float value;
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}
class Node_Float64 extends Node {
	DFloat64 dfloat64;
	protected Node_Float64( DFloat64 bt ){
		super( bt );
		dfloat64 = bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Float64; }
	double value;
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}
class Node_String extends Node {
	DString dstring;
	protected Node_String( DString bt ){
		super( bt );
		dstring = bt;
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.String; }
	String value;
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}
