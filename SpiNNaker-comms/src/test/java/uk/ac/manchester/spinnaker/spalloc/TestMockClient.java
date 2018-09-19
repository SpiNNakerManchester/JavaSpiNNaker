package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

/**
 *
 * @author Christian
 */
public class TestMockClient {

        int timeout = 1000;
        SpallocClient client = new MockConnectedClient(timeout);
        
        void testListJobs() throws IOException, SpallocServerException, Exception {
            try (AutoCloseable c = client.withConnection()) {
                List<JobDescription> jobs = client.listJobs(timeout);
                assertNotNull(jobs);
		int[] expectedIDs = {
				47224, 47444
		};
                assertArrayEquals(expectedIDs, 
                        jobs.stream().mapToInt(j -> j.getJobID()).toArray());
            }
        }

        @Test
        void testListMachines() throws IOException, SpallocServerException, Exception {
            try (AutoCloseable c = client.withConnection()) {
                List<Machine> machines = client.listMachines(timeout);
                assertNotNull(machines);
            }
        }
        
        @Test
        void testJob() throws IOException, SpallocServerException, Exception {
            try (AutoCloseable c = client.withConnection()) {
                List<Integer> args = new ArrayList<>();
                Map<String, Object> kwargs = new HashMap<>();   
                kwargs.put("owner", "Unittest. OK to kill after 1 minute.");
                int jobId = client.createJob(args, kwargs, timeout);
                System.out.println(jobId);
                JobMachineInfo machineInfo = client.getJobMachineInfo(jobId, timeout);
                System.out.println(machineInfo);
                JobState state = client.getJobState(jobId, timeout);
                System.out.println(state.getState());
                client.jobKeepAlive(jobId, timeout);
                client.powerOffJobBoards(jobId, timeout);
                client.powerOnJobBoards(jobId, timeout);
                WhereIs whereis = client.whereIs(jobId, new ChipLocation(1,1), timeout);
                System.out.println(whereis);
                client.destroyJob(jobId, "Test finished", timeout);
             }   
        }

 }
