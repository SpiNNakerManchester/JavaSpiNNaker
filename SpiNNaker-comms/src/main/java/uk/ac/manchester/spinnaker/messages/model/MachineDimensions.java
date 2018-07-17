package uk.ac.manchester.spinnaker.messages.model;

/** Represents the size of a machine in chips. */
public final class MachineDimensions {
	/** The width of the machine in chips */
	public final int width;
	/** The height of the machine in chips */
	public final int height;

	public MachineDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof MachineDimensions)
				&& equals((MachineDimensions) o);
	}

	/**
	 * Tests whether this object is equal to another dimension.
	 *
	 * @param d
	 *            The other dimension object to compare to.
	 * @return True exactly when they are equal.
	 */
	public boolean equals(MachineDimensions d) {
		return width == d.width && height == d.height;
	}

	@Override
	public int hashCode() {
		return width << 16 | height;
	}
}