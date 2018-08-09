package uk.ac.manchester.spinnaker.spalloc.commands;

/**
 * Request to get the location of a board in a machine by logical location.
 */
public class WhereIsMachineBoardLogicalCommand extends Command<Integer> {
	/**
	 * Create a request to locate a board on a machine.
	 *
	 * @param machine
	 *            The machine to request about.
	 * @param x
	 *            The logical X coordinate of the board to ask about.
	 * @param y
	 *            The logical Y coordinate of the board to ask about.
	 * @param z
	 *            The logical Z coordinate of the board to ask about.
	 */
	public WhereIsMachineBoardLogicalCommand(String machine, int x, int y,
			int z) {
		super("where_is");
		addKwArg("machine", machine);
		addKwArg("x", x);
		addKwArg("y", y);
		addKwArg("z", z);
	}
}
