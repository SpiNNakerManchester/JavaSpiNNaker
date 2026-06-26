/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.IOUtils.buffer;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_POST_POWER_ON_SLEEP_TIME;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_POWER_ON_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.bmp.SerialVector.SERIAL_LENGTH;
import static uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer.FLASH_CHUNK_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;
import static uk.ac.manchester.spinnaker.transceiver.BMPCommandProcess.BMP_RETRIES;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.MustBeClosed;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.DelegatingSCPConnection;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SDPConnection;
import uk.ac.manchester.spinnaker.connections.SingletonConnectionSelector;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest;
import uk.ac.manchester.spinnaker.messages.bmp.BMPSetLED;
import uk.ac.manchester.spinnaker.messages.bmp.GetBMPVersion;
import uk.ac.manchester.spinnaker.messages.bmp.GetFPGAResetStatus;
import uk.ac.manchester.spinnaker.messages.bmp.ReadADC;
import uk.ac.manchester.spinnaker.messages.bmp.ReadCANStatus;
import uk.ac.manchester.spinnaker.messages.bmp.ReadFPGARegister;
import uk.ac.manchester.spinnaker.messages.bmp.ReadSerialFlashCRC;
import uk.ac.manchester.spinnaker.messages.bmp.ReadSerialVector;
import uk.ac.manchester.spinnaker.messages.bmp.ResetFPGA;
import uk.ac.manchester.spinnaker.messages.bmp.SetPower;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFPGARegister;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.GetChipInfo;
import uk.ac.manchester.spinnaker.utils.MappableIterable;
import uk.ac.manchester.spinnaker.utils.Ping;

/**
 * An encapsulation of various communications with the SpiNNaker board. Acts as
 * a Fa&ccedil;ade for most of the rest of this package.
 * <p>
 * The methods of this class are designed to be thread-safe; thus you can make
 * multiple calls to the same (or different) methods from multiple threads and
 * expect each call to work as if it had been called sequentially, although the
 * order of returns is not guaranteed. Note also that with multiple connections
 * to the board, using multiple threads in this way may result in an increase in
 * the overall speed of operation, since the multiple calls may be made
 * separately over the set of given connections.
 * <p>
 * For details of thread safety, see the methods annotated with
 * {@link ParallelSafe}, {@link ParallelSafeWithCare} and {@link ParallelUnsafe}
 * in {@link TransceiverInterface}. <em>Note that operations on an individual
 * BMP are <strong>always</strong> parallel-unsafe, other documentation in this
 * class notwithstanding; BMPs must only ever have one outstanding call made to
 * them as they do not handle asynchronous calls at all well due to known
 * firmware bugs.</em>
 */
public class BMPTransceiver implements BMPTransceiverInterface, RetryTracker {
	private static final Logger log = getLogger(Transceiver.class);

	private static final String BMP_NAME = "BC&MP";

	private static final Set<Integer> BMP_MAJOR_VERSIONS = Set.of(1, 2);

	private static final int CONNECTION_CHECK_RETRY_COUNT = 3;

	private static final int CONNECTION_CHECK_DELAY = 100;

	/** The BMP connections. */
	private final List<BMPConnection> bmpConnections = new ArrayList<>();

	/** Connection selectors for the BMP processes. */
	private final Map<BMPCoords,
			ConnectionSelector<BMPConnection>> bmpSelectors = new HashMap<>();

	private boolean machineOff = false;

	private long retryCount = 0L;

	private BMPCoords boundBMP = new BMPCoords(0, 0);

