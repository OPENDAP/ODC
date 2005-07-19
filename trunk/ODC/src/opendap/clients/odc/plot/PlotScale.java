package opendap.clients.odc.plot;

/**
 * Title:        PlotScale
 * Description:  Support for defining plotting scales
 * Copyright:    Copyright (c) 2003
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.38
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.GridBagConstraints;
import java.awt.Dimension;

public class PlotScale {

	final static int SECTION_Margin = 1;
	final static int SECTION_Zoom = 2;
	final static int UNITS_Pixels = 1;
	final static int UNITS_Inches_Tenths = 2;
	final static int UNITS_Inches_Eighths = 3; // integral
	final static int UNITS_Centimeters = 4;
	final static int SCALE_MODE_Canvas = 1;
	final static int SCALE_MODE_PlotArea = 2;
	final static int SCALE_MODE_Output = 3;
	final static int SCALE_MODE_Zoom = 4;
	final static int ZOOM_Max = 1;
	final static int ZOOM_50 = 2;
	final static int ZOOM_75 = 3;
	final static int ZOOM_100 = 4;
	final static int ZOOM_200 = 5;
	final static int ZOOM_300 = 6;
	final static int ZOOM_400 = 7;
	final static int ZOOM_Custom = 8;
	final static String[] AS_Units = { "pixels", "inches", "1/8\"", "cm" };
	final static String[] AS_ScaleMode = { "canvas", "plot area", "output", "zoom" };
	final static String[] AS_ZoomFactor = { "Max", "50%","75%","100%","200%","300%","400%", "Custom" };
	private float mfMarginLeft, mfMarginTop, mfMarginRight, mfMarginBottom;
	int miPixelsPerData, miDataPointsPerPixel;
	private int meMarginUnits = UNITS_Pixels;
	private int meScaleMode = SCALE_MODE_Output;
	private int meScaleUnits = UNITS_Pixels;
	private int meZoom = ZOOM_Max;
	private int miPixelWidth, miPixelHeight;
	public final static int PX_DEFAULT_MARGIN = 50;
	final static float fPIXELS_PER_CM = 28.34645669291f;
	int miDataWidth, miDataHeight;

	IChanged mListener;

	PlotScale(){ // set defaults
		mfMarginLeft = 65;
		mfMarginTop = PX_DEFAULT_MARGIN;
		mfMarginRight = PX_DEFAULT_MARGIN;
		mfMarginBottom = PX_DEFAULT_MARGIN;
		meMarginUnits = UNITS_Pixels;
		miPixelsPerData = 1;
		miDataPointsPerPixel = 1;
		meScaleMode = SCALE_MODE_Output; // peter wants max to be the default
	    meScaleUnits = UNITS_Pixels;
	}

	void setListener( IChanged listener ){
		mListener = listener;
	}

	/**************** GET ******************************/
	int getScaleMode(){ return meScaleMode; }
	int getScaleUnits(){ return meScaleUnits; }
	String getUnits_String(int e){
		if( e < 1 || e > PlotScale.AS_Units.length ) e = 1;
		return PlotScale.AS_Units[e-1];
	}
	String getScaleMode_String(int e){
		if( e < 1 || e > PlotScale.AS_ScaleMode.length ) e = 1;
		return PlotScale.AS_ScaleMode[e-1];
	}
	int getMarginUnits(){ return meMarginUnits; }
	int getMarginLeft_px(){ return getPixels( mfMarginLeft, meMarginUnits); }
	int getMarginTop_px(){ return getPixels( mfMarginTop, meMarginUnits); }
	int getMarginRight_px(){ return getPixels( mfMarginRight, meMarginUnits); }
	int getMarginBottom_px(){ return getPixels( mfMarginBottom, meMarginUnits); }

	int getCanvas_Width( boolean zFillOutput ){
		switch( meScaleMode ){
			case SCALE_MODE_Canvas:
				return getAbsoluteWidth_px();
			case SCALE_MODE_PlotArea:
				return getAbsoluteWidth_px() + getMarginLeft_px() + getMarginRight_px();
			case SCALE_MODE_Output:
				if( zFillOutput ){
					return getOutputWidth();
				} else { // make proportional output
					float fScale = getOutputScale();
					return (int)((float)miDataWidth * fScale) + getMarginLeft_px() + getMarginRight_px();
				}
			default:
			case SCALE_MODE_Zoom:
				int iCanvasWidth = miDataWidth * miPixelsPerData / miDataPointsPerPixel + getMarginLeft_px() + getMarginRight_px();
				return iCanvasWidth;
		}
	}
	int getCanvas_Height( boolean zFillOutput ){
		switch( meScaleMode ){
			case SCALE_MODE_Canvas:
				return getAbsoluteHeight_px();
			case SCALE_MODE_PlotArea:
				return getAbsoluteHeight_px() + getMarginTop_px() + getMarginBottom_px();
			case SCALE_MODE_Output:
				if( zFillOutput ){
					return getOutputHeight();
				} else { // make proportional output
					float fScale = getOutputScale();
					return (int)((float)miDataHeight * fScale) + getMarginTop_px() + getMarginBottom_px();
				}
			default:
			case SCALE_MODE_Zoom:
				return miDataHeight * miPixelsPerData / miDataPointsPerPixel + getMarginTop_px() + getMarginBottom_px();
		}
	}

	int getPlot_Width( boolean zFillOutput ){
		switch( meScaleMode ){
			case SCALE_MODE_Canvas:
				return getAbsoluteWidth_px() - (getMarginLeft_px() + getMarginRight_px());
			case SCALE_MODE_PlotArea:
				return getAbsoluteWidth_px();
			case SCALE_MODE_Output:
				if( zFillOutput ){
					return getOutputWidth() - (getMarginLeft_px() + getMarginRight_px());
				} else { // make proportional output
					float fScale = getOutputScale();
					return (int)((float)miDataWidth * fScale);
				}
			default:
			case SCALE_MODE_Zoom:
				return miDataWidth * miPixelsPerData / miDataPointsPerPixel;
		}
	}

	int getPlot_Height( boolean zFillOutput ){
		switch( meScaleMode ){
			case SCALE_MODE_Canvas:
				return getAbsoluteHeight_px() - (getMarginTop_px() + getMarginBottom_px());
			case SCALE_MODE_PlotArea:
				return getAbsoluteHeight_px();
			case SCALE_MODE_Output:
				if( zFillOutput ){
					return getOutputHeight() - (getMarginTop_px() + getMarginBottom_px());
				} else { // make proportional output
					float fScale = getOutputScale();
					return (int)((float)miDataHeight * fScale);
				}
			default:
			case SCALE_MODE_Zoom:
				return miDataHeight * miPixelsPerData / miDataPointsPerPixel;
		}
	}

	private float getOutputScale(){
		int iOutputWidth = getOutputWidth();
		int iOutputHeight = getOutputHeight();
		if( iOutputWidth == 0 || iOutputHeight == 0 ){
			ApplicationController.vShowWarning("System error: output width/height undetermined, returning default canvas width");
			return 1.0f;
		}
		int pxAvailableWidth = iOutputWidth - getMarginLeft_px() - getMarginRight_px();
		int pxAvailableHeight = iOutputHeight - getMarginTop_px() - getMarginBottom_px();
		float fScaleByWidth = pxAvailableWidth / (float)miDataWidth;
		float fScaleByHeight = pxAvailableHeight / (float)miDataHeight;
		return fScaleByWidth < fScaleByHeight ? fScaleByWidth : fScaleByHeight;
	}

	/**************** Active Accessors ************************/

	void setMarginLeft( float f ){
		mfMarginLeft = f;
		notifyListener(SECTION_Margin);
	}
	void setMarginTop( float f ){
		mfMarginTop = f;
		notifyListener(SECTION_Margin);
	}
	void setMarginRight( float f ){
		mfMarginRight = f;
		notifyListener(SECTION_Margin);
	}
	void setMarginBottom( float f ){
		mfMarginBottom = f;
		notifyListener(SECTION_Margin);
	}
	void setMarginUnits(int eUnits){
		meMarginUnits = eUnits;
		notifyListener(SECTION_Margin);
	}

	void setPixelWidth( int i ){
	    if( i < 0 ) return;
		miPixelWidth = i;
		notifyListener();
	}
	void setPixelHeight( int i ){
		if( i < 0 ) return;
		miPixelHeight = i;
		notifyListener();
	}
	void setUnits( int eUnits ){
	    meScaleUnits = eUnits;
		notifyListener();
	}

	void setScaleMode( int eScaleMode ){
		meScaleMode = eScaleMode;
		notifyListener();
	}

	void setScaleRatio( int iPixelsPerDataPoint, int iDataPointsPerPixel ){
		miPixelsPerData = iPixelsPerDataPoint;
		miDataPointsPerPixel = iDataPointsPerPixel;
		notifyListener();
	}

	void setDataDimension( int iNewWidth, int iNewHeight ){
		if( iNewWidth < 1 || iNewHeight < 1 ){
			ApplicationController.vShowWarning("invalid output dimensions (" + iNewWidth + ", " + iNewHeight + "), change ignored by scale");
		} else {
			miDataWidth = iNewWidth;
			miDataHeight = iNewHeight;
			notifyListener();
		}
	}

	boolean zZoomIn(){
		if( meZoom == ZOOM_400 || meZoom == ZOOM_Custom ) return false; // already big (or custom)
		if( meZoom == ZOOM_Max ){
			meZoom = ZOOM_100;
		} else {
			meZoom++;
		}
		notifyListener(SECTION_Zoom);
		return true;
	}

	boolean zZoomOut(){
		if( meZoom == ZOOM_50 || meZoom == ZOOM_Custom ) return false; // already small (or custom)
		if( meZoom == ZOOM_Max ){
			meZoom = ZOOM_100;
		} else {
			meZoom--;
		}
		notifyListener(SECTION_Zoom);
		return true;
	}

	boolean zZoomMaximize(){
		if( meZoom == ZOOM_Max ) return false; // already maxed
		meZoom = ZOOM_Max;
		notifyListener(SECTION_Zoom);
		return true;
	}

	private void setPixelsPerData(int i){
		if( i >= 0 ){
			miPixelsPerData = i;
			notifyListener();
		}
	}
	private void setDataPointsPerPixel(int i){
		if( i >= 0 ){
			miDataPointsPerPixel = i;
			notifyListener();
		}
	}

	private void notifyListener(){
		if( mListener != null ) mListener.update();
	}

	private void notifyListener(int eSection){
		if( mListener != null ) mListener.update(eSection);
	}

	///////////////// Passive Accessors

	int getPixels( float f, int eUnits ){
		switch( eUnits ){
			case UNITS_Inches_Tenths:
				return (int)(f*72);
			case UNITS_Inches_Eighths:
				return (int)(f*9);
			case UNITS_Centimeters:
				return (int)(f*fPIXELS_PER_CM);
			case UNITS_Pixels:
			default:
				return (int)f;
		}
	}
	float getUnits( int pixels, int eUnits ){
		switch( eUnits ){
			case UNITS_Inches_Tenths:
				return (float)pixels/72;
			case UNITS_Inches_Eighths:
				return (float)pixels/9;
			case UNITS_Centimeters:
				return (float)pixels/fPIXELS_PER_CM;
			case UNITS_Pixels:
			default:
				return (float)pixels;
		}
	}
	private int getAbsoluteHeight_px(){
		return miPixelHeight;
	}
	private int getAbsoluteWidth_px(){
		return miPixelWidth;
	}

	private int getPixelsPerData(){ return miPixelsPerData; }
	private int getDataPointsPerPixel(){ return miDataPointsPerPixel; }

	public int getOutputWidth(){
		int eOutputOption = Panel_View_Plot.getOutputOption();
		switch( eOutputOption ){
			case Output_ToPlot.FORMAT_ExternalWindow:
				return Output_ToPlot.mPlotFrame.getWidth();
			case Output_ToPlot.FORMAT_NewWindow:
				return miDataWidth + (int)mfMarginLeft + (int)mfMarginRight;
			case Output_ToPlot.FORMAT_FullScreen:
				Dimension dimScreenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
				return (int)dimScreenSize.getWidth();
			case Output_ToPlot.FORMAT_Print:
				return miDataWidth + (int)mfMarginLeft + (int)mfMarginRight; // cannot figure out this size ahead of time
			case Output_ToPlot.FORMAT_PreviewPane:
				// Dimension dimView = Output_ToPlot.mPreviewScrollPane.getSize(); // does not work: .getViewport().getViewSize();
				Dimension dimView = Panel_View_Plot.getPreviewPane().getSize();
//				Dimension dimTabbed = Panel_View_Plot.getTabbedPane().getSize();
				int iPreviewPaneWidth = (int)dimView.getWidth();
//				int iTabbedPaneWidth = (int)dimTabbed.getWidth();
//System.out.println(" preview width: " + iPreviewPaneWidth + " tabbed pane width: " + iTabbedPaneWidth);
				return iPreviewPaneWidth;
			case Output_ToPlot.FORMAT_File_PNG:
				return miDataWidth + (int)mfMarginLeft + (int)mfMarginRight;
			case Output_ToPlot.FORMAT_Thumbnail:
				return miDataWidth + (int)mfMarginLeft + (int)mfMarginRight;
			default:
				ApplicationController.vShowWarning("unknown output format to getOutputWidth");
				return 0;
		}
	}

	public int getOutputHeight(){
		int eOutputOption = Panel_View_Plot.getOutputOption();
		switch( eOutputOption ){
			case Output_ToPlot.FORMAT_ExternalWindow:
				return Output_ToPlot.mPlotFrame.getHeight();
			case Output_ToPlot.FORMAT_NewWindow:
				return miDataHeight + (int)mfMarginTop + (int)mfMarginBottom;
			case Output_ToPlot.FORMAT_FullScreen:
				Dimension dimScreenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
				return (int)dimScreenSize.getHeight();
			case Output_ToPlot.FORMAT_Print:
				return miDataHeight + (int)mfMarginTop + (int)mfMarginBottom; // cannot figure out this size ahead of time
			case Output_ToPlot.FORMAT_PreviewPane:
				Dimension dimView = Panel_View_Plot.getPreviewPane().getSize();
				int iPreviewPaneHeight = (int)dimView.getHeight();
				return iPreviewPaneHeight;
			case Output_ToPlot.FORMAT_File_PNG:
				return miDataHeight + (int)mfMarginTop + (int)mfMarginBottom; // cannot figure out this size ahead of time
			case Output_ToPlot.FORMAT_Thumbnail:
				return miDataHeight + (int)mfMarginTop + (int)mfMarginBottom; // cannot figure out this size ahead of time
			default:
				ApplicationController.vShowWarning("unknown output format to getOutputHeight");
				return 0;
		}
	}

	public String toString(){
		StringBuffer sb = new StringBuffer(250);
		sb.append("Scale {\n");
		sb.append("\tmargin_units: " + getUnits_String(meMarginUnits) + "\n");
		sb.append("\tmargin_left: " + mfMarginLeft + "\n");
		sb.append("\tmargin_top: " + mfMarginTop + "\n");
		sb.append("\tmargin_right: " + mfMarginRight + "\n");
		sb.append("\tmargin_bottom: " + mfMarginBottom + "\n");
		sb.append("\tpixels_per_data: " + miPixelsPerData + "\n");
		sb.append("\tdata_per_pixel: " + miDataPointsPerPixel + "\n");
		sb.append("\tscale_mode: " + getScaleMode_String(meScaleMode) + "\n");
		sb.append("\tpixel_height: " + miPixelHeight + "\n");
		sb.append("\tpixel_width: " + miPixelWidth + "\n");
		sb.append("\tzoom: " + getZoomDescriptor(meZoom) + "\n");
		sb.append("}\n");
		return sb.toString();
	}

	private String getZoomDescriptor( int eZoom ){
		switch( eZoom ){
			case ZOOM_Max: return "Max";
			case ZOOM_50: return "50";
			case ZOOM_75: return "75";
			case ZOOM_100: return "100";
			case ZOOM_200: return "200";
			case ZOOM_300: return "300";
			case ZOOM_400: return "400";
			case ZOOM_Custom: return "Custom";
			default: return "?";
		}
	}

	private int getZoomFromDescriptor( String sDescriptor ){
		for( int eZoom = 1; eZoom <= ZOOM_Custom; eZoom++ ){
			if( getZoomDescriptor(eZoom).equalsIgnoreCase(sDescriptor) ) return eZoom;
		}
		return ZOOM_Max;
	}

}

