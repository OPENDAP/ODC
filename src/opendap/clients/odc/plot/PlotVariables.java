package opendap.clients.odc.plot;

/**
 * Title:        VariableSelector_Plot_Panel
 * Description:  Select variables for plotting, used by Panel_View_Plot
 * Copyright:    Copyright (c) 2003-4
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.49
 */

import opendap.clients.odc.*;

import java.util.*;
import java.net.*;
import java.io.*;
import opendap.dap.*;

/** The plot types have the following requirements:

pseudocolor/contour:
	value matrix 2D
	optional x-axis 1D (must match value width)
	optional y-axis 1D (must match value height)

vector:
	value matrix U: 2D (these two must match each other)
	value matrix V: 2D
	optional x-axis 1D (must match value width)
	optional y-axis 1D (must match value height)

histogram:
	value matrix (any dimensions)

scatter:
	value matrix 1D
	optional x- or y-axis 1D (must match value matrix length)

line:
	one or more value matrix 1D
	optional x- or y-axis 1D (must match value matrix(s) length)
*/

/** represents a valid set of variables for a given plot type
 *  in other words given a DataDDS and a plot type what are the possible variables that
 *  can be arranged to generate the plot and how should they be labeled
 */
public class PlotVariables {

	private DataDDS mddds;
	private DAS mdas;
	private String[] masCaptions1;
	private String[] masLabels1;
	private String[] masVariableName1;
	private VariableSpecification[] maVariables1;

	DataDDS getDDDS(){ return mddds; }
	DAS getDAS(){ return mdas; }
	String[] getLabels(){ return masLabels1; }
	VariableSpecification[] getVariableSpecifications(){ return maVariables1; }

	VariableSpecification getVariable1( int index ){
		if( maVariables1 == null ){
			ApplicationController.vShowWarning("internal error, variable array does not exist");
			return null;
		}
		if( index < 1 || index >= maVariables1.length ){
			ApplicationController.vShowWarning("internal error, invalid variable index " + index);
			return null;
		}
		return maVariables1[index];
	}

	int getVariableCount(){
		if( maVariables1 == null ) return 0;
		if( maVariables1.length == 1 ) return 0;
		return maVariables1.length - 1;
	}

	boolean zLoadVariables( DataDDS ddds, DAS das, int size, StringBuffer sbError ){
		return zLoadVariables( ddds, das, 0, size, sbError ); // plot type is ignored if size > 0 todo make more robust
	}

	boolean zLoadVariables( DataDDS ddds, DAS das, int ePLOT_TYPE, int size, StringBuffer sbError ){
		try {
			if( ddds == null ){ sbError.append("data DDS missing"); return false; }
			ArrayList listVariableSpecs = new ArrayList();
			Enumeration enumVariables = ddds.getVariables();
			if( !zDiscoverVariables_recursion( ePLOT_TYPE, size, null, enumVariables, das, listVariableSpecs, sbError ) ){
				sbError.insert(0, "error traversing variable tree: " );
				return false;
			}
			int ctVariables = listVariableSpecs.size();
			String[] asLabels1_buffer = new String[ctVariables + 1];
			String[] asVariableName1_buffer = new String[ctVariables + 1];
			VariableSpecification[] aVariables1_buffer = new VariableSpecification[ctVariables + 1];
			for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
				VariableSpecification vs = ((VariableSpecification)listVariableSpecs.get(xVariable-1));
				int ctNestingDepth = vs.maiPath1[0];
				String sVariableName = vs.masPath1[ctNestingDepth];
				asLabels1_buffer[xVariable] = sVariableName;
				asVariableName1_buffer[xVariable] = sVariableName;
				aVariables1_buffer[xVariable] = vs;
			}

			// make labels - use prefixes only if var name is ambiguous
			// here the label may be further modified if it needs to be qualified further
			for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
				String sVariableName = asLabels1_buffer[xVariable];
				if( sVariableName.startsWith("!") ) continue; // already processed this variable
				boolean zDuplicate = false;
				int ctInstances = 1;
				for( int xVariableRest = xVariable + 1; xVariableRest <= ctVariables; xVariableRest++ ){
					String sNextVariableName = asLabels1_buffer[xVariableRest];
					if( sNextVariableName.equals(sVariableName) ){
						ctInstances++;
						zDuplicate = true;
						asLabels1_buffer[xVariableRest] = '!' + aVariables1_buffer[xVariableRest].getFullyQualifiedName();
					}
				}
			}
			for( int xVariable = 1; xVariable <= ctVariables; xVariable++ ){
				if( asLabels1_buffer[xVariable].startsWith("!") ) asLabels1_buffer[xVariable] = asLabels1_buffer[xVariable].substring(1);
			}

			// complete data store
			mddds = ddds;
			mdas  = das;
			masLabels1 = asLabels1_buffer;
			masVariableName1 = asVariableName1_buffer;
			maVariables1 = aVariables1_buffer;