	/**
	 * Create a BMPTransceiver from connection data.
	 *
	 * @param bmpConnectionData
	 *            the details of the BMP connections used to boot multi-board
	 *            systems
	 * @return the created transceiver
	 * @throws IOException
	 *             if networking fails
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public static BMPTransceiver createBMPTransceiver(
			Collection<BMPConnectionData> bmpConnectionData)
			throws IOException, SpinnmanException, InterruptedException {
		var connections = new ArrayList<BMPConnection>();

		// handle BMP connections
		for (var connData : bmpConnectionData) {
			var connection = new BMPConnection(connData);
			connections.add(connection);
		}
		return new BMPTransceiver(connections);
	}

	/**
	 * Create a BMP Transceiver.
	 *
	 * @param connections
	 *            The connections to use in the transceiver. Note that the
	 *            transceiver may make additional connections. <em>This should
	 *            be modifiable (or {@code null}) if {@code scampConnections}
	 *            supplied and not empty.</em>
	 * @throws IOException
	 *             if networking fails
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@SuppressWarnings("MustBeClosed")
	public BMPTransceiver(Collection<BMPConnection> connections)
			throws IOException, SpinnmanException, InterruptedException {
		for (BMPConnection bmpc : connections) {
			bmpConnections.add(bmpc);
			bmpSelectors.put(bmpc.getCoords(),
					new SingletonConnectionSelector<>(bmpc));
		}
		checkBMPConnections();
	}

	private ConnectionSelector<BMPConnection> bmpConnection(BMPCoords bmp) {
		if (!bmpSelectors.containsKey(bmp)) {
			throw new IllegalArgumentException(
					"Unknown combination of cabinet (" + bmp.getCabinet()
							+ ") and frame (" + bmp.getFrame() + ")");
		}
		return bmpSelectors.get(bmp);
	}

	/** Check that the BMP connections are actually connected to valid BMPs. */
	private void checkBMPConnections()
			throws IOException, SpinnmanException, InterruptedException {
		/*
		 * Check that the UDP BMP conn is actually connected to a BMP via the
		 * SVER command
		 */
		for (var conn : bmpConnections) {
			// try to send a BMP SVER to check if it responds as expected
			try {
				var versionInfo = readBMPVersion(conn.getCoords(), conn.boards);
				if (!BMP_NAME.equals(versionInfo.name) || !BMP_MAJOR_VERSIONS
						.contains(versionInfo.versionNumber.majorVersion)) {
					throw new IOException(format(
							"The BMP at %s is running %s %s which is "
									+ "incompatible with this transceiver, "
									+ "required version is %s %s",
							conn.getRemoteIPAddress(), versionInfo.name,
							versionInfo.versionString, BMP_NAME,
							BMP_MAJOR_VERSIONS));
				}

				log.info("Using BMP at {} with version {} {}",
						conn.getRemoteIPAddress(), versionInfo.name,
						versionInfo.versionString);
			} catch (SocketTimeoutException e) {
				/*
				 * If it fails to respond due to timeout, maybe that the
				 * connection isn't valid.
				 */
				throw new SpinnmanException(
						format("BMP connection to %s is not responding",
								conn.getRemoteIPAddress()),
						e);
			} catch (ProcessException e) {
				log.error("Failed to speak to BMP at {}",
						conn.getRemoteIPAddress(), e);
				throw e;
			}
		}
	}

	@Override
	public void retryNeeded() {
		retryCount++;
	}

	/**
	 * Check that the given connection to the given chip works.
	 *
	 * @param connection
	 *            the connection to use when doing the check
	 * @param chip
	 *            the chip coordinates to try to talk to
	 * @return True if a valid response is received, False otherwise
	 * @throws InterruptedException
	 *             If interrupted while waiting for a response.
	 */
	@CheckReturnValue
	private boolean checkConnection(SCPConnection connection,
			HasChipLocation chip) throws InterruptedException {
		for (int r = 0; r < CONNECTION_CHECK_RETRY_COUNT; r++) {
			try {
				var chipInfo = simpleProcess(connection)
						.retrieve(new GetChipInfo(chip));
				if (chipInfo.isEthernetAvailable) {
					return true;
				}
				sleep(CONNECTION_CHECK_DELAY);
			} catch (SocketTimeoutException | ProcessException e) {
				// do nothing
			} catch (IOException e) {
				break;
			}
		}
		return false;
	}

	/**
	 * A neater way of getting a process for running simple SCP requests against
	 * a specific SDP connection. Note that the connection is just SDP, not
	 * guaranteed to be SCP; that matters because it is used to set up
	 * cross-firewall/NAT routing.
	 *
	 * @param connector
	 *            The specific connector to talk to the board along.
	 * @return The SCP runner process
	 * @throws IOException
	 *             If anything fails (unexpected).
	 */
	private TxrxProcess simpleProcess(SDPConnection connector)
			throws IOException {
		// Avoid delegation of the connection if not needed
		if (connector instanceof SCPConnection) {
			return new TxrxProcess(new SingletonConnectionSelector<>(
					(SCPConnection) connector), this);
		}
		return new TxrxProcess(new SingletonConnectionSelector<>(
				new DelegatingSCPConnection(connector)), this);
	}

