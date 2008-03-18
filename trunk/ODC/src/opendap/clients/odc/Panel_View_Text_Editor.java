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
import javax.swing.*;

import java.io.*;

public class Panel_View_Text_Editor extends JPanel implements IControlPanel {

	private String msFileName = null;
	private String msFileDirectory = null;
	private boolean mzDirty = false;
	private Panel_View_Text parent = null;
	
	public Panel_View_Text_Editor(){}

	private JScrollPane jspDisplay = new JScrollPane();
	private final JTextArea jtaDisplay = new JTextArea("");

	boolean _zInitialize( Panel_View_Text parent, String sDirectory, String sName, String sContent, StringBuffer sbError ){

		try {
			if( parent == null ){
				sbError.append("parent missing");
				return false;
			}
			this.parent = parent;
			msFileDirectory = sDirectory;
			msFileName = sName;

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

			// Add special warning message to display jta
			jtaDisplay.addKeyListener(
				new KeyListener(){
					public void keyPressed( KeyEvent ke ){
						int iKeyCode = ke.getKeyCode();
						int iModifiers = ke.getModifiersEx();
						switch( iKeyCode ){
							case KeyEvent.VK_S:
								if( (iModifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) == java.awt.event.InputEvent.CTRL_DOWN_MASK ){
									if( (iModifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) == java.awt.event.InputEvent.SHIFT_DOWN_MASK ){
										_saveAs();
									} else {
										_save();
									}
									ke.consume();
								} 
								break;
							case KeyEvent.VK_N:
								if( (iModifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) == java.awt.event.InputEvent.CTRL_DOWN_MASK ){
									Panel_View_Text_Editor.this.parent.editorNew();
									ke.consume();
								}
								break;
							case KeyEvent.VK_O:
								if( (iModifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) == java.awt.event.InputEvent.CTRL_DOWN_MASK ){
									Panel_View_Text_Editor.this.parent.editorOpen();
									ke.consume();
								}
								break;
							case KeyEvent.VK_X:
								if( (iModifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) == java.awt.event.InputEvent.CTRL_DOWN_MASK ){
									if( (iModifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) == java.awt.event.InputEvent.SHIFT_DOWN_MASK ){
										Panel_View_Text_Editor.this.parent.editorCloseNoSave();
										ke.consume();
									} else if( (iModifiers & java.awt.event.InputEvent.ALT_DOWN_MASK) == java.awt.event.InputEvent.ALT_DOWN_MASK ){
										_save();
										Panel_View_Text_Editor.this.parent.editorCloseNoSave();
										ke.consume();
									}
								}
								break;
						}
						if( ! _isDirty() ) Panel_View_Text_Editor.this.parent.updateTabTitles();						
					}
					public void keyReleased(KeyEvent ke){}
					public void keyTyped(KeyEvent ke){}
				}
		    );

			if( sContent != null ) jtaDisplay.setText( sContent );
			
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

	public String _getText(){ return jtaDisplay.getText(); }
	
    /** file name only, example: "image_processing.py" */
	public String _getFileName(){ return msFileName; }

    /** returns the directory for the file with the terminating separator usually
     *  example: "c:\odc\scripts\" */
	public String _getFileDirectory(){ return msFileDirectory; }

    /** set file name only, example: "image_processing.py" */
	public void _setFileName( String sNewName ){ msFileName = sNewName; }

    /** sets the directory for the file (including terminating separator is optional)
     *  example: "c:\odc\scripts\" */
	public void _setFileDirectory( String sNewDirectory ){ msFileDirectory = sNewDirectory; }
    
    /** indicates that the file may not be saved / may be changed */
	public boolean _isDirty(){ return mzDirty; }
	
	public void _setClean(){
	}
    
	/** returns false if the action was cancelled or failed */
	boolean _save(){
		String sDirectory = _getFileDirectory();
		String sFileName  = _getFileName();
		if( sDirectory == null || sFileName == null ){
			return _saveAs();
		} else {
			try {
				StringBuffer sbError = new StringBuffer();
				File file = Utility.fileDefine( sDirectory, sFileName, sbError);
				if( file == null ){					
					ApplicationController.vShowWarning( "error defining file (dir: " + sDirectory + " name: " + sFileName + "): " );
					return _saveAs();
				} else {
					if( Utility.fileSave( file, jtaDisplay.getText(), sbError ) ){
						ApplicationController.vShowStatus( "Saved " + file );
						_setClean();
						return true;
					} else {
						ApplicationController.vShowError( "Error saving file [" + file + "]: " + sbError );
						return false;
					}
				}
			} catch( Throwable t ) {
				ApplicationController.vUnexpectedError( t, "while saving file" );
				return false;
			}
		}
	}

	/** returns false if the action was cancelled or failed */
	boolean _saveAs(){
		StringBuffer sbError = new StringBuffer();
		String sSuggestedDirectory = _getFileDirectory();
		if( sSuggestedDirectory == null ) sSuggestedDirectory = ConfigurationManager.getInstance().getProperty_DIR_Scripts();
		File fileSaved = Utility.fileSaveAs( ApplicationController.getInstance().getAppFrame(), "Save As...", sSuggestedDirectory, _getFileName(), jtaDisplay.getText(), sbError );
		if( fileSaved == null ){
			if( sbError.length() == 0 ){
				ApplicationController.vShowStatus_NoCache( "save cancelled" );
			} else { // an error occurred
				ApplicationController.vShowError( "Error saving file: " +  sbError );
			}
			return false;
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
		_setClean();
		parent.updateTabTitles(); // update the name of the tab
		return true;

    }
	
    
}



