package sushi.configure;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import jbse.apps.run.CannotBuildDecisionProcedureException;
import jbse.apps.run.RunParameters;
import jbse.bc.Classpath;
import jbse.bc.Signature;
import jbse.dec.DecisionProcedure;
import jbse.dec.DecisionProcedureAlgorithms;
import jbse.dec.DecisionProcedureAlwSat;
import jbse.dec.DecisionProcedureClassInit;
import jbse.jvm.EngineParameters;
import jbse.jvm.RunnerParameters;
import jbse.jvm.EngineParameters.BreadthMode;
import jbse.jvm.EngineParameters.StateIdentificationMode;
import jbse.mem.State;
import jbse.rewr.CalculatorRewriting;
import jbse.rewr.Rewriter;
import jbse.rules.ClassInitRulesRepo;
import jbse.rules.LICSRulesRepo;
import jbse.val.ReferenceSymbolic;

public final class JBSEParameters implements Cloneable {
	/**
	 * Enumeration of the possible decision procedures.
	 * 
	 * @author Pietro Braione
	 */
	public static enum DecisionProcedureType {
		/** Uses Z3. */
		Z3,
		
		/** Uses CVC4. */
		CVC4
	}

	/**
	 * A Strategy for creating {@link DecisionProcedure}s. 
	 * The strategy receives as inputs the necessary dependencies
	 * to inject in it, an must return the decision procedure
	 * object.
	 * 
	 * @author Pietro Braione
	 *
	 */
	@FunctionalInterface
	public interface DecisionProcedureCreationStrategy {
		/**
		 * Creates a {@link DecisionProcedure}.
		 * 
		 * @param core a previously built {@link DecisionProcedure}.
		 * @param calc a {@link CalculatorRewriting}.
		 * @return a new {@link DecisionProcedure} that (possibly) has {@code core} 
		 *         as Decorator component, or next decision procedure in the 
		 *         Chain Of Responsibility, and (possibly) uses {@code calc} for 
		 *         calculations and simplifications.
		 */
		DecisionProcedure createAndWrap(DecisionProcedure core, CalculatorRewriting calc)
		throws CannotBuildDecisionProcedureException;
	}
	
	/**
	 * The trace (leaf) types.
	 * 
	 * @author Pietro Braione
	 */
	public enum TraceTypes {
		/** 
		 * A safe leaf, i.e., the final state of a 
		 * trace that does not violate any assertion
		 * or assumption. 
		 */
		SAFE,

		/** 
		 * An unsafe leaf, i.e., the final state of a 
		 * trace that violates an assertion. 
		 */
		UNSAFE,

		/** 
		 * A leaf that exhausts a bound.
		 */
		OUT_OF_SCOPE,
		
		/**
		 * A contradictory leaf, i.e, the final 
		 * state of a trace that violates an 
		 * assumption.
		 */
		CONTRADICTORY
	}

	/**
	 * Enumeration of the possible output display formats.
	 * 
	 * @author Pietro Braione
	 */
	public enum StateFormatMode {
		/**
		 * Displays an EvoSuite wrapper class for Sushi;
		 * distance is calculated by comparing against
		 * a partial heap built from the path condition.
		 */
		SUSHI_PARTIAL_HEAP,
		
        /**
         * Displays an EvoSuite wrapper class for Sushi;
         * distance is calculated by directly comparing 
         * against the path condition clauses.
         */
		SUSHI_PATH_CONDITION
	}

	/** The runner parameters. */
	private RunnerParameters runnerParameters;

	/** The {@link Class}es of all the rewriters to be applied to terms (order matters). */
	private ArrayList<Class<? extends Rewriter>> rewriterClasses = new ArrayList<>();
	
	/**
	 * The decision procedure to be used for deciding the 
	 * arithmetic conditions.
	 */
	private DecisionProcedureType decisionProcedureType = DecisionProcedureType.Z3;
	
	/** The {@link Path} where the executable of the external decision procedure is. */
	private Path externalDecisionProcedurePath = null;

	/** 
	 * Whether the engine should use its sign analysis 
	 * decision support.
	 */
	private boolean doSignAnalysis = false;
	
	/** Whether the engine should do sign analysis before invoking the decision procedure. */
	private boolean doEqualityAnalysis = false;
	
	/** 
	 * Whether the engine should use the LICS decision procedure.
	 * Set to true by default because the LICS decision procedure
	 * also resolves class initialization. 
	 */
	private boolean useLICS = true;
	
	/** The {@link LICSRuleRepo}, containing all the LICS rules. */
	private LICSRulesRepo repoLICS = new LICSRulesRepo();
	
    /** The {@link ClassInitRulesRepo}, containing all the class initialization rules. */
    private ClassInitRulesRepo repoInit = new ClassInitRulesRepo();
    
	/** 
	 * Whether the engine should use the conservative 
	 * repOK decision procedure.
	 */
    private boolean useConservativeRepOks = false;
	
	/**
	 *  Associates classes with the name of their respective
	 *  conservative repOK methods. 
	 */
	private HashMap<String, String> conservativeRepOks = new HashMap<>();

	/** The heap scope for conservative repOK execution. */
	private HashMap<String, Function<State, Integer>> concretizationHeapScope = new HashMap<>();

	/** The depth scope for conservative repOK execution. */
	private int concretizationDepthScope = 0;

	/** The count scope for conservative repOK execution. */
	private int concretizationCountScope = 0;
	
	/** The {@link DecisionProcedureCreationStrategy} list. */
	private ArrayList<DecisionProcedureCreationStrategy> creationStrategies = new ArrayList<>();
	
	/** The identifying number of the method to be executed. */
	private long methodNumber = 0;
	
	/** The start number of the trace counter. */
	private long traceCounterStart = 0;
	
	/** Whether it must emit coverage, branches and traces log files. */
	private boolean mustLogCoverageData = true;
	
	/** The builder function for the wrapper file path. */
	private BiFunction<Long, Long, Path> wrapperFilePathBuilder = null;

	/** The builder function for the coverage file path. */
	private Function<Long, Path> coverageFilePathBuilder = null;

	/** The builder function for the branches file path. */
	private Function<Long, Path> branchesFilePathBuilder = null;

	/** The builder function for the traces file path. */
	private Function<Long, Path> tracesFilePathBuilder = null;

	/** The traces to show. */
	private EnumSet<TraceTypes> tracesToShow = EnumSet.allOf(TraceTypes.class);

	/** The format mode. */
	private StateFormatMode stateFormatMode = StateFormatMode.SUSHI_PATH_CONDITION;
	
    /** Whether the symbolic execution is guided along a concrete one. */
    private boolean guided = false;
    
	/** The signature of the driver method when guided == true. */
    private Signature driverSignature = null;
	
	/**
	 * Constructor.
	 */
	public JBSEParameters() {
		this.runnerParameters = new RunnerParameters();
	}
	
	/**
	 * Constructor.
	 * 
	 * @param runnerParameters a {@link RunnerParameters} object.
	 *        The created object will be backed by {@code runnerParameters}.
	 */
	public JBSEParameters(RunnerParameters runnerParameters) {
		this.runnerParameters = runnerParameters;
	}
	
