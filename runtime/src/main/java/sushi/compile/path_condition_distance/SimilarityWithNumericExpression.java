package sushi.compile.path_condition_distance;

import static sushi.compile.path_condition_distance.InverseDistances.inverseDistanceRatio;

import java.util.ArrayList;
import java.util.Map;

import sushi.logging.Logger;

public class SimilarityWithNumericExpression implements ClauseSimilarityHandler {
	private static final Logger logger = new Logger(SimilarityWithNumericExpression.class);
	
	private final ValueCalculator theValueCalculator;
	
	public SimilarityWithNumericExpression(ValueCalculator theValueCalculator) {
		if (theValueCalculator == null) {
			throw new SimilarityComputationException("Value calculator cannot be null");
		}
		this.theValueCalculator = theValueCalculator;
	}

	@Override
	public double evaluateSimilarity(CandidateBackbone backbone, Map<String, Object> candidateObjects) {
		logger.debug("Handling similarity for numeric expression");
		
		double similarity = 0.0d;
		String theVariableOrigin = null; //only for exceptions
		try {
			final ArrayList<Object> variables = new ArrayList<>();
			for (String variableOrigin : this.theValueCalculator.getVariableOrigins()) {
				theVariableOrigin = variableOrigin;
				Object variableValue = backbone.retrieveOrVisitField(variableOrigin, candidateObjects);
				variables.add(variableValue);
			}
			similarity += inverseDistanceRatio(this.theValueCalculator.calculate(variables), 1.0d);
		} catch (FieldNotInCandidateException e) {
			logger.debug("Field " + theVariableOrigin + " does not yet exist in candidate");			
		} catch (FieldDependsOnInvalidFieldPathException e) {
			logger.debug("Field " + theVariableOrigin + " depends on field path that did not converge yet: " + e.getMessage());			
		}
		
		logger.debug("Similarity increases by: " + similarity);
		return similarity;
	}

}
