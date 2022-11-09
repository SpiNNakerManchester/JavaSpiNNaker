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
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
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

	/** Cache of what methods implement operations in a class. */
	private static final Map<Class<? extends FunctionAPI>,
			Map<Commands, Method>> OPS_MAP = synchronizedMap(new HashMap<>());

	/**
	 * Cache of callables for a particular operation on a particular executor.
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
				__ -> manufactureCallables(new WeakReference<>(funcs),
						OPS_MAP.computeIfAbsent(funcs.getClass(),
								OperationMapper::getOperations)));
		return map.get(opcode);
	}

	private static Map<Commands, Callable> manufactureCallables(
			WeakReference<FunctionAPI> objref, Map<Commands, Method> ops) {
		return ops.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> {
			var c = e.getKey();
			var m = e.getValue();
			// Note that validateOperationMethod() below ensures safety of this
			if (m.getReturnType().equals(Void.TYPE)) {
				return cmd -> doVoidCall(objref.get(), m, c, cmd);
			} else {
				return cmd -> doIntCall(objref.get(), m, c, cmd);
			}
		}));
	}

	private static Map<Commands, Method> getOperations(Class<?> cls) {
		return stream(cls.getMethods())
				.filter(m -> m.isAnnotationPresent(Operation.class))
				.collect(toMap(m -> m.getAnnotation(Operation.class).value(),
						m -> validateOperationMethod(cls, m)));
	}

	private static final Set<Class<?>> ALLOWED_RETURN_TYPES =
			Set.of(Void.TYPE, Integer.TYPE);

	/**
	 * If there are any arguments, or the method has a return type that isn't
	 * {@code void} or {@code int}, that's an error.
	 *
	 * @param cls
	 *            The class, for the exception message.
	 * @param m
	 *            The (annotated) method to check.
	 * @return The checked method.
	 */
	private static Method validateOperationMethod(Class<?> cls, Method m) {
		if (m.getParameterCount() != 0
				|| !ALLOWED_RETURN_TYPES.contains(m.getReturnType())) {
			throw new IllegalArgumentException(
					format("bad Operation annotation on method %s of %s",
							m.getName(), cls));
		}
		return m;
	}

	private static void preCall(FunctionAPI funcs, Commands command,
			int encodedOpcode) {
		requireNonNull(funcs, "unexpectedly early deallocation");
		funcs.unpack(encodedOpcode);
		if (log.isDebugEnabled()) {
			log.debug("EXEC: {} ({})", command, format("%08x", encodedOpcode));
		}
	}

	/**
	 * Ugly stuff to wrap methods as {@link Callable}. The truly nasty part is
	 * the handling of exceptions, which have to be unwrapped from the dynamic
	 * method calling machinery.
	 */
	private static int doIntCall(FunctionAPI funcs, Method method,
			Commands command, int encodedOpcode)
			throws DataSpecificationException {
		preCall(funcs, command, encodedOpcode);
		try {
			try {
				return (int) method.invoke(funcs);
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

	/**
	 * Ugly stuff to wrap methods as {@link Callable}. The truly nasty part is
	 * the handling of exceptions, which have to be unwrapped from the dynamic
	 * method calling machinery.
	 */
	private static int doVoidCall(FunctionAPI funcs, Method method,
			Commands command, int encodedOpcode)
			throws DataSpecificationException {
		preCall(funcs, command, encodedOpcode);
		try {
			try {
				method.invoke(funcs);
				return 0;
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
