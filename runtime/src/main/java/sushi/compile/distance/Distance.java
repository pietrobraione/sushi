package sushi.compile.distance;

import java.util.HashSet;

import sushi.compile.reflection.ObjectField;

public class Distance {
	public static final double ARRAY_CELL_FACTOR = 5.0d;
	
	private Distance() {}
	
	public static double distance(HashSet<ObjectField> nullObjectFields, Object... objects) {
		return DistanceBySimilarityOfObjectGraphs.distance(nullObjectFields, -1, objects);
		//return DistanceSBES.distance(nullObjectFields, objects);
	}
}

