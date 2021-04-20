package sushi.execution.evosuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import sushi.configure.Options;
import sushi.exceptions.EvosuiteException;
import sushi.execution.Coordinator;
import sushi.execution.Tool;
import sushi.execution.Worker;
import sushi.logging.Logger;
import sushi.modifier.Modifier;
import sushi.util.ArrayUtils;
import sushi.util.DirectoryUtils;
import sushi.util.IOUtils;

public class Evosuite extends Tool<String[]> {
	private static final Logger logger = new Logger(Evosuite.class);
	
	private final EvosuiteCoordinator evosuiteCoordinator;
	private String commandLine;
	private ArrayList<Integer> tasks = null;

	public Evosuite() { 
		this.evosuiteCoordinator = new EvosuiteCoordinator(this);
	}

	public String getCommandLine() {
		return this.commandLine; 
	}
	
	public TestGenerationNotifier getTestGenerationNotifier() {
		return this.evosuiteCoordinator::onTestGenerated;
	}
	
	@Override
	public List<Integer> tasks() {
		if (this.tasks == null) {
			this.tasks = new ArrayList<>();
			final int numTasks;
			try {
				final int numPaths = (int) Files.lines(DirectoryUtils.I().getMinimizerOutFilePath()).count();
				numTasks = (numPaths / Options.I().getNumMOSATargets()) + (numPaths % Options.I().getNumMOSATargets() == 0 ? 0 : 1);
			} catch (IOException e) {
				logger.error("Unable to find and open minimizer output file " + DirectoryUtils.I().getMinimizerOutFilePath().toString());
				throw new EvosuiteException(e);
			}
			for (int i = 0; i < numTasks; ++i) {
				this.tasks.add(i);
			}
		}
		return this.tasks;
	}
	
