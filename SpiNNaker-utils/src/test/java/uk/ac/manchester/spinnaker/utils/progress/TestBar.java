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
package uk.ac.manchester.spinnaker.utils.progress;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestBar {

	private static final String PERCENTS =
			"|0%                          50%                         100%|";

	private static final String DASHES =
			" ------------------------------------------------------------";

	private static final String NEWLINE = "\\r?\\n";

	public TestBar() {
	}

	@Test
	public void testBasic() {
		var pb = new ProgressBar(5, null,
				new PrintStream(new ByteArrayOutputStream()));
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
				new PrintStream(new ByteArrayOutputStream()));
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
		var pb = new ProgressBar(5, description, new PrintStream(baos));
		for (int i = 0; i < 5; i++) {
			pb.update();
		}
		pb.close();
		var lines = baos.toString().split(NEWLINE);
		assertEquals(4, lines.length);
		assertEquals(description, lines[0]);
		assertEquals(PERCENTS, lines[1]);
		assertEquals(DASHES, lines[2]);
	}

	@Test
	public void testNoDivSmall() {
		var baos = new ByteArrayOutputStream();
		var description = "Thirteen";
		try (var pb = new ProgressBar(13, description, new PrintStream(baos))) {
			for (int i = 0; i < 13; i++) {
				pb.update();
			}
		}
		var lines = baos.toString().split(NEWLINE);
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
		var pb = new ProgressBar(133, description, new PrintStream(baos));
		for (int i = 0; i < 133; i++) {
			pb.update();
		}
		var lines = baos.toString().split(NEWLINE);
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
		var pb = new ProgressBar(10, description, new PrintStream(baos));
		for (int i = 0; i < 3; i++) {
			pb.update();
		}
		pb.close();
		var lines = baos.toString().split(NEWLINE);
		assertEquals(4, lines.length);
		assertEquals(description, lines[0]);
		assertEquals(PERCENTS, lines[1]);
		assertEquals(60 / 10 * 3 + 1, lines[2].length());
	}
}
