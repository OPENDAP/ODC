package opendap.clients.odc.plot;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class PreviewPane extends JScrollPane {
	JPanel panelBlank;
	public PreviewPane(){
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		panelBlank = new JPanel();
		panelBlank.setLayout(new BorderLayout());
		panelBlank.add(Box.createGlue(), BorderLayout.CENTER);
		setClear();
	}
	void setClear(){
		setViewportView(panelBlank);
		revalidate();
	}
	void setContent( Component c ){
		setViewportView( c );
		revalidate();
	}
}