	/**
	 * Call a BMP operation on a BMP.
	 *
	 * @param <T>
	 *            The type of the response.
	 * @param bmp
	 *            The BMP to call.
	 * @param request
	 *            The request to make.
	 * @return The response from the request.
	 * @throws IOException
	 *             If networking fails.
	 * @throws ProcessException
	 *             If the BMP rejects the message.
	 * @throws InterruptedException
	 *             If the thread is interrupted.
	 */
	private <T extends BMPRequest.BMPResponse> T call(BMPCoords bmp,
			BMPRequest<T> request)
			throws IOException, ProcessException, InterruptedException {
		return new BMPCommandProcess(bmpConnection(bmp), this).execute(request);
	}

	/**
	 * Call a BMP operation on a BMP.
	 *
	 * @param <T>
	 *            The type of the response.
	 * @param bmp
	 *            The BMP to call.
	 * @param timeout
	 *            The timeout, in milliseconds.
	 * @param retries
	 *            The number of times to retry the call on a transient failure.
	 * @param request
	 *            The request to make.
	 * @return The response from the request.
	 * @throws IOException
	 *             If networking fails.
	 * @throws ProcessException
	 *             If the BMP rejects the message.
	 * @throws InterruptedException
	 *             If the thread is interrupted.
	 */
	private <T extends BMPRequest.BMPResponse> T call(BMPCoords bmp,
			int timeout, int retries, BMPRequest<T> request)
					throws IOException, ProcessException, InterruptedException {
		return new BMPCommandProcess(bmpConnection(bmp), timeout, this)
				.execute(request, retries);
	}

	/**
	 * Call a BMP operation on a BMP and return the parsed payload of the
	 * response.
	 *
	 * @param <T>
	 *            The type of the parsed payload.
	 * @param <R>
	 *            The type of the response.
	 * @param bmp
	 *            The BMP to call.
	 * @param request
	 *            The request to make.
	 * @return The response from the request.
	 * @throws IOException
	 *             If networking fails.
	 * @throws ProcessException
	 *             If the BMP rejects the message.
	 * @throws InterruptedException
	 *             If the thread is interrupted.
	 */
	private <T, R extends BMPRequest.PayloadedResponse<T>> T get(BMPCoords bmp,
			BMPRequest<R> request)
			throws IOException, ProcessException, InterruptedException {
		return new BMPCommandProcess(bmpConnection(bmp), this).call(request);
	}

	/**
	 * Call a BMP operation on a BMP and return the parsed payload of the
	 * response.
	 *
	 * @param <T>
	 *            The type of the parsed payload.
	 * @param <R>
	 *            The type of the response.
	 * @param bmp
	 *            The BMP to call.
	 * @param timeout
	 *            The timeout, in milliseconds.
	 * @param retries
	 *            The number of times to retry the call on a transient failure.
	 * @param request
	 *            The request to make.
	 * @return The response from the request.
	 * @throws IOException
	 *             If networking fails.
	 * @throws ProcessException
	 *             If the BMP rejects the message.
	 * @throws InterruptedException
	 *             If the thread is interrupted.
	 */
	private <T, R extends BMPRequest.PayloadedResponse<T>> T get(BMPCoords bmp,
			int timeout, int retries, BMPRequest<R> request)
			throws IOException, ProcessException, InterruptedException {
		return new BMPCommandProcess(bmpConnection(bmp), timeout, this)
				.execute(request, retries).get();
	}

	@Override
	@ParallelUnsafe
	public void powerOnMachine()
			throws InterruptedException, IOException, ProcessException {
		if (bmpConnections.isEmpty()) {
			log.warn("No BMP connections, so can't power on");
		}
		for (var connection : bmpConnections) {
			power(POWER_ON, connection.getCoords(), connection.boards);
		}
	}

