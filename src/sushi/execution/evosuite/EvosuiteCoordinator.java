package sushi.execution.evosuite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import sushi.exceptions.CoordinatorException;
import sushi.exceptions.WorkerException;
import sushi.execution.Coordinator;
import sushi.execution.ExecutionResult;
import sushi.execution.Tool;
import sushi.logging.Logger;
import sushi.util.DirectoryUtils;

public class EvosuiteCoordinator extends Coordinator {
	private static final Logger logger = new Logger(EvosuiteCoordinator.class);
	
	private ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures; //alias for coordination
	private final HashSet<Integer> coveredBranches = new HashSet<>();
	private ArrayList<HashSet<Integer>> coverageData;
	private ArrayList<Integer> traceOfTask;
	private HashSet<Integer> branchesToIgnore;
	
	public EvosuiteCoordinator(Tool<?> tool) { super(tool); }
	
	@Override
	public ExecutionResult[] start(ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures) {
		final ExecutionResult[] retVal = new ExecutionResult[this.tool.tasks().size() * this.tool.redundance()];
		this.tasksFutures = tasksFutures;
		try {
			loadCoverageData();
			loadTraceOfTask();
			loadBranchesToIgnore();
		} catch (IOException e) {
			logger.fatal("Error occurred while reading coverage or minimizer data");
			throw new CoordinatorException(e);
		} catch (NumberFormatException e) {
			logger.fatal("Coverage or minimizer data files are ill-formed");
			throw new CoordinatorException(e);
		}
		//from here this.coverageData and this.traceOfTask are read-only
		
		final Thread[] takers = new Thread[retVal.length];
		for (int i = 0; i < takers.length; ++i) {
			final int threadNumber = i; //to make the compiler happy
			final int taskNumber = threadNumber / this.tool.redundance();
			final Future<ExecutionResult> thisThreadFuture = tasksFutures.get(taskNumber).get(threadNumber % this.tool.redundance());
			takers[i] = new Thread(() -> {
				//waits for the result of its worker
				try {
					retVal[threadNumber] = thisThreadFuture.get();
				} catch (CancellationException e) {
					//the worker was cancelled: nothing left to do
					return;
				} catch (ExecutionException e) {
					logger.fatal("Error occurred during execution of tool " + this.tool.getName());
					throw new WorkerException(e);
				} catch (InterruptedException e)  {
					//should never happen, but if it happens
					//it's ok to fall through to shutdown
				}
				
				//updates total coverage
				addCoveredBranches(taskNumber);
				
				//cancels all tasks that have been fully covered
				cancelCovered();
			});
			takers[i].start();
		}
		
		for (int i = 0; i < takers.length; ++i) {
			try {
				takers[i].join();
			} catch (InterruptedException e) {
				//does nothing
			}
		}
		
		this.coveredBranches.removeAll(branchesToIgnore);
		try (final BufferedWriter w = Files.newBufferedWriter(DirectoryUtils.I().getCoveredByTestFilePath())) {
			for (Integer branch : this.coveredBranches) {
				w.write(branch.toString());
				w.newLine();
			}
		} catch (IOException e) {
			logger.error("I/O error while writing " + DirectoryUtils.I().getCoveredByTestFilePath().toString());
			throw new CoordinatorException(e);
		}
		return retVal;
	}
	
	private void loadCoverageData() throws IOException, NumberFormatException {
		this.coverageData = new ArrayList<>();
		try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getCoverageFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				final HashSet<Integer> coverage = new HashSet<>();
				final String[] fields = line.split(",");
				for (int i = 3; i < fields.length; ++i) {
					coverage.add(Integer.parseInt(fields[i].trim()));
				}
				this.coverageData.add(coverage);
			}
		}
	}
	
	private void loadTraceOfTask() throws IOException, NumberFormatException {
		this.traceOfTask = new ArrayList<>();
		try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getMinimizerOutFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				final String[] fields = line.split(",");
				this.traceOfTask.add(Integer.parseInt(fields[0].trim()));
			}
		}
	}
	
	private void loadBranchesToIgnore() throws IOException, NumberFormatException {
		this.branchesToIgnore = new HashSet<>();
		try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getBranchesToIgnoreFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				this.branchesToIgnore.add(Integer.parseInt(line.trim()));
			}
		}
	}
	
	private synchronized void addCoveredBranches(int taskNumber) {
		this.coveredBranches.addAll(coverageOfTask(taskNumber));
	}
	
	private synchronized void cancelCovered() {
		for (int task = 0; task < this.traceOfTask.size(); ++task) {
			if (taskCovered(task)) {
				final ArrayList<Future<ExecutionResult>> futures = this.tasksFutures.get(task);
				for (Future<ExecutionResult> f : futures) {
					f.cancel(true);
				}
			}
		}
	}
	
	//here synchronization is possibly redundant

	private synchronized HashSet<Integer> coverageOfTask(int taskNumber) {
		return this.coverageData.get(this.traceOfTask.get(taskNumber));
	}
	
	private synchronized boolean taskCovered(int taskNumber) {
		final HashSet<Integer> coverageOfTask = new HashSet<>(coverageOfTask(taskNumber));
		coverageOfTask.removeAll(this.branchesToIgnore);
		return this.coveredBranches.containsAll(coverageOfTask);
	}
}
