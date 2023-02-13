/*
 * Copyright (c) 2019-2023 The University of Manchester
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

import static java.lang.Boolean.getBoolean;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.utils.progress.ProgressBar;

/**
 * Simple wrapper for progress bars that allows them to be handled in parallel
 * and to be turned on/off by system property.
 *
 * @author Donal Fellows
 * @see ProgressBar
 */
public final class Progress implements AutoCloseable {
	@GuardedBy("this")
	private final ProgressBar bar;

	/**
	 * The name of the system property that controls whether to show progress
	 * bars.
	 */
	public static final String PROGRESS_PROP = "spinnaker.progress_bar";

	private static final boolean DO_PROGRESS = getBoolean(PROGRESS_PROP);

	/**
	 * Create a progress bar that writes to {@code System.out} with the given
	 * number of steps and label. Or don't, if progress bars are disabled by the
	 * system property; if that's the case, this class transparently does
	 * nothing.
	 *
	 * @param count
	 *            The number of steps in the progress bar.
	 * @param label
	 *            The label on the progress bar.
	 */
	public Progress(int count, String label) {
		if (DO_PROGRESS) {
			this.bar = new ProgressBar(count, label);
		} else {
			this.bar = null;
		}
	}

	/**
	 * Advances the progress bar by one step.
	 */
	public synchronized void update() {
		if (bar != null) {
			bar.update();
		}
	}

	/**
	 * Advances the progress bar by several steps.
	 *
	 * @param numSteps
	 *            The number of steps to advance.
	 */
	public synchronized void update(int numSteps) {
		if (bar != null) {
			bar.update(numSteps);
		}
	}

	@Override
	public synchronized void close() {
		if (bar != null) {
			bar.close();
		}
	}
}