	/**
	 * Gets the embedder {@link RunnerParameters} object.
	 * 
	 * @return the {@link RunnerParameters} that backs this 
	 *         {@link RunParameters} object.
	 */
	public RunnerParameters getRunnerParameters() {
		return this.runnerParameters;
	}

	/**
	 * Sets the state identification mode, i.e., how a state will be
	 * identified.
	 * 
	 * @param stateIdMode a {@link StateIdentificationMode}.
	 * @throws NullPointerException if {@code stateIdMode == null}.
	 */
	public void setStateIdentificationMode(StateIdentificationMode stateIdMode) {
		this.runnerParameters.setStateIdentificationMode(stateIdMode);
	}
	
	/**
	 * Sets the breadth mode, i.e., how many branches 
	 * will be created during execution.
	 * 
	 * @param breadthMode a {@link BreadthMode}.
	 * @throws NullPointerException if {@code breadthMode == null}.
	 */
	public void setBreadthMode(BreadthMode breadthMode) {
		this.runnerParameters.setBreadthMode(breadthMode);
	}

	/**
	 * Sets the symbolic execution's classpath; the 
	 * default classpath is {@code "."}.
	 * 
	 * @param paths a varargs of {@link String}, 
	 *        the paths to be added to the classpath.
	 */
	public void addClasspath(String... paths) { 
		this.runnerParameters.addClasspath(paths);
	}
	
    /**
     * Clears the symbolic execution's classpath.
     */
	public void clearClasspath() {
	    this.runnerParameters.clearClasspath();
	}

	/**
	 * Returns the symbolic execution's classpath.
	 * 
	 * @return a {@link Classpath} object. 
	 */
	public Classpath getClasspath() {
		return this.runnerParameters.getClasspath();
	}

	/**
	 * Sets the signature of the method which must be symbolically executed.
	 * 
	 * @param className the name of the class containing the method.
	 * @param parametersSignature the types of the method parameters.
	 * @param methodName the name of the method. 
	 * @throws NullPointerException if any of the above parameters is {@code null}.
	 */
	public void setMethodSignature(String className, String parametersSignature, String methodName) { 
		this.runnerParameters.setMethodSignature(className, parametersSignature, methodName); 
	}
	
	/**
	 * Gets the signature of the method which must be symbolically executed.
	 * 
	 * @return a {@link Signature}, or {@code null} if no method signature
	 *         has been provided.
	 */
	public Signature getMethodSignature() {
		return this.runnerParameters.getMethodSignature();
	}
	
	/**
	 * Specifies an alternative, meta-level implementation of a method 
	 * that must override the standard one. 
	 * 
	 * @param className the name of the class containing the overridden method.
	 * @param parametersSignature the types of the method parameters.
	 * @param methodName the name of the method.
	 * @param metaDelegateClassName the name of a {@link Class} that implements
	 *        the semantics of calls to the {@code methodName} method.
	 * @throws NullPointerException if any of the above parameters is {@code null}.
	 */
	public void addMetaOverridden(String className, String parametersSignature, String methodName, String metaDelegateClassName) {
		this.runnerParameters.addMetaOverridden(className, parametersSignature, methodName, metaDelegateClassName);
	}

	/**
	 * Specifies that a method must be treated as an uninterpreted pure
	 * function, rather than executed.
	 * 
	 * @param className the name of the class containing the method not to be
	 *        interpreted.
	 * @param parametersSignature the types of the method parameters and of
	 *        the return value. They all must be primitive, with the exception
	 *        of the first ("this") parameter if the method is not static.
	 * @param methodName the name of the method.
	 * @param functionName a {@link String}, the name that will be given to 
	 *        the uninterpreted function.
	 * @throws NullPointerException if any of the above parameters is {@code null}.
	 */
	public void addUninterpreted(String className, String parametersSignature, String methodName, String functionName) {
		this.runnerParameters.addUninterpreted(className, parametersSignature, methodName, functionName);
	}

	/**
	 * Sets a timeout for execution.
	 * 
	 * @param time a {@code long}, the amount of time.
	 * @param timeUnit the {@link TimeUnit} of {@code long}.
	 */
	public void setTimeout(long time, TimeUnit timeUnit) { 
		this.runnerParameters.setTimeout(time, timeUnit);
	}

	/**
	 * Sets no time limit for execution. This is the 
	 * default behavior.
	 */
	public void setTimeoutUnlimited() { 
		this.runnerParameters.setTimeoutUnlimited();
	}
	
	/**
	 * Sets a limited heap scope for the objects of a given class. 
	 * The heap scope is the maximum number of objects of a given class 
	 * in the initial state's heap. If during the symbolic execution 
	 * the number of assumed objects of a given class is above the associated 
	 * heap scope, the exploration of the branch is interrupted.
	 * 
	 * @param className a {@link String}, the name of a class.
	 * @param heapScope an {@code int}, the heap scope associated to {@link className}.
	 */
	public void setHeapScope(String className, int heapScope) { 
		this.runnerParameters.setHeapScope(className, heapScope); 
	}

	/**
	 * Sets an unlimited heap scope for the objects of a given class. 
	 * The heap scope is the maximum number of objects of a given class 
	 * in the initial state's heap. If during the symbolic execution 
	 * the number of assumed objects of a given class is above the associated 
	 * heap scope, the exploration of the branch is interrupted.
	 */
	public void setHeapScopeUnlimited(String className) { 
		this.runnerParameters.setHeapScopeUnlimited(className); 
	}

	/**
	 * Sets an unlimited heap scope for all the classes; this is the default 
	 * behaviour. 
	 * 
	 * @see {@link #setHeapScopeUnlimited(String)}
	 */
	public void setHeapScopeUnlimited() { 
		this.runnerParameters.setHeapScopeUnlimited(); 
	}
	
	/**
	 * Gets the heap scope for the objects of a given class. 
	 * 
	 * @return heapScope a {@link Map}{@code <}{@link String}{@code , }{@link Integer}{@code >}, 
	 *        associating class names with their respective heap scopes.
	 *        If a class is not present in the map, its scope is unlimited.
	 */
	public Map<String, Integer> getHeapScope() {
		return this.runnerParameters.getHeapScope();
	}
	
	/**
	 * Sets a limited depth scope. 
	 * The depth of a state is the number of branches above it. If 
	 * a state has a depth greater than the depth scope the exploration 
	 * of the branch it belongs is interrupted.
	 * 
	 * @param depthScope an {@code int}, the depth scope.
	 */
	public void setDepthScope(int depthScope) { 
		this.runnerParameters.setDepthScope(depthScope); 
	}

	/**
	 * Sets an unlimited depth scope; this is the default behaviour.
	 * 
	 * @see {@link #setDepthScope(int)}
	 */
	public void setDepthScopeUnlimited() { 
		this.runnerParameters.setDepthScopeUnlimited(); 
	}
	
	/**
	 * Sets a limited count scope. 
	 * If a state has a number of predecessor states greater than the 
	 * count scope the exploration of the branch it belongs is interrupted.
	 * 
	 * @param countScope an {@code int}, the count scope.
	 */
	public void setCountScope(int countScope) { 
		this.runnerParameters.setCountScope(countScope); 
	}
	
