package opendap.clients.odc.plot;

/**
 * Title:        ColorSpecification
 * Description:  Support for defining color ranges
 * Copyright:    Copyright (c) 2003-2004
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.50
 */

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.DAP;
import opendap.clients.odc.ConfigurationManager;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Styles;
import javax.swing.JPanel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/** Colors are specified by associating a range of color to a range of values
 *  and a color for missing values. You can also ask that a range of color be
 *  restricted by hue, saturation, brightness, or any combination of the three.
 *     @param iHue  a value from -1 to 255 where -1 means no restriction
 *     @param iSaturation  a value from -1 to 255 where -1 means no restriction
 *     @param iBrightness  a value from -1 to 255 where -1 means no restriction
 */

class ColorSpecification extends AbstractListModel {

	// list model interface
	public int getSize(){ return mctRanges + 1; }
	public Object getElementAt(int index0){    // element zero is always the missing colors
		return getRangeString(index0, false);
	}

	private int mBufferLength = 0;
	public final static int DATA_TYPE_Byte = 1;
	public final static int DATA_TYPE_Int16 = 2;
	public final static int DATA_TYPE_Int32 = 3;
	public final static int DATA_TYPE_UInt16 = 4;
	public final static int DATA_TYPE_UInt32 = 5;
	public final static int DATA_TYPE_Float32 = 6;
	public final static int DATA_TYPE_Float64 = 7;
	public final static int COLOR_STYLE_Default = 1;

	public final static int COLOR_STEP_SynchronizedUp = 0;
	public final static int COLOR_STEP_SynchronizedDown = 1;
	public final static int COLOR_STEP_ContinuousUp = 2;
	public final static int COLOR_STEP_ContinuousDown = 3;

	public final static String[] COLOR_STEP_codes = { "SA", "SD", "CA", "CD" };

	// persistent fields
	private String msName;
	private int mctRanges = 0;
//	private boolean mzProportional;
	private int miDataType = 0;
	private Image[] imageSwatch; int mpxSwatchWidth = 50; int mpxSwatchHeight = 16;
	private Image mMissingSwatch = null;
	private int miMissingColor = 0xFFFFFFFF;
	private int mrgbMissingColor = 0xFFFFFFFF;
	private Color mcolorMissing = Color.WHITE;
	private int mctMissing = 0;
	private short[] ashMissing1;
	private int[] aiMissing1;
	private long[] anMissing1;
	private float[] afMissing1;
	private double[] adMissing1;
	private float[] afDataFrom_Proportional; float[] afDataTo_Proportional;
	private short[] ashDataFrom; short[] ashDataTo; // one-based
	private int[] aiDataFrom; int[] aiDataTo;
	private long[] anDataFrom; long[] anDataTo;
	private float[] afDataFrom; float[] afDataTo;
	private double[] adDataFrom; double[] adDataTo;
	private int[] ahsbColorFrom; int[] ahsbColorTo;
	private int[] aeColorStep; // see constants
	private int[] aiHue; int[] aiSaturation; int[] aiBrightness; int[] aiAlpha;

	/** if zProportional is true then the data type arg is ignored (it will automatically be float) */
	public ColorSpecification( String sName, int eDATA_TYPE ){
		msName = sName;
		miDataType = eDATA_TYPE;
		mBufferLength = 100;
		afDataFrom_Proportional = new float[mBufferLength + 1];
		afDataTo_Proportional = new float[mBufferLength + 1];
		ashDataFrom = new short[mBufferLength + 1];
		ashDataTo = new short[mBufferLength + 1];
		aiDataFrom = new int[mBufferLength + 1];
		aiDataTo = new int[mBufferLength + 1];
		anDataFrom = new long[mBufferLength + 1];
		anDataTo = new long[mBufferLength + 1];
		afDataFrom = new float[mBufferLength + 1];
		afDataTo = new float[mBufferLength + 1];
		adDataFrom = new double[mBufferLength + 1];
		adDataTo = new double[mBufferLength + 1];
		ahsbColorFrom  = new int[mBufferLength + 1];
		ahsbColorTo    = new int[mBufferLength + 1];
		aeColorStep    = new int[mBufferLength + 1];
		aiAlpha      = new int[mBufferLength + 1];
		aiHue        = new int[mBufferLength + 1];
		aiSaturation = new int[mBufferLength + 1];
		aiBrightness = new int[mBufferLength + 1];
		imageSwatch  = new Image[mBufferLength + 1];
	}
	String getName(){ return msName; }

	public void setDataType( int eDataType ){
		StringBuffer sbError = new StringBuffer(80);
		if( miDataType != eDataType ){
			this.rangeRemoveAll(sbError);
			this.setMissing( null, miDataType, 0x00000000 );
		}
		miDataType = eDataType;
		Panel_ColorSpecification panel_cs = Panel_View_Plot.getPanel_ColorSpecification();
		if( panel_cs != null ) panel_cs.vUpdateInfo();
	}

	public String toString(){
		return getName();
	}

	public String toString(String sIndent){
		StringBuffer sb = new StringBuffer(500);
		sb.append(sIndent).append("Color Specification:\n");
		sb.append(sIndent).append("\tname: " + getName() + "\n");
		sb.append(sIndent).append("\ttype: " + DAP.getType_String(miDataType) + "\n");
		sb.append(sIndent).append("\tranges(" + (mctRanges + 1) + "):\n"); // plus one for the missing
		for( int xRange = 0; xRange <= mctRanges; xRange++ ){
			sb.append(sIndent).append("\t\t").append( getRangeString(xRange, true) ).append("\n");
		}
		sb.append("\n");
		return sb.toString();
	}

	int getRangeCount(){ return mctRanges; }

	StringBuffer sbRangeString = new StringBuffer(255);
	String getRangeString( int x1, boolean zFraming ){
		sbRangeString.setLength(0);
		if( x1 == 0 ){
			sbRangeString.append("Missing:");
			if( zFraming ){
				sbRangeString.append(' ');
				sbRangeString.append(Integer.toHexString(miMissingColor));
			}
			sbRangeString.append(' ');
			if( mctMissing == 0 ){
				sbRangeString.append("[none]");
			} else {
				sbRangeString.append(this.getMissingString());
			}
		} else {
			if( zFraming ){
				sbRangeString.append("Range: ");
			}
			switch( miDataType ){
				case DATA_TYPE_Byte:
				case DATA_TYPE_Int16:
					sbRangeString.append(ashDataFrom[x1]);
					sbRangeString.append(' ');
					sbRangeString.append(ashDataTo[x1]); break;
				case DATA_TYPE_UInt16:
				case DATA_TYPE_Int32:
					sbRangeString.append(aiDataFrom[x1]);
					sbRangeString.append(' ');
					sbRangeString.append(aiDataTo[x1]); break;
				case DATA_TYPE_UInt32:
					sbRangeString.append(anDataFrom[x1]);
					sbRangeString.append(' ');
					sbRangeString.append(anDataTo[x1]); break;
				case DATA_TYPE_Float32:
					sbRangeString.append(afDataFrom[x1]);
					sbRangeString.append(' ');
					sbRangeString.append(afDataTo[x1]); break;
				case DATA_TYPE_Float64:
					sbRangeString.append(adDataFrom[x1]);
					sbRangeString.append(' ');
					sbRangeString.append(adDataTo[x1]); break;
				case DAP.DATA_TYPE_String:
				default:
					// not supported
			}
			sbRangeString.append(' ');
			sbRangeString.append(Utility.sToHex(ahsbColorFrom[x1], 8));
			sbRangeString.append(' ');
			sbRangeString.append(Utility.sToHex(ahsbColorTo[x1], 8));
			sbRangeString.append(' ');
			sbRangeString.append(COLOR_STEP_codes[aeColorStep[x1]]);
			sbRangeString.append(' ');
			if( aiHue[x1] == -1 )
			    sbRangeString.append("~");
		    else
				sbRangeString.append(Utility.sToHex(aiHue[x1], 2));
			sbRangeString.append(' ');
			if( aiSaturation[x1] == -1 )
			    sbRangeString.append("~");
		    else
		    	sbRangeString.append(Utility.sToHex(aiSaturation[x1], 2));
			sbRangeString.append(' ');
			if( aiBrightness[x1] == -1 )
			    sbRangeString.append("~");
		    else
	    		sbRangeString.append(Utility.sToHex(aiBrightness[x1], 2));
			sbRangeString.append(' ');
			if( aiAlpha[x1] == -1 )
			    sbRangeString.append("~");
		    else
				sbRangeString.append(Utility.sToHex(aiAlpha[x1], 2));
		}
		return sbRangeString.toString();
	}

