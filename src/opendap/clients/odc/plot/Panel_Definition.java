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

package opendap.clients.odc.plot;

import opendap.dap.*;
import opendap.clients.odc.*;
import opendap.clients.odc.data.Model_Dataset;

import javax.swing.JPanel;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

// The definition panel has the controls which allow the user to define what is plotted.
//
// It has tabs for variables, colors, scale, text etc. The most important sub panel is the
// one for defining which variable or variables from the dataset are to be used. For details
// on this panel see the Panel_Variables class.
//
// Center of Panel_View_Plot qv

/*
	SplitPane Left:
		Panel_Composition
			NORTH: Composition Controls
			CENTER: Composition Canvas
	SplitPane Right:
		Panel_Composition_Element
			LEFT: Panel_Element_Configuration
			CENTER: Panel_Configuration_Detail
				- Panel_Variables
				- Panel_PlotOptions
				- Panel_PlotText
				- Panel_PlotScale
				- Panel_PlotAxes
				- Panel_PlotColors
 */

public class Panel_Definition extends JPanel {
	final static long serialVersionUID = 1;

	private final static int PANEL_INDEX_Variables = 0;
	private final static int PANEL_INDEX_Preview = 5;
	private final static int PANEL_INDEX_Thumbnails = 6;

	private Panel_View_Plot mParent;

	private final Panel_VariableTab mpanelVariables = new Panel_VariableTab();
	private final JTabbedPane mjtpPlotDefinition = new JTabbedPane();
	private final Panel_PlotOptions mpanelOptions = new Panel_PlotOptions();
	private final Panel_PlotScale mpanelScale = new Panel_PlotScale();
	private final Panel_PlotText mpanelText = new Panel_PlotText();
	private final Panel_PlotAxes mpanelAxes = new Panel_PlotAxes();
	private final PreviewPane mPreviewPane = new PreviewPane();
	private final Panel_ColorSpecification mpanelColors = new Panel_ColorSpecification();
	private final Panel_Thumbnails mpanelThumbnails = new Panel_Thumbnails(this);

	public PreviewPane getPreviewScrollPane(){ return mPreviewPane; }
	public Panel_VariableTab getPanel_Variables(){ return mpanelVariables; }
	public Panel_PlotScale getPanel_PlotScale(){ return mpanelScale; }
	public Panel_PlotText getPanel_PlotText(){ return mpanelText; }
	public Panel_PlotAxes getPanel_PlotAxes(){ return mpanelAxes; }
	public Panel_PlotOptions getPanel_PlotOptions(){ return mpanelOptions; }
	public Panel_ColorSpecification getPanel_ColorSpecification(){ return mpanelColors; }
	public Panel_Thumbnails getPanel_Thumbnails(){ return mpanelThumbnails; }
	public JTabbedPane getTabbedPane(){ return mjtpPlotDefinition; }

	boolean zInitialize( final Panel_View_Plot parent, StringBuffer sbError ){
		if( parent == null ){
			sbError.append("no parent");
			return false;
		}
		mParent = parent;

		if( ! mpanelVariables.zInitialize( this, sbError) ){
			sbError.insert(0, "Failed to initialize variable panel: ");
			return false;
		}

		JScrollPane jspVariables = new JScrollPane(mpanelVariables);
		JScrollPane jspThumbnails = new JScrollPane(mpanelThumbnails);
		jspThumbnails.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		jspThumbnails.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		mjtpPlotDefinition.removeAll();
		mjtpPlotDefinition.addTab(" Variables", jspVariables);
		mjtpPlotDefinition.addTab(" Options", mpanelOptions);
		mjtpPlotDefinition.addTab(" Text", mpanelText);
		mjtpPlotDefinition.addTab(" Scale", mpanelScale);
//		mjtpPlotDefinition.addTab(" Axes", mpanelAxes);
		mjtpPlotDefinition.addTab(" Colors", mpanelColors);
//		mjtpPlotDefinition.addTab(" Geo",  mpanelGeo);      // todo add in future version
		mjtpPlotDefinition.addTab(" Preview", mPreviewPane);
		mjtpPlotDefinition.addTab(" Thumbnails", jspThumbnails);

		mjtpPlotDefinition.addChangeListener(
			new ChangeListener(){
			    public void stateChanged( ChangeEvent e ){
					if( mjtpPlotDefinition.getSelectedIndex() == 6){
						Panel_View_Plot._getTN_Controls().setVisible(true);
					} else {
						Panel_View_Plot._getTN_Controls().setVisible(false);
					}
				}
			}
		);

		this.setLayout(new BorderLayout());
		this.add(mjtpPlotDefinition, BorderLayout.CENTER);

		return true;
	}

