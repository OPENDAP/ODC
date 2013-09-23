package opendap.clients.odc.plot;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import opendap.clients.odc.ApplicationController;

import org.python.core.PyObject;

// This panel is added to whatever output context is determined by Visualizer
// For example, if the output is delivered to the Data Preview Pane then it goes to the content pane of PreviewPane

public class Plot_Expression extends Plot {
	Model_PlottableExpression model;
	PlotCoordinates coordinates = null;
	ColorSpecification cs = null;
	private String msExpression = null;
	private org.python.core.PyCode pycodeExpression = null;
	private org.python.core.PyCode pycodeExpression_x = null;
	private org.python.core.PyCode pycodeExpression_y = null;
	private org.python.core.PyCode pycodeExpression_z = null;

	private Plot_Expression( PlotEnvironment environment, PlotLayout layout ){
		super( environment, layout );		
	}

	public boolean draw( StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}

	public static Plot_Expression create( PlotEnvironment environment, PlotLayout layout, Model_PlottableExpression model, String sCaption, StringBuffer sbError ){
		Plot_Expression plot = new Plot_Expression( environment, layout );
		// plot.data = data;
		plot.cs = environment.getColorSpecification();
		plot.msCaption = sCaption;
		return plot;
	}
	
	public boolean setExpressionModel( Model_PlottableExpression model, StringBuffer sbError ){
		if( model == null ){
			sbError.append( "expression model missing" );
			return false;
		}
		this.model = model;
		return true;
	}
	
	public String getDescriptor(){ return "E"; }

	public boolean render( int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){
		coordinates = model.getPlotCoordinates( pxPlotWidth, pxPlotHeight, sbError );
		if( coordinates == null ){
			sbError.insert( 0, "failed to get coordinates for plot dimensions (" + pxPlotWidth + ", " + pxPlotHeight +  "): " );
			return false;
		}
		if( coordinates.zIsFillPlot ){ // pseudocolor-type plot
			ColorSpecification cs = null; // TODO
			cs.render1to1( null, coordinates.madPlotBuffer );
		} else {
			if( coordinates.zHasAlpha ){
				if( coordinates.zHasColor ){
				} else {
					int argbBaseColor = coordinates.argbBaseColor;
					for( int xPoint = 0; xPoint < coordinates.ctPoints_antialiased; xPoint++ ){
						int argb = ( argbBaseColor & 0x00FFFFFF ) | ( coordinates.aAlpha[xPoint] << 24 ) ; // black
						int x = coordinates.ax0_antialiased[xPoint];
						int y = pxPlotHeight - coordinates.ay0_antialiased[xPoint] - 1;
						raster[ x + y * pxPlotHeight ] = argb;
					}
				}
			} else {
				if( coordinates.zHasColor ){
				} else {
					int argb = coordinates.argbBaseColor;
					for( int xPoint = 0; xPoint < coordinates.ctPoints; xPoint++ ){
						int coordinateX = coordinates.ax0[xPoint];
						int coordinateY = pxPlotHeight - coordinates.ay0[xPoint] - 1;
						raster[ coordinateX + coordinateY * pxPlotHeight ] = argb;
					}
				}
			}
		}
		return true;
	}

	private double[] y_value_calculated;
	private int[] y_value_plotted; // point values  (not anti-aliased or connected)
	private double[] x_value_calculated;
	private int[] x_value_plotted; // point values  (not anti-aliased or connected)

