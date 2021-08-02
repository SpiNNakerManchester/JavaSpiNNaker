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

import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.query;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;

import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Row;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.SQLQueries;

/**
 * Manages user quotas.
 *
 * @author Donal Fellows
 */
@Component
public class QuotaManager extends SQLQueries {
	@Autowired
	private DatabaseEngine db;

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
	 * @throws SQLException
	 *             If database access fails.
	 */
	public boolean hasQuotaRemaining(int machineId, String user)
			throws SQLException {
		try (Connection c = db.getConnection();
				Query getQuota = query(c, GET_USER_QUOTA);
				Query getCurrentUsage = query(c, GET_CURRENT_USAGE)) {
			Integer quota = null;
			int userId = 0;
			for (Row row : getQuota.call(machineId, user)) {
				quota = row.getInteger("quota");
				userId = row.getInt("user_id");
			}
			if (quota == null) {
				return true;
			}
			// Quota is defined; check if current usage exceeds it
			if (quota > 0) {
				for (Row row : getCurrentUsage.call(machineId, userId)) {
					quota -= row.getInt("current_usage");
					break;
				}
			}
			// If board-seconds are left, we're good to go
			return (quota > 0);
		}
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
	 * @throws SQLException
	 *             If database access fails.
	 */
	public boolean hasQuotaRemaining(int machineId, int jobId)
			throws SQLException {
		try (Connection c = db.getConnection();
				Query getUsageAndQuota = query(c, GET_JOB_USAGE_AND_QUOTA)) {
			for (Row row : getUsageAndQuota.call(machineId, jobId)) {
				int usage = row.getInt("usage");
				int quota = row.getInt("quota");
				if (usage > quota) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Consolidates usage from finished jobs onto quotas. Runs hourly.
	 *
	 * @throws SQLException
	 *             If database access fails.
	 */
	@Scheduled(cron = "0 0 * * * *")
	public void consolidateQuotas() throws SQLException {
		try (Connection c = db.getConnection();
				Query getConsoldationTargets =
						query(c, GET_CONSOLIDATION_TARGETS);
				Update decrementQuota = update(c, DECREMENT_QUOTA);
				Update markConsolidated = update(c, MARK_CONSOLIDATED)) {
			transaction(c, () -> {
				for (Row row : getConsoldationTargets.call()) {
					decrementQuota.call(row.getObject("usage"),
							row.getInt("quota_id"));
					markConsolidated.call(row.getInt("job_id"));
				}
			});
		}
	}
}
