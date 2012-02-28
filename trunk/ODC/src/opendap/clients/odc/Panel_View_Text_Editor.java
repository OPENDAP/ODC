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

import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.gui.Styles;

public class Panel_View_Text_Editor extends JPanel implements IControlPanel {

	SavableImplementation savableString;
	Model_Dataset model = null;
	
	private Panel_View_Text parent = null;

	private Panel_View_Text_Editor(){}

	private JScrollPane jspDisplay;
	private final JTextArea jtaDisplay = new JTextArea("");

	public static Panel_View_Text_Editor _create( Panel_View_Text parent, String sDirectory, String sName, String sContent, StringBuffer sbError ){
		final Panel_View_Text_Editor editor = new Panel_View_Text_Editor();
		try {
			editor.parent = parent;
			editor.savableString = new SavableImplementation( java.lang.String.class, sDirectory, sName );

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			editor.setBorder(borderStandard);

			editor.setLayout( new java.awt.BorderLayout() );

			// Initialize the scrollpane
			// It is important to set the minimum size of the scroll pane because otherwise the 
			// minimum size will be set automatically its content
			editor.jspDisplay = new JScrollPane();
			editor.jspDisplay.setMinimumSize( new java.awt.Dimension( 20, 20 ) );
			
			// Create and intialize the text area
			Styles.vApply(Styles.STYLE_Terminal, editor.jtaDisplay);
			editor.jtaDisplay.setColumns( ConfigurationManager.getInstance().getProperty_Editing_ColumnCount() );
			editor.jtaDisplay.setLineWrap( ConfigurationManager.getInstance().getProperty_Editing_LineWrap() );
			editor.jtaDisplay.setWrapStyleWord( ConfigurationManager.getInstance().getProperty_Editing_WrapByWords() );
			editor.jtaDisplay.setTabSize( ConfigurationManager.getInstance().getProperty_Editing_TabSize() );
			editor.jspDisplay.setViewportView( editor.jtaDisplay );
		    editor.add( editor.jspDisplay, java.awt.BorderLayout.CENTER);

			// Add special warning message to display jta
			editor.jtaDisplay.addKeyListener(
				new KeyListener(){
					public void keyPressed( KeyEvent ke ){
						int iKeyCode = ke.getKeyCode();
						int iModifiers = ke.getModifiersEx();
						switch( iKeyCode ){
							case KeyEvent.VK_S:
								if( (iModifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) == java.awt.event.InputEvent.CTRL_DOWN_MASK ){
									if( (iModifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) == java.awt.event.InputEvent.SHIFT_DOWN_MASK ){
										editor._saveAs();
									} else {
										editor._save();
									}
									ke.consume();
								} 
								break;
							case KeyEvent.VK_N:
								if( (iModifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) == java.awt.event.InputEvent.CTRL_DOWN_MASK ){
									if( editor.parent != null ) editor.parent.editorNew();
									ke.consume();
								}
								break;
							case KeyEvent.VK_O:
								if( (iModifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) == java.awt.event.InputEvent.CTRL_DOWN_MASK ){
									if( editor.parent != null ) editor.parent.editorOpen();
									ke.consume();
								}
								break;
							case KeyEvent.VK_X:
								if( (iModifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK) == java.awt.event.InputEvent.CTRL_DOWN_MASK ){
									if( (iModifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK) == java.awt.event.InputEvent.SHIFT_DOWN_MASK ){
										if( editor.parent != null ) editor.parent.editorCloseNoSave();
										ke.consume();
									} else if( (iModifiers & java.awt.event.InputEvent.ALT_DOWN_MASK) == java.awt.event.InputEvent.ALT_DOWN_MASK ){
										editor._save();
										if( editor.parent != null ) editor.parent.editorCloseNoSave();
										ke.consume();
									}
								}
								break;
						}
						if( ! editor._isDirty() && ( editor.parent != null ) ) editor.parent.updateTabTitles();						
					}
					public void keyReleased(KeyEvent ke){
						if( editor.model != null ) editor.model.setTextContent( editor._getText() );
					}
					public void keyTyped(KeyEvent ke){}
				}
		    );

			if( sContent != null ) editor.jtaDisplay.setText( sContent );
			
            return editor;

		} catch(Exception ex){
            sbError.insert( 0, "Unexpected error: " + ex );
            return null;
		}
	}

	public void _vSetFocus(){    	
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				jtaDisplay.requestFocus();
			}
		});
	}	

	public void _setModel( Model_Dataset m ){
		model = m;
		if( model != null ){
			jtaDisplay.setText( model.getTextContent() );
			savableString = m.getSavable();
		}
	}
	
	public String _getText(){ return jtaDisplay.getText(); }
	
    /** indicates that the file may not be saved / may be changed */
	public boolean _isDirty(){ return savableString._isDirty(); }
	
	public void _setClean(){
		savableString._makeClean();
	}
    
	/** returns false if the action was cancelled or failed */
	boolean _save(){
		if( model == null ){
			StringBuffer sbError = new StringBuffer();
			model = Model_Dataset.createPlainText( sbError );
		}
		model.setTextContent( _getText() );
		return savableString._save( model );
	}

	/** returns false if the action was cancelled or failed */
	boolean _saveAs(){
		if( model == null ){
			StringBuffer sbError = new StringBuffer();
			model = Model_Dataset.createPlainText( sbError );
		}
		model.setTextContent( _getText() );
		if( savableString._saveAs( model ) ){
			if( Panel_View_Text_Editor.this.parent != null ) parent.updateTabTitles(); // update the name of the tab
			return true;
		} else {
			return false;
		}
    }
	
    
}



