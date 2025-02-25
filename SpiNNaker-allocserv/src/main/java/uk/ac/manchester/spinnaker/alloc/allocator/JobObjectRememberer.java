/*
 * Copyright (c) 2022 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.allocator;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.alloc.proxy.ProxyCore;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * Remembers job objects so that they can be closed when the state of a
 * job invalidates them. This class takes care to be thread-safe. The
 * information it holds is <em>not</em> persistent.
 *
 * @author Donal Fellows
 */
@Component
class JobObjectRememberer {

	private static final Log log = LogFactory.getLog(JobObjectRememberer.class);

	@GuardedBy("this")
	private final Map<Integer, List<ProxyCore>> proxies = new HashMap<>();

	@GuardedBy("this")
	private final Map<Integer, TransceiverInterface> transceivers =
			new HashMap<>();

	/**
	 * Called when service is shutting down. Kill <em>everything!</em>
	 */
	@PreDestroy
	private synchronized void closeAll() {
		proxies.values().forEach(list -> list.forEach(ProxyCore::close));
		proxies.clear(); // Just in case
		transceivers.values().forEach(txrx -> {
			try {
				txrx.close();
			} catch (IOException e) {
				log.error("Error closing Transceiver", e);
			}});
	}

	/**
	 * Note down that a job has a websocket proxy active.
	 *
	 * @param jobId
	 *            The job ID.
	 * @param proxy
	 *            The websocket proxy.
	 */
	synchronized void rememberProxyForJob(Integer jobId, ProxyCore proxy) {
		proxies.computeIfAbsent(jobId, __ -> new ArrayList<>()).add(proxy);
	}

	/**
	 * Stop remembering a job's particular websocket proxy.
	 *
	 * @param jobId
	 *            The job ID.
	 * @param proxy
	 *            The websocket proxy.
	 */
	synchronized void removeProxyForJob(Integer jobId, ProxyCore proxy) {
		var list = proxies.get(jobId);
		if (nonNull(list)) {
			list.remove(proxy);
		}
	}

	/** Set the transceiver for a job.
	 *
	 * @param jobId The job ID.
	 * @param txrx The transceiver.
	 */
	synchronized void setTransceiverForJob(Integer jobId,
			TransceiverInterface txrx) {
		if (transceivers.containsKey(jobId)) {
			throw new RuntimeException(
					"Job " + jobId + " already has a transciever!");
		}
		transceivers.put(jobId, txrx);
	}

	/*
	 * Get the transceiver for a job.
	 *
	 * @param jobId The job ID.
	 *
	 * @return The transceiver.
	 */
	synchronized TransceiverInterface getTransceiverForJob(Integer jobId) {
		if (transceivers.containsKey(jobId)) {
			return transceivers.get(jobId);
		}
		throw new RuntimeException(
				"Job " + jobId + " does not have a transciever!");
	}

	/** Check if the transceiver for a job is set.
	 *
	 * @param jobId The job ID.
	 * @return True if the transceiver is set.
	 */
	synchronized boolean isTransceiverForJob(Integer jobId) {
		return transceivers.containsKey(jobId);
	}

	private synchronized List<ProxyCore> removeProxyListForJob(Integer jobId) {
		return proxies.remove(jobId);
	}

	/**
	 * Close all remembered objects for a job. This is called when the
	 * state of a job changes significantly (i.e., when the set of boards that
	 * may be communicated with changes).
	 *
	 * @param jobId
	 *            The job ID.
	 */
	void closeJob(Integer jobId) {
		var list = removeProxyListForJob(jobId);
		if (nonNull(list)) {
			list.forEach(ProxyCore::close);
		}
		synchronized (this) {
			var txrx = transceivers.remove(jobId);
			if (txrx != null) {
				try {
					txrx.close();
				} catch (IOException e) {
					log.error("Error closing Transceiver", e);
				}
			}
		}
	}
}
