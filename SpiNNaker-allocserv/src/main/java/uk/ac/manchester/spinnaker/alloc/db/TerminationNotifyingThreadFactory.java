/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.db;

import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.defaultThreadFactory;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * A thread factory that makes it possible to get a notification when a thread
 * terminates. The termination callback, if set to non-{@code null}, will be run
 * by each thread immediately prior to termination. Otherwise, this factory is
 * just like the factory produced by {@link Executors#defaultThreadFactory()}.
 *
 * @author Donal Fellows
 */
@Component
@Role(ROLE_INFRASTRUCTURE)
@UsedInJavadocOnly(Executors.class)
public class TerminationNotifyingThreadFactory implements ThreadFactory {
	private static final Logger log =
			getLogger(TerminationNotifyingThreadFactory.class);

	private ThreadFactory realThreadFactory;

	private Runnable terminationCallback;

	private UncaughtExceptionHandler exceptionHandler;

	TerminationNotifyingThreadFactory() {
		realThreadFactory = defaultThreadFactory();
		exceptionHandler = (t, ex) -> {
			log.warn("thread '{}' quit unexpectedly", t, ex);
		};
	}

	private synchronized Runnable getTerminationCallback() {
		return terminationCallback;
	}

	/**
	 * @param callback
	 *            An action to carry out when a thread made by this factory
	 *            actually terminates. If {@code null}, remove any current
	 *            termination callback.
	 */
	public synchronized void setTerminationCallback(Runnable callback) {
		terminationCallback = callback;
	}

	private synchronized UncaughtExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	/**
	 * @param callback
	 *            An action to carry out when a thread made by this factory
	 *            terminates with an exception. If {@code null}, remove any
	 *            current termination callback so that the exception vanishes
	 *            silently. (The default handler just logs the exception.)
	 */
	public synchronized void setExceptionHandler(
			UncaughtExceptionHandler callback) {
		exceptionHandler = callback;
	}

	@Override
	public Thread newThread(Runnable r) {
		var t = realThreadFactory.newThread(() -> {
			try {
				r.run();
			} finally {
				var cb = getTerminationCallback();
				if (nonNull(cb)) {
					cb.run();
				}
			}
		});
		t.setUncaughtExceptionHandler((__, ex) -> {
			var eh = getExceptionHandler();
			if (nonNull(eh)) {
				eh.uncaughtException(t, ex);
			}
		});
		return t;
	}
}
