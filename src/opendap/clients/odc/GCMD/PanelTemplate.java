package opendap.clients.odc.GCMD;

import opendap.clients.odc.*;
import java.lang.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.swing.table.*;

/**
 * This class searches the GCMD database for datasets.
 */
public class PanelTemplate extends SearchInterface implements ActionListener, ListSelectionListener {

	private final static String ENCODING = "UTF-8";

	private String msGCMD_URL;

    private DefaultHandler handler;
    private Hashtable difs;
    private Hashtable hashDifPanels;

    private final JTable jtableURLs = new JTable();
	private final GCMDTableModel mModel = new GCMDTableModel();

    private JScrollPane jscrollURLs;
    private JScrollPane jscrollTop;

	private Button_Select jbuttonSelect;
    private JButton jbuttonSearch;
	private JButton jbuttonToggleSpatial;

	private ActionListener mactionSearch;

    private JPanel panelSearchControls; // in North
	private JSplitPane paneCenter;
    private JPanel panelDatasetList;

    private JPanel panelInfo;
	private JPanel panelInfoText;
	private JScrollPane scrollInfo;
	private JTextArea jtaInfo;

	private GCMDSearch myParent;

	private Panel_SpatialTemporal panelSpatial;

    public PanelTemplate(String sBaseURL, GCMDSearch parent) {
		myParent = parent;
		msGCMD_URL = sBaseURL;
		difs = new Hashtable();
		hashDifPanels = new Hashtable();
		handler = new DifHandler();
		panelSpatial = new Panel_SpatialTemporal();
		initGUI();
    }

    public boolean zInitialize(StringBuffer sbError){ return true; }

    private void initGUI() {
		panelDatasetList = new JPanel();

		// must be set up before the buttons are set up
		jtableURLs.setModel(mModel);
		jtableURLs.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		jtableURLs.setPreferredScrollableViewportSize(new Dimension(600, 300));
		jtableURLs.getSelectionModel().addListSelectionListener(this);

		// Create and intialize the command panel
		jbuttonSelect = new Button_Select(this);

		jbuttonSearch = new JButton("Search");
		Styles.vApply(Styles.STYLE_NetworkAction, jbuttonSearch);
		mactionSearch = new ActionListener(){
		    public void actionPerformed(ActionEvent e) {
				PanelTemplate.this.vDoSearch();
			}
		};
		jbuttonSearch.addActionListener(mactionSearch);

		jbuttonToggleSpatial = new JButton("Spatial/Temporal Constraint");
		jbuttonToggleSpatial.addActionListener(this);
		jbuttonToggleSpatial.setActionCommand("ToggleSpatial");

		final JButton jbuttonClear = new JButton("Clear Results");
		jbuttonClear.addActionListener(this);
		jbuttonClear.setActionCommand("Clear");

		JPanel jpanelCommand = new JPanel();
		jpanelCommand.setLayout(new FlowLayout(FlowLayout.LEFT));
		jpanelCommand.add( jbuttonSelect );
		jpanelCommand.add( jbuttonSearch );
		jpanelCommand.add( jbuttonToggleSpatial );
		jpanelCommand.add( jbuttonClear );

		// Setup the search panel
		panelSearchControls = new JPanel();
		panelSearchControls.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		panelSearchControls.setLayout(new BoxLayout(panelSearchControls, BoxLayout.Y_AXIS));
		panelSearchControls.add(jpanelCommand);

		panelDatasetList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Matching Datasets"));
		panelDatasetList.setLayout(new BoxLayout(panelDatasetList, BoxLayout.Y_AXIS));

		jscrollURLs = new JScrollPane(jtableURLs);
		jscrollURLs.setBackground(java.awt.Color.white);
		panelDatasetList.add(jscrollURLs);

		StringBuffer sbError = new StringBuffer(80); // clear any garbage in panel
		if( !panelSpatial.zInitialize(sbError) ){
			ApplicationController.vShowError("Failed to initialize spatial/temporal panel: " + sbError);
		}

		vSetupInfoPanel();

		// Add the components into the main panel
		setLayout(new BorderLayout());

		this.add(panelSearchControls, BorderLayout.NORTH);

		paneCenter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		paneCenter.setTopComponent(panelDatasetList);
		paneCenter.setBottomComponent(panelInfo);
		double dblProportionalLocation = 0.5;
		paneCenter.setDividerLocation(dblProportionalLocation);

		vSetMode(MODE_DatasetList);
    }

