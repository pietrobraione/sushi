package sushi.execution.minimizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.gnu.glpk.*;

import sushi.configure.MinimizerParameters;
import sushi.exceptions.MinimizerException;
import sushi.exceptions.TerminationException;

public class RunMinimizer {
	private final MinimizerParameters parameters;
	/**
	 * the number of branches
	 */
	private int nBranches;
	 /**
	  * the number of traces
	  */
	private int nTraces;
	/**
	 * the set of all the branch numbers
	 */
	private TreeSet<Integer> branchNumbers;
	/**
	 * the set of all the trace numbers
	 */
	private TreeSet<Integer> traceNumbers; 
	/**
	 * the set of all the branch numbers that must be ignored 
	 * because they are not coverage targets
	 */
	private TreeSet<Integer> branchNumbersToIgnore;
	/**
	 * the set of all the trace numbers that must be ignored 
	 * because they have been already tried; progressively
	 * enriched by successive iteration of the linear problem
	 * solution
	 */
	private TreeSet<Integer> traceNumbersToIgnore;
	/**
	 * the number of rows in the linear problem
	 */
	int rows;
	/**
	 * the number of columns in the linear problem
	 */
	int cols;
	/**
	 * maps every column number (1 to cols) to the 
	 * corresponding trace number
	 */
	ArrayList<Integer> cols2Traces;
	
	public RunMinimizer(MinimizerParameters parameters) {
		this.parameters = parameters;
	}
	
