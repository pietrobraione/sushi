package sushi.execution.minimizer;

import static org.ojalgo.function.constant.BigMath.ONE;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Optimisation.Result;

final class MinimizerProblemOjAlgo extends MinimizerProblem {
	private final MinimizerParameters parameters;
	private final ExpressionsBasedModel modelOjAlgo;
	private final int cols;
	private final ArrayList<Integer> cols2Traces;
	private Result result;
	
	MinimizerProblemOjAlgo(MinimizerParameters parameters, ExpressionsBasedModel modelOjAlgo, int cols, ArrayList<Integer> cols2Traces) {
		this.parameters = parameters;
		this.modelOjAlgo = modelOjAlgo;
		this.cols = cols;
		this.cols2Traces = cols2Traces;
	}
	
	@Override
	boolean solve() {
		this.modelOjAlgo.options.time_abort = this.parameters.getTimeout() * 1000;
		this.result = this.modelOjAlgo.minimise();
		final Optimisation.State state = this.result.getState();
		return (state == Optimisation.State.DISTINCT || state == Optimisation.State.OPTIMAL || state == Optimisation.State.FEASIBLE || state == Optimisation.State.FAILED);
	}

	@Override
	boolean solutionFound() {
		final Optimisation.State state = this.result.getState();
		return (state == Optimisation.State.DISTINCT || state == Optimisation.State.OPTIMAL || state == Optimisation.State.FEASIBLE);
	}

	@Override
	ArrayList<Integer> getSolution() {
		final ArrayList<Integer> retVal = new ArrayList<>();
		for (int col = 0; col < this.cols; ++col) {
			final BigDecimal val = this.result.get(col);
			if (val.compareTo(ONE) >= 0) {
				final int traceNumber = this.cols2Traces.get(col);
				retVal.add(traceNumber);
			}
		}
		return retVal;
	}

	@Override
	public void close() {
		//nothing to do
	}
}
