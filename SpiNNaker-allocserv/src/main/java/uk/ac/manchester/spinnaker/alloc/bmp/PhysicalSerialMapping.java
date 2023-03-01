/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;
import static org.slf4j.LoggerFactory.getLogger;
import static org.apache.commons.io.IOUtils.buffer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Holds the mapping between physical board IDs and BMP IDs. Physical board IDs
 * were allocated by the manufacturer of the boards (Norcott) and are
 * <em>only</em> available in source form as labels on the boards. BMP
 * identifiers can be read remotely (they come from the LPC17xx In-Application
 * Programming entry point's {@code READ_SERIAL_NUMBER} call).
 * <p>
 * The original form of blacklists stores them according to their physical board
 * ID because that's what is easily available during commissioning.
 *
 * @author Donal Fellows
 */
@Component
class PhysicalSerialMapping {
	private static final Logger log = getLogger(PhysicalSerialMapping.class);

	@Value("classpath:blacklists/spin5-serial.txt")
	private Resource spin5serialFile;

	private final Map<String, String> physicalToLogical = new HashMap<>();

	private final Map<String, String> logicalToPhysical = new HashMap<>();

	@PostConstruct
	void loadMapping() throws IOException {
		try (var isr = new InputStreamReader(spin5serialFile.getInputStream(),
				UTF_8)) {
			buffer(isr).lines().map(s -> s.replaceFirst("#.*", "").strip())
					.filter(not(String::isBlank))
					.forEach(this::parseOneMapping);
		}
		log.info("loaded physical/logical board ID map: {} entries",
				physicalToLogical.size());
	}

	private void parseOneMapping(String line) {
		var bits = line.split("\\s+", 2);
		if (log.isTraceEnabled()) {
			log.trace("parsing line: {}", Arrays.toString(bits));
		}
		if (bits.length < 2) {
			log.debug("bogus line: {}", line);
			return;
		}
		var physical = bits[0];
		var logical = bits[1];
		var old = physicalToLogical.put(physical, logical);
		if (old != null) {
			log.warn("replaced mapping for {} (to {}) with {}", physical, old,
					logical);
		}
		old = logicalToPhysical.put(logical, physical);
		if (old != null) {
			log.warn("replaced mapping for {} (to {}) with {}", logical, old,
					physical);
		}
	}

	/**
	 * Given a BMP serial identifier, get the physical serial ID for that board.
	 *
	 * @param bmpSerialId
	 *            The BMP serial ID.
	 * @return The physical board identifier, or {@code null} if the BMP serial
	 *         ID is not recognised.
	 */
	public String getPhysicalId(String bmpSerialId) {
		return logicalToPhysical.get(bmpSerialId);
	}

	/**
	 * Given a physical serial identifier, get the BMP serial ID for that board.
	 *
	 * @param physicalSerialId
	 *            The physical board serial ID.
	 * @return The BMP identifier, or {@code null} if the physical board serial
	 *         ID is not recognised.
	 */
	public String getBMPId(String physicalSerialId) {
		return physicalToLogical.get(physicalSerialId);
	}
}
