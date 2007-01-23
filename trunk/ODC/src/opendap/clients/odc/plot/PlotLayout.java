package opendap.clients.odc.plot;

/**
 * Title:        PlotLayout
 * Description:  Support for defining color ranges
 * Copyright:    Copyright (c) 2003
 * Company:      OPeNDAP
 * @author       John Chamberlain
 * @version      2.43
 */

import opendap.clients.odc.Utility;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.geom.AffineTransform;

class PlotLayout {

	public final static int OBJECT_Canvas = 1;
	public final static int OBJECT_Plot = 2;
	public final static int OBJECT_AxisHorizontal = 3;
	public final static int OBJECT_AxisVertical = 4;
	public final static int OBJECT_Legend = 5;
	public final static int OBJECT_Scale = 6;
	public final static String[] AS_Object = { "Canvas", "Plot", "Axis Horizontal", "Axis Vertical", "Legend", "Scale" };
	public final static String[] AS_Object_NoScale = { "Canvas", "Plot", "Axis Horizontal", "Axis Vertical", "Legend" };
	public final static String[] AS_Object_NoLegend = { "Canvas", "Plot", "Axis Horizontal", "Axis Vertical" };
	String getObject_String(int e){
		if( e < 1 || e > AS_Object.length ) e = 1;
		return AS_Object[e-1];
	}

	public final static int ORIENT_TopLeft = 1;
	public final static int ORIENT_TopMiddle = 2;
	public final static int ORIENT_TopRight = 3;
	public final static int ORIENT_BottomLeft = 4;
	public final static int ORIENT_BottomMiddle = 5;
	public final static int ORIENT_BottomRight = 6;
	public final static int ORIENT_LeftMiddle = 7;
	public final static int ORIENT_RightMiddle = 8;
	public final static int ORIENT_Center = 9;
	public final static String[] AS_Orientation = { "Top Left", "Top Middle", "Top Right", "Bottom Left", "Bottom Middle", "Bottom Right", "Left Middle", "Right Middle", "Center" };
	String getOrientation_String(int e){
		if( e < 1 || e > AS_Orientation.length ) e = 1;
		return AS_Orientation[e-1];
	}

	private int meObject = 1;
	private int meOrientation = 1;
	private int meAlignment = 1; // uses orientation constants
	private int mpxOffsetHorizontal = 0;
	private int mpxOffsetVertical = 0;
	private int miRotation = 0;

	public int getObject(){ return meObject; }
	public int getOrientation(){ return meOrientation; }
	public int getAlignment(){ return meAlignment; }
	public int getOffsetHorizontal(){ return mpxOffsetHorizontal; }
	public int getOffsetVertical(){ return mpxOffsetVertical; }
	public int getRotation(){ return miRotation; }

	public void setObject( int i ){ meObject = i; }
	public void setOrientation( int i ){ meOrientation = i; }
	public void setAlignment( int i ){ meAlignment = i; }
	public void setOffsetHorizontal( int i ){ mpxOffsetHorizontal = i; }
	public void setOffsetVertical( int i ){ mpxOffsetVertical = i; }
	public void setRotation( int i ){ miRotation = i; }

	public final static int DEFAULT_STYLE_Legend = 1;
	public final static int DEFAULT_STYLE_Scale = 2;

	public PlotLayout( int eDEFAULT_STYLE ){
		if( eDEFAULT_STYLE == DEFAULT_STYLE_Legend ){
			meObject = OBJECT_Plot;
			meOrientation = ORIENT_TopRight;
			meAlignment = ORIENT_TopLeft; // uses orientation constants
			mpxOffsetHorizontal = 10;
			mpxOffsetVertical = 0;
			miRotation = 270;
		} else if( eDEFAULT_STYLE == DEFAULT_STYLE_Scale ) {
			meObject = OBJECT_Plot;
			meOrientation = ORIENT_BottomRight;
			meAlignment = ORIENT_BottomRight; // uses orientation constants
			mpxOffsetHorizontal = -10;
			mpxOffsetVertical = 0;
			miRotation = 0;
		}
	}

