package sushi.execution.jbse;

import static jbse.algo.Util.valueString;
import static jbse.bc.Signatures.JAVA_STRING;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import jbse.algo.exc.CannotManageStateException;
import jbse.apps.run.CannotBuildCalculatorException;
import jbse.apps.run.CannotBuildDecisionProcedureException;
import jbse.apps.run.CannotBuildFormatterException;
import jbse.apps.run.DecisionProcedureConservativeRepOk;
import jbse.apps.run.DecisionProcedureGuidance;
import jbse.apps.run.GuidanceException;
import jbse.bc.Opcodes;
import jbse.bc.exc.InvalidClassFileFactoryClassException;
import jbse.common.exc.ClasspathException;
import jbse.common.exc.UnexpectedInternalException;
import jbse.dec.DecisionProcedure;
import jbse.dec.DecisionProcedureAlgorithms;
import jbse.dec.DecisionProcedureAlwSat;
import jbse.dec.DecisionProcedureClassInit;
import jbse.dec.DecisionProcedureEquality;
import jbse.dec.DecisionProcedureLICS;
import jbse.dec.DecisionProcedureSMTLIB2_AUFNIRA;
import jbse.dec.DecisionProcedureSignAnalysis;
import jbse.dec.exc.DecisionException;
import jbse.jvm.Engine;
import jbse.jvm.EngineParameters;
import jbse.jvm.Runner;
import jbse.jvm.RunnerBuilder;
import jbse.jvm.RunnerParameters;
import jbse.jvm.exc.CannotBacktrackException;
import jbse.jvm.exc.CannotBuildEngineException;
import jbse.jvm.exc.EngineStuckException;
import jbse.jvm.exc.FailureException;
import jbse.jvm.exc.InitializationException;
import jbse.jvm.exc.NonexistingObservedVariablesException;
import jbse.mem.Frame;
import jbse.mem.Objekt;
import jbse.mem.State;
import jbse.mem.exc.ContradictionException;
import jbse.mem.exc.InvalidNumberOfOperandsException;
import jbse.mem.exc.ThreadStackEmptyException;
import jbse.rewr.CalculatorRewriting;
import jbse.rewr.Rewriter;
import jbse.rewr.RewriterOperationOnSimplex;
import jbse.tree.StateTree;
import jbse.tree.StateTree.BranchPoint;
import jbse.val.PrimitiveSymbolic;
import jbse.val.Reference;
import jbse.val.Simplex;
import jbse.val.Value;
import sushi.configure.JBSEParameters;
import sushi.configure.JBSEParameters.DecisionProcedureCreationStrategy;
import sushi.configure.JBSEParameters.DecisionProcedureType;
import sushi.configure.JBSEParameters.StateFormatMode;
import sushi.configure.JBSEParameters.TraceTypes;

public class RunJBSE_Sushi {
	/** The {@link JBSEParameters} of the symbolic execution. */
	private final JBSEParameters parameters;

	/** Set by {@link #build()}, the {@link Runner} used to run the method. */
	private Runner runner = null;
	
	/** Set by {@link #build()}, the {@link Engine} underlying {@code runner}. */
	private Engine engine = null;
    
    /** Set by {@link #build()}, the {@link DecisionProcedure} used by {@code engine}. */
    private DecisionProcedureAlgorithms decisionProcedure = null;
	
	/** Set by {@link #build()}, the {@link FormatterSushi} to output states. */
	private FormatterSushi formatter = null;

	/** 
	 * Set by {@link #build()}, the {@link DecisionProcedureConservativeRepOk}, whenever 
	 * this decision procedure is chosen. 
	 */
	private DecisionProcedureConservativeRepOk consRepOk = null;
	
	/** 
	 * Set by {@link #build()}, the {@link DecisionProcedureGuidance}, whenever this 
	 * method is chosen for stepping the {@link Engine}. 
	 */
	private DecisionProcedureGuidance guidance = null;
	
