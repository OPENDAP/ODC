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
 * Title:        Panel_Plot_Vector
 * Description:  Plots vector grids
 * Copyright:    Copyright (c) 2003, 2008
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.02
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Model_Dataset;
import opendap.clients.odc.Utility;
import opendap.clients.odc.DAP;

import java.awt.*;

class Panel_Plot_Vector extends Panel_Plot {

	Panel_Plot_Vector( PlotScale scale, String sID, String sCaption, Model_Dataset url ){
		super(scale, sID, sCaption, url);
	}

	public String getDescriptor(){ return "V"; }

	final static int PX_DEFAULT_VECTOR_SIZE = 10;
	private float[] mafU = null; // generates max as a side effect
	private float[] mafV = null;
	private float mUmax, mVmax; // will be * PX_MAX_VECTOR squared
	private float mfAverageU, mfAverageV;

	StringBuffer msbError = new StringBuffer();

	// When the image is generated only one arrow is drawn per vector region, a region
	// being a square with the size of the nominal maximum arrow. To find the net magnitude for
	// the arrow, all the vectors in the region are averaged.
	public void vGenerateImage( int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
		if( mPlottable.getDataType() == 0 ) return; // nothing to plot

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

		int iDataDim_Width = mPlottable.getDimension_x();
		int iDataDim_Height = mPlottable.getDimension_y();
		
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

		g2.setClip(0, 0, pxCanvasWidth, pxCanvasHeight);

	}

	public boolean zCreateRGBArray(int pxWidth, int pxHeight, boolean zAveraged, StringBuffer sbError){
		sbError.append("internal error, rgb array creation not applicable to vector plot");
		return false;
	}

	boolean getUV( StringBuffer sbError ){
		int iDataType = mPlottable.getDataType();
		int iDataDim_Width = mPlottable.getDimension_x();
		int iDataDim_Height = mPlottable.getDimension_y();
		
		short[] ashData = mPlottable.getShortArray();
		int[] aiData = mPlottable.getIntArray();
		long[] anData = mPlottable.getLongArray();
		float[] afData = mPlottable.getFloatArray();
		double[] adData = mPlottable.getDoubleArray();
		short[] ashData2 = mPlottable.getShortArray2();
		int[] aiData2 = mPlottable.getIntArray2();
		long[] anData2 = mPlottable.getLongArray2();
		float[] afData2 = mPlottable.getFloatArray2();
		double[] adData2 = mPlottable.getDoubleArray2();
		
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
						if( xMissing > mPlottable.getMissingCount1() ){
							ctU++;
							mafU[xData] = (float)ashData[xData];
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( ashData[xData] == mPlottable.getMissingShort1()[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > mPlottable.getMissingCount1() ){
							ctV++;
							mafV[xData] = (float)ashData2[xData];
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( ashData2[xData] == mPlottable.getMissingShort2()[xMissing] ){
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
						if( xMissing > mPlottable.getMissingCount1() ){
							ctU++;
							mafU[xData] = (float)(aiData[xData]);
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( aiData[xData] == mPlottable.getMissingInt2()[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > mPlottable.getMissingCount2() ){
							ctV++;
							mafV[xData] = (float)(aiData2[xData]);
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( aiData2[xData] == mPlottable.getMissingInt2()[xMissing] ){
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
						if( xMissing > mPlottable.getMissingCount1() ){
							ctU++;
							mafU[xData] = (float)(anData[xData]);
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( anData[xData] == mPlottable.getMissingLong2()[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > mPlottable.getMissingCount2() ){
							ctV++;
							mafV[xData] = (float)(anData2[xData]);
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( anData2[xData] == mPlottable.getMissingLong2()[xMissing] ){
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
						if( xMissing > mPlottable.getMissingCount1() ){
							ctU++;
							mafU[xData] = afData[xData];
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( afData[xData] == mPlottable.getMissingFloat1()[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > mPlottable.getMissingCount2() ){
							ctV++;
							mafV[xData] = afData2[xData];
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( afData2[xData] == mPlottable.getMissingFloat2()[xMissing] ){
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
						if( xMissing > mPlottable.getMissingCount1() ){
							ctU++;
							mafU[xData] = (float)adData[xData];
							if( mafU[xData] > 0 ) fTotalU += mafU[xData]; else fTotalU -= mafU[xData]; // total of absolute value
							if( mafU[xData] > mUmax ) mUmax = mafU[xData];
							else if( mafU[xData]*-1 > mUmax ) mUmax = mafU[xData]*-1;
							break;
						}
						if( adData[xData] == mPlottable.getMissingDouble1()[xMissing] ){
							mafU[xData] = Float.NaN;
							break;
						}
						xMissing++;
					}
					xMissing = 1;
					while( true ){
						if( xMissing > mPlottable.getMissingCount2() ){
							ctV++;
							mafV[xData] = (float)adData2[xData];
							if( mafV[xData] > 0 ) fTotalV += mafV[xData]; else fTotalV -= mafV[xData]; // total of absolute value
							if( mafV[xData] > mVmax ) mVmax = mafV[xData];
							else if( mafV[xData]*-1 > mVmax ) mVmax = mafV[xData]*-1;
							break;
						}
						if( adData2[xData] == mPlottable.getMissingDouble2()[xMissing] ){
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

