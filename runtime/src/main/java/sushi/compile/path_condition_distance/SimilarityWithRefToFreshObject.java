package sushi.compile.path_condition_distance;

import sushi.compile.distance.LevenshteinDistance;
import sushi.compile.distance.PrefixDistance;
import sushi.logging.Logger;

public class SimilarityWithRefToFreshObject extends SimilarityWithRef {
	private static final Logger logger = new Logger(SimilarityWithRefToFreshObject.class);

	private final Class<?> theReferredClass;
	
	public SimilarityWithRefToFreshObject(String theReferenceOrigin, Class<?> theReferredClass) {
		super(theReferenceOrigin);
		if (theReferredClass == null) {
			throw new SimilarityComputationException("Class cannot be null");
		}
		this.theReferredClass = theReferredClass;
	}

	@Override
	protected double evaluateSimilarity(CandidateBackbone backbone, Object referredObject) {
		logger.debug("Ref to a fresh object");
		
		final double freshnessSimilarity = 0.3d;
		final double samePackageSimilarity = 0.3d;
		final double sameClassSimilarity = 0.4d;
		assert (freshnessSimilarity + samePackageSimilarity + sameClassSimilarity == 1.0d);
		
		boolean isFreshObject = false;
		double similarity = 0.0d;
		
		if (referredObject == null) {
			logger.debug(theReferenceOrigin + " is not a fresh object in candidate, rather it is null");
		}
		else {
			String objOrigin = backbone.getOrigin(referredObject);
			if (!objOrigin.equals(theReferenceOrigin)) { //it is an alias rather than a fresh object
				logger.debug(theReferenceOrigin + " is not a fresh object in candidate, rather it aliases " + objOrigin);
				int distance = PrefixDistance.calculateDistance(theReferenceOrigin, objOrigin);
				assert (distance != 0);
				similarity += InverseDistances.inverseDistanceExp(distance, freshnessSimilarity);
			}
			else {
				logger.debug(theReferenceOrigin + " is a fresh object also in candidate");
				isFreshObject = true;
				similarity += freshnessSimilarity;
			}
		}

		if (!isFreshObject) {
			logger.debug("Similarity increases by: " + similarity);
			return similarity;
		}
			
		if (!referredObject.getClass().equals(theReferredClass)) {
			logger.debug(theReferenceOrigin + " refers to an object of class " + referredObject.getClass() + " rather than " + theReferredClass);
			String classNameTarget = theReferredClass.getName();
			int splitPoint = classNameTarget.lastIndexOf('.');
			String packageTarget = classNameTarget.substring(0, splitPoint);
			classNameTarget = classNameTarget.substring(splitPoint, classNameTarget.length());
			
			String classNameCandidate = referredObject.getClass().getName();
			splitPoint = classNameCandidate.lastIndexOf('.');
			String packageCandidate = classNameCandidate.substring(0, splitPoint);
			classNameCandidate = classNameCandidate.substring(splitPoint, classNameCandidate.length());
			
			int packageDistance = PrefixDistance.calculateDistance(packageTarget, packageCandidate);
			similarity += InverseDistances.inverseDistanceExp(packageDistance, samePackageSimilarity);
			if (packageDistance == 0) {
				logger.debug("The packages are the same");
				double classNameDistance = LevenshteinDistance.calculateDistance(classNameTarget, classNameCandidate);
				similarity += InverseDistances.inverseDistanceExp(classNameDistance, sameClassSimilarity);
			}
		}
		else {
			logger.debug(theReferenceOrigin + " refers to an object that matches " + theReferredClass);
			similarity += sameClassSimilarity + samePackageSimilarity;
		}
			
		logger.debug("Similarity increases by: " + similarity);
		return similarity;
	}

}
