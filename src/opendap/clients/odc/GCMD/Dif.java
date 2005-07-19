package opendap.clients.odc.GCMD;

import opendap.clients.odc.*;
import java.lang.*;
import java.util.*;
/**
 * A <code>Dif</code> represents an xml Dif element, which in turn holds
 * the information about a dataset in the GCMD database.  At this point
 * in time, it only holds information for a few of the fields in the dif,
 * but more can be added as they become needed.
 *
 * @author rhonhart
 *
 * @modified Sheila (zhifang) Jiang more fields are added
 *
 */
public class Dif {

    Vector urls;
    Vector contentTypes;
    Vector personnels;
    Vector parameters;

    String entryTitle;
    String entryID;
    String summary;

    SpatialCoverage spatialCoverage;
    TemporalCoverage temporalCoverage;
    DataResolution dataResolution;

    public Dif() {
	entryID = "";
	urls = new Vector();
	contentTypes = new Vector();
	personnels = new Vector();
	parameters = new Vector();
	summary = "";
	spatialCoverage = new SpatialCoverage();
	temporalCoverage = new TemporalCoverage();
	dataResolution = new DataResolution();
    }

    /**
     * Create a <code>Dif</code> with the unique ID <code>id</code>
     *
     * @param id The Entry_ID of the Dif
     */
    public Dif(String id) {
		entryID = id;
		urls = new Vector();
		contentTypes = new Vector();
		personnels = new Vector();
		parameters = new Vector();
		summary = "";
		spatialCoverage = new SpatialCoverage();
		temporalCoverage = new TemporalCoverage();
		dataResolution = new DataResolution();
    }

    public void setID(String id) {
		entryID = id;
    }

    /**
     * Set the title of the Dif
     *
     * @param title The Entry_Title of the Dif
     */
    public void setTitle(String title) {
		entryTitle = title;
    }

    /**
     * Set the summary information for the Dif.
     *
     * @param text The information from the <Summary> tag of the Dif
     */
    public void setSummary(String text) {
		summary = text;
    }

    /**
     * Add the contact information from a <Personnel> tag.
     *
     * @param role
     * @param name
     * @param email
     * @param phone
     * @param fax
     * @param address
     */
    public void addContactInfo(String role, String name, String email, String phone, String fax, String address) {
		personnels.addElement(new Personnel(role, name, email, phone, fax, address));
    }

    /**
     * Add the spatial coverage information from a <Spatial_Coverage> tag.
     *
     * @param southernmost
     * @param northernmost
     * @param westernmost
     * @param easternmost
     */
    public void setSpatialCoverage(String southernmost, String northernmost, String westernmost, String easternmost) {
		spatialCoverage = new SpatialCoverage(southernmost, northernmost, westernmost, easternmost);
    }

    /**
     * Add the temporal coverage information from a <Temporal_Coverage> tag.
     *
     * @param startDate
     * @param stopDate
     */
    public void setTemporalCoverage(String startDate, String stopDate) {
		temporalCoverage = new TemporalCoverage(startDate, stopDate);
    }

    /**
     * Add the data resolution information from a <Data_Resolution> tag.
     *
     * @param latResolution
     * @param longResolution
     * @param temporalResolution
     */
    public void setDataResolution(String latRes, String longRes, String tempRes)
	{
	    dataResolution = new DataResolution(latRes, longRes, tempRes);
	}


    /**
     * Add the information from a <RelatedURL> tag.
     *
     * @param url The URL
     * @param contentType The type of URL, usually from the <ContentType>
     *                    tag.
     */
    public void addRelatedURL(String url, String contentType) {
		urls.addElement(url);
		contentTypes.addElement(contentType);
    }

    /**
     * Add a <Parameter> object.
     *
     * @param cat The category
     * @param top The topic
     * @param ter The term
     * @param var The variable
     */
    public void addParameters(String cat, String top, String ter, String var) {
		parameters.addElement(new Parameters(cat, top, ter, var));
    }

   /**
     * returns the ID of the Dif
     * @return the ID of the Dif
     */
    public String getID() {
		return entryID;
    }

    /**
     * returns the title of the Dif
     * @return the title of the Dif
     */
    public String getTitle() {
		return entryTitle;
    }

    public String getDodsURL() {
		 StringBuffer sbError = new StringBuffer();
		 DodsURL url = getDodsURL(sbError);
		 if( url == null ) return "[" + sbError.toString() + "]";
		 return url.getFullURL();
	}

