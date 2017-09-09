package sushi.compile.path_condition_distance;

import java.util.List;

public interface ValueCalculator {

	Iterable<String> getVariableOrigins();

	double calculate(List<Object> variables);
	
}
