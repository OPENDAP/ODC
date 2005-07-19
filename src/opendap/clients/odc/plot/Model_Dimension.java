package opendap.clients.odc.plot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import opendap.clients.odc.*;
import opendap.dap.*;

public class Model_Dimension {
	private String msName;
	private String msCaption = null;
	private boolean mzConstrained = false;
	private int[] maiIndex1 = null; // maps constrained elements to real index numbers
	private int miSize_full;
	private int miSize_constrained;
	private String msConstraint = "";

	Model_Dimension(){
		maiIndex1 = new int[2];
		maiIndex1[1] = 0; // default slice is 0

	}

	void setName( String s ){ msName = s; }
	void setCaption( String s ){ msCaption = s; }
	void setSize( int i ){
		miSize_full = i;
		miSize_constrained =  i; // todo - not implemented
	}
	int getSize_Unconstrained(){ return miSize_full; }

	String getConstraint(){ return msConstraint; }
	boolean isConstrained(){ return mzConstrained; }
	String getName(){ return msName; }
	int getSize(){ return miSize_constrained; }
	int getSliceCount(){ return maiIndex1.length - 1; }
	int getSliceIndex0( int iSliceNumber1 ){
		if( maiIndex1 == null ) return iSliceNumber1 - 1;
		return maiIndex1[iSliceNumber1];
	}
	String getSliceCaption( int iSliceNumber1 ){
		String[] as = Utility.split(msCaption, ',');
		if( as == null ) return null;
		if( iSliceNumber1 < 1 || iSliceNumber1 > as.length ) return null;
		return as[iSliceNumber1 - 1].trim();
	}
	int[] getSliceIndexes1(){ return maiIndex1; }

	void setNoConstraint(){
		msConstraint = null;
		maiIndex1 = null;
		miSize_constrained = miSize_full;
		mzConstrained = false;
	}

	boolean zSetConstraint( String s, StringBuffer sbError ){
		try {
			if( s == null ){
				setNoConstraint();
				return true;
			}
			StringBuffer sbConstraint = new StringBuffer();
			String[] asRanges = Utility.split(s, ',');
			int ctRanges = asRanges.length;
			int xRange = 0;
			for( xRange = 0; xRange < ctRanges; xRange++ ){ asRanges[xRange] = asRanges[xRange].trim(); }

			// count the number of slices
			int ctIndex = 0;
			int iMaxIndex = getSize_Unconstrained() - 1;
			for( xRange = 0; xRange < ctRanges; xRange++ ){
				String[] asBeginEnd;
				if( asRanges[xRange].indexOf(':') >= 0 ){
					asBeginEnd = Utility.split(asRanges[xRange], ':');
				} else {
					asBeginEnd = Utility.split(asRanges[xRange], '-');
				}
				int ctBeginEnd = asBeginEnd.length;
				for( int xBeginEnd = 0; xBeginEnd < ctBeginEnd; xBeginEnd++ ){ asBeginEnd[xBeginEnd] = asBeginEnd[xBeginEnd].trim(); }
				switch ( ctBeginEnd ){
					case 0: // no value - ignore
						break;
					case 1: // single value
						if( asBeginEnd[0].length() == 0 ) asBeginEnd[0] = "0";
						try {
							int iValue = Integer.parseInt(asBeginEnd[0]);
							if( iValue < 1 ){
								ApplicationController.vShowError("Ignoring slice range " + xRange + " [" + asRanges[xRange] + "]; integers less than one not valid");
							} else if( (iValue-1) > iMaxIndex ){
								ApplicationController.vShowError("Ignoring slice range " + xRange + " [" + asRanges[xRange] + "]; number out of range, max " + (iMaxIndex+1) + " (indexes are one-based)");
							} else {
								ctIndex++;
							}
						} catch(Throwable ex) {
							ApplicationController.vShowError("Ignoring range " + xRange + " [" + asRanges[xRange] + "]; unable to interpret as an integer");
						}
						break;
					case 2: // range
						if( asBeginEnd[0].length() == 0 ) asBeginEnd[0] = "1";
						if( asBeginEnd[1].length() == 0 ) asBeginEnd[1] = Integer.toString(iMaxIndex + 1);
						try {
							int iValue1 = Integer.parseInt(asBeginEnd[0]);
							int iValue2 = Integer.parseInt(asBeginEnd[1]);
							if( iValue1 < 1 || iValue2 < 1){
								ApplicationController.vShowError("Ignoring range " + xRange + " [" + asRanges[xRange] + "]; integers less than 1 not valid");
							} else if( iValue1 > (iMaxIndex+1) || iValue2 > (iMaxIndex+1) ){
								ApplicationController.vShowError("Ignoring range " + xRange + " [" + asRanges[xRange] + "]; number out of range, max " + (iMaxIndex+1));
							} else {
								if( iValue1 < iValue2 ){
									ctIndex += iValue2 - iValue1 + 1;
								} else {
									ctIndex += iValue1 - iValue2 + 1;
								}
							}
						} catch(Throwable ex) {
							ApplicationController.vShowError("Ignoring range " + xRange + " [" + asRanges[xRange] + "]; unable to interpret as a range of integers: " + ex);
						}
						break;
					default:
						ApplicationController.vShowError("Ignoring range " + xRange + " [" + asRanges[xRange] + "]; not an integer or range of integers");
				}
			}

			// build the slice index
			maiIndex1 = new int[ctIndex + 1];
			int xSlice1 = 0;
			for( xRange = 0; xRange < ctRanges; xRange++ ){
				String[] asBeginEnd;
				if( asRanges[xRange].indexOf(':') >= 0 ){
					asBeginEnd = Utility.split(asRanges[xRange], ':');
				} else {
					asBeginEnd = Utility.split(asRanges[xRange], '-');
				}
				int ctBeginEnd = asBeginEnd.length;
				for( int xBeginEnd = 0; xBeginEnd < asBeginEnd.length; xBeginEnd++ ){ asBeginEnd[xBeginEnd] = asBeginEnd[xBeginEnd].trim(); }
				switch ( ctBeginEnd ){
					case 0: // no value - ignore
						break;
					case 1: // single value
						if( asBeginEnd[0].length() == 0 ) asBeginEnd[0] = "1";
						try {
							int iValue = Integer.parseInt(asBeginEnd[0]);
							if( iValue > 0  && iValue <= (iMaxIndex+1) ){
								xSlice1++;
								maiIndex1[xSlice1] = iValue-1; // zero-based
								if( sbConstraint.length() > 0 ) sbConstraint.append(',');
								sbConstraint.append(iValue);
							}
						} catch(Throwable ex) {}
						break;
					case 2: // range
						if( asBeginEnd[0].length() == 0 ) asBeginEnd[0] = "1";
						if( asBeginEnd[1].length() == 0 ) asBeginEnd[1] = Integer.toString(iMaxIndex + 1);
						try {
							int iValue1 = Integer.parseInt(asBeginEnd[0]);
							int iValue2 = Integer.parseInt(asBeginEnd[1]);
							if( iValue1 > 0 || iValue2 > 0 && iValue1 <= (iMaxIndex+1) && iValue2 <= (iMaxIndex+1) ){
								if( sbConstraint.length() > 0 ) sbConstraint.append(',');
								sbConstraint.append(iValue1).append('-').append(iValue2);
								if( iValue1 < iValue2 ){
									for( int xValue = iValue1; xValue <= iValue2; xValue++ ){
										xSlice1++;
										maiIndex1[xSlice1] = xValue-1; // real array is zero-based
									}
								} else {
									for( int xValue = iValue1; xValue >= iValue2; xValue-- ){
										xSlice1++;
										maiIndex1[xSlice1] = xValue-1; // real array is zero-based
									}
								}
							}
						} catch(Throwable ex) {
							sbError.append("Ignoring range " + xRange + " [" + asRanges[xRange] + "]; unable to build slice index");
							return false;
						}
						break;
					default:
						sbError.append("Ignoring range " + xRange + " [" + asRanges[xRange] + "]; not an integer or range of integers");
						return false;
				}
			}
			msConstraint = sbConstraint.toString();
		} catch( Throwable ex ) {
			sbError.append("Analyzing constraint [" + s + "]: ");
			Utility.vUnexpectedError(ex, sbError);
			setNoConstraint();
			return false;
		}
		return true;
	}
}

class Panel_Dimension {
	private Model_Dimension mModel = null;
	final JRadioButton com_jrbX = new JRadioButton();
	final JRadioButton com_jrbY = new JRadioButton();
	final JTextField com_jtfConstraint = new JTextField(8);
	final JTextField com_jtfCaption = new JTextField(8);
	Panel_Dimension( DArrayDimension dim ){
		setModel( new Model_Dimension() );
		mModel.setName( dim.getName() );
		mModel.setSize( dim.getSize() );
		com_jrbX.setOpaque(false);
		com_jrbY.setOpaque(false);
		com_jtfConstraint.setOpaque(false);
		com_jtfConstraint.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					setConstraint(com_jtfConstraint.getText());
				}
			}
		);
		com_jtfCaption.setOpaque(false);
	}

	void setModel( Model_Dimension model ){
		mModel = model;
		final Model_Dimension mModel_final = mModel;
		com_jtfCaption.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					mModel_final.setCaption(com_jtfCaption.getText());
				}
			}
		);
	}

	// constraints are in the form "1,3,4-9,10" for example
	void setConstraint( String s ){
		StringBuffer sbError = new StringBuffer();
		if( !mModel.zSetConstraint( s, sbError ) ){
			ApplicationController.vShowError( sbError.toString() );
		}
	}

	Model_Dimension getModel(){ return mModel; }
}
