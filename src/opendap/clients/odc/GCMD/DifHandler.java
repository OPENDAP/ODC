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

package opendap.clients.odc.GCMD;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import java.util.*;

/**
 * This class is used by the SAX Parser to create a <code>Dif</code> from
 * the xml representation of a Dif.
 */
public class DifHandler extends DefaultHandler {
    boolean insideTitle;
    boolean insideSummary;
    boolean insideURL;
    boolean insideContentType;
    boolean insideID;

    boolean insideRole;
    boolean insideFirstName;
    boolean insideMiddleName;
    boolean insideLastName;
    boolean insideEmail;
    boolean insidePhone;
    boolean insideFax;
    boolean insideAddress;

    boolean insideSouthernmost;
    boolean insideNorthernmost;
    boolean insideWesternmost;
    boolean insideEasternmost;

    //boolean insideParameters;
    boolean insideCategory;
    boolean insideTopic;
    boolean insideTerm;
    boolean insideVariable;

    boolean insideStartDate;
    boolean insideStopDate;

    boolean insideLatResolution;
    boolean insideLongResolution;
    boolean insideTemporalResolution;

    String relatedURL;
    String contentType;
    String summary;

    String role;
    String name;
    String email;
    String phone;
    String fax;
    String address;

    String southernmost;
    String northernmost;
    String westernmost;
    String easternmost;

    String category;
    String topic;
    String term;
    String variable;

    String startDate;
    String stopDate;

    String latResolution;
    String longResolution;
    String temporalResolution;

    Vector difs;
    Dif dif;

	/*
	<Related_URL>
			  <URL_Content_Type>
				 <Type>GET DATA</Type>
			  </URL_Content_Type>
			  <URL>http://ingrid.ldgo.columbia.edu/SOURCES/.IGOSS/.nmc/.monthly/.sst/</URL>
			  <Description>
				 NMC SST Monthly data and analyses
			  </Description>
	  </Related_URL>
	*/
	public static final int STATE_Undefined = 0;
	public static final int STATE_InRelatedURL = 1;
	public static final int STATE_InRelatedInformation = 2;
	public static final int STATE_InContentType = 3;
	public static final int STATE_InType = 4;
	public static final int STATE_InDataURL = 5;
	public static final int STATE_InURL = 6;
	public static final int STATE_InURLDescription = 7;
	int miCurrentState = STATE_Undefined;

	String sURL_type;
	String sURL_text;
	String sURL_description;

