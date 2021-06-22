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

import static javax.ws.rs.core.Response.accepted;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.SERV;

import java.net.URI;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

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
public class SpallocServiceImpl implements SpallocServiceAPI {
	private static final Logger log = getLogger(SpallocServiceImpl.class);

	private static final int WAIT_TIMEOUT = 30000; // 30s

	/**
	 * Maximum number of dimensions that can be used in
	 * {@link #createJob(CreateJobRequest, UriInfo, AsyncResponse)
	 * createJob(...)}
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
	private Executor executor;

	@Autowired
	private JsonMapper mapper;

	public SpallocServiceImpl(@Value("${version}") String version) {
		v = new Version(version.replaceAll("-.*", ""));
	}

	/**
	 * An action that produces a response, usually handled asynchronously. Care
	 * should be taken as the action may be run on a thread other than the
	 * thread that creates it.
	 *
	 * @author Donal Fellows
	 */
	@FunctionalInterface
	private static interface BackgroundAction {
		/**
		 * Does the action that produces the result.
		 *
		 * @return The result of the action. A {@code null} is mapped as a
		 *         generic 404 with no special message. Custom messages should
		 *         be supported by throwing a suitable
		 *         {@link RequestFailedException}. If the result is not a
		 *         {@link Response} and not a {@link Throwable}, it will be
		 *         returned as the entity that populates a {@code 200 OK} (and
		 *         so must be convertible to JSON).
		 * @throws RequestFailedException
		 *             If anything goes wrong. This is the <em>expected</em>
		 *             exception type; it is <em>not</em> logged.
		 * @throws Exception
		 *             If anything goes wrong. This is reported as an
		 *             <em>unexpected</em> exception and logged.
		 */
		Object respond() throws RequestFailedException, Exception;
	}

	/**
	 * Run the action in the background and wrap it into the response when it
	 * completes.
	 *
	 * @param response
	 *            The asynchronous response.
	 * @param action
	 *            The action that generates a {@link Response}
	 */
	private void bgAction(AsyncResponse response, BackgroundAction action) {
		executor.execute(() -> fgAction(response, action));
	}

	/**
	 * Run the action immediately and wrap it into the response
	 *
	 * @param response
	 *            The asynchronous response.
	 * @param action
	 *            The action that generates a {@link Response}
	 */
	private static void fgAction(AsyncResponse response,
			BackgroundAction action) {
		try {
			Object r = action.respond();
			if (r == null) {
				// If you want something else, don't return null
				response.resume(new NotFound("not found"));
			} else {
				response.resume(r);
			}
		} catch (RequestFailedException | SQLException e) {
			// Known exception mappers for these
			response.resume(e);
		} catch (Exception e) {
			response.resume(
					new RequestFailedException("unexpected server problem", e));
		}
	}

	@Override
	public ServiceDescription describeService(UriInfo ui) {
		return new ServiceDescription(v, ui);
	}

	@Override
	public MachinesResponse getMachines(UriInfo ui) throws SQLException {
		return new MachinesResponse(core.getMachines(), ui);
	}

