package opendap.clients.odc.plot;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.gui.Resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

public class Panel_Rendering extends JPanel implements Printable, MouseListener, MouseMotionListener, Scrollable {

	Composition composition = null;

	public static void main(String[] args) {
		StringBuffer sbError = new StringBuffer();
		Frame frame = new Frame("Plot Renderer Demo");
		frame.setSize( 600, 600 );
		PlotScale scale = PlotScale.create();
		String sID = "demo_id";
		String sCaption = "demo composition";
		PlotEnvironment environment = new PlotEnvironment();
		PlotLayout layout = PlotLayout.create( PlotLayout.LayoutStyle.PlotArea );
		environment.getScale().setOutputTarget( Renderer.OutputTarget.NewWindow );
		environment.getScale().setDataDimension( 600, 600 );
		Plot_Surface demo_surface = Plot_Surface.create( environment, layout );
		Composition composition = Composition.create( demo_surface );
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

	private Panel_Rendering(){} // see create() method

	public Composition getCurrentComposition(){ return composition; }

	public static Panel_Rendering _create( Composition composition, StringBuffer sbError ){
		Panel_Rendering panel = new Panel_Rendering();
		panel.composition = composition;
		panel.addMouseListener( panel );
		panel.addMouseMotionListener( panel );
		return panel;
	}

    public Dimension getPreferredSize() {
    	if( composition == null ) return new Dimension( 250, 250 );
		return composition.getLayout().getCompositionDimensions();
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
			ApplicationController.vUnexpectedError( ex, "While trying to print" );
			return java.awt.print.Printable.NO_SUCH_PAGE;
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		try {
			if( composition != null && composition.getBuffer() != null ) ((Graphics2D)g).drawImage( composition.getBuffer(), null, 0, 0); // flip image to canvas
		} catch( Exception ex ){
			ApplicationController.vUnexpectedError( ex, "Error rendering composition" );
		}
	}

	// TODO move microscope to plot areas

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
		int pxPlotWidth = 0; // mscale.getPlot_Width_pixels();
		int pxPlotHeight = 0; // mScale.getPlot_Height_pixels();
		int xClick = xPlot * iDataDim_Width / pxPlotWidth;
		int yClick = yPlot * iDataDim_Height / pxPlotHeight;

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
//				mColors.render( aRGB, plottableData.getFloatArray(), 6, 6, 6, 6, false );
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
