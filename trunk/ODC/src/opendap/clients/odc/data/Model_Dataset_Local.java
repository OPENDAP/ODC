package opendap.clients.odc.data;

import opendap.dap.*;
import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.SavableImplementation;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.DAP.*;

import java.util.ArrayList;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

// local ODC copy of a dataset
// used for modifying and creating datasets
// the reasons this class is needed are that the "vars" member of DStructure is not public, so
// there is no way to change the order of variables or to insert new variables in a particular 
// place in the structure, and the design of the DAP is clumsy so it is awkward to use it
// Every time the user makes a change to the local copy of the structure the DAP model is
// completely regenerated.
public class Model_Dataset_Local extends DefaultTreeModel implements java.io.Serializable {
	
	public static final long serialVersionUID = 1L;
	public static final char SEPARATOR = '.';
	Model_Dataset mSourceModel = null;
	transient private SavableImplementation mSavable;
	boolean zHasData;
	private String sDatasetName;
	private Model_Dataset_Local( Node nodeRoot ){
		super( nodeRoot );
	}
	
	public SavableImplementation getSavable(){ return mSavable; }	

	public String getName(){
		return sDatasetName;
	}

	public boolean setName( String sNewName, StringBuffer sbError ){
		try {
			mSourceModel.getData().setName( sNewName );
			return true;
		} catch( Throwable t ) {
			sbError.append( "Error setting dataset name: " + t );
			return false;
		}
	}
	
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
		Node nodeRoot = Node.createRoot( dds, sbError);
		if( nodeRoot == null ){
			sbError.insert( 0, "failed to create root node: " );
			return null;
		}
		Model_Dataset_Local local = new Model_Dataset_Local( nodeRoot );
		nodeRoot.setModel( local );
		local.mSourceModel = model;
		if( data_dds == null ){
			local.zHasData = false;
		} else {
			dds = data_dds;
			local.zHasData = true;
		}
		local.sDatasetName = dds.getName();
		local.mSavable = new SavableImplementation( Model_Dataset_Local.class, null, null );
		return local;
	}
	
	// creates an empty DDS
	public static Model_Dataset_Local create( String sName, StringBuffer sbError ){
		opendap.dap.DDS dds = new opendap.dap.DDS();
		Node nodeRoot = Node.createRoot( dds, sbError );
		Model_Dataset_Local local = new Model_Dataset_Local( nodeRoot );
		nodeRoot.setModel( local );
		if( ! DAP.isValidIdentifier( sName, sbError ) ){
			sbError.insert( 0, "invalid identifier (" + sName + "): " );
			return null;
		}
		local.sDatasetName = sName;
		return local;
	}

	Node getRootNode(){ return (Node)this.getRoot(); }
	void _update( BaseType bt ){
		_update( (Node)getRoot(), bt ); 
	}
	void _update( Node node, BaseType bt ){
		if( node.getBaseType().equals( bt ) ){
			nodeChanged( node );
			return;
		}
		java.util.Enumeration<Node> children = (java.util.Enumeration<Node>)node.children();
		while( children.hasMoreElements() ){
			_update( children.nextElement(), bt );
		}
	}
	String getPathForNode( Node node ){
		if( node == null ) return "";
		TreeNode[] aNodes = node.getPath();
		StringBuffer sbPath = new StringBuffer(80);
		for( int xNode = 1; xNode < aNodes.length; xNode++ ){ // skip the root
			Node nodeCurrent = (Node)aNodes[xNode];
			sbPath.append(nodeCurrent.getName());
			sbPath.append( SEPARATOR );
		}
		return sbPath.toString();
	}
	String getPrintout(){
		Node nodeRoot = (Node)this.getRoot();
		return getPrintoutForNode( nodeRoot, 0 );
	}
	private String getPrintoutForNode(Node node, int iIndent){
		if( node == null ) return Utility_String.sRepeatChar('\t', iIndent) + "[null]\n";
		StringBuffer sbOut = new StringBuffer();
		sbOut.append(Utility_String.sRepeatChar('\t', iIndent));
		String sNodeTitle = node.getTitle();
	    if( sNodeTitle == null ) sNodeTitle = "[unnamed node]";
		sbOut.append(sNodeTitle + (node.isSelected() ? " *" : "") + '\n');
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			Node nodeChild = (Node)node.getChildAt(xChild);
			sbOut.append(getPrintoutForNode(nodeChild, iIndent+1));
		}
		return sbOut.toString();
	}
	void vClearNodeSelection(){
		Node nodeRoot = (Node)this.getRoot();
		vClearNodeSelection_ForNode( nodeRoot );
	}
	private void vClearNodeSelection_ForNode( Node node ){
		for( int xChild = 0; xChild < node.getChildCount(); xChild++ ){
			Node nodeChild = (Node)node.getChildAt(xChild);
			nodeChild.setSelected(false);
		}
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
		if( ! zAddNodeToStructure( this.getRootNode(), dds, sbError ) ){ // this call recursively adds the whole structure
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
			new_bt.setClearName( nodeCurrent.getName() );
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
							sbError.insert( 0, "error adding member " + nodeMember.getName() + " to structure: " );
							return false;
						}
					}
					break;
				case Grid:
					DGrid btGrid = (DGrid)new_bt;
					Node_Grid nodeGrid = (Node_Grid)nodeCurrent;
					Node_Array nodeValueArray = nodeGrid.arrayValues;
					DArray btValueArray = new DArray();
					btValueArray.getPrimitiveVector().setInternalStorage( nodeValueArray.getPrimitiveVector() );
					for( int xDimension = 1; xDimension <= nodeValueArray.listDimensions.size(); xDimension++ ){
						DArrayDimension dimension = nodeValueArray.listDimensions.get( xDimension - 1 );
						btValueArray.appendDim( dimension.getSize(), dimension.getClearName() );
						try {
							btValueArray.getDimension( xDimension - 1 ).setProjection( dimension.getStart(), dimension.getStride(), dimension.getStop() );
						} catch( Throwable t ){
							sbError.append( "error setting projection for DArray " + nodeCurrent.getName() + ": " + t );
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
					btArray.getPrimitiveVector().setInternalStorage( nodeArray.getPrimitiveVector() );
					for( int xDimension = 1; xDimension <= nodeArray.listDimensions.size(); xDimension++ ){
						DArrayDimension dimension = nodeArray.listDimensions.get( xDimension - 1 );
						btArray.appendDim( dimension.getSize(), dimension.getClearName() );
						try {
							btArray.getDimension( xDimension - 1 ).setProjection( dimension.getStart(), dimension.getStride(), dimension.getStop() );
						} catch( Throwable t ){
							sbError.append( "error setting projection for DArray " + nodeCurrent.getName() + ": " + t );
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





