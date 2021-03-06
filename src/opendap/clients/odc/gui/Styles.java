package opendap.clients.odc.gui;

/**
 * Title:        Styles
 * Description:  Standard borders and fonts
 * Copyright:    Copyright (c) 2002-2012
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      3.08
 */

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

import java.awt.Font;
import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.border.Border;

public class Styles {
    public final static int STYLE_NetworkAction = 1;
    public final static int STYLE_Terminal = 2;
    public final static int STYLE_BigActionButton = 3;
    public final static int STYLE_BigBlueButton = 4;
    public final static int STYLE_RedWarning = 5;
    public final static Font fontSerifItalic18 = new Font("Serif", Font.ITALIC, 18);
    public final static Font fontSansSerifBold12 = new Font("Sans Serif", Font.BOLD, 12);
    public final static Font fontFixed10 = new Font("Monospaced", Font.PLAIN, 10);
    public final static Font fontFixed12 = new Font("Monospaced", Font.PLAIN, 12);
	public final static Font fontSansSerif8 = new Font("Sans Serif", Font.PLAIN, 8);
	public final static Font fontSansSerif10 = new Font("Sans Serif", Font.PLAIN, 10);
	public final static Font fontSansSerif12 = new Font("Sans Serif", Font.PLAIN, 12);
	public final static Font fontSansSerif14 = new Font("Sans Serif", Font.PLAIN, 14);
	public final static Font fontSansSerif18 = new Font("Sans Serif", Font.PLAIN, 18);
	public final static Font fontSansSerifBold10 = new Font("Sans Serif", Font.BOLD, 10);
	public final static Font fontLucidaBrightDemibold = new Font( "Lucida Bright Demibold", Font.PLAIN, 12 ); 
	public final static Font fontLucidaBrightItalic = new Font( "Lucida Bright Italic", Font.PLAIN, 12 ); 
	public final static Font fontLucidaBrightRegular = new Font( "Lucida Bright Regular", Font.PLAIN, 12 ); 
	public final static Font fontLucidaConsole = new Font( "Lucida Console", Font.PLAIN, 12 ); 
	public final static Font fontLucidaSansDemibold = new Font( "Lucida Sans Demibold", Font.PLAIN, 12 ); 
	public final static Font fontLucidaSansDemiboldItalic = new Font( "Lucida Sans Demibold Italic", Font.PLAIN, 12 ); 
	public final static Font fontLucidaSansDemiboldRoman = new Font( "Lucida Sans Demibold Roman", Font.PLAIN, 12 ); 
	public final static Font fontLucidaSansItalic = new Font( "Lucida Sans Italic", Font.PLAIN, 12 ); 
	public final static Font fontLucidaSansRegular = new Font( "Lucida Sans Regular", Font.PLAIN, 12 ); 		
	public final static Font fontLucidaSansTypewriterBold = new Font( "Lucida Sans Typewriter Bold", Font.PLAIN, 12 ); 
	public final static Font fontLucidaSansTypewriterBoldOblique = new Font( "Lucida Sans Typewriter Bold Oblique", Font.PLAIN, 12 ); 
	public final static Font fontLucidaSansTypewriterOblique = new Font( "Lucida Sans Typewriter Oblique", Font.PLAIN, 12 ); 
	public final static Font fontLucidaSansTypewriterRegular = new Font( "Lucida Sans Typewriter Regular", Font.PLAIN, 12 ); 
	public final static Font fontLucidaSansUnicode = new Font( "Lucida Sans Unicode", Font.PLAIN, 12 ); 
	
	public final static Color colorNeutralYellow1 = new Color(0xFFEBCD);
	public final static Color colorLightGray = new Color(0xD3D3D3);
	public final static Color colorCyanHighlight = new Color(0x8040FF);
	public final static Border BORDER_Blue = new javax.swing.border.LineBorder(Color.blue);

    public Styles(){}
    public static void vApply( int STYLE, JComponent component ){
        switch( STYLE ){
            case STYLE_NetworkAction:
                component.setForeground(Color.red);
                component.setFont(fontSansSerifBold12);
				break;
            case STYLE_Terminal:
                component.setForeground(Color.black);
                component.setFont(fontFixed12);
				break;
            case STYLE_BigActionButton:
                component.setForeground(Color.black);
                component.setFont(fontSansSerifBold12);
				break;
            case STYLE_BigBlueButton:
                component.setForeground(Color.blue);
                component.setFont(fontSansSerifBold12);
				break;
            case STYLE_RedWarning:
                component.setForeground(Color.red);
                component.setFont(fontSansSerifBold12);
				break;
        }
    }
}

//		g2.setFont( new Font( "Lucida Bright Demibold", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Bright Demibold", 50, 50 ); 
//		g2.setFont( new Font( "Lucida Bright Italic", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Bright Italic", 50, 75 ); 
//		g2.setFont( new Font( "Lucida Bright Regular", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Bright Regular", 50, 100 ); 
//		g2.setFont( new Font( "Lucida Console", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Console", 50, 125 ); 
//		g2.setFont( new Font( "Lucida Sans Demibold", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Demibold", 50, 150 ); 
//		g2.setFont( new Font( "Lucida Sans Demibold Italic", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Demibold Italic", 50, 175 ); 
//		g2.setFont( new Font( "Lucida Sans Demibold Roman", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Demibold Roman", 50, 200 ); 
//		g2.setFont( new Font( "Lucida Sans Italic", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Italic", 50, 225 ); 
//		g2.setFont( new Font( "Lucida Sans Regular", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Regular", 50, 250 ); 		
//		g2.setFont( new Font( "Lucida Sans Typewriter Bold", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Typewriter Bold", 50, 275 ); 
//		g2.setFont( new Font( "Lucida Sans Typewriter Bold Oblique", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Typewriter Bold Oblique", 50, 300 ); 
//		g2.setFont( new Font( "Lucida Sans Typewriter Oblique", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Typewriter Oblique", 50, 325 ); 
//		g2.setFont( new Font( "Lucida Sans Typewriter Regular", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Typewriter Regular", 50, 350 ); 
//		g2.setFont( new Font( "Lucida Sans Unicode", Font.PLAIN, 12 ) ); g2.drawString( "0123456789 Lucida Sans Unicode", 50, 375 ); 


