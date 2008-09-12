package opendap.clients.odc.plot;

public interface IPlottable {
	int getDataType();
	int getDataElementCount();
	short[] getShortArray();
	int[] getIntArray();
	long[] getLongArray();
	float[] getFloatArray();
	double[] getDoubleArray();
	short[] getShortArray2();
	int[] getIntArray2();
	long[] getLongArray2();
	float[] getFloatArray2();
	double[] getDoubleArray2();
	double getExtents_x();
	double getExtents_y();
	double getExtents_z();
	double getExtents_t();
	int getDimension_x();
	int getDimension_y();
	int getDimension_z();
	int getDimension_t();
	int getMissingCount1();
	short[] getMissingShort1();
	int[] getMissingInt1();
	long[] getMissingLong1();
	float[] getMissingFloat1();
	double[] getMissingDouble1();
	int getMissingCount2();
	short[] getMissingShort2();
	int[] getMissingInt2();
	long[] getMissingLong2();
	float[] getMissingFloat2();
	double[] getMissingDouble2();
}
