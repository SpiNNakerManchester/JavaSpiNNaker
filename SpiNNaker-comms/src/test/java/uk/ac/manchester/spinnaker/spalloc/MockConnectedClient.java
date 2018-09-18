package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;
import uk.ac.manchester.spinnaker.spalloc.messages.Command;
import uk.ac.manchester.spinnaker.spalloc.messages.ListJobsCommand;

/**
 *
 * @author micro
 */
public class MockConnectedClient extends SpallocClient {

    static final String LIST_JOBS_C = (new ListJobsCommand()).getCommand();
    static final String LIST_JOBS_R = "[" + (
    		("{\"job_id\":47224,"
    				+ "\"owner\":\"someone@manchester.ac.uk\","
    				+ "\"start_time\":1.536925243666607E9,"
    				+ "\"keepalive\":60.0,"
    				+ "\"state\":3,"
    				+ "\"power\":true,"
    				+ "\"args\":[1],"
    				+ "\"kwargs\":{\"tags\":null,"
    				+ "\"max_dead_boards\":0,"
    				+ "\"machine\":null,"
    				+ "\"min_ratio\":0.333,"
    				+ "\"max_dead_links\":null,"
    				+ "\"require_torus\":false},"
    				+ "\"allocated_machine_name\":\"Spin24b-223\","
    				+ "\"boards\":[[1,1,2]],"
    				+ "\"keepalivehost\":\"130.88.198.171\"}") + "," +
    		("{\"job_id\":47444,"
    				+ "\"owner\":\"another.person@manchester.ac.uk\","
    				+ "\"start_time\":1.537098968439959E9,"
    				+ "\"keepalive\":60.0,"
    				+ "\"state\":3,"
    				+ "\"power\":true,"
    				+ "\"args\":[1],"
    				+ "\"kwargs\":{\"tags\":null,"
    				+ "\"max_dead_boards\":0,"
    				+ "\"machine\":null,"
    				+ "\"min_ratio\":0.333,"
    				+ "\"max_dead_links\":null,"
    				+ "\"require_torus\":false},"
    				+ "\"allocated_machine_name\":\"Spin24b-223\","
    				+ "\"boards\":[[2,0,2]],"
    				+ "\"keepalivehost\":\"130.88.198.171\"}"))
    		+ "]";

    public MockConnectedClient() {
        super("127.0.0.0", 8080, 2000);
    }

    @Override
	public void connect(Integer timeout) throws IOException {
        //DO nothing
    }

    @Override
	protected String call(Command<?> command, Integer timeout) {
        if (command.getClass() == ListJobsCommand.class) {
            return LIST_JOBS_R;
        }
        return null;
    }
}