	final JLabel mjlabelInfoTitle = new JLabel();
	final JLabel mjlabelInfoURL = new JLabel();
	private void setInfoTitle( String sTitle ){ if( mjlabelInfoTitle != null ) mjlabelInfoTitle.setText(sTitle); };
	private void setInfoURL( String sURL ){ if( mjlabelInfoURL != null ) mjlabelInfoURL.setText(sURL); };

	private void vSetupInfoPanel(){

		panelInfo = new JPanel();
		panelInfo.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Dataset Information"));
		Dimension dimScreen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		int iPreferredHeight = (int)(dimScreen.getHeight() * .10); // info panel should be about a sixth of screen height
		panelInfo.setPreferredSize(new Dimension(600,iPreferredHeight));
		BoxLayout boxlayoutInfo = new BoxLayout(panelInfo, BoxLayout.Y_AXIS);
		panelInfo.setLayout(boxlayoutInfo);
		mjlabelInfoTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		panelInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
		panelInfo.add( this.mjlabelInfoTitle );
		panelInfo.add( this.mjlabelInfoURL );
		panelInfo.add( Box.createVerticalStrut(3) );

		// setup buttons
		final JPanel panelInfoButtons = new JPanel();
		panelInfoButtons.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		panelInfoButtons.setLayout(new BoxLayout(panelInfoButtons, BoxLayout.X_AXIS));
		panelInfoButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
		panelInfoButtons.add(Box.createHorizontalStrut(3));
		final JButton jbuttonSummary = new JButton("Get Summary");
		jbuttonSummary.addActionListener(this);
		jbuttonSummary.setActionCommand("getSummary");
		panelInfoButtons.add(jbuttonSummary);
		panelInfoButtons.add(Box.createHorizontalStrut(3));
		final JButton jbuttonGeneralInfo = new JButton("Get General Info");
		jbuttonGeneralInfo.addActionListener(this);
		jbuttonGeneralInfo.setActionCommand("getGeneralInfo");
		panelInfoButtons.add(jbuttonGeneralInfo);
		panelInfoButtons.add(Box.createHorizontalStrut(3));
		final JLabel jlabelShowConstraint = new JLabel("        ");
		panelInfoButtons.add(jlabelShowConstraint);
		jlabelShowConstraint.addMouseListener(
			new MouseAdapter(){
				public void mousePressed( MouseEvent me ){
					if( me.getClickCount() == 1){
						if( (me.getModifiers() & me.CTRL_MASK) == 0 ){
							PanelTemplate.this.vInfo_Set("");
						} else {
							String sQuery = PanelTemplate.this.jtaInfo.getText();
							PanelTemplate.this.vDoSearch(sQuery);
						}
					} else {
					   PanelTemplate.this.vShowConstraint();
					}
				}
			}
		);

		// setup info text
		jtaInfo = new JTextArea();
		scrollInfo = new JScrollPane(jtaInfo);
		final JPanel panelInfoText = new JPanel();
		panelInfoText.setLayout(new BorderLayout());
		panelInfoText.add(panelInfoButtons, BorderLayout.NORTH);
		panelInfoText.add(scrollInfo, BorderLayout.CENTER);

		panelInfo.add(panelInfoText);

	}

	void vInfo_Set( String sInfo ){
		// not in use panelInfoText.setVisible(true);
		if( sInfo == null ) sInfo = "";
		jtaInfo.setText(sInfo);
		if( sInfo.length() == 0 ){
		} else {
			jtaInfo.setCaretPosition( 0 );
		}
	}

	void vInfo_Clear(){
		jtaInfo.setText("");
	}

	protected JPanel getSearchControlsPanel(){ return panelSearchControls; }

    protected Dif getDif(String difName, String sections) {
		return (Dif)((DifHandler)handler).getDifs().elementAt(0);
    }

    /**
     * The function to handle action events.
     * @param e The event.
     */
    public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		if(command.equals("ToggleSpatial")) {
			this.vSetMode(this.MODE_Spatial);
		}

		if(command.equals("Clear")) {
			this.vClear();
		}

		else if(command.equals("getSummary")) {
			vShowSummary();
		}