	/** returns a packed long HHHHVVVV or -1 to indicate failure
	 *  the width/height of are of the text item, the "object" is the object the orientation is relative to
	 * */
	long getLocation( int width, int height, int hObject, int vObject, int widthObject, int heightObject ){
		if( hObject == - 1 || vObject == -1 || widthObject == -1 || heightObject == -1 ) return -1;
		int hLocation = hObject;
		int vLocation = vObject;
		switch( meOrientation ){
			case ORIENT_TopLeft: break;
			case ORIENT_TopMiddle:
				hLocation += widthObject / 2 + 1; break;
			case ORIENT_TopRight:
				hLocation += widthObject + 1; break;
			case ORIENT_BottomLeft:
				vLocation += heightObject + 1; break;
			case ORIENT_BottomMiddle:
				hLocation += widthObject / 2 + 1;
				vLocation += heightObject + 1; break;
			case ORIENT_BottomRight:
				hLocation += widthObject + 1;
				vLocation += heightObject + 1; break;
			case ORIENT_LeftMiddle:
				vLocation += heightObject / 2 + 1; break;
			case ORIENT_RightMiddle:
				hLocation += widthObject + 1;
				vLocation += heightObject / 2 + 1; break;
			case ORIENT_Center:
				hLocation += widthObject / 2 + 1;
				vLocation += heightObject / 2 + 1; break;
		}
		hLocation += mpxOffsetHorizontal;
		vLocation += mpxOffsetVertical;
		switch( meAlignment ){
			case ORIENT_TopLeft: break;
			case ORIENT_TopMiddle:
				hLocation -= width / 2; break;
			case ORIENT_TopRight:
				hLocation -= width + 1; break;
			case ORIENT_BottomLeft:
				vLocation -= height; break;
			case ORIENT_BottomMiddle:
				hLocation -= width / 2;
				vLocation -= height; break;
			case ORIENT_BottomRight:
				hLocation -= width + 1;
				vLocation -= height; break;
			case ORIENT_LeftMiddle:
				vLocation -= height / 2; break;
			case ORIENT_RightMiddle:
				hLocation -= width;
				vLocation -= height / 2; break;
			case ORIENT_Center:
				hLocation -= width / 2;
				vLocation -= height / 2; break;
		}
		long nLocationPacked = hLocation;
		nLocationPacked = nLocationPacked << 32;
		nLocationPacked = nLocationPacked | vLocation;
		return nLocationPacked;
	}

	public String toString(){
		StringBuffer sb = new StringBuffer(250);
		sb.append("PlotLayout {\n");
		sb.append("\tObject: " + getObject_String(meObject) + "\n");
		sb.append("\tOrientation: " + getOrientation_String(meOrientation) + "\n");
		sb.append("\tAlignment: " + getOrientation_String(meAlignment) + "\n");
		sb.append("\tOffset Horizontal: " + mpxOffsetHorizontal + "\n");
		sb.append("\tOffset Vertical: " + mpxOffsetVertical + "\n");
		sb.append("\tRotation: " + miRotation + "\n");
		sb.append("}\n");
		return sb.toString();
	}

}

