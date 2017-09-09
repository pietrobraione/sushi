package sushi.compile.path_condition_distance;

import sushi.compile.distance.PrefixDistance;
import sushi.logging.Logger;

public class SimilarityWithRefToAlias extends SimilarityWithRef {
	private static final Logger logger = new Logger(SimilarityWithRefToAlias.class);

	private final String theAliasOrigin;
	
	public SimilarityWithRefToAlias(String theReferenceOrigin, String theAliasOrigin) {
		super(theReferenceOrigin);
		if (theAliasOrigin == null) {
			throw new SimilarityComputationException("Alias origin cannot be null");
		}
		this.theAliasOrigin = theAliasOrigin;
	}

	@Override
	protected double evaluateSimilarity(CandidateBackbone backbone, Object referredObject) {
		logger.debug("Ref that aliases another ref");
		
		double similarity = 0.0d;
		
		Object alias = backbone.getVisitedObject(theAliasOrigin);

		if (referredObject == alias) {
			logger.debug("Matching aliases between field " + theReferenceOrigin + " and field " + theAliasOrigin);
			similarity += 1.0d;
		}
		else if (referredObject == null) {
			logger.debug("Non matching aliases: field " + theReferenceOrigin + " is null rather than alias of " + theAliasOrigin);
		}
		else {
			String objOrigin = backbone.getOrigin(referredObject);
			logger.debug("Non matching aliases: field " + theReferenceOrigin + " corresponds to field " + 
					objOrigin + " rather than to " + theAliasOrigin);
			assert (objOrigin != null);
			int distance = PrefixDistance.calculateDistance(theAliasOrigin, objOrigin);
			assert (distance != 0);
			similarity += InverseDistances.inverseDistanceExp(distance, 1.0d);
		}	
		
		logger.debug("Similarity increases by: " + similarity);
		return similarity;
	}

}
