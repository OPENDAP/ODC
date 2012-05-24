package opendap.clients.odc.plot;

import java.awt.image.BufferedImage;
import java.awt.Rectangle;

import opendap.clients.odc.ApplicationController;

abstract public class Plot extends CompositionElement {
	protected Plot( PlotEnvironment environment, PlotLayout layout ){
		this.environment = environment;
		this.layout = layout;
	}
	protected IPlottableVariable data = null; // needed for data microscope
	protected String msCaption = null; // needed for data microscope
	public PlotEnvironment environment = null;
	public PlotLayout layout = null;
	public String getCaption(){ return msCaption; }
	abstract public String getDescriptor();
	abstract public boolean render( int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ); // returns the raster
	protected int[] raster = null;
	public int[] getRaster( int iRasterLength ){
		if( raster == null || raster.length != iRasterLength ){
			raster = new int[ iRasterLength ];
		}
		return raster;
	}

	// draws the plot into the designated buffer
	public boolean zRender( BufferedImage bi, Rectangle location, StringBuffer sbError ){
		try {
			int iDataPoint_width = data.getDimension_x();
			int iDataPoint_height = data.getDimension_y();
			PlotScale scale = environment.getScale();
			scale.setDataDimension( iDataPoint_width, iDataPoint_height );
			
			// determine offset
			int pxOffset_left = scale.getMarginLeft_px();
			int pxOffset_top  = scale.getMarginTop_px();
			layout.setOffsetHorizontal( pxOffset_left );
			layout.setOffsetVertical( pxOffset_top );
			
			// standard scaled area
			int pxPlotWidth = scale.getPlot_Width_pixels();
			int pxPlotHeight = scale.getPlot_Height_pixels();	
			if( pxPlotWidth == 0 || pxPlotHeight == 0 ){
				sbError.append( "invalid scale, plot width/height cannot be <= 0" );
				return false;
			}
			
			return zWriteToImageBuffer( bi, pxOffset_left, pxOffset_top, pxPlotWidth, pxPlotHeight, sbError );
		} catch( Throwable ex ){
			ApplicationController.vUnexpectedError( ex, sbError );
			return false;
		}
	}

	public boolean zWriteToImageBuffer( BufferedImage bi, int pxOffset_left, int pxOffset_top, int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){
		if( bi == null ){
			sbError.append( "internal error, no buffer (out of memory?)" );
			return false;
		} else {
			if( raster.length != pxPlotWidth * pxPlotHeight ){
				raster = null; // trigger collection
				raster = new int[ pxPlotWidth * pxPlotHeight ];
			}
			if( ! render( pxPlotWidth, pxPlotHeight, sbError ) ){
				sbError.insert( 0, "failed to render plot: " );
				return false;
			}
			bi.setRGB( pxOffset_left, pxOffset_top, pxPlotWidth, pxPlotHeight, raster, 0, pxPlotWidth );
			return true;
		}
	}
}
