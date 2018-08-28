package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Thread.sleep;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_USER_0_START_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_USER_1_START_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_USER_2_START_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.NO_ROUTER_DIAGNOSTIC_FILTERS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.READY;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.RUN_TIME_EXCEPTION;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.WATCHDOG;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;
import static uk.ac.manchester.spinnaker.messages.model.Signal.START;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.sdram_heap_address;
import static uk.ac.manchester.spinnaker.processes.FillProcess.DataType.WORD;
import static uk.ac.manchester.spinnaker.transceiver.Utils.getVcpuAddress;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SDPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter;
import uk.ac.manchester.spinnaker.messages.model.ExecutableTargets;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.IOBuffer;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.model.Signal;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.processes.FillProcess.DataType;
import uk.ac.manchester.spinnaker.processes.Process.Exception;

/**
 * The interface supported by the {@link Transceiver}. Emulates a lot of default
 * handling and variant-type handling by Python.
 *
 * @author Donal Fellows
 */
public interface TransceiverInterface {
	/**
	 * Delay between starting a program on a core and checking to see if the
	 * core is ready for operational use. In milliseconds.
	 */
	int LAUNCH_DELAY = 500;
	/**
	 * Coordinate of a <i>default</i> destination.
	 */
	int DEFAULT_DESTINATION_COORDINATE = 255;
	/**
	 * The default destination chip.
	 */
	ChipLocation DEFAULT_DESTINATION = new ChipLocation(
			DEFAULT_DESTINATION_COORDINATE, DEFAULT_DESTINATION_COORDINATE);
	/**
	 * A marker to indicate that no timeout applies.
	 */
	Integer TIMEOUT_DISABLED = null;
	/**
	 * How often to poll by default.
	 */
	int DEFAULT_POLL_INTERVAL = 100;
	/**
	 * The set of states that indicate a core in a failure state.
	 */
	Set<CPUState> DEFAULT_ERROR_STATES = unmodifiableSet(
			new HashSet<>(asList(RUN_TIME_EXCEPTION, WATCHDOG)));
	/**
	 * What proportion of checks are to be expensive full checks.
	 */
	int DEFAULT_CHECK_INTERVAL = 100;
	/** How many times to try booting a board. */
	int BOARD_BOOT_RETRIES = 5;

	/**
	 * @return The connection selector to use for SCP messages.
	 */
	ConnectionSelector<SCPConnection> getScampConnectionSelector();

	/**
	 * Sends an SCP message, without expecting a response.
	 *
	 * @param message
	 *            The message to send
	 * @param connection
	 *            The connection to use (omit to pick a random one)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 */
	void sendSCPMessage(SCPRequest<?> message, SCPConnection connection)
			throws IOException;

	/**
	 * Sends an SDP message using one of the connections.
	 *
	 * @param message
	 *            The message to send
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 */
	default void sendSDPMessage(SDPMessage message) throws IOException {
		sendSDPMessage(message, null);
	}

	/**
	 * Sends an SDP message using one of the connections.
	 *
	 * @param message
	 *            The message to send
	 * @param connection
	 *            An optional connection to use
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 */
	void sendSDPMessage(SDPMessage message, SDPConnection connection)
			throws IOException;

	/**
	 * Get the maximum chip x-coordinate and maximum chip y-coordinate of the
	 * chips in the machine.
	 *
	 * @return The dimensions of the machine
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	MachineDimensions getMachineDimensions() throws IOException, Exception;

	/**
	 * Get the details of the machine made up of chips on a board and how they
	 * are connected to each other.
	 *
	 * @return A machine description
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	Machine getMachineDetails() throws IOException, Exception;

	/**
	 * Determines if the board can be contacted.
	 *
	 * @return True if the board can be contacted, False otherwise
	 */
	default boolean isConnected() {
		return isConnected(null);
	}

	/**
	 * Determines if the board can be contacted.
	 *
	 * @param connection
	 *            The connection which is to be tested. If <tt>null</tt>, all
	 *            connections will be tested, and the board will be considered
	 *            to be connected if any one connection works.
	 * @return True if the board can be contacted, False otherwise
	 */
	boolean isConnected(Connection connection);

	/**
	 * Get the version of SCAMP which is running on the board.
	 *
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default VersionInfo getScampVersion() throws IOException, Exception {
		return getScampVersion(DEFAULT_DESTINATION,
				getScampConnectionSelector());
	}

	/**
	 * Get the version of SCAMP which is running on the board.
	 *
	 * @param connectionSelector
	 *            the connection to send the SCAMP version or none (if none then
	 *            a random SCAMP connection is used).
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default VersionInfo getScampVersion(
			ConnectionSelector<SCPConnection> connectionSelector)
			throws IOException, Exception {
		return getScampVersion(DEFAULT_DESTINATION, connectionSelector);
	}

	/**
	 * Get the version of SCAMP which is running on the board.
	 *
	 * @param chip
	 *            the coordinates of the chip to query for SCAMP version
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default VersionInfo getScampVersion(HasChipLocation chip)
			throws IOException, Exception {
		return getScampVersion(chip, getScampConnectionSelector());
	}

	/**
	 * Get the version of SCAMP which is running on the board.
	 *
	 * @param connectionSelector
	 *            the connection to send the SCAMP version or none (if none then
	 *            a random SCAMP connection is used).
	 * @param chip
	 *            the coordinates of the chip to query for SCAMP version
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	VersionInfo getScampVersion(HasChipLocation chip,
			ConnectionSelector<SCPConnection> connectionSelector)
			throws IOException, Exception;

	/**
	 * Attempt to boot the board. No check is performed to see if the board is
	 * already booted.
	 *
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void bootBoard() throws InterruptedException, IOException {
		bootBoard(null);
	}

	/**
	 * Attempt to boot the board. No check is performed to see if the board is
	 * already booted.
	 *
	 * @param extraBootValues
	 *            extra values to set during boot
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	void bootBoard(Map<SystemVariableDefinition, Object> extraBootValues)
			throws InterruptedException, IOException;

	/**
	 * Ensure that the board is ready to interact with this version of the
	 * transceiver. Boots the board if not already booted and verifies that the
	 * version of SCAMP running is compatible with this transceiver.
	 *
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default VersionInfo ensureBoardIsReady()
			throws IOException, Exception, InterruptedException {
		return ensureBoardIsReady(BOARD_BOOT_RETRIES, null);
	}

	/**
	 * Ensure that the board is ready to interact with this version of the
	 * transceiver. Boots the board if not already booted and verifies that the
	 * version of SCAMP running is compatible with this transceiver.
	 *
	 * @param extraBootValues
	 *            Any additional values to set during boot
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default VersionInfo ensureBoardIsReady(
			Map<SystemVariableDefinition, Object> extraBootValues)
			throws IOException, Exception, InterruptedException {
		return ensureBoardIsReady(BOARD_BOOT_RETRIES, extraBootValues);
	}

	/**
	 * Ensure that the board is ready to interact with this version of the
	 * transceiver. Boots the board if not already booted and verifies that the
	 * version of SCAMP running is compatible with this transceiver.
	 *
	 * @param numRetries
	 *            The number of times to retry booting
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default VersionInfo ensureBoardIsReady(int numRetries)
			throws IOException, Exception, InterruptedException {
		return ensureBoardIsReady(numRetries, null);
	}

	/**
	 * Ensure that the board is ready to interact with this version of the
	 * transceiver. Boots the board if not already booted and verifies that the
	 * version of SCAMP running is compatible with this transceiver.
	 *
	 * @param numRetries
	 *            The number of times to retry booting
	 * @param extraBootValues
	 *            Any additional values to set during boot
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	VersionInfo ensureBoardIsReady(int numRetries,
			Map<SystemVariableDefinition, Object> extraBootValues)
			throws IOException, Exception, InterruptedException;

	/**
	 * Get information about the processors on the board.
	 *
	 * @return An iterable of the CPU information for all cores.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default Iterable<CPUInfo> getCPUInformation()
			throws IOException, Exception {
		return getCPUInformation((CoreSubsets) null);
	}

	/**
	 * Get information about a specific processor on the board.
	 *
	 * @param core
	 *            The coordinates of the core to get the information about
	 * @return The CPU information for the selected core
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default CPUInfo getCPUInformation(HasCoreLocation core)
			throws IOException, Exception {
		CoreSubsets coreSubsets = new CoreSubsets();
		coreSubsets.addCore(core.asCoreLocation());
		return getCPUInformation(coreSubsets).iterator().next();
	}

	/**
	 * Get information about some processors on the board.
	 *
	 * @param coreSubsets
	 *            A set of chips and cores from which to get the information. If
	 *            <tt>null</tt>, the information from all of the cores on all of
	 *            the chips on the board are obtained.
	 * @return An iterable of the CPU information for the selected cores, or all
	 *         cores if <tt>coreSubsets</tt> is <tt>null</tt>.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	Iterable<CPUInfo> getCPUInformation(CoreSubsets coreSubsets)
			throws IOException, Exception;

	/**
	 * Get the address of user<sub>0</sub> for a given processor on the board.
	 *
	 * @param core
	 *            the coordinates of the core to get the user<sub>0</sub>
	 *            address from
	 * @return The address for user<sub>0</sub> register for this processor
	 */
	default int getUser0RegisterAddress(HasCoreLocation core) {
		return getVcpuAddress(core) + CPU_USER_0_START_ADDRESS;
	}

