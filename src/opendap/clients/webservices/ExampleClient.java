package opendap.clients.webservices;

import java.net.*;
import java.util.*;
import org.apache.soap.*;
import org.apache.soap.rpc.*;

public class ExampleClient {

	/** supply host name like "myhost.org" to go to another machine instead of the local */
	public static void main(String[] args) throws Exception {
		String sHost = "127.0.0.1:8080"; // default host
		if( args.length > 0 ) sHost = args[0]; // user can supply hgst if desired
		String sWebServicesURL = "http://" + sHost + "/soap/servlet/rpcrouter"; // default url
		URL url = null;
		try {
			url = new URL(sWebServicesURL);
		} catch(Exception ex) {
			System.out.println("error evaluating web services URL [" + sWebServicesURL + "]: " + ex);
			System.exit(1);
		}
		System.out.println("testing web services for URL: " + sWebServicesURL);
		org.apache.soap.rpc.Call call = new Call();
		call.setTargetObjectURI("urn:opendap-services");
		call.setEncodingStyleURI(Constants.NS_URI_SOAP_ENC);
		System.out.println("version: " + getVersion(call, url));
		System.out.println("example blob url for http://" + sHost + "/dods/dts/D1: \n" + getDataURL(call, url, "http://" + sHost + "/dods/dts/D1", ""));
		System.out.println("example ddx for http://" + sHost + "/dods/dts/D1: \n" + getDDX(call, url, "http://" + sHost + "/dods/dts/D1"));
	}

	public static String getVersion( org.apache.soap.rpc.Call call, java.net.URL url ){
		call.setMethodName("serviceVersion");
        System.out.println("\n--  --  --  --  --  --  --  --  --  --  --  --  --");
        System.out.println("Soap Call:\n"+call+"\n");

        Envelope env = call.buildEnvelope();

        System.out.println("\nEnvelope:\n"+env+"\n");
		return getResponseString( call, url );
	}

	public static String getDDX(  org.apache.soap.rpc.Call call, java.net.URL url, String sOPeNDAPurl ){
		call.setMethodName("serviceDDX");
		Vector params = new Vector();
		params.addElement(new Parameter("sDodsURL", String.class, sOPeNDAPurl, null));
		call.setParams (params);
        System.out.println("\n--  --  --  --  --  --  --  --  --  --  --  --  --");
        System.out.println("Soap Call:\n"+call+"\n");
        Envelope env = call.buildEnvelope();

        System.out.println("\nEnvelope:\n"+env+"\n");
		return getResponseString(call, url);
	}

	public static String getDataURL(  org.apache.soap.rpc.Call call, java.net.URL url, String sOPeNDAPurl, String sConstraint ){
		call.setMethodName("serviceDataURL");
		Vector params = new Vector();
		params.addElement(new Parameter("sDodsURL", String.class, sOPeNDAPurl, null));
		params.addElement(new Parameter("sConstraint", String.class, sConstraint, null));
		call.setParams (params);
        System.out.println("\n--  --  --  --  --  --  --  --  --  --  --  --  --");
        System.out.println("Soap Call:\n"+call+"\n");
        Envelope env = call.buildEnvelope();

        System.out.println("\nEnvelope:\n"+env+"\n");
		return getResponseString(call, url);
	}

	public static String getResponseString( org.apache.soap.rpc.Call call, java.net.URL url ){

		Response response = null;
		try {
			response = call.invoke(url, "" );
		} catch(Exception ex) {
			System.out.println("error invoking service call: " + ex);
		}

		if( response == null ){
			System.out.println("no response from server");
			return null;
		}

		if( response.generatedFault() ) {
			Fault fault = response.getFault ();
			StringBuffer sbError = new StringBuffer();
			sbError.append("Error: " + fault.getFaultCode() + " " + fault.getFaultString());
			System.out.println(sbError.toString());
			return null;
		} else {
			Parameter result = response.getReturnValue();
			return (String)result.getValue();
		}
	}
}


