package sushi.optionhandlers;

import java.util.Locale;

import org.apache.logging.log4j.Level;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Localizable;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.DelimitedOptionHandler;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

/**
 * A {@link DelimitedOptionHandler} for lists of method signatures.
 * 
 * @author Pietro Braione
 */
public final class LoggingLevelOptionHandler extends OneArgumentOptionHandler<Level> {
    /**
     * Constructor.
     * 
     * @param parser A {@link CmdLineParser}.
     * @param option An {@link OptionDef}.
     * @param setter A {@link Setter}.
     */
    public LoggingLevelOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Level> setter) {
        super(parser, option, setter);
    }

    @Override
    protected Level parse(String argument) throws CmdLineException {
        switch (argument) {
        case "OFF":
            return Level.OFF;
        case "FATAL":
            return Level.FATAL;
        case "ERROR":
            return Level.ERROR;
        case "WARN":
            return Level.WARN;
        case "INFO":
            return Level.INFO;
        case "DEBUG":
            return Level.DEBUG;
        case "TRACE":
            return Level.TRACE;
        case "ALL":
            return Level.ALL;
        default:
            throw new CmdLineException(this.owner, new Localizable() {
                @Override
                public String formatWithLocale(Locale locale, Object... args) {
                        return format(args); //no localization, sorry
                }
                
                @Override
                public String format(Object... args) {
                        return "Wrong argument for the option " + option.toString() + 
                               ": Argument " + argument + " is not one of OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL.";
                }
            });
        }
    }
}
