package sushi.execution.jbse;

import java.util.Set;

import jbse.apps.Formatter;

/**
 * A formatter for symbolic execution.
 * 
 * @author Pietro Braione
 */
interface FormatterSushi extends Formatter {
	void formatStringLiterals(Set<String> stringLiterals);
}
