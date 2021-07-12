package sushi.optionhandlers;

import java.util.EnumSet;
import java.util.Locale;

import org.kohsuke.args4j.*;
import org.kohsuke.args4j.spi.FieldSetter;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

/**
 * Parses options into a {@link EnumSet}. It is an abstract class and must be subclassed
 * to inject the specific enum type.
 *
 * <pre><code>
 * enum E { A, B, C };
 * 
 * class Foo {
 *  {@literal @}Option(name="-P",handler={@link AbstractEnumSetOptionHandler}.class)
 *   EnumSet&lt;E&gt; args;
 * }
 * </code></pre>
 *
 * <p>
 * With this, <code>-P A:C </code> parses to EnumSet of size {@code 2}.
 * This option handler can be subtyped if you want to convert values to different types
 * or to handle <code>key=value</code> in other formats, like <code>key:=value</code>.
 * */
public abstract class AbstractEnumSetOptionHandler<E extends Enum<E>> extends OptionHandler<EnumSet<E>> {

	public AbstractEnumSetOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super EnumSet<E>> setter) {
		super(parser, option, setter);
		if (setter.asFieldSetter() == null) {
			throw new IllegalArgumentException("AbstractEnumSetOptionHandler can only work with fields");
		}
	}

	@Override
	public String getDefaultMetaVariable() {
		return null;
	}

	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		FieldSetter fs = this.setter.asFieldSetter();
		@SuppressWarnings("unchecked")
		EnumSet<E> enumSet = (EnumSet<E>) fs.getValue();
		if (enumSet == null) {
			enumSet = createNewEnumSet();
			fs.addValue(enumSet);
		}
		addToEnumSet(params.getParameter(0), enumSet);
		return 1;
	}

	/**
	 * Creates a new instance of the collection.
	 */
	protected abstract EnumSet<E> createNewEnumSet();

	/**
	 * Encapsulates how a single string argument gets converted into key and value.
	 */
	protected void addToEnumSet(String argument, EnumSet<E> enumSet) throws CmdLineException {
		final String[] items = argument.split(":");
		for (String item : items) {
			try {
				enumSet.add(valueOf(item));
			} catch (IllegalArgumentException e) {
				throw new CmdLineException(this.owner, new Localizable() {
					@Override
					public String formatWithLocale(Locale locale, Object... args) {
						return format(args); //no localization, sorry
					}
					
					@Override
					public String format(Object... args) {
						return "Wrong argument for the option " + option.toString() + 
								": Argument is not a semicolon-separated list of enum values.";
					}
				});
			}
		}
	}

	/**
	 * This is the opportunity to convert values to some typed objects.
	 */
	protected abstract E valueOf(String value);

}
