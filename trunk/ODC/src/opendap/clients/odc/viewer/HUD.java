package opendap.clients.odc.viewer;

import opendap.clients.odc.ApplicationController;

import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/** The HUD (Heads Up Display) controls rendering of the following screen elements:
 * 		- Screen Pixel Ruler
 * 		- FPS (frames per second)
 * 		- Help
 * 		- Scale control
 * 		- Mode notification box
 * 		- Info box
 * 	These elements are all rendered in 2D only on the top level.
 *  Any element that renders in 3D must be on a texture layer.
 */

public class HUD {
	private ViewManager view_manager;
	private Color colorHUD = Color.WHITE;
	
	private ArrayList<HUD_Element> listElements = new ArrayList<HUD_Element>();
	
//	private Rectangle rectCursorBox = null; // this is used for efficiency
	HUD_Element_CursorBox cursor_box;
	HUD_Element_NavBox nav_box;
	HUD_Element_Dimensions dimensions;
	HUD_Element_Animation animation;
	
	private int miMousePosition_X = 0;
	private int miMousePosition_Y = 0;
	public int getMouseX(){ return miMousePosition_X; }
	public int getMouseY(){ return miMousePosition_Y; }
	
	public boolean zInitialize( ViewManager vm, StringBuffer sbError ){
		try {
			view_manager = vm;
			
			// create elements
			cursor_box = new HUD_Element_CursorBox( this );
			nav_box = new HUD_Element_NavBox( vm );
			dimensions = new HUD_Element_Dimensions( vm );
			animation = new HUD_Element_Animation( vm );
			
			// define location of each element
			cursor_box.setRelativeLayout( new RelativeLayout( vm,
					RelativeLayout.Orientation.TopRight,
					RelativeLayout.Orientation.TopRight,
					10, 10, 0 ) );
			nav_box.setRelativeLayout( new RelativeLayout( vm,
					RelativeLayout.Orientation.BottomRight,
					RelativeLayout.Orientation.BottomRight,
					10, 10, 0 ) );
			dimensions.setRelativeLayout( new RelativeLayout( vm,
					RelativeLayout.Orientation.TopLeft,
					RelativeLayout.Orientation.TopLeft,
					10, 10, 0 ) );
			animation.setRelativeLayout( new RelativeLayout( vm,
					RelativeLayout.Orientation.RightMiddle,
					RelativeLayout.Orientation.RightMiddle,
					10, 10, 0 ) );
			
			// add elements to management list
			listElements.add( dimensions );
			listElements.add( cursor_box );
			listElements.add( nav_box );
			listElements.add( animation );
			
			return true;
		} catch( Exception t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
	}

	public void elementAddImage( BufferedImage image ){
		HUD_Element_Image element = new HUD_Element_Image( image );
		element.setRelativeLayout( new RelativeLayout( view_manager,
				RelativeLayout.Orientation.BottomLeft,
				RelativeLayout.Orientation.BottomLeft,
				10, 10, 0 ) );
		listElements.add( element );		
	}
	
// possible optimization: -Duse.clip.optimization=true	
	
	public void render( Graphics g, Model2D_Raster rasterNavBox ){
		g.setColor( colorHUD );
		((Graphics2D) g).setRenderingHint( 
				RenderingHints.KEY_TEXT_ANTIALIASING, 
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON
		);
		if( g.getClipBounds() == cursor_box.getRect() ){
			cursor_box.draw( g );
		} else {
			nav_box.setRaster( rasterNavBox );
			for( HUD_Element element : listElements ){
				element.drawBackground( g );
				element.draw( g );
			}
		}

		if( view_manager.iFrameRate > 0 ){
			g.drawString("FPS: " + view_manager.iFrameRate, view_manager.getViewport().getWidth() - 140, view_manager.getViewport().getHeight() - 30);
		}
		
	}

	void vResize(){
		for( HUD_Element element : listElements ){
			element.resize();
		}
	}

	void mouseClick( int x, int y ){
		for( HUD_Element element : listElements ){
			if( element.getRect().contains( x, y ) ){
				element.mouseClick(x, y);
				break;
			}
		}
	}
	
	// mouse move
	void mouseMove( int x, int y ){
		miMousePosition_X = x + view_manager.iVP_x;
		miMousePosition_Y = y + view_manager.iVP_y;
		view_manager.getViewport().repaint( cursor_box.getRect() );
	}
}

abstract class HUD_Element {
	protected int x = 0;
	protected int y = 0;
	protected int width  = 100;
	protected int height = 100;
	protected Font font = new Font("SansSerif", Font.PLAIN, 12);
	protected FontMetrics metrics = null;
	
