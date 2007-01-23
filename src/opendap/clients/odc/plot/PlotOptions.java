package opendap.clients.odc.plot;

/**
 * Title:        PlotOptions
 * Description:  Support for the various plot options
 * Copyright:    Copyright (c) 2003-4
 * Company:      OPeNDAP
 * @author       John Chamberlain
 * @version      2.45
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Styles;
import java.util.ArrayList;
import java.awt.Font;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.border.*;
import javax.swing.JCheckBox;

public class PlotOptions extends javax.swing.AbstractListModel {

	final public static int DEFAULT_HistogramClassCount = 200;

	// graphically defined values for legend
	boolean    option_legend_zShow  = true;
	int        option_legend_iSize  = 0;
	String     option_legend_sLabel = null;
	boolean    option_legend_zDoBiasAdjustment = false;
	double     option_legend_dBiasSlope = 1;
	double     option_legend_dBiasIntercept = 0;
	final PlotLayout option_legend_Layout = new PlotLayout( PlotLayout.DEFAULT_STYLE_Legend );

	// graphically defined values for scale
	boolean    option_scale_zShow       = false;
	int        option_scale_iXLength    = 0;
	int        option_scale_iRealLength = 0;
	String     option_scale_sLabel      = null;
	final PlotLayout option_scale_Layout      = new PlotLayout( PlotLayout.DEFAULT_STYLE_Scale );

	private ArrayList mlistText = new ArrayList(); // contains PlotOptionItems

	// list model interface
	public int getSize(){ return mlistText.size(); }
	public Object getElementAt(int index){ return mlistText.get(index); }

	public final static int OPTION_AutoCalcMissing = 0;
	public final static int OPTION_HistogramClassCount = 1;
	public final static int OPTION_Boxed = 2;
	public final static int OPTION_VectorSize = 3;
	public final static int OPTION_TextAntiAliasing = 4;
	public final static int OPTION_GraphicAntiAliasing = 5;
	public final static int OPTION_ThumbnailWidth = 6;
	public final static int OPTION_MultisliceDelay = 7;
	public final static int OPTION_MultiplotDelay = 8;
	public final static int OPTION_GenerateAxisCaptions = 9;
	public final static int OPTION_XY_CircleSize = 10;
	public final static int OPTION_XY_ShowLines = 11;
	public final static int OPTION_XY_ShowPoints = 12;
	public final static int OPTION_ShowCoastLine = 13;
	public final static String[] OPTION_Labels = {
		"Auto Calculate Missing",
		"Histogram Class Count",
		"Boxed Plot Area",
		"Sub-Sample Vector Plot by",
		"Text Anti-aliasing",
		"Graphic Anti-aliasing",
		"Thumbnail Width",
		"Multislice Delay (ms)",
		"Multiplot Delay (ms)",
		"Generate Axis Captions",
		"XY Circle Size",
		"XY Show Lines",
		"XY Show Points",
		"Show Coastline"
	};

	PlotOptions(){
		newItem( OPTION_Labels[0], OptionItem.TYPE_YesNo, "Y" ); // auto calc missing values
		newItem( OPTION_Labels[1], OptionItem.TYPE_NonNegativeInteger, DEFAULT_HistogramClassCount ); // add histogram class count
		newItem( OPTION_Labels[2], OptionItem.TYPE_YesNo, "Y" ); // boxed plot
		newItem( OPTION_Labels[3], OptionItem.TYPE_NonNegativeInteger, 10 ); // vector size
		newItem( OPTION_Labels[4], OptionItem.TYPE_YesNo, "Y" ); // text anti-aliasing
		newItem( OPTION_Labels[5], OptionItem.TYPE_YesNo, "Y" ); // text anti-aliasing
		newItem( OPTION_Labels[6], OptionItem.TYPE_NonNegativeInteger, 100 ); // text anti-aliasing
		newItem( OPTION_Labels[7], OptionItem.TYPE_NonNegativeInteger, 150 ); // text anti-aliasing
		newItem( OPTION_Labels[8], OptionItem.TYPE_NonNegativeInteger, 2000 ); // text anti-aliasing
		newItem( OPTION_Labels[9], OptionItem.TYPE_YesNo, "Y" ); // axis captions
		newItem( OPTION_Labels[10], OptionItem.TYPE_NonNegativeInteger, 5 ); // size of circles used for scatter plot
		newItem( OPTION_Labels[11], OptionItem.TYPE_YesNo, "Y" ); // xy show lines
		newItem( OPTION_Labels[12], OptionItem.TYPE_YesNo, "N" ); // xy show points
		newItem( OPTION_Labels[OPTION_ShowCoastLine], OptionItem.TYPE_YesNo, "N" );
	}
	OptionItem newItem(String sItemName, int eTYPE, int iDefaultValue){
		OptionItem item = new OptionItem(sItemName, eTYPE, this);
		item.setValue_text(Integer.toString(iDefaultValue));
		return newItem( item );
	}
	OptionItem newItem(String sItemName, int eTYPE, String s){
		OptionItem item = new OptionItem(sItemName, eTYPE, this);
		item.setValue_text(s);
		return newItem( item );
	}
	OptionItem newItem(OptionItem item){
		mlistText.add(item);
		int xNewItem = getSize()-1;
		this.fireIntervalAdded(this, xNewItem, xNewItem);
		return item;
	}
	void fireItemChanged( OptionItem item ){
		for( int xOption = 1; xOption <= getSize(); xOption++ ){
			if( get(xOption - 1) ==  item ) this.fireContentsChanged(this, xOption, xOption);
		}
	}
	void removeAll(){
		int xLastItem = getSize() - 1;
		mlistText.clear();
		this.fireIntervalRemoved(this, 0, xLastItem);
	}
	void remove( int xItemToRemove0 ){
		mlistText.remove(xItemToRemove0);
		this.fireIntervalRemoved(this, xItemToRemove0, xItemToRemove0);
	}
	OptionItem getOption( int index0 ){
		if( index0 < 0 || index0 >= getSize() ){
			return null;
		}
		return get(index0);
	}
	int getValue_int(int xItem0){
		return get(xItem0).getValue_int();
	}
	boolean getValue_boolean(int xItem0){
		return get(xItem0).getValue_YesNo();
	}
	OptionItem get(int index0){
		if( index0 < 0 ) return null;
		if( index0 >= mlistText.size() ) return null;
	    return (OptionItem)mlistText.get(index0);
	}
	void setItem_boolean(int xItem0, boolean zValue){
		OptionItem item = get( xItem0 );
		if( item == null ){
			ApplicationController.vShowWarning("system error, attempt to set non-existent item");
			return;
		}
		item.setValue_YesNo( zValue );
	}
	public String toString(){
		StringBuffer sb = new StringBuffer(250);
		sb.append("Options:\n");
		for( int xOption = 1; xOption <= getSize(); xOption++ ){
			sb.append("\t").append( get(xOption - 1).toString() ).append("\n");
		}
		return sb.toString();
	}
}

class OptionItem {
	public final static int TYPE_NonNegativeInteger = 1;
	public final static int TYPE_Integer = 2;
	public final static int TYPE_Float = 3;
	public final static int TYPE_String = 4;
	public final static int TYPE_YesNo = 5;
	private int meTYPE;
	private String msName;
	private String mValue_text;
	private int mValue_int;
	private float mValue_float;
	private boolean mValue_YesNo;
	private PlotOptions mParent;
	OptionItem(String sName, int eTYPE, PlotOptions parent){
		mParent = parent;
		msName = sName;
		meTYPE = eTYPE;
	}
	int getType(){ return meTYPE; }
	String getTypeString(){
		switch( meTYPE ){
			case TYPE_NonNegativeInteger: return "Non-Negative Integer";
			case TYPE_Integer: return "Integer";
			case TYPE_Float: return "Float";
			case TYPE_String: return "String";
			case TYPE_YesNo: return "YesNo";
			default: return "?";
		}
	}
	String getValue_text(){
		switch( meTYPE ){
			case TYPE_NonNegativeInteger:
			case TYPE_Integer:
				return Integer.toString( mValue_int );
			case TYPE_Float:
				return Float.toString( mValue_float );
			default:
			case TYPE_String:
				return mValue_text;
			case TYPE_YesNo:
				return mValue_YesNo ? "Yes" : "No";
		}
	}
	int getValue_int(){ return mValue_int; }
	float getValue_float(){ return mValue_float; }
	boolean getValue_YesNo(){ return mValue_YesNo; }
	void setValue_YesNo( boolean z ){
		mValue_YesNo = z;
		mValue_text = z ? "Yes" : "No";
		mParent.fireItemChanged(this);
	}
	void setValue_text( String s ){
		try {
			if( s == null ) mValue_text = ""; else mValue_text = s;
			switch( meTYPE ){
				case TYPE_NonNegativeInteger:
				case TYPE_Integer:
					if( s == null ) mValue_int = 0; mValue_int = Integer.parseInt(s);
					break;
				case TYPE_Float:
					if( s == null ) mValue_int = 0; mValue_float = Float.parseFloat(s);
					break;
				default:
				case TYPE_String:
					mValue_text = s;
					break;
				case TYPE_YesNo:
					if( mValue_text.startsWith("Y") || mValue_text.startsWith("y") || mValue_text.startsWith("T") || mValue_text.startsWith("t") ){
						mValue_YesNo = true;
					} else {
						mValue_YesNo = false;
					}
					break;
			}
			mParent.fireItemChanged(this);
		} catch(Exception ex) {
			ApplicationController.vShowError("Error setting option (" + msName + ") to value " + s + ": " + ex);
		}
	}
	public String toString(){
		String sValue = getValue_text();
		return msName + ": " + sValue;
	}
	public String toString(int iIndent){
		String sValue = getValue_text();
		return msName + " { " + getTypeString() + " } [" + sValue.length() + "]: " + sValue;
	}
}

class Panel_PlotOptions extends JPanel {

	PlotOptions mPlotOptions = null;
	final Panel_PlotLayout mLegendLayoutPanel = new Panel_PlotLayout(Panel_PlotLayout.MODE_NoLegend, null);
	final Panel_PlotLayout mScaleLayoutPanel = new Panel_PlotLayout(Panel_PlotLayout.MODE_NoScale, null);
	final JList jlistOptions = new JList();
	final JPanel panelValue_text = new JPanel();
	final JPanel panelValue_boolean = new JPanel();
	final JTextField jtfValue = new JTextField();
	final JRadioButton jrbValue_yes = new JRadioButton("Yes");
	final JRadioButton jrbValue_no = new JRadioButton("No");
	final javax.swing.DefaultListModel modelBlank = new javax.swing.DefaultListModel();

	final JCheckBox legend_JCheck = new JCheckBox("Show Legend/Colorbar");
	final JTextField legend_jtfLabel = new JTextField(10);
	final JTextField legend_jtfSize = new JTextField(24);
	final JCheckBox legend_checkBias = new JCheckBox("No Adjustment");
	final JTextField legend_jtfBiasSlope = new JTextField(12);
	final JTextField legend_jtfBiasIntercept = new JTextField(12);

	final JCheckBox scale_JCheck = new JCheckBox("Show Scale");
	final JTextField scale_jtfLabel = new JTextField(24);
	final JTextField scale_jtfXLength = new JTextField(12);
	final JTextField scale_jtfRealLength = new JTextField(12);

	Panel_PlotOptions(){

		// options panel
		JPanel panelOptions = new JPanel();
		panelOptions.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options"));
		panelOptions.setLayout(new BoxLayout(panelOptions, BoxLayout.Y_AXIS));
		javax.swing.JScrollPane jspList = new javax.swing.JScrollPane(jlistOptions);

		// value panel
		JButton buttonUpdate = new JButton("update");
		buttonUpdate.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					// no need to do anything since jtf has focus lost event
				}
			}
		);
//		panelValue_text.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Value"));
		panelValue_text.setLayout(new BoxLayout(panelValue_text, BoxLayout.X_AXIS));
		JLabel labValue_text = new JLabel("Text Value:");
		jtfValue.setEnabled(false);
		panelValue_text.add(labValue_text);
		panelValue_text.add(Box.createHorizontalStrut(3));
		panelValue_text.add(jtfValue);
		panelValue_text.add(Box.createHorizontalStrut(3));
		panelValue_text.add(buttonUpdate);
		panelValue_text.setMaximumSize(new java.awt.Dimension(400, 25));

		// yes-no value
		javax.swing.ButtonGroup bgSelectorOptions = new javax.swing.ButtonGroup();
		bgSelectorOptions.add(jrbValue_yes);
		bgSelectorOptions.add(jrbValue_no);
		ActionListener listener_YesNo =
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					vUpdateValue();
				}
			};
		jrbValue_yes.addActionListener(listener_YesNo);
		jrbValue_no.addActionListener(listener_YesNo);
		panelValue_boolean.setLayout(new BoxLayout(panelValue_boolean, BoxLayout.X_AXIS));
		panelValue_boolean.add(Box.createHorizontalStrut(5));
		panelValue_boolean.add(jrbValue_yes);
		panelValue_boolean.add(Box.createHorizontalStrut(3));
		panelValue_boolean.add(jrbValue_no);
		panelValue_boolean.setMaximumSize(new java.awt.Dimension(400, 25));
		panelValue_boolean.setVisible(false);

		panelOptions.add(jspList);
		panelOptions.add(Box.createVerticalStrut(3));
		panelOptions.add(panelValue_text);
		panelOptions.add(panelValue_boolean);

		// legend panel
		legend_checkBias.setSelected(true);
		legend_jtfBiasSlope.setEnabled(false);
		legend_jtfBiasIntercept.setEnabled(false);
		final JPanel panelLegendLabel = new JPanel();
		final JLabel labelLegendLabel = new JLabel("Label:");
		panelLegendLabel.setMaximumSize(new java.awt.Dimension(400, 25));
		panelLegendLabel.setLayout(new BoxLayout(panelLegendLabel, BoxLayout.X_AXIS));
		panelLegendLabel.add(Box.createHorizontalStrut(10));
		panelLegendLabel.add(labelLegendLabel);
		panelLegendLabel.add(Box.createHorizontalStrut(5));
		panelLegendLabel.add(legend_jtfLabel);
		final JLabel labelLegendSize = new JLabel("Size:");
		final JPanel panelLegendSize = new JPanel();
		panelLegendSize.setMaximumSize(new java.awt.Dimension(400, 25));
		panelLegendSize.setLayout(new BoxLayout(panelLegendSize, BoxLayout.X_AXIS));
		panelLegendSize.add(Box.createHorizontalStrut(10));
		panelLegendSize.add(labelLegendSize);
		panelLegendSize.add(Box.createHorizontalStrut(5));
		panelLegendSize.add(legend_jtfSize);
		final JPanel panelLegendAttributes = new JPanel();
		panelLegendAttributes.setLayout(new BoxLayout(panelLegendAttributes, BoxLayout.Y_AXIS));
		panelLegendAttributes.add(legend_JCheck);
		panelLegendAttributes.add(Box.createVerticalStrut(6));
		panelLegendAttributes.add(panelLegendLabel);
		panelLegendAttributes.add(Box.createVerticalStrut(6));
		panelLegendAttributes.add(panelLegendSize);
		final JPanel panelLegendBias_NoAdjustment = new JPanel();
		final JPanel panelLegendBias_Slope = new JPanel();
		final JPanel panelLegendBias_Intercept = new JPanel();
		final JLabel labelLegendBias_Slope = new JLabel("Slope:");
		final JLabel labelLegendBias_Intercept = new JLabel("Intercept:");
		panelLegendBias_NoAdjustment.setLayout(new BoxLayout(panelLegendBias_NoAdjustment, BoxLayout.X_AXIS));
		panelLegendBias_NoAdjustment.setMaximumSize(new java.awt.Dimension(400, 25));
		panelLegendBias_NoAdjustment.add(Box.createHorizontalStrut(10));
		panelLegendBias_NoAdjustment.add(legend_checkBias);
		panelLegendBias_Slope.setLayout(new BoxLayout(panelLegendBias_Slope, BoxLayout.X_AXIS));
		panelLegendBias_Slope.setMaximumSize(new java.awt.Dimension(400, 25));
		panelLegendBias_Slope.add(Box.createHorizontalStrut(10));
		panelLegendBias_Slope.add(labelLegendBias_Slope);
		panelLegendBias_Slope.add(Box.createHorizontalStrut(5));
		panelLegendBias_Slope.add(legend_jtfBiasSlope);
		panelLegendBias_Intercept.setLayout(new BoxLayout(panelLegendBias_Intercept, BoxLayout.X_AXIS));
		panelLegendBias_Intercept.setMaximumSize(new java.awt.Dimension(400, 25));
		panelLegendBias_Intercept.add(Box.createHorizontalStrut(10));
		panelLegendBias_Intercept.add(labelLegendBias_Intercept);
		panelLegendBias_Intercept.add(Box.createHorizontalStrut(5));
		panelLegendBias_Intercept.add(legend_jtfBiasIntercept);
		final JPanel panelLegendBias = new JPanel();
		panelLegendBias.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Linear Bias"));
		panelLegendBias.setLayout(new BoxLayout(panelLegendBias, BoxLayout.Y_AXIS));
		panelLegendBias.add(panelLegendBias_NoAdjustment);
		panelLegendBias.add(Box.createVerticalStrut(4));
		panelLegendBias.add(panelLegendBias_Slope);
		panelLegendBias.add(Box.createVerticalStrut(4));
		panelLegendBias.add(panelLegendBias_Intercept);
		final JPanel panelLegend = new JPanel();
		panelLegend.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Legend/Colorbar"));
		panelLegend.setLayout(new BoxLayout(panelLegend, BoxLayout.Y_AXIS));
		final JPanel panelLegend_top = new JPanel();
		panelLegend_top.setLayout(new BoxLayout(panelLegend_top, BoxLayout.X_AXIS));
		panelLegend_top.add(panelLegendAttributes);
		panelLegend_top.add(panelLegendBias);
		panelLegend.add(panelLegend_top);
		panelLegend.add(Box.createVerticalStrut(5));
		panelLegend.add(mLegendLayoutPanel);

		// scale panel
		final JPanel panelScaleUnits = new JPanel();
		panelScaleUnits.setMaximumSize(new java.awt.Dimension(400, 25));
		final JLabel labelScaleUnits = new JLabel("Label:");
		panelScaleUnits.setLayout(new BoxLayout(panelScaleUnits, BoxLayout.X_AXIS));
		panelScaleUnits.add(Box.createHorizontalStrut(10));
		panelScaleUnits.add(labelScaleUnits);
		panelScaleUnits.add(Box.createHorizontalStrut(5));
		panelScaleUnits.add(scale_jtfLabel);
		final JPanel panelScaleXLength = new JPanel();
		panelScaleXLength.setMaximumSize(new java.awt.Dimension(400, 25));
		final JLabel labelScaleXLength = new JLabel("X-Length:");
		panelScaleXLength.setLayout(new BoxLayout(panelScaleXLength, BoxLayout.X_AXIS));
		panelScaleXLength.add(Box.createHorizontalStrut(10));
		panelScaleXLength.add(labelScaleXLength);
		panelScaleXLength.add(Box.createHorizontalStrut(5));
		panelScaleXLength.add(scale_jtfXLength);
		final JPanel panelScaleRealLength = new JPanel();
		panelScaleRealLength.setMaximumSize(new java.awt.Dimension(400, 25));
		final JLabel labelScaleRealLength = new JLabel("Real Length:");
		panelScaleRealLength.setLayout(new BoxLayout(panelScaleRealLength, BoxLayout.X_AXIS));
		panelScaleRealLength.add(Box.createHorizontalStrut(10));
		panelScaleRealLength.add(labelScaleRealLength);
		panelScaleRealLength.add(Box.createHorizontalStrut(5));
		panelScaleRealLength.add(scale_jtfRealLength);
		JPanel panelScale = new JPanel();
		panelScale.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scale"));
		panelScale.setLayout(new BoxLayout(panelScale, BoxLayout.Y_AXIS));
		JPanel panelScale_top = new JPanel();
		panelScale_top.setLayout(new BoxLayout(panelScale_top, BoxLayout.Y_AXIS));
		panelScale_top.add(scale_JCheck);
		panelScale_top.add(Box.createVerticalStrut(6));
		panelScale_top.add(panelScaleUnits);
		panelScale_top.add(Box.createVerticalStrut(6));
		panelScale_top.add(panelScaleXLength);
		panelScale_top.add(Box.createVerticalStrut(6));
		panelScale_top.add(panelScaleRealLength);
		panelScale.add(panelScale_top);
		panelScale.add(Box.createVerticalStrut(5));
		panelScale.add(mScaleLayoutPanel);
		vSetupListeners();

		// Do layout
		this.setLayout(new java.awt.GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.BOTH;

		// top margin
		gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.0; gbc.weighty = 0.0;
		gbc.gridwidth = 3; gbc.gridheight = 1;
		add(Box.createVerticalStrut(10), gbc);

		// options
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridwidth = 1;
		add(panelOptions, gbc);

		// legend
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridwidth = 1;
		add(panelLegend, gbc);

		// scale
		gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridwidth = 1;
		add(panelScale, gbc);

		// bottom margin
		gbc.gridy = 2; gbc.gridx = 0; gbc.weightx = 0.0; gbc.weighty = 0.0;
		gbc.gridwidth = 3; gbc.gridheight = 1;
		add(Box.createVerticalStrut(10), gbc);

		setEnabled( false );
	}

	PlotOptions getPlotOptions(){ return mPlotOptions; }

	void setPlotOptions( PlotOptions pt ){
		mPlotOptions = pt;
		if( pt == null ){
			jlistOptions.setModel(modelBlank);
			mLegendLayoutPanel.setPlotLayout(null);
			mScaleLayoutPanel.setPlotLayout(null);
			setEnabled( false );
		} else {
			jlistOptions.setModel(mPlotOptions);
			mLegendLayoutPanel.setPlotLayout(mPlotOptions.option_legend_Layout);
			mScaleLayoutPanel.setPlotLayout(mPlotOptions.option_scale_Layout);
			setEnabled( true );
			legend_JCheck.setSelected( pt.option_legend_zShow ); // todo add other fixed options
		}
	}

	public void setEnabled( boolean z ){
		mLegendLayoutPanel.setFieldsEnabled(z);
		mScaleLayoutPanel.setFieldsEnabled(z);
		jlistOptions.setEnabled(z);
		jtfValue.setEnabled(z);
		legend_JCheck.setEnabled(z);
		legend_jtfLabel.setEnabled(z);
		legend_jtfSize.setEnabled(z);
		scale_JCheck.setEnabled(z);
		scale_jtfLabel.setEnabled(z);
		scale_jtfXLength.setEnabled(z);
		scale_jtfRealLength.setEnabled(z);
	}

	void setOption( int index0 ){
		if( mPlotOptions == null ) return;
		if( index0 < 0 || index0 >= mPlotOptions.getSize() ){
			return;
		}
		OptionItem item = mPlotOptions.get(index0);
		if( item.getType() == OptionItem.TYPE_YesNo ){
			panelValue_text.setVisible(false);
			panelValue_boolean.setVisible(true);
			jrbValue_yes.setEnabled(true);
			jrbValue_no.setEnabled(true);
			jrbValue_yes.setSelected(item.getValue_YesNo());
			jrbValue_no.setSelected(!item.getValue_YesNo());
		} else {
			panelValue_text.setVisible(true);
			panelValue_boolean.setVisible(false);
			jtfValue.setText(item.getValue_text());
		}
	}

	OptionItem getOption( int index0 ){
		if( mPlotOptions == null ){
			return null;
		}
		if( index0 < 0 || index0 >= mPlotOptions.getSize() ){
			return null;
		}
		return mPlotOptions.get(index0);
	}

	private void vUpdateValue(){
		OptionItem item = Panel_PlotOptions.this.getOption( jlistOptions.getSelectedIndex() );
		if( item == null ){
			ApplicationController.vShowWarning("no option item selected");
			return;
		}
		if( item.getType() == OptionItem.TYPE_YesNo ){
			item.setValue_YesNo(jrbValue_yes.isSelected());
			jtfValue.setText( item.getValue_text() );
		} else {
			item.setValue_text( jtfValue.getText() );
			jtfValue.setText( item.getValue_text() );
		}
	}

	private void vSetupListeners(){
		jlistOptions.addListSelectionListener(
			new javax.swing.event.ListSelectionListener(){
				public void valueChanged(javax.swing.event.ListSelectionEvent e) {
					int xSelected = jlistOptions.getSelectedIndex();
					if( xSelected < 0 ){
						jtfValue.setEnabled(false);
						jrbValue_yes.setEnabled(false);
						jrbValue_yes.setEnabled(false);
					} else {
						jtfValue.setEnabled(true);
						jrbValue_yes.setEnabled(true);
						jrbValue_yes.setEnabled(true);
						Panel_PlotOptions.this.setOption( xSelected );
					}
				}
			}
		);
		jtfValue.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					vUpdateValue();
				}
			}
		);
		jtfValue.addKeyListener(
	    	new java.awt.event.KeyAdapter(){
		    	public void keyPressed(java.awt.event.KeyEvent ke){
			    	if( ke.getKeyCode() == ke.VK_ENTER ){
						vUpdateValue();
	    			}
		    	}
			}
		);
		legend_JCheck.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					mPlotOptions.option_legend_zShow = legend_JCheck.isSelected();
				}
			}
		);
		legend_jtfLabel.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					mPlotOptions.option_legend_sLabel = legend_jtfLabel.getText();
				}
			}
		);
		legend_jtfSize.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					String sText = legend_jtfSize.getText();
					int iSize;
					if( sText.length() == 0 ){
						mPlotOptions.option_legend_iSize = 0;
						return;
					}
					try {
						iSize = Integer.parseInt(sText);
					} catch(Exception ex) {
						ApplicationController.vShowWarning("unable to interpret legend size (" + sText + ") as a non-negative integer");
						legend_jtfSize.setText( Integer.toString(mPlotOptions.option_legend_iSize) );
						return;
					}
					if( iSize < 0 ){
						ApplicationController.vShowWarning("legend size must be non-negative");
						legend_jtfSize.setText( Integer.toString(mPlotOptions.option_legend_iSize) );
						return;
					}
					mPlotOptions.option_legend_iSize = iSize;
				}
			}
		);
		legend_checkBias.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					boolean zDoBiasAdjustment = !legend_checkBias.isSelected();
					mPlotOptions.option_legend_zDoBiasAdjustment = zDoBiasAdjustment;
					if( zDoBiasAdjustment ){
						legend_jtfBiasSlope.setEnabled(true);
						legend_jtfBiasIntercept.setEnabled(true);
						vValidateBiasSlope();
						vValidateBiasIntercept();
					} else {
						legend_jtfBiasSlope.setEnabled(false);
						legend_jtfBiasIntercept.setEnabled(false);
					}
				}
			}
		);
		legend_jtfBiasSlope.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					vValidateBiasSlope();
				}
			}
		);
		legend_jtfBiasIntercept.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					vValidateBiasIntercept();
				}
			}
		);
		scale_JCheck.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					mPlotOptions.option_scale_zShow = scale_JCheck.isSelected();
				}
			}
		);
		scale_jtfLabel.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					mPlotOptions.option_scale_sLabel = scale_jtfLabel.getText();
				}
			}
		);
		scale_jtfXLength.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					String sText = scale_jtfXLength.getText();
					int iValue;
					if( sText.length() == 0 ){
						mPlotOptions.option_scale_iXLength = 0;
						return;
					}
					try {
						iValue = Integer.parseInt(sText);
					} catch(Exception ex) {
						ApplicationController.vShowWarning("unable to interpret scale x-length (" + sText + ") as a non-negative integer");
						scale_jtfXLength.setText( Integer.toString(mPlotOptions.option_scale_iXLength) );
						return;
					}
					if( iValue < 0 ){
						ApplicationController.vShowWarning("scale x-length must be non-negative");
						scale_jtfXLength.setText( Integer.toString(mPlotOptions.option_scale_iXLength) );
						return;
					}
					mPlotOptions.option_scale_iXLength = iValue;
				}
			}
		);
		scale_jtfRealLength.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					String sText = scale_jtfRealLength.getText();
					int iValue;
					if( sText.length() == 0 ){
						mPlotOptions.option_scale_iRealLength = 0;
						return;
					}
					try {
						iValue = Integer.parseInt(sText);
					} catch(Exception ex) {
						ApplicationController.vShowWarning("unable to interpret scale real length (" + sText + ") as a non-negative integer");
						scale_jtfRealLength.setText( Integer.toString(mPlotOptions.option_scale_iRealLength) );
						return;
					}
					if( iValue < 0 ){
						ApplicationController.vShowWarning("scale real length must be non-negative");
						scale_jtfRealLength.setText( Integer.toString(mPlotOptions.option_scale_iRealLength) );
						return;
					}
					mPlotOptions.option_scale_iRealLength = iValue;
				}
			}
		);

	}

	void vValidateBiasSlope(){
		String sText = legend_jtfBiasSlope.getText();
		double dValue;
		if( sText.length() == 0 ){
			mPlotOptions.option_legend_dBiasSlope = 1d;
			legend_jtfBiasSlope.setText("1");
			return;
		}
		try {
			dValue = Double.parseDouble(sText);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("unable to interpret legend slope (" + sText + ") as a double");
			legend_jtfBiasSlope.setText( Double.toString(mPlotOptions.option_legend_dBiasSlope) );
			return;
		}
		mPlotOptions.option_legend_dBiasSlope = dValue;
	}

	void vValidateBiasIntercept(){
		String sText = legend_jtfBiasIntercept.getText();
		double dValue;
		if( sText.length() == 0 ){
			mPlotOptions.option_legend_dBiasIntercept = 1d;
			legend_jtfBiasIntercept.setText("1");
			return;
		}
		try {
			dValue = Double.parseDouble(sText);
		} catch(Exception ex) {
			ApplicationController.vShowWarning("unable to interpret legend intercept (" + sText + ") as a double");
			legend_jtfBiasIntercept.setText( Double.toString(mPlotOptions.option_legend_dBiasIntercept) );
			return;
		}
		mPlotOptions.option_legend_dBiasIntercept = dValue;
	}
}



