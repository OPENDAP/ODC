package opendap.clients.odc.data;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import opendap.clients.odc.ApplicationController;
import opendap.clients.odc.Panel_View_Text_Editor;
import opendap.clients.odc.data.Model_Dataset.DATASET_TYPE;

public class Panel_Edit_Expression extends JPanel {
	private Model_Dataset mModel;
	private Panel_Define_Expression mParent;
	private Panel_View_Text_Editor mEditor;
	boolean _zInitialize( Panel_Define_Expression parent, String sDirectory, String sName, String sContent, StringBuffer sbError ){
		mModel = null;
		mParent = parent;
		if( parent == null ){
			sbError.append( "no parent supplied" );
			return false;
		}
		Border borderEtched = BorderFactory.createEtchedBorder();
		setBorder( BorderFactory.createTitledBorder( borderEtched, "Expression Editor", TitledBorder.RIGHT, TitledBorder.TOP ) );
		setLayout( new BorderLayout() );
		
		// set up editor
		mEditor = Panel_View_Text_Editor._create( null, sDirectory, sName, sContent, sbError );
		this.add( mEditor, BorderLayout.CENTER );
		
		// set up control buttons
		JPanel panelControlButtons = new JPanel();
		JButton buttonGenerateDataset = new JButton( "Generate Dataset" );
		JButton buttonPlot = new JButton( "Plot" );
		panelControlButtons.setLayout( new BoxLayout(panelControlButtons, BoxLayout.X_AXIS) );
		panelControlButtons.add( buttonGenerateDataset );
		panelControlButtons.add( buttonPlot );
		this.add( panelControlButtons, BorderLayout.NORTH );
		
		// define actions
		buttonGenerateDataset.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event ){
					if( mParent == null ){
						ApplicationController.vShowError( "internal error, Panel_Edit_Expression has no parent" );
						return;
					}
					Panel_View_Data panelViewData = mParent._getParent();
					if( panelViewData == null ){
						ApplicationController.vShowError( "internal error, no panel exists for generate dataset" );
						return;
					}
					Model_DataView modelDataView = panelViewData._getModelDataView();
					if( modelDataView == null ){
						ApplicationController.vShowError( "internal error, no data view model exists for generate dataset" );
						return;
					}
					modelDataView.action_GenerateDatasetFromExpression();
				}
			}
		);
		buttonPlot.addActionListener(
			new java.awt.event.ActionListener(){
				public void actionPerformed( ActionEvent event ){
					if( mParent == null ){
						ApplicationController.vShowError( "internal error, Panel_Edit_Expression has no parent" );
						return;
					}
					Panel_View_Data panelViewData = mParent._getParent();
					if( panelViewData == null ){
						ApplicationController.vShowError( "internal error, no panel exists for generate dataset" );
						return;
					}
					Model_DataView modelDataView = panelViewData._getModelDataView();
					if( modelDataView == null ){
						ApplicationController.vShowError( "internal error, no data view model exists for plotting" );
						return;
					}
					modelDataView.action_PlotFromExpression();
				}
			}
		);
		
		return true;
	}
	Model_Dataset _getModel(){ return mModel; }
	boolean _setModel( Model_Dataset model, StringBuffer sbError ){
		if( model == null ){
			sbError.append( "no model supplied" );
			return false;
		}
		if( model.getType() != DATASET_TYPE.PlottableExpression ){
			sbError.append( "supplied model is not an expression" );
			return false;
		}
		mModel = model;
		mEditor._setModel( model );
		return true;
	}
	Panel_Define_Expression _getParent(){ return mParent; }
}

