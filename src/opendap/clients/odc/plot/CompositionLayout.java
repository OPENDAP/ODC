package opendap.clients.odc.plot;

public class CompositionLayout {

	public static enum SCALE_MODE {
		CanvasFitsWindow,
		FixedSizeCanvas,
		FixedSizePlot
	}
	
	public enum Type {
		Single,
		Gridded,
		Panelled,
		FreeForm
	}
}
