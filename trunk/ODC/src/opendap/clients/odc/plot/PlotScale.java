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
 * Title:        PlotScale
 * Description:  Support for defining plotting scales
 * Copyright:    Copyright (c) 2003-2010
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.06
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Resources;
import opendap.clients.odc.Utility;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JButton;
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
	
	public static enum UNITS {
		Pixels,
		Inches_Tenths,
		Inches_Eighths,
		Centimeters
	};
	
	public static enum SCALE_MODE {
		Canvas,
		PlotArea,
		Output,
		Zoom
	}
		
	public static enum ZOOM_FACTOR {
		Max,
		Zoom50,
		Zoom75,
		Zoom100,
		Zoom200,
		Zoom300,
		Zoom400,
		Custom
	}
	final static String[] AS_Units = { "pixels", "inches", "1/8\"", "cm" };
	final static String[] AS_ScaleMode = { "canvas", "plot area", "output", "zoom" };
	final static String[] AS_ZoomFactor = { "Max", "50%","75%","100%","200%","300%","400%", "Custom" };
	final static int[] AI_ZoomFactor = { 0, 50, 75, 100, 200, 300, 400, 0 };
	public final static int PX_DEFAULT_MARGIN = 50;

	// independent values
	int miDataWidth;  // the number of data elements in the x-dimension
	int miDataHeight; // the number of data elements in the y-dimension
	int dpiOutput;
	private SCALE_MODE meScaleMode = SCALE_MODE.Zoom;
	private ZOOM_FACTOR meZoomFactor = ZOOM_FACTOR.Max;
	private float mfMarginLeft, mfMarginTop, mfMarginRight, mfMarginBottom;
	private UNITS meMarginUnits = UNITS.Pixels;
	private UNITS meScaleUnits = UNITS.Pixels;
	private Dimension dimOutput = null;

	// dependent values
	private int miPixelsPerData;
	private int miDataPointsPerPixel;
	private int miPixelWidth_PlotArea;
	private int miPixelHeight_PlotArea;
	private int miPixelWidth_Canvas;
	private int miPixelHeight_Canvas;
	private int miMarginLeft, miMarginTop, miMarginRight, miMarginBottom;
	
	IChanged mListener;

	PlotScale(){ // set defaults
		dpiOutput = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
		mfMarginLeft = 65;
		mfMarginTop = PX_DEFAULT_MARGIN;
		mfMarginRight = PX_DEFAULT_MARGIN;
		mfMarginBottom = PX_DEFAULT_MARGIN;
		meMarginUnits = UNITS.Pixels;
		miPixelsPerData = 1;
		miDataPointsPerPixel = 1;
		meScaleMode = SCALE_MODE.Zoom; // peter wants max to be the default
	    meScaleUnits = UNITS.Pixels;
	}

	void setListener( IChanged listener ){
		mListener = listener;
	}

	/**************** GET ******************************/
	int getOutputResolution(){ return this.dpiOutput; }
	SCALE_MODE getScaleMode(){ return meScaleMode; }
	UNITS getScaleUnits(){ return meScaleUnits; }
	String getUnits_String(int e){
		if( e < 1 || e > PlotScale.AS_Units.length ) e = 1;
		return PlotScale.AS_Units[e-1];
	}
	String getScaleMode_String(int e){
		if( e < 1 || e > PlotScale.AS_ScaleMode.length ) e = 1;
		return PlotScale.AS_ScaleMode[e-1];
	}
	UNITS getMarginUnits(){ return meMarginUnits; }
	int getMarginLeft_px(){ return getPixels( mfMarginLeft, meMarginUnits); }
	int getMarginTop_px(){ return getPixels( mfMarginTop, meMarginUnits); }
	int getMarginRight_px(){ return getPixels( mfMarginRight, meMarginUnits); }
	int getMarginBottom_px(){ return getPixels( mfMarginBottom, meMarginUnits); }

	int getCanvas_Width(){
		return miPixelWidth_Canvas;
	}
	int getCanvas_Height(){
		return miPixelHeight_Canvas;
	}

	int getPlot_Width(){
		return miPixelWidth_PlotArea;
	}

	int getPlot_Height(){
		return miPixelHeight_PlotArea;
	}

