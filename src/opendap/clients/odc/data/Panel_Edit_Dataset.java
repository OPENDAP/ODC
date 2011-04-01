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

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.gui.Resources;

// see Panel_Edit_Container for UI guide
// this panel is used by Panel_Define_Dataset

public class Panel_Edit_Dataset extends JPanel {
	ButtonGroup buttongroupSelection = new ButtonGroup(); // this is used to select the active variable for viewing
	Panel_EditVariable_Controls mControls;
	
	public Panel_Edit_Dataset(){
		mControls = new Panel_EditVariable_Controls( this );
		setLayout( new java.awt.BorderLayout() );
		add( mControls, java.awt.BorderLayout.CENTER );
		setPreferredSize( new java.awt.Dimension( 400, 25 ) );
	}
	
	void actionAddVariable( Node node ){
	}
	void actionAddMemberVariable( Node node ){
		if( node.eVariableType != DAP.DAP_VARIABLE.Structure ){
			ApplicationController.vShowError( "Attempt to add variable to a " + node.eVariableType + ". Variables may only be added to a structure." );
			return;
		}
		DAP.DAP_VARIABLE eVariableType = mControls.getSelectedType();
		
	}
	void actionDeleteVariable( Node node ){
	}
	void actionMoveVariableUp( Node node ){
	}
	void actionMoveVariableDown( Node node ){
	}
}

class Panel_EditVariable_Controls extends JPanel {
	
	JRadioButton jrbSelectVariable;
	JButton button_Add;
	JButton button_AddMember; // add variable member of this structure
	JButton button_Delete;
	JButton button_Up;
	JButton button_Down;
	JComboBox jcbVariableType;
	JLabel labelSummary;
	
	Panel_Edit_Dataset parent;
	Node node;
	
	public Panel_EditVariable_Controls( final Panel_Edit_Dataset parent ){
		
		this.parent = parent;
		
		jrbSelectVariable = new JRadioButton();
		parent.buttongroupSelection.add( jrbSelectVariable );
		jrbSelectVariable.setToolTipText( "select variable for viewing in data pane below" );

		button_Add = new JButton();
		button_Add.setIcon( new ImageIcon( Resources.imageNavigatePlus ) );
		button_Add.setToolTipText( "add new variable below" );
		button_Add.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed( java.awt.event.ActionEvent e ){
				Node node = Panel_EditVariable_Controls.this.node; 
				Panel_EditVariable_Controls.this.parent.actionAddVariable( node );
			}
		});

		button_AddMember = new JButton();
		button_AddMember.setIcon( new ImageIcon( Resources.imageNavigatePlus ) );
		button_AddMember.setText("{}");
		button_AddMember.setToolTipText( "add member variable inside this one" );
		button_AddMember.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed( java.awt.event.ActionEvent e ){
				Node node = Panel_EditVariable_Controls.this.node; 
				Panel_EditVariable_Controls.this.parent.actionAddMemberVariable( node );
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
				// this list box currently causes no side effect
			}
		});

		labelSummary = new JLabel();
		
		JPanel panelAddSubgroup = new JPanel();
		panelAddSubgroup.setLayout( new BoxLayout( panelAddSubgroup, BoxLayout.X_AXIS ));
		panelAddSubgroup.add( button_Add );
		panelAddSubgroup.add( Box.createRigidArea( new Dimension( 2, 0)) );
		panelAddSubgroup.add( button_AddMember );
		panelAddSubgroup.add( Box.createRigidArea( new Dimension( 2, 0)) );
		panelAddSubgroup.add( new JLabel(":") );
		panelAddSubgroup.add( Box.createRigidArea( new Dimension( 2, 0)) );
		panelAddSubgroup.add( jcbVariableType );
		javax.swing.border.Border border = javax.swing.border.LineBorder.createBlackLineBorder();
		border.getBorderInsets( panelAddSubgroup ).set( 2, 2, 2, 2 );
		panelAddSubgroup.setBorder( border );
		
		setLayout( new BoxLayout( this, BoxLayout.X_AXIS ));
		setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0) );  // top left bottom right
		setAlignmentX( LEFT_ALIGNMENT );
		add( panelAddSubgroup );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_Delete );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_Up );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
		add( button_Down );
		add( Box.createRigidArea( new Dimension( 4, 0)) );
	}
	
	void setNode( Node node ){
		this.node = node;
		jcbVariableType.setSelectedItem( node.eVariableType.toString() );
		labelSummary.setText( node.sGetSummaryText() );
	}
	
	DAP.DAP_VARIABLE getSelectedType(){
		Object oSelectedItem = jcbVariableType.getSelectedItem();
		DAP.DAP_VARIABLE new_type = DAP.DAP_VARIABLE.valueOf( oSelectedItem.toString() );
		return new_type;
	}
	
}