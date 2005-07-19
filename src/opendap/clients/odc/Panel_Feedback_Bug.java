package opendap.clients.odc;

/**
 * Title:        Panel_Feedback_Bug
 * Description:  Output text area which displays messages and data
 * Copyright:    Copyright (c) 2004
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.57
 */

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.util.ArrayList;

public class Panel_Feedback_Bug extends JPanel {

    public Panel_Feedback_Bug() {}

	public final static String[] CHOICE_OS = {
		"Windows XP/Milennium",
		"Windows 95/98",
		"Windows NT",
		"Windows 2000",
		"Windows other",
		"MacOS X 2.6+",
		"Mac other",
		"Linux x86",
		"Solaris",
		"Unix other" };

	// not used
	public final static String[] CHOICE_NatureOfBug = {
		"Program crashes/terminates abruptly",
		"Program hangs",
		"Feature does not work",
		"Feature behaviour/design problem",
		"Feature hard to understand/misleading",
		"Functionality ok, but getting error/warning",
		"Cosmetic blemish",
		"Incorrect/Missing help documentation",
		"New feature request" };

	public final static String[] CHOICE_KindOfProblem = {
		"Program Malfunction",
		"Dataset Missing/Incorrect",
		"Installation/Configuration/Help",
		"Startup/Shutdown",
		"Other" };


	public final static String[] CHOICE_AreaOfProblem = {
		"Search / Dataset List",
		"Search / GCMD / Free Text",
		"Search / GCMD / Key Word",
		"Search / GCMD / Spatial Panel",
		"Search / Favorites",
		"Search / Recent",
		"Retrieve : Dataset Selection",
		"Retrieve : Directories",
		"Retrieve : AdditionCriteria/DDS View",
		"Retrieve : Output",
		"Retrieve (other)",
		"View / Text",
		"View / Table",
		"View / Image File",
		"View / Plotter : dataset list",
		"View / Plotter : Variables",
		"View / Plotter : Options",
		"View / Plotter : Text",
		"View / Plotter : Scale",
		"View / Plotter : Colors",
		"View / Plotter : Preview",
		"View / Plotter : Thumbnails",
		"View / Plotter : (output target - window/file/etc)",
		"View / Plotter : (pseudocolor output)",
		"View / Plotter : (xy output)",
		"View / Plotter : (vector output)",
		"View / Plotter : (histogram output)",
		"View / Plotter : (lables/borders/titles/legend output)",
		"View / Plotter (other)",
		"other area (specify)" };

	public final static String[] CHOICE_AreaOfProblem_Search = {
		"Dataset List",
		"GCMD / Free Text",
		"GCMD / Key Word",
		"GCMD / Spatial Panel",
		"Favorites",
		"Recent"};

	public final static String[] CHOICE_AreaOfProblem_Retrieve = {
		"Retrieve : Dataset Selection",
		"Retrieve : Directories",
		"Retrieve : AdditionCriteria/DDS View",
		"Retrieve : Output (interface)",
		"Retrieve : Output (results)",
		"Retrieve (other)" };

	public final static String[] CHOICE_AreaOfProblem_View = {
		"Text",
		"Table",
		"Image File",
		"Plotter : dataset list",
		"Plotter : Variables",
		"Plotter : Options",
		"Plotter : Text",
		"Plotter : Scale",
		"Plotter : Colors",
		"Plotter : Preview",
		"Plotter : Thumbnails",
		"Plotter : (output target - window/file/etc)",
		"Plotter : (pseudocolor output)",
		"Plotter : (xy output)",
		"Plotter : (vector output)",
		"Plotter : (histogram output)",
		"Plotter : (lables/borders/titles/legend output)",
		"Plotter : (coastline)",
		"Plotter (other)" };

	public final static String[] CHOICE_AreaOfProblem_Other = {
		"program crashing/hanging",
		"program graphics incorrect/flaky",
		"program conflicts with another program",
		"splash screen",
		"program window frame",
		"interprocess server",
		"help",
		"documentation",
		"copy and paste",
		"miscellaneous" };

//http://dodsdev.gso.uri.edu/bugzilla/enter_bug.cgi?product=DODS%20clients
//http://dodsdev.gso.uri.edu/bugzilla/enter_bug.cgi?Bugzilla_login=j.chamberlain%40opendap.org&Bugzilla_password=empty&product=DODS+clients&GoAheadAndLogIn=Login

