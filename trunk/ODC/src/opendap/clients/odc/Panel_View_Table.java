package opendap.clients.odc;

/**
 * Title:        Panel_View_Table
 * Description:  Output table grid which is used to display data in matrix form
 * Copyright:    Copyright (c) 2002-4
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.41
 */

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import opendap.dap.*;

public class Panel_View_Table extends JPanel {

    public Panel_View_Table() {}

	private final JScrollPane jspDisplay = new JScrollPane();
	private JTable mjtDisplay;
	public Model_DataTable theModel;

	private String[][] maData0 = new String[1][1];
	private int mCurrentRow = 0; // always next one past existing info
	private final JTextField mjtfCount_Rows = new JTextField(4);
	private final JTextField mjtfCount_Cols = new JTextField(4);

	public String[] getColumn1( int iColumn, int iFromRow, int iToRow, StringBuffer sbError ){
		if( iFromRow < 1 || iFromRow > maData0.length ){
			sbError.append("invalid row-from-coordinate [" + iFromRow + "]; must be between 1 and " + maData0.length);
			return null;
		}
		if( iToRow < 1 || iToRow > maData0.length ){
			sbError.append("invalid row-to-coordinate [" + iToRow + "]; must be between 1 and " + maData0.length);
			return null;
		}
		if( iColumn < 1 || iColumn > maData0[iFromRow].length ){
			sbError.append("invalid column-from-coordinate [" + iColumn + "]; must be between 1 and " + maData0[iFromRow].length);
			return null;
		}
		int iStep = iFromRow < iToRow ? 1 : -1;
		int iArraySize = (iToRow - iFromRow) * iStep + 1;
		String[] asResultVector = new String[iArraySize + 1]; // one-based
		int xResultVector = 0;
		for( int xRow = iFromRow; xRow <= iToRow; xRow += iStep ){
			xResultVector++;
			asResultVector[xResultVector] = maData0[xRow-1][iColumn-1];
		}
		return asResultVector;
	}

	/** one-based */
	public String[][] getColumnMatrix0( int iColumn, int iFromRow, int iToRow, StringBuffer sbError ){
		if( iFromRow < 1 || iFromRow > maData0.length ){
			sbError.append("invalid row-from-coordinate [" + iFromRow + "]; must be between 1 and " + maData0.length);
			return null;
		}
		if( iToRow < 1 || iToRow > maData0.length ){
			sbError.append("invalid row-to-coordinate [" + iToRow + "]; must be between 1 and " + maData0.length);
			return null;
		}
		if( iColumn < 1 || iColumn > maData0[iFromRow].length ){
			sbError.append("invalid column-from-coordinate [" + iColumn + "]; must be between 1 and " + maData0[iFromRow].length);
			return null;
		}
		int iStep = iFromRow < iToRow ? 1 : -1;
		int iArraySize = (iToRow - iFromRow) * iStep + 1;
		String[][] asResultVector = new String[1][iArraySize];
		int xResultVector = -1;
		for( int xRow = iFromRow; xRow <= iToRow; xRow += iStep ){
			xResultVector++;
			asResultVector[0][xResultVector] = maData0[xRow-1][iColumn-1];
		}
		return asResultVector;
	}

	public String[] getRow1( int iRow, int iFromColumn, int iToColumn, StringBuffer sbError ){
		if( iRow < 1 || iRow > maData0.length ){
			sbError.append("invalid row coordinate [" + iRow + "]; must be between 1 and " + maData0.length);
			return null;
		}
		if( iFromColumn < 1 || iFromColumn > maData0[iRow].length ){
			sbError.append("invalid from-column coordinate [" + iFromColumn + "]; must be between 1 and " + maData0[iRow].length);
			return null;
		}
		if( iToColumn < 1 || iToColumn > maData0[iRow].length ){
			sbError.append("invalid to-column coordinate [" + iToColumn + "]; must be between 1 and " + maData0[iRow].length);
			return null;
		}
		int iStep = iFromColumn < iToColumn ? 1 : -1;
		int iArraySize = (iToColumn - iFromColumn) * iStep + 1;
		String[] asResultVector = new String[iArraySize + 1]; // one-based
		int xResultVector = 0;
		for( int xColumn = iFromColumn; xColumn <= iToColumn; xColumn += iStep ){
			xResultVector++;
			asResultVector[xResultVector] = maData0[iRow-1][xColumn-1];
		}
		return asResultVector;
	}

