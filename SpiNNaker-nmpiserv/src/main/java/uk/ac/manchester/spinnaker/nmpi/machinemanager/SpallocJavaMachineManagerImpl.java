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
package uk.ac.manchester.spinnaker.nmpi.machinemanager;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.UriBuilder;

import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.spinnaker.nmpi.model.machine.ChipCoordinates;
import uk.ac.manchester.spinnaker.nmpi.model.machine.SpinnakerMachine;
import uk.ac.manchester.spinnaker.alloc.client.CreateJob;
import uk.ac.manchester.spinnaker.alloc.client.SpallocClient;
import uk.ac.manchester.spinnaker.alloc.client.SpallocClientFactory;
import uk.ac.manchester.spinnaker.alloc.client.State;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * A machine manager that interfaces to the new spalloc service.
 */
public final class SpallocJavaMachineManagerImpl implements MachineManager {
	/** The version to use for the boards. */
	private static final String VERSION = "5";

	/** The reason to use when closed. */
	private static final String REASON_STOPPING = "RemoteSpiNNaker Stopping";

	/** The reason to use when a job is finished. */
	private static final String REASON_FINISHED = "Finished";

	/**
	 * The URI of the spalloc server. <em>Includes user/password
	 * credentials.</em>
	 */
	@Value("${spalloc.server}")
	private URI spallocUri;

	/** The spalloc client to use. */
	private SpallocClient client;

	/** A map between machines and spalloc jobs. */
	private Map<SpinnakerMachine, SpallocClient.Job> jobMap = new HashMap<>();

	/**
	 * The last state of a job for a given machine, so we know when it has
	 * changed.
	 */
	private Map<SpinnakerMachine, State> lastJobState = new HashMap<>();

	@PostConstruct
	private void setup() throws IOException {
		var userInfo = spallocUri.getUserInfo();
		int splitPoint = userInfo.indexOf(':');
		var username = userInfo.substring(0, splitPoint);
		var password = userInfo.substring(splitPoint + 1);
		var spalloc = UriBuilder.fromUri(spallocUri).userInfo(null).build();
		client = new SpallocClientFactory(spalloc).login(username, password);
	}

	@Override
	public void close() throws Exception {
		for (var job : jobMap.values()) {
			job.delete(REASON_STOPPING);
		}
	}

	@Override
	public List<SpinnakerMachine> getMachines() throws IOException {
		return client.listMachines().stream()
				.map(m -> new SpinnakerMachine(m.getName(), VERSION,
						m.getWidth(), m.getHeight(), m.getLiveBoardCount(),
						null))
				.collect(toList());
	}

	@Override
	public SpinnakerMachine getNextAvailableMachine(int nBoards, String owner,
			int jobId) {
		try {
			var createJob = new CreateJob(nBoards);
			createJob.setOwner(owner);
			createJob.setNmpiJobId(jobId);
			var job = client.createJob(createJob);
			job.waitForPower();
			var m = job.machine();
			var machine = new SpinnakerMachine(
					m.getConnections().get(0).getHostname(), VERSION,
					m.getWidth(), m.getHeight(), nBoards, null);
			jobMap.put(machine, job);
			lastJobState.put(machine, State.READY);
			return machine;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isMachineAvailable(SpinnakerMachine machine) {
		return jobMap.containsKey(machine);
	}

	@Override
	public boolean waitForMachineStateChange(SpinnakerMachine machine,
			int waitTime) {
		var job = jobMap.get(machine);
		if (isNull(job)) {
			return true;
		}
		var state = lastJobState.get(machine);
		try {
			var newState = job.describe(true).getState();
			lastJobState.put(machine, newState);
			return newState == state;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void releaseMachine(SpinnakerMachine machine) {
		var job = jobMap.get(machine);
		if (isNull(job)) {
			return;
		}
		try {
			job.delete(REASON_FINISHED);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		jobMap.remove(machine);
	}

	@Override
	public void setMachinePower(SpinnakerMachine machine, boolean powerOn) {
		var job = jobMap.get(machine);
		if (isNull(job)) {
			return;
		}
		try {
			job.setPower(powerOn);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ChipCoordinates getChipCoordinates(SpinnakerMachine machine, int x,
			int y) {
		var job = jobMap.get(machine);
		if (isNull(job)) {
			return null;
		}
		try {
			var whereIs =
					job.whereIs(new ChipLocation(x, y)).getPhysicalCoords();
			return new ChipCoordinates(whereIs.c(), whereIs.f(), whereIs.b());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
