package sushi.compile.path_condition_distance;

import java.util.Map;

public interface ClauseSimilarityHandler {
	
	double evaluateSimilarity(CandidateBackbone vdata, Map<String, Object> candidateObjects);
	
}
