package opendap.clients.odc;

/**
 * Title:        Application Frame
 * Description:  This is the outer container of the GUI
 * Copyright:    Copyright (c) 2002-4
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.57
 */

import opendap.clients.odc.plot.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class ApplicationFrame extends JFrame {

	private ApplicationController mApplicationController;
	private StatusBar mStatusBar;

	private final JTabbedPane jtpMain = new JTabbedPane();
	private final JTabbedPane jtpSelect = new JTabbedPane();
	private final JTabbedPane jtpView   = new JTabbedPane();
	private final JTabbedPane jtpFeedback   = new JTabbedPane();
	private Panel_Select_Favorites panelFavorites;
	private Panel_Select_Recent panelRecent;
	private Panel_Retrieve jpanelRetrieve;
	private Panel_View_Text panelTextView;
	private Panel_View_Table panelTableView;
	private Panel_View_Image panelImageView;
	private Panel_View_Plot panelPlotter;
	private Panel_Help panelHelp;
	private Panel_Feedback_Email panelFeedbackEmail;
	private Panel_Feedback_Bug panelFeedbackBug;

	KeyListener klMaster = new MasterKeyListener();

    public ApplicationFrame() {}

    boolean zInitialize(String sTitle, ApplicationController appcontroller, StringBuffer sbError){

	    try {
			ApplicationController.getInstance().vShowStartupMessage("initializing main frame");
			mApplicationController = appcontroller;
			if( mApplicationController == null ){
				sbError.append("no controller exists");
				return false;
			}

			this.setTitle(' ' + sTitle);

			Utility.iconAdd(this);

			this.getContentPane().setLayout(new java.awt.BorderLayout());

			// tabs
			ApplicationController.getInstance().vShowStartupMessage("creating tabs");
			if( !this.zBuildMainTabs( jtpMain, sbError ) ){
				sbError.insert(0, "failed to create interface: ");
				return false;
			}
			this.add(jtpMain, java.awt.BorderLayout.CENTER);

			// status bar
			ApplicationController.getInstance().vShowStartupMessage("creating status bar");
			mStatusBar = new StatusBar();
			if( !mStatusBar.zInitialize(sbError) ){
				sbError.insert(0, "Failed to initialize status bar: ");
				return false;
			}
			this.add(mStatusBar, java.awt.BorderLayout.SOUTH);

			vSizeDisplay();

			// window closer
			WindowListener listenerCloser = new WindowAdapter(){
				public void windowClosing(WindowEvent e){
					ApplicationController.vForceExit();
				}
			};
			this.addWindowListener(listenerCloser);

			this.addKeyListener(klMaster);
			return true;

		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
    }

	void vClearStatus(){
		if( mStatusBar != null ) mStatusBar.vClear();
	}

	void vShowStatus( String sMessage ){
		if( mStatusBar != null ) mStatusBar.vShowStatus(sMessage);
	}

	void vAppendStatus( String sMessage ){
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

		ApplicationController.getInstance().vShowStartupMessage("creating data viewers");
		panelTextView = new Panel_View_Text();
		if( !panelTextView.zInitialize(sbError) ){
			sbError.insert(0, "Failed to initialize text view panel: ");
			return false;
		}
		panelTableView = new Panel_View_Table();
		if( !panelTableView.zInitialize(sbError) ){
			sbError.insert(0, "Failed to initialize table view panel: ");
			return false;
		}
		panelImageView = new Panel_View_Image();
		if( !panelImageView.zInitialize(sbError) ){
			ApplicationController.vShowWarning("Failed to initialize image view panel: " + sbError);
			sbError.setLength(0);
		}
		panelPlotter = new opendap.clients.odc.plot.Panel_View_Plot();
		if( !panelPlotter.zInitialize(sbError) ){
			ApplicationController.vShowWarning("Failed to initialize plotter: " + sbError);
			sbError.setLength(0);
		}
		ApplicationController.getInstance().vShowStartupMessage("creating retrieval panel");
		jpanelRetrieve = new Panel_Retrieve();
		if( !jpanelRetrieve.zInitialize(sbError) ){
			sbError.insert(0, "Failed to initialize retrieval panel: ");
			return false;
		}
		if( !ApplicationController.getInstance().getRetrieveModel().zInitialize(jpanelRetrieve, sbError) ){
			sbError.insert(0, "Failed to initialize retrieval model: ");
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
		if( !panelHelp.zInitialize(sbError) ){
			System.err.println("Failed to initialize help: " + sbError);
			sbError.setLength(0);
		}

		ApplicationController.getInstance().vShowStartupMessage("creating GCMD search tab");
		opendap.clients.odc.GCMD.GCMDSearch frameGCMDSearch = ApplicationController.getInstance().getGCMDSearch();

		ApplicationController.getInstance().vShowStartupMessage("creating dataset list search tab");
		opendap.clients.odc.DatasetList.DatasetList searchDataSetList = ApplicationController.getInstance().getDatasetListSearch();

		ApplicationController.getInstance().vShowStartupMessage("creating ECHO search tab");
		opendap.clients.odc.ECHO.ECHOSearchWindow frameECHOSearch = ApplicationController.getInstance().getECHOSearch();

// not implemented currently
//		ApplicationController.getInstance().vShowStartupMessage("creating THREDDS search tab");
//		opendap.clients.odc.THREDDS.THREDDSSearch frameTHREDDSSearch = ApplicationController.getInstance().getTHREDDSSearch();

		// now add everything to the tabs
		boolean zShowViewTab = ConfigurationManager.getInstance().getProperty_DISPLAY_ShowViewTab();
		jtpMain.addTab(" Search", null, jtpSelect, "Search, find and select the datasets you want to work with");
		jtpMain.addTab(" Retrieve", null, jpanelRetrieve, "Download your selected datasets and output them");
		if( zShowViewTab ) jtpMain.addTab(" View", null, jtpView, "Built-in viewers that allow you browse data and other information");
		jtpMain.addTab(" Help", null, panelHelp, "Help!");
		jtpMain.addTab(" Feedback", null, jtpFeedback, "Send comment to devs or post a bug");

		if( searchDataSetList != null ) jtpSelect.addTab(" Dataset List", null, searchDataSetList, "Master list of known datasets");
		if( frameGCMDSearch != null ) jtpSelect.addTab(" GCMD", null, frameGCMDSearch, "NASA's Global Change Master Directory provides access to a wide range of important datasets");
		if( frameECHOSearch != null ) jtpSelect.addTab(" ECHO", null, frameECHOSearch, "NASA EOS ClearingHOuse Search");
//		if( frameTHREDDSSearch != null ) jtpSelect.addTab(" THREDDS", null, frameTHREDDSSearch, "THREDDS Catalog Search");
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

		jtpView.addTab(" Text", panelTextView);
		jtpView.addTab(" Table", panelTableView);
		jtpView.addTab(" Image File", panelImageView);
		jtpView.addTab(" Plotter", panelPlotter);

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

    void vActivate() {
		setVisible(true);
    }

    private void vSizeDisplay(){
        Dimension dimScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int iScreenSize_Height = dimScreenSize.height;
		int iScreenSize_Width  = dimScreenSize.width;
		int iBottomMargin = ConfigurationManager.getInstance().getProperty_MarginBottom();
		int iRightMargin = ConfigurationManager.getInstance().getProperty_MarginRight();
		int iStartupSize_Width = ConfigurationManager.getInstance().getProperty_StartupSize_Width();
		int iStartupSize_Height = ConfigurationManager.getInstance().getProperty_StartupSize_Height();
		if( iStartupSize_Width == 0 && iStartupSize_Width == 0 ){ // unconfigured -- use default
			if( ConfigurationManager.getInstance().isUNIX() ){
				iStartupSize_Width = (int)(iScreenSize_Width * 0.80f);
				iStartupSize_Height = (int)(iScreenSize_Height * 0.80f);
			} else {
				iStartupSize_Width = iScreenSize_Width;
				iStartupSize_Height = iScreenSize_Height;
			}
		}
		int iStartupSize_AdjWidth = iStartupSize_Width; // adjusted for margin
		int iStartupSize_AdjHeight = iStartupSize_Height; // adjusted for margin
		if( (iScreenSize_Height - iStartupSize_Height)/2 < iBottomMargin ){
			iStartupSize_AdjHeight -= (iBottomMargin - (iScreenSize_Height - iStartupSize_Height)/2);
		}
		if( (iScreenSize_Width - iStartupSize_Width)/2 < iRightMargin ){
			iStartupSize_AdjWidth -= (iRightMargin - (iScreenSize_Height - iStartupSize_Height)/2);
		}
		Dimension dimStartup = new Dimension(iStartupSize_AdjWidth, iStartupSize_AdjHeight);
		int iStartupLocation_x = iScreenSize_Width/2 - (iStartupSize_Width/2);
		int iStartupLocation_y = iScreenSize_Height/2 - (iStartupSize_Height/2); // do NOT use adjusted height
		setSize(dimStartup);
		setLocation(iStartupLocation_x, iStartupLocation_y);
    }

	public void vActivateRetrievalPanel(){
		jtpMain.setSelectedIndex(1); // activate the retrieve tab
	}

	public void vActivateViewTextPanel(){
		jtpMain.setSelectedIndex(2); // activate the view tab
		jtpView.setSelectedIndex(0);
	}

	public void vActivateViewTablePanel(){
	   SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jtpMain.setSelectedIndex(2); // activate the view tab
					jtpView.setSelectedIndex(1); // activate the table tab
				}
			}
		);
	}

	public void vActivatePlotter(){
	   SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jtpMain.setSelectedIndex(2); // activate the view tab
					jtpView.setSelectedIndex(3); // activate the plotting tab
					panelPlotter.getPanel_Definition().vActivateVariableSelector();
				}
			}
		);
	}

	public void vActivateViewImagePanel(){
	   SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jtpMain.setSelectedIndex(2); // activate the view tab
					jtpView.setSelectedIndex(2); // activate the view/images tab
				}
			}
		);
	}

	public java.io.OutputStream getTextViewerOS(){
		return panelTextView.getOutputStream();
	}

	public java.io.OutputStream getImageViewerOS( String sImageFileName, StringBuffer sbError ){
		return panelImageView.getCacheFileOutputStream(sImageFileName, sbError);
	}

	public java.io.OutputStream getTableViewerOS( StringBuffer sbError ){
		return panelTableView.getOutputStream(sbError);
	}

	public boolean zAddDataToPlotter_Invoked( DodsURL url, StringBuffer sbError ){
		return panelPlotter.zAddData_Invoked(url, sbError);
	}

	public Panel_View_Image getImageViewer(){ return this.panelImageView; }

	public Panel_View_Plot getPlotter(){ return this.panelPlotter; }

	public Panel_View_Table getTableViewer(){ return this.panelTableView; }

	StatusBar getStatusBar(){
		return mStatusBar;
	}

	Panel_Select_Recent getPanel_Recent(){ return this.panelRecent; }

	void vUpdateFavorites(){
		panelFavorites.vRefreshFavoritesList();
	}

	void vUpdateRecent(){
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

	void vShowAlert_Status( String sStatus ){
	}

	void vShowAlert_Error( String sError ){
		vShowAlert( sError );
	}

	private final static Font fontMessage = new java.awt.Font("SansSerif", Font.BOLD, 12);
	private final static LayoutManager layoutAlert = new GridBagLayout();
	private final static GridBagConstraints constraintAlert = new GridBagConstraints();
	private final static LayoutManager layoutBL = new BorderLayout();

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

class StatusBar extends JPanel {

	private JPanel jpanelStatus;
	private JLabel jlabelStatus;
	private JPanel jpanelMemory;
	private JPanel jpanelProgress;
	private JProgressBar jpbActivity;
	private JProgressBar jpbMemory;
	private java.util.Timer mStatusTimer;
	private long STATUS_TIMEOUT_MS = 20000L; // 20 seconds
	private boolean zUseStatusTimeout =  true;
        private StatusTimeout mtimeoutCurrent = null;

	StatusBar(){}

	void vClear(){
		this.vShowStatus("");
		this.vShowProgress(0);
	}

	void vShowStatus( final String sStatusMessage ){
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				StatusBar.this.jlabelStatus.setText(" " + sStatusMessage);
			}
		});
		Thread.yield();
		if( zUseStatusTimeout ) vResetStatusTimer();
	}

	void vAppendStatus( final String sStatusMessage ){
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				String sExistingText = StatusBar.this.jlabelStatus.getText();
				StatusBar.this.jlabelStatus.setText(sExistingText + sStatusMessage);
			}
		});
		Thread.yield();
		if( zUseStatusTimeout ) vResetStatusTimer();
	}

	void vShowProgress( final int iPercent ){
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				jpbActivity.setValue(iPercent);
			}
		});
		Thread.yield();
	}

	int getProgress(){
		return jpbActivity.getValue();
	}

	boolean zInitialize( StringBuffer sbError ) {

		this.setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
		javax.swing.border.Border borderInset = BorderFactory.createLoweredBevelBorder();

		// set up status label
		jlabelStatus = new JLabel();
		jlabelStatus.setOpaque(true);
		jlabelStatus.setBackground(Styles.colorLightGray);
		jlabelStatus.setPreferredSize(new Dimension(1000, 20));
		jlabelStatus.setMinimumSize(new Dimension(100, 20));
		jlabelStatus.setText(" ");
		jlabelStatus.setBorder(borderInset);
		jlabelStatus.addMouseListener(
			new MouseAdapter(){
				public void mousePressed( MouseEvent me ){
					if( me.getClickCount() == 2 ){
						ApplicationController.getInstance().vClearStatus();
					}
				}
			}
		);

		// set up memory bar
		jpanelMemory = new JPanel();
		jpanelMemory.setPreferredSize(new Dimension(100, 20));
		jpanelMemory.setMinimumSize(new Dimension(100, 20));
		jpanelMemory.setBorder(borderInset);
		jpanelMemory.setLayout(new BorderLayout());
		jpbMemory = new JProgressBar();
		int iMaximumMemory = (int)(ApplicationController.getMemory_Max() / 1048576); // memory is measured in megabtyes
		jpbMemory.setMaximum(iMaximumMemory);
		jpbMemory.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));
		jpanelMemory.add(jpbMemory, BorderLayout.CENTER);
		jpbMemory.addMouseListener(
			new MouseAdapter(){
				public void mousePressed( MouseEvent me ){
					if( me.getClickCount() == 2 ){
						String sMemoryStatus = ApplicationController.getInstance().sMemoryStatus();
						JOptionPane jop = new JOptionPane(sMemoryStatus);
						jop.setFont( Styles.fontFixed10 );
						jop.showMessageDialog(ApplicationController.getInstance().getAppFrame(), sMemoryStatus);
					}
				}
			}
		);
		vUpdateMemoryBar();

		// set up progress bar
		jpanelProgress = new JPanel();
		jpanelProgress.setPreferredSize(new Dimension(100, 20));
		jpanelProgress.setMinimumSize(new Dimension(100, 20));
		jpanelProgress.setBorder(borderInset);
		jpanelProgress.setLayout(new BorderLayout());
		jpbActivity = new JProgressBar();
		jpbActivity.setMaximum(100);
		jpbActivity.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));
		jpanelProgress.add(jpbActivity, BorderLayout.CENTER);
		jpbActivity.addMouseListener(
			new MouseAdapter(){
				public void mousePressed( MouseEvent me ){
					if( me.getClickCount() == 2 ){
						ApplicationController.getInstance().vCancelActivities();
					}
				}
			}
		);

		// set up help button
		JButton jbuttonHelp = new JButton("?");
		jbuttonHelp.setMargin(new Insets(2, 2, 2, 2));
		jbuttonHelp.setPreferredSize(new Dimension(20, 20));
		jbuttonHelp.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event){
					ApplicationController.getInstance().vShowHelp();
				}
			}
		);

		// add pieces to status bar
		this.add(jlabelStatus);
		this.add(Box.createHorizontalStrut(2));
		this.add(jpanelMemory);
		this.add(Box.createHorizontalStrut(2));
		this.add(jpanelProgress);
		this.add(Box.createHorizontalStrut(2));
//		this.add(jbuttonHelp);  // no longer in use

		// set up status time out
		mStatusTimer = new java.util.Timer();
		STATUS_TIMEOUT_MS = (long)ConfigurationManager.getInstance().getProperty_StatusTimeout();
		if( STATUS_TIMEOUT_MS == 0 ) zUseStatusTimeout = false; else zUseStatusTimeout = true;

		return true;
	}

	static boolean zMemoryReadingAvailable = true;
//		long nMax = getMemory_Max();
//		long nTotal = getMemory_Total();
//		long nFree = getMemory_Free();
//		sb.append("--- Memory Status ------------------------------------------------\n");
//		sb.append("       max: " + Utility.sFormatFixedRight(nMax, 12, '.') + "  max available to the ODC\n");
//		sb.append("     total: " + Utility.sFormatFixedRight(nTotal, 12, '.') + "  total amount currently allocated\n");
//		sb.append("      free: " + Utility.sFormatFixedRight(nFree, 12, '.') + "  unused memory in the allocation\n");
//		sb.append("      used: " + Utility.sFormatFixedRight((nTotal - nFree) , 12, '.')+ "  used memory (total-free)\n");
//		sb.append(" available: " + (int)((nMax - nTotal + nFree)/1048576) + " M  amount left (max - total + free)\n");
//		return sb.toString();
	void vUpdateMemoryBar(){
		try {
			final long nTotalMemory = ApplicationController.getMemory_Total();
			final long nFreeMemory = ApplicationController.getMemory_Free();
			final long nUsedMemory = (nTotalMemory - nFreeMemory);
			final int iUsedMemoryM = (int)(nUsedMemory / 1048576);
			zMemoryReadingAvailable = true;
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					jpbMemory.setValue(iUsedMemoryM);
					jpbMemory.setString(" " + iUsedMemoryM);
				}
			});
			Thread.yield();
		} catch(Exception ex) {
			if( zMemoryReadingAvailable ){
				zMemoryReadingAvailable = false;
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						jpbMemory.setString("?");
					}
				});
			}
		}
	}

	private void vResetStatusTimer(){
		if( mStatusTimer == null ) return; // nothing to reset
		StatusTimeout statustimeout = new StatusTimeout();
		mStatusTimer.schedule(statustimeout, STATUS_TIMEOUT_MS);
        mtimeoutCurrent = statustimeout;
	}

	class StatusTimeout extends java.util.TimerTask {
		public final void run(){
			if( this == mtimeoutCurrent ) StatusBar.this.vClear(); // only the latest timeout should do a clear
		}
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


