package opendap.clients.odc.plot;

/**
 * Title:        Panel_View_Plot
 * Description:  Manages the plotting interface
 * Copyright:    Copyright (c) 2003-4
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.45
 */

import opendap.dap.*;
import opendap.clients.odc.*;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.io.*;
import java.util.*;

public class Panel_View_Plot extends JPanel {

	private static Panel_View_Plot thisInstance;

	private Panel_Definition mDefinitionPanel;

	public final static int SOURCE_Table = 1;
	public final static int SOURCE_SelectedURL = 2;
	JPanel mjpanelDatasets;
	JRadioButton jrbFromTable;
	JRadioButton jrbFromSelectedURL;
	final JList jlistSelectedURLs = new JList();
	final DefaultListModel lmSelectedURLs = new DefaultListModel();
	final static DataParameters mDataParameters = new DataParameters();

	ArrayList listPlotterData = new ArrayList(); // type: DodsURL

	final static String[] asOutputOptions = {"Preview Pane", "External Window", "New Window", "Full Screen", "Print", "File (PNG)", "Thumbnails"};

	final private JButton buttonPlot = new JButton("Plot to");
	final private JButton buttonSelectAll = new JButton("Select All");
	final private JButton buttonUnload = new JButton("Unload");
	final private JButton buttonFileSave = new JButton("File Save...");
	final private JButton buttonFileLoad = new JButton("File Load...");
	final private JButton buttonInfo = new JButton("Info...");
	final private JPanel panelZoom = new JPanel();
	final private JComboBox jcbOutputOptions = new JComboBox(asOutputOptions);
	final private JPanel panelTN_Controls = new JPanel();

	private int mPlotType = Output_ToPlot.PLOT_TYPE_Pseudocolor; // default

	JFileChooser jfc = null;

	public Panel_View_Plot() {
		thisInstance = this;
	}

	public static Panel_View_Plot getInstance(){ return thisInstance; }

	public static JPanel getTN_Controls(){
		return thisInstance.panelTN_Controls;
	}

	public static PreviewPane getPreviewPane(){
		if( thisInstance.getActivePlottingDefinition() == null ){
		    return null;
		} else {
			return thisInstance.mDefinitionPanel.getPreviewPane();
		}
	}

	public static Panel_VariableTab getPanel_VariableTab(){
		if( thisInstance.getActivePlottingDefinition() == null ){
		    return null;
		} else {
			return thisInstance.mDefinitionPanel.getPanel_Variables();
		}
	}

	public static Panel_PlotScale getPanel_PlotScale(){
		if( thisInstance.getActivePlottingDefinition() == null ){
		    return null;
		} else {
			return thisInstance.mDefinitionPanel.getPanel_PlotScale();
		}
	}

	public static Panel_Thumbnails getPanel_Thumbnails(){
		if( thisInstance.getActivePlottingDefinition() == null ){
		    return null;
		} else {
			return thisInstance.mDefinitionPanel.getPanel_Thumbnails();
		}
	}

	public static Panel_ColorSpecification getPanel_ColorSpecification(){
		if( thisInstance.getActivePlottingDefinition() == null ){
		    return null;
		} else {
			return thisInstance.mDefinitionPanel.getPanel_ColorSpecification();
		}
	}

	public static Panel_Definition getPanel_Definition(){
		return thisInstance.mDefinitionPanel;
	}

	public static DataParameters getDataParameters(){ return mDataParameters; }

	public static void vTestLayout(){
		opendap.clients.odc.plot.Test_PlotLayout tpl = new opendap.clients.odc.plot.Test_PlotLayout();
		tpl.setVisible(true);
	}

