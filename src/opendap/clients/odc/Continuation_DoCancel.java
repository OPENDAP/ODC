package opendap.clients.odc;

public interface Continuation_DoCancel {
	public void Do();
	public void Cancel();
}

interface Continuation_SuccessFailure {
	public void Success();
	public void Failure(String sReason);
}
