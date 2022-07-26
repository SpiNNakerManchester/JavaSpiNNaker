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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Float.POSITIVE_INFINITY;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.util.Set;

import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.model.CPUState;

/**
 * Cores failed to reach a given state within a timeout.
 *
 * @author Donal Fellows
 */
public class CoresNotInStateException extends SpinnmanException {
	private static final long serialVersionUID = 1790369744408178478L;

	private static final String OP_TMPL =
			"waiting for cores to reach one of %s";

	private static final String TMPL =
			"operation '" + OP_TMPL + "' timed out after %f seconds";

	/** What the timeout was. */
	private float timeout;

	/** What operation was being done. */
	private String operation;

	/** Which cores have failed. */
	private CoreSubsets failedCores;

	CoresNotInStateException(Integer timeout, Set<CPUState> expectedStates,
			CoreSubsets failedCores) {
		this(convertTimeout(timeout), expectedStates, failedCores);
	}

	private static float convertTimeout(Integer timeout) {
		if (isNull(timeout)) {
			return POSITIVE_INFINITY;
		}
		return timeout / (float) MSEC_PER_SEC;
	}

	CoresNotInStateException(float timeout, Set<CPUState> expectedStates,
			CoreSubsets failedCores) {
		super(format(TMPL, expectedStates, timeout));
		this.operation = format(OP_TMPL, timeout);
		this.timeout = timeout;
		this.failedCores = failedCores;
	}

	/**
	 * @return How long did we wait for the cores to enter the desired state?
	 */
	public float getTimeout() {
		return timeout;
	}

	/** @return What were we attempting to do when the failure happened? */
	public String getOperation() {
		return operation;
	}

	/** @return What cores have failed? */
	public CoreSubsets getFailedCores() {
		return failedCores;
	}
}
