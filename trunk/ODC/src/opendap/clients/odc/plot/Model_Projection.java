package opendap.clients.odc.plot;

/** A projection model is used to define the way values are mapped to a 2D plot area. */

abstract public class Model_Projection {
	abstract PointSet getPointSet( double[] adPointData_X, double[] adPointData_Y );
	abstract double[] getMapping_Latitude();
	abstract double[] getMapping_Longitude();
}

class Model_Projection_Linear extends Model_Projection {
	double x_offset;
	double y_offset;
	double x_scale;
	double y_scale;
	private Model_Projection_Linear(){};
	public static Model_Projection_Linear create( int iViewportWidth, int iViewportHeight, double dBeginX, double dEndX, double dBeginY, double dEndY ){
		Model_Projection_Linear model = new Model_Projection_Linear(); 
		model.x_offset = dBeginX;
		model.y_offset = dBeginY;
		model.x_scale = (dEndX - dBeginX)/iViewportWidth;
		model.y_scale = (dEndY - dBeginY)/iViewportHeight;
		return model;
	}
	PointSet getPointSet( double[] adPointData_X, double[] adPointData_Y ){
		int ctPoints = adPointData_X.length;
		PointSet points = PointSet.create( ctPoints );
		for( int xPoint = 1; xPoint <= ctPoints; xPoint++ ){
			points.ax1[xPoint] = (int)((adPointData_X[xPoint - 1] - x_offset)/x_scale); 
			points.ay1[xPoint] = (int)((adPointData_Y[xPoint - 1] - y_offset)/y_scale); 
		}
		return points;
	}
	double[] getMapping_Latitude(){ // TODO
		return null;
	}
	double[] getMapping_Longitude(){ // TODO
		return null;
	}
}

class PointSet {
	private PointSet(){}
	public static PointSet create( int count ){
		PointSet points = new PointSet();
		points.ax1 = new int[count+1];
		points.ay1 = new int[count+1];
		return points;
	}
	int count;
	int[] ax1; // one-based arrays
	int[] ay1;
}
