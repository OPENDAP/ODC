package opendap.clients.odc;

import tmap_30.map.*;
import tmap_30.convert.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class Panel_SpatialTemporal extends JPanel implements MouseListener, MouseMotionListener, MapConstants {
	private final static String RESOURCE_PATH_WorldMap = "/tmap_30/images/java_0_world.gif";

	final static int IMAGE_SIZE_X = 500;
	final static int IMAGE_SIZE_Y = 240;
	final static Color MAPTOOL_COLOR1 = Color.white;
	final static int TOOL_TYPE_PT = 0;
	final static int TOOL_TYPE_X  = 1;
	final static int TOOL_TYPE_Y  = 2;
	final static int TOOL_TYPE_XY = 3;
	final static int DEFAULT_ToolType = 3;

	private MapCanvas mMapCanvas;
	private MapGrid grid;
	private MapTool [] toolArray = new MapTool[1];
	private MapRegion [] regionArray = new MapRegion[0];
	private Convert XConvert, YConvert;
	private Image mMapImage;
	private int miToolType = DEFAULT_ToolType;

	// todo deal with these
    private String img_x_domain = null; // getParameter("img_x_domain");
    private String img_y_domain = null; // getParameter("img_y_domain");
    private String sToolType = null; // getParameter("toolType");
    private String tool_x_range = null; // getParameter("tool_x_range");
    private String tool_y_range = null; // getParameter("tool_y_range");

	private JButton buttonClear;

	private JTextField jtfNorth;
	private JTextField jtfWest;
	private JTextField jtfEast;
	private JTextField jtfSouth;

	JComboBox jcbTemporalQualifier_From;
	JComboBox jcbTemporalQualifier_To;
	private final JTextField jtfTemporal_FromYear = new JTextField("", 6);
	private final JTextField jtfTemporal_FromMonth = new JTextField("", 3);
	private final JTextField jtfTemporal_FromDay = new JTextField("", 3);
	private final JTextField jtfTemporal_ToYear = new JTextField("", 6);
	private final JTextField jtfTemporal_ToMonth = new JTextField("", 3);
	private final JTextField jtfTemporal_ToDay = new JTextField("", 3);

	private JPanel panelEast;
	private JPanel panelCanvas;
	private JPanel panelMapButtons;
	private JPanel panelGazetteer;
	private boolean mzGazetteerEnabled;
	private boolean mzApply = false;
	private JList mjlistCustom;

	public Panel_SpatialTemporal(){
		super();
	}

	public boolean zInitialize( StringBuffer sbError ) {

		if( !zInitializeGUI( sbError ) ){
			sbError.insert(0, "failed to initialize canvas: ");
			return false;
		}

		if( !zInitializeCanvas( sbError ) ){
			sbError.insert(0, "failed to initialize canvas: ");
			return false;
		}

		if( zInitializeGazetteer( sbError ) ){
			this.setGazetteerEnabled(true);
		} else {
			ApplicationController.getInstance().vShowWarning("failed to initialize gazetteer: " + sbError);
			this.setGazetteerEnabled(false);
		}

		this.setLayout(new BorderLayout());
		add(panelCanvas, BorderLayout.CENTER);
		add(panelEast, BorderLayout.EAST);
		if( this.isGazetteerEnabled() ) add(panelGazetteer, BorderLayout.SOUTH);

		// vUpdateMap("60 N", "60 S", "80 W", "20 E");
		setGlobal(); // global setting is the default

		return true;
	}

	private boolean zInitializeCanvas( StringBuffer sbError ){
		try {
			XConvert = new ConvertLongitude(ConvertLongitude.SPACE_E_W);
			YConvert = new ConvertLatitude(ConvertLatitude.SPACE_N_S);
			grid = new MapGrid(180, -180, -90.0, 90.0);

			if ( sToolType == null ) sToolType = "XY";

			if ( sToolType.equals("PT") ) {
				toolArray[0] = new PTTool(50,50,1,1,MAPTOOL_COLOR1);
			} else if ( sToolType.equals("X") ) {
				toolArray[0] = new XTool(50,50,100,1,MAPTOOL_COLOR1);
			} else if ( sToolType.equals("Y") ) {
				toolArray[0] = new YTool(50,50,1,50,MAPTOOL_COLOR1);
			} else { // default to XY
				toolArray[0] = new XYTool(50,50,100,50,MAPTOOL_COLOR1);
			}

			toolArray[0].setRange_X(180, -180);
			toolArray[0].setRange_Y(-90.0, 90.0);
			toolArray[0].setSnapping(true, true);

			double lo = -180;
			double hi = 180;
			StringTokenizer st;
			if ( img_x_domain != null ) {
				st = new StringTokenizer(img_x_domain);
				if ( st.hasMoreTokens() ) {
					lo = Double.valueOf(st.nextToken()).doubleValue();
					if ( st.hasMoreTokens() ) hi = Double.valueOf(st.nextToken()).doubleValue();
				}
			}

			grid.setDomain_X(lo, hi);
			toolArray[0].setRange_X(lo,hi);
			XConvert.setRange(lo, hi);

			lo = -90.0;
			hi = 90.0;
			if ( img_y_domain != null ) {
				st = new StringTokenizer(img_y_domain);
				if ( st.hasMoreTokens() ) {
					lo = Double.valueOf(st.nextToken()).doubleValue();
					if ( st.hasMoreTokens() ) hi = Double.valueOf(st.nextToken()).doubleValue();
				}
			}

			grid.setDomain_Y(lo, hi);
			toolArray[0].setRange_Y(lo,hi);
			YConvert.setRange(lo, hi);

			Image mMapImage = Utility.imageLoadResource(RESOURCE_PATH_WorldMap);
			if( mMapImage == null ){
				sbError.append("failed to load map " + RESOURCE_PATH_WorldMap);
				return false;
			}
			mMapCanvas = new MapCanvas(mMapImage, IMAGE_SIZE_X, IMAGE_SIZE_Y, toolArray, grid);

			mMapCanvas.setToolArray(toolArray);

			mMapCanvas.addMouseListener(this);
			mMapCanvas.addMouseMotionListener( this );
			enableEvents(AWTEvent.MOUSE_EVENT_MASK|AWTEvent.MOUSE_MOTION_EVENT_MASK);

			double x_lo = 0.0;
			double x_hi = 0.0;
			double y_lo = 0.0;
			double y_hi = 0.0;

			try {
				if ( tool_x_range != null ) {
					st = new StringTokenizer(tool_x_range);
					x_lo = XConvert.toDouble(st.nextToken());
					if ( st.hasMoreTokens() ) x_hi = XConvert.toDouble(st.nextToken());
				}

				if ( tool_y_range != null ) {
					st = new StringTokenizer(tool_y_range);
					y_lo = YConvert.toDouble(st.nextToken());
					if ( st.hasMoreTokens() ) y_hi = YConvert.toDouble(st.nextToken());
				}
			} catch (IllegalArgumentException e) {
				ApplicationController.vShowError("Error converting spatial coordinates: " + e);
			}

			if ( sToolType.equals("PT") ) {
				miToolType = TOOL_TYPE_PT;
				x_hi = x_lo;
				y_hi = y_lo;
			} else if ( sToolType.equals("X") ) {
				miToolType = TOOL_TYPE_X;
				y_hi = y_lo;
			} else if ( sToolType.equals("Y") ) {
				miToolType = TOOL_TYPE_Y;
				x_hi = x_lo;
			} else { // default to XY
				miToolType = TOOL_TYPE_XY;
			}

			boolean need_to_center; // todo doesn't seem to be used
			if ( x_lo < grid.domain_X[LO] || x_lo > grid.domain_X[HI] )	{ x_lo = grid.domain_X[LO]; need_to_center = true; }
			if ( x_hi < grid.domain_X[LO] || x_hi > grid.domain_X[HI] ) { x_hi = grid.domain_X[HI]; need_to_center = true; }
			if ( y_lo < grid.domain_Y[LO] || y_lo > grid.domain_Y[HI] ) { y_lo = grid.domain_Y[LO]; need_to_center = true; }
			if ( y_hi < grid.domain_Y[LO] || y_hi > grid.domain_Y[HI] ) { y_hi = grid.domain_Y[HI]; need_to_center = true; }

			toolArray[0].setRange_X(x_lo, x_hi);
			toolArray[0].setRange_Y(y_lo, y_hi);
			toolArray[0].setSnapping(true, true);
			toolArray[0].setUserBounds(x_lo, x_hi, y_lo, y_hi);

			mMapCanvas.setRegionArray(regionArray);

			XConvert.setRange(-180, 180);
			YConvert.setRange(-90.0, 90.0);
			toolArray[0].setRange_X(-180, 180);
			toolArray[0].setRange_Y(-90.0, 90.0);

			vUpdateSpatialCoordinates();

			JButton jbuttonScrollRight = new JButton(">");
			jbuttonScrollRight.setMargin(new Insets(0, 1, 0, 1));
			JButton jbuttonScrollLeft = new JButton("<");
			jbuttonScrollLeft.setMargin(new Insets(0, 1, 0, 1));
			final ButtonModel buttonmodelScrollRight = jbuttonScrollRight.getModel();
			final ButtonModel buttonmodelScrollLeft = jbuttonScrollLeft.getModel();
			jbuttonScrollRight.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent event) {
						if( buttonmodelScrollRight.isPressed() ){
							mMapCanvas.vScrollRight();
						} else {
							mMapCanvas.vScrollStop();
						}
					}
				}
			);
			jbuttonScrollLeft.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent event) {
						if( buttonmodelScrollLeft.isPressed() ) {
							mMapCanvas.vScrollLeft();
						} else {
							mMapCanvas.vScrollStop();
						}
					}
				}
			);
			panelCanvas = new JPanel();
			panelCanvas.setLayout(new BorderLayout());
			panelCanvas.setBorder(BorderFactory.createEtchedBorder());
			panelCanvas.add(mMapCanvas, BorderLayout.CENTER);
			jbuttonScrollRight.setMaximumSize(new Dimension(50, mMapCanvas.getHeight_Clip()));
			jbuttonScrollLeft.setMaximumSize(new Dimension(50, mMapCanvas.getHeight_Clip()));
			panelCanvas.add(jbuttonScrollLeft, BorderLayout.WEST);
			panelCanvas.add(jbuttonScrollRight, BorderLayout.EAST);
			panelCanvas.add(jbuttonScrollLeft, BorderLayout.WEST);

			return true;

		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	static Gazetteer mGazetteer; // there should be only one gazetteer
	private boolean zInitializeGazetteer( StringBuffer sbError ){
		try {
			if( mGazetteer == null ){
				mGazetteer = new Gazetteer();
	    		String sResourcePath = ConfigurationManager.getInstance().getProperty_PATH_Gazetteer();
		    	if( !mGazetteer.zLoad(sResourcePath, sbError) ){
			    	sbError.insert(0, "failed to load " + sResourcePath + ": ");
				    return false;
				}
			}
			String[] asTopics = mGazetteer.getTopics();
			if( asTopics == null ){
				sbError.append("gazetteer has no topics");
				return false;
			}
			if( asTopics.length < 1 ){
				sbError.append("gazetteer has no topics");
				return false;
			}
			String[] asEntries = {};
			final JList jlistTopic = new JList(asTopics);
		    //jlistTopic.setPreferredSize(new Dimension(120, 120));
			final JList jlistEntries = new JList(asEntries);
			final JList jlistCustom = new JList(mGazetteer.getCustom());
			mjlistCustom = jlistCustom;
			jlistTopic.addListSelectionListener(
				new ListSelectionListener(){
					public void valueChanged(ListSelectionEvent e) {
						int xTopic_0 = jlistTopic.getSelectedIndex();
						if (xTopic_0 < 0) {
							jlistEntries.setListData(new Vector());
						} else {
							Object[] aoEntries = mGazetteer.getEntriesForTopic(xTopic_0);
							if( aoEntries == null ){
								jlistEntries.setListData(new Vector());
						    } else {
								jlistEntries.setListData(aoEntries);
							}
							jlistEntries.revalidate();
							Panel_SpatialTemporal.this.validate();
						}
					}
				}
			);
			JScrollPane jspTopics = new JScrollPane(jlistTopic);
			jlistEntries.addListSelectionListener(
				new ListSelectionListener(){
					public void valueChanged(ListSelectionEvent e) {
						int xTopic_0 = jlistTopic.getSelectedIndex();
						int xEntry_0 = jlistEntries.getSelectedIndex();
						Panel_SpatialTemporal.this.vUpdateMapFromGazetteer(xTopic_0, xEntry_0);
					}
				}
			);
			mjlistCustom.addListSelectionListener(
				new ListSelectionListener(){
					public void valueChanged(ListSelectionEvent e) {
						int xCustom_0 = mjlistCustom.getSelectedIndex();
						if( xCustom_0 == -1 ) return; // nothing selected
						Panel_SpatialTemporal.this.vUpdateMapFromGazetteer(xCustom_0);
					}
				}
			);
			final JButton jbuttonAddCustom = new JButton("Add");
			final JButton jbuttonDeleteCustom = new JButton("Delete");
			jbuttonDeleteCustom.setEnabled(false);
			JScrollPane jspEntries = new JScrollPane(jlistEntries);
			jlistCustom.addListSelectionListener(
				new ListSelectionListener(){
					public void valueChanged(ListSelectionEvent e) {
						int xCustom_0 = jlistCustom.getSelectedIndex();
						if (xCustom_0 < 0) {
							jbuttonDeleteCustom.setEnabled(false);
						} else {
							jbuttonDeleteCustom.setEnabled(true);
							Panel_SpatialTemporal.this.vUpdateMapFromGazetteer(xCustom_0);
						}
					}
				}
			);
			JScrollPane jspCustom = new JScrollPane(jlistCustom);
			JPanel jpanelCustom = new JPanel();

			// Add Custom
			jbuttonAddCustom.addActionListener(
				new ActionListener(){
				    public void actionPerformed(ActionEvent event) {
					    Panel_SpatialTemporal.this.vAction_AddCustom();
				    }
			    }
			);

			// Delete Custom
			jbuttonDeleteCustom.addActionListener(
				new ActionListener(){
				    public void actionPerformed(ActionEvent event) {
					    Panel_SpatialTemporal.this.vAction_DeleteCustom();
				    }
			    }
			);

			// layout custom panel
			jpanelCustom.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jpanelCustom.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH; gbc.gridwidth = 2; gbc.gridheight = 1; gbc.weightx = 1.0; gbc.weighty = 1.0;
			jpanelCustom.add( jspCustom, gbc );
			gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.gridheight = 1; gbc.weightx = 0.0; gbc.weighty = 0.0;
			jpanelCustom.add( jbuttonAddCustom, gbc );
			gbc.gridx = 1; gbc.gridy = 1;
			jpanelCustom.add( jbuttonDeleteCustom, gbc );

			panelGazetteer = new JPanel();
		    panelGazetteer.setPreferredSize(new Dimension(500, 160));
			panelGazetteer.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
			panelGazetteer.setLayout(new BoxLayout(panelGazetteer, BoxLayout.X_AXIS));
			panelGazetteer.add(jspTopics);
			panelGazetteer.add(jspEntries);
			panelGazetteer.add(jpanelCustom);
			return true;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}

	}

	String getCoordinatesNSWE(){
		StringBuffer sb = new StringBuffer();
		double dbl = 0;
		dbl = this.getNorthernmost();
		sb.append(Math.abs(dbl));
		if( dbl >= 0 ) sb.append("N "); else sb.append("S ");
		dbl = this.getSouthernmost();
		sb.append(Math.abs(dbl));
		if( dbl >= 0 ) sb.append("N "); else sb.append("S ");
		dbl = this.getWesternmost();
		sb.append(Math.abs(dbl));
		if( dbl >= 0 ) sb.append("E "); else sb.append("W ");
		dbl = this.getEasternmost();
		sb.append(Math.abs(dbl));
		if( dbl >= 0 ) sb.append("E"); else sb.append("W");
		return sb.toString();
	}

    void vAction_AddCustom(){
		StringBuffer sbError = new StringBuffer(80);
		String sResponse = null;
		try {
			String sRegionCoordinates = this.getCoordinatesNSWE();
			while( true ){
				boolean zNoDigits = true;
				sResponse = JOptionPane.showInputDialog(this, "Enter label for this region (" + sRegionCoordinates + "):", "Add Custom Gazetteer Entry", JOptionPane.OK_CANCEL_OPTION);
				if( sResponse == null ) return;
				for( int xChar = 0; xChar < sResponse.length(); xChar++ ){
					if( Character.isDigit( sResponse.charAt(xChar) ) ){
						JOptionPane.showMessageDialog(this, "No digits are allowed in the region label.");
						zNoDigits = false;
					}
				}
				if( zNoDigits ) break;
			}
			if( mGazetteer.zAddCustom( sResponse, sRegionCoordinates, sbError) ){
				if( sbError.length() > 0 ) ApplicationController.vShowWarning("While adding custom gazetteer entry " + sResponse + ": " + sbError);
				mjlistCustom.setListData(mGazetteer.getCustom());
			} else {
				ApplicationController.vShowError("While adding custom gazetteer entry " + sResponse + ": " + sbError);
			}
		} catch(Exception ex) {
			sbError.append("While adding custom gazetteer entry: ");
			Utility.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError("While adding custom gazetteer entry " + sResponse + ": " + sbError);
		}
	}

    void vAction_DeleteCustom(){
		StringBuffer sbError = new StringBuffer(80);
		try {
			if( mjlistCustom.getSelectedIndex() < 0 ) return;
			String sLabelToDelete = mjlistCustom.getSelectedValue().toString();
			if( this.mGazetteer.zDeleteCustom( sLabelToDelete, sbError) ){
				if( sbError.length() > 0 ) ApplicationController.vShowWarning("While deleting custom gazetteer entry " + sLabelToDelete + ": " + sbError);
				mjlistCustom.setListData(mGazetteer.getCustom());
			} else {
				ApplicationController.vShowError("While deleting custom gazetteer entry " + sLabelToDelete + ": " + sbError);
			}
		} catch(Exception ex) {
			sbError.append("While adding custom gazetteer entry: ");
			Utility.vUnexpectedError(ex, sbError);
		}
	}

	int mLastSelection = -1;
	private void vUpdateMapFromGazetteer(int xTopic_0, int xEntry_0){
		if( xEntry_0 == mLastSelection ) return;
		if (xTopic_0 < 0 || xEntry_0 < 0) {
			// do nothing
		} else {
			String sNorth = mGazetteer.getEntry_North(xTopic_0, xEntry_0);
			String sSouth = mGazetteer.getEntry_South(xTopic_0, xEntry_0);
			String sWest = mGazetteer.getEntry_West(xTopic_0, xEntry_0);
			String sEast = mGazetteer.getEntry_East(xTopic_0, xEntry_0);
			vUpdateMap( sNorth, sSouth, sWest, sEast );
			mLastSelection = xEntry_0;
		}
	}

	private void vUpdateMapFromGazetteer(int xCustom_0){
		if( xCustom_0 == mLastSelection ) return;
		if (xCustom_0 < 0) {
			// do nothing
		} else {
			if( xCustom_0 >= mGazetteer.getCustomCount() ) return; // invalid entry
			String sNorth = mGazetteer.getCustom_North(xCustom_0);
			String sSouth = mGazetteer.getCustom_South(xCustom_0);
			String sWest = mGazetteer.getCustom_West(xCustom_0);
			String sEast = mGazetteer.getCustom_East(xCustom_0);
			vUpdateMap( sNorth, sSouth, sWest, sEast );
			mLastSelection = xCustom_0;
		}
	}

	void vUpdateMap( String sNorth, String sSouth, String sWest, String sEast ){
		jtfNorth.setText(sNorth);
		jtfSouth.setText(sSouth);
		jtfWest.setText(sWest);
		jtfEast.setText(sEast);
		vUpdateMarquee(jtfNorth);
		mzApply = true;
		buttonClear.setEnabled(true);
	}

	private boolean zInitializeGUI( StringBuffer sbError ){

		try {
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();

			Font textFont = new Font("Courier", Font.PLAIN, 12);
			jtfNorth = new JTextField("", 8);
			jtfWest  = new JTextField("", 8);
			jtfEast  = new JTextField("", 8);
			jtfSouth = new JTextField("", 8);
			jtfNorth.setFont(textFont);
			jtfSouth.setFont(textFont);
			jtfEast.setFont(textFont);
			jtfWest.setFont(textFont);

			ActionListener actionEnter = new ActionListener(){
				public void actionPerformed(ActionEvent e){
					vUpdateMarquee((JTextField)e.getSource());
				}
			};
			jtfNorth.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,false), JComponent.WHEN_FOCUSED);
			jtfNorth.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_N,0,false), JComponent.WHEN_FOCUSED);
			jtfNorth.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_S,0,false), JComponent.WHEN_FOCUSED);
			jtfWest.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,false), JComponent.WHEN_FOCUSED);
			jtfWest.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_W,0,false), JComponent.WHEN_FOCUSED);
			jtfWest.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_E,0,false), JComponent.WHEN_FOCUSED);
			jtfEast.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,false), JComponent.WHEN_FOCUSED);
			jtfEast.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_W,0,false), JComponent.WHEN_FOCUSED);
			jtfEast.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_E,0,false), JComponent.WHEN_FOCUSED);
			jtfSouth.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0,false), JComponent.WHEN_FOCUSED);
			jtfSouth.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_N,0,false), JComponent.WHEN_FOCUSED);
			jtfSouth.registerKeyboardAction(actionEnter, KeyStroke.getKeyStroke(KeyEvent.VK_S,0,false), JComponent.WHEN_FOCUSED);

			// lat and lon text fields
			JPanel panelCoordinates = new JPanel();
			panelCoordinates.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			panelCoordinates.setLayout(gridbag);

			c.gridx = 1;
			c.gridy = 0;
			c.gridwidth = 2;
			c.gridheight = 1;
			c.anchor = GridBagConstraints.CENTER;
			c.insets.left = 4;
			c.insets.right = 4;
			gridbag.setConstraints(jtfNorth, c);
			panelCoordinates.add(jtfNorth);

			c.gridx = 0;
			c.gridy = 1;
			gridbag.setConstraints(jtfWest, c);
			panelCoordinates.add(jtfWest);

			c.gridx = 2;
			c.gridy = 1;
			gridbag.setConstraints(jtfEast, c);
			panelCoordinates.add(jtfEast);

			c.gridx = 1;
			c.gridy = 2;
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(jtfSouth, c);
			panelCoordinates.add(jtfSouth);

			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			c.insets.top = 4;
			c.insets.bottom = 4;
			gridbag.setConstraints(panelCoordinates, c);

			// Button Panel
			JButton buttonGlobal = new JButton("Global");
			buttonGlobal.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						Panel_SpatialTemporal.this.setGlobal();
						mzApply = true;
						buttonClear.setEnabled(true);
					}
				}
			);
			buttonClear = new JButton("Clear");
			buttonClear.setEnabled(false);
			buttonClear.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						mzApply = false;
						Panel_SpatialTemporal.this.jtfNorth.setText("90");
						Panel_SpatialTemporal.this.jtfSouth.setText("-90");
						Panel_SpatialTemporal.this.jtfEast.setText("-180");
						Panel_SpatialTemporal.this.jtfWest.setText("180");
						vUpdateMarquee(jtfNorth);
						jtfTemporal_FromYear.setText("");
						jtfTemporal_FromMonth.setText("");
						jtfTemporal_FromDay.setText("");
						jtfTemporal_ToYear.setText("");
						jtfTemporal_ToMonth.setText("");
						jtfTemporal_ToDay.setText("");
						mzApply = false;
						buttonClear.setEnabled(false);
					}
				}
			);
			JButton buttonZoomIn = new JButton("Zoom In");
			buttonZoomIn.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						try {
							mMapCanvas.zoom_in();
						} catch(Exception ex) {}
					}
				}
			);
			JButton buttonZoomOut = new JButton("Zoom Out");
			buttonZoomOut.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent event) {
						try {
							mMapCanvas.zoom_out();
						} catch(Exception ex) {}
					}
				}
			);
			Font buttonFont = new Font("TimesRoman", Font.PLAIN, 12);
			buttonZoomIn.setFont(buttonFont);
			buttonZoomOut.setFont(buttonFont);
			buttonGlobal.setFont(buttonFont);
			buttonClear.setFont(buttonFont);

			JPanel panelMapButtons = new JPanel();
			panelMapButtons.setLayout(new BoxLayout(panelMapButtons, BoxLayout.X_AXIS));
			panelMapButtons.setAlignmentX(CENTER_ALIGNMENT);
			panelMapButtons.add(Box.createHorizontalStrut(10));
			panelMapButtons.add(buttonClear);
			panelMapButtons.add(buttonGlobal);
			panelMapButtons.add(Box.createHorizontalStrut(3));
			panelMapButtons.add(buttonZoomIn);
			panelMapButtons.add(Box.createHorizontalStrut(3));
			panelMapButtons.add(buttonZoomOut);

			JPanel panelMapControls = new JPanel();
			panelMapControls.setBorder(BorderFactory.createEtchedBorder());
			panelMapControls.setLayout(new BoxLayout(panelMapControls, BoxLayout.Y_AXIS));
			panelMapControls.setAlignmentY(Box.TOP_ALIGNMENT);
			panelMapControls.add(panelMapButtons);
			panelMapControls.add(Box.createVerticalStrut(2));
			panelMapControls.add(panelCoordinates);

