/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.clients.odc.plot;

import opendap.clients.odc.Utility_String;
import opendap.clients.odc.geo.Projection;

import java.awt.*;

public class PlotEnvironment {
	public enum PLOT_TYPE {
		Pseudocolor,
		Vector,
		XY,
		Surface,
		Histogram
    }
	public static PLOT_TYPE getDefaultPlotType(){ return PLOT_TYPE.Pseudocolor; }
	public static String[] getPlotTypeDisplayList(){
    	return opendap.clients.odc.Utility_String.enumToStringArray( PLOT_TYPE.values() );
    }
	private String msPlotID;
	final private PlotAxes mAxes = PlotAxes.create();
	final private PlotText mText = PlotText.create();
	final private PlotOptions mOptions = new PlotOptions();
	private PlotScale mScale = PlotScale.create();
	private GeoReference mGeoReference = null;
	private Projection mProjection = null;
	private ColorSpecification mColorSpecification = null;
	private Color mBackgroundColor = null;
	private PLOT_TYPE mePlotType;
	public PLOT_TYPE getPlotType(){ return mePlotType; }
	public PlotAxes getAxes(){ return mAxes; }
	public PlotText getText(){ return mText; }
	public PlotScale getScale(){ return mScale; }
	public PlotOptions getOptions(){ return mOptions; }
	public void setPlotType( PLOT_TYPE ePLOT_TYPE ){ mePlotType = ePLOT_TYPE; }
	public GeoReference getGeoReference(){ return mGeoReference; }
	public void setGeoReference( GeoReference gr ){
		mGeoReference = gr;
	}
	public Projection getProjection(){ return mProjection; }
	public void setProjection( Projection p ){
		mProjection = p;
	}
	public ColorSpecification getColorSpecification(){ return mColorSpecification; }
	public void setColorSpecification( ColorSpecification cs ){
		mColorSpecification = cs;
	}
	public 	void setScale( PlotScale ps ){
		mScale = ps;
	}
	public String getPlotID(){ return msPlotID; }
	public void setPlotID(String s){ msPlotID = s; }
	public void setDimensionalType( int TYPE ){}
	public Color getBackgroundColor(){ return mBackgroundColor; }
	public void setBackgroundColor( Color c ){
		mBackgroundColor = c;
	}
	
//	void vGenerateText(){
//		getText().removeAll();
//		String sTitle = mddds.getLongName();
//		if( sTitle == null ) sTitle = mddds.getName();
//		if( sTitle == null ) sTitle = getPlotID();
//		PlotTextItem item = getText().getNew(TEXT_ID_Title);
//		item.setExpression(sTitle);
//		item.setFont(Styles.fontSansSerif18);
//		PlotLayout layout = item.getPlotLayout();
//		layout.setObject(PlotLayout.OBJECT_Plot);
//		layout.setOrientation(PlotLayout.ORIENT_TopMiddle);
//		layout.setAlignment(PlotLayout.ORIENT_BottomMiddle);
//		layout.setOffsetVertical(-15);
//	}
	
}

