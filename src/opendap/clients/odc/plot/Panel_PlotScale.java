package opendap.clients.odc.plot;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.data.Node;
import opendap.clients.odc.gui.Resources;
import opendap.clients.odc.Utility_Numeric;

class Panel_PlotScale extends JPanel {
	final static long serialVersionUID = 1;
	public static void main( String[] args ){
		try {
//			JDialog jd;
//			JOptionPane jop;
			Panel_PlotScale panelPlotScale = new Panel_PlotScale();
			PlotScale plot_scale = new PlotScale();
			plot_scale.setDataDimension( 1000, 900 );
			panelPlotScale._setScale( plot_scale );
			javax.swing.JFrame frame = new javax.swing.JFrame();
			frame.setLayout( new java.awt.BorderLayout() );
			frame.add( panelPlotScale, java.awt.BorderLayout.CENTER );
			frame.pack();
			frame.setVisible( true );
//			jop = new JOptionPane( panelPlotScale, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION );
//			jd = jop.createDialog( null, "Set Scale" );
//			jd.setVisible( true );
//			System.out.println( mHSBpicker.getCurrentColor_ARGB() );
//			System.out.println( mHSBpicker.getComparisonColor_ARGB() );
//			System.exit( 0 );
		} catch( Throwable ex ) {
			System.err.println("Error: " + ex);
		}
	}

	PlotScale scaleActive = null;
	private final JComboBox jcbMarginUnits = new JComboBox(PlotScale.AS_Units);
	private final JComboBox jcbScaleUnits = new JComboBox(PlotScale.AS_Units);
	private final JComboBox jcbZoomFactor = new JComboBox(PlotScale.AS_ZoomFactor);
	private final JRadioButton jrbPixelsPerData = new JRadioButton();
	private final JRadioButton jrbEntireCanvas = new JRadioButton("Entire Canvas");
	private final JRadioButton jrbPlotArea = new JRadioButton("Plot Area");
	private final JTextField jtfMarginLeft = new JTextField(6);
	private final JTextField jtfMarginTop = new JTextField(6);
	private final JTextField jtfMarginRight = new JTextField(6);
	private final JTextField jtfMarginBottom = new JTextField(6);
	private final JTextField jtfWidth = new JTextField(6);
	private final JTextField jtfHeight = new JTextField(6);
	private final JTextField jtfResolution = new JTextField(6);
	private final JCheckBox jcheckAspectRatio = new JCheckBox("Maintain Aspect Ratio: ");
	private final JButton jbSetScreenDPI = new JButton( Resources.getIcon( Resources.Icons.DisplayScreen ) );

