/*
 * Copyright (c) 2022 The University of Manchester
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

/**
 * How to actually talk to a SpiNNaker machine and to Spalloc, the allocation
 * service.
 *
 * @author Donal Fellows
 */
module spinnaker.comms {
	requires transitive spinnaker.utils;
	requires transitive spinnaker.machine;
	requires transitive spinnaker.storage;

	requires org.slf4j;
	requires org.apache.commons.configuration2;
	requires transitive com.fasterxml.jackson.databind;

	exports uk.ac.manchester.spinnaker.connections;
	exports uk.ac.manchester.spinnaker.connections.model;
	exports uk.ac.manchester.spinnaker.messages;
	exports uk.ac.manchester.spinnaker.messages.bmp;
	exports uk.ac.manchester.spinnaker.messages.boot;
	exports uk.ac.manchester.spinnaker.messages.eieio;
	exports uk.ac.manchester.spinnaker.messages.model;
	exports uk.ac.manchester.spinnaker.messages.notification;
	exports uk.ac.manchester.spinnaker.messages.scp;
	exports uk.ac.manchester.spinnaker.messages.sdp;
	exports uk.ac.manchester.spinnaker.io;
	exports uk.ac.manchester.spinnaker.spalloc;
	exports uk.ac.manchester.spinnaker.spalloc.messages;
	exports uk.ac.manchester.spinnaker.transceiver;

}
