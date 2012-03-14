package opendap.clients.odc.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Utility;

public class StatusBar extends JPanel {

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

	public void vShowProgress( final int iPercent ){
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				jpbActivity.setValue(iPercent);
			}
		});
		Thread.yield();
	}

	public int getProgress(){
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
						ApplicationController.vClearStatus();
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
		jpbMemory.setToolTipText("Double-click for memory info"); // not working for unknown reason TODO
		int iMaximumMemory = (int)(Utility.getMemory_Max() / 1048576); // memory is measured in megabtyes
		jpbMemory.setMaximum(iMaximumMemory);
		jpbMemory.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));
		jpanelMemory.add(jpbMemory, BorderLayout.CENTER);
		final JTextArea textArea = new JTextArea();
		jpbMemory.addMouseListener(
			new MouseAdapter(){
				public void mousePressed( MouseEvent me ){
					if( me.getClickCount() == 2 ){
						String sMemoryStatus = ApplicationController.getInstance().sMemoryStatus();
						textArea.setPreferredSize( new Dimension( 500, 200 ) );
						textArea.setLineWrap(true);
						textArea.setWrapStyleWord(true);
						textArea.setMargin(new Insets(5,5,5,5));
						textArea.setText( sMemoryStatus );
						textArea.setFont( Styles.fontFixed12 );
						JOptionPane.showMessageDialog( ApplicationController.getInstance().getAppFrame(), textArea );
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
					ApplicationController.vShowHelp();
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
	public void vUpdateMemoryBar(){
		try {
			final long nTotalMemory = Utility.getMemory_Total();
			final long nFreeMemory = Utility.getMemory_Free();
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
				javax.swing.SwingUtilities.invokeLater(
					new Runnable(){
						public void run() {
							jpbMemory.setString("?");
						}
					}
				);
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
