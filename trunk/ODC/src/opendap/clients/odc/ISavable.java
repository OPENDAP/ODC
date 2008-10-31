package opendap.clients.odc;

public interface ISavable {
	public Object _open();
	public boolean _save( java.io.Serializable tObjectToBeSaved );
	public boolean _saveAs( java.io.Serializable tObjectToBeSaved );
	public boolean _isDirty();
	public void _makeDirty();
	public void _makeClean();
}