	@Override
	public String[] getInvocationParameters(int taskNumber) {
		final Options options = Options.I();

		final ArrayList<Integer> targetMethodNumbers = new ArrayList<>();
		final ArrayList<Integer> traceNumbersLocal = new ArrayList<>();
		{
			Integer targetMethodNumber_ = null;
			Integer traceNumberLocal_ = null;
			try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getMinimizerOutFilePath())) {
				String line;
				while ((line = r.readLine()) != null) {
					final String[] fields = line.split(",");
					targetMethodNumber_ = Integer.parseInt(fields[1].trim());
					traceNumberLocal_ = Integer.parseInt(fields[2].trim());
					if (targetMethodNumber_ == null || traceNumberLocal_ == null) {
						logger.error("Minimizer output file " + DirectoryUtils.I().getMinimizerOutFilePath().toString() + " ill-formed, or task number " + taskNumber + " is wrong");
						throw new EvosuiteException("Minimizer output file " + DirectoryUtils.I().getMinimizerOutFilePath().toString() + " ill-formed, or task number " + taskNumber + " is wrong");
					}
					targetMethodNumbers.add(targetMethodNumber_.intValue());
					traceNumbersLocal.add(traceNumberLocal_.intValue());
				}
			} catch (IOException e) {
				logger.error("I/O error while reading " + DirectoryUtils.I().getMinimizerOutFilePath().toString());
				throw new EvosuiteException(e);
			}
		}

		final ArrayList<String> targetMethodSignatures = new ArrayList<>();
		final String targetClassName, targetMethodSignature;
		//currently SUSHI tests at most a single class, so whatever will be
		//targetMethodName all the rows in the methods.txt file will have same
		//class name and we don't need to take a list of target class names for MOSA
		{
			String[] signature = null;
			if (options.getTargetMethod() == null) {
				try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getMethodsFilePath())) {
					String line;
					while ((line = r.readLine()) != null) {
						signature = line.split(":");
						targetMethodSignatures.add(signature[2] + signature[1]);
					}
				} catch (IOException e) {
					logger.error("I/O error while reading " + DirectoryUtils.I().getMethodsFilePath().toString());
					throw new EvosuiteException(e);
				}

				if (signature == null) {
					logger.error("Methods file " + DirectoryUtils.I().getMethodsFilePath() + " and coverage file " + DirectoryUtils.I().getCoverageFilePath().toString() + " disagree");
					throw new EvosuiteException("Methods file " + DirectoryUtils.I().getMethodsFilePath() + " and coverage file " + DirectoryUtils.I().getCoverageFilePath().toString() + " disagree");
				}
			} else {
				signature = options.getTargetMethod().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
			}

			targetClassName = signature[0].replace('/', '.');
			targetMethodSignature = signature[2] + signature[1];
			
			if (options.getTargetMethod() != null) {
				//must add the only targetMethodSignature to targetMethodSignatures, 
				//because the previous code skipped the loop that populates it
				targetMethodSignatures.add(targetMethodSignature);
			}
		}
		
		final List<String> evo = new ArrayList<String>();
		if (options.getJava8Path() != null && !options.getJava8Path().toString().equals("")) {
			evo.add(Options.I().getJava8Path().resolve("bin/java").toString());
		} else {
			evo.add("java");
		}
		evo.add("-Xmx4G");
		evo.add("-jar");
		evo.add(options.getEvosuitePath().toString());
		evo.add("-class");
		evo.add(targetClassName);
		evo.add("-mem");
		evo.add("2048");
		evo.add("-Dmock_if_no_generator=false");
		evo.add("-Dreplace_system_in=false");
		evo.add("-Dreplace_gui=false");
		evo.add("-Dp_functional_mocking=0.0");
		evo.add("-DCP=" + getClassPath());
		evo.add("-Dassertions=false");
		evo.add("-Dreport_dir=" + DirectoryUtils.I().getTmpDirPath().toString());
		evo.add("-Djunit_suffix=_Test");
		evo.add("-Dsearch_budget=" + getTimeBudget());
		evo.add("-Dtest_dir=" + DirectoryUtils.I().getTmpDirPath().toString());
		evo.add("-Dvirtual_fs=false");
		evo.add("-Dselection_function=ROULETTEWHEEL");
		evo.add("-Dcriterion=PATHCONDITION");		
		evo.add("-Dsushi_statistics=true");
		evo.add("-Dinline=false");
		evo.add("-Dsushi_modifiers_local_search=true");
		//evo.add("-Dpath_condition_target=LAST_ONLY");  TODO this was for concolic, should we use it?
		evo.add("-Duse_minimizer_during_crossover=true");
		evo.add("-Davoid_replicas_of_individuals=true"); 
		evo.add("-Dno_change_iterations_before_reset=30");
        if (options.getEvosuiteNoDependency()) {
        	evo.add("-Dno_runtime_dependency");
        }
        evo.add("-Dpath_condition_evaluators_dir=" + DirectoryUtils.I().getTmpDirPath().toString());
        evo.add("-Demit_tests_incrementally=true");
        evo.add("-Dcrossover_function=SUSHI_HYBRID");
        evo.add("-Dalgorithm=DYNAMOSA");
        evo.add("-generateMOSuite");

		setUserDefinedParameters(evo);

		this.commandLine = evo.toString().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", "");

		final StringBuilder optionPC = new StringBuilder("-Dpath_condition=");
		boolean firstDone = false;
		for (int i = options.getNumMOSATargets() * taskNumber; i < Math.min(options.getNumMOSATargets() * (taskNumber + 1), targetMethodNumbers.size()); ++i) {
			if (firstDone) {
				optionPC.append(":");
			} else {
				firstDone = true;
			}
			final int targetMethodNumber_i = targetMethodNumbers.get(i).intValue();
			final int traceNumberLocal_i = traceNumbersLocal.get(i).intValue();
			final String targetMethodSignature_i = targetMethodSignatures.get(targetMethodNumber_i);
			optionPC.append(targetClassName + "," + targetMethodSignature_i + "," + DirectoryUtils.I().getJBSEOutClassQualified(targetMethodNumber_i, traceNumberLocal_i));
		}
		evo.add(optionPC.toString());
		this.commandLine += " " + optionPC.toString();

		return evo.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
	}
	
	private String getClassPath() {
		final Options options = Options.I();
		return IOUtils.concatClassPath(
				IOUtils.concatClassPath(options.getClassesPath()),
				IOUtils.concatClassPath(options.getSushiLibPath(), options.getJBSELibraryPath()));
	}
	
	private void setUserDefinedParameters(List<String> evo) {
        Modifier.I().modify(evo);
	}
	
	@Override
	public void reset() {
		this.tasks = null;
	}
	
	@Override
	public boolean delegateTimeoutToCoordinator() {
		return true; //pleonastic because the EvosuiteCoordinator knows it has to manage timeout
	}
	
	@Override
	public int getTimeBudget() {
		return Options.I().getEvosuiteBudget();
	}

	@Override
	public Worker getWorker(int taskNumber) {
		return new EvosuiteWorker(this, taskNumber);
	}
	
	@Override
	public Coordinator getCoordinator() {
		return this.evosuiteCoordinator;
	}
	
	@Override
	public int degreeOfParallelism() {
		return (Options.I().getParallelismEvosuite() == 0 ? tasks().size() * redundance() : Options.I().getParallelismEvosuite());
	}
	
	@Override
	public int redundance() {
		return Options.I().getRedundanceEvosuite();
	}
}
