/*
 * Copyright (c) 2025 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.web;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.model.CPUState;

/** A process in a list of processes. */
public class Process {

    private CPUInfo info;

    /**
     * Construct a Process based on the provided CPU info.
     *
     * @param info
     *     Information for CPU.
     */
    public Process(CPUInfo info) {
        this.info = info;
    }

    /**
     * {@return The state of the last info}
     */
    public int getVirtualId() {
        return info.getP();
    }

    /**
     * {@return The state of the last info}
     */
    public int getPhysicalId() {
        return info.getPhysicalCPUID();
    }

    /**
     * {@return The state of the last info}
     */
    public String getState() {
        return info.getState().name();
    }

    /**
     * {@return The application name used in this process}
     */
    public String getApplication() {
        return info.getApplicationName();
    }

    /**
     * {@return The application ID used in this process}
     */
    public int getAppId() {
        return info.getApplicationID();
    }

    /**
     * {@return Time this process started}
     */
    public String getStart() {
        var instant = Instant.ofEpochSecond(info.getTime());
        var date = Date.from(instant);
        var formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
        return formatter.format(date);
    }

    /**
     * {@return True it the CPU state is RUN_TIME_EXCEPTION}
     */
    public boolean isRte() {
        return info.getState() == CPUState.RUN_TIME_EXCEPTION;
    }

    /**
     * {@return Name of the runtime error or empty if no error}
     */
    public String getRteName() {
        if (!isRte()) {
            return "";
        }
        return info.getRunTimeError().name();
    }

    /**
     * {@return Concatenation of the registers or empty if no rubn time error}
     */
    public String getRteRegisters() {
        if (!isRte()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        var registers = info.getRegisters();
        for (var r = 0; r < registers.length; r++) {
            sb.append(String.format("R%d: %08x ", r, registers[r]));
        }
        return sb.toString();
    }
}
