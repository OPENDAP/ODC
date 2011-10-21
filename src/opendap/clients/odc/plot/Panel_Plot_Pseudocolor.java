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
 * Title:        Panel_Plot_Pseudocolor
 * Description:  Plots pseudocolor grids
 * Copyright:    Copyright (c) 2002-4
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.41
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.data.Model_Dataset;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.swing.JOptionPane;
import javax.swing.JDialog;

class Panel_Plot_Pseudocolor extends Panel_Plot {

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
				bi.setRGB(pxStartX, pxStartY, pxPlotWidth, pxPlotHeight, maiRGBArray, pxOffset, pxScanlineStride);
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
	public boolean zCreateRGBArray( int pxWidth, int pxHeight, boolean zAveraged, StringBuffer sbError ){
		int iDataDim_Width = mPlottable.getDimension_x();
		int iDataDim_Height = mPlottable.getDimension_y();
		switch( mPlottable.getDataType() ){
			case DAP.DATA_TYPE_Float32:
				maiRGBArray = mColors.aiRender( mPlottable.getFloatArray(), iDataDim_Width, iDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DAP.DATA_TYPE_Float64:
				maiRGBArray = mColors.aiRender( mPlottable.getDoubleArray(), iDataDim_Width, iDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				maiRGBArray = mColors.aiRender( mPlottable.getShortArray(), iDataDim_Width, iDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				maiRGBArray = mColors.aiRender( mPlottable.getIntArray(), iDataDim_Width, iDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
			case DAP.DATA_TYPE_UInt32:
				maiRGBArray = mColors.aiRender( mPlottable.getLongArray(), iDataDim_Width, iDataDim_Height, pxWidth, pxHeight, zAveraged, sbError);
				break;
		}
		return maiRGBArray != null;
	}

	void vShowDataMicroscope(int xPlot, int yPlot){
		int iDataDim_Width = mPlottable.getDimension_x();
		int iDataDim_Height = mPlottable.getDimension_y();

		// determine data coordinates
		int xClick = xPlot * iDataDim_Width / mpxRenderedPlotWidth;
		int yClick = yPlot * iDataDim_Height / mpxRenderedPlotHeight;

		// render colors and store values
		int[] aRGB = new int[36];
		String[] as = new String[36];
		switch( mPlottable.getDataType() ){
			case DAP.DATA_TYPE_Float32:
				for( int xDataWidth = 0; xDataWidth < 6; xDataWidth++ ){
					for( int xDataHeight = 0; xDataHeight < 6; xDataHeight++ ){
						int xRGB = 6 * xDataHeight + xDataWidth;
						int xData = iDataDim_Width * (yClick + xDataHeight) + (xClick + xDataWidth);
						if( xData < 0 || xData > mPlottable.getFloatArray().length ){
							as[xRGB] = null;
						} else {
							as[xRGB] = Float.toString( mPlottable.getFloatArray()[xData] );
						}
					}
				}
				aRGB = mColors.aiRender( mPlottable.getFloatArray(), 6, 6, 6, 6, false, msbError);
				if( aRGB == null ){
					msbError.setLength(0);
					return;  // todo errors
				}
				break;
			case DAP.DATA_TYPE_Float64:
				break;
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				break;
			case DAP.DATA_TYPE_UInt32:
				break;
		}

		final Panel_Microscope microscope = new Panel_Microscope();
		final JOptionPane jop = new JOptionPane(microscope, JOptionPane.INFORMATION_MESSAGE);
		final JDialog jd = jop.createDialog(ApplicationController.getInstance().getAppFrame(), "Data Microscope ( " + xPlot + ", " + yPlot + " )");
		microscope.set(aRGB, as);
		jd.setVisible( true );
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



