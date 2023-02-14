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

import java.net.InetAddress;

/** The IP addresses associated with a SpiNNaker board. */
public final class Addresses {
	// TODO convert to record in 17
	/** The IPv4 address of the BMP. */
	public final InetAddress bmpIPAddress;

	/** The IPv4 address of the managed SpiNNaker board. */
	public final InetAddress spinIPAddress;

	/**
	 * @param bmpIPAddress
	 *            The IPv4 address of the BMP.
	 * @param spinIPAddress
	 *            The IPv4 address of the managed SpiNNaker board.
	 */
	public Addresses(InetAddress bmpIPAddress, InetAddress spinIPAddress) {
		this.bmpIPAddress = bmpIPAddress;
		this.spinIPAddress = spinIPAddress;
	}
}
