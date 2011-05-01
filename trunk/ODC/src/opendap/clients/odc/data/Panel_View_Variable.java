package opendap.clients.odc.data;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.SoftBevelBorder;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Utility;
import opendap.clients.odc.Utility_String;
import opendap.clients.odc.DAP.DAP_VARIABLE;

import  org.python.core.PyObject;

// depending on mode/selected variable type
// draw on canvas
// on click/double click events
// right click to resize stuff
// be able to record and store presentation info in class below
public class Panel_View_Variable extends JPanel implements java.awt.event.ComponentListener, java.awt.event.FocusListener {
	private Model_ExpressionHistory mOneLiners;
	JPanel panelArray_Command;
	Panel_Edit_Cell panelArray_CellEditor;
	Panel_Edit_ViewArray panelArray_View;
	JTextField jtfArray_row;
	JTextField jtfArray_column;
	JTextField jtfArray_value;
	JComboBox jcbArray_exp_text;
	JComboBox jcbArray_exp_mode;
	Node_Array nodeActive;
	JSplitPane mSplitPane;     // this panel is in the lower half of this split pane
	final ClickableButton buttonRow = new ClickableButton( "r:" );
	final JLabel labelColumn = new JLabel( "c:" );
	final JLabel labelValue = new JLabel( "value:" );
	final JLabel labelExp = new JLabel( "exp:" );
	private boolean mzShowingSelectionCoordinates = true;
	private StringBuffer sbError_local = new StringBuffer( 256 );
	private Panel_View_Variable(){}
	public static Panel_View_Variable _create( JSplitPane jsp, StringBuffer sbError ){
		final Panel_View_Variable panel = new Panel_View_Variable();
		panel.mSplitPane = jsp;
				
		// set up command panel for array viewer
		panel.jtfArray_row = new JTextField();
		panel.jtfArray_column = new JTextField();
		panel.jtfArray_value = new JTextField();
		panel.jcbArray_exp_text = new JComboBox();
		String[] asExpressionRangeModes = { "All", "Value", "Selection", "Command", "View" };
		panel.jcbArray_exp_mode = new JComboBox( asExpressionRangeModes );
		panel.jcbArray_exp_mode.setEditable( false );
		panel.labelColumn.setHorizontalAlignment( JLabel.RIGHT );
//		panel.labelY.setBorder( BorderFactory.createLineBorder( Color.BLUE ) );
		panel.labelValue.setHorizontalAlignment( JLabel.RIGHT );
//		panel.labelValue.setBorder( BorderFactory.createLineBorder( Color.BLUE ) );
		panel.buttonRow.setHorizontalAlignment( JLabel.CENTER );
		panel.buttonRow.addMouseListener( panel.buttonRow );
		panel.panelArray_Command = new JPanel();
		panel.panelArray_Command.setPreferredSize( new Dimension( 400, 30 ) );
		panel.panelArray_Command.setLayout( null );
		panel.panelArray_Command.add( panel.buttonRow );
		panel.panelArray_Command.add( panel.jtfArray_row );
		panel.panelArray_Command.add( panel.labelColumn );
		panel.panelArray_Command.add( panel.jtfArray_column );
		panel.panelArray_Command.add( panel.labelValue );
		panel.panelArray_Command.add( panel.jtfArray_value );
		panel.panelArray_Command.add( panel.labelExp );
		panel.panelArray_Command.add( panel.jcbArray_exp_text );
		panel.panelArray_Command.add( panel.jcbArray_exp_mode );
		
		// set up one liner combo
		panel.mOneLiners = Model_ExpressionHistory._create( panel );
		panel.jcbArray_exp_text.setEditable( true );
		panel.jcbArray_exp_text.setModel( panel.getExpressionHistory() );
		panel.jcbArray_exp_text.addItemListener(
			new java.awt.event.ItemListener(){
				public void itemStateChanged( ItemEvent event ){
//					System.out.println( "item state changed exp text" );
//					if( mParent.mzAddingItemToList ) return; // item is being programmatically added
//					if( event.getStateChange() == ItemEvent.SELECTED ){
//					System.out.println( "...selected" );
//					} else if( event.getStateChange() == ItemEvent.DESELECTED ){
//					System.out.println( "...deselected" );
//					} else if( event.getStateChange() == ItemEvent.ITEM_FIRST ){
//					System.out.println( "...first" );
//					} else if( event.getStateChange() == ItemEvent.ITEM_LAST ){
//					System.out.println( "...last" );
//						Model_Dataset modelSelected = (Model_Dataset)event.getItem();
//						mParent._vActivate( modelSelected );
//					}
				}
			}
		);
		panel.jcbArray_exp_text.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent e ){
//					System.out.println( "action performed: " + e.getActionCommand() );					
            }});
		/**
	      Enter             execute the one liner
	      Shift+Enter       execute the one-liner and save it to the history
	      Ctrl+Enter        save the one-liner to the history, do not exec it
	      Ctrl+Shift+Enter  replace the selected expression, with the newly edited one
	    */
		Component editorExpCombo = panel.jcbArray_exp_text.getEditor().getEditorComponent();
		editorExpCombo.addKeyListener(
			new java.awt.event.KeyListener(){
				public void keyReleased( KeyEvent ke ){
					int iModifiersEx = ke.getModifiersEx();
					Object oEditBox = ke.getSource();
					if( ! (oEditBox instanceof JTextField) ){
						ApplicationController.vShowError( "internal error, combo box editor was not a JTextField" );
						return;
					}
					JTextField jtfEditor = (JTextField)oEditBox;
					String sText = jtfEditor.getText();
					String sScript = sText;
//					String sMode = ((javax.swing.text.JTextComponent)panel.jcbArray_exp_mode.getEditor().getEditorComponent()).getText();
					String sMode = panel.jcbArray_exp_mode.getSelectedItem().toString();
					Node node = panel.nodeActive;
					switch( ke.getKeyCode() ){
						case KeyEvent.VK_ENTER:
							if( sText == null || sText.length() == 0 ){
								// combo box editor is blank
								return;
							}
							if( (iModifiersEx & KeyEvent.SHIFT_DOWN_MASK) == 0 ){
								if(( iModifiersEx & KeyEvent.CTRL_DOWN_MASK) == 0 ){ // plain enter, exec only
									panel._executeOneLiner(sScript, sMode, node);
								} else { // Ctrl only: save, but do not exec
									panel.mOneLiners._addExpression( sText );
								}
							} else {
								if( (iModifiersEx & KeyEvent.CTRL_DOWN_MASK) == 0 ){ // Shift only, add and exec
									panel.mOneLiners._addExpression( sText );
									panel._executeOneLiner(sScript, sMode, node);
								} else { // Shift+Ctrl
									panel.mOneLiners._replaceSelectedExpression( sText );
								}
							}
							break;
						case KeyEvent.VK_DELETE:
							panel.mOneLiners._removeSelectedExpression();
							break;
					}
					// ke.consume(); // consume the event (don't let other components be triggered by these events)
				}
				public void keyPressed( KeyEvent ke ){}
				public void keyTyped( KeyEvent ke ){}
			});

		
		// set up array viewer
		panel.panelArray_View = Panel_Edit_ViewArray._create( panel, sbError );
		if( panel.panelArray_View ==  null ){
			sbError.insert( 0, "failed to create array viewer" );
			return null;
		}