	/**
	 * Detects the optimal cost subset of traces that cover all the 
	 * (desired) branches that the set of all the traces covers. 
	 * 
	 * @return 0 if everything ok, >=1 if some error.
	 */
	public int run() {
		final long start = System.currentTimeMillis();
		
		//nBranches is the total number of branches, nTraces is the total number of traces
		try {
			this.nBranches = (int) Files.lines(this.parameters.getBranchesFilePath()).count();
			this.nTraces = (int) Files.lines(this.parameters.getCoverageFilePath()).count();
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		
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
		try {
			this.branchNumbersToIgnore = branchNumbersToIgnore();
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		
		//the branches to ignore are those that the user do not want to cover
		try {
			this.traceNumbersToIgnore = traceNumbersToIgnore();
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		//calculates the number of rows and cols for the optimization problem
		this.rows = this.nBranches - this.branchNumbersToIgnore.size();
		this.cols = this.nTraces - this.traceNumbersToIgnore.size();
		if (this.rows == 0 || this.cols == 0) {
			throw new TerminationException("Minimizer invoked with no branches to cover and/or no traces that cover the uncovered branches");
		}


		//generates the optimal solution and emits it; then
		//generates more solutions until the emitted rows
		//saturate the number of tasks
		int emittedRows = 0;
		boolean firstIteration = true;
		do {
			//makes the GLPK problem
			final glp_prob p;
			try {
				p = makeProblem();
			} catch (IOException | NumberFormatException e) {
				e.printStackTrace();
				return 1;
			}

			//solves it
			final glp_iocp iocp = new glp_iocp();
			GLPK.glp_init_iocp(iocp);
			iocp.setMsg_lev(GLPK.GLP_MSG_OFF);
			iocp.setTm_lim(this.parameters.getTimeout() * 200);
			iocp.setPresolve(GLPK.GLP_ON);
			final int res = GLPK.glp_intopt(p, iocp);
			if (res != 0 && res != GLPK.GLP_ETMLIM) {
				return (firstIteration ? 1 : 0);
			}
			final int status = GLPK.glp_mip_status(p);
			if (status != GLPK.GLP_OPT && status != GLPK.GLP_FEAS) {
				throw new TerminationException("Minimizer was unable to find a set of traces that covers the uncovered branches");
			}

			//gets the solution and emits it
			final ArrayList<Integer> solution = makeSolution(p);
			try {
				emitSolution(solution, !firstIteration);
				this.traceNumbersToIgnore.addAll(solution);
				this.branchNumbersToIgnore.addAll(notCoveredBranches());
				emittedRows += solution.size();
			} catch (IOException | NumberFormatException e) {
				e.printStackTrace();
				return 1;
			}

			//disposes garbage
			GLPK.glp_delete_prob(p);
			
			firstIteration = false;
			if (System.currentTimeMillis() - start > this.parameters.getTimeout() * 1000) {
				return 0;
			}

			//calculates the number of rows and cols for the next iteration optimization problem
			this.rows = this.nBranches - this.branchNumbersToIgnore.size();
			this.cols = this.nTraces - this.traceNumbersToIgnore.size();
		} while (emittedRows < this.parameters.getNumberOfTasks() && this.rows > 0 && this.cols > 0);
		
		return 0;
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
	
	/**
	 * Builds a MIP problem for detecting the optimal subset of all 
	 * traces that covers the same set of branches. The MIP problem 
	 * is structured as follows.
	 * <pre>
	 * Minimize c_1 * x_1 + c_2 * x_2 + ... c_t * x_t
	 * subject to
	 *   a_1_1 * x_1 + ... + a_1_t * x_t >= 1 (or 0)
	 *   ...
	 *   a_b_1 * x_1 + ... + a_b_t * x_t >= 1 (or 0)
	 * where
	 *   x_1 binary
	 *   ...
	 *   x_t binary
	 * </pre>
	 * where t is the number of traces, x_1 ... x_t are binary variables
	 * stating whether the associated trace belongs to the optimal subset
	 * or not, c_1 ... c_t are the costs of the traces, b is the number of 
	 * branches, a_i_j is 1 if trace j covers branch i, otherwise 0, the
	 * linear constraints are >= 1 if the branch associated to the row 
	 * must be covered, >= 0 if the branch is not a coverage target or is 
	 * not covered by any trace in the set of all the traces. Actually the
	 * problem is slightly more complex than that because the method allows
	 * to exclude some branches and some traces.
	 * 
	 * @return a GLPK problem.
	 * @throws IOException if reading some file fails.
	 * @throws NumberFormatException if some file has wrong format.
	 */
	private glp_prob makeProblem() throws IOException, NumberFormatException {
		final ArrayList<Integer> costs = new ArrayList<>();  //list of all costs
		//the next three arrays encode the [a_i_j] matrix of the linear constraints. 
		//The thing works as follows: ar stores the coefficients as 
		//ar = {a_1_1, ..., a_1_t, a_2_1, ..., a_2_t, ...} 
		//ia = {1,     ..., 1    , 2,     ..., 2,     ...}
		//ja = {1,     ..., t    , 1,     ..., t,     ...}
		//(actually, they are not in this exact order)
		final SWIGTYPE_p_int ia = GLPK.new_intArray(this.rows * this.cols + 1); //the row (i) indices of the a_i_k coefficients
		final SWIGTYPE_p_int ja = GLPK.new_intArray(this.rows * this.cols + 1); //the column (j) indices of the a_i_k coefficients
		final SWIGTYPE_p_double ar = GLPK.new_doubleArray(this.rows * this.cols + 1); //all the a_i_j coefficients
		//the three arrays above have 1 element more than rows * cols because GLPK
		//strangely wants you to store everything starting from position 1


		//calculates the relevant (i.e., to not ignore) branch numbers, and 
		//the variables branches2Rows and rows2Branches, which map in 
		//both directions branch numbers with their respective row numbers 
		//in the linear problem
		final TreeSet<Integer> relevantBranchNumbers = new TreeSet<>(this.branchNumbers);
		relevantBranchNumbers.removeAll(this.branchNumbersToIgnore);
		final HashMap<Integer, Integer> branches2Rows = new HashMap<>();
		final ArrayList<Integer> rows2Branches = new ArrayList<>();
		rows2Branches.add(0); //skips index 0 because rows are counted starting from 1
		{
			int row = 1;
			for (Integer branchNumber : relevantBranchNumbers) {
				branches2Rows.put(branchNumber, row++);
				rows2Branches.add(branchNumber);
			}
		}
		
		//similarly for traces and columns of the problem
		final TreeSet<Integer> relevantTraceNumbers = new TreeSet<>(this.traceNumbers);
		relevantTraceNumbers.removeAll(this.traceNumbersToIgnore);
		final HashMap<Integer, Integer> traces2Cols = new HashMap<>();
		this.cols2Traces = new ArrayList<>();
		this.cols2Traces.add(0); //skips index 0 because cols are counted starting from 1
		{
			int col = 1;
			for (Integer traceNumber : relevantTraceNumbers) {
				traces2Cols.put(traceNumber, col++);
				this.cols2Traces.add(traceNumber);
			}
		}
		
		//reads coverage information and generates the constraint matrix rows for trace coverage
		int pos = 1; //current position in ia, ja and ar; starts from position 1 as required by GLPK interface
		try (final BufferedReader r = Files.newBufferedReader(this.parameters.getCoverageFilePath())) {
			String line;
			int traceNumber = 0;
			while ((line = r.readLine()) != null) {
				if (this.traceNumbersToIgnore.contains(traceNumber)) {
					++traceNumber;
					continue;
				}
				final String[] fields = line.split(",");
				//field 0 is the method to test and field 1 is the local trace number, 
				//do not care here
				final int cost = Integer.parseInt(fields[2].trim());
				costs.add(cost);
				final TreeSet<Integer> uncoveredByTraceBranchNumbers = new TreeSet<>(relevantBranchNumbers); //the branch numbers not covered by this trace
				for (int i = 3; i < fields.length; ++i) {
					final int branchNumber = Integer.parseInt(fields[i].trim());
					if (this.branchNumbersToIgnore.contains(branchNumber)) {
						continue;
					}
					uncoveredByTraceBranchNumbers.remove(branchNumber);
					//sets a_i_j to 1
					GLPK.intArray_setitem(ia, pos, branches2Rows.get(branchNumber));
					GLPK.intArray_setitem(ja, pos, traces2Cols.get(traceNumber));
					GLPK.doubleArray_setitem(ar, pos, 1.0);
					++pos;
				}
				for (int branchNumber : uncoveredByTraceBranchNumbers) {
					//sets a_i_j to 0
					GLPK.intArray_setitem(ia, pos, branches2Rows.get(branchNumber));
					GLPK.intArray_setitem(ja, pos, traces2Cols.get(traceNumber));
					GLPK.doubleArray_setitem(ar, pos, 0.0);
					++pos;
				}
				++traceNumber;
			}
		}
		
		//generates the problem
		glp_prob p = GLPK.glp_create_prob();
		GLPK.glp_set_prob_name(p, "setCoverage");
		GLPK.glp_add_rows(p, this.rows);
		for (int row = 1; row <= this.rows; ++row) {
			final int branchNumber = rows2Branches.get(row);
			GLPK.glp_set_row_name(p, row, "branch" + branchNumber);
			GLPK.glp_set_row_bnds(p, row, GLPK.GLP_LO, 1.0, 0.0);
		}
		GLPK.glp_add_cols(p, this.cols);
		for (int col = 1; col <= this.cols; ++col) {
			final int traceNumber = this.cols2Traces.get(col);
			GLPK.glp_set_col_name(p, col, "trace" + traceNumber);
			GLPK.glp_set_col_kind(p, col, GLPK.GLP_BV); //BV = binary variable
		}
		GLPK.glp_load_matrix(p, pos - 1, ia, ja, ar);
		GLPK.glp_set_obj_name(p, "cost");
		GLPK.glp_set_obj_dir(p, GLPK.GLP_MIN);
		{
			int i = 1;
			for (int cost : costs) {
				GLPK.glp_set_obj_coef(p, i, cost);
				++i;
			}
		}
		
		//disposes garbage
		GLPK.delete_doubleArray(ar);
		GLPK.delete_intArray(ja);
		GLPK.delete_intArray(ia);
		
		return p;
	}

	private ArrayList<Integer> makeSolution(glp_prob p) {
		final ArrayList<Integer> retVal = new ArrayList<>();
		for (int col = 1; col <= this.cols; ++col) {
			final double val = GLPK.glp_mip_col_val(p, col);
			if (val >= 1) {
				final int traceNumber = this.cols2Traces.get(col);
				retVal.add(traceNumber);
			}
		}
		return retVal;
	}

	private void emitSolution(ArrayList<Integer> solution, boolean append) throws IOException {
		final OpenOption[] options = (append ? new OpenOption[]{ StandardOpenOption.APPEND } : new OpenOption[]{ StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE });
		try (final BufferedWriter wOutput = Files.newBufferedWriter(this.parameters.getOutputFilePath(), options)) {
			for (int traceNumberGlobal : solution) {
				int methodNumber = -1, traceNumberLocal = -1;
				try (final BufferedReader r = Files.newBufferedReader(this.parameters.getCoverageFilePath())) {
					int current = 0;
					String line;
					while ((line = r.readLine()) != null) {
						if (current == traceNumberGlobal) {
							final String[] fields = line.split(",");
							methodNumber = Integer.parseInt(fields[0].trim());
							traceNumberLocal = Integer.parseInt(fields[1].trim());
						}
						++current;
					}
				}

				if (methodNumber == -1 || traceNumberLocal == -1) {
					throw new MinimizerException("Method not found");
				}

				wOutput.write(Integer.toString(traceNumberGlobal));
				wOutput.write(", ");
				wOutput.write(Integer.toString(methodNumber));
				wOutput.write(", ");
				wOutput.write(Integer.toString(traceNumberLocal));
				wOutput.newLine();
			}
		}
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
}