//	private float getOutputScale(){
//		int iOutputWidth = getOutputWidth();
//		int iOutputHeight = getOutputHeight();
//		if( iOutputWidth == 0 || iOutputHeight == 0 ){
//			ApplicationController.vShowWarning("System error: output width/height undetermined, returning default canvas width");
//			return 1.0f;
//		}
//		int pxAvailableWidth = iOutputWidth - getMarginLeft_px() - getMarginRight_px();
//		int pxAvailableHeight = iOutputHeight - getMarginTop_px() - getMarginBottom_px();
//		float fScaleByWidth = pxAvailableWidth / (float)miDataWidth;
//		float fScaleByHeight = pxAvailableHeight / (float)miDataHeight;
//		return fScaleByWidth < fScaleByHeight ? fScaleByWidth : fScaleByHeight;
//	}

	/**************** Active Accessors ************************/

	void setOuputDimensions( Dimension dimOutput ){
		this.dimOutput = dimOutput;
	}
	
	void setOutputResolution( int iNewOutputResolution ){
		if( this.dpiOutput == iNewOutputResolution ) return;
		this.dpiOutput = iNewOutputResolution;
		notifyListener();
	}
	
	void setOutputToScreenResolution(){
		int dpiScreen = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
		setOutputResolution( dpiScreen ); 
	}
	
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
	void setMarginUnits( UNITS eUnits ){
		meMarginUnits = eUnits;
		notifyListener(SECTION_Margin);
	}

	/** the pixel width for the canvas */
	void setPixelWidth_Canvas( int i ){
		if( ! ( meScaleMode == SCALE_MODE.Canvas) ){
			ApplicationController.getInstance().vShowError_NoModal( "internal error, attempt to set canvas width outside of canvas scale mode" );
			return;
		}
		if( i <= ( miMarginLeft + miMarginRight ) ){
			ApplicationController.getInstance().vShowError_NoModal( "internal error, attempt to set canvas width to less than margins" );
			return;
		}
		miPixelWidth_Canvas = i;
		miPixelWidth_PlotArea = miPixelWidth_Canvas - ( miMarginLeft + miMarginRight );
	}
	
	/** the pixel height for the canvas */
	void setPixelHeight_Canvas( int i ){
		if( ! ( meScaleMode == SCALE_MODE.Canvas) ){
			ApplicationController.getInstance().vShowError_NoModal( "internal error, attempt to set canvas height outside of canvas scale mode" );
			return;
		}
		if( i <= ( miMarginTop + miMarginBottom ) ){
			ApplicationController.getInstance().vShowError_NoModal( "internal error, attempt to set canvas height to less than margins" );
			return;
		}
		miPixelHeight_Canvas = i;
		miPixelHeight_PlotArea = miPixelHeight_Canvas - ( miMarginTop + miMarginBottom );
	}

	/** the pixel width for the plot area */
	void setPixelWidth_PlotArea( int i ){
		if( ! ( meScaleMode == SCALE_MODE.PlotArea ) ){
			ApplicationController.getInstance().vShowError_NoModal( "internal error, attempt to set plot area width outside of plot area scale mode" );
			return;
		}
		if( i <= 0 ){
			ApplicationController.getInstance().vShowError_NoModal( "internal error, attempt to set plot area width to " + i );
			return;
		}
		miPixelWidth_PlotArea = i;
		miPixelWidth_Canvas = miPixelWidth_PlotArea + ( miMarginLeft + miMarginRight );
	}
	
	/** the pixel height for the plot area */
	void setPixelHeight_PlotArea( int i ){
		if( ! ( meScaleMode == SCALE_MODE.Canvas) ){
			ApplicationController.getInstance().vShowError_NoModal( "internal error, attempt to set plot area height outside of plot area scale mode" );
			return;
		}
		if( i <= 0 ){
			ApplicationController.getInstance().vShowError_NoModal( "internal error, attempt to set plot area height to " + i );
			return;
		}
		miPixelHeight_PlotArea = i;
		miPixelHeight_Canvas = miPixelHeight_PlotArea + ( miMarginTop + miMarginBottom );
	}
	
	void setUnits( UNITS eUnits ){
	    meScaleUnits = eUnits;
		vUpdateValues();
	}

	void setScaleMode( SCALE_MODE eScaleMode ){
		meScaleMode = eScaleMode;
		vUpdateValues();
	}

	void setScaleRatio( int iPixelsPerDataPoint, int iDataPointsPerPixel ){
		miPixelsPerData = iPixelsPerDataPoint;
		miDataPointsPerPixel = iDataPointsPerPixel;
		notifyListener();
	}

	/** This sets the "Data Dimension" which is the number of elements in the data array for a given dimension. */
	void setDataDimension( int iNewWidth, int iNewHeight ){
		if( iNewWidth < 1 || iNewHeight < 1 ){
			ApplicationController.vShowWarning("invalid output dimensions (" + iNewWidth + ", " + iNewHeight + "), change ignored by scale");
		} else {
			miDataWidth = iNewWidth;
			miDataHeight = iNewHeight;
			vUpdateValues();
		}
	}

	// independent values
