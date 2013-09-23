package opendap.clients.odc.plot;

import opendap.clients.odc.ApplicationController;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.*;

// central panel of Panel_Composition

public class Panel_Composition_Canvas extends JPanel implements MouseListener, MouseMotionListener, Scrollable {
	private Composition composition;

	public Panel_Composition_Canvas(){
		this.addMouseListener( this );
		this.addMouseMotionListener( this );
	}

	public Composition _getCurrentComposition(){ return composition; }

	public boolean _setCurrentComposition( Composition new_composition, StringBuffer sbError ){
		sbError.append( "not implemented" );
		return false;
	}

    public Dimension getPreferredSize() {
    	if( composition == null ) return new Dimension( 250, 250 );
		return composition.getLayout().getCompositionDimensions();
    }

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		try {
			if( composition != null && composition.getBuffer() != null ) ((Graphics2D)g).drawImage( composition.getBuffer(), null, 0, 0); // flip image to canvas
		} catch( Exception ex ){
			ApplicationController.vUnexpectedError( ex, "Error rendering composition layout canvas" );
		}
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
	// if in histogram area or if in expression area
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