	/** Set by {@link #run()}, the error code. */
	private int errorCodeAfterRun = 0;
	
	/** Set by {@link #run()}, the final value of the trace counter. */
	private long traceCounter;
	
	/**
	 * Constructor.
	 * 
	 * @param parameters a {@link JBSEParameters} object. It must not be {@code null}.
	 */
	public RunJBSE_Sushi(JBSEParameters parameters) {
		this.parameters = parameters;
		this.traceCounter = parameters.getTraceCounterStart();
	}
	
	private class ActionsRun extends Runner.Actions {
		private TraceTypes traceKind;
		private long branchCounter = 0;
		private final HashMap<String, Long> branchNumberOf = new HashMap<>();
		private final ArrayList<String> branchTargets = new ArrayList<>();
		private final HashMap<String, TreeSet<Long>> coverage = new HashMap<>();
		private final HashMap<String, TreeSet<String>> stringLiterals = new HashMap<>();
		private final HashMap<BranchPoint, Boolean> atJumpBacktrack = new HashMap<>();
		private final HashMap<BranchPoint, Integer> jumpPCBacktrack = new HashMap<>();
		private TreeSet<Long> coverageCurrentTrace = new TreeSet<>();
		private TreeSet<String> stringLiteralsCurrentTrace = new TreeSet<>();
		private Frame stringLiteralFrame = null;
		private boolean atJump = false;
		private int jumpPC = 0;
		private boolean atLoadConstant = false;
		
		@Override
		public boolean atRoot() {
			if (RunJBSE_Sushi.this.parameters.getMustLogCoverageData()) {
				try {
					Files.deleteIfExists(RunJBSE_Sushi.this.parameters.getCoverageFilePath());
					Files.createFile(RunJBSE_Sushi.this.parameters.getCoverageFilePath());
					Files.deleteIfExists(RunJBSE_Sushi.this.parameters.getTracesFilePath());
					Files.createFile(RunJBSE_Sushi.this.parameters.getTracesFilePath());
				} catch (IOException e) {
					System.err.println("ERROR: exception raised:");
					e.printStackTrace(System.err);
					RunJBSE_Sushi.this.errorCodeAfterRun = 1;
					return true;
				}
			}
			return false;
		}
		
		@Override
		public boolean atTraceStart() {
			//trace initially assumed to be safe
			this.traceKind = TraceTypes.SAFE;
			return false;
		}
		
		private void doUpdateCoverage(String currentStateIdentifier, String branchTarget) {
			final long branchNumber;
			if (this.branchNumberOf.containsKey(branchTarget)) {
				branchNumber = this.branchNumberOf.get(branchTarget);
			} else {
				branchNumber = this.branchCounter++;
				this.branchNumberOf.put(branchTarget, branchNumber);
				this.branchTargets.add(branchTarget);
			}
			this.coverageCurrentTrace.add(branchNumber);
			this.coverage.put(currentStateIdentifier, new TreeSet<>(this.coverageCurrentTrace));
		}
		
		private void updateCoverage(State currentState) throws ThreadStackEmptyException {
			final String branchTarget = currentState.getCurrentMethodSignature().toString() + ":" + currentState.getPC();
			doUpdateCoverage(currentState.getIdentifier(), branchTarget);
		}
		
		private void updateCoverage(State currentState, int branchPC) throws ThreadStackEmptyException {
			final String branchTarget = currentState.getCurrentMethodSignature().toString() + ":" + branchPC + ":" + currentState.getPC();
			doUpdateCoverage(currentState.getIdentifier(), branchTarget);
		}
		
