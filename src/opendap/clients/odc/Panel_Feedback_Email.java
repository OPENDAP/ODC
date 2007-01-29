package opendap.clients.odc;

/**
 * Title:        Panel_Feedback_Email
 * Description:  User can send developers comments via email
 * Copyright:    Copyright (c) 2004
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.59
 */

/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

public class Panel_Feedback_Email extends JPanel {

    public Panel_Feedback_Email() {}

	JScrollPane jspDisplay = new JScrollPane();
	private final JTextArea jtaDisplay = new JTextArea("");
	private final JTextField jtfUserEmail = new JTextField();
	private final JCheckBox jcheckIncludeMe = new JCheckBox("Include me on mailing list");
	private final JCheckBox jcheckCanWeContactYou = new JCheckBox("Can we contact you");
	private final PostOffice mPostOffice = new PostOffice();

    boolean zInitialize(StringBuffer sbError){

        try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			setBorder(borderStandard);

			setLayout(new java.awt.BorderLayout());

			// Header
			add( new JLabel("Enter your comment or suggestion:"), BorderLayout.NORTH );

			// Create and intialize the text area
			Styles.vApply(Styles.STYLE_Terminal, jtaDisplay);
			jtaDisplay.setLineWrap(true);
			jtaDisplay.setWrapStyleWord(true);
			jspDisplay.setViewportView(jtaDisplay);
		    add(jspDisplay, java.awt.BorderLayout.CENTER);

			// User email address panel
			JPanel panelUserEmail = new JPanel();
			panelUserEmail.setLayout(new BoxLayout(panelUserEmail, BoxLayout.X_AXIS));
			panelUserEmail.add(new JLabel("Your Email Address: "));
			panelUserEmail.add(jtfUserEmail);
			String sUserEmail = ConfigurationManager.getInstance().getProperty_FEEDBACK_EmailUserAddress();
			if( sUserEmail != null && sUserEmail.trim().length() > 0 ) jtfUserEmail.setText(sUserEmail);

			// Layout options
			panelUserEmail.setAlignmentX(Component.LEFT_ALIGNMENT);
			jcheckCanWeContactYou.setAlignmentX(Component.LEFT_ALIGNMENT);
			jcheckIncludeMe.setAlignmentX(Component.LEFT_ALIGNMENT);
			JPanel panelOptions = new JPanel();
			panelOptions.setLayout(new BoxLayout(panelOptions, BoxLayout.Y_AXIS));
			panelOptions.add(panelUserEmail);
			panelOptions.add(jcheckCanWeContactYou);
			panelOptions.add(jcheckIncludeMe);

			// Create and intialize the command panel
			JPanel jpanelCommand = new JPanel();
			JPanel jpanelCommandButtons = new JPanel();

			jpanelCommand.setLayout( new BorderLayout() );
			jpanelCommand.add( panelOptions, BorderLayout.NORTH );

			// Show Log
			JButton jbuttonSendEmail = new JButton("Send Email");
			jbuttonSendEmail.addActionListener(
				new ActionListener(){
				    public void actionPerformed(ActionEvent event) {
					    Panel_Feedback_Email.this.vSendEmail_Relay();
					}
				}
			);

			// Clear Display
			JButton jbuttonClearDisplay = new JButton("Clear Display");
			jbuttonClearDisplay.addActionListener(
				new ActionListener(){
	    			public void actionPerformed(ActionEvent event) {
						Panel_Feedback_Email.this.vClearDisplay();
					}
				}
			);

			jpanelCommandButtons.add( jbuttonSendEmail );
			jpanelCommandButtons.add( jbuttonClearDisplay );

			jpanelCommand.add( jpanelCommandButtons, BorderLayout.SOUTH );

			add(jpanelCommand, BorderLayout.SOUTH);

            return true;

        } catch(Exception ex){
			Utility.vUnexpectedError(ex, sbError);
            return false;
        }
	}

	void vClearDisplay(){
		jtaDisplay.setText("");
	}

	// sends mail via a web-server based relay
	// the relay is a perl script on the server (see src/clients/odc/cgi)
	void vSendEmail_Relay(){
		StringBuffer sbError = new StringBuffer(250);
		String sMailRelayURL = ConfigurationManager.getInstance().getProperty_FEEDBACK_EmailRelayURL();
		if( sMailRelayURL == null ){
			ApplicationController.vShowError("Unable to obtain mail relay URL from configuration manager.");
			return;
		}
		String sToAddress = "feedback@opendap.org";
		String sFromAddress = jtfUserEmail.getText();
		String sReturnAddress = jtfUserEmail.getText(); // same as from address
		String sSubject = "[ODC feedback]";

		// determine message content
		String sDisplayText = jtaDisplay.getText().trim();
		StringBuffer sbMessage = new StringBuffer(jtaDisplay.getText().length() + 250);
		sbMessage.append( (sDisplayText.length() == 0 ? "[no message]" : sDisplayText) );
		sbMessage.append( "[add me to mailing list: " + (jcheckIncludeMe.isSelected() ? "yes]" : "no]") );
		sbMessage.append( "[can we contact you: " + (jcheckCanWeContactYou.isSelected() ? "yes]" : "no]") );
		String sMessageContent = sbMessage.toString();

		String sSubject_encoded, sContent_encoded;
		try {
			sSubject_encoded = java.net.URLEncoder.encode(sSubject, "UTF-8");
			sContent_encoded = java.net.URLEncoder.encode(sMessageContent, "UTF-8");
		} catch( Exception ex ) {
			ApplicationController.vShowError("Failed to construct mail message because of an encoding failure: " + ex);
			return;
		}

		// assemble post content
		StringBuffer sbContent = new StringBuffer(1000);
		sbContent.append("send_to=").append(sToAddress).append('&');
		sbContent.append("reply_to=").append(sReturnAddress).append('&');
		sbContent.append("subject=").append(sSubject_encoded).append('&');
		sbContent.append("content=").append(sContent_encoded);

		// make posting
		String sCommand = "POST";
		String sMailHost = ConfigurationManager.getInstance().getProperty_MailHost();
		int iPort = 80;
		String sQuery = null;
		String sProtocol = "HTTP/1.1";
		String sReferer = null;
		String sContentType = "application/x-www-form-urlencoded";
		String sContent = sbContent.toString();
		java.util.ArrayList listServerCookies = null;
		java.util.ArrayList listClientCookies = null;
		String sPageReturn = IO.getStaticContent(sCommand, sMailHost, iPort, sMailRelayURL, sQuery, sProtocol, sReferer, sContentType, sContent, listClientCookies, listServerCookies, null, null, sbError);
		String sHostAddress = sMailHost + ":" + iPort + sMailRelayURL;
		if( sPageReturn == null ){
			ApplicationController.vShowError("Failed to email to " + sHostAddress + ":" + iPort + sMailRelayURL + ", HTTP failure: " + sbError);
			return;
		} else {
			if( sPageReturn.toUpperCase().startsWith("MAIL SENT") ){
				jtaDisplay.setText(""); // clear the display
				ApplicationController.getInstance().vShowErrorDialog("Comment emailed. Thanks for your feedback.");
			} if( sPageReturn.toUpperCase().startsWith("ERROR:") ){
				ApplicationController.vShowError("Feedback email attempt to " + sHostAddress + " failed: " + sPageReturn);
			} else {
				ApplicationController.vShowError("Feedback email attempt to " + sHostAddress + " had unexpected result: " + sPageReturn);
			}
		}

	}

	void vSendEmail_Direct(){

		StringBuffer sbError = new StringBuffer(250);

		// Initialize Post Office
		String sMailHost = ConfigurationManager.getInstance().getProperty_MailHost();
		if( !mPostOffice.zInitialize(sMailHost, sbError) ){
			ApplicationController.vShowError("Failed to initialize post office: " + sbError);
			return;
		}

		// determine email parameters
		String sRecipientAddress = ConfigurationManager.getInstance().getProperty_FEEDBACK_EmailAddress();
		if( Utility.zNullOrBlank( sRecipientAddress  )){
			ApplicationController.vShowError("The recipient address for the email is not defined. Check configuration.");
			return;
		}
		String sFromAddress = jtfUserEmail.getText();
		String sReturnAddress = null; // same as from address
		String sSubject = "[ODC feedback]";

		// Store User Email
		String sUserEmail = sFromAddress;
		if( sUserEmail != null && sUserEmail.trim().length() > 0 ){
			String sStoredEmail = ConfigurationManager.getInstance().getProperty_FEEDBACK_EmailUserAddress();
			if( ! sUserEmail.equals(sStoredEmail) ){
				ConfigurationManager.getInstance().setOption(ConfigurationManager.PROPERTY_FEEDBACK_EmailUserAddress, sUserEmail);
			}
		}

		// determine message content
		String sDisplayText = jtaDisplay.getText().trim();
		StringBuffer sbMessage = new StringBuffer(jtaDisplay.getText().length() + 250);
		sbMessage.append( (sDisplayText.length() == 0 ? "[no message]" : sDisplayText) );
		sbMessage.append( "[add me to mailing list: " + (jcheckIncludeMe.isSelected() ? "yes]" : "no]") );
		sbMessage.append( "[can we contact you: " + (jcheckCanWeContactYou.isSelected() ? "yes]" : "no]") );
		String sMessageContent = sbMessage.toString();

		if( mPostOffice.zSendEmail(sRecipientAddress, sFromAddress, sReturnAddress, sSubject, sMessageContent, sbError) ){
			jtaDisplay.setText(""); // clear the display
			ApplicationController.getInstance().vShowErrorDialog("Comment emailed. Thanks for your feedback.");
		} else {
			ApplicationController.vShowError("Feedback email attempt failed: " + sbError);
		}
	}

}