	Panel_PlotScale(){

		JLabel labMarginLeft = new JLabel( "Left: ", JLabel.RIGHT);
		JLabel labMarginTop = new JLabel( "Top: ", JLabel.RIGHT);
		JLabel labMarginBottom = new JLabel( "Bottom: ", JLabel.RIGHT);
		JLabel labMarginRight = new JLabel( "Right: ", JLabel.RIGHT);
		JLabel labMarginUnits = new JLabel( "Margin Units: ", JLabel.RIGHT);
		JLabel labScaleUnits = new JLabel( "Units: ", JLabel.RIGHT);
		JLabel labZoom = new JLabel( "Zoom Factor: ", JLabel.RIGHT);
		JLabel labWidth = new JLabel( "Width: ", JLabel.RIGHT);
		JLabel labHeight = new JLabel( "Height: ", JLabel.RIGHT);
		JLabel labResolution = new JLabel( "Resolution: ", JLabel.RIGHT);
		JLabel labDPI = new JLabel( "dpi", JLabel.LEFT);
		javax.swing.ButtonGroup bg = new javax.swing.ButtonGroup();
		bg.add(jrbPixelsPerData);
		bg.add(jrbEntireCanvas);
		bg.add(jrbPlotArea);
		jrbPixelsPerData.setSelected(true);
		vSetupListeners();

		jbSetScreenDPI.setToolTipText( "Set resolution to native display resolution in dots per inch (dpi)." );
		jtfResolution.setToolTipText( "Output resolution in dots per inch (dpi)." );
		
		JPanel panelDPI = new JPanel();
		panelDPI.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0) );  // top left bottom right
		panelDPI.setLayout( new BoxLayout( panelDPI, BoxLayout.X_AXIS ));
		panelDPI.add( jtfResolution );
		panelDPI.add( labDPI );
		panelDPI.add( Box.createHorizontalStrut(6) );
		panelDPI.add( jbSetScreenDPI );

		JPanel panelScaleContext = new JPanel(); // used for the two radio buttons
		panelScaleContext.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0) );  // top left bottom right
		panelScaleContext.setLayout( new BoxLayout( panelScaleContext, BoxLayout.X_AXIS ));
		panelScaleContext.add( jrbEntireCanvas );
		panelScaleContext.add( jrbPlotArea );
		
		JPanel panelScale = new JPanel();
		panelScale.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Plot Scale"));
		panelScale.setLayout(new java.awt.GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;

		// margin units
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labMarginUnits, gbc);
		gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jcbMarginUnits, gbc);
		gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5;
		panelScale.add(Box.createVerticalStrut(4), gbc);

		// margin left
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labMarginLeft, gbc);
		gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfMarginLeft, gbc);
		gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// margin top
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labMarginTop, gbc);
		gbc.gridx = 2; gbc.gridy = 3; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfMarginTop, gbc);
		gbc.gridx = 3; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// margin right
		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labMarginRight, gbc);
		gbc.gridx = 2; gbc.gridy = 4; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfMarginRight, gbc);
		gbc.gridx = 3; gbc.gridy = 4; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// margin bottom
		gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labMarginBottom, gbc);
		gbc.gridx = 2; gbc.gridy = 5; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfMarginBottom, gbc);
		gbc.gridx = 3; gbc.gridy = 5; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// resolution/DPI panel
		gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labResolution, gbc);
		gbc.gridx = 2; gbc.gridy = 7; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(panelDPI, gbc);
		gbc.gridx = 3; gbc.gridy = 7; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// zoom
		gbc.gridx = 0; gbc.gridy = 9; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 9; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labZoom, gbc);
		gbc.gridx = 2; gbc.gridy = 9; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jcbZoomFactor, gbc);
		gbc.gridx = 3; gbc.gridy = 9; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// plot area / canvas radio selector
		gbc.gridx = 0; gbc.gridy = 10; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 10; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(Box.createVerticalStrut(10), gbc);
		gbc.gridx = 2; gbc.gridy = 10; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(panelScaleContext, gbc);
		gbc.gridx = 3; gbc.gridy = 10; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 11; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// absolute scale units
		gbc.gridx = 0; gbc.gridy = 12; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 12; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labScaleUnits, gbc);
		gbc.gridx = 2; gbc.gridy = 12; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jcbScaleUnits, gbc);
		gbc.gridx = 3; gbc.gridy = 12; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 13; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// absolute - width
		gbc.gridx = 0; gbc.gridy = 14; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 14; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labWidth, gbc);
		gbc.gridx = 2; gbc.gridy = 14; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfWidth, gbc);
		gbc.gridx = 3; gbc.gridy = 14; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// absolute - height
		gbc.gridx = 0; gbc.gridy = 15; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 15; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(labHeight, gbc);
		gbc.gridx = 2; gbc.gridy = 15; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jtfHeight, gbc);
		gbc.gridx = 3; gbc.gridy = 15; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		// separator
		gbc.gridx = 0; gbc.gridy = 16; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7;
		panelScale.add(Box.createVerticalStrut(10), gbc);

		// aspect ratio
		gbc.gridx = 0; gbc.gridy = 17; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1; gbc.gridheight = 1;
		panelScale.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.gridy = 17; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(Box.createHorizontalStrut(4), gbc);
		gbc.gridx = 2; gbc.gridy = 17; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelScale.add(jcheckAspectRatio, gbc);
		gbc.gridx = 3; gbc.gridy = 17; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 2;
		panelScale.add(Box.createHorizontalGlue(), gbc);

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(panelScale);

		updatePanel();
	}

	// when the scale changes or is set then this method makes the panel reflect the
	// values in the scale data structure
	boolean mzUpdating = false;
	public void updatePanel(){
		if( mzUpdating ) return; // prevent re-entrancy
		try {
			mzUpdating = true;
			boolean zEnable = (scaleActive != null);
			jcbMarginUnits.setEnabled(zEnable);
			jcbScaleUnits.setEnabled(zEnable);
			jcbZoomFactor.setEnabled(zEnable);
			jrbEntireCanvas.setEnabled(zEnable);
			jrbPlotArea.setEnabled(zEnable);
			jtfMarginLeft.setEnabled(zEnable);
			jtfMarginTop.setEnabled(zEnable);
			jtfMarginRight.setEnabled(zEnable);
			jtfMarginBottom.setEnabled(zEnable);
			jtfWidth.setEnabled(zEnable);
			jtfHeight.setEnabled(zEnable);
			jtfResolution.setEnabled(zEnable);
			jbSetScreenDPI.setEnabled(zEnable);
			jcheckAspectRatio.setEnabled(zEnable);
			jcheckAspectRatio.setSelected(true);
			if( scaleActive == null ) return;
			jcbMarginUnits.setSelectedIndex( scaleActive.getMarginUnits().ordinal() );
			jcbScaleUnits.setSelectedIndex( scaleActive.getScaleUnits().ordinal() );

			PlotScale.SCALE_MODE eScaleMode = scaleActive.getScaleMode();

			// set dpi
			jtfResolution.setText( Integer.toString( scaleActive.getOutputResolution() ) );
			int dpiScreen = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
			if( scaleActive.getOutputResolution() == dpiScreen ){
				jbSetScreenDPI.setVisible( false );
			} else {
				jbSetScreenDPI.setVisible( true );
			}
			
			// set zoom
			jcbZoomFactor.setSelectedIndex( scaleActive.getZoomFactor().ordinal() );

			// set mode radios
			switch( eScaleMode ){
				case Canvas:
					jrbEntireCanvas.setSelected(true);
					jrbPlotArea.setSelected(false);
					break;
				case PlotArea:
					jrbEntireCanvas.setSelected(false);
					jrbPlotArea.setSelected(true);
					break;
				case Output:
				case Zoom:
					jrbEntireCanvas.setSelected(false);
					jrbPlotArea.setSelected(true);
					break;
			}

			// set absolute width/height
			PlotScale.UNITS eUNITS = scaleActive.getScaleUnits();
			int pxWidth, pxHeight;
			if( jrbEntireCanvas.isSelected() ){
				pxWidth = scaleActive.getCanvas_Width();
				pxHeight = scaleActive.getCanvas_Height();
			} else {
				pxWidth = scaleActive.getPlot_Width();
				pxHeight = scaleActive.getPlot_Height();
			}
			float fWidth = scaleActive.getUnits( pxWidth, eUNITS );
			float fHeight = scaleActive.getUnits( pxHeight, eUNITS );
			if( fWidth == (int)fWidth ){
				jtfWidth.setText(Integer.toString((int)fWidth));
			} else {
				jtfWidth.setText(Float.toString(fWidth));
			}
			if( fHeight == (int)fHeight ){
				jtfHeight.setText( Integer.toString((int)fHeight) );
			} else {
				jtfHeight.setText( Float.toString(fHeight) );
			}

			jcbScaleUnits.setEnabled( true );
			
			// enable width/height/aspect
			boolean zAbsoluteEnabled = (eScaleMode == PlotScale.SCALE_MODE.Canvas) || (eScaleMode == PlotScale.SCALE_MODE.PlotArea);
			jrbEntireCanvas.setEnabled(zAbsoluteEnabled);
			jrbPlotArea.setEnabled(zAbsoluteEnabled);
			jtfWidth.setEnabled(zAbsoluteEnabled);
			jtfHeight.setEnabled(zAbsoluteEnabled);
			jcheckAspectRatio.setEnabled(zAbsoluteEnabled);

			// set margin text boxes
			jtfMarginLeft.setText(Integer.toString(scaleActive.getMarginLeft_px()));
			jtfMarginTop.setText(Integer.toString(scaleActive.getMarginTop_px()));
			jtfMarginRight.setText(Integer.toString(scaleActive.getMarginRight_px()));
			jtfMarginBottom.setText(Integer.toString(scaleActive.getMarginBottom_px()));
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, "while updating scale panel");
		} finally {
			mzUpdating = false;
		}
	}

	PlotScale _getScale(){ return scaleActive; }
	void _setScale( PlotScale scale ){
		scaleActive = scale;
		updatePanel();
	}

	void _changeDataDimension( int iNewWidth, int iNewHeight ){
		if( scaleActive != null ){
			scaleActive.setDataDimension( iNewWidth, iNewHeight );
		}
	}

	private void vSetupListeners(){
		jbSetScreenDPI.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					scaleActive.setOutputToScreenResolution();
					updatePanel();
				}
			}
		);
		jtfResolution.addKeyListener(
			new java.awt.event.KeyListener(){
				public void keyReleased( KeyEvent ke ){}
				public void keyPressed( KeyEvent ke ){
					System.out.println( "pressed:  " + ke );
				}
				public void keyTyped( KeyEvent ke ){
					switch( ke.getKeyChar() ){
						case '\n':
							vUpdateResolution();
							break;
						case KeyEvent.VK_ESCAPE:
							jtfResolution.setText( Integer.toString( scaleActive.dpiOutput ) );
							break;
					}
				}
			});
		jcbScaleUnits.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					int xSelected = jcbScaleUnits.getSelectedIndex();
					scaleActive.setUnits( PlotScale.UNITS.values()[xSelected] );
					updatePanel();
				}
			}
		);
		jcbMarginUnits.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					int xSelected = jcbMarginUnits.getSelectedIndex();
					scaleActive.setMarginUnits( PlotScale.UNITS.values()[xSelected] );
					jtfMarginTop.setText( Utility_Numeric.floatToString_Rounded( scaleActive.getMarginTop_display() ) );
					jtfMarginBottom.setText( Utility_Numeric.floatToString_Rounded( scaleActive.getMarginBottom_display() ) );
					jtfMarginRight.setText( Utility_Numeric.floatToString_Rounded( scaleActive.getMarginRight_display() ) );
					jtfMarginLeft.setText( Utility_Numeric.floatToString_Rounded( scaleActive.getMarginLeft_display() ) );
				}
			}
		);
		jtfResolution.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						jtfResolution.setText( Integer.toString( scaleActive.dpiOutput ) );
					} catch(Exception ex){} // ignore invalid entries
				}
			}
		);
		jcbZoomFactor.addActionListener(
			new ActionListener(){
				public void actionPerformed( ActionEvent event ){
					PlotScale.SCALE_MODE eScaleMode_radio_button = jrbEntireCanvas.isSelected() ? PlotScale.SCALE_MODE.Canvas : PlotScale.SCALE_MODE.PlotArea;   
					int xSelected = jcbZoomFactor.getSelectedIndex();
					switch( xSelected ){
						case 0: // max
							scaleActive.setScaleMode( PlotScale.SCALE_MODE.Output );
							break;
						case 1: // 50%
							scaleActive.setZoomFactor( PlotScale.ZOOM_FACTOR.Zoom50, eScaleMode_radio_button );
							break;
						case 2: // 75%
							scaleActive.setZoomFactor( PlotScale.ZOOM_FACTOR.Zoom75, eScaleMode_radio_button );
							break;
						case 3: // 100%
							scaleActive.setZoomFactor( PlotScale.ZOOM_FACTOR.Zoom100, eScaleMode_radio_button );
							break;
						case 4: // 200%
							scaleActive.setZoomFactor( PlotScale.ZOOM_FACTOR.Zoom200, eScaleMode_radio_button );
							break;
						case 5: // 300%
							scaleActive.setZoomFactor( PlotScale.ZOOM_FACTOR.Zoom300, eScaleMode_radio_button );
							break;
						case 6: // 400%
							scaleActive.setZoomFactor( PlotScale.ZOOM_FACTOR.Zoom400, eScaleMode_radio_button );
							break;
						case 7: // custom
							scaleActive.setZoomFactor( PlotScale.ZOOM_FACTOR.Custom, eScaleMode_radio_button );
							break;
						default: return; // do nothing
					}
					updatePanel();
				}
			}
		);
		jrbEntireCanvas.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					scaleActive.setScaleMode( PlotScale.SCALE_MODE.Canvas );
					updatePanel();
				}
			}
		);
		jrbPlotArea.addActionListener(
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					scaleActive.setScaleMode( PlotScale.SCALE_MODE.PlotArea );
					updatePanel();
				}
			}
		);
		jtfWidth.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						vUpdateWidth();
					} catch(Exception ex){} // ignore invalid entries
				}
			}
		);
