package sushi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import jbse.apps.settings.SettingsReader;
import sushi.execution.jbse.JBSEParameters;
import sushi.execution.merger.MergerParameters;
import sushi.execution.minimizer.MinimizerParameters;

public class ParametersModifier {
	public void modify(Options p) { }
	
	public void modify(JBSEParameters p)
	throws FileNotFoundException, ParseException, IOException { }
	
	protected final void loadHEXFile(Path path, JBSEParameters p) 
	throws ParseException, IOException {
		final SettingsReader sr;
		try {
			sr = new SettingsReader(path);
		} catch (jbse.apps.settings.ParseException e) {
			throw new ParseException("File " + path.toString() + ": " + e.getMessage());
		}
		sr.fillRunnerParameters(p.getRunnerParameters());
		sr.fillRulesLICS(p.getLICSRulesRepo());
		sr.fillRulesClassInit(p.getClassInitRulesRepo());
	}
	
	public void modify(MergerParameters p) { }

	public void modify(MinimizerParameters p) { }

	public void modify(List<String> p) { }	
}
