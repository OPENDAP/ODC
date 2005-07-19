package opendap.clients.odc;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.*;
import java.util.ArrayList;

public class Activity extends Thread {
	public final static int MODE_Created = 1;
	public final static int MODE_Running = 2;
	public final static int MODE_BlockedOnSocket = 3;
	public final static int MODE_Stopped = 4;
	public final static String BUTTON_TEXT_ACTION_CANCEL = "Cancel...";
	public final static javax.swing.JTextArea jtaMessage = new javax.swing.JTextArea("[activity popup]");
	public final static Font fontMessage = new java.awt.Font("SansSerif", Font.BOLD, 12);
	public final static LayoutManager layoutGB = new GridBagLayout();
	public final static GridBagConstraints constraintGB = new GridBagConstraints();
	public static Dimension dimMax;
	static {
		Dimension dimScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double dScreenHeight = dimScreenSize.getHeight();
		double dScreenWidth  = dimScreenSize.getWidth();
		dimMax = new Dimension((int)(dScreenWidth * 0.80d), 200);
		jtaMessage.setOpaque(true);
		jtaMessage.setEditable(false);
		jtaMessage.setFont(fontMessage);
		jtaMessage.setBackground(java.awt.Color.yellow);
		jtaMessage.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED, Color.lightGray, Color.darkGray));
		jtaMessage.setMargin(new Insets(10, 10, 10, 10));
		jtaMessage.setMaximumSize(new Dimension((int)(dScreenWidth * 0.80d), (int)(dScreenHeight * 0.80d) ));
	}
	static int mctCurrentActivities = 0;
	private long mID = System.currentTimeMillis();
	private int meMODE = MODE_Created;
	private boolean mzCancelled = false;

	public Activity(){
		setDaemon(true);
	}
	public void vUpdateStatus( final String sStatusUpdate ){
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jtaMessage.setText( msStatusMessage + "\n" + sStatusUpdate + "\n\t(click to cancel)" );
				}
			}
		);
	}
	public void setMode( int eMODE ){ meMODE = eMODE; }
	public void setSocket( Socket socket ){ mSocket = socket; }
	ArrayList listContinuations = new ArrayList();
	MouseAdapter mmouseadapterPopupCancel;
	javax.swing.JButton mjbuttonActivator;
	java.awt.event.ActionListener mactionDo;
	java.awt.event.ActionListener mactionCancel;
	String msStatusMessage;
	Socket mSocket;
	public String getMessage(){ return msStatusMessage; }
	public void vDoActivity( javax.swing.JButton jbuttonActivator, java.awt.event.ActionListener actionButton, Continuation_DoCancel con, String sStatusMessage ){
		mjbuttonActivator = jbuttonActivator;
		mactionDo = actionButton;
		listContinuations.add( con );
		msStatusMessage = sStatusMessage;
		start();
	}
	public void vAddContinuation( Continuation_DoCancel con, String sStatusMessage ){
		listContinuations.add( con );
		vUpdateStatus( sStatusMessage );
		con.Do();
	}
	public void vDoActivity( Continuation_DoCancel con ){
		listContinuations.add( con );
		this.start();
	}
	public void vCancelActivity(){
		mzCancelled = true;
		final JPanel jpanelGlassPane = (JPanel)ApplicationController.getInstance().getAppFrame().getGlassPane();
		SwingUtilities.invokeLater(
			new Runnable(){
				public void run(){
					jpanelGlassPane.setVisible(false);
				}
			}
		);
		ApplicationController.getInstance().vShowStatus("Cancelling action... " + this.msStatusMessage);
		try {
			if( meMODE == MODE_BlockedOnSocket ) if( mSocket != null ) mSocket.close();
		} catch(Exception ex) {
		}
		for( int xContinuation = listContinuations.size(); xContinuation > 0; xContinuation-- ){
			Continuation_DoCancel con_current = (Continuation_DoCancel)listContinuations.get(xContinuation - 1);
			con_current.Cancel();
		}
	}
	boolean isCancelled(){ return mzCancelled; }
	public void run(){
		mctCurrentActivities++;
		ApplicationController.getInstance().vActivity_Add(this);
		String sActivatorText = "";
		final JPanel jpanelGlassPane = (JPanel)ApplicationController.getInstance().getAppFrame().getGlassPane();
		if( mjbuttonActivator != null ){
			sActivatorText = mjbuttonActivator.getText();
		}
		boolean zMessageActive = false;
		if( msStatusMessage != null ){
			ApplicationController.vShowStatus(msStatusMessage);
			if( ConfigurationManager.getInstance().getProperty_DISPLAY_ShowPopupCancel() ){
				SwingUtilities.invokeLater(
					new Runnable(){
						public void run(){
							jtaMessage.setText( msStatusMessage + "\n(click to cancel)" );
							mmouseadapterPopupCancel =
								new MouseAdapter(){
									public void mousePressed( MouseEvent me ){
										vCancelActivity();
									}
								 };
							jpanelGlassPane.setLayout(layoutGB);
							jpanelGlassPane.add(jtaMessage, constraintGB);
							if( msStatusMessage.length() > 80 ){
								jpanelGlassPane.setSize(dimMax);
							}
							jtaMessage.addMouseListener(mmouseadapterPopupCancel);
							jpanelGlassPane.setVisible(true);
						}
					}
				);
				zMessageActive = true;
			}
		}
		try {
			final Continuation_DoCancel con_first = (Continuation_DoCancel)listContinuations.get(0);
			if( con_first == null ){
				ApplicationController.vShowError("Internal Error, no continuation");
			} else {
				SwingUtilities.invokeLater(
					new Runnable(){
						public void run(){
		    				if( mctCurrentActivities == 1 ) ProgressManager.getInstance().start();
							if( mjbuttonActivator != null ){
								mjbuttonActivator.removeActionListener(mactionDo);
								mjbuttonActivator.setText(BUTTON_TEXT_ACTION_CANCEL);
								mactionCancel = new ActionListener(){
									public void actionPerformed(ActionEvent event) {
										Activity.this.vCancelActivity();
									}
								};
								mjbuttonActivator.addActionListener(mactionCancel);
							}
						}
					}
				);
				con_first.Do();
			}
		} catch(Exception ex) {
			if( this.isInterrupted() ){
				try {
					ApplicationController.vShowStatus("Activity " + mID + " '" + msStatusMessage + "' interrupted");
				} catch(Exception exCancel) {
					ApplicationController.vShowError("Unexpected error cancelling continuation: " + exCancel);
				}
			} else {
				StringBuffer sbError = new StringBuffer("Unexpected error running activity " + mID + ": ");
				Utility.vUnexpectedError(ex, sbError);
				ApplicationController.vShowError(sbError.toString());
			}
		} finally {
			mctCurrentActivities--;
			if( mctCurrentActivities == 0 ) ProgressManager.getInstance().interrupt();
			try {
				jtaMessage.removeMouseListener(mmouseadapterPopupCancel);
				if( zMessageActive ){
					SwingUtilities.invokeLater(
						new Runnable(){
							public void run(){
								jpanelGlassPane.remove(jtaMessage);
								jpanelGlassPane.setVisible(false);
							}
						}
					);
				}
				if( mjbuttonActivator != null ){
					final String sActivatorText_final = sActivatorText;
					SwingUtilities.invokeLater(
						new Runnable(){
							public void run(){
								mjbuttonActivator.removeActionListener(mactionCancel);
								mjbuttonActivator.setText(sActivatorText_final);
								mjbuttonActivator.addActionListener(mactionDo);
							}
						}
					);
				}
				ApplicationController.getInstance().vActivity_Remove(this);
			} catch(Exception ex) {
				ApplicationController.vShowError("Unexpected error finalizing activity " + mID + ": " + ex);
			}
		}
	}
}