//		javax.swing.plaf.basic.BasicSplitPaneUI jspUI = (javax.swing.plaf.basic.BasicSplitPaneUI)panel.mSplitPane.getUI(); 
//		jspUI.getDivider().addComponentListener( panel.panelArray_View ); // listens for movements of the split pane

		panel.setLayout( new BorderLayout() );
		panel.add( panel.panelArray_Command, BorderLayout.NORTH );
		panel.add( panel.panelArray_View, BorderLayout.CENTER );
		panel.addComponentListener( panel );
		panel.addFocusListener( panel );
		
		panel.jtfArray_row.addActionListener(			
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event) {
					try {
						int xNewRow = Integer.parseInt( panel.jtfArray_row.getText() );
						if( xNewRow < 0 ) xNewRow = 0;
						if( xNewRow >= panel.nodeActive._getRowCount() ) xNewRow = panel.nodeActive._getRowCount() - 1;
						panel._setCursor( xNewRow, panel.nodeActive._view.cursor_column );
					} catch( NumberFormatException ex ) {
						ApplicationController.vShowError_NoModal( "Unable to intepret new row number [" + panel.jtfArray_row.getText() + "] as an integer" );
						panel.jtfArray_row.setText( String.valueOf( panel.nodeActive._view.cursor_row ) );
					} catch( Throwable t ){
						ApplicationController.vUnexpectedError( t, "While setting selection coordinate for x" );
					}
				}
			}
		);

		panel.jtfArray_column.addActionListener(			
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event) {
					try {
							int xNewColumn = Integer.parseInt( panel.jtfArray_column.getText() );
						if( panel.nodeActive._getColumnCount() == 0 ){ // if this is a vector there are no columns
							xNewColumn = 0; // for a vector the column index is always zero
						} else {
							if( xNewColumn < 0 ) xNewColumn = 0;
							if( xNewColumn >= panel.nodeActive._getColumnCount() ) xNewColumn = panel.nodeActive._getColumnCount() - 1;
						}
						panel._setCursor( panel.nodeActive._view.cursor_row, xNewColumn );
					} catch( NumberFormatException ex ) {
						ApplicationController.vShowError_NoModal( "Unable to intepret new Column number [" + panel.jtfArray_column.getText() + "] as an integer" );
						panel.jtfArray_row.setText( String.valueOf( panel.nodeActive._view.cursor_column ) );
					} catch( Throwable t ){
						ApplicationController.vUnexpectedError( t, "While setting selection coordinate for y" );
					}
				}
			}
		);

		panel.jtfArray_value.addActionListener(			
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event) {
					panel.sbError_local.setLength( 0 );
					if( panel._setSelectedValue( panel.jtfArray_value.getText(), panel.sbError_local ) ){
						panel.panelArray_View._vDrawImage( panel.nodeActive );
					} else {
						panel.jtfArray_value.setText( panel.nodeActive._getValueString_cursor() );
					}
				}
			}
		);
		
		return panel;
	}

	public Model_ExpressionHistory getExpressionHistory(){ return mOneLiners; }
	
	private class ClickableButton extends JLabel implements java.awt.event.MouseListener {
		Border borderX_up = new SoftBevelBorder( SoftBevelBorder.RAISED );
		Border borderX_down = new SoftBevelBorder( SoftBevelBorder.LOWERED );
		public ClickableButton( String sLabel ){
			super( sLabel );
			setBorder( borderX_up );
		}
		public void mouseReleased( java.awt.event.MouseEvent e ){
			setBorder( borderX_up );
		}
		public void mouseEntered( java.awt.event.MouseEvent e ){}
		public void mouseExited( java.awt.event.MouseEvent e ){}
		public void mousePressed( java.awt.event.MouseEvent e ){
			setBorder( borderX_down );
		}
		public void mouseClicked( java.awt.event.MouseEvent e ){
			if( mzShowingSelectionCoordinates ){
				labelColumn.setVisible( false );
				jtfArray_row.setVisible( false );
				jtfArray_column.setVisible( false );
				mzShowingSelectionCoordinates = false;
			} else {
				labelColumn.setVisible( true );
				jtfArray_row.setVisible( true );
				jtfArray_column.setVisible( true );
				mzShowingSelectionCoordinates = true;
			}
			_resize();
		}
	}
	
	private void _resize(){
		int posSelection_x = 6;
		int posSelection_y = 4;
		int pxLabelMargin = 3;
		int pxButtonX_width = 20;
		int pxSelectionJTF_width = 80;
		int pxLabelY_width = 15;
		int pxLabelValue_width = 38;
		int pxExpValue_width = 25;
		buttonRow.setBounds( posSelection_x, posSelection_y + 2, pxButtonX_width, 20 );
		jtfArray_row.setBounds( posSelection_x + pxButtonX_width + pxLabelMargin, posSelection_y, pxSelectionJTF_width, 25 );
		int posYLabel_x = posSelection_x + pxButtonX_width + pxLabelMargin + pxSelectionJTF_width + 2;
		labelColumn.setBounds( posYLabel_x, posSelection_y, pxLabelY_width, 22 );
		jtfArray_column.setBounds( posYLabel_x + pxLabelY_width + pxLabelMargin, posSelection_y, pxSelectionJTF_width, 25 );
		int pxSelection_width;   
		if( mzShowingSelectionCoordinates ){
			pxSelection_width = pxButtonX_width + pxLabelMargin + pxSelectionJTF_width *2 + 2 + pxLabelY_width + 6;   
		} else {
			pxSelection_width = pxButtonX_width + 2;
		}
		
		// value editor
		int pxValueEditor_x = posSelection_x + pxSelection_width + 2;
		labelValue.setBounds( pxValueEditor_x, posSelection_y, pxLabelValue_width, 22 );
		int posJTFArray_x = pxValueEditor_x + pxLabelValue_width + pxLabelMargin;
		int pxJTFArray_width = 160; 
		jtfArray_value.setBounds( posJTFArray_x, posSelection_y, pxJTFArray_width, 25 );
		int pxValueEditor_width = pxLabelValue_width + pxLabelMargin + pxJTFArray_width; 
		
		// expression one-liner and mode combo
		int pxExpressionEditor_x = pxValueEditor_x + pxValueEditor_width + 2;
		labelExp.setBounds( pxExpressionEditor_x, posSelection_y, pxExpValue_width, 22 );
		int posJTFExp_x = pxExpressionEditor_x + pxExpValue_width;
		int pxRangeModeCombo_width = 100;
		int pxRightHandMargin = 6;
		int pxJTFexp_width = this.getWidth() - posJTFExp_x - pxRangeModeCombo_width - pxRightHandMargin;
		jcbArray_exp_text.setBounds( posJTFExp_x, posSelection_y, pxJTFexp_width, 25 );
		jcbArray_exp_mode.setBounds( posJTFExp_x + pxJTFexp_width + 3, posSelection_y, pxRangeModeCombo_width, 25 );
	}
	
	public void componentHidden( ComponentEvent e ){ _resize(); }
	public void componentMoved( ComponentEvent e ){ _resize(); }
	public void componentResized( ComponentEvent e ){ _resize(); }
	public void componentShown( ComponentEvent e ){ _resize(); }

	public static final Border borderBlue = BorderFactory.createLineBorder( Color.BLUE ); 
	public void focusGained( java.awt.event.FocusEvent e ){
		setBorder( borderBlue );
	}

	public void focusLost( java.awt.event.FocusEvent e ){
		setBorder( null );
	}
	
	public void _show( Node node ){
		if( node == null ){
			setVisible( false );
			return;
		}
		if( node.getType() == DAP_VARIABLE.Array ){
			setVisible( true );
			nodeActive = (Node_Array)node;
			_setCursor( nodeActive._view.cursor_row, nodeActive._view.cursor_column );
			return;
		} else {
			setVisible( false );
			return;
		}
	}
	void _setCursor( int row, int column ){
		if( nodeActive == null ) return;
		if( row > nodeActive._getRowCount() - 1 ) row = nodeActive._getRowCount() - 1;  
		if( column > nodeActive._getColumnCount() - 1 ) column = nodeActive._getColumnCount() - 1;  
		if( row < 0 ) row = 0;
		if( column < 0 ) column = 0;
		int xRow_origin_offset = nodeActive._view.cursor_row - nodeActive._view.origin_row; 
		int xColumn_origin_offset = nodeActive._view.cursor_column - nodeActive._view.origin_column; 
		if( row > panelArray_View.xLastFullRow || row < nodeActive._view.origin_row ){
			nodeActive._view.origin_row = row - xRow_origin_offset;
			if( nodeActive._view.origin_row < 0 ) nodeActive._view.origin_row = 0;
		}
		if( column > panelArray_View.xLastFullColumn || column < nodeActive._view.origin_column ){
			nodeActive._view.origin_column = column - xColumn_origin_offset;
			if( nodeActive._view.origin_column < 0 ) nodeActive._view.origin_column = 0;
		}
		nodeActive._view.cursor_row = row;
		nodeActive._view.cursor_column = column;
		jtfArray_row.setText( Integer.toString( row ) );
		jtfArray_column.setText( Integer.toString( column ) );
		jtfArray_value.setText( nodeActive._getValueString( row, column ) );
		panelArray_View._vDrawImage( nodeActive );
	}
	
	void _setCursorUp(){
		_selectCell( nodeActive._view.cursor_row - 1, nodeActive._view.cursor_column );
		_setCursor( nodeActive._view.cursor_row - 1, nodeActive._view.cursor_column );
	}

	void _setCursorDown(){
		_selectCell( nodeActive._view.cursor_row + 1, nodeActive._view.cursor_column );
		_setCursor( nodeActive._view.cursor_row + 1, nodeActive._view.cursor_column );
	}

	void _setCursorLeft(){
		_selectCell( nodeActive._view.cursor_row, nodeActive._view.cursor_column - 1 );
		_setCursor( nodeActive._view.cursor_row, nodeActive._view.cursor_column - 1 );
	}

	void _setCursorRight(){
		_selectCell( nodeActive._view.cursor_row, nodeActive._view.cursor_column + 1 );
		_setCursor( nodeActive._view.cursor_row, nodeActive._view.cursor_column + 1 );
	}

	void _setCursorSliceUp(){
		ApplicationController.vShowWarning( "slice up not implemented" );
	}

	void _setCursorSliceDown(){
		ApplicationController.vShowWarning( "slice down not implemented" );
	}
	
	void _setCursorPageUp(){
		_selectCell( nodeActive._view.cursor_row - panelArray_View.iPageSize_row, nodeActive._view.cursor_column );
		_setCursor( nodeActive._view.cursor_row - panelArray_View.iPageSize_row, nodeActive._view.cursor_column );
	}

	void _setCursorPageDown(){
		_selectCell( nodeActive._view.cursor_row + panelArray_View.iPageSize_row, nodeActive._view.cursor_column );
		_setCursor( nodeActive._view.cursor_row + panelArray_View.iPageSize_row, nodeActive._view.cursor_column );
	}

	void _setCursorPageLeft(){
		_selectCell( nodeActive._view.cursor_row, nodeActive._view.cursor_column - panelArray_View.iPageSize_column );
		_setCursor( nodeActive._view.cursor_row, nodeActive._view.cursor_column - panelArray_View.iPageSize_column );
	}

	void _setCursorPageRight(){
		_selectCell( nodeActive._view.cursor_row, nodeActive._view.cursor_column + panelArray_View.iPageSize_column );
		_setCursor( nodeActive._view.cursor_row, nodeActive._view.cursor_column + panelArray_View.iPageSize_column );
	}

	void _setCursorPageDiagonalUp(){
		_selectCell( nodeActive._view.cursor_row - panelArray_View.iPageSize_row, nodeActive._view.cursor_column - panelArray_View.iPageSize_column );
		_setCursor( nodeActive._view.cursor_row - panelArray_View.iPageSize_row, nodeActive._view.cursor_column - panelArray_View.iPageSize_column );
	}

	void _setCursorPageDiagonalDown(){
		_selectCell( nodeActive._view.cursor_row + panelArray_View.iPageSize_row, nodeActive._view.cursor_column + panelArray_View.iPageSize_column );
		_setCursor( nodeActive._view.cursor_row + panelArray_View.iPageSize_row, nodeActive._view.cursor_column + panelArray_View.iPageSize_column );
	}

	void _setCursorHome(){
		_selectCell( 0, 0 );
		_setCursor( 0, 0 );
	}

	void _setCursorEnd(){
		_selectCell( nodeActive._getRowCount() - 1, nodeActive._getColumnCount() - 1 );
		_setCursor( nodeActive._getRowCount() - 1, nodeActive._getColumnCount() - 1 );
	}

	void _setCursorAdvance(){
		if( nodeActive._view.selectionUL_row == nodeActive._view.selectionLR_row &&
			nodeActive._view.selectionUL_column == nodeActive._view.selectionLR_column ){
			int xNewColumn = nodeActive._view.cursor_column + 1;
			int xNewRow = nodeActive._view.cursor_row;
			if( xNewColumn >= nodeActive._getColumnCount() ){
				xNewColumn = 0;
				xNewRow++;
				if( xNewRow >= nodeActive._getRowCount() ){
					xNewRow = 0;
				}
			}
			_selectCell( xNewRow, xNewColumn );
			_setCursor( xNewRow, xNewColumn );
		} else {
			int xNewColumn = nodeActive._view.cursor_column + 1;
			int xNewRow = nodeActive._view.cursor_row;
			if( xNewColumn > nodeActive._view.selectionLR_column ){
				xNewColumn = nodeActive._view.selectionUL_column;
				xNewRow++;
				if( xNewRow > nodeActive._view.selectionLR_row ){
					xNewRow = nodeActive._view.selectionUL_row;
				}
			}
			_setCursor( xNewRow, xNewColumn );
		}
	}

	void _setCursorRetreat(){
		if( nodeActive._view.selectionUL_row == nodeActive._view.selectionLR_row &&
			nodeActive._view.selectionUL_column == nodeActive._view.selectionLR_column ){
			int xNewColumn = nodeActive._view.cursor_column - 1;
			int xNewRow = nodeActive._view.cursor_row;
			if( xNewColumn < nodeActive._getColumnCount() ){
				xNewColumn = nodeActive._getColumnCount() - 1;
				xNewRow--;
				if( xNewRow < 0 ){
					xNewRow = 0;
				}
			}
			_selectCell( xNewRow, xNewColumn );
			_setCursor( xNewRow, xNewColumn );
		} else {
			int xNewColumn = nodeActive._view.cursor_column - 1;
			int xNewRow = nodeActive._view.cursor_row;
			if( xNewColumn < nodeActive._view.selectionUL_column ){
				xNewColumn = nodeActive._view.selectionLR_column;
				xNewRow--;
				if( xNewRow < nodeActive._view.selectionUL_row ){
					xNewRow = nodeActive._view.selectionLR_row;
				}
			}
			_setCursor( xNewRow, xNewColumn );
		}
	}

	void _setCursorRotate(){
		if( nodeActive._view.cursor_row == nodeActive._view.selectionUL_row ){
			if( nodeActive._view.cursor_column == nodeActive._view.selectionUL_column ){
				_setCursor( nodeActive._view.selectionUL_row, nodeActive._view.selectionLR_column );
			} else if( nodeActive._view.cursor_column == nodeActive._view.selectionLR_column ){
				_setCursor( nodeActive._view.selectionLR_row, nodeActive._view.selectionLR_column );
			} else {
				_setCursor( nodeActive._view.selectionUL_row, nodeActive._view.selectionUL_column );
			}
		} else if( nodeActive._view.cursor_row == nodeActive._view.selectionLR_row ){ 
			if( nodeActive._view.cursor_column == nodeActive._view.selectionLR_column ){
				_setCursor( nodeActive._view.selectionLR_row, nodeActive._view.selectionUL_column );
			} else if( nodeActive._view.cursor_column == nodeActive._view.selectionUL_column ){
				_setCursor( nodeActive._view.selectionUL_row, nodeActive._view.selectionUL_column );
			} else {
				_setCursor( nodeActive._view.selectionUL_row, nodeActive._view.selectionUL_column );
			}
		} else {
			_setCursor( nodeActive._view.selectionUL_row, nodeActive._view.selectionUL_column );
		}
	}
	
	void _selectCell( int xRow, int xColumn ){
		nodeActive._view.selectionUL_row = xRow;
		nodeActive._view.selectionUL_column = xColumn;
		nodeActive._view.selectionLR_row = xRow;
		nodeActive._view.selectionLR_column = xColumn;
	}
	
	void _selectRow( int xRow ){
		nodeActive._view.selectionUL_row = xRow;
		nodeActive._view.selectionUL_column = 0;
		nodeActive._view.selectionLR_row = xRow;
		nodeActive._view.selectionLR_column = nodeActive._getColumnCount() - 1;
		if( nodeActive._view.selectionLR_column < 0 ) nodeActive._view.selectionLR_column = 0;
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectColumn( int xColumn ){
		nodeActive._view.selectionUL_row = 0;
		nodeActive._view.selectionUL_column = xColumn;
		nodeActive._view.selectionLR_row = nodeActive._getRowCount() - 1;
		nodeActive._view.selectionLR_column = xColumn;
		if( nodeActive._view.selectionLR_row < 0 ) nodeActive._view.selectionLR_row = 0;
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectAll(){
		nodeActive._view.selectionUL_row = 0;
		nodeActive._view.selectionUL_column = 0;
		nodeActive._view.selectionLR_row = nodeActive._getRowCount() - 1;
		nodeActive._view.selectionLR_column = nodeActive._getColumnCount() - 1;
		if( nodeActive._view.selectionLR_column < 0 ) nodeActive._view.selectionLR_column = 0;
		if( nodeActive._view.selectionLR_row < 0 ) nodeActive._view.selectionLR_row = 0;
		panelArray_View._vDrawImage( nodeActive );
	}
	
	void _selectRangeBySize( int xRow, int xColumn, int ctRows, int ctColumns ){
		nodeActive._view.selectionUL_row = xRow;
		nodeActive._view.selectionUL_column = xColumn;
		nodeActive._view.selectionLR_row = xRow + ctRows - 1;
		nodeActive._view.selectionLR_column = xColumn + ctColumns - 1;
		if( nodeActive._view.selectionLR_column < 0 ) nodeActive._view.selectionLR_column = 0;
		if( nodeActive._view.selectionLR_row < 0 ) nodeActive._view.selectionLR_row = 0;
		panelArray_View._vDrawImage( nodeActive );
	}
	
	void _selectRange( int xRow1, int xColumn1, int xRow2, int xColumn2 ){
		if( xRow1 < xRow2 ){
			nodeActive._view.selectionUL_row = xRow1;
			nodeActive._view.selectionLR_row = xRow2;
		} else {
			nodeActive._view.selectionUL_row = xRow2;
			nodeActive._view.selectionLR_row = xRow1;
		}
		if( xColumn1 < xColumn2 ){
			nodeActive._view.selectionUL_column = xColumn1;
			nodeActive._view.selectionLR_column = xColumn2;
		} else {
			nodeActive._view.selectionUL_column = xColumn2;
			nodeActive._view.selectionLR_column = xColumn1;
		}
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectActiveRows(){
		_selectRows( nodeActive._view.selectionUL_row, nodeActive._view.selectionLR_row );  
	}

	void _selectActiveColumns(){
		_selectColumns( nodeActive._view.selectionUL_column, nodeActive._view.selectionLR_column );  
	}
	
	void _selectRows( int xRow1, int xRow2 ){
		if( xRow1 < xRow2 ){
			nodeActive._view.selectionUL_row = xRow1;
			nodeActive._view.selectionUL_column = 0;
			nodeActive._view.selectionLR_row = xRow2;
			nodeActive._view.selectionLR_column = nodeActive._getColumnCount() - 1;
		} else {
			nodeActive._view.selectionUL_row = xRow2;
			nodeActive._view.selectionUL_column = 0;
			nodeActive._view.selectionLR_row = xRow1;
			nodeActive._view.selectionLR_column = nodeActive._getColumnCount() - 1;
		}
		if( nodeActive._view.selectionLR_column < 0 ) nodeActive._view.selectionLR_column = 0; 
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectColumns( int xColumn1, int xColumn2 ){
		if( xColumn1 < xColumn2 ){
			nodeActive._view.selectionUL_row = 0;
			nodeActive._view.selectionUL_column = xColumn1;
			nodeActive._view.selectionLR_row = nodeActive._getRowCount() - 1;
			nodeActive._view.selectionLR_column = xColumn2;
		} else {
			nodeActive._view.selectionUL_row = 0;
			nodeActive._view.selectionUL_column = xColumn2;
			nodeActive._view.selectionLR_row = nodeActive._getRowCount() - 1;
			nodeActive._view.selectionLR_column = xColumn1;
		}
		if( nodeActive._view.selectionLR_row < 0 ) nodeActive._view.selectionLR_row = 0;
		panelArray_View._vDrawImage( nodeActive );
	}
	
	void _selectExtendUp(){
		if( nodeActive._view.selectionLR_row > nodeActive._view.cursor_row ){
			nodeActive._view.selectionLR_row--;
		} else {
			if( nodeActive._view.selectionUL_row == 0 ) return;
			nodeActive._view.selectionUL_row--;
		}
		if( nodeActive._view.selectionUL_row < nodeActive._view.origin_row ){
			nodeActive._view.origin_row = nodeActive._view.selectionUL_row; // when extending the selection, the extended region must always be in view 
		}
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectExtendDown(){
		if( nodeActive._view.selectionLR_row > nodeActive._view.cursor_row || nodeActive._view.selectionLR_row == nodeActive._view.selectionUL_row ){
			if( nodeActive._view.selectionLR_row == nodeActive._getRowCount() - 1 ) return; // at end
			nodeActive._view.selectionLR_row++;
			if( nodeActive._view.selectionLR_row > panelArray_View.xLastFullRow ){
				nodeActive._view.origin_row++; // when extending the selection, the extended region must always be in view 
			}
		} else {
			nodeActive._view.selectionUL_row++;
		}
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectExtendLeft(){
		if( nodeActive._view.selectionLR_column > nodeActive._view.cursor_column ){
			nodeActive._view.selectionLR_column--;
		} else {
			if( nodeActive._view.selectionUL_column == 0 ) return;
			nodeActive._view.selectionUL_column--;
			if( nodeActive._view.selectionUL_column < nodeActive._view.origin_column ){
				nodeActive._view.origin_column = nodeActive._view.selectionUL_column; // when extending the selection, the extended region must always be in view 
			}
		}
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectExtendRight(){
		if( nodeActive._view.selectionLR_column > nodeActive._view.cursor_column || nodeActive._view.selectionLR_column == nodeActive._view.selectionUL_column ){ // right edge moving to right
			if( nodeActive._view.selectionLR_column == nodeActive._getColumnCount() - 1 ) return; // at end
			nodeActive._view.selectionLR_column++;
			if( nodeActive._view.selectionLR_column > panelArray_View.xLastFullColumn ){
				nodeActive._view.origin_column++; // when extending the selection, the extended region must always be in view 
			}
		} else {
			nodeActive._view.selectionUL_column++;
		}
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectExtendPageUp(){
		if( nodeActive._view.selectionLR_row - panelArray_View.iPageSize_row >= nodeActive._view.cursor_row ){
			nodeActive._view.selectionLR_row -= panelArray_View.iPageSize_row;
		} else {
			if( nodeActive._view.selectionUL_row == 0 ) return;
			nodeActive._view.selectionUL_row -= panelArray_View.iPageSize_row;
			if( nodeActive._view.selectionUL_row < 0 ) nodeActive._view.selectionUL_row = 0;
			if( nodeActive._view.selectionUL_row < nodeActive._view.origin_row ){
				nodeActive._view.origin_row = nodeActive._view.selectionUL_row; // when extending the selection, the extended region must always be in view 
			}
		}
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectExtendPageDown(){
		if( nodeActive._view.selectionLR_row > nodeActive._view.cursor_row || nodeActive._view.selectionLR_row == nodeActive._view.selectionUL_row ){
			System.out.println("2");
			nodeActive._view.selectionLR_row += panelArray_View.iPageSize_row;
		} else {
			System.out.println("3");
			nodeActive._view.selectionUL_row += panelArray_View.iPageSize_row;
			if( nodeActive._view.selectionUL_row > nodeActive._view.cursor_row ){
				System.out.println("3.5");
				nodeActive._view.selectionLR_row = nodeActive._view.selectionUL_row - nodeActive._view.cursor_row;
				nodeActive._view.cursor_row = nodeActive._view.cursor_row;
			}
		}
		if( nodeActive._view.selectionLR_row >= nodeActive._getRowCount() ){
			System.out.println("4");
			nodeActive._view.selectionLR_row = nodeActive._getRowCount() - 1;
		}
		if( nodeActive._view.selectionLR_row > panelArray_View.xLastFullRow ){
			System.out.println("5");
			nodeActive._view.origin_row = nodeActive._view.selectionLR_row - panelArray_View.iPageSize_row + 1; // when extending the selection, the extended region must always be in view 
		}
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectExtendPageLeft(){
		if( nodeActive._view.selectionLR_column - panelArray_View.iPageSize_column >= nodeActive._view.cursor_column ){
			nodeActive._view.selectionLR_column -= panelArray_View.iPageSize_column;
		} else {
			if( nodeActive._view.selectionUL_column == 0 ) return;
			nodeActive._view.selectionUL_column -= panelArray_View.iPageSize_column;
			if( nodeActive._view.selectionUL_column < 0 ) nodeActive._view.selectionUL_column = 0;
			if( nodeActive._view.selectionUL_column < nodeActive._view.origin_column ){
				nodeActive._view.origin_column = nodeActive._view.selectionUL_column; // when extending the selection, the extended region must always be in view 
			}
		}
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectExtendPageRight(){
		if( nodeActive._view.selectionLR_column > nodeActive._view.cursor_column || nodeActive._view.selectionLR_column == nodeActive._view.selectionUL_column ){
			nodeActive._view.selectionLR_column += panelArray_View.iPageSize_column;
		} else {
			nodeActive._view.selectionUL_column += panelArray_View.iPageSize_column;
			if( nodeActive._view.selectionUL_column > nodeActive._view.cursor_column ){
				nodeActive._view.selectionLR_column = nodeActive._view.selectionUL_column - nodeActive._view.cursor_column;
				nodeActive._view.cursor_column = nodeActive._view.cursor_column;
			}
		}
		if( nodeActive._view.selectionLR_column >= nodeActive._getColumnCount() ){
			nodeActive._view.selectionLR_column = nodeActive._getColumnCount() - 1;
		}
		if( nodeActive._view.selectionLR_column > panelArray_View.xLastFullColumn ){
			nodeActive._view.origin_column = nodeActive._view.selectionLR_column - panelArray_View.iPageSize_column + 1; // when extending the selection, the extended region must always be in view 
		}
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectExtendPageDiagonalUp(){
		_selectExtendPageUp();
		_selectExtendPageLeft();
	}

	void _selectExtendPageDiagonalDown(){
		_selectExtendPageRight();
		_selectExtendPageDown();
	}

	void _selectExtendHome(){
		nodeActive._view.selectionUL_row = 0;
		nodeActive._view.selectionUL_column = 0;
		nodeActive._view.origin_row = 0;
		nodeActive._view.origin_column = 0;
		panelArray_View._vDrawImage( nodeActive );
	}

	void _selectExtendEnd(){
		nodeActive._view.selectionLR_row = nodeActive._getRowCount() - 1;
		nodeActive._view.selectionLR_column = nodeActive._getColumnCount() - 1;
		nodeActive._view.origin_row = nodeActive._getRowCount() - panelArray_View.iPageSize_row;
		nodeActive._view.origin_column = nodeActive._getColumnCount() - panelArray_View.iPageSize_column;
		panelArray_View._vDrawImage( nodeActive );
	}
	
	boolean _setSelectedValue( String sNewValue, StringBuffer sbError ){
		return nodeActive._setSelectedValue( sNewValue, sbError );
	}

	void _deleteSelectedValues(){
		StringBuffer sbError = new StringBuffer( 256 );
		if( ! nodeActive._deleteAllSelectedValues( sbError ) ){
			ApplicationController.vShowError( "Error deleting selection: " + sbError.toString() );
		}
		panelArray_View._vDrawImage( nodeActive );
		return;
	}

	void _copy(){
		java.awt.datatransfer.Clipboard clipboard = getToolkit().getSystemClipboard ();
		String sClipContent = nodeActive._getValueString_selection();
		java.awt.datatransfer.StringSelection ssContent = new java.awt.datatransfer.StringSelection( sClipContent );  
		clipboard.setContents( ssContent, ssContent );
		ApplicationController.vShowStatus_NoCacheWithTime( nodeActive._getSelectionSize() + " cells copied to clipboard" );
	}
	
	void _paste(){
		String[][] asData = Utility.getClipboardStringArray();
		pasteData( asData );
	}

	// will expand the size of the array to accomodate additional data
	void _pasteExpand(){
		String[][] asData = Utility.getClipboardStringArray();
		if( asData == null ) return;
		int ctRows = asData.length;
		int ctColumns = asData[0].length;
		Node_Array array = (Node_Array)nodeActive;
		StringBuffer sbError = new StringBuffer();
		if( array._view.cursor_row + ctRows > array._getRowCount() ){
			int xDimension1 = array._view.dim_row;
			int ctRows_expanded = array._view.cursor_row + ctRows;
			if( ! array._setDimensionSize( xDimension1, ctRows_expanded, sbError ) ){
				ApplicationController.vShowError( "Error resizing dimension " + xDimension1 + " to " + ctRows_expanded + ": " + sbError );
			}
		}
		if( array._view.cursor_column + ctColumns > array._getColumnCount() ){
			int xDimension1 = array._view.dim_column;
			int ctColumns_expanded = array._view.cursor_column + ctColumns;
			if( ! array._setDimensionSize( xDimension1, ctColumns_expanded, sbError ) ){
				ApplicationController.vShowError( "Error resizing dimension " + xDimension1 + " to " + ctColumns_expanded + ": " + sbError );
			}
		}
		pasteData( asData );
	}

	// pastes data at 0,0 changes size of dimensions to match the pasted data
	void _pasteOverwrite(){
		String[][] asData = Utility.getClipboardStringArray();
		if( asData == null ) return;
		int ctRows = asData.length;
		int ctColumns = asData[0].length;
		Node_Array array = (Node_Array)nodeActive;
		StringBuffer sbError = new StringBuffer();
		if( ctRows != array._getRowCount() ){
			int xDimension1 = array._view.dim_row;
			if( ! array._setDimensionSize( xDimension1, ctRows, sbError ) ){
				ApplicationController.vShowError( "Error resizing dimension " + xDimension1 + " to " + ctRows + ": " + sbError );
			}
		}
		if( ctColumns != array._getColumnCount() ){
			int xDimension1 = array._view.dim_column;
			if( ! array._setDimensionSize( xDimension1, ctColumns, sbError ) ){
				ApplicationController.vShowError( "Error resizing dimension " + xDimension1 + " to " + ctColumns + ": " + sbError );
			}
		}
		_setCursor( 0, 0 );
		pasteData( asData );
	}
	
	private void pasteData( String[][] asData ){
		Node_Array array = (Node_Array)nodeActive;
		int ctCellsModified = 0;
		StringBuffer sbError = new StringBuffer();
		int ctErrors = 0;
		String sError = null;
		for( int xRow = 0; xRow < asData.length; xRow++ ){
			int xRow_paste_offset = nodeActive._view.cursor_row + xRow;
			if( xRow_paste_offset >= nodeActive._getRowCount() ) continue;
			for( int xColumn = 0; xColumn < asData[xRow].length; xColumn++ ){
				int xColumn_paste_offset = nodeActive._view.cursor_column + xColumn;
				if( xColumn_paste_offset >= nodeActive._getColumnCount() ) continue;
				int index = array._getValueIndex( xRow_paste_offset, xColumn_paste_offset );
				if( array._setValue( asData[xRow][xColumn], index, sbError ) ){
					ctCellsModified++;
				} else {
					array._setError( index );
					if( ctErrors == 0 ) sError = sbError.toString();
					sbError.setLength( 0 );
					ctErrors++;
				}
			}
		}
		if( ctErrors > 0 ){
			ApplicationController.vShowError( ctErrors + " errors pasting data: " + sError );
		} else {
			ApplicationController.vShowStatus_NoCacheWithTime( "cells pasted to array: " + nodeActive._getSelectionSize() );
		}
		_selectRangeBySize( nodeActive._view.cursor_row, nodeActive._view.cursor_column, asData.length, asData[0].length );
	}
	
	public boolean _isShowingSelectionCoordinates(){ return mzShowingSelectionCoordinates; } 
	
	public void _executeOneLiner( String sScript, String sMode, opendap.clients.odc.data.Node node ){
		boolean zTest = false;
		int iTestErrorCount = 0;
		boolean zFilterCursor = false;
		boolean zFilterSelection = false;
		boolean zFilterRange = false;
		StringBuffer sbNumber = new StringBuffer();
		StringBuffer sbExpression = new StringBuffer();
		int xDimension = 0;
		int[] actFilters1 = new int[10]; // the number of filters for this particular dimension
		int[][] aiFilterBegin = new int[10][100]; // the begin of the filtered range, -1 means no contraint 
		int[][] aiFilterEnd = new int[10][100]; // the begin of the filtered range, -1 means no contraint
		int posToken = 0; // this is used for error reporting
		int pos = 0;
		int len = sScript.length();
		int state = 1; // start
		String sError = null;
		while( pos < len ){
			char c = sScript.charAt( pos );
			switch( state ){
				case 1: // start
					if( Character.isWhitespace( c ) ) break; // ignore whitespace
					if( c == '#' ){ state = 2; break; } // after hash
					sbExpression.append( c );
					state = 10; //  in expression
					break;
				case 2: // after hash
					if( c == '!' ){ state = 3; break; } // after hash-bang, before directive
					sError = "comment character (#) not followed by '!'; regular comments not allowed in one-liners";
					break;
				case 3: // before directive
					if( c=='[' ){
						state = 4; // after initial range directive bracket
						posToken = pos;
						zFilterRange = true;
						break; } // at beginning of range directive
					if( c=='t' ){
						if( sScript.startsWith( "test", pos ) ){
							posToken = pos;
							pos += "test".length();
							zTest = true;
							state = 5; // after test directive
						} else {
							state = 6; // in informational label
						}
						break; }
					if( c=='c' ){
						if( sScript.startsWith( "cursor", pos ) ){
							pos += "cursor".length();
							zFilterCursor = true;
							state = 7; // after cursor directive
						} else {
							state = 6; // in informational label
						}
						break; }
					if( c=='s' ){
						if( sScript.startsWith( "selection", pos ) ){
							pos += "selection".length();
							zFilterSelection = true;
							state = 8; // after cell directive
						} else {
							state = 6; // in informational label
						}
						break; }
					if( c==';' ){
						state = 10; // in expression
						break; }
					state = 6; // in informational label
					break;
				case 4: // at beginning of range directive
					xDimension++;
					if( c == ']' ){ state = 3; break; } // blank range restriction, after range directive dimension
					if( c == '-' ){ state = 12; break; } // after leading hyphen in range filter
					if( c == '"' ){ state = 13; posToken = pos; break; } // after leading quote in range filter
					if( c == '\'' ){ state = 14; posToken = pos; break; } // after leading single quote in range filter
					if( c == ',' ) break; // ignore commas at the beginning of range
					if( Character.isDigit( c ) ){
						posToken = pos;
						sbNumber.append( 'c' );
						state = 11; // in range directive number
						break;
					}
					if( Character.isWhitespace( c ) ) break; // ignore whitespace
					sError = "unrecognized character '" + c + " in range filter";
					state = 0;
					break;
				case 5: // after test directive
					if( Character.isWhitespace( c ) ) break; // ignore whitespace
					if( Character.isDigit( c ) ){
						sbNumber.append( 'c' );
						state = 15; // in range directive number
						break;
					}
					sError = "unexpected character '" + c + "' found after 'test' directive at " + posToken + ", expected number of errors allowed";
					state = 0;
					break;
				case 6: // in informational label
					if( c == ':' ) state = 1; // begin at start again, label is complete
					break;
				case 7: // after cell directive
				case 8: // after selection directive
					state = 1; // begin at start again
					break;
				case 10: //  in expression
					sbExpression.append( c );
					break;
				case 11: // in range directive number, begin
					if( Character.isWhitespace( c ) || c == '-' || c == ',' || c == ']' ){
						String sNumber = sbNumber.toString();
						try {
							int iNumber = Integer.parseInt( sNumber );
							if( iNumber < 0 ){
								sError = "range restriction at " + posToken + " is negative";
								state = 0;
							} else {
								actFilters1[xDimension]++;
								aiFilterBegin[xDimension][actFilters1[xDimension]] = iNumber;
								if( c == '-' ){
									state = 17; // after hyphen in  range restriction with begin
								} else if( c == ',' ) {
									aiFilterEnd[xDimension][actFilters1[xDimension]] = iNumber;
									state = 4; // at beginning of range directive 
								} else if( c == ']' ) {
									aiFilterEnd[xDimension][actFilters1[xDimension]] = iNumber;
									state = 3; // before directive 
								} else {
									state = 16; // after begin filter in range restriction
								}
							}
						} catch( Throwable t ) {
							sError = "unable to interpret " + sNumber + " as a positive integer";
							state = 0;
						}
						sbNumber.setLength( 0 ); // reset the buffer
						break;
					}
					if( Character.isDigit( c ) ){ sbNumber.append( 'c' ); break; }
					sError = "unexpected character '" + c + "' found in number in range restriction at " + posToken + ", expected a digit or space or a hyphen";
					state = 0;
					break;
				case 12: // after leading hyphen in range directive
					if( Character.isWhitespace( c ) ) break; // ignore whitespace
					if( c == ',' ){ state = 4; break; }// ignore blank restriction
					if( c == ']' ){ state = 3; break; } // blank range restriction, after range directive dimension
					if( Character.isDigit( c ) ){
						actFilters1[xDimension]++;
						aiFilterBegin[xDimension][actFilters1[xDimension]] = -1; // the begin restriction is unconstrained
						sbNumber.append( 'c' );
						state = 18; // in range directive number after hyphen
						break;
					}
					sError = "unexpected character '" + c + "' found after 'test' directive at " + posToken + ", expected number of errors allowed";
					state = 0;
					break;
				case 13: // after leading quote in range filter
				case 14: // after leading single quote in range filter
					if( (c == '"' && state == 13) || (c == '\'' && state == 14) ){
						// named column TODO
						state = 4;
						break;
					}
					sbNumber.append( c );
					break;
				case 15: // in number of test errors
					if( Character.isWhitespace( c ) || c == ';' ){
						String sNumber = sbNumber.toString();
						try {
							int iNumber = Integer.parseInt( sNumber );
							if( iNumber < 0 ){
								sError = "specified number of test errors at " + posToken + " is negative " + sNumber;
								state = 0;
							} else {
								iTestErrorCount = iNumber;
								if( c == ';' ){
									state = 10; // in expression
								} else {
									state = 3; // after begin filter in range restriction
								}
							}
						} catch( Throwable t ) {
							sError = "unable to interpret " + sNumber + " as a positive integer";
							state = 0;
						}
						sbNumber.setLength( 0 ); // reset the buffer
						break;
					}
					if( Character.isDigit( c ) ){ sbNumber.append( 'c' ); break; }
					sError = "unexpected character '" + c + "' found in number for test errors at " + posToken + ", expected a digit or space or a semicolon";
					state = 0;
					break;
				case 16: // after whitespace after begin filter in range restriction
					if( Character.isWhitespace( c ) ) break; // ignore more whitespace
					if( c == ',' || c == ']' ){
						aiFilterEnd[xDimension][actFilters1[xDimension]] = aiFilterBegin[xDimension][actFilters1[xDimension]]; // the end restriction is same as the begin restriction
						if( c == ',' ){ 
							state = 4;
						} else {
							state = 3;
						}
						break; }
					if( c == '-' ){
						state = 17;
						break;
					}
					sError = "unexpected character '" + c + "' found after range restriction at " + posToken + ", multiple restrictions must be separated by commas";
					state = 0;
					break;
				case 17: // after hyphen in  range restriction with a begin
					if( Character.isWhitespace( c ) ) break; // ignore whitespace
					if( c == ',' || c == ']' ){
						aiFilterEnd[xDimension][actFilters1[xDimension]] = -1; // the end restriction is unconstrained
						if( c == ']' ){ 
							state = 3; 
						} else {
							state = 4;
						}
						break;
					}
					if( Character.isDigit( c ) ){
						sbNumber.append( 'c' );
						state = 18; // in range directive number after hyphen
						break;
					}
					sError = "unexpected character '" + c + "' found after hyphen in range restriction at " + posToken;
					state = 0;
					break;
				case 18: // in number after range directive hyphen (end)
					if( Character.isWhitespace( c ) || c == ',' || c == ']' ){
						String sNumber = sbNumber.toString();
						try {
							int iNumber = Integer.parseInt( sNumber );
							if( iNumber < 0 ){
								sError = "end range restriction at " + posToken + " is negative";
								state = 0;
							} else {
								aiFilterEnd[xDimension][actFilters1[xDimension]] = iNumber;
								if( c == ',' ) {
									state = 4; // at beginning of range directive 
								} else if( c == ']' ) {
									state = 3; // before directive 
								} else {
									state = 19; // after end filter in range restriction
								}
							}
						} catch( Throwable t ) {
							sError = "unable to interpret " + sNumber + " as a positive integer";
							state = 0;
						}
						sbNumber.setLength( 0 ); // reset the buffer
						break;
					}
					if( Character.isDigit( c ) ){ sbNumber.append( 'c' ); break; }
					sError = "unexpected character '" + c + "' found in number in end range restriction at " + posToken + ", expected a digit or space or a bracket";
					state = 0;
					break;
				case 19: // after end constraint (whitespace) in range restriction
					if( Character.isWhitespace( c ) ) break; // ignore whitespace
					if( c == ',' ){ state = 4; break; }
					if( c == ']' ){ state = 3; break; }
					sError = "unexpected character '" + c + "' found after end constraint at " + posToken + ", expected comma or end bracket";
					state = 0;
					break;
				default:
					sError = "internal error, undefined state " + state;
					state = 0;
					break;
			}
			if( state == 0 ){ // error occurred
				ApplicationController.vShowError( "Error executing one-liner '" + sScript + "' at position " + pos + ": " + sError );
				return;
			}
			pos++;
		}
		String sExpression = null;
		switch( state ){
			case 1: // start
			case 2: // after hash
			case 3: // before directive
			case 5: // after test directive
			case 6: // in informational label
			case 7: // after cell directive
			case 8: // after selection directive
			case 15: // in number of test errors
				ApplicationController.vShowError( "No expression found in one liner: " + sScript );
				return;
			case 4: // at beginning of range directive
			case 11: // in range directive number, begin
			case 12: // after leading hyphen in range directive
			case 16: // after whitespace after begin filter in range restriction
			case 17: // after hyphen in  range restriction with a begin
			case 18: // in number after range directive hyphen (end)
			case 19: // after end constraint (whitespace) in range restriction
				ApplicationController.vShowError( "Unterminated range directive at " + posToken + " in one liner: " + sScript );
				return;
			case 13: // after leading quote in range filter
			case 14: // after leading single quote in range filter
				ApplicationController.vShowError( "Unterminated quote at " + posToken + " in range directive in one liner: " + sScript );
				return;
			case 10: //  in expression
				sExpression = sbExpression.toString();
		}
		String sExpression_macroed = Utility_String.sReplaceString( sExpression, "$0", "_value0" );
		sExpression_macroed = Utility_String.sReplaceString( sExpression_macroed, "$1", "_dim1" );
		sExpression_macroed = Utility_String.sReplaceString( sExpression_macroed, "$2", "_dim2" );
		opendap.clients.odc.Interpreter interpreter = ApplicationController.getInstance().getInterpreter();
		org.python.core.PyCode pycodeExpression = interpreter.getInterpeter().compile( sExpression_macroed ); // the expression must be precompiled for performance reasons
		StringBuffer sbError = new StringBuffer( 256 );
		opendap.clients.odc.data.Node_Array array = (opendap.clients.odc.data.Node_Array)node;
		if( sMode.equals( "Value" ) || sMode.equals( "Command" ) || zFilterCursor ){
			int index = array._getValueIndex_Cursor();
			Object oCurrentCellValue = array._getValueObject( index );
			if( ! interpreter.zSet( "_value0", oCurrentCellValue, sbError ) ){
				ApplicationController.vShowError( "failed to set current cell value (row " + array._view.cursor_row + ", column " + array._view.cursor_column + "): " + sbError );
				return;
			}
			PyObject pyobject = interpreter.zEval( pycodeExpression, sExpression_macroed, sbError );
			if( pyobject == null ){
				ApplicationController.vShowError( "failed to set eval cell value (row " + array._view.cursor_row + ", column " + array._view.cursor_column + ") with expression " +  sExpression_macroed + ": " + sbError );
				return;
			}
			if( sMode.equals( "Value" ) ){
				jtfArray_value.requestFocus();
				jtfArray_value.setText( pyobject.toString() );
				jtfArray_value.selectAll();
			} else if( sMode.equals( "Command" ) ){
				String sCommand = "print " + sExpression_macroed;
				interpreter.vEnterCommand( sCommand );
//				try {
//					ApplicationController.getInstance().getTextViewerOS().write( sCommand.getBytes() );
//					if( ! interpreter.zExecute( sCommand, sbError ) ){
//						ApplicationController.vShowError( "error executing command at (row " + array._view.cursor_row + ", column " + array._view.cursor_column + ") with expression " +  sExpression_macroed + ": " + sbError );
//						return;
//					}
//				} catch( Throwable t ) {
//					ApplicationController.vShowError( "error sending command to text viewer at (row " + array._view.cursor_row + ", column " + array._view.cursor_column + ") with expression " +  sExpression_macroed + ": " + sbError );
//					return;
//				}
			} else {
				if( ! array._setValue( pyobject, index, sbError ) ){
					ApplicationController.vShowError( "failed to set current cell value (row " + array._view.cursor_row + ", column " + array._view.cursor_column + "): " + sbError );
					return;
				}
				panelArray_View._vDrawImage( nodeActive );
			}
		} else if( sMode.equals( "Selection" ) || zFilterSelection ){
			int xDimRow = node._view.dim_row;
			int xDimColumn = node._view.dim_column;
			for( int row = array._view.selectionUL_row; row <= array._view.selectionLR_row; row++ ){
				if( ! interpreter.zSet( "_dim1", new Integer( row ), sbError ) ){
					ApplicationController.vShowError( "failed to set current row value (" + row + "): " + sbError );
					return;
				}
				if( zFilterRange ){ // apply filter
					boolean zIncludeRow = true;
					for( int xRowFilter = 1; xRowFilter <= actFilters1[xDimRow]; xRowFilter++ ){
						if( aiFilterBegin[xDimRow][xRowFilter] >= 0 && row < aiFilterBegin[xDimRow][xRowFilter] ){
							zIncludeRow = false;
							break;
						}
						if( aiFilterEnd[xDimRow][xRowFilter] >= 0 && row > aiFilterEnd[xDimRow][xRowFilter] ){
							zIncludeRow = false;
							break;
						}
					}
					if( ! zIncludeRow ) continue; // row does not meet filter criteria
				}
				for( int column = array._view.selectionUL_column; column <= array._view.selectionLR_column; column++ ){
					if( ! interpreter.zSet( "_dim2", new Integer( column ), sbError ) ){
						ApplicationController.vShowError( "failed to set current column value (" + column + "): " + sbError );
						return;
					}
					if( zFilterRange ){ // apply filter
						boolean zIncludeColumn = true;
						for( int xColumnFilter = 1; xColumnFilter <= actFilters1[xDimColumn]; xColumnFilter++ ){
							if( aiFilterBegin[xDimColumn][xColumnFilter] >= 0 && column < aiFilterBegin[xDimColumn][xColumnFilter] ){
								zIncludeColumn = false;
								break;
							}
							if( aiFilterEnd[xDimColumn][xColumnFilter] >= 0 && column > aiFilterEnd[xDimColumn][xColumnFilter] ){
								zIncludeColumn = false;
								break;
							}
						}
						if( ! zIncludeColumn ) continue; // column does not meet filter criteria
					}
					int index = array._getValueIndex( row, column );
					Object oCurrentCellValue = array._getValueObject( index );
					if( ! interpreter.zSet( "_value0", oCurrentCellValue, sbError ) ){
						ApplicationController.vShowError( "failed to set current cell value (row " + row + ", column " + column + "): " + sbError );
						return;
					}
					PyObject pyobject = interpreter.zEval( pycodeExpression, sExpression_macroed, sbError );
					if( pyobject == null ){
						ApplicationController.vShowError( "failed to set eval cell value (row " + row + ", column " + column + ") with expression " +  sExpression_macroed + ": " + sbError );
						return;
					}
					if( ! array._setValue( pyobject, index, sbError ) ){
						ApplicationController.vShowError( "failed to set current cell value (row " + row + ", column " + column + "): " + sbError );
						return;
					}
				}
			}
			panelArray_View._vDrawImage( nodeActive );
		} else if( sMode.equals( "All" ) ){
			boolean zHasDims = sExpression_macroed.indexOf( "_dim" ) >= 0;
			Integer oZero = new Integer( 0 );
			if( zFilterRange ){
				ApplicationController.vShowError( "range filters not implemented for All mode" );
				return;
			} else { // no range filter, process every cell in the dataset
				int ctDim = array._getDimensionCount();
				int[] aiDimLengths = array._getDimensionLengths1();
				int[] axDim = new int[ctDim + 1];
				int xDim = ctDim;
				int xValue = 0;
				int ctValues = array._getValueCount();
				for( int xDimInitialization = 1; xDimInitialization <= ctDim; xDimInitialization++ ){
					if( ! interpreter.zSet( "_dim" + xDimInitialization, oZero, sbError ) ){
						ApplicationController.vShowError( "failed to initialize dim value (dim " + xDim + ") to zero: " + sbError );
						return;
					}
				}
				while( true ){
					if( xValue == ctValues ) break; // done
					Object oCurrentCellValue = array._getValueObject( xValue );
					if( ! interpreter.zSet( "_value0", oCurrentCellValue, sbError ) ){
						ApplicationController.vShowError( "failed to set current cell value (index " + xValue + "): " + sbError );
						return;
					}
//					PyObject pyobject = interpreter.zEval( sExpression_macroed, sbError ); // don't do this, compilation causes massive object creation/collection
					PyObject pyobject = interpreter.zEval( pycodeExpression, sExpression_macroed, sbError );
					if( pyobject == null ){
						ApplicationController.vShowError( "failed to eval expression (index " + xValue + ") with expression " +  sExpression_macroed + ": " + sbError );
						return;
					}
					if( ! array._setValue( pyobject, xValue, sbError ) ){
						ApplicationController.vShowError( "failed to set cell value (index " + xValue + "): " + sbError );
						return;
					}
					while( true ){
						axDim[xDim]++;
						if( zHasDims ){ // need to track dims
							if( ! interpreter.zSet( "_dim" + xDim, new Integer( axDim[xDim] ), sbError ) ){
								ApplicationController.vShowError( "failed to set current dim value (dim " + xDim + ", value " + axDim[xDim] + "): " + sbError );
								return;
							}
						}
						if( axDim[xDim] >= aiDimLengths[xDim] ){
                            axDim[xDim] = 0;
							if( zHasDims ){ // need to track dims
								if( ! interpreter.zSet( "_dim" + xDim, oZero, sbError ) ){
									ApplicationController.vShowError( "failed to set current dim value (dim " + xDim + ") to zero: " + sbError );
									return;
								}
							}
							xDim--;
							if( xDim == 0 ) break; // done
						} else break;
					}
					xDim = ctDim;
					xValue++;
				}
			}
			panelArray_View._vDrawImage( nodeActive );
		} else if( sMode.equals( "View" ) ){
			panelArray_View._vDrawImage( nodeActive );
		} else {
			ApplicationController.vShowError( "Unknown mode for one-liner: " + sMode );
			return;
		}
		
	}
}

// stores information about how the dataset should be viewed (column widths etc)
class Model_DatasetView {
}