//	int miDataWidth;  // the number of data elements in the x-dimension
//	int miDataHeight; // the number of data elements in the y-dimension
//	int dpiOutput;
//	private SCALE_MODE meScaleMode = SCALE_MODE.Zoom;
//	private ZOOM_FACTOR meZoomFactor = ZOOM_FACTOR.Max;
//	private float mfMarginLeft, mfMarginTop, mfMarginRight, mfMarginBottom;
//	private int meMarginUnits = UNITS_Pixels;
//	private int meScaleUnits = UNITS_Pixels;

	// dependent values
//	int miPixelsPerData, miDataPointsPerPixel;
//	private int miPixelWidth; // pixel width value, may be width of canvas or plot area depending on scale mode
//	private int miPixelHeight; // pixel height value, may be height of canvas or plot area depending on scale mode
	
	void vUpdateValues(){
		int iOutputWidth_px = getOutputWidth();
		int iOutputHeight_px = getOutputHeight();
		switch( meScaleMode ){
			case Canvas:
			case PlotArea:
				// in this case the 
				return;
			case Output:
				miPixelWidth_Canvas = iOutputWidth_px;
				miPixelHeight_Canvas = iOutputHeight_px;
				vAdjustPlotAreaToCanvas();
				break;
			case Zoom:
				switch( meZoomFactor ){
					case Max:
						miPixelWidth_Canvas = iOutputWidth_px;
						miPixelHeight_Canvas = iOutputHeight_px;
						vAdjustPlotAreaToCanvas();
						break;
					case Zoom50:
						miPixelWidth_PlotArea = miDataWidth / 2;
						miPixelHeight_PlotArea = miDataHeight / 2;
						vAdjustCanvasToPlotArea();
						break;
					case Zoom75:
						miPixelWidth_PlotArea = miDataWidth * 3 / 4;
						miPixelHeight_PlotArea = miDataHeight * 3 / 4;
						vAdjustCanvasToPlotArea();
						break;
					case Zoom100:
						miPixelWidth_PlotArea = miDataWidth;
						miPixelHeight_PlotArea = miDataHeight;
						vAdjustCanvasToPlotArea();
						break;
					case Zoom200:
						miPixelWidth_PlotArea = miDataWidth * 2;
						miPixelHeight_PlotArea = miDataHeight * 2;
						vAdjustCanvasToPlotArea();
						break;
					case Zoom300:
						miPixelWidth_PlotArea = miDataWidth * 3;
						miPixelHeight_PlotArea = miDataHeight * 3;
						vAdjustCanvasToPlotArea();
						break;
					case Zoom400:
						miPixelWidth_PlotArea = miDataWidth * 4;
						miPixelHeight_PlotArea = miDataHeight * 4;
						vAdjustCanvasToPlotArea();
						break;
					case Custom:
						return; // nothing needs to be done
				}
		}
		notifyListener();
	}
	
	private void vAdjustCanvasToPlotArea(){
		miPixelWidth_Canvas = miPixelWidth_PlotArea + ( miMarginLeft + miMarginRight );
		miPixelHeight_Canvas = miPixelHeight_PlotArea + ( miMarginTop + miMarginBottom );
	}

	private void vAdjustPlotAreaToCanvas(){
		miPixelWidth_PlotArea = miPixelWidth_Canvas - ( miMarginLeft + miMarginRight );
		miPixelHeight_PlotArea = miPixelHeight_Canvas - ( miMarginTop + miMarginBottom );
	}
	
	boolean zZoomIn(){
		if( meZoomFactor == ZOOM_FACTOR.Zoom400 || meZoomFactor == ZOOM_FACTOR.Custom ) return false; // already big (or custom)
		if( meZoomFactor == ZOOM_FACTOR.Max ){
			meZoomFactor = ZOOM_FACTOR.Zoom100;
		} else {
			meZoomFactor = ZOOM_FACTOR.values()[meZoomFactor.ordinal() + 1];
		}
		vUpdateValues();
		return true;
	}

	boolean zZoomOut(){
		if( meZoomFactor == ZOOM_FACTOR.Zoom50 || meZoomFactor == ZOOM_FACTOR.Custom ) return false; // already small (or custom)
		if( meZoomFactor == ZOOM_FACTOR.Max ){
			meZoomFactor = ZOOM_FACTOR.Zoom100;
		} else {
			meZoomFactor = ZOOM_FACTOR.values()[meZoomFactor.ordinal() + 1];
		}
		vUpdateValues();
		return true;
	}

	boolean zZoomMaximize(){
		if( meZoomFactor == ZOOM_FACTOR.Max ) return false; // already maxed
		meZoomFactor = ZOOM_FACTOR.Max;
		vUpdateValues();
		return true;
	}
	
	void setZoomFactor( ZOOM_FACTOR eZoomFactor ){
		if( eZoomFactor == this.meZoomFactor ) return;
		this.meZoomFactor = eZoomFactor;
		vUpdateValues();
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

	int getPixels( float f, UNITS eUnits ){
		switch( eUnits ){
			case Inches_Tenths:
				return (int)(f*dpiOutput);
			case Inches_Eighths:
				return (int)(f*dpiOutput/8.0f);
			case Centimeters:
				return (int)(f*(dpiOutput/2.54f));
			case Pixels:
			default:
				return (int)f;
		}
	}
	float getUnits( int pixels, UNITS eUnits ){
		switch( eUnits ){
			case Inches_Tenths:
				return (float)pixels/(dpiOutput);
			case Inches_Eighths:
				return (float)pixels/(dpiOutput/8.0f);
			case Centimeters:
				return (float)pixels/(dpiOutput/2.54f);
			case Pixels:
			default:
				return (float)pixels;
		}
	}
	
	private int getPixelsPerData(){ return miPixelsPerData; }
	private int getDataPointsPerPixel(){ return miDataPointsPerPixel; }

	public int getOutputWidth(){
		if( dimOutput != null ) return dimOutput.width;
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
		if( dimOutput != null ) return dimOutput.height;
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
		sb.append("\tmargin_units: " + meMarginUnits.toString() + "\n");
		sb.append("\tmargin_left: " + mfMarginLeft + "\n");
		sb.append("\tmargin_top: " + mfMarginTop + "\n");
		sb.append("\tmargin_right: " + mfMarginRight + "\n");
		sb.append("\tmargin_bottom: " + mfMarginBottom + "\n");
		sb.append("\tpixels_per_data: " + miPixelsPerData + "\n");
		sb.append("\tdata_per_pixel: " + miDataPointsPerPixel + "\n");
		sb.append("\tscale_mode: " + meScaleMode + "\n");
		sb.append("\tpixel_height_canvas: " + miPixelHeight_Canvas + "\n");
		sb.append("\tpixel_width_canvas: " + miPixelWidth_Canvas + "\n");
		sb.append("\tpixel_height_plot_area: " + miPixelHeight_PlotArea + "\n");
		sb.append("\tpixel_width_plot_area: " + miPixelWidth_PlotArea + "\n");
		sb.append("\tzoom: " + getZoomDescriptor( meZoomFactor ) + "\n");
		sb.append("}\n");
		return sb.toString();
	}

	private String getZoomDescriptor( ZOOM_FACTOR eZoom ){
		switch( eZoom ){
			case Max: return "Max";
			case Zoom50: return "50";
			case Zoom75: return "75";
			case Zoom100: return "100";
			case Zoom200: return "200";
			case Zoom300: return "300";
			case Zoom400: return "400";
			case Custom: return "Custom";
			default: return "?";
		}
	}

//	private int getZoomFromDescriptor( String sDescriptor ){
//		for( int eZoom = 1; eZoom <= Custom; eZoom++ ){
//			if( getZoomDescriptor(eZoom).equalsIgnoreCase(sDescriptor) ) return eZoom;
//		}
//		return ZOOM_Max;
//	}

}

