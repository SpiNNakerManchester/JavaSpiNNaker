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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.db.Utils.isBusy;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import uk.ac.manchester.spinnaker.alloc.ServiceMasterControl;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
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
	 * Can the user create another job at this point? If not, they're currently
	 * out of resources.
	 *
	 * @param machineId
	 *            On what machine do they want to create the job? Quotas are
	 *            theoretically per-machine.
	 * @param user
	 *            Who wants to create the job.
	 * @return True if they can make a job. False if they can't.
	 */
	public boolean mayCreateJob(int machineId, String user) {
		try (Connection c = getConnection();
				// These could be combined, but they're complicated enough
				Query getQuota = c.query(GET_USER_QUOTA);
				Query getCurrentUsage = c.query(GET_CURRENT_USAGE)) {
			return c.transaction(false, () -> mayCreateJob(machineId, user,
					getQuota, getCurrentUsage));
		}
	}

	private boolean mayCreateJob(int machineId, String user, Query getQuota,
			Query getCurrentUsage) {
		return getQuota.call1(machineId, user).map(result -> {
			Integer quota = result.getInteger("quota");
			if (isNull(quota)) {
				return true;
			}
			int userId = result.getInt("user_id");
			// Quota is defined; check if current usage exceeds it
			int usage = getCurrentUsage.call1(machineId, userId)
					.map(row -> row.getInteger("current_usage")).orElse(0);
			// If board-seconds are left, we're good to go
			return (quota > usage);
		}).orElse(true);
	}

	/**
	 * Has the execution of a job exceeded its owner's resource allocation at
	 * this point?
	 *
	 * @param machineId
	 *            On what machine is the job running? Quotas are theoretically
	 *            per-machine.
	 * @param jobId
	 *            What job is consuming resources?
	 * @return True if the job can continue to run. False if it can't.
	 */
	public boolean mayLetJobContinue(int machineId, int jobId) {
		try (Connection c = getConnection();
				Query getUsageAndQuota = c.query(GET_JOB_USAGE_AND_QUOTA)) {
			return c.transaction(false, () -> getUsageAndQuota
					.call1(machineId, jobId)
					// If we have an entry, check if usage <= quota
					.map(row -> row.getInt("usage") <= row.getInt("quota"))
					// Otherwise, we'll just allow it
					.orElse(true));
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
		try (Connection c = getConnection()) {
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

	// Accessible for testing only
	final void doConsolidate(Connection c) {
		try (Query getConsoldationTargets = c.query(GET_CONSOLIDATION_TARGETS);
				Update decrementQuota = c.update(DECREMENT_QUOTA);
				Update markConsolidated = c.update(MARK_CONSOLIDATED)) {
			c.transaction(() -> consolidate(getConsoldationTargets,
					decrementQuota, markConsolidated));
		}
	}

	private void consolidate(Query getConsoldationTargets,
			Update decrementQuota, Update markConsolidated) {
		for (Row row : getConsoldationTargets.call()) {
			decrementQuota.call(row.getObject("usage"), row.getInt("quota_id"));
			markConsolidated.call(row.getInt("job_id"));
		}
	}
}
