package opendap.clients.odc.plot;

/**
 * Title:        PlotAxis
 * Description:  Stores axis parameters
 * Copyright:    Copyright (c) 2002-4
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.40
 */

import opendap.clients.odc.*;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ActionListener;
import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Color;

public class PlotAxis {

	private boolean mzWholeNumbers;
	private int mDataTYPE = 0;
	private int mctValues = 0;
	private int[] maiValues = null;
	private double[] madValues1 = null;	private double mdValueFrom = 0;
	private double mdValueTo = 0;

	// scale quantities
	private int mpxLength = 0; // the length of the axis in screen units

	// tick quantities
	int mpxLowerOffset, mpxUpperOffset; // the offsets from the ends of the axis where the first and last ticks are located
	double mpxTick_MajorInterval = 0;
	int mpxTick_MediumInterval = 0;
	int mpxTick_MinorInterval = 0;

	// tick attributes
	private boolean mTick_z_show;
	private boolean mTick_z_pointing_out;
	private Color   mTick_color;
	private int     mTick_px_width;

	int getType(){ return mDataTYPE; }
	double[] getValues1(){ return madValues1; }
	void setValues(Object[] eggValues, int eDataTYPE, boolean zYAxis ){
		double[] adValues1;
		int len;
		switch( eDataTYPE ){
			case DAP.DATA_TYPE_Byte:
			case DAP.DATA_TYPE_Int16:
				short[] ash = (short[])eggValues[0];
				len = ash.length;
				adValues1 = new double[len + 1];
				for( int xData = 0; xData < len; xData++ ) adValues1[xData + 1] = (double)ash[xData];
				break;
			case DAP.DATA_TYPE_UInt16:
			case DAP.DATA_TYPE_Int32:
				int[] ai = (int[])eggValues[0];
				len = ai.length;
				adValues1 = new double[len + 1];
				for( int xData = 0; xData < len; xData++ ) adValues1[xData + 1] = (double)ai[xData];
				break;
			case DAP.DATA_TYPE_UInt32:
				long[] an = (long[])eggValues[0];
				len = an.length;
				adValues1 = new double[len + 1];
				for( int xData = 0; xData < len; xData++ ) adValues1[xData + 1] = (double)(an[xData]);
				break;
			case DAP.DATA_TYPE_Float32:
				float[] af = (float[])eggValues[0];
				len = af.length;
				adValues1 = new double[len + 1];
				for( int xData = 0; xData < len; xData++ ) adValues1[xData + 1] = (double)af[xData];
				break;
			case DAP.DATA_TYPE_Float64:
				double[] ad = (double[])eggValues[0];
				len = ad.length;
				adValues1 = new double[len + 1];
				for( int xData = 0; xData < len; xData++ ) adValues1[xData + 1] = (double)ad[xData];
				break;
			case DAP.DATA_TYPE_String: return; // not supported
			default: return;
		}
		mDataTYPE = eDataTYPE;
		madValues1 = adValues1;
		double dMinValue = Double.MAX_VALUE;
		double dMaxValue = Double.MAX_VALUE * -1;
		int mctValues = madValues1.length - 1;
		for( int xValues = 1; xValues <= mctValues; xValues++ ){
			double dValue = madValues1[xValues];
			if( Double.isNaN(dValue) ) continue;
			if( dValue < dMinValue ) dMinValue = dValue;
			if( dValue > dMaxValue ) dMaxValue = dValue;
		}
		setRange( madValues1[1], madValues1[mctValues] );
	}
	void setRange( double dFrom, double dTo ){
		mdValueFrom = dFrom;
		mdValueTo = dTo;
//System.out.println("setting range " + mdValueFrom + " to " + mdValueTo);
	}

	void setIndexed( int iFrom, int iTo ){
		int len = iTo > iFrom ? iTo - iFrom + 1 : iFrom - iTo + 1;
		madValues1 = new double[len + 1];
		if( iTo > iFrom ){
			for( int x = iFrom; x <= iTo; x++ ){
				madValues1[x] = (double)x;
			}
		} else {
			for( int x = iFrom; x <= iTo; x-- ){
				madValues1[x] = (double)x;
			}
		}
		setRange( (double)iFrom, (double)iTo );
		mDataTYPE = DAP.DATA_TYPE_Float64;
	}

	private String msCaption;
	void setCaption( String sCaption ){
		msCaption = sCaption;
	}
	String getCaption(){
		return msCaption;
	}

	private String[] masScaleLabels1 = null;
	String[] getScaleLabels1(){ return masScaleLabels1; }
	private int mctTicks;
	int getTickCount(){ return mctTicks; }

	public String toString(){
		StringBuffer sb = new StringBuffer(80);
		sb.append("Axis:\n");
		sb.append("caption: " + msCaption + "\n");
		sb.append("data type: " + DAP.getType_String(mDataTYPE) + "\n");
		sb.append("whole numbers: " + (mzWholeNumbers ? "Yes" : "No") + "\n");
		sb.append("value count: " + mctValues + "\n");
		sb.append("from: " + mdValueFrom + " to " + mdValueTo + "\n");
		return sb.toString();
	}

