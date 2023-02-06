/*
 * Copyright (c) 2018-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.data_spec;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.slf4j.Logger;

import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Clever stuff to turn a method annotated with {@link Operation @Operation}
 * into a {@link Callable}.
 *
 * @author Donal Fellows
 */
abstract class OperationMapper {
	private static final Logger log = getLogger(OperationMapper.class);

	/**
	 * Disable strict validation if {@code true}.
	 *
	 * @deprecated For testing only.
	 */
	@Deprecated
	static boolean looseValidation;

	/** The types we allow as return values: {@code int} and {@code void}. */
	private static final Set<Class<?>> ALLOWED_RETURN_TYPES =
			Set.of(Void.TYPE, Integer.TYPE, Integer.class);

	/**
	 * Manufactures wrapped methods for a function API class. Methods are only
	 * wrapped if they are annotated with {@code @}{@link Operation}.
	 */
	private static final class OpsMapMaker
			extends ClassValue<Map<Commands, WrappedMethod>> {
		@Override
		protected Map<Commands, WrappedMethod> computeValue(Class<?> type) {
			return stream(type.getMethods()).filter(this::validOperation)
					.collect(toUnmodifiableMap(this::getCommands,
							m -> wrapMethod(type, m)));
		}

		private boolean validOperation(Method m) {
			if (!m.isAnnotationPresent(Operation.class)) {
				return false;
			}
			// Validate the constraints imposed by Operation
			if (m.getParameterCount() != 0 && !looseValidation) {
				log.error("method {} must not take parameters "
						+ "when annotated with Operation", m);
				return false;
			}
			if (!ALLOWED_RETURN_TYPES.contains(m.getReturnType())
					&& !looseValidation) {
				log.error("method {} must return int or void "
						+ "when annotated with Operation", m);
				return false;
			}
			for (var et : m.getExceptionTypes()) {
				if (!Exception.class.isAssignableFrom(et)) {
					continue;
				}
				if (RuntimeException.class.isAssignableFrom(et)) {
					continue;
				}
				if (!DataSpecificationException.class.isAssignableFrom(et)
						&& !looseValidation) {
					// Only complain about the first one
					log.error("method {} throws checked exception "
							+ "that is not a {}; may result in runtime faults",
							m, et, DataSpecificationException.class);
					break;
				}
			}
			return true;
		}

		private Commands getCommands(Method m) {
			return m.getAnnotation(Operation.class).value();
		}
	}

	/** Cache of what methods implement operations in a class. */
	private static final OpsMapMaker OPS_MAP_MAKER = new OpsMapMaker();

	/**
	 * Cache of callables for a particular operation on a particular executor.
	 * This is a <em>weak</em> map; entries are reclaimed when the instance
	 * isn't used elsewhere.
	 */
	private static final Map<FunctionAPI, Map<Commands, Callable>> MAP =
			synchronizedMap(new WeakHashMap<>());

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
	 * @return How to invoke that operation, or {@code null} if that operation
	 *         has no registered implementation.
	 */
	@CheckReturnValue
	static Callable getOperationImpl(FunctionAPI funcs, Commands opcode) {
		// Note that MAP is using the object identity; this is by design
		var map = MAP.computeIfAbsent(requireNonNull(funcs,
				"can only look up method implementations of real objects"),
				/*
				 * Careful! Must only pass in references to funcs as a weak so
				 * that it doesn't compromise the weak hash map. But we can use
				 * the same weak reference for all the method wrappers that we
				 * create.
				 */
				__ -> manufactureCallables(new WeakReference<>(funcs)));
		return map.get(opcode);
	}

	/**
	 * Manufacture callables for a particular function API instance.
	 *
	 * @param objref
	 *            The reference to the function API instance.
	 * @return The callables for the commands.
	 */
	private static Map<Commands, Callable> manufactureCallables(
			WeakReference<FunctionAPI> objref) {
		var ops = OPS_MAP_MAKER.get(objref.get().getClass());
		return ops.entrySet().stream().collect(toMap(Map.Entry::getKey,
				e -> cmd -> e.getValue().call(objref, e.getKey(), cmd)));
	}

	/**
	 * Check a particular method for validity and wrap it. If there are any
	 * arguments, or the method has a return type that isn't {@code void} or
	 * {@code int}, that's an error.
	 *
	 * @param cls
	 *            The class, for the exception message.
	 * @param m
	 *            The (annotated) method to check.
	 * @return The checked method, wrapped.
	 */
	private static WrappedMethod wrapMethod(Class<?> cls, Method m) {
		if (m.getParameterCount() != 0
				|| !ALLOWED_RETURN_TYPES.contains(m.getReturnType())) {
			throw new IllegalArgumentException(
					format("bad Operation annotation on method %s of %s",
							m.getName(), cls));
		}

		var rt = m.getReturnType();
		if (rt.equals(Integer.TYPE) || rt.equals(Integer.class)) {
			return obj -> (Integer) m.invoke(obj);
		}
		// Synthesise a return value
		return obj -> {
			m.invoke(obj);
			return 0;
		};
	}

	/**
	 * A wrapper around {@link Method} of a {@link FunctionAPI} subclass.
	 */
	@FunctionalInterface
	private interface WrappedMethod {
		/**
		 * How to actually call the wrapped method.
		 *
		 * @param funcs
		 *            The instance to call the method on.
		 * @return The integer result.
		 * @throws InvocationTargetException
		 *             If the method throws
		 * @throws IllegalAccessException
		 *             If the method can't be accessed.
		 */
		int operation(FunctionAPI funcs)
				throws InvocationTargetException, IllegalAccessException;

		/**
		 * Ugly stuff to wrap {@link WrappedMethod} as {@link Callable}. The
		 * truly nasty part is the handling of exceptions, which have to be
		 * unwrapped from the dynamic method calling machinery.
		 *
		 * @param objref
		 *            Reference to target object.
		 * @param command
		 *            What command this operation is.
		 * @param encodedOpcode
		 *            The actual operation code from the DSE bytecode.
		 * @return The result of the operation
		 * @throws DataSpecificationException
		 *             If the operation fails.
		 */
		default int call(WeakReference<FunctionAPI> objref, Commands command,
				int encodedOpcode) throws DataSpecificationException {
			var funcs = objref.get();
			requireNonNull(funcs, "unexpectedly early deallocation");
			funcs.unpack(encodedOpcode);
			if (log.isDebugEnabled()) {
				log.debug("EXEC: {} ({})", command,
						format("%08x", encodedOpcode));
			}
			try {
				try {
					return operation(funcs);
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
}
