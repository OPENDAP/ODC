/*
 * GenericSelector.java
 *
 * Created on December 23, 2001, 2:55 PM
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

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.data.Model_Retrieve;
import opendap.dap.*;

/**
 * @author  Honhart, Chamberlain
 */
public class VSelector_Generic extends VariableSelector {

	private ArrayList<Constraint> listConstraint = new ArrayList<Constraint>();

	BaseType mbtVariable;
	int miDataWidth;

    /** Creates a new instance of GenericSelector */
    public VSelector_Generic( String sQualifiedName, Panel_DDSView owner, BaseType var, DAS das, javax.swing.ButtonGroup bg, Model_Retrieve rm ) {

		super( owner, sQualifiedName, bg, rm );

		setBackground(Color.WHITE);

		if( var == null ){
			ApplicationController.vShowError("Internal error, generic variable was missing");
			return;
		}

		mbtVariable = var;

		String sName = var.getLongName();
		String sTypeName = var.getTypeName();
		vAddNewConstraint();

		try {
			switch( DAP.getTypeByName(sTypeName) ){
				case DAP.DATA_TYPE_Byte: miDataWidth = 1; break;
				case DAP.DATA_TYPE_Int16: miDataWidth = 2; break;
				case DAP.DATA_TYPE_Int32: miDataWidth = 4; break;
				case DAP.DATA_TYPE_UInt16: miDataWidth = 4; break;
				case DAP.DATA_TYPE_UInt32: miDataWidth = 8; break;
				case DAP.DATA_TYPE_Float32: miDataWidth = 4; break;
				case DAP.DATA_TYPE_Float64: miDataWidth = 8; break;
				case DAP.DATA_TYPE_String: miDataWidth = 24; break;
	    		default: miDataWidth = 8; break;
			}

			setDescription( DAP.getAttributeString( das, sName ) );

			boolean zShowDescriptions = getOwner().zShowDescriptions();
			vUpdateInfo(zShowDescriptions);
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, "While building generic interface for " + sName);
		}

    }

	StringBuffer msbLabel = new StringBuffer(80);
	public void vUpdateInfo( boolean zShowDescription ){
		msbLabel.setLength(0);
		String sName = getQualifiedName();
// TODO		String sSize = ""; // cannot estimate size Utility.getByteCountString( Panel_Retrieve_Dimension.getEstimatedSize(listDimPanels, miDataWidth ));
		msbLabel.append( sName ).append(' ');
		msbLabel.append(' '); // cannot estimate size .append('(').append(sSize).append(')');

		if( zShowDescription ){
			String sTypeName = DAP.getType_String( mbtVariable );
			msbLabel.append('(').append(sTypeName).append(')');
		}

		vShowDescription( zShowDescription && isSelected() );

		setTitle( msbLabel.toString() );

	}

	public void vUpdateStep( int iStep ){ return; } // no step for generics

	public boolean hasStep(){ return false; }

    public String generateCE_Projection(String prefix) {
		if( isSelected() ){
			return prefix + getQualifiedName();
		} else {
			return "";
		}
    }

    public String generateCE_Selection(String prefix) {
		String ce = "";
		int ctConstraints = listConstraint.size();
		for( int xConstraint = 0; xConstraint < ctConstraints; xConstraint++ ){
			Constraint constraintCurrent = (Constraint)listConstraint.get(xConstraint);
			if( constraintCurrent.isEnabled() ){
				String sRelation = constraintCurrent.getRelation();
				String sValue = constraintCurrent.getValue();
				if( sRelation != null && sValue != null ){
					ce += "&" + prefix +  getQualifiedName() + sRelation + sValue;
				}
			}
		}
        return ce;
    }

    public void reset() {
		setSelected(true);
		int ctConstraints = listConstraint.size();
		for( int xConstraint = 0; xConstraint < ctConstraints; xConstraint++ ){
			Constraint constraintCurrent = listConstraint.get(xConstraint);
			constraintCurrent.vReset();
		}
    }

	void vAddNewConstraint(){
		Constraint constraint = new Constraint();
		listConstraint.add(constraint);
		mpanelHeader.add( constraint.getCombo() );
		mpanelHeader.add( constraint.getTextField() );
	}

	class Constraint {

		private String[] asConstraintOperators = { "", "=", "!=", ">", "<", ">=", "<=" };
		private JComboBox jcbConstraint;
		private JTextField jtfConstraint;
		String sRelation = null;
		String sValue = null;
		boolean zLastConstraint = true;

		Constraint(){

			// set up combo box
			jcbConstraint = new JComboBox(asConstraintOperators);
			jcbConstraint.setOpaque(false);
			jcbConstraint.addItemListener(
				new ItemListener(){
				    public void itemStateChanged(ItemEvent ie){
						String sItem = (String)ie.getItem();
						if( sItem.trim().length() == 0 ){
							sRelation = null;
							sValue = null;
							jtfConstraint.setEnabled(false);
						} else {
							sRelation = sItem;
							if( sRelation.trim().length() == 0 ){
								sRelation = null;
								sValue = null;
								jtfConstraint.setEnabled(false);
							} else {
								sRelation = sItem;
								jtfConstraint.setEnabled(true);
							}
							if( zLastConstraint ){
								zLastConstraint = false;
								VSelector_Generic.this.vAddNewConstraint();
							}
						}
						ApplicationController.getInstance().getRetrieveModel().vUpdateSubset();
					}
				}
			);

			// set up text box
			jtfConstraint = new JTextField("");
			jtfConstraint.setOpaque(false);
			jtfConstraint.setPreferredSize(new Dimension(60, 25));
			jtfConstraint.addFocusListener(
				new FocusAdapter(){
					public void focusLost(FocusEvent evt) {
						Constraint.this.vEnter();
					}
				}
			);
			jtfConstraint.addKeyListener(
				new KeyAdapter(){
				    public void keyPressed( KeyEvent ke ){
						int iKeyCode = ke.getKeyCode();
						if( iKeyCode == KeyEvent.VK_ENTER ) Constraint.this.vEnter();
					}
				}
			);
			jtfConstraint.setEnabled(false);
		}

		void vEnter(){
			String sText = jtfConstraint.getText();
			if( sText.trim().length() == 0 ){
				sValue = null;
			} else {
				sValue = sText;
			}
			ApplicationController.getInstance().getRetrieveModel().vUpdateSubset();
		}

		void vReset(){
			sValue = null;
			sRelation = null;
			jcbConstraint.setSelectedIndex(0);
			jtfConstraint.setText("");
		}

		boolean isEnabled(){
			if( sValue != null && sRelation != null ) return true;
			return false;
		}

		JComboBox getCombo(){ return jcbConstraint; }
		JTextField getTextField(){ return jtfConstraint; }
		String getRelation(){ return sRelation; }
		String getValue(){ return sValue; }

	}

}


