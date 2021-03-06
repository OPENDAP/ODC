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

package opendap.clients.odc;

import java.lang.*;
import javax.swing.*;
import java.awt.event.*;

public class ImportProgressWindow extends javax.swing.JFrame
    implements ActionListener
{

    public ImportProgressWindow() {
	currentValue = 0;
	minimum = 0;
	maximum = 20;
        initComponents();
    }

    public ImportProgressWindow(int min, int max) {
	currentValue = 0;
	minimum = min;
	maximum = max;
        initComponents();
    }

    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        progress = new javax.swing.JProgressBar(minimum, maximum);
        outputScroller = new javax.swing.JScrollPane();
        outputArea = new javax.swing.JTextArea();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gridBagConstraints1;

        setTitle("Importing Data into Matlab");

        jPanel1.setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gridBagConstraints2;

        jPanel1.setBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(10, 10, 10, 10)));
        gridBagConstraints2 = new java.awt.GridBagConstraints();
		progress.setStringPainted(true);
        jPanel1.add(progress, gridBagConstraints2);

        outputArea.setAutoscrolls(false);
        outputScroller.setPreferredSize(new java.awt.Dimension(250, 200));
        outputScroller.setViewportView(outputArea);

        gridBagConstraints2 = new java.awt.GridBagConstraints();
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.gridy = 1;
        jPanel1.add(outputScroller, gridBagConstraints2);

        gridBagConstraints1 = new java.awt.GridBagConstraints();
        gridBagConstraints1.gridx = 0;
        gridBagConstraints1.gridy = 0;
        gridBagConstraints1.gridwidth = 2;
        getContentPane().add(jPanel1, gridBagConstraints1);

        cancelButton.setText("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancel");
        gridBagConstraints1 = new java.awt.GridBagConstraints();
        gridBagConstraints1.gridx = 1;
        gridBagConstraints1.gridy = 1;
		gridBagConstraints1.weightx = 0.5;
		gridBagConstraints1.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(cancelButton, gridBagConstraints1);

		okButton.addActionListener(this);
		okButton.setActionCommand("ok");
        okButton.setText("OK");
        okButton.setEnabled(false);
        gridBagConstraints1 = new java.awt.GridBagConstraints();
        gridBagConstraints1.gridx = 0;
        gridBagConstraints1.gridy = 1;
		gridBagConstraints1.weightx = 0.5;
		gridBagConstraints1.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(okButton, gridBagConstraints1);

        pack();
    }

	public void setMinimum(int value) {
		minimum = value;
		progress.setMinimum(minimum);
	}

	public void setMaximum(int value) {
		maximum = value;
		progress.setMaximum(maximum);
	}

	public void finishedVar(String name) {
		currentValue++;
		if (currentValue == maximum) {
			okButton.setEnabled(true);
			cancelButton.setEnabled(false);
		}

		progress.setValue(currentValue);
		outputArea.append("Downloaded variable: " + name + "\n");
    }

	public void actionPerformed(ActionEvent e) {
		setVisible(false);
	}

/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		new ImportProgressWindow().setVisible( true );
	}

    private int currentValue;
    private int maximum;
    private int minimum;

    private javax.swing.JPanel jPanel1;
    private javax.swing.JProgressBar progress;
    private javax.swing.JScrollPane outputScroller;
    private javax.swing.JTextArea outputArea;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton okButton;

}



