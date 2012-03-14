package opendap.clients.odc.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import opendap.clients.odc.ApplicationController;

public class Message extends JPanel {
	private static boolean zActive;
	private static Message instance = null;
	private static JDialog jd;
	private static JTextArea jtaMessage;
	private static JOptionPane jop;
	private static JLabel labelSuppressionInfo;
	public static void show( String sMessage ){

		if( zActive ) return; // do not show two messages simultaneously

		try {

			zActive = true;

			// setup picker dialog if it does not exist yet
			if( instance == null ){
				instance = new Message();
				instance.setLayout(new BorderLayout());
				jtaMessage = new JTextArea();
				jtaMessage.setLineWrap(true);
				labelSuppressionInfo = new JLabel( "To suppress error dialogs use config setting 'ShowErrorPopups'" ); 
				JScrollPane jsp = new JScrollPane(jtaMessage);
				instance.add( jsp, BorderLayout.CENTER );
				instance.add( labelSuppressionInfo, BorderLayout.SOUTH ); 
			}

			// activate message box
			jtaMessage.setText(sMessage);
			jop = new JOptionPane(instance, JOptionPane.PLAIN_MESSAGE);
			jd = jop.createDialog( ApplicationController.getInstance().getAppFrame(), "ODC Message" );

			int len = sMessage.length();
			int iWidth, iHeight;
			Dimension dimScreen = Toolkit.getDefaultToolkit().getScreenSize();
			int screen_width = (int)dimScreen.getWidth();
			int screen_height = (int)dimScreen.getHeight();
			if( len < 120 ){
				iWidth = 400;
				iHeight = 160;
			} else if( len < 1000 ) {
				iWidth = 600;
				iHeight = 400;
			} else {
				iWidth = (int)(dimScreen.getWidth() * .80f);
				iHeight = (int)(dimScreen.getHeight() * .80f);
			}
			jd.setSize(new Dimension(iWidth, iHeight));
			jd.setLocation((screen_width - iWidth)/2, (screen_height - iHeight)/2);

			jd.setVisible( true );
		} catch( Exception ex ) {
			// ignore errors
		} finally {
			zActive = false;
		}

		return;
	}
}
