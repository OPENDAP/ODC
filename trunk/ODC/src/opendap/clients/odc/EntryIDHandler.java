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

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import java.util.Vector;

/**
 * This class is used by the SAX parser to turn an XML file containing
 * <Entry_ID> tags into a Vector of Strings representing the IDs.
 */
public class EntryIDHandler extends DefaultHandler {

    private boolean insideEntryID;
    private Vector ids;

    public EntryIDHandler() {
	ids = new Vector();
	insideEntryID = false;
    }

    //
    // Default Handler Functions
    //
    public void startElement(String namespaceURI,
			     String lName, // local name
			     String qName, // qualified name
			     Attributes attrs)
	throws SAXException
    {
	if(lName.equals("Entry_ID"))
	    insideEntryID = true;
	else
	    insideEntryID = false;

    }

    public void endElement(String namespaceURI,
			   String sName, // simple name
			   String qName  // qualified name
			   )
	throws SAXException
    {
	insideEntryID = false;
    }

    public void characters(char buf[], int offset, int len)
	throws SAXException
    {
	if(insideEntryID) {
	    String s = new String(buf, offset, len);
	    ids.addElement(s);
	}
    }

    //
    // Other Functions
    //

    /**
     * Returns the Vector of IDs.
     * @return the Vector of IDs.
     */
    public Vector getIDs() {
	return ids;
    }

}