class Panel_PlotScale extends JPanel implements IChanged {

	PlotScale mScale = null;
	JComboBox jcbMarginUnits = new JComboBox(PlotScale.AS_Units);
	JComboBox jcbScaleUnits = new JComboBox(PlotScale.AS_Units);
	JComboBox jcbZoomFactor = new JComboBox(PlotScale.AS_ZoomFactor);
	JRadioButton jrbPixelsPerData = new JRadioButton();
	JRadioButton jrbEntireCanvas = new JRadioButton("Entire Canvas");
	JRadioButton jrbPlotArea = new JRadioButton("Plot Area");
	JTextField jtfMarginLeft = new JTextField(6);
	JTextField jtfMarginTop = new JTextField(6);
	JTextField jtfMarginRight = new JTextField(6);
	JTextField jtfMarginBottom = new JTextField(6);
	JTextField jtfPixelsPerData = new JTextField(5);
	JTextField jtfDataPointsPerPixel = new JTextField(5);
	JTextField jtfWidth = new JTextField(6);
	JTextField jtfHeight = new JTextField(6);
	JCheckBox jcheckAspectRatio = new JCheckBox("Maintain Aspect Ratio: ");
	Panel_PlotScale(){

		JLabel labMarginTitle = new JLabel("Margins: ");
		JLabel labMarginLeft = new JLabel("Left: ", JLabel.RIGHT);
		JLabel labMarginTop = new JLabel("Top: ", JLabel.RIGHT);
		JLabel labMarginBottom = new JLabel("Bottom: ", JLabel.RIGHT);
		JLabel labMarginRight = new JLabel("Right: ", JLabel.RIGHT);
		JLabel labMarginUnits = new JLabel("Margin Units: ", JLabel.RIGHT);
		JLabel labScale = new JLabel("Scale", JLabel.RIGHT);
		JLabel labScaleUnits = new JLabel("Units: ", JLabel.RIGHT);
		JLabel labZoom = new JLabel("Zoom Factor: ", JLabel.RIGHT);
		JLabel labWidth = new JLabel("Width: ", JLabel.RIGHT);
		JLabel labHeight = new JLabel("Height: ", JLabel.RIGHT);
		javax.swing.ButtonGroup bg = new javax.swing.ButtonGroup();
		bg.add(jrbPixelsPerData);
		bg.add(jrbEntireCanvas);
		bg.add(jrbPlotArea);
		jrbPixelsPerData.setSelected(true);
		vSetupListeners();

		JPanel panelScale = new JPanel();
		panelScale.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Plot Scale"));
		panelScale.setLayout(new java.awt.GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.BOTH;

		// margin units
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labMarginUnits, gbc);
		gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jcbMarginUnits, gbc);
		gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5;
		panelScale.add(Box.createVerticalStrut(4), gbc);

