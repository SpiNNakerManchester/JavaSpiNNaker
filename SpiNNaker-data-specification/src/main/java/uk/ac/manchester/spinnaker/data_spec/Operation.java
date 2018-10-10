package uk.ac.manchester.spinnaker.data_spec;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate which methods should be used to implement
 * DSE operations. Methods annotated with this <i>must</i> take no arguments,
 * and <i>must</i> have a return type of either {@code void} or {@code int}.
 * The operation calling mechanism treats methods that don't return a value as
 * if they returned zero.
 *
 * @author Donal Fellows
 */
@Retention(RUNTIME)
@Target(METHOD)
@Documented
public @interface Operation {
	/**
	 * Describes what Data Specification operation is implemented by the method
	 * the annotation is on. This <i>must not</i> be {@code null}.
	 *
	 * @return The DSE operation descriptor.
	 */
	Commands value();
}