    /**
     * Go through the internal vector of related URLs and pick a URL
     * to return as the DODS URL.
     *
     * @return the Dods URL of the dataset
     */
    public DodsURL getDodsURL( StringBuffer sbError ) {
		String url = "";
		DodsURL dodsURL;
		int index = 1;

		//for(int i=0;i<contentTypes.size();i++) {
		//    System.out.println(contentTypes.elementAt(i));
		//}

// file servers no longer supported
//		if( (index = contentTypes.indexOf("DODS_FILESERVER")) != -1) {
//			url = (String)urls.elementAt(index);
//			dodsURL = new DodsURL(url, DodsURL.CATALOG_URL);
//		} else
	    if ( (index = contentTypes.indexOf("DODS_URL")) != -1) {
			url = (String)urls.elementAt(index);
			dodsURL = new DodsURL(url, DodsURL.TYPE_Data);
		}

		// DODS_INFO and DODS_HTML both point to the dods dataset, but
		// with the .info and .html extensions added respectively.
		// If we strip those of, we get the url we need.
		else if( (index = contentTypes.indexOf("DODS_HTML")) != -1) {
			url = (String)urls.elementAt(index);
			url = url.substring(0,url.length() - 5);
			dodsURL = new DodsURL(url, DodsURL.TYPE_Data);
		} else if( (index = contentTypes.indexOf("DODS_INFO")) != -1) {
			url = (String)urls.elementAt(index);
			url = url.substring(0,url.length() - 5);
			dodsURL = new DodsURL(url, DodsURL.TYPE_Data);
		} else if( (index = contentTypes.indexOf("DODS_DIR")) != -1) {
			url = (String)urls.elementAt(index);
			dodsURL = new DodsURL(url, DodsURL.TYPE_Directory);
		}

		// Often, the DODS_SITEINFO url can become the dods url by adding
		// dods to the end of it.  This is only the case if the url ends
		// with a / though.
		else if( (index = contentTypes.indexOf("DODS_SITEINFO")) != -1
				&& ((String)urls.elementAt(index)).endsWith("/"))
		{
			url = (String)urls.elementAt(index);
			url += "dods";
			dodsURL = new DodsURL(url, DodsURL.TYPE_Data);
		} else {
			sbError.append("Unable to form URL; no recognized content type for entry id " + this.getID() + " " + this.getTitle());
			return null;
		}

		dodsURL.setTitle(entryTitle);
		return dodsURL;
    }

    /**
     * returns the summary information for a Dif
     * @return the summary information for a Dif
     */
    public String getSummary() {
		return summary;
    }

    /**
     * returns the contact information for a Dif
     * @return the contact information for a Dif
     */
    public Vector getPersonnels() {
		return personnels;
    }

    /**
     * returns the role for a Personnel
     * @return the role for a Personnel
     */
    public String getRole(int i) {
		return ((Personnel)personnels.elementAt(i)).getRole();
    }

    /**
     * returns the name for a Personnel
     * @return the name for a Personnel
     */
    public String getName(int i) {
		return ((Personnel)personnels.elementAt(i)).getName();
    }

    /**
     * returns the email for a Personnel
     * @return the email for a Personnel
     */
    public String getEmail(int i) {
		return ((Personnel)personnels.elementAt(i)).getEmail();
    }

    /**
     * returns the phone for a Personnel
     * @return the phone for a Personnel
     */
    public String getPhone(int i) {
		return ((Personnel)personnels.elementAt(i)).getPhone();
    }

    /**
     * returns the fax for a Personnel
     * @return the fax for a Personnel
     */
    public String getFax(int i) {
		return ((Personnel)personnels.elementAt(i)).getFax();
    }

    /**
     * returns the address for a Personnel
     * @return the address for a Personnel
     */
    public String getAddress(int i) {
		return ((Personnel)personnels.elementAt(i)).getAddress();
    }

    /**
     * returns the spatial coverage information for a Dif
     * @return the spatial coverage information for a Dif
     */
    public SpatialCoverage getSpatialCoverage() {
		return spatialCoverage;
    }

    /**
     * returns the southernmost latitude
     * @return the southernmost latitude
     */
    public String getSouthernmost() {
		return spatialCoverage.getSouthernmost();
    }

    /**
     * returns the northernmost latitude
     * @return the northernmost latitude
     */
    public String getNorthernmost() {
		return spatialCoverage.getNorthernmost();
    }

    /**
     * returns the westernmost longitude
     * @return the westernmost longitude
     */
    public String getWesternmost() {
		return spatialCoverage.getWesternmost();
    }

     /**
     * returns the easternmost longitude
     * @return the easternmost longitude
     */
    public String getEasternmost() {
		return spatialCoverage.getEasternmost();
    }

    /**
     * returns the parameters for a Dif
     * @return the parameters for a Dif
     */
    public Vector getParameters() {
		return parameters;
    }

    /**
     * returns the category for a Parameter set
     * @return the category for a Parameter set
     */
    public String getCategory(int i) {
		return ((Parameters)parameters.elementAt(i)).getCategory();
    }

