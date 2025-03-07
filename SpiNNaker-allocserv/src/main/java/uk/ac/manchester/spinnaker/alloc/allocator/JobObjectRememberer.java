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
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.protocols.FastDataIn;
import uk.ac.manchester.spinnaker.protocols.download.Downloader;
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

	private final Map<Integer, Map<ChipLocation, FastDataIn>> fastDataCache =
			new HashMap<>();

	private final Map<Integer, Map<ChipLocation, Downloader>> downloaders =
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
			}
		});
		transceivers.clear(); // Just in case
		fastDataCache.values().forEach(map -> map.values().forEach(fdi -> {
			try {
				fdi.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}));
		fastDataCache.clear(); // Just in case
		downloaders.values().forEach(map -> map.values().forEach(dl -> {
			try {
				dl.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}));
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

	/*
	 * Get the transceiver for a job.
	 *
	 * @param jobId The job ID.
	 *
	 * @return The transceiver or null if none.
	 */
	synchronized TransceiverInterface getTransceiverForJob(int jobId) {
		return transceivers.get(jobId);
	}

	/** Set the transceiver for a job.
	 *
	 * @param jobId The job ID.
	 * @param txrx The transceiver.
	 */
	synchronized void rememberTransceiverForJob(Integer jobId,
			TransceiverInterface txrx) {
		transceivers.put(jobId, txrx);
	}

	/** Get the fast data in for a job.
	 *
	 * @param jobId The job ID.
	 * @param chip  The ethernet chip to get the fast data in for.
	 * @return The fast data in or null if none.
	 */
	synchronized FastDataIn getFastDataIn(Integer jobId, ChipLocation chip) {
		return fastDataCache.getOrDefault(jobId, Map.of()).get(chip);
	}

	/**
	 * Remember the fast data in for a job.
	 *
	 * @param jobId The job ID.
	 * @param chip  The ethernet chip to remember the fast data in for.
	 * @param fdi   The fast data in.
	 */
	synchronized void rememberFastDataIn(Integer jobId, ChipLocation chip,
			FastDataIn fdi) {
		fastDataCache.computeIfAbsent(jobId, __ -> new HashMap<>()).put(
				chip, fdi);
	}

	/**
	 * Get the downloader for a job.
	 *
	 * @param jobId The job ID.
	 * @param chip  The ethernet chip to get the downloader for.
	 * @return The downloader or null if none.
	 */
	synchronized Downloader getDownloader(Integer jobId, ChipLocation chip) {
		return downloaders.getOrDefault(jobId, Map.of()).get(chip);
	}

	/**
	 * Remember the downloader for a job.
	 *
	 * @param jobId      The job ID.
	 * @param chip       The ethernet chip to remember the downloader for.
	 * @param downloader The downloader.
	 */
	synchronized void rememberDownloader(Integer jobId, ChipLocation chip,
			Downloader downloader) {
		downloaders.computeIfAbsent(jobId, __ -> new HashMap<>()).put(
				chip, downloader);
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
		synchronized (this) {
			var proxyList = proxies.remove(jobId);
			if (nonNull(proxyList)) {
				proxyList.forEach(ProxyCore::close);
			}
			var txrx = transceivers.remove(jobId);
			if (nonNull(txrx)) {
				try {
					txrx.close();
				} catch (IOException e) {
					log.error("Error closing Transceiver", e);
				}
			}
			var fdc = fastDataCache.remove(jobId);
			if (nonNull(fdc)) {
				fdc.values().forEach(fdi -> {
					try {
						fdi.close();
					} catch (IOException e) {
						log.error("Error closing FastDataIn", e);
					}
				});
			}
			var dl = downloaders.remove(jobId);
			if (nonNull(dl)) {
				dl.values().forEach(downloader -> {
					try {
						downloader.close();
					} catch (IOException e) {
						log.error("Error closing Downloader", e);
					}
				});
			}
		}
	}
}
