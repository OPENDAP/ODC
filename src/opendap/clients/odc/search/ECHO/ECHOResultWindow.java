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

package opendap.clients.odc.search.ECHO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import gnu.regexp.*;

/**
 * This class displays a window for initiating a search query
 *
 * @author Zhifang(Sheila Jiang)
 */
public class ECHOResultWindow extends JFrame implements ActionListener {

	private Vector actionListeners;
	private String actionCommand;
	// private JTabbedPane tabbedPanel;
	private JScrollPane validsPanel;
	private JPanel valuesPanel;
	private JPanel buttonPanel;
	private JButton nextButton;
	private JButton previousButton;
	private JButton finishButton;
	private JButton cancelButton;

	/**
	 * Create a new <code>    </code>
	 */
	public ECHOResultWindow() {
		super("ECHO Search Result");
		actionListeners = new Vector();
		/*
		tabbedPane = new JTabbedPane();

		Component searchPanel = new DiscoverySearchPanel();
		tabbedPane.addTab("Discovery", searchPanel);
		tabbedPane.setSelectedIndex(0);
		Component panel2 = makeTextPanel("Blah blah");//to be changed
		tabbedPane.addTab("Granule", panel2);
		*/
		validsPanel = new ResultValidsPanel(true);
		valuesPanel = new ResultValuesPanel();
		nextButton = new JButton("Next >");
		previousButton = new JButton("< Previous");
		finishButton = new JButton("Finish");
		cancelButton = new JButton("Cancel");

		//
		// Setup the button bar at the bottom
		//
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));

		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
		cancelButton.setEnabled(false);
		buttonPanel.add(cancelButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));

		previousButton.setActionCommand("previous");
		previousButton.addActionListener(this);
		previousButton.setEnabled(false);
		buttonPanel.add(previousButton);

		nextButton.setActionCommand("next");
		nextButton.addActionListener(this);
		nextButton.setEnabled(false);
		buttonPanel.add(nextButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		finishButton.setActionCommand("finish");
		finishButton.addActionListener(this);
		//finishButton.setEnabled(false);
		buttonPanel.add(finishButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(Box.createHorizontalGlue());

		//add valid panel, value panel and button panel
		//getContentPane().add(tabbedPane, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(validsPanel, BorderLayout.WEST);
		getContentPane().add(valuesPanel, BorderLayout.EAST);

		pack();

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
/*
if(command.equals("next")) {
getContentPane().remove(tabbedPane);
searchPanel = new ResultValidsPanel();
getContentPane().add(searchPanel, BorderLayout.CENTER);
pack();
finishButton.setEnabled(true);
previousButton.setEnabled(true);
nextButton.setEnabled(false);
}
else */
		if(command.equals("finish")) {
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
		ECHOResultWindow window = new ECHOResultWindow();
		window.setLocation(50,50);

		window.setVisible(true);
	}

}