	/**
	 * Sets an unlimited count scope; this is the default behaviour.
	 */
	public void setCountScopeUnlimited() { 
		this.runnerParameters.setCountScopeUnlimited(); 
	}
	
	/**
	 * Sets the identifier of the initial state in the state space subregion 
	 * to be explored.
	 * 
	 * @param identifierSubregion a {@link String}, the subregion identifier.
	 *        For example, if {@code identifierSubregion == ".1.2.1"} the 
	 *        execution will explore only the traces whose identifier starts
	 *        with .1.2.1 (i.e., 1.2.1.1.2, 1.2.1.3.2.1.4, and not 1.2.2.1.2).
	 * @throws NullPointerException if {@code identifierSubregion == null}.
	 */
	public void setIdentifierSubregion(String identifierSubregion) {
		this.runnerParameters.setIdentifierSubregion(identifierSubregion);
	}
	
	/**
	 * Instructs to explore the whole state space starting
	 * from the root state. This is the default behavior.
	 */
	public void setIdentifierSubregionRoot() {
		this.runnerParameters.setIdentifierSubregionRoot();
	}	
	
	/**
	 * Sets the classes of the rewriters to be applied to
	 * the terms created during symbolic execution.
	 * 
	 * @param rewriterClasses a vararg of {@link Class}{@code <? extends }
	 *        {@link Rewriter}{@code >}s.
	 *        They all must be either {@code null} (no rewriter)
	 *        or the class name of a class extending 
	 *        {@code Rewriter}, with a default constructor, and
	 *        in the classpath of the symbolic executor.
	 */
	@SafeVarargs
	public final void addRewriter(Class<? extends Rewriter>... rewriterClasses) {
		Collections.addAll(this.rewriterClasses, rewriterClasses);
	}
	
	/**
	 * Clears the classes of the rewriters to be applied to
     * the terms created during symbolic execution.
	 */
	public void clearRewriters() {
	    this.rewriterClasses.clear();
	}
	
	/**
	 * Returns the classes of the rewriters to be applied to
     * the terms created during symbolic execution.
     * 
	 * @return a {@link List}{@code <}{@link Class}{@code <? extends }
	 * {@link Rewriter}{@code >>}. It may contain {@code null}.
	 */
    public List<Class<? extends Rewriter>> getRewriters() {
	    return new ArrayList<>(this.rewriterClasses);
	}

	/**
	 * Sets the decision procedure. Overrides any previous call to
	 * {@link #setDecisionProcedureGuidance}.
	 * 
	 * @param decisionProcedureType A {@link DecisionProcedureType} 
	 * representing the decision procedure.
	 * @throws NullPointerException if {@code decisionProcedureType == null}.
	 */
	public void setDecisionProcedureType(DecisionProcedureType decisionProcedureType) { 
		if (decisionProcedureType == null) {
			throw new NullPointerException();
		}
		this.decisionProcedureType = decisionProcedureType; 
	}
	
	/**
	 * Gets the decision procedure type.
	 * 
	 * @return a {@link DecisionProcedureType}.
	 */
	public DecisionProcedureType getDecisionProcedureType() {
		return this.decisionProcedureType;
	}

	/**
	 * Sets the pathname of the executable
	 * of the decision procedure (should match 
	 * {@link #setDecisionProcedureType(DecisionProcedureType)}).
	 * 
	 * @param externalDecisionProcedurePath a {@link String} containing a valid 
	 *        pathname for the decision procedure executable.
	 * @throws NullPointerException if {@code externalDecisionProcedurePath == null}.
	 * @throws InvalidPathException if {@code externalDecisionProcedurePath} is not
	 *         a valid path file name.
	 */
	public void setExternalDecisionProcedurePath(String externalDecisionProcedurePath) { 
		if (externalDecisionProcedurePath == null) {
			throw new NullPointerException();
		}
		this.externalDecisionProcedurePath = Paths.get(externalDecisionProcedurePath); 
	}
	
	/**
	 * Gets the pathname of the executable
	 * of the decision procedure set with 
	 * {@link #setExternalDecisionProcedurePath(String)}.
	 * 
	 * @return a nonnull {@link String}.
	 */
	public Path getExternalDecisionProcedurePath() {
		return this.externalDecisionProcedurePath;
	}
    
    /**
     * Adds a creation strategy to the strategies 
     * for creating the {@link DecisionProcedure}.
     * 
     * @param creationStrategy a {@link DecisionProcedureCreationStrategy}.
     * @throws NullPointerException if {@code creationStrategy == null}.
     */
    public void addDecisionProcedureCreationStrategy(DecisionProcedureCreationStrategy creationStrategy) {
        if (creationStrategy == null) {
            throw new NullPointerException();
        }
        this.creationStrategies.add(creationStrategy);
    }
    
    /**
     * Sets the creation strategy for the {@link DecisionProcedure} 
     * to plain decoration with
     * {@link DecisionProcedureAlgorithms}. This is the default.
     */
    public void clearDecisionProcedureCreationStrategies() {
        this.creationStrategies.clear();
    }
    
    /**
     * Returns all the strategies for creating the {@link DecisionProcedure},
     * in their order of addition.
     * 
     * @return a {@link List}{@code <}{@link DecisionProcedureCreationStrategy}{@code >}.
     */
    public List<DecisionProcedureCreationStrategy> getDecisionProcedureCreationStrategies() {
        return new ArrayList<>(this.creationStrategies);
    }
	
	/**
	 * Sets whether the engine should perform sign analysis
	 * for deciding inequations before invoking the decision procedure
	 * set with {@link #setDecisionProcedureType(DecisionProcedureType)}.
	 * 
	 * @param doSignAnalysis {@code true} iff the engine must do sign analysis.
	 */
	public void setDoSignAnalysis(boolean doSignAnalysis) {
		this.doSignAnalysis = doSignAnalysis;
	}
	
	/**
	 * Gets whether the engine should perform sign analysis
     * for deciding inequations.
	 * 
	 * @return {@code true} iff the engine must do sign analysis.
	 */
	public boolean getDoSignAnalysis() {
	    return this.doSignAnalysis;
	}
	
	/**
	 * Sets whether the engine should decide equality with a
	 * simple closure algorithm. 
	 * 
	 * @param doEqualityAnalysis {@code true} iff the engine must decide equalities.
	 */
	public void setDoEqualityAnalysis(boolean doEqualityAnalysis) {
		this.doEqualityAnalysis = doEqualityAnalysis;
	}

	/**
	 * Gets whether the engine should decide equality.
	 * 
	 * @return {@code true} iff the engine must decide equalities.
	 */
    public boolean getDoEqualityAnalysis() {
        return this.doEqualityAnalysis;
    }
    
	/**
	 * Sets whether the engine shall invoke or not the conservative
	 * repOk methods at every heap expansion. By default they are
	 * not invoked.
	 * 
	 * @param useConservativeRepOks {@code true} iff conservative
	 * repOk methods are invoked.
	 */
	public void setUseConservativeRepOks(boolean useConservativeRepOks) {
		this.useConservativeRepOks = useConservativeRepOks;
	}
	
	/**
	 * Returns whether the engine shall invoke or not the conservative
     * repOk methods at every heap expansion.
	 * 
	 * @return {@code true} iff conservative repOk methods are invoked.
	 */
	public boolean getUseConservativeRepOks() {
	    return this.useConservativeRepOks;
	}

	/**
	 * Specifies the conservative repOK method of a class.
	 * 
     * @param className the name of a class.
     * @param methodName the name of the conservative repOK method 
     *        contained in the class. It must be a parameterless
     *        nonnative instance method returning a boolean and it 
     *        must be defined in the class (i.e., it may not be
     *        inherited).
	 */
	public void addConservativeRepOk(String className, String methodName) {
	    this.conservativeRepOks.put(className, methodName);
	}
	
	/**
	 * Clears the conservative repOK methods of classes.
	 */
	public void clearConservativeRepOks() {
	    this.conservativeRepOks.clear();
	}
	
	/**
	 * Gets the conservative repOK methods of classes.
	 * 
	 * @return a {@link Map}{@code <}{@link String}{@code , }{@link String}{@code >}
	 *         mapping class names with the name of their respective conservative
	 *         repOK methods.
	 */
	public Map<String, String> getConservativeRepOks() {
	    return new HashMap<>(this.conservativeRepOks);
	}
	
	//TODO static (noncomputed) concretization heap scope

	/**
	 * Sets a limited heap scope for the objects of a given class
	 * during the symbolic execution of the concretization methods. 
	 * The heap scope is the maximum number of objects of a given class 
	 * in the initial state's heap. If during the symbolic execution 
	 * the number of assumed objects of a given class is above the associated 
	 * heap scope, the exploration of the branch is interrupted.
	 * 
	 * @param className a {@link String}, the name of a class.
	 * @param heapScopeCalculator a {@link Function}{@code <}{@link State}{@code , }{@link Integer}{@code >}, 
	 *        that calculates the heap scope associated to {@link className} from the initial
	 *        state.
	 */
	public void setConcretizationHeapScope(String className, Function<State, Integer> heapScopeCalculator) { 
		this.concretizationHeapScope.put(className, heapScopeCalculator); 
	}

	/**
	 * Sets an unlimited heap scope for the objects of a given class
	 * during the symbolic execution of the concretization methods. 
	 * The heap scope is the maximum number of objects of a given class 
	 * in the initial state's heap. If during the symbolic execution 
	 * the number of assumed objects of a given class is above the associated 
	 * heap scope, the exploration of the branch is interrupted.
	 */
	public void setConcretizationHeapScopeUnlimited(String className) { 
		this.concretizationHeapScope.remove(className); 
	}

	/**
	 * Sets an unlimited heap scope for all the classes during the 
	 * symbolic execution of the concretization methods; this is 
	 * the default behaviour. 
	 * The heap scope is the maximum number of objects of a given class 
	 * in the initial state's heap. If during the symbolic execution 
	 * the number of assumed objects of a given class is above the associated 
	 * heap scope, the exploration of the branch is interrupted.
	 */
	public void setConcretizationHeapScopeUnlimited() { 
		this.concretizationHeapScope.clear(); 
	}

	/**
	 * Sets a limited depth scope for the symbolic execution 
	 * of the concretization methods. 
	 * The depth of a state is the number of branches above it. If 
	 * a state has a depth greater than the depth scope the exploration 
	 * of the branch it belongs is interrupted.
	 * 
	 * @param depthScope an {@code int}, the depth scope.
	 */
	public void setConcretizationDepthScope(int depthScope) { 
		this.concretizationDepthScope = depthScope; 
	}

	/**
	 * Sets an unlimited depth scope for the symbolic execution 
	 * of the concretization methods; this is the default behaviour. 
	 * The depth of a state is the number of branches above it. If 
	 * a state has a depth greater than the depth scope the exploration 
	 * of the branch it belongs is interrupted.
	 */
	public void setConcretizationDepthScopeUnlimited() { 
		this.concretizationDepthScope = 0; 
	}

	/**
	 * Sets a limited count scope for the symbolic execution 
	 * of the concretization methods. 
	 * If a state has a number of predecessor states greater than the 
	 * count scope the exploration of the branch it belongs is interrupted.
	 * 
	 * @param countScope an {@code int}, the count scope.
	 */
	public void setConcretizationCountScope(int countScope) { 
		this.concretizationCountScope = countScope; 
	}

	/**
	 * Sets an unlimited count scope for the symbolic execution 
	 * of the concretization methods; this is the default behaviour.
	 * If a state has a number of predecessor states greater than the 
	 * count scope the exploration of the branch it belongs is interrupted.
	 */
	public void setConcretizationCountScopeUnlimited() { 
		this.concretizationCountScope = 0; 
	}
    
    /**
     * Sets whether the engine should use LICS rules
     * to decide on references resolution. By default
     * LICS rules are used.
     * 
     * @param useLICS {@code true} iff the engine must 
     * use LICS rules.
     */
    public void setUseLICS(boolean useLICS) {
        this.useLICS = useLICS;
    }
    
    /**
     * Gets whether the engine should use LICS rules
     * to decide on references resolution.
     * 
     * @return {@code true} iff the engine must 
     * use LICS rules.
     */
    public boolean getUseLICS() {
        return this.useLICS;
    }
    
    /**
     * Returns the {@link LICSRulesRepo} 
     * containing all the LICS rules that
     * must be used.
     * 
     * @return a {@link LICSRulesRepo}. It
     *         is the one that backs this
     *         {@link RunParameters}, not a
     *         safety copy.
     */
    public LICSRulesRepo getLICSRulesRepo() {
        return this.repoLICS;
    }

    /**
     * Returns the {@link ClassInitRulesRepo} 
     * containing all the class initialization 
     * rules (list of classes that are assumed
     * not to be initialized) that must be used.
     * 
     * @return a {@link ClassInitRulesRepo}. It
     *         is the one that backs this
     *         {@link EngineParameters}, not a
     *         safety copy.
     */
    public ClassInitRulesRepo getClassInitRulesRepo() {
        return this.repoInit;
    }
    
    /**
     * Specifies a LICS rule for symbolic reference expansion. By default a 
     * symbolic reference is expanded to a fresh symbolic object with class
     * of its static type, or is not expanded if the static type of the reference
     * is an abstract class or an interface.
     * This method allows to override this default.
     * 
     * @param toExpand     the static type of the reference to be expanded. 
     *                     It must be {@code toExpand != null}.
     * @param originExp    a path expression describing the origin of the 
     *                     symbolic references that match this rule.
     *                     If {@code originExp == null}, all the symbolic 
     *                     references with static type {@code toExpand} 
     *                     will match. 
     * @param classAllowed the name of the class whose instances are possible 
     *                     expansions for {@code toExpand}. During  
     *                     symbolic execution, every symbolic reference with 
     *                     static type {@code toExpand} and origin matching 
     *                     {@code originExp}, will be expanded 
     *                     when necessary to a symbolic object with class 
     *                     {@code classAllowed}. If {@code classAllowed == null}, 
     *                     the matching {@link ReferenceSymbolic}s will not be expanded.
     */
	public void addExpandToLICS(String toExpand, String originExp, String classAllowed) {
		this.repoLICS.addExpandTo(toExpand, originExp, classAllowed);
	}

    /**
     * Specifies a LICS rule for symbolic reference resolution by alias. 
     * By default, symbolic references are resolved by aliases to all the 
     * type-compatible objects assumed by previous epoch-compatible expansions. 
     * This method allows to override this default.
     * 
     * @param toResolve      the static type of the reference to be resolved. 
     *                       It must be {@code toResolve != null}.
     * @param originExp      a path expression describing the origin of the 
     *                       symbolic references that match this rule.
     *                       The path expression is a slash-separated list of field
     *                       names that starts from {ROOT}:x, indicating the 
     *                       parameter with name {@code x} of the root method 
     *                       invocation (including {@code this}).
     *                       If {@code originExp == null}, all the symbolic 
     *                       references with static type {@code toResolve} 
     *                       will match. 
     * @param pathAllowedExp a path expression describing the objects that are 
     *                       acceptable as alias for {@code toResolve}. 
     *                       The path expression is a slash-separated list of field
     *                       names that starts from {ROOT}:x, indicating the 
     *                       parameter with name {@code x} of the root method 
     *                       invocation (including {@code this}), or from 
     *                       {REF}, indicating a path starting from the origin 
     *                       of the reference matched by the left part of the rule. 
     *                       You can also use the special {UP} to move back in the 
     *                       path; for instance, if the reference matching 
     *                       {@code originExp} has origin 
     *                       {ROOT}:this/list/head/next/next, then you can use both 
     *                       {REF}/{UP}/{UP}/{UP} and {ROOT}:this/list to denote 
     *                       the field with name {@code list} of the object that is
     *                       referred by the {@code this} parameter of the root method
     *                       invocation. Start the expression with {MAX} to indicate
     *                       a max-rule.
     *                       During symbolic execution, every symbolic reference 
     *                       with class {@code toResolve} and origin matching 
     *                       {@code originExp} will be resolved when necessary 
     *                       to all the type- and epoch-compatible 
     *                       symbolic objects whose origins match
     *                       {@code pathAllowedExp}. If {@code pathAllowedExp == null}
     *                       the matching {@link ReferenceSymbolic} will not be
     *                       resolved by alias.
     */
	public void addResolveAliasOriginLICS(String toResolve, String originExp, String pathAllowedExp) {
	    this.repoLICS.addResolveAliasOrigin(toResolve, originExp, pathAllowedExp);
	}

    /**
     * Specifies a LICS rule for symbolic reference resolution by alias. 
     * By default, symbolic references are resolved by aliases to all the 
     * type-compatible objects assumed by previous epoch-compatible expansions. 
     * This method allows to override this default.
     * 
     * @param toResolve    the static type of the reference to be resolved. 
     *                     It must be {@code toResolve != null}.
     * @param originExp    a path expression describing the origin of the 
     *                     symbolic references that match this rule.
     *                     The path expression is a slash-separated list of field
     *                     names that starts from {ROOT}:x, indicating the 
     *                     parameter with name {@code x} of the root method 
     *                     invocation (including {@code this}).
     *                     If {@code originExp == null}, all the symbolic 
     *                     references with static type {@code toResolve} 
     *                     will match. 
     * @param classAllowed the name of the class whose instances are possible 
     *                     aliases for {@code toResolve}. During  
     *                     symbolic execution, every symbolic reference with 
     *                     static type {@code toResolve} and origin matching 
     *                     {@code originExp}, will be resolved 
     *                     when necessary to all the epoch-compatible symbolic 
     *                     objects with class {@code classAllowed}. If 
     *                     {@code classAllowed == null} the matching 
     *                     {@link ReferenceSymbolic} will not be resolved by alias.
     */
	public void addResolveAliasInstanceofLICS(String toResolve, String originExp, String classAllowed) {
	    this.repoLICS.addResolveAliasInstanceof(toResolve, originExp, classAllowed);
	}

    /**
     * Specifies a LICS rule for symbolic reference resolution by alias. 
     * By default, symbolic references are resolved by aliases to all the 
     * type-compatible objects assumed by previous epoch-compatible expansions. 
     * This method allows to override this default.
     * 
     * @param toResolve      the static type of the reference to be resolved. 
     *                       It must be {@code toResolve != null}.
     * @param originExp      a path expression describing the origin of the 
     *                       symbolic references that match this rule.
     *                       The path expression is a slash-separated list of field
     *                       names that starts from {ROOT}:x, indicating the 
     *                       parameter with name {@code x} of the root method 
     *                       invocation (including {@code this}).
     *                       If {@code originExp == null}, all the symbolic 
     *                       references with static type {@code toResolve} 
     *                       will match. 
     * @param pathDisallowedExp a path expression describing the objects that are not
     *                          acceptable as alias for {@code toResolve}. 
     *                          The path expression is a slash-separated list of field
     *                          names that starts from {ROOT}:x, indicating the 
     *                          parameter with name {@code x} of the root method 
     *                          invocation (including {@code this}), or from 
     *                          {REF}, indicating a path starting from the origin 
     *                          of the reference matched by the left part of the rule. 
     *                          You can also use the special {UP} to move back in the 
     *                          path; for instance, if the reference matching 
     *                          {@code originExp} has origin 
     *                          {ROOT}:this/list/head/next/next, then you can use both 
     *                          {REF}/{UP}/{UP}/{UP} and {ROOT}:this/list to denote 
     *                          the field with name {@code list} of the object that is
     *                          referred by the {@code this} parameter of the root method
     *                          invocation.
     *                          During symbolic execution, every symbolic reference 
     *                          with class {@code toResolve} and origin matching 
     *                          {@code originExp} will not be resolved when necessary 
     *                          to a type- and epoch-compatible symbolic object 
     *                          if its origin matches {@code pathDisallowedExp}.
     */
    public void addResolveAliasNeverLICS(String toResolve, String originExp, String pathDisallowedExp) {
        this.repoLICS.addResolveAliasNever(toResolve, originExp, pathDisallowedExp);
    }

    /**
     * Specifies a LICS rule for symbolic reference resolution by null. By 
     * default all symbolic references are resolved by null. This method
     * allows to override this default.
     * 
     * @param toResolve the static type of the reference to be resolved. 
     *                  It must be {@code toResolve != null}.
     * @param originExp a path expression describing the origin of the 
     *                  symbolic references which match this rule.
     *                  The path expression is a slash-separated list of field
     *                  names that starts from {ROOT}:x, indicating the 
     *                  parameter with name {@code x} of the root method 
     *                  invocation (including {@code this}).
     *                  If {@code originExp == null}, all the symbolic 
     *                  references with static type {@code toResolve} 
     *                  will match.
     */ 
	public void addResolveNotNullLICS(String toResolve, String originExp) {
	    this.repoLICS.addResolveNotNull(toResolve, originExp);
	}

    /**
     * Adds class names to the set of not initialized classes.
     * 
     * @param notInitializedClasses a list of class names as a {@link String} varargs.
     */
    public void addNotInitializedClasses(String... notInitializedClasses) {
        this.repoInit.addNotInitializedClass(notInitializedClasses);
    }
    
