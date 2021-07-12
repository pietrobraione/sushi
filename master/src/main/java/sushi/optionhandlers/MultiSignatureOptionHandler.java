package sushi.optionhandlers;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Localizable;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.DelimitedOptionHandler;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

/**
 * A {@link DelimitedOptionHandler} for lists of method signatures.
 * 
 * @author Pietro Braione
 */
public final class MultiSignatureOptionHandler extends DelimitedOptionHandler<List<String>> {
    /**
     * Character used as separator within a signature.
     */
    private static final String SIGNATURE_SEPARATOR = ":";
    
    /**
     * Character used to separate different signatures.
     */
    private static final String LIST_SEPARATOR = "!";
    
    /**
     * Constructor.
     * 
     * @param parser A {@link CmdLineParser}.
     * @param option An {@link OptionDef}.
     * @param setter A {@link Setter}.
     */
    public MultiSignatureOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super List<String>> setter) {
        super(parser, option, setter, LIST_SEPARATOR, new OneArgumentOptionHandler<List<String>>(parser, option, setter) {
            @Override
            protected List<String> parse(String argument) {
                return Arrays.asList(argument.split(SIGNATURE_SEPARATOR));
            }
        });
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        final String[] list = params.getParameter(0).split(LIST_SEPARATOR);
        for (String p : list) {
            if (p.split(SIGNATURE_SEPARATOR).length != 3) {
                throw new CmdLineException(this.owner, new Localizable() {
                    @Override
                    public String formatWithLocale(Locale locale, Object... args) {
                        return format(args);
                    }

                    @Override
                    public String format(Object... args) {
                        return "Wrong argument for the option " + option.toString() + 
                        ": On of the arguments in the list has not shape <class_name>" + SIGNATURE_SEPARATOR + "<params_types>" + SIGNATURE_SEPARATOR + "<method_name>.";
                    }
                });
            }
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
