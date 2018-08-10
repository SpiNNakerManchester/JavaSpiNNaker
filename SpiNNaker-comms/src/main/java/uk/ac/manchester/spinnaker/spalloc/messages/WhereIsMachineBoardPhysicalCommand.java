package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to get the location of a board in a machine by physical location.
 */
public class WhereIsMachineBoardPhysicalCommand extends Command<Integer> {
	/**
	 * Create a request to locate a board on a machine.
	 *
	 * @param machine
	 *            The machine to request about.
	 * @param cabinet
	 *            The cabinet containing the board to ask about.
	 * @param frame
	 *            The frame containing the board to ask about.
	 * @param board
	 *            The board (within the cabinet and frame) to ask about.
	 */
	public WhereIsMachineBoardPhysicalCommand(String machine, int cabinet,
			int frame, int board) {
		super("where_is");
		addKwArg("machine", machine);
		addKwArg("cabinet", cabinet);
		addKwArg("frame", frame);
		addKwArg("board", board);
	}

	/**
	 * Create a request to locate a board on a machine.
	 *
	 * @param machine
	 *            The machine to request about.
	 * @param coords
	 *            The physical coordinates of the board to ask about.
	 */
	public WhereIsMachineBoardPhysicalCommand(String machine,
			BoardPhysicalCoordinates coords) {
		super("where_is");
		addKwArg("machine", machine);
		addKwArg("cabinet", coords.getCabinet());
		addKwArg("frame", coords.getFrame());
		addKwArg("board", coords.getBoard());
	}
}
