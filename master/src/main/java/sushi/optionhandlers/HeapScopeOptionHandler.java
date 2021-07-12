package sushi.optionhandlers;

import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.MapOptionHandler;
import org.kohsuke.args4j.spi.Setter;

public class HeapScopeOptionHandler extends MapOptionHandler {
	private static final String PAIR_SEPARATOR = ":";
	public HeapScopeOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Map<?, ?>> setter) {
		super(parser, option, setter);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected void addToMap(String argument, Map m) throws CmdLineException {
		final String[] pairs = argument.split(PAIR_SEPARATOR);
		for (String pair : pairs) {
			super.addToMap(pair, m);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void addToMap(Map m, String key, String value) {
		m.put(key, Integer.parseInt(value));
	}
}
