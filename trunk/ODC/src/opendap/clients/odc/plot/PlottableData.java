/**
 * Title:        PlottableData
 * Description:  Container for static data to be plottable (see PlottableExpression)
 * Copyright:    Copyright (c) 2002-8
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      3.02
 */

package opendap.clients.odc.plot;

import opendap.clients.odc.DAP;

public class PlottableData implements IPlottable {

	private int miDataType = 0; // 0 = no data, see opendap.clients.odc.DAP for constants
	private short[] mashData = null; // byte and int16
	private int[] maiData = null; // uint16 and int32
	private long[] manData = null; // uint32
	private float[] mafData = null; // float32
	private double[] madData = null; // float64
	private short[] mashData2 = null; // the #2's are for vector plots
	private int[] maiData2 = null;
	private long[] manData2 = null;
	private float[] mafData2 = null;
	private double[] madData2 = null;
	private int mDimension_x = 0;
	private int mDimension_y = 0;
	private int mDimension_z = 0;
	private int mDimension_t = 0;
	private double mExtents_x = 0;
	private double mExtents_y = 0;
	private double mExtents_z = 0;
	private double mExtents_t = 0;

	// Missing Values
	int mctMissing1;
	int mctMissing2;
	short[] mashMissing1;
	int[] maiMissing1;
	long[] manMissing1;
	float[] mafMissing1;
	double[] madMissing1;
	short[] mashMissing2;
	int[] maiMissing2;
	long[] manMissing2;
	float[] mafMissing2;
	double[] madMissing2;
	
	public int getDataType(){ return miDataType; } 
	public short[] getShortArray(){ return mashData; }
	public int[] getIntArray(){ return maiData; }
	public long[] getLongArray(){ return manData; }
	public float[] getFloatArray(){ return mafData; }
	public double[] getDoubleArray(){ return madData; }
	public short[] getShortArray2(){ return mashData2; }
	public int[] getIntArray2(){ return maiData2; }
	public long[] getLongArray2(){ return manData2; }
	public float[] getFloatArray2(){ return mafData2; }
	public double[] getDoubleArray2(){ return madData2; }
	public double getExtents_x(){ return mExtents_x; }
	public double getExtents_y(){ return mExtents_y; }
	public double getExtents_z(){ return mExtents_z; }
	public double getExtents_t(){ return mExtents_t; }
	public int getDimension_x(){ return mDimension_x; }
	public int getDimension_y(){ return mDimension_y; }
	public int getDimension_z(){ return mDimension_z; }
	public int getDimension_t(){ return mDimension_t; }
	
	public int getMissingCount1(){ return mctMissing1; }
	public short[] getMissingShort1(){ return mashMissing1; }
	public int[] getMissingInt1(){ return maiMissing1; }
	public long[] getMissingLong1(){ return manMissing1; }
	public float[] getMissingFloat1(){ return mafMissing1; }
	public double[] getMissingDouble1(){ return madMissing1; }

	public int getMissingCount2(){ return mctMissing2; }
	public short[] getMissingShort2(){ return mashMissing2; }
	public int[] getMissingInt2(){ return maiMissing2; }
	public long[] getMissingLong2(){ return manMissing2; }
	public float[] getMissingFloat2(){ return mafMissing2; }
	public double[] getMissingDouble2(){ return madMissing2; }
	
	public int getDataElementCount(){
		switch( getDataType() ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				if( mashData == null ) return 0; else return mashData.length;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				if( maiData == null ) return 0; else return maiData.length;
			case DAP.DATA_TYPE_UInt32:
				if( manData == null ) return 0; else return mashData.length;
			case DAP.DATA_TYPE_Float32:
				if( mafData == null ) return 0; else return mafData.length;
			case DAP.DATA_TYPE_Float64:
				if( madData == null ) return 0; else return madData.length;
		}
		return 0;
	}

	boolean setPlotData( int eTYPE, Object[] eggData, Object[] eggMissing, Object[] eggData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		if( iWidth <= 0 || iHeight <= 0 ){
			sbError.append("Width " + iWidth + " and Height " + iHeight + " cannot be zero or negative.");
			return false;
		}
		if( eggData2 == null ){
			switch( eTYPE ){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:
					return setTypedPlotData( eTYPE, (short[])eggData[0], eggMissing, null, eggMissing2, iWidth, iHeight, sbError );
				case DAP.DATA_TYPE_UInt16:
				case DAP.DATA_TYPE_Int32:
					return setTypedPlotData( eTYPE, (int[])eggData[0], eggMissing, null, eggMissing2, iWidth, iHeight, sbError );
				case DAP.DATA_TYPE_UInt32:
					return setTypedPlotData( (long[])eggData[0], eggMissing, null, eggMissing2, iWidth, iHeight, sbError );
				case DAP.DATA_TYPE_Float32:
					return setTypedPlotData( (float[])eggData[0], eggMissing, null, eggMissing2, iWidth, iHeight, sbError );
				case DAP.DATA_TYPE_Float64:
					return setTypedPlotData( (double[])eggData[0], eggMissing, null, eggMissing2, iWidth, iHeight, sbError );
				default:
					sbError.append("Data type " + eTYPE + " not supported by pseudocolor plotter");
					return false;
			}
		} else {
			switch( eTYPE ){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:
					return setTypedPlotData( eTYPE, (short[])eggData[0], eggMissing, (short[])eggData2[0], eggMissing2, iWidth, iHeight, sbError );
				case DAP.DATA_TYPE_UInt16:
				case DAP.DATA_TYPE_Int32:
					return setTypedPlotData( eTYPE, (int[])eggData[0], eggMissing, (int[])eggData2[0], eggMissing2, iWidth, iHeight, sbError );
				case DAP.DATA_TYPE_UInt32:
					return setTypedPlotData( (long[])eggData[0], eggMissing, (long[])eggData2[0], eggMissing2, iWidth, iHeight, sbError );
				case DAP.DATA_TYPE_Float32:
					return setTypedPlotData( (float[])eggData[0], eggMissing, (float[])eggData2[0], eggMissing2, iWidth, iHeight, sbError );
				case DAP.DATA_TYPE_Float64:
					return setTypedPlotData( (double[])eggData[0], eggMissing, (double[])eggData2[0], eggMissing2, iWidth, iHeight, sbError );
				default:
					sbError.append("Data type " + eTYPE + " not supported by pseudocolor plotter");
					return false;
			}
		}
	}

	boolean setTypedPlotData( int eTYPE, short[] ashortData, Object[] eggMissing, short[] ashortData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		miDataType = eTYPE;
		mashData = ashortData;
		mashData2 = ashortData2;
		if( eggMissing != null && eggMissing[0] != null ){
			mashMissing1 = (short[])eggMissing[0];
			mctMissing1 = mashMissing1.length - 1;
		}
		if( eggMissing2 != null && eggMissing2[0] != null ){
			mashMissing2 = (short[])eggMissing2[0];
			mctMissing2 = mashMissing2.length - 1;
		}
		return setPlotDimensions( iWidth, iHeight, sbError );
	}
	boolean setTypedPlotData( int eTYPE, int[] aiData, Object[] eggMissing, int[] aiData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		miDataType = eTYPE;
		maiData = aiData;
		maiData2 = aiData2;
		if( eggMissing != null && eggMissing[0] != null ){
			maiMissing1 = (int[])eggMissing[0];
			mctMissing1 = maiMissing1.length - 1;
		}
		if( eggMissing2 != null && eggMissing2[0] != null ){
			maiMissing2 = (int[])eggMissing2[0];
			mctMissing2 = maiMissing2.length - 1;
		}
		return setPlotDimensions( iWidth, iHeight, sbError );
	}
	boolean setTypedPlotData( long[] anData, Object[] eggMissing, long[] anData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		miDataType = DAP.DATA_TYPE_UInt32;
		manData = anData;
		manData2 = anData2;
		if( eggMissing != null && eggMissing[0] != null ){
			manMissing1 = (long[])eggMissing[0];
			mctMissing1 = manMissing1.length - 1;
		}
		if( eggMissing2 != null && eggMissing2[0] != null ){
			manMissing2 = (long[])eggMissing2[0];
			mctMissing2 = manMissing2.length - 1;
		}
		return setPlotDimensions( iWidth, iHeight, sbError );
	}
	boolean setTypedPlotData( float[] afData, Object[] eggMissing, float[] afData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		miDataType = DAP.DATA_TYPE_Float32;
		mafData = afData;
		mafData2 = afData2;
		if( eggMissing != null && eggMissing[0] != null ){
			mafMissing1 = (float[])eggMissing[0];
			mctMissing1 = mafMissing1.length - 1;
		}
		if( eggMissing2 != null && eggMissing2[0] != null ){
			mafMissing2 = (float[])eggMissing2[0];
			mctMissing2 = mafMissing2.length - 1;
		}
		return setPlotDimensions( iWidth, iHeight, sbError );
	}
	boolean setTypedPlotData( double[] adData, Object[] eggMissing, double[] adData2, Object[] eggMissing2, int iWidth, int iHeight, StringBuffer sbError ){
		miDataType = DAP.DATA_TYPE_Float64;
		madData = adData;
		madData2 = adData2;
		if( eggMissing != null && eggMissing[0] != null ){
			madMissing1 = (double[])eggMissing[0];
			mctMissing1 = madMissing1.length - 1;
		}
		if( eggMissing2 != null && eggMissing2[0] != null ){
			madMissing2 = (double[])eggMissing2[0];
			mctMissing2 = madMissing2.length - 1;
		}
		return setPlotDimensions( iWidth, iHeight, sbError );
	}
	
	boolean setPlotDimensions( int iWidth, int iHeight, StringBuffer sbError ){
		mDimension_x = iWidth;
		mDimension_y = iHeight;
		return true;
	}

	
}
