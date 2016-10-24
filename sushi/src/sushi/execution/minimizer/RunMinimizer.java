package sushi.execution.minimizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gnu.glpk.*;

import sushi.configure.MinimizerParameters;
import sushi.exceptions.MinimizerException;

public class RunMinimizer {
	private final MinimizerParameters parameters;
	
	public RunMinimizer(MinimizerParameters parameters) {
		this.parameters = parameters;
	}
	
	public int run() {
		final int rows, cols;
		try {
			rows = (int) Files.lines(this.parameters.getBranchesFilePath()).count();
			cols = (int) Files.lines(this.parameters.getCoverageFilePath()).count();
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		
		final TreeSet<Integer> branchNumbers = new TreeSet<>();
		for (int branchNumber = 0; branchNumber < rows; ++branchNumber) {
			branchNumbers.add(branchNumber);
		}
		
		final TreeSet<Integer> branchNumbersToIgnore;
		try {
			branchNumbersToIgnore = branchNumbersToIgnore(branchNumbers);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		
		
		final glp_prob p;
		try {
			p = makeProblem(rows, cols, branchNumbers, branchNumbersToIgnore);
		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
			return 1;
		}
		
		final glp_iocp iocp = new glp_iocp();
		GLPK.glp_init_iocp(iocp);
		iocp.setPresolve(GLPK.GLP_ON);
		final int res = GLPK.glp_intopt(p, iocp);
		if (res != 0) {
			return 1;
		}
		
		try {
			emitOutput(p, cols);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		GLPK.glp_delete_prob(p);
		return 0;
	}
	
	private TreeSet<Integer> branchNumbersToIgnore(TreeSet<Integer> branchNumbers) throws IOException {
		final TreeSet<Integer> retVal;
		if (this.parameters.getBranchesToIgnore() != null) {
			retVal = new TreeSet<>();
			try (final BufferedReader r = Files.newBufferedReader(this.parameters.getBranchesFilePath())) {
				final Pattern p = this.parameters.getBranchesToIgnore();
				int branchNumber = 0;
				String line;
				while ((line = r.readLine()) != null) {
					final Matcher m = p.matcher(line.trim());
					if (m.matches()) {
						retVal.add(branchNumber);
					}
					++branchNumber;
				}
			}
		} else if (this.parameters.getBranchesToCover() != null) {
			retVal = new TreeSet<>(branchNumbers);
			try (final BufferedReader r = Files.newBufferedReader(this.parameters.getBranchesFilePath())) {
				final Pattern p = this.parameters.getBranchesToCover();
				int branchNumber = 0;
				String line;
				while ((line = r.readLine()) != null) {
					final Matcher m = p.matcher(line.trim());
					if (m.matches()) {
						retVal.remove(branchNumber);
					}
					++branchNumber;
				}
			}
		} else {
			retVal = new TreeSet<>();
		}

		return retVal;
	}
	
	private glp_prob makeProblem(int rows, int cols, TreeSet<Integer> branchNumbers, TreeSet<Integer> branchNumbersToIgnore) 
	throws IOException, NumberFormatException {
		final ArrayList<Integer> traces = new ArrayList<>();
		final ArrayList<Integer> costs = new ArrayList<>();
		final SWIGTYPE_p_int ia = GLPK.new_intArray(rows * cols);
		final SWIGTYPE_p_int ja = GLPK.new_intArray(rows * cols);
		final SWIGTYPE_p_double ar = GLPK.new_doubleArray(rows * cols);
		final TreeSet<Integer> uncoveredBranchNumbers = new TreeSet<>(branchNumbers);
		int pos = 0;
		try (final BufferedReader r = Files.newBufferedReader(this.parameters.getCoverageFilePath())) {
			String line;
			int traceNumber = 0;
			while ((line = r.readLine()) != null) {
				final String[] fields = line.split(",");
				//field 0 is the method to test, does not care here
				final int trace = Integer.parseInt(fields[1].trim());
				traces.add(trace);
				final int cost = Integer.parseInt(fields[2].trim());
				costs.add(cost);
				final TreeSet<Integer> unusedBranchNumbers = new TreeSet<>(branchNumbers);
				for (int i = 3; i < fields.length; ++i) {
					final int branchNumber = Integer.parseInt(fields[i].trim());
					unusedBranchNumbers.remove(branchNumber);
					uncoveredBranchNumbers.remove(branchNumber);
					GLPK.intArray_setitem(ia, pos + 1, branchNumber + 1);
					GLPK.intArray_setitem(ja, pos + 1, traceNumber + 1);
					GLPK.doubleArray_setitem(ar, pos + 1, 1.0);
					++pos;
				}
				for (int branchNumber : unusedBranchNumbers) {
					GLPK.intArray_setitem(ia, pos + 1, branchNumber + 1);
					GLPK.intArray_setitem(ja, pos + 1, traceNumber + 1);
					GLPK.doubleArray_setitem(ar, pos + 1, 0.0);
					++pos;
				}
				++traceNumber;
			}
		}
		
		glp_prob p = GLPK.glp_create_prob();
		GLPK.glp_set_prob_name(p, "setCoverage");
		GLPK.glp_add_rows(p, rows);
		for (int row = 1; row <= rows; ++row) {
			final int branchNumber = row - 1;
			GLPK.glp_set_row_name(p, row, "branch" + branchNumber);
			final boolean unconstrained = uncoveredBranchNumbers.contains(branchNumber) || branchNumbersToIgnore.contains(branchNumber);
			if (unconstrained) {
				//nothing
			} else {
				GLPK.glp_set_row_bnds(p, row, GLPK.GLP_LO, 1.0, 0.0);
			}
		}
		GLPK.glp_add_cols(p, cols);
		for (int col = 1; col <= cols; ++col) {
			final int traceNumber = col - 1;
			GLPK.glp_set_col_name(p, col, "trace" + traceNumber);
			GLPK.glp_set_col_kind(p, col, GLPK.GLP_BV);
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
		
		return p;
	}
	
	private void emitOutput(glp_prob p, int cols) throws IOException {
		try (final BufferedWriter wOutput = Files.newBufferedWriter(this.parameters.getOutputFilePath())) {
			for (int col = 1; col <= cols; ++col) {
				final double val = GLPK.glp_mip_col_val(p, col);
				if (val >= 1) {
					final int traceNumberGlobal = col - 1;
										
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
}
