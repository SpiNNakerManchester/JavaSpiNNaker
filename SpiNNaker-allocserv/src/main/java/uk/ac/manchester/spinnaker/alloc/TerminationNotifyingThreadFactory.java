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
package uk.ac.manchester.spinnaker.alloc;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.stereotype.Component;

/**
 * A thread factory that makes it possible to get a notification when a thread
 * terminates. The termination callback, if set to non-{@code null}, will be run
 * by each thread immediately prior to termination. Otherwise, this factory is
 * just like the factory produced by {@link Executors#defaultThreadFactory()}.
 *
 * @author Donal Fellows
 */
@Component
public class TerminationNotifyingThreadFactory implements ThreadFactory {
	private ThreadFactory realThreadFactory;

	private Runnable terminationCallback;

	public TerminationNotifyingThreadFactory() {
		realThreadFactory = Executors.defaultThreadFactory();
	}

	public void setTerminationCallback(Runnable callback) {
		terminationCallback = callback;
	}

	@Override
	public Thread newThread(Runnable r) {
		return realThreadFactory.newThread(() -> {
			try {
				r.run();
			} finally {
				if (terminationCallback != null) {
					terminationCallback.run();
				}
			}
		});
	}
}
