package sushi.coverage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;

import sushi.logging.Level;
import sushi.logging.Logger;

/**
 * A small utility that calculates the statement and branch coverage 
 * of a test suite.
 * 
 * @author Giovanni Denaro
 *
 */
public final class CoverageCalculator {
	private static final Logger logger = new Logger(CoverageCalculator.class);
	
	private final IRuntime runtime;
	private final InstrumentingClassLoader instrumentingClassLoader;
	
	/**
	 * Entry point to run this as a Java application.
	 * 
	 * @param args
	 *            list of program arguments
	 */
	public static void main(final String[] args) {
		Path baseTestFolder = Paths.get("/");
		String[] tsuite = null;
		String[] covTargets = null;
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-testsrcfolder")) {
				baseTestFolder = Paths.get(args[++i]);
				baseTestFolder = baseTestFolder.toAbsolutePath();
			} else if (args[i].equals("-testsuite")) {
				tsuite = args[++i].split(":");
			} else if (args[i].equals("-covtargets")) {
				covTargets = args[++i].split(":");
			} else if (args[i].equals("-v")) {
				Logger.setLevel(Level.DEBUG);
			} 
		}

		if (tsuite == null || covTargets == null) {
			logger.info("Usage: java " + CoverageCalculator.class.getCanonicalName() + " [-v] [-testsrcfolder folder] -testsuite test_suite -covtargets covTarget[:covTarget[...]]");
			System.exit(1);
		}
		
		tsuite = expandTestSuiteWithWildChars(tsuite, baseTestFolder);
		
		logger.debug("Calculating coverage of test suite in classes " + Arrays.toString(tsuite) + " with targets in " + Arrays.toString(covTargets));
		try {
			new CoverageCalculator().displayCoverage(tsuite, covTargets, false);
		} catch (Exception e) {
			logger.error("Error while calculating coverage", e);
			System.exit(1);
		}
		
		System.exit(0);
	}

	private static String[] expandTestSuiteWithWildChars(String[] tSuite, Path baseTestFolder) {
		List<String> newTSuite = new ArrayList<>();
		for (String tCase : tSuite) {
			if (!tCase.endsWith(".*")) {
				newTSuite.add(tCase);
				continue;
			}
			
			String packageName = tCase.substring(0, tCase.length() - 1);
			File sourceFolder = baseTestFolder.resolve(packageName.replace('.', '/')).toFile();
			if (!sourceFolder.exists()) {
				throw new RuntimeException("Folder " + sourceFolder + " does not exist");
			}
			File[] javaFiles = sourceFolder.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".java") && name.contains("Test");
				}

			});

		    for (int i = 0; i < javaFiles.length; i++) {
		    	String newTCase =  packageName + javaFiles[i].getName();
				newTSuite.add(newTCase.substring(0, newTCase.length() - 5));
		    }
		}
		
		return newTSuite.toArray(new String[newTSuite.size()]);
	}
	
	private CoverageCalculator() {	
		// For instrumentation and runtime we need a IRuntime instance
		// to collect execution data:
		runtime = new LoggerRuntime();
		
		// We use a special class loader to directly load the
		// instrumented class definition from a byte[] instances.
		instrumentingClassLoader = new InstrumentingClassLoader(CoverageCalculator.class.getClassLoader(), runtime);	
	}
	
	private void 
	displayCoverage(String[] testSuiteClassNames, String[] coverageTargetClassNames, boolean verboseTestExecution) 
	throws Exception {		
		final HashMap<Method, ExecutionDataStore> executionDataMap = 
				executionDataForTheTestMethods(testSuiteClassNames, coverageTargetClassNames, verboseTestExecution);
		calculateBranchAndInstructionCoverage(executionDataMap, coverageTargetClassNames);
	}

	private HashMap<Method, ExecutionDataStore> 
	executionDataForTheTestMethods(String testSuiteClassNames[], String[] coverageTargetClassNames, boolean verboseTestExecution) 
	throws Exception {
		System.setSecurityManager(new NoExitSecurityManager());
		
		// Now we're ready to run our instrumented class and need to startup the
		// runtime first:
		final RuntimeData data = new RuntimeData();
		runtime.startup(data);
		
		final HashMap<Method, ExecutionDataStore> coverageDataByTestMethod = new HashMap<Method, ExecutionDataStore>();
		
		for (String testSuiteClassName : testSuiteClassNames) {
			// We load and instrument the test suite
			Class<?> testSuiteClass = Class.forName(testSuiteClassName, true, instrumentingClassLoader);
			
			for (Method testMethod: testSuiteClass.getDeclaredMethods()) {
				logger.debug("Analyzing method " + testMethod.getName());
				
				// We consider only methods with a "Test..." annotation
				Annotation[] annotations = testMethod.getAnnotations();
				if (annotations.length == 0 || !annotations[0].toString().contains("Test")) {
					logger.debug("Skipping method " + testMethod.getName());
					continue;
				}
				
				if (annotations.length != 0) {
					String name = annotations[0].toString();
					name.toString();
				}

				Object instance = testSuiteClass.newInstance();
				try {
					logger.debug("Executing test case " + testMethod);
					testMethod.invoke(instance, new Object[testMethod.getParameterTypes().length]);
				} catch (IllegalAccessException | IllegalArgumentException e) {
					throw e;
				} catch (InvocationTargetException e) {
					// We must ignore the exceptions thrown during the execution of testMthod
				}

				// At the end of test execution we collect execution data 
				// NB: We reset the execution data (for the next test method)
				ExecutionDataStore executionDataStore = collectExecutionDataAndReset(data);
				coverageDataByTestMethod.put(testMethod, executionDataStore);

				// We log the execution data (only for debug purpose)
				if (verboseTestExecution) 
					logExecutionData(testMethod, executionDataStore, coverageTargetClassNames);
			}	
		}
		runtime.shutdown();
		
		System.setSecurityManager(null);
		return coverageDataByTestMethod;
	}

	private ExecutionDataStore collectExecutionDataAndReset(RuntimeData data) throws IOException {
		// We collect execution data in a byte array and reset
		final ByteArrayOutputStream byteOStream = new ByteArrayOutputStream();
		data.collect(new ExecutionDataWriter(byteOStream), new SessionInfoStore(), /*reset data:*/true);
		final byte[] executionDataAsBytes = byteOStream.toByteArray();

		// We store the execution data in our coverage data map
		ExecutionDataReader executionDataReader = new ExecutionDataReader(new ByteArrayInputStream(executionDataAsBytes));
		ExecutionDataStore executionDataStore = new ExecutionDataStore();
		executionDataReader.setExecutionDataVisitor(executionDataStore);
		executionDataReader.read();

		return executionDataStore;
	}

	private void logExecutionData(Method testMethod, ExecutionDataStore executionDataStore, 
			String[] coverageTargetClasses) throws IOException {
		if (coverageTargetClasses == null)
			logger.debug("Working with no specified coverage targets (that is, consider all traversed classes as targets):" + 
					"verbose output is disabled in this case");
		// We log the execution data for debug purpose
		// NB: here we do not log data related to inner classes 
		logger.debug("**************");
		logger.debug("Executed method " + testMethod);
		logger.debug("**************");

		final CoverageBuilder coverageBuilder = new CoverageBuilder();
		final Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
		for (String targetName : coverageTargetClasses) {
			analyzer.analyzeClass(getTargetClass(targetName), targetName);
		}
		
		logger.debug("\n");
		logger.debug("=============================================");
		logger.debug("=============================================");
		logger.debug("\n");
		for (final IClassCoverage cc : coverageBuilder.getClasses()) {
			System.out.printf("Coverage of class %s%n", cc.getName());
			printCounter("instructions", cc.getInstructionCounter());
			printCounter("branches", cc.getBranchCounter());
			printCounter("lines", cc.getLineCounter());
			printCounter("methods", cc.getMethodCounter());
			printCounter("complexity", cc.getComplexityCounter());
			
			for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
				System.out.printf("Line %s: %s%n", Integer.valueOf(i), getColor(cc
						.getLine(i).getStatus()));
				if (cc.getLine(i).getBranchCounter().getTotalCount() > 0)
					logger.debug("-->Branch :" + cc.getLine(i).getBranchCounter().getTotalCount());
			}
			
		}		
	}
	
	private void 
	calculateBranchAndInstructionCoverage(HashMap<Method, ExecutionDataStore> executionDataMap, String[] coverageTargetClasses) 
	throws IOException, ClassNotFoundException {
		if (coverageTargetClasses == null) {
			logger.debug("Not implemeted yet: Working with no specified coverage targets shall consider all traversed classes as targets");
			throw new RuntimeException("Not implemeted yet: Working with no specified coverage targets shall consider all traversed classes as targets");
		}

		// Unfold inner classes in coverage targets
		final Map<String, byte[]> covTargetClasses = new HashMap<String, byte[]>();
		for (String name : coverageTargetClasses)  {
			int classDim = getTargetClassSize(name);
			byte[] classBytes = new byte[classDim];
			getTargetClass(name).read(classBytes);
			covTargetClasses.put(name, classBytes); //consider the outer class...
			Class<?> clazz = Class.forName(name, true, instrumentingClassLoader);
			//...and the related inner classes
			for (Class<?> inner : clazz.getDeclaredClasses()) {
				classDim = getTargetClassSize(name);
				classBytes = new byte[classDim];
				getTargetClass(name).read(classBytes);
				covTargetClasses.put(inner.getName(), classBytes);
			}
		}

		ExecutionDataStore totalCovDataStore = null;
		int totalBranchCov = 0;
		int totalInstrCov = 0;
		int totalRefBranchCov = 0;
		int totalRefInstrCov = 0;
		
		int methodCount = 0;
		int methodWithErrorsCount = 0;
		int classesInStore = 0;
		for (Method testMethod: executionDataMap.keySet()) {
			++methodCount;
			//logger.debug((methodCount) + ": checking cov of " + testMethod + " -- errors: " + methodWithErrorsCount);
			//Add coverage data of m
			final ExecutionDataStore testDataStore = executionDataMap.get(testMethod);
			if (totalCovDataStore == null) {
				totalCovDataStore = testDataStore;
			}
			else {
				for (ExecutionData testDataOfClass: testDataStore.getContents()) {
					long classId = testDataOfClass.getId();
					ExecutionData totalCovDataOfClass = totalCovDataStore.get(classId);
					if (totalCovDataOfClass == null) {
						// add as new item to the data of this class
						totalCovDataStore.visitClassExecution(testDataOfClass);
					}
					else {
						//merge to the data of this class
						totalCovDataOfClass.merge(testDataOfClass);
					}
				}				
			}
			
			int updtClassesInStore = totalCovDataStore.getContents().size();
			if (updtClassesInStore < classesInStore) {
				logger.error("The number of executed classes cannot decrease (" + classesInStore + "-->" +
						classesInStore + ") while cumulating coverage of test cases");
			}
			else if (updtClassesInStore > classesInStore) {
				classesInStore = totalCovDataStore.getContents().size();
			}
			
			// Compute coverage indicators
			int currBranchCov = 0;
			int currInstrCov = 0;
			int currRefBranchCov = 0;
			int currRefInstrCov = 0;
			final CoverageBuilder coverageBuilder = new CoverageBuilder();
			final Analyzer analyzer = new Analyzer(totalCovDataStore, coverageBuilder);
			try {
				for (String targetClass : covTargetClasses.keySet()) {
					analyzer.analyzeClass(covTargetClasses.get(targetClass), targetClass);
				}
			} catch (Exception e) {
				methodWithErrorsCount++;
				continue;
			}
			for (final IClassCoverage cc : coverageBuilder.getClasses()) {
				currBranchCov += cc.getBranchCounter().getCoveredCount();				
				currInstrCov += cc.getInstructionCounter().getCoveredCount();				
				currRefBranchCov += cc.getBranchCounter().getTotalCount();				
				currRefInstrCov += cc.getInstructionCounter().getTotalCount();						
			}
			
			// If coverage increases, update the minimal test suite
			logger.debug("Test method: " + testMethod); 
			if(currBranchCov > totalBranchCov || currInstrCov > totalInstrCov) {
				logger.debug("Coverage increases: " + 
						totalBranchCov + " (out of " + totalRefBranchCov + ") branches and " + 
						totalInstrCov + " (out of " + totalRefInstrCov + ") instructions" +
						" --> " + 
						currBranchCov  + " (out of " + currRefBranchCov + ") branches and " + 
						currInstrCov + " (out of " + currRefInstrCov + ") instructions");
				totalBranchCov = currBranchCov;
				totalInstrCov = currInstrCov;
				totalRefBranchCov = currRefBranchCov;
				totalRefInstrCov = currRefInstrCov;
				
				for (final IClassCoverage cc : coverageBuilder.getClasses()) {
					logger.debug("Coverage of class " + cc.getName());
					printCounter("instructions", cc.getInstructionCounter());
					printCounter("branches", cc.getBranchCounter());
					printCounter("lines", cc.getLineCounter());
					printCounter("methods", cc.getMethodCounter());
					printCounter("complexity", cc.getComplexityCounter());
					
					for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
						logger.debug("Line " + Integer.valueOf(i) +": " + getColor(cc
								.getLine(i).getStatus()));
						if (cc.getLine(i).getBranchCounter().getTotalCount() > 0)
							logger.debug("-->Branch :" + cc.getLine(i).getBranchCounter().getTotalCount());
					}
				}
			} else {
				logger.debug("Coverage does not increase: " + 
						currBranchCov  + " (out of " + currRefBranchCov + ") branches and " + 
						currInstrCov + " (out of " + currRefInstrCov + ") instructions");
			}
		}
		logger.info("Analyzed data for " + methodCount + " test cases");
		logger.debug("...with " + methodWithErrorsCount + " errors");
		logger.info("In total covered " + 
				totalBranchCov + " (out of " + totalRefBranchCov + ") branches");
		logger.debug("In total covered " + 
				totalInstrCov + " (out of " + totalRefInstrCov + ") instructions");
	}
	
	private int getTargetClassSize(final String name) throws IOException {
		final byte[] store = new byte[1000];
		final InputStream is = getTargetClass(name);
		int classDim = 0;
		int readBytes = 0;
		while ((readBytes = is.read(store)) != -1) {
			//TODO check for overflow?
			classDim += readBytes;
		}
		return classDim;
	}

	private InputStream getTargetClass(final String name) {
		final String resource = '/' + name.replace('.', '/') + ".class";
		return getClass().getResourceAsStream(resource);
	}

	private void printCounter(final String unit, final ICounter counter) {
		final Integer missed = Integer.valueOf(counter.getMissedCount());
		final Integer total = Integer.valueOf(counter.getTotalCount());
		logger.debug("missed " + missed + " of " + total + " in " + unit);
	}

	private String getColor(final int status) {
		switch (status) {
		case ICounter.NOT_COVERED:
			return "red";
		case ICounter.PARTLY_COVERED:
			return "yellow";
		case ICounter.FULLY_COVERED:
			return "green";
		}
		return "";
	}

	/**
	 * A class loader that loads classes from instrumented in-memory data.
	 */
	private class InstrumentingClassLoader extends ClassLoader {

		private final Instrumenter instr;
    	private final Collection<String> doNotInstrument_packageSubstrings;
		private final Map<String, Class<?>> instrumentedClasses;

		public InstrumentingClassLoader(ClassLoader parent, IRuntime coveDataRuntime) {
	        super(parent);
	  
	        instr = new Instrumenter(runtime);
	  
	        doNotInstrument_packageSubstrings = new HashSet<String>();
	    	doNotInstrument_packageSubstrings.add("java.");
	    	doNotInstrument_packageSubstrings.add("sun.");
	    	doNotInstrument_packageSubstrings.add("jacoco.");
	    	doNotInstrument_packageSubstrings.add("jbse.meta.");
	    	
	    	instrumentedClasses = new HashMap<String, Class<?>>();
	    }
	    
	    private boolean jumpInstrument(String className) {
	    	if (className == null)
	    		return true;
	    	if(className.indexOf('.') == -1)
	    		return false;
	        for (String packageSubstring : doNotInstrument_packageSubstrings) {
	        	if (className.contains(packageSubstring)) 
	        		return true;
	        }
	        return false;
	    }
	    
		@Override
		protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
			logger.debug("Loading class " + name);
			try {
				if (!jumpInstrument(name)) {
					Class<?> clazz = instrumentedClasses.get(name);
					
					// The class is already instrumented, just return it
					if (clazz != null) {
						logger.debug(" ***already there");
						return clazz;
					}
					
					// Do instrumentation and return
					byte[] instrumented = instr.instrument(getTargetClass(name), name);
					clazz = defineClass(name, instrumented, 0, instrumented.length);
					instrumentedClasses.put(name, clazz);
					logger.debug("Loaded and instrumented " + name);
					return clazz;
				}
			} catch (Throwable e) {
				logger.error("Error while loading instrumented class " + name, e);
			}
			
			// The class must not or cannot be instrumented, delegate loading to system class loader
			logger.debug("Delegating (not instrumenting) " + name);
			return super.loadClass(name, resolve);
		}
	}
	
	private static class ExitException extends SecurityException {
		private static final long serialVersionUID = 6328690905970098721L;		
	}
	
	/**
	 * http://stackoverflow.com/questions/309396
	 */
	private static class NoExitSecurityManager extends SecurityManager {
		@Override
		public void checkPermission(Permission perm) { }
		@Override
		public void checkPermission(Permission perm, Object context) { }
		
		@Override
		public void checkExit(int status) {
			super.checkExit(status);
			throw new ExitException();
		}
	}
}
