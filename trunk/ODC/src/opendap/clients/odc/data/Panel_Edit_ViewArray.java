package opendap.clients.odc.data;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.plot.PlotOptions;

public class Panel_Edit_ViewArray extends javax.swing.JPanel {
	private BufferedImage mbi = null;
	private static boolean mzFatalError = false;
	
	public void paintComponent(Graphics g) {
		try {
			if( mbi != null ) ((Graphics2D)g).drawImage(mbi, null, 0, 0); // flip image to canvas
		} catch(Exception ex) {
			if( mzFatalError ) return; // got the training wheels on here todo
			mzFatalError = true;
			ApplicationController.vUnexpectedError(ex, "Error rendering plot image");
		}
		super.paintComponent(g);
	}

	private void vUpdateImage( int[][] ai, int xRow, int xColumn ){

		// standard scaled area
		int pxCanvasWidth = this.getWidth();
		int pxCanvasHeight = this.getHeight();

		if( mbi == null ){
			mbi = new BufferedImage( pxCanvasWidth, pxCanvasHeight, BufferedImage.TYPE_INT_ARGB );
		}
		Graphics2D g2 = (Graphics2D)mbi.getGraphics();
		repaint();
	}

}
