package sushi.execution.jbse;

import static sushi.util.ClassReflectionUtils.getVisibleMethods;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
import sushi.exceptions.CheckClasspathException;
import sushi.exceptions.ToolException;
import sushi.execution.Tool;
import sushi.execution.Worker;
import sushi.execution.jbse.JBSEParameters.StateFormatMode;
import sushi.util.CollectionUtils;
import sushi.util.DirectoryUtils;

public abstract class JBSEAbstract extends Tool<JBSEParameters> {	
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
	
	private final boolean emitWrappers;
	private final boolean mustLogCoverageData;
	
	protected final Options options;
	protected List<List<String>> testMethods = null;	

	protected JBSEAbstract(Options options, boolean emitWrappers, boolean mustLogCoverageData) 
	throws ToolException, CheckClasspathException {
		this.options = options;
		this.emitWrappers = emitWrappers;
		this.mustLogCoverageData = mustLogCoverageData;
		if (this.options.getTargetMethod() == null) {
			final String targetClass = this.options.getTargetClass();
			if (targetClass == null) {
				throw new ToolException("Neither a target class nor a target method was specified.");
			}
			try {
				this.testMethods = getVisibleMethods(this.options, targetClass, this.options.getVisibility() == Visibility.PUBLIC);
			} catch (ClassNotFoundException | MalformedURLException | SecurityException e) {
				throw new CheckClasspathException(e);
			}
		} else {
			final List<String> methodSignature = this.options.getTargetMethod();
			this.testMethods = Collections.singletonList(methodSignature);
		}
	}
	
	@Override
	public JBSEParameters getInvocationParameters(int taskNumber) throws FileNotFoundException, ParseException, IOException {
		final JBSEParameters p = new JBSEParameters();
		p.addClasspath(this.options.getJBSELibraryPath().toString()); //for Analysis.*
		p.addClasspath(CollectionUtils.toStringArray(this.options.getClassesPath())); //target code
		p.setMethodNumber(taskNumber);
		final String[] methodSignature = this.testMethods.get(taskNumber).toArray(EMPTY_STRING_ARRAY);
		p.setMethodSignature(methodSignature[0], methodSignature[1], methodSignature[2]);
		p.setTimeout(this.options.getJBSEBudget(), TimeUnit.SECONDS);
		p.setExternalDecisionProcedurePath(this.options.getZ3Path().toString());
		p.setStateFormatMode(this.emitWrappers ? StateFormatMode.SUSHI_PATH_CONDITION : null);
		p.setMustLogCoverageData(this.mustLogCoverageData);
		p.setWrapperFilePathBuilder((t1, t2) -> DirectoryUtils.getJBSEOutFilePath(this.options, t1, t2));
		p.setCoverageFilePathBuilder((t) -> DirectoryUtils.getCoverageFilePath(this.options, t));
		p.setBranchesFilePathBuilder((t) -> DirectoryUtils.getBranchesFilePath(this.options, t));
		p.setTracesFilePathBuilder((t) -> DirectoryUtils.getTracesFilePath(this.options, t));
		p.loadHEXFiles(this.options.getHEXFiles());
		for (Map.Entry<String, Integer> entry : this.options.getHeapScope().entrySet()) {
			p.setHeapScope(entry.getKey(), entry.getValue());
		}
		p.setDepthScope(this.options.getDepthScope());
		p.setCountScope(this.options.getCountScope());
		for (List<String> sig : this.options.getUninterpreted()) {
			p.addUninterpreted(sig.get(0), sig.get(1), sig.get(2));
		}
		p.setDoSignAnalysis(this.options.getDoSignAnalysis());
		p.setDoEqualityAnalysis(this.options.getDoEqualityAnalysis());
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
	public int degreeOfParallelism() throws Exception {
		return (this.options.getParallelismJBSE() == 0 ? tasks().size() * redundance() : this.options.getParallelismJBSE());
	}
}