	JFileChooser jfc;
	boolean zStorage_save( StringBuffer sbError ){
		try {

			// ask user for desired location
			String sPlotsDirectory = ConfigurationManager.getInstance().getProperty_DIR_Plots();
			File filePlotsDirectory = Utility.fileEstablishDirectory(sPlotsDirectory);
			if (jfc == null) jfc = new JFileChooser();
			if( filePlotsDirectory == null ){
				// no default directory
			} else {
				jfc.setCurrentDirectory(filePlotsDirectory);
			}
			String sName = this.getName();
			String sSuggestedFileName = Utility.sFriendlyFileName(sName) + ConfigurationManager.EXTENSION_ColorSpecification;
			jfc.setSelectedFile(new File(sSuggestedFileName));
			int iState = jfc.showDialog(ApplicationController.getInstance().getAppFrame(), "Select Save Location");
			File file = jfc.getSelectedFile();
			if (file == null || iState != JFileChooser.APPROVE_OPTION) return true; // user cancel

			// try to save this directory as the new plots directory
			File fileNewPlotsDirectory = file.getParentFile();
			if( fileNewPlotsDirectory != null ) if( !fileNewPlotsDirectory.equals(filePlotsDirectory) ){
				String sNewPlotsDirectory = fileNewPlotsDirectory.getCanonicalPath();
				ConfigurationManager.getInstance().setOption(ConfigurationManager.getInstance().PROPERTY_DIR_Plots, sNewPlotsDirectory );
			}

			// open file
			FileOutputStream fos;
		    try {
			    fos = new java.io.FileOutputStream(file);
				if( fos == null ){
					sbError.append("Failed to open file (" + file + ") for saving color specification, empty stream");
				}
			} catch(Exception ex) {
				sbError.append("Failed to open file (" + file + ") for saving color specification: " + ex);
				return false;
			}

			// save to file
			FileChannel fc = fos.getChannel();
			String sCS = this.toString("");
			try {
				fc.write(ByteBuffer.wrap(sCS.getBytes()));
			} catch(Exception ex) {
				ApplicationController.vShowError("Failed to write color specification to file [" + file + "]: " + ex);
			} finally {
				try {
					if(fos!=null) fos.close();
				} catch(Exception ex) {}
			}

			ApplicationController.vShowStatus("Wrote color specification " + sName + " to file " + file);
			return true;
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	boolean zStorage_load( StringBuffer sbError ){
		try {

			// ask user for desired location
			String sPlotsDirectory = ConfigurationManager.getInstance().getProperty_DIR_Plots();
			File filePlotsDirectory = Utility.fileEstablishDirectory(sPlotsDirectory);
			if (jfc == null) jfc = new JFileChooser();
			if( filePlotsDirectory == null ){
				// no default directory
			} else {
				jfc.setCurrentDirectory(filePlotsDirectory);
			}
			String sRawName = this.getName();
			String sSuggestedFileName = Utility.sFriendlyFileName(sRawName) + ConfigurationManager.EXTENSION_ColorSpecification;
			jfc.setSelectedFile(new File(sSuggestedFileName));
			int iState = jfc.showDialog(ApplicationController.getInstance().getAppFrame(), "Select Color Specification to Load");
			File file = jfc.getSelectedFile();
			if (file == null || iState != JFileChooser.APPROVE_OPTION) return true; // user cancel

			// try to save this directory as the new plots directory
			File fileNewPlotsDirectory = file.getParentFile();
			if( fileNewPlotsDirectory != null ) if( !fileNewPlotsDirectory.equals(filePlotsDirectory) ){
				String sNewPlotsDirectory = fileNewPlotsDirectory.getCanonicalPath();
				ConfigurationManager.getInstance().setOption(ConfigurationManager.getInstance().PROPERTY_DIR_Plots, sNewPlotsDirectory );
			}

			// load the lines of the file
			String sPath = file.getAbsolutePath();
			ArrayList listLines = Utility.zLoadLines( sPath, 1000, sbError );
			if( listLines == null ){
				sbError.insert(0, "Failed to load lines from " + sPath + ": ");
				return false;
			}

			// process lines
			int eState = 1; // loading name
			int xLine = 0;
			int iRangeIndex1 = 0, ctRanges = 0;
			boolean zFireEvent = false; // do not fire events while loading
			while( xLine < listLines.size() ){
				String sLine = (String)listLines.get(xLine);
				if( sLine == null ){ xLine++; continue; }
				sLine = sLine.trim();
				if( sLine.length() == 0 ){ xLine++; continue; } // ignore blank lines
				switch( eState ){
					case 1: // loading name
						if( sLine.toUpperCase().startsWith("NAME:") ){
							String sName = Utility.getEnclosedSubstring(sLine, ":", null).trim();
							if( sName.length() == 0 ){
								sbError.append("name field is blank");
								return false;
							}
							msName = sName;
							eState = 2;
						}
						break;
					case 2: // loading type
						if( sLine.toUpperCase().startsWith("TYPE:") ){
							String sType = Utility.getEnclosedSubstring(sLine, ":", null).trim();
							if( sType == null || sType.length() == 0 ){
								sbError.append("type field is blank");
								return false;
							}
							int eDataType = DAP.getTypeByName(sType);
							if( eDataType == 0 ){
								sbError.append("type \"" + sType + "\" is unknown");
								return false;
							}
							setDataType(eDataType);
							eState = 3;
						}
						break;
					case 3: // loading range header
						if( sLine.toUpperCase().startsWith("RANGES") ){
							String sRangeCount = Utility.getEnclosedSubstring(sLine, "(", ")");
							if( sRangeCount == null || sRangeCount.length() == 0 ){
								sbError.append("range count was missing");
								return false;
							}
							ctRanges = -1;
							try { ctRanges = Integer.parseInt(sRangeCount); } catch(Exception ex){}
							if( ctRanges < 1 ){
								sbError.append("range count (" + sRangeCount + ") was not a positive integer");
								return false;
							}
							eState = 4;
						}
						break;
					case 4: // loading a range
						iRangeIndex1++;
						String[] asLine = Utility.splitCommaWhiteSpace(sLine);
						if( asLine == null || asLine.length < 3 ){
							sbError.append("invalid range line (" + sLine + ")");
							return false;
						}
						if( asLine[0].toUpperCase().startsWith("MISSING:") ){
							String sMissingColor = asLine[1];
							int iMissingColor;
							try { iMissingColor = (int)Long.parseLong(sMissingColor, 16); }
							catch( Exception ex ) {
								sbError.append("missing color (" + sMissingColor + ") invalid: " + ex);
								return false;
							}
							int posMissingColor = sLine.indexOf(sMissingColor);
							if( posMissingColor == -1 ){ sbError.append("internal error, unable to relocate missing color"); return false; }
							String sMissingValues = sLine.substring(posMissingColor + sMissingColor.length(), sLine.length() );
							if( setMissing(sMissingValues, iMissingColor, sbError) ){
								// got missing values
							} else {
								sbError.insert(0, "failed to set missing values (" + sMissingValues + "): ");
								return false;
							}
						} else if( asLine[0].toUpperCase().startsWith("RANGE:") ){
							if( asLine.length < 10 ){
								sbError.append("expected at least 10 elements in range string (" + sLine + ")");
								return false;
							}
							int iColorFrom, iColorTo, iHue, iSat, iBri, iAlpha, iParam = 3;
							try {
								iColorFrom = (int)Long.parseLong(asLine[3], 16); iParam = 4;
								iColorTo   = (int)Long.parseLong(asLine[4], 16); iParam = 6;
								iHue       = asLine[6].equals("~") ? -1 : (int)Long.parseLong(asLine[6], 16); iParam = 7;
								iSat       = asLine[7].equals("~") ? -1 : (int)Long.parseLong(asLine[7], 16); iParam = 8;
								iBri       = asLine[8].equals("~") ? -1 : (int)Long.parseLong(asLine[8], 16); iParam = 9;
								iAlpha     = asLine[9].equals("~") ? -1 : (int)Long.parseLong(asLine[9], 16);
							} catch(Exception ex){
								sbError.append("error parsing color parameter " + iParam + "(" + asLine[iParam] + ") in line (" + sLine + ")");
								return false;
							}
							int eColorStep;
							if( asLine[5].equals("SA") ){
								eColorStep = ColorSpecification.COLOR_STEP_SynchronizedUp;
							} else if( asLine[5].equals("SD") ){
								eColorStep = ColorSpecification.COLOR_STEP_SynchronizedDown;
							} else if( asLine[5].equals("CA") ){
								eColorStep = ColorSpecification.COLOR_STEP_ContinuousUp;
							} else if( asLine[5].equals("CD") ){
								eColorStep = ColorSpecification.COLOR_STEP_ContinuousDown;
							} else {
								eColorStep = ColorSpecification.COLOR_STEP_SynchronizedDown; // default
							}
							String sDataFrom = asLine[1];
							String sDataTo = asLine[2];
							switch( getDataType() ){
								case DAP.DATA_TYPE_Byte:
								case DAP.DATA_TYPE_Int16:
									try {
										short shDataFrom = Short.parseShort(sDataFrom);
										short shDataTo = Short.parseShort(sDataTo);
										if( !rangeAdd( shDataFrom, shDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, zFireEvent, sbError) ){
											sbError.insert(0, "Unable to set range: ");
											return false;
										}
									} catch(Exception ex) {
										sbError.append("Unable to understand data as short: [" + sDataFrom + " to " + sDataTo + "]");
										return false;
									}
									break;
								case DAP.DATA_TYPE_Int32:
								case DAP.DATA_TYPE_UInt16:
									try {
										int iDataFrom = Integer.parseInt(sDataFrom);
										int iDataTo = Integer.parseInt(sDataTo);
										if( !rangeAdd( iDataFrom, iDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, zFireEvent, sbError) ){
											sbError.insert(0, "Unable to set range: ");
											return false;
										}
									} catch(Exception ex) {
										sbError.append("Unable to understand data as int: [" + sDataFrom + " to " + sDataTo + "]");
										return false;
									}
									break;
								case DAP.DATA_TYPE_UInt32:
									try {
										long nDataFrom = Long.parseLong(sDataFrom);
										long nDataTo = Long.parseLong(sDataTo);
										if( !rangeAdd( nDataFrom, nDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, zFireEvent, sbError) ){
											sbError.insert(0, "Unable to set range: ");
											return false;
										}
									} catch(Exception ex) {
										sbError.append("Unable to understand data as long: [" + sDataFrom + " to " + sDataTo + "]");
										return false;
									}
									break;
								case DAP.DATA_TYPE_Float32:
									try {
										float fDataFrom = Float.parseFloat(sDataFrom);
										float fDataTo = Float.parseFloat(sDataTo);
										if( !rangeAdd( fDataFrom, fDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, zFireEvent, sbError) ){
											sbError.insert(0, "Unable to set range: ");
											return false;
										}
									} catch(Exception ex) {
										sbError.append("Unable to understand data as float: [" + sDataFrom + " to " + sDataTo + "]");
										return false;
									}
									break;
								case DAP.DATA_TYPE_Float64:
									try {
										double dDataFrom = Double.parseDouble(sDataFrom);
										double dDataTo = Double.parseDouble(sDataTo);
										if( !rangeAdd( dDataFrom, dDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, zFireEvent, sbError) ){
											sbError.insert(0, "Unable to set range: ");
											return false;
										}
									} catch(Exception ex) {
										sbError.insert(0, "Unable to understand data as double: [" + sDataFrom + " to " + sDataTo + "]");
										return false;
									}
									break;
								case DAP.DATA_TYPE_String:
								default:
									sbError.append("Internal Error, unexpected data type in range load");
								    return false;
							}
						} else {
							sbError.append("expected range " + iRangeIndex1 + " (" + sLine + ")");
							return false;
						}
						if( iRangeIndex1 == ctRanges ) eState = 6;
						break;
					case 6: // done
						break; // ignore any remaining lines
				}
				xLine++;
			}

			// validate
			if( eState < 6 ){
				String sProblem = ( eState == 1 ? "name" : ( eState == 2 ? "type" : ( eState == 3 ? "range header" : ( eState == 4 ? "ranges" : "missing" ))));
				sbError.append("Error loading, " + eState + " field was missing or invalid");
				return false;
			}

			return true; // done, yeah

		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	void setMissingColor( int hsbColor ){
		miMissingColor = hsbColor;
		mrgbMissingColor = iHSBtoRGBA(hsbColor);
		mcolorMissing = new Color(mrgbMissingColor);
		this.vMakeSwatch(0);
	}
	boolean isSingleColor( int xRange1 ){
		if( xRange1 < 1 || xRange1 > mctRanges ) return false;
		return ahsbColorFrom[xRange1] == ahsbColorTo[xRange1];
	}
	boolean isSingleValue( int xRange1 ){
		if( xRange1 < 1 || xRange1 > mctRanges ) return false;
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				return ashDataFrom[xRange1] == ashDataTo[xRange1];
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				return aiDataFrom[xRange1] == aiDataTo[xRange1];
			case DATA_TYPE_UInt32:
				return anDataFrom[xRange1] == anDataTo[xRange1];
			case DATA_TYPE_Float32:
			    return afDataFrom[xRange1] == afDataTo[xRange1];
			case DATA_TYPE_Float64:
				return adDataFrom[xRange1] == adDataTo[xRange1];
		}
		return false;
	}
	boolean setMissing( String sMissingValues, int hsbColor, StringBuffer sbError ){
		if( sMissingValues == null ) sMissingValues = "";
		String[] as = Utility.split(sMissingValues, ' ');
		if( as[0].length() == 0 ){
			mctMissing = 0;
		} else {
			mctMissing = as.length;
		}
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				ashMissing1 = new short[mctMissing + 1];
				for( int xValue0 = 0; xValue0 < mctMissing; xValue0++ ){
					try {
						ashMissing1[xValue0 + 1] = Short.parseShort( as[xValue0] );
					} catch(Exception ex) {
						sbError.append("Unable to interpret [" + as[xValue0]  + "] as a short");
						mctMissing = xValue0; // in this case only the missing values successfully processed will be allowed
						return false;
					}
				}
				break;
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				aiMissing1 = new int[mctMissing + 1];
				for( int xValue0 = 0; xValue0 < mctMissing; xValue0++ ){
					try {
						aiMissing1[xValue0 + 1] = Integer.parseInt( as[xValue0] );
					} catch(Exception ex) {
						if( miDataType == DATA_TYPE_UInt16 ){
							sbError.append("Unable to interpret [" + as[xValue0]  + "] as an unsigned short");
						} else {
							sbError.append("Unable to interpret [" + as[xValue0]  + "] as an int");
						}
						mctMissing = xValue0; // in this case only the missing values successfully processed will be allowed
						return false;
					}
				}
				break;
			case DATA_TYPE_UInt32:
				anMissing1 = new long[mctMissing + 1];
				for( int xValue0 = 0; xValue0 < mctMissing; xValue0++ ){
					try {
						anMissing1[xValue0 + 1] = Long.parseLong( as[xValue0] );
					} catch(Exception ex) {
						sbError.append("Unable to interpret [" + as[xValue0]  + "] as an unsigned int");
						mctMissing = xValue0; // in this case only the missing values successfully processed will be allowed
						return false;
					}
				}
				break;
			case DATA_TYPE_Float32:
				afMissing1 = new float[mctMissing + 1];
				for( int xValue0 = 0; xValue0 < mctMissing; xValue0++ ){
					try {
						afMissing1[xValue0 + 1] = Float.parseFloat( as[xValue0] );
					} catch(Exception ex) {
						sbError.append("Unable to interpret [" + as[xValue0]  + "] as a float");
						mctMissing = xValue0; // in this case only the missing values successfully processed will be allowed
						return false;
					}
				}
				break;
			case DATA_TYPE_Float64:
				adMissing1 = new double[mctMissing + 1];
				for( int xValue0 = 0; xValue0 < mctMissing; xValue0++ ){
					try {
						adMissing1[xValue0 + 1] = Double.parseDouble( as[xValue0] );
					} catch(Exception ex) {
						sbError.append("Unable to interpret [" + as[xValue0]  + "] as a double");
						mctMissing = xValue0; // in this case only the missing values successfully processed will be allowed
						return false;
					}
				}
				break;
		}
		setMissingColor(hsbColor);
		fireContentsChanged(this, 0, 0);
		return true;
	}
	void setMissing( Object[] eggMissing, int meType, int hsbColor ){
		if( eggMissing == null ){
			mctMissing = 0;
			return;
		}
		if( eggMissing[0] == null ){
			mctMissing = 0;
			return;
		}
		if( meType != miDataType ){
			ApplicationController.vShowWarning("internal error, missing type mismatch in color specification");
			return;
		}
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				ashMissing1 = (short[])eggMissing[0];
				mctMissing = ashMissing1.length - 1;
				break;
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				aiMissing1 = (int[])eggMissing[0];
				mctMissing = aiMissing1.length - 1;
				break;
			case DATA_TYPE_UInt32:
				anMissing1 = (long[])eggMissing[0];
				mctMissing = anMissing1.length - 1;
				break;
			case DATA_TYPE_Float32:
				afMissing1 = (float[])eggMissing[0];
				mctMissing = afMissing1.length - 1;
				break;
			case DATA_TYPE_Float64:
				adMissing1 = (double[])eggMissing[0];
				mctMissing = adMissing1.length - 1;
				break;
		}
		setMissingColor(hsbColor);
		fireContentsChanged(this, 0, 0);
	}
	int getMissingCount(){ return mctMissing; }
	String getMissingString(){
		if( this.mctMissing == 0 ) return "";
		StringBuffer sb = new StringBuffer(80);
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				for( int xMissing = 1; xMissing <= mctMissing; xMissing++ )
					sb.append(" " + ashMissing1[xMissing]);
				break;
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				for( int xMissing = 1; xMissing <= mctMissing; xMissing++ )
					sb.append(" " + aiMissing1[xMissing]);
				break;
			case DATA_TYPE_UInt32:
				for( int xMissing = 1; xMissing <= mctMissing; xMissing++ )
					sb.append(" " + anMissing1[xMissing]);
				break;
			case DATA_TYPE_Float32:
				for( int xMissing = 1; xMissing <= mctMissing; xMissing++ )
					sb.append(" " + afMissing1[xMissing]);
				break;
			case DATA_TYPE_Float64:
				for( int xMissing = 1; xMissing <= mctMissing; xMissing++ )
					sb.append(" " + adMissing1[xMissing]);
				break;
			default:
				return "";
		}
		return sb.substring(1);
	}
	double getDataFrom_double(int index1){
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				return (double)ashDataFrom[index1];
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				return (double)aiDataFrom[index1];
			case DATA_TYPE_UInt32:
				return (double)anDataFrom[index1];
			case DATA_TYPE_Float32:
				return (double)afDataFrom[index1];
			case DATA_TYPE_Float64:
				return (double)adDataFrom[index1];
			default: return 0d;
		}
	}
	double getDataTo_double(int index1){
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				return (double)ashDataTo[index1];
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				return (double)aiDataTo[index1];
			case DATA_TYPE_UInt32:
				return (double)anDataTo[index1];
			case DATA_TYPE_Float32:
				return (double)afDataTo[index1];
			case DATA_TYPE_Float64:
				return (double)adDataTo[index1];
			default: return 0d;
		}
	}
	String getDataFromS(int index1){
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				return "" + ashDataFrom[index1];
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				return "" + aiDataFrom[index1];
			case DATA_TYPE_UInt32:
				return "" + anDataFrom[index1];
			case DATA_TYPE_Float32:
				return "" + afDataFrom[index1];
			case DATA_TYPE_Float64:
				return "" + adDataFrom[index1];
			default: return "?";
		}
	}
	String getDataToS(int index1){
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				return "" + ashDataTo[index1];
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				return "" + aiDataTo[index1];
			case DATA_TYPE_UInt32:
				return "" + anDataTo[index1];
			case DATA_TYPE_Float32:
			    return "" + afDataTo[index1];
			case DATA_TYPE_Float64:
				return "" + adDataTo[index1];
			default: return "?";
		}
	}
	int getColorMissingHSB(){
		return miMissingColor;
	}
	Color getColorFrom( int index1 ){
		return new Color( ahsbColorFrom[index1], true );
	}
	Color getColorTo( int index1 ){
		return new Color( ahsbColorTo[index1], true );
	}
	int getColorStep( int index1 ){
		return aeColorStep[index1];
	}
	int getColorFromHSB( int index1 ){
		return ahsbColorFrom[index1];
	}
	int getColorToHSB( int index1 ){
		return ahsbColorTo[index1];
	}
	String getColorFromS( int index1 ){
		if( ahsbColorFrom[index1] == -1 ) return "[varies]";
		return Utility.sFixedWidth(Integer.toHexString(ahsbColorFrom[index1]), 8, '0', Utility.ALIGNMENT_RIGHT);
	}
	String getColorToS( int index1 ){
		if( ahsbColorTo[index1] == -1 ) return "[varies]";
		return Utility.sFixedWidth(Integer.toHexString(ahsbColorTo[index1]), 8, '0', Utility.ALIGNMENT_RIGHT);
	}
	int getHue( int index1 ){ return aiHue[index1]; }
	int getSaturation( int index1 ){ return aiSaturation[index1]; }
	int getBrightness( int index1 ){ return aiBrightness[index1]; }
	int getAlpha( int index1 ){ return aiAlpha[index1]; }
	int getDataType(){ return miDataType; }
	void setSwatchSize( int pxWidth, int pxHeight ){ mpxSwatchWidth = pxWidth; mpxSwatchHeight = pxHeight; }
	int getSwatchWidth(){ return this.mpxSwatchWidth; }
	Image getMissingSwatch(){ return mMissingSwatch; }
	Image getSwatch( int xEntry1 ){
		if( xEntry1 < 0 ) return null;
		if( xEntry1 == 0 ) return mMissingSwatch; else return imageSwatch[xEntry1];
	}
	boolean rangeAdd( short shortDataFrom, short shortDataTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, boolean zFireEvent, StringBuffer sbError ){
		if( miDataType != DATA_TYPE_Int16 && miDataType != DATA_TYPE_Byte ){ sbError.append("bad data type, received short, expected " + DAP.getType_String(miDataType)); return false; } // ignore invalid range types
		if( mctRanges == mBufferLength ){
			sbError.append("color specification can only have " + mBufferLength + " ranges");
			return false; // ignore excessive number of ranges
		}
		mctRanges++;
		ashDataFrom[mctRanges] = shortDataFrom;
		ashDataTo[mctRanges] = shortDataTo;
		return rangeSet( mctRanges, iColorFrom, iColorTo, iColorStep, iHue, iSaturation, iBrightness, iAlpha, zFireEvent, sbError );
	}
	boolean rangeAdd( int iDataFrom, int iDataTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, boolean zFireEvent, StringBuffer sbError ){
		if( miDataType != DATA_TYPE_Int32 && miDataType != DATA_TYPE_UInt16 ){ sbError.append("bad data type, received int, expected " + DAP.getType_String(miDataType)); return false; } // ignore invalid range types
		if( mctRanges == mBufferLength ){
			sbError.append("color specification can only have " + mBufferLength + " ranges");
			return false; // ignore excessive number of ranges
		}
		mctRanges++;
		aiDataFrom[mctRanges] = iDataFrom;
		aiDataTo[mctRanges] = iDataTo;
		return rangeSet( mctRanges, iColorFrom, iColorTo, iColorStep, iHue, iSaturation, iBrightness, iAlpha, zFireEvent, sbError );
	}
	boolean rangeAdd( long nDataFrom, long nDataTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, boolean zFireEvent, StringBuffer sbError ){
		if( miDataType != DATA_TYPE_UInt32 ){ sbError.append("bad data type, received long, expected " + DAP.getType_String(miDataType)); return false; } // ignore invalid range types
		if( mctRanges == mBufferLength ){
			sbError.append("color specification can only have " + mBufferLength + " ranges");
			return false; // ignore excessive number of ranges
		}
		mctRanges++;
		anDataFrom[mctRanges] = nDataFrom;
		anDataTo[mctRanges] = nDataTo;
		return rangeSet( mctRanges, iColorFrom, iColorTo, iColorStep, iHue, iSaturation, iBrightness, iAlpha, zFireEvent, sbError );
	}
	boolean rangeAdd( float fDataFrom, float fDataTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, boolean zFireEvent, StringBuffer sbError ){
		if( miDataType != DATA_TYPE_Float32 ){ sbError.append("bad data type, received float, expected " + DAP.getType_String(miDataType)); return false; } // ignore invalid range types
		if( mctRanges == mBufferLength ){
			sbError.append("color specification can only have " + mBufferLength + " ranges");
			return false; // ignore excessive number of ranges
		}
		mctRanges++;
		afDataFrom[mctRanges] = fDataFrom;
		afDataTo[mctRanges] = fDataTo;
		return rangeSet( mctRanges, iColorFrom, iColorTo, iColorStep, iHue, iSaturation, iBrightness, iAlpha, zFireEvent, sbError );
	}
	boolean rangeAdd( double dDataFrom, double dDataTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, boolean zFireEvent, StringBuffer sbError ){
		if( miDataType != DATA_TYPE_Float64 ){ sbError.append("bad data type, received double, expected " + DAP.getType_String(miDataType)); return false; } // ignore invalid range types
		if( mctRanges == mBufferLength ){
			sbError.append("color specification can only have " + mBufferLength + " ranges");
			return false; // ignore excessive number of ranges
		}
		mctRanges++;
		adDataFrom[mctRanges] = dDataFrom;
		adDataTo[mctRanges] = dDataTo;
		return rangeSet( mctRanges, iColorFrom, iColorTo, iColorStep, iHue, iSaturation, iBrightness, iAlpha, zFireEvent, sbError );
	}
	boolean rangeSet( int x1, int iColorFrom, int iColorTo, int eColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, boolean zFireEvent, StringBuffer sbError ){
		ahsbColorFrom[x1] = iColorFrom;
		ahsbColorTo[x1] = iColorTo;
		if( eColorStep < 0 || eColorStep > 3 ){
			sbError.append("color step must be between 0 and 3");
			return false;
		} else {
			aeColorStep[x1] = eColorStep;
		}
		if( iHue >= -1 && iHue <= 0xFF ){
			aiHue[x1] = iHue;
		} else {
			sbError.append("invalid hue (" + Utility.sToHex(iHue, 8) + "), must be between -1 and 0xFF");
			return false;
		}
		if( iSaturation >= -1 && iSaturation <= 0xFF ){
			aiSaturation[x1] = iSaturation;
		} else {
			sbError.append("invalid saturation (" + Utility.sToHex(iSaturation, 8) + "), must be between -1 and 0xFF");
			return false;
		}
		if( iBrightness >= -1 && iBrightness <= 0xFF ){
			aiBrightness[x1] = iBrightness;
		} else {
			sbError.append("invalid brightness (" + Utility.sToHex(iBrightness, 8) + "), must be between -1 and 0xFF");
			return false;
		}
		if( iAlpha >= -1 && iAlpha <= 0xFF ){
			aiAlpha[x1] = iAlpha;
		} else {
			sbError.append("invalid alpha (" + Utility.sToHex(iAlpha, 8) + "), must be between -1 and 0xFF");
			return false;
		}
		vMakeSwatch( x1 );
		if( zFireEvent ) fireIntervalAdded(this, x1, x1); else fireContentsChanged(this, x1, x1);
		return true;
	}
	boolean rangeSet( int x1, short dataFrom, short dataTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, StringBuffer sbError ){
		if( miDataType != DATA_TYPE_Byte && miDataType != DATA_TYPE_Int16 ){ sbError.append("bad data type for set, received short, expected " + DAP.getType_String(miDataType)); return false; } // ignore invalid range types
		ashDataFrom[x1] = dataFrom;
		ashDataTo[x1] = dataTo;
		return rangeSet( x1, iColorFrom, iColorTo, iColorStep, iHue, iSaturation, iBrightness, iAlpha, false, sbError );
	}
	boolean rangeSet( int x1, int dataFrom, int dataTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, StringBuffer sbError ){
		if( miDataType != DATA_TYPE_Int32 && miDataType != DATA_TYPE_UInt16 ){ sbError.append("bad data type"); return false; } // ignore invalid range types
		aiDataFrom[x1] = dataFrom;
		aiDataTo[x1] = dataTo;
		return rangeSet( x1, iColorFrom, iColorTo, iColorStep, iHue, iSaturation, iBrightness, iAlpha, false, sbError );
	}
	boolean rangeSet( int x1, long dataFrom, long dataTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, StringBuffer sbError ){
		if( miDataType != DATA_TYPE_UInt32 ){ sbError.append("bad data type"); return false; } // ignore invalid range types
		anDataFrom[x1] = dataFrom;
		anDataTo[x1] = dataTo;
		return rangeSet( x1, iColorFrom, iColorTo, iColorStep, iHue, iSaturation, iBrightness, iAlpha, false, sbError );
	}
	boolean rangeSet( int x1, float dataFrom, float dataTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, StringBuffer sbError ){
		if( miDataType != DATA_TYPE_Float32 ){ sbError.append("bad data type"); return false; } // ignore invalid range types
		afDataFrom[x1] = dataFrom;
		afDataTo[x1] = dataTo;
		return rangeSet( x1, iColorFrom, iColorTo, iColorStep, iHue, iSaturation, iBrightness, iAlpha, false, sbError );
	}
	boolean rangeSet( int x1, double dataFrom, double dataTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSaturation, int iBrightness, int iAlpha, StringBuffer sbError ){
		if( miDataType != DATA_TYPE_Float64 ){ sbError.append("bad data type"); return false; } // ignore invalid range types
		adDataFrom[x1] = dataFrom;
		adDataTo[x1] = dataTo;
		return rangeSet( x1, iColorFrom, iColorTo, iColorStep, iHue, iSaturation, iBrightness, iAlpha, false, sbError );
	}
	boolean rangeRemoveAll( StringBuffer sbError ){
		int iLastRange = mctRanges;
		mctRanges = 0;
		if( iLastRange > 0 ) this.fireIntervalRemoved(this, 0, iLastRange - 1);
		return true;
	}
	boolean rangeRemove( int xRangeToRemove1, StringBuffer sbError ){
		if( mctRanges == 0 ){
			sbError.append("color specification has no ranges");
			return false;
		}
		if( xRangeToRemove1 > mctRanges ){
			sbError.append("range index " + xRangeToRemove1 + " invalid [" + mctRanges + " ranges]");
			return false; // ignore excessive number of ranges
		}
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				for( int xRange=xRangeToRemove1; xRange < mctRanges; xRange++ ){
					ashDataFrom[xRange] = ashDataFrom[xRange+1];
					ashDataTo[xRange] = ashDataTo[xRange+1];
				}
				break;
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				for( int xRange=xRangeToRemove1; xRange < mctRanges; xRange++ ){
					aiDataFrom[xRange] = aiDataFrom[xRange+1];
					aiDataTo[xRange] = aiDataTo[xRange+1];
				}
				break;
			case DATA_TYPE_UInt32:
				for( int xRange=xRangeToRemove1; xRange < mctRanges; xRange++ ){
					anDataFrom[xRange] = anDataFrom[xRange+1];
					anDataTo[xRange] = anDataTo[xRange+1];
				}
				break;
			case DATA_TYPE_Float32:
				for( int xRange=xRangeToRemove1; xRange < mctRanges; xRange++ ){
					afDataFrom[xRange] = afDataFrom[xRange+1];
					afDataTo[xRange] = afDataTo[xRange+1];
				}
				break;
			case DATA_TYPE_Float64:
				for( int xRange=xRangeToRemove1; xRange < mctRanges; xRange++ ){
					adDataFrom[xRange] = adDataFrom[xRange+1];
					adDataTo[xRange] = adDataTo[xRange+1];
				}
				break;
		}
		for( int xRange=xRangeToRemove1; xRange < mctRanges; xRange++ ){
			ahsbColorFrom[xRange] = ahsbColorFrom[xRange+1];
	    	ahsbColorTo[xRange] = ahsbColorTo[xRange+1];
			aeColorStep[xRange] = aeColorStep[xRange+1];
		    aiHue[xRange] = aiHue[xRange+1];
			aiSaturation[xRange] = aiSaturation[xRange+1];
	    	aiBrightness[xRange] = aiBrightness[xRange+1];
		    aiAlpha[xRange] = aiAlpha[xRange+1];
			imageSwatch[xRange] = imageSwatch[xRange+1];
		}
		mctRanges--;
		this.fireIntervalRemoved(this, xRangeToRemove1-1, xRangeToRemove1-1);
		return true;
	}
	void vMakeSwatch( int xRange1 ){
		Image image = imageMakeSwatch( xRange1, mpxSwatchWidth, mpxSwatchHeight, true, true, true );
		if( xRange1 == 0 ){ // made missing swatch
			mMissingSwatch = image;
		} else { // set range swatch
			imageSwatch[xRange1] = image;
		}
	}
	// ascending means the colors go from left to right, or from bottom to top
	Image imageMakeSwatch( int xRange1, int pxSwatchWidth, int pxSwatchHeight, boolean zHorizontal, boolean zAscending, boolean zBorder ){
		BufferedImage bi = new BufferedImage(pxSwatchWidth, pxSwatchHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.getGraphics();
		if( xRange1 == 0 ){ // make missing swatch
			g.setColor(mcolorMissing);
			g.fillRect(0, 0, pxSwatchWidth, pxSwatchHeight);
		} else {
			int[] aiRGB =  new int[pxSwatchWidth * pxSwatchHeight];
			if( zHorizontal ){
				for( int xSwatchPixel = 1; xSwatchPixel <= pxSwatchWidth; xSwatchPixel++ ){
					float fProportion = (float)xSwatchPixel / (float)pxSwatchWidth;
					int rgb = rgbGetForRange(fProportion, xRange1);
					int xBand;
				    if( zAscending ){
						xBand = xSwatchPixel - 1; // low bands should be at the left of the image
					} else {
						xBand = pxSwatchWidth - xSwatchPixel; // low bands should be at the right of the image
					}
					for( int xSwatchHeight = 1; xSwatchHeight <= pxSwatchHeight; xSwatchHeight++ ){
						aiRGB[(xSwatchHeight-1)*pxSwatchWidth + xBand] = rgb;
					}
				}
			} else {
				for( int xSwatchHeight = 1; xSwatchHeight <= pxSwatchHeight; xSwatchHeight++ ){
					float fProportion = (float)xSwatchHeight / (float)pxSwatchHeight;
					int rgb = rgbGetForRange(fProportion, xRange1);
					int yBand;
				    if( zAscending ){
						yBand = pxSwatchHeight - xSwatchHeight; // low bands should be at the bottom of the image
					} else {
						yBand = xSwatchHeight - 1; // low bands should be at the top of the image
					}
					for( int xSwatchWidth = 1; xSwatchWidth <= pxSwatchWidth; xSwatchWidth++ ){
						aiRGB[yBand*pxSwatchWidth + (xSwatchWidth-1)] = rgb;
					}
				}
			}
			bi.setRGB(0, 0, pxSwatchWidth, pxSwatchHeight, aiRGB, 0, pxSwatchWidth);
		}
		if( zBorder ){
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, pxSwatchWidth-1, pxSwatchHeight-1);
		}
		return bi;
	}
	boolean setStandardColors(int eCOLOR_STYLE, StringBuffer sbError){
		vGenerateCG_Band(0.0f, 1.0f, 0xFFDFFFFF, 0xFF00FFFF, COLOR_STEP_SynchronizedDown, -1, 0xFF, 0xFF);
		return true;
	}

	void vGenerateCG_Banded(int ctBands){
		if( ctBands < 1 ) return;
		DataParameters dp = Panel_View_Plot.getDataParameters();

		// vary hue and saturation (go from magenta to red, ie down)
		int iHue = 0xDF; int fHueInterval = (int)((float)0xDF / (float)(ctBands - 1)); // subtract one so that endpoints are both included
		int iSat = 0x7F; int fSatInterval = (int)((float)(0xFF - 0x7F) / ((float)(ctBands - 1)) * 3f);
		StringBuffer sbError = new StringBuffer(80);
		float fProFrom = 0;
		boolean zFireEvent = true;
		for( int xBand = 1; xBand <= ctBands; xBand++ ){
			int hsbBand = 0xFF << 24 | iHue << 16 | iSat << 8 | 0xFF;
			float fProTo = (float)xBand / (float)ctBands;
			float fRange;
			switch( miDataType ){
				case DATA_TYPE_Byte:
				case DATA_TYPE_Int16:
					fRange = (float)(dp.getDataTo_short() - dp.getDataFrom_short());
					short shFrom = (short)((float)dp.getDataFrom_short() + fRange * fProFrom);
					short shTo = (short)((float)dp.getDataFrom_short() + fRange * fProTo);
					this.rangeAdd(shFrom, shTo, hsbBand, hsbBand, COLOR_STEP_SynchronizedUp, -1, -1, -1, -1, zFireEvent, sbError);
					break;
				case DATA_TYPE_UInt16:
				case DATA_TYPE_Int32:
					fRange = (float)(dp.getDataTo_int() - dp.getDataFrom_int());
					int iFrom = (int)((float)dp.getDataFrom_int() + fRange * fProFrom);
					int iTo = (int)((float)dp.getDataFrom_int() + fRange * fProTo);
					this.rangeAdd(iFrom, iTo, hsbBand, hsbBand, COLOR_STEP_SynchronizedUp, -1, -1, -1, -1, zFireEvent, sbError);
					break;
				case DATA_TYPE_UInt32:
					long nRange = dp.getDataTo_long() - dp.getDataFrom_long();
					long nFrom = (long)(dp.getDataFrom_long() + nRange * fProFrom);
					long nTo = (long)(dp.getDataFrom_long() + nRange * fProTo);
					this.rangeAdd(nFrom, nTo, hsbBand, hsbBand, COLOR_STEP_SynchronizedUp, -1, -1, -1, -1, zFireEvent, sbError);
					break;
				case DATA_TYPE_Float32:
					fRange = dp.getDataTo_float() - dp.getDataFrom_float();
					float fFrom = dp.getDataFrom_float() + fRange * fProFrom;
					float fTo = dp.getDataFrom_float() + fRange * fProTo;
					this.rangeAdd(fFrom, fTo, hsbBand, hsbBand, COLOR_STEP_SynchronizedUp, -1, -1, -1, -1, zFireEvent, sbError);
					break;
				case DATA_TYPE_Float64:
					double dRange = (float)(dp.getDataTo_double() - dp.getDataFrom_double());
					double dFrom = dp.getDataFrom_double() + (double)(dRange * fProFrom);
					double dTo = dp.getDataFrom_double() + (double)(dRange* fProTo);
					this.rangeAdd(dFrom, dTo, hsbBand, hsbBand, COLOR_STEP_SynchronizedUp, -1, -1, -1, -1, zFireEvent, sbError);
					break;
			}
			fProFrom = fProTo;
			iHue -= fHueInterval;
			iSat += fSatInterval;
			if( iSat > 0xFF ) iSat = 0x7f;
		}
	}

	public static Color[] getColorBands1_Color( int ctBands ){
		int[] ahsbBands = getColorBands1_HSB( ctBands );
		Color[] aColors = new Color[ ctBands + 1 ];
		for( int xBand = 1; xBand <= ctBands; xBand++ ){
			aColors[xBand] = new Color( iHSBtoRGB(ahsbBands[xBand]), true );
		}
		return aColors;
	}

	public static int[] getColorBands1_HSB( int ctBands ){
		int[] ahsbBands = new int [ctBands + 1];
		int iHue = 0; int fHueInterval = (int)((float)0xFF / (float)ctBands);
		int iSat = 0x7F; int fSatInterval = (int)((float)0xFF / (float)ctBands);
		StringBuffer sbError = new StringBuffer(80);
		float fProFrom = 0;
		for( int xBand = 1; xBand <= ctBands; xBand++ ){
			int hsbBand = 0xFF << 24 | iHue << 16 | iSat << 8 | 0xFF;
			ahsbBands[xBand] = hsbBand;
			iHue += fHueInterval;
			iSat += fSatInterval;
			if( iSat < 0x7f ) iSat = 0x7f;
		}
		return ahsbBands;
	}

	void vGenerateCG_GrayScale(){
		vGenerateCG_Band( 0.0f, 1.0f, 0xFF000000, 0xFF0000FF, COLOR_STEP_SynchronizedUp, 0xFF, 0xFF, -1 );
	}

	void vGenerateCG_BWPrinter(){
		vGenerateCG_Band( 0.0f, 1.0f, 0x00000000, 0xFFFFFFFF, COLOR_STEP_SynchronizedUp, -1, -1, -1 );
	}

	void vGenerateCG_Rainbow_Full(){
		vGenerateCG_Band( 0.0f, 1.0f, 0xFFFFFFFF, 0xFF00FFFF, COLOR_STEP_SynchronizedDown, -1, 0xFF, 0xFF );
	}
	void vGenerateCG_Rainbow_Little(){
		vGenerateCG_Band( 0.0f, 1.0f, 0xFFDFFFFF, 0xFF00FFFF, COLOR_STEP_SynchronizedDown, -1, 0xFF, 0xFF );
	}
	void vGenerateCG_Rainbow_Weighted(){
		vGenerateCG_Band( 0.0f, 1.0f/7.0f, 0xFFDFFFFF, 0xFF90FFFF, COLOR_STEP_SynchronizedDown, -1, 0xFF, 0xFF );
		vGenerateCG_Band( 1.0f/7.0f, 3.0f/7.0f, 0xFF8FFFFF, 0xFF70FFFF, COLOR_STEP_SynchronizedDown, -1, 0xFF, 0xFF );
		vGenerateCG_Band( 3.0f/7.0f, 4.0f/7.0f, 0xFF6FFFFF, 0xFF40FFFF, COLOR_STEP_SynchronizedDown, -1, 0xFF, 0xFF );
		vGenerateCG_Band( 4.0f/7.0f, 6.0f/7.0f, 0xFF3FFFFF, 0xFF20FFFF, COLOR_STEP_SynchronizedDown, -1, 0xFF, 0xFF );
		vGenerateCG_Band( 6.0f/7.0f, 1.0f, 0xFF1FFFFF, 0xFF00FFFF, COLOR_STEP_SynchronizedDown, -1, 0xFF, 0xFF );
	}
	void vGenerateCG_MultiHue(int ctHues){
		if( ctHues == 2 ){
			vGenerateCG_Band( 0.0f, 0.5f, 0xFFCFFFA0, 0xFFA0FFFF, COLOR_STEP_SynchronizedUp, -1, 0xFF, -1 );
			vGenerateCG_Band( 0.5f, 1.0f, 0xFFD0FFFF, 0xFFFFFFA0, COLOR_STEP_SynchronizedUp, -1, 0xFF, -1 );
		} else if( ctHues == 3 ) {
			vGenerateCG_Band( 0.0f, 0.5f, 0xFF9FFFA0, 0xFF58FFFF, COLOR_STEP_SynchronizedDown, -1, 0xFF, -1 );
			vGenerateCG_Band( 0.5f, 1.0f, 0xFF57FFFF, 0xFF00FFA0, COLOR_STEP_SynchronizedDown, -1, 0xFF, -1 );
		}
	}
	void vGenerateCG_Band(float fProFrom, float fProTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSat, int iBri){
		DataParameters dp = Panel_View_Plot.getDataParameters();
		StringBuffer sbError = new StringBuffer(250);
		boolean zFireEvent = true;
		switch( miDataType ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:{
				short dpFrom = dp.getDataFrom_short();
				short range = (short)(dp.getDataTo_short() - dp.getDataFrom_short());
				short from  = (short)(dpFrom + (short)(range * fProFrom));
				short to    = (short)(dpFrom + (short)(range * fProTo));
				if( !rangeAdd(from, to, iColorFrom, iColorTo, iColorStep, iHue, iSat, iBri, 0xFF, zFireEvent, sbError) ){
					ApplicationController.vShowError("Failed to generate band: " + sbError);
				}}
				break;
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:{
				int dpFrom = dp.getDataFrom_int();
				int range = (int)(dp.getDataTo_int() - dp.getDataFrom_int());
				int from  = (int)(dpFrom + (int)(range * fProFrom));
				int to    = (int)(dpFrom + (int)(range * fProTo));
				if( !rangeAdd(from, to, iColorFrom, iColorTo, iColorStep, iHue, iSat, iBri, 0xFF, zFireEvent, sbError) ){
					ApplicationController.vShowError("Failed to generate band: " + sbError);
				}}
				break;
			case DATA_TYPE_UInt32:{
				long dpFrom = dp.getDataFrom_long();
				long range = (long)(dp.getDataTo_long() - dp.getDataFrom_long());
				long from  = (long)(dpFrom + (long)(range * fProFrom));
				long to    = (long)(dpFrom + (long)(range * fProTo));
				if( !rangeAdd(from, to, iColorFrom, iColorTo, iColorStep, iHue, iSat, iBri, 0xFF, zFireEvent, sbError) ){
					ApplicationController.vShowError("Failed to generate band: " + sbError);
				}}
				break;
			case DATA_TYPE_Float32:{
				float dpFrom = dp.getDataFrom_float();
				float range = (float)(dp.getDataTo_float() - dp.getDataFrom_float());
				float from  = (float)(dpFrom + (float)(range * fProFrom));
				float to    = (float)(dpFrom + (float)(range * fProTo));
				if( !rangeAdd(from, to, iColorFrom, iColorTo, iColorStep, iHue, iSat, iBri, 0xFF, zFireEvent, sbError) ){
					ApplicationController.vShowError("Failed to generate band: " + sbError);
				}}
				break;
			case DATA_TYPE_Float64:{
				double dpFrom = dp.getDataFrom_double();
				double range = (double)(dp.getDataTo_double() - dp.getDataFrom_double());
				double from  = (double)(dpFrom + (double)(range * fProFrom));
				double to    = (double)(dpFrom + (double)(range * fProTo));
				if( !rangeAdd(from, to, iColorFrom, iColorTo, iColorStep, iHue, iSat, iBri, 0xFF, zFireEvent, sbError) ){
					ApplicationController.vShowError("Failed to generate band: " + sbError);
				}}
				break;
			default: return;
		}
	}

	// The CS is proportional if its ranges go from 0 to 1
	boolean zConvertToProportional(){
		boolean zIncludes0 = false;
		boolean zIncludes1 = false;
		for( int xRange = 1; xRange <= mctRanges; xRange++ ){
			switch(miDataType){
				case DATA_TYPE_Byte:
				case DATA_TYPE_Int16:
					if( ashDataFrom[xRange] < 0 || ashDataFrom[xRange] > 1 ) return false;
					if( ashDataTo[xRange] < 0 || ashDataTo[xRange] > 1 ) return false;
					if( ashDataTo[xRange] == 0 ) zIncludes0 = true;
					if( ashDataFrom[xRange] == 0 ) zIncludes0 = true;
					if( ashDataTo[xRange] == 0 ) zIncludes0 = true;
					if( ashDataFrom[xRange] == 1 ) zIncludes1 = true;
					if( ashDataTo[xRange] == 1 ) zIncludes1 = true;
					break;
				case DATA_TYPE_UInt16:
				case DATA_TYPE_Int32:
					if( aiDataFrom[xRange] < 0 || aiDataFrom[xRange] > 1 ) return false;
					if( aiDataTo[xRange] < 0 || aiDataTo[xRange] > 1 ) return false;
					if( aiDataTo[xRange] == 0 ) zIncludes0 = true;
					if( aiDataFrom[xRange] == 0 ) zIncludes0 = true;
					if( aiDataTo[xRange] == 0 ) zIncludes0 = true;
					if( aiDataFrom[xRange] == 1 ) zIncludes1 = true;
					if( aiDataTo[xRange] == 1 ) zIncludes1 = true;
					break;
				case DATA_TYPE_UInt32:
					if( anDataFrom[xRange] < 0 || anDataFrom[xRange] > 1 ) return false;
					if( anDataTo[xRange] < 0 || anDataTo[xRange] > 1 ) return false;
					if( anDataTo[xRange] == 0 ) zIncludes0 = true;
					if( anDataFrom[xRange] == 0 ) zIncludes0 = true;
					if( anDataTo[xRange] == 0 ) zIncludes0 = true;
					if( anDataFrom[xRange] == 1 ) zIncludes1 = true;
					if( anDataTo[xRange] == 1 ) zIncludes1 = true;
					break;
				case DATA_TYPE_Float32:
					if( afDataFrom[xRange] < 0 || afDataFrom[xRange] > 1 ) return false;
					if( afDataTo[xRange] < 0 || afDataTo[xRange] > 1 ) return false;
					if( afDataTo[xRange] == 0 ) zIncludes0 = true;
					if( afDataFrom[xRange] == 0 ) zIncludes0 = true;
					if( afDataTo[xRange] == 0 ) zIncludes0 = true;
					if( afDataFrom[xRange] == 1 ) zIncludes1 = true;
					if( afDataTo[xRange] == 1 ) zIncludes1 = true;
					break;
				case DATA_TYPE_Float64:
					if( adDataFrom[xRange] < 0 || adDataFrom[xRange] > 1 ) return false;
					if( adDataTo[xRange] < 0 || adDataTo[xRange] > 1 ) return false;
					if( adDataTo[xRange] == 0 ) zIncludes0 = true;
					if( adDataFrom[xRange] == 0 ) zIncludes0 = true;
					if( adDataTo[xRange] == 0 ) zIncludes0 = true;
					if( adDataFrom[xRange] == 1 ) zIncludes1 = true;
					if( adDataTo[xRange] == 1 ) zIncludes1 = true;
					break;
			}
		}
		return (zIncludes0 && zIncludes1);
	}