	@Override
	@ParallelUnsafe
	public void powerOffMachine()
			throws InterruptedException, IOException, ProcessException {
		if (bmpConnections.isEmpty()) {
			log.warn("No BMP connections, so can't power off");
		}
		for (var connection : bmpConnections) {
			power(POWER_OFF, connection.getCoords(), connection.boards);
		}
	}

	@Override
	@ParallelUnsafe
	public void power(PowerCommand powerCommand, BMPCoords bmp,
			Collection<BMPBoard> boards)
			throws InterruptedException, IOException, ProcessException {
		int timeout = (int) (MSEC_PER_SEC
				* (powerCommand == POWER_ON ? BMP_POWER_ON_TIMEOUT
						: BMP_TIMEOUT));
		requireNonNull(call(bmp, timeout, 0,
				new SetPower(powerCommand, boards, 0.0)));
		machineOff = powerCommand == POWER_OFF;

		// Sleep for 5 seconds if the machine has just been powered on
		if (!machineOff) {
			sleep((int) (BMP_POST_POWER_ON_SLEEP_TIME * MSEC_PER_SEC));
		}
	}

	@Override
	@ParallelUnsafe
	public void setLED(Collection<Integer> leds, LEDAction action,
			BMPCoords bmp, Collection<BMPBoard> board)
			throws IOException, ProcessException, InterruptedException {
		call(bmp, new BMPSetLED(leds, action, board));
	}

	@Override
	@CheckReturnValue
	@ParallelUnsafe
	public int readFPGARegister(FPGA fpga, MemoryLocation register,
			BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException, InterruptedException {
		return get(bmp, new ReadFPGARegister(fpga, register, board));
	}

	@Override
	@ParallelUnsafe
	public void writeFPGARegister(FPGA fpga, MemoryLocation register, int value,
			BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException, InterruptedException {
		call(bmp, new WriteFPGARegister(fpga, register, value, board));
	}

	@Override
	@CheckReturnValue
	@ParallelUnsafe
	public ADCInfo readADCData(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException, InterruptedException {
		return get(bmp, new ReadADC(board));
	}

	@Override
	@CheckReturnValue
	@ParallelUnsafe
	public VersionInfo readBMPVersion(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException, InterruptedException {
		return get(bmp, new GetBMPVersion(board));
	}

	@Override
	@CheckReturnValue
	@ParallelSafeWithCare
	public ByteBuffer readBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length)
			throws ProcessException, IOException, InterruptedException {
		return new BMPReadMemoryProcess(bmpConnection(bmp), this).read(board,
				baseAddress, length);
	}

	@Override
	public void writeBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		new BMPWriteMemoryProcess(bmpConnection(bmp), this).writeMemory(board,
				baseAddress, data);
	}

	@Override
	public void writeBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, File file)
			throws IOException, ProcessException, InterruptedException {
		var wmp = new BMPWriteMemoryProcess(bmpConnection(bmp), this);
		try (var f = buffer(new FileInputStream(file))) {
			// The file had better fit...
			wmp.writeMemory(board, baseAddress, f, (int) file.length());
		}
	}

	@Override
	public MemoryLocation getSerialFlashBuffer(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException, InterruptedException {
		return get(bmp, new ReadSerialVector(board)).getFlashBuffer();
	}

	@Override
	public String readBoardSerialNumber(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException, InterruptedException {
		var serialNumber = new int[SERIAL_LENGTH];
		get(bmp, new ReadSerialVector(board)).getSerialNumber()
				.get(serialNumber);
		return format("%08x-%08x-%08x-%08x",
				stream(serialNumber).mapToObj(Integer::valueOf).toArray());
	}

	@Override
	@CheckReturnValue
	public ByteBuffer readSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException, InterruptedException {
		return new BMPReadSerialFlashProcess(bmpConnection(bmp), this)
				.read(board, baseAddress, length);
	}

	// CRC calculations of megabytes can take a bit
	private static final int CRC_TIMEOUT = 2000;

	@Override
	@CheckReturnValue
	public int readSerialFlashCRC(BMPCoords bmp, BMPBoard board,
			MemoryLocation address, int length)
			throws IOException, ProcessException, InterruptedException {
		return get(bmp, CRC_TIMEOUT, BMP_RETRIES /* =default */,
				new ReadSerialFlashCRC(board, address, length));
	}

	@Override
	public void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data)
			throws ProcessException, IOException, InterruptedException {
		new BMPWriteSerialFlashProcess(bmpConnection(bmp), this).write(board,
				baseAddress, data);
	}

	@Override
	public void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int size, InputStream stream)
			throws ProcessException, IOException, InterruptedException {
		new BMPWriteSerialFlashProcess(bmpConnection(bmp), this).write(board,
				baseAddress, stream, size);
	}

	@Override
	public void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, File file)
			throws ProcessException, IOException, InterruptedException {
		try (var f = buffer(new FileInputStream(file))) {
			// The file had better fit...
			new BMPWriteSerialFlashProcess(bmpConnection(bmp), this)
					.write(board, baseAddress, f, (int) file.length());
		}
	}

