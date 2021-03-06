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

import uk.ac.manchester.spinnaker.utils.progress.ProgressBar;

/**
 * Simple wrapper for progress bars that allows them to be handled in parallel
 * and to be turned on/off by system property.
 *
 * @author Donal Fellows
 * @see ProgressBar
 */
public final class Progress implements AutoCloseable {
	private final ProgressBar bar;

	/**
	 * The name of the system property that controls whether to show progress
	 * bars.
	 */
	public static final String PROGRESS_PROP = "spinnaker.progress_bar";

	private static final boolean DO_PROGRESS =
			Boolean.getBoolean(PROGRESS_PROP);

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
	public void update() {
		if (bar != null) {
			synchronized (bar) {
				bar.update();
			}
		}
	}

	/**
	 * Advances the progress bar by several steps.
	 *
	 * @param numSteps
	 *            The number of steps to advance.
	 */
	public void update(int numSteps) {
		if (bar != null) {
			synchronized (bar) {
				bar.update(numSteps);
			}
		}
	}

	@Override
	public void close() {
		if (bar != null) {
			synchronized (bar) {
				bar.close();
			}
		}
	}
}
