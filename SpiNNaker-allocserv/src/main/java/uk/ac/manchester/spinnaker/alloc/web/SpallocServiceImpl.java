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

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateBoard.address;
import static uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateBoard.physical;
import static uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateBoard.triad;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.SERV;
import static uk.ac.manchester.spinnaker.utils.OptionalUtils.ifElse;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.ServiceVersion;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDescriptor;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDimensions;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateDimensionsAt;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.CreateNumBoards;
import uk.ac.manchester.spinnaker.alloc.security.Permit;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.BadArgs;
import uk.ac.manchester.spinnaker.alloc.web.RequestFailedException.NotFound;

/**
 * The implementation of the user-facing REST API. Operations are delegated to
 * {@link SpallocAPI} for fulfilment; this class is responsible for turning the
 * operations described by users into the form understood by the service core,
 * and for converting the responses. It also handles the transfer of calls onto
 * suitable worker threads, where appropriate.
 *
 * @author Donal Fellows
 */
@Service("service")
@Path(SERV)
public class SpallocServiceImpl extends BackgroundSupport
		implements SpallocServiceAPI {
	private static final Logger log = getLogger(SpallocServiceImpl.class);

	@Autowired
	private ServiceVersion version;

	@Autowired
	private SpallocProperties properties;

	@Autowired
	private SpallocAPI core;

	@Autowired
	private JsonMapper mapper;

	/**
	 * Factory for {@linkplain MachineAPI machines}. Only use via
	 * {@link #getMachine(String, UriInfo, SecurityContext) getMachine(...)};
	 * this is because we're dealing with prototype beans.
	 */
	@Autowired
	private ObjectProvider<MachineAPI> machineFactory;

	/**
	 * Factory for {@linkplain JobAPI jobs}. Only use via
	 * {@link #getJob(int, UriInfo, HttpServletRequest, SecurityContext)
	 * getJob(...)}; this is because we're dealing with prototype beans.
	 */
	@Autowired
	private ObjectProvider<JobAPI> jobFactory;

	@Override
	public ServiceDescription describeService(UriInfo ui, SecurityContext sec,
			HttpServletRequest req) {
		var token = (CsrfToken) req.getAttribute("_csrf");
		return new ServiceDescription(version.getVersion(), ui, sec, token);
	}

	@Override
	public MachinesResponse getMachines(UriInfo ui) {
		return new MachinesResponse(core.getMachines(false), ui);
	}

	@Override
	public MachineAPI getMachine(String name, UriInfo ui, SecurityContext sec) {
		var permit = new Permit(sec);
		var machine = core.getMachine(name, permit.admin)
				.orElseThrow(() -> new NotFound("no such machine"));
		// Wrap so we can use security annotations
		return machineFactory.getObject(machine, ui);
	}

	@Override
	public JobAPI getJob(int id, UriInfo ui, HttpServletRequest req,
			SecurityContext security) {
		var permit = new Permit(security);
		var j = core.getJob(permit, id)
				.orElseThrow(() -> new NotFound("no such job"));
		// Wrap so we can use security annotations
		return jobFactory.getObject(j, req.getRemoteHost(), permit, ui, req);
	}

	// Could be configurable, but no real point
	private static final int LIMIT_LIMIT = 200;

	/**
	 * Adds in the {@code Link:} header with general paging info.
	 *
	 * @param value The core response.
	 * @param ui    Information about URIs
	 * @param start The start offset.
	 * @param limit The size of chunk.
	 * @return Annotated response.
	 */
	private Response wrapPaging(ListJobsResponse value, UriInfo ui, int start,
			int limit) {
		var r = ok(value);
		var links = new HashMap<String, URI>();
		if (start > 0) {
			var prev = ui.getRequestUriBuilder()
					.replaceQueryParam("wait", false)
					.replaceQueryParam("start", max(start - limit, 0)).build();
			value.setPrev(prev);
			links.put("prev", prev);
		}
		if (value.jobs.size() == limit) {
			var next = ui.getRequestUriBuilder()
					.replaceQueryParam("wait", false)
					.replaceQueryParam("start", start + limit).build();
			value.setNext(next);
			links.put("next", next);
		}
		if (!links.isEmpty()) {
			r.header("Link", join(", ", links.entrySet().stream().map(
					e -> format("<%s>; rel=\"%s\"", e.getValue(), e.getKey()))
					.collect(toList())));
		}
		return r.build();
	}

	@Override
	public void listJobs(boolean wait, boolean destroyed, int limit, int start,
			UriInfo ui, AsyncResponse response) {
		if (limit > LIMIT_LIMIT || limit < 1) {
			throw new BadArgs("limit must not be bigger than " + LIMIT_LIMIT);
		}
		if (start < 0) {
			throw new BadArgs("start must not be less than 0");
		}
		var jc = core.getJobs(destroyed, limit, start);
		if (wait) {
			bgAction(response, () -> {
				log.debug("starting wait for change of job list");
				jc.waitForChange(properties.getWait());
				var newJc = core.getJobs(destroyed, limit, start);
				return wrapPaging(new ListJobsResponse(newJc, ui), ui, start,
						limit);
			});
		} else {
			fgAction(response, () -> wrapPaging(new ListJobsResponse(jc, ui),
					ui, start, limit));
		}
	}

	private static String trim(String str) {
		if (isNull(str)) {
			return null;
		}
		return str.strip();
	}

	@Override
	public void createJob(CreateJobRequest req, UriInfo ui,
			SecurityContext security, AsyncResponse response) {
		var crds = validateAndApplyDefaultsToJobRequest(req, security);

		// Async because it involves getting a write lock
		bgAction(response, () -> ifElse(
				core.createJob(trim(req.owner), trim(req.group), crds,
						req.machineName, req.tags, req.keepaliveInterval,
						mapper.writeValueAsBytes(req)),
				job -> created(ui.getRequestUriBuilder().path("{id}")
						.build(job.getId()))
								.entity(new CreateJobResponse(job, ui)).build(),
				() -> status(BAD_REQUEST).type(TEXT_PLAIN)
						// Most likely reason for failure
						.entity("out of quota").build()));
	}

	private CreateDescriptor validateAndApplyDefaultsToJobRequest(
			CreateJobRequest req, SecurityContext security) throws BadArgs {
		if (isNull(req)) {
			throw new BadArgs("request must be supplied");
		}

		if (!security.isUserInRole("ADMIN") || isNull(req.owner)
				|| req.owner.isBlank()) {
			req.owner = security.getUserPrincipal().getName();
		}
		if (isNull(req.owner) || req.owner.isBlank()) {
			throw new BadArgs(
					"request must be connected to an identified owner");
		}
		req.owner = req.owner.strip();

		var ka = properties.getKeepalive();
		if (isNull(req.keepaliveInterval)
				|| req.keepaliveInterval.compareTo(ka.getMin()) < 0) {
			throw new BadArgs(
					"keepalive interval must be at least " + ka.getMin());
		}
		if (req.keepaliveInterval.compareTo(ka.getMax()) > 0) {
			throw new BadArgs(
					"keepalive interval must be no more than " + ka.getMax());
		}

		if (isNull(req.tags)) {
			req.tags = new ArrayList<>();
			if (isNull(req.machineName)) {
				req.tags.add("default");
			}
		}
		if (nonNull(req.machineName) && !req.tags.isEmpty()) {
			throw new BadArgs(
					"must not specify machine name and tags together");
		}

		if (isNull(req.maxDeadBoards)) {
			req.maxDeadBoards = 0;
		} else if (req.maxDeadBoards < 0) {
			throw new BadArgs(
					"the maximum number of dead boards must not be negative");
		}

		if (nonNull(req.numBoards)) {
			return new CreateNumBoards(req.numBoards, req.maxDeadBoards);
		} else if (nonNull(req.dimensions)) {
			if (nonNull(req.board)) {
				// Both dimensions AND board; rooted rectangle
				if (nonNull(req.board.x)) {
					return new CreateDimensionsAt(req.dimensions.width,
							req.dimensions.height, req.board.x, req.board.y,
							req.board.z);
				} else if (nonNull(req.board.cabinet)) {
					return CreateDimensionsAt.physical(req.dimensions.width,
							req.dimensions.height, req.board.cabinet,
							req.board.frame, req.board.board,
							req.maxDeadBoards);
				} else {
					return new CreateDimensionsAt(req.dimensions.width,
							req.dimensions.height, req.board.address,
							req.maxDeadBoards);
				}
			}
			return new CreateDimensions(req.dimensions.width,
					req.dimensions.height, req.maxDeadBoards);
		} else if (nonNull(req.board)) {
			if (nonNull(req.board.x)) {
				return triad(req.board.x, req.board.y, req.board.z);
			} else if (nonNull(req.board.cabinet)) {
				return physical(req.board.cabinet, req.board.frame,
						req.board.board);
			} else {
				return address(req.board.address);
			}
		} else {
			// It's a single board
			return new CreateNumBoards(1, 0);
		}
	}
}
