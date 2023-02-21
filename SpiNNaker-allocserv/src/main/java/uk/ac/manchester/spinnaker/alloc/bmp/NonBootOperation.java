/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Non-boot operations that may be performed on a BMP.
 *
 * @author Donal Fellows
 */
@UsedInJavadocOnly(ADCInfo.class)
public enum NonBootOperation {
	// Careful: values must match SQL (CHECK constraints and queries)
	/** Read a blacklist from a board's BMP's flash. */
	READ_BL,
	/** Write a blacklist to a board's BMP's flash. */
	WRITE_BL,
	/** Read the serial numbers from a board's BMP. */
	GET_SERIAL,
	/** Read the {@linkplain ADCInfo temperature data} from a board's BMP. */
	READ_TEMP
}
