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

import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.alloc.db.SQLQueries;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * A report of an issue with a board.
 *
 * @author Donal Fellows
 */
public class BoardIssueReport {
	private int id;

	private int boardId;

	private String issue;

	private String reporter;

	private Instant timestamp;

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

	public void setId(int id) {
		this.id = id;
	}

	/** @return The board ID. */
	public int getBoardId() {
		return boardId;
	}

	public void setBoardId(int id) {
		this.boardId = id;
	}

	/**
	 * @return What did they report?
	 */
	public String getIssue() {
		return issue;
	}

	public void setIssue(String issue) {
		this.issue = issue;
	}

	/**
	 * @return Who reported it?
	 */
	public String getReporter() {
		return reporter;
	}

	public void setReporter(String reporter) {
		this.reporter = reporter;
	}

	/**
	 * @return When was it reported?
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
}
