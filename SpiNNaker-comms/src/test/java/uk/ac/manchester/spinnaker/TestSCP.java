package uk.ac.manchester.spinnaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.machine.Processor;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader;
import uk.ac.manchester.spinnaker.messages.sdp.SDPHeader.Flag;
import uk.ac.manchester.spinnaker.spalloc.SpallocJob;
import uk.ac.manchester.spinnaker.transceiver.RetryTracker;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.MultiConnectionProcess;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;
import uk.ac.manchester.spinnaker.utils.progress.ProgressBar;

public class TestSCP {

    public static void main(String[] args) throws Exception {
        URL execUri = ClassLoader.getSystemResource("scp_test.aplx");
        File executable = new File(execUri.getFile());
        if (!executable.exists()) {
            throw new FileNotFoundException(executable.getAbsolutePath());
        }
        Map<String, Object> kwargs = new HashMap<String, Object>();
        kwargs.put("owner", "Andrew.Rowley@manchester.ac.uk");
        try (SpallocJob job = new SpallocJob("spinnaker.cs.man.ac.uk", Arrays.asList(1), kwargs)) {
            job.waitUntilReady(null);
            InetAddress hostname = InetAddress.getByName(job.getHostname());
            MachineVersion version = MachineVersion.FIVE;
            AppID appID = new AppID(18);
            try (Transceiver txrx = new Transceiver(hostname, version)) {
                txrx.ensureBoardIsReady();

                System.err.println("Loading Binary");
                CoreSubsets coreSubsets = new CoreSubsets();
                Machine machine = txrx.getMachineDetails();
                for (ChipLocation chip : machine.chipCoordinates()) {
                    for (Processor p : machine.getChipAt(chip).userProcessors()) {
                        coreSubsets.addCore(chip, p.processorId);
                    }
                }
                txrx.executeFlood(coreSubsets, executable, appID);

                System.err.println("Running SCP test");
                TestProcess process = new TestProcess(txrx.getScampConnectionSelector());
                process.test(coreSubsets, 1000, 3);
            }
        }
    }

}


final class TestProcess extends MultiConnectionProcess<SCPConnection> {

    private ProgressBar progress = null;

    public TestProcess(ConnectionSelector<SCPConnection> connectionSelector) {
        super(connectionSelector, 10, 5000, 8, 7, null);
    }

    public void handleResponse(CheckOKResponse response) {
        progress.update();
    }

    private void checkError() {
        try {
            checkForError();
        } catch (ProcessException e) {
            e.printStackTrace();
        }
    }

    public void test(CoreSubsets subsets, int repeats, int port) throws IOException{
        progress = new ProgressBar(repeats * subsets.size(), "Sending test messages");
        for (int i = 0; i < repeats; i++) {
            for (HasCoreLocation location : subsets) {
                sendRequest(new TestMessage(location, port),
                        response -> handleResponse(response));
                checkError();
            }
        }
        finish();
        progress.close();
        checkError();
    }
}


final class TestMessage extends SCPRequest<CheckOKResponse> {

    public TestMessage(HasCoreLocation location, int port) {
        super(new SDPHeader(Flag.REPLY_EXPECTED, location, port),
                SCPCommand.CMD_VER, 0, 0, 0, NO_DATA);
    }

    @Override
    public CheckOKResponse getSCPResponse(ByteBuffer buffer) throws Exception {
        return new CheckOKResponse("TEST", SCPCommand.CMD_VER, buffer);
    }
}
