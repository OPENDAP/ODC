package opendap.clients.odc;

/**
 * Title:        Panel_View_Text
 * Description:  Output text area which displays messages and data
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.32
 */

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

public class Panel_View_Text extends JPanel {

    public Panel_View_Text() {}

	JScrollPane jspDisplay = new JScrollPane();
	private final JTextArea jtaDisplay = new JTextArea("");
	private final JTextField jtfCommand = new JTextField();

    boolean zInitialize(StringBuffer sbError){

        try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			this.setBorder(borderStandard);

			this.setLayout(new java.awt.BorderLayout());

			// Create and intialize the text area
			Styles.vApply(Styles.STYLE_Terminal, jtaDisplay);
			jtaDisplay.setLineWrap(true);
			jtaDisplay.setWrapStyleWord(true);
			jspDisplay.setViewportView(jtaDisplay);
		    this.add(jspDisplay, java.awt.BorderLayout.CENTER);

			// Add special warning message to display jta
			jtaDisplay.addKeyListener(
				new KeyListener(){
					public void keyPressed(KeyEvent ke){
						if( ke.getKeyCode() == ke.VK_ENTER ){
						    // int iCaretPosition = jtaDisplay.getCaretPosition();
							// JOptionPane.showMessageDialog(Panel_View_Text.this, "caret position: " + iCaretPosition, "How to Enter Commands", JOptionPane.OK_OPTION);
							JOptionPane.showMessageDialog(Panel_View_Text.this, "Enter commands by typing them in the box at the bottom of the screen and hitting enter.", "How to Enter Commands", JOptionPane.OK_OPTION);
						}
					}
					public void keyReleased(KeyEvent ke){}
					public void keyTyped(KeyEvent ke){}
				}
		    );

			// Create and intialize the command panel
			JPanel jpanelCommand = new JPanel();
			JPanel jpanelCommandButtons = new JPanel();

			jpanelCommand.setLayout( new BorderLayout() );
			jpanelCommand.add( jtfCommand, BorderLayout.NORTH );

			// Command
			jtfCommand.addKeyListener(
				new KeyListener(){
					public void keyPressed(KeyEvent ke){
						if( ke.getKeyCode() == ke.VK_ENTER ){
							String sCommand = jtfCommand.getText();
							ApplicationController.getInstance().vCommand(sCommand, null);
							jtfCommand.setText("");
						}
					}
					public void keyReleased(KeyEvent ke){}
					public void keyTyped(KeyEvent ke){}
				}
		    );

			// Show Log
			JButton jbuttonShowLog = new JButton("Show Log");
			jbuttonShowLog.addActionListener(
				new ActionListener(){
				    public void actionPerformed(ActionEvent event) {
					    Panel_View_Text.this.vShowLog();
					}
				}
			);

			// Show Errors
			JButton jbuttonShowErrors = new JButton("Show Errors");
			jbuttonShowErrors.addActionListener(
				new ActionListener(){
				    public void actionPerformed(ActionEvent event) {
					    Panel_View_Text.this.vShowErrors();
					}
				}
			);

			// Clear Display
			JButton jbuttonClearDisplay = new JButton("Clear Display");
			jbuttonClearDisplay.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					Panel_View_Text.this.vClearDisplay();
					}
				}
			);

			// Clear History
			JButton jbuttonClearHistory = new JButton("Clear History");
			jbuttonClearHistory.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					Panel_View_Text.this.vClearHistory();
					}
				}
			);

			jpanelCommandButtons.add( jbuttonShowLog );
			jpanelCommandButtons.add( jbuttonShowErrors );
			jpanelCommandButtons.add( jbuttonClearDisplay );
			jpanelCommandButtons.add( jbuttonClearHistory );

			jpanelCommand.add( jpanelCommandButtons, BorderLayout.SOUTH );

			this.add(jpanelCommand, BorderLayout.SOUTH);

            return true;

        } catch(Exception ex){
            sbError.insert(0, "Unexpected error: " + ex);
            return false;
        }
	}

	void vClearDisplay(){
		jtaDisplay.setText("");
	}

	void vClearHistory(){
		ApplicationController.getInstance().vClearHistory();
	}

	void vDisplayAppendLine(String sTextToAppend){
		if( sTextToAppend.endsWith(CommandListener.FORMATTING_ResponseTermination) ){
			sTextToAppend = sTextToAppend.substring(0, sTextToAppend.length()-CommandListener.FORMATTING_ResponseTermination.length());
		}
		jtaDisplay.append(sTextToAppend + "\r\n");
		try { // scroll to end of display
			jspDisplay.scrollRectToVisible(
					new java.awt.Rectangle(0,
					jtaDisplay.getLineEndOffset(
					jtaDisplay.getLineCount()), 1,1));
			} catch(Exception ex) {}
	}

	OutputStream getOutputStream(){
		return new ViewFilter(null);
	}

	void vShowMessages(){
		ApplicationController.getInstance().vDumpMessages();
	}

	void vShowLog(){
		ApplicationController.getInstance().vDumpLog();
	}

	void vShowErrors(){
		ApplicationController.getInstance().vDumpErrors();
	}

	class ViewFilter extends FilterOutputStream {
		boolean zMaxedOut = false;
		int MAX_ViewCharacters = 160000;
		final static int MIN_BUFFER_LENGTH = 10000;
		byte[] mabBuffer = new byte[MIN_BUFFER_LENGTH];
		int mlenBuffer = MIN_BUFFER_LENGTH;
		int mlenData = 0;
		public ViewFilter(OutputStream os){
			super(os);
			MAX_ViewCharacters = ConfigurationManager.getInstance().getProperty_MaxViewCharacters();
		}
		public void write(byte[] buffer) throws IOException {
			try {
				if( zMaxedOut ) throw new IOException("screen buffer size exceeded");
				while( mlenData + buffer.length > mlenBuffer ){
					Thread.yield();
					mlenBuffer *= 2;
					byte[] abNewBuffer = new byte[mlenBuffer];
					System.arraycopy(mabBuffer, 0, abNewBuffer, 0, mlenData);
					mabBuffer = abNewBuffer;
				}
				synchronized(mabBuffer){
					System.arraycopy(buffer, 0, mabBuffer, mlenData, buffer.length);
					mlenData += buffer.length;
				}
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						vAppendToScreen();
					}
				});
			} catch( Throwable throwable ) {
				mabBuffer =  null;
				throw new IOException("error: " + throwable.toString());
			}
		}
		public void write(final byte[] buffer, final int offset, final int count) throws IOException {
			try {
				if( zMaxedOut ) throw new IOException("screen buffer size exceeded on offset write");
				while( mlenData + count > mlenBuffer ){
					Thread.yield();
					mlenBuffer *= 2;
					byte[] abNewBuffer = new byte[mlenBuffer];
					System.arraycopy(mabBuffer, 0, abNewBuffer, 0, mlenData);
					mabBuffer = abNewBuffer;
				}
				synchronized(mabBuffer){
					System.arraycopy(buffer, offset, mabBuffer, mlenData, count);
					mlenData += count;
				}
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						vAppendToScreen();
					}
				});
			} catch( Throwable throwable ) {
				mabBuffer =  null;
				throw new IOException("error on offset write: " + throwable.toString());
			}
		}
		public void write(int iByte) throws IOException {
			try {
				if( zMaxedOut ) throw new IOException("screen buffer size exceeded on offset write");
				if( mlenData == mlenBuffer ){
					Thread.yield();
					mlenBuffer *= 2;
					byte[] abNewBuffer = new byte[mlenBuffer];
					System.arraycopy(mabBuffer, 0, abNewBuffer, 0, mlenData);
					mabBuffer = abNewBuffer;
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							vAppendToScreen();
						}
					});
				}
				synchronized(mabBuffer){
					mlenData ++;
					mabBuffer[mlenData-1] = (byte)iByte;
				}
			} catch( Throwable throwable ) {
				mabBuffer =  null;
				throw new IOException("error on offset write: " + throwable.toString());
			}
		}

		// should only be run on the GUI thread
		private void vAppendToScreen(){
			if (jtaDisplay.getText().length() > MAX_ViewCharacters) {
				ApplicationController.vShowStatus("View panel limit of " + MAX_ViewCharacters +
					" characters exceeded. Remaining output omitted.");
				zMaxedOut = true;
				try { close(); } catch(Exception ex){}
			}
			synchronized(mabBuffer){
				String sToAppend = new String(mabBuffer, 0, mlenData);
				mlenData = 0;
				jtaDisplay.append(sToAppend);
			}
			jtaDisplay.setCaretPosition( jtaDisplay.getDocument().getLength() );
		}
		public void flush(){
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					vAppendToScreen();
				}
			});
			Thread.yield();
		}
	}

}



