/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.SubMachine;
import uk.ac.manchester.spinnaker.alloc.model.JobState;
import uk.ac.manchester.spinnaker.alloc.proxy.ProxyCore;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * A job that just throws {@link UnsupportedOperationException} for every
 * operation. Subclass to add the behaviours you want.
 *
 * @author Donal Fellows
 */
public abstract class StubJob implements Job {
	@Override
	public void waitForChange(Duration timeout) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void access(String keepaliveAddress) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void destroy(String reason) {
		throw new UnsupportedOperationException();
	}

	@Override
	public JobState getState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Instant getStartTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> getKeepaliveHost() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Instant getKeepaliveTimestamp() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> getOwner() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<byte[]> getOriginalRequest() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Instant> getFinishTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> getReason() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<SubMachine> getMachine() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<BoardLocation> whereIs(int x, int y) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<ChipLocation> getRootChip() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Integer> getWidth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Integer> getHeight() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Integer> getDepth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String reportIssue(IssueReportRequest reqBody, Permit permit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void rememberProxy(ProxyCore proxy) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void forgetProxy(ProxyCore proxy) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean equals(Object other) {
		if (other instanceof Job) {
			var j = (Job) other;
			return getId() == j.getId();
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return getId();
	}
}
