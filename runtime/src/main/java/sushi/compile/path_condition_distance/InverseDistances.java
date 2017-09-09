package sushi.compile.path_condition_distance;

public class InverseDistances {
	
	public static double inverseDistanceExp(double distance, double maxValue) {		
		return maxValue * Math.exp(-0.01d * Math.pow(distance, 2.0d)); 
	}
	
	public static double inverseDistanceRatio(double distance, double maxValue) {		
		return maxValue / (1.0d + (double)distance);
	}

}
