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

/**
 * Title:        Panel_View_Plot
 * Description:  Manages the plotting interface
 * Copyright:    Copyright (c) 2003-13
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.09
 */

import opendap.dap.*;
import opendap.clients.odc.*;
import opendap.clients.odc.data.Model_LoadedDatasets;
import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.gui.Resources;
import opendap.clients.odc.gui.Styles;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

public class Panel_View_Plot extends JPanel implements IControlPanel {

	private static Panel_View_Plot thisInstance;

	private Panel_Definition mDefinitionPanel;

	public final static int SOURCE_Table = 1;
	public final static int SOURCE_SelectedURL = 2;
	JPanel mjpanelDatasets;
	JRadioButton jrbFromTable;
	JRadioButton jrbFromSelectedURL;
	final JList jlistSelectedURLs = new JList();
	final JList jlistPlottableExpressions = new JList();
	final static DataParameters mDataParameters = new DataParameters();

	final static String[] asOutputOptions = {"Preview Pane", "External Window", "New Window", "Full Screen", "Print", "File (PNG)", "Thumbnails"};

	final private JButton buttonPlot = new JButton("Plot to");
	final private JButton buttonSelectAll = new JButton("Select All");
	final private JButton buttonInfo = new JButton("Info...");
	final private JPanel panelZoom = new JPanel();
	final private JComboBox jcbOutputOptions = new JComboBox(asOutputOptions);
	final private JPanel panelTN_Controls = new JPanel();

	// expression panel
	JPanel mjpanelExpressions;
	final private JButton buttonEvaluate = new JButton("Evaluate");
	final private JButton buttonFileSaveExpression = new JButton("Save...");
	final private JButton buttonFileLoadExpression = new JButton("Load...");

	PlotEnvironment.PLOT_TYPE mePlotType = PlotEnvironment.PLOT_TYPE.Pseudocolor; // default

	JFileChooser jfc = null;

	public Panel_View_Plot() {
		thisInstance = this;
	}

