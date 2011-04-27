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
}
