package sushi.compile.path_condition_distance;

import sushi.logging.Logger;

public class SimilarityWithRefToNull extends SimilarityWithRef {
	private static final Logger logger = new Logger(SimilarityWithRefToNull.class);
	
	public SimilarityWithRefToNull(String theReferenceOrigin) {
		super(theReferenceOrigin);
	}

	@Override
	protected double evaluateSimilarity(CandidateBackbone backbone, Object referredObject) {
		logger.debug("Null reference");
		
		double similarity = 0.0d;
		
		if (referredObject == null) {
			logger.debug("Field " + theReferenceOrigin + " is null also in candidate");
			similarity += 1.0d;	
		}
		else {
			logger.debug("Field " + theReferenceOrigin + " is not null in candidate");
		}
		
		logger.debug("Similarity increases by: " + similarity);
		return similarity;
	}

}
