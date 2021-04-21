package sushi.execution.minimizer;

import java.util.ArrayList;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.glp_iocp;
import org.gnu.glpk.glp_prob;

import sushi.configure.MinimizerParameters;

class MinimizerProblemGLPK extends MinimizerProblem {
	private final MinimizerParameters parameters;
	private final glp_prob p;
	private final int cols;
	private final ArrayList<Integer> cols2Traces;
	
	MinimizerProblemGLPK(MinimizerParameters parameters, glp_prob p, int cols, ArrayList<Integer> cols2Traces) {
		this.parameters = parameters;
		this.p = p;
		this.cols = cols;
		this.cols2Traces = cols2Traces;
	}
	
	@Override
	boolean solve() {
		final glp_iocp iocp = new glp_iocp();
		GLPK.glp_init_iocp(iocp);
		iocp.setMsg_lev(GLPK.GLP_MSG_OFF);
		iocp.setTm_lim(this.parameters.getTimeout() * 200);
		iocp.setPresolve(GLPK.GLP_ON);
		final int res = GLPK.glp_intopt(p, iocp);
		return (res == 0 || res == GLPK.GLP_ETMLIM);
	}

	@Override
	boolean solutionFound() {
		final int status = GLPK.glp_mip_status(p);
		return (status == GLPK.GLP_OPT || status == GLPK.GLP_FEAS);
	}

	@Override
	ArrayList<Integer> getSolution() {
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

	@Override
	public void close() {
		GLPK.glp_delete_prob(p);
	}
}
