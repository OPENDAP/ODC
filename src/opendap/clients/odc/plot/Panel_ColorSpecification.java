package opendap.clients.odc.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.Model;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.gui.Styles;

public class Panel_ColorSpecification extends JPanel implements IRangeChanged {
	private ColorSpecification mcsDefault;
	private Panel_RangeEditor mRangeEditor;
	private ColorSpecification mColorSpecification;
	private ColorSpecificationRenderer mcsr;
	private javax.swing.JList mlistEntries;
	private ListModel lmBlank = new DefaultListModel();
	private DefaultComboBoxModel mCSListModel;
	private JComboBox jcbCS;
	private JRadioButton jrbGenerated;
	private JRadioButton jrbSelected;
	private JButton buttonNewCS;
	private JLabel mlabelDataType;
	private JLabel mlabelTypeMismatchWarning;

	private final ArrayList<JComponent> mlistEditorControls = new ArrayList<JComponent>(); // all the controls that have to be enabled or disabled depending on the automatic selection

	Panel_ColorSpecification(){

		// create range editor
		mRangeEditor = new Panel_RangeEditor(this);
		mRangeEditor.set(null, 0);
		mlistEntries = new JList();
		mlistEntries.addListSelectionListener(
			new ListSelectionListener(){
				public void valueChanged(ListSelectionEvent lse) {
					if( lse.getValueIsAdjusting() ){ // this event gets fired multiple times on a mouse click--but the value "adjusts" only once
						int xEntry = mlistEntries.getSelectedIndex();
						if (xEntry < 0) {
							vRange_Edit(-1); // disable
						} else {
							vRange_Edit(xEntry);
						}
					}
				}
			}
		);
		mcsr = new ColorSpecificationRenderer();
		mlistEntries.setCellRenderer(mcsr);
		JScrollPane jspEntries = new JScrollPane(mlistEntries);
		JButton buttonAdd = new JButton("Add");
		buttonAdd.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					vRange_Add();
				}
			}
		);
		JButton buttonEdit = new JButton("Edit");
		buttonEdit.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					int xSelected0 = mlistEntries.getSelectedIndex();
					if( xSelected0 == -1 ) return; // nothing selected
					vRange_Edit( xSelected0 + 1 );
				}
			}
		);
		buttonEdit.setVisible(false);  // todo not in use convert to up/down buttons
		JButton buttonDel = new JButton("Del");
		buttonDel.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					int xSelected0 = mlistEntries.getSelectedIndex();
					if( xSelected0 == -1 ) return; // nothing selected
					if( xSelected0 == 0 ){
						JOptionPane.showMessageDialog(ApplicationController.getInstance().getAppFrame(), "The missing values specification cannot be deleted.");
					} else {
						vRange_Del( xSelected0 );
					}
				}
			}
		);
		JButton buttonDelAll = new JButton("Del All");
		buttonDelAll.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					StringBuffer sbError = new StringBuffer(80);
					if( !mColorSpecification.rangeRemoveAll(sbError) ){
						ApplicationController.vShowError("Failed to remove all ranges from CS: " + sbError);
					}
				}
			}
		);
		JButton buttonSave = new JButton("Save");
		buttonSave.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					vSave();
				}
			}
		);
		JButton buttonLoad = new JButton("Load");
		buttonLoad.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					vLoad();
				}
			}
		);

		// Range List
		JPanel panelRangeList = new JPanel();
		panelRangeList.setLayout(new java.awt.GridBagLayout());
		panelRangeList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Color Specification Ranges"));
		panelRangeList.setMinimumSize(new Dimension(300, 200));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0; gbc.gridy = 0;
		gbc.gridwidth = 5;
		gbc.weightx = 1; gbc.weighty = .99;
		panelRangeList.add(jspEntries, gbc);
		gbc.gridy = 1; gbc.gridwidth = 1; gbc.weighty = .01; gbc.weightx = .20;
		panelRangeList.add(buttonAdd, gbc);
		gbc.gridx = 1;
		panelRangeList.add(buttonDel, gbc);
		gbc.gridx = 2;
		panelRangeList.add(buttonDelAll, gbc);
		gbc.gridx = 3;
		panelRangeList.add(buttonSave, gbc);
		gbc.gridx = 4;
		panelRangeList.add(buttonLoad, gbc);

		ActionListener listenSetCS =
			new ActionListener(){
			    public void actionPerformed( java.awt.event.ActionEvent ae ){
					vUpdate();
				}
			};

		//  main toggle
		jrbGenerated = new JRadioButton("Plots generate their own colors");
		jrbSelected = new JRadioButton("Plot uses: ");
		jrbGenerated.addActionListener(listenSetCS);
		jrbSelected.addActionListener(listenSetCS);

		// setup cs list
		mlabelDataType = new JLabel("Data Type: [no cs]");
		mlabelTypeMismatchWarning = new JLabel("Active Dataset: [no cs]");
		mlabelTypeMismatchWarning.setVisible(false);
		Styles.vApply(Styles.STYLE_RedWarning, mlabelTypeMismatchWarning);
		mCSListModel = new DefaultComboBoxModel();
		jcbCS = new JComboBox(mCSListModel);
		jcbCS.addActionListener(listenSetCS);

		// create default color specification (can only be done after range editor is created)
		StringBuffer sbError = new StringBuffer(80);
		mcsDefault = new ColorSpecification("Default", DAP.DATA_TYPE_Float32);
		if( mcsDefault.rangeAdd(0f, 1f, 0xFFDFFFFF, 0xFF00FFFF, ColorSpecification.COLOR_STEP_SynchronizedDown, -1, 0xFF, 0xFF, 0xFF, false, sbError) ){
			mcsDefault.setMissingColor(0xFF0000FF);
			mCSListModel.addElement(mcsDefault);
		} else {
			ApplicationController.vShowError("failed to create default color specification: " + sbError);
		}

		// new CS
		buttonNewCS = new JButton("New CS");
		buttonNewCS.addActionListener(
			new java.awt.event.ActionListener(){
			    public void actionPerformed( java.awt.event.ActionEvent ae ){
					Panel_ColorSpecification.this.vNewCS();
				}
			}
		);

		// selection area
		ButtonGroup bgSelection = new ButtonGroup();
		jrbGenerated.setSelected(true); // default
		bgSelection.add(jrbGenerated);
		bgSelection.add(jrbSelected);
		JPanel panelSelection = new JPanel();
		panelSelection.setLayout(new java.awt.GridBagLayout());
		panelSelection.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Specification to Use"));
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);
		gbc.gridy = 1;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0; gbc.gridwidth = 5;
		panelSelection.add(jrbGenerated, gbc);
		gbc.gridx = 4; gbc.weightx = 1; gbc.gridwidth = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);
		gbc.gridy = 3;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelSelection.add(jrbSelected, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 1;
		panelSelection.add(jcbCS, gbc);
		gbc.gridx = 4; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 5; gbc.weightx = 0.0;
		panelSelection.add(buttonNewCS, gbc);
		gbc.gridx = 6; gbc.weightx = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);

		// data type
		gbc.gridy = 5;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelSelection.add(mlabelDataType, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 1;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 4; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 5; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc); // nothing in this slot
		gbc.gridx = 6; gbc.weightx = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);

		gbc.gridy = 6;
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);

		gbc.gridy = 7;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelSelection.add(mlabelTypeMismatchWarning, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 1;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 4; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 5; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc); // nothing in this slot
		gbc.gridx = 6; gbc.weightx = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);

		final JPanel panelColorGenerator = new JPanel();
		final JPanel panelCG_Rainbow = new JPanel();
		final JPanel panelCG_Banded = new JPanel();
		final JPanel panelCG_GrayScale = new JPanel();
		final JPanel panelCG_MultiHue = new JPanel();

		// set up rainbow
		final JRadioButton jrbFull = new JRadioButton("Full Spectrum");
		final JRadioButton jrbLittle = new JRadioButton("Little Rainbow");
		final JRadioButton jrbWeighted = new JRadioButton("Weighted Spectrum");
		final ButtonGroup bgRainbow = new ButtonGroup();
		bgRainbow.add(jrbFull);
		bgRainbow.add(jrbLittle);
		bgRainbow.add(jrbWeighted);
		jrbLittle.setSelected(true);
		panelCG_Rainbow.setLayout(new BoxLayout(panelCG_Rainbow, BoxLayout.Y_AXIS));
		panelCG_Rainbow.add(jrbFull);
		panelCG_Rainbow.add(jrbLittle);
		panelCG_Rainbow.add(jrbWeighted);
		panelCG_Rainbow.add(Box.createVerticalGlue());

		// set up banded
		final String[] asBandNumbers = new String[50];
		for( int x= 0; x< 50; x++)asBandNumbers[x] = Integer.toString(x+1);
		final JLabel labelBandCount = new JLabel("Band count:");
		final JComboBox jcbBandCount = new JComboBox(asBandNumbers);
		panelCG_Banded.add(labelBandCount);
		panelCG_Banded.add(Box.createHorizontalStrut(5));
		panelCG_Banded.add(jcbBandCount);

		// set up multi-hue options
		final String[] asHueNumbers = new String[2];
		asHueNumbers[0] = "2";
		asHueNumbers[1] = "3";
		final JLabel labelHueCount = new JLabel("Hue count:");
		final JComboBox jcbHueCount = new JComboBox(asHueNumbers);
		panelCG_MultiHue.add(labelHueCount);
		panelCG_MultiHue.add(Box.createHorizontalStrut(5));
		panelCG_MultiHue.add(jcbHueCount);

		panelColorGenerator.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Color Generator"));
		final String[] asColorGenerators = { "Rainbow", "Banded", "Gray Scale", "Multi-Hue" };
		final JComboBox jcbCG_selector = new JComboBox( asColorGenerators );
		jcbCG_selector.addActionListener(
			new java.awt.event.ActionListener(){
			    public void actionPerformed( java.awt.event.ActionEvent ae ){
					int iSelectedIndex = jcbCG_selector.getSelectedIndex();
					switch( iSelectedIndex ){
						case 0:
							panelCG_Rainbow.setVisible(true);
							panelCG_Banded.setVisible(false);
							panelCG_GrayScale.setVisible(false);
							panelCG_MultiHue.setVisible(false);
							break;
						case 1:
							panelCG_Rainbow.setVisible(false);
							panelCG_Banded.setVisible(true);
							panelCG_GrayScale.setVisible(false);
							panelCG_MultiHue.setVisible(false);
							break;
						case 2:
							panelCG_Rainbow.setVisible(false);
							panelCG_Banded.setVisible(false);
							panelCG_GrayScale.setVisible(true);
							panelCG_MultiHue.setVisible(false);
							break;
						case 3:
							panelCG_Rainbow.setVisible(false);
							panelCG_Banded.setVisible(false);
							panelCG_GrayScale.setVisible(false);
							panelCG_MultiHue.setVisible(true);
							break;
					}
					panelColorGenerator.revalidate();
				}
			}
		);
		final JButton buttonCG_create = new JButton("Create: ");
		buttonCG_create.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					ColorSpecification cs = Panel_ColorSpecification.this.mColorSpecification;
					if( cs == null ) return;
					switch( jcbCG_selector.getSelectedIndex() ){
						case 0: // rainbow
							if( jrbFull.isSelected() ){
								cs.vGenerateCG_Rainbow_Full();
							} else if( jrbLittle.isSelected() ){
								cs.vGenerateCG_Rainbow_Little();
							} else if( jrbWeighted.isSelected() ){
								cs.vGenerateCG_Rainbow_Weighted();
							}
							break;
						case 1: // banded
							int iBandCount = jcbBandCount.getSelectedIndex() + 1;
							cs.vGenerateCG_Banded(iBandCount);
							break;
						case 2: // gray scale
							cs.vGenerateCG_GrayScale();
							break;
						case 3: // multi-hue
							int iHueCount = jcbHueCount.getSelectedIndex() + 2;
							cs.vGenerateCG_MultiHue(iHueCount);
							break;
					}
				}
			}
		);

		final JPanel panelCG_control = new JPanel();
		panelCG_control.setMaximumSize(new java.awt.Dimension(400, 25));
		panelCG_control.setLayout(new BoxLayout(panelCG_control, BoxLayout.X_AXIS));
		panelCG_control.add(buttonCG_create);
		panelCG_control.add(Box.createHorizontalStrut(5));
		panelCG_control.add( jcbCG_selector );
		panelColorGenerator.setLayout(new BoxLayout(panelColorGenerator, BoxLayout.Y_AXIS));
		panelColorGenerator.add( panelCG_control );
		panelColorGenerator.add( Box.createVerticalStrut(6) );
		panelColorGenerator.add( panelCG_Rainbow );
		panelColorGenerator.add( panelCG_Banded );
		panelColorGenerator.add( panelCG_GrayScale );
		panelColorGenerator.add( panelCG_MultiHue );
		panelCG_GrayScale.setVisible(false);
		panelCG_Banded.setVisible(false);
		panelCG_GrayScale.setVisible(false);
		panelCG_MultiHue.setVisible(false);

		final JPanel panelLeft = new JPanel();
		panelLeft.setLayout(new BoxLayout(panelLeft, BoxLayout.Y_AXIS));
		panelLeft.add( panelSelection );
		panelLeft.add( panelRangeList );

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.add(panelLeft);
		this.add(mRangeEditor);
		this.add(panelColorGenerator);

		// setup enabling
		mlistEditorControls.add( mRangeEditor );
		mlistEditorControls.add( mlistEntries );
		mlistEditorControls.add( buttonAdd );
		mlistEditorControls.add( buttonEdit );
		mlistEditorControls.add( buttonDel );
		mlistEditorControls.add( buttonDelAll );
		mlistEditorControls.add( buttonSave );
		mlistEditorControls.add( buttonLoad );
		mlistEditorControls.add( jcbBandCount );
		mlistEditorControls.add( jrbFull );
		mlistEditorControls.add( jrbLittle );
		mlistEditorControls.add( jrbWeighted );
		mlistEditorControls.add( jcbHueCount );
		mlistEditorControls.add( jcbBandCount );
		mlistEditorControls.add( jcbHueCount );
		mlistEditorControls.add( jcbCG_selector );
		mlistEditorControls.add( buttonCG_create );

		listenSetCS.actionPerformed( null );
	}

	private void vUpdate(){
		if( jrbGenerated.isSelected() ){
			setColorSpecification( null );
			setEditorEnabled( false );
		} else {
			int xSelection = jcbCS.getSelectedIndex();
			if( xSelection < 0 ){
				setColorSpecification( null );
				setEditorEnabled( false );
			} else {
				setColorSpecification((ColorSpecification)mCSListModel.getElementAt(xSelection));
				setEditorEnabled( true );
			}
		}
	}

	private void setEditorEnabled( boolean z ){
		for( int xControl = 0; xControl < mlistEditorControls.size(); xControl++ ){
			Component component = (Component)mlistEditorControls.get(xControl);
			component.setEnabled(z);
		}
	}

	StringBuffer msbError = new StringBuffer(80);

	ColorSpecification getDefaultCS(){ return mcsDefault; }

	public void setMissingValueText( String sMissingText ){
		if( mRangeEditor != null && mColorSpecification != null ){
			mRangeEditor.setMissingValuesText(sMissingText);
			vRangeChanged(0);
		}
	}

	public void vRangeChanged( int iRangeIndex1 ){
		if( mColorSpecification == null ){
			ApplicationController.vShowError("Internal Error, range changed without cs existing");
			return;
		}
		if( iRangeIndex1 == 0 ){ // missing changed
			int iColorMissing = mRangeEditor.getColorMissing();
			String sMissingValues = mRangeEditor.getMissingValuesText();
			msbError.setLength(0);
			if( !mColorSpecification.setMissing(sMissingValues, iColorMissing, msbError) ){
				ApplicationController.vShowError("Unable to set missing: " + msbError);
			}
		} else { // range changed
			int iColorFrom = mRangeEditor.getColorFromHSB();
			int iColorTo = mRangeEditor.getColorToHSB();
			int eColorStep = mRangeEditor.getColorStep();
			int iHue = mRangeEditor.getHue();
			int iSat = mRangeEditor.getSat();
			int iBri = mRangeEditor.getBri();
			int iAlpha = mRangeEditor.getAlpha();
			String sDataFrom = mRangeEditor.getDataFromString();
			if( sDataFrom.length() == 0 ) sDataFrom = "0";
			String sDataTo = mRangeEditor.getDataToString();
			if( sDataTo.length() == 0 ) sDataTo = "1";
			msbError.setLength(0);
			switch( mColorSpecification.getDataType() ){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:
					try {
						short shDataFrom = Short.parseShort(sDataFrom);
						short shDataTo = Short.parseShort(sDataTo);
						if( !mColorSpecification.rangeSet( iRangeIndex1, shDataFrom, shDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, msbError) ){
							ApplicationController.vShowError("Unable to set range: " + msbError);
						}
					} catch(Exception ex) {
						ApplicationController.vShowError("Unable to understand data as short: [" + sDataFrom + " to " + sDataTo + "]");
					}
					return;
				case DAP.DATA_TYPE_Int32:
				case DAP.DATA_TYPE_UInt16:
					try {
						int iDataFrom = Integer.parseInt(sDataFrom);
						int iDataTo = Integer.parseInt(sDataTo);
						if( !mColorSpecification.rangeSet( iRangeIndex1, iDataFrom, iDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, msbError) ){
							ApplicationController.vShowError("Unable to set range: " + msbError);
						}
					} catch(Exception ex) {
						ApplicationController.vShowError("Unable to understand data as int: [" + sDataFrom + " to " + sDataTo + "]");
					}
					return;
				case DAP.DATA_TYPE_UInt32:
					try {
						long nDataFrom = Long.parseLong(sDataFrom);
						long nDataTo = Long.parseLong(sDataTo);
						if( !mColorSpecification.rangeSet( iRangeIndex1, nDataFrom, nDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, msbError) ){
							ApplicationController.vShowError("Unable to set range: " + msbError);
						}
					} catch(Exception ex) {
						ApplicationController.vShowError("Unable to understand data as long: [" + sDataFrom + " to " + sDataTo + "]");
					}
					return;
				case DAP.DATA_TYPE_Float32:
					try {
						float fDataFrom = Float.parseFloat(sDataFrom);
						float fDataTo = Float.parseFloat(sDataTo);
						if( !mColorSpecification.rangeSet( iRangeIndex1, fDataFrom, fDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, msbError) ){
							ApplicationController.vShowError("Unable to set range: " + msbError);
						}
					} catch(Exception ex) {
						ApplicationController.vShowError("Unable to understand data as float: [" + sDataFrom + " to " + sDataTo + "]");
					}
					return;
				case DAP.DATA_TYPE_Float64:
					try {
						double dDataFrom = Double.parseDouble(sDataFrom);
						double dDataTo = Double.parseDouble(sDataTo);
						if( !mColorSpecification.rangeSet( iRangeIndex1, dDataFrom, dDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, msbError) ){
							ApplicationController.vShowError("Unable to set range: " + msbError);
						}
					} catch(Exception ex) {
						ApplicationController.vShowError("Unable to understand data as double: [" + sDataFrom + " to " + sDataTo + "]");
					}
					return;
				case DAP.DATA_TYPE_String:
				default:
					ApplicationController.vShowError("Internal Error, unexpected data type in range change");
				return;
			}
		}
	}
	ColorSpecification getColorSpecification(){
		if( jrbSelected.isSelected() ) return mColorSpecification;
		return null;
	}
	ColorSpecification getColorSpecification_Showing(){
		return mColorSpecification;
	}
	void setColorSpecification( ColorSpecification cs ){
		mColorSpecification = cs;
		if( cs == null ){
			mlistEntries.setModel(lmBlank);
		} else {
			ListModel lmCurrent = mlistEntries.getModel();
			if( lmCurrent != null ) lmCurrent.removeListDataListener(mcsr);
			mlistEntries.setModel(cs);
			mcsr.vInitialize(cs);
			cs.addListDataListener(mcsr);
		}
		vUpdateInfo();
		mRangeEditor.set(cs, 0);
		opendap.clients.odc.data.Model_Dataset modelActive = Model.get().getPlotDataModel();
		if( modelActive != null ){
			PlotEnvironment pd = modelActive.getPlotEnvironment();
			if( pd != null ) pd.setColorSpecification(cs);
		}
	}
	public void vUpdateInfo(){
		if( mColorSpecification == null ){
			mlistEntries.setEnabled(false);
			mlabelDataType.setText("[no CS selected]");
			mlabelTypeMismatchWarning.setVisible(false);
		} else {
			mlistEntries.setEnabled(true);
			int eDataType_Active = Panel_View_Plot.getDataParameters().getTYPE();
			int eDataType_CS     = mColorSpecification.getDataType();
			mlabelDataType.setText("Data Type: " + DAP.getType_String( eDataType_CS ));
			if( eDataType_Active == 0 || eDataType_Active == eDataType_CS ){
				mlabelTypeMismatchWarning.setVisible(false);
			} else {
				mlabelTypeMismatchWarning.setText("Active Dataset:  " + DAP.getType_String( eDataType_Active ) + "  (cannot use)");
				mlabelTypeMismatchWarning.setVisible(true);
			}
		}
	}
	void vNewCS(){
		DataParameters dp = Panel_View_Plot.getDataParameters();
		String sName = JOptionPane.showInputDialog( ApplicationController.getInstance().getAppFrame(), "Enter name for new color specification: ");
		if( sName == null ) return;
		StringBuffer sbError = new StringBuffer(80);
		try {
			int eTYPE = dp.getTYPE();
			ColorSpecification csNew = new ColorSpecification(sName, eTYPE);
			int iMissingColor = 0xFF0000FF;
			csNew.setMissing(dp.getMissingEgg(), eTYPE, iMissingColor);
			csNew.vGenerateCG_Rainbow_Little(); // default style
			vAddCS( csNew );
			return; // all done
		} catch( Exception ex ) {
			ApplicationController.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError("Error attempting generate new CS from active definition: " + sbError);
			sbError.setLength(0);
		}
	}
	private void vAddCS( ColorSpecification cs ){
		mCSListModel.addElement( cs );
		jcbCS.setSelectedIndex(jcbCS.getItemCount() - 1);
		jrbSelected.setSelected(true);
	}
	void setRange( int xRange0 ){ jcbCS.setSelectedIndex(xRange0); }
	void vRange_Add(){
		if( mColorSpecification == null ) return;
		mColorSpecification.vGenerateCG_Rainbow_Little();
	}
	void vRange_Edit( int xEntry1 ){
		if( xEntry1 >= 0 )
			mRangeEditor.set(mColorSpecification, xEntry1);
	}
	void vRange_Del( int xEntry1 ){
		if( mColorSpecification == null ) return;
		if( xEntry1 == 0 ){
			mColorSpecification.setMissing(null, 0, 0);
		} else {
			StringBuffer sbError = new StringBuffer();
			if( !mColorSpecification.rangeRemove(xEntry1, sbError) ){
				ApplicationController.vShowError("Failed to remove item " + xEntry1 + ": " + sbError);
			}
		}
	}
	void vSave(){
		if( mColorSpecification == null ){
			// do nothing
		} else {
			StringBuffer sbError = new StringBuffer(80);
			if( !mColorSpecification.zStorage_save( sbError ) ){
				ApplicationController.vShowError(sbError.toString());
			}
		}
	}
	void vLoad(){
		StringBuffer sbError = new StringBuffer(80);
		String sInitializationString = "[loaded cs]";
		ColorSpecification cs = new ColorSpecification(sInitializationString, 0);
		if( cs.zStorage_load( sbError ) ){
			if( cs.getName() == null || cs.getName().equals(sInitializationString) ){
				return; // CS is blank, user cancelled
			}
			vAddCS( cs );
		} else {
			ApplicationController.vShowError(sbError.toString());
		}
	}
}

