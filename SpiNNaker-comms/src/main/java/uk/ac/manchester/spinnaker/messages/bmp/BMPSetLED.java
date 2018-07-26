package uk.ac.manchester.spinnaker.messages.bmp;

import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_LED;

import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

public class BMPSetLED extends BMPRequest<BMPSetLED.Response> {

	public BMPSetLED(int led, LEDAction action, int board) {
		super(board, CMD_LED, argument1(action, led), argument2(board), null);
	}

	public BMPSetLED(Collection<Integer> leds, LEDAction action, int board) {
		super(board, CMD_LED, argument1(action, leds), argument2(board), null);
	}

	public BMPSetLED(int led, LEDAction action, Collection<Integer> boards) {
		super(boards, CMD_LED, argument1(action, led), argument2(boards), null);
	}

	public BMPSetLED(Collection<Integer> leds, LEDAction action,
			Collection<Integer> boards) {
		super(boards, CMD_LED, argument1(action, leds), argument2(boards),
				null);
	}

	private static int argument1(LEDAction action, int led) {
		return action.value << (led * 2);
	}

	private static int argument1(LEDAction action, Collection<Integer> leds) {
		return leds.stream().mapToInt(led -> action.value << (led * 2)).sum();
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

	public class Response extends BMPRequest.BMPResponse {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Set the LEDs of a board", CMD_LED, buffer);
		}
	}
}