    /**
     * Create a new <code>DifHandler</code>.
     * @param id The Entry_ID of the Dif.
     */
    public DifHandler() {
	relatedURL = contentType = "";
	insideSummary = insideURL = insideContentType = insideTitle
	    = insideID = insideRole = insideFirstName = insideMiddleName
	    = insideLastName = insideEmail = insidePhone = insideFax
	    = insideAddress = insideSouthernmost = insideNorthernmost
            = insideWesternmost = insideEasternmost //= insideParameters
	    = insideCategory = insideTopic = insideTerm = insideVariable
	    = insideStartDate = insideStopDate = insideLatResolution
	    = insideLongResolution = insideTemporalResolution
	    = false;


	dif = new Dif();
	difs = new Vector();
	summary = "";

	role = "";
	name = "";
	email = "";
	phone = "";
	fax = "";
	address = "";

	southernmost = "";
	northernmost = "";
	westernmost = "";
	easternmost = "";

	category = "";
	topic = "";
	term = "";
	variable = "";

	startDate = "";
	stopDate = "";

	latResolution = "";
	longResolution = "";
	temporalResolution = "";
    }
    public void startElement(String namespaceURI,
			     String lName, // local name
			     String qName, // qualified name
			     Attributes attrs)
	throws SAXException
    {
	insideID = false;
	insideURL = false;
	insideContentType = false;
	insideTitle = false;
	insideSummary = false;

	insideSummary = insideURL = insideContentType = insideTitle
	    = insideID = insideRole = insideFirstName = insideMiddleName
	    = insideLastName = insideEmail = insidePhone = insideFax
	    = insideAddress = insideSouthernmost = insideNorthernmost
            = insideWesternmost = insideEasternmost //= insideParameters
	    = insideCategory = insideTopic = insideTerm = insideVariable
	    = insideStartDate = insideStopDate = insideLatResolution
	    = insideLongResolution = insideTemporalResolution
	    = false;

	//System.out.println("start: qName = " + qName);
	if(qName.equals("DIF")){
//System.out.println("created new dif");
	    dif = new Dif();
	} else {
//System.out.println("received qName: " + qName);

		if( qName.equals("Related_URL") ){
			miCurrentState = this.STATE_InRelatedURL;
		} else if( qName.equals("URL_Content_Type") ){
			miCurrentState = STATE_InContentType;
		} else if(qName.equals("Type")){
			if( miCurrentState == this.STATE_InContentType ){
				miCurrentState = this.STATE_InType;
			} else {
				miCurrentState = this.STATE_Undefined;
			}
		} else if( qName.equals("URL") ){
			if( miCurrentState == this.STATE_InRelatedURL ){
				miCurrentState = this.STATE_InURL;
			} else {
				miCurrentState = this.STATE_Undefined;
			}
		} else if( qName.equals("Description") ){
			if( miCurrentState == this.STATE_InRelatedURL ){
				miCurrentState = this.STATE_InURLDescription;
			} else {
				miCurrentState = this.STATE_Undefined;
			}
		}

		if(qName.equals("Entry_ID"))
			insideID = true;
		else if(qName.equals("Entry_Title"))
			insideTitle = true;
		else if(qName.equals("URL"))
			insideURL = true;
		else if(qName.equals("URL_Content_Type"))
			insideContentType = true;
		else if(qName.equals("Summary"))
			insideSummary = true;
		else if(qName.equals("Role"))
			insideRole = true;
		else if(qName.equals("First_Name"))
			insideFirstName = true;
		else if(qName.equals("Middle_Name"))
			insideMiddleName = true;
		else if(qName.equals("Last_Name"))
			insideLastName = true;
		else if(qName.equals("Email"))
			insideEmail = true;
		else if(qName.equals("Phone"))
			insidePhone = true;
		else if(qName.equals("Fax"))
			insideFax = true;
		else if(qName.equals("Address"))
			insideAddress = true;
		else if(qName.equals("Southernmost_Latitude"))
			insideSouthernmost = true;
		else if(qName.equals("Northernmost_Latitude"))
			insideNorthernmost = true;
		else if(qName.equals("Westernmost_Longitude"))
			insideWesternmost = true;
		else if(qName.equals("Easternmost_Longitude"))
			insideEasternmost = true;
		else if(qName.equals("Category"))
			insideCategory = true;
		else if(qName.equals("Topic"))
			insideTopic = true;
		else if(qName.equals("Term"))
			insideTerm = true;
		else if(qName.equals("Variable"))
			insideVariable = true;
		else if(qName.equals("Start_Date"))
			insideStartDate = true;
		else if(qName.equals("Stop_Date"))
			insideStopDate = true;
		else if(qName.equals("Latitude_Resolution"))
			insideLatResolution = true;
		else if(qName.equals("Longitude_Resolution"))
			insideLongResolution = true;
		else if(qName.equals("Temporal_Resolution"))
			insideTemporalResolution = true;
		}
    }