	protected RelativeLayout layout = null;

	abstract void draw( Graphics g );
	void mouseClick( int x, int y ){}

	void drawBackground( Graphics g ){
		g.setColor( Color.BLACK );
		g.fillRect( x, y, width, height );
		g.setColor( Color.WHITE );
	}
	
	void setRelativeLayout( RelativeLayout layout ){ this.layout = layout; } 
	java.awt.Rectangle getRect(){
		return new java.awt.Rectangle( x, y, width, height ); 
	}
	
	void resize(){
		java.awt.Point p = layout.getLocation( width, height ); 
		x = p.x;
		y = p.y;
	}
	
}

class HUD_Element_Dimensions extends HUD_Element {
	ViewManager view_manager = null;
	HUD_Element_Dimensions( ViewManager vm ){ view_manager = vm; }
	
	void draw( Graphics g ){
		g.setFont( font );
		if( metrics == null ){
			metrics = g.getFontMetrics( font );
		}
		int xLineNumber = 0;
		int iLineHeight = metrics.getHeight();
		int iLineWidth = metrics.stringWidth( "bounds w: " + view_manager.iVPB_w );
		g.drawString( "x: " + view_manager.iVP_x, x, y + iLineHeight * ++xLineNumber ); 
		g.drawString( "y: " + view_manager.iVP_y, x, y + iLineHeight * ++xLineNumber ); 
		g.drawString( "w: " + view_manager.iVP_w, x, y + iLineHeight * ++xLineNumber ); 
		g.drawString( "h: " + view_manager.iVP_h, x, y + iLineHeight * ++xLineNumber ); 
		g.drawString( "bounds w: " + view_manager.iVPB_w, x, y + iLineHeight * ++xLineNumber ); 
		g.drawString( "bounds h: " + view_manager.iVPB_h, x, y + iLineHeight * ++xLineNumber ); 
		g.drawString( "center x: " + view_manager.iVPC_x, x, y + iLineHeight * ++xLineNumber ); 
		g.drawString( "center y: " + view_manager.iVPC_y, x, y + iLineHeight * ++xLineNumber ); 
		g.drawString( "zoom: " + view_manager.iZoomLevel, x, y + iLineHeight * ++xLineNumber );
		height = iLineHeight * xLineNumber;
		width = iLineWidth;
	}

	void mouseClick( int x, int y ){}

}

class HUD_Element_CursorBox extends HUD_Element {
	HUD theHUD = null;
	HUD_Element_CursorBox( HUD hud ){ theHUD = hud; } 
	void draw( Graphics g ){
		if( ! g.hitClip( x, y, width, height ) ) return;
		int xLineNumber = 0;
		int iLineHeight = g.getFontMetrics( font ).getHeight();
		int offX = x;
		int offY = y;
		g.drawString( "x: " + theHUD.getMouseX(), offX, offY + iLineHeight * ++xLineNumber ); 
		g.drawString( "y: " + theHUD.getMouseY(), offX, offY + iLineHeight * ++xLineNumber ); 
	}
}

class HUD_Element_Animation extends HUD_Element {
	private ViewManager view_manager = null;
	HUD_Element_Animation(  ViewManager vm ){ 
		view_manager = vm;
	} 
	void draw( Graphics g ){
		if( ! g.hitClip( x, y, width, height ) ) return;
		int xLineNumber = 0;
		int iLineHeight = g.getFontMetrics( font ).getHeight();
		int offX = x;
		int offY = y;
		String s = "timeslice: " + view_manager.iTimeslice;
		g.drawString( s, offX, offY + iLineHeight * ++xLineNumber );
		java.awt.Graphics2D g2 = (java.awt.Graphics2D)g;
//		width = g2.getFontMetrics().stringWidth( s );
		height = g2.getFontMetrics().getHeight();
	}
}

class HUD_Element_NavBox extends HUD_Element {

