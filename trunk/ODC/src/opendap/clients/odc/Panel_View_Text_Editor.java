package opendap.clients.odc;

/**
 * Title:        Panel_View_Text_Editor
 * Description:  Text editor window
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
import javax.swing.text.*;
import java.io.*;

public class Panel_View_Text_Editor extends JPanel {

	private String msFileName = null;
	private String msFileDirectory = null;
	private boolean mzDirty = false;
	
	public Panel_View_Text_Editor() {}

	JScrollPane jspDisplay = new JScrollPane();
	private final JTextArea jtaDisplay = new JTextArea("");

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
					public void keyPressed(KeyEvent ke){}
					public void keyReleased(KeyEvent ke){}
					public void keyTyped(KeyEvent ke){
						mzDirty = true;
						ke.getModifiers();
						if( ke.getKeyCode() == java.awt.event.KeyEvent.VK_S ){
							int iModifiers = ke.getModifiersEx();
							if( (iModifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) == java.awt.event.InputEvent.CTRL_DOWN_MASK ){
								save();
								ke.consume();
							}
						}
					}
				}
		    );

            return true;

		} catch(Exception ex){
            sbError.insert(0, "Unexpected error: " + ex);
            return false;
		}
	}

    /** file name only, example: "image_processing.py" */
	public String getFileName(){ return msFileName; }
    
    /** returns the directory for the file with the terminating separator usually
     *  example: "c:\odc\scripts\" */
	public String getFileDirectory(){ return msFileDirectory; }
    
    /** set file name only, example: "image_processing.py" */
	public void setFileName( String sNewName ){ msFileName = sNewName; }
    
    /** sets the directory for the file (including terminating separator is optional)
     *  example: "c:\odc\scripts\" */
	public void setFileDirectory( String sNewDirectory ){ msFileDirectory = sNewDirectory; }
    
    /** indicates that the file may not be saved / may be changed */
	public boolean getDirty(){ return mzDirty; }
    
	void save(){
		String sDirectory = getFileDirectory();
		String sFileName  = getFileName();
		if( sDirectory == null || sFileName == null ){
			saveAs();
		} else {
			try {
				StringBuffer sbError = new StringBuffer();
				File file = Utility.fileDefine( sDirectory, sFileName, sbError);
				if( file == null ){					
					ApplicationController.vShowWarning( "error defining file (dir: " + sDirectory + " name: " + sFileName + "): " );
					saveAs();
				} else {
					if( Utility.fileSave( file, jtaDisplay.getText(), sbError ) ){
						ApplicationController.vShowStatus( "Saved " + file );
					} else {
						ApplicationController.vShowError( "Error saving file [" + file + "]: " + sbError );
					}
				}
			} catch( Throwable t ) {
				ApplicationController.vUnexpectedError( t, "while saving file" );
			}
		}
	}

	void saveAs(){
		StringBuffer sbError = new StringBuffer();
		String sSuggestedDirectory = getFileDirectory();
		if( sSuggestedDirectory == null ) sSuggestedDirectory = ConfigurationManager.getInstance().getProperty_DIR_Scripts();
		File fileSaved = Utility.fileSaveAs( sSuggestedDirectory, getFileName(), jtaDisplay.getText(), sbError );
		if( fileSaved == null ){
			if( sbError.length() == 0 ){
				ApplicationController.vShowStatus_NoCache( "save cancelled" );
			} else { // an error occurred
				ApplicationController.vShowError( "Error saving file: " +  sbError );
			}
			return;
		}

		// remember this directory as the new text file (scripts) directory
		File fileNewScriptsDirectory = fileSaved.getParentFile();
		if( fileNewScriptsDirectory != null ){
			try {
				String sNewScriptsDirectory = fileNewScriptsDirectory.getCanonicalPath();
				ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_DIR_Scripts, sNewScriptsDirectory );
			} catch( Throwable t ) {
				ApplicationController.vShowWarning( "failed to determine canonical path for [" + fileNewScriptsDirectory + "]: " + t );
			}
		}

		this.mzDirty = false;
		ApplicationController.vShowStatus( "Saved file as " + fileSaved );

    }
    
}



