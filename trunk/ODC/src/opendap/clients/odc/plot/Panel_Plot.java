package opendap.clients.odc.plot;

/**
 * Title:        Panel_Plot
 * Description:  Base class for plotting
 * Copyright:    Copyright (c) 2002-4
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.49
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Styles;
import opendap.clients.odc.DodsURL;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

abstract class Panel_Plot extends JPanel implements Printable, MouseListener, MouseMotionListener {

	public final static boolean DEBUG_Layout = false;

	public final static double TwoPi = 2d * 3.14159d;
	public final static String TEXT_ID_CaptionY = "CaptionY";
	public final static String TEXT_ID_CaptionX = "CaptionX";
	public final static String TEXT_ID_CaptionColorBar = "CaptionColorbar";

	private final static Dimension MIN_DIMENSION = new Dimension(200, 400);

	protected BufferedImage mbi = null;
	protected boolean mzMode_FullScreen = false;

	// scaling
	protected int mpxAxisOffsetHeight = 0; // these offsets are mostly used for plotting line data
	protected int mpxAxisOffsetWidth = 0;

	// data
	public final static int DATA_TYPE_Byte = 1;
	public final static int DATA_TYPE_Int16 = 2;
	public final static int DATA_TYPE_Int32 = 3;
	public final static int DATA_TYPE_UInt16 = 4;
	public final static int DATA_TYPE_UInt32 = 5;
	public final static int DATA_TYPE_Float32 = 6;
	public final static int DATA_TYPE_Float64 = 7;

	protected int miDataType = 0; // no data
	protected short[] mashData = null; // byte and int16
	protected int[] maiData = null; // uint16 and int32
	protected long[] manData = null; // uint32
	protected float[] mafData = null; // float32
	protected double[] madData = null; // float64
	protected short[] mashData2 = null; // the #2's are for vector plots
	protected int[] maiData2 = null;
	protected long[] manData2 = null;
	protected float[] mafData2 = null;
	protected double[] madData2 = null;
	protected int[] maiRGBArray = null;
	protected int mDataDim_Width = 0;
	protected int mDataDim_Height = 0;

	protected ColorSpecification mColors = null;
	protected GeoReference mGeoReference = null;

	// Missing Values
	int mctMissing1;
	int mctMissing2;
	short[] mashMissing1;
	int[] maiMissing1;
	long[] manMissing1;
	float[] mafMissing1;
	double[] madMissing1;
	short[] mashMissing2;
	int[] maiMissing2;
	long[] manMissing2;
	float[] mafMissing2;
	double[] madMissing2;

	// axes
	protected PlotAxis axisVertical = null;
	protected PlotAxis axisHorizontal = null;
	protected String msAxisLabel_Vertical = null;
	protected String msAxisLabel_Horizontal = null;
	protected int mctTick_vertical, mctTick_horizontal;
	protected int mpxTickOffset_vertical_lower;
	protected int mpxTickOffset_vertical_upper;
	protected int mpxTickOffset_horizontal_lower;
	protected int mpxTickOffset_horizontal_upper;
	protected String[] masTickLabels_vertical, masTickLabels_horizontal;
	Font fontAxisTicks = Styles.fontFixed10;

	// legend
	protected int mpxLegend_X = 0;
	protected int mpxLegend_Y = 0;
	protected int mpxLegend_Width = 0;
	protected int mpxLegend_Height = 0;

	// Scale
	protected int mpxScale_X = 0;
	protected int mpxScale_Y = 0;
	protected int mpxScale_Width = 0;
	protected int mpxScale_Height = 0;

	private static int miSessionCount = 0;
	final private static String FORMAT_ID_date = "yyyyMMdd";
	private String msID;
	private String msCaption = null;
	private DodsURL mURL = null;

	abstract public void vGenerateImage(int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight );
	abstract public String getDescriptor();
	abstract public boolean zCreateRGBArray(int pxWidth, int pxHeight, boolean zAveraged, StringBuffer sbError);

	Panel_Plot(PlotScale ps, String sID_descriptive, String sCaption, DodsURL url){
		if( ps == null ){
			ApplicationController.vShowError("system error, invalid plot panel, no scale");
		}
		if( miSessionCount == 0 ){ // update from properties
			miSessionCount = ConfigurationManager.getInstance().getProperty_PlotCount();
		}
		miSessionCount++;
		ConfigurationManager.getInstance().setOption(ConfigurationManager.PROPERTY_COUNT_Plots, Integer.toString(miSessionCount));
		int iCountWidth;
		if( miSessionCount < 1000 ) iCountWidth = 3;
		else if( miSessionCount < 100000 ) iCountWidth = 5;
		else iCountWidth = 10;
		if( sID_descriptive == null ){ // descriptive portion of id is generated from date and type
			sID_descriptive = Utility.getCurrentDate(FORMAT_ID_date);
		}
		msID = sID_descriptive + "-" + getDescriptor() + "-" + Utility.sFormatFixedRight(miSessionCount, iCountWidth, '0'); // append plot count to ID descriptive string
		mScale = ps;
		mURL = url;
		msCaption = sCaption;
	}

	// this is needed to tell any container how big the panel wants to be
	// the preferred values should be set whenever an image is generated (see vGenerateImage)
	int mpxPreferredWidth = 250;
	int mpxPreferredHeight = 250;
    public Dimension getPreferredSize() {
		return new Dimension(mpxPreferredWidth, mpxPreferredHeight);
    }

	BufferedImage getImage(){ return mbi; }
	String getID(){ return msID; }
	String getCaption(){ return msCaption; }
	DodsURL getURL(){ return mURL; }

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
		if( mScale == null ){
			ApplicationController.vShowError("System error, unable to print, no scale defined.");
			return java.awt.print.Printable.NO_SUCH_PAGE;
		}
		try {
			// todo use black and white / color models for bw printers
			vCreateImage( false );
			((Graphics2D)g).drawImage(mbi, null, 0, 0); // flip image to printer
			return java.awt.print.Printable.PAGE_EXISTS;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, "While trying to print");
			return java.awt.print.Printable.NO_SUCH_PAGE;
		}
	}

	static boolean mzFatalError = false;
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		try {
			if( mbi != null ) ((Graphics2D)g).drawImage(mbi, null, 0, 0); // flip image to canvas
		} catch(Exception ex) {
			if( mzFatalError ) return; // got the training wheels on here todo
			mzFatalError = true;
			Utility.vUnexpectedError(ex, "Error rendering plot image");
		}
	}

	public boolean zPlot( StringBuffer sbError ){
		try {
			boolean zFill = (this instanceof Panel_Plot_Histogram);
			vCreateImage( zFill );
			return true;
		} catch(Throwable ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	private void vCreateImage(boolean zFill ){

		// standard scaled area
		int pxCanvasWidth = mScale.getCanvas_Width(zFill);
		int pxCanvasHeight = mScale.getCanvas_Height(zFill);
		int pxPlotWidth = mScale.getPlot_Width(zFill);
		int pxPlotHeight = mScale.getPlot_Height(zFill);

		// special requirements
		if( this instanceof Panel_Plot_Vector ){
			PlotOptions po = this.getPlotOptions();
			int iVectorSize = 10;
			if( po != null ) iVectorSize = po.getValue_int(PlotOptions.OPTION_VectorSize);
			if( pxPlotHeight < iVectorSize * 2 ) pxPlotHeight = iVectorSize * 2;
		}

		if( mbi == null ){
			mbi = new BufferedImage( pxCanvasWidth, pxCanvasHeight, BufferedImage.TYPE_INT_ARGB );
		}
		vWriteImageBuffer(mbi, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight );
	}

	private void vWriteImageBuffer( BufferedImage bi, int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
		if( bi == null ){
			ApplicationController.vShowError("internal error, no buffer");
			mbi.getGraphics().drawString("[error rendering plot, no buffer (out of memory?)]", 10, 30);
		} else {
			StringBuffer sbError = new StringBuffer(80);

			Graphics2D g2 = (Graphics2D)mbi.getGraphics();
			g2.setColor(mcolorBackground);
			g2.fillRect(0,0,pxCanvasWidth,pxCanvasHeight); // draw background
			int pxPlotLeft = mpxMargin_Left;
			int pxPlotTop  = mpxMargin_Top;

			if( axisVertical != null ){
				if( !axisVertical.zDetermineScaleInterval(pxPlotHeight, fontAxisTicks, true, mbi.createGraphics(), false, 0, 0, sbError) ){
					ApplicationController.vShowError("Failed to generate vertical axis: " + sbError);
				}
				if( axisVertical.mpxTick_MajorInterval == 0 ){
					ApplicationController.vShowError("Failed to generate vertical axis");
				}
				mpxTickOffset_vertical_lower = axisVertical.getOffset_LowerPX();
				mpxTickOffset_vertical_upper = axisVertical.getOffset_UpperPX();
				mctTick_vertical = axisVertical.getTickCount();
				masTickLabels_vertical = axisVertical.getScaleLabels1();
			}
			if( axisHorizontal != null ){
				if( !axisHorizontal.zDetermineScaleInterval(pxPlotWidth, fontAxisTicks, false, mbi.createGraphics(), false, 0, 0, sbError) ){
					ApplicationController.vShowError("Failed to generate vertical axis: " + sbError);
				}
				if( axisHorizontal.mpxTick_MajorInterval == 0 ){
					ApplicationController.vShowError("Failed to generate horizontal axis");
				}
				mpxTickOffset_horizontal_lower = axisHorizontal.getOffset_LowerPX();
				mpxTickOffset_horizontal_upper = axisHorizontal.getOffset_UpperPX();
				mctTick_horizontal = axisHorizontal.getTickCount();
				masTickLabels_horizontal = axisHorizontal.getScaleLabels1();
			}

			g2.setClip(pxPlotLeft, pxPlotTop, pxPlotWidth, pxPlotHeight);
			vGenerateImage(pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight );
			g2.setClip(0, 0, pxCanvasWidth, pxCanvasHeight);
			 // todo regularize histograms
			if( !(this instanceof Panel_Plot_Histogram) ) vDrawAnnotations(g2, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight);
		}
	}

	public int[] getRGBArray( int pxPlotWidth, int pxPlotHeight, StringBuffer sbError ){
		zCreateRGBArray(pxPlotWidth, pxPlotHeight, false, sbError);
		return maiRGBArray;
	}

	protected boolean mzHasAxes = true;
	protected int mpxAxisThickness = 1; // todo need to make settable as well as others
	protected int mpxGraphOffset = 0; // this is the offset between the y-axis and left/right edges of the graph
	protected int mpxMargin_Top = 70;
	protected int mpxMargin_Bottom = 60;
	protected int mpxMargin_Left = 50;
	protected int mpxMargin_Right = 40;
	protected int mpxVerticalTick_LabelOffset = 2;
	protected int mpxHorizontalTick_LabelOffset = 2;
	protected int mpxTickMajorLength = 4;
	protected int mpxTickMediumLength = 5;
	protected int mpxTickMinorLength = 3;
	protected boolean mzBoxed = true;
	boolean getBoxed(){ return mzBoxed; }
//	int getCanvasWidth(){ return this.mpxCanvasWidth; }
//	int getCanvasHeight(){ return this.mpxCanvasHeight; }
	int getMarginPixels_Top(){ return mpxMargin_Top; }
	int getMarginPixels_Bottom(){ return mpxMargin_Bottom; }
	int getMarginPixels_Left(){ return mpxMargin_Left; }
	int getMarginPixels_Right(){ return mpxMargin_Right; }
	void setMarginPixels_Top( int iPixels ){ mpxMargin_Top =  iPixels; }
	void setMarginPixels_Bottom( int iPixels ){ mpxMargin_Bottom =  iPixels; }
	void setMarginPixels_Left( int iPixels ){ mpxMargin_Left =  iPixels; }
	void setMarginPixels_Right( int iPixels ){ mpxMargin_Right =  iPixels; }

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

	// Data Management
	int getDataElementCount(){
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				if( mashData == null ) return 0; else return mashData.length;
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				if( maiData == null ) return 0; else return maiData.length;
			case DATA_TYPE_UInt32:
				if( manData == null ) return 0; else return mashData.length;
			case DATA_TYPE_Float32:
				if( mafData == null ) return 0; else return mafData.length;
			case DATA_TYPE_Float64:
				if( madData == null ) return 0; else return madData.length;
		}
		return 0;
	}

	boolean setData( int eTYPE, Object[] eggData, Object[] eggMissing, Object[] eggData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		if( iWidth <= 0 || iHeight <= 0 ){
			sbError.append("Width " + iWidth + " and Height " + iHeight + " cannot be zero or negative.");
			return false;
		}
		if( mOptions != null ){
			mzBoxed = mOptions.getValue_boolean(PlotOptions.OPTION_Boxed);
		}
		if( !zUpdateDimensions(iWidth, iHeight, sbError) ){
			sbError.insert(0, "failed to update dimensions (" + iWidth + ", " + iHeight + "): ");
			return false;
		}
		if( eggData2 == null ){
			switch( eTYPE ){
				case DATA_TYPE_Byte:
				case DATA_TYPE_Int16:
					return setData( eTYPE, (short[])eggData[0], eggMissing, null, eggMissing2, iWidth, iHeight, sbError );
				case DATA_TYPE_UInt16:
				case DATA_TYPE_Int32:
					return setData( eTYPE, (int[])eggData[0], eggMissing, null, eggMissing2, iWidth, iHeight, sbError );
				case DATA_TYPE_UInt32:
					return setData( (long[])eggData[0], eggMissing, null, eggMissing2, iWidth, iHeight, sbError );
				case DATA_TYPE_Float32:
					return setData( (float[])eggData[0], eggMissing, null, eggMissing2, iWidth, iHeight, sbError );
				case DATA_TYPE_Float64:
					return setData( (double[])eggData[0], eggMissing, null, eggMissing2, iWidth, iHeight, sbError );
				default:
					sbError.append("Data type " + eTYPE + " not supported by pseudocolor plotter");
					return false;
			}
		} else {
			switch( eTYPE ){
				case DATA_TYPE_Byte:
				case DATA_TYPE_Int16:
					return setData( eTYPE, (short[])eggData[0], eggMissing, (short[])eggData2[0], eggMissing2, iWidth, iHeight, sbError );
				case DATA_TYPE_UInt16:
				case DATA_TYPE_Int32:
					return setData( eTYPE, (int[])eggData[0], eggMissing, (int[])eggData2[0], eggMissing2, iWidth, iHeight, sbError );
				case DATA_TYPE_UInt32:
					return setData( (long[])eggData[0], eggMissing, (long[])eggData2[0], eggMissing2, iWidth, iHeight, sbError );
				case DATA_TYPE_Float32:
					return setData( (float[])eggData[0], eggMissing, (float[])eggData2[0], eggMissing2, iWidth, iHeight, sbError );
				case DATA_TYPE_Float64:
					return setData( (double[])eggData[0], eggMissing, (double[])eggData2[0], eggMissing2, iWidth, iHeight, sbError );
				default:
					sbError.append("Data type " + eTYPE + " not supported by pseudocolor plotter");
					return false;
			}
		}
	}

	boolean zUpdateDimensions(int iDataPoint_width, int iDataPoint_height, StringBuffer sbError){
		mScale.setDataDimension(iDataPoint_width, iDataPoint_height);
		mpxMargin_Left = mScale.getMarginLeft_px();
		mpxMargin_Right = mScale.getMarginRight_px();
		mpxMargin_Top = mScale.getMarginTop_px();
		mpxMargin_Bottom = mScale.getMarginBottom_px();
		this.invalidate(); // when the canvas dimensions change, the layout is invalidated
		mbi = null;
		return true;
	}

	boolean setData( int eTYPE, short[] ashortData, Object[] eggMissing, short[] ashortData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		miDataType = eTYPE;
		mashData = ashortData;
		mashData2 = ashortData2;
		if( eggMissing != null && eggMissing[0] != null ){
			mashMissing1 = (short[])eggMissing[0];
			mctMissing1 = mashMissing1.length - 1;
		}
		if( eggMissing2 != null && eggMissing2[0] != null ){
			mashMissing2 = (short[])eggMissing2[0];
			mctMissing2 = mashMissing2.length - 1;
		}
		return setData( iWidth, iHeight, sbError );
	}
	boolean setData( int eTYPE, int[] aiData, Object[] eggMissing, int[] aiData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		miDataType = eTYPE;
		maiData = aiData;
		maiData2 = aiData2;
		if( eggMissing != null && eggMissing[0] != null ){
			maiMissing1 = (int[])eggMissing[0];
			mctMissing1 = maiMissing1.length - 1;
		}
		if( eggMissing2 != null && eggMissing2[0] != null ){
			maiMissing2 = (int[])eggMissing2[0];
			mctMissing2 = maiMissing2.length - 1;
		}
		return setData( iWidth, iHeight, sbError );
	}
	boolean setData( long[] anData, Object[] eggMissing, long[] anData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		miDataType = DATA_TYPE_UInt32;
		manData = anData;
		manData2 = anData2;
		if( eggMissing != null && eggMissing[0] != null ){
			manMissing1 = (long[])eggMissing[0];
			mctMissing1 = manMissing1.length - 1;
		}
		if( eggMissing2 != null && eggMissing2[0] != null ){
			manMissing2 = (long[])eggMissing2[0];
			mctMissing2 = manMissing2.length - 1;
		}
		return setData( iWidth, iHeight, sbError );
	}
	boolean setData( float[] afData, Object[] eggMissing, float[] afData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		miDataType = DATA_TYPE_Float32;
		mafData = afData;
		mafData2 = afData2;
		if( eggMissing != null && eggMissing[0] != null ){
			mafMissing1 = (float[])eggMissing[0];
			mctMissing1 = mafMissing1.length - 1;
		}
		if( eggMissing2 != null && eggMissing2[0] != null ){
			mafMissing2 = (float[])eggMissing2[0];
			mctMissing2 = mafMissing2.length - 1;
		}
		return setData( iWidth, iHeight, sbError );
	}
	boolean setData( double[] adData, Object[] eggMissing, double[] adData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		miDataType = DATA_TYPE_Float64;
		madData = adData;
		madData2 = adData2;
		if( eggMissing != null && eggMissing[0] != null ){
			madMissing1 = (double[])eggMissing[0];
			mctMissing1 = madMissing1.length - 1;
		}
		if( eggMissing2 != null && eggMissing2[0] != null ){
			madMissing2 = (double[])eggMissing2[0];
			mctMissing2 = madMissing2.length - 1;
		}
		return setData( iWidth, iHeight, sbError );
	}
	boolean setData( int iWidth, int iHeight, StringBuffer sbError ){
		try {
			mDataDim_Width = iWidth;
			mDataDim_Height = iHeight;

			// establish scale
			mScale.setDataDimension(iWidth, iHeight);
			mpxMargin_Left   = mScale.getMarginLeft_px();
			mpxMargin_Right  = mScale.getMarginLeft_px();
			mpxMargin_Top    = mScale.getMarginTop_px();
			mpxMargin_Bottom = mScale.getMarginBottom_px();
			return true;
		} catch(Throwable ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	// Axes Management
	void setAxisVertical( PlotAxis axis ){
		axisVertical = axis;
	}
	void setAxisHorizontal( PlotAxis axis ){
		axisHorizontal = axis;
	}

	PlotText mText = null;
	public PlotText getText(){ return mText; }
	void setText( PlotText pt ){
		mText = pt;
	}

	private PlotOptions mOptions = null;
	protected PlotOptions getPlotOptions(){ return mOptions; }
	void setOptions( PlotOptions po ){ mOptions = po; }

	private PlotScale mScale = null;
	PlotScale getPlotScale(){ return mScale; }

	int mpxAxis_Horizontal_X, mpxAxis_Horizontal_Y, mpxAxis_Horizontal_width, mpxAxis_Horizontal_height;
	int mpxAxis_Vertical_X, mpxAxis_Vertical_Y, mpxAxis_Vertical_width, mpxAxis_Vertical_height;

	// the order of the annotations is important because they can be positioned relative to each other
	protected void vDrawAnnotations(Graphics2D g2, int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){

		vGenerateText();

		vDrawCoastline(g2, pxPlotWidth, pxPlotHeight);
		vDrawBorders(g2, pxPlotWidth, pxPlotHeight);
		vDrawAxes(g2, pxPlotWidth, pxPlotHeight);
		vDrawLegend(g2, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight );
		vDrawScale(g2, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight );
		vDrawText(g2, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight );
	}

	private void vGenerateText(){
		mText.remove(TEXT_ID_CaptionY); // there may be a pre-existing, automatically generated caption
		mText.remove(TEXT_ID_CaptionX); // there may be a pre-existing, automatically generated caption
		mText.remove(TEXT_ID_CaptionColorBar); // there may be a pre-existing, automatically generated caption
		if( mOptions == null || mText == null ) return;  // need these structures to continue
	    if( ! mOptions.getValue_boolean(PlotOptions.OPTION_GenerateAxisCaptions) ) return;

		// it is kind of hokey to have this here but there is no other easy option
		if( Panel_View_Plot.getPlotType() == Output_ToPlot.PLOT_TYPE_Histogram ) return;

		// Vertical Axis
		if( axisVertical == null ){
		} else {
			String sCaption = axisVertical.getCaption();
			if( sCaption == null ){
			} else {
				PlotTextItem text = mText.getNew(TEXT_ID_CaptionY);
				text.setExpression(sCaption);
				Font font;
				if( mpxAxis_Vertical_height > 100 ){
					font = Styles.fontSansSerif12;
				} else {
					font = Styles.fontSansSerif10;
				}
				text.setFont(font);
				PlotLayout layout = text.getPlotLayout();
				layout.setObject(PlotLayout.OBJECT_AxisVertical);
				layout.setOrientation(PlotLayout.ORIENT_LeftMiddle);
				layout.setAlignment(PlotLayout.ORIENT_RightMiddle);
				layout.setOffsetHorizontal(-10);
				layout.setRotation(270);
			}
		}

		// Horizontal Axis
		if( axisHorizontal != null){
			String sCaption = axisHorizontal.getCaption();
			if( sCaption != null ){
				PlotTextItem text = mText.getNew(TEXT_ID_CaptionX);
				text.setExpression(sCaption);
				Font font;
				if( mpxAxis_Vertical_height > 100 ){
					font = Styles.fontSansSerif12;
				} else {
					font = Styles.fontSansSerif10;
				}
				text.setFont(font);
				PlotLayout layout = text.getPlotLayout();
				layout.setObject(PlotLayout.OBJECT_AxisHorizontal);
				layout.setOrientation(PlotLayout.ORIENT_BottomMiddle);
				layout.setAlignment(PlotLayout.ORIENT_TopMiddle);
				layout.setOffsetVertical(5);
				layout.setRotation(0);
			}
		}

	}

	private void vDrawCoastline( Graphics2D g2, int pxPlotWidth, int pxPlotHeight ){
		if( ! (this instanceof Panel_Plot_Pseudocolor) &&
		    ! (this instanceof Panel_Plot_Vector) ) return;
		if( mOptions != null ){
			if( !mOptions.getValue_boolean(PlotOptions.OPTION_ShowCoastLine) ) return;
		}
		if( axisHorizontal == null || axisVertical == null ){
			ApplicationController.vShowWarning("unable to draw coastline, no axes/geo reference");
			return;
		} else {
			mGeoReference = new GeoReference();
			StringBuffer sbError = new StringBuffer(80);
			if( !mGeoReference.zInitialize(axisHorizontal.getValues1(), axisVertical.getValues1(), GeoReference.TYPE_IntegerG0, sbError) ){
				ApplicationController.vShowWarning("unable to draw coastline: " + sbError);
				return;
			}
		}
		int xPlotArea_LowerLeft = mpxMargin_Left;
		int yPlotArea_LowerLeft = mpxMargin_Top + pxPlotHeight;
		Coastline.vDrawCoastline(g2, xPlotArea_LowerLeft, yPlotArea_LowerLeft, pxPlotWidth, pxPlotHeight, mGeoReference);
	}

	protected void vDrawAxes( Graphics2D g2, int pxPlotWidth, int pxPlotHeight ){
		g2.setFont(Styles.fontSansSerif10);
		g2.setColor(Color.black);
		FontMetrics mfontmetricsSansSerif10 = g2.getFontMetrics(Styles.fontSansSerif10);

		// draw vertical axis
		int pxTick_Left = mpxMargin_Left - mpxAxisThickness - mpxTickMajorLength;
		mpxAxis_Horizontal_X = mpxMargin_Left;
		mpxAxis_Horizontal_X -= mpxAxisThickness;
		if( axisVertical == null ){
			// draw nothing
		} else {
			mpxAxis_Vertical_Y = mpxMargin_Top;
			mpxAxis_Vertical_width = mpxTickMajorLength + mpxAxisThickness;
			mpxAxis_Vertical_height = pxPlotHeight; // + mpxAxisOffsetHeight;
			int pxLabelLeft_min = mpxMargin_Left;
			if( mpxMargin_Left >= mpxAxisThickness ){
				g2.fillRect(mpxAxis_Horizontal_X, mpxMargin_Top, mpxAxisThickness, mpxAxis_Vertical_height);
				int pxTickRight = pxTick_Left + mpxTickMajorLength - 1;
				int iLabelHalfHeight = mfontmetricsSansSerif10.getAscent() / 2;
				for( int xTick = 1; xTick <= mctTick_vertical; xTick++ ){
					int pxTickHeight = (int)((xTick-1)*axisVertical.mpxTick_MajorInterval) + mpxTickOffset_vertical_lower;
					int pxTickTop = mpxMargin_Top + (pxPlotHeight - pxTickHeight) - 1;
					g2.drawLine(pxTick_Left, pxTickTop, pxTickRight, pxTickTop);
					if( masTickLabels_vertical != null ){
						String sTickLabel = masTickLabels_vertical[xTick];
						int pxLabelLeft = pxTick_Left - mpxVerticalTick_LabelOffset - mfontmetricsSansSerif10.stringWidth(sTickLabel);
						if( pxLabelLeft < pxLabelLeft_min ) pxLabelLeft_min = pxLabelLeft;
						int pxLabelTop  = pxTickTop + iLabelHalfHeight - 1;
						g2.drawString(sTickLabel, pxLabelLeft, pxLabelTop);
					}
				}

			}
			mpxAxis_Vertical_X = pxLabelLeft_min;  // this can only be determined after all the labels are calculated because it is dependent on the longest label
		}

		// draw x-axis (horizontal)
		if( axisHorizontal == null ){
			// draw nothing
		} else {
			int pxHorizontalLabelHeight = mfontmetricsSansSerif10.getHeight();
			mpxAxis_Horizontal_Y = mpxMargin_Top + pxPlotHeight;
			mpxAxis_Horizontal_width = pxPlotWidth + 2 * mpxAxisThickness;
			mpxAxis_Horizontal_height = mpxAxisThickness + mpxTickMajorLength + mpxHorizontalTick_LabelOffset + pxHorizontalLabelHeight;
			if( pxPlotWidth < 10 && axisHorizontal != null ) return; // abort - canvas too small
			if( mpxAxisThickness <= mpxMargin_Bottom ){
				g2.fillRect(mpxAxis_Horizontal_X, mpxAxis_Horizontal_Y, mpxAxis_Horizontal_width, mpxAxisThickness);
				int pxTick_Top = mpxMargin_Top + pxPlotHeight + mpxAxisThickness;
				int pxLabelTop  = pxTick_Top + mpxTickMajorLength + mpxHorizontalTick_LabelOffset + pxHorizontalLabelHeight;
				double pxTickSpacing = axisHorizontal.mpxTick_MajorInterval;
				for( int xTick = 1; xTick <= mctTick_horizontal; xTick++ ){
					int pxTick_Width = (int)((xTick-1)*pxTickSpacing) + mpxTickOffset_horizontal_lower;
					pxTick_Left = mpxMargin_Left + pxTick_Width;
					g2.drawLine(pxTick_Left, pxTick_Top, pxTick_Left, pxTick_Top + mpxTickMajorLength);
					if( masTickLabels_horizontal != null ){
						String sTickLabel = masTickLabels_horizontal[xTick];
						int iLabelHalfWidth = mfontmetricsSansSerif10.stringWidth(sTickLabel) / 2;
						int pxLabelLeft = pxTick_Left - iLabelHalfWidth + 1; // for some reason the plus one makes it come out evenly // mpxMargin_Left - mpxTickMajorLength - mfontmetricsSansSerif10.stringWidth(sTickLabel) - mpxVerticalTick_LabelOffset;
						g2.drawString(sTickLabel, pxLabelLeft, pxLabelTop);
					}
				}
			}
		}
	}

// fyi drawing a filled box
//g2.setColor(Color.BLACK);
//g2.drawRect(10, 10, 9, 9);
//g2.setColor(Color.CYAN);
//g2.fillRect(11, 11, 8, 8);
//g2.setColor(Color.BLACK);

	private void vDrawBorders( Graphics2D g2, int pxPlotWidth, int pxPlotHeight){
		if( mzBoxed ){

			int pxAxisLength_horizontal = pxPlotWidth + mpxAxisOffsetWidth;
			int pxAxisLength_vertical = pxPlotHeight + mpxAxisOffsetHeight;

			int xBox      = mpxMargin_Left - mpxAxisThickness;
			int yBox      = mpxMargin_Top - mpxAxisThickness;
			int widthBox  = pxPlotWidth + mpxAxisOffsetWidth + mpxAxisThickness * 2;
			int heightBox = pxPlotHeight + mpxAxisOffsetHeight + mpxAxisThickness * 2;

			g2.setColor(Color.black);
			g2.fillRect(xBox, yBox, widthBox - 1, mpxAxisThickness); // top
			g2.fillRect(xBox, yBox, mpxAxisThickness, heightBox - 1); // left
			g2.fillRect(xBox + widthBox - 1, yBox, mpxAxisThickness, heightBox - 1); // right
			g2.fillRect(xBox, yBox + heightBox - 1, widthBox - 1, mpxAxisThickness); // bottom
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
			case PlotLayout.OBJECT_Canvas:
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
			case PlotLayout.OBJECT_Plot:
				if( iRotation == 90 || iRotation == 270 ){
					iDefaultLength = pxPlotHeight - 2 * pxOffset_height;
				} else {
					iDefaultLength = pxPlotWidth - 2 * pxOffset_width;
				}
				hObject = mpxMargin_Left;
				vObject = mpxMargin_Top;
				heightObject = pxPlotHeight;
				widthObject  = pxPlotWidth;
				break;
			case PlotLayout.OBJECT_AxisVertical:
				iDefaultLength = mpxAxis_Vertical_height - 2 * pxOffset_height;
				hObject = mpxAxis_Vertical_X;
				vObject = mpxAxis_Vertical_Y;
				heightObject = mpxAxis_Vertical_height;
				widthObject  = mpxAxis_Vertical_width;
				break;
			default:
			case PlotLayout.OBJECT_AxisHorizontal:
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
		if( this instanceof Panel_Plot_Line ){
			vDrawLegend_Lines( g2, font, iLength, iRotation, layout, hObject, vObject, widthObject, heightObject );
		} else {
			vDrawLegend_Colorbar( g2, font, iLength, iRotation, layout, hObject, vObject, widthObject, heightObject );
		}
	}

	protected void vDrawLegend_Lines( Graphics2D g2, Font font, int iLength, int iRotation, PlotLayout layout, int hObject, int vObject, int widthObject, int heightObject ){
		Panel_Plot_Line panel = (Panel_Plot_Line)this;
		Color[] colors = panel.getColors();
		String[] captions = panel.getCaptions();
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
		if( mColors == null ) return;
		StringBuffer sbError = new StringBuffer(80);
		FontMetrics fontmetrics = g2.getFontMetrics(font);
		int ctRanges = mColors.getRangeCount();
		if( ctRanges == 0 ) return;
		int iSwatchLength = iLength / ctRanges;
		int iSwatchThickness = 15; // constant for now
		int pxSwatchHeight, pxSwatchWidth, pxColorBarWidth, pxColorBarHeight;
		boolean zColorBarHorizontal;
		boolean zColorsAscending;
		switch( iRotation ){
			default:
			case 0:
				pxSwatchWidth = iSwatchLength;
				pxSwatchHeight = iSwatchThickness;
				pxColorBarWidth = iLength;
				pxColorBarHeight = iSwatchThickness;
				zColorBarHorizontal = true;
				zColorsAscending = true;    // colors go from left to right
				break;
			case 90:
				pxSwatchWidth = iSwatchThickness;
				pxSwatchHeight = iSwatchLength;
				pxColorBarWidth = iSwatchThickness;
				pxColorBarHeight = iLength;
				zColorBarHorizontal = false;
				zColorsAscending = false;    // colors go from top to bottom
				break;
			case 180:
				pxSwatchHeight = iSwatchThickness;
				pxSwatchWidth = iSwatchLength;
				pxColorBarWidth = iLength;
				pxColorBarHeight = iSwatchThickness;
				zColorBarHorizontal = true;
				zColorsAscending = false;    // colors go from right to left
				break;
			case 270:
				pxSwatchWidth = iSwatchThickness;
				pxSwatchHeight = iSwatchLength;
				pxColorBarWidth = iSwatchThickness;
				pxColorBarHeight = iLength;
				zColorBarHorizontal = false;
				zColorsAscending = true;    // colors go from bottom to top
				break;
		}

		long nLocationPacked = layout.getLocation(pxColorBarWidth, pxColorBarHeight, hObject, vObject, widthObject, heightObject );


		mpxLegend_X = (int)(nLocationPacked >> 32);
		mpxLegend_Y = (int)(nLocationPacked & 0xFFFFFFFF);

		int iLabelAscent = fontmetrics.getAscent();
		int iLabelOffset = 2; // constant for now
		int acrossColorBar = zColorBarHorizontal ? mpxLegend_Y : mpxLegend_X;
		int alongColorBar = zColorBarHorizontal ? mpxLegend_X : mpxLegend_Y;
		int acrossSwatch = acrossColorBar;
		int acrossLabel = acrossSwatch + iSwatchThickness + iLabelOffset + iLabelAscent;
		boolean zSwatchBorder = false;
        int pxMaxLabelLength = 0;
		for( int xRange = 1; xRange <= ctRanges; xRange++ ){
			Image imageSwatch = mColors.imageMakeSwatch(xRange, pxSwatchWidth, pxSwatchHeight, zColorBarHorizontal, zColorsAscending, zSwatchBorder);
			int alongSwatch;
			if( (zColorsAscending && zColorBarHorizontal) || (!zColorsAscending && !zColorBarHorizontal) ){
				alongSwatch = alongColorBar + (xRange - 1) * iSwatchLength;
			} else {
				alongSwatch = alongColorBar + (ctRanges - xRange) * iSwatchLength;
			}
//			if( zColorsAscending ){
//				alongSwatch = alongColorBar + (xRange - 1) * iSwatchLength;
//			} else {
//				alongSwatch = alongColorBar + (ctRanges - xRange) * iSwatchLength;
//			}
			if( zColorBarHorizontal ){
				g2.drawImage(imageSwatch, alongSwatch, acrossSwatch, pxSwatchWidth, pxSwatchHeight, null);
			} else {
				g2.drawImage(imageSwatch, acrossSwatch, alongSwatch, pxSwatchWidth, pxSwatchHeight, null);
			}
			String sFrom = mColors.getDataFromS(xRange);
			String sTo   = mColors.getDataToS(xRange);
			if( sFrom == null ) sFrom = "";
			if( sTo == null ) sTo = "";
			int pxAscent = iLabelAscent / 2;
			if( mColors.getColorFromHSB(xRange) == mColors.getColorToHSB(xRange) ){
				String sSwatchLabel = null;
				if( sFrom.equals(sTo) ){
					sSwatchLabel = sFrom;
				} else {
					sSwatchLabel = sFrom + "-" + sTo;
				}
				int iLabelWidth = fontmetrics.stringWidth(sSwatchLabel);
				double dRotate = 0d;
				int alongLabel;
				if( iLabelWidth < iSwatchLength ){ // make labels parallel to colorbar
					if( zColorBarHorizontal ){
						alongLabel = alongSwatch + (int)(0.5 * iSwatchLength);
					} else {
						dRotate = 270d;
						alongLabel = alongSwatch + (int)(0.5 * (iSwatchLength - iLabelWidth)) - iLabelWidth; // adjust for rotation
					}
				} else { // make labels orthogonal to colorbar
					alongLabel = alongSwatch + (int)(0.5 * (iSwatchLength + pxAscent) + 1);
					if( zColorBarHorizontal ) dRotate = 90d;
				}
				if( dRotate != 0 ){
					AffineTransform transform = AffineTransform.getRotateInstance(2d * 3.14159 * dRotate / 360d);
					font = font.deriveFont(transform);
				}
	    		g2.setFont(font);
                int pxSwatchLabelLength = fontmetrics.stringWidth(sSwatchLabel);
                if( pxSwatchLabelLength > pxMaxLabelLength ) pxMaxLabelLength = pxSwatchLabelLength;
				if( zColorBarHorizontal ){
					g2.drawString(sSwatchLabel, alongLabel, acrossLabel);
				} else {
					g2.drawString(sSwatchLabel, acrossLabel - 5, alongLabel); // the -5 is because there is no tick
				}
			} else { // draw tick marks and labels
				PlotAxis axisColorbar = new PlotAxis();
				double dDataFrom = mColors.getDataFrom_double(xRange);
				double dDataTo   = mColors.getDataTo_double(xRange);
				axisColorbar.setRange(dDataFrom, dDataTo);
				boolean zOrthogonalLabels = !zColorBarHorizontal;
				boolean zDoBiasAdjustment = mOptions.option_legend_zDoBiasAdjustment;
				double dSlope = mOptions.option_legend_dBiasSlope;
				double dIntercept = mOptions.option_legend_dBiasIntercept;
				if( axisColorbar.zDetermineScaleInterval(iSwatchLength, font, zOrthogonalLabels, g2, zDoBiasAdjustment, dSlope, dIntercept, sbError) ){
					String[] asScaleLabels = axisColorbar.getScaleLabels1();
					int pxLabelHeight = fontmetrics.getHeight();
					int pxTickSize = 5;
					int pxTickLabelOffset = 4; // should be two but for some reason not working
					int pxTick_Across  = acrossSwatch + iSwatchThickness; // not used
					int pxLabel_Across = pxTick_Across + pxTickSize + pxTickLabelOffset; // not used
					int ctTick = axisColorbar.getTickCount();
					int pxTickOffset = axisColorbar.getOffset_LowerPX();
					double pxTickSpacing = axisColorbar.mpxTick_MajorInterval;
					for( int xTick = 1; xTick <= ctTick; xTick++ ){
						int pxTick_Along;
						if( zColorsAscending ){
							pxTick_Along = (int)((xTick-1)*pxTickSpacing) + pxTickOffset;
						} else {
							pxTick_Along = iSwatchLength - (int)((xTick-1)*pxTickSpacing) + pxTickOffset;
						}
						int x_tick, y_tick, pxLabelLeft, pxLabelTop;
						String sTickLabel = asScaleLabels[xTick];
						if( sTickLabel == null ) sTickLabel = "~";
						int iLabelWidth = fontmetrics.stringWidth(sTickLabel);
						if( zColorBarHorizontal ){
							x_tick = alongSwatch + pxTick_Along;
							y_tick = acrossSwatch + iSwatchThickness;
							g2.drawLine(x_tick, y_tick, x_tick, y_tick + pxTickSize);
							pxLabelLeft = x_tick - iLabelWidth / 2 + 1; // don't know why this plus one seems to be needed
							pxLabelTop = y_tick + pxTickSize + pxTickLabelOffset + pxAscent;
						} else {
							x_tick = acrossSwatch + iSwatchThickness;
							y_tick = alongSwatch + iSwatchLength - pxTick_Along;
							g2.drawLine(x_tick, y_tick, x_tick + pxTickSize , y_tick);
							pxLabelLeft = x_tick + pxTickSize + pxTickLabelOffset;
							pxLabelTop = y_tick + pxAscent / 2 + 1; // don't know why this plus one seems to be needed
						}
						g2.drawString(sTickLabel, pxLabelLeft, pxLabelTop);
                        int pxTickLabelLength = fontmetrics.stringWidth(sTickLabel);
                        if( pxTickLabelLength > pxMaxLabelLength ) pxMaxLabelLength = pxTickLabelLength;
					}
				} else {
					ApplicationController.vShowError("Error setting up color bar ticks for range " + xRange +": " + sbError);
				}
			}
		}
	    mpxLegend_Width = pxSwatchWidth + (iRotation == 0 || iRotation == 180 ? 0 : pxMaxLabelLength + 5);
	    mpxLegend_Height = pxSwatchHeight + (iRotation == 0 || iRotation == 180 ? pxMaxLabelLength + 5 : 0);

		// Colorbar text
		mText.remove(TEXT_ID_CaptionColorBar); // there may be a pre-existing, automatically generated caption
		if( msLabel_Values != null ){
			String sCaption = msLabel_Values + (msLabel_Units == null ? "" : msLabel_Units);
			PlotTextItem text = mText.getNew(TEXT_ID_CaptionColorBar);
			text.setExpression(sCaption);
			if( mpxAxis_Vertical_height > 100 ){
				font = Styles.fontSansSerif12;
			} else {
				font = Styles.fontSansSerif10;
			}
			text.setFont(font);
			PlotLayout layoutText = text.getPlotLayout();
			layoutText.setObject(PlotLayout.OBJECT_Legend);
			switch( iRotation ){
				default:
				case 0:
		    		layoutText.setOrientation(PlotLayout.ORIENT_BottomMiddle);
	    	    	layoutText.setAlignment(PlotLayout.ORIENT_TopMiddle);
        			layoutText.setOffsetVertical(10);
					break;
				case 90:
		    		layoutText.setOrientation(PlotLayout.ORIENT_LeftMiddle);
	    	    	layoutText.setAlignment(PlotLayout.ORIENT_RightMiddle);
        			layoutText.setOffsetHorizontal(-10);
					break;
				case 180:
		    		layoutText.setOrientation(PlotLayout.ORIENT_TopMiddle);
	    	    	layoutText.setAlignment(PlotLayout.ORIENT_BottomMiddle);
        			layoutText.setOffsetVertical(-10);
					break;
				case 270:
		    		layoutText.setOrientation(PlotLayout.ORIENT_RightMiddle);
	    	    	layoutText.setAlignment(PlotLayout.ORIENT_LeftMiddle);
        			layoutText.setOffsetHorizontal(10);
					break;
			}
			layoutText.setRotation(iRotation);
		}

	}

	protected void vDrawScale( Graphics2D g2, int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){
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
			case PlotLayout.OBJECT_Canvas:
				hObject = 0;
				vObject = 0;
				heightObject = pxCanvasHeight;
				widthObject  = pxCanvasWidth;
				break;
			case PlotLayout.OBJECT_Plot:
				hObject = mpxMargin_Left;
				vObject = mpxMargin_Top;
				heightObject = pxPlotHeight;
				widthObject  = pxPlotWidth;
				break;
			case PlotLayout.OBJECT_AxisVertical:
				hObject = mpxAxis_Vertical_X;
				vObject = mpxAxis_Vertical_Y;
				heightObject = mpxAxis_Vertical_height;
				widthObject  = mpxAxis_Vertical_width;
				break;
			default:
			case PlotLayout.OBJECT_AxisHorizontal:
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
			Font font = item.getFont();
			if( font == null ) font = Styles.fontSansSerifBold10;
			Color color = item.getColor();
			if( color == null ) if( this.getBackground() == Color.BLACK ) color = Color.WHITE; else color = Color.BLACK;
			String sExpression = item.getExpression();
			String sText = sExpression;
			vDrawText( g2, sText, font, color, layout, pxCanvasWidth, pxCanvasHeight, pxPlotWidth, pxPlotHeight );
		}
	}

	protected void vDrawText( Graphics2D g2, String sText, Font font, Color color, PlotLayout layout, int pxCanvasWidth, int pxCanvasHeight, int pxPlotWidth, int pxPlotHeight ){

		int hObject, vObject, widthObject, heightObject;
		switch( layout.getObject() ){
			default:
			case PlotLayout.OBJECT_Canvas:
				hObject = 0; vObject = 0; widthObject = pxCanvasWidth; heightObject = pxCanvasHeight;
				break;
			case PlotLayout.OBJECT_Plot:
				hObject = mpxMargin_Left + this.mpxAxisOffsetWidth; vObject = mpxMargin_Top; widthObject = pxPlotWidth; heightObject = pxPlotHeight;
				break;
			case PlotLayout.OBJECT_AxisHorizontal:
				hObject = mpxAxis_Horizontal_X; vObject = mpxAxis_Horizontal_Y; widthObject = mpxAxis_Horizontal_width; heightObject = mpxAxis_Horizontal_height;
				break;
			case PlotLayout.OBJECT_AxisVertical:
				hObject = mpxAxis_Vertical_X; vObject = mpxAxis_Vertical_Y; widthObject = mpxAxis_Vertical_width; heightObject = mpxAxis_Vertical_height;
				break;
			case PlotLayout.OBJECT_Legend:
				hObject = mpxLegend_X; vObject = mpxLegend_Y; widthObject = mpxLegend_Width; heightObject = mpxLegend_Height;
				break;
			case PlotLayout.OBJECT_Scale:
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

	// Mouse motion interface
	public void mouseMoved(MouseEvent evt){ }

	// Mouse listener interface
	public void mousePressed(MouseEvent evt){ }
	public void mouseDragged(MouseEvent evt){ }
	public void mouseReleased(MouseEvent evt){ }
	public void mouseEntered(MouseEvent evt) { }
	public void mouseExited(MouseEvent evt) { }
	public void mouseClicked(MouseEvent evt){ }


}




