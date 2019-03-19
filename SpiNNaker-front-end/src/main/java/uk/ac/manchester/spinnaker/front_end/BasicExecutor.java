/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end;

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
		Tasks collector = new Tasks();
		tasks.forEach(t -> collector
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
		Tasks collector = new Tasks();
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
		executor.awaitTermination(1, TimeUnit.DAYS);
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
		private final List<Future<Exception>> tasks;

		private Tasks() {
			tasks = new ArrayList<>();
		}

		private void add(Future<Exception> task) {
			tasks.add(task);
		}

		/**
		 * Wait for all tasks to finish.
		 *
		 * @throws Exception
		 *             If anything fails. If multiple tasks fail, the exception
		 *             from first to fail is thrown and the other exceptions are
		 *             added as suppressed exceptions.
		 */
		public void awaitAndCombineExceptions() throws Exception {
			// Combine the possibly multiple exceptions into one
			Exception ex = null;
			for (Future<Exception> f : tasks) {
				Exception e = f.get();
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
