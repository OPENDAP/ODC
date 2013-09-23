package opendap.clients.odc.plot;

/**
 * Title:        Panel_Plot_Line
 * Description:  Plots lines
 * Copyright:    Copyright (c) 2002-12
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.09
 */

import opendap.clients.odc.Utility;
import opendap.clients.odc.DAP;
import opendap.clients.odc.Utility_Geometry;

import java.util.ArrayList;
import java.awt.*;

class Plot_Line extends Plot {

	public final static int TYPE_Line = 1;
	public final static int TYPE_Scatter = 2;
	public final static int TYPE_HiLo = 3;

	private IPlottable data = null; // needed for data microscope
	private ColorSpecification cs = null;
	private PlotOptions po = null;
	
	private Plot_Line( PlotEnvironment environment, PlotLayout layout ){
		super( environment, layout );		
	}

	public String getDescriptor(){ return "L"; }

	public boolean draw( StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}

	// series
	private int mctLines = 0;
	private int[][][] mapxX1 = null;
	private int[][][] mapxY1 = null;
	private int[][][] mapxHi = null; // only used for hi-lo plots
	private int[][][] mapxLo = null; // only used for hi-lo plots
	private Color[] maColors1;
	private String[] masCaptions1;

	// axes
	private PlotAxis maxisY = null;
	private PlotAxis maxisX = null;
	private Object[] meggMappingY = null; // contains one or more eggs containing linear data series
	private int meDataType_MappingY = 0;
	private Object[] meggMappingX = null;
	private int meDataType_MappingX = 0;

	public Color[] getColors(){ return maColors1; }
	public String[] getCaptions(){ return masCaptions1; }

	private int miCircleSize = 5;
	private boolean	mezShowLines = true;
	private boolean	mezShowPoints = false;

	private int mpxMargin_Top = 10; // TODO
	private int mpxMargin_Left = 10; // TODO
	private int mpxGraphOffset = 50; // TODO
	private int mpxAxisOffsetHeight = 1; // TODO
	private int mpxAxisOffsetWidth = 1; // TODO
	
	public static Plot_Line create( PlotEnvironment environment, PlotLayout layout, IPlottable data, String sCaption, StringBuffer sbError ){
		Plot_Line plot = new Plot_Line( environment, layout );
		plot.data = data;
		plot.cs = environment.getColorSpecification();
		plot.po = environment.getOptions();
		plot.msCaption = sCaption;
		return plot;
	}

	// there can be either one mapping egg in which case all data is mapped to it, or there can be one mapping
	// for each series; or it can be null (and the x-values will be 1, 2, 3, 4 etc)
	/** currently the series eggs must contain arrays of doubles */
	boolean setLineData( PlotScale scale, ArrayList list_eggSeriesY, ArrayList list_eggSeriesX, ArrayList listCaptions_X, ArrayList listCaptions_Y, int eDataType_Lines, String sCaptionY, String sCaptionX, StringBuffer sbError ){
		if( po == null ){
			miCircleSize = 5;
			mezShowLines = true;
			mezShowPoints = false;
		} else {
			miCircleSize = po.getValue_int(PlotOptions.OPTION_XY_CircleSize);
			mezShowLines = po.getValue_boolean(PlotOptions.OPTION_XY_ShowLines);
			mezShowPoints = po.getValue_boolean(PlotOptions.OPTION_XY_ShowPoints);
		}

		if( list_eggSeriesY == null && list_eggSeriesX == null ){
			sbError.append("no data supplied");
			return false;
		}
		int ctSeriesY = list_eggSeriesY  == null ? 0 : list_eggSeriesY.size();
		int ctSeriesX = list_eggSeriesX  == null ? 0 : list_eggSeriesX.size();
		int ctSeries  = ctSeriesY > ctSeriesX ? ctSeriesY : ctSeriesX;
		if( ctSeries == 0 ){
			sbError.append("data series array was empty, there must be at least one vector for xy plots");
			return false;
		}
		mctLines = ctSeries;

		// validate that number of series matches
		if( ctSeriesY > 1 && ctSeriesX > 1 && ctSeriesY != ctSeriesX ){
			sbError.append("number of Y-series (" + ctSeriesY + ") does not match number of X-series (" + ctSeriesX + ")");
			return false;
		}

		// validate that vectors all have the same size
		// todo this may be relaxed in the future if we allow lines of different sizes
		int iSeriesLength = 0;
		String sComparisonSeries = null;
		for( int xSeriesY = 0; xSeriesY < ctSeriesY; xSeriesY++ ){
			int lenCurrentSeries = ((double[])list_eggSeriesY.get(xSeriesY)).length;
			if( iSeriesLength == 0 ){
				iSeriesLength = lenCurrentSeries;
				sComparisonSeries = "Y " + xSeriesY + " (" + iSeriesLength + ")";
			}
			if( lenCurrentSeries != iSeriesLength ){
				sbError.append("size of Y-series " + xSeriesY + " (" + lenCurrentSeries + ") does not match size of " + sComparisonSeries);
				return false;
			}
		}
		for( int xSeriesX = 0; xSeriesX < ctSeriesX; xSeriesX++ ){
			int lenCurrentSeries = ((double[])list_eggSeriesX.get(xSeriesX)).length;
			if( iSeriesLength == 0 ){
				iSeriesLength = lenCurrentSeries;
				sComparisonSeries = "X " + xSeriesX + " (" + iSeriesLength + ")";
			}
			if( lenCurrentSeries != iSeriesLength ){
				sbError.append("size of X-series " + xSeriesX + " (" + lenCurrentSeries + ") does not match size of " + sComparisonSeries);
				return false;
			}
		}

		// store captions - captions are used for the legend labels
		masCaptions1 = new String[mctLines +1];
		if( ctSeriesX > 0 && ctSeriesY > 0 ){
			for (int xCaption = 1; xCaption <= mctLines; xCaption++) {
				if( listCaptions_X.get(xCaption - 1) != null || listCaptions_Y.get(xCaption - 1) != null ){
					masCaptions1[xCaption] = (String) listCaptions_X.get(xCaption - 1) + " vs " + (String)listCaptions_Y.get(xCaption - 1);
				}
			}
		} else if( ctSeriesX > 0 ){
			for (int xCaption = 1; xCaption <= mctLines; xCaption++) {
				masCaptions1[xCaption] = (String) listCaptions_X.get(xCaption - 1);
			}
		} else {
			for (int xCaption = 1; xCaption <= mctLines; xCaption++) {
				masCaptions1[xCaption] = (String) listCaptions_Y.get(xCaption - 1);
			}
		}

		// build default mappings if necessary
		if( ctSeriesY == 0 ){
			if( !Utility.zMemoryCheck(iSeriesLength, 8, sbError) ) return false;
			double[] adY = new double[iSeriesLength];
			for( int xY = 0; xY < iSeriesLength; xY++ ) adY[xY] = (double)(xY + 1);
			list_eggSeriesY.add( adY );
			ctSeriesY = 1;
		} else if( ctSeriesX == 0 ){
			if( !Utility.zMemoryCheck(iSeriesLength, 8, sbError) ) return false;
			double[] adX = new double[iSeriesLength];
			for( int xX = 0; xX < iSeriesLength; xX++ ) adX[xX] = (double)(xX + 1);
			list_eggSeriesX.add( adX );
			ctSeriesX = 1;
		}

		// determine Y (dependent) sizing
		mapxX1 = new int[mctLines + 1][][];
		mapxY1 = new int[mctLines + 1][][];
		double dMin_y = Double.MAX_VALUE;
		double dMax_y = Double.MAX_VALUE * -1;
		int iMaxLenY = 0;
		int ctTotalValues_Y = 0;
		int ctTotalMissing_Y = 0;
		for( int xSeries = 0; xSeries < ctSeriesY; xSeries++ ){
			switch(eDataType_Lines){
				case DAP.DATA_TYPE_Float64:
					double[] adY = (double[])list_eggSeriesY.get(xSeries);
					int lenY = adY.length;
					if( lenY == 0 ){
						sbError.append("series " + xSeries + " Y has no entries");
						return false;
					}
					if( lenY > iMaxLenY ) iMaxLenY = lenY;
					for( int xData = 0; xData < lenY; xData++ ){
						double dValue = adY[xData];
						if( Double.isNaN(dValue) ){
							ctTotalMissing_Y++;
							continue;
						}
						ctTotalValues_Y++;
						if( dValue < dMin_y ) dMin_y = dValue;
						if( dValue > dMax_y ) dMax_y = dValue;
					}
					break;
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Float32:
				case DAP.DATA_TYPE_Int16:
				case DAP.DATA_TYPE_UInt16:
				case DAP.DATA_TYPE_Int32:
				case DAP.DATA_TYPE_UInt32:
				default:
					sbError.append("data type " + eDataType_Lines + " not supported");
					return false;
			}
		}
		if( ctTotalValues_Y == 0 ){
			sbError.append("y vector has no values (all " + ctTotalMissing_Y + " are missing)");
			return false;
		}
		double dRange_y = dMax_y - dMin_y;

		// determine X (independent) sizing
		double dMin_x = Double.MAX_VALUE;
		double dMax_x = Double.MAX_VALUE * -1;
		int iMaxLenX = 0;
		int ctTotalValues_X = 0;
		int ctTotalMissing_X = 0;
		for( int xSeries = 0; xSeries < ctSeriesX; xSeries++ ){
			switch(eDataType_Lines){
				case DAP.DATA_TYPE_Float64:
					double[] adX = (double[])list_eggSeriesX.get(xSeries);
					int lenX = adX.length;
					if( lenX == 0 ){
						sbError.append("series " + xSeries + " X has no entries");
						return false;
					}
					if( lenX > iMaxLenX ) iMaxLenX = lenX;
					for( int xData = 0; xData < lenX; xData++ ){
						double dValue = adX[xData];
						if( Double.isNaN(dValue) ){
							ctTotalMissing_X++;
							continue;
						}
						ctTotalValues_X++;
						if( dValue < dMin_x ) dMin_x = dValue;
						if( dValue > dMax_x ) dMax_x = dValue;
					}
					break;
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Float32:
				case DAP.DATA_TYPE_Int16:
				case DAP.DATA_TYPE_UInt16:
				case DAP.DATA_TYPE_Int32:
				case DAP.DATA_TYPE_UInt32:
				default:
					sbError.append("data type " + eDataType_Lines + " not supported");
					return false;
			}
		}

		// validate x-range
		if( ctTotalValues_X == 0 ){
			sbError.append("x vector has no values (all " + ctTotalMissing_X + " are missing)");
			return false;
		}
		double dRange_x = dMax_x - dMin_x;

// System.out.println(" dMin_x: " + dMin_x + " dMax_x: " + dMax_x + " dMin_y: " + dMin_y + " dMax_y: " + dMax_y);

		// setup y-axis
		maxisY = PlotAxis.createLinear_Y( sCaptionY, dMin_y, dMax_y );

		// setup x-axis and x-mapping
		maxisX = PlotAxis.createLinear_X( sCaptionX, dMin_x, dMax_x );

		// the data point width/height is purely conventional because the points may be
		// overlapping etc; in any case the practice here is to default to a square graph
		// TODO determine dimensions if( !zUpdateDimensions( iSeriesLength, iSeriesLength, sbError ) ) return false;

		// the axes can only be set after the dimensions are established
//		setAxisVertical(maxisY);
//		setAxisHorizontal(maxisX);

		// the plot dimensions are only known after they have been set above
		int pxPlotWidth = scale.getPlot_Width_pixels();
		int pxPlotHeight = scale.getPlot_Height_pixels();

		// make lines
		double dX_lower = maxisX.getValueFrom();
		double dX_upper = maxisX.getValueTo();
		double dY_lower = maxisY.getValueFrom();
		double dY_upper = maxisY.getValueTo();
		double dX_range = dX_upper - dX_lower; // can be negative
		double dY_range = dY_upper - dY_lower; // can be negative
		if( dX_range == 0 ){
			sbError.append("X range is 0");
			return false;
		}
		if( dY_range == 0 ){
			sbError.append("Y range is 0");
			return false;
		}
		int pxStartX = mpxMargin_Left + mpxGraphOffset;
		int pxStartY = mpxMargin_Top;
		for( int xLines = 0; xLines < mctLines; xLines++ ){

			int xX = ctSeriesX == 1 ? 0 : xLines;
			int xY = ctSeriesY == 1 ? 0 : xLines;

			double[] adX, adY;
			adX = (double[])list_eggSeriesX.get(xX);
			adY = (double[])list_eggSeriesY.get(xY);
			int lenData = iSeriesLength;

			// count line segments and allocate memory for them
			int ctSegments = 0;
			boolean zInLine = false;
			for( int xData = 0; xData < lenData; xData++ ){
				double dValueX = adX[xData];
				double dValueY = adY[xData];
				if( Double.isNaN( dValueX ) || Double.isNaN( dValueY )){
					if( zInLine ) zInLine = false;
				} else {
					if( !zInLine ){
						ctSegments++;
						zInLine = true;
					}
				}
			}
			mapxX1[xLines + 1] = new int[ctSegments + 1][];
			mapxY1[xLines + 1] = new int[ctSegments + 1][];

			// count points in each segment and allocate memory for them
			int xSegment = 0;
			zInLine = false;
			int ctPoints = 0;
			int xData = -1;
			while( true ){
				xData++;
				if( xData >= lenData ){
					if( ctPoints > 0 ){
						if( !Utility.zMemoryCheck(xLines * xSegment * ctPoints * 2, 4, sbError) ) return false;
						mapxX1[xLines + 1][xSegment] = new int[ctPoints];  // point array is passed to library routine so is zero-based
						mapxY1[xLines + 1][xSegment] = new int[ctPoints];
					}
					break;
				}
				double dValueX = adX[xData];
				double dValueY = adY[xData];
				if( Double.isNaN( dValueX ) || Double.isNaN( dValueY ) ){
					if( zInLine ){
						if( ctPoints > 0 ){
							if( !Utility.zMemoryCheck(xLines * xSegment * ctPoints * 2, 4, sbError) ) return false;
							mapxX1[xLines + 1][xSegment] = new int[ctPoints];
							mapxY1[xLines + 1][xSegment] = new int[ctPoints];
						}
						zInLine = false;
						ctPoints = 0;
					}
				} else {
					if( !zInLine ){
						xSegment++;
						zInLine = true;
					}
					ctPoints++;
				}
			}

			// populate arrays
			xSegment = 0;
			zInLine = false;
			int xPoint = -1; // the pointpart of the array is zero-based
			for( xData = 0; xData < lenData; xData++ ){
				double dValueX = adX[xData];
				double dValueY = adY[xData];
// System.out.println(" series " + (xLines + 1) + " seg " + xSegment + " data " + xData + ": (" + adX[xData] + ", " + adY[xData] + " )");
				if( Double.isNaN( dValueY ) || Double.isNaN( dValueX ) ){
					if( zInLine ){
						zInLine = false;
						xPoint = -1;
					}
				} else {
					if( !zInLine ){
						xSegment++;
						zInLine = true;
					}
					xPoint++;
					int pxValueWidth = (int)((double)pxPlotWidth * (dValueX - dX_lower) / dX_range + mpxAxisOffsetWidth);
	    			int pxValueHeight = (int)((double)pxPlotHeight * (dValueY - dY_lower) / dY_range + mpxAxisOffsetHeight);
// System.out.println(" series " + (xLines + 1) + " seg " + xSegment + " pt " + xPoint + " xStart: " + pxStartX + " valw: " + pxValueWidth + " xval " + dValueX + " xlower: " + dX_lower + " y start: " + pxStartY + " pxPlotHeight: " + pxPlotHeight + " value h: " + pxValueHeight + " )");
		    		mapxX1[xLines + 1][xSegment][xPoint] = pxStartX + pxValueWidth;
			    	mapxY1[xLines + 1][xSegment][xPoint] = pxStartY + pxPlotHeight - pxValueHeight;
				}
			}
		}
		if( mctLines == 1 ){
			maColors1 = new Color[2];
			maColors1[1] = Color.BLACK;
		} else {
			maColors1 = ColorSpecification.getColorBands1_Color( mctLines );
		}
		return true;
	}

