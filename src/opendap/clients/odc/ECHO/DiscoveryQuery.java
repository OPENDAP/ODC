package opendap.clients.odc.ECHO;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.jdom.*;
import org.jdom.output.XMLOutputter;
import opendap.clients.odc.*;

/**
 * @author Sheila Jiang
 */

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

public class DiscoveryQuery implements QueryRequest {

	private Document queryReqDocument;
	private Document iimsaqlDocument;
	private SpatialQuery spacialQuery;
	private TemporalQuery temporalQuery;
	private PresentResult presentResult;
//     private CollectionValids collectionValids;

	public DiscoveryQuery(){
		Element catalogServiceElement = new Element("CatalogService");
		queryReqDocument = new Document(catalogServiceElement);
		Element queryElement = new Element("query");
		iimsaqlDocument = new Document(queryElement);
		presentResult = new PresentResult();
	}

	public Document getQueryRequest(){
		return queryReqDocument;
	}

	public Document getIIMSAQL(){
		return iimsaqlDocument;
	}



	public Document buildQueryRequest(String sECHOurl, SpatialQuery spatialQuery, TemporalQuery temporalQuery, Vector valids, Vector resultValids){
		Element rootElement = queryReqDocument.getRootElement();

		//set element to be constrained and the systemID of referenced DTD
		String sCatalogURL = opendap.clients.odc.Utility.sConnectPaths(sECHOurl, "/", "dtd/CatalogService.dtd");
		DocType docType = new DocType("CatalogService", sCatalogURL);
		queryReqDocument.setDocType(docType);

		//add more elements

		// QueryRequest
		Element queryRequestElement = new Element("QueryRequest");
		rootElement.addContent(queryRequestElement);

		//QueryExpression
		Element queryExprElement = new Element("QueryExpression");
		queryRequestElement.addContent(queryExprElement);

		//query
		Element queryElement = new Element("query");
		queryExprElement.addContent(queryElement);

		//CDATA

		//execute buildIIMSAQL first
		buildIIMSAQL(spatialQuery, temporalQuery, valids);
		XMLOutputter myXMLOutputter = new XMLOutputter();
		CDATA cData = new CDATA(myXMLOutputter.outputString(iimsaqlDocument));
		queryElement.addContent(cData);

		//namespace
		Element nameSpaceElement = new Element("namespace");
		nameSpaceElement.addContent("none");
		queryExprElement.addContent(nameSpaceElement);

		//QueryLanguage
		Element queryLanguageElement = new Element("QueryLanguage");
		queryLanguageElement.addContent(new Element("IIMSAQL"));
		queryExprElement.addContent(queryLanguageElement);

		//ResultType
		Element resultTypeElement = new Element("ResultType");
		resultTypeElement.addContent(new Element("RESULTS"));
		queryRequestElement.addContent(resultTypeElement);

		//Result presentation details
		//The constructor is to be change to a taking para one
		presentResult.buildResultDetail(resultValids);
		Element[] resultDet = presentResult.getResultDetail();

		//add resultDet to the Document
		for (int i=0; i<3; i++){
			queryRequestElement.addContent(resultDet[i]);
		}
		return queryReqDocument;
	}

	public Document buildIIMSAQL(SpatialQuery spatialQuery, TemporalQuery temporalQuery, Vector valids) {
		Element rootElement =  iimsaqlDocument.getRootElement();

		// set element to be constrained and the systemID of referenced DTD
		String sECHO_URL = ConfigurationManager.getInstance().getProperty_URL_ECHO();
		String sDTD = Utility.sConnectPaths(sECHO_URL, "/", "dtd/IIMSAQLQueryLanguage.dtd");
		DocType docType = new DocType("query", sDTD);
		iimsaqlDocument.setDocType(docType);

		// add more elements for
		Element forElement = new Element("for");
		forElement.setAttribute(new Attribute("value", "collections"));
		rootElement.addContent(forElement);

		// from ???

		//dataCenterId
		Element dataCenterIdElement = new Element("dataCenterId");
		dataCenterIdElement.addContent(new Element("all"));
		rootElement.addContent(dataCenterIdElement);

		//where
		Element whereElement = new Element("where");
		rootElement.addContent(whereElement);

		//collectionCondition
		Element collectionCondElement;
		for (int i=0; i<valids.size(); i++) {
			Object o = valids.elementAt(i);
			if (!o.getClass().isInstance(new JTextField())) {
				CollectionValids theList = (CollectionValids)valids.elementAt(i);
				if (theList.isSelected() && theList.getSelectedIndex() != -1) {
					collectionCondElement = new Element("collectionCondition");
					Element temp = new Element(theList.getName());
					Element list = new Element("list");
					temp.addContent(list);
					for (int j=0; j<theList.getValids().length; j++){
						Element value = new Element("value");
						if (theList.getSelection(j)) {
							value.addContent("'" + theList.getValids()[j] + "'");
							list.addContent(value);
						}
					}
					collectionCondElement.addContent(temp);
					whereElement.addContent(collectionCondElement);
				}
			}
			else {
				JTextField theText = (JTextField)valids.elementAt(i);
				if (!theText.getText().equals("")) {
					collectionCondElement = new Element("collectionCondition");
					Element temp = new Element(theText.getName());
					Element value = new Element("value");
					value.addContent("'" + theText.getText() + "'");
					temp.addContent(value);
					collectionCondElement.addContent(temp);
					whereElement.addContent(collectionCondElement);
				}
			}
		}

		//spacial query
		collectionCondElement = new Element("collectionCondition");
		if (spatialQuery.getSpatialQuery()[0] != null) {
			collectionCondElement.addContent(spatialQuery.getSpatialQuery()[0]);
		} else if (spatialQuery.getSpatialQuery()[1] != null) {
			collectionCondElement.addContent(spatialQuery.getSpatialQuery()[1]);
			whereElement.addContent(collectionCondElement);
		}

//Element temporalElement = new Element("temporal");
//collectionCondElement.addContent(temporalElement);
//spatialElement.addContent(spatialQuery.getSpatial()[0]);
//temporalElement.addContent(temporalQuery.getTemporal()[0]);

//collectionValids
/* old code
Element campaignElement = new Element("campaign");
campaignElement.addContent(new Element("value").addContent(valids.getValids()[0]));
collectionCondElement.addContent(campaignElement);
*/

		return iimsaqlDocument;

	}

	public PresentResult getPresentResult() {
		return presentResult;
	}

}



