package opendap.clients.odc;

/**
 * Title:        Panel_Feedback_Bug
 * Description:  Output text area which displays messages and data
 * Copyright:    Copyright (c) 2004-6
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.57
 */

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import java.util.ArrayList;

public class Panel_Feedback_Bug extends JPanel {

    public Panel_Feedback_Bug() {}

	public final static String WIKI_LineBreak = "[[BR]]";

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
		"feedback",
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

			// the bug summary is not used because the developer is supposed to review
			// user submitted bugs and retitle them appropriately
//			JLabel labelSummary = new JLabel("  Summary (bug title or one-line summary phrase):");
//			labelSummary.setAlignmentX(Component.LEFT_ALIGNMENT);
//			jtfShortDescription.setMaximumSize(new Dimension(1600, 20));
//			panelText.add(labelSummary);
//			panelText.add(jtfShortDescription);
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
		jtfShortDescription.setText("");
		jtaFullDescription.setText("");
		jtaSystemLog.setText("");
	}

	void vSendBug(){

		// get parameters
		String sBugHost = ConfigurationManager.getInstance().getProperty_FEEDBACK_BugHost();
		String sBugRoot = ConfigurationManager.getInstance().getProperty_FEEDBACK_BugRoot();
		int iBugPort    = ConfigurationManager.getInstance().getProperty_FEEDBACK_BugPort();

		// request parameters
		String sCommand = "GET";
		int iPort = iBugPort;
		String sPath;
		String sQuery = null;
		String sProtocol = "HTTP/1.1";
		String sContentType = null;
		String sContent = null;
		String sReferer = null;
		String sBasicAuthentication = "amNoYW1iZXI6ZWR5dnU3OFl1";
		String[] eggLocation = new String[1];
		ArrayList listClientCookies = new ArrayList();
		ArrayList listServerCookies = new ArrayList();
		ByteCounter bc = null;
		Activity activity = null;
		StringBuffer sbError = new StringBuffer(80);
		String sPageReturn;

		// start session
		// http://scm.opendap.org:8090/trac
		sPath = sBugRoot;
		sPageReturn = IO.getStaticContent( sCommand, sBugHost, iPort, sPath, sQuery, sProtocol, sReferer, sContentType, sContent, listClientCookies, listServerCookies, sBasicAuthentication, eggLocation, bc, activity, sbError );
		if( sPageReturn == null ){
			ApplicationController.vShowError("Failed to contact bug server: " + sbError);
			return;
		}

		// make sure a session cookie has been obtained
		int xServerCookie = 1;
		while( true ){
			if( xServerCookie > listServerCookies.size() ){
				ApplicationController.vShowError("Bug server did not return expected session cookie");
				return;
			}
			Object oServerCookie = listServerCookies.get(xServerCookie-1);
			if( oServerCookie == null ){
				System.out.println("cookie " +  xServerCookie + " was null");
			} else {
				Cookie theCookie = IO.parseCookie( oServerCookie.toString(), sbError );
				if( theCookie == null ){
					ApplicationController.vShowError("Error parsing cookie " +  xServerCookie + ": " + sbError );
					return;
				}
				if( theCookie.sName.equalsIgnoreCase("trac_session") ){
					listClientCookies.add( theCookie.getClientCookie() ); // add authorization cookie
					System.out.println("added session cookie: " + oServerCookie);
					break;
				}
			}
			xServerCookie++;
		}

		// login
		// http://scm.opendap.org:8090/trac/login
		sPath = sBugRoot + "/login";
		sPageReturn = IO.getStaticContent( sCommand, sBugHost, iPort, sPath, sQuery, sProtocol, sReferer, sContentType, sContent, listClientCookies, listServerCookies, sBasicAuthentication, eggLocation, bc, activity, sbError );
		if( sPageReturn == null ){
			ApplicationController.vShowError("Failed to login: " + sbError);
			return;
		}

		// make sure authorization cookie is there
		xServerCookie = 1;
		while( true ){
			if( xServerCookie > listServerCookies.size() ){
				ApplicationController.vShowError("Bug server did not return expected authorization cookie after login");
				return;
			}
			Object oServerCookie = listServerCookies.get(xServerCookie-1);
			if( oServerCookie == null ){
				System.out.println("cookie " +  xServerCookie + " was null");
			} else {
				Cookie theCookie = IO.parseCookie( oServerCookie.toString(), sbError );
				if( theCookie == null ){
					ApplicationController.vShowError("Error parsing cookie " +  xServerCookie + ": " + sbError );
					return;
				}
				if( theCookie.sName.equalsIgnoreCase("trac_auth") ){
					listClientCookies.add( theCookie.getClientCookie() ); // add authorization cookie
					System.out.println("added authorization cookie: " + oServerCookie);
					break;
				}
			}
			xServerCookie++;
		}

		// Store User Email
		String sUserEmail = jtfUserEmail.getText();
		if( sUserEmail != null && sUserEmail.trim().length() > 0 ){
			String sStoredEmail = ConfigurationManager.getInstance().getProperty_FEEDBACK_EmailUserAddress();
			if( ! sUserEmail.equals(sStoredEmail) ){
				ConfigurationManager.getInstance().setOption(ConfigurationManager.PROPERTY_FEEDBACK_EmailUserAddress, sUserEmail);
			}
		}

		// Get Bug Summary
		String sBugSummary = "[user-submitted bug]";
		String sBugSummary_encoded = null;
		try {
			sBugSummary_encoded = java.net.URLEncoder.encode( sBugSummary, "UTF-8" );
		} catch( Throwable t ) {
			ApplicationController.vShowError("Failed to encode the bug summary: " + sBugSummary );
			return;
		}

		// Assemble Detailed Bug Description
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
		sbBugDescription.append("OPeNDAP Data Connector: User Feedback Bug" + WIKI_LineBreak);
		sbBugDescription.append("Version: " + ApplicationController.getInstance().getVersionString() + WIKI_LineBreak);
		sbBugDescription.append("Operating System: " + sOperatingSystem + WIKI_LineBreak);
		sbBugDescription.append("Kind of Problem: " + sKindOfProblem + WIKI_LineBreak);
		if( jtabAreaOfProblem.isVisible() ) sbBugDescription.append("Area of Problem: ").append(sAreaOfProblem).append(WIKI_LineBreak);
		if( sUserEmail.trim().length() == 0 ) sUserEmail = "[none provided]";
		sbBugDescription.append(WIKI_LineBreak + WIKI_LineBreak + "**************** USER EMAIL *******************" + WIKI_LineBreak);
		sbBugDescription.append(sUserEmail);
		sbBugDescription.append(WIKI_LineBreak + WIKI_LineBreak + "**************** USER BUG DESCRIPTION ********************" + WIKI_LineBreak);
		sbBugDescription.append(jtaFullDescription.getText());
		sbBugDescription.append(WIKI_LineBreak + WIKI_LineBreak + "**************** SYSTEM LOG ********************" + WIKI_LineBreak);
		sbBugDescription.append(jtaSystemLog.getText());
		String sBugDescription_encoded;
		try {
			sBugDescription_encoded = java.net.URLEncoder.encode( sbBugDescription.toString(), "UTF-8" );
		} catch(Exception ex) {
			ApplicationController.vShowError("Failed to emcode bug/feedback to UTF-8: " + ex);
			return;
		}

		String sBugCategory = "Bug";
		if( sNatureOfBug.equalsIgnoreCase( "New feature request" ) ){ sBugCategory = "Feature"; }

		// Assemble Posting
		// EXAMPLE // reporter=jchamber&summary=Coastline+outline+have+horizontal+lines&description=The+coastline%27s+sometimes+have+horizontal+lines+apparently+caused+by+the+renderer+not+knowing+the+correct+location+of+the+meridian.&mode=newticket&action=create&status=new&component=ODC&version=&severity=normal&keywords=&priority=normal&milestone=Java-OPeNDAP+Release&owner=jchamber&cc=&custom_category=Bug&create=Submit+ticket
		StringBuffer sbContent = new StringBuffer(1000);
		sbContent.append("reporter=jchamber").append('&');
		sbContent.append("summary=").append(sBugSummary_encoded).append('&');
		sbContent.append("description=").append(sBugDescription_encoded).append('&');
		sbContent.append("mode=newticket").append('&');
		sbContent.append("action=create").append('&');
		sbContent.append("status=new").append('&');
		sbContent.append("component=ODC").append('&');
		sbContent.append("version=").append('&'); // todo try using version string
		sbContent.append("severity=normal").append('&');
	    sbContent.append("keywords=").append('&');
		sbContent.append("priority=normal").append('&');
		sbContent.append("milestone=Java-OPeNDAP+Release").append('&');
		sbContent.append("owner=jchamber").append('&');
		sbContent.append("cc=").append('&');
		sbContent.append("custom_category=" + sBugCategory).append('&');
		sbContent.append("create=Submit+ticket");

		// Prepend Content Length // todo
		// EXAMPLE // Content-Length: 407
		// int iContentLength = sbContent.toString().length();

// test posting
//		sbContent.append("reporter=j.chamberlain%40opendap.org&product=DODS+clients&version=3.1.x&component=ODC&rep_platform=PC&op_sys=Windows+2000&priority=P1&bug_severity=normal&assigned_to=&cc=&bug_file_loc=http%3A%2F%2Fnone2&short_desc=test+bug+for+ODC+feedback+%232&comment=content+description+for+%232%0D%0Asecond+line+for+description+%232&bit-256=0&bit-512=0&form_name=enter_bug");

		// make posting
		sCommand = "POST";
		sPath = sBugRoot;
		sQuery = null;
		sProtocol = "HTTP/1.1";
		sReferer = "http://" + sBugHost + sBugRoot + "/newticket"; // Referer: http://scm.opendap.org:8090/trac/newticket
		sContentType = "application/x-www-form-urlencoded";
		sContent = sbContent.toString();
		listServerCookies.clear();
		sPageReturn = IO.getStaticContent(sCommand, sBugHost, iPort, sPath, sQuery, sProtocol, sReferer, sContentType, sContent, listClientCookies, listServerCookies, sBasicAuthentication, eggLocation, bc, activity, sbError);
		if( sPageReturn == null ){
			ApplicationController.vShowError("Failed to post bug/feature to " + sBugHost + ":" + iPort + sPath + ", HTTP failure: " + sbError);
			return;
		} else {
			if( eggLocation[0] != null && eggLocation[0].startsWith("/trac/ticket/") ){
				String sTicketNumber = eggLocation[0].substring(13);
				ApplicationController.vShowStatus("Bug/Feature report posted as ticket #" + sTicketNumber);
				ApplicationController.getInstance().vShowErrorDialog("Bug/Feature report posted as ticket #" + sTicketNumber + ". Thanks for your feedback.");
			} else {
				ApplicationController.vShowError("May have failed to post bug/feature, bug system returned unexpected response (check system.err)");
				System.err.println("page: " + sPageReturn);
				return;
			}
		}

	}

}

