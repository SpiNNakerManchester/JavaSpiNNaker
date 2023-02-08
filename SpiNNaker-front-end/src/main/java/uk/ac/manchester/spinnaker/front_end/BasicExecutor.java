/*
 * Copyright (c) 2019 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.front_end;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.DAYS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.concurrent.GuardedBy;

/**
 * A thread pool designed for simple task execution with combining of
 * exceptions.
 *
 * @author Donal Fellows
 */
public class BasicExecutor implements AutoCloseable {
	private final ExecutorService executor;

	/**
	 * Create an instance of the pool.
	 *
	 * @param parallelSize
	 *            The number of threads to use inside the pool.
	 */
	@MustBeClosed
	public BasicExecutor(int parallelSize) {
		executor = newFixedThreadPool(parallelSize);
	}

	/**
	 * Submit some tasks to the pool.
	 *
	 * @param tasks
	 *            The tasks to submit. <em>Should not</em> be a parallel stream.
	 * @return The future holding the results of the execution.
	 */
	public Tasks submitTasks(Stream<SimpleCallable> tasks) {
		var collector = new Tasks();
		tasks.forEach(t -> collector
				.add(executor.submit(() -> collectExceptions(t))));
		return collector;
	}

	/**
	 * Submit some tasks to the pool.
	 *
	 * @param <T>
	 *            The type of the items.
	 * @param items
	 *            The items representing tasks to submit. <em>Should not</em> be
	 *            a parallel stream.
	 * @param taskMapper
	 *            How to convert the item to a task.
	 * @return The future holding the results of the execution.
	 */
	public <T> Tasks submitTasks(Stream<T> items,
			Function<T, SimpleCallable> taskMapper) {
		var collector = new Tasks();
		items.map(taskMapper).forEach(t -> collector
				.add(executor.submit(() -> collectExceptions(t))));
		return collector;
	}

	/**
	 * Submit some tasks to the pool.
	 *
	 * @param <T>
	 *            The type of the items.
	 * @param items
	 *            The items representing tasks to submit.
	 * @param taskMapper
	 *            How to convert the item to a task.
	 * @return The future holding the results of the execution.
	 */
	public <T> Tasks submitTasks(Collection<T> items,
			Function<T, SimpleCallable> taskMapper) {
		var collector = new Tasks();
		items.stream().map(taskMapper).forEach(t -> collector
				.add(executor.submit(() -> collectExceptions(t))));
		return collector;
	}

	/**
	 * Submit some tasks to the pool.
	 *
	 * @param tasks
	 *            The tasks to submit.
	 * @return The future holding the results of the execution.
	 */
	public Tasks submitTasks(Iterable<SimpleCallable> tasks) {
		var collector = new Tasks();
		tasks.forEach(t -> collector
				.add(executor.submit(() -> collectExceptions(t))));
		return collector;
	}

	private Exception collectExceptions(SimpleCallable callable) {
		try {
			callable.call();
			return null;
		} catch (Exception e) {
			return e;
		}
	}

	@Override
	public void close() throws InterruptedException {
		executor.shutdown();
		executor.awaitTermination(1, DAYS);
	}

	/**
	 * The type of task that this executor can run.
	 *
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	public interface SimpleCallable {
		/**
		 * Does an action, or throws an exception if it fails to do so.
		 *
		 * @throws Exception
		 *             If unable to perform the action
		 */
		void call() throws Exception;
	}

	/**
	 * Holds the future results of the submitted tasks.
	 *
	 * @author Donal Fellows
	 */
	public static final class Tasks {
		@GuardedBy("this")
		private List<Future<Exception>> tasks;

		private Tasks() {
			tasks = new ArrayList<>();
		}

		private synchronized void add(Future<Exception> task) {
			if (tasks == null) {
				throw new IllegalStateException("tasks already awaited");
			}
			tasks.add(task);
		}

		/**
		 * Wait for all tasks to finish.
		 *
		 * @throws Exception
		 *             If anything fails. If multiple tasks fail, the exception
		 *             from first to fail is thrown and the other exceptions are
		 *             added as suppressed exceptions; all succeeding tasks are
		 *             still awaited-for.
		 * @throws IllegalStateException
		 *             If the tasks have already been awaited-for.
		 */
		public void awaitAndCombineExceptions() throws Exception {
			// Combine the possibly multiple exceptions into one
			Exception ex = null;
			List<Future<Exception>> tasks;
			synchronized (this) {
				tasks = this.tasks;
				this.tasks = null;
			}
			if (tasks == null) {
				throw new IllegalStateException("tasks already awaited");
			}
			for (var f : tasks) {
				var e = f.get();
				if (e != null) {
					if (ex == null) {
						ex = e;
					} else {
						ex.addSuppressed(e);
					}
				}
			}
			if (ex != null) {
				throw ex;
			}
		}
	}
}
