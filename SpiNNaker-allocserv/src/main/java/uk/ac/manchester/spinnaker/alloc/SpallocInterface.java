package uk.ac.manchester.spinnaker.alloc;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface SpallocInterface {

	Map<String, Machine> getMachines() throws SQLException;

	Machine getMachine(String name) throws SQLException;

	JobCollection getJobs() throws SQLException;

	Job getJob(int id) throws SQLException;

	Job createJob(String owner, List<Integer> dimensions, String machineName,
			List<String> tags, Integer maxDeadBoards) throws SQLException;

}