// todo we are redundantly calculating the data proportion for each range
// on every data point; instead we should store the data proportion for each
// range when the range is set
	int[] aiRender( float[] afData, int iDataWidth, int iDataHeight, int pxWidth, int pxHeight, boolean zAverage, StringBuffer sbError ){
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_Float32 ){
			ApplicationController.getInstance().vShowError("cannot render float data for type " + DAP.getType_String(getDataType()));
			return null;
		}
		if( !Utility.zMemoryCheck(pxWidth * pxHeight, 4, sbError) ) return null;
		int[] aiRGB = new int[pxWidth * pxHeight];
		final boolean zScale = ( iDataWidth != pxWidth || iDataHeight != pxHeight );
		int xDataWidth, xDataHeight;
		int xRGB = 0;
		int xData = 0;
		int ctMissingRendered = 0;
		for( int xWidth = 0; xWidth < pxWidth; xWidth++ ){
			for( int xHeight = 0; xHeight < pxHeight; xHeight++ ){
				if( zScale ){
					xDataWidth = xWidth * iDataWidth / pxWidth;
					xDataHeight = xHeight * iDataHeight / pxHeight;
				} else {
					xDataWidth = xWidth;
					xDataHeight = xHeight;
				}
				int xCartesianHeight = pxHeight - xHeight - 1; // cartesian re-orientation
				xRGB = pxWidth * xCartesianHeight + xWidth;
				xData = iDataWidth * xDataHeight + xDataWidth;
				int xRange = 0;
	Ranges:
				while(true){
					xRange++;
					if( xRange > mctRanges ){
						aiRGB[xRGB] = 0xFFFFFFFF; // no color is defined for this value
						break;
					}
					float fDataValue = afData[xData];
					for(int xMissing = 1; xMissing <= mctMissing; xMissing++){
						if( fDataValue == afMissing1[xMissing] ){
							aiRGB[xRGB] = mrgbMissingColor;
							break Ranges;
						}
					}
					if( fDataValue >= afDataFrom[xRange] && fDataValue <= afDataTo[xRange] ){
						float fRangeWidth = afDataTo[xRange] - afDataFrom[xRange];
						float fDataProportion;
						if( fRangeWidth == 0 )
							fDataProportion = -1;
						else
							fDataProportion = (fDataValue - afDataFrom[xRange])/ fRangeWidth;
						aiRGB[xRGB] = rgbGetForRange( fDataProportion, xRange );
						break; // found color
					}
				}
			}
		}
		return aiRGB;
	}

	// averaging not implemented currently
	// Cartesian Presentation: the data origin is in document orientation. In other words the
	// the first element is at the upper left. When plotted a cartesian coordinate system is
	// used with the first element at the lower left.
	int[] aiRender( double[] adData, int iDataWidth, int iDataHeight, int pxWidth, int pxHeight, boolean zAverage, StringBuffer sbError ){
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_Float64 ){
			ApplicationController.getInstance().vShowError("cannot render double data for type " + DAP.getType_String(getDataType()));
			return null;
		}
		if( !Utility.zMemoryCheck(pxWidth * pxHeight, 4, sbError) ) return null;
		int[] aiRGB = new int[pxWidth * pxHeight];
		final boolean zScale = ( iDataWidth != pxWidth || iDataHeight != pxHeight );
		int xDataWidth, xDataHeight;
		int xRGB = 0;
		int xData = 0;
		for( int xWidth = 0; xWidth < pxWidth; xWidth++ ){
			for( int xHeight = 0; xHeight < pxHeight; xHeight++ ){
				if( zScale ){
					xDataWidth = xWidth * iDataWidth / pxWidth;
					xDataHeight = xHeight * iDataHeight / pxHeight;
				} else {
					xDataWidth = xWidth;
					xDataHeight = xHeight;
				}
				int xCartesianHeight = pxHeight - xHeight - 1; // cartesian re-orientation
				xRGB = pxWidth * xCartesianHeight + xWidth;
				xData = iDataWidth * xDataHeight + xDataWidth;
				int xRange = 0;
	Ranges:
				while(true){
					xRange++;
					if( xRange > mctRanges ){
						aiRGB[xRGB] = 0xFFFFFFFF; // no color is defined for this value
						break;
					}
					double dDataValue = adData[xData];
					for(int xMissing = 1; xMissing <= mctMissing; xMissing++){
						if( dDataValue == adMissing1[xMissing] ){
							aiRGB[xRGB] = mrgbMissingColor;
							break Ranges;
						}
					}
					if( dDataValue >= adDataFrom[xRange] && dDataValue <= adDataTo[xRange] ){
						double dRangeWidth = adDataTo[xRange] - adDataFrom[xRange];
						float fDataProportion;
						if( dRangeWidth == 0 )
							fDataProportion = -1;
						else
							fDataProportion = (float)((dDataValue - adDataFrom[xRange])/ (float)dRangeWidth);
						aiRGB[xRGB] = rgbGetForRange( fDataProportion, xRange );
						break;
					}
				}
			}
		}
		return aiRGB;
	}

	int[] aiRender( short[] ashData, int iDataWidth, int iDataHeight, int pxWidth, int pxHeight, boolean zAverage, StringBuffer sbError ){
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_Byte && iDataType != DATA_TYPE_Int16 ){
			ApplicationController.getInstance().vShowError("cannot render short data for type " + DAP.getType_String(getDataType()));
			return null;
		}
		if( !Utility.zMemoryCheck(pxWidth * pxHeight, 4, sbError) ) return null;
		int[] aiRGB = new int[pxWidth * pxHeight];
		final boolean zScale = ( iDataWidth != pxWidth || iDataHeight != pxHeight );
		int xDataWidth, xDataHeight;
		int xData = 0;
		int xRGB = 0;
		for( int xWidth = 0; xWidth < pxWidth; xWidth++ ){
			for( int xHeight = 0; xHeight < pxHeight; xHeight++ ){
				if( zScale ){
					xDataWidth = xWidth * iDataWidth / pxWidth;
					xDataHeight = xHeight * iDataHeight / pxHeight;
				} else {
					xDataWidth = xWidth;
					xDataHeight = xHeight;
				}
				int xCartesianHeight = pxHeight - xHeight - 1; // cartesian re-orientation
				xRGB = pxWidth * xCartesianHeight + xWidth;
				xData = iDataWidth * xDataHeight + xDataWidth;
				int xRange = 0;
	Ranges:
				while(true){
					xRange++;
					if( xRange > mctRanges ){
						aiRGB[xRGB] = 0xFFFFFFFF; // no color is defined for this value
						break;
					}
					short shDataValue = ashData[xData];
					for(int xMissing = 1; xMissing <= mctMissing; xMissing++){
						if( shDataValue == ashMissing1[xMissing] ){
							aiRGB[xRGB] = mrgbMissingColor;
							break Ranges;
						}
					}
					if( shDataValue >= ashDataFrom[xRange] && shDataValue <= ashDataTo[xRange] ){
						int iRangeWidth = ashDataTo[xRange] - ashDataFrom[xRange];
						float fDataProportion;
						if( iRangeWidth == 0 )
							fDataProportion = -1;
						else
							fDataProportion = (float)(shDataValue - ashDataFrom[xRange])/ (float)iRangeWidth;
						aiRGB[xRGB] = rgbGetForRange( fDataProportion, xRange );
						break;
					}
				}
			}
		}
		return aiRGB;
	}
	int[] aiRender( int[] aiData, int iDataWidth, int iDataHeight, int pxWidth, int pxHeight, boolean zAverage, StringBuffer sbError ){
		if( !Utility.zMemoryCheck(pxWidth * pxHeight, 4, sbError) ) return null;
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_Int32 && iDataType != DATA_TYPE_UInt16 ){
			sbError.append("cannot render int data for type " + DAP.getType_String(iDataType));
			return null;
		}
		int[] aiRGB = new int[pxWidth * pxHeight];
		final boolean zScale = ( iDataWidth != pxWidth || iDataHeight != pxHeight );
		int xDataWidth, xDataHeight;
		int xRGB = 0;
		int xData = 0;
		for( int xWidth = 0; xWidth < pxWidth; xWidth++ ){
			for( int xHeight = 0; xHeight < pxHeight; xHeight++ ){
				if( zScale ){
					xDataWidth = xWidth * iDataWidth / pxWidth;
					xDataHeight = xHeight * iDataHeight / pxHeight;
				} else {
					xDataWidth = xWidth;
					xDataHeight = xHeight;
				}
				int xCartesianHeight = pxHeight - xHeight - 1; // cartesian re-orientation
				xRGB = pxWidth * xCartesianHeight + xWidth;
				xData = iDataWidth * xDataHeight + xDataWidth;
				int xRange = 0;
Ranges:
				while(true){
					xRange++;
					if( xRange > mctRanges ){
						aiRGB[xRGB] = 0xFFFFFFFF; // no color is defined for this value
						break;
					}
					int iDataValue = aiData[xData];
					for(int xMissing = 1; xMissing <= mctMissing; xMissing++){
						if( iDataValue == aiMissing1[xMissing] ){
							aiRGB[xRGB] = mrgbMissingColor;
							break Ranges;
						}
					}
					if( iDataValue >= aiDataFrom[xRange] && iDataValue <= aiDataTo[xRange] ){
						int iRangeWidth = aiDataTo[xRange] - aiDataFrom[xRange];
						float fDataProportion;
						if( iRangeWidth == 0 )
							fDataProportion = -1;
						else
							fDataProportion = ((float)(iDataValue - aiDataFrom[xRange]))/ (float)iRangeWidth;
						aiRGB[xRGB] = rgbGetForRange( fDataProportion, xRange );
						break;
					}
				}
			}
		}
		return aiRGB;
	}
	int[] aiRender( long[] anData, int iDataWidth, int iDataHeight, int pxWidth, int pxHeight, boolean zAverage, StringBuffer sbError ){
		if( !Utility.zMemoryCheck(pxWidth * pxHeight, 4, sbError) ) return null;
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_UInt32 ){
			sbError.append("cannot render long data for type " + DAP.getType_String(iDataType));
			return null;
		}
		int[] aiRGB = new int[pxWidth * pxHeight];
		final boolean zScale = ( iDataWidth != pxWidth || iDataHeight != pxHeight );
		int xDataWidth, xDataHeight;
		int xRGB = 0;
		int xData = 0;
		for( int xWidth = 0; xWidth < pxWidth; xWidth++ ){
			for( int xHeight = 0; xHeight < pxHeight; xHeight++ ){
				if( zScale ){
					xDataWidth = xWidth * iDataWidth / pxWidth;
					xDataHeight = xHeight * iDataHeight / pxHeight;
				} else {
					xDataWidth = xWidth;
					xDataHeight = xHeight;
				}
				int xCartesianHeight = pxHeight - xHeight - 1; // cartesian re-orientation
				xRGB = pxWidth * xCartesianHeight + xWidth;
				xData = iDataWidth * xDataHeight + xDataWidth;
				int xRange = 0;
Ranges:
				while(true){
					xRange++;
					if( xRange > mctRanges ){
						aiRGB[xRGB] = 0xFFFFFFFF; // no color is defined for this value
						break;
					}
					long nDataValue = anData[xData];
					for(int xMissing = 1; xMissing <= mctMissing; xMissing++){
						if( nDataValue == anMissing1[xMissing] ){
							aiRGB[xRGB] = mrgbMissingColor;
							break Ranges;
						}
					}
					if( nDataValue >= anDataFrom[xRange] && nDataValue <= anDataTo[xRange] ){
						long nRangeWidth = anDataTo[xRange] - anDataFrom[xRange];
						float fDataProportion;
						if( nRangeWidth == 0 )
							fDataProportion = -1;
						else
							fDataProportion = (float)((nDataValue - anDataFrom[xRange])/ nRangeWidth);
						aiRGB[xRGB] = rgbGetForRange( fDataProportion, xRange );
						break;
					}
				}
			}
		}
		return aiRGB;
	}
