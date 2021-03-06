package opendap.clients.odc;

/**
 * Title:        DAP Utility
 * Description:  Contains utility routines for working with the DAP
 * Copyright:    Copyright (c) 2003-9
 * Company:      OPeNDAP
 * @author       John Chamberlain
 * @version      3.06
 */

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

import opendap.dap.*;
import java.io.*;
import java.util.Enumeration;

import org.python.core.PyObject;

public class DAP {

	public final static int JAVA_TYPE_byte = 1;
	public final static int JAVA_TYPE_short = 2;
	public final static int JAVA_TYPE_int = 3;
	public final static int JAVA_TYPE_long = 4;
	public final static int JAVA_TYPE_float = 5;
	public final static int JAVA_TYPE_double = 6;
	public final static int JAVA_TYPE_string = 7;

	public final static int DATA_TYPE_Byte = 1;     // unsigned char
	public final static int DATA_TYPE_Int16 = 2;
	public final static int DATA_TYPE_Int32 = 3;
	public final static int DATA_TYPE_UInt16 = 4;
	public final static int DATA_TYPE_UInt32 = 5;
	public final static int DATA_TYPE_Float32 = 6;
	public final static int DATA_TYPE_Float64 = 7;
	public final static int DATA_TYPE_String = 8;   // null-terminated, US-ASCII

	public final static int INVERSION_XYZ = 1;
	public final static int INVERSION_XZY = 2;
	public final static int INVERSION_YXZ = 3;
	public final static int INVERSION_YZX = 4;
	public final static int INVERSION_ZXY = 5;
	public final static int INVERSION_ZYX = 6;

	public static enum DAP_TYPE {
		Byte,
		Int16,
		Int32,
		UInt16,
		UInt32,
		Float32,
		Float64,
		String
	}
	
	public static enum DAP_VARIABLE {
		Array,
		Grid,
		Sequence,
		Structure,
		Byte,
		Int16,
		Int32,
		UInt16,
		UInt32,
		Float32,
		Float64,
		String
	}
	
	public static int getDataSize( DAP_TYPE eTYPE ){
		switch( eTYPE ){
			case Byte: return 1;
			case Int16: return 2;
			case Int32: return 4;
			case UInt16: return 2;
			case UInt32: return 4;
			case Float32: return 4;
			case Float64: return 8;
			case String: return 16;
			default: return 0;
		}
	}

	public static int getDataSize( int eTYPE ){
		switch( eTYPE ){
			case DATA_TYPE_Byte: return 1;
			case DATA_TYPE_Int16: return 2;
			case DATA_TYPE_Int32: return 4;
			case DATA_TYPE_UInt16: return 2;
			case DATA_TYPE_UInt32: return 4;
			case DATA_TYPE_Float32: return 4;
			case DATA_TYPE_Float64: return 8;
			case DATA_TYPE_String: return 16;
			default: return 0;
		}
	}
	
	public static DAP_VARIABLE getVariableType( BaseType bt, StringBuffer sbError ){
		if( bt instanceof DArray ) return DAP_VARIABLE.Array;
		else if( bt instanceof DGrid ) return DAP_VARIABLE.Grid;
		else if( bt instanceof DSequence ) return DAP_VARIABLE.Sequence;
		else if( bt instanceof DStructure ) return DAP_VARIABLE.Structure;
		else if( bt instanceof DByte ) return DAP_VARIABLE.Byte;
		else if( bt instanceof DInt16 ) return DAP_VARIABLE.Int16;
		else if( bt instanceof DInt32 ) return DAP_VARIABLE.Int32;
		else if( bt instanceof DUInt16 ) return DAP_VARIABLE.UInt16;
		else if( bt instanceof DUInt32 ) return DAP_VARIABLE.UInt32;
		else if( bt instanceof DFloat32 ) return DAP_VARIABLE.Float32;
		else if( bt instanceof DFloat64 ) return DAP_VARIABLE.Float64;
		else if( bt instanceof DString ) return DAP_VARIABLE.String;
		else {
			sbError.append( "base type is unknown or obsolete, " + bt );
			return null;
		}
	}
	
	public final static DDS getDDSforText( String sTextDDS, StringBuffer sbError ){
		try {
			if( sTextDDS == null || sTextDDS.length() == 0 ){
				sbError.append( "DDS Text is blank" );
				return null;
			}
			opendap.dap.DDS dds = new opendap.dap.DDS();
			ByteArrayInputStream isTextDDS = new ByteArrayInputStream( sTextDDS.getBytes() ); 
			dds.parse( isTextDDS );
			return dds;
		} catch( Throwable t ) {
			sbError.insert( 0, "unexpected error " + t );
			return null;
		}
	}
	
	public static int getDArrayType( DArray darray ){
		if( darray == null ) return 0;
		PrimitiveVector pvector = darray.getPrimitiveVector();
		if( pvector instanceof BytePrimitiveVector ) return DATA_TYPE_Byte;
		if( pvector instanceof Int16PrimitiveVector ) return DATA_TYPE_Int16;
		if( pvector instanceof Int32PrimitiveVector ) return DATA_TYPE_Int32;
		if( pvector instanceof Float32PrimitiveVector ) return DATA_TYPE_Float32;
		if( pvector instanceof Float64PrimitiveVector ) return DATA_TYPE_Float64;
		if( pvector instanceof UInt16PrimitiveVector ) return DATA_TYPE_UInt16;
		if( pvector instanceof UInt32PrimitiveVector ) return DATA_TYPE_Int32;
		return 0;
	}
	public static DAP_TYPE getDAPType( DArray darray ){
		if( darray == null ) return null;
		PrimitiveVector pvector = darray.getPrimitiveVector();
		if( pvector instanceof BytePrimitiveVector ) return DAP_TYPE.Byte;
		if( pvector instanceof Int16PrimitiveVector ) return DAP_TYPE.Int16;
		if( pvector instanceof Int32PrimitiveVector ) return DAP_TYPE.Int32;
		if( pvector instanceof Float32PrimitiveVector ) return DAP_TYPE.Float32;
		if( pvector instanceof Float64PrimitiveVector ) return DAP_TYPE.Float64;
		if( pvector instanceof UInt16PrimitiveVector ) return DAP_TYPE.UInt16;
		if( pvector instanceof UInt32PrimitiveVector ) return DAP_TYPE.Int32;
		return null;
	}
	
	public static String getDArrayType_String( DArray darray ){
		return getType_String(getDArrayType(darray));
	}
	public static String getType_String( int eTYPE ){
		switch( eTYPE ){
			case DATA_TYPE_Byte: return "Byte";
			case DATA_TYPE_Int16: return "Int16";
			case DATA_TYPE_Int32: return "Int32";
			case DATA_TYPE_UInt16: return "UInt16";
			case DATA_TYPE_UInt32: return "UInt32";
			case DATA_TYPE_Float32: return "Float32";
			case DATA_TYPE_Float64: return "Float64";
			case DATA_TYPE_String: return "String";
			default: return "?";
		}
	}
	public static String getType_String( DAP_TYPE eTYPE ){
		switch( eTYPE ){
			case Byte: return "Byte";
			case Int16: return "Int16";
			case Int32: return "Int32";
			case UInt16: return "UInt16";
			case UInt32: return "UInt32";
			case Float32: return "Float32";
			case Float64: return "Float64";
			case String: return "String";
			default: return "?";
		}
	}
	
	public static int getType_Plot( BaseType bt ){
		if( bt instanceof DByte ) return DATA_TYPE_Byte;
		else if( bt instanceof DInt16 ) return DATA_TYPE_Int16;
		else if( bt instanceof DInt32 ) return DATA_TYPE_Int32;
		else if( bt instanceof DUInt16 ) return DATA_TYPE_UInt16;
		else if( bt instanceof DUInt32 ) return DATA_TYPE_UInt32;
		else if( bt instanceof DFloat32 ) return DATA_TYPE_Float32;
		else if( bt instanceof DFloat64 ) return DATA_TYPE_Float64;
		else if( bt instanceof DString ) return DATA_TYPE_String;
		else return 0;
	}