    /**
     * returns the topic for a Parameter set
     * @return the topic for a Parameter set
     */
    public String getTopic(int i) {
		return ((Parameters)parameters.elementAt(i)).getTopic();
    }

    /**
     * returns the term for a Parameter set
     * @return the term for a Parameter set
     */
    public String getTerm(int i) {
		return ((Parameters)parameters.elementAt(i)).getTerm();
    }

    /**
     * returns the variable for a Parameter set
     * @return the variable for a Parameter set
     */
    public String getVariable(int i) {
		return ((Parameters)parameters.elementAt(i)).getVariable();
    }

    /**
     * returns the temporal coverage for a Dif
     * @return the temporal coverage for a Dif
     */
    public TemporalCoverage getTemporalCoverage() {
		return temporalCoverage;
    }

    /**
     * returns the start date
     * @return the start date
     */
    public String getStartDate() {
		return temporalCoverage.getStartDate();
    }

    /**
     * returns the stop date
     * @return the stop date
     */
    public String getStopDate() {
		return temporalCoverage.getStopDate();
    }

    /**
     * returns the data resolution
     * @return the data resolution
     */
    public DataResolution getDataResolution() {
		return dataResolution;
    }

    /**
     * returns the latitude resolution
     * @return the latitude resolution
     */
    public String getLatResolution() {
		return dataResolution.getLatResolution();
    }

    /**
     * returns the longitude resolution
     * @return the longitude resolution
     */
    public String getLongResolution() {
		return dataResolution.getLongResolution();
    }

    /**
     * returns the temporal resolution
     * @return the temporal resolution
     */
    public String getTemporalResolution() {
		return dataResolution.getTemporalResolution();
    }


    public String toString() {
		return getTitle();
    }

    /*
     * A <code>Parameters</code> represents an xml Parameters element, which in
     * turn holds the information about a parameter set in a Dif element.
     *
     * @author Zhifang Jiang
     */
    public class Parameters {
	private String category;
	private String topic;
	private String term;
	private String variable;

	public Parameters() {
	    category = "";
	    topic = "";
	    term = "";
	    variable = "";
	}

	public Parameters(String cat, String top, String ter, String var) {
	    category = cat;
	    topic = top;
	    term = ter;
	    variable = var;
	}

	public String getCategory() {
	    return category;
	}

	public String getTopic() {
	    return topic;
	}

	public String getTerm() {
	    return term;
	}

	public String getVariable() {
	    return variable;
	}
    }

    /*
     * A <code>Personnel</code> represents an xml Personnel element, which in
     * turn holds the information about a set of contact information in a Dif
     * element.
     *
     * @author Zhifang Jiang
     */
    public class Personnel {
		private String role;
		private String name;
		private String email;
		private String phone;
		private String fax;
		private String address;

		public Personnel() {
			role = "";
			name = "";
			email = "";
			phone = "";
			fax = "";
			address = "";
		}

		public Personnel(String role, String name, String email, String phone, String fax, String address) {
			this.role = role;
			this.name = name;
			this.email = email;
			this.phone = phone;
			this.fax = fax;
			this.address = address;
		}

		public String getRole() {
			return role;
		}

		public String getName() {
			return name;
		}

		public String getEmail() {
			return email;
		}

		public String getPhone() {
			return phone;
		}

		public String getFax() {
			return fax;
		}

		public String getAddress() {
			return address;
		}
    }

    /*
     * A <code>SpatialCoverage</code> represents an xml SpatialCoverage
     * element, which in turn holds the information about a set of spatial
     * coverage information in a Dif element.
     *
     * @author Zhifang Jiang
     */
    public class SpatialCoverage {
		private String southernmost;
		private String northernmost;
		private String westernmost;
		private String easternmost;

		public SpatialCoverage() {
			southernmost = "";
			northernmost = "";
			westernmost = "";
			easternmost = "";
		}

		public SpatialCoverage(String southernmost, String northernmost, String westernmost, String easternmost) {
			this.southernmost = southernmost;
			this.northernmost = northernmost;
			this.westernmost = westernmost;
			this.easternmost = easternmost;
		}

		public String getSouthernmost() {
			return southernmost;
		}

		public String getNorthernmost() {
			return northernmost;
		}

		public String getWesternmost() {
			return westernmost;
		}

		public String getEasternmost() {
			return easternmost;
		}
	}

	/*
	* A <code>TemporalCoverage</code> represents an xml TemporalCoverage
	* element, which in turn holds the information about a set of temporal
	* coverage information in a Dif element.
	*
	* @author Zhifang Jiang
	 */
	public class TemporalCoverage {
		private String startDate;
		private String stopDate;