	public void _vSetFocus(){
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				mDefinitionPanel.requestFocus();
			}
		});
	}

	public final static Panel_View_Plot _getInstance(){ return thisInstance; }

	public static JPanel _getTN_Controls(){
		return thisInstance.panelTN_Controls;
	}

	public static PreviewPane _getPreviewScrollPane(){
		return thisInstance.mDefinitionPanel.getPreviewScrollPane();
	}

	public static Panel_VariableTab _getPanel_VariableTab(){
		return thisInstance.mDefinitionPanel.getPanel_Variables();
	}

	public static Panel_PlotScale _getPanel_PlotScale(){
		return thisInstance.mDefinitionPanel.getPanel_PlotScale();
	}

	public static Panel_PlotAxes _getPanel_PlotAxes(){
		if( Model.get().getPlotDataModel() == null ){
		    return null;
		} else {
			return thisInstance.mDefinitionPanel.getPanel_PlotAxes();
		}
	}

	public static Panel_Thumbnails _getPanel_Thumbnails(){
		if( Model.get().getPlotDataModel() == null ){
		    return null;
		} else {
			return thisInstance.mDefinitionPanel.getPanel_Thumbnails();
		}
	}

	public static Panel_ColorSpecification _getPanel_ColorSpecification(){
		if( Model.get().getPlotDataModel()== null ){
		    return null;
		} else {
			return thisInstance.mDefinitionPanel.getPanel_ColorSpecification();
		}
	}

	public static Panel_Definition _getPanel_Definition(){
		return thisInstance.mDefinitionPanel;
	}

	public static DataParameters _getDataParameters(){ return mDataParameters; }

	public static void _vTestLayout(){
		opendap.clients.odc.plot.Test_PlotLayout tpl = new opendap.clients.odc.plot.Test_PlotLayout();
		tpl.setVisible(true);
	}

	public boolean _zInitialize( StringBuffer sbError ){
		try {
			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			this.setBorder(borderStandard);

			ApplicationController.getInstance().vShowStartupMessage("creating definition panel");
			mDefinitionPanel = new Panel_Definition();
			if( !mDefinitionPanel.zInitialize( this, sbError ) ){
				sbError.insert(0, "failed to initialize definition panel");
				return false;
			}

			// setup plot type
			String[] asDisplayList = PlotEnvironment.getPlotTypeDisplayList();
			final JComboBox jcbPlotType = new JComboBox( asDisplayList );
			jcbPlotType.setSelectedIndex(0); // default to first item
			Border borderInset = javax.swing.BorderFactory.createLoweredBevelBorder();
			jcbPlotType.setBorder( borderInset );
			jcbPlotType.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						int index = jcbPlotType.getSelectedIndex();
						if( index < 0 ) index = 0;
						setPlotType( PlotEnvironment.PLOT_TYPE.values()[index] );
						Panel_View_Plot._getPanel_Definition()._vActivateVariableSelector();
					}
				}
			);

			jcbOutputOptions.setSelectedIndex(0);

			// Create command panel
			ApplicationController.getInstance().vShowStartupMessage("creating plotter panels");
			JPanel panelCommand = new JPanel();
			panelCommand.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			Styles.vApply(Styles.STYLE_BigBlueButton, buttonPlot);
			buttonPlot.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						_zPlot();
					}
				}
			);

			// create show datasets check box
			final JCheckBox jcheckShowDatasets = new JCheckBox(" Show Datasets");
			jcheckShowDatasets.setSelected(true);
			jcheckShowDatasets.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						_vShowDatasets( jcheckShowDatasets.isSelected() );
					}
				}
			);

			// create freeze definition check box
			final JCheckBox jcheckFreezeDefinition = new JCheckBox(" Freeze Definition");
			jcheckFreezeDefinition.setSelected(false);
			jcheckFreezeDefinition.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						_vFreezeDefinition( jcheckFreezeDefinition.isSelected() );
					}
				}
			);

			final JPanel panelChecks = new JPanel();
			panelChecks.setLayout(new BoxLayout(panelChecks, BoxLayout.Y_AXIS));
			panelChecks.add( jcheckShowDatasets );
			panelChecks.add( jcheckFreezeDefinition );

			// create zoom control
			ApplicationController.getInstance().vShowStartupMessage("creating plotter zoom control");
			panelZoom.setLayout(new BoxLayout(panelZoom, BoxLayout.X_AXIS));
			final JButton buttonZoom_In = new JButton("+");
			final JButton buttonZoom_Out = new JButton("-");
			final JButton buttonZoom_Maximize = new JButton("MAX");
			panelZoom.add(Box.createHorizontalStrut(10));
			panelZoom.add(new JLabel("Zoom:"));
			panelZoom.add(Box.createHorizontalStrut(6));
			panelZoom.add(buttonZoom_In);
			panelZoom.add(Box.createHorizontalStrut(4));
			panelZoom.add(buttonZoom_Out);
			panelZoom.add(Box.createHorizontalStrut(4));
			panelZoom.add(buttonZoom_Maximize);
			panelZoom.add(Box.createHorizontalStrut(10));
			Border borderEtched = BorderFactory.createEtchedBorder();
			panelZoom.setBorder(borderEtched);

			// set up zoom actions
			ActionListener listenerZoom = new ActionListener(){
	    		public void actionPerformed(ActionEvent event) {
					PlotEnvironment pd = Model.get().getPlotDataModel().getPlotEnvironment();
					if( pd != null ){
						PlotScale scale = pd.getScale();
						if( scale != null ){
							Object oSource = event.getSource();
							if( oSource == buttonZoom_In ){
								if( scale.zZoomIn() ) _zPlot();
							}
							if( oSource == buttonZoom_Out ){
								if( scale.zZoomOut() ) _zPlot();
							}
							if( oSource == buttonZoom_Maximize ){
								if( scale.zZoomMaximize() ) _zPlot();
							}
						}
					}
			    }
			};
			buttonZoom_In.addActionListener(listenerZoom);
			buttonZoom_Out.addActionListener(listenerZoom);
			buttonZoom_Maximize.addActionListener(listenerZoom);

			// thumbnail control
			ApplicationController.getInstance().vShowStartupMessage("creating thumbnail panels");
			panelTN_Controls.setLayout(new BoxLayout(panelTN_Controls, BoxLayout.X_AXIS));
			final JButton buttonTN_SelectAll = new JButton("Select All");
			final JButton buttonTN_DeleteSelected = new JButton("Delete Selected");
			final JButton buttonTN_ReRetrieve = new JButton("Re-Retrieve");
			panelTN_Controls.add(buttonTN_SelectAll);
			panelTN_Controls.add(Box.createHorizontalStrut(2));
			panelTN_Controls.add(buttonTN_DeleteSelected);
			panelTN_Controls.add(Box.createHorizontalStrut(2));
			panelTN_Controls.add(buttonTN_ReRetrieve);
			panelTN_Controls.add(Box.createHorizontalStrut(2));
			panelTN_Controls.setVisible(false);

			// set up TN actions
			ActionListener listenerTN = new ActionListener(){
	    		public void actionPerformed(ActionEvent event) {
					Object oSource = event.getSource();
					if( oSource == buttonTN_SelectAll ) Panel_View_Plot._getInstance().mDefinitionPanel.vTN_SelectAll();
					if( oSource == buttonTN_DeleteSelected ) Panel_View_Plot._getInstance().mDefinitionPanel.vTN_DeleteSelected();
					if( oSource == buttonTN_ReRetrieve ) Panel_View_Plot._getInstance().mDefinitionPanel.vTN_ReRetrieve();
			    }
			};
			buttonTN_SelectAll.addActionListener(listenerTN);
			buttonTN_DeleteSelected.addActionListener(listenerTN);
			buttonTN_ReRetrieve.addActionListener(listenerTN);

			// layout command panel
			// Plot to: [target]  Plot Type: [plot type]  X Show Datasets
			panelCommand.setLayout(new BoxLayout(panelCommand, BoxLayout.X_AXIS));
			panelCommand.add(buttonPlot);
			panelCommand.add(Box.createHorizontalStrut(2));
			panelCommand.add(jcbOutputOptions);
			panelCommand.add(Box.createHorizontalStrut(4));
			panelCommand.add(new JLabel("Plot Type:"));
			panelCommand.add(Box.createHorizontalStrut(2));
			panelCommand.add(jcbPlotType);
			panelCommand.add(Box.createHorizontalStrut(4));
			panelCommand.add(jcheckShowDatasets);
			panelCommand.add(Box.createHorizontalStrut(12));
			panelCommand.add(panelTN_Controls);
			panelCommand.add(Box.createHorizontalStrut(12));
			panelCommand.add(panelZoom);
			panelCommand.add(Box.createHorizontalGlue());

			// Combine command buttons with options
			JPanel panelCommandOptions = new JPanel();
			BoxLayout boxlayoutCommandOptions = new BoxLayout(panelCommandOptions, BoxLayout.Y_AXIS);
			panelCommandOptions.setLayout(boxlayoutCommandOptions);
			panelCommandOptions.add(panelCommand);

			_vSetupSourceSelector();

			if( mjpanelDatasets == null ){ sbError.append("dataset panel does not exist"); return false; }
			if( mDefinitionPanel == null ){ sbError.append("definition panel does not exist"); return false; }

			JPanel jpanelNorth = new JPanel();
			jpanelNorth.setLayout(new BoxLayout(jpanelNorth, BoxLayout.Y_AXIS));
			jpanelNorth.add(mjpanelDatasets);
			jpanelNorth.add(panelCommand);

			this.setLayout(new BorderLayout());
			this.add(jpanelNorth, BorderLayout.NORTH);
			this.add(mDefinitionPanel, BorderLayout.CENTER);
			// this.add(jpanelInstructions, BorderLayout.SOUTH); // leave out the instructions for now

			mDefinitionPanel._vClear();

			setPlotType( PlotEnvironment.PLOT_TYPE.Pseudocolor );

            return true;

        } catch(Exception ex){
			ApplicationController.vUnexpectedError(ex, sbError);
            return false;
        }
	}

	private boolean mzFreezeDefinition = false;
	void _vFreezeDefinition( boolean z ){
		mzFreezeDefinition = z;
	}

	void _vPlot( Model_Dataset urlToPlot ){
		Renderer.OutputTarget eOutputOption = _getOutputOption();
		_vPlot( urlToPlot, eOutputOption );
	}

	void _vPlot( Model_Dataset urlToPlot, Renderer.OutputTarget eTarget ){
		Model_LoadedDatasets modelLoadedDatasets = Model.get().getDatasets();
		for( int xListItem = 0; xListItem < modelLoadedDatasets.getSize(); xListItem++ ){
			Model_Dataset urlCurrent = (Model_Dataset)modelLoadedDatasets.getElementAt(xListItem);
			if( urlCurrent == urlToPlot ){
				vActivateListItem(xListItem);
				_zPlot( eTarget );
				return;
			}
		}
		ApplicationController.vShowWarning("URL not found in dataset list: " + urlToPlot);
	}

	public static Renderer.OutputTarget _getOutputOption(){
		if( thisInstance == null ){
			return Renderer.OutputTarget.NewWindow;
		} else {
			return (Renderer.OutputTarget)thisInstance.jcbOutputOptions.getSelectedItem();
		}
	}

	boolean _zPlot(){
		Renderer.OutputTarget eOutputOption = _getOutputOption();
		return _zPlot( eOutputOption );
	}

	public boolean _zPlotExpressionToPreview( final Model_Dataset model, StringBuffer sbError ){
		try {
			final PlotEnvironment environment = model.getPlotEnvironment();
			if( environment == null ){
				sbError.append( "internal error, no plotting definition" );
				return false;
			}
			final Activity activitySinglePlot = new Activity();
			Continuation_DoCancel con = new Continuation_DoCancel(){
				public void Do(){
					StringBuffer sbError_plot = new StringBuffer();
					
					// gather parameters
					String sCaption = model.getTitle();
					Renderer.OutputTarget eOutputOption = Renderer.OutputTarget.ExpressionPreview;
					PlotScale ps = environment.getScale();
					ps.setOutputTarget( eOutputOption );
					PlotLayout layout = PlotLayout.create( PlotLayout.LayoutStyle.PlotArea );

					// plot
					if( Renderer.zPlot_Expression( environment, layout, model, eOutputOption, sCaption, sbError_plot ) ){
						Panel_View_Plot._getInstance().mDefinitionPanel._vActivatePreview();
					} else {
						ApplicationController.vShowError( "Error plotting expression: " + sbError_plot );
					}
				}
				public void Cancel(){}
			};
			activitySinglePlot.vDoActivity( null, null, con, null );
	   } catch(Throwable t) {
		   ApplicationController.vUnexpectedError( t, sbError );
		   return false;
	   }
	   return true;
	}
	
	boolean _zPlot( final Renderer.OutputTarget eTarget ){ // on the event loop
		try {

			if( eTarget == Renderer.OutputTarget.Thumbnail && Panel_View_Plot.getPlotType() == PlotEnvironment.PLOT_TYPE.Vector ){
				ApplicationController.vShowError("Currently thumbnails cannot be generated for vector plots.");
				return false;
			}

			final StringBuffer sbError = new StringBuffer(80);
			final int[] aiSelected = jlistSelectedURLs.getSelectedIndices();
			final boolean zMultiplot = ( jrbFromSelectedURL.isSelected() && aiSelected.length > 1 );
			try {
				final PlotEnvironment environment = Model.get().getPlotDataModel().getPlotEnvironment();
				if( environment == null ){
					ApplicationController.vShowWarning("null definition encountered during multi-plot");
					return false;
				}
				if( zMultiplot ){
					if( eTarget == Renderer.OutputTarget.PreviewPane ) Panel_View_Plot._getInstance().mDefinitionPanel._vActivatePreview();
					if( eTarget == Renderer.OutputTarget.Thumbnail ) Panel_View_Plot._getInstance().mDefinitionPanel._vActivateThumbnails();
					vActivateListItem(aiSelected[0]);
					PlotOptions po = environment.getOptions();
					final int miMultiplotDelay = po.getValue_int(PlotOptions.OPTION_MultiplotDelay);
					final Activity activityMultiplot = new Activity();
					Continuation_DoCancel con = new Continuation_DoCancel(){
						public void Do(){
							int ctSelections = aiSelected.length;
							long nLastPlot = System.currentTimeMillis();
							for( int xSelection = 0; xSelection < ctSelections; xSelection++ ){
								vActivateListItem(aiSelected[xSelection]);
								Thread.yield(); // allow swing events to occur after activation
								Model_Dataset model = Model.get().getPlotDataModel();
								final PlottingData pdat = Panel_View_Plot._getPanel_VariableTab().getPlottingData( sbError );
								if( pdat == null ){
									ApplicationController.vShowError_NoModal("Error plotting selection " + (xSelection+1) + " of " + ctSelections + ": " + sbError);
									sbError.setLength(0);
									continue;
								}
								// ??? if( Renderer.zPlot( environment, model, pdat, eTarget, sbError ) ){
								if( Renderer.zGenerateComposition( null, environment, model, eTarget, sbError ) ){ // composition is missing
									try {
										Thread.sleep(100); // Thread.yield(); // allow screen updates to occur between plots
									} catch(Exception ex) {}
								} else {
									ApplicationController.vShowError("Plotting error: " + sbError);
								}
								if( eTarget != Renderer.OutputTarget.Thumbnail ){
									try {
										int iTimeInView = (int)(System.currentTimeMillis() - nLastPlot);
										int iTimeToSleep = miMultiplotDelay - iTimeInView;
										if( iTimeToSleep > 0 ) Thread.sleep(iTimeToSleep);
									} catch(Exception ex) {}
									nLastPlot = System.currentTimeMillis();
								}
								ApplicationController.vShowStatus_NoCache("plotted " + (xSelection + 1) + " of " + ctSelections);
							}
						}
						public void Cancel(){}
					};
					activityMultiplot.vDoActivity(null, null, con, null);
				} else { // if there is only one selection then this happens, must still plot in thread because multislice can do screen updates
					final PlottingData pdat = Panel_View_Plot._getPanel_VariableTab().getPlottingData(sbError);
					if( pdat == null ){
						ApplicationController.vShowError("Invalid data selection or error acquiring data: " + sbError);
						return false;
					} else {
						final Activity activitySinglePlot = new Activity();
						Continuation_DoCancel con = new Continuation_DoCancel(){
							public void Do(){
								Model_Dataset model = Model.get().getPlotDataModel();
// ???								if( Renderer.zPlot( environment, model, pdat, eTarget, sbError ) ){
								if( Renderer.zGenerateComposition( null, environment, model, eTarget, sbError ) ){ // composition is missing
									if( eTarget == Renderer.OutputTarget.PreviewPane ) Panel_View_Plot._getInstance().mDefinitionPanel._vActivatePreview();
									if( eTarget == Renderer.OutputTarget.Thumbnail ) Panel_View_Plot._getInstance().mDefinitionPanel._vActivateThumbnails();
								} else {
									ApplicationController.vShowError("Plotting error: " + sbError);
								}
							}
							public void Cancel(){}
						};
						activitySinglePlot.vDoActivity(null, null, con, null);
					}
				}
		   } catch(Throwable t) {
			   ApplicationController.vUnexpectedError(t, "While plotting");
		   }
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, "Error plotting: ");
		}
		return true;
	}

	void _vShowDatasets( boolean z ){
		this.mjpanelDatasets.setVisible(z);
	}

	void _setPlottingEnabled( boolean z ){
		this.buttonPlot.setEnabled(z);
	}

	void _vSetupSourceSelector(){

		jrbFromTable = new JRadioButton("Table View");
		jrbFromTable.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					Panel_View_Plot.this.setPlotType( Panel_View_Plot.getPlotType() );
				}
			}
		);

		jrbFromSelectedURL = new JRadioButton("Selected URL:");
		jrbFromSelectedURL.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					Panel_View_Plot.this.setPlotType( Panel_View_Plot.getPlotType() );
				}
			}
		);
		final JPanel panelSourceType = new JPanel();
		panelSourceType.setLayout(new BoxLayout(panelSourceType, BoxLayout.X_AXIS));
		panelSourceType.add(Box.createHorizontalStrut(6));
		panelSourceType.add(jrbFromTable);
		panelSourceType.add(Box.createHorizontalStrut(3));
		panelSourceType.add(jrbFromSelectedURL);
		panelSourceType.add(Box.createHorizontalGlue());
		panelSourceType.setVisible(false); // todo this control is targeted for elimination

		// buttons
		StringBuffer sbError = new StringBuffer();
		buttonSelectAll.addActionListener(
			new ActionListener(){
	    		public void actionPerformed(ActionEvent event) {
					try {
						jlistSelectedURLs.setSelectionInterval(0, jlistSelectedURLs.getModel().getSize() - 1);
					} catch(Exception ex) {
						ApplicationController.vUnexpectedError(ex, "while selecting all plotting datasets");
					}
			    }
			}
		);
		buttonInfo.addActionListener(
			new ActionListener(){
	    		public void actionPerformed(ActionEvent event) {
					try {
						int xURL_0 = jlistSelectedURLs.getSelectedIndex();
						if( xURL_0 < 0 ){
							ApplicationController.vShowWarning("nothing selected");
							return;
						}
						Panel_View_Plot.this.source_Info(xURL_0);
						return;
					} catch(Exception ex) {
						StringBuffer sbError = new StringBuffer(80);
						ApplicationController.vUnexpectedError(ex, sbError);
						ApplicationController.vShowError("Unexpected error gettomg item info: " + sbError);
					}
			    }
			}
		);

		// plottable data list
		jlistSelectedURLs.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		jlistSelectedURLs.setFont(Styles.fontFixed12);
		JScrollPane jspSelectedURLs = new JScrollPane(jlistSelectedURLs);
		jspSelectedURLs.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); // otherwise panel resizes when long url added
		jlistSelectedURLs.addListSelectionListener(
		    new ListSelectionListener(){
			    public void valueChanged(ListSelectionEvent lse) {
					if( lse.getValueIsAdjusting() ){ // this event gets fired multiple times on a mouse click--but the value "adjusts" only once
						int xURL_0 = jlistSelectedURLs.getSelectedIndex();
						if( xURL_0 < 0 ) return;
						vSetDataToDefinition(xURL_0);
					}
				}
			}
		);

		// plottable expression list
		jlistPlottableExpressions.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		jlistPlottableExpressions.setFont(Styles.fontFixed12);
		JScrollPane jspPlottableExpressions = new JScrollPane(jlistPlottableExpressions);
		jspSelectedURLs.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); // otherwise panel resizes when long url added
		jlistPlottableExpressions.addListSelectionListener(
		    new ListSelectionListener(){
			    public void valueChanged(ListSelectionEvent lse) {
					if( lse.getValueIsAdjusting() ){ // this event gets fired multiple times on a mouse click--but the value "adjusts" only once
						int xURL_0 = jlistPlottableExpressions.getSelectedIndex();
						if( xURL_0 < 0 ) return;
						vSetDataToExpression(xURL_0);
					}
				}
			}
		);

		javax.swing.ButtonGroup bgSelectorOptions = new ButtonGroup();
		bgSelectorOptions.add( jrbFromTable );
		bgSelectorOptions.add( jrbFromSelectedURL );
		jlistSelectedURLs.setModel( Model.get().getDatasets() );

		// dataset panel
		mjpanelDatasets = new JPanel();
		mjpanelDatasets.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1.0; gbc.weighty = 0;
		gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 6; gbc.gridheight = 1;
		mjpanelDatasets.add( panelSourceType, gbc );   // source type
		gbc.gridx = 0; gbc.gridy = 1;
		gbc.weighty = 1;
		mjpanelDatasets.add( jspSelectedURLs, gbc );
		gbc.weighty = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.gridx = 0;
		mjpanelDatasets.add( buttonSelectAll, gbc );
		gbc.gridx = 1;
		mjpanelDatasets.add( buttonInfo, gbc );
		gbc.gridx = 2;
		mjpanelDatasets.add( Box.createHorizontalGlue(), gbc );
		gbc.gridx = 0; gbc.gridy = 3;
		mjpanelDatasets.add( Box.createVerticalStrut(4), gbc );

		// expression panel
		mjpanelExpressions = new JPanel();
		mjpanelDatasets.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.weightx = 1.0; gbc.weighty = 0;
		gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 5; gbc.gridheight = 1;
		gbc.gridx = 0; gbc.gridy = 1;
		gbc.weighty = 1;
		mjpanelExpressions.add( jspPlottableExpressions, gbc );
		gbc.weighty = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.weightx = 0;
		gbc.gridx = 0;
		mjpanelExpressions.add( buttonEvaluate, gbc );
		gbc.gridx = 1;
		mjpanelExpressions.add( buttonFileSaveExpression, gbc );
		gbc.gridx = 2;
		mjpanelExpressions.add( buttonFileLoadExpression, gbc );
		gbc.gridx = 3;
		mjpanelExpressions.add( Box.createHorizontalGlue(), gbc );
		gbc.gridx = 0; gbc.gridy = 3;
		mjpanelExpressions.add( Box.createVerticalStrut(4), gbc );

