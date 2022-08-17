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
 * Python to JSON configuration converter. Takes the old-style configurations
 * used for old Spalloc and converts them into a form that new Spalloc can
 * import. Since the old configuration files were executable, this is
 * non-trivial.
 *
 * @author Donal Fellows
 */
module spinnaker.allocator.py2json {
	// External dependencies
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires spinnaker.comms;
	requires info.picocli;
	requires jython.slim;

	opens uk.ac.manchester.spinnaker.py2json
			to com.fasterxml.jackson.databind;
}
