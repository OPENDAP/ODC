package opendap.clients.odc.plot;

/**
 * <p>Title: Model_Variables</p>
 * <p>Description: Represents a suite of variables for a plot</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: OPeNDAP</p>
 * @author John Chamberlain
 * @version 2.58
 */

import java.util.ArrayList;
import java.util.Vector;
import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility;
import opendap.clients.odc.DAP;
import opendap.dap.*;

/** The value lists must have the same number of values */
public class Model_Variables {
    public Model_Variables() {}
	private Model_Variable mVar1 = null;
	private Model_Variable mVar2 = null;
	private Model_Variable mAxis_X;
	private Model_Variable mAxis_Y;
	void vClear(){
		mVar1   = null;
		mVar2   = null;
		mAxis_X = null;
		mAxis_Y = null;
	}
	void setVariable1( Model_Variable model ){ mVar1 = model; }
	void setVariable2( Model_Variable model ){ mVar2 = model; }
	void setX( Model_Variable model ){
		mAxis_X = model;
	}
	void setY( Model_Variable model ){
		mAxis_Y = model;
	}
	Model_Variable getModel_Variable1(){ return mVar1; }
	Model_Variable getModel_Variable2(){ return mVar2; }
	Model_Variable getModel_Axis_X(){ return mAxis_X; }
	Model_Variable getModel_Axis_Y(){ return mAxis_Y; }
	int getCount_Variable1(){
		if( mVar1 == null ) return 0;
		return mVar1.getVariableCount();
	}
	int getCount_Variable2(){
		if( mVar2 == null ) return 0;
		return mVar2.getVariableCount();
	}
}

// The path shows the route to the variable in the dataset tree.
// For example if the path is (2,3,4) then it means the 4th field, of the 3rd field of the
// 2nd field of the variable; The string path validates the path against the variable/structure names.
class Model_Variable {
	private VariableInfo[] maVariableInfo = null;
	private VariableSpecification theVariableSpecification = null;
	private boolean mzUseEntireVariable = false; // true for histogram currently
	private int mxDimX = 0;
	private int mxDimY = 0;
	private int[][] maSliceIndexes; // mSliceIndexes[dim index][0] = count of slices, mSliceIndexes[dim index][xSlice] = array index of slice
	private String[][] masSliceCaptions;
	private boolean mzReversedX, mzReversedY, mzReversedZ;

	Model_Variable( VariableSpecification vs ){ theVariableSpecification = vs; }

	void setUseEntireVariable( boolean z ){ mzUseEntireVariable = z; }
	boolean getUseEntireVariable(){ return mzUseEntireVariable; }
	void setDimX( int xIndex ){ mxDimX = xIndex; } // this is the number of the dimension selected for the x-axis in a 2D plot
	void setDimY( int xIndex ){ mxDimY = xIndex; } // this is the number of the dimension selected for the y-axis in a 2D plot
	void setReversedX( boolean z ){ mzReversedX = z; }
	void setReversedY( boolean z ){ mzReversedY = z; }
	void setReversedZ( boolean z ){ mzReversedZ = z; }
	String getName(){ if( theVariableSpecification == null ) return null; else return theVariableSpecification.getName(); }
	int getDimX(){ return mxDimX; } // this is the number of the dimension selected for the x-axis in a 2D plot
	int getDimY(){ return mxDimY; } // this is the number of the dimension selected for the y-axis in a 2D plot
	int getInversion(){ return ( mxDimX > mxDimY ) ? DAP.INVERSION_YXZ : DAP.INVERSION_XYZ; }
	boolean getReversedX(){ return mzReversedX; }
	boolean getReversedY(){ return mzReversedY; }
	boolean getReversedZ(){ return mzReversedZ; }

	int getVariableCount(){
		if( maVariableInfo == null ) return 0;
		return maVariableInfo.length - 1;
	}

	VariableSpecification getVariableSpecification(){ return theVariableSpecification; }

