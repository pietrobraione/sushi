package sushi.execution.javac;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import sushi.configure.Options;
import sushi.exceptions.JavacException;
import sushi.execution.Tool;
import sushi.logging.Logger;
import sushi.util.ArrayUtils;
import sushi.util.DirectoryUtils;
import sushi.util.IOUtils;

public final class Javac extends Tool<String[]> {
	private static final Logger logger = new Logger(Javac.class);

	private String commandLine;
	private ArrayList<Integer> tasks = null;
	private ArrayList<Integer> targetMethodNumbers = null;
	private ArrayList<Integer> traceNumbersLocal = null;

	public Javac() { }

	public String getCommandLine() {
		return this.commandLine;
	}
	
	@Override
	public List<Integer> tasks() {
		if (this.tasks == null) {
			this.tasks = new ArrayList<>();
			this.targetMethodNumbers = new ArrayList<>();
			this.traceNumbersLocal = new ArrayList<>();
			try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.I().getMinimizerOutFilePath())) {
				String line;
				int task = 0;
				while ((line = r.readLine()) != null) {
					this.tasks.add(task);
					final String[] fields = line.split(",");
					this.targetMethodNumbers.add(Integer.parseInt(fields[1].trim()));
					this.traceNumbersLocal.add(Integer.parseInt(fields[2].trim()));
					++task;
				}
			} catch (IOException e) {
				logger.error("Unable to find and open minimizer output file " + DirectoryUtils.I().getMinimizerOutFilePath().toString());
				throw new JavacException(e);
			}
		}
		return this.tasks;
	}

	@Override
	public String[] getInvocationParameters(int i) {
		final ArrayList<String> javac = new ArrayList<>();
		javac.add("-cp");
		final String classPath = IOUtils.concatClassPath( 
				IOUtils.concatClassPath(Options.I().getClassesPath()),
				IOUtils.concatClassPath(Options.I().getSushiLibPath()));
		javac.add(classPath);
		final Path emitFile = DirectoryUtils.I().getJBSEOutFilePath(this.targetMethodNumbers.get(i), this.traceNumbersLocal.get(i));
		javac.add("-d");
		javac.add(emitFile.getParent().toString());
		javac.add(emitFile.toString());
		this.commandLine = "javac " + javac.toString().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", "");
		return javac.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
	}

	@Override
	public int getTimeBudget() {
		return Options.I().getJavacBudget();
	}

	@Override
	public JavacWorker getWorker(int taskNumber) {
		return new JavacWorker(this, taskNumber);
	}
}
