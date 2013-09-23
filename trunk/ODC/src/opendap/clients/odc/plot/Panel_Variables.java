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

/**
 * Title:        Panel_Variables
 * Description:  Select variables for plotting, used by Panel_View_Plot
 * Copyright:    Copyright (c) 2003-4
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.59
 */

import opendap.clients.odc.*;
import opendap.clients.odc.gui.Resources;
import opendap.clients.odc.gui.Styles;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import opendap.dap.*;

// This class creates the interface which allows the user to select which variable(s) they
// want to plot out of a dataset. A dataset may have many variables in it. Note that the list of
// variables is held in the Panel_View_Plot class which also has the main control buttons such as
// as the "Plot" button. When the user selects a dataset ultimately that causes zShowDataDDS in
// this class to be called. This method determines every variable in the dataset that can be
// plotted using the selected plot type (ie, histogram, pseudocolor etc) and updates the
// variable selector VSelector_Plot_Schematic to show the variable choices and their parameters.

/*
	Panel_VariableTab
		Panel_Variables
			VSelector_Plot_Schematic
 */

public class Panel_Variables extends JPanel {
	VSelector_Plot_Schematic mvselector;
    public Panel_Variables() {}
	public boolean zInitialize( StringBuffer sbError ){
		setLayout(new BorderLayout());
		VSelector_Plot_Schematic vselector_new = new VSelector_Plot_Schematic();
		mvselector = vselector_new;
		add( mvselector, BorderLayout.CENTER );
		return true;
	}

	boolean zShowDataDDS( PlotEnvironment.PLOT_TYPE ePlotType, DataDDS ddds, DAS das, PlotOptions po, StringBuffer sbError ){
		if( ddds == null ){
			sbError.append("no data DDS provided");
			return false;
		}

		// if the data DDS already loaded into the selector matches the supplied one in
		// definition then do not change the selector, just update the data set
		// this allows multiple datasets with the same DDS to be thumbnailed all at once
		// using the same settings
 		if( mvselector != null && mvselector.zCompareData( ddds, ePlotType ) ){
			if( mvselector.setData( ddds, das, ePlotType, sbError ) ){
				if( mvselector.zSetModelValueSpecifications( sbError ) ){
					mvselector.vUpdateModels();
				} else {
					sbError.insert(0, "failed to set model value specifications: ");
					return false;
				}
			} else {
				sbError.insert(0, "failed to load data variables into selector: ");
				return false;
			}
		} else {

			// this method will determine all the valid variables in the DataDDS for the
			// selected plot type, create VariableSpecifications for those variables, and
			// update the selector so that it lists them; a VariableSpecification is an
			// object that stores information about where a variable is inside of a DataDDS
			// as well as ancillary information about the variable
			if( !mvselector.zShowVariableChoices( ePlotType, ddds, das, po, sbError ) ){
				sbError.insert(0, "failed to initialize variable selector: ");
				return false;
			}
		}
		return true;
	}

	VariableDataset getDataset( StringBuffer sbError ){
		return mvselector.getDataset( sbError );
	}

	String[] masLabels1_buffer;
	String[] masVariableName1_buffer;
	VariableSpecification[] maVariables1_buffer;

	// Variable Size
	// the size variable is used to restrict the discovery to variables that are
	// one-dimensional vectors of a particular length; this is used when getting
	// variables for an axis; if size is 0 there are no restrictions

	// Sequences
	// sequences are stored in a VariableSpec by specifying the BaseType of the root sequence
	// and then the path to the non-sequence variable; the node name is store for the root sequence
	private boolean zDiscoverVariables_recursion( PlotEnvironment.PLOT_TYPE ePLOT_TYPE, int size, String sNodeName, Enumeration enumVariables, DAS das, ArrayList listVariableSpecs, StringBuffer sbError ){
		try {
		    Enumeration enumChildVariables = null;
			while( enumVariables.hasMoreElements() ){
				BaseType bt = (BaseType)enumVariables.nextElement();
				String sVariableName = bt.getName();
				String sLongName = DAP.getAttributeValue( das, sVariableName, "long_name", sbError );
				String sUnits = DAP.getAttributeValue( das, sVariableName, "units", sbError );
				if( sVariableName == null ) continue; // ignore variables without a name
				if( bt instanceof DStructure ){
					enumChildVariables = ((DConstructor)bt).getVariables();
					sNodeName = ( sNodeName == null ) ? sVariableName : sNodeName + '.' + sVariableName;
	    			zDiscoverVariables_recursion( ePLOT_TYPE, size, sNodeName, enumChildVariables, das, listVariableSpecs, sbError );
				} else if( bt instanceof DSequence) {
					DSequence dsequence = (DSequence)bt;
					VariableSpecification vsNewSequence = new VariableSpecification();
					if( !vsNewSequence.zInitialize(bt, sVariableName, sLongName, sUnits, sbError) ){
						sbError.insert(0, "Error creating variable specification for sequence root: ");
						return false;

					}
					vsNewSequence.sBaseNode = sNodeName;
					vsNewSequence.maiPath1[0] = 1;
					vsNewSequence.maiPath1[1] = 0; // a field number of 0 indicates the root
					vsNewSequence.masPath1[1] = sVariableName;
					if( !zDiscoverVariables_sequence(ePLOT_TYPE, vsNewSequence, listVariableSpecs, sbError) ){
						sbError.insert(0, "Error analyzing sequence variable: ");
						return false;
					}
				} else if( bt instanceof DGrid ) {
					final DGrid dgrid = (DGrid)bt;
					enumChildVariables = dgrid.getVariables();
					sNodeName = ( sNodeName == null ) ? sVariableName : sNodeName + '.' + sVariableName;
				    zDiscoverVariables_recursion( ePLOT_TYPE, size, sNodeName, enumChildVariables, das, listVariableSpecs, sbError );
				} else if( bt instanceof DArray ) { // can plot maybe
					DArray darray = (DArray)bt;
					int iEffectiveDimCount = iDetermineEffectiveDimCount( darray );
					if( iEffectiveDimCount == 0 ) continue; // ignore
					if( size > 0 ){ // this parameter is only used for variables for axes
						if( iEffectiveDimCount != 1 ) continue; // not a valid map array
						if( darray.getFirstDimension().getSize() != size ) continue; // not a matching map array
					} else {
						switch( ePLOT_TYPE ){
							case Histogram: // any
							case XY: // 1- or 2-D
							case Vector: // 1- or 2-D
								break;
							case Pseudocolor: // 1 2-D
								if( iEffectiveDimCount >= 2 ) break; else continue;
						}
					}
					VariableSpecification vsNewArray = new VariableSpecification();
					if( !vsNewArray.zInitialize(bt, sVariableName, sLongName, sUnits, sbError) ){
						sbError.insert(0, "Error creating variable specification for array root: ");
						return false;

					}
					vsNewArray.sBaseNode = sNodeName;
					vsNewArray.maiPath1[0] = 1;
					vsNewArray.masPath1[1] = sVariableName;
					listVariableSpecs.add( vsNewArray );
				} else { // other types are not plotable
					continue;
				}
			}
			return true;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return false;
		}
	}

	boolean zDiscoverVariables_sequence( PlotEnvironment.PLOT_TYPE ePLOT_TYPE, VariableSpecification vsSequence, ArrayList listVariableSpecs, StringBuffer sbError ){
	    DSequence sequence = (DSequence)vsSequence.getArrayTable(sbError).bt;
		int xPath = 1;
		BaseType btPathCursor = sequence;
		while( true ){
			if( !(btPathCursor instanceof DSequence) ){
				sbError.append("invalid sequence path");
				return false;
			}
			xPath++;
			if( xPath > vsSequence.maiPath1[0] ) break;
			int iFieldNumber = vsSequence.maiPath1[xPath];
			try {
				btPathCursor = ((DSequence)btPathCursor).getVar(iFieldNumber-1); // this is a zero-based call
			} catch(Exception ex) {
				sbError.append("path cursor not found");
				return false;
			}
		}
		DSequence sequenceNested = (DSequence)btPathCursor;
		int ctFields = sequenceNested.elementCount();
		for( int xField = 1; xField <= ctFields; xField++ ){
			BaseType btField = null;
		    try {
			    btField = sequenceNested.getVar(xField-1); // zero-based call
			} catch(Exception ex) {
				sbError.append("field not found");
				return false;
			}
			if( btField == null ){
				sbError.append("sequence '" + sequenceNested.getName() + "' field " + xField + " unexpectedly null");
				return false;
			}
			String sFieldName = btField.getName();
			VariableSpecification vsNewLevel = vsSequence.clone_();
			int ctNestingLevel = vsNewLevel.maiPath1[0];
			ctNestingLevel++;
			vsNewLevel.maiPath1[0] = ctNestingLevel;
			vsNewLevel.maiPath1[ctNestingLevel] = xField;
			vsNewLevel.masPath1[ctNestingLevel] = sFieldName;
			if( btField instanceof DSequence ){
				if( !zDiscoverVariables_sequence( ePLOT_TYPE, vsNewLevel, listVariableSpecs, sbError ) ){
					return false;
				}
			} else if( btField instanceof DArray || btField instanceof DGrid || btField instanceof DStructure ){
				ApplicationController.vShowWarning("nested sequence array/grid ignored (sequence '" + sequenceNested.getName() + "' field " + xField + ")");
				// not supported (todo)
			} else if( btField instanceof DByte ||
					   btField instanceof DInt16 ||
					   btField instanceof DUInt16 ||
					   btField instanceof DInt32 ||
					   btField instanceof DUInt32 ||
					   btField instanceof DFloat32 ||
					   btField instanceof DFloat64 ||
					   btField instanceof DString   // will try to convert to number
					  ){
				int ctDims = vsNewLevel.getDimCount();
				switch( ePLOT_TYPE ){
					case Histogram: // any
					case XY: // 1- or 2-D
					case Vector: // 1- or 2-D
						break;
					case Pseudocolor: // 1 2-D
						if( ctDims >= 2 ) break; else continue;
				}
				if( zSequenceHasOnlyOneRow( vsNewLevel, sbError ) ) continue; // ignore
				listVariableSpecs.add( vsNewLevel );
			} else {
				ApplicationController.vShowWarning("unknown nested sequence type ignored");
			}
		}
		return true;
	}

	// without getting data vectors this is the best that can be done
	private boolean zSequenceHasOnlyOneRow( VariableSpecification vs, StringBuffer sbError ){
		if( vs.maiPath1[0] == 2 && ((DSequence)vs.getArrayTable(sbError).bt).getRowCount() < 2 ) return true;
		sbError.append("internal error, invalid multirow");
		return false;
	}

	int iDetermineEffectiveDimCount( DArray darray ){
		try {
			int iEffectiveDimCount = 0;
			for( int xDim = 0; xDim < darray.numDimensions(); xDim++ ){
				DArrayDimension theDim = darray.getDimension(xDim);
				if( theDim.getSize() > 1 ) iEffectiveDimCount++;
			}
			return iEffectiveDimCount;
		} catch(Exception ex) {
			return 0; // todo consider handling this error
		}
	}

}

class VSelector_Plot_Schematic extends JPanel {

	// data
	private PlotEnvironment.PLOT_TYPE mePLOT_TYPE;
	PlotVariables mPlotVariables;
	private Model_Variables mVariablesModel =  null;

