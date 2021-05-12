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
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.allocator.Job;
import uk.ac.manchester.spinnaker.alloc.allocator.JobCollection;
import uk.ac.manchester.spinnaker.alloc.allocator.Machine;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocInterface;
import uk.ac.manchester.spinnaker.alloc.allocator.SubMachine;
import uk.ac.manchester.spinnaker.messages.model.Version;

@Component("service")
public class SpallocImpl implements SpallocAPI {
	private static final Logger log = getLogger(SpallocImpl.class);

	private final Version v;

	@Autowired
	private SpallocInterface core;

	public SpallocImpl(@Value("${version}") String version) {
		v = new Version(version.replaceAll("-.*", ""));
	}

	@Override
	public Response describeService(UriInfo ui) {
		return ok(new ServiceDescription(v, ui)).build();
	}

	@Override
	public Response getMachines(UriInfo ui) {
		try {
			return ok(new MachinesResponse(core.getMachines(), ui)).build();
		} catch (SQLException e) {
			log.error("failed to list machines", e);
			throw new WebApplicationException("failed to list machines");
		}
	}

	@Override
	public MachineAPI getMachine(String name, UriInfo ui) {
		Machine machine;
		try {
			machine = core.getMachine(name);
		} catch (SQLException e) {
			log.error("failed to get machine", e);
			throw new WebApplicationException("failed to get machine: " + name);
		}
		if (machine == null) {
			throw new WebApplicationException("no such machine", NOT_FOUND);
		}
		return new MachineAPI() {
			@Override
			public Response describeMachine(boolean wait) {
				if (wait) {
					machine.waitForChange();
					/*
					 * Assume that machines don't change often enough for us to
					 * care about whether they vanish; therefore handle still
					 * valid after wait
					 */
				}
				return ok(new MachineResponse(machine, ui)).build();
			}

			@Override
			public Response whereIsLogicalPosition(int x, int y, int z) {
				try {
					return ok(new WhereIsResponse(
							machine.getBoardByLogicalCoords(x, y, z), ui))
									.build();
				} catch (SQLException e) {
					log.error("failed to locate board", e);
					throw new WebApplicationException("failed to locate board");
				}
			}

			@Override
			public Response whereIsPhysicalPosition(int cabinet, int frame,
					int board) {
				try {
					return ok(new WhereIsResponse(machine
							.getBoardByPhysicalCoords(cabinet, frame, board),
							ui)).build();
				} catch (SQLException e) {
					log.error("failed to locate board", e);
					throw new WebApplicationException("failed to locate board");
				}
			}

			@Override
			public Response whereIsMachineChipLocation(int x, int y) {
				try {
					return ok(new WhereIsResponse(machine.getBoardByChip(x, y),
							ui)).build();
				} catch (SQLException e) {
					log.error("failed to locate board", e);
					throw new WebApplicationException("failed to locate board");
				}
			}
		};
	}

	@Override
	public JobAPI getJob(int id, UriInfo ui, HttpServletRequest req) {
		Job j;
		try {
			j = core.getJob(id);
		} catch (SQLException e) {
			log.error("failed to get job", e);
			throw new WebApplicationException("failed to get job: " + id);
		}
		if (j == null) {
			throw new WebApplicationException("no such job", NOT_FOUND);
		}
		return new JobAPI() {
			@Override
			public Response keepAlive(String reqBody) {
				j.access(req.getRemoteAddr());
				return ok("ok").build();
			}

			@Override
			public Response getState(boolean wait) {
				Job nj = j;
				if (wait) {
					j.waitForChange();
					// Refresh the handle
					try {
						nj = core.getJob(id);
					} catch (SQLException e) {
						log.error("failed to get job", e);
						throw new WebApplicationException(
								"failed to get job: " + id);
					}
					if (nj == null) {
						throw new WebApplicationException("no such job", GONE);
					}
				}
				return ok(new StateResponse(nj, ui)).build();
			}

			@Override
			public Response deleteJob(String reason) {
				try {
					core.getJob(id).destroy(reason);
				} catch (SQLException e) {
					log.error("failed to destroy job", e);
					throw new WebApplicationException(
							"failed to destroy job: " + id);
				}
				return noContent().build();
			}

			@Override
			public Response getMachine() {
				j.access(req.getRemoteAddr());
				SubMachine m = j.getMachine();
				if (m == null) {
					// No machine allocated yet
					return noContent().build();
				}
				return ok(new SubMachineResponse(m, ui)).build();
			}

			@Override
			public Response getMachinePower() {
				j.access(req.getRemoteAddr());
				return ok(new MachinePower(j.getMachine().getPower())).build();
			}

			@Override
			public Response setMachinePower(MachinePower reqBody) {
				j.access(req.getRemoteAddr());
				j.getMachine().setPower(reqBody.power);
				return accepted().build();
			}

			@Override
			public Response getJobChipLocation(int x, int y) {
				j.access(req.getRemoteAddr());
				return ok(new WhereIsResponse(j.whereIs(x, y), ui)).build();
			}
		};
	}

	private static final int LIMIT_LIMIT = 200;

	@Override
	public Response listJobs(boolean wait, int limit, int start,
			@Context UriInfo ui) {
		if (limit > LIMIT_LIMIT || limit < 1) {
			throw new WebApplicationException(
					"limit must not be bigger than " + LIMIT_LIMIT,
					BAD_REQUEST);
		}
		if (start < 0) {
			throw new WebApplicationException("start must not be less than 0",
					BAD_REQUEST);
		}
		JobCollection jc;
		try {
			jc = core.getJobs();
			if (wait) {
				jc.waitForChange();
				jc = core.getJobs();
			}
		} catch (SQLException e) {
			log.error("failed to list jobs", e);
			throw new WebApplicationException("failed to list jobs");
		}

		return ok(new ListJobsResponse(jc, limit, start, ui)).build();
	}

	@Override
	public Response createJob(CreateJobRequest req, @Context UriInfo ui) {
		if (req.owner == null) {
			throw new WebApplicationException("owner must be supplied",
					BAD_REQUEST);
		}
		if (req.dimensions == null) {
			req.dimensions = new ArrayList<>();
		}
		if (req.dimensions.size() > 3) {
			throw new WebApplicationException("must be zero to 3 dimensions",
					BAD_REQUEST);
		}
		if (req.dimensions.size() == 0) {
			req.dimensions.add(1);
		}
		if (req.tags == null) {
			req.tags = new ArrayList<>();
			if (req.machineName == null) {
				req.tags.add("default");
			}
		}
		if (req.machineName != null && req.tags.size() > 0) {
			throw new WebApplicationException(
					"must not specify machine name and tags together",
					BAD_REQUEST);
		}
		Integer maxDeadBoards = null; // FIXME
		Integer maxDeadLinks = null; // FIXME
		Job j;
		try {
			j = core.createJob(req.owner, req.dimensions, req.machineName,
					req.tags, maxDeadBoards);
		} catch (SQLException e) {
			log.error("failed to create job", e);
			throw new WebApplicationException(
					"failed to create job: " + req.dimensions);
		}
		return created(ui.getRequestUriBuilder().path("{id}").build(j.getId()))
				.entity(new CreateJobResponse(j, ui)).build();
	}

}