	public void vGenerateImage_Polar( BufferedImage bi, Graphics2D g2, int pxPlotWidth, int pxPlotHeight ){
		opendap.clients.odc.Interpreter interpreter = ApplicationController.getInstance().getInterpreter();
		StringBuffer sbError = new StringBuffer();

		// determine the figure sizing (how big the figure is as a proportion of the plot area)
		double dFigureProportion_x = 0.90; // 90%
		double dFigureProportion_y = 0.90; // 90%
		int pxFigureMargin_left = (int)((1 - dFigureProportion_x) * pxPlotWidth / 2);
		int pxFigureMargin_right = pxFigureMargin_left;
		int pxFigureMargin_top = (int)((1 - dFigureProportion_y) * pxPlotWidth / 2);
		int pxFigureMargin_bottom = pxFigureMargin_top;
		int pxPlotWidth_figure = pxPlotWidth - pxFigureMargin_left - pxFigureMargin_right;  
		int pxPlotHeight_figure = pxPlotHeight - pxFigureMargin_top - pxFigureMargin_bottom;  
		int pxPlotCenter_x = pxFigureMargin_left + pxPlotWidth_figure / 2; 
		int pxPlotCenter_y = pxFigureMargin_bottom + pxPlotHeight_figure / 2; 
		
		// determine the angular resolution in radians based on the plot area
		double dCornerDistance = Math.sqrt( pxPlotWidth_figure * pxPlotWidth_figure + pxPlotHeight_figure * pxPlotHeight_figure );
		double angular_resolution_radians = Math.asin( 1 / dCornerDistance );

		// size the plotting arrays according to the angular resolution
		int iRadialCount = (int)( Math.PI / angular_resolution_radians ); 
		y_value_calculated = new double[iRadialCount];
		y_value_plotted = new int[iRadialCount];
		x_value_calculated = new double[iRadialCount];
		x_value_plotted = new int[iRadialCount];
		double y_scale = 1d;
		double y_offset = 0d;
		double x_scale = 1d;
		double x_offset = 0d;
		
		// calculate the direct xy-values and determine xy-range for a full circle
		double y_max = Double.MIN_VALUE;
		double y_min = Double.MAX_VALUE;
		double x_max = Double.MIN_VALUE;
		double x_min = Double.MAX_VALUE;
		for( int xRadial = 1; xRadial <= iRadialCount; xRadial++ ){
			double theta = angular_resolution_radians * xRadial;
			Object oCurrentThetaValue = new Double( theta );
			if( ! interpreter.zSet( "_theta", oCurrentThetaValue, sbError ) ){
				ApplicationController.vShowError( "failed to set theta-value to " + oCurrentThetaValue + " for radial " + xRadial + ": " + sbError );
				return;
			}
			PyObject pyobject_range = interpreter.zEval( pycodeExpression, sbError );
			if( pyobject_range == null ){
				ApplicationController.vShowError( "failed to evaluate polar expression '" + msExpression + "' with theta value of " + oCurrentThetaValue + ": " + sbError );
				return;
			}
			Double dValue = pyobject_range.asDouble();
			y_value_calculated[xRadial] = dValue * Math.sin( theta ); 
			x_value_calculated[xRadial] = dValue * Math.cos( theta ); 
			if( y_value_calculated[xRadial] > y_max ) y_max = y_value_calculated[xRadial]; 
			else if( y_value_calculated[xRadial] < y_min ) y_min = y_value_calculated[xRadial];
			if( x_value_calculated[xRadial] > x_max ) x_max = x_value_calculated[xRadial]; 
			else if( x_value_calculated[xRadial] < x_min ) x_min = x_value_calculated[xRadial];
		}
		y_offset = y_min + pxPlotCenter_y;
		y_scale = pxPlotHeight_figure / (y_max - y_min);
		x_offset = x_min + pxPlotCenter_x;
		x_scale = pxPlotWidth_figure / (x_max - x_min);
		
		// plot the values, interpolate and anti-alias TODO
		int alpha = 128; // alpha channel
		int argb = 0xFFFFFFFF;
		for( int xRadial = 1; xRadial <= iRadialCount; xRadial++ ){
			y_value_plotted[xRadial] = (int)(y_value_calculated[xRadial] * y_scale + y_offset);
			x_value_plotted[xRadial] = (int)(x_value_calculated[xRadial] * x_scale + x_offset);
			bi.setRGB( x_value_plotted[xRadial], y_value_plotted[xRadial], argb );
		}
	}