// no longer used
//			JButton jbuttonClearTemporal = new JButton("Clear");
//			jbuttonClearTemporal.addActionListener(
//				new ActionListener(){
//					public void actionPerformed(ActionEvent event) {
//						try {
//							jtfTemporal_FromYear.setText("");
//							jtfTemporal_FromMonth.setText("");
//							jtfTemporal_FromDay.setText("");
//							jtfTemporal_ToYear.setText("");
//							jtfTemporal_ToMonth.setText("");
//							jtfTemporal_ToDay.setText("");
//						} catch(Exception ex) {}
//					}
//				}
//			);

			// no longer used
/*
			String[] asConstraintQualifiers = { "=", ">", "<", ">=", "<=" };
			jcbTemporalQualifier_From = new JComboBox(asConstraintQualifiers);
			jcbTemporalQualifier_From.setMinimumSize(new Dimension(50, 25));
			jcbTemporalQualifier_From.setPreferredSize(new Dimension(50, 25));
			jcbTemporalQualifier_To = new JComboBox(asConstraintQualifiers);
			jcbTemporalQualifier_To.setMinimumSize(new Dimension(50, 25));
			jcbTemporalQualifier_To.setPreferredSize(new Dimension(50, 25));

			JPanel panelTemporal_From = new JPanel();
			panelTemporal_From.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			panelTemporal_From.setMaximumSize(new Dimension(50, 50));
			panelTemporal_From.setLayout(new BoxLayout(panelTemporal_From, BoxLayout.X_AXIS));
			panelTemporal_From.setAlignmentX(LEFT_ALIGNMENT);
			panelTemporal_From.add(Box.createHorizontalStrut(4));
			// panelTemporal_From.add(jcbTemporalQualifier_From);
			// panelTemporal_From.add(Box.createHorizontalStrut(2));
			panelTemporal_From.add(jtfTemporal_FromYear);
			panelTemporal_From.add(Box.createHorizontalStrut(2));
			panelTemporal_From.add(jtfTemporal_FromMonth);
			panelTemporal_From.add(Box.createHorizontalStrut(2));
			panelTemporal_From.add(jtfTemporal_FromDay);
			JPanel panelTemporal_To = new JPanel();
			panelTemporal_To.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			panelTemporal_To.setLayout(new BoxLayout(panelTemporal_To, BoxLayout.X_AXIS));
			panelTemporal_To.setMaximumSize(new Dimension(50, 50));
			panelTemporal_To.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			panelTemporal_To.add(Box.createHorizontalStrut(4));
			// panelTemporal_To.add(jcbTemporalQualifier_To);
			// panelTemporal_To.add(Box.createHorizontalStrut(2));
			panelTemporal_To.add(jtfTemporal_ToYear);
			panelTemporal_To.add(Box.createHorizontalStrut(2));
			panelTemporal_To.add(jtfTemporal_ToMonth);
			panelTemporal_To.add(Box.createHorizontalStrut(2));
			panelTemporal_To.add(jtfTemporal_ToDay);
//			panelTemporal_To.add(Box.createHorizontalStrut(4));
//			panelTemporal_To.add(jbuttonClearTemporal);
//			panelTemporal_To.add(Box.createHorizontalStrut(4));
*/
			JPanel panelTemporal = new JPanel();
			Border borderTemporal = BorderFactory.createEtchedBorder();
			panelTemporal.setBorder(BorderFactory.createTitledBorder(borderTemporal, "Temporal Constraint"));
			GridBagLayout layoutTemporal = new GridBagLayout();
			panelTemporal.setLayout(layoutTemporal);
			GridBagConstraints gbc_temporal = new GridBagConstraints();
			gbc_temporal.fill = GridBagConstraints.NONE;


			// top margin
			gbc_temporal.gridy = 0; gbc_temporal.weightx = 0.0; gbc_temporal.weighty = 1.0;
			gbc_temporal.gridx = 0; panelTemporal.add(Box.createVerticalGlue(), gbc_temporal);

			// header
			gbc_temporal.gridy = 1; gbc_temporal.weightx = 0.0; gbc_temporal.weighty = 0.0;
			gbc_temporal.gridx = 0; panelTemporal.add(Box.createHorizontalStrut(5), gbc_temporal);
			gbc_temporal.gridx = 1; panelTemporal.add(Box.createHorizontalStrut(4), gbc_temporal);
			gbc_temporal.gridx = 2; panelTemporal.add(new JLabel("  YEAR"), gbc_temporal);
			gbc_temporal.gridx = 3; panelTemporal.add(Box.createHorizontalStrut(3), gbc_temporal);
			gbc_temporal.gridx = 4; panelTemporal.add(new JLabel("MON"), gbc_temporal);
			gbc_temporal.gridx = 5; panelTemporal.add(Box.createHorizontalStrut(3), gbc_temporal);
			gbc_temporal.gridx = 6; panelTemporal.add(new JLabel("DAY"), gbc_temporal);
			gbc_temporal.weightx = 1.0;
			gbc_temporal.gridx = 7; panelTemporal.add(Box.createHorizontalStrut(5), gbc_temporal);

			// from
			JPanel panelFrom = new JPanel();
			panelFrom.setLayout(new BoxLayout(panelFrom, BoxLayout.X_AXIS));
			JLabel labelFrom = new JLabel("From:");
			labelFrom.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
			panelFrom.add(Box.createHorizontalStrut(10));
			panelFrom.add(labelFrom);
			gbc_temporal.gridy = 2; gbc_temporal.weightx = 0.0;
			gbc_temporal.gridx = 0; gbc_temporal.anchor = GridBagConstraints.EAST; panelTemporal.add(panelFrom, gbc_temporal);
			gbc_temporal.gridx = 1; gbc_temporal.anchor = GridBagConstraints.WEST; panelTemporal.add(Box.createHorizontalStrut(4), gbc_temporal);
			gbc_temporal.gridx = 2; panelTemporal.add(jtfTemporal_FromYear, gbc_temporal);
			gbc_temporal.gridx = 3; panelTemporal.add(Box.createHorizontalStrut(3), gbc_temporal);
			gbc_temporal.gridx = 4; panelTemporal.add(jtfTemporal_FromMonth, gbc_temporal);
			gbc_temporal.gridx = 5; panelTemporal.add(Box.createHorizontalStrut(3), gbc_temporal);
			gbc_temporal.gridx = 6; panelTemporal.add(jtfTemporal_FromDay, gbc_temporal);
			gbc_temporal.weightx = 1.0;
			gbc_temporal.gridx = 7; panelTemporal.add(Box.createHorizontalStrut(5), gbc_temporal);

			// to
			JPanel panelTo = new JPanel();
			panelTo.setLayout(new BoxLayout(panelTo, BoxLayout.X_AXIS));
			JLabel labelTo = new JLabel("To:");
			labelTo.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
			panelTo.add(Box.createHorizontalStrut(10));
			panelTo.add(labelTo);
			gbc_temporal.gridy = 3; gbc_temporal.weightx = 0.0;
			gbc_temporal.gridx = 0; gbc_temporal.anchor = GridBagConstraints.EAST; panelTemporal.add(panelTo, gbc_temporal);
			gbc_temporal.gridx = 1; gbc_temporal.anchor = GridBagConstraints.WEST; panelTemporal.add(Box.createHorizontalStrut(4), gbc_temporal);
			gbc_temporal.gridx = 2; panelTemporal.add(jtfTemporal_ToYear, gbc_temporal);
			gbc_temporal.gridx = 3; panelTemporal.add(Box.createHorizontalStrut(3), gbc_temporal);
			gbc_temporal.gridx = 4; panelTemporal.add(jtfTemporal_ToMonth, gbc_temporal);
			gbc_temporal.gridx = 5; panelTemporal.add(Box.createHorizontalStrut(3), gbc_temporal);
			gbc_temporal.gridx = 6; panelTemporal.add(jtfTemporal_ToDay, gbc_temporal);
			gbc_temporal.weightx = 1.0;
			gbc_temporal.gridx = 7; panelTemporal.add(Box.createHorizontalStrut(5), gbc_temporal);

			// bottom margin
			gbc_temporal.gridy = 4; gbc_temporal.weightx = 0.0; gbc_temporal.weighty = 1.0;
			gbc_temporal.gridx = 0; panelTemporal.add(Box.createVerticalGlue(), gbc_temporal);

			panelEast = new JPanel();
			panelEast.setLayout(new BoxLayout(panelEast, BoxLayout.Y_AXIS));
			panelEast.add(panelMapControls);
			panelEast.add(panelTemporal);

			return true;

		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}

	}

	public void setGazetteerEnabled( boolean z ){
		mzGazetteerEnabled = z;
// todo update gui
	}

	public boolean isGazetteerEnabled(){ return mzGazetteerEnabled; }
	public boolean getGazetteerEnabled(){ return mzGazetteerEnabled; }

	public void setGlobal(){
		jtfNorth.setText("90 N");
		jtfSouth.setText("90 S");
		jtfWest.setText("180 W");
		jtfEast.setText("180 E");
		vUpdateMarquee(jtfNorth);
	}

	public void mouseEntered(MouseEvent e) {//do nothing
	}

	public void mousePressed(MouseEvent e) {//keep in track if in map
	}

	public void mouseReleased(MouseEvent e) {//do nothing
	}

	public void mouseExited(MouseEvent e) {//do nothing
	}

	public void mouseClicked(MouseEvent e) {//zoom
	}

	public void mouseDragged(MouseEvent e) {//do nothing
		Object o = e.getSource();
		if(o == mMapCanvas) {
			XConvert.setRange(grid.domain_X[LO],grid.domain_X[HI]);
			YConvert.setRange(grid.domain_Y[LO],grid.domain_Y[HI]);
			try {
				jtfNorth.setText(YConvert.toString((int)mMapCanvas.getTool().user_Y[HI]));
				jtfSouth.setText(YConvert.toString((int)mMapCanvas.getTool().user_Y[LO]));
				jtfWest.setText(XConvert.toString((int)mMapCanvas.getTool().user_X[LO]));
				jtfEast.setText(XConvert.toString((int)mMapCanvas.getTool().user_X[HI]));
				mzApply = true;
				buttonClear.setEnabled(true);
			}
			catch (IllegalArgumentException ex) {
				System.out.println("During setting text fields: " + ex);
			}
		}
	}

	public void mouseMoved(MouseEvent e) {//do nothing
	}

	private void vUpdateMarquee(JTextField target) {

		double top = 0.0;
		double bottom = 0.0;
		double left = 0.0;
		double right = 0.0;
		try {

			switch (miToolType) {
				case TOOL_TYPE_PT:
					if ( target == jtfNorth ) {
						bottom = top = YConvert.toDouble(jtfNorth.getText());
						right = left = XConvert.toDouble(jtfWest.getText());
					} else if ( target == jtfSouth ) {
						bottom = top = YConvert.toDouble(jtfSouth.getText());
						right = left = XConvert.toDouble(jtfWest.getText());
					} else if ( target == jtfEast ) {
						bottom = top = YConvert.toDouble(jtfNorth.getText());
						right = left = XConvert.toDouble(jtfEast.getText());
					} else if ( target == jtfWest ) {
						bottom = top = YConvert.toDouble(jtfNorth.getText());
						right = left = XConvert.toDouble(jtfWest.getText());
					} else {
						bottom = top = YConvert.toDouble(jtfNorth.getText());
						right = left = XConvert.toDouble(jtfWest.getText());
					}
					break;

				case TOOL_TYPE_X:
					if ( target == jtfNorth ) {
						bottom = top = YConvert.toDouble(jtfNorth.getText());
					} else if ( target == jtfSouth ) {
						bottom = top = YConvert.toDouble(jtfSouth.getText());
					} else {
						bottom = top = YConvert.toDouble(jtfNorth.getText());
					}
					left = XConvert.toDouble(jtfWest.getText());
					right = XConvert.toDouble(jtfEast.getText());
					break;

				case TOOL_TYPE_Y:
					if ( target == jtfWest ) {
						right = left = XConvert.toDouble(jtfWest.getText());
					} else if ( target == jtfEast ) {
						right = left = XConvert.toDouble(jtfEast.getText());
					} else {
						right = left = XConvert.toDouble(jtfWest.getText());
					}
					top = YConvert.toDouble(jtfNorth.getText());
					bottom = YConvert.toDouble(jtfSouth.getText());
					break;

				case TOOL_TYPE_XY:
				default:
					top = YConvert.toDouble(jtfNorth.getText());
				    bottom = YConvert.toDouble(jtfSouth.getText());
				    left = XConvert.toDouble(jtfWest.getText());
				    right = XConvert.toDouble(jtfEast.getText());
				    break;
			}

			if ( top < bottom ) {
				double old_bottom = bottom;
				bottom = top;
				top = old_bottom;
			}

			mMapCanvas.getTool().setUserBounds(left, right, bottom, top);
			mMapCanvas.center_tool(1.0);

		} catch (IllegalArgumentException e) {
			ApplicationController.getInstance().vShowWarning("invalid coordinate: " + e);
			// todo beep
			// todo restore previous coordinate
		} finally {
			mMapCanvas.repaint();
			vUpdateSpatialCoordinates();
		}
	}

	public double getWesternmost() { return XConvert.toDouble(jtfWest.getText()); }
	public double getEasternmost() { return XConvert.toDouble(jtfEast.getText()); }
	public double getNorthernmost() { return YConvert.toDouble(jtfNorth.getText() ); }
	public double getSouthernmost() { return YConvert.toDouble(jtfSouth.getText() ); }

	public String getTemporal_FromQualifier(){ return this.jcbTemporalQualifier_From.getSelectedItem().toString(); }
	public String getTemporal_ToQualifier(){ return this.jcbTemporalQualifier_To.getSelectedItem().toString(); }
	public String getTemporal_FromYear(){ return this.jtfTemporal_FromYear.getText(); }
	public String getTemporal_FromMonth(){ return this.jtfTemporal_FromMonth.getText(); }
	public String getTemporal_FromDay(){ return this.jtfTemporal_FromDay.getText(); }
	public String getTemporal_ToYear(){ return this.jtfTemporal_ToYear.getText(); }
	public String getTemporal_ToMonth(){ return this.jtfTemporal_ToMonth.getText(); }
	public String getTemporal_ToDay(){ return this.jtfTemporal_ToDay.getText(); }

	public boolean zHasSpatialConstraint() {
		try {
			if (
					jtfNorth.getText().trim().length() == 0 &&
					jtfWest.getText().trim().length() == 0 &&
					jtfEast.getText().trim().length() == 0 &&
					jtfSouth.getText().trim().length() == 0
					) return false;
			return mzApply;
		} catch(Exception ex) { return false; }
	}

	public boolean zHasTemporalConstraint() {
		try {
			if (
					jtfTemporal_FromYear.getText().trim().length() == 0 &&
					jtfTemporal_FromMonth.getText().trim().length() == 0 &&
					jtfTemporal_FromDay.getText().trim().length() == 0 &&
					jtfTemporal_ToYear.getText().trim().length() == 0 &&
					jtfTemporal_ToMonth.getText().trim().length() == 0 &&
					jtfTemporal_ToDay.getText().trim().length() == 0
					) return false;
			return true;
		} catch(Exception ex) { return false; }
	}

	public void vUpdateSpatialCoordinates() {
		XConvert.setRange(grid.domain_X[LO],grid.domain_X[HI]);
		YConvert.setRange(grid.domain_Y[LO],grid.domain_Y[HI]);
		try {
			if ( miToolType == TOOL_TYPE_PT || miToolType == TOOL_TYPE_X ) {
				jtfNorth.setText(YConvert.toString(mMapCanvas.getTool().user_Y[PT]));
				jtfSouth.setText(YConvert.toString(mMapCanvas.getTool().user_Y[PT]));
			} else {
				jtfNorth.setText(YConvert.toString(mMapCanvas.getTool().user_Y[HI]));
				jtfSouth.setText(YConvert.toString(mMapCanvas.getTool().user_Y[LO]));
			}
			if ( miToolType == TOOL_TYPE_PT || miToolType == TOOL_TYPE_Y ) {
				jtfEast.setText(XConvert.toString(mMapCanvas.getTool().user_X[PT]));
				jtfWest.setText(XConvert.toString(mMapCanvas.getTool().user_X[PT]));
			} else {
				jtfEast.setText(XConvert.toString(mMapCanvas.getTool().user_X[HI]));
				jtfWest.setText(XConvert.toString(mMapCanvas.getTool().user_X[LO]));
			}
		} catch (IllegalArgumentException e) {
			ApplicationController.getInstance().vShowError("Error updating spatial coordinates: " + e);
		}

	}

	public void setToolColor(String color_name) {
		System.out.println("setToolColor(" + color_name + ")");
		if ( color_name.equals("black") ) {
			mMapCanvas.getTool().setColor(Color.black);
		} else if ( color_name.equals("white") ) {
			mMapCanvas.getTool().setColor(Color.white);
		} else if ( color_name.equals("blue") ) {
			mMapCanvas.getTool().setColor(Color.blue);
		} else if ( color_name.equals("red") ) {
			mMapCanvas.getTool().setColor(Color.red);
		} else if ( color_name.equals("green") ) {
			mMapCanvas.getTool().setColor(Color.green);
		} else if ( color_name.equals("yellow") ) {
			mMapCanvas.getTool().setColor(Color.yellow);
		} else if ( color_name.equals("orange") ) {
			mMapCanvas.getTool().setColor(Color.orange);
		}
		mMapCanvas.repaint();
	}

	public void selectTool(int i) {
		mMapCanvas.selectTool(i);
	}

	/**
	 * Replace a tool in the toolArray.
	 * @param i index of the tool in the toolArray
	 * @param type the type of the new, replacement tool
	 */
	public void setTool(String type) {
		setTool(mMapCanvas.getSelected(),type);
	}

	/**
	 * Replace a tool in the toolArray.
	 * @param i index of the tool in the toolArray
	 * @param type the type of the new, replacement tool
	 */
	public void setTool(int i, String type) {

		System.out.println("setTool(" + i + ", " + type + ")");
		MapTool newTool;
		MapTool oldTool = mMapCanvas.getTool(i);

		if ( type.equals("PT") ) {
			newTool = new PTTool(oldTool.getRectangle(),oldTool.getColor());
			miToolType = TOOL_TYPE_PT;
		} else if ( type.equals("X") ) {
			newTool = new XTool(oldTool.getRectangle(),oldTool.getColor());
			miToolType = TOOL_TYPE_X;
		} else if ( type.equals("Y") ) {
			newTool = new YTool(oldTool.getRectangle(),oldTool.getColor());
			miToolType = TOOL_TYPE_Y;
		} else if ( type.equals("XY") ) {
			newTool = new XYTool(oldTool.getRectangle(),oldTool.getColor());
			miToolType = TOOL_TYPE_XY;
		} else if ( type.equals("XcY") ) {
			newTool = new XcYTool(oldTool.getRectangle(),oldTool.getColor());
			miToolType = TOOL_TYPE_XY;
		} else if ( type.equals("YcX") ) {
			newTool = new YcXTool(oldTool.getRectangle(),oldTool.getColor());
			miToolType = TOOL_TYPE_XY;
		} else if ( type.equals("PTcX") ) {
			newTool = new PTcXTool(oldTool.getRectangle(),oldTool.getColor());
			miToolType = TOOL_TYPE_X;
		} else if ( type.equals("PTcY") ) {
			newTool = new PTcYTool(oldTool.getRectangle(),oldTool.getColor());
			miToolType = TOOL_TYPE_Y;
		} else if ( type.equals("PTcXY") ) {
			newTool = new PTcXYTool(oldTool.getRectangle(),oldTool.getColor());
			miToolType = TOOL_TYPE_XY;
		} else { // default to XY
			newTool = new XYTool(oldTool.getRectangle(),oldTool.getColor());
			miToolType = TOOL_TYPE_XY;
		}

		mMapCanvas.newToolFromOld(i, newTool, oldTool);
		mMapCanvas.repaint();
		vUpdateSpatialCoordinates();
	}

	public void setToolRange(int i, double x_lo, double x_hi, double y_lo, double y_hi) {
//		System.out.println("setToolRange(" + i + ", " + x_lo + ", " + x_hi +
//						   ", " + y_lo + ", " + y_hi + ")");
		XConvert.setRange(x_lo, x_hi);
		YConvert.setRange(y_lo, y_hi);
		toolArray[i].setRange_X(x_lo, x_hi);
		toolArray[i].setRange_Y(y_lo, y_hi);
		positionTool(i, x_lo, x_hi, y_lo, y_hi);
	}


	public void restrictToolRange(int i, double x_lo, double x_hi, double y_lo, double y_hi) {
		System.out.println("restrictToolRange(" + i + ", " + x_lo + ", " + x_hi +
						   ", " + y_lo + ", " + y_hi + ")");
		double [] xvals = {mMapCanvas.getTool().user_X[LO], mMapCanvas.getTool().user_X[HI]};
		double [] yvals = {mMapCanvas.getTool().user_Y[LO], mMapCanvas.getTool().user_Y[HI]};

// The converters (ie. XConvert) may alter the incoming values
// so we need to use their getRange() method to set the ranges
// for the tool.
		XConvert.setRange(x_lo, x_hi);
		YConvert.setRange(y_lo, y_hi);
		toolArray[i].setRange_X(XConvert.getRange(LO), XConvert.getRange(HI));
		toolArray[i].setRange_Y(YConvert.getRange(LO), YConvert.getRange(HI));

// If there is no intersection, default to the new range.
		try {
			xvals = XConvert.intersectRange(xvals[LO], xvals[HI]);
			yvals = YConvert.intersectRange(yvals[LO], yvals[HI]);
		} catch (IllegalArgumentException e) {
			System.out.println(e);
			System.out.println(e);
			xvals[LO] = XConvert.getRange(LO);
			xvals[HI] = XConvert.getRange(HI);
			yvals[LO] = YConvert.getRange(LO);
			yvals[HI] = YConvert.getRange(HI);
		} finally {
			mMapCanvas.getTool(i).setUserBounds(xvals[LO], xvals[HI], yvals[LO], yvals[HI]);
			mMapCanvas.center_tool(1.0);
			mMapCanvas.repaint();
			vUpdateSpatialCoordinates();
		}
	}


	public void positionTool(int i, double x_lo, double x_hi, double y_lo, double y_hi) {
//		System.out.println("positionTool(" + i + ", " + x_lo + ", " + x_hi +
//						   ", " + y_lo + ", " + y_hi + ")");
		double [] xvals = {x_lo, x_hi};
		double [] yvals = {y_lo, y_hi};

// If there is no intersection, default to the old tool
// values which, presumably, lie within the range.
		try {
			xvals = XConvert.intersectRange(x_lo, x_hi);
			yvals = YConvert.intersectRange(y_lo, y_hi);
		} catch (IllegalArgumentException e) {
			System.out.println(e);
			xvals[LO] = mMapCanvas.getTool().user_X[LO];
			xvals[HI] = mMapCanvas.getTool().user_X[HI];
			yvals[LO] = mMapCanvas.getTool().user_Y[LO];
			yvals[HI] = mMapCanvas.getTool().user_Y[HI];
		} finally {
// An XTool should be able to lie on the bottom of the data region
// but an XYTool should not.
			System.out.println("Tool needs range.  Keeping old tool values.");
			if ( (xvals[LO] == xvals[HI] && mMapCanvas.getTool().needsRange_X) ||
				(yvals[LO] == yvals[HI] && mMapCanvas.getTool().needsRange_Y) ) {
				xvals[LO] = mMapCanvas.getTool().user_X[LO];
				xvals[HI] = mMapCanvas.getTool().user_X[HI];
				yvals[LO] = mMapCanvas.getTool().user_Y[LO];
				yvals[HI] = mMapCanvas.getTool().user_Y[HI];
			}
			mMapCanvas.getTool(i).setUserBounds(xvals[LO], xvals[HI], yvals[LO], yvals[HI]);
			mMapCanvas.center_tool(1.0);
			mMapCanvas.repaint();
			vUpdateSpatialCoordinates();
		}
	}