	/** Returns a one-based list of VariableInfo objects. There will only be more than one object
	 *  in the list if the user has asked for multiple slices of the data.
	 */
	VariableInfo[] getVariableInfo( StringBuffer sbError ){
		try {

			if( maVariableInfo != null ) return maVariableInfo; /* the variable info is cached */

			if( theVariableSpecification == null ){
				sbError.append("variable specification does not exist");
				return null;
			}

			int ctDimensions;
			int[] aiDimSizes;
			ArrayTable atValues = theVariableSpecification.getArrayTable(sbError);
			if( atValues == null ) return null;
			BaseType btValues = atValues.bt;
			String sVariableName = btValues.getName();
			String sVariableLongName = btValues.getLongName();
			if( btValues instanceof DArray ){
				DArray darray = (DArray)btValues;
				int eDataType = DAP.getDArrayType(darray);
				if( eDataType == 0 ){
					sbError.append("unrecognized data type for dataset array");
					return null;
				}
				ctDimensions = darray.numDimensions();
				aiDimSizes = new int[ctDimensions + 1];
				for( int xDim = 1; xDim <= ctDimensions; xDim++ ){
					aiDimSizes[xDim] = darray.getDimension(xDim-1).getSize();
				}
			} else if( btValues instanceof DSequence ){ // convert data to vectors of doubles
				DSequence dsequence = (DSequence)btValues;
				VariableInfo varSequenceVector = zCreateVariable_FromDSequence( dsequence, sVariableName, sbError );
				if( varSequenceVector == null ){
					sbError.insert(0, "Failed to form variable info for field " + sVariableName + " of sequence: ");
					return null;
				} else {
					varSequenceVector.setName( sVariableName );
					varSequenceVector.setLongName( sVariableLongName );
					varSequenceVector.setUserCaption( null );
					// todo how is this determined: varSequenceVector.setUnits( vs.getAttribute_Units() );
					maVariableInfo = new VariableInfo[2];
					maVariableInfo[1] = varSequenceVector;
					return maVariableInfo;
				}
			} else {
				sbError.append("base type is not an array or a sequence");
				return null;
			}

			// this is true when doing a histogram
			if( mzUseEntireVariable ){
				VariableInfo infoValues = zCreateVariable_FromDArrayNT( btValues, sbError);
				if( infoValues == null ){
					sbError.insert(0, "error creating value variable info for histogram: ");
					return null;
				} else {
					infoValues.setName( sVariableName );
					infoValues.setLongName( sVariableLongName );
					infoValues.setUserCaption( null );
					// todo ??? infoValues.setUnits( vs.getAttribute_Units() );
					infoValues.setSliceCaption("[all data]");
					maVariableInfo = new VariableInfo[2];
					maVariableInfo[1] = infoValues;
					return maVariableInfo;
				}
			}

			// determine slices and verify that there is at most one multi-slice dim
			int xDimX = getDimX();
			int xDimY = getDimY();
			int ctUsedDimensions = ((xDimX > 0) ? 1 : 0) + ((xDimY > 0) ? 1 : 0);
			if( ctUsedDimensions ==  0 ){
				sbError.append("No dimensions defined for model. There must be at least an X- or Y-dimension.");
				return null;
			}
			int ctDimensions_extra = ctDimensions - ctUsedDimensions;
			int[] axSliceIndex1 = new int[ctDimensions + 1]; // this is the slice index0 for each of the extra dimensions; if one of the extra dims has multiple slices then the value will be 0; the x and y dims will have value 0
			int xDim = 0;
			int xExtraDim = 0;
			int xMultiSliceDim = 0;
			int[] axMultiSlice = null;
			int ctMultiSlice = 1;
			while(true){
				xDim++;
				if( xDim > ctDimensions ) break;
				if( xDim == xDimX || xDim == xDimY ) continue;
				int[] axSlices = maSliceIndexes[xDim];
				if( axSlices == null ){
					sbError.append("unable to get slice indexes for dim " + xDim);
					return null;
				}
				int ctSlices = axSlices.length - 1;
				if( ctSlices < 1 ){
					axSliceIndex1[xDim] = 0; // default to zero index for undefined slices
				} else if( ctSlices == 1 ) {
					axSliceIndex1[xDim] = axSlices[1];
				} else { // multi-slice definition
					if( xMultiSliceDim > 0 ){
						sbError.append("Only one dimension can have multiple slices.");
						return null;
					} else {
						xMultiSliceDim = xDim;
						axMultiSlice = axSlices;
						ctMultiSlice = axMultiSlice.length - 1; // because this is a one-based array
					}
				}
			}

			// iterate slices and add a value set for each one
			int[] axSliceIndex1_current = new int[ctDimensions + 1]; // this is the slice index0 for each of the extra dimensions; if one of the extra dims has multiple slices then the value will be the current slice index; the x and y dims will have value 0

			StringBuffer sbSliceCaption = new StringBuffer();
			String sSliceCaption = null;
			int ctVariables = 0;
			for( int xSlice = 1; xSlice <= ctMultiSlice; xSlice++ ){

				sSliceCaption = null;
				String sMultiSliceDim = "";
				if( xMultiSliceDim > 0 ){
					String sMultiSliceCaption = masSliceCaptions[xMultiSliceDim][xSlice];
					if( sMultiSliceCaption == null ){
						// no user defined caption - caption will be auto generated
					} else {
						sSliceCaption = sMultiSliceCaption;
					}
					if( btValues instanceof DArray ){
						DArray darray = (DArray)btValues;
						try {
							sMultiSliceDim = ((DArray)btValues).getDimension(xMultiSliceDim).getName();
						} catch(Exception ex) {
							sMultiSliceDim = "??"; // todo
						}
					} else {
						sMultiSliceDim = "?";
					}
				}

				String[] masDimCaption = new String[ctDimensions + 1]; // new String[ctDimensions_extra + 1];
				for( xDim = 1; xDim <= ctDimensions; xDim++ ){
					String sDimCaption = masSliceCaptions[xDim][xSlice];
					masDimCaption[xDim] = sDimCaption; // not used
					if( xDim == xDimX || xDim == xDimY ){
						axSliceIndex1_current[xDim] = 0;
					} else if( xDim == xMultiSliceDim ) {
						axSliceIndex1_current[xDim] = axMultiSlice[xSlice];
					} else {
						axSliceIndex1_current[xDim] = axSliceIndex1[xDim];
					}
				}

				if( sSliceCaption == null ){  // build caption
					sbSliceCaption.setLength(0);
					for( xDim = 1; xDim <= ctDimensions; xDim++ ){
						if( xDim != xDimX && xDim != xDimY ){
							String sConstrainedDim;
							if( btValues instanceof DArray ){
								try {
									sConstrainedDim = ((DArray)btValues).getDimension(xDim-1).getName();
								} catch(Exception ex) {
									sConstrainedDim = "[?]"; // todo
								}
							} else {
								sConstrainedDim = "[" + xDim + "]";
							}
							sbSliceCaption.append(sConstrainedDim);
							sbSliceCaption.append(" " + (axSliceIndex1_current[xDim] + 1)); // slice caption indexes are one-based
						}
					}
					sSliceCaption = sbSliceCaption.toString();
				}
				if( sSliceCaption.length() == 0 ) sSliceCaption = null;

				// primary values (or U-Component)
				VariableInfo infoValues = zCreateVariable_FromDArray( btValues, xDimX, xDimY, ctDimensions, aiDimSizes, axSliceIndex1_current, sbError);
				if( infoValues == null ){
					sbError.insert(0, "error creating value variable info " + xSlice + ": ");
					return null;
				} else {
					infoValues.setName( sVariableName );
					infoValues.setLongName( sVariableLongName );
					infoValues.setUserCaption( null );
					// todo xxx infoValues.setUnits( vs.getAttribute_Units() );
					infoValues.setSliceCaption( sSliceCaption );

					// add the variable;
					ctVariables++;
					if( maVariableInfo == null ){
						maVariableInfo = new VariableInfo[ctMultiSlice + 1];
					} else if( ctVariables > maVariableInfo.length - 1){
						VariableInfo[] new_maVariableInfo = new VariableInfo[maVariableInfo.length * 2];
						System.arraycopy(maVariableInfo, 0, new_maVariableInfo, 0, maVariableInfo.length);
					}
					maVariableInfo[ctVariables] = infoValues;
				}
			}

			// contract array to its natural size
			if( ctVariables < maVariableInfo.length - 1 ){
				VariableInfo[] contracted_maVariableInfo = new VariableInfo[ctVariables + 1];
				System.arraycopy(maVariableInfo, 0, contracted_maVariableInfo, 0, ctVariables + 1);
				maVariableInfo = contracted_maVariableInfo;
			}

			return maVariableInfo;
		} catch( Exception ex ) {
			Utility.vUnexpectedError( ex, sbError );
			return null;
		}
	}

