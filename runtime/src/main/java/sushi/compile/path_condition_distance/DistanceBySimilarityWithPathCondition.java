package sushi.compile.path_condition_distance;

import java.util.List;
import java.util.Map;

import sushi.logging.Logger;

public class DistanceBySimilarityWithPathCondition {
	private static final Logger logger = new Logger(DistanceBySimilarityWithPathCondition.class);

	public static double distance(List<ClauseSimilarityHandler> pathConditionHandler, Map<String, Object> candidateObjects) {
		logger.debug("Computing similarity with path condition: BEGIN ");
		
		double achievedSimilarity = 0.0d;		
		CandidateBackbone backbone = new CandidateBackbone();
		for (ClauseSimilarityHandler handler : pathConditionHandler) {
			achievedSimilarity += handler.evaluateSimilarity(backbone, candidateObjects);
		}
		
		logger.debug("Computing similarity with path condition: END: Similarity is " + achievedSimilarity);
		
		double goalSimilarity = pathConditionHandler.size();
		double distance = goalSimilarity - achievedSimilarity;
		assert (distance > 0);
		
		logger.debug("Distance from path condition is " + distance);
		
		return distance;
	}
}
