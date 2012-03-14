/*
 * DDSSelector.java
 *
 * Created on December 21, 2001, 11:37 PM
 * new design April 2004 by John Chamberlain
 * expanded to support data viewing September 2008 by John Chamberlain
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

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.data.Model_Retrieve;
import opendap.dap.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;

/**
 * @author  Honhart, John Chamberlain
 */
public class Panel_DDSView extends JPanel {

	ArrayList<BaseType> listVariables = new ArrayList<BaseType>();
	ArrayList<String> listQualifiedNames = new ArrayList<String>();
	ArrayList<JPanel> listControls = new ArrayList<JPanel>();
	JPanel mpanelGlobalDAS;
	private javax.swing.ButtonGroup mButtonGroup = null;
	private boolean mzShowDescriptions = false;

	public enum eMODE {
		Select,
		Edit,
		Expression
	}
	
    public Panel_DDSView() {
//		setBorder( new javax.swing.border.EmptyBorder(0, 0, 0, 0) );
		setBorder( new javax.swing.border.LineBorder(Color.BLUE) );
		setBackground( Color.WHITE );
		setLayout( new GridBagLayout() );
    }

	void vClear(){
		listVariables.clear();
		listControls.clear();
		removeAll();
	}

    // edit mode is used by the data viewer for editing DataDDSs, value is false for regular CE display
	boolean zSetDDS( DDS dds, DAS das, boolean zShowDescriptions, eMODE mode, StringBuffer sbError ){
		try {
			vClear();
			if( dds == null ){
				sbError.append( "no DDS supplied" );
				return false;
			}
			int ctVariables = dds.numVariables();
			for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
				BaseType bt = dds.getVar(xVariable - 1); // zero-based
				if( !zAddBaseTypeToLists( bt, null, listVariables, listQualifiedNames, 1, sbError ) ){
					sbError.insert(0, "adding variable " + xVariable + ": ");
					return false;
				}
			}
			if( mode == eMODE.Edit ){
				mButtonGroup = new javax.swing.ButtonGroup();
				if( !zBuildInterface( listVariables, listQualifiedNames, das, zShowDescriptions, mButtonGroup, null, sbError ) ){
					sbError.insert(0, "building editing interface: ");
					return false;
				}
			} else {
				Model_Retrieve rm = ApplicationController.getInstance().getRetrieveModel();
				if( !zBuildInterface( listVariables, listQualifiedNames, das, zShowDescriptions, null, rm, sbError ) ){
					sbError.insert(0, "building display interface: ");
					return false;
				}
			}
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

    // edit mode is used by the data viewer for editing DataDDSs, value is false for regular CE display
	boolean zSetExpressionDDS( Model_Dataset model, StringBuffer sbError ){
		if( model == null ){
			sbError.append( "no model supplied" );
			return false;
		}
		eMODE mode = Panel_DDSView.eMODE.Expression;
		DDS dds = model.getDDS_Full();
		if( dds == null ){
			sbError.append( "model has no DDS" );
			return false;
		}
		DAS das = null;
		boolean zShowDescriptions = false;
		return zSetDDS( dds, das, zShowDescriptions, mode, sbError );
	}
	
	public boolean zShowDescriptions(){ return mzShowDescriptions; }

	public void setShowDescriptions( boolean z ){ mzShowDescriptions = z; }
	
	// adds the information in the DDS to the reference lists 
	private boolean zAddBaseTypeToLists( BaseType bt, String sParentName, ArrayList<BaseType> listBaseTypes, ArrayList<String> listQualifiedNames, int iLevel, StringBuffer sbError ){
		try {

			// create qualified name
			String sQualifiedName;
			if( sParentName == null ){
				sQualifiedName = bt.getName();
			} else {
				sQualifiedName = sParentName + '.' + bt.getName();
			}

			// create variable entry
			Enumeration<BaseType> enumVariables;
		    if( bt instanceof DGrid ){
				listQualifiedNames.add(sQualifiedName);
				listBaseTypes.add(bt); // add variable as leaf
				return true;
			} else if( bt instanceof DConstructor ){
				DConstructor dconstructor = (DConstructor)bt;
				enumVariables = dconstructor.getVariables();
			} else if( bt instanceof DStructure ){
				DStructure dstructure = (DStructure)bt;
				enumVariables = dstructure.getVariables();
			} else if( bt instanceof DSequence ){
				DSequence dsequence = (DSequence)bt;
				enumVariables = dsequence.getVariables();
			} else {
				listQualifiedNames.add(sQualifiedName);
				listBaseTypes.add(bt); // add variable as leaf
				return true;
			}
			while( enumVariables.hasMoreElements() ){
				Object o = enumVariables.nextElement();
				BaseType btChild = (BaseType)o;
				if( !zAddBaseTypeToLists( btChild, sQualifiedName, listBaseTypes, listQualifiedNames, iLevel + 1, sbError ) ){
					return false;
				}
			}
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	private boolean zBuildInterface( ArrayList<BaseType> listVariables, ArrayList<String> listQualifiedNames, DAS das, boolean zShowDescriptions, javax.swing.ButtonGroup bg, Model_Retrieve mr, StringBuffer sbError ){
		int ctVariables = listVariables.size();
		removeAll();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 1;
		gbc.weighty = 0;
		for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
			BaseType bt = (BaseType)listVariables.get( xVariable - 1 ); // zero-based
			String sName_Qualified = (String)listQualifiedNames.get( xVariable - 1 ); // zero-based
//			String sName_Simple = bt.getName();
			JPanel variable_panel;
			if( bt instanceof DGrid ){
				variable_panel = new VSelector_DGrid( sName_Qualified, this, (DGrid)bt, das, bg, mr );
			} else if(  bt instanceof DArray ){
				variable_panel = new VSelector_DArray( sName_Qualified, this, (DArray)bt, das, bg, mr );
			} else if(  bt instanceof DByte ||
						bt instanceof DInt16 ||
						bt instanceof DInt32 ||
						bt instanceof DFloat32 ||
						bt instanceof DFloat64 ||
						bt instanceof DString ){
				variable_panel = new VSelector_Generic( sName_Qualified, this, bt, das, bg, mr );
			} else if(  bt instanceof DVector ){
				variable_panel = new UnsupportedVariablePanel( sName_Qualified, this, bt );
				ApplicationController.vShowWarning("variable " + sName_Qualified + " ignored, unsupported type");
			} else {
				variable_panel = new UnsupportedVariablePanel( sName_Qualified, this, bt);
				ApplicationController.vShowWarning("variable " + sName_Qualified + " ignored, unknown type");
			}
			variable_panel.setOpaque(false);
			gbc.gridy = xVariable;
			add( variable_panel, gbc );
			listControls.add( variable_panel );
		}
		if( das != null ){
			vMakeGlobalDAS( das, zShowDescriptions );
		    gbc.gridy++;
		    add( mpanelGlobalDAS, gbc );
		}
		Component compVerticalFill = Box.createVerticalGlue();
		gbc.gridy++;
		gbc.weighty = 1;
		add( compVerticalFill, gbc );
		return true;
	}

	private void vMakeGlobalDAS( DAS das, boolean zShowDescriptions ){
		mpanelGlobalDAS = new JPanel();
		mpanelGlobalDAS.setOpaque( false );
		mpanelGlobalDAS.setVisible( zShowDescriptions );
		ArrayList<String> listGlobalNames = new ArrayList<String>();
		Enumeration enumNames = das.getNames();
		while( enumNames.hasMoreElements() ){
			String sAttributeName = (String)enumNames.nextElement();
			if( sAttributeName == null ) continue;
			int ctVariables = listVariables.size();
		    if( ctVariables == 0 ) return;
			int xVariable = 0;
			while( true ){
				xVariable++;
			    if( xVariable > ctVariables ){ // found a global container
			    	listGlobalNames.add( sAttributeName );
					break;
				}
	    		BaseType bt = (BaseType)listVariables.get( xVariable - 1 ); // zero-based
				String sVariableName = bt.getName();
				if( sAttributeName.equals( sVariableName ) ) break; // variable container
			}
		}
		mpanelGlobalDAS.setLayout( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		if( listGlobalNames.size() > 0 ){
			c.gridy++;
			mpanelGlobalDAS.add(Box.createVerticalStrut(10), c);
			JLabel jlabel = new JLabel("____________ Other Attributes _______________");
			jlabel.setOpaque(false);
			c.gridy++;
			mpanelGlobalDAS.add(jlabel, c);
			c.gridy++;
			mpanelGlobalDAS.add(Box.createVerticalStrut(5), c);
			JTextArea jtaAT = new JTextArea();
			jtaAT.setOpaque( false );
			for( int xDAS = 0; xDAS < listGlobalNames.size(); xDAS++ ){
				String sAttributeName = (String)listGlobalNames.get(xDAS);
				Attribute attribute = das.getAttribute(sAttributeName);
				if( attribute != null ){
					String sDump;
					if( attribute.isContainer() ){
						try {
							AttributeTable atGlobal = attribute.getContainer();
							sDump = DAP.dumpDAS(atGlobal);
						} catch( Exception ex ) {
							sDump = "[error]";
						}
					} else {
						java.io.StringWriter sw = new java.io.StringWriter();
						das.print(new java.io.PrintWriter(sw));
						sDump = sw.toString();
					}
					jtaAT.append(sDump);
				}
			}
			c.gridy++;
			mpanelGlobalDAS.add( jtaAT, c );
			c.gridy++;
			mpanelGlobalDAS.add( Box.createVerticalStrut(5), c );
		}
	}

	private String getDescription( AttributeTable at ){
		if( at == null ) return "[null]";
		StringBuffer sb = new StringBuffer( 160 );
		java.util.Enumeration enumAttributeNames = at.getNames();;
		while( enumAttributeNames.hasMoreElements() ){
			String sAttributeName = (String)enumAttributeNames.nextElement();
			Attribute attribute = at.getAttribute(sAttributeName);
			if (attribute != null) {
				if (attribute.isContainer()) {
					sb.append("  " + sAttributeName + ": {");
					try {
						AttributeTable atSubTable = attribute.getContainer();
						sb.append( getDescription(atSubTable) );
					} catch(Exception ex) {
						sb.append(": error: " + ex + "\n");
					}
					sb.append("}");
				} else {
					sb.append(sAttributeName + ": ");
					sb.append(getAttributeDescription(attribute));
				}
			}
		}
		return sb.toString();
	}

	private String findDescription( AttributeTable at, String sVariableName ){
		try {
			if( at == null || sVariableName == null ){
				return null;
			} else {
				if( sVariableName.equals( at.getName() ) ){
					return getDescription( at );
				} else {
					Enumeration enumNames = at.getNames();
					while( enumNames.hasMoreElements() ){
						String sName = (String)enumNames.nextElement();
						Attribute attribute = at.getAttribute(sName);
						if( attribute == null ){
							continue;
						} else if( attribute.isContainer() ){
							AttributeTable atSubtable = attribute.getContainer();
							String sAnswer = findDescription( atSubtable, sVariableName );
							if( sAnswer == null ) continue;
							return sAnswer;
						} else if( attribute.isAlias() ) {
							continue;
						} else {
							continue;
						}
					}
				}
				return null;
			}
		} catch(Exception ex) {
			return null;
		}
	}

	StringBuffer sbAttributeDescription = new StringBuffer(160);
	private String getAttributeDescription( Attribute attribute ){
		sbAttributeDescription.setLength(0);
		try {
			java.util.Enumeration enumAttributeValues = attribute.getValues();
			while(enumAttributeValues.hasMoreElements()){
				sbAttributeDescription.append((String)enumAttributeValues.nextElement()).append("; ");
			}
		} catch(Exception ex) {
			sbAttributeDescription.append(" error: " + ex);
		}
		return sbAttributeDescription.toString();
	}

    public String generateCE(String prefix) {

		// determine projection
		String sProjection = "";
		int ctVariables = listControls.size();
		for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
			VariableSelector vs = (VariableSelector)listControls.get(xVariable - 1);
			String sVariableProjection = vs.generateCE_Projection(prefix);
			if( sVariableProjection != null && sVariableProjection.length() > 0 ) sProjection += (sProjection.length() > 0 ? "," : "") + sVariableProjection;
		}

		// determine selection
		String sSelection = "";
		for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
			VariableSelector vs = (VariableSelector)listControls.get(xVariable - 1);
			String sVariableSelection = vs.generateCE_Selection(prefix);
			if( sVariableSelection != null && sVariableSelection.length() > 0 ) sSelection += (sSelection.length() > 0 ? "," : "") + sVariableSelection;
		}

// old way:

		// determine projection
//		String sProjection = "";
//        Enumeration children = getChildren();
//		children = getChildren();
//		while(children.hasMoreElements()) {
//			vselectorCurrentChild = (VariableSelector)children.nextElement();
//			if(vselectorCurrentChild.isEnabled()) {
//				String sChildProjection = vselectorCurrentChild.generateCE_Projection(prefix);
//				sChildProjection.trim();
//				if( sChildProjection.length() > 0 ){
//				    if( sProjection.length() > 0 ) sProjection += ",";
//					sProjection += sChildProjection;
//				}
//			}
//		}

		// determine selection
//		String sSelection = "";
//		children = getChildren();
//		while(children.hasMoreElements()) {
//			vselectorCurrentChild = (VariableSelector)children.nextElement();
//			sSelection += vselectorCurrentChild.generateCE_Selection(prefix);
//		}

		return sProjection + sSelection;
    }

    // returns the uniquely selected variable (if any)
    public VariableSelector getSelection(){
    	if( mButtonGroup == null ) return null;
		int ctVariables = listControls.size();
		for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
			VariableSelector vs = (VariableSelector)listControls.get(xVariable - 1);
			if( vs.isSelected_unique() ) return vs;
		}
		return null;
    }
    
