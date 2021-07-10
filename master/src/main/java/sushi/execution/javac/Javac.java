package sushi.execution.javac;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import sushi.Options;
import sushi.exceptions.JavacException;
import sushi.execution.Tool;
import sushi.logging.Logger;
import sushi.util.ArrayUtils;
import sushi.util.DirectoryUtils;
import sushi.util.IOUtils;

public final class Javac extends Tool<String[]> {
	private static final Logger logger = new Logger(Javac.class);

	private final Options options;
	private String commandLine;
	private ArrayList<Integer> tasks = null;
	private ArrayList<Integer> targetMethodNumbers = null;
	private ArrayList<Integer> traceNumbersLocal = null;

	public Javac(Options options) { 
		this.options = options;
	}

	public String getCommandLine() {
		return this.commandLine;
	}
	
	@Override
	public List<Integer> tasks() {
		if (this.tasks == null) {
			this.tasks = new ArrayList<>();
			this.targetMethodNumbers = new ArrayList<>();
			this.traceNumbersLocal = new ArrayList<>();
			try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.getMinimizerOutFilePath(this.options))) {
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
				logger.error("Unable to find and open minimizer output file " + DirectoryUtils.getMinimizerOutFilePath(this.options).toString());
				throw new JavacException(e);
			}
		}
		return this.tasks;
	}

	@Override
	public String[] getInvocationParameters(int i) {
		final String classPath = IOUtils.concatClassPath( 
				IOUtils.concatClassPath(this.options.getClassesPath()),
				IOUtils.concatClassPath(this.options.getSushiLibPath()));
		final Path destinationDirectory = DirectoryUtils.getTmpDirPath(this.options);
		final Path fileToCompile = DirectoryUtils.getJBSEOutFilePath(this.options, this.targetMethodNumbers.get(i), this.traceNumbersLocal.get(i));
		final ArrayList<String> javac = new ArrayList<>();
		javac.add("-cp");
		javac.add(classPath);
		javac.add("-d");
		javac.add(destinationDirectory.toString());
		javac.add(fileToCompile.toString());
		this.commandLine = "javac " + javac.toString().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", "");
		return javac.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
	}
	
	@Override
	public void reset() {
		this.tasks = null;
	}

	@Override
	public int getTimeBudget() {
		return this.options.getJavacBudget();
	}

	@Override
	public JavacWorker getWorker(int taskNumber) {
		return new JavacWorker(this.options, this, taskNumber);
	}
}