		@Override
		public boolean atStepPre() {
			//detects if the current bytecode is a jumping bytecode
			try {
				final State currentState = RunJBSE_Sushi.this.engine.getCurrentState();
				final int currentPC = currentState.getPC();
				
				//detect whether we are at the entry point of a method
				//and in case adds a pseudobranch for the entry point
				if (currentPC == 0) {
					updateCoverage(currentState);
				}
				
				//detects whether we are at a branch
				final byte currentBytecode = currentState.getInstruction();
				this.atJump = 
						(currentBytecode == Opcodes.OP_IF_ACMPEQ ||
						currentBytecode == Opcodes.OP_IF_ACMPNE ||	
						currentBytecode == Opcodes.OP_IFNONNULL ||	
						currentBytecode == Opcodes.OP_IFNULL ||	
						currentBytecode == Opcodes.OP_IFEQ ||
						currentBytecode == Opcodes.OP_IFGE ||	
						currentBytecode == Opcodes.OP_IFGT ||	
						currentBytecode == Opcodes.OP_IFLE ||	
						currentBytecode == Opcodes.OP_IFLT ||	
						currentBytecode == Opcodes.OP_IFNE ||	
						currentBytecode == Opcodes.OP_IF_ICMPEQ ||	
						currentBytecode == Opcodes.OP_IF_ICMPGE ||	
						currentBytecode == Opcodes.OP_IF_ICMPGT ||	
						currentBytecode == Opcodes.OP_IF_ICMPLE ||	
						currentBytecode == Opcodes.OP_IF_ICMPLT ||	
						currentBytecode == Opcodes.OP_IF_ICMPNE ||	
						currentBytecode == Opcodes.OP_LOOKUPSWITCH ||	
						currentBytecode == Opcodes.OP_TABLESWITCH);
				if (this.atJump) {
					this.jumpPC = currentPC;
				}
				
				//detects whether we are at a load constant bytecode
				this.atLoadConstant = 
						(currentBytecode == Opcodes.OP_LDC ||
						currentBytecode == Opcodes.OP_LDC_W ||
						currentBytecode == Opcodes.OP_LDC2_W);
				if (this.atLoadConstant) {
					this.stringLiteralFrame = currentState.getCurrentFrame();
				}
			} catch (ThreadStackEmptyException e) {
				System.err.println("ERROR: exception raised:");
				e.printStackTrace(System.err);
				RunJBSE_Sushi.this.errorCodeAfterRun = 2;
				return true;
			}
			
			return false;
		}
		
		@Override
		public boolean atBranch(BranchPoint bp) {
			this.atJumpBacktrack.put(bp, this.atJump);
			this.jumpPCBacktrack.put(bp, this.jumpPC);
			return false;
		}
		
		@Override
		public boolean atStepPost() {
			final State currentState = RunJBSE_Sushi.this.engine.getCurrentState();
			
			//if we stepped a branching bytecode, records coverage
			if (this.atJump) {
				try {
					updateCoverage(currentState, this.jumpPC);
				} catch (ThreadStackEmptyException e) {
					System.err.println("ERROR: exception raised:");
					e.printStackTrace(System.err);
					RunJBSE_Sushi.this.errorCodeAfterRun = 2;
					return true;
				}
			}
			
			//if we stepped a load constant bytecode, records string literals
			if (this.atLoadConstant) {
				try {
					if (currentState.getCurrentFrame() == this.stringLiteralFrame) {
						final Value operand = this.stringLiteralFrame.operands(1)[0];
						if (operand instanceof Reference) {
							final Reference r = (Reference) operand;
							final Objekt o = currentState.getObject(r);
							if (o != null && o.getType().equals(JAVA_STRING)) {
								final String s = valueString(currentState, r);
								this.stringLiteralsCurrentTrace.add(s);
								this.stringLiterals.put(currentState.getIdentifier(), new TreeSet<>(this.stringLiteralsCurrentTrace));
							}
						}
					}
				} catch (ThreadStackEmptyException | InvalidNumberOfOperandsException e) {
					System.err.println("ERROR: exception raised:");
					e.printStackTrace(System.err);
					RunJBSE_Sushi.this.errorCodeAfterRun = 2;
					return true;
				}
			}
			
			return false;
		}
		