class ColorSpecificationRenderer extends JLabel implements ListCellRenderer, ListDataListener {
	protected Border mBorder_FocusCell = BorderFactory.createLineBorder(Color.cyan,1);
	protected Border mBorder_RegularCell = BorderFactory.createEmptyBorder(1,1,1,1);
	protected Insets mInsets = new Insets(1, 1, 1, 1);
	private int mTextX, mTextY;
	protected String sTextValue = "";
	ColorSpecification mColorSpecification;
	private Image mimageSwatch;
	private int mpxSwatchSize;
	public ColorSpecificationRenderer(){
		super();
	}
	public void vInitialize( ColorSpecification cs ){
		setOpaque(true);
		this.setMinimumSize(new Dimension(80, 20));
		this.setPreferredSize(new Dimension(400, 20));
		mColorSpecification = cs;
		mpxSwatchSize = cs.getSwatchWidth();
		mTextX = mInsets.left + mpxSwatchSize + 5;
	}
	public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean zCellHasFocus ){
		if( isSelected ){
			setForeground(list.getSelectionForeground());
			setBackground(list.getSelectionBackground());
		} else {
			setForeground(list.getForeground());
			setBackground(list.getBackground());
		}
		if( zCellHasFocus ){
			setBorder(mBorder_FocusCell);
		} else {
			setBorder(mBorder_RegularCell);
		}
		if( index == 0 ) mimageSwatch = mColorSpecification.getMissingSwatch();
		if( index > 0 ) mimageSwatch = mColorSpecification.getSwatch(index);
		if( value == null ){
			sTextValue = "[error]";
		} else {
			sTextValue = value.toString();
		}
		return this;
	}

	public void paint(Graphics g) {
		super.paint(g);
		int iCellWidth = getWidth();
		int iCellHeight = getHeight();
		g.setColor(getBackground());
		g.fillRect(0, 0, iCellWidth, iCellHeight);
		getBorder().paintBorder(this, g, 0, 0, iCellWidth, iCellHeight);
		if( mimageSwatch != null ) g.drawImage(mimageSwatch, mInsets.left+2, mInsets.top+2, getBackground(), null);
		g.setColor(getForeground());
		g.setFont(Styles.fontFixed12);
		FontMetrics fm = g.getFontMetrics();
		mTextY = mInsets.top + fm.getAscent();
		if( sTextValue == null ) sTextValue = "[error]";
		g.drawString(sTextValue, mTextX, mTextY);
	}

	public void intervalAdded(ListDataEvent e){ }
	public void intervalRemoved(ListDataEvent e){ }
	public void contentsChanged(ListDataEvent e){  }

}

