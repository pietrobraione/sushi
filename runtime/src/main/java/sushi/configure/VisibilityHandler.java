package sushi.configure;

import java.util.Locale;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Localizable;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

public class VisibilityHandler extends OneArgumentOptionHandler<Visibility> {

	public VisibilityHandler(final CmdLineParser parser, final OptionDef option, final Setter<? super Visibility> setter) {
		super(parser, option, setter);
	}

	@Override
	protected Visibility parse(String argument) throws NumberFormatException, CmdLineException {
		if (argument.equalsIgnoreCase("public")) {
			return Visibility.PUBLIC;
		} else if (argument.equalsIgnoreCase("package")) {
			return Visibility.PACKAGE;
		}

		throw new CmdLineException(this.owner, new Localizable() {
			@Override
			public String formatWithLocale(Locale locale, Object... args) {
				return format(args); //no localization, sorry
			}
			
			@Override
			public String format(Object... args) {
				return "Wrong argument for the option " + option.toString() + 
						": Argument is not one of PUBLIC, PACKAGE.";
			}
		});
	}

}
