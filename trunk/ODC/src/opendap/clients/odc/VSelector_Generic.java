/*
 * GenericSelector.java
 *
 * Created on December 23, 2001, 2:55 PM
 */

package opendap.clients.odc;

import java.lang.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import opendap.dap.*;

/**
 * @author  Honhart, Chamberlain
 */
public class VSelector_Generic extends VariableSelector {

	private ArrayList listConstraint = new ArrayList();

	BaseType mbtVariable;
	int miDataWidth;

    /** Creates a new instance of GenericSelector */
    public VSelector_Generic( String sQualifiedName, DDSSelector owner, BaseType var, DAS das ) {

		super( owner, sQualifiedName );

		setBackground(Color.WHITE);

		if( var == null ){
			ApplicationController.getInstance().vShowError("Internal error, generic variable was missing");
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

			boolean zShowDescriptions = getOwner().mGenerator.zShowDescriptions();
			vUpdateInfo(zShowDescriptions);
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, "While building generic interface for " + sName);
		}

    }

	StringBuffer msbLabel = new StringBuffer(80);
	public void vUpdateInfo( boolean zShowDescription ){
		msbLabel.setLength(0);
		String sName = getQualifiedName();
		String sSize = ""; // cannot estimate size Utility.getByteCountString( Panel_Retrieve_Dimension.getEstimatedSize(listDimPanels, miDataWidth ));
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
			Constraint constraintCurrent = (Constraint)listConstraint.get(xConstraint);
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
						if( iKeyCode == ke.VK_ENTER ) Constraint.this.vEnter();
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


