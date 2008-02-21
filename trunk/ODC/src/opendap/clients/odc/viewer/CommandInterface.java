package opendap.clients.odc.viewer;

public interface CommandInterface {
	public String[] getCommands();
	public void command( String s, int[] i );
}