		@Override
		public boolean atBacktrackPost(BranchPoint bp) {
			final State currentState = RunJBSE_Sushi.this.engine.getCurrentState();
			String uptraceId = currentState.getIdentifier();
			{
				TreeSet<Long> uptraceCoverage = this.coverage.get(uptraceId);
				while (uptraceCoverage == null && uptraceId.lastIndexOf(StateTree.IDENTIFIER_SEPARATOR_COMPACT) != -1) {
					uptraceId = uptraceId.substring(0, uptraceId.lastIndexOf(StateTree.IDENTIFIER_SEPARATOR_COMPACT));
					uptraceCoverage = this.coverage.get(uptraceId);
				}
				this.coverageCurrentTrace = (uptraceCoverage == null ? new TreeSet<>() : new TreeSet<>(uptraceCoverage));
			}
			{
				TreeSet<String> uptraceStringLiterals = this.stringLiterals.get(uptraceId);
				while (uptraceStringLiterals == null && uptraceId.lastIndexOf(StateTree.IDENTIFIER_SEPARATOR_COMPACT) != -1) {
					uptraceId = uptraceId.substring(0, uptraceId.lastIndexOf(StateTree.IDENTIFIER_SEPARATOR_COMPACT));
					uptraceStringLiterals = this.stringLiterals.get(uptraceId);
				}
				this.stringLiteralsCurrentTrace = (uptraceStringLiterals == null ? new TreeSet<>() : new TreeSet<>(uptraceStringLiterals));
			}
			final Boolean atJump_ = this.atJumpBacktrack.get(bp);
			this.atJump = (atJump_ == null ? false : atJump_);
			final Integer jumpPC_ = this.jumpPCBacktrack.get(bp);
			this.jumpPC = (jumpPC_ == null ? 0 : jumpPC_);

			if (this.atJump) {
				try {
					updateCoverage(currentState, this.jumpPC);
				} catch (ThreadStackEmptyException e) {
					System.err.println("ERROR: exception raised:");
					e.printStackTrace(System.err);
					RunJBSE_Sushi.this.errorCodeAfterRun = 2;
					return true;
				}
			}
			
			return false;
		}
		
		@Override
		public boolean atContradictionException(ContradictionException e) {
			this.traceKind = TraceTypes.CONTRADICTORY;
			return false;
		}

		@Override
		public boolean atFailureException(FailureException e) {
			this.traceKind = TraceTypes.UNSAFE;
			return false;
		}
		
		@Override
		public boolean atCannotManageStateException(CannotManageStateException e) {
			this.traceKind = TraceTypes.UNMANAGEABLE;
			return false;
		}
		
		@Override
		public boolean atScopeExhaustionHeap() {
			this.traceKind = TraceTypes.OUT_OF_SCOPE;
			return false;
		}
		
		@Override
		public boolean atScopeExhaustionDepth() {
			this.traceKind = TraceTypes.OUT_OF_SCOPE;
			return false;
		}
		
		@Override
		public boolean atScopeExhaustionCount() {
			this.traceKind = TraceTypes.OUT_OF_SCOPE;
			return false;
		}

