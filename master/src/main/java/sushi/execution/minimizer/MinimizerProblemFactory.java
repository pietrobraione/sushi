package sushi.execution.minimizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.TreeSet;

import sushi.configure.MinimizerParameters;
import sushi.exceptions.TerminationException;

abstract class MinimizerProblemFactory<P extends MinimizerProblem> {
	/** The parameters. */
	protected final MinimizerParameters parameters;

	/** The total number of branches. */
	protected final int nBranches;
	 
	/** The total number of traces. */
	protected final int nTraces;

	/** The set containing all the branch numbers. */
	protected final TreeSet<Integer> branchNumbers;

	/** The set containing all the trace numbers. */
	protected final TreeSet<Integer> traceNumbers; 

	/**
	 * The set of all the branch numbers that must be ignored 
	 * because they are not coverage targets.
	 */
	protected final TreeSet<Integer> branchNumbersToIgnore;

	/**
	 * The set of all the trace numbers that must be ignored 
	 * because they have been already tried during previous
	 * iterations.
	 */
	protected final TreeSet<Integer> traceNumbersToIgnore;

	/** The number of rows in the linear problem. */
	protected int rows;

	/** The number of columns in the linear problem. */
	protected int cols;
	
	MinimizerProblemFactory(MinimizerParameters parameters) throws IOException {
		this.parameters = parameters;
		this.nBranches = (int) Files.lines(this.parameters.getBranchesFilePath()).count();
		this.nTraces = (int) Files.lines(this.parameters.getCoverageFilePath()).count();
		
		//branch numbers are just all the numbers between 0 and nBranches - 1
		this.branchNumbers = new TreeSet<>();
		for (int branchNumber = 0; branchNumber < this.nBranches; ++branchNumber) {
			this.branchNumbers.add(branchNumber);
		}
		
		//trace numbers are just all the numbers between 0 and nTraces - 1
		this.traceNumbers = new TreeSet<>();
		for (int traceNumber = 0; traceNumber < this.nTraces; ++traceNumber) {
			this.traceNumbers.add(traceNumber);
		}
		
		//the branches to ignore are those that the user do not want to cover
		this.branchNumbersToIgnore = branchNumbersToIgnore();
		
		//also, we shall ignore all the branches that are not covered
		//by any trace (it might happen at first iteration, since a JBSE
		//instance may record a branch in the branches file, and then 
		//crash before producing any coverage info)
		this.branchNumbersToIgnore.addAll(branchesThatMayNotBeCovered());
		
		//the branches to ignore are those that the user do not want to cover
		this.traceNumbersToIgnore = traceNumbersToIgnore();

		//calculates the number of rows and cols for the optimization problem
		this.rows = this.nBranches - this.branchNumbersToIgnore.size();
		this.cols = this.nTraces - this.traceNumbersToIgnore.size();
		if (this.rows == 0 || this.cols == 0) {
			throw new TerminationException("Minimizer invoked with no branches to cover and/or no traces that cover the uncovered branches");
		}
	}
	
	void ignore(List<Integer> traceNumbers) throws IOException, NumberFormatException {
		this.traceNumbersToIgnore.addAll(traceNumbers);
		this.branchNumbersToIgnore.addAll(notCoveredBranches());
		this.rows = this.nBranches - this.branchNumbersToIgnore.size();
		this.cols = this.nTraces - this.traceNumbersToIgnore.size();
	}
	
	boolean isEmpty() {
		return this.rows == 0 || this.cols == 0;
	}

	private TreeSet<Integer> branchNumbersToIgnore() throws IOException {
		final TreeSet<Integer> retVal = new TreeSet<>();
		try (final BufferedReader r = Files.newBufferedReader(this.parameters.getBranchesToIgnoreFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				retVal.add(Integer.parseInt(line.trim()));
			}
		}
		return retVal;
	}
	
	private TreeSet<Integer> branchesThatMayNotBeCovered() throws IOException {
		final TreeSet<Integer> retVal = new TreeSet<>(this.branchNumbers);
		try (final BufferedReader r = Files.newBufferedReader(this.parameters.getCoverageFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				final String[] fields = line.split(",");
				for (int i = 3; i < fields.length; ++i) {
					final int branchNumber = Integer.parseInt(fields[i].trim());
					retVal.remove(branchNumber);
				}
			}
		}
		return retVal;
	}
	
	private TreeSet<Integer> traceNumbersToIgnore() throws IOException {
		final TreeSet<Integer> retVal = new TreeSet<>();
		try (final BufferedReader r = Files.newBufferedReader(this.parameters.getTracesToIgnoreFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				retVal.add(Integer.parseInt(line.trim()));
			}
		}
		return retVal;
	}
	
	private TreeSet<Integer> notCoveredBranches() throws IOException, NumberFormatException {
		final TreeSet<Integer> mayBeCovered = new TreeSet<>();
		try (final BufferedReader r = Files.newBufferedReader(this.parameters.getCoverageFilePath())) {
			String line;
			int traceNumber = 0;
			while ((line = r.readLine()) != null) {
				if (this.traceNumbersToIgnore.contains(traceNumber)) {
					++traceNumber;
					continue;
				}
				final String[] fields = line.split(",");
				for (int i = 3; i < fields.length; ++i) {
					final int branchNumber = Integer.parseInt(fields[i].trim());
					mayBeCovered.add(branchNumber);
				}
				++traceNumber;
			}
		}
		final TreeSet<Integer> retVal = new TreeSet<Integer>(this.branchNumbers);
		retVal.removeAll(mayBeCovered);
		return retVal;
	}
	
	/**
	 * Builds a MIP problem for detecting the optimal subset of all 
	 * traces that covers the same set of branches. The MIP problem 
	 * is structured as follows.
	 * <pre>
	 * Minimize c_1 * x_1 + c_2 * x_2 + ... c_t * x_t
	 * subject to
	 *   a_1_1 * x_1 + ... + a_1_t * x_t >= 1
	 *   ...
	 *   a_b_1 * x_1 + ... + a_b_t * x_t >= 1
	 * where
	 *   x_1 binary
	 *   ...
	 *   x_t binary
	 * </pre>
	 * where t is the number of traces, x_1 ... x_t are binary variables
	 * stating whether the associated trace belongs to the optimal subset
	 * or not, c_1 ... c_t are the costs of the traces, b is the number of 
	 * branches, a_i_j is 1 if trace j covers branch i, otherwise 0. Actually 
	 * the problem is slightly more complex than that because the method allows
	 * to exclude some branches and some traces.
	 * 
	 * @return a {@link MinimizerProblem}.
	 * @throws IOException if reading some file fails.
	 * @throws NumberFormatException if some file has wrong format.
	 */
	abstract P makeProblem() throws IOException, NumberFormatException;
}