	/**
	 * Get the address of user<sub>1</sub> for a given processor on the board.
	 *
	 * @param core
	 *            the coordinates of the core to get the user<sub>1</sub>
	 *            address from
	 * @return The address for user<sub>1</sub> register for this processor
	 */
	default int getUser1RegisterAddress(HasCoreLocation core) {
		return getVcpuAddress(core) + CPU_USER_1_START_ADDRESS;
	}

	/**
	 * Get the address of user<sub>2</sub> for a given processor on the board.
	 *
	 * @param core
	 *            the coordinates of the core to get the user<sub>2</sub>
	 *            address from
	 * @return The address for user<sub>2</sub> register for this processor
	 */
	default int getUser2RegisterAddress(HasCoreLocation core) {
		return getVcpuAddress(core) + CPU_USER_2_START_ADDRESS;
	}

	/**
	 * Get the contents of the IOBUF buffer for all processors.
	 *
	 * @return An iterable of the buffers, order undetermined.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default Iterable<IOBuffer> getIobuf() throws IOException, Exception {
		return getIobuf((CoreSubsets) null);
	}

	/**
	 * Get the contents of IOBUF for a given core.
	 *
	 * @param core
	 *            The coordinates of the processor to get the IOBUF for
	 * @return An IOBUF buffer
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default IOBuffer getIobuf(HasCoreLocation core)
			throws IOException, Exception {
		CoreSubsets coreSubsets = new CoreSubsets();
		coreSubsets.addCore(core.asCoreLocation());
		return getIobuf(coreSubsets).iterator().next();
	}

	/**
	 * Get the contents of the IOBUF buffer for a collection of processors.
	 *
	 * @param coreSubsets
	 *            A set of chips and cores from which to get the buffers.
	 * @return An iterable of the buffers, which may not be in the order of
	 *         <tt>coreSubsets</tt>
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	Iterable<IOBuffer> getIobuf(CoreSubsets coreSubsets)
			throws IOException, Exception;

	/**
	 * Set the value of the watch dog timer on a specific chip.
	 *
	 * @param chip
	 *            coordinates of the chip to write new watchdog parameter to
	 * @param watchdog
	 *            value to set the timer count to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void setWatchDogTimeoutOnChip(HasChipLocation chip, int watchdog)
			throws IOException, Exception;

	/**
	 * Enable or disable the watch dog timer on a specific chip.
	 *
	 * @param chip
	 *            coordinates of the chip to write new watchdog parameter to
	 * @param watchdog
	 *            whether to enable (True) or disable (False) the watchdog timer
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void enableWatchDogTimerOnChip(HasChipLocation chip, boolean watchdog)
			throws IOException, Exception;

	/**
	 * Set the value of the watch dog timer.
	 *
	 * @param watchdog
	 *            value to set the timer count to.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void setWatchDogTimeout(int watchdog)
			throws IOException, Exception {
		for (ChipLocation chip : getMachineDetails().chipCoordinates()) {
			setWatchDogTimeoutOnChip(chip, watchdog);
		}
	}

	/**
	 * Enable or disable the watch dog timer.
	 *
	 * @param watchdog
	 *            whether to enable (True) or disable (False) the watch dog
	 *            timer
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void enableWatchDogTimer(boolean watchdog)
			throws IOException, Exception {
		for (ChipLocation chip : getMachineDetails().chipCoordinates()) {
			enableWatchDogTimerOnChip(chip, watchdog);
		}
	}

	/**
	 * Get a count of the number of cores which have a given state.
	 *
	 * @param appID
	 *            The ID of the application from which to get the count.
	 * @param state
	 *            The state count to get
	 * @return A count of the cores with the given status
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	int getCoreStateCount(int appID, CPUState state)
			throws IOException, Exception;

	/**
	 * Start an executable running on a single core.
	 *
	 * @param core
	 *            The coordinates of the core on which to run the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param numBytes
	 *            The number of bytes to read from the input stream.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void execute(HasCoreLocation core, InputStream executable,
			int numBytes, int appID)
			throws IOException, Exception, InterruptedException {
		execute(core, singleton(core.getP()), executable, numBytes, appID);
	}

	/**
	 * Start an executable running on a single chip.
	 *
	 * @param chip
	 *            The coordinates of the chip on which to run the executable
	 * @param processors
	 *            The cores on the chip on which to run the application
	 * @param executable
	 *            The data that is to be executed.
	 * @param numBytes
	 *            The number of bytes to read from the input stream.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void execute(HasChipLocation chip, Collection<Integer> processors,
			InputStream executable, int numBytes, int appID)
			throws IOException, Exception, InterruptedException {
		execute(chip, processors, executable, numBytes, appID, false);
	}

	/**
	 * Start an executable running on a single core.
	 *
	 * @param core
	 *            The coordinates of the core on which to run the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param numBytes
	 *            The number of bytes to read from the input stream.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @param wait
	 *            True if the binary should enter a "wait" state on loading
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void execute(HasCoreLocation core, InputStream executable,
			int numBytes, int appID, boolean wait)
			throws IOException, Exception, InterruptedException {
		execute(core, singleton(core.getP()), executable, numBytes, appID,
				wait);
	}

	/**
	 * Start an executable running on a single chip.
	 *
	 * @param chip
	 *            The coordinates of the chip on which to run the executable
	 * @param processors
	 *            The cores on the chip on which to run the application
	 * @param executable
	 *            The data that is to be executed.
	 * @param numBytes
	 *            The number of bytes to read from the input stream.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @param wait
	 *            True if the binary should enter a "wait" state on loading
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	void execute(HasChipLocation chip, Collection<Integer> processors,
			InputStream executable, int numBytes, int appID, boolean wait)
			throws IOException, Exception, InterruptedException;

	/**
	 * Start an executable running on a single core.
	 *
	 * @param core
	 *            The coordinates of the core on which to run the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void execute(HasCoreLocation core, File executable, int appID)
			throws IOException, Exception, InterruptedException {
		execute(core, singleton(core.getP()), executable, appID, false);
	}

	/**
	 * Start an executable running on a single chip.
	 *
	 * @param chip
	 *            The coordinates of the chip on which to run the executable
	 * @param processors
	 *            The cores on the chip on which to run the application
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void execute(HasChipLocation chip, Collection<Integer> processors,
			File executable, int appID)
			throws IOException, Exception, InterruptedException {
		execute(chip, processors, executable, appID, false);
	}

	/**
	 * Start an executable running on a single core.
	 *
	 * @param core
	 *            The coordinates of the core on which to run the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @param wait
	 *            True if the binary should enter a "wait" state on loading
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void execute(HasCoreLocation core, File executable, int appID,
			boolean wait) throws IOException, Exception, InterruptedException {
		execute(core, singleton(core.getP()), executable, appID, wait);
	}

	/**
	 * Start an executable running on a single chip.
	 *
	 * @param chip
	 *            The coordinates of the chip on which to run the executable
	 * @param processors
	 *            The cores on the chip on which to run the application
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @param wait
	 *            True if the binary should enter a "wait" state on loading
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	void execute(HasChipLocation chip, Collection<Integer> processors,
			File executable, int appID, boolean wait)
			throws IOException, Exception, InterruptedException;

	/**
	 * Start an executable running on a single core.
	 *
	 * @param core
	 *            The coordinates of the core on which to run the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void execute(HasCoreLocation core, ByteBuffer executable, int appID)
			throws IOException, Exception, InterruptedException {
		execute(core, singleton(core.getP()), executable, appID, false);
	}

	/**
	 * Start an executable running on a single chip.
	 *
	 * @param chip
	 *            The coordinates of the chip on which to run the executable
	 * @param processors
	 *            The cores on the chip on which to run the application
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void execute(HasChipLocation chip, Collection<Integer> processors,
			ByteBuffer executable, int appID)
			throws IOException, Exception, InterruptedException {
		execute(chip, processors, executable, appID, false);
	}

	/**
	 * Start an executable running on a single core.
	 *
	 * @param core
	 *            The coordinates of the core on which to run the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @param wait
	 *            True if the binary should enter a "wait" state on loading
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void execute(HasCoreLocation core, ByteBuffer executable, int appID,
			boolean wait) throws IOException, Exception, InterruptedException {
		execute(core, singleton(core.getP()), executable, appID, wait);
	}

	/**
	 * Start an executable running on a single chip.
	 *
	 * @param chip
	 *            The coordinates of the chip on which to run the executable
	 * @param processors
	 *            The cores on the chip on which to run the application
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @param wait
	 *            True if the binary should enter a "wait" state on loading
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	void execute(HasChipLocation chip, Collection<Integer> processors,
			ByteBuffer executable, int appID, boolean wait)
			throws IOException, Exception, InterruptedException;

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 *
	 * @param coreSubsets
	 *            Which cores on which chips to start the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param numBytes
	 *            The size of the executable data in bytes.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void executeFlood(CoreSubsets coreSubsets, InputStream executable,
			int numBytes, int appID)
			throws IOException, Exception, InterruptedException {
		executeFlood(coreSubsets, executable, numBytes, appID, false);
	}

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 *
	 * @param coreSubsets
	 *            Which cores on which chips to start the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param numBytes
	 *            The size of the executable data in bytes.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @param wait
	 *            True if the processors should enter a "wait" state on loading
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	void executeFlood(CoreSubsets coreSubsets, InputStream executable,
			int numBytes, int appID, boolean wait)
			throws IOException, Exception, InterruptedException;

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 *
	 * @param coreSubsets
	 *            Which cores on which chips to start the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void executeFlood(CoreSubsets coreSubsets, File executable,
			int appID) throws IOException, Exception, InterruptedException {
		executeFlood(coreSubsets, executable, appID, false);
	}

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 *
	 * @param coreSubsets
	 *            Which cores on which chips to start the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @param wait
	 *            True if the processors should enter a "wait" state on loading
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	void executeFlood(CoreSubsets coreSubsets, File executable, int appID,
			boolean wait) throws IOException, Exception, InterruptedException;

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 *
	 * @param coreSubsets
	 *            Which cores on which chips to start the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void executeFlood(CoreSubsets coreSubsets, ByteBuffer executable,
			int appID) throws IOException, Exception, InterruptedException {
		executeFlood(coreSubsets, executable, appID, false);
	}

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 *
	 * @param coreSubsets
	 *            Which cores on which chips to start the executable
	 * @param executable
	 *            The data that is to be executed.
	 * @param appID
	 *            The ID of the application with which to associate the
	 *            executable
	 * @param wait
	 *            True if the processors should enter a "wait" state on loading
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	void executeFlood(CoreSubsets coreSubsets, ByteBuffer executable, int appID,
			boolean wait) throws IOException, Exception, InterruptedException;

	/**
	 * Execute a set of binaries that make up a complete application on
	 * specified cores, wait for them to be ready and then start all of the
	 * binaries. Note this will get the binaries into <tt>c_main()</tt> but will
	 * not signal the barrier.
	 *
	 * @param executableTargets
	 *            The binaries to be executed and the cores to execute them on
	 * @param appID
	 *            The application ID to give this application
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 * @throws SpinnmanException
	 *             If some cores enter an unexpected state.
	 */
	default void executeApplication(ExecutableTargets executableTargets,
			int appID) throws IOException, Exception, InterruptedException,
			SpinnmanException {
		// Execute each of the binaries and get them in to a "wait" state
		for (String binary : executableTargets.getBinaries()) {
			executeFlood(executableTargets.getCoresForBinary(binary),
					new File(binary), appID, true);
		}

		// Sleep to allow cores to get going
		sleep(LAUNCH_DELAY);

		// Check that the binaries have reached a wait state
		int count = getCoreStateCount(appID, READY);
		if (count < executableTargets.getTotalProcessors()) {
			Map<CoreLocation, CPUInfo> coresNotReady = getCoresNotInState(
					executableTargets.getAllCoreSubsets(), READY);
			if (!coresNotReady.isEmpty()) {
				StringBuilder b = new StringBuilder(String.format(
						"Only %d of %d cores reached ready state:", count,
						executableTargets.getTotalProcessors()));
				for (CPUInfo info : coresNotReady.values()) {
					b.append('\n').append(info.getStatusDescription());
				}
				throw new SpinnmanException(b.toString());
			}
		}

		// Send a signal telling the application to start
		sendSignal(appID, START);
	}

	/**
	 * Power on the whole machine.
	 *
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	void powerOnMachine() throws InterruptedException, IOException, Exception;

	/**
	 * Power on a set of boards in the machine.
	 *
	 * @param boards
	 *            The board or boards to power on
	 * @param frame
	 *            the ID of the frame in the cabinet containing the boards, or 0
	 *            if the boards are not in a frame
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void powerOn(Collection<Integer> boards, int frame)
			throws InterruptedException, IOException, Exception {
		power(POWER_ON, boards, 0, frame);
	}

	/**
	 * Power on a set of boards in the machine.
	 *
	 * @param boards
	 *            The board or boards to power on
	 * @param cabinet
	 *            the ID of the cabinet containing the frame, or 0 if the frame
	 *            is not in a cabinet
	 * @param frame
	 *            the ID of the frame in the cabinet containing the boards, or 0
	 *            if the boards are not in a frame
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void powerOn(Collection<Integer> boards, int cabinet, int frame)
			throws InterruptedException, IOException, Exception {
		power(POWER_ON, boards, cabinet, frame);
	}

	/**
	 * Power on a board in the machine.
	 *
	 * @param board
	 *            The board to power off (in cabinet 0, frame 0)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void powerOn(int board)
			throws InterruptedException, IOException, Exception {
		power(POWER_ON, singleton(board), 0, 0);
	}

	/**
	 * Power on a board in the machine.
	 *
	 * @param board
	 *            The board to power on
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board, or 0
	 *            if the board is not in a frame
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void powerOn(int board, int frame)
			throws InterruptedException, IOException, Exception {
		power(POWER_ON, singleton(board), 0, frame);
	}

	/**
	 * Power on a board in the machine.
	 *
	 * @param board
	 *            The board to power on
	 * @param cabinet
	 *            the ID of the cabinet containing the frame, or 0 if the frame
	 *            is not in a cabinet
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board, or 0
	 *            if the board is not in a frame
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void powerOn(int board, int cabinet, int frame)
			throws InterruptedException, IOException, Exception {
		power(POWER_ON, singleton(board), cabinet, frame);
	}

	/**
	 * Power off the whole machine.
	 *
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	void powerOffMachine() throws InterruptedException, IOException, Exception;

	/**
	 * Power off a set of boards in the machine.
	 *
	 * @param boards
	 *            The board or boards to power off
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board(s), or
	 *            0 if the board is not in a frame
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void powerOff(Collection<Integer> boards, int frame)
			throws InterruptedException, IOException, Exception {
		power(POWER_OFF, boards, 0, frame);
	}

	/**
	 * Power off a set of boards in the machine.
	 *
	 * @param boards
	 *            The board or boards to power off
	 * @param cabinet
	 *            the ID of the cabinet containing the frame, or 0 if the frame
	 *            is not in a cabinet
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board(s), or
	 *            0 if the board is not in a frame
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void powerOff(Collection<Integer> boards, int cabinet, int frame)
			throws InterruptedException, IOException, Exception {
		power(POWER_OFF, boards, cabinet, frame);
	}

	/**
	 * Power off a board in the machine.
	 *
	 * @param board
	 *            The board to power off (in cabinet 0, frame 0)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void powerOff(int board)
			throws InterruptedException, IOException, Exception {
		power(POWER_OFF, singleton(board), 0, 0);
	}

	/**
	 * Power off a board in the machine.
	 *
	 * @param board
	 *            The board to power off
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board, or 0
	 *            if the board is not in a frame
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void powerOff(int board, int frame)
			throws InterruptedException, IOException, Exception {
		power(POWER_OFF, singleton(board), 0, frame);
	}

	/**
	 * Power off a board in the machine.
	 *
	 * @param board
	 *            The board to power off
	 * @param cabinet
	 *            the ID of the cabinet containing the frame, or 0 if the frame
	 *            is not in a cabinet
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board, or 0
	 *            if the board is not in a frame
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	default void powerOff(int board, int cabinet, int frame)
			throws InterruptedException, IOException, Exception {
		power(POWER_OFF, singleton(board), cabinet, frame);
	}

	/**
	 * Send a power request to the machine.
	 *
	 * @param powerCommand
	 *            The power command to send
	 * @param boards
	 *            The boards to send the command to
	 * @param cabinet
	 *            the ID of the cabinet containing the frame, or 0 if the frame
	 *            is not in a cabinet
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board(s), or
	 *            0 if the board is not in a frame
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	void power(PowerCommand powerCommand, Collection<Integer> boards,
			int cabinet, int frame)
			throws InterruptedException, IOException, Exception;

	/**
	 * Set the LED state of a board in the machine.
	 *
	 * @param leds
	 *            Collection of LED numbers to set the state of (0-7)
	 * @param action
	 *            State to set the LED to, either on, off or toggle
	 * @param board
	 *            Specifies the board to control the LEDs of. The command will
	 *            actually be sent to the first board in the collection.
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void setLED(Collection<Integer> leds, LEDAction action,
			Collection<Integer> board, int cabinet, int frame)
			throws IOException, Exception;

	/**
	 * Set the LED state of a board in the machine.
	 *
	 * @param leds
	 *            Collection of LED numbers to set the state of (0-7)
	 * @param action
	 *            State to set the LED to, either on, off or toggle
	 * @param board
	 *            Specifies the board to control the LEDs of.
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void setLED(Collection<Integer> leds, LEDAction action, int board,
			int cabinet, int frame) throws IOException, Exception {
		setLED(leds, action, singleton(board), cabinet, frame);
	}

	/**
	 * Set the LED state of a board in the machine.
	 *
	 * @param led
	 *            Number of the LED to set the state of (0-7)
	 * @param action
	 *            State to set the LED to, either on, off or toggle
	 * @param board
	 *            Specifies the board to control the LEDs of. The command will
	 *            actually be sent to the first board in the collection.
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void setLED(int led, LEDAction action, Collection<Integer> board,
			int cabinet, int frame) throws IOException, Exception {
		setLED(singleton(led), action, board, cabinet, frame);
	}

	/**
	 * Set the LED state of a board in the machine.
	 *
	 * @param led
	 *            Number of the LED to set the state of (0-7)
	 * @param action
	 *            State to set the LED to, either on, off or toggle
	 * @param board
	 *            Specifies the board to control the LEDs of.
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void setLED(int led, LEDAction action, int board, int cabinet,
			int frame) throws IOException, Exception {
		setLED(singleton(led), action, singleton(board), cabinet, frame);
	}

	/**
	 * Read a register on a FPGA of a board. The meaning of the register's
	 * contents will depend on the FPGA's configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	int readFPGARegister(int fpgaNumber, int register, int cabinet, int frame,
			int board) throws IOException, Exception;

	/**
	 * Write a register on a FPGA of a board. The meaning of setting the
	 * register's contents will depend on the FPGA's configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param value
	 *            the value to write into the FPGA register
	 * @param cabinet
	 *            cabinet: the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to write the FPGA register to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void writeFPGARegister(int fpgaNumber, int register, int value, int cabinet,
			int frame, int board) throws IOException, Exception;

	/**
	 * Read the ADC data.
	 *
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the ADC data from
	 * @return the FPGA's ADC data object
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	ADCInfo readADCData(int board, int cabinet, int frame)
			throws IOException, Exception;

	/**
	 * Read the BMP version.
	 *
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param boards
	 *            which board to request the data from; the first board in the
	 *            collection will be queried
	 * @return the SVER from the BMP
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default VersionInfo readBMPVersion(Iterable<Integer> boards, int cabinet,
			int frame) throws IOException, Exception {
		return readBMPVersion(boards.iterator().next(), cabinet, frame);
	}

	/**
	 * Read the BMP version.
	 *
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the data from
	 * @return the SVER from the BMP
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	VersionInfo readBMPVersion(int board, int cabinet, int frame)
			throws IOException, Exception;

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param chip
	 *            The coordinates of the chip where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataStream
	 *            The stream of data that is to be written.
	 * @param numBytes
	 *            The amount of data to be written in bytes.
	 * @throws IOException
	 *             If anything goes wrong with networking or reading from the
	 *             input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeMemory(HasChipLocation chip, int baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, Exception {
		writeMemory(chip.getScampCore(), baseAddress, dataStream, numBytes);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataStream
	 *            The stream of data that is to be written.
	 * @param numBytes
	 *            The amount of data to be written in bytes.
	 * @throws IOException
	 *             If anything goes wrong with networking or reading from the
	 *             input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void writeMemory(HasCoreLocation core, int baseAddress,
			InputStream dataStream, int numBytes) throws IOException, Exception;

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param chip
	 *            The coordinates of the chip where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The file holding the data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking or reading from the
	 *             file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeMemory(HasChipLocation chip, int baseAddress,
			File dataFile) throws IOException, Exception {
		writeMemory(chip.getScampCore(), baseAddress, dataFile);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The file holding the data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking or reading from the
	 *             file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void writeMemory(HasCoreLocation core, int baseAddress, File dataFile)
			throws IOException, Exception;

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param chip
	 *            The coordinates of the chip where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written (as 4 bytes).
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeMemory(HasChipLocation chip, int baseAddress,
			int dataWord) throws IOException, Exception {
		writeMemory(chip.getScampCore(), baseAddress, dataWord);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written (as 4 bytes).
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeMemory(HasCoreLocation core, int baseAddress,
			int dataWord) throws IOException, Exception {
		ByteBuffer b = allocate(WORD_SIZE).order(LITTLE_ENDIAN);
		b.putInt(dataWord).flip();
		writeMemory(core, baseAddress, b);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param chip
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeMemory(HasChipLocation chip, int baseAddress, byte[] data)
			throws IOException, Exception {
		writeMemory(chip.getScampCore(), baseAddress, wrap(data));
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeMemory(HasCoreLocation core, int baseAddress, byte[] data)
			throws IOException, Exception {
		writeMemory(core, baseAddress, wrap(data));
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param chip
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. The data should be from the
	 *            <i>position</i> to the <i>limit</i>.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeMemory(HasChipLocation chip, int baseAddress,
			ByteBuffer data) throws IOException, Exception {
		writeMemory(chip.getScampCore(), baseAddress, data);
	}

	/**
	 * Write to the SDRAM on the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is that is to be
	 *            written to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. The data should be from the
	 *            <i>position</i> to the <i>limit</i>.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void writeMemory(HasCoreLocation core, int baseAddress, ByteBuffer data)
			throws IOException, Exception;

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataStream
	 *            The stream of data that is to be written.
	 * @param numBytes
	 *            The amount of data to be written in bytes.
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, InputStream dataStream, int numBytes)
			throws IOException, Exception {
		writeNeighbourMemory(chip.getScampCore(), link, baseAddress, dataStream,
				numBytes);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataStream
	 *            The stream of data that is to be written.
	 * @param numBytes
	 *            The amount of data to be written in bytes.
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void writeNeighbourMemory(HasCoreLocation core, int link, int baseAddress,
			InputStream dataStream, int numBytes) throws IOException, Exception;

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The file holding the data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, File dataFile) throws IOException, Exception {
		writeNeighbourMemory(chip.getScampCore(), link, baseAddress, dataFile);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The name of the file holding the data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void writeNeighbourMemory(HasCoreLocation core, int link, int baseAddress,
			File dataFile) throws IOException, Exception;

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, int dataWord) throws IOException, Exception {
		writeNeighbourMemory(chip.getScampCore(), link, baseAddress, dataWord);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, int dataWord) throws IOException, Exception {
		ByteBuffer b = allocate(WORD_SIZE).order(LITTLE_ENDIAN);
		b.putInt(dataWord).flip();
		writeNeighbourMemory(core, link, baseAddress, b);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, byte[] data) throws IOException, Exception {
		writeNeighbourMemory(chip.getScampCore(), link, baseAddress, data);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, byte[] data) throws IOException, Exception {
		writeNeighbourMemory(core, link, baseAddress, wrap(data));
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. The data should be from the
	 *            <i>position</i> to the <i>limit</i>.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, ByteBuffer data) throws IOException, Exception {
		writeNeighbourMemory(chip.getScampCore(), link, baseAddress, data);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_READ SCP command.
	 * If sent to a BMP, this command can be used to communicate with the FPGAs'
	 * debug registers.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. The data should be from the
	 *            <i>position</i> to the <i>limit</i>.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void writeNeighbourMemory(HasCoreLocation core, int link, int baseAddress,
			ByteBuffer data) throws IOException, Exception;

	/**
	 * Write to the SDRAM of all chips.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataStream
	 *            The stream of data that is to be written.
	 * @param numBytes
	 *            The amount of data to be written in bytes.
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the input stream.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void writeMemoryFlood(int baseAddress, InputStream dataStream, int numBytes)
			throws IOException, Exception;

	/**
	 * Write to the SDRAM of all chips.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The name of the file holding the data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void writeMemoryFlood(int baseAddress, File dataFile)
			throws IOException, Exception;

	/**
	 * Write to the SDRAM of all chips.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeMemoryFlood(int baseAddress, int dataWord)
			throws IOException, Exception {
		ByteBuffer b = allocate(WORD_SIZE).order(LITTLE_ENDIAN);
		b.putInt(dataWord).flip();
		writeMemoryFlood(baseAddress, b);
	}

	/**
	 * Write to the SDRAM of all chips.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void writeMemoryFlood(int baseAddress, byte[] data)
			throws IOException, Exception {
		writeMemoryFlood(baseAddress, wrap(data));
	}

	/**
	 * Write to the SDRAM of all chips.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. The data should be from the
	 *            <i>position</i> to the <i>limit</i>.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void writeMemoryFlood(int baseAddress, ByteBuffer data)
			throws IOException, Exception;

	/**
	 * Read some areas of SDRAM from the board.
	 *
	 * @param chip
	 *            The coordinates of the chip where the memory is to be read
	 *            from
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory to be read
	 *            starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data read, positioned at the start of
	 *         the data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default ByteBuffer readMemory(HasChipLocation chip, int baseAddress,
			int length) throws IOException, Exception {
		return readMemory(chip.getScampCore(), baseAddress, length);
	}

	/**
	 * Read some areas of SDRAM from the board.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is to be read
	 *            from
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory to be read
	 *            starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data read, positioned at the start of
	 *         the data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	ByteBuffer readMemory(HasCoreLocation core, int baseAddress, int length)
			throws IOException, Exception;

	/**
	 * Read some areas of memory on a neighbouring chip using a LINK_READ SCP
	 * command.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be read from
	 * @param link
	 *            The link index to send the request to
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory to be read
	 *            starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data that has been read, positioned at
	 *         the start of the data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default ByteBuffer readNeighbourMemory(HasChipLocation chip, int link,
			int baseAddress, int length) throws IOException, Exception {
		return readNeighbourMemory(chip.getScampCore(), link, baseAddress,
				length);
	}

	/**
	 * Read some areas of memory on a neighbouring chip using a LINK_READ SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers.
	 *
	 * @param core
	 *            The coordinates of the chip whose neighbour is to be read
	 *            from, plus the CPU to use (typically 0, or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link index to send the request to (or if BMP, the FPGA
	 *            number)
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory to be read
	 *            starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data that has been read, positioned at
	 *         the start of the data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	ByteBuffer readNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, int length) throws IOException, Exception;

	/**
	 * Sends a stop request for an application ID.
	 *
	 * @param appID
	 *            The ID of the application to send to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void stopApplication(int appID) throws IOException, Exception;

	/**
	 * Waits for the specified cores running the given application to be in some
	 * target state or states. Handles failures.
	 *
	 * @param allCoreSubsets
	 *            the cores to check are in a given sync state
	 * @param appID
	 *            the application ID that being used by the simulation
	 * @param cpuStates
	 *            The expected states once the applications are ready; success
	 *            is when each application is in one of these states
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 * @throws SpinnmanException
	 *             If some cores enter an error state.
	 */
	default void waitForCoresToBeInState(CoreSubsets allCoreSubsets, int appID,
			Set<CPUState> cpuStates) throws IOException, Exception,
			InterruptedException, SpinnmanException {
		waitForCoresToBeInState(allCoreSubsets, appID, cpuStates,
				TIMEOUT_DISABLED, DEFAULT_POLL_INTERVAL, DEFAULT_ERROR_STATES,
				DEFAULT_CHECK_INTERVAL);
	}

