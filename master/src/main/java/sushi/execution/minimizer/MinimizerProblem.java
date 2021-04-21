package sushi.execution.minimizer;

import java.util.ArrayList;

abstract class MinimizerProblem implements AutoCloseable {
	/**
	 * Solves the problem.
	 * 
	 * @return {@code true} iff solving had success. 
	 */
	abstract boolean solve();
	
	/**
	 * Checks whether a solution was found;
	 * to be invoked after {@link #solve()}
	 * and before {@link #getSolution()}.
	 * 
	 * @return {@code true} iff solving has found 
	 *         a solution.
	 */
	abstract boolean solutionFound();

	/**
	 * Returns a solution.
	 * 
	 * @return an {@link ArrayList}{@code <}{@link Integer}{@code >}.
	 */
	abstract ArrayList<Integer> getSolution();
	
	@Override
	abstract public void close();

}