// JC_TODO: setDelta currently adjusts Delta only for the selected tool
	public void setDelta(double delta_x, double delta_y) {
		System.out.println("setDelta(" + delta_x + ", " + delta_y + ")");
		mMapCanvas.getTool().setDelta_X(delta_x);
		mMapCanvas.getTool().setDelta_Y(delta_y);
		mMapCanvas.selectTool(mMapCanvas.getSelected());
	}


	public void setImage(String sResourcePath, double x_lo, double x_hi, double y_lo, double y_hi) throws Exception {

		Image img=null;
//		System.out.println("setImage(" + img_name + ", " + x_lo + ", " + x_hi +
//						   ", " + y_lo + ", " + y_hi + ")");

		if ( sResourcePath == null ) {
			ApplicationController.getInstance().vShowError("Unable to set spatial image, null input");
			return;
		} else {
			Image image = Utility.imageLoadResource(sResourcePath);
			if( image == null ){
				ApplicationController.vShowError("failed to load map " + sResourcePath);
				return;
			}
			mMapImage = image;
		}

		XConvert.setRange(x_lo, x_hi);
		YConvert.setRange(y_lo, y_hi);
		mMapCanvas.getTool().setRange_X(x_lo,x_hi);
		mMapCanvas.getTool().setRange_Y(y_lo,y_hi);
		grid.setDomain_X(x_lo, x_hi);
		grid.setDomain_Y(y_lo, y_hi);
		mMapCanvas.setGrid(grid);
		mMapCanvas.setImage(mMapImage);

		restrictToolRange(mMapCanvas.getSelected(), x_lo, x_hi, y_lo, y_hi);
	}


	public String get_x_range() {

/*--
// JC_NOTE: The commented out code would return values
// within the range defined in XConvert but this can
// result in (x_hi < x_lo) which Ferret doesn't handle
// properly.  So I've used simpler code which doesn't
// have any Longitude-wrap-around smarts.
//
// The 'smart' code is left here in case we ever wish
// to return to it.

Convert Xout = new ConvertLongitude();
Xout.setRange(XConvert.getRange(LO),XConvert.getRange(HI));

if ( grid.x_type != LONGITUDE_AXIS ) {
Xout = new ConvertLength();
Xout.setRange(XConvert.getRange(LO),XConvert.getRange(HI));
}

switch (tool_type) {
case TOOL_TYPE_PT:
sbuf.append(Xout.toString(mMapCanvas.getTool().user_X[PT]));
break;

case TOOL_TYPE_X:
if ( grid.x_type == LONGITUDE_AXIS && x_lo == x_hi) {
sbuf.append("0.0 360.0");
} else if ( x_hi < x_lo ) {
x_hi += 360.0;
sbuf.append(Xout.toString(x_lo) + " " + Xout.toString(x_hi));
} else {
sbuf.append(Xout.toString(x_lo) + " " + Xout.toString(x_hi));
}
break;

case TOOL_TYPE_Y:
sbuf.append(Xout.toString(mMapCanvas.getTool().user_X[PT]));
break;

case TOOL_TYPE_XY:
if ( grid.x_type == LONGITUDE_AXIS && x_lo == x_hi) {
sbuf.append("0.0 360.0");
} else if ( x_hi < x_lo ) {
x_hi += 360.0;
sbuf.append(Xout.toString(x_lo) + " " + Xout.toString(x_hi));
} else {
sbuf.append(Xout.toString(x_lo) + " " + Xout.toString(x_hi));
}
break;

default:
sbuf.append("");
break;

}
return sbuf.toString();
--*/

		StringBuffer sbuf = new StringBuffer("");
		double x_lo=mMapCanvas.getTool().user_X[LO];
		double x_hi=mMapCanvas.getTool().user_X[HI];

		switch (miToolType) {
			case TOOL_TYPE_PT:
				sbuf.append(mMapCanvas.getTool().user_X[PT]);
				break;

			case TOOL_TYPE_X:
				if ( grid.x_type == LONGITUDE_AXIS && x_lo == x_hi) {
					sbuf.append("-180 180");
				} else if ( x_hi < x_lo ) {
					x_hi += 180;
					sbuf.append(x_lo + " " + x_hi);
				} else {
					sbuf.append(x_lo + " " + x_hi);
				}
				break;

			case TOOL_TYPE_Y:
				sbuf.append(mMapCanvas.getTool().user_X[PT]);
				break;

			case TOOL_TYPE_XY:
				if ( grid.x_type == LONGITUDE_AXIS && x_lo == x_hi) {
					sbuf.append("-180 180");
				} else if ( x_hi < x_lo ) {
					x_hi += 180;
					sbuf.append(x_lo + " " + x_hi);
				} else {
					sbuf.append(x_lo + " " + x_hi);
				}
				break;

			default:
				sbuf.append("");
			break;

		}
		return sbuf.toString();
	}


	public String get_y_range() {
		ConvertLatitude Yout = new ConvertLatitude();
		Yout.setRange(YConvert.getRange(LO),YConvert.getRange(HI));

		StringBuffer sbuf = new StringBuffer("");

		switch (miToolType) {
			case TOOL_TYPE_PT:
				sbuf.append(Yout.toString(mMapCanvas.getTool().user_Y[PT]));
				break;

			case TOOL_TYPE_X:
				sbuf.append(Yout.toString(mMapCanvas.getTool().user_Y[PT]));
				break;

			case TOOL_TYPE_Y:
				sbuf.append(Yout.toString(mMapCanvas.getTool().user_Y[LO]) + " " +
							Yout.toString(mMapCanvas.getTool().user_Y[HI]));
				break;

			case TOOL_TYPE_XY:
				sbuf.append(Yout.toString(mMapCanvas.getTool().user_Y[LO]) + " " +
							Yout.toString(mMapCanvas.getTool().user_Y[HI]));
				break;

			default:
				sbuf.append("");
			break;
		}

		return sbuf.toString();
	}

}

