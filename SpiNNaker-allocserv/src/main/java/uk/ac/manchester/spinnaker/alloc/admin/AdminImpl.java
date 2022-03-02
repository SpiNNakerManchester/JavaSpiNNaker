/*
 * Copyright (c) 2021-2022 The University of Manchester
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

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.Paths.BASE_PATH;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;

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
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.Machine;
import uk.ac.manchester.spinnaker.alloc.admin.MachineStateControl.BoardState;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.MemberRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
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
public class AdminImpl implements AdminAPI {
	private static final Logger log = getLogger(AdminImpl.class);

	@Autowired
	private MachineDefinitionLoader loader;

	@Autowired
	private MachineStateControl machineController;

	@Autowired
	private UserControl userManager;

	@Override
	public void importMachinesByContent(
			MachineDefinitionLoader.Configuration definitions) {
		log.warn("CALLED importMachinesByContent({})", definitions.getMachines()
				.stream().map(Machine::getName).collect(toList()));
		loader.loadMachineDefinitions(definitions);
	}

	private static final Method DESCRIBE_GROUP;

	private static final Method DESCRIBE_USER;

	private static final Method DESCRIBE_MEMBER;

	static {
		try {
			DESCRIBE_GROUP = AdminAPI.class.getMethod("describeGroup",
					int.class, UriInfo.class);
			DESCRIBE_USER = AdminAPI.class.getMethod("describeUser", int.class,
					UriInfo.class);
			DESCRIBE_MEMBER = AdminAPI.class.getMethod("describeMember",
					int.class, int.class, UriInfo.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("failed to discover method signatures",
					e);
		}
	}

	private static WebApplicationException noBoard() {
		return new WebApplicationException("no such board", NOT_FOUND);
	}

	private static WebApplicationException noUser() {
		return new WebApplicationException("no such user", NOT_FOUND);
	}

	private static WebApplicationException noGroup() {
		return new WebApplicationException("no such group", NOT_FOUND);
	}

	private static WebApplicationException noMember() {
		return new WebApplicationException("no such membership", NOT_FOUND);
	}

	@Override
	public boolean getBoardStateXYZ(String name, int x, int y, int z) {
		log.info("CALLED boardState({}:XYZ=({},{},{}))", name, x, y, z);
		BoardState board = machineController.findTriad(name, x, y, z)
				.orElseThrow(AdminImpl::noBoard);
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateXYZ(String name, int x, int y, int z,
			boolean enabled) {
		log.warn("CALLED boardState({}:XYZ=({},{},{})) := {}", name, x, y, z,
				enabled);
		BoardState board = machineController.findTriad(name, x, y, z)
				.orElseThrow(AdminImpl::noBoard);
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public boolean getBoardStateCFB(String name, int c, int f, int b) {
		log.info("CALLED boardState({}:CFB=({},{},{}))", name, c, f, b);
		BoardState board = machineController.findPhysical(name, c, f, b)
				.orElseThrow(AdminImpl::noBoard);
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateCFB(String name, int c, int f, int b,
			boolean enabled) {
		log.warn("CALLED boardState({}:CFB=({},{},{})) := {}", name, c, f, b,
				enabled);
		BoardState board = machineController.findPhysical(name, c, f, b)
				.orElseThrow(AdminImpl::noBoard);
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public boolean getBoardStateAddress(String name, String address) {
		log.info("CALLED boardState({}:IP=({}))", name, address);
		BoardState board = machineController.findIP(name, address)
				.orElseThrow(AdminImpl::noBoard);
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateAddress(String name, String address,
			boolean enabled) {
		log.warn("CALLED boardState({}:IP=({})) := {}", name, address, enabled);
		BoardState board = machineController.findIP(name, address)
				.orElseThrow(AdminImpl::noBoard);
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public Map<String, URI> listUsers(UriInfo ui) {
		log.info("CALLED listUsers()");
		UriBuilder ub = ui.getBaseUriBuilder().path(DESCRIBE_USER);
		return unmodifiableMap(
				userManager.listUsers(user -> ub.build(user.getUserId())));
	}

	@Override
	public Response createUser(UserRecord providedUser, UriInfo ui) {
		log.warn("CALLED createUser({})", providedUser.getUserName());
		providedUser.initCreationDefaults();
		UserRecord realUser = userManager.createUser(providedUser)
				.orElseThrow(() -> new RequestFailedException(NOT_MODIFIED,
						"user already exists"));
		UriBuilder ub = ui.getBaseUriBuilder().path(DESCRIBE_USER);
		int id = realUser.getUserId();
		return created(ub.build(id)).type(APPLICATION_JSON)
				.entity(realUser.sanitise()).build();
	}

	@Override
	public UserRecord describeUser(int id, UriInfo ui) {
		log.info("CALLED describeUser({})", id);
		UriBuilder ub = ui.getBaseUriBuilder().path(DESCRIBE_GROUP);
		return userManager.getUser(id, m -> ub.build(m.getGroupId()))
				.orElseThrow(AdminImpl::noUser).sanitise();
	}

	@Override
	public UserRecord updateUser(int id, UserRecord providedUser, UriInfo ui,
			SecurityContext security) {
		log.warn("CALLED updateUser({})", providedUser.getUserName());
		String adminUser = security.getUserPrincipal().getName();
		providedUser.setUserId(null);
		UriBuilder ub = ui.getBaseUriBuilder().path(DESCRIBE_GROUP);
		return userManager
				.updateUser(id, providedUser, adminUser,
						m -> ub.build(m.getGroupId()))
				.orElseThrow(AdminImpl::noUser).sanitise();
	}

	@Override
	public Response deleteUser(int id, SecurityContext security) {
		log.warn("CALLED deleteUser({})", id);
		String adminUser = security.getUserPrincipal().getName();
		userManager.deleteUser(id, adminUser).orElseThrow(AdminImpl::noUser);
		return noContent().build();
	}

	@Override
	public Map<String, URI> listGroups(UriInfo ui) {
		log.warn("CALLED listGroups()");
		UriBuilder ub = ui.getBaseUriBuilder().path(DESCRIBE_GROUP);
		return userManager.listGroups(g -> ub.build(g.getGroupId()));
	}

	@Override
	public Response createGroup(GroupRecord group, UriInfo ui) {
		log.warn("CALLED createGroup({})", group.getGroupName());
		GroupRecord realGroup =
				userManager.createGroup(group, group.getType())
						.orElseThrow(() -> new WebApplicationException(
								"group already exists", BAD_REQUEST));
		UriBuilder ub = ui.getBaseUriBuilder().path(DESCRIBE_GROUP);
		return created(ub.build(realGroup.getGroupId())).type(APPLICATION_JSON)
				.entity(realGroup).build();
	}

	@Override
	public GroupRecord describeGroup(int groupId, UriInfo ui) {
		log.warn("CALLED describeGroup({})", groupId);
		UriBuilder ub = ui.getBaseUriBuilder().path(DESCRIBE_MEMBER);
		return userManager.getGroup(groupId, m -> ub.build(groupId, m.getId()))
				.orElseThrow(AdminImpl::noGroup);
	}

	@Override
	public GroupRecord updateGroup(int groupId, GroupRecord group, UriInfo ui) {
		log.warn("CALLED updateGroup({})", groupId);
		UriBuilder ub = ui.getBaseUriBuilder().path(DESCRIBE_MEMBER);
		return userManager
				.updateGroup(groupId, group,
						m -> ub.build(group.getGroupId(), m.getId()))
				.orElseThrow(AdminImpl::noGroup);
	}

	@Override
	public Response deleteGroup(int groupId) {
		log.warn("CALLED deleteGroup({})", groupId);
		userManager.deleteGroup(groupId).orElseThrow(AdminImpl::noGroup);
		return noContent().build();
	}

	@Override
	public Response addMember(int groupId, MemberRecord request, UriInfo ui) {
		String userName = request.getUserName();
		log.warn("CALLED addMember({},{})", groupId, userName);
		UriBuilder ub = ui.getBaseUriBuilder().path(DESCRIBE_MEMBER);
		GroupRecord group = userManager.getGroup(groupId, null)
				.orElseThrow(AdminImpl::noGroup);
		UserRecord user = userManager.getUser(userName, null)
				.orElseThrow(AdminImpl::noUser);
		return userManager.addUserToGroup(user, group)
				.map(member -> created(ub.build(member.getId()))
						.type(APPLICATION_JSON).entity(member))
				.orElseThrow(() -> new WebApplicationException(
						"user already a member of group", BAD_REQUEST))
				.build();
	}

	@Override
	public MemberRecord describeMember(int groupId, int memberId, UriInfo ui) {
		log.warn("CALLED describeMember({},{})", groupId, memberId);
		UriBuilder ubGroup = ui.getBaseUriBuilder().path(DESCRIBE_GROUP);
		UriBuilder ubUser = ui.getBaseUriBuilder().path(DESCRIBE_USER);
		return userManager
				.describeMembership(memberId,
						m -> ubGroup.build(m.getGroupId()),
						m -> ubUser.build(m.getUserId()))
				.orElseThrow(AdminImpl::noMember);
	}

	@Override
	public Response removeMember(int groupId, int memberId) {
		log.warn("CALLED removeMember({groupId},{memberId})");
		GroupRecord group = userManager.getGroup(groupId, null)
				.orElseThrow(AdminImpl::noGroup);
		MemberRecord member =
				userManager.describeMembership(memberId, null, null)
						.orElseThrow(AdminImpl::noMember);
		if (!userManager.removeMembershipOfGroup(member, group)) {
			throw new WebApplicationException("remove failed", BAD_REQUEST);
		}
		return noContent().build();
	}
}
