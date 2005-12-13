package opendap.clients.odc;

/**
 * Title:        Panel_Retrieve_Output
 * Description:  Output controls for a list of selected datasets
 * Copyright:    Copyright (c) 2003-4
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.59
 */

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Panel_Retrieve_Output extends JPanel {

	Panel_Retrieve_Output() {}

	private Model_Retrieve theModel;

    private javax.swing.JTextField jtfPath;
	private JFileChooser jfc = null;
	private JComboBox jcbFileFormat;
	private JComboBox jcbTarget;
	private JLabel labelSize;

	ComboBoxModel modelCombo_Blank;
	ComboBoxModel modelCombo_Text;
	ComboBoxModel modelCombo_Image;
	ComboBoxModel modelCombo_Data;

	final JPanel panelFileSpec = new JPanel();

    boolean zInitialize( StringBuffer sbError ){

        try {

			Model_Retrieve model = ApplicationController.getInstance().getRetrieveModel();
			if( model == null ){
				sbError.append("no model");
				return false;
			} else {
				theModel = model;
			}

			final JPanel panelTarget = new JPanel();
			panelFileSpec.setVisible(false);

			javax.swing.ImageIcon imageInternet = Utility.imageiconLoadResource("icons/internet-connection-icon.gif");

			String[] asTargets_Blank = { "[no selection]" };
			String[] asTargets_Text = { "Text View", "File", "Standard Out", "Clipboard" };
			String[] asTargets_Image = { "Image Viewer" };
			String[] asTargets_Data = { "Plotter", "Text View", "Table View" };

			modelCombo_Blank = new DefaultComboBoxModel( asTargets_Blank );
			modelCombo_Text = new DefaultComboBoxModel( asTargets_Text );
			modelCombo_Image = new DefaultComboBoxModel( asTargets_Image );
			modelCombo_Data = new DefaultComboBoxModel( asTargets_Data );

			jcbTarget = new JComboBox();
			jcbTarget.setModel( modelCombo_Blank );
			final ActionListener actionActivateFileSpec =
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						if( jcbTarget.getSelectedItem().equals("File") ){
							panelFileSpec.setVisible(true);
						} else {
							panelFileSpec.setVisible(false);
						}
					}
				};
			jcbTarget.addActionListener(actionActivateFileSpec);

			final JButton buttonOutput = new JButton();
			Styles.vApply( Styles.STYLE_NetworkAction, buttonOutput );
			buttonOutput.setText("Output to");
			buttonOutput.setIcon(imageInternet);
			final JButton buttonTarget = null; // no longer allow canceling on this button because of reset failure bug todo
			final ActionListener actionSend =
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						int xSelection = jcbTarget.getSelectedIndex();
						if( xSelection < 0 ) return;
						StringBuffer sbError = new StringBuffer(80);
						Model_URLList url_model = theModel.getURLList();
						DodsURL[] aURLs  = url_model.getSelectedURLs(sbError);
						if( aURLs == null ){
							ApplicationController.vShowError("Failed to get selected URLs: " + sbError);
							return;
						}
						ComboBoxModel model = jcbTarget.getModel();
						String sSelection = model.getSelectedItem().toString();
						if( sSelection == null ){
							ApplicationController.vShowWarning("output selection model was unexpectedly missing");
						} else if( sSelection.equalsIgnoreCase("plotter") ){
							if( !ApplicationController.getInstance().getOutputEngine().zOutputToPlotter(aURLs, buttonTarget, this, sbError) ){
								ApplicationController.vShowError("Failed to output to plotter: " + sbError);
							}
						} else if( sSelection.equalsIgnoreCase("text") ){
							if( model == modelCombo_Text ){
								OutputStream os = ApplicationController.getInstance().getAppFrame().getTextViewerOS();
								String sRetrievePanelText = ApplicationController.getInstance().getAppFrame().getPanel_Retrieve().getPanelDDX().getRetrieveMessage();
								try {
									os.write( sRetrievePanelText.getBytes() );
								} catch( Throwable t ) {
									ApplicationController.getInstance().vShowError("Unexpected error trying to send retrieve panel text to Text View: " + t);
								}
							} else {
								if( !ApplicationController.getInstance().getOutputEngine().zOutputToTextView(aURLs, buttonTarget, this, sbError) ){
									ApplicationController.vShowError("Failed to output to text view: " + sbError);
								}
							}
						} else if( sSelection.equalsIgnoreCase("table") ){
							if( !ApplicationController.getInstance().getOutputEngine().zOutputToTableView(aURLs, buttonTarget, this, sbError) ){
								ApplicationController.vShowError("Failed to output to table view: " + sbError);
							}
						} else if( sSelection.equalsIgnoreCase("image") ){
							if( !ApplicationController.getInstance().getOutputEngine().zOutputToImageViewer(aURLs, buttonTarget, this, sbError) ){
								ApplicationController.vShowError("Failed to output to image viewer: " + sbError);
							}
						} else if( sSelection.equalsIgnoreCase("file") ){
							String sPath = jtfPath.getText();
							if( sPath.length() == 0 ){
								String sSeparator = Utility.getFileSeparator();
								String sBaseDirectory = ConfigurationManager.getInstance().getBaseDirectory();
								String sDataDirectory = Utility.sConnectPaths(sBaseDirectory, "data") + sSeparator;
								File fileDataDirectory = new File(sDataDirectory);
								if( !fileDataDirectory.exists() ){
									try {
										fileDataDirectory.createNewFile();
									} catch(Exception ex) {
										ApplicationController.vShowError("Unable to create default data directory (" + sDataDirectory + "): " + ex);
										return;
									}
								}
								sPath = sDataDirectory;
								jtfPath.setText(sPath);
							}
							int eFormat;
							String sFormat = jcbFileFormat.getSelectedItem().toString();
							if( sFormat.equalsIgnoreCase("BINARY") ){
								eFormat = OutputProfile.FORMAT_Data_Raw;
							} else if( sFormat.equalsIgnoreCase("ASCII") ){
								eFormat = OutputProfile.FORMAT_Data_ASCII_text;
							} else if( sFormat.equalsIgnoreCase("ASCII FORMATTED") ){
								eFormat = OutputProfile.FORMAT_Data_ASCII_records;
							} else {
								eFormat = OutputProfile.FORMAT_Data_ASCII_text; // default
							}
							if( ApplicationController.getInstance().getOutputEngine().zOutputToFile(aURLs, sPath, eFormat, buttonOutput, this, sbError) ){
								ApplicationController.getInstance().vShowStatus("" + aURLs.length + " data set" + (aURLs.length==1?"":"s") + " sent to file " + sPath);
							} else {
								ApplicationController.vShowError("Failed to output to file(s): " + sbError);
							}
						} else if( sSelection.equalsIgnoreCase("standard out") ){
							OutputProfile op = new OutputProfile(aURLs, OutputProfile.TARGET_StandardOut, null, OutputProfile.FORMAT_URLs);
							if( ApplicationController.getInstance().getOutputEngine().zOutputProfile( buttonTarget, this, op, sbError ) ){
								ApplicationController.getInstance().vShowStatus("" + aURLs.length + " data set" + (aURLs.length==1?"":"s") + " sent to standard out");
							} else {
								ApplicationController.vShowError("Failed to output to standard out: " + sbError);
							}
						} else if( sSelection.equalsIgnoreCase("clipboard") ){
							String sRetrievePanelText = ApplicationController.getInstance().getAppFrame().getPanel_Retrieve().getPanelDDX().getRetrieveMessage();
							Utility.vCopyToClipboard( sRetrievePanelText );
						}
				    }
				};
			buttonOutput.addActionListener(actionSend);

			labelSize = new JLabel();
			labelSize.setVisible(false);

			jtfPath = new javax.swing.JTextField();
			jtfPath.setText("");
			jtfPath.setPreferredSize(new Dimension(150, 20));

			JButton jbuttonBrowse = new JButton("...");
			final ActionListener actionBrowse =
				new ActionListener(){
				    public void actionPerformed(ActionEvent e){
						Panel_Retrieve_Output.this.vShowFileChooser();
					}
				};
			jbuttonBrowse.addActionListener(actionBrowse);

			String[] asFileFormats = { "Binary", "ASCII", "Formatted" };
			jcbFileFormat = new JComboBox(asFileFormats);

			setLayout(new java.awt.GridBagLayout());
			setMinimumSize(new Dimension(300, 200));
			GridBagConstraints gbc = new GridBagConstraints();

