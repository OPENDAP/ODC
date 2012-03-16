package opendap.clients.odc;

import opendap.clients.odc.data.Model_Dataset;
import opendap.clients.odc.data.Model_LoadedDatasets;
import opendap.clients.odc.data.Model_Retrieve;
import opendap.clients.odc.data.OutputEngine;

// Master model for the application

public class Model {
	private Model(){}

	private static final Model thisSingleton = new Model();
	public static final Model get(){ return thisSingleton; }

	public static final void initialize(){
		thisSingleton.mOutputEngine = new OutputEngine();
		thisSingleton.mRetrieve     = new Model_Retrieve();
		thisSingleton.mDatasets     = Model_LoadedDatasets._create();
	}
	
	private OutputEngine mOutputEngine = null;
	public final OutputEngine getOutputEngine(){ return mOutputEngine; }

	private Model_Retrieve mRetrieve = null;
	public Model_Retrieve getRetrieveModel(){ return mRetrieve; }

	private Model_LoadedDatasets mDatasets = null;
	public Model_LoadedDatasets getDatasets(){ return mDatasets; }

	private Model_Dataset modelActivePlotData = null;
	public Model_Dataset getPlotDataModel(){ return modelActivePlotData; }
	public void setPlotDataModel( Model_Dataset model ){ modelActivePlotData = model; }

}