	private final JComboBox jcbOperatingSystem = new JComboBox(CHOICE_OS);
	private final JComboBox jcbNatureOfBug = new JComboBox(CHOICE_NatureOfBug);
	private final JComboBox jcbKindOfProblem = new JComboBox(CHOICE_KindOfProblem);
	private final JTabbedPane jtabAreaOfProblem = new javax.swing.JTabbedPane();
	private final JComboBox jcbAreaOfProblem_Search = new JComboBox(CHOICE_AreaOfProblem_Search);
	private final JComboBox jcbAreaOfProblem_Retrieve = new JComboBox(CHOICE_AreaOfProblem_Retrieve);
	private final JComboBox jcbAreaOfProblem_View = new JComboBox(CHOICE_AreaOfProblem_View);
	private final JComboBox jcbAreaOfProblem_Other = new JComboBox(CHOICE_AreaOfProblem_Other);
	private final JLabel labelOperatingSystem = new JLabel("Operating System: ");
	private final JLabel labelNatureOfBug = new JLabel("Nature of Bug: ");
	private final JLabel labelKindOfProblem = new JLabel("Kind of Problem: ");
	private final JLabel labelAreaOfProblem = new JLabel("Area of Problem: ");

	private final JScrollPane jspFullDescription = new JScrollPane();
	private final JScrollPane jspSystemLog = new JScrollPane();
	private final JTextArea jtaFullDescription = new JTextArea("");
	private final JTextArea jtaSystemLog = new JTextArea("");
	private final JTextField jtfShortDescription = new JTextField("");
	private final JTextField jtfUserEmail = new JTextField();

