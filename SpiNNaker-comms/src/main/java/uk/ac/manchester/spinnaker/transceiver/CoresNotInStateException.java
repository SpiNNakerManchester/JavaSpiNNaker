/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Float.POSITIVE_INFINITY;
import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.util.EnumSet;

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

	CoresNotInStateException(Integer timeout, EnumSet<CPUState> expectedStates,
			CoreSubsets failedCores) {
		this(convertTimeout(timeout), expectedStates, failedCores);
	}

	private static float convertTimeout(Integer timeout) {
		if (timeout == null) {
			return POSITIVE_INFINITY;
		}
		return timeout / (float) MSEC_PER_SEC;
	}

	CoresNotInStateException(float timeout, EnumSet<CPUState> expectedStates,
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
