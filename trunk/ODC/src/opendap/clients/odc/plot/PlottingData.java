package opendap.clients.odc.plot;

import opendap.clients.odc.DAP;
import java.util.ArrayList;

class PlottingData {
	public final static int AXES_None = 0;
	public final static int AXES_Linear = 1;
	public final static int AXES_Mapped = 2;
	private int mctDimensions;
	private int[] maiDim_TYPE1;
	private int mAXES_TYPE;
	private ArrayList<PlottingVariable> listVariables1 = new ArrayList<PlottingVariable>();
	private ArrayList<PlottingVariable> listVariables2 = new ArrayList<PlottingVariable>();
	private VariableInfo mvarAxis_X = null;
	private VariableInfo mvarAxis_Y = null;
	VariableInfo getAxis_X(){ return mvarAxis_X; }
	VariableInfo getAxis_Y(){ return mvarAxis_Y; }
	void setAxis_X(VariableInfo var, boolean zUseIndex){
		mvarAxis_X = var;
		if( zUseIndex ) var.setUseIndex();
	}
	void setAxis_Y(VariableInfo var, boolean zUseIndex){
		mvarAxis_Y = var;
		if( zUseIndex ) var.setUseIndex();
	}
	void vInitialize( int ctDimensions, int iAXES_TYPE ){
		mctDimensions = ctDimensions;
		mAXES_TYPE = iAXES_TYPE;
	}
	boolean zAddVariable( Object[] eggData1, Object[] eggData2, int eDataType1, int eDataType2, int[] aiDimLength1, Object[] eggMissing1, Object[] eggMissing2, String sDataCaption1, String sDataCaption2, String sDataUnits1, String sDataUnits2, String sSliceCaption1, String sSliceCaption2, StringBuffer sbError ){
		if( eggData1 == null && eggData2 == null ){ sbError.append("data egg not supplied"); return false; }
		if( eggData1 != null ){
			if( eggData1[0] == null ){ sbError.append("data egg 1 is empty"); return false; }
	        if( eggData1 != null && eggData1.length != 1 ){ sbError.append("data egg 1 does not have exactly one element"); return false; }
			if( eggMissing1 != null ) if( eggMissing1.length != 1 ){ sbError.append("missing egg 1 does not have exactly one element"); return false; }
			PlottingVariable pv = new PlottingVariable();
	    	if( !pv.zSet(eDataType1, eggData1, aiDimLength1, eggMissing1, sDataCaption1, sDataUnits1, sSliceCaption1, sbError) ) return false;
		    listVariables1.add( pv );
		}
		if( eggData2 != null ){
			if( eggData2[0] == null ){ sbError.append("data egg 2 is empty"); return false; }
	        if( eggData2 != null && eggData2.length != 1 ){ sbError.append("data egg 2 does not have exactly one element"); return false; }
		    if( eggMissing2 != null ) if( eggMissing2.length != 1 ){ sbError.append("missing egg 2 does not have exactly one element"); return false; }
			PlottingVariable pv2 = new PlottingVariable();
			if( !pv2.zSet(eDataType2, eggData2, aiDimLength1, eggMissing2, sDataCaption2, sDataUnits2, sSliceCaption2, sbError) ) return false;
			listVariables2.add( pv2 );
		}
		return true;
	}
	int getDimensionCount(){ return mctDimensions; }
	int getAxesTYPE(){ return mAXES_TYPE; }
	int getVariable1Count(){ return listVariables1.size(); }
	int getVariable2Count(){ return listVariables2.size(); }
	int getVariableCount(){ if( listVariables1.size() == 0 ) return listVariables2.size(); else return listVariables1.size(); }
	PlottingVariable getVariable_Primary(){
		if( getVariable1Count() > 0 ) return getVariable1(1);
		return getVariable2(1);
	}
	PlottingVariable getVariable1(int xVariable1){
		if( xVariable1 < 1 || xVariable1 > listVariables1.size() ) return null;
		return (PlottingVariable)listVariables1.get( xVariable1 - 1);
	}
	PlottingVariable getVariable2(int xVariable1){
		if( xVariable1 < 1 || xVariable1 > listVariables2.size() ) return null;
		return (PlottingVariable)listVariables2.get( xVariable1 - 1);
	}
	String getAxesTYPE_S(){
		switch(mAXES_TYPE){
			case AXES_None: return "None";
			case AXES_Linear: return "Linear";
			case AXES_Mapped: return "Mapped";
			default: return "[unknown " + mAXES_TYPE + "]";
		}
	}
	public String toString(){
		StringBuffer sb = new StringBuffer(120);
		sb.append("PlottingData (axes: " + getAxesTYPE_S() + ", dims: " + mctDimensions);
		sb.append(" {");
		for( int xDim = 1; xDim <= mctDimensions; xDim++ ){
			if( xDim > 1 ) sb.append(", ");
			sb.append( DAP.getType_String(maiDim_TYPE1[xDim]) );
		}
		sb.append("} variable count " + getVariableCount() + ":\n");
		for( int xVar = 1; xVar <= getVariableCount(); xVar++ ){
			PlottingVariable pv = getVariable1(xVar);
			sb.append("var " + xVar + ": " + pv);
		}
		return sb.toString();
	}

}

class PlottingVariable {
	private int meDataType; // see DAP for types
	private int[] maiDimLength1; // all four of these arrays must have same length
	private Object[] meggData; // the egg containing the data
	private Object[] meggMissing1; // the egg containing the missing values in a one-based linear array
	private String msDataCaption;
	private String msDataUnits;
	private String msSliceCaption;

	/** the egg containing the missing values is a one-based linear array which can be null */
	boolean zSet( int eDataType, Object[] eggData, int[] aiDimLength1, Object[] eggMissing, String sDataCaption, String sDataUnits, String sSliceCaption, StringBuffer sbError ){
		if( eggData == null ){
			sbError.append("no egg supplied");
			return false;
		}
		if( eggData[0] == null ){
			sbError.append("egg is empty");
			return false;
		}
		meggData = eggData;
		meggMissing1 = eggMissing;
		meDataType = eDataType;
		maiDimLength1 = aiDimLength1;
		msDataCaption = sDataCaption;
		msDataUnits = sDataUnits;
		msSliceCaption = sSliceCaption;
		return true;
	}
	String getDataCaption(){ return msDataCaption; }
	String getDataUnits(){ return msDataUnits; }
	String getSliceCaption(){ return msSliceCaption; }
	Object[] getDataEgg(){ return meggData; }
	Object[] getMissingEgg(){ return meggMissing1; }
	void setMissingEgg( Object[] eggMissing1 ){ meggMissing1 = eggMissing1; }
	int getDataType(){ return meDataType; }
	int getDimCount(){ return maiDimLength1[0]; }
	int getDimLength( int xDim1 ){
		if( maiDimLength1 == null ) return 0;
		if( xDim1 < 1 || xDim1 >= maiDimLength1.length ) return 0;
		return maiDimLength1[xDim1];
	}
	public String toString(){
		StringBuffer sb = new StringBuffer(120);
		sb.append("PlottingVariable type: " + DAP.getType_String(meDataType) + " dims: {");
		for( int xDim = 0; xDim < maiDimLength1.length; xDim++ ){
			if( xDim > 0 ) sb.append(",");
			sb.append(" " + maiDimLength1[xDim]);
		}
		sb.append(" } \n");
		sb.append("data caption: " + msDataCaption + "\n");
		sb.append("data units: " + msDataUnits + "\n");
		sb.append("slice caption: " + msSliceCaption + "\n");
		if( meggMissing1 != null ){
			sb.append(" missing: {");
			int len;
			switch( meDataType ){
				case DAP.DATA_TYPE_Byte:
					byte[] ab = (byte[])meggMissing1[0];
					len = ab.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + ab[xMissing]);
					break;
				case DAP.DATA_TYPE_Int16:
					short[] ash = (short[])meggMissing1[0];
					len = ash.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + ash[xMissing]);
					break;
				case DAP.DATA_TYPE_Int32:
					int[] ai = (int[])meggMissing1[0];
					len = ai.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + ai[xMissing]);
					break;
				case DAP.DATA_TYPE_UInt16:
					ash = (short[])meggMissing1[0];
					len = ash.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + DAP.toSigned(ash[xMissing]));
					break;
				case DAP.DATA_TYPE_UInt32:
					ai = (int[])meggMissing1[0];
					len = ai.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + DAP.toSigned(ai[xMissing]));
					break;
				case DAP.DATA_TYPE_Float32:
					float[] af = (float[])meggMissing1[0];
					len = af.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + af[xMissing]);
					break;
				case DAP.DATA_TYPE_Float64:
					double[] ad = (double[])meggMissing1[0];
					len = ad.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" " + ad[xMissing]);
					break;
				case DAP.DATA_TYPE_String:
					String[] as = (String[])meggMissing1[0];
					len = as.length;
					for( int xMissing = 1; xMissing < len; xMissing++ ) sb.append(" \"" + as[xMissing] + "\"");
					break;
			}
			sb.append(" } ");
		}
		return sb.toString();
	}

}