	public void vUpdateSelections(){
		int ctVariables = listControls.size();
		for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
			VariableSelector vs = (VariableSelector)listControls.get(xVariable - 1);
			vs.vUpdateSelection(); // control should know whether it is selected or not (abstract method)434
		}
	}

	public void vUpdateInfo( boolean zShowDescription ){
		int ctVariables = listControls.size();
		for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
			VariableSelector vs = (VariableSelector)listControls.get(xVariable - 1);
			vs.vUpdateInfo( zShowDescription ); // control should know whether it is selected or not (abstract method)434
		}
		if( mpanelGlobalDAS != null ) mpanelGlobalDAS.setVisible( zShowDescription );
	}

	public void vUpdateStep( int iStep ){
		int ctVariables = listControls.size();
		boolean zDoUpdate = false;
		for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
			VariableSelector vs = (VariableSelector)listControls.get(xVariable - 1);
			if( vs.isSelected() && vs.hasStep() ){
				zDoUpdate = true;
				vs.vUpdateStep( iStep );
			}
		}
		if( zDoUpdate ) ApplicationController.getInstance().getRetrieveModel().vUpdateSubset();
	}

    public void vSelectAll( boolean z ) {
		int ctVariables = listControls.size();
		for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
			VariableSelector vs = (VariableSelector)listControls.get(xVariable - 1);
			vs.setSelected( z );
		}
    }
}

class UnsupportedVariablePanel extends VariableSelector {
	UnsupportedVariablePanel( String sQualifiedName, Panel_DDSView owner, BaseType bt ){
		super( owner, sQualifiedName, null, null );
		JLabel label = new JLabel( sQualifiedName );
		add( label );
	}
	public void vUpdateInfo( boolean z ){}
	public void setEnabled( boolean z ){}
	public boolean hasStep(){ return false; }
	public void vUpdateStep( int i ){}
}