		else if(command.equals("getGeneralInfo")) {
			vShowGeneralInfo();
		}
    }

	void vClear(){
		mModel.setData(null);
		// not in use panelInfoText.setVisible(false);
	}

	void vDoSearch(){
		String sQueryString_Complete = getConstraint_Complete();
		vDoSearch( sQueryString_Complete );
	}

	void vDoSearch(String sQueryString_Complete){
		try {
			Activity activitySearch = new Activity();
			SearchContinuation conSearch = new SearchContinuation(sQueryString_Complete);
			activitySearch.vDoActivity(
				jbuttonSearch,
				mactionSearch,
			    conSearch,
				"Making GCMD search " + sQueryString_Complete
			);
			this.vSetMode(this.MODE_DatasetList);
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("GCMD free text search error");
			Utility.vUnexpectedError(ex, sbError);
		}
	}

	String getConstraint_Complete(){
		String sQueryString_Complete;
		String sDODSqualifier = "[Project:Short_Name='DODS']";
		String sQueryString_Search = getConstraint_Interface();
		String sQueryString_Spatial = getConstraint_Spatial();
		String sQueryString_Temporal = getConstraint_Temporal();
		sQueryString_Complete = sDODSqualifier;
		if( sQueryString_Search.length() > 0) sQueryString_Complete += " AND " + sQueryString_Search;
		if( sQueryString_Spatial.length() > 0) sQueryString_Complete +=	" AND " + sQueryString_Spatial;
		if( sQueryString_Temporal.length() > 0) sQueryString_Complete += " AND " + sQueryString_Temporal;
		return sQueryString_Complete;
	}

	String getConstraint_Interface(){
		return ""; // the child class should override this method
	}

	String getConstraint_Spatial(){
		try {
			String sConstraint = "";
			if( panelSpatial != null ){
				if (panelSpatial.zHasSpatialConstraint()) {

					//southernmost
					double dblConstraintValue = panelSpatial.getSouthernmost();
					int iConstraintValue = (int)dblConstraintValue; // currently the server only supports ints
					sConstraint += "[Spatial_Coverage:Southernmost_Latitude='" + iConstraintValue + "',";

					//northernmost
					dblConstraintValue = panelSpatial.getNorthernmost();
					iConstraintValue = (int)dblConstraintValue; // currently the server only supports ints
					sConstraint += "Northernmost_Latitude='" + iConstraintValue + "',";

					//westernmost
					dblConstraintValue = panelSpatial.getWesternmost();
					iConstraintValue = (int)dblConstraintValue; // currently the server only supports ints
					sConstraint += "Westernmost_Longitude='" + iConstraintValue + "',";

					//easternmost
					dblConstraintValue = panelSpatial.getEasternmost();
					iConstraintValue = (int)dblConstraintValue; // currently the server only supports ints
					sConstraint += "Easternmost_Longitude='" + iConstraintValue + "']";
				}
			}
			return sConstraint;
		} catch(Exception ex) {
			StringBuffer sb = new StringBuffer("While formating spatial constraint: ");
			Utility.vUnexpectedError(ex, sb);
			ApplicationController.vShowError(sb.toString());
			return "";
		}
	}

	// looks like [Temporal_Coverage:Start_Date>='1990-01-01',Stop_Date<='1990-12-31']
	String getConstraint_Temporal(){
		try {
			String sConstraint = "";
			if( panelSpatial != null ){
				if (panelSpatial.zHasTemporalConstraint()) {

					String sFromQualifier = panelSpatial.getTemporal_FromQualifier();
					String sFromYear = panelSpatial.getTemporal_FromYear();
					String sFromMonth = panelSpatial.getTemporal_FromMonth();
					String sFromDay = panelSpatial.getTemporal_FromDay();
					String sToQualifier = panelSpatial.getTemporal_ToQualifier();
					String sToYear = panelSpatial.getTemporal_ToYear();
					String sToMonth = panelSpatial.getTemporal_ToMonth();
					String sToDay = panelSpatial.getTemporal_ToDay();

					if( sFromQualifier == null ) sFromQualifier = ">=";
					if( sFromYear == null ) sFromYear = "";
					if( sFromMonth == null ) sFromMonth = "";
					if( sFromDay == null ) sFromDay = "";
					if( sToQualifier == null ) sToQualifier = "<=";
					if( sToYear == null ) sToYear = "";
					if( sToMonth == null ) sToMonth = "";
					if( sToDay == null ) sToDay = "";

					if( (sFromYear + sFromMonth + sFromDay + sToYear + sToMonth + sToDay).trim().length() == 0 ) return "";

					String sFrom, sTo;
					if( (sFromYear + sFromMonth + sFromDay).trim().length() == 0 ){
						sFrom = "";
					} else {
						sFrom = ((sFromYear.length()==0) ? "0001" : sFromYear) + ((sFromMonth.length()==0) ? "-01" : "-" + sFromMonth) + ((sFromDay.length()==0) ? "-01" : "-" + sFromDay);
					}
					if( (sToYear + sToMonth + sToDay).trim().length() == 0 ){
						sTo = "";
					} else {
						Date dateNow = new Date();
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTime(dateNow);
						int iCurrentYear = gc.get(GregorianCalendar.YEAR);
						String sCurrentYear = Utility.sFormatFixedRight(iCurrentYear, 4, '0');
						sTo = ((sToYear.length()==0) ? sCurrentYear : sToYear) + ((sToMonth.length()==0) ? "-12" : "-" + sToMonth) + ((sToDay.length()==0) ? "-31" : "-" + sToDay);
					}


					if( sFrom.length() == 0 ){
						return "[Temporal_Coverage:Stop_Date" + sToQualifier + "'" + sTo + "']";
					} else if( sTo.length() == 0 ){
						return "[Temporal_Coverage:Start_Date" + sFromQualifier + "'" + sFrom + "']";
					} else {
						return "[Temporal_Coverage:Start_Date" + sFromQualifier + "'" + sFrom + "',Stop_Date" + sToQualifier + "'" + sTo + "']";
					}

				}
			}
			return sConstraint;
		} catch(Exception ex) {
			StringBuffer sb = new StringBuffer("While formating spatial constraint: ");
			Utility.vUnexpectedError(ex, sb);
			ApplicationController.vShowError(sb.toString());
			return "";
		}
	}

	void vShowConstraint(){
		vInfo_Set(this.getConstraint_Complete());
	}

	void vShowSummary(){
		if( jtableURLs.getRowCount() < 1 ){
			vInfo_Set("[no search results]");
			return;
		}
		int index = jtableURLs.getSelectionModel().getMinSelectionIndex();
		if( index < 0 ){
			vInfo_Set("[no item selected]");
			return;
		}
		Object oData = jtableURLs.getValueAt(index,0);
		if( oData == null ) return;
		Dif dif = (Dif)oData;
		if(dif.getSummary().equals("")) {
			//Dif temp = getDif(dif.getID(), "Summary");
			//dif.setSummary(temp.getSummary());
			//if(dif.getSummary().equals(""))
			dif.setSummary("No Summary Available");
		}
		vInfo_Set(dif.getSummary());
	}

	void vShowGeneralInfo(){
		try {
			if( jtableURLs.getRowCount() < 1 ){
				vInfo_Set("[no search results]");
				return;
			}
			int index = jtableURLs.getSelectionModel().getMinSelectionIndex();
			if( index < 0 ){
				vInfo_Set("[no item selected]");
				return;
			}
			Object oData = jtableURLs.getValueAt(index,0);
			if( oData == null ) return;
			Dif dif = (Dif)oData;
			if( dif == null ){
				vInfo_Set( "no info available" );
			} else {
				vInfo_Set(dif.getGeneralInfo());
			}
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("General info error");
			Utility.vUnexpectedError(ex, sbError);
		}
	}

	private final static int MODE_DatasetList = 2;
	private final static int MODE_Spatial = 3;
	int miMode = 2;
	private void vSetMode( int iMode ){
		this.remove(paneCenter);
		this.remove(panelSpatial);
		switch (iMode){
			case MODE_DatasetList:
				this.add(paneCenter, BorderLayout.CENTER);
				jbuttonToggleSpatial.setText("Spatial/Temporal Constraint");
				break;
			case MODE_Spatial:
				if( miMode == MODE_Spatial ){
					this.add(paneCenter, BorderLayout.CENTER);
					iMode = MODE_DatasetList;
					jbuttonToggleSpatial.setText("Spatial/Temporal Constraint");
				} else {
					this.add(panelSpatial, BorderLayout.CENTER);
					jbuttonToggleSpatial.setText("Results");
				}
				break;
		}
		miMode = iMode;
		if( getRootPane() != null  )
			if( getRootPane().getContentPane() != null )
				getRootPane().getContentPane().validate();
		paneCenter.invalidate();
		paneCenter.repaint();
		panelSpatial.invalidate();
		panelSpatial.repaint();
	}

	public void addListSelectionListener(ListSelectionListener listener){
		jtableURLs.getSelectionModel().addListSelectionListener(listener);
	}

    /**
     * The function to handle list selection events.
     * @param e The event.
     */
    public void valueChanged(ListSelectionEvent e) {
		try {
			if(!e.getValueIsAdjusting()) {
				int index = ((ListSelectionModel)e.getSource()).getMinSelectionIndex();
				if(index != -1) {
					Dif dif = (Dif)jtableURLs.getModel().getValueAt(index,0);
					String sBaseURL;
					StringBuffer sbError = new StringBuffer();
					DodsURL url = dif.getDodsURL(sbError);
					if( url == null ){
						sBaseURL = "Error: " + sbError;
					} else {
						sBaseURL = url.getBaseURL();
					}
					this.setInfoTitle(" Title: " + dif.getTitle());
					this.setInfoURL(" URL: " + sBaseURL);
				}
				vInfo_Set("");
			}
		} catch(Exception ex) {
			// just forget it
		}
    }

    /**
     * Returns all the selected urls.
     * @return all the selected urls
     */
    public DodsURL[] getURLs( StringBuffer sbError ) {
		int[] aiSelectedRows = jtableURLs.getSelectedRows();
		if( aiSelectedRows == null ) return null;
		int ctURLs = aiSelectedRows.length;
		DodsURL[] urls = new DodsURL[ctURLs];
		for( int xSelectedRow = 0; xSelectedRow < ctURLs; xSelectedRow++ ){
			Dif dif = (Dif)jtableURLs.getValueAt(aiSelectedRows[xSelectedRow],0);
			urls[xSelectedRow] = dif.getDodsURL(sbError);
			if( urls[xSelectedRow] == null ) return null;
		}
		int ctValidURLs = 0;
		for( int xURL = 0; xURL < ctURLs; xURL++ ){
			if( urls[xURL] != null ) ctValidURLs++;
		}
		if( ctValidURLs < ctURLs ){ // there were errors
			ApplicationController.getInstance().vShowWarning("One or more selected URLs was invalid, check GCMD info for errors");
			if( ctValidURLs == 0 ) return null;
			DodsURL[] urlsValid = new DodsURL[ctValidURLs];
			int xValidURL = -1;
			for( int xURL = 0; xURL < ctURLs; xURL++ ){
				if( urls[xURL] != null ){
					xValidURL++;
					urlsValid[xValidURL] = urls[xURL];
				}
			}
			urls = urlsValid;
		}
		return urls;
    }

    /**
     * This class makes a request to the GCMD servlets and
     * displays the results when it's done.
     */
    class SearchContinuation implements Continuation_DoCancel {
		InputStream mis = null;
		String msQueryString;
		boolean mzDrawTable;
		SearchContinuation(String sQuery){
			msQueryString = sQuery;
			mzDrawTable = true;
		}
		public void Do(){
			handler = new DifHandler();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			try {
				URL urlQuery;
				String sQueryURL;
			    sQueryURL = msGCMD_URL + "/getdifs.py?query=";
//			    if( msQueryString.length() > 0 ) sQueryURL += URLEncoder.encode(msQueryString, ENCODING); 1.4.1 only
			    if( msQueryString.length() > 0 ) sQueryURL += URLEncoder.encode(msQueryString);
				urlQuery = new URL(sQueryURL);
				ApplicationController.getInstance().vShowStatus("Opening GCMD query: " + sQueryURL);
				try {
					mis = urlQuery.openStream();
				} catch(Exception ex) {
					ApplicationController.getInstance().vShowError("GCMD connection failure: " + ex);
					return;
				}
				SAXParser saxParser = factory.newSAXParser();
				saxParser.parse( mis, handler );

				// If the request has been canceled, drawTable will be false at this point.
				if(mzDrawTable) {
					Vector idVector = ((DifHandler)handler).getDifs();
					if( idVector.size() == 0 ){
						ApplicationController.getInstance().vShowStatus("Search returned no matches");
					}

					// remove any results lacking a DODS URL
					StringBuffer sbError = new StringBuffer(80);
					for( int xDIF = idVector.size()-1; xDIF >= 0; xDIF-- ){
						Dif difCurrent = (Dif)idVector.get(xDIF);
						if( difCurrent.getDodsURL(sbError) == null ){
							idVector.remove(xDIF);
						}
					}

					Object[] selected = new Boolean[idVector.size()];
					Object[] ids = new Object[idVector.size()];
					idVector.copyInto(ids);
					for(int i=0;i<ids.length;i++) {
						selected[i] = new Boolean(false);
					}

					mModel.setData(ids);
				}
			} catch(Throwable ex) {
				StringBuffer sbError = new StringBuffer("while executing GCMD search thread");
				Utility.vUnexpectedError(ex, sbError);
				ApplicationController.getInstance().vShowError(sbError.toString());
			}
		}
		public void Cancel(){
			mzDrawTable = false;
			if( mis != null ){
			    try { mis.close(); } catch(Exception ex) {}
		    }
		}
	}
}


