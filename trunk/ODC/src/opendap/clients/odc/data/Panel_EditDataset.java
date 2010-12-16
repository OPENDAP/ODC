package opendap.clients.odc.data;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.ImageIcon;
import javax.swing.DefaultComboBoxModel;

import opendap.clients.odc.DAP;
import opendap.clients.odc.DAP.DAP_VARIABLE;
import opendap.clients.odc.gui.Resources;

public class Panel_EditDataset extends JPanel {
	ButtonGroup buttongroupSelection = new ButtonGroup(); // this is used to select the active variable for viewing
	
	void actionAddVariable( Node nodeNext ){
	}
	void actionDeleteVariable( Node node ){
	}
	void actionMoveVariableUp( Node node ){
	}
	void actionMoveVariableDown( Node node ){
	}
	void actionChangeVariableType( Node node, DAP.DAP_VARIABLE new_type ){
	}
}

class Panel_EditVariable_Controls extends JPanel {
	
	JRadioButton jrbSelectVariable;
	JButton button_Add;
	JButton button_Delete;
	JButton button_Up;
	JButton button_Down;
	JComboBox jcbVariableType;
	JLabel labelSummary;
	
	Panel_EditDataset parent;
	Node node;
	
	public Panel_EditVariable_Controls( final Panel_EditDataset parent ){
		
		this.parent = parent;
		
		jrbSelectVariable = new JRadioButton();
		parent.buttongroupSelection.add( jrbSelectVariable );
		jrbSelectVariable.setToolTipText( "select variable for viewing in data pane below" );

		button_Add = new JButton();
		button_Add.setIcon( new ImageIcon( Resources.imageNavigatePlus ) );
		button_Add.setToolTipText( "add new variable above" );
		button_Add.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed( java.awt.event.ActionEvent e ){
				Node node = Panel_EditVariable_Controls.this.node; 
				Panel_EditVariable_Controls.this.parent.actionAddVariable( node );
			}
		});

		button_Delete = new JButton();
		button_Delete.setIcon( new ImageIcon( Resources.imageNavigateMinus ) );
		button_Delete.setToolTipText( "delete variable" );
		button_Delete.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed(java.awt.event.ActionEvent e ){
				Node node = Panel_EditVariable_Controls.this.node; 
				Panel_EditVariable_Controls.this.parent.actionDeleteVariable( node );
			}
		});
		
		button_Up = new JButton();
		button_Up.setIcon( new ImageIcon( Resources.imageArrowUp ) );
		button_Up.setToolTipText( "move variable up" );
		button_Up.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed(java.awt.event.ActionEvent e ){
				Node node = Panel_EditVariable_Controls.this.node; 
				Panel_EditVariable_Controls.this.parent.actionMoveVariableUp( node );
			}
		});
		
		button_Down = new JButton();
		button_Down.setIcon( new ImageIcon( Resources.imageArrowDown ) );
		button_Down.setToolTipText( "move variable down" );
		button_Down.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed(java.awt.event.ActionEvent e ){
				Node node = Panel_EditVariable_Controls.this.node; 
				Panel_EditVariable_Controls.this.parent.actionMoveVariableDown( node );
			}
		});
		
		jcbVariableType = new JComboBox();
//		jcbVariableType.setModel( new DefaultComboBoxModel( new String[]{"a", "b", "c" } ) );
		jcbVariableType.setModel( new DefaultComboBoxModel( DAP.DAP_VARIABLE.values() ) );
		jcbVariableType.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed( java.awt.event.ActionEvent e ){
				JComboBox jcb = (JComboBox)e.getSource();
				Object oSelectedItem = jcb.getSelectedItem();
				DAP.DAP_VARIABLE new_type = DAP.DAP_VARIABLE.valueOf( oSelectedItem.toString() );
				Node node = Panel_EditVariable_Controls.this.node;
				if( new_type == node.eVariableType ) return; // no change has been made
				Panel_EditVariable_Controls.this.parent.actionChangeVariableType( node, new_type );
			}
		});

		labelSummary = new JLabel();
		
		setLayout( new BoxLayout( this, BoxLayout.X_AXIS ));
		setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0) );  // top left bottom right
		setAlignmentX( LEFT_ALIGNMENT );
		add( jrbSelectVariable );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_Add );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_Delete );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_Up );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_Down );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( jcbVariableType );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( labelSummary );
	}
	
	void setNode( Node node ){
		this.node = node;
		jcbVariableType.setSelectedItem( node.eVariableType.toString() );
		labelSummary.setText( node.sGetSummaryText() );
	}
	
}