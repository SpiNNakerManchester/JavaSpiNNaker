/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.board;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 *
 * @author Christian
 */
public class TestJson {

	private static ObjectMapper createMapper() {
		return new ObjectMapper().registerModule(new SimpleModule())
				.setPropertyNamingStrategy(LOWER_CAMEL_CASE)
				.configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
				.configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	}

	@Test
	public void testBMPCoordsAsArray() throws IOException {
		var json = "[1, 2]";
		var mapper = createMapper();
		var fromJson = mapper.readValue(json, BMPCoords.class);
		assertEquals(new BMPCoords(1, 2), fromJson);

		assertThrows(IOException.class,
				() -> mapper.readValue("[]", BMPCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("[1]", BMPCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("[1,2,3]", BMPCoords.class));
	}

	@Test
	public void testBMPCoordsAsObject() throws IOException {
		var json = "{\"c\":1,\"f\":2}";
		var mapper = createMapper();
		var fromJson = mapper.readValue(json, BMPCoords.class);
		assertEquals(new BMPCoords(1, 2), fromJson);

		assertThrows(IOException.class,
				() -> mapper.readValue("{}", BMPCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("{\"c\":1}", BMPCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("{\"f\":2}", BMPCoords.class));
		assertThrows(IOException.class, () -> mapper
				.readValue("{\"c\":1,\"f\":2,\"b\":3}", BMPCoords.class));
		assertThrows(IOException.class, () -> mapper
				.readValue("{\"c\":1,\"f\":2,\"c\":3}", BMPCoords.class));
	}

	@Test
	public void testBMPCoordsAsString() throws IOException {
		var json = "\"[c:1,f:2]\"";
		var mapper = createMapper();
		var fromJson = mapper.readValue(json, BMPCoords.class);
		assertEquals(new BMPCoords(1, 2), fromJson);

		assertThrows(IllegalArgumentException.class,
				() -> mapper.readValue("\"gorp\"", BMPCoords.class));
	}

	@Test
	public void testBMPCoordsToJson() throws IOException {
		var mapper = createMapper();
		assertEquals("{\"cabinet\":1,\"frame\":2}",
				mapper.writeValueAsString(new BMPCoords(1, 2)));
	}

	@Test
	public void testPhysicalCoordsAsArray() throws IOException {
		var json = "[1, 2, 3]";
		var mapper = createMapper();
		var fromJson = mapper.readValue(json, PhysicalCoords.class);
		assertEquals(new PhysicalCoords(1, 2, 3), fromJson);

		assertThrows(IOException.class,
				() -> mapper.readValue("[]", PhysicalCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("[1]", PhysicalCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("[1,2]", PhysicalCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("[1,2,3,4]", PhysicalCoords.class));
	}

	@Test
	public void testPhysicalCoordsAsObject() throws IOException {
		var json = "{\"c\":1,\"f\":2,\"b\":3}";
		var mapper = createMapper();
		var fromJson = mapper.readValue(json, PhysicalCoords.class);
		assertEquals(new PhysicalCoords(1, 2, 3), fromJson);

		assertThrows(IOException.class,
				() -> mapper.readValue("{}", PhysicalCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("{\"c\":1}", PhysicalCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("{\"f\":2}", PhysicalCoords.class));
		assertThrows(IOException.class, () -> mapper
				.readValue("{\"c\":1,\"f\":2,\"x\":3}", PhysicalCoords.class));
		assertThrows(IOException.class, () -> mapper
				.readValue("{\"c\":1,\"f\":2,\"c\":3}", PhysicalCoords.class));
	}

	@Test
	public void testPhysicalCoordsAsString() throws IOException {
		var json = "\"[c:1,f:2,b:3]\"";
		var mapper = createMapper();
		var fromJson = mapper.readValue(json, PhysicalCoords.class);
		assertEquals(new PhysicalCoords(1, 2, 3), fromJson);

		assertThrows(IllegalArgumentException.class,
				() -> mapper.readValue("\"gorp\"", PhysicalCoords.class));
	}

	@Test
	public void testPhysicalCoordsToJson() throws IOException {
		var mapper = createMapper();
		assertEquals("{\"c\":1,\"f\":2,\"b\":3}",
				mapper.writeValueAsString(new PhysicalCoords(1, 2, 3)));
	}

	@Test
	public void testTriadCoordsAsArray() throws IOException {
		var json = "[1, 2, 0]";
		var mapper = createMapper();
		var fromJson = mapper.readValue(json, TriadCoords.class);
		assertEquals(new TriadCoords(1, 2, 0), fromJson);

		assertThrows(IOException.class,
				() -> mapper.readValue("[]", TriadCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("[1]", TriadCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("[1,2]", TriadCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("[1,2,3,4]", TriadCoords.class));
	}

	@Test
	public void testTriadCoordsAsObject() throws IOException {
		var json = "{\"x\":1,\"y\":2,\"z\":0}";
		var mapper = createMapper();
		var fromJson = mapper.readValue(json, TriadCoords.class);
		assertEquals(new TriadCoords(1, 2, 0), fromJson);

		assertThrows(IOException.class,
				() -> mapper.readValue("{}", TriadCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("{\"x\":1}", TriadCoords.class));
		assertThrows(IOException.class,
				() -> mapper.readValue("{\"y\":2}", TriadCoords.class));
		assertThrows(IOException.class, () -> mapper
				.readValue("{\"x\":1,\"y\":2,\"b\":3}", TriadCoords.class));
		assertThrows(IOException.class, () -> mapper
				.readValue("{\"x\":1,\"y\":2,\"x\":3}", TriadCoords.class));
	}

	@Test
	public void testTriadCoordsAsString() throws IOException {
		var json = "\"[x:1,y:2,z:0]\"";
		var mapper = createMapper();
		var fromJson = mapper.readValue(json, TriadCoords.class);
		assertEquals(new TriadCoords(1, 2, 0), fromJson);

		assertThrows(IllegalArgumentException.class,
				() -> mapper.readValue("\"gorp\"", TriadCoords.class));
	}

	@Test
	public void testTriadCoordsToJson() throws IOException {
		var mapper = createMapper();
		assertEquals("{\"x\":1,\"y\":2,\"z\":0}",
				mapper.writeValueAsString(new TriadCoords(1, 2, 0)));
	}
}