//	void vGenerateProportionalRanges( double[] adData ){
//		if( !mzProportional ){
//			ApplicationController.getInstance().vShowError("System error: attempt to generate proportional ranges when CS is not proportional");
//			return;
//		}
//		double dMax = Double.MIN_VALUE; double dMin = Double.MAX_VALUE;
//		int ctData = adData.length;
//		for( int xData = 0; xData < ctData; xData++ ){
//			if( adData[xData] < dMin ) dMin = adData[xData];
//			if( adData[xData] > dMax ) dMax = adData[xData];
//		}
//		if( ctData == 0 ){ dMin = 0; dMax = 1; }
//		double dRange = (double)(dMax - dMin);
//		for( int xRange = 1; xRange <= mctRanges; xRange++ ){
//			if( afDataFrom_Proportional[xRange] == 0 ){ // the checks against zero and one are to ensure that the endpoints are exact and not affected by rounding
//				adDataFrom[xRange] = dMin;
//			} else {
//				adDataFrom[xRange] = dMin + (double)afDataFrom_Proportional[xRange]*dRange;
//			}
//			if( afDataTo_Proportional[xRange] == 1 ){
//				adDataTo[xRange] = dMax;
//			} else {
//				adDataTo[xRange] = dMin + (double)afDataFrom_Proportional[xRange]*dRange;
//			}
//		}
//		adMissing1 = new double[mctMissing + 1];
//		for( int xMissing = 1; xMissing <= mctMissing; xMissing++ ){
//			adMissing1[xMissing] = (double)afMissing1[xMissing];
//		}
//	}
//
//	void vGenerateProportionalRanges( float[] afData ){
//		if( !mzProportional ){
//			ApplicationController.getInstance().vShowError("System error: attempt to generate proportional ranges when CS is not proportional");
//			return;
//		}
//		float fMin = Float.MAX_VALUE; float fMax = Float.MAX_VALUE * -1;
//		int ctData = afData.length;
//		for( int xData = 0; xData < ctData; xData++ ){
//			if( afData[xData] < fMin ) fMin = afData[xData];
//			if( afData[xData] > fMax ) fMax = afData[xData];
//		}
//		if( ctData == 0 ){ fMin = 0; fMax = 1; }
//		float fRange = (float)(fMax - fMin);
//		for( int xRange = 1; xRange <= mctRanges; xRange++ ){
//			if( afDataFrom_Proportional[xRange] == 0 ){ // the checks against zero and one are to ensure that the endpoints are exact and not affected by rounding
//				afDataFrom[xRange] = fMin;
//			} else {
//				afDataFrom[xRange] = fMin + afDataFrom_Proportional[xRange]*fRange;
//			}
//			if( afDataTo_Proportional[xRange] == 1 ){
//				afDataTo[xRange] = fMax;
//			} else {
//				afDataTo[xRange] = fMin + afDataFrom_Proportional[xRange]*fRange;
//			}
//		}
//		// missing values are correct
//	}
//
//	void vGenerateProportionalRanges( int[] aiData ){
//		if( !mzProportional ){
//			ApplicationController.getInstance().vShowError("System error: attempt to generate proportional ranges when CS is not proportional");
//			return;
//		}
//		long nMin = Long.MAX_VALUE; long nMax = Long.MAX_VALUE * -1;
//		int ctData = aiData.length;
//		for( int xData = 0; xData < ctData; xData++ ){
//			long nDataValue;
//			nDataValue = (long)aiData[xData];
//			if( nDataValue < nMin ) nMin = nDataValue;
//			if( nDataValue > nMax ) nMax = nDataValue;
//		}
//		if( ctData == 0 ){ nMin = 0; nMax = 1; }
//		long nRange = nMax - nMin;
//		for( int xRange = 1; xRange <= mctRanges; xRange++ ){
//			if( afDataFrom_Proportional[xRange] == 0f ){ // the checks against zero and one are to ensure that the endpoints are exact and not affected by rounding
//				aiDataFrom[xRange] = (int)nMin;
//			} else {
//			    aiDataFrom[xRange] = (int)(nMin + (long)(afDataFrom_Proportional[xRange]*nRange));
//			}
//			if( afDataTo_Proportional[xRange] == 1.0f ){
//				aiDataTo[xRange] = (int)nMax;
//			} else {
//			    aiDataTo[xRange] = (int)(nMin + (long)(afDataFrom_Proportional[xRange]*nRange));
//			}
//		}
//		aiMissing1 = new int[mctMissing + 1];
//		for( int xMissing = 1; xMissing <= mctMissing; xMissing++ ){
//			aiMissing1[xMissing] = (int)afMissing1[xMissing];
//		}
//	}
//
//	void vGenerateProportionalRanges( short[] ashData ){
//		if( !mzProportional ){
//			ApplicationController.getInstance().vShowError("System error: attempt to generate proportional ranges when CS is not proportional");
//			return;
//		}
//		int iMin = Integer.MAX_VALUE; int iMax = Integer.MAX_VALUE * -1;
//		int ctData = ashData.length;
//		for( int xData = 0; xData < ctData; xData++ ){
//			int iDataValue;
//			iDataValue = (int)ashData[xData];
//			if( iDataValue < iMin ) iMin = iDataValue;
//			if( iDataValue > iMax ) iMax = iDataValue;
//		}
//		if( ctData == 0 ){ iMin = 0; iMax = 1; }
//		int iRange = iMax - iMin;
//		for( int xRange = 1; xRange <= mctRanges; xRange++ ){
//			if( afDataFrom_Proportional[xRange] == 0f ){ // the checks against zero and one are to ensure that the endpoints are exact and not affected by rounding
//				ashDataFrom[xRange] = (short)iMin;
//			} else {
//			    ashDataFrom[xRange] = (short)(iMin + (int)(afDataFrom_Proportional[xRange]*iRange));
//			}
//			if( afDataTo_Proportional[xRange] == 1.0f ){
//				ashDataTo[xRange] = (short)iMax;
//			} else {
//			    ashDataTo[xRange] = (short)(iMin + (int)(afDataFrom_Proportional[xRange]*iRange));
//			}
//		}
//		ashMissing1 = new short[mctMissing + 1];
//		for( int xMissing = 1; xMissing <= mctMissing; xMissing++ ){
//			ashMissing1[xMissing] = (short)afMissing1[xMissing];
//		}
//	}
//
//	void vGenerateProportionalRanges( long[] anData ){
//		if( !mzProportional ){
//			ApplicationController.getInstance().vShowError("System error: attempt to generate proportional ranges when CS is not proportional");
//			return;
//		}
//		long nMin = Long.MAX_VALUE; long nMax = Long.MAX_VALUE * -1;
//		int ctData = anData.length;
//		for( int xData = 0; xData < ctData; xData++ ){
//			long nDataValue;
//			nDataValue = anData[xData];
//			if( nDataValue < nMin ) nMin = nDataValue;
//			if( nDataValue > nMax ) nMax = nDataValue;
//		}
//		if( ctData == 0 ){ nMin = 0; nMax = 1; }
//		long nRange = nMax - nMin;
//		for( int xRange = 1; xRange <= mctRanges; xRange++ ){
//			if( afDataFrom_Proportional[xRange] == 0f ){ // the checks against zero and one are to ensure that the endpoints are exact and not affected by rounding
//				anDataFrom[xRange] = nMin;
//			} else {
//			    anDataFrom[xRange] = nMin + (long)(afDataFrom_Proportional[xRange]*nRange);
//			}
//			if( afDataTo_Proportional[xRange] == 1.0f ){
//				anDataTo[xRange] = nMax;
//			} else {
//			    anDataTo[xRange] = nMin + (long)(afDataFrom_Proportional[xRange]*nRange);
//			}
//		}
//		anMissing1 = new long[mctMissing + 1];
//		for( int xMissing = 1; xMissing <= mctMissing; xMissing++ ){
//			anMissing1[xMissing] = (long)afMissing1[xMissing];
//		}
//	}

	private int rgbGetForRange( float fDataProportion, int xRange ){
		int iRGB = 0;
		int iHue = aiHue[xRange];
		int iSaturation = aiSaturation[xRange];
		int iBrightness = aiBrightness[xRange];
		int iAlpha = aiAlpha[xRange];
		if( fDataProportion == -1 ){ // unitary color mapping using components
			if( iHue >= 0 && iSaturation >= 0 && iBrightness >= 0 && iAlpha >= 0 ){ // constant color
				int iHSB = (iAlpha << 24) | (iHue << 16) | (iSaturation << 8) | iBrightness;
				return iHSBtoRGBA( iHSB );
			} else {
				return iHSBtoRGBA( ahsbColorFrom[xRange] ); // by convention the color-from is used for unitary mappings
			}
		} else {
			long iColorFrom = ((long)ahsbColorFrom[xRange]) & 0xFFFFFFFF;
			long iColorTo   = ((long)ahsbColorTo[xRange]) & 0xFFFFFFFF;
			if( iColorFrom == iColorTo ) return iHSBtoRGBA(ahsbColorFrom[xRange]); // unitary color mapping
			if( iHue >= 0 && iSaturation >= 0 && iBrightness >= 0 && iAlpha >= 0 ){ // constant color
				return iHSBtoRGBA( iAlpha, iHue, iSaturation, iBrightness );
			} else {
				int eColorStep = aeColorStep[xRange];
				int hsbRaw;
				if( eColorStep == COLOR_STEP_SynchronizedUp || eColorStep == COLOR_STEP_ContinuousUp ){ // up
					if( iColorFrom < iColorTo ){
						hsbRaw = (int)(fDataProportion * (iColorTo - iColorFrom) + iColorFrom);
					} else {
						long nOffset = (long)(fDataProportion * (0xFFFFFFFFL - iColorFrom + iColorTo));
						if( 0xFFFFFFFFL - nOffset < iColorFrom ){ // must wrap
							hsbRaw = (int)(nOffset - (0xFFFFFFFFL - iColorFrom));
						} else {
							hsbRaw = (int)(iColorFrom + nOffset);
						}
					}
				} else {
					if( iColorFrom < iColorTo ){
						int iOffset = (int)(fDataProportion * (0xFFFFFFFF - iColorTo + iColorFrom));
						if( iOffset > iColorFrom ){ // must wrap
							hsbRaw = (int)(0xFFFFFFFF - iOffset - iColorFrom);
						} else {
							hsbRaw = (int)(iColorFrom - iOffset);
						}
					} else {
						hsbRaw = (int)(iColorFrom - (fDataProportion * (iColorFrom - iColorTo)));
					}
				}
				if( eColorStep == COLOR_STEP_SynchronizedUp ){ // scale any variable quantity
					if( iAlpha == -1 ){ // scale alpha
						int iAlphaFrom = (int)((iColorFrom & 0xFF000000L) >> 24);
						int iAlphaTo   = (int)((iColorTo & 0xFF000000L) >> 24);
						if( iAlphaFrom < iAlphaTo ){
							int iRange = iAlphaTo - iAlphaFrom;
							iAlpha = iAlphaFrom + (int)(iRange * fDataProportion);
						} else { // wrap
							int iRange = 0x100 - (iAlphaFrom - iAlphaTo);
							iAlpha = iAlphaFrom + (int)(iRange * fDataProportion);
							if( iAlpha > 0xFF ) iAlpha = iAlpha - 0x100;
						}
					}
					if( iHue == -1 ){ // scale Hue
						int iHueFrom   = (int)(iColorFrom & 0x00FF0000) >> 16;
						int iHueTo     = (int)(iColorTo & 0x00FF0000) >> 16;
						if( iHueFrom < iHueTo ){
							int iRange = iHueTo - iHueFrom;
							iHue = iHueFrom + (int)(iRange * fDataProportion);
						} else { // wrap
							int iRange = 0x100 - (iHueFrom - iHueTo);
							iHue = iHueFrom + (int)(iRange * fDataProportion);
							if( iHue > 0xFF ) iHue = iHue - 0x100;
						}
					}
					if( iSaturation == -1 ){ // scale Sat
						int iSatFrom   = ((int)iColorFrom & 0x0000FF00) >> 8;
						int iSatTo     = ((int)iColorTo & 0x0000FF00) >> 8;
						if( iSatFrom < iSatTo ){
							int iRange = iSatTo - iSatFrom;
							iSaturation = iSatFrom + (int)(iRange * fDataProportion);
						} else { // wrap
							int iRange = 0x100 - (iSatFrom - iSatTo);
							iSaturation = iSatFrom + (int)(iRange * fDataProportion);
							if( iSaturation > 0xFF ) iSaturation = iSaturation - 0x100;
						}
					}
					if( iBrightness == -1 ){ // scale Bri
						int iBriFrom   = ((int)iColorFrom & 0x000000FF);
						int iBriTo     = ((int)iColorTo & 0x000000FF);
						if( iBriFrom < iBriTo ){
							int iRange = iBriTo - iBriFrom;
							iBrightness = iBriFrom + (int)(iRange * fDataProportion);
						} else { // wrap
							int iRange = 0x100 - (iBriFrom - iBriTo);
							iBrightness = iBriFrom + (int)(iRange * fDataProportion);
							if( iBrightness > 0xFF ) iBrightness = iBrightness - 0x100;
						}
					}
				} else if( eColorStep == COLOR_STEP_SynchronizedDown ){
					if( iAlpha == -1 ){ // scale alpha
						int iAlphaFrom = (int)((iColorFrom & 0xFF000000L) >> 24);
						int iAlphaTo   = (int)((iColorTo & 0xFF000000L) >> 24);
						if( iAlphaFrom < iAlphaTo ){ // wrap
							int iRange = 0x100 - (iAlphaTo - iAlphaFrom);
							iAlpha = iAlphaFrom - (int)(iRange * fDataProportion);
							if( iAlpha < 0 ) iAlpha = 0x100 + iAlpha;
						} else {
							int iRange = iAlphaFrom - iAlphaTo;
							iAlpha = iAlphaFrom - (int)(iRange * fDataProportion);
						}
					}
					if( iHue == -1 ){ // scale Hue
						int iHueFrom   = (int)(iColorFrom & 0x00FF0000) >> 16;
						int iHueTo     = (int)(iColorTo & 0x00FF0000) >> 16;
						int iRange;
						if( iHueFrom < iHueTo ){ // wrap
							iRange = 0x100 - (iHueTo - iHueFrom);
							iHue = iHueFrom - (int)(iRange * fDataProportion);
							if( iHue < 0 ) iHue = 0x100 + iHue;
						} else {
							iRange = iHueFrom - iHueTo;
							iHue = iHueFrom - (int)(iRange * fDataProportion);
						}
					}
					if( iSaturation == -1 ){ // scale Sat
						int iSatFrom   = ((int)iColorFrom & 0x0000FF00) >> 8;
						int iSatTo     = ((int)iColorTo & 0x0000FF00) >> 8;
						if( iSatFrom < iSatTo ){ // wrap
							int iRange = 0x100 - (iSatTo - iSatFrom);
							iSaturation = iSatFrom - (int)(iRange * fDataProportion);
							if( iSaturation < 0 ) iSaturation = 0x100 + iSaturation;
						} else {
							int iRange = iSatFrom - iSatTo;
							iSaturation = iSatFrom - (int)(iRange * fDataProportion);
						}
					}
					if( iBrightness == -1 ){ // scale Bri
						int iBriFrom   = ((int)iColorFrom & 0x000000FF);
						int iBriTo     = ((int)iColorTo & 0x000000FF);
						if( iBriFrom < iBriTo ){ // wrap
							int iRange = 0x100 - (iBriTo - iBriFrom);
							iBrightness = iBriFrom - (int)(iRange * fDataProportion);
							if( iBrightness < 0 ) iBrightness = 0x100 + iBrightness;
						} else {
							int iRange = iBriFrom - iBriTo;
							iBrightness = iBriFrom - (int)(iRange * fDataProportion);
						}
					}
				}
				if( iAlpha != -1 )      hsbRaw = (hsbRaw & 0x00FFFFFF) | (iAlpha << 24);
				if( iHue != -1 )        hsbRaw = (hsbRaw & 0xFF00FFFF) | (iHue << 16);
				if( iSaturation != -1 ) hsbRaw = (hsbRaw & 0xFFFF00FF) | (iSaturation << 8);
				if( iBrightness != -1 ) hsbRaw = (hsbRaw & 0xFFFFFF00) | iBrightness;
				return iHSBtoRGBA( hsbRaw );
			}
		}
	}

    public static int iHSBtoRGBA( int iHSB ) {
		int iAlpha      = (int)((iHSB & 0xFF000000L) >> 24);
		int iHue        = (iHSB & 0x00FF0000) >> 16;
		int iSaturation = (iHSB & 0x0000FF00) >> 8;
		int iBrightness =  iHSB & 0x000000FF;
		return iHSBtoRGBA( iAlpha, iHue, iSaturation, iBrightness );
	}

    public static int iHSBtoRGB( int iHSB ) {
		int iHue        = (iHSB & 0x00FF0000) >> 16;
		int iSaturation = (iHSB & 0x0000FF00) >> 8;
		int iBrightness =  iHSB & 0x000000FF;
		return iHSBtoRGBA( iHSB, iHue, iSaturation, iBrightness );
	}

    public static int iHSBtoRGBA(int iAlpha, int ihue, int isaturation, int ibrightness) {
		float hue = (float)ihue/255;
		float saturation = (float)isaturation/255;
		float brightness = (float)ibrightness/255;
		int r = 0, g = 0, b = 0;
    	if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0f + 0.5f);
		} else {
			float h = (hue - (float)Math.floor(hue)) * 6.0f;
			float f = h - (float)java.lang.Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - (saturation * (1.0f - f)));
			switch ((int) h) {
				case 0:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (t * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 1:
					r = (int) (q * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 2:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (t * 255.0f + 0.5f);
					break;
				case 3:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (q * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 4:
					r = (int) (t * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 5:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (q * 255.0f + 0.5f);
					break;
			}
		}
		return (iAlpha << 24) | (r << 16) | (g << 8) | (b << 0);
    }

	public static int iRGBtoHSB( int rgb ){
		int iAlpha      = (int)((rgb & 0xFF000000L) >> 24);
		int iRed        = (rgb & 0x00FF0000) >> 16;
		int iGreen      = (rgb & 0x0000FF00) >> 8;
		int iBlue       =  rgb & 0x000000FF;
		return iRGBtoHSB(iAlpha, iRed, iGreen, iBlue);
	}

	// could be wrong check carefully by doing inversions
	public static int iRGBtoHSB(int alpha, int r, int g, int b) {
		int hue, saturation, brightness;
		int iLargestComponent = (r > g) ? r : g;
		if (b > iLargestComponent) iLargestComponent = b;
		int iSmallestComponent = (r < g) ? r : g;
		if (b < iSmallestComponent) iSmallestComponent = b;
		brightness = iLargestComponent;
		if (iLargestComponent == 0){
	        saturation = 0;
		    hue = 0;
		} else {
			float huec;
			float fSpread = (float) (iLargestComponent - iSmallestComponent);
			saturation = (int)(255 * fSpread / iLargestComponent);
			float redc = ((float) (iLargestComponent - r)) / fSpread;
			float greenc = ((float) (iLargestComponent - g)) / fSpread;
			float bluec = ((float) (iLargestComponent - b)) / fSpread;
			if (r == iLargestComponent)
			    huec = bluec - greenc;
		    else if (g == iLargestComponent)
			    huec = 2.0f + redc - bluec;
		    else
			    huec = 4.0f + greenc - redc;
		    huec = huec / 6.0f;
		    if (huec < 0)
			huec = huec + 1.0f;
			hue = (int)(huec * 255f);
		}
		return (alpha << 24) & ( hue << 16 ) & ( saturation << 8 ) & brightness;
    }

}

class Panel_ColorSpecification extends JPanel implements IRangeChanged {
	private Panel_Definition mParent;
	private ColorSpecification mcsDefault;
	private Panel_RangeEditor mRangeEditor;
	private ColorSpecification mColorSpecification;
	private ColorSpecificationRenderer mcsr;
	private javax.swing.JList mlistEntries;
	private ListModel lmBlank = new DefaultListModel();
	private DefaultComboBoxModel mCSListModel;
	private JComboBox jcbCS;
	private JRadioButton jrbGenerated;
	private JRadioButton jrbSelected;
	private JButton buttonNewCS;
	private JLabel mlabelDataType;
	private JLabel mlabelTypeMismatchWarning;

	private final ArrayList mlistEditorControls = new ArrayList(); // all the controls that have to be enabled or disabled depending on the automatic selection

	Panel_ColorSpecification( Panel_Definition parent ){

		mParent = parent;

		// create range editor
		mRangeEditor = new Panel_RangeEditor(this);
		mRangeEditor.set(null, 0);
		mlistEntries = new JList();
		mlistEntries.addListSelectionListener(
			new ListSelectionListener(){
				public void valueChanged(ListSelectionEvent lse) {
					if( lse.getValueIsAdjusting() ){ // this event gets fired multiple times on a mouse click--but the value "adjusts" only once
						int xEntry = mlistEntries.getSelectedIndex();
						if (xEntry < 0) {
							vRange_Edit(-1); // disable
						} else {
							vRange_Edit(xEntry);
						}
					}
				}
			}
		);
		mcsr = new ColorSpecificationRenderer();
		mlistEntries.setCellRenderer(mcsr);
		JScrollPane jspEntries = new JScrollPane(mlistEntries);
		JButton buttonAdd = new JButton("Add");
		buttonAdd.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					vRange_Add();
				}
			}
		);
		JButton buttonEdit = new JButton("Edit");
		buttonEdit.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					int xSelected0 = mlistEntries.getSelectedIndex();
					if( xSelected0 == -1 ) return; // nothing selected
					vRange_Edit( xSelected0 + 1 );
				}
			}
		);
		buttonEdit.setVisible(false);  // todo not in use convert to up/down buttons
		JButton buttonDel = new JButton("Del");
		buttonDel.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					int xSelected0 = mlistEntries.getSelectedIndex();
					if( xSelected0 == -1 ) return; // nothing selected
					if( xSelected0 == 0 ){
						JOptionPane.showMessageDialog(ApplicationController.getInstance().getAppFrame(), "The missing values specification cannot be deleted.");
					} else {
						vRange_Del( xSelected0 );
					}
				}
			}
		);
		JButton buttonDelAll = new JButton("Del All");
		buttonDelAll.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					StringBuffer sbError = new StringBuffer(80);
					if( !mColorSpecification.rangeRemoveAll(sbError) ){
						ApplicationController.vShowError("Failed to remove all ranges from CS: " + sbError);
					}
				}
			}
		);
		JButton buttonSave = new JButton("Save");
		buttonSave.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					vSave();
				}
			}
		);
		JButton buttonLoad = new JButton("Load");
		buttonLoad.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					vLoad();
				}
			}
		);

		// Range List
		JPanel panelRangeList = new JPanel();
		panelRangeList.setLayout(new java.awt.GridBagLayout());
		panelRangeList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Color Specification Ranges"));
		panelRangeList.setMinimumSize(new Dimension(300, 200));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.BOTH;
		gbc.gridx = 0; gbc.gridy = 0;
		gbc.gridwidth = 5;
		gbc.weightx = 1; gbc.weighty = .99;
		panelRangeList.add(jspEntries, gbc);
		gbc.gridy = 1; gbc.gridwidth = 1; gbc.weighty = .01; gbc.weightx = .20;
		panelRangeList.add(buttonAdd, gbc);
		gbc.gridx = 1;
		panelRangeList.add(buttonDel, gbc);
		gbc.gridx = 2;
		panelRangeList.add(buttonDelAll, gbc);
		gbc.gridx = 3;
		panelRangeList.add(buttonSave, gbc);
		gbc.gridx = 4;
		panelRangeList.add(buttonLoad, gbc);

		ActionListener listenSetCS =
			new ActionListener(){
			    public void actionPerformed( java.awt.event.ActionEvent ae ){
					vUpdate();
				}
			};

		//  main toggle
		jrbGenerated = new JRadioButton("Plots generate their own colors");
		jrbSelected = new JRadioButton("Plot uses: ");
		jrbGenerated.addActionListener(listenSetCS);
		jrbSelected.addActionListener(listenSetCS);

		// setup cs list
		mlabelDataType = new JLabel("Data Type: [no cs]");
		mlabelTypeMismatchWarning = new JLabel("Active Dataset: [no cs]");
		mlabelTypeMismatchWarning.setVisible(false);
		Styles.vApply(Styles.STYLE_RedWarning, mlabelTypeMismatchWarning);
		mCSListModel = new DefaultComboBoxModel();
		jcbCS = new JComboBox(mCSListModel);
		jcbCS.addActionListener(listenSetCS);

		// create default color specification (can only be done after range editor is created)
		StringBuffer sbError = new StringBuffer(80);
		mcsDefault = new ColorSpecification("Default", DAP.DATA_TYPE_Float32);
		if( mcsDefault.rangeAdd(0f, 1f, 0xFFDFFFFF, 0xFF00FFFF, ColorSpecification.COLOR_STEP_SynchronizedDown, -1, 0xFF, 0xFF, 0xFF, false, sbError) ){
			mcsDefault.setMissingColor(0xFF0000FF);
			mCSListModel.addElement(mcsDefault);
		} else {
			ApplicationController.getInstance().vShowError("failed to create default color specification: " + sbError);
		}

		// new CS
		buttonNewCS = new JButton("New CS");
		buttonNewCS.addActionListener(
			new java.awt.event.ActionListener(){
			    public void actionPerformed( java.awt.event.ActionEvent ae ){
					Panel_ColorSpecification.this.vNewCS();
				}
			}
		);

		// selection area
		ButtonGroup bgSelection = new ButtonGroup();
		jrbGenerated.setSelected(true); // default
		bgSelection.add(jrbGenerated);
		bgSelection.add(jrbSelected);
		JPanel panelSelection = new JPanel();
		panelSelection.setLayout(new java.awt.GridBagLayout());
		panelSelection.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Specification to Use"));
		gbc.fill = gbc.BOTH;
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);
		gbc.gridy = 1;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0; gbc.gridwidth = 5;
		panelSelection.add(jrbGenerated, gbc);
		gbc.gridx = 4; gbc.weightx = 1; gbc.gridwidth = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);
		gbc.gridy = 3;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelSelection.add(jrbSelected, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 1;
		panelSelection.add(jcbCS, gbc);
		gbc.gridx = 4; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 5; gbc.weightx = 0.0;
		panelSelection.add(buttonNewCS, gbc);
		gbc.gridx = 6; gbc.weightx = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);

		// data type
		gbc.gridy = 5;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelSelection.add(mlabelDataType, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 1;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 4; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 5; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc); // nothing in this slot
		gbc.gridx = 6; gbc.weightx = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);

		gbc.gridy = 6;
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 7; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);

		gbc.gridy = 7;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelSelection.add(mlabelTypeMismatchWarning, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 1;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 4; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 5; gbc.weightx = 0.0;
		panelSelection.add(Box.createHorizontalStrut(5), gbc); // nothing in this slot
		gbc.gridx = 6; gbc.weightx = 1;
		panelSelection.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelSelection.add(Box.createVerticalStrut(5), gbc);

		final JPanel panelColorGenerator = new JPanel();
		final JPanel panelCG_Rainbow = new JPanel();
		final JPanel panelCG_Banded = new JPanel();
		final JPanel panelCG_GrayScale = new JPanel();
		final JPanel panelCG_MultiHue = new JPanel();

		// set up rainbow
		final JRadioButton jrbFull = new JRadioButton("Full Spectrum");
		final JRadioButton jrbLittle = new JRadioButton("Little Rainbow");
		final JRadioButton jrbWeighted = new JRadioButton("Weighted Spectrum");
		final ButtonGroup bgRainbow = new ButtonGroup();
		bgRainbow.add(jrbFull);
		bgRainbow.add(jrbLittle);
		bgRainbow.add(jrbWeighted);
		jrbLittle.setSelected(true);
		panelCG_Rainbow.setLayout(new BoxLayout(panelCG_Rainbow, BoxLayout.Y_AXIS));
		panelCG_Rainbow.add(jrbFull);
		panelCG_Rainbow.add(jrbLittle);
		panelCG_Rainbow.add(jrbWeighted);
		panelCG_Rainbow.add(Box.createVerticalGlue());

		// set up banded
		final String[] asBandNumbers = new String[50];
		for( int x= 0; x< 50; x++)asBandNumbers[x] = Integer.toString(x+1);
		final JLabel labelBandCount = new JLabel("Band count:");
		final JComboBox jcbBandCount = new JComboBox(asBandNumbers);
		panelCG_Banded.add(labelBandCount);
		panelCG_Banded.add(Box.createHorizontalStrut(5));
		panelCG_Banded.add(jcbBandCount);

		// set up multi-hue options
		final String[] asHueNumbers = new String[2];
		asHueNumbers[0] = "2";
		asHueNumbers[1] = "3";
		final JLabel labelHueCount = new JLabel("Hue count:");
		final JComboBox jcbHueCount = new JComboBox(asHueNumbers);
		panelCG_MultiHue.add(labelHueCount);
		panelCG_MultiHue.add(Box.createHorizontalStrut(5));
		panelCG_MultiHue.add(jcbHueCount);

		panelColorGenerator.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Color Generator"));
		final String[] asColorGenerators = { "Rainbow", "Banded", "Gray Scale", "Multi-Hue" };
		final JComboBox jcbCG_selector = new JComboBox( asColorGenerators );
		jcbCG_selector.addActionListener(
			new java.awt.event.ActionListener(){
			    public void actionPerformed( java.awt.event.ActionEvent ae ){
					int iSelectedIndex = jcbCG_selector.getSelectedIndex();
					switch( iSelectedIndex ){
						case 0:
							panelCG_Rainbow.setVisible(true);
							panelCG_Banded.setVisible(false);
							panelCG_GrayScale.setVisible(false);
							panelCG_MultiHue.setVisible(false);
							break;
						case 1:
							panelCG_Rainbow.setVisible(false);
							panelCG_Banded.setVisible(true);
							panelCG_GrayScale.setVisible(false);
							panelCG_MultiHue.setVisible(false);
							break;
						case 2:
							panelCG_Rainbow.setVisible(false);
							panelCG_Banded.setVisible(false);
							panelCG_GrayScale.setVisible(true);
							panelCG_MultiHue.setVisible(false);
							break;
						case 3:
							panelCG_Rainbow.setVisible(false);
							panelCG_Banded.setVisible(false);
							panelCG_GrayScale.setVisible(false);
							panelCG_MultiHue.setVisible(true);
							break;
					}
					panelColorGenerator.revalidate();
				}
			}
		);
		final JButton buttonCG_create = new JButton("Create: ");
		buttonCG_create.addActionListener(
			new ActionListener(){
			    public void actionPerformed(ActionEvent event) {
					ColorSpecification cs = Panel_ColorSpecification.this.mColorSpecification;
					if( cs == null ) return;
					switch( jcbCG_selector.getSelectedIndex() ){
						case 0: // rainbow
							if( jrbFull.isSelected() ){
								cs.vGenerateCG_Rainbow_Full();
							} else if( jrbLittle.isSelected() ){
								cs.vGenerateCG_Rainbow_Little();
							} else if( jrbWeighted.isSelected() ){
								cs.vGenerateCG_Rainbow_Weighted();
							}
							break;
						case 1: // banded
							int iBandCount = jcbBandCount.getSelectedIndex() + 1;
							cs.vGenerateCG_Banded(iBandCount);
							break;
						case 2: // gray scale
							cs.vGenerateCG_GrayScale();
							break;
						case 3: // multi-hue
							int iHueCount = jcbHueCount.getSelectedIndex() + 2;
							cs.vGenerateCG_MultiHue(iHueCount);
							break;
					}
				}
			}
		);

		final JPanel panelCG_control = new JPanel();
		panelCG_control.setMaximumSize(new java.awt.Dimension(400, 25));
		panelCG_control.setLayout(new BoxLayout(panelCG_control, BoxLayout.X_AXIS));
		panelCG_control.add(buttonCG_create);
		panelCG_control.add(Box.createHorizontalStrut(5));
		panelCG_control.add( jcbCG_selector );
		panelColorGenerator.setLayout(new BoxLayout(panelColorGenerator, BoxLayout.Y_AXIS));
		panelColorGenerator.add( panelCG_control );
		panelColorGenerator.add( Box.createVerticalStrut(6) );
		panelColorGenerator.add( panelCG_Rainbow );
		panelColorGenerator.add( panelCG_Banded );
		panelColorGenerator.add( panelCG_GrayScale );
		panelColorGenerator.add( panelCG_MultiHue );
		panelCG_GrayScale.setVisible(false);
		panelCG_Banded.setVisible(false);
		panelCG_GrayScale.setVisible(false);
		panelCG_MultiHue.setVisible(false);

		final JPanel panelLeft = new JPanel();
		panelLeft.setLayout(new BoxLayout(panelLeft, BoxLayout.Y_AXIS));
		panelLeft.add( panelSelection );
		panelLeft.add( panelRangeList );

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.add(panelLeft);
		this.add(mRangeEditor);
		this.add(panelColorGenerator);

		// setup enabling
		mlistEditorControls.add( mRangeEditor );
		mlistEditorControls.add( mlistEntries );
		mlistEditorControls.add( buttonAdd );
		mlistEditorControls.add( buttonEdit );
		mlistEditorControls.add( buttonDel );
		mlistEditorControls.add( buttonDelAll );
		mlistEditorControls.add( buttonSave );
		mlistEditorControls.add( buttonLoad );
		mlistEditorControls.add( jcbBandCount );
		mlistEditorControls.add( jrbFull );
		mlistEditorControls.add( jrbLittle );
		mlistEditorControls.add( jrbWeighted );
		mlistEditorControls.add( jcbHueCount );
		mlistEditorControls.add( jcbBandCount );
		mlistEditorControls.add( jcbHueCount );
		mlistEditorControls.add( jcbCG_selector );
		mlistEditorControls.add( buttonCG_create );

		listenSetCS.actionPerformed( null );
	}

	private void vUpdate(){
		if( jrbGenerated.isSelected() ){
			setColorSpecification( null );
			setEditorEnabled( false );
		} else {
			int xSelection = jcbCS.getSelectedIndex();
			if( xSelection < 0 ){
				setColorSpecification( null );
				setEditorEnabled( false );
			} else {
				setColorSpecification((ColorSpecification)mCSListModel.getElementAt(xSelection));
				setEditorEnabled( true );
			}
		}
	}

	private void setEditorEnabled( boolean z ){
		for( int xControl = 0; xControl < mlistEditorControls.size(); xControl++ ){
			Component component = (Component)mlistEditorControls.get(xControl);
			component.setEnabled(z);
		}
	}

	StringBuffer msbError = new StringBuffer(80);

	ColorSpecification getDefaultCS(){ return mcsDefault; }

	public void setMissingValueText( String sMissingText ){
		if( mRangeEditor != null && mColorSpecification != null ){
			mRangeEditor.setMissingValuesText(sMissingText);
			vRangeChanged(0);
		}
	}

	public void vRangeChanged( int iRangeIndex1 ){
		if( mColorSpecification == null ){
			ApplicationController.getInstance().vShowError("Internal Error, range changed without cs existing");
			return;
		}
		if( iRangeIndex1 == 0 ){ // missing changed
			int iColorMissing = mRangeEditor.getColorMissing();
			String sMissingValues = mRangeEditor.getMissingValuesText();
			msbError.setLength(0);
			if( !mColorSpecification.setMissing(sMissingValues, iColorMissing, msbError) ){
				ApplicationController.vShowError("Unable to set missing: " + msbError);
			}
		} else { // range changed
			int iColorFrom = mRangeEditor.getColorFromHSB();
			int iColorTo = mRangeEditor.getColorToHSB();
			int eColorStep = mRangeEditor.getColorStep();
			int iHue = mRangeEditor.getHue();
			int iSat = mRangeEditor.getSat();
			int iBri = mRangeEditor.getBri();
			int iAlpha = mRangeEditor.getAlpha();
			String sDataFrom = mRangeEditor.getDataFromString();
			if( sDataFrom.length() == 0 ) sDataFrom = "0";
			String sDataTo = mRangeEditor.getDataToString();
			if( sDataTo.length() == 0 ) sDataTo = "1";
			msbError.setLength(0);
			switch( mColorSpecification.getDataType() ){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:
					try {
						short shDataFrom = Short.parseShort(sDataFrom);
						short shDataTo = Short.parseShort(sDataTo);
						if( !mColorSpecification.rangeSet( iRangeIndex1, shDataFrom, shDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, msbError) ){
							ApplicationController.vShowError("Unable to set range: " + msbError);
						}
					} catch(Exception ex) {
						ApplicationController.vShowError("Unable to understand data as short: [" + sDataFrom + " to " + sDataTo + "]");
					}
					return;
				case DAP.DATA_TYPE_Int32:
				case DAP.DATA_TYPE_UInt16:
					try {
						int iDataFrom = Integer.parseInt(sDataFrom);
						int iDataTo = Integer.parseInt(sDataTo);
						if( !mColorSpecification.rangeSet( iRangeIndex1, iDataFrom, iDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, msbError) ){
							ApplicationController.vShowError("Unable to set range: " + msbError);
						}
					} catch(Exception ex) {
						ApplicationController.vShowError("Unable to understand data as int: [" + sDataFrom + " to " + sDataTo + "]");
					}
					return;
				case DAP.DATA_TYPE_UInt32:
					try {
						long nDataFrom = Long.parseLong(sDataFrom);
						long nDataTo = Long.parseLong(sDataTo);
						if( !mColorSpecification.rangeSet( iRangeIndex1, nDataFrom, nDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, msbError) ){
							ApplicationController.vShowError("Unable to set range: " + msbError);
						}
					} catch(Exception ex) {
						ApplicationController.vShowError("Unable to understand data as long: [" + sDataFrom + " to " + sDataTo + "]");
					}
					return;
				case DAP.DATA_TYPE_Float32:
					try {
						float fDataFrom = Float.parseFloat(sDataFrom);
						float fDataTo = Float.parseFloat(sDataTo);
						if( !mColorSpecification.rangeSet( iRangeIndex1, fDataFrom, fDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, msbError) ){
							ApplicationController.vShowError("Unable to set range: " + msbError);
						}
					} catch(Exception ex) {
						ApplicationController.vShowError("Unable to understand data as float: [" + sDataFrom + " to " + sDataTo + "]");
					}
					return;
				case DAP.DATA_TYPE_Float64:
					try {
						double dDataFrom = Double.parseDouble(sDataFrom);
						double dDataTo = Double.parseDouble(sDataTo);
						if( !mColorSpecification.rangeSet( iRangeIndex1, dDataFrom, dDataTo, iColorFrom, iColorTo, eColorStep, iHue, iSat, iBri, iAlpha, msbError) ){
							ApplicationController.vShowError("Unable to set range: " + msbError);
						}
					} catch(Exception ex) {
						ApplicationController.vShowError("Unable to understand data as double: [" + sDataFrom + " to " + sDataTo + "]");
					}
					return;
				case DAP.DATA_TYPE_String:
				default:
					ApplicationController.getInstance().vShowError("Internal Error, unexpected data type in range change");
				return;
			}
		}
	}
	ColorSpecification getColorSpecification(){
		if( jrbSelected.isSelected() ) return mColorSpecification;
		return null;
	}
	ColorSpecification getColorSpecification_Showing(){
		return mColorSpecification;
	}
	void setColorSpecification( ColorSpecification cs ){
		mColorSpecification = cs;
		if( cs == null ){
			mlistEntries.setModel(lmBlank);
		} else {
			ListModel lmCurrent = mlistEntries.getModel();
			if( lmCurrent != null ) lmCurrent.removeListDataListener(mcsr);
			mlistEntries.setModel(cs);
			mcsr.vInitialize(cs);
			cs.addListDataListener(mcsr);
		}
		vUpdateInfo();
		mRangeEditor.set(cs, 0);
		Plot_Definition pd = this.mParent.getActivePlottingDefinition();
		if( pd != null ) pd.setColorSpecification(cs);
	}
	public void vUpdateInfo(){
		if( mColorSpecification == null ){
			mlistEntries.setEnabled(false);
			mlabelDataType.setText("[no CS selected]");
			mlabelTypeMismatchWarning.setVisible(false);
		} else {
			mlistEntries.setEnabled(true);
			int eDataType_Active = Panel_View_Plot.getDataParameters().getTYPE();
			int eDataType_CS     = mColorSpecification.getDataType();
			mlabelDataType.setText("Data Type: " + DAP.getType_String( eDataType_CS ));
			if( eDataType_Active == 0 || eDataType_Active == eDataType_CS ){
				mlabelTypeMismatchWarning.setVisible(false);
			} else {
				mlabelTypeMismatchWarning.setText("Active Dataset:  " + DAP.getType_String( eDataType_Active ) + "  (cannot use)");
				mlabelTypeMismatchWarning.setVisible(true);
			}
		}
	}
	void vNewCS(){
		DataParameters dp = Panel_View_Plot.getDataParameters();
		String sName = JOptionPane.showInputDialog(ApplicationController.getInstance().getAppFrame(), "Enter name for new color specification: ");
		if( sName == null ) return;
		StringBuffer sbError = new StringBuffer(80);
		Plot_Definition def = mParent.getActivePlottingDefinition();
		try {
			int eTYPE = dp.getTYPE();
			ColorSpecification csNew = new ColorSpecification(sName, eTYPE);
			int iMissingColor = 0xFF0000FF;
			csNew.setMissing(dp.getMissingEgg(), eTYPE, iMissingColor);
			csNew.vGenerateCG_Rainbow_Little(); // default style
			vAddCS( csNew );
			return; // all done
		} catch( Exception ex ) {
			Utility.vUnexpectedError(ex, sbError);
			ApplicationController.vShowError("Error attempting generate new CS from active definition: " + sbError);
			sbError.setLength(0);
		}
	}
	private void vAddCS( ColorSpecification cs ){
		mCSListModel.addElement( cs );
		jcbCS.setSelectedIndex(jcbCS.getItemCount() - 1);
		jrbSelected.setSelected(true);
	}
	void setRange( int xRange0 ){ jcbCS.setSelectedIndex(xRange0); }
	void vRange_Add(){
		if( mColorSpecification == null ) return;
		mColorSpecification.vGenerateCG_Rainbow_Little();
	}
	void vRange_Edit( int xEntry1 ){
		if( xEntry1 >= 0 )
			mRangeEditor.set(mColorSpecification, xEntry1);
	}
	void vRange_Del( int xEntry1 ){
		if( mColorSpecification == null ) return;
		if( xEntry1 == 0 ){
			mColorSpecification.setMissing(null, 0, 0);
		} else {
			StringBuffer sbError = new StringBuffer();
			if( !mColorSpecification.rangeRemove(xEntry1, sbError) ){
				ApplicationController.vShowError("Failed to remove item " + xEntry1 + ": " + sbError);
			}
		}
	}
	void vSave(){
		if( mColorSpecification == null ){
			// do nothing
		} else {
			StringBuffer sbError = new StringBuffer(80);
			if( !mColorSpecification.zStorage_save( sbError ) ){
				ApplicationController.vShowError(sbError.toString());
			}
		}
	}
	void vLoad(){
		StringBuffer sbError = new StringBuffer(80);
		String sInitializationString = "[loaded cs]";
		ColorSpecification cs = new ColorSpecification(sInitializationString, 0);
		if( cs.zStorage_load( sbError ) ){
			if( cs.getName() == null || cs.getName().equals(sInitializationString) ){
				return; // CS is blank, user cancelled
			}
			vAddCS( cs );
		} else {
			ApplicationController.vShowError(sbError.toString());
		}
	}
}

