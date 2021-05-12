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
package uk.ac.manchester.spinnaker.alloc;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.KEBAB_CASE;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;

import org.json.JSONException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.alloc.web.ServiceDescription;
import uk.ac.manchester.spinnaker.alloc.web.StateResponse;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.messages.State;

class JsonTest {
	JsonFactory f = new JsonFactory();

	JsonTest() {
		ObjectMapper m = new ObjectMapper();
		m.setPropertyNamingStrategy(KEBAB_CASE);
		f.setCodec(m);
	}

	String serialize(Object obj) throws IOException {
		try (StringWriter w = new StringWriter();
				JsonGenerator g = f.createGenerator(w)) {
			g.writeObject(obj);
			return w.getBuffer().toString();
		}
	}

	@Nested
	class Serialization {
		@Test
		void testServiceDescription() throws IOException, JSONException {
			ServiceDescription d = new ServiceDescription();
			d.setVersion(new Version("1.2.3"));
			JSONAssert.assertEquals(
					"{ \"version\": { \"major-version\": 1,"
							+ "\"minor-version\": 2, \"revision\": 3 },"
							+ "\"jobs-ref\": null, \"machines-ref\": null }",
					serialize(d), false);
		}

		@Test
		void testStateResponse() throws IOException, JSONException {
			StateResponse r = new StateResponse();
			r.setState(State.READY);
			r.setStartTime(123F);
			r.setKeepaliveHost("127.0.0.1");
			r.setOwner("gorp");
			JSONAssert.assertEquals(
					"{ \"state\": 3, \"start-time\": 123.0, "
							+ "\"owner\": \"gorp\", "
							+ "\"keepalive-host\": \"127.0.0.1\" }",
					serialize(r), false);
		}
	}

}