		@Override
		public boolean atTraceEnd() {
			if (RunJBSE_Sushi.this.parameters.getTracesToShow().contains(this.traceKind)) {
				final State currentState = RunJBSE_Sushi.this.engine.getCurrentState();
				
				final StateFormatMode stateFormatMode = RunJBSE_Sushi.this.parameters.getStateFormatMode();
				if (stateFormatMode != null) {
					//emits the wrapper
					final Path f =  RunJBSE_Sushi.this.parameters.getWrapperFilePath(RunJBSE_Sushi.this.traceCounter);
					try (final BufferedWriter w = Files.newBufferedWriter(f)) {
						RunJBSE_Sushi.this.formatter.formatPrologue();
						RunJBSE_Sushi.this.formatter.formatStringLiterals(this.stringLiteralsCurrentTrace);
						RunJBSE_Sushi.this.formatter.formatState(currentState);
						RunJBSE_Sushi.this.formatter.formatEpilogue();
				        w.write(RunJBSE_Sushi.this.formatter.emit());
				        RunJBSE_Sushi.this.formatter.cleanup();
					} catch (IOException e) {
						System.err.println("ERROR: exception raised:");
						e.printStackTrace(System.err);
						RunJBSE_Sushi.this.errorCodeAfterRun = 1;
						return true;
					}
				}
				
				//updates the coverage and traces files
				if (this.coverageCurrentTrace.size() == 0) {
					//does nothing, the trace does not cover any branch
				} else if (RunJBSE_Sushi.this.parameters.getMustLogCoverageData()) {
					final int hardness = currentState.getPathCondition().size();
					try (final BufferedWriter wCoverage = Files.newBufferedWriter(RunJBSE_Sushi.this.parameters.getCoverageFilePath(), StandardOpenOption.APPEND);
						 final BufferedWriter wTraces = Files.newBufferedWriter(RunJBSE_Sushi.this.parameters.getTracesFilePath(), StandardOpenOption.APPEND)) {
						wCoverage.write(RunJBSE_Sushi.this.traceCounter + ", " + hardness);
						for (long branchNumber : this.coverageCurrentTrace) {
							wCoverage.write(", " + branchNumber);
						}
						wCoverage.newLine();
						wTraces.write(RunJBSE_Sushi.this.traceCounter + ", " + RunJBSE_Sushi.this.engine.getCurrentState().getIdentifier());
						wTraces.newLine();
					} catch (IOException e) {
						System.err.println("ERROR: exception raised:");
						e.printStackTrace(System.err);
						RunJBSE_Sushi.this.errorCodeAfterRun = 1;
						return true;
					}
				}
				
				++RunJBSE_Sushi.this.traceCounter;
			}
			return false;
		}
		
		@Override
		public void atEnd() {
			if (RunJBSE_Sushi.this.parameters.getMustLogCoverageData()) {
				try (final BufferedWriter wBranches = Files.newBufferedWriter(RunJBSE_Sushi.this.parameters.getBranchesFilePath())) {
					for (String branchTarget : this.branchTargets) {
						wBranches.write(branchTarget);
						wBranches.write("\n");
					}
				} catch (IOException e) {
					System.err.println("ERROR: exception raised:");
					e.printStackTrace(System.err);
					RunJBSE_Sushi.this.errorCodeAfterRun = 1;
				}
			}
		}
		
		@Override
		public void atTimeout() {
			//same as atEnd;
			atEnd();
		}
	}

	/**
	 * Runs the method.
	 * 
	 * @return an {@code int} value representing an error code, 
	 * {@code 0} if everything went ok, {@code 1} if the cause 
	 * of the error was external (inputs), {@code 2} if the cause
	 * of the error was internal (bugs).
	 */
	public int run() {
		// sets up this object
		int retVal = build();
		if (retVal > 0) {
			return retVal;
		}

		//runs
		try {
			this.runner.run();
			retVal = this.errorCodeAfterRun;
		} catch (ClasspathException | 
		         DecisionException | CannotManageStateException | 
				 EngineStuckException | CannotBacktrackException e) {
			System.err.println("ERROR: exception raised:");
			e.printStackTrace(System.err);
			retVal = 1;
		} catch (ThreadStackEmptyException | ContradictionException |
		         FailureException | UnexpectedInternalException e) {
			System.err.println("ERROR: exception raised:");
			e.printStackTrace(System.err);
			retVal = 2;
		}
		
		// quits the engine
		try {
			this.engine.close();
		} catch (DecisionException | UnexpectedInternalException e) {
			//really this does not care
		}

		// returns the error code
		return retVal;
	}
	
    private static final String COMMANDLINE_LAUNCH_Z3   = System.getProperty("os.name").toLowerCase().contains("windows") ? " /smt2 /in /t:10" : " -smt2 -in -t:10";
    private static final String COMMANDLINE_LAUNCH_CVC4 = " --lang=smt2 --output-lang=smt2 --no-interactive --incremental --tlimit-per=10000";
    
