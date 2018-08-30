package uk.ac.manchester.spinnaker.data_spec;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;

abstract class OperationCallableMapper {
	private OperationCallableMapper() {
	}

	private static final Map<Object, Map<Commands, OperationCallable>> MAP =
			new WeakHashMap<>();

	static OperationCallable getOperationImpl(FunctionAPI obj,
			Commands opcode) {
		Map<Commands, OperationCallable> map = MAP.get(obj);
		if (map == null) {
			map = new HashMap<>();
			MAP.put(obj, map);
			WeakReference<FunctionAPI> o = new WeakReference<>(obj);
			for (Method m : obj.getClass().getMethods()) {
				if (!m.isAnnotationPresent(Operation.class)
						|| m.getParameterCount() != 0) {
					continue;
				}
				Commands c = m.getAnnotation(Operation.class).value();
				if (m.getReturnType().equals(Void.TYPE)) {
					map.put(c, cmd -> {
						o.get().unpack(cmd);
						System.out.println("EXEC: " + c + " (" + cmd + ")");
						try {
							m.invoke(o.get());
							return 0;
						} catch (IllegalAccessException
								| IllegalArgumentException e) {
							// Should be unreachable
							throw new RuntimeException("bad call", e);
						} catch (InvocationTargetException e) {
							try {
								throw e.getTargetException();
							} catch (RuntimeException
									| DataSpecificationException real) {
								throw real;
							} catch (Throwable impossible) {
								// Should be unreachable
								throw new RuntimeException("bad call",
										impossible);
							}
						}
					});
				} else {
					map.put(c, cmd -> {
						o.get().unpack(cmd);
						System.out.println("EXEC: " + c + " (" + cmd + ")");
						try {
							return (int) m.invoke(o.get());
						} catch (IllegalAccessException
								| IllegalArgumentException e) {
							// Should be unreachable
							throw new RuntimeException("bad call", e);
						} catch (InvocationTargetException e) {
							try {
								throw e.getTargetException();
							} catch (RuntimeException
									| DataSpecificationException real) {
								throw real;
							} catch (Throwable impossible) {
								// Should be unreachable
								throw new RuntimeException("bad call",
										impossible);
							}
						}
					});
				}
			}
		}
		return map.get(opcode);
	}
}