	public void _vActivateVariableSelector(){
		mjtpPlotDefinition.setSelectedIndex(PANEL_INDEX_Variables);
		Panel_View_Plot._getTN_Controls().setVisible(false);
	}

	public void _vActivatePreview(){
		mjtpPlotDefinition.setSelectedIndex(PANEL_INDEX_Preview);
		Panel_View_Plot._getTN_Controls().setVisible(false);
		mjtpPlotDefinition.invalidate();
		this.validate();
	}

	public void _vActivateThumbnails(){
		mjtpPlotDefinition.setSelectedIndex(PANEL_INDEX_Thumbnails);
		Panel_View_Plot._getTN_Controls().setVisible(true);
		mjtpPlotDefinition.invalidate();
		this.validate();
	}

	void _vClear(){
		setModel( null, PlotEnvironment.getDefaultPlotType() );
	}

	void _setPlotType( PlotEnvironment.PLOT_TYPE ePlotType ){
		Model_Dataset modelActive = Model.get().getPlotDataModel();
		if( modelActive != null ){
			modelActive.getPlotEnvironment().setPlotType( ePlotType );
			mpanelVariables.vShowDDDSForm( ePlotType, modelActive );
		}
	}

	boolean zSetting = false;
	void setModel( Model_Dataset model, PlotEnvironment.PLOT_TYPE ePlotType ){
		if( zSetting ){ // reentrant
			System.err.println( "internal error, setData in Panel_Definition is re-entrant" );
			Thread.dumpStack();
			return;
		}
		zSetting = true;
		try {
			if( model == null ){
				mParent._setPlottingEnabled( false );
				mpanelScale._setScale( null );
				mpanelText.setPlotText( null );
				mpanelOptions.setPlotOptions( null );
				mpanelVariables.vClear();
			} else {
				Model.get().setPlotDataModel( model );
				PlotEnvironment pd = model.getPlotEnvironment();
				mpanelOptions.setPlotOptions( pd.getOptions()); // must be done before cs is set
				mpanelScale._setScale( pd.getScale());
				mpanelText.setPlotText( pd.getText());
				mParent._setPlottingEnabled( true );
				pd.setPlotType( ePlotType );
				mpanelVariables.vShowDDDSForm( ePlotType, model );
				vRefresh();
			}
		} finally {
			zSetting = false;
		}
	}

//	void vUpdateVariableSelector_FromDDS( DodsURL urlEntry, int ePlotType ){
//
// todo - is this necessary?
//		int ePlotType = mParent.getPlotType();
//		if( mActivePlottingDefinition != null ) mActivePlottingDefinition.setPlotType(ePlotType);
//				int xURL_0 = this.jlistSelectedURLs.getSelectedIndex();
//				if( xURL_0 < 0 ){
//					mpanelVariables.vShowMessage("Nothing selected from dataset list.");
//				} else {
//					DodsURL urlEntry = (DodsURL) listPlotterData.get(xURL_0);
//				}
//	}

	private void vRefresh(){
		invalidate();
		repaint();
	}

	// thumbnail support
	void vTN_SelectAll(){ mpanelThumbnails.vSelectAll(); }
	void vTN_DeleteSelected(){ mpanelThumbnails.vDeleteSelected(); }
	void vTN_ReRetrieve(){ mpanelThumbnails.vReRetrieve(); }
	public Model_Dataset[] getSelectedThumbs(){ return mpanelThumbnails.getSelectedURLs0(); }

}

