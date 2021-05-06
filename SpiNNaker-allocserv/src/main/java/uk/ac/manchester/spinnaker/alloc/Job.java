package uk.ac.manchester.spinnaker.alloc;

import java.sql.Connection;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.State;

public class Job {

	int id;
	Integer width;
	Integer height;
	int state;
	Integer root;
	long keepaliveTime;
	String keepaliveHost;

	Job(Connection conn) {
		// TODO Auto-generated constructor stub
	}

	public void access(String keepaliveAddress) {
		// TODO Auto-generated method stub

	}

	public SubMachine getMachine() {
		// TODO Auto-generated method stub
		return null;
	}

	public void destroy(String reason) {
		// TODO Auto-generated method stub

	}

	public void waitForChange() {
		// TODO Auto-generated method stub

	}

	public int getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	public State getState() {
		// TODO Auto-generated method stub
		return null;
	}

	public Float getStartTime() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getReason() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getKeepaliveHost() {
		// TODO Auto-generated method stub
		return null;
	}

	public BoardLocation whereIs(int x, int y) {
		// TODO Auto-generated method stub
		return null;
	}

	public ChipLocation getRootChip() {
		// TODO Auto-generated method stub
		return null;
	}

}