	/**
	 * Processes the provided {@link RunParameters} and builds the {@link Engine}
	 * which will be used by the runner to perform the symbolic execution.
	 * 
	 * @return a {@code int} value representing an error code.
	 */
	private int build() {
		//TODO possibly move inside a builder
		//TODO lots of controls on parameters
		//TODO rethrow exception rather than returning an int, and centralize logging in the receiver

		try {
			createFormatter();
			final RunnerParameters runnerParameters = this.parameters.getRunnerParameters();
			runnerParameters.setActions(new ActionsRun());
            final CalculatorRewriting calc = createCalculator();
			final EngineParameters engineParameters = runnerParameters.getEngineParameters();
			engineParameters.setCalculator(calc);
			createDecisionProcedure(calc);
			engineParameters.setDecisionProcedure(this.decisionProcedure);
			final RunnerBuilder rb = new RunnerBuilder();
			this.runner = rb.build(runnerParameters);
			this.engine = rb.getEngine();
			if (this.engine == null) {
				return 1;
			}
			if (this.consRepOk != null) {
				this.consRepOk.setInitialStateSupplier(this.engine::getInitialState); 
				this.consRepOk.setCurrentStateSupplier(this.engine::getCurrentState); 
			}
		} catch (NonexistingObservedVariablesException e) {
			//we do nothing
		} catch (CannotBuildDecisionProcedureException | DecisionException | 
				InitializationException | ClasspathException e) {
			System.err.println("ERROR: exception raised:");
			e.printStackTrace(System.err);
		    return 1;
		} catch (CannotBuildEngineException | InvalidClassFileFactoryClassException | 
				UnexpectedInternalException e) {
			System.err.println("ERROR: exception raised:");
			e.printStackTrace(System.err);
		    return 2;
		}
		
		return 0;
	}
	
	private long getTraceCounter() {
		return this.traceCounter;
	}
	
	private State getInitialState() {
		return this.engine.getInitialState();
	}
	
	private Map<PrimitiveSymbolic, Simplex> getModel() {
        try {
            return this.decisionProcedure.getModel();
        } catch (DecisionException e) {
            return null;
        }
	}
	
	private void createFormatter() throws CannotBuildFormatterException {
		final StateFormatMode type = this.parameters.getStateFormatMode();
        if (type == StateFormatMode.SUSHI_PARTIAL_HEAP) {
            this.formatter = new StateFormatterSushiPartialHeap(RunJBSE_Sushi.this.parameters.getMethodNumber(), this::getTraceCounter, this::getInitialState, this::getModel);
        } else if (type == StateFormatMode.SUSHI_PATH_CONDITION) {
            this.formatter = new StateFormatterSushiPathCondition(RunJBSE_Sushi.this.parameters.getMethodNumber(), this::getTraceCounter, this::getInitialState);
        } else if (type != null) {
            throw new CannotBuildFormatterException("Wrong formatter type " + this.parameters.getStateFormatMode());
        }
	}
	
	private CalculatorRewriting createCalculator() throws CannotBuildEngineException {
		final CalculatorRewriting calc;
		try {
			calc = new CalculatorRewriting();
			calc.addRewriter(new RewriterOperationOnSimplex()); //indispensable
			for (final Class<? extends Rewriter> rewriterClass : this.parameters.getRewriters()) {
				if (rewriterClass == null) { 
				    //no rewriter
				    continue; 
				}
				final Rewriter rewriter = (Rewriter) rewriterClass.newInstance();
				calc.addRewriter(rewriter);
			}
		} catch (InstantiationException | IllegalAccessException | UnexpectedInternalException e) {
			throw new CannotBuildCalculatorException(e);
		}
		return calc;
	}
	
