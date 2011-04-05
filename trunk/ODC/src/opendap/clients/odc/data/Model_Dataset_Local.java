package opendap.clients.odc.data;

import opendap.dap.*;
import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.SavableImplementation;
import opendap.clients.odc.DAP.*;

import java.util.ArrayList;

// local ODC copy of a dataset
// used for modifying and creating datasets
// the reasons this class is needed are that the "vars" member of DStructure is not public, so
// there is no way to change the order of variables or to insert new variables in a particular 
// place in the structure, and the design of the DAP is clumsy so it is awkward to use it
// Every time the user makes a change to the local copy of the structure the DAP model is
// completely regenerated.
public class Model_Dataset_Local implements java.io.Serializable {
	
	private static final long serialVersionUID = 1L;
	transient private SavableImplementation mSavable;
	boolean zHasData;
	String sDatasetName;
	Node dataset_root;
	
	private Model_Dataset_Local(){}
	
	public SavableImplementation getSavable(){ return mSavable; }	

    /**
     * Set the directory location of the file this dataset is currently being saved to.
     * @param urlTitle The title of the URL.
     */
    public void setFileName( String sNewFileName ){
		mSavable._setFileName( sNewFileName );
    }    

    /**
     * Set the directory location of the file this dataset is currently being saved to.
     * @param urlTitle The title of the URL.
     */
    public void setFileDirectory( String sNewDirectory ){
		mSavable._setFileDirectory( sNewDirectory );
    }    
	
	public static Model_Dataset_Local create( Model_Dataset model, StringBuffer sbError ){
		if( model == null ){
			sbError.append( "local dataset model can only be created from an existing DAP model or with a starting name" );
			return null;
		}
		DataDDS data_dds = model.getData();
		DDS dds = model.getDDS_Full();
		if( data_dds == null && dds == null ){
			sbError.append( "no data or dds supplied, invalid mode" );
			return null;
		}
		Model_Dataset_Local local = new Model_Dataset_Local();
		if( data_dds == null ){
			local.zHasData = false;
		} else {
			dds = data_dds;
			local.zHasData = true;
		}
		local.sDatasetName = dds.getName();
		local.dataset_root = Node.createRoot( dds, sbError);
		local.mSavable = new SavableImplementation( Model_Dataset_Local.class, null, null );
		return local;
	}
	
	// creates an empty DDS
	public static Model_Dataset_Local create( String sName, StringBuffer sbError ){
		Model_Dataset_Local local = new Model_Dataset_Local();
		if( ! DAP.isValidIdentifier( sName, sbError ) ){
			sbError.insert( 0, "invalid identifier (" + sName + "): " );
			return null;
		}
		opendap.dap.DDS dds = new opendap.dap.DDS();
		local.sDatasetName = sName;
		local.dataset_root = Node.createRoot( dds, sbError );
		return local;
	}
	
	// generates DataDDS if there is data in the model
	Model_Dataset generateModel( StringBuffer sbError){
		Model_Dataset model = null;
		opendap.dap.DDS dds = null;
		if( zHasData ){
			try {
				String sServerVersion = "2.1.5";
				int iHeaderType = opendap.dap.ServerVersion.XDAP; // this is the simple version (eg "2.1.5"), server version strings are like "XDODS/2.1.5", these are only used in HTTP, not in files
				opendap.dap.ServerVersion server_version = new opendap.dap.ServerVersion( sServerVersion, iHeaderType );
				dds = new opendap.dap.DataDDS( server_version );
			} catch( Throwable t ) {
				ApplicationController.vUnexpectedError( t, sbError );
			}
		} else {
			dds = new opendap.dap.DDS();
		}
		dds.setName( this.sDatasetName );
		if( ! zAddNodeToStructure( dataset_root, dds, sbError ) ){ // this call recursively adds the whole structure
			sbError.insert( 0, "error adding root to DDS: " );
			return null;
		}
		if( zHasData ){
			try {
				model = Model_Dataset.createData( (DataDDS)dds, sbError );
				if( model == null ){
					sbError.insert( 0, "(Model_Dataset_Local) error creating data model: " );
					return null;
				}
			} catch( Throwable t ) {
				ApplicationController.vUnexpectedError( t, sbError );
			}
		} else {
			model = Model_Dataset.createDataDefinition( dds, sbError );
			if( model == null ){
				sbError.insert( 0, "error creating data definition: " );
				return null;
			}
		}
		return model;
	}
	
