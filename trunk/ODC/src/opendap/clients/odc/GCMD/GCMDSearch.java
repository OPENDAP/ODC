package opendap.clients.odc.GCMD;

import opendap.clients.odc.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Displays a window for GCMD search
 * @author Zhifang(Sheila Jiang)
 */
public class GCMDSearch extends JPanel {
    //private Vector actionListeners;
    //private String actionCommand;
	private final JTabbedPane tabbedPane = new JTabbedPane();
    private JPanel freeTextSearch;
    private JPanel keywordSearch;
    private DodsURL[] urls;

    /**
     * Create a new <code>GCMDSearch</code>
     */
    public GCMDSearch() {
		//super("ECHO Search Wizard");
		//actionListeners = new Vector();

		//add title info
		tabbedPane.setBorder(BorderFactory.createTitledBorder("Global Change Master Directory Search"));
		//tabbedPane.addChangeListener(this);
		//add tabbed panel and button panel
		setLayout(new BorderLayout());

		// Create and intialize the command panel
		final JPanel jpanelAccessGCMD = new JPanel();
		final JButton jbuttonAccessGCMD = new JButton("Click to \nAccess GCMD");
		Styles.vApply( Styles.STYLE_NetworkAction, jbuttonAccessGCMD);
		jbuttonAccessGCMD.setMinimumSize(new Dimension(250, 100));
		jbuttonAccessGCMD.setMaximumSize(new Dimension(250, 100));
		jbuttonAccessGCMD.setPreferredSize(new Dimension(250, 100));
		StringBuffer sbError = new StringBuffer();
		if( Resources.imageiconInternet == null ){
			// do not use an icon
		} else {
			jbuttonAccessGCMD.setIcon(Resources.imageiconInternet);
		}
		final ActionListener action =
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					Activity activityAccessGCMD = new Activity();
					Continuation_DoCancel con =
						new Continuation_DoCancel(){
						    public void Do(){
								GCMDSearch.this.remove(jpanelAccessGCMD);
								add(tabbedPane, BorderLayout.CENTER);
								tabbedPane.removeAll();
                                try {
                                    freeTextSearch = new FreeTextSearch( GCMDSearch.this );
                                    tabbedPane.addTab("Free Text", freeTextSearch);
                                } catch(Exception ex){
                                    StringBuffer sb = new StringBuffer(80);
                                    Utility.vUnexpectedError(ex, sb);
                                    ApplicationController.getInstance().vShowError("Error creating free text search panel: " + sb);
                                }
                                try {
                                    keywordSearch = new KeywordSearch( GCMDSearch.this );
                                    tabbedPane.addTab("Keyword", keywordSearch);
                                } catch(Exception ex){
                                    StringBuffer sb = new StringBuffer(80);
                                    Utility.vUnexpectedError(ex, sb);
                                    ApplicationController.getInstance().vShowError("Error creating keyword search panel: " + sb);
                                }
								jbuttonAccessGCMD.setVisible(false);
							}
							public void Cancel(){
								tabbedPane.removeAll();
							}
						};
					activityAccessGCMD.vDoActivity(	jbuttonAccessGCMD, this, con, "Accessing GCMD" );
				}
			};
		jbuttonAccessGCMD.addActionListener(action);

		// add button to center of display
		jpanelAccessGCMD.setBorder(javax.swing.BorderFactory.createEmptyBorder());
		jpanelAccessGCMD.setLayout(new java.awt.GridBagLayout());
		jpanelAccessGCMD.add( jbuttonAccessGCMD );
		this.add(jpanelAccessGCMD, BorderLayout.CENTER);

    }

    public static void main(String args[]) {
		JFrame frame = new JFrame("GCMD");
		frame.getContentPane().add(new GCMDSearch());
		frame.pack();
		frame.setVisible(true);
    }
}




