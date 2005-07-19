package opendap.clients.odc.plot;

/**
 * Title:        Panel_Plot_Pseudocolor
 * Description:  Plots pseudocolor grids
 * Copyright:    Copyright (c) 2002-4
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.41
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility;
import opendap.clients.odc.DodsURL;

import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

class Panel_Plot_Pseudocolor extends Panel_Plot {

	private String mDisplay_sMessage = null;
	private int mpxRenderedCanvasWidth, mpxRenderedCanvasHeight;
	private int mpxRenderedPlotWidth, mpxRenderedPlotHeight;

	Panel_Plot_Pseudocolor( PlotScale scale, String sID, String sCaption, DodsURL url ){
		super(scale, sID, sCaption, url);
//		addMouseListener(this); // interfering with full screen cancel
	}

	public String getDescriptor(){ return "P"; }

	static boolean mzTraceErrorOnce = true;
	StringBuffer msbError = new StringBuffer(80);
	public void vGenerateImage( int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
		if( mbi == null ){
			ApplicationController.vShowError("system error, no graphics buffer");
			return;
		}
		if( pxPlotWidth < 10 || pxPlotHeight < 10 ){
			ApplicationController.vShowWarning("invalid plot width / height " + pxPlotWidth + " / " + pxPlotHeight);
			if( pxPlotWidth < 10 ) pxPlotWidth = 10;
			if( pxPlotHeight < 10 ) pxPlotHeight = 10;
		}

		mpxRenderedCanvasWidth = pxCanvasWidth;
		mpxRenderedCanvasHeight = pxCanvasHeight;
		mpxRenderedPlotWidth = pxPlotWidth;
		mpxRenderedPlotHeight = pxPlotHeight;

		Graphics2D g2 = (Graphics2D)mbi.getGraphics();
		g2.setColor(Color.BLACK);
		try {

			// create pixel colormap
			if( maiRGBArray == null ){
				if( zCreateRGBArray(pxPlotWidth, pxPlotHeight, false, msbError) ){
					// created
				} else {
					g2.setColor(Color.BLACK);
					g2.drawString("Error: " + msbError, 10, 25);
					msbError.setLength(0);
				}
			}

		} catch( Throwable t ) {
			if( mzTraceErrorOnce ){
				Utility.vUnexpectedError(t, "Failed to render RGB array");
				mzTraceErrorOnce = false;
			}
			g2.setColor(Color.BLACK);
			g2.drawString("Unexpected error generating plot (see errors)", 10, 25);
		}


		try {

			// draw it to the buffer
			if( maiRGBArray == null ){
				g2.setColor(Color.BLACK);
				g2.drawString("Internal error, graphic buffer does not exist", 10, 25);
				return;
			}
			int pxStartX = mpxMargin_Left;
			int pxStartY = mpxMargin_Top;
			int pxOffset = 0;
			int pxScanlineStride = pxPlotWidth;

			if( pxStartX + pxPlotWidth > mbi.getWidth() || pxStartY + pxPlotHeight > mbi.getHeight() ){
				if( mzTraceErrorOnce ){
					ApplicationController.vShowError("Internal error, invalid plotting dimensions. startx: " + pxStartX + " + plot width: " + pxPlotWidth + " > buffer width: " + mbi.getWidth() + " or starty: " + pxStartY + " plot height: " + pxPlotHeight + " > buffer height: " + mbi.getHeight() );
		    		mzTraceErrorOnce = false;
				}
				g2.drawString("Unexpected error generating plot (see errors)", 10, 25);
			} else {
				mbi.setRGB(pxStartX, pxStartY, pxPlotWidth, pxPlotHeight, maiRGBArray, pxOffset, pxScanlineStride);
			}

//System.out.println(" mbi width: " + mbi.getWidth() + " height: " + mbi.getHeight());
//System.out.println(" startx: " + pxStartX + " starty: " + pxStartY + " plot width: " + pxPlotWidth + " plot height: " + pxPlotHeight);
//		    fyi pixel   = rgbArray[offset + (y-startY)*scansize + (x-startX)];
//			g2.setColor(Color.WHITE); // for debugging
//			g2.drawRect(pxStartX, pxStartY, pxPlotWidth - 1, pxPlotHeight - 1); // for debugging
		} catch( Throwable t ) {
			if( mzTraceErrorOnce ){
				Utility.vUnexpectedError(t, "Failed to generate plot");
			}
			g2.setColor(Color.BLACK);
			g2.drawString("Unexpected error generating plot (see errors)", 10, 25);
		}
	}

	/** if the image is scaled down then averaging will average pixels instead of sampling them */
	// averaging is not functional currently
	public boolean zCreateRGBArray( int pxWidth, int pxHeight, boolean zAveraged, StringBuffer sbError ){

		switch(miDataType){
			case DATA_TYPE_Float32:
				maiRGBArray = mColors.aiRender(mafData, mDataDim_Width, mDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DATA_TYPE_Float64:
				maiRGBArray = mColors.aiRender(madData, mDataDim_Width, mDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				maiRGBArray = mColors.aiRender(mashData, mDataDim_Width, mDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				maiRGBArray = mColors.aiRender(maiData, mDataDim_Width, mDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DATA_TYPE_UInt32:
				maiRGBArray = mColors.aiRender(manData, mDataDim_Width, mDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
		}
		return maiRGBArray != null;
	}

	void vShowDataMicroscope(int xPlot, int yPlot){

		// determine data coordinates
		int xClick = xPlot * mDataDim_Width / mpxRenderedPlotWidth;
		int yClick = yPlot * mDataDim_Height / mpxRenderedPlotHeight;

		// render colors and store values
		int[] aRGB = new int[36];
		String[] as = new String[36];
		switch(miDataType){
			case DATA_TYPE_Float32:
				float[] afData = new float[36];
				for( int xDataWidth = 0; xDataWidth < 6; xDataWidth++ ){
					for( int xDataHeight = 0; xDataHeight < 6; xDataHeight++ ){
						int xRGB = 6 * xDataHeight + xDataWidth;
						int xData = mDataDim_Width * (yClick + xDataHeight) + (xClick + xDataWidth);
						if( xData < 0 || xData > mafData.length ){
							as[xRGB] = null;
						} else {
							as[xRGB] = Float.toString( mafData[xData] );
							afData[xRGB] = mafData[xData];
						}
					}
				}
				aRGB = mColors.aiRender(afData, 6, 6, 6, 6, false, msbError);
				if( aRGB == null ){
					msbError.setLength(0);
					return;  // todo errors
				}
				break;
			case DATA_TYPE_Float64:
				break;
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				break;
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				break;
			case DATA_TYPE_UInt32:
				break;
		}

		final Panel_Microscope microscope = new Panel_Microscope();
		final JOptionPane jop = new JOptionPane(microscope, JOptionPane.INFORMATION_MESSAGE);
		final JDialog jd = jop.createDialog(ApplicationController.getInstance().getAppFrame(), "Data Microscope ( " + xPlot + ", " + yPlot + " )");
		microscope.set(aRGB, as);
		jd.show();
	}

	/** Do not use this currently because we need to receive mouse events
	 *  to trap mouse click on full screen. revisit later

	// Mouse motion interface
	public void mouseMoved(MouseEvent evt){
	}

	// Mouse listener interface
	public void mousePressed(MouseEvent evt){
	}

	public void mouseDragged(MouseEvent evt){
	}

	public void mouseReleased(MouseEvent evt){
	}

	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }
	public void mouseClicked(MouseEvent evt){
		int xPX = evt.getX();
		int yPX = evt.getY();
		if( xPX > mpxMargin_Left && xPX < (mpxRenderedCanvasWidth - mpxMargin_Right) && yPX > mpxMargin_Top && yPX < (mpxRenderedCanvasHeight - mpxMargin_Bottom) )
		    vShowDataMicroscope( xPX - mpxMargin_Left, yPX - mpxMargin_Top );
	}
	 */

}



