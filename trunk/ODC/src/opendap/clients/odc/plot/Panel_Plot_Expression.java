package opendap.clients.odc.plot;

import java.awt.Graphics2D;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility_String;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

public class Panel_Plot_Expression extends Panel_Plot {
	
	public enum PlottableExpressionType {
		Cartesian,
		Polar,
		Parametric
	};

	private PlottableExpressionType mePlottableExpressionType;
	private String msExpression = null;
	private org.python.core.PyCode pycodeExpression = null;
	private org.python.core.PyCode pycodeExpression_x = null;
	private org.python.core.PyCode pycodeExpression_y = null;
	private org.python.core.PyCode pycodeExpression_z = null;
		
	Panel_Plot_Expression( PlotScale scale, String sID, String sCaption ){
		super( scale, sID, sCaption );
	}

	public boolean setExpression( String sScript, StringBuffer sbError ){
		if( sScript == null ){
			sbError.append( "script missing" );
			return false;
		}
		PythonInterpreter interpreter = ApplicationController.getInstance().getInterpreter().getInterpeter();
		PlottableExpressionType type;
//		String sExpression_macroed = Utility_String.sReplaceString( msExpression, "_x", "_value0" );
//	private org.python.core.PyCode pycodeExpression = interpreter.getInterpeter().compile( sExpression_macroed ); // the expression must be precompiled for performance reasons
		return true;
	}
	
	public String getDescriptor(){ return "E"; }

	// overrides method in Panel_Plot
	public void vGenerateImage( int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
		Graphics2D g2 = (Graphics2D)mbi.getGraphics();
		switch( mePlottableExpressionType ){
			case Cartesian:
				vGenerateImage_Cartesian( g2, pxPlotWidth, pxPlotHeight );
				break;
			case Polar:
				vGenerateImage_Polar( g2, pxPlotWidth, pxPlotHeight );
				break;
			case Parametric:
				vGenerateImage_Parametric( g2, pxPlotWidth, pxPlotHeight );
				break;
		}
	}

	private double[] y_value_calculated;
	private int[] y_value_plotted; // point values  (not anti-aliased or connected)
	private double[] x_value_calculated;
	private int[] x_value_plotted; // point values  (not anti-aliased or connected)

	public void vGenerateImage_Cartesian( Graphics2D g2, int pxPlotWidth, int pxPlotHeight ){
		opendap.clients.odc.Interpreter interpreter = ApplicationController.getInstance().getInterpreter();
		StringBuffer sbError = new StringBuffer();
		y_value_calculated = new double[pxPlotWidth];
		y_value_plotted = new int[pxPlotWidth];
		double y_scale = 1d;
		double y_offset = 0d;

		// calculate the direct y-values and determine y-range
		double y_max = Double.MIN_VALUE;
		double y_min = Double.MAX_VALUE;
		for( int x_value = 0; x_value < pxPlotWidth; x_value++ ){
			Object oCurrentXValue = new Integer( x_value );
			if( ! interpreter.zSet( "_x", oCurrentXValue, sbError ) ){
				ApplicationController.vShowError( "failed to set x-value to " + oCurrentXValue + ": " + sbError );
				return;
			}
			PyObject pyobject_y = interpreter.zEval( pycodeExpression, sbError );
			if( pyobject_y == null ){
				ApplicationController.vShowError( "failed to evaluate '" + msExpression + "': " + sbError );
				return;
			}
			Double dValue = pyobject_y.asDouble();
			y_value_calculated[x_value] = dValue; 
			if( dValue > y_max ) y_max = dValue; 
			else if( dValue < y_min ) y_min = dValue;
		}
		y_offset = y_min;
		y_scale = pxPlotHeight / (y_max - y_min);
		
		// plot the values, interpolate and anti-alias TODO
		int alpha = 128; // alpha channel
		int argb = 0xFFFFFFFF;
		y_value_plotted[0] = (int)(y_value_calculated[0] * y_scale + y_offset);
		for( int x_value = 1; x_value < pxPlotWidth; x_value++ ){
			y_value_plotted[x_value] = (int)(y_value_calculated[x_value] * y_scale + y_offset);
			if( y_value_plotted[x_value] > y_value_plotted[x_value - 1] ){
				for( int y_point = y_value_plotted[x_value - 1] + 1; y_point <= y_value_plotted[x_value]; y_point++ )
					mbi.setRGB( x_value, y_point, argb );
			} else if( y_value_plotted[x_value] < y_value_plotted[x_value - 1] ){
				for( int y_point = y_value_plotted[x_value - 1] - 1; y_point >= y_value_plotted[x_value]; y_point-- )
					mbi.setRGB( x_value, y_point, argb );
			} else { // horizontal line
				mbi.setRGB( x_value, y_value_plotted[x_value], argb );
			}
		}
	}

	public void vGenerateImage_Polar( Graphics2D g2, int pxPlotWidth, int pxPlotHeight ){
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
			mbi.setRGB( x_value_plotted[xRadial], y_value_plotted[xRadial], argb );
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
	
}
