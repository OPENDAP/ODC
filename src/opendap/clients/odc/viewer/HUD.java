package opendap.clients.odc.viewer;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.Color;

import javax.imageio.ImageIO;

import opendap.clients.odc.Utility;

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
	private BasicStroke strokeDottedLine = new BasicStroke(
      1f, 
      BasicStroke.CAP_ROUND, 
      BasicStroke.JOIN_ROUND, 
      1f, 
      new float[] {2f}, 
      0f);
	
	int iNavBox_w;
	int iNavBox_h;
	int iNavBox_x;
	int iNavBox_y;
	
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
			Utility.vUnexpectedError( t, sbError );
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

	private static Font fpsFont = new Font("SansSerif", Font.BOLD, 24);
	public void render( Graphics g ){
		g.setColor( colorHUD );
		
//        Rectangle rect = view_manager.getImageClipBounds();
//        Graphics2D gr2 = (Graphics2D)g;
//        gr2.setRenderingHint(
//            RenderingHints.KEY_INTERPOLATION, 
//            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//        gr2.drawImage(subimage, dstx, dsty, dstw, dsth,
//                              rect.x, rect.y, rect.width, rect.height, null);

		if( view_manager.iFrameRate > 0 ){
			((Graphics2D) g).setRenderingHint( 
					RenderingHints.KEY_TEXT_ANTIALIASING, 
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON
			);
			g.setFont(fpsFont);
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

		// draw nav box (1 pixel width) if image size exceeds viewport size
		if( view_manager.iVPB_h > view_manager.iVP_h ||
			view_manager.iVPB_w > view_manager.iVP_w ){
			int iNavBox_x_offset = 10;
			int iNavBox_y_offset = 10;
			iNavBox_w = 100;
			iNavBox_h = iNavBox_w * view_manager.iVPB_h / view_manager.iVPB_w;
			iNavBox_x = view_manager.iVP_w - iNavBox_w - iNavBox_x_offset - 2;
			iNavBox_y = view_manager.iVP_h - iNavBox_h - iNavBox_y_offset - 2;
			g.drawRect( iNavBox_x, iNavBox_y, iNavBox_w, iNavBox_h);
			int iNavBlock_w = iNavBox_w * view_manager.iVP_w / view_manager.iVPB_w;
			int iNavBlock_h = iNavBox_h * view_manager.iVP_h / view_manager.iVPB_h;
			int iNavBlock_x = iNavBox_x + view_manager.iVP_x * view_manager.iVP_w / view_manager.iVPB_w;
			int iNavBlock_y = iNavBox_y + view_manager.iVP_y * view_manager.iVP_h / view_manager.iVPB_h;
			((Graphics2D)g).setStroke( strokeDottedLine );
			g.drawRect( iNavBlock_x, iNavBlock_y, iNavBlock_w, iNavBlock_h);
		}

		
	}

	void click( int x, int y ){
		System.out.println("nav box dims: " + iNavBox_x + " " + iNavBox_y + " " + iNavBox_w + " " + iNavBox_h );
		if( x > iNavBox_x && x < iNavBox_x + iNavBox_w && y > iNavBox_y && y < iNavBox_y + iNavBox_h ){ // in nav box
			System.out.println("in box");
			int iNewCenter_x = (x - iNavBox_x - 1) * view_manager.iVPB_w / iNavBox_w;
			int iNewCenter_y = (y - iNavBox_y - 1) * view_manager.iVPB_h / iNavBox_h;
			view_manager.setCenter( iNewCenter_x, iNewCenter_y );
		}
	}
	
	
}
