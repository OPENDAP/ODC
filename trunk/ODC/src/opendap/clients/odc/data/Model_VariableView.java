package opendap.clients.odc.data;

public class Model_VariableView {
	int origin_row = 0;
	int origin_column = 0;
	int cursor_row = 0;
	int cursor_column = 0;
	int selectionUL_row = 0;
	int selectionUL_column = 0;
	int selectionLR_row = 0;
	int selectionLR_column = 0;
	int dim_row = 1;
	int dim_column = 2;
	int[] page = new int[10];
	String sLastSelectedValue = null; // value of the last cell where Ctrl+arrow key was pressed
}
