package sushi.execution.jbse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jbse.rewr.RewriterAbsSum;
import jbse.rewr.RewriterArcTan;
import jbse.rewr.RewriterDivisionEqualsZero;
import jbse.rewr.RewriterPolynomials;
import jbse.rewr.RewriterSinCos;
import jbse.rewr.RewriterSqrt;
import jbse.rewr.RewriterTan;
import sushi.Options;
import sushi.ParseException;
import sushi.Rewriter;
import sushi.Visibility;
import sushi.exceptions.JBSEException;
import sushi.execution.Tool;
import sushi.execution.Worker;
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
		p.addClasspath(this.options.getJBSELibraryPath().toString()); //for Analysis.*
		p.addClasspath(CollectionUtils.toStringArray(this.options.getClassesPath())); //target code
		p.setMethodNumber(taskNumber);
		final String[] methodSignature = this.testMethods.get(taskNumber).toArray(ArrayUtils.EMPTY_STRING_ARRAY);
		p.setMethodSignature(methodSignature[0], methodSignature[1], methodSignature[2]);
		p.setTimeout(this.options.getJBSEBudget(), TimeUnit.SECONDS);
		p.setExternalDecisionProcedurePath(this.options.getZ3Path().toString());
		p.setStateFormatMode(this.emitWrappers ? StateFormatMode.SUSHI_PATH_CONDITION : null);
		p.setMustLogCoverageData(this.mustLogCoverageData);
		p.setWrapperFilePathBuilder((t1, t2) -> DirectoryUtils.getJBSEOutFilePath(this.options, t1, t2));
		p.setCoverageFilePathBuilder((t) -> DirectoryUtils.getCoverageFilePath(this.options, t));
		p.setBranchesFilePathBuilder((t) -> DirectoryUtils.getBranchesFilePath(this.options, t));
		p.setTracesFilePathBuilder((t) -> DirectoryUtils.getTracesFilePath(this.options, t));
		try {
			p.loadHEXFiles(this.options.getHEXFiles());
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
		setRewriters(this.options.getRewriters(), p);
		return p;
	}
	
	private static void setRewriters(EnumSet<Rewriter> rewriters, JBSEParameters p) {
		if (rewriters.contains(Rewriter.ABS_SUM)) {
			rewriters.add(Rewriter.POLYNOMIALS);
		}

		if (rewriters.contains(Rewriter.ARCTAN)) {
			p.addRewriter(RewriterArcTan.class);
		}
		if (rewriters.contains(Rewriter.DIVISION_EQUALS_ZERO)) {
			p.addRewriter(RewriterDivisionEqualsZero.class);
		}
		if (rewriters.contains(Rewriter.SIN_COS)) {
			p.addRewriter(RewriterSinCos.class);
		}
		if (rewriters.contains(Rewriter.SQRT)) {
			p.addRewriter(RewriterSqrt.class);
		}
		if (rewriters.contains(Rewriter.TAN)) {
			p.addRewriter(RewriterTan.class);
		}
		if (rewriters.contains(Rewriter.POLYNOMIALS)) {
			p.addRewriter(RewriterPolynomials.class);
		}
		if (rewriters.contains(Rewriter.ABS_SUM)) {
			p.addRewriter(RewriterAbsSum.class);
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
