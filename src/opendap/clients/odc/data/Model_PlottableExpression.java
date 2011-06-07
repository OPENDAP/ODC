package opendap.clients.odc.data;

/** Models a plottable expression such as "y=x^2"
 *  Created from a plottable script. See "Plotting Expressions" in the help text.
 *  See opendap.clients.odc.Interpreter.generatePlottableExpression() for generation code.
 */

public class Model_PlottableExpression {
	private PlottableExpression_TYPE type;
	private Model_PlottableExpression(){}
	PlottableExpression_TYPE getType(){ return type; } 
	final static Model_PlottableExpression create( Script script, StringBuffer sbError ){
		Model_PlottableExpression model = new Model_PlottableExpression();
		
		String sScriptText = script.getText();
		// gather symbols
		// convert symbols
		// determine type of expression
		// make compiled objects
		
		return model;
	}
}

enum PlottableExpression_TYPE {
	Cartesian,
	Polar,
	Parametric
}
