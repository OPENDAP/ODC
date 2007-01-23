package opendap.clients.odc.plot;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility;
import java.awt.Color;
import java.awt.BasicStroke;

public class PlotLinestyle {
	public final static int DEFAULT_Line      = 1;
	public final static int DEFAULT_PlotBox   = 2;
	public final static int DEFAULT_Boundary  = 3;
	public final static int DEFAULT_Guideline = 4;
	public final static int DEFAULT_Coastline = 5;

	private Color mColor;
	private int miThickness;
	private String msDashPattern; // "3 2 5 6 : 5" (gap pattern : phase)
	private BasicStroke mStroke;

	public PlotLinestyle(){
		setStyle( DEFAULT_Line );
	}
	public PlotLinestyle( int eStyle ){
		setStyle( eStyle );
	}
	public void setStyle( int eStyle ){
		switch( eStyle ){
			case DEFAULT_PlotBox:
			case DEFAULT_Boundary:
			case DEFAULT_Guideline:
			case DEFAULT_Coastline:
			case DEFAULT_Line:
			default:
				mColor = Color.BLACK;
				setStroke(1, null);
		}
	}

	public Color getColor(){ return mColor; }
	public int getThickness(){ return miThickness; }
	public String getDashPattern(){ return msDashPattern; }
	BasicStroke getStroke(){ return mStroke; }

	public void setColor(Color color){
		if( color == null ) color = Color.BLACK;
		mColor = color;
	}

	public void setThickness(int iThickness){
		if( iThickness < 1 ) iThickness = 1;
		setStroke( iThickness, msDashPattern );
	}

	public void setDashPattern(String sDashPattern){
		setStroke( miThickness, sDashPattern );
	}

	private void setStroke( int iThickness, String sDashPattern ){
		if( msDashPattern == null || sDashPattern.length() == 0 ){
			if( iThickness < 1){
				miThickness = 1;
		    } else {
				miThickness = iThickness;
			}
			float fMiterLimit = miThickness;
			mStroke = new BasicStroke(miThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, fMiterLimit);
		} else {
			sDashPattern = sDashPattern.trim();
			String[] asPhase = Utility.split(sDashPattern, ':');
			float fDashPhase = 0f; // default;
			String sDashPhase = null;
			if( asPhase.length > 1 ){
				sDashPattern = asPhase[0].trim();
				sDashPhase = asPhase[1].trim();
			}
			int iDashPatternLength = 0;
			String[] asPattern = Utility.splitCommaWhiteSpace(sDashPattern);
			int ctIntervals = asPattern.length;
			float[] afDashPattern = new float[ctIntervals];
			for( int xInterval = 0; xInterval < ctIntervals; xInterval++ ){
				try {
					int iInterval = Integer.parseInt(asPattern[xInterval]);
					if( iInterval < 1 || iInterval >= iDashPatternLength ){
						ApplicationController.vShowError("Invalid dash interval (" + asPattern[xInterval] + "). Must be a positive integer. See help topic on line style.");
					}
					afDashPattern[xInterval] = (float)iInterval;
				} catch(Exception ex) {
					ApplicationController.vShowError("Unable to interpret dash phase " + sDashPhase + " as an integer. See help topic on line style.");
					return;
				}
			}
			if( sDashPhase != null && sDashPhase.length() > 0 ){
				try {
					int iDashPhase = Integer.parseInt(sDashPhase);
					if( iDashPhase < 0 || iDashPhase >= iDashPatternLength ){
						ApplicationController.vShowError("Dash phase " + sDashPhase + " cannot be < 0 or >= pattern length (" + iDashPatternLength + "). See help topic on line style.");
					}
					fDashPhase = (float)iDashPhase;
				} catch(Exception ex) {
					ApplicationController.vShowError("Unable to interpret dash phase " + sDashPhase + " as an integer. See help topic on line style.");
					return;
				}
			}
			float fMiterLimit = miThickness;
			mStroke = new BasicStroke(miThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, fMiterLimit, afDashPattern, fDashPhase);
			miThickness = iThickness;
			msDashPattern = sDashPattern;
		}
	}

}

class Panel_PlotLinestyle {
    public static void main(String[] args) {
    }
}