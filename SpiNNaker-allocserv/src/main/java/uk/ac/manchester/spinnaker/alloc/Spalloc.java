package uk.ac.manchester.spinnaker.alloc;

import java.util.List;
import java.util.Map;

public class Spalloc implements SpallocInterface {

	@Override
	public Map<String, Machine> getMachines() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Machine getMachine(String name) {
		return getMachines().get(name);
	}

	@Override
	public JobCollection getJobs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Job getJob(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Job createJob(String owner, List<Integer> dimensions,
			String machineName, List<String> tags) {
		// TODO Auto-generated method stub
		return null;
	}

}
