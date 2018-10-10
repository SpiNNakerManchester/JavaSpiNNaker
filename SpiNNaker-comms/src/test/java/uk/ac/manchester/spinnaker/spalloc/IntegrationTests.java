/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.manchester.spinnaker.spalloc;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.spalloc.SpallocClient;
import uk.ac.manchester.spinnaker.spalloc.exceptions.JobDestroyedException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.messages.JobDescription;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.Machine;
import uk.ac.manchester.spinnaker.spalloc.messages.Notification;

/**
 *
 * @author micro
 */
public class IntegrationTests {
	private static final Logger log = getLogger(IntegrationTests.class);

	public static void main(String[] callargs)
			throws IOException, SpallocServerException, JobDestroyedException {
		String hostname = "spinnaker.cs.man.ac.uk";
		int port = 22244;
		Integer timeout = 1000;
		List<Integer> args = new ArrayList<>();
		args.add(1);
		args.add(1);
		Map<String, Object> kwargs = new HashMap<>();
		kwargs.put("owner", "Christian testing ok to kill");
		kwargs.put("keepalive", "10");
		kwargs.put("max_dead_boards", "2");
		try (SpallocClient client =
				new SpallocClient(hostname, port, timeout)) {
			log.info(client.toString());
			try (AutoCloseable c = client.withConnection()) {
				log.info(client.toString());
				int id = client.createJob(args, kwargs, timeout);
				log.info("New job: " + id);
				List<JobDescription> jobs = client.listJobs(timeout);
				for (JobDescription job : jobs) {
					log.info(job.toString());
				}
				List<Machine> machines = client.listMachines(timeout);
				for (Machine machine : machines) {
					log.info(machine.toString());
				}
				JobState state = client.getJobState(id);
				log.info(state.toString());
				JobMachineInfo machineInfo = client.getJobMachineInfo(id, null);
				log.info(machineInfo.toString());
				client.notifyMachine(machineInfo.getMachineName(), true, null);
				client.notifyJob(id, true, timeout);
				Notification notification = client.waitForNotification(50000);
				log.info("notify:" + notification);
				state = client.getJobState(id);
				log.info(state.toString());
				notification = client.waitForNotification(15000);
				log.info("notify:" + notification);
				notification = client.waitForNotification(15000);
				log.info("notify:" + notification);
				state = client.getJobState(id);
				log.info(state.toString());
			} catch (Exception ex) {
				log.error("problem in testing", ex);
			}

			// log.info(aJob.getHostname());
			// log.info(aJob.getID());
			// log.info(test.getMachineName());
			// log.info(test.getPower());
			// log.info(test.getState());
			// log.info(test.getDimensions());
			// log.info(test.getDestroyReason());
		}
	}
}
