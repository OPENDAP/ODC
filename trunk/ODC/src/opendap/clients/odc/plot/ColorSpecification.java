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
import opendap.clients.odc.Utility_String;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
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

public class ColorSpecification extends AbstractListModel {

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
			sbRangeString.append(Utility_String.sToHex(ahsbColorFrom[x1], 8));
			sbRangeString.append(' ');
			sbRangeString.append(Utility_String.sToHex(ahsbColorTo[x1], 8));
			sbRangeString.append(' ');
			sbRangeString.append(COLOR_STEP_codes[aeColorStep[x1]]);
			sbRangeString.append(' ');
			if( aiHue[x1] == -1 )
			    sbRangeString.append("~");
		    else
				sbRangeString.append(Utility_String.sToHex(aiHue[x1], 2));
			sbRangeString.append(' ');
			if( aiSaturation[x1] == -1 )
			    sbRangeString.append("~");
		    else
		    	sbRangeString.append(Utility_String.sToHex(aiSaturation[x1], 2));
			sbRangeString.append(' ');
			if( aiBrightness[x1] == -1 )
			    sbRangeString.append("~");
		    else
	    		sbRangeString.append(Utility_String.sToHex(aiBrightness[x1], 2));
			sbRangeString.append(' ');
			if( aiAlpha[x1] == -1 )
			    sbRangeString.append("~");
		    else
				sbRangeString.append(Utility_String.sToHex(aiAlpha[x1], 2));
		}
		return sbRangeString.toString();
	}

	JFileChooser jfc;
	boolean zStorage_save( StringBuffer sbError ){
		try {

			// ask user for desired location
			String sPlotsDirectory = ConfigurationManager.getInstance().getProperty_DIR_Plots();
			File filePlotsDirectory = Utility.fileEstablishDirectory( sPlotsDirectory, sbError );
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
				ConfigurationManager.getInstance().setOption(ConfigurationManager.PROPERTY_DIR_Plots, sNewPlotsDirectory );
			}

			// open file
			FileOutputStream fos;
		    try {
			    fos = new java.io.FileOutputStream(file);
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
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	boolean zStorage_load( StringBuffer sbError ){
		try {

			// ask user for desired location
			String sPlotsDirectory = ConfigurationManager.getInstance().getProperty_DIR_Plots();
			File filePlotsDirectory = Utility.fileEstablishDirectory(sPlotsDirectory, sbError );
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
				ConfigurationManager.getInstance().setOption(ConfigurationManager.PROPERTY_DIR_Plots, sNewPlotsDirectory );
			}

			// load the lines of the file
			String sPath = file.getAbsolutePath();
			ArrayList<String> listLines = Utility.zLoadLines( sPath, 1000, sbError );
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
							String sName = Utility_String.getEnclosedSubstring(sLine, ":", null).trim();
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
							String sType = Utility_String.getEnclosedSubstring( sLine, ":", null ).trim();
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
							String sRangeCount = Utility_String.getEnclosedSubstring( sLine, "(", ")" );
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
						String[] asLine = Utility_String.splitCommaWhiteSpace( sLine );
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
				sbError.append("Error loading, " + sProblem + " field was missing or invalid");
				return false;
			}

			return true; // done, yeah

		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	void setMissingColor( int hsbColor ){
		miMissingColor = hsbColor;
		mrgbMissingColor = Color_HSB.iHSBtoRGBA(hsbColor);
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
		String[] as = Utility_String.split(sMissingValues, ' ');
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
			ApplicationController.vShowWarning("internal error, missing type in mismatch color specification");
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
		int iOrder;
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
				iOrder = iDetermineDataRangeOrder();
				return "" + Utility.round( (double)afDataFrom[index1], iOrder - 2 );
			case DATA_TYPE_Float64:
				iOrder = iDetermineDataRangeOrder();
				return "" + Utility.round( adDataFrom[index1], iOrder - 2 );
			default: return "?";
		}
	}
	String getDataToS(int index1){
		int iOrder;
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
				iOrder = iDetermineDataRangeOrder();
			    return "" + Utility.round( (double)afDataTo[index1], iOrder - 2 );
			case DATA_TYPE_Float64:
				iOrder = iDetermineDataRangeOrder();
				return "" + Utility.round( adDataTo[index1], iOrder - 2 );
			default: return "?";
		}
	}

	/* the data range order is the order of magnitude of the range of the data
	 * where order 2 is 100, order 3 is 1000, order -2 is .01 etc
	 */
	int iDetermineDataRangeOrder(){
		long nMinimum = Long.MAX_VALUE;
		long nMaximum = Long.MIN_VALUE;
		double dMinimum = Double.MAX_VALUE;
		double dMaximum = Double.MIN_VALUE;
		for( int xRange = 1; xRange <= this.mctRanges; xRange++ ){
			switch (miDataType) {
				case DATA_TYPE_Byte:
				case DATA_TYPE_Int16:
					if( ashDataTo[xRange] < nMinimum ) nMinimum = ashDataTo[xRange];
					if( ashDataTo[xRange] > nMaximum ) nMaximum = ashDataTo[xRange];
					if( ashDataFrom[xRange] < nMinimum ) nMinimum = ashDataFrom[xRange];
					if( ashDataFrom[xRange] > nMaximum ) nMaximum = ashDataFrom[xRange];
					break;
				case DATA_TYPE_UInt16:
				case DATA_TYPE_Int32:
					if( aiDataTo[xRange] < nMinimum ) nMinimum = aiDataTo[xRange];
					if( aiDataTo[xRange] > nMaximum ) nMaximum = aiDataTo[xRange];
					if( aiDataFrom[xRange] < nMinimum ) nMinimum = aiDataFrom[xRange];
					if( aiDataFrom[xRange] > nMaximum ) nMaximum = aiDataFrom[xRange];
					break;
				case DATA_TYPE_UInt32:
					if( anDataTo[xRange] < nMinimum ) nMinimum = anDataTo[xRange];
					if( anDataTo[xRange] > nMaximum ) nMaximum = anDataTo[xRange];
					if( anDataFrom[xRange] < nMinimum ) nMinimum = anDataFrom[xRange];
					if( anDataFrom[xRange] > nMaximum ) nMaximum = anDataFrom[xRange];
					break;
				case DATA_TYPE_Float32:
					if( afDataTo[xRange] < dMinimum ) dMinimum = afDataTo[xRange];
					if( afDataTo[xRange] > dMaximum ) dMaximum = afDataTo[xRange];
					if( afDataFrom[xRange] < dMinimum ) dMinimum = afDataFrom[xRange];
					if( afDataFrom[xRange] > dMaximum ) dMaximum = afDataFrom[xRange];
					break;
				case DATA_TYPE_Float64:
					if( adDataTo[xRange] < dMinimum ) dMinimum = adDataTo[xRange];
					if( adDataTo[xRange] > dMaximum ) dMaximum = adDataTo[xRange];
					if( adDataFrom[xRange] < dMinimum ) dMinimum = adDataFrom[xRange];
					if( adDataFrom[xRange] > dMaximum ) dMaximum = adDataFrom[xRange];
					break;
				default:
					return 0;
			}
		}
		switch (miDataType) {
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
			case DATA_TYPE_UInt32:
				long nRangeMagnitude = nMaximum - nMinimum;
				String sRange = Long.toString( nRangeMagnitude );
				if( nRangeMagnitude < 0 ){
					return sRange.length() - 1;
				} else {
					return sRange.length();
				}
			case DATA_TYPE_Float32:
			case DATA_TYPE_Float64:
				double dRangeMagnitude = dMaximum - dMinimum;
				long nClosestLong = Math.round( Math.log10(dRangeMagnitude) );
				return (int)nClosestLong;
			default:
				return 0;
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
		return Utility_String.sFixedWidth(Integer.toHexString(ahsbColorFrom[index1]), 8, '0', Utility_String.ALIGNMENT_RIGHT);
	}
	String getColorToS( int index1 ){
		if( ahsbColorTo[index1] == -1 ) return "[varies]";
		return Utility_String.sFixedWidth(Integer.toHexString(ahsbColorTo[index1]), 8, '0', Utility_String.ALIGNMENT_RIGHT);
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
			sbError.append("invalid hue (" + Utility_String.sToHex(iHue, 8) + "), must be between -1 and 0xFF");
			return false;
		}
		if( iSaturation >= -1 && iSaturation <= 0xFF ){
			aiSaturation[x1] = iSaturation;
		} else {
			sbError.append("invalid saturation (" + Utility_String.sToHex(iSaturation, 8) + "), must be between -1 and 0xFF");
			return false;
		}
		if( iBrightness >= -1 && iBrightness <= 0xFF ){
			aiBrightness[x1] = iBrightness;
		} else {
			sbError.append("invalid brightness (" + Utility_String.sToHex(iBrightness, 8) + "), must be between -1 and 0xFF");
			return false;
		}
		if( iAlpha >= -1 && iAlpha <= 0xFF ){
			aiAlpha[x1] = iAlpha;
		} else {
			sbError.append("invalid alpha (" + Utility_String.sToHex(iAlpha, 8) + "), must be between -1 and 0xFF");
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
			aColors[xBand] = new Color( Color_HSB.iHSBtoRGB(ahsbBands[xBand]), true );
		}
		return aColors;
	}

	public static int[] getColorBands1_HSB( int ctBands ){
		int[] ahsbBands = new int [ctBands + 1];
		int iHue = 0; int fHueInterval = (int)((float)0xFF / (float)ctBands);
		int iSat = 0x7F; int fSatInterval = (int)((float)0xFF / (float)ctBands);
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
	void vGenerateCG_MultiHue( int ctHues ){
		if( ctHues == 2 ){
			vGenerateCG_Band( 0.0f, 0.5f, 0xFFCFFFA0, 0xFFA0FFFF, COLOR_STEP_SynchronizedUp, -1, 0xFF, -1 );
			vGenerateCG_Band( 0.5f, 1.0f, 0xFFD0FFFF, 0xFFFFFFA0, COLOR_STEP_SynchronizedUp, -1, 0xFF, -1 );
		} else if( ctHues == 3 ) {
			vGenerateCG_Band( 0.0f, 0.5f, 0xFF9FFFA0, 0xFF58FFFF, COLOR_STEP_SynchronizedDown, -1, 0xFF, -1 );
			vGenerateCG_Band( 0.5f, 1.0f, 0xFF57FFFF, 0xFF00FFA0, COLOR_STEP_SynchronizedDown, -1, 0xFF, -1 );
		}
	}
	void vGenerateCG_Band( float fProFrom, float fProTo, int iColorFrom, int iColorTo, int iColorStep, int iHue, int iSat, int iBri ){
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

	int getColorForValue( float fDataValue ){
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_Float32 ){
			ApplicationController.vShowError("cannot get float-based color for type " + DAP.getType_String(getDataType()));
			return 0;
		}
		int xRange = 0;
		while( true ){
			xRange++;
			if( xRange > mctRanges ){
				return 0xFFFFFFFF; // no color is defined for this value
			}
			for(int xMissing = 1; xMissing <= mctMissing; xMissing++){
				if( fDataValue == afMissing1[xMissing] ){
					return mrgbMissingColor;
				}
			}
			if( fDataValue >= afDataFrom[xRange] && fDataValue <= afDataTo[xRange] ){
				float fRangeWidth = afDataTo[xRange] - afDataFrom[xRange];
				float fDataProportion;
				if( fRangeWidth == 0 )
					fDataProportion = -1;
				else
					fDataProportion = (fDataValue - afDataFrom[xRange])/ fRangeWidth;
				return rgbGetForRange( fDataProportion, xRange );
			}
		}
	}

	int getColorForValue( double dDataValue ){
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_Float64 ){
			ApplicationController.vShowError("cannot get double-based color for type " + DAP.getType_String(getDataType()));
			return 0;
		}
		int xRange = 0;
		while( true ){
			xRange++;
			if( xRange > mctRanges ){
				return 0xFFFFFFFF; // no color is defined for this value
			}
			for(int xMissing = 1; xMissing <= mctMissing; xMissing++){
				if( dDataValue == adMissing1[xMissing] ){
					return mrgbMissingColor;
				}
			}
			if( dDataValue >= adDataFrom[xRange] && dDataValue <= adDataTo[xRange] ){
				double dRangeWidth = adDataTo[xRange] - adDataFrom[xRange];
				float fDataProportion;
				if( dRangeWidth == 0 )
					fDataProportion = -1;
				else
					fDataProportion = (float)((dDataValue - adDataFrom[xRange])/ (float)dRangeWidth);
				return rgbGetForRange( fDataProportion, xRange );
			}
		}
	}
		