class ColorSpecificationRenderer extends JLabel implements ListCellRenderer, ListDataListener {
	protected Border mBorder_FocusCell = BorderFactory.createLineBorder(Color.cyan,1);
	protected Border mBorder_RegularCell = BorderFactory.createEmptyBorder(1,1,1,1);
	protected Insets mInsets = new Insets(1, 1, 1, 1);
	private int mTextX, mTextY;
	protected String sTextValue = "";
	ColorSpecification mColorSpecification;
	private Image mimageSwatch;
	private int mpxSwatchSize;
	private static int miColorCount = 12;
	public ColorSpecificationRenderer(){
		super();
	}
	public void vInitialize( ColorSpecification cs ){
		setOpaque(true);
		this.setMinimumSize(new Dimension(80, 20));
		this.setPreferredSize(new Dimension(400, 20));
		mColorSpecification = cs;
		mpxSwatchSize = cs.getSwatchWidth();
		mTextX = mInsets.left + mpxSwatchSize + 5;
	}
	public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean zCellHasFocus ){
		if( isSelected ){
			setForeground(list.getSelectionForeground());
			setBackground(list.getSelectionBackground());
		} else {
			setForeground(list.getForeground());
			setBackground(list.getBackground());
		}
		if( zCellHasFocus ){
			setBorder(mBorder_FocusCell);
		} else {
			setBorder(mBorder_RegularCell);
		}
		if( index == 0 ) mimageSwatch = mColorSpecification.getMissingSwatch();
		if( index > 0 ) mimageSwatch = mColorSpecification.getSwatch(index);
		if( value == null ){
			sTextValue = "[error]";
		} else {
			sTextValue = value.toString();
		}
		return this;
	}

	public void paint(Graphics g) {
		super.paint(g);
		int iCellWidth = getWidth();
		int iCellHeight = getHeight();
		g.setColor(getBackground());
		g.fillRect(0, 0, iCellWidth, iCellHeight);
		getBorder().paintBorder(this, g, 0, 0, iCellWidth, iCellHeight);
		if( mimageSwatch != null ) g.drawImage(mimageSwatch, mInsets.left+2, mInsets.top+2, getBackground(), null);
		g.setColor(getForeground());
		g.setFont(Styles.fontFixed12);
		FontMetrics fm = g.getFontMetrics();
		mTextY = mInsets.top + fm.getAscent();
		if( sTextValue == null ) sTextValue = "[error]";
		g.drawString(sTextValue, mTextX, mTextY);
	}

	public void intervalAdded(ListDataEvent e){ }
	public void intervalRemoved(ListDataEvent e){ }
	public void contentsChanged(ListDataEvent e){  }

}

