/*
 * Copyright (c) 2021-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.admin;

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
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import io.swagger.v3.oas.annotations.Hidden;
import uk.ac.manchester.spinnaker.alloc.admin.MachineDefinitionLoader.Machine;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.MemberRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;

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
		log.warn("CALLED importMachinesByContent({})", definitions.machines()
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
		var board = machineController.findTriad(name, new TriadCoords(x, y, z))
				.orElseThrow(AdminImpl::noBoard);
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateXYZ(String name, int x, int y, int z,
			boolean enabled) {
		log.warn("CALLED boardState({}:XYZ=({},{},{})) := {}", name, x, y, z,
				enabled);
		var board = machineController.findTriad(name, new TriadCoords(x, y, z))
				.orElseThrow(AdminImpl::noBoard);
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public boolean getBoardStateCFB(String name, int c, int f, int b) {
		log.info("CALLED boardState({}:CFB=({},{},{}))", name, c, f, b);
		var board = machineController
				.findPhysical(name, new PhysicalCoords(c, f, b))
				.orElseThrow(AdminImpl::noBoard);
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateCFB(String name, int c, int f, int b,
			boolean enabled) {
		log.warn("CALLED boardState({}:CFB=({},{},{})) := {}", name, c, f, b,
				enabled);
		var board = machineController
				.findPhysical(name, new PhysicalCoords(c, f, b))
				.orElseThrow(AdminImpl::noBoard);
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public boolean getBoardStateAddress(String name, String address) {
		log.info("CALLED boardState({}:IP=({}))", name, address);
		var board = machineController.findIP(name, address)
				.orElseThrow(AdminImpl::noBoard);
		return board.getState();
	}

	@Override
	@ManagedOperation
	public boolean setBoardStateAddress(String name, String address,
			boolean enabled) {
		log.warn("CALLED boardState({}:IP=({})) := {}", name, address, enabled);
		var board = machineController.findIP(name, address)
				.orElseThrow(AdminImpl::noBoard);
		board.setState(enabled);
		return board.getState();
	}

	@Override
	public Map<String, URI> listUsers(UriInfo ui) {
		log.info("CALLED listUsers()");
		var ub = ui.getBaseUriBuilder().path(DESCRIBE_USER);
		return userManager.listUsers(user -> ub.build(user.getUserId()));
	}

	@Override
	public Response createUser(UserRecord providedUser, UriInfo ui) {
		log.warn("CALLED createUser({})", providedUser.getUserName());
		providedUser.initCreationDefaults();
		var realUser = userManager.createUser(providedUser)
				.orElseThrow(() -> new RequestFailedException(NOT_MODIFIED,
						"user already exists"));
		var ub = ui.getBaseUriBuilder().path(DESCRIBE_USER);
		int id = realUser.getUserId();
		return created(ub.build(id)).type(APPLICATION_JSON)
				.entity(realUser.sanitise()).build();
	}

	@Override
	public UserRecord describeUser(int id, UriInfo ui) {
		log.info("CALLED describeUser({})", id);
		var ub = ui.getBaseUriBuilder().path(DESCRIBE_GROUP);
		return userManager.getUser(id, m -> ub.build(m.getGroupId()))
				.orElseThrow(AdminImpl::noUser).sanitise();
	}

	@Override
	public UserRecord updateUser(int id, UserRecord providedUser, UriInfo ui,
			SecurityContext security) {
		log.warn("CALLED updateUser({})", providedUser.getUserName());
		var adminUser = security.getUserPrincipal().getName();
		providedUser.setUserId(null);
		var ub = ui.getBaseUriBuilder().path(DESCRIBE_GROUP);
		return userManager
				.updateUser(id, providedUser, adminUser,
						m -> ub.build(m.getGroupId()))
				.orElseThrow(AdminImpl::noUser).sanitise();
	}

	@Override
	public String deleteUser(int id, SecurityContext security) {
		log.warn("CALLED deleteUser({})", id);
		var adminUser = security.getUserPrincipal().getName();
		return "deleted user " + userManager.deleteUser(id, adminUser)
				.orElseThrow(AdminImpl::noUser);
	}

	@Override
	public Map<String, URI> listGroups(UriInfo ui) {
		log.warn("CALLED listGroups()");
		var ub = ui.getBaseUriBuilder().path(DESCRIBE_GROUP);
		return userManager.listGroups(g -> ub.build(g.getGroupId()));
	}

	@Override
	public Response createGroup(GroupRecord group, UriInfo ui) {
		log.warn("CALLED createGroup({})", group.getGroupName());
		var realGroup = userManager.createGroup(group, group.getType())
				.orElseThrow(() -> new WebApplicationException(
						"group already exists", BAD_REQUEST));
		var ub = ui.getBaseUriBuilder().path(DESCRIBE_GROUP);
		return created(ub.build(realGroup.getGroupId())).type(APPLICATION_JSON)
				.entity(realGroup).build();
	}

	@Override
	public GroupRecord describeGroup(int groupId, UriInfo ui) {
		log.warn("CALLED describeGroup({})", groupId);
		var ub = ui.getBaseUriBuilder().path(DESCRIBE_MEMBER);
		return userManager.getGroup(groupId, m -> ub.build(groupId, m.getId()))
				.orElseThrow(AdminImpl::noGroup);
	}

	@Override
	public GroupRecord updateGroup(int groupId, GroupRecord group, UriInfo ui) {
		log.warn("CALLED updateGroup({})", groupId);
		var ub = ui.getBaseUriBuilder().path(DESCRIBE_MEMBER);
		return userManager
				.updateGroup(groupId, group,
						m -> ub.build(group.getGroupId(), m.getId()))
				.orElseThrow(AdminImpl::noGroup);
	}

	@Override
	public String deleteGroup(int groupId) {
		log.warn("CALLED deleteGroup({})", groupId);
		return "deleted group " + userManager.deleteGroup(groupId)
				.orElseThrow(AdminImpl::noGroup);
	}

	@Override
	public Response addMember(int groupId, MemberRecord request, UriInfo ui) {
		var userName = request.getUserName();
		log.warn("CALLED addMember({},{})", groupId, userName);
		var ub = ui.getBaseUriBuilder().path(DESCRIBE_MEMBER);
		var group = userManager.getGroup(groupId, null)
				.orElseThrow(AdminImpl::noGroup);
		var user = userManager.getUser(userName, null)
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
		var ubGroup = ui.getBaseUriBuilder().path(DESCRIBE_GROUP);
		var ubUser = ui.getBaseUriBuilder().path(DESCRIBE_USER);
		return userManager
				.describeMembership(memberId,
						m -> ubGroup.build(m.getGroupId()),
						m -> ubUser.build(m.getUserId()))
				.orElseThrow(AdminImpl::noMember);
	}

	@Override
	public Response removeMember(int groupId, int memberId) {
		log.warn("CALLED removeMember({groupId},{memberId})");
		var group = userManager.getGroup(groupId, null)
				.orElseThrow(AdminImpl::noGroup);
		var member = userManager.describeMembership(memberId, null, null)
				.orElseThrow(AdminImpl::noMember);
		if (!userManager.removeMembershipOfGroup(member, group)) {
			throw new WebApplicationException("remove failed", BAD_REQUEST);
		}
		return noContent().build();
	}
}