	/**
	 * Waits for the specified cores running the given application to be in some
	 * target state or states. Handles failures.
	 *
	 * @param allCoreSubsets
	 *            the cores to check are in a given sync state
	 * @param appID
	 *            the application ID that being used by the simulation
	 * @param cpuStates
	 *            The expected states once the applications are ready; success
	 *            is when each application is in one of these states
	 * @param timeout
	 *            The amount of time to wait in milliseconds for the cores to
	 *            reach one of the states, or <tt>null</tt> to wait for an
	 *            unbounded amount of time.
	 * @param timeBetweenPolls
	 *            Time between checking the state, in milliseconds
	 * @param errorStates
	 *            Set of states that the application can be in that indicate an
	 *            error, and so should raise an exception
	 * @param countsBetweenFullCheck
	 *            The number of times to use the count signal before instead
	 *            using the full CPU state check
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 * @throws SpinnmanException
	 *             If some cores enter an error state.
	 */
	void waitForCoresToBeInState(CoreSubsets allCoreSubsets, int appID,
			Set<CPUState> cpuStates, Integer timeout, int timeBetweenPolls,
			Set<CPUState> errorStates, int countsBetweenFullCheck)
			throws IOException, Exception, InterruptedException,
			SpinnmanException;

	/**
	 * Get all cores that are in a given state.
	 *
	 * @param allCoreSubsets
	 *            The cores to filter
	 * @param state
	 *            The states to filter on
	 * @return Core subsets object containing cores in the given state
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default CoreSubsets getCoresInState(CoreSubsets allCoreSubsets,
			CPUState state) throws IOException, Exception {
		return getCoresInState(allCoreSubsets, singleton(state));
	}

	/**
	 * Get all cores that are in a given set of states.
	 *
	 * @param allCoreSubsets
	 *            The cores to filter
	 * @param states
	 *            The states to filter on
	 * @return Core subsets object containing cores in the given states
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default CoreSubsets getCoresInState(CoreSubsets allCoreSubsets,
			Set<CPUState> states) throws IOException, Exception {
		Iterable<CPUInfo> coreInfos = getCPUInformation(allCoreSubsets);
		CoreSubsets coresInState = new CoreSubsets();
		for (CPUInfo coreInfo : coreInfos) {
			if (states.contains(coreInfo.getState())) {
				coresInState.addCore(coreInfo.asCoreLocation());
			}
		}
		return coresInState;
	}

	/**
	 * Get all cores that are not in a given state.
	 *
	 * @param allCoreSubsets
	 *            The cores to filter
	 * @param state
	 *            The state to filter on
	 * @return Core subsets object containing cores not in the given state
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default Map<CoreLocation, CPUInfo> getCoresNotInState(
			CoreSubsets allCoreSubsets, CPUState state)
			throws IOException, Exception {
		return getCoresNotInState(allCoreSubsets, singleton(state));
	}

	/**
	 * Get all cores that are not in a given set of states.
	 *
	 * @param allCoreSubsets
	 *            The cores to filter
	 * @param states
	 *            The states to filter on
	 * @return Core subsets object containing cores not in the given states
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default Map<CoreLocation, CPUInfo> getCoresNotInState(
			CoreSubsets allCoreSubsets, Set<CPUState> states)
			throws IOException, Exception {
		Iterable<CPUInfo> coreInfos = getCPUInformation(allCoreSubsets);
		Map<CoreLocation, CPUInfo> coresNotInState = new TreeMap<>();
		for (CPUInfo coreInfo : coreInfos) {
			if (!states.contains(coreInfo.getState())) {
				coresNotInState.put(coreInfo.asCoreLocation(), coreInfo);
			}
		}
		return coresNotInState;
	}

	/**
	 * Send a signal to an application.
	 *
	 * @param appID
	 *            The ID of the application to send to
	 * @param signal
	 *            The signal to send
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void sendSignal(int appID, Signal signal) throws IOException, Exception;

	/**
	 * Set LED states.
	 *
	 * @param chip
	 *            The coordinates of the chip on which to set the LEDs
	 * @param ledStates
	 *            A map from LED index to state with 0 being off, 1 on and 2
	 *            inverted.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void setLEDs(HasChipLocation chip,
			Map<Integer, LEDAction> ledStates) throws IOException, Exception {
		setLEDs(chip.getScampCore(), ledStates);
	}

	/**
	 * Set LED states.
	 *
	 * @param core
	 *            The coordinates of the core on which to set the LEDs
	 * @param ledStates
	 *            A map from LED index to state with 0 being off, 1 on and 2
	 *            inverted.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void setLEDs(HasCoreLocation core, Map<Integer, LEDAction> ledStates)
			throws IOException, Exception;

	/**
	 * Find a connection that matches the given board IP address.
	 *
	 * @param boardAddress
	 *            The IP address of the Ethernet connection on the board
	 * @return A connection for the given IP address, or <tt>null</tt> if no
	 *         such connection exists
	 */
	SCPConnection locateSpinnakerConnection(InetAddress boardAddress);

