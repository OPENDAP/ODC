/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007-2011 OPeNDAP, Inc.
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
 * Title:        PlotAxis
 * Description:  Stores axis parameters
 * Copyright:    Copyright (c) 2002-2011
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.07
 */

import opendap.clients.odc.DAP;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_Numeric;
import opendap.clients.odc.plot.PlotLayout.LayoutStyle;
import opendap.clients.odc.plot.PlotScale.UNITS;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**  Plot Axis
 *   A plot axis is composed of three elements
 *     - baseline
 *     - one or more tick intervals
 *     - axis label
 */

public class PlotAxis {
	private static int serial_number = 0; // TODO remember
	private AxisStyle axis_style; 
	private AxisLayout mLayout = null;
	private AxisProjection mProjection = null;
	private AxisLabeling mLabeling = null;
	private java.awt.image.BufferedImage mbi = null;
	private PlotText listAxisText = PlotText.create();
	private String msID;
	public final static String TEXT_ID_Caption = "Caption";
	private transient boolean mzActive = false; // whether axis is active in the current environment

	public String getID(){ return msID; }
	public boolean getActive(){ return mzActive; }
	public void setActive( boolean z ){ mzActive = z; }
	public String toString(){
		return getID();
	}
	
	private PlotAxis(){ // use create() methods only
		serial_number++;
		msID = "Axis" + serial_number;
	}

	public static PlotAxis create(){
		PlotAxis axis = new PlotAxis();
		axis.mLayout = AxisLayout.createFreestanding();
		axis.mProjection = AxisProjection.create_Automatic();
		axis.mLabeling = AxisLabeling.create();
		return axis;
	}

	public static PlotAxis create( String sID ){
		PlotAxis axis = new PlotAxis();
		axis.mLayout = AxisLayout.createFreestanding();
		axis.mProjection = AxisProjection.create_Automatic();
		axis.mLabeling = AxisLabeling.create();
		axis.msID = sID;
		return axis;
	}
	
	public static PlotAxis createFreestanding( String sID ){
		PlotAxis axis = new PlotAxis();
		axis.mLayout = AxisLayout.createFreestanding();
		axis.mProjection = AxisProjection.create_Automatic();
		axis.mLabeling = AxisLabeling.create();
		axis.msID = sID;
		return axis;
	}
	
	public static PlotAxis createX( String sID ){
		PlotAxis axis = new PlotAxis();
		axis.mLayout = AxisLayout.createX();
		axis.mProjection = AxisProjection.create_Automatic();
		axis.mLabeling = AxisLabeling.create();
		axis.msID = sID;
		return axis;
	}

	public static PlotAxis createY( String sID ){
		PlotAxis axis = new PlotAxis();
		axis.mLayout = AxisLayout.createY();
		axis.mProjection = AxisProjection.create_Automatic();
		axis.mLabeling = AxisLabeling.create();
		axis.msID = sID;
		return axis;
	}
	
	public static PlotAxis createLinear_X( String sCaption, double dValue_from, double dValue_to ){
		PlotAxis axis = new PlotAxis();
		axis.mLayout = AxisLayout.createX();
		axis.mProjection = AxisProjection.create_LinearRange_Double( dValue_from, dValue_to );
		axis.mLabeling = AxisLabeling.create();
		if( sCaption != null ){
			PlotTextItem text = axis.listAxisText.getNew(TEXT_ID_Caption);
			text.setString( sCaption );
			PlotLayout layout = text.getPlotLayout();
			layout.setObject( PlotLayout.LAYOUT_OBJECT.AxisHorizontal );
			layout.setOrientation( PlotLayout.ORIENTATION.BottomMiddle);
			layout.setAlignment( PlotLayout.ORIENTATION.TopMiddle);
			layout.setOffsetVertical(5);
			layout.setRotation(0);
		}
		return axis;
	}
	
	public static PlotAxis createLinear_Y( String sCaption, double dValue_from, double dValue_to ){
		PlotAxis axis = new PlotAxis();
		axis.mLayout = AxisLayout.createY();
		axis.mProjection = AxisProjection.create_LinearRange_Double( dValue_from, dValue_to );
		axis.mLabeling = AxisLabeling.create();
		if( sCaption != null ){
			PlotTextItem text = axis.listAxisText.getNew(TEXT_ID_Caption);
			text.setString( sCaption );
			PlotLayout layout = text.getPlotLayout();
			layout.setObject( PlotLayout.LAYOUT_OBJECT.AxisVertical );
			layout.setOrientation( PlotLayout.ORIENTATION.LeftMiddle);
			layout.setAlignment( PlotLayout.ORIENTATION.RightMiddle);
			layout.setOffsetHorizontal(-10);
			layout.setRotation(270);
		}
		return axis;
	}
	
//	// determines the maximum possible height of the axis, not including the axis label
//	// includes the ticks and tick labeling
//	int getMaxIntervalHeightAboveBaseline(){
//		return 0;
//	}
//
//	// determines the width of the axis and its labeling below the baseline, inclusive 
//	int getMaxWidthBelowBaseline(){
//		return 0;
//	}
	