	float[] mafU = null; // generates max as a side effect
	float[] mafV = null;
	float mUmax;
	float mVmax; // will be * PX_MAX_VECTOR squared
	public boolean render( int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){

		int pxCircleOffset = miCircleSize / 2;

		// draw plot
		for( int xLine = 1; xLine <= mctLines; xLine++ ){
			int iRGBA = maColors1[xLine].getRGB();
			int[][] aiXsegments = mapxX1[xLine];
			int[][] aiYsegments = mapxY1[xLine];
			for( int xSegment = 1; xSegment < aiXsegments.length; xSegment++ ){
				int[] aiXcoordinates = aiXsegments[xSegment];
				int[] aiYcoordinates = aiYsegments[xSegment];
				if( mezShowLines ){
					Utility_Geometry.drawPolylineToRaster( raster, pxPlotWidth, pxPlotHeight, aiXcoordinates, aiYcoordinates, iRGBA );
				}
				/* TODO
				if( mezShowPoints ){
					for( int xData = 0; xData < aiXcoordinates.length; xData++ ){
						g2.fillOval(aiXcoordinates[xData] - pxCircleOffset, aiYcoordinates[xData] - pxCircleOffset, miCircleSize, miCircleSize);
					}
				}
				*/
			}
		}

		// TODO why is this here?
		int pxAxisLength_horizontal = pxPlotWidth + mpxAxisOffsetWidth;
		int pxAxisLength_vertical = pxPlotHeight + mpxAxisOffsetHeight;
		return true;
	}

}