class PostOffice {

	private final static String DEFAULT_MailUser = "ODC_Feedback";

	PostOffice(){}

	boolean zInitialize(String sMailHost, StringBuffer sbError){
		try {

			// validate that mail host is reachable todo
			return true;
		} catch (Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	String getMailHost(){ return ConfigurationManager.getInstance().getProperty_MailHost(); }
	int getMailPort(){ return ConfigurationManager.getInstance().getProperty_MailPort(); }

//mail.debug 	boolean 	The initial debug mode. Default is false.
//mail.from 	String 	The return email address of the current user, used by the InternetAddress method getLocalAddress.
//mail.mime.address.strict 	boolean 	The MimeMessage class uses the InternetAddress method parseHeader to parse headers in messages. This property controls the strict flag passed to the parseHeader method. The default is true.
//mail.host 	String 	The default host name of the mail server for both Stores and Transports. Used if the mail.protocol.host property isn't set.
//mail.store.protocol 	String 	Specifies the default message access protocol. The Session method getStore() returns a Store object that implements this protocol. By default the first Store provider in the configuration files is returned.
//mail.transport.protocol 	String 	Specifies the default message access protocol. The Session method getTransport() returns a Transport object that implements this protocol. By default the first Transport provider in the configuration files is returned.
//mail.user 	String 	The default user name to use when connecting to the mail server. Used if the mail.protocol.user property isn't set.
//mail..class 	String 	Specifies the fully qualified class name of the provider for the specified protocol. Used in cases where more than one provider for a given protocol exists; this property can be used to specify which provider to use by default. The provider must still be listed in a configuration file.
//mail..host 	String 	The host name of the mail server for the specified protocol. Overrides the mail.host property.
//mail..port 	int 	The port number of the mail server for the specified protocol. If not specified the protocol's default port number is used.
//mail..user

	boolean zSendEmail( String sRecipientAddress, String sFromAddress, String sReturnAddress, String sSubject, String sMessageContent, StringBuffer sbError ){
		return zSendEmail( DEFAULT_MailUser, sRecipientAddress, sFromAddress, sReturnAddress, sSubject, sMessageContent, sbError );
	}

	boolean zSendEmail( String sMailUser, String sRecipientAddress, String sFromAddress, String sReturnAddress, String sSubject, String sMessageContent, StringBuffer sbError){
		try {
System.out.println("sMailUser: " + sMailUser);
System.out.println("sFromAddress: " + sFromAddress);
System.out.println("sRecipientAddress: " + sRecipientAddress);
System.out.println("sReturnAddress: " + sReturnAddress);
			String sMailServerHost = getMailHost();
			if (sMailServerHost == null) {
				sbError.append("no mail host defined");
				return false;
			}
			int iMailServerPort = getMailPort();
			java.util.Properties mail_props = new java.util.Properties();
			mail_props.put("mail.transport.protocol", "smtp");
			mail_props.put("mail.smtp.host", sMailServerHost);
			mail_props.put("mail.smtp.port", Integer.toString(iMailServerPort));
			if( sMailUser != null ) mail_props.put("mail.user", sMailUser);
			javax.mail.Session theJavaMailSession = javax.mail.Session.getInstance(mail_props, null);
//			theJavaMailSession.setDebug(true);
			if(theJavaMailSession==null){
				sbError.append("unable to create mail session");
				return false;
			}
			InternetAddress addressRecipient, addressReturn, addressFrom;
			if(sRecipientAddress == null) {
				sbError.append("recipient missing");
				return false;
			}
			javax.mail.Message msg = new MimeMessage(theJavaMailSession);

			// determine recipient address
			if( Utility.zNullOrBlank(sRecipientAddress) ){
				sbError.append("Recipient address is missing."); return false;
			} else {
				try { addressRecipient = new InternetAddress(sRecipientAddress); } catch(Exception ex) { sbError.append("failed to parse recipient address [" + sRecipientAddress + "]: " + ex); return false; }
			}

			// determine from address
			if( Utility.zNullOrBlank(sFromAddress) ){
				addressFrom = addressRecipient;
			} else {
				try { addressFrom = new InternetAddress(sFromAddress); } catch(Exception ex) { sbError.append("failed to parse from address [" + sFromAddress + "]: " + ex); return false; }
			}

			// determine return address
			if( Utility.zNullOrBlank(sReturnAddress) ){
				addressReturn = addressFrom;
			} else {
				try { addressReturn = new InternetAddress(sReturnAddress); } catch(Exception ex) { sbError.append("failed to parse return address [" + sReturnAddress + "]: " + ex); return false; }
			}

			// define message parameters and content
			// set return address todo
System.out.println("from msg: " + addressFrom.toString());
			msg.setFrom(addressFrom);
			msg.setRecipient(javax.mail.Message.RecipientType.TO, addressRecipient);
			msg.setSubject(sSubject);
			msg.setSentDate(new java.util.Date());
			msg.setDataHandler(new DataHandler(sMessageContent, "text/html"));
//			msg.setContent(sMessageContent, "text/html");
			msg.setContent(sMessageContent, "text/plain");
			msg.setHeader("ODC_Version", ApplicationController.getInstance().getVersionString());

			// send message
			Transport theTransport = theJavaMailSession.getTransport("smtp");
			if(theTransport==null){ sbError.append("unable to get smtp transport"); return false; }
			try {
				theTransport.send(msg);
				ApplicationController.vShowStatus("Mail sent to: " + sRecipientAddress + " via " + sMailServerHost + ":" + iMailServerPort);
			} catch( javax.mail.MessagingException exMessaging ){
				sbError.append("Failed to send message to " + sRecipientAddress + ". You may be behind a firewall or the mail server may be down or non-operational. Error: " + exMessaging);
				return false;
			}
			return true;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

}