    public void endElement(String namespaceURI,
			   String sName, // simple name
			   String qName  // qualified name
			   )
	throws SAXException
    {
	insideURL = false;
	insideContentType = false;
	insideTitle = false;
	insideSummary = false;
	insideID = false;
	insideSummary = insideURL = insideContentType = insideTitle
	    = insideID = insideRole = insideFirstName = insideMiddleName
	    = insideLastName = insideEmail = insidePhone = insideFax
	    = insideAddress = insideSouthernmost = insideNorthernmost
            = insideWesternmost = insideEasternmost //= insideParameters
	    = insideCategory = insideTopic = insideTerm = insideVariable
	    = insideStartDate = insideStopDate = insideLatResolution
	    = insideLongResolution = insideTemporalResolution
	    = false;

	switch( miCurrentState ){
		case STATE_InContentType:
			miCurrentState = this.STATE_InRelatedURL;
			break;
		case STATE_InDataURL:
			miCurrentState = this.STATE_InRelatedURL;
			break;
		case STATE_InRelatedInformation:
			miCurrentState = this.STATE_InRelatedURL;
			break;
		case STATE_InRelatedURL:
			miCurrentState = this.STATE_Undefined;
			break;
		case STATE_InType:
			miCurrentState = this.STATE_InContentType;
			break;
		case STATE_InURL:
			miCurrentState = this.STATE_InRelatedURL;
			break;
		case STATE_InURLDescription:
			miCurrentState = this.STATE_InRelatedURL;
			break;
	}

	if(qName.equals("DIF"))
	    difs.addElement(dif);
	else if(qName.equals("Related_URL")) {
//		System.out.println("state is " + miCurrentState + " adding url: " + sURL_text + " " + sURL_type);
	    dif.addRelatedURL( sURL_text, sURL_type );
	    relatedURL = contentType = "";
		sURL_type = "";
		sURL_text = "";
		sURL_description = "";
		miCurrentState = this.STATE_Undefined;
	}
	else if(qName.equals("Summary")) {
	    dif.setSummary(summary);
	    summary = "";
	}
	else if(qName.equals("Personnel")){
	    dif.addContactInfo(role, name, email, phone, fax, address);
	    role = "";
	    name = "";
	    email = "";
	    phone = "";
	    fax = "";
	    address = "";
	}
	else if(qName.equals("Spatial_Coverage")){
	    dif.setSpatialCoverage(southernmost, northernmost, westernmost, easternmost);
	    southernmost = "";
	    northernmost = "";
	    westernmost = "";
	    easternmost = "";
	}
	else if(qName.equals("Temporal_Coverage")){
	    dif.setTemporalCoverage(startDate, stopDate);
	    startDate = stopDate = "";
	}
	else if(qName.equals("Parameters")) {
	    dif.addParameters(category, topic, term, variable);
	    category = topic = term = variable = "";
	}
	else if(qName.equals("Data_Resolution")) {
	    dif.setDataResolution(latResolution, longResolution, temporalResolution);
	    latResolution = longResolution = temporalResolution = "";
	}

    }

    public void characters(char buf[], int offset, int len)
	throws SAXException
    {
	String s = new String(buf, offset, len);
	//System.out.println("buf " + buf + ", offset " + offset + ", len" + len);
	if( miCurrentState == this.STATE_InType ){
		sURL_type = s;
	} else if( miCurrentState == this.STATE_InURL ) {
		sURL_text = s;
	} else if( miCurrentState == this.STATE_InURLDescription ) {
		sURL_description = s;
	}

	if(insideTitle)
	    dif.setTitle(s);
	else if(insideID)
	    dif.setID(s);
	else if(insideURL) {
	    relatedURL = s;
	}
	else if(insideContentType) {
	    contentType = s;
	}
	else if(insideSummary) {
	    summary = summary + s;
	}
	else if(insideRole) {
	    role = s;
	}
	else if(insideFirstName) {
	    name = s + " " + name;
	}
	else if(insideMiddleName) {
	    name = name + " " + s;
	}
	else if(insideLastName) {
	    name = name + " " + s;
	}
	else if(insideEmail) {
	    email = s;
	}
	else if(insidePhone) {
	    phone = s;
	}
	else if(insideFax) {
	    fax = s;
	}
	else if(insideAddress) {
	    address = s;
	    //ystem.out.println("s " + s + ", address " + address);
	}
	else if(insideSouthernmost) {
	    southernmost = s;
	}
	else if(insideNorthernmost) {
	    northernmost = s;
	}
	else if(insideWesternmost) {
	    westernmost = s;
	}
	else if(insideEasternmost) {
	    easternmost = s;
	}
	else if(insideCategory) {
	    category = s;
	}
	else if(insideTopic) {
	    topic = s;
	}
	else if(insideTerm) {
	    term = s;
	}
	else if(insideVariable) {
	    variable = s;
	}
	else if(insideStartDate) {
	    startDate = s;
	}
	else if(insideStopDate) {
	    stopDate = s;
	}
	else if(insideLatResolution) {
	    latResolution = s;
	}
	else if(insideLongResolution) {
	    longResolution = s;
	}
	else if(insideTemporalResolution) {
	    temporalResolution = s;
	}
    }

    public Vector getDifs() {
	return difs;
    }
}






