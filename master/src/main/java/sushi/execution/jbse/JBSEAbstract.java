package sushi.execution.jbse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import sushi.Options;
import sushi.ParseException;
import sushi.Visibility;
import sushi.exceptions.JBSEException;
import sushi.execution.Tool;
import sushi.execution.Worker;
import sushi.execution.jbse.JBSEParameters.DecisionProcedureType;
import sushi.execution.jbse.JBSEParameters.StateFormatMode;
import sushi.logging.Logger;
import sushi.util.ArrayUtils;
import sushi.util.CollectionUtils;
import sushi.util.DirectoryUtils;
import sushi.util.ClassReflectionUtils;

public abstract class JBSEAbstract extends Tool<JBSEParameters> {	
	private static final Logger logger = new Logger(JBSEAbstract.class);
	
	private final boolean emitWrappers;
	private final boolean mustLogCoverageData;
	
	protected final Options options;
	protected List<List<String>> testMethods = null;	

	protected JBSEAbstract(Options options, boolean emitWrappers, boolean mustLogCoverageData) {
		this.options = options;
		this.emitWrappers = emitWrappers;
		this.mustLogCoverageData = mustLogCoverageData;
		if (this.options.getTargetMethod() == null) {
			final String targetClass = this.options.getTargetClass();
			if (targetClass == null) {
				logger.error("ERROR: neither a target class nor a target method was specified.");
				throw new JBSEException("ERROR: neither a target class nor a target method was specified.");
			}
			try {
				this.testMethods = ClassReflectionUtils.getVisibleMethods(this.options, targetClass, this.options.getVisibility() == Visibility.PUBLIC);
			} catch (ClassNotFoundException e) {
				logger.error("Unexpected error: Cannot load the target class.");
				throw new JBSEException(e);
			}
		} else {
			final List<String> methodSignature = this.options.getTargetMethod();
			this.testMethods = Collections.singletonList(methodSignature);
		}
	}
	
	@Override
	public JBSEParameters getInvocationParameters(int taskNumber) {
		final JBSEParameters p = new JBSEParameters();
		p.addClasspath(options.getJBSELibraryPath().toString()); //for Analysis.*
		p.addClasspath(CollectionUtils.toStringArray(options.getClassesPath())); //target code
		p.setMethodNumber(taskNumber);
		final String[] methodSignature = this.testMethods.get(taskNumber).toArray(ArrayUtils.EMPTY_STRING_ARRAY);
		p.setMethodSignature(methodSignature[0], methodSignature[1], methodSignature[2]);
		p.setTimeout(options.getJBSEBudget(), TimeUnit.SECONDS);
		p.setDecisionProcedureType(DecisionProcedureType.Z3);
		p.setExternalDecisionProcedurePath(options.getZ3Path().toString());
		p.setMustLogCoverageData(this.mustLogCoverageData);
		p.setWrapperFilePathBuilder((t1, t2) -> DirectoryUtils.getJBSEOutFilePath(this.options, t1, t2));
		p.setCoverageFilePathBuilder((t) -> DirectoryUtils.getCoverageFilePath(this.options, t));
		p.setBranchesFilePathBuilder((t) -> DirectoryUtils.getBranchesFilePath(this.options, t));
		p.setTracesFilePathBuilder((t) -> DirectoryUtils.getTracesFilePath(this.options, t));
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
	    	this.options.getParametersModifier().modify(p);
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
	
	@Override
	public int getTimeBudget() {
		return this.options.getJBSEBudget();
	}

	@Override
	public final Worker getWorker(int taskNumber) {
		return new JBSEWorker(this, taskNumber);
	}
	
	@Override
	public int degreeOfParallelism() {
		return (this.options.getParallelismJBSE() == 0 ? tasks().size() * redundance() : this.options.getParallelismJBSE());
	}
}
