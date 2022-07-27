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
 * Data specification generator and executor.
 *
 * @author Donal Fellows
 */
module spinnaker.data_spec {
	requires spinnaker.utils;
	requires spinnaker.comms;
	requires spinnaker.storage;
	requires org.apache.commons.lang3;
	requires static java.desktop;
	requires org.slf4j;
	requires org.apache.commons.io;

	exports uk.ac.manchester.spinnaker.data_spec;
}
