package sushi;

import static sushi.util.ClassReflectionUtils.getInternalClassloader;
import static sushi.util.DirectoryUtils.possiblyCreateTmpDir;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;

import sushi.exceptions.CheckClasspathException;
import sushi.exceptions.CoordinatorException;
import sushi.exceptions.NoEvosuiteException;
import sushi.exceptions.NoTmpDirException;
import sushi.exceptions.ToolException;
import sushi.exceptions.WorkerTerminationException;
import sushi.exceptions.InternalException;
import sushi.exceptions.WorkerFailureException;
import sushi.execution.ExecutionManager;
import sushi.execution.ExitStatus;
import sushi.execution.Tool;
import sushi.execution.bestpath.BestPath;
import sushi.execution.evosuite.Evosuite;
import sushi.execution.javac.Javac;
import sushi.execution.jbse.JBSEMethods;
import sushi.execution.jbse.JBSETraces;
import sushi.execution.listpaths.ListPaths;
import sushi.execution.loopend.LoopEnd;
import sushi.execution.loopmgr.LoopMgr;
import sushi.execution.merger.Merger;
import sushi.execution.minimizer.Minimizer;

public class Main {
    /** The logger. */
    private static Logger LOGGER;
    
    /** The logger context. */
    private static LoggerContext LOGGER_CONTEXT;
    
	/** The configuration {@link Options}. */
	private final Options options;
	
	/** A flag that indicates if the global timeout has expired. */
	private boolean timedOut;

	/**
	 * Constructor.
	 * 
	 * @param options the confoguration {@link Options}.
	 */
	public Main(Options options) { 
		this.options = options;
	}
	
	/**
	 * Runs SUSHI.
	 * 
	 * @return An {@code int} exit code, {@code 0} meaning successful exit, {@code 1} meaning 
     *         exit due to an error, {@code 2} meaning exit due to an internal error.
	 */
	public int start() {
		configureLogger();
		LOGGER = LogManager.getFormatterLogger(Main.class);

		try {
			LOGGER.info("This is %s, version %s, \u00a9 2015-2026 %s", getName(), getVersion(), getVendor());

			checkPrerequisites();

			final Tool<?>[] tools;
			final int repeatFrom;
			switch (this.options.getCoverage()) {
			case PATHS:
				tools = new Tool[]{ new JBSEMethods(this.options, true), new Merger(this.options), new ListPaths(this.options), new Javac(this.options), new Evosuite(this.options), new LoopEnd() };
				repeatFrom = -1;
				break;
			case UNSAFE:
				tools = new Tool[]{ new JBSEMethods(this.options, false), new Merger(this.options), new BestPath(this.options), new JBSETraces(this.options), new Javac(this.options), new Evosuite(this.options), new LoopEnd() };
				repeatFrom = -1;
				break;
			case BRANCHES:
				tools = new Tool[]{ new JBSEMethods(this.options, false), new Merger(this.options), new Minimizer(this.options), new JBSETraces(this.options), new Javac(this.options), new Evosuite(this.options), new LoopMgr(this.options) };
				repeatFrom = 2;
				break;
			default:
				LOGGER.error("Unexpected internal error: unexpected value for -cov option");
				return 2;
			}

			final boolean doEverything = (this.options.getPhases() == null);
			makeGlobalTimeoutThread();
			doMainToolsLoop(tools, repeatFrom, doEverything);

			LOGGER.info("%s ends", getName());
			return 0;
		} catch (ToolException e) {
			LOGGER.error(e.getMessage());
			return 1;
		} catch (CheckClasspathException e) {
			LOGGER.error("Failed to find class/method under test in the target application classpath.");
			return 1;
		} catch (NoEvosuiteException e) {
			LOGGER.error("Failed to find the EvoSuite jar file.");
			return 1;
		} catch (NoTmpDirException e) {
			LOGGER.error("Failed to create the temporary directories for SUSHI and JBSE execution.");
			return 1;
		} catch (InternalException e) {
			if (e.getTask() == -1) {
				if (e.getCause() != null) {
					LOGGER.error("Tool %s: aborted with cause: %s.", e.getTool(), e.getCause().toString());
				} else {
					LOGGER.error("Tool %s: aborted with message: %s.", e.getTool(), e.getMessage());
				}
			} else {
				if (e.getCause() == null && e.getMessage() == null) {
					LOGGER.error("Tool %s: task %s aborted with exit status: %s.", e.getTool(), Integer.toString(e.getTask()), Integer.toString(e.getToolExitStatus()));
				} else if (e.getCause() != null) {
					LOGGER.error("Tool %s: task %s aborted with cause: %s.", e.getTool(), Integer.toString(e.getTask()), e.getCause().toString());
				} else {
					LOGGER.error("Tool %s: task %s aborted with message: %s.", e.getTool(), Integer.toString(e.getTask()), e.getMessage());
				}
			}
			return 2;
		} finally {
			shutdownLogger();
		}
	}
	
