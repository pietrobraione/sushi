package sushi.execution.loopmgr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sushi.exceptions.WorkerTerminationException;
import sushi.execution.ExitStatus;
import sushi.execution.Worker;

public class LoopMgrWorker extends Worker {
	private static final Logger LOGGER = LogManager.getFormatterLogger(LoopMgrWorker.class);
	
	private final LoopMgr loopMgr;

	public LoopMgrWorker(LoopMgr loopMgr) {
		this.loopMgr = loopMgr;
	}

	@Override
	public ExitStatus call() throws WorkerTerminationException, IOException {
		final LoopMgrParameters p = this.loopMgr.getInvocationParameters(this.taskNumber);
		
		//nBranches is the total number of branches, nTraces is the total number of traces
		final int nBranches = (int) Files.lines(p.getBranchesFilePath()).count();
		final int nTraces = (int) Files.lines(p.getCoverageFilePath()).count();
		
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
		final TreeSet<Integer> branchNumbersToIgnore = branchNumbersToIgnore(p);
		
		//the traces to ignore are those that have been tried before
		final TreeSet<Integer> traceNumbersToIgnore = traceNumbersToIgnore(p);
		
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
				LOGGER.info("Unable to cover branch # %s.", Integer.toString(branchNumber));
			}
		}
		
		//finished calculation of branchNumbersToIgnore:
		//sets branchNumbers to the set of relevant branches 
		branchNumbers.removeAll(branchNumbersToIgnore);

		//some logging
		LOGGER.info("Branches to cover: %s, paths to explore: %s.", Integer.toString(branchNumbers.size()), Integer.toString((traceNumbers.size())));

		//decides whether to terminate
		if (branchNumbers.isEmpty()) {
			throw new WorkerTerminationException(this.taskNumber, "All targets covered.");
		} else if (traceNumbers.isEmpty()) {
			throw new WorkerTerminationException(this.taskNumber, "Traces exhausted.");
		}
		
		//emits the files
		writeFile(p.getBranchesToIgnoreFilePath(), branchNumbersToIgnore);
		writeFile(p.getTracesToIgnoreFilePath(), traceNumbersToIgnore);
		
		final ExitStatus result = new ExitStatus(0);
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
