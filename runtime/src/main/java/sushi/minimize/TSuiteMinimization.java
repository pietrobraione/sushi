package sushi.minimize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

public final class TSuiteMinimization {
	private static final Logger logger = new Logger(TSuiteMinimization.class);
	
	private final IRuntime runtime;
	private final InstrumentingClassLoader instrumentingClassLoader;
	
	public TSuiteMinimization() {	
		// For instrumentation and runtime we need a IRuntime instance
		// to collect execution data:
		runtime = new LoggerRuntime();
		
		// We use a special class loader to directly load the
		// instrumented class definition from a byte[] instances.
		instrumentingClassLoader = new InstrumentingClassLoader(TSuiteMinimization.class.getClassLoader(), runtime);	
	}
	
	/**
	 * Run the minimization algorithm with reference to branch and instruction coverage.
	 * 
	 * @throws Exception
	 *             in case of errors
	 */
	public List<Method> 
	minimizeAgainstBranchCoverage(String[] testSuiteClassNames, String[] coverageTargetClassNames, boolean verboseTestExecution) 
	throws Exception {		
		final HashMap<Method, ExecutionDataStore> executionDataMap = 
				executionDataForTheTestMethods(testSuiteClassNames, coverageTargetClassNames, verboseTestExecution);
		final List<Method> minimizedSuite = 
				minimizeAgainstBranchAndInstructionCoverage(executionDataMap, coverageTargetClassNames);
		return minimizedSuite;
	}