class Panel_PlotLayout extends JPanel {
	public final static int MODE_Full = 1;
	public final static int MODE_NoLegend = 2;
	public final static int MODE_NoScale = 3;
	PlotLayout mPlotLayout = null;
	JComboBox jcbObject;
	JComboBox jcbOrientation;
	JComboBox jcbAlignment;
	final JTextField jtfOffsetHorizontal = new JTextField(6);
	final JTextField jtfOffsetVertical = new JTextField(6);
	final JTextField jtfRotation = new JTextField(6);
	final JButton buttonView = new JButton("View Layout");
	Panel_PlotLayout( int eMode, ActionListener listenerViewButton ){

		String[] asPlotAnchors;
		switch( eMode ){
			case MODE_NoLegend:
				asPlotAnchors = PlotLayout.AS_Object_NoLegend; break;
			case MODE_NoScale:
				asPlotAnchors = PlotLayout.AS_Object_NoScale; break;
			case MODE_Full:
			default:
				asPlotAnchors = PlotLayout.AS_Object; break;
		}

		if( listenerViewButton == null ){
			listenerViewButton =
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						Test_PlotLayout tpl = new Test_PlotLayout(mPlotLayout);
						tpl.setVisible(true);
						tpl.setSize(new Dimension(800, 450));
					}
				};
		}
		buttonView.addActionListener(listenerViewButton);

		jcbObject = new JComboBox(asPlotAnchors);
		jcbOrientation = new JComboBox(PlotLayout.AS_Orientation);
		jcbAlignment = new JComboBox(PlotLayout.AS_Orientation);
		setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Layout"));
		JLabel labObject = new JLabel("Relative To: ");
		JLabel labOrientation = new JLabel("Orientation: ");
		JLabel labAlignment = new JLabel("Alignment: ");
		JLabel labOffsetHorizontal = new JLabel("Horizontal Offset: ");
		JLabel labOffsetVertical = new JLabel("Vertical Offset: ");
		JLabel labRotation = new JLabel("Rotation: ");
		vSetupListeners();
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.BOTH;

		// top margin
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		add(Box.createVerticalStrut(15), gbc);

		// Object
		gbc.gridy = 1;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		add(labObject, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		add(jcbObject, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		add(Box.createHorizontalGlue(), gbc);

		// Orientation
		gbc.gridy = 2;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		add(labOrientation, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		add(jcbOrientation, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		add(Box.createHorizontalGlue(), gbc);

		// Alignment
		gbc.gridy = 3;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		add(labAlignment, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		add(jcbAlignment, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		add(Box.createHorizontalGlue(), gbc);

		// OffsetHorizontal
		gbc.gridy = 4;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		add(labOffsetHorizontal, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		add(jtfOffsetHorizontal, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		add(Box.createHorizontalGlue(), gbc);

		// OffsetVertical
		gbc.gridy = 5;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		add(Box.createVerticalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		add(labOffsetVertical, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		add(Box.createVerticalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		add(jtfOffsetVertical, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		add(Box.createVerticalGlue(), gbc);

		// Rotation
		gbc.gridy = 6;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		add(labRotation, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		add(jtfRotation, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		add(Box.createHorizontalGlue(), gbc);

		// spacer
		gbc.gridy = 7;
		add(Box.createVerticalStrut(10), gbc);

		// View Button
		gbc.gridy = 8;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		gbc.gridwidth = 3;
		add(buttonView, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		gbc.gridwidth = 1;
		add(Box.createHorizontalGlue(), gbc);

		// bottom margin
		gbc.gridy = 9;
		gbc.gridx = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		add(Box.createVerticalStrut(15), gbc);

		setFieldsEnabled( false );
	}

	void setFieldsEnabled( boolean z ){
		jcbObject.setEnabled(z);
		jcbOrientation.setEnabled(z);
		jcbAlignment.setEnabled(z);
		jtfOffsetHorizontal.setEnabled(z);
		jtfOffsetVertical.setEnabled(z);
		jtfRotation.setEnabled(z);
	}

	private void vSetupListeners(){
		jcbObject.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					int xSelected = jcbObject.getSelectedIndex();
					mPlotLayout.setObject(xSelected + 1);
				}
			}
		);
		jcbOrientation.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					int xSelected = jcbOrientation.getSelectedIndex();
					mPlotLayout.setOrientation(xSelected + 1);
				}
			}
		);
		jcbAlignment.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					int xSelected = jcbAlignment.getSelectedIndex();
					mPlotLayout.setAlignment(xSelected + 1);
				}
			}
		);
		jtfOffsetHorizontal.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						mPlotLayout.setOffsetHorizontal( Integer.parseInt(jtfOffsetHorizontal.getText()) );
					} catch(Exception ex){
						jtfOffsetHorizontal.setText("0");
						mPlotLayout.setOffsetHorizontal( 0 );
					}
				}
			}
		);
		jtfOffsetVertical.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						mPlotLayout.setOffsetVertical( Integer.parseInt(jtfOffsetVertical.getText()) );
					} catch(Exception ex){
						jtfOffsetVertical.setText("0");
						mPlotLayout.setOffsetVertical( 0 );
					}
				}
			}
		);
		jtfRotation.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						mPlotLayout.setRotation( Integer.parseInt(jtfRotation.getText()) );
					} catch(Exception ex){
						jtfRotation.setText("0");
						mPlotLayout.setRotation( 0 );
					}
				}
			}
		);
	}
	PlotLayout getPlotLayout(){ return mPlotLayout; }
	void setPlotLayout( PlotLayout layout ){
		mPlotLayout = layout;
		setFieldsEnabled(layout != null);
		if( layout == null ) return;
		jcbObject.setSelectedIndex(layout.getObject()-1);
		jcbOrientation.setSelectedIndex(layout.getOrientation()-1);
		jcbAlignment.setSelectedIndex(layout.getAlignment()-1);
		jtfOffsetHorizontal.setText(Integer.toString(layout.getOffsetHorizontal()));
		jtfOffsetVertical.setText(Integer.toString(layout.getOffsetVertical()));
		jtfRotation.setText(Integer.toString(layout.getRotation()));
	}
}

