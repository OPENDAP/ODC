package opendap.clients.odc.data;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.plot.PlotOptions;

public class Panel_Edit_ViewArray extends javax.swing.JPanel {
	private BufferedImage mbi = null;
	private static boolean mzFatalError = false;
	private Font fontHeader = new Font( "LucidaBrightDemiBold", Font.PLAIN, 12 );
	private Font fontValue = new Font( "LucidaBrightDemiBold", Font.PLAIN, 12 );
	
	public void paintComponent( Graphics g ){
		try {
			if( mbi != null ) ((Graphics2D)g).drawImage(mbi, null, 0, 0); // flip image to canvas
		} catch(Exception ex) {
			if( mzFatalError ) return; // got the training wheels on here todo
			mzFatalError = true;
			ApplicationController.vUnexpectedError(ex, "Error rendering plot image");
		}
		super.paintComponent(g);
	}

	private void vUpdateImage( int[][] ai, int xD1, int xD2 ){

		// standard scaled area
		int pxCanvasWidth = this.getWidth();
		int pxCanvasHeight = this.getHeight();

		if( mbi == null ){
			mbi = new BufferedImage( pxCanvasWidth, pxCanvasHeight, BufferedImage.TYPE_INT_ARGB );
		}
		Graphics2D g2 = (Graphics2D)mbi.getGraphics();
		int pxCellWidth = 80;
		int pxCellHeight = 20;
		
		int ctRows = ai.length;
		int ctColumns = ai[0].length;
		
		// draw column headers centered
		g2.setFont( fontHeader );
		FontMetrics fmHeader = g2.getFontMetrics( fontHeader );
		int pxHeaderFontHeight = fmHeader.getAscent() + fmHeader.getLeading(); // digits do not have descents
		int offsetY = (pxCellHeight - pxHeaderFontHeight) / 2; 
		int posCell_x = 0;
		int posCell_y = 0;
		posCell_x += pxCellWidth; // advance cursor past row header
		for( int xColumn = xD2; xColumn <= ctColumns; xColumn++ ){
			String sHeaderText = Integer.toString( xColumn );
			int iStringWidth = fmHeader.stringWidth( sHeaderText );
			int offsetX = (pxCellWidth - iStringWidth ) / 2;
			g2.drawString( sHeaderText, posCell_x + offsetX, posCell_y + offsetY );
			posCell_x += pxCellWidth; // advance to next cell
			if( posCell_x > pxCanvasWidth ) break; // not enough canvas to draw all the columns
		}

		// draw row headers centered
		posCell_x = 0; // start at lefthand edge of screen
		posCell_y += pxCellHeight; // advance cursor past column header
		for( int xRow = xD1; xRow <= ai.length; xRow++ ){
			String sHeaderText = Integer.toString( xRow );
			int iStringWidth = fmHeader.stringWidth( sHeaderText );
			int offsetX = (pxCellWidth - iStringWidth ) / 2;
			g2.drawString( sHeaderText, posCell_x + offsetX, posCell_y + offsetY );
			posCell_y += pxCellHeight; // advance to next cell
			if( posCell_y > pxCanvasHeight ) break; // not enough canvas to draw all the columns
		}

		// draw cell values
		g2.setFont( fontValue );
		FontMetrics fmValue = g2.getFontMetrics( fontValue );
		int pxValueFontHeight = fmValue.getAscent() + fmValue.getLeading(); // digits do not have descents
		for( int xRow = xD1; xRow <= ctRows; xRow++ ){
			for( int xColumn = xD2; xColumn <= ai[0].length; xColumn++ ){
			}
		}
		
		// draw grid lines
		
		repaint();
	}

}