	public String[][] getRowMatrix0( int iRow, int iFromColumn, int iToColumn, StringBuffer sbError ){
		if( iRow < 1 || iRow > maData0.length ){
			sbError.append("invalid row coordinate [" + iRow + "]; must be between 1 and " + maData0.length);
			return null;
		}
		if( iFromColumn < 1 || iFromColumn > maData0[iRow].length ){
			sbError.append("invalid from-column coordinate [" + iFromColumn + "]; must be between 1 and " + maData0[iRow].length);
			return null;
		}
		if( iToColumn < 1 || iToColumn > maData0[iRow].length ){
			sbError.append("invalid to-column coordinate [" + iToColumn + "]; must be between 1 and " + maData0[iRow].length);
			return null;
		}
		int iStep = iFromColumn < iToColumn ? 1 : -1;
		int iArraySize = (iToColumn - iFromColumn) * iStep + 1;
		String[][] asResultVector = new String[iArraySize][1];
		int xResultVector = -1;
		for( int xColumn = iFromColumn; xColumn <= iToColumn; xColumn += iStep ){
			xResultVector++;
			asResultVector[xResultVector][0] = maData0[iRow-1][xColumn-1];
		}
		return asResultVector;
	}

	/** flattens range */
	public String[] getRange0_XY( int iFromRow, int iToRow, int iFromColumn, int iToColumn, StringBuffer sbError ){
		if( !zValidateRange(iFromRow, iToRow, iFromColumn, iToColumn, sbError) ){
			sbError.insert(0, "invalid range: ");
			return null;
		}
		int iRowStep = iFromRow < iToRow ? 1 : -1;
		int iColumnStep = iFromColumn < iToColumn ? 1 : -1;
		int iArraySize = ((iToColumn - iFromColumn) * iColumnStep + 1) * ((iToRow - iFromRow) * iRowStep + 1);
		String[] asResultVector = new String[iArraySize]; // zero-based
		int xResultVector = -1;
		for( int xRow = iFromRow; xRow <= iToRow; xRow += iRowStep ){
			for( int xColumn = iFromColumn; xColumn <= iToColumn; xColumn += iColumnStep ){
				xResultVector++;
				asResultVector[xResultVector] = maData0[xRow - 1][xColumn - 1];
			}
		}
		return asResultVector;
	}

	public String[][] getRangeMatrix0_XY( int iFromRow, int iToRow, int iFromColumn, int iToColumn, StringBuffer sbError ){
		if( !zValidateRange(iFromRow, iToRow, iFromColumn, iToColumn, sbError) ){
			sbError.insert(0, "invalid range: ");
			return null;
		}
		int iRowStep = iFromRow < iToRow ? 1 : -1;
		int iColumnStep = iFromColumn < iToColumn ? 1 : -1;
		int iArraySize = ((iToColumn - iFromColumn) * iColumnStep + 1) * ((iToRow - iFromRow) * iRowStep + 1);
		int iRowSize = (iToRow - iFromRow)*iRowStep + 1;
		int iColumnSize = (iToColumn - iFromColumn)*iColumnStep + 1;
		String[][] asResult = new String[iColumnSize][iRowSize]; // zero-based
		int xRowResult = -1;
		for( int xRow = iFromRow; xRow <= iToRow; xRow += iRowStep ){
			xRowResult++;
			int xColumnResult = -1;
			for( int xColumn = iFromColumn; xColumn <= iToColumn; xColumn += iColumnStep ){
				xColumnResult++;
				asResult[xColumnResult][xRowResult] = maData0[xRow - 1][xColumn - 1];
			}
		}
		return asResult;
	}

	public String[] getRange0_YX( int iFromRow, int iToRow, int iFromColumn, int iToColumn, StringBuffer sbError ){
		if( !zValidateRange(iFromRow, iToRow, iFromColumn, iToColumn, sbError) ){
			sbError.insert(0, "invalid range: ");
			return null;
		}
		int iRowStep = iFromRow < iToRow ? 1 : -1;
		int iColumnStep = iFromColumn < iToColumn ? 1 : -1;
		int iArraySize = ((iToColumn - iFromColumn) * iColumnStep + 1) * ((iToRow - iFromRow) * iRowStep + 1);
		String[] asResultVector = new String[iArraySize]; // zero-based
		int xResultVector = -1;
		for( int xColumn = iFromColumn; xColumn <= iToColumn; xColumn += iColumnStep ){
			for( int xRow = iFromRow; xRow <= iToRow; xRow += iRowStep ){
				xResultVector++;
				asResultVector[xResultVector] = maData0[xRow - 1][xColumn - 1];
			}
		}
		return asResultVector;
	}

	public boolean zValidateRange( int iFromRow, int iToRow, int iFromColumn, int iToColumn, StringBuffer sbError ){
		if( iFromRow < 1 || iFromRow > maData0.length ){
			sbError.append("from-row coordinate [" + iFromRow + "]; must be between 1 and " + maData0.length);
			return false;
		}
		if( iToRow < 1 || iToRow > maData0.length ){
			sbError.append("to-row coordinate [" + iToRow + "]; must be between 1 and " + maData0.length);
			return false;
		}
		if( iFromColumn < 1 || iFromColumn > maData0[iFromRow].length ){
			sbError.append("from-column coordinate [" + iFromColumn + "]; must be between 1 and " + maData0[iFromRow].length);
			return false;
		}
		if( iToColumn < 1 || iToColumn > maData0[iFromRow].length ){
			sbError.append("to-column coordinate [" + iToColumn + "]; must be between 1 and " + maData0[iFromRow].length);
			return false;
		}
		return true;
	}

