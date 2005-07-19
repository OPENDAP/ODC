package opendap.clients.odc.ECHO;

import java.lang.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.input.DOMBuilder;
import gov.nasa.echo.soap.*;
import java.io.*;
import opendap.clients.odc.*;

/**
 * @author Sheila Jiang
 */

 public class SOAPMessenger
 {
     private Document outXMLDoc;

     public Document exeQuery(Document xmlDoc){

		 //convert xmlDoc to a string
		 XMLOutputter myXMLOutputter = new XMLOutputter();
		 String sXMLMessage = myXMLOutputter.outputString(xmlDoc);
		 System.out.println("request: " + sXMLMessage);

		 //wrap the xml up into SOAP and send it
		 try {
			 //create ECHO SOAP object
			 String sECHO_URL = ConfigurationManager.getInstance().getProperty_URL_ECHO();
			 String sProxyURL = Utility.sConnectPaths(sECHO_URL, "/", "soap/servlet/rpcrouter");
			 EchoSOAPProxy echoRef = new EchoSOAPProxy(sProxyURL); // uses gov.nasa.echo.soap

			 //perform XML transaction on ECHO
			 System.out.println("proxy url: " + sProxyURL);
			 String response = echoRef.perform(sXMLMessage);

			 //convert the response to a Document object
			 ByteArrayInputStream inputStream = new ByteArrayInputStream(response.getBytes());
			 DOMBuilder domBuilder = new DOMBuilder(false);
			 outXMLDoc = domBuilder.build(inputStream);
		 } catch(java.net.MalformedURLException ex){
			 opendap.clients.odc.ApplicationController.vShowError("Bad URL: " + ex.getMessage());
		 } catch(EchoSOAPException ex){
			 opendap.clients.odc.ApplicationController.vShowError("XML Transaction to ECHO failed: " + ex.getMessage());
		 } catch(JDOMException ex){
			 opendap.clients.odc.ApplicationController.vShowError("SOAP messenger XML file conversion to Document failed: " + ex.getMessage());
		 } catch( Exception ex) {
			 StringBuffer sbError = new StringBuffer("Error executing SOAP query: ");
			 Utility.vUnexpectedError(ex, sbError);
			 ApplicationController.vShowError(sbError.toString());
		 }
		 return outXMLDoc;
     }

 }