// not in use currently
//		JPanel jpanelInstructions = new JPanel();
//		JLabel jlabelInstructions = new JLabel("here are the instructions");
//		jpanelInstructions.add(jlabelInstructions);

		if( !Renderer.zInitialize( sbError ) ){
			ApplicationController.vShowError("Error initializing plotting handler: " + sbError);
		}

	}

	void vActivateLastListItem(){
		Model_LoadedDatasets modelLoadedDatasets = Model.get().getDatasets();
		int ctItems = modelLoadedDatasets.getSize();
		if( ctItems > 0 ){
			vActivateListItem(ctItems - 1);
		}
	}

	void vActivateListItem( int xItem_0 ){
		Model_LoadedDatasets modelLoadedDatasets = Model.get().getDatasets();
		int ctItems = modelLoadedDatasets.getSize();
		if( xItem_0 < 0 || xItem_0 >= ctItems ){
			buttonInfo.setEnabled(false);
		}
		buttonInfo.setEnabled(true);
		if( !jrbFromSelectedURL.isSelected() ) jrbFromSelectedURL.setSelected(true);
		jlistSelectedURLs.setSelectedIndex(xItem_0);
		vSetDataToDefinition(xItem_0);
	}

	void vSetDataToDefinition( int xDataset_0 ){
		Model_LoadedDatasets modelLoadedDatasets = Model.get().getDatasets();
		final Model_Dataset model = modelLoadedDatasets._get( xDataset_0 );		
		PlotEnvironment.PLOT_TYPE ePlotType = getPlotType();
		mDefinitionPanel.setModel( model, ePlotType );
		PlotEnvironment pd = model.getPlotEnvironment();
		if( pd == null ) return;
		pd.setColorSpecification( Panel_View_Plot._getPanel_ColorSpecification().getColorSpecification() );
	}

	void vSetDataToExpression( int xDataset_0 ){
		// TODO
		Model_LoadedDatasets modelLoadedDatasets = Model.get().getDatasets();
		final Model_Dataset url = modelLoadedDatasets._get( xDataset_0 );		
		PlotEnvironment.PLOT_TYPE ePlotType = getPlotType();
		mDefinitionPanel.setModel( url, ePlotType );
		PlotEnvironment pd = url.getPlotEnvironment();
		if( pd == null ) return;
		pd.setColorSpecification( Panel_View_Plot._getPanel_ColorSpecification().getColorSpecification() );
	}

	public static PlotEnvironment.PLOT_TYPE getPlotType(){
	    return Panel_View_Plot._getInstance().mePlotType;
	}

    void setPlotType( PlotEnvironment.PLOT_TYPE eTYPE ){
		if( eTYPE == mePlotType ) return; // no change
        mePlotType = eTYPE;
		mDefinitionPanel._setPlotType( eTYPE );
    }

	int getSelectedSourceType(){
		if( jrbFromTable.isSelected() ) return SOURCE_Table;
		return SOURCE_SelectedURL;
	}
	String getSourceURL(){
		if( getSelectedSourceType() != SOURCE_SelectedURL ) return null;
		int iSelection = jlistSelectedURLs.getSelectedIndex();
		if( iSelection < 0 ) return null;

		return null;
	}
	StringBuffer sbItem = new StringBuffer(80);
	void source_Info( int xURL_0 ){
		try {
			Model_LoadedDatasets modelLoadedDatasets = Model.get().getDatasets();
			if( xURL_0 >= modelLoadedDatasets.getSize() || xURL_0 < 0){
				ApplicationController.vShowError("system error; url index outside loaded range");
				return;
			}
			Model_Dataset urlEntry = modelLoadedDatasets._get(xURL_0);
			StringBuffer sbInfo = new StringBuffer(120);
			sbInfo.append(urlEntry.getTitle());
			sbInfo.append("\n");
			sbInfo.append(urlEntry);
			sbInfo.append("\n\n");
			sbInfo.append(urlEntry.getInfo());
			JFrame frame = new JFrame();
			Resources.iconAdd(frame);
			frame.setTitle("Dataset Info for " + urlEntry.getTitle());
			Dimension dimScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setSize((dimScreenSize.width/2), (dimScreenSize.height/2));
			JTextArea jta = new JTextArea();
			JScrollPane jsp = new JScrollPane(jta);
			jta.setText(sbInfo.toString());
			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(jsp, BorderLayout.CENTER);
			frame.setVisible(true);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					JFrame fr = (JFrame)event.getSource();
					fr.setVisible(false);
					fr.dispose();
				}
			});
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("Unexpected error getting info: ");
			ApplicationController.vUnexpectedError( ex, sbError );
			ApplicationController.vShowError(sbError.toString());
		}
	}
}