/** not implemented for 2.38
class PlottingDefinition_Table extends PlottingDefinition {
	int eDimensionalType;
	ArrayList listVariables = new ArrayList();
	private Panel_View_Table mTable = null;
	private final int COLUMNS_AXIS_VARIABLE = 40;
	private final int COLUMNS_AXIS_UNITS = 15;
	private JRadioButton jrbLinesInRows = new JRadioButton("lines in rows");
	private JRadioButton jrbLinesInColumns = new JRadioButton("lines in columns");
	PlottingDefinition_Table(){
		mTable = ApplicationController.getInstance().getAppFrame().getTableViewer();

		// x-axis
		TableElement teXMapping = new TableElement(this, "X-Mapping:");
		teXMapping.setDimension_Type(DimensionalPanel.DIM_x_axis);
		listVariables.add(teXMapping);

		// first range
		TableElement te = new TableElement(this, "Values:");
		te.setDimension_Type(DimensionalPanel.DIM_Y_value);
		listVariables.add(te);
		JButton buttonAddVariable = new JButton("Add Dimension");
		buttonAddVariable.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					TableElement te = new TableElement(PlottingDefinition_Table.this, "Value:");
					listVariables.add(te);
					PlottingDefinition_Table.this.add( Box.createVerticalStrut(3) );
					PlottingDefinition_Table.this.add(te);
					PlottingDefinition_Table.this.revalidate();
					PlottingDefinition_Table.this.invalidate();
				}
			}
		);

		// matrix orientation
		JPanel panelMatrixOrientation = new JPanel();
		panelMatrixOrientation.setLayout(new BoxLayout(panelMatrixOrientation, BoxLayout.Y_AXIS));
		panelMatrixOrientation.add(jrbLinesInRows);
		panelMatrixOrientation.add(Box.createVerticalStrut(4));
		panelMatrixOrientation.add(jrbLinesInColumns);
		ButtonGroup bgOrientation = new ButtonGroup();
		bgOrientation.add(jrbLinesInRows);
		bgOrientation.add(jrbLinesInColumns);

		// layout
		this.setAlignmentX(Component.LEFT_ALIGNMENT);
		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Table Variables"));
		BoxLayout boxlayoutInfo = new BoxLayout(this, BoxLayout.Y_AXIS);
		this.setLayout(boxlayoutInfo);
		this.add( panelMatrixOrientation );
		this.add( Box.createVerticalStrut(6) );
		this.add( teXMapping );
		this.add( Box.createVerticalStrut(6) );
		this.add( buttonAddVariable );
		this.add( Box.createVerticalStrut(4) );
		this.add( te );
	}

	boolean zLinesInRows(){ return jrbLinesInRows.isSelected(); }

	boolean zHasData(){
		int ctRows = mTable.theModel.getRowCount();
		int ctColumns = mTable.theModel.getColumnCount();
		if( ctRows > 1 || ctColumns > 1 ) return true;
		return false;
	}

	void setDimensionalType( int TYPE ){
		this.eDimensionalType = TYPE;
		for(int xVariable = 1; xVariable <= listVariables.size(); xVariable++){
			TableElement te = (TableElement)this.listVariables.get(xVariable-1);
			te.revalidate();
		}
		this.invalidate();
	}
	int getDatasetCount(){
		int ctValues = 0;
		for(int xVariable = 1; xVariable <= listVariables.size(); xVariable++){
			TableElement te = (TableElement)this.listVariables.get(xVariable-1);
			int eDimension = te.getDimension_Type();
			if( eDimension == DimensionalPanel.DIM_Y_value ){
				ctValues++;
			}
		}
		return ctValues;
	}
	void vGenerateText(){
	}
	PlottingData getDataset( StringBuffer sbError ){
		try {
			PlotText pt = getText();
			// PlotTextItem pti = pt.newItem();
			// pti.setExpression("x axis");
			// etc todo

			PlottingData pdat = new PlottingData();
			int[] aiDim_TYPE1 = new int[2];
			pdat.vInitialize( 1, PlottingData.AXES_None );

			// look for values
			int xVariable = 1;
			while(true){
			    if( xVariable > listVariables.size() ){
					break;
				}
				TableElement te = (TableElement)this.listVariables.get(xVariable-1);
				int eType = te.getDimension_Type();
				if( eType == DimensionalPanel.DIM_Y_value ){
					String sRange = te.getRange();
					String[][] asData0 = getRangeXY0( sRange, sbError );
					if( asData0 == null ){
						sbError.insert(0, "invalid range (" + sRange + "): ");
						return null;
					}
					int lenV_x = asData0.length;
					int lenV_y = asData0[0].length;
					if( lenV_x == 1 || lenV_y == 1 ){  // option 1 - one line
						double[] adTheLine0;
						int len;
					    if( lenV_x == 1 ){
						    len = lenV_y;
						} else {
						    len = lenV_x;
					    }
						adTheLine0 = new double[len];
						for (int xData = 0; xData < len; xData++) {
							try {
								if( lenV_x == 1 ){
									adTheLine0[xData] = Double.parseDouble(asData0[0][xData]);
								} else {
									adTheLine0[xData] = Double.parseDouble(asData0[xData][0]);
								}
							} catch (Exception ex) { // todo (1) use missing values instead (2) trap out of bounds error
								if( lenV_x == 1 ){
									sbError.append("unable to interpret value " + (xData + 1) + " [" + asData0[0][xData] + "] as a double");
								} else {
									sbError.append("unable to interpret value " + (xData + 1) + " [" + asData0[xData][0] + "] as a double");
								}
								return null;
							}
						}
						Object[] eggData = new Object[1];
						eggData[0] = adTheLine0;
						int[] aiDimLength1 = new int[2];
						aiDimLength1[1] = len;
// todo
//						if( !pdat.zAddVariable(PlottingVariable.TYPE_VALUE, eggData, DAP.DATA_TYPE_Float64, aiDimLength1, null, null, sbError) ){
//							sbError.insert(0, "Error adding value variable: ");
//							return null;
//						}
					} else if( lenV_x == 2 || lenV_y == 2 ){  // option 2 - value pairs
						int len;
					    if( lenV_x == 2 ){
						    len = lenV_y;
						} else {
						    len = lenV_x;
					    }
						double[] adTheMapping0 = new double[len];
						double[] adTheLine0 = new double[len];
						for (int xData = 0; xData < len; xData++) {
							try {
								if( lenV_x == 2 ){
									adTheMapping0[xData] = Double.parseDouble(asData0[0][xData]);
									adTheLine0[xData] = Double.parseDouble(asData0[1][xData]);
								} else {
									adTheMapping0[xData] = Double.parseDouble(asData0[xData][0]);
	    							adTheLine0[xData] = Double.parseDouble(asData0[xData][1]);
								}
							} catch (Exception ex) {
								if( lenV_x == 2 ){
									sbError.append("unable to interpret value " + (xData + 1) + " [" + asData0[0][xData] + " or " + asData0[1][xData] + " ] as a double");
								} else {
									sbError.append("unable to interpret value " + (xData + 1) + " [" + asData0[xData][0] + " or " + asData0[xData][1] + " ] as a double");
								}
								return null;
							}
						}
						int[] aiDimLength1 = new int[2];
						aiDimLength1[1] = len;
						Object[] eggData = new Object[1];
						eggData[0] = adTheLine0;
// todo
//						if( !pdat.zAddVariable(PlottingVariable.TYPE_VALUE, eggData, DAP.DATA_TYPE_Float64, aiDimLength1, null, null, sbError) ){
//							sbError.insert(0, "Error adding value variable: ");
//							return null;
//						}
						Object[] eggMapping = new Object[1];
						eggMapping[0] = adTheMapping0;
// todo
//						if( !pdat.zAddVariable(PlottingVariable.TYPE_MAPPING_X, eggMapping, DAP.DATA_TYPE_Float64, aiDimLength1, null, null, sbError) ){
//							sbError.insert(0, "Error adding value variable: ");
//							return null;
//						}
					} else { // option 3 - lines are rows/columns in matrix
						boolean zLinesInRows = this.zLinesInRows();
						int ctLines;
						int lenData;
					    if( zLinesInRows ){
							ctLines = lenV_y;
						    lenData = lenV_x;
						} else {
							ctLines = lenV_x;
						    lenData = lenV_y;
					    }
						for( int xLine = 1; xLine <= ctLines; xLine++ ){
							double[] adTheLine0 = new double[lenData];
							for (int xData = 0; xData < lenData; xData++) {
								try {
									if( zLinesInRows ){
										adTheLine0[xData] = Double.parseDouble(asData0[xData][xLine-1]);
									} else {
										adTheLine0[xData] = Double.parseDouble(asData0[xLine-1][xData]);
									}
								} catch (Exception ex) {
									if( zLinesInRows ){
										sbError.append("unable to interpret value " + (xData) + " [" + asData0[xData][xLine-1] + "] in line " + xLine + " as a double");
									} else {
										sbError.append("unable to interpret value " + (xData) + " [" + asData0[xLine-1][xData] + "] in line " + xLine + " as a double");
									}
									return null;
								}
							}
							int[] aiDimLength1 = new int[2];
							aiDimLength1[1] = lenData;
							Object[] eggData = new Object[1];
							eggData[0] = adTheLine0;
// todo
//							if( !pdat.zAddVariable(PlottingVariable.TYPE_VALUE, eggData, DAP.DATA_TYPE_Float64, aiDimLength1, null, null, sbError) ){
//								sbError.insert(0, "Error adding value variable: ");
//								return null;
//							}
						}
					}
				}
				xVariable++;
			}
			return pdat;

		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return null;
		}
	}

	String[][] getRangeXY0( String sRange, StringBuffer sbError ){
		try {
			sRange = sRange.toUpperCase();
			String[] asRanges = Utility.split(sRange, ',');
			int lenRange = 0;
			String[][] asAggregatedData0 = null;
			for( int xRange = 1; xRange <= asRanges.length; xRange++ ){
				String[] asRC = Utility.split(asRanges[xRange-1], ':');
				if( asRC.length != 2 ){
					sbError.append("Range [" + asRanges[xRange-1] + "] is invalid; must be in R1C1:R2C2 format");
					return null;
				}
				String sFrom_cell = asRC[0];
				String sTo_cell = asRC[1];
				int colFrom = iSpreadsheetCoordinateP_Column(sFrom_cell);
				int rowFrom = iSpreadsheetCoordinateP_Row(sFrom_cell);
				int colTo = iSpreadsheetCoordinateP_Column(sTo_cell);
				int rowTo = iSpreadsheetCoordinateP_Row(sTo_cell);
				int ctColumns = colTo - colFrom + 1; if( ctColumns < 0 ) ctColumns *= -1;
				int ctRows = rowTo - rowFrom + 1; if( ctRows < 0 ) ctRows *= -1;
				String[][] asData0 = null;
				if( rowFrom == rowTo ){
					asData0 = mTable.getRowMatrix0(rowFrom, colFrom, colTo, sbError);
					if (asData0 == null) {
						sbError.insert(0, "invalid row (row " + rowFrom + " column " + colFrom + ":" + colTo + " ):");
						return null;
					}
				} else if( colFrom == colTo ){
					asData0 = mTable.getColumnMatrix0(colFrom, rowFrom, rowTo, sbError);
					if (asData0 == null) {
						sbError.insert(0, "invalid col (column " + colFrom + " row " + rowFrom + ":" + rowTo + " ): ");
						return null;
					}
				} else {
					asData0 = mTable.getRangeMatrix0_XY(rowFrom, rowTo, colFrom, colTo, sbError);
					if( asData0 == null ){
						sbError.insert(0, "invalid range (column " + colFrom + ":" + colTo + " row " + rowFrom + ":" + rowTo + " ):");
						return null;
					}
				}
				if( asAggregatedData0 == null ){
					asAggregatedData0 = new String[asData0.length][asData0[0].length];
					for( int xRow = 0; xRow < ctRows; xRow++ ){
						for( int xColumn = 0; xColumn < ctColumns; xColumn++ ){
							asAggregatedData0[xColumn][xRow] = asData0[xColumn][xRow];
						}
					}
				} else {
					int ctAggregatedRows = asAggregatedData0[0].length;
					int ctAggregatedColumns = asAggregatedData0.length;
					String[][] asBuffer0 = new String[ctAggregatedColumns][ctAggregatedRows];
					for( int xRow = 0; xRow < ctAggregatedRows; xRow++ ){
						for( int xColumn = 0; xColumn < ctAggregatedColumns; xColumn++ ){
							asBuffer0[xColumn][xRow] = asAggregatedData0[xColumn][xRow];
						}
					}
					if( ctColumns == ctAggregatedColumns ){
						asBuffer0 = new String[ctAggregatedColumns][ctAggregatedRows + ctRows];
						for( int xRow = 0; xRow < ctRows; xRow++ ){
							for( int xColumn = 0; xColumn < ctColumns; xColumn++ ){
								asBuffer0[xColumn][ctAggregatedRows + xRow] = asData0[xColumn][xRow];
							}
						}
					} else if( ctRows == ctAggregatedRows ){
						asBuffer0 = new String[ctAggregatedColumns][ctAggregatedRows + ctRows];
						for( int xRow = 0; xRow < ctRows; xRow++ ){
							for( int xColumn = 0; xColumn < ctColumns; xColumn++ ){
								asBuffer0[ctAggregatedColumns + xColumn][xRow] = asData0[xColumn][xRow];
							}
						}
					} else {
						sbError.append("unable to aggregate data (" + ctColumns + "," + ctRows + ") with matrix (" + ctAggregatedColumns + "," + ctAggregatedRows + ")");
						return null;
					}
					asAggregatedData0 = asBuffer0;
				}
			}
			return asAggregatedData0;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return null;
		}
	}
	int iSpreadsheetCoordinateP_Row( String s_cell ){
		int pos = 0;
		int len = s_cell.length();
		int iRow = 0;
		while(true){
			if( pos == len ) return iRow;
			char c = s_cell.charAt(pos);
			if( c >= '0' && c <= '9' )	iRow = iRow * 10 + c - '0';
			pos++;
		}
	}
	int iSpreadsheetCoordinateP_Column( String s_cell ){
		int pos = 0;
		int len = s_cell.length();
		int iColumn = 0;
		while(true){
			if( pos == len ) return iColumn;
			char c = s_cell.charAt(pos);
			if( c >= 'A' && c <= 'Z' )	iColumn = iColumn * 26 + c - 'A' + 1;
			pos++;
		}
	}
}

class DimensionalPanel extends JPanel {

	private boolean[] mazReverse;
	private int[] meType1;
	private String[] masRegions1;
	private JComboBox mjcbVector = null;
	private JComboBox[] mCombos1;
	public final static int DIM_NONE = 0;
	public final static int DIM_x_axis = 1;
	public final static int DIM_y_axis = 2;
	public final static int DIM_X_value = 5;
	public final static int DIM_Y_value = 6;
	public final static int DIM_uX_value = 9;
	public final static int DIM_uY_value = 10;
	public final static int DIM_vX_value = 11;
	public final static int DIM_vY_value = 12;
	String[] asComboChoices_Vector = { "-", "vector x-comp", "vector y-comp" };
	String[] asComboChoices_Grid = { "-", "x-values", "y-values", "x-values reversed", "y-values reversed" };
	String[] asComboChoices_Complete = { "-", "x-axis", "y-axis", "x-values", "y-values", "x-values reversed", "y-values reversed" };
	JComboBox jcbCurrent = null;

	private ArrayList listCombos = new ArrayList();

	DimensionalPanel(String[] asRegions1, boolean zIsGrid, boolean zShowUVSelector){
		this.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		BoxLayout boxlayoutInfo = new BoxLayout(this, BoxLayout.X_AXIS);
		this.setLayout(boxlayoutInfo);
		masRegions1 = asRegions1;
		int ctRegion = getRegionCount();
		meType1  = new int[ctRegion + 1];
		mazReverse = new boolean[ctRegion + 1];
		mCombos1 = new JComboBox[ctRegion + 1];
		if( zShowUVSelector ){
			mjcbVector = new JComboBox(asComboChoices_Vector);
			mjcbVector.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						actionResolveDimensions();
					}
				}
			);
			this.add(mjcbVector);
			this.add(Box.createHorizontalStrut(3));
		}
		for( int xRegion = 1; xRegion <= ctRegion; xRegion++ ){
			if( zIsGrid ){
				jcbCurrent = new JComboBox(asComboChoices_Grid);
			} else {
				jcbCurrent = new JComboBox(asComboChoices_Complete);
			}
			meType1[xRegion] = DIM_NONE;
			mCombos1[xRegion] = jcbCurrent;
			final JComboBox jcb_final = jcbCurrent;
			final int xRegion_final = xRegion;
			jcbCurrent.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						actionResolveDimensions();
					}
				}
			);
			String sRegionLabel = masRegions1[xRegion];
			if( sRegionLabel == null ) sRegionLabel = "[undefined]";
			if( sRegionLabel.toUpperCase().startsWith("LAT") ) jcbCurrent.setSelectedItem("y-values");
			if( sRegionLabel.toUpperCase().startsWith("LON") ) jcbCurrent.setSelectedItem("x-values");
			this.add(jcbCurrent);
			this.add(Box.createHorizontalStrut(3));
			this.add(new JLabel(masRegions1[xRegion]));
			this.add(Box.createHorizontalStrut(5));
		}
	}
	private void actionResolveDimensions(){
		int ctRegions = this.getRegionCount();
		boolean zIsVector = false;
		boolean zIsVector_U = false;
		if( mjcbVector != null ){
			String sItem = (String) mjcbVector.getSelectedItem();
			if( sItem.equals("-") ){
				// not a vector
			} else if( sItem.equals("vector x-comp") ){
				zIsVector = true;
				zIsVector_U = true;
			} else if( sItem.equals("vector y-comp") ){
				zIsVector = true;
				zIsVector_U = false;
			} else {
				// unknown choice - treat as not a vector
			}
		}
		for( int xRegion = 1; xRegion <= ctRegions; xRegion++ ){
			JComboBox jcbCurrent = mCombos1[xRegion];
			if( jcbCurrent == null ) continue; // this can happen during initialization
			int index = jcbCurrent.getSelectedIndex();
			int iDIM = 0;
			boolean zReversed = false;
			if( index < 0 ){
				iDIM = DIM_NONE;
			} else {
				String sItem = (String) jcbCurrent.getSelectedItem();
				if( sItem.equals("-") ) iDIM = DIM_NONE;
				else if( sItem.equals("x-axis") ) iDIM = DIM_x_axis;
				else if( sItem.equals("y-axis") ) iDIM = DIM_y_axis;
				else if( sItem.equals("x-axis reversed") ){ iDIM = DIM_x_axis; zReversed = true; }
				else if( sItem.equals("y-axis reversed") ){ iDIM = DIM_y_axis; zReversed = true; }
				else if( sItem.equals("x-values") || sItem.equals("x-values reversed") ){
					if( zIsVector ){
						if( zIsVector_U ){
							iDIM = this.DIM_uX_value;
						} else {
							iDIM = this.DIM_vX_value;
						}
					} else {
						iDIM = DIM_X_value;
					}
					if( sItem.equals("x-values reversed") ) zReversed = true;
				}
				else if( sItem.equals("y-values") || sItem.equals("y-values reversed") ){
					if( zIsVector ){
						if( zIsVector_U ){
							iDIM = this.DIM_uY_value;
						} else {
							iDIM = this.DIM_vY_value;
						}
					} else {
						iDIM = DIM_Y_value;
					}
					if( sItem.equals("y-values reversed") ) zReversed = true;
				}
				else iDIM = DIM_NONE;
			}
			setDimension( xRegion, iDIM, zReversed );
		}
	}
	private void setDimension(int xRegion1, int eValue, boolean zReversed){
	    meType1[xRegion1] = eValue;
		mazReverse[xRegion1] = zReversed;
	}
	int getRegionCount(){ if( masRegions1 == null ) return 0; return masRegions1.length - 1; }

	int getDimension(int xRegion1){ return meType1[xRegion1]; }
	void setDimension(int xRegion1, int eTYPE){ meType1[xRegion1] = eTYPE; }
	boolean getReversed(int xRegion1){ return mazReverse[xRegion1]; }
	String getRegionLabel(int xRegion1){ if( masRegions1 == null ) return ""; return masRegions1[xRegion1]; }
}

class TableElement extends JPanel {
	private PlottingDefinition_Table myParent;
	private DimensionalPanel dimensional_panel;
	private JTextField jtfRange = new JTextField(12);
	private JLabel jlabelID = new JLabel("ID:");
	private JTextField jtfID = new JTextField(14);
	void setDimension_Type(int eTYPE){
		dimensional_panel.setDimension(1, eTYPE);
	}
	int getDimension_Type(){
		return dimensional_panel.getDimension(1);
	}
// todo
//	int getDimension_Class(){
//		switch( dimensional_panel.getDimension(1) ){
//			case DimensionalPanel.DIM_NONE:
//				return VariableInfo.CLASS_undefined;
//			case DimensionalPanel.DIM_x_axis:
//				return VariableInfo.CLASS_axis;
//			case DimensionalPanel.DIM_y_axis:
//				return VariableInfo.CLASS_axis;
//			case DimensionalPanel.DIM_X_value:
//				return VariableInfo.CLASS_value;
//			case DimensionalPanel.DIM_Y_value:
//				return VariableInfo.CLASS_value;
//			case DimensionalPanel.DIM_uX_value:
//				return VariableInfo.CLASS_U_component;
//			case DimensionalPanel.DIM_uY_value:
//				return VariableInfo.CLASS_U_component;
//			case DimensionalPanel.DIM_vX_value:
//				return VariableInfo.CLASS_V_component;
//			case DimensionalPanel.DIM_vY_value:
//				return VariableInfo.CLASS_V_component;
//			default:
//				return VariableInfo.CLASS_undefined;
//		}
//	}
	String getRange(){ return jtfRange.getText(); }
	String getID(){ return jtfID.getText(); }
	TableElement(PlottingDefinition_Table parent, String sLabel){
		myParent = parent;
		String[] asRegions1 = new String[2];
		asRegions1[1] = "";
		JLabel jlabelRange = new JLabel(sLabel);
		dimensional_panel = new DimensionalPanel(asRegions1, false, false);
		this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		BoxLayout boxlayoutInfo = new BoxLayout(this, BoxLayout.X_AXIS);
		this.setLayout(boxlayoutInfo);
		this.setMaximumSize(new Dimension(400, 25));
//		this.add( dimensional_panel );  // todo not using dimensional panel anymore
//		this.add( Box.createHorizontalStrut(2) );
		this.add( jlabelRange );
		this.add( Box.createHorizontalStrut(3) );
		this.add( jtfRange );
		this.add( Box.createHorizontalStrut(5) );
		this.add( jlabelID );
		this.add( Box.createHorizontalStrut(3) );
		this.add( jtfID );
	}
}
*/