class Panel_RangeEditor extends JPanel implements IColorChanged {
	private IRangeChanged mOwner;
	private int mxRange1;
	final private JRadioButton jrbSingleValue_Yes = new JRadioButton("Yes");
	final private JRadioButton jrbSingleValue_No = new JRadioButton("No");
	final private JRadioButton jrbSingleColor_Yes = new JRadioButton("Yes");
	final private JRadioButton jrbSingleColor_No = new JRadioButton("No");
	final private static String[] asColorSteps = {"synch up", "synch down", "cont up", "cont down"};
	final private JComboBox jcbColorStep = new JComboBox(asColorSteps);
	private JLabel mlabelRangeSwatch;
	private JLabel mlabelSwatch;
	private JTextField jtfDataFrom;
	private JTextField jtfDataTo;
	private JLabel labelColorFrom;
	private JLabel labelColorTo;
	private JLabel labelColorStep;
	private JLabel labelDataFrom;
	private JLabel labelDataTo;
	private JLabel labelHueRange;
	private JLabel labelSatRange;
	private JLabel labelBriRange;
	private JLabel labelAlphaRange;
	private Panel_ColorPicker pickerFrom; final static String ID_PickerFrom = "FROM";
	private Panel_ColorPicker pickerTo; final static String ID_PickerTo = "TO";
	private Panel_ColorPicker pickerMissing; final static String ID_PickerMissing = "MISSING";
	final static String[] asFactor = {
		"varies", "00", "10", "20", "30", "40", "50", "60", "70", "80", "90",
		"A0", "B0", "C0", "D0", "E0", "F0", "FF" };
	private JComboBox jcbHue = new JComboBox(asFactor);
	private JComboBox jcbSat = new JComboBox(asFactor);
	private JComboBox jcbBri = new JComboBox(asFactor);
	private JComboBox jcbAlpha = new JComboBox(asFactor);
	private boolean mzSystemUpdating = false;
	private JTextField jtfMissing;
	private JPanel panelMissing;
	private JPanel panelRange;