class Panel_RangeEditor extends JPanel implements IColorChanged {
	private IRangeChanged mOwner;
	private int mxRange1;
	final private JRadioButton jrbSingleValue_Yes = new JRadioButton("Yes");
	final private JRadioButton jrbSingleValue_No = new JRadioButton("No");
	final private JRadioButton jrbSingleColor_Yes = new JRadioButton("Yes");
	final private JRadioButton jrbSingleColor_No = new JRadioButton("No");
	final private static String[] asColorSteps = {"synch up", "synch down", "cont up", "cont down"};
	final private JComboBox jcbColorStep = new JComboBox(asColorSteps);
	private JLabel mlabelRangeSwatch;
	private JLabel mlabelSwatch;
	private JTextField jtfDataFrom;
	private JTextField jtfDataTo;
	private JLabel labelColorFrom;
	private JLabel labelColorTo;
	private JLabel labelColorStep;
	private JLabel labelDataFrom;
	private JLabel labelDataTo;
	private JLabel labelHueRange;
	private JLabel labelSatRange;
	private JLabel labelBriRange;
	private JLabel labelAlphaRange;
	private Panel_ColorPicker pickerFrom; final static String ID_PickerFrom = "FROM";
	private Panel_ColorPicker pickerTo; final static String ID_PickerTo = "TO";
	private Panel_ColorPicker pickerMissing; final static String ID_PickerMissing = "MISSING";
	final static String[] asFactor = {
		"varies", "00", "10", "20", "30", "40", "50", "60", "70", "80", "90",
		"A0", "B0", "C0", "D0", "E0", "F0", "FF" };
	private JComboBox jcbHue = new JComboBox(asFactor);
	private JComboBox jcbSat = new JComboBox(asFactor);
	private JComboBox jcbBri = new JComboBox(asFactor);
	private JComboBox jcbAlpha = new JComboBox(asFactor);
	private boolean mzSystemUpdating = false;
	private JTextField jtfMissing;
	private JPanel panelMissing;
	private JPanel panelRange;