    boolean zInitialize(StringBuffer sbError){

        try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			setBorder(borderStandard);

			setLayout(new java.awt.BorderLayout());

			// setup alignment
			jcbOperatingSystem.setAlignmentX(Component.LEFT_ALIGNMENT);
			jcbNatureOfBug.setAlignmentX(Component.LEFT_ALIGNMENT);
			jcbKindOfProblem.setAlignmentX(Component.LEFT_ALIGNMENT);
			jtabAreaOfProblem.setAlignmentX(Component.LEFT_ALIGNMENT);
			jcbAreaOfProblem_Search.setAlignmentX(Component.LEFT_ALIGNMENT);
			jcbAreaOfProblem_Retrieve.setAlignmentX(Component.LEFT_ALIGNMENT);
			jcbAreaOfProblem_View.setAlignmentX(Component.LEFT_ALIGNMENT);
			jcbAreaOfProblem_Other.setAlignmentX(Component.LEFT_ALIGNMENT);
			labelOperatingSystem.setAlignmentX(Component.LEFT_ALIGNMENT);
			labelNatureOfBug.setAlignmentX(Component.LEFT_ALIGNMENT);
			labelKindOfProblem.setAlignmentX(Component.LEFT_ALIGNMENT);
			labelAreaOfProblem.setAlignmentX(Component.LEFT_ALIGNMENT);
			jspFullDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
			jspSystemLog.setAlignmentX(Component.LEFT_ALIGNMENT);

			// Create operating parameters choices
			JPanel panelParameters = new JPanel();
			panelParameters.setLayout(new GridBagLayout());
			panelParameters.setAlignmentX(Component.LEFT_ALIGNMENT);
			GridBagConstraints gbc = new GridBagConstraints();

			// Create area of problem tabbed pane
			jtabAreaOfProblem.add(" Search", jcbAreaOfProblem_Search);
			jtabAreaOfProblem.add(" Retrieve", jcbAreaOfProblem_Retrieve);
			jtabAreaOfProblem.add(" View", jcbAreaOfProblem_View);
			jtabAreaOfProblem.add(" Other", jcbAreaOfProblem_Other);

			// top margin
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 0.0;
			gbc.gridwidth = 5;
			gbc.gridheight = 1;
			panelParameters.add(Box.createVerticalStrut(15), gbc);

		    // Operating system
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.weightx = 1;
			gbc.weighty = 0.0;
			gbc.gridwidth = 1;
			panelParameters.add(Box.createHorizontalGlue(), gbc);
			gbc.gridx = 1;
			gbc.weightx = 0.0;
			panelParameters.add(labelOperatingSystem, gbc);
			gbc.gridx = 2;
			gbc.weightx = 0.0;
			panelParameters.add(Box.createHorizontalStrut(5), gbc);
			gbc.gridx = 3;
			gbc.weightx = 0.0;
			panelParameters.add(jcbOperatingSystem, gbc);
			gbc.gridx = 4;
			gbc.weightx = 1;
			panelParameters.add(Box.createHorizontalGlue(), gbc);

			// Nature of Bug (not in use)
//			gbc.gridy = 2;
//			gbc.gridx = 0;
//			gbc.weightx = 1;
//			gbc.weighty = 0.0;
//			gbc.gridwidth = 1;
//			panelParameters.add(Box.createHorizontalGlue(), gbc);
//			gbc.gridx = 1;
//			gbc.weightx = 0.0;
//			panelParameters.add(labelNatureOfBug, gbc);
//			gbc.gridx = 2;
//			gbc.weightx = 0.0;
//			panelParameters.add(Box.createHorizontalStrut(5), gbc);
//			gbc.gridx = 3;
//			gbc.weightx = 0.0;
//			panelParameters.add(jcbNatureOfBug, gbc);
//			gbc.gridx = 4;
//			gbc.weightx = 1;
//			panelParameters.add(Box.createHorizontalGlue(), gbc);

			// Kind of Problem
			gbc.gridy = 3;
			gbc.gridx = 0;
			gbc.weightx = 1;
			gbc.weighty = 0.0;
			gbc.gridwidth = 1;
			panelParameters.add(Box.createHorizontalGlue(), gbc);
			gbc.gridx = 1;
			gbc.weightx = 0.0;
			panelParameters.add(labelKindOfProblem, gbc);
			gbc.gridx = 2;
			gbc.weightx = 0.0;
			panelParameters.add(Box.createHorizontalStrut(5), gbc);
			gbc.gridx = 3;
			gbc.weightx = 0.0;
			panelParameters.add(jcbKindOfProblem, gbc);
			gbc.gridx = 4;
			gbc.weightx = 1;
			panelParameters.add(Box.createHorizontalGlue(), gbc);

			// Area of Problem
			gbc.gridy = 4;
			gbc.gridx = 0;
			gbc.weightx = 1;
			gbc.weighty = 0.0;
			gbc.gridwidth = 1;
			panelParameters.add(Box.createHorizontalGlue(), gbc);
			gbc.gridx = 1;
			gbc.weightx = 0.0;
			panelParameters.add(labelAreaOfProblem, gbc);
			gbc.gridx = 2;
			gbc.weightx = 0.0;
			panelParameters.add(Box.createHorizontalStrut(5), gbc);
			gbc.gridx = 3;
			gbc.weightx = 0.0;
			panelParameters.add(jtabAreaOfProblem, gbc);
			gbc.gridx = 4;
			gbc.weightx = 1;
			panelParameters.add(Box.createHorizontalGlue(), gbc);

			add(panelParameters, BorderLayout.NORTH);

			// Create and initialize the full description area
			Styles.vApply(Styles.STYLE_Terminal, jtaFullDescription);
			jtaFullDescription.setLineWrap(true);
			jtaFullDescription.setWrapStyleWord(true);
			jspFullDescription.setViewportView(jtaFullDescription);
			jspFullDescription.setMinimumSize(new Dimension(400, 200));

			// Create and initialize the steps to system log area
			Styles.vApply(Styles.STYLE_Terminal, jtaSystemLog);
			jtaSystemLog.setLineWrap(true);
			jtaSystemLog.setWrapStyleWord(true);
			jspSystemLog.setViewportView(jtaSystemLog);

			// Create description/log panel
			JPanel panelText = new JPanel();
			panelText.setLayout(new BoxLayout(panelText, BoxLayout.Y_AXIS));
			JLabel labelFullDescription = new JLabel("  Description (include steps to reproduce if relevant):");
			labelFullDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
			panelText.add(labelFullDescription);
			panelText.add(jspFullDescription);
			JLabel labelSystemLog = new JLabel("  System Log:");
			labelSystemLog.setAlignmentX(Component.LEFT_ALIGNMENT);
			panelText.add(labelSystemLog);
			panelText.add(jspSystemLog);
		    add(panelText, java.awt.BorderLayout.CENTER);

			// Create and intialize the command panel
			JPanel jpanelCommand = new JPanel();
			JPanel jpanelCommandButtons = new JPanel();

			jpanelCommand.setLayout( new BorderLayout() );

			// User email address panel
			JPanel panelUserEmail = new JPanel();
			panelUserEmail.setLayout(new BoxLayout(panelUserEmail, BoxLayout.X_AXIS));
			JLabel labelEmail = new JLabel("Email Address: ");
			panelUserEmail.add(labelEmail);
			panelUserEmail.add(jtfUserEmail);
			labelEmail.setAlignmentX(Component.LEFT_ALIGNMENT);
			panelUserEmail.setAlignmentX(Component.LEFT_ALIGNMENT);
			String sUserEmail = ConfigurationManager.getInstance().getProperty_FEEDBACK_EmailUserAddress();
			if( sUserEmail != null && sUserEmail.trim().length() > 0 ) jtfUserEmail.setText(sUserEmail);

			// Area of Problem Visibility
			labelAreaOfProblem.setVisible(true);
			jtabAreaOfProblem.setVisible(true);
			jcbKindOfProblem.addActionListener(
				new ActionListener(){
	    			public void actionPerformed(ActionEvent event) {
						int xSelection = jcbKindOfProblem.getSelectedIndex();
						if( xSelection == 0 ){
							labelAreaOfProblem.setVisible(true);
							jtabAreaOfProblem.setVisible(true);
						} else {
							labelAreaOfProblem.setVisible(false);
							jtabAreaOfProblem.setVisible(false);
						}
					}
				}
			);

			// Clear Display
			JButton jbuttonClearDisplay = new JButton("Clear");
			jbuttonClearDisplay.addActionListener(
				new ActionListener(){
	    			public void actionPerformed(ActionEvent event) {
						Panel_Feedback_Bug.this.vClearDisplay();
					}
				}
			);

			// Send Bug
			JButton jbuttonSendBug = new JButton("Send Bug");
			jbuttonSendBug.addActionListener(
				new ActionListener(){
	    			public void actionPerformed(ActionEvent event) {
						Panel_Feedback_Bug.this.vSendBug();
					}
				}
			);

			jpanelCommandButtons.add( jbuttonSendBug );
			jpanelCommandButtons.add( jbuttonClearDisplay );

			jpanelCommand.add( panelUserEmail, BorderLayout.NORTH );
			jpanelCommand.add( jpanelCommandButtons, BorderLayout.SOUTH );

			add(jpanelCommand, BorderLayout.SOUTH);

            return true;

        } catch(Exception ex){
			Utility.vUnexpectedError(ex, sbError);
            return false;
        }
	}

	// this is called whenever the user clicks the "Feedback Bug" tab (see ApplicationFrame)
	void vUpdateLog(){
		jtaSystemLog.setText(ApplicationController.getInstance().getLog());
	}

	void vClearDisplay(){
		jtaFullDescription.setText("");
	}

	void vSendBug(){

		// get parameters
		String sBugzillaHost = ConfigurationManager.getInstance().getProperty_FEEDBACK_BugHost();
		String sBugzillaRoot = ConfigurationManager.getInstance().getProperty_FEEDBACK_BugzillaRoot();

		// login and get session cookies
		String sCommand = "GET";
		int iPort = 80;
		String sPath = sBugzillaRoot + "/enter_bug.cgi";
		String sQuery = "Bugzilla_login=j.chamberlain%40opendap.org&Bugzilla_password=empty&product=DODS+clients&GoAheadAndLogIn=Login";
		String sProtocol = "HTTP/1.1";
		String sContentType = null;
		String sContent = null;
		String sReferer = null;
		ArrayList listClientCookies = null;
		ArrayList listServerCookies = new ArrayList();
		ByteCounter bc = null;
		Activity activity = null;
		StringBuffer sbError = new StringBuffer(80);
		String sPageReturn = IO.getStaticContent(sCommand, sBugzillaHost, iPort, sPath, sQuery, sProtocol, sReferer, sContentType, sContent, listClientCookies, listServerCookies, bc, activity, sbError);
		if( sPageReturn == null ){
			ApplicationController.vShowError("Failed to initiate bugzilla session: " + sbError);
			return;
		}

		// Store User Email
		String sUserEmail = jtfUserEmail.getText();
		if( sUserEmail != null && sUserEmail.trim().length() > 0 ){
			String sStoredEmail = ConfigurationManager.getInstance().getProperty_FEEDBACK_EmailUserAddress();
			if( ! sUserEmail.equals(sStoredEmail) ){
				ConfigurationManager.getInstance().setOption(ConfigurationManager.PROPERTY_FEEDBACK_EmailUserAddress, sUserEmail);
			}
		}

		// Assemble Bug Description
		String sOperatingSystem = jcbOperatingSystem.getSelectedItem().toString();
		String sNatureOfBug = jcbNatureOfBug.getSelectedItem().toString(); // not used
		String sKindOfProblem = jcbKindOfProblem.getSelectedItem().toString();
		String sAreaOfProblem;
	    if( jtabAreaOfProblem.isVisible() ){
			switch( jtabAreaOfProblem.getSelectedIndex() ){
				case 0:
					sAreaOfProblem = jcbAreaOfProblem_Search.getSelectedItem().toString(); break;
				case 1:
					sAreaOfProblem = jcbAreaOfProblem_Retrieve.getSelectedItem().toString(); break;
				case 2:
					sAreaOfProblem = jcbAreaOfProblem_View.getSelectedItem().toString(); break;
				case 3:
					sAreaOfProblem = jcbAreaOfProblem_Other.getSelectedItem().toString(); break;
				default:
					sAreaOfProblem = "[undefined]";
			}
		} else {
			sAreaOfProblem = "[none]";
		}
		StringBuffer sbBugDescription = new StringBuffer(2500);
		sbBugDescription.append("OPeNDAP Data Connector: User Feedback Bug" + "\n");
		sbBugDescription.append("Version: " + ApplicationController.getInstance().getVersionString() + "\n");
		sbBugDescription.append("Operating System: " + sOperatingSystem + "\n");
		sbBugDescription.append("Kind of Problem: " + sKindOfProblem + "\n");
		if( jtabAreaOfProblem.isVisible() ) sbBugDescription.append(sAreaOfProblem).append("\n");
		if( sUserEmail.trim().length() == 0 ) sUserEmail = "[none provided]";
		sbBugDescription.append("\n**************** USER EMAIL ********************\n");
		sbBugDescription.append(sUserEmail);
		sbBugDescription.append("\n\n**************** USER BUG DESCRIPTION ********************\n");
		sbBugDescription.append(jtaFullDescription.getText());
		sbBugDescription.append("\n\n**************** SYSTEM LOG ********************\n");
		sbBugDescription.append(jtaSystemLog.getText());
		String sBugDescription_encoded;
		try {
			sBugDescription_encoded = java.net.URLEncoder.encode( sbBugDescription.toString(), "UTF-8" );
		} catch(Exception ex) {
			ApplicationController.vShowError("Failed to emcode bug/feedback to UTF-8: " + ex);
			return;
		}

		// Assemble Posting
		StringBuffer sbContent = new StringBuffer(1000);
		sbContent.append("reporter=j.chamberlain%40opendap.org").append('&');
		sbContent.append("product=DODS+clients").append('&');
		sbContent.append("version=3.1.x").append('&');
		sbContent.append("component=ODC").append('&');
		sbContent.append("rep_platform=PC").append('&');
		sbContent.append("op_sys=Windows+2000").append('&');
		sbContent.append("priority=P1").append('&');
		sbContent.append("bug_severity=normal").append('&');
	    sbContent.append("assigned_to=").append('&'); // defaults to component owner
		sbContent.append("cc=").append('&');
		sbContent.append("bug_file_loc=http%3A%2F%2Fnone").append('&');
		sbContent.append("short_desc=%5Buser+bug%5D").append('&');
		sbContent.append("comment=").append(sBugDescription_encoded).append('&');
		sbContent.append("bit-256=0").append('&');
		sbContent.append("bit-512=0").append('&');
		sbContent.append("form_name=enter_bug");

// test posting
//		sbContent.append("reporter=j.chamberlain%40opendap.org&product=DODS+clients&version=3.1.x&component=ODC&rep_platform=PC&op_sys=Windows+2000&priority=P1&bug_severity=normal&assigned_to=&cc=&bug_file_loc=http%3A%2F%2Fnone2&short_desc=test+bug+for+ODC+feedback+%232&comment=content+description+for+%232%0D%0Asecond+line+for+description+%232&bit-256=0&bit-512=0&form_name=enter_bug");

		// Assemble Cookies
		listClientCookies = new ArrayList();
		if( listServerCookies.size() > 0 ){
			for( int xCookie = 1; xCookie <= listServerCookies.size(); xCookie++ ){
				String sCookie = (String)listServerCookies.get(xCookie-1);
				int posEndValue = sCookie.indexOf(';');
				if( posEndValue < 1 ) continue;
				int posStartValue = sCookie.indexOf('=');
				if( posStartValue < 1 ) continue;
				String sClientCookie = sCookie.substring(0, posEndValue).trim();
				String sCookieValue = sCookie.substring(posStartValue + 1, posEndValue).trim();
				if( sCookieValue.length() < 1 ) continue; // ignore any cookies without value
				listClientCookies.add(sClientCookie);
			}
		}

		// make posting
		sCommand = "POST";
		sPath = sBugzillaRoot + "/post_bug.cgi";
		sQuery = null;
		sProtocol = "HTTP/1.1";
		sReferer = "http://" + sBugzillaHost + sBugzillaRoot + "/enter_bug.cgi?Bugzilla_login=j.chamberlain%40opendap.org&Bugzilla_password=empty&product=DODS+clients&GoAheadAndLogIn=Login";
		sContentType = "application/x-www-form-urlencoded";
		sContent = sbContent.toString();
		listServerCookies.clear();
		sPageReturn = IO.getStaticContent(sCommand, sBugzillaHost, iPort, sPath, sQuery, sProtocol, sReferer, sContentType, sContent, listClientCookies, listServerCookies, bc, activity, sbError);
		if( sPageReturn == null ){
			ApplicationController.vShowError("Failed to post bug/feature, HTTP failure: " + sbError);
			return;
		} else {
			String sSuccessSnippet = "<TITLE>Posting Bug -- Please wait</TITLE>";
			if( sPageReturn.indexOf(sSuccessSnippet) >= 0 ){
				ApplicationController.vShowStatus("Bug/Feature report posted.");
				ApplicationController.getInstance().vShowErrorDialog("Bug/Feature report posted. Thanks for your feedback.");
			} else {
				ApplicationController.vShowError("Failed to post bug/feature, Bugzilla failure (check system.err)");
				System.err.println("page: " + sPageReturn);
				return;
			}
		}

	}

}