// The data parameters tell the system what kind of data is currently selected.
// Whenever the user changes the selected variable the current parameters (maintained
// statically in Panel_View_Plot) must be compared to the newly selected variable. If
// it does not match then new parameters must be calculated. This is done in the
// vUpdateInfo method of the variable selector (see VariableSelector_Plot_Panel)
class DataParameters {
	private String msVariableName;
	private int miD1_length;
	private int miD2_length;
	private int miD3_length;
	private boolean mzInverted;
	private int meType;
	private Object[] meggWorkingValues = new Object[1];
	private Object[] meggMissing;
	private String msMissingCode = "";
	short shDataFrom; short shDataTo;
	int iDataFrom; int iDataTo;
	long nDataFrom; long nDataTo;
	float fDataFrom; float fDataTo;
	double dDataFrom; double dDataTo;
	public DataParameters(){
		meType = 0; // no data selected
		meggMissing = new Object[1];
		meggMissing[0] = null;
	}

	// this is done because Java does not support unsigned bytes so all bytes
	// are converted to shorts and similarly for unsigned shorts/ints
	public static int getJavaType_for_DAPType( int eDAPType ){
		switch( eDAPType ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				return DAP.JAVA_TYPE_short;
			case DAP.DATA_TYPE_Int32:
			case DAP.DATA_TYPE_UInt16:
				return DAP.JAVA_TYPE_int;
			case DAP.DATA_TYPE_UInt32:
				return DAP.JAVA_TYPE_long;
			case DAP.DATA_TYPE_Float32:
				return DAP.JAVA_TYPE_float;
			case DAP.DATA_TYPE_Float64:
				return DAP.JAVA_TYPE_double;
			case DAP.DATA_TYPE_String:
				return DAP.JAVA_TYPE_string;
			default:
				return 0;
		}
	}

