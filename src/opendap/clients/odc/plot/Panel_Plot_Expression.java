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
		
	Panel_Plot_Expression( PlotScale scale, String sID, String sCaption ){
		super( scale, sID, sCaption );
	}

	public boolean setExpression( String sScript, PlottableExpressionType type, StringBuffer sbError ){
		PythonInterpreter interpreter = ApplicationController.getInstance().getInterpreter().getInterpeter();
//		String sExpression_macroed = Utility_String.sReplaceString( msExpression, "_x", "_value0" );
//	private org.python.core.PyCode pycodeExpression = interpreter.getInterpeter().compile( sExpression_macroed ); // the expression must be precompiled for performance reasons
		return true;
	}
	
	public String getDescriptor(){ return "E"; }

	// overrides method in Panel_Plot
	public void vGenerateImage( int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
		opendap.clients.odc.Interpreter interpreter = ApplicationController.getInstance().getInterpreter();
		StringBuffer sbError = new StringBuffer();
		Graphics2D g2 = (Graphics2D)mbi.getGraphics();
		Object oCurrentXValue = new Integer(1);
		switch( mePlottableExpressionType ){
			case Cartesian:
				if( ! interpreter.zSet( "_x", oCurrentXValue, sbError ) ){
					ApplicationController.vShowError( "failed to set x-value to " + oCurrentXValue + ": " + sbError );
					return;
				}
				PyObject pyobject_y = interpreter.zEval( pycodeExpression, msExpression, sbError );
			case Polar:
			case Parametric:
				
		}
//		for( int xLine = 1; xLine <= mctLines; xLine++ ){
//			g2.setColor(maColors1[xLine]);
//			int[][] aiXsegments = mapxX1[xLine];
//			int[][] aiYsegments = mapxY1[xLine];
//			for( int xSegment = 1; xSegment < aiXsegments.length; xSegment++ ){
//				int[] aiXcoordinates = aiXsegments[xSegment];
//				int[] aiYcoordinates = aiYsegments[xSegment];
//				if( mezShowLines ){
//					g2.drawPolyline(aiXcoordinates, aiYcoordinates, aiXcoordinates.length);
//				}
//				if( mezShowPoints ){
//					for( int xData = 0; xData < aiXcoordinates.length; xData++ ){
//						g2.fillOval(aiXcoordinates[xData] - pxCircleOffset, aiYcoordinates[xData] - pxCircleOffset, miCircleSize, miCircleSize);
//					}
//				}
//			}
//		}

	}

	// overrides method in Panel_Plot
	public boolean zCreateRGBArray(int pxWidth, int pxHeight, boolean zAveraged, StringBuffer sbError){
		sbError.append("internal error, rgb array creation not applicable to plottable expressions");
		return false;
	}
	
}
