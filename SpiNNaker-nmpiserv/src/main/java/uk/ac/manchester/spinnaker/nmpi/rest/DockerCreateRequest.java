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
package uk.ac.manchester.spinnaker.nmpi.rest;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A request to {@code docker create}.
 */
public class DockerCreateRequest {
    private String image;

    private List<String> cmd;

    private NetworkingConfig networkingConfig;

    /**
     * Construct the request to create the docker.
     *
     * @param image
     *            The image to instantiate.
     * @param cmd
     *            The command to run (as list of arguments).
     * @param networkName
     *               The name of the network to connect to, or null for default
     */
    public DockerCreateRequest(String image, List<String> cmd,
            String networkName) {
        this.image = image;
        this.cmd = cmd;
        if (networkName != null && !networkName.isBlank()) {
            this.networkingConfig = new NetworkingConfig(networkName);
        }
    }

    /**
     * {@return The image to instantiate}
     */
    @JsonProperty("Image")
    public String getImage() {
        return image;
    }

    /**
     * Set the image.
     *
     * @param image
     *            the image to set
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * {@return The command to run (as list of arguments)}
     */
    @JsonProperty("Cmd")
    public List<String> getCmd() {
        return cmd;
    }

    /**
     * Set the command.
     *
     * @param cmd
     *            the cmd to set
     */
    public void setCmd(List<String> cmd) {
        this.cmd = cmd;
    }

    /**
     * {@return Network configuration based on the provided name.}
     */
    @JsonProperty("NetworkingConfig")
    public NetworkingConfig getNetworkingConfig() {
        return networkingConfig;
    }

    /**
     * Networking Configuration based on an End point with this network name.
     */
    public static final class NetworkingConfig {
        private final EndpointsConfig endpointsConfig;

        /**
         * Construct a Network configuration with an end point to the name.
         *
         * @param networkName
         *     Name of the end point
         */
        public NetworkingConfig(final String networkName) {
            this.endpointsConfig = new EndpointsConfig(networkName);
        }

        /**
         * {@return The end point configuration based on the network name}
         */
        @JsonProperty("EndpointsConfig")
        public EndpointsConfig getEndpointsConfig() {
            return endpointsConfig;
        }
    }

    /**
     * Description of the endpoint based on the network name.
     */
    public static final class EndpointsConfig {
        private final String networkID;

        private EndpointsConfig(final String networkName) {
            this.networkID = networkName;
        }

        /**
         * {@return network ID to itself}
         */
        @JsonAnyGetter
        public Map<String, Object> getDynamicProperties() {
            return Map.of(networkID, Map.of("NetworkID", networkID));
        }
    }
}