// GET /bugzilla/enter_bug.cgi?Bugzilla_login=j.chamberlain%40opendap.org&Bugzilla_password=empty&product=DODS+clients&GoAheadAndLogIn=Login HTTP/1.1
// Host: dodsdev.gso.uri.edu
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.7) Gecko/20040707 Firefox/0.9.2
// Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Cookie: Bugzilla_login=j.chamberlain@opendap.org; Bugzilla_logincookie=396
//
// HTTP/1.x 200 OK
// Date: Mon, 12 Jul 2004 17:37:06 GMT
// Server: Apache/2.0.40 (Red Hat Linux)
// Set-Cookie: Bugzilla_login=j.chamberlain@opendap.org ; path=/; expires=Sun, 30-Jun-2029 00:00:00 GMT
// Set-Cookie: Bugzilla_logincookie=399 ; path=/; expires=Sun, 30-Jun-2029 00:00:00 GMT
// Set-Cookie: Bugzilla_password= ; path=/; expires=Sun, 30-Jun-80 00:00:00 GMT
// Connection: close
// Transfer-Encoding: chunked
// Content-Type: text/html; charset=ISO-8859-1
// ----------------------------------------------------------
// http://dodsdev.gso.uri.edu/bugzilla/post_bug.cgi
//
// POST /bugzilla/post_bug.cgi HTTP/1.1
// Host: dodsdev.gso.uri.edu
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.7) Gecko/20040707 Firefox/0.9.2
// Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Referer: http://dodsdev.gso.uri.edu/bugzilla/enter_bug.cgi?Bugzilla_login=j.chamberlain%40opendap.org&Bugzilla_password=empty&product=DODS+clients&GoAheadAndLogIn=Login
// Cookie: Bugzilla_login=j.chamberlain@opendap.org; Bugzilla_logincookie=399
// Content-Type: application/x-www-form-urlencoded
// Content-Length: 359
// reporter=j.chamberlain%40opendap.org&product=DODS+clients&version=3.1.x&component=ODC&rep_platform=PC&op_sys=Windows+2000&priority=P1&bug_severity=normal&assigned_to=&cc=&bug_file_loc=http%3A%2F%2Fnone2&short_desc=test+bug+for+ODC+feedback+%232&comment=content+description+for+%232%0D%0Asecond+line+for+description+%232&bit-256=0&bit-512=0&form_name=enter_bug

//
// HTTP/1.x 200 OK
// Date: Mon, 12 Jul 2004 17:37:46 GMT
// Server: Apache/2.0.40 (Red Hat Linux)
// Set-Cookie: PLATFORM=DODS clients ; path=/ ; expires=Sun, 30-Jun-2029 00:00:00 GMT
// Set-Cookie: VERSION-DODS clients=3.1.x ; path=/ ; expires=Sun, 30-Jun-2029 00:00:00 GMT
// Connection: close
// Transfer-Encoding: chunked
// Content-Type: text/html; charset=ISO-8859-1
// ----------------------------------------------------------
//