/** note that the order of coordinate bounds in gazetteer must be N S W E */
class Gazetteer {
	private String msLoadedGazetteerPath = null;
	private String[] asTopics = null;
	private String[][] asEntries = null;
	private String[][] asCoordinates = null;
	private String[] asCustom = null;
	private String[] asCustom_Coordinates = null;
	Gazetteer(){}
	public String getGazetteerPath(){ return msLoadedGazetteerPath; }
	public String[] getTopics(){ return asTopics; }
	public String[] getEntriesForTopic( int xTopic_0 ){
		return asEntries[xTopic_0];
	}
	public String[] getCustom(){
		if( asCustom == null ){
			asCustom = new String[0];
		}
		return asCustom;
	}
	public int getCustomCount(){
		if( asCustom == null ) return 0;
		return asCustom.length;
	}
	private String sCoordinate(String s, int xCoordinate_Desired){
		int pos = 0;
		int xCoordinate = 0;
		int eState = 1;
		int lenCoordinates = s.length();
		StringBuffer sbNumber = new StringBuffer();
		char cOrientation = 'N';
		String sNumber, sOrientation;
		while(true){
			char c = s.charAt(pos);
			switch(eState){
				case 1: // before coordinate
					if( Character.isWhitespace(c) ){
						pos++; break; }
					xCoordinate++;
					eState = 2;
					break;
				case 2: // in coordinate start
					if( Character.isWhitespace(c) ){
						eState = 4; pos++; break; }
					if( Character.isDigit(c) || c == '-' ){
						sbNumber.setLength(0);
						sbNumber.append(c);
						eState = 3;
					} else if( c == 'N' || c == 'S' || c == 'W' || c == 'E' ) {
						cOrientation = c;
						eState = 5;
					} else {
						// error todo
					}
					pos++;
					break;
				case 3: // after number start
					if( Character.isWhitespace(c) ){
						eState = 4; pos++; break; }
					if( Character.isDigit(c) || c == '.' ){
						sbNumber.append(c);
					} else if( c == 'N' || c == 'S' || c == 'W' || c == 'E' ) {
						cOrientation = c;
						eState = 5;
					} else {
						// ignore
						// error todo
					}
					pos++;
					break;
				case 4: // after number end
					if( Character.isWhitespace(c) ){
						pos++; break; }
					if( c == 'N' || c == 'S' || c == 'W' || c == 'E' ) {
						cOrientation = c;
						pos++;
						eState = 5; break; }
					if( Character.isDigit(c) ){
						eState = 5; break; }
				case 5: // end of coordinate
					if( xCoordinate == xCoordinate_Desired ){
						String sResult = sbNumber.toString() + " " + cOrientation;
						return sResult;
					}
					eState = 1;
			}
			if( pos >= lenCoordinates ){
				eState = 5;
				pos = lenCoordinates - 1;
			}
		}
	}
	public String getEntry_North( int xTopic_0, int xEntry_0 ){
		return sCoordinate(asCoordinates[xTopic_0][xEntry_0], 1);
	}
	public String getEntry_East( int xTopic_0, int xEntry_0 ){
		return sCoordinate(asCoordinates[xTopic_0][xEntry_0], 4);
	}
	public String getEntry_West( int xTopic_0, int xEntry_0 ){
		return sCoordinate(asCoordinates[xTopic_0][xEntry_0], 3);
	}
	public String getEntry_South( int xTopic_0, int xEntry_0 ){
		return sCoordinate(asCoordinates[xTopic_0][xEntry_0], 2);
	}
	boolean zDeleteCustom( String sLabel, StringBuffer sbError ){
		String sGazetteerPath = getGazetteerPath();
		if( sGazetteerPath == null ){
			sbError.append("No existing gazetteer (there must be a loaded gazetteer before entries can be deleted from it).");
			return false;
		}
		if( sLabel == null ){
			sbError.append("No label given to delete.");
			return false;
		}
		StringBuffer sbGazetteer = new StringBuffer(10000);
		if( !Utility.zLoadStringFile(sGazetteerPath, sbGazetteer, sbError) ){
			sbError.insert(0, "failed to load gazetteer to delete custom entry: ");
			return false;
		}
		int posCustomTopic = Utility.find( sbGazetteer, "*custom", true );
		if( posCustomTopic < 0 ){ // need to add custom topic
			sbError.append("no custom topic in gazetteer");
			return false;
		}
		if( asCustom == null ){
			sbError.append("internal error, no custom array");
			return false;
		} else {
			int ctCustom = asCustom.length;
			String[] asCustom_Buffer = new String[ctCustom - 1];
			String[] asCustom_Coordinates_Buffer = new String[ctCustom - 1];
			int xDeleteLocation = 0; // will be 0 to count
			for( ; xDeleteLocation < ctCustom; xDeleteLocation++ ){
				if( sLabel.compareTo(asCustom[xDeleteLocation]) == 0 ) break; // found the delete location
				asCustom_Buffer[xDeleteLocation] = asCustom[xDeleteLocation];
				asCustom_Coordinates_Buffer[xDeleteLocation] = asCustom_Coordinates[xDeleteLocation];
			}
			for( int xRemainingEntries = xDeleteLocation + 1; xRemainingEntries < ctCustom; xRemainingEntries++ ){
				asCustom_Buffer[xRemainingEntries - 1] = asCustom[xRemainingEntries];
				asCustom_Coordinates_Buffer[xRemainingEntries - 1] = asCustom_Coordinates[xRemainingEntries];
			}
			asCustom = asCustom_Buffer;
			asCustom_Coordinates = asCustom_Coordinates_Buffer;
			int posDeleteLocation_begin = Utility.find( sbGazetteer, sLabel, true, posCustomTopic );
			if( posDeleteLocation_begin < 0 ){
				sbError.append("Unable to find custom entry '" + sLabel + "' in gazetteer file to delete it.");
			} else {
				int posDeleteLocation_end = Utility.find( sbGazetteer, "\n", true, posDeleteLocation_begin ) + 1;
				if( posDeleteLocation_end < 1 ) posDeleteLocation_end = sbGazetteer.length();
				sbGazetteer.delete(posDeleteLocation_begin, posDeleteLocation_end);
			}
		}
		if( !Utility.zSaveStringFile( sGazetteerPath, sbGazetteer, sbError) ){
			sbError.append(sbError);
		}
		return true;
	}
	// if sbError has content but returns true then treat as warning
	boolean zAddCustom( String sLabel, String sCoordinates, StringBuffer sbError ){
		String sGazetteerPath = getGazetteerPath();
		if( sGazetteerPath == null ){
			sbError.append("No existing gazetteer (there must be a loaded gazetteer before entries can be added to it).");
			return false;
		}
		if( sLabel == null ) sLabel = "[user defined]";
		if( sCoordinates == null ) sCoordinates = "0N 0N 0S 0S";
		StringBuffer sbGazetteer = new StringBuffer(10000);
		if( !Utility.zLoadStringFile(sGazetteerPath, sbGazetteer, sbError) ){
			sbError.insert(0, "failed to load gazetteer to add custom entry: ");
			return false;
		}
		int posCustomTopic = Utility.find( sbGazetteer, "*custom", true );
		if( posCustomTopic < 0 ){ // need to add custom topic
			int lenGazetteer = sbGazetteer.length();
			if( lenGazetteer > 0 ) if( sbGazetteer.charAt(lenGazetteer - 1) != '\n' ) sbGazetteer.append("\n");
			sbGazetteer.append("*Custom\n");
		}
		posCustomTopic = Utility.find( sbGazetteer, "*custom", true ); // try to find it again
		if( posCustomTopic < 0 ){
			sbError.append("internal error, unable to add custom topic to buffer");
			return false;
		}
		if( asCustom == null ){
			asCustom = new String[1];
			asCustom_Coordinates = new String[1];
			asCustom[0] = sLabel;
			asCustom_Coordinates[0] = sCoordinates;
			sbGazetteer.append(sLabel).append(" ").append(sCoordinates).append("\n");
		} else {
			int ctCustom = asCustom.length;
			String[] asCustom_Buffer = new String[ctCustom + 1];
			String[] asCustom_Coordinates_Buffer = new String[ctCustom + 1];
			int xInsertLocation = 0; // will be 0 to count
			for( ; xInsertLocation < ctCustom; xInsertLocation++ ){
				if( sLabel.compareTo(asCustom[xInsertLocation]) < 0 ) break; // found the insertion location
				asCustom_Buffer[xInsertLocation] = asCustom[xInsertLocation];
				asCustom_Coordinates_Buffer[xInsertLocation] = asCustom_Coordinates[xInsertLocation];
			}
			asCustom_Buffer[xInsertLocation] = sLabel;
			asCustom_Coordinates_Buffer[xInsertLocation] = sCoordinates;
			for( int xRemainingEntries = xInsertLocation; xRemainingEntries < ctCustom; xRemainingEntries++ ){
				asCustom_Buffer[xRemainingEntries + 1] = asCustom[xRemainingEntries];
				asCustom_Coordinates_Buffer[xRemainingEntries + 1] = asCustom_Coordinates[xRemainingEntries];
			}
			asCustom = asCustom_Buffer;
			asCustom_Coordinates = asCustom_Coordinates_Buffer;
			int posInsertLocation;
			if( xInsertLocation == ctCustom ){ // add to end
				posInsertLocation = -1;
			} else {
				posInsertLocation = Utility.find( sbGazetteer, asCustom[xInsertLocation + 1], true, posCustomTopic );
			}
			int lenGazetteer = sbGazetteer.length();
			if( posInsertLocation < 0 ){
				if( sbGazetteer.charAt(lenGazetteer - 1) != '\n' ){
					sbGazetteer.append("\n");
				}
				sbGazetteer.append(sLabel).append(" ").append(sCoordinates).append("\n");
			} else {
				String sInsertion = sLabel + " " + sCoordinates + "\n";
				sbGazetteer.insert(posInsertLocation, sInsertion);
			}
		}
		if( !Utility.zSaveStringFile( sGazetteerPath, sbGazetteer, sbError) ){
			sbError.append(sbError);
		}
		return true;
	}
	public String getCustom_North( int xCustom_0 ){
		return sCoordinate(asCustom_Coordinates[xCustom_0], 1);
	}
	public String getCustom_East( int xCustom_0 ){
		return sCoordinate(asCustom_Coordinates[xCustom_0], 4);
	}
	public String getCustom_West( int xCustom_0 ){
		return sCoordinate(asCustom_Coordinates[xCustom_0], 3);
	}
	public String getCustom_South( int xCustom_0 ){
		return sCoordinate(asCustom_Coordinates[xCustom_0], 2);
	}
	boolean zLoad( String sGazetteerPath, StringBuffer sbError ){ // may be a file or a directory
		StringBuffer sbGazetteer = new StringBuffer(10000);
		if( !Utility.zLoadStringFile(sGazetteerPath, sbGazetteer, sbError) ){
			sbError.insert(0, "failed to load: ");
			return false;
		}
		msLoadedGazetteerPath = sGazetteerPath;
		int ctTopic = 0;
		int pos = 0;
		int eState = 1; // before entry
		int eLastState = 0;
		int posLast = 0;
		StringBuffer sb = new StringBuffer(50);
		int lenBuffer = sbGazetteer.length();

		// count topics
		boolean zHasCustom = false;
		while(true){
			if( eState == eLastState && pos == posLast ){
				sbError.append("circular scan error while counting at " + pos + " : " + eState);
				return false;
			} else {
				eLastState = eState;
				posLast = pos;
			}
			char c = sbGazetteer.charAt(pos);
			switch(eState){
				case 1: // before item
					if( Character.isWhitespace(c) ){
						pos++; break; }
					if( c == '#' ){
						eState = 2; break; }
					if( c == '*' ){
						if( pos + 1 < lenBuffer )
							zHasCustom = sbGazetteer.substring(pos + 1, pos + 7).equalsIgnoreCase("CUSTOM");
							// because this flag gets overwritten with every topic zHasCustom will
						    // only be true if the last topic is custom
						eState = 3; pos++; break;
					}
					eState = 4;
					break;
				case 2: // in comment
					if( c == '\n' || c == '\r' ) eState = 1;
					pos++;
					break;
				case 3: // in topic
					if( c == '\n' || c == '\r' ){
						ctTopic++;
						eState = 1;
					}
					pos++;
					break;
				case 4: // in entry
					if( c == '\n' || c == '\r' ) eState = 1;
					pos++;
					break;
			}
			if( pos >= lenBuffer ) break;
		}

		// size arrays
		if( zHasCustom ) ctTopic--; // the custom topic is not included because it has its own array
		asTopics = new String[ctTopic];
		asEntries = new String[ctTopic][];
		asCoordinates = new String[ctTopic][];
		ArrayList listEntries = new ArrayList();
		ArrayList listCoordinates = new ArrayList();
	    int xTopic = 0;
		int xEntry = 0;
		pos = 0;
		eState = 1; // before entry
		eLastState = 0;
		posLast = 0;
		String sTopic = null;
		while(true){
			if( eState == eLastState && pos == posLast ){
				sbError.append("circular scan error at " + pos + " : " + eState);
				return false;
			} else {
				eLastState = eState;
				posLast = pos;
			}
			char c = sbGazetteer.charAt(pos);
			switch(eState){
				case 1: // before item
					if( Character.isWhitespace(c) ){
						pos++; break; }
					if( c == '#' ){
						eState = 2; break; }
					if( c == '*' ){
						eState = 3; pos++; break; }
					eState = 4;
					break;
				case 2: // in comment
					if( c == '\n' || c == '\r' ) eState = 1;
					pos++;
					break;
				case 3: // in new topic
					if( c == '\n' || c == '\r' || c == '#' ){
						if( xTopic > 0 ){ // store entries for previous topic
							asEntries[xTopic-1] = new String[listEntries.size()];
							asCoordinates[xTopic-1] = new String[listEntries.size()];
							for( int xTopicEntry = 0; xTopicEntry < listEntries.size(); xTopicEntry++ ){
								asEntries[xTopic-1][xTopicEntry] = (String)listEntries.get(xTopicEntry);
								asCoordinates[xTopic-1][xTopicEntry] = (String)listCoordinates.get(xTopicEntry);
							}
							listEntries.clear();
							listCoordinates.clear();
							xEntry = 0;
						}
						sTopic = sb.toString();
						sb.setLength(0);
						if( sTopic.length() == 0 ) sTopic = "*";
						if( xTopic < ctTopic ){
							asTopics[xTopic] = sTopic;
							xTopic++;
						}
						if( c == '#' ) eState = 2; else eState = 1;
					} else sb.append(c);
					pos++;
					break;
				case 4: // in entry label
					if( c == '\n' || c == '\r' ){
					    eState = 1;
						sb.setLength(0);
					    break; } // ignore entry
					if( c == '#' ){
					    eState = 1;
						sb.setLength(0);
						break; } // ignore entry
					if( Character.isDigit(c) ){
						String sEntryLabel = sb.toString().trim();
						sb.setLength(0);
						if( sEntryLabel.length() == 0 ) sEntryLabel = "?";
						xEntry++;
						listEntries.add(sEntryLabel);
						eState = 5;
					} else {
						sb.append(c);
						pos++;
					}
					break;
				case 5: // in entry coordinates
					if( c == '\n' || c == '\r' || c == '#' ){
						String sEntryCoordinates = sb.toString().trim();
						sb.setLength(0);
						if( sEntryCoordinates.length() == 0 ) sEntryCoordinates = "0N 0N 0S 0S";
						listCoordinates.add(sEntryCoordinates);
						if( c == '#' ) eState = 2; else eState = 1;
					} else {
						sb.append(c);
						pos++;
					}
					break;
			}
			if( pos >= lenBuffer ) break;
		}
		if( xTopic > 0 ){ // store entries for last topic
			if( sTopic.equalsIgnoreCase("custom") ){
				asCustom = new String[listEntries.size()];
				asCustom_Coordinates = new String[listEntries.size()];
				for( int xTopicEntry = 0; xTopicEntry < listEntries.size(); xTopicEntry++ ){
					asCustom[xTopicEntry] = (String)listEntries.get(xTopicEntry);
					asCustom_Coordinates[xTopicEntry] = (String)listCoordinates.get(xTopicEntry);
				}
			} else {
				asEntries[xTopic-1] = new String[listEntries.size()];
				asCoordinates[xTopic-1] = new String[listEntries.size()];
				for( int xTopicEntry = 0; xTopicEntry < listEntries.size(); xTopicEntry++ ){
					asEntries[xTopic-1][xTopicEntry] = (String)listEntries.get(xTopicEntry);
					asCoordinates[xTopic-1][xTopicEntry] = (String)listCoordinates.get(xTopicEntry);
				}
			}
		}
		return true;
	}
}

/* may be used in the future for more sophisticated constraints
class Constraint_Spatial {
}

class Constraint_Temporal {
}
*/



