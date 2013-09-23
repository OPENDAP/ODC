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
 * Copyright:    Copyright (c) 2003-2011
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.07
 */

import opendap.clients.odc.ApplicationController;

import java.awt.Dimension;

public class PlotScale {

	final static int SECTION_Margin = 1;
	final static int SECTION_Zoom = 2;
	
	public static enum UNITS {
		Pixels,
		Inches_Tenths,
		Inches_Eighths,
		Inches_32nds,
		Inches_Mils,       // 1/1000"
		Centimeters,
		Picas,             // 1/6"
		Points,            // 1/12 of a Pica
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
	final static String[] AS_Units = { "pixels", "inches", "1/10\"", "1/8\"", "1/32\"", "mils\"", "picas (1/6\")", "points (1/72\")" };
	final static String[] AS_ScaleMode = { "canvas", "plot area", "output", "zoom" };
	final static String[] AS_ZoomFactor = { "Max", "50%","75%","100%","200%","300%","400%", "Custom" };
	final static int[] AI_ZoomFactor = { 0, 50, 75, 100, 200, 300, 400, 0 };
	public final static int PX_DEFAULT_MARGIN = 50;

	// independent values
	Renderer.OutputTarget meOutputOption;
	int miDataWidth;  // the number of data elements in the x-dimension
	int miDataHeight; // the number of data elements in the y-dimension
	int dpiOutput;
	private SCALE_MODE meScaleMode = SCALE_MODE.Zoom;
	private ZOOM_FACTOR meZoomFactor = ZOOM_FACTOR.Max;
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
	private int miMarginLeft, miMarginTop, miMarginRight, miMarginBottom; // pixel values

	// display values (unit-sensitive)
	private float mfWidth_PlotArea;
	private float mfHeight_PlotArea;
	private float mfWidth_Canvas;
	private float mfHeight_Canvas;
	private float mfMarginLeft, mfMarginTop, mfMarginRight, mfMarginBottom;
	
	private PlotScale(){}
	
	public static final PlotScale create(){ // set defaults
		PlotScale ps = new PlotScale();
		ps.meOutputOption = Renderer.OutputTarget.PreviewPane;
		ps.dpiOutput = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
		ps.miMarginLeft = 65;
		ps.miMarginTop = PX_DEFAULT_MARGIN;
		ps.miMarginRight = PX_DEFAULT_MARGIN;
		ps.miMarginBottom = PX_DEFAULT_MARGIN;
		ps.meMarginUnits = UNITS.Pixels;
		ps.miPixelsPerData = 1;
		ps.miDataPointsPerPixel = 1;
		ps.meScaleMode = SCALE_MODE.Zoom; // peter wants max to be the default
	    ps.meScaleUnits = UNITS.Pixels;
	    return ps;
	}

	/**************** GET ******************************/
	int getOutputResolution(){ return this.dpiOutput; }
	SCALE_MODE getScaleMode(){ return meScaleMode; }
	ZOOM_FACTOR getZoomFactor(){ return meZoomFactor; }
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
	int getMarginTop_px(){ return miMarginTop; }
	int getMarginBottom_px(){ return miMarginBottom; }
	int getMarginLeft_px(){ return miMarginLeft; }
	int getMarginRight_px(){ return miMarginRight; }
	float getMarginLeft_display(){ return mfMarginLeft; }
	float getMarginTop_display(){ return mfMarginTop; }
	float getMarginRight_display(){ return mfMarginRight; }
	float getMarginBottom_display(){ return mfMarginBottom; }

	int getCanvas_Width_pixels(){
		return miPixelWidth_Canvas;
	}
	int getCanvas_Height_pixels(){
		return miPixelHeight_Canvas;
	}

	int getPlot_Width_pixels(){
		return miPixelWidth_PlotArea;
	}

	int getPlot_Height_pixels(){
		return miPixelHeight_PlotArea;
	}

	/**************** Active Accessors ************************/

	void setOutputTarget( Renderer.OutputTarget eOutputOption ){
		meOutputOption = eOutputOption;
		vCalculatePlotDimensions();
	}

	void setOutputToScreenResolution(){
		int dpiScreen = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
		setOutputResolution( dpiScreen ); 
	}
	
	void setOutputResolution( int iNewOutputResolution ){
		if( this.dpiOutput == iNewOutputResolution ) return;
		dpiOutput = iNewOutputResolution;
		if( meMarginUnits != UNITS.Pixels ){
			miMarginTop = getPixels( mfMarginTop, meMarginUnits );
			miMarginBottom = getPixels( mfMarginBottom, meMarginUnits );
			miMarginLeft = getPixels( mfMarginLeft, meMarginUnits );
			miMarginRight = getPixels( mfMarginRight, meMarginUnits );
		}
		if( meScaleUnits != UNITS.Pixels ){
			miPixelWidth_PlotArea = getPixels( mfWidth_PlotArea, meScaleUnits );
			miPixelHeight_PlotArea = getPixels( mfHeight_PlotArea, meScaleUnits );
			miPixelWidth_Canvas = getPixels( mfWidth_Canvas, meScaleUnits );
			miPixelHeight_Canvas = getPixels( mfHeight_Canvas, meScaleUnits );
		}
		vCalculatePlotDimensions();
	}
	
