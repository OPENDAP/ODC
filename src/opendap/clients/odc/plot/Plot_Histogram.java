package opendap.clients.odc.plot;

/**
 * Title:        Panel_Plot_Histogram
 * Description:  Plots histograms
 * Copyright:    Copyright (c) 2002-12
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.08
 */

import opendap.clients.odc.*;
import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.gui.Styles;

import java.awt.event.*;
import java.awt.*;

import javax.swing.*;

import java.io.*;
import java.util.ArrayList;

class Plot_Histogram extends Plot {

	private IPlottable data = null; // needed for data microscope
	private ColorSpecification cs = null;
	private PlotOptions po = null;
	
	private final static int DEFAULT_BAR_COUNT = 16;

	private final static int MAX_REPORT_LINES = 2400;

	private final static int iMIN_SIZE_WIDTH = 400;
	private final static int iMIN_SIZE_HEIGHT = 200;
	private final static Dimension MIN_DIMENSION = new Dimension(200, 400);
	private String mDisplay_sMessage = null;

	private Plot_Histogram( PlotEnvironment environment, PlotLayout layout ){
		super( environment, layout );		
	}

	public boolean draw( StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}

	public String getDescriptor(){ return "H"; }

	// Histogram Data
	private int[] maiClassSize1 = new int[1];
	private int[] maiClassBegin1 = new int[1];
	private int[] maiClassEnd1 = new int[1];
	int meTYPE; // see DAP.java for data types
	private double[] madSortedData = null;
	private float[] mafSortedData = null;
	private long[] manSortedData = null;
	private int[] maiSortedData = null;
	private short[] mashSortedData = null;
	private byte[] mabSortedData = null;
	private String[] masSortedData = null;
	private int mctMissing_Histogram;
	private int mctMissing_Histogram_Total = 0; // the number of missing values removed
	private double[] madMissing = null;
	private float[] mafMissing = null;
	private long[] manMissing = null;
	private int[] maiMissing = null;
	private short[] mashMissing = null;
	private byte[] mabMissing = null;
	private String[] masMissing = null;
	private int miDataElementCount = 0;
	private int miClassCount = 0;
	private int miMaxClassSize = 0;
	private short mshValue_Begin = 0;
	private short mshValue_End = 0;
	private int miValue_Begin = 0;
	private int miValue_End = 0;
	private long mnValue_Begin = 0;
	private long mnValue_End = 0;
	private float mfValue_Begin = 0f;
	private float mfValue_End = 0f;
	private double mdValue_Begin = 0d;
	private double mdValue_End = 0d;
	private String msValue_Begin = "";
	private String msValue_End = "";

	private int[] mxRectTopLeft = new int[1];
	private int[] myRectTopLeft = new int[1];
	private int[] mxRectBottomLeft = new int[1];
	private int[] myRectBottomLeft = new int[1];

	// Graphical Parameters
	private int mpxGraphOffset = 10; // this is the offset between the y-axis and left/right edges of the graph
	private int mpxVerticalTick_LabelOffset = 3;
	private int mpxHorizontalTick_LabelOffset = 3;
	private int mpxTickMajorLength = 8;
	private int mpxTickMediumLength = 5;
	private int mpxTickMinorLength = 3;
	private boolean mzBoxed = false;
	private int miVerticalScale = 0;
	private int miVerticalTick_MajorInterval = 0;
	private int miVerticalTick_MinorInterval = 0;
	private int miVerticalTick_MediumInterval = 0;
	
	private int mpxMargin_Top = 10; // TODO
	private int mpxMargin_Left = 10; // TODO
	private int mpxAxisThickness = 1; // TODO

	//int getGraphOffsetPixels(){ return mpxGraphOffset; }
	//void setGraphOffsetPixels( int iPixels ){ mpxGraphOffset = iPixels; }
	public static Plot_Histogram create( PlotEnvironment environment, PlotLayout layout, IPlottable data, String sCaption, StringBuffer sbError ){
		Plot_Histogram plot = new Plot_Histogram( environment, layout );
		plot.data = data;
		plot.cs = environment.getColorSpecification();
		plot.po = environment.getOptions();
		plot.msCaption = sCaption;
		return plot;
	}

