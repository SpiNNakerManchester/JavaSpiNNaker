/*
 * Copyright (c) 2018-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.front_end.download.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.front_end.dse.FastExecuteDataSpecification;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;

/**
 * Data speed up packet gatherer description.
 *
 * @author Christian-B
 */
@JsonFormat(shape = OBJECT)
public class Gather implements HasCoreLocation {

    
    private static final Logger log =
            getLogger(FastExecuteDataSpecification.class);
    
    /** The x value of the core this placement is on. */
    private final int x;
    /** The y value of the core this placement is on. */
    private final int y;
    /** The p value of the core this placement is on. */
    private final int p;
    /** The IPTag of the package gatherer. */
    private final IPTag iptag;
    /** The extra monitor cores, and what to retrieve from them. */
    private final List<Monitor> monitors;
    /** The current transaction id for the board. */
    private int transactionId;

	/**
	 * Constructor with minimum information needed.
	 * <p>
	 * Could be called from an unmarshaller.
	 *
	 * @param x
	 *            Gatherer X coordinate.
	 * @param y
	 *            Gatherer Y coordinate.
	 * @param p
	 *            Gatherer processor ID.
	 * @param iptag
	 *            Information about IPtag for the gatherer to use.
	 * @param monitors
	 *            What information to retrieve and from where. This should be
	 *            information about the extra monitor cores that have been
	 *            placed on the same board as this data speed up packet
	 *            gatherer.
	 */
    Gather(@JsonProperty(value = "x", required = true) int x,
            @JsonProperty(value = "y", required = true) int y,
            @JsonProperty(value = "p", required = true) int p,
            @JsonProperty(value = "iptag", required = true) IPTag iptag,
            @JsonProperty(value = "monitors", required = true)
                    List<Monitor> monitors) {
        this.x = x;
        this.y = y;
        this.p = p;
        this.iptag = iptag;
        this.monitors = monitors;
        this.transactionId = 0;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getP() {
        return p;
    }

    public int getTransactionId() {
    	return transactionId;
    }

    /** sets the transaction id to a new value.
     * @param newTransactionId
     *          the new value to set the transaction id to.
     */
    public void setTransactionId(int newTransactionId) {
        log.debug("the new transaction id is {}", newTransactionId);
        transactionId = newTransactionId;
    }

    /**
     * @return the iptag
     */
    public IPTag getIptag() {
        return iptag;
    }

    /**
     * @return the monitors
     */
    public List<Monitor> getMonitors() {
        return Collections.unmodifiableList(monitors);
    }
}