	// components
	JPanel mpanelY, mpanelX, mpanelV;
	JPanel mpanelValue;
	VSelector_Plot_Values mvarpanelValues1;
	VSelector_Plot_Values mvarpanelValues2;
	boolean mzUsingSecondVariable;
	VSelector_Plot_Axis mvarpanelXAxis;
	VSelector_Plot_Axis mvarpanelYAxis;
	VSelector_Plot_Schematic(){}

	// schematic arrow renderer
	Stroke strokeThickness4 = new BasicStroke(4); // these are module-level to speed rendering
	Stroke strokeThickness3 = new BasicStroke(3); // these are module-level to speed rendering
	Stroke strokeThickness1 = new BasicStroke(1);
	int[] aiPoints_x = new int[3];
	int[] aiPoints_y = new int[3];
	public void paintComponent( Graphics g ){
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		if( mpanelY == null || mpanelX == null || mpanelV == null ) return;
		if( mePLOT_TYPE != PlotEnvironment.PLOT_TYPE.Histogram ){
			g2.setColor(Color.BLACK);
			int pxWidth = 4;

			// calculate y-axis bar
			int y_YPanelRight = mpanelY.getX() + mpanelY.getWidth();
			int value_begin = mpanelV.getX();
			int y_Margin = 20;
			int y_ValuePanelBottom = mpanelV.getY() + mpanelV.getHeight();
			int pxXAxisSpacing = mpanelX.getY() - y_ValuePanelBottom;
			int pxYAxisHeight =  y_ValuePanelBottom + (pxXAxisSpacing - pxWidth)*2/3 - y_Margin;
			int pxYAxisSpacing = value_begin - y_YPanelRight;
			int x_YAxisLine = y_YPanelRight + ( pxYAxisSpacing - pxWidth)/3;

			// calculate x-axis bar
			int x_XAxis_width = mpanelV.getWidth() + pxYAxisSpacing;
			int y_XAxis = y_Margin + pxYAxisHeight;
			int x_XAxis_right = x_YAxisLine + x_XAxis_width;

			g2.setStroke(strokeThickness3);

			// draw axes
			aiPoints_x[0] = x_YAxisLine; aiPoints_y[0] = y_Margin; // top
			aiPoints_x[1] = x_YAxisLine; aiPoints_y[1] = y_XAxis; // corner
			aiPoints_x[2] = x_XAxis_right; aiPoints_y[2] = y_XAxis; // right
			g.drawPolyline(aiPoints_x, aiPoints_y, 3);

			// draw y-axis arrow head
			aiPoints_x[0] = x_YAxisLine - 10; aiPoints_y[0] = y_Margin + 13;
			aiPoints_x[1] = x_YAxisLine; aiPoints_y[1] = y_Margin - 2;
			aiPoints_x[2] = x_YAxisLine + 10; aiPoints_y[2] = y_Margin + 13;
			g.drawPolyline(aiPoints_x, aiPoints_y, 3);

			// draw x-axis arrow head
			aiPoints_x[0] = x_XAxis_right - 13; aiPoints_y[0] = y_XAxis - 10;
			aiPoints_x[1] = x_XAxis_right + 2; aiPoints_y[1] = y_XAxis;
			aiPoints_x[2] = x_XAxis_right - 13; aiPoints_y[2] = y_XAxis + 10;
			g.drawPolyline(aiPoints_x, aiPoints_y, 3);

			g2.setStroke(strokeThickness1);

			// draw labels
			g.setFont(Styles.fontSerifItalic18);
			g.drawString("y", x_YAxisLine - 14, y_Margin - 3);
			g.drawString("x", x_YAxisLine + x_XAxis_width + 3, y_XAxis + 20);
		}
	}

	Model_Variables getModel(){ return this.mVariablesModel; }

	boolean setData( DataDDS ddds, DAS das, PlotEnvironment.PLOT_TYPE ePlotType, StringBuffer sbError ){
		if( mPlotVariables == null ) mPlotVariables = new PlotVariables();
		return mPlotVariables.zLoadVariables( ddds, das, ePlotType, 0, sbError );
	}

	int getVariableCount(){ return mPlotVariables.getVariableCount(); }

	VariableSpecification getVariable1( int index, StringBuffer sbError ){
		return mPlotVariables.getVariable1( index, sbError );
	}

	// Determines the style of the interface and populates it with the possible variables.
	// There are three modes or styles or interface: 0-dimensional, 1-dimensional or 2-dimensional.
	// For example, histogram uses a 0-dimensional style wheras a pseudocolor uses a 2D one.
	boolean zShowVariableChoices( PlotEnvironment.PLOT_TYPE ePLOT_TYPE, DataDDS ddds, DAS das, final PlotOptions po, StringBuffer sbError ){
		try {

			mePLOT_TYPE = ePLOT_TYPE;
			if( !setData( ddds, das, ePLOT_TYPE, sbError ) ){
				sbError.insert(0, "failed to set data: ");
				return false;
			}

			switch( ePLOT_TYPE ){
				case Histogram:
					this.mVariablesModel = new Model_Variables( false, false );
					break;
				case Pseudocolor:
					this.mVariablesModel = new Model_Variables( false, true );
					break;
				case Vector:
					this.mVariablesModel = new Model_Variables( true, true );
					break;
				case XY:
					this.mVariablesModel = new Model_Variables( true, true );
					break;
				default:
					sbError.append("unknown plot type " + ePLOT_TYPE);
					return false;
			}

			this.removeAll(); // todo re-use axes etc

			// the axes need to exist before the values are initialized
			if( ePLOT_TYPE != PlotEnvironment.PLOT_TYPE.Histogram ){

				// x-axis
				mvarpanelXAxis = new VSelector_Plot_Axis( this, this.getModel().getModel_Axis_X() );
				String sLabelX = ""; // old label: "X-Axis:"
				if( !mvarpanelXAxis.zInitialize(sLabelX, null, null, true, sbError) ){
					sbError.insert(0, "failed to initialize x-axis panel: ");
					return false;
				}

				// y-axis
				mvarpanelYAxis = new VSelector_Plot_Axis( this, this.getModel().getModel_Axis_Y() );
				String sLabelY = ""; // old label: "Y-Axis:"
				if( !mvarpanelYAxis.zInitialize(sLabelY, null, null, true, sbError) ){
					sbError.insert(0, "failed to initialize y-axis panel: ");
					return false;
				}

			}

			// determine mode
			int ctDimensions;
			if( ePLOT_TYPE == PlotEnvironment.PLOT_TYPE.Histogram ){
				ctDimensions = 0;
			} else if( ePLOT_TYPE == PlotEnvironment.PLOT_TYPE.XY ){
				ctDimensions = 1;
			} else {
				ctDimensions = 2;
			}

			// value panel 1
			mvarpanelValues1 = new VSelector_Plot_Values( this, this.getModel().getModel_Variable1() );
//			masLabels1 = masLabels1_buffer;
//			maVariables1 = maVariables1_buffer;
			String sLabel = "Choose Values:";
			if( ePLOT_TYPE == PlotEnvironment.PLOT_TYPE.Vector ){
				sLabel = "U:";
			} else if( ePLOT_TYPE == PlotEnvironment.PLOT_TYPE.XY ){
				sLabel = "Y Variable:";
			}
			boolean zShowTransforms = true;
			boolean zAllowCaptions  = true;
			boolean zNoneAreIndexes = false;
			boolean zXonly = false;
			boolean zYonly = false;
			if( ePLOT_TYPE == PlotEnvironment.PLOT_TYPE.XY ){
				zYonly = true;
				zNoneAreIndexes = true;
			}
			if( !mvarpanelValues1.zInitialize(sLabel, mPlotVariables.getLabels(), das, zShowTransforms, ctDimensions, zAllowCaptions, zNoneAreIndexes, zXonly, zYonly,  sbError) ){
				sbError.insert(0, "failed to initialize value panel 1: ");
				return false;
			}

			// value panel 2
			mzUsingSecondVariable = (this.getModel().getModel_Variable2() == null) ? false : true;
			if( mzUsingSecondVariable ){
				mvarpanelValues2 = new VSelector_Plot_Values( this, this.getModel().getModel_Variable2() );
				String sLabel2;
				switch( ePLOT_TYPE ){
					case Vector:
						sLabel2 = "V:"; break;
					case XY:
						sLabel2 = "X Variable:";
						break;
					default:
						sLabel2 = "2nd Variable:";
						break;
				}
				zShowTransforms = false;
				zAllowCaptions  = false;
				zNoneAreIndexes = false;
				if( ePLOT_TYPE == PlotEnvironment.PLOT_TYPE.XY ){
	    			zYonly = false;
	    			zXonly = true;
					zNoneAreIndexes = true;
		    	}
				if( !mvarpanelValues2.zInitialize(sLabel2, mPlotVariables.getLabels(), das, zShowTransforms, ctDimensions, zAllowCaptions, zNoneAreIndexes, zXonly, zYonly, sbError) ){
					sbError.insert(0, "failed to initialize value panel 2: ");
					return false;
				}
			}

			// determine what variables should be selected to start with
			// it's important to try to select a compatible pair
			// unfortunately it is too complex to do this currently
			if( mzUsingSecondVariable ){
//				VariableSpecification vs = mPlotVariables.getVariable1(1);
//				vs.getArrayTable(sbError)
				mvarpanelValues1.setSelection(1);
				mvarpanelValues2.setSelection(2);
			} else { // if only variable is needed just use first one
				mvarpanelValues1.setSelection(1);
			}

			mpanelValue = new JPanel();
			mpanelValue.setBackground(Color.WHITE);
			mpanelValue.setLayout(new BoxLayout(mpanelValue, BoxLayout.X_AXIS));
			mpanelValue.add(mvarpanelValues1);
			if( mzUsingSecondVariable ){
				mpanelValue.add(Box.createHorizontalStrut(10));
				mpanelValue.add(mvarpanelValues2);
			}

			// setup graphics
			setBackground(Color.WHITE);
			JPanel panelXYOptions = null;
			if( ePLOT_TYPE == PlotEnvironment.PLOT_TYPE.XY ){
				final JCheckBox jcheckShowLines = new JCheckBox("Show Lines");
				final JCheckBox jcheckShowPoints = new JCheckBox("Show Points");
				jcheckShowLines.setOpaque(false);
				jcheckShowPoints.setOpaque(false);
				jcheckShowLines.setSelected( po.getValue_boolean(PlotOptions.OPTION_XY_ShowLines) );
				jcheckShowPoints.setSelected( po.getValue_boolean(PlotOptions.OPTION_XY_ShowPoints) );
				jcheckShowLines.addActionListener(
				    new java.awt.event.ActionListener(){
						public void actionPerformed( java.awt.event.ActionEvent ae ){
							boolean zLines = jcheckShowLines.isSelected();
							boolean zPoints = jcheckShowPoints.isSelected();
							if( !zLines && !zPoints ){
								jcheckShowPoints.setSelected(true); // at least one must be selected
								zPoints = true;
							}
							po.setItem_boolean(PlotOptions.OPTION_XY_ShowLines, zLines);
							po.setItem_boolean(PlotOptions.OPTION_XY_ShowPoints, zPoints);
						}
					});
				jcheckShowPoints.addActionListener(
				    new java.awt.event.ActionListener(){
						public void actionPerformed( java.awt.event.ActionEvent ae ){
							boolean zLines = jcheckShowLines.isSelected();
							boolean zPoints = jcheckShowPoints.isSelected();
							if( !zLines && !zPoints ){
								jcheckShowLines.setSelected(true); // at least one must be selected
								zLines = true;
							}
							po.setItem_boolean(PlotOptions.OPTION_XY_ShowLines, zLines);
							po.setItem_boolean(PlotOptions.OPTION_XY_ShowPoints, zPoints);
						}
					});
				panelXYOptions = new JPanel();
				panelXYOptions.setOpaque(false);
				panelXYOptions.setBackground(Color.WHITE);
				panelXYOptions.setLayout(new BoxLayout(panelXYOptions, BoxLayout.Y_AXIS));
				panelXYOptions.add(jcheckShowLines);
				panelXYOptions.add(Box.createVerticalStrut(6));
				panelXYOptions.add(jcheckShowPoints);
			}

			if( ePLOT_TYPE == PlotEnvironment.PLOT_TYPE.XY ){
				mpanelY = mvarpanelValues1;
				mpanelX = mvarpanelValues2;
				mpanelV = panelXYOptions;
			} else {
				mpanelY = mvarpanelYAxis;
				mpanelX = mvarpanelXAxis;
				mpanelV = mpanelValue;
			}

			// do layout
			this.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			gbc.fill = gbc.BOTH;

			// left margin
			gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
			gbc.gridheight = 1; gbc.gridwidth = 1;
			this.add(Box.createHorizontalStrut(20), gbc);

			// top margin
			gbc.gridx = 1; gbc.gridy = 0;
			this.add(Box.createVerticalStrut(20), gbc);

			if( ePLOT_TYPE != PlotEnvironment.PLOT_TYPE.Histogram ){
				gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.0;
				add(mpanelY, gbc);
				gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0;
				this.add(Box.createHorizontalStrut(80), gbc);
			}
			gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 0.0;
			add(mpanelV, gbc);
			gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 0.0;
			this.add(Box.createVerticalStrut(50), gbc);
			if( ePLOT_TYPE != PlotEnvironment.PLOT_TYPE.Histogram ){
				gbc.gridx = 3; gbc.gridy = 3; gbc.weightx = 0.0;
				if( ePLOT_TYPE == PlotEnvironment.PLOT_TYPE.Vector ) gbc.gridwidth = 2;
				add(mpanelX, gbc);
				gbc.gridx = 3; gbc.gridy = 4;
				this.add(Box.createVerticalGlue(), gbc);
			}
			gbc.gridx = 4; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 1;
			gbc.gridheight = 5;
			this.add(Box.createHorizontalGlue(), gbc);
			return true;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return false;
		}
	}

