package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Describes a connection by its chip and hostname.
 */
@JsonPropertyOrder({
		"chip", "hostname"
})
@JsonFormat(shape = ARRAY)
public final class Connection {
	private Chip chip;
	private String hostname;

	/**
	 * Create with defaults.
	 */
	public Connection() {
		chip = new Chip();
		hostname = "";
	}

	/**
	 * Create.
	 *
	 * @param chip
	 *            the chip
	 * @param hostname
	 *            the host
	 */
	public Connection(Chip chip, String hostname) {
		this.chip = chip;
		this.hostname = hostname;
	}

	/**
	 * Create.
	 *
	 * @param chip
	 *            the chip
	 * @param hostname
	 *            the host
	 */
	public Connection(HasChipLocation chip, String hostname) {
		this.chip = new Chip(chip.getX(), chip.getY());
		this.hostname = hostname;
	}

	public Chip getChip() {
		return chip;
	}

	public void setChip(Chip chip) {
		this.chip = chip;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && other instanceof Connection) {
			Connection c = (Connection) other;
			return chip.equals(c.chip) && hostname.equals(c.hostname);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 5 * hostname.hashCode() + 7 * chip.hashCode();
	}

	@Override
	public String toString() {
		return "Connection(" + chip + "@" + hostname + ")";
	}
}
