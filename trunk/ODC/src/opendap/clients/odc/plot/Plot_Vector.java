package opendap.clients.odc.plot;

/**
 * Title:        Panel_Plot_Vector
 * Description:  Plots vector grids
 * Copyright:    Copyright (c) 2003, 2008, 2012
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.08
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_Geometry;
import opendap.clients.odc.DAP;
import opendap.clients.odc.data.Model_Dataset;

import java.awt.*;
import java.awt.image.BufferedImage;

class Plot_Vector extends Plot {

	private Plot_Vector( PlotEnvironment environment, PlotLayout layout ){
		super( environment, layout );		
	}
	public String getDescriptor(){ return "V"; }

	private IPlottable data = null; // needed for data microscope
	private ColorSpecification cs = null;
	private PlotOptions po = null;

	public static Plot_Vector create( PlotEnvironment environment, PlotLayout layout, IPlottable data, String sCaption, StringBuffer sbError ){
		Plot_Vector plot = new Plot_Vector( environment, layout );
		if( data.getDataType() == 0 ){
			sbError.append( "data type undefined" );
			return null; // nothing to plot
		}
		plot.data = data;
		plot.cs = environment.getColorSpecification();
		plot.po = environment.getOptions();
		plot.msCaption = sCaption;
		return plot;
	}	
	
	final static int PX_DEFAULT_VECTOR_SIZE = 10;
	private float[] mafU = null; // generates max as a side effect
	private float[] mafV = null;
	private float mUmax, mVmax; // will be * PX_MAX_VECTOR squared
	private float mfAverageU, mfAverageV, mfAverageMag;

	private int mpxMargin_Top = 10; // TODO
	private int mpxMargin_Left = 10; // TODO
	private int mpxAxisOffsetHeight = 1; // TODO
	private int mpxAxisOffsetWidth = 1; // TODO
	
	StringBuffer msbError = new StringBuffer();

	public boolean draw( StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}

	// When the image is generated only one arrow is drawn per vector region, a region
	// being a square with the size of the nominal maximum arrow. To find the net magnitude for
	// the arrow, all the vectors in the region are averaged.
	public boolean render( int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){

		// determine vector size in pixels
		int iVectorSize;
		if( po == null ){
			iVectorSize = PX_DEFAULT_VECTOR_SIZE;
		} else {
			iVectorSize = po.getValue_int( PlotOptions.OPTION_VectorSize );
			if( iVectorSize <= 0 ) iVectorSize = PX_DEFAULT_VECTOR_SIZE;
		}

		int iDataDim_Width = data.getDimension_x();
		int iDataDim_Height = data.getDimension_y();
		
		// determine overall scale
		float fPlotScaleX = (float)(pxPlotWidth - iVectorSize) / iDataDim_Width; // extra size is added to the top and left to accommodate vectors pointing in that direction
		float fPlotScaleY = (float)(pxPlotHeight - iVectorSize) / iDataDim_Height;

		// determine vector scaling
		int iArrowSize_U = 0;
		int iArrowSize_V = 0;
		iArrowSize_U = iVectorSize;
		iArrowSize_V = iVectorSize;
		if( mafU == null ){
			if( getUV( msbError ) ){
				// generates max as a side effect
			} else {
				// TODO g2.drawString("Error: " + msbError, 20, 20);
			}
		}
		if( mUmax == 0 || mVmax == 0 ){
			sbError.append( "umax and/or vmax is zero" );
			return false;
		}
		iArrowSize_U /= fPlotScaleX; // if the plot is bigger then the sample size should be smaller (and vice versa)
		iArrowSize_V /= fPlotScaleY;
		float fScaleU = mfAverageU * 2f / iArrowSize_U;
		float fScaleV = mfAverageV * 2f / iArrowSize_V;
		// draw plot (u = x  v = y)
		int pxX_Origin = mpxMargin_Left + mpxAxisOffsetWidth + iArrowSize_U;
//		int pxY_Origin = mpxMargin_Top + mpxAxisOffsetHeight + iArrowSize_V;
		int pxY_Origin = mpxMargin_Top + pxPlotHeight - mpxAxisOffsetHeight - iArrowSize_V;
		for( int xX = 0; xX < iDataDim_Width; xX += iArrowSize_U ){
			for( int xY = 0; xY < iDataDim_Height; xY += iArrowSize_V ){
				int sampleX = xX;
				int sampleY = xY;
				float uTotal = 0;
				float vTotal = 0;
				int xXOff = 0;
				int ctRegionVectors = 0;
AbortVector:
				while(true){ // find average of all vectors in region
					if( xXOff == iArrowSize_U || xX + xXOff == iDataDim_Width ){ // done
						if( ctRegionVectors == 0 ){
							// no vectors in this region, draw nothing
						} else {
							float fUavg = uTotal/(float)ctRegionVectors;
							float fVavg = vTotal/(float)ctRegionVectors;
							float fXmag = fUavg/fScaleU;
							float fYmag = fVavg/fScaleV;
							double lenArrow = Math.sqrt((double)(fXmag*fXmag*fPlotScaleX*fPlotScaleX + fYmag*fYmag*fPlotScaleY*fPlotScaleY));
							double dBladeLength = lenArrow / 3d;
							double dBladeHeight = dBladeLength * 4d / 5d;
							int pxX_from = pxX_Origin + Math.round( fPlotScaleX * (float)sampleX );
							int pxY_from = pxY_Origin - Math.round( fPlotScaleY * (float)sampleY );
							int pxX_to   = pxX_Origin + Math.round( fPlotScaleX * ((float)sampleX + fXmag));
							int pxY_to   = pxY_Origin - Math.round( fPlotScaleY * ((float)sampleY + fYmag));
// debugging
//if( (xX % 20 == 0) && (xY % 30 == 0) ){
//	System.out.println("points:: pxStartX: " + pxStartX + " pxStartY: " + pxStartX + " from/to: " + pxX_from + " " + pxY_from + " " + pxX_to + " " + pxY_to + " fPlotScaleX: " + fPlotScaleX + " fPlotScaleY: " + fPlotScaleY);
//}
							
							int iRGBA;
							if( cs == null ){
								iRGBA = 0;
							} else {
								double dMag = Math.sqrt( fUavg * fUavg + fVavg * fVavg );
								iRGBA = cs.getColorForValue( dMag );
							}

							if( ctRegionVectors > 0 )
								vDrawArrow( raster, pxPlotWidth, pxPlotHeight, pxX_from, pxY_from, pxX_to, pxY_to, dBladeHeight, dBladeLength, iRGBA );

							// debugging assistance
//							int pxX_region = pxStartX + Math.round( fPlotScaleX * (float)xX );
//							int pxY_region = pxStartY + Math.round( fPlotScaleY * (float)xY );
//							g2.setColor(Color.CYAN);
//							g2.drawOval(pxX_region, pxY_region, iArrowSize_U, iArrowSize_V);
//							g2.setColor(Color.BLACK);
						}
						break;
					}
					for( int xYOff = 0; xYOff < iArrowSize_V; xYOff++ ){
						if( xY + xYOff == iDataDim_Height ) break; // off boundary of data
						int xDataPoint = (xY+xYOff)*iDataDim_Width + xX+xXOff;
						if( Float.isNaN(mafU[xDataPoint]) || Float.isNaN(mafV[xDataPoint]) ) break AbortVector;
						uTotal += mafU[xDataPoint];
						vTotal += mafV[xDataPoint];
						ctRegionVectors++;
					}
					xXOff++;
				}
			}
		}

		return true;
	}

	boolean getUV( StringBuffer sbError ){
		int iDataType = data.getDataType();
		int iDataDim_Width = data.getDimension_x();
		int iDataDim_Height = data.getDimension_y();
		
		short[] ashData = null; // ??? data.getShortArray();
		int[] aiData = null; // ??? data.getIntArray();
		long[] anData = null; // ??? data.getLongArray();
		float[] afData = null; // ??? data.getFloatArray();
		double[] adData = null; // ??? data.getDoubleArray();
		short[] ashData2 = null; // ??? data.getShortArray2();
		int[] aiData2 = null; // ??? data.getIntArray2();
		long[] anData2 = null; // ??? data.getLongArray2();
		float[] afData2 = null; // ??? data.getFloatArray2();
		double[] adData2 = null; // ??? data.getDoubleArray2();
		
		// validate that U matches V
		boolean zUmatchesV = false;
		switch( iDataType ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				if( ashData.length == ashData2.length ) zUmatchesV = true;
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				if( aiData.length == aiData2.length ) zUmatchesV = true;
				break;
			case DAP.DATA_TYPE_UInt32:
				if( anData.length == anData2.length ) zUmatchesV = true;
				break;
			case DAP.DATA_TYPE_Float32:
				if( afData.length == afData2.length ) zUmatchesV = true;
				break;
			case DAP.DATA_TYPE_Float64:
				if( adData.length == adData2.length ) zUmatchesV = true;
				break;
			default:
				sbError.append( opendap.clients.odc.DAP.getType_String( iDataType ) + " is not a valid data type for a vector plot");
				return false;
		}
		if( ! zUmatchesV ){
			sbError.append("Data selection is invalid because the U variable does not have the same dimensions as the V variable");
			return false;
		}

		int lenData = iDataDim_Height * iDataDim_Width;
		mUmax = Float.MIN_VALUE;
		mVmax = Float.MIN_VALUE;
		if( ! Utility.zMemoryCheck(lenData * 2, 4, sbError) ) return false;
		mafU = new float[lenData];
		mafV = new float[lenData];
		float fTotalU = 0;
		float fTotalV = 0;
		int ctU = 0;
		int ctV = 0;
		switch( iDataType ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				if( ashData.length != ashData2.length ){
					sbError.append("Data selection is invalid because the U variable does not have the same dimensions as the V variable");
				}
				for( int xData = 0; xData < lenData; xData++ ){
					int xMissing = 1;
					while( true ){
						if( xMissing > 0 ){// ??? data.getMissingCount1() ){
							ctU++;
							mafU[xData] = (float)ashData[xData];
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( ashData[xData] == 0 ){ // ??? data.getMissingShort1()[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > 0 ){// ??? data.getMissingCount1() ){
							ctV++;
							mafV[xData] = (float)ashData2[xData];
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( ashData2[xData] == 0 ){ // ??? data.getMissingShort2()[xMissing] ){
							mafV[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
				}
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				if( aiData.length != aiData2.length ){
					sbError.append("Data selection is invalid because the U variable does not have the same dimensions as the V variable");
				}
				for( int xData = 0; xData < lenData; xData++ ){
					int xMissing = 1;
					while( true ){
						if( xMissing > 0 ){ // ??? data.getMissingCount1() ){
							ctU++;
							mafU[xData] = (float)(aiData[xData]);
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( aiData[xData] == 0 ){ // ??? data.getMissingInt2()[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > 0 ){ // ??? data.getMissingCount2() ){
							ctV++;
							mafV[xData] = (float)(aiData2[xData]);
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( aiData2[xData] == 0 ){ // ??? data.getMissingInt2()[xMissing] ){
							mafV[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
				}
				break;
			case DAP.DATA_TYPE_UInt32:
				for( int xData = 0; xData < lenData; xData++ ){
					int xMissing = 1;
					while( true ){
						if( xMissing > 0 ){ // ??? data.getMissingCount1() ){
							ctU++;
							mafU[xData] = (float)(anData[xData]);
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( anData[xData] == 0 ){ // ??? data.getMissingLong2()[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > 0 ){ // ??? data.getMissingCount2() ){
							ctV++;
							mafV[xData] = (float)(anData2[xData]);
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( anData2[xData] == 0 ){ // ??? data.getMissingLong2()[xMissing] ){
							mafV[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
				}
				break;
			case DAP.DATA_TYPE_Float32:
				for( int xData = 0; xData < lenData; xData++ ){
					int xMissing = 1;
					while( true ){
						if( xMissing > 0 ){ // ??? data.getMissingCount1() ){
							ctU++;
							mafU[xData] = afData[xData];
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( afData[xData] == 0 ){ // ??? data.getMissingFloat1()[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > 0 ){ // ??? data.getMissingCount2() ){
							ctV++;
							mafV[xData] = afData2[xData];
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( afData2[xData] == 0 ){ // ??? data.getMissingFloat2()[xMissing] ){
							mafV[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
				}
				break;
			case DAP.DATA_TYPE_Float64:
				for( int xData = 0; xData < lenData; xData++ ){
					int xMissing = 1;
					while( true ){
						if( xMissing > 0 ){ // ??? data.getMissingCount1() ){
							ctU++;
							mafU[xData] = (float)adData[xData];
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( adData[xData] == 0 ){ // ??? data.getMissingDouble1()[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > 0 ){ // ??? data.getMissingCount2() ){
							ctV++;
							mafV[xData] = (float)adData2[xData];
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( adData2[xData] == 0 ){ // ???? data.getMissingDouble2()[xMissing] ){
							mafV[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
				}
				break;
		}
		if( mUmax == 0 || mVmax == 0 ){
			ApplicationController.vShowError("U max " + mUmax + " and V max " + mVmax + " cannot be zero. Unplottable.");
		} else {
			mfAverageU = fTotalU / ctU;
			mfAverageV = fTotalV / ctV;
			mfAverageMag = (float)Math.sqrt( mfAverageU * mfAverageU + mfAverageV * mfAverageV );
		}
		return true;

	}

	// xy1 arrow base
	// xy2 arrow point
	// K   blade height
	// N   blade length (hypotenuse)
	// L   blade width (distance from arrow line)
	void vDrawArrow( int[] raster, int pxWidth, int pxHeight, int x1, int y1, int x2, int y2, double K, double N, int iRGBA ){

		if( K >= N ) return; // draw nothing if height is greater than length
		double L = Math.sqrt(N * N - K * K);
		int x = x2 - x1;
		int y = y2 - y1;
		double A = Math.sqrt(x * x + y * y);

		int x3 = (int)(( (A - K) / A) * x - (L / A) * y);
		int y3 = (int)(( (A - K) / A) * y + (L / A) * x);
		int x5 = (int)(( (A - K) / A) * x + (L / A) * y);
		int y5 = (int)(( (A - K) / A) * y - (L / A) * x);

		int x4 = x1 + x3;
		int y4 = y1 + y3;
		int x6 = x1 + x5;
		int y6 = y1 + y5;

		Utility_Geometry.drawLineToRaster( raster, pxWidth, pxHeight, x1,y1,x2,y2, iRGBA ); // line
		Utility_Geometry.drawLineToRaster( raster, pxWidth, pxHeight, x2,y2,x4,y4, iRGBA); // blade left
		Utility_Geometry.drawLineToRaster( raster, pxWidth, pxHeight, x2,y2,x6,y6, iRGBA); // blade right

	}

}