	private HashMap<Method, ExecutionDataStore> 
	executionDataForTheTestMethods(String testSuiteClassNames[], String[] coverageTargetClassNames, boolean verboseTestExecution) 
	throws Exception {
		
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
				if (annotations.length == 0 || annotations[0].getClass().getName().contains("Test")) {
					logger.debug("Skipping method " + testMethod.getName());
					continue;
				}
				
				Object instance = testSuiteClass.newInstance();
				logger.info("Executing method " + testMethod);
				final ExecutorService ex = Executors.newCachedThreadPool();
				final Callable<Object> task = 
						() -> testMethod.invoke(instance, new Object[testMethod.getParameterTypes().length]);
				final Future<Object> f = ex.submit(task);
				try {
					f.get(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					throw e; //this should not happen
				} catch (TimeoutException e) {
					//do nothing
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					if (cause instanceof IllegalAccessException) {
						throw (IllegalAccessException) cause;
					} else if (cause instanceof IllegalArgumentException) { 
						throw (IllegalArgumentException) cause;
					} else if (cause instanceof InvocationTargetException) {
						// We must ignore the exceptions thrown during the execution of testMethod
					} else {
						throw e; //this should not happen
					}
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
	
	private List<Method> 
	minimizeAgainstBranchAndInstructionCoverage(HashMap<Method, ExecutionDataStore> executionDataMap, String[] coverageTargetClasses) 
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

		final List<Method> minimizedSuite = new ArrayList<Method>();
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
				
				/*
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
				}*/

			}
			
			// If coverage increases, update the minimal test suite
			if(currBranchCov > totalBranchCov || currInstrCov > totalInstrCov) {
				logger.debug("Coverage increases: " + 
						totalBranchCov + " (out of " + totalRefBranchCov + ") branches and " + 
						totalInstrCov + " (out of " + totalRefInstrCov + ") instructions" +
						" --> " + 
						currBranchCov  + " (out of " + currRefBranchCov + ") branches and " + 
						currInstrCov + " (out of " + currRefInstrCov + ") instructions" +
						"\n --> update minimal test suite with: " + testMethod);
				minimizedSuite.add(testMethod);
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

			}
		}
		logger.debug("Analyzed data for " + methodCount + " methods, with " + methodWithErrorsCount + " errors");

		logger.info("In total covered " + 
				totalBranchCov + " (out of " + totalRefBranchCov + ") branches and " + 
				totalInstrCov + " (out of " + totalRefInstrCov + ") instructions");

		return minimizedSuite;
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
	
	/**
	 * Entry point to run this as a Java application.
	 * 
	 * @param args
	 *            list of program arguments
	 */
	public static void main(final String[] args) {
		Logger.setLevel(Level.DEBUG);
		String outFile = null;
		String[] tsuite = null;
		String[] covTargets = null;
		if (args.length == 2) {
			tsuite = args[0].split(":");
			covTargets = args[1].split(":");
		} else if (args.length == 4 && args[0].equals("-f")) {
			outFile = args[1];
			tsuite = args[2].split(":");
			covTargets = args[3].split(":");
		} else {
			tsuite = 
					//new String[] {"dll_hard.Main_ESTest"};
					//new String[] {"dll_hard.otherTools.PexTests"};
					//new String[] {"dll_hard.otherTools.Seeker3Tests"};
					//TestSuites.dll_hardSushi();

					//new String[] {"treemap.otherTools.TreeMap_ESTest"};
					//new String[] {"treemap.otherTools.PexTests"};
					//new String[] {"treemap.otherTools.Seeker3Tests"};
					//TestSuites.treemapSushi();
					
					//new String[] {"avl_tree.otherTools.AvlTree_ESTest"};
					//new String[] {"avl_tree.otherTools.PexTests"};
					//new String[] {"avl_tree.otherTools.Seeker3Tests"};
					//TestSuites.avlSushi();

					//new String[] {"node_caching_linked_list.otherTools.NodeCachingLinkedList_ESTest"};
					//new String[] {"node_caching_linked_list.otherTools.PexTests"};
					//new String[] {"node_caching_linked_list.otherTools.Seeker3Tests"};
					//TestSuites.cachingSushi();
					
					new String[] {"tsafe.otherTools.Driver_TS_R_ESTest"};
					//new String[] {"tsafe.otherTools.PexTests"};
					//new String[] {"tsafe.otherTools.Seeker3Tests"};
					//TestSuites.tsafeSushi();
					
					//new String[] {"ganttproject.DependencyGraph_ESTest"};
					//new String[] {"ganttproject.PexTests"};
					//new String[] {"ganttproject.Seeker3Tests"};
					//TestSuites.ganttSushi();
			
					//new String[] {"com.google.javascript.jscomp.RemoveUnusedVars_ESTest"};
					//TestSuites.closure01Sushi();
					
					//new String[] {"com.google.javascript.jscomp.RenameLabels_ESTest"};
					//TestSuites.closure72Sushi();
			
					//TestSuites.closure11Sushi();
			covTargets = new String[] {
					//"dll_hard.Main"
					//"treemap.TreeMap"
					//"avl_tree.AvlTree"
					//"node_caching_linked_list.NodeCachingLinkedList"
					"tsafe.Driver_TS_R"
					//"ganttproject.DependencyGraph"
					
					/*closure01*//*"com.google.javascript.jscomp.RemoveUnusedVars", 
					"com.google.javascript.jscomp.RemoveUnusedVars$CallSiteOptimizer", 
					"com.google.javascript.jscomp.RemoveUnusedVars$Continuation", 
					"com.google.javascript.jscomp.RemoveUnusedVars$Assign"*/
					
					/*closure72*//*"com.google.javascript.jscomp.RenameLabels",
					"com.google.javascript.jscomp.RenameLabels$DefaultNameSupplier",
					"com.google.javascript.jscomp.RenameLabels$ProcessLabels",
					"com.google.javascript.jscomp.RenameLabels$LabelInfo",
					"com.google.javascript.jscomp.RenameLabels$LabelNamespace"*/

					/*closure11*///"com.google.javascript.jscomp.TypeCheck"
			};
			
			logger.info("Usage: java " + TSuiteMinimization.class.getCanonicalName() + " [-f fileName] test_suite covTarget[:covTarget[...]]");
			//System.exit(1);
		}
		
		logger.debug("Minimizing test suite in class " + tsuite + " with targets in " + covTargets);

		List<Method> minimizedSuite = null;
		try {
			minimizedSuite = 
				new TSuiteMinimization().minimizeAgainstBranchCoverage(tsuite, covTargets, false);
		} catch (Exception e) {
			logger.error("Error while minimizing", e);
			System.exit(1);
		}
		logger.info("Minimized test suite includes " + minimizedSuite.size() + " test methods");
		
		for (Method test: minimizedSuite) {
			logger.info(test.toString());
		}
		
		if (outFile != null) {
			try (final PrintWriter writer = new PrintWriter(outFile)) {
				for (Method test: minimizedSuite) {
					writer.println(test.getName());
				}
			} catch (FileNotFoundException e) {
				logger.error("Error while opening the output file", e);
				System.exit(1);
			}			
		}
		
		System.exit(0);
	}
}