class Panel_PlotScale extends JPanel implements IChanged {

	public static void main( String[] args ){
		try {
			JDialog jd;
			JOptionPane jop;
			Panel_PlotScale panelPlotScale = new Panel_PlotScale();
			PlotScale plot_scale = new PlotScale();
			plot_scale.setDataDimension( 1000, 900 );
			panelPlotScale._setScale( plot_scale );
			jop = new JOptionPane( panelPlotScale, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION );
			jd = jop.createDialog( null, "Set Scale" );
			jd.setVisible( true );
//			System.out.println( mHSBpicker.getCurrentColor_ARGB() );
//			System.out.println( mHSBpicker.getComparisonColor_ARGB() );
			System.exit( 0 );
		} catch( Throwable ex ) {
			System.err.println("Error: " + ex);
		}
	}
	
	PlotScale mScale = null;
	private final JComboBox jcbMarginUnits = new JComboBox(PlotScale.AS_Units);
	private final JComboBox jcbScaleUnits = new JComboBox(PlotScale.AS_Units);
	private final JComboBox jcbZoomFactor = new JComboBox(PlotScale.AS_ZoomFactor);
	private final JRadioButton jrbPixelsPerData = new JRadioButton();
	private final JRadioButton jrbEntireCanvas = new JRadioButton("Entire Canvas");
	private final JRadioButton jrbPlotArea = new JRadioButton("Plot Area");
	private final JTextField jtfMarginLeft = new JTextField(6);
	private final JTextField jtfMarginTop = new JTextField(6);
	private final JTextField jtfMarginRight = new JTextField(6);
	private final JTextField jtfMarginBottom = new JTextField(6);
	private final JTextField jtfPixelsPerData = new JTextField(5);
	private final JTextField jtfDataPointsPerPixel = new JTextField(5);
	private final JTextField jtfWidth = new JTextField(6);
	private final JTextField jtfHeight = new JTextField(6);
	private final JTextField jtfResolution = new JTextField(6);
	private final JCheckBox jcheckAspectRatio = new JCheckBox("Maintain Aspect Ratio: ");
	private final JButton jbSetScreenDPI = new JButton( Resources.getIcon( Resources.Icons.DisplayScreen ) );

