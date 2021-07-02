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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static uk.ac.manchester.spinnaker.alloc.SecurityConfig.IS_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.Paths.USER;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.springframework.security.access.prepost.PreAuthorize;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.Hidden;
import uk.ac.manchester.spinnaker.alloc.IPAddress;
import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException;

/**
 * Administration interface.
 *
 * @author Donal Fellows
 */
@Hidden
@PreAuthorize(IS_ADMIN)
public interface AdminAPI {
	/**
	 * Common paths in the interface.
	 */
	interface Paths {
		/** The location of the admin "service". */
		String BASE_PATH = "/admin";

		/** Service root. */
		String ROOT = "/";

		/** Machine definition importing. */
		String IMPORT = "import";

		/** Board control. */
		String BOARD = "board";

		/** User account control. */
		String USER = "users";
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
			UriBuilder b = ui.getAbsolutePathBuilder().path("{resource}");
			importMachines = b.build(Paths.IMPORT);
			setBoardEnabled = b.build(Paths.BOARD);
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
	@Path(Paths.IMPORT)
	default String describeImport() {
		return "This resource only really supports POST.";
	}

	/**
	 * Import a machine definition from a file.
	 *
	 * @param filename
	 *            What file to load from.
	 */
	@POST
	@Path(Paths.IMPORT)
	void importMachinesFromFile(
			@QueryParam("filename") @NotBlank String filename);

	/**
	 * Import a machine definition by posting it.
	 *
	 * @param definitions
	 *            The definitions.
	 */
	@POST
	@Consumes(APPLICATION_JSON)
	@Path(Paths.IMPORT)
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
	 * @throws SQLException
	 *             On a serious problem
	 * @throws RequestFailedException
	 *             If bad query combinations are supplied
	 */
	@GET
	@Produces(TEXT_PLAIN)
	@Path(Paths.BOARD)
	@SuppressWarnings("checkstyle:parameternumber")
	default boolean getBoardState(
			@NotBlank(message = "machine name is required")
			@QueryParam("machine") String machineName,
			@QueryParam("x") Integer x, @QueryParam("y") Integer y,
			@QueryParam("z") Integer z, @QueryParam("cabinet") Integer c,
			@QueryParam("frame") Integer f, @QueryParam("board") Integer b,
			@QueryParam("address") String address) throws SQLException {
		if (x != null && y != null && z != null) {
			return getBoardStateXYZ(machineName, x, y, z);
		}
		if (c != null && f != null && b != null) {
			return getBoardStateCFB(machineName, c, f, b);
		}
		if (address != null) {
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
	 * @throws SQLException
	 *             On a serious problem
	 */
	boolean getBoardStateXYZ(String name, @PositiveOrZero int x,
			@PositiveOrZero int y, @PositiveOrZero int z) throws SQLException;

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
	 * @throws SQLException
	 *             On a serious problem
	 */
	boolean getBoardStateCFB(String name, @PositiveOrZero int c,
			@PositiveOrZero int f, @PositiveOrZero int b) throws SQLException;

	/**
	 * Find board by IP address and return its state.
	 *
	 * @param name
	 *            The name of the machine
	 * @param address
	 *            The IP address of the board (dotted quad)
	 * @return Whether the board is enabled
	 * @throws SQLException
	 *             On a serious problem
	 */
	boolean getBoardStateAddress(String name, @IPAddress String address)
			throws SQLException;

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
	 * @throws SQLException
	 *             On a serious problem
	 * @throws RequestFailedException
	 *             If bad query combinations are supplied
	 */
	@PUT
	@Consumes(TEXT_PLAIN)
	@Produces(TEXT_PLAIN)
	@Path(Paths.BOARD)
	@SuppressWarnings("checkstyle:parameternumber")
	default boolean setBoardState(
			@NotBlank(message = "machine name is required")
			@QueryParam("machine") String machineName,
			@QueryParam("x") Integer x, @QueryParam("y") Integer y,
			@QueryParam("z") Integer z, @QueryParam("cabinet") Integer c,
			@QueryParam("frame") Integer f, @QueryParam("board") Integer b,
			@QueryParam("address") String address, boolean enabled)
			throws SQLException {
		if (x != null && y != null && z != null) {
			return setBoardStateXYZ(machineName, x, y, z, enabled);
		}
		if (c != null && f != null && b != null) {
			return setBoardStateCFB(machineName, c, f, b, enabled);
		}
		if (address != null) {
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
	 * @throws SQLException
	 *             On a serious problem
	 */
	boolean setBoardStateXYZ(@NotBlank String name,
			@NotNull @PositiveOrZero int x, @NotNull @PositiveOrZero int y,
			@NotNull @PositiveOrZero int z, boolean enabled)
			throws SQLException;

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
	 * @throws SQLException
	 *             On a serious problem
	 */
	boolean setBoardStateCFB(@NotBlank String name,
			@NotNull @PositiveOrZero int c, @NotNull @PositiveOrZero int f,
			@NotNull @PositiveOrZero int b, boolean enabled)
			throws SQLException;

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
	 * @throws SQLException
	 *             On a serious problem
	 */
	boolean setBoardStateAddress(@NotBlank String name,
			@IPAddress String address, boolean enabled) throws SQLException;

	/**
	 * The description of a user. POD class.
	 */
	class User {
		/**
		 * The user identifier. Read-only; cannot be set.
		 */
		@JsonInclude(NON_NULL)
		public Integer userId;

		/**
		 * The user's username.
		 */
		public @NotBlank String userName;

		/**
		 * The user's password. <em>Never</em> returned, but may be written.
		 */
		@JsonInclude(NON_NULL)
		public String password; // Set only

		/**
		 * Whether the user has a password set. If they don't, they'll have to
		 * log in by other mechanisms (e.g., HBP/EBRAINS OpenID Connect).
		 */
		public Boolean hasPassword;

		/**
		 * Whether this account is enabled. Disabled accounts <em>cannot</em>
		 * use the service.
		 */
		public Boolean isEnabled;

		/**
		 * Whether this account is temporarily locked. Locked accounts should
		 * unlock automatically after 24 hours.
		 */
		public Boolean isLocked;

		/** The permissions of the account. */
		public TrustLevel trustLevel;

		/**
		 * The quota map of the account, saying how many board-seconds can be
		 * used on each machine.
		 */
		public Map<String, Long> quota;

		/**
		 * The time that the last successful login was. Read-only; cannot be
		 * set.
		 */
		@JsonInclude(NON_NULL)
		public Instant lastSuccessfulLogin;

		/**
		 * The time that the last failed login was. Read-only; cannot be set.
		 */
		@JsonInclude(NON_NULL)
		public Instant lastFailedLogin;
	}

	/**
	 * List the usernames and the URIs used to describe and manipulate them.
	 *
	 * @param ui
	 *            For building URIs.
	 * @return A sorted map from username to details-handling URI
	 * @throws SQLException
	 *             If DB access fails.
	 */
	@GET
	@Path(USER)
	@Produces(APPLICATION_JSON)
	Map<String, URI> listUsers(@Context UriInfo ui) throws SQLException;

	/**
	 * Create a new user.
	 *
	 * @param user
	 *            Description of user to create. Username must be unique.
	 * @param ui
	 *            For building URIs.
	 * @return REST response (CREATED on success)
	 * @throws SQLException
	 *             If DB access fails.
	 */
	@POST
	@Path(USER)
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	Response createUser(User user, @Context UriInfo ui) throws SQLException;

	/**
	 * Read a particular user's details.
	 *
	 * @param id
	 *            The ID of the user
	 * @return Description of the user.
	 * @throws SQLException
	 *             If DB access fails.
	 */
	@GET
	@Path(USER + "/{id}")
	@Produces(APPLICATION_JSON)
	User describeUser(@PathParam("id") int id) throws SQLException;

	/**
	 * Update a particular user's details
	 *
	 * @param id
	 *            The ID of the user
	 * @param user
	 *            What to set the details to. {@code null} fields are ignored.
	 * @param security
	 *            Used to check who the current user actually is.
	 * @return The updated user details.
	 * @throws SQLException
	 *             If DB access fails.
	 */
	@PUT
	@Path(USER + "/{id}")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	User updateUser(@PathParam("id") int id, User user,
			@Context SecurityContext security) throws SQLException;

	/**
	 * Delete a user.
	 *
	 * @param id
	 *            The ID of the user
	 * @param security
	 *            Used to check who the current user actually is.
	 * @return REST response
	 * @throws SQLException
	 *             If DB access fails.
	 */
	@DELETE
	@Path(USER + "/{id}")
	Response deleteUser(@PathParam("id") int id,
			@Context SecurityContext security) throws SQLException;
}
