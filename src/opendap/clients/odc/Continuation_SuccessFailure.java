package opendap.clients.odc;

public interface Continuation_SuccessFailure {
	public void Success();
	public void Failure(String sReason);
}