// no longer used
			// list
//			gbc.fill = gbc.BOTH;
//			gbc.gridwidth = 1;
//			gbc.weightx = 0; gbc.weighty = .01;
//			gbc.gridx = 0; gbc.gridy = 0;
//			this.add(labelSelected, gbc);
//			gbc.gridx = 1; gbc.weightx = 1;
//			this.add(Box.createHorizontalGlue(), gbc);
//			gbc.gridx = 2; gbc.weightx = 1;
//			this.add(buttonDeselect, gbc);
//			gbc.gridwidth = 3;
//			gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = .01;
//			this.add(Box.createVerticalStrut(6), gbc);
//			gbc.gridx = 0; gbc.gridy = 2;
//			gbc.weighty = .99;
//			this.add(jspSelected, gbc);

			// target button
			panelTarget.setLayout(new BoxLayout(panelTarget, BoxLayout.X_AXIS));
			panelTarget.add(buttonOutput);
			panelTarget.add(Box.createHorizontalStrut(5));
			panelTarget.add(jcbTarget);
			panelTarget.add(Box.createHorizontalStrut(8));
			panelTarget.add(labelSize);
			panelTarget.add(Box.createHorizontalGlue());
			gbc.gridwidth = 3;
			gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = .01;
			add(Box.createVerticalStrut(6), gbc);
			gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 1; gbc.weighty = .01;
			add(panelTarget, gbc);

			// file output
			panelFileSpec.setLayout(new BoxLayout(panelFileSpec, BoxLayout.X_AXIS));
