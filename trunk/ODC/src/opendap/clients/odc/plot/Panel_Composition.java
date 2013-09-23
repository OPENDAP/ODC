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
import javax.swing.*;

/** Displays a composition layout and allows a user to modify a composition
 *  [[ NORTH - Panel_Composition_Controls
 *     [jcbLayout] [jbuttonOptions] [jbuttonScale] [jbuttonAddText] [jbuttonAddLegend] [jbuttonAddScale] ]]
 *  [[ CENTER - Panel_Composition_Canvas ]]
 */

class Panel_Composition extends JPanel {

	private Composition composition = null;
	private Panel_Composition_Controls controls;
	private Panel_Composition_Canvas canvas;

	private Panel_Composition(){} // see create() method

	public Composition getCurrentComposition(){ return composition; }

	public boolean setCurrentComposition( Composition new_composition, StringBuffer sbError ){
		return canvas._setCurrentComposition( new_composition, sbError );
	}

	public static Panel_Composition _create( StringBuffer sbError ){
		Panel_Composition panel = new Panel_Composition();
		panel.composition = null;
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

}




