/*
 * Copyright (c) 2014 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.nmpiexec.jobprocess;

import static java.util.Objects.isNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import uk.ac.manchester.spinnaker.nmpi.model.job.JobParameters;

/**
 * A factory for creating {@link JobProcess} instances given a
 * {@link JobParameters} instance.
 */
public class JobProcessFactory {

	/**
	 * The thread group of the factory.
	 */
	private final ThreadGroup threadGroup;

	/**
	 * Create a factory.
	 *
	 * @param threadGroupParam
	 *            The thread group for the factory. All threads created by the
	 *            factory will be within this group.
	 */
	public JobProcessFactory(final ThreadGroup threadGroupParam) {
		this.threadGroup = threadGroupParam;
	}

	/**
	 * Create a factory.
	 *
	 * @param threadGroupName
	 *            The name of the thread group for the factory. All threads
	 *            created by the factory will be within this group.
	 */
	public JobProcessFactory(final String threadGroupName) {
		this(new ThreadGroup(threadGroupName));
	}

	/**
	 * A version of {@link Supplier} that builds a job process. Required to work
	 * around type inference rules.
	 *
	 * @param <P>
	 *            The type of job parameters handled by the job process this
	 *            supplier produces.
	 */
	@FunctionalInterface
	public interface ProcessSupplier<P extends JobParameters>
			extends Supplier<JobProcess<P>> {
	}

	/**
	 * A map between parameter types and process suppliers. Note that the type
	 * is guaranteed by the {@link #addMapping(Class,ProcessSupplier)} method,
	 * which is the only place that this map should be modified.
	 */
	private final Map<Class<? extends JobParameters>,
			ProcessSupplier<? extends JobParameters>> typeMap = new HashMap<>();

	/**
	 * Adds a new type mapping.
	 *
	 * @param <P>
	 *            The type of parameters that this mapping will handle.
	 * @param parameterType
	 *            The job parameter type
	 * @param processSupplier
	 *            The job process supplier
	 */
	public <P extends JobParameters> void addMapping(
			final Class<P> parameterType,
			final ProcessSupplier<P> processSupplier) {
		typeMap.put(parameterType, processSupplier);
	}

	/**
	 * Get the types of parameters supported.
	 *
	 * @return A collection of types of parameters
	 */
	public Collection<Class<? extends JobParameters>> getParameterTypes() {
		return typeMap.keySet();
	}

	/**
	 * Creates a {@link JobProcess} given a {@link JobParameters} instance.
	 *
	 * @param <P>
	 *            The type of the job parameters.
	 * @param parameters
	 *            The parameters of the job
	 * @return A JobProcess matching the parameters
	 * @throws IllegalArgumentException
	 *             If the type of the job parameters is unexpected.
	 */
	public <P extends JobParameters> JobProcess<P> createProcess(
			final P parameters) {
		/*
		 * We know that this is of the correct type, because the addMapping
		 * method will only allow the correct type mapping in
		 */
		@SuppressWarnings("unchecked")
		final var supplier =
				(ProcessSupplier<P>) typeMap.get(parameters.getClass());
		if (isNull(supplier)) {
			throw new IllegalArgumentException(
					"unsupported job parameter type: " + parameters.getClass());
		}

		final var process = supplier.get();
		// Magically set the thread group if there is one
		setField(process, "threadGroup", threadGroup);
		return process;
	}

	/**
	 * Set a static field in an object.
	 *
	 * @param clazz The class of the object
	 * @param fieldName The name of the field to set
	 * @param value The value to set the field to
	 */
	@SuppressWarnings("unused")
	private static void setField(final Class<?> clazz, final String fieldName,
			final Object value) {
		try {
			final var threadGroupField = clazz.getDeclaredField(fieldName);
			threadGroupField.setAccessible(true);
			threadGroupField.set(null, value);
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			// Treat any exception as just a simple refusal to set the field.
		}
	}

	/**
	 * Set a field in an instance.
	 *
	 * @param instance The instance
	 * @param fieldName The name of the field
	 * @param value The value to set
	 */
	private static void setField(final Object instance, final String fieldName,
			final Object value) {
		try {
			final var threadGroupField =
					instance.getClass().getDeclaredField(fieldName);
			threadGroupField.setAccessible(true);
			threadGroupField.set(instance, value);
		} catch (NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException e) {
			// Treat any exception as just a simple refusal to set the field.
		}
	}
}
