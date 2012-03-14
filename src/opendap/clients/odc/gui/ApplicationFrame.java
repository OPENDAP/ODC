package opendap.clients.odc.gui;

/**
 * Title:        Application Frame
 * Description:  This is the outer container of the GUI
 * Copyright:    Copyright (c) 2002-2012
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.08
 */

/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007-2012 OPeNDAP, Inc.
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

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.IControlPanel;
import opendap.clients.odc.gui.Message;
import opendap.clients.odc.TabbedPane_Focusable;
import opendap.clients.odc.plot.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class ApplicationFrame extends JFrame {

	private ApplicationController mApplicationController;
	private StatusBar mStatusBar;

	private final JTabbedPane jtpMain = new JTabbedPane();
	private final TabbedPane_Focusable jtpSelect = new TabbedPane_Focusable();
	private final TabbedPane_Focusable jtpView   = new TabbedPane_Focusable();
	private final TabbedPane_Focusable jtpFeedback   = new TabbedPane_Focusable();
	private Panel_Select_Favorites panelFavorites;
	private Panel_Select_Recent panelRecent;
	private Panel_Retrieve jpanelRetrieve;
	private Panel_View_Command panelCommand;
	private Panel_View_Text panelTextEditor;
	private Panel_View_Data panelDataView;
	private Panel_View_Table panelTableView;
	private Panel_View_Image panelImageView;
	private Panel_View_Plot panelPlotter;
	private Panel_Help panelHelp;
	private Panel_Feedback_Email panelFeedbackEmail;
	private Panel_Feedback_Bug panelFeedbackBug;

	KeyListener klMaster = new MasterKeyListener();

	public ApplicationFrame() {}

	public boolean zInitialize( String sTitle, ApplicationController appcontroller, StringBuffer sbError ){

	    try {
			ApplicationController.getInstance().vShowStartupMessage("initializing main frame");
			mApplicationController = appcontroller;
			if( mApplicationController == null ){
				sbError.append( "no controller exists" );
				return false;
			}

			this.setTitle(' ' + sTitle);

			Resources.iconAdd(this);

			this.getContentPane().setLayout(new java.awt.BorderLayout());

			// tabs
			ApplicationController.getInstance().vShowStartupMessage( "creating tabs" );
			if( !this.zBuildMainTabs( jtpMain, sbError ) ){
				sbError.insert(0, "failed to create interface: ");
				return false;
			}
			this.add( jtpMain, java.awt.BorderLayout.CENTER );

			// focus control for subpanels
			jtpMain.addChangeListener( new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							IControlPanel componentSelected = (IControlPanel)jtpMain.getSelectedComponent();
							componentSelected._vSetFocus();
						}
					});
				}
			} );

			// focus control for view sub tab
			jtpView.addChangeListener( new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							IControlPanel componentSelected = (IControlPanel)jtpView.getSelectedComponent();
							componentSelected._vSetFocus();
						}
					});
				}
			} );

			// focus control for feedback sub tab
			jtpFeedback.addChangeListener( new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					SwingUtilities.invokeLater( new Runnable() {
						public void run() {
							IControlPanel componentSelected = (IControlPanel)jtpFeedback.getSelectedComponent();
							componentSelected._vSetFocus();
						}
					});
				}
			} );			
			
			// status bar
			ApplicationController.getInstance().vShowStartupMessage("creating status bar");
			mStatusBar = new StatusBar();
			if( !mStatusBar.zInitialize(sbError) ){
				sbError.insert(0, "Failed to initialize status bar: ");
				return false;
			}
			this.add( mStatusBar, java.awt.BorderLayout.SOUTH );

			vSizeDisplay();

			// window closer
			WindowListener listenerCloser = new WindowAdapter(){
				public void windowClosing(WindowEvent e){
					ApplicationController.vForceExit();
				}
			};
			this.addWindowListener( listenerCloser );

			this.addKeyListener( klMaster );
			return true;

		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
    }

	public void vClearStatus(){
		if( mStatusBar != null ) mStatusBar.vClear();
	}

	public void vShowStatus( String sMessage ){
		if( mStatusBar != null ) mStatusBar.vShowStatus(sMessage);
	}

	public void vAppendStatus( String sMessage ){
		if( mStatusBar != null ) mStatusBar.vAppendStatus(sMessage);
	}

	boolean zBuildMainTabs( final JTabbedPane jtpMain, StringBuffer sbError ){

		boolean zReadOnly = ConfigurationManager.getInstance().getProperty_MODE_ReadOnly();

	    // favorites and recent
		panelFavorites = new Panel_Select_Favorites();
		panelRecent = new Panel_Select_Recent();
		if( zReadOnly ){
			// do not add these panels
		} else {
			ApplicationController.getInstance().vShowStartupMessage("creating favorites panel");
			if( !panelFavorites.zInitialize(sbError) ){
				sbError.insert(0, "Failed to initialize favorites panel: ");
				return false;
			}
			ApplicationController.getInstance().vShowStartupMessage("creating recent panel");
			if( !panelRecent.zInitialize(sbError) ){
				sbError.insert(0, "Failed to initialize recent panel: ");
				return false;
			}
		}

		ApplicationController.getInstance().vShowStartupMessage("creating command shell");
		panelCommand = new Panel_View_Command();
		if( ! panelCommand.zInitialize(sbError) ){
			sbError.insert(0, "Failed to initialize command shell window: ");
			return false;
		}
		ApplicationController.getInstance().vShowStartupMessage("creating text editor");
		panelTextEditor = new Panel_View_Text();
		if( ! panelTextEditor.zInitialize(sbError) ){
			sbError.insert(0, "Failed to initialize text editor panel: ");
			return false;
		}
		ApplicationController.getInstance().vShowStartupMessage("creating data viewer");
		panelDataView = new Panel_View_Data();
		if( !panelDataView._zInitialize( ApplicationController.getInstance().getDatasets(), sbError ) ){
			sbError.insert(0, "Failed to initialize data view panel: ");
			return false;
		}
		ApplicationController.getInstance().vShowStartupMessage("creating table viewer");
		panelTableView = new Panel_View_Table();
		if( !panelTableView.zInitialize( sbError ) ){
			sbError.insert(0, "Failed to initialize table view panel: ");
			return false;
		}
		ApplicationController.getInstance().vShowStartupMessage("creating image viewer");
		panelImageView = new Panel_View_Image();
		if( !panelImageView.zInitialize(sbError) ){
			ApplicationController.vShowWarning("Failed to initialize image view panel: " + sbError);
			sbError.setLength(0);
		}
		ApplicationController.getInstance().vShowStartupMessage("creating plotter");
		panelPlotter = new opendap.clients.odc.plot.Panel_View_Plot();
		if( !panelPlotter.zInitialize(sbError) ){
			ApplicationController.vShowWarning("Failed to initialize plotter: " + sbError);
			sbError.setLength(0);
		}
		ApplicationController.getInstance().vShowStartupMessage("creating retrieval panel");
		jpanelRetrieve = new Panel_Retrieve();
		if( !ApplicationController.getInstance().getRetrieveModel().zInitialize( jpanelRetrieve, sbError ) ){
			sbError.insert(0, "Failed to initialize retrieval model: ");
			return false;
		}
		if( !jpanelRetrieve.zInitialize(sbError) ){
			sbError.insert(0, "Failed to initialize retrieval panel: ");
			return false;
		}
		ApplicationController.getInstance().vShowStartupMessage("creating feedback panel");
		panelFeedbackEmail = new Panel_Feedback_Email();
		if( !panelFeedbackEmail.zInitialize(sbError) ){
			System.err.println("Failed to initialize email feedback panel: " + sbError);
			sbError.setLength(0);
		}
		panelFeedbackBug = new Panel_Feedback_Bug();
		if( !panelFeedbackBug.zInitialize(sbError) ){
			System.err.println("Failed to initialize bug feedback panel: " + sbError);
			sbError.setLength(0);
		}
		ApplicationController.getInstance().vShowStartupMessage("creating help panel");
		panelHelp = new Panel_Help();
		if( !panelHelp.zInitialize( sbError ) ){
			System.err.println("Failed to initialize help: " + sbError);
			sbError.setLength(0);
		}

		ApplicationController.getInstance().vShowStartupMessage("creating GCMD search tab");
		opendap.clients.odc.search.GCMD.GCMDSearch frameGCMDSearch = ApplicationController.getInstance().getGCMDSearch();

		ApplicationController.getInstance().vShowStartupMessage("creating dataset list search tab");
		opendap.clients.odc.search.DatasetList panelSearchDatasetList = ApplicationController.getInstance().getSearchPanel_DatasetList( sbError );
		if( panelSearchDatasetList == null ){
			ApplicationController.vShowWarning( "Dataset list search unavailable: " + sbError.toString() );
			sbError.setLength( 0 );
		}

		ApplicationController.getInstance().vShowStartupMessage("creating dataset list search tab");
		opendap.clients.odc.search.DatasetList panelSearchTHREDDS = ApplicationController.getInstance().getSearchPanel_THREDDS( sbError );
		if( panelSearchTHREDDS == null ){
			ApplicationController.vShowWarning( "THREDDS search unavailable: " + sbError.toString() );
			sbError.setLength( 0 );
		}
		
		ApplicationController.getInstance().vShowStartupMessage("creating ECHO search tab");
		opendap.clients.odc.search.ECHO.ECHOSearchWindow frameECHOSearch = ApplicationController.getInstance().getECHOSearch();

		// now add everything to the tabs
		boolean zShowViewTab = ConfigurationManager.getInstance().getProperty_DISPLAY_ShowViewTab();
		jtpMain.addTab(" Search", null, jtpSelect, "Search, find and select the datasets you want to work with");
		jtpMain.addTab(" Retrieve", null, jpanelRetrieve, "Download your selected datasets and output them");
		if( zShowViewTab ) jtpMain.addTab(" View", null, jtpView, "Built-in viewers that allow you browse data and other information");
		jtpMain.addTab(" Help", null, panelHelp, "Help!");
		jtpMain.addTab(" Feedback", null, jtpFeedback, "Send comment to devs or post a bug");

		if( panelSearchDatasetList != null ) jtpSelect.addTab(" Dataset List", null, panelSearchDatasetList, "Master list of known datasets from OPeNDAP");
		if( panelSearchTHREDDS != null ) jtpSelect.addTab(" THREDDS", null, panelSearchTHREDDS, "Master catalog of known datasets from THREDDS/UCAR");
		if( frameGCMDSearch != null ) jtpSelect.addTab(" GCMD", null, frameGCMDSearch, "NASA's Global Change Master Directory provides access to a wide range of important datasets");
		if( frameECHOSearch != null ) jtpSelect.addTab(" ECHO", null, frameECHOSearch, "NASA EOS ClearingHOuse Search");
		if( !zReadOnly ) jtpSelect.addTab(" Favorites", null, panelFavorites, "A list of your favorite datasets");
		if( !zReadOnly ) jtpSelect.addTab(" Recent", null, panelRecent, "A list of datasets that have been selected most recently");

		// load extension search interfaces
		ClassLoader class_loader = this.getClass().getClassLoader();
		if( class_loader == null ){
			ApplicationController.vShowError("failed to get class loader, any extensions ignored");
		} else {
			// get extension file names
			// loop names
			//    load class
			//    call zinitialize
			//    add tab
		}

		jtpView.addTab(" Command", panelCommand);
		jtpView.addTab(" Editor", panelTextEditor);
		jtpView.addTab(" Data", panelDataView);
		jtpView.addTab(" Plotter", panelPlotter);
		jtpView.addTab(" Image", panelImageView);

		jtpFeedback.addTab(" Email Comment", panelFeedbackEmail );
		jtpFeedback.addTab(" Post Bug Report", panelFeedbackBug );

		jtpMain.addChangeListener(
			new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
				    if (((JTabbedPane)e.getSource()).getSelectedIndex() == 4) { // if the feedback tab is activated
						panelFeedbackBug.vUpdateLog();
				    }
				}
		    }
		);

		return true;
	}

	public void vActivate() {
		setVisible(true);
    }

	private int miStartupLocation_x;
	private int miStartupLocation_y;
	private int miStartupSize_Width;
	private int miStartupSize_Height;

    private void vSizeDisplay(){
        Dimension dimScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int iScreenSize_Height = dimScreenSize.height;
		int iScreenSize_Width  = dimScreenSize.width;
		int iBottomMargin = ConfigurationManager.getInstance().getProperty_MarginBottom();
		int iRightMargin = ConfigurationManager.getInstance().getProperty_MarginRight();
		miStartupSize_Width = ConfigurationManager.getInstance().getProperty_StartupSize_Width();
		miStartupSize_Height = ConfigurationManager.getInstance().getProperty_StartupSize_Height();
		miStartupLocation_x = ConfigurationManager.getInstance().getProperty_StartupLocation_X();
		miStartupLocation_y = ConfigurationManager.getInstance().getProperty_StartupLocation_Y();
		if( miStartupSize_Width == 0 && miStartupSize_Width == 0 ){ // unconfigured -- use default
			if( ConfigurationManager.getInstance().isUNIX() ){
				miStartupSize_Width = (int)(iScreenSize_Width * 0.80f);
				miStartupSize_Height = (int)(iScreenSize_Height * 0.80f);
			} else {
				miStartupSize_Width = iScreenSize_Width;
				miStartupSize_Height = iScreenSize_Height;
			}
		}
		int iStartupSize_AdjWidth = miStartupSize_Width; // adjusted for margin
		int iStartupSize_AdjHeight = miStartupSize_Height; // adjusted for margin
		if( (iScreenSize_Height - miStartupSize_Height)/2 < iBottomMargin ){
			iStartupSize_AdjHeight -= (iBottomMargin - (iScreenSize_Height - miStartupSize_Height)/2);
		}
		if( (iScreenSize_Width - miStartupSize_Width)/2 < iRightMargin ){
			iStartupSize_AdjWidth -= (iRightMargin - (iScreenSize_Height - miStartupSize_Height)/2);
		}
		Dimension dimStartup = new Dimension(iStartupSize_AdjWidth, iStartupSize_AdjHeight);
		if( miStartupLocation_x == 0 && miStartupLocation_y == 0 ){
			miStartupLocation_x = iScreenSize_Width/2 - (miStartupSize_Width/2);
			miStartupLocation_y = iScreenSize_Height/2 - (miStartupSize_Height/2); // do NOT use adjusted height
		}
		setSize(dimStartup);
		setLocation(miStartupLocation_x, miStartupLocation_y);
		miStartupSize_Width = iStartupSize_AdjWidth;
		miStartupSize_Height = iStartupSize_AdjHeight;
    }

	public void vSaveDisplayPositioning(){
		Point locCurrent = this.getLocation();
		Dimension dimCurrentSize = this.getSize();
		if( (this.getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH ){ // if maximized reset startup dims
			ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_DISPLAY_StartupLocation_X, "0" );
			ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_DISPLAY_StartupLocation_Y, "0" );
			ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_DISPLAY_StartupSize_Width, "0" );
			ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_DISPLAY_StartupSize_Height, "0" );
		} else if( miStartupLocation_x != locCurrent.x ||
			miStartupLocation_y != locCurrent.y ||
			miStartupSize_Width != dimCurrentSize.width ||
			miStartupSize_Height != dimCurrentSize.height ){
			ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_DISPLAY_StartupLocation_X, Integer.toString(locCurrent.x) );
			ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_DISPLAY_StartupLocation_Y, Integer.toString(locCurrent.y) );
			ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_DISPLAY_StartupSize_Width, Integer.toString(dimCurrentSize.width) );
			ConfigurationManager.getInstance().setOption( ConfigurationManager.PROPERTY_DISPLAY_StartupSize_Height, Integer.toString(dimCurrentSize.height) );
		} else {
			// user did not change window, do not store settings
		}
	}
	
	// This method is used to initialize parts of the interface that are dynamically sized but 
	// are not subject to sizing by layout managers; an example would be slider positionings
	private static void _vSetInitialSizings(){
		
	}	

	public void vActivateRetrievalPanel(){
		jtpMain.setSelectedIndex(1); // activate the retrieve tab
	}

	public void vActivateCommandPanel(){
		jtpMain.setSelectedIndex(2); // activate the view tab
		jtpView.setSelectedIndex(0);
	}

	public void vActivateViewDataPanel(){
	   SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jtpMain.setSelectedIndex(2); // activate the view tab
					jtpView.setSelectedIndex(2); // activate the table tab
				}
			}
		);
	}
	
	public void vActivateViewTablePanel(){
	   SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jtpMain.setSelectedIndex(2); // activate the view tab
					jtpView.setSelectedIndex(2); // activate the table tab
				}
			}
		);
	}

	public void vActivatePlotter(){
	   SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jtpMain.setSelectedIndex(2); // activate the view tab
					jtpView.setSelectedIndex(4); // activate the plotting tab
					Panel_View_Plot.getPanel_Definition()._vActivateVariableSelector();
				}
			}
		);
	}

	public void vActivateViewImagePanel(){
	   SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jtpMain.setSelectedIndex(2); // activate the view tab
					jtpView.setSelectedIndex(3); // activate the view/images tab
				}
			}
		);
	}

	public java.io.OutputStream getTextViewerOS(){
		return panelCommand.getOutputStream();
	}

	public java.io.OutputStream getImageViewerOS( String sImageFileName, StringBuffer sbError ){
		return panelImageView.getCacheFileOutputStream(sImageFileName, sbError);
	}

	public java.io.OutputStream getTableViewerOS( StringBuffer sbError ){
		return panelTableView.getOutputStream(sbError);
	}

	public Panel_View_Variable getVariableViewer(){ return this.panelDataView.panelVarView; }

	public Panel_View_Data getDataViewer(){ return this.panelDataView; }
	
	public Panel_View_Image getImageViewer(){ return this.panelImageView; }

	public Panel_View_Plot getPlotter(){ return this.panelPlotter; }

	public Panel_View_Table getTableViewer(){ return this.panelTableView; }

	public StatusBar getStatusBar(){
		return mStatusBar;
	}

	public Panel_View_Command getPanel_Command(){ return this.panelCommand; }
	
	public Panel_Select_Recent getPanel_Recent(){ return this.panelRecent; }

	public Panel_Retrieve getPanel_Retrieve(){ return jpanelRetrieve; }

	public void vUpdateFavorites(){
		panelFavorites.vRefreshFavoritesList();
	}

	public void vUpdateRecent(){
		if( panelRecent != null ) panelRecent.vRefreshRecentList();
	}

	/* this is overloaded because you cannot add to a top-level swing component, you
	 * must add to the content pane instead. */
	public void add(Component componentToAdd, Object constraints) {
		if(componentToAdd instanceof JRootPane){
			super.add(componentToAdd, constraints);
		} else {
			this.getContentPane().add(componentToAdd, constraints);
		}
	}

	public void vShowAlert_Status( String sStatus ){
	}

	public void vShowAlert_Error( String sError ){
		vShowAlert( sError );
	}

//	private final static Font fontMessage = new java.awt.Font("SansSerif", Font.BOLD, 12);
//	private final static LayoutManager layoutAlert = new GridBagLayout();
//	private final static GridBagConstraints constraintAlert = new GridBagConstraints();
//	private final static LayoutManager layoutBL = new BorderLayout();

	private void vShowAlert(final String sMessage){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					Message.show(sMessage);
					// old way: JOptionPane.showMessageDialog(ApplicationFrame.this, sMessage);
				}
			}
		);
		Thread.yield();
	}

}

class MasterKeyListener implements KeyListener {

	public MasterKeyListener( ){}

	public void keyPressed(KeyEvent ke){
//		System.out.println("key pressed: "  +  ke.getKeyCode());
//		if( ke.getKeyCode() == ke.VK_F10 ){
//			ApplicationController.getInstance().vDumpMessages();
//		}
	}

	public void keyReleased(KeyEvent ke){
	}

	public void keyTyped(KeyEvent ke){
		// consumed
	}
}


