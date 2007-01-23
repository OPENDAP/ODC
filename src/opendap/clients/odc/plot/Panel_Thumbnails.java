package opendap.clients.odc.plot;

/**
 * Title:        Panel_Thumbnails
 * Description:  Thumbnail canvas
 * Copyright:    Copyright (c) 2003
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.38
 */

import opendap.clients.odc.DodsURL;
import java.util.ArrayList;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Styles;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

public class Panel_Thumbnails extends JPanel implements Printable, MouseListener {

	private final static Dimension MIN_DIMENSION = new Dimension(200, 400);

	protected BufferedImage mbi = null;
	protected boolean mzMode_FullScreen = false;

	// margins and spacing
	int mpxMargin_Left   = 10;
	int mpxMargin_Right  = 10;
	int mpxMargin_Top    = 10;
	int mpxMargin_Bottom = 10;

	// colors
	protected Color mcolorBackground = null;
	protected Color mcolorMissing = null;
	void setColor_Background( Color color ){ mcolorBackground = color; }

	private final ArrayList listThumbnails = new ArrayList();
	private final int[] Thumbnail_x1 = new int[501]; // records corner coordinates of thumbnails for hit testing
	private final int[] Thumbnail_y1 = new int[501];
	private final int[] Thumbnail_x2 = new int[501];
	private final int[] Thumbnail_y2 = new int[501];

	Panel_Definition mParent = null;

	Panel_Thumbnails(Panel_Definition parent){
		mParent = parent;
		mpxMargin_Left   = 10;
		mpxMargin_Right  = 10;
		mpxMargin_Top    = 10;
		mpxMargin_Bottom = 10;
		setBackground(Styles.colorNeutralYellow1);
		this.addMouseListener(this);
	}

	// this is needed to tell any container how big the panel wants to be
	private Dimension dimPreferred = new Dimension(10, 10);
    public Dimension getPreferredSize() {
		return dimPreferred;
    }

	private void setDimensions( int iWidth, int iHeight ){
		dimPreferred = new Dimension(iWidth, iHeight);
	}

	BufferedImage getImage(){ return mbi; }