    /**
     * Adds a trigger method that fires when some references are resolved by
     * expansion.
     * 
     * @param toExpand     the static type of the reference to be expanded. 
     *                     It must be {@code toExpand != null}.
     * @param originExp    a path expression describing the origin of the 
     *                     symbolic references that match this rule.
     *                     If {@code originExp == null}, all the symbolic 
     *                     references with static type {@code toExpand} 
     *                     will match. 
     * @param classAllowed the name of the class whose instances are possible 
     *                     expansions for {@code toExpand}. During  
     *                     symbolic execution, every symbolic reference with 
     *                     static type {@code toExpand} and origin matching 
     *                     {@code originExp}, will be expanded 
     *                     when necessary to a symbolic object with class 
     *                     {@code classAllowed}. If {@code classAllowed == null}, 
     *                     the matching {@link ReferenceSymbolic}s will not be expanded.
     * @param triggerClassName 
     *                     the class of the instrumentation method to be triggered 
     *                     when this rule fires.
     * @param triggerParametersSignature 
     *                     the types of the parameters of the instrumentation method 
     *                     to be triggered when this rule fires.
     * @param triggerMethodName 
     *                     the name of the instrumentation method to be triggered 
     *                     when this rule fires.
     * @param triggerParameter
     *                     the parameter to be passed to the trigger when the rule fires. 
     */
    public void addExpandToTrigger(String toExpand, String originExp, String classAllowed, 
            String triggerClassName, String triggerParametersSignature, String triggerMethodName,
            String triggerParameter) {
        this.runnerParameters.addExpandToTrigger(toExpand, originExp, classAllowed, 
                triggerClassName, triggerParametersSignature, triggerMethodName, triggerParameter);
    }
    
    /**
     * Adds a trigger method that fires when some references are resolved by
     * alias.
     * 
     * @param toResolve      the static type of the reference to be resolved. 
     *                       It must be {@code toResolve != null}.
     * @param originExp      a path expression describing the origin of the 
     *                       symbolic references that match this rule.
     *                       The path expression is a slash-separated list of field
     *                       names that starts from {ROOT}:x, indicating the 
     *                       parameter with name {@code x} of the root method 
     *                       invocation (including {@code this}).
     *                       If {@code originExp == null}, all the symbolic 
     *                       references with static type {@code toResolve} 
     *                       will match. 
     * @param pathAllowedExp a path expression describing the objects that are 
     *                       acceptable as alias for {@code toResolve}. 
     *                       The path expression is a slash-separated list of field
     *                       names that starts from {ROOT}:x, indicating the 
     *                       parameter with name {@code x} of the root method 
     *                       invocation (including {@code this}), or from 
     *                       {REF}, indicating a path starting from the origin 
     *                       of the reference matched by the left part of the rule. 
     *                       You can also use the special {UP} to move back in the 
     *                       path; for instance, if the reference matching 
     *                       {@code originExp} has origin 
     *                       {ROOT}:this/list/head/next/next, then you can use both 
     *                       {REF}/{UP}/{UP}/{UP} and {ROOT}:this/list to denote 
     *                       the field with name {@code list} of the object that is
     *                       referred by the {@code this} parameter of the root method
     *                       invocation.
     *                       During symbolic execution, every symbolic reference 
     *                       with class {@code toResolve} and origin matching 
     *                       {@code originExp} will be resolved when necessary 
     *                       to all the type- and epoch-compatible 
     *                       symbolic objects whose origins match
     *                       {@code pathAllowedExp}. If {@code pathAllowedExp == null}
     *                       the matching {@link ReferenceSymbolic} will not be
     *                       resolved by alias.
     * @param triggerClassName 
     *                       the class of the instrumentation method to be triggered 
     *                       when this rule fires.
     * @param triggerParametersSignature 
     *                       the types of the parameters of the instrumentation method 
     *                       to be triggered when this rule fires.
     * @param triggerMethodName 
     *                       the name of the instrumentation method to be triggered 
     *                       when this rule fires.
     * @param triggerParameter
     *                       the parameter to be passed to the trigger when the rule fires. 
     */
    public void addResolveAliasOriginTrigger(String toResolve, String originExp, String pathAllowedExp, 
            String triggerClassName, String triggerParametersSignature, String triggerMethodName,
            String triggerParameter) {
        this.runnerParameters.addResolveAliasOriginTrigger(toResolve, originExp, pathAllowedExp, 
                triggerClassName, triggerParametersSignature, triggerMethodName, triggerParameter);
    }

    /**
     * Adds a trigger method that fires when some references are resolved by
     * alias.
     * 
     * @param toResolve    the static type of the reference to be resolved. 
     *                     It must be {@code toResolve != null}.
     * @param originExp    a path expression describing the origin of the 
     *                     symbolic references that match this rule.
     *                     The path expression is a slash-separated list of field
     *                     names that starts from {ROOT}:x, indicating the 
     *                     parameter with name {@code x} of the root method 
     *                     invocation (including {@code this}).
     *                     If {@code originExp == null}, all the symbolic 
     *                     references with static type {@code toResolve} 
     *                     will match. 
     * @param classAllowed the name of the class whose instances are possible 
     *                     aliases for {@code toResolve}. During  
     *                     symbolic execution, every symbolic reference with 
     *                     static type {@code toResolve} and origin matching 
     *                     {@code originExp}, will be resolved 
     *                     when necessary to all the epoch-compatible symbolic 
     *                     objects with class {@code classAllowed}. If 
     *                     {@code classAllowed == null} the matching 
     *                     {@link ReferenceSymbolic} will not be resolved by alias.
     * @param triggerClassName 
     *                     the class of the instrumentation method to be triggered 
     *                     when this rule fires.
     * @param triggerParametersSignature 
     *                     the types of the parameters of the instrumentation method 
     *                     to be triggered when this rule fires.
     * @param triggerMethodName 
     *                     the name of the instrumentation method to be triggered 
     *                     when this rule fires.
     * @param triggerParameter
     *                     the parameter to be passed to the trigger when the rule fires. 
     */
    public void addResolveAliasInstanceofTrigger(String toResolve, String originExp, String classAllowed, 
            String triggerClassName, String triggerParametersSignature, String triggerMethodName,
            String triggerParameter) {
        this.runnerParameters.addResolveAliasInstanceofTrigger(toResolve, originExp, classAllowed, 
                triggerClassName, triggerParametersSignature, triggerMethodName, triggerParameter);
    }

    /**
     * Adds a trigger method that fires when some references are resolved by
     * null.
     * 
     * @param toResolve the static type of the reference to be resolved. 
     *                  It must be {@code toResolve != null}.
     * @param originExp a path expression describing the origin of the 
     *                  symbolic references that match this rule.
     *                  The path expression is a slash-separated list of field
     *                  names that starts from {ROOT}:x, indicating the 
     *                  parameter with name {@code x} of the root method 
     *                  invocation (including {@code this}).
     *                  If {@code originExp == null}, all the symbolic 
     *                  references with static type {@code toResolve} 
     *                  will match.
     * @param triggerClassName 
     *                  the class of the trigger method.
     * @param triggerParametersSignature 
     *                  the types of the parameters of the trigger method.
     * @param triggerMethodName 
     *                  the name of the trigger method.
     * @param triggerParameter
     *                  the parameter to be passed to the trigger method. 
     */ 
	public void addResolveNullTrigger(String toResolve, String originExp, 
			String triggerClassName, String triggerParametersSignature, String triggerMethodName,
			String triggerParameter) {
		this.runnerParameters.addResolveNullTrigger(toResolve, originExp, triggerClassName, 
				triggerParametersSignature, triggerMethodName, triggerParameter);
	}
	
