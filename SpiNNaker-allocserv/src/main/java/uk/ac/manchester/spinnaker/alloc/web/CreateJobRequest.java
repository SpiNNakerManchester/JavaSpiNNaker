package uk.ac.manchester.spinnaker.alloc.web;

import java.util.List;

public class CreateJobRequest {
	public String owner;

	public List<Integer> dimensions;

	public String machineName;

	public List<String> tags;
}
