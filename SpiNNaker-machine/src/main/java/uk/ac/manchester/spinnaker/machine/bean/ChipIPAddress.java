/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.InetAddress;
import java.net.UnknownHostException;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;

/**
 *
 * @author Christian-B
 */
@JsonPropertyOrder({
		"x", "y", "ipAddress"
})
@JsonFormat(shape = ARRAY)
public class ChipIPAddress {
	public final ChipLocation location;
    public final InetAddress ipAddress;

    @JsonCreator
    public ChipIPAddress(
            @JsonProperty(value = "x", required=true) int x,
            @JsonProperty(value = "y", required=true) int y,
            @JsonProperty(value = "ipAddress", required=true)
                    String ipAddress) throws UnknownHostException {
        location = new ChipLocation(x, y);
        this.ipAddress = InetAddress.getByName(ipAddress);
    }

    public ChipLocation getLocation() {
        return location;
    }

    /**
     * @return the direction
     */
    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public String toString() {
		return location + ", " + ipAddress;
    }
}