	public static int getTypeByName( String sTypeName ){
		String sDataType = sTypeName.toLowerCase();
		if( sDataType.equals("byte") ) return DAP.DATA_TYPE_Byte;
		if( sDataType.equals("int16") ) return DAP.DATA_TYPE_Int16;
		if( sDataType.equals("uint16") ) return DAP.DATA_TYPE_UInt16;
		if( sDataType.equals("int32") ) return DAP.DATA_TYPE_Int32;
		if( sDataType.equals("uint32") ) return DAP.DATA_TYPE_UInt32;
		if( sDataType.equals("float") ) return DAP.DATA_TYPE_Float32;
		if( sDataType.equals("double") ) return DAP.DATA_TYPE_Float64;
		if( sDataType.equals("string") ) return DAP.DATA_TYPE_String;
		return 0;
	}

	public static DAP_VARIABLE getType( BaseType bt ){
		if( bt instanceof opendap.dap.DArray ) return DAP_VARIABLE.Array;
		if( bt instanceof opendap.dap.DGrid ) return DAP_VARIABLE.Grid;
		if( bt instanceof opendap.dap.DSequence ) return DAP_VARIABLE.Sequence;
		if( bt instanceof opendap.dap.DStructure ) return DAP_VARIABLE.Structure;
		if( bt instanceof opendap.dap.DByte ) return DAP_VARIABLE.Byte;
		if( bt instanceof opendap.dap.DInt16 ) return DAP_VARIABLE.Int16;
		if( bt instanceof opendap.dap.DUInt16 ) return DAP_VARIABLE.UInt16;
		if( bt instanceof opendap.dap.DInt32 ) return DAP_VARIABLE.Int32;
		if( bt instanceof opendap.dap.DUInt32 ) return DAP_VARIABLE.UInt32;
		if( bt instanceof opendap.dap.DFloat32 ) return DAP_VARIABLE.Float32;
		if( bt instanceof opendap.dap.DFloat64 ) return DAP_VARIABLE.Float64;
		if( bt instanceof opendap.dap.DString ) return DAP_VARIABLE.String;
		return null;
	}
	
	public static DAP_TYPE getTypeEnumByName( String sTypeName ){
		switch( getTypeByName( sTypeName ) ){
			case 0:
				return null;
			case DAP.DATA_TYPE_Byte:	return DAP_TYPE.Byte;
			case DAP.DATA_TYPE_Int16:	return DAP_TYPE.Int16;
			case DAP.DATA_TYPE_UInt16:	return DAP_TYPE.UInt16;
			case DAP.DATA_TYPE_Int32:	return DAP_TYPE.Int32;
			case DAP.DATA_TYPE_UInt32:	return DAP_TYPE.UInt32;
			case DAP.DATA_TYPE_Float32:	return DAP_TYPE.Float32;
			case DAP.DATA_TYPE_Float64:	return DAP_TYPE.Float64;
			case DAP.DATA_TYPE_String:	return DAP_TYPE.String;
			default: return null;
		}
	}

	public static String getType_String( BaseType bt ){
		if( bt == null ) return "[null error]";
		if( bt instanceof DByte ) return "Byte";
		else if( bt instanceof DInt16 ) return "Int16";
		else if( bt instanceof DInt32 ) return "Int32";
		else if( bt instanceof DUInt32 ) return "UInt32";
		else if( bt instanceof DFloat32 ) return "Float32";
		else if( bt instanceof DFloat64 ) return "Float64";
		else if( bt instanceof DString ) return "String";
		else if( bt instanceof DArray ) return "Array";
		else if( bt instanceof DConstructor ) return "Constructor";
		else if( bt instanceof DStructure ) return "Structure";
		else if( bt instanceof DGrid ) return "Grid";
		else if( bt instanceof DSequence ) return "Sequence";
		else if( bt instanceof DVector ) return "Vector";
		else return "[unknown type: " + bt.getClass() + "]";
	}
	public static String getDArrayValueString( DArray darray, int xDim1_1, int xDim2_1, int xDim3_1 ){
		int xDim = 0;
		int ctDim = darray.numDimensions();
		int lenDim1, lenDim2;
		try {
			switch( ctDim ){
				case 1:
					xDim = xDim1_1 - 1;
					break;
				case 2:
					lenDim1 = darray.getDimension(0).getSize();
					xDim = (xDim1_1 - 1) * lenDim1 + xDim2_1 - 1;
					break;
				case 3:
					lenDim1 = darray.getDimension(0).getSize();
					lenDim2 = darray.getDimension(1).getSize();
					xDim = (xDim1_1 - 1) * lenDim1 * lenDim2 + (xDim2_1 - 1) * lenDim2 + xDim3_1 - 1;
					break;
				default: return "?";
			}
		} catch(Exception ex) { return "?"; }
		PrimitiveVector pvector = darray.getPrimitiveVector();
		switch( getDArrayType(darray) ){
			case DATA_TYPE_Byte: return Byte.toString(((BytePrimitiveVector)pvector).getValue(xDim));
			case DATA_TYPE_Int16: return Short.toString(((Int16PrimitiveVector)pvector).getValue(xDim));
			case DATA_TYPE_Int32: return Integer.toString(((Int32PrimitiveVector)pvector).getValue(xDim));
			case DATA_TYPE_UInt16: return Integer.toString((int)(((UInt16PrimitiveVector)pvector).getValue(xDim)) & 0xFFFF);
			case DATA_TYPE_UInt32: return Long.toString((long)((UInt32PrimitiveVector)pvector).getValue(xDim) & 0xFFFFFFFF);
			case DATA_TYPE_Float32: return Float.toString(((Float32PrimitiveVector)pvector).getValue(xDim));
			case DATA_TYPE_Float64: return Double.toString(((Float64PrimitiveVector)pvector).getValue(xDim));
			default: return "?";
		}
	}

	public static String[] getDArrayStringVector0( DArray darray, StringBuffer sbError ){
		int ctDim = darray.numDimensions();
		if( ctDim != 1 ){
			sbError.append("DArray is not a vector");
		}
		int lenDim1;
		try {
			lenDim1 = darray.getDimension(0).getSize();
		} catch(Exception ex) {
			sbError.append("failed to get dimension length");
		    return null;
		}
		switch( getDArrayType(darray) ){
			case DATA_TYPE_Byte:
				if( !Utility.zMemoryCheck(lenDim1, 3, sbError) ) return null; else break;
			case DATA_TYPE_Int16:
				if( !Utility.zMemoryCheck(lenDim1, 6, sbError) ) return null; else break;
			case DATA_TYPE_Int32:
				if( !Utility.zMemoryCheck(lenDim1, 12, sbError) ) return null; else break;
			case DATA_TYPE_UInt16:
				if( !Utility.zMemoryCheck(lenDim1, 12, sbError) ) return null; else break;
			case DATA_TYPE_UInt32:
				if( !Utility.zMemoryCheck(lenDim1, 16, sbError) ) return null; else break;
			case DATA_TYPE_Float32:
				if( !Utility.zMemoryCheck(lenDim1, 12, sbError) ) return null; else break;
			case DATA_TYPE_Float64:
				if( !Utility.zMemoryCheck(lenDim1, 16, sbError) ) return null; else break;
			default:
			case DATA_TYPE_String:
				if( !Utility.zMemoryCheck(lenDim1, 24, sbError) ) return null; else break;
		}
		String[] asValues = new String[lenDim1];
		PrimitiveVector pvector = darray.getPrimitiveVector();
		for( int xValue = 0; xValue < lenDim1; xValue++ ){
			switch( getDArrayType(darray) ){
				case DATA_TYPE_Byte: asValues[xValue] = Byte.toString(((BytePrimitiveVector)pvector).getValue(xValue));
				case DATA_TYPE_Int16: asValues[xValue] = Short.toString(((Int16PrimitiveVector)pvector).getValue(xValue));
				case DATA_TYPE_Int32: asValues[xValue] = Integer.toString(((Int32PrimitiveVector)pvector).getValue(xValue));
				case DATA_TYPE_UInt16: asValues[xValue] = Integer.toString((int)(((UInt16PrimitiveVector)pvector).getValue(xValue)) & 0xFFFF);
				case DATA_TYPE_UInt32: asValues[xValue] = Long.toString((long)((UInt32PrimitiveVector)pvector).getValue(xValue) & 0xFFFFFFFF);
				case DATA_TYPE_Float32: asValues[xValue] = Float.toString(((Float32PrimitiveVector)pvector).getValue(xValue));
				case DATA_TYPE_Float64: asValues[xValue] = Double.toString(((Float64PrimitiveVector)pvector).getValue(xValue));
				case DATA_TYPE_String: asValues[xValue] = ((BaseType[])pvector.getInternalStorage())[xValue].toString();
				default:
					sbError.append("cannot handle DArray type");
					return null;
			}
		}
		return asValues;
	}

