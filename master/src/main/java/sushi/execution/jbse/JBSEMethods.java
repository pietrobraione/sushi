package sushi.execution.jbse;

import static sushi.util.DirectoryUtils.getMethodsFilePath;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import sushi.Coverage;
import sushi.Options;
import sushi.ParseException;
import sushi.exceptions.CheckClasspathException;
import sushi.exceptions.ToolException;

public final class JBSEMethods extends JBSEAbstract {
	private List<Integer> tasks = null;

	public JBSEMethods(Options options, boolean emitWrappers) 
	throws ToolException, CheckClasspathException {
		super(options, emitWrappers, true);
	}
	
	@Override
	public List<Integer> tasks() throws IOException {
		if (this.tasks == null) {
			this.tasks = new ArrayList<>(this.testMethods.size());
			for (int i = 0; i < this.testMethods.size(); ++i) {
				this.tasks.add(i);
			}
			
			try (final BufferedWriter w = Files.newBufferedWriter(getMethodsFilePath(this.options))) {
				for (List<String> signature : this.testMethods) {
					w.write(signature.get(0));
					w.write(":");
					w.write(signature.get(1));
					w.write(":");
					w.write(signature.get(2));
					w.newLine();
				}
			}
		}
		return this.tasks;
	}
	
	@Override
	public JBSEParameters getInvocationParameters(int taskNumber) 
	throws FileNotFoundException, ParseException, IOException {
		JBSEParameters p = super.getInvocationParameters(taskNumber);
		p.setShowSafe(this.options.getCoverage() == Coverage.UNSAFE ? false : true);
		p.setShowUnsafe(true);
		p.setShowOutOfScope(false);
		p.setShowContradictory(false);
		p.setShowUnmanageable(false);
		return p;
	}
	
	@Override
	public void reset() {
		this.tasks = null;
	}
	
	//TODO getTimeBudget(), degreeOfParallelism()
}