	private int iNavBox_w;
	private int iNavBox_h;
	private int iNavBox_x;
	private int iNavBox_y;

	private final BasicStroke strokeDottedLine = new BasicStroke(
      1f, 
      BasicStroke.CAP_ROUND, 
      BasicStroke.JOIN_ROUND, 
      1f, 
      new float[] {2f}, 
      0f);
	
	private ViewManager view_manager = null;
	private Model2D_Raster raster = null;

	void setRaster( Model2D_Raster raster ){ this.raster = raster; }
	
	HUD_Element_NavBox( ViewManager vm ){
		view_manager = vm;
	}

	// draw nav box (1 pixel width) if image size exceeds viewport size
	void draw( Graphics g  ){
		if( view_manager.iVPB_h > view_manager.iVP_h ||
			view_manager.iVPB_w > view_manager.iVP_w ){
			int iNavBox_x_offset = 10;
			int iNavBox_y_offset = 10;
			iNavBox_w = 100;
			iNavBox_h = iNavBox_w * view_manager.iVPB_h / view_manager.iVPB_w;
			iNavBox_x = view_manager.iVP_w - iNavBox_w - iNavBox_x_offset - 2;
			iNavBox_y = view_manager.iVP_h - iNavBox_h - iNavBox_y_offset - 2;
			float fScale = 1;
			if( view_manager.iZoomLevel > 0 ){
				fScale = 1 / (view_manager.iZoomLevel + 1);
			} else if( view_manager.iZoomLevel < 0 ){
				fScale = view_manager.iZoomLevel * -1 + 1; 
			}
			int iNavBlock_w = (int)(iNavBox_w * view_manager.iVP_w * fScale / view_manager.iVPB_w);
			int iNavBlock_h = (int)(iNavBox_h * view_manager.iVP_h * fScale / view_manager.iVPB_h);
			int iNavBlock_x = iNavBox_x + view_manager.iVP_x * iNavBox_w / view_manager.iVPB_w;
			int iNavBlock_y = iNavBox_y + view_manager.iVP_y * iNavBox_h / view_manager.iVPB_h;
			if( iNavBlock_w < 5 ) iNavBlock_w = 5;
			if( iNavBlock_h < 5 ) iNavBlock_h = 5;
			raster.renderThumb( g, iNavBox_x, iNavBox_y, iNavBox_w, iNavBox_h );
			g.drawRect( iNavBox_x, iNavBox_y, iNavBox_w, iNavBox_h);
			Stroke strokeDefault = ((Graphics2D)g).getStroke(); 
			((Graphics2D)g).setStroke( strokeDottedLine );
			g.drawRect( iNavBlock_x, iNavBlock_y, iNavBlock_w, iNavBlock_h);
			((Graphics2D)g).setStroke( strokeDefault );
//			System.out.println("nav block: " + iNavBlock_x + " " + iNavBlock_y + " " + iNavBlock_w + " " + iNavBlock_h );
//			zNavBoxVisible = true;
			x = iNavBox_x;
			y = iNavBox_y;
			width = iNavBox_w;
			height = iNavBox_h;
		} else {
//			zNavBoxVisible = false;
			x = 0;
			y = 0;
			width = 0;
			height = 0;
		}
	}

	void mouseClick( int x, int y ){
		int iNewCenter_x = (x - iNavBox_x - 1) * view_manager.iVPB_w / iNavBox_w;
		int iNewCenter_y = (y - iNavBox_y - 1) * view_manager.iVPB_h / iNavBox_h;
		view_manager.setCenter( iNewCenter_x, iNewCenter_y );
	}
	
}

class HUD_Element_Image extends HUD_Element {
	private BufferedImage mbi = null;
	HUD_Element_Image( BufferedImage bi ){
		mbi = bi;
		width = mbi.getWidth();
		width = mbi.getHeight();
	}
	void draw( Graphics g  ){
		g.drawImage( mbi, x, y, null);
	}

}



