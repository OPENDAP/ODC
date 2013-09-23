package opendap.clients.odc.plot;

public interface IPlottable {
	public int getDimension_x();
	public int getDimension_y();
	public int getDataType();
	public float[] getFloatArray();

}