	boolean setData( PlotScale scale, int eTYPE, Object[] eggData, Object[] eggMissing, StringBuffer sbError ){
		try {
			int pxCanvasWidth = scale.getCanvas_Width_pixels();
			int pxPlotWidth = scale.getPlot_Width_pixels();
			meTYPE = eTYPE;
			int ctMissing;
			switch(meTYPE){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:
					short[] ashData = (short[])eggData[0];
					miDataElementCount = ashData.length;
	    			if( !Utility.zMemoryCheck(miDataElementCount, 2, sbError) ) return false;
					mashSortedData = new short[miDataElementCount];
					System.arraycopy(ashData, 0, mashSortedData, 0, miDataElementCount);
					java.util.Arrays.sort(mashSortedData);

					// remove any missing values
					if( eggMissing != null && eggMissing[0] != null ){
						mashMissing = (short[])eggMissing[0];
						mctMissing_Histogram = mashMissing.length - 1;
						for( int xMissing = 1; xMissing <= mctMissing_Histogram ; xMissing++ ){
							int xMissing_begin = -1;
							int xMissing_end = -1;
							int xData = -1;
							short shMissingValue = mashMissing[xMissing];
							while(true){
								xData++;
								if( xData == miDataElementCount ) break; // missing val not found
								if( mashSortedData[xData] == shMissingValue ){
									xMissing_begin = xData;
									xMissing_end = xData;
									xData++;
									while( xData < miDataElementCount && mashSortedData[xData] == shMissingValue ){
										xMissing_end++;
										xData++;
									}
									int ctDataMissing = xMissing_end - xMissing_begin + 1;
		    		    			if( !Utility.zMemoryCheck(miDataElementCount - ctDataMissing, 2, sbError) ) return false;
									short[] ashDataNoMissing = new short[miDataElementCount - ctDataMissing];
									if( xMissing_begin > 0 ) System.arraycopy(mashSortedData, 0, ashDataNoMissing, 0, xMissing_begin);
									if( (miDataElementCount - xMissing_end - 1) > 0 ) System.arraycopy(mashSortedData, xMissing_end + 1, ashDataNoMissing, xMissing_begin, miDataElementCount - xMissing_end - 1);
									mashSortedData = ashDataNoMissing;
									miDataElementCount -= ctDataMissing;
									mctMissing_Histogram_Total   += ctDataMissing;
									break;
								}
							}
						}
					}
					break;
				case DAP.DATA_TYPE_UInt16:
				case DAP.DATA_TYPE_Int32:
					int[] aiData = (int[])eggData[0];
					miDataElementCount = aiData.length;
					try {
   		    			if( !Utility.zMemoryCheck(miDataElementCount, 4, sbError) ) return false;
						maiSortedData = new int[miDataElementCount];
					} catch(Throwable t ){
						sbError.append("unable to create " + miDataElementCount + " ints: " + t);
						return false;
					}
					System.arraycopy(aiData, 0, maiSortedData, 0, miDataElementCount);
					java.util.Arrays.sort(maiSortedData);

					// remove any missing values
					if( eggMissing != null && eggMissing[0] != null ){
						maiMissing = (int[])eggMissing[0];
						mctMissing_Histogram = maiMissing.length - 1;
						for( int xMissing = 1; xMissing <= mctMissing_Histogram ; xMissing++ ){
							int xMissing_begin = -1;
							int xMissing_end = -1;
							int xData = -1;
							int iMissingValue = maiMissing[xMissing];
							while(true){
								xData++;
								if( xData == miDataElementCount ) break; // missing val not found
								if( maiSortedData[xData] == iMissingValue ){
									xMissing_begin = xData;
									xMissing_end = xData;
									xData++;
									while( xData < miDataElementCount && maiSortedData[xData] == iMissingValue ){
										xMissing_end++;
										xData++;
									}
									int ctDataMissing = xMissing_end - xMissing_begin + 1;
		    		    			if( !Utility.zMemoryCheck(miDataElementCount - ctDataMissing, 4, sbError) ) return false;
									int[] aiDataNoMissing = new int[miDataElementCount - ctDataMissing];
									if( xMissing_begin > 0 ) System.arraycopy(maiSortedData, 0, aiDataNoMissing, 0, xMissing_begin);
									if( (miDataElementCount - xMissing_end - 1) > 0 ) System.arraycopy(maiSortedData, xMissing_end + 1, aiDataNoMissing, xMissing_begin, miDataElementCount - xMissing_end - 1);
									maiSortedData = aiDataNoMissing;
									miDataElementCount -= ctDataMissing;
									mctMissing_Histogram_Total   += ctDataMissing;
									break;
								}
							}
						}
					}
					break;
				case DAP.DATA_TYPE_UInt32:
					long[] anData = (long[])eggData[0];
					miDataElementCount = anData.length;
	    			if( !Utility.zMemoryCheck(miDataElementCount, 8, sbError) ) return false;
					manSortedData = new long[miDataElementCount];
					System.arraycopy(anData, 0, manSortedData, 0, miDataElementCount);
					java.util.Arrays.sort(manSortedData);

					// remove any missing values
					if( eggMissing != null && eggMissing[0] != null ){
						long[] manMissing = (long[])eggMissing[0];
						mctMissing_Histogram = manMissing.length - 1;
						for( int xMissing = 1; xMissing <= mctMissing_Histogram ; xMissing++ ){
							int xMissing_begin = -1;
							int xMissing_end = -1;
							int xData = -1;
							long nMissingValue = manMissing[xMissing];
							while(true){
								xData++;
								if( xData == miDataElementCount ) break; // missing val not found
								if( manSortedData[xData] == nMissingValue ){
									xMissing_begin = xData;
									xMissing_end = xData;
									xData++;
									while( xData < miDataElementCount && manSortedData[xData] == nMissingValue ){
										xMissing_end++;
										xData++;
									}
									int ctDataMissing = xMissing_end - xMissing_begin + 1;
					    			if( !Utility.zMemoryCheck(miDataElementCount - ctDataMissing, 8, sbError) ) return false;
									long[] anDataNoMissing = new long[miDataElementCount - ctDataMissing];
									if( xMissing_begin > 0 ) System.arraycopy(manSortedData, 0, anDataNoMissing, 0, xMissing_begin);
									if( (miDataElementCount - xMissing_end - 1) > 0 ) System.arraycopy(manSortedData, xMissing_end + 1, anDataNoMissing, xMissing_begin, miDataElementCount - xMissing_end - 1);
									manSortedData = anDataNoMissing;
									miDataElementCount -= ctDataMissing;
									mctMissing_Histogram_Total   += ctDataMissing;
									break;
								}
							}
						}
					}
					break;
				case DAP.DATA_TYPE_Float32:
					float[] afData = (float[])eggData[0];
					miDataElementCount = afData.length;
	    			if( !Utility.zMemoryCheck(miDataElementCount, 4, sbError) ) return false;
					mafSortedData = new float[miDataElementCount];
					System.arraycopy(afData, 0, mafSortedData, 0, miDataElementCount);
					java.util.Arrays.sort(mafSortedData);

					// remove any missing values
					if( eggMissing != null && eggMissing[0] != null ){
						mafMissing = (float[])eggMissing[0];
						mctMissing_Histogram = mafMissing.length - 1;
						for( int xMissing = 1; xMissing <= mctMissing_Histogram ; xMissing++ ){
							int xMissing_begin = -1;
							int xMissing_end = -1;
							int xData = -1;
							float fMissingValue = mafMissing[xMissing];
							while(true){
								xData++;
								if( xData == miDataElementCount ) break; // missing val not found
								if( mafSortedData[xData] == fMissingValue ){
									xMissing_begin = xData;
									xMissing_end = xData;
									xData++;
									while( xData < miDataElementCount && mafSortedData[xData] == fMissingValue ){
										xMissing_end++;
										xData++;
									}
									int ctDataMissing = xMissing_end - xMissing_begin + 1;
					    			if( !Utility.zMemoryCheck(miDataElementCount - ctDataMissing, 4, sbError) ) return false;
									float[] afDataNoMissing = new float[miDataElementCount - ctDataMissing];
									if( xMissing_begin > 0 ) System.arraycopy(mafSortedData, 0, afDataNoMissing, 0, xMissing_begin);
									if( (miDataElementCount - xMissing_end - 1) > 0 ) System.arraycopy(mafSortedData, xMissing_end + 1, afDataNoMissing, xMissing_begin, miDataElementCount - xMissing_end - 1);
									mafSortedData = afDataNoMissing;
									miDataElementCount -= ctDataMissing;
									mctMissing_Histogram_Total   += ctDataMissing;
									break;
								}
							}
						}
					}
					break;
				case DAP.DATA_TYPE_Float64:
					double[] adData = (double[])eggData[0];
					miDataElementCount = adData.length;
	    			if( !Utility.zMemoryCheck(miDataElementCount, 8, sbError) ) return false;
					madSortedData = new double[miDataElementCount];
					System.arraycopy(adData, 0, madSortedData, 0, miDataElementCount);
					java.util.Arrays.sort(madSortedData);

					// remove any missing values
					if( eggMissing != null && eggMissing[0] != null ){
						madMissing = (double[])eggMissing[0];
						mctMissing_Histogram = madMissing.length - 1;
						for( int xMissing = 1; xMissing <= mctMissing_Histogram ; xMissing++ ){
							int xMissing_begin = -1;
							int xMissing_end = -1;
							int xData = -1;
							double dMissingValue = madMissing[xMissing];
							while(true){
								xData++;
								if( xData == miDataElementCount ) break; // missing val not found
								if( madSortedData[xData] == dMissingValue ){
									xMissing_begin = xData;
									xMissing_end = xData;
									xData++;
									while( xData < miDataElementCount && madSortedData[xData] == dMissingValue ){
										xMissing_end++;
										xData++;
									}
									int ctDataMissing = xMissing_end - xMissing_begin + 1;
		    		    			if( !Utility.zMemoryCheck(miDataElementCount - ctDataMissing, 8, sbError) ) return false;
									double[] adDataNoMissing = new double[miDataElementCount - ctDataMissing];
									if( xMissing_begin > 0 ) System.arraycopy(madSortedData, 0, adDataNoMissing, 0, xMissing_begin);
									if( (miDataElementCount - xMissing_end - 1) > 0 ) System.arraycopy(madSortedData, xMissing_end + 1, adDataNoMissing, xMissing_begin, miDataElementCount - xMissing_end - 1);
									madSortedData = adDataNoMissing;
									miDataElementCount -= ctDataMissing;
									mctMissing_Histogram_Total   += ctDataMissing;
									break;
								}
							}
						}
					}
					break;
				case DAP.DATA_TYPE_String:
					String[] asData = (String[])eggData[0];
					miDataElementCount = asData.length;
	    			if( !Utility.zMemoryCheck(miDataElementCount, 2, sbError) ) return false;
					masSortedData = new String[miDataElementCount];
					System.arraycopy(asData, 0, masSortedData, 0, miDataElementCount);
					java.util.Arrays.sort(masSortedData);

					// remove any missing values
					if( eggMissing != null && eggMissing[0] != null ){
						masMissing = (String[])eggMissing[0];
						mctMissing_Histogram = masMissing.length - 1;
						for( int xMissing = 1; xMissing <= mctMissing_Histogram ; xMissing++ ){
							int xMissing_begin = -1;
							int xMissing_end = -1;
							int xData = -1;
							String sMissingValue = masMissing[xMissing];
							while(true){
								xData++;
								if( xData == miDataElementCount ) break; // missing val not found
								if( masSortedData[xData] == sMissingValue ){
									xMissing_begin = xData;
									xMissing_end = xData;
									xData++;
									while( xData < miDataElementCount && masSortedData[xData] == sMissingValue ){
										xMissing_end++;
										xData++;
									}
									int ctDataMissing = xMissing_end - xMissing_begin + 1;
					    			if( !Utility.zMemoryCheck(miDataElementCount - ctDataMissing, 2, sbError) ) return false;
									String[] asDataNoMissing = new String[miDataElementCount - ctDataMissing];
									if( xMissing_begin > 0 ) System.arraycopy(masSortedData, 0, asDataNoMissing, 0, xMissing_begin);
									if( (miDataElementCount - xMissing_end - 1) > 0 ) System.arraycopy(masSortedData, xMissing_end + 1, asDataNoMissing, xMissing_begin, miDataElementCount - xMissing_end - 1);
									masSortedData = asDataNoMissing;
									miDataElementCount -= ctDataMissing;
									mctMissing_Histogram_Total   += ctDataMissing;
									break;
								}
							}
						}
					}
					break;
			}
			if( po == null ){
				setClass_Count(DEFAULT_BAR_COUNT, pxCanvasWidth); // the bar count affects the vertical scale
			} else {
				int iClassCount = po.getValue_int(PlotOptions.OPTION_HistogramClassCount);
				if( iClassCount <= 0 ){
					setClass_Minimum(pxPlotWidth);
				} else {
					setClass_Count( iClassCount, pxPlotWidth );
				}
			}
			return true;
		} catch(Throwable ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	public boolean zCreateRGBArray(int pxWidth, int pxHeight, boolean zAveraged, StringBuffer sbError){
		sbError.append("internal error, rgb array creation not applicable to plot line");
		return false;
	}

	long getBarCount(){ return miClassCount; }
	double getValue_Begin(){ return mdValue_Begin; }
	double getValue_End(){ return mdValue_End; }
	void setClass_Minimum( int pxPlotWidth ){
		if( miDataElementCount == 0 ) return; // nothing is possible
		int pxGraphWidth = pxPlotWidth - mpxGraphOffset * 2;
		if( pxGraphWidth < 3 ){ miClassCount = 0; return; } // not possible to graph
		miClassCount = pxGraphWidth;
		vDetermineClassSizes();
	}
	void setClass_Count( int iNewBarCount, int pxPlotWidth ){
		if( miDataElementCount == 0 ) return; // nothing is possible
		if( iNewBarCount < 3 ) return; // nothing is possible
		int pxGraphWidth = pxPlotWidth - mpxGraphOffset * 2;
		if( pxGraphWidth < 3 ){ miClassCount = 0; return; } // not possible to graph
		if( miClassCount > pxGraphWidth ) miClassCount = pxGraphWidth; else miClassCount = iNewBarCount;
		vDetermineClassSizes();
	}
	void setClass_Interval( double dNewInterval, int pxPlotWidth ){
		if( pxPlotWidth < 3 ){ miClassCount = 0; return; } // not possible to graph
		if( miDataElementCount == 0 ) return; // nothing is possible
		if( dNewInterval <= 0 ) return; // invalid input
		double dBegin = getValue_double(0);
		double dEnd = getValue_double(miDataElementCount - 1);
		double dDataRange = dBegin - dEnd;
		if( dNewInterval > dDataRange/3 ) dNewInterval = dDataRange/3; // maximum interval
		long nProvisionalClassCount = Math.round( dDataRange / dNewInterval );
		if( nProvisionalClassCount > pxPlotWidth ){
			setClass_Minimum( pxPlotWidth );
		} else {
			miClassCount = (int)nProvisionalClassCount;
			vDetermineClassSizes();
		}
	}

	private void vDetermineClassSizes(){

		// adjust miClassCount if integral value
		int ctPossibleValues;
		switch(meTYPE){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				mshValue_Begin = mashSortedData[0];
				mshValue_End = mashSortedData[miDataElementCount - 1];
				ctPossibleValues = (int)mshValue_End - (int)mshValue_Begin + 1;
				if( miClassCount > ctPossibleValues && ctPossibleValues > 0 ) miClassCount = ctPossibleValues;
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				miValue_Begin = maiSortedData[0];
				miValue_End = maiSortedData[miDataElementCount - 1];
				ctPossibleValues = miValue_End - miValue_Begin + 1;
				if( miClassCount > ctPossibleValues && ctPossibleValues > 0 ) miClassCount = ctPossibleValues; // must be > 0 because could wrap
				break;
			case DAP.DATA_TYPE_UInt32:
				mnValue_Begin = manSortedData[0];
				mnValue_End = manSortedData[miDataElementCount - 1];
				long ctPossibleValues_long = mnValue_End - mnValue_Begin + 1;
				if( miClassCount > ctPossibleValues_long && ctPossibleValues_long > 0 ) miClassCount = (int)ctPossibleValues_long;
				break;
			default:
				// no adjustment
		}

		maiClassSize1   = new int[miClassCount + 1];
		maiClassBegin1  = new int[miClassCount + 1];
		maiClassEnd1    = new int[miClassCount + 1];
		mxRectTopLeft = new int[miClassCount + 1];
		myRectTopLeft = new int[miClassCount + 1];
		mxRectBottomLeft = new int[miClassCount + 1];
		myRectBottomLeft = new int[miClassCount + 1];
		miMaxClassSize = 0;
		int iInterval_begin = 0;
		int iInterval_end = 0;
		long nDataRange = 0;
		long nValue_Interval = 0;
		maiClassBegin1[1] = 0; // the first class always begins with the first data element
		maiClassEnd1[miClassCount] = miDataElementCount - 1; // the last class always ends with the last data element

		{ // counting loop
		int iClass = 1;
		int xData = 0;
		switch(meTYPE){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:

				// determine interval
				mshValue_Begin = mashSortedData[0];
				mshValue_End = mashSortedData[miDataElementCount - 1];
				int iDataRange = (int)mshValue_End - (int)mshValue_Begin + 1;
				short shValue_Interval = (short)(iDataRange / miClassCount);

				// build class size array
				int shInterval_next = mshValue_Begin + shValue_Interval;
				while( true ){
					if( xData == miDataElementCount ) break;
					if( mashSortedData[xData] <= shInterval_next ){
						maiClassSize1[iClass]++;
						xData++;
					} else {
						maiClassEnd1[iClass] = xData - 1; // will be -1 if class size is zero
						while( true ){
							iClass++;
							shInterval_next += shValue_Interval;
							if( mashSortedData[xData] <= shInterval_next || iClass == miClassCount){
								maiClassBegin1[iClass] = xData;
								maiClassSize1[iClass]  = 1;
								xData++;
								break;
							}
						}
						if( iClass == miClassCount ) break;
					}
				}
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:

				// determine interval
				miValue_Begin = maiSortedData[0];
				miValue_End = maiSortedData[miDataElementCount - 1];
				iDataRange = miValue_End - miValue_Begin + 1;
				int iValue_Interval = iDataRange / miClassCount;

				// build class size array
				int iInterval_next = miValue_Begin + iValue_Interval;
				while( true ){
					if( xData == miDataElementCount ) break;
					if( maiSortedData[xData] <= iInterval_next ){
						maiClassSize1[iClass]++;
						xData++;
					} else {
						maiClassEnd1[iClass] = xData - 1; // will be -1 if class size is zero
						while( true ){
							iClass++;
							iInterval_next += iValue_Interval;
							if( maiSortedData[xData] <= iInterval_next || iClass == miClassCount){
								maiClassBegin1[iClass] = xData;
								maiClassSize1[iClass]  = 1;
								xData++;
								break;
							}
						}
						if( iClass == miClassCount ) break;
					}
				}
				break;
			case DAP.DATA_TYPE_UInt32:

				// determine interval
				mnValue_Begin = manSortedData[0];
				mnValue_End = manSortedData[miDataElementCount - 1];
				nDataRange = mnValue_End - mnValue_Begin + 1;
				nValue_Interval = nDataRange / miClassCount;

				// build class size array
				long nInterval_next = mnValue_Begin + nValue_Interval;
				while( true ){
					if( xData == miDataElementCount ) break;
					if( manSortedData[xData] <= nInterval_next ){
						maiClassSize1[iClass]++;
						xData++;
					} else {
						maiClassEnd1[iClass] = xData - 1; // will be -1 if class size is zero
						while( true ){
							iClass++;
							nInterval_next += nValue_Interval;
							if( manSortedData[xData] <= nInterval_next || iClass == miClassCount){
								maiClassBegin1[iClass] = xData;
								maiClassSize1[iClass]  = 1;
								xData++;
								break;
							}
						}
						if( iClass == miClassCount ) break;
					}
				}
				break;
			case DAP.DATA_TYPE_Float32:

				// determine interval
				mfValue_Begin = mafSortedData[0];
				mfValue_End = mafSortedData[miDataElementCount - 1];
				float fDataRange = mfValue_End - mfValue_Begin;
				float fValue_Interval = fDataRange / miClassCount;

				// build class size array
				float fInterval_next = mfValue_Begin + fValue_Interval;
				while( true ){
					if( xData == miDataElementCount ) break;
					if( mafSortedData[xData] <= fInterval_next ){
						maiClassSize1[iClass]++;
						xData++;
					} else {
						maiClassEnd1[iClass] = xData - 1; // will be -1 if index is zero
						while( true ){
							iClass++;
							fInterval_next += fValue_Interval;
							if( mafSortedData[xData] <= fInterval_next || iClass == miClassCount){
								maiClassBegin1[iClass] = xData;
								maiClassSize1[iClass]  = 1;
								xData++;
								break;
							}
						}
						if( iClass == miClassCount ) break;
					}
				}
				break;

			case DAP.DATA_TYPE_Float64:

				// determine interval
				mdValue_Begin = madSortedData[0];
				mdValue_End = madSortedData[miDataElementCount - 1];
				double dDataRange = mdValue_End - mdValue_Begin;
				double dValue_Interval = dDataRange / miClassCount;

				// build class size array
				double dInterval_next = mdValue_Begin + dValue_Interval;
				while( true ){
					if( xData == miDataElementCount ) break;
					if( madSortedData[xData] <= dInterval_next ){
						maiClassSize1[iClass]++;
						xData++;
					} else {
						maiClassEnd1[iClass] = xData - 1; // will be -1 if class size is zero
						while( true ){
							iClass++;
							dInterval_next += dValue_Interval;
							if( madSortedData[xData] <= dInterval_next || iClass == miClassCount){
								maiClassBegin1[iClass] = xData;
								maiClassSize1[iClass]  = 1;
								xData++;
								break;
							}
						}
						if( iClass == miClassCount ) break;
					}
				}
				break;
			case DAP.DATA_TYPE_String: // does not work currently

				// determine interval
				msValue_Begin = masSortedData[0];
				msValue_End = masSortedData[miDataElementCount - 1];
				String sValue_Interval = "";

				// build class size array
				String sInterval_next = msValue_Begin + sValue_Interval; // todo
				while( true ){
					if( xData == miDataElementCount ) break;
					if( masSortedData[xData].compareTo(sInterval_next) <= 0 ){
						maiClassSize1[iClass]++;
						xData++;
					} else {
						maiClassEnd1[iClass] = xData - 1; // will be -1 if class size is zero
						while( true ){
							iClass++;
							sInterval_next += sValue_Interval;
							if( masSortedData[xData].compareTo( sInterval_next ) <= 0 || iClass == miClassCount){
								maiClassBegin1[iClass] = xData;
								maiClassSize1[iClass]  = 1;
								xData++;
								break;
							}
						}
						if( iClass == miClassCount ) break;
					}
				}
				break;
		}
	    	maiClassEnd1[iClass] = miDataElementCount - 1; // the last (used) class always ends with the last data element
		    int iPreviousOccupiedClassEnd = 0;
			int xPreviousOccupiedClass1 = iClass - 1;
			while( true ){ // look for last occupied class before the final one
				if( xPreviousOccupiedClass1 < 1 ) break;
				if( maiClassEnd1[xPreviousOccupiedClass1] > 0 ){
					iPreviousOccupiedClassEnd = maiClassEnd1[xPreviousOccupiedClass1];
					break;
				}
				xPreviousOccupiedClass1--;
			}
			maiClassSize1[iClass]  = miDataElementCount - (iPreviousOccupiedClassEnd + 1); // +1 is because the var is zero-based
		} // end counting loop

		// determine maximum class size
		for( int xClass = 1; xClass <= miClassCount; xClass++ ){
			if( maiClassSize1[xClass] > miMaxClassSize ) miMaxClassSize = maiClassSize1[xClass];
		}

		vDetermineVerticalScale();
	}
	void vDetermineVerticalScale(){
		double dOrder = Math.log(miMaxClassSize) / Math.log(10);
		int iOrder =  (int)dOrder;
		int iMultiplier = (int)Math.pow( 10, iOrder);
		int iBase = miMaxClassSize;
		int iScale1 = iBase + iMultiplier;
		miVerticalTick_MajorInterval = iMultiplier;
		if( iOrder > 0 ) miVerticalTick_MinorInterval = (int)Math.pow( 10, (iOrder - 1));
		miVerticalTick_MediumInterval = miVerticalTick_MinorInterval * 5;
		if( iScale1 > 10*(iScale1 - miMaxClassSize) ){
			miVerticalScale = iScale1; // scale1 works
		} else {
			if( iOrder == 0 ){
				miVerticalScale = 10; // absolute minimum
			} else {
				for( int iScale2 = iScale1; iScale2 > iBase;  iScale2 -= miVerticalTick_MinorInterval ){
					if( iScale2 > 10*(iScale2 - miMaxClassSize) ){
						miVerticalScale = iScale2;
						break;
					}
				}
			}
		}
	}
	/** the following policy is applied; it affects the way horizontal value labels
	 *  are rounded:
	 *    n is an integer 0 decimal places
	 *    n >= 100  1 decimal place
	 *    n > 0     2 decimal places
	 *    n < 0     3 decimal places
	 */
	int iDetermineDisplayValuePrecision( double d ){
		d = Math.abs(d);
		int iOrder = (int)(Math.log(d) / Math.log(10)) + 1;
		if( d == (int)d ) return iOrder;
		if( d >= 100 ) return iOrder + 1;
		if( d >= 0 ) return iOrder + 2;
		return 3;
	}

	private String msLabel_HorizontalAxis = null;
	private String msLabel_Title = null;
	void setLabel_HorizontalAxis( String sText ){ msLabel_HorizontalAxis = sText; }
	void setLabel_Title( String sText ){ msLabel_Title = sText; }

	public boolean render( int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){

		int iRGBA = Color.black.getRGB();

		if( miClassCount == 0 ){
			sbError.append("Error generating histogram, class count was 0. Data set may be empty.");
			return false; // nothing to graph
		}

//		g2.setFont(Styles.fontSansSerif10);
//		FontMetrics mfontmetricsSansSerif10 = g2.getFontMetrics(Styles.fontSansSerif10);

		// draw y-axis (vertical)
		int pxVerticalAxisHeight = pxPlotWidth;
		/*  TODO
		if( pxVerticalAxisHeight < 10 ){ // abort - canvas too small
			sbError.append( "Error generating histogram, canvas is too small." );
			return false;
		}
		g2.drawRect( mpxMargin_Left, mpxMargin_Top, mpxAxisThickness, pxVerticalAxisHeight );
		*/

		// draw vertical ticks and labels
		/* TODO should be handled by plot interface not here
		if( mpxMargin_Left > mpxTickMajorLength + 2 ){ // otherwise there is no room for ticks
			int iMaxLabelWidth = mfontmetricsSansSerif10.stringWidth(Integer.toString(this.miVerticalScale)) + mpxVerticalTick_LabelOffset;
			int iLabelHalfHeight = mfontmetricsSansSerif10.getAscent() / 2;
			boolean zShowLabels = (iMaxLabelWidth + mpxTickMajorLength < mpxMargin_Left);
			int pxTickMajor_Left = mpxMargin_Left - mpxTickMajorLength;
			int pxTickMedium_Left = mpxMargin_Left - mpxTickMediumLength;
			int pxTickMinor_Left = mpxMargin_Left - mpxTickMinorLength;
			int iNextMajorTick = 0;
			int iNextMinorTick = 0;
			int iNextMediumTick = 0;
			int pxTickTop = 0;
			while(true){
				iNextMediumTick = iNextMajorTick + miVerticalTick_MediumInterval;
				iNextMajorTick += miVerticalTick_MajorInterval;
				while(true){
					iNextMinorTick += miVerticalTick_MinorInterval;
					if( iNextMinorTick > miVerticalScale ) break;
					if( iNextMinorTick == iNextMajorTick ) break;
					int pxTickHeight = Math.round((float)pxVerticalAxisHeight * (float)iNextMinorTick / (float)miVerticalScale);
					pxTickTop = mpxMargin_Top + (pxVerticalAxisHeight - pxTickHeight);
					if( iNextMinorTick == iNextMediumTick ){
						g2.drawLine(pxTickMedium_Left, pxTickTop, mpxMargin_Left, pxTickTop);
					} else {
						g2.drawLine(pxTickMinor_Left, pxTickTop, mpxMargin_Left, pxTickTop);
					}
					if( iNextMinorTick > iNextMajorTick || iNextMinorTick == 0 ) break;
				}
				if( iNextMajorTick > miVerticalScale || iNextMajorTick == 0 ) break;
				pxTickTop = mpxMargin_Top + Math.round((float)pxVerticalAxisHeight*(1 - (float)iNextMajorTick/(float)miVerticalScale));
				g2.drawLine(pxTickMajor_Left, pxTickTop, mpxMargin_Left, pxTickTop);
				if( zShowLabels ){
					String sLabel = Integer.toString(iNextMajorTick);
					int pxLabelLeft = mpxMargin_Left - mpxTickMajorLength - mfontmetricsSansSerif10.stringWidth(sLabel) - mpxVerticalTick_LabelOffset;
					int pxLabelTop  = pxTickTop + iLabelHalfHeight;
					g2.drawString(sLabel, pxLabelLeft, pxLabelTop);
				}
			}
		}
		*/

/* should be handled by plot interface TODO		
		// draw x-axis (horizontal)
		int pxHorizontalAxisWidth = pxCanvasWidth - mpxMargin_Left - mpxMargin_Right;
		if( pxHorizontalAxisWidth < 10 ){
			sbError.append( "Error generating histogram, horizontal width of canvas is too small (" + pxHorizontalAxisWidth + ")." );
			return false; // nothing to graph
		}
		g2.drawRect(mpxMargin_Left, mpxMargin_Top + pxVerticalAxisHeight - mpxAxisThickness, pxHorizontalAxisWidth, mpxAxisThickness);
		// draw horizontal ticks and labels
		// policy is:
		// (a) if you have room to mark every box with the average of the range
		// (b) if there is not enough room than mark from edge to edge with even spaced ticks in between
//System.out.println("horiz axis width: " + pxHorizontalAxisWidth + " right location: " + (mpxMargin_Left+pxHorizontalAxisWidth));
		if( mzBoxed ){

			// draw box right
			g2.drawRect(mpxMargin_Left + pxHorizontalAxisWidth - mpxAxisThickness, mpxMargin_Top, mpxAxisThickness, pxVerticalAxisHeight);

			// draw box top
			g2.drawRect(mpxMargin_Left, mpxMargin_Top, pxHorizontalAxisWidth, mpxAxisThickness);
		}
*/

		// draw class rectangles
		int pxGraphWidth = pxPlotWidth - mpxGraphOffset*2;
		int pxRectWidth = (int)(pxGraphWidth / miClassCount);
		if( pxRectWidth < 1 ) pxRectWidth = 1;
		int offBarLeft = mpxMargin_Left + mpxAxisThickness + mpxGraphOffset;
		for( int xBar = 1; xBar <= miClassCount; xBar++ ){
			if( maiClassSize1[xBar] > 0 ){ // draw class bar
				int pxRectHeight = Math.round((float)maiClassSize1[xBar] * (float)pxVerticalAxisHeight / (float)miVerticalScale);
				int offRectTop = mpxMargin_Top + pxVerticalAxisHeight - pxRectHeight;
				if( pxRectWidth == 1 )
				    Utility_Geometry.drawLineToRaster( raster, pxPlotWidth, pxPlotHeight, offBarLeft, offRectTop, offBarLeft, offRectTop + pxRectHeight - 1, iRGBA );
				else
				    Utility_Geometry.drawRectangle( raster, pxPlotWidth, pxPlotHeight, offBarLeft, offRectTop, offBarLeft + pxRectWidth - 1, offRectTop + pxRectHeight - 1, iRGBA );

				// store these coordinates for mouse events
				mxRectTopLeft[xBar] = offBarLeft;
				myRectTopLeft[xBar] = offRectTop;
				mxRectBottomLeft[xBar] = offBarLeft + pxRectWidth - 1;
				myRectBottomLeft[xBar] = offRectTop + pxRectHeight - 1;
			}
			offBarLeft += pxRectWidth;
		}

		// draw horizontal ticks and labels (follows pattern of graph drawing)
		/* TODO should be done with new axis capability
		int iLabelHeight_SanSerif10 = mfontmetricsSansSerif10.getAscent();
		if( mpxMargin_Bottom > mpxTickMajorLength + iLabelHeight_SanSerif10 + 4 ){ // otherwise there is no room for ticks

			// draw left tick
			int pxHTick_Left = mpxMargin_Left + mpxAxisThickness + mpxGraphOffset;
			int pxHTick_Top  = mpxMargin_Top + pxVerticalAxisHeight;
			g2.drawLine(pxHTick_Left, pxHTick_Top, pxHTick_Left, pxHTick_Top + mpxTickMajorLength);
			double dBegin = getValue_double(0);
			int iDisplayPrecision = iDetermineDisplayValuePrecision(dBegin);
			String sHLabel = Utility_String.sDoubleToPrecisionString(dBegin, iDisplayPrecision);
			int pxHLabelTop  = pxHTick_Top + mpxTickMajorLength + mpxHorizontalTick_LabelOffset + iLabelHeight_SanSerif10;
			g2.drawString(sHLabel, pxHTick_Left, pxHLabelTop);

			// draw right tick
			pxHTick_Left += miClassCount * pxRectWidth;
			g2.drawLine(pxHTick_Left, pxHTick_Top, pxHTick_Left, pxHTick_Top + mpxTickMajorLength);
			double dEnd = getValue_double(miDataElementCount - 1);
			iDisplayPrecision = iDetermineDisplayValuePrecision(dEnd);
			sHLabel = Utility_String.sDoubleToPrecisionString(dEnd, iDisplayPrecision);
			int pxHLabelLeft = pxHTick_Left - mfontmetricsSansSerif10.stringWidth(sHLabel);
			g2.drawString(sHLabel, pxHLabelLeft, pxHLabelTop);
		}

		// draw external labels
		mText.remove(TEXT_ID_CaptionColorBar); // there may be a pre-existing, automatically generated caption
		vDrawText(g2, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight);
		*/
		
		return true;
	}

	double getValue_double(int index){
		switch( meTYPE ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16: return (double)mashSortedData[index];
			case DAP.DATA_TYPE_Int32:
			case DAP.DATA_TYPE_UInt16: return (double)maiSortedData[index];
			case DAP.DATA_TYPE_UInt32: return (double)manSortedData[index];
			case DAP.DATA_TYPE_Float32: return (double)mafSortedData[index];
			case DAP.DATA_TYPE_Float64: return madSortedData[index];
			case DAP.DATA_TYPE_String: // todo
				if( index == 0 ) return 0;
				else return 26;
			default: return 0d;
		}
	}

	void vShowClassInfo( int iClassNumber ){
		StringBuffer sbInfo = new StringBuffer(80);
		sbInfo.append("Class ").append(iClassNumber).append(" with ").append(maiClassSize1[iClassNumber]).append(" members:");
		sbInfo.append("\n  begins: ").append(getValueString(maiClassBegin1[iClassNumber]));
		sbInfo.append("\n  ends: ").append(getValueString(maiClassEnd1[iClassNumber]));
		String[] as = new String[11];
		int ctUnique = getUnique( iClassNumber, as );
		sbInfo.append("\nContains ").append(ctUnique).append(" unique values (");
		if( ctUnique > 10 ) ctUnique = 10;
		for( int xUnique = 1; xUnique <= ctUnique; xUnique++ ){
			sbInfo.append(as[xUnique]);
			if( xUnique < ctUnique ) sbInfo.append(", ");
		}
		sbInfo.append(")");
		Component compParent = ApplicationController.getInstance().getAppFrame();
		JOptionPane.showMessageDialog(compParent, sbInfo.toString(), "Class Info " + iClassNumber, JOptionPane.INFORMATION_MESSAGE);
	}

	void vSendReport(){
		try {
			int iReportLines = miClassCount > MAX_REPORT_LINES ? MAX_REPORT_LINES : miClassCount;
			OutputStream os = ApplicationController.getInstance().getAppFrame().getTextViewerOS();
			if( os == null ) return;
			String sTitle = " Histogram Report with " + iReportLines + " classes\n";
			String sRule  = Utility_String.sRepeatChar('-',140) + "\n";
			os.write(sRule.getBytes());
			os.write(sTitle.getBytes());
			StringBuffer sbInfo = new StringBuffer(160);
			sbInfo.setLength(0);
			sbInfo.append("   Class");
			sbInfo.append("  ");
			sbInfo.append("    Size");
			sbInfo.append("  ");
			sbInfo.append("Begin       ");
			sbInfo.append("  ");
			sbInfo.append("End         ");
			sbInfo.append("  ");
			sbInfo.append("  Unique");
			sbInfo.append("  ");
			sbInfo.append("Values...\n");
			os.write(sbInfo.toString().getBytes());
			os.write(sRule.getBytes());
			String[] as = new String[11];
			int iTotal_ClassSize = 0;
			int iTotal_Unique    = 0;
			for( int xClass = 1; xClass <= iReportLines; xClass++ ){
				sbInfo.setLength(0);
				sbInfo.append("  ");
				sbInfo.append(Utility_String.sFormatFixedRight(xClass, 6, ' '));
				sbInfo.append("  ");
				sbInfo.append(Utility_String.sFormatFixedRight(maiClassSize1[xClass], 8, ' '));
				if( maiClassSize1[xClass] == 0 ) continue; // none of the remaining info is relevant
				sbInfo.append("  ");
				sbInfo.append(Utility_String.sFixedWidth(getValueString(maiClassBegin1[xClass]), 12, ' '));
				sbInfo.append("  ");
				sbInfo.append(Utility_String.sFixedWidth(getValueString(maiClassEnd1[xClass]), 12, ' '));
				int ctUnique = getUnique( xClass, as );
				sbInfo.append("  ");
				sbInfo.append(Utility_String.sFormatFixedRight(ctUnique, 8, ' '));
				sbInfo.append("  ");
				int ctValues = ctUnique > 10 ? 10 : ctUnique;
				for( int xUnique = 1; xUnique <= ctValues; xUnique++ ){
					sbInfo.append(as[xUnique]);
					if( xUnique < ctUnique ) sbInfo.append(", ");
				}
				if( ctUnique > ctValues ) sbInfo.append("...");
				sbInfo.append('\n');
				os.write(sbInfo.toString().getBytes());

				iTotal_ClassSize += maiClassSize1[xClass];
				iTotal_Unique += ctUnique;
			}
			os.write(sRule.getBytes());

			// used/total line
			sbInfo.setLength(0);
			if( mctMissing_Histogram == 0 ){
				sbInfo.append("   Total: ");
			} else {
				sbInfo.append("    Used: ");
			}
			sbInfo.append(Utility_String.sFormatFixedRight(iTotal_ClassSize, 8, ' '));
			sbInfo.append(Utility_String.sRepeatChar(' ', 30));
			sbInfo.append(Utility_String.sFormatFixedRight(iTotal_Unique, 8, ' '));
			sbInfo.append('\n');
			os.write(sbInfo.toString().getBytes());
			if( mctMissing_Histogram > 0 ){

				// missing line
				sbInfo.setLength(0);
				sbInfo.append(" Missing: ");
				sbInfo.append(Utility_String.sFormatFixedRight(mctMissing_Histogram_Total, 8, ' '));
				sbInfo.append(Utility_String.sRepeatChar(' ', 30));
				sbInfo.append(Utility_String.sFormatFixedRight(mctMissing_Histogram, 8, ' '));
				sbInfo.append("  ");
				int ctMissing = mctMissing_Histogram > 10 ? 10 : mctMissing_Histogram;
				as = getMissingAll();
				for( int xMissing = 1; xMissing <= ctMissing; xMissing++ ){
					sbInfo.append(as[xMissing]);
					if( xMissing < ctMissing ) sbInfo.append(", ");
				}
				if( mctMissing_Histogram > ctMissing ) sbInfo.append("...");
				sbInfo.append('\n');
				os.write(sbInfo.toString().getBytes());

				// total line
				sbInfo.setLength(0);
				sbInfo.append("   Total: ");
				sbInfo.append(Utility_String.sFormatFixedRight((iTotal_ClassSize + mctMissing_Histogram_Total), 8, ' '));
				sbInfo.append(Utility_String.sRepeatChar(' ', 30));
				sbInfo.append(Utility_String.sFormatFixedRight(iTotal_Unique + mctMissing_Histogram, 8, ' '));
				sbInfo.append('\n');
				os.write(sbInfo.toString().getBytes());
			}
			os.write(sRule.getBytes());
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, "while sending histogram report");
		}
	}

	// returns an array of up to 10 strings + the count as the return value
	int getUnique( int iClassNumber, String[] as ){
		int ctUnique = 1;
		switch( meTYPE ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				{short last = mashSortedData[maiClassBegin1[iClassNumber]];
				as[1] = Short.toString(last);
				for( int xData = maiClassBegin1[iClassNumber] + 1; xData <= maiClassEnd1[iClassNumber]; xData++ ){
					if( mashSortedData[xData] == last ) continue;
					ctUnique++;
					last = mashSortedData[xData];
					if( ctUnique <= 10 ) as[ctUnique] = Short.toString(last);
				}
				return ctUnique;}
			case DAP.DATA_TYPE_Int32:
			case DAP.DATA_TYPE_UInt16:
				{int last = maiSortedData[maiClassBegin1[iClassNumber]];
				as[1] = Integer.toString(last);
				for( int xData = maiClassBegin1[iClassNumber] + 1; xData <= maiClassEnd1[iClassNumber]; xData++ ){
					if( maiSortedData[xData] == last ) continue;
					ctUnique++;
					last = maiSortedData[xData];
					if( ctUnique <= 10 ) as[ctUnique] = Integer.toString(last);
				}
				return ctUnique;}
			case DAP.DATA_TYPE_UInt32:
				{long last = manSortedData[maiClassBegin1[iClassNumber]];
				as[1] = Long.toString(last);
				for( int xData = maiClassBegin1[iClassNumber] + 1; xData <= maiClassEnd1[iClassNumber]; xData++ ){
					if( manSortedData[xData] == last ) continue;
					ctUnique++;
					last = manSortedData[xData];
					if( ctUnique <= 10 ) as[ctUnique] = Long.toString(last);
				}
				return ctUnique;}
			case DAP.DATA_TYPE_Float32:
				{float last = mafSortedData[maiClassBegin1[iClassNumber]];
				as[1] = Float.toString(last);
				for( int xData = maiClassBegin1[iClassNumber] + 1; xData <= maiClassEnd1[iClassNumber]; xData++ ){
					if( mafSortedData[xData] == last ) continue;
					ctUnique++;
					last = mafSortedData[xData];
					if( ctUnique <= 10 ) as[ctUnique] = Float.toString(last);
				}
				return ctUnique;}
			case DAP.DATA_TYPE_Float64:
				{double last = madSortedData[maiClassBegin1[iClassNumber]];
				as[1] = Double.toString(last);
				for( int xData = maiClassBegin1[iClassNumber] + 1; xData <= maiClassEnd1[iClassNumber]; xData++ ){
					if( madSortedData[xData] == last ) continue;
					ctUnique++;
					last = madSortedData[xData];
					if( ctUnique <= 10 ) as[ctUnique] = Double.toString(last);
				}
				return ctUnique;}
			case DAP.DATA_TYPE_String:
				{String last = masSortedData[maiClassBegin1[iClassNumber]];
				as[1] = last;
				for( int xData = maiClassBegin1[iClassNumber] + 1; xData <= maiClassEnd1[iClassNumber]; xData++ ){
					if( masSortedData[xData] != null && masSortedData[xData].equals(last) ) continue;
					ctUnique++;
					last = masSortedData[xData];
					if( ctUnique <= 10 ) as[ctUnique] = last;
				}
				return ctUnique;}
		}
		return 0;
	}

	String getValueString( int xData ){
		switch( meTYPE ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				return Short.toString( mashSortedData[xData] );
			case DAP.DATA_TYPE_Int32:
			case DAP.DATA_TYPE_UInt16:
				return Integer.toString( maiSortedData[xData] );
			case DAP.DATA_TYPE_UInt32:
				return Long.toString( manSortedData[xData] );
			case DAP.DATA_TYPE_Float32:
				return Float.toString( mafSortedData[xData] );
			case DAP.DATA_TYPE_Float64:
				return Double.toString( madSortedData[xData] );
			case DAP.DATA_TYPE_String:
				return masSortedData[xData];
			default: return "?";
		}
	}

	String[] getMissingAll(){
		String[] as = new String[mctMissing_Histogram + 1];
		for( int xMissing = 1; xMissing <= mctMissing_Histogram; xMissing++ ) as[xMissing] = getMissingValue(xMissing);
		return as;
	}

	String getMissingValue( int xMissing ){
		switch( meTYPE ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				return Short.toString( mashMissing[xMissing] );
			case DAP.DATA_TYPE_Int32:
			case DAP.DATA_TYPE_UInt16:
				return Integer.toString( maiMissing[xMissing] );
			case DAP.DATA_TYPE_UInt32:
				return Long.toString( manMissing[xMissing] );
			case DAP.DATA_TYPE_Float32:
				return Float.toString( mafMissing[xMissing] );
			case DAP.DATA_TYPE_Float64:
				return Double.toString( madMissing[xMissing] );
			case DAP.DATA_TYPE_String:
				return masMissing[xMissing];
			default: return "?";
		}
	}
	
	public void handleMouseClicked( MouseEvent evt ){
		int xPX = evt.getX();
		int yPX = evt.getY();
		for( int xClass = 1; xClass <= miClassCount; xClass++ ){
			if( xPX >= mxRectTopLeft[xClass] &&
				xPX <= mxRectBottomLeft[xClass] &&
				yPX >= myRectTopLeft[xClass] &&
			    yPX <= myRectBottomLeft[xClass] ){
					vShowClassInfo( xClass );
					return;
			}
		}
		Component compParent = ApplicationController.getInstance().getAppFrame();
		if( JOptionPane.showConfirmDialog(compParent,"Do you want to send a report to the text view?","Histogram Report", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION ){
			vSendReport();
		}
	}

}



