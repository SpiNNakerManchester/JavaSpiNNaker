/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Extra monitor core information.
 *
 * @author Christian-B
 */
@JsonFormat(shape = OBJECT)
class Monitor implements HasCoreLocation {
    /** The X coordinate of the core this monitor is placed on. */
    final int x;
    /** The Y coordinate of the core this monitor is placed on. */
    final int y;
    /** The processor ID of the core this monitor is placed on. */
    final int p;
    /** The vertex placements that this monitor will read. */
    private final List<Placement> placements;

    /**
	 * Constructor with minimum information needed.
	 * <p>
	 * Could be called from an unmarshaller.
	 *
	 * @param x
	 *            Monitor X coordinate.
	 * @param y
	 *            Monitor Y coordinate.
	 * @param p
	 *            Monitor processor ID.
	 * @param placements
	 *            Vertex placement info handled by this monitor.
	 */
    Monitor(@JsonProperty(value = "x", required = true) int x,
            @JsonProperty(value = "y", required = true) int y,
            @JsonProperty(value = "p", required = true) int p,
            @JsonProperty(value = "placements", required = true)
                    List<Placement> placements) {
        this.x = x;
        this.y = y;
        this.p = p;
        this.placements = placements;
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

    /**
     * @return the placements
     */
    public List<Placement> getPlacements() {
        return Collections.unmodifiableList(placements);
    }
}
