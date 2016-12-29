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
	private String commandLine;
	private ArrayList<Integer> tasks = null;

	public Evosuite() { }

	private static final Logger logger = new Logger(Evosuite.class);
	
	public String getCommandLine() {
		return this.commandLine; 
	}
	
	@Override
	public List<Integer> tasks() {
		if (this.tasks == null) {
			this.tasks = new ArrayList<>();
			final int numTasks;
			try {
				numTasks = (int) Files.lines(DirectoryUtils.I().getMinimizerOutFilePath()).count();
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

		final int targetMethodNumber, traceNumberLocal;
		{
			Integer targetMethodNumber_ = null;
			Integer traceNumberLocal_ = null;
			try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getMinimizerOutFilePath())) {
				int current = 0;
				String line;
				while ((line = r.readLine()) != null) {
					if (current == taskNumber) {
						String[] fields = line.split(",");
						targetMethodNumber_ = Integer.parseInt(fields[1].trim());
						traceNumberLocal_ = Integer.parseInt(fields[2].trim());
						break;
					}
					++current;
				}
			} catch (IOException e) {
				logger.error("I/O error while reading " + DirectoryUtils.I().getMinimizerOutFilePath().toString());
				throw new EvosuiteException(e);
			}

			if (targetMethodNumber_ == null || traceNumberLocal_ == null) {
				logger.error("Minimizer output file " + DirectoryUtils.I().getMinimizerOutFilePath().toString() + " ill-formed, or task number " + taskNumber + " is wrong");
				throw new EvosuiteException("Minimizer output file " + DirectoryUtils.I().getMinimizerOutFilePath().toString() + " ill-formed, or task number " + taskNumber + " is wrong");
			}

			targetMethodNumber = targetMethodNumber_.intValue();
			traceNumberLocal = traceNumberLocal_.intValue();
		}

		final String targetClassName, targetMethodSignature;
		{
			String[] signature = null;
			if (options.getTargetMethod() == null) {
				try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getMethodsFilePath())) {
					int current = 0;
					String line;
					while ((line = r.readLine()) != null) {
						if (current == targetMethodNumber) {
							signature = line.split(":");
						}
						++current;
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
		}
		
		final List<String> evo = new ArrayList<String>();
		if (options.getJava8Path() != null && !options.getJava8Path().equals("")) {
			evo.add(Options.I().getJava8Path().resolve("bin/java").toString());
		} else {
			evo.add("java");
		}
		evo.add("-Xmx4G");
		evo.add("-jar");
		evo.add(Options.I().getEvosuitePath().toString());
		evo.add("-class");
		evo.add(targetClassName);
		evo.add("-mem");
		evo.add("2048");
		evo.add("-DCP=" + getClassPath(taskNumber));
		evo.add("-Dassertions=false");
		evo.add("-Dhtml=false");
		evo.add("-Dglobal_timeout=" + getTimeBudget());
		evo.add("-Dreport_dir=" + DirectoryUtils.I().getTmpDirPath().toString());
		evo.add("-Djunit_suffix=" /*+ "_output_"*/ + "_" + 
				targetMethodSignature.substring(0, targetMethodSignature.indexOf('(')) + "_" +
				"PC_" + targetMethodNumber + "_" + traceNumberLocal + "_" 
				/*+ Thread.currentThread().getName().replace('-', '_')*/ + "Test");
		evo.add("-Dsearch_budget=" + getTimeBudget());
		//evo.add("-Dtarget_method_prefix=test");
		evo.add("-Dtest_dir=" + Options.I().getOutDirectory());
		evo.add("-Dvirtual_fs=false");
		evo.add("-Dselection_function=ROULETTEWHEEL");
		evo.add("-Dcrossover_function=SINGLEPOINT");
		evo.add("-Dcriterion=PATHCONDITION");		
		evo.add("-Davoid_replicas_of_individuals=true"); 
		evo.add("-Dsushi_statistics=true");
		evo.add("-Dcrossover_implementation=SUSHI_HYBRID");
		evo.add("-Duse_minimizer_during_crossover=true");
		evo.add("-Dno_change_iterations_before_reset=30");
		evo.add("-Dmax_size=1");
		evo.add("-Dmax_initial_tests=1");
		//evo.add("-Dsushi_modifiers_local_search=true"); does not work

		setUserDefinedParameters(evo);

		this.commandLine = evo.toString().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", "");

		evo.add("-Dpath_condition=" + targetClassName + "," + targetMethodSignature + "," + DirectoryUtils.I().getJBSEOutClass(targetMethodNumber, traceNumberLocal));
		this.commandLine += " -Dpath_condition=" + targetClassName + "," + targetMethodSignature + "," + DirectoryUtils.I().getJBSEOutClass(targetMethodNumber, traceNumberLocal);

		return evo.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
	}
	
	private String getClassPath(int i) {
		return IOUtils.concatClassPath(
					IOUtils.concatClassPath(Options.I().getClassesPath()),
					IOUtils.concatClassPath(
							DirectoryUtils.I().getTmpDirPath(), 
							Options.I().getSushiLibPath()));
	}
	
	private void setUserDefinedParameters(List<String> evo) {
        Modifier.I().modify(evo);
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
		return new EvosuiteCoordinator();
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