	boolean zAddNodeToStructure( Node node, DStructure structure, StringBuffer sbError ){
		for( int xSubnode = 1; xSubnode <= node.subnodes.size(); xSubnode++ ){
			Node nodeCurrent = node.subnodes.get( xSubnode - 1 );
			BaseType new_bt = null;
			switch( nodeCurrent.eVariableType ){
				case Structure:
					new_bt = new DStructure(); break;
				case Grid:
					new_bt = new DGrid(); break;
				case Sequence:
					new_bt = new DSequence(); break;
				case Array:
					new_bt = new DArray(); break;
				case Byte:
					new_bt = new DByte(); break;
				case Int16:
					new_bt = new DInt16(); break;
				case Int32:
					new_bt = new DInt32(); break;
				case UInt16:
					new_bt = new DUInt16(); break;
				case UInt32:
					new_bt = new DUInt32(); break;
				case Float32:
					new_bt = new DFloat32(); break;
				case Float64:
					new_bt = new DFloat64(); break;
				case String:
					new_bt = new DString(); break;
			}
			new_bt.setClearName( nodeCurrent.sName );
			new_bt.setParent( structure );
			if( ! zCopyAttributeTable( nodeCurrent, new_bt, sbError ) ){
				sbError.insert( 0, "error copying attribute table: " );
				return false;
			}
			switch( nodeCurrent.eVariableType ){
				case Structure:
					DStructure new_structure = (DStructure)new_bt;
					int xMember = 0;
					while( xMember < nodeCurrent.subnodes.size() ){
						xMember++;
						Node nodeMember = nodeCurrent.subnodes.get( xMember - 1 );
						if( ! zAddNodeToStructure( nodeMember, new_structure, sbError ) ){
							sbError.insert( 0, "error adding member " + nodeMember.sName + " to structure: " );
							return false;
						}
					}
					break;
				case Grid:
					DGrid btGrid = (DGrid)new_bt;
					Node_Grid nodeGrid = (Node_Grid)nodeCurrent;
					Node_Array nodeValueArray = nodeGrid.arrayValues;
					DArray btValueArray = new DArray();
					btValueArray.getPrimitiveVector().setInternalStorage( nodeValueArray.primitive_vector );
					for( int xDimension = 1; xDimension <= nodeValueArray.listDimensions.size(); xDimension++ ){
						DArrayDimension dimension = nodeValueArray.listDimensions.get( xDimension - 1 );
						btValueArray.appendDim( dimension.getSize(), dimension.getClearName() );
						try {
							btValueArray.getDimension( xDimension - 1 ).setProjection( dimension.getStart(), dimension.getStride(), dimension.getStop() );
						} catch( Throwable t ){
							sbError.append( "error setting projection for DArray " + nodeCurrent.sName + ": " + t );
							return false;
						}
					}
					btGrid.addVariable( btValueArray, DGrid.ARRAY );
					for( int xDimension = 1; xDimension <= nodeGrid.darrayDimensionalVectors.size(); xDimension++ ){
						btGrid.addVariable( nodeGrid.darrayDimensionalVectors.get( xDimension - 1 ), DGrid.MAPS );
					}
					break;
				case Sequence:
					// TODO
					break;
				case Array:
					DArray btArray = (DArray)new_bt;
					Node_Array nodeArray = (Node_Array)nodeCurrent;
					btArray.getPrimitiveVector().setInternalStorage( nodeArray.primitive_vector );
					for( int xDimension = 1; xDimension <= nodeArray.listDimensions.size(); xDimension++ ){
						DArrayDimension dimension = nodeArray.listDimensions.get( xDimension - 1 );
						btArray.appendDim( dimension.getSize(), dimension.getClearName() );
						try {
							btArray.getDimension( xDimension - 1 ).setProjection( dimension.getStart(), dimension.getStride(), dimension.getStop() );
						} catch( Throwable t ){
							sbError.append( "error setting projection for DArray " + nodeCurrent.sName + ": " + t );
							return false;
						}
					}
					break;
				case Byte:
					DByte dbyte = (DByte)new_bt;
					Node_Byte nodeByte = (Node_Byte)nodeCurrent;
					dbyte.setValue( (byte)nodeByte.value );
					break;
				case Int16:
					DInt16 dint16 = (DInt16)new_bt;
					Node_Int16 nodeInt16 = (Node_Int16)nodeCurrent;
					dint16.setValue( (short)nodeInt16.value );
					break;
				case Int32:
					DInt32 dint32 = (DInt32)new_bt;
					Node_Int32 nodeInt32 = (Node_Int32)nodeCurrent;
					dint32.setValue( (int)nodeInt32.value );
					break;
				case UInt16:
					DUInt16 duint16 = (DUInt16)new_bt;
					Node_UInt16 nodeUInt16 = (Node_UInt16)nodeCurrent;
					duint16.setValue( (short)nodeUInt16.value );
					break;
				case UInt32:
					DUInt32 duint32 = (DUInt32)new_bt;
					Node_UInt32 nodeUInt32 = (Node_UInt32)nodeCurrent;
					duint32.setValue( (int)nodeUInt32.value );
					break;
				case Float32:
					DFloat32 dfloat32 = (DFloat32)new_bt;
					Node_Float32 nodeFloat32 = (Node_Float32)nodeCurrent;
					dfloat32.setValue( nodeFloat32.value );
					break;
				case Float64:
					DFloat64 dfloat64 = (DFloat64)new_bt;
					Node_Float64 nodeFloat64 = (Node_Float64)nodeCurrent;
					dfloat64.setValue( nodeFloat64.value );
					break;
				case String:
					DString dstring = (DString)new_bt;
					Node_String nodeString = (Node_String)nodeCurrent;
					dstring.setValue( nodeString.value );
					break;
			}
			structure.addVariable( new_bt );
		}
		return true;
	}
	
