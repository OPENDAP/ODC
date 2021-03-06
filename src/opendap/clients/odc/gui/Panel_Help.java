package opendap.clients.odc.gui;

/**
 * Title:        Panel_Help
 * Description:  Displays the help screens
 * Copyright:    Copyright (c) 2002-2012
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.08
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
import javax.swing.*;
import javax.swing.border.*;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.IControlPanel;
import opendap.clients.odc.Utility;

public class Panel_Help extends JPanel implements IControlPanel {

    public Panel_Help() {}

	String[] asTopics = new String[0];
	String[] asEntries = new String[0];
	JTextArea mjtaDisplay = new JTextArea("");
	JList mjlistTopics = null;

    boolean zInitialize( StringBuffer sbError ){

        try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			this.setBorder(borderStandard);

			this.setLayout(new java.awt.BorderLayout());

			if( !zLoadHelp(sbError) ){
				this.add( new JLabel("Failed to load help: " + sbError), java.awt.BorderLayout.CENTER);
				return false;
			}

			// Create and intialize the text area
			JScrollPane jspDisplay = new JScrollPane();
			mjtaDisplay.setLineWrap(true);
			mjtaDisplay.setWrapStyleWord(true);
			mjtaDisplay.setBorder(new EmptyBorder(10, 10, 10, 10));
			jspDisplay.setViewportView(mjtaDisplay);
		    this.add(jspDisplay, java.awt.BorderLayout.CENTER);

			mjlistTopics = new JList();
			mjlistTopics.setListData(asTopics);
			mjlistTopics.addMouseListener(
				new MouseAdapter(){
					public void mousePressed( MouseEvent me ){
						if( me.getClickCount() == 1 ){
							JList list = (JList)me.getSource();
							int x0Item = list.locationToIndex(me.getPoint());
							if( x0Item > -1 ){
								Panel_Help.this.vAction_LoadHelpTopic( x0Item );
							}
						}
					}
				}
			);
			JScrollPane jspTopics = new JScrollPane();
			jspTopics.setViewportView(mjlistTopics);
			this.add(jspTopics, java.awt.BorderLayout.WEST);

			// show the first topic
			if( asTopics.length > 0 ){
				mjlistTopics.setSelectedIndex(0);
				Panel_Help.this.vAction_LoadHelpTopic( 0 );
			}

            return true;

        } catch( Throwable t ){
        	ApplicationController.vUnexpectedError( t, "initializing help" );
            return false;
        }
	}

	public void _vSetFocus(){    	
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				mjtaDisplay.requestFocus();
			}
		});
	}
    
	void vAction_LoadHelpTopic( int x0Item ){
		if( x0Item < 0 || x0Item >= asTopics.length ){
			mjtaDisplay.setText("[internal error, help topic " + x0Item + " out of range]");
		} else {
			mjtaDisplay.setText(asTopics[x0Item] + "\n" + asEntries[x0Item]);
		}
	}

	boolean zLoadHelp( StringBuffer sbError ){
		StringBuffer sbContent = new StringBuffer( 10000 );
		if( Utility.zLoadStringResource(Resources.pathHelpText, sbContent, sbError) ){
			// help successfully loaded
		} else {
			sbError.insert(0, "Failed to find help file [" + Resources.pathHelpText + "]: ");
			return false;
		}
		int ctTopic = 0;
		int pos = 0;
		int eState = 1; // before entry
		int eLastState = 0;
		int posLast = 0;
		StringBuffer sb = new StringBuffer(50);
		int lenBuffer = sbContent.length();

		// count topics
		while(true){
			if( eState == eLastState && pos == posLast ){
				sbError.append("circular scan error while counting help topics at " + pos + " : " + eState);
				return false;
			} else {
				eLastState = eState;
				posLast = pos;
			}
			char c = sbContent.charAt(pos);
			switch(eState){
				case 1: // beginning of line
					if( c == '#' ) eState = 2; else eState = 4;
					pos++;
					break;
				case 2: // after # at beginning of line
					if( c == '~' ){
						eState = 3; 
					} else {
						eState = 4;
					}
					pos++;
					break;
				case 3: // in topic line
					if( c == '\n' || c == '\r' ){
						ctTopic++;
						eState = 5;
					}
					pos++;
					break;
				case 4: // in normal line
					if( c == '\n' || c == '\r' ) eState = 5;
					pos++;
					break;
				case 5: // in line terminator
					if( c == '\n' || c == '\r' ) pos++; else eState = 1;
					break;
			}
			if( pos >= lenBuffer ) break;
		}

		// size arrays
		asTopics = new String[ctTopic];
		asEntries = new String[ctTopic];
		StringBuffer sbEntry = new StringBuffer(1000);
	    int xTopic = 0;
		pos = 0;
		eState = 1; // before entry
		eLastState = 0;
		posLast = 0;
		while(true){
			if( eState == eLastState && pos == posLast ){
				sbError.append("scan error at " + pos + " : " + eState);
				return false;
			} else {
				eLastState = eState;
				posLast = pos;
			}
			char c = sbContent.charAt(pos);
			switch(eState){
				case 1: // beginning of line
					switch( c ){
						case '#':
						    eState = 2;
							break;
						case '\n':
							eState = 5;
							break;
						case '\r':
							eState = 6;
							break;
						default:
							sbEntry.append(c);
							eState = 4;
					}
					pos++;
					break;
				case 2: // beginning of line, after #
					switch( c ){
						case '~':
						    eState = 3;
							break;
						case '\n':
							eState = 5;
							break;
						case '\r':
							eState = 6;
							break;
						default:
							sbEntry.append(c);
							eState = 4;
					}
					pos++;
					break;
				case 3: // in topic line
					if( c == '\n' || c == '\r' ){
						if( xTopic > 0 ){ // store entry for previous topic
							if( xTopic == 1 ){ // always add version header to first topic
								String sHeader = "\n" + ApplicationController.getInstance().getAppName() + "\n" +
												    "Version " + ApplicationController.getInstance().getAppVersion() + "\n" +
													ApplicationController.getInstance().getAppReleaseDate() + "\n\n";
								sbEntry.insert(0, sHeader);
							}
							asEntries[xTopic-1] = sbEntry.toString();
							sbEntry.setLength(0);
						}
						String sTopic = sb.toString();
						sb.setLength(0);
						if( sTopic.length() == 0 ) sTopic = "*";
						asTopics[xTopic] = sTopic;
						xTopic++;
						if( c == '\n' ) eState = 5; else eState = 6;
					} else sb.append(c);
					pos++;
					break;
				case 4: // in normal line
					if( c == '\n' ){
					    eState = 4;
					} else if( c == '\r' ){
					    eState = 6;
					} else {
						sbEntry.append(c);
					}
					pos++;
					break;
				case 5: // after new line
					if( c == '\n' ){
						sbEntry.append('\n');
						sbEntry.append('\n');
						pos++;
					} else if( c == '\r' ){
						sbEntry.append('\n');
						pos++;
					}
					eState = 1;
					break;
				case 6: // after carriage return
					if( c == '\n' ){
						sbEntry.append('\n');
						pos++;
					} else if( c == '\r' ){
						sbEntry.append('\n');
						sbEntry.append('\n');
						pos++;
					}
					eState = 1;
					break;
			}
			if( pos >= lenBuffer ) break;
		}
		if( xTopic > 0 ){ // store entries for previous topic
			asEntries[xTopic-1] = sbEntry.toString();
		}
		return true;
	}

}


