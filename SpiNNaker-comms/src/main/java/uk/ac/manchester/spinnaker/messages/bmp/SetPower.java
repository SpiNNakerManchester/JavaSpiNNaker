package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.Constants.MS_PER_S;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_BMP_POWER;

import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP request for the BMP to power on or power off a rack of boards.
 * <p>
 * <b>Note:</b> There is currently a bug in the BMP that means some boards don't
 * respond to power commands not sent to BMP 0. Because of this, this message
 * should <i>always</i> be sent to BMP 0!
 */
public class SetPower extends BMPRequest<SetPower.Response> {
	private static final int DELAY_SHIFT = 16;

	/**
	 * @param powerCommand
	 *            The power command being sent
	 * @param board
	 *            The board on the backplane to power on or off
	 */
	public SetPower(PowerCommand powerCommand, int board) {
		super(0, CMD_BMP_POWER, argument1(0.0, powerCommand), argument2(board),
				null);
	}

	/**
	 * @param powerCommand
	 *            The power command being sent
	 * @param boards
	 *            The boards on the same backplane to power on or off
	 * @param delay
	 *            Number of seconds delay between power state changes of the
	 *            different boards.
	 */
	public SetPower(PowerCommand powerCommand, Collection<Integer> boards,
			double delay) {
		super(0, CMD_BMP_POWER, argument1(delay, powerCommand),
				argument2(boards), null);
	}

	/**
	 * @param powerCommand
	 *            The power command being sent
	 * @param board
	 *            The board on the backplane to power on or off
	 * @param boardToSendTo
	 *            The optional board to send the command to if this is to be
	 *            sent to a frame of boards.
	 */
	@Deprecated
	public SetPower(PowerCommand powerCommand, int board, int boardToSendTo) {
		super(boardToSendTo, CMD_BMP_POWER, argument1(0.0, powerCommand),
				argument2(board), null);
	}

	/**
	 * @param powerCommand
	 *            The power command being sent
	 * @param boards
	 *            The boards on the same backplane to power on or off
	 * @param delay
	 *            Number of seconds delay between power state changes of the
	 *            different boards.
	 * @param boardToSendTo
	 *            The optional board to send the command to if this is to be
	 *            sent to a frame of boards.
	 */
	@Deprecated
	public SetPower(PowerCommand powerCommand, Collection<Integer> boards,
			double delay, int boardToSendTo) {
		super(boardToSendTo, CMD_BMP_POWER, argument1(delay, powerCommand),
				argument2(boards), null);
	}

	private static int argument1(double delay, PowerCommand powerCommand) {
		return ((int) (delay * MS_PER_S) << DELAY_SHIFT) | powerCommand.value;
	}

	private static int argument2(int board) {
		return 1 << board;
	}

	private static int argument2(Collection<Integer> boards) {
		return boards.stream().mapToInt(board -> 1 << board).sum();
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer) throws Exception {
		return new Response(buffer);
	}

	/** The response from the powering message. */
	public final class Response extends BMPRequest.BMPResponse {
		private Response(ByteBuffer buffer)
				throws UnexpectedResponseCodeException {
			super("powering request", CMD_BMP_POWER, buffer);
		}
	}
}
