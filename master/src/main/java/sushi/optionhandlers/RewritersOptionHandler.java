package sushi.optionhandlers;

import java.util.EnumSet;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.Setter;

import sushi.Rewriter;

public final class RewritersOptionHandler extends AbstractEnumSetOptionHandler<Rewriter> {
	public RewritersOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super EnumSet<Rewriter>> setter) {
		super(parser, option, setter);
	}

	@Override
	protected EnumSet<Rewriter> createNewEnumSet() {
		return EnumSet.noneOf(Rewriter.class);
	}

	@Override
	protected Rewriter valueOf(String value) {
		return Rewriter.valueOf(value);
	}
}
