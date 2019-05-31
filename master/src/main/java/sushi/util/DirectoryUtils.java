package sushi.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import sushi.configure.Options;
import sushi.logging.Logger;

public class DirectoryUtils {

	private static final Logger logger = new Logger(DirectoryUtils.class);

	private static DirectoryUtils instance = null;
	public static final String jbseGeneratedOutClass = "EvoSuiteWrapper";
	private static final String javaSourceExtension = ".java"; 
	private static final String methodsFileName = "methods.txt"; 
	private static final String branchesFileName = "branches.txt";
	private static final String branchesFileNamePattern = "branches_$.txt";
	private static final String coverageFileName = "coverage.txt"; 
	private static final String coverageFileNamePattern = "coverage_$.txt"; 
	private static final String tracesFileName = "alltraces.txt"; 
	private static final String tracesFileNamePattern = "alltraces_$.txt"; 
	private static final String branchesToIgnoreFileName = "branches_to_ignore.txt"; 
	private static final String tracesToIgnoreFileName = "traces_to_ignore.txt"; 
	private static final String minimizerOutFileName = "traces.txt"; 
	private static final String coveredByTestFileName = "covered_by_test.txt"; 
	
	public static DirectoryUtils I() {
		if (instance == null) {
			instance = new DirectoryUtils();
			instance.possiblyCreateTmpDir();
		}
		return instance;
	}
	
	public static void reset() {
		instance = null;
	}

	private void possiblyCreateTmpDir() {
		logger.debug("Creating experiment directories");
		
		final Path path = getJBSEOutDirPath();
		try {
			Files.createDirectories(path); //this creates the temporary directory and all the subdirectories for the wrappers
		} catch (IOException e) {
			logger.error("Unable to create experiment directories: ", e);
		}

		logger.debug("Creating experiment directories - done");
	}
	
	public String getJBSEOutClass(long targetMethodNumber, long traceNumberLocal) {
		return jbseGeneratedOutClass + "_" + targetMethodNumber + "_" + traceNumberLocal;
	}

	public String getJBSEOutClassQualified(long targetMethodNumber, long traceNumberLocal) {
		final String targetClass = (Options.I().getTargetClass() == null ? Options.I().getTargetMethod().get(0) : Options.I().getTargetClass()); 
		final int endOfPackageNameIndex = targetClass.lastIndexOf('/');
		final String targetClassPackageName = (endOfPackageNameIndex == -1 ? "" : (targetClass.substring(0, endOfPackageNameIndex) + ".")).replace('/', '.');
		return targetClassPackageName + getJBSEOutClass(targetMethodNumber, traceNumberLocal);
	}

	public Path getJBSEOutFilePath(long targetMethodNumber, long traceNumberLocal) {
		return getJBSEOutDirPath().resolve(getJBSEOutClass(targetMethodNumber, traceNumberLocal) + javaSourceExtension);
	}	
	
	public Path getTmpDirPath() {
		return Options.I().getTmpDirectoryBase().resolve(Options.I().getTmpDirectoryName());
	}
	
	public Path getJBSEOutDirPath() {
		final String targetClass = (Options.I().getTargetClass() == null ? Options.I().getTargetMethod().get(0) : Options.I().getTargetClass()); 
		final int endOfPackageNameIndex = targetClass.lastIndexOf('/');
		final String targetClassPackageName = (endOfPackageNameIndex == -1 ? "" : targetClass.substring(0, endOfPackageNameIndex));
		return getTmpDirPath().resolve(targetClassPackageName);
	}
	
	public Path getMethodsFilePath() {
		return getTmpDirPath().resolve(methodsFileName);
	}
	
	public Path getBranchesFilePath() {
		return getTmpDirPath().resolve(branchesFileName);
	}
	
	public Path getBranchesFilePath(long i) {
		return getTmpDirPath().resolve(branchesFileNamePattern.replace("$", Long.toString(i)));
	}
	
	public Path getCoverageFilePath() {
		return getTmpDirPath().resolve(coverageFileName);
	}

	public Path getCoverageFilePath(long i) {
		return getTmpDirPath().resolve(coverageFileNamePattern.replace("$", Long.toString(i)));
	}

	public Path getTracesFilePath() {
		return getTmpDirPath().resolve(tracesFileName);
	}
	
	public Path getTracesFilePath(long i) {
		return getTmpDirPath().resolve(tracesFileNamePattern.replace("$", Long.toString(i)));
	}	
	
	public Path getBranchesToIgnoreFilePath() {
		return getTmpDirPath().resolve(branchesToIgnoreFileName);
	}
	
	public Path getTracesToIgnoreFilePath() {
		return getTmpDirPath().resolve(tracesToIgnoreFileName);
	}
	
	public Path getMinimizerOutFilePath() {
		return getTmpDirPath().resolve(minimizerOutFileName);
	}
	
	public Path getCoveredByTestFilePath() {
		return getTmpDirPath().resolve(coveredByTestFileName);
	}
}
