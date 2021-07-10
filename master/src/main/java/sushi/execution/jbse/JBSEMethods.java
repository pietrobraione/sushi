package sushi.execution.jbse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import sushi.Coverage;
import sushi.Options;
import sushi.exceptions.JBSEException;
import sushi.logging.Logger;
import sushi.util.DirectoryUtils;

public final class JBSEMethods extends JBSEAbstract {
	private static final Logger logger = new Logger(JBSEMethods.class);
	
	private List<Integer> tasks = null;

	public JBSEMethods(Options options, boolean emitWrappers) {
		super(options, emitWrappers, true);
	}
	
	@Override
	public List<Integer> tasks() {
		if (this.tasks == null) {
			this.tasks = new ArrayList<>(this.testMethods.size());
			for (int i = 0; i < this.testMethods.size(); ++i) {
				this.tasks.add(i);
			}
			
			try (final BufferedWriter w = Files.newBufferedWriter(DirectoryUtils.getMethodsFilePath(this.options))) {
				for (List<String> signature : this.testMethods) {
					w.write(signature.get(0));
					w.write(":");
					w.write(signature.get(1));
					w.write(":");
					w.write(signature.get(2));
					w.newLine();
				}
			} catch (IOException e) {
				logger.error("Unable to find and open methods output file " + DirectoryUtils.getMethodsFilePath(this.options).toString());
				throw new JBSEException(e);
			}
		}
		return this.tasks;
	}
	
	@Override
	public JBSEParameters getInvocationParameters(int taskNumber) {
		JBSEParameters p = super.getInvocationParameters(taskNumber);
		p.setShowSafe(this.options.getCoverage() == Coverage.UNSAFE ? false : true);
		p.setShowUnsafe(true);
		p.setShowOutOfScope(false);
		p.setShowContradictory(false);
		return p;
	}
	
	@Override
	public void reset() {
		this.tasks = null;
	}
	
	//TODO getTimeBudget(), degreeOfParallelism()
}
