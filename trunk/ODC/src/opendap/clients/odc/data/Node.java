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
	DAP_VARIABLE eVariableType = null;
	private String msName = "[undefined]";
	protected transient Model_VariableView _view = new Model_VariableView();
	AttributeTable attributes;
	Node nodeParent;
	ArrayList<Node> subnodes;
	private Node(){ // not a valid constructor
		super();
	}
	protected Node( opendap.dap.BaseType bt ){
		super();
		setBaseType( bt );
		subnodes = new ArrayList<Node>();
	}
	void setModel( Model_Dataset_Local parent ){
		modelParent = parent;
	}
	public Model_VariableView _getView(){ return _view; }
	opendap.dap.BaseType getBaseType(){ return mBaseType; }
	abstract DAP_VARIABLE getType();
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
				sbError.insert( 0, "error loading root variable " + xVariable + " " + bt.getClearName() );
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
	
	Node createDefaultMember( DAP_VARIABLE variable_type, StringBuffer sbError ){
		
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
		new_bt.setParent( getBaseType() );
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
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Structure; }
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
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Grid; }
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
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Sequence; }
	ArrayList<String> listFieldNames = new ArrayList<String>();
	ArrayList<DAP_VARIABLE> listFieldTypes = new ArrayList<DAP_VARIABLE>();
	ArrayList<ArrayList<BaseType>> listRows = new ArrayList<ArrayList<BaseType>>(); // this contains the data, if any 
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}