	public static int getArraySize( Object oArray ){
		if( oArray instanceof byte[] ){
			byte[] ab = (byte[])oArray;
			return ab.length;
		} else if( oArray instanceof short[] ){
				short[] ash = (short[])oArray;
				return ash.length;
		} else if( oArray instanceof int[] ){
				int[] ai = (int[])oArray;
				return ai.length;
		} else if( oArray instanceof long[] ){
				long[] an = (long[])oArray;
				return an.length;
		} else if( oArray instanceof float[] ){
				float[] af = (float[])oArray;
				return af.length;
		} else if( oArray instanceof double[] ){
				double[] ad = (double[])oArray;
				return ad.length;
		} else if( oArray instanceof String[] ){
				String[] as = (String[])oArray;
				return as.length;
		} else {
				return -1;
		}
	}

	public static String getValueString( Object[] egg, int eTYPE ){
		if( egg == null ) return "";
		if( egg[0] == null ) return "";
		int ctValues = DAP.getArraySize(egg[0]) - 1;
		StringBuffer sb = new StringBuffer(80);
		for(int xValue = 1; xValue <= ctValues; xValue++){
			if( xValue > 1 ) sb.append(' ');
			switch( eTYPE ){
				case DATA_TYPE_Byte:
				case DATA_TYPE_Int16:
					if( egg[0] instanceof short[] ){
						sb.append(((short[])egg[0])[xValue]); break;
					} else {
						sb.append("type mismatch (short[] : " + egg[0].getClass() + ")"); break;
					}
				case DATA_TYPE_UInt16:
				case DATA_TYPE_Int32:
					if( egg[0] instanceof int[] ){
						sb.append(((int[])egg[0])[xValue]); break;
					} else {
						sb.append("type mismatch (int[] : " + egg[0].getClass() + ")"); break;
					}
				case DATA_TYPE_UInt32:
					if( egg[0] instanceof long[] ){
						sb.append(((long[])egg[0])[xValue]); break;
					} else {
						sb.append("type mismatch (int[] : " + egg[0].getClass() + ")"); break;
					}
				case DATA_TYPE_Float32:
					if( egg[0] instanceof float[] ){
						sb.append(((float[])egg[0])[xValue]); break;
					} else {
						sb.append("type mismatch (float[] : " + egg[0].getClass() + ")"); break;
					}
				case DATA_TYPE_Float64:
					if( egg[0] instanceof double[] ){
						sb.append(((double[])egg[0])[xValue]); break;
					} else {
						sb.append("type mismatch (double[] : " + egg[0].getClass() + ")"); break;
					}
				case DAP.DATA_TYPE_String:
				default:
					// not supported
			}
		}
		return sb.toString();
	}

	/** converts an array of doubles into an array of type eTYPE which is then put into an egg */
	// todo figure out unsigned conversions
	public static Object[] convertToEgg_Java( double[] ad, int eJAVA_TYPE, StringBuffer sbError ){
		if( ad == null ){
			sbError.append("no array of doubles was supplied");
			return null;
		}
		int lenData = ad.length;
		Object[] egg = new Object[1];
		switch( eJAVA_TYPE ){
			case DAP.JAVA_TYPE_byte:
				if( !Utility.zMemoryCheck(lenData, 1, sbError) ) return null;
				byte[] ab = new byte[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					ab[xValue] = (byte)ad[xValue];
				}
				egg[0] = ab;
				break;
			case DAP.JAVA_TYPE_short:
				if( !Utility.zMemoryCheck(lenData, 2, sbError) ) return null;
				short[] ash = new short[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					ash[xValue] = (short)ad[xValue];
				}
				egg[0] = ash;
				break;
			case DAP.JAVA_TYPE_int:
				if( !Utility.zMemoryCheck(lenData, 4, sbError) ) return null;
				int[] ai = new int[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					ai[xValue] = (int)ad[xValue];
				}
				egg[0] = ai;
				break;
			case DAP.JAVA_TYPE_long:
				if( !Utility.zMemoryCheck(lenData, 8, sbError) ) return null;
				long[] an = new long[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					an[xValue] = (long)ad[xValue];
				}
				egg[0] = an;
				break;
			case DAP.JAVA_TYPE_float:
				if( !Utility.zMemoryCheck(lenData, 4, sbError) ) return null;
				float[] af = new float[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					af[xValue] = (float)ad[xValue];
				}
				egg[0] = af;
				break;
			case DAP.JAVA_TYPE_double:
				egg[0] = ad;
				break;
			default:
				sbError.append("unknown Java data type: " + eJAVA_TYPE);
				return null;
		}
		return egg;
	}

	// todo handle errors
	public static Object[] convertToEgg_Java( String[] as, int eJAVA_TYPE, StringBuffer sbError ){
		if( as == null ){
			sbError.append("no array of Strings was supplied");
			return null;
		}
		int lenData = as.length;
		Object[] egg = new Object[1];
		switch( eJAVA_TYPE ){
			case DAP.JAVA_TYPE_byte:
				if( !Utility.zMemoryCheck(lenData, 1, sbError) ) return null;
				byte[] ab = new byte[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					try {
						ab[xValue] = Byte.parseByte( as[xValue] );
					} catch(Exception ex) {
						// ignore errors
					}
				}
				egg[0] = ab;
				break;
			case DAP.JAVA_TYPE_short:
				if( !Utility.zMemoryCheck(lenData, 2, sbError) ) return null;
				short[] ash = new short[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					try {
						ash[xValue] = Short.parseShort( as[xValue] );
					} catch(Exception ex) {
						// ignore errors
					}
				}
				egg[0] = ash;
				break;
			case DAP.JAVA_TYPE_int:
				if( !Utility.zMemoryCheck(lenData, 4, sbError) ) return null;
				int[] ai = new int[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					try {
						ai[xValue] = Integer.parseInt( as[xValue] );
					} catch(Exception ex) {
						// ignore errors
					}
				}
				egg[0] = ai;
				break;
			case DAP.JAVA_TYPE_long:
				if( !Utility.zMemoryCheck(lenData, 8, sbError) ) return null;
				long[] an = new long[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					try {
						an[xValue] = Long.parseLong( as[xValue] );
					} catch(Exception ex) {
						// ignore errors
					}
				}
				egg[0] = an;
				break;
			case DAP.JAVA_TYPE_float:
				if( !Utility.zMemoryCheck(lenData, 4, sbError) ) return null;
				float[] af = new float[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					try {
						af[xValue] = Float.parseFloat( as[xValue] );
					} catch(Exception ex) {
						// ignore errors
					}
				}
				egg[0] = af;
				break;
			case DAP.JAVA_TYPE_double:
				if( !Utility.zMemoryCheck(lenData, 8, sbError) ) return null;
				double[] ad = new double[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					try {
						ad[xValue] = Double.parseDouble( as[xValue] );
					} catch(Exception ex) {
						// ignore errors
					}
				}
				egg[0] = ad;
				break;
			default:
				sbError.append("unknown Java data type: " + eJAVA_TYPE);
				return null;
		}
		return egg;
	}