	/**
	 * Sets the method number.
	 * 
	 * @param methodNumber a {@code long} identifying the method that is executed.
	 */
	public void setMethodNumber(long methodNumber) {
		this.methodNumber = methodNumber;
	}
	
	/**
	 * Returns the method number.
	 * 
	 * @return the identifying number of the method that is executed.
	 */
	public long getMethodNumber() {
		return this.methodNumber;
	}
	
	/**
	 * Sets the start value of the trace counter.
	 * 
	 * @param traceCounterStart a {@code long}.
	 * @throws IllegalArgumentException if {@code traceCounterStart < 0}.
	 */
	public void setTraceCounterStart(long traceCounterStart) {
		if (traceCounterStart < 0) {
			throw new IllegalArgumentException();
		}
		this.traceCounterStart = traceCounterStart;
	}
	
	/**
	 * Gets the start value of the trace counter.
	 * 
	 * @return a {@code long}.
	 */
	public long getTraceCounterStart() {
		return this.traceCounterStart;
	}
	
	/**
	 * Sets whether the coverage, branches and traces log files
	 * must be emitted.
	 * 
	 * @param mustLogCoverageData a {@code boolean}.
	 */
	public void setMustLogCoverageData(boolean mustLogCoverageData) {
		this.mustLogCoverageData = mustLogCoverageData;
	}
	
	/**
	 * Gets whether the coverage, branches and traces log files
	 * must be emitted.
	 * 
	 * @return a {@code boolean}.
	 */
	public boolean getMustLogCoverageData() {
		return this.mustLogCoverageData;
	}
	
	/**
	 * Sets the path of the wrapper file.
	 * 
	 * @param wrapperFilePathBuilder A {@link BiFunction} accepting as parameters two {@link Long}s, 
	 *        a method number and a local trace number, and returning a {@link Path}
	 *        for the wrapper file for that trace. 
	 * @throws NullPointerException if {@code wrapperFilePathBuilder == null}.
	 */
	public void setWrapperFilePathBuilder(BiFunction<Long, Long, Path> wrapperFilePathBuilder) {
		if (wrapperFilePathBuilder == null) {
			throw new NullPointerException();
		}
		this.wrapperFilePathBuilder = wrapperFilePathBuilder;
	}
	
	/**
	 * Returns the path of the wrapper file.
	 * 
	 * @param localTraceNumber a {@code long}, local trace number.
	 * @return a {@link Path}.
	 */
	public Path getWrapperFilePath(long localTraceNumber) {
		return this.wrapperFilePathBuilder.apply(this.methodNumber, localTraceNumber);
	}
		
	/**
	 * Sets the path of the coverage file.
	 * 
	 * @param coverageFilePathBuilder A {@link Function} accepting as parameter a {@link Long}s, 
	 *        a method number, and returning a {@link Path} for the coverage file for that method. 
	 * @throws NullPointerException if {@code coverageFilePathBuilder == null}.
	 */
	public void setCoverageFilePathBuilder(Function<Long, Path> coverageFilePathBuilder) {
		if (coverageFilePathBuilder == null) {
			throw new NullPointerException();
		}
		this.coverageFilePathBuilder = coverageFilePathBuilder;
	}
	
	/**
	 * Returns the path of the coverage file.
	 * 
	 * @return a {@link Path}.
	 */
	public Path getCoverageFilePath() {
		return this.coverageFilePathBuilder.apply(this.methodNumber);
	}
	
	/**
	 * Sets the path of the branches file.
	 * 
	 * @param branchesFilePathBuilder A {@link Function} accepting as parameter a {@link Long}s, 
	 *        a method number, and returning a {@link Path} for the branches file for that method. 
	 * @throws NullPointerException if {@code branchesFilePathBuilder == null}.
	 */
	public void setBranchesFilePathBuilder(Function<Long, Path> branchesFilePathBuilder) {
		if (branchesFilePathBuilder == null) {
			throw new NullPointerException();
		}
		this.branchesFilePathBuilder = branchesFilePathBuilder;
	}
	
	/**
	 * Returns the path of the branches file.
	 * 
	 * @return a {@link Path}.
	 */
	public Path getBranchesFilePath() {
		return this.branchesFilePathBuilder.apply(this.methodNumber);
	}
	
	/**
	 * Sets the path of the traces file.
	 * 
	 * @param tracesFilePathBuilder A {@link Function} accepting as parameter a {@link Long}s, 
	 *        a method number, and returning a {@link Path} for the traces file for that method. 
	 * @throws NullPointerException if {@code tracesFilePathBuilder == null}.
	 */
	public void setTracesFilePathBuilder(Function<Long, Path> tracesFilePathBuilder) {
		if (tracesFilePathBuilder == null) {
			throw new NullPointerException();
		}
		this.tracesFilePathBuilder = tracesFilePathBuilder;
	}
	
	/**
	 * Returns the path of the traces file.
	 * 
	 * @return a {@link Path}.
	 */
	public Path getTracesFilePath() {
		return this.tracesFilePathBuilder.apply(this.methodNumber);
	}
	
	/**
	 * Relevant only when {@link #setStepShowMode(StepShowMode)}
	 * is set to {@link StepShowMode#LEAVES} or 
	 * {@link StepShowMode#SUMMARIES} to further filter
	 * which leaves/summaries must be shown.
	 * 
	 * @param show {@code true} iff the leaves/summaries 
	 *        of safe traces must be shown.
	 */
	public void setShowSafe(boolean show) {
		if (show) {
			this.tracesToShow.add(TraceTypes.SAFE);
		} else {
			this.tracesToShow.remove(TraceTypes.SAFE);
		}
	}
	
	/**
	 * Relevant only when {@link #setStepShowMode(StepShowMode)}
	 * is set to {@link StepShowMode#LEAVES} or 
	 * {@link StepShowMode#SUMMARIES} to further filter
	 * which leaves/summaries must be shown.
	 * 
	 * @param show {@code true} iff the leaves/summaries 
	 *        of unsafe traces must be shown.
	 */
	public void setShowUnsafe(boolean show) {
		if (show) {
			this.tracesToShow.add(TraceTypes.UNSAFE);
		} else {
			this.tracesToShow.remove(TraceTypes.UNSAFE);
		}
	}
	
	/**
	 * Relevant only when {@link #setStepShowMode(StepShowMode)}
	 * is set to {@link StepShowMode#LEAVES} or 
	 * {@link StepShowMode#SUMMARIES} to further filter
	 * which leaves/summaries must be shown.
	 * 
	 * @param show {@code true} iff the leaves/summaries 
	 *        of contradictory traces must be shown.
	 */
	public void setShowContradictory(boolean show) {
		if (show) {
			this.tracesToShow.add(TraceTypes.CONTRADICTORY);
		} else {
			this.tracesToShow.remove(TraceTypes.CONTRADICTORY);
		}
	}
	