	// Note that the buffer is situated according to the layout specified in the layout variable.
	public java.awt.image.BufferedImage render( java.awt.Graphics2D g2, PlotScale scale, StringBuffer sbError ){
		int pxBufferWidth = 0;
		int pxBufferHeight = 0;
		if( mbi == null || mbi.getWidth() != pxBufferWidth || mbi.getHeight() != pxBufferHeight ){
			createBuffer( pxBufferWidth, pxBufferHeight );
		}
		if( ! axis_style.render( g2, mbi, scale, mLayout, mProjection, mLabeling, sbError ) ){
			sbError.insert( 0, "failed to render axis: " );
			return null;
		}
		return mbi;
	}
	
	public void createBuffer( int width, int height ){
		mbi = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB);
	}

	// Determine the maximum space each label takes on the axis (pxLabelLength)
	private int pxCalculateMaxLabelLength_double( Graphics g, Font font, boolean zOrthogonalLabels, double dValueFrom, double dValueTo ){
		java.awt.FontMetrics fm = g.getFontMetrics(font);
		if( zOrthogonalLabels ){
			return fm.getHeight();
		} else {
			double diff = dValueTo - dValueFrom; if( diff < 0 ) diff *= -1;
			int iOrder = (int)(Math.log(diff) / Math.log(10));
			String sFrom, sTo;
			if( iOrder >= 0 ){
				sTo = Utility.sDoubleToRoundedString(dValueTo, 0);
				sFrom = Utility.sDoubleToRoundedString(dValueFrom, 0);
			} else {
				int iDecimalPlaces = iOrder * -1;
				sTo = Utility.sDoubleToRoundedString(dValueTo, iDecimalPlaces);
				sFrom = Utility.sDoubleToRoundedString(dValueFrom, iDecimalPlaces);
			}
			int lenTo = fm.stringWidth(sTo);
			int lenFrom = fm.stringWidth(sFrom);
			return lenTo > lenFrom ? lenTo : lenFrom;
		}
	}
	
	void setCaption( String sCaption ){
		PlotTextItem text = listAxisText.getNew(TEXT_ID_Caption);
		if( text == null ) return;
		text.setString( sCaption );
	}
	String getCaption(){
		PlotTextItem text = listAxisText.getNew(TEXT_ID_Caption);
		if( text == null ) return null;
		return text.getString();
	}
	
	// ***************  NEW STUFF above
	// ***************  OLD STUFF below
	
	private boolean mzWholeNumbers;
	private int mDataTYPE = 0;
	private int mctValues = 0;
	private int[] maiValues = null;
	private double[] madValues1 = null;	private double mdValueFrom = 0;
	private double mdValueTo = 0;

	// tick quantities
	int mpxLowerOffset, mpxUpperOffset; // the offsets from the ends of the axis where the first and last ticks are located
	double mpxTick_MajorInterval = 0;
	int mpxTick_MediumInterval = 0;
	int mpxTick_MinorInterval = 0;

	// tick attributes
	private boolean mTick_z_show;
	private boolean mTick_z_pointing_out;
	private Color   mTick_color;
	private int     mTick_px_width;

	int getType(){ return mDataTYPE; }
	double[] getValues1(){ return madValues1; }
	void setValues(Object[] eggValues, int eDataTYPE){
		double[] adValues1;
		int len;
		switch( eDataTYPE ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				short[] ash = (short[])eggValues[0];
				len = ash.length;
				adValues1 = new double[len + 1];
				for( int xData = 0; xData < len; xData++ ) adValues1[xData + 1] = (double)ash[xData];
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				int[] ai = (int[])eggValues[0];
				len = ai.length;
				adValues1 = new double[len + 1];
				for( int xData = 0; xData < len; xData++ ) adValues1[xData + 1] = (double)ai[xData];
				break;
			case DAP.DATA_TYPE_UInt32:
				long[] an = (long[])eggValues[0];
				len = an.length;
				adValues1 = new double[len + 1];
				for( int xData = 0; xData < len; xData++ ) adValues1[xData + 1] = (double)(an[xData]);
				break;
			case DAP.DATA_TYPE_Float32:
				float[] af = (float[])eggValues[0];
				len = af.length;
				adValues1 = new double[len + 1];
				for( int xData = 0; xData < len; xData++ ) adValues1[xData + 1] = (double)af[xData];
				break;
			case DAP.DATA_TYPE_Float64:
				double[] ad = (double[])eggValues[0];
				len = ad.length;
				adValues1 = new double[len + 1];
				for( int xData = 0; xData < len; xData++ ) adValues1[xData + 1] = (double)ad[xData];
				break;
			case DAP.DATA_TYPE_String: return; // not supported
			default: return;
		}
		mDataTYPE = eDataTYPE;
		madValues1 = adValues1;
		double dMinValue = Double.MAX_VALUE;
		double dMaxValue = Double.MAX_VALUE * -1;
		int mctValues = madValues1.length - 1;
		for( int xValues = 1; xValues <= mctValues; xValues++ ){
			double dValue = madValues1[xValues];
			if( Double.isNaN(dValue) ) continue;
			if( dValue < dMinValue ) dMinValue = dValue;
			if( dValue > dMaxValue ) dMaxValue = dValue;
		}
		setRange( madValues1[1], madValues1[mctValues] );
	}

	void setRangeX( PlotRange range ){
		mdValueFrom = range.dBeginX;
		mdValueTo = range.dEndX;
//System.out.println("setting range " + mdValueFrom + " to " + mdValueTo);
	}

	void setRangeY( PlotRange range ){
		mdValueFrom = range.dBeginY;
		mdValueTo = range.dEndY;
//System.out.println("setting range " + mdValueFrom + " to " + mdValueTo);
	}
	
	void setRange( double dFrom, double dTo ){
		mdValueFrom = dFrom;
		mdValueTo = dTo;
//System.out.println("setting range " + mdValueFrom + " to " + mdValueTo);
	}

	void setIndexed( int iFrom, int iTo ){
		int len = iTo > iFrom ? iTo - iFrom + 1 : iFrom - iTo + 1;
		madValues1 = new double[len + 1];
		if( iTo > iFrom ){
			for( int x = iFrom; x <= iTo; x++ ){
				madValues1[x] = (double)x;
			}
		} else {
			for( int x = iFrom; x <= iTo; x-- ){
				madValues1[x] = (double)x;
			}
		}
		setRange( (double)iFrom, (double)iTo );
		mDataTYPE = DAP.DATA_TYPE_Float64;
	}

	private String[] masScaleLabels1 = null;
	String[] getScaleLabels1(){ return masScaleLabels1; }
	private int mctTicks;
	int getTickCount(){ return mctTicks; }

	public String sDump(){
		StringBuffer sb = new StringBuffer(80);
		sb.append("Axis:\n");
		sb.append("caption: " + getCaption() + "\n");
		sb.append("data type: " + DAP.getType_String(mDataTYPE) + "\n");
		sb.append("whole numbers: " + (mzWholeNumbers ? "Yes" : "No") + "\n");
		sb.append("value count: " + mctValues + "\n");
		sb.append("from: " + mdValueFrom + " to " + mdValueTo + "\n");
		return sb.toString();
	}

	double getValueFrom(){ return mdValueFrom; }
	double getValueTo(){ return mdValueTo; }

	/** The distance from lower bound before the first tick
	 * generated as a side effect of getting the scale interval */
	int getOffset_LowerPX(){ return mpxLowerOffset; }

	/** The distance to the upper bound from the last tick
	 * generated as a side effect of getting the scale interval */
	int getOffset_UpperPX(){ return mpxUpperOffset; }

	// getting the scale interval generates the labels as a side effect
	// sets:
	//   - mpxLength (axis length)
	//   - mpxTick_MajorInterval
	// generates:
	//   - masScaleLabels1
	//   - mpxLowerOffset and mpxUpperOffset
