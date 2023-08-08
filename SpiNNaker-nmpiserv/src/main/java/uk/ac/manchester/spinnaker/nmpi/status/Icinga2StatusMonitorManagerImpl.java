/*
 * Copyright (c) 2020 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpi.status;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.ws.rs.WebApplicationException;

import org.apache.ws.commons.util.Base64;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.spinnaker.nmpi.jobmanager.JobManager;
import uk.ac.manchester.spinnaker.nmpi.model.Icinga2CheckResult;
import uk.ac.manchester.spinnaker.nmpi.rest.Icinga2;

/**
 * Status monitor manager that reports to Icinga.
 *
 */
public class Icinga2StatusMonitorManagerImpl implements StatusMonitorManager {
	/** The status to report - always 0 if reporting as OK. */
	private static final int STATUS = 0;

	/** The status message to report. */
	private static final String STATUS_MESSAGE = "OK";

	/** The multiplier for the TTL vs. the status update time. */
	private static final int STATUS_UPDATE_TTL_MULTIPLIER = 2;

	/** The proxy to speak to the service with. */
	private Icinga2 icinga;

	/** Authentication header to use when calling the service. */
	private String authHeader;

	/** The URL of the service. */
	@Value("${icinga2.url}")
	private URL icingaUrl;

	/** The host to report on. */
	@Value("${icinga2.host:#{null}}")
	private String host;

	/** The service to report on. */
	@Value("${icinga2.service:#{null}}")
	private String service;

	/** The username to log in with. */
	@Value("${icinga2.username}")
	private String username;

	/** The password to log in with. */
	@Value("${icinga2.password}")
	private String password;

	/** Logging. */
	private static final Logger logger =
			getLogger(Icinga2StatusMonitorManagerImpl.class);

	/**
	 * Initialise the service.
	 *
	 * @throws UnsupportedEncodingException
	 *             if the
	 * @throws MalformedURLException
	 *             if the URL isn't a URL
	 */
	@PostConstruct
	private void init() throws UnsupportedEncodingException {
		var userPassString = username + ":" + password;
		var userPassEnc = Base64.encode(userPassString.getBytes(US_ASCII));
		authHeader = "Basic " + userPassEnc;
		icinga = Icinga2.createClient(icingaUrl.toString());
	}

	@Override
	public void updateStatus(int runningJobs, int nBoardsInUse) {
		var performanceData = "'Running Jobs'=" + runningJobs
				+ " 'Boards In Use'=" + nBoardsInUse;
		int ttl =
				JobManager.STATUS_UPDATE_PERIOD * STATUS_UPDATE_TTL_MULTIPLIER;
		var result = new Icinga2CheckResult(STATUS, STATUS_MESSAGE,
				performanceData, ttl, host, service);
		try {
			var response = icinga.processCheckResult(authHeader, result);
			logger.debug("Status updated, result = {}", response);
		} catch (WebApplicationException e) {
			var response = e.getResponse();
			logger.error("Error updating to Icinga on {}:",
					response.getLocation(), e);
			logger.error("    Status: {}", response.getStatus());
			logger.error("    Message: {}",
					response.getStatusInfo().getReasonPhrase());
			logger.error("    Body: {}", response.readEntity(String.class));
		} catch (Throwable e) {
			logger.error("Error updating to Icinga", e);
		}
	}
}
