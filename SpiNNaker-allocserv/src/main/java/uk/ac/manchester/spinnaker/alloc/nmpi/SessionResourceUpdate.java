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

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * A NMPI job with only resources to be updated.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SessionResourceUpdate {

    /**
     * The status of the job.
     */
    private String status;

    /**
     * A count of how much resource has been used by the job.
     */
    private ResourceUsage resourceUsage;

    /**
     * Get the status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param statusParam the status to set
     */
    public void setStatus(final String statusParam) {
        this.status = statusParam;
    }

    /**
     * Get the resourceUsage.
     *
     * @return the resourceUsage
     */
    public ResourceUsage getResourceUsage() {
        return resourceUsage;
    }

    /**
     * Sets the resourceUsage.
     *
     * @param resourceUsageParam the resourceUsage to set
     */
    public void setResourceUsage(final ResourceUsage resourceUsageParam) {
        this.resourceUsage = resourceUsageParam;
    }
}