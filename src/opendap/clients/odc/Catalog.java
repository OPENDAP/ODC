/**
 * Catalog.java
 *
 * 1.00 2001/8/16
 * 2.00 2002/9/10
 *
 */
package opendap.clients.odc;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import opendap.dap.*;

/**
 * This class provides the base structure for
 * the Inventory application.
 *
 * @version     1.00 16 Aug 2001
 * @author      Kashan A. Shaikh
 */
public class Catalog extends JPanel implements ActionListener {

    private CatalogInformation mCatalogInformation;

    private int lowYear,lowMonth,lowDay,highYear,highMonth,highDay;
    private String datasetName;
    private String[] varNames;
    private String[][] varContents;

    private Vector actionListeners;

    // String of URLs
	private String msExampleURL;
    private DodsURL[] URLs;

    private String msURL;

    // DAS objects
    opendap.dap.AttributeTable dods_global;

    // Title panel
    private JPanel titlePanel;

    // DateRange panel
    private DateRange dateRangePanel;

    // Variable panel
    private JPanel varPanel;

    // Gather URL" panel & button
    private JPanel gatherURLPanel;
    private JButton gatherURLButton;

    public Catalog() {}

	String getExampleFileFullURL(){
		return msExampleURL;
	}

	boolean zInitialize( DodsURL url, DDS dds, DAS das, StringBuffer sbError ){
		try {
			msURL = url.getBaseURL();
			actionListeners = new Vector();

			// Remove the constraint expression from the url
			int endOfURL = msURL.indexOf(".ascii?");
			if(endOfURL != -1) {
				msURL = msURL.substring(0, endOfURL);
			}

			mCatalogInformation = new CatalogInformation();
			if( !mCatalogInformation.zLoad(das, dds, sbError) ){
				sbError.insert(0, "Failed to load catalog: " + sbError);
				return false;
			}

			if( !zDrawGUI( mCatalogInformation, dds, sbError ) ){
				sbError.insert(0, "Failed to create interface: " + sbError);
				return false;
			}

			return true;
		} catch( Exception ex ){
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

    // Draw the GUI interface
    private boolean zDrawGUI( CatalogInformation info, DDS dds, StringBuffer sbError ){

		if (dds == null) {
			sbError.append("DDS is missing");
			return false;
		}

        if ( info.isFileserver() ) {

            // format the panel
            GridBagLayout gridbag = new GridBagLayout();
            setLayout(gridbag);
            GridBagConstraints c = new GridBagConstraints();
            setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
			setBorder(
		      BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Inventory Search"), this.getBorder())
			);
            // create dataset title
            datasetName = info.getDatasetName();
            if (datasetName == null) {
                datasetName = dds.getName();
            }
            JLabel title = new JLabel("Dataset name: "+datasetName);
            titlePanel = new JPanel();
            titlePanel.setBorder(BorderFactory.createCompoundBorder(
                                     BorderFactory.createEtchedBorder(),
                                     BorderFactory.createEmptyBorder(5,5,2,2)));
            titlePanel.add(title);
            c.gridy = 0;
            c.insets = new Insets(0,0,20,20);  //padding
            gridbag.setConstraints(titlePanel,c);
            c.gridy = 1;
            c.insets = new Insets(5,5,5,5);  //padding
            add(titlePanel);

            // create variable selection panels
            if (info.variablesExist()) {
                varNames = info.getVariableNames();
                varContents = info.getVariableContents();
                if (varNames != null) {
                    varPanel = new JPanel();
                    varPanel.setBorder(BorderFactory.createCompoundBorder(
                                           BorderFactory.createTitledBorder("Variable Constraints"),
                                           BorderFactory.createEmptyBorder(5,5,5,5)));
                    varPanel.setLayout(new FlowLayout());
                    c.anchor = GridBagConstraints.NORTH;
                    c.gridwidth = 1;
                    for (int i = 0; i < varNames.length; i++) {
                        Variable tvar = new Variable(varNames[i],varContents[i]);
                        varPanel.add(tvar);
                    }
                    gridbag.setConstraints(varPanel,c);
                    add(varPanel);
                    c.gridy += 1;
                }
            }

            // create date range selection panel
            if (info.timeExists()) {
                lowYear = info.getLowYear();
                lowMonth = info.getLowMonth();
                lowDay = info.getLowDay();
                highYear = info.getHighYear();
                highMonth = info.getHighMonth();
                highDay = info.getHighDay();

                int t_lowYear = getMin(lowYear,highYear);
                if (t_lowYear != lowYear) {
                    int t_lowMonth = highMonth;
                    int t_lowDay = highDay;
                    highYear = lowYear;
                    highMonth = lowMonth;
                    highDay = lowDay;
                    lowYear = t_lowYear; lowMonth = t_lowMonth; lowDay = t_lowDay;
                }

                if ( (lowYear == highYear) && (lowYear == 1) ) {
                    if ( (lowDay == highDay) ) {	// monthly dataset
                        dateRangePanel = new DateRange(lowMonth,highMonth);
                    }
                } else if ( (lowDay == highDay) ) { 	// multi-year monthly
                    dateRangePanel = new DateRange(lowYear,lowMonth,highYear,highMonth);
                } else { 	// yearly
                    dateRangePanel = new DateRange(lowYear,lowMonth,lowDay,highYear,highMonth,highDay);
                }

                c.gridx = 0;
                gridbag.setConstraints(dateRangePanel,c);
                c.gridy += 1;
                add(dateRangePanel);
            }

            // create "gather URL" button
            gatherURLButton = new JButton("Gather URLs");
            gatherURLButton.setVerticalTextPosition(AbstractButton.CENTER);
            gatherURLButton.setHorizontalTextPosition(AbstractButton.LEFT);
            gatherURLButton.setToolTipText("Gather the URLs that meet the specified constraints.");
            gatherURLButton.setActionCommand("gather");
            gatherURLButton.addActionListener(this);
            // create a panel to hold it
            gatherURLPanel = new JPanel();
            gatherURLPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
            gatherURLPanel.add(gatherURLButton);
            c.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(gatherURLPanel,c);
            // add it to the main panel
            add(gatherURLPanel);

			return true;
        } else {
            sbError.append("Not a file server");
			return false;
        }
    }

    public void actionPerformed(ActionEvent event) {
        final String sSubsetExpression = sGetSubsetExpression();
		if (sSubsetExpression.equals("")) {
			JLabel error = new JLabel("Error: negative date range! Please select again.");
			LayoutManager gridbag = getLayout();
			GridBagConstraints c = ((GridBagLayout)gridbag).getConstraints(gatherURLPanel);
			c.gridx = 0;
			c.gridy += 1;
			c.anchor = GridBagConstraints.WEST;
			((GridBagLayout)gridbag).setConstraints(error, c);
			add(error);
			validate();
		} else {
			Activity activityGetSubsetData = new Activity();
			final String sURL_final = msURL;
			Continuation_DoCancel con = new Continuation_DoCancel(){
				public void Do(){

					// get data DDS
					DataDDS data = null;
					try {
						opendap.dap.DConnect connection = new opendap.dap.DConnect(sURL_final);
						data = connection.getData(sSubsetExpression, null);
					} catch(Exception e) {
					    ApplicationController.getInstance().vShowError("Failed to make data connection: " + e);
						return;
					}
					if (data == null) {
					    ApplicationController.getInstance().vShowError("Data DDS unexpectedly null");
						return;
					}

					try {
						Enumeration tvar = data.getVariables();
						DSequence seq = (DSequence) tvar.nextElement();
						tvar = seq.getVariables();

						String urlName = ((DString) tvar.nextElement()).getName();
						String dateName = "";
						if (tvar.hasMoreElements()) { 	// get date field
							dateName = ((DString) tvar.nextElement()).getName();
						}

						Catalog.this.URLs = new DodsURL[seq.getRowCount()];
						for (int i = 0; i < seq.getRowCount(); i++) {
							String sURL = ((DString) seq.getVariable(i,urlName)).getValue();
							Catalog.this.URLs[i] = new DodsURL(sURL, DodsURL.TYPE_Data);
							if (dateName != "") {
								URLs[i].setTitle( ((DString) seq.getVariable(i,dateName)).getValue()
										+ " - " + datasetName + " - " + URLs[i].getBaseURL() );
							}
						}
					} catch(Exception ex) {
						StringBuffer sbError = new StringBuffer("While getting data variables: ");
						Utility.vUnexpectedError( ex, sbError );
					}
				}
				public void Cancel(){
				}
			};
			activityGetSubsetData.vDoActivity(con); // todo add ability to cancel

		}
    }

    // --- Form the constraint expression ---
    private String sGetSubsetExpression() {
        String ce = "?DODS_URL";
        String fields = "";
        String varChoose = "";
        String dateChoose = "";
        if (mCatalogInformation.getDateFields() != "") {
            fields = "," + mCatalogInformation.getDateFields();
        } else { 	// last hope
            fields = ",DODS_Date(" + mCatalogInformation.getSequenceName() + ")";
        }

        if (mCatalogInformation.variablesExist()) {
            for (int i = 0; i < mCatalogInformation.getNumVariables(); i++) {
                String[] selVars = ((Variable) varPanel.getComponent(i)).getSelectedItems();
                if (selVars.length > 0) {
                    varChoose += "&" + varNames[i] + "={";
                    for (int z = 0; z < selVars.length; z++) {
                        if (z != 0) { varChoose += ","; }
                        varChoose += "\"" + selVars[z] + "\"";
                    }
                    varChoose += "}";
                }
            }
        }
        if (mCatalogInformation.timeExists()) {
			//
			// Deal with negative date ranges
			//
			if (dateRangePanel.getLowYear() > dateRangePanel.getHighYear()
				|| (dateRangePanel.getLowYear() == dateRangePanel.getHighYear()
				&& (dateRangePanel.getLowMonth() > dateRangePanel.getHighMonth()
				|| (dateRangePanel.getLowMonth() == dateRangePanel.getHighMonth()
				&& dateRangePanel.getLowDay() > dateRangePanel.getHighDay()))))
			{
				//Set ce to empty string. This will cause an error message
				//in GUI with "gather" command.
				ce = "";
			} else { //if date range is not negative
				dateChoose += "&date(\""
                          + Integer.toString(dateRangePanel.getLowYear()) + "/"
                          + Integer.toString(dateRangePanel.getLowMonth()) + "/"
                          + Integer.toString(dateRangePanel.getLowDay()) + "\",\""
                          + Integer.toString(dateRangePanel.getHighYear()) + "/"
                          + Integer.toString(dateRangePanel.getHighMonth()) + "/"
                          + Integer.toString(dateRangePanel.getHighDay()) + "\")";
				ce += fields + varChoose + dateChoose;
			}
		}
        //ce += fields + varChoose + dateChoose;
        return ce;
    }

    // --- Returns true if this dataset is a fileserver ---
    public boolean isFileserver() {
        return mCatalogInformation.isFileserver();
    }

    // --- URL access method ---
    public DodsURL[] getURLs() {
        return URLs;
    }

    // --- Returns the minimum of two integers ---
    private int getMin(int v1, int v2) {
        if (v1 < v2) {
            return v1;
        } else {
            return v2;
        }
    }

    // --- Returns the maximum of two integers ---
    private int getMax(int v1, int v2) {
        if (v1 > v2) {
            return v1;
        } else {
            return v2;
        }
    }
}

class CatalogInformation {

	private int lowYear,lowMonth,lowDay,highYear,highMonth,highDay;
    private int numVars;
    private String datasetName;
    private String[] varNames;
    private String[][] varContents;
    private String sequenceName;
    private boolean mzIsFileServer;

	// DODS DAP objects
    private opendap.dap.DConnect connect;
    private opendap.dap.DAS mDAS;
    private opendap.dap.DDS mDDS;

    // DAS objects
    opendap.dap.AttributeTable dods_global;

    private String dateFields,dateFunction,showFields;
    private boolean showTime,showVars;

	public CatalogInformation() {}

	boolean zLoad( DAS das, DDS dds, StringBuffer sbError ){

		if( das == null ){
			sbError.append( "DAS missing" );
			return false;
		}

		if( dds == null ){
			sbError.append( "DDS missing" );
			return false;
		}

		mDAS = das;
		mDDS = dds;

	 	mzIsFileServer = false;

	 	sequenceName = "";
	 	showVars = false;
		showTime = true;

		lowYear = 0;
		lowMonth = 0;
		lowDay = 0;
		highYear = 0;
		highMonth = 0;
		highDay = 0;

		dateFields = "";
		dateFunction = "";
		showFields = "";

		numVars = 0;

		mzIsFileServer = zDetermineIfFileserver( sbError );

		if( sbError.length() > 0 ) return false;

		return zLoad(sbError);

	}

	// Returns true if this dataset is a fileserver
    public boolean isFileserver() {
    	return mzIsFileServer;
    }

	// Gather the dataset information
    private boolean zLoad( StringBuffer sbError ) {

		opendap.dap.AttributeTable dods_inventory = null;
		opendap.dap.Attribute attr = null;

        String temp = null;
		boolean dateSpecified = false;

		return false;

// eliminate old regex stuff

//		RE spaces = null;
//		RE quotes = null;
//		try {
//	    	spaces = new RE(" ");
//	    	quotes = new RE("\"");
//		} catch(Exception e) {}
//
// 		if ( isFileserver() ) {
//	    	// try to determine the dataset name
//	    	try {
//				attr = dods_global.getAttribute("DODS_Title");
//				if (attr != null) {
//					datasetName = quotes.substituteAll(attr.getValueAt(0),"");
//				}
//	    	} catch(Exception e) {
//			    sbError.append("DODS_TITLE not defined");
//				return false;
//		    }
//
//	    	// try to get the sequence name (if it contains a sequence)
//	    	try {
//	    		sequenceName = ((DSequence) mDDS.getVariables().nextElement()).getName();
//	    	} catch(Exception e) {}
//
//
//	    	// is the date range already specified?
//	    	try {
//				RE exp = new RE("\"?(\\d+)/(\\d+)/(\\d+)\"?");
//				REMatch m1;
//				// get the low date
//				attr = dods_global.getAttribute("DODS_StartDate");
//				if (attr != null) {
//					temp = quotes.substituteAll(attr.getValueAt(0),"");
//					m1 = exp.getMatch(temp);
//					lowYear = Integer.parseInt(m1.toString(1));
//					lowMonth = Integer.parseInt(m1.toString(2));
//					lowDay = Integer.parseInt(m1.toString(3));
//					dateSpecified = true;
//					showTime = true;
//				}
//
//				// get the high date
//				attr = dods_global.getAttribute("DODS_EndDate");
//				if (attr != null) {
//					temp = quotes.substituteAll(attr.getValueAt(0),"");
//					m1 = exp.getMatch(temp);
//					highYear = Integer.parseInt(m1.toString(1));
//					highMonth = Integer.parseInt(m1.toString(2));
//					highDay = Integer.parseInt(m1.toString(3));
//					dateSpecified = true;
//					showTime = true;
//				}
//	    	} catch(Exception ex) {
//			    sbError.append("DODS_StartDate/DODS_EndDate may not be defined: " + ex);
//				return false;
//		    }
//
//	    	// look for date specs
//	    	try {
//				dods_inventory = mDAS.getAttributeTable("DODS_Inventory");
//				if (dods_inventory != null) {
//					attr = dods_inventory.getAttribute("Inventory_DateFields");
//					if (attr != null) {
//						dateFields = quotes.substituteAll(attr.getValueAt(0),"");
//					}
//
//					attr = dods_inventory.getAttribute("Inventory_DateFunction");
//					if (attr != null) {
//						dateFunction = quotes.substituteAll(attr.getValueAt(0),"");
//					}
//				}
//	 	  	} catch(Exception ex) {
//			   sbError.append("Inventory date field/function may not be defined: " + ex);
//			   return false;
//			}
//
//
//	    	// do we want to use the date range selection?
//	    	try {
//				if (dods_inventory != null) {
//					attr = dods_inventory.getAttribute("Inventory_SelectTime");
//					if (attr != null) {
//						temp = quotes.substituteAll(attr.getValueAt(0),"");
//						if (temp.equals("false")) {
//							showTime = false;	// no date selection
//						} else {
//							showTime = true;
//						}
//					}
//				}
//	    	} catch(Exception ex) {
//				sbError.append("Inventory_SelectTime may not be defined: " + ex);
//				return false;
//			}
//
//	    	// are there date fields specified?
//	    	try {
//				if (dods_inventory != null) {
//					attr = dods_inventory.getAttribute("Inventory_DateFields");
//					if (attr != null) {
//						dateFields = quotes.substituteAll(attr.getValueAt(0),"");
//					}
//				}
//	    	} catch(Exception ex) {
//				sbError.append("Inventory_DateFields not defined: " + ex);
//				return false;
//			}
//
//	    	// is there a date function specified?
//	    	try {
//				if (dods_inventory != null) {
//					attr = dods_inventory.getAttribute("Inventory_DateFunction");
//					if (attr != null) {
//						dateFunction = quotes.substituteAll(attr.getValueAt(0),"");
//					}
//				}
//	    	} catch(Exception ex) {
//			    sbError.append("Inventory_DateFunction not defined: " + ex);
//				return false;
//			}
//
//
//	    	// determine which fields should be included
//	    	try {
//				if (dods_inventory != null) {
//					attr = dods_inventory.getAttribute("Inventory_ShowFields");
//					if (attr != null) {
//						showFields = quotes.substituteAll(attr.getValueAt(0),"");
//					}
//				}
//	    	} catch(Exception e) {
//				sbError.append("Inventory_ShowFields not defined");
//				return false;
//			}
//
//
//	    	// are there variables to be selected?
//	    	try {
//				if (dods_inventory != null) {
//					attr = dods_inventory.getAttribute("Inventory_SelectVars");
//					if (attr != null) {
//						showVars = true;
//						String val = attr.getValueAt(0);
//						val = spaces.substituteAll(val,"");
//						val = quotes.substituteAll(val,"");
//						RE exp = new RE(",?([^,.]*)");
//						REMatch[] matches = exp.getAllMatches(val);
//						varNames = new String[matches.length];
//						varContents = new String[matches.length][];
//						DataDDS data = null;
//						Enumeration tvars = null;
//						Hashtable thash = new Hashtable();
//						Vector tvect = new Vector();
//						DSequence seq = null;
//						numVars = matches.length;
//						for (int i=0; i < numVars; i++) {
//							varNames[i] = matches[i].toString(1);
//							data = connect.getData("?"+varNames[i],null);
//							tvars = data.getVariables();
//							seq = (DSequence) tvars.nextElement();
//							for (int j = 0; j < seq.getRowCount(); j++) {
//								String tmp = ((DString) seq.getVariable(j,varNames[i])).getValue();
//								if ( ! thash.contains(tmp) ) {
//									tvect.addElement(tmp);
//									thash.put(tmp,tmp);
//								}
//							}
//							varContents[i] = new String[tvect.size()];
//							for (int j = 0; j < tvect.size(); j++) {
//								varContents[i][j] = (String) tvect.elementAt(j);
//							}
//							thash.clear();
//							tvect.removeAllElements();
//						}
//					}
//				}
//			} catch(Exception ex) {
//				sbError.append("Inventory_SelectVars may not be defined: " + ex);
//				return false;
//			}
//
//
//	    	// Let's try to determine the date range
//	    	try {
//				if ( (showTime) && (! dateSpecified) ) {
//					if ( (dateFields != "") && (dateFunction != "") ) {
//						String CE = "?" + dateFields;
//						DataDDS data = null;
//						try {
//							data = connect.getData(CE,null);
//						} catch(Exception ex) {
//						    sbError.append("Error forming constraint for date/time: " + ex);
//							return false;
//						}
//						if (data != null) {
//							RE exp = new RE("(\\d+)/(\\d+)/(\\d+)");
//							REMatch m1;
//
//							Enumeration tvar = data.getVariables();
//							DSequence seq = (DSequence) tvar.nextElement();
//							tvar = seq.getVariables();
//
//							// get low date
//							String vnme = ((DString) tvar.nextElement()).getName();
//							temp = ((DString) seq.getVariable(0,vnme)).getValue();
//							m1 = exp.getMatch(temp);
//							lowYear = Integer.parseInt(m1.toString(1));
//							lowMonth = Integer.parseInt(m1.toString(2));
//							lowDay = Integer.parseInt(m1.toString(3));
//
//							// get high date
//							temp = ((DString) seq.getVariable(seq.getRowCount()-1,vnme)).getValue();
//							m1 = exp.getMatch(temp);
//							highYear = Integer.parseInt(m1.toString(1));
//							highMonth = Integer.parseInt(m1.toString(2));
//							highDay = Integer.parseInt(m1.toString(3));
//
//							if (seq.elementCount() > 1) {  // start/end date type
//								vnme = ((DString) tvar.nextElement()).getName();
//
//								// determine low date
//								temp = ((DString) seq.getVariable(0,vnme)).getValue();
//								m1 = exp.getMatch(temp);
//								lowYear = getMin( lowYear, Integer.parseInt(m1.toString(1)) );
//								lowMonth = getMin( lowMonth, Integer.parseInt(m1.toString(2)) );
//								lowDay = getMin( lowDay, Integer.parseInt(m1.toString(3)) );
//
//								// determine high date
//								temp = ((DString) seq.getVariable(seq.getRowCount()-1,vnme)).getValue();
//								m1 = exp.getMatch(temp);
//								highYear = getMax( highYear, Integer.parseInt(m1.toString(1)) );
//								highMonth = getMax( highMonth, Integer.parseInt(m1.toString(2)) );
//								highDay = getMax( highDay, Integer.parseInt(m1.toString(3)) );
//							}
//						}
//					} else { 		// ** last hope
//						String CE = "?DODS_Date(" + sequenceName + ")";
//						DataDDS data = null;
//						try {
//							data = connect.getData(CE,null);
//						} catch(Exception e) {
//						    showTime = false;
//						}
//						if (data != null) {
//							RE exp = new RE("(\\d+)/(\\d+)/(\\d+)");
//							REMatch m1;
//
//							Enumeration tvar = data.getVariables();
//							DSequence seq = (DSequence) tvar.nextElement();
//							tvar = seq.getVariables();
//
//							// get low date
//							String vnme = ((DString) tvar.nextElement()).getName();
//							temp = ((DString) seq.getVariable(0,vnme)).getValue();
//							m1 = exp.getMatch(temp);
//							lowYear = Integer.parseInt(m1.toString(1));
//							lowMonth = Integer.parseInt(m1.toString(2));
//							lowDay = Integer.parseInt(m1.toString(3));
//
//							// get high date
//							temp = ((DString) seq.getVariable(seq.getRowCount()-1,vnme)).getValue();
//							m1 = exp.getMatch(temp);
//							highYear = Integer.parseInt(m1.toString(1));
//							highMonth = Integer.parseInt(m1.toString(2));
//							highDay = Integer.parseInt(m1.toString(3));
//
//							showTime = true;
//						}
//					}
//				}
//	    	} catch(Exception e) {
//			    showTime = false;
//			}
//
//	    	if ((showTime != false)||(showVars != false)) {
//				mzIsFileServer = true;
//				return true;
//	    	} else {
//				mzIsFileServer = false;
//				return false;
//	   		}
//		} else {
//	    	return false;
//		}
    }


    // --- Returns the minimum of two integers ---
    private int getMin(int v1, int v2) {
    	if (v1 < v2) {
	    	return v1;
    	} else {
	    	return v2;
    	}
    }

    // --- Returns the maximum of two integers ---
    private int getMax(int v1, int v2) {
    	if (v1 > v2) {
	    	return v1;
    	} else {
	   		return v2;
    	}
    }

    private boolean zDetermineIfFileserver(StringBuffer sbError) {
    	opendap.dap.AttributeTable ff_global;
    	opendap.dap.Attribute attr = null;
    	String temp = null;

		return false;

//    	RE spaces = null;
//		RE quotes = null;
//		try {
//	    	spaces = new RE(" ");
//	    	quotes = new RE("\"");
//		} catch(Exception ex) {
//			sbError.append("unable to form regular expression: " + ex);
//			return false;
//		}
//
//    	try {
//	    	dods_global = mDAS.getAttributeTable("DODS_Global");
//	    	if (dods_global != null) {
//				attr = dods_global.getAttribute("DODS_Filetype");
//				if (attr == null) {
//		    		attr = dods_global.getAttribute("DODS_FileType");	// check for case
//				}
//				if (attr != null) {
//		    		temp = quotes.substituteAll(attr.getValueAt(0),"");
//		    		if (temp.toLowerCase().compareTo("fileserver") == 0) {
//						return true;
//		    		}
//				}
//	    	}
//
//			// *** this is a temporary solution todo
//			ff_global = mDAS.getAttributeTable("FF_GLOBAL");
//			if (ff_global == null) {
//				return false;
//			} else {
//				attr = ff_global.getAttribute("Native_file");
//				if (attr == null) {
//					return false;
//				} else {
//					return true;
//				}
//			}
//
//		} catch(Exception e) {
//		    sbError.append("DODS_Filetype not defined");
//			return false;
//		}
    }


// ----------- Access Functions --------------
    public String getDatasetName() {
    	return datasetName;
    }

    public boolean variablesExist() {
     	return showVars;
    }

    public boolean timeExists() {
     	return showTime;
    }

    public String getSequenceName() {
     	return sequenceName;
    }

    // get date information
    public int getLowYear() {
     	return lowYear;
    }
    public int getLowMonth() {
    	return lowMonth;
    }
    public int getLowDay() {
     	return lowDay;
    }
    public int getHighYear() {
    	return highYear;
    }
    public int getHighMonth() {
    	return highMonth;
    }
    public int getHighDay() {
     	return highDay;
    }

    public String getDateFields() {
     	return dateFields;
    }
    public String getDateFunction() {
    	return dateFunction;
    }


    // get variable information
    public int getNumVariables() {
     	return numVars;
    }
    public String[] getVariableNames() {
     	return varNames;
    }
    public String[][] getVariableContents() {
     	return varContents;
    }

}

/**
 * This class creates a panel with a
 * variable selection box
 *
 * @version     1.00 19 Jul 2001
 * @author      Kashan A. Shaikh
 */
class Variable extends JPanel{
	final static int VISIBLE_ROW_COUNT = 8;
	private String varName;
	private String[] varContents;

	private JList selList;


	// Constructor
	public Variable(String name, String[] contents) {
		varName = name;
		varContents = contents;

		setForeground(Color.orange);

		// format the panel
		setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		JLabel lbl = new JLabel(varName);
		lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(lbl);

		// create & add the selection box
		createSelection();
	}

    // Create & add the selection box to the panel
    private void createSelection() {
		selList = new JList(varContents);
		if (varContents.length < VISIBLE_ROW_COUNT) {
			selList.setVisibleRowCount(varContents.length);
		}
		selList.setAlignmentX(Component.CENTER_ALIGNMENT);
		JScrollPane tspane = new JScrollPane(selList);
		add(tspane);
	}


    // Get the current selection
    public String[] getSelectedItems() {
		Object[] tobj = selList.getSelectedValues();
		String[] sel = new String[tobj.length];
		for (int i = 0; i < tobj.length; i++) {
			sel[i] = (String) tobj[i];
		}
		return sel;
    }

	// Get the variable name
    public String getName() {
		return varName;
    }
}


