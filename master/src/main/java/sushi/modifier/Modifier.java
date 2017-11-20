package sushi.modifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import sushi.configure.JBSEParameters;
import sushi.configure.MergerParameters;
import sushi.configure.MinimizerParameters;
import sushi.configure.Options;
import sushi.configure.ParametersModifier;
import sushi.configure.ParseException;
import sushi.execution.jbse.JBSEMethods;
import sushi.logging.Logger;

public class Modifier {
	private static final Logger logger = new Logger(JBSEMethods.class);

	private static Modifier instance = null;

	public static Modifier I() {
		if (instance == null) {
			instance = new Modifier();
		}
		return instance;
	}

	private ParametersModifier modi;

	private Modifier() {
		final Options options = Options.I();
		if (options.getParametersModifierClassname() == null) {
			//no modifier
			this.modi = new ParametersModifier();
			return; 
		}
		final URL url;
		try {
			url = options.getParametersModifierPath().toUri().toURL();
		} catch (MalformedURLException e) {
			logger.error("Parameters modifier class home folder " + options.getParametersModifierPath() + " not found", e);
			this.modi = new ParametersModifier();
			return; 
		}
		try {
			@SuppressWarnings("resource")
			final URLClassLoader loader = new URLClassLoader(new URL[] { url });
			final Class<? extends ParametersModifier> clazz =
					loader.loadClass(options.getParametersModifierClassname()).
					asSubclass(ParametersModifier.class);
			this.modi = clazz.newInstance();
		} catch (ClassNotFoundException e) {
			logger.error("Parameters modifier class " + options.getParametersModifierClassname() + " not found", e);
			this.modi = new ParametersModifier();
			return; 
		} catch (ClassCastException e) {
			logger.error("Parameters modifier class " + options.getParametersModifierClassname() + " not a subclass of " + ParametersModifier.class.getCanonicalName(), e);
			this.modi = new ParametersModifier();
			return; 
		} catch (InstantiationException e) {
			logger.error("Parameters modifier class " + options.getParametersModifierClassname() + " cannot be instantiated or has no nullary constructor", e);
			this.modi = new ParametersModifier();
			return; 
		} catch (IllegalAccessException e) {
			logger.error("Parameters modifier class " + options.getParametersModifierClassname() + " constructor is not visible", e);
			this.modi = new ParametersModifier();
			return; 
		}
	}

	public void modify(Options p) { 
		this.modi.modify(p);
	}

	public void modify(JBSEParameters p)
			throws FileNotFoundException, ParseException, IOException {
		this.modi.modify(p);
	}

	public void modify(MergerParameters p) {
		this.modi.modify(p);
	}

	public void modify(MinimizerParameters p) { 
		this.modi.modify(p);
	}

	public void modify(List<String> p) { 
		this.modi.modify(p);
	}
}
