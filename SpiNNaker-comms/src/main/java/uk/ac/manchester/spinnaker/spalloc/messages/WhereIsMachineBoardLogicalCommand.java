package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to get the location of a board in a machine by logical location.
 */
public class WhereIsMachineBoardLogicalCommand extends Command<Integer> {
	/**
	 * Create a request to locate a board on a machine.
	 *
	 * @param machine
	 *            The machine to request about.
	 * @param coords
	 *            The logical coordinates of the board to ask about.
	 */
	public WhereIsMachineBoardLogicalCommand(String machine,
			BoardCoordinates coords) {
		super("where_is");
		addKwArg("machine", machine);
		addKwArg("x", coords.getX());
		addKwArg("y", coords.getY());
		addKwArg("z", coords.getZ());
	}
}