//	boolean zDetermineScaleInterval( int pxLength, Font font, boolean zOrthogonalLabels, Graphics g, boolean zDoBiasAdjustment, double dSlope, double dIntercept, boolean zRenderAsTime, StringBuffer sbError ){
//
//		// generate graphics to use
//		// java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(10,10,java.awt.image.BufferedImage.TYPE_INT_ARGB);
//		// Graphics g = bi.getGraphics();
//
//		// input values
//		double dValueFrom, dValueTo;
//		if( zDoBiasAdjustment ){
//			dValueFrom = mdValueFrom * dSlope + dIntercept;
//			dValueTo = mdValueTo * dSlope + dIntercept;
//		} else {
//			dValueFrom = mdValueFrom;
//			dValueTo = mdValueTo;
//		}
//
//		// set and validate axis length
//		mpxLength = pxLength;
//		double dRange = (dValueTo > dValueFrom) ? dValueTo - dValueFrom : dValueFrom - dValueTo;
//		if( pxLength == 0 ){
//			sbError.append("axis length is zero");
//			return false;
//		}
//		if( dRange == 0 ){
//			sbError.append("data range is zero");
//			return false;
//		}
//
//		int pxLabelLength = pxCalculateMaxLabelLength_double( g, font, zOrthogonalLabels, dValueFrom, dValueTo );
//		pxLabelLength += 3; // add some buffer space
//
//		double dDataInterval = dCalculateDataInterval( pxLabelLength, dRange );
//		mpxTick_MajorInterval = (((double)(mpxLength - 1)) * dDataInterval / dRange); // the length is shortened by one because the offset goes from say 0 to 9 if the length is 10
//		// calculate offsets and labels
//		int iIntervalOrder = (int)(Math.log(dDataInterval) / Math.log(10));
//		if( Math.abs(dDataInterval) < 1 ) iIntervalOrder--;
//		double dValueFrom_rounded = Utility.round(dValueFrom, iIntervalOrder-1);
//		double dValueTo_rounded = Utility.round(dValueTo, iIntervalOrder-1);
//		int ctDecimalPlaces = iIntervalOrder >= 0 ? 0 : iIntervalOrder * -1;
//		double dIncrement;
//		int ctTicks = (int)((double)mpxLength / mpxTick_MajorInterval) + 1;
//		masScaleLabels1 = new String[ctTicks + 2]; // one for the one-based array, one for the fencepost
//		mctTicks = 0;
//		boolean zAscending = dValueTo > dValueFrom;
////		dIncrement = Math.pow(10, iIntervalOrder - 1); // non-rounded numbers
//		dIncrement = dDataInterval; // rounded numbers
//		double dTick;
//		double dAspectRatio = mpxLength / dRange; // pixels per unit range
//		if( dValueFrom_rounded % dIncrement == 0 ){
//			dTick = dValueFrom_rounded;
//			mpxLowerOffset = 0;
//		} else {
//			if( zAscending ){
//				dTick = Utility.round_up(dValueFrom, iIntervalOrder);
////			    dTick = dLowerBound_rounded - Math.pow(10, iIntervalOrder);
////				double dStep = Math.pow(10, iIntervalOrder - 1);
////System.out.println("dtick search from " + dTick + " step: " + dStep);
////				while( dTick < dLowerBound_rounded ) dTick += dStep;
//				mpxLowerOffset = (int)((dTick - dValueFrom_rounded) * dAspectRatio);
//			} else {
//				dTick = Utility.round_down(dValueFrom, iIntervalOrder);
////				dTick = dLowerBound_rounded + Math.pow(10, iIntervalOrder);
////				double dStep = -1 * Math.pow(10, iIntervalOrder - 1);
////System.out.println("dtick search from " + dTick + " step: " + dStep);
////				while( dTick > dLowerBound_rounded ) dTick += dStep;
//				mpxLowerOffset = (int)((dValueFrom_rounded - dTick) * dAspectRatio);
//			}
//		}
//		if( !zAscending ) dIncrement *= -1;
//		while( (zAscending ? dTick <= dValueTo_rounded : dTick >= dValueTo_rounded ) ){
//			mctTicks++;
//			if( zRenderAsTime ){
//				masScaleLabels1[mctTicks] = sDetermineTimeLabel( dTick, dRange );
//			} else {
//				masScaleLabels1[mctTicks] = Utility.sDoubleToRoundedString(dTick, ctDecimalPlaces);
//			}
//			dTick += dIncrement;
//		}
//		if( dTick > dValueTo_rounded ){
//			mpxUpperOffset = (int)((dTick - dValueTo_rounded) * dAspectRatio);
//		} else {
//			mpxUpperOffset = (int)((dValueTo_rounded - dTick) * dAspectRatio);
//		}
//
//		return true;
//	}
//
//	static String msLastError = null;
//	private String sDetermineTimeLabel( Double dTimeValue_ms1970, Double dTimeRange_ms ){
//		long nTimeValue_ms1970;
//		long nTimeRange_ms;
//		try {
//			nTimeValue_ms1970 = Long.parseLong( Utility.sDoubleToRoundedString( dTimeValue_ms1970, 0 ) );
//			nTimeRange_ms = Long.parseLong( Utility.sDoubleToRoundedString( dTimeRange_ms, 0 ) );
//		} catch( Exception ex ) {
//			String sError = "Error converting double time value to long milliseconds since 1970: " + ex;
//			if( ! sError.equalsIgnoreCase( msLastError ) ){
//				ApplicationController.vShowError_NoModal( sError );
//				msLastError = sError;
//			}
//			return "Err";
//		}
//		java.util.Date dateTimeValue = new java.util.Date( nTimeValue_ms1970 );
//
//		// determine time resolution
//		if( nTimeRange_ms < 60000 ){ // use seconds
//		} else if( nTimeRange_ms < 60000 * 60 * 5 ){ // use minutes
//		} else if( nTimeRange_ms < 60000 * 60 * 72 ){ // use hours
//		} else if( nTimeRange_ms < 60000 * 60 * 24 * 30 * 6 ){ // use days
//		} else if( nTimeRange_ms < 60000 * 60 * 24 * 30 * 12 * 3 ){ // use months
//		} else { // use years
//		}
//		return null;
//	}