		public TemporalCoverage() {
			startDate = "";
			stopDate = "";
		}

		public TemporalCoverage(String start, String stop) {
			startDate = start;
			stopDate = stop;
		}

		public String getStartDate() {
			return startDate;
		}

		public String getStopDate() {
			return stopDate;
		}
    }

     /*
     * A <code>DataResolution</code> represents an xml Data_Resolution
     * element, which in turn holds the information about data resolution
     * information in a Dif element.
     *
     * @author Zhifang Jiang
     */
    public class DataResolution {
		private String latResolution;
		private String longResolution;
		private String temporalResolution;

		public DataResolution () {
			latResolution = "";
			longResolution = "";
			temporalResolution = "";
		}

		public DataResolution (String latRes, String longRes, String tempRes)
		{
			latResolution = latRes;
			longResolution = longRes;
			temporalResolution = tempRes;
		}

		public String getLatResolution() {
			return latResolution;
		}

		public String getLongResolution() {
			return longResolution;
		}

		public String getTemporalResolution() {
			return temporalResolution;
		}
    }

	String getGeneralInfo(){
		StringBuffer sbInfo = new StringBuffer(500);

		sbInfo.append(this.getTitle() + "\n");
		sbInfo.append(this.getDodsURL() + "\n");

		if(getSummary().equals("")) {
			sbInfo.append( "No Summary Infomation Available" );
		} else {
			sbInfo.append( "Summary\n" + getSummary() );
		}

		if(getParameters() == null) {
			sbInfo.append( "No Parameters Available" );
		} else {
			sbInfo.append( "\n\nParameters\n" );
			for(int i=0; i<getParameters().size(); i++) {
				sbInfo.append( "\n" );
				sbInfo.append( getCategory(i) + " > "
					+ getTopic(i) + " > " + getTerm(i) + " > "
					+ getVariable(i) );
			}
		}

		if(getSpatialCoverage() == null) {
			//Dif temp = getDif(getID(), "Summary");
			//setSummary(temp.getSummary());
			//if(getSummary().equals(""))
			sbInfo.append( "No Spatial Coverage Infomation Available" );
		} else {
			sbInfo.append( "\n\nSpatial Coverage" );
			sbInfo.append( "\n\nSouthernmost Latitude: " );
			sbInfo.append( getSouthernmost() );
			sbInfo.append( "\nNorthernmost Latitude: " );
			sbInfo.append( getNorthernmost() );
			sbInfo.append( "\nWesternmost Longitude: " );
			sbInfo.append( getWesternmost() );
			sbInfo.append( "\nEasternmost Longitude: " );
			sbInfo.append( getEasternmost() );
		}

		if(getTemporalCoverage() == null) {
			//Dif temp = getDif(getID(), "Summary");
			//setSummary(temp.getSummary());
			//if(getSummary().equals(""))
			sbInfo.append( "No Temporal Coverage Infomation Available" );
		} else {
			sbInfo.append( "\n\nTemporal Coverage" );
			sbInfo.append( "\n\nStart Date: " );
			sbInfo.append( getStartDate() );
			sbInfo.append( "\nStop Date: " );
			sbInfo.append( getStopDate() );

		}

		if(getDataResolution() == null) {
			//Dif temp = getDif(getID(), "Summary");
			//setSummary(temp.getSummary());
			//if(getSummary().equals(""))
			sbInfo.append( "No Data Resolution Infomation Available" );
		} else {
			sbInfo.append( "\n\nData Resolution" );
			sbInfo.append( "\n\nLatitude Resolution: " );
			sbInfo.append( getLatResolution() );
			sbInfo.append( "\nLongitude Resolution: " );
			sbInfo.append( getLongResolution() );
			sbInfo.append( "\nTemporal Resolution: " );
			sbInfo.append( getTemporalResolution() );
		}

		if(getPersonnels() == null) {
			//Dif temp = getDif(getID(), "Summary");
			//setSummary(temp.getSummary());
			//if(getSummary().equals(""))
			sbInfo.append( "No Contact Infomation Available" );
		} else {
			sbInfo.append( "Contact Information" );
			for(int i=0; i<getPersonnels().size(); i++) {
				sbInfo.append( "\n\nRole   " );
				sbInfo.append( getRole(i) );
				sbInfo.append( "\nName   " );
				sbInfo.append( getName(i) );
				sbInfo.append( "\nEmail   " );
				sbInfo.append( getEmail(i) );
				sbInfo.append( "\nPhone   " );
				sbInfo.append( getPhone(i) );
				sbInfo.append( "\nFax   " );
				sbInfo.append( getFax(i) );
				sbInfo.append( "\nAddress   " );
				sbInfo.append( getAddress(i) );
			}
		}
		return sbInfo.toString();
	}

}


