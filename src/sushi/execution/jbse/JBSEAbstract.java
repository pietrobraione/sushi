package sushi.execution.jbse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jbse.apps.settings.ParseException;
import sushi.configure.Coverage;
import sushi.configure.JBSEParameters;
import sushi.configure.JBSEParameters.DecisionProcedureType;
import sushi.configure.JBSEParameters.StateFormatMode;
import sushi.configure.Options;
import sushi.configure.Visibility;
import sushi.exceptions.JBSEException;
import sushi.execution.Tool;
import sushi.execution.Worker;
import sushi.logging.Logger;
import sushi.modifier.Modifier;
import sushi.util.ArrayUtils;
import sushi.util.CollectionUtils;
import sushi.util.DirectoryUtils;
import sushi.util.ReflectionUtils;

public abstract class JBSEAbstract extends Tool<JBSEParameters> {	
	private static final Logger logger = new Logger(JBSEAbstract.class);
	
	private final boolean emitWrappers;
	private final boolean mustLogCoverageData;
	
	protected List<List<String>> testMethods = null;	

	protected JBSEAbstract(boolean emitWrappers, boolean mustLogCoverageData) { 
		this.emitWrappers = emitWrappers;
		this.mustLogCoverageData = mustLogCoverageData;
	}
	
	protected void populateTestMethods() {
		final Options options = Options.I();
		if (options.getTargetMethod() == null) {
			final String targetClass = options.getTargetClass();
			if (targetClass == null) {
				logger.error("ERROR: neither a target class nor a target method was specified.");
				throw new JBSEException("ERROR: neither a target class nor a target method was specified.");
			}
			try {
				this.testMethods = ReflectionUtils.getVisibleMethods(targetClass, options.getVisibility() == Visibility.PUBLIC);
			} catch (ClassNotFoundException e) {
				logger.error("Unexpected error: Cannot load the target class.");
				throw new JBSEException(e);
			}
		} else {
			final List<String> methodSignature = options.getTargetMethod();
			this.testMethods = Collections.singletonList(methodSignature);
		}
	}
	
	@Override
	public JBSEParameters getInvocationParameters(int taskNumber) {
		final Options options = Options.I();
		final JBSEParameters p = new JBSEParameters();
		p.addClasspath(options.getJBSELibraryPath().toString()); //for Analysis.*
		p.addClasspath(options.getJREPath().toString()); //for JRE
		p.addClasspath(CollectionUtils.toStringArray(options.getClassesPath())); //target code
		p.setMethodNumber(taskNumber);
		final String[] methodSignature = this.testMethods.get(taskNumber).toArray(ArrayUtils.EMPTY_STRING_ARRAY);
		p.setMethodSignature(methodSignature[0], methodSignature[1], methodSignature[2]);
		p.setTimeout(options.getJBSEBudget(), TimeUnit.SECONDS);
		p.setDecisionProcedureType(DecisionProcedureType.Z3);
		p.setExternalDecisionProcedurePath(options.getZ3Path().toString());
		p.setMustLogCoverageData(this.mustLogCoverageData);
		final DirectoryUtils utils = DirectoryUtils.I();
		p.setWrapperFilePathBuilder(utils::getJBSEOutFilePath);
		p.setCoverageFilePathBuilder(utils::getCoverageFilePath);
		p.setBranchesFilePathBuilder(utils::getBranchesFilePath);
		p.setTracesFilePathBuilder(utils::getTracesFilePath);
		p.setShowSafe(options.getCoverage() == Coverage.UNSAFE ? false : true);
		p.setShowUnsafe(true);
		p.setShowOutOfScope(false);
		p.setShowContradictory(false);
		p.setStateFormatMode(this.emitWrappers ? StateFormatMode.SUSHI_PATH_CONDITION : null);
		//BEGIN settings for debugging with jbse.run.Run
		/*p.setOutputFileName(DirectoryUtils.I().getExperimentDirPath().resolve("foo.txt").toString());
		p.setStepShowMode(StepShowMode.LEAVES);
		p.setShowInfo(false);
		p.setShowWarnings(false);
		p.setShowOnConsole(false);
		p.setStateIdentificationMode(StateIdentificationMode.LONG);
		p.setBreadthMode(BreadthMode.ALL_DECISIONS);*/
		//END settings for debugging with jbse.run.Run
		
		setUserDefinedParameters(p);
		
		return p;
	}
	
	private void setUserDefinedParameters(JBSEParameters p) {
	    try {
            Modifier.I().modify(p);
		} catch (FileNotFoundException e) {
			logger.error("Settings file for symbolic execution not found", e);
			throw new JBSEException(e);
		} catch (ParseException e) {
			logger.error("Settings file for symbolic execution ill-formed", e);
			throw new JBSEException(e);
		} catch (IOException e) {
			logger.error("Error while closing settings file", e);
			throw new JBSEException(e);
		}
	}
	
	//TODO
	@Override
	public int getTimeBudget() {
		return Options.I().getJBSEBudget();
	}

	@Override
	public final Worker getWorker(int taskNumber) {
		return new JBSEWorker(this, taskNumber);
	}
	
	//TODO
	@Override
	public int degreeOfParallelism() {
		return (Options.I().getParallelismJBSE() == 0 ? tasks().size() * redundance() : Options.I().getParallelismJBSE());
	}
}