class Node_Array extends Node {
	public static String DEFAULT_ArrayName = "array";	
	public static String DEFAULT_DimensionName = "dim";
	public static int DEFAULT_DimensionSize = 100;
	DArray darray;
	protected Node_Array( DArray bt ){
		super( bt );
		darray = bt;
	}
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Array; }
	DAP_TYPE eDataType;
	ArrayList<DArrayDimension> listDimensions = new ArrayList<DArrayDimension>();
	PrimitiveVector _getPrimitiveVector(){ return darray.getPrimitiveVector(); }
	int _getValueCount(){ return darray.getLength(); }
	int _getByteSize(){ return darray.getLength() * DAP.getDataSize( _getValueType() ); }
	int _getDimensionCount(){ return darray.numDimensions(); }
	int _getDimensionSize( int xDimension1 ){
		try {
			return darray.getDimension( xDimension1 - 1 ).getSize(); 
		} catch( Throwable t ) {
			return 0;
		}
	}
	int _getRowCount(){ // calculates row count according to view
		int[] aiDimensionLengths = _getDimensionLengths1();
		if( _view.dim_row == 0 || _view.dim_row > aiDimensionLengths[0] ) return 0; 
		return _getDimensionLengths1()[_view.dim_row];
	}
	int _getColumnCount(){ // calculates row count according to view
		int[] aiDimensionLengths = _getDimensionLengths1();
		if( _view.dim_column == 0 || _view.dim_column > aiDimensionLengths[0] ) return 0;
		int column_count = aiDimensionLengths[_view.dim_column];
		return column_count;
	}
	int _getValueIndex( int row, int column ){ // calculates index of value according to view, zero-based
		if( _getDimensionCount() == 1 ) if( _view.dim_row == 1 ) return row; else return column;
		if( _getDimensionCount() == 2 ) if( _view.dim_row == 1 ) return column + row * _getDimensionLengths1()[2]; else return row + column * _getDimensionLengths1()[1];
		else return 0; // TODO
	}
	private final int[] aiDimSelector = new int[10];
	private final int[] aiDimSize = new int[10];
	public int _getValueIndex_Cursor(){  // returns the value index of the view cursor
		int xDimRow = _view.dim_row;
		int xDimColumn = _view.dim_column;
		aiDimSelector[0] = _getDimensionCount();
		for( int xDimension = 1; xDimension <= aiDimSelector[0]; xDimension++ ){
			if( xDimension == xDimRow ){
				aiDimSelector[xDimension] = _view.cursor_row;
			} else if( xDimension == xDimColumn ){
				aiDimSelector[xDimension] = _view.cursor_column;
			} else {
				aiDimSelector[xDimension] = _view.page[xDimension];
			}
		}
		return _getValueIndex( aiDimSelector );  
	}
	int _getValueIndex( int[] axDim1 ){ // calculates index of value according to exact set of dimensional indices
		int index = 0;
		int[] aiDimLengths1 = _getDimensionLengths1();
		int ctDimensions = aiDimSelector[0];
		for( int xDimension = 1; xDimension <= ctDimensions ; xDimension++ )
			index += java.lang.Math.pow( aiDimLengths1[xDimension], xDimension - 1 ) * axDim1[ctDimensions - xDimension + 1];
		return index;
	}
	String _getValueString_cursor(){
		return _getValueString( _view.cursor_row, _view.cursor_column );
	}
	String _getValueString_selection(){
		StringBuffer sb = new StringBuffer();
		for( int xRow = _view.selectionUL_row; xRow <= _view.selectionLR_row; xRow++ ){
			sb.append( _getValueString( xRow, _view.selectionUL_column ) ); 
			for( int xColumn = _view.selectionUL_column + 1; xColumn <= _view.selectionLR_column; xColumn++ ){
				sb.append( '\t' );
				sb.append( _getValueString( xRow, xColumn ) ); 
			}
			sb.append( '\n' );
		}
		return sb.toString();
	}
	int _getSelectionSize(){
		return (_view.selectionLR_row - _view.selectionUL_row + 1) * (_view.selectionLR_column - _view.selectionUL_column + 1);  
	}
	String _getValueString( int row, int column ){
		int xValue = _getValueIndex( row, column );
		opendap.dap.PrimitiveVector pv = _getPrimitiveVector();
		Object oValues = pv.getInternalStorage();
		int[] aiValues = (int[])oValues;
		return Integer.toString( aiValues[xValue] );
	}
	Object _getValueObject( int index ){
		opendap.dap.PrimitiveVector pv = _getPrimitiveVector();
		Object oValues = pv.getInternalStorage();
		switch( _getValueType() ){
			case Byte:
			case Int16:
				short[] ashValues = (short[])oValues;
				return new Short( ashValues[index] ); 
			case Int32:
			case UInt16:
				int[] aiValues = (int[])oValues;
				return new Integer( aiValues[index] ); 
			case UInt32:
				long[] anValues = (long[])oValues;
				return new Long( anValues[index] ); 
			case Float32:
				float[] afValues = (float[])oValues;
				return new Float( afValues[index] ); 
			case Float64:
				double[] adValues = (double[])oValues;
				return new Double( adValues[index] ); 
			case String:
				String[] asValues = (String[])oValues;
				return asValues[index]; 
			default:
				return null;
		}
	}
	public boolean _deleteSelectedValue( StringBuffer sbError ){
		return _deleteValue( _getValueIndex( _view.cursor_row, _view.cursor_column ), sbError );  
	}
	public boolean _deleteAllSelectedValues( StringBuffer sbError ){  // TODO process errors
		for( int xRow = _view.selectionUL_row; xRow <= _view.selectionLR_row; xRow++ ){ 
			for( int xColumn = _view.selectionUL_column; xColumn <= _view.selectionLR_column; xColumn++ ){
				_deleteValue( _getValueIndex( xRow, xColumn ), sbError );
			}
		}
		return true;
	}
	public boolean _deleteValue( int index, StringBuffer sbError ){
		try {
			opendap.dap.PrimitiveVector pv = _getPrimitiveVector();
			Object oValues = pv.getInternalStorage();
			switch( _getValueType() ){
				case Byte:
				case Int16:
					short[] ashValues = (short[])oValues;
					ashValues[index] = 0; 
					break;
				case Int32:
				case UInt16:
					int[] aiValues = (int[])oValues;
					aiValues[index] = 0; 
					break;
				case UInt32:
					long[] anValues = (long[])oValues;
					anValues[index] = 0; 
					break;
				case Float32:
					float[] afValues = (float[])oValues;
					afValues[index] = 0; 
					break;
				case Float64:
					double[] adValues = (double[])oValues;
					adValues[index] = 0; 
					break;
				case String:
					String[] asValues = (String[])oValues;
					asValues[index] = null; 
					break;
				default:
					sbError.append( "unknown DAP type" );
					return false;
			}
			return true;
		} catch( NumberFormatException ex ) {
			sbError.append( "numerical error deleting value of type " + _getValueType() );
			return false;
		}
	}
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		return _setValue( sNewValue, _getValueIndex( _view.cursor_row, _view.cursor_column ), sbError );  
	}
	public boolean _setAllSelectedValues( String sNewValue, StringBuffer sbError ){  // TODO process errors
		for( int xRow = _view.selectionUL_row; xRow <= _view.selectionLR_row; xRow++ ){ 
			for( int xColumn = _view.selectionUL_column; xColumn <= _view.selectionLR_column; xColumn++ ){
				_setValue( sNewValue, _getValueIndex( xRow, xColumn ), sbError );
			}
		}
		return true;
	}
	public boolean _setValue( String sNewValue, int index, StringBuffer sbError ){
		try {
			opendap.dap.PrimitiveVector pv = _getPrimitiveVector();
			Object oValues = pv.getInternalStorage();
			switch( _getValueType() ){
				case Byte:
				case Int16:
					short[] ashValues = (short[])oValues;
					ashValues[index] = Short.parseShort( sNewValue ); 
					break;
				case Int32:
				case UInt16:
					int[] aiValues = (int[])oValues;
					aiValues[index] = Integer.parseInt( sNewValue ); 
					break;
				case UInt32:
					long[] anValues = (long[])oValues;
					anValues[index] = Integer.parseInt( sNewValue ); 
					break;
				case Float32:
					float[] afValues = (float[])oValues;
					afValues[index] = Integer.parseInt( sNewValue ); 
					break;
				case Float64:
					double[] adValues = (double[])oValues;
					adValues[index] = Double.parseDouble( sNewValue ); 
					break;
				case String:
					String[] asValues = (String[])oValues;
					asValues[index] = sNewValue; 
					break;
				default:
					sbError.append( "unknown DAP type" );
					return false;
			}
			return true;
		} catch( NumberFormatException ex ) {
			sbError.append( "new value '" + sNewValue + "' could not be interpeted as a " + _getValueType() );
			return false;
		}
	}
	public void _setError( int index ){
		try {
			opendap.dap.PrimitiveVector pv = _getPrimitiveVector();
			Object oValues = pv.getInternalStorage();
			switch( _getValueType() ){
				case Byte:
					short[] ashValues_byte = (short[])oValues;
					ashValues_byte[index] = 0xFF;
					break;
				case Int16:
					short[] ashValues_short = (short[])oValues;
					ashValues_short[index] = Short.MIN_VALUE; 
					break;
				case Int32:
					int[] aiValues_int32 = (int[])oValues;
					aiValues_int32[index] = Integer.MIN_VALUE; 
				case UInt16:
					int[] aiValues_uint16 = (int[])oValues;
					aiValues_uint16[index] = 0xFFFF; 
					break;
				case UInt32:
					long[] anValues = (long[])oValues;
					anValues[index] = 0xFFFFFFFF; 
					break;
				case Float32:
					float[] afValues = (float[])oValues;
					afValues[index] = Float.NaN; 
					break;
				case Float64:
					double[] adValues = (double[])oValues;
					adValues[index] = Double.NaN; 
					break;
				case String:
					String[] asValues = (String[])oValues;
					asValues[index] = "#ERROR#"; 
					break;
				default:
					return;
			}
			return;
		} catch( Throwable t ) {
			ApplicationController.getInstance().vUnexpectedError( t, "while attempting to set array value to error" );
			return;
		}
	}	
	public boolean _setValue( PyObject oNewValue, int index, StringBuffer sbError ){
		try {
			opendap.dap.PrimitiveVector pv = _getPrimitiveVector();
			Object oValues = pv.getInternalStorage();
			switch( _getValueType() ){
				case Byte:
				case Int16:
					short[] ashValues = (short[])oValues;
					ashValues[index] = ((Short)oNewValue.__tojava__(Short.class)).shortValue(); 
					break;
				case Int32:
				case UInt16:
					int[] aiValues = (int[])oValues;
					aiValues[index] = ((Integer)oNewValue.__tojava__(Integer.class)).intValue(); 
					break;
				case UInt32:
					long[] anValues = (long[])oValues;
					anValues[index] = ((Long)oNewValue.__tojava__(Long.class)).longValue(); 
					break;
				case Float32:
					float[] afValues = (float[])oValues;
					afValues[index] = ((Float)oNewValue.__tojava__(Float.class)).floatValue(); 
					break;
				case Float64:
					double[] adValues = (double[])oValues;
					adValues[index] = ((Double)oNewValue.__tojava__(Double.class)).doubleValue(); 
					break;
				case String:
					String[] asValues = (String[])oValues;
					asValues[index] = (String)oNewValue.__tojava__(String.class); 
					break;
				default:
					sbError.append( "unknown DAP type" );
					return false;
			}
			return true;
		} catch( NumberFormatException ex ) {
			sbError.append( "new value '" + oNewValue + "' could not be interpeted as a " + _getValueType() );
			return false;
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
	}
	String _getValueTypeString(){
		return DAP.getDArrayType_String( darray );
	}
	DAP_TYPE _getValueType(){
		return DAP.getDAPType( darray );
	}
	String _getDimensionName( int xDimension1 ){
		try {
			return darray.getDimension( xDimension1 - 1 ).getName(); 
		} catch( Throwable t ) {
			return null;
		}
	}
	boolean _addDimension( StringBuffer sbError ){
		return _addDimension( DEFAULT_DimensionSize, sbError );
	}
	boolean _addDimension( int iDimensionSize, StringBuffer sbError ){
		if( Integer.MAX_VALUE / iDimensionSize < _getValueCount() ){
			sbError.append( "unable to add dimension because arrays may not have more than " + Utility_String.getByteCountReadable( Integer.MAX_VALUE ) + " values" );
			return false;
		}
		int iNewValueCount = iDimensionSize * _getValueCount();
		if( Integer.MAX_VALUE / DAP.getDataSize( _getValueType() ) < iNewValueCount ){
			sbError.append( "unable to add dimension because size of array would exceed " + Utility_String.getByteCountReadable( Integer.MAX_VALUE ) + " bytes" );
			return false;
		}
		int iNewByteSize = DAP.getDataSize( _getValueType() ) * iNewValueCount;
		int iAdditionalBytesRequired = iNewByteSize - _getByteSize();  
		if( ! Utility.zMemoryCheck( iNewByteSize ) ){ // need to accommodate new array in memory
			sbError.append( "unable to add dimension because there is not enough memory for an additional " + Utility_String.getByteCountReadable( iAdditionalBytesRequired ) + " bytes. See help for info on memory." );
			return false;
		}
		darray.appendDim( iDimensionSize, DEFAULT_DimensionName + (_getDimensionCount() + 1) );
		Object[] eggPrimitiveVector = new Object[1];
		eggPrimitiveVector[0] = darray.getPrimitiveVector().getInternalStorage();
		if( DAP.zDimension_Add( eggPrimitiveVector, _getValueType(), iDimensionSize, sbError ) ){
			darray.getPrimitiveVector().setInternalStorage( eggPrimitiveVector[0] );
		} else {
			sbError.insert( 0, "dimension size " + iDimensionSize + ": " );
			return false;
		}
		return true;
	}
	boolean _setDimensionName( int xDimension1, String sNewName, StringBuffer sbError ){
		try {
			DArrayDimension dim = darray.getDimension( xDimension1 - 1 );
			dim.setName( sNewName );
			return true;
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
	}
	int[] _getDimensionLengths1(){
		int ctDimension = _getDimensionCount();
		int[] aiDimensionLengths = new int[ctDimension + 1];
		aiDimensionLengths[0] = ctDimension;
		for( int xDimension1 = 1; xDimension1 <= ctDimension; xDimension1++ ){
			aiDimensionLengths[xDimension1] = _getDimensionSize( xDimension1 );
		}
		return aiDimensionLengths;
	}
	boolean _setDimensionSize( int xDimension1, int iNewSize, StringBuffer sbError ){
		try {
			if( iNewSize == 0 ){
				sbError.append( "dimensions cannot have a size of 0" );
				return false;
			}
			if( iNewSize == -1 ){
				sbError.append( "dimensions sizes must be positive integers" );
				return false;
			}
			DArrayDimension dim = darray.getDimension( xDimension1 - 1 );
			Object[] eggPrimitiveVector = new Object[1];
			eggPrimitiveVector[0] = darray.getPrimitiveVector().getInternalStorage();
			int[] aiDimLengths = _getDimensionLengths1();
			if( DAP.zDimension_ModifySize( eggPrimitiveVector, _getValueType(), aiDimLengths, xDimension1, iNewSize, sbError ) ){
				darray.getPrimitiveVector().setInternalStorage( eggPrimitiveVector[0] );
			} else {
				sbError.insert( 0, "new dimension " + xDimension1 + " size " + iNewSize + ": " );
				return false;
			}
			dim.setSize( iNewSize );
			return true;
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
	}
	static BaseType createDefaultBaseType( int iIndexNumber, StringBuffer sbError ){
		DArray array = new DArray();
		array.appendDim( DEFAULT_DimensionSize, DEFAULT_DimensionName + "1" );
		opendap.dap.DInt32 template = new opendap.dap.DInt32();
		template.setName( DEFAULT_ArrayName + iIndexNumber );
		array.addVariable( template ); // this creates the primitive vector inside the array
		int[] aiDefaultArray = new int[DEFAULT_DimensionSize];
		PrimitiveVector pv = array.getPrimitiveVector();
		pv.setInternalStorage( aiDefaultArray );
		return array;
	}
	String _sGetSummaryText(){
		StringBuffer sb = new StringBuffer( 250 );
		sb.append( getName() );
		long nTotalSize = 1;
		for( int xDimension = 1; xDimension <= listDimensions.size(); xDimension++ ){
			DArrayDimension dim = listDimensions.get( xDimension - 1);
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
}

class Node_Byte extends Node {
	DByte dbyte;
	protected Node_Byte( DByte bt ){
		super( bt );
		dbyte = bt;
	}
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Byte; }
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
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Int16; }
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
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Int32; }
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
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.UInt16; }
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
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.UInt32; }
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
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Float32; }
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
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Float64; }
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
	DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.String; }
	String value;
	public boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}
