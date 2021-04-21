package sushi.execution.minimizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

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
		final long start = System.currentTimeMillis();
		
		final MinimizerProblemFactory<?> factory;
		try {
			//TODO decide which factory based on parameters
			factory = new MinimizerProblemFactoryGLPK(this.parameters);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		
		//generates the optimal solution and emits it; then
		//generates more solutions until the emitted rows
		//saturate the number of tasks
		int emittedRows = 0;
		boolean firstIteration = true;
		do {
			try (final MinimizerProblem p = factory.makeProblem()) {
				final boolean ok = p.solve();
				if (!ok) {
					//no solution
					return (firstIteration ? 1 : 0);
				}
				final boolean found = p.solutionFound();
				if (!found) {
					throw new TerminationException("Minimizer was unable to find a set of traces that covers the uncovered branches");
				}
				
				//gets the solution and emits it
				final ArrayList<Integer> solution = p.getSolution();
				emitSolution(solution, !firstIteration);
				factory.ignore(solution);
				emittedRows += solution.size();
			} catch (IOException | NumberFormatException e) {
				e.printStackTrace();
				return 1;
			}
			
			firstIteration = false;
			if (System.currentTimeMillis() - start > this.parameters.getTimeout() * 1000) {
				return 0;
			}
		} while (emittedRows < this.parameters.getNumberOfTasks() && !factory.isEmpty());
		
		return 0;
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
