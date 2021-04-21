package sushi.execution.minimizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_prob;

import sushi.configure.MinimizerParameters;

final class MinimizerProblemFactoryGLPK extends MinimizerProblemFactory<MinimizerProblemGLPK> {
	MinimizerProblemFactoryGLPK(MinimizerParameters parameters) throws IOException {
		super(parameters);
	}

	@Override
	MinimizerProblemGLPK makeProblem() throws IOException, NumberFormatException {
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
		final ArrayList<Integer> cols2Traces = new ArrayList<>();
		cols2Traces.add(0); //skips index 0 because cols are counted starting from 1
		{
			int col = 1;
			for (Integer traceNumber : relevantTraceNumbers) {
				traces2Cols.put(traceNumber, col++);
				cols2Traces.add(traceNumber);
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
			final int traceNumber = cols2Traces.get(col);
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
		
		return new MinimizerProblemGLPK(this.parameters, p, this.cols, cols2Traces);
	}

}
