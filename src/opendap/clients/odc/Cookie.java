package opendap.clients.odc;

public class Cookie {
	public String sName;
	public String sValue;
	public String sExpires;
	public String sPath;
	public String getClientCookie(){ return sName + '=' + sValue; }
}

