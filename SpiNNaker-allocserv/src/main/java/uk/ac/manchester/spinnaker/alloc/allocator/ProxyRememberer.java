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