//	private double dCalculateDataInterval( int pxLabelLength, double dRange ){
//		int ctMaxLabels = mpxLength / pxLabelLength;
//		double dMinDataInterval = dRange / ctMaxLabels;
//		return determineInterval_Natural(dMinDataInterval); // round up to nearest multiple power of 10
//	}

	/* the natural interval is the multiple of the lower order bound of the interval
	For example, if the interval is 4 the natural interval is 4; if it is 16 the ni is 20,
	if it is 143 it is 200, if it is 642 it is 700 etc
	*/
	double determineInterval_Natural( double dInterval ){
		int iOrder = (int)(Math.log(dInterval) / Math.log(10));
		double dBase = Math.pow(10, iOrder);
		double dIncrement = Math.pow(10, iOrder);
		double dNaturalInterval = dBase;
		while( dNaturalInterval < dInterval ) dNaturalInterval += dIncrement;
		return dNaturalInterval;
	}

	/* rounds up to the nearest 10-power unless that number is greater than 3x the interval in which
	   case it is the nearest 10-power / 2
	*/
	double determineInterval_TensAndFives( double dInterval ){
		int iOrder = (int)(Math.log(dInterval) / Math.log(10));
		double dTenAndFive = Math.pow(10, iOrder + 1);
		if( dTenAndFive > 3 * dInterval ) dTenAndFive *= 0.5;
		return dTenAndFive;
	}

}

