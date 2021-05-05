package uk.ac.manchester.spinnaker.alloc.web;

import static javax.ws.rs.core.Response.accepted;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.manchester.spinnaker.alloc.Job;
import uk.ac.manchester.spinnaker.alloc.JobCollection;
import uk.ac.manchester.spinnaker.alloc.Machine;
import uk.ac.manchester.spinnaker.alloc.SpallocInterface;
import uk.ac.manchester.spinnaker.alloc.SubMachine;
import uk.ac.manchester.spinnaker.messages.model.Version;

public class SpallocImpl implements SpallocAPI {
	private final Version v;
	@Autowired
	private SpallocInterface core;

	public SpallocImpl(String version) {
		v = new Version(version.replaceAll("-.*", ""));
	}

	@Override
	public Response describeService(UriInfo ui) {
		return ok(new ServiceDescription(v, ui)).build();
	}

	@Override
	public Response getMachines(UriInfo ui) {
		return ok(new MachinesResponse(core.getMachines(), ui)).build();
	}

	@Override
	public MachineAPI getMachine(String name, UriInfo ui) {
		Machine machine = core.getMachine(name);
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
				return ok(new WhereIsResponse(
						machine.getBoardByLogicalCoords(x, y, z), ui)).build();
			}

			@Override
			public Response whereIsPhysicalPosition(int cabinet, int frame,
					int board) {
				return ok(new WhereIsResponse(
						machine.getBoardByPhysicalCoords(cabinet, frame, board),
						ui)).build();
			}

			@Override
			public Response whereIsMachineChipLocation(int x, int y) {
				return ok(new WhereIsResponse(machine.getBoardByChip(x, y), ui))
						.build();
			}
		};
	}

	@Override
	public JobAPI getJob(int id, UriInfo ui, HttpServletRequest req) {
		Job j = core.getJob(id);
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
					nj = core.getJob(id);
					if (nj == null) {
						throw new WebApplicationException("no such job", GONE);
					}
				}
				return ok(new StateResponse(nj, ui)).build();
			}

			@Override
			public Response deleteJob(String reason) {
				core.getJob(id).destroy(reason);
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
		JobCollection jc = core.getJobs();
		if (wait) {
			jc.waitForChange();
			jc = core.getJobs();
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
		}
		if (req.machineName != null && req.tags.size() > 0) {
			throw new WebApplicationException(
					"must not specify machine name and tags together",
					BAD_REQUEST);
		}
		Job j = core.createJob(req.owner, req.dimensions, req.machineName,
				req.tags);
		return created(ui.getRequestUriBuilder().path("{id}").build(j.getId()))
				.entity(new CreateJobResponse(j, ui)).build();
	}

}
