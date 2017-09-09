package sushi.compile.distance;

public class ObjectDistance {
	private static final double NULL_WEIGHT = 1000000.0d;

	public static double getNullDistance(Object oPartial, Object oComplete) {
		return NULL_WEIGHT;
	}
}
