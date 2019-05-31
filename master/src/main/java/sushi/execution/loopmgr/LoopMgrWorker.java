package sushi.execution.loopmgr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
		
		//detects the traces that cover only branches to ignore and adds them 
		//to the traces to ignore
		final ArrayList<TreeSet<Integer>> coverage = new ArrayList<>();
		try (final BufferedReader r = Files.newBufferedReader(p.getCoverageFilePath())) {
			String line;
			int traceNumber = 0;
			while ((line = r.readLine()) != null) {
				final String[] fields = line.split(",");
				final TreeSet<Integer> traceCoverage = new TreeSet<>();
				for (int i = 3; i < fields.length; ++i) {
					final int branchNumber = Integer.parseInt(fields[i].trim());
					traceCoverage.add(branchNumber);
				}
				coverage.add(traceCoverage);
				final TreeSet<Integer> traceCoverageRelevant = new TreeSet<>(traceCoverage);
				traceCoverageRelevant.removeAll(branchNumbersToIgnore);
				if (traceCoverageRelevant.isEmpty()) {
					traceNumbersToIgnore.add(traceNumber);
				}
				++traceNumber;
			}
		} catch (IOException e) {
			logger.error("I/O error while reading " + p.getCoverageFilePath().toString());
			throw new LoopMgrException(e);
		}
		
		//finished calculation of traceNumbersToIgnore:
		//sets traceNumbers to the set of relevant traces 
		traceNumbers.removeAll(traceNumbersToIgnore);

		//detects the branches that are not covered by any trace and
		//adds them to the branches to ignore
		final TreeSet<Integer> branchNumbersToCover = new TreeSet<>(branchNumbers);
		branchNumbersToCover.removeAll(branchNumbersToIgnore);
		for (int branchNumber : branchNumbersToCover) {
			boolean mayBeCovered = false;
			for (int traceNumber : traceNumbers) {
				final TreeSet<Integer> traceCoverage = coverage.get(traceNumber);
				if (traceCoverage.contains(branchNumber)) {
					mayBeCovered = true;
					break;
				}
			}
			if (!mayBeCovered) {
				branchNumbersToIgnore.add(branchNumber);
				logger.info("Unable to cover branch #" + branchNumber);
			}
		}
		
		//finished calculation of branchNumbersToIgnore:
		//sets branchNumbers to the set of relevant branches 
		branchNumbers.removeAll(branchNumbersToIgnore);

		//some logging
		logger.info("Branches to cover: " + branchNumbers.size() + ", paths to explore: " + traceNumbers.size());

		//decides whether to terminate
		if (branchNumbers.isEmpty()) {
			throw new TerminationException("All targets covered");
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
