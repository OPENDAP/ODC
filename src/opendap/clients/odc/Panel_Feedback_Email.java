package opendap.clients.odc;

/**
 * Title:        Panel_Feedback_Email
 * Description:  User can send developers comments via email
 * Copyright:    Copyright (c) 2004
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.57
 */

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

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
			panelUserEmail.add(new JLabel("Email Address: "));
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
					    Panel_Feedback_Email.this.vSendEmail();
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

	void vSendEmail(){

		StringBuffer sbError = new StringBuffer(250);

		// Initialize Post Office
		String sMailHost = ConfigurationManager.getInstance().getProperty_MailHost();
		if( !mPostOffice.zInitialize(sMailHost, sbError) ){
			sbError.insert(0 , "Failed to initialize post office: ");
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

	boolean zSendEmail(String sRecipientAddress, String sFromAddress, String sReturnAddress, String sSubject, String sMessageContent, StringBuffer sbError){
		try {
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