	public void vGenerateImage_Parametric( Graphics2D g2, int pxPlotWidth, int pxPlotHeight ){
		opendap.clients.odc.Interpreter interpreter = ApplicationController.getInstance().getInterpreter();
		StringBuffer sbError = new StringBuffer();

		// determine the figure sizing (how big the figure is as a proportion of the plot area)
		double dFigureProportion_x = 0.90; // 90%
		double dFigureProportion_y = 0.90; // 90%
		int pxFigureMargin_left = (int)((1 - dFigureProportion_x) * pxPlotWidth / 2);
		int pxFigureMargin_right = pxFigureMargin_left;
		int pxFigureMargin_top = (int)((1 - dFigureProportion_y) * pxPlotWidth / 2);
		int pxFigureMargin_bottom = pxFigureMargin_top;
		int pxPlotWidth_figure = pxPlotWidth - pxFigureMargin_left - pxFigureMargin_right;  
		int pxPlotHeight_figure = pxPlotHeight - pxFigureMargin_top - pxFigureMargin_bottom;  
		int pxPlotCenter_x = pxFigureMargin_left + pxPlotWidth_figure / 2; 
		int pxPlotCenter_y = pxFigureMargin_bottom + pxPlotHeight_figure / 2; 
		
		// determine the increment TODO what happens when these are undefined?
		double dRangeBegin = interpreter.get( "_t_range_begin", sbError );
		if( dRangeBegin == Double.NEGATIVE_INFINITY ){
			ApplicationController.vShowError( "failed to get _t_range_begin: " + sbError );
			return;
		}
		double dRangeEnd = interpreter.get( "_t_range_end", sbError );
		if( dRangeEnd == Double.NEGATIVE_INFINITY ){
			ApplicationController.vShowError( "failed to get _t_range_end: " + sbError );
			return;
		}
		PyObject oStepCount = interpreter.getInteger( "_t_count", sbError );
		if( oStepCount == null ){
			ApplicationController.vShowError( "failed to get _t_count: " + sbError );
			return;
		}
		int iStepCount = oStepCount.asInt();
		if( iStepCount < 2 ){
			ApplicationController.vShowError( "invalid step count: " + iStepCount );
			return;
		}

		// size the plotting arrays according to the angular resolution
		y_value_calculated = new double[iStepCount];
		y_value_plotted = new int[iStepCount];
		x_value_calculated = new double[iStepCount];
		x_value_plotted = new int[iStepCount];
		double y_scale = 1d;
		double y_offset = 0d;
		double x_scale = 1d;
		double x_offset = 0d;
		
		// calculate the direct xy-values and determine xy-range for a full circle
		double y_max = Double.MIN_VALUE;
		double y_min = Double.MAX_VALUE;
		double x_max = Double.MIN_VALUE;
		double x_min = Double.MAX_VALUE;
		double dRange = dRangeEnd - dRangeBegin;
		for( int xStep = 1; xStep <= iStepCount; xStep++ ){
			double t = dRangeBegin + (xStep - 1) * dRange /  iStepCount;
			Object oCurrentTValue = new Double( t );
			if( ! interpreter.zSet( "_theta", oCurrentTValue, sbError ) ){
				ApplicationController.vShowError( "failed to set t-value to " + oCurrentTValue + " for step" + xStep + ": " + sbError );
				return;
			}
			PyObject pyobject_x = interpreter.zEval( pycodeExpression_x, sbError );
			if( pyobject_x == null ){
				ApplicationController.vShowError( "failed to evaluate parametric expression for x '" + msExpression + "' with t-value of " + oCurrentTValue + ": " + sbError );
				return;
			}
			PyObject pyobject_y = interpreter.zEval( pycodeExpression_y, sbError );
			if( pyobject_y == null ){
				ApplicationController.vShowError( "failed to evaluate parametric expression for y '" + msExpression + "' with t-value of " + oCurrentTValue + ": " + sbError );
				return;
			}
			y_value_calculated[xStep] = pyobject_x.asDouble();
			x_value_calculated[xStep] = pyobject_y.asDouble();
			if( y_value_calculated[xStep] > y_max ) y_max = y_value_calculated[xStep]; 
			else if( y_value_calculated[xStep] < y_min ) y_min = y_value_calculated[xStep];
			if( x_value_calculated[xStep] > x_max ) x_max = x_value_calculated[xStep]; 
			else if( x_value_calculated[xStep] < x_min ) x_min = x_value_calculated[xStep];
		}
		y_offset = y_min + pxPlotCenter_y;
		y_scale = pxPlotHeight_figure / (y_max - y_min);
		x_offset = x_min + pxPlotCenter_x;
		x_scale = pxPlotWidth_figure / (x_max - x_min);
		
		// plot the values, interpolate and anti-alias TODO
		int alpha = 128; // alpha channel
		int argb = 0xFFFFFFFF;
		y_value_plotted[1] = (int)(y_value_calculated[1] * y_scale + y_offset);
		x_value_plotted[1] = (int)(x_value_calculated[1] * x_scale + x_offset);
		for( int xStep = 1; xStep <= iStepCount; xStep++ ){
			x_value_plotted[xStep] = (int)(x_value_calculated[xStep] * x_scale + x_offset);
			y_value_plotted[xStep] = (int)(y_value_calculated[xStep] * y_scale + y_offset);
			g2.drawLine( x_value_plotted[xStep - 1], y_value_plotted[xStep - 1], y_value_plotted[xStep], y_value_plotted[xStep] );
		}
	}
	
