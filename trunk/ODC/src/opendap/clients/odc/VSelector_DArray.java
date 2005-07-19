/*
 * Created on December 21, 2001, 8:53 PM
 */
package opendap.clients.odc;

import java.lang.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import opendap.dap.*;
import java.util.regex.*;

/**
 * @author Rich Honhart, John Chamberlain
 */
public class VSelector_DArray extends VariableSelector {

	private final static String ENCODING = "UTF-8";

//	private JTextField[] jtfConstraint; // one-based
//    private String[] msDefaultConstraint;
//	private static String msPattern_Array_NoStride = "\\[(\\d+):(\\d+)\\]"; // $1 = start $2 = stop
//	private static String msPattern_Array_WithStride = "\\[(\\d+):(\\d+):(\\d+)\\]"; // $1 = start $2 = stride $3 = stop
//	private static java.util.regex.Pattern mPattern_Array_NoStride = null;
//	private static java.util.regex.Pattern mPattern_Array_WithStride = null;
	private DArray mDArray;
	private ArrayList listDimPanels;
	private int miDataWidth;

	DArray getArray(){ return mDArray; }

    /** Creates a new instance of DArraySelector */
    public VSelector_DArray( String sQualifiedName, DDSSelector owner, DArray darray, DAS das ) {

		super( owner, sQualifiedName );

		if( darray == null ){
			ApplicationController.getInstance().vShowError("Internal error, darray was missing in vselector setup");
			return;
		} else {
			mDArray = darray;
		}

		setBackground(Color.WHITE);

		String sArrayName = mDArray.getName();
		String sTypeName = DAP.getDArrayType_String(mDArray);

        setName(sArrayName);

		try {

			int ctDimensions = mDArray.numDimensions();

			// create dimension panels and add them the detail
			listDimPanels = new ArrayList();
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
						boolean zShowDescriptions = getOwner().mGenerator.zShowDescriptions();
						vUpdateInfo(zShowDescriptions); // so size of array is correct
				    }
					public void Cancel(){
				    }
				};
				Panel_Retrieve_Dimension panel = new Panel_Retrieve_Dimension(con);
				DArrayDimension dim = mDArray.getDimension(xDimension - 1); // zero-based array
				String sDimensionName = dim.getName();
				String sDimensionDescription = DAP.getAttributeString( das, sDimensionName );
				DArray darrayMapping = null; // arrays have no mappings
				if( panel.zInitialize(dim, darrayMapping, sDimensionDescription, sbError) ){
					listDimPanels.add(panel);
				} else {
					ApplicationController.getInstance().vShowError("Error in grid creating panel for dimension " + xDimension + ": " + sbError);
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

			setDescription( DAP.getAttributeString( das, sArrayName ) );

			boolean zShowDescriptions = getOwner().mGenerator.zShowDescriptions();
			vUpdateInfo(zShowDescriptions);
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, "While building array interface");
		}

    }

	public boolean hasStep(){ return true; }

	public void vUpdateStep( int iStep ){
		if( listDimPanels == null ) return;
		int ctDims = listDimPanels.size();
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Retrieve_Dimension dim_panel = (Panel_Retrieve_Dimension)listDimPanels.get(xDim-1); // zero-based
			dim_panel.setStep( iStep );
		}
	}

	StringBuffer msbLabel = new StringBuffer(80);
	public void vUpdateInfo( boolean zShowDescription ){

		// basic label (name + dimensions + size)
		msbLabel.setLength(0);
		String sName = getQualifiedName(); // does not work: DAP.getQualifiedName( mDArray );
		String sSize = Utility.getByteCountString( Panel_Retrieve_Dimension.getEstimatedSize(listDimPanels, miDataWidth ));
		msbLabel.append( sName ).append(' ');
		appendDimensionSizes( msbLabel );
		msbLabel.append(' ').append('(').append(sSize).append(')');

		if( zShowDescription ){
			String sTypeName = DAP.getDArrayType_String( mDArray );
			msbLabel.append(' ').append(sTypeName).append(" array");
		}

		setTitle( msbLabel.toString() );

		vShowDescription( zShowDescription && isSelected() );

		if( listDimPanels != null ){
			int ctDims = listDimPanels.size();
			for( int xDim = 1; xDim <= ctDims; xDim++ ){
				Panel_Retrieve_Dimension dim_panel = (Panel_Retrieve_Dimension)listDimPanels.get(xDim-1); // zero-based
				dim_panel.vUpdateInfo( zShowDescription );
			}
		}

	}

    public String generateCE_Projection(String prefix) {
		if( isSelected() ){
			String sVariableName = getQualifiedName();
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
			Panel_Retrieve_Dimension dim_panel = (Panel_Retrieve_Dimension)listDimPanels.get(xDim-1); // zero-based
			sbConstraint.append('[').append(dim_panel.getConstraint(sbConstraint)).append(']');
		}
		return sbConstraint.toString();
	}

    public String generateCE_Selection(String prefix) {
        return "";
    }

	private void appendDimensionSizes( StringBuffer sb ){
		if( listDimPanels == null ) return;
		int ctDims = listDimPanels.size();
		if( ctDims == 0 ) return;
		for( int xDim = 1; xDim <= ctDims; xDim++ ){
			Panel_Retrieve_Dimension dim_panel = (Panel_Retrieve_Dimension)listDimPanels.get(xDim-1); // zero-based
			sb.append('[').append(dim_panel.getDimensionLength()).append(']');
		}
	}

}

