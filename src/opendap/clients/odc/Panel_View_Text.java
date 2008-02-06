package opendap.clients.odc;

/**
 * Title:        Panel_View_Text
 * Description:  Text editor with multiple tabs
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
import java.io.File;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Panel_View_Text extends JPanel implements IControlPanel {

	private int mctFilesOpened; // used to generate default file name of new window
	
    public Panel_View_Text() {}

	private final TabbedPane_Focusable jtpEditors = new TabbedPane_Focusable();
//	private final ArrayList<Panel_View_Text_Editor> listEditors = new ArrayList<Panel_View_Text_Editor>();

	boolean zInitialize(StringBuffer sbError){

        try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			setBorder(borderStandard);
			setLayout(new java.awt.BorderLayout());


			// Create and intialize the command panel
			JPanel jpanelCommand = new JPanel();
			JPanel jpanelCommandButtons = new JPanel();
			jpanelCommand.setLayout( new BorderLayout() );
			add( jtpEditors, BorderLayout.CENTER );
			add( jpanelCommand, BorderLayout.SOUTH );

			// focus control for subpanels
			jtpEditors.addChangeListener( new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							IControlPanel componentSelected = (IControlPanel)jtpEditors.getSelectedComponent();
							componentSelected.vSetFocus();
						}
					});
				}
			} );
			
			// Create the default editor
			editorNew();

			// New
			JButton jbuttonNew = new JButton("New (ctrl+N)");
			jbuttonNew.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					    Panel_View_Text.this.editorNew( null, null, null );
					}
				}
			);

			// Open
			JButton jbuttonOpen = new JButton("Open (ctrl+O)");
			jbuttonOpen.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					    Panel_View_Text.this.editorOpen();
					}
				}
			);

			// Save
			JButton jbuttonSave = new JButton("Save (ctrl+S)");
			jbuttonSave.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					    Panel_View_Text.this.editorSave();
					}
				}
			);

			// Save As...
			JButton jbuttonSaveAs = new JButton("Save As... (ctrl+shift+S)");
			jbuttonSaveAs.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					    Panel_View_Text.this.editorSaveAs();
					}
				}
			);

			// Save and Close
			JButton jbuttonSaveAndClose = new JButton("Save and Close (ctrl+X)");
			jbuttonSaveAndClose.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					    Panel_View_Text.this.editorSaveClose();
					}
				}
			);

			// Close without saving
			JButton jbuttonCloseNoSave = new JButton("Close No Save (ctrl+shift+X)");
			jbuttonCloseNoSave.addActionListener(
				new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					    Panel_View_Text.this.editorCloseNoSave();
					}
				}
			);
			
			jpanelCommandButtons.add( jbuttonNew );
			jpanelCommandButtons.add( jbuttonOpen );
			jpanelCommandButtons.add( jbuttonSave );
			jpanelCommandButtons.add( jbuttonSaveAs );
			jpanelCommandButtons.add( jbuttonSaveAndClose );
			jpanelCommandButtons.add( jbuttonCloseNoSave );

			jpanelCommand.add( jpanelCommandButtons, BorderLayout.SOUTH );

            return true;

        } catch(Exception ex){
            sbError.insert(0, "Unexpected error: " + ex);
            return false;
        }
    }

	public void vSetFocus(){    	
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				IControlPanel componentSelected = (IControlPanel)jtpEditors.getSelectedComponent();
				componentSelected.vSetFocus();				
			}
		});
	}
	
	public void updateSelectedTab(){
		Panel_View_Text_Editor editor = (Panel_View_Text_Editor)jtpEditors.getSelectedComponent();
		String sNewName = editor.getFileName() + (editor.isDirty() ? '*' : "");
		jtpEditors.setTitleAt( jtpEditors.getSelectedIndex(), sNewName );
	}
	
	public void editorNew(){
		editorNew( null, null, null );
	}
	
	public void editorNew( String sDirectory, String sName, String sContent ){
		try {
			StringBuffer sbError = new StringBuffer(250);
			mctFilesOpened++;
			if( sName == null ) sName = "" + mctFilesOpened + ".txt";
			if( sDirectory == null ) sDirectory = ConfigurationManager.getInstance().getDefault_DIR_Scripts();
			Panel_View_Text_Editor editor = new Panel_View_Text_Editor();
			if( ! editor.zInitialize( this, sDirectory, sName, sContent, sbError ) ){
				ApplicationController.vShowError( "Error creating new editor window for directory: " + sDirectory + " file: " + sName + " " + sContent.length() + " bytes: " + sbError );
			}
			jtpEditors.addTab( editor.getFileName(), editor );
			jtpEditors.setSelectedComponent( editor );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while opening new editor" );
		}
	}

	javax.swing.JFileChooser mjfcOpen = null;
	void editorOpen(){
		try {
			StringBuffer sbError = new StringBuffer( 250 );

			// ask user for desired location
			if( mjfcOpen == null ){
				mjfcOpen = new javax.swing.JFileChooser();
				String sDirectory = ConfigurationManager.getInstance().getDefault_DIR_Scripts();
				File fileDirectory = Utility.fileEstablishDirectory( sDirectory, sbError );
				if( fileDirectory == null ){
					// no default directory
				} else {
					mjfcOpen.setCurrentDirectory( fileDirectory );
				}
			}
			int iState = mjfcOpen.showDialog( ApplicationController.getInstance().getAppFrame(), "Open Text File" );
			File file = mjfcOpen.getSelectedFile();
			if( file == null || iState != javax.swing.JFileChooser.APPROVE_OPTION || ! file.isFile() ){
				ApplicationController.vShowStatus_NoCache( "file open cancelled" );
				return;
			}
			
			// open the selected file
			String sContent = Utility.fileLoadIntoString( file, sbError);
			if( sContent == null ){
				ApplicationController.vShowError( "Error opening file " + file + ": " + sbError );
				return;
			}
			editorNew(  file.getParent(), file.getName(), sContent );

		} catch(Exception ex) {
			ApplicationController.vUnexpectedError( ex, "while opening file" );
		}
	}
	
	void editorSave(){
		Panel_View_Text_Editor editor = (Panel_View_Text_Editor)jtpEditors.getSelectedComponent();
		editor.save();
	}
	
	void editorSaveAs(){
		Panel_View_Text_Editor editor = (Panel_View_Text_Editor)jtpEditors.getSelectedComponent();
		editor.saveAs();		
	}
	void editorSaveClose(){
		Panel_View_Text_Editor editor = (Panel_View_Text_Editor)jtpEditors.getSelectedComponent();
		if( editor == null ){
			ApplicationController.vShowStatus_NoCache( "no active editor to save/close" );
			return;
		}
		if( ! editor.save() ) return; // do not close if action was cancelled or failed
		jtpEditors.remove( editor );
	}
	void editorCloseNoSave(){
		Panel_View_Text_Editor editor = (Panel_View_Text_Editor)jtpEditors.getSelectedComponent();
		if( editor == null ){
			ApplicationController.vShowStatus_NoCache( "no active editor to close" );
			return;
		}
		jtpEditors.remove( editor );
	}

}



