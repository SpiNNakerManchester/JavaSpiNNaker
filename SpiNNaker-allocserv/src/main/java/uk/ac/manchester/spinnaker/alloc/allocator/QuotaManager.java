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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.isBusy;
import static uk.ac.manchester.spinnaker.alloc.nmpi.ResourceUsage.BOARD_SECONDS;
import static uk.ac.manchester.spinnaker.alloc.nmpi.ResourceUsage.CORE_HOURS;
import static uk.ac.manchester.spinnaker.alloc.model.GroupRecord.GroupType.COLLABRATORY;
import static uk.ac.manchester.spinnaker.alloc.security.TrustLevel.USER;

import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.RestrictedApi;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.QuotaProperties;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Update;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.nmpi.Job;
import uk.ac.manchester.spinnaker.alloc.nmpi.JobResourceUpdate;
import uk.ac.manchester.spinnaker.alloc.nmpi.NMPIv3API;
import uk.ac.manchester.spinnaker.alloc.nmpi.Project;
import uk.ac.manchester.spinnaker.alloc.nmpi.ResourceUsage;
import uk.ac.manchester.spinnaker.alloc.nmpi.SessionRequest;
import uk.ac.manchester.spinnaker.alloc.nmpi.SessionResourceUpdate;

/**
 * Manages user quotas.
 *
 * @author Donal Fellows
 */
@Service
public class QuotaManager extends DatabaseAwareBean {
	private static final Logger log = getLogger(QuotaManager.class);

	/**
	 * The status of the quote to request from the NMPI service.
	 */
	private static final String STATUS_ACCEPTED = "accepted";

	/**
	 * An approximation of cores per board.
	 */
	private static final int APPROX_CORES_PER_BOARD = 48 * 16;

	/**
	 * The number of seconds per hour.
	 */
	private static final int SECONDS_PER_HOUR = 60 * 60;

	@Autowired
	private ServiceMasterControl control;

	@Autowired
	private QuotaProperties quotaProps;

	/** The wrapped NMPI proxy, bound to the API key. */
	private NMPI nmpi;

	/**
	 * Make the NMPI access interface.
	 */
	@PostConstruct
	private void createProxy() {
		var nmpiUrl = quotaProps.getNMPIUrl();
		if (!nmpiUrl.isEmpty()) {
			nmpi = new NMPI(quotaProps);
		}
	}

	/**
	 * Can the user (in a specific group) create another job at this point? If
	 * not, they're currently out of resources.
	 *
	 * @param groupId
	 *            What group will the job be accounted against.
	 * @return True if they can make a job. False if they can't.
	 */
	public boolean mayCreateJob(int groupId) {
		try (var sql = new CreateCheckSQL()) {
			return sql.transactionRead(() -> sql.mayCreateJob(groupId));
		}
	}

	private final class CreateCheckSQL extends AbstractSQL {
		// TODO These should be combined (but one is an aggregate so...)
		private final Query getQuota = conn.query(GET_GROUP_QUOTA);

		private final Query getCurrentUsage = conn.query(GET_CURRENT_USAGE);

		@Override
		public void close() {
			getQuota.close();
			getCurrentUsage.close();
			super.close();
		}

		private boolean mayCreateJob(int groupId) {
			return getQuota.call1(result -> {
				var quota = result.getInteger("quota");
				log.debug("Group {} has quota {}", groupId, quota);
				if (isNull(quota)) {
					return true;
				}
				// Quota is defined; check if current usage exceeds it
				int usage = getCurrentUsage.call1(
						integer("current_usage"), groupId).orElse(0);
				log.debug("Group {} has usage {}", groupId, usage);
				// If board-seconds are left, we're good to go
				return (quota > usage);
			}, groupId).orElse(true);
		}
	}

	/**
	 * Has the execution of a job remained within its group's resource
	 * allocation at this point?
	 *
	 * @param jobId
	 *            What job is consuming resources?
	 * @return True if the job can continue to run. False if it can't.
	 */
	public boolean mayLetJobContinue(int jobId) {
		try (var sql = new ContinueCheckSQL()) {
			return sql.transactionRead(() -> sql.mayLetJobContinue(jobId));
		}
	}

	/**
	 * Has the execution of a job exceeded its group's resource allocation at
	 * this point?
	 *
	 * @param jobId
	 *            What job is consuming resources?
	 * @return False if the job should be killed. True otherwise.
	 */
	public boolean shouldKillJob(int jobId) {
		return !mayLetJobContinue(jobId);
	}

	private final class ContinueCheckSQL extends AbstractSQL {
		private final Query getUsageAndQuota =
				conn.query(GET_JOB_USAGE_AND_QUOTA);

		@Override
		public void close() {
			getUsageAndQuota.close();
			super.close();
		}

		private boolean mayLetJobContinue(int jobId) {
			return getUsageAndQuota.call1(
					// If we have an entry, check if usage <= quota
					row -> row.getInt("quota_used") <= row.getInt("quota"),
					jobId)

					// Otherwise, we'll just allow it
					.orElse(true);
		}
	}

	/**
	 * Adjust a group's quota.
	 *
	 * @param groupId
	 *            Which group's quota to change
	 * @param delta
	 *            Amount to change by, in board-seconds
	 * @return Information about what group's quota was adjusted and what it has
	 *         become.
	 */
	public Optional<AdjustedQuota> addQuota(int groupId, int delta) {
		try (var sql = new AdjustQuotaSQL()) {
			return sql.transaction(() -> sql.adjustQuota(groupId, delta));
		}
	}

	/**
	 * Describes the result of the {@link QuotaManager#addQuota(int,int)}
	 * operation.
	 *
	 * @author Donal Fellows
	 * @param name
	 *            The name of the group.
	 * @param quota
	 *            The new quota of the group.
	 */
	public record AdjustedQuota(String name, Long quota) {
		private AdjustedQuota(Row row) {
			this(row.getString("group_name"), row.getLong("quota"));
		}
	}

	private final class AdjustQuotaSQL extends AbstractSQL {
		private final Update adjustQuota = conn.update(ADJUST_QUOTA);

		private final Query getQuota = conn.query(GET_GROUP_QUOTA);

		@Override
		public void close() {
			adjustQuota.close();
			getQuota.close();
			super.close();
		}

		private Optional<AdjustedQuota> adjustQuota(int groupId, int delta) {
			if (adjustQuota.call(delta, groupId) == 0) {
				return Optional.empty();
			}
			return getQuota.call1(AdjustedQuota::new, groupId);
		}
	}

	/**
	 * Consolidates usage from finished jobs onto quotas. Runs hourly.
	 */
	@Scheduled(cron = "#{quotaProperties.consolidationSchedule}")
	public void consolidateQuotas() {
		if (control.isPaused()) {
			return;
		}
		// Split off for testability
		try (var c = getConnection()) {
			doConsolidate(c);
		} catch (DataAccessException e) {
			if (isBusy(e)) {
				log.info("database is busy; "
						+ "will try job quota consolidation processing later");
				return;
			}
			throw e;
		}
	}

	private void doConsolidate(Connection c) {
		try (var sql = new ConsolidateSQL(c)) {
			sql.transaction(sql::consolidate);
		}
	}

	private final class ConsolidateSQL extends AbstractSQL {
		private final Query getConsoldationTargets =
				conn.query(GET_CONSOLIDATION_TARGETS);

		private final Update decrementQuota = conn.update(DECREMENT_QUOTA);

		private final Update markConsolidated = conn.update(MARK_CONSOLIDATED);

		ConsolidateSQL(Connection c) {
			super(c);
		}

		@Override
		public void close() {
			getConsoldationTargets.close();
			decrementQuota.close();
			markConsolidated.close();
			super.close();
		}

		// Result is arbitrary and ignored
		private Void consolidate() {
			for (var target : getConsoldationTargets.call(Target::new)) {
				decrementQuota.call(target.quotaUsed, target.groupId);
				markConsolidated.call(target.jobId);
			}
			return null;
		}

		private class Target {
			final Object quotaUsed;

			final int groupId;

			final int jobId;

			Target(Row row) {
				quotaUsed = row.getObject("quota_used");
				groupId = row.getInt("group_id");
				jobId = row.getInt("job_id");
			}
		}
	}

	/**
	 * @param quota
	 *            The size of quota remaining, in board-seconds.
	 * @param units
	 *            The units that the quota was measured in on the NMPI.
	 */
	private record QuotaInfo(long quota, String units) {
	}

	private QuotaInfo parseQuotaData(List<Project> projects) {
		String units = null;
		long total = 0;
		for (var project : projects) {
			for (var quota : project.getQuotas()) {
				if (!quota.getPlatform().equals(quotaProps.getNMPIPlaform())) {
					continue;
				}
				if (units == null) {
					units = quota.getUnits();
				} else if (units != quota.getUnits()) {
					throw new RuntimeException("Quotas have multiple units!");
				}

				switch (quota.getUnits()) {
				case BOARD_SECONDS:
					total += (long) (quota.getLimit() - quota.getUsage());
					break;
				case CORE_HOURS:
					total += toBoardSeconds(quota.getLimit())
							- toBoardSeconds(quota.getUsage());
					break;
				default:
					throw new RuntimeException(
							"Unknown Quota units: " + quota.getUnits());
				}
			}
		}
		return new QuotaInfo(total, units);
	}

	final Optional<String> mayCreateNMPISession(String collab) {
		// Read collab from NMPI; fail if not there
		var projects = nmpi.getProjects(STATUS_ACCEPTED, collab);
		var info = parseQuotaData(projects);

		log.debug("Setting quota of collab {} to {}", collab, info.quota());

		// Update quota in group for collab from NMPI
		try (var c = getConnection();
				var setQuota = c.update(SET_COLLAB_QUOTA)) {
			c.transaction(() -> setQuota.call(info.quota(), collab));
		}

		if (info.quota() > 0) {
			return Optional.of(info.units());
		}
		return Optional.empty();
	}

	void associateNMPISession(int jobId, String user, String collab,
			String quotaUnits) {
		var sessionId = nmpi.createSession(collab, user);

		// Associate NMPI session with Job in the database
		try (var c = getConnection();
				var setSession = c.update(SET_JOB_SESSION)) {
			c.transaction(
					() -> setSession.call(jobId, sessionId, quotaUnits));
		}
	}

	private final class InflateUser extends AbstractSQL {
		private final Query getUserByName =
				conn.query(GET_USER_DETAILS_BY_NAME);

		private final Query getGroupByName = conn.query(GET_GROUP_BY_NAME);

		private final Update createUser = conn.update(CREATE_USER);

		private final Update createGroup =
				conn.update(CREATE_GROUP_IF_NOT_EXISTS);

		private final Update addUserToGroup = conn.update(ADD_USER_TO_GROUP);

		@Override
		public void close() {
			getUserByName.close();
			getGroupByName.close();
			createUser.close();
			createGroup.close();
			addUserToGroup.close();
			super.close();
		}

		private Optional<Integer> getUserByName(String user) {
			return getUserByName.call1(r -> r.getInt("user_id"), user);
		}

		private Optional<Integer> getGroupByName(String group) {
			return getGroupByName.call1(r -> r.getInt("group_id"), group);
		}

		private boolean createUser(String user) {
			return createUser.call(user, null, USER, false, null) > 0;
		}

		private boolean createGroup(String group) {
			return createGroup.call(group, 0.0, COLLABRATORY) > 0;
		}

		private boolean addUserToGroup(int userId, Optional<Integer> groupId) {
			return addUserToGroup.call(userId, groupId) > 0;
		}

		/**
		 * Construct the user and group records if necessary and associate them.
		 *
		 * @param user
		 *            The user name.
		 * @param job
		 *            The NMPI job details containing the collabratory (group)
		 *            name.
		 */
		void inflateUser(String user, Job job) {
			var userId = getUserByName(user).or(() -> {
				/*
				 * The user has never logged in directly, so we have to create
				 * them.
				 */
				if (!createUser(user)) {
					log.warn("failed to make user: {}", user);
				}
				return getUserByName(user);
			});

			createGroup(job.getCollab());
			var groupId = getGroupByName(job.getCollab());
			addUserToGroup(userId.orElseThrow(() -> new IllegalStateException(
					"failed to find or create user")), groupId);
		}
	}

	final Optional<NMPIJobQuotaDetails> mayUseNMPIJob(String user,
			int nmpiJobId) {
		// Read job from NMPI to get collab ID
		var job = nmpi.getJob(nmpiJobId);

		// If it is possible to run this job, we need to associate the user
		// with it because only special users can run jobs like this.
		try (var sql = new InflateUser()) {
			sql.transaction(() -> sql.inflateUser(user, job));
		}

		// This is now a collab so check there instead
		var quotaUnits = mayCreateNMPISession(job.getCollab());
		if (quotaUnits.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(
				new NMPIJobQuotaDetails(job.getCollab(), quotaUnits.get()));
	}

	/**
	 * @param collabId
	 *            The collaboratory ID.
	 * @param quotaUnits
	 *            The units of the Quota.
	 */
	record NMPIJobQuotaDetails(String collabId, String quotaUnits) {
	}

	void associateNMPIJob(int jobId, int nmpiJobId, String quotaUnits) {
		// Associate NMPI Job with job in database
		try (var c = getConnection();
				var setNMPIJob = c.update(SET_JOB_NMPI_JOB)) {
			c.transaction(() -> setNMPIJob.call(jobId, nmpiJobId, quotaUnits));
		}
	}

	/** Results of database queries. */
	private record FinishInfo(Optional<Long> quota, Optional<Session> session,
			Optional<NMPIJob> job) {
	}

	private FinishInfo getFinishingInfo(int jobId) {
		try (var c = getConnection();
				var getSession = c.query(GET_JOB_SESSION);
				var getNMPIJob = c.query(GET_JOB_NMPI_JOB);
				var getUsage = c.query(GET_JOB_USAGE_AND_QUOTA)) {
			// Get the quota used
			return c.transaction(false,
					() -> new FinishInfo(
							getUsage.call1(r -> r.getLong("quota_used"), jobId),
							getSession.call1(Session::new, jobId),
							getNMPIJob.call1(NMPIJob::new, jobId)));
		}
	}

	final void finishJob(int jobId) {
		// Get the information about the job from the DB
		var info = getFinishingInfo(jobId);

		// From here on, we don't touch the DB but we do touch the network

		if (!info.quota().isPresent()) {
			// No quota? No update!
			return;
		}

		// If job has associated session, update quota in session
		info.session().ifPresent(session -> {
			try {
				nmpi.setSessionStatusAndResources(session.id(), "finished",
						getResourceUsage(info.quota().get(),
								session.quotaUnits()));
			} catch (BadRequestException e) {
				log.error(e.getResponse().readEntity(String.class));
				throw e;
			}
		});

		// If job has associated NMPI job, update quota on NMPI job
		info.job().ifPresent(nmpiJob -> {
			try {
				nmpi.setJobResources(nmpiJob.id(), getResourceUsage(
						info.quota().get(), nmpiJob.quotaUnits()));
			} catch (BadRequestException e) {
				log.error(e.getResponse().readEntity(String.class));
				throw e;
			}
		});
	}

	private record Session(int id, String quotaUnits) {
		private Session(Row r) {
			this(r.getInt("session_id"), r.getString("quota_units"));
		}
	}

	private record NMPIJob(int id, String quotaUnits) {
		private NMPIJob(Row r) {
			this(r.getInt("nmpi_job_id"), r.getString("quota_units"));
		}
	}

	private static ResourceUsage getResourceUsage(long boardSeconds,
			String units) {
		var resourceUsage = new ResourceUsage();
		resourceUsage.setUnits(units);
		if (units.equals(BOARD_SECONDS)) {
			resourceUsage.setValue(boardSeconds);
		} else if (units.equals(CORE_HOURS)) {
			resourceUsage.setValue(toCoreHours(boardSeconds));
		} else {
			throw new IllegalArgumentException("Unknown units " + units);
		}
		return resourceUsage;
	}

	/**
	 * Convert board-seconds to core-hours (approximately).
	 *
	 * @param boardSeconds
	 *            The number of board-seconds to convert.
	 * @return The number of board-hours, which may have fractional values.
	 */
	private static double toCoreHours(long boardSeconds) {
		return ((double) (boardSeconds * APPROX_CORES_PER_BOARD))
				/ SECONDS_PER_HOUR;
	}

	/**
	 * Convert core-hours to board-seconds (approximately).
	 *
	 * @param coreHours
	 *            The number of core-hours to convert.
	 * @return The integer number of board seconds.
	 */
	private static long toBoardSeconds(double coreHours) {
		return (long) ((coreHours * SECONDS_PER_HOUR) / APPROX_CORES_PER_BOARD);
	}

	/**
	 * Operations for testing only.
	 *
	 * @hidden
	 */
	@ForTestingOnly
	interface TestAPI {
		/**
		 * Consolidates usage from finished jobs onto quotas.
		 *
		 * @param c
		 *            How to talk to the DB.
		 */
		void doConsolidate(Connection c);
	}

	/**
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 * @hidden
	 */
	@ForTestingOnly
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
	@Deprecated
	TestAPI getTestAPI() {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new TestAPI() {
			@Override
			public void doConsolidate(Connection c) {
				QuotaManager.this.doConsolidate(c);
			}
		};
	}
}

/**
 * Wrapper round the proxy and its API key. Does a bit of wrapping and
 * unwrapping of arguments and results.
 *
 * @author Donal Fellows
 */
final class NMPI {
	private final NMPIv3API proxy;

	private final String apiKey;

	private final String platform;

	NMPI(QuotaProperties quotaProps) {
		proxy = NMPIv3API.createClient(quotaProps.getNMPIUrl());
		apiKey = quotaProps.getNMPIApiKey();
		platform = quotaProps.getNMPIPlaform();
	}

	Job getJob(int jobId) {
		return proxy.getJob(apiKey, jobId);
	}

	void setJobResources(int jobId, ResourceUsage resources) {
		var wrapper = new JobResourceUpdate();
		wrapper.setResourceUsage(resources);
		proxy.setJobResources(apiKey, jobId, wrapper);
	}

	List<Project> getProjects(String status, String collab) {
		return proxy.getProjects(apiKey, status, collab);
	}

	int createSession(String collab, String user) {
		var request = new SessionRequest();
		request.setCollab(collab);
		request.setHardwarePlatform(platform);
		request.setUserId(user);
		return proxy.createSession(apiKey, request).getId();
	}

	void setSessionStatusAndResources(int sessionId, String status,
			ResourceUsage resources) {
		var wrapper = new SessionResourceUpdate();
		wrapper.setStatus(status);
		wrapper.setResourceUsage(resources);
		proxy.setSessionStatusAndResources(apiKey, sessionId, wrapper);
	}
}
