package sushi;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.PathOptionHandler;

import sushi.optionhandlers.MultiPathOptionHandlerPatched;
import sushi.optionhandlers.PhasesOptionHandler;
import sushi.optionhandlers.SignatureOptionHandler;

public final class Options {
	@Option(name = "-help",
			usage = "Prints usage and exits",
			help = true)
	private boolean help = false;

	@Option(name = "-classes",
			usage = "Classpath of the project to analyze",
			handler = MultiPathOptionHandlerPatched.class)
	private List<Path> classesPath;

	@Option(name = "-target_method",
			forbids = {"-target_class", "-params_modifier_class"},
			usage = "Java signature of the method for which the tests must be generated (default: none, either this or the -target_class option or the -params_modifier_class option must be specified)",
			handler = SignatureOptionHandler.class)
	private List<String> targetMethodSignature;

	@Option(name = "-target_class",
			forbids = {"-target_method", "-params_modifier_class"},
			usage = "Java signature of the class for which the tests must be generated (default: none, either this or the -target_method option or the -params_modifier_class option must be specified)")
	private String targetClassSignature;

	/* TODO -target_package? */

	@Option(name = "-visibility",
			usage = "For which methods defined in the class should generate tests: PUBLIC (public methods), PACKAGE (public, protected and package methods)")
	private Visibility visibility = Visibility.PUBLIC;

	@Option(name = "-cov",
			usage = "Coverage: PATHS (all paths), BRANCHES (all branches), UNSAFE (failed assertion, works for only one assertion)")
	private Coverage coverage = Coverage.BRANCHES;

	@Option(name = "-tmp_base",
			usage = "Base directory where the temporary subdirectory is found or created",
			handler = PathOptionHandler.class)
	private Path tmpDirBase = Paths.get(".", "tmp");

	@Option(name = "-tmp_name",
			usage = "Name of the temporary subdirectory to use or create")
	private String tmpDirName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());

	@Option(name = "-out",
			usage = "Output directory where the java source files of the created test suite must be put",
			handler = PathOptionHandler.class)
	private Path outDir = Paths.get(".", "out");

	@Option(name = "-z3",
			usage = "Path to Z3 binary",
			handler = PathOptionHandler.class)
	private Path z3Path = Paths.get("/usr", "bin", "z3");

	@Option(name = "-java8_home",
			usage = "Path to Java 8 home (default: none, use Java executables on the system PATH)",
			handler = PathOptionHandler.class)
	private Path java8Path;

	@Option(name = "-jbse_lib",
			usage = "Path to JBSE library",
			handler = PathOptionHandler.class)
	private Path jbsePath = Paths.get(".", "lib", "jbse.jar");

	@Option(name = "-evosuite",
			usage = "Path to Evosuite jar",
			handler = PathOptionHandler.class)
	private Path evosuitePath = Paths.get(".", "lib", "evosuite.jar");

	@Option(name = "-sushi_lib",
			usage = "Path to Sushi library",
			handler = PathOptionHandler.class)
	private Path sushiPath = Paths.get(".", "lib", "sushi-lib.jar");

	@Option(name = "-phases",
			usage = "List of sushi phases to be executed (default: all)",
			handler = PhasesOptionHandler.class)
	private List<Integer> phases;

	@Option(name = "-generation_time_budget",
			usage = "Time budget in seconds for generation of trace information")
	private int budgetJBSE = 180;

	@Option(name = "-generation_parallelism",
			usage = "Number of parallel tasks during generation phase, 0 means maximum")
	private int parallelismJBSE = 0;

	@Option(name = "-selection_time_budget",
			usage = "Time budget in seconds for selection of traces")
	private int budgetMinimizer = 180;

	@Option(name = "-compilation_time_budget",
			usage = "Time budget in seconds for compilation")
	private int budgetJavac = 180;

	@Option(name = "-synthesis_time_budget",
			usage = "Time budget in seconds for syntesis of interface invocations")
	private int budgetEvosuite = 180;

	@Option(name = "-synthesis_parallelism",
			usage = "Number of parallel tasks during synthesis phase, 0 means maximum")
	private int parallelismEvosuite = 0;

	@Option(name = "-synthesis_redundance",
			usage = "Number of parallel synthesis tasks per trace")
	private int redundanceEvosuite = 3;

	@Option(name = "-evosuite_no_dependency",
			usage = "Whether the generated tests should have no dependency on the EvoSuite runtime")
	private boolean evosuiteNoDependency = false;

	@Option(name = "-global_time_budget",
			usage = "Time budget in seconds for the whole generation process, -1 for unlimited")
	private int budgetGlobal = -1;

	@Option(name = "-num_mosa_targets",
			usage = "Maximum number of target passed to a MOSA job")
	private int numMOSATargets = 5;

	@Option(name = "-log_level",
			usage = "Logging level to be used: FATAL, ERROR, WARN, INFO, DEBUG")
	private Level logLevel = Level.INFO;

	@Option(name = "-verbose",
			usage = "Produce verbose output of tools executions")
	private boolean verbose = false;

	@Option(name = "-params_modifier_path",
			usage = "Path for the classfile of the parameters modifier",
			handler = PathOptionHandler.class)
	private Path paramsHome = Paths.get(".", "params");

	@Option(name = "-params_modifier_class",
			forbids = {"-target_class", "-target_method"},
			depends = {"-params_modifier_path"},
			usage = "Parameters modifier class name (default: none, either this or the -target_class option or the -target_method option must be specified)")
	private String paramsClass;
	
	private ParametersModifier parametersModifier;

	public boolean isConsistent() {
		if (this.paramsClass == null &&
			this.targetClassSignature == null &&
			this.targetMethodSignature == null) {
			return false;
		}
		return true;
	}

	public boolean getHelp() {
		return this.help;
	}

	public List<Path> getClassesPath() {
		return this.classesPath;
	}

	public void setClassesPath(Path... paths) {
		this.classesPath = Arrays.asList(paths);
	}

	public List<String> getTargetMethod() {
		return this.targetMethodSignature;
	}

	public void setTargetMethod(String... signature) {
		if (signature.length != 3) {
			return;
		}
		this.targetMethodSignature = Arrays.asList(signature);
	}

	public String getTargetClass() {
		return this.targetClassSignature;
	}

	public void setTargetClass(String targetClass) {
		this.targetClassSignature = targetClass;
	}

	public Visibility getVisibility() {
		return this.visibility;
	}

	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}

	public Coverage getCoverage() {
		return this.coverage;
	}

	public void setCoverage(Coverage coverage) {
		this.coverage = coverage;
	}

	public Path getTmpDirectoryBase() {
		return this.tmpDirBase;
	}

	public void setTmpDirectoryBase(Path base) {
		this.tmpDirBase = base;
	}

	public String getTmpDirectoryName() {
		return this.tmpDirName;
	}

	public void setTmpDirectoryName(String name) {
		this.tmpDirName = name;
	}

	public Path getOutDirPath() {
		return this.outDir;
	}

	public void setOutDirPath(Path dir) {
		this.outDir = dir;
	}

	public Path getZ3Path() {
		return this.z3Path;
	}

	public void setZ3Path(Path z3Path) {
		this.z3Path = z3Path;
	}

	public Path getJava8Path() {
		return this.java8Path;
	}

	public void setJava8Path(Path java8Path) {
		this.java8Path = java8Path;
	}

	public Path getJBSELibraryPath() {
		return this.jbsePath;
	}

	public void setJBSELibraryPath(Path jbsePath) {
		this.jbsePath = jbsePath;
	}

	public Path getEvosuitePath() {
		return this.evosuitePath;
	}

	public void setEvosuitePath(Path evosuitePath) {
		this.evosuitePath = evosuitePath;
	}

	public Path getSushiLibPath() {
		return this.sushiPath;
	}

	public void setSushiLibPath(Path sushiPath) {
		this.sushiPath = sushiPath;
	}

	public List<Integer> getPhases() {
		return this.phases;
	}

	public void setPhases(Integer... phases) {
		this.phases = Arrays.asList(phases);
	}

	public int getJBSEBudget() {
		return this.budgetJBSE;
	}

	public void setJBSEBudget(int budgetJBSE) {
		this.budgetJBSE = budgetJBSE;
	}

	public int getParallelismJBSE() {
		return this.parallelismJBSE;
	}

	public void setParallelismJBSE(int parallelismJBSE) {
		this.parallelismJBSE = parallelismJBSE;
	}

	public int getMinimizerBudget() {
		return this.budgetMinimizer;
	}

	public void setMinimizerBudget(int budgetMinimizer) {
		this.budgetMinimizer = budgetMinimizer;
	}

	public int getJavacBudget() {
		return this.budgetJavac;
	}

	public void setJavacBudget(int budgetJavac) {
		this.budgetJavac = budgetJavac;
	}

	public int getEvosuiteBudget() {
		return this.budgetEvosuite;
	}

	public void setEvosuiteBudget(int budgetEvosuite) {
		this.budgetEvosuite = budgetEvosuite;
	}

	public int getParallelismEvosuite() {
		return this.parallelismEvosuite;
	}

	public void setParallelismEvosuite(int parallelismEvosuite) {
		this.parallelismEvosuite = parallelismEvosuite;
	}

	public int getRedundanceEvosuite() {
		return this.redundanceEvosuite;
	}

	public void setRedundanceEvosuite(int redundanceEvosuite) {
		this.redundanceEvosuite = redundanceEvosuite;
	}

	public boolean getEvosuiteNoDependency() {
		return this.evosuiteNoDependency;
	}

	public void setEvosuiteNoDependency(boolean evosuiteNoDependency) {
		this.evosuiteNoDependency = evosuiteNoDependency;
	}

	public int getGlobalBudget() {
		return this.budgetGlobal;
	}

	public void setGlobalBudget(int budgetGlobal) {
		this.budgetGlobal = budgetGlobal;
	}

	public int getNumMOSATargets() {
		return this.numMOSATargets;
	}

	public void setNumMOSATargets(int numMOSATargets) {
		this.numMOSATargets = numMOSATargets;
	}

	public Level getLogLevel() {
		return this.logLevel;
	}

	public void setLogLevel(Level logLevel) {
		this.logLevel = logLevel;
	}

	public boolean isVerbose() {
		return this.verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public Path getParametersModifierPath() {
		return this.paramsHome;
	}

	public void setParametersModifierPath(Path paramsModifierPath) {
		this.paramsHome = paramsModifierPath;
	}

	public String getParametersModifierClassname() {
		return this.paramsClass;
	}
	
	public void setParametersModifierClassname(String paramsModifierClassname) {
		this.paramsClass = paramsModifierClassname;
	}
	
	public ParametersModifier getParametersModifier() {
		return this.parametersModifier;
	}
	
	void setParametersModifier(ParametersModifier parametersModifier) {
		this.parametersModifier = parametersModifier;
	}
}
