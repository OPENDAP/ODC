package opendap.clients.odc.GCMD;

import opendap.clients.odc.*;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.event.*;
import java.net.URL;
import java.util.Vector;
import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.input.*;
import org.jdom.input.DOMBuilder;

public class KeywordSearch extends PanelTemplate {

    JPanel panelTop;
    JScrollPane topScroller;
    JList jlistTopic;
    JList jlistTerms;
    Document outXMLDoc;

    public KeywordSearch(String sBaseURL, GCMDSearch parent) {
		super(sBaseURL, parent);
		try {
			//xml = new File("http://gcmd.nasa.gov/servlets/md/get_valids.py?type=parametersvalid");
			//xml = new File("get_valids.py");
			// convert a file to a JDOM Document
			DOMBuilder domBuilder = new DOMBuilder(false);
			outXMLDoc = domBuilder.build(new URL("http://gcmd.nasa.gov/servlets/md/get_valids.py?type=parametersvalid"));

// causes out of memory error:
//			SAXBuilder saxBuilder = new SAXBuilder(false);
//                        URL urlGCMD = new URL("http://gcmd.nasa.gov/servlets/md/get_valids.py?type=parametersvalid");
//			outXMLDoc = saxBuilder.build(urlGCMD);
		} catch(NullPointerException ex){
			opendap.clients.odc.ApplicationController.vShowError("Keyword Search error, file does not exist");
                        return;
		} catch(JDOMException ex){
			opendap.clients.odc.ApplicationController.vShowError("Keyword Search Panel XML file conversion to Document failed: " + ex.getMessage());
                        return;
		} catch(Exception ex){
			opendap.clients.odc.ApplicationController.vShowError("Keyword Search Panel XML file conversion to Document failed: " + ex.getMessage());
                        return;
		}
		initKeywordGUI();
    }

    private void initKeywordGUI() {
		//
		// Setup the search panel
		//
		panelTop = new JPanel();
		panelTop.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		panelTop.setLayout(new BoxLayout(panelTop, BoxLayout.X_AXIS));

		String[] topics = {"Agriculture", "Atmosphere", "Biosphere", "Cryosphere", "Human  Dimensions", "Hydrosphere", "Land Surface", "Oceans", "Paleoclimate", "Radiance Or Imagery", "Solid Earth", "Sun Earth Interactions"};

		jlistTopic = new JList(topics);
		jlistTopic.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		Dimension dimScreen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		int iPreferredHeight = (int)(dimScreen.getHeight() * .20); // topic list should be about a fifth of screen height

		JScrollPane topicScroller = new JScrollPane(jlistTopic);
		topicScroller.setPreferredSize(new Dimension(150, iPreferredHeight));
		topicScroller.setMinimumSize(new Dimension(150,iPreferredHeight));

		panelTop.add(topicScroller);

		jlistTerms = new JList();
		JScrollPane termScroller = new JScrollPane(jlistTerms);
		termScroller.setPreferredSize(new Dimension(300, iPreferredHeight));
		termScroller.setMinimumSize(new Dimension(300,iPreferredHeight));

		vSetupTopicListener(jlistTopic, jlistTerms); // so that topic terms are automatically displayed

		panelTop.add(termScroller);

		super.getSearchControlsPanel().add(panelTop);

    }

	String getConstraint_Interface(){
		try {
			String sQueryString = "";
			String sCategory = new String("EARTH%20SCIENCE");

			//if no <code>topic</code> selected, show all earth science
			if (jlistTopic.getSelectedValue() == null) {
				sQueryString += "[Parameters:Category='" + sCategory + "'";
			} else {

				// add topic constraint
				String sTopic = jlistTopic.getSelectedValue().toString().toUpperCase();
				sTopic = Utility.sReplaceString(sTopic, " ", "%20");
				sQueryString += "[Parameters:Category='" + sCategory + "',Topic='" + sTopic + "'";

				// add term constraints
				Object[] select = jlistTerms.getSelectedValues();
				if (select.length > 0) { // if no term selected, show all with this topic
					int indexOfSep = select[0].toString().indexOf('>');
					String term = select[0].toString().substring(0, indexOfSep-1);
					term = Utility.sReplaceString(term, " ", "%20");
					String var = select[0].toString().substring(indexOfSep+2);
					if (var.equals("")) { // if variable is empty, show all with this term
						sQueryString +=  ",Term='" + term + "'";
					} else {
						var = Utility.sReplaceString(var, " ", "%20");
						sQueryString +=  ",Term='" + term + "',Variable='" + var + "'";
						for(int i=1;i<select.length;i++) { // if more than one selected
							indexOfSep = select[i].toString().indexOf('>');
							term = select[i].toString().substring(0, indexOfSep-1);
							var = select[i].toString().substring(indexOfSep+2);
							term = Utility.sReplaceString(term, " ", "%20");
							var = Utility.sReplaceString(var, " ", "%20");
							sQueryString += "] OR " + "[Parameters:Category='" + sCategory + "',Topic='" + sTopic + "',Term='" + term + "',Variable='" + var + "'";
						}
					}
				}
			}
			sQueryString += "]";
			return sQueryString;
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("GCMD keyword search error");
			Utility.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError(sbError.toString());
			return "";
		}
	}

    public void vSetupTopicListener(final JList jlistTopics, final JList jlistTerms){
		jlistTopics.addListSelectionListener(
			new ListSelectionListener(){
			    public void valueChanged(ListSelectionEvent e) {
					if (jlistTopics.getSelectedValue() != null) {
						Vector valids = new Vector();
						try { //get the desired valids
							Element root = outXMLDoc.getRootElement();
							java.util.List parameters = root.getChildren();
							String select = jlistTopics.getSelectedValue().toString().toUpperCase();
							for (int i=0; i<parameters.size(); i++) {
								Element elementCurrent = (Element)parameters.get(i);
								if( elementCurrent == null ) continue;
								String sCategory = elementCurrent.getChildText("Category");
								String sTopic    = elementCurrent.getChildText("Topic");
								if( sCategory == null || sTopic == null ) continue;
								if (sCategory.equals("EARTH SCIENCE") && sTopic.equals(select)) {
									String temp = ((Element)parameters.get(i)).getChildText("Term") + " > " + ((Element)parameters.get(i)).getChildText("Variable");
									valids.add(temp);
								}
							}
						} catch(Exception ex){
							Utility.vUnexpectedError(ex, "Trying to get the desired valids");
						}
						jlistTerms.setListData(valids);
					}
				}
			}
		);
    }


}


