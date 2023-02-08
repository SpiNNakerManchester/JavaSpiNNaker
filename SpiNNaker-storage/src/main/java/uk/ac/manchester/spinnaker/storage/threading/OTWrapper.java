/*
 * Copyright (c) 2021 The University of Manchester
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
abstract sealed class OTWrapper implements
		Wrapper permits OTConnection, OTMeta, OTResults, OTRSMeta, OTStatement {
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
		return new OTConnection(ot, c, false);
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