	void setMarginLeft( float f ){
		if( mfMarginLeft == f ) return;
		mfMarginLeft = f;
		miMarginLeft = getPixels( f, meMarginUnits );
		vCalculatePlotDimensions();
	}
	void setMarginTop( float f ){
		if( mfMarginTop == f ) return;
		mfMarginTop = f;
		miMarginTop = getPixels( f, meMarginUnits );
		vCalculatePlotDimensions();
	}
	void setMarginRight( float f ){
		if( mfMarginRight == f ) return;
		mfMarginRight = f;
		miMarginRight = getPixels( f, meMarginUnits );
		vCalculatePlotDimensions();
	}
	void setMarginBottom( float f ){
		if( mfMarginBottom == f ) return;
		mfMarginBottom = f;
		miMarginBottom = getPixels( f, meMarginUnits );
		vCalculatePlotDimensions();
	}
	void setMarginUnits( UNITS eUnits ){ // does not change the dimensions
		if( meMarginUnits == eUnits ) return;
		meMarginUnits = eUnits;
		mfMarginTop = getUnits( miMarginTop, meMarginUnits );
		mfMarginBottom = getUnits( miMarginBottom, meMarginUnits );
		mfMarginLeft = getUnits( miMarginLeft, meMarginUnits );
		mfMarginRight = getUnits( miMarginRight, meMarginUnits );
	}

	void setDimensions_CanvasPixels( int pxWidth, int pxHeight ){
		setScaleMode( PlotScale.SCALE_MODE.Canvas );
		miPixelWidth_Canvas = pxWidth;
		miPixelHeight_Canvas = pxHeight;
		vAdjustPlotAreaToCanvas();
	}
	
	/** the pixel width for the canvas */
	void setPixelWidth_Canvas( int i ){
		if( ! ( meScaleMode == SCALE_MODE.Canvas) ){
			ApplicationController.vShowError_NoModal( "internal error, attempt to set canvas width outside of canvas scale mode" );
			return;
		}
		if( i <= ( miMarginLeft + miMarginRight ) ){
			ApplicationController.vShowError_NoModal( "internal error, attempt to set canvas width to less than margins" );
			return;
		}
		miPixelWidth_Canvas = i;
		vAdjustPlotAreaToCanvas();
	}
	
	/** the pixel height for the canvas */
	void setPixelHeight_Canvas( int i ){
		if( ! ( meScaleMode == SCALE_MODE.Canvas) ){
			ApplicationController.vShowError_NoModal( "internal error, attempt to set canvas height when not in canvas scale mode" );
			return;
		}
		if( i <= ( miMarginTop + miMarginBottom ) ){
			ApplicationController.vShowError_NoModal( "internal error, attempt to set canvas height to less than margins" );
			return;
		}
		miPixelHeight_Canvas = i;
		vAdjustPlotAreaToCanvas();
	}

	/** the pixel width for the plot area */
	void setPixelWidth_PlotArea( int i ){
		if( ! ( meScaleMode == SCALE_MODE.PlotArea ) ){
			ApplicationController.vShowError_NoModal( "internal error, attempt to set plot area width when not in plot area scale mode" );
			return;
		}
		if( i <= 0 ){
			ApplicationController.vShowError_NoModal( "internal error, attempt to set plot area width to " + i );
			return;
		}
		miPixelWidth_PlotArea = i;
		vAdjustCanvasToPlotArea();
	}
	
	/** the pixel height for the plot area */
	void setPixelHeight_PlotArea( int i ){
		if( ! ( meScaleMode == SCALE_MODE.PlotArea) ){
			ApplicationController.vShowError_NoModal( "internal error, attempt to set plot area height when not in plot area scale mode" );
			return;
		}
		if( i <= 0 ){
			ApplicationController.vShowError_NoModal( "internal error, attempt to set plot area height to " + i );
			return;
		}
		miPixelHeight_PlotArea = i;
		vAdjustCanvasToPlotArea();
	}
	
	void setUnits( UNITS eUnits ){
	    meScaleUnits = eUnits;
		vCalculatePlotDimensions();
	}

	void setScaleMode( SCALE_MODE eScaleMode ){
		meScaleMode = eScaleMode;
		vCalculatePlotDimensions();
	}

