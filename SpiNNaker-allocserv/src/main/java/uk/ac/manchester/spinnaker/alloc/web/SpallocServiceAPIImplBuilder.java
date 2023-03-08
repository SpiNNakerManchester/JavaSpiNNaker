/*
 * Copyright (c) 2021 The University of Manchester
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

import static java.util.Objects.isNull;
import static javax.ws.rs.core.Response.accepted;
import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_APPLICATION;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;

import java.util.Optional;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.SubMachine;
import uk.ac.manchester.spinnaker.alloc.model.Prototype;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.BadArgs;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.EmptyResponse;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.ItsGone;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.NotFound;
import uk.ac.manchester.spinnaker.alloc.web.SpallocServiceAPI.JobAPI;
import uk.ac.manchester.spinnaker.alloc.web.SpallocServiceAPI.MachineAPI;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;

/**
 * Manufactures instances of {@link MachineAPI} and {@link JobAPI}. This
 * indirection allows Spring to insert any necessary security interceptors on
 * methods.
 * <p>
 * Do not call the {@link #machine(Machine,UriInfo) machine()} and
 * {@link #job(Job,String,Permit,UriInfo) job()} methods directly. Use the
 * factories.
 */
@Configuration
@Role(ROLE_SUPPORT)
class SpallocServiceAPIImplBuilder extends BackgroundSupport {
	private static final Logger log =
			getLogger(SpallocServiceAPIImplBuilder.class);

	@Autowired
	private SpallocAPI core;

	@Autowired
	private JsonMapper mapper;

	@Autowired
	private SpallocProperties props;

	@Value("${spring.mvc.servlet.path}")
	private String servletPath;

	/**
	 * Make a machine access interface.
	 *
	 * @param machine
	 *            The machine object to wrap
	 * @param ui
	 *            How the API was accessed
	 * @return A machine access interface. Will be wrapped by Spring with
	 *         security support (if needed).
	 */
	@Bean
	@Prototype
	@Role(ROLE_APPLICATION)
	public MachineAPI machine(Machine machine, UriInfo ui) {
		return new MachineAPI() {
			@Override
			public void describeMachine(boolean wait, AsyncResponse response) {
				if (wait) {
					bgAction(response, () -> {
						log.debug("starting wait for change of machine");
						machine.waitForChange(props.getWait());
						/*
						 * Assume that machines don't change often enough for us
						 * to care about whether they vanish; therefore handle
						 * still valid after wait
						 */
						return new MachineResponse(machine, ui);
					});
				} else {
					fgAction(response, () -> new MachineResponse(machine, ui));
				}
			}

			private WhereIsResponse
					makeResponse(Optional<BoardLocation> boardLocation) {
				return new WhereIsResponse(
						boardLocation.orElseThrow(
								() -> new NotFound("failed to locate board")),
						ui);
			}

			@Override
			public WhereIsResponse whereIsLogicalPosition(int x, int y, int z) {
				return makeResponse(machine
						.getBoardByLogicalCoords(new TriadCoords(x, y, z)));
			}

			@Override
			public WhereIsResponse whereIsPhysicalPosition(int cabinet,
					int frame, int board) {
				return makeResponse(machine.getBoardByPhysicalCoords(
						new PhysicalCoords(cabinet, frame, board)));
			}

			@Override
			public WhereIsResponse whereIsMachineChipLocation(int x, int y) {
				return makeResponse(
						machine.getBoardByChip(new ChipLocation(x, y)));
			}

			@Override
			public WhereIsResponse whereIsIPAddress(String address) {
				return makeResponse(machine.getBoardByIPAddress(address));
			}
		};
	}

	/**
	 * Make a job access interface.
	 *
	 * @param j
	 *            The job object to wrap
	 * @param caller
	 *            What host made the call? Used in keepalive tracking.
	 * @param permit
	 *            The security permissions pertinent to the job
	 * @param ui
	 *            How the API was accessed
	 * @return A machine access interface. Will be wrapped by Spring with
	 *         security support (if needed).
	 */
	@Bean
	@Prototype
	@Role(ROLE_APPLICATION)
	public JobAPI job(Job j, String caller, Permit permit, UriInfo ui) {
		var sp = props.getProxy().isEnable() ? servletPath : null;
		return new JobAPI() {
			@Override
			public String keepAlive(String reqBody) {
				log.debug("keeping job {} alive: {}", j.getId(), caller);
				j.access(caller);
				return "ok";
			}

			@Override
			public void getState(boolean wait, AsyncResponse response) {
				if (wait) {
					bgAction(response, permit, () -> {
						log.debug("starting wait for change of job");
						j.waitForChange(props.getWait());
						// Refresh the handle
						var nj = core.getJob(permit, j.getId())
								.orElseThrow(() -> new ItsGone("no such job"));
						return new JobStateResponse(nj, ui, mapper, sp);
					});
				} else {
					fgAction(response, () -> new JobStateResponse(j, ui, mapper,
							sp));
				}
			}

			@Override
			public Response deleteJob(String reason) {
				if (isNull(reason)) {
					reason = "unspecified";
				}
				j.destroy(reason);
				return noContent().build();
			}

			private SubMachine allocation() {
				return j.getMachine().orElseThrow(
						// No machine allocated yet
						EmptyResponse::new);
			}

			@Override
			public SubMachineResponse getMachine() {
				j.access(caller);
				return new SubMachineResponse(allocation(), ui);
			}

			@Override
			public MachinePower getMachinePower() {
				j.access(caller);
				return new MachinePower(allocation().getPower());
			}

			@Override
			public void setMachinePower(MachinePower reqBody,
					AsyncResponse response) {
				// Async because it involves getting a write lock
				if (isNull(reqBody)) {
					throw new BadArgs("bad power description");
				}
				bgAction(response, permit, () -> {
					j.access(caller);
					allocation().setPower(reqBody.getPower());
					return accepted().build();
				});
			}

			@Override
			public WhereIsResponse getJobChipLocation(int x, int y) {
				j.access(caller);
				var loc = j.whereIs(x, y).orElseThrow(EmptyResponse::new);
				return new WhereIsResponse(loc, ui);
			}

			@Override
			public void reportBoardIssue(IssueReportRequest reqBody,
					AsyncResponse response) {
				// Async because it involves getting a write lock
				if (isNull(reqBody)) {
					throw new BadArgs("bad issue description");
				}
				bgAction(response, () -> new IssueReportResponse(
						j.reportIssue(reqBody, permit)));
			}
		};
	}
}
