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
 * Title:        Output_ToPlot
 * Description:  Methods to generate plotting output
 * Copyright:    Copyright (c) 2002-2012
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.08
 */

import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.print.*;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import opendap.clients.odc.*;
import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.data.Script;
import opendap.clients.odc.gui.Resources;
import opendap.clients.odc.plot.PlotEnvironment.PLOT_TYPE;
import opendap.dap.*;

/** The plot types have the following requirements:

pseudocolor/contour:
	value matrix 2D
	optional x-axis 1D (must match value width)
	optional y-axis 1D (must match value height)

vector:
	value matrix U: 2D (these two must match each other)
	value matrix V: 2D
	optional x-axis 1D (must match value width)
	optional y-axis 1D (must match value height)

histogram:
	value matrix (any dimensions)

scatter:
	value matrix 1D
	optional x- or y-axis 1D (must match value matrix length)

line:
	one or more value matrix 1D
	optional x- or y-axis 1D (must match value matrix(s) length)
*/

public class Output_ToPlot {

    public enum OutputTarget {
    	PreviewPane,
    	FullScreen,
    	ExternalWindow,
    	NewWindow,
    	Print,
    	File_PNG,
    	Thumbnail,
    	ExpressionPreview
    };

	private static final javax.swing.JWindow windowFullScreen_final = new javax.swing.JWindow( ApplicationController.getInstance().getAppFrame() );
	private static final int SCREEN_Width = Toolkit.getDefaultToolkit().getScreenSize().width;
	private static final int SCREEN_Height = Toolkit.getDefaultToolkit().getScreenSize().height;
	private static JFileChooser jfc;

	private static int miMultisliceDelay = 150;

	public static javax.swing.JFrame mPlotFrame = null;
	public static Panel_Plot mExternalWindowPanel = null;
	public static Panel_Plot mFullScreenPanel = null;
	public static PlotEnvironment mExternalWindowEnvironment = null;
	public static PlotEnvironment mFullScreenEnvironment = null;
	private final static int EXTERNAL_WINDOW_Width = 650;
	private final static int EXTERNAL_WINDOW_Height = 600;

	Output_ToPlot(){}

	static boolean zInitialize( StringBuffer sbError ){
		try {

			// Create external plot frame
			mPlotFrame = new javax.swing.JFrame();
			Resources.iconAdd(mPlotFrame);
			Dimension dimScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
			mPlotFrame.setSize(EXTERNAL_WINDOW_Width, EXTERNAL_WINDOW_Height);
			mPlotFrame.getContentPane().setLayout(new java.awt.BorderLayout());
			mPlotFrame.addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(java.awt.event.WindowEvent event) {
					JFrame fr = (JFrame)event.getSource();
					fr.setVisible(false);
					fr.dispose();
				}
			});