	Panel_PlotScale(){

		JLabel labMarginTitle = new JLabel( "Margins: ");
		JLabel labMarginLeft = new JLabel( "Left: ", JLabel.RIGHT);
		JLabel labMarginTop = new JLabel( "Top: ", JLabel.RIGHT);
		JLabel labMarginBottom = new JLabel( "Bottom: ", JLabel.RIGHT);
		JLabel labMarginRight = new JLabel( "Right: ", JLabel.RIGHT);
		JLabel labMarginUnits = new JLabel( "Margin Units: ", JLabel.RIGHT);
		JLabel labScale = new JLabel( "Scale", JLabel.RIGHT);
		JLabel labScaleUnits = new JLabel( "Units: ", JLabel.RIGHT);
		JLabel labZoom = new JLabel( "Zoom Factor: ", JLabel.RIGHT);
		JLabel labWidth = new JLabel( "Width: ", JLabel.RIGHT);
		JLabel labHeight = new JLabel( "Height: ", JLabel.RIGHT);
		JLabel labResolution = new JLabel( "Resolution: ", JLabel.RIGHT);
		JLabel labDPI = new JLabel( "dpi", JLabel.LEFT);
		javax.swing.ButtonGroup bg = new javax.swing.ButtonGroup();
		bg.add(jrbPixelsPerData);
		bg.add(jrbEntireCanvas);
		bg.add(jrbPlotArea);
		jrbPixelsPerData.setSelected(true);
		vSetupListeners();

		jbSetScreenDPI.setToolTipText( "Set resolution to native display resolution in dots per inch (dpi)." );
		jtfResolution.setToolTipText( "Output resolution in dots per inch (dpi)." );
		
		JPanel panelDPI = new JPanel();
		panelDPI.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0) );  // top left bottom right
		panelDPI.setLayout( new BoxLayout( panelDPI, BoxLayout.X_AXIS ));
		panelDPI.add( jtfResolution );
		panelDPI.add( labDPI );
		panelDPI.add( Box.createHorizontalStrut(6) );
		panelDPI.add( jbSetScreenDPI );

		JPanel panelScaleContext = new JPanel(); // used for the two radio buttons
		panelScaleContext.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0) );  // top left bottom right
		panelScaleContext.setLayout( new BoxLayout( panelScaleContext, BoxLayout.X_AXIS ));
		panelScaleContext.add( jrbEntireCanvas );
		panelScaleContext.add( jrbPlotArea );
		
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

		// resolution/DPI panel
		gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labResolution, gbc);
		gbc.gridx = 2; gbc.gridy = 7; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(panelDPI, gbc);
		gbc.gridx = 3; gbc.gridy = 7; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// zoom
		gbc.gridx = 0; gbc.gridy = 9; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 9; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labZoom, gbc);
		gbc.gridx = 2; gbc.gridy = 9; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jcbZoomFactor, gbc);
		gbc.gridx = 3; gbc.gridy = 9; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// plot area / canvas radio selector
		gbc.gridx = 0; gbc.gridy = 10; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 10; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(Box.createVerticalStrut(10), gbc);
		gbc.gridx = 2; gbc.gridy = 10; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(panelScaleContext, gbc);
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

	public void update( int eSection ){
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
			jtfResolution.setEnabled(zEnable);
			jbSetScreenDPI.setEnabled(zEnable);
			jcheckAspectRatio.setEnabled(zEnable);
			jcheckAspectRatio.setSelected(true);
			if( mScale == null ) return;
			jcbMarginUnits.setSelectedIndex( mScale.getMarginUnits().ordinal() );
			jcbScaleUnits.setSelectedIndex( mScale.getScaleUnits().ordinal() );

			PlotScale.SCALE_MODE eScaleMode = mScale.getScaleMode();

			// set dpi
			jtfResolution.setText( Integer.toString( mScale.getOutputResolution() ) );
			int dpiScreen = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
			if( mScale.getOutputResolution() == dpiScreen ){
				jbSetScreenDPI.setVisible( false );
			} else {
				jbSetScreenDPI.setVisible( true );
			}
			
			// set zoom
			jcbZoomFactor.setSelectedIndex( eScaleMode.ordinal() );

			// set mode radios
			switch( eScaleMode ){
				case Canvas:
					jrbEntireCanvas.setSelected(true);
					jrbPlotArea.setSelected(false);
					break;
				case PlotArea:
					jrbEntireCanvas.setSelected(false);
					jrbPlotArea.setSelected(true);
					break;
				case Output:
				case Zoom:
					jrbEntireCanvas.setSelected(false);
					jrbPlotArea.setSelected(true);
					break;
			}

			// set absolute width/height
			PlotScale.UNITS eUNITS = mScale.getScaleUnits();
			int pxWidth, pxHeight;
			if( jrbEntireCanvas.isSelected() ){
				pxWidth = mScale.getCanvas_Width();
				pxHeight = mScale.getCanvas_Height();
			} else {
				pxWidth = mScale.getPlot_Width();
				pxHeight = mScale.getPlot_Height();
			}
			float fWidth = mScale.getUnits( pxWidth, eUNITS );
			float fHeight = mScale.getUnits( pxHeight, eUNITS );
			if( fWidth == (int)fWidth ){
				jtfWidth.setText(Integer.toString((int)fWidth));
			} else {
				jtfWidth.setText(Float.toString(fWidth));
			}
			if( fHeight == (int)fHeight ){
				jtfHeight.setText( Integer.toString((int)fHeight) );
			} else {
				jtfHeight.setText( Float.toString(fHeight) );
			}

			jcbScaleUnits.setEnabled( true );
			
			// enable width/height/aspect
			boolean zAbsoluteEnabled = (eScaleMode == PlotScale.SCALE_MODE.Canvas) || (eScaleMode == PlotScale.SCALE_MODE.PlotArea);
			jrbEntireCanvas.setEnabled(zAbsoluteEnabled);
			jrbPlotArea.setEnabled(zAbsoluteEnabled);
			jtfWidth.setEnabled(zAbsoluteEnabled);
			jtfHeight.setEnabled(zAbsoluteEnabled);
			jcheckAspectRatio.setEnabled(zAbsoluteEnabled);

			// set margin text boxes
			jtfMarginLeft.setText(Integer.toString(mScale.getMarginLeft_px()));
			jtfMarginTop.setText(Integer.toString(mScale.getMarginTop_px()));
			jtfMarginRight.setText(Integer.toString(mScale.getMarginRight_px()));
			jtfMarginBottom.setText(Integer.toString(mScale.getMarginBottom_px()));
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, "while updating scale panel");
		} finally {
			mzUpdating = false;
		}
	}

	PlotScale _getScale(){ return mScale; }
	void _setScale( PlotScale scale ){
		mScale = scale;
		if( mScale != null ) mScale.setListener( this );
		update();
	}

	void _changeDataDimension( int iNewWidth, int iNewHeight ){
		if( mScale != null ){
			mScale.setDataDimension( iNewWidth, iNewHeight );
		}
	}

	private void vSetupListeners(){
		jbSetScreenDPI.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					mScale.setOutputToScreenResolution();
				}
			}
		);
		jcbScaleUnits.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					int xSelected = jcbScaleUnits.getSelectedIndex();
					mScale.setUnits( PlotScale.UNITS.values()[xSelected] );
				}
			}
		);
		jcbMarginUnits.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					int xSelected = jcbMarginUnits.getSelectedIndex();
					mScale.setMarginUnits( PlotScale.UNITS.values()[xSelected] );
				}
			}
		);
		jtfResolution.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						vUpdateResolution();
					} catch(Exception ex){} // ignore invalid entries
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
					mScale.setScaleMode( PlotScale.SCALE_MODE.Canvas );
					update();
				}
			}
		);
		jrbPlotArea.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					mScale.setScaleMode( PlotScale.SCALE_MODE.PlotArea );
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

	private void vUpdateResolution(){
		String sResolution = jtfResolution.getText();
		int iResolutionDPI = 0;
	    try {
			iResolutionDPI = Integer.parseInt( sResolution );
			if( iResolutionDPI <= 0 ){
				ApplicationController.vShowError("Output resolution must be a positive number");
				return;
			}
		} catch( NumberFormatException ex ) {
			ApplicationController.vShowError("Unable to interpret " + sResolution + " as a positive integer");
			jtfResolution.setText( Integer.toString( mScale.dpiOutput ) );
			return;
		}
		int dpiScreen = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
		if( iResolutionDPI == dpiScreen ){
			jbSetScreenDPI.setVisible( false );
		} else {
			jbSetScreenDPI.setVisible( true );
		}
		mScale.setOutputResolution( iResolutionDPI );
	}
	
	private void vZoomChanged(){
		PlotScale scale = _getScale();
		if( scale == null ) return; // cannot do anything
		int xSelected = jcbZoomFactor.getSelectedIndex();
		int iPPD, iDPP;
		switch( xSelected ){
			case 0: // max
				iPPD = 1; iDPP = 1; // will be ignored
				scale.setScaleMode( PlotScale.SCALE_MODE.Output );
				break;
			case 1: // 50%
				iPPD = 1; iDPP = 2;
				scale.setScaleMode( PlotScale.SCALE_MODE.Zoom );
				break;
			case 2: // 75%
				iPPD = 3; iDPP = 4;
				scale.setScaleMode(PlotScale.SCALE_MODE.Zoom);
				break;
			case 3: // 100%
				iPPD = 1; iDPP = 1;
				scale.setScaleMode(PlotScale.SCALE_MODE.Zoom);
				break;
			case 4: // 200%
				iPPD = 2; iDPP = 1;
				scale.setScaleMode(PlotScale.SCALE_MODE.Zoom);
				break;
			case 5: // 300%
				iPPD = 3; iDPP = 1;
				scale.setScaleMode(PlotScale.SCALE_MODE.Zoom);
				break;
			case 6: // 400%
				iPPD = 4; iDPP = 1;
				scale.setScaleMode(PlotScale.SCALE_MODE.Zoom);
				break;
			case 7: // custom
				iPPD = 1; iDPP = 1; // will be ignored
				if( jrbEntireCanvas.isSelected() ){
					scale.setScaleMode(PlotScale.SCALE_MODE.Canvas);
				} else {
					scale.setScaleMode(PlotScale.SCALE_MODE.PlotArea);
				}
				break;
			default: return; // do nothing
		}
		scale.setScaleRatio( iPPD, iDPP );
		update();
	}

	private final void vUpdateWidth(){
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
		PlotScale.UNITS eScaleUnits = mScale.getScaleUnits();
		int iWidth_pixels = mScale.getPixels( fWidth, eScaleUnits );
		if( jrbEntireCanvas.isSelected() ){
			mScale.setPixelWidth_Canvas( iWidth_pixels );
		} else if( jrbPlotArea.isSelected() ){
			mScale.setPixelWidth_PlotArea( iWidth_pixels );
		}
		if( jcheckAspectRatio.isSelected() ){  // scale to aspect ratio
			float fRatio = (float)mScale.miDataHeight / (float)mScale.miDataWidth;
			float fHeight = fWidth * fRatio;
			int iHeight_pixels = mScale.getPixels( fHeight, eScaleUnits );
			if( jrbEntireCanvas.isSelected() ){
				mScale.setPixelHeight_Canvas( iHeight_pixels);
			} else if( jrbPlotArea.isSelected() ){
				mScale.setPixelHeight_PlotArea( iWidth_pixels );
			}
			if( fHeight == (int)fHeight ){
				jtfHeight.setText(Integer.toString((int)fHeight));
			} else {
	    		jtfHeight.setText(Float.toString(fHeight));
			}
		}
	}
	private final void vUpdateHeight(){
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
		PlotScale.UNITS eScaleUnits = mScale.getScaleUnits();
		int iHeight_pixels = mScale.getPixels(fHeight, eScaleUnits);
		if( jrbEntireCanvas.isSelected() ){
			mScale.setPixelHeight_Canvas( iHeight_pixels );
		} else if( jrbPlotArea.isSelected() ){
			mScale.setPixelHeight_PlotArea( iHeight_pixels );
		}
		if( jcheckAspectRatio.isSelected() ){  // scale to aspect ratio
			float fRatio = (float)mScale.miDataWidth / (float)mScale.miDataHeight;
			float fWidth = fHeight * fRatio;
			int iWidth_pixels = mScale.getPixels( fWidth, eScaleUnits );
			if( jrbEntireCanvas.isSelected() ){
				mScale.setPixelWidth_Canvas( iWidth_pixels );
			} else if( jrbPlotArea.isSelected() ){
				mScale.setPixelWidth_PlotArea( iWidth_pixels );
			}
			if( fWidth == (int)fWidth ){
				jtfWidth.setText(Integer.toString((int)fWidth));
			} else {
	    		jtfWidth.setText(Float.toString(fWidth));
			}
		}
	}
}