	boolean zCompareData( DataDDS ddds, PlotEnvironment.PLOT_TYPE ePLOT_TYPE ){
		if( ePLOT_TYPE != mePLOT_TYPE ) return false;
		return DAP.zCompareDDS( ddds, mPlotVariables.getDDDS() );
	}

	StringBuffer msbError = new StringBuffer(); // throwaway buffer
	void vRecalcAxes(){
		try {
			StringBuffer sbError = new StringBuffer();
			if( mvarpanelXAxis == null || mvarpanelYAxis == null ) return;
			VariableSpecification vs1 = mvarpanelValues1.getSelectedVS( sbError );
			if( vs1 == null ){
				if( sbError.length() > 0 ){
					ApplicationController.vShowError("Error in variable 1: " + sbError);
					sbError.setLength(0);
				}
				mvarpanelXAxis.vSetBlank();
				mvarpanelYAxis.vSetBlank();
				return;
			}

			// get the current selections for x and y dims
			int xDimX = mvarpanelValues1.getDimX();
			int xDimY = mvarpanelValues1.getDimY();
			Panel_Dimension dimX = mvarpanelValues1.getDimX_Variable();
			Panel_Dimension dimY = mvarpanelValues1.getDimY_Variable();
			int lenDimX = (dimX == null) ? 0 : dimX.getModel().getSize();
			int lenDimY = (dimY == null) ? 0 : dimY.getModel().getSize();

			if( xDimX == 0 ) mvarpanelXAxis.vSetBlank();
			if( xDimY == 0 ) mvarpanelYAxis.vSetBlank();

			// see if we can get axes automatically
			ArrayTable at = vs1.getArrayTable(msbError);
			BaseType btParent = at.bt.getParent();
			if( btParent != null && btParent instanceof DGrid ){
				DGrid gridParent = (DGrid)btParent;
				BaseType btX = null;
				BaseType btY = null;
				try {
					btX = (xDimX == 0) ? null : gridParent.getVar(xDimX); // these dims are one-based so they match up naturally
					btY = (xDimY == 0) ? null : gridParent.getVar(xDimY);
				} catch(Exception ex) {}
				DAS das = mPlotVariables.getDAS();
				if( btX != null ){
					String sNameX = btX.getName();
					String sCaptionX = DAP.getAttributeValue( das, sNameX, "long_name", msbError);
					String sUnitsX = DAP.getAttributeValue( das, sNameX, "units", msbError);
                    if( sCaptionX == null ) sCaptionX = sNameX;
					VariableSpecification vsX = new VariableSpecification();
					if( !vsX.zInitialize(btX, sNameX, sCaptionX, sUnitsX, sbError) ){
						sbError.insert(0, "Error creating variable specification for x-axis: ");
						return;

					}
					mvarpanelXAxis.vSetFixed(sNameX, vsX);
				}
				if( btY != null ){
					String sNameY = btY.getName();
					String sCaptionY = DAP.getAttributeValue( das, sNameY, "long_name", msbError);
					String sUnitsY = DAP.getAttributeValue(das, sNameY, "units", msbError);
                    if( sCaptionY == null ) sCaptionY = sNameY;
					VariableSpecification vsY = new VariableSpecification();
					if( !vsY.zInitialize(btY, sNameY, sCaptionY, sUnitsY, sbError) ){
						sbError.insert(0, "Error creating variable specification for y-axis: ");
						return;

					}
					mvarpanelYAxis.vSetFixed(btY.getName(), vsY);
				}
				return; // maps found automatically for grid
			}

			// automatic did not work, determine possibilities for valid map arrays
			// x-axis discovery
			if( mvarpanelXAxis.zSetData( mPlotVariables.getDDDS(), mPlotVariables.getDAS(), lenDimX, sbError) ){
				// x-axis ok
			} else {
				ApplicationController.vShowError("Failed to set variables for x-axis: " + sbError);
				sbError.setLength(0);
			}

			// y-axis discovery
			if( mvarpanelYAxis.zSetData( mPlotVariables.getDDDS(), mPlotVariables.getDAS(), lenDimY, sbError) ){
				// y-axis ok
		    } else {
				ApplicationController.vShowError("Failed to set variables for y-axis: " + sbError);
				sbError.setLength(0);
			}

		} catch(Throwable ex) {
			ApplicationController.vUnexpectedError(ex, "Error updating axes: ");
		}
	}

	boolean zSetModelValueSpecifications( StringBuffer sbError ){
		boolean zSetVariable_1_Specification = false;
		boolean zSetVariable_2_Specification = false;
		switch( this.mePLOT_TYPE ){
			case Histogram:
			case Pseudocolor:
				zSetVariable_1_Specification = true;
				break;
			case Vector:
			case XY:
				zSetVariable_1_Specification = true;
				zSetVariable_2_Specification = true;
				break;
			default:
				sbError.append("unknown plot type " + this.mePLOT_TYPE);
				return false;
		}
		if( zSetVariable_1_Specification ){
			VariableSpecification vs1 = this.mvarpanelValues1.getSelectedVS( sbError );
			if( vs1 == null && sbError.length() > 0 ){
				sbError.insert(0, "Error getting variable specification for variable 1: " + sbError);
				return false;
			}
			this.getModel().getModel_Variable1().setVariableSpecfication( vs1 );
		}
		if( zSetVariable_2_Specification ){
			VariableSpecification vs2 = this.mvarpanelValues2.getSelectedVS( sbError );
			if( vs2 == null && sbError.length() > 0 ){
				sbError.insert(0, "Error getting variable specification for variable 2: " + sbError);
				return false;
			}
			this.getModel().getModel_Variable1().setVariableSpecfication( vs2 );
		}
		return true;
	}

	void vUpdateModels(){
		if( this.getModel() == null ){
			Thread.dumpStack();
			ApplicationController.getInstance().vShowError("internal error, model update called but models do not exist");
			return;
		} else {

			if( this.mePLOT_TYPE == PlotEnvironment.PLOT_TYPE.Histogram ){
				this.getModel().getModel_Variable1().setUseEntireVariable( true ); // all that needs to be done
				return;
			} else {
				this.getModel().getModel_Variable1().setUseEntireVariable( false );
				if( this.getModel().getModel_Variable2() != null ) this.getModel().getModel_Variable2().setUseEntireVariable( false );
			}

			boolean zReversedX = mvarpanelXAxis.zReversed();
			boolean zReversedY = mvarpanelYAxis.zReversed();
			boolean zReversedZ = false; // no z-axis support currently

			int xDimX_1 = mvarpanelValues1.getDimX();
			int xDimY_1 = mvarpanelValues1.getDimY();
			this.getModel().getModel_Variable1().setValues( xDimX_1, xDimY_1, zReversedX, zReversedY, zReversedZ );

			if( this.getModel().getModel_Variable2() != null ){
				int xDimX_2 = mvarpanelValues2.getDimX();
				int xDimY_2 = mvarpanelValues2.getDimY();
				this.getModel().getModel_Variable2().setValues( xDimX_2, xDimY_2, zReversedX, zReversedY, zReversedZ );
			}

			int ctDims_VariableSelector1 = mvarpanelValues1.getDimCount();
			int[][] aSliceIndexes_1 = new int[ctDims_VariableSelector1 + 1][];
			String[][] aSliceCaptions_1 = new String[ctDims_VariableSelector1 + 1][];
			for( int xDim = 1; xDim <= ctDims_VariableSelector1; xDim++ ){
				aSliceIndexes_1[xDim] = mvarpanelValues1.getSliceIndex( xDim );
				int ctSlices = mvarpanelValues1.getSliceCount( xDim );
				aSliceCaptions_1[xDim] = new String[ctSlices + 1];
				for( int xSlice = 1; xSlice <= ctSlices; xSlice++ ){
					aSliceCaptions_1[xDim][xSlice] = mvarpanelValues1.getSliceCaption( xDim, xSlice );
				}
			}
			this.getModel().getModel_Variable1().setSlices( aSliceIndexes_1, aSliceCaptions_1 );

			if( this.getModel().getModel_Variable2() != null ){
				if( mvarpanelValues2.getDimCount() > 0 ){
					int ctDims_VariableSelector2 = mvarpanelValues2.getDimCount();
					int[][] aSliceIndexes_2 = new int[ctDims_VariableSelector2 + 1][];
					String[][] aSliceCaptions_2 = new String[ctDims_VariableSelector2 + 1][];
					for( int xDim = 1; xDim <= ctDims_VariableSelector2; xDim++ ){
						aSliceIndexes_2[xDim] = mvarpanelValues2.getSliceIndex( xDim );
						int ctSlices = mvarpanelValues2.getSliceCount( xDim );
						aSliceCaptions_2[xDim] = new String[ctSlices + 1];
						for( int xSlice = 1; xSlice <= ctSlices; xSlice++ ){
							aSliceCaptions_2[xDim][xSlice] = mvarpanelValues2.getSliceCaption( xDim, xSlice );
						}
					}
					this.getModel().getModel_Variable2().setSlices( aSliceIndexes_2, aSliceCaptions_2 );
				}
//			System.out.println("model info: " +  this.getModel().getModel_Variable1().sDump());
			}
		}
	}

