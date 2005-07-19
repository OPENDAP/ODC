package opendap.clients.odc.GCMD;

import opendap.clients.odc.*;
import java.util.Vector;
import java.awt.Dimension;
import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This class searches the GCMD database for datasets.
 */
public class FreeTextSearch extends PanelTemplate {

	JComboBox logicTypeBox;
    private Vector vectorSearchPanels;

    public FreeTextSearch(String sBaseURL, GCMDSearch parent) {
		super(sBaseURL, parent);
		initFreeTextGUI();
    }

    protected void initFreeTextGUI() {
		String[] logicTypes = { "AND", "OR" };
		logicTypeBox = new JComboBox(logicTypes);

		SearchPanel searchpanelOne = new SearchPanel();
		searchpanelOne.addActionListener(this);
		searchpanelOne.setActionCommands("togglePanel", "search");
		searchpanelOne.setMaximumSize(new Dimension(32768,30));
		vectorSearchPanels = new Vector();
		vectorSearchPanels.addElement(searchpanelOne);
		getSearchControlsPanel().add(searchpanelOne);
		searchpanelOne.setEnabled(true);

		SearchPanel searchpanelTwo = new SearchPanel();
		searchpanelTwo.addActionListener(this);
		searchpanelTwo.setActionCommands("togglePanel", "search");
		searchpanelTwo.setMaximumSize(new Dimension(32768,30));
		vectorSearchPanels.addElement(searchpanelTwo);
		getSearchControlsPanel().add(searchpanelTwo);
		searchpanelTwo.setEnabled(false);

    }

	String getConstraint_Interface(){
		String sQueryString = "";
		String logicType = (String)logicTypeBox.getSelectedItem();
		for(int i=0;i<vectorSearchPanels.size();i++) {
			if(((SearchPanel)vectorSearchPanels.elementAt(i)).isEnabled()) {
				if(!sQueryString.equals("")) sQueryString += " " + logicType + " ";
				sQueryString += ((SearchPanel)vectorSearchPanels.elementAt(i)).getSearchString();
			}
		}
		return sQueryString;
	}

    public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		// This event is generated when the user clicks the checkbox next
		// to a search field.  Enable the search panel if it's disabled and
		// if it's the last one, create a new disabled search panel.
		if(command.equals("togglePanel")) {
			if(vectorSearchPanels.lastElement().equals(e.getSource())
			   && ((SearchPanel)e.getSource()).isEnabled() == false)
			{
				SearchPanel search = new SearchPanel();
				search.setEnabled(false);
				search.setMaximumSize(new Dimension(32768, 30));
				search.addActionListener(this);
				search.setActionCommands("togglePanel", "search");
				vectorSearchPanels.addElement(search);
				getSearchControlsPanel().add(search);
				getSearchControlsPanel().revalidate();
				validate();
			}

			((SearchPanel)e.getSource()).toggleEnabled();
		}

		super.actionPerformed(e);

	}
}


