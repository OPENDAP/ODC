package opendap.clients.odc.viewer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import java.awt.RenderingHints;

import opendap.clients.odc.ApplicationController;

public class Model2D_Raster {
	BufferedImage mbi = null;
	int iWidth = 0;
	int iHeight = 0;

	void render(  Graphics g, int x_VP, int y_VP, int w_VP, int h_VP, int zoom, int iSubImage_x, int iSubImage_y ){
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(
				RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY ); // emphasize image quality
		g2.setRenderingHint(
				RenderingHints.KEY_INTERPOLATION, 
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		int iSubImage_width = 0;
		int iSubImage_height = 0;
		int iDestination_width = iWidth > w_VP - x_VP ? w_VP - x_VP : iWidth; 
		int iDestination_height = iHeight > h_VP - y_VP ? h_VP - y_VP : iHeight; 
		if( zoom == 0 ){ // image is 1:1
			iSubImage_width = iDestination_width;
			iSubImage_height = iDestination_height;
		} else if ( zoom < 1 ){ // image is shrunken
			int iPixelRatio = zoom * -1 + 1;
			iSubImage_width = iDestination_width * iPixelRatio;
			iSubImage_height = iDestination_height * iPixelRatio;
		} else { // image is enlarged
			int iPixelRatio = zoom + 1;
			iSubImage_width = iDestination_width / iPixelRatio;
			iSubImage_height = iDestination_height / iPixelRatio;
		}
//		System.out.println("drawing image at " + x_VP + " " + y_VP + " " + iSubImage_width + " " + iSubImage_height);  
//		System.out.println("subimage at " + iSubImage_x + " " + iSubImage_y + " " + iSubImage_width + " " + iSubImage_height);  
		g2.drawImage( mbi,
				x_VP, y_VP, x_VP + iDestination_width, y_VP + iDestination_height,        // destination coordinates
				iSubImage_x, iSubImage_y, iSubImage_x + iSubImage_width, iSubImage_y + iSubImage_height,  // source coordinates
				null);
	}

	/** this version can be used when the whole image is desired */
	void render(  Graphics g, int x, int y, int w, int h  ){
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(
				RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY ); // emphasize image quality
		g2.setRenderingHint(
				RenderingHints.KEY_INTERPOLATION, 
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		System.out.println("drawing image at " + x + " " + y + " " + w + " " + h );
		g2.drawImage( mbi,
				x, y, x + w, y + h,        // destination coordinates
				0, 0, iWidth, iHeight,  // source coordinates
				null);
	}
	
	/** renders and caches a thumbnail image */
	BufferedImage mbiThumb = null;
	int iThumb_width = 0;
	int iThumb_height = 0;
	void renderThumb( Graphics g, int x, int y, int w, int h  ){
		if( w != iThumb_width || h != iThumb_height ){ // need to make new thumb 
			mbiThumb = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
			iThumb_width = w;
			iThumb_height = h;
			int scaleX = iWidth / iThumb_width;
			int scaleY = iHeight / iThumb_height;

	//					int iValueSum = 0;
	//					int ctValues = 0;
	//					for( int xCol_avg = -10; xCol_avg <= 10; xCol_avg++ ){ // average values around pixel
	//						for( int xRow_avg = -10; xRow_avg <= 10; xRow_avg++ ){
	//							if( xMBI + xCol_avg >= 0 && xMBI + xCol_avg < iWidth  && yMBI + xRow_avg >= 0 && yMBI + xRow_avg < iHeight ){
	//								iValueSum += mbi.getRGB( xMBI + xCol_avg, yMBI + xRow_avg );
	//								ctValues++;
	//							}
	//						}
	//					}
	//					mbiThumb.setRGB( xCol, xRow, iValueSum / ctValues ); 

			if( scaleX <= 1 && scaleY <= 1 ){
				for( int xCol = 0; xCol < w; xCol++ ){
					int xMBI = xCol * scaleX; 
					for( int xRow = 0; xRow < h; xRow++ ){
						int yMBI = xRow * scaleY;
						mbiThumb.setRGB( xCol, xRow, mbi.getRGB( xMBI, yMBI ) ); // no averaging
					}
				}
			} else {
				for( int xCol_thumb = 0; xCol_thumb < w; xCol_thumb++ ){
					for( int xRow_thumb = 0; xRow_thumb < h; xRow_thumb++ ){
						int ctElements = 0;
						int iTotal_1 = 0;
						int iTotal_2 = 0;
						int iTotal_3 = 0;
						int iTotal_4 = 0;
						int offCol = (iWidth - scaleX - 1) * xCol_thumb / iThumb_width;
						for( int xCol = offCol; xCol < offCol + scaleX; xCol++ ){
							int offRow = (iHeight - scaleY - 1) * xRow_thumb / iThumb_height;
							for( int xRow = offRow; xRow < offRow + scaleY; xRow++ ){
								ctElements++;
								int iRGB = mbi.getRGB( xCol, xRow );
								iTotal_1 += (iRGB & 0xFF000000) >>> 24;
								iTotal_2 += (iRGB & 0x00FF0000) >> 16;
								iTotal_3 += (iRGB & 0x0000FF00) >> 8;
								iTotal_4 += (iRGB & 0x000000FF);
							}
						}
						int iValue_1 = iTotal_1 / ctElements; 
						int iValue_2 = iTotal_2 / ctElements;
						int iValue_3 = iTotal_3 / ctElements;
						int iValue_4 = iTotal_4 / ctElements;
						int iRGB_average = (iValue_1 << 24 ) | (iValue_2 << 16) | (iValue_3 << 8) | iValue_4;
						mbiThumb.setRGB( xCol_thumb, xRow_thumb, iRGB_average );
					}
				}
			}
			
		}
		((Graphics2D)g).drawImage( mbiThumb, x, y, w, h, null);
	}
	
	public final boolean zLoadImageFromFile( String sFullPath, StringBuffer sbError ){
		try {
			File fileImage = new File( sFullPath );
			if( ! fileImage.exists() ){ sbError.append("file (" + sFullPath + ") does not exist"); return false; }
			mbi = ImageIO.read( fileImage );
			iWidth = mbi.getWidth();
			iHeight = mbi.getHeight(); 
		} catch( Exception t ) {
			ApplicationController.vUnexpectedError( t, sbError );
			return false;
		}
		return true;
	}
}