	public boolean zInitialize(StringBuffer sbError){

		try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			this.setBorder(borderStandard);

			mDefinitionPanel = new Panel_Definition();
			if( !mDefinitionPanel.zInitialize( this, sbError ) ){
				sbError.insert(0, "failed to initialize definition panel");
				return false;
			}

			// setup plot type
			final JComboBox jcbPlotType = new JComboBox(Output_ToPlot.asPLOT_TYPES);
			jcbPlotType.setSelectedIndex(0); // default to first item
			Border borderInset = javax.swing.BorderFactory.createLoweredBevelBorder();
			jcbPlotType.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						int index = jcbPlotType.getSelectedIndex();
						if( index < 0 ) index = 0;
						String sPlotType = Output_ToPlot.asPLOT_TYPES[index];
						if( sPlotType.equals("Pseudocolor") ){
							setPlotType( Output_ToPlot.PLOT_TYPE_Pseudocolor );
						} else if( sPlotType.equals("Vector") ){
							setPlotType( Output_ToPlot.PLOT_TYPE_Vector );
						} else if( sPlotType.equals("XY") ){
							setPlotType( Output_ToPlot.PLOT_TYPE_XY );
						} else if( sPlotType.equals("Histogram") ){
							setPlotType( Output_ToPlot.PLOT_TYPE_Histogram );
						}
						Panel_View_Plot.getPanel_Definition().vActivateVariableSelector();
					}
				}
			);

			jcbOutputOptions.setSelectedIndex(0);

			// Create command panel
			JPanel panelCommand = new JPanel();
			panelCommand.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			Styles.vApply(Styles.STYLE_BigBlueButton, buttonPlot);
			buttonPlot.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e) {
						vPlot();
					}
				}
			);

			// create show datasets check box
			final JCheckBox jcheckShowDatasets = new JCheckBox(" Show Datasets");
			jcheckShowDatasets.setSelected(true);
			jcheckShowDatasets.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						vShowDatasets( jcheckShowDatasets.isSelected() );
					}
				}
			);

			// create freeze definition check box
			final JCheckBox jcheckFreezeDefinition = new JCheckBox(" Freeze Definition");
			jcheckFreezeDefinition.setSelected(false);
			jcheckFreezeDefinition.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						vFreezeDefinition( jcheckFreezeDefinition.isSelected() );
					}
				}
			);

			final JPanel panelChecks = new JPanel();
			panelChecks.setLayout(new BoxLayout(panelZoom, BoxLayout.Y_AXIS));
			panelChecks.add( jcheckShowDatasets );
			panelChecks.add( jcheckFreezeDefinition );

			// create zoom control
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
					Plot_Definition pd = getActivePlottingDefinition();
					if( pd != null ){
						PlotScale scale = pd.getScale();
						if( scale != null ){
							Object oSource = event.getSource();
							if( oSource == buttonZoom_In ){
								if( scale.zZoomIn() ) vPlot();
							}
							if( oSource == buttonZoom_Out ){
								if( scale.zZoomOut() ) vPlot();
							}
							if( oSource == buttonZoom_Maximize ){
								if( scale.zZoomMaximize() ) vPlot();
							}
						}
					}
			    }
			};
			buttonZoom_In.addActionListener(listenerZoom);
			buttonZoom_Out.addActionListener(listenerZoom);
			buttonZoom_Maximize.addActionListener(listenerZoom);

			// thumbnail control
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
					if( oSource == buttonTN_SelectAll ) Panel_View_Plot.getInstance().mDefinitionPanel.vTN_SelectAll();
					if( oSource == buttonTN_DeleteSelected ) Panel_View_Plot.getInstance().mDefinitionPanel.vTN_DeleteSelected();
					if( oSource == buttonTN_ReRetrieve ) Panel_View_Plot.getInstance().mDefinitionPanel.vTN_ReRetrieve();
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

			vSetupSourceSelector();

			JPanel jpanelNorth = new JPanel();
			jpanelNorth.setLayout(new BoxLayout(jpanelNorth, BoxLayout.Y_AXIS));
			jpanelNorth.add(mjpanelDatasets);
			jpanelNorth.add(panelCommand);

			this.setLayout(new BorderLayout());
			this.add(jpanelNorth, BorderLayout.NORTH);
			this.add(mDefinitionPanel, BorderLayout.CENTER);
			// this.add(jpanelInstructions, BorderLayout.SOUTH); // leave out the instructions for now

			mDefinitionPanel.vClear();

			setPlotType(Output_ToPlot.PLOT_TYPE_Pseudocolor);

            return true;

        } catch(Exception ex){
			Utility.vUnexpectedError(ex, sbError);
            return false;
        }
	}

	private boolean mzFreezeDefinition = false;
	void vFreezeDefinition( boolean z ){
		mzFreezeDefinition = z;
	}

	void vPlot(DodsURL urlToPlot){
		int eOutputOption = getOutputOption();
		vPlot(urlToPlot, eOutputOption);
	}

	void vPlot(DodsURL urlToPlot, int eOutput){
		for( int xListItem = 0; xListItem < lmSelectedURLs.getSize(); xListItem++ ){
			DodsURL urlCurrent = (DodsURL)lmSelectedURLs.get(xListItem);
			if( urlCurrent == urlToPlot ){
				vActivateListItem(xListItem);
				vPlot(eOutput);
				return;
			}
		}
		ApplicationController.vShowWarning("URL not found in dataset list: " + urlToPlot);
	}

	public static int getOutputOption(){ return thisInstance.jcbOutputOptions.getSelectedIndex() + 1; }

	void vPlot(){
		int eOutputOption = getOutputOption();
		vPlot(eOutputOption);
	}

	void vPlot(final int eOutputOption){ // on the event loop
		try {

			if( eOutputOption == Output_ToPlot.FORMAT_Thumbnail && Panel_View_Plot.getPlotType() == Output_ToPlot.PLOT_TYPE_Vector ){
				ApplicationController.vShowError("Currently thumbnails cannot be generated for vector plots.");
				return;
			}

			final StringBuffer sbError = new StringBuffer(80);
			if( eOutputOption == Output_ToPlot.FORMAT_PreviewPane ) Panel_View_Plot.getInstance().mDefinitionPanel.vActivatePreview();
			if( eOutputOption == Output_ToPlot.FORMAT_Thumbnail ) Panel_View_Plot.getInstance().mDefinitionPanel.vActivateThumbnails();
			final int[] aiSelected = jlistSelectedURLs.getSelectedIndices();
			final boolean zMultiplot = ( jrbFromSelectedURL.isSelected() && aiSelected.length > 1 );
			try {
				final Plot_Definition defActive = getActivePlottingDefinition();
				PlotOptions po = defActive.getOptions();
				if( defActive == null ){
					ApplicationController.vShowWarning("null definition encountered during multi-plot");
					return;
				}
				if( zMultiplot ){
					vActivateListItem(aiSelected[0]);
					final int miMultiplotDelay = po.getValue_int(PlotOptions.OPTION_MultiplotDelay);
					final Activity activityMultiplot = new Activity();
					Continuation_DoCancel con = new Continuation_DoCancel(){
						public void Do(){
							int ctSelections = aiSelected.length;
							long nLastPlot = System.currentTimeMillis();
							for( int xSelection = 0; xSelection < ctSelections; xSelection++ ){
								vActivateListItem(aiSelected[xSelection]);
								Thread.yield(); // allow swing events to occur after activation
								PlottingData pdat = Panel_View_Plot.getPanel_VariableTab().getDataset(sbError);
								if( pdat == null ){
									ApplicationController.vShowError_NoModal("Error plotting selection " + (xSelection+1) + " of " + ctSelections + ": " + sbError);
									sbError.setLength(0);
									continue;
								}
								if( Output_ToPlot.zPlot(pdat, defActive, eOutputOption, sbError) ){
									try {
										Thread.sleep(100); // Thread.yield(); // allow screen updates to occur between plots
									} catch(Exception ex) {}
								} else {
									ApplicationController.vShowError("Plotting error: " + sbError);
								}
								if( eOutputOption != Output_ToPlot.FORMAT_Thumbnail ){
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
					final Activity activitySinglePlot = new Activity();
					Continuation_DoCancel con = new Continuation_DoCancel(){
						public void Do(){
							PlottingData pdat = Panel_View_Plot.getPanel_VariableTab().getDataset(sbError);
							if( pdat == null ){
								ApplicationController.vShowError_NoModal("Plotting: " + sbError);
							} else {
								if( !Output_ToPlot.zPlot(pdat, defActive, eOutputOption, sbError) ){
									ApplicationController.vShowError("Plotting error: " + sbError);
								}
							}
						}
						public void Cancel(){}
					};
					activitySinglePlot.vDoActivity(null, null, con, null);
				}
		   } catch(Throwable t) {
			   Utility.vUnexpectedError(t, "While plotting");
		   }
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, "Error plotting: ");
		}
	}

	void vShowDatasets( boolean z ){
		this.mjpanelDatasets.setVisible(z);
	}

	// this method must be invoked on the event thread
	public boolean zAddData_Invoked( DodsURL url, StringBuffer sbError ){
		return this.source_Add(url, sbError);
	}

	void setPlottingEnabled( boolean z ){
		this.buttonPlot.setEnabled(z);
	}

	void vSetupSourceSelector(){

		jrbFromTable = new JRadioButton("Table View");
		jrbFromTable.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					Panel_View_Plot.this.setPlotType(Panel_View_Plot.this.getPlotType());
				}
			}
		);

		jrbFromSelectedURL = new JRadioButton("Selected URL:");
		jrbFromSelectedURL.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					Panel_View_Plot.this.setPlotType(Panel_View_Plot.this.getPlotType());
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
		javax.swing.ImageIcon imageInternet = Utility.imageiconLoadResource(Resources.ICONS_InternetConnection, sbError);
		if( imageInternet == null ){
			ApplicationController.getInstance().vShowError( "failed to load internet icon while setting up source selector: " + sbError );
			return;
		}
		buttonSelectAll.addActionListener(
			new ActionListener(){
	    		public void actionPerformed(ActionEvent event) {
					try {
						jlistSelectedURLs.setSelectionInterval(0, jlistSelectedURLs.getModel().getSize() - 1);
					} catch(Exception ex) {
						Utility.vUnexpectedError(ex, "while selecting all plotting datasets");
					}
			    }
			}
		);
		buttonUnload.addActionListener(
			new ActionListener(){
	    		public void actionPerformed(ActionEvent event) {
					int xURL_0;
					try {
						xURL_0 = jlistSelectedURLs.getSelectedIndex();
						if( xURL_0 < 0 ){
							ApplicationController.vShowWarning("nothing selected");
							return;
						}
						Panel_View_Plot.this.source_Unload(xURL_0);
						return;
					} catch(Exception ex) {
						StringBuffer sbError = new StringBuffer(80);
						Utility.vUnexpectedError(ex, sbError);
						ApplicationController.vShowError("Unexpected error unloading item: " + sbError);
					}
			    }
			}
		);
		buttonFileSave.addActionListener(
			new ActionListener(){
	    		public void actionPerformed(ActionEvent event) {
					vSaveSelectedListing();
			    }
			}
		);
		buttonFileLoad.addActionListener(
			new ActionListener(){
	    		public void actionPerformed(ActionEvent event) {
					vFileLoad();
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
						Utility.vUnexpectedError(ex, sbError);
						ApplicationController.vShowError("Unexpected error gettomg item info: " + sbError);
					}
			    }
			}
		);

		// list
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

		javax.swing.ButtonGroup bgSelectorOptions = new ButtonGroup();
		bgSelectorOptions.add(jrbFromTable);
		bgSelectorOptions.add(jrbFromSelectedURL);
		jlistSelectedURLs.setModel(lmSelectedURLs);

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
		mjpanelDatasets.add( buttonUnload, gbc );
		gbc.gridx = 2;
		mjpanelDatasets.add( buttonFileSave, gbc );
		gbc.gridx = 3;
		mjpanelDatasets.add( buttonFileLoad, gbc );
		gbc.gridx = 4;
		mjpanelDatasets.add( buttonInfo, gbc );
		gbc.gridx = 5;
		mjpanelDatasets.add( Box.createHorizontalGlue(), gbc );
		gbc.gridx = 0; gbc.gridy = 3;
		mjpanelDatasets.add( Box.createVerticalStrut(4), gbc );

		if( ConfigurationManager.getInstance().getProperty_DISPLAY_AllowPlotterFiles() ){
			buttonFileSave.setVisible(true);
			buttonFileLoad.setVisible(true);
		} else {
			buttonFileSave.setVisible(false);
			buttonFileLoad.setVisible(false);
		}

