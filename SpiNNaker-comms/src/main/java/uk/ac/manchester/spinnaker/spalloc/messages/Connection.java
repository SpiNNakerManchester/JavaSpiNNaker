package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Describes a connection by its chip and hostname.
 */
@JsonPropertyOrder({
		"chip", "hostname"
})
@JsonFormat(shape = ARRAY)
public final class Connection {
	private ChipLocation chip;
	private String hostname;

	/**
	 * Create with defaults.
	 */
	public Connection() {
		chip = null;
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
	public Connection(ChipLocation chip, String hostname) {
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
		this.chip = chip.asChipLocation();
		this.hostname = hostname;
	}

	public ChipLocation getChip() {
		return chip;
	}

	/**
	 * Set the chip for the connection.
	 *
	 * @param chip
	 *            The chip to set.
	 */
	public void setChip(ChipLocationBean chip) {
		if (chip == null) {
			this.chip = null;
		} else {
			this.chip = chip.asChipLocation();
		}
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
			return Objects.equals(chip, c.chip)
                    && Objects.equals(hostname, c.hostname);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hashcode = 0;
		if (hostname != null) {
			hashcode += 5 * hostname.hashCode();
		}
		if (chip != null) {
			hashcode += 7 * chip.hashCode();
		}
		return hashcode;
	}

	@Override
	public String toString() {
		return "Connection(" + chip + "@" + hostname + ")";
	}
}
