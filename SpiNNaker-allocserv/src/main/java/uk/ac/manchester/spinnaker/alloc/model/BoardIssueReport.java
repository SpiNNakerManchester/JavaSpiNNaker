/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * A report of an issue with a board.
 *
 * @author Donal Fellows
 * @param id
 *            The report ID.
 * @param boardId
 *            The board ID.
 * @param issue
 *            What did they report?
 * @param reporter
 *            Who reported it?
 * @param timestamp
 *            When was it reported?
 */
public record BoardIssueReport(int id, int boardId, String issue,
		String reporter, Instant timestamp) {
	@JsonCreator
	BoardIssueReport(@JsonProperty int id, @JsonProperty int boardId,
			@JsonProperty String issue, @JsonProperty String reporter,
			@JsonProperty String timestamp) {
		this(id, boardId, issue, reporter, Instant.parse(timestamp));
	}

	/**
	 * Create a record from a row.
	 *
	 * @param row
	 *            The database row.
	 * @see SQLQueries#GET_BOARD_REPORTS
	 */
	@UsedInJavadocOnly(SQLQueries.class)
	public BoardIssueReport(Row row) {
		this(row.getInt("report_id"), row.getInt("board_id"),
				row.getString("reported_issue"), row.getString("reporter_name"),
				row.getInstant("report_timestamp"));
	}
}
