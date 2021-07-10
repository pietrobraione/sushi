package sushi.execution.minimizer;

import static org.ojalgo.function.constant.BigMath.ONE;
import static org.ojalgo.function.constant.BigMath.ZERO;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Variable;

final class MinimizerProblemFactoryOjAlgo extends MinimizerProblemFactory<MinimizerProblemOjAlgo> {
	/** The set of the relevant branches (i.e., the branches to cover). */
	private TreeSet<Integer> relevantBranchNumbers;
	
	/** 
	 * Maps branch numbers to the corresponding row numbers in the 
	 * linear problem. 
	 */
	private HashMap<Integer, Integer> branches2Rows;
	
	/** 
	 * Maps row numbers in the linear problem (the position in the 
	 * list) to the corresponding branch numbers. 
	 */
	private ArrayList<Integer> rows2Branches;
	
	/** 
	 * The set of the relevant traces (i.e., the traces not already
	 * covered, or not already attempted to cover and failed). 
	 */
	private TreeSet<Integer> relevantTraceNumbers;
		
	/** 
	 * Maps trace numbers to the corresponding column numbers in the 
	 * linear problem. 
	 */
	private HashMap<Integer, Integer> traces2Cols;
	
	/** 
	 * Maps column numbers in the linear problem (the position in the 
	 * list) to the corresponding trace numbers. 
	 */
	private ArrayList<Integer> cols2Traces;
	
	MinimizerProblemFactoryOjAlgo(MinimizerParameters parameters) throws IOException {
		super(parameters);
	}

	@Override
	MinimizerProblemOjAlgo makeProblem() throws IOException, NumberFormatException {
		//calculates relevantBranchNumbers, branches2Rows and rows2Branches
		calculateBranchesAndTranslationToRows();
		
		//calculates relevantTraceNumbers, traces2Cols and cols2Traces
		calculateTracesAndTranslationToColumns();

		
		//creates the model
		final ExpressionsBasedModel modelOjAlgo = createModel();
		
		//returns the problem
		return new MinimizerProblemOjAlgo(this.parameters, modelOjAlgo, this.cols, this.cols2Traces);
	}
	
	private void calculateBranchesAndTranslationToRows() {
		this.relevantBranchNumbers = new TreeSet<>(this.branchNumbers);
		this.relevantBranchNumbers.removeAll(this.branchNumbersToIgnore);
		this.branches2Rows = new HashMap<>();
		this.rows2Branches = new ArrayList<>();
		int row = 0;
		for (Integer branchNumber : this.relevantBranchNumbers) {
			this.branches2Rows.put(branchNumber, row++);
			this.rows2Branches.add(branchNumber);
		}
	}
	
	private void calculateTracesAndTranslationToColumns() {
		this.relevantTraceNumbers = new TreeSet<>(this.traceNumbers);
		this.relevantTraceNumbers.removeAll(this.traceNumbersToIgnore);
		this.traces2Cols = new HashMap<>();
		this.cols2Traces = new ArrayList<>();
		int col = 0;
		for (Integer traceNumber : this.relevantTraceNumbers) {
			this.traces2Cols.put(traceNumber, col++);
			this.cols2Traces.add(traceNumber);
		}
	}

	private ExpressionsBasedModel createModel() throws IOException {
		//creates the model
		final ExpressionsBasedModel retVal = new ExpressionsBasedModel();
		
		//creates the variables (columns)
		final Variable[] variables = new Variable[this.cols];
		for (int traceNumber = 0; traceNumber < this.cols; ++traceNumber) {
			variables[traceNumber] = retVal.addVariable("trace" + (traceNumber + 1)).binary();
		}
		
		//creates the expressions (rows)
		final Expression[] expressions = new Expression[this.rows];
		for (int branchNumber = 0; branchNumber < this.rows; ++branchNumber) {
			expressions[branchNumber] = retVal.addExpression("branch" + (branchNumber + 1)).lower(ONE);
		}
		
		//reads the information from the file and builds the model
		try (final BufferedReader r = Files.newBufferedReader(this.parameters.getCoverageFilePath())) {
			String line;
			int traceNumber = 0;
			while ((line = r.readLine()) != null) {
				if (this.traceNumbersToIgnore.contains(traceNumber)) {
					++traceNumber;
					continue;
				}
				
				final int col = this.traces2Cols.get(traceNumber);
				
				final String[] fields = line.split(",");
				//field 0 is the method to test and field 1 is the local trace number, 
				//do not care here
				
				//sets the cost
				final int cost = Integer.parseInt(fields[2].trim());
				variables[col].weight(cost);
				
				//sets the constraint
				final TreeSet<Integer> uncoveredByTraceBranchNumbers = new TreeSet<>(this.relevantBranchNumbers); //the branch numbers not covered by this trace
				for (int i = 3; i < fields.length; ++i) {
					final int branchNumber = Integer.parseInt(fields[i].trim());
					if (this.branchNumbersToIgnore.contains(branchNumber)) {
						continue;
					}
					uncoveredByTraceBranchNumbers.remove(branchNumber);
					//sets a_i_j to 1
					expressions[this.branches2Rows.get(branchNumber)].set(col, ONE);
				}
				for (int branchNumber : uncoveredByTraceBranchNumbers) {
					//sets a_i_j to 0
					expressions[this.branches2Rows.get(branchNumber)].set(col, ZERO);
				}
				++traceNumber;
			}
		}
		
		return retVal;
	}
}
