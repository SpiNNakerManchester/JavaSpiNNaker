package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A field that has an equivalent in a structure in SARK (or SCAMP, or the BMP).
 *
 * @author Donal Fellows
 */
@Documented
@Retention(SOURCE)
@Target(FIELD)
public @interface SARKField {
	/**
	 * @return The name of the field in the equivalent structure.
	 */
	String value();
}
