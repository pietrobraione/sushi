package sushi.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import sushi.Options;
import sushi.logging.Logger;

public final class DirectoryUtils {
	private static final Logger logger = new Logger(DirectoryUtils.class);

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
	
	public static void possiblyCreateTmpDir(Options options) throws IOException {
		logger.debug("Creating experiment directories");
		
		final Path path = getJBSEOutDirPath(options);
		try {
			Files.createDirectories(path); //this creates the temporary directory and all the subdirectories for the wrappers
		} catch (IOException e) {
			logger.error("Unable to create experiment directories: ", e);
			throw e;
		}

		logger.debug("Creating experiment directories - done");
	}
	
	public static String getJBSEOutClass(long targetMethodNumber, long traceNumberLocal) {
		return jbseGeneratedOutClass + "_" + targetMethodNumber + "_" + traceNumberLocal;
	}

	public static String getJBSEOutClassQualified(Options options, long targetMethodNumber, long traceNumberLocal) {
		final String targetClass = (options.getTargetClass() == null ? options.getTargetMethod().get(0) : options.getTargetClass()); 
		final int endOfPackageNameIndex = targetClass.lastIndexOf('/');
		final String targetClassPackageName = (endOfPackageNameIndex == -1 ? "" : (targetClass.substring(0, endOfPackageNameIndex) + ".")).replace('/', '.');
		return targetClassPackageName + getJBSEOutClass(targetMethodNumber, traceNumberLocal);
	}

	public static Path getJBSEOutFilePath(Options options, long targetMethodNumber, long traceNumberLocal) {
		return getJBSEOutDirPath(options).resolve(getJBSEOutClass(targetMethodNumber, traceNumberLocal) + javaSourceExtension);
	}	
	
	public static Path getTmpDirPath(Options options) {
		return options.getTmpDirectoryBase().resolve(options.getTmpDirectoryName());
	}
	
	public static Path getJBSEOutDirPath(Options options) {
		final String targetClass = (options.getTargetClass() == null ? options.getTargetMethod().get(0) : options.getTargetClass()); 
		final int endOfPackageNameIndex = targetClass.lastIndexOf('/');
		final String targetClassPackageName = (endOfPackageNameIndex == -1 ? "" : targetClass.substring(0, endOfPackageNameIndex));
		return getTmpDirPath(options).resolve(targetClassPackageName);
	}
	
	public static Path getMethodsFilePath(Options options) {
		return getTmpDirPath(options).resolve(methodsFileName);
	}
	
	public static Path getBranchesFilePath(Options options) {
		return getTmpDirPath(options).resolve(branchesFileName);
	}
	
	public static Path getBranchesFilePath(Options options, long i) {
		return getTmpDirPath(options).resolve(branchesFileNamePattern.replace("$", Long.toString(i)));
	}
	
	public static Path getCoverageFilePath(Options options) {
		return getTmpDirPath(options).resolve(coverageFileName);
	}

	public static Path getCoverageFilePath(Options options, long i) {
		return getTmpDirPath(options).resolve(coverageFileNamePattern.replace("$", Long.toString(i)));
	}

	public static Path getTracesFilePath(Options options) {
		return getTmpDirPath(options).resolve(tracesFileName);
	}
	
	public static Path getTracesFilePath(Options options, long i) {
		return getTmpDirPath(options).resolve(tracesFileNamePattern.replace("$", Long.toString(i)));
	}	
	
	public static Path getBranchesToIgnoreFilePath(Options options) {
		return getTmpDirPath(options).resolve(branchesToIgnoreFileName);
	}
	
	public static Path getTracesToIgnoreFilePath(Options options) {
		return getTmpDirPath(options).resolve(tracesToIgnoreFileName);
	}
	
	public static Path getMinimizerOutFilePath(Options options) {
		return getTmpDirPath(options).resolve(minimizerOutFileName);
	}
	
	public static Path getCoveredByTestFilePath(Options options) {
		return getTmpDirPath(options).resolve(coveredByTestFileName);
	}
	
	/**
	 * Do not instantiate!
	 */
	private DirectoryUtils() {
		//nothing to do
	}
}
