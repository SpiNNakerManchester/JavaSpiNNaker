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

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.ws.rs.BadRequestException;

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
import uk.ac.manchester.spinnaker.alloc.nmpi.JobResourceUpdate;
import uk.ac.manchester.spinnaker.alloc.nmpi.NMPIv3API;
import uk.ac.manchester.spinnaker.alloc.nmpi.ResourceUsage;
import uk.ac.manchester.spinnaker.alloc.nmpi.SessionRequest;
import uk.ac.manchester.spinnaker.alloc.nmpi.SessionResourceUpdate;
import uk.ac.manchester.spinnaker.alloc.nmpi.SessionResponse;

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

	private NMPIv3API nmpiProxy = null;

	@PostConstruct
	public void createProxy() {
		String nmpiUrl = quotaProps.getNMPIUrl();
		if (!nmpiUrl.isEmpty()) {
			nmpiProxy = NMPIv3API.createClient(quotaProps.getNMPIUrl());
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

	private class CreateCheckSQL extends AbstractSQL {
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

	private class ContinueCheckSQL extends AbstractSQL {
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
	 */
	public static final class AdjustedQuota {
		private final String name;

		private final Long quota;

		private AdjustedQuota(Row row) {
			this.name = row.getString("group_name");
			this.quota = row.getLong("quota");
		}

		/** @return The name of the group. */
		public String getName() {
			return name;
		}

		/** @return The new quota of the group. */
		public Long getQuota() {
			return quota;
		}
	}

	private class AdjustQuotaSQL extends AbstractSQL {
		private final Update adjustQuota = conn.update(ADJUST_QUOTA);

		private final Query getQuota = conn.query(GET_GROUP_QUOTA);

		@Override
		public void close() {
			adjustQuota.close();
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

	private class ConsolidateSQL extends AbstractSQL {
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
			Object quotaUsed;

			int groupId;

			int jobId;

			Target(Row row) {
				quotaUsed = row.getObject("quota_used");
				groupId = row.getInt("group_id");
				jobId = row.getInt("job_id");
			}
		}
	}

	Optional<String> mayCreateNMPISession(String collab) {
		// Read collab from NMPI; fail if not there
		var projects = nmpiProxy.getProjects(quotaProps.getNMPIApiKey(),
				STATUS_ACCEPTED, collab);
		String quotaUnits = null;
		long totalBoardSeconds = 0L;
		for (var project : projects) {
			for (var quota : project.getQuotas()) {
				if (quota.getPlatform().equals(quotaProps.getNMPIPlaform())) {
					if (quotaUnits == null) {
						quotaUnits = quota.getUnits();
					} else if (quotaUnits != quota.getUnits()) {
						throw new RuntimeException(
								"Quotas have multiple units!");
					}

					long limitBoardSeconds = (long) quota.getLimit();
					long usageBoardSeconds = (long) quota.getUsage();
					if (quota.getUnits().equals(CORE_HOURS)) {
						limitBoardSeconds = toBoardSeconds(quota.getLimit());
						usageBoardSeconds = toBoardSeconds(quota.getUsage());
					} else if (!quota.getUnits().equals(BOARD_SECONDS)) {
						throw new RuntimeException("Unknown Quota units: "
								+ quota.getUnits());
					}
					totalBoardSeconds += limitBoardSeconds - usageBoardSeconds;
				}
			}
		}

		log.debug("Setting quota of collab {} to {}", collab,
				totalBoardSeconds);

		// Update quota in group for collab from NMPI
		try (var c = getConnection();
				Update setQuota = c.update(SET_COLLAB_QUOTA)) {
			setQuota.call(totalBoardSeconds, collab);
		}

		if (totalBoardSeconds > 0) {
			return Optional.of(quotaUnits);
		}
		return Optional.empty();
	}

	void associateNMPISession(int jobId, String user, String collab,
			String quotaUnits) {
		SessionRequest request = new SessionRequest();
		request.setCollab(collab);
		request.setHardwarePlatform(quotaProps.getNMPIPlaform());
		request.setUserId(user);
		SessionResponse session = nmpiProxy.createSession(
				quotaProps.getNMPIApiKey(), request);

		// Associate NMPI session with Job in the database
		try (var c = getConnection();
				var setSession = c.update(SET_JOB_SESSION)) {
			setSession.call(jobId, session.getId(), quotaUnits);
		}
	}

	Optional<NMPIJobQuotaDetails> mayUseNMPIJob(String user, int nmpiJobId) {
		// Read job from NMPI to get collab ID
		var job = nmpiProxy.getJob(quotaProps.getNMPIApiKey(), nmpiJobId);

		// If it is possible to run this job, we need to associate the user
		// with it because only special users can run jobs like this.
		try (var c = getConnection();
				Query getUserByName = c.query(GET_USER_DETAILS_BY_NAME);
				Query getGroupByName = c.query(GET_GROUP_BY_NAME);
				Update createUser = c.update(CREATE_USER);
				Update createGroup = c.update(CREATE_GROUP_IF_NOT_EXISTS);
				Update addUserToGroup = c.update(ADD_USER_TO_GROUP)) {
			createGroup.call(job.getCollab(), 0.0, COLLABRATORY);
			var userId = getUserByName.call1(r -> r.getInt("user_id"), user);

			// The user has never logged in directly, so we have to create them
			if (!userId.isPresent()) {
				createUser.call(user, null, USER, false, null);
				userId = getUserByName.call1(r -> r.getInt("user_id"), user);
			}
			var groupId = getGroupByName.call1(r -> r.getInt("group_id"),
					job.getCollab());
			addUserToGroup.call(userId.get(), groupId);
		}

		// This is now a collab so check there instead
		var quotaUnits = mayCreateNMPISession(job.getCollab());
		if (quotaUnits.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(
				new NMPIJobQuotaDetails(job.getCollab(), quotaUnits.get()));
	}

	final class NMPIJobQuotaDetails {
		/**
		 * The collaboratory ID.
		 */
		final String collabId;

		/**
		 * The units of the Quota.
		 */
		final String quotaUnits;

		private NMPIJobQuotaDetails(String collabId, String quotaUnits) {
			this.collabId = collabId;
			this.quotaUnits = quotaUnits;
		}
	}

	void associateNMPIJob(int jobId, int nmpiJobId, String quotaUnits) {
		// Associate NMPI Job with job in database
		try (var c = getConnection();
				var setNMPIJob = c.update(SET_JOB_NMPI_JOB)) {
			setNMPIJob.call(jobId, nmpiJobId, quotaUnits);
		}
	}

	void finishJob(int jobId) {
		try (var c = getConnection();
				var getSession = c.query(GET_JOB_SESSION);
				var getNMPIJob = c.query(GET_JOB_NMPI_JOB);
				var getUsage = c.query(GET_JOB_USAGE_AND_QUOTA)) {

			// Get the quota used
			var quota = getUsage.call1(
					r -> r.getLong("quota_used"), jobId);
			// If job has associated session, update quota in session
			getSession.call1(
					r -> new Session(r), jobId).ifPresent(
					session -> {
							try {
								var update = new SessionResourceUpdate();
								update.setStatus("finished");
								update.setResourceUsage(getResourceUsage(
										quota.get(), session.quotaUnits));
								nmpiProxy.setSessionStatusAndResources(
										quotaProps.getNMPIApiKey(), session.id,
										update);
							} catch (BadRequestException e) {
								log.error(e.getResponse().readEntity(
										String.class));
								throw e;
							}
						});

			// If job has associated NMPI job, update quota on NMPI job
			getNMPIJob.call1(r -> new NMPIJob(r), jobId).ifPresent(
					nmpiJob -> {
						try {
							var update = new JobResourceUpdate();
							update.setResourceUsage(getResourceUsage(
									quota.get(), nmpiJob.quotaUnits));
							nmpiProxy.setJobResources(
									quotaProps.getNMPIApiKey(), nmpiJob.id,
									update);
						} catch (BadRequestException e) {
							log.error(e.getResponse().readEntity(String.class));
							throw e;
						}
					});
		}
	}

	private final class Session {
		private int id;

		private String quotaUnits;

		private Session(Row r) {
			this.id = r.getInt("session_id");
			this.quotaUnits = r.getString("quota_units");
		}
	}

	private final class NMPIJob {
		private int id;

		private String quotaUnits;

		private NMPIJob(Row r) {
			this.id = r.getInt("nmpi_job_id");
			this.quotaUnits = r.getString("quota_units");
		}
	}

	private static ResourceUsage getResourceUsage(long boardSeconds,
			String units) {
		ResourceUsage resourceUsage = new ResourceUsage();
		resourceUsage.setUnits(units);
		if (units.equals(BOARD_SECONDS)) {
			resourceUsage.setValue(boardSeconds);
		} else if (units.equals(CORE_HOURS)) {
			resourceUsage.setValue(toCoreHours(boardSeconds));
		} else {
			throw new RuntimeException("Unknown units " + units);
		}
		return resourceUsage;
	}

	/**
	 * Convert board-seconds to core-hours (approximately).
	 * @param boardSeconds The number of board-seconds to convert.
	 * @return The number of board-hours, which may have fractional values.
	 */
	private static double toCoreHours(long boardSeconds) {
		return ((double) (boardSeconds * APPROX_CORES_PER_BOARD))
				/ SECONDS_PER_HOUR;
	}

	/**
	 * Convert core-hours to board-seconds (approximately).
	 * @param coreHours The number of core-hours to convert.
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