	public boolean zInitialize(StringBuffer sbError){

        try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			this.setBorder(borderStandard);

			this.setLayout(new java.awt.BorderLayout());

			// Create and intialize the command panel
			JPanel jpanelCommandButtons = new JPanel();

			// Clear Button
			JButton jbuttonClearDisplay = new JButton("Clear Display");
			jbuttonClearDisplay.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					Panel_View_Table.this.vClearDisplay();
					}
				}
			);
			jpanelCommandButtons.add(jbuttonClearDisplay);

			// Load File Button
			JButton jbuttonLoadFromFile = new JButton("Load from File");
			jbuttonLoadFromFile.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					Panel_View_Table.this.vLoadFromFile();
					}
				}
			);
			jpanelCommandButtons.add(jbuttonLoadFromFile);

			// Save File Button
			JButton jbuttonSaveToFile = new JButton("Save to File");
			jbuttonSaveToFile.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
	    				Panel_View_Table.this.vSaveToFile();
		    		}
			    }
			);
			jpanelCommandButtons.add(jbuttonSaveToFile);

			// Save File Button
			JButton jbuttonToPlotter = new JButton("To Plotter");
			jbuttonToPlotter.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						StringBuffer sbError = new StringBuffer(80);
	    				if( Panel_View_Table.this.zToPlotter(sbError) ){
							ApplicationController.vShowStatus("Sent table contents to plotter");
						} else {
							ApplicationController.vShowError("Error sending table data to plotter: " + sbError);
						}
		    		}
			    }
			);
			jpanelCommandButtons.add(jbuttonToPlotter);

			// Setup Info
			JPanel jpanelInfo = new JPanel();
			JLabel labelRows = new JLabel("Rows: ");
			JLabel labelCols = new JLabel("  Cols: ");
			jpanelInfo.setLayout(new BoxLayout(jpanelInfo, BoxLayout.X_AXIS));
			jpanelInfo.add(labelRows);
			jpanelInfo.add(mjtfCount_Rows);
			jpanelInfo.add(labelCols);
			jpanelInfo.add(mjtfCount_Cols);
			mjtfCount_Rows.setMaximumSize(new Dimension(60, 20));
			mjtfCount_Cols.setMaximumSize(new Dimension(60, 20));
			KeyListener listenEnter_Dimensions =
				new java.awt.event.KeyListener(){
					public void keyPressed(java.awt.event.KeyEvent ke){
						if( ke.getKeyCode() == ke.VK_ENTER ){
							vUpdateDataFromDimensions();
						}
					}
					public void keyReleased(java.awt.event.KeyEvent ke){}
					public void keyTyped(java.awt.event.KeyEvent ke){}
				};
			FocusAdapter focus_Dimensions =
				new FocusAdapter(){
					public void focusLost(FocusEvent evt) {
						vUpdateDataFromDimensions();
					}
				};
			mjtfCount_Rows.addKeyListener(listenEnter_Dimensions);
			mjtfCount_Cols.addKeyListener(listenEnter_Dimensions);
			mjtfCount_Rows.addFocusListener(focus_Dimensions);
			mjtfCount_Cols.addFocusListener(focus_Dimensions);

			// Setup Table
			final int iROW_HEIGHT = 20;
			final int iCOLUMN_WIDTH = 100;
			mjtDisplay = new JTable();
			mjtDisplay.setRowSelectionAllowed(true);
			mjtDisplay.setShowGrid(true);
			mjtDisplay.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			mjtDisplay.setRowHeight(iROW_HEIGHT);

			// establish model
			theModel = new Model_DataTable();
			mjtDisplay.setModel(theModel);

			// define row headers
			ListModel lm = new AbstractListModel() {
				public int getSize() { return theModel.getRowCount(); }
				public Object getElementAt(int index) {
					return Integer.toString(index + 1);
				}
			};
			JList rowHeader = new JList(lm);
			rowHeader.setFixedCellWidth(50);
			rowHeader.setFixedCellHeight(iROW_HEIGHT);
