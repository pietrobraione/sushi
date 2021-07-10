package sushi.optionhandlers;

import java.nio.file.Path;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.MultiPathOptionHandler;
import org.kohsuke.args4j.spi.Setter;

public class MultiPathOptionHandlerPatched extends MultiPathOptionHandler {
    public MultiPathOptionHandlerPatched(CmdLineParser parser, OptionDef option, Setter<Path> setter) {
        super(parser, option, setter);
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