	boolean zCopyAttributeTable( Node node, BaseType bt, StringBuffer sbError ){
		AttributeTable at_src = node.attributes;
		AttributeTable at_dest = bt.getAttributeTable();
		java.util.Enumeration attribute_name_list = at_src.getNames();
		while( attribute_name_list.hasMoreElements() ){
			String sAttributeName = (String)attribute_name_list.nextElement();
			Attribute source_attribute = at_src.getAttribute( sAttributeName );
			try {
				at_dest.appendAttribute( sAttributeName, source_attribute.getType(), source_attribute.getValueAt(0) );
			} catch( Throwable t ){}
		}
		return true;
	}

}

class Node {   // functions as both the root node and a structure node
	BaseType bt; // null only in the case of the root node
	DAP_VARIABLE eVariableType;
	String sName;
	AttributeTable attributes;
	Node nodeParent;
	ArrayList<Node> subnodes;
	private void Node(){} // not a valid constructor
	protected Node( BaseType bt ){
		this.bt = bt;
		subnodes = new ArrayList<Node>();
	}
	public static Node createRoot( DDS dds, StringBuffer sbError ){
		Node nodeRoot = new Node( null ); // only the root node has a null basetype
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
		return nodeRoot;
	}
	public static Node create( Node parent_node, BaseType bt, StringBuffer sbError ){
		if( bt == null ){
			sbError.append( "no base type supplied (root nodes can only be created by the _createRoot method)" );
			return null;
		}
		DAP_VARIABLE variable_type = DAP.getVariableType( bt, sbError );
		if( variable_type == null ) return null; // an error occurred
		Node new_node = null;
		switch( variable_type ){
			case Structure:
				new_node = new Node( bt ); break;
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
		new_node.eVariableType = variable_type;
		new_node.sName = bt.getName();
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
						sbError.insert( 0, "error loading DStructure " + new_node.sName + ": " );
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
						sbError.insert( 0, "error loading value array for DGrid " + new_node.sName + ": " );
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
				nodeArray.primitive_vector = array.getPrimitiveVector();
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
		return sName;
	}
	
	boolean createDefaultMember( DAP_VARIABLE variable_type, StringBuffer sbError ){
		String sDefaultVariableName = "new_variable";
		BaseType new_bt = null;
		switch( variable_type ){
			case Structure:
				new_bt = new DStructure(); break;
			case Grid:
				new_bt = new DGrid(); break;
			case Sequence:
				new_bt = new DSequence(); break;
			case Array:
				new_bt = new DArray(); break;
			case Byte:
				new_bt = new DByte(); break;
			case Int16:
				new_bt = new DInt16(); break;
			case Int32:
				new_bt = new DInt32(); break;
			case UInt16:
				new_bt = new DUInt16(); break;
			case UInt32:
				new_bt = new DUInt32(); break;
			case Float32:
				new_bt = new DFloat32(); break;
			case Float64:
				new_bt = new DFloat64(); break;
			case String:
				new_bt = new DString(); break;
		}
		new_bt.setClearName( sDefaultVariableName );
		new_bt.setParent( this.bt );
		// create attribute table TODO
		switch( variable_type ){
			case Structure:
				// no further initialization required
				break;
			case Grid:
				// no further initialization required
				break;
			case Sequence:
				// no further initialization required
				break;
			case Array:
				// no further initialization required
				break;
			case Byte:
				// no further initialization required
				break;
			case Int16:
				// no further initialization required
				break;
			case Int32:
				// no further initialization required
				break;
			case UInt16:
				// no further initialization required
				break;
			case UInt32:
				// no further initialization required
				break;
			case Float32:
				// no further initialization required
				break;
			case Float64:
				// no further initialization required
				break;
			case String:
				// no further initialization required
				break;
		}
		Node new_node = Node.create( this, new_bt, sbError );
		if( new_node == null ){
			sbError.insert( 0, "failed to create new node: " );
			return false;
		}
		subnodes.add( new_node );
		return true;
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
	String sGetSummaryText(){
		StringBuffer sb = new StringBuffer( 250 );
		sb.append( sName );
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
}

class Node_Sequence extends Node {
	DSequence dsequence;
	protected Node_Sequence( DSequence bt ){
		super( bt );
		dsequence = bt;
	}
	ArrayList<String> listFieldNames = new ArrayList<String>();
	ArrayList<DAP_VARIABLE> listFieldTypes = new ArrayList<DAP_VARIABLE>();
	ArrayList<ArrayList<BaseType>> listRows = new ArrayList<ArrayList<BaseType>>(); // this contains the data, if any 
}

class Node_Array extends Node {
	DArray darray;
	protected Node_Array( DArray bt ){
		super( bt );
		darray = bt;
	}
	DAP_TYPE eDataType;
	ArrayList<DArrayDimension> listDimensions = new ArrayList<DArrayDimension>(); 
	PrimitiveVector primitive_vector; // the data values, if any	
	String sGetSummaryText(){
		StringBuffer sb = new StringBuffer( 250 );
		sb.append( sName );
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
	int value;
}
class Node_Int16 extends Node {
	DInt16 dint16;
	protected Node_Int16( DInt16 bt ){
		super( bt );
		dint16 = bt;
	}
	int value;
}
class Node_Int32 extends Node {
	DInt32 dint32;
	protected Node_Int32( DInt32 bt ){
		super( bt );
		dint32 = bt;
	}
	int value;
}
class Node_UInt16 extends Node {
	DUInt16 duint16;
	protected Node_UInt16( DUInt16 bt ){
		super( bt );
		duint16 = bt;
	}
	int value;
}
class Node_UInt32 extends Node {
	DUInt32 duint32;
	protected Node_UInt32( DUInt32 bt ){
		super( bt );
		duint32 = bt;
	}
	long value;
}
class Node_Float32 extends Node {
	DFloat32 dfloat32;
	protected Node_Float32( DFloat32 bt ){
		super( bt );
		dfloat32 = bt;
	}
	float value;
}
class Node_Float64 extends Node {
	DFloat64 dfloat64;
	protected Node_Float64( DFloat64 bt ){
		super( bt );
		dfloat64 = bt;
	}
	double value;
}
class Node_String extends Node {
	DString dstring;
	protected Node_String( DString bt ){
		super( bt );
		dstring = bt;
	}
	String value;
}



