package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A structure that has an equivalent in a structure in SARK (or SCAMP, or the
 * BMP).
 *
 * @author Donal Fellows
 */
@Documented
@Retention(SOURCE)
@Target(TYPE)
public @interface SARKStruct {
	/**
	 * The name of the struct in SARK.
	 */
	String value();

	/**
	 * Which API contains the structure.
	 */
	API api() default API.SARK;

	/**
	 * The supported APIs for mapped structures.
	 *
	 * @author Donal Fellows
	 */
	public enum API {
		SARK, SCAMP, SPIN1API, BMP
	}
}