			return true;
		} catch( Exception ex ) {
			Utility.vUnexpectedError( ex, sbError );
			return false;
		}
	}

	// Variable Size
	// the size variable is used to restrict the discovery to variables that are
	// one-dimensional vectors of a particular length; this is used when getting
	// variables for an axis; if size is 0 there are no restrictions

	// Sequences
	// sequences are stored in a VariableSpec by specifying the BaseType of the root sequence
	// and then the path to the non-sequence variable; the node name is stored for the root sequence
	private boolean zDiscoverVariables_recursion( int ePLOT_TYPE, int size, String sNodeName, Enumeration enumVariables, DAS das, ArrayList listVariableSpecs, StringBuffer sbError ){
		try {
		    Enumeration enumChildVariables = null;
			while( enumVariables.hasMoreElements() ){
				BaseType bt = (BaseType)enumVariables.nextElement();
				String sVariableName = bt.getName();
				String sVariableCaption = DAP.getAttributeValue( das, sVariableName, "long_name", sbError );
				String sVariableUnits = DAP.getAttributeValue( das, sVariableName, "units", sbError );
				if( sVariableName == null ) continue; // ignore variables without a name
				if( bt instanceof DStructure ){
					enumChildVariables = ((DConstructor)bt).getVariables();
					sNodeName = ( sNodeName == null ) ? sVariableName : sNodeName + '.' + sVariableName;
	    			zDiscoverVariables_recursion( ePLOT_TYPE, size, sNodeName, enumChildVariables, das, listVariableSpecs, sbError );
				} else if( bt instanceof DSequence) {
					DSequence dsequence = (DSequence)bt;
 					if( !zDiscoverVariables_sequence( dsequence, sNodeName, das, listVariableSpecs, sbError) ){
						 String sSequenceName = sNodeName == null ? sVariableName : sNodeName + '.' + sVariableName;
						sbError.insert(0, "error analyzing sequence variable " + sSequenceName + ": ");
						return false;
					}
				} else if( bt instanceof DGrid ) {
					final DGrid dgrid = (DGrid)bt;
					enumChildVariables = dgrid.getVariables();
					sNodeName = ( sNodeName == null ) ? sVariableName : sNodeName + '.' + sVariableName;
				    zDiscoverVariables_recursion( ePLOT_TYPE, size, sNodeName, enumChildVariables, das, listVariableSpecs, sbError );
				} else if( bt instanceof DArray ) { // can plot maybe
					DArray darray = (DArray)bt;
					int iEffectiveDimCount = iDetermineEffectiveDimCount( darray );
					if( iEffectiveDimCount == 0 ) continue; // ignore
					if( size > 0 ){
						if( iEffectiveDimCount != 1 ) continue; // not a valid map array
						if( darray.getFirstDimension().getSize() != size ) continue; // not a matching map array
					} else {
						switch(ePLOT_TYPE){
							case Output_ToPlot.PLOT_TYPE_Histogram: // any
							case Output_ToPlot.PLOT_TYPE_XY: // 1- or 2-D
							case Output_ToPlot.PLOT_TYPE_Vector: // 1- or 2-D
								break;
							case Output_ToPlot.PLOT_TYPE_Pseudocolor: // 1 2-D
								if( iEffectiveDimCount >= 2 ) break; else continue;
						}
					}
					VariableSpecification vsNewArray = new VariableSpecification();
					if( !vsNewArray.zInitialize(bt, sVariableName, sVariableCaption, sVariableUnits, sbError) ){
						sbError.insert(0, "error creating variable specification: ");
						return false;
					}
					vsNewArray.sBaseNode = sNodeName;
					vsNewArray.maiPath1[0] = 1; // there is one level in the path
					vsNewArray.maiPath1[1] = 0; // the first level in the path is the root and has no field number
					vsNewArray.masPath1[1] = sVariableName;
					listVariableSpecs.add( vsNewArray );
				} else { // other types are not plotable
					continue;
				}
			}
			return true;
		} catch( Exception ex ) {
			Utility.vUnexpectedError( ex, sbError );
			return false;
		}
	}

	private boolean zDiscoverVariables_sequence( DSequence sequence, String sNodeName, DAS das, ArrayList listVariableSpecs, StringBuffer sbError ){
		try {
			String sVariableName = sequence.getName();
			String sVariableCaption = DAP.getAttributeValue(das, sVariableName,
				"long_name", sbError);
			String sVariableUnits = DAP.getAttributeValue(das, sVariableName,
				"units", sbError);
			VariableSpecification vsNewSequence = new VariableSpecification();
			if (!vsNewSequence.zInitialize(sequence, sVariableName,
										   sVariableCaption, sVariableUnits,
										   sbError)) {
				sbError.insert(0, "error creating root variable specification: ");
				return false;
			}
			vsNewSequence.sBaseNode = sNodeName;
			vsNewSequence.maiPath1[0] = 1; // path level count
			vsNewSequence.maiPath1[1] = 0; // the root has no field number
			vsNewSequence.masPath1[1] = sVariableName;
			return zDiscoverVariables_sequence_recursion(vsNewSequence, das, listVariableSpecs, sbError);
		} catch(Exception ex) {
			Utility.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	// the idea here is that a sequence (or table) has field names and rows. A field can contain a terminal type such as a scalar
	// or array or another sequence; we wish to generate a variable specification for all possible terminals
	// each variable specification contains the root sequence plus a path to the terminal variable:
/*
		root_sequence
			field 1: scalar A 1
		    field 2: sequence_R.2
				field 5: sequence_R.2.5
				    field 3: sequence_R.2.5.3
						f1: scalar B 2.5.3.1
						f2: scalar C 2.5.3.2
						f3: scalar D 2.5.3.3

	Here four variable specifications must be made, one for each scalar (A, B, C, D). Each will have root_sequence as its root and a path
	as shown (eg 2.5.3.2) which is stored in the variable specification.
*/
	private boolean zDiscoverVariables_sequence_recursion( VariableSpecification vsParentSequence, DAS das, ArrayList listVariableSpecs, StringBuffer sbError ){

		// find the sequence pointed to by the path in the variable specification
	    DSequence root_sequence = (DSequence)vsParentSequence.getBaseType();
		int ctPathLevels = vsParentSequence.maiPath1[0];
		int xPath = 1;
		BaseType btPathCursor = root_sequence;
		while( true ){
			if( !(btPathCursor instanceof DSequence) ){
				sbError.append("invalid sequence path: " + vsParentSequence.toString_path());
				return false;
			}
			xPath++;
			if( xPath > ctPathLevels ) break;
			int iFieldNumber = vsParentSequence.maiPath1[xPath];
			try {
				btPathCursor = ((DSequence)btPathCursor).getVar(iFieldNumber-1); // this is a zero-based call
			} catch(Exception ex) {
				sbError.append("path cursor not found");
				return false;
			}
		}
		DSequence sequenceNested = (DSequence)btPathCursor;

		// go through the fields in the pointed-to sequence and add each one that is a terminal type as a new variable
		// if the field is a sequence then call this method recursively to repeat the procedure
		int ctFields = sequenceNested.elementCount();
		for( int xField = 1; xField <= ctFields; xField++ ){
			BaseType btField = null;
		    try {
			    btField = sequenceNested.getVar(xField-1); // zero-based call
			} catch(Exception ex) {
				sbError.append("field not found");
				return false;
			}
			if( btField == null ){
				sbError.append("sequence '" + sequenceNested.getName() + "' field " + xField + " unexpectedly null");
				return false;
			}
			String sFieldName = btField.getName();
			VariableSpecification vsNewLevel = vsParentSequence.clone_();
			int ctNestingLevel = vsNewLevel.maiPath1[0] + 1;
			vsNewLevel.maiPath1[0] = ctNestingLevel;
			vsNewLevel.maiPath1[ctNestingLevel] = xField;
			vsNewLevel.masPath1[ctNestingLevel] = sFieldName;
			if( btField instanceof DSequence ){
				if( !zDiscoverVariables_sequence_recursion( vsNewLevel, das, listVariableSpecs, sbError ) ){
					return false;
				}
			} else if( btField instanceof DArray || btField instanceof DGrid || btField instanceof DStructure ){
				ApplicationController.vShowWarning("nested sequence array/grid ignored (sequence '" + sequenceNested.getName() + "' field " + xField + ")");
				// not supported (todo)
			} else if( btField instanceof DBoolean ){
				ApplicationController.vShowWarning("nested sequence boolean ignored (sequence '" + sequenceNested.getName() + "' field " + xField + ")");
				// not supported (todo)
			} else if( btField instanceof DByte ||
					   btField instanceof DInt16 ||
					   btField instanceof DUInt16 ||
					   btField instanceof DInt32 ||
					   btField instanceof DUInt32 ||
					   btField instanceof DFloat32 ||
					   btField instanceof DFloat64 ||
					   btField instanceof DString   // will try to convert to number
					  ){
				int ctDims = vsNewLevel.getDimCount();
				if( sequenceNested.getRowCount() < 2 ) continue; // ignore sequences with only one row (or less)

				// update vs info for this scalar
				String sVariableName = btField.getName();
				String sLongName = null;
			    String sUnits = null;
				StringBuffer sbDASerror = new StringBuffer();
				if( das != null ){
					String sLongName1 = DAP.getAttributeValue(das, sVariableName, "long_name", sbDASerror);
					String sLongName2 = DAP.getAttributeValue(das, sVariableName, "longname", sbDASerror);
					sLongName = sLongName1 != null ? sLongName1 : sLongName2 != null ? sLongName2 : null;
					sUnits = DAP.getAttributeValue(das, sVariableName, "units", sbDASerror);
				}
				vsNewLevel.setName(sVariableName);
				vsNewLevel.setAttribute_LongName(sLongName);
				vsNewLevel.setAttribute_Units(sUnits);
				listVariableSpecs.add( vsNewLevel );
			} else {
				ApplicationController.vShowWarning("unknown nested sequence type ignored");
			}
		}
		return true;
	}

	// without getting data vectors this is the best that can be done
	// the error must be clear going in here
	private boolean zSequenceHasOnlyOneRow( VariableSpecification vs, StringBuffer sbError ){
		ArrayTable at = vs.getArrayTable(sbError);
		if( at == null ){
			sbError.insert(0, "internal error, unable to get array table for vs: ");
			return false;
		}
		if( vs.maiPath1[0] == 2 && ((DSequence)at.bt).getRowCount() < 2 ) return true;
		sbError.append("internal error, attempt to get get only one row for multi-row path");
		return false;
	}

	int iDetermineEffectiveDimCount( DArray darray ){
		try {
			int iEffectiveDimCount = 0;
			for( int xDim = 0; xDim < darray.numDimensions(); xDim++ ){
				DArrayDimension theDim = darray.getDimension(xDim);
				if( theDim.getSize() > 1 ) iEffectiveDimCount++;
			}
			return iEffectiveDimCount;
		} catch(Exception ex) {
			return 0; // todo consider handling this error
		}
	}
}

class VariableDataset {

	private ArrayList listValues = new ArrayList();
	private ArrayList listValues2 = new ArrayList();
	private VariableInfo mvarAxisX = null;
	private VariableInfo mvarAxisY = null;
	public VariableDataset(){}
	void addValue( VariableInfo var ){
		listValues.add( var );
	}
	void addValue2( VariableInfo var ){
		listValues2.add( var );
	}
	int getValueCount(){ return listValues.size() > 0 ? listValues.size() : listValues2.size(); }
	VariableInfo getValue( int index1 ){ if( index1 > listValues.size() ) return null; else return (VariableInfo)listValues.get(index1 - 1); }
	VariableInfo getValue2( int index1 ){ if( index1 > listValues2.size() ) return null; else return (VariableInfo)listValues2.get(index1 - 1); }
	VariableInfo getAxisX(){ return mvarAxisX; }
	VariableInfo getAxisY(){ return mvarAxisY; }

	private boolean mzAxisX_Indexed = false;
	private boolean mzAxisY_Indexed = false;
	void setAxisX_Indexed(){
		mzAxisX_Indexed = true;
		mvarAxisX = null;
	}
	void setAxisY_Indexed(){
		mzAxisY_Indexed = true;
		mvarAxisY = null;
	}
	boolean getAxisX_Indexed(){ return mzAxisX_Indexed; }
	boolean getAxisY_Indexed(){ return mzAxisY_Indexed; }
	void setAxisX( VariableInfo var ){
		mzAxisX_Indexed = false;
		mvarAxisX = var;
	}
	void setAxisY( VariableInfo var ){
		mzAxisY_Indexed = false;
		mvarAxisY = var;
	}

	private int meDataType_Var1;
	private int meDataType_Var2;
	private int mctDimensions;
	private int mLenX = 0;
	private int mLenY = 0;
	private int mLenZ = 0;
	private int meInversion = DAP.INVERSION_XYZ;
	int getDataType_Var1(){ return meDataType_Var1; }
	int getDataType_Var2(){ return meDataType_Var2; }
	int getInversion(){ return meInversion; }
	private boolean mzReversed_x = false;
	private boolean mzReversed_y = false;
	private boolean mzReversed_z = false;
	private String msID = null;
	String getID(){ return msID; }
	void setID( String s ){ msID = s; }
	private String msID_x;
	private String msID_y;
	private String msID_z;
	void setParameters( int eDataType_Var1, int eDataType_Var2, int ctDimensions, int lenX, int lenY, int eInversion, boolean zReversed_x, boolean zReversed_y, String sID_x, String sID_y){
		meDataType_Var1 = eDataType_Var1;
		meDataType_Var2 = eDataType_Var2;
		mctDimensions = ctDimensions;
	    mLenX = lenX;
	    mLenY = lenY;
		meInversion = eInversion;
		mzReversed_x = zReversed_x;
		mzReversed_y = zReversed_y;
		msID_x = sID_x;
		msID_y = sID_y;
	}
	int getDimCount(){ return mctDimensions; }
	int getLength_x(){ return mLenX; }
	int getLength_y(){ return mLenY; }
	boolean getReversed_x(){ return mzReversed_x; }
	boolean getReversed_y(){ return mzReversed_y; }
	String getID_x(){ return msID_x; }
	String getID_y(){ return msID_y; }

	private Object[] meggMissing_var1 = null;
	void setMissingEgg_Var1(Object[] egg){ meggMissing_var1 = egg; }
	Object[] getMissingEgg_Var1(){ return meggMissing_var1; }

	private Object[] meggMissing_var2 = null;
	void setMissingEgg_Var2(Object[] egg){ meggMissing_var2 = egg; }
	Object[] getMissingEgg_Var2(){ return meggMissing_var2; }

// todo fix this up
//	public String toString(){
//		StringBuffer sb = new StringBuffer(100);
//		sb.append("Variable Info for \"" + msVariableName + "\" units: " + msUnits + " type: ");
//		sb.append(" len D1: " + mLenD1 + " len D2: " + mLenD2 + " len D3: " + mLenD3 + " values size: " + madValues0.length);
//		return sb.toString();
//	}

}

class VariableInfo {
	private BaseType mbt = null;
	private int meDataType = 0;
	private int meDimType = 0;
	private short[] mashValues0 = null;
	private int[] maiValues0 = null;
	private long[] manValues0 = null;
	private float[] mafValues0 = null;
	private double[] madValues0 = null;
	private String[] masValues0 = null;
	private boolean mzIsReversed = false;
	private int[] maiDimLengths = new int[4];
	private String msVariableName = null;
	private String msVariableLongName = null;
	private String msVariableUserCaption = null;
	private String msUnits = null;
	private String msSliceCaption = null;
	private boolean mzUseIndex = false;

	VariableInfo(){}

	VariableInfo( BaseType bt, String sName, String sLongName, String sUserCaption, String sSliceCaption, String sUnits ){
		mbt = bt;
		msVariableName = sName;
	    msVariableLongName = sLongName;
	    msVariableUserCaption = sUserCaption;
		msSliceCaption = sSliceCaption;
	    msUnits = sUnits;
	}

	String getName(){ return msVariableName; }
	String getLongName(){ return msVariableLongName; }
	String getUserCaption(){ return msVariableUserCaption; }
	String getUnits(){ return msUnits; }
	String getSliceCaption(){ return msSliceCaption; }
	boolean getUseIndex(){ return mzUseIndex; }
	int getDataType(){ return meDataType; }
	Object[] getValueEgg(){
		Object[] egg = new Object[1];
		switch( meDataType ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				egg[0] = mashValues0; break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				egg[0] = maiValues0; break;
			case DAP.DATA_TYPE_UInt32:
				egg[0] = manValues0; break;
			case DAP.DATA_TYPE_Float32:
				egg[0] = mafValues0; break;
			case DAP.DATA_TYPE_Float64:
				egg[0] = madValues0; break;
			case DAP.DATA_TYPE_String:
				egg[0] = masValues0; break;
			default:
				return null;
		}
		return egg;
	}
	short[] getValues0_short(){ return mashValues0; }
	int[] getValues0_int(){ return maiValues0; }
	long[] getValues0_long(){ return manValues0; }
	float[] getValues0_float(){ return mafValues0; }
	double[] getValues0_double(){ return madValues0; }
	String[] getValues0_string(){ return masValues0; }
	String getValueSample(){
		StringBuffer sb = new StringBuffer();
		switch( meDataType ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				for( int x=0; x<5; x++ ) sb.append(" " + mashValues0[x]);
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				for( int x=0; x<5; x++ ) sb.append(" " + maiValues0[x]);
				break;
			case DAP.DATA_TYPE_UInt32:
				for( int x=0; x<5; x++ ) sb.append(" " + manValues0[x]);
				break;
			case DAP.DATA_TYPE_Float32:
				for( int x=0; x<5; x++ ) sb.append(" " + mashValues0[x]);
				break;
			case DAP.DATA_TYPE_Float64:
				for( int x=0; x<5; x++ ) sb.append(" " + madValues0[x]);
				break;
			case DAP.DATA_TYPE_String:
				for( int x=0; x<5; x++ ) sb.append(" " + masValues0[x]);
				break;
		}
		return sb.toString();
	}
	void setBaseType( BaseType bt ){ mbt = bt; }
	void setName( String s ){ msVariableName = s; }
	void setLongName( String s ){ msVariableLongName = s; }
	void setUserCaption( String s ){ msVariableUserCaption = s; }
	void setSliceCaption( String s ){ msSliceCaption = s; }
	void setUnits( String s ){ msUnits = s; }
	int mctDimensions = 0;
	int mlenDimX, mlenDimY;
	int getDimensionCount(){ return mctDimensions; }
	int[] getDimLengths1(){ return maiDimLengths; }
	int getDimLength( int index1 ){ return maiDimLengths[index1]; }
	void setValues( short[] ashData0, int eType, int ctDimensions, int lenD1, int lenD2 ){
		mashValues0 = ashData0;
		if( eType == DAP.DATA_TYPE_Int16 ) meDataType = DAP.DATA_TYPE_Int16;
		else meDataType = DAP.DATA_TYPE_Byte;
		setDims( ctDimensions, lenD1, lenD2 );
	}
	void setValues( int[] aiData0, int eType, int ctDimensions, int lenD1, int lenD2 ){
		maiValues0 = aiData0;
		meDataType = (eType == DAP.DATA_TYPE_Int32) ? DAP.DATA_TYPE_Int32 : DAP.DATA_TYPE_UInt16;
		setDims( ctDimensions, lenD1, lenD2 );
	}
	void setValues( long[] anData0, int ctDimensions, int lenD1, int lenD2 ){
		manValues0 = anData0;
		meDataType = DAP.DATA_TYPE_UInt32;
		setDims( ctDimensions, lenD1, lenD2 );
	}
	void setValues( float[] afData0, int ctDimensions, int lenD1, int lenD2 ){
		mafValues0 = afData0;
		meDataType = DAP.DATA_TYPE_Float32;
		setDims( ctDimensions, lenD1, lenD2 );
	}
	void setValues( double[] adData0, int ctDimensions, int lenD1, int lenD2 ){
	    madValues0 = adData0;
		meDataType = DAP.DATA_TYPE_Float64;
		setDims( ctDimensions, lenD1, lenD2 );
	}
	void setUseIndex(){ mzUseIndex = true; }
	void setValues( String[] asData0, int ctDimensions, int lenD1, int lenD2 ){
		masValues0 = asData0;
		meDataType = DAP.DATA_TYPE_String;
		setDims( ctDimensions, lenD1, lenD2 );
	}
	private void setDims( int ctDimensions, int lenD1, int lenD2 ){
		mctDimensions = ctDimensions;
		maiDimLengths[0] = ctDimensions;
		maiDimLengths[1] = lenD1 <= 0 ? 1 : lenD1;
		maiDimLengths[2] = lenD2 <= 0 ? 1 : lenD2;
		mzUseIndex = false;
	}
	public String toString(){
		StringBuffer sb = new StringBuffer(1000);
		sb.append("name: " + msVariableName + "\n");
		sb.append("long name: " + msVariableLongName + "\n");
		sb.append("user caption: " + msVariableUserCaption + "\n");
		sb.append("units: " + msUnits + "\n");
		sb.append("slice caption: " + msSliceCaption + "\n");
		return sb.toString();
	}
}

// the path is used in the case of a sequence
// for a sequence only the top-most basetype is recorded and desired vector is specified
// for example if the path is (2,3,4) then it means the 4th field, of the 3rd field of the
// 2nd field of the sequence; currently only nested sequences/scalars are supported so
// all the fields in the path will point to sequences except the last which will always
// point to a scalar
// the minimum sequence has 2 levels, one for the root and one for the chosen scalar
class VariableSpecification {
	private ArrayTable theArrayTable;
	private String sArrayTableError = null;
	private BaseType mCoreBaseType;
	private String msName = null;
	private String msAttribute_LongName = null;
	private String msAttribute_Units = null;
	String sBaseNode;
	int[] maiPath1 = new int[25]; // one based (24 possible levels) [0] = path count
	String[] masPath1 = new String[25]; // one based (24 possible levels)

	public VariableSpecification(){}

	boolean zInitialize( BaseType bt, String sVariableName, String sLongName, String sUnits, StringBuffer sbError ){
		if( bt == null ){
			sbError.append("base type is missing");
			return false;
		}
		if( sVariableName == null ){
			sbError.append("sVariableName is missing");
			return false;
		}
		mCoreBaseType = bt;
		msName = sVariableName;
		msAttribute_LongName = sLongName;
		msAttribute_Units = sUnits;
		return true;
	}

	BaseType getBaseType(){ return mCoreBaseType; }

	VariableSpecification clone_(){
		String sName = new String(msName);
		String sLongName = msAttribute_LongName == null ? null : new String(msAttribute_LongName);
		String sUnits = msAttribute_Units == null ? null : new String(msAttribute_Units);
		VariableSpecification vs = new VariableSpecification();
		StringBuffer sbError = new StringBuffer();
		if( !vs.zInitialize(mCoreBaseType, sName, sLongName, sUnits, sbError) ){
			return null;
		}
		vs.sBaseNode = sBaseNode == null ? null : new String(sBaseNode);
		if( maiPath1 != null ){
			vs.maiPath1 = new int[ maiPath1.length ];
			System.arraycopy(maiPath1, 0, vs.maiPath1, 0, maiPath1.length);
		}
		if( masPath1 != null ){
			vs.masPath1 = new String[ masPath1.length ];
			System.arraycopy(masPath1, 0, vs.masPath1, 0, masPath1.length);
		}
		return vs;
	}

	String getName(){
		return msName;
	}

	String getAttribute_LongName(){
		return msAttribute_LongName;
	}

	String getAttribute_Units(){
		return msAttribute_Units;
	}

	void setName( String sName ){
		msName = sName;
	}

	void setAttribute_LongName( String s ){
		msAttribute_LongName = s;
	}

	void setAttribute_Units( String s ){
		msAttribute_Units = s;
	}

	String getFullyQualifiedName(){
		StringBuffer sbName = new StringBuffer(80);
		if( sBaseNode != null ) sbName.append(sBaseNode).append(".");
		int ctNestingLevel = maiPath1[0];
		for( int xPath = 1; xPath <= ctNestingLevel; xPath++ ){
		    if( xPath > 1 ) sbName.append(".");
			sbName.append(masPath1[xPath]);
		}
		return sbName.toString();
	}

	int getDimCount(){
		if( theArrayTable != null ){
			return ((DArray)theArrayTable.bt).numDimensions();
		}
		return is2DSequence() ? 2 : 1;
	}

	// assume it is a sequence in which case it will be two dimensions if it has any
	// multirow scalars in the non-leaf levels
	private boolean is2DSequence(){
		if( !(mCoreBaseType instanceof DSequence) ) return false;
		DSequence seq = (DSequence)mCoreBaseType;
		int ctLevels = maiPath1[0];
		if( ctLevels < 1 ) return false;
		int ctFields = 0;
		DSequence seqCursor = seq;
		for( int xLevel = 2; xLevel < ctLevels; xLevel++ ){
			int ctLevelRows = seqCursor.getRowCount();
			if( ctLevelRows > 1 ){ // add fields
				int ctLevelFields = seqCursor.elementCount();
				for( int xLevelField = 1; xLevelField <= ctLevelFields; xLevelField++ ){
					BaseType btField = null;
					try {
						btField = seqCursor.getVar(xLevelField-1); // zero-based call
					} catch(Exception ex) {
						return false;
					}
					if( btField == null ){
						return false;
					}
					String sFieldName = btField.getName();
					if( btField instanceof DSequence ){
						// ignore
					} else if( btField instanceof DArray || btField instanceof DGrid || btField instanceof DStructure ){
						// ignore
					} else if( btField instanceof DBoolean ||
							   btField instanceof DByte ||
							   btField instanceof DInt16 ||
							   btField instanceof DUInt16 ||
							   btField instanceof DInt32 ||
							   btField instanceof DUInt32 ||
							   btField instanceof DFloat32 ||
							   btField instanceof DFloat64 ||
							   btField instanceof DString   // will try to convert to number
							  ){
						return true;
					} else {
						// ignore
					}
				}
			}

			// move to next level
			int iFieldNumber = maiPath1[xLevel];
			BaseType btPathField;
			try {
				btPathField = seqCursor.getVar(iFieldNumber-1);
			} catch(Exception ex) {
				return false;
			}
			if( ! (btPathField instanceof DSequence) ){
				return false;
			}
			seqCursor = (DSequence)btPathField;
		}
		return false;
	}

	// flattens a sequence into a DArray and adds field names if the variable is a sequence
	ArrayTable getArrayTable(StringBuffer sbError){
		if( theArrayTable != null ) return theArrayTable;
		if( mCoreBaseType == null ){
			sbError.append("internal error, core base type is null");
		}
		if( mCoreBaseType instanceof DArray || mCoreBaseType instanceof DGrid ){
			ArrayTable at = new ArrayTable(mCoreBaseType, null);
			theArrayTable = at;
			return theArrayTable;
		}
FlattenSequence:
		{
	    try {
			if( mCoreBaseType instanceof DSequence ){
				DSequence seq = (DSequence)mCoreBaseType;
				int ctLevels = maiPath1[0];
				if( ctLevels < 1 ){
					sArrayTableError = "system error, sequence has no levels";
					break FlattenSequence;
				}

				//    --- accumulate field names
				int ctFields = 0;
				String[] asFieldNames = new String[120];
				DSequence seqCursor = seq;
				for( int xLevel = 1; xLevel < ctLevels-1; xLevel++ ){

					// add multirow scalars in the upstream path as additional fields
					int ctLevelRows = seqCursor.getRowCount();
					if( ctLevelRows > 1 ){ // add fields
						int ctLevelFields = seqCursor.elementCount();
						for( int xLevelField = 1; xLevelField <= ctLevelFields; xLevelField++ ){
							BaseType btField = null;
							try {
								btField = seqCursor.getVar(xLevelField-1); // zero-based call
							} catch(Exception ex) {
								sArrayTableError = "sequence '" + seqCursor.getName() + "' field " + xLevelField + " field not found";
								break FlattenSequence;
							}
							if( btField == null ){
								sArrayTableError = "sequence '" + seqCursor.getName() + "' field " + xLevelField + " unexpectedly null";
								break FlattenSequence;
							}
							String sFieldName = btField.getName();
							if( btField instanceof DSequence ){
								// ignore
							} else if( btField instanceof DArray || btField instanceof DGrid || btField instanceof DStructure ){
								// ignore
							} else if( btField instanceof DBoolean ||
									   btField instanceof DByte ||
									   btField instanceof DInt16 ||
									   btField instanceof DUInt16 ||
									   btField instanceof DInt32 ||
									   btField instanceof DUInt32 ||
									   btField instanceof DFloat32 ||
									   btField instanceof DFloat64 ||
									   btField instanceof DString   // will try to convert to number
									  ){
								ctFields++;
								asFieldNames[ctFields] = sFieldName;
							} else {
								// ignore
							}
						}
					}

					// move to next level
					int iFieldNumber = maiPath1[xLevel + 1];
					BaseType btPathField;
					try {
						btPathField = seqCursor.getVar(iFieldNumber-1);
					} catch(Exception ex) {
						sArrayTableError = "system error, invalid path field " + iFieldNumber;
						break FlattenSequence;
					}
					if( ! (btPathField instanceof DSequence) ){
						sArrayTableError = "system error, path field " + iFieldNumber + " in level " + xLevel + " of " + ctLevels + " levels is not a sequence";
						break FlattenSequence;
					}
					seqCursor = (DSequence)btPathField;
				}

				// add the scalar field
				ctFields++;
				int iScalarFieldNumber = maiPath1[ctLevels];
				BaseType btScalarField;
				try {
					btScalarField = seqCursor.getVar(iScalarFieldNumber-1);
				} catch(Exception ex) {
					sArrayTableError = "internal error, invalid path number " + iScalarFieldNumber + " for scalar field (" + this.toString_path() + ")";
					break FlattenSequence;
				}
				asFieldNames[ctFields] = btScalarField.getName();

				//    --- accumulate number of normalized rows and fields (cross-product of all rows)
				int ctNormalizedRows = recursion_CountNormalizedRows( seq, 1 );
				if( ctNormalizedRows < 2 ){
					sArrayTableError = "sequence has less than 2 normalized rows";
					break FlattenSequence;
				}

				//   --- size array
				double[][] adLoadingArray;
				try {
					adLoadingArray = new double[ctFields + 1][ctNormalizedRows + 1];
				} catch(Throwable t) {
					sArrayTableError = "error allocating memory for sequence data of " + ctFields + " fields and " + ctNormalizedRows + " normalized rows: " + t;
					break FlattenSequence;
				}

				//   --- convert data into loading array (uses logic of above)
				mctProcessedRows = 0; // these two variables are used only for progress reporting
				mctTotalRows = ctNormalizedRows;
				recursion_AddNormalizedData( seq, 1, adLoadingArray, ctFields, 1, 1 );

				//   --- size and load vector array
				double[] adVector;
				int iVectorSize = ctFields * ctNormalizedRows;
				try {
					adVector = new double[iVectorSize];
				} catch(Throwable t) {
					sArrayTableError = "error allocating memory for sequence data of " + ctFields + " fields and " + ctNormalizedRows + " normalized rows: " + t;
					break FlattenSequence;
				}
				for( int xField = 1; xField <= ctFields; xField++ ){
					for( int xRow = 1; xRow <= ctNormalizedRows; xRow++ ){
						adVector[(xField-1)*ctNormalizedRows + (xRow-1)] = adLoadingArray[xField][xRow];
					}
				}

				//   --- create DArray
				String sName = masPath1[maiPath1[0]];
				DArray darray = new DArray(sName);
				darray.appendDim(ctNormalizedRows,"rows");
				if( ctFields > 1 ){
					darray.appendDim(ctFields,"fields");
				}
				DFloat64 float64TypeTemplate = new DFloat64(sName);
				darray.addVariable(float64TypeTemplate);
				PrimitiveVector pv = darray.getPrimitiveVector();
				pv.setInternalStorage(adVector);

				//   --- create and return the ArrayTable
				String[] asFieldNames_sized = new String[ctFields + 1];
				System.arraycopy(asFieldNames, 1, asFieldNames_sized, 1, ctFields);
				ArrayTable at = new ArrayTable(darray, asFieldNames_sized);
				return at;

			} else {
				sArrayTableError = "non-supported base type for variable specification: " + DAP.getType_String(mCoreBaseType);
			}
		} catch(Exception ex) {
			sArrayTableError = "Unexpected error flattening sequence: " + Utility.extractStackTrace(ex);
		}
		} // end flatten sequence
		if( sArrayTableError == null ){
			return theArrayTable;
		} else {
			sbError.append("flattening error: " + sArrayTableError);
			return null;
		}
	}

	private int recursion_CountNormalizedRows( DSequence seq, int iLevel ){
		int iRowCount = seq.getRowCount();
		int iFieldNumber = maiPath1[iLevel + 1];
		BaseType btField;
		try {
			btField = seq.getVar(iFieldNumber - 1);
		} catch(Exception ex) {
			ApplicationController.vShowError( "field at level " + iLevel + " field " + iFieldNumber + " not available: " + ex );
			return 0;
		}
		if( btField instanceof DSequence ){ // add rows associated with the sequence
			int iTotalRows = 0;
			for( int xRow = 1; xRow <= iRowCount; xRow++ ){
				Vector vectorRow = seq.getRow(xRow - 1);
				DSequence sub_sequence = (DSequence)vectorRow.get(iFieldNumber - 1);
				iTotalRows += recursion_CountNormalizedRows( sub_sequence, iLevel+1 );
			}
			return iTotalRows;
		} else {
			return iRowCount;
		}
	}

	// uses a 2D storage array where D1 is Field and D2 is rows
	// returns the number of rows added
	private int mctProcessedRows = 0; // these variables are used for progress reporting only
	private int mctTotalRows = 0;
	private int recursion_AddNormalizedData( DSequence seq, int iLevel, double[][] ad, int iFieldCount, int xFieldCursor, int xRowCursor ){ // flatten
		int iRowCount = seq.getRowCount();
		int iFieldNumber = maiPath1[iLevel + 1];
		BaseType btPathField;
		try {
			btPathField = seq.getVar(iFieldNumber - 1);
		} catch(Exception ex) {
			ApplicationController.vShowError( "while adding data, field at level " + iLevel + " field# " + iFieldNumber + " not available: " + ex );
			return 0;
		}
		if( btPathField instanceof DSequence ){ // add sub-sequence data for each row
			int ctRowsAdded = 0;
			for( int xRow = 1; xRow <= iRowCount; xRow++ ){
				Vector vectorRow = seq.getRow(xRow - 1);

				// store the scalars for the parent sequence
				int ctScalarFields = 0;
				if( iRowCount > 1 ){
					for( int xField = 1; xField <= vectorRow.size(); xField++ ){
						Object oField = vectorRow.get(xField - 1);
						BaseType btField = (BaseType)oField;
						if( btField instanceof DBoolean ||
							btField instanceof DByte ||
							btField instanceof DInt16 ||
							btField instanceof DUInt16 ||
							btField instanceof DInt32 ||
							btField instanceof DUInt32 ||
							btField instanceof DFloat32 ||
							btField instanceof DFloat64 ||
							btField instanceof DString ){
							double dValue = DAP.convertToDouble(btField);
							ad[xFieldCursor + xField - 1][xRowCursor] = dValue;
							ctScalarFields ++;
						}
					}
				}

				// add the rows for the sub-sequence
				DSequence sub_sequence = (DSequence)vectorRow.get(iFieldNumber - 1);
				ctRowsAdded += recursion_AddNormalizedData( sub_sequence, iLevel+1, ad, iFieldCount, xFieldCursor + ctScalarFields, xRowCursor + ctRowsAdded );
			}
			return ctRowsAdded;
		} else { // load data
			for( int xRow = 1; xRow <= iRowCount; xRow++ ){

				// store scalar in array
				Vector vectorRow = seq.getRow(xRow - 1);
				BaseType btValue = (BaseType)vectorRow.get(iFieldNumber - 1);
				double dValue = DAP.convertToDouble(btValue);
				ad[iFieldCount][xRowCursor + xRow - 1] = dValue;

				// copy any normalized field data to this row
				if( xRow > 1 && iFieldCount > 1 ){
					for( int xField = 1; xField < iFieldCount; xField++ ){
						ad[xField][xRowCursor + xRow - 1] = ad[xField][xRowCursor] = dValue;
					}
				}

				// notify user of progress
				mctProcessedRows++;
				if( mctProcessedRows % 10000 == 0 ) ApplicationController.vShowStatus_NoCache("processed " + mctProcessedRows + " sequence rows out of " + mctTotalRows);
			}
			return iRowCount;
		}
	}

	public String toString(){
		StringBuffer sb = new StringBuffer(120);
		sb.append("variable specification: \n");
		sb.append("\tname: " + msName + "\n");
		sb.append("\tqualified name: " + getFullyQualifiedName() + "\n");
		sb.append("\tlong name: " + msAttribute_LongName + "\n");
		sb.append("\tunits: " + msAttribute_Units + "\n");
		sb.append("\tbase node: " + sBaseNode + "\n");
		sb.append("\tpath: \n");
		for( int xPath = 1; xPath <= maiPath1[0]; xPath++){
			sb.append(" " + maiPath1[xPath] + "-" + masPath1[xPath]).append("\n");
		}
		sb.append("\terror: " + sArrayTableError);
		return sb.toString();
	}

	public String toString_path(){
		StringBuffer sb = new StringBuffer(120);
		for( int xPath = 1; xPath <= maiPath1[0]; xPath++){
			sb.append( (xPath > 1 ? "." : "") + maiPath1[xPath] + "-" + masPath1[xPath]);
		}
		return sb.toString();
	}

}

class ArrayTable {
	ArrayTable( BaseType bt, String[] fields ){
		this.bt = bt;
		aFieldNames = fields;
	}
	BaseType bt; // either a DGrid or a DArray
	String[] aFieldNames;
}

class AxisSpecification {
	public final static int MODE_None = 0;
	public final static int MODE_Indexed = 1;
	public final static int MODE_Vector = 2;
	private int mMODE;
	private VariableSpecification mVS = null;
	public AxisSpecification( VariableSpecification vs ){
		mMODE = MODE_Vector;
		mVS = vs;
	}
	public AxisSpecification( int eMODE ){
		mMODE = eMODE;
	}
	public int getMODE(){ return mMODE; }
	public VariableSpecification getVariableSpecification(){ return mVS; }
}

