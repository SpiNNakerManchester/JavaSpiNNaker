package uk.ac.manchester.spinnaker.alloc;

public class SpallocImpl implements SpallocAPI {

	@Override
	public VersionResponse getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MachinesResponse getMachines() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MachineResponse getMachine(String name, boolean wait) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WhereIsResponse getPhysicalPosition(String name, int x, int y,
			int z) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WhereIsResponse getLogicalPosition(String name, int cabinet,
			int frame, int board) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WhereIsResponse getMachineChipLocation(String name, int x, int y) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListJobsResponse listJobs(boolean wait) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CreateJobResponse createJob(CreateJobRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String keepAlive(int id, String req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StateResponse getState(int id, boolean wait) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeleteResponse deleteJob(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MachineResponse getMachine(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MachinePowerResponse getMachinePower(int id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MachinePowerResponse setMachinePower(int id,
			MachinePowerRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WhereIsResponse getJobChipLocation(int id, int x, int y) {
		// TODO Auto-generated method stub
		return null;
	}

}
