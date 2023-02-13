/*
 * Copyright (c) 2021-2023 The University of Manchester
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

import static java.util.Objects.nonNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.Paths.BOARD;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.Paths.GROUP;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.Paths.IMPORT;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.Paths.MEMBER;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.Paths.USER;
import static uk.ac.manchester.spinnaker.alloc.security.SecurityConfig.IS_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.SERV;

import java.net.URI;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.springframework.security.access.prepost.PreAuthorize;

import io.swagger.v3.oas.annotations.Hidden;
import uk.ac.manchester.spinnaker.alloc.model.GroupRecord;
import uk.ac.manchester.spinnaker.alloc.model.MemberRecord;
import uk.ac.manchester.spinnaker.alloc.model.UserRecord;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException;
import uk.ac.manchester.spinnaker.machine.board.ValidBoardNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidCabinetNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidFrameNumber;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/**
 * Administration interface.
 *
 * @author Donal Fellows
 */
@Hidden
@PreAuthorize(IS_ADMIN)
@SuppressWarnings("checkstyle:parameternumber")
public interface AdminAPI {
	/** Common paths in the interface. */
	interface Paths {
		/** The location of the admin "service". */
		String BASE_PATH = SERV + "/admin";

		/** Service root. */
		String ROOT = "/";

		/** Machine definition importing. */
		String IMPORT = "import";

		/** Board control. */
		String BOARD = "board";

		/** User account control. */
		String USER = "users";

		/** User group control. */
		String GROUP = "groups";

		/** Group membership control. */
		String MEMBER = "members";
	}

	/**
	 * Where the other resources are. Designed for conversion to JSON.
	 */
	final class Description {
		/** Machine definition importing. */
		public final URI importMachines;

		/** Board control. */
		public final URI setBoardEnabled;

		private Description(UriInfo ui) {
			var b = ui.getAbsolutePathBuilder().path("{resource}");
			importMachines = b.build(IMPORT);
			setBoardEnabled = b.build(BOARD);
		}
	}

	/**
	 * Describe the admin interface.
	 *
	 * @param ui
	 *            How to mint URIs.
	 * @return Machine-readable description.
	 */
	@GET
	@Produces(APPLICATION_JSON)
	@Path(Paths.ROOT)
	default Description describeOperations(@Context UriInfo ui) {
		return new Description(ui);
	}

	/**
	 * "Describes" the import resource.
	 *
	 * @return Human-readable description.
	 */
	@Hidden
	@GET
	@Produces(TEXT_PLAIN)
	@Path(IMPORT)
	default String describeImport() {
		return "This resource only really supports POST.";
	}

	/**
	 * Import a machine definition by posting it.
	 *
	 * @param definitions
	 *            The definitions.
	 */
	@POST
	@Consumes(APPLICATION_JSON)
	@Path(IMPORT)
	void importMachinesByContent(
			@NotNull @Valid MachineDefinitionLoader.Configuration definitions);

	/**
	 * Describe the enable state of a board.
	 *
	 * @param machineName
	 *            The name of the machine; required
	 * @param x
	 *            The X coordinate, used to identify a board by triad coords
	 * @param y
	 *            The Y coordinate, used to identify a board by triad coords
	 * @param z
	 *            The Z coordinate, used to identify a board by triad coords
	 * @param c
	 *            The cabinet number, used to identify a board physically
	 * @param f
	 *            The frame number, used to identify a board physically
	 * @param b
	 *            The board number, used to identify a board physically
	 * @param address
	 *            The IP address of the board (dotted quad)
	 * @return Whether the board is enabled
	 * @throws RequestFailedException
	 *             If bad query combinations are supplied
	 */
	@GET
	@Produces(TEXT_PLAIN)
	@Path(BOARD)
	default boolean getBoardState(
			@NotBlank(message = "machine name is required")
			@QueryParam("machine") String machineName,
			@QueryParam("x") @ValidTriadX Integer x,
			@QueryParam("y") @ValidTriadY Integer y,
			@QueryParam("z") @ValidTriadZ Integer z,
			@QueryParam("cabinet") @ValidCabinetNumber Integer c,
			@QueryParam("frame") @ValidFrameNumber Integer f,
			@QueryParam("board") @ValidBoardNumber Integer b,
			@QueryParam("address") @IPAddress(nullOK = true) String address) {
		if (nonNull(x) && nonNull(y) && nonNull(z)) {
			return getBoardStateXYZ(machineName, x, y, z);
		}
		if (nonNull(c) && nonNull(f) && nonNull(b)) {
			return getBoardStateCFB(machineName, c, f, b);
		}
		if (nonNull(address)) {
			return getBoardStateAddress(machineName, address);
		}
		throw new RequestFailedException(BAD_REQUEST,
				"You need to supply coordinates (as "
						+ "x=&y=&z=, c=&f=&b= or address=)");
	}