class ProgressManager extends Thread {
	private static final int SLEEP_INTERVAL_MS = 200;
    private static final ProgressManager thisSingleton = new ProgressManager();
    static ProgressManager getInstance(){return thisSingleton;}
	private StatusBar mStatusBar;
	ProgressManager(){
		mStatusBar = ApplicationController.getInstance().getAppFrame().getStatusBar();
		mStatusBar.addMouseListener(
			new MouseAdapter(){
				public void mousePressed( MouseEvent me ){
					if( me.getClickCount() == 2 ){
						int iResponse = javax.swing.JOptionPane.showConfirmDialog(null, "Cancel activities?", "Cancel", javax.swing.JOptionPane.YES_NO_OPTION);
						if( iResponse == javax.swing.JOptionPane.YES_OPTION ){
							ApplicationController.getInstance().vCancelActivities();
						}
					}
				}
			}
		);
	}
	public void run(){
		while(true){
			try {
				sleep(SLEEP_INTERVAL_MS);
				mStatusBar.vUpdateMemoryBar();
				int iProgress = mStatusBar.getProgress();
				if( iProgress == 100 ){
					mStatusBar.vShowProgress(0);
				} else {
					mStatusBar.vShowProgress(iProgress + 5);
				}
			} catch(InterruptedException ex) {
				mStatusBar.vShowProgress(0);
				break; // and exit
			}
		}
	}
}


