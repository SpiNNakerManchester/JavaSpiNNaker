package uk.ac.manchester.spinnaker.messages.model;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/** The contents of IOBUF for a core */
public class IOBuffer implements HasCoreLocation {
	private final HasCoreLocation core;
	private final Object iobuf;

	/**
	 * @param core
	 *            The coordinates of a core
	 * @param contents
	 *            The contents of the buffer for the chip
	 */
	public IOBuffer(HasCoreLocation core, Object contents) {// FIXME what type for contents?
		this.core = core;
		this.iobuf = contents;
	}

	@Override
	public int getX() {
		return core.getX();
	}

	@Override
	public int getY() {
		return core.getY();
	}

	@Override
	public int getP() {
		return core.getP();
	}

	/** The contents of the buffer */
	public Object getContents() {
		return iobuf;
	}
}
