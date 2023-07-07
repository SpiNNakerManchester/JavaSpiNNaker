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

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Duration;

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

public class SpallocJavaMachineManagerImpl implements MachineManager {

	/**
	 * The version to use for the boards.
	 */
	private static final String VERSION = "5";

	/**
	 * The reason to use when closed.
	 */
	private static final String REASON_STOPPING = "RemoteSpiNNaker Stopping";

	/**
	 * The reason to use when a job is finished.
	 */
	private static final String REASON_FINISHED = "Finished";

	/**
	 * Keepalive interval in seconds.
	 */
	private static final int KEEPALIVE_SECONDS = 30;

	/**
	 * Keepalive interval as a duration.
	 */
	private static final Duration KEEPALIVE =
			Duration.ofSeconds(KEEPALIVE_SECONDS);

	/**
	 * The URI of the spalloc server.
	 */
	@Value("${spalloc.server}")
	private URI spallocUri;

	/**
	 * The spalloc client to use.
	 */
	private SpallocClient client;

	/**
	 * A map between machines and spalloc jobs.
	 */
	private Map<SpinnakerMachine, SpallocClient.Job> jobMap = new HashMap<>();

	/**
	 * The last state of a job for a given machine, so we know when it has
	 * changed.
	 */
	private Map<SpinnakerMachine, State> lastJobState = new HashMap<>();

	@PostConstruct
	private void setup() throws IOException {
		String userInfo = spallocUri.getUserInfo();
		int splitPoint = userInfo.indexOf(':');
		String username = userInfo.substring(0, splitPoint);
		String password = userInfo.substring(splitPoint + 1);
		URI spalloc = UriBuilder.fromUri(spallocUri).userInfo(null).build();
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
		return client.listMachines().stream().map(
				m -> new SpinnakerMachine(m.getName(), VERSION,
						m.getWidth(), m.getHeight(), m.getLiveBoardCount(),
						null))
				.collect(Collectors.toList());
	}

	@Override
	public SpinnakerMachine getNextAvailableMachine(final int nBoards,
			final String owner, final int jobId) {
		try {
			var createJob = new CreateJob(nBoards);
			createJob.setOwner(owner);
			createJob.setNmpiJobId(jobId);
			createJob.setKeepaliveInterval(KEEPALIVE);
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
	public boolean isMachineAvailable(final SpinnakerMachine machine) {
		final var job = jobMap.get(machine);
		if (isNull(job)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean waitForMachineStateChange(final SpinnakerMachine machine,
			final int waitTime) {
		final var job = jobMap.get(machine);
		if (isNull(job)) {
			return true;
		}
		final var state = lastJobState.get(machine);
		try {
			final var newState = job.describe(true).getState();
			lastJobState.put(machine, newState);
			return newState == state;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void releaseMachine(final SpinnakerMachine machine) {
		final var job = jobMap.get(machine);
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
	public void setMachinePower(final SpinnakerMachine machine,
			final boolean powerOn) {
		final var job = jobMap.get(machine);
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
	public ChipCoordinates getChipCoordinates(final SpinnakerMachine machine,
			final int x, final int y) {
		final var job = jobMap.get(machine);
		if (isNull(job)) {
			return null;
		}
		try {
			var whereIs = job.whereIs(new ChipLocation(x, y))
					.getPhysicalCoords();
			return new ChipCoordinates(whereIs.c, whereIs.f, whereIs.b);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
