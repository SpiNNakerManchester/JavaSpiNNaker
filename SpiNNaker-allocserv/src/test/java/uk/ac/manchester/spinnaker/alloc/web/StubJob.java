/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
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
public abstract class StubJob implements SpallocAPI.Job {
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
}