	public static double[] convertToDouble( Object[] eggArray, int eTYPE, Object[] eggMissing, StringBuffer sbError ){
		if( eggArray == null ){
			sbError.append("no egg was supplied");
			return null;
		}
		Object oArray = eggArray[0];
		return convertToDouble( oArray, eTYPE, eggMissing, sbError );
	}

	public static double[] convertToDouble( Object oArray,  int eTYPE, Object[] eggMissing, StringBuffer sbError ){
		if( oArray == null ){
			sbError.append("no array object was supplied");
			return null;
		}
		boolean zHasMissing = ( eggMissing != null && eggMissing[0] != null );
		int lenData;
		double[] adData0;
		int lenMissing;
		switch( eTYPE ){
			case DATA_TYPE_Byte:
			case DATA_TYPE_Int16:
				short[] ash = (short[])oArray;
				short[] ashMissing = ( zHasMissing ? (short[])eggMissing[0] : null );
				lenMissing = ( zHasMissing ? ashMissing.length - 1: 0 );
				lenData = ash.length;
				adData0 = new double[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					double dValue = (double)ash[xValue];
					if( zHasMissing ){
						for( int xMissing = 1; xMissing <= lenMissing; xMissing++ ){
							if( ash[xValue] == ashMissing[xMissing] ){
								dValue = Double.NaN;
								break;
							}
						}
					}
					adData0[xValue] = dValue;
				}
				break;
			case DATA_TYPE_UInt16:
			case DATA_TYPE_Int32:
				int[] ai = (int[])oArray;
				lenData = ai.length;
				int[] aiMissing = ( zHasMissing ? (int[])eggMissing[0] : null );
				lenMissing = ( zHasMissing ? aiMissing.length - 1 : 0 );
				adData0 = new double[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					double dValue = (double)ai[xValue];
					if( zHasMissing ){
						for( int xMissing = 1; xMissing <= lenMissing; xMissing++ ){
							if( ai[xValue] == aiMissing[xMissing] ){
								dValue = Double.NaN;
								break;
							}
						}
					}
					adData0[xValue] = dValue;
				}
				break;
			case DAP.DATA_TYPE_UInt32:
				long[] an = (long[])oArray;
				lenData = an.length;
				long[] anMissing = ( zHasMissing ? (long[])eggMissing[0] : null );
				lenMissing = ( zHasMissing ? anMissing.length - 1 : 0 );
				adData0 = new double[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					double dValue = (double)an[xValue];
					if( zHasMissing ){
						for( int xMissing = 1; xMissing <= lenMissing; xMissing++ ){
							if( an[xValue] == anMissing[xMissing] ){
								dValue = Double.NaN;
								break;
							}
						}
					}
					adData0[xValue] = dValue;
				}
				break;
			case DAP.DATA_TYPE_Float32:
				float[] af = (float[])oArray;
				lenData = af.length;
				float[] afMissing = ( zHasMissing ? (float[])eggMissing[0] : null );
				lenMissing = ( zHasMissing ? afMissing.length - 1 : 0 );
				adData0 = new double[lenData];
				for( int xValue = 0; xValue < lenData; xValue++ ){
					double dValue = (double)af[xValue];
					if( zHasMissing ){
						for( int xMissing = 1; xMissing <= lenMissing; xMissing++ ){
							if( af[xValue] == afMissing[xMissing] ){
								dValue = Double.NaN;
								break;
							}
						}
					}
					adData0[xValue] = dValue;
				}
				break;
			case DAP.DATA_TYPE_Float64:
				adData0 = (double[])oArray;
				lenData = adData0.length;
				double[] adMissing = ( zHasMissing ? (double[])eggMissing[0] : null );
				lenMissing = ( zHasMissing ? adMissing.length - 1 : 0 );
				if( zHasMissing ){
					for( int xValue = 0; xValue < lenData; xValue++ ){
						for( int xMissing = 1; xMissing <= lenMissing; xMissing++ ){
							if( adData0[xValue] == adMissing[xMissing] ){
								adData0[xValue] = Double.NaN;
								break;
							}
						}
					}
				}
				break;
			case DAP.DATA_TYPE_String:
				sbError.append("string type cannot be converted to double");
				return null;
			default:
				sbError.append("unknown array data type: " + eTYPE);
				return null;
		}
		return adData0;
	}

	public static double convertToDouble( BaseType bt ){
		if( bt instanceof DByte ){
			return (double)((int)(((DByte)bt).getValue()) & 0xFF);
		} else if( bt instanceof DInt16 ){
			return (double)((DInt16)bt).getValue();
		} else if( bt instanceof DUInt16 ){
			return (double)((int)(((DUInt16)bt).getValue()) & 0xFFFF);
		} else if( bt instanceof DInt32 ){
			return (double)(((DInt32)bt).getValue());
		} else if( bt instanceof DUInt32 ){
			return (double)((long)(((DUInt32)bt).getValue()) & 0xFFFFFFFF);
		} else if( bt instanceof DFloat32 ){
			return (double)(((DFloat32)bt).getValue());
		} else if( bt instanceof DFloat64 ){
			return ((DFloat64)bt).getValue();
		} else if( bt instanceof DString ){
			String s = ((DString)bt).getValue();
			try {
				return Double.parseDouble(s);
			} catch(Exception ex) {
				return Double.NaN;
			}
		} else {
			return Double.NaN;
		}
	}

