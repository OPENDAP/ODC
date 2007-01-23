package opendap.clients.odc;

public interface ByteCounter {
	void vReportByteCount_EverySecond( long nByteCount );
	void vReportByteCount_Total( long nByteCount );
}
