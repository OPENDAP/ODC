package opendap.clients.odc.plot;

import opendap.clients.odc.gui.FormLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CompositionLayout {

	public enum TYPE {
		Single,
		Gridded,
		Panelled,
		FreeForm
	}

	public static enum SCALE_MODE {
		CompositionFitsWindow,
		FixedSizeComposition,
		FixedSizePlot
	}
	
	private CompositionLayout(){}
	final public static CompositionLayout create(){
		CompositionLayout layout = new CompositionLayout();
		layout.eType = TYPE.Single;
		layout.eScaleMode = SCALE_MODE.CompositionFitsWindow;
		return layout;
	}
	
	private TYPE eType;
	private SCALE_MODE eScaleMode;
	public int iRowCount;
	public int iColumnCount;
	
	TYPE getCompositionType(){ return eType; }
	void setCompositionType( TYPE type ){ eType = type; }

	SCALE_MODE getScaleMode(){ return eScaleMode; }
	void setScaleMode( SCALE_MODE mode ){ eScaleMode = mode; }

	public Dimension getCompositionDimensions(){
		return new Dimension( 0, 0 );
	}
	
	public Rectangle getElementLayout( Plot element ){
		switch( getScaleMode() ){
			case CompositionFitsWindow:
				; break;
			case FixedSizeComposition:
				; break;
			case FixedSizePlot:
				; break;
		}
		Rectangle rect = new Rectangle( 0, 0, 0, 0 );
		return rect;
	}

}

class Panel_CompositionLayout extends JPanel {
	CompositionLayout mCompositionLayout = null;
	JComboBox jcbCompositionType;
	JRadioButton jrbScaleMode_CompositionFitsWindow = new JRadioButton( "Composition Fits Window" ); 
	JRadioButton jrbScaleMode_FixedSizeComposition = new JRadioButton( "Fixed Size Composition" ); 
	JRadioButton jrbScaleMode_FixedSizePlot = new JRadioButton( "Fixed Size Plot" );
	ButtonGroup jbgScaleMode = new ButtonGroup(); 
	final JTextField jtfRows = new JTextField(6);
	final JTextField jtfColumns = new JTextField(6);
	final JButton buttonView = new JButton("View Layout");
	final JPanel panelExemplar = new JPanel();
	Panel_CompositionLayout( ActionListener listenerViewButton ){
		jcbCompositionType = new JComboBox( CompositionLayout.TYPE.values() );
		setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Composition Layout"));
		JLabel labelCompositionType = new JLabel( "Composition Type: " );
		JLabel labelRows = new JLabel("Rows: ");
		JLabel labelColumns = new JLabel("Columns: ");
		jbgScaleMode.add( jrbScaleMode_CompositionFitsWindow );
		jbgScaleMode.add( jrbScaleMode_FixedSizeComposition );
		jbgScaleMode.add( jrbScaleMode_FixedSizePlot );
		vSetupListeners();
		panelExemplar.setPreferredSize( new Dimension( 200, 200 ) );
		FormLayout layout = new FormLayout( this );
		layout.add( labelCompositionType, jcbCompositionType );
		layout.add( null, panelExemplar );
		layout.add( null, jrbScaleMode_CompositionFitsWindow );
		layout.add( null, jrbScaleMode_FixedSizeComposition );
		layout.add( null, jrbScaleMode_FixedSizePlot );
		layout.add( labelRows, jtfRows );
		layout.add( labelColumns, jtfColumns );
	}
	
	CompositionLayout getCompositionLayout(){ return mCompositionLayout; }
	void setCompositionLayout( CompositionLayout layout ){
		mCompositionLayout = layout;
		setFieldsEnabled( layout != null );
		if( layout == null ) return;
		jcbCompositionType.setSelectedIndex( layout.getCompositionType().ordinal() );
		switch( layout.getScaleMode() ){
			case CompositionFitsWindow:
				jrbScaleMode_CompositionFitsWindow.setSelected( true ); break;
			case FixedSizeComposition:
				jrbScaleMode_FixedSizeComposition.setSelected( true ); break;
			case FixedSizePlot:
				jrbScaleMode_FixedSizePlot.setSelected( true ); break;
		}
		drawExemplar( layout );
	}

	void drawExemplar( CompositionLayout  layout ){
		panelExemplar.setBackground( Color.white );
	}
	
	void setFieldsEnabled( boolean z ){
		jcbCompositionType.setEnabled(z);
		jrbScaleMode_CompositionFitsWindow.setEnabled(z);
		jrbScaleMode_FixedSizeComposition.setEnabled(z);
		jrbScaleMode_FixedSizePlot.setEnabled(z);
		jtfRows.setEnabled(z);
		jtfColumns.setEnabled(z);
	}

