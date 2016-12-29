package sushi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;

import sushi.configure.Options;
import sushi.exceptions.InternalUnexpectedException;
import sushi.execution.ExecutionManager;
import sushi.execution.ExecutionResult;
import sushi.execution.Tool;
import sushi.execution.bestpath.BestPath;
import sushi.execution.evosuite.Evosuite;
import sushi.execution.javac.Javac;
import sushi.execution.jbse.JBSEMethods;
import sushi.execution.jbse.JBSETraces;
import sushi.execution.listpaths.ListPaths;
import sushi.execution.merger.Merger;
import sushi.execution.minimizer.Minimizer;
import sushi.logging.Logger;
import sushi.modifier.Modifier;
import sushi.util.ClasspathUtils;

public class Main {
	private static final String VERSION = "0.1";

	public Main() { }

	public static void main(String[] args) {
		Main main = new Main();
		main.startSushi(args);
	}
	
	private void startSushi(String[] args) {
		final Options options = Options.I();
		
		final CmdLineParser parser = new CmdLineParser(options, ParserProperties.defaults().withUsageWidth(200));
		try {
			parser.parseArgument(processArgs(args));
		} catch (CmdLineException e) {
			System.err.println("Error: " + e.getMessage());
			printUsage(parser);
			System.exit(-1);
		}
		
		if (options.getHelp()) {
			printUsage(parser);
			System.exit(0);
		}
		
		Modifier.I().modify(options);

		Logger.setLevel(options.getLogLevel());
		final Logger logger = new Logger(Main.class);
		logger.info("This is Sushi, version " + VERSION + ", © 2015-2017 University of Milano-Bicocca and University of Lugano");
		
		ClasspathUtils.checkClasspath();
		
		final Tool<?>[] tools;
		switch (options.getCoverage()) {
		case PATHS:
			tools = new Tool[]{ new JBSEMethods(true), new Merger(), new ListPaths(), new Javac(), new Evosuite() };
			break;
		case UNSAFE:
			tools = new Tool[]{ new JBSEMethods(false), new Merger(), new BestPath(), new JBSETraces(), new Javac(), new Evosuite() };
			break;
		case BRANCHES:
			tools = new Tool[]{ new JBSEMethods(false), new Merger(), new Minimizer(), new JBSETraces(), new Javac(), new Evosuite() };
			break;
		default:
			logger.error("Unexpected internal error: unexpected value for -emit option");
			throw new InternalUnexpectedException("Unexpected internal error: unexpected value for -emit option");
		}
		
		final boolean doEverything = (options.getPhases() == null);
		
		int currentPhase = 1;
		for (Tool<?> tool : tools) {
			if (doEverything || options.getPhases().contains(currentPhase)) {
				logger.info("Phase " + currentPhase +": executing tool " + tool.getName());
				final ExecutionResult[] result = ExecutionManager.execute(tool);
				for (int i = 0; i < result.length; ++i) {
					if (result[i] != null && result[i].getExitStatus() != 0) {
						logger.error("Tool " + tool.getName() + " task " + tool.tasks().get(i / tool.redundance()) + " terminated with exit status " + result[i].getExitStatus());
						System.exit(1);
					}
				}
			}
			++currentPhase;
		}

		logger.info("Sushi terminates");
		System.exit(0);
	}

	private static String[] processArgs(final String[] args) {
		final Pattern argPattern = Pattern.compile("(-[a-zA-Z_-]+)=(.*)");
		final Pattern quotesPattern = Pattern.compile("^['\"](.*)['\"]$");
		final List<String> processedArgs = new ArrayList<String>();

		for (String arg : args) {
			final Matcher matcher = argPattern.matcher(arg);
			if (matcher.matches()) {
				processedArgs.add(matcher.group(1));
				final String value = matcher.group(2);
				final Matcher quotesMatcher = quotesPattern.matcher(value);
				if (quotesMatcher.matches()) {
					processedArgs.add(quotesMatcher.group(1));
				} else {
					processedArgs.add(value);
				}
			} else {
				processedArgs.add(arg);
			}
		}

		return processedArgs.toArray(new String[0]);
	}

	private static void printUsage(final CmdLineParser parser) {
		System.err.println("Usage: java " + Main.class.getName() + " <options>");
		System.err.println("where <options> are:");
		// print the list of available options
		parser.printUsage(System.err);
	}
}
