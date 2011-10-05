package opendap.clients.odc.plot;

// all measurements are in pixels

public class DrawText {
	private DrawText(){}
	
	// orientation is relative to the text bounds
	public static void drawText( java.awt.image.BufferedImage mbi, String sText, TextStyle style, int x, int y, PlotLayout.ORIENTATION alignment, int rotation_degrees ){
		java.awt.Graphics2D g2 = (java.awt.Graphics2D)mbi.getGraphics();
		int x_origin = 0;
		int y_origin = 0;
		int pxTextLength = style.getTextWidthX_pixels( g2, sText );
		int pxTextHeight = style.getTextHeightY_pixels( g2, sText );
		switch( alignment ){
			case TopLeft:
				x_origin = x;
				y_origin = y;
				break;
			case TopMiddle:
				x_origin = x + pxTextLength / 2;
				y_origin = y;
				break;
			case TopRight:
				x_origin = x - pxTextLength;
				y_origin = y;
				break;
			case BottomLeft:
				x_origin = x;
				y_origin = y - pxTextHeight;
				break;
			case BottomMiddle:
				x_origin = x + pxTextLength / 2;
				y_origin = y - pxTextHeight;
				break;
			case BottomRight:
				x_origin = x - pxTextLength;
				y_origin = y - pxTextHeight;
				break;
			case LeftMiddle:
				x_origin = x - pxTextLength;
				y_origin = y - pxTextHeight / 2;
				break;
			case RightMiddle:
				x_origin = x;
				y_origin = y - pxTextHeight / 2;
				break;
			case Center:
				x_origin = x + pxTextLength / 2;
				y_origin = y + pxTextHeight / 2;
				break;
		}
		g2.setFont( style.getFont() );
		g2.setColor( style.getColor() );
		g2.drawString( sText, x_origin, y_origin );
	}
}
