package sushi.execution.minimizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.TreeSet;

import org.gnu.glpk.*;

import sushi.configure.MinimizerParameters;
import sushi.exceptions.MinimizerException;
import sushi.exceptions.TerminationException;

public class RunMinimizer {
	private final MinimizerParameters parameters;
	
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
		//nBranches is the total number of branches, nTraces is the total number of traces
		final int nBranches, nTraces;
		try {
			nBranches = (int) Files.lines(this.parameters.getBranchesFilePath()).count();
			nTraces = (int) Files.lines(this.parameters.getCoverageFilePath()).count();
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		
		//branch numbers are just all the numbers between 0 and nBranches - 1
		final TreeSet<Integer> branchNumbers = new TreeSet<>();
		for (int branchNumber = 0; branchNumber < nBranches; ++branchNumber) {
			branchNumbers.add(branchNumber);
		}
		
		//trace numbers are just all the numbers between 0 and nTraces - 1
		final TreeSet<Integer> traceNumbers = new TreeSet<>();
		for (int traceNumber = 0; traceNumber < nTraces; ++traceNumber) {
			traceNumbers.add(traceNumber);
		}
		
		//the branches to ignore are those that the user do not want to cover
		final TreeSet<Integer> branchNumbersToIgnore;
		try {
			branchNumbersToIgnore = branchNumbersToIgnore();
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		
		//the branches to ignore are those that the user do not want to cover
		final TreeSet<Integer> traceNumbersToIgnore;
		try {
			traceNumbersToIgnore = traceNumbersToIgnore();
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		
		//generates the optimal solution and emits it; then
		//generates more solutions until the emitted rows
		//saturate the number of tasks
		ArrayList<Integer> solution;
		int emittedRows = 0;
		boolean firstIteration = true;
		do {
			//makes the GLPK problem
			final glp_prob p;
			try {
				p = makeProblem(nBranches, nTraces, branchNumbers, traceNumbers, branchNumbersToIgnore, traceNumbersToIgnore);
			} catch (IOException | NumberFormatException e) {
				e.printStackTrace();
				return 1;
			}

			//solves it
			final glp_iocp iocp = new glp_iocp();
			GLPK.glp_init_iocp(iocp);
			iocp.setMsg_lev(GLPK.GLP_MSG_OFF);
			iocp.setPresolve(GLPK.GLP_ON);
			final int res = GLPK.glp_intopt(p, iocp);
			if (res != 0) {
				return (firstIteration ? 1 : 0);
			}
			if (Thread.interrupted()) {
				return (firstIteration ? 1 : 0);
			}
			final int status = GLPK.glp_mip_status(p);
			if (status != GLPK.GLP_OPT && status != GLPK.GLP_FEAS) {
				throw new TerminationException("Minimizer was unable to find a set of traces that covers the uncovered branches");
			}

			//gets the solution and emits it
			solution = makeSolution(p, nTraces);
			try {
				emitSolution(solution, !firstIteration);
			} catch (IOException e) {
				e.printStackTrace();
				return 1;
			}
			
			traceNumbersToIgnore.addAll(solution);
			emittedRows += solution.size();

			//disposes garbage
			GLPK.glp_delete_prob(p);
			
			firstIteration = false;
		} while (emittedRows < parameters.getNumberOfTasks());
		
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
	 * @param nBranches the number of branches.
	 * @param nTraces the number of traces.
	 * @param branchNumbers the set of all the branch numbers.
	 * @param traceNumbers the set of all the trace numbers.
	 * @param branchNumbersToIgnore the set of all the branch numbers that
	 *        must be ignored because they are not coverage targets.
	 * @param branchNumbersToIgnore the set of all the trace numbers that
	 *        must be ignored because they have been already tried.
	 * @param solutionsToIgnore a list of solutions of the MIP problem
	 *        that must be ignored.
	 * @return a GLPK problem.
	 * @throws IOException if reading some file fails.
	 * @throws NumberFormatException if some file has wrong format.
	 */
	private glp_prob makeProblem(int nBranches, int nTraces, TreeSet<Integer> branchNumbers, TreeSet<Integer> traceNumbers, 
			TreeSet<Integer> branchNumbersToIgnore, TreeSet<Integer> traceNumbersToIgnore) 
	throws IOException, NumberFormatException {
		final ArrayList<Integer> costs = new ArrayList<>();  //list of all costs
		//the next three arrays encode the [a_i_j] matrix of the linear constraints. 
		//The thing works as follows: ar stores the coefficients as 
		//ar = {a_1_1, ..., a_1_t, a_2_1, ..., a_2_t, ...} 
		//ia = {1,     ..., 1    , 2,     ..., 2,     ...}
		//ja = {1,     ..., t    , 1,     ..., t,     ...}
		//(actually, they are not in this exact order)
		final int rows = nBranches + traceNumbersToIgnore.size(); //the number of rows
		final int cols = nTraces; //the number of cols
		final SWIGTYPE_p_int ia = GLPK.new_intArray(rows * cols + 1); //the row (i) indices of the a_i_k coefficients
		final SWIGTYPE_p_int ja = GLPK.new_intArray(rows * cols + 1); //the column (j) indices of the a_i_k coefficients
		final SWIGTYPE_p_double ar = GLPK.new_doubleArray(rows * cols + 1); //all the a_i_j coefficients
		//the three arrays above have 1 element more than rows * cols because GLPK
		//strangely wants you to store everything starting from position 1
		final TreeSet<Integer> uncoveredBranchNumbers = new TreeSet<>(branchNumbers); //the branch numbers not covered by any trace
		int pos = 0;
		
		//reads coverage information and generates the constraint matrix rows for trace coverage
		try (final BufferedReader r = Files.newBufferedReader(this.parameters.getCoverageFilePath())) {
			String line;
			int traceNumber = 0;
			while ((line = r.readLine()) != null) {
				final String[] fields = line.split(",");
				//field 0 is the method to test and field 1 is the local trace number, 
				//do not care here
				final int cost = Integer.parseInt(fields[2].trim());
				costs.add(cost);
				final TreeSet<Integer> uncoveredByTraceBranchNumbers = new TreeSet<>(branchNumbers); //the branch numbers not covered by this trace
				for (int i = 3; i < fields.length; ++i) {
					final int branchNumber = Integer.parseInt(fields[i].trim());
					uncoveredByTraceBranchNumbers.remove(branchNumber);
					uncoveredBranchNumbers.remove(branchNumber);
					//sets a_i_j to 1
					GLPK.intArray_setitem(ia, pos + 1, branchNumber + 1);
					GLPK.intArray_setitem(ja, pos + 1, traceNumber + 1);
					GLPK.doubleArray_setitem(ar, pos + 1, 1.0);
					++pos;
				}
				for (int branchNumber : uncoveredByTraceBranchNumbers) {
					//sets a_i_j to 0
					GLPK.intArray_setitem(ia, pos + 1, branchNumber + 1);
					GLPK.intArray_setitem(ja, pos + 1, traceNumber + 1);
					GLPK.doubleArray_setitem(ar, pos + 1, 0.0);
					++pos;
				}
				++traceNumber;
			}
		}
		
		//generates the constraint matrix rows for the trace numbers to ignore
		{
			int row = nBranches + 1; 
			for (final Integer traceNumberToIgnore : traceNumbersToIgnore) {
				//sets a_i_j to 1
				GLPK.intArray_setitem(ia, pos + 1, row);
				GLPK.intArray_setitem(ja, pos + 1, traceNumberToIgnore + 1);
				GLPK.doubleArray_setitem(ar, pos + 1, 1.0);
				++pos;
				final TreeSet<Integer> notToIgnoreTraceNumbers = new TreeSet<>(traceNumbers); //the trace numbers different from this trace
				notToIgnoreTraceNumbers.remove(traceNumberToIgnore);
				for (int traceNumber : notToIgnoreTraceNumbers) {
					//sets a_i_j to 0
					GLPK.intArray_setitem(ia, pos + 1, row);
					GLPK.intArray_setitem(ja, pos + 1, traceNumber + 1);
					GLPK.doubleArray_setitem(ar, pos + 1, 0);
					++pos;
				}
				++row;
			}
		}
		
		//generates the problem
		glp_prob p = GLPK.glp_create_prob();
		GLPK.glp_set_prob_name(p, "setCoverage");
		GLPK.glp_add_rows(p, rows);
		for (int row = 1; row <= nBranches; ++row) {
			final int branchNumber = row - 1;
			GLPK.glp_set_row_name(p, row, "branch" + branchNumber);
			final boolean unconstrained = uncoveredBranchNumbers.contains(branchNumber) || branchNumbersToIgnore.contains(branchNumber);
			if (unconstrained) {
				//nothing
			} else {
				GLPK.glp_set_row_bnds(p, row, GLPK.GLP_LO, 1.0, 0.0);
			}
		}
		for (int row = nBranches + 1; row <= rows; ++row) {
			final int traceNumber = row - nBranches - - 1;
			GLPK.glp_set_row_name(p, row, "trace" + traceNumber);
			GLPK.glp_set_row_bnds(p, row, GLPK.GLP_FX, 0.0, 0.0);
		}
		GLPK.glp_add_cols(p, cols);
		for (int col = 1; col <= cols; ++col) {
			final int traceNumber = col - 1;
			GLPK.glp_set_col_name(p, col, "trace" + traceNumber);
			GLPK.glp_set_col_kind(p, col, GLPK.GLP_BV); //BV = binary variable
		}
		GLPK.glp_load_matrix(p, pos, ia, ja, ar);
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

	private ArrayList<Integer> makeSolution(glp_prob p, int nTraces) {
		final int cols = nTraces; //pleonastic
		final ArrayList<Integer> retVal = new ArrayList<>();
		for (int col = 1; col <= cols; ++col) {
			final double val = GLPK.glp_mip_col_val(p, col);
			if (val >= 1) {
				final int traceNumberGlobal = col - 1;
				retVal.add(traceNumberGlobal);
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
}