	@Override
	public MachineAPI getMachine(String name, UriInfo ui) throws SQLException {
		Machine machine = core.getMachine(name)
				.orElseThrow(() -> new NotFound("no such machine"));

		return new MachineAPI() {
			@Override
			public void describeMachine(boolean wait, AsyncResponse response) {
				if (wait) {
					bgAction(response, () -> {
						log.debug("starting wait for change of machine");
						machine.waitForChange(WAIT_TIMEOUT);
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

			@Override
			public WhereIsResponse whereIsLogicalPosition(int x, int y, int z)
					throws SQLException {
				if (x < 0 || y < 0 || z < 0) {
					throw new BadArgs("coordinates must not be negative");
				}
				// No epoch; value not retained
				return new WhereIsResponse(
						machine.getBoardByLogicalCoords(x, y, z).orElseThrow(
								() -> new NotFound("failed to locate board")),
						ui);
			}

			@Override
			public WhereIsResponse whereIsPhysicalPosition(int cabinet,
					int frame, int board) throws SQLException {
				if (cabinet < 0 || frame < 0 || board < 0) {
					throw new BadArgs("coordinates must not be negative");
				}
				// No epoch; value not retained
				return new WhereIsResponse(machine
						.getBoardByPhysicalCoords(cabinet, frame, board)
						.orElseThrow(
								() -> new NotFound("failed to locate board")),
						ui);
			}

			@Override
			public WhereIsResponse whereIsMachineChipLocation(int x, int y)
					throws SQLException {
				if (x < 0 || y < 0) {
					throw new BadArgs("coordinates must not be negative");
				}
				// No epoch; value not retained
				return new WhereIsResponse(
						machine.getBoardByChip(x, y).orElseThrow(
								() -> new NotFound("failed to locate board")),
						ui);
			}

			@Override
			public WhereIsResponse whereIsIPAddress(String address)
					throws SQLException {
				// TODO Can we sanity-check the address?
				// No epoch; value not retained
				return new WhereIsResponse(
						machine.getBoardByIPAddress(address).orElseThrow(
								() -> new NotFound("failed to locate board")),
						ui);
			}
		};
	}

	@Override
	public JobAPI getJob(int id, UriInfo ui, HttpServletRequest req)
			throws SQLException {
		String caller = req.getRemoteHost();
		Job j = core.getJob(id).orElseThrow(() -> new NotFound("no such job"));

		return new JobAPI() {
			@Override
			public String keepAlive(String reqBody) throws SQLException {
				log.debug("keeping job {} alive: {}", id, caller);
				j.access(req.getRemoteAddr());
				return "ok";
			}

			@Override
			public void getState(boolean wait, AsyncResponse response) {
				if (wait) {
					bgAction(response, () -> {
						log.debug("starting wait for change of job");
						j.waitForChange(WAIT_TIMEOUT);
						// Refresh the handle
						Job nj = core.getJob(id)
								.orElseThrow(() -> new ItsGone("no such job"));
						return new JobStateResponse(nj, ui, mapper);
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
			public Response setMachinePower(MachinePower reqBody)
					throws SQLException {
				if (reqBody == null || reqBody.power == null) {
					throw new BadArgs("bad power description");
				}
				j.access(caller);
				SubMachine m = j.getMachine().orElseThrow(
						// No machine allocated yet
						EmptyResponse::new);
				m.setPower(reqBody.power);
				return accepted().build();
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

	private static final int LIMIT_LIMIT = 200;

	@Override
	public void listJobs(boolean wait, boolean destroyed, int limit, int start,
			UriInfo ui, AsyncResponse response) throws SQLException {
		if (limit > LIMIT_LIMIT || limit < 1) {
			throw new BadArgs("limit must not be bigger than " + LIMIT_LIMIT);
		}
		if (start < 0) {
			throw new BadArgs("start must not be less than 0");
		}
		if (wait) {
			bgAction(response, () -> {
				Jobs jc = core.getJobs(destroyed, limit, start);
				log.debug("starting wait for change of job list");
				jc.waitForChange(WAIT_TIMEOUT);
				Jobs newJc = core.getJobs(destroyed, limit, start);
				return new ListJobsResponse(newJc, ui);
			});
		} else {
			Jobs jc = core.getJobs(destroyed, limit, start);
			fgAction(response, () -> new ListJobsResponse(jc, ui));
		}
	}

	@Override
	public void createJob(CreateJobRequest req, UriInfo ui,
			AsyncResponse response) {
		if (req == null) {
			throw new BadArgs("request must be supplied");
		}
		if (req.owner == null || req.owner.trim().isEmpty()) {
			throw new BadArgs("owner must be supplied");
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
			URI uri = ui.getRequestUriBuilder().path("{id}").build(j.getId());
			return created(uri).entity(new CreateJobResponse(j, ui)).build();
		});
	}
}