// this button is no longer used
//			panelFileSpec.add(buttonTarget_File);
//			panelFileSpec.add(Box.createHorizontalStrut(4));
			panelFileSpec.add(jtfPath);
			panelFileSpec.add(Box.createHorizontalStrut(4));
			panelFileSpec.add(jbuttonBrowse);
			panelFileSpec.add(Box.createHorizontalStrut(4));
			panelFileSpec.add(jcbFileFormat);
			gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 1; gbc.weighty = .01;
			this.add(Box.createVerticalStrut(6), gbc);
			gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 1; gbc.weighty = .01;
			this.add(panelFileSpec, gbc);

            return true;

        } catch(Exception ex){
            Utility.vUnexpectedError(ex, sbError);
            return false;
        }

    }

	public void vUpdateOutput_Text(){
		jcbTarget.setModel( modelCombo_Text );
		panelFileSpec.setVisible(false);
	}

	public void vUpdateOutput_Blank(){
		jcbTarget.setModel( modelCombo_Blank );
		panelFileSpec.setVisible(false);
	}
	public void vUpdateOutput_Image(){
		jcbTarget.setModel( modelCombo_Image );
		panelFileSpec.setVisible(false);
	}
	public void vUpdateOutput_Data(){
		jcbTarget.setModel( modelCombo_Data );
		panelFileSpec.setVisible(false);
	}

	private static StringBuffer sbSelectedError = new StringBuffer(80);

//	void vUpdateSelected(){
//		lmSelected.removeAllElements();
//		Model_URLTable urlList = theModel.getURLList();
//		DodsURL[] aURLs = urlList.getSubSelectedURLs(sbSelectedError);
//		if( aURLs == null ){
//			ApplicationController.getInstance().vShowError("Failed to get selected URLs: " + sbSelectedError);
//			return;
//		}
//		if( aURLs.length < 1 ) return; // there are no URLs selected
//		for( int xURL = 0; xURL < aURLs.length; xURL++ ){
//			DodsURL urlKnown = theModel.getURLList().getKnownURL(aURLs[xURL].getFullURL());
//			if( urlKnown != null ) if( urlKnown.getDDS_Subset() != null && aURLs[xURL].getDDS_Subset() == null ) aURLs[xURL].setDDS_Subset(urlKnown.getDDS_Subset());
//			String sSizeEstimate = aURLs[xURL].getSizeEstimateS();
//			String sListEntry = sSizeEstimate + ' ' + aURLs[xURL].getFullURL();
//			lmSelected.addElement(sListEntry);
//		}
//		theModel.setLocationString(aURLs[0].getFullURL());
//	}


// not in use yet
	void setEstimatedDownloadSize( String sSize ){
		if( sSize == null ){
			labelSize.setVisible(false);
		} else {
			labelSize.setText("~size: " + sSize);
//			labelSize.setVisible(true);
			labelSize.setVisible(false);
		}
	}

	private void vShowFileChooser(){
		if( jfc == null ) jfc = new JFileChooser();
		int iState = jfc.showDialog(null, "Select File Location");
		File file = jfc.getSelectedFile();
		if( file != null && iState == JFileChooser.APPROVE_OPTION ){
			jtfPath.setText(file.getPath());
		}
	}

}