class Panel_VariableTab extends JPanel {
	final static long serialVersionUID = 1;
	private Panel_Definition mParent;
//	private Plot_Definition mPlottingDefinition_active = null;
//	private PlottingDefinition_DataDDS mDefinition_DataDDS;
//	private PlottingDefinition_Table mDefinition_Table;
	private Panel_Variables mpanelVariableSelector;
	private JPanel mpanelMessage;
	private JLabel mlabelMessagePanelText;
	Panel_VariableTab(){}
	boolean zInitialize( Panel_Definition parent, StringBuffer sbError ){
		mParent = parent;
		setLayout(new BorderLayout());

		// message panel
		mpanelMessage = new JPanel();
		mpanelMessage.setBackground(new Color(0xFFF8DC));
		mpanelMessage.setLayout(new BorderLayout());
		mlabelMessagePanelText = new JLabel();
		mpanelMessage.add(new JLabel("  "), BorderLayout.WEST);
		mpanelMessage.add(mlabelMessagePanelText, BorderLayout.CENTER);

		// variable definition panel
		mpanelVariableSelector = new Panel_Variables();
		if( ! mpanelVariableSelector.zInitialize( sbError ) ){
			sbError.insert( 0, "failed to initialize definition panel: " );
			return false;
		}

		return true;
	}
	PlottingData getPlottingData( StringBuffer sbError ){
		VariableDataset variable_dataset = mpanelVariableSelector.getDataset( sbError );
		if( variable_dataset == null ) return null;
		return getNormalizedPlottingData( variable_dataset, sbError );
	}

	void vClear(){
		Model.get().setPlotDataModel( null );
		removeAll();
		revalidate();
	}
	void vShowMessage( String sMessage ){
		Model.get().setPlotDataModel( null );
		removeAll();
		mlabelMessagePanelText.setText(sMessage);
		add(mpanelMessage, BorderLayout.CENTER);
		revalidate();
	}
	void vShowDDDSForm( PlotEnvironment.PLOT_TYPE ePlotType, Model_Dataset model ){
		if( model == null ){
			removeAll();  // TODO
			return;
		}
		DataDDS ddds = model.getData();
		DAS das = model.getDAS();
		StringBuffer sbError = new StringBuffer(80);
		try {
			if( mpanelVariableSelector.zShowDataDDS( ePlotType, ddds, das, mParent.getPanel_PlotOptions().getPlotOptions(), sbError) ){
				removeAll();
				add(mpanelVariableSelector, BorderLayout.CENTER);
				mParent._vActivateVariableSelector();
			} else if( sbError.length() == 0 ){
				vShowMessage("Data set has no valid variables for this plot type");
			} else {
				ApplicationController.vShowError("Failed to set data definition panel: " + sbError);
				vShowMessage("Failed to set data definition panel (see errors)");
			}
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError("Error showing data DDS form: " + sbError);
			vShowMessage("Error showing data DDS form: " + sbError);
		}
		revalidate();
	}

