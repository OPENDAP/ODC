package opendap.clients.odc.plot;

/**
 * Title:        Panel_Plot_Pseudocolor
 * Description:  Plots pseudocolor grids
 * Copyright:    Copyright (c) 2002-12
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.41
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;

class Panel_Plot_Pseudocolor extends Panel_Plot {

	IPlottable plottableData = null;
	
	private int mpxRenderedPlotWidth, mpxRenderedPlotHeight;
	private int mpxMargin_Top = 10; // TODO
	private int mpxMargin_Left = 10; // TODO

	Panel_Plot_Pseudocolor( PlotEnvironment environment, String sID, String sCaption ){
		super( environment, sID, sCaption );
//		addMouseListener(this); // interfering with full screen cancel
	}

	public String getDescriptor(){ return "P"; }

	static boolean mzTraceErrorOnce = true;
	StringBuffer msbError = new StringBuffer(80);
	public boolean zGenerateImage( BufferedImage bi, int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){
		if( bi == null ){
			sbError.append( "system error, no graphics buffer" );
			return false;
		}
		if( pxPlotWidth < 10 || pxPlotHeight < 10 ){
			ApplicationController.vShowWarning("invalid plot width / height " + pxPlotWidth + " / " + pxPlotHeight);
			if( pxPlotWidth < 10 ) pxPlotWidth = 10;
			if( pxPlotHeight < 10 ) pxPlotHeight = 10;
		}

		mpxRenderedPlotWidth = pxPlotWidth;
		mpxRenderedPlotHeight = pxPlotHeight;

		Graphics2D g2 = (Graphics2D)bi.getGraphics();
		g2.setColor(Color.BLACK);
		try {

			// create pixel colormap
			if( maiRGBArray == null ){
				if( zCreateRGBArray( pxPlotWidth, pxPlotHeight, false, msbError ) ){
					// created
				} else {
					g2.setColor(Color.BLACK);
					g2.drawString("Error: " + msbError, 10, 25);
					msbError.setLength(0);
				}
			}

		} catch( Throwable t ) {
			if( mzTraceErrorOnce ){
				sbError.append( "Failed to render RGB array" );
				ApplicationController.vUnexpectedError( t, sbError  );
				mzTraceErrorOnce = false;
			}
			return false;
		}


		try {

			// draw it to the buffer
			if( maiRGBArray == null ){
				sbError.append( "Internal error, graphic buffer does not exist" );
				return false;
			}
			int pxStartX = mpxMargin_Left;
			int pxStartY = mpxMargin_Top;
			int pxOffset = 0;
			int pxScanlineStride = pxPlotWidth;

			if( pxStartX + pxPlotWidth > bi.getWidth() || pxStartY + pxPlotHeight > bi.getHeight() ){
				if( mzTraceErrorOnce ){
					mzTraceErrorOnce = false;
				}
				sbError.append( "Internal error, invalid plotting dimensions. startx: " + pxStartX + " + plot width: " + pxPlotWidth + " > buffer width: " + bi.getWidth() + " or starty: " + pxStartY + " + plot height: " + pxPlotHeight + " > buffer height: " + bi.getHeight() );
				return false;
			} else {
				bi.setRGB( pxStartX, pxStartY, pxPlotWidth, pxPlotHeight, maiRGBArray, pxOffset, pxScanlineStride );
			}

			return true;
//System.out.println(" mbi width: " + mbi.getWidth() + " height: " + mbi.getHeight());
//System.out.println(" startx: " + pxStartX + " starty: " + pxStartY + " plot width: " + pxPlotWidth + " plot height: " + pxPlotHeight);
//		    fyi pixel   = rgbArray[offset + (y-startY)*scansize + (x-startX)];
//			g2.setColor(Color.WHITE); // for debugging
//			g2.drawRect(pxStartX, pxStartY, pxPlotWidth - 1, pxPlotHeight - 1); // for debugging
		} catch( Throwable t ) {
			if( mzTraceErrorOnce ){
				sbError.append( "Failed to generate plot" );
				ApplicationController.vUnexpectedError( t, sbError  );
				mzTraceErrorOnce = false;
			}
			return false;
		}
	}

	/** if the image is scaled down then averaging will average pixels instead of sampling them */
	// averaging is not functional currently
	public boolean zCreateRGBArray( IPlottable data, int pxWidth, int pxHeight, boolean zAveraged, StringBuffer sbError ){
		plottableData = data;
		int iDataDim_Width = data.getDimension_x();
		int iDataDim_Height = data.getDimension_y();
		switch( data.getDataType() ){
			case DAP.DATA_TYPE_Float32:
				maiRGBArray = mColors.aiRender( data.getFloatArray(), iDataDim_Width, iDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DAP.DATA_TYPE_Float64:
				maiRGBArray = mColors.aiRender( data.getDoubleArray(), iDataDim_Width, iDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				maiRGBArray = mColors.aiRender( data.getShortArray(), iDataDim_Width, iDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				maiRGBArray = mColors.aiRender( data.getIntArray(), iDataDim_Width, iDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DAP.DATA_TYPE_UInt32:
				maiRGBArray = mColors.aiRender( data.getLongArray(), iDataDim_Width, iDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
		}
		return maiRGBArray != null;
	}

}



