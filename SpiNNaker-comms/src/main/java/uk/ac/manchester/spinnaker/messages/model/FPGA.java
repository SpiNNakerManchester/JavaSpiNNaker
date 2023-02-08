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
package uk.ac.manchester.spinnaker.messages.model;

/**
 * Identifiers for the FPGAs on a SpiNNaker board, as managed by BMP.
 * <p>
 * SpiNNaker FPGA identifiers, taken from the
 * <a href="http://spinnakermanchester.github.io/docs/spin5-links.pdf">SpiNN-5
 * FPGA SATA Links</a> datasheet.
 */
public enum FPGA {
	/** The first FPGA. Handles east and south. */
	FPGA_E_S("0", 0, 1),
	/** The second FPGA. Handles south-west and west. */
	FPGA_SW_W("1", 1, 2),
	/** The third FPGA. Handles north and north-east. */
	FPGA_N_NE("2", 2, 4),
	/**
	 * All three FPGAs. Note that only a subset of APIs that handle FPGAs will
	 * accept this.
	 */
	FPGA_ALL("0-2", 3, 7);

	/** The "name" of the FPGA. */
	public final String name;

	/** The FPGA identifier in protocol terms. */
	public final int value;

	/** The bit encoding for read and write requests. */
	public final int bits;

	FPGA(String name, int value, int bits) {
		this.name = name;
		this.value = value;
		this.bits = bits;
	}

	@Override
	public String toString() {
		return name;
	}

	/** @return Whether this identifies a single FPGA. */
	public boolean isSingleFPGA() {
		return this != FPGA_ALL;
	}
}
