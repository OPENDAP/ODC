package opendap.clients.odc.data;

import java.util.ArrayList;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.DAP.DAP_TYPE;
import opendap.clients.odc.DAP.DAP_VARIABLE;
import opendap.dap.BaseType;
import opendap.dap.DArray;
import opendap.dap.DArrayDimension;
import opendap.dap.PrimitiveVector;

import org.python.core.PyObject;

public class Node_Array extends Node {
	public transient Model_ArrayView _view;
	public static String DEFAULT_ArrayName = "array";	
	public static String DEFAULT_DimensionName = "dim";
	public static int DEFAULT_DimensionSize = 100;
	DArray darray;
	protected Node_Array( DArray bt ){
		super( bt );
		darray = bt;
		_view = new Model_ArrayView( this._getDimensionLengths1() );
	}
	public DAP_VARIABLE getType(){ return DAP.DAP_VARIABLE.Array; }
	DAP_TYPE eDataType;
	ArrayList<DArrayDimension> listDimensions = new ArrayList<DArrayDimension>();
	public PrimitiveVector _getPrimitiveVector(){ return darray.getPrimitiveVector(); }
	public Model_ArrayView _getView(){ return _view; }
	public int _getValueCount(){ return darray.getLength(); }
	public int _getByteSize(){ return darray.getLength() * DAP.getDataSize( _getValueType() ); }
	public int _getDimensionCount(){ return darray.numDimensions(); }
	public int _getDimensionLength( int xDimension1 ){
		try {
			return darray.getDimension( xDimension1 - 1 ).getSize(); 
		} catch( Throwable t ) {
			return 0;
		}
	}
	public int _getRowCount(){ // calculates row count according to view
		int[] aiDimensionLengths = _getDimensionLengths1();
		if( _view.getDimRow() == 0 || _view.getDimRow() > aiDimensionLengths[0] ) return 0; 
		return _getDimensionLengths1()[_view.getDimRow()];
	}
	public int _getColumnCount(){ // calculates row count according to view
		int[] aiDimensionLengths = _getDimensionLengths1();
		if( _view.getDimColumn() == 0 || _view.getDimColumn() > aiDimensionLengths[0] ) return 0;
		int column_count = aiDimensionLengths[_view.getDimColumn()];
		return column_count;
	}
	public String _getValueString_cursor(){
		return _getValueString( _view.cursor_row, _view.cursor_column );
	}
	public String _getValueString_selection(){
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
	public int _getSelectionSize(){
		return (_view.selectionLR_row - _view.selectionUL_row + 1) * (_view.selectionLR_column - _view.selectionUL_column + 1);  
	}
	public String _getValueString( int row, int column ){
		int xValue = _view.getIndex( row, column );
		opendap.dap.PrimitiveVector pv = _getPrimitiveVector();
		Object oValues = pv.getInternalStorage();
		int[] aiValues = (int[])oValues;
		return Integer.toString( aiValues[xValue] );
	}
	public Object _getValueObject( int index ){
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
		return _deleteValue( _view.getIndexCursor(), sbError );  
	}
	public boolean _deleteAllSelectedValues( StringBuffer sbError ){  // TODO process errors
		for( int xRow = _view.selectionUL_row; xRow <= _view.selectionLR_row; xRow++ ){ 
			for( int xColumn = _view.selectionUL_column; xColumn <= _view.selectionLR_column; xColumn++ ){
				_deleteValue( _view.getIndex( xRow, xColumn ), sbError );
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
		return _setValue( sNewValue, _view.getIndexCursor(), sbError );  
	}
	public boolean _setAllSelectedValues( String sNewValue, StringBuffer sbError ){  // TODO process errors
		for( int xRow = _view.selectionUL_row; xRow <= _view.selectionLR_row; xRow++ ){ 
			for( int xColumn = _view.selectionUL_column; xColumn <= _view.selectionLR_column; xColumn++ ){
				_setValue( sNewValue, _view.getIndex( xRow, xColumn ), sbError );
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
	public String _getValueTypeString(){
		return DAP.getDArrayType_String( darray );
	}
	public DAP_TYPE _getValueType(){
		return DAP.getDAPType( darray );
	}
	public String _getDimensionName( int xDimension1 ){
		try {
			return darray.getDimension( xDimension1 - 1 ).getName(); 
		} catch( Throwable t ) {
			return null;
		}
	}
	public boolean _addDimension( StringBuffer sbError ){
		return _addDimension( DEFAULT_DimensionSize, sbError );
	}
	public boolean _addDimension( int iDimensionSize, StringBuffer sbError ){
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
		_view.setDimLengths( _getDimensionLengths1() );
		return true;
	}
	public boolean _setDimensionName( int xDimension1, String sNewName, StringBuffer sbError ){
		try {
			DArrayDimension dim = darray.getDimension( xDimension1 - 1 );
			dim.setName( sNewName );
			return true;
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
	}
	public int[] _getDimensionLengths1(){
		int ctDimension = _getDimensionCount();
		int[] aiDimensionLengths = new int[ctDimension + 1];
		aiDimensionLengths[0] = ctDimension;
		for( int xDimension1 = 1; xDimension1 <= ctDimension; xDimension1++ ){
			aiDimensionLengths[xDimension1] = _getDimensionLength( xDimension1 );
		}
		return aiDimensionLengths;
	}
	public boolean _setDimensionSize( int xDimension1, int iNewSize, StringBuffer sbError ){
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
			_view.setDimLengths( _getDimensionLengths1() );
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

