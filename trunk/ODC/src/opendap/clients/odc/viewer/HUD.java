package opendap.clients.odc.viewer;

import opendap.clients.odc.ApplicationController;

import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Color;

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
//	private BufferedImage bimageJavaLogo;
//	private BufferedImage bimageOpenGLLogo;
	private Color colorHUD = Color.WHITE;
	private Font fontHUD = new Font("SansSerif", Font.PLAIN, 12);
	private FontMetrics metricsHUD = null; // set during paint
	private BasicStroke strokeDottedLine = new BasicStroke(
      1f, 
      BasicStroke.CAP_ROUND, 
      BasicStroke.JOIN_ROUND, 
      1f, 
      new float[] {2f}, 
      0f);
	
	int iInfoBox_t_offset = 10;
	int iInfoBox_r_offset = 10;
	
	boolean zNavBoxVisible = false;
	int iNavBox_w;
	int iNavBox_h;
	int iNavBox_x;
	int iNavBox_y;

	private Rectangle rectClip = new Rectangle(); // this is used for efficiency 
	private Rectangle rectNavBox = new Rectangle(); // this is used for efficiency 
	private Rectangle rectInfoBox = new Rectangle(); // this is used for efficiency 
	private Rectangle rectCursorBox = new Rectangle(); // this is used for efficiency
	
	private int miMousePosition_X = 0;
	private int miMousePosition_Y = 0;
	private int mctMousePosition_Strings = 0;
	private String[] masMousePosition_Coordinate = new String[100]; // holds mctMousePosition_Strings of coordinate strings

	public boolean zInitialize( ViewManager vm, StringBuffer sbError ){
		try {
			view_manager = vm;
//			String sJavaLogoPath = "C:/src/ODC/src/opendap/clients/odc/images/java_logo.png"; 
//			String sOpenGLLogoPath = "C:/src/ODC/src/opendap/clients/odc/images/opengl_logo.png";
//			File fileJavaLogo = new File( sJavaLogoPath );
//			File fileOpenGLLogo = new File( sOpenGLLogoPath );
//			BufferedImage bimageJavaLogo_raw = ImageIO.read( fileJavaLogo );
//			bimageJavaLogo = scaleImage( bimageJavaLogo_raw, 0.25f, 0.25f);
//			BufferedImage bimageOpenGLLogo_raw = ImageIO.read( fileOpenGLLogo );
//			bimageJavaLogo = scaleImage( bimageOpenGLLogo_raw, 0.45f, 0.45f);
			return true;
		} catch( Exception t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
	}

	private BufferedImage scaleImage(BufferedImage img, float xScale, float yScale) {
		BufferedImage scaled = new BufferedImage((int) (img.getWidth() * xScale), (int) (img.getHeight() * yScale), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.drawRenderedImage(img, AffineTransform.getScaleInstance(xScale, yScale));
		return scaled;
	}

	public void render( Graphics g, Model2D_Raster rasterNavBox ){
		rectClip = g.getClipBounds( rectClip );
		g.setColor( colorHUD );
		((Graphics2D) g).setRenderingHint( 
				RenderingHints.KEY_TEXT_ANTIALIASING, 
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON
		);
		g.setFont( fontHUD );
		if( metricsHUD == null ){
			metricsHUD = g.getFontMetrics( fontHUD );
			vResize();
		}
		vDrawCursorBox( g );
		vDrawWindowDimensions( g );
		vDrawNavBox( g, rasterNavBox );

//        Rectangle rect = view_manager.getImageClipBounds();
//        Graphics2D gr2 = (Graphics2D)g;
//        gr2.setRenderingHint(
//            RenderingHints.KEY_INTERPOLATION, 
//            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//        gr2.drawImage(subimage, dstx, dsty, dstw, dsth,
//                              rect.x, rect.y, rect.width, rect.height, null);

		if( view_manager.iFrameRate > 0 ){
			g.drawString("FPS: " + view_manager.iFrameRate, view_manager.getViewport().getWidth() - 140, view_manager.getViewport().getHeight() - 30);
		}

		// java logo
//		int sp = 10;
//		if( bimageJavaLogo != null ){
//			g.drawImage(bimageJavaLogo, sp, view_manager.getViewport().getHeight() - bimageJavaLogo.getHeight() - sp, null);
//			if (bimageOpenGLLogo != null) {
//				g.drawImage(bimageOpenGLLogo, sp + bimageJavaLogo.getWidth() + sp, view_manager.getViewport().getHeight() - bimageOpenGLLogo.getHeight() - sp, null);
//			}
//		}


		
	}

	void vResize(){
		int iLineHeight = metricsHUD == null ? 10 : metricsHUD.getHeight();		
		rectCursorBox.width = 100;
		rectCursorBox.height = iLineHeight * 2;
		rectCursorBox.x = view_manager.iVP_w - rectCursorBox.width - iInfoBox_r_offset - 2;
		rectCursorBox.y = iInfoBox_t_offset - 2;
//		rectInfoBox.width = 100;
//		rectInfoBox.height = iNavBox_w * view_manager.iVPB_h / view_manager.iVPB_w;
//		rectInfoBox.x = view_manager.iVP_w - rectInfoBox.width - iInfoBox_r_offset - 2;
//		rectInfoBox.y = view_manager.iVP_h - iNavBox_h - iInfoBox_t_offset - 2;
	}

	void vDrawCursorBox( Graphics g ){
		if( ! g.hitClip( rectCursorBox.x, rectCursorBox.y, rectCursorBox.width, rectCursorBox.height ) ) return;
		int xLineNumber = 0;
		int iLineHeight = metricsHUD.getHeight();
		int offX = rectCursorBox.x;
		int offY = rectCursorBox.y;
		g.drawString( "x: " + miMousePosition_X, offX, offY + iLineHeight * ++xLineNumber ); 
		g.drawString( "y: " + miMousePosition_Y, offX, offY + iLineHeight * ++xLineNumber ); 
	}
	
	void vDrawWindowDimensions( Graphics g ){
		int offX = 10;
		int offY = 5;
		int iLineHeight = metricsHUD.getHeight();
		int xLineNumber = 0;
		g.drawString( "x: " + view_manager.iVP_x, offX, offY + iLineHeight * ++xLineNumber ); 
		g.drawString( "y: " + view_manager.iVP_y, offX, offY + iLineHeight * ++xLineNumber ); 
		g.drawString( "w: " + view_manager.iVP_w, offX, offY + iLineHeight * ++xLineNumber ); 
		g.drawString( "h: " + view_manager.iVP_h, offX, offY + iLineHeight * ++xLineNumber ); 
		g.drawString( "bounds w: " + view_manager.iVPB_w, offX, offY + iLineHeight * ++xLineNumber ); 
		g.drawString( "bounds h: " + view_manager.iVPB_h, offX, offY + iLineHeight * ++xLineNumber ); 
		g.drawString( "center x: " + view_manager.iVPC_x, offX, offY + iLineHeight * ++xLineNumber ); 
		g.drawString( "center y: " + view_manager.iVPC_y, offX, offY + iLineHeight * ++xLineNumber ); 
		g.drawString( "zoom: " + view_manager.iZoomLevel, offX, offY + iLineHeight * ++xLineNumber ); 
	}
	
	// draw nav box (1 pixel width) if image size exceeds viewport size
	void vDrawNavBox( Graphics g, Model2D_Raster raster ){
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
			zNavBoxVisible = true;
		} else {
			zNavBoxVisible = false;
		}
	}
	
	void mouseClick( int x, int y ){
		// System.out.println("nav box dims: " + iNavBox_x + " " + iNavBox_y + " " + iNavBox_w + " " + iNavBox_h );
		if( x > iNavBox_x && x < iNavBox_x + iNavBox_w && y > iNavBox_y && y < iNavBox_y + iNavBox_h ){ // in nav box
			int iNewCenter_x = (x - iNavBox_x - 1) * view_manager.iVPB_w / iNavBox_w;
			int iNewCenter_y = (y - iNavBox_y - 1) * view_manager.iVPB_h / iNavBox_h;
			view_manager.setCenter( iNewCenter_x, iNewCenter_y );
		}
	}
	
	// mouse move
	void mouseMove( int x, int y ){
		miMousePosition_X = x + view_manager.iVP_x;
		miMousePosition_Y = y + view_manager.iVP_y;
		mctMousePosition_Strings = 0;
		view_manager.getViewport().repaint( rectCursorBox );
	}
}
