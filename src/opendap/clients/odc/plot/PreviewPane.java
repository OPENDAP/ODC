package opendap.clients.odc.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Box;
import javax.swing.JScrollPane;

public class PreviewPane extends JScrollPane {
	java.awt.Color colorBackground = Color.WHITE;
	Panel_Composition panel = null;
	public PreviewPane(){
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		StringBuffer sbError = new StringBuffer();
		panel = Panel_Composition._create( null, sbError );
		panel.setLayout( new BorderLayout() );
		panel.add(Box.createGlue(), BorderLayout.CENTER);
		_setContent_Default();
	}
	public void _clear(){
		Graphics g = panel.getGraphics();
		g.setColor( colorBackground );
		g.fillRect( 0, 0, getWidth(), getHeight() );
		revalidate();
	}
	public Panel_Composition _getCanvas(){ return panel; }
	public void _setContent_Default(){
		setViewportView( panel );
	}
	public void _setContent( java.awt.Component component ){
		setViewportView( component );
	}
}
