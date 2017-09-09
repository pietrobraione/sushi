package sushi.compile.distance;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sushi.compile.reflection.ObjectField;
import sushi.logging.Logger;
import sushi.util.ReflectionUtils;

public class DistanceBySimilarityOfObjectGraphs {

	private static final Logger logger = new Logger(DistanceBySimilarityOfObjectGraphs.class);

	private static boolean targetIsPartiallySymbolicObject = false;
	private static HashSet<ObjectField> fieldsToBeConsidered = null;
	private static boolean considerAliases = true;

	/*The two maps below track the visited objects
	 * and associate each visited object with an Id
	 * that is equivalent between the paired object
	 * of target and candidate, respectively*/
	private static Map<ObjectMapWrapper, String> visitedInTarget = new HashMap<ObjectMapWrapper, String>(); 
	private static Map<ObjectMapWrapper, String> visitedInCandidate = new HashMap<ObjectMapWrapper, String>();

	private static List<DistancePair> worklist = new LinkedList<DistancePair>();
	private static boolean converged;

	private DistanceBySimilarityOfObjectGraphs() {}
	
	private static void initGlobals(HashSet<ObjectField> initializedObjectFields, boolean aliases) {
		//setting parameters for the computation 
		targetIsPartiallySymbolicObject = (initializedObjectFields != null);
		fieldsToBeConsidered = initializedObjectFields;
		considerAliases = aliases;		
		
		/*The two maps below track the visited objects
		 * and associate each visited object with an Id
		 * that is equivalent between the paired object
		 * of target and candidate, respectively*/
		visitedInTarget.clear();
		visitedInCandidate.clear();
		
		converged = true; //assume convergence until observing a convergence failure
	}


	public static double distance(HashSet<ObjectField> initializedObjectFields, 
			double knownRefSimilarity, Object... objects) {
				
		initGlobals(initializedObjectFields, true);
		
		logger.debug("Computing similarity: BEGIN ");
		double achievedSimilarity = 0.0d;
		for (int i = 0; i < objects.length - (objects.length % 2); i += 2) {
			Object oTarget = objects[i];
			Object oCandidate = objects[i + 1];

			logger.debug("Computing similarity value for " + oCandidate + " wrt " + oTarget);
			achievedSimilarity += computeSimilarity(oTarget, oCandidate, "obj" + i);
			logger.debug("Actual similarity is " + achievedSimilarity);

		}
		
		logger.debug("Computing similarity: END: Similarity is " + achievedSimilarity);

		double distance = (converged) ? 0.0d : inverseDistanceRatio(achievedSimilarity, 100.00d);
		logger.debug("Current distance is " + distance);
		return distance;
	}

	/* kept only for debugging purposes */
	public static double refSimilarity(HashSet<ObjectField> initializedObjectFields, 
			boolean aliases, Object... objects) {
		
		initGlobals(initializedObjectFields, aliases);
		
		logger.debug("Computing ref similarity: BEGIN ");
		double refSimilarity = 0.0d;
		for (int i = 0; i < objects.length - (objects.length % 2); i += 2) {
			Object oTarget = objects[i];
			
			logger.debug("Computing ref similarity value for " + oTarget);
			refSimilarity += computeSimilarity(oTarget, oTarget, "obj" + i); 
			logger.debug("Ref similarity of " + oTarget + " is " + refSimilarity);
		}
		
		logger.debug("Computing ref similarity: END: Ref similarity is " + refSimilarity);
		
		return refSimilarity;
	}
	