//                             + mjtDisplay.getRowMargin());
//                             + mjtDisplay.getIntercellSpacing().height);
			rowHeader.setCellRenderer(new RowHeaderRenderer(mjtDisplay));

			// wrap table in scroll pane
			JScrollPane jscrollpaneTable = new JScrollPane();
			jscrollpaneTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			jscrollpaneTable.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			jscrollpaneTable.setViewportView(mjtDisplay);
			jscrollpaneTable.setRowHeaderView(rowHeader);

			JPanel panelSouth = new JPanel();
			panelSouth.add(Box.createHorizontalGlue());
			panelSouth.setLayout(new BoxLayout(panelSouth, BoxLayout.X_AXIS));
			panelSouth.add(jpanelCommandButtons);
			panelSouth.add(Box.createHorizontalStrut(15));
			panelSouth.add(jpanelInfo);
			panelSouth.add(Box.createHorizontalGlue());

			this.add(jscrollpaneTable, BorderLayout.CENTER);
			this.add(panelSouth, BorderLayout.SOUTH);

			vUpdateDimensionsFromData();

            return true;

        } catch(Exception ex){
			Utility.vUnexpectedError(ex, sbError);
            return false;
        }
	}

	void vUpdateDataFromDimensions(){
		int ctCurrentRows = maData0 == null ? 0 : maData0.length;
		int ctCurrentCols = maData0 == null ? 0 : maData0[ctCurrentRows - 1].length;
		String sRowCount = mjtfCount_Rows.getText();
		String sColCount = mjtfCount_Cols.getText();
		int iRowCount, iColCount;
		try {
			iRowCount = Integer.parseInt(sRowCount);
		} catch(Exception ex) {
			iRowCount = ctCurrentRows;
		}
		try {
			iColCount = Integer.parseInt(sColCount);
		} catch(Exception ex) {
			iColCount = ctCurrentCols;
		}
		if( iRowCount == ctCurrentRows && iColCount == ctCurrentCols ) return; // no changes
		String[][] maData0_buffer = new String[iRowCount][iColCount];
		int xRow = 0, xCol = 0;
		for( xRow = 0; xRow < iRowCount && xRow < ctCurrentRows; xRow++ )
			for( xCol = 0; xCol < iColCount && xCol < ctCurrentCols; xCol++ )
			    maData0_buffer[xRow][xCol] = maData0[xRow][xCol];
	    maData0 = maData0_buffer;
		theModel.fireTableStructureChanged();
		vUpdateDimensionsFromData();
	}

	void vUpdateDimensionsFromData(){
		if( maData0 == null ){
			mjtfCount_Rows.setText("0");
			mjtfCount_Cols.setText("0");
		} else {
			mjtfCount_Rows.setText(Integer.toString(maData0.length));
			mjtfCount_Cols.setText(Integer.toString(maData0[0].length));
		}
	}

	void vClearDisplay(){
		maData0 = null;
		System.gc();
		maData0 = new String[1][1];
		vUpdateDimensionsFromData();
		mCurrentRow = 0; // always next one past existing info
		theModel.fireTableStructureChanged();
	}

	void vSetData(int[][] data){
		if( data == null ) return;
		int iRowCount = data.length;
		int iColCount = data[0].length;
		maData0 = new String[iRowCount][iColCount];
		int xRow = 0, xCol = 0;
		for( xRow = 0; xRow < iRowCount; xRow++ )
			for( xCol = 0; xCol < iColCount; xCol++ )
			    maData0[xRow][xCol] = Integer.toString(data[xRow][xCol]);
		theModel.fireTableStructureChanged();
		vUpdateDimensionsFromData();
	}

	void vLoadFromFile(){}
	void vSaveToFile(){}

	boolean zToPlotter(StringBuffer sbError){
		if( maData0 == null || maData0.length < 1 || maData0[0].length < 1 ){
			sbError.append("no data in table");
			return false;
		}
		String sCorner = maData0[0][0];
		String sX = maData0[0][1];
		String sY = maData0[1][0];
		String[] asFieldNames1 = null;
		int eTYPE = DAP.DATA_TYPE_Float64; // double is the default
		int originRow, originCol;
		String sDimensionX = null;
		String sDimensionY = null;
		int ctRows = 0, ctCols = 0;
		boolean zSequence = false;
		originRow = 0;
		originCol = 0;
		sX = "x-dimension";
		sY = "y-dimension";
		ctRows = maData0.length - originRow;
		ctCols = maData0[0].length - originCol;

		// create dataset
		String sArrayName = "table " + ctRows + " x " + ctCols;
		DArray darray = new DArray(sArrayName);
		if( ctRows == 1 ){
			darray.appendDim(ctCols, sX);
		} else if( ctCols == 1 ){
			darray.appendDim(ctRows, sY);
		} else {
			darray.appendDim(ctCols, sX);
			darray.appendDim(ctRows, sY);
		}
		int eJAVA_TYPE;
		BaseType template;
		String sName = "array";
		switch( eTYPE ){
			case DAP.DATA_TYPE_Byte:
				eJAVA_TYPE = DAP.JAVA_TYPE_short;
				template = new DByte(sName);
				break;
			case DAP.DATA_TYPE_Int16:
				eJAVA_TYPE = DAP.JAVA_TYPE_short;
				template = new DInt16(sName);
				break;
			case DAP.DATA_TYPE_Int32:
				eJAVA_TYPE = DAP.JAVA_TYPE_int;
				template = new DInt32(sName);
				break;
			case DAP.DATA_TYPE_UInt16:
				eJAVA_TYPE = DAP.JAVA_TYPE_int;
				template = new DUInt16(sName);
				break;
			case DAP.DATA_TYPE_UInt32:
				eJAVA_TYPE = DAP.JAVA_TYPE_long;
				template = new DUInt32(sName);
				break;
			case DAP.DATA_TYPE_Float32:
				eJAVA_TYPE = DAP.JAVA_TYPE_float;
				template = new DFloat32(sName);
				break;
			case DAP.DATA_TYPE_Float64:
				eJAVA_TYPE = DAP.JAVA_TYPE_double;
				template = new DFloat64(sName);
				break;
			default:
				eJAVA_TYPE = DAP.JAVA_TYPE_double;
				template = new DFloat64(sName);
				break;
		}
		darray.addVariable(template);
		String[] as = new String[ ctRows * ctCols ];
		int xValue = -1;
		for( int xRow = originRow; xRow < ctRows; xRow++ )
			for( int xCol = originCol; xCol < ctCols; xCol++ )
			    as[++xValue] = maData0[xRow][xCol];
		Object[] egg = DAP.convertToEgg_Java(as, eJAVA_TYPE, sbError);
		if( egg == null ){
			sbError.insert(0, "error converting values: ");
			return false;
		}
		PrimitiveVector pv = darray.getPrimitiveVector();
		pv.setInternalStorage(egg[0]);

		// send to plotter
		DataDDS ddds = new DataDDS(null, null);
		ddds.addVariable(darray);
		final DodsURL url = new DodsURL();
		url.setTitle("[table data]");
		url.setData(ddds);
		javax.swing.SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					StringBuffer sbAddError = new StringBuffer(80);
					if( ApplicationController.getInstance().getAppFrame().zAddDataToPlotter_Invoked( url, sbAddError ) ){
					   // ok
					} else {
						ApplicationController.vShowError("Failed to add data URL to plotter: " + sbAddError);
					}
				}
			}
		);

		return true;
	}

	OutputStream getOutputStream( StringBuffer sbError ){
		try {
			return new TableOutputStream();
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return null;
		}
	}

	class TableOutputStream extends OutputStream {
		final static int NOMINAL_PROCESS_PACKET_SIZE = 1000;
		boolean zMaxedOut = false;
		int MAX_Rows = 10000;
		StringBuffer msbBuffer = new StringBuffer(NOMINAL_PROCESS_PACKET_SIZE);
		ArrayList listValues = new ArrayList();
		StringBuffer msbValBuffer = new StringBuffer();
		public TableOutputStream(){
			MAX_Rows = ConfigurationManager.getInstance().getProperty_MaxTableRows();
		}
		int eState = 1;
		int eLastState = -1;
		public void write(int iByte) throws IOException {
			// not used because printing to table should always send arrays
		}
		public void write(byte[] buffer) throws IOException {
			write(buffer, 0, buffer.length);
		}
		public void write(byte[] abWriteBuffer, int offset, int count) throws IOException { // loads the buffer
			if( zMaxedOut ) return; // no more output
			String sBuffer = new String(abWriteBuffer, offset, count);
			msbBuffer.append(sBuffer);
			try {
				vProcessBuffer();
			} catch(TableFormatException ex) {
				throw new java.io.WriteAbortedException("Table format problem", ex);
			}
		}
		private void vProcessBuffer() throws TableFormatException {
			if( msbBuffer.length() ==  0 ) return; // nothing to process
			if( maData0.length >= MAX_Rows ){
				ApplicationController.vShowStatus("Table view limit of " + MAX_Rows + " rows exceeded. Remaining output omitted.");
				zMaxedOut = true;
				msbBuffer.setLength(0);
				return;
			}
			int posBuffer = 0;
			int posLast = -1;
			while(true){
				char c = msbBuffer.charAt(posBuffer);
				if( eState == eLastState && posBuffer == posLast ){
					vTerminateWithError("Internal error, infinite loop in state machine, " + "state: " + eState + " char: '" + c + "'");
				}
				eLastState = eState;
				posLast = posBuffer;
				// System.out.println("state: " + eState + " char: '" + c + "'");
				switch(eState){
					case 1: // looking for header
						if(Character.isWhitespace(c)){
							// keep looking
						} else if(c == '{'){
							eState = 2; // in header
						} else {
							vTerminateWithError("Invalid character (" + c + " [" + (int)c + "]) encountered while looking for header. Expected '{'. " + Utility.sSafeSubstring(msbBuffer, posBuffer-12, 24));
						}
						posBuffer++;
						break;
					case 2: // inside header // outside value
						if(Character.isWhitespace(c)){
							posBuffer++; // keep looking
						} else if( c == '"' ){
							posBuffer++;
							eState = 4; // after quote
						} else if( c == '}' ) {
							eState = 6; // outside header
							posBuffer++;
						} else {
							eState = 3; // in space-delimited value
						}
						break;
					case 3: // in space-delimited value in header
						if(Character.isWhitespace(c)){
							vAddValue();
							eState = 2;
						} else if( c == '}' ) { // reached end of header
							vAddValue();
							eState = 6;
						} else {
							msbValBuffer.append(c);
						}
						posBuffer++;
						break;
					case 4: // after an initial quote in header
						if( c == '"' ){
							eState = 5;
						} else {
							msbValBuffer.append(c);
						}
						posBuffer++;
						break;
					case 5: // after internal quote in header
						if( c == '"' ){
							msbValBuffer.append(c); // escaped quote
							eState = 4;
						} else if(Character.isWhitespace(c)){
							vAddValue();
							eState = 2;
						} else if(c=='}'){
							vAddValue();
							eState = 6;
						} else {
							vTerminateWithError("Unexpected character after quote in header (" + c + " [" + (int)c + "]). Must be whitespace, quote or closing brace. " + Utility.sSafeSubstring(msbBuffer, posBuffer-12, 24));
						}
						posBuffer++;
						break;
					case 6: // after end of header
						vProcessHeader();
						eState = 7;
						break;
					case 7: // looking for start of record
						if(c == '{'){
							eState = 8; // in record
						} else {
							// keep looking
						}
						posBuffer++;
						break;
					case 8: // start of record
						if(Character.isWhitespace(c)){ // keep looking
							posBuffer++;
						} else if(c == '/'){ // this is the terminal record
							eState = 14;
						} else if(c == '}'){ // record is empty
							mCurrentRow++;
							eState = 7;
							posBuffer++;
						} else {
							eState = 9;
						}
						break;
					case 9: // inside record, outside value
						if(Character.isWhitespace(c)){
							posBuffer++; // keep looking
						} else if( c == '"' ){
							posBuffer++;
							eState = 11; // after quote
						} else if( c == '}' ) {
							eState = 13; // after record
							posBuffer++;
						} else {
							eState = 10; // in comma-delimited or space-delimited value
						}
						break;
					case 10: // in comma/space-delimited value
						if(Character.isWhitespace(c)){
							vAddValue();
							eState = 9;
						} else if( c == ',' ) {
							vAddValue();
							eState = 9;
						} else if( c == '}' ) { // reached end of record
							vAddValue();
							eState = 13;
						} else {
							msbValBuffer.append(c);
						}
						posBuffer++;
						break;
					case 11: // after initial quote in quoted value
						if( c == '"' ){
							eState = 12;
						} else {
							msbValBuffer.append(c);
						}
						posBuffer++;
						break;
					case 12: // after internal quote in quoted value
						if( c == '"' ){
							msbValBuffer.append(c); // escaped quote
							eState = 11;
						} else if(c==',' || Character.isWhitespace(c)){
							vAddValue();
							eState = 9;
						} else if(c=='}'){
							vAddValue();
							eState = 13;
						} else {
							vTerminateWithError("Unexpected character after quote in record (" + c + " [" + (int)c + "]). Must be quote, comma, space or closing brace. " + Utility.sSafeSubstring(msbBuffer, posBuffer-12, 24));
						}
						posBuffer++;
						break;
					case 13: // after record
						vProcessRecord();
						eState = 7;
						break;
					case 14: // in terminal record
						if( c == '}' ){
							eState = 1;
						} else {
							// keep looking
						}
						posBuffer++;
						break;
				}
				if( posBuffer >= msbBuffer.length() ) break;
			}
			msbBuffer.setLength(0);
		}
		private void vAddValue(){
			listValues.add(msbValBuffer.toString());
			msbValBuffer.setLength(0);
		}
		// header format:
		//   "table name"
		//   RowCount: iRowCount
		//   ColumnCount: iFieldCount
		//   DimColumns: "dim 1" "dim 2" ... "dim n-1"
		//   ValColumns: "val" 1 3 5 7 ... n
		//   ColumnTypes: String Int32 ...}
		private void vProcessHeader() throws TableFormatException {
			Iterator iterator = listValues.iterator();
			if( !iterator.hasNext() ){ vTerminateWithError("incomplete header, no table name"); }
			String sTableName = (String)iterator.next();
			if( !iterator.hasNext() ){ vTerminateWithError("incomplete header, no row count label"); }
			String sRowCountLabel = (String)iterator.next();
			if( !sRowCountLabel.equals("RowCount:") ){ vTerminateWithError("incomplete header, invalid row count label (" + sRowCountLabel + ")"); }
			if( !iterator.hasNext() ){ vTerminateWithError("incomplete header, no row count"); }
			String sRowCount = (String)iterator.next();
			if( !Utility.isInteger(sRowCount) ){ vTerminateWithError("invalid header, row count is not an integer (" + sRowCount + ")"); }
			int iDataRowCount = Integer.parseInt(sRowCount);
			int iRowCount = iDataRowCount + 1; // need extra row for header
			if( !iterator.hasNext() ){ vTerminateWithError("incomplete header, no column count label"); }
			String sColumnCountLabel = (String)iterator.next();
			if( !sColumnCountLabel.equals("ColumnCount:") ){ vTerminateWithError("incomplete header, invalid column count label (" + sColumnCountLabel + ")"); }
			if( !iterator.hasNext() ){ vTerminateWithError("incomplete header, no column count"); }
			String sColumnCount = (String)iterator.next();
			if( !Utility.isInteger(sColumnCount) ){ vTerminateWithError("invalid header, column count is not an integer (" + sColumnCount + ")"); }
			int iColumnCount = Integer.parseInt(sColumnCount);
			int iPreviousRowCount = maData0.length;
			int iPreviousColumnCount = maData0[0].length;
			if( iRowCount + iPreviousRowCount > MAX_Rows ) iRowCount = MAX_Rows - iPreviousRowCount;
			if( iRowCount < 1 ){ vTerminateWithError("unable to continue, table is full, maximum rows are (" + MAX_Rows + ")"); }
			if( iColumnCount > MAX_Rows ){ vTerminateWithError("unable to continue, excessive column count (" + iColumnCount + ")"); }
			if( iPreviousColumnCount > iColumnCount ) iColumnCount = iPreviousColumnCount;
			try { // expand data array to accomodate the new data
				String[][] asNew = new String[iRowCount + iPreviousRowCount][iColumnCount]; // an extra row is included for the headings
				for( int xArray = 0; xArray < maData0.length; xArray++ ){
					System.arraycopy(maData0[xArray], 0, asNew[xArray], 0, maData0[xArray].length);
				}
				maData0 = asNew;
				vUpdateDimensionsFromData();
			} catch(Exception ex) {
				vTerminateWithError("unable to allocated memory for table: " + ex);
			}
			int xColumnLabel = 0;
			int eState = 0;
			String sValColumnName = "";
			while( iterator.hasNext() ){ // put the dim columns and val columns across the top
				String sValue = (String)iterator.next();
				if( sValue.equals("DimColumns:") ){ eState = 1; continue; }
				if( sValue.equals("ValColumns:") ){ eState = 2; continue; }
				if( sValue.equals("ColumnTypes:") ){ eState = 4; continue; }
				if( eState == 1 || eState == 3 ){
						maData0[mCurrentRow][xColumnLabel] = sValue;
						xColumnLabel++;
						if( xColumnLabel >= maData0[mCurrentRow].length ) break;
				} else if( eState == 2 ){
					sValColumnName = sValue;
					eState = 3;
				}
			}
			mCurrentRow++; // advance past label row
			listValues.clear();
			// all done, ready to add records
			theModel.fireTableStructureChanged();
		}
		private void vProcessRecord(){
			if( mCurrentRow >= maData0.length ){
				listValues.clear();
				return;
			}
			if( mCurrentRow % 100 == 0 ){
				ApplicationController.vShowStatus_NoCache("Loading row " + mCurrentRow);
				theModel.fireTableStructureChanged();
				try {
					this.wait(40);
				} catch(Exception ex) {}
			}
			int xCurrentColumn = 0;
			int iMaxColumns = maData0[mCurrentRow].length;
			Iterator iterator = listValues.iterator();
			while(iterator.hasNext() && xCurrentColumn < iMaxColumns){
				maData0[mCurrentRow][xCurrentColumn] = (String)iterator.next();
				xCurrentColumn++;
			}
			mCurrentRow++;
			listValues.clear();
		}
		private void vCheckTermination(){
			// todo, should validate that the output is complete
		}
		private void vTerminateWithError(String sError) throws TableFormatException {
			// todo add position // buffer content // debugging:
//			System.out.println(msbBuffer.subSequence(0, 1000));
//			Iterator iter = listValues.iterator();
//			System.out.println("values: ");
//			while(iter.hasNext()){
//				System.out.println(iter.next());
//			}
			ApplicationController.getInstance().vShowError(sError);
			throw new TableFormatException(sError);
		}
		public void close(){
			flush();
			vCheckTermination();
		}
		public void flush(){
			try {
				vProcessBuffer();
			} catch(TableFormatException ex) {
				// throw away
			}
		}
	}

	public class TableFormatException extends Exception {
		public TableFormatException( String sMessage ){
			super(sMessage);
		}
	}

	public class Model_DataTable extends javax.swing.table.AbstractTableModel {
		public int getRowCount(){
			return maData0.length;
		}
		public int getColumnCount(){
			return maData0[0].length;
		}
		public String getColumnName( int col0 ){
			int col = col0 + 1; // make 1-based
			if( col < 27 ){
				char[] ac = new char[1];
				ac[0] = (char)('A' + col - 1);
				return new String(ac);
			}
			if( col < 677 ){
				char[] ac = new char[2];
				ac[0] = (char)('A' + (col0 - col0%26)/26 - 1);
				ac[1] = (char)('A' + col0%26);
				return new String(ac);
			}
			if( col < 17577 ){
				char[] ac = new char[3];
				ac[0] = (char)('A' + (col - col%676)/676 - 1);
				ac[1] = (char)('A' + (col - col%26)/26 - 1);
				ac[2] = (char)('A' + col%26 - 1);
				return new String(ac);
			}
			return "?";
		}
		public Object getValueAt( int row, int col ){
			return maData0[row][col];
		}
		public boolean isCellEditable(int row, int col){
			return true;
		}
	}

}

