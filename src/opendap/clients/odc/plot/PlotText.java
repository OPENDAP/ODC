package opendap.clients.odc.plot;

/**
 * Title:        PlotLayout
 * Description:  Support for defining text and other annotations to plots
 * Copyright:    Copyright (c) 2003
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.40
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Styles;
import opendap.clients.odc.Utility;
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
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;

public class PlotText extends javax.swing.AbstractListModel {

	// list model interface
	public int getSize(){ return mlistText.size(); }
	public Object getElementAt(int index){ return mlistText.get(index); }

	private ArrayList mlistText = new ArrayList(); // contains PlotTextItems
	PlotText(){}
	PlotTextItem newItem(String sID){
		PlotTextItem item = new PlotTextItem(sID);
		mlistText.add(item);
		int xNewItem = getSize()-1;
		fireIntervalAdded(this, xNewItem, xNewItem);
		return item;
	}
	void removeAll(){
		if( getSize() == 0 ) return; // nothing to remove
		int xLastItem = getSize() - 1;
		mlistText.clear();
		fireIntervalRemoved(this, 0, xLastItem);
	}
	void remove( int xItemToRemove0 ){
		mlistText.remove(xItemToRemove0);
		fireIntervalRemoved(this, xItemToRemove0, xItemToRemove0);
	}
	void remove( String sID_ToRemove ){
		for( int xItem = 0; xItem < mlistText.size(); xItem++ ){
			PlotTextItem item = (PlotTextItem)mlistText.get(xItem);
			if( Utility.equalsUPPER( item.getID(), sID_ToRemove ) ){
				mlistText.remove(xItem);
				fireIntervalRemoved(this, xItem, xItem);
			}
		}
	}
	PlotTextItem get(int index0){
		return (PlotTextItem)mlistText.get(index0);
	}
	PlotTextItem getNew(String sID){
		for( int xItem = 0; xItem < mlistText.size(); xItem++ ){
			PlotTextItem item = (PlotTextItem)mlistText.get(xItem);
			if( Utility.equalsUPPER( item.getID(), sID ) ) return item;
		}
		return newItem( sID );
	}
	void vFireItemChanged(int index0){
		this.fireContentsChanged(this, index0, index0);
	}
}

class PlotTextItem {
	static Font[] FONT_Defaults = new Font[7];
	static {
		FONT_Defaults[0] = Styles.fontFixed12;
		FONT_Defaults[PlotLayout.OBJECT_Canvas] = Styles.fontSansSerif14;
		FONT_Defaults[PlotLayout.OBJECT_Plot] = Styles.fontSansSerif14;
		FONT_Defaults[PlotLayout.OBJECT_AxisHorizontal] = Styles.fontSansSerif12;
		FONT_Defaults[PlotLayout.OBJECT_AxisVertical] = Styles.fontSansSerif12;
		FONT_Defaults[PlotLayout.OBJECT_Legend] = Styles.fontSansSerif10;
		FONT_Defaults[PlotLayout.OBJECT_Scale] = Styles.fontSansSerif8;
	}
	static Color COLOR_Default = Color.BLACK;
	String msID;
	String msExpression;
	final PlotLayout mLayout = new PlotLayout(0);
	Font mFont;
	Color mColor;
	int mColor_HSB = 0xFFFFFF00;
	PlotTextItem(String sID){
		msID = sID;
		msExpression = new String("[new item]");
		mFont = null;
		mColor = null;
	}
	String getID(){ return msID; }
	String getExpression(){ return msExpression; }
	PlotLayout getPlotLayout(){ return mLayout; }
	Font getFont(){ if( mFont == null ) return FONT_Defaults[mLayout.getObject()]; else return mFont; }
	Color getColor(){ if( mColor == null ) return COLOR_Default; else return mColor; }
	int getColor_HSB(){ return mColor_HSB; }
	void setExpression( String s ){
		if( s == null ) msExpression = ""; else msExpression = s;
	}
	void setFont( Font f ){ mFont = f; }
	void setColor( int hsb ){
		mColor_HSB = hsb;
		mColor = new Color(ColorSpecification.iHSBtoRGBA(hsb), true);
	}
	public String toString(){ return getTextValue(); }
	String getTextValue(){
		return msExpression;
	}
}

class Panel_PlotText extends JPanel {
	PlotText mPlotText = null;
	Panel_PlotLayout mTextLayoutPanel = new Panel_PlotLayout(Panel_PlotLayout.MODE_Full, null);
	FontPicker mTextFontPanel = new FontPicker();
	JList mjlistText = new JList();
	JTextField jtfExpression = new JTextField();
	javax.swing.DefaultListModel modelBlank = new javax.swing.DefaultListModel();
	JButton buttonAdd = new JButton("Add");
	JButton buttonDel = new JButton("Remove");
	Panel_PlotText(){
		javax.swing.JScrollPane jspList = new javax.swing.JScrollPane(mjlistText);
		JLabel labText = new JLabel("Text Annotations");
		buttonAdd.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					addPlotItem();
				}
			}
		);
		buttonDel.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					int xSelected0 = mjlistText.getSelectedIndex();
					if( xSelected0 == -1 ) return; // nothing selected
					removePlotItem( xSelected0 );
				}
			}
		);
		JPanel panelButtons = new JPanel();
		panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.X_AXIS));
		panelButtons.add(buttonAdd);
		panelButtons.add(Box.createHorizontalStrut(2));
		panelButtons.add(buttonDel);
		vSetupListeners();

		JPanel panelList = new JPanel();
		panelList.setLayout(new BoxLayout(panelList, BoxLayout.Y_AXIS));
		panelList.add(labText);
		panelList.add(Box.createVerticalStrut(2));
		panelList.add(jspList);
		panelList.add(Box.createVerticalStrut(2));
		panelList.add(panelButtons);

		JPanel panelExpression = new JPanel();
		JLabel labExpression = new JLabel("Expression:");
		JButton buttonExpression = new JButton("update");
		buttonExpression.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					// no need to do anything since jtf has focus lost event
				}
			}
		);
		panelExpression.setLayout(new BoxLayout(panelExpression, BoxLayout.X_AXIS));
		panelExpression.add(labExpression);
		panelExpression.add(Box.createVerticalStrut(2));
		panelExpression.add(jtfExpression);
		panelExpression.add(Box.createVerticalStrut(2));
		panelExpression.add(buttonExpression);

		mTextFontPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Font"));

		// Do layout
		this.setLayout(new java.awt.GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.BOTH;

		// top margin
		gbc.gridy = 0;
		gbc.gridx = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 2; gbc.gridheight = 1;
		add(Box.createVerticalStrut(15), gbc);

		// list
		gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.gridheight = 4;
		gbc.weightx = 1; gbc.weighty = 1;
		add(panelList, gbc);

		// fields
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 0; gbc.gridwidth = 1; gbc.gridheight = 1;
		add(panelExpression, gbc);
		gbc.gridx = 1; gbc.gridy = 2;
		add(Box.createVerticalStrut(4), gbc);
		gbc.gridx = 1; gbc.gridy = 3;
		add(mTextLayoutPanel, gbc);
		gbc.gridx = 1; gbc.gridy = 4;
		add(mTextFontPanel, gbc);

		// bottom margin
		gbc.gridy = 5;
		gbc.gridx = 0; gbc.weightx = 0.0; gbc.weighty = 1; gbc.gridwidth = 2; gbc.gridheight = 1;
		add(Box.createVerticalStrut(15), gbc);
	}
	void addPlotItem(){
		if( mPlotText == null ){
			ApplicationController.vShowWarning("Attempt to add plot item to non-existent model");
			return;
		}
		PlotTextItem item = mPlotText.newItem(null);
		item.setExpression("[new text]");
	}
	void removePlotItem( int index0 ){
		if( mPlotText == null ){
			ApplicationController.vShowWarning("Attempt to remove plot item from non-existent model");
			return;
		}
		mPlotText.remove(index0);
	}
	void setPlotText( PlotText pt ){
		mPlotText = pt;
		if( pt == null ){
			mjlistText.setModel(modelBlank);
			buttonAdd.setEnabled(false);
			buttonDel.setEnabled(false);
		} else {
			mjlistText.setModel(mPlotText);
			buttonAdd.setEnabled(true);
			buttonDel.setEnabled(true);
		}
	}
	void setPlotItem( int index0 ){
		if( mPlotText == null ) return;
		if( index0 < 0 || index0 >= mPlotText.getSize() ){
			mTextLayoutPanel.setPlotLayout(null);
			return;
		}
		PlotTextItem item = mPlotText.get(index0);
		this.jtfExpression.setText(item.getExpression());
		mTextLayoutPanel.setPlotLayout(item.getPlotLayout());
		mTextFontPanel.setTextItem(item);
	}
	PlotTextItem getPlotItem( int index0 ){
		if( mPlotText == null ) return null;
		if( index0 < 0 || index0 >= mPlotText.getSize() ){
			return null;
		}
		return mPlotText.get(index0);
	}
	private void vSetupListeners(){
		mjlistText.addListSelectionListener(
			new javax.swing.event.ListSelectionListener(){
				public void valueChanged(javax.swing.event.ListSelectionEvent e) {
					Panel_PlotText.this.setPlotItem( mjlistText.getSelectedIndex() );
				}
			}
		);
		jtfExpression.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					vUpdateExpression();
				}
			}
		);
		jtfExpression.addKeyListener(
	    	new java.awt.event.KeyListener(){
		    	public void keyPressed(java.awt.event.KeyEvent ke){
			    	if( ke.getKeyCode() == ke.VK_ENTER ){
						vUpdateExpression();
	    			}
		    	}
			    public void keyReleased(java.awt.event.KeyEvent ke){}
				public void keyTyped(java.awt.event.KeyEvent ke){}
			}
		);
	}
	void vUpdateExpression(){
		int indexSelected =  mjlistText.getSelectedIndex();
		PlotTextItem item;
		if( indexSelected < 0 ){ // if nothing is selected (eg box is empty) add a new item
			item = mPlotText.newItem(null);
		} else {
			item = Panel_PlotText.this.getPlotItem(indexSelected);
		}
		if( item == null ) return; // todo
		item.setExpression( jtfExpression.getText() );
		this.mPlotText.vFireItemChanged( indexSelected );
		this.mTextFontPanel.vUpdateFields(); // must update example label
		this.mjlistText.setSelectedValue(item, true);
	}
}

class FontPicker extends JPanel implements IColorChanged {
	PlotTextItem mTextItem = null;
	JComboBox jcbFont;
	JComboBox jcbSize;
	JCheckBox jcheckBold = new JCheckBox("Bold");
	JCheckBox jcheckItalic = new JCheckBox("Italic");
	Panel_ColorPicker mColorPicker;
	final static String[] AS_FontSizes = { "4", "5", "6", "7", "8", "9", "10", "11", "12", "14", "16", "18", "24", "32", "36", "48", "56", "64" };
	JLabel labelExample = new JLabel();
	FontPicker(){
		jcbFont = new JComboBox(Utility.getFontFamilies());
		jcbSize = new JComboBox(AS_FontSizes);
		jcbSize.setSelectedItem("10"); // default size
		mColorPicker = new Panel_ColorPicker(this, "", 0xFFFFFF00, true);
		JLabel labFont = new JLabel("Family:", JLabel.RIGHT);
		JLabel labSize = new JLabel("Size:", JLabel.RIGHT);
		vSetupListeners();
		setTextItem(null);

		labelExample.setOpaque(true);
		labelExample.setBackground(Color.WHITE);

		setLayout(new java.awt.GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.NONE;

		// separator
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6;
		add(Box.createVerticalStrut(4), gbc);

		// Font & Size
		JPanel panelFont = new JPanel();
		panelFont.setLayout(new BoxLayout(panelFont, BoxLayout.X_AXIS));
		panelFont.add(Box.createHorizontalStrut(10));
		panelFont.add(labFont);
		panelFont.add(Box.createHorizontalStrut(3));
		panelFont.add(jcbFont);
		panelFont.add(Box.createHorizontalStrut(3));
		panelFont.add(jcbSize);
		panelFont.add(Box.createHorizontalGlue());
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 3; gbc.gridheight = 1;
		add(panelFont, gbc);

		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 3;
		add(Box.createVerticalStrut(4), gbc);

		// Bold / Italic
		JPanel panelAttributes = new JPanel();
		panelAttributes.setLayout(new BoxLayout(panelAttributes, BoxLayout.X_AXIS));
		panelAttributes.add( jcheckBold );
		panelAttributes.add( Box.createHorizontalStrut(3) );
		panelAttributes.add( jcheckItalic );
		gbc.gridwidth = 1; gbc.gridheight = 1;
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 0.0;
		add(Box.createHorizontalStrut(10), gbc);
		gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.0;
		add(panelAttributes, gbc);
		gbc.gridx = 2; gbc.gridy = 3; gbc.weightx = 1;
		add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 1; gbc.weighty = 0; gbc.gridwidth = 3;
		add(Box.createVerticalStrut(5), gbc);

		// color
		gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 1;
		add(Box.createHorizontalStrut(10), gbc);
		gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 0.0;
		add(mColorPicker, gbc);
		gbc.gridx = 3; gbc.gridy = 5; gbc.weightx = 1;
		add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 1; gbc.weighty = 0; gbc.gridwidth = 3;
		add(Box.createVerticalStrut(5), gbc);

		// example
		gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 3; gbc.gridheight = 1;
		add( labelExample, gbc );

	}
	void setTextItem( PlotTextItem item ){
		mTextItem = item;
		vUpdateFields();
	}
	void vUpdateFields(){
		boolean zEnable = false;
		if( mTextItem == null ){
			labelExample.setText("");
		} else {
			zEnable = true;
			Font font = mTextItem.getFont();
			if( font == null ) font = Styles.fontSansSerif10;
			String sFamily = font.getFamily();
			String[] asFamily = Utility.getFontFamilies();
			int xSelectedFont = 0;
			int xSansSerif = 0;
			for( int xFont = 0; xFont < asFamily.length; xFont++ ){
				if( asFamily[xFont].equalsIgnoreCase("SansSerif") ) xSansSerif = xFont;
				if( asFamily[xFont].equalsIgnoreCase(sFamily) ) xSelectedFont = xFont;
			}
			if( xSelectedFont == 0 ) xSelectedFont = xSansSerif;
			jcbFont.setSelectedIndex(xSelectedFont);
			jcbSize.setSelectedItem(Integer.toString(font.getSize()));
			jcheckBold.setSelected(font.isBold());
			jcheckItalic.setSelected(font.isItalic());
			mColorPicker.setColor(mTextItem.getColor_HSB());
			labelExample.setText(mTextItem.getExpression());
			labelExample.setFont(font);
			labelExample.setForeground(mTextItem.getColor());
			int hsb = mTextItem.getColor_HSB();
			int iSaturation = (hsb & 0x0000FF00) >> 8;
			int iBrightness = (hsb & 0x000000FF);
			if( iSaturation < 0x20 && iBrightness > 0xA0)
			    labelExample.setBackground(Color.BLACK);
		    else
			    labelExample.setBackground(Color.WHITE);
		}
		jcbFont.setEnabled(zEnable);
		jcbSize.setEnabled(zEnable);
		jcheckBold.setEnabled(zEnable);
		jcheckItalic.setEnabled(zEnable);
		mColorPicker.setEnabled(zEnable);
	}
	public void vColorChanged( String sID, int iHSB, int iRGB, int iHue, int iSat, int iBri ){
		mTextItem.setColor( mColorPicker.getHSB() );
		vUpdateFields();
	}
	public void vProportionalSwap(){} // do nothing
	private void vSetFont(){
		try {
			String sFamily = jcbFont.getSelectedItem().toString();
			int eSTYLE = Font.PLAIN | (jcheckBold.isSelected() ? Font.BOLD : 0) | (jcheckItalic.isSelected() ? Font.ITALIC : 0);
			int iSize = Integer.parseInt( jcbSize.getSelectedItem().toString() );
			Font font = new Font(sFamily, eSTYLE, iSize);
			if( font == null ){
				ApplicationController.vShowError("Failed to set font to: " + sFamily + " " + eSTYLE + " " + iSize);
			}
			if( mTextItem == null ){
				ApplicationController.vShowError("Internal Error, attempt to assign font to non-existent text item");
			} else {
				mTextItem.setFont(font);
			}
			vUpdateFields();
		} catch(Exception ex) {
			ApplicationController.vShowError("Unexpected error setting font: " + ex);
		}
	}
	private void vSetupListeners(){
		jcbFont.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					vSetFont();
				}
			}
		);
		jcbSize.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					vSetFont();
				}
			}
		);
		jcheckBold.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					vSetFont();
				}
			}
		);
		jcheckItalic.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					vSetFont();
				}
			}
		);
	}
}

