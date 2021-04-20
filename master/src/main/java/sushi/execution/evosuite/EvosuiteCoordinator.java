package sushi.execution.evosuite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import sushi.configure.Coverage;
import sushi.configure.Options;
import sushi.exceptions.CoordinatorException;
import sushi.exceptions.WorkerException;
import sushi.execution.Coordinator;
import sushi.execution.ExecutionResult;
import sushi.execution.Tool;
import sushi.logging.Logger;
import sushi.util.DirectoryUtils;

public class EvosuiteCoordinator extends Coordinator implements TestGenerationNotifier {
	private static final Logger logger = new Logger(EvosuiteCoordinator.class);
	
	private ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures; //alias for coordination
	private final HashSet<Integer> coveredBranches = new HashSet<>();
	private ArrayList<String[]> methods;
	private ArrayList<HashSet<Integer>> coverageData;
	private ArrayList<HashSet<Integer>> tracesOfTask;
	private ArrayList<int[]> minimizerOutput;
	private HashSet<Integer> branchesToIgnore;
	private HashSet<Integer> cancelledTasks = new HashSet<>();
	
	public EvosuiteCoordinator(Tool<?> tool) { super(tool); }
	
	@Override
	public ExecutionResult[] start(ArrayList<ArrayList<Future<ExecutionResult>>> tasksFutures) {
		final ExecutionResult[] retVal = new ExecutionResult[this.tool.tasks().size() * this.tool.redundance()];
		this.tasksFutures = tasksFutures;
		try {
			loadMethods();
			loadCoverageData();
			loadTracesOfTasks();
			loadMinimizerOutput();
			loadBranchesToIgnore();
		} catch (IOException e) {
			logger.fatal("Error occurred while reading coverage or minimizer data");
			throw new CoordinatorException(e);
		} catch (NumberFormatException e) {
			logger.fatal("Coverage or minimizer data files are ill-formed");
			throw new CoordinatorException(e);
		}
		//from here this.coverageData and this.traceOfTask are read-only
		
		for (int i = 0; i < retVal.length; ++i) {
			final int threadNumber = i; //to make the compiler happy
			final int taskNumber = threadNumber / this.tool.redundance();
			final int replicaNumber = threadNumber % this.tool.redundance();
			final Future<ExecutionResult> thisThreadFuture = tasksFutures.get(taskNumber).get(replicaNumber);
			//waits for the result of its worker
			try {
				retVal[i] = thisThreadFuture.get();
			} catch (CancellationException e) {
				//the worker was cancelled
				retVal[i] = null;
			} catch (ExecutionException e) {
				logger.fatal("Error occurred during execution of tool " + this.tool.getName());
				throw new WorkerException(e);
			} catch (InterruptedException e)  {
				//should never happen, but if it happens
				//it's ok to fall through to shutdown
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
	
	private void loadMethods() throws IOException {
		this.methods = new ArrayList<>();
		try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getMethodsFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				final String[] fields = line.split(":");
				this.methods.add(fields);
			}
		}
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
	
	private void loadTracesOfTasks() throws IOException, NumberFormatException {
		this.tracesOfTask = new ArrayList<>();
		try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getMinimizerOutFilePath())) {
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
				if (mosaTargetsCounter == Options.I().getNumMOSATargets()) {
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
		try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getMinimizerOutFilePath())) {
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
		try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getBranchesToIgnoreFilePath())) {
			String line;
			while ((line = r.readLine()) != null) {
				this.branchesToIgnore.add(Integer.parseInt(line.trim()));
			}
		}
	}
	
	@Override
	public synchronized void onTestGenerated(int taskNumber, int methodNumber, int localTraceNumber) {
		final HashSet<Integer> branchesOfTarget = branchesOfTarget(taskNumber, methodNumber, localTraceNumber);
		final HashSet<Integer> branchesNew = new HashSet<>(branchesOfTarget);
		branchesNew.removeAll(this.coveredBranches);
		branchesNew.removeAll(this.branchesToIgnore);
		this.coveredBranches.addAll(branchesOfTarget);
		final int numBranchesNew = branchesNew.size();
		if (Options.I().getCoverage() == Coverage.BRANCHES) {
			logger.info("Generated test, covered " + numBranchesNew + " new branches");
			if (numBranchesNew > 0) {
				cancelTasksFullyCoveredBranches();
				emitTest(methodNumber, localTraceNumber);
			}
		} else {
			logger.info("Generated test");
			emitTest(methodNumber, localTraceNumber);
		}
	}
	
	//here synchronization is possibly redundant

	private synchronized HashSet<Integer> branchesOfTarget(int taskNumber, int methodNumber, int localTraceNumber) {
		for (int i = taskNumber * Options.I().getNumMOSATargets(); i < Math.min((taskNumber + 1) * Options.I().getNumMOSATargets(), this.minimizerOutput.size()); ++i) {
			final int[] row = this.minimizerOutput.get(i);
			if (row[1] == methodNumber && row[2] == localTraceNumber) {
				final int trace = row[0];
				final HashSet<Integer> retVal = new HashSet<>();
				retVal.addAll(this.coverageData.get(trace));
				return retVal;
			}
		}
		logger.error("Missing coverage information for task " + taskNumber + ", method " + methodNumber + ", local trace " + localTraceNumber);
		throw new CoordinatorException("Missing coverage information for task " + taskNumber + ", method " + methodNumber + ", local trace " + localTraceNumber);
	}
	
	private synchronized void cancelTasksFullyCoveredBranches() {
		for (int task = 0; task < this.tracesOfTask.size(); ++task) {
			if (taskCovered(task) && !this.cancelledTasks.contains(task)) {
				cancelTask(task);
				logger.debug("Task " + task + " cancelled");
			}
		}
	}
	
	private synchronized void cancelTask(int task) {
		final ArrayList<Future<ExecutionResult>> futures = this.tasksFutures.get(task);
		for (Future<ExecutionResult> f : futures) {
			f.cancel(true);
		}
		this.cancelledTasks.add(task);
	}
	
	private synchronized void emitTest(int methodNumber, int localTraceNumber) {
        //builds the relative path name of the test and scaffolding source files
    	final String relativeTestFileName = this.methods.get(methodNumber)[0] + "_" + methodNumber + "_" + localTraceNumber + "_Test.java";
    	final String relativeScaffoldingFileName = this.methods.get(methodNumber)[0] + "_" + methodNumber + "_" + localTraceNumber + "_Test_scaffolding.java";

    	//copies the test in out
        try {
            //creates the intermediate package directories if they do not exist
            final int lastSlash = relativeTestFileName.lastIndexOf('/');
            if (lastSlash != -1) {
                final String dirs = relativeTestFileName.substring(0, lastSlash);
                final Path destinationDir = Options.I().getOutDirPath().resolve(dirs);
                Files.createDirectories(destinationDir);
            }
            
            //copies the test file
            final Path source = DirectoryUtils.I().getTmpDirPath().resolve(relativeTestFileName);
            final Path destination = Options.I().getOutDirPath().resolve(relativeTestFileName);
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

            //possibly copies the scaffolding file
            if (!Options.I().getEvosuiteNoDependency()) {
            	final Path sourceScaffolding = DirectoryUtils.I().getTmpDirPath().resolve(relativeScaffoldingFileName);
            	final Path destinationScaffolding = Options.I().getOutDirPath().resolve(relativeScaffoldingFileName);
            	Files.copy(sourceScaffolding, destinationScaffolding, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
    		logger.error("Unexpected I/O error while attempting to copy file " + relativeTestFileName + " or " + relativeScaffoldingFileName + " to its destination directory");
    		throw new CoordinatorException(e);
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
