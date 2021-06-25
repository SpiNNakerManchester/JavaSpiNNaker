package uk.ac.manchester.spinnaker.alloc.admin;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.net.URI;
import java.sql.SQLException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Administration interface.
 *
 * @author Donal Fellows
 */
// TODO You must have admin permission to use this
@Hidden
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
	}

	/**
	 * Where the other resources are.
	 */
	class Description {
		/** Machine definition importing. */
		public final URI importMachines;

		/** Board control. */
		public final URI setBoardEnabled;

		public Description(UriInfo ui) {
			UriBuilder b = ui.getAbsolutePathBuilder().path("{resource}");
			importMachines = b.build(Paths.IMPORT);
			setBoardEnabled = b.build(Paths.BOARD);
		}
	}

	/** Describe the admin interface. */
	@GET
	@Produces(APPLICATION_JSON)
	@Path(Paths.ROOT)
	default Description describeOperations(@Context UriInfo ui) {
		return new Description(ui);
	}

	/** "Describes" the import resource. */
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
	void importMachinesFromFile(@QueryParam("filename") String filename);

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
			MachineDefinitionLoader.Configuration definitions);

	/** Describe the enable state of a board. */
	@GET
	@Produces(TEXT_PLAIN)
	@Path(Paths.BOARD)
	default String getBoardState() {
		// TODO expand to cover addressing gamut in this description
		return "You need to supply machine= and coordinates (as "
				+ "x=&y=&z=, c=&f=&b= or address=)";
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
	@GET
	@Produces(TEXT_PLAIN)
	@Path(Paths.BOARD)
	boolean getBoardStateXYZ(@QueryParam("machine") String name,
			@QueryParam("x") int x, @QueryParam("y") int y,
			@QueryParam("z") int z) throws SQLException;

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
	@GET
	@Produces(TEXT_PLAIN)
	@Path(Paths.BOARD)
	boolean getBoardStateCFB(@QueryParam("machine") String name,
			@QueryParam("cabinet") int c, @QueryParam("frame") int f,
			@QueryParam("board") int b) throws SQLException;

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
	@GET
	@Produces(TEXT_PLAIN)
	@Path(Paths.BOARD)
	boolean getBoardStateAddress(@QueryParam("machine") String name,
			@QueryParam("address") String address) throws SQLException;

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
	@PUT
	@Consumes(TEXT_PLAIN)
	@Produces(TEXT_PLAIN)
	@Path(Paths.BOARD)
	boolean setBoardStateXYZ(@QueryParam("machine") String name,
			@QueryParam("x") int x, @QueryParam("y") int y,
			@QueryParam("z") int z, boolean enabled) throws SQLException;

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
	@PUT
	@Consumes(TEXT_PLAIN)
	@Produces(TEXT_PLAIN)
	@Path(Paths.BOARD)
	boolean setBoardStateCFB(@QueryParam("machine") String name,
			@QueryParam("cabinet") int c, @QueryParam("frame") int f,
			@QueryParam("board") int b, boolean enabled) throws SQLException;

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
	@PUT
	@Consumes(TEXT_PLAIN)
	@Produces(TEXT_PLAIN)
	@Path(Paths.BOARD)
	boolean setBoardStateAddress(@QueryParam("machine") String name,
			@QueryParam("address") String address, boolean enabled)
			throws SQLException;
}