class Test_PlotLayout extends JFrame {
	Panel_PlotLayout panelLayout;
	PlotLayout mPlotLayout;
	PlotLayout_TestCanvas panelCanvas;
	public Test_PlotLayout(){
		this( null );
	}
	public Test_PlotLayout( PlotLayout layout ){
		if( layout == null ){
			mPlotLayout = new PlotLayout(PlotLayout.DEFAULT_STYLE_Legend);
		} else {
			mPlotLayout = layout;
		}
		ActionListener test_button =
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					vUpdateCanvas();
				}
			};
		panelLayout = new Panel_PlotLayout(Panel_PlotLayout.MODE_Full, test_button);
		panelLayout.setPlotLayout(mPlotLayout);
		panelCanvas = new PlotLayout_TestCanvas(mPlotLayout);
		panelCanvas.setPreferredSize(new Dimension(400, 300));
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(panelLayout, BorderLayout.EAST);
		getContentPane().add(panelCanvas, BorderLayout.CENTER);
	}
	void vUpdateCanvas(){
		panelCanvas.repaint();
	}
}

class PlotLayout_TestCanvas extends JPanel {

	public final static boolean DEBUG_Layout = false;
	public final static double TwoPi = 2d * 3.14159d;

	PlotLayout mPlotLayout;
	public PlotLayout_TestCanvas( PlotLayout pl ){
		mPlotLayout = pl;
	}
	public void paintComponent( Graphics g ){
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.WHITE);
		int iObjectX, iObjectY, iObjectHeight, iObjectWidth;
		switch( mPlotLayout.getObject() ){
			default:
			case PlotLayout.OBJECT_Canvas:
				iObjectX = 0;
				iObjectY = 0;
				iObjectWidth = 399;
				iObjectHeight = 299;
				g2.drawRect(iObjectX, iObjectY, iObjectWidth, iObjectHeight);
				g2.drawString("Canvas", 250, 30);
				break;
			case PlotLayout.OBJECT_Plot:
				iObjectX = 60;
				iObjectY = 50;
				iObjectWidth = 380;
				iObjectHeight = 200;
				g2.drawRect(iObjectX, iObjectY, iObjectWidth, iObjectHeight);
				g2.drawString("Plot Area", iObjectX + iObjectWidth - 80, iObjectY + 40);
				break;
			case PlotLayout.OBJECT_Legend:
				iObjectX = 120;
				iObjectY = 70;
				iObjectWidth = 240;
				iObjectHeight = 160;
				g2.drawRect(iObjectX, iObjectY, iObjectWidth, iObjectHeight);
				g2.drawString("Legend", iObjectX + iObjectWidth - 80, iObjectY + 40);
				break;
			case PlotLayout.OBJECT_Scale:
				iObjectX = 250;
				iObjectY = 210;
				iObjectWidth = 60;
				iObjectHeight = 16;
				g2.drawRect(iObjectX, iObjectY, iObjectWidth, iObjectHeight);
				g2.drawString("Scale", iObjectX + 10, iObjectY);
				break;
			case PlotLayout.OBJECT_AxisHorizontal:
				iObjectX = 60;
				iObjectY = 250;
				iObjectWidth = 300;
				iObjectHeight = 2;
				g2.drawRect(iObjectX, iObjectY, iObjectWidth, iObjectHeight);
				g2.drawString("X-Axis", iObjectX + iObjectWidth + 10, iObjectY - 5);
				break;
			case PlotLayout.OBJECT_AxisVertical:
				iObjectX = 60;
				iObjectY = 50;
				iObjectWidth = 2;
				iObjectHeight = 200;
				g2.drawRect(iObjectX, iObjectY, iObjectWidth, iObjectHeight);
				g2.drawString("Y-Axis", iObjectX - 20, iObjectY + iObjectHeight + 40);
				break;
		}
		Panel_Plot.vDrawText( g2, "Layout Location", opendap.clients.odc.Styles.fontSansSerifBold12, Color.RED, mPlotLayout, iObjectX, iObjectY, iObjectWidth, iObjectHeight, true );
	}

}
