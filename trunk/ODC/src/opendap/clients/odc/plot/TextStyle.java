package opendap.clients.odc.plot;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import opendap.clients.odc.gui.Styles;
import opendap.clients.odc.plot.PlotLayout.LayoutStyle;

public class TextStyle {
	static Font[] FONT_Defaults = new Font[7];
	static {
		FONT_Defaults[0] = Styles.fontFixed12;
		FONT_Defaults[PlotLayout.RELATIVE_OBJECT.Canvas.ordinal() + 1] = Styles.fontSansSerif14;
		FONT_Defaults[PlotLayout.RELATIVE_OBJECT.Plot.ordinal() + 1] = Styles.fontSansSerif14;
		FONT_Defaults[PlotLayout.RELATIVE_OBJECT.AxisHorizontal.ordinal() + 1] = Styles.fontSansSerif12;
		FONT_Defaults[PlotLayout.RELATIVE_OBJECT.AxisVertical.ordinal() + 1] = Styles.fontSansSerif12;
		FONT_Defaults[PlotLayout.RELATIVE_OBJECT.Legend.ordinal() + 1] = Styles.fontSansSerif10;
		FONT_Defaults[PlotLayout.RELATIVE_OBJECT.Scale.ordinal() + 1] = Styles.fontSansSerif8;
	}
	static Color COLOR_Default = Color.BLACK;
	private LayoutStyle layout_style = LayoutStyle.Axis_X;
	private final PlotLayout mLayout = PlotLayout.create( layout_style );
	private Font mFont;
	private Color mColor;
	private int mColor_HSB = 0xFFFFFF00;
	TextStyle(){
		mFont = null;
		mColor = null;
	}
	public static TextStyle create( LayoutStyle ls ){
		TextStyle ts = new TextStyle();
		ts.layout_style = ls;
		return ts;
	}
	PlotLayout getPlotLayout(){ return mLayout; }
	Font getFont(){ if( mFont == null ) return FONT_Defaults[mLayout.getObject().ordinal() + 1]; else return mFont; }
	Color getColor(){ if( mColor == null ) return COLOR_Default; else return mColor; }
	int getColor_HSB(){ return mColor_HSB; }
	void setFont( Font f ){ mFont = f; }
	void setColor( int hsb ){
		mColor_HSB = hsb;
		mColor = new Color( Color_HSB.iHSBtoRGBA(hsb), true);
	}
	
	/** Orthogonal distance from the plot layout baseline to the top of the text */
	public int getBaseLineOffset_top( Graphics2D g2 ){
		return 0;
	}

	/** Orthogonal distance from the plot layout baseline to the bottom of the text */
	public int getBaseLineOffset_bottom( Graphics2D g2 ){
		return 0;
	}
	public int getTextWidthX_pixels( Graphics2D g2, String sText ){
		if( sText == null ) return 0;
		FontMetrics fm = g2.getFontMetrics( getFont() );
		Rectangle2D rect = fm.getStringBounds( sText, g2 );
		return (int)rect.getWidth();
	}
	public int getTextHeightY_pixels( Graphics2D g2, String sText ){
		if( sText == null ) return 0;
		FontMetrics fm = g2.getFontMetrics( getFont() );
		Rectangle2D rect = fm.getStringBounds( sText, g2 );
		return (int)rect.getHeight();
	}
}

