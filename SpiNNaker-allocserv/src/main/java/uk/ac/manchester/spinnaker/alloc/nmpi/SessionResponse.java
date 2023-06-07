/*
 * Copyright (c) 2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.manchester.spinnaker.alloc.nmpi;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * A NMPI session response.
 */
public class SessionResponse {

    /**
     * The ID of the session.
     */
    private Integer id;


    /**
     * Get the id.
     *
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param idParam the id to set
     */
    public void setId(final Integer idParam) {
        this.id = idParam;
    }

    /**
     * Used for JSON serialisation;
     * ignores other properties we don't care about.
     *
     * @param name
     *            The parameter to set.
     * @param value
     *            The value to set it to.
     */
    @JsonAnySetter
    public void set(final String name, final Object value) {
        // Ignore any other values
    }
}