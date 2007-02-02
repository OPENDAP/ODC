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

package opendap.clients.odc.ECHO;


import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import gnu.regexp.*;

/**
 * This class displays a panel for result valids display
 *
 * @author Zhifang(Sheila Jiang)
 */

public class ResultValidsPanel extends JScrollPane implements ActionListener {

    //private final int SIZE = 38;
    private int SIZE;
	private JCheckBox[] enableBox;
    private Vector actionListeners;
    private boolean[] isShown;

    public ResultValidsPanel(boolean isDiscovery) {

       	actionListeners = new Vector();
		String[] validNames;
		if (isDiscovery) {
		    String[] discoveryValids = {"archiveCenter", "accessConstraints", "campaign", "collectionDescription", "collectionId", "collectionState", "dataCenterId", "dataSetId", "externalPublicationCitation", "id", "instrument", "instrumentLongName", "lastUpdate", "localInsertDate", "localLastUpdate", "longName", "maintenanceUpdateFrequency", "ordered", "parameter", "platform", "platformLongName", "price", "primaryCollectionFlag", "processingCenter", "processingLevel", "processingLevelDescription", "psa", "revisionDate", "sensor", "sensorLongName", "shortName", "spatialKeywords", "spatialType", "suggestedUsage", "temporalKeywords", "type", "versionDescription", "versionId"};
		    validNames = discoveryValids;
		    SIZE = validNames.length;
		} else {
		    String[] granuleValids = {"browse", "beginningDateTime", "campaign", "calendarDate", "cloudCover", "dataSetId", "dayNightFlag", "deleteEffectiveDate", "endingDateTime", "globalGranule", "granuleId", "id", "insertTime", "instrument", "lastUpdate", "localGranuleId", "localInsertDate", "localLastUpdate", "localVersionId", "orderId", "pgeVersion", "platform", "psa", "price", "primaryCollectionId", "productionDateTime", "rangeBeginningDate", "rangeBeginningTime", "rangeEndingDate", "rangeEndingTime", "reprocessingActual", "reprocessingPlanned", "sensor", "shortName", "sizeMBData", "timeOfDay", "versionId", "zoneIdentifier"};
		    validNames = granuleValids;
		    SIZE = validNames.length;
		}
		enableBox = new JCheckBox[SIZE];
		for (int i=0; i<SIZE; i++){
			enableBox[i] = new JCheckBox(validNames[i]);
			enableBox[i].addActionListener(this);
        }

		//set all check boxes visible
		isShown = new boolean[SIZE];
		for (int i=0; i<SIZE; i++){
			isShown[i] = true;
        }

		initGUI();

		//set all invisible, only selected will be shown later
		for (int i=0; i<SIZE; i++){
			isShown[i] = false;
        }

    }

    public void initGUI() {

		JPanel resultPanel = new JPanel();
		resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
	    resultPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Valids for query return"), BorderFactory.createEmptyBorder(0,20,10,0)));
		//resultPanel.add(new JLabel("Select valids from following for the query result"));
	    resultPanel.add(Box.createRigidArea(new Dimension(0,10)));

		for (int i=0; i<SIZE; i++){
			if(isShown[i]) {
		    	resultPanel.add(enableBox[i]);
			    resultPanel.add(Box.createVerticalGlue());
			} else {
	    		resultPanel.remove(enableBox[i]);
		    	resultPanel.remove(Box.createVerticalGlue());
			}
		}
		setPreferredSize(new Dimension(300, 250));
		setViewportView((Component) resultPanel);
    }

    /**
     * Add an action listener to each checkbox.  It will receive action events
     * when the checkbox is clicked.
     * @param a The <code>ActionListener</code>.
     */

    public void addActionListener(ActionListener a) {
	    actionListeners.addElement(a);
    }

    /**
     * Set the action commands.
     * @param enabledCommand The command used when the check box is clicked.
     */
    public void setActionCommands(String[] enabledCommand) {
		for (int i=0; i<SIZE; i++){
			enableBox[i].setActionCommand(enabledCommand[i]);
        }
    }

    /**
     * Catch events from the GUI components and pass them on to the
     * action listeners.
     * @param e The event.
     */

    public void actionPerformed(ActionEvent e) {

	/*
	ActionEvent evt = new ActionEvent(this, 0, e.getActionCommand());
	for(int i=0;i<actionListeners.size();i++) {
	((ActionListener)actionListeners.elementAt(i)).actionPerformed(evt);
	}*/
		Object o = e.getSource();
	    for (int i=0; i<SIZE; i++){
	        if (o == enableBox[i]) isShown[i] = enableBox[i].isSelected();
        }
    }

    /**
     * Returns a Vector of strings  of chosen result valids
     * @return The string Vector for the result valids being chosen
     */

    public Vector getResultValids() {
		Vector valids = new Vector();
	    for(int i=0; i<SIZE; i++){
			if(enableBox[i].isSelected()) valids.addElement(enableBox[i].getText());
        }
		return valids;
    }

}
