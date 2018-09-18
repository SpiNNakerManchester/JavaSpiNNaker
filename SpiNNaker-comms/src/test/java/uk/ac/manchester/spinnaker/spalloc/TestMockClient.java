package uk.ac.manchester.spinnaker.spalloc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;

/**
 *
 * @author Christian
 */
public class TestMockClient {

	@Test
	void testListJobs() throws Exception {
		int[] expectedIDs = {
				47224, 47444
		};
		try (SpallocClient client = new MockConnectedClient();
				AutoCloseable c = client.withConnection()) {
			List<JobDescription> jobs = client.listJobs();
			assertArrayEquals(expectedIDs,
					jobs.stream().mapToInt(j -> j.getJobID()).toArray());
		}
	}
}