	int getDimIndex0(DArray darray, int iOrdinal){
		int ctEffectiveDimensions = 0;
		for( int xDim = 0; xDim < darray.numDimensions(); xDim++ ){
			try {
				DArrayDimension dimCurrent = darray.getDimension(xDim);
				if( dimCurrent.getSize() > 1 ){
					ctEffectiveDimensions++;
					if( ctEffectiveDimensions == iOrdinal ) return xDim;
				}
			} catch(Exception ex) { return -1; }
		}
		return -1;
	}

	VariableDataset getDataset( StringBuffer sbError ){

		// load the model with the selected variables
		try {
//			this.mvarpanelValues1.getSelectedVS() );
//			mVariablesModel.setVariable1( modelVariable1 );
//			if( this.mzUsingSecondVariable ){
//				Model_Variable modelVariable2 = new Model_Variable( this.mvarpanelValues2.getSelectedVS() );
//				mVariablesModel.setVariable1( modelVariable2 );
//			}
		} catch( Throwable t ) {
			sbError.insert(0, "while loading model: ");
			ApplicationController.vUnexpectedError( t, sbError );
			return null;
		}
		try {

			int ctValues1 = mVariablesModel.getCount_Variable1( sbError );
			if( ctValues1 == - 1 ){
				sbError.insert(0, "unable to determine variable 1 value count: ");
				return null;
			}
			int ctValues2 = mVariablesModel.getCount_Variable2( sbError );
			boolean zUsingVariable2 = false;
			if( ctValues2 == - 1 ){
				sbError.insert(0, "unable to determine variable 2 value count: ");
				return null;
			} else if( ctValues2 == 0 ){
				zUsingVariable2 = false;
			} else {
				zUsingVariable2 = true;
			}

			// verify that there is something to plot
			if( ctValues1 == 0 ){
				sbError.append("no values specified in variable definition");
				return null;
			}

			// verify variable count matches
			if( zUsingVariable2 ){
				if( ctValues1 != ctValues2 ){
					sbError.append("values in group 1 (" + ctValues1 + ") vary in number from values in group 2 (" + ctValues2 + ")");
					return null;
				}
			}

			// collect primary dimensions
			Model_Variable var1_model = mVariablesModel.getModel_Variable1();
			VariableInfo[] list1 = var1_model.getVariableInfo( sbError );
			if( list1 == null ){
				sbError.insert(0, "failed to resolve variable: ");
				return null;
			}
			if( list1.length < 2 ){
				sbError.insert(0, "variable 1 has no members");
				return null;
			}
			VariableInfo variable_info_1 = list1[1];
			int ctDimensions = variable_info_1.getDimensionCount();

			// todo validate dimensions for each set

			int eInversion = var1_model.getInversion();

			// determine parameters (variable 1 controls)
			boolean zReversedX = var1_model.getReversedX();
			boolean zReversedY = var1_model.getReversedY();
			boolean zReversedZ = var1_model.getReversedZ();
			String sDatasetID  = var1_model.getName();
			int lenX = 1;
			int lenY = 1;
			switch( eInversion ){
				case DAP.INVERSION_XYZ:
					lenX = variable_info_1.getDimLength(1);
					lenY = variable_info_1.getDimLength(2);
					break;
				case DAP.INVERSION_YXZ:
				    lenX = variable_info_1.getDimLength(2);
				    lenY = variable_info_1.getDimLength(1);
					break;
				default:
					sbError.append("unsupported inversion");
					return null;
			}
			if( lenX * lenY <= 1 ){
				sbError.append("variable definition has no dimension");
				return null;
			}

			Model_Variable varXAxis = mVariablesModel.getModel_Axis_X();
			Model_Variable varYAxis = mVariablesModel.getModel_Axis_Y();
			String sID_x = varXAxis == null ? null : varXAxis.getName();
			String sID_y = varYAxis == null ? null : varYAxis.getName();

			// collect missing values
			Object[] eggMissing_var1 = mvarpanelValues1.getMissingEgg();
			Object[] eggMissing_var2 = zUsingVariable2 ? mvarpanelValues2.getMissingEgg() : null;

			// determine data types
			int eDataType_var1 = variable_info_1.getDataType();
			int eDataType_var2 = 0; // undefined
			VariableInfo[] list2 = null;
			if( zUsingVariable2 ){
				Model_Variable var2_model = mVariablesModel.getModel_Variable2();
				list2 = var2_model.getVariableInfo(sbError);
				if( list2 == null ){
					sbError.insert(0, "Variable two inaccessible: ");
					return null;
				}
				if( list2.length < 2 ){
					sbError.insert(0, "variable two has no members");
					return null;
				}
				eDataType_var2 = list2[1].getDataType();
			}

			// create dataset
			VariableDataset dataset = new VariableDataset();
			dataset.setID(sDatasetID);
			dataset.setMissingEgg_Var1(eggMissing_var1);
			if( zUsingVariable2 ) dataset.setMissingEgg_Var2(eggMissing_var2);
			dataset.setParameters(eDataType_var1, eDataType_var2, ctDimensions, lenX, lenY, eInversion, zReversedX, zReversedY, sID_x, sID_y);
			for( int xVar = 1; xVar <= ctValues1; xVar++ ){
				dataset.addValue(list1[xVar]);
			}
			if( zUsingVariable2 ){
				for( int xVar = 1; xVar <= ctValues2; xVar++ ){
					dataset.addValue2(list2[xVar]);
				}
			}

			// x-axis
			if( mvarpanelXAxis != null ){
				if( mvarpanelXAxis.zHasValue() ){
					AxisSpecification axisX = mvarpanelXAxis.getAxisSpecification();
					if( axisX == null ){
						ApplicationController.vShowWarning("internal error, x-axis specification was null");
					} else {
						switch( axisX.getMODE() ){
							default:
							case AxisSpecification.MODE_None:
								dataset.setAxisX(null);
								break;
							case AxisSpecification.MODE_Indexed:
								dataset.setAxisX_Indexed();
								break;
							case AxisSpecification.MODE_Vector:
								BaseType btX = axisX.getVariableSpecification().getArrayTable(sbError).bt;
								if( !( btX instanceof DArray) ){
									sbError.append("x-axis base type is not an array");
									return null;
								}
								VariableSpecification vs = axisX.getVariableSpecification();
								VariableInfo infoAxisX = new VariableInfo();
								if( zCreateVariable_Axis(infoAxisX, btX, lenX, lenX, lenY, sbError) ){
									infoAxisX.setIsReversed( mvarpanelXAxis.zReversed() );
									infoAxisX.setName( vs.getName() );
									infoAxisX.setLongName( vs.getAttribute_LongName() );
									infoAxisX.setUserCaption( null );
									infoAxisX.setUnits( vs.getAttribute_Units() );
									dataset.setAxisX(infoAxisX);
								} else {
									sbError.insert(0, "error adding x-axis variable info: ");
									return null;
								}
								break;
						}
					}
				} else {
					dataset.setAxisX(null);
				}
			}

			// y-axis
			if( mvarpanelYAxis != null ){
				if( mvarpanelYAxis.zHasValue() ){
					AxisSpecification axisY = mvarpanelYAxis.getAxisSpecification();
					if( axisY == null ){
						ApplicationController.vShowWarning("internal error, y-axis specification was null");
					} else {
						switch( axisY.getMODE() ){
							default:
							case AxisSpecification.MODE_None:
								dataset.setAxisY(null);
								break;
							case AxisSpecification.MODE_Indexed:
								dataset.setAxisY_Indexed();
								break;
							case AxisSpecification.MODE_Vector:
								BaseType btY = axisY.getVariableSpecification().getArrayTable(sbError).bt;
								if( !( btY instanceof DArray) ){
									sbError.append("y-axis base type is not an array");
									return null;
								}
								VariableSpecification vs = axisY.getVariableSpecification();
								VariableInfo infoAxisY = new VariableInfo();
								if( zCreateVariable_Axis(infoAxisY, btY, lenY, lenX, lenY, sbError) ){
									infoAxisY.setIsReversed( mvarpanelYAxis.zReversed() );
									infoAxisY.setName( vs.getName() );
									infoAxisY.setLongName( vs.getAttribute_LongName() );
									infoAxisY.setUserCaption( null );
									infoAxisY.setUnits( vs.getAttribute_Units() );
									dataset.setAxisY(infoAxisY);
								} else {
									sbError.insert(0, "error adding x-axis variable info: ");
									return null;
								}
								break;
						}
					}
				} else {
					dataset.setAxisY(null);
				}
			}

			return dataset;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return null;
		} catch( Throwable t ) { // memory errors
			sbError.append("Failed to gather data: " + t);
			return null;
		}
	}

	// note that this routine must make a duplicate array no matter what because during
	// reversal transformation elsewhere the matrix may be modified in place so you do
	// want to transform the base type primitive vector when this is done
	private boolean zCreateVariable_Axis(VariableInfo infoValues, BaseType btValues, int ctValuesExpected, int lenX, int lenY, StringBuffer sbError){

		try {
			if( infoValues == null ){
				sbError.append("system error, no variable info structure");
				return false;
			}
			if( !( btValues instanceof DArray) ){
				sbError.append("base type is not an array");
				return false;
			}
			DArray darray = (DArray)btValues;

			// do any necessary type conversions
			int eVALUE_TYPE = DAP.getDArrayType(darray);
			if( eVALUE_TYPE == 0 ){
				sbError.append("unsupportable array type");
				return false;
			}
			Object oValues = darray.getPrimitiveVector().getInternalStorage();
			int ctValues;
			int xValue;
			int ctDimensions = 1;
			byte[] abValues = null;
			short[] ashValues = null;
			int[] aiValues = null;
			long[] anValues = null;
			switch( eVALUE_TYPE ){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:
					if( eVALUE_TYPE == DAP.DATA_TYPE_Byte ){
						abValues = (byte[])oValues;
						ctValues = abValues.length;
					} else {
						ashValues = (short[])oValues;
						ctValues = ashValues.length;
					}
					if( !Utility.zMemoryCheck(ctValues, 2, sbError) ) return false;
					short[] ashTransformedValues0 = new short[ctValues];
					if( eVALUE_TYPE == DAP.DATA_TYPE_Byte ){
					    for( xValue = 0; xValue < ctValues; xValue++ )
							ashTransformedValues0[xValue] = (short)((short)abValues[xValue] & 0xFF);
					} else {
					    for( xValue = 0; xValue < ctValues; xValue++ )
							ashTransformedValues0[xValue] = ashValues[xValue];
					}
					infoValues.setValues(ashTransformedValues0, eVALUE_TYPE, ctDimensions, lenX, lenY);
					break;
				case DAP.DATA_TYPE_UInt16:
				case DAP.DATA_TYPE_Int32:
					if( eVALUE_TYPE == DAP.DATA_TYPE_UInt16 ){
						ashValues = (short[])oValues;
						ctValues = ashValues.length;
					} else {
						aiValues = (int[])oValues;
						ctValues = aiValues.length;
					}
					if( !Utility.zMemoryCheck(ctValues, 4, sbError) ) return false;
					int[] aiTransformedValues0 = new int[ctValues];
					if( eVALUE_TYPE == DAP.DATA_TYPE_UInt16 ){
						for( xValue = 0; xValue < ctValues; xValue++ )
							aiTransformedValues0[xValue] = (int)((int)ashValues[xValue] & 0xFFFF);
					} else {
						for( xValue = 0; xValue < ctValues; xValue++ )
							aiTransformedValues0[xValue] = aiValues[xValue];
					}
					infoValues.setValues(aiTransformedValues0, eVALUE_TYPE, ctDimensions, lenX, lenY);
					break;
				case DAP.DATA_TYPE_UInt32:
					aiValues = (int[])oValues;
					ctValues = aiValues.length;
					if( !Utility.zMemoryCheck(ctValues, 8, sbError) ) return false;
					long[] anTransformedValues0 = new long[ctValues];
					for( xValue = 0; xValue < ctValues; xValue++ ){
						anTransformedValues0[xValue] = (long)((long)aiValues[xValue] & 0xFFFFFFFF);
					}
					infoValues.setValues(anTransformedValues0, ctDimensions, lenX, lenY);
					break;
				case DAP.DATA_TYPE_Float32:
					float[] afValues = (float[])oValues;
					ctValues = afValues.length;
					if( !Utility.zMemoryCheck(ctValues, 4, sbError) ) return false;
					float[] afTransformedValues0 = new float[ctValues];
					for( xValue = 0; xValue < ctValues; xValue++ ){
						afTransformedValues0[xValue] = afValues[xValue];
					}
					infoValues.setValues(afTransformedValues0, ctDimensions, lenX, lenY);
					break;
				case DAP.DATA_TYPE_Float64:
					double[] adValues = (double[])oValues;
					ctValues = adValues.length;
					if( !Utility.zMemoryCheck(ctValues, 8, sbError) ) return false;
					double[] adTransformedValues0 = new double[ctValues];
					for( xValue = 0; xValue < ctValues; xValue++ ){
						adTransformedValues0[xValue] = adValues[xValue];
					}
					infoValues.setValues(adTransformedValues0, ctDimensions, lenX, lenY);
					break;
				default:
					sbError.append("unknown array data type: " + eVALUE_TYPE);
	    			return false;
			}
			return true;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return false;
		}
	}

}

class VSelector_Plot_Values extends JPanel {
	VSelector_Plot_Schematic myParent;
	private Model_Variable mVariableModel;
	String msTitle;
	JComboBox jcbValue;
	//VariableSpecification[] maVariables1;
	String[] masVariableName1;
	Panel_MissingValues panelMissingValues;
	int meClass;
	final JPanel panelDims = new JPanel();
	final ArrayList listDims = new ArrayList();
	final JRadioButton jrbDimNone_X = new JRadioButton();
	final JRadioButton jrbDimNone_Y = new JRadioButton();
	final JTextArea jtaInfo = new JTextArea();
	DefaultComboBoxModel cbmValues;
	int mctDimensions;
	boolean mzAllowCaptions = false;
	boolean mzNoneAreIndexes = false;
	boolean mzXonly = false;
	boolean mzYonly = false;
	DAS mDAS;
	VSelector_Plot_Values( VSelector_Plot_Schematic parent, Model_Variable model ){
		myParent = parent;
		this.mVariableModel = model;
	}
	boolean zInitialize( String sTitle, String[] asVariableLabels1, DAS das, final boolean zShowTransforms, final int ctDimensions, final boolean zAllowCaptions, final boolean zNoneAreIndexes, final boolean zXonly, final boolean zYonly, StringBuffer sbError ){

		try {
			setOpaque(false);
			setBackground(Color.WHITE);
			msTitle = sTitle;
			mctDimensions = ctDimensions;
			mzAllowCaptions  = zAllowCaptions;
			mzNoneAreIndexes  = zNoneAreIndexes;
			mzXonly = zXonly;
			mzYonly = zYonly;
			if( asVariableLabels1 == null ){ asVariableLabels1 = new String[1]; }
			asVariableLabels1[0] = "[none]";
			mDAS = das;
			masVariableName1 = asVariableLabels1;
			JLabel jlabelTitle = new JLabel(msTitle);
			jlabelTitle.setFont(Styles.fontSansSerifBold12);
			jcbValue = new JComboBox();
			cbmValues = new DefaultComboBoxModel();
			panelMissingValues = new Panel_MissingValues(this);
			setValueLabels(asVariableLabels1);
			jcbValue.setModel(cbmValues);
			jcbValue.setOpaque(false);
			setLayout(new GridBagLayout());
			setMinimumSize(new Dimension(300, 200));
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = gbc.BOTH;
			gbc.weighty = 0; gbc.weightx = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.gridy = 0;
			add( Box.createVerticalStrut(2), gbc );
			gbc.gridy = 1;
			add( jlabelTitle, gbc );
			gbc.gridy = 2;
			add( Box.createVerticalStrut(4), gbc );
			gbc.gridy = 3;
			add(jcbValue, gbc);
			gbc.gridy = 4;
			add( Box.createVerticalStrut(4), gbc );
			gbc.gridy = 5;
			add(panelMissingValues, gbc);
			gbc.gridy = 6;
			add( Box.createVerticalStrut(4), gbc );
			gbc.gridy = 7;
			add(panelDims, gbc);
			gbc.gridy = 8; gbc.weighty = 1;
			add(Box.createVerticalGlue(), gbc);

			// dimensional panel
			panelDims.setOpaque(false);
			panelDims.setLayout(new GridBagLayout());
			panelDims.setMinimumSize(new Dimension(300, 50));
			jrbDimNone_X.setOpaque(false);
			jrbDimNone_Y.setOpaque(false);
			ActionListener listenDimNone =
				new ActionListener(){
					public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
						vUpdateDimConstraints();
					}
				};
			jrbDimNone_X.addActionListener(listenDimNone);
			jrbDimNone_Y.addActionListener(listenDimNone);

			// if this is a value matrix it must dynamically cause an axis recalc if it changes
			jcbValue.addActionListener(
				new ActionListener(){
				    public void actionPerformed(ActionEvent e) {
						StringBuffer sbError = new StringBuffer();
						VariableSpecification vs = getSelectedVS( sbError );
						if( vs == null && sbError.length() > 0 ){
							ApplicationController.getInstance().vShowError("Error updating info: " + sbError);
							sbError.setLength(0);
						}
						VSelector_Plot_Values.this.getModel().setVariableSpecfication( vs );
						vUpdateInfo( vs );
						myParent.vRecalcAxes();
						myParent.vUpdateModels();
					}});
			return true;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return false;
		}
	}

	Model_Variable getModel(){ return this.mVariableModel; }

	// todo review this functionality
	boolean zUseEntireVariable(){ return mctDimensions == 0; }

	// The basic idea here is that both X and Y cannot be selected for the same
	// dim so the logic below moves the selection to the next radio button in the
	// list (or to none if there are no more). A button group cannot be used for
	// this purpose because these controls are already in a button group for
	// the column. Likewise if the values must be one-dimensional only an X or a Y
	// can be selected, not both.
	void vUpdateDimRadios(JRadioButton jrbSelected){
		int ctDims = listDims.size();
		if( (mctDimensions == 1) || (ctDims < 2) ){
			int eState = 0; // none

			// determine what kind of axis was selected
			for( int xDim = 1; xDim <= ctDims; xDim++ ){
				Panel_Dimension dim = (Panel_Dimension)listDims.get(xDim-1);
				if( jrbSelected == dim.com_jrbX ){
					eState = 1; // X
					break;
				} else if( jrbSelected == dim.com_jrbY ){
					eState = 2; // Y
					break;
				}
			}

			// set the other axis off if it is set
			switch( eState ){
				case 0: // none
					break;
				case 1: // X
					setDimY(0); break;
				case 2: // Y
					setDimX(0); break;
			}
		} else {
			for( int xDim = 1; xDim <= ctDims; xDim++ ){
				Panel_Dimension dim = (Panel_Dimension)listDims.get(xDim-1);
				if( jrbSelected == dim.com_jrbX ){
					if( dim.com_jrbY.isSelected() ){
						dim.com_jrbY.setSelected(false);
						int xNewDimY = xDim + 1;
						if( xNewDimY > ctDims ) xNewDimY = 1;
						if( xNewDimY == xDim ){
							setDimY(0);
						} else {
							setDimY(xNewDimY);
						}
					}
				} else if( jrbSelected == dim.com_jrbY ){
					if( dim.com_jrbX.isSelected() ){
						dim.com_jrbX.setSelected(false);
						int xNewDimX = xDim + 1;
						if( xNewDimX > ctDims ) xNewDimX = 1;
						if( xNewDimX == xDim ){
							setDimX(0);
						} else {
							setDimX(xNewDimX);
						}
					}
				}
			}
		}
		vUpdateDimConstraints();
	}
	void vUpdateDimConstraints(){
		int ctDims = listDims.size();
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Dimension dim = (Panel_Dimension)listDims.get(xDim-1);
			boolean zDimSelected = dim.com_jrbX.isSelected() || dim.com_jrbY.isSelected();
			if( zDimSelected ){
				dim.com_jtfConstraint.setVisible( false ); // constraints are only shown for non-selected dims
			} else {
				dim.com_jtfConstraint.setVisible( true );
				if( dim.com_jtfConstraint.getText().length() == 0 ) dim.setConstraint("1"); // default is first slice
				dim.com_jtfConstraint.setText( dim.getModel().getConstraint() );
			}
			dim.com_jtfConstraint.invalidate();
		}
		myParent.vRecalcAxes();
		myParent.vUpdateModels();
		revalidate();
		vUpdateOutputDimensions();
	}

	void vUpdateOutputDimensions(){
		int ctDims = listDims.size();
		int lenX = 1;
		int lenY = 1;
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Dimension dim = (Panel_Dimension)listDims.get(xDim-1);
			if( dim.com_jrbX.isSelected() ){

				lenX = dim.getModel().getSize();
			}
			if( dim.com_jrbY.isSelected() ) lenY = dim.getModel().getSize();
		}
		Panel_PlotScale panel_scale = Panel_View_Plot._getPanel_PlotScale();
		PlotScale scale = panel_scale._getScale();
		scale.setDataDimension(lenX, lenY);
	}

	int getVariableClass(){
		return meClass;
	}
	private void setValueLabels( String[] asLabels1 ){
		cbmValues.removeAllElements();
		if( mzNoneAreIndexes ) cbmValues.addElement("[index]"); else cbmValues.addElement("[none]");
		if( asLabels1 == null ) return;
		for( int xLabel = 1; xLabel < asLabels1.length; xLabel++ ){
			cbmValues.addElement(asLabels1[xLabel]);
		}
	}
	BaseType mLastBaseType = null;
	String msLastVariableName = "";
	final GridBagConstraints gbc = new GridBagConstraints();
	void vUpdateInfo( VariableSpecification vs ){
		StringBuffer sbError = new StringBuffer();
		panelDims.removeAll();
		gbc.fill = gbc.BOTH;
		gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 9;
		panelDims.add(jtaInfo, gbc);
		if( vs == null ){
			jtaInfo.setText("nothing selected");
			panelMissingValues.setEnabled(false);
			return;
		}
		ArrayTable at = vs.getArrayTable(sbError);
		if( at == null ){
			jtaInfo.setText("Error: " + sbError);
			panelDims.add(jtaInfo);
			return;
		}
		BaseType bt = at.bt;
		String sVariableName = getSelectedVariableName();
		panelMissingValues.setEnabled(true);
		StringBuffer sbInfo = new StringBuffer(80);
		if(bt instanceof DArray) {
			try {
				DArray array = (DArray)bt;
				String sType = DAP.getDArrayType_String(array);
				sbInfo.append("Data Type: " + sType);

				if( mctDimensions > 0 ){
					int ctDim = array.numDimensions();
					listDims.clear();
					ButtonGroup bgX = new ButtonGroup();
					ButtonGroup bgY = new ButtonGroup();

					// label row
					int iGridX = 0;
					gbc.gridy = 2; gbc.gridwidth = 1;
					gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalStrut(8), gbc);
	// this label seems to be superfluous so it is omitted
	//				JLabel labelDimName = new JLabel("Dimension Name");
	//				labelDimName.setOpaque(false);
					gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalGlue(), gbc);
					gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalStrut(6), gbc);
					if( mzXonly || !mzYonly ){
						JLabel labelDimX = new JLabel("X");
						labelDimX.setOpaque(false);
						gbc.gridx = iGridX++; panelDims.add(labelDimX, gbc);
						gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalStrut(4), gbc);
					}
					if( mzYonly || !mzXonly ){
						JLabel labelDimY = new JLabel("Y");
						labelDimY.setOpaque(false);
						gbc.gridx = iGridX++; panelDims.add(labelDimY, gbc);
					}
					gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalStrut(6), gbc);
					JLabel labelConstraint = new JLabel("Constraint");
					labelConstraint.setOpaque(false);
					gbc.gridx = iGridX++; panelDims.add(labelConstraint, gbc);
					gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalStrut(6), gbc);
					if( mzAllowCaptions ){
						JLabel labelCaption = new JLabel("Caption");
						labelCaption.setOpaque(false);
						gbc.gridx = iGridX++; panelDims.add(labelCaption, gbc);
					}
					gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalGlue(), gbc);

					// label top margin
					gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = iGridX;
					panelDims.add(Box.createVerticalStrut(3), gbc);

					// dim rows
					int xDim = 1;
					for( ; xDim <= ctDim; xDim++ ){
						Panel_Dimension dim  = new Panel_Dimension(array.getDimension(xDim-1));
						String sDimName   = dim.getModel().getName();
						if( sDimName == null ) sDimName = "";
						String sNameLabel = sDimName + " [" + dim.getModel().getSize() + "]";
						JLabel labelName = new JLabel(sNameLabel);
						labelName.setOpaque(false);
						bgX.add(dim.com_jrbX);
						bgY.add(dim.com_jrbY);
// System.out.println("dim " + xDim + " of " + ctDim + " named " + sDimName + " in base type " + bt.getName() + " added for variable selector " + msTitle);
						listDims.add(dim);
						gbc.gridy = xDim*2 + 2; gbc.gridwidth = 1;
						iGridX = 0;
						gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalStrut(8), gbc);
						gbc.gridx = iGridX++; panelDims.add(labelName, gbc);
						gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalStrut(6), gbc);
						if( mzXonly || !mzYonly ){
							gbc.gridx = iGridX++; panelDims.add(dim.com_jrbX, gbc);
							gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalStrut(4), gbc);
						}
						if( mzYonly || !mzXonly ){
							gbc.gridx = iGridX++; panelDims.add(dim.com_jrbY, gbc);
						}
						gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalStrut(6), gbc);
						gbc.gridx = iGridX++; panelDims.add(dim.com_jtfConstraint, gbc);
						if( mzAllowCaptions ){
							gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalStrut(6), gbc);
							gbc.gridx = iGridX++; panelDims.add(dim.com_jtfCaption, gbc);
						}
						gbc.gridx = iGridX++; panelDims.add(Box.createHorizontalGlue(), gbc);

						// header separator
						gbc.gridx = 0; gbc.gridy = xDim*2 + 1; gbc.gridwidth = iGridX;
						panelDims.add(Box.createVerticalStrut(3), gbc);
					}

					// add to button group
					bgX.add(jrbDimNone_X);
					bgY.add(jrbDimNone_Y);

					// add (none) line
					if( !mzYonly && !mzXonly ){
						gbc.gridy = xDim*2 + 2; gbc.gridwidth = 1;
						gbc.gridx = 0; panelDims.add(Box.createHorizontalStrut(8), gbc);
						gbc.gridx = 1; panelDims.add(new JLabel("(none)"), gbc);
						gbc.gridx = 2; panelDims.add(Box.createHorizontalStrut(6), gbc);
						gbc.gridx = 3; panelDims.add(jrbDimNone_X, gbc);
						gbc.gridx = 4; panelDims.add(Box.createHorizontalStrut(4), gbc);
						gbc.gridx = 5; panelDims.add(jrbDimNone_Y, gbc);
					}

					// footer glue
					gbc.gridwidth = 3;
					gbc.gridx = 6; panelDims.add(Box.createHorizontalGlue(), gbc);
				}

			} catch(Exception ex) { sbInfo.append("[error]"); }

			jtaInfo.setText(sbInfo.toString());
			vSetupRadioListeners();

			if( mctDimensions > 0 ){

				// determine initial settings
				int xDim1 = 0; int xDim2 = 0;
				int ctDims = listDims.size();
				int ctSizedDims = 0;

				// determine largest two dims
				int len1 = 0;
				int len2 = 0;
				for( int xDim = 1; xDim <= ctDims; xDim++ ){
					Panel_Dimension dim = (Panel_Dimension)listDims.get(xDim-1);
					if( dim.getModel().getSize() > len1 ){
						xDim2 = xDim1;
						xDim1 = xDim;
						len2 = len1;
						len1 = dim.getModel().getSize();
					} else if( dim.getModel().getSize() > len2 ){
						xDim2 = xDim;
						len2 = dim.getModel().getSize();
					}
				}
				if( (mctDimensions == 1) || (listDims.size() < 2) ){
					if( mzYonly ){
						if( xDim1 > 0 ) setDimY(xDim1); // the largest dim is y
						setDimX(0);
					}
					if( mzXonly ){
						if( xDim1 > 0 ) setDimX(xDim1); // the largest dim is y
						setDimY(0);
					}
				} else {
					Panel_Dimension dim1 = (Panel_Dimension)listDims.get(xDim1-1);
					Panel_Dimension dim2 = (Panel_Dimension)listDims.get(xDim2-1);
					if( dim1.getModel().getName().toUpperCase().startsWith("LAT") ){
						setDimY(xDim1);
						setDimX(xDim2);
					} else if( dim1.getModel().getName().toUpperCase().startsWith("LON") ){
						setDimX(xDim1);
						setDimY(xDim2);
					} else if( dim2.getModel().getName().toUpperCase().startsWith("LAT") ){
						setDimX(xDim1);
						setDimY(xDim2);
					} else if( dim2.getModel().getName().toUpperCase().startsWith("LON") ){
						setDimY(xDim1);
						setDimX(xDim2);
					} else {
						if( xDim1 > 0 ) setDimX(xDim1); // the largest dim is x
						if( xDim2 > 0 ) setDimY(xDim2);
					}
				}
				vUpdateDimConstraints();
			}
		} else {
			jtaInfo.setText("not a recognized base type");
			panelDims.add(jtaInfo);
			return;
		}

/* this is not necessary
		if( at.aFieldNames != null ){ // add the field names to the info
			if( sbInfo.length() > 0 ) sbInfo.append("\n");
			sbInfo.append("Fields:\n");
			for( int xField = 1; xField < at.aFieldNames.length; xField++ ){
				sbInfo.append("  " + xField + ": " + at.aFieldNames[xField] + "\n");
			}
			jtaInfo.setText(sbInfo.toString());
			panelDims.add(jtaInfo);
		}
*/

		// TODO if for any reason the data parameters do not inititialize successfully
		// the missing values will be untyped leading to errors elsewhere, therefore
		// code should be added that will ensure the missing values are typed correctly
		// (or that the variables are unselectable)

		// updating the data parameters should be the last step because the initialization
		// of the selected dims needs to occur beforehand; this is because the shape of the
		// data dimensions depends on these selections
		if( bt != mLastBaseType && !msLastVariableName.equals(sVariableName) ){
			vUpdateDataParameters();
		}
		mLastBaseType = bt;
		msLastVariableName = (sVariableName == null) ? "" : sVariableName;

	}

	void vSetupRadioListeners(){
		ActionListener listenDimRadio =
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					JRadioButton jrbSource = (JRadioButton)actionEvent.getSource();
					vUpdateDimRadios(jrbSource);
				}
			};
		FocusAdapter adapterConstraint =
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					vUpdateDimConstraints();
				}
			};
		int ctDims = listDims.size();
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Dimension dim = (Panel_Dimension)listDims.get(xDim-1);
			dim.com_jrbX.addActionListener(listenDimRadio);
			dim.com_jrbY.addActionListener(listenDimRadio);
			dim.com_jtfConstraint.addFocusListener(adapterConstraint);
		}
	}

	StringBuffer sbError = new StringBuffer(80);
	void vUpdateDataParameters(){
		StringBuffer sbError = new StringBuffer();
		VariableSpecification vs = getSelectedVS( sbError );
		if( vs == null ) return;
		ArrayTable at = vs.getArrayTable(sbError);
		if( at == null ){
			ApplicationController.vShowError("Failed to update data parameters because variable array table unavailable: " + sbError);
			sbError.setLength(0);
			return;
		}
		BaseType bt = at.bt;
		int eTYPE = DAP.getDArrayType((DArray)bt); // assumes DArray TODO
		int xDimX = getDimX();
		int xDimY = getDimY();
		DataParameters dp = Panel_View_Plot._getDataParameters();
		if( dp.zCompare(bt, xDimX, xDimY) ){
			if( panelMissingValues.getUseMissing() ){
				// the hand-entered values will be used
			} else {
				panelMissingValues.setMissingValues(dp.getMissingEgg(), eTYPE, dp.getMissingMethodCode());
			}
		} else {
			Object[] eggMissing = null;
			if( panelMissingValues.getUseMissing() ){
				eggMissing = panelMissingValues.getMissingEgg(eTYPE);
			}
			if( dp.zLoad(bt, xDimX, xDimY, eggMissing, mDAS, sbError) ){
				Panel_Dimension dimX = getDimX_Variable();
				Panel_Dimension dimY = getDimY_Variable();
				int iDimXsize = (dimX == null) ? 1 : dimX.getModel().getSize();
				int iDimYsize = (dimY == null) ? 1 : dimY.getModel().getSize();

				// update axes settings
				Panel_PlotAxes axes = Panel_View_Plot._getPanel_PlotAxes();
				axes.getAxisParameters_X().vSetDimSize(iDimXsize);
				axes.getAxisParameters_Y().vSetDimSize(iDimYsize);

				// update scale settings
				PlotScale scale = Panel_View_Plot._getPanel_PlotScale()._getScale();
				if( scale == null ){
					// ApplicationController.vShowWarning("no plot scale exists during parameters update");
				} else {
					scale.setDataDimension( iDimXsize, iDimYsize );
				}
				if( panelMissingValues.getUseMissing() ){
					// the hand-entered values will be used
				} else {
					panelMissingValues.setMissingValues(dp.getMissingEgg(), eTYPE, dp.getMissingMethodCode());
				}
			} else {
				sbError.insert(0, "Failed to load data parameters: ");
				ApplicationController.vShowError(sbError.toString());
			}
		}
	}
	void setSelection( int index0 ){
		if( jcbValue == null ) return;
		if( index0 < 0 ) return;
		if( index0 > jcbValue.getItemCount() - 1 ) return;
		jcbValue.setSelectedIndex(index0);
	}
	Object[] getMissingEgg(){
		StringBuffer sbError = new StringBuffer();
		VariableSpecification vs = getSelectedVS( sbError );
		if( vs == null ) return null;
		ArrayTable at = vs.getArrayTable(sbError);
		if( at == null ) return null;
		BaseType btSelected = at.bt;
		if( btSelected == null ) return null;
		int eTYPE;
		if( btSelected instanceof DArray ){
			eTYPE = DAP.getDArrayType((DArray)btSelected);
		} else if( btSelected instanceof DArray ){
			DSequence dsequence = (DSequence)btSelected;
			String sField = getSelectedVariableName();
			try {
				BaseType btField = dsequence.getVariable(sField);
				eTYPE = DAP.getType_Plot(btField);
			} catch(Exception ex) { return null; }
		} else {
			return null;
		}
		return panelMissingValues.getMissingEgg(eTYPE);
	}
	VariableSpecification getSelectedVS( StringBuffer sbError ){
		if( jcbValue == null ){
			sbError.append( "internal error, variable combo box does not exist" );
			return null;
		}
		int indexSelected = jcbValue.getSelectedIndex();
		if( indexSelected == -1 ){
			sbError.append( "variable combo box is unitialized" );
			return null;
		}
		if( indexSelected == 0 ){
			return null;
		}
		return myParent.getVariable1( indexSelected, sbError );
	}
	String getSelectedVariableName(){
		if( jcbValue == null ) return null;
		int indexSelected = jcbValue.getSelectedIndex();
		if( indexSelected == -1 ) return null;
		if( indexSelected == 0 ) return null;
		return masVariableName1[indexSelected];
	}
	String getTitle(){ if( msTitle == null ) return ""; else return msTitle; }
	boolean zHasValue(){ if( jcbValue == null ) return false; return jcbValue.getSelectedIndex() > 0; }

	Panel_Dimension getDim(int xDim){
		if( xDim == 0 ) return null;
		return (Panel_Dimension)listDims.get(xDim-1);
	}

	Panel_Dimension getDimX_Variable(){
		int xDim = getDimX();
		if( xDim == 0 ) return null;
		return (Panel_Dimension)listDims.get(xDim-1);
	}

	Panel_Dimension getDimY_Variable(){
		int xDim = getDimY();
		if( xDim == 0 ) return null;
		return (Panel_Dimension)listDims.get(xDim-1);
	}

	/** 0 means none was selected */
	int getDimX(){
		int ctDims = listDims.size();
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Dimension dim = (Panel_Dimension)listDims.get(xDim-1);
			if( dim.com_jrbX.isSelected() ) return xDim;
		}
		return 0;
	}

	/** 0 means none was selected */
	int getDimY(){
		int ctDims = listDims.size();
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Dimension dim = (Panel_Dimension)listDims.get(xDim-1);
			if( dim.com_jrbY.isSelected() ) return xDim;
		}
		return 0;
	}

	// supply 0 to set the X to (none)
	void setDimX( int xDim ){
		if( xDim == 0 ){
			jrbDimNone_X.setSelected(true);
	    } else {
			((Panel_Dimension)listDims.get(xDim - 1)).com_jrbX.setSelected(true);
		}
	}
	void setDimY( int xDim ){
		if( xDim == 0 ){
			jrbDimNone_Y.setSelected(true);
	    } else {
			((Panel_Dimension)listDims.get(xDim - 1)).com_jrbY.setSelected(true);
		}
	}
	int getDimCount(){ return listDims.size(); }
	int[] getSliceIndex( int xDim1 ){
		Panel_Dimension dim = ((Panel_Dimension)listDims.get(xDim1 - 1));
		return dim.getModel().getSliceIndexes1();
	}
	String getSliceCaption( int xDim1, int xSlice1 ){
		Panel_Dimension dim = ((Panel_Dimension)listDims.get(xDim1 - 1));
		return dim.getModel().getSliceCaption(xSlice1);
	}
	int getSliceCount( int xDim1 ){
		Panel_Dimension dim = ((Panel_Dimension)listDims.get(xDim1 - 1));
		return dim.getModel().getSliceCount();
	}
}

class VSelector_Plot_Axis extends JPanel {
	VSelector_Plot_Schematic myParent;
	private Model_Variable mVariableModel;
	String msTitle;
	JComboBox jcbMapVector;
	JCheckBox jcheckReverse;
	JTextArea jtaInfo;
	VariableSpecification[] avsMapVectors;
	DefaultComboBoxModel cbmMapVectors;
	VSelector_Plot_Axis( VSelector_Plot_Schematic parent, Model_Variable model ){
		myParent = parent;
		this.mVariableModel = model;
	}
	Model_Variable getModel(){ return this.mVariableModel; }
	boolean zInitialize( String sTitle, String[] asLabels1, VariableSpecification[] aVariables1, final boolean zShowTransforms, StringBuffer sbError ){
		try {
			setOpaque(false);
			msTitle = sTitle;
			if( asLabels1 == null ){ asLabels1 = new String[1]; }
			if( aVariables1 == null ){ aVariables1 = new VariableSpecification[1]; }
			if( asLabels1.length != aVariables1.length ){ sbError.append("input arrays not the same size"); return false; }
			asLabels1[0] = "[none]";
			avsMapVectors = aVariables1;
			JLabel jlabelTitle = new JLabel(msTitle);
			jlabelTitle.setFont(Styles.fontSansSerifBold12);
			jcbMapVector = new JComboBox();
			cbmMapVectors = new DefaultComboBoxModel();
			vAddMapLabels(asLabels1);
			jcbMapVector.setModel(cbmMapVectors);
			jcbMapVector.setOpaque(false);
			jcheckReverse = new JCheckBox("Reverse");
			jcheckReverse.setOpaque(false);
			jcheckReverse.addActionListener(
				new ActionListener(){
				    public void actionPerformed(ActionEvent e) {
						vUpdateAxisInfo();
					}});
			jtaInfo = new JTextArea();
			jtaInfo.setEditable(false);
			setLayout(new GridBagLayout());
			setMinimumSize(new Dimension(300, 200));
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = gbc.BOTH;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 1; gbc.weighty = 1;
			gbc.gridy = 0;
			gbc.gridy = 1;
			add( jlabelTitle, gbc );
			gbc.gridy = 2; gbc.gridwidth = 1; gbc.weighty = 0; gbc.weightx = 0;
			add(jcbMapVector, gbc);
			gbc.gridy = 3; gbc.gridwidth = 1; gbc.weighty = 0; gbc.weightx = 0;
			if( zShowTransforms ){
				add(jcheckReverse, gbc);
			}
			gbc.gridx = 1;
			add(Box.createGlue(), gbc);
			gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
			add(jtaInfo, gbc);

			// if this is a value matrix it must dynamically cause an axis recalc if it changes
			jcbMapVector.addActionListener(
				new ActionListener(){
				    public void actionPerformed(ActionEvent e) {
						vEnsureAscendingValues();
						vUpdateAxisInfo();
					}});
			return true;
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return false;
		}
	}
	private void vAddMapLabels( String[] asLabels1 ){
		cbmMapVectors.removeAllElements();
		cbmMapVectors.addElement("[none]");
		cbmMapVectors.addElement("[index]");
		if( asLabels1 == null ) return;
		for( int xLabel = 1; xLabel < asLabels1.length; xLabel++ ){
			if( asLabels1[xLabel] == null ) cbmMapVectors.addElement("");
			else cbmMapVectors.addElement(asLabels1[xLabel]);
		}
	}
	BaseType mLastBaseType = null;
	void vUpdateAxisInfo(){
		AxisSpecification axis = getAxisSpecification();
		if( axis == null ){
			return;
		}
		switch( axis.getMODE() ){
			default:
			case AxisSpecification.MODE_None:
				jtaInfo.setText("");
	    		break;
			case AxisSpecification.MODE_Indexed:
				jtaInfo.setText("");
	    		break;
			case AxisSpecification.MODE_Vector:
				jtaInfo.setText( getAxisInfo(axis) );
				break;
		}
		myParent.vUpdateModels();
	}

	String getAxisInfo( AxisSpecification axis ){
		StringBuffer sbInfo = new StringBuffer(80);
	    BaseType bt;
		VariableSpecification vs = axis.getVariableSpecification();
		ArrayTable at = vs.getArrayTable(sbInfo);
		if( at == null ){
			return "Error constructing flat data: " + sbInfo;
		}
		bt = at.bt;
		mLastBaseType = bt;
		String sName = vs.getAttribute_LongName();
		String sUnits = vs.getAttribute_Units();
		sbInfo.append(sName + (sUnits == null ? "" : " (" + sUnits + ")"));
		if( bt instanceof DArray ) { // normal case
			try {
				DArray array = (DArray)bt;
				String sType = DAP.getDArrayType_String(array);
				int ctDim = array.numDimensions();
				int lenDim = array.getDimension(0).getSize();
				String sValueFrom;
				String sValueTo;
				String sIncrement;
				if( zReversed() ){
					sValueTo = DAP.getDArrayValueString(array, 1, 0, 0);
					sValueFrom = DAP.getDArrayValueString(array, lenDim, 0, 0);
				} else {
					sValueFrom = DAP.getDArrayValueString(array, 1, 0, 0);
					sValueTo = DAP.getDArrayValueString(array, lenDim, 0, 0);
				}
				double dFrom = Double.parseDouble(sValueFrom);
				double dTo = Double.parseDouble(sValueTo);
				double diff = dTo - dFrom;
				double dIncrement = diff / lenDim;
				if( diff < 0 ) diff *= -1;
				int iOrder = (int)(Math.log(diff) / Math.log(10));
				int iIncrementOrder = iOrder - 2;
				if( iIncrementOrder >= 0 ){
					sIncrement = Utility.sDoubleToRoundedString(dIncrement, 0);
				} else {
					int iDecimalPlaces = iIncrementOrder * -1;
					sIncrement = Utility.sDoubleToRoundedString(dIncrement, iDecimalPlaces);
				}
				try {
					if( sValueFrom.length() > 8 || sValueTo.length() > 8 ){
						if( iOrder >= 0 ){
							sValueFrom = Utility.sDoubleToRoundedString(dFrom, 0);
							sValueTo = Utility.sDoubleToRoundedString(dTo, 0);
						} else {
							int iDecimalPlaces = iOrder * -1;
							sValueFrom = Utility.sDoubleToRoundedString(dFrom, iDecimalPlaces);
							sValueTo = Utility.sDoubleToRoundedString(dTo, iDecimalPlaces);
						}
					}
				} catch(Exception ex) {}
				sbInfo.append("from ").append(sValueFrom).append(" to ").append(sValueTo);
				sbInfo.append("\nincrement: ").append(sIncrement);
			} catch(Exception ex) { sbInfo.append("[error]"); }
		}
		return sbInfo.toString();
	}

	StringBuffer sbError = new StringBuffer(80);
	AxisSpecification getAxisSpecification(){
		if( jcbMapVector == null ) return null;
		int indexSelected = jcbMapVector.getSelectedIndex();
		if( indexSelected == -1 ) return new AxisSpecification( AxisSpecification.MODE_None );
		if( indexSelected == 0 ) return new AxisSpecification( AxisSpecification.MODE_None );
		if( indexSelected == 1 ) return new AxisSpecification( AxisSpecification.MODE_Indexed );
		return new AxisSpecification( avsMapVectors[indexSelected - 1] );
	}
	String getTitle(){ if( msTitle == null ) return ""; else return msTitle; }
	boolean zHasValue(){ if( jcbMapVector == null ) return false; return jcbMapVector.getSelectedIndex() > 0; }
	boolean zReversed(){ if( jcheckReverse == null ) return false; else if( !jcheckReverse.isVisible() ) return false; else return jcheckReverse.isSelected(); }