	public int print( Graphics g, PageFormat page_format, int page_index ){
		if( page_index < 0 || page_index > 1 ){
			// ApplicationController.vShowError("Cannot make multi-page printouts");
			return java.awt.print.Printable.NO_SUCH_PAGE;
		}
//		vGenerateImage();
		((Graphics2D)g).drawImage(mbi, null, 0, 0); // flip image to printer
		return java.awt.print.Printable.PAGE_EXISTS;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		try {
			if( mbi == null ) vGenerateImage();
			((Graphics2D)g).drawImage(mbi, null, 0, 0); // flip image to canvas
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, "Error rendering thumbnails panel");
		}
	}

	Stroke strokeMarquee = new BasicStroke(2);
	public void vGenerateImage(){
		int pxContainerWidth = getParent().getWidth();
		if( pxContainerWidth < 250 ) pxContainerWidth = 250;

		Color colorBackground = Styles.colorNeutralYellow1;

		// determine panel dimensions
		int pxMargin = 10;
		int pxVSpace = 30;
		int pxHSpace = 15;
		int ctThumbnails = listThumbnails.size();
		if( ctThumbnails == 0 ){
		}
		int pxWidthNeeded = pxMargin;
		int pxHeightNeeded = pxMargin;
		int pxRowWidth = pxMargin;
		int pxRowHeight = 0;
		int ctThumbnailsInRow = 0;
		int xThumbnail = 1;
		while( true ){
			if( xThumbnail > ctThumbnails ) break; // done
			if( xThumbnail > 500 ) break; // max number of thumbnails
			Thumbnail tn = (Thumbnail)listThumbnails.get(xThumbnail - 1);
			int pxImageHeight = tn.image.getHeight(null);
			int pxImageWidth = tn.image.getWidth(null);
			if( pxRowWidth + pxImageWidth + pxMargin > pxContainerWidth && ctThumbnailsInRow > 0){ // end of row
				ctThumbnailsInRow = 0;
				pxHeightNeeded += pxRowHeight + pxVSpace;
				pxRowWidth = pxMargin;
				pxRowHeight = 0;
			} else { // add image to row
				ctThumbnailsInRow++;
				pxRowWidth += pxImageWidth + pxHSpace;
				if( pxImageHeight > pxRowHeight ) pxRowHeight = pxImageHeight;
				if( pxRowWidth > pxWidthNeeded ) pxWidthNeeded = pxRowWidth;
				if( pxImageHeight > pxHeightNeeded ) pxHeightNeeded = pxImageHeight;
				xThumbnail++;
			}
		}
		pxHeightNeeded += pxRowHeight + pxVSpace + pxMargin;  // for final row
		mbi = new BufferedImage(pxWidthNeeded, pxHeightNeeded, BufferedImage.TYPE_INT_ARGB);
		setDimensions( pxWidthNeeded, pxHeightNeeded );
		Graphics2D g2 = (Graphics2D)mbi.getGraphics();

		// draw thumbnails
		pxRowWidth = pxMargin;
		pxRowHeight = 0;
		ctThumbnailsInRow = 0;
		xThumbnail = 1;
		int coordinates_x1 = pxMargin, coordinates_y1 = pxMargin;
		g2.setStroke(strokeMarquee);
		g2.setFont(Styles.fontSansSerif10);
		FontMetrics fm = g2.getFontMetrics(Styles.fontSansSerif10);
		int iFontAscent = fm.getAscent();
		while( true ){
			if( xThumbnail > ctThumbnails ) break; // done
			if( xThumbnail > 500 ) break; // max number of thumbnails
			Thumbnail tn = (Thumbnail)listThumbnails.get(xThumbnail - 1);
			int pxImageWidth = tn.image.getWidth(null);
			int pxImageHeight = tn.image.getHeight(null);
			if( pxRowWidth + pxImageWidth + pxHSpace > pxContainerWidth && ctThumbnailsInRow > 0){ // end of row
				ctThumbnailsInRow = 0;
				coordinates_x1 = pxMargin;
				coordinates_y1 += pxRowHeight + pxVSpace;
				pxRowWidth = pxMargin;
				pxRowHeight = 0;
			} else { // add image to row
				ctThumbnailsInRow++;

				// draw image
				g2.setClip(coordinates_x1 - 4, coordinates_y1 - 4, pxImageWidth + 8, pxImageHeight + 11 + iFontAscent);
				if( tn.zSelected ){ // draw selection highlight
					g2.setColor(Styles.colorCyanHighlight);
					g2.fillRect(coordinates_x1 - 4, coordinates_y1 - 4, pxImageWidth + 8, pxImageHeight + 11 + iFontAscent);
				}
				g2.drawImage(tn.image, coordinates_x1, coordinates_y1, colorBackground, null);
				Thumbnail_x1[xThumbnail] = coordinates_x1;
				Thumbnail_y1[xThumbnail] = coordinates_y1;
				Thumbnail_x2[xThumbnail] = coordinates_x1 + pxImageWidth - 1;
				Thumbnail_y2[xThumbnail] = coordinates_y1 + pxImageHeight - 1;

				// draw caption
				if( tn.zSelected ){
					g2.setColor(Color.WHITE);
				} else {
					g2.setColor(Color.BLACK);
				}
				g2.setClip(coordinates_x1, coordinates_y1 + pxImageHeight + 2, pxImageWidth, 30);
				String sCaption = tn.sCaption;
				if( sCaption == null ) sCaption = tn.url.getFileName();
				if( sCaption == null ) sCaption = "image " + xThumbnail;
				g2.drawString(sCaption, coordinates_x1 + 1, coordinates_y1 + pxImageHeight + 2 + iFontAscent);

				// draw selection box
				g2.setClip(null);
				if( tn.zSelected ){
					g2.setColor(Color.BLUE);
					g2.drawRect(coordinates_x1 - 4, coordinates_y1 - 4, pxImageWidth + 8, pxImageHeight + 11 + iFontAscent);
				}

				coordinates_x1 += pxImageWidth + pxHSpace;
				pxRowWidth = coordinates_x1;
				if( pxImageHeight > pxRowHeight ) pxRowHeight = pxImageHeight;
				xThumbnail++;
			}
		}

	}

	public void addThumbnail(DodsURL url, String sCaption, Image imageThumbnail){
		if( url == null ){
			ApplicationController.vShowError("system error: attempt to add thumbnail with null URL");
			return;
		}
		Thumbnail thumbnailNew = new Thumbnail();
		thumbnailNew.url = url;
		if( sCaption.length() > 16 ){
			thumbnailNew.sCaption = url.getFileName(); // caption is too long
		} else {
			thumbnailNew.sCaption = sCaption;
		}
		thumbnailNew.image = imageThumbnail;
		thumbnailNew.zSelected = false;
		listThumbnails.add(thumbnailNew);
		vGenerateImage();
		repaint();
	}

	public void vSelectAll(){
		int ctThumbnails = listThumbnails.size();
		for( int xThumbnail = 1; xThumbnail <= ctThumbnails; xThumbnail++ ){
			Thumbnail tn = (Thumbnail)listThumbnails.get(xThumbnail - 1);
			tn.zSelected = true;
		}
		vGenerateImage();
		repaint();
	}

	public void vDeleteSelected(){
		int ctThumbnails = listThumbnails.size();
		for( int xThumbnail = ctThumbnails; xThumbnail > 0 ; xThumbnail-- ){
			Thumbnail tn = (Thumbnail)listThumbnails.get(xThumbnail - 1);
			if( tn.zSelected ) listThumbnails.remove(tn);
		}
		vGenerateImage();
		repaint();
	}

	public void vReRetrieve(){
		int ctSelected = 0;
		int ctThumbnails = listThumbnails.size();
		for( int xThumbnail = 1; xThumbnail <= ctThumbnails; xThumbnail++ ){
			Thumbnail tn = (Thumbnail)listThumbnails.get(xThumbnail - 1);
			if( tn.zSelected ){
				ctSelected++;
			}
		}
		DodsURL[] listURLsToReRetrieve = new DodsURL[ctSelected];
		int xSelected = -1;
		for( int xThumbnail = 1; xThumbnail <= ctThumbnails; xThumbnail++ ){
			Thumbnail tn = (Thumbnail)listThumbnails.get(xThumbnail - 1);
			if( tn.zSelected ){
				xSelected++;
				listURLsToReRetrieve[xSelected] = tn.url;
			}
		}
		if( ctSelected == 0 ){
			ApplicationController.vShowWarning("nothing to re-retrieve, no thumbnail is selected (use ctrl-click to select)");
		} else {
			opendap.clients.odc.Model_URLList urlList = ApplicationController.getInstance().getRetrieveModel().getURLList();
			if( urlList == null ){
				ApplicationController.vShowWarning("internal error, unable to re-retrieve, no URL list");
			} else {
				urlList.vDatasets_Add( listURLsToReRetrieve, false );
			}
		}
	}

	public DodsURL[] getSelectedURLs0(){
		int ctSelected = 0;
		int ctThumbnails = listThumbnails.size();
		for( int xThumbnail = 1; xThumbnail <= ctThumbnails; xThumbnail++ ){
			Thumbnail tn = (Thumbnail)listThumbnails.get(xThumbnail - 1);
			if( tn.zSelected ) ctSelected++;
		}
		DodsURL[] urls = new DodsURL[ctSelected];
		int xSelection = -1;
		for( int xThumbnail = 1; xThumbnail <= ctThumbnails; xThumbnail++ ){
			Thumbnail tn = (Thumbnail)listThumbnails.get(xThumbnail - 1);
			if( tn.zSelected ){
				xSelection++;
				urls[xSelection] = tn.url;
			}
		}
		return urls;
	}

	private void vClickSelect(Thumbnail tn){
		if( tn.zSelected ){
			tn.zSelected = false;
		} else {
			tn.zSelected = true;
		}
		vGenerateImage();
		repaint();
	}

	private void vClickPlot(Thumbnail tn){
System.out.println("click plotting url: " + tn.url);
		Panel_View_Plot.getInstance().vPlot(tn.url, Output_ToPlot.FORMAT_ExternalWindow);
	}

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

	Thumbnail mtnActive = null;
	final Timer timerSingleClickCapture = new Timer( 200, new ActionListener(){
		public void actionPerformed(ActionEvent e) { // single click
			timerSingleClickCapture.stop();
		}
	});
	public void mouseClicked(MouseEvent evt){
		int xPX = evt.getX();
		int yPX = evt.getY();
		int ctThumbnails = listThumbnails.size();
		int ctClicks = evt.getClickCount();
		for( int xThumbnail = 1; xThumbnail <= ctThumbnails; xThumbnail++ ){
			if( xPX >= Thumbnail_x1[xThumbnail] &&
				xPX <= Thumbnail_x2[xThumbnail] &&
				yPX >= Thumbnail_y1[xThumbnail] &&
				yPX <= Thumbnail_y2[xThumbnail] ){
				final Thumbnail tn = (Thumbnail)listThumbnails.get(xThumbnail - 1);
				if( evt.isControlDown() || evt.isShiftDown() ){
					vClickSelect(tn);
				} else if( ctClicks == 2 ){
					vClickPlot(tn);
				}
				break;
			}
		}
		mousePressed( evt );
	}

}

class Thumbnail {
	DodsURL url;
	Image image;
	boolean zSelected;
	String sCaption;
}


