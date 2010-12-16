package opendap.clients.odc.data;

import java.io.OutputStream;

public class OutputProfile {

	public final static int TARGET_StandardOut = 1;
	public final static int TARGET_File = 2;
	public final static int TARGET_ViewText = 4;
	public final static int TARGET_ViewImage = 8;
	public final static int TARGET_ViewTable = 16;
	public final static int TARGET_Plotter = 32;
	public final static int FORMAT_Data_Raw = 1;
	public final static int FORMAT_Data_DODS = 2;
	public final static int FORMAT_Data_ASCII_text = 3;
	public final static int FORMAT_Data_ASCII_records = 4;
	public final static int FORMAT_Info = 5;
	public final static int FORMAT_URLs = 6;
	public final static int FORMAT_IDL = 7;
	private OutputStream mos;
	private Model_Dataset[] maurls; // 0-based
	private int[] maiTarget;
	private int[] maiFormat;
	private String[] masTargetOption; // file name
	private String msTerminator; // string to be appended to output
	public OutputProfile( Model_Dataset[] urls, OutputStream os, int iFormat, String sTerminator){
		maurls = urls;
		maiTarget = new int[urls.length];
		maiFormat = new int[urls.length];
		masTargetOption = new String[urls.length];
		for( int xURL = 0; xURL < urls.length; xURL++ ){
			maiTarget[xURL] = 0;
			masTargetOption[xURL] = null;
			maiFormat[xURL] = iFormat;
		}
		mos = os;
		msTerminator = sTerminator;
	}
	public OutputProfile( Model_Dataset url, int iTarget, String sTargetOption, int iFormat, OutputStream os ){
		maurls = new Model_Dataset[1];
		maiTarget = new int[1];
		maiFormat = new int[1];
		masTargetOption = new String[1];
		maurls[0] = url;
		maiTarget[0] = iTarget;
		masTargetOption[0] = sTargetOption;
		maiFormat[0] = iFormat;
		mos = os;
	}
	public OutputProfile( Model_Dataset url, int iTarget, String sTargetOption, int iFormat ){
		maurls = new Model_Dataset[1];
		maiTarget = new int[1];
		maiFormat = new int[1];
		masTargetOption = new String[1];
		maurls[0] = url;
		maiTarget[0] = iTarget;
		masTargetOption[0] = sTargetOption;
		maiFormat[0] = iFormat;
	}
	public OutputProfile( Model_Dataset[] urls, int iTarget, int iFormat, String[] asTargetOption, OutputStream os ){
		maurls = urls;
		maiTarget = new int[urls.length];
		maiFormat = new int[urls.length];
		for( int xURL = 0; xURL < urls.length; xURL++ ){
			maiTarget[xURL] = iTarget;
			maiFormat[xURL] = iFormat;
		}
		masTargetOption = asTargetOption;
		mos = os;
	}
	public OutputProfile( Model_Dataset[] urls, int[] aiTarget, int[] aiFormat, String[] asTargetOption, OutputStream os ){
		maurls = urls;
		maiTarget = aiTarget;
		maiFormat = aiFormat;
		masTargetOption = asTargetOption;
		mos = os;
	}
	public OutputProfile( Model_Dataset[] urls, int iTarget, String sTargetOption, int iFormat ){
		maurls = urls;
		maiTarget = new int[urls.length];
		maiFormat = new int[urls.length];
		masTargetOption = new String[urls.length];
		for( int xURL = 0; xURL < urls.length; xURL++ ){
			maiTarget[xURL] = iTarget;
			masTargetOption[xURL] = sTargetOption;
			maiFormat[xURL] = iFormat;
		}
	}
	String getTerminator(){ return msTerminator; }
	int getTarget(int x){ if( maiTarget == null ) return 0; else return maiTarget[x-1]; }
	String getTargetOption(int x){ if( masTargetOption == null ) return null; else return masTargetOption[x-1]; }
	int getFormat(int x){ if( maiFormat == null ) return 0; else return maiFormat[x-1]; }
	int getURLCount(){ if( maurls == null ) return 0; else return maurls.length; }
    Model_Dataset getURL(int x){ if( maurls == null ) return null; else return maurls[x-1]; }
	OutputStream getTargetOS(){ return mos; }
	public static String sTargetDescription( final int TARGET ){
		switch( TARGET ){
			case TARGET_StandardOut: return "Standard Out";
			case TARGET_File: return "File";
			case TARGET_ViewText: return "View Text";
			case TARGET_ViewImage: return "View Image";
			default: return "[unknown]";
		}
	}
	public static String sFormatDescription( final int FORMAT ){
		switch( FORMAT ){
			case FORMAT_Data_Raw: return "Raw";
			case FORMAT_Data_DODS: return "DODS";
			case FORMAT_Data_ASCII_text: return "ASCII Text";
			case FORMAT_Data_ASCII_records: return "ASCII Records";
			case FORMAT_Info: return "Info";
			case FORMAT_URLs: return "URL";
			case FORMAT_IDL: return "IDL";
			default: return "[unknown]";
		}
	}
	public String toString(){
		return "" + maurls.length + " URLs " + maurls[0] + " target: " + sTargetDescription(maiTarget[0]) + " format: " + sFormatDescription(maiFormat[0]) + " option: " + masTargetOption[0];
	}

}
