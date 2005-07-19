package opendap.clients.odc.plot;

/**
 * Title:        Panel_Plot_Vector
 * Description:  Plots vector grids
 * Copyright:    Copyright (c) 2003
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.31
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DodsURL;
import opendap.clients.odc.Utility;

import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

class Panel_Plot_Vector extends Panel_Plot {

	private String mDisplay_sMessage = null;

	Panel_Plot_Vector( PlotScale scale, String sID, String sCaption, DodsURL url ){
		super(scale, sID, sCaption, url);
	}

	public String getDescriptor(){ return "V"; }

	final static int PX_DEFAULT_VECTOR_SIZE = 10;
	float[] mafU = null; // generates max as a side effect
	float[] mafV = null;
	float[] mafMissingU = null;
	float[] mafMissingV = null;
	float mUmax, mVmax; // will be * PX_MAX_VECTOR squared
	float mUavg, mVavg;
	float mfAverageU, mfAverageV;

	StringBuffer msbError = new StringBuffer();

	// When the image is generated only one arrow is drawn per vector region, a region
	// being a square with the size of the nominal maximum arrow. To find the net magnitude for
	// the arrow, all the vectors in the region are averaged.
	public void vGenerateImage( int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
		if( miDataType == 0 ) return; // nothing to plot

		Graphics2D g2 = (Graphics2D)mbi.getGraphics();
		g2.setColor(Color.black);

		g2.setClip(mpxMargin_Left, mpxMargin_Top, pxPlotWidth, pxPlotHeight);

		// determine vector size in pixels
		int iVectorSize;
		if( getPlotOptions() == null ){
			iVectorSize = PX_DEFAULT_VECTOR_SIZE;
		} else {
			iVectorSize = getPlotOptions().getValue_int(PlotOptions.OPTION_VectorSize);
			if( iVectorSize <= 0 ) iVectorSize = PX_DEFAULT_VECTOR_SIZE;
		}

		// determine overall scale
		float fPlotScaleX = (float)(pxPlotWidth - iVectorSize) / mDataDim_Width; // extra size is added to the top and left to accommodate vectors pointing in that direction
		float fPlotScaleY = (float)(pxPlotHeight - iVectorSize) / mDataDim_Height;

		// determine vector scaling
		int iArrowSize_U = 0;
		int iArrowSize_V = 0;
		iArrowSize_U = iVectorSize;
		iArrowSize_V = iVectorSize;
		if( mafU == null ){
			if( getUV( msbError ) ){
				// generates max as a side effect
			} else {
				g2.drawString("Error: " + msbError, 20, 20);
			}
		}
		if( mUmax == 0 || mVmax == 0 ) return; // cannot plot
		iArrowSize_U /= fPlotScaleX; // if the plot is bigger then the sample size should be smaller (and vice versa)
		iArrowSize_V /= fPlotScaleY;
		float fScaleU = mfAverageU * 2f / iArrowSize_U;
		float fScaleV = mfAverageV * 2f / iArrowSize_V;
		// draw plot (u = x  v = y)
		int pxX_Origin = mpxMargin_Left + mpxAxisOffsetWidth + iArrowSize_U;
//		int pxY_Origin = mpxMargin_Top + mpxAxisOffsetHeight + iArrowSize_V;
		int pxY_Origin = mpxMargin_Top + pxPlotHeight - mpxAxisOffsetHeight - iArrowSize_V;
		for( int xX = 0; xX < mDataDim_Width; xX += iArrowSize_U ){
			for( int xY = 0; xY < mDataDim_Height; xY += iArrowSize_V ){
				int sampleX = xX;
				int sampleY = xY;
				float uTotal = 0;
				float vTotal = 0;
				int xXOff = 0;
				int ctRegionVectors = 0;
AbortVector:
				while(true){ // find average of all vectors in region
					if( xXOff == iArrowSize_U || xX + xXOff == mDataDim_Width ){ // done
						if( ctRegionVectors == 0 ){
							// no vectors in this region, draw nothing
						} else {
							float fXmag = uTotal/(float)ctRegionVectors/fScaleU;
							float fYmag = vTotal/(float)ctRegionVectors/fScaleV;
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
							if( ctRegionVectors > 0 )
								vDrawArrow( g2, pxX_from, pxY_from, pxX_to, pxY_to, dBladeHeight, dBladeLength);

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
						if( xY + xYOff == mDataDim_Height ) break; // off boundary of data
						int xDataPoint = (xY+xYOff)*mDataDim_Width + xX+xXOff;
						if( Float.isNaN(mafU[xDataPoint]) || Float.isNaN(mafV[xDataPoint]) ) break AbortVector;
						uTotal += mafU[xDataPoint];
						vTotal += mafV[xDataPoint];
						ctRegionVectors++;
					}
					xXOff++;
				}
			}
		}

		g2.setClip(0, 0, pxCanvasWidth, pxCanvasHeight);

	}

	public boolean zCreateRGBArray(int pxWidth, int pxHeight, boolean zAveraged, StringBuffer sbError){
		sbError.append("internal error, rgb array creation not applicable to plot line");
		return false;
	}

	boolean getUV( StringBuffer sbError ){
		int lenData = mDataDim_Height * mDataDim_Width;
		mUmax = Float.MIN_VALUE;
		mVmax = Float.MIN_VALUE;
		if( !Utility.zMemoryCheck(lenData * 2, 4, sbError) ) return false;
		mafU = new float[lenData];
		mafV = new float[lenData];
		float fTotalU = 0;
		float fTotalV = 0;
		int ctU = 0;
		int ctV = 0;
		switch(miDataType){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				for( int xData = 0; xData < lenData; xData++ ){
					int xMissing = 1;
					while( true ){
						if( xMissing > mctMissing1 ){
							ctU++;
							mafU[xData] = (float)mashData[xData];
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( mashData[xData] == mashMissing1[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > mctMissing2 ){
							ctV++;
							mafV[xData] = (float)mashData2[xData];
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( mashData2[xData] == mashMissing2[xMissing] ){
							mafV[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
				}
				break;
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				for( int xData = 0; xData < lenData; xData++ ){
					int xMissing = 1;
					while( true ){
						if( xMissing > mctMissing1 ){
							ctU++;
							mafU[xData] = (float)(maiData[xData]);
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( maiData[xData] == maiMissing1[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > mctMissing2 ){
							ctV++;
							mafV[xData] = (float)(maiData2[xData]);
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( maiData2[xData] == maiMissing2[xMissing] ){
							mafV[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
				}
				break;
			case DATA_TYPE_UInt32:
				for( int xData = 0; xData < lenData; xData++ ){
					int xMissing = 1;
					while( true ){
						if( xMissing > mctMissing1 ){
							ctU++;
							mafU[xData] = (float)(manData[xData]);
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( manData[xData] == manMissing1[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > mctMissing2 ){
							ctV++;
							mafV[xData] = (float)(manData2[xData]);
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( manData2[xData] == manMissing2[xMissing] ){
							mafV[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
				}
				break;
			case DATA_TYPE_Float32:
				for( int xData = 0; xData < lenData; xData++ ){
					int xMissing = 1;
					while( true ){
						if( xMissing > mctMissing1 ){
							ctU++;
							mafU[xData] = mafData[xData];
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( mafData[xData] == mafMissing1[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > mctMissing2 ){
							ctV++;
							mafV[xData] = mafData2[xData];
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( mafData2[xData] == mafMissing2[xMissing] ){
							mafV[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
				}
				break;
			case DATA_TYPE_Float64:
				for( int xData = 0; xData < lenData; xData++ ){
					int xMissing = 1;
					while( true ){
						if( xMissing > mctMissing1 ){
							ctU++;
							mafU[xData] = (float)madData[xData];
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( madData[xData] == madMissing1[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > mctMissing2 ){
							ctV++;
							mafV[xData] = (float)madData2[xData];
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( madData2[xData] == madMissing2[xMissing] ){
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
		}
		return true;

	}

	// xy1 arrow base
	// xy2 arrow point
	// K   blade height
	// N   blade length (hypotenuse)
	// L   blade width (distance from arrow line)
	void vDrawArrow(Graphics g, int x1, int y1, int x2, int y2, double K, double N){

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

		g.drawLine(x1,y1,x2,y2); // line
		g.drawLine(x2,y2,x4,y4); // blade left
		g.drawLine(x2,y2,x6,y6); // blade right

	}

}

