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
 * The front-end interface. NB: not a GUI!
 *
 * @author Donal Fellows
 */
module spinnaker.front_end {
	requires spinnaker.comms;
	requires spinnaker.data_spec;
	requires spinnaker.storage;

	requires com.fasterxml.jackson.databind;
	requires org.slf4j;
	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;
	requires diffutils; // Ugh!
	requires org.apache.commons.io;
	requires org.apache.commons.text;
}
