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

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.Paths.BASE_PATH;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import io.swagger.v3.oas.annotations.Hidden;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.Machine;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException;

/**
 * Implements the service administration interface.
 *
 * @author Donal Fellows
 */
@Service("admin")
@Path(BASE_PATH)
@Hidden
@ManagedResource("Spalloc:type=Admin,name=admin")
public class AdminImpl extends SQLQueries implements AdminAPI {
	private static final Logger log = getLogger(AdminImpl.class);

	@Autowired
	private MachineDefinitionLoader loader;

	@Autowired
	private MachineStateControl machineController;

	@Autowired
	private UserControl userController;

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

	private static WebApplicationException noBoard() {
		return new WebApplicationException("no such board", NOT_FOUND);
	}

	private static WebApplicationException noUser() {
		return new WebApplicationException("no such user", NOT_FOUND);
	}

	@Override
	public boolean getBoardStateXYZ(String name, int x, int y, int z)
			throws SQLException {
		log.warn("CALLED boardState({}:XYZ=({},{},{}))", name, x, y, z);
		BoardState board = machineController.findTriad(name, x, y, z)
				.orElseThrow(AdminImpl::noBoard);
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateXYZ(String name, int x, int y, int z,
			boolean enabled) throws SQLException {
		log.warn("CALLED boardState({}:XYZ=({},{},{})) := {}", name, x, y, z,
				enabled);
		BoardState board = machineController.findTriad(name, x, y, z)
				.orElseThrow(AdminImpl::noBoard);
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public boolean getBoardStateCFB(String name, int c, int f, int b)
			throws SQLException {
		log.warn("CALLED boardState({}:CFB=({},{},{}))", name, c, f, b);
		BoardState board = machineController.findPhysical(name, c, f, b)
				.orElseThrow(AdminImpl::noBoard);
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateCFB(String name, int c, int f, int b,
			boolean enabled) throws SQLException {
		log.warn("CALLED boardState({}:CFB=({},{},{})) := {}", name, c, f, b,
				enabled);
		BoardState board = machineController.findPhysical(name, c, f, b)
				.orElseThrow(AdminImpl::noBoard);
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public boolean getBoardStateAddress(String name, String address)
			throws SQLException {
		log.warn("CALLED boardState({}:IP=({}))", name, address);
		BoardState board = machineController.findIP(name, address)
				.orElseThrow(AdminImpl::noBoard);
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateAddress(String name, String address,
			boolean enabled) throws SQLException {
		log.warn("CALLED boardState({}:IP=({})) := {}", name, address, enabled);
		BoardState board = machineController.findIP(name, address)
				.orElseThrow(AdminImpl::noBoard);
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public Map<String, URI> listUsers(UriInfo ui) throws SQLException {
		Map<String, URI> result = new TreeMap<>();
		UriBuilder ub = ui.getAbsolutePathBuilder().path("{id}");
		for (User user : userController.listUsers()) {
			result.put(user.getUserName(), ub.build(user.getUserId()));
		}
		return unmodifiableMap(result);
	}

	@Override
	public Response createUser(User providedUser, UriInfo ui)
			throws SQLException {
		providedUser.setUserId(null);
		if (providedUser.getTrustLevel() == null) {
			providedUser.setTrustLevel(TrustLevel.USER);
		}
		if (providedUser.getQuota() == null) {
			providedUser.setQuota(emptyMap());
		}
		if (providedUser.isEnabled() == null) {
			providedUser.setEnabled(true);
		}
		if (providedUser.isLocked() == null) {
			providedUser.setLocked(false);
		}
		User realUser = userController.createUser(providedUser)
				.orElseThrow(() -> new RequestFailedException(NOT_MODIFIED,
						"user already exists"));
		UriBuilder ub = ui.getAbsolutePathBuilder().path("{id}");
		int id = realUser.getUserId();
		return Response.created(ub.build(id)).type(APPLICATION_JSON)
				.entity(realUser.sanitise()).build();
	}

	@Override
	public User describeUser(int id) throws SQLException {
		return userController.getUser(id).orElseThrow(AdminImpl::noUser)
				.sanitise();
	}

	@Override
	public User updateUser(int id, User providedUser, SecurityContext security)
			throws SQLException {
		String adminUser = security.getUserPrincipal().getName();
		providedUser.setUserId(null);
		return userController.updateUser(id, providedUser, adminUser)
				.orElseThrow(AdminImpl::noUser).sanitise();
	}

	@Override
	public Response deleteUser(int id, SecurityContext security)
			throws SQLException {
		String adminUser = security.getUserPrincipal().getName();
		userController.deleteUser(id, adminUser).orElseThrow(AdminImpl::noUser);
		return Response.noContent().build();
	}
}
