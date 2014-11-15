package opendap.clients.odc.plot;

/**
 * Title:        Panel_Composition
 * Description:  Base class for plotting
 * Copyright:    Copyright (c) 2002-13
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.10
 */

import opendap.clients.odc.ApplicationController;

import java.awt.*;
import java.awt.print.*;
import javax.swing.*;

/* Displays a composition layout and allows a user to modify a composition
 *  [[ NORTH - Panel_Composition_Controls
 *     [jcbLayout] [jbuttonOptions] [jbuttonScale] [jbuttonAddText] [jbuttonAddLegend] [jbuttonAddScale] ]]
 *  [[ CENTER - Panel_Composition_Canvas ]]
 *
 *  parent: Panel_Definition
 */

class Panel_Composition extends JPanel implements java.awt.print.Printable {

	private Composition composition = null;
	private Panel_Composition_Controls controls;
	private Panel_Composition_Canvas canvas;

	private Panel_Composition(){} // see create() method

	public Composition getCurrentComposition(){ return composition; }

	public boolean setCurrentComposition( Composition new_composition, StringBuffer sbError ){
		return canvas._setCurrentComposition( new_composition, sbError );
	}

	public static Panel_Composition _createx( StringBuffer sbError ){
		Panel_Composition panel = new Panel_Composition();
		panel.composition = null;
		panel.controls = new Panel_Composition_Controls( panel );
		panel.canvas = new Panel_Composition_Canvas();
		panel.setLayout( new BorderLayout() );
		panel.add( panel.controls, BorderLayout.NORTH );
		panel.add( panel.canvas, BorderLayout.CENTER );
		return panel;
	}

	public static Panel_Composition _create( Composition composition, StringBuffer sbError ){
		Panel_Composition panel = new Panel_Composition();
		panel.composition = composition;
		panel.controls = new Panel_Composition_Controls( panel );
		panel.canvas = new Panel_Composition_Canvas();
		panel.setLayout( new BorderLayout() );
		panel.add( panel.controls, BorderLayout.NORTH );
		panel.add( panel.canvas, BorderLayout.CENTER );
		return panel;
	}

    public Dimension getPreferredSize() {
    	if( composition == null ) return new Dimension( 250, 250 ); 
		return composition.getLayout().getCompositionDimensions();
    }

	public int print(Graphics g, java.awt.print.PageFormat pf, int page) throws PrinterException {

		// We have only one page, and 'page' is zero-based
		if (page > 0) {
			return NO_SUCH_PAGE;
		}

		// User (0,0) is typically outside the
		// imageable area, so we must translate
		// by the X and Y values in the PageFormat
		// to avoid clipping.
		Graphics2D g2d = (Graphics2D)g;
		g2d.translate(pf.getImageableX(), pf.getImageableY());

		// Now we perform our rendering
		g.drawString( "Hello world!", 100, 100 );

		// tell the caller that this page is part of the printed document
		return PAGE_EXISTS;
	}
}




