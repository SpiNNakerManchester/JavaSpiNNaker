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

import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.errorprone.annotations.RestrictedApi;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAPI.Update;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.Row;

/**
 * Manages user quotas.
 *
 * @author Donal Fellows
 */
@Service
public class QuotaManager extends DatabaseAwareBean {
	private static final Logger log = getLogger(QuotaManager.class);

	@Autowired
	private ServiceMasterControl control;

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
				if (isNull(quota)) {
					return true;
				}
				// Quota is defined; check if current usage exceeds it
				int usage = getCurrentUsage.call1(
						integer("current_usage"), groupId).orElse(0);
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
