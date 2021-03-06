package opendap.clients.odc.gui;

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

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Continuation_DoCancel;
import opendap.clients.odc.DAP;
import opendap.clients.odc.Model;
import opendap.clients.odc.Utility_String;
import opendap.dap.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;

public class Panel_Retrieve_Dimension extends JPanel {
	final public static int MODE_Step_Text = 1;
	final public static int MODE_Step_List = 2;
	final public static int MODE_Mapping_Index = 1;
	final public static int MODE_Mapping_Value = 1;
	int eMODE_Step = 0;
	int eMODE_Mapping = MODE_Mapping_Index;
	DArrayDimension mDimension;
	DArray mMappingArray;
	JLabel jlabelName;
	JTextField jtfFrom;
	JTextField jtfTo;
	JTextField jtfStride;
	String msDimensionName;
	int miDimensionStart;
	int miDimensionStride;
	int miDimensionStop;
	JComboBox jcFrom, jcTo, jcStep;
	JPanel mpanelField;
	JPanel mpanelDescription;
	JTextArea mjtaDescription;
	Continuation_DoCancel mDoUpdate;

	Panel_Retrieve_Dimension( Continuation_DoCancel doUpdate ){
		mDoUpdate = doUpdate;
	}

	public void setEnabled( boolean zEnabled ){
		super.setEnabled( zEnabled );
		if( jtfFrom != null ) jtfFrom.setEnabled( zEnabled );
		if( jtfTo != null ) jtfTo.setEnabled( zEnabled );
		if( jtfStride != null ) jtfStride.setEnabled( zEnabled );
		if( jcFrom != null ) jcFrom.setEnabled( zEnabled );
		if( jcTo != null ) jcTo.setEnabled( zEnabled );
		if( jcStep != null ) jcStep.setEnabled( zEnabled );
	}

	public void setStep( int iStep ){
		int iListSize = jcStep.getModel().getSize();
		for( int xList = 0; xList < iListSize; xList++ ){
			Object oItem = jcStep.getModel().getElementAt(xList);
			int iInteger = Utility_String.parseInteger_positive( oItem.toString() );
			if( iInteger == iStep ){
				jcStep.setSelectedIndex(xList);
				break;
			}
		}
	}

	int getValue_From(){
		int xFrom = miDimensionStart;
		if( eMODE_Step == MODE_Step_Text ){
			String sValue = jtfFrom.getText();
			xFrom = Utility_String.parseInteger_nonnegative( sValue );
			if( xFrom == -1 ){
				xFrom = 1;
				ApplicationController.vShowWarning("unable to interpret " + sValue + " as non-negative integer in constraint for grid dim " + msDimensionName);
			}
		} else {
			xFrom = jcFrom.getSelectedIndex();
		}
		return xFrom;
	}
	int getValue_To(){
		int xTo = miDimensionStop;
		if( eMODE_Step == MODE_Step_Text ){
		    String sValue = jtfTo.getText();
		    xTo = Utility_String.parseInteger_nonnegative( sValue );
			if( xTo == -1 ){
				xTo = miDimensionStop;
				ApplicationController.vShowWarning( "unable to interpret " + sValue + " as non-negative integer in constraint for grid dim " + msDimensionName);
			}
		} else {
			xTo = jcTo.getSelectedIndex();
		}
		return xTo;
	}
	int getValue_Stride(){
		int xStride = miDimensionStride;
//		if( eMODE == MODE_Text ){
//			String sValue = jtfStride.getText();
//			xStride = Utility.parseInteger_positive( sValue );
//			if( xStride == -1 ){
//				xStride = miDimensionStride;
//				ApplicationController.vShowWarning("unable to interpret " + sValue + " as positive integer in constraint for grid dim " + msDimensionName);
//			}
//		} else if( eMODE == MODE_List ){
//		}

		// right now always using combo box
			String sStride = jcStep.getSelectedItem().toString();
			xStride = Utility_String.parseInteger_positive( sStride );
			if( xStride == -1 ){
				xStride = miDimensionStride;
				ApplicationController.vShowWarning("unable to interpret " + sStride + " as positive integer in constraint for grid dim " + msDimensionName);
			}
		return xStride;
	}
	String getConstraint( StringBuffer sbError ){
		int xFrom = getValue_From();
		int xTo = getValue_To();
		int xStride = getValue_Stride();
		if( xStride == 0 ){
			return	"" + xFrom + ":" + xTo;
		} else {
			return	"" + xFrom + ":" + xStride + ":" + xTo;
		}
	}
	int getDimensionLength(){
		return (getValue_To() - getValue_From())/getValue_Stride() + 1;
	}
	boolean zInitialize( DArrayDimension dim, DArray arrayMapping, String sDescription, StringBuffer sbError ){
		mDimension = dim;
		setOpaque(false);
		if( dim == null ){
			sbError.append("input missing");
			return false;
		}
		msDimensionName = dim.getName();
		miDimensionStart = dim.getStart();
		miDimensionStride = dim.getStride();
		miDimensionStop = dim.getStop();
		int iRange = miDimensionStop - miDimensionStart + 1;

		if( iRange < 10 ){
			String[] asStride = { "1", "2", "3", "4", "5" };
			jcStep = new JComboBox(asStride);
		} else if( iRange < 50 ) {
			String[] asStride = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "12", "15", "20" };
			jcStep = new JComboBox(asStride);
		} else if( iRange < 1000 ){
			String[] asStride = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "12", "15", "20", "25", "30", "40", "50", "75", "100" };
			jcStep = new JComboBox(asStride);
		} else {
			String[] asStride = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "12", "15", "20", "25", "30", "40", "50", "75", "100", "250", "500", "1000" };
			jcStep = new JComboBox(asStride);
		}
		jcStep.setOpaque(false);

		mpanelField = new JPanel();
		mpanelField.setOpaque(false);
		mpanelField.setLayout( new BoxLayout(mpanelField, BoxLayout.X_AXIS) );
		jlabelName = new JLabel();
		JLabel labelTo = new JLabel(" to ");
		JLabel labelStep = new JLabel(" step ");
		jlabelName.setOpaque(false);
		labelTo.setOpaque(false);
		labelStep.setOpaque(false);
		if( iRange > 1000 || mMappingArray == null ){
			eMODE_Step = MODE_Step_Text;
			FocusListener fl =
				new FocusAdapter(){
					public void focusLost(FocusEvent evt) {
						mDoUpdate.Do();
						Model.get().getRetrieveModel().vUpdateSubset();
					}
				};
			KeyListener kl =
				new KeyAdapter(){
				    public void keyPressed( KeyEvent ke ){
						int iKeyCode = ke.getKeyCode();
						if( iKeyCode == KeyEvent.VK_ENTER )
							mDoUpdate.Do();
							Model.get().getRetrieveModel().vUpdateSubset();
					}
				};

			jtfFrom = new JTextField(4);
			jtfFrom.setOpaque(false);
			jtfFrom.setText(Integer.toString(miDimensionStart));
			jtfFrom.addFocusListener(fl);
			jtfFrom.addKeyListener(kl);

			jtfTo = new JTextField(4);
			jtfTo.setOpaque(false);
			jtfTo.setText(Integer.toString(miDimensionStop));
			jtfTo.addFocusListener(fl);
			jtfTo.addKeyListener(kl);

			jcStep.addActionListener(
				new ActionListener(){
	    		    public void actionPerformed( java.awt.event.ActionEvent ae ){
						mDoUpdate.Do();
						Model.get().getRetrieveModel().vUpdateSubset();
		    		}
			    }
			);

//			jtfStride = new JTextField(4);
//			jtfStride = new JTextField(4);
//			jtfStride.setOpaque(false);
//			jtfStride.setText(Integer.toString(miDimensionStride));
//			jtfStride.addFocusListener(fl);
//			jtfStride.addKeyListener(kl);

			mpanelField.add(jlabelName);
			mpanelField.add(jtfFrom);
			mpanelField.add(labelTo);
			mpanelField.add(jtfTo);
			mpanelField.add(labelStep);
			mpanelField.add(jcStep);
		} else {
			eMODE_Step = MODE_Step_List;
			String[] asMappingValues = DAP.getDArrayStringVector0(arrayMapping, sbError);
			if( asMappingValues == null ) return false;
			jcFrom = new JComboBox(asMappingValues);
			jcFrom.setOpaque(false);
			jcTo = new JComboBox(asMappingValues);
			jcTo.setOpaque(false);
			mpanelField.add(jlabelName);
			mpanelField.add(jcFrom);
			mpanelField.add(labelTo);
			mpanelField.add(jcTo);
			mpanelField.add(labelStep);
			mpanelField.add(jcStep);
		}

		mjtaDescription = new JTextArea();
		if( sDescription != null ) mjtaDescription.setText( sDescription );
		mpanelDescription = new JPanel();
		mpanelDescription.setOpaque(false);
		mpanelDescription.setVisible(false);
		mpanelDescription.setLayout( new BorderLayout() );
		mpanelDescription.add( mjtaDescription, BorderLayout.CENTER );

		setLayout(new BorderLayout());
		add( mpanelField, BorderLayout.NORTH );
		add( mpanelDescription, BorderLayout.CENTER );

		return true;
	}

	public void vUpdateInfo( boolean zShowDescription ){
		jlabelName.setText( mDimension.getName() + (eMODE_Mapping == MODE_Mapping_Index ? " index" : "") + ": ");
		String sDescriptionText = mjtaDescription.getText();
		if( sDescriptionText == null ){
			mpanelDescription.setVisible( false );
			return;
		}
		if( sDescriptionText.length() == 0 ){
			mpanelDescription.setVisible( false );
			return;
		}
		mpanelDescription.setVisible( zShowDescription );
	}

	public static long getEstimatedSize( ArrayList<Panel_Retrieve_Dimension> listDimPanels, int iWidth ){
		if( listDimPanels == null ) return 0;
		int ctDims = listDimPanels.size();
		if( ctDims == 0 ) return 0;
		long nSize = 1;
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Retrieve_Dimension dim_panel = (Panel_Retrieve_Dimension)listDimPanels.get(xDim-1); // zero-based
			nSize *= dim_panel.getDimensionLength();
		}
		return nSize * iWidth;
	}

}