	public static int toSigned( byte bUnsigned ){ return ((int)bUnsigned) & 0xFF; }
	public static int toSigned( short shUnsigned ){ return ((int)shUnsigned) & 0xFFFF; }
	public static long toSigned( int iUnsigned ){ return ((long)iUnsigned) & 0xFFFFFFFF; }
	public static boolean zTransform( Object[] eggData, int eDataType, int[] aiDimLengths, boolean zReverseD1, boolean zReverseD2, StringBuffer sbError ){
		if( !zReverseD1 && !zReverseD2 ) return true; // nothing to do
		try {
			int lenD1 = aiDimLengths[1];
			int lenD2 = aiDimLengths[2];
			if( lenD1 < 2 || lenD2 < 2 ){ // can only do reversal (inversion does not mean anything)
				if( (lenD1 > 1 && zReverseD1) || (lenD2 > 2 && zReverseD2) ){
					return zReverse( eggData, eDataType, sbError );
				}
			}
			int xValue = -1;
			int xValue2 = -1;
			int ctValues;
			long[] anValues = null;
			short[] ashValues = null;
			int[] aiValues = null;
			float[] afValues = null;
			double[] adValues = null;
			String[] asValues = null;
			switch( eDataType ){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:
					ashValues = (short[])eggData[0];
					ctValues = ashValues.length;
					break;
				case DAP.DATA_TYPE_UInt16:
				case DAP.DATA_TYPE_Int32:
					aiValues = (int[])eggData[0];
					ctValues = aiValues.length;
					break;
				case DAP.DATA_TYPE_UInt32:
					anValues = (long[])eggData[0];
					ctValues = anValues.length;
					break;
				case DAP.DATA_TYPE_Float32:
					afValues = (float[])eggData[0];
					ctValues = afValues.length;
					break;
				case DAP.DATA_TYPE_Float64:
					adValues = (double[])eggData[0];
					ctValues = adValues.length;
					break;
				case DAP.DATA_TYPE_String:
					asValues = (String[])eggData[0];
					ctValues = asValues.length;
					break;
				default:
					sbError.append("unknown array data type: " + eDataType);
					return false;
			}
			switch( eDataType ){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:
					if( zReverseD1 & !zReverseD2 ){
						for( int xD2 = 1; xD2 <= lenD2; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1 >> 1; xD1++ ){
								xValue = (xD2 - 1) * lenD1 + xD1 - 1;
								xValue2 = (xD2 - 1) * lenD1 + lenD1 - xD1;
								short shValue1 = ashValues[xValue];
								ashValues[xValue] = ashValues[xValue2];
								ashValues[xValue2] = shValue1;
							}
						}
					} else if( !zReverseD1 & zReverseD2 ){
						xValue = -1;
						for( int xD2 = 1; xD2 <= lenD2 >> 1; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1; xD1++ ){
								xValue++;
								xValue2 = lenD1*(lenD2 - xD2) + xD1 - 1;
								short shValue1 = ashValues[xValue];
								ashValues[xValue] = ashValues[xValue2];
								ashValues[xValue2] = shValue1;
							}
						}
					} else if( zReverseD1 & zReverseD2 ){
						for( xValue = 1; xValue <= ctValues >> 1; xValue++ ){
							xValue2 = ctValues - xValue;
							short shValue1 = ashValues[xValue];
							ashValues[xValue] = ashValues[xValue2];
							ashValues[xValue2] = shValue1;
						}
					}
					eggData[0] = ashValues;
					break;
				case DAP.DATA_TYPE_UInt16:
				case DAP.DATA_TYPE_Int32:
					if( zReverseD1 & !zReverseD2 ){
						for( int xD2 = 1; xD2 <= lenD2; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1 >> 1; xD1++ ){
								xValue = (xD2 - 1) * lenD1 + xD1 - 1;
								xValue2 = (xD2 - 1) * lenD1 + lenD1 - xD1;
								int iValue1 = aiValues[xValue];
								aiValues[xValue] = aiValues[xValue2];
								aiValues[xValue2] = iValue1;
							}
						}
					} else if( !zReverseD1 & zReverseD2 ){
						xValue = -1;
						for( int xD2 = 1; xD2 <= lenD2 >> 1; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1; xD1++ ){
								xValue++;
								xValue2 = lenD1*(lenD2 - xD2) + xD1 - 1;
								int iValue1 = aiValues[xValue];
								aiValues[xValue] = aiValues[xValue2];
								aiValues[xValue2] = iValue1;
							}
						}
					} else if( zReverseD1 & zReverseD2 ){
						for( xValue = 1; xValue <= ctValues >> 1; xValue++ ){
							xValue2 = ctValues - xValue;
							int iValue1 = aiValues[xValue];
							aiValues[xValue] = aiValues[xValue2];
							aiValues[xValue2] = iValue1;
						}
					}
					eggData[0] = aiValues;
					break;
				case DAP.DATA_TYPE_UInt32:
					if( zReverseD1 & !zReverseD2 ){
						for( int xD2 = 1; xD2 <= lenD2; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1 >> 1; xD1++ ){
								xValue = (xD2 - 1) * lenD1 + xD1 - 1;
								xValue2 = (xD2 - 1) * lenD1 + lenD1 - xD1;
								long nValue1 = anValues[xValue];
								anValues[xValue] = anValues[xValue2];
								anValues[xValue2] = nValue1;
							}
						}
					} else if( !zReverseD1 & zReverseD2 ){
						xValue = -1;
						for( int xD2 = 1; xD2 <= lenD2 >> 1; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1; xD1++ ){
								xValue++;
								xValue2 = lenD1*(lenD2 - xD2) + xD1 - 1;
								long nValue1 = anValues[xValue];
								anValues[xValue] = anValues[xValue2];
								anValues[xValue2] = nValue1;
							}
						}
					} else if( zReverseD1 & zReverseD2 ){
						for( xValue = 1; xValue <= ctValues >> 1; xValue++ ){
							xValue2 = ctValues - xValue;
							long nValue1 = anValues[xValue];
							anValues[xValue] = anValues[xValue2];
							anValues[xValue2] = nValue1;
						}
					}
					eggData[0] = anValues;
					break;
				case DAP.DATA_TYPE_Float32:
					if( zReverseD1 & !zReverseD2 ){
						for( int xD2 = 1; xD2 <= lenD2; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1 >> 1; xD1++ ){
								xValue = (xD2 - 1) * lenD1 + xD1 - 1;
								xValue2 = (xD2 - 1) * lenD1 + lenD1 - xD1;
								float fValue1 = afValues[xValue];
								afValues[xValue] = afValues[xValue2];
								afValues[xValue2] = fValue1;
							}
						}
					} else if( !zReverseD1 & zReverseD2 ){
						xValue = -1;
						for( int xD2 = 1; xD2 <= lenD2 >> 1; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1; xD1++ ){
								xValue++;
								xValue2 = lenD1*(lenD2 - xD2) + xD1 - 1;
								float fValue1 = afValues[xValue];
								afValues[xValue] = afValues[xValue2];
								afValues[xValue2] = fValue1;
							}
						}
					} else if( zReverseD1 & zReverseD2 ){
						for( xValue = 1; xValue <= ctValues >> 1; xValue++ ){
							xValue2 = ctValues - xValue;
							float fValue1 = afValues[xValue];
							afValues[xValue] = afValues[xValue2];
							afValues[xValue2] = fValue1;
						}
					}
					eggData[0] = afValues;
					break;
				case DAP.DATA_TYPE_Float64:
					if( zReverseD1 & !zReverseD2 ){
						for( int xD2 = 1; xD2 <= lenD2; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1 >> 1; xD1++ ){
								xValue = (xD2 - 1) * lenD1 + xD1 - 1;
								xValue2 = (xD2 - 1) * lenD1 + lenD1 - xD1;
								double dValue1 = adValues[xValue];
								adValues[xValue] = adValues[xValue2];
								adValues[xValue2] = dValue1;
							}
						}
					} else if( !zReverseD1 & zReverseD2 ){
						xValue = -1;
						for( int xD2 = 1; xD2 <= lenD2 >> 1; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1; xD1++ ){
								xValue++;
								xValue2 = lenD1*(lenD2 - xD2) + xD1 - 1;
								double dValue1 = adValues[xValue];
								adValues[xValue] = adValues[xValue2];
								adValues[xValue2] = dValue1;
							}
						}
					} else if( zReverseD1 & zReverseD2 ){
						for( xValue = 1; xValue <= ctValues >> 1; xValue++ ){
							xValue2 = ctValues - xValue;
							double dValue1 = adValues[xValue];
							adValues[xValue] = adValues[xValue2];
							adValues[xValue2] = dValue1;
						}
					}
					eggData[0] = adValues;
					break;
				case DAP.DATA_TYPE_String:
					if( zReverseD1 & !zReverseD2 ){
						for( int xD2 = 1; xD2 <= lenD2; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1 >> 1; xD1++ ){
								xValue = (xD2 - 1) * lenD1 + xD1 - 1;
								xValue2 = (xD2 - 1) * lenD1 + lenD1 - xD1;
								String sValue1 = asValues[xValue];
								asValues[xValue] = asValues[xValue2];
								asValues[xValue2] = sValue1;
							}
						}
					} else if( !zReverseD1 & zReverseD2 ){
						xValue = -1;
						for( int xD2 = 1; xD2 <= lenD2 >> 1; xD2++ ){
							for( int xD1 = 1; xD1 <= lenD1; xD1++ ){
								xValue++;
								xValue2 = lenD1*(lenD2 - xD2) + xD1 - 1;
								String sValue1 = asValues[xValue];
								asValues[xValue] = asValues[xValue2];
								asValues[xValue2] = sValue1;
							}
						}
					} else if( zReverseD1 & zReverseD2 ){
						for( xValue = 1; xValue <= ctValues >> 1; xValue++ ){
							xValue2 = ctValues - xValue;
							String sValue1 = asValues[xValue];
							asValues[xValue] = asValues[xValue2];
							asValues[xValue2] = sValue1;
						}
					}
					eggData[0] = asValues;
					break;
				default:
					sbError.append("unknown array data type: " + eDataType);
					return false;
			}
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	public static boolean zReverse( Object[] eggData, int eDataType, StringBuffer sbError ){
		try {
			int ctValues;
			switch( eDataType ){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:
					short[] ashValues = (short[])eggData[0];
					ctValues = ashValues.length;
					for( int xValue = 0; xValue < ctValues/2; xValue++ ){
						short shSwap = ashValues[xValue];
						ashValues[xValue] = ashValues[ctValues - xValue - 1];
						ashValues[ctValues - xValue - 1] = shSwap;
					}
					break;
				case DAP.DATA_TYPE_UInt16:
				case DAP.DATA_TYPE_Int32:
					int[] aiValues = (int[])eggData[0];
					ctValues = aiValues.length;
					for( int xValue = 0; xValue < ctValues/2; xValue++ ){
						int iSwap = aiValues[xValue];
						aiValues[xValue] = aiValues[ctValues - xValue - 1];
						aiValues[ctValues - xValue - 1] = iSwap;
					}
					break;
				case DAP.DATA_TYPE_UInt32:
					long[] anValues = (long[])eggData[0];
					ctValues = anValues.length;
					for( int xValue = 0; xValue < ctValues/2; xValue++ ){
						long nSwap = anValues[xValue];
						anValues[xValue] = anValues[ctValues - xValue - 1];
						anValues[ctValues - xValue - 1] = nSwap;
					}
					break;
				case DAP.DATA_TYPE_Float32:
					float[] afValues = (float[])eggData[0];
					ctValues = afValues.length;
					for( int xValue = 0; xValue < ctValues/2; xValue++ ){
						float fSwap = afValues[xValue];
						afValues[xValue] = afValues[ctValues - xValue - 1];
						afValues[ctValues - xValue - 1] = fSwap;
					}
					break;
				case DAP.DATA_TYPE_Float64:
					double[] adValues = (double[])eggData[0];
					ctValues = adValues.length;
					for( int xValue = 0; xValue < ctValues/2; xValue++ ){
						double dSwap = adValues[xValue];
						adValues[xValue] = adValues[ctValues - xValue - 1];
						adValues[ctValues - xValue - 1] = dSwap;
					}
					break;
				case DAP.DATA_TYPE_String:
					String[] asValues = (String[])eggData[0];
					ctValues = asValues.length;
					for( int xValue = 0; xValue < ctValues/2; xValue++ ){
						String sSwap = asValues[xValue];
						asValues[xValue] = asValues[ctValues - xValue - 1];
						asValues[ctValues - xValue - 1] = sSwap;
					}
					break;
				default:
					sbError.append("unknown array data type: " + eDataType);
					return false;
			}
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	public static boolean zDimension_Add( Object[] eggData, DAP_TYPE eDataType, int iNewDimLength, StringBuffer sbError ){
		try {
			int ctValues;
			int ctValues_new;
			switch( eDataType ){
				case Byte:
				case Int16:
					short[] ashValues = (short[])eggData[0];
					ctValues = ashValues.length;
					ctValues_new = ctValues * iNewDimLength;
					short[] ashValues_new = new short[ctValues_new];
					System.arraycopy( ashValues, 0, ashValues_new, 0, ctValues );
					eggData[0] = ashValues_new;
					break;
				case UInt16:
				case Int32:
					int[] aiValues = (int[])eggData[0];
					ctValues = aiValues.length;
					ctValues_new = ctValues * iNewDimLength;
					int[] aiValues_new = new int[ctValues_new];
					System.arraycopy( aiValues, 0, aiValues_new, 0, ctValues );
					eggData[0] = aiValues_new;
					break;
				case UInt32:
					long[] anValues = (long[])eggData[0];
					ctValues = anValues.length;
					ctValues_new = ctValues * iNewDimLength;
					long[] anValues_new = new long[ctValues_new];
					System.arraycopy( anValues, 0, anValues_new, 0, ctValues );
					eggData[0] = anValues_new;
					break;
				case Float32:
					float[] afValues = (float[])eggData[0];
					ctValues = afValues.length;
					ctValues_new = ctValues * iNewDimLength;
					float[] afValues_new = new float[ctValues_new];
					System.arraycopy( afValues, 0, afValues_new, 0, ctValues );
					eggData[0] = afValues_new;
					break;
				case Float64:
					double[] adValues = (double[])eggData[0];
					ctValues = adValues.length;
					ctValues_new = ctValues * iNewDimLength;
					double[] adValues_new = new double[ctValues_new];
					System.arraycopy( adValues, 0, adValues_new, 0, ctValues );
					eggData[0] = adValues_new;
					break;
				case String:
					String[] asValues = (String[])eggData[0];
					ctValues = asValues.length;
					ctValues_new = ctValues * iNewDimLength;
					String[] asValues_new = new String[ctValues_new];
					System.arraycopy( asValues, 0, asValues_new, 0, ctValues );
					eggData[0] = asValues_new;
					break;
				default:
					sbError.append("unknown array data type: " + eDataType);
					return false;
			}
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	public static boolean zDimension_ModifySize( Object[] eggData, DAP_TYPE eDataType, int[] aiDimLengths, int xDimension1, int iNewDimLength, StringBuffer sbError ){
		try {
			int ctValues;
			int ctValues_new;
			int ctDimensions = aiDimLengths[0];
			int[] axDim = new int[ctDimensions + 1]; // used to traverse arrays
			int xDim = ctDimensions;
			short[] ashValues = null; short[] ashValues_new = null;
			int[] aiValues = null; int[] aiValues_new = null;
			long[] anValues = null; long[] anValues_new = null;
			float[] afValues = null; float[] afValues_new = null;
			double[] adValues = null; double[] adValues_new = null;
			String[] asValues = null; String[] asValues_new = null;
			switch( eDataType ){
				case Byte:
				case Int16:
					ashValues = (short[])eggData[0];
					ctValues = ashValues.length;
					ctValues_new = ctValues / aiDimLengths[xDimension1] * iNewDimLength;
					ashValues_new = new short[ctValues_new];
					eggData[0] = ashValues_new;
					
					break;
				case UInt16:
				case Int32:
					aiValues = (int[])eggData[0];
					ctValues = aiValues.length;
					ctValues_new = ctValues / aiDimLengths[xDimension1] * iNewDimLength;
					aiValues_new = new int[ctValues_new];
					System.out.format( "new size dap %d %d %d %d ", ctValues_new, ctValues, aiDimLengths[xDimension1], iNewDimLength );
					eggData[0] = aiValues_new;
					break;
				case UInt32:
					anValues = (long[])eggData[0];
					ctValues = anValues.length;
					ctValues_new = ctValues / aiDimLengths[xDimension1] * iNewDimLength;
					anValues_new = new long[ctValues_new];
					eggData[0] = anValues_new;
					break;
				case Float32:
					afValues = (float[])eggData[0];
					ctValues = afValues.length;
					ctValues_new = ctValues / aiDimLengths[xDimension1] * iNewDimLength;
					afValues_new = new float[ctValues_new];
					eggData[0] = afValues_new;
					break;
				case Float64:
					adValues = (double[])eggData[0];
					ctValues = adValues.length;
					ctValues_new = ctValues / aiDimLengths[xDimension1] * iNewDimLength;
					adValues_new = new double[ctValues_new];
					eggData[0] = adValues_new;
					break;
				case String:
					asValues = (String[])eggData[0];
					ctValues = asValues.length;
					ctValues_new = ctValues / aiDimLengths[xDimension1] * iNewDimLength;
					asValues_new = new String[ctValues_new];
					eggData[0] = asValues_new;
					break;
				default:
					sbError.append("unknown array data type: " + eDataType);
					return false;
			}

			// copy old values into new value array
			int iOldDimLength = aiDimLengths[xDimension1]; 
			aiDimLengths[xDimension1] = iNewDimLength;
			int xValue_old = 0; 
			int xValue_new = 0;
			while( true ){
				if( xValue_old == ctValues || xValue_new == ctValues_new ) break; // done
				switch( eDataType ){
					case Byte:
					case Int16:
						ashValues_new[xValue_new] = ashValues[xValue_old];
						break;
					case UInt16:
					case Int32:
						aiValues_new[xValue_new] = aiValues[xValue_old];
						break;
					case UInt32:
						anValues_new[xValue_new] = anValues[xValue_old];
						break;
					case Float32:
						afValues_new[xValue_new] = afValues[xValue_old];
						break;
					case Float64:
						adValues_new[xValue_new] = adValues[xValue_old];
						break;
					case String:
						asValues_new[xValue_new] = asValues[xValue_old];
						break;
					default:
						sbError.append("unknown array data type: " + eDataType);
						return false;
				}
				while( true ){
					boolean zOldInRange = true; // whether the address of the old value is in the range of the new array 
					while( true ){
						axDim[xDim]++;
						if( xDim == xDimension1 && axDim[xDim] >= iOldDimLength ) zOldInRange = false; 
						if( axDim[xDim] >= aiDimLengths[xDim] || !zOldInRange ){
							if( xDim == xDimension1 && axDim[xDim] < iNewDimLength ){ // in this case the new array is larger in this dimension
								xValue_new += (iNewDimLength - axDim[xDim]); //...and we need to skip over the values not in the old array 
							}
                            axDim[xDim] = 0;
							xDim--;
						} else break;
					}
					xDim = ctDimensions;
					if( zOldInRange ) break;
				}
				xValue_old++;
				xValue_new++;
			}
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}
	
	
	public static String toBinaryString( byte b ){
		StringBuffer sb = new StringBuffer(8);
		for(int xDigit = 1; xDigit <= 8; xDigit++){
			sb.insert(0, (b & 1));
			b = (byte)(b >> 1);
		}
		return sb.toString();
	}

	public static String toBinaryString( int i ){
		StringBuffer sb = new StringBuffer(32);
		for(int xDigit = 1; xDigit <= 32; xDigit++){
			sb.insert(0, (i & 1));
			i = i >> 1;
		}
		return sb.toString();
	}

	public static String dump( BaseType bt ){
		if( bt == null ) return "[null]";
		StringBuffer sb = new StringBuffer(400);
		try {
			dump( bt, 0, sb );
			return sb.toString();
		} catch(Exception ex) {
			return "error calling DAP basetype dump: " + ex;
		}
	}

	private static void dump( BaseType bt, int iIndentLevel, StringBuffer sb ){
		try {
			if( bt == null ){
				sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("[null]\n");
				return;
			}
			switch( getType( bt ) ){
				case Array:
					DArray array = (DArray)bt;
					int ctDimension = array.numDimensions();
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append( getDArrayType_String( array ) ).append(' ');
					for( int xDimension = 1; xDimension <= ctDimension; xDimension++ ){
						DArrayDimension dimension = array.getDimension( xDimension - 1 );
						sb.append('[');
						sb.append( dimension.getName() );
						sb.append(" = ");
						sb.append( dimension.getSize() );
						sb.append(']');
						sb.append(';');
					}
					break;
				case Grid:
					DGrid grid = (DGrid)bt;
					DArray arrayGrid = (DArray)grid.getVar( DGrid.ARRAY );
					ctDimension = arrayGrid.numDimensions();
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("Grid {\n");
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel + 1)).append("Array:\n");
					dump( arrayGrid, iIndentLevel + 2, sb );
					sb.append("\n");
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel + 1)).append("Maps:\n");
					for( int xDimension = 1; xDimension <= ctDimension; xDimension++ ){
						DArrayDimension dimension = arrayGrid.getDimension( xDimension - 1 ); // zero-based array
						DArray darrayMapping = (DArray)grid.getVar( xDimension ); // 0 - value array, 1,2,3 - mapping arrays
						dump( darrayMapping, iIndentLevel + 2, sb );
					}
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("}");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				case Sequence:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("Structure {\n");
					DSequence sequence = (DSequence)bt;
					Enumeration enumerationSequence = sequence.getVariables();
					while( enumerationSequence.hasMoreElements() ){
						BaseType bt_subnode = (BaseType)enumerationSequence.nextElement();
						dump( bt, iIndentLevel + 1, sb );
						sb.append("\n");
					}
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("}");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				case Structure:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("Structure {\n");
					DStructure structure = (DStructure)bt;
					Enumeration enumerationStructure = structure.getVariables();
					while( enumerationStructure.hasMoreElements() ){
						BaseType bt_subnode = (BaseType)enumerationStructure.nextElement();
						dump( bt, iIndentLevel + 1, sb );
						sb.append("\n");
					}
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("}");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				case Byte:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("Byte");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				case Int16:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("Int16");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				case UInt16:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("UInt16");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				case Int32:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("Int32");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				case UInt32:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("UInt32");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				case Float32:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("Float32");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				case Float64:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("Float64");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				case String:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("String");
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
				default:
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append( bt.getTypeName() );
					sb.append(' ').append( bt.getLongName() ).append(';');
					break;
			}
		} catch(Exception ex) {
			sb.append( "[error: " + Utility.errorExtractStackTrace( ex ) + "]" );
		}
		return;
	}
	
	public static String dumpDAS( AttributeTable dasAT ){
		if( dasAT == null ) return "[null]";
		StringBuffer sb = new StringBuffer(400);
		try {
			dumpAttributeTable( dasAT, 0, sb );
			return sb.toString();
		} catch(Exception ex) {
			return "[error: " + ex + "]";
		}
	}

	private static void dumpAttributeTable( AttributeTable at, int iIndentLevel, StringBuffer sb ){
		if( at == null ){
			sb.append(Utility_String.sRepeatChar('\t', iIndentLevel)).append("[null]\n");
			return;
		}
		java.util.Enumeration enumAttributeNames = at.getNames();;
		while(enumAttributeNames.hasMoreElements()){
			String sAttributeName = (String)enumAttributeNames.nextElement();
			Attribute attribute = at.getAttribute(sAttributeName);
			if (attribute != null) {
				if (attribute.isContainer()) {
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel));
					sb.append("table: " + sAttributeName);
					try {
						AttributeTable atSubTable = attribute.getContainer();
						sb.append(":\n");
						dumpAttributeTable( atSubTable, iIndentLevel + 1, sb );
					} catch(Exception ex) {
						sb.append(": error: " + ex + "\n");
					}
				} else {
					sb.append(Utility_String.sRepeatChar('\t', iIndentLevel));
					sb.append(sAttributeName + " values: ");
					try {
						java.util.Enumeration enumAttributeValues = attribute.getValues();
						while(enumAttributeValues.hasMoreElements()){
							sb.append((String)enumAttributeValues.nextElement()).append("; ");
						}
						sb.append("\n");
					} catch(Exception ex) {
						sb.append(": error: " + ex + "\n");
					}
				}
			}
		}
		return;
	}

	/** returns only the first value of the named attribute */
	public static String getAttributeValue( DAS das, String sVariableName, String sAttributeName, StringBuffer sbError ){
		if( das == null || sVariableName == null ) return null;
		try {
			AttributeTable atMatching = findAttributeTable( das, sVariableName );
			if( atMatching == null ) return null;
			java.util.Enumeration enumNames = atMatching.getNames();
			while( enumNames.hasMoreElements() ){
				String sElementAttributeName = (String)enumNames.nextElement();
				if( sElementAttributeName.equalsIgnoreCase(sAttributeName) ){
					Attribute attr = atMatching.getAttribute(sAttributeName);
					java.util.Enumeration enumAttributeValues = attr.getValues();
					if( enumAttributeValues.hasMoreElements() ){
						String s = (String)enumAttributeValues.nextElement();
						if( s == null ) continue;
						s = s.trim();
						if( s.startsWith("\"") ) s = s.substring(1);
						if( s.endsWith("\"") ) s = s.substring(0, s.length() - 1);
						return s;
					}
				}
			}
			return null;
		} catch(Exception ex) {
			sbError.append("[error: " + ex + "]");
			return null;
		}
	}

	public static Object[] getMissingValues( DAS das, String sVariableName, StringBuffer sbError ){
		if( das == null || sVariableName == null ) return null;
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		try {
			AttributeTable atMatching = findAttributeTable( das, sVariableName );
			if( atMatching == null ) return null;
			java.util.Enumeration enumNames = atMatching.getNames();
			while( enumNames.hasMoreElements() ){
				String sAttributeName = (String)enumNames.nextElement();
				if( sAttributeName.toUpperCase().startsWith("MISSING") ){
					Attribute attr = atMatching.getAttribute(sAttributeName);
					java.util.Enumeration enumAttributeValues = attr.getValues();
					while(enumAttributeValues.hasMoreElements()){
						String s = (String)enumAttributeValues.nextElement();
						if( s == null ) continue;
						s = s.trim();
						if( s.startsWith("\"") ) s = s.substring(1);
						if( s.endsWith("\"") ) s = s.substring(0, s.length() - 1);
						list.add(s);
					}
					return list.toArray();
				}
			}
			return null;
		} catch(Exception ex) {
			sbError.append("[error: " + ex + "]");
			return null;
		}
	}

	public static String getAttributeString( DAS das, String sVariableName ){
		AttributeTable at = DAP.findAttributeTable( das, sVariableName );
		if( at == null ) return null;
		StringBuffer sb = new StringBuffer(80);
		dumpAttributeTable( at, 0, sb );
		return sb.toString();
	}

	public static AttributeTable findAttributeTable( AttributeTable at, String sVariableName ){
		try {
			if( at == null || sVariableName == null ){
				return null;
			} else {
				if( sVariableName.equals( at.getName() ) ){
					return at;
				} else {
					java.util.Enumeration enumNames = at.getNames();
					while( enumNames.hasMoreElements() ){
						String sName = (String)enumNames.nextElement();
						Attribute sub_attribute = at.getAttribute(sName);
						if( sub_attribute == null ){
							continue;
						} else if( sub_attribute.isContainer() ){
							AttributeTable atContainer = sub_attribute.getContainer();
							AttributeTable atResult = findAttributeTable( atContainer, sVariableName );
							if( atResult == null ) continue;
							return atResult;
						} else if( sub_attribute.isAlias() ) {
							continue;
						} else {
							continue;
						}
					}
				}
				return null;
			}
		} catch(Exception ex) {
			return null;
		}
	}

	public static boolean zCompareDDS( DataDDS dds1, DataDDS dds2 ){
		try {
			if( dds1 == null ) return dds2 == null;
			if( dds2 == null ) return false;
//System.out.println("comparing 1: ");
//			dds1.printDecl(System.out);
//System.out.println("  to 2: ");
//			dds2.printDecl(System.out);
			boolean zResult = zCompareVariables( dds1.getVariables(), dds2.getVariables() );
//System.out.println("returning: " + zResult);
			return zResult;
		} catch(Exception ex) {
			ApplicationController.vShowWarning("Unexpected error comparing variables: " + ex);
			return false;
		}
	}

	private static boolean zCompareVariables( Enumeration enumVar1, Enumeration enumVar2 ){
		if( enumVar1 == null ) return enumVar2 == null;
		if( enumVar2 == null ) return false;
		while( enumVar1.hasMoreElements() ){
			if( !enumVar2.hasMoreElements() ) return false;
			BaseType bt1 = (BaseType)enumVar1.nextElement();
			BaseType bt2 = (BaseType)enumVar2.nextElement();
			if( !zCompareBaseTypes( bt1, bt2 ) ) return false;
		}
		if( enumVar2.hasMoreElements() ) return false;
		return true;
	}

	private static boolean zCompareBaseTypes( BaseType bt1, BaseType bt2 ){
		if( bt1 == null ) return bt2 == null;
		if( bt2 == null ) return false;
		if( !bt1.getClass().getName().equals(bt2.getClass().getName()) ) return false;
		String sVarName1 = bt1.getName();
		String sVarName2 = bt2.getName();
		if( sVarName1 == null && sVarName2 != null ) return false;
		if( sVarName2 == null && sVarName1 != null ) return false;
		if( sVarName1 != null && !sVarName1.equals(sVarName2) ) return false;
		if(bt1 instanceof DArray) {
			DArray array1 = (DArray)bt1;
			DArray array2 = (DArray)bt2;
			int ctDim1 = array1.numDimensions();
			int ctDim2 = array2.numDimensions();
			if( ctDim1 != ctDim2 ) return false;
			try {
				for( int xDim = 0; xDim < ctDim1; xDim++ ){
					DArrayDimension dim1 = array1.getDimension(xDim);
					DArrayDimension dim2 = array2.getDimension(xDim);
					String sDim1Name = dim1.getName();
					String sDim2Name = dim2.getName();
					if( sDim1Name == null && sDim2Name != null ) return false;
					if( sDim1Name != null && sDim2Name == null ) return false;
					if( sDim1Name != null ) if( !sDim1Name.equals(sDim2Name) ) return false;
					if( dim1.getSize() != dim2.getSize() ) return false;
					if( dim1.getStart() != dim2.getStart() ) return false;
					if( dim1.getStop() != dim2.getStop() ) return false;
					if( dim1.getStride() != dim2.getStride() ) return false;
				}
			} catch(Exception ex) {
				ApplicationController.vShowWarning("Unexpected error comparing variables, bad dimension index: " + ex);
				return false;
			}
			return true;
		}
		else if(bt1 instanceof DStructure) {
			DStructure struct1 = (DStructure)bt1;
			DStructure struct2 = (DStructure)bt2;
			Enumeration vars1 = struct1.getVariables();
			Enumeration vars2 = struct2.getVariables();
			return zCompareVariables( vars1, vars2 );
		}
		else if(bt1 instanceof DSequence) {
			DSequence seq1 = (DSequence)bt1;
			DSequence seq2 = (DSequence)bt2;
			Enumeration enumVar1 = seq1.getVariables();
			Enumeration enumVar2 = seq2.getVariables();
			while( enumVar1.hasMoreElements() ){
				if( !enumVar2.hasMoreElements() ) return false;
				BaseType varbt1 = (BaseType)enumVar1.nextElement();
				BaseType varbt2 = (BaseType)enumVar2.nextElement();
				if( !varbt1.getTypeName().equals(varbt2.getTypeName()) ) return false;
			}
			if( enumVar2.hasMoreElements() ) return false;
			return true;
		}
		else if(bt1 instanceof DGrid) {
			DGrid grid1 = (DGrid)bt1;
			DGrid grid2 = (DGrid)bt2;
			Enumeration vars1 = grid1.getVariables();
			Enumeration vars2 = grid2.getVariables();
			return zCompareVariables( vars1, vars2 );
		}
		else {
			String sTypeName1 = bt1.getTypeName();
			String sTypeName2 = bt2.getTypeName();
			if( sTypeName1 == null ) return sTypeName2 == null;
			if( sTypeName2 == null ) return false;
			return sTypeName1.equals(sTypeName2);
		}
	}

