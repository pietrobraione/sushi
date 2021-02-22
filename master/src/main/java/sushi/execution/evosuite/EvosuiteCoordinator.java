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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import sushi.configure.Options;
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
	private ArrayList<HashSet<Integer>> tracesOfTask;
	private HashSet<Integer> branchesToIgnore;
	private HashSet<Integer> cancelledTasks = new HashSet<>();
	
	public EvosuiteCoordinator(Tool<?> tool) { super(tool); }
	
	@Override
	public ExecutionResult[] start(ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures) {
		final ExecutionResult[] retVal = new ExecutionResult[this.tool.tasks().size() * this.tool.redundance()];
		this.tasksFutures = tasksFutures;
		try {
			loadCoverageData();
			loadTracesOfTask();
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
			final int replicaNumber = threadNumber % this.tool.redundance();
			final Future<ExecutionResult> thisThreadFuture = tasksFutures.get(taskNumber).get(replicaNumber);
			takers[i] = new Thread(() -> {
				//waits for the result of its worker
				try {
					retVal[threadNumber] = thisThreadFuture.get(this.tool.getTimeBudget(), TimeUnit.SECONDS);
				} catch (TimeoutException e) {
					logger.debug("Task " + taskNumber + " replica " + replicaNumber + " timed out");
					thisThreadFuture.cancel(true);
					return;
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
				
				//cancel this task's redundant threads
				cancelTask(taskNumber);
				
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
		
		this.coveredBranches.removeAll(this.branchesToIgnore);
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
	
	private void loadTracesOfTask() throws IOException, NumberFormatException {
		this.tracesOfTask = new ArrayList<>();
		try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getMinimizerOutFilePath())) {
			String line;
			HashSet<Integer> traces = null;
			while ((line = r.readLine()) != null) {
				if (traces == null) {
					traces = new HashSet<>();
				}
				final String[] fields = line.split(",");
				traces.add(Integer.parseInt(fields[0].trim()));
				if (!Options.I().getUseMOSA()) {
					this.tracesOfTask.add(traces);
					traces = null;
				}
			}
			if (traces != null) {
				this.tracesOfTask.add(traces);
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
	
	private synchronized void cancelTask(int task) {
		final ArrayList<Future<ExecutionResult>> futures = this.tasksFutures.get(task);
		for (Future<ExecutionResult> f : futures) {
			f.cancel(true);
		}
		this.cancelledTasks.add(task);
	}
	
	private synchronized void cancelCovered() {
		for (int task = 0; task < this.tracesOfTask.size(); ++task) {
			if (taskCovered(task) && !this.cancelledTasks.contains(task)) {
				cancelTask(task);
				logger.info("Task " + task + " cancelled");
			}
		}
	}
	
	//here synchronization is possibly redundant

	private synchronized HashSet<Integer> coverageOfTask(int taskNumber) {
		final HashSet<Integer> retVal = new HashSet<>();
		this.tracesOfTask.get(taskNumber).stream().map(trace -> this.coverageData.get(trace)).forEach(coverage -> retVal.addAll(coverage));
		return retVal;
	}
	
	private synchronized boolean taskCovered(int taskNumber) {
		final HashSet<Integer> coverageOfTask = new HashSet<>(coverageOfTask(taskNumber));
		coverageOfTask.removeAll(this.branchesToIgnore);
		return this.coveredBranches.containsAll(coverageOfTask);
	}
}
