package sushi.execution.jbse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import sushi.exceptions.JBSEException;
import sushi.logging.Logger;
import sushi.util.DirectoryUtils;

public final class JBSEMethods extends JBSEAbstract {
	private static final Logger logger = new Logger(JBSEMethods.class);
	
	private List<Integer> tasks = null;

	public JBSEMethods(boolean emitWrappers) {
		super(emitWrappers, true);
	}
	
	@Override
	public List<Integer> tasks() {
		if (this.tasks == null) {
			this.tasks = new ArrayList<>(this.testMethods.size());
			for (int i = 0; i < this.testMethods.size(); ++i) {
				this.tasks.add(i);
			}
			
			try (final BufferedWriter w = Files.newBufferedWriter(DirectoryUtils.I().getMethodsFilePath())) {
				for (List<String> signature : this.testMethods) {
					w.write(signature.get(0));
					w.write(":");
					w.write(signature.get(1));
					w.write(":");
					w.write(signature.get(2));
					w.newLine();
				}
			} catch (IOException e) {
				logger.error("Unable to find and open methods output file " + DirectoryUtils.I().getMethodsFilePath().toString());
				throw new JBSEException(e);
			}
		}
		return this.tasks;
	}
	
	@Override
	public void reset() {
		this.tasks = null;
	}
	
	//TODO getTimeBudget(), degreeOfParallelism()
}
