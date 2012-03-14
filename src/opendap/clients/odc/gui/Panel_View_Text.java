package opendap.clients.odc.gui;

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
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.IControlPanel;
import opendap.clients.odc.SavableImplementation;
import opendap.clients.odc.TabbedPane_Focusable;

public class Panel_View_Text extends JPanel implements IControlPanel {

	private int mctFilesOpened; // used to generate default file name of new window
	public ArrayList<Panel_View_Text_Editor> listEditors = null;
	
    public Panel_View_Text() {}

	private final TabbedPane_Focusable jtpEditors = new TabbedPane_Focusable();
//	private final ArrayList<Panel_View_Text_Editor> listEditors = new ArrayList<Panel_View_Text_Editor>();

	boolean zInitialize(StringBuffer sbError){

        try {

			javax.swing.border.Border borderStandard = BorderFactory.createEtchedBorder();
			setBorder(borderStandard);
			setLayout(new java.awt.BorderLayout());

			listEditors =  new ArrayList<Panel_View_Text_Editor>();

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
							if( componentSelected != null ) componentSelected._vSetFocus();
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
					    Panel_View_Text.this.editorNew( null, null );
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
			JButton jbuttonSaveAndClose = new JButton("Save and Close (ctrl+alt+X)");
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

	public void _vSetFocus(){    	
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				IControlPanel componentSelected = (IControlPanel)jtpEditors.getSelectedComponent();
				componentSelected._vSetFocus();				
			}
		});
	}
	
	public void updateTabTitles(){
		for( Component component : jtpEditors.getComponents() ){
			if( ( component == null ) || ! (component instanceof Panel_View_Text_Editor) ) continue; 			
			Panel_View_Text_Editor editor = (Panel_View_Text_Editor)component;	
			String sNewName = editor.savableString._getFileName() + (editor._isDirty() ? '*' : "");
			jtpEditors.setTitleAt( jtpEditors.getSelectedIndex(), sNewName );
		}
	}
	
	public void editorNew(){
		editorNew( null, null );
	}

	public void editorNew( SavableImplementation savable, String sContent ){
		try {
			String sDirectory = savable == null ? null : savable._getFileDirectory();
			String sName = savable == null ? null : savable._getFileName();
			StringBuffer sbError = new StringBuffer(250);
			mctFilesOpened++;
			if( sName == null ) sName = "" + mctFilesOpened + ".txt";
			if( sDirectory == null ) sDirectory = ConfigurationManager.getInstance().getDefault_DIR_Scripts();
			Panel_View_Text_Editor editorNew = Panel_View_Text_Editor._create( this, sDirectory, sName, sContent, sbError );
			if( editorNew == null ){
				ApplicationController.vShowError( "Error creating new editor window for directory: " + sDirectory + " file: " + sName + " " + sContent.length() + " bytes: " + sbError );
				return;
			}
			jtpEditors.addTab( sName, editorNew );
			jtpEditors.setSelectedComponent( editorNew );
			listEditors.add( editorNew );
			if( listEditors.size() > 1 ){
				for( Panel_View_Text_Editor editor : listEditors ){
					if( editor == editorNew ) continue;
					if( editor._getText().length() == 0 ){
						editorCloseNoSave( editor );
					}
				}
			}
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while opening new editor" );
		}
	}

	void editorOpen(){
		try {
			SavableImplementation savable = new SavableImplementation( java.lang.String.class, null, null );
			Object oSavable = savable._open();
			if( oSavable == null ) return; // user cancelled
			if( ! (oSavable instanceof opendap.clients.odc.data.Model_Dataset) ){
				ApplicationController.vShowError( "File could not be interpreted in a way that is loadable in the editor. May be an unrecognized binary type." );
				return;
			}
			opendap.clients.odc.data.Model_Dataset model = (opendap.clients.odc.data.Model_Dataset)oSavable;
			String sContent = model.getTextContent();
			editorNew( savable, sContent );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while opening editor file" );
		}
	}
	
	void editorSave(){
		try {
			Panel_View_Text_Editor editor = (Panel_View_Text_Editor)jtpEditors.getSelectedComponent();
			editor._save();
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while saving editor file" );
		}
	}
	
	void editorSaveAs(){
		try {
			Panel_View_Text_Editor editor = (Panel_View_Text_Editor)jtpEditors.getSelectedComponent();
			editor._saveAs();		
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while saving-as editor file" );
		}
	}
	void editorSaveClose(){
		try {
			Panel_View_Text_Editor editor = (Panel_View_Text_Editor)jtpEditors.getSelectedComponent();
			if( editor == null ){
				ApplicationController.vShowStatus_NoCache( "no active editor to save/close" );
				return;
			}
			if( ! editor._save() ) return; // do not close if action was cancelled or failed
			jtpEditors.remove( editor );
			listEditors.remove( editor );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while saving and closing editor file" );
		}
	}
	void editorCloseNoSave(){
		try {
			Panel_View_Text_Editor editor = (Panel_View_Text_Editor)jtpEditors.getSelectedComponent();
			editorCloseNoSave( editor );
		} catch( Throwable t ) {
			ApplicationController.vUnexpectedError( t, "while closing without saving editor file" );
		}
	}
	void editorCloseNoSave( Panel_View_Text_Editor editor ){
		if( editor == null ){
			ApplicationController.vShowStatus_NoCache( "no active editor to close" );
			return;
		}
		jtpEditors.remove( editor );
		listEditors.remove( editor );
	}

}



