/*
 * VariableSelector.java
 *
 * Created on December 21, 2001, 8:52 PM
 * Root of DDX/CE panel creation
 */

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

package opendap.clients.odc.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.data.Model_Retrieve;

/**
 * This is the base class for the classes used by CEGenerator to make
 * a form to allow the user to constrain the data.
 *
 * @author Rich Honhart <rhonhart@virginia.edu>, John Chamberlain
 */
public abstract class VariableSelector extends JPanel {

	private boolean mzSelected;
	private boolean mzSelected_unique;
	private JCheckBox checkSelection;
	private javax.swing.JRadioButton mRadioButton = null;
	private javax.swing.ButtonGroup mButtonGroup = null;
	
	protected JPanel mpanelHeader; // the name and checkbox
	protected JPanel mpanelDescription; // DAS description
	protected JPanel mpanelDetail; // the dimensions for grids/arrays
	private JLabel mlabelTitle;
	private JTextArea mjtaDescripton;
	
	protected Panel_DDSView mOwner;
	protected String msQualifiedName;
	protected Model_Retrieve mRetrieveModel = null;

    /** Creates a new instance of VariableSelector 
     *  if button_group is non-null then a radio button will be added and added to the group
     *  */
	public VariableSelector( Panel_DDSView owner, String sQualifiedName, javax.swing.ButtonGroup button_group, Model_Retrieve modelRetrieve ) {

		mOwner = owner;
		msQualifiedName = sQualifiedName;
		mButtonGroup = button_group;
		mRetrieveModel = modelRetrieve;

		// multi-selection check box
        checkSelection = new JCheckBox();
		checkSelection.setOpaque(false);
		checkSelection.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					setSelected( checkSelection.isSelected() );
				}
			}
		);

		// unique selection radio button
		if( button_group != null ){
			mRadioButton = new javax.swing.JRadioButton();
			mRadioButton.setOpaque(false);
			mButtonGroup.add( mRadioButton );
		}
		
        mzSelected = true;
		setBackground(Color.WHITE);

		// setup header
		mpanelHeader = new JPanel();
		mlabelTitle = new JLabel();
		mpanelHeader.setOpaque(false);
		mlabelTitle.setOpaque(false);
		mpanelHeader.setLayout( new BoxLayout(mpanelHeader, BoxLayout.X_AXIS) );
		mpanelHeader.add( checkSelection );
		if( mRadioButton != null ) mpanelHeader.add( mRadioButton );
		mpanelHeader.add( mlabelTitle );
		mpanelHeader.add( Box.createHorizontalGlue() );

		mpanelDescription = new JPanel();
		mpanelDescription.setOpaque(false);
		mpanelDescription.setVisible(false);
		mjtaDescripton = new JTextArea();
		mpanelDescription.setLayout( new BorderLayout() );
		mpanelDescription.add( mjtaDescripton, BorderLayout.CENTER );

		mpanelDetail = new JPanel();
		mpanelDetail.setOpaque(false);
		mpanelDetail.setVisible(false);

		setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
		add(mpanelHeader);
		add(mpanelDescription);
		add(mpanelDetail);

    }

	void setTitle( String sTitle ){
		if( sTitle == null ) sTitle = "";
		mlabelTitle.setText( sTitle );
	}

	void setDescription( String sDescription ){
		if( sDescription == null ) sDescription = "";
		mjtaDescripton.setText( sDescription );
	}

	void vShowDescription( boolean zShow ){
		if( mjtaDescripton.getText() == null ){
			mpanelDescription.setVisible( false );
			return;
		}
		if( mjtaDescripton.getText().length() == 0 ){
			mpanelDescription.setVisible( false );
			return;
		}
		mpanelDescription.setVisible( zShow );
	}

	Panel_DDSView getOwner(){ return mOwner; }

    /**
     * Update the components on the screen to match a given constraint
     * expression.  This function does not work with any Dods compatible
     * constraint expression, only the subset that can be generated by
     * generateCE().
     * @param ce The constraint expression.
     */
    public void applyCE(String ce) {
	// If a specialization doesn't overload this function,
	// it is assumed that it has no components which need to be
	// updated, so this is left empty.
    }

    /**
     * Deselect this variable selector and all it's children.
     */
    public void deselectAll() {
        setSelected(false);
    }

    /**
     * Reset the everything in this <code>VariableSelector</code> and
     * all it's children.
     */
    public void reset() {
		setSelected(true);
    }

	final public String getQualifiedName(){
		return msQualifiedName;
	}

    /**
     * Return the radiobutton connected to the VariableSelector
     */
    public JCheckBox getButton() {
        return checkSelection;
    }

    /**
     * Generate a DODS constraint expression for the variable.
     * @param prefix Anything that needs to come before the constraint
     *               expression.  This is usually a Structure, Sequence,
     *               or other container class.  If a '.' is needed between
     *               the prefix and the part of the CE this class generates,
     *               it must be included at the end of the prefix.
     * @return a DODS constraint expression that will return the variable
     *         represented by this VariableSelector.
     */
    public String generateCE_Projection(String prefix) {
        return "";
    }

    public String generateCE_Selection(String prefix) {
        return "";
    }

    /**
     * @return Whether or not this VariableSelector is selected.
     *         (whether or not it should be included in a generated CE)
     */
    public boolean isSelected() {
        return mzSelected;
    }

    /**
     * @return Whether or not this VariableSelector is selected uniquely out of all the variables.
     *         (determined from the radio button, not the check box)
     *         this mode is only used in the data viewer primarily to determine which data element
     *         is to be displayed
     */
    public boolean isSelected_unique() {
        return mzSelected_unique;
    }
    
	abstract public void vUpdateInfo( boolean zShowDetails );
	abstract public void vUpdateStep( int iStep );
	abstract public boolean hasStep();

	public void vUpdateSelection(){
		setSelected( checkSelection.isSelected() );
	}

    /**
     * Select or deselect the VariableSelector.  (Select the associated
     * button, and enable or disable the selector itself).
     * @param select Select(true) or Deselect(false).
     */
	boolean mzSettingSelected = false; // todo remove
    public void setSelected( boolean z ) {
		if( mzSettingSelected ){
			System.out.println("system error recursive, already setting selected");
			Thread.dumpStack();
			return;
		}
		mzSettingSelected = true;
        mzSelected = z;
		boolean zShowDescriptions = getOwner().zShowDescriptions();
		vUpdateInfo( zShowDescriptions );
		mpanelDetail.setVisible( z );

		// Whenever a selection is changed the subset is updated
		// the reason for this is that if the user selects just a map vector then
		// the "data" returned will the scale of vector. By selecting such
		// a vector is the only way to get the scale of a mapped dimension
		if( ApplicationController.getInstance().isConstraintChanging() ){
			// this is not a user change--the system is updating the radio button
		} else if( mRetrieveModel == null ){
			// this is not a constraint editor
		} else {
			mRetrieveModel.vUpdateSubset();
		}
		mzSettingSelected = false;
    }

}




