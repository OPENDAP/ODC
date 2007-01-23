package opendap.clients.odc.ECHO;

//import opendap.clients.odc.SearchInterface;
import opendap.clients.odc.*;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import org.jdom.*;
import org.jdom.output.XMLOutputter;

/**
 * This class displays a window for initiating a search query
 *
 * @author Zhifang(Sheila Jiang)
 */
public class ECHOSearchWindow extends opendap.clients.odc.SearchInterface implements ActionListener
{
    private Vector actionListeners;
    private String actionCommand;
    private JTabbedPane tabbedPane;
    private JSplitPane discoveryPanel;
    private JSplitPane granulePanel;
    private JPanel spatialPanel;
    private JPanel temporalPanel;
    private JPanel buttonPanel;
    private JScrollPane resultValidsPanel;
    private JButton nextButton;
    private JButton previousButton;
    private JButton submitButton;
    private JButton cancelButton;
    private DodsURL[] urls;

    /**
     * Create a new <code>DiscoverySearchPanel/code>
     */
    public ECHOSearchWindow() {

		String sPathXMLValids = ConfigurationManager.getInstance().getProperty_PATH_XML_ECHO_Valids(); // "/home/DODS/Java-DODS/ECHO_static_valids.xml"

		actionListeners = new Vector();
		resultValidsPanel = new ResultValidsPanel(true); //default is discovery
		discoveryPanel = new DiscoverySearchPanel(sPathXMLValids);

	    tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Discovery", discoveryPanel);
        tabbedPane.setSelectedIndex(0);
		granulePanel = new GranuleSearchPanel(sPathXMLValids);
	    tabbedPane.addTab("Granule", granulePanel);
		spatialPanel = new Panel_SpatialTemporal();
        tabbedPane.addTab("Spatial", spatialPanel);
	    temporalPanel = new JPanel();//temp
        tabbedPane.addTab("Temporal", temporalPanel);

		nextButton = new JButton("Next >");
		previousButton = new JButton("< Previous");
		submitButton = new JButton("Submit");
		cancelButton = new JButton("Cancel");

		// Setup the button bar at the bottom
        buttonPanel = new JPanel();
	    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

	    buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));

	    cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
	    buttonPanel.add(cancelButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));

	    previousButton.setActionCommand("previous");
		previousButton.addActionListener(this);
	    previousButton.setEnabled(false);
		buttonPanel.add(previousButton);

	    nextButton.setActionCommand("next");
		nextButton.addActionListener(this);
	    buttonPanel.add(nextButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		submitButton.setActionCommand("submit");
		submitButton.addActionListener(this);
		submitButton.setEnabled(false);
		buttonPanel.add(submitButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(Box.createHorizontalGlue());

		//add title info
		tabbedPane.setBorder(BorderFactory.createTitledBorder("EOS ClearingHOuse Search"));
		//add tabbed panel and button panel
		setLayout(new BorderLayout());
		add(tabbedPane, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

    }

    public boolean zInitialize(StringBuffer sbError){ return true; }

	public void addListSelectionListener(ListSelectionListener listener){
		// todo
	}

    /**
     * Add an action listener to each combo box.  It will receive action events
     * when an item is selected.
     * @param a The <code>ActionListener</code>.
     */
    public void addActionListener(ActionListener a) {
		actionListeners.addElement(a);
    }

    /**
     * Set the action command.
     * @param command The command used when the button is clicked.
     */
    public void setActionCommand(String command) {
		actionCommand = command;
    }

    /**
     * Catch events from the GUI components and pass them on to the
     * action listeners.
     * @param e The event.
     */
    public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("next")) {
			if (tabbedPane.getSelectedIndex() == 0)
			resultValidsPanel = new ResultValidsPanel(true);
			else if (tabbedPane.getSelectedIndex() == 1)
			resultValidsPanel = new ResultValidsPanel(false);
			tabbedPane.setVisible(false);
			resultValidsPanel.setVisible(true);
			remove(tabbedPane);
			add(resultValidsPanel, BorderLayout.CENTER);
			//pack();
			submitButton.setEnabled(true);
			previousButton.setEnabled(true);
			nextButton.setEnabled(false);
			getRootPane().getContentPane().repaint();
		}
		else if(command.equals("previous")) {
			tabbedPane.setVisible(true);
			resultValidsPanel.setVisible(false);
			remove(resultValidsPanel);
			add(tabbedPane, BorderLayout.CENTER);
			//pack();
			submitButton.setEnabled(false);
			previousButton.setEnabled(false);
			nextButton.setEnabled(true);
			getRootPane().getContentPane().repaint();
		}
		else if (command.equals("submit")) {
			StringBuffer sbError = new StringBuffer(80);
			if (tabbedPane.getSelectedIndex() == 0) {
				Vector vectorQueryValids = ((DiscoverySearchPanel)discoveryPanel).getQueryValids(sbError);
				if( vectorQueryValids == null ){
					ApplicationController.vShowError("Error getting query valids: " + sbError);
					return;
				}
				Vector resultValids = ((ResultValidsPanel)resultValidsPanel).getResultValids();

			   /*debug
			   for (int i=0;i<temp.size();i++){
			   CollectionValids theList = (CollectionValids)temp.elementAt(i);
			   System.out.println("\n" + theList.getName() + "\t" + theList.isSelected());
			   for (int j=0;j<theList.getValids().length;j++){
				System.out.println(theList.getValids()[j] + "   " + theList.getSelection(j));
			   }
			   }*/

				// call methods to build and submit query
				SpatialQuery spatial = new SpatialQuery();
				// spatial.buildSpatialQuery(((Panel_SpatialTemporal)spatialPanel).getEasternmost(), ((Panel_SpatialTemporal)spatialPanel).getWesternmost(), ((Panel_SpatialTemporal)spatialPanel).getNorthernmost(), ((Panel_SpatialTemporal)spatialPanel).getSouthernmost(), ((Panel_SpatialTemporal)spatialPanel).getKeywords());

				TemporalQuery temporal = new TemporalQuery();
				XMLOutputter myXMLOutputter = new XMLOutputter();
				SOAPMessenger myMessenger = new SOAPMessenger();
				DiscoveryQuery myQuery = new DiscoveryQuery();
				String sECHO_URL = ConfigurationManager.getInstance().getProperty_URL_ECHO();
				Document outDoc = myMessenger.exeQuery(myQuery.buildQueryRequest(sECHO_URL, spatial, temporal, vectorQueryValids, resultValids));
				//display result
				myQuery.getPresentResult().displayResult(outDoc, true);
				//popup granule window
				//		System.out.println("Before generate granule panel...");
				String sValidsXML = ConfigurationManager.getInstance().getProperty_PATH_XML_ECHO_Valids();
				granulePanel = new GranuleSearchPanel(sValidsXML, outDoc);
				//		System.out.println("after generate granule panel...");
				//resultValidsPanel = new ResultValidsPanel(false);
				tabbedPane.setVisible(true);
				resultValidsPanel.setVisible(false);
				remove(resultValidsPanel);
				tabbedPane.setComponentAt(1, (Component)granulePanel);
				tabbedPane.setSelectedIndex(1);
				add(tabbedPane, BorderLayout.CENTER);
				//pack();
				submitButton.setEnabled(false);
				previousButton.setEnabled(true);
				nextButton.setEnabled(true);
				getRootPane().getContentPane().repaint();

				//output to screen (debug)
				String myXML = myXMLOutputter.outputString(outDoc);
				System.out.println("The following output is converted from a JDOM Document.");
				System.out.println(myXML);
			}
			else if (tabbedPane.getSelectedIndex() == 1) {
				ApplicationController.vShowStatus("Executing ECHO granule search");
				Vector vectorQueryValids = ((GranuleSearchPanel)granulePanel).getQueryValids();
				Vector vectorResultValids = ((ResultValidsPanel)resultValidsPanel).getResultValids();

				// call methods to build and submit query
				SpatialQuery spatial = new SpatialQuery();
				// spatial.buildSpatialQuery(((Panel_SpatialTemporal)spatialPanel).getEasternmost(), ((Panel_SpatialTemporal)spatialPanel).getWesternmost(), ((Panel_SpatialTemporal)spatialPanel).getNorthernmost(), ((Panel_SpatialTemporal)spatialPanel).getSouthernmost(), ((Panel_SpatialTemporal)spatialPanel).getKeywords());

				TemporalQuery temporal = new TemporalQuery();
				XMLOutputter myXMLOutputter = new XMLOutputter();
				SOAPMessenger myMessenger = new SOAPMessenger();

				GranuleQuery myQuery = new GranuleQuery();
				Document docQueryRequest = myQuery.buildQueryRequest(spatial, temporal, vectorQueryValids, vectorResultValids);
				if( docQueryRequest == null ){
					ApplicationController.vShowError("Internal error - query request document was blank");
					return;
				}
				Document docResult = myMessenger.exeQuery(docQueryRequest);
				if( docResult == null ){
					ApplicationController.vShowError("Internal error - query results document was blank");
					return;
				}
				//output to screen
				String myXML = myXMLOutputter.outputString(docResult);
				System.out.println("The following output is converted from a JDOM Document.");
				System.out.println(myXML);
				//display result
				myQuery.getPresentResult().displayResult(docResult, false);
			}
			getRootPane().getContentPane().repaint();
		}
		else if(command.equals("cancel")) {
			setVisible(false);
		}
    }

    //Temporally used for granule panel
    protected Component makeTextPanel(String text) {
        JPanel panel = new JPanel(false);
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);
        return panel;
    }

    public static void main(String args[]) {
		JFrame window = new JFrame("ECHO Search Wizard");
		ECHOSearchWindow ECHOWin = new ECHOSearchWindow();
		window.getContentPane().add(ECHOWin);
		window.pack();
		window.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {System.exit(0);}
		});
		window.setLocation(50,50);
		window.setVisible(true);
    }

    //To be implemented
    public DodsURL[] getURLs( StringBuffer sbError ){
		return urls;
    }

}




