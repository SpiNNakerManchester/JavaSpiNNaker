package uk.ac.manchester.spinnaker.transceiver.processes;

import static java.lang.String.format;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Encapsulates exceptions from processes which communicate with some core/chip.
 */
public class ProcessException extends Exception {
	private static final long serialVersionUID = 7759365416594564702L;
	private static final String S = "     "; // five spaces

	/**
	 * Create an exception.
	 *
	 * @param core
	 *            What core were we talking to.
	 * @param cause
	 *            What exception caused problems.
	 */
	public ProcessException(HasCoreLocation core, Throwable cause) {
		super(format("\n" + S + "Received exception class: %s\n" + S
				+ "With message: %s\n" + S + "When sending to %d:%d:%d\n",
				cause.getClass().getName(), cause.getMessage(), core.getX(),
				core.getY(), core.getP()), cause);
	}
}
