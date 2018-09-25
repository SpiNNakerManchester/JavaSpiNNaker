package uk.ac.manchester.spinnaker.data_spec;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;

/**
 * Clever stuff to turn a method annotated with {@link Operation} into a
 * {@link Callable}.
 *
 * @author Donal Fellows
 */
abstract class OperationMapper {
	private static final Logger log = getLogger(OperationMapper.class);
	private static final Map<FunctionAPI, Map<Commands, Callable>> MAP =
			new WeakHashMap<>();

	private OperationMapper() {
	}

	/**
	 * Given an object that has (or should have) {@link Operation}-annotated
	 * methods, and given a {@link Commands} that we want to invoke, get the
	 * implementation of how to invoke the operation that is done by calling
	 * first {@link FunctionAPI#unpack(int) unpack(...)} and then the annotated
	 * method.
	 *
	 * @param funcs
	 *            The object containing the method implementations.
	 * @param opcode
	 *            The opcode that we want to invoke.
	 * @return How to invoke that operation, or <tt>null</tt> if that operation
	 *         has no registered implementation.
	 */
	static Callable getOperationImpl(FunctionAPI funcs, Commands opcode) {
		// Note that MAP is using the object identity; this is by design
		Map<Commands, Callable> map = MAP.get(requireNonNull(funcs,
				"can only look up method implementations of real objects"));
		if (map == null) {
			map = new HashMap<>();
			MAP.put(funcs, map);

			/*
			 * Careful! Must only pass in references to funcs as a weak so that
			 * it doesn't compromise the weak hash map. But we can use the same
			 * weak reference for all the method wrappers that we create.
			 */
			WeakReference<FunctionAPI> objref = new WeakReference<>(funcs);

			for (Method m : funcs.getClass().getMethods()) {
				// Skip methods without the annotation. They're no problem.
				if (!m.isAnnotationPresent(Operation.class)) {
					continue;
				}
				Commands c = m.getAnnotation(Operation.class).value();

				/*
				 * If there are any arguments, or the method has a return type
				 * that isn't void or int, or the annotation value is null,
				 * that's an error.
				 */
				if (m.getParameterCount() != 0
						|| !(m.getReturnType().equals(Void.TYPE)
								|| m.getReturnType().equals(Integer.TYPE))
						|| c == null) {
					throw new IllegalArgumentException(format(
							"bad Operation annotation on method %s of %s",
							m.getName(), funcs));
				}

				map.put(c, cmd -> doCall(objref.get(), m, c, cmd));
			}
		}
		return map.get(opcode);
	}

	/**
	 * Ugly stuff to wrap methods as {@link Callable}. The truly nasty part is
	 * the handling of exceptions, which have to be unwrapped from the dynamic
	 * method calling machinery.
	 */
	private static int doCall(FunctionAPI funcs, Method method,
			Commands command, int encodedOpcode)
			throws DataSpecificationException {
		requireNonNull(funcs, "unexpectedly early deallocation");
		funcs.unpack(encodedOpcode);
		log.debug(format("EXEC: %s (%08x)", command, encodedOpcode));

		try {
			try {
				if (method.getReturnType().equals(Void.TYPE)) {
					method.invoke(funcs);
					return 0;
				} else {
					return (int) method.invoke(funcs);
				}
			} catch (InvocationTargetException innerException) {
				// Unwrap the inner exception
				throw innerException.getTargetException();
			}
		} catch (RuntimeException | Error
				| DataSpecificationException realException) {
			// These are the real things that can be thrown
			throw realException;
		} catch (Throwable badException) {
			// Should be unreachable
			throw new RuntimeException("bad call", badException);
		}
	}
}