	private void createDecisionProcedure(CalculatorRewriting calc)
	throws CannotBuildDecisionProcedureException {
        final Path path = this.parameters.getExternalDecisionProcedurePath();       

		//initializes cores
        final boolean needHeapCheck = this.parameters.getUseConservativeRepOks();
		DecisionProcedure core = new DecisionProcedureAlwSat();
		DecisionProcedure coreNumeric = (needHeapCheck ? new DecisionProcedureAlwSat() : null);
		
		//wraps cores with external numeric decision procedure
		final DecisionProcedureType type = this.parameters.getDecisionProcedureType();
		try {
		    if (type == DecisionProcedureType.Z3) {
		        final String z3 = (path == null ? "z3" : path.toString()) + COMMANDLINE_LAUNCH_Z3;
		        core = new DecisionProcedureSMTLIB2_AUFNIRA(core, calc, z3);
		        coreNumeric = (needHeapCheck ? new DecisionProcedureSMTLIB2_AUFNIRA(coreNumeric, calc, z3) : null);
		    } else if (type == DecisionProcedureType.CVC4) {
                final String cvc4 = (path == null ? "cvc4" : path.toString()) + COMMANDLINE_LAUNCH_CVC4;
		        core = new DecisionProcedureSMTLIB2_AUFNIRA(core, calc, cvc4);
		        coreNumeric = (needHeapCheck ? new DecisionProcedureSMTLIB2_AUFNIRA(coreNumeric, calc, cvc4) : null);
		    } else {
		        core.close();
		        if (coreNumeric != null) {
		            coreNumeric.close();
		        }
		        throw new CannotBuildDecisionProcedureException("Wrong decision procedure type " + type);
		    }
		} catch (DecisionException e) {
			throw new CannotBuildDecisionProcedureException(e);
		}
		
		//further wraps cores with sign analysis, if required
		if (this.parameters.getDoSignAnalysis()) {
			core = new DecisionProcedureSignAnalysis(core, calc);
			coreNumeric = (needHeapCheck ? new DecisionProcedureSignAnalysis(coreNumeric, calc) : null);
		}
		
		//further wraps cores with equality analysis, if required
		if (this.parameters.getDoEqualityAnalysis()) {
			core = new DecisionProcedureEquality(core, calc);
			coreNumeric = (needHeapCheck ? new DecisionProcedureEquality(coreNumeric, calc) : null);
		}
		
		//further wraps core with LICS decision procedure
		if (this.parameters.getUseLICS()) {
			core = new DecisionProcedureLICS(core, calc, this.parameters.getLICSRulesRepo());
		}
		
		//further wraps core with class init decision procedure
		core = new DecisionProcedureClassInit(core, calc, this.parameters.getClassInitRulesRepo());
		
		//further wraps core with conservative repOk decision procedure
		if (this.parameters.getUseConservativeRepOks()) {
		    final RunnerParameters checkerParameters = this.parameters.getConcretizationDriverParameters();
		    checkerParameters.setDecisionProcedure(new DecisionProcedureAlgorithms(coreNumeric, calc));
			this.consRepOk = 
			    new DecisionProcedureConservativeRepOk(core, calc, checkerParameters, this.parameters.getConservativeRepOks());
			core = this.consRepOk;
		}

		//wraps core with custom wrappers
		for (DecisionProcedureCreationStrategy c : this.parameters.getDecisionProcedureCreationStrategies()) {
			core = c.createAndWrap(core, calc);
		}

		//finally guidance
		if (this.parameters.isGuided()) {
			final RunnerParameters guidanceDriverParameters = this.parameters.getGuidanceDriverParameters(calc);
			try {
				this.guidance = new DecisionProcedureGuidance(core, calc, guidanceDriverParameters, this.parameters.getMethodSignature());
			} catch (GuidanceException | UnexpectedInternalException e) {
				throw new CannotBuildDecisionProcedureException(e);
			}
			core = this.guidance;
		}
		
		//sets the result
		this.decisionProcedure = ((core instanceof DecisionProcedureAlgorithms) ? 
		    (DecisionProcedureAlgorithms) core :
		    new DecisionProcedureAlgorithms(core, calc));
	}    
}
