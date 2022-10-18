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
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.alloc.proxy.ProxyCore;

/**
 * Remembers websocket proxies so that they can be closed when the state of a
 * job invalidates them. This class takes care to be thread-safe. The
 * information it holds is <em>not</em> persistent.
 *
 * @author Donal Fellows
 */
@Component
class ProxyRememberer {
	@GuardedBy("itself")
	private final Map<Integer, List<ProxyCore>> proxies = new HashMap<>();

	/**
	 * Called when service is shutting down. Kill <em>everything!</em>
	 */
	@PreDestroy
	private void closeAllProxies() {
		synchronized (proxies) {
			proxies.values().forEach(list -> list.forEach(ProxyCore::close));
			proxies.clear(); // Just in case
		}
	}

	/**
	 * Note down that a job has a websocket proxy active.
	 *
	 * @param jobId
	 *            The job ID.
	 * @param proxy
	 *            The websocket proxy.
	 */
	void rememberProxyForJob(Integer jobId, ProxyCore proxy) {
		synchronized (proxies) {
			proxies.computeIfAbsent(jobId, __ -> new ArrayList<>()).add(proxy);
		}
	}

	/**
	 * Stop remembering a job's particular websocket proxy.
	 *
	 * @param jobId
	 *            The job ID.
	 * @param proxy
	 *            The websocket proxy.
	 */
	void removeProxyForJob(Integer jobId, ProxyCore proxy) {
		synchronized (proxies) {
			var list = proxies.get(jobId);
			if (nonNull(list)) {
				list.remove(proxy);
			}
		}
	}

	private List<ProxyCore> removeProxyListForJob(Integer jobId) {
		synchronized (proxies) {
			return proxies.remove(jobId);
		}
	}

	/**
	 * Close all remembered websocket proxies for a job. This is called when the
	 * state of a job changes significantly (i.e., when the set of boards that
	 * may be communicated with changes).
	 *
	 * @param jobId
	 *            The job ID.
	 */
	void killProxies(Integer jobId) {
		var list = removeProxyListForJob(jobId);
		if (nonNull(list)) {
			list.forEach(ProxyCore::close);
		}
	}
}
