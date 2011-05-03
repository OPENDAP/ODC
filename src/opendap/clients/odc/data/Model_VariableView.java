package opendap.clients.odc.data;

public class Model_VariableView {
	public int origin_row = 0;
	public int origin_column = 0;
	public int cursor_row = 0;
	public int cursor_column = 0;
	public int selectionUL_row = 0;
	public int selectionUL_column = 0;
	public int selectionLR_row = 0;
	public int selectionLR_column = 0;
	public int dim_row = 1;
	public int dim_column = 2;
	public int[] page = new int[10];
	public String sLastSelectedValue = null; // value of the last cell where Ctrl+arrow key was pressed
	
	// these constants are used to index values in the primitive vector
	// for example the array time[hours][minutes][seconds] has constants 60 and 60 and 1
	// if hours are rows and minutes are columns
	public int index_factor_row = 0;
	public int index_factor_column = 0;
	public int index_factor_remainder = 0; // this number is added at the end
}
