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
package uk.ac.manchester.spinnaker.alloc.web;

import com.google.errorprone.annotations.Immutable;

/**
 * Describes whether an issue with a board was reported successfully.
 *
 * @author Donal Fellows
 */
@Immutable
public class IssueReportResponse {
	/**
	 * What immediate action will be taken. Typically "{@code noted}" or
	 * "{@code taken out of service}".
	 */
	public final String action;

	IssueReportResponse(String action) {
		this.action = action;
	}
}