	boolean zCompare( BaseType btValues, int iDimX1, int iDimY1 ){
		boolean zInverted = iDimX1 < iDimY1; // todo xxx
		if( btValues == null ) return false;
		if( !( btValues instanceof DArray) ) return false;
		if( !btValues.getName().equals( msVariableName ) ) return false;
		DArray darray = (DArray)btValues;
		int eType = DAP.getDArrayType(darray);
		if( eType != meType ) return false;
		if( zInverted != mzInverted ){ mzInverted = zInverted; } // no need to recalculate entire thing; just update inversion
		try {
			switch( darray.numDimensions() ){
				case 0:
					return false;
				case 1:
					if( miD1_length != darray.getDimension(0).getSize() ) return false;
					break;
				case 2:
					if( miD1_length != darray.getDimension(0).getSize() ) return false;
					if( miD2_length != darray.getDimension(1).getSize() ) return false;
					break;
				case 3:
					if( miD1_length != darray.getDimension(0).getSize() ) return false;
					if( miD2_length != darray.getDimension(1).getSize() ) return false;
					if( miD3_length != darray.getDimension(2).getSize() ) return false;
					break;
			}
		} catch(Exception ex) {
			ApplicationController.vShowError("failed to ascertain size(s) of comparison array dimensions");
			return false;
		}
		return true;
	}

	public String toString(String sIndent){
		StringBuffer sb = new StringBuffer(250);
		sb.append(sIndent).append("Data Parameters:\n");
		sb.append(sIndent).append("type: " + DAP.getType_String(meType) + "\n");
		sb.append(sIndent).append("D1_length: " + miD1_length + "\n");
		sb.append(sIndent).append("D2_length: " + miD2_length + "\n");
		sb.append(sIndent).append("D3_length: " + miD3_length + "\n");
		sb.append(sIndent).append("missing: " + DAP.getValueString(meggMissing, meType) + "\n");
		sb.append(sIndent).append("missing_method_code: " + msMissingCode + "\n");
		switch( meType ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				sb.append(sIndent).append("data_from: " + shDataFrom + "\n");
				sb.append(sIndent).append("data_to: " + shDataTo + "\n");
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				sb.append(sIndent).append("data_from: " + iDataFrom + "\n");
				sb.append(sIndent).append("data_to: " + iDataTo + "\n");
				break;
			case DAP.DATA_TYPE_UInt32:
				sb.append(sIndent).append("data_from: " + nDataFrom + "\n");
				sb.append(sIndent).append("data_to: " + nDataTo + "\n");
				break;
			case DAP.DATA_TYPE_Float32:
				sb.append(sIndent).append("data_from: " + fDataFrom + "\n");
				sb.append(sIndent).append("data_to: " + fDataTo + "\n");
				break;
			case DAP.DATA_TYPE_Float64:
				sb.append(sIndent).append("data_from: " + dDataFrom + "\n");
				sb.append(sIndent).append("data_to: " + dDataTo + "\n");
				break;
		}
		return sb.toString();
	}