	private void vSetupListeners(){
		jcbCompositionType.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					int xSelected = jcbCompositionType.getSelectedIndex();
					mCompositionLayout.setCompositionType( CompositionLayout.TYPE.values()[xSelected] );
				}
			}
		);
		jrbScaleMode_CompositionFitsWindow.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					mCompositionLayout.setScaleMode( CompositionLayout.SCALE_MODE.CompositionFitsWindow );
				}
			}
		);
		jrbScaleMode_FixedSizeComposition.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					mCompositionLayout.setScaleMode( CompositionLayout.SCALE_MODE.FixedSizeComposition );
				}
			}
		);
		jrbScaleMode_FixedSizePlot.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					mCompositionLayout.setScaleMode( CompositionLayout.SCALE_MODE.FixedSizePlot );
				}
			}
		);
		jtfRows.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						mCompositionLayout.iRowCount = Integer.parseInt( jtfRows.getText() );
					} catch(Exception ex){
						jtfRows.setText("0");
						mCompositionLayout.iRowCount = 0;
					}
				}
			}
		);
		jtfColumns.addFocusListener(
			new FocusAdapter(){
				public void focusLost(FocusEvent evt) {
					try {
						mCompositionLayout.iColumnCount = Integer.parseInt( jtfColumns.getText() );
					} catch(Exception ex){
						jtfColumns.setText("0");
						mCompositionLayout.iColumnCount = 0;
					}
				}
			}
		);
	}
}

class Test_CompositionLayout extends JFrame {
	Panel_CompositionLayout panelLayout;
	CompositionLayout mCompositionLayout;
	CompositionLayout_Exemplar panelCanvas;
	public Test_CompositionLayout(){
		this( null );
	}
	public Test_CompositionLayout( CompositionLayout layout ){
		if( layout == null ){
			mCompositionLayout = CompositionLayout.create();
		} else {
			mCompositionLayout = layout;
		}
		ActionListener test_button =
			new ActionListener(){
				public void actionPerformed(ActionEvent event) {
					vUpdateCanvas();
				}
			};
		panelLayout = new Panel_CompositionLayout( test_button );
		panelLayout.setCompositionLayout( mCompositionLayout );
		panelCanvas = new CompositionLayout_Exemplar( mCompositionLayout );
		panelCanvas.setPreferredSize(new Dimension(400, 300));
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(panelLayout, BorderLayout.EAST);
		getContentPane().add(panelCanvas, BorderLayout.CENTER);
	}
	void vUpdateCanvas(){
		panelCanvas.repaint();
	}
}

class CompositionLayout_Exemplar extends JPanel {
	public final static boolean DEBUG_Layout = false;
	public final static double TwoPi = 2d * 3.14159d;
	CompositionLayout mCompositionLayout;
	public CompositionLayout_Exemplar( CompositionLayout pl ){
		mCompositionLayout = pl;
	}
	public void paintComponent( Graphics g ){
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.WHITE);
		int iObjectX, iObjectY, iObjectHeight, iObjectWidth;
		switch( mCompositionLayout.getCompositionType() ){
			default:
			case Single:
				iObjectX = 0;
				iObjectY = 0;
				iObjectWidth = 399;
				iObjectHeight = 299;
				g2.drawRect(iObjectX, iObjectY, iObjectWidth, iObjectHeight);
				g2.drawString("Canvas", 250, 30);
				break;
			case Gridded:
				iObjectX = 60;
				iObjectY = 50;
				iObjectWidth = 380;
				iObjectHeight = 200;
				g2.drawRect(iObjectX, iObjectY, iObjectWidth, iObjectHeight);
				g2.drawString("Plot Area", iObjectX + iObjectWidth - 80, iObjectY + 40);
				break;
			case Panelled:
				iObjectX = 120;
				iObjectY = 70;
				iObjectWidth = 240;
				iObjectHeight = 160;
				g2.drawRect(iObjectX, iObjectY, iObjectWidth, iObjectHeight);
				g2.drawString("Legend", iObjectX + iObjectWidth - 80, iObjectY + 40);
				break;
			case FreeForm:
				iObjectX = 250;
				iObjectY = 210;
				iObjectWidth = 60;
				iObjectHeight = 16;
				g2.drawRect(iObjectX, iObjectY, iObjectWidth, iObjectHeight);
				g2.drawString("Scale", iObjectX + 10, iObjectY);
				break;
		}
	}

}