	private VariableInfo zCreateVariable_FromDSequence(DSequence dsequence, String sField, StringBuffer sbError){
		if( dsequence == null ){
			sbError.append("sequence was missing");
			return null;
		}
		if( sField == null ){
			sbError.append("field name was missing");
			return null;
		}
		int ctRows = dsequence.getRowCount();
		int xRow = 0;
		if( !Utility.zMemoryCheck(ctRows, 8, sbError) ) return null;
		double[] adValues = new double[ctRows];
		int ctErrors = 0;
		try {
			for( xRow = 0; xRow < ctRows; xRow++ ){
				BaseType btRow = dsequence.getVariable(xRow, sField);
				int eVALUE_TYPE = DAP.getType_Plot(btRow);
				if( eVALUE_TYPE == 0 ){
					sbError.append("unsupportable variable type in row " + xRow);
					return null;
				}
				switch( eVALUE_TYPE ){
					case DAP.DATA_TYPE_Byte:
						adValues[xRow] = (double)((int)((DByte)btRow).getValue() & 0xFF);
						break;
					case DAP.DATA_TYPE_Int16:
						adValues[xRow] = (double)((DInt16)btRow).getValue();
						break;
					case DAP.DATA_TYPE_UInt16:
						adValues[xRow] = (double)((int)((DUInt16)btRow).getValue() & 0xFFFF);
						break;
					case DAP.DATA_TYPE_Int32:
						adValues[xRow] = (double)((DInt32)btRow).getValue();
						break;
					case DAP.DATA_TYPE_UInt32:
						adValues[xRow] = (double)((long)((DUInt32)btRow).getValue() & 0xFFFFFFFF);
						break;
					case DAP.DATA_TYPE_Float32:
						adValues[xRow] = (double)((DFloat32)btRow).getValue();
						break;
					case DAP.DATA_TYPE_Float64:
						adValues[xRow] = (double)((DFloat64)btRow).getValue();
						break;
					case DAP.DATA_TYPE_String:
						String sValue = ((DString)btRow).getValue();
						try {
							adValues[xRow] = Double.parseDouble(sValue);
						} catch(Exception ex) {
							ctErrors++;
							if( ctErrors == 1 ){
								sbError.append("Error converting string value to double in row " + (xRow+1) + ".");
							}
						}
						break;
					default:
						sbError.append("unsupported data type: " + eVALUE_TYPE);
						return null;
				}
			}
			if( ctErrors > 1 ) sbError.append("There were " + ctErrors + " conversion errors.");
			ApplicationController.vShowWarning(sbError.toString());
			sbError.setLength(0);
		} catch(Exception ex) {
			sbError.append("error processing row " + (xRow+1) + " of sequence: " + ex);
			return null;
		}
		VariableInfo infoValues = new VariableInfo();
		infoValues.setValues(adValues, 1, ctRows, 1);
		return infoValues;
	}