	// Variables in the plotter have a real type and a working type. The real
	// type is the type they are in when they come from the server. The working
	// type is the type the plotter uses. The mapping is:
	//    real          working
	//    byte, int16   short
    //    i32, uint16   int
	//    uint32        long
	//    float32       float
	//    float64       double
	// This conversion is done in two places, either below (only for the purposes
	// of calculating the parameters) or in vUpdateInfo in the  Variable Selector
	boolean zLoad( BaseType btValues, int iDimX1, int iDimY1, Object[] eggMissing, DAS das, StringBuffer sbError ){
		boolean zInverted = iDimX1 < iDimY1; // todo xxx
		if( btValues == null ){
			sbError.append("no base type supplied");
			return false;
		}
		if( !( btValues instanceof DArray) ){
			sbError.append("base type is not an array");
			return false;
		}
		DArray darray = (DArray)btValues;
		meType = DAP.getDArrayType(darray);
		if( meType == 0 ){
			sbError.append("unsupportable array type");
//Thread.dumpStack();
			return false;
		}
		msVariableName = darray.getName();
		try {
			switch( darray.numDimensions() ){
				case 0:
					sbError.append("array has zero dimensions");
					return false;
				case 1:
					miD1_length = darray.getDimension(0).getSize();
					miD2_length = 0;
					miD3_length = 0;
					break;
				case 2:
					miD1_length = darray.getDimension(0).getSize();
					miD2_length = darray.getDimension(1).getSize();
					miD3_length = 0;
					break;
				case 3:
					miD1_length = darray.getDimension(0).getSize();
					miD2_length = darray.getDimension(1).getSize();
					miD3_length = darray.getDimension(2).getSize();
					break;
			}
		} catch(Exception ex) {
			sbError.append("failed to ascertain dimension size(s)");
			return false;
		}
		Object oValues = darray.getPrimitiveVector().getInternalStorage();
		mzInverted = zInverted;

		// convert values to working type
		try {
			int ctValues;
			byte[] abValues = null;
			short[] ashValues = null;
			int[] aiValues = null;
			// long[] anValues = null; TODO check this
			switch( meType ){
				case DAP.DATA_TYPE_Int16:
				case DAP.DATA_TYPE_Int32:
				case DAP.DATA_TYPE_Float32:
				case DAP.DATA_TYPE_Float64:
					// existing array is ok
					break;
				case DAP.DATA_TYPE_Byte:
					abValues = (byte[])oValues;
					ctValues = abValues.length;
   					if( !Utility.zMemoryCheck(ctValues, 2, sbError) ) return false;
					short[] ashWorkingValues0 = new short[ctValues];
					for( int xValue = 0; xValue < ctValues; xValue++ ){
						ashWorkingValues0[xValue] = (short)((short)abValues[xValue] & 0xFF);
					}
					oValues = ashWorkingValues0;
					break;
				case DAP.DATA_TYPE_UInt16:
					ashValues = (short[])oValues;
					ctValues = ashValues.length;
   					if( !Utility.zMemoryCheck(ctValues, 4, sbError) ) return false;
					int[] aiWorkingValues0 = new int[ctValues];
					for( int xValue = 0; xValue < ctValues; xValue++ ){
						aiWorkingValues0[xValue] = (int)((int)ashValues[xValue] & 0xFFFF);
					}
					oValues = aiWorkingValues0;
					break;
				case DAP.DATA_TYPE_UInt32:
					aiValues = (int[])oValues;
					ctValues = aiValues.length;
   					if( !Utility.zMemoryCheck(ctValues, 8, sbError) ) return false;
					long[] anWorkingValues0 = new long[ctValues];
					for( int xValue = 0; xValue < ctValues; xValue++ ){
						anWorkingValues0[xValue] = (long)((long)aiValues[xValue] & 0xFFFFFFFF);
					}
					oValues = anWorkingValues0;
					break;
				default:
					sbError.append("unknown array data type: " + meType);
	    			return false;
			}
		} catch(Exception ex) {
			sbError.append("error converting values: " + meType);
   			return false;
		}
		meggWorkingValues[0] = oValues;
		boolean zGotMVfromDAS = false;
		if( das == null ){
		} else {
			StringBuffer sbDASerror = new StringBuffer();
			String sVariableName = btValues.getName();
			Object[] aoMV = DAP.getMissingValues( das, sVariableName, sbDASerror );
			if( aoMV == null ){
				if( sbDASerror.length() > 0 ) ApplicationController.vShowWarning("error extracting missing values from DAS: " + sbDASerror);
			} else {

				// make the array one-based
				String[] asMV1 = new String[aoMV.length + 1];
				for( int xMV = 0; xMV < aoMV.length; xMV++ ){
					asMV1[xMV + 1] = (String)aoMV[xMV];
				}
				int eJAVA_TYPE_missing = DataParameters.getJavaType_for_DAPType(meType);
				eggMissing = DAP.convertToEgg_Java(asMV1, eJAVA_TYPE_missing, sbError);
				zGotMVfromDAS = true;
			}
		}

		if( zGotMVfromDAS ){
			msMissingCode = "D";
		} else {
			PlotEnvironment def = Model.get().getPlotDataModel().getPlotEnvironment();
			boolean zCalcMissingValues = true;
			if( def != null ){
				PlotOptions po = def.getOptions();
				if( po != null ){
					OptionItem optionAutoCalcMissing = po.getOption(PlotOptions.OPTION_AutoCalcMissing);
					if( optionAutoCalcMissing != null ){
						zCalcMissingValues = optionAutoCalcMissing.getValue_YesNo();
					}}}
			if( zCalcMissingValues ){
				if( eggMissing == null || eggMissing[0] == null ){
					eggMissing = calcMissingValuesEgg( meggWorkingValues, meType, sbError );
					msMissingCode = "C";
				}
			} else {
				double[] adMissingValues1 = new double[1]; // no missing values
				int eJAVA_TYPE_missing = DataParameters.getJavaType_for_DAPType(meType);
				eggMissing = DAP.convertToEgg_Java(adMissingValues1, eJAVA_TYPE_missing, sbError);
				msMissingCode = "-";
			}
		}
		if( eggMissing == null ){
			ApplicationController.vShowWarning("Failed to calculate missing values: " + sbError);
			sbError.setLength(0);
			double[] adMissingValues1 = new double[1]; // no missing values
			int eJAVA_TYPE_missing = DataParameters.getJavaType_for_DAPType(meType);
			eggMissing = DAP.convertToEgg_Java(adMissingValues1, eJAVA_TYPE_missing, sbError);
			msMissingCode = "-";
		}
		meggMissing = eggMissing;
		vUpdateRanges();
		return true;
	}

