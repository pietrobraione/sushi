package sushi.configure;

import java.util.Locale;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Localizable;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.DelimitedOptionHandler;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

public class PhasesHandler extends DelimitedOptionHandler<Integer> {
	private static final String PHASE_SEPARATOR = ":";
	public PhasesHandler(CmdLineParser parser, OptionDef option, Setter<? super Integer> setter) {
		super(parser, option, setter, PHASE_SEPARATOR, new OneArgumentOptionHandler<Integer>(parser, option, setter) {
			@Override
			protected Integer parse(String argument) throws CmdLineException {
				try {
					return Integer.parseInt(argument);
				} catch (NumberFormatException e) {
					throw new CmdLineException(this.owner, new Localizable() {
						@Override
						public String formatWithLocale(Locale locale, Object... args) {
							return format(args); //no localization, sorry
						}
						
						@Override
						public String format(Object... args) {
							return "Wrong argument for the option " + option.toString() + 
									": Argument is not a semicolon-separated list of integers.";
						}
					});
					
				}
			}
		});
	}
	
	//bug workaround
	@Override
	public String printDefaultValue() {
		try {
			return super.printDefaultValue();
		} catch (NullPointerException e) {
			return null;
		}
	}
}
