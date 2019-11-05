package uk.ac.manchester.spinnaker;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SDPConnection;
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
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.MultiConnectionProcess;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;
import uk.ac.manchester.spinnaker.utils.progress.ProgressBar;

public class TestSCP {

    public static void main(String[] args) throws Exception {
        File executable = new File("src/test/resources/scp_test.aplx");
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

                System.err.println("Sending Data");
                SDPConnection conn = new SDPConnection(
                        new ChipLocation(0, 0), hostname, 17894);
                byte[][] inputData = new byte[1000][];
                Receiver receiver = new Receiver(1000, conn);
                receiver.start();
                for (int i = 0; i < 1000; i++) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream(1464);
                    DataOutputStream data = new DataOutputStream(bytes);
                    data.writeInt(i);
                    Random r = new Random();
                    for (int j = 0; j < 1460; j++) {
                       data.writeByte(r.nextInt(255) - 128);
                    }
                    inputData[i] = bytes.toByteArray();
                    conn.send(inputData[i]);
                }

                System.err.println("Waiting for receive to finish");
                while (!receiver.isError()) {
                    Thread.sleep(500);
                }

                int lastReceived = -1;
                for (int i = 0; i < inputData.length; i++) {
                    if (receiver.received[i] != null) {
                        if (lastReceived + 1 != i) {
                            System.err.println("Missing " + lastReceived + " - " + (i - 1));
                        }
                        lastReceived = i;
                        System.err.println("Received " + i);
                        if (!Arrays.equals(inputData[i], receiver.received[i])) {
                            System.err.println("    Not equal!");
                            System.err.println("    " + inputData[i]);
                            System.err.println("    " + receiver.received[i]);
                        }
                    }
                }
                if (lastReceived + 1 != inputData.length) {
                    System.err.println("Missing " + lastReceived + " - " + (inputData.length - 1));
                }

                System.err.println(txrx.getScampVersion());
                System.err.println(txrx.getScampVersion(new ChipLocation(1, 1)));
            }
        }
    }

}


final class Receiver extends Thread {
    public final byte[][] received;

    private final SDPConnection connection;

    private boolean error = false;

    public Receiver(int size, SDPConnection connection) {
        received = new byte[size][];
        this.connection = connection;
    }

    public void run() {
        while (!error) {
            try {
                ByteBuffer data = connection.receive(2000);
                int index = data.getInt();
                received[index] = new byte[data.capacity()];
                data.rewind();
                data.get(received[index]);
            } catch (Exception e) {
                e.printStackTrace();
                error = true;
            }
        }
    }

    public boolean isError() {
        return error;
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
