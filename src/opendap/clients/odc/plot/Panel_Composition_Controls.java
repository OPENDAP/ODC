package opendap.clients.odc.plot;

import javax.swing.*;
import java.awt.*;

/** See Panel_Composition for parent
 *     [jcbLayout] [Options] [Set Scale] [Add Text] [Add Legend] [Add Ruler] ]]
 */

public class Panel_Composition_Controls extends JPanel {

	JLabel labelLayout;
	JComboBox jcbLayout;
	JButton button_Options;
	JButton button_SetScale;
	JButton button_AddText;
	JButton button_AddLegend;
	JButton button_AddRuler;

	Panel_Composition parent;

	public Panel_Composition_Controls( final Panel_Composition parent ){

		this.parent = parent;

		labelLayout = new JLabel( "Layout: " );

		jcbLayout = new JComboBox();
		jcbLayout.setModel( new DefaultComboBoxModel( Composition.LayoutStyle.values() ) );
		jcbLayout.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed( java.awt.event.ActionEvent e ){
				// this list box currently causes no side effect
			}
		});

		button_Options = new JButton( "Options" );
		button_Options.setToolTipText( "Show options for this composition" );
		button_Options.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed( java.awt.event.ActionEvent e ){
			}
		});

		button_SetScale = new JButton( "Set Scale" );
		button_SetScale.setToolTipText( "Set scaling for this composition" );
		button_SetScale.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed(java.awt.event.ActionEvent e ){
			}
		});

		button_AddText = new JButton( "Add Text" );
		button_AddText.setToolTipText( "Add text to composition only (not to a plot)" );
		button_AddText.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed(java.awt.event.ActionEvent e ){
			}
		});

		button_AddLegend = new JButton( "Add Legend" );
		button_AddLegend.setToolTipText( "Add a legend to this composition (not to a plot)"  );
		button_AddLegend.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed(java.awt.event.ActionEvent e ){
			}
		});

		button_AddRuler = new JButton( "Add Ruler" );
		button_AddRuler.setToolTipText( "Add a ruler to this composition (not to a plot)"  );
		button_AddRuler.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed(java.awt.event.ActionEvent e ){
			}
		});

		setLayout( new BoxLayout( this, BoxLayout.X_AXIS ));
		setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0) );  // top left bottom right
		setAlignmentX( LEFT_ALIGNMENT );
		add( labelLayout );
		add( Box.createRigidArea( new Dimension( 2, 0)) );
		add( jcbLayout );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_Options );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_SetScale );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_AddText );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_AddLegend );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_AddRuler );
	}

	Composition.LayoutStyle getSelectedLayoutType(){
		Object oSelectedItem = jcbLayout.getSelectedItem();
		return (Composition.LayoutStyle)oSelectedItem;
	}

}