	double getValueFrom(){ return mdValueFrom; }
	double getValueTo(){ return mdValueTo; }

	/** The distance from lower bound before the first tick
	 * generated as a side effect of getting the scale interval */
	int getOffset_LowerPX(){ return mpxLowerOffset; }

	/** The distance to the upper bound from the last tick
	 * generated as a side effect of getting the scale interval */
	int getOffset_UpperPX(){ return mpxUpperOffset; }

	// getting the scale interval generates the labels as a side effect
	// sets:
	//   - mpxLength (axis length)
	//   - mpxTick_MajorInterval
	// generates:
	//   - masScaleLabels1
	//   - mpxLowerOffset and mpxUpperOffset
	boolean zDetermineScaleInterval( int pxLength, Font font, boolean zOrthogonalLabels, Graphics g, boolean zDoBiasAdjustment, double dSlope, double dIntercept, StringBuffer sbError ){

		// generate graphics to use
		// java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(10,10,java.awt.image.BufferedImage.TYPE_INT_ARGB);
		// Graphics g = bi.getGraphics();

		// input values
		double dValueFrom, dValueTo;
		if( zDoBiasAdjustment ){
			dValueFrom = mdValueFrom * dSlope + dIntercept;
			dValueTo = mdValueTo * dSlope + dIntercept;
		} else {
			dValueFrom = mdValueFrom;
			dValueTo = mdValueTo;
		}

		// set and validate axis length
		mpxLength = pxLength;
		double dRange = (dValueTo > dValueFrom) ? dValueTo - dValueFrom : dValueFrom - dValueTo;
		if( pxLength == 0 ){
			sbError.append("axis length is zero");
			return false;
		}
		if( dRange == 0 ){
			sbError.append("data range is zero");
			return false;
		}

		int pxLabelLength = pxCalculateLabelLength( g, font, zOrthogonalLabels, dValueFrom, dValueTo );
		pxLabelLength += 3; // add some buffer space

		double dDataInterval = dCalculateDataInterval( pxLabelLength, dRange );
		mpxTick_MajorInterval = (((double)(mpxLength - 1)) * dDataInterval / dRange); // the length is shortened by one because the offset goes from say 0 to 9 if the length is 10
		// calculate offsets and labels
		int iIntervalOrder = (int)(Math.log(dDataInterval) / Math.log(10));
		if( Math.abs(dDataInterval) < 1 ) iIntervalOrder--;
		double dValueFrom_rounded = Utility.round(dValueFrom, iIntervalOrder-1);
		double dValueTo_rounded = Utility.round(dValueTo, iIntervalOrder-1);
		int ctDecimalPlaces = iIntervalOrder >= 0 ? 0 : iIntervalOrder * -1;
		double dIncrement;
		int ctTicks = (int)((double)mpxLength / mpxTick_MajorInterval) + 1;
		masScaleLabels1 = new String[ctTicks + 2]; // one for the one-based array, one for the fencepost
		mctTicks = 0;
		boolean zAscending = dValueTo > dValueFrom;
//		dIncrement = Math.pow(10, iIntervalOrder - 1); // non-rounded numbers
		dIncrement = dDataInterval; // rounded numbers
		double dTick;
		double dAspectRatio = mpxLength / dRange; // pixels per unit range
		if( dValueFrom_rounded % dIncrement == 0 ){
			dTick = dValueFrom_rounded;
			mpxLowerOffset = 0;
		} else {
			if( zAscending ){
				dTick = Utility.round_up(dValueFrom, iIntervalOrder);
//			    dTick = dLowerBound_rounded - Math.pow(10, iIntervalOrder);
//				double dStep = Math.pow(10, iIntervalOrder - 1);
//System.out.println("dtick search from " + dTick + " step: " + dStep);
//				while( dTick < dLowerBound_rounded ) dTick += dStep;
				mpxLowerOffset = (int)((dTick - dValueFrom_rounded) * dAspectRatio);
			} else {
				dTick = Utility.round_down(dValueFrom, iIntervalOrder);
//				dTick = dLowerBound_rounded + Math.pow(10, iIntervalOrder);
//				double dStep = -1 * Math.pow(10, iIntervalOrder - 1);
//System.out.println("dtick search from " + dTick + " step: " + dStep);
//				while( dTick > dLowerBound_rounded ) dTick += dStep;
				mpxLowerOffset = (int)((dValueFrom_rounded - dTick) * dAspectRatio);
			}
		}
		if( !zAscending ) dIncrement *= -1;
		while( (zAscending ? dTick <= dValueTo_rounded : dTick >= dValueTo_rounded ) ){
			mctTicks++;
			masScaleLabels1[mctTicks] = Utility.sDoubleToRoundedString(dTick, ctDecimalPlaces);
			dTick += dIncrement;
		}
		if( dTick > dValueTo_rounded ){
			mpxUpperOffset = (int)((dTick - dValueTo_rounded) * dAspectRatio);
		} else {
			mpxUpperOffset = (int)((dValueTo_rounded - dTick) * dAspectRatio);
		}

		return true;
	}