class AxisProjection {
	public enum AxisProjection_TYPE {
		Automatic,
		Range_Linear,
		Range_Function,
		IndexedValues
	}
	private AxisProjection_TYPE type;
	private Object eggValues;
	private DAP.DAP_TYPE data_type;
	private int range_iBegin = 0;
	private int range_iEnd = 100;
	private double range_dBegin = 0;
	private double range_dEnd = 100;
	private AxisProjection(){}
	public AxisProjection_TYPE getType(){ return type; }
	public DAP.DAP_TYPE getDataType(){ return data_type; }
	public int getRange_begin_int(){ return range_iBegin; }
	public int getRange_end_int(){ return range_iEnd; }
	public double getRange_begin_double(){ return range_dBegin; }
	public double getRange_end_double(){ return range_dEnd; }
	public static AxisProjection create_Automatic(){
		AxisProjection projection = new AxisProjection();
		projection.type = AxisProjection_TYPE.Automatic;
		return projection;
	}
	public static AxisProjection create_LinearRange_Integer(){
		AxisProjection projection = new AxisProjection();
		projection.type = AxisProjection_TYPE.Range_Linear;
		projection.data_type = DAP.DAP_TYPE.Int32;
		return projection;
	}
	public static AxisProjection create_LinearRange_Integer( int iBegin, int iEnd ){
		AxisProjection projection = new AxisProjection();
		projection.type = AxisProjection_TYPE.Automatic; 
		projection.data_type = DAP.DAP_TYPE.Int32;
		projection.range_iBegin = iBegin;
		projection.range_iEnd = iEnd;
		return projection;
	}
	public static AxisProjection create_LinearRange_Double(){
		AxisProjection projection = new AxisProjection();
		projection.data_type = DAP.DAP_TYPE.Float32;
		projection.type = AxisProjection_TYPE.Automatic; 
		return projection;
	}
	public static AxisProjection create_LinearRange_Double( double dBegin, double dEnd ){
		AxisProjection projection = new AxisProjection();
		projection.type = AxisProjection_TYPE.Range_Linear; 
		projection.data_type = DAP.DAP_TYPE.Float32;
		projection.range_dBegin = dBegin;
		projection.range_dEnd = dEnd;
		return projection;
	}
}

class AxisLayout {
	private AxisLayout(){}
	public static AxisLayout createFreestanding(){
		AxisLayout layout = new AxisLayout();
		layout.eAlignment = AxisAlignment.Freestanding;
		return layout;
	}
	public static AxisLayout createX(){
		AxisLayout layout = new AxisLayout();
		layout.eAlignment = AxisAlignment.X;
		return layout;
	}
	public static AxisLayout createY(){
		AxisLayout layout = new AxisLayout();
		layout.eAlignment = AxisAlignment.Y;
		return layout;
	}
	public enum AxisAlignment {
		X,
		Y,
		Radial,
		Axial,
		Freestanding,
		Path
	};	
	AxisAlignment eAlignment;
	UNITS eUnits = UNITS.Inches_Mils;
	int iOffset = 0;
	int iMarginLeft = 200;
	int iMarginRight = 200;
	int iThickness = 400;   // thickness orthogonal to the baseline
}

abstract class AxisStyle {
	public abstract boolean render( java.awt.Graphics2D g2, java.awt.image.BufferedImage mbi, PlotScale ps, AxisLayout layout, AxisProjection projection, AxisLabeling labeling, StringBuffer sbError );

	// these default methods can be overridden by particular styles
	// the size determinations must be exactly matched to the location
	public int getBufferSize_pxX( PlotScale scale, AxisLayout layout, StringBuffer sbError ){
		switch( layout.eAlignment ){
			case X:
				int pxMarginLeft = scale.dpiOutput * layout.iMarginLeft / 1000;
				int pxMarginRight = scale.dpiOutput * layout.iMarginRight / 1000;
				return scale.getPlot_Width() + pxMarginLeft + pxMarginRight;
			case Y:
				return scale.dpiOutput * layout.iThickness / 1000;
			case Radial:
			case Axial:
			case Freestanding:
			case Path:
				sbError.append( "unsupported alignment" );
				return -1;
			default:
				sbError.append( "unsupported alignment" );
				return -1;
		}
	}
	public int getBufferSize_pxY( PlotScale scale, AxisLayout layout, StringBuffer sbError ){
		switch( layout.eAlignment ){
			case X:
				return scale.dpiOutput * layout.iThickness / 1000;
			case Y:
				int pxMarginLeft = scale.dpiOutput * layout.iMarginLeft / 1000;
				int pxMarginRight = scale.dpiOutput * layout.iMarginRight / 1000;
				return scale.getPlot_Height() + pxMarginLeft + pxMarginRight;
			case Radial:
			case Axial:
			case Freestanding:
			case Path:
				sbError.append( "unsupported alignment" );
				return -1;
			default:
				sbError.append( "unsupported alignment" );
				return -1;
		}
	}
	public int getBufferLocation_pxX( PlotScale scale, AxisLayout layout, StringBuffer sbError ){
		switch( layout.eAlignment ){
			case X:
				int pxMarginLeft = scale.dpiOutput * layout.iMarginLeft / 1000;
				int pxMarginRight = scale.dpiOutput * layout.iMarginRight / 1000;
				return scale.getPlot_Width() + pxMarginLeft + pxMarginRight;
			case Y:
				return scale.dpiOutput * layout.iThickness / 1000;
			case Radial:
			case Axial:
			case Freestanding:
			case Path:
				sbError.append( "unsupported alignment" );
				return -1;
			default:
				sbError.append( "unsupported alignment" );
				return -1;
		}
	}
	public int getBufferLocation_pxY( PlotScale scale, AxisLayout layout, StringBuffer sbError ){
		switch( layout.eAlignment ){
			case X:
				int pxMarginLeft = scale.dpiOutput * layout.iMarginLeft / 1000;
				int pxMarginRight = scale.dpiOutput * layout.iMarginRight / 1000;
				return scale.getPlot_Width() + pxMarginLeft + pxMarginRight;
			case Y:
				return scale.dpiOutput * layout.iThickness / 1000;
			case Radial:
			case Axial:
			case Freestanding:
			case Path:
				sbError.append( "unsupported alignment" );
				return -1;
			default:
				sbError.append( "unsupported alignment" );
				return -1;
		}
	}
}