	// no transformations version of main routine (used when entire array is returned in bulk) for histogram
	private VariableInfo zCreateVariable_FromDArrayNT(BaseType btValues, StringBuffer sbError){
		try {
			VariableInfo infoValues = new VariableInfo();
			if( !( btValues instanceof DArray) ){
				sbError.append("base type is not an array");
				return null;
			}
			DArray darray = (DArray)btValues;

			// execute the mapping and do any necessary type conversions
			int eVALUE_TYPE = DAP.getDArrayType(darray);
			if( eVALUE_TYPE == 0 ){
				sbError.append("unsupportable array type for DArray conversion");
				return null;
			}
			Object oValues = darray.getPrimitiveVector().getInternalStorage();
			byte[] abValues = null;
			short[] ashValues = null;
			int[] aiValues = null;
			long[] anValues = null;
			int xValue, ctValues;
			switch( eVALUE_TYPE ){
				case DAP.DATA_TYPE_Byte:
					abValues = (byte[])oValues;
					ctValues = abValues.length;
					if( !Utility.zMemoryCheck(ctValues, 2, sbError) ) return null;
					short[] ashTransformedValues0 = new short[ctValues];
					for( xValue = 0; xValue < ctValues; xValue++ ){
						ashTransformedValues0[xValue] = (short)((short)abValues[xValue] & 0xFF);
					}
					infoValues.setValues(ashTransformedValues0, eVALUE_TYPE, 1, ctValues, 1);
					break;
				case DAP.DATA_TYPE_Int16:
					ashValues = (short[])oValues;
					ctValues = ashValues.length;
					infoValues.setValues(ashValues, eVALUE_TYPE, 1, ctValues, 1);
					break;
				case DAP.DATA_TYPE_UInt16:
					ashValues = (short[])oValues;
					ctValues = ashValues.length;
					if( !Utility.zMemoryCheck(ctValues, 4, sbError) ) return null;
					int[] aiTransformedValues0 = new int[ctValues];
					for( xValue = 0; xValue < ctValues; xValue++ ){
						aiTransformedValues0[xValue] = (int)((int)ashValues[xValue] & 0xFFFF);
					}
					infoValues.setValues(aiTransformedValues0, eVALUE_TYPE, 1, ctValues, 1);
					break;
				case DAP.DATA_TYPE_Int32:
					aiValues = (int[])oValues;
					ctValues = aiValues.length;
					infoValues.setValues(aiValues, eVALUE_TYPE, 1, ctValues, 1);
					break;
				case DAP.DATA_TYPE_UInt32:
					aiValues = (int[])oValues;
					ctValues = aiValues.length;
					if( !Utility.zMemoryCheck(ctValues, 8, sbError) ) return null;
					long[] anTransformedValues0 = new long[ctValues];
					for( xValue = 0; xValue < ctValues; xValue++ ){
						anTransformedValues0[xValue] = (long)((long)aiValues[xValue] & 0xFFFFFFFF);
					}
					infoValues.setValues(anTransformedValues0, 1, ctValues, 1);
					break;
				case DAP.DATA_TYPE_Float32:
					float[] afValues = (float[])oValues;
					ctValues = afValues.length;
					infoValues.setValues(afValues, 1, ctValues, 1);
					break;
				case DAP.DATA_TYPE_Float64:
					double[] adValues = (double[])oValues;
					ctValues = adValues.length;
					infoValues.setValues(adValues, 1, ctValues, 1);
					break;
				case DAP.DATA_TYPE_String:
					sbError.append("cannot create variable from String data");
	    			return null;
				default:
					sbError.append("unknown array data type: " + eVALUE_TYPE);
	    			return null;
			}
			return infoValues;
		} catch( Exception ex ) {
			Utility.vUnexpectedError( ex, sbError );
			return null;
		}
	}

