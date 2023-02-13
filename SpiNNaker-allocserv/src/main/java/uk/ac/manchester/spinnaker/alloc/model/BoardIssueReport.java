/*
 * Copyright (c) 2022-2023 The University of Manchester
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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * A report of an issue with a board.
 *
 * @author Donal Fellows
 */
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public class BoardIssueReport {
	private int id;

	private int boardId;

	private String issue;

	private String reporter;

	private Instant timestamp;

	/** Create a record. */
	public BoardIssueReport() {
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
		id = row.getInt("report_id");
		boardId = row.getInt("board_id");
		issue = row.getString("reported_issue");
		reporter = row.getString("reporter_name");
		timestamp = row.getInstant("report_timestamp");
	}

	/** @return The report ID. */
	public int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}

	/** @return The board ID. */
	public int getBoardId() {
		return boardId;
	}

	void setBoardId(int id) {
		this.boardId = id;
	}

	/** @return What did they report? */
	public String getIssue() {
		return issue;
	}

	void setIssue(String issue) {
		this.issue = issue;
	}

	/** @return Who reported it? */
	public String getReporter() {
		return reporter;
	}

	void setReporter(String reporter) {
		this.reporter = reporter;
	}

	/** @return When was it reported? */
	public Instant getTimestamp() {
		return timestamp;
	}

	void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
}