//http://scm.opendap.org:8090/trac/
//
//GET /trac/ HTTP/1.1
//Host: scm.opendap.org:8090
//User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.7.8) Gecko/20050511 Firefox/1.0.4
//Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
//Accept-Language: en-us,en;q=0.5
//Accept-Encoding: gzip,deflate
//Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
//Keep-Alive: 300
//Connection: keep-alive
//
//HTTP/1.x 200 OK
//Date: Sun, 14 May 2006 23:50:37 GMT
//Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
//Cache-Control: no-cache
//Expires: Fri, 01 Jan 1999 00:00:00 GMT
//Set-Cookie: trac_session=1be443bdb7392f575344e139; expires=Thu, 05-Sep-2019 02:30:38 GMT; Path=/trac;
//Content-Length: 8688
//Keep-Alive: timeout=15, max=100
//Connection: Keep-Alive
//Content-Type: text/html;charset=utf-8
//----------------------------------------------------------
//http://scm.opendap.org:8090/trac/login
//
//GET /trac/login HTTP/1.1
//Host: scm.opendap.org:8090
//User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.7.8) Gecko/20050511 Firefox/1.0.4
//Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
//Accept-Language: en-us,en;q=0.5
//Accept-Encoding: gzip,deflate
//Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
//Keep-Alive: 300
//Connection: keep-alive
//Referer: http://scm.opendap.org:8090/trac/
//Cookie: trac_session=1be443bdb7392f575344e139
//
//HTTP/1.x 401 Authorization Required
//Date: Sun, 14 May 2006 23:51:08 GMT
//Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
//WWW-Authenticate: Basic realm="Trac"
//Content-Length: 498
//Keep-Alive: timeout=15, max=100
//Connection: Keep-Alive
//Content-Type: text/html; charset=iso-8859-1
//----------------------------------------------------------
//http://scm.opendap.org:8090/trac/login
//
//GET /trac/login HTTP/1.1
//Host: scm.opendap.org:8090
//User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.7.8) Gecko/20050511 Firefox/1.0.4
//Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
//Accept-Language: en-us,en;q=0.5
//Accept-Encoding: gzip,deflate
//Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
//Keep-Alive: 300
//Connection: keep-alive
//Referer: http://scm.opendap.org:8090/trac/
//Cookie: trac_session=1be443bdb7392f575344e139
//Authorization: Basic amNoYW1iZXI6ZWR5dnU3OFl1
//
//HTTP/1.x 302 OK
//Date: Sun, 14 May 2006 23:51:22 GMT
//Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
//Pragma: no-cache
//Cache-Control: no-cache
//Expires: Fri, 01 Jan 1999 00:00:00 GMT
//Set-Cookie: trac_auth=d00c4ea3a8bb1834e51586063a6ef0ed; Path=/trac;
//Location: http://scm.opendap.org:8090/trac/
//Keep-Alive: timeout=15, max=99
//Connection: Keep-Alive
//Transfer-Encoding: chunked
//Content-Type: text/plain
//----------------------------------------------------------
//http://scm.opendap.org:8090/trac/
//
//GET /trac/ HTTP/1.1
//Host: scm.opendap.org:8090
//User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.7.8) Gecko/20050511 Firefox/1.0.4
//Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
//Accept-Language: en-us,en;q=0.5
//Accept-Encoding: gzip,deflate
//Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
//Keep-Alive: 300
//Connection: keep-alive
//Referer: http://scm.opendap.org:8090/trac/
//Cookie: trac_session=1be443bdb7392f575344e139; trac_auth=d00c4ea3a8bb1834e51586063a6ef0ed
//Authorization: Basic amNoYW1iZXI6ZWR5dnU3OFl1
//
//HTTP/1.x 200 OK
//Date: Sun, 14 May 2006 23:51:23 GMT
//Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
//Cache-Control: no-cache
//Expires: Fri, 01 Jan 1999 00:00:00 GMT
//Set-Cookie: trac_session=1be443bdb7392f575344e139; expires=Thu, 05-Sep-2019 02:31:23 GMT; Path=/trac;
//Content-Length: 9687
//Keep-Alive: timeout=15, max=100
//Connection: Keep-Alive
//Content-Type: text/html;charset=utf-8
//
// http://scm.opendap.org:8090/trac/login
//
// GET /trac/login HTTP/1.1
// Host: scm.opendap.org:8090
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1
// Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Referer: http://scm.opendap.org:8090/trac/report
// Cookie: trac_session=7b3e7bcc2147856d91b233a0; trac_auth=07de33e72226614b34064d97848d2db2
// Authorization: Basic amNoYW1iZXI6ZWR5dnU3OFl1
//
// HTTP/1.x 302 OK
// Date: Fri, 05 May 2006 18:31:23 GMT
// Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
// Pragma: no-cache
// Cache-Control: no-cache
// Expires: Fri, 01 Jan 1999 00:00:00 GMT
// Set-Cookie: trac_auth=765e50fde47ec92cef30edb55374b8c1; Path=/trac;
// Location: http://scm.opendap.org:8090/trac/report
// Keep-Alive: timeout=15, max=98
// Connection: Keep-Alive
// Transfer-Encoding: chunked
// Content-Type: text/plain
// ----------------------------------------------------------
// http://scm.opendap.org:8090/trac/report
//
// GET /trac/report HTTP/1.1
// Host: scm.opendap.org:8090
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1
// Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Referer: http://scm.opendap.org:8090/trac/report
// Cookie: trac_session=7b3e7bcc2147856d91b233a0; trac_auth=765e50fde47ec92cef30edb55374b8c1
// Authorization: Basic amNoYW1iZXI6ZWR5dnU3OFl1
//
// HTTP/1.x 200 OK
// Date: Fri, 05 May 2006 18:31:24 GMT
// Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
// Cache-Control: no-cache
// Expires: Fri, 01 Jan 1999 00:00:00 GMT
// Set-Cookie: trac_session=7b3e7bcc2147856d91b233a0; expires=Mon, 26-Aug-2019 21:11:24 GMT; Path=/trac;
// Content-Length: 10136
// Keep-Alive: timeout=15, max=100
// Connection: Keep-Alive
// Content-Type: text/html;charset=utf-8
// ----------------------------------------------------------
// http://scm.opendap.org:8090/trac/newticket
//
// GET /trac/newticket HTTP/1.1
// Host: scm.opendap.org:8090
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1
// Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Referer: http://scm.opendap.org:8090/trac/report
// Cookie: trac_session=7b3e7bcc2147856d91b233a0; trac_auth=765e50fde47ec92cef30edb55374b8c1
// Authorization: Basic amNoYW1iZXI6ZWR5dnU3OFl1
//
// HTTP/1.x 200 OK
// Date: Fri, 05 May 2006 18:31:48 GMT
// Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
// Cache-Control: no-cache
// Expires: Fri, 01 Jan 1999 00:00:00 GMT
// Set-Cookie: trac_session=7b3e7bcc2147856d91b233a0; expires=Mon, 26-Aug-2019 21:11:48 GMT; Path=/trac;
// Content-Length: 8206
// Keep-Alive: timeout=15, max=100
// Connection: Keep-Alive
// Content-Type: text/html;charset=utf-8
// ----------------------------------------------------------
// http://scm.opendap.org:8090/static-trac/css/ticket.css
//
// GET /static-trac/css/ticket.css HTTP/1.1
// Host: scm.opendap.org:8090
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1
// Accept: text/css,*/*;q=0.1
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Referer: http://scm.opendap.org:8090/trac/newticket
//
// HTTP/1.x 200 OK
// Date: Fri, 05 May 2006 18:31:48 GMT
// Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
// Last-Modified: Fri, 18 Mar 2005 15:43:21 GMT
// Etag: "c290d-81e-3ce93840"
// Accept-Ranges: bytes
// Content-Length: 2078
// Keep-Alive: timeout=15, max=100
// Connection: Keep-Alive
// Content-Type: text/css
// ----------------------------------------------------------
// http://scm.opendap.org:8090/static-trac/edit_toolbar.png
//
// GET /static-trac/edit_toolbar.png HTTP/1.1
// Host: scm.opendap.org:8090
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1
// Accept: image/png,*/*;q=0.5
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Referer: http://scm.opendap.org:8090/static-trac/css/trac.css
//
// HTTP/1.x 200 OK
// Date: Fri, 05 May 2006 18:31:48 GMT
// Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
// Last-Modified: Fri, 18 Mar 2005 15:43:21 GMT
// Etag: "f6102-3d1-3ce93840"
// Accept-Ranges: bytes
// Content-Length: 977
// Keep-Alive: timeout=15, max=99
// Connection: Keep-Alive
// Content-Type: image/png
// ----------------------------------------------------------
// http://scm.opendap.org:8090/trac#preview
//
// POST /trac HTTP/1.1
// Host: scm.opendap.org:8090
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1
// Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Referer: http://scm.opendap.org:8090/trac/newticket
// Cookie: trac_session=7b3e7bcc2147856d91b233a0; trac_auth=765e50fde47ec92cef30edb55374b8c1
// Content-Type: application/x-www-form-urlencoded
// Content-Length: 407
// reporter=jchamber&summary=Coastline+outline+have+horizontal+lines&description=The+coastline%27s+sometimes+have+horizontal+lines+apparently+caused+by+the+renderer+not+knowing+the+correct+location+of+the+meridian.&mode=newticket&action=create&status=new&component=ODC&version=&severity=normal&keywords=&priority=normal&milestone=Java-OPeNDAP+Release&owner=jchamber&cc=&custom_category=Bug&create=Submit+ticket
// HTTP/1.x 302 OK
// Date: Fri, 05 May 2006 19:01:13 GMT
// Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
// Pragma: no-cache
// Cache-Control: no-cache
// Expires: Fri, 01 Jan 1999 00:00:00 GMT
// Set-Cookie: trac_session=7b3e7bcc2147856d91b233a0; expires=Mon, 26-Aug-2019 21:41:13 GMT; Path=/trac;
// Location: /trac/ticket/369
// Keep-Alive: timeout=15, max=100
// Connection: Keep-Alive
// Transfer-Encoding: chunked
// Content-Type: text/plain
// ----------------------------------------------------------
// http://scm.opendap.org:8090/trac/ticket/369#preview
//
// GET /trac/ticket/369 HTTP/1.1
// Host: scm.opendap.org:8090
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1
// Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Referer: http://scm.opendap.org:8090/trac/newticket
// Cookie: trac_session=7b3e7bcc2147856d91b233a0; trac_auth=765e50fde47ec92cef30edb55374b8c1
// Authorization: Basic amNoYW1iZXI6ZWR5dnU3OFl1
//
// HTTP/1.x 200 OK
// Date: Fri, 05 May 2006 19:01:13 GMT
// Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
// Cache-Control: no-cache
// Expires: Fri, 01 Jan 1999 00:00:00 GMT
// Set-Cookie: trac_session=7b3e7bcc2147856d91b233a0; expires=Mon, 26-Aug-2019 21:41:13 GMT; Path=/trac;
// Content-Length: 12201
// Keep-Alive: timeout=15, max=99
// Connection: Keep-Alive
// Content-Type: text/html;charset=utf-8
// ----------------------------------------------------------
// http://scm.opendap.org:8090/trac#preview
//
// POST /trac HTTP/1.1
// Host: scm.opendap.org:8090
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1
// Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Referer: http://scm.opendap.org:8090/trac/ticket/369
// Cookie: trac_session=7b3e7bcc2147856d91b233a0; trac_auth=765e50fde47ec92cef30edb55374b8c1
// Content-Type: application/x-www-form-urlencoded
// Content-Length: 388
// mode=ticket&id=369&author=jchamber&comment=&summary=Coastline+outline+have+horizontal+lines&description=The+coastline%27s+sometimes+have+horizontal+lines+apparently+caused+by+the+renderer+not+knowing+the+correct+location+of+the+meridian.&reporter=jchamber&component=ODC&version=&severity=normal&keywords=&priority=normal&milestone=Java-OPeNDAP+Release&cc=&custom_category=Bug&action=leave
// HTTP/1.x 302 OK
// Date: Fri, 05 May 2006 19:02:06 GMT
// Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
// Pragma: no-cache
// Cache-Control: no-cache
// Expires: Fri, 01 Jan 1999 00:00:00 GMT
// Set-Cookie: trac_session=7b3e7bcc2147856d91b233a0; expires=Mon, 26-Aug-2019 21:42:06 GMT; Path=/trac;
// Location: /trac/ticket/369
// Keep-Alive: timeout=15, max=100
// Connection: Keep-Alive
// Transfer-Encoding: chunked
// Content-Type: text/plain
// ----------------------------------------------------------
// http://scm.opendap.org:8090/trac/ticket/369#preview
//
// GET /trac/ticket/369 HTTP/1.1
// Host: scm.opendap.org:8090
// User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.0; en-US; rv:1.8.0.1) Gecko/20060111 Firefox/1.5.0.1
// Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
// Accept-Language: en-us,en;q=0.5
// Accept-Encoding: gzip,deflate
// Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
// Keep-Alive: 300
// Connection: keep-alive
// Referer: http://scm.opendap.org:8090/trac/ticket/369
// Cookie: trac_session=7b3e7bcc2147856d91b233a0; trac_auth=765e50fde47ec92cef30edb55374b8c1
// Authorization: Basic amNoYW1iZXI6ZWR5dnU3OFl1
//
// HTTP/1.x 200 OK
// Date: Fri, 05 May 2006 19:02:06 GMT
// Server: Apache/2.0.54 (Unix) DAV/2 SVN/1.2.0
// Cache-Control: no-cache
// Expires: Fri, 01 Jan 1999 00:00:00 GMT
// Set-Cookie: trac_session=7b3e7bcc2147856d91b233a0; expires=Mon, 26-Aug-2019 21:42:06 GMT; Path=/trac;
// Content-Length: 12201
// Keep-Alive: timeout=15, max=100
// Connection: Keep-Alive
// Content-Type: text/html;charset=utf-8
