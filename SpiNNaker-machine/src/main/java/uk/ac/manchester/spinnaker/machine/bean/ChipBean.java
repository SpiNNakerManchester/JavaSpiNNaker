/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;

/**
 *
 * @author Christian-B
 */
//@JsonPropertyOrder({
//		"x", "y", "details", "resources",
//})
@JsonFormat(shape = ARRAY)
public class ChipBean {
	public final ChipLocation location;
    public final ChipDetails details;
    public ChipResources resources;

    @JsonCreator
    public ChipBean(
            @JsonProperty(value = "x", required=true) int x,
            @JsonProperty(value = "y", required=true) int y,
            @JsonProperty(value = "details", required=true) ChipDetails details,
            @JsonProperty(value = "resources", required=false)
                    ChipResources resources) {
        location = new ChipLocation(x, y);
        this.details = details;
        this.resources = resources;
    }

    public ChipLocation getLocation() {
        return location;
    }

    /**
     * @return the resources
     */
    public ChipResources  getResources() {
        return resources;
    }

    /**
     * @return the resources
     */
    public ChipDetails  getDetails() {
        return details;
    }

    public String toString() {
        if (resources != null) {
            return location + ", " + details + " " + resources;
        } else {
            return location + ", " + details + " DEFAULTS";
        }
    }

    public void addDefaults(ChipResources defaults) {
        if (resources == null) {
            resources = defaults;
        }
        resources.addDefaults(defaults);
    }
}
