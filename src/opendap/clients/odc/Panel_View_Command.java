package opendap.clients.odc;

/**
 * Title:        Panel_View_Command
 * Description:  Output text area which displays messages and data and allows command inputs
 * Copyright:    Copyright (c) 2008
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.00
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

import java.io.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class Panel_View_Command extends JPanel implements IControlPanel {

	public Panel_View_Command() {}

	JScrollPane jspDisplay = new JScrollPane();
	private final JTextArea jtaDisplay = new JTextArea("");
	private final JTextField jtfCommand = new JTextField();
	ArrayList<String> listCommands = new ArrayList<String>();
	private int mposBeginningOfLine = 0;
	private int mCommandOnLine_1 = 0; // if user has used up arrow to insert command this will be incremented 

	boolean zInitialize(StringBuffer sbError){

		try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			this.setBorder(borderStandard);

			this.setLayout(new java.awt.BorderLayout());

			// Create and intialize the text area
			Styles.vApply(Styles.STYLE_Terminal, jtaDisplay);
			jtaDisplay.setColumns( ConfigurationManager.getInstance().getProperty_Editing_ColumnCount() );
			jtaDisplay.setLineWrap( ConfigurationManager.getInstance().getProperty_Editing_LineWrap() );
			jtaDisplay.setWrapStyleWord( ConfigurationManager.getInstance().getProperty_Editing_WrapByWords() );
			jtaDisplay.setTabSize( ConfigurationManager.getInstance().getProperty_Editing_TabSize() );
			jspDisplay.setViewportView(jtaDisplay);
		    this.add(jspDisplay, java.awt.BorderLayout.CENTER);

			InputMap theInputMap = jtaDisplay.getInputMap();
		    KeyStroke keystrokeCtrlEnter = KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, Event.CTRL_MASK );
		    theInputMap.put( keystrokeCtrlEnter, javax.swing.text.DefaultEditorKit.insertBreakAction );

//make new execution isolater with rule that every time prompt is output beginning of line is set
//keep a list of all beginning of lines so that
//if the person hits enter BEFORE that position then use that line
//each command should be stored separately in a list

		    // Add special warning message to display jta
			jtaDisplay.addKeyListener(
				new KeyListener(){
					public void keyTyped( KeyEvent ke ){}
					public void keyReleased(KeyEvent ke){}
					public void keyPressed(KeyEvent ke){
						int iKeyCode = ke.getKeyCode();
						if( iKeyCode == KeyEvent.VK_ENTER ){
							if( ke.isControlDown() ) return; // treat ctrl+enter as literal carriage return
							
							// JOptionPane.showMessageDialog(Panel_View_Text.this, "Enter commands by typing them in the box at the bottom of the screen and hitting enter.", "How to Enter Commands", JOptionPane.OK_OPTION);
							String sDisplayText = jtaDisplay.getText();
							int posEndOfLine = sDisplayText.length();
							String sLine = sDisplayText.substring( mposBeginningOfLine, posEndOfLine );
							System.out.println("command [" + sLine + "]");
//							JOptionPane.showMessageDialog(Panel_View_Text.this, "line is: [" + sLine + "] posb: " + posBeginningOfLine, "How to Enter Commands", JOptionPane.OK_OPTION);
							listCommands.add( sLine );
							ApplicationController.getInstance().vCommand( sLine );
							mCommandOnLine_1 = 0;
							ke.consume();
						} else 
						if( iKeyCode == KeyEvent.VK_UP ){
							final int ctCommands = listCommands.size();
							if( ctCommands > 0 ){
								javax.swing.SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										try {
											int iDocumentLength = jtaDisplay.getDocument().getLength();
											if( iDocumentLength > mposBeginningOfLine ) jtaDisplay.getDocument().remove( mposBeginningOfLine, iDocumentLength - mposBeginningOfLine );
										} catch( Throwable t ) {}
										mCommandOnLine_1--;
										if( mCommandOnLine_1 < 1 ) mCommandOnLine_1 = ctCommands; 
										jtaDisplay.append( listCommands.get(mCommandOnLine_1 - 1) );
									}
								});
							}
							ke.consume();
						}
						else if( iKeyCode == KeyEvent.VK_DOWN ){
							final int ctCommands = listCommands.size();
							if( ctCommands > 0 ){
								javax.swing.SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										try {
											int iDocumentLength = jtaDisplay.getDocument().getLength();
											if( iDocumentLength > mposBeginningOfLine ) jtaDisplay.getDocument().remove( mposBeginningOfLine, iDocumentLength - mposBeginningOfLine );
										} catch( Throwable t ) {}
										mCommandOnLine_1++;
										if( mCommandOnLine_1 > ctCommands ) mCommandOnLine_1 = 1; 
										jtaDisplay.append( listCommands.get(mCommandOnLine_1 - 1) );
									}
								});
							}
							ke.consume();
						}
					}
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
							ApplicationController.getInstance().vCommand( sCommand, null );
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
					    Panel_View_Command.this.vShowLog();
					}
				}
			);

			// Show Errors
			JButton jbuttonShowErrors = new JButton("Show Errors");
			jbuttonShowErrors.addActionListener(
				new ActionListener(){
				    public void actionPerformed(ActionEvent event) {
					    Panel_View_Command.this.vShowErrors();
					}
				}
			);

			// Clear Display
			JButton jbuttonClearDisplay = new JButton("Clear Display");
			jbuttonClearDisplay.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					Panel_View_Command.this.vClearDisplay();
					}
				}
			);

			// Clear History
			JButton jbuttonClearHistory = new JButton("Clear History");
			jbuttonClearHistory.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					Panel_View_Command.this.vClearHistory();
					}
				}
			);

			jpanelCommandButtons.add( jbuttonShowLog );
			jpanelCommandButtons.add( jbuttonShowErrors );
			jpanelCommandButtons.add( jbuttonClearDisplay );
			jpanelCommandButtons.add( jbuttonClearHistory );

			jpanelCommand.add( jpanelCommandButtons, BorderLayout.SOUTH );

			this.add( jpanelCommand, BorderLayout.SOUTH );

            return true;

        } catch(Exception ex){
            sbError.insert(0, "Unexpected error: " + ex);
            return false;
        }
	}

	public void vSetFocus(){    	
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				jtaDisplay.requestFocus();
			}
		});
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
		int MAX_ViewCharacters = 500000;
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
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (jtaDisplay.getText().length() > MAX_ViewCharacters) {
						ApplicationController.vShowStatus("View panel limit of " + MAX_ViewCharacters +
							" characters exceeded. Remaining output omitted.");
						zMaxedOut = true;
						try { close(); } catch(Exception ex){}
					}
					synchronized(mabBuffer){
						String sToAppend = new String(mabBuffer, 0, mlenData);
						mlenData = 0;
						mposBeginningOfLine = jtaDisplay.getText().length() + sToAppend.length();
						jtaDisplay.append(sToAppend);
					}
				}
			});
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					int posEndOfDocument = jtaDisplay.getDocument().getLength();
					jtaDisplay.setCaretPosition( posEndOfDocument );
//					int iOldPos = mposBeginningOfLine;
//					mposBeginningOfLine = posEndOfDocument;
//					System.out.println("old eol: " + iOldPos + " new : " + mposBeginningOfLine + " diff: " + (mposBeginningOfLine - iOldPos)); 
				}
			});
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