	private void vUpdateRanges(){
		int lenData;
		int lenMissing;
		switch( meType ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				short[] ashData = (short[])meggWorkingValues[0];
				short[] ashMissing1 = (short[])meggMissing[0];
				lenData = ashData.length;
				lenMissing = ashMissing1.length - 1;
				shDataTo = Short.MAX_VALUE *-1; shDataFrom = Short.MAX_VALUE;
				for( int xData = 0; xData < lenData; xData++ ){
					short shValue = ashData[xData];
					int xMissing = 0;
					while( true ){
						xMissing++;
						if( xMissing > lenMissing ){
							if( shValue < shDataFrom ) shDataFrom = shValue;
	        				if( shValue > shDataTo ) shDataTo = shValue;
							break;
						}
						if( shValue == ashMissing1[xMissing] ) break;
					}
				}
				break;
			case DAP.DATA_TYPE_Int32:
			case DAP.DATA_TYPE_UInt16:
				int[] aiData = (int[])meggWorkingValues[0];
				int[] aiMissing1 = (int[])meggMissing[0];
				lenData = aiData.length;
				lenMissing = aiMissing1.length - 1;
				iDataTo = Integer.MAX_VALUE *-1; iDataFrom = Integer.MAX_VALUE;
				for( int xData = 0; xData < lenData; xData++ ){
					int iValue = aiData[xData];
					int xMissing = 0;
					while( true ){
						xMissing++;
						if( xMissing > lenMissing ){
							if( iValue < iDataFrom ) iDataFrom = iValue;
	        				if( iValue > iDataTo ) iDataTo = iValue;
							break;
						}
						if( iValue == aiMissing1[xMissing] ) break;
					}
				}
				break;
			case DAP.DATA_TYPE_UInt32:
				long[] anData = (long[])meggWorkingValues[0];
				long[] anMissing1 = (long[])meggMissing[0];
				lenData = anData.length;
				lenMissing = anMissing1.length - 1;
				nDataTo = Long.MAX_VALUE*-1; nDataFrom = Long.MAX_VALUE;
				for( int xData = 0; xData < lenData; xData++ ){
					long nValue = anData[xData];
					int xMissing = 0;
					while( true ){
						xMissing++;
						if( xMissing > lenMissing ){
							if( nValue < nDataFrom ) nDataFrom = nValue;
	        				if( nValue > nDataTo ) nDataTo = nValue;
							break;
						}
						if( nValue == anMissing1[xMissing] ) break;
					}
				}
				break;
			case DAP.DATA_TYPE_Float32:
				float[] afData = (float[])meggWorkingValues[0];
				float[] afMissing1 = (float[])meggMissing[0];
				lenData = afData.length;
				lenMissing = afMissing1.length - 1;
				fDataTo = Short.MAX_VALUE * -1; fDataFrom = Short.MAX_VALUE;
				for( int xData = 0; xData < lenData; xData++ ){
					float fValue = afData[xData];
					int xMissing = 0;
					while( true ){
						xMissing++;
						if( xMissing > lenMissing ){
							if( fValue < fDataFrom ) fDataFrom = fValue;
	        				if( fValue > fDataTo ) fDataTo = fValue;
							break;
						}
						if( fValue == afMissing1[xMissing] ) break;
					}
				}
				break;
			case DAP.DATA_TYPE_Float64:
				double[] adData = (double[])meggWorkingValues[0];
				double[] adMissing1 = (double[])meggMissing[0];
				lenData = adData.length;
				lenMissing = adMissing1.length - 1;
				dDataTo = Short.MAX_VALUE * -1; dDataFrom = Short.MAX_VALUE;
				for( int xData = 0; xData < lenData; xData++ ){
					double dValue = adData[xData];
					int xMissing = 0;
					while( true ){
						xMissing++;
						if( xMissing > lenMissing ){
							if( dValue < dDataFrom ) dDataFrom = dValue;
	        				if( dValue > dDataTo ) dDataTo = dValue;
							break;
						}
						if( dValue == adMissing1[xMissing] ) break;
					}
				}
				break;
			case DAP.DATA_TYPE_String:
			default:
				ApplicationController.vShowWarning("Unable to process type during plot parameter calculation: " + meType);
			    return;
		}

		// set default CS
		Panel_ColorSpecification panelCS = Panel_View_Plot._getPanel_ColorSpecification();
		if( panelCS != null ){
			ColorSpecification csDefault = panelCS.getDefaultCS();
			if( csDefault != null ){
				int iMissingColor = 0xFF0000FF;
				csDefault.setDataType( meType );
				csDefault.rangeRemoveAll(null);
				csDefault.setMissing(meggMissing, meType, iMissingColor);
				csDefault.vGenerateCG_Rainbow_Little(); // default style
				if( panelCS.getColorSpecification_Showing() == csDefault ) panelCS.setRange(0);
			}
		}

		// make sure CS info is updated (in case a mismatch is occurring)
		panelCS.vUpdateInfo();

	}

	public int getTYPE(){ return meType; }
	public short getDataFrom_short(){ return shDataFrom; }
	public int getDataFrom_int(){ return iDataFrom; }
	public long getDataFrom_long(){ return nDataFrom; }
	public float getDataFrom_float(){ return fDataFrom; }
	public double getDataFrom_double(){ return dDataFrom; }
	public short getDataTo_short(){ return shDataTo; }
	public int getDataTo_int(){ return iDataTo; }
	public long getDataTo_long(){ return nDataTo; }
	public float getDataTo_float(){ return fDataTo; }
	public double getDataTo_double(){ return dDataTo; }
	public int getDim1Length(){ return miD1_length; }
	public int getDim2Length(){ return miD2_length; }
	public boolean getInverted(){ return mzInverted; }
	public Object[] getMissingEgg(){ return meggMissing; }
	public void setMissingEgg( Object[] o ){
		meggMissing = o;
		vUpdateRanges();
	}
	public String getMissingMethodCode(){ return msMissingCode; }

	static StringBuffer msbMissingValueCalculation = new StringBuffer(120);

	final public static int MAX_MISSING_VALUES = 50;
	public static Object[] calcMissingValuesEgg( Object[] eggData, int eVALUE_TYPE, StringBuffer sbError ){
		try {
			if( eggData == null ) return null;
			double[] ad = DAP.convertToDouble( eggData, eVALUE_TYPE, null, sbError );
			if( ad == null ){
				sbError.insert(0, "Failed to convert to convert data to double: ");
				return null;
			}
			Object[] eggMissingValues = new Object[1];
			Object[] eggClassCount = new Object[1];
			double[] adMissingValues1;
			if( calcPossibleMissingValues( ad, eggMissingValues, eggClassCount, true, sbError ) ){
				adMissingValues1 = (double[])eggMissingValues[0];
				int[] aiClassCount = (int[])eggClassCount[0];
				int ctMissingValues = adMissingValues1.length;
				if( ctMissingValues > MAX_MISSING_VALUES ){

					// build new artificial data array which is already sorted
					int iTotalValues = 0;
					for( int xClass = 1; xClass < aiClassCount.length; xClass++ ) iTotalValues += aiClassCount[xClass];
					if( !Utility.zMemoryCheck(iTotalValues, 8, sbError) ) return null;
					double[] adNew = new double[iTotalValues];
					int xInstance = 0;
					for( int xClass = 1; xClass < aiClassCount.length; xClass++ ){
						double dMV = adMissingValues1[xClass];
						for( int xClassInstance = 0; xClassInstance < aiClassCount[xClass]; xClassInstance++ ) adNew[xInstance++] = dMV;
					}

					// repeat algorithm
					if( calcPossibleMissingValues( adNew, eggMissingValues, eggClassCount, false, sbError ) ){
						adMissingValues1 = (double[])eggMissingValues[0];
					} else {
						return null;
					}
				}
			} else {
				return null;
			}

			// convert missing values back to the core type and return them
			int eJAVA_TYPE = DataParameters.getJavaType_for_DAPType(eVALUE_TYPE);
			Object[] eggMissing = DAP.convertToEgg_Java(adMissingValues1, eJAVA_TYPE, sbError);
			if( eggMissing == null ){
				sbError.insert(0, "Failed to convert missing doubles to native type: ");
				return null;
			}
			return eggMissing;
		} catch(Exception ex) {
			msbMissingValueCalculation.append("error calculating missing values: ");
			ApplicationController.vUnexpectedError(ex, sbError);
			return null;
		} finally {
			ApplicationController.getInstance().set("_mvcalc", msbMissingValueCalculation.toString());
		}
	}

