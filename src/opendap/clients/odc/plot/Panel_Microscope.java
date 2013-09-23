/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.clients.odc.plot;

/**
 * Title:        Panel_Microscope
 * Description:  Support for a data microscope
 * Copyright:    Copyright (c) 2003
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.07
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.gui.Styles;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;

public class Panel_Microscope extends JPanel implements MouseListener {

	final public static Panel_Microscope microscope = new Panel_Microscope();
	public static JFrame frameMicroscope = null;
	public static Panel_Rendering panelHost = null;
	
	BufferedImage mbi = null;
	int mpxCanvasWidth;
	int mpxCanvasHeight;
	Color mcolorBackground = Color.WHITE;
	Graphics2D g2 = null;
	FontMetrics mfontmetricsSansSerif10 = null;
	FontMetrics mfontmetricsSansSerifBold12 = null;
	int mpxTextBoldHeight = 0;
	int mpxTextBoldAscent = 0;
	int mpxTextHeight = 0;
	int mpxTextAscent = 0;
	int mpxCellWidth = 0;
	int mpxFFBold12Width = 0;
	int mpxMarginLeft = 20;
	int mpxMarginTop = 20;
	int mpxInset = 7;
	int mpxLabelWidth_color;
	int mpxTestSelect_width = 50;
	int mpxTestSelect_height = 20;
	int mhsbSelect = 0xFF0000FF;
	int mhsbCompare = 0xFF000000;
	int miAlphaSelect = 0xFF;
	int miHueSelect = 0;
	int miSatSelect = 0;
	int miBriSelect = 0xFF;
	Color colorSelect = Color.WHITE;
	Color colorCompare = Color.BLACK;
	Color[] colorHue;
	Color[] colorSat;
	Color[] colorBri;
	Color[] colorAlpha;
	int mxHueGrid;
	int myHueGrid;
	int myHueHeader;
	int mhOutlineBoxes;
	int mxColorSquare;
	int myColorSquare;
	int mpxHue_Left;
	int mpxSaturation_Left;
	int mpxBrightness_Left;
	int mpxAlpha_Left;
	int mpxHueWidth;
	int mpxSatWidth;
	int mpxBriWidth;
	int mpxAlphaWidth;
	int mxSatLabels;
	int mxBriLabels;
	int mxAlphaLabels;
	int mxSatSwatch;
	int mxBriSwatch;
	int mxAlphaSwatch;
	int mxFooterOrigin;
	int myFooterOrigin;
	int mxColorLabel;
	GeneralPath mshapeIndicator;

	int mxWidth = 0;
	int mxHeight = 0;
	Color[][] maColors = new Color[7][7];
	String[][] masValues = new String[7][7];
	String[][] masColors = new String[7][7];

	private Panel_Microscope(){
		mbi = new BufferedImage( 10, 10, BufferedImage.TYPE_INT_ARGB ); // smaller buffer just used to get sizes
		g2 = (Graphics2D)mbi.getGraphics();
		mfontmetricsSansSerif10 = g2.getFontMetrics(Styles.fontSansSerif10);
		mfontmetricsSansSerifBold12 = g2.getFontMetrics(Styles.fontSansSerifBold12);
		mpxTextHeight = mfontmetricsSansSerif10.getHeight() + 2;
		mpxTextAscent = mfontmetricsSansSerif10.getAscent();
		mpxTextBoldHeight = mfontmetricsSansSerifBold12.getHeight();
		mpxTextBoldAscent = mfontmetricsSansSerifBold12.getAscent();
		mpxCellWidth = 75;

		// dimensional calculations
		mpxCanvasHeight = mpxCellWidth * 6;
		mpxCanvasWidth = mpxCellWidth * 6;

		this.setPreferredSize(new Dimension(mpxCanvasWidth, mpxCanvasHeight));
		mbi = new BufferedImage( mpxCanvasWidth, mpxCanvasHeight, BufferedImage.TYPE_INT_ARGB );
		g2 = (Graphics2D)mbi.getGraphics();
		addMouseListener(this);

	}

	public static void _activate( Panel_Rendering host, int[][] aRGB, String[][] asData, int iMicroscopeWidth, int iMicroscopeHeight ){
		microscope._set( aRGB, asData, iMicroscopeWidth, iMicroscopeHeight );
		panelHost = host;
		if( frameMicroscope == null ){
			frameMicroscope = new JFrame();
			frameMicroscope.add( microscope );
			WindowListener listenerCloser = new WindowAdapter(){
				public void windowClosing(WindowEvent e){
					Panel_Microscope.panelHost.deactivateMicroscope();
				}
			};
			frameMicroscope.addWindowListener( listenerCloser );
		}
		frameMicroscope.setTitle( "Data Microscope: " + panelHost.getCurrentComposition()._getCaption() );
		frameMicroscope.setVisible( true );
		frameMicroscope.pack();
	}
	
	public static void _deactivate(){
		if( frameMicroscope != null ) frameMicroscope.setVisible( false );
	}
	
	StringBuffer sb = new StringBuffer(80);
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g2.setColor(mcolorBackground);
        g2.fillRect(0,0,mpxCanvasWidth,mpxCanvasHeight); // draw background

		// draw color boxes
		for( int x = 1; x <= mxWidth; x++ ){
			for( int y = 1; y <= mxHeight; y++ ){
				g2.setColor(maColors[x][y]);	
				g2.fillRect((x - 1)*mpxCellWidth, (y - 1)*mpxCellWidth, mpxCellWidth-1, mpxCellWidth-1);
			}
		}

		// draw data labels
		g2.setColor(Color.white);
//		g2.setColor(Color.black);
		g2.setFont(Styles.fontSansSerifBold12);
		for( int x = 1; x <= mxWidth; x++ ){
			for( int y = 1; y <= mxHeight; y++ ){
				String s = masValues[x][y];
				if( s == null ) continue;
				int iWidth = mfontmetricsSansSerifBold12.stringWidth(s);
				int iOffset = (mpxCellWidth - iWidth)/2;
				g2.drawString(s, (x - 1)*mpxCellWidth + iOffset, (y - 1)*mpxCellWidth + mpxCellWidth/2 - mpxTextBoldAscent);
			}
		}

		// draw labels
//		g2.setColor(Color.black);
//		g2.setColor(Color.white);
//		g2.setFont(Styles.fontSansSerifBold12);
//		for( int x = 1; x <= 6; x++ ){
//			for( int y = 1; y <= 6; y++ ){
//				String s = masValues[x][y];
//				if( s == null ) continue;
//				int iWidth = mfontmetricsSansSerifBold12.stringWidth(s);
//				int yText = (y - 1)*mpxCellWidth + mpxCellWidth/2 - mpxTextBoldAscent;
//				int iOffset;
//				if( iWidth < mpxCellWidth - 6 ){
//					iOffset = (mpxCellWidth - iWidth)/2;
//					g2.drawString(s, (x - 1)*mpxCellWidth + iOffset, yText);
//				} else { // split data string in half
//					String s1 = s.substring(0, 8);
//					String s2 = s.substring(8);
//					int iNewWidth = mfontmetricsSansSerifBold12.stringWidth(s);
//					iOffset = (mpxCellWidth - iNewWidth)/2;
//					g2.drawString(s1, (x - 1)*mpxCellWidth + iOffset, yText);
//					yText +=  mpxTextHeight + 2;
//					g2.drawString(s2, (x - 1)*mpxCellWidth + iOffset, yText);
//				}

				// draw color label
//				String sColor = masColors[xRow][xColumn];
//				iWidth = mfontmetricsSansSerifBold12.stringWidth(sColor);
//				iOffset = (mpxCellWidth - iWidth)/2;
//				yText +=  mpxTextHeight + 4;
//				g2.drawString(sColor, (xColumn - 1)*mpxCellWidth + iOffset, yText);
//			}
//		}

		((Graphics2D)g).drawImage(mbi, null, 0, 0); // flip mbi (the draw target) to the active graphics
	}

	public void _set( int[][] rgbRaster0, String[][] asValues0, int iWidth, int iHeight ){
		mxWidth = iWidth;
		mxHeight = iHeight;
		maColors = new Color[mxWidth + 1][mxHeight + 1];
		masValues = new String[mxWidth + 1][mxHeight + 1];
		masColors = new String[mxWidth + 1][mxHeight + 1];
		mpxCanvasHeight = mpxCellWidth * iHeight;
		mpxCanvasWidth = mpxCellWidth * iWidth;
		mbi = new BufferedImage( mpxCanvasWidth, mpxCanvasHeight, BufferedImage.TYPE_INT_ARGB );
		g2 = (Graphics2D)mbi.getGraphics();
		this.setPreferredSize( new Dimension( mpxCanvasWidth, mpxCanvasHeight ) );
		_update( rgbRaster0, asValues0 );
	}

	public void _update( int[][] rgbRaster0, String[][] asValues0 ){
		for( int x = 0; x < mxWidth; x++ ){
			for( int y = 0; y < mxHeight; y++ ){
				maColors[x + 1][y + 1] = new Color( rgbRaster0[x][y], true );
				masValues[x + 1][y + 1] = asValues0[x][y];
				masColors[x + 1][y + 1] = Utility_String.sToHex( Color_HSB.iRGBtoHSB(rgbRaster0[x][y]), 8);
			}
		}
		this.repaint();
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
	public void mouseClicked(MouseEvent evt){
	}
}



