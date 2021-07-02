package uk.ac.manchester.spinnaker.alloc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * Validates that a string looks like an IP address.
 *
 * @author Donal Fellows
 */
@Documented
@Retention(RUNTIME)
@Target({
	METHOD, FIELD, PARAMETER, TYPE_USE
})
@Constraint(validatedBy = IPAddress.Validator.class)
public @interface IPAddress {
	/** Message on constraint violated. */
	String message() default "definitely bad IP address";

	/** Group of constraints. Required by validation spec. */
	Class<?>[] groups() default {};

	/** Payload info. Required by validation spec. */
	Class<? extends Payload>[] payload() default {};

	static class Validator implements ConstraintValidator<IPAddress, String> {
		private Pattern pattern;

		@Override
		public void initialize(IPAddress constraintAnnotation) {
			if (pattern == null) {
				pattern = Pattern.compile("^\\d+[.]\\d+[.]\\d+[.]\\d+$");
			}
		}

		@Override
		public boolean isValid(String value,
				ConstraintValidatorContext context) {
			return value != null && pattern.matcher(value).matches();
		}
	}
}
