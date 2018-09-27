package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request the logical location of a board.
 */
public class GetBoardAtPositionCommand extends Command<Integer> {
	/**
	 * Create a request.
	 *
	 * @param machine
	 *            The machine to ask about.
	 * @param coords
	 *            The logical coordinates of the board.
	 */
	public GetBoardAtPositionCommand(String machine,
			BoardPhysicalCoordinates coords) {
		super("get_board_at_position");
		addKwArg("machine_name", machine);
                // The current spalloc server expects the param names x, y, z
		addKwArg("x", coords.getCabinet());
		addKwArg("y", coords.getFrame());
		addKwArg("z", coords.getBoard());
	}
}
