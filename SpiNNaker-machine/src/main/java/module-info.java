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
 * Describes a SpiNNaker machine.
 *
 * @author Christian Brenninkmeijer
 */
open module spinnaker.machine {
	// Internal dependencies
	requires spinnaker.utils;

	// External dependencies
	requires org.slf4j;
	requires com.fasterxml.jackson.annotation;
	requires transitive com.fasterxml.jackson.databind;

	exports uk.ac.manchester.spinnaker.machine;
	exports uk.ac.manchester.spinnaker.machine.bean;
	exports uk.ac.manchester.spinnaker.machine.datalinks;
	exports uk.ac.manchester.spinnaker.machine.tags;
}
