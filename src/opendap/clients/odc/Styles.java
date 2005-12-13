package opendap.clients.odc;

/**
 * Title:        Styles
 * Description:  Standard borders and fonts
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.43
 */

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



