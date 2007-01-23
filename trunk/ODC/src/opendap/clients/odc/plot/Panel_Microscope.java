package opendap.clients.odc.plot;

/**
 * Title:        Panel_Microscope
 * Description:  Support for a data microscope
 * Copyright:    Copyright (c) 2003
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.25
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Styles;
import javax.swing.JPanel;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;

public class Panel_Microscope extends JPanel implements MouseListener {
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

	Color[][] maColors = new Color[7][7];
	String[][] masValues = new String[7][7];
	String[][] masColors = new String[7][7];

	public Panel_Microscope(){
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

	StringBuffer sb = new StringBuffer(80);
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g2.setColor(mcolorBackground);
        g2.fillRect(0,0,mpxCanvasWidth,mpxCanvasHeight); // draw background

		// draw color boxes
		for( int xRow = 1; xRow <= 6; xRow++ ){
			for( int xColumn = 1; xColumn <= 6; xColumn++ ){
				g2.setColor(maColors[xRow][xColumn]);
				g2.fillRect((xRow - 1)*mpxCellWidth, (xColumn - 1)*mpxCellWidth, mpxCellWidth-1, mpxCellWidth-1);
			}
		}

		// draw data labels
		g2.setColor(Color.black);
		g2.setFont(Styles.fontSansSerifBold12);
		for( int xRow = 1; xRow <= 6; xRow++ ){
			for( int xColumn = 1; xColumn <= 6; xColumn++ ){
				String s = masValues[xRow][xColumn];
				int iWidth;
				if( s == null ){
					iWidth = iWidth = mfontmetricsSansSerifBold12.stringWidth("null");
				} else {
					iWidth = mfontmetricsSansSerifBold12.stringWidth(s);
				}
				int iOffset = (mpxCellWidth - iWidth)/2;
				g2.drawString(s, (xColumn - 1)*mpxCellWidth + iOffset, (xRow - 1)*mpxCellWidth + mpxCellWidth/2 - mpxTextBoldAscent);
			}
		}

		// draw labels
		g2.setColor(Color.black);
		g2.setFont(Styles.fontSansSerifBold12);
		for( int xRow = 1; xRow <= 6; xRow++ ){
			for( int xColumn = 1; xColumn <= 6; xColumn++ ){
				String s = masValues[xRow][xColumn];
				int iWidth = mfontmetricsSansSerifBold12.stringWidth(s);
				int yText = (xRow - 1)*mpxCellWidth + mpxCellWidth/2 - mpxTextBoldAscent;
				int iOffset;
				if( iWidth < mpxCellWidth - 6 ){
					iOffset = (mpxCellWidth - iWidth)/2;
					g2.drawString(s, (xColumn - 1)*mpxCellWidth + iOffset, yText);
				} else { // split data string in half
					String s1 = s.substring(0, 8);
					String s2 = s.substring(8);
					int iNewWidth = mfontmetricsSansSerifBold12.stringWidth(s);
					iOffset = (mpxCellWidth - iNewWidth)/2;
					g2.drawString(s1, (xColumn - 1)*mpxCellWidth + iOffset, yText);
					yText +=  mpxTextHeight + 2;
					g2.drawString(s2, (xColumn - 1)*mpxCellWidth + iOffset, yText);
				}

				// draw color label
//				String sColor = masColors[xRow][xColumn];
//				iWidth = mfontmetricsSansSerifBold12.stringWidth(sColor);
//				iOffset = (mpxCellWidth - iWidth)/2;
//				yText +=  mpxTextHeight + 4;
//				g2.drawString(sColor, (xColumn - 1)*mpxCellWidth + iOffset, yText);
			}
		}

		((Graphics2D)g).drawImage(mbi, null, 0, 0); // flip mbi (the draw target) to the active graphics
	}

	public void set( int[] rgbRaster, String[] asValues ){
		for( int xRow = 1; xRow <= 6; xRow++ ){
			for( int xColumn = 1; xColumn <= 6; xColumn++ ){
				int iRasterIndex = 6 * (xRow-1) + (xColumn-1);
				maColors[xRow][xColumn] = new Color(rgbRaster[iRasterIndex]);
				masValues[xRow][xColumn] = asValues[iRasterIndex];
				masColors[xRow][xColumn] = Utility.sToHex(ColorSpecification.iRGBtoHSB(rgbRaster[iRasterIndex]), 8);
			}
		}
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



