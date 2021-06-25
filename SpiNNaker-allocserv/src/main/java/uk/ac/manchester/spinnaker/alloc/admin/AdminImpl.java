/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import io.swagger.v3.oas.annotations.Hidden;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.Machine;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;

/**
 * Implements the service administration interface.
 *
 * @author Donal Fellows
 */
@Service("admin")
@Path("/")
@Hidden
@ManagedResource("Spalloc:type=Admin,name=admin")
public class AdminImpl extends SQLQueries implements AdminAPI {
	private static final Logger log = getLogger(AdminImpl.class);

	@Autowired
	private MachineDefinitionLoader loader;

	@Autowired
	private MachineStateControl controller;

	@Override
	@ManagedOperation
	public void importMachinesFromFile(String filename) {
		log.warn("CALLED importMachinesFromFile({})", filename);
		File f = new File(filename);
		if (f.exists() && f.canRead()) {
			try {
				loader.loadMachineDefinitions(f);
			} catch (SQLException | IOException e) {
				throw new WebApplicationException(
						"failed to load machine definitions", e, BAD_REQUEST);
			}
		} else {
			throw new WebApplicationException(
					"no such load machine definition file", BAD_REQUEST);
		}
	}

	@Override
	public void importMachinesByContent(
			MachineDefinitionLoader.Configuration definitions) {
		log.warn("CALLED importMachinesByContent({})", definitions.getMachines()
				.stream().map(Machine::getName).collect(toList()));
		try {
			loader.loadMachineDefinitions(definitions);
		} catch (SQLException e) {
			throw new WebApplicationException(
					"failed to load machine definitions", e, BAD_REQUEST);
		}
	}

	@Override
	public boolean getBoardStateXYZ(String name, int x, int y, int z)
			throws SQLException {
		log.warn("CALLED boardState({}:XYZ=({},{},{}))", name, x, y, z);
		BoardState board = controller.findTriad(name, x, y, z).orElseThrow(
				() -> new WebApplicationException("no such board", NOT_FOUND));
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateXYZ(String name, int x, int y, int z,
			boolean enabled) throws SQLException {
		log.warn("CALLED boardState({}:XYZ=({},{},{})) := {}", name, x, y, z,
				enabled);
		BoardState board = controller.findTriad(name, x, y, z).orElseThrow(
				() -> new WebApplicationException("no such board", NOT_FOUND));
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public boolean getBoardStateCFB(String name, int c, int f, int b)
			throws SQLException {
		log.warn("CALLED boardState({}:CFB=({},{},{}))", name, c, f, b);
		BoardState board = controller.findPhysical(name, c, f, b).orElseThrow(
				() -> new WebApplicationException("no such board", NOT_FOUND));
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateCFB(String name, int c, int f, int b,
			boolean enabled) throws SQLException {
		log.warn("CALLED boardState({}:CFB=({},{},{})) := {}", name, c, f, b,
				enabled);
		BoardState board = controller.findPhysical(name, c, f, b).orElseThrow(
				() -> new WebApplicationException("no such board", NOT_FOUND));
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public boolean getBoardStateAddress(String name, String address)
			throws SQLException {
		log.warn("CALLED boardState({}:IP=({}))", name, address);
		BoardState board = controller.findIP(name, address).orElseThrow(
				() -> new WebApplicationException("no such board", NOT_FOUND));
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateAddress(String name, String address,
			boolean enabled) throws SQLException {
		log.warn("CALLED boardState({}:IP=({})) := {}", name, address, enabled);
		BoardState board = controller.findIP(name, address).orElseThrow(
				() -> new WebApplicationException("no such board", NOT_FOUND));
		board.setState(enabled);
		return board.getState();
	}
}
