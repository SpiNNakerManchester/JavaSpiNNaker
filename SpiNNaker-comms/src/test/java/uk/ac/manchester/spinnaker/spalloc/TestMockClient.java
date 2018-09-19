package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.State;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

/**
 * Tests the Spalloc Client ideally using an actual connection 
 *      but with a backup of canned replies if the connection fails/ timesout.
 * 
 * @author Christian
 */
public class TestMockClient {

        static int timeout = 1000;
        static MockConnectedClient client = new MockConnectedClient(timeout);
        
        void testListJobs() throws IOException, SpallocServerException, Exception {
            try (AutoCloseable c = client.withConnection()) {
                List<JobDescription> jobs = client.listJobs(timeout);
                if (client.isActual()) {
                    // Don't know the jobids currently on the machine if any
                    jobs.forEach(d -> assertThat("Jobid > 0", d.getJobID(), greaterThan(0)));
                } else {
                    int[] expectedIDs = { 47224, 47444};
                    assertArrayEquals(expectedIDs, 
                            jobs.stream().mapToInt(j -> j.getJobID()).toArray());
                }
            }
        }

        @Test
        void testListMachines() throws IOException, SpallocServerException, Exception {
            try (AutoCloseable c = client.withConnection()) {
                List<Machine> machines = client.listMachines(timeout);
                if (client.isActual()) {
                    // Don't know the jobids currently on the machine if any
                    machines.forEach(m -> assertNotNull(m.getName()));
                } else {
                    String[] expectedNames = { "Spin24b-223", "Spin24b-225", "Spin24b-226"};
                    List<String> foundNames = machines.stream().map(Machine::getName).collect(Collectors.toList());
                    assertArrayEquals(expectedNames, foundNames.toArray());
                }
            }
        }
        
        @Test
        void testJob() throws IOException, SpallocServerException, Exception {
            try (AutoCloseable c = client.withConnection()) {
                List<Integer> args = new ArrayList<>();
                Map<String, Object> kwargs = new HashMap<>();   
                kwargs.put("owner", "Unittest. OK to kill after 1 minute.");
                int jobId = client.createJob(args, kwargs, timeout);
                if (client.isActual()) {
                    assertThat("Jobid > 0", jobId, greaterThan(0));
                } else {
                    assertEquals(client.MOCK_ID, jobId);
                }
                JobMachineInfo machineInfo = client.getJobMachineInfo(jobId, timeout);
                List<Connection> connections = machineInfo.getConnections();
                String hostName = connections.get(0).getHostname();
                if (client.isActual()) {
                    InetAddress.getAllByName(hostName);
                } else {
                    assertEquals("10.11.223.33", hostName);
                }
                JobState state = client.getJobState(jobId, timeout);
                assertEquals(State.POWER, state.getState());
                assertTrue(state.getPower());
                client.jobKeepAlive(jobId, timeout);
                client.powerOffJobBoards(jobId, timeout);
                state = client.getJobState(jobId, timeout);
                assertEquals(State.POWER, state.getState());
                if (client.isActual()) {
                    assertFalse(state.getPower());
                }
                client.powerOnJobBoards(jobId, timeout);
                state = client.getJobState(jobId, timeout);
                assertEquals(State.POWER, state.getState());
                assertTrue(state.getPower());
                ChipLocation chip = new ChipLocation(1,1);
                WhereIs whereis = client.whereIs(jobId, chip, timeout);
                assertEquals(chip, whereis.getJobChip());
                assertEquals(jobId, whereis.getJobId());
                if (client.isActual()) {
                    assertNotNull(whereis.getBoardChip());
                } else {
                    assertEquals(chip, whereis.getBoardChip());
                }
                client.destroyJob(jobId, "Test finished", timeout);
                state = client.getJobState(jobId, timeout);
                if (client.isActual()) {
                   assertEquals(State.DESTROYED, state.getState());
                }
             }   
        }

 }