	Panel_RangeEditor(IRangeChanged owner){
		mOwner = owner;

		// missing
		JLabel labelMissingValues = new JLabel("Missing Values:", JLabel.RIGHT);
		jtfMissing = new JTextField();
		JLabel labelMissingColor = new JLabel("Missing Color:", JLabel.RIGHT);
		pickerMissing = new Panel_ColorPicker(this, this.ID_PickerMissing, 0xFF0000FF, true);
		panelMissing = new JPanel();
		panelMissing.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.BOTH;

//		// top margin
//		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
//		panelMissing.add(Box.createVerticalStrut(15), gbc);

		// Missing - Values
		gbc.gridy = 1;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelMissing.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelMissing.add(labelMissingValues, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelMissing.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelMissing.add(jtfMissing, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelMissing.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelMissing.add(Box.createVerticalStrut(6), gbc);

		// Missing - Color
		gbc.gridy = 3;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelMissing.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelMissing.add(labelMissingColor, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelMissing.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelMissing.add(pickerMissing, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelMissing.add(Box.createHorizontalGlue(), gbc);

		// single value/color
		final JLabel labelSingleValue = new JLabel("Single value:", JLabel.RIGHT);
		final JLabel labelSingleColor = new JLabel("Single color:", JLabel.RIGHT);
		final JPanel panelSingleValue = new JPanel();
		final JPanel panelSingleColor = new JPanel();
		final ButtonGroup bgSingleValue = new ButtonGroup();
		bgSingleValue.add(jrbSingleValue_Yes);
		bgSingleValue.add(jrbSingleValue_No);
		final ButtonGroup bgSingleColor = new ButtonGroup();
		bgSingleColor.add(jrbSingleColor_Yes);
		bgSingleColor.add(jrbSingleColor_No);
		panelSingleValue.setLayout(new BoxLayout(panelSingleValue, BoxLayout.X_AXIS));
		panelSingleValue.add(jrbSingleValue_Yes);
		panelSingleValue.add(jrbSingleValue_No);
		panelSingleColor.setLayout(new BoxLayout(panelSingleColor, BoxLayout.X_AXIS));
		panelSingleColor.add(jrbSingleColor_Yes);
		panelSingleColor.add(jrbSingleColor_No);

		// standard range
		mlabelRangeSwatch = new JLabel("", JLabel.RIGHT);
		mlabelRangeSwatch.setFont(Styles.fontSansSerifBold12);
		mlabelSwatch = new JLabel();
		labelColorFrom = new JLabel("Color From:", JLabel.RIGHT);
		labelColorTo = new JLabel("Color To:", JLabel.RIGHT);
		labelColorStep = new JLabel("Color Step:", JLabel.RIGHT);
		labelDataFrom = new JLabel("Data From:", JLabel.RIGHT);
		labelDataTo = new JLabel("Data To:", JLabel.RIGHT);
		JLabel labelAlpha = new JLabel("Alpha:", JLabel.RIGHT);

		JLabel labelHue = new JLabel("Hue:", JLabel.RIGHT);
		JLabel labelSat = new JLabel("Saturation:", JLabel.RIGHT);
		JLabel labelBri = new JLabel("Brightness:", JLabel.RIGHT);
		labelHueRange = new JLabel("");
		labelSatRange = new JLabel("");
		labelBriRange = new JLabel("");
		labelAlphaRange = new JLabel("");
		jtfDataFrom = new JTextField();
		jtfDataTo = new JTextField();
		pickerFrom = new Panel_ColorPicker(this, ID_PickerFrom, 0x00000000, true);
		pickerTo = new Panel_ColorPicker(this, ID_PickerTo, 0x00000000, true);
		vSetupListeners();

		panelRange = new JPanel();
		panelRange.setLayout(new GridBagLayout());
		gbc.fill = gbc.BOTH;

		// top margin
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(5), gbc);

		// Range Swatch
//		gbc.gridy = 1;
//		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
//		panelRange.add(Box.createHorizontalGlue(), gbc);
//		gbc.gridx = 1; gbc.weightx = 0.0;
//		panelRange.add(Box.createHorizontalStrut(5), gbc); // get rid of range swatch
//		gbc.gridx = 2; gbc.weightx = 0.0;
//		panelRange.add(Box.createHorizontalStrut(5), gbc);
//		gbc.gridx = 3; gbc.weightx = 0.0;
//		panelRange.add(Box.createHorizontalStrut(5), gbc); // nothing here anymore
//		gbc.gridx = 4; gbc.weightx = 1;
//		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Single Value
		gbc.gridy = 3;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelSingleValue, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(panelSingleValue, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(4), gbc);

		// Single Color
		gbc.gridy = 5;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelSingleColor, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(panelSingleColor, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Data From
		gbc.gridy = 7;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelDataFrom, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jtfDataFrom, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Data To
		gbc.gridy = 9;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelDataTo, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jtfDataTo, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 10; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Color From
		gbc.gridy = 11;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelColorFrom, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(pickerFrom, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 12; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Color To
		gbc.gridy = 13;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createVerticalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelColorTo, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createVerticalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(pickerTo, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createVerticalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 14; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Color step
		gbc.gridy = 15;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelColorStep, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jcbColorStep, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 16; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Hue
		gbc.gridy = 17;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelHue, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jcbHue, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(labelHueRange, gbc);
		gbc.gridx = 5; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 18; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);


		// Sat
		gbc.gridy = 19;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelSat, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jcbSat, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(labelSatRange, gbc);
		gbc.gridx = 5; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 20; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Bri
		gbc.gridy = 21;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelBri, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jcbBri, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(labelHueRange, gbc);
		gbc.gridx = 5; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 22; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Alpha
		gbc.gridy = 23;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelAlpha, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jcbAlpha, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(labelHueRange, gbc);
		gbc.gridx = 5; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		// bottom margin
		gbc.gridy = 24;
		gbc.gridx = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(15), gbc);

		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Range Editor"));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(panelMissing);
		this.add(panelRange);

	}

	public void setEnabled( boolean z ){
		jcbHue.setEnabled(z);
		jcbSat.setEnabled(z);
		jcbBri.setEnabled(z);
		jcbAlpha.setEnabled(z);
		jtfMissing.setEnabled(z);
		jrbSingleValue_Yes.setEnabled(z);
		jrbSingleValue_No.setEnabled(z);
		jrbSingleColor_Yes.setEnabled(z);
		jrbSingleColor_No.setEnabled(z);
		jcbColorStep.setEnabled(z);
		jtfDataFrom.setEnabled(z);
		jtfDataTo.setEnabled(z);
		pickerFrom.setEnabled(z);
		pickerTo.setEnabled(z);
	}

	private void vSetupListeners(){
		ActionListener listenSingleValue =
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					vUpdateSingleValue();
				}
			};
		KeyListener listenEnter =
	    	new java.awt.event.KeyListener(){
		    	public void keyPressed(java.awt.event.KeyEvent ke){
			    	if( ke.getKeyCode() == ke.VK_ENTER ){
						mOwner.vRangeChanged(mxRange1);
	    			}
		    	}
			    public void keyReleased(java.awt.event.KeyEvent ke){}
				public void keyTyped(java.awt.event.KeyEvent ke){}
			};
		FocusAdapter focus =
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					mOwner.vRangeChanged(mxRange1);
				}
			};
		jrbSingleValue_Yes.addActionListener(listenSingleValue);
		jrbSingleValue_No.addActionListener(listenSingleValue);
		ActionListener listenSingleColor =
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					vUpdateSingleColor();
				}
			};
		jrbSingleColor_Yes.addActionListener(listenSingleColor);
		jrbSingleColor_No.addActionListener(listenSingleColor);
		jtfDataFrom.addFocusListener( focus );
	    jtfDataTo.addFocusListener( focus );
		jtfMissing.addFocusListener( focus );
		jtfDataFrom.addKeyListener(listenEnter);
		jtfDataTo.addKeyListener(listenEnter);
		jtfMissing.addKeyListener(listenEnter);
		ActionListener listenJCB =
			new java.awt.event.ActionListener(){
			    public void actionPerformed( java.awt.event.ActionEvent ae ){
					if( mzSystemUpdating ) return;
					mOwner.vRangeChanged(mxRange1);
				}
			};
		jcbColorStep.addActionListener(listenJCB);
		jcbHue.addActionListener(listenJCB);
		jcbSat.addActionListener(listenJCB);
		jcbBri.addActionListener(listenJCB);
		jcbAlpha.addActionListener(listenJCB);
	}
	void vUpdateSingleValue(){
		if( jrbSingleValue_Yes.isSelected() ){
			labelDataFrom.setText("Data Value:");
			labelDataTo.setVisible(false);
			jtfDataTo.setVisible(false);
			jtfDataTo.setText(jtfDataFrom.getText());
			jrbSingleColor_Yes.setSelected(true);
			jrbSingleColor_Yes.setEnabled(false);
			jrbSingleColor_No.setEnabled(false);
		} else {
			labelDataFrom.setText("Data From:");
			labelDataTo.setVisible(true);
			jtfDataTo.setVisible(true);
			jrbSingleColor_Yes.setEnabled(true);
			jrbSingleColor_No.setEnabled(true);
		}
		vUpdateSingleColor();
	}
	void vUpdateSingleColor(){
		if( mzSystemUpdating ) return;
		if( jrbSingleColor_Yes.isSelected() ){
			labelColorFrom.setText("Color:");
			labelColorTo.setVisible(false);
			pickerTo.setVisible(false);
			pickerTo.setColor(pickerFrom.getHSB());
		} else {
			labelColorFrom.setText("Color From:");
			labelColorTo.setVisible(false);
			pickerTo.setVisible(true);
		}
		mOwner.vRangeChanged( mxRange1 );
	}
	void set(ColorSpecification cs, int xRange1){
		try {
			mzSystemUpdating = true;
			boolean zEnable = (cs != null);
			jrbSingleColor_Yes.setEnabled( zEnable );
			jrbSingleColor_No.setEnabled( zEnable );
			jrbSingleValue_Yes.setEnabled( zEnable );
			jrbSingleValue_No.setEnabled( zEnable );
			jtfDataFrom.setEnabled( zEnable );
			jtfDataTo.setEnabled( zEnable );
			pickerFrom.setEnabled( zEnable );
			pickerTo.setEnabled( zEnable );
			jcbColorStep.setEnabled( zEnable );
			jcbHue.setEnabled( zEnable );
			jcbSat.setEnabled( zEnable );
			jcbBri.setEnabled( zEnable );
			jcbAlpha.setEnabled( zEnable );
			if( cs == null ){
				mlabelRangeSwatch.setText("[no color specification active]");
				jtfDataFrom.setText( "" );
				jtfDataTo.setText( "" );
				pickerFrom.setColor( 0xFF00007F ); // 50% gray
				pickerTo.setColor( 0xFF00007F ); // 50% gray
				jcbColorStep.setSelectedIndex(-1);
				int iHue = -1;
				int iSaturation = -1;
				int iBrightness = -1;
				int iAlpha = -1;
				vUpdateCombo( jcbHue, iHue );
				vUpdateCombo( jcbSat, iSaturation );
				vUpdateCombo( jcbBri, iBrightness );
				vUpdateCombo( jcbAlpha, iAlpha );
				vUpdateColorComponentRanges(0, 0, 0, 0, 0, 0);
			} else {
				mxRange1 = xRange1;
				if( mxRange1 == 0 ){
					panelMissing.setVisible(true);
					panelRange.setVisible(false);
				} else {
					panelMissing.setVisible(false);
					panelRange.setVisible(true);
				}
				jtfMissing.setText(cs.getMissingString());
				pickerMissing.setColor(cs.getColorMissingHSB());
				mlabelRangeSwatch.setText("Range " + mxRange1 + ":");
				jtfDataFrom.setText( cs.getDataFromS(xRange1) );
				jtfDataTo.setText( cs.getDataToS(xRange1) );
				pickerFrom.setColor(cs.getColorFromHSB(xRange1));
				pickerTo.setColor(cs.getColorToHSB(xRange1));
				int iColorStep = cs.getColorStep(xRange1);
				jcbColorStep.setSelectedIndex(cs.getColorStep(xRange1));
				int hsbColorFrom = cs.getColorFromHSB(xRange1);
				int hsbColorTo = cs.getColorFromHSB(xRange1);
				int iHue = cs.getHue(xRange1);
				int iSaturation = cs.getSaturation(xRange1);
				int iBrightness = cs.getBrightness(xRange1);
				int iAlpha = cs.getAlpha(xRange1);
				vUpdateCombo( jcbHue, iHue );
				vUpdateCombo( jcbSat, iSaturation );
				vUpdateCombo( jcbBri, iBrightness );
				vUpdateCombo( jcbAlpha, iAlpha );
				jrbSingleValue_Yes.setSelected( cs.isSingleValue(xRange1) );
				jrbSingleColor_Yes.setSelected( cs.isSingleColor(xRange1) );
				jrbSingleValue_No.setSelected( !cs.isSingleValue(xRange1) );
				jrbSingleColor_No.setSelected( !cs.isSingleColor(xRange1) );
				vUpdateSingleValue();
				vUpdateColorComponentRanges(iHue, iSaturation, iBrightness, iAlpha, hsbColorFrom, hsbColorTo);
			}
		} finally {
			mzSystemUpdating = false;
		}
	}
	private final void vUpdateColorComponentRanges(int iHue, int iSat, int iBri, int iAlpha, int hsbColorFrom, int hsbColorTo ){
		if( iHue == -1 ){
			int iHueFrom   = (int)(hsbColorFrom & 0x00FF0000) >> 16;
			int iHueTo     = (int)(hsbColorTo & 0x00FF0000) >> 16;
			StringBuffer sbRange = new StringBuffer();
			sbRange.append(Utility_String.sToHex(iHueFrom, 2)).append("-").append(Utility_String.sToHex(iHueTo, 2));
		} else {
			labelHueRange.setText("");
		}
		if( iSat == -1 ){
			int iSatFrom   = ((int)hsbColorFrom & 0x0000FF00) >> 8;
			int iSatTo     = ((int)hsbColorTo & 0x0000FF00) >> 8;
			StringBuffer sbRange = new StringBuffer();
			sbRange.append(Utility_String.sToHex(iSatFrom, 2)).append("-").append(Utility_String.sToHex(iSatTo, 2));
		} else {
			labelHueRange.setText("");
		}
		if( iBri == -1 ){
			int iBriFrom   = ((int)hsbColorFrom & 0x000000FF);
			int iBriTo     = ((int)hsbColorTo & 0x000000FF);
			StringBuffer sbRange = new StringBuffer();
			sbRange.append(Utility_String.sToHex(iBriFrom, 2)).append("-").append(Utility_String.sToHex(iBriTo, 2));
		} else {
			labelHueRange.setText("");
		}
		if( iAlpha == -1 ){
			int iAlphaFrom = (int)((hsbColorFrom & 0xFF000000L) >> 24);
			int iAlphaTo   = (int)((hsbColorTo & 0xFF000000L) >> 24);
			StringBuffer sbRange = new StringBuffer();
			sbRange.append(Utility_String.sToHex(iAlphaFrom, 2)).append("-").append(Utility_String.sToHex(iAlphaTo, 2));
		} else {
			labelHueRange.setText("");
		}
	}
	private final void vUpdateCombo( JComboBox jcb, int iValue ){
		if( iValue == -1 ){ // varies
			jcb.setSelectedIndex(0);
			return;
		}
		if( iValue == 0xFF ){
			jcb.setSelectedIndex(17);
			return;
		}
		if( iValue < 0 || iValue > 0xFF ) return; // internal error has occurred
		jcb.setSelectedIndex(iValue/0x10 + 1);
	}
	private int getComboValue( JComboBox jcb ){
		int x = jcb.getSelectedIndex();
		if( x == -1 || x == 0 ) return -1;
		if( x >= 17 ) return 0xFF;
		return (x - 1)* 0x10;
	}
	public void vColorChanged( String sID, int iHSB, int iRGB, int iHue, int iSat, int iBri ){
		if( mzSystemUpdating ) return;
		mOwner.vRangeChanged( mxRange1 );
	}
	public String getMissingValuesText(){ return jtfMissing.getText(); }
	public void setMissingValuesText(String sText){ jtfMissing.setText(sText); }
	public String getDataFromString(){ return jtfDataFrom.getText(); }
	public String getDataToString(){
		if( jrbSingleValue_Yes.isSelected() ){
			return jtfDataFrom.getText();
		} else {
			return jtfDataTo.getText();
		}
	}
	public int getColorMissing(){ return pickerMissing.getHSB(); }
	public int getColorFromHSB(){ return pickerFrom.getHSB(); }
	public int getColorToHSB(){
		if( jrbSingleColor_Yes.isSelected() ){
			return pickerFrom.getHSB();
		} else {
			return pickerTo.getHSB();
		}
	}
	public int getColorStep(){
		int iSelectedIndex = jcbColorStep.getSelectedIndex();
		return iSelectedIndex; // currently there is a 1:1 relationship between the enum values of the colorstep constants (see ColorSpecification) and the combo box index
	}
	public int getAlpha(){ return getComboValue( jcbAlpha ); }
	public int getHue(){ return getComboValue( jcbHue ); }
	public int getSat(){ return getComboValue( jcbSat ); }
	public int getBri(){ return getComboValue( jcbBri ); }
}

