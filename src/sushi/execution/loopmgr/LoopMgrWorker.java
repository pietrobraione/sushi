package sushi.execution.loopmgr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;

import sushi.exceptions.LoopMgrException;
import sushi.exceptions.TerminationException;
import sushi.execution.ExecutionResult;
import sushi.execution.Worker;
import sushi.logging.Logger;

public class LoopMgrWorker extends Worker {
	private static final Logger logger = new Logger(LoopMgrWorker.class);
	
	private final LoopMgr loopMgr;

	public LoopMgrWorker(LoopMgr loopMgr) {
		this.loopMgr = loopMgr;
	}

	@Override
	public ExecutionResult call() throws LoopMgrException, TerminationException {
		final LoopMgrParameters p = this.loopMgr.getInvocationParameters(this.taskNumber);
		
		//nBranches is the total number of branches, nTraces is the total number of traces
		final int nBranches, nTraces;
		try {
			nBranches = (int) Files.lines(p.getBranchesFilePath()).count();
			nTraces = (int) Files.lines(p.getCoverageFilePath()).count();
		} catch (IOException e) {
			logger.error("I/O error while reading " + p.getBranchesFilePath().toString() + " or " + p.getCoverageFilePath().toString());
			throw new LoopMgrException(e);
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
		//or that have already been covered by previously generated tests
		final TreeSet<Integer> branchNumbersToIgnore;
		try {
			branchNumbersToIgnore = branchNumbersToIgnore(p);
		} catch (IOException e) {
			logger.error("I/O error while reading " + p.getBranchesToIgnoreFilePath().toString());
			throw new LoopMgrException(e);
		}
		
		//the traces to ignore are those that have been tried before
		final TreeSet<Integer> traceNumbersToIgnore;
		try {
			traceNumbersToIgnore = traceNumbersToIgnore(p);
		} catch (IOException e) {
			logger.error("I/O error while reading " + p.getTracesToIgnoreFilePath().toString());
			throw new LoopMgrException(e);
		}

		//decides whether to terminate
		branchNumbers.removeAll(branchNumbersToIgnore);
		traceNumbers.removeAll(traceNumbersToIgnore);
		if (branchNumbers.isEmpty()) {
			throw new TerminationException("All branches covered");
		} else if (traceNumbers.isEmpty()) {
			throw new TerminationException("Traces exhausted");
		}
		
		//emits the files
		try {
			writeFile(p.getBranchesToIgnoreFilePath(), branchNumbersToIgnore);
			writeFile(p.getTracesToIgnoreFilePath(), traceNumbersToIgnore);
		} catch (IOException e) {
			logger.error("I/O error while writing " + p.getBranchesToIgnoreFilePath().toString() + " or " + p.getTracesToIgnoreFilePath().toString());
			throw new LoopMgrException(e);
		}
		
		final ExecutionResult result = new ExecutionResult();
		result.setExitStatus(0);
		return result;
	}
	
	private TreeSet<Integer> branchNumbersToIgnore(LoopMgrParameters p) throws IOException {
		final TreeSet<Integer> retVal = new TreeSet<>();
		try (final BufferedReader r = Files.newBufferedReader(p.getBranchesToIgnoreFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				retVal.add(Integer.parseInt(line.trim()));
			}
		}
		try (final BufferedReader r = Files.newBufferedReader(p.getCoveredByTestFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				retVal.add(Integer.parseInt(line.trim()));
			}
		}
		return retVal;
	}
	
	private TreeSet<Integer> traceNumbersToIgnore(LoopMgrParameters p) throws IOException {
		final TreeSet<Integer> retVal = new TreeSet<>();
		try (final BufferedReader r = Files.newBufferedReader(p.getTracesToIgnoreFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				retVal.add(Integer.parseInt(line.trim()));
			}
		}
		try (final BufferedReader r = Files.newBufferedReader(p.getMinimizerOutFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				retVal.add(Integer.parseInt(line.split(",")[0].trim()));
			}
		}
		return retVal;
	}
	
	private void writeFile(Path f, TreeSet<Integer> rows) throws IOException {
		try (final BufferedWriter w = Files.newBufferedWriter(f)) {
			for (Integer row : rows) {
				w.write(row.toString());
				w.newLine();
			}
		}
	}
}