	Panel_RangeEditor(IRangeChanged owner){
		mOwner = owner;

		// missing
		JLabel labelMissingValues = new JLabel("Missing Values:", JLabel.RIGHT);
		jtfMissing = new JTextField();
		JLabel labelMissingColor = new JLabel("Missing Color:", JLabel.RIGHT);
		pickerMissing = new Panel_ColorPicker(this, this.ID_PickerMissing, 0xFF0000FF, true);
		panelMissing = new JPanel();
		panelMissing.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.BOTH;

//		// top margin
//		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
//		panelMissing.add(Box.createVerticalStrut(15), gbc);

		// Missing - Values
		gbc.gridy = 1;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelMissing.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelMissing.add(labelMissingValues, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelMissing.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelMissing.add(jtfMissing, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelMissing.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelMissing.add(Box.createVerticalStrut(6), gbc);

		// Missing - Color
		gbc.gridy = 3;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelMissing.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelMissing.add(labelMissingColor, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelMissing.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelMissing.add(pickerMissing, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelMissing.add(Box.createHorizontalGlue(), gbc);

		// single value/color
		final JLabel labelSingleValue = new JLabel("Single value:", JLabel.RIGHT);
		final JLabel labelSingleColor = new JLabel("Single color:", JLabel.RIGHT);
		final JPanel panelSingleValue = new JPanel();
		final JPanel panelSingleColor = new JPanel();
		final ButtonGroup bgSingleValue = new ButtonGroup();
		bgSingleValue.add(jrbSingleValue_Yes);
		bgSingleValue.add(jrbSingleValue_No);
		final ButtonGroup bgSingleColor = new ButtonGroup();
		bgSingleColor.add(jrbSingleColor_Yes);
		bgSingleColor.add(jrbSingleColor_No);
		panelSingleValue.setLayout(new BoxLayout(panelSingleValue, BoxLayout.X_AXIS));
		panelSingleValue.add(jrbSingleValue_Yes);
		panelSingleValue.add(jrbSingleValue_No);
		panelSingleColor.setLayout(new BoxLayout(panelSingleColor, BoxLayout.X_AXIS));
		panelSingleColor.add(jrbSingleColor_Yes);
		panelSingleColor.add(jrbSingleColor_No);

		// standard range
		mlabelRangeSwatch = new JLabel("", JLabel.RIGHT);
		mlabelRangeSwatch.setFont(Styles.fontSansSerifBold12);
		mlabelSwatch = new JLabel();
		labelColorFrom = new JLabel("Color From:", JLabel.RIGHT);
		labelColorTo = new JLabel("Color To:", JLabel.RIGHT);
		labelColorStep = new JLabel("Color Step:", JLabel.RIGHT);
		labelDataFrom = new JLabel("Data From:", JLabel.RIGHT);
		labelDataTo = new JLabel("Data To:", JLabel.RIGHT);
		JLabel labelAlpha = new JLabel("Alpha:", JLabel.RIGHT);

		JLabel labelHue = new JLabel("Hue:", JLabel.RIGHT);
		JLabel labelSat = new JLabel("Saturation:", JLabel.RIGHT);
		JLabel labelBri = new JLabel("Brightness:", JLabel.RIGHT);
		labelHueRange = new JLabel("");
		labelSatRange = new JLabel("");
		labelBriRange = new JLabel("");
		labelAlphaRange = new JLabel("");
		jtfDataFrom = new JTextField();
		jtfDataTo = new JTextField();
		pickerFrom = new Panel_ColorPicker(this, ID_PickerFrom, 0x00000000, true);
		pickerTo = new Panel_ColorPicker(this, ID_PickerTo, 0x00000000, true);
		vSetupListeners();

		panelRange = new JPanel();
		panelRange.setLayout(new GridBagLayout());
		gbc.fill = gbc.BOTH;

		// top margin
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(5), gbc);

		// Range Swatch
//		gbc.gridy = 1;
//		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
//		panelRange.add(Box.createHorizontalGlue(), gbc);
//		gbc.gridx = 1; gbc.weightx = 0.0;
//		panelRange.add(Box.createHorizontalStrut(5), gbc); // get rid of range swatch
//		gbc.gridx = 2; gbc.weightx = 0.0;
//		panelRange.add(Box.createHorizontalStrut(5), gbc);
//		gbc.gridx = 3; gbc.weightx = 0.0;
//		panelRange.add(Box.createHorizontalStrut(5), gbc); // nothing here anymore
//		gbc.gridx = 4; gbc.weightx = 1;
//		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Single Value
		gbc.gridy = 3;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelSingleValue, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(panelSingleValue, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(4), gbc);

		// Single Color
		gbc.gridy = 5;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelSingleColor, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(panelSingleColor, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Data From
		gbc.gridy = 7;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelDataFrom, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jtfDataFrom, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Data To
		gbc.gridy = 9;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelDataTo, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jtfDataTo, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 10; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Color From
		gbc.gridy = 11;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelColorFrom, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(pickerFrom, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 12; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 5; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Color To
		gbc.gridy = 13;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createVerticalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelColorTo, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createVerticalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(pickerTo, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createVerticalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 14; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Color step
		gbc.gridy = 15;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelColorStep, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jcbColorStep, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 16; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Hue
		gbc.gridy = 17;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelHue, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jcbHue, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(labelHueRange, gbc);
		gbc.gridx = 5; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 18; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);


		// Sat
		gbc.gridy = 19;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelSat, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jcbSat, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(labelSatRange, gbc);
		gbc.gridx = 5; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 20; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Bri
		gbc.gridy = 21;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelBri, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jcbBri, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(labelHueRange, gbc);
		gbc.gridx = 5; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx = 0; gbc.gridy = 22; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(6), gbc);

		// Alpha
		gbc.gridy = 23;
		gbc.gridx = 0; gbc.weightx = 1; gbc.weighty = 0.0; gbc.gridwidth = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);
		gbc.gridx = 1; gbc.weightx = 0.0;
		panelRange.add(labelAlpha, gbc);
		gbc.gridx = 2; gbc.weightx = 0.0;
		panelRange.add(Box.createHorizontalStrut(5), gbc);
		gbc.gridx = 3; gbc.weightx = 0.0;
		panelRange.add(jcbAlpha, gbc);
		gbc.gridx = 4; gbc.weightx = 1;
		panelRange.add(labelHueRange, gbc);
		gbc.gridx = 5; gbc.weightx = 1;
		panelRange.add(Box.createHorizontalGlue(), gbc);

		// bottom margin
		gbc.gridy = 24;
		gbc.gridx = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 6; gbc.gridheight = 1;
		panelRange.add(Box.createVerticalStrut(15), gbc);

		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Range Editor"));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(panelMissing);
		this.add(panelRange);

	}

	public void setEnabled( boolean z ){
		jcbHue.setEnabled(z);
		jcbSat.setEnabled(z);
		jcbBri.setEnabled(z);
		jcbAlpha.setEnabled(z);
		jtfMissing.setEnabled(z);
		jrbSingleValue_Yes.setEnabled(z);
		jrbSingleValue_No.setEnabled(z);
		jrbSingleColor_Yes.setEnabled(z);
		jrbSingleColor_No.setEnabled(z);
		jcbColorStep.setEnabled(z);
		jtfDataFrom.setEnabled(z);
		jtfDataTo.setEnabled(z);
		pickerFrom.setEnabled(z);
		pickerTo.setEnabled(z);
	}

	private void vSetupListeners(){
		ActionListener listenSingleValue =
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					vUpdateSingleValue();
				}
			};
		KeyListener listenEnter =
	    	new java.awt.event.KeyListener(){
		    	public void keyPressed(java.awt.event.KeyEvent ke){
			    	if( ke.getKeyCode() == ke.VK_ENTER ){
						mOwner.vRangeChanged(mxRange1);
	    			}
		    	}
			    public void keyReleased(java.awt.event.KeyEvent ke){}
				public void keyTyped(java.awt.event.KeyEvent ke){}
			};
		FocusAdapter focus =
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					mOwner.vRangeChanged(mxRange1);
				}
			};
		jrbSingleValue_Yes.addActionListener(listenSingleValue);
		jrbSingleValue_No.addActionListener(listenSingleValue);
		ActionListener listenSingleColor =
			new ActionListener(){
				public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
					vUpdateSingleColor();
				}
			};
		jrbSingleColor_Yes.addActionListener(listenSingleColor);
		jrbSingleColor_No.addActionListener(listenSingleColor);
		jtfDataFrom.addFocusListener( focus );
	    jtfDataTo.addFocusListener( focus );
		jtfMissing.addFocusListener( focus );
		jtfDataFrom.addKeyListener(listenEnter);
		jtfDataTo.addKeyListener(listenEnter);
		jtfMissing.addKeyListener(listenEnter);
		ActionListener listenJCB =
			new java.awt.event.ActionListener(){
			    public void actionPerformed( java.awt.event.ActionEvent ae ){
					if( mzSystemUpdating ) return;
					mOwner.vRangeChanged(mxRange1);
				}
			};
		jcbColorStep.addActionListener(listenJCB);
		jcbHue.addActionListener(listenJCB);
		jcbSat.addActionListener(listenJCB);
		jcbBri.addActionListener(listenJCB);
		jcbAlpha.addActionListener(listenJCB);
	}
	void vUpdateSingleValue(){
		if( jrbSingleValue_Yes.isSelected() ){
			labelDataFrom.setText("Data Value:");
			labelDataTo.setVisible(false);
			jtfDataTo.setVisible(false);
			jtfDataTo.setText(jtfDataFrom.getText());
			jrbSingleColor_Yes.setSelected(true);
			jrbSingleColor_Yes.setEnabled(false);
			jrbSingleColor_No.setEnabled(false);
		} else {
			labelDataFrom.setText("Data From:");
			labelDataTo.setVisible(true);
			jtfDataTo.setVisible(true);
			jrbSingleColor_Yes.setEnabled(true);
			jrbSingleColor_No.setEnabled(true);
		}
		vUpdateSingleColor();
	}
	void vUpdateSingleColor(){
		if( mzSystemUpdating ) return;
		if( jrbSingleColor_Yes.isSelected() ){
			labelColorFrom.setText("Color:");
			labelColorTo.setVisible(false);
			pickerTo.setVisible(false);
			pickerTo.setColor(pickerFrom.getHSB());
		} else {
			labelColorFrom.setText("Color From:");
			labelColorTo.setVisible(false);
			pickerTo.setVisible(true);
		}
		mOwner.vRangeChanged( mxRange1 );
	}
	void set(ColorSpecification cs, int xRange1){
		try {
			mzSystemUpdating = true;
			boolean zEnable = (cs != null);
			jrbSingleColor_Yes.setEnabled( zEnable );
			jrbSingleColor_No.setEnabled( zEnable );
			jrbSingleValue_Yes.setEnabled( zEnable );
			jrbSingleValue_No.setEnabled( zEnable );
			jtfDataFrom.setEnabled( zEnable );
			jtfDataTo.setEnabled( zEnable );
			pickerFrom.setEnabled( zEnable );
			pickerTo.setEnabled( zEnable );
			jcbColorStep.setEnabled( zEnable );
			jcbHue.setEnabled( zEnable );
			jcbSat.setEnabled( zEnable );
			jcbBri.setEnabled( zEnable );
			jcbAlpha.setEnabled( zEnable );
			if( cs == null ){
				mlabelRangeSwatch.setText("[no color specification active]");
				jtfDataFrom.setText( "" );
				jtfDataTo.setText( "" );
				pickerFrom.setColor( 0xFF00007F ); // 50% gray
				pickerTo.setColor( 0xFF00007F ); // 50% gray
				jcbColorStep.setSelectedIndex(-1);
				int iHue = -1;
				int iSaturation = -1;
				int iBrightness = -1;
				int iAlpha = -1;
				vUpdateCombo( jcbHue, iHue );
				vUpdateCombo( jcbSat, iSaturation );
				vUpdateCombo( jcbBri, iBrightness );
				vUpdateCombo( jcbAlpha, iAlpha );
				vUpdateColorComponentRanges(0, 0, 0, 0, 0, 0);
			} else {
				mxRange1 = xRange1;
				if( mxRange1 == 0 ){
					panelMissing.setVisible(true);
					panelRange.setVisible(false);
				} else {
					panelMissing.setVisible(false);
					panelRange.setVisible(true);
				}
				jtfMissing.setText(cs.getMissingString());
				pickerMissing.setColor(cs.getColorMissingHSB());
				mlabelRangeSwatch.setText("Range " + mxRange1 + ":");
				jtfDataFrom.setText( cs.getDataFromS(xRange1) );
				jtfDataTo.setText( cs.getDataToS(xRange1) );
				pickerFrom.setColor(cs.getColorFromHSB(xRange1));
				pickerTo.setColor(cs.getColorToHSB(xRange1));
				int iColorStep = cs.getColorStep(xRange1);
				jcbColorStep.setSelectedIndex(cs.getColorStep(xRange1));
				int hsbColorFrom = cs.getColorFromHSB(xRange1);
				int hsbColorTo = cs.getColorFromHSB(xRange1);
				int iHue = cs.getHue(xRange1);
				int iSaturation = cs.getSaturation(xRange1);
				int iBrightness = cs.getBrightness(xRange1);
				int iAlpha = cs.getAlpha(xRange1);
				vUpdateCombo( jcbHue, iHue );
				vUpdateCombo( jcbSat, iSaturation );
				vUpdateCombo( jcbBri, iBrightness );
				vUpdateCombo( jcbAlpha, iAlpha );
				jrbSingleValue_Yes.setSelected( cs.isSingleValue(xRange1) );
				jrbSingleColor_Yes.setSelected( cs.isSingleColor(xRange1) );
				jrbSingleValue_No.setSelected( !cs.isSingleValue(xRange1) );
				jrbSingleColor_No.setSelected( !cs.isSingleColor(xRange1) );
				vUpdateSingleValue();
				vUpdateColorComponentRanges(iHue, iSaturation, iBrightness, iAlpha, hsbColorFrom, hsbColorTo);
			}
		} finally {
			mzSystemUpdating = false;
		}
	}
	private final void vUpdateColorComponentRanges(int iHue, int iSat, int iBri, int iAlpha, int hsbColorFrom, int hsbColorTo ){
		if( iHue == -1 ){
			int iHueFrom   = (int)(hsbColorFrom & 0x00FF0000) >> 16;
			int iHueTo     = (int)(hsbColorTo & 0x00FF0000) >> 16;
			StringBuffer sbRange = new StringBuffer();
			sbRange.append(Utility.sToHex(iHueFrom, 2)).append("-").append(Utility.sToHex(iHueTo, 2));
		} else {
			labelHueRange.setText("");
		}
		if( iSat == -1 ){
			int iSatFrom   = ((int)hsbColorFrom & 0x0000FF00) >> 8;
			int iSatTo     = ((int)hsbColorTo & 0x0000FF00) >> 8;
			StringBuffer sbRange = new StringBuffer();
			sbRange.append(Utility.sToHex(iSatFrom, 2)).append("-").append(Utility.sToHex(iSatTo, 2));
		} else {
			labelHueRange.setText("");
		}
		if( iBri == -1 ){
			int iBriFrom   = ((int)hsbColorFrom & 0x000000FF);
			int iBriTo     = ((int)hsbColorTo & 0x000000FF);
			StringBuffer sbRange = new StringBuffer();
			sbRange.append(Utility.sToHex(iBriFrom, 2)).append("-").append(Utility.sToHex(iBriTo, 2));
		} else {
			labelHueRange.setText("");
		}
		if( iAlpha == -1 ){
			int iAlphaFrom = (int)((hsbColorFrom & 0xFF000000L) >> 24);
			int iAlphaTo   = (int)((hsbColorTo & 0xFF000000L) >> 24);
			StringBuffer sbRange = new StringBuffer();
			sbRange.append(Utility.sToHex(iAlphaFrom, 2)).append("-").append(Utility.sToHex(iAlphaTo, 2));
		} else {
			labelHueRange.setText("");
		}
	}
	private final void vUpdateCombo( JComboBox jcb, int iValue ){
		if( iValue == -1 ){ // varies
			jcb.setSelectedIndex(0);
			return;
		}
		if( iValue == 0xFF ){
			jcb.setSelectedIndex(17);
			return;
		}
		if( iValue < 0 || iValue > 0xFF ) return; // internal error has occurred
		jcb.setSelectedIndex(iValue/0x10 + 1);
	}
	private int getComboValue( JComboBox jcb ){
		int x = jcb.getSelectedIndex();
		if( x == -1 || x == 0 ) return -1;
		if( x >= 17 ) return 0xFF;
		return (x - 1)* 0x10;
	}
	public void vColorChanged( String sID, int iHSB, int iRGB, int iHue, int iSat, int iBri ){
		if( mzSystemUpdating ) return;
		mOwner.vRangeChanged( mxRange1 );
	}
	public String getMissingValuesText(){ return jtfMissing.getText(); }
	public void setMissingValuesText(String sText){ jtfMissing.setText(sText); }
	public String getDataFromString(){ return jtfDataFrom.getText(); }
	public String getDataToString(){
		if( jrbSingleValue_Yes.isSelected() ){
			return jtfDataFrom.getText();
		} else {
			return jtfDataTo.getText();
		}
	}
	public int getColorMissing(){ return pickerMissing.getHSB(); }
	public int getColorFromHSB(){ return pickerFrom.getHSB(); }
	public int getColorToHSB(){
		if( jrbSingleColor_Yes.isSelected() ){
			return pickerFrom.getHSB();
		} else {
			return pickerTo.getHSB();
		}
	}
	public int getColorStep(){
		int iSelectedIndex = jcbColorStep.getSelectedIndex();
		return iSelectedIndex; // currently there is a 1:1 relationship between the enum values of the colorstep constants (see ColorSpecification) and the combo box index
	}
	public int getAlpha(){ return getComboValue( jcbAlpha ); }
	public int getHue(){ return getComboValue( jcbHue ); }
	public int getSat(){ return getComboValue( jcbSat ); }
	public int getBri(){ return getComboValue( jcbBri ); }
}

