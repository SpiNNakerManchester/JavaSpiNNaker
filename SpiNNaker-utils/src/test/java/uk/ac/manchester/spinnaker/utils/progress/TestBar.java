/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.utils.progress;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;


/**
 *
 * @author Christian-B
 */
@SuppressWarnings("resource")
public class TestBar {

	private static final String PERCENTS =
			"|0%                          50%                         100%|";

	private static final String DASHES =
			" ------------------------------------------------------------";

	private static final String NEWLINE = "\\r?\\n";

	private static PrintStream printer(ByteArrayOutputStream baos) {
		return new PrintStream(baos, true, UTF_8);
	}

	private static String[] lines(ByteArrayOutputStream baos) {
		return baos.toString(UTF_8).stripTrailing().split(NEWLINE, -1);
	}

	@Test
	public void testBasic() {
		var pb = new ProgressBar(5, null,
				printer(new ByteArrayOutputStream()));
		for (int i = 0; i < 3; i++) {
			pb.update();
		}
		pb.close();
		// closed so error even if not at end.
		assertThrows(IllegalStateException.class, () -> {
			pb.update();
		});
		pb.close();

	}

	@Test
	public void testToMany() {
		var description = "tooMany";
		var pb = new ProgressBar(5, description,
				printer(new ByteArrayOutputStream()));
		for (int i = 0; i < 5; i++) {
			pb.update();
		}
		assertThrows(IllegalStateException.class, () -> {
			pb.update();
		});
		pb.close();
	}

	@Test
	public void testSimple() {
		var baos = new ByteArrayOutputStream();
		var description = "Easiest";
		var pb = new ProgressBar(5, description, printer(baos));
		for (int i = 0; i < 5; i++) {
			pb.update();
		}
		pb.close();
		var lines = lines(baos);
		assertEquals(4, lines.length);
		assertEquals(description, lines[0]);
		assertEquals(PERCENTS, lines[1]);
		assertEquals(DASHES, lines[2]);
	}

	@Test
	public void testNoDivSmall() {
		var baos = new ByteArrayOutputStream();
		var description = "Thirteen";
		try (var pb = new ProgressBar(13, description, printer(baos))) {
			for (int i = 0; i < 13; i++) {
				pb.update();
			}
		}
		var lines = lines(baos);
		// Include duration as try calls close.
		assertEquals(4, lines.length);
		assertEquals(description, lines[0]);
		assertEquals(PERCENTS, lines[1]);
		assertEquals(DASHES, lines[2]);
	}

	@Test
	public void testNoDivBig() {
		var baos = new ByteArrayOutputStream();
		var description = "Big";
		var pb = new ProgressBar(133, description, printer(baos));
		for (int i = 0; i < 133; i++) {
			pb.update();
		}
		var lines = lines(baos);
		// No close so no duration
		assertEquals(3, lines.length);
		assertEquals(description, lines[0]);
		assertEquals(PERCENTS, lines[1]);
		assertEquals(DASHES, lines[2]);
		pb.close();
	}

	@Test
	public void testStopEarly() {
		var baos = new ByteArrayOutputStream();
		var description = "Early";
		var pb = new ProgressBar(10, description, printer(baos));
		for (int i = 0; i < 3; i++) {
			pb.update();
		}
		pb.close();
		var lines = lines(baos);
		assertEquals(4, lines.length);
		assertEquals(description, lines[0]);
		assertEquals(PERCENTS, lines[1]);
		assertEquals(60 / 10 * 3 + 1, lines[2].length());
	}
}
