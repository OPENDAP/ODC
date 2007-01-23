package opendap.clients.odc.plot;

import opendap.clients.odc.ApplicationController;
import java.awt.Font;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.border.*;
import javax.swing.JCheckBox;

/**
The rulers are:
	- PlotBox the box around the plot area
	- ticks associated with the plot box
	- guidelines associated with the plot box
	- axes
	- scale
	- colorbar / legend
*/
public class PlotRulers {

	// graphically defined values for contours
	boolean    option_contour_zShow  = false;
	int        option_contour_hsbColor  = 0;
	Font       option_contour_font = null;
	boolean    option_contour_zAntialias = true;

	// graphically defined values for legend
	boolean    option_legend_zShow  = true;
	int        option_legend_iSize  = 0;
	String     option_legend_sLabel = null;
	final PlotLayout option_legend_Layout = new PlotLayout( PlotLayout.DEFAULT_STYLE_Legend );

	// graphically defined values for scale
	boolean    option_scale_zShow       = false;
	int        option_scale_iXLength    = 0;
	int        option_scale_iRealLength = 0;
	String     option_scale_sLabel      = null;
	final PlotLayout option_scale_Layout      = new PlotLayout( PlotLayout.DEFAULT_STYLE_Scale );

}

class Panel_PlotRulers extends JPanel {

	PlotRulers mPlotRulers = null;

	final Panel_PlotLayout mLegendLayoutPanel = new Panel_PlotLayout(Panel_PlotLayout.MODE_NoLegend, null);
	final Panel_PlotLayout mScaleLayoutPanel = new Panel_PlotLayout(Panel_PlotLayout.MODE_NoScale, null);

	final JCheckBox legend_JCheck = new JCheckBox("Show Legend/Colorbar");
	final JTextField legend_jtfLabel = new JTextField(10);
	final JTextField legend_jtfSize = new JTextField(24);

	final JCheckBox scale_JCheck = new JCheckBox("Show Scale");
	final JTextField scale_jtfLabel = new JTextField(24);
	final JTextField scale_jtfXLength = new JTextField(12);
	final JTextField scale_jtfRealLength = new JTextField(12);