	/**
	 * Set up an IP tag.
	 *
	 * @param tag
	 *            The tag to set up; note its board address can be
	 *            <tt>null</tt>, in which case, the tag will be assigned to all
	 *            boards
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void setIPTag(IPTag tag) throws IOException, Exception;

	/**
	 * Set up a reverse IP tag.
	 *
	 * @param tag
	 *            The reverse tag to set up; note its board address can be
	 *            <tt>null</tt>, in which case, the tag will be assigned to all
	 *            boards
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void setReverseIPTag(ReverseIPTag tag) throws IOException, Exception;

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void clearIPTag(Tag tag) throws IOException, Exception {
		clearIPTag(tag.getTag(), null, tag.getBoardAddress());
	}

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag ID
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void clearIPTag(int tag) throws IOException, Exception {
		clearIPTag(tag, null, null);
	}

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag ID
	 * @param connection
	 *            Connection where the tag should be cleared. If not specified,
	 *            all SCPSender connections will send the message to clear the
	 *            tag
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void clearIPTag(int tag, SCPConnection connection)
			throws IOException, Exception {
		clearIPTag(tag, requireNonNull(connection), null);
	}

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag
	 * @param connection
	 *            Connection where the tag should be cleared. If not specified,
	 *            all SCPSender connections will send the message to clear the
	 *            tag
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void clearIPTag(Tag tag, SCPConnection connection)
			throws IOException, Exception {
		clearIPTag(tag.getTag(), requireNonNull(connection),
				tag.getBoardAddress());
	}

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag ID
	 * @param boardAddress
	 *            Board address where the tag should be cleared.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void clearIPTag(int tag, InetAddress boardAddress)
			throws IOException, Exception {
		clearIPTag(tag, null, requireNonNull(boardAddress));
	}

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag ID
	 * @param connection
	 *            Connection where the tag should be cleared. If not specified,
	 *            all SCPSender connections will send the message to clear the
	 *            tag
	 * @param boardAddress
	 *            Board address where the tag should be cleared. If not
	 *            specified, all SCPSender connections will send the message to
	 *            clear the tag
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void clearIPTag(int tag, SCPConnection connection, InetAddress boardAddress)
			throws IOException, Exception;

	/**
	 * Get the current set of tags that have been set on the board using all
	 * SCPSender connections.
	 *
	 * @return An iterable of tags
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default List<Tag> getTags() throws IOException, Exception {
		return getTags(null);
	}

	/**
	 * Get the current set of tags that have been set on the board.
	 *
	 * @param connection
	 *            Connection from which the tags should be received.
	 * @return An iterable of tags
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	List<Tag> getTags(SCPConnection connection) throws IOException, Exception;

	/**
	 * Allocates a chunk of SDRAM on a chip on the machine.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to allocate memory
	 * @param size
	 *            The amount of memory to allocate in bytes
	 * @return the base address of the allocated memory
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default int mallocSDRAM(HasChipLocation chip, int size)
			throws IOException, Exception {
		return mallocSDRAM(chip, size, 0, 0);
	}

	/**
	 * Allocates a chunk of SDRAM on a chip on the machine.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to allocate memory
	 * @param size
	 *            The amount of memory to allocate in bytes
	 * @param appID
	 *            The ID of the application with which to associate the routes.
	 * @return the base address of the allocated memory
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default int mallocSDRAM(HasChipLocation chip, int size, int appID)
			throws IOException, Exception {
		return mallocSDRAM(chip, size, appID, 0);
	}

	/**
	 * Allocates a chunk of SDRAM on a chip on the machine.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to allocate memory
	 * @param size
	 *            The amount of memory to allocate in bytes
	 * @param appID
	 *            The ID of the application with which to associate the routes.
	 * @param tag
	 *            The tag for the SDRAM, a 8-bit (chip-wide) tag that can be
	 *            looked up by a SpiNNaker application to discover the address
	 *            of the allocated block.
	 * @return the base address of the allocated memory
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	int mallocSDRAM(HasChipLocation chip, int size, int appID, int tag)
			throws IOException, Exception;

	/**
	 * Free allocated SDRAM.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to free memory
	 * @param baseAddress
	 *            The base address of the allocated memory
	 * @param appID
	 *            The app ID of the allocated memory
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void freeSDRAM(HasChipLocation chip, int baseAddress, int appID)
			throws IOException, Exception;

	/**
	 * Free all SDRAM allocated to a given application ID.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to free memory
	 * @param appID
	 *            The app ID of the allocated memory
	 * @return The number of blocks freed
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	int freeSDRAMByAppID(HasChipLocation chip, int appID)
			throws IOException, Exception;

	/**
	 * Load a set of multicast routes on to a chip associated with the default
	 * application ID.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to load the routes
	 * @param routes
	 *            An iterable of multicast routes to load
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void loadMulticastRoutes(HasChipLocation chip,
			Collection<MulticastRoutingEntry> routes)
			throws IOException, Exception {
		loadMulticastRoutes(chip, routes, 0);
	}

	/**
	 * Load a set of multicast routes on to a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to load the routes
	 * @param routes
	 *            An iterable of multicast routes to load
	 * @param appID
	 *            The ID of the application with which to associate the routes.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void loadMulticastRoutes(HasChipLocation chip,
			Collection<MulticastRoutingEntry> routes, int appID)
			throws IOException, Exception;

	/**
	 * Loads a fixed route routing table entry onto a chip's router.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to load the route
	 * @param fixedRoute
	 *            the route for the fixed route entry on this chip
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void loadFixedRoute(HasChipLocation chip, RoutingEntry fixedRoute)
			throws IOException, Exception {
		loadFixedRoute(chip, fixedRoute, 0);
	}

	/**
	 * Loads a fixed route routing table entry onto a chip's router.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to load the route
	 * @param fixedRoute
	 *            the route for the fixed route entry on this chip
	 * @param appID
	 *            The ID of the application with which to associate the route.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void loadFixedRoute(HasChipLocation chip, RoutingEntry fixedRoute,
			int appID) throws IOException, Exception;

	/**
	 * Reads a fixed route routing table entry from a chip's router.
	 *
	 * @param chip
	 *            The coordinate of the chip from which to read the route.
	 * @return the route as a fixed route entry
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default RoutingEntry readFixedRoute(HasChipLocation chip)
			throws IOException, Exception {
		return readFixedRoute(chip, 0);
	}

	/**
	 * Reads a fixed route routing table entry from a chip's router.
	 *
	 * @param chip
	 *            The coordinate of the chip from which to read the route.
	 * @param appID
	 *            The ID of the application associated the route.
	 * @return the route as a fixed route entry
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	RoutingEntry readFixedRoute(HasChipLocation chip, int appID)
			throws IOException, Exception;

	/**
	 * Get the current multicast routes set up on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip from which to get the routes
	 * @return An iterable of multicast routes
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default List<MulticastRoutingEntry> getMulticastRoutes(HasChipLocation chip)
			throws IOException, Exception {
		return getMulticastRoutes(chip, null);
	}

	/**
	 * Get the current multicast routes set up on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip from which to get the routes
	 * @param appID
	 *            The ID of the application to filter the routes for.
	 * @return An iterable of multicast routes
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default List<MulticastRoutingEntry> getMulticastRoutes(HasChipLocation chip,
			int appID) throws IOException, Exception {
		return getMulticastRoutes(chip, appID);
	}

	/**
	 * Get the current multicast routes set up on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip from which to get the routes
	 * @param appID
	 *            The ID of the application to filter the routes for.
	 * @return An iterable of multicast routes
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	List<MulticastRoutingEntry> getMulticastRoutes(HasChipLocation chip,
			Integer appID) throws IOException, Exception;

	/**
	 * Remove all the multicast routes on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip on which to clear the routes
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void clearMulticastRoutes(HasChipLocation chip)
			throws IOException, Exception;

	/**
	 * Get router diagnostic information from a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip from which to get the information
	 * @return The router diagnostic information
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	RouterDiagnostics getRouterDiagnostics(HasChipLocation chip)
			throws IOException, Exception;

	/**
	 * Sets a router diagnostic filter in a router.
	 *
	 * @param chip
	 *            the address of the router in which this filter is being set
	 * @param position
	 *            the position in the list of filters where this filter is to be
	 *            added, between 0 and 15 (note that positions 0 to 11 are used
	 *            by the default filters, and setting these positions will
	 *            result in a warning).
	 * @param diagnosticFilter
	 *            the diagnostic filter being set in the position.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void setRouterDiagnosticFilter(HasChipLocation chip, int position,
			DiagnosticFilter diagnosticFilter) throws IOException, Exception;

	/**
	 * Gets a router diagnostic filter from a router.
	 *
	 * @param chip
	 *            the address of the router from which this filter is being
	 *            retrieved
	 * @param position
	 *            the position in the list of filters where this filter is to be
	 *            read from
	 * @return The diagnostic filter read
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	DiagnosticFilter getRouterDiagnosticFilter(HasChipLocation chip,
			int position) throws IOException, Exception;

	/**
	 * Clear router diagnostic information on a chip. Resets and enables all
	 * diagnostic counters.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void clearRouterDiagnosticCounters(HasChipLocation chip)
			throws IOException, Exception {
		clearRouterDiagnosticCounters(chip, false,
				range(0, NO_ROUTER_DIAGNOSTIC_FILTERS).boxed()
						.collect(toList()));
	}

	/**
	 * Clear router diagnostic information on a chip. Resets all diagnostic
	 * counters.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param enable
	 *            True (default) if the counters should be enabled
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void clearRouterDiagnosticCounters(HasChipLocation chip,
			boolean enable) throws IOException, Exception {
		clearRouterDiagnosticCounters(chip, enable,
				range(0, NO_ROUTER_DIAGNOSTIC_FILTERS).boxed()
						.collect(toList()));
	}

	/**
	 * Clear router diagnostic information on a chip. Resets and enables all the
	 * numbered counters.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param counterIDs
	 *            The IDs of the counters to reset and enable; each must be
	 *            between 0 and 15
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void clearRouterDiagnosticCounters(HasChipLocation chip,
			Iterable<Integer> counterIDs) throws IOException, Exception {
		clearRouterDiagnosticCounters(chip, false, counterIDs);
	}

	/**
	 * Clear router diagnostic information on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param enable
	 *            True (default) if the counters should be enabled
	 * @param counterIDs
	 *            The IDs of the counters to reset and enable if enable is True;
	 *            each must be between 0 and 15
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void clearRouterDiagnosticCounters(HasChipLocation chip, boolean enable,
			Iterable<Integer> counterIDs) throws IOException, Exception;

	/**
	 * Get the contents of the SDRAM heap on a given chip.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @return the list of chunks in the heap
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default List<HeapElement> getHeap(HasChipLocation chip)
			throws IOException, Exception {
		return getHeap(chip, sdram_heap_address);
	}

	/**
	 * Get the contents of the given heap on a given chip.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param heap
	 *            The SystemVariableDefinition which is the heap to read
	 * @return the list of chunks in the heap
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	List<HeapElement> getHeap(HasChipLocation chip,
			SystemVariableDefinition heap) throws IOException, Exception;

	/**
	 * Fill some memory with repeated data.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param baseAddress
	 *            The address at which to start the fill
	 * @param repeatValue
	 *            The data to repeat
	 * @param size
	 *            The number of bytes to fill. Must be compatible with the data
	 *            type i.e. if the data type is WORD, the number of bytes must
	 *            be divisible by 4
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	default void fillMemory(HasChipLocation chip, int baseAddress,
			int repeatValue, int size) throws Exception, IOException {
		fillMemory(chip, baseAddress, repeatValue, size, WORD);
	}

	/**
	 * Fill some memory with repeated data.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @param baseAddress
	 *            The address at which to start the fill
	 * @param repeatValue
	 *            The data to repeat
	 * @param size
	 *            The number of bytes to fill. Must be compatible with the data
	 *            type i.e. if the data type is WORD, the number of bytes must
	 *            be divisible by 4
	 * @param dataType
	 *            The type of data to fill.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	void fillMemory(HasChipLocation chip, int baseAddress, int repeatValue,
			int size, DataType dataType) throws Exception, IOException;
}
