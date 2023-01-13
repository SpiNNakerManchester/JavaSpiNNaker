/*
 * Copyright (c) 2021-2022 The University of Manchester
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
			return getQuota.call1(groupId).map(result -> {
				var quota = result.getInteger("quota");
				if (isNull(quota)) {
					return true;
				}
				// Quota is defined; check if current usage exceeds it
				int usage = getCurrentUsage.call1(groupId)
						.map(integer("current_usage")).orElse(0);
				// If board-seconds are left, we're good to go
				return (quota > usage);
			}).orElse(true);
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
			return getUsageAndQuota.call1(jobId)
					// If we have an entry, check if usage <= quota
					.map(row -> row.getInt("usage") <= row.getInt("quota"))
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
			return sql.transaction(() -> sql.adjustQuota(groupId, delta)
					.map(AdjustedQuota::new));
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
		private final Query adjustQuota = conn.query(ADJUST_QUOTA);

		@Override
		public void close() {
			adjustQuota.close();
			super.close();
		}

		private Optional<Row> adjustQuota(int groupId, int delta) {
			return adjustQuota.call1(delta, groupId);
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
			for (var row : getConsoldationTargets.call()) {
				decrementQuota.call(row.getObject("usage"),
						row.getInt("group_id"));
				markConsolidated.call(row.getInt("job_id"));
			}
			return null;
		}
	}

	/** Operations for testing only. */
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