	private static final class ObjectMapWrapper {
		private Object o;
		ObjectMapWrapper(Object o) { this.o = o; }
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ObjectMapWrapper)) {
				return false;
			}
			final ObjectMapWrapper omw = (ObjectMapWrapper) obj;
			return (this.o == omw.o);
		}
		
		@Override
		public int hashCode() {
			return System.identityHashCode(this.o);
		}
	}
	
	private static double computeSimilarity(Object oTarget, Object oCandidate, String rootObjId) {
		logger.debug("computing similarity value for " + oCandidate + " wrt " + oTarget);
		
		double similarity = 0.0d;

		if (oTarget == null || oCandidate == null) {
			similarity += similarityWithNull(oTarget, oCandidate);
			logger.debug("Similarity: " + similarity);
			return similarity;
		}

		worklist.clear();

		similarity += handleEdgeToSubElement(oTarget, oCandidate, "<ROOT>." + rootObjId);
		
		while (!worklist.isEmpty()) {
			DistancePair pair = worklist.remove(0);
			Object objTarget = pair.o1;
			Object objCandidate = pair.o2;
			
			assert(visitedInTarget.containsKey(new ObjectMapWrapper(objTarget)));
			assert(visitedInCandidate.containsKey(new ObjectMapWrapper(objCandidate)));
			assert(visitedInTarget.get(new ObjectMapWrapper(objTarget)).equals(visitedInCandidate.get(new ObjectMapWrapper(objCandidate))));
			
			if (objTarget == null || objCandidate == null) {
				similarity += similarityWithNull(objTarget, objCandidate);
			}
			else if (ReflectionUtils.isPrimitive(objTarget)) {
				similarity += similarityWithPrimitiveObject(objTarget, objCandidate);
			}
			else if (ReflectionUtils.isString(objTarget)) {
				similarity += similarityWithString(objTarget, objCandidate);
			}
			else if (ReflectionUtils.isArray(objTarget)) {
				similarity += similarityWithArray(objTarget, objCandidate);
			}
			else /* OBJECT */{
				similarity += similarityWithObject(objTarget, objCandidate);
			}
			logger.debug("Similarity after this step: " + similarity);
		}
		logger.debug("Similarity: " + similarity);
		return similarity;
	}
	
	private static double similarityWithObject(Object objTarget, Object objCandidate) {
		logger.debug("Handling OBJECT");
		double similarity = 0.0d;
		
		assert (objTarget != null && objCandidate != null); //handled in similarityWithNull
		if (!objTarget.getClass().equals(objCandidate.getClass())) {
			logger.debug("different classes: " + objTarget.getClass() + " vs " + objCandidate.getClass());
			//similarity += 0.5d; //A wrong object is better than null :-)
			String classNameTarget = objTarget.getClass().getName();
			int splitPoint = classNameTarget.lastIndexOf('.');
			String packageTarget = classNameTarget.substring(0, splitPoint);
			classNameTarget = classNameTarget.substring(splitPoint, classNameTarget.length());

			String classNameCandidate = objCandidate.getClass().getName();
			splitPoint = classNameCandidate.lastIndexOf('.');
			String packageCandidate = classNameCandidate.substring(0, splitPoint);
			classNameCandidate = classNameCandidate.substring(splitPoint, classNameCandidate.length());
			
			int packageDistance = PrefixDistance.calculateDistance(packageTarget, packageCandidate);
			similarity += inverseDistanceExp(packageDistance, 0.5d);
			if (packageDistance == 0) {
				logger.debug("same packages: " + objTarget.getClass() + " vs " + objCandidate.getClass());
				double classNameDistance = LevenshteinDistance.calculateDistance(classNameTarget, classNameCandidate);
				similarity += inverseDistanceExp(classNameDistance, 0.5d);
			}

			logger.debug("Similarity increases by: " + similarity);
			converged = false;
			return similarity;
		}
		else {
			logger.debug("Objects of same classes: " + objTarget.getClass() + " vs " + objCandidate.getClass());
			similarity += 1.0d;
			logger.debug("Similarity increases by: " + similarity);
		}
		
		logger.debug("Looking into fields");
		List<Field> fields = ReflectionUtils.getInheritedPrivateFields(objTarget.getClass());
		for (int i = 0; i < fields.size(); i++) {
			try {
				Field aField = fields.get(i);
				aField.setAccessible(true);
					
				// skip comparison of constants, excluded by filter, and don't care fields
				if (ReflectionUtils.isConstant(aField)) {
					logger.debug(" Skip: " + Modifier.toString(aField.getModifiers()) + " " + aField.getType() + " " + aField.getName());
				}
				else if (FieldFilter.exclude(aField)) {
					logger.debug(" Exclude: " + Modifier.toString(aField.getModifiers()) + " " + aField.getType() + " " + aField.getName());
				}
				else if (targetIsPartiallySymbolicObject && !fieldsToBeConsidered.contains(new ObjectField(objTarget, aField))) {
					logger.debug(" Don't care: " + Modifier.toString(aField.getModifiers()) + " " + aField.getType() + " " + aField.getName());
				}
				else {					
					logger.debug(" field: " + Modifier.toString(aField.getModifiers()) + " " + aField.getType() + " " + aField.getName());
					assert (visitedInTarget.containsKey(new ObjectMapWrapper(objTarget)));
					String subElementId = visitedInTarget.get(new ObjectMapWrapper(objTarget)) + "." + aField.getName();
					similarity += handleEdgeToSubElement(aField.get(objTarget), aField.get(objCandidate), subElementId); 
				}

			} catch (Exception e) {
				logger.error("Error during Similarity calculation", e);
			}
		}
		logger.debug("With fields: Similarity increases by: " + similarity);
		return similarity;
	}

	private static double inverseDistanceExp(double distance, double maxValue) {		
		return maxValue * Math.exp(-0.01d * Math.pow(distance, 2.0d)); 
	}
	
	private static double inverseDistanceRatio(double distance, double maxValue) {		
		return maxValue / (1.0d + (double)distance);
	}


	private static double similarityWithArray(Object objTarget, Object objCandidate) {
		logger.debug("Handling array");
		double similarity = 0.0d;
		//boolean isPrimitiveType = objTarget.getClass().getComponentType().isPrimitive();
		try {
			final Object[] arrayTarget = Object[].class.cast(objTarget);
			final Object[] arrayCandidate = Object[].class.cast(objCandidate);
			
			//similarity depends on arrays being of same type and on their length distance
			int lengthCommon = Math.min(Array.getLength(arrayTarget), Array.getLength(arrayCandidate));
			int lengthDistance = Math.max(Array.getLength(arrayTarget), Array.getLength(arrayCandidate)) - lengthCommon;
			if (lengthDistance == 0.0d) similarity += 1.0;
			else {
				similarity += inverseDistanceExp(lengthDistance, 1.0d);
				converged = false;
			}
			logger.debug("Similarity increases by: " + similarity);

			logger.debug("Looking into array items");
			for (int i = 0; i <  lengthCommon; i++) { 
				if (targetIsPartiallySymbolicObject && 
						!fieldsToBeConsidered.contains(new ObjectField(objTarget, Integer.toString(i)))) {
					logger.debug("Don't care: at array item [" + i +"]");
				}
				else {
					assert (visitedInTarget.containsKey(new ObjectMapWrapper(objTarget)));
					String subElementId = visitedInTarget.get(new ObjectMapWrapper(objTarget)) + "[" + i + "]";
					similarity += handleEdgeToSubElement(arrayTarget[i], arrayCandidate[i], subElementId); 
				}
			}	
		} catch (IllegalArgumentException e) {
			logger.error("Error during cast", e);
			logger.debug("Arrays differently typed: Similarity increases by: " + similarity);
			converged = false;
		}
		logger.debug("With array items: Similarity increases by: " + similarity);
		return similarity;
	}

	private static double similarityWithString(Object objTarget, Object objCandidate) {
		logger.debug("Handling String");
		double similarity = 0.0d;
		try {
			if (targetIsPartiallySymbolicObject && !fieldsToBeConsidered.contains(new ObjectField(objTarget, String.class.getDeclaredField("value")))) {
				similarity += 1.0d;
			} else {
				double distance = LevenshteinDistance.calculateDistance((String) objTarget, (String) objCandidate);
				if (distance == 0.0d) similarity += 1.0d;
				else {
					similarity += inverseDistanceExp(distance, 1.0d);
					converged = false;
				}
			}
		} catch (NoSuchFieldException | SecurityException e) {
			logger.error("Error during Similarity calculation", e);
			converged = false;
		}
		logger.debug("Similarity increases by: " + similarity);
		return similarity;
	}

	private static double similarityWithPrimitiveObject(Object objTarget, Object objCandidate) {
		// this definition of primitive contains also
		// primitive classes (e.g. Integer)
		logger.debug("Handling primitive objects: " + objTarget + " " +  objCandidate);
		double similarity = 0.0d;
		if (objCandidate == objTarget) return 1.0d;
		double distance = PrimitiveDistance.distance(objTarget, objCandidate);
		if (distance == 0.0d) similarity += 1.0d;
		else {
			similarity += inverseDistanceRatio(distance, 1.0d);
			converged = false;
		}
		logger.debug("Similarity increases by: " + similarity);
		return similarity;
	}

	private static double similarityWithNull(Object objTarget, Object objCandidate) {
		logger.debug("Handling null");
		double similarity = 0.0d;
		if (objTarget == null) {	
			logger.debug("Target is null");
			if (objCandidate == null) {
				logger.debug("Candidate is null");
				similarity += 1.0d;
			}
		}
		else if (objCandidate == null) {
			logger.debug("Target is not null, but candidate is null");
		}
		logger.debug("Similarity increases by: " + similarity);
		//else: both non null, nothing to handle here
		if (similarity != 1.0d) converged = false;
		return similarity;
	}

	private static double handleEdgeToSubElement(Object objTarget, Object objCandidate, String subElementId) {
		
		double similarity = 0.0d;
		
		similarity += similarityWithEdge(objTarget, objCandidate, subElementId);
		
		// null values and corner cases are managed at the  beginning of the iteration
		boolean bothNewObjects = !visitedInTarget.containsKey(new ObjectMapWrapper(objTarget)) && 
								 !visitedInCandidate.containsKey(new ObjectMapWrapper(objCandidate));
		if (mustIgnoreAliases(objTarget)) {
			logger.debug("may visit multiple times element in target:" + subElementId);
		}
		else {
			visitedInTarget.put(new ObjectMapWrapper(objTarget), subElementId);
			logger.debug("mark as visited element in target:" + subElementId);
		}
		if (mustIgnoreAliases(objCandidate)) {
			logger.debug("may visit multiple times element in candidate:" + subElementId);
		} 
		else {
			visitedInCandidate.put(new ObjectMapWrapper(objCandidate), subElementId);
			logger.debug("mark as visited element in candidate:" + subElementId);
		}
		if (bothNewObjects) {
			logger.debug("Adding sub elements to the worklist:" + subElementId);
			worklist.add(new DistancePair(objTarget, objCandidate));
		}
		
		return similarity;
	}

	private static boolean mustIgnoreAliases(Object obj) {
		return 	obj == null || 
				ReflectionUtils.isPrimitive(obj) ||
				ReflectionUtils.isString(obj);
	}
	
	private static double similarityWithEdge(Object objTarget, Object objCandidate, String subElementId) {
		if (!considerAliases) {
			return 0.0d;
		}

		logger.debug("Handling edge");
		double similarity = 0.0d;
		
		assert (subElementId != null);
		
		String targetId = mustIgnoreAliases(objTarget) ? "" : visitedInTarget.get(new ObjectMapWrapper(objTarget));
		String candidateId = mustIgnoreAliases(objCandidate) ? "" : visitedInCandidate.get(new ObjectMapWrapper(objCandidate));

		if (targetId == null) {
			logger.debug("Target is edge to a new object");
			targetId = subElementId;
		}
		if (candidateId == null) {
			logger.debug("Candidate is edge to a new object");
			candidateId = subElementId;
		}
		
		if (targetId.equals(candidateId)) {
			logger.debug("Matching edges");
			similarity += 1.0d;
		}
		else {	
			logger.debug("Non-matching edges");
			//double edgeDistance = LevenshteinDistance.calculateDistance(targetId, candidateId);
			int edgeDistance = PrefixDistance.calculateDistance(targetId, candidateId);
			assert (edgeDistance != 0);
			similarity += inverseDistanceExp(edgeDistance, 1.0d);
			converged = false;
		}

		logger.debug("Similarity increases by: " + similarity);
		return similarity;

	}

}
