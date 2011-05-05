package opendap.clients.odc.data;

public class Model_ArrayView {
	public int origin_row = 0;
	public int origin_column = 0;
	public int cursor_row = 0;
	public int cursor_column = 0;
	public int selectionUL_row = 0;
	public int selectionUL_column = 0;
	public int selectionLR_row = 0;
	public int selectionLR_column = 0;
	public String sLastSelectedValue = null; // value of the last cell where Ctrl+arrow key was pressed
	private int dim_row = 1;
	private int dim_column = 2;
	private int[] page = new int[10];

	private Model_ArrayView(){}

	public Model_ArrayView( int[] dim_lengths1 ){
		setView( 1, 2, dim_lengths1 );
	}
	
	public int getDimRow(){ return dim_row; }
	public int getDimColumn(){ return dim_column; }
	
	public void setView( int dim_row, int dim_column, int[] dim_lengths1 ){
		this.dim_row = dim_row;
		this.dim_column = dim_column;
		update_index_factors( dim_lengths1 );
	}

	public void setView_page( int page_index1_9, int value, int[] dim_lengths1 ){
		page[page_index1_9] = value;
		update_index_factors( dim_lengths1 );
	}
	public void setDimLengths( int[] dim_lengths1 ){
		update_index_factors( dim_lengths1 );
	}
	
	// these constants are used to index values in a primitive vector
	// for example the array time[hours][minutes][seconds] has constants 60 and 60 and 1
	// if hours are rows and minutes are columns
	private int index_factor_row = 0;
	private int index_factor_column = 0;
	private int index_factor_remainder = 0; // this number is added at the end
	public int getIndex( int row, int column ){
//System.out.println( "index factor row: " + index_factor_row + " column: " + index_factor_column );  		
		return index_factor_row * row + index_factor_column * column + index_factor_remainder;  
	}
	public int getIndexCursor(){
		return getIndex( cursor_row, cursor_column );  
	}
	private void update_index_factors( int[] dim_lengths1 ){
		int iFactor = 1;
		index_factor_remainder = 0;
		for( int xDim = dim_lengths1[0]; xDim > 0; xDim-- ){
			if( xDim == dim_row ){
				index_factor_row = iFactor;
			} else if( xDim == dim_column ){
				index_factor_column = iFactor;
			} else {
				index_factor_remainder += page[xDim] * iFactor;
			}
			iFactor *= dim_lengths1[xDim]; 
		}
	}

// old value index code
//	private final int[] aiDimSelector = new int[10];
//	private final int[] aiDimSize = new int[10];
//	public int _getValueIndex_Cursor(){  // returns the value index of the view cursor
//		int xDimRow = _view.dim_row;
//		int xDimColumn = _view.dim_column;
//		aiDimSelector[0] = _getDimensionCount();
//		for( int xDimension = 1; xDimension <= aiDimSelector[0]; xDimension++ ){
//			if( xDimension == xDimRow ){
//				aiDimSelector[xDimension] = _view.cursor_row;
//			} else if( xDimension == xDimColumn ){
//				aiDimSelector[xDimension] = _view.cursor_column;
//			} else {
//				aiDimSelector[xDimension] = _view.page[xDimension];
//			}
//		}
//		return _getValueIndex( aiDimSelector );  
//	}
//	int _getValueIndex( int[] axDim1 ){ // calculates index of value according to exact set of dimensional indices
//		int index = 0;
//		int[] aiDimLengths1 = _getDimensionLengths1();
//		int ctDimensions = aiDimSelector[0];
//		for( int xDimension = 1; xDimension <= ctDimensions ; xDimension++ )
//			index += java.lang.Math.pow( aiDimLengths1[xDimension], xDimension - 1 ) * axDim1[ctDimensions - xDimension + 1];
//		return index;
//	}
	
}
