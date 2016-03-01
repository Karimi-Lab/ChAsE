package still.analysis;

import still.data.Function;
import still.data.Map;

public class AnalysisPair {

	private Function function 	= null;
	private Map map 			= null;

	public AnalysisPair( Function function, Map map ) {
		
		this.function	= function;
		this.map 		= map;
	}

	public Function getFunction() {
		return function;
	}

	public void setFunction(Function function) {
		this.function = function;
	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}
	
	
}