class Panel_ColorPicker extends JPanel {
	IColorChanged mOwner;
	String msID;
	JButton buttonColor;
	JLabel labelColor;
	int miHSB;
	JDialog jd;
	ColorPicker_HSB mHSBpicker;
	JOptionPane jop;
	Panel_ColorPicker(IColorChanged owner, String sID, int iHSB, boolean zShowValue){
		mOwner = owner;
		msID = sID;
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		buttonColor = new JButton();
		labelColor = new JLabel();
		this.add(buttonColor);
		if( zShowValue ){
			this.add(Box.createHorizontalStrut(6));
			this.add(labelColor);
		}
		mHSBpicker = new ColorPicker_HSB();
		jop = new JOptionPane(mHSBpicker, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		jd = jop.createDialog(ApplicationController.getInstance().getAppFrame(), "Choose Color");
		buttonColor.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					mHSBpicker.setColor(miHSB);
					jd.setVisible( true );
					Object oValue = jop.getValue();
					if( oValue == null || oValue.toString().equals("2")){ // todo figure this out
						return; // cancel
					} else {
						int iHSB = mHSBpicker.getHSB();
						setColor( iHSB );
						mOwner.vColorChanged(msID, iHSB, 0, 0, 0, 0);
					}
				}
			}
		);
		setColor( iHSB );
	}
	public void setEnabled( boolean z ){
		buttonColor.setEnabled(z);
	}
	private StringBuffer sbColorText = new StringBuffer();
	void setColor( int iHSB ){
		miHSB = iHSB;
		buttonColor.setIcon(getColorIcon(miHSB));
		sbColorText.setLength(0);
		sbColorText.append(Utility_String.sToHex(getAlpha(), 2));
		sbColorText.append(' ');
		sbColorText.append(Utility_String.sToHex(getHue(), 2));
		sbColorText.append(' ');
		sbColorText.append(Utility_String.sToHex(getSat(), 2));
		sbColorText.append(' ');
		sbColorText.append(Utility_String.sToHex(getBri(), 2));
		sbColorText.append(' ');
		labelColor.setText( sbColorText.toString() );
	}
	int getHSB(){ return miHSB; }
	int getRGB(){ return Color_HSB.iHSBtoRGBA(miHSB); }
	int getAlpha(){ return miHSB >> 24; }
	int getHue(){ return (miHSB & 0x00FF0000) >> 16; }
	int getSat(){ return (miHSB & 0x0000FF00) >> 8; }
	int getBri(){ return miHSB & 0x000000FF; }
	ImageIcon getColorIcon( int iHSB ){ // todo render yourself
		BufferedImage bi = new BufferedImage( 20, 15, BufferedImage.TYPE_INT_ARGB ); // smaller buffer just used to get sizes
		Graphics2D g2 = (Graphics2D)bi.getGraphics();
		g2.setColor(Color.BLACK);
		g2.drawString("alpha", 2, 2);
		Color color = new Color( Color_HSB.iHSBtoRGBA(iHSB));
		g2.setColor(color);
		g2.fillRect(0, 0, 20, 15);
		return new javax.swing.ImageIcon(bi);
	}
}

interface IRangeChanged {
	void vRangeChanged( int iRangeIndex1 );
}

interface IColorChanged {
	void vColorChanged( String sID, int iHSB, int iRGB, int iHue, int iSat, int iBri );
}