class Panel_ColorPicker extends JPanel {
	IColorChanged mOwner;
	String msID;
	JButton buttonColor;
	JLabel labelColor;
	int miHSB;
	JDialog jd;
	ColorPicker_HSB mHSBpicker;
	JOptionPane jop;
	Panel_ColorPicker(IColorChanged owner, String sID, int iHSB, boolean zShowValue){
		mOwner = owner;
		msID = sID;
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		buttonColor = new JButton();
		labelColor = new JLabel();
		this.add(buttonColor);
		if( zShowValue ){
			this.add(Box.createHorizontalStrut(6));
			this.add(labelColor);
		}
		mHSBpicker = new ColorPicker_HSB();
		jop = new JOptionPane(mHSBpicker, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		jd = jop.createDialog(ApplicationController.getInstance().getAppFrame(), "Choose Color");
		buttonColor.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					mHSBpicker.setColor(miHSB);
					jd.show();
					Object oValue = jop.getValue();
					if( oValue == null || oValue.toString().equals("2")){ // todo figure this out
						return; // cancel
					} else {
						int iHSB = mHSBpicker.getHSB();
						setColor( iHSB );
						mOwner.vColorChanged(msID, iHSB, 0, 0, 0, 0);
					}
				}
			}
		);
		setColor( iHSB );
	}
	public void setEnabled( boolean z ){
		buttonColor.setEnabled(z);
	}
	private StringBuffer sbColorText = new StringBuffer();
	void setColor( int iHSB ){
		miHSB = iHSB;
		buttonColor.setIcon(getColorIcon(miHSB));
		sbColorText.setLength(0);
		sbColorText.append(Utility.sToHex(getAlpha(), 2));
		sbColorText.append(' ');
		sbColorText.append(Utility.sToHex(getHue(), 2));
		sbColorText.append(' ');
		sbColorText.append(Utility.sToHex(getSat(), 2));
		sbColorText.append(' ');
		sbColorText.append(Utility.sToHex(getBri(), 2));
		sbColorText.append(' ');
		labelColor.setText( sbColorText.toString() );
	}
	int getHSB(){ return miHSB; }
	int getRGB(){ return ColorSpecification.iHSBtoRGBA(miHSB); }
	int getAlpha(){ return miHSB >> 24; }
	int getHue(){ return (miHSB & 0x00FF0000) >> 16; }
	int getSat(){ return (miHSB & 0x0000FF00) >> 8; }
	int getBri(){ return miHSB & 0x000000FF; }
	ImageIcon getColorIcon( int iHSB ){ // todo render yourself
		BufferedImage bi = new BufferedImage( 20, 15, BufferedImage.TYPE_INT_ARGB ); // smaller buffer just used to get sizes
		Graphics2D g2 = (Graphics2D)bi.getGraphics();
		g2.setColor(Color.BLACK);
		g2.drawString("alpha", 2, 2);
		Color color = new Color(ColorSpecification.iHSBtoRGBA(iHSB));
		g2.setColor(color);
		g2.fillRect(0, 0, 20, 15);
		return new javax.swing.ImageIcon(bi);
	}
}

interface IRangeChanged {
	void vRangeChanged( int iRangeIndex1 );
}

interface IColorChanged {
	void vColorChanged( String sID, int iHSB, int iRGB, int iHue, int iSat, int iBri );
}



