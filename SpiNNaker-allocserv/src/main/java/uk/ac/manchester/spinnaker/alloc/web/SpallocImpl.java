package uk.ac.manchester.spinnaker.alloc.web;

import static javax.ws.rs.core.Response.accepted;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
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
	public Response getVersion() {
		return ok(new VersionResponse(v)).build();
	}

	@Override
	public Response getMachines(UriInfo ui) {
		return ok(new MachinesResponse(core.getMachines(), ui)).build();
	}

	@Override
	public MachineAPI getMachine(String name) {
		Machine machine = core.getMachine(name);
		if (machine == null) {
			throw new WebApplicationException("no such machine", NOT_FOUND);
		}
		return new MachineAPI() {
			@Override
			public Response describeMachine(boolean wait, UriInfo ui) {
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
			public Response getPhysicalPosition(int x, int y, int z) {
				return ok(new WhereIsResponse(
						machine.getBoardByLogicalCoords(x, y, z))).build();
			}

			@Override
			public Response getLogicalPosition(int cabinet, int frame,
					int board) {
				return ok(new WhereIsResponse(machine
						.getBoardByPhysicalCoords(cabinet, frame, board)))
								.build();
			}

			@Override
			public Response getMachineChipLocation(int x, int y) {
				return ok(new WhereIsResponse(machine.getBoardByChip(x, y)))
						.build();
			}
		};
	}

	@Override
	public JobAPI getJob(int id) {
		Job j = core.getJob(id);
		if (j == null) {
			throw new WebApplicationException("no such job", NOT_FOUND);
		}
		return new JobAPI() {
			@Override
			public Response keepAlive(String req) {
				j.access();
				return ok("ok").build();
			}

			@Override
			public Response getState(boolean wait, UriInfo ui) {
				Job nj = j;
				if (wait) {
					j.waitForChange();
					// Refresh the handle
					nj = core.getJob(id);
					if (nj == null) {
						throw new WebApplicationException("no such job",
								NOT_FOUND);
					}
				}
				return ok(new StateResponse(nj, ui)).build();
			}

			@Override
			public Response deleteJob() {
				core.getJob(id).destroy();
				return noContent().build();
			}

			@Override
			public Response getMachine(UriInfo ui) {
				SubMachine m = j.getMachine();
				if (m == null) {
					// No machine allocated yet
					return noContent().build();
				}
				return ok(new SubMachineResponse(m, ui)).build();
			}

			@Override
			public Response getMachinePower() {
				return ok(new MachinePower(j.getMachine().getPower())).build();
			}

			@Override
			public Response setMachinePower(MachinePower req) {
				j.getMachine().setPower(req.power);
				return accepted().build();
			}

			@Override
			public Response getJobChipLocation(int x, int y) {
				// TODO Auto-generated method stub
				return ok().build();
			}
		};
	}

	@Override
	public Response listJobs(boolean wait, @Context UriInfo ui) {
		JobCollection jc = core.getJobs();
		if (wait) {
			jc.waitForChange();
			jc = core.getJobs();
		}
		// FIXME finish this
		return ok(new ListJobsResponse(jc, ui)).build();
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
		CreateJobResponse response = new CreateJobResponse();
		response.jobId = j.getId();
		return created(ui.getRequestUriBuilder().path("{id}").build(j.getId()))
				.entity(response).build();
	}

}
