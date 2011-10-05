package opendap.clients.odc.plot;

import java.awt.GridBagConstraints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Panel_PlotAxes extends JPanel {
	Panel_Axis panelX;
	Panel_Axis panelY;
	Panel_PlotAxes(){

		panelX = new Panel_Axis();
		panelY = new Panel_Axis();

		this.setLayout(new java.awt.GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = gbc.BOTH;

		// top margin
		gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.gridwidth = 3; // column 1, gutter, 2
		this.add(Box.createVerticalStrut(4), gbc);

		// x-axis
		JLabel labelX = new JLabel("X-Axis");
		labelX.setFont(opendap.clients.odc.gui.Styles.fontSansSerif14);
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(new JLabel(), gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(Box.createVerticalStrut(3), gbc);
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(panelX, gbc);

		// separator
		gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.0; gbc.weighty = 1; gbc.gridwidth = 1;
		this.add(Box.createHorizontalStrut(2), gbc);

		// y-axis
		JLabel labelY = new JLabel("Y-Axis");
		labelY.setFont(opendap.clients.odc.gui.Styles.fontSansSerif14);
		gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(new JLabel(), gbc);
		gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(Box.createVerticalStrut(3), gbc);
		gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(panelY, gbc);
	}
	public Panel_Axis getAxisParameters_X(){ return this.panelX; }
	public Panel_Axis getAxisParameters_Y(){ return this.panelY; }
}

class Panel_Axis extends JPanel {
	javax.swing.JTextField mjtfOffset = new javax.swing.JTextField();
	javax.swing.JLabel mlabelIndexSize = new JLabel();
	int mDimSize = 0;
	Panel_Axis(){
		vSetupHandlers();
		vSetupInterface();
	}

	public void vSetDimSize( int i ){
		mDimSize = i;
	}

	public void vUpdateOffsetLabel(){
		if( mDimSize == 0 ){
			mlabelIndexSize.setText( "" );
		} else {
			int iPercentage = getOffset() / mDimSize;
			mlabelIndexSize.setText( iPercentage + "% (dim size: " + mDimSize + ")");
		}
	}

	public int getOffset(){
		try {
			return Integer.parseInt( mjtfOffset.getText() );
		} catch(Throwable t) {
			return 0;
		}
	}

	void vSetupHandlers(){
		mjtfOffset.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						vUpdateOffsetLabel();
					} catch(Exception ex){} // ignore invalid entries
				}
			}
		);
	}

	void vSetupInterface(){
		JPanel panelLines = new JPanel();
		panelLines.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Baseline"));

		JPanel panelTicks = new JPanel();
		panelTicks.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Ticks"));

		JPanel panelLabels = new JPanel();
		panelLabels.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Labels"));

		JPanel PanelOffset = new JPanel();
		PanelOffset.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Offset"));
		PanelOffset.setLayout( new BoxLayout(PanelOffset, BoxLayout.X_AXIS) );
		PanelOffset.add( new JLabel("offset:") );
		PanelOffset.add( Box.createHorizontalStrut(2) );
		PanelOffset.add( mjtfOffset );
		PanelOffset.add( Box.createHorizontalStrut(2) );
		PanelOffset.add( new JLabel("") );

		this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
		this.add(panelLines);
		this.add(Box.createVerticalStrut(2));
		this.add(panelTicks);
		this.add(Box.createVerticalStrut(2));
		this.add(panelLabels);
		this.add(Box.createVerticalStrut(2));
		this.add(PanelOffset);
	}
}