class RowHeaderRenderer extends JLabel implements ListCellRenderer {

	RowHeaderRenderer(JTable table) {
		javax.swing.table.JTableHeader header = table.getTableHeader();
		setOpaque(true);
		setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		setHorizontalAlignment(CENTER);
		setForeground(header.getForeground());
		setBackground(header.getBackground());
		setFont(header.getFont());
	}

	public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		setText((value == null) ? "" : value.toString());
		return this;
	}
}

/**
* ExcelAdapter enables Copy-Paste Clipboard functionality on JTables.
* The clipboard data format used by the adapter is compatible with
* the clipboard format used by Excel. This provides for clipboard
* interoperability between enabled JTables and Excel.

    * The Excel Adapter is constructed with a
    * JTable on which it enables Copy-Paste and acts
    * as a Clipboard listener.
*/
class ExcelAdapter implements ActionListener {

   private String rowstring,value;
   private Clipboard system;
   private StringSelection stsel;
   private JTable jTable1 ;

	ExcelAdapter(JTable myJTable) {
		jTable1 = myJTable;
		KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.CTRL_MASK,false);

		// Identifying the copy KeyStroke user can modify this
		// to copy on some other Key combination.
		KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.CTRL_MASK,false);

		// Identifying the Paste KeyStroke user can modify this
		//to copy on some other Key combination.
		jTable1.registerKeyboardAction(this,"Copy",copy,JComponent.WHEN_FOCUSED);
		jTable1.registerKeyboardAction(this,"Paste",paste,JComponent.WHEN_FOCUSED);
		system = Toolkit.getDefaultToolkit().getSystemClipboard();
	}

   /**
    * Public Accessor methods for the Table on which this adapter acts.
    */
	public JTable getJTable() {return jTable1;}
	public void setJTable(JTable jTable1) {this.jTable1=jTable1;}

   /**
    * This method is activated on the Keystrokes we are listening to
    * in this implementation. Here it listens for Copy and Paste ActionCommands.
    * Selections comprising non-adjacent cells result in invalid selection and
    * then copy action cannot be performed.
    * Paste is done by aligning the upper left corner of the selection with the
    * 1st element in the current selection of the JTable.
    */
	public void actionPerformed(ActionEvent e){
		if (e.getActionCommand().compareTo("Copy")==0){
			StringBuffer sbf=new StringBuffer();

			// Check to ensure we have selected only a contiguous block of cells
			int numcols=jTable1.getSelectedColumnCount();
			int numrows=jTable1.getSelectedRowCount();
			int[] rowsselected=jTable1.getSelectedRows();
			int[] colsselected=jTable1.getSelectedColumns();
			if (!((numrows-1==rowsselected[rowsselected.length-1]-rowsselected[0] &&
				   numrows==rowsselected.length) &&
			    (numcols-1==colsselected[colsselected.length-1]-colsselected[0] &&
				numcols==colsselected.length)))
		   {
			    JOptionPane.showMessageDialog(null, "Invalid Copy Selection", "Invalid Copy Selection", JOptionPane.ERROR_MESSAGE);
				return;
			}
			for (int i=0;i<numrows;i++){
				for (int j=0;j<numcols;j++){
					sbf.append(jTable1.getValueAt(rowsselected[i],colsselected[j]));
					if (j<numcols-1) sbf.append("\t");
				}
				sbf.append("\n");
			}
			stsel  = new StringSelection(sbf.toString());
			system = Toolkit.getDefaultToolkit().getSystemClipboard();
			system.setContents(stsel,stsel);
		}
		if (e.getActionCommand().compareTo("Paste")==0) {
          int startRow=(jTable1.getSelectedRows())[0];
          int startCol=(jTable1.getSelectedColumns())[0];
          try {
             String trstring= (String)(system.getContents(this).getTransferData(DataFlavor.stringFlavor));
             // System.out.println("String is:"+trstring);
             StringTokenizer st1=new StringTokenizer(trstring,"\n");
             for(int i=0;st1.hasMoreTokens();i++){
                rowstring=st1.nextToken();
                StringTokenizer st2=new StringTokenizer(rowstring,"\t");
                for(int j=0;st2.hasMoreTokens();j++){
                   value=(String)st2.nextToken();
                   if (startRow+i< jTable1.getRowCount()  &&
                       startCol+j< jTable1.getColumnCount())
                      jTable1.setValueAt(value,startRow+i,startCol+j);
                   // System.out.println("Putting "+ value+"at row="+startRow+i+"column="+startCol+j);
               }
            }
         } catch(Exception ex) {
			 Utility.vUnexpectedError(ex, "while pasting");
		 }
      }
   }
}
