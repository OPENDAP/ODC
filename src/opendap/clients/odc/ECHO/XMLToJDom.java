package opendap.clients.odc.ECHO;

import opendap.clients.odc.*;

import java.lang.*;
import javax.swing.*;
import java.awt.event.*;
//import java.awt.*;
import java.util.*;
import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.input.DOMBuilder;
import java.io.*;

/**
 * @author Sheila Jiang
 */
public class XMLToJDom {
	private Document outXMLDoc;

	public Document convert(File xmlFile){

		//String XMLMessage = myXMLOutputter.outputString(xmlDoc);
		try {
			org.jdom.input.SAXBuilder saxBuilder = new org.jdom.input.SAXBuilder();
			outXMLDoc = saxBuilder.build(xmlFile);
		} catch(JDOMException ex){
			opendap.clients.odc.ApplicationController.vShowError("XMLToJDOM XML file conversion to Document failed: " + ex.getMessage());
		} catch( java.io.IOException ioex ) {
			opendap.clients.odc.ApplicationController.vShowError("XMLToJDOM XML file conversion to Document had an IO failure: " + ioex.getMessage());
		}
		return outXMLDoc;
	}

	public Document getDoc(){
		return outXMLDoc;
	}

	public static void main(String[] argv) {
		File xml;
		XMLToJDom test = new XMLToJDom();
		Vector queryValids = new Vector();
		//convert xmlFile to a JDOM document
		try {
			xml = new File("/home/DODS/Java-DODS/ECHO_static_valids.xml");
			test.convert(xml);
		} catch(NullPointerException ex){
			ApplicationController.vShowError("XML to JDOM main: file does not exist");
		}

		//convert the JDOM document back to a string
		XMLOutputter myXMLOutputter = new XMLOutputter();
		//String xmlMessage = myXMLOutputter.outputString(test.getDoc());
		Element root = (test.getDoc()).getRootElement();
		List rootChildren = root.getChildren();
		//Object it = theList.get(4);
		//System.out.println(it.toString());
		String[] categoryName = {"archiveCenter", "campaign", "sensorName"};
		for (int i=0; i<categoryName.length; i++) {
			for (int j=0; j<rootChildren.size(); j++) {
				Element category = (Element)rootChildren.get(j);
				//Vector valids = new Vector();
				String[] valids;
				System.out.println(j+"  " + category.getChildText("CategoryName"));
				System.out.println(j+"  " + categoryName[i]);
				if ((category.getChildText("CategoryName")).equals(categoryName[i])) {
					Element criteriaValues = category.getChild("CriteriaList").getChild("Criteria").getChild("CriteriaValues");
					java.util.List values = criteriaValues.getChildren(); //<CriteriaValue>
					System.out.println("Debug");
					valids = new String[values.size()];
					for (int k=0; k<values.size(); k++) {
						String theValue = ((Element)values.get(k)).getText();
						System.out.println(theValue);
						//valids.addElement(theValue);
						valids[k] = theValue;
					}
					CollectionValids theCategory = new CollectionValids(categoryName[i], valids);
					queryValids.addElement(theCategory);
					System.out.println(theCategory.getName());
					System.out.println(theCategory.getValids().length);
					System.out.println(theCategory.getValids()[2]);
				}
			}
		}
	}
}