// this does not work because the getParent() method in the Java DAP is bugged
//	static String getQualifiedName( BaseType bt ){
//		if( bt == null ) return "";
//		StringBuffer sbName = new StringBuffer(80);
//		sbName.append(bt.getName());
//		java.util.ArrayList listParents = new java.util.ArrayList();
//		while( true ){
//			BaseType btParent = bt.getParent();
//			if( btParent == null || btParent == bt || listParents.contains(btParent) ) break; // this is to protect against circularity which appears to occur sometimes for some reason (example columbia world ocean atlas)
//			listParents.add( btParent );
//			sbName.insert(0, btParent.getName() + '.');
//		}
//		return sbName.toString();
//	}

	public final static boolean isValidIdentifier( String s, StringBuffer sbError ){
		if( s == null ){ sbError.append( "identifier is null" ); return false; }
		int len = s.length();
		if( len == 0 ){ sbError.append( "identifier is blank" ); return false; }
		char cFirst = s.charAt(0);
		if( Character.isDigit( cFirst ) ){ sbError.append( "identifiers cannot begin with a digit" ); return false; }
		if( ! ( Character.isLetter( cFirst ) || cFirst == '_'  ) ){ sbError.append( "identifier does not begin a letter or underscore" ); return false; }
		for( int xChar = 1; xChar < len; xChar++ ){
			// put any restrictions on identifier characters here, for now I am allowing whitespace and all other characters
			// if( Character.isWhitespace( s.charAt(0) ) ){ sbError.append( "character " + xChar + " in identifier is whitespace" ); return false; }
		}
		return true;
	}
	
}