	// note that when we do the data transformations here the tranform is done
	// on a clone of the original data, not on the data itself
	private PlottingData getNormalizedPlottingData( VariableDataset dataset, StringBuffer sbError ){
		try {
			if( dataset == null ){
				sbError.insert(0, "internal error, no dataset provided to normalize");
				return null;
			}
			int ctSlices = dataset.getValueCount();
			if( ctSlices < 1 ){
				sbError.append("no selected variables");
				return null;
			}

			// initialize plotting data
			PlottingData pdat = new PlottingData();
			int eDataType_Var1 = dataset.getDataType_Var1();
			int eDataType_Var2 = dataset.getDataType_Var2();
			int ctValues = dataset.getValueCount();
		    if( ctValues == 0 ){
				sbError.append("no value variables found.");
    			return null;
			}
			int ctDimensions = dataset.getDimCount();
			int eInversion   = dataset.getInversion();
			int iAXES_TYPE = PlottingData.AXES_Linear; // None, Linear, Mapped
			pdat.vInitialize( ctDimensions, iAXES_TYPE );

			// add slices to plotting data package
			for( int xValue = 1; xValue <= ctValues; xValue++ ){

				int[] aiDimLength1 = null;

				// build var 1
				VariableInfo var1        = dataset.getValue(xValue);
				Object[] eggData1        = null;
				Object[] eggMissing_var1 = dataset.getMissingEgg_Var1();
				String sDataCaption1     = null;
				String sSliceCaption1    = null;
				String sVarUnits1        = null;
				if( var1 != null ){
					eggData1 = var1.getValueEgg().clone();
					eggMissing_var1 = dataset.getMissingEgg_Var1();
					String sVarName1 = var1.getName();
					String sVarLongName1 = var1.getLongName();
					String sVarUserCaption1 = var1.getUserCaption();
		    		sDataCaption1 = sVarUserCaption1 != null ? sVarUserCaption1 : sVarLongName1 != null ? sVarLongName1 :  sVarName1;
	    			sVarUnits1 = var1.getUnits();
					sSliceCaption1 = var1.getSliceCaption();
					aiDimLength1 = var1.getDimLengths1();
				    if( !DAP.zTransform(eggData1, eDataType_Var1, aiDimLength1, dataset.getReversed_x(), dataset.getReversed_y(), sbError) ){
					    sbError.insert(0, "transformation for var1 failed: ");
					    return null;
				    }
				}

				// build var 2
				VariableInfo var2        = dataset.getValue2(xValue);
				Object[] eggData2        = null;
				Object[] eggMissing_var2 = null;
				String sDataCaption2     = null;
				String sSliceCaption2    = null;
				String sVarUnits2        = null;
				if( var2 != null ){
					if( var1 == null ) aiDimLength1 = var2.getDimLengths1();
					eggData2 = var2.getValueEgg();
					eggMissing_var2 = dataset.getMissingEgg_Var2();
					String sVarName2 = var2.getName();
					String sVarLongName2 = var2.getLongName();
					String sVarUserCaption2 = var2.getUserCaption();
		    		sDataCaption2 = sVarUserCaption2 != null ? sVarUserCaption2 : sVarLongName2 != null ? sVarLongName2 :  sVarName2;
	    			sVarUnits2 = var2.getUnits();
			    	sSliceCaption2 = var2.getSliceCaption();
					if( !DAP.zTransform(eggData2, eDataType_Var2, aiDimLength1, dataset.getReversed_x(), dataset.getReversed_y(), sbError) ){
						sbError.insert(0, "transformation for var2 failed: ");
						return null;
					}
				}

				// add variable pair
				if( !pdat.zAddVariable( eggData1, eggData2, eDataType_Var1, eDataType_Var2, aiDimLength1, eggMissing_var1, eggMissing_var2, sDataCaption1, sDataCaption2, sVarUnits1, sVarUnits2, sSliceCaption1, sSliceCaption2, sbError) ){
					sbError.insert(0, "Error adding slice " + xValue + ": ");
					return null;
				}
			}

			// transform and set axes
			if( dataset.getAxisX_Indexed() ){ // generate a variable info for the indexed axis
				VariableInfo varAxis_X = new VariableInfo(null, dataset.getReversed_x(), "[x-index]", null, null, null, null);
				pdat.setAxis_X(varAxis_X, true);
			} else {
				VariableInfo varAxis_X = dataset.getAxisX();
				if( varAxis_X != null && dataset.getReversed_x() ){
					if( !DAP.zReverse(varAxis_X.getValueEgg(), varAxis_X.getDataType(), sbError) ){
						sbError.insert(0, "Error reversing X-axis: ");
						return null;
					}
				}
				pdat.setAxis_X(varAxis_X, false);
			}
			if( dataset.getAxisY_Indexed() ){
				VariableInfo varAxis_Y = new VariableInfo(null, dataset.getReversed_y(), "[y-index]", null, null, null, null);
				pdat.setAxis_Y(varAxis_Y, true);
			} else {
				VariableInfo varAxis_Y = dataset.getAxisY();
				if( varAxis_Y != null && dataset.getReversed_y() ){
					if( !DAP.zReverse(varAxis_Y.getValueEgg(), varAxis_Y.getDataType(), sbError) ){
						sbError.insert(0, "Error reversing Y-axis: ");
						return null;
					}
				} else {
				}
				pdat.setAxis_Y(varAxis_Y, false);
			}

			return pdat;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return null;
		}
	}

}