		// margin left
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labMarginLeft, gbc);
		gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfMarginLeft, gbc);
		gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// margin top
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labMarginTop, gbc);
		gbc.gridx = 2; gbc.gridy = 3; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfMarginTop, gbc);
		gbc.gridx = 3; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// margin right
		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labMarginRight, gbc);
		gbc.gridx = 2; gbc.gridy = 4; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfMarginRight, gbc);
		gbc.gridx = 3; gbc.gridy = 4; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// margin bottom
		gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labMarginBottom, gbc);
		gbc.gridx = 2; gbc.gridy = 5; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfMarginBottom, gbc);
		gbc.gridx = 3; gbc.gridy = 5; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// zoom
		gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labZoom, gbc);
		gbc.gridx = 2; gbc.gridy = 7; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jcbZoomFactor, gbc);
		gbc.gridx = 3; gbc.gridy = 7; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// absolute - canvas
		gbc.gridx = 0; gbc.gridy = 9; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 9; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(Box.createVerticalStrut(10), gbc);
		gbc.gridx = 2; gbc.gridy = 9; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jrbEntireCanvas, gbc);
		gbc.gridx = 3; gbc.gridy = 9; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// absolute - plot area
		gbc.gridx = 0; gbc.gridy = 10; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 10; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(Box.createVerticalStrut(10), gbc);
		gbc.gridx = 2; gbc.gridy = 10; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jrbPlotArea, gbc);
		gbc.gridx = 3; gbc.gridy = 10; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 11; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// absolute scale units
		gbc.gridx = 0; gbc.gridy = 12; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 12; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labScaleUnits, gbc);
		gbc.gridx = 2; gbc.gridy = 12; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jcbScaleUnits, gbc);
		gbc.gridx = 3; gbc.gridy = 12; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 13; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// absolute - width
		gbc.gridx = 0; gbc.gridy = 14; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 14; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labWidth, gbc);
		gbc.gridx = 2; gbc.gridy = 14; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfWidth, gbc);
		gbc.gridx = 3; gbc.gridy = 14; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// absolute - height
		gbc.gridx = 0; gbc.gridy = 15; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 15; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labHeight, gbc);
		gbc.gridx = 2; gbc.gridy = 15; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfHeight, gbc);
		gbc.gridx = 3; gbc.gridy = 15; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 16; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// aspect ratio
		gbc.gridx = 0; gbc.gridy = 17; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 17; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(Box.createHorizontalStrut(4), gbc);
		gbc.gridx = 2; gbc.gridy = 17; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jcheckAspectRatio, gbc);
		gbc.gridx = 3; gbc.gridy = 17; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(panelScale);

		update();
	}

	public void update(int eSection){
		update(); // ignore section for now
	}

	// when the scale changes or is set then this method makes the panel reflect the
	// values in the scale data structure
	boolean mzUpdating = false;
	public void update(){
		if( mzUpdating ) return; // prevent re-entrancy
		try {
			mzUpdating = true;
			boolean zEnable = (mScale != null);
			jcbMarginUnits.setEnabled(zEnable);
			jcbScaleUnits.setEnabled(zEnable);
			jcbZoomFactor.setEnabled(zEnable);
			jrbEntireCanvas.setEnabled(zEnable);
			jrbPlotArea.setEnabled(zEnable);
			jtfMarginLeft.setEnabled(zEnable);
			jtfMarginTop.setEnabled(zEnable);
			jtfMarginRight.setEnabled(zEnable);
			jtfMarginBottom.setEnabled(zEnable);
			jtfWidth.setEnabled(zEnable);
			jtfHeight.setEnabled(zEnable);
			jcheckAspectRatio.setEnabled(zEnable);
			jcheckAspectRatio.setSelected(true);
			if( mScale == null ) return;
			jcbMarginUnits.setSelectedIndex(mScale.getMarginUnits()-1);
			jcbScaleUnits.setSelectedIndex(mScale.getScaleUnits()-1);

			int eScaleMode = mScale.getScaleMode();

			// set zoom
			if( eScaleMode == PlotScale.SCALE_MODE_Output ){
				jcbZoomFactor.setSelectedIndex(0);
			} else if( eScaleMode == PlotScale.SCALE_MODE_Zoom ){
				int iPPD = mScale.miPixelsPerData;
				int iDPP = mScale.miDataPointsPerPixel;
				float fScaleRatio = (float)iPPD / (float)iDPP;
				if( fScaleRatio == 0.50f ){
					jcbZoomFactor.setSelectedIndex(1);
				} else if( fScaleRatio == 0.75f ){
					jcbZoomFactor.setSelectedIndex(2);
				} else if( fScaleRatio == 1.00f ){
					jcbZoomFactor.setSelectedIndex(3);
				} else if( fScaleRatio == 2.0f ){
					jcbZoomFactor.setSelectedIndex(4);
				} else if( fScaleRatio == 3.0f ){
					jcbZoomFactor.setSelectedIndex(5);
				} else if( fScaleRatio == 4.0f ){
					jcbZoomFactor.setSelectedIndex(6);
				}
			} else { // custom
				jcbZoomFactor.setSelectedIndex(7);
			}

			// set mode radios
			switch( eScaleMode ){
				case PlotScale.SCALE_MODE_Canvas:
					jrbEntireCanvas.setSelected(true);
					jrbPlotArea.setSelected(false);
					break;
				case PlotScale.SCALE_MODE_PlotArea:
					jrbEntireCanvas.setSelected(false);
					jrbPlotArea.setSelected(true);
					break;
				case PlotScale.SCALE_MODE_Output:
				case PlotScale.SCALE_MODE_Zoom:
					jrbEntireCanvas.setSelected(false);
					jrbPlotArea.setSelected(true);
					break;
			}

			// set absolute width/height
			int eUNITS = mScale.getScaleUnits();
			int pxWidth, pxHeight;
			if( jrbEntireCanvas.isSelected() ){
				pxWidth = mScale.getCanvas_Width(false);
				pxHeight = mScale.getCanvas_Height(false);
			} else {
				pxWidth = mScale.getPlot_Width(false);
				pxHeight = mScale.getPlot_Height(false);
			}
			mScale.setPixelWidth(pxWidth);
			mScale.setPixelHeight(pxHeight);
			float fWidth = mScale.getUnits( pxWidth, eUNITS );
			float fHeight = mScale.getUnits( pxHeight, eUNITS );
			if( fWidth == (int)fWidth ){
				jtfWidth.setText(Integer.toString((int)fWidth));
			} else {
				jtfWidth.setText(Float.toString(fWidth));
			}
			if( fHeight == (int)fHeight ){
				jtfHeight.setText(Integer.toString((int)fHeight));
			} else {
				jtfHeight.setText(Float.toString(fHeight));
			}

			// enable width/height/aspect
			boolean zAbsoluteEnabled = (eScaleMode == PlotScale.SCALE_MODE_Canvas) || (eScaleMode == PlotScale.SCALE_MODE_PlotArea);
			jrbEntireCanvas.setEnabled(zAbsoluteEnabled);
			jrbPlotArea.setEnabled(zAbsoluteEnabled);
			jcbScaleUnits.setEnabled(zAbsoluteEnabled);
			jtfWidth.setEnabled(zAbsoluteEnabled);
			jtfHeight.setEnabled(zAbsoluteEnabled);
			jcheckAspectRatio.setEnabled(zAbsoluteEnabled);

			// set margin text boxes
			jtfMarginLeft.setText(Integer.toString(mScale.getMarginLeft_px()));
			jtfMarginTop.setText(Integer.toString(mScale.getMarginTop_px()));
			jtfMarginRight.setText(Integer.toString(mScale.getMarginRight_px()));
			jtfMarginBottom.setText(Integer.toString(mScale.getMarginBottom_px()));
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, "while updating scale panel");
		} finally {
			mzUpdating = false;
		}
	}

	PlotScale getScale(){ return mScale; }
	void setScale( PlotScale scale ){
		mScale = scale;
		update();
	}

	void changeDataDimension( int iNewWidth, int iNewHeight ){
		if( mScale != null ){
			mScale.setDataDimension( iNewWidth, iNewHeight );
		}
	}

	private void vSetupListeners(){
		jcbScaleUnits.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					int xSelected = jcbScaleUnits.getSelectedIndex();
					mScale.setUnits(xSelected + 1);
				}
			}
		);
		jcbMarginUnits.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					int xSelected = jcbMarginUnits.getSelectedIndex();
					mScale.setMarginUnits(xSelected + 1);
				}
			}
		);
		jcbZoomFactor.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					vZoomChanged();
				}
			}
		);
		jrbEntireCanvas.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					mScale.setScaleMode(PlotScale.SCALE_MODE_Canvas);
					update();
				}
			}
		);
		jrbPlotArea.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					mScale.setScaleMode(PlotScale.SCALE_MODE_PlotArea);
					update();
				}
			}
		);
		jtfWidth.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						vUpdateWidth();
					} catch(Exception ex){} // ignore invalid entries
				}
			}
		);