	/**
	 * Find board by logical triad coordinates and return its state.
	 *
	 * @param name
	 *            The name of the machine
	 * @param x
	 *            The X coordinate
	 * @param y
	 *            The Y coordinate
	 * @param z
	 *            The Z coordinate
	 * @return Whether the board is enabled
	 */
	boolean getBoardStateXYZ(String name, @ValidTriadX int x,
			@ValidTriadY int y, @ValidTriadZ int z);

	/**
	 * Find board by physical coordinates and return its state.
	 *
	 * @param name
	 *            The name of the machine
	 * @param c
	 *            The cabinet number
	 * @param f
	 *            The frame number
	 * @param b
	 *            The board number
	 * @return Whether the board is enabled
	 */
	boolean getBoardStateCFB(String name, @ValidCabinetNumber int c,
			@ValidFrameNumber int f, @ValidBoardNumber int b);

	/**
	 * Find board by IP address and return its state.
	 *
	 * @param name
	 *            The name of the machine
	 * @param address
	 *            The IP address of the board (dotted quad)
	 * @return Whether the board is enabled
	 */
	boolean getBoardStateAddress(String name, @IPAddress String address);

	/**
	 * Enable or disable a board.
	 *
	 * @param machineName
	 *            The name of the machine
	 * @param x
	 *            The X coordinate, used to identify a board by triad coords
	 * @param y
	 *            The Y coordinate, used to identify a board by triad coords
	 * @param z
	 *            The Z coordinate, used to identify a board by triad coords
	 * @param c
	 *            The cabinet number, used to identify a board physically
	 * @param f
	 *            The frame number, used to identify a board physically
	 * @param b
	 *            The board number, used to identify a board physically
	 * @param address
	 *            The IP address of the board (dotted quad)
	 * @param enabled
	 *            Whether the board should be set to the enabled state
	 * @return Whether the board is enabled
	 * @throws RequestFailedException
	 *             If bad query combinations are supplied
	 */
	@PUT
	@Consumes(TEXT_PLAIN)
	@Produces(TEXT_PLAIN)
	@Path(BOARD)
	default boolean setBoardState(
			@NotBlank(message = "machine name is required")
			@QueryParam("machine") String machineName,
			@QueryParam("x") @ValidTriadX Integer x,
			@QueryParam("y") @ValidTriadY Integer y,
			@QueryParam("z") @ValidTriadZ Integer z,
			@QueryParam("cabinet") @ValidCabinetNumber Integer c,
			@QueryParam("frame") @ValidFrameNumber Integer f,
			@QueryParam("board") @ValidBoardNumber Integer b,
			@QueryParam("address") @IPAddress(nullOK = true) String address,
			boolean enabled) {
		if (nonNull(x) && nonNull(y) && nonNull(z)) {
			return setBoardStateXYZ(machineName, x, y, z, enabled);
		}
		if (nonNull(c) && nonNull(f) && nonNull(b)) {
			return setBoardStateCFB(machineName, c, f, b, enabled);
		}
		if (nonNull(address)) {
			return setBoardStateAddress(machineName, address, enabled);
		}
		throw new RequestFailedException(BAD_REQUEST,
				"You need to supply coordinates (as "
						+ "x=&y=&z=, c=&f=&b= or address=)");
	}

	/**
	 * Enable or disable a board. Find by logical triad coordinates.
	 *
	 * @param name
	 *            The name of the machine
	 * @param x
	 *            The X coordinate
	 * @param y
	 *            The Y coordinate
	 * @param z
	 *            The Z coordinate
	 * @param enabled
	 *            Whether the board should be set to the enabled state
	 * @return Whether the board is enabled
	 */
	boolean setBoardStateXYZ(@NotBlank String name, @ValidTriadX int x,
			@ValidTriadY int y, @ValidTriadZ int z, boolean enabled);

	/**
	 * Enable or disable a board. Find by physical coordinates.
	 *
	 * @param name
	 *            The name of the machine
	 * @param c
	 *            The cabinet number
	 * @param f
	 *            The frame number
	 * @param b
	 *            The board number
	 * @param enabled
	 *            Whether the board should be set to the enabled state
	 * @return Whether the board is enabled
	 */
	boolean setBoardStateCFB(@NotBlank String name,
			@NotNull @ValidCabinetNumber int c,
			@NotNull @ValidFrameNumber int f,
			@NotNull @ValidBoardNumber int b, boolean enabled);

	/**
	 * Enable or disable a board. Find by IP address.
	 *
	 * @param name
	 *            The name of the machine
	 * @param address
	 *            The IP address of the board (dotted quad)
	 * @param enabled
	 *            Whether the board should be set to the enabled state
	 * @return Whether the board is enabled
	 */
	boolean setBoardStateAddress(@NotBlank String name,
			@IPAddress String address, boolean enabled);

	/**
	 * List the usernames and the URIs used to describe and manipulate them.
	 *
	 * @param ui
	 *            For building URIs.
	 * @return A sorted map from username to details-handling URI
	 */
	@GET
	@Path(USER)
	@Produces(APPLICATION_JSON)
	Map<String, URI> listUsers(@Context UriInfo ui);

	/**
	 * Create a new user.
	 *
	 * @param user
	 *            Description of user to create. Username must be unique.
	 * @param ui
	 *            For building URIs.
	 * @return REST response (CREATED on success)
	 */
	@POST
	@Path(USER)
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	Response createUser(@Valid UserRecord user, @Context UriInfo ui);

	/**
	 * Read a particular user's details.
	 *
	 * @param id
	 *            The ID of the user
	 * @param ui
	 *            For building URIs.
	 * @return Description of the user.
	 */
	@GET
	@Path(USER + "/{id}")
	@Produces(APPLICATION_JSON)
	UserRecord describeUser(@PathParam("id") int id, @Context UriInfo ui);

	/**
	 * Update a particular user's details.
	 *
	 * @param id
	 *            The ID of the user
	 * @param user
	 *            What to set the details to. {@code null} fields are ignored.
	 * @param ui
	 *            For building URIs.
	 * @param security
	 *            Used to check who the current user actually is.
	 * @return The updated user details.
	 */
	@PUT
	@Path(USER + "/{id}")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	UserRecord updateUser(@PathParam("id") int id, @Valid UserRecord user,
			@Context UriInfo ui, @Context SecurityContext security);

	/**
	 * Delete a user.
	 *
	 * @param id
	 *            The ID of the user
	 * @param security
	 *            Used to check who the current user actually is.
	 * @return Description of what user was deleted.
	 */
	@DELETE
	@Path(USER + "/{id}")
	@Produces(TEXT_PLAIN)
	String deleteUser(@PathParam("id") int id,
			@Context SecurityContext security);

	/**
	 * List the groups and the URIs used to describe and manipulate them.
	 *
	 * @param ui
	 *            For building URIs.
	 * @return A sorted map from group name to details-handling URI
	 */
	@GET
	@Path(GROUP)
	@Produces(APPLICATION_JSON)
	Map<String, URI> listGroups(@Context UriInfo ui);

	/**
	 * Create a new group.
	 *
	 * @param group
	 *            Description of group to create. Group name must be unique.
	 * @param ui
	 *            For building URIs.
	 * @return REST response (CREATED on success)
	 */
	@POST
	@Path(GROUP)
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	Response createGroup(@Valid GroupRecord group, @Context UriInfo ui);

	/**
	 * Read a particular group's details.
	 *
	 * @param groupId
	 *            The ID of the group
	 * @param ui
	 *            For building URIs.
	 * @return Description of the group.
	 */
	@GET
	@Path(GROUP + "/{groupId}")
	@Produces(APPLICATION_JSON)
	GroupRecord describeGroup(@PathParam("groupId") int groupId,
			@Context UriInfo ui);

	/**
	 * Update a particular group's details. This particularly includes the name
	 * and the quota, but <em>excludes the memberships;</em> those are separate
	 * resources.
	 *
	 * @param groupId
	 *            The ID of the group
	 * @param group
	 *            The description of the group to update to be like.
	 * @param ui
	 *            For building URIs.
	 * @return Description of the group.
	 */
	@PUT
	@Path(GROUP + "/{groupId}")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	GroupRecord updateGroup(@PathParam("groupId") int groupId,
			@Valid GroupRecord group, @Context UriInfo ui);

	/**
	 * Delete a group.
	 *
	 * @param groupId
	 *            The ID of the group
	 * @return Description of what group was deleted
	 */
	@DELETE
	@Path(GROUP + "/{groupId}")
	@Produces(TEXT_PLAIN)
	String deleteGroup(@PathParam("groupId") int groupId);

	/**
	 * Add a user to a group.
	 *
	 * @param groupId
	 *            Which group to add to.
	 * @param user
	 *            Description of user to add. User name must be present.
	 * @param ui
	 *            For building URIs.
	 * @return REST response (CREATED on success)
	 */
	@POST
	@Path(GROUP + "/{groupId}/" + MEMBER)
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	Response addMember(@PathParam("groupId") int groupId,
			@Valid MemberRecord user, @Context UriInfo ui);

	/**
	 * Read a particular group's details.
	 *
	 * @param groupId
	 *            The ID of the group
	 * @param memberId
	 *            The ID of the membership.
	 * @param ui
	 *            For building URIs.
	 * @return Description of the membership.
	 */
	@GET
	@Path(GROUP + "/{groupId}/" + MEMBER + "/{memberId}")
	@Produces(APPLICATION_JSON)
	MemberRecord describeMember(@PathParam("groupId") int groupId,
			@PathParam("memberId") int memberId, @Context UriInfo ui);

	/**
	 * Delete a membership of a group.
	 *
	 * @param groupId
	 *            The ID of the group
	 * @param memberId
	 *            The ID of the membership
	 * @return REST response
	 */
	@DELETE
	@Path(GROUP + "/{groupId}/" + MEMBER + "/{memberId}")
	Response removeMember(@PathParam("groupId") int groupId,
			@PathParam("memberId") int memberId);
}