	// Determine the maximum space each label takes on the axis (pxLabelLength)
	private int pxCalculateLabelLength( Graphics g, Font font, boolean zOrthogonalLabels, double dValueFrom, double dValueTo ){
		java.awt.FontMetrics fm = g.getFontMetrics(font);
		int pxLabelLength;
		if( zOrthogonalLabels ){
			return fm.getHeight();
		} else {
			double diff = dValueTo - dValueFrom; if( diff < 0 ) diff *= -1;
			int iOrder = (int)(Math.log(diff) / Math.log(10));
			String sFrom, sTo;
			if( iOrder >= 0 ){
				sTo = Utility.sDoubleToRoundedString(dValueTo, 0);
				sFrom = Utility.sDoubleToRoundedString(dValueFrom, 0);
			} else {
				int iDecimalPlaces = iOrder * -1;
				sTo = Utility.sDoubleToRoundedString(dValueTo, iDecimalPlaces);
				sFrom = Utility.sDoubleToRoundedString(dValueFrom, iDecimalPlaces);
			}
			int lenTo = fm.stringWidth(sTo);
			int lenFrom = fm.stringWidth(sFrom);
			return lenTo > lenFrom ? lenTo : lenFrom;
		}
	}

	private double dCalculateDataInterval( int pxLabelLength, double dRange ){
		int ctMaxLabels = mpxLength / pxLabelLength;
		double dMinDataInterval = dRange / ctMaxLabels;
		return determineInterval_Natural(dMinDataInterval); // round up to nearest multiple power of 10
	}

	/* the natural interval is the multiple of the lower order bound of the interval
	For example, if the interval is 4 the natural interval is 4; if it is 16 the ni is 20,
	if it is 143 it is 200, if it is 642 it is 700 etc
	*/
	double determineInterval_Natural( double dInterval ){
		int iOrder = (int)(Math.log(dInterval) / Math.log(10));
		double dBase = Math.pow(10, iOrder);
		double dIncrement = Math.pow(10, iOrder);
		double dNaturalInterval = dBase;
		while( dNaturalInterval < dInterval ) dNaturalInterval += dIncrement;
		return dNaturalInterval;
	}

	/* rounds up to the nearest 10-power unless that number is greater than 3x the interval in which
	   case it is the nearest 10-power / 2
	*/
	double determineInterval_TensAndFives( double dInterval ){
		int iOrder = (int)(Math.log(dInterval) / Math.log(10));
		double dTenAndFive = Math.pow(10, iOrder + 1);
		if( dTenAndFive > 3 * dInterval ) dTenAndFive *= 0.5;
		return dTenAndFive;
	}

}

class Panel_PlotAxes extends JPanel {
	Panel_AxisParameters panelX;
	Panel_AxisParameters panelY;
	Panel_PlotAxes(){

		panelX = new Panel_AxisParameters();
		panelY = new Panel_AxisParameters();

		this.setLayout(new java.awt.GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.BOTH;

		// top margin
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 3; // column 1, gutter, 2
		this.add(Box.createVerticalStrut(4), gbc);

		// x-axis
		JLabel labelX = new JLabel("X-Axis");
		labelX.setFont(opendap.clients.odc.Styles.fontSansSerif14);
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(new JLabel(), gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(Box.createVerticalStrut(3), gbc);
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(panelX, gbc);

		// separator
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.0; gbc.weighty = 1; gbc.gridwidth = 1;
		this.add(Box.createHorizontalStrut(2), gbc);


		// x-axis
		JLabel labelY = new JLabel("Y-Axis");
		labelY.setFont(opendap.clients.odc.Styles.fontSansSerif14);
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(new JLabel(), gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(Box.createVerticalStrut(3), gbc);
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(panelY, gbc);
	}
}

class Panel_AxisParameters extends JPanel {
	PlotAxis mPlotAxis = null;
	Panel_AxisParameters(){
		JPanel panelLines = new JPanel();
		panelLines.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Baseline"));

		JPanel panelTicks = new JPanel();
		panelTicks.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Ticks"));

		JPanel panelLabels = new JPanel();
		panelLabels.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Labels"));

		JPanel PanelRuling = new JPanel();
		PanelRuling.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Ruling"));

		this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
		this.add(panelLines);
		this.add(Box.createVerticalStrut(2));
		this.add(panelTicks);
		this.add(Box.createVerticalStrut(2));
		this.add(panelLabels);
		this.add(Box.createVerticalStrut(2));
		this.add(PanelRuling);

	}
}