	Panel_PlotRulers(){

		// plot box panel
//		final JPanel panelLegendLabel = new JPanel();
//		final JLabel labelLegendLabel = new JLabel("Label:");
//		panelLegendLabel.setMaximumSize(new java.awt.Dimension(400, 25));
//		panelLegendLabel.setLayout(new BoxLayout(panelLegendLabel, BoxLayout.X_AXIS));
//		panelLegendLabel.add(Box.createHorizontalStrut(10));
//		panelLegendLabel.add(labelLegendLabel);
//		panelLegendLabel.add(Box.createHorizontalStrut(5));
//		panelLegendLabel.add(legend_jtfLabel);
//		final JLabel labelLegendSize = new JLabel("Size:");
//		final JPanel panelLegendSize = new JPanel();
//		panelLegendSize.setMaximumSize(new java.awt.Dimension(400, 25));
//		panelLegendSize.setLayout(new BoxLayout(panelLegendSize, BoxLayout.X_AXIS));
//		panelLegendSize.add(Box.createHorizontalStrut(10));
//		panelLegendSize.add(labelLegendSize);
//		panelLegendSize.add(Box.createHorizontalStrut(5));
//		panelLegendSize.add(legend_jtfSize);
//		final JPanel panelLegend = new JPanel();
//		panelLegend.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Legend/Colorbar"));
//		panelLegend.setLayout(new BoxLayout(panelLegend, BoxLayout.Y_AXIS));
//		final JPanel panelLegend_top = new JPanel();
//		panelLegend_top.setLayout(new BoxLayout(panelLegend_top, BoxLayout.Y_AXIS));
//		panelLegend_top.add(legend_JCheck);
//		panelLegend_top.add(Box.createVerticalStrut(6));
//		panelLegend_top.add(panelLegendLabel);
//		panelLegend_top.add(Box.createVerticalStrut(6));
//		panelLegend_top.add(panelLegendSize);
//		panelLegend.add(panelLegend_top);
//		panelLegend.add(Box.createHorizontalStrut(5));
//		panelLegend.add(mLegendLayoutPanel);

		// panel axes
		final JPanel panelAxes = new JPanel();

		// legend panel
		final JPanel panelLegendLabel = new JPanel();
		final JLabel labelLegendLabel = new JLabel("Label:");
		panelLegendLabel.setMaximumSize(new java.awt.Dimension(400, 25));
		panelLegendLabel.setLayout(new BoxLayout(panelLegendLabel, BoxLayout.X_AXIS));
		panelLegendLabel.add(Box.createHorizontalStrut(10));
		panelLegendLabel.add(labelLegendLabel);
		panelLegendLabel.add(Box.createHorizontalStrut(5));
		panelLegendLabel.add(legend_jtfLabel);
		final JLabel labelLegendSize = new JLabel("Size:");
		final JPanel panelLegendSize = new JPanel();
		panelLegendSize.setMaximumSize(new java.awt.Dimension(400, 25));
		panelLegendSize.setLayout(new BoxLayout(panelLegendSize, BoxLayout.X_AXIS));
		panelLegendSize.add(Box.createHorizontalStrut(10));
		panelLegendSize.add(labelLegendSize);
		panelLegendSize.add(Box.createHorizontalStrut(5));
		panelLegendSize.add(legend_jtfSize);
		final JPanel panelLegend = new JPanel();
		panelLegend.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Legend/Colorbar"));
		panelLegend.setLayout(new BoxLayout(panelLegend, BoxLayout.Y_AXIS));
		final JPanel panelLegend_top = new JPanel();
		panelLegend_top.setLayout(new BoxLayout(panelLegend_top, BoxLayout.Y_AXIS));
		panelLegend_top.add(legend_JCheck);
		panelLegend_top.add(Box.createVerticalStrut(6));
		panelLegend_top.add(panelLegendLabel);
		panelLegend_top.add(Box.createVerticalStrut(6));
		panelLegend_top.add(panelLegendSize);
		panelLegend.add(panelLegend_top);
		panelLegend.add(Box.createHorizontalStrut(5));
		panelLegend.add(mLegendLayoutPanel);

		// scale panel
		final JPanel panelScaleUnits = new JPanel();
		panelScaleUnits.setMaximumSize(new java.awt.Dimension(400, 25));
		final JLabel labelScaleUnits = new JLabel("Label:");
		panelScaleUnits.setLayout(new BoxLayout(panelScaleUnits, BoxLayout.X_AXIS));
		panelScaleUnits.add(Box.createHorizontalStrut(10));
		panelScaleUnits.add(labelScaleUnits);
		panelScaleUnits.add(Box.createHorizontalStrut(5));
		panelScaleUnits.add(scale_jtfLabel);
		final JPanel panelScaleXLength = new JPanel();
		panelScaleXLength.setMaximumSize(new java.awt.Dimension(400, 25));
		final JLabel labelScaleXLength = new JLabel("X-Length:");
		panelScaleXLength.setLayout(new BoxLayout(panelScaleXLength, BoxLayout.X_AXIS));
		panelScaleXLength.add(Box.createHorizontalStrut(10));
		panelScaleXLength.add(labelScaleXLength);
		panelScaleXLength.add(Box.createHorizontalStrut(5));
		panelScaleXLength.add(scale_jtfXLength);
		final JPanel panelScaleRealLength = new JPanel();
		panelScaleRealLength.setMaximumSize(new java.awt.Dimension(400, 25));
		final JLabel labelScaleRealLength = new JLabel("Real Length:");
		panelScaleRealLength.setLayout(new BoxLayout(panelScaleRealLength, BoxLayout.X_AXIS));
		panelScaleRealLength.add(Box.createHorizontalStrut(10));
		panelScaleRealLength.add(labelScaleRealLength);
		panelScaleRealLength.add(Box.createHorizontalStrut(5));
		panelScaleRealLength.add(scale_jtfRealLength);
		JPanel panelScale = new JPanel();
		panelScale.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scale"));
		panelScale.setLayout(new BoxLayout(panelScale, BoxLayout.Y_AXIS));
		JPanel panelScale_top = new JPanel();
		panelScale_top.setLayout(new BoxLayout(panelScale_top, BoxLayout.Y_AXIS));
		panelScale_top.add(scale_JCheck);
		panelScale_top.add(Box.createVerticalStrut(6));
		panelScale_top.add(panelScaleUnits);
		panelScale_top.add(Box.createVerticalStrut(6));
		panelScale_top.add(panelScaleXLength);
		panelScale_top.add(Box.createVerticalStrut(6));
		panelScale_top.add(panelScaleRealLength);
		panelScale.add(panelScale_top);
		panelScale.add(Box.createHorizontalStrut(5));
		panelScale.add(mScaleLayoutPanel);
		vSetupListeners();

		// Do layout
		this.setLayout(new java.awt.GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.BOTH;

		// top margin
		gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.0; gbc.weighty = 0.0;
		gbc.gridwidth = 3; gbc.gridheight = 1;
		add(Box.createVerticalStrut(10), gbc);

		// plot box / axes
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridwidth = 1;
		add(panelAxes, gbc);

		// legend
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridwidth = 1;
		add(panelLegend, gbc);

		// scale
		gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1;
		gbc.gridwidth = 1;
		add(panelScale, gbc);

		// bottom margin
		gbc.gridy = 2; gbc.gridx = 0; gbc.weightx = 0.0; gbc.weighty = 0.0;
		gbc.gridwidth = 3; gbc.gridheight = 1;
		add(Box.createVerticalStrut(10), gbc);

		setEnabled( false );
	}

	PlotRulers getPlotOptions(){ return mPlotRulers; }

	void setPlotRulers( PlotRulers pr ){
		mPlotRulers = pr;
		if( pr == null ){
			mLegendLayoutPanel.setPlotLayout(null);
			mScaleLayoutPanel.setPlotLayout(null);
			setEnabled( false );
		} else {
//			mLegendLayoutPanel.setPlotLayout(mPlotRulers.option_legend_Layout);
//			mScaleLayoutPanel.setPlotLayout(mPlotRulers.option_scale_Layout);
			setEnabled( true );
//			legend_JCheck.setSelected( pr.option_legend_zShow ); // todo add other fixed options
		}
	}

	public void setEnabled( boolean z ){
		mLegendLayoutPanel.setFieldsEnabled(z);
		mScaleLayoutPanel.setFieldsEnabled(z);
		legend_JCheck.setEnabled(z);
		legend_jtfLabel.setEnabled(z);
		legend_jtfSize.setEnabled(z);
		scale_JCheck.setEnabled(z);
		scale_jtfLabel.setEnabled(z);
		scale_jtfXLength.setEnabled(z);
		scale_jtfRealLength.setEnabled(z);
	}

	private void vSetupListeners(){
		legend_JCheck.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					mPlotRulers.option_legend_zShow = legend_JCheck.isSelected();
				}
			}
		);
		legend_jtfLabel.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					mPlotRulers.option_legend_sLabel = legend_jtfLabel.getText();
				}
			}
		);
		legend_jtfSize.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					String sText = legend_jtfSize.getText();
					int iSize;
					if( sText.length() == 0 ){
						mPlotRulers.option_legend_iSize = 0;
						return;
					}
					try {
						iSize = Integer.parseInt(sText);
					} catch(Exception ex) {
						ApplicationController.vShowWarning("unable to interpret legend size (" + sText + ") as a non-negative integer");
						legend_jtfSize.setText( Integer.toString(mPlotRulers.option_legend_iSize) );
						return;
					}
					if( iSize < 0 ){
						ApplicationController.vShowWarning("legend size must be non-negative");
						legend_jtfSize.setText( Integer.toString(mPlotRulers.option_legend_iSize) );
						return;
					}
					mPlotRulers.option_legend_iSize = iSize;
				}
			}
		);
		scale_JCheck.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					mPlotRulers.option_scale_zShow = scale_JCheck.isSelected();
				}
			}
		);
		scale_jtfLabel.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					mPlotRulers.option_scale_sLabel = scale_jtfLabel.getText();
				}
			}
		);
		scale_jtfXLength.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					String sText = scale_jtfXLength.getText();
					int iValue;
					if( sText.length() == 0 ){
						mPlotRulers.option_scale_iXLength = 0;
						return;
					}
					try {
						iValue = Integer.parseInt(sText);
					} catch(Exception ex) {
						ApplicationController.vShowWarning("unable to interpret scale x-length (" + sText + ") as a non-negative integer");
						scale_jtfXLength.setText( Integer.toString(mPlotRulers.option_scale_iXLength) );
						return;
					}
					if( iValue < 0 ){
						ApplicationController.vShowWarning("scale x-length must be non-negative");
						scale_jtfXLength.setText( Integer.toString(mPlotRulers.option_scale_iXLength) );
						return;
					}
					mPlotRulers.option_scale_iXLength = iValue;
				}
			}
		);
		scale_jtfRealLength.addFocusListener(
			new java.awt.event.FocusAdapter(){
				public void focusLost(java.awt.event.FocusEvent evt) {
					String sText = scale_jtfRealLength.getText();
					int iValue;
					if( sText.length() == 0 ){
						mPlotRulers.option_scale_iRealLength = 0;
						return;
					}
					try {
						iValue = Integer.parseInt(sText);
					} catch(Exception ex) {
						ApplicationController.vShowWarning("unable to interpret scale real length (" + sText + ") as a non-negative integer");
						scale_jtfRealLength.setText( Integer.toString(mPlotRulers.option_scale_iRealLength) );
						return;
					}
					if( iValue < 0 ){
						ApplicationController.vShowWarning("scale real length must be non-negative");
						scale_jtfRealLength.setText( Integer.toString(mPlotRulers.option_scale_iRealLength) );
						return;
					}
					mPlotRulers.option_scale_iRealLength = iValue;
				}
			}
		);

	}
}