	/** When the variable selector is unable to automatically determine which variables should
	 *  be used for an axis then it must discover which ones are possible candidates so it
	 *  calls this function to do that.
	 */
	boolean zSetData( DataDDS ddds, DAS das, int size, StringBuffer sbError ){
		if( myParent.mPlotVariables == null ){
			myParent.mPlotVariables = new PlotVariables();
			if( !myParent.mPlotVariables.zLoadVariables(ddds, das, size, sbError) ){
				sbError.insert(0, "failed to load variables");
				return false;
			}
		}
		String[] asLabels1 = myParent.mPlotVariables.getLabels();
		VariableSpecification[] aVariables1 = myParent.mPlotVariables.getAllVariableSpecifications();
		if( asLabels1 == null ){ sbError.append("labels missing"); return false; }
		if( aVariables1 == null ){ sbError.append("variables missing"); return false; }
		if( asLabels1.length != aVariables1.length ){ sbError.append("input arrays not the same size"); return false; }
		vAddMapLabels(asLabels1);
		avsMapVectors = aVariables1;
		vEnsureAscendingValues();
		vUpdateAxisInfo();
		return true;
	}
	void vSetBlank(){
		vAddMapLabels(null);
		jcheckReverse.setSelected(false);
		jcbMapVector.setEnabled( false );
		jtaInfo.setText("");
	}
	boolean vSetFixed( String sLabel, VariableSpecification vs ){
		String[] asLabel = new String[2];
		asLabel[1] = sLabel;
		vAddMapLabels(asLabel);
		avsMapVectors = new VariableSpecification[2];
		avsMapVectors[1] = vs;
		jcbMapVector.setSelectedIndex(2);
		vEnsureAscendingValues();
		vUpdateAxisInfo();
		return true;
	}

	/** The axis should have values ascending from bottom to top and left to right.
	 *  This routine will check the reverse box if it finds that the currently
	 *  selected map vector has descending values.
	 */
	StringBuffer msbError = new StringBuffer();
	void vEnsureAscendingValues(){
		AxisSpecification axis = getAxisSpecification();
		if( axis == null ) return; // no need to reverse
		if( axis.getMODE() != AxisSpecification.MODE_Vector ) return; // no need to reverse
	    BaseType bt = axis.getVariableSpecification().getArrayTable(msbError).bt;
		if( bt == null ) return;
		if( bt instanceof DArray ) { // normal case
			try {
				DArray array = (DArray)bt;
				String sType = DAP.getDArrayType_String(array);
				int ctDim = array.numDimensions();
				int lenDim = array.getDimension(0).getSize();
				String sValueFrom;
				String sValueTo;
				String sIncrement;
				sValueFrom = DAP.getDArrayValueString(array, 1, 0, 0);
				sValueTo = DAP.getDArrayValueString(array, lenDim, 0, 0);
				double dFrom = Double.parseDouble(sValueFrom);
				double dTo = Double.parseDouble(sValueTo);
				if( dTo > dFrom ){ // normal
					jcheckReverse.setSelected(false);
				} else { // the map vector is reversed
					jcheckReverse.setSelected(true);
				}
			} catch(Exception ex) {
				// errors are ignored
			}
		}
	}
}

class Panel_MissingValues extends JPanel {
	private JCheckBox checkUseMissing = new JCheckBox(); // if this is off then it is as if the egg is null
	private JButton buttonCalculateMissingValues;
	private JButton buttonClearMissingValues;
	private JTextField jtfMissingValues = new JTextField(20);
	private JLabel jlabelMethodCode = new JLabel();
	VSelector_Plot_Values mOwner;
	private Object[] meggMissing = null;
	private int meTYPE = 0;
	boolean getUseMissing(){ return checkUseMissing.isSelected(); }
	Panel_MissingValues(VSelector_Plot_Values owner){
		mOwner = owner;
		setOpaque(false);
		checkUseMissing.setOpaque(false);
		ImageIcon iiCalculator = Resources.imageiconCalculator;
		if( iiCalculator == null ){
			buttonCalculateMissingValues = new JButton("calc");
		} else {
			buttonCalculateMissingValues = new JButton(iiCalculator);
		}
		checkUseMissing.addActionListener(
			new java.awt.event.ActionListener(){
			    public void actionPerformed( java.awt.event.ActionEvent ae ){
					if( checkUseMissing.isSelected() ){
						Panel_MissingValues.this.jlabelMethodCode.setText("M"); // manual
					} else {
						DataParameters dp = Panel_View_Plot._getDataParameters();
						int eTYPE = dp.getTYPE();
						setMissingValues(dp.getMissingEgg(), eTYPE, dp.getMissingMethodCode());
					}
				}
			}
		);
		jtfMissingValues.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					checkUseMissing.setSelected( true );
					setMissingString( jtfMissingValues.getText(), meTYPE, "M" );
				}
			}
		);
		jtfMissingValues.addKeyListener(
	    	new java.awt.event.KeyAdapter(){
		    	public void keyPressed(java.awt.event.KeyEvent ke){
			    	if( ke.getKeyCode() == ke.VK_ENTER ){
						checkUseMissing.setSelected(true);
						setMissingString( jtfMissingValues.getText(), meTYPE, "M" );
	    			}
		    	}
			}
		);
		buttonCalculateMissingValues.setMargin( new Insets(1, 1, 1, 1) );
		buttonCalculateMissingValues.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					StringBuffer sbError = new StringBuffer(80);
					VariableSpecification vs = mOwner.getSelectedVS( sbError );
					if( vs == null ) return;
					ArrayTable at = vs.getArrayTable(sbError);
					if( at == null ) return;
					BaseType btValues = at.bt;
					DataParameters dp = Panel_View_Plot._getDataParameters();
					if( dp.zLoad(btValues, mOwner.getDimX(), mOwner.getDimY(), null, null, sbError) ){
						int iDataWidth = dp.getDim1Length();
					} else {
						ApplicationController.vShowError("Failed to calculate data parameters: " + sbError);
						return;
					}
					Object[] eggMissing = dp.getMissingEgg();
					if( eggMissing == null ){
						ApplicationController.vShowError("Missing values missing from parameters: " + sbError);
					} else {
						int ctMissing = DAP.getArraySize(eggMissing[0]) - 1;
						if( ctMissing < 0 ){
							ApplicationController.vShowError("Error counting missing values");
						} else {
							ApplicationController.vShowStatus("Found " + ctMissing + " missing values for " + btValues.getLongName());
							Panel_MissingValues.this.checkUseMissing.setSelected(true);
							Panel_MissingValues.this.setMissingValues( eggMissing, dp.getTYPE(), dp.getMissingMethodCode() );
						}
					}
				}
			}
		);
		buttonClearMissingValues = new JButton("CLR");
		buttonClearMissingValues.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					checkUseMissing.setSelected(true);
					setMissingString(null, Panel_MissingValues.this.meTYPE, "M");
				}
			}
		);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(checkUseMissing);
		add(Box.createHorizontalStrut(3));
		add(jtfMissingValues);
		add(Box.createHorizontalStrut(3));
		add(jlabelMethodCode);
		add(Box.createHorizontalStrut(3));
		add(buttonCalculateMissingValues);
		add(Box.createHorizontalStrut(3));
		add(buttonClearMissingValues);
	}
	Object[] getMissingEgg(int eTYPE){
		if( eTYPE == meTYPE ){
			if( checkUseMissing.isSelected() ){
				setMissingString( jtfMissingValues.getText(), eTYPE, "M" );
			}
			return meggMissing;
		} else { // missing values are of wrong type // should not happen
			ApplicationController.vShowError("Internal Error, missing value type mismatch [" + meTYPE + " : " + eTYPE + "]");
			return null;
		}
	}
	public void setEnabled( boolean z ){
		checkUseMissing.setEnabled(z);
		buttonCalculateMissingValues.setEnabled(z);
		jtfMissingValues.setEnabled(z);
		if( z ){
			if( meggMissing == null ){ // we don't know if there are missing values or not
				// do nothing
			} else {
				jtfMissingValues.setText(DAP.getValueString(meggMissing, meTYPE));
		    }
	    } else {
			jtfMissingValues.setText("");
		}
	}
	void setMissingString( String sMissing, int eTYPE, String sMethodCode ){
		StringBuffer sbError = new StringBuffer(80);
		try {
			String[] asMissing = null;
			int ctMissing = 0;
			if( sMissing != null ) if( sMissing.length() > 0 ){
				asMissing = Utility_String.splitCommaWhiteSpace(sMissing);
				ctMissing = asMissing.length;
			}
			double[] adMissing1 = new double[ctMissing + 1];
			int xMissing = 1;
			for( int xString = 0; xString < ctMissing; xString++ ){
				try {
					adMissing1[xMissing] = Double.parseDouble(asMissing[xString]);
					xMissing++;
				} catch(Exception ex) {
					ApplicationController.vShowWarning("missing string ignored because of parse error on " + xString + ": " + asMissing[xString]);
				}
			}
			if( xMissing <= ctMissing ){ // then some values were ignored, shrink array to fit
				double[] adBuffer1 = new double[xMissing + 1];
				System.arraycopy(adMissing1, 0, adBuffer1, 0, xMissing + 1);
				adMissing1 = adBuffer1;
			}
			// this is done because Java does not support unsigned bytes so all bytes
			// are converted to shorts
			int eJAVA_TYPE;
			switch( eTYPE ){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:
					eJAVA_TYPE = DAP.JAVA_TYPE_short;
					break;
				case DAP.DATA_TYPE_Int32:
				case DAP.DATA_TYPE_UInt16:
					eJAVA_TYPE = DAP.JAVA_TYPE_int;
					break;
				case DAP.DATA_TYPE_UInt32:
					eJAVA_TYPE = DAP.JAVA_TYPE_long;
					break;
				case DAP.DATA_TYPE_Float32:
					eJAVA_TYPE = DAP.JAVA_TYPE_float;
					break;
				case DAP.DATA_TYPE_Float64:
					eJAVA_TYPE = DAP.JAVA_TYPE_double;
					break;
				default:
					ApplicationController.vShowError("Error converting missing doubles to " + eTYPE + " (Unsupportable data type)");
	    			return;
			}
			Object[] eggMissing = DAP.convertToEgg_Java(adMissing1, eJAVA_TYPE, sbError);
			if( eggMissing == null ){
				ApplicationController.vShowError("Error converting missing doubles to " + eTYPE + ": " + sbError);
			}
 			Panel_View_Plot._getDataParameters().setMissingEgg(eggMissing); // need to set dp egg so that NewCS creation can pick up manually generated egg
			setMissingValues( eggMissing, eTYPE, sMethodCode );
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError("While setting missing text: ");
			meggMissing = null;
			jtfMissingValues.setText("");
		}
	}
	void setMissingValues( Object[] eggMissing, int eTYPE, String sMethodCode ){
		String sMissingText;
		if( eggMissing == null ){ // we don't know if there are missing values or not
			sMissingText = "";
		} else {
			meggMissing = eggMissing;
			meTYPE = eTYPE;
			sMissingText = DAP.getValueString(eggMissing, eTYPE);
		}
		jtfMissingValues.setText(sMissingText);
		Panel_View_Plot._getDataParameters().setMissingEgg(eggMissing);
		if( sMethodCode.equals("M") ){
			checkUseMissing.setSelected(true);
			Panel_View_Plot._getPanel_ColorSpecification().setMissingValueText(sMissingText); // if the missing string was changed manually then update the color spec also
		}
		jlabelMethodCode.setText(sMethodCode);
	}
}