// not in use currently
//		JPanel jpanelInstructions = new JPanel();
//		JLabel jlabelInstructions = new JLabel("here are the instructions");
//		jpanelInstructions.add(jlabelInstructions);

		if( !Output_ToPlot.zInitialize( sbError) ){
			ApplicationController.vShowError("Error initializing plotting handler: " + sbError);
		}

	}

	void vActivateLastListItem(){
		int ctItems = listPlotterData.size();
		if( ctItems > 0 ){
			vActivateListItem(ctItems - 1);
		}
	}

	void vActivateListItem( int xItem_0 ){
		if( xItem_0 < 0 || xItem_0 >= listPlotterData.size() ){
			buttonUnload.setEnabled(false);
			buttonFileSave.setEnabled(false);
			buttonInfo.setEnabled(false);
		}
		buttonUnload.setEnabled(true);
		buttonFileSave.setEnabled(true);
		buttonInfo.setEnabled(true);
		if( !jrbFromSelectedURL.isSelected() ) jrbFromSelectedURL.setSelected(true);
		jlistSelectedURLs.setSelectedIndex(xItem_0);
		vSetDataToDefinition(xItem_0);
	}

	void vSetDataToDefinition(int xDataset_0){
		final DodsURL url = (DodsURL)Panel_View_Plot.this.listPlotterData.get(xDataset_0);
		int ePlotType = getPlotType();
		mDefinitionPanel.setData(url, Panel_Definition.VARIABLE_MODE_DDS, ePlotType);
		Plot_Definition pd = mDefinitionPanel.getActivePlottingDefinition();
		if( pd == null ) return;
		pd.setColorSpecification(Panel_View_Plot.getPanel_ColorSpecification().getColorSpecification());
	}

	void vSaveSelectedListing(){
		int xURL_0;
		try {
			xURL_0 = jlistSelectedURLs.getSelectedIndex();
			if( xURL_0 < 0 ){
				ApplicationController.vShowWarning("nothing selected");
				return;
			}
			final DodsURL url = (DodsURL)Panel_View_Plot.this.listPlotterData.get(xURL_0);
			String sTitle = url.getTitle();
			DataConnectorFile dcf = new DataConnectorFile();
			dcf.setTitle(url.getTitle());
			dcf.setURL(url);
			dcf.setData(url.getData());

			// ask user for desired location
			String sCacheDirectory = ConfigurationManager.getInstance().getProperty_DIR_DataCache();
			File fileCacheDirectory = Utility.fileEstablishDirectory(sCacheDirectory);
			if (jfc == null) jfc = new JFileChooser();
			if( fileCacheDirectory == null ){
				// not established
			} else {
				jfc.setCurrentDirectory(fileCacheDirectory);
			}
			String sSuggestedFileName = Utility.sFriendlyFileName(sTitle) + ConfigurationManager.EXTENSION_Data;
			jfc.setSelectedFile(new File(sSuggestedFileName));
			int iState = jfc.showDialog(Panel_View_Plot.this, "Select Save Location");
			File file = jfc.getSelectedFile();
			if (file == null || iState != JFileChooser.APPROVE_OPTION) return; // user cancel

			// try to save this directory as the new data cache directory
			File fileNewCacheDirectory = file.getParentFile();
			if( fileCacheDirectory != null ) if( !fileCacheDirectory.equals(fileNewCacheDirectory) ){
				String sNewCacheDirectory = fileNewCacheDirectory.getCanonicalPath();
				ConfigurationManager.getInstance().setOption(ConfigurationManager.getInstance().PROPERTY_DIR_DataCache, sNewCacheDirectory );
			}

			// save DCF
			String sPath = file.getPath();
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			try {
				oos.writeObject(dcf);
			} catch(Exception ex) {
				ApplicationController.vShowError("Failed to serialize object to file [" + sPath + "]: " + ex);
			} finally {
				try {
					if(fos!=null) fos.close();
				} catch(Exception ex) {}
			}
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer(80);
			Utility.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError("Unexpected error unloading item: " + sbError);
		}
	}

	void vFileLoad(){
		try {

			// determine file path
			String sCacheDirectory = ConfigurationManager.getInstance().getProperty_DIR_DataCache();
			File fileCacheDirectory = Utility.fileEstablishDirectory(sCacheDirectory);
			if (jfc == null) jfc = new JFileChooser();
			if( fileCacheDirectory != null ) jfc.setCurrentDirectory(fileCacheDirectory);
			int iState = jfc.showDialog(Panel_View_Plot.this, "Load");
			File file = jfc.getSelectedFile();
			if (iState != JFileChooser.APPROVE_OPTION)	return;
			if (file == null) return;

			// load serialized object
			StringBuffer sbError = new StringBuffer(80);
			Object o = Utility.oLoadObject(file, sbError);
			if( o == null ){
				ApplicationController.vShowError("Error loading Data DDS from file: " + sbError);
				return;
			}
			if( o instanceof opendap.clients.odc.plot.DataConnectorFile ){
				DataConnectorFile dcf = (DataConnectorFile)o;
				DodsURL url = dcf.getURL();
				url.setData(dcf.getData());
				if( Panel_View_Plot.this.source_Add(url, sbError) ){
					ApplicationController.vShowStatus("File loaded to plotter: " + url);
				} else {
					ApplicationController.vShowError("Error adding file as plotting source: " + sbError);
				}
			} else {
				ApplicationController.vShowError("Object in file " + file.getPath() + " is of type " +  o.getClass().getName() + " not a loadable type");
				return;
			}

		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer(80);
			Utility.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError("Unexpected error loading item: " + sbError);
		}
	}

	public static int getPlotType(){
		if( Panel_View_Plot.getInstance().mPlotType == 0 ) return Output_ToPlot.PLOT_TYPE_Pseudocolor;
	    return Panel_View_Plot.getInstance().mPlotType;
	}

    void setPlotType( int eTYPE ){
		if( eTYPE == mPlotType ) return; // no change
        mPlotType = eTYPE;
		mDefinitionPanel.setPlotType( eTYPE );
    }

	Plot_Definition getActivePlottingDefinition(){
		return mDefinitionPanel.getActivePlottingDefinition();
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
	boolean source_Add( DodsURL url, StringBuffer sbError ){
		try {
			if( url == null ){ sbError.append("no source supplied"); return false; }
			if( url.getData() == null ){ sbError.append("source has no data"); return false; }
			listPlotterData.add(url);
			lmSelectedURLs.addElement(url);
			Panel_View_Plot.this.vActivateLastListItem(); // activate this item that was just added
			return true;
		} catch(Exception ex) {
			sbError.append("System error adding plot source: " + ex);
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	void source_Unload( int xURL_0 ){
		try {
			if( xURL_0 >= this.listPlotterData.size() ){
				ApplicationController.vShowError("system error; url index outside loaded range");
			} else {
				DodsURL urlEntry = (DodsURL)listPlotterData.get(xURL_0);
				DataDDS ddds = urlEntry.getData();
				lmSelectedURLs.removeElement(urlEntry);
				listPlotterData.remove(xURL_0);
				if (ddds == mDefinitionPanel.getDataDDS()) mDefinitionPanel.vClear();
				Runtime.getRuntime().gc();
				Thread.yield();
			}
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("Unexpected error unloading: ");
			Utility.vUnexpectedError( ex, sbError );
			ApplicationController.vShowError(sbError.toString());
		}
	}
	void source_Info( int xURL_0 ){
		try {
			DataDDS ddds = null;
			if( xURL_0 >= this.listPlotterData.size() || xURL_0 < 0){
				ApplicationController.vShowError("system error; url index outside loaded range");
				return;
			}
			DodsURL urlEntry = (DodsURL)listPlotterData.get(xURL_0);
			StringBuffer sbInfo = new StringBuffer(120);
			sbInfo.append(urlEntry.getTitle()).append("\n");
			if( urlEntry == null ){
				sbInfo.append("[no url available]\n");
			} else {
				sbInfo.append(urlEntry).append("\n\n");
				sbInfo.append(urlEntry.getInfo());
			}
			JFrame frame = new JFrame();
			Utility.iconAdd(frame);
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
			Utility.vUnexpectedError( ex, sbError );
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
			long[] anValues = null;
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
			Plot_Definition def = Panel_View_Plot.getInstance().getActivePlottingDefinition();
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
		Panel_ColorSpecification panelCS = Panel_View_Plot.getPanel_ColorSpecification();
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
			Utility.vUnexpectedError(ex, sbError);
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
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

}

class PreviewPane extends JScrollPane {
	JPanel panelBlank;
	public PreviewPane(){
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		panelBlank = new JPanel();
		panelBlank.setLayout(new BorderLayout());
		panelBlank.add(Box.createGlue(), BorderLayout.CENTER);
		setClear();
	}
	void setClear(){
		setViewportView(panelBlank);
		revalidate();
	}
	void setContent( Component c ){
		setViewportView( c );
		revalidate();
	}
}


