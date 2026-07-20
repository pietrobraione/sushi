package sushi.execution.evosuite;

import static sushi.util.DirectoryUtils.getBranchesToIgnoreFilePath;
import static sushi.util.DirectoryUtils.getCoverageFilePath;
import static sushi.util.DirectoryUtils.getCoveredByTestFilePath;
import static sushi.util.DirectoryUtils.getMethodsFilePath;
import static sushi.util.DirectoryUtils.getMinimizerOutFilePath;
import static sushi.util.DirectoryUtils.getTmpDirPath;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sushi.Coverage;
import sushi.Options;
import sushi.exceptions.CoordinatorException;
import sushi.exceptions.ToolException;
import sushi.exceptions.WorkerFailureException;
import sushi.execution.Coordinator;
import sushi.execution.ExitStatus;
import sushi.execution.Tool;

public class EvosuiteCoordinator extends Coordinator implements TestGenerationNotifier {
	private static final Logger LOGGER = LogManager.getFormatterLogger(EvosuiteCoordinator.class);
	
	private final Options options;
	private ArrayList<ArrayList<Future<ExitStatus>>> allTasksFutures;
	private final HashSet<Integer> coveredBranches = new HashSet<>();
	private ArrayList<String[]> methods;
	private ArrayList<HashSet<Integer>> coverageData;
	private ArrayList<HashSet<Integer>> tracesOfTask;
	private ArrayList<int[]> minimizerOutput;
	private HashSet<Integer> branchesToIgnore;
	private HashSet<Integer> cancelledTasks = new HashSet<>();
	private CoordinatorException emitException = null; //access to this field must be synchronized
	
	public EvosuiteCoordinator(Tool<?> tool, Options options) { 
		super(tool);
		this.options = options;
	}
	
	@Override
	public ExitStatus[] start(ArrayList<ArrayList<Future<ExitStatus>>> allTasksFutures) 
	throws CoordinatorException, ToolException, WorkerFailureException {
		List<Integer> tasks;
		try {
			tasks = this.tool.tasks();
		} catch (Exception e) {
			throw new ToolException(e);
		}
		final ExitStatus[] retVal = new ExitStatus[tasks.size() * this.tool.redundance()];
		this.allTasksFutures = allTasksFutures; //from here this.allTasksFutures is read-only
		try {
			loadMethods();
			loadCoverageData();
			loadTracesOfTasks();
			loadMinimizerOutput();
			loadBranchesToIgnore();
		} catch (IOException | NumberFormatException e) {
			throw new CoordinatorException(e);
		}
		//from here this.coverageData and this.traceOfTask are read-only
		
		for (int i = 0; i < retVal.length; ++i) {
			final int threadNumber = i; //to make the compiler happy
			final int taskNumber = threadNumber / this.tool.redundance();
			final int replicaNumber = threadNumber % this.tool.redundance();
			final Future<ExitStatus> thisThreadFuture = allTasksFutures.get(taskNumber).get(replicaNumber);
			//waits for the result of its worker
			try {
				retVal[threadNumber] = thisThreadFuture.get(); //timeout is not handled by this coordinator
			} catch (CancellationException e) {
				//the worker was cancelled
				retVal[threadNumber] = null;
			} catch (ExecutionException e) {
				//the worker threw an exception:
				//cancels all the workers and exits
				cancelAllTasks();
				throw new WorkerFailureException(taskNumber, e.getCause());
			} catch (InterruptedException e)  {
				//should never happen, but if it happens
				//behaves as a cancellation
				retVal[threadNumber] = null;
			}
			
			final CoordinatorException _emitException;
			synchronized (this) {
				_emitException = this.emitException;
			}
			if (_emitException != null) {
				//the coordinator threw an IOException
				//but in the context of the TestDetector
				//thread: cancels all the workers and
				//rethrows
				cancelAllTasks();
				throw _emitException;
			}
		}
		
		//writes the file with the coverage information
		this.coveredBranches.removeAll(this.branchesToIgnore);
		try (final BufferedWriter w = Files.newBufferedWriter(getCoveredByTestFilePath(this.options))) {
			for (Integer branch : this.coveredBranches) {
				w.write(branch.toString());
				w.newLine();
			}
		} catch (IOException e) {
			throw new CoordinatorException(e);
		}
		
		return retVal;
	}
	
	private void loadMethods() throws IOException {
		this.methods = new ArrayList<>();
		try (final BufferedReader r = Files.newBufferedReader(getMethodsFilePath(this.options))) {
			String line;
			while ((line = r.readLine()) != null) {
				final String[] fields = line.split(":");
				this.methods.add(fields);
			}
		}
	}
	
	private void loadCoverageData() throws IOException, NumberFormatException {
		this.coverageData = new ArrayList<>();
		try (final BufferedReader r = Files.newBufferedReader(getCoverageFilePath(this.options))) {
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
	
	private void loadTracesOfTasks() throws IOException, NumberFormatException {
		this.tracesOfTask = new ArrayList<>();
		try (final BufferedReader r = Files.newBufferedReader(getMinimizerOutFilePath(this.options))) {
			String line;
			int mosaTargetsCounter = 0;
			HashSet<Integer> traces = null;
			while ((line = r.readLine()) != null) {
				if (traces == null) {
					traces = new HashSet<>();
				}
				final String[] fields = line.split(",");
				traces.add(Integer.parseInt(fields[0].trim()));
				++mosaTargetsCounter;
				if (mosaTargetsCounter == this.options.getNumMOSATargets()) {
					this.tracesOfTask.add(traces);
					traces = null;
					mosaTargetsCounter = 0;
				}
			}
			if (traces != null) {
				this.tracesOfTask.add(traces);
			}
		}
	}
	
	private void loadMinimizerOutput() throws IOException, NumberFormatException {
		this.minimizerOutput = new ArrayList<>();
		try (final BufferedReader r = Files.newBufferedReader(getMinimizerOutFilePath(this.options))) {
			String line;
			while ((line = r.readLine()) != null) {
				final int[] row = new int[3];
				final String[] fields = line.split(",");
				row[0] = Integer.parseInt(fields[0].trim()); //global trace number
				row[1] = Integer.parseInt(fields[1].trim()); //method number				
				row[2] = Integer.parseInt(fields[2].trim()); //local trace number
				this.minimizerOutput.add(row);
			}
		}
	}
	
	private void loadBranchesToIgnore() throws IOException, NumberFormatException {
		this.branchesToIgnore = new HashSet<>();
		try (final BufferedReader r = Files.newBufferedReader(getBranchesToIgnoreFilePath(this.options))) {
			String line;
			while ((line = r.readLine()) != null) {
				this.branchesToIgnore.add(Integer.parseInt(line.trim()));
			}
		}
	}
	
	@Override
	public synchronized void onTestGenerated(int taskNumber, int methodNumber, int localTraceNumber) 
	throws CoordinatorException {
		final HashSet<Integer> branchesOfTarget = branchesOfTarget(taskNumber, methodNumber, localTraceNumber);
		final HashSet<Integer> branchesNew = new HashSet<>(branchesOfTarget);
		branchesNew.removeAll(this.coveredBranches);
		branchesNew.removeAll(this.branchesToIgnore);
		this.coveredBranches.addAll(branchesOfTarget);
		final int numBranchesNew = branchesNew.size();
		if (this.options.getCoverage() == Coverage.BRANCHES) {
			LOGGER.info("Generated test, covered %s new branches.", numBranchesNew);
			if (numBranchesNew > 0) {
				cancelTasksFullyCoveredBranches();
				emitTest(methodNumber, localTraceNumber);
			}
		} else {
			LOGGER.info("Generated test.");
			emitTest(methodNumber, localTraceNumber);
		}
	}
	
	//here synchronization is possibly redundant

	private synchronized HashSet<Integer> branchesOfTarget(int taskNumber, int methodNumber, int localTraceNumber) throws CoordinatorException {
		for (int i = taskNumber * this.options.getNumMOSATargets(); i < Math.min((taskNumber + 1) * this.options.getNumMOSATargets(), this.minimizerOutput.size()); ++i) {
			final int[] row = this.minimizerOutput.get(i);
			if (row[1] == methodNumber && row[2] == localTraceNumber) {
				final int trace = row[0];
				final HashSet<Integer> retVal = new HashSet<>();
				retVal.addAll(this.coverageData.get(trace));
				return retVal;
			}
		}
		throw new CoordinatorException("Missing coverage information for task " + taskNumber + ", method " + methodNumber + ", local trace " + localTraceNumber);
	}
	
	private synchronized void cancelTasksFullyCoveredBranches() {
		for (int taskNumber = 0; taskNumber < this.tracesOfTask.size(); ++taskNumber) {
			if (taskCovered(taskNumber) && !this.cancelledTasks.contains(taskNumber)) {
				LOGGER.debug("Task %s cancelled, all its relevant branches have been covered.", taskNumber);
				cancelTask(taskNumber);
			}
		}
	}
	
	private synchronized void cancelAllTasks() {
		for (final ArrayList<Future<ExitStatus>> group : this.allTasksFutures) {
			for (final Future<ExitStatus> f : group) {
				f.cancel(true);
			}
		}
	}
		
	private synchronized void cancelTask(int task) {
		final ArrayList<Future<ExitStatus>> thisTaskFutures = this.allTasksFutures.get(task);
		for (Future<ExitStatus> f : thisTaskFutures) {
			f.cancel(true);
		}
		this.cancelledTasks.add(task);
	}
	
	private synchronized void emitTest(int methodNumber, int localTraceNumber) throws CoordinatorException {
        //builds the relative path name of the test and scaffolding source files
    	final String relativeTestFileName = this.methods.get(methodNumber)[0] + "_" + methodNumber + "_" + localTraceNumber + "_Test.java";
    	final String relativeScaffoldingFileName = this.methods.get(methodNumber)[0] + "_" + methodNumber + "_" + localTraceNumber + "_Test_scaffolding.java";

    	//copies the test in out
        try {
            //creates the intermediate package directories if they do not exist
            final int lastSlashPosition = relativeTestFileName.lastIndexOf('/');
            if (lastSlashPosition != -1) {
                final String dirs = relativeTestFileName.substring(0, lastSlashPosition);
                final Path destinationDir = this.options.getOutDirPath().resolve(dirs);
                Files.createDirectories(destinationDir);
            }
            
            //copies the test file
            final Path source = getTmpDirPath(this.options).resolve(relativeTestFileName);
            final Path destination = this.options.getOutDirPath().resolve(relativeTestFileName);
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

            //possibly copies the scaffolding file
            if (!this.options.getEvosuiteNoDependency()) {
            	final Path sourceScaffolding = getTmpDirPath(this.options).resolve(relativeScaffoldingFileName);
            	final Path destinationScaffolding = this.options.getOutDirPath().resolve(relativeScaffoldingFileName);
            	Files.copy(sourceScaffolding, destinationScaffolding, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
        	this.emitException = new CoordinatorException(e);
    		throw this.emitException;
        }
	}
	
	private synchronized boolean taskCovered(int taskNumber) {
		final HashSet<Integer> relevantBranchesOfTask = new HashSet<>(branchesOfTask(taskNumber));
		relevantBranchesOfTask.removeAll(this.branchesToIgnore);
		return this.coveredBranches.containsAll(relevantBranchesOfTask);
	}
	
	private synchronized HashSet<Integer> branchesOfTask(int taskNumber) {
		final HashSet<Integer> retVal = new HashSet<>();
		this.tracesOfTask.get(taskNumber).stream().map(trace -> this.coverageData.get(trace)).forEach(coverage -> retVal.addAll(coverage));
		return retVal;
	}
}
