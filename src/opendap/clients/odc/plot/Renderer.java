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
 * Title:        Visualizer
 * Description:  Methods to generate plotting output
 * Copyright:    Copyright (c) 2002-2013
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.10
 */

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.print.*;
import java.awt.image.BufferedImage;

import opendap.clients.odc.*;
import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.data.Script;
import opendap.clients.odc.gui.Resources;

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

public class Renderer {

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

	public static javax.swing.JFrame mCompositionFrame = null;
	public static Panel_Composition mPanel_ExternalWindow = null;
	public static Panel_Composition mPanel_FullScreen = null;
	public static Panel_Composition mPanel_Print = null;
	public static Panel_Composition mPanel_Image = null;
	private final static int EXTERNAL_WINDOW_Width = 650;
	private final static int EXTERNAL_WINDOW_Height = 600;
	private static PlotEnvironment mExternalWindowEnvironment = null;
	private static PlotEnvironment mFullScreenEnvironment = null;

	Renderer(){}

	static boolean zInitialize( StringBuffer sbError ){
		try {

			// Create external plot frame
			mCompositionFrame = new javax.swing.JFrame();
			Resources.iconAdd( mCompositionFrame );
			Dimension dimScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
			mCompositionFrame.setSize( EXTERNAL_WINDOW_Width, EXTERNAL_WINDOW_Height );
			mCompositionFrame.getContentPane().setLayout(new java.awt.BorderLayout());
			mCompositionFrame.addWindowListener( new java.awt.event.WindowAdapter() {
				public void windowClosing( java.awt.event.WindowEvent event ){
					JFrame fr = ( JFrame ) event.getSource();
					fr.setVisible( false );
					fr.dispose();
				}
			} );

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

//     Independent      Dependent
//       none            1 x/y      lines only (scatter plots must have 2 variables)
//       none            slices     lines only (scatter plots must have 2 variables)
//     1 x/y/slice     1 y/x/slice
//     1 x/y/slice       n slices
//       n slices        n slices    (number of slices must match for both variables)

	static boolean zPlot_XY( PlotEnvironment environment, PlotLayout layout, Model_Dataset model, String sCaption, PlottingData pdat, VariableInfo varAxisX, VariableInfo varAxisY, OutputTarget eOutputOption, StringBuffer sbError ){
		try {

			// initialize panel
			Plot_Line plot = Plot_Line.create( environment, layout, null, sCaption, sbError );
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
				if( eggMissing == null ) eggMissing = Panel_View_Plot._getDataParameters().getMissingEgg();
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
				if( eggMissing == null ) eggMissing = Panel_View_Plot._getDataParameters().getMissingEgg();
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

			sbError.append( "not implemented" );
			return false; // zPlot( environment, plot, model, eOutputOption, sbError );
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

	static boolean zPlot_Expression( PlotEnvironment environment, PlotLayout layout, final Model_Dataset model, OutputTarget eOutputOption, String sCaption, StringBuffer sbError ){
		try {

			// generate expression model from script
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

			Plot_Expression plot = Plot_Expression.create( environment, layout, modelExpression, sCaption, sbError );
			if( plot == null ){
				sbError.insert( 0, "failed to generate plot from model" );
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

			sbError.append( "not implemented" );
			return false; // zPlot( environment, plot, model, eOutputOption, sbError );
		} catch(Exception ex) {
			sbError.append("While building expression plot: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	
	static boolean zPlot_Histogram( PlotEnvironment environment, PlotLayout layout, Model_Dataset model, String sCaption, PlottingVariable pv, OutputTarget eOutputOption, int iFrame, int ctFrames, StringBuffer sbError ){
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

			IPlottable plottableData = null; // TODO
			Plot_Histogram plot = Plot_Histogram.create( environment, layout, plottableData, sCaption, sbError );
			if( plot == null ){
				sbError.insert( 0, "failed to generate histogram plot from data: " );
				return false;
			}
			/*
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
			*/
			return true; // zPlot( environment, plot, model, eOutputOption, sbError );
		} catch(Exception ex) {
			sbError.append("While building histogram: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	static boolean zPlot_Vector( PlotEnvironment environment, PlotLayout layout, Model_Dataset model, String sCaption, IPlottable data, OutputTarget eOutputOption, int iFrame, int ctFrames, StringBuffer sbError ){
		try {
			
//			if( pv == null || pv2 == null ){
//				sbError.append("a vector plot requires exactly two variables");
//				return false;
//			}
			int eDATA_TYPE = 0; // pv.getDataType();
//			if( pv2.getDataType() != eDATA_TYPE ){
//				String sType1 = opendap.clients.odc.DAP.getType_String(eDATA_TYPE);
//				String sType2 = "???"; // opendap.clients.odc.DAP.getType_String(pv2.getDataType());
//				sbError.append("two variables supplied of different types; variable 1 is " + sType1 + " and variable 2 is " + sType2);
//				return false;
//			}
			int iWidth       = data.getDimension_x(); // .getDimLength(1);
			int iHeight      = data.getDimension_y(); // pv.getDimLength(2);
//			if( iWidth <= 1 || iHeight <=1 ){
//				sbError.append("cannot vector plot with width " + iWidth + " and height " + iHeight + "; data must be two-dimensional");
//				return false;
//			}

			// validate that the dimensions of the supplied plotting variables are the same
//			int ctDims = pv.getDimCount();
//			if( ctDims != pv2.getDimCount() ){
//				sbError.append("variable one has a different number of dimensions (" + ctDims + " than variable 2 (" + pv2.getDimCount() + ")");
//				return false;
//			}
//			for( int xDim = 1; xDim <= ctDims; xDim++ ){
//				if( pv.getDimLength(xDim) != pv2.getDimLength(xDim) ){
//					sbError.append("variable one dimension " + xDim + " has a different length (" + pv.getDimLength(xDim) + ") than variable 2 (" + pv2.getDimLength(xDim) + ")");
//					return false;
//				}
//			}

			// set data
			Object[] eggDataU = null; // pv.getDataEgg();
			Object[] eggDataV = null; // pv2.getDataEgg();
			Object[] eggMissingU = null; // pv.getMissingEgg();
			Object[] eggMissingV = null; // pv2.getMissingEgg();
			

			// create the data bundle
			Object[] eggData = null; // pv.getDataEgg();
			PlottableData plottableData = PlottableData.create( eDATA_TYPE, eggData, null, null, null, iWidth, iHeight, sbError );
			if( plottableData == null ){
				sbError.insert(0, "Failed to set pseudocolor data (type " + DAP.getType_String(eDATA_TYPE) + ") with width " + iWidth + " and height " + iHeight + ": ");
				return false;
			}

			Plot_Vector plot = Plot_Vector.create( environment, layout, data, sCaption, sbError );
			if( plot == null ){
				sbError.insert(0, "Failed to set pseudocolor data (type " + DAP.getType_String(eDATA_TYPE) + ") with width " + iWidth + " and height " + iHeight + ": ");
				return false;
			}

			// set axes
//			if( pv.getDimCount() == 2 ){
//				PlotAxis axisHorizontal = PlotAxis.createLinear_X( "X Axis", 0, 0 );
//				if( varAxisX == null ){ // todo resolve reversals/inversions
//					axisHorizontal = null;
//				} else if( varAxisX.getUseIndex() ) {
////					axisHorizontal = new PlotAxis();
////					axisHorizontal.setIndexed(1, iWidth);
////					axisHorizontal.setCaption("[indexed]");
//				} else {
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
//				}
//				PlotAxis axisVertical = PlotAxis.createLinear_Y( "Y Axis", 0, 0 );
//				if( varAxisY == null ){
//					axisVertical = null;
//				} else if( varAxisY.getUseIndex() ) {
////					axisVertical = new PlotAxis();
////					axisVertical.setIndexed(1, iHeight);
////					axisVertical.setCaption("[indexed]");
//				} else {
////					axisVertical = new PlotAxis();
////					Object[] eggAxisY = varAxisY.getValueEgg();
////					axisVertical.setValues(eggAxisY, varAxisY.getDataType(), true);
////					String sNameY = varAxisY.getName();
////					String sLongNameY = varAxisY.getLongName();
////					String sUserCaptionY = varAxisY.getUserCaption();
////					String sUnitsY = varAxisY.getUnits();
////					axisVertical.setValues(eggAxisY, varAxisY.getDataType(), false);
////					String sCaptionAxisY = (sUserCaptionY != null ? sUserCaptionY : sLongNameY != null ? sLongNameY : sNameY) +
////											(sUnitsY == null ? "" : sUnitsY);
////					axisVertical.setCaption(sCaptionAxisY);
//				}
//				panelVector.setAxisHorizontal(axisHorizontal);
//				panelVector.setAxisVertical(axisVertical);
//			} else {
//				sbError.append("can only plot vector data with two dimensions");
//				return false;
//			}

			sbError.append( "not implemented" );
			return false; // zPlot( environment, plot, model, eOutputOption, iFrame, ctFrames, sbError );
		} catch(Exception ex) {
			sbError.append("While building vector plot: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	static boolean zGenerateComposition( final Composition composition, PlotEnvironment environment, Model_Dataset model, OutputTarget eOutputOption, StringBuffer sbError ){
		return zGenerateComposition( composition, environment, model, eOutputOption, 1, 1, sbError );
	}

	static boolean zGenerateComposition(  final Composition composition, PlotEnvironment environment, Model_Dataset model, OutputTarget eOutputOption, int iFrameNumber, int ctFrames, StringBuffer sbError ){
		if( composition == null ){
			sbError.append( "internal error, no composition" );
			return false;
		}
		try {
			PlotScale scale;
			String sOutput;
			Panel_Composition plot_panel = null;
			switch( eOutputOption ){
				
				case ExternalWindow: // use the existing external window
				case NewWindow: // create a new window
					if( iFrameNumber > 1 ) Thread.sleep( miMultisliceDelay ); // wait for two seconds before continuing

					// establish frame
					JFrame frame;
				    if( eOutputOption == OutputTarget.ExternalWindow ){
				    	sOutput = "the external window";
						frame = mCompositionFrame;
						if( mPanel_ExternalWindow == null || mExternalWindowEnvironment != environment ){
							plot_panel = Panel_Composition._create( composition, sbError );
							if( plot_panel == null ){
								sbError.insert( 0, "failed to create fresh plot panel for external window: " );
								return false;
							}
						} else {
							plot_panel = mPanel_ExternalWindow;
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
						plot_panel = Panel_Composition._create( composition, sbError );
					    JComponent compToAdd;
						if( (float)composition.getWidth_pixels() > .9f * SCREEN_Width ||
						    (float)composition.getHeight_pixels() > .9f * SCREEN_Height ){
							if( iFrameNumber == 1 ) frame.setSize( SCREEN_Width, SCREEN_Height );
							JScrollPane jspNew = new JScrollPane( plot_panel );
							compToAdd = jspNew;
						} else {
							if( iFrameNumber == 1 ){
								int iExtraWidth = ApplicationController.getInstance().getFrame_ExtraWidth();
		    					int iExtraHeight = ApplicationController.getInstance().getFrame_ExtraHeight();
			    				int iFrameWidth = composition.getWidth_pixels() + iExtraWidth;
				    			int iFrameHeight = composition.getHeight_pixels() + iExtraHeight;
					    		frame.setSize(iFrameWidth, iFrameHeight);
							}
							compToAdd = plot_panel;
						}
						frame.getContentPane().add( compToAdd, BorderLayout.CENTER );
					}
//					if( ! plot_panel._zComposeRendering( plot, sbError ) ){
//						sbError.insert(0, "plotting to " + sOutput + ": ");
//						return false;
//					}

					frame.setVisible( true );
					Thread.yield();
					break;
					
				case FullScreen:
					sOutput = "full screen";
					boolean zNewFullScreenPanelCreated = false;
					if( mPanel_FullScreen == null ){
						plot_panel = Panel_Composition._create( null, sbError );
						zNewFullScreenPanelCreated = true;
					} else {
						if( mFullScreenEnvironment == environment ){
							plot_panel = mPanel_FullScreen; 
						} else {
							plot_panel = Panel_Composition._create( null, sbError );
							zNewFullScreenPanelCreated = true;
						}
					}
					if( plot_panel == null ){
						sbError.insert( 0, "failed to create panel for full screen plot" );
						return false;
					}
					mPanel_FullScreen = plot_panel; 
					mFullScreenEnvironment = environment;
//					if( ! plot_panel._zComposeRendering( plot, sbError ) ){
//						sbError.insert(0, "plotting to full screen: ");
//						return false;
//					}
					if( iFrameNumber > 1 ) Thread.sleep(miMultisliceDelay); // wait for two seconds before continuing
					if( zNewFullScreenPanelCreated ){
						scale = null; // ???.getPlotScale();
						int iPanelWidth = scale.getCanvas_Width_pixels();
						int iPanelHeight = scale.getCanvas_Height_pixels();
						java.awt.Container container = windowFullScreen_final.getContentPane();
						container.removeAll();
						container.add( plot_panel );
						Utility.vCenterComponent( plot_panel, iPanelWidth, iPanelHeight, SCREEN_Width, SCREEN_Height );
//						container.setBackground( plot_panel.getColor_Background() );
						windowFullScreen_final.setVisible(true);
					}
					Thread.yield();
					windowFullScreen_final.requestFocus(); // so window can handle key strokes
					break;
				case Print:
					plot_panel = Panel_Composition._create( null, sbError );
					if( plot_panel == null ){
						sbError.insert( 0, "failed to create panel for printing" );
						return false;
					}
//					if( ! plot_panel._zComposeRendering( plot, sbError ) ){
//						sbError.insert( 0, "failed to create rendering for printer" );
//						return false;
//					}
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
					plot_panel = Panel_View_Plot._getPreviewScrollPane()._getCanvas();
					if( iFrameNumber > 1 ) Thread.sleep( miMultisliceDelay ); // wait for two seconds before continuing
//					if( ! plot_panel._zComposeRendering( plot, sbError ) ){
//						sbError.insert(0, "plotting to preview pane: ");
//						return false;
//					}
					Thread.yield();
					break;
				case ExpressionPreview:
					sOutput = "expression preview pane";
					plot_panel = Panel_View_Plot._getPreviewScrollPane()._getCanvas();
					if( iFrameNumber > 1 ) Thread.sleep( miMultisliceDelay ); // wait for two seconds before continuing
//					if( ! plot_panel._zComposeRendering( plot, sbError ) ){
//						sbError.insert(0, "plotting to expression preview pane: ");
//						return false;
//					}
					if( iFrameNumber > 1 ) Thread.sleep( miMultisliceDelay ); // wait for two seconds before continuing
					ApplicationController.getInstance().getAppFrame().getDataViewer()._getPreviewPane()._setContent_Default();
					Thread.yield();
					break;
				case File_PNG:
					sOutput = "image file";
					if( mPanel_Image == null ){
						plot_panel = Panel_Composition._create( null, sbError );
						mPanel_Image = plot_panel;
					} else {
						plot_panel = mPanel_Image;
					}
////					if( ! plot_panel._zComposeRendering( plot, sbError ) ){
////						sbError.insert(0, "plotting to buffer for image: ");
////						return false;
////					}
					java.awt.image.RenderedImage imagePlot = null; // ??? plot_panel._getRenderedImage();
					if( imagePlot == null ){
						sbError.append("internal error, no image");
						return false;
					}
					if (jfc == null) jfc = new JFileChooser();
					int iState = jfc.showDialog(ApplicationController.getInstance().getAppFrame(), "Create File");
					File file = jfc.getSelectedFile();
					if (file == null || iState != JFileChooser.APPROVE_OPTION) return true; // user cancel
					if( !javax.imageio.ImageIO.write( imagePlot, "png", file ) ){
						sbError.append( "error writing file " + file );
						return false;
					}
					sOutput = "PNG file " + file;
					break;
				case Thumbnail:
					Panel_Thumbnails panelThumbnails = Panel_View_Plot._getPanel_Thumbnails();
					Panel_View_Plot._getPreviewScrollPane()._setContent( panelThumbnails );
					int pxThumbnailWidth = 0; // ???.environment.getOptions().get(PlotOptions.OPTION_ThumbnailWidth).getValue_int();
					int pxThumbnailHeight = 0; // // ??? plot.data.getDimension_y() * pxThumbnailWidth / plot.data.getDimension_x();
					int[] rgb_array = new int[ pxThumbnailWidth * pxThumbnailHeight ]; // TODO re use buffer
//					if( ! plot.render( rgb_array, pxThumbnailWidth, pxThumbnailHeight, sbError ) ){
//						sbError.append("Failed to render thumbnail: " + sbError);
//						return false;
//					}
					BufferedImage biThumbnail = new BufferedImage(pxThumbnailWidth, pxThumbnailHeight, BufferedImage.TYPE_INT_ARGB);
					biThumbnail.setRGB( 0, 0, pxThumbnailWidth, pxThumbnailHeight, rgb_array, 0, pxThumbnailWidth );
					Graphics g = biThumbnail.getGraphics();
					g.setColor(Color.BLACK);
					g.drawRect(0, 0, pxThumbnailWidth - 1, pxThumbnailHeight - 1);
//					panelThumbnails.addThumbnail( model, plot.getCaption(), biThumbnail );
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
//				ApplicationController.vShowStatus("Plotted " + ctFrames + " frames " + plot_panel._getID() + " to " + sOutput);
			} else {
//				ApplicationController.vShowStatus("Plotted " + plot_panel._getID() + " to " + sOutput);
			}
			return true;
		} catch(Exception ex) {
			sbError.append("While building output window: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

}

//class DataConnectorFile implements Serializable {
//	static final long serialVersionUID = 1;
//	private String msTitle;
//	private Model_Dataset mURL;
//	private DataDDS mData;
//	String getTitle(){ return msTitle; }
//	DataDDS getData(){ return mData; }
//	Model_Dataset getURL(){ return mURL; }
//	void setTitle(String sTitle){ msTitle = sTitle; }
//	void setData(DataDDS ddds){ mData = ddds; }
//	void setURL(Model_Dataset url){ mURL = url; }
//}