//		jtfWidth.addKeyListener(
//	    	new java.awt.event.KeyListener(){
//		    	public void keyPressed(java.awt.event.KeyEvent ke){
//			    	if( ke.getKeyCode() == ke.VK_ENTER ){
//						vUpdateWidth();
//	    			}
//		    	}
//			    public void keyReleased(java.awt.event.KeyEvent ke){}
//				public void keyTyped(java.awt.event.KeyEvent ke){}
//			}
//		);
		jtfHeight.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						vUpdateHeight();
					} catch(Exception ex){} // ignore invalid entries
				}
			}
		);
//		jtfHeight.addKeyListener(
//	    	new java.awt.event.KeyListener(){
//		    	public void keyPressed(java.awt.event.KeyEvent ke){
//			    	if( ke.getKeyCode() == ke.VK_ENTER ){
//						vUpdateHeight();
//	    			}
//		    	}
//			    public void keyReleased(java.awt.event.KeyEvent ke){}
//				public void keyTyped(java.awt.event.KeyEvent ke){}
//			}
//		);
		jtfMarginLeft.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						mScale.setMarginLeft( Float.parseFloat(jtfMarginLeft.getText()) );
					} catch(Exception ex){
						jtfMarginLeft.setText("50");
						mScale.setMarginLeft( 50 );
					}
				}
			}
		);
		jtfMarginTop.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						mScale.setMarginTop( Float.parseFloat(jtfMarginTop.getText()) );
					} catch(Exception ex){
						jtfMarginTop.setText("50");
						mScale.setMarginTop( 50 );
					}
				}
			}
		);
		jtfMarginRight.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						mScale.setMarginRight( Float.parseFloat(jtfMarginRight.getText()) );
					} catch(Exception ex){
						jtfMarginRight.setText("50");
						mScale.setMarginRight( 50 );
					}
				}
			}
		);
		jtfMarginBottom.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						mScale.setMarginBottom( Float.parseFloat(jtfMarginBottom.getText()) );
					} catch(Exception ex){
						jtfMarginBottom.setText("50");
						mScale.setMarginBottom( 50 );
					}
				}
			}
		);
	}

	private void vZoomChanged(){
		PlotScale scale = getScale();
		if( scale == null ) return; // cannot do anything
		int xSelected = jcbZoomFactor.getSelectedIndex();
		int iPPD, iDPP;
		switch( xSelected ){
			case 0: // max
				iPPD = 1; iDPP = 1; // will be ignored
				scale.setScaleMode(PlotScale.SCALE_MODE_Output);
				break;
			case 1: // 50%
				iPPD = 1; iDPP = 2;
				scale.setScaleMode(PlotScale.SCALE_MODE_Zoom);
				break;
			case 2: // 75%
				iPPD = 3; iDPP = 4;
				scale.setScaleMode(PlotScale.SCALE_MODE_Zoom);
				break;
			case 3: // 100%
				iPPD = 1; iDPP = 1;
				scale.setScaleMode(PlotScale.SCALE_MODE_Zoom);
				break;
			case 4: // 200%
				iPPD = 2; iDPP = 1;
				scale.setScaleMode(PlotScale.SCALE_MODE_Zoom);
				break;
			case 5: // 300%
				iPPD = 3; iDPP = 1;
				scale.setScaleMode(PlotScale.SCALE_MODE_Zoom);
				break;
			case 6: // 400%
				iPPD = 4; iDPP = 1;
				scale.setScaleMode(PlotScale.SCALE_MODE_Zoom);
				break;
			case 7: // custom
				iPPD = 1; iDPP = 1; // will be ignored
				if( jrbEntireCanvas.isSelected() ){
					scale.setScaleMode(PlotScale.SCALE_MODE_Canvas);
				} else {
					scale.setScaleMode(PlotScale.SCALE_MODE_PlotArea);
				}
				break;
			default: return; // do nothing
		}
		scale.setScaleRatio( iPPD, iDPP );
		update();
	}

	void vUpdateWidth(){
		float fWidth;
		String sWidth = jtfWidth.getText();
	    try {
			fWidth = Float.parseFloat(sWidth);
			if( fWidth <= 0 ){
				ApplicationController.vShowError("Plot dimensions must be positive numbers");
				return;
			}
		} catch( NumberFormatException ex ) {
			ApplicationController.vShowError("Unable to interpret " + sWidth + " as a positive floating point number");
			return;
		}
		int eScaleUnits = mScale.getScaleUnits();
		float fWidth_pixels = mScale.getPixels(fWidth, eScaleUnits);
		mScale.setPixelWidth( (int)fWidth_pixels );
		if( jcheckAspectRatio.isSelected() ){  // scale to aspect ratio
			float fRatio = (float)mScale.miDataHeight / (float)mScale.miDataWidth;
			float fHeight = fWidth * fRatio;
			float fHeight_pixels = mScale.getPixels(fHeight, eScaleUnits);
			mScale.setPixelHeight((int)fHeight_pixels);
			if( fHeight == (int)fHeight ){
				jtfHeight.setText(Integer.toString((int)fHeight));
			} else {
	    		jtfHeight.setText(Float.toString(fHeight));
			}
		}
	}
	void vUpdateHeight(){
		float fHeight;
		String sHeight = jtfHeight.getText();
	    try {
			fHeight = Float.parseFloat(sHeight);
			if( fHeight <= 0 ){
				ApplicationController.vShowError("Plot dimensions must be positive numbers");
				return;
			}
		} catch( NumberFormatException ex ) {
			ApplicationController.vShowError("Unable to interpret " + sHeight + " as a positive floating point number");
			return;
		}
		int eScaleUnits = mScale.getScaleUnits();
		float fHeight_pixels = mScale.getPixels(fHeight, eScaleUnits);
		mScale.setPixelHeight( (int)fHeight_pixels );
		if( jcheckAspectRatio.isSelected() ){  // scale to aspect ratio
			float fRatio = (float)mScale.miDataWidth / (float)mScale.miDataHeight;
			float fWidth = fHeight * fRatio;
			float fWidth_pixels = mScale.getPixels(fWidth, eScaleUnits);
			mScale.setPixelWidth((int)fWidth_pixels);
			if( fWidth == (int)fWidth ){
				jtfWidth.setText(Integer.toString((int)fWidth));
			} else {
	    		jtfWidth.setText(Float.toString(fWidth));
			}
		}
	}
}



