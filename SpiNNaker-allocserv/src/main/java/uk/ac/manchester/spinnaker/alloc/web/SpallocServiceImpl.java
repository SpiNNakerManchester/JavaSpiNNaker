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

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.accepted;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.SERV;

import java.net.URI;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.Permit;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Jobs;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.SubMachine;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.BadArgs;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.EmptyResponse;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.ItsGone;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.NotFound;
import uk.ac.manchester.spinnaker.messages.model.Version;

@Service("service")
@Path(SERV)
public class SpallocServiceImpl extends BackgroundSupport
		implements SpallocServiceAPI {
	private static final Logger log = getLogger(SpallocServiceImpl.class);

	private static final int WAIT_TIMEOUT = 30000; // 30s

	/**
	 * Maximum number of dimensions that can be used in
	 * {@link #createJob(CreateJobRequest, UriInfo, AsyncResponse)
	 * createJob(...)}.
	 */
	private static final int MAX_CREATE_DIMENSIONS = 3;

	private static final Duration MIN_KEEPALIVE_DURATION =
			Duration.parse("PT30S");

	private static final Duration MAX_KEEPALIVE_DURATION =
			Duration.parse("PT300S");

	private final Version v;

	@Autowired
	private SpallocAPI core;

	@Autowired
	private JsonMapper mapper;

	@Autowired
	private ObjectProvider<MachineAPI> machineFactory;

	@Autowired
	private ObjectProvider<JobAPI> jobFactory;

	public SpallocServiceImpl(@Value("${version}") String version) {
		v = new Version(version.replaceAll("-.*", ""));
	}

	/**
	 * Manufactures instances of {@link MachineAPI} and {@link JobAPI}. This
	 * indirection allows Spring to insert any necessary security interceptors
	 * on methods.
	 */
	@Configuration
	static class APIBuilder extends BackgroundSupport {
		@Autowired
		private SpallocAPI core;

		@Autowired
		private JsonMapper mapper;

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
		@Scope("prototype")
		public MachineAPI machine(Machine machine, UriInfo ui) {
			return new MachineAPI() {
				@Override
				public void describeMachine(boolean wait,
						AsyncResponse response) {
					if (wait) {
						bgAction(response, () -> {
							log.debug("starting wait for change of machine");
							machine.waitForChange(WAIT_TIMEOUT);
							/*
							 * Assume that machines don't change often enough
							 * for us to care about whether they vanish;
							 * therefore handle still valid after wait
							 */
							return new MachineResponse(machine, ui);
						});
					} else {
						fgAction(response,
								() -> new MachineResponse(machine, ui));
					}
				}

				private WhereIsResponse makeResponse(
						Optional<BoardLocation> boardLocation)
						throws SQLException {
					return new WhereIsResponse(boardLocation.orElseThrow(
							() -> new NotFound("failed to locate board")), ui);
				}

				@Override
				public WhereIsResponse whereIsLogicalPosition(int x, int y,
						int z) throws SQLException {
					return makeResponse(
							machine.getBoardByLogicalCoords(x, y, z));
				}

				@Override
				public WhereIsResponse whereIsPhysicalPosition(int cabinet,
						int frame, int board) throws SQLException {
					return makeResponse(machine
							.getBoardByPhysicalCoords(cabinet, frame, board));
				}

				@Override
				public WhereIsResponse whereIsMachineChipLocation(int x, int y)
						throws SQLException {
					return makeResponse(machine.getBoardByChip(x, y));
				}

				@Override
				public WhereIsResponse whereIsIPAddress(String address)
						throws SQLException {
					return makeResponse(machine.getBoardByIPAddress(address));
				}
			};
		}

		@SuppressWarnings("serial")
		private static final class TempAuth
				implements Authentication, AutoCloseable {
			private final Permit permit;

			TempAuth(Permit permit) {
				this.permit = permit;
				SecurityContextHolder.getContext().setAuthentication(this);
			}

			@Override
			public String getName() {
				return permit.name;
			}

			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				Set<GrantedAuthority> s = Collections.emptySet();
				if (permit.admin) {
					s = Collections.singleton(() -> "ROLE_ADMIN");
				}
				return s;
			}

			@Override
			public Object getCredentials() {
				return null;
			}

			@Override
			public Object getDetails() {
				return null;
			}

			@Override
			public Object getPrincipal() {
				return null;
			}

			@Override
			public boolean isAuthenticated() {
				return true;
			}

			@Override
			public void setAuthenticated(boolean isAuthenticated)
					throws IllegalArgumentException {
			}

			@Override
			public void close() throws Exception {
				SecurityContextHolder.getContext().setAuthentication(null);
			}
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
		@Scope("prototype")
		public JobAPI job(Job j, String caller, Permit permit, UriInfo ui) {
			return new JobAPI() {
				@Override
				public String keepAlive(String reqBody) throws SQLException {
					log.debug("keeping job {} alive: {}", j.getId(), caller);
					j.access(caller);
					return "ok";
				}

				@Override
				public void getState(boolean wait, AsyncResponse response) {
					if (wait) {
						bgAction(response, () -> {
							log.debug("starting wait for change of job");
							j.waitForChange(WAIT_TIMEOUT);
							// Refresh the handle
							try (TempAuth t = new TempAuth(permit)) {
								Job nj = core.getJob(permit, j.getId())
										.orElseThrow(() -> new ItsGone(
												"no such job"));
								return new JobStateResponse(nj, ui, mapper);
							}
						});
					} else {
						fgAction(response,
								() -> new JobStateResponse(j, ui, mapper));
					}
				}

				@Override
				public Response deleteJob(String reason) throws SQLException {
					if (reason == null) {
						reason = "unspecified";
					}
					j.destroy(reason);
					return noContent().build();
				}

				@Override
				public SubMachineResponse getMachine() throws SQLException {
					j.access(caller);
					SubMachine m = j.getMachine().orElseThrow(
							// No machine allocated yet
							EmptyResponse::new);
					return new SubMachineResponse(m, ui);
				}

				@Override
				public MachinePower getMachinePower() throws SQLException {
					j.access(caller);
					SubMachine m = j.getMachine().orElseThrow(
							// No machine allocated yet
							EmptyResponse::new);
					return new MachinePower(m.getPower());
				}

				@Override
				public void setMachinePower(MachinePower reqBody,
						AsyncResponse response) throws SQLException {
					// Async because it involves getting a write lock
					if (reqBody == null) {
						throw new BadArgs("bad power description");
					}
					bgAction(response, () -> {
						j.access(caller);
						try (TempAuth t = new TempAuth(permit)) {
							SubMachine m = j.getMachine().orElseThrow(
									// No machine allocated yet
									EmptyResponse::new);
							m.setPower(reqBody.getPower());
						}
						return accepted().build();
					});
				}

				@Override
				public WhereIsResponse getJobChipLocation(int x, int y)
						throws SQLException {
					j.access(caller);
					BoardLocation loc =
							j.whereIs(x, y).orElseThrow(EmptyResponse::new);
					return new WhereIsResponse(loc, ui);
				}
			};
		}
	}

	@Override
	public ServiceDescription describeService(UriInfo ui, SecurityContext sec,
			HttpServletRequest req) {
		CsrfToken token = (CsrfToken) req.getAttribute("_csrf");
		return new ServiceDescription(v, ui, sec, token);
	}

	@Override
	public MachinesResponse getMachines(UriInfo ui) throws SQLException {
		return new MachinesResponse(core.getMachines(), ui);
	}

	@Override
	public MachineAPI getMachine(String name, UriInfo ui) throws SQLException {
		Machine machine = core.getMachine(name)
				.orElseThrow(() -> new NotFound("no such machine"));
		// Wrap so we can use security annotations
		return machineFactory.getObject(machine, ui);
	}

	@Override
	public JobAPI getJob(int id, UriInfo ui, HttpServletRequest req,
			SecurityContext security) throws SQLException {
		Permit permit = new Permit(security);
		Job j = core.getJob(permit, id)
				.orElseThrow(() -> new NotFound("no such job"));
		// Wrap so we can use security annotations
		return jobFactory.getObject(j, req.getRemoteHost(), permit, ui);
	}

	private static final int LIMIT_LIMIT = 200;

	/**
	 * Adds in the {@code Link:} header with general paging info.
	 *
	 * @param value
	 *            The core response.
	 * @param ui
	 *            Information about URIs
	 * @param start
	 *            The start offset.
	 * @param limit
	 *            The size of chunk.
	 * @return Annotated response.
	 */
	private Response wrapPaging(ListJobsResponse value, UriInfo ui, int start,
			int limit) {
		ResponseBuilder r = Response.ok(value);
		Map<String, URI> links = new HashMap<>();
		if (start > 0) {
			URI prev = ui.getRequestUriBuilder()
					.replaceQueryParam("wait", false)
					.replaceQueryParam("start", Math.max(start - limit, 0))
					.build();
			value.setPrev(prev);
			links.put("prev", prev);
		}
		if (value.jobs.size() == limit) {
			URI next = ui.getRequestUriBuilder()
					.replaceQueryParam("wait", false)
					.replaceQueryParam("start", start + limit).build();
			value.setNext(next);
			links.put("next", next);
		}
		if (!links.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			String sep = "";
			for (Entry<String, URI> e : links.entrySet()) {
				sb.append("<").append(e.getValue()).append(">; rel=\"")
						.append(e.getKey()).append("\"").append(sep);
				sep = ", ";
			}
			r.header("Link", sb.toString());
		}
		return r.build();
	}

	@Override
	public void listJobs(boolean wait, boolean destroyed, int limit, int start,
			UriInfo ui, AsyncResponse response) throws SQLException {
		if (limit > LIMIT_LIMIT || limit < 1) {
			throw new BadArgs("limit must not be bigger than " + LIMIT_LIMIT);
		}
		if (start < 0) {
			throw new BadArgs("start must not be less than 0");
		}
		Jobs jc = core.getJobs(destroyed, limit, start);
		if (wait) {
			bgAction(response, () -> {
				log.debug("starting wait for change of job list");
				jc.waitForChange(WAIT_TIMEOUT);
				Jobs newJc = core.getJobs(destroyed, limit, start);
				return wrapPaging(new ListJobsResponse(newJc, ui), ui, start,
						limit);
			});
		} else {
			fgAction(response, () -> wrapPaging(new ListJobsResponse(jc, ui),
					ui, start, limit));
		}
	}

	@Override
	public void createJob(CreateJobRequest req, UriInfo ui,
			SecurityContext security, AsyncResponse response) {
		// Async because it involves getting a write lock
		if (req == null) {
			throw new BadArgs("request must be supplied");
		}
		if (!security.isUserInRole("ADMIN") || req.owner == null
				|| req.owner.trim().isEmpty()) {
			req.owner = security.getUserPrincipal().getName();
		}
		if (req.owner == null || req.owner.trim().isEmpty()) {
			throw new BadArgs(
					"request must be connected to an identified owner");
		}
		if (req.keepaliveInterval == null || req.keepaliveInterval
				.compareTo(MIN_KEEPALIVE_DURATION) < 0) {
			throw new BadArgs("keepalive interval must be at least "
					+ MIN_KEEPALIVE_DURATION);
		}
		if (req.keepaliveInterval.compareTo(MAX_KEEPALIVE_DURATION) > 0) {
			throw new BadArgs("keepalive interval must be no more than "
					+ MAX_KEEPALIVE_DURATION);
		}
		if (req.dimensions == null) {
			req.dimensions = new ArrayList<>();
		}
		if (req.dimensions.size() > MAX_CREATE_DIMENSIONS) {
			throw new BadArgs(
					"must be zero to " + MAX_CREATE_DIMENSIONS + " dimensions");
		}
		if (req.dimensions.size() == 0) {
			req.dimensions.add(1);
		}
		if (req.dimensions.stream().anyMatch(x -> x < 0)) {
			throw new BadArgs("dimensions must not be negative");
		}
		if (req.tags == null) {
			req.tags = new ArrayList<>();
			if (req.machineName == null) {
				req.tags.add("default");
			}
		}
		if (req.machineName != null && req.tags.size() > 0) {
			throw new BadArgs(
					"must not specify machine name and tags together");
		}
		if (req.maxDeadBoards == null) {
			req.maxDeadBoards = 0;
		}
		if (req.maxDeadBoards < 0) {
			throw new BadArgs(
					"the maximum number of dead boards must not be negative");
		}

		bgAction(response, () -> {
			Job j = core.createJob(req.owner.trim(), req.dimensions,
					req.machineName, req.tags, req.keepaliveInterval,
					req.maxDeadBoards, mapper.writeValueAsBytes(req));
			if (j == null) {
				// Most likely reason for failure
				return status(BAD_REQUEST).type(TEXT_PLAIN)
						.entity("out of quota").build();
			}
			URI uri = ui.getRequestUriBuilder().path("{id}").build(j.getId());
			return created(uri).entity(new CreateJobResponse(j, ui)).build();
		});
	}
}