class AxisStyle_SingleTick extends AxisStyle {
	private AxisStyle_SingleTick(){} // create using create methods only
	public AxisStyle_SingleTick create(
			String sName,               // name of the interval (displayed in editor)
			PlotScale.UNITS units,      // units of measure (like mils, pixels, or mm)
			int length,                 // length orthogonal to the baseline                 
			int thickness,              // thickness in the direction of the baseline
			int offset,                 // distance from the baseline edge of the buffer
			int inversion,              // 1 or -1, if it is -1 the ticks will go into the plot instead of away from it
			int color,                  // color of the ticks in RGBA
			AxisLabeling labeling,  // labeling configuration, if any
			boolean zOmitOrigin,        // whether to include a tick at the origin, if there is one
			int modulo                  // the value divisor, eg 5 or 10, a divisor of 0 means automatic
		){
		AxisStyle_SingleTick interval = new AxisStyle_SingleTick();
		interval.msName = sName;               
		interval.scale_units = units;         
		interval.miLength = length;     
		interval.miThickness = thickness;             
		interval.miTickOffset = offset;           
		interval.miColor = color;             
		interval.mLabeling = labeling;
		interval.mzOmitOrigin = zOmitOrigin;
		interval.miModulo = modulo;
		return interval;
	}

	private String msName = "Default Interval";
	private PlotScale.UNITS scale_units = PlotScale.UNITS.Inches_Mils;
	private int miLength = 150;
	private int miThickness = 10;
	private int miTickOffset = 0; // the distance of the start of the tick from the plot side edge of the baseline
	private int miColor;
	private AxisLabeling mLabeling;
	private boolean mzOmitOrigin = false;
	private int miModulo = 0;
	private int miInversion = 1; // if this is -1 then the tick will go towards the plot instead of away from it
	int getOffsetFromOrigin_px( int ppi ){ // number of pixels from left/bottom of axis based on labeling
		return 0;
	}
	int getOffsetFromEndpoint_px( int ppi ){
		return 0;
	}
	
	public boolean render( java.awt.Graphics2D g2, java.awt.image.BufferedImage mbi, PlotScale ps, AxisLayout layout, AxisProjection projection, AxisLabeling labeling, StringBuffer sbError ){
		int pxBufferWidth = mbi.getWidth();
		int pxBufferHeight = mbi.getHeight();
		int pxAxisLength;
		int ctAxisLengthInches;
		int ctDivisions;
		int ctTicks;
		int x, x1, x2, y, y1, y2;
		int pxLeftMargin   = ps.dpiOutput * layout.iMarginLeft / 1000; // this is actually the top margin in the case of the y-axis
		int pxRightMargin   = ps.dpiOutput * layout.iMarginRight / 1000;  // bottom margin in case of y-axis
		int pxBaselineOffset = miInversion * ps.dpiOutput * layout.iOffset / 1000;  // conversion from mils
		int pxTickOffset     = ps.dpiOutput * miTickOffset / 1000;  // conversion from mils
		int pxTickLength     = miInversion * ps.dpiOutput * miLength / 1000;  // conversion from mils
		TextStyle style;
		int rotation_degrees = 0;
		PlotLayout.ORIENTATION alignmentTickLabel;
		DAP.DAP_TYPE data_type;
		switch( projection.getType() ){
			case Range_Linear:
				data_type = projection.getDataType();
				switch( layout.eAlignment ){
					case X:
						pxAxisLength = pxBufferWidth - layout.iMarginLeft - layout.iMarginRight;
						ctAxisLengthInches = pxAxisLength / ps.dpiOutput;
						ctDivisions = ctDivisions( ctAxisLengthInches );
						ctTicks = ctDivisions + 1;
						alignmentTickLabel = PlotLayout.ORIENTATION.TopMiddle; 
						style = TextStyle.create( LayoutStyle.Axis_X );
						DrawLine.drawLine_horizontal( g2, mbi, pxBaselineOffset, pxLeftMargin, pxLeftMargin + pxAxisLength ); // draw the baseline
						if( data_type == DAP.DAP_TYPE.Int32 ){
							int begin = projection.getRange_begin_int();
							int end = projection.getRange_end_int();
							int range = end - begin;
							int divisor = range / ctDivisions;
							int base = begin - begin % divisor; // the location of first tick
							y1 = pxBaselineOffset + pxTickOffset;
							y2 = pxBaselineOffset + pxTickOffset + pxTickLength;
							x = pxLeftMargin;
							if( begin == base ){ // then there is a tick at the left end of the axis
								DrawLine.drawLine_vertical( g2, mbi, pxLeftMargin, y1, y2 );
								String sTickLabel = labeling.getValueString( begin );
								DrawText.drawText( mbi, sTickLabel, style, x, y2, alignmentTickLabel, rotation_degrees );
							}
							for( int xDivision = 1; xDivision <= ctDivisions; xDivision++ ){
								int iDivisionValue = base + divisor * xDivision;  
								x = ( iDivisionValue - begin ) * pxAxisLength / range;    
								DrawLine.drawLine_vertical( g2, mbi, x, y1, y2 );
								String sTickLabel = labeling.getValueString( iDivisionValue );
								DrawText.drawText( mbi, sTickLabel, style, x, y2, alignmentTickLabel, rotation_degrees );
							}
							int iDivisionValue = base + divisor * ctTicks; // the value of the last tick  
							x = ( iDivisionValue - begin ) * pxAxisLength / range;    
							if( end == iDivisionValue ){ // then there is a tick at the end (facing inward)
								// x_baseline -= width of tick TODO when tick widths are supported
							}
							DrawLine.drawLine_vertical( g2, mbi, x, y1, y2 );
							String sTickLabel = labeling.getValueString( iDivisionValue );
							DrawText.drawText( mbi, sTickLabel, style, x, y2, alignmentTickLabel, rotation_degrees );
						} else if( data_type == DAP.DAP_TYPE.Float32 ){
							double begin = projection.getRange_begin_double();
							double end = projection.getRange_end_double();
							double range = end - begin;
							double divisor = range / ctDivisions;
							double base = begin - begin % divisor; // the location of first tick
							y1 = pxBaselineOffset + pxTickOffset;
							y2 = pxBaselineOffset + pxTickOffset + pxTickLength;
							if( begin == base ){ // then there is a tick at the left end of the axis
								x = pxLeftMargin;
								DrawLine.drawLine_vertical( g2, mbi, x, y1, y2 );
								String sTickLabel = labeling.getValueString( begin );
								DrawText.drawText( mbi, sTickLabel, style, x, y2, alignmentTickLabel, rotation_degrees );
							}
							for( int xDivision = 1; xDivision <= ctDivisions; xDivision++ ){
								double iDivisionValue = base + divisor * xDivision;  
								x = (int)Math.round( ( iDivisionValue - begin ) * pxAxisLength / range );    
								DrawLine.drawLine_vertical( g2, mbi, x, y1, y2 );
								String sTickLabel = labeling.getValueString( iDivisionValue );
								DrawText.drawText( mbi, sTickLabel, style, x, y2, alignmentTickLabel, rotation_degrees );
							}
							double iDivisionValue = base + divisor * ctTicks; // the value of the last tick  
							x = (int)Math.round( ( iDivisionValue - begin ) * pxAxisLength / range );    
							if( end == iDivisionValue ){ // then there is a tick at the end (facing inward)
								// x_baseline -= width of tick TODO when tick widths are supported
							}
							DrawLine.drawLine_vertical( g2, mbi, x, y1, y2 );
							String sTickLabel = labeling.getValueString( iDivisionValue );
							DrawText.drawText( mbi, sTickLabel, style, x, y2, alignmentTickLabel, rotation_degrees );
						} else {
							sbError.append( "unsupported data type for axis bounds: " + data_type );
							return false;
						}
						break;
					case Y: // axis from lower left to upper left of plot area
						pxAxisLength = pxBufferHeight - layout.iMarginLeft - layout.iMarginRight;
						ctAxisLengthInches = pxAxisLength / ps.dpiOutput;
						ctDivisions = ctDivisions( ctAxisLengthInches );
						ctTicks = ctDivisions + 1;
						alignmentTickLabel = PlotLayout.ORIENTATION.RightMiddle;
						style = TextStyle.create( LayoutStyle.Axis_Y );
						x = pxBufferWidth - pxBaselineOffset;
						y1 = pxLeftMargin;
						y2 = pxLeftMargin + pxAxisLength; 
						DrawLine.drawLine_vertical( g2, mbi, x, y1, y2 ); // draw the baseline
						data_type = projection.getDataType();
						if( data_type == DAP.DAP_TYPE.Int32 ){
							int begin = projection.getRange_begin_int();
							int end = projection.getRange_end_int();
							int range = end - begin;
							int divisor = range / ctDivisions;
							int base = begin - begin % divisor; // the location of first tick
							x1 = pxBufferWidth - pxBaselineOffset - pxTickOffset;
							x2 = pxBufferWidth - pxBaselineOffset - pxTickOffset - pxTickLength;
							if( begin == base ){ // then there is a tick at the bottom end of the axis
								y = pxBufferHeight - pxRightMargin;
								DrawLine.drawLine_horizontal( g2, mbi, y, x1, x2 );
								String sTickLabel = labeling.getValueString( begin );
								DrawText.drawText( mbi, sTickLabel, style, x2, y, alignmentTickLabel, rotation_degrees );
							}
							for( int xDivision = 1; xDivision <= ctDivisions; xDivision++ ){
								int iDivisionValue = base + divisor * xDivision;  
								y = pxBufferHeight - pxRightMargin - ( iDivisionValue - begin ) * pxAxisLength / range;    
								DrawLine.drawLine_horizontal( g2, mbi, y, x1, x2 );
								String sTickLabel = labeling.getValueString( iDivisionValue );
								DrawText.drawText( mbi, sTickLabel, style, x2, y, alignmentTickLabel, rotation_degrees );
							}
							int iDivisionValue = base + divisor * ctTicks; // the value of the last tick  
							y = pxBufferHeight - pxRightMargin - ( iDivisionValue - begin ) * pxAxisLength / range;    
							if( end == iDivisionValue ){ // then there is a tick at the end (facing inward)
								// x_baseline -= width of tick TODO when tick widths are supported
							}
							DrawLine.drawLine_horizontal( g2, mbi, y, x1, x2 );
							String sTickLabel = labeling.getValueString( iDivisionValue );
							DrawText.drawText( mbi, sTickLabel, style, x2, y, alignmentTickLabel, rotation_degrees );
						} else if( data_type == DAP.DAP_TYPE.Float32 ){
							double begin = projection.getRange_begin_double();
							double end = projection.getRange_end_double();
							double range = end - begin;
							double divisor = range / ctDivisions;
							double base = begin - begin % divisor; // the location of first tick
							x1 = pxBufferWidth - pxBaselineOffset - pxTickOffset;
							x2 = pxBufferWidth - pxBaselineOffset - pxTickOffset - pxTickLength;
							if( begin == base ){ // then there is a tick at the bottom end of the axis
								y = pxBufferHeight - pxRightMargin;
								DrawLine.drawLine_horizontal( g2, mbi, y, x1, x2 );
								String sTickLabel = labeling.getValueString( begin );
								DrawText.drawText( mbi, sTickLabel, style, x2, y, alignmentTickLabel, rotation_degrees );
							}
							for( int xDivision = 1; xDivision <= ctDivisions; xDivision++ ){
								double iDivisionValue = base + divisor * xDivision;  
								y = (int)Math.round( pxBufferHeight - pxRightMargin - ( iDivisionValue - begin ) * pxAxisLength / range );    
								DrawLine.drawLine_horizontal( g2, mbi, y, x1, x2 );
								String sTickLabel = labeling.getValueString( iDivisionValue );
								DrawText.drawText( mbi, sTickLabel, style, x2, y, alignmentTickLabel, rotation_degrees );
							}
							double iDivisionValue = base + divisor * ctTicks; // the value of the last tick  
							y = (int)Math.round( pxBufferHeight - pxRightMargin - ( iDivisionValue - begin ) * pxAxisLength / range );    
							if( end == iDivisionValue ){ // then there is a tick at the end (facing inward)
								// x_baseline -= width of tick TODO when tick widths are supported
							}
							DrawLine.drawLine_horizontal( g2, mbi, y, x1, x2 );
							String sTickLabel = labeling.getValueString( iDivisionValue );
							DrawText.drawText( mbi, sTickLabel, style, x2, y, alignmentTickLabel, rotation_degrees );
						} else {
							sbError.append( "unsupported data type for axis bounds: " + data_type );
							return false;
						}
						break;
					case Radial:
					case Axial:
					case Freestanding:
					case Path:
				}
			case Range_Function:
			case IndexedValues:
		}
		return true;
	}
	
	// logic to determine how many ticks there shouuld be by length of axis
	int ctDivisions( int ctAxisLengthInInches ){
		if( ctAxisLengthInInches == 1 ) return 2;
		if( ctAxisLengthInInches == 2 ) return 3;
		if( ctAxisLengthInInches <= 4 ) return 4;
		if( ctAxisLengthInInches <= 6 ) return 5;
		if( ctAxisLengthInInches <= 9 ) return 7;
		if( ctAxisLengthInInches <= 12 ) return 8;
		if( ctAxisLengthInInches <= 15 ) return 10;
		if( ctAxisLengthInInches <= 20 ) return 12;
		return ctAxisLengthInInches / 2;
	}
	
}

class AxisStyle_MajorMinor extends AxisStyle {
	public boolean render( java.awt.Graphics2D g2, java.awt.image.BufferedImage mbi, PlotScale ps, AxisLayout layout, AxisProjection projection, AxisLabeling labeling, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}

class AxisStyle_Boxed extends AxisStyle {
	public boolean render( java.awt.Graphics2D g2, java.awt.image.BufferedImage mbi, PlotScale ps, AxisLayout layout, AxisProjection projection, AxisLabeling labeling, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}
}

class AxisIntervalBox {
	private int miBorderThickness;
	private int miFill;
	private ColorSpecification mFillCS;
}

class AxisLabeling {
	private AxisLabeling(){}
	TextStyle text_style = null; // contains formatting specification
	String sFormatString = null;
	int iLogarithmicScale; // eg -1 means 1/10 and 2 means * 100
	public static AxisLabeling create(){
		AxisLabeling il = new AxisLabeling();
		il.text_style = new TextStyle();
		il.iLogarithmicScale = 0;
		il.sFormatString = null;		
 		return il;
	}
	int getValueWidth_pixels( Graphics2D g2, int iValue ){
		String sValue = String.format( sFormatString, iValue );
		return text_style.getTextWidthX_pixels( g2, sValue );
	}
	int getValueWidth_pixels( Graphics2D g2, double dValue ){
		String sValue = String.format( sFormatString, dValue );
		return text_style.getTextWidthX_pixels( g2, sValue );
	}
	String getValueString( int iValue ){
		if( iLogarithmicScale != 0 && iValue %10 != 0 ) return getValueString( (double)iValue ); // due to scaling the value is no longer an integer
		if( iLogarithmicScale > 0 ){
			iValue = iValue / Utility_Numeric.power10( 1, iLogarithmicScale ); 
		} else if( iLogarithmicScale < 0 ){
			iValue = iValue * Utility_Numeric.power10( 1, iLogarithmicScale ); 
		}
		if( sFormatString == null ){
			return String.format( sFormatString, iValue );
		} else {
			return Integer.toString( iValue );
		}
	}
	String getValueString( double dValue ){
		if( iLogarithmicScale > 0 ){
			dValue = dValue / Utility_Numeric.power10( 1, iLogarithmicScale ); 
		} else if( iLogarithmicScale < 0 ){
			dValue = dValue * Utility_Numeric.power10( 1, iLogarithmicScale ); 
		}
		if( sFormatString == null ){
			return String.format( sFormatString, dValue );
		} else {
			return Double.toString( dValue );
		}
	}
}