	/**
	 * Relevant only when {@link #setStepShowMode(StepShowMode)}
	 * is set to {@link StepShowMode#LEAVES} or 
	 * {@link StepShowMode#SUMMARIES} to further filter
	 * which leaves/summaries must be shown.
	 * 
	 * @param show {@code true} iff the leaves/summaries 
	 *        of out of scope traces must be shown.
	 */
	public void setShowOutOfScope(boolean show) {
		if (show) {
			this.tracesToShow.add(TraceTypes.OUT_OF_SCOPE);
		} else {
			this.tracesToShow.remove(TraceTypes.OUT_OF_SCOPE);
		}
	}
	
	/**
	 * Returns the traces types to be shown.
	 * 
	 * @return an {@link EnumSet}{@code <}{@link TraceTypes}{@code >}
	 *         containing the trace types to be shown.
	 */
	public EnumSet<TraceTypes> getTracesToShow() {
	    return this.tracesToShow.clone();
	}
	
	/**
	 * Sets the state output format mode. 
	 * 
	 * @param stateFormatMode A {@link StateFormatMode} 
	 *        representing the output format mode of the
	 *        states, or {@code null} if nothing must be
	 *        emitted.
	 */
	public void setStateFormatMode(StateFormatMode stateFormatMode) { 
		this.stateFormatMode = stateFormatMode; 
	}
	
	/**
	 * Gets the state output format mode.
	 * 
	 * @return A {@link StateFormatMode}.
	 */
	public StateFormatMode getStateFormatMode() {
	    return this.stateFormatMode;
	}

	/**
	 * Sets the symbolic execution to be guided by a concrete one starting
	 * from a driver method. The driver method <em>must</em> set 
	 * up all the necessary concrete inputs and then invoke the method set 
	 * by {@link #setMethodSignature}.
	 * 
	 * @param driverClass a {@link String}, the class name of the driver method. 
	 * @param driverParametersSignature a {@link String}, the parameters of the 
	 *        driver method (e.g., {@code "([Ljava/lang/String;)V"}. 
	 * @param driverName a {@link String}, the name of the driver method.
	 * @throws NullPointerException when any parameter is {@code null}.
	 */
	public void setGuided(String driverClass, String driverParametersSignature, String driverName) {
		if (driverClass == null || driverParametersSignature == null || driverName == null) {
			throw new NullPointerException();
		}
		this.guided = true;
		this.driverSignature = new Signature(driverClass, driverParametersSignature, driverName); 
	}
	
	/**
	 * Sets ordinary symbolic execution, not guided by a concrete one.
	 * This is the default behaviour.
	 */
	public void setUnguided() {
		this.guided = false;
		this.driverSignature = null;
	}
	
	/**
	 * Tests whether the symbolic execution is guided.
	 * 
	 * @return {@code true} iff the symbolic execution is guided.
	 */
	public boolean isGuided() {
		return this.guided;
	}

	/**
	 * Returns a new {@link RunnerParameters} that can be used
	 * to run a conservative repOk method.
	 * 
	 * @return a new instance of {@link RunnerParameters}.
	 */
	public RunnerParameters getConservativeRepOkDriverParameters(DecisionProcedureAlgorithms dec) {
		final RunnerParameters retVal = this.runnerParameters.clone();
		retVal.setDecisionProcedure(dec);
		retVal.setStateIdentificationMode(StateIdentificationMode.COMPACT);
		retVal.setBreadthMode(BreadthMode.MORE_THAN_ONE);
		/* TODO should be:
		 * retVal.setHeapScopeUnlimited();
		 * retVal.setDepthScopeUnlimited();
		 * retVal.setCountScopeUnlimited();
		 */
		retVal.setHeapScopeComputed(this.concretizationHeapScope);
		retVal.setDepthScope(this.concretizationDepthScope);
		retVal.setCountScope(this.concretizationCountScope);
		retVal.setIdentifierSubregionRoot();
		return retVal;
	}
	
	//TODO move these two methods, and do not use cloning but set all the parameters in a predictable way.

	/**
	 * Returns a new {@link RunnerParameters} that can be used
	 * to run a concretization method (sets only scopes).
	 * 
	 * @return a new instance of {@link RunnerParameters}.
	 */
	public RunnerParameters getConcretizationDriverParameters() {
		final RunnerParameters retVal = this.runnerParameters.clone();
		retVal.setStateIdentificationMode(StateIdentificationMode.COMPACT);
		retVal.setBreadthMode(BreadthMode.MORE_THAN_ONE);
		retVal.setHeapScopeComputed(this.concretizationHeapScope);
		retVal.setDepthScope(this.concretizationDepthScope);
		retVal.setCountScope(this.concretizationCountScope);
		retVal.setIdentifierSubregionRoot();
		return retVal;
	}

	/**
	 * Returns a new {@link RunnerParameters} that can be used
	 * to run the guidance driver method.
	 * 
	 * @param calc the {@link CalculatorRewriting} to be used by the decision procedure.
	 * @return a new instance of {@link RunnerParameters}, 
	 * or {@code null} iff {@link #isGuided()} {@code == false}.
	 */
	public RunnerParameters getGuidanceDriverParameters(CalculatorRewriting calc) {
		final RunnerParameters retVal;
		if (isGuided()) {
			retVal = this.runnerParameters.clone();
			retVal.setMethodSignature(this.driverSignature.getClassName(), this.driverSignature.getDescriptor(), this.driverSignature.getName());
			retVal.setCalculator(calc);
			retVal.setDecisionProcedure(new DecisionProcedureAlgorithms(new DecisionProcedureClassInit(new DecisionProcedureAlwSat(), calc, new ClassInitRulesRepo()), calc)); //for concrete execution
			retVal.setStateIdentificationMode(StateIdentificationMode.COMPACT);
			retVal.setBreadthMode(BreadthMode.MORE_THAN_ONE);
			retVal.setIdentifierSubregionRoot();
		} else {
			retVal = null;
		}
		return retVal;
	}
	
	@SuppressWarnings("unchecked")
	@Override 
	public JBSEParameters clone() {
		final JBSEParameters o;
		try {
			o = (JBSEParameters) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e); //will not happen
		}
		o.runnerParameters = this.runnerParameters.clone();
		o.rewriterClasses = (ArrayList<Class<? extends Rewriter>>) this.rewriterClasses.clone();
		o.repoLICS = this.repoLICS.clone();
        o.repoInit = this.repoInit.clone();
		o.conservativeRepOks = (HashMap<String, String>) this.conservativeRepOks.clone();
		o.concretizationHeapScope = (HashMap<String, Function<State, Integer>>) this.concretizationHeapScope.clone();
		o.creationStrategies = (ArrayList<DecisionProcedureCreationStrategy>) this.creationStrategies.clone();
		o.tracesToShow = this.tracesToShow.clone();
		return o;
	}
}