    /**
     * Configures the logger.
     */
	private void configureLogger() {
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.WARN);

        //appender
        AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE");
        appenderBuilder.addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout");
        layoutBuilder.addAttribute("pattern", "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n");
        appenderBuilder.add(layoutBuilder);
        builder.add(appenderBuilder);

        //root logger
        RootLoggerComponentBuilder rootLoggerBuilder = builder.newRootLogger(this.options.getLogLevel());
        rootLoggerBuilder.add(builder.newAppenderRef("Stdout"));
        builder.add(rootLoggerBuilder);
        
        LOGGER_CONTEXT = Configurator.initialize(builder.build());
	}
    
    private void shutdownLogger() {
    	Configurator.shutdown(LOGGER_CONTEXT);
    }
	
    /**
     * Checks prerequisites.
     * 
     * @throws CheckClasspathException if some required element in the provided
     *         classpath of the program to test does not exist or is not as expected.
     * @throws NoEvosuiteException if the Evosuite jar is not found at the path
     *         specified by them user.
     * @throws NoTmpDirException if some I/O exception occurs while trying to create
     *         the temporary directories.
     */
	private void checkPrerequisites() throws CheckClasspathException, NoEvosuiteException, NoTmpDirException {
		//check classpath: if the class is not found it raises an exception
		final String className = (this.options.getTargetMethod() == null ?
				this.options.getTargetClass() :
				this.options.getTargetMethod().get(0));
		try {
			final ClassLoader ic = getInternalClassloader(this.options);
			ic.loadClass(className.replace('/', '.'));
		} catch (ClassNotFoundException | MalformedURLException | SecurityException e) {
			throw new CheckClasspathException(e);
		}
		
		//checks the presence of EvoSuite
		if (!Files.exists(this.options.getEvosuitePath()) || !Files.isReadable(this.options.getEvosuitePath())) {
			throw new NoEvosuiteException();
		}
		
		//checks the presence of (or creates) the temporary directories 
		possiblyCreateTmpDir(this.options);
	}
	
	/**
	 * Makes and runs a thread that detects when the global
	 * timeout expires.
	 */
	private void makeGlobalTimeoutThread() {
		this.timedOut = false;
		if (this.options.getGlobalBudget() > 0) {
			Thread chrono = new Thread(() -> {
				try {
					Thread.sleep(this.options.getGlobalBudget() * 1000);
				} catch (InterruptedException e) {
					//should never happen, in any case fallthrough
					//should be ok
				}
				setTimedOut();
			});
			chrono.start();
		}
	}
	
	private synchronized void setTimedOut() {
		this.timedOut = true;
	}
	
	private synchronized boolean timedOut() {
		return this.timedOut;
	}
	
	private void doMainToolsLoop(Tool<?>[] tools, int repeatFrom, boolean doEverything) 
	throws InternalException {
		int currentPhase = 1;
		int nextToolIndex = 0;
		int lastRequiredPhase = (doEverything ? -1 : Collections.max(this.options.getPhases()));
		while (true) {
			if (doEverything || this.options.getPhases().contains(currentPhase)) {
				//gets the next tool and submits it to the ExecutionManager
				final Tool<?> tool = tools[nextToolIndex];
				LOGGER.info("Phase %s: executing tool %s.", Integer.toString(currentPhase), tool.getName());
				final ExitStatus[] result;
				try {
					result = ExecutionManager.execute(tool);
				} catch (CoordinatorException | ToolException e) {
					if (e.getMessage() != null) {
						throw new InternalException(tool.getName(), e.getMessage());
					} else {
						throw new InternalException(tool.getName(), e.getCause());
					}
				} catch (WorkerTerminationException e) {
					if (e.getTask() > 0) {
						if (e.getMessage() != null) {
							LOGGER.info("Task %s asked to terminate SUSHI with cause: %s", Integer.toString(e.getTask()), e.getMessage());
						}
					} else {
						if (e.getMessage() != null) {
							LOGGER.info("SUSHI terminated with cause: %s", e.getMessage());
						}
					}
					return;
				} catch (WorkerFailureException e) {
					throw new InternalException(tool.getName(), e.getTask(), e.getCause());
				}
				
				//resets the tool in case it must be executed again later
				tool.reset();
				
				//checks if tool execution went ok
				for (int i = 0; i < result.length; ++i) {
					if (result[i] != null && result[i].getExitStatus() != 0) {
						final List<Integer> tasks;
						try {
							tasks = tool.tasks();
						} catch (Exception e) {
							throw new InternalException(tool.getName(), e);
						}
						final int currentTaskNumber = tasks.get(i / tool.redundance());
						throw new InternalException(tool.getName(), currentTaskNumber, result[i].getExitStatus());
					}
				}
			} else if (currentPhase > lastRequiredPhase) {
				return;
			} else if (timedOut()) {
				LOGGER.info("Global time budget exhausted.");
				return;
			}
			
			//next iteration
			++currentPhase;
			nextToolIndex = (nextToolIndex == tools.length - 1 ? repeatFrom : nextToolIndex + 1);
			
		}
	}

    /**
     * Returns the name of this application, as resulting
     * from the containing jar file.
     * 
     * @return a {@link String} or {@code null} if this 
     *         class is not packaged in a jar file.
     */
    private static String getName() {
        return Main.class.getPackage().getImplementationTitle();
    }

    /**
     * Returns the vendor of this application, as resulting
     * from the containing jar file.
     * 
     * @return a {@link String} or {@code null} if this 
     *         class is not packaged in a jar file.
     */
    private static String getVendor() {
        return Main.class.getPackage().getImplementationVendor();
    }

    /**
     * Returns the version of this application, as resulting
     * from the containing jar file.
     * 
     * @return a {@link String} or {@code null} if this 
     *         class is not packaged in a jar file.
     */
    private static String getVersion() {
        return Main.class.getPackage().getImplementationVersion();
    }

    //Here starts the static part of the class, for managing the command line

	public static void main(String[] args) {
		final Options options = new Options();
		
        //parses options from the command line and exits if the command line
        //is ill-formed
		final CmdLineParser parser = new CmdLineParser(options, ParserProperties.defaults().withUsageWidth(200));
		try {
			parser.parseArgument(processArgs(args));
		} catch (CmdLineException e) {
			System.err.println("Error: " + e.getMessage());
			printUsage(parser);
			System.exit(1);
		}
		
		//does a basic consistency check of the options
		if (!options.isConsistent()) {
			System.err.println("Error: none of -target_class, -target_method, or -params_modifier_class options was specified.");
			printUsage(parser);
			System.exit(1);
		}
		
        //prints help and exits if asked to
		if (options.getHelp()) {
			printUsage(parser);
			System.exit(0);
		}
		
        //invokes the options configurator if present
		if (options.hasOptionsConfigurator()) {
			try {
				configureOptions(options);
			} catch (MalformedURLException e) {
				System.err.println("Error: parameters modifier class home folder " + options.getOptionsConfiguratorPath() + " not found or ill-formed: " + e);
				System.exit(1);
			} catch (ClassNotFoundException e) {
				System.err.println("Error: parameters modifier class " + options.getOptionsConfiguratorClass() + " not found: " + e);
				System.exit(1);
			} catch (ClassCastException e) {
				System.err.println("Error: parameters modifier class " + options.getOptionsConfiguratorClass() + " not a subclass of " + OptionsConfigurator.class.getCanonicalName() + ": " + e);
				return; 
			} catch (InstantiationException e) {
				System.err.println("Error: parameters modifier class " + options.getOptionsConfiguratorClass() + " cannot be instantiated or has no nullary constructor: " + e);
				return; 
			} catch (IllegalAccessException e) {
				System.err.println("Error: parameters modifier class " + options.getOptionsConfiguratorClass() + " or its constructor is not visible: " + e);
				return; 
			}
		}

		//runs
		final Main main = new Main(options);
		final int exitCode = main.start();
		System.exit(exitCode);
	}

    /**
     * Processes the command line arguments so they
     * can be parsed by the command line parser.
     * 
     * @param args the {@link String}{@code []} from the command line.
     * @return a processed {@link String}{@code []}.
     */
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

    /**
     * Prints usage on the standard error.
     * 
     * @param parser a {@link CmdLineParser}.
     */
	private static void printUsage(final CmdLineParser parser) {
		System.err.println("Usage: java " + Main.class.getName() + " <options>");
		System.err.println("where <options> are:");
		parser.printUsage(System.err);
	}
	
    /**
     * Applies a {@link OptionsConfigurator} to an {@link Options} object.
     * 
     * @param options an {@link Options} object. It must contain the information
     *        about the {@link OptionsConfigurator} that will be applied to 
     *        configure it.
     * @throws MalformedURLException 
     * @throws ClassNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    private static void configureOptions(Options options) 
    throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
    	final URL url = options.getOptionsConfiguratorPath().toUri().toURL();
    	@SuppressWarnings("resource")
    	final URLClassLoader loader = new URLClassLoader(new URL[] { url });
    	final Class<? extends OptionsConfigurator> clazz =  
    	loader.loadClass(options.getOptionsConfiguratorClass()).
    	asSubclass(OptionsConfigurator.class);
    	final OptionsConfigurator configurator = clazz.newInstance();
    	configurator.configure(options);
   }
}
