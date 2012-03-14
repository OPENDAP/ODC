package opendap.clients.odc.gui;

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
import opendap.clients.odc.data.Node;

// see Panel_Edit_Container for UI guide
// this is the action bar for variable editing
// this panel is used by Panel_Define_Dataset

public class Panel_Edit_Dataset extends JPanel {
	Panel_View_Data view; 
	ButtonGroup buttongroupSelection = new ButtonGroup(); // this is used to select the active variable for viewing
	Panel_EditVariable_Controls mControls;
	Panel_Edit_Container container;
	private Panel_Edit_Dataset(){}
	public Panel_Edit_Dataset( Panel_View_Data parent, Panel_Edit_Container container ){
		view = parent;
		this.container = container;
		mControls = new Panel_EditVariable_Controls( this, container );
		setLayout( new java.awt.BorderLayout() );
		add( mControls, java.awt.BorderLayout.CENTER );
		setPreferredSize( new java.awt.Dimension( 400, 25 ) );
	}	
	void actionAddVariable( Node node ){
		if( node == null ){
			ApplicationController.vShowError( "Internal error, attempt to add variable to a null node." );
			return;
		}
		if( ! node.isRoot() && node.eVariableType != DAP.DAP_VARIABLE.Structure ){
			ApplicationController.vShowError( "Attempt to add variable to a " + node.eVariableType + ". Variables may only be added to a structure." );
			return;
		}
		DAP.DAP_VARIABLE eVariableType = mControls.getSelectedType();
		if( eVariableType == null ){
			ApplicationController.vShowError( "No variable type selected for adding as member of structure " + node.getName() );
			return;
		}
		StringBuffer sbError = new StringBuffer();
		Node new_node = node.createDefaultMember( eVariableType, sbError ); // creates and adds the node 
		if( new_node == null ){
			ApplicationController.vShowError( "Failed to create new " +  node.eVariableType + ": " + sbError.toString() );
			return;
		}
		container._getStructureView()._select( new_node );
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
	JButton button_Add; // add variable member of this structure
	JButton button_Delete;
	JButton button_Up;
	JButton button_Down;
	JComboBox jcbVariableType;
	JLabel labelSummary;

	Panel_Edit_Dataset parent;
	Panel_Edit_Container container;
	
	public Panel_EditVariable_Controls( final Panel_Edit_Dataset parent, final Panel_Edit_Container container ){
		
		this.parent = parent;
		this.container = container;
		
		jrbSelectVariable = new JRadioButton();
		parent.buttongroupSelection.add( jrbSelectVariable );
		jrbSelectVariable.setToolTipText( "select variable for viewing in data pane below" );

		button_Add = new JButton();
		button_Add.setIcon( new ImageIcon( Resources.imageNavigatePlus ) );
		button_Add.setToolTipText( "add new variable below" );
		button_Add.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed( java.awt.event.ActionEvent e ){
				Panel_Edit_ViewStructure structure = container._getStructureView();
				Node nodeSelected = structure._getSelectedNode();
				if( nodeSelected == null ){
					ApplicationController.vShowStatus_NoCache( "no element selected to add member to" );
					return;
				}
				Panel_EditVariable_Controls.this.parent.actionAddVariable( nodeSelected );
			}
		});

		button_Delete = new JButton();
		button_Delete.setIcon( new ImageIcon( Resources.imageNavigateMinus ) );
		button_Delete.setToolTipText( "delete variable" );
		button_Delete.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed(java.awt.event.ActionEvent e ){
				Panel_Edit_ViewStructure structure = container._getStructureView();
				Node nodeSelected = structure._getSelectedNode();
				if( nodeSelected == null ){
					ApplicationController.vShowStatus_NoCache( "no element selected for deletion" );
					return;
				}
				Panel_EditVariable_Controls.this.parent.actionDeleteVariable( nodeSelected );
			}
		});
		
		button_Up = new JButton();
		button_Up.setIcon( new ImageIcon( Resources.imageArrowUp ) );
		button_Up.setToolTipText( "move variable up" );
		button_Up.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed(java.awt.event.ActionEvent e ){
				Panel_Edit_ViewStructure structure = container._getStructureView();
				Node nodeSelected = structure._getSelectedNode();
				if( nodeSelected == null ){
					ApplicationController.vShowStatus_NoCache( "no element selected for moving up" );
					return;
				}
				Panel_EditVariable_Controls.this.parent.actionMoveVariableUp( nodeSelected );
			}
		});
		
		button_Down = new JButton();
		button_Down.setIcon( new ImageIcon( Resources.imageArrowDown ) );
		button_Down.setToolTipText( "move variable down" );
		button_Down.addActionListener(new java.awt.event.ActionListener( ){
			public void actionPerformed(java.awt.event.ActionEvent e ){
				Panel_Edit_ViewStructure structure = container._getStructureView();
				Node nodeSelected = structure._getSelectedNode();
				if( nodeSelected == null ){
					ApplicationController.vShowStatus_NoCache( "no element selected for moving down" );
					return;
				}
				Panel_EditVariable_Controls.this.parent.actionMoveVariableDown( nodeSelected );
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
	
	DAP.DAP_VARIABLE getSelectedType(){
		Object oSelectedItem = jcbVariableType.getSelectedItem();
		DAP.DAP_VARIABLE new_type = DAP.DAP_VARIABLE.valueOf( oSelectedItem.toString() );
		return new_type;
	}
	
}