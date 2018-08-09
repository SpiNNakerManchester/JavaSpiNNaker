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
		addKwArg("cabinet", coords.getCabinet());
		addKwArg("frame", coords.getFrame());
		addKwArg("board", coords.getBoard());
	}
}