	// overrides method in Panel_Plot
	public boolean zCreateRGBArray(int pxWidth, int pxHeight, boolean zAveraged, StringBuffer sbError){
		sbError.append("internal error, rgb array creation not applicable to plottable expressions");
		return false;
	}

	int iMicroscopeWidth = 10;
	int iMicroscopeHeight = 10;
	int[][] aRGB = new int[iMicroscopeWidth][iMicroscopeHeight];               // the raster that the microscope will display
	String[][] asData = new String[iMicroscopeWidth][iMicroscopeHeight];       // the data values as strings

	void vUpdateMicroscopeArrays( int xClick, int yClick, int pxPlotHeight ){
		if( coordinates == null ) return;
		for( int x = 0; x < iMicroscopeWidth; x++ ){
			for( int y = 0; y < iMicroscopeHeight; y++ ){
				aRGB[x][y] = 0xFFFFFFFF; // initialize to white
			}
		}
		if( coordinates.zHasAlpha ){
			if( coordinates.zHasColor ){
			} else {
				int argbBaseColor = coordinates.argbBaseColor;
				for( int xPoint = 0; xPoint < coordinates.ctPoints_antialiased; xPoint++ ){
					int x = coordinates.ax0_antialiased[xPoint];
					int y = pxPlotHeight - coordinates.ay0_antialiased[xPoint] - 1;
					if( x >= xClick && x < xClick + iMicroscopeWidth &&
						y >= yClick && y < yClick + iMicroscopeHeight ){
						int argb = ( argbBaseColor & 0x00FFFFFF ) | ( coordinates.aAlpha[xPoint] << 24 ) ; // black
						int xMicroscope = x - xClick;
						int yMicroscope = y - yClick;
						aRGB[xMicroscope][yMicroscope] = argb;
						asData[xMicroscope][yMicroscope] = Integer.toString( coordinates.aAlpha[xPoint] );
					}
				}
			}
		} else {
			if( coordinates.zHasColor ){
			} else {
				int argb = coordinates.argbBaseColor;
				for( int xPoint = 0; xPoint < coordinates.ctPoints; xPoint++ ){
					int coordinateX = coordinates.ax0[xPoint];
					int coordinateY = pxPlotHeight - coordinates.ay0[xPoint] - 1;
//					bi.setRGB( coordinateX, coordinateY, argb );
				}
			}
		}

	}

}
