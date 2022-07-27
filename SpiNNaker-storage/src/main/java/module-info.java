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
 * Storage management.
 *
 * @author Donal Fellows
 */
open module spinnaker.storage {
	requires spinnaker.utils;
	requires transitive spinnaker.machine;
	requires org.slf4j;
	requires org.xerial.sqlitejdbc;
	requires transitive java.sql;
	requires org.apache.commons.io;
	requires java.base;

	exports uk.ac.manchester.spinnaker.storage;
}
