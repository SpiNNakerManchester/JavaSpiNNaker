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
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Vertex placement information.
 *
 * @author Christian-B
 */
@JsonFormat(shape = OBJECT)
public class Placement implements HasCoreLocation {
    /** The X coordinate of the core this vertex is placed on. */
	private final int x;
    /** The Y coordinate of the core this vertex is placed on. */
	private final int y;
    /** The processor ID of the core this vertex is placed on. */
	private final int p;
    /** Minimal vertex info. */
	private final Vertex vertex;

	/**
	 * Constructor with minimum information needed.
	 * <p>
	 * Could be called from an unmarshaller.
	 *
	 * @param x
	 *            Vertex X coordinate.
	 * @param y
	 *            Vertex Y coordinate.
	 * @param p
	 *            Vertex processor ID.
	 * @param vertex
	 *            Vertex recording region information.
	 */
    Placement(@JsonProperty(value = "x", required = true) int x,
            @JsonProperty(value = "y", required = true) int y,
            @JsonProperty(value = "p", required = true) int p,
            @JsonProperty(value = "vertex", required = true) Vertex vertex) {
        this.x = x;
        this.y = y;
        this.p = p;
        this.vertex = vertex;
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

    /** @return The information about the vertex. */
    public Vertex getVertex() {
        return vertex;
    }
}
