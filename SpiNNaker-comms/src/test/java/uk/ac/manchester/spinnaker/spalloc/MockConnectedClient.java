package uk.ac.manchester.spinnaker.spalloc;

import java.io.IOException;
import uk.ac.manchester.spinnaker.spalloc.messages.Command;
import uk.ac.manchester.spinnaker.spalloc.messages.CreateJobCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.GetJobMachineInfoCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.GetJobStateCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.ListJobsCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.ListMachinesCommand;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIsJobChipCommand;

/**
 *
 * @author micro
 */
public class MockConnectedClient extends SpallocClient {

    static final String LIST_JOBS_R = "["
            + "{\"job_id\":47224,"
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
                + "\"keepalivehost\":\"130.88.198.171\"},"
            + "{\"job_id\":47444,"
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
                + "\"keepalivehost\":\"130.88.198.171\"}]";

    static final String LIST_MACHINE_R = "["
        + "{\"name\":\"Spin24b-223\","
            + "\"tags\":[\"default\",\"machine-room\"],"
            + "\"width\":4,"
            + "\"height\":2,"
            + "\"dead_boards\":[],"
            + "\"dead_links\":[]},"
        + "{\"name\":\"Spin24b-225\","
            + "\"tags\":[\"default\",\"machine-room\"],"
            + "\"width\":4,"
            + "\"height\":2,"
            + "\"dead_boards\":[],"
            + "\"dead_links\":[]},"
        + "{\"name\":\"Spin24b-226\","
            + "\"tags\":[\"default\","
            + "\"machine-room\"],"
            + "\"width\":4,"
            + "\"height\":2,"
            + "\"dead_boards\":[],"
            + "\"dead_links\":[]}"
        + "]";
    
    public static final int MOCK_ID = 9999;
    
    static final String JOB_MACHINE_INFO_R = 
            "{\"connections\":[[[0,0],\"10.11.223.33\"]],"
            + "\"width\":8,"
            + "\"machine_name\":\"Spin24b-223\","
            + "\"boards\":[[0,0,2]],"
            + "\"height\":8}";
    
    static final String STATE_POWER_R = 
            "{\"state\":2,"
            + "\"power\":true,"
            + "\"keepalive\":60.0,"
            + "\"reason\":null,"
            + "\"start_time\":1.537284307847865E9,"
            + "\"keepalivehost\":\"86.82.216.229\"}";
            
    
    static final String WHERE_IS_CHIP = 
            "{\"job_chip\":[1,1],"
            + "\"job_id\":47894,"
            + "\"chip\":[5,9],"
            + "\"logical\":[0,0,2],"
            + "\"machine\":\"Spin24b-223\","
            + "\"board_chip\":[1,1],"
            + "\"physical\":[0,0,4]}";
            
    private boolean actual;
 
    public MockConnectedClient(int timeout) {
        //super("spinnaker.cs.man.ac.uk", 22244, timeout);
        super("127.0.0.0", 22244, timeout);
        actual = true;
    }
    
    @Override
    public void connect(Integer timeout) throws IOException {
        if (actual) {
            try {
                super.connect(timeout);
                actual = true;
            } catch (Exception ex) {
                actual = false;
                System.out.println("Connect fail using mock");
            }
        }
    }
    
    @Override
    protected String call(Command<?> command, Integer timeout) {
        if (actual) {
            try {
                return super.call(command, timeout);
            } catch (Exception ex) {
                actual = false;
                System.out.println("Call fail using mock");
            }
        }
        if (command.getClass() == ListJobsCommand.class) {
            return LIST_JOBS_R;
        }
        if (command.getClass() == ListMachinesCommand.class) {
            return LIST_MACHINE_R;
        }
        if (command.getClass() == CreateJobCommand.class) {
            return "" + MOCK_ID;
        }
        if (command.getClass() == GetJobMachineInfoCommand.class) {
            return JOB_MACHINE_INFO_R;
        }        
        if (command.getClass() ==GetJobStateCommand.class) {
            return STATE_POWER_R;
        }
        if (command.getClass() == WhereIsJobChipCommand.class) {
            return WHERE_IS_CHIP;
        }
        return null;
    }

    /**
     * @return the actual
     */
    public boolean isActual() {
        return actual;
    }

}
