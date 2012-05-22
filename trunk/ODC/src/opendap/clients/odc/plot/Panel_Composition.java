package opendap.clients.odc.plot;

/**
 * Title:        Panel_Plot
 * Description:  Base class for plotting
 * Copyright:    Copyright (c) 2002-12
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.08
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.DAP;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.gui.Resources;
import opendap.clients.odc.gui.Styles;
import opendap.clients.odc.plot.Output_ToPlot.OutputTarget;

import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.awt.print.*;

import javax.swing.*;

import java.util.ArrayList;

class Panel_Composition extends JPanel implements Printable, MouseListener, MouseMotionListener, Scrollable {

	Composition composition = null;
	public final static boolean DEBUG_Layout = false;
	public final static double TwoPi = 2d * 3.14159d;
	public final static String TEXT_ID_CaptionColorBar = "CaptionColorbar";

	private Panel_Composition(){} // see create() method

	public static void main(String[] args) {
		StringBuffer sbError = new StringBuffer();
		Frame frame = new Frame("Plot Demo");
		frame.setSize( 600, 600 );
		PlotScale scale = PlotScale.create();
		String sID = "demo_id";
		String sCaption = "demo plot";
		PlotEnvironment environment = new PlotEnvironment();
		environment.getScale().setOutputTarget( OutputTarget.NewWindow );
		environment.getScale().setDataDimension( 600, 600 );
		Plot_Surface demo_surface = Plot_Surface.create( environment );
		Panel_Composition panel = Panel_Composition._create( sbError );
		if( panel == null ){
			System.err.println( "plot failed: " + sbError.toString() );
		}
		frame.add( panel );
		frame.addWindowListener( new WindowAdapter(){
			public void windowClosing(WindowEvent e) {
				new Thread(new Runnable() {
					public void run() {
						System.exit(0);
					}
				}).start();
			}
		});
		frame.setVisible( true );
	}
	
	public static Panel_Composition _create( StringBuffer sbError ){
		Panel_Composition panel = new Panel_Composition();
		panel.addMouseListener( panel );
		panel.addMouseMotionListener( panel );
		return panel;
	}

	// this is needed to tell any container how big the panel wants to be
	// the preferred values should be set whenever an image is generated (see vGenerateImage)
	protected int mpxPreferredWidth = 250;
	protected int mpxPreferredHeight = 250;
    public Dimension getPreferredSize() {
		return new Dimension( mpxPreferredWidth, mpxPreferredHeight );
    }

	// the way printing works is that the printer keeps asking for pages and when you return no_such_page it stops
	// in the current implementation the Java printer always asks for the same page twice (with different
	// affine transforms); to deal with this the mazPagePrinted array is used, if a page is marked as
	// printed it is not printed again
	boolean[] mazPagePrinted = new boolean[2];
	public int print( Graphics g, PageFormat page_format, int page_index ){
		if( page_index < 0 || page_index > 1 ){ // there will always be a page n+1
			return java.awt.print.Printable.NO_SUCH_PAGE;
		}
		if( mazPagePrinted[page_index] ) return java.awt.print.Printable.NO_SUCH_PAGE; // already printed this page
		try {
			// todo use black and white / color models for bw printers
			((Graphics2D)g).drawImage( composition.getBuffer(), null, 0, 0); // flip image to printer
			return java.awt.print.Printable.PAGE_EXISTS;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, "While trying to print");
			return java.awt.print.Printable.NO_SUCH_PAGE;
		}
	}

	static boolean mzFatalError = false;
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		try {
			if( composition != null && composition.getBuffer() != null ) ((Graphics2D)g).drawImage( composition.getBuffer(), null, 0, 0); // flip image to canvas
		} catch( Exception ex ){
			if( mzFatalError ) return; // got the training wheels on here todo
			mzFatalError = true;
			ApplicationController.vUnexpectedError(ex, "Error rendering plot image");
		}
	}

	// draws a full rendering of annotations and one or more plots
	public boolean _zRenderComposition( Composition composition, StringBuffer sbError ){
		for( Plot plot : composition.getPlotList() ){
			if( ! _zPlot( plot, sbError ) ){
				sbError.append( "error rendering plot " +  plot.getDescriptor() );
				return false;
			}
		}
		vAnnotateComposition(); 
		return true;
	}
	
	// draws a single plot into the designated plot area
	public boolean _zPlot( Plot plot, BufferedImage bi, StringBuffer sbError ){
		if( plot == null ){
			sbError.append( "no plot supplied" );
			return false;
		}
		try {
			int iDataPoint_width = plot.data.getDimension_x();
			int iDataPoint_height = plot.data.getDimension_y();
			PlotScale scale = plot.environment.getScale();
			scale.setDataDimension( iDataPoint_width, iDataPoint_height );
			this.invalidate(); // when the canvas dimensions change, the layout is invalidated
			
			// determine offset
			int pxOffset_left = scale.getMarginLeft_px();
			int pxOffset_top  = scale.getMarginTop_px();
			plot.layout.setOffsetHorizontal( pxOffset_left );
			plot.layout.setOffsetVertical( pxOffset_top );
			
			// standard scaled area
			int pxPlotWidth = scale.getPlot_Width_pixels();
			int pxPlotHeight = scale.getPlot_Height_pixels();	
			if( pxPlotWidth == 0 || pxPlotHeight == 0 ){
				sbError.append( "invalid scale, plot width/height cannot be <= 0" );
				return false;
			}
			
			return _zWritePlotToImageBuffer( plot, bi, pxOffset_left, pxOffset_top, pxPlotWidth, pxPlotHeight, sbError );
		} catch( Throwable ex ){
			ApplicationController.vUnexpectedError( ex, sbError );
			return false;
		}
	}

	public boolean _zWritePlotToImageBuffer( Plot plot, BufferedImage bi, int pxOffset_left, int pxOffset_top, int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){
		if( bi == null ){
			sbError.append( "internal error, no buffer (out of memory?)" );
			return false;
		} else {
			int[] raster = plot.render( pxPlotWidth, pxPlotHeight, sbError );
			if( raster == null ){
				sbError.insert( 0, "failed to render plot: " );
				return false;
			}
			bi.setRGB( pxOffset_left, pxOffset_top, pxPlotWidth, pxPlotHeight, raster, 0, pxPlotWidth );
			return true;
		}
	}

//	int getCanvasWidth(){ return this.mpxCanvasWidth; }
//	int getCanvasHeight(){ return this.mpxCanvasHeight; }
//	int getMarginPixels_Top(){ return mpxMargin_Top; }
//	int getMarginPixels_Left(){ return mpxMargin_Left; }
//	int getMarginPixels_Bottom(){ return mpxMargin_Bottom; }
//	int getMarginPixels_Right(){ return mpxMargin_Right; }
//	void setMarginPixels_Top( int iPixels ){ mpxMargin_Top =  iPixels; }
//	void setMarginPixels_Bottom( int iPixels ){ mpxMargin_Bottom =  iPixels; }
//	void setMarginPixels_Left( int iPixels ){ mpxMargin_Left =  iPixels; }
//	void setMarginPixels_Right( int iPixels ){ mpxMargin_Right =  iPixels; }

	private String msLabel_HorizontalAxis = null;
	private String msLabel_VerticalAxis = null;
	private String msLabel_Title = null;
	private String msLabel_Values = null;
	private String msLabel_Units = null;
	void setLabel_HorizontalAxis( String sText ){ msLabel_HorizontalAxis = sText; }
	void setLabel_VerticalAxis( String sText ){ msLabel_HorizontalAxis = sText; }
	void setLabel_Title( String sText ){ msLabel_Title = sText; }
	void setLabel_Values( String sText ){ msLabel_Values = sText; }
	void setLabel_Units( String sText ){ msLabel_Units = sText; }

	// colors
	protected Color mcolorBackground = Color.WHITE;  // todo make into a parameter
	void setColor_Background( Color color ){ mcolorBackground = color; }
	Color getColor_Background(){ return mcolorBackground; }
	void setColors( ColorSpecification cs ){ mColors = cs; }

	// legend parameters
	private boolean mzHasLegend = true;
	private int mpxLegendHeight = 0;
	private int mpxLegendWidth = 0;
	private int mpxLegendKeyWidth = 10;

	PlotText mText = null;
	public PlotText getText(){ return mText; }
	void setText( PlotText pt ){
		mText = pt;
	}

	private PlotOptions mOptions = null;
	protected PlotOptions getPlotOptions(){ return mOptions; }
	void setOptions( PlotOptions po ){ mOptions = po; }

	int mpxAxis_Horizontal_X, mpxAxis_Horizontal_Y, mpxAxis_Horizontal_width, mpxAxis_Horizontal_height;
	int mpxAxis_Vertical_X, mpxAxis_Vertical_Y, mpxAxis_Vertical_width, mpxAxis_Vertical_height;

	protected void vAnnotateComposition( Graphics2D g2 ){
	}
	
	
	// the order of the annotations is important because they can be positioned relative to each other
	protected void vDrawAnnotations( Graphics2D g2, int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
		vDrawCoastline( g2, pxPlotWidth, pxPlotHeight);
		vDrawBorders( g2, pxPlotWidth, pxPlotHeight);
		vDrawAxes( g2, pxPlotWidth, pxPlotHeight);
		vDrawLegend( g2, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight );
		vDrawScale( g2, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight );
		vDrawText( g2, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight );
	}

	private void vDrawCoastline( Graphics2D g2, int pxPlotWidth, int pxPlotHeight ){
		if( mOptions != null ){
			if( !mOptions.getValue_boolean(PlotOptions.OPTION_ShowCoastLine) ) return;
		}
		if( mProjection == null ){
			ApplicationController.vShowWarning("unable to draw coastline, no geographic projection defined");
			return;
		} else {
			mGeoReference = new GeoReference();
			StringBuffer sbError = new StringBuffer(80);
			double[] adLatitudeMapping = mProjection.getMapping_Latitude();
			double[] adLongitudeMapping = mProjection.getMapping_Longitude();
			if( ! mGeoReference.zInitialize( adLongitudeMapping, adLatitudeMapping, GeoReference.TYPE_IntegerG0, sbError) ){
				ApplicationController.vShowWarning("unable to draw coastline: " + sbError);
				return;
			}
		}
		int xPlotArea_LowerLeft = 10; // mpxMargin_Left;
		int yPlotArea_LowerLeft = 10; // mpxMargin_Top + pxPlotHeight;
		Coastline.vDrawCoastline(g2, xPlotArea_LowerLeft, yPlotArea_LowerLeft, pxPlotWidth, pxPlotHeight, mGeoReference);
	}

	protected void vDrawAxes( Graphics2D g2, PlotScale scale ){
		StringBuffer sbError = new StringBuffer();
		ArrayList<PlotAxis> listAxes = environment.getAxes()._getActive();
		for( int xAxis = 0; xAxis < listAxes.size(); xAxis++ ){
			PlotAxis axis = listAxes.get( xAxis );
			BufferedImage biAxis = axis.render( g2, mScale, sbError );
			if( biAxis == null  ){
				ApplicationController.vShowError_NoModal( "error rendering axis: " + sbError.toString() );
				continue;
			}
			// TODO compose buffer to canvas
		}	
	}

// fyi drawing a filled box
//g2.setColor(Color.BLACK);
//g2.drawRect(10, 10, 9, 9);
//g2.setColor(Color.CYAN);
//g2.fillRect(11, 11, 8, 8);
//g2.setColor(Color.BLACK);

	private void vDrawBorders( Graphics2D g2, int pxPlotWidth, int pxPlotHeight){
		if( mOptions.getValue_boolean( PlotOptions.OPTION_Boxed ) ){
			int xBox      = mpxMargin_Left;
			int yBox      = mpxMargin_Top;
			int widthBox  = pxPlotWidth + 20;
			int heightBox = pxPlotHeight + 20;
			g2.setColor(Color.black);
			g2.fillRect(xBox, yBox, widthBox - 1, 1); // top
			g2.fillRect(xBox, yBox, 1, heightBox - 1); // left
			g2.fillRect(xBox + widthBox - 1, yBox, 1, heightBox - 1); // right
			g2.fillRect(xBox, yBox + heightBox - 1, widthBox - 1, 1); // bottom
		}
	}

	// default rotation/size
	//	y-axis orientation: 270 degrees, size = y-axis length
	//	x-axis orientation: 0 degrees, size = x-axis length
	//	plot orientation, top/bottom alignment: 0 degrees, size = x-axis length
	//	plot orientation, left/right alignment: 270 degrees, size = y-axis length
	//	canvas orientation, top/bottom alignment: 0 degrees, size = canvas width
	//	canvas orientation, left/right alignment: 270 degrees, size = canvas height
	protected void vDrawLegend( Graphics2D g2, int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
		if( mOptions == null ) return;
		if( !mOptions.option_legend_zShow ) return;
		Font font = Styles.fontSansSerif10; // todo make settable font
		g2.setColor(Color.black); // todo settable colors
		int iDefaultLength = 0;
		int iRotation = mOptions.option_legend_Layout.getRotation();
		PlotLayout layout = mOptions.option_legend_Layout;
		int pxOffset_width = layout.getOffsetHorizontal();
		int pxOffset_height = layout.getOffsetVertical();
		int hObject, vObject, heightObject, widthObject;
		switch( layout.getObject() ){
			case Canvas:
				if( iRotation == 90 || iRotation == 270 ){
					iDefaultLength = pxCanvasHeight - 2 * pxOffset_height;
				} else {
					iDefaultLength = pxCanvasWidth - 2 * pxOffset_width;
				}
				hObject = 0;
				vObject = 0;
				heightObject = pxCanvasHeight;
				widthObject  = pxCanvasWidth;
				break;
			case Plot:
				if( iRotation == 90 || iRotation == 270 ){
					iDefaultLength = pxPlotHeight - 2 * pxOffset_height;
				} else {
					iDefaultLength = pxPlotWidth - 2 * pxOffset_width;
				}
				hObject = 10; // mpxMargin_Left;
				vObject = 10; // mpxMargin_Top;
				heightObject = pxPlotHeight;
				widthObject  = pxPlotWidth;
				break;
			case AxisVertical:
				iDefaultLength = mpxAxis_Vertical_height - 2 * pxOffset_height;
				hObject = mpxAxis_Vertical_X;
				vObject = mpxAxis_Vertical_Y;
				heightObject = mpxAxis_Vertical_height;
				widthObject  = mpxAxis_Vertical_width;
				break;
			default:
			case AxisHorizontal:
				iDefaultLength = mpxAxis_Horizontal_width - 2 * pxOffset_width;
				hObject = mpxAxis_Horizontal_X;
				vObject = mpxAxis_Horizontal_Y;
				heightObject = mpxAxis_Horizontal_height;
				widthObject  = mpxAxis_Horizontal_width;
				break;
		}
		if( iDefaultLength < 10 ) iDefaultLength = 10;
		int iLength = mOptions.option_legend_iSize;
		if( iLength == 0 ) iLength = iDefaultLength;

		// draw appropriate kind of legend
		// TODO
		/*
		if( plotPrimary instanceof Plot_Line ){
			vDrawLegend_Lines( g2, font, iLength, iRotation, layout, hObject, vObject, widthObject, heightObject );
		} else {
			vDrawLegend_Colorbar( g2, font, iLength, iRotation, layout, hObject, vObject, widthObject, heightObject );
		}
		*/
	}

	protected void vDrawLegend_Lines( Plot_Line plotLine, Graphics2D g2, Font font, int iLength, int iRotation, PlotLayout layout, int hObject, int vObject, int widthObject, int heightObject ){
		Color[] colors = plotLine.getColors();
		String[] captions = plotLine.getCaptions();
		int ctLines = colors.length - 1;
		int ctDefinedCaptions = 0;
		for( int xCaption = 1; xCaption <= ctLines; xCaption++ ){
			if( captions[xCaption] == null ){
				captions[xCaption] = "?";
			} else {
				ctDefinedCaptions++;
			}
		}
		if( ctDefinedCaptions == 0 ) return; // nothing to draw
		int pxMargin = 10;
		int pxLineSpacing = 6;
		int pxSampleWidth = 18;
		int pxSampleSpacing = 4;
		FontMetrics fm = g2.getFontMetrics(font);
		int pxAscent = fm.getAscent();
		if( colors == null || captions == null ) return;
		int pxCaptionMaxWidth = 0;
		for( int xCaption = 1; xCaption <= ctLines; xCaption++ ){
			if( fm.stringWidth(captions[xCaption]) > pxCaptionMaxWidth )
				pxCaptionMaxWidth = fm.stringWidth(captions[xCaption]);
		}
		int pxLegendWidth = pxMargin * 2 + pxSampleWidth + pxSampleSpacing + pxCaptionMaxWidth;
		int pxLegendHeight = pxMargin * 2 + pxAscent * ctLines + pxLineSpacing * (ctLines - 1);
		long nLocationPacked = layout.getLocation(pxLegendWidth, pxLegendHeight, hObject, vObject, widthObject, heightObject );
		int xLegend = (int)(nLocationPacked >> 32);
		int yLegend = (int)(nLocationPacked & 0xFFFFFFFF);

		// draw line details
		int xSample  = xLegend + pxMargin;
		int xCaption = xSample + pxSampleWidth + pxSampleSpacing;
		for( int xLine = 1; xLine <= ctLines; xLine++ ){
			g2.setColor(colors[xLine]);
			int yCaption = yLegend + pxMargin + pxAscent * xLine + pxLineSpacing * (xLine - 1);
			int ySample = yCaption - pxAscent/2;
			String sCaption = captions[xLine] == null ? "???" : (captions[xLine].length() == 0 ? "?" : captions[xLine]);
			g2.drawLine(xSample, ySample, xLegend + pxMargin + pxSampleWidth, ySample);
			g2.setColor(Color.BLACK);
			g2.drawString(sCaption, xCaption , yCaption);
		}

		// draw box around legend
		g2.setColor(Color.BLACK);
		g2.drawRect(xLegend, yLegend, pxLegendWidth, pxLegendHeight);
	}

	protected void vDrawLegend_Colorbar( Graphics2D g2, Font font, int iLength, int iRotation, PlotLayout layout, int hObject, int vObject, int widthObject, int heightObject ){
		// TODO (see dead code file)
	}

	protected void vDrawScale( Graphics2D g2, int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
		if( mOptions == null ) return;
		if( !mOptions.option_scale_zShow ) return;
		PlotLayout layout = mOptions.option_scale_Layout;
		Font font = Styles.fontSansSerif10; // todo make settable font
		int pxRuleThickness = 2;

		// calculate length of rule
		int iRealLength = mOptions.option_scale_iRealLength;
		int iXLength = mOptions.option_scale_iXLength;
		if( iRealLength < 1 ){
			if( iXLength < 1 ){
				iRealLength = 1;
				iXLength = 10;
			} else {
				iRealLength = iXLength / 10;
				if( iRealLength < 1 ) iRealLength = 1;
			}
		} else if( iXLength < 1 ){
			iXLength = 10 * iRealLength;
		}
		int pxRuleLength = mpxAxis_Horizontal_width * iRealLength / iXLength;

		// determine scale length and thickness
		String sLabel = mOptions.option_scale_sLabel;
		if( sLabel == null ) sLabel = "";
		FontMetrics mfontmetricsSansSerif10 = g2.getFontMetrics(Styles.fontSansSerif10);
		int iLabelAscent = mfontmetricsSansSerif10.getAscent();
		int iLabelDescent = mfontmetricsSansSerif10.getDescent();
		int iLabelHeight = mfontmetricsSansSerif10.getHeight();
		int iLabelOffset = 2; // constant for now
		int iLabelLength = mfontmetricsSansSerif10.stringWidth(sLabel);
		int iScaleLength = ( iLabelLength > pxRuleLength ) ? iLabelLength : pxRuleLength;
		int iScaleThickness = 1 + iLabelOffset + iLabelHeight;

		// determine the layout object dimensions
		int hObject, vObject, heightObject, widthObject;
		switch( layout.getObject() ){
			case Canvas:
				hObject = 0;
				vObject = 0;
				heightObject = pxCanvasHeight;
				widthObject  = pxCanvasWidth;
				break;
			case Plot:
				hObject = 10; // mpxMargin_Left; TODO fix all these
				vObject = 10; // mpxMargin_Top;
				heightObject = pxPlotHeight;
				widthObject  = pxPlotWidth;
				break;
			case AxisVertical:
				hObject = mpxAxis_Vertical_X;
				vObject = mpxAxis_Vertical_Y;
				heightObject = mpxAxis_Vertical_height;
				widthObject  = mpxAxis_Vertical_width;
				break;
			default:
			case AxisHorizontal:
				hObject = mpxAxis_Horizontal_X;
				vObject = mpxAxis_Horizontal_Y;
				heightObject = mpxAxis_Horizontal_height;
				widthObject  = mpxAxis_Horizontal_width;
				break;
		}

		// determine upper left coordinates before rotation
		int pxScaleWidth, pxScaleHeight;
		int iRotation = mOptions.option_scale_Layout.getRotation();
		switch( iRotation ){
			default:
			case 0:
			case 180:
				pxScaleWidth = iScaleLength;
				pxScaleHeight = iScaleThickness;
				break;
			case 90:
			case 270:
				pxScaleWidth = iScaleThickness;
				pxScaleHeight = iScaleLength;
				break;
		}
		long nLocationPacked = layout.getLocation(pxScaleWidth, pxScaleHeight, hObject, vObject, widthObject, heightObject );
		int xScale_before_rotation = (int)(nLocationPacked >> 32);
		int yScale_before_rotation = (int)(nLocationPacked & 0xFFFFFFFF);

		// determine rotated coordinates for rule and label
		int xRule, yRule, xLabel, yLabel;
		int iCenteringOffset = (pxRuleLength - iLabelLength)/2;
		switch( iRotation ){
			default:
			case 0:
				xRule  = xScale_before_rotation - ((pxRuleLength >= iLabelLength) ? 0 : iCenteringOffset);
				yRule  = yScale_before_rotation;
				xLabel = xRule + iCenteringOffset;
				yLabel = yRule + iLabelOffset + iLabelAscent;
				break;
			case 90:
				xRule  = xScale_before_rotation;
				yRule  = yScale_before_rotation - ((pxRuleLength >= iLabelLength) ? 0 : iCenteringOffset);
				xLabel = xRule - iLabelOffset - iLabelAscent;
				yLabel = yRule + iCenteringOffset;
				AffineTransform transform90 = AffineTransform.getRotateInstance(2d * 3.14159 * 90d / 360d);
				font = font.deriveFont(transform90);
				break;
			case 180:
				xRule  = xScale_before_rotation - ((pxRuleLength >= iLabelLength) ? 0 : iCenteringOffset);
				yRule  = yScale_before_rotation + iLabelOffset + iLabelHeight;
				xLabel = xRule + iCenteringOffset;
				yLabel = yScale_before_rotation + iLabelAscent;
				break;
			case 270:
				xRule  = xScale_before_rotation;
				yRule  = yScale_before_rotation;
				xLabel = xRule + iLabelOffset + iLabelAscent;
				yLabel = yRule + iCenteringOffset;
				yLabel += iLabelLength; // adjust for rotation
				AffineTransform transform270 = AffineTransform.getRotateInstance(2d * 3.14159 * 270d / 360d);
				font = font.deriveFont(transform270);
				break;
		}

		// draw lines
		int iTickLength = pxRuleLength / 20 + 2;
		switch( iRotation ){
			default:
			case 0:
				g2.fillRect(xRule, yRule, pxRuleLength, pxRuleThickness);
				g2.drawLine(xRule, yRule, xRule, yRule + iTickLength);
				g2.drawLine(xRule + pxRuleLength, yRule, xRule + pxRuleLength, yRule + iTickLength);
				break;
			case 90:
				g2.fillRect(xRule, yRule, pxRuleThickness, pxRuleLength);
				g2.drawLine(xRule, yRule, xRule - iTickLength, yRule);
				g2.drawLine(xRule, yRule + pxRuleLength, xRule - iTickLength, yRule + pxRuleLength);
				break;
			case 180:
				g2.fillRect(xRule, yRule, pxRuleLength, pxRuleThickness);
				g2.drawLine(xRule, yRule, xRule, yRule - iTickLength);
				g2.drawLine(xRule + pxRuleLength, yRule, xRule + pxRuleLength, yRule - iTickLength);
				break;
			case 270:
				g2.fillRect(xRule, yRule, pxRuleThickness, pxRuleLength);
				g2.drawLine(xRule, yRule, xRule + iTickLength, yRule);
				g2.drawLine(xRule, yRule + pxRuleLength, xRule + iTickLength, yRule + pxRuleLength);
				break;
		}

		// draw label
   		g2.setFont(font);
		g2.drawString(sLabel, xLabel, yLabel);
	}

	protected void vDrawText( Graphics2D g2, int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
		if( mText == null ) return;
		if( mOptions == null ) return;
		Object oAA_value = (mOptions.getValue_boolean(PlotOptions.OPTION_TextAntiAliasing) ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oAA_value);
		for( int xTextItem = 0; xTextItem < mText.getSize(); xTextItem++ ){
			PlotTextItem item = (PlotTextItem)mText.getElementAt(xTextItem);
			PlotLayout layout = item.getPlotLayout();
			if( layout == null ) continue; // hope this doesn't happen
			Font font = item.getStyle().getFont();
			if( font == null ) font = Styles.fontSansSerifBold10;
			Color color = item.getStyle().getColor();
			if( color == null ) if( this.getBackground() == Color.BLACK ) color = Color.WHITE; else color = Color.BLACK;
			String sExpression = item.getString();
			String sText = sExpression;
			vDrawText( g2, sText, font, color, layout, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight );
		}
	}

	protected void vDrawText( Graphics2D g2, String sText, Font font, Color color, PlotLayout layout, int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){

		int hObject, vObject, widthObject, heightObject;
		switch( layout.getObject() ){
			default:
			case Canvas:
				hObject = 0; vObject = 0; widthObject = pxCanvasWidth; heightObject = pxCanvasHeight;
				break;
			case Plot:
				hObject = 10; // mpxMargin_Left; TODO fix
				vObject = 10; // mpxMargin_Top; 
				widthObject = pxPlotWidth; 
				heightObject = pxPlotHeight;
				break;
			case AxisHorizontal:
				hObject = mpxAxis_Horizontal_X; vObject = mpxAxis_Horizontal_Y; widthObject = mpxAxis_Horizontal_width; heightObject = mpxAxis_Horizontal_height;
				break;
			case AxisVertical:
				hObject = mpxAxis_Vertical_X; vObject = mpxAxis_Vertical_Y; widthObject = mpxAxis_Vertical_width; heightObject = mpxAxis_Vertical_height;
				break;
			case Legend:
				hObject = mpxLegend_X; vObject = mpxLegend_Y; widthObject = mpxLegend_Width; heightObject = mpxLegend_Height;
				break;
			case Scale:
				hObject = mpxScale_X; vObject = mpxScale_Y; widthObject = mpxScale_Width; heightObject = mpxScale_Height;
				break;
		}

        boolean zShowBoundingBox = false;
        vDrawText( g2, sText, font, color, layout, hObject, vObject, widthObject, heightObject, zShowBoundingBox );

	}

	public static void vDrawText( Graphics2D g2, String sText, Font font, Color color, PlotLayout layout, int hObject, int vObject, int widthObject, int heightObject, boolean zShowBoundingBox ){
		FontMetrics fm = g2.getFontMetrics(font);
		int pxWidth = fm.stringWidth( sText );
		int pxHeight = fm.getAscent(); // todo calculate multi-line
		if( pxWidth == 0 || pxHeight == 0 ) return; // invalid
		int pxAscent = g2.getFontMetrics(font).getAscent();
		int iRotation_degrees = layout.getRotation();
		double dRotationRadians = 0;
		int pxRotationalTranslation_X = 0;
		int pxRotationalTranslation_Y = 0;
		int pxOffset_X = 0; // (int)((double)pxAscent * Math.sin(dRotationRadians) - (double)pxAscent * Math.cos(dRotationRadians));
		int pxOffset_Y = 0; // pxAscent - (int)((double)pxAscent * Math.sin(dRotationRadians));
		if( iRotation_degrees == 0 ){
			pxOffset_Y = pxAscent;
		} else {

			// create rotated font
			iRotation_degrees = iRotation_degrees % 360;
			if( iRotation_degrees < 0 ) iRotation_degrees = 360 + iRotation_degrees;
			dRotationRadians = TwoPi * (double)iRotation_degrees / 360d;
			double dTextCenter_X = 0; // ((double)pxWidth)/2d;
			double dTextCenter_Y = 0; // pxAscent; // ((double)pxAscent)/-2d;
			AffineTransform transform = AffineTransform.getRotateInstance(dRotationRadians, dTextCenter_X, dTextCenter_Y);
			font = font.deriveFont(transform);

			// calculate new width/height
			double dWidth = (double)pxWidth;
			double dHeight = (double)pxHeight;
			double dDiameter = Math.sqrt(dWidth*dWidth + dHeight*dHeight);
			double dDiagonalAngle = Math.atan( dHeight/dWidth );
			double dAngleRotated = dDiagonalAngle - dRotationRadians;

			// determine rotational xy transformations
			int pxWidth_Rotated, pxHeight_Rotated;
			if( iRotation_degrees <= 90 ){
				pxOffset_Y = (int)Utility.round_up((double)pxAscent * Math.cos(dRotationRadians), 0);
				pxWidth_Rotated = (int)(dDiameter * Math.cos(dAngleRotated));
				pxHeight_Rotated = (int)(dDiameter * Math.sin(dRotationRadians) + pxOffset_Y);
			} else if( iRotation_degrees <= 180 ){
				pxOffset_X = (int)Utility.round_up((double)pxAscent * Math.sin(dRotationRadians), 0);
				pxWidth_Rotated = -1 * (int)(dDiameter * Math.cos(dRotationRadians)) + pxOffset_X;
				pxHeight_Rotated = (int)(dDiameter * Math.sin(dRotationRadians) - pxAscent * Math.cos(dRotationRadians));
				pxRotationalTranslation_X = - pxOffset_X - (int)(dDiameter * Math.cos(dRotationRadians));
			} else if( iRotation_degrees <= 270 ){
				pxOffset_X = -1 * (int)Utility.round_up((double)pxAscent * Math.cos(dRotationRadians), 0);
				pxOffset_Y = (int)Utility.round_up((double)pxAscent * Math.cos(dRotationRadians), 0);
				pxWidth_Rotated = -1 * (int)(dDiameter * Math.cos(dAngleRotated));
				pxHeight_Rotated = -1 * ((int)(dDiameter * Math.sin(dRotationRadians)) + (int)Utility.round_up((double)pxAscent * Math.cos(dRotationRadians), 0));
				pxRotationalTranslation_X = pxWidth_Rotated - pxOffset_X;
				pxRotationalTranslation_Y = pxHeight_Rotated;
			} else { // > 270
				pxOffset_X = -1 * (int)Utility.round_up((double)pxAscent * Math.sin(dRotationRadians), 0);
				pxWidth_Rotated = (int)(dDiameter * Math.cos(dRotationRadians)) + pxOffset_X;
				pxHeight_Rotated = (int)Utility.round_up((double)pxAscent * Math.cos(dRotationRadians), 0) - (int)(dDiameter * Math.sin(dRotationRadians));
				pxRotationalTranslation_X = 0;
				pxRotationalTranslation_Y = pxHeight_Rotated;
			}

			// adjust size of bounding box
			pxWidth = pxWidth_Rotated;
			pxHeight = pxHeight_Rotated;

		}
		long nLocationPacked = layout.getLocation(pxWidth, pxHeight, hObject, vObject, widthObject, heightObject);
		int pxLocation_X = (int)(nLocationPacked >> 32);
		int pxLocation_Y = (int)(nLocationPacked & 0xFFFFFFFF);
		int xText = pxLocation_X + pxRotationalTranslation_X + pxOffset_X;
		int yText = pxLocation_Y + pxRotationalTranslation_Y + pxOffset_Y;
		g2.setFont(font);
		g2.setColor(color);
		g2.drawString( sText, xText, yText);
//        System.out.println("drawing text [" + sText + "] " + xText + " " + yText + "( object: " + hObject + " " + vObject + " " + widthObject + " " + heightObject + " )");
		if( zShowBoundingBox ){
			g2.drawRect(pxLocation_X, pxLocation_Y, pxWidth, pxHeight);
		}
	}
	
	private boolean mzMicroscopeActive = false;
	
	public boolean _isMicroscopeActive(){ return mzMicroscopeActive; }

	protected void activateMicroscope( int[][] aRGB, String[][] asData, int iMicroscopeWidth, int iMicroscopeHeight ){
		Panel_Microscope._activate( this, aRGB, asData, iMicroscopeWidth, iMicroscopeHeight );
		setCursor( Resources.getMicroscopeCursor() );
		mzMicroscopeActive = true;
	}

	public void deactivateMicroscope(){
		setCursor( Cursor.getDefaultCursor() );
		mzMicroscopeActive = false;
	}
	
	// Mouse motion interface
	public void mouseMoved( MouseEvent evt ){}

	// Mouse listener interface
	public void mousePressed(MouseEvent evt){ }
	public void mouseDragged(MouseEvent evt){ }
	public void mouseReleased(MouseEvent evt){ }
	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }
	public void mouseClicked( MouseEvent evt ){
		
	// TODO needs to be region sensitive // plot sensitive
	// if in histogram area or if in expressin area
/*
		histogram.handleMouseClicked( evt );

				int mpxMargin_Left = 10;
				int mpxMargin_Top =  10;
				int xPX = evt.getX();
				int yPX = evt.getY();
			    expression.vUpdateMicroscopeArrays( xPX - mpxMargin_Left, yPX - mpxMargin_Top, mScale.getPlot_Height_pixels() );
	*/
	}

	// Scrollable interface
	public Dimension getPreferredScrollableViewportSize(){
		return getPreferredSize();
	}

	// Components that display logical rows or columns should compute the scroll increment that will completely expose one block of rows or columns, depending on the value of orientation.
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction){
		return 1;
	}

	// Return true if a viewport should always force the height of this Scrollable to match the height of the viewport.
	public boolean getScrollableTracksViewportHeight(){ return false; }

	// Return true if a viewport should always force the width of this Scrollable to match the width of the viewport.
	public boolean getScrollableTracksViewportWidth(){ return false; }

	// Components that display logical rows or columns should compute the scroll increment that will completely expose one new row or column, depending on the value of orientation.
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction){
		return 1;
	}

	void vShowDataMicroscope( IPlottable plottableData, int xPlot, int yPlot ){
		if( plottableData == null ) return;
		int iDataDim_Width = plottableData.getDimension_x();
		int iDataDim_Height = plottableData.getDimension_y();

		// determine data coordinates
		int xClick = xPlot * iDataDim_Width / mScale.getPlot_Width_pixels();
		int yClick = yPlot * iDataDim_Height / mScale.getPlot_Height_pixels();

		// render colors and store values
		int[] aRGB = new int[36];
		String[] as = new String[36];
		switch( plottableData.getDataType() ){
			case DAP.DATA_TYPE_Float32:
				for( int xDataWidth = 0; xDataWidth < 6; xDataWidth++ ){
					for( int xDataHeight = 0; xDataHeight < 6; xDataHeight++ ){
						int xRGB = 6 * xDataHeight + xDataWidth;
						int xData = iDataDim_Width * (yClick + xDataHeight) + (xClick + xDataWidth);
						if( xData < 0 || xData > plottableData.getFloatArray().length ){
							as[xRGB] = null;
						} else {
							as[xRGB] = Float.toString( plottableData.getFloatArray()[xData] );
						}
					}
				}
				mColors.render( aRGB, plottableData.getFloatArray(), 6, 6, 6, 6, false );
				break;
			case DAP.DATA_TYPE_Float64:
				break;
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				break;
			case DAP.DATA_TYPE_UInt32:
				break;
		}

//		final Panel_Microscope microscope = new Panel_Microscope();
//		final JOptionPane jop = new JOptionPane(microscope, JOptionPane.INFORMATION_MESSAGE);
//		final JDialog jd = jop.createDialog(ApplicationController.getInstance().getAppFrame(), "Data Microscope ( " + xPlot + ", " + yPlot + " )");
//		microscope.set(aRGB, as);
//		jd.setVisible( true );
	}

	/** Do not use this currently because we need to receive mouse events
	 *  to trap mouse click on full screen. revisit later

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
	public void mouseClicked(MouseEvent evt){
		int xPX = evt.getX();
		int yPX = evt.getY();
		if( xPX > mpxMargin_Left && xPX < (mpxRenderedCanvasWidth - mpxMargin_Right) && yPX > mpxMargin_Top && yPX < (mpxRenderedCanvasHeight - mpxMargin_Bottom) )
		    vShowDataMicroscope( xPX - mpxMargin_Left, yPX - mpxMargin_Top );
	}
	 */

}




