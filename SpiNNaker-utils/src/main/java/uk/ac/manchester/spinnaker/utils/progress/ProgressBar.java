/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.utils.progress;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.formatDuration;

import java.io.Closeable;
import java.io.PrintStream;

/**
 * Progress bar for telling the user where a task is up to and for reporting the
 * duration.
 * <p>
 * The timer is started and the header of the bar is written during
 * construction. The dash line is terminated when the last expected update
 * arrives. (or if {@link #close()} is called before that update). The timer is
 * stopped and the duration written when {@link #close()} is called. If
 * {@link #close()} is not called the duration is never written out.
 *
 * @author Christian-B
 */
public class ProgressBar implements Closeable {
	// An int when we need an index
	private static final int MAX_LENGTH_IN_CHARS = 60;

	// A float to force float division
	private static final float MAX_LENGTH = MAX_LENGTH_IN_CHARS;

	// The space between 0% and 50% is the mid-point minus the width of
	// 0% and ~half the width of 50%
	private static final int LEFT_SPACES = MAX_LENGTH_IN_CHARS / 2 - 4;

	// The space between 50% and 100% is the mid-point minus the rest of
	// the width of 50% and the width of 100%
	private static final int RIGHT_SPACES = MAX_LENGTH_IN_CHARS / 2 - 5;

	private static final String DISTANCE_INDICATOR = distanceIndicator();

	private static final char STEP_CHAR = '-';

	private static final char END_CHAR = '|';

	private static final char START_CHAR = '|';

	private final int numberOfThings;

	private final String description;

	private final PrintStream output;

	private final float charsPerThing;

	private int currentlyCompleted = 0;

	private int charsDone = 0;

	private boolean closed = false;

	private final long startTime;

	/**
	 * Creates a progress bar which outputs to the given {@link PrintStream}.
	 *
	 * @param numberOfThings
	 *            The number of items to progress over
	 * @param description
	 *            A text description to add at the start and when reporting
	 *            duration.
	 * @param output
	 *            The stream to write output too. For example
	 *            {@link System#out}.
	 */
	public ProgressBar(int numberOfThings, String description,
			PrintStream output) {
		this.numberOfThings = numberOfThings;
		this.description = description;
		this.output = output;
		charsPerThing = MAX_LENGTH / numberOfThings;
		startTime = currentTimeMillis();
		printHeader();
	}

	/**
	 * Creates a Progress bar which outputs to {@link System#out}.
	 *
	 * @param numberOfThings
	 *            The number of items to progress over
	 * @param description
	 *            A text description to add at the start and when reporting
	 *            duration.
	 */
	public ProgressBar(int numberOfThings, String description) {
		this(numberOfThings, description, out);
	}

	/**
	 * Update the progress bar by a given amount.
	 *
	 * @param amountToAdd
	 *            Amount of things to update by.
	 * @throws IllegalStateException
	 *             Throws if the bar is updated too often or updated after it
	 *             was closed.
	 */
	public void update(int amountToAdd) {
		if ((currentlyCompleted + amountToAdd) > numberOfThings) {
			throw new IllegalStateException("too many update steps!");
		}
		if (isClosed()) {
			throw new IllegalStateException("bar already closed!");
		}
		currentlyCompleted += amountToAdd;
		printProgress((int) (currentlyCompleted * charsPerThing));
	}

	/**
	 * Update the progress bar by a one unit.
	 *
	 * @throws IllegalStateException
	 *             Throws if the bar is updated too often or updated after it
	 *             was closed.
	 */
	public void update() {
		update(1);
	}

	/**
	 * Ends the Progress bar line and prints a duration line.
	 * <p>
	 * If the bar is already closed then invoking this method has no effect.
	 */
	@Override
	public void close() {
		if (isClosed()) {
			return;
		}
		if (charsDone < MAX_LENGTH_IN_CHARS) {
			output.println();
		}
		long duration = currentTimeMillis() - startTime;
		var durationSt = formatDuration(duration);
		if (isNull(description)) {
			output.println("This took " + durationSt);
		} else {
			output.println(description + " took " + durationSt);
		}
		closed = true;
	}

	private void printHeader() {
		if (nonNull(description)) {
			output.println(description);
		}
		output.println(START_CHAR + DISTANCE_INDICATOR + END_CHAR);
		output.print(" ");
	}

	private void printProgress(int expectedCharsDone) {
		for (int i = charsDone; i < expectedCharsDone; i++) {
			output.print(STEP_CHAR);
		}
		charsDone = expectedCharsDone;
		if (charsDone >= MAX_LENGTH_IN_CHARS) {
			output.println();
		}
	}

	private static String distanceIndicator() {
		var builder = new StringBuilder("0%");
		for (int i = 0; i < LEFT_SPACES; i += 1) {
			builder.append(" ");
		}
		builder.append("50%");
		for (int i = 0; i < RIGHT_SPACES; i += 1) {
			builder.append(" ");
		}
		builder.append("100%");
		return builder.toString();
	}

	/**
	 * @return True if the progress bar is closed
	 */
	public boolean isClosed() {
		return closed;
	}

}
