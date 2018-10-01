/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;
import uk.ac.manchester.spinnaker.spalloc.SpallocJob;
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
    public static void main(String[] callargs) throws IOException, SpallocServerException, JobDestroyedException {
        String hostname = "spinnaker.cs.man.ac.uk";
        int port = 22244;
        Integer timeout = 1000;
        List<Integer> args = new ArrayList<>();
        args.add(1);    
        args.add(1);
        Map<String, Object> kwargs = new HashMap<>();
        kwargs.put("owner", "Christian testing ok to kill");
        kwargs.put("keepalive", "10");
        kwargs.put("max_dead_boards","2");
        SpallocClient client = new SpallocClient(hostname, port, timeout);
        System.out.println(client);
        try (AutoCloseable c = client.withConnection()) {
            System.out.println(client);
            int id = client.createJob(args, kwargs, timeout);
            System.out.println("New job: " + id);
            List<JobDescription> jobs = client.listJobs(timeout);
            for (JobDescription job:jobs) {
                System.out.println(job);
            }
            List<Machine> machines = client.listMachines(timeout);
            for (Machine machine:machines) {
                System.out.println(machine);
            }
            JobState state = client.getJobState(id);
            System.out.println(state);
            JobMachineInfo machineInfo = client.getJobMachineInfo(id, null);
            System.out.println(machineInfo);
            client.notifyMachine(machineInfo.getMachineName(), true, null);
            client.notifyJob(id, true, timeout);
            Notification notification = client.waitForNotification(50000);
            System.out.println(notification);
            state = client.getJobState(id);
            System.out.println(state);
            notification = client.waitForNotification(15000);
            System.out.println(notification);
            notification = client.waitForNotification(15000);
            System.out.println(notification);
            state = client.getJobState(id);
            System.out.println(state);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
//        System.out.println(aJob.getHostname());
//        System.out.println(aJob.getID());
        //System.out.println(test.getMachineName());
        //System.out.println(test.getPower());
        //System.out.println(test.getState());
        //System.out.println(test.getDimensions());
        //System.out.println(test.getDestroyReason());
    }
}
