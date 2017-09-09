package sushi.configure;

import java.util.Locale;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Localizable;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.DelimitedOptionHandler;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class SignatureHandler extends DelimitedOptionHandler<String> {
	private static final String SIGNATURE_SEPARATOR = ":";
	public SignatureHandler(CmdLineParser parser, OptionDef option, Setter<? super String> setter) {
		super(parser, option, setter, SIGNATURE_SEPARATOR, new OneArgumentOptionHandler<String>(parser, option, setter) {
			@Override
			protected String parse(String argument) {
				return argument;
			}
		});
	}
	
	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		if (params.getParameter(0).split(SIGNATURE_SEPARATOR).length != 3) {
			throw new CmdLineException(this.owner, new Localizable() {
				
				@Override
				public String formatWithLocale(Locale locale, Object... args) {
					return format(args);
				}
				
				@Override
				public String format(Object... args) {
					return "Wrong argument for the option " + option.toString() + 
							": Argument has not shape <class_name>" + SIGNATURE_SEPARATOR + "<params_types>" + SIGNATURE_SEPARATOR + "<method_name>.";
				}
			});
		}
		return super.parseArguments(params);
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
