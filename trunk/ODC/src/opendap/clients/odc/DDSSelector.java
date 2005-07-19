/*
 * DDSSelector.java
 *
 * Created on December 21, 2001, 11:37 PM
 * new design April 2004 by John Chamberlain
 */

package opendap.clients.odc;

import opendap.dap.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;

/**
 * @author  Honhart, John Chamberlain
 */
public class DDSSelector extends JPanel {

	Panel_Retrieve_DDX mGenerator;
	ArrayList listVariables = new ArrayList();
	ArrayList listQualifiedNames = new ArrayList();
	ArrayList listControls = new ArrayList();
	JPanel mpanelGlobalDAS;

    public DDSSelector(Panel_Retrieve_DDX generator) {
		mGenerator = generator;
//		setBorder( new javax.swing.border.EmptyBorder(0, 0, 0, 0) );
//		setBorder( new javax.swing.border.LineBorder(Color.BLUE) );
		setBackground(Color.WHITE);
		setLayout( new GridBagLayout() );
    }

	boolean zSetDDS( DDS dds, DAS das, StringBuffer sbError ){
		try {
			vClear();
			int ctVariables = dds.numVariables();
			for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
				BaseType bt = dds.getVar(xVariable - 1); // zero-based
				if( !zAddBaseType( bt, null, listVariables, listQualifiedNames, 1, sbError ) ){
					sbError.insert(0, "error adding variable " + xVariable + ": ");
					return false;
				}
			}
			if( !zBuildInterface( listVariables, listQualifiedNames, das, sbError ) ){
				sbError.insert(0, "error building interface: ");
				return false;
			}
			return true;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	private boolean zAddBaseType( BaseType bt, String sParentName, ArrayList listBaseTypes, ArrayList listQualifiedNames, int iLevel, StringBuffer sbError ){
		try {

			// create qualified name
			String sQualifiedName;
			if( sParentName == null ){
				sQualifiedName = bt.getName();
			} else {
				sQualifiedName = sParentName + '.' + bt.getName();
			}

			// create variable entry
			Enumeration enum;
		    if( bt instanceof DGrid ){
				listQualifiedNames.add(sQualifiedName);
				listBaseTypes.add(bt); // add variable as leaf
				return true;
			} else if( bt instanceof DConstructor ){
				DConstructor dconstructor = (DConstructor)bt;
				enum = dconstructor.getVariables();
			} else if( bt instanceof DStructure ){
				DStructure dstructure = (DStructure)bt;
				enum = dstructure.getVariables();
			} else if( bt instanceof DSequence ){
				DSequence dsequence = (DSequence)bt;
				enum = dsequence.getVariables();
			} else {
				listQualifiedNames.add(sQualifiedName);
				listBaseTypes.add(bt); // add variable as leaf
				return true;
			}
			while( enum.hasMoreElements() ){
				Object o = enum.nextElement();
				BaseType btChild = (BaseType)o;
				if( !zAddBaseType( btChild, sQualifiedName, listBaseTypes, listQualifiedNames, iLevel + 1, sbError ) ){
					return false;
				}
			}
			return true;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	void vClear(){
		listVariables.clear();
		listControls.clear();
		removeAll();
	}

	boolean zBuildInterface( ArrayList listVariables, ArrayList listQualifiedNames, DAS das, StringBuffer sbError ){
		int ctVariables = listVariables.size();
		if( ctVariables == 0 ){
			sbError.append("DDS has no variables");
			return false;
		}
		removeAll();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 1;
		gbc.weighty = 0;
		for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
			BaseType bt = (BaseType)listVariables.get( xVariable - 1 ); // zero-based
			String sName_Qualified = (String)listQualifiedNames.get( xVariable - 1 ); // zero-based
			String sName_Simple = bt.getName();
			JPanel variable_panel;
			if( bt instanceof DGrid ){
				variable_panel = new VSelector_DGrid( sName_Qualified, this, (DGrid)bt, das );
			} else if(  bt instanceof DArray ){
				variable_panel = new VSelector_DArray( sName_Qualified, this, (DArray)bt, das );
			} else if(  bt instanceof DBoolean ||
						bt instanceof DByte ||
						bt instanceof DInt16 ||
						bt instanceof DInt32 ||
						bt instanceof DFloat32 ||
						bt instanceof DFloat64 ||
						bt instanceof DString ){
				variable_panel = new VSelector_Generic( sName_Qualified, this, bt, das );
			} else if(  bt instanceof DList ||
						bt instanceof DVector ){
				variable_panel = new UnsupportedVariablePanel( sName_Qualified, this, bt );
				ApplicationController.getInstance().vShowWarning("variable " + sName_Qualified + " ignored, unsupported type");
			} else {
				variable_panel = new UnsupportedVariablePanel( sName_Qualified, this, bt);
				ApplicationController.getInstance().vShowWarning("variable " + sName_Qualified + " ignored, unknown type");
			}
			variable_panel.setOpaque(false);
			gbc.gridy = xVariable;
			add( variable_panel, gbc );
			listControls.add( variable_panel );
		}
		if( das != null ){
			vMakeGlobalDAS( das );
		    gbc.gridy++;
		    add( mpanelGlobalDAS, gbc );
		}
		Component compVerticalFill = Box.createVerticalGlue();
		gbc.gridy++;
		gbc.weighty = 1;
		add( compVerticalFill, gbc );
		return true;
	}

	private void vMakeGlobalDAS( DAS das ){
		mpanelGlobalDAS = new JPanel();
		mpanelGlobalDAS.setOpaque( false );
		boolean zShowDescriptions = mGenerator.zShowDescriptions();
		mpanelGlobalDAS.setVisible( zShowDescriptions );
		ArrayList listGlobalNames = new ArrayList();
		Enumeration enum = das.getNames();
		while( enum.hasMoreElements() ){
			String sAttributeName = (String)enum.nextElement();
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
		while(enumAttributeNames.hasMoreElements()){
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
					Enumeration enum = at.getNames();
					while( enum.hasMoreElements() ){
						String sName = (String)enum.nextElement();
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
	UnsupportedVariablePanel( String sQualifiedName, DDSSelector owner, BaseType bt ){
		super( owner, sQualifiedName );
		JLabel label = new JLabel( sQualifiedName );
		add( label );
	}
	public void vUpdateInfo( boolean z ){}
	public void setEnabled( boolean z ){}
	public boolean hasStep(){ return false; }
	public void vUpdateStep( int i ){}
}