// todo we are redundantly calculating the data proportion for each range
// on every data point; instead we should store the data proportion for each
// range when the range is set
	void render( int[] aiRGB, float[] afData, int iDataWidth, int iDataHeight, int pxWidth, int pxHeight, boolean zAverage ){
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_Float32 ){
			ApplicationController.vShowError("cannot render float data for type " + DAP.getType_String(getDataType()));
			return;
		}
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
		return;
	}

	// averaging not implemented currently
	// Cartesian Presentation: the data origin is in document orientation. In other words the
	// the first element is at the upper left. When plotted a cartesian coordinate system is
	// used with the first element at the lower left.
	void render( int[] aiRGB, double[] adData, int iDataWidth, int iDataHeight, int pxWidth, int pxHeight, boolean zAverage ){
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_Float64 ){
			ApplicationController.vShowError("cannot render double data for type " + DAP.getType_String(getDataType()));
			return;
		}
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
		return;
	}

	void render( int[] aiRGB, short[] ashData, int iDataWidth, int iDataHeight, int pxWidth, int pxHeight, boolean zAverage ){
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_Byte && iDataType != DATA_TYPE_Int16 ){
			ApplicationController.vShowError("cannot render short data for type " + DAP.getType_String(getDataType()));
			return;
		}
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
		return;
	}
	void render( int[] aiRGB, int[] aiData, int iDataWidth, int iDataHeight, int pxWidth, int pxHeight, boolean zAverage ){
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_Int32 && iDataType != DATA_TYPE_UInt16 ){
			ApplicationController.vShowError( "cannot render int data for type " + DAP.getType_String(iDataType) );
			return;
		}
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
					for( int xMissing = 1; xMissing <= mctMissing; xMissing++ ){
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
		return;
	}
	void render( int[] aiRGB, long[] anData, int iDataWidth, int iDataHeight, int pxWidth, int pxHeight, boolean zAverage ){
		int iDataType = getDataType();
		if( iDataType != DATA_TYPE_UInt32 ){
			ApplicationController.vShowError( "cannot render long data for type " + DAP.getType_String(iDataType) );
			return;
		}
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
		return;
	}

	// this is used to render the expression fill plots (pseudocolors)
	// the rendering buffer must already be allocated and ready to be written
	void render1to1( int[] aiRGBA, double[] adData ){
		int xRGB = 0;
		int ctData = adData.length;
		for( int xData = 0; xData < ctData; xData++ ){
			int xRange = 0;
Ranges:
			while(true){
				xRange++;
				if( xRange > mctRanges ){
					aiRGBA[xData] = 0xFFFFFFFF; // no color is defined for this value
					break;
				}
				double dDataValue = adData[xData];
				for(int xMissing = 1; xMissing <= mctMissing; xMissing++){
					if( dDataValue == adMissing1[xMissing] ){
						aiRGBA[xRGB] = mrgbMissingColor;
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
					aiRGBA[xRGB] = rgbGetForRange( fDataProportion, xRange );
					break;
				}
			}
		}
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
		int iHue = aiHue[xRange];
		int iSaturation = aiSaturation[xRange];
		int iBrightness = aiBrightness[xRange];
		int iAlpha = aiAlpha[xRange];
		if( fDataProportion == -1 ){ // unitary color mapping using components
			if( iHue >= 0 && iSaturation >= 0 && iBrightness >= 0 && iAlpha >= 0 ){ // constant color
				int iHSB = (iAlpha << 24) | (iHue << 16) | (iSaturation << 8) | iBrightness;
				return Color_HSB.iHSBtoRGBA( iHSB );
			} else {
				return Color_HSB.iHSBtoRGBA( ahsbColorFrom[xRange] ); // by convention the color-from is used for unitary mappings
			}
		} else {
			long iColorFrom = ((long)ahsbColorFrom[xRange]) & 0xFFFFFFFF;
			long iColorTo   = ((long)ahsbColorTo[xRange]) & 0xFFFFFFFF;
			if( iColorFrom == iColorTo ) return Color_HSB.iHSBtoRGBA(ahsbColorFrom[xRange]); // unitary color mapping
			if( iHue >= 0 && iSaturation >= 0 && iBrightness >= 0 && iAlpha >= 0 ){ // constant color
				return Color_HSB.iHSBtoRGBA( iAlpha, iHue, iSaturation, iBrightness );
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
				return Color_HSB.iHSBtoRGBA( hsbRaw );
			}
		}
	}
}