	/** This sets the "Data Dimension" which is the number of elements in the data array for a given dimension. */
	void setDataDimension( int iNewWidth, int iNewHeight ){
		if( iNewWidth < 1 || iNewHeight < 1 ){
			ApplicationController.vShowWarning("invalid output dimensions (" + iNewWidth + ", " + iNewHeight + "), change ignored by scale");
		} else {
			miDataWidth = iNewWidth;
			miDataHeight = iNewHeight;
			vCalculatePlotDimensions();
		}
	}

	void setZoomFactor( ZOOM_FACTOR eZoomFactor, SCALE_MODE eScaleMode ){
		if( eZoomFactor == meZoomFactor ) return; // no change
		meZoomFactor = eZoomFactor;
		int iPPD, iDPP;
		switch( eZoomFactor ){
			case Max:
				iPPD = 1; iDPP = 1; // will be ignored
				setScaleMode( SCALE_MODE.Output );
				break;
			case Zoom50:
				iPPD = 1; iDPP = 2;
				setScaleMode( SCALE_MODE.Zoom );
				break;
			case Zoom75:
				iPPD = 3; iDPP = 4;
				setScaleMode( SCALE_MODE.Zoom );
				break;
			case Zoom100:
				iPPD = 1; iDPP = 1;
				setScaleMode( SCALE_MODE.Zoom );
				break;
			case Zoom200:
				iPPD = 2; iDPP = 1;
				setScaleMode( SCALE_MODE.Zoom );
				break;
			case Zoom300:
				iPPD = 3; iDPP = 1;
				setScaleMode( SCALE_MODE.Zoom );
				break;
			case Zoom400:
				iPPD = 4; iDPP = 1;
				setScaleMode( SCALE_MODE.Zoom );
				break;
			case Custom: // custom
				iPPD = 1; iDPP = 1; // will be ignored
				meScaleMode = eScaleMode; // should be canvas or plot area
				break;
			default: return; // do nothing
		}
		miPixelsPerData = iPPD;
		miDataPointsPerPixel = iDPP;
		vCalculatePlotDimensions();
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
//	private int miPixelWidth_Canvas;
//	private int miPixelHeight_Canvas;
//	private int miPixelWidth_PlotArea;
//	private int miPixelHeight_PlotArea;
	
	void vCalculatePlotDimensions(){
		int iOutputWidth_px = getOutputWidth();
		int iOutputHeight_px = getOutputHeight();
		switch( meScaleMode ){
			case Canvas:
				vAdjustPlotAreaToCanvas();
				break;
			case PlotArea:
				vAdjustCanvasToPlotArea();
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
						ApplicationController.vShowError_NoModal( "internal error, zoom factor is custom, but scale mode is zoom" );
						return; // if the ZoomFactor is custom, the scale mode will be Canvas or PlotArea, so this should never happen
				}
		}
	}
	
	private void vAdjustCanvasToPlotArea(){
		miPixelWidth_Canvas = miPixelWidth_PlotArea + ( miMarginLeft + miMarginRight );
		miPixelHeight_Canvas = miPixelHeight_PlotArea + ( miMarginTop + miMarginBottom );
		mfWidth_Canvas = getUnits( miPixelWidth_Canvas, meScaleUnits );
		mfHeight_Canvas = getUnits( miPixelHeight_Canvas, meScaleUnits );
	}

	private void vAdjustPlotAreaToCanvas(){
		miPixelWidth_PlotArea = miPixelWidth_Canvas - ( miMarginLeft + miMarginRight );
		miPixelHeight_PlotArea = miPixelHeight_Canvas - ( miMarginTop + miMarginBottom );
		mfWidth_PlotArea = getUnits( miPixelWidth_PlotArea, meScaleUnits );
		mfHeight_PlotArea = getUnits( miPixelHeight_PlotArea, meScaleUnits );
	}
	
	boolean zZoomIn(){
		if( meZoomFactor == ZOOM_FACTOR.Zoom400 || meZoomFactor == ZOOM_FACTOR.Custom ) return false; // already big (or custom)
		if( meZoomFactor == ZOOM_FACTOR.Max ){
			meZoomFactor = ZOOM_FACTOR.Zoom100;
		} else {
			meZoomFactor = ZOOM_FACTOR.values()[meZoomFactor.ordinal() + 1];
		}
		vCalculatePlotDimensions();
		return true;
	}

	boolean zZoomOut(){
		if( meZoomFactor == ZOOM_FACTOR.Zoom50 || meZoomFactor == ZOOM_FACTOR.Custom ) return false; // already small (or custom)
		if( meZoomFactor == ZOOM_FACTOR.Max ){
			meZoomFactor = ZOOM_FACTOR.Zoom100;
		} else {
			meZoomFactor = ZOOM_FACTOR.values()[meZoomFactor.ordinal() + 1];
		}
		vCalculatePlotDimensions();
		return true;
	}