	private VariableInfo zCreateVariable_FromDArray(BaseType btValues, int xDimX, int xDimY, int ctDimensions, int[] aiDimSizes, int[] axSliceIndex1, StringBuffer sbError){
		if( aiDimSizes == null ){
			sbError.append("internal error, no dim sizes");
			return null;
		}
		if( xDimX < 0 || xDimX >= aiDimSizes.length ){
			sbError.append("internal error, invalid dimX index");
			return null;
		}
		if( xDimY < 0 || xDimY >= aiDimSizes.length ){
			sbError.append("internal error, invalid dimY index");
			return null;
		}
		int lenX = aiDimSizes[xDimX];
		int lenY = aiDimSizes[xDimY];
		if( lenX == 0 ) lenX = 1; // unused dimensions are considered to be length 1
		if( lenY == 0 ) lenY = 1;
		if( ((long)lenX *(long)lenY) > (long)Integer.MAX_VALUE ){
			sbError.append("X (" + lenX + ") x Y (" + lenY + ") exceeds the maximum allocatable size of an array (" + Integer.MAX_VALUE + ")");
			return null;
		}
		int ctValues = (xDimX == 0 ? 1 : lenX) * (xDimY == 0 ? 1 : lenY);
		try {
			VariableInfo infoValues = new VariableInfo();
			if( !( btValues instanceof DArray) ){
				sbError.append("base type is not an array");
				return null;
			}
			DArray darray = (DArray)btValues;

			// make a mapping for converting column-major to row-major for this variables shape
			// model for 2 dimensions:
			//   xD1*lenD2 + xD2  transforms to:
			//   xD1 + xD2*lenD1
			// model for 3 dimensions:
			//   xD1*lenD2*lenD3 + xD2*lenD3 + xD3  transforms to:
			//   xD1 + xD2*lenD1 + xD3*lenD2*lenD1
			// the algorithm below extrapolates this to n-dimensions
			int ctTransformedDimensions = (xDimX == 0 ? 0 : 1) + (xDimY == 0 ? 0 : 1);
			if( !Utility.zMemoryCheck(ctValues * 2, 4, sbError) ) return null;
			int[] axMappingCM2RM_base = new int[ctValues + 1]; // will contain mapping from base array to transformed array
			int[] axMappingCM2RM_transform = new int[ctValues + 1]; // will contain mapping from base array to transformed array
			int[] aiDimCursor = new int[ctDimensions+1];
			int xValue = 0;
			for( int x = 0; x < lenX; x++ ){
				for( int y = 0; y < lenY; y++ ){
					xValue++;
					int xBase_index = 0;
					int xTransform_index = 0;
					for( int xDimCursor = 1; xDimCursor <= ctDimensions; xDimCursor++ ){
						int indexCursor = (xDimCursor == xDimX ? x : (xDimCursor == xDimY ? y : axSliceIndex1[xDimCursor] ));
						int xBase_multiplier = 1;
						for( int xMultiplier = xDimCursor + 1; xMultiplier <= ctDimensions; xMultiplier++ )
							xBase_multiplier *= aiDimSizes[xMultiplier];
						xBase_index      += indexCursor * xBase_multiplier;
					}
					xTransform_index = x + y * lenX;
					axMappingCM2RM_base[xValue] = xBase_index;
					axMappingCM2RM_transform[xValue] = xTransform_index;
				}
			}

			// execute the mapping and do any necessary type conversions
			int eVALUE_TYPE = DAP.getDArrayType(darray);
			if( eVALUE_TYPE == 0 ){
				sbError.append("unsupportable array type");
				return null;
			}
			Object oValues = darray.getPrimitiveVector().getInternalStorage();
			byte[] abValues = null;
			short[] ashValues = null;
			int[] aiValues = null;
			long[] anValues = null;
			switch( eVALUE_TYPE ){
				case DAP.DATA_TYPE_Byte:
				case DAP.DATA_TYPE_Int16:

					// convert from column-major to row-major and to short
					if( eVALUE_TYPE == DAP.DATA_TYPE_Byte ){
						abValues = (byte[])oValues;
					} else {
						ashValues = (short[])oValues;
					}
					if( !Utility.zMemoryCheck(ctValues, 2, sbError) ) return null;
					short[] ashTransformedValues0 = new short[ctValues];
					for( xValue = 1; xValue <= ctValues; xValue++ ){
						if( eVALUE_TYPE == DAP.DATA_TYPE_Byte )
							ashTransformedValues0[axMappingCM2RM_transform[xValue]] = (short)((short)abValues[axMappingCM2RM_base[xValue]] & 0xFF);
						else
							ashTransformedValues0[axMappingCM2RM_transform[xValue]] = ashValues[axMappingCM2RM_base[xValue]];
					}
					infoValues.setValues(ashTransformedValues0, eVALUE_TYPE, ctTransformedDimensions, lenX, lenY);
					break;

				case DAP.DATA_TYPE_UInt16:
				case DAP.DATA_TYPE_Int32:

					// convert from column-major to row-major and to int
					if( eVALUE_TYPE == DAP.DATA_TYPE_UInt16 ){
						ashValues = (short[])oValues;
					} else {
						aiValues = (int[])oValues;
					}
					if( !Utility.zMemoryCheck(ctValues, 4, sbError) ) return null;
					int[] aiTransformedValues0 = new int[ctValues];
					for( xValue = 1; xValue <= ctValues; xValue++ ){
						if( eVALUE_TYPE == DAP.DATA_TYPE_UInt16 )
							aiTransformedValues0[axMappingCM2RM_transform[xValue]] = (int)((int)ashValues[axMappingCM2RM_base[xValue]] & 0xFFFF);
						else
							aiTransformedValues0[axMappingCM2RM_transform[xValue]] = aiValues[axMappingCM2RM_base[xValue]];
					}
					infoValues.setValues(aiTransformedValues0, eVALUE_TYPE, ctTransformedDimensions, lenX, lenY);
					break;

				case DAP.DATA_TYPE_UInt32:

					// convert from column-major to row-major and to long
					aiValues = (int[])oValues;
					if( !Utility.zMemoryCheck(ctValues, 8, sbError) ) return null;
					long[] anTransformedValues0 = new long[ctValues];
					for( xValue = 1; xValue <= ctValues; xValue++ ){
						anTransformedValues0[axMappingCM2RM_transform[xValue]] = (long)((long)aiValues[axMappingCM2RM_base[xValue]] & 0xFFFFFFFF);
					}
					infoValues.setValues(anTransformedValues0, ctTransformedDimensions, lenX, lenY);
					break;

				case DAP.DATA_TYPE_Float32:

					// convert from column-major to row-major
					float[] afValues = (float[])oValues;
					if( !Utility.zMemoryCheck(ctValues, 4, sbError) ) return null;
					float[] afTransformedValues0 = new float[ctValues];
					for( xValue = 1; xValue <= ctValues; xValue++ ){
						afTransformedValues0[axMappingCM2RM_transform[xValue]] = afValues[axMappingCM2RM_base[xValue]];
					}
					infoValues.setValues(afTransformedValues0, ctTransformedDimensions, lenX, lenY);
					break;
				case DAP.DATA_TYPE_Float64:

					// convert from column-major to row-major
					double[] adValues = (double[])oValues;
					if( !Utility.zMemoryCheck(ctValues, 8, sbError) ) return null;
					double[] adTransformedValues0 = new double[ctValues];
					for( xValue = 1; xValue <= ctValues; xValue++ ){
						adTransformedValues0[axMappingCM2RM_transform[xValue]] = adValues[axMappingCM2RM_base[xValue]];
					}
					infoValues.setValues(adTransformedValues0, ctTransformedDimensions, lenX, lenY);
					break;
				default:
					sbError.append("unknown array data type: " + eVALUE_TYPE);
	    			return null;
			}
			return infoValues;
		} catch( Exception ex ) {
			Utility.vUnexpectedError( ex, sbError );
			return null;
		} catch( Throwable t ) {
			sbError.append("Unable to transform " + ctValues + " values: " + t);
			return null;
		}
	}

}