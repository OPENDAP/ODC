/*
 * DGridSelector.java
 *
 * Created on   December 27, 2001, 9:29 PM
 * Version 2    December 2002
 * Version 2.45 May 2004
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

package opendap.clients.odc;

import opendap.dap.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
//import java.util.regex.*;

/**
 * This class allows the user to constrain a variable of type DGrid.
 * It's the same a DConstructorSelector except that it keeps the
 * ranges on the map vectors in sync with the ranges on the main array.
 *
 * @author Richard Honhart, John Chamberlain
 */
public class VSelector_DGrid extends VariableSelector {

//    private VSelector_DArray m_vsdarray; TODO what is this?
	private DGrid  mDGrid;
	private DArray mDArray;
	private ArrayList<Panel_Retrieve_Dimension> listDimPanels;
	private int miDataWidth = 1;
// stride patterns not used TODO
//	private static String msPattern_Array_NoStride = "\\[(\\d+):(\\d+)\\]"; // $1 = start $2 = stop
//	private static String msPattern_Array_WithStride = "\\[(\\d+):(\\d+):(\\d+)\\]"; // $1 = start $2 = stride $3 = stop
	// private static java.util.regex.Pattern mPattern_Array_NoStride = null;
	// private static java.util.regex.Pattern mPattern_Array_WithStride = null;

    /** Creates a new instance of GridSelector */
    public VSelector_DGrid( String sQualifiedName, Panel_DDSView owner, DGrid grid, DAS das, javax.swing.ButtonGroup bg, Model_Retrieve mr ){
		super( owner, sQualifiedName, bg, mr );
        try {
//			mPattern_Array_NoStride = Pattern.compile(msPattern_Array_NoStride);
//			mPattern_Array_WithStride = Pattern.compile(msPattern_Array_WithStride);
        } catch( Exception ex ) {
			ApplicationController.vShowError("Internal error building DGrid patterns: " + ex);
        }

		if( grid == null ){
			ApplicationController.vShowError("Internal error, grid was missing");
			return;
		}

		mDGrid = grid;

		try {

			// obtain main array variable
			Object oArrayVariable = mDGrid.getVar(0);
			if( oArrayVariable != null && oArrayVariable instanceof DArray ){
				mDArray = (DArray)oArrayVariable;
			} else {
				ApplicationController.vShowError("Internal error, could not find array content of grid " + sQualifiedName);
				return;
			}

			// validate count of mapping variables
			int ctDimensions = mDArray.numDimensions();
			int ctMappingVariables = mDGrid.elementCount(false) - 1;
			if( ctDimensions != ctMappingVariables ){
				ApplicationController.vShowError("Error: grid " + sQualifiedName + " has " + ctDimensions + " dimensions, but " + ctMappingVariables + " mapping variables");
				return;
			}

// labelTitle.setBorder(javax.swing.BorderFactory.createLineBorder(Color.RED));
			listDimPanels = new ArrayList<Panel_Retrieve_Dimension>();

			// create dimension panels and add them the detail
//			mpanelDetail.setBorder( javax.swing.BorderFactory.createLineBorder(Color.BLUE) );
			mpanelDetail.removeAll();
			GridBagConstraints gbc = new GridBagConstraints();
			mpanelDetail.setLayout(new GridBagLayout());
			gbc.anchor = GridBagConstraints.NORTHWEST;
			gbc.gridy = 0; gbc.gridx = 0;
			gbc.gridwidth = 4; gbc.gridheight = 1;
			mpanelDetail.add( Box.createHorizontalStrut(4), gbc ); // spacing
			StringBuffer sbError = new StringBuffer(80);
			for( int xDimension = 1; xDimension <= ctDimensions; xDimension++ ){
				Continuation_DoCancel con = new Continuation_DoCancel(){
					public void Do(){
						boolean zShowDescriptions = getOwner().zShowDescriptions();
						vUpdateInfo(zShowDescriptions); // so size of array is correct
				    }
					public void Cancel(){
				    }
				};
				Panel_Retrieve_Dimension panel = new Panel_Retrieve_Dimension(con);
				DArrayDimension dim = mDArray.getDimension(xDimension - 1); // zero-based array
				String sDimensionName = dim.getName();
				DArray darrayMapping = (DArray)mDGrid.getVar(xDimension);
				String sDimensionDescription = DAP.getAttributeString( das, sDimensionName );
				if( panel.zInitialize(dim, darrayMapping, sDimensionDescription, sbError) ){
					listDimPanels.add(panel);
				} else {
					ApplicationController.vShowError("Error in grid creating panel for dimension " + xDimension + ": " + sbError);
					return;
				}
				gbc.gridwidth = 2;
				gbc.gridx = 0;
				gbc.gridy = xDimension * 2 - 1;
				mpanelDetail.add( Box.createVerticalStrut(5), gbc );
				gbc.gridy = xDimension * 2;
				gbc.gridwidth = 1;
				mpanelDetail.add( Box.createHorizontalStrut(25), gbc);
				gbc.gridx = 1;
				mpanelDetail.add( panel, gbc);
			}

			switch( DAP.getDArrayType(mDArray) ){
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
			setDescription( DAP.getAttributeString( das, sQualifiedName ) );
			boolean zShowDescriptions = getOwner().zShowDescriptions();
			vUpdateInfo(zShowDescriptions);
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, "While building grid interface");
		}
    }

	public boolean hasStep(){ return true; }

	public void vUpdateStep( int iStep ){
		if( listDimPanels == null ) return;
		int ctDims = listDimPanels.size();
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Retrieve_Dimension dim_panel = listDimPanels.get(xDim-1); // zero-based
			dim_panel.setStep( iStep );
		}
	}

    public String generateCE_Projection(String prefix) {
		if( isSelected() ){
			String sVariableName = getQualifiedName( mDGrid );
			String ce = prefix + sVariableName + getConstraint();
			return ce;
		} else {
			return "";
		}
    }

	private String getConstraint(){
		if( listDimPanels == null ) return "";
		int ctDims = listDimPanels.size();
		if( ctDims == 0 ) return "";
		StringBuffer sbConstraint = new StringBuffer(50);
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Retrieve_Dimension dim_panel = listDimPanels.get(xDim-1); // zero-based
			sbConstraint.append('[').append(dim_panel.getConstraint(sbConstraint)).append(']');
		}
		return sbConstraint.toString();
	}

	StringBuffer msbLabel = new StringBuffer(80);
	public void vUpdateInfo( boolean zShowDescription ){

		// basic label (name + dimensions + size)
		msbLabel.setLength(0);
		String sName = getQualifiedName( mDGrid );
		long nSize = Panel_Retrieve_Dimension.getEstimatedSize( listDimPanels, miDataWidth );
		String sSize = Utility.getByteCountString( nSize );
		msbLabel.append( sName ).append(' ');
		appendDimensionSizes( msbLabel );
		msbLabel.append(' ').append('(').append(sSize).append(')');

		if( zShowDescription ){
			String sTypeName = DAP.getDArrayType_String( mDArray );
			msbLabel.append(' ').append(sTypeName).append(" grid");
		}

		setTitle( msbLabel.toString() );

		vShowDescription( zShowDescription && isSelected() );

		if( listDimPanels != null ){
			int ctDims = listDimPanels.size();
			for( int xDim = 1; xDim <= ctDims; xDim++ ){
				Panel_Retrieve_Dimension dim_panel = listDimPanels.get(xDim-1); // zero-based
				dim_panel.vUpdateInfo( zShowDescription );
			}
		}
	}

	private void appendDimensionSizes( StringBuffer sb ){
		if( listDimPanels == null ) return;
		int ctDims = listDimPanels.size();
		if( ctDims == 0 ) return;
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Retrieve_Dimension dim_panel = listDimPanels.get(xDim-1); // zero-based
			sb.append('[').append(dim_panel.getDimensionLength()).append(']');
		}
	}

	String getQualifiedName( BaseType bt ){
		if( bt == null ) return "";
		StringBuffer sbName = new StringBuffer(80);
		sbName.append(bt.getName());
		java.util.ArrayList<BaseType> listParents = new java.util.ArrayList<BaseType>();
		while( true ){
			BaseType btParent = bt.getParent();
			if( btParent == null || btParent == bt || listParents.contains(btParent) ) break; // this is to protect against circularity which appears to occur sometimes for some reason (example columbia world ocean atlas)
			listParents.add( btParent );
			sbName.insert(0, btParent.getName() + '.');
		}
		return sbName.toString();
	}

}