	@Override
	public void writeBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation address)
			throws IOException, ProcessException, InterruptedException {
		call(bmp, new WriteFlashBuffer(board, address, true));
	}

	@Override
	public void writeFlash(@Valid BMPCoords bmp, @Valid BMPBoard board,
			@NotNull MemoryLocation baseAddress, @NotNull ByteBuffer data)
			throws ProcessException, IOException, InterruptedException {
		if (!data.hasRemaining()) {
			// Zero length write?
			log.warn("zero length write to flash ignored");
			return;
		}

		var serialVector = get(bmp, new ReadSerialVector(board));
		var workingBuffer = serialVector.getFlashBuffer();
		var targetAddr = baseAddress;
		for (var buf : sliceUp(data, FLASH_CHUNK_SIZE)) {
			writeBMPMemory(bmp, board, workingBuffer, buf);
			call(bmp, new WriteFlashBuffer(board, targetAddr, true));
			targetAddr = targetAddr.add(FLASH_CHUNK_SIZE);
		}
	}

	@Override
	@ParallelSafe
	public boolean getResetStatus(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException, InterruptedException {
		return get(bmp, new GetFPGAResetStatus(board));
	}

	@Override
	@ParallelSafe
	public void resetFPGA(BMPCoords bmp, BMPBoard board,
			FPGAResetType resetType)
			throws IOException, ProcessException, InterruptedException {
		call(bmp, new ResetFPGA(board, resetType));
	}

	@Override
	@CheckReturnValue
	public MappableIterable<BMPBoard> availableBoards(BMPCoords bmp)
			throws IOException, ProcessException, InterruptedException {
		return get(bmp, new ReadCANStatus());
	}

	/**
	 * Close the transceiver and any threads that are running.
	 *
	 * @throws IOException
	 *             If anything goes wrong
	 */
	@Override
	public void close() throws IOException {
		try {
			close(false);
		} catch (InterruptedException e) {
			log.warn("unexpected interruption", e);
		}
	}

	/**
	 * Close the transceiver and any threads that are running.
	 *
	 * @param closeOriginalConnections
	 *            If True, the original connections passed to the transceiver in
	 *            the constructor are also closed. If False, only newly
	 *            discovered connections are closed.
	 * @param powerOffMachine
	 *            if true, the machine is sent a power down command via its BMP
	 *            (if it has one)
	 * @throws IOException
	 *             If anything goes wrong with networking
	 * @throws InterruptedException
	 *             If interrupted while waiting for the machine to power down
	 *             (only if that is requested).
	 */
	public void close(boolean powerOffMachine)
			throws IOException, InterruptedException {
		if (powerOffMachine && !bmpConnections.isEmpty()) {
			try {
				powerOffMachine();
			} catch (ProcessException e) {
				log.warn("failed to power off machine", e);
			}
		}

		for (var connection : bmpConnections) {
			connection.close();
		}

		log.info("total retries used: {}", retryCount);
	}

	/** @return The connection selectors used for BMP connections. */
	public Map<BMPCoords,
			ConnectionSelector<BMPConnection>> getBMPConnection() {
		return bmpSelectors;
	}

	@Override
	public void bind(BMPCoords bmp) {
		boundBMP = bmp;
	}

	@Override
	public BMPCoords getBoundBMP() {
		return boundBMP;
	}

	@Override
	public int pingBoard(String address) {
		return Ping.ping(address);
	}
}
