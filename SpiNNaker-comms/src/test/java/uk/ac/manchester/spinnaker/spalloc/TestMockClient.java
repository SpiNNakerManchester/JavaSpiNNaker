package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;

/**
 *
 * @author Christian
 */
public class TestMockClient {

	@Test
        void testlistJobs() throws IOException, SpallocServerException, Exception {
            SpallocClient client = new MockConnectedClient();
            try (AutoCloseable c = client.withConnection()) {
                List<JobDescription> jobs = client.listJobs();
                for (JobDescription job:jobs) {
                    System.out.println(job);
                }
            }

        }
}
