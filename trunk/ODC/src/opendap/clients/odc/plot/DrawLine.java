package opendap.clients.odc.plot;

import opendap.clients.odc.ApplicationController;

// all measurements are in pixels

public class DrawLine {
	private DrawLine(){}
	public static void drawLine_straight( java.awt.Graphics2D g2, java.awt.image.BufferedImage mbi, int x1, int y1, int x2, int y2 ){
		java.awt.image.WritableRaster raster = mbi.getRaster();
		int[] blackpixel = new int[] { 0x00, 0x00, 0x00, 0xff};
		int slope_numerator = y2 - y1;
		int slope_denominator = x2 - x1;
		for( int x = x1; x <= x2; x++ ){
			int y = slope_numerator / slope_denominator * (x - x1) + y1;
			raster.setPixel( x, y, blackpixel );
		}
	}
	public static void drawLine_vertical( java.awt.Graphics2D g2, java.awt.image.BufferedImage mbi, int x, int y1, int y2 ){
		java.awt.image.WritableRaster raster = mbi.getRaster();
		int[] blackpixel = new int[] { 0x00, 0x00, 0x00, 0xff };
		if( x < 0 ){
			ApplicationController.vShowWarning( "internal error, invalid x coordinate while drawing vertical line: " + x );
			return;
		}
		if( x >= raster.getWidth() ){ 
			Thread.dumpStack();
			ApplicationController.vShowWarning( "internal error, invalid x coordinate while drawing vertical line: " + x + ", raster width is " + raster.getWidth() );
			return;
		}
		if( y1 < 0 || y2 < 0 ){
			ApplicationController.vShowWarning( "internal error, invalid y coordinate(s) while drawing vertical line :" + y1 + " " + y2 );
			return;
		}
		if( y1 >= raster.getHeight() || y2 >= raster.getHeight() ){ 
			ApplicationController.vShowWarning( "internal error, invalid y coordinate(s) while drawing vertical line :" + y1 + " " + y2 + ", raster height is " + raster.getHeight() );
			return;
		}
		for( int y = y1; y <= y2; y++ ){
			raster.setPixel( x, y, blackpixel );
		}
	}
	public static void drawLine_horizontal( java.awt.Graphics2D g2, java.awt.image.BufferedImage mbi, int y, int x1, int x2 ){
		java.awt.image.WritableRaster raster = mbi.getRaster();
		int[] blackpixel = new int[] { 0x00, 0x00, 0x00, 0xff};
		if( y < 0 ){
			ApplicationController.vShowWarning( "internal error, invalid y coordinate while drawing horizontal line: " + y );
			return;
		}
		if( y >= raster.getHeight() ){ 
			ApplicationController.vShowWarning( "internal error, invalid y coordinate while drawing horizontal line: " + y + ", raster height is " + raster.getHeight() );
			return;
		}
		if( x1 < 0 || x2 < 0 ){
			ApplicationController.vShowWarning( "internal error, invalid x coordinate(s) while drawing horizontal line: " + x1 + " " + x2 );
			return;
		}
		if( x1 >= raster.getWidth() || x2 >= raster.getWidth() ){ 
			Thread.dumpStack();
			ApplicationController.vShowWarning( "internal error, invalid x coordinate(s) while drawing horizontal line: " + x1 + " " + x2 + ", raster width is " + raster.getWidth() );
			return;
		}
		for( int x = x1; x <= x2; x++ ){
			raster.setPixel( x, y, blackpixel );
		}
	}

	//	int[] blackpixel = new int[] { 0x00, 0x00, 0x00, 0xff };
	public static boolean drawLine_points( java.awt.Graphics2D g2, java.awt.image.BufferedImage mbi, int iCount, int offsetX, int[] y_values0, int[] color, boolean zAntialias ){ 
		java.awt.image.WritableRaster raster = mbi.getRaster();
		int xPoint = 0;
		int x = offsetX + xPoint;
		int y = y_values0[xPoint];
		raster.setPixel( x, y, color );      // set first point
		for( ; xPoint < iCount; xPoint++ ){
			int x_last = x;
			int y_last = y;
			x = offsetX + xPoint;
			y = y_values0[xPoint];
			if( y > y_last + 1 ){			// draw intermediate points up
				for( int y_intermediate = y_last + 1; y_intermediate < y; y++ ) 
					raster.setPixel( x, y_intermediate, color );
			} else if( y < y_last - 1 ) {   // draw intermediate points down
				for( int y_intermediate = y_last - 1; y_intermediate > y; y-- ) 
					raster.setPixel( x, y_intermediate, color );
			}
			raster.setPixel( x, y, color );
		}
		return true;
	}
}