//		jtfWidth.addKeyListener(
//	    	new java.awt.event.KeyListener(){
//		    	public void keyPressed(java.awt.event.KeyEvent ke){
//			    	if( ke.getKeyCode() == ke.VK_ENTER ){
//						vUpdateWidth();
//	    			}
//		    	}
//			    public void keyReleased(java.awt.event.KeyEvent ke){}
//				public void keyTyped(java.awt.event.KeyEvent ke){}
//			}
//		);
		jtfHeight.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						vUpdateHeight();
					} catch(Exception ex){} // ignore invalid entries
				}
			}
		);
//		jtfHeight.addKeyListener(
//	    	new java.awt.event.KeyListener(){
//		    	public void keyPressed(java.awt.event.KeyEvent ke){
//			    	if( ke.getKeyCode() == ke.VK_ENTER ){
//						vUpdateHeight();
//	    			}
//		    	}
//			    public void keyReleased(java.awt.event.KeyEvent ke){}
//				public void keyTyped(java.awt.event.KeyEvent ke){}
//			}
//		);
		jtfMarginLeft.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						scaleActive.setMarginLeft( Float.parseFloat(jtfMarginLeft.getText()) );
					} catch(Exception ex){
						jtfMarginLeft.setText("50");
						scaleActive.setMarginLeft( 50 );
					}
				}
			}
		);
		jtfMarginTop.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						scaleActive.setMarginTop( Float.parseFloat(jtfMarginTop.getText()) );
					} catch(Exception ex){
						jtfMarginTop.setText("50");
						scaleActive.setMarginTop( 50 );
					}
				}
			}
		);
		jtfMarginRight.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						scaleActive.setMarginRight( Float.parseFloat(jtfMarginRight.getText()) );
					} catch(Exception ex){
						jtfMarginRight.setText("50");
						scaleActive.setMarginRight( 50 );
					}
				}
			}
		);
		jtfMarginBottom.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						scaleActive.setMarginBottom( Float.parseFloat(jtfMarginBottom.getText()) );
					} catch(Exception ex){
						jtfMarginBottom.setText("50");
						scaleActive.setMarginBottom( 50 );
					}
				}
			}
		);
	}

	private void vUpdateResolution(){
		String sResolution = jtfResolution.getText();
		int iResolutionDPI = 0;
	    try {
			iResolutionDPI = Integer.parseInt( sResolution );
			if( iResolutionDPI <= 0 ){
				ApplicationController.vShowError("Invalid output resolution (" + sResolution + ") must be a positive number");
				return;
			}
		} catch( NumberFormatException ex ) {
			ApplicationController.vShowError("Unable to interpret " + sResolution + " as a positive integer");
			jtfResolution.setText( Integer.toString( scaleActive.dpiOutput ) );
			return;
		}
		int dpiScreen = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
		if( iResolutionDPI == dpiScreen ){
			jbSetScreenDPI.setVisible( false );
		} else {
			jbSetScreenDPI.setVisible( true );
		}
		scaleActive.setOutputResolution( iResolutionDPI );
		updatePanel();
	}
	
	private final void vUpdateWidth(){
		float fWidth;
		String sWidth = jtfWidth.getText();
	    try {
			fWidth = Float.parseFloat(sWidth);
			if( fWidth <= 0 ){
				ApplicationController.vShowError("Plot dimensions must be positive numbers");
				return;
			}
		} catch( NumberFormatException ex ) {
			ApplicationController.vShowError("Unable to interpret " + sWidth + " as a positive floating point number");
			return;
		}
		PlotScale.UNITS eScaleUnits = scaleActive.getScaleUnits();
		int iWidth_pixels = scaleActive.getPixels( fWidth, eScaleUnits );
		if( jrbEntireCanvas.isSelected() ){
			scaleActive.setPixelWidth_Canvas( iWidth_pixels );
		} else if( jrbPlotArea.isSelected() ){
			scaleActive.setPixelWidth_PlotArea( iWidth_pixels );
		}
		if( jcheckAspectRatio.isSelected() ){  // scale to aspect ratio
			float fRatio = (float)scaleActive.miDataHeight / (float)scaleActive.miDataWidth;
			float fHeight = fWidth * fRatio;
			int iHeight_pixels = scaleActive.getPixels( fHeight, eScaleUnits );
			if( jrbEntireCanvas.isSelected() ){
				scaleActive.setPixelHeight_Canvas( iHeight_pixels);
			} else if( jrbPlotArea.isSelected() ){
				scaleActive.setPixelHeight_PlotArea( iWidth_pixels );
			}
			if( fHeight == (int)fHeight ){
				jtfHeight.setText(Integer.toString((int)fHeight));
			} else {
	    		jtfHeight.setText(Float.toString(fHeight));
			}
		}
	}
	private final void vUpdateHeight(){
		float fHeight;
		String sHeight = jtfHeight.getText();
	    try {
			fHeight = Float.parseFloat(sHeight);
			if( fHeight <= 0 ){
				ApplicationController.vShowError("Plot dimensions must be positive numbers");
				return;
			}
		} catch( NumberFormatException ex ) {
			ApplicationController.vShowError("Unable to interpret " + sHeight + " as a positive floating point number");
			return;
		}
		PlotScale.UNITS eScaleUnits = scaleActive.getScaleUnits();
		int iHeight_pixels = scaleActive.getPixels(fHeight, eScaleUnits);
		if( jrbEntireCanvas.isSelected() ){
			scaleActive.setPixelHeight_Canvas( iHeight_pixels );
		} else if( jrbPlotArea.isSelected() ){
			scaleActive.setPixelHeight_PlotArea( iHeight_pixels );
		}
		if( jcheckAspectRatio.isSelected() ){  // scale to aspect ratio
			float fRatio = (float)scaleActive.miDataWidth / (float)scaleActive.miDataHeight;
			float fWidth = fHeight * fRatio;
			int iWidth_pixels = scaleActive.getPixels( fWidth, eScaleUnits );
			if( jrbEntireCanvas.isSelected() ){
				scaleActive.setPixelWidth_Canvas( iWidth_pixels );
			} else if( jrbPlotArea.isSelected() ){
				scaleActive.setPixelWidth_PlotArea( iWidth_pixels );
			}
			if( fWidth == (int)fWidth ){
				jtfWidth.setText(Integer.toString((int)fWidth));
			} else {
	    		jtfWidth.setText(Float.toString(fWidth));
			}
		}
	}
}
