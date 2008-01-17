package opendap.clients.odc.viewer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import opendap.clients.odc.Utility;
import java.awt.RenderingHints;

public class Model2D_Raster {
	BufferedImage mbi = null;
	int iWidth = 0;
	int iHeight = 0;

	void render(  Graphics g, int x_VP, int y_VP, int w_VP, int h_VP, int zoom, int iSubImage_x, int iSubImage_y  ){
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(
				RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY ); // emphasize image quality
		g2.setRenderingHint(
				RenderingHints.KEY_INTERPOLATION, 
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		int iSubImage_width = 0;
		int iSubImage_height = 0;
		if( zoom == 0 ){ // image is 1:1
			iSubImage_width = iWidth > w_VP - x_VP ? w_VP - x_VP : iWidth;
			iSubImage_height = iHeight > h_VP - y_VP ? h_VP - y_VP : iHeight;
		} else if ( zoom < 1 ){ // image is shrunken
			int iZoom_width_10k;
		} else { // image is enlarged
		}
		g2.drawImage( mbi,
				x_VP, y_VP, iSubImage_width, iSubImage_height,        // destination coordinates
				iSubImage_x, iSubImage_y, iSubImage_width, iSubImage_height,  // source coordinates
				null);
	}
	
	boolean zLoadImageFromFile( String sFullPath, StringBuffer sbError ){
		try {
			File fileImage = new File( sFullPath );
			if( ! fileImage.exists() ){ sbError.append("file (" + sFullPath + ") does not exist"); return false; }
			mbi = ImageIO.read( fileImage );
			iWidth = mbi.getWidth();
			iHeight = mbi.getHeight(); 
		} catch( Exception t ) {
			Utility.vUnexpectedError( t, sbError );
			return false;
		}
		return true;
	}
}
