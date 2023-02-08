/*
 * Copyright (c) 2022 The University of Manchester
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
