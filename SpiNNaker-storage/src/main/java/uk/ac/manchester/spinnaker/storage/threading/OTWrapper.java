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
package uk.ac.manchester.spinnaker.storage.threading;

import static java.lang.Thread.currentThread;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Wrapper;

/**
 * Core wrapper functionality, and single-threadedness enforcement point.
 *
 * @author Donal Fellows
 */
abstract class OTWrapper implements Wrapper {
	private final OneThread ot;

	private final Wrapper w;

	OTWrapper(OneThread ot, Wrapper w) {
		this.ot = ot;
		this.w = w;
	}

	@Override
	public final <T> T unwrap(Class<T> iface) throws SQLException {
		validateThread();
		return w.unwrap(iface);
	}

	@Override
	public final boolean isWrapperFor(Class<?> iface) throws SQLException {
		validateThread();
		return w.isWrapperFor(iface);
	}

	@Override
	public final String toString() {
		return w.toString();
	}

	@Override
	public final int hashCode() {
		return w.hashCode();
	}

	@Override
	public final boolean equals(Object o) {
		return w.equals(o);
	}

	/**
	 * The point that enforces the single-threaded nature of the wrapper.
	 *
	 * @throws IllegalStateException
	 *             When the current thread is the wrong thread.
	 */
	final void validateThread() {
		if (currentThread() != ot.thread) {
			throw new IllegalStateException(
					"use of database connection outside its owner thread");
		}
	}

	final CallableStatement wrap(CallableStatement c) {
		return new OTCallable(ot, c);
	}

	final PreparedStatement wrap(PreparedStatement c) {
		return new OTPrepared(ot, c);
	}

	final Statement wrap(Statement c) {
		return new OTStatement(ot, c);
	}

	final Connection wrap(Connection c) {
		return new OTConnection(ot, c);
	}

	final ResultSet wrap(ResultSet c) {
		return new OTResults(ot, c);
	}

	final ResultSet wrap(Statement s, ResultSet c) {
		return new OTResults(ot, s, c);
	}

	final ResultSetMetaData wrap(ResultSetMetaData metadata) {
		return new OTRSMeta(ot, metadata);
	}

	final DatabaseMetaData wrap(DatabaseMetaData metadata) {
		return new OTMeta(ot, metadata);
	}
}