	/** This algorithm works as follows:
	 *  (1) determine the average group size (the average size of groups of the same value)
	 *  (2) determine the standard deviation of the group size
	 *  (3) determine the multiplier as log(ctGroups)
	 *  (4) the outlier threshhold is then taken to be the multiplier x standard deviations plus average group size
	 *  (5) any groups of values containing more members than this threshhold are considered possible missing values
	 */
	private static boolean calcPossibleMissingValues( double[] ad, Object[] eggMissingValues, Object[] eggClassCount, boolean zSort, StringBuffer sbError ){
		if( ad == null ){
			sbError.insert(0, "no data supplied");
			return false;
		}
		try {
			int ctData = ad.length;

			double[] adSorted;
			if( zSort ){
				if( !Utility.zMemoryCheck(ctData, 8, sbError) ) return false;
				adSorted = new double[ctData];
				System.arraycopy(ad, 0, adSorted, 0, ctData);
				java.util.Arrays.sort(adSorted);
			} else {
				adSorted = ad; // in the case of a generated data array the data will already be sorted
			}
			double dGroupValue = Double.NaN;
			int ctGroups = 0;
			int sizeCurrentGroup = 1;
			int nRunningTotalSizes = 0;
			for( int xSorted = 0; xSorted < ctData; xSorted++ ){
				if( adSorted[xSorted] == dGroupValue ) sizeCurrentGroup++;
				if( adSorted[xSorted] != dGroupValue || (xSorted == ctData-1) ){
					ctGroups++;
					nRunningTotalSizes += sizeCurrentGroup;
					sizeCurrentGroup = 1;
					dGroupValue = adSorted[xSorted];
				}
			}
			long nAverageGroupSize = Math.round( nRunningTotalSizes / ctGroups );
			msbMissingValueCalculation.append("average group size: " + nAverageGroupSize +"\n");
			sizeCurrentGroup = 0;
			dGroupValue = Double.NaN;
			long nRunningTotalDeviationSquared = 0;
			for( int xSorted = 0; xSorted < ctData; xSorted++ ){
				if( adSorted[xSorted] == dGroupValue ) sizeCurrentGroup++;
				if( adSorted[xSorted] != dGroupValue || (xSorted == ctData-1) ){
					nRunningTotalDeviationSquared += ((nAverageGroupSize - sizeCurrentGroup)*(nAverageGroupSize - sizeCurrentGroup));
					sizeCurrentGroup = 1;
					dGroupValue = adSorted[xSorted];
				}
			}
			long nVariation = Math.round( nRunningTotalDeviationSquared / ctGroups );
			int ctMissingValues = 0;
			double[] adMissingValuesBuffer = new double[MAX_MISSING_VALUES+1];
			int[] aiClassCount = new int[MAX_MISSING_VALUES + 1];
			if( nVariation == 0 ){
				// all groups are the same size - no missing values
				msbMissingValueCalculation.append("no variation in group size");
			} else {
				double dStandardDeviation = Math.sqrt(nVariation);
				double dMultiplier99 = Math.log(ctGroups) / Math.log(10);
				long nOutlierThreshhold = nAverageGroupSize + Math.round( dStandardDeviation * dMultiplier99 );
				msbMissingValueCalculation.append("value group count: " + ctGroups + "\n");
				msbMissingValueCalculation.append("standard deviation: " + dStandardDeviation + "\n");
				msbMissingValueCalculation.append("multiplier 99% confidence: " + dMultiplier99 + "\n");
				msbMissingValueCalculation.append("outlier threshhold: " + nOutlierThreshhold + "\n");
				sizeCurrentGroup = 0;
				dGroupValue = adSorted[0];
				for( int xSorted = 0; xSorted < ctData; xSorted++ ){
					if( adSorted[xSorted] == dGroupValue ) sizeCurrentGroup++;
					if( adSorted[xSorted] != dGroupValue || (xSorted == ctData-1) ){
						if( sizeCurrentGroup > nOutlierThreshhold ){
							ctMissingValues++;
							msbMissingValueCalculation.append("mv " + ctMissingValues + ": " + dGroupValue + " count: " + sizeCurrentGroup + "\n");
							if( ctMissingValues > adMissingValuesBuffer.length - 1 ){ // expand the storage arrays
								int iNewBufferLength = (adMissingValuesBuffer.length-1)*2 + 1;
		    					if( !Utility.zMemoryCheck(iNewBufferLength, 8, sbError) ) return false;
								double[] adNewValuesBuffer = new double[iNewBufferLength];
								System.arraycopy(adMissingValuesBuffer, 0, adNewValuesBuffer, 0, adMissingValuesBuffer.length);
								adMissingValuesBuffer = adNewValuesBuffer;
								int[] aiBuffer = new int[iNewBufferLength];
								System.arraycopy(aiClassCount, 0, aiBuffer, 0, aiClassCount.length);
								aiClassCount = aiBuffer;
							}
							adMissingValuesBuffer[ctMissingValues] = dGroupValue;
							aiClassCount[ctMissingValues] = sizeCurrentGroup;
						}
						sizeCurrentGroup = 1;
						dGroupValue = adSorted[xSorted];
					}
				}
				msbMissingValueCalculation.append("outlier count: " + ctMissingValues + "\n");
			}
			double[] adMissingValues1 = new double[ctMissingValues + 1];
			System.arraycopy(adMissingValuesBuffer, 1, adMissingValues1, 1, ctMissingValues);
			int[] aiClassCount1 = new int[ctMissingValues + 1];
			System.arraycopy(aiClassCount, 1, aiClassCount1, 1, ctMissingValues);
			eggMissingValues[0] = adMissingValues1;
			eggClassCount[0] = aiClassCount1;
			return true;
		} catch(Exception ex) {
			msbMissingValueCalculation.append("error during calculation");
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

}



