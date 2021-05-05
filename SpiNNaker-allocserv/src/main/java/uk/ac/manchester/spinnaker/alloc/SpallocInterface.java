package uk.ac.manchester.spinnaker.alloc;

import java.util.List;
import java.util.Map;

public interface SpallocInterface {

	Map<String, Machine> getMachines();

	Machine getMachine(String name);

	JobCollection getJobs();

	Job getJob(int id);

	Job createJob(String owner, List<Integer> dimensions, String machineName,
			List<String> tags);

}