	boolean zZoomMaximize(){
		if( meZoomFactor == ZOOM_FACTOR.Max ) return false; // already maxed
		meZoomFactor = ZOOM_FACTOR.Max;
		vCalculatePlotDimensions();
		return true;
	}
	
	///////////////// Passive Accessors

	int getPixels( float f, UNITS eUnits ){
		switch( eUnits ){
			case Inches_Tenths:
				return (int)(f*dpiOutput/10.0f);
			case Inches_Eighths:
				return (int)(f*dpiOutput/8.0f);
			case Inches_32nds:
				return (int)(f*dpiOutput/32.0f);
			case Inches_Mils:
				return (int)(f*dpiOutput/1000.0f);
			case Centimeters:
				return (int)(f*(dpiOutput/2.54f));
			case Picas:
				return (int)(f*(dpiOutput/6.0f));
			case Points:
				return (int)(f*(dpiOutput/72.0f));
			case Pixels:
			default:
				return (int)f;
		}
	}
	float getUnits( int pixels, UNITS eUnits ){
		switch( eUnits ){
			case Inches_Tenths:
				return (float)pixels/(dpiOutput/10.0f);
			case Inches_Eighths:
				return (float)pixels/(dpiOutput/8.0f);
			case Inches_32nds:
				return (float)pixels/(dpiOutput/32.0f);
			case Inches_Mils:
				return (float)pixels/(dpiOutput/1000.0f);
			case Centimeters:
				return (float)pixels/(dpiOutput/2.54f);
			case Picas:
				return (float)pixels/(dpiOutput/6.0f);
			case Points:
				return (float)pixels/(dpiOutput/72.0f);
			case Pixels:
			default:
				return (float)pixels;
		}
	}
	
	public int getOutputWidth(){
		if( dimOutput != null ) return dimOutput.width;
		switch( meOutputOption ){
			case ExternalWindow:
				return Renderer.mCompositionFrame.getWidth();
			case NewWindow:
				return miDataWidth + (int)mfMarginLeft + (int)mfMarginRight;
			case FullScreen:
				Dimension dimScreenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
				return (int)dimScreenSize.getWidth();
			case Print:
				return miDataWidth + (int)mfMarginLeft + (int)mfMarginRight; // cannot figure out this size ahead of time
			case PreviewPane:
				// Dimension dimView = Visualizer.mPreviewScrollPane.getSize(); // does not work: .getViewport().getViewSize();
				Dimension dimView = Panel_View_Plot._getPreviewScrollPane().getSize();
//				Dimension dimTabbed = Panel_View_Plot.getTabbedPane().getSize();
				int iPreviewPaneWidth = (int)dimView.getWidth();
//				int iTabbedPaneWidth = (int)dimTabbed.getWidth();
//System.out.println(" preview width: " + iPreviewPaneWidth + " tabbed pane width: " + iTabbedPaneWidth);
				return iPreviewPaneWidth;
			case ExpressionPreview:
				Dimension dimExpressionView = ApplicationController.getInstance().getAppFrame().getDataViewer()._getPreviewPane().getSize();
				return (int)dimExpressionView.getWidth() - 3;
			case File_PNG:
				return miDataWidth + (int)mfMarginLeft + (int)mfMarginRight;
			case Thumbnail:
				return miDataWidth + (int)mfMarginLeft + (int)mfMarginRight;
			default:
				ApplicationController.vShowWarning("unknown output format to getOutputWidth");
				return 0;
		}
	}

	public int getOutputHeight(){
		if( dimOutput != null ) return dimOutput.height;
		switch( meOutputOption ){
			case ExternalWindow:
				return Renderer.mCompositionFrame.getHeight();
			case NewWindow:
				return miDataHeight + (int)mfMarginTop + (int)mfMarginBottom;
			case FullScreen:
				Dimension dimScreenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
				return (int)dimScreenSize.getHeight();
			case Print:
				return miDataHeight + (int)mfMarginTop + (int)mfMarginBottom; // cannot figure out this size ahead of time
			case PreviewPane:
				Dimension dimView = Panel_View_Plot._getPreviewScrollPane().getSize();
				int iPreviewPaneHeight = (int)dimView.getHeight();
				return iPreviewPaneHeight;
			case ExpressionPreview:
				Dimension dimExpressionView = ApplicationController.getInstance().getAppFrame().getDataViewer()._getPreviewPane().getSize();
				return (int)dimExpressionView.getHeight() - 3;
			case File_PNG:
				return miDataHeight + (int)mfMarginTop + (int)mfMarginBottom; // cannot figure out this size ahead of time
			case Thumbnail:
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