			// create full screen window
			try {
				windowFullScreen_final.getContentPane().setLayout(null); // manual layout
				final int iScreenWidth = dimScreenSize.width;
				final int iScreenHeight = dimScreenSize.height;
				windowFullScreen_final.setSize(iScreenWidth, iScreenHeight);
				windowFullScreen_final.getContentPane().setSize(dimScreenSize);
				windowFullScreen_final.setLocation(0, 0);
				windowFullScreen_final.setBackground(Color.WHITE);
				windowFullScreen_final.addMouseListener(
					new java.awt.event.MouseAdapter(){
						public void mousePressed( java.awt.event.MouseEvent me ){
							if( windowFullScreen_final != null ) windowFullScreen_final.setVisible(false);
						}
					}
				);
				windowFullScreen_final.addKeyListener(
					new java.awt.event.KeyAdapter(){
				    	public void keyPressed(java.awt.event.KeyEvent ke){
							int kc = ke.getKeyCode();
				    		if( kc == java.awt.event.KeyEvent.VK_SPACE ||
								kc == java.awt.event.KeyEvent.VK_ESCAPE
							   ){
								if( windowFullScreen_final != null ) windowFullScreen_final.setVisible(false);
							}
						}
					}
				);
			} catch(Exception ex) {
				StringBuffer sbErrorScreen = new StringBuffer("While setting up full screen: ");
				if( windowFullScreen_final != null ) windowFullScreen_final.setVisible(false);
				ApplicationController.vUnexpectedError(ex, sbErrorScreen);
				ApplicationController.vShowError(sbError.toString());
			}

			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError( ex, sbError );
			return false;
		}
	}

	public static boolean zPlot( PlotEnvironment environment, final Model_Dataset model, final PlottingData pdat, final OutputTarget eOutputOption, final StringBuffer sbError){
		if( environment == null ){
			sbError.append("internal error, no plotting definition");
			return false;
		}
		final PLOT_TYPE ePlotType = environment.getPlotType();
		ColorSpecification       cs = environment.getColorSpecification();
		final PlotScale          ps = environment.getScale();
		final PlotOptions        po = environment.getOptions();
		final PlotText    plot_text = environment.getText();
		try {

			if( pdat == null ){
				sbError.insert(0, "Failed to get dataset: ");
				return false;
			}

			if( ps == null ){
				sbError.insert(0, "No plot scale");
				return false;
			}

			// this is a hack to add extra space in the margin for a legend/colorbar
			// if the defaults are being used
			// if in a future version a more generalized layout system is installed which
			// is capable of automatically adjusting the margins exactly then this should
			// be removed todo
			if( po.option_legend_zShow ){
				PlotLayout.ORIENTATION eORIENT = po.option_legend_Layout.getOrientation();
				if( ps.getMarginRight_px() == PlotScale.PX_DEFAULT_MARGIN
				    && (eORIENT == PlotLayout.ORIENTATION.TopRight ) ){
					ps.setMarginRight(130f);
				}
			}

			miMultisliceDelay = po.getValue_int(PlotOptions.OPTION_MultisliceDelay);

			PlottingVariable varPrimary = pdat.getVariable_Primary();
			if( cs != null && varPrimary != null ){
				if( cs.getDataType() != varPrimary.getDataType() ){
					ApplicationController.vShowWarning("color specification data type does not match variable 1 data type; cs " + cs.getName() + " ignored");
					cs = null;
				}
			}

			String sCaption = model.getTitle();

			final VariableInfo varAxisX = pdat.getAxis_X();
			final VariableInfo varAxisY = pdat.getAxis_Y();

			final int ctVariable1 = pdat.getVariableCount();
			final int ctVariable2 = pdat.getVariable2Count();
			final int ctVariable  = (ctVariable1 == 0 ? ctVariable2 : ctVariable1);
			if( ctVariable < 1 ){
				sbError.append("no variables defined for plotting output");
				return false;
			}

			boolean zResult;
			if( ePlotType == PLOT_TYPE.XY ){ // all variables on same plot
				return zPlot_XY( environment, model, sCaption, pdat, varAxisX, varAxisY, eOutputOption, sbError);
			} else { // each variable goes on a different plot
				for( int xVariable = 1; xVariable <= ctVariable; xVariable++ ){
					PlottingVariable pv1 = pdat.getVariable1(xVariable);
					PlottingVariable pv2 = pdat.getVariable2(xVariable);
					if( ctVariable > 1 && eOutputOption == OutputTarget.Thumbnail ) sCaption = pv1.getSliceCaption();
					switch(ePlotType){
						case Histogram:
							zResult = zPlot_Histogram( environment, model, sCaption, pv1, eOutputOption, xVariable, ctVariable, sbError);
							break;
						case Pseudocolor:
							zResult = zPlot_PseudoColor( environment, model, sCaption, pv1, varAxisX, varAxisY, eOutputOption, xVariable, ctVariable, sbError);
							break;
						case Vector:
							zResult = zPlot_Vector( environment, model, sCaption, pv1, pv2, varAxisX, varAxisY, eOutputOption, xVariable, ctVariable, sbError);
							break;
						case XY:
							sbError.append( "not applicable" );
							return false;
						case Surface:
							sbError.append( "plot type not implemented " + ePlotType );
							return false;
						default:
							sbError.append("unknown plot type " + ePlotType);
							return false;
					}
					if( !zResult ){
						sbError.insert(0, "Error plotting variable " + xVariable + ": ");
						return false;
					}
				}
			}
			return true;
		} catch(Exception ex) {
			sbError.append("error creating data sets: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

//     Independent      Dependent
//       none            1 x/y      lines only (scatter plots must have 2 variables)
//       none            slices     lines only (scatter plots must have 2 variables)
//     1 x/y/slice     1 y/x/slice
//     1 x/y/slice       n slices
//       n slices        n slices    (number of slices must match for both variables)

	static boolean zPlot_XY( PlotEnvironment environment, Model_Dataset model, String sCaption, PlottingData pdat, VariableInfo varAxisX, VariableInfo varAxisY, OutputTarget eOutputOption, StringBuffer sbError ){
		try {
			ColorSpecification cs = environment.getColorSpecification();
			PlotText pt = environment.getText();
			PlotOptions po = environment.getOptions();

			// initialize panel
			Plot_Line plot = Plot_Line.create( environment, null, sCaption, sbError );
			if( plot == null ){
				sbError.insert( 0, "unable to create line plot: " );
				return false;
			}

			// add variables to it
			// line plots should have a variable and a mapping; scatter plots should have pairs of variables
			int ctVariables = pdat.getVariableCount();
			if( ctVariables < 1 ){
				sbError.append("no variables for line plot");
				return false;
			}

			// validate that the variable counts match
			int ctVariable1 = pdat.getVariable1Count();
			int ctVariable2 = pdat.getVariable2Count();
			if( (ctVariable1 > 1 && ctVariable2 > 1) && (ctVariable1 != ctVariable2) ){
				sbError.append("variable count mismatch; dependent " + ctVariable1 + " variables vs " + ctVariable2 + " independent variables");
				return false;
			}

			// get primary variable (may be x or y)
			PlottingVariable varPrimary = pdat.getVariable_Primary();
			int iVarPrimaryDimCount = varPrimary.getDimCount();
			if( iVarPrimaryDimCount != 1 ){
				sbError.append("dim count for primary variable was " + iVarPrimaryDimCount + "; lines can only be plotted from one-dimensional vectors");
				return false;
			}

			// validate dim count for var 1 (dependent)
			if( ctVariable1 > 0 ){
				for( int xVar1 = 1; xVar1 <= ctVariable1; xVar1++ ){
					PlottingVariable pv1 = pdat.getVariable1(xVar1);
					int iVar1DimCount = pv1.getDimCount();
					if( iVar1DimCount != 1 ){
						sbError.append("dim count for first variable was " + iVar1DimCount + "; lines can only be plotted from one-dimensional vectors");
						return false;
					}
				}
			}

			// validate dim count for var 2 (independent)
			if( ctVariable2 > 0 ){
				for( int xVar2 = 1; xVar2 <= ctVariable2; xVar2++ ){
					PlottingVariable pv2 = pdat.getVariable2(xVar2);
					int iVar2DimCount = pv2.getDimCount();
					if( iVar2DimCount != 1 ){
						sbError.append("dim count for second variable was " + iVar2DimCount + "; lines can only be plotted from one-dimensional vectors");
						return false;
					}
				}
			}

//			String sValueCaption = varPrimary.getDataCaption();

			// make dependent variable list
			ArrayList<double[]> listSeries_Y = new ArrayList<double[]>();
			ArrayList<String> listCaptions_Y = new ArrayList<String>();
			String sCaption_Y_data = null;
			String sCaption_Y_units = null;
			for( int xVariable = 1; xVariable <= ctVariable1; xVariable++ ){
				PlottingVariable pv1 = pdat.getVariable1(xVariable);
				sCaption_Y_data = pv1.getDataCaption();
				sCaption_Y_units = pv1.getDataUnits();
				Object[] eggLineData = pv1.getDataEgg();
				Object[] eggMissing = pv1.getMissingEgg();
				if( eggMissing == null ) eggMissing = Panel_View_Plot.getDataParameters().getMissingEgg();
				double[] adValue = DAP.convertToDouble(eggLineData, pv1.getDataType(), eggMissing, sbError);
				if( adValue == null ){
					sbError.insert(0, "Failed to convert data from variable " + xVariable + " to doubles: ");
					return false;
				}
				listSeries_Y.add( adValue );
				listCaptions_Y.add( pv1.getSliceCaption() );
			}

			// x-axis
			String sCaption_X_data = null;
			String sCaption_X_units = null;
			ArrayList<double[]> listSeries_X = new ArrayList<double[]>();
			ArrayList<String> listCaptions_X = new ArrayList<String>();
			for( int xVariable = 1; xVariable <= ctVariable2; xVariable++ ){
				PlottingVariable pv2 = pdat.getVariable2(xVariable);
				sCaption_X_data = pv2.getDataCaption();
				sCaption_X_units = pv2.getDataUnits();
				Object[] eggLineData = pv2.getDataEgg();
				Object[] eggMissing = pv2.getMissingEgg();
				if( eggMissing == null ) eggMissing = Panel_View_Plot.getDataParameters().getMissingEgg();
				double[] adValue = DAP.convertToDouble(eggLineData, pv2.getDataType(), eggMissing, sbError);
				if( adValue == null ){
					sbError.insert(0, "Failed to convert data from variable " + xVariable + " to doubles: ");
					return false;
				}
				listSeries_X.add(adValue);
				listCaptions_X.add(pv2.getSliceCaption());
			}

			// determine axis captions in the case that either x or y is dependent
			String sCaption_axis_Y = null;
			String sCaption_axis_X = null;
			if( sCaption_X_data == null && sCaption_X_units == null ){
				sCaption_axis_X = null;
			} else if( sCaption_X_data == null ){
				sCaption_axis_X = sCaption_X_units;
			} else {
				sCaption_axis_X = sCaption_X_data + " (" + sCaption_X_units + ")";
			}
			if( sCaption_Y_data == null && sCaption_Y_units == null ){
				sCaption_axis_Y = null;
			} else if( sCaption_Y_data == null ){
				sCaption_axis_Y = sCaption_Y_units;
			} else {
				sCaption_axis_Y = sCaption_Y_data + " (" + sCaption_Y_units + ")";
			}

			// set data (will generate the line points)
			if( !plot.setLineData( environment.getScale(), listSeries_Y, listSeries_X, listCaptions_X, listCaptions_Y, DAP.DATA_TYPE_Float64, sCaption_axis_Y, sCaption_axis_X, sbError) ){
				sbError.insert(0, "Error setting line data: ");
				return false;
			}

			return zPlot( plot, model, eOutputOption, sbError );
		} catch(Exception ex) {
			sbError.append("While building line plot: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

//     Independent      Dependent
//       none            1 x/y      lines only (scatter plots must have 2 variables)
//       none            slices     lines only (scatter plots must have 2 variables)
//     1 x/y/slice     1 y/x/slice
//     1 x/y/slice       n slices
//       n slices        n slices    (number of slices must match for both variables)

	static boolean zPlot_Expression( PlotEnvironment environment, Model_Dataset model, OutputTarget eOutputOption, String sCaption, StringBuffer sbError ){
		try {
			Panel_Plot_Expression panelPE = new Panel_Plot_Expression( environment, null, sCaption );
			ColorSpecification cs = environment.getColorSpecification();
			PlotText pt = environment.getText();
			PlotOptions po = environment.getOptions();
			panelPE.setColors(cs);
			panelPE.setOptions(po);
			panelPE.setText(pt);
			if( model.getType() != Model_Dataset.DATASET_TYPE.PlottableExpression && model.getType() != Model_Dataset.DATASET_TYPE.Text ){
				sbError.append( "dataset model is not a plottable expression" );
				return false;
			}
			String sExpressionText = model.getTextContent();
			if( sExpressionText == null || sExpressionText.length() <= 0 ){
				sbError.append( "expression has no text" );
				return false;
			}
			Interpreter interpreter = ApplicationController.getInstance().getInterpreter();
			Script script = interpreter.generateScriptFromText( sExpressionText, sbError );
			if( script == null ){
				sbError.insert( 0, "failed to analyze script text " + model.getTitle() );
				return false;
			}
			Model_PlottableExpression modelExpression = Model_PlottableExpression.create( script, sbError );
			if( modelExpression == null ){
				sbError.insert( 0, "failed to create expression model: " );
				return false;
			}
			PlotAxes axes = environment.getAxes();
			PlotRange range = modelExpression.getPlotRange(sbError);
			if( range == null ) return false;
			if( axes.mzAutomatic || axes.mzAutomaticX ){
				PlotAxis axisDefaultX = axes._getDefaultX();
				axisDefaultX.setRangeX( range );
				axisDefaultX.setActive( true );
			}
			if( axes.mzAutomatic || axes.mzAutomaticY ){
				PlotAxis axisDefaultY = axes._getDefaultY();
				axisDefaultY.setRangeY( range );
				axisDefaultY.setActive( true );
			}
			if( ! panelPE.setExpressionModel( modelExpression, sbError ) ){
				sbError.insert( 0, "error setting expression model: " );
				return false;
			}
			return zPlot( panelPE, model, eOutputOption, sbError );
		} catch(Exception ex) {
			sbError.append("While building expression plot: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	
	static boolean zPlot_Histogram( PlotEnvironment environment, Model_Dataset model, String sCaption, PlottingVariable pv, OutputTarget eOutputOption, int iFrame, int ctFrames, StringBuffer sbError ){
		try {
			PlotOptions po = environment.getOptions();
			PlotText pt = environment.getText();
			PlotScale ps = environment.getScale();
 
			if( eOutputOption == OutputTarget.Thumbnail ){
				sbError.append("cannot thumbnail histograms");
				return false;
			}

			// get variable to plot
			int eDATA_TYPE   = pv.getDataType();
			Object[] eggHistogramData = pv.getDataEgg();
			Object[] eggHistogramMissing = pv.getMissingEgg();

			Panel_Plot_Histogram plotHistogram = new Panel_Plot_Histogram( environment, null, sCaption );
			plotHistogram.setText(pt);
			plotHistogram.setBoxed(false);
			plotHistogram.setMarginPixels_Top( 50 );
			plotHistogram.setMarginPixels_Bottom( 50 );
			plotHistogram.setMarginPixels_Left( 70 );
			plotHistogram.setMarginPixels_Right( 50 );
			plotHistogram.setLabel_HorizontalAxis( "" );
			plotHistogram.setLabel_Title( "todo" );
			int iDataPointCount = DAP.getArraySize(eggHistogramData[0]);
			if( plotHistogram.setData( ps, eDATA_TYPE, eggHistogramData, eggHistogramMissing, po, sbError) ){
				// ApplicationController.vShowStatus("plotting histogram of " + iDataPointCount + " values");
			} else {
				sbError.insert(0, "failed to set data for histogram of " + iDataPointCount + " values: ");
				return false;
			}
			return zPlot( plotHistogram, model, eOutputOption, iFrame, ctFrames, sbError );
		} catch(Exception ex) {
			sbError.append("While building histogram: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	static boolean zPlot_PseudoColor( PlotEnvironment environment, Model_Dataset model, String sCaption, PlottingVariable pv, VariableInfo varAxisX, VariableInfo varAxisY, OutputTarget eOutputOption, int iFrame, int ctFrames, StringBuffer sbError ){
		try {

			ColorSpecification cs = environment.getColorSpecification();
			PlotText pt = environment.getText();
			PlotOptions po = environment.getOptions();
			int eDATA_TYPE   = pv.getDataType();

			// setup generated cs if necessary
			if( cs == null ){
				cs = new ColorSpecification("[system generated]", eDATA_TYPE);
				int iDefaultMissingColor = 0xFF0000FF;
				// cs.setMissing( Panel_View_Plot.getDataParameters().getMissingEgg(), eDATA_TYPE, iDefaultMissingColor );
				cs.setMissing( pv.getMissingEgg(), eDATA_TYPE, iDefaultMissingColor );
				if( !cs.setStandardColors(ColorSpecification.COLOR_STYLE_Default, sbError) ){
					sbError.insert(0, "Failed to create default standard colors: ");
					return false;
				}
			} else {
				if( cs.getDataType() != eDATA_TYPE ){
					sbError.append("Color specification type (" + DAP.getType_String(eDATA_TYPE) + ") does not match data type (" +  DAP.getType_String(eDATA_TYPE) + "); see help topic color specification for more information");
					return false;
				}
			}

			int iWidth       = pv.getDimLength(1);
			int iHeight      = pv.getDimLength(2);
			if( iWidth <= 1 || iHeight <=1 ){
				sbError.append("cannot plot pseudocolor with width " + iWidth + " and height " + iHeight + "; data must be two-dimensional");
				return false;
			}
			Panel_Plot_Pseudocolor panelPC;
			if( eOutputOption == OutputTarget.Thumbnail ){
				int pxThumbnailWidth = po.get(PlotOptions.OPTION_ThumbnailWidth).getValue_int();
				int pxThumbnailHeight = iHeight * pxThumbnailWidth / iWidth;
				PlotScale psThumbnail = PlotScale.create();
				psThumbnail.setScaleMode( PlotScale.SCALE_MODE.PlotArea );
				psThumbnail.setPixelWidth_PlotArea( pxThumbnailWidth );
				psThumbnail.setPixelHeight_PlotArea( pxThumbnailHeight );
				environment.setScale( psThumbnail );
				panelPC = new Panel_Plot_Pseudocolor( environment, null, sCaption );
			} else {
				panelPC = new Panel_Plot_Pseudocolor( environment, null, sCaption );
			}
			panelPC.setColors(cs);
			panelPC.setText(pt);
			panelPC.setOptions(po);

			// set data
			Object[] eggData = pv.getDataEgg();
			PlottableData plottable = new PlottableData();
			if( ! plottable.setPlotData( eDATA_TYPE, eggData, null, null, null, iWidth, iHeight, sbError) ){
				sbError.insert(0, "Failed to set pseudocolor data (type " + DAP.getType_String(eDATA_TYPE) + ") with width " + iWidth + " and height " + iHeight + ": ");
				return false;
			}
			if( !panelPC.setData( plottable, sbError) ){
				sbError.insert(0, "Failed to set pseudocolor data (type " + DAP.getType_String(eDATA_TYPE) + ") with width " + iWidth + " and height " + iHeight + ": ");
				return false;
			}

			// set axes
			if( eOutputOption == OutputTarget.Thumbnail ){
				// do not set axes
			} else {
				if( pv.getDimCount() == 2 ){
					PlotAxis axisHorizontal = PlotAxis.createLinear_X( "X-Axis", 0, 0 );
					if( varAxisX == null ){ // todo resolve reversals/inversions
						axisHorizontal = null;
					} else {
						if( varAxisX.getUseIndex() ) {
//							axisHorizontal = PlotAxis.();
//							axisHorizontal.setIndexed(1, iWidth);
//							axisHorizontal.setCaption("[indexed]");
						} else {
//							axisHorizontal = new PlotAxis();
//							String sNameX = varAxisX.getName();
//							String sLongNameX = varAxisX.getLongName();
//							String sUserCaptionX = varAxisX.getUserCaption();
//							String sUnitsX = varAxisX.getUnits();
//							Object[] eggAxisX = varAxisX.getValueEgg();
//							axisHorizontal.setValues(eggAxisX, varAxisX.getDataType(), false);
//							String sCaptionAxisX = (sUserCaptionX != null ? sUserCaptionX : sLongNameX != null ? sLongNameX : sNameX) +
//												    (sUnitsX == null ? "" : sUnitsX);
//							axisHorizontal.setCaption(sCaptionAxisX);
						}
					}
					PlotAxis axisVertical = PlotAxis.createLinear_Y( "Y-Axis", 0, 0 );
					if( varAxisY == null ){
						axisVertical = null;
					} else {
//						String sName = varAxisY.getName();
//						String sUnits = varAxisY.getUnits();
						if( varAxisY.getUseIndex() ) {
//							axisVertical = new PlotAxis();
//							axisVertical.setIndexed(1, iHeight);
//							axisVertical.setCaption("[indexed]");
						} else {
//							axisVertical = new PlotAxis();
//							Object[] eggAxisY = varAxisY.getValueEgg();
//							axisVertical.setValues(eggAxisY, varAxisY.getDataType(), true);
//							String sNameY = varAxisY.getName();
//							String sLongNameY = varAxisY.getLongName();
//							String sUserCaptionY = varAxisY.getUserCaption();
//							String sUnitsY = varAxisY.getUnits();
//							axisVertical.setValues(eggAxisY, varAxisY.getDataType(), false);
//							String sCaptionAxisY = (sUserCaptionY != null ? sUserCaptionY : sLongNameY != null ? sLongNameY : sNameY) +
//												    (sUnitsY == null ? "" : sUnitsY);
//							axisVertical.setCaption(sCaptionAxisY);
						}
					}
//					panelPC.setAxisHorizontal(axisHorizontal);
//					panelPC.setAxisVertical(axisVertical);
				} else {
					sbError.append("can only plot pseudocolor data with two axes");
					return false;
				}
			}

			panelPC.setLabel_Values(pv.getDataCaption());

			return zPlot( panelPC, model, eOutputOption, iFrame, ctFrames, sbError );
		} catch(Exception ex) {
			sbError.append("While building pseudocolor plot: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	static boolean zPlot_Vector( PlotEnvironment environment, Model_Dataset model, String sCaption, PlottingVariable pv, PlottingVariable pv2, VariableInfo varAxisX, VariableInfo varAxisY, OutputTarget eOutputOption, int iFrame, int ctFrames, StringBuffer sbError ){
		try {
			ColorSpecification cs = environment.getColorSpecification();
			PlotText pt = environment.getText();
			PlotOptions po = environment.getOptions();
			Panel_Plot_Vector panelVector = new Panel_Plot_Vector( environment, null, sCaption );
			if( pv == null || pv2 == null ){
				sbError.append("a vector plot requires exactly two variables");
				return false;
			}
			int eDATA_TYPE = pv.getDataType();
			if( pv2.getDataType() != eDATA_TYPE ){
				String sType1 = opendap.clients.odc.DAP.getType_String(eDATA_TYPE);
				String sType2 = opendap.clients.odc.DAP.getType_String(pv2.getDataType());
				sbError.append("two variables supplied of different types; variable 1 is " + sType1 + " and variable 2 is " + sType2);
				return false;
			}
			int iWidth       = pv.getDimLength(1);
			int iHeight      = pv.getDimLength(2);
//			if( iWidth <= 1 || iHeight <=1 ){
//				sbError.append("cannot vector plot with width " + iWidth + " and height " + iHeight + "; data must be two-dimensional");
//				return false;
//			}

			// validate that the dimensions of the supplied plotting variables are the same
			int ctDims = pv.getDimCount();
			if( ctDims != pv2.getDimCount() ){
				sbError.append("variable one has a different number of dimensions (" + ctDims + " than variable 2 (" + pv2.getDimCount() + ")");
				return false;
			}
			for( int xDim = 1; xDim <= ctDims; xDim++ ){
				if( pv.getDimLength(xDim) != pv2.getDimLength(xDim) ){
					sbError.append("variable one dimension " + xDim + " has a different length (" + pv.getDimLength(xDim) + ") than variable 2 (" + pv2.getDimLength(xDim) + ")");
					return false;
				}
			}

			// set data
			Object[] eggDataU = pv.getDataEgg();
			Object[] eggDataV = pv2.getDataEgg();
			Object[] eggMissingU = pv.getMissingEgg();
			Object[] eggMissingV = pv2.getMissingEgg();
			PlottableData plottable = new PlottableData();
			if( ! plottable.setPlotData( eDATA_TYPE, eggDataU, eggMissingU, eggDataV, eggMissingV, iWidth, iHeight, sbError) ){
				sbError.insert(0, "Failed to set data type " + DAP.getType_String(eDATA_TYPE) + " with width " + iWidth + " and height " + iHeight + " for vector plot: ");
				return false;
			}
			if( ! panelVector.setData( plottable, sbError) ){
				sbError.insert(0, "Failed to set plottable data for vector plot: ");
				return false;
			}

			// set axes
//			if( pv.getDimCount() == 2 ){
				PlotAxis axisHorizontal = PlotAxis.createLinear_X( "X Axis", 0, 0 );
				if( varAxisX == null ){ // todo resolve reversals/inversions
					axisHorizontal = null;
				} else if( varAxisX.getUseIndex() ) {
//					axisHorizontal = new PlotAxis();
//					axisHorizontal.setIndexed(1, iWidth);
//					axisHorizontal.setCaption("[indexed]");
				} else {
//					axisHorizontal = new PlotAxis();
//					Object[] eggAxisX = varAxisX.getValueEgg();
//					axisHorizontal.setValues(eggAxisX, varAxisX.getDataType(), false);
//					String sNameX = varAxisX.getName();
//					String sLongNameX = varAxisX.getLongName();
//					String sUserCaptionX = varAxisX.getUserCaption();
//					String sUnitsX = varAxisX.getUnits();
//					axisHorizontal.setValues(eggAxisX, varAxisX.getDataType(), false);
//					String sCaptionAxisX = (sUserCaptionX != null ? sUserCaptionX : sLongNameX != null ? sLongNameX : sNameX) +
//											(sUnitsX == null ? "" : sUnitsX);
//					axisHorizontal.setCaption(sCaptionAxisX);
				}
				PlotAxis axisVertical = PlotAxis.createLinear_Y( "Y Axis", 0, 0 );
				if( varAxisY == null ){
					axisVertical = null;
				} else if( varAxisY.getUseIndex() ) {
//					axisVertical = new PlotAxis();
//					axisVertical.setIndexed(1, iHeight);
//					axisVertical.setCaption("[indexed]");
				} else {
//					axisVertical = new PlotAxis();
//					Object[] eggAxisY = varAxisY.getValueEgg();
//					axisVertical.setValues(eggAxisY, varAxisY.getDataType(), true);
//					String sNameY = varAxisY.getName();
//					String sLongNameY = varAxisY.getLongName();
//					String sUserCaptionY = varAxisY.getUserCaption();
//					String sUnitsY = varAxisY.getUnits();
//					axisVertical.setValues(eggAxisY, varAxisY.getDataType(), false);
//					String sCaptionAxisY = (sUserCaptionY != null ? sUserCaptionY : sLongNameY != null ? sLongNameY : sNameY) +
//											(sUnitsY == null ? "" : sUnitsY);
//					axisVertical.setCaption(sCaptionAxisY);
				}
//				panelVector.setAxisHorizontal(axisHorizontal);
//				panelVector.setAxisVertical(axisVertical);
//			} else {
//				sbError.append("can only plot vector data with two dimensions");
//				return false;
//			}
			panelVector.setColors(cs);
			panelVector.setOptions(po);
			panelVector.setText(pt);
			return zPlot( panelVector, model, eOutputOption, iFrame, ctFrames, sbError );
		} catch(Exception ex) {
			sbError.append("While building vector plot: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	static boolean zPlot( PlotEnvironment environment, Plot plot, Model_Dataset model, OutputTarget eOutputOption, StringBuffer sbError ){
		return zPlot( environment, plot, model, eOutputOption, 1, 1, sbError );
	}

	static boolean zPlot( PlotEnvironment environment, Plot plot, Model_Dataset model, OutputTarget eOutputOption, int iFrameNumber, int ctFrames, StringBuffer sbError ){
		try {
			PlotScale scale;
			String sOutput;
			Panel_Plot plot_panel;
			switch( eOutputOption ){
				
				case ExternalWindow: // use the existing external window
				case NewWindow: // create a new window
					if( iFrameNumber > 1 ) Thread.sleep(miMultisliceDelay); // wait for two seconds before continuing

					// establish frame
					JFrame frame;
				    if( eOutputOption == OutputTarget.ExternalWindow ){
				    	sOutput = "the external window";
						frame = mPlotFrame;
						if( mExternalWindowPanel == null || mExternalWindowEnvironment != environment ){
							plot_panel = Panel_Plot.create( environment, plot, sbError );
							if( plot_panel == null ){
								sbError.insert( 0, "failed to create fresh plot panel for external window: " );
								return false;
							}
						} else {
							plot_panel = mExternalWindowPanel;
						}
					} else {
						sOutput = "new window";
						frame = new javax.swing.JFrame();
						frame.getContentPane().setLayout(new java.awt.BorderLayout());
						frame.addWindowListener(new java.awt.event.WindowAdapter() {
							public void windowClosing(java.awt.event.WindowEvent event) {
								JFrame fr = (JFrame)event.getSource();
								fr.setVisible(false);
								fr.dispose();
							}
						});
						Resources.iconAdd(frame);
						plot_panel = Panel_Plot.create( environment, plot, sbError );
					    JComponent compToAdd;
						scale = plot_panel.getPlotScale();
						if( (float)scale.getCanvas_Width_pixels() > .9f * SCREEN_Width ||
						    (float)scale.getCanvas_Height_pixels() > .9f * SCREEN_Height ){
							if( iFrameNumber == 1 ) frame.setSize( SCREEN_Width, SCREEN_Height );
							JScrollPane jspNew = new JScrollPane( plot_panel );
							compToAdd = jspNew;
						} else {
							if( iFrameNumber == 1 ){
								int iExtraWidth = ApplicationController.getInstance().getFrame_ExtraWidth();
		    					int iExtraHeight = ApplicationController.getInstance().getFrame_ExtraHeight();
			    				int iFrameWidth = scale.getCanvas_Width_pixels() + iExtraWidth;
				    			int iFrameHeight = scale.getCanvas_Height_pixels() + iExtraHeight;
					    		frame.setSize(iFrameWidth, iFrameHeight);
							}
							compToAdd = plot_panel;
						}
						frame.getContentPane().add( compToAdd, BorderLayout.CENTER );
					}
					if( ! plot_panel.zPlot( sbError ) ){
						sbError.insert(0, "plotting to " + sOutput + ": ");
						return false;
					}

					frame.setVisible( true );
					Thread.yield();
					break;
					
				case FullScreen:
					sOutput = "full screen";
					boolean zNewFullScreenPanelCreated = false;
					if( mFullScreenPanel == null ){
						plot_panel = Panel_Plot.create( environment, plot, sbError );
						zNewFullScreenPanelCreated = true;
					} else {
						if( mFullScreenEnvironment == environment ){
							plot_panel = mFullScreenPanel; 
						} else {
							plot_panel = Panel_Plot.create( environment, plot, sbError );
							zNewFullScreenPanelCreated = true;
						}
					}
					if( plot_panel == null ){
						sbError.insert( 0, "failed to create panel for full screen plot" );
						return false;
					}
					mFullScreenPanel = plot_panel; 
					mFullScreenEnvironment = environment;
					if( ! plot_panel.zPlot( sbError ) ){
						sbError.insert(0, "plotting to full screen: ");
						return false;
					}
					if( iFrameNumber > 1 ) Thread.sleep(miMultisliceDelay); // wait for two seconds before continuing
					if( zNewFullScreenPanelCreated ){
						scale = plot_panel.getPlotScale();
						int iPanelWidth = scale.getCanvas_Width_pixels();
						int iPanelHeight = scale.getCanvas_Height_pixels();
						java.awt.Container container = windowFullScreen_final.getContentPane();
						container.removeAll();
						container.add( plot_panel );
						Utility.vCenterComponent( plot_panel, iPanelWidth, iPanelHeight, SCREEN_Width, SCREEN_Height );
						container.setBackground( plot_panel.getColor_Background() );
						windowFullScreen_final.setVisible(true);
					}
					Thread.yield();
					windowFullScreen_final.requestFocus(); // so window can handle key strokes
					break;
				case Print:
					plot_panel = Panel_Plot.create( environment, plot, sbError );
					if( plot_panel == null ){
						sbError.insert( 0, "failed to create panel for printing" );
						return false;
					}
					RepaintManager theRepaintManager = RepaintManager.currentManager( plot_panel );
					try {
						theRepaintManager.setDoubleBufferingEnabled(false);  // turn off double buffering
						PrinterJob printer_job = PrinterJob.getPrinterJob();
						PageFormat page_format_default = printer_job.defaultPage();
						PageFormat page_format_user = printer_job.pageDialog(page_format_default);
						if( page_format_user == page_format_default ){
							return true; // user cancelled action
						} else {
							printer_job.setPrintable( plot_panel );
							printer_job.print();
						}
						sOutput = "printer " + " " + printer_job.getPrintService().getName() + " job " + printer_job.getJobName() ;
					} finally {
						theRepaintManager.setDoubleBufferingEnabled(true);  // turn it back on
					}
					break;
				case PreviewPane:
					sOutput = "preview pane";
					if( ! plot_panel.zPlot(sbError) ){
						sbError.insert(0, "plotting to preview pane: ");
						return false;
					}
					if( iFrameNumber > 1 ) Thread.sleep( miMultisliceDelay ); // wait for two seconds before continuing
					Panel_View_Plot.getPreviewPane().setContent( plot_panel );
					Thread.yield();
					break;
				case ExpressionPreview:
					sOutput = "expression preview pane";
					if( ! plot_panel.zPlot(sbError) ){
						sbError.insert(0, "plotting to expression preview pane: ");
						return false;
					}
					if( iFrameNumber > 1 ) Thread.sleep(miMultisliceDelay); // wait for two seconds before continuing
					ApplicationController.getInstance().getAppFrame().getDataViewer()._getPreviewPane().setContent( panelPlot );
					Thread.yield();
					break;
				case File_PNG:
					if( ! plot_panel.zPlot(sbError) ){
						sbError.insert(0, "plotting to buffer for image: ");
						return false;
					}
					java.awt.image.RenderedImage imagePlot = plot_panel._getImage();
					if( imagePlot == null ){
						sbError.append("internal error, no image");
						return false;
					}
					if (jfc == null) jfc = new JFileChooser();
					int iState = jfc.showDialog(ApplicationController.getInstance().getAppFrame(), "Create File");
					File file = jfc.getSelectedFile();
					if (file == null || iState != JFileChooser.APPROVE_OPTION) return true; // user cancel
					if( !javax.imageio.ImageIO.write(imagePlot, "png", file) ){
						sbError.append("error writing file " + file);
						return false;
					}
					sOutput = "PNG file " + file;
					break;
				case Thumbnail:
					Panel_Thumbnails panelThumbnails = Panel_View_Plot.getPanel_Thumbnails();
					Panel_View_Plot.getPreviewPane().setContent( plot_panel );
					int pxThumbnailWidth = plot_panel.getPlotOptions().get(PlotOptions.OPTION_ThumbnailWidth).getValue_int();
					int pxThumbnailHeight = plot_panel.mPlottable.getDimension_y() * pxThumbnailWidth / plot_panel.mPlottable.getDimension_x();
					int[] rgb_array = new int[ pxThumbnailWidth * pxThumbnailHeight ]; // TODO re use buffer
					plot_panel.zWriteRGBArray( rgb_array, pxThumbnailWidth, pxThumbnailHeight, false, sbError);
					if( rgb_array == null ){
						sbError.append("Unable to generate plot: " + sbError);
						return false;
					}
					BufferedImage biThumbnail = new BufferedImage(pxThumbnailWidth, pxThumbnailHeight, BufferedImage.TYPE_INT_ARGB);
					biThumbnail.setRGB( 0, 0, pxThumbnailWidth, pxThumbnailHeight, rgb_array, 0, pxThumbnailWidth );
					Graphics g = biThumbnail.getGraphics();
					g.setColor(Color.BLACK);
					g.drawRect(0, 0, pxThumbnailWidth - 1, pxThumbnailHeight - 1);
					panelThumbnails.addThumbnail( model, plot_panel._getCaption(), biThumbnail );
					panelThumbnails.revalidate();
					sOutput = "thumbnail";
					break;
				default:
					sbError.append("unsupported output type for plot");
				    return false;
			}
			if( iFrameNumber < ctFrames ){
				ApplicationController.vShowStatus_NoCache("plot frame " + iFrameNumber + " / " + ctFrames);
			} else if( ctFrames > 1 ) {
				ApplicationController.vShowStatus("Plotted " + ctFrames + " frames " + plot_panel._getID() + " to " + sOutput);
			} else {
				ApplicationController.vShowStatus("Plotted " + plot_panel._getID() + " to " + sOutput);
			}
			return true;
		} catch(Exception ex) {
			sbError.append("While building output window: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

}

class PlottingData {
	public final static int AXES_None = 0;
	public final static int AXES_Linear = 1;
	public final static int AXES_Mapped = 2;
	private int mctDimensions;
	private int[] maiDim_TYPE1;
	private int mAXES_TYPE;
	private ArrayList<PlottingVariable> listVariables1 = new ArrayList<PlottingVariable>();
	private ArrayList<PlottingVariable> listVariables2 = new ArrayList<PlottingVariable>();
	private VariableInfo mvarAxis_X = null;
	private VariableInfo mvarAxis_Y = null;
	VariableInfo getAxis_X(){ return mvarAxis_X; }
	VariableInfo getAxis_Y(){ return mvarAxis_Y; }
	void setAxis_X(VariableInfo var, boolean zUseIndex){
		mvarAxis_X = var;
		if( zUseIndex ) var.setUseIndex();
	}
	void setAxis_Y(VariableInfo var, boolean zUseIndex){
		mvarAxis_Y = var;
		if( zUseIndex ) var.setUseIndex();
	}
	void vInitialize( int ctDimensions, int iAXES_TYPE ){
		mctDimensions = ctDimensions;
		mAXES_TYPE = iAXES_TYPE;
	}
	boolean zAddVariable( Object[] eggData1, Object[] eggData2, int eDataType1, int eDataType2, int[] aiDimLength1, Object[] eggMissing1, Object[] eggMissing2, String sDataCaption1, String sDataCaption2, String sDataUnits1, String sDataUnits2, String sSliceCaption1, String sSliceCaption2, StringBuffer sbError ){
		if( eggData1 == null && eggData2 == null ){ sbError.append("data egg not supplied"); return false; }
		if( eggData1 != null ){
			if( eggData1[0] == null ){ sbError.append("data egg 1 is empty"); return false; }
	        if( eggData1 != null && eggData1.length != 1 ){ sbError.append("data egg 1 does not have exactly one element"); return false; }
			if( eggMissing1 != null ) if( eggMissing1.length != 1 ){ sbError.append("missing egg 1 does not have exactly one element"); return false; }
			PlottingVariable pv = new PlottingVariable();
	    	if( !pv.zSet(eDataType1, eggData1, aiDimLength1, eggMissing1, sDataCaption1, sDataUnits1, sSliceCaption1, sbError) ) return false;
		    listVariables1.add( pv );
		}
		if( eggData2 != null ){
			if( eggData2[0] == null ){ sbError.append("data egg 2 is empty"); return false; }
	        if( eggData2 != null && eggData2.length != 1 ){ sbError.append("data egg 2 does not have exactly one element"); return false; }
		    if( eggMissing2 != null ) if( eggMissing2.length != 1 ){ sbError.append("missing egg 2 does not have exactly one element"); return false; }
			PlottingVariable pv2 = new PlottingVariable();
			if( !pv2.zSet(eDataType2, eggData2, aiDimLength1, eggMissing2, sDataCaption2, sDataUnits2, sSliceCaption2, sbError) ) return false;
			listVariables2.add( pv2 );
		}
		return true;
	}
	int getDimensionCount(){ return mctDimensions; }
	int getAxesTYPE(){ return mAXES_TYPE; }
	int getVariable1Count(){ return listVariables1.size(); }
	int getVariable2Count(){ return listVariables2.size(); }
	int getVariableCount(){ if( listVariables1.size() == 0 ) return listVariables2.size(); else return listVariables1.size(); }
	PlottingVariable getVariable_Primary(){
		if( getVariable1Count() > 0 ) return getVariable1(1);
		return getVariable2(1);
	}
	PlottingVariable getVariable1(int xVariable1){
		if( xVariable1 < 1 || xVariable1 > listVariables1.size() ) return null;
		return (PlottingVariable)listVariables1.get( xVariable1 - 1);
	}
	PlottingVariable getVariable2(int xVariable1){
		if( xVariable1 < 1 || xVariable1 > listVariables2.size() ) return null;
		return (PlottingVariable)listVariables2.get( xVariable1 - 1);
	}
	String getAxesTYPE_S(){
		switch(mAXES_TYPE){
			case AXES_None: return "None";
			case AXES_Linear: return "Linear";
			case AXES_Mapped: return "Mapped";
			default: return "[unknown " + mAXES_TYPE + "]";
		}
	}
	public String toString(){
		StringBuffer sb = new StringBuffer(120);
		sb.append("PlottingData (axes: " + getAxesTYPE_S() + ", dims: " + mctDimensions);
		sb.append(" {");
		for( int xDim = 1; xDim <= mctDimensions; xDim++ ){
			if( xDim > 1 ) sb.append(", ");
			sb.append( DAP.getType_String(maiDim_TYPE1[xDim]) );
		}
		sb.append("} variable count " + getVariableCount() + ":\n");
		for( int xVar = 1; xVar <= getVariableCount(); xVar++ ){
			PlottingVariable pv = getVariable1(xVar);
			sb.append("var " + xVar + ": " + pv);
		}
		return sb.toString();
	}

}

class PlottingVariable {
	private int meDataType; // see DAP for types
	private int[] maiDimLength1; // all four of these arrays must have same length
	private Object[] meggData; // the egg containing the data
	private Object[] meggMissing1; // the egg containing the missing values in a one-based linear array
	private String msDataCaption;
	private String msDataUnits;
	private String msSliceCaption;

	/** the egg containing the missing values is a one-based linear array which can be null */
	boolean zSet( int eDataType, Object[] eggData, int[] aiDimLength1, Object[] eggMissing, String sDataCaption, String sDataUnits, String sSliceCaption, StringBuffer sbError ){
		if( eggData == null ){
			sbError.append("no egg supplied");
			return false;
		}
		if( eggData[0] == null ){
			sbError.append("egg is empty");
			return false;
		}
		meggData = eggData;
		meggMissing1 = eggMissing;
		meDataType = eDataType;
		maiDimLength1 = aiDimLength1;
		msDataCaption = sDataCaption;
		msDataUnits = sDataUnits;
		msSliceCaption = sSliceCaption;
		return true;
	}
	String getDataCaption(){ return msDataCaption; }
	String getDataUnits(){ return msDataUnits; }
	String getSliceCaption(){ return msSliceCaption; }
	Object[] getDataEgg(){ return meggData; }
	Object[] getMissingEgg(){ return meggMissing1; }
	void setMissingEgg( Object[] eggMissing1 ){ meggMissing1 = eggMissing1; }
	int getDataType(){ return meDataType; }
	int getDimCount(){ return maiDimLength1[0]; }
	int getDimLength( int xDim1 ){
		if( maiDimLength1 == null ) return 0;
		if( xDim1 < 1 || xDim1 >= maiDimLength1.length ) return 0;
		return maiDimLength1[xDim1];
	}
	public String toString(){
		StringBuffer sb = new StringBuffer(120);
		sb.append("PlottingVariable type: " + DAP.getType_String(meDataType) + " dims: {");
		for( int xDim = 0; xDim < maiDimLength1.length; xDim++ ){
			if( xDim > 0 ) sb.append(",");
			sb.append(" " + maiDimLength1[xDim]);
		}
		sb.append(" } \n");
		sb.append("data caption: " + msDataCaption + "\n");
		sb.append("data units: " + msDataUnits + "\n");
		sb.append("slice caption: " + msSliceCaption + "\n");
		if( meggMissing1 != null ){
			sb.append(" missing: {");
			int len;
			switch( meDataType ){
				case DAP.DATA_TYPE_Byte:
					byte[] ab = (byte[])meggMissing1[0];
					len = ab.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + ab[xMissing]);
					break;
				case DAP.DATA_TYPE_Int16:
					short[] ash = (short[])meggMissing1[0];
					len = ash.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + ash[xMissing]);
					break;
				case DAP.DATA_TYPE_Int32:
					int[] ai = (int[])meggMissing1[0];
					len = ai.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + ai[xMissing]);
					break;
				case DAP.DATA_TYPE_UInt16:
					ash = (short[])meggMissing1[0];
					len = ash.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + DAP.toSigned(ash[xMissing]));
					break;
				case DAP.DATA_TYPE_UInt32:
					ai = (int[])meggMissing1[0];
					len = ai.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + DAP.toSigned(ai[xMissing]));
					break;
				case DAP.DATA_TYPE_Float32:
					float[] af = (float[])meggMissing1[0];
					len = af.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + af[xMissing]);
					break;
				case DAP.DATA_TYPE_Float64:
					double[] ad = (double[])meggMissing1[0];
					len = ad.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + ad[xMissing]);
					break;
				case DAP.DATA_TYPE_String:
					String[] as = (String[])meggMissing1[0];
					len = as.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" \"" + as[xMissing] + "\"");
					break;
			}
			sb.append(" } ");
		}
		return sb.toString();
	}

}

class DataConnectorFile implements Serializable {
	static final long serialVersionUID = 1;
	private String msTitle;
	private Model_Dataset mURL;
	private DataDDS mData;
	String getTitle(){ return msTitle; }
	DataDDS getData(){ return mData; }
	Model_Dataset getURL(){ return mURL; }
	void setTitle(String sTitle){ msTitle = sTitle; }
	void setData(DataDDS ddds){ mData = ddds; }
	void setURL(Model_Dataset url){ mURL = url; }
}


