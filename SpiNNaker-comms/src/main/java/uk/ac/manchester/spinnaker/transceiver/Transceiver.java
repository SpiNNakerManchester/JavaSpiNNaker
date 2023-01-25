/*
 * Copyright (c) 2018-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.net.InetAddress.getByAddress;
import static java.nio.ByteBuffer.allocate;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.IOUtils.buffer;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.NUM_ROUTER_DIAGNOSTIC_COUNTERS;
import static uk.ac.manchester.spinnaker.machine.SpiNNakerTriadGeometry.getSpinn5Geometry;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_POST_POWER_ON_SLEEP_TIME;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_POWER_ON_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.Constants.NO_ROUTER_DIAGNOSTIC_FILTERS;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_DEFAULT_FILTERS_MAX_POSITION;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_DIAGNOSTIC_FILTER_SIZE;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.bmp.ReadSerialVector.SerialVector.SERIAL_LENGTH;
import static uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer.FLASH_CHUNK_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime.TIMEOUT_2560_ms;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.ethernet_ip_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.iobuf_size;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.router_table_copy_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.software_watchdog_count;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.y_size;
import static uk.ac.manchester.spinnaker.messages.scp.SCPRequest.BOOT_CHIP;
import static uk.ac.manchester.spinnaker.transceiver.BMPCommandProcess.BMP_RETRIES;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.EXECUTABLE_ADDRESS;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.ROUTER_DIAGNOSTIC_COUNTER;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.ROUTER_FILTERS;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.SYS_VARS;
import static uk.ac.manchester.spinnaker.transceiver.Utils.defaultBMPforMachine;
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.sliceUp;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.concurrent.GuardedBy;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.BootConnection;
import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.DelegatingSCPConnection;
import uk.ac.manchester.spinnaker.connections.MachineAware;
import uk.ac.manchester.spinnaker.connections.MostDirectConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SDPConnection;
import uk.ac.manchester.spinnaker.connections.SingletonConnectionSelector;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.MachineVersion;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.bmp.BMPRequest;
import uk.ac.manchester.spinnaker.messages.bmp.BMPSetLED;
import uk.ac.manchester.spinnaker.messages.bmp.EraseFlash;
import uk.ac.manchester.spinnaker.messages.bmp.GetBMPVersion;
import uk.ac.manchester.spinnaker.messages.bmp.GetFPGAResetStatus;
import uk.ac.manchester.spinnaker.messages.bmp.ReadADC;
import uk.ac.manchester.spinnaker.messages.bmp.ReadCANStatus;
import uk.ac.manchester.spinnaker.messages.bmp.ReadFPGARegister;
import uk.ac.manchester.spinnaker.messages.bmp.ReadSerialFlashCRC;
import uk.ac.manchester.spinnaker.messages.bmp.ReadSerialVector;
import uk.ac.manchester.spinnaker.messages.bmp.ResetFPGA;
import uk.ac.manchester.spinnaker.messages.bmp.SetPower;
import uk.ac.manchester.spinnaker.messages.bmp.UpdateFlash;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFPGARegister;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer;
import uk.ac.manchester.spinnaker.messages.boot.BootMessages;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.IOBuffer;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.model.Signal;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.ApplicationRun;
import uk.ac.manchester.spinnaker.messages.scp.ApplicationStop;
import uk.ac.manchester.spinnaker.messages.scp.CheckOKResponse;
import uk.ac.manchester.spinnaker.messages.scp.CountState;
import uk.ac.manchester.spinnaker.messages.scp.GetChipInfo;
import uk.ac.manchester.spinnaker.messages.scp.GetVersion;
import uk.ac.manchester.spinnaker.messages.scp.IPTagClear;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSet;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSetTTO;
import uk.ac.manchester.spinnaker.messages.scp.PayloadedResponse;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.ReverseIPTagSet;
import uk.ac.manchester.spinnaker.messages.scp.RouterClear;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SDRAMAlloc;
import uk.ac.manchester.spinnaker.messages.scp.SDRAMDeAlloc;
import uk.ac.manchester.spinnaker.messages.scp.SendSignal;
import uk.ac.manchester.spinnaker.messages.scp.SetLED;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * An encapsulation of various communications with the SpiNNaker board.
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
 * in {@link TransceiverInterface}. <em>Note that operations on a BMP are
 * <strong>always</strong> parallel-unsafe, other documentation in this class
 * notwithstanding.</em>
 */
public class Transceiver extends UDPTransceiver
		implements TransceiverInterface, RetryTracker {
	private static final Logger log = getLogger(Transceiver.class);

	private static final String SCAMP_NAME = "SC&MP";

	private static final Version SCAMP_VERSION = new Version(3, 0, 1);

	private static final String BMP_NAME = "BC&MP";

	private static final Set<Integer> BMP_MAJOR_VERSIONS = Set.of(1, 2);

	/**
	 * How many times do we try to find SCAMP?
	 */
	private static final int INITIAL_FIND_SCAMP_RETRIES_COUNT = 3;

	private static final int CONNECTION_CHECK_RETRY_COUNT = 3;

	private static final int CONNECTION_CHECK_DELAY = 100;

	private static final int NNID_MAX = 0x7F;

	private static final int POST_BOOT_DELAY = 2000;

	/**
	 * The number of milliseconds after powering on the machine to wait before
	 * attempting to boot SCAMP on its chips. This is time to allow the code on
	 * each chip's ROM to figure out what the state of the hardware is enough
	 * for booting to be viable.
	 */
	private static final int POST_POWER_ON_DELAY = 2000;

	private static final int ENABLE_SHIFT = 16;

	/**
	 * How much data to pile into SCAMP before reducing the number of messages
	 * in flight at a time.
	 */
	private static final int LARGE_DATA_WRITE_THRESHOLD = 16 * 1024;

	/**
	 * The maximum number of SCP messages to have in flight in a large data
	 * write.
	 */
	private static final int LARGE_WRITE_PARALLEL_MESSAGE_COUNT = 4;

	/** The version of the board being connected to. */
	private MachineVersion version;

	/** The discovered machine model. */
	private Machine machine;

	private MachineDimensions dimensions;

	/**
	 * A set of chips to ignore in the machine. Requests for a "machine" will
	 * have these chips excluded, as if they never existed. The processor IDs of
	 * the specified chips are ignored.
	 */
	private final Set<ChipLocation> ignoreChips = new HashSet<>();

	/**
	 * A set of cores to ignore in the machine. Requests for a "machine" will
	 * have these cores excluded, as if they never existed.
	 */
	private final Map<ChipLocation, Set<Integer>> ignoreCores = new HashMap<>();

	/**
	 * A set of links to ignore in the machine. Requests for a "machine" will
	 * have these links excluded, as if they never existed.
	 */
	private final Map<ChipLocation, Set<Direction>> ignoreLinks =
			new HashMap<>();

	/**
	 * The max size each chip can say it has for SDRAM. (This is mainly used for
	 * debugging purposes.)
	 */
	private final Integer maxSDRAMSize;

	private Integer iobufSize;

	private AppIdTracker appIDTracker;

	/**
	 * A set of the original connections. Used to determine what can be closed.
	 */
	private final Set<Connection> originalConnections = new HashSet<>();

	/** A set of all connections. Used for closing. */
	private final Set<Connection> allConnections = new HashSet<>();

	/**
	 * A boot send connection. There can only be one in the current system, or
	 * otherwise bad things can happen!
	 */
	private BootConnection bootConnection;

	/** A list of all connections that can be used to send SDP messages. */
	private final List<SDPConnection> sdpConnections = new ArrayList<>();

	/**
	 * A map of IP address &rarr; SCAMP connection. These are those that can be
	 * used for setting up IP Tags.
	 */
	private final Map<InetAddress, SCPConnection> udpScpConnections =
			new HashMap<>();

	/**
	 * A list of all connections that can be used to send and receive SCP
	 * messages for SCAMP interaction.
	 */
	private final List<SCPConnection> scpConnections = new ArrayList<>();

	/** The BMP connections. */
	private final List<BMPConnection> bmpConnections = new ArrayList<>();

	/** Connection selectors for the BMP processes. */
	private final Map<BMPCoords,
			ConnectionSelector<BMPConnection>> bmpSelectors = new HashMap<>();

	/** Connection selectors for the SCP processes. */
	private final ConnectionSelector<SCPConnection> scpSelector;

	/** The nearest neighbour start ID. */
	@GuardedBy("nearestNeighbourLock")
	private int nearestNeighbourID = 1;

	/** The nearest neighbour lock. */
	private final Object nearestNeighbourLock = new Object();

	/**
	 * A lock against multiple flood fill writes. This is needed as SCAMP cannot
	 * cope with this.
	 */
	private final Object floodWriteLock = new Object();

	/**
	 * Lock against single chip executions. The condition should be acquired
	 * before the locks are checked or updated.
	 * <p>
	 * The write lock condition should also be acquired to avoid a flood fill
	 * during an individual chip execute.
	 */
	private final Map<ChipLocation, Semaphore> chipExecuteLocks =
			new HashMap<>();

	@GuardedBy("itself")
	private final FloodLock executeFloodLock = new FloodLock();

	private boolean machineOff = false;

	private long retryCount = 0L;

	private BMPCoords boundBMP = new BMPCoords(0, 0);

	/**
	 * Create a Transceiver by creating a UDPConnection to the given hostname on
	 * port 17893 (the default SCAMP port), and a BootConnection on port 54321
	 * (the default boot port), optionally discovering any additional links
	 * using the UDPConnection, and then returning the transceiver created with
	 * the conjunction of the created UDPConnection and the discovered
	 * connections.
	 *
	 * @param host
	 *            The host IP address of the board
	 * @param version
	 *            The type of SpiNNaker board used within the SpiNNaker machine
	 *            being used. May be {@code null} if the board in question can
	 *            be assumed to be always already booted.
	 * @param numberOfBoards
	 *            a number of boards expected to be supported, or {@code null},
	 *            which defaults to a single board
	 * @param ignoredChips
	 *            An optional set of chips to ignore in the machine. Requests
	 *            for a "machine" will have these chips excluded, as if they
	 *            never existed. The processors of the specified chips are
	 *            ignored.
	 * @param ignoredCores
	 *            An optional map of cores to ignore in the machine. Requests
	 *            for a "machine" will have these cores excluded, as if they
	 *            never existed.
	 * @param ignoredLinks
	 *            An optional set of links to ignore in the machine. Requests
	 *            for a "machine" will have these links excluded, as if they
	 *            never existed.
	 * @param bmpConnectionData
	 *            the details of the BMP connections used to boot multi-board
	 *            systems
	 * @param autodetectBMP
	 *            True if the BMP of version 4 or 5 boards should be
	 *            automatically determined from the board IP address
	 * @param bootPortNumber
	 *            the port number used to boot the machine
	 * @param scampConnections
	 *            the list of connections used for SCAMP communications
	 * @param maxSDRAMSize
	 *            the max size each chip can say it has for SDRAM (mainly used
	 *            in debugging purposes)
	 * @throws IOException
	 *             if networking fails
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@MustBeClosed
	@SuppressWarnings("MustBeClosed")
	public Transceiver(InetAddress host, MachineVersion version,
			Collection<BMPConnectionData> bmpConnectionData,
			Integer numberOfBoards, Set<ChipLocation> ignoredChips,
			Map<ChipLocation, Set<Integer>> ignoredCores,
			Map<ChipLocation, Set<Direction>> ignoredLinks,
			boolean autodetectBMP, List<ConnectionDescriptor> scampConnections,
			Integer bootPortNumber, Integer maxSDRAMSize)
			throws IOException, SpinnmanException, InterruptedException {
		log.info("Creating transceiver for {}", requireNonNull(host,
				"SpiNNaker machine host name must be not null"));
		var connections = new ArrayList<Connection>();

		/*
		 * if no BMP has been supplied, but the board is a spinn4 or a spinn5
		 * machine, then an assumption can be made that the BMP is at -1 on the
		 * final value of the IP address
		 */
		if (version != null && !version.isFourChip && autodetectBMP
				&& (bmpConnectionData == null || bmpConnectionData.isEmpty())) {
			bmpConnectionData =
					List.of(defaultBMPforMachine(host, numberOfBoards));
		}

		// handle BMP connections
		if (bmpConnectionData != null) {
			var bmpIPs = new ArrayList<>();
			for (var connData : bmpConnectionData) {
				var connection = new BMPConnection(connData);
				connections.add(connection);
				bmpIPs.add(connection.getRemoteIPAddress());
			}
			log.info("Transceiver using BMPs: {}", bmpIPs);
		}

		// handle the SpiNNaker connection
		if (scampConnections == null) {
			scampConnections = List.of();
		}
		if (scampConnections.isEmpty()) {
			connections.add(createScpConnection(BOOT_CHIP, host));
		}

		// handle the boot connection
		connections.add(new BootConnection(host, bootPortNumber));

		this.version = version;
		if (ignoredChips != null) {
			ignoreChips.addAll(ignoredChips);
		}
		if (ignoredCores != null) {
			ignoreCores.putAll(ignoredCores);
		}
		if (ignoredLinks != null) {
			ignoreLinks.putAll(ignoredLinks);
		}
		this.maxSDRAMSize = maxSDRAMSize;

		originalConnections.addAll(connections);
		allConnections.addAll(connections);
		// if there has been SCAMP connections given, build them
		for (var desc : scampConnections) {
			if (desc.portNumber != null
					&& desc.portNumber != SCP_SCAMP_PORT) {
				log.warn("ignoring unexpected SCAMP port: {}",
						desc.portNumber);
			}
			connections.add(createScpConnection(desc.chip, desc.hostname));
		}
		for (Connection conn : connections) {
			identifyConnection(conn);
		}
		scpSelector = makeConnectionSelector();
		checkBMPConnections();
	}

	/**
	 * Create a Transceiver by creating a UDPConnection to the given hostname on
	 * port 17893 (the default SCAMP port), and a BootConnection on port 54321
	 * (the default boot port), discovering any additional links using the
	 * UDPConnection, and then returning the transceiver created with the
	 * conjunction of the created UDPConnection and the discovered connections.
	 *
	 * @param hostname
	 *            The hostname or IP address of the board
	 * @param version
	 *            The type of SpiNNaker board used within the SpiNNaker machine
	 *            being used. May be {@code null} if the board in question can
	 *            be assumed to be always already booted.
	 * @throws IOException
	 *             if networking fails
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@MustBeClosed
	public Transceiver(InetAddress hostname, MachineVersion version)
			throws IOException, SpinnmanException, InterruptedException {
		this(hostname, version, null, 0, Set.of(), Map.of(), Map.of(), false,
				null, null, null);
	}

	/**
	 * Create a transceiver.
	 *
	 * @param version
	 *            The SpiNNaker board version number.
	 * @throws IOException
	 *             if networking fails
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@MustBeClosed
	public Transceiver(MachineVersion version)
			throws IOException, SpinnmanException, InterruptedException {
		this(version, null, null, null, null, null, null);
	}

	/**
	 * Create a transceiver.
	 *
	 * @param version
	 *            The type of SpiNNaker board used within the SpiNNaker machine
	 *            being used. May be {@code null} if the board in question can
	 *            be assumed to be always already booted.
	 * @param connections
	 *            The connections to use in the transceiver. Note that the
	 *            transceiver may make additional connections.
	 * @throws IOException
	 *             if networking fails
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@MustBeClosed
	public Transceiver(MachineVersion version,
			Collection<Connection> connections)
			throws IOException, SpinnmanException, InterruptedException {
		this(version, connections, null, null, null, null, null);
	}

	/**
	 * Given a machine, make a transceiver for talking to all boards of that
	 * machine.
	 *
	 * @param machine
	 *            The machine description
	 * @throws IOException
	 *             if networking fails
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@MustBeClosed
	public Transceiver(Machine machine)
			throws IOException, SpinnmanException, InterruptedException {
		this(requireNonNull(machine, "need a real machine")
				.getBootEthernetAddress(), machine.version, null, null, null,
				null, null, false, generateScampConnections(machine), null,
				null);
		this.machine = machine;
		if (scpSelector instanceof MachineAware) {
			((MachineAware) scpSelector).setMachine(machine);
		}
		log.info("known connections to this transceiver: {}",
				udpScpConnections);
	}

	private static List<ConnectionDescriptor> generateScampConnections(
			Machine machine) {
		return machine.ethernetConnectedChips().stream()
				.map(chip -> new ConnectionDescriptor(chip.ipAddress,
						SCP_SCAMP_PORT, chip.asChipLocation()))
				.collect(toList());
	}

	/**
	 * Create a transceiver.
	 *
	 * @param version
	 *            The type of SpiNNaker board used within the SpiNNaker machine
	 *            being used. May be {@code null} if the board in question can
	 *            be assumed to be always already booted.
	 * @param connections
	 *            The connections to use in the transceiver. Note that the
	 *            transceiver may make additional connections. <em>This should
	 *            be modifiable (or {@code null}) if {@code scampConnections}
	 *            supplied and not empty.</em>
	 * @param ignoredChips
	 *            Blacklisted chips.
	 * @param ignoredCores
	 *            Blacklisted cores.
	 * @param ignoredLinks
	 *            Blacklisted links.
	 * @param scampConnections
	 *            Descriptions of SCP connections to create.
	 * @param maxSDRAMSize
	 *            If not {@code null}, the maximum SDRAM size to allow.
	 * @throws IOException
	 *             if networking fails
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable or SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@SuppressWarnings("MustBeClosed")
	public Transceiver(MachineVersion version,
			Collection<Connection> connections,
			Collection<ChipLocation> ignoredChips,
			Map<ChipLocation, Set<Integer>> ignoredCores,
			Map<ChipLocation, Set<Direction>> ignoredLinks,
			Collection<ConnectionDescriptor> scampConnections,
			Integer maxSDRAMSize)
			throws IOException, SpinnmanException, InterruptedException {
		this.version = version;
		if (ignoredChips != null) {
			ignoreChips.addAll(ignoredChips);
		}
		if (ignoredCores != null) {
			ignoreCores.putAll(ignoredCores);
		}
		if (ignoredLinks != null) {
			ignoreLinks.putAll(ignoredLinks);
		}
		this.maxSDRAMSize = maxSDRAMSize;

		if (connections == null) {
			// Needs to be modifiable
			connections = new ArrayList<>();
		}
		originalConnections.addAll(connections);
		allConnections.addAll(connections);
		// if there has been SCAMP connections given, build them
		if (scampConnections != null) {
			for (ConnectionDescriptor desc : scampConnections) {
				if (desc.portNumber != null
						&& desc.portNumber != SCP_SCAMP_PORT) {
					log.warn("ignoring unexpected SCAMP port: {}",
							desc.portNumber);
				}
				connections.add(createScpConnection(desc.chip, desc.hostname));
			}
		}
		for (Connection conn : connections) {
			identifyConnection(conn);
		}
		scpSelector = makeConnectionSelector();
		checkBMPConnections();
	}

	@Override
	public SCPConnection createScpConnection(ChipLocation chip,
			InetAddress addr) throws IOException {
		return new SCPConnection(chip, addr);
	}

	private ConnectionSelector<SCPConnection> makeConnectionSelector() {
		return new MostDirectConnectionSelector<SCPConnection>(machine,
				scpConnections);
	}

	/**
	 * Work out what is going on with a connection. There are many types of
	 * connections, and some connections are several different things at once.
	 *
	 * @param conn
	 *            The connection to be handled.
	 */
	private void identifyConnection(Connection conn) {
		// locate the only boot send conn
		if (conn instanceof BootConnection) {
			if (bootConnection != null) {
				throw new IllegalArgumentException(
						"Only a single BootSender can be specified");
			}
			bootConnection = (BootConnection) conn;
		}

		// Locate any connections listening on a UDP port
		if (conn instanceof UDPConnection) {
			registerConnection((UDPConnection<?>) conn);
		}

		// Locate any connections that can send SDP
		if (conn instanceof SDPConnection) {
			sdpConnections.add((SDPConnection) conn);
		}

		// Locate any connections that can send and receive SCP
		// If it is a BMP connection, add it here
		if (conn instanceof BMPConnection) {
			var bmpc = (BMPConnection) conn;
			bmpConnections.add(bmpc);
			bmpSelectors.put(bmpc.getCoords(),
					new SingletonConnectionSelector<>(bmpc));
		} else if (conn instanceof SCPConnection) {
			var scpc = (SCPConnection) conn;
			scpConnections.add(scpc);
			udpScpConnections.put(scpc.getRemoteIPAddress(), scpc);
		}
	}

	/**
	 * Get the connections for talking to a board.
	 *
	 * @param boardAddress
	 *            The address of the board to talk to. May be {@code null} to
	 *            use all connections.
	 * @return All the connections that could reach the board.
	 */
	private Collection<SCPConnection> getConnectionList(
			InetAddress boardAddress) {
		if (boardAddress == null) {
			return scpConnections;
		}
		var connection = locateSpinnakerConnection(boardAddress);
		if (connection == null) {
			return List.of();
		}
		return List.of(connection);
	}

	/**
	 * Get the connections for talking to a board.
	 *
	 * @param connection
	 *            Directly gives the connection to use. May be {@code null} to
	 *            use defaults.
	 * @return List of connections that could reach a board.
	 */
	private Collection<SCPConnection> getConnectionList(
			SCPConnection connection) {
		if (connection == null) {
			return scpConnections;
		}
		return List.of(connection);
	}

	@CheckReturnValue
	private Object getSystemVariable(HasChipLocation chip,
			SystemVariableDefinition dataItem)
			throws IOException, ProcessException, InterruptedException {
		var buffer = readMemory(chip, SYS_VARS.add(dataItem.offset),
				dataItem.type.value);
		switch (dataItem.type) {
		case BYTE:
			return Byte.toUnsignedInt(buffer.get());
		case SHORT:
			return Short.toUnsignedInt(buffer.getShort());
		case INT:
			return buffer.getInt();
		case LONG:
			return buffer.getLong();
		case BYTE_ARRAY:
			byte[] dst = (byte[]) dataItem.getDefault();
			buffer.get(dst);
			return dst;
		case ADDRESS:
			return new MemoryLocation(buffer.getInt());
		default:
			// Unreachable
			throw new IllegalStateException();
		}
	}

	private ConnectionSelector<BMPConnection> bmpConnection(BMPCoords bmp) {
		if (!bmpSelectors.containsKey(bmp)) {
			throw new IllegalArgumentException(
					"Unknown combination of cabinet (" + bmp.getCabinet()
							+ ") and frame (" + bmp.getFrame() + ")");
		}
		return bmpSelectors.get(bmp);
	}

	private byte getNextNearestNeighbourID() {
		synchronized (nearestNeighbourLock) {
			int next = (nearestNeighbourID + 1) & NNID_MAX;
			nearestNeighbourID = next;
			return (byte) next;
		}
	}

	@Override
	public ConnectionSelector<SCPConnection> getScampConnectionSelector() {
		return scpSelector;
	}

	/**
	 * Returns the given connection, or else picks one at random.
	 *
	 * @param <C>
	 *            the connection type
	 * @param conn
	 *            the connection to use if it is not {@code null}
	 * @param connections
	 *            the list of connections to locate a random one from
	 * @return a connection object
	 */
	private static <C> C useOrRandomConnection(C conn, List<C> connections) {
		if (conn != null) {
			return conn;
		}
		if (connections.isEmpty()) {
			return null;
		}
		int idx = ThreadLocalRandom.current().nextInt(0, connections.size());
		return connections.get(idx);
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

	@Override
	public void sendSCPMessage(SCPRequest<?> message, SCPConnection connection)
			throws IOException {
		useOrRandomConnection(connection, scpConnections).send(message);
	}

	@Override
	public void sendSDPMessage(SDPMessage message, SDPConnection connection)
			throws IOException {
		useOrRandomConnection(connection, sdpConnections).send(message);
	}

	/**
	 * Get the current machine status and store it.
	 *
	 * @throws IOException
	 *             if the OS has networking troubles
	 * @throws ProcessException
	 *             if SpiNNaker rejects a message
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void updateMachine()
			throws IOException, ProcessException, InterruptedException {
		// Get the width and height of the machine
		getMachineDimensions();

		// Get the coordinates of the boot chip
		var versionInfo = getScampVersion();

		// Get the details of all the chips
		machine = new GetMachineProcess(scpSelector, ignoreChips, ignoreCores,
				ignoreLinks, maxSDRAMSize, this)
						.getMachineDetails(versionInfo.core, dimensions);

		/*
		 * Ask the machine to check itself and if required to rebuild itself
		 * with out invalid links or chips etc.
		 */
		machine = machine.rebuild();

		// update the SCAMP selector with the machine
		if (scpSelector instanceof MachineAware) {
			((MachineAware) scpSelector).setMachine(machine);
		}

		/*
		 * update the SCAMP connections replacing any x and y with the default
		 * SCP request params with the boot chip coordinates
		 */
		for (var sc : scpConnections) {
			if (sc.getChip().equals(BOOT_CHIP)) {
				sc.setChip(machine.boot);
			}
		}

		// Work out and add the SpiNNaker links and FPGA links
		machine.addSpinnakerLinks();
		machine.addFpgaLinks();

		// TODO: Actually get the existing APP_IDs in use
		appIDTracker = new AppIdTracker();

		log.info("Detected a machine on IP address {} which has {}",
				bootConnection.getRemoteIPAddress(),
				machine.coresAndLinkOutputString());
	}

	/**
	 * Find connections to the board and store these for future use. Note that
	 * connections can be empty, in which case another local discovery mechanism
	 * will be used. Note that an exception will be thrown if no initial
	 * connections can be found to the board.
	 *
	 * @return An iterable of discovered connections, not including the
	 *         initially given connections in the constructor
	 * @throws IOException
	 *             if networking fails
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@CheckReturnValue
	@SuppressWarnings("MustBeClosed")
	public List<SCPConnection> discoverScampConnections()
			throws IOException, ProcessException, InterruptedException {
		/*
		 * Currently, this only finds other UDP connections given a connection
		 * that supports SCP - this is done via the machine
		 */
		if (scpConnections.isEmpty()) {
			return List.of();
		}

		// Get the machine dimensions
		var dims = getMachineDimensions();

		// Find all the new connections via the machine Ethernet-connected chips
		var newConnections = new ArrayList<SCPConnection>();
		for (var chip : getSpinn5Geometry().getPotentialRootChips(dims)) {
			var ipAddress = getByAddress(
					(byte[]) getSystemVariable(chip, ethernet_ip_address));
			if (udpScpConnections.containsKey(ipAddress)) {
				continue;
			}
			var conn = searchForProxies(chip);

			// if no data, no proxy
			if (conn == null) {
				conn = createScpConnection(chip, ipAddress);
			} else {
				// proxy, needs an adjustment
				udpScpConnections.remove(conn.getRemoteIPAddress());
			}

			// check if it works
			if (checkConnection(conn, chip)) {
				allConnections.add(conn);
				udpScpConnections.put(ipAddress, conn);
				scpConnections.add(conn);
				newConnections.add(conn);
			} else {
				log.warn(
						"Additional Ethernet connection on {} at "
								+ "chip {},{} cannot be contacted",
						ipAddress, chip.getX(), chip.getY());
			}
		}

		// Update the connection queues after finding new connections
		return newConnections;
	}

	/**
	 * Looks for an entry within the UDP SCAMP connections which is linked to a
	 * given chip.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @return connection or {@code null} if there is no such connection
	 */
	private SCPConnection searchForProxies(ChipLocation chip) {
		for (var connection : scpConnections) {
			if (connection.getChip().equals(chip)) {
				return connection;
			}
		}
		return null;
	}

	/**
	 * Get the currently known connections to the board, made up of those passed
	 * in to the transceiver and those that are discovered during calls to
	 * {@link #discoverScampConnections()}. No further discovery is done here.
	 *
	 * @return The connections known to the transceiver
	 */
	public Set<Connection> getConnections() {
		return unmodifiableSet(allConnections);
	}

	@Override
	public MachineDimensions getMachineDimensions()
			throws IOException, ProcessException, InterruptedException {
		if (dimensions == null) {
			var data = readMemory(BOOT_CHIP, SYS_VARS.add(y_size.offset), 2);
			int height = toUnsignedInt(data.get());
			int width = toUnsignedInt(data.get());
			dimensions = new MachineDimensions(width, height);
		}
		return dimensions;
	}

	@Override
	public Machine getMachineDetails()
			throws IOException, ProcessException, InterruptedException {
		if (machine == null) {
			updateMachine();
		}
		return machine;
	}

	/**
	 * @return the application ID tracker for this transceiver.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	public AppIdTracker getAppIdTracker()
			throws IOException, ProcessException, InterruptedException {
		if (appIDTracker == null) {
			updateMachine();
		}
		return appIDTracker;
	}

	@Override
	public boolean isConnected(Connection connection) {
		if (connection != null) {
			return connectedTest(connection);
		}
		return scpConnections.stream().anyMatch(this::connectedTest);
	}

	private boolean connectedTest(Connection c) {
		try {
			return c.isConnected();
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	@CheckReturnValue
	public VersionInfo getScampVersion(HasChipLocation chip,
			ConnectionSelector<SCPConnection> connectionSelector)
			throws IOException, ProcessException, InterruptedException {
		if (connectionSelector == null) {
			connectionSelector = scpSelector;
		}
		return simpleProcess(connectionSelector)
				.retrieve(new GetVersion(chip.getScampCore()));
	}

	@Override
	@ParallelUnsafe
	public void bootBoard(Map<SystemVariableDefinition, Object> extraBootValues)
			throws InterruptedException, IOException {
		var bootMessages = new BootMessages(version, extraBootValues);
		var msgs = bootMessages.getMessages().iterator();
		while (msgs.hasNext()) {
			bootConnection.sendBootMessage(msgs.next());
		}
		sleep(POST_BOOT_DELAY);
	}

	/**
	 * Determine if the version of SCAMP is compatible with this transceiver.
	 *
	 * @param version
	 *            The version to test
	 * @return true exactly when they are compatible
	 */
	public static boolean isScampVersionCompatible(Version version) {
		// The major version must match exactly
		if (version.majorVersion != SCAMP_VERSION.majorVersion) {
			return false;
		}

		/*
		 * If the minor version matches, the patch version must be >= the
		 * required version
		 */
		if (version.minorVersion == SCAMP_VERSION.minorVersion) {
			return version.revision >= SCAMP_VERSION.revision;
		}

		/*
		 * If the minor version is > than the required version, the patch
		 * version is irrelevant
		 */
		return version.minorVersion > SCAMP_VERSION.minorVersion;
	}

	/**
	 * A neater way of getting a process for running simple SCP requests.
	 *
	 * @return The SCP runner process
	 */
	private TxrxProcess simpleProcess() {
		return new TxrxProcess(scpSelector, this);
	}

	/**
	 * A neater way of getting a process for running simple SCP requests.
	 *
	 * @param selector
	 *            The connection selector to use.
	 * @return The SCP runner process.
	 */
	private TxrxProcess simpleProcess(
			ConnectionSelector<SCPConnection> selector) {
		return new TxrxProcess(selector, this);
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
	 * Do a synchronous call of an SCP operation using the default connection
	 * for a request, sending the given message and completely processing the
	 * interaction before returning its response. This can only properly handle
	 * those calls that involve a single request and a single reply;
	 * fortunately, that's many of them!
	 *
	 * @param <T>
	 *            The type of the response.
	 * @param request
	 *            The request to make.
	 * @return The successful response to the request.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a request.
	 * @throws IOException
	 *             If anything fails with networking.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private <T extends CheckOKResponse> T call(SCPRequest<T> request)
			throws ProcessException, IOException, InterruptedException {
		return new TxrxProcess(scpSelector, this).call(request);
	}

	/**
	 * Do a synchronous call of an SCP operation using the default connection
	 * for a request, sending the given message and completely processing the
	 * interaction before returning its response. This can only properly handle
	 * those calls that involve a single request and a single reply;
	 * fortunately, that's many of them!
	 *
	 * @param <T>
	 *            The type of the payload.
	 * @param <R>
	 *            The type of the response that the payload is extracted from.
	 * @param request
	 *            The request to make.
	 * @return The successful response to the request.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a request.
	 * @throws IOException
	 *             If anything fails with networking.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private <T, R extends PayloadedResponse<T, ?>> T get(SCPRequest<R> request)
			throws ProcessException, IOException, InterruptedException {
		return new TxrxProcess(scpSelector, this).retrieve(request);
	}

	@Override
	@ParallelUnsafe
	public VersionInfo ensureBoardIsReady(int numRetries,
			Map<SystemVariableDefinition, Object> extraBootValues)
			throws IOException, ProcessException, InterruptedException {
		// try to get a SCAMP version once
		log.info("Working out if machine is booted");
		VersionInfo versionInfo;
		if (machineOff) {
			versionInfo = null;
		} else {
			versionInfo = findScampAndBoot(INITIAL_FIND_SCAMP_RETRIES_COUNT,
					extraBootValues);
		}

		// If we fail to get a SCAMP version this time, try other things
		if (versionInfo == null && !bmpConnections.isEmpty()) {
			// start by powering up each BMP connection
			log.info("Attempting to power on machine");
			powerOnMachine();

			// Sleep a bit to let things get going
			sleep(POST_POWER_ON_DELAY);
			log.info("Attempting to boot machine");

			// retry to get a SCAMP version, this time trying multiple times
			versionInfo = findScampAndBoot(numRetries, extraBootValues);
		}

		// verify that the version is the expected one for this transceiver
		if (versionInfo == null) {
			throw new IOException("Failed to communicate with the machine");
		}
		if (!versionInfo.name.equals(SCAMP_NAME)
				|| !isScampVersionCompatible(versionInfo.versionNumber)) {
			throw new IOException(format(
					"The machine is currently booted with %s %s "
							+ "which is incompatible with this transceiver, "
							+ "required version is %s %s",
					versionInfo.name, versionInfo.versionNumber, SCAMP_NAME,
					SCAMP_VERSION));
		}

		log.info("Machine communication successful");

		/*
		 * Change the default SCP timeout on the machine, keeping the old one to
		 * revert at close
		 */
		var process = simpleProcess();
		for (var connection : scpConnections) {
			process.call(
					new IPTagSetTTO(connection.getChip(), TIMEOUT_2560_ms));
		}

		return versionInfo;
	}

	/**
	 * Try to detect if SCAMP is running, and if not, boot the machine.
	 *
	 * @param numAttempts
	 *            how many attempts should be supported
	 * @param extraBootValues
	 *            Any additional values to set during boot
	 * @return version info for SCAMP on the booted system
	 * @throws IOException
	 *             if networking fails
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@CheckReturnValue
	private VersionInfo findScampAndBoot(int numAttempts,
			Map<SystemVariableDefinition, Object> extraBootValues)
			throws InterruptedException, IOException, ProcessException {
		VersionInfo versionInfo = null;
		int triesLeft = numAttempts;
		while (versionInfo == null && triesLeft > 0) {
			try {
				versionInfo = getScampVersion();
				if (versionInfo.core.asChipLocation().equals(BOOT_CHIP)) {
					versionInfo = null;
					sleep(CONNECTION_CHECK_DELAY);
				}
			} catch (ProcessException e) {
				if (e.getCause() instanceof SocketTimeoutException) {
					log.info("Attempting to boot machine");
					bootBoard(extraBootValues);
					triesLeft--;
				} else if (e.getCause() instanceof IOException) {
					throw new IOException(
							"Failed to communicate with the machine", e);
				} else {
					throw e;
				}
			} catch (SocketTimeoutException e) {
				log.info("Attempting to boot machine");
				bootBoard(extraBootValues);
				triesLeft--;
			} catch (IOException e) {
				throw new IOException("Failed to communicate with the machine",
						e);
			}
		}

		// The last thing we tried was booting, so try again to get the version
		if (versionInfo == null) {
			versionInfo = getScampVersion();
			if (versionInfo.core.asChipLocation().equals(BOOT_CHIP)) {
				versionInfo = null;
			}
		}
		if (versionInfo != null) {
			log.info("Found board with hardware {} firmware {} version {}",
					versionInfo.hardware, versionInfo.name,
					versionInfo.versionNumber);
		}
		return versionInfo;
	}

	private CoreSubsets getAllCores()
			throws IOException, ProcessException, InterruptedException {
		if (machine == null) {
			updateMachine();
		}
		var coreSubsets = new CoreSubsets();
		for (var chip : machine.chips()) {
			for (var processor : chip.allProcessors()) {
				coreSubsets.addCore(new CoreLocation(chip.getX(), chip.getY(),
						processor.processorId));
			}
		}
		return coreSubsets;
	}

	@Override
	@CheckReturnValue
	@ParallelSafeWithCare
	public MappableIterable<CPUInfo> getCPUInformation(CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException {
		// Get all the cores if the subsets are not given
		if (coreSubsets == null) {
			coreSubsets = getAllCores();
		}

		return new GetCPUInfoProcess(scpSelector, this).getCPUInfo(coreSubsets);
	}

	@Override
	@CheckReturnValue
	@ParallelSafeWithCare
	public MappableIterable<IOBuffer> getIobuf(CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException {
		// making the assumption that all chips have the same iobuf size.
		if (iobufSize == null) {
			iobufSize = (Integer) getSystemVariable(BOOT_CHIP, iobuf_size);
		}

		// Get all the cores if the subsets are not given
		if (coreSubsets == null) {
			coreSubsets = getAllCores();
		}

		// read iobuf from machine
		return new RuntimeControlProcess(scpSelector, this).readIOBuf(iobufSize,
				coreSubsets);
	}

	@Override
	@ParallelSafeWithCare
	public void clearIobuf(CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException {
		// Get all the cores if the subsets are not given
		if (coreSubsets == null) {
			coreSubsets = getAllCores();
		}

		// read iobuf from machine
		new RuntimeControlProcess(scpSelector, this).clearIOBUF(coreSubsets);
	}

	@Override
	@ParallelSafeWithCare
	public void updateRuntime(Integer runTimesteps, int currentTime,
			int syncTimesteps, CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException {
		// Get all the cores if the subsets are not given
		if (coreSubsets == null) {
			coreSubsets = getAllCores();
		}

		// set the information
		new RuntimeControlProcess(scpSelector, this).updateRuntime(runTimesteps,
				currentTime, syncTimesteps, coreSubsets);
	}

	@Override
	@ParallelSafeWithCare
	public void updateProvenanceAndExit(CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException {
		// Get all the cores if the subsets are not given
		if (coreSubsets == null) {
			coreSubsets = getAllCores();
		}

		// set the information
		new RuntimeControlProcess(scpSelector, this)
				.updateProvenanceAndExit(coreSubsets);
	}

	private static ByteBuffer oneByte(int value) {
		var data = allocate(1);
		data.put((byte) value).flip();
		return data;
	}

	@Override
	@ParallelSafe
	public void setWatchDogTimeoutOnChip(HasChipLocation chip, int watchdog)
			throws IOException, ProcessException, InterruptedException {
		// write data
		writeMemory(chip, SYS_VARS.add(software_watchdog_count.offset),
				oneByte(watchdog));
	}

	@Override
	@ParallelSafe
	public void enableWatchDogTimerOnChip(HasChipLocation chip,
			boolean watchdog)
			throws IOException, ProcessException, InterruptedException {
		// write data
		writeMemory(chip, SYS_VARS.add(software_watchdog_count.offset), oneByte(
				watchdog ? (Integer) software_watchdog_count.getDefault() : 0));
	}

	@Override
	@CheckReturnValue
	@ParallelUnsafe
	public int getCoreStateCount(AppID appID, CPUState state)
			throws IOException, ProcessException, InterruptedException {
		return get(new CountState(appID, state));
	}

	/**
	 * The guardian of the flood lock. Also the lock around that piece of global
	 * state.
	 *
	 * @see ExecuteLock
	 * @author Donal Fellows
	 */
	private static class FloodLock {
		private int count = 0;

		/**
		 * Wait for the system to be ready to perform a flood fill. Must only
		 * ever be called with this object already locked.
		 *
		 * @throws InterruptedException
		 *             If the wait is interrupted.
		 */
		void waitForReady() throws InterruptedException {
			while (count > 0) {
				wait();
			}
		}

		/**
		 * Increment the lock counter. Must only ever be called with this object
		 * already locked.
		 */
		void increment() {
			count++;
		}

		/**
		 * Decrement the lock counter. Must only ever be called with this object
		 * already locked.
		 */
		void decrement() {
			count--;
			notifyAll();
		}
	}

	/**
	 * Helper class that makes lock management for application launch a lot
	 * easier.
	 *
	 * @see FloodLock
	 * @author Donal Fellows
	 */
	private class ExecuteLock implements AutoCloseable {
		private final Semaphore lock;

		/**
		 * Acquire the lock associated with a particular chip.
		 *
		 * @param chip
		 *            The chip we're talking about.
		 * @throws InterruptedException
		 *             If any waits to acquire locks are interrupted.
		 */
		@MustBeClosed
		ExecuteLock(HasChipLocation chip) throws InterruptedException {
			var key = chip.asChipLocation();
			synchronized (executeFloodLock) {
				lock = chipExecuteLocks.computeIfAbsent(key,
						__ -> new Semaphore(1));
			}
			lock.acquire();
			synchronized (executeFloodLock) {
				executeFloodLock.increment();
			}
		}

		/**
		 * Release the lock associated with a particular chip.
		 */
		@Override
		public void close() {
			synchronized (executeFloodLock) {
				lock.release();
				executeFloodLock.decrement();
			}
		}
	}

	@Override
	@ParallelSafe
	public void execute(HasChipLocation chip, Collection<Integer> processors,
			InputStream executable, int numBytes, AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		// Lock against updates
		try (var lock = new ExecuteLock(chip)) {
			// Write the executable
			writeMemory(chip, EXECUTABLE_ADDRESS, executable, numBytes);

			// Request the start of the executable
			call(new ApplicationRun(appID, chip, processors, wait));
		}
	}

	@Override
	@ParallelSafe
	public final void execute(HasChipLocation chip,
			Collection<Integer> processors, File executable, AppID appID,
			boolean wait)
			throws IOException, ProcessException, InterruptedException {
		// Lock against updates
		try (var lock = new ExecuteLock(chip)) {
			// Write the executable
			writeMemory(chip, EXECUTABLE_ADDRESS, executable);

			// Request the start of the executable
			call(new ApplicationRun(appID, chip, processors, wait));
		}
	}

	@Override
	@ParallelSafe
	public void execute(HasChipLocation chip, Collection<Integer> processors,
			ByteBuffer executable, AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		// Lock against updates
		try (var lock = new ExecuteLock(chip)) {
			// Write the executable
			writeMemory(chip, EXECUTABLE_ADDRESS, executable);

			// Request the start of the executable
			call(new ApplicationRun(appID, chip, processors, wait));
		}
	}

	@Override
	@ParallelSafeWithCare
	public void executeFlood(CoreSubsets coreSubsets, InputStream executable,
			int numBytes, AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		// Lock against other executables
		synchronized (executeFloodLock) {
			executeFloodLock.waitForReady();

			// Flood fill the system with the binary
			writeMemoryFlood(EXECUTABLE_ADDRESS, executable, numBytes);

			// Execute the binary on the cores on the chips where required
			new ApplicationRunProcess(scpSelector, this).run(appID, coreSubsets,
					wait);
		}
	}

	@Override
	@ParallelSafeWithCare
	public void executeFlood(CoreSubsets coreSubsets, File executable,
			AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		// Lock against other executables
		synchronized (executeFloodLock) {
			executeFloodLock.waitForReady();

			// Flood fill the system with the binary
			writeMemoryFlood(EXECUTABLE_ADDRESS, executable);

			// Execute the binary on the cores on the chips where required
			new ApplicationRunProcess(scpSelector, this).run(appID, coreSubsets,
					wait);
		}
	}

	@Override
	@ParallelSafeWithCare
	public void executeFlood(CoreSubsets coreSubsets, ByteBuffer executable,
			AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		// Lock against other executables
		synchronized (executeFloodLock) {
			executeFloodLock.waitForReady();

			// Flood fill the system with the binary
			writeMemoryFlood(EXECUTABLE_ADDRESS, executable);

			// Execute the binary on the cores on the chips where required
			new ApplicationRunProcess(scpSelector, this).run(appID, coreSubsets,
					wait);
		}
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
			@NotNull MemoryLocation baseAddress, @NotNull ByteBuffer data,
			boolean update)
			throws ProcessException, IOException, InterruptedException {
		if (!data.hasRemaining()) {
			// Zero length write?
			log.warn("zero length write to flash ignored");
			return;
		}

		int size = data.remaining();
		var workingBuffer =
				get(bmp, new EraseFlash(board, baseAddress, size));
		var targetAddr = baseAddress;
		for (var buf : sliceUp(data, FLASH_CHUNK_SIZE)) {
			writeBMPMemory(bmp, board, workingBuffer, buf);
			call(bmp, new WriteFlashBuffer(board, targetAddr, false));
			targetAddr = targetAddr.add(FLASH_CHUNK_SIZE);
		}

		if (update) {
			call(bmp, (int) (MSEC_PER_SEC * BMP_TIMEOUT), 0,
					new UpdateFlash(board, baseAddress, size));
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
		return get(bmp, new ReadCANStatus()).map(BMPBoard::new);
	}

	private WriteMemoryProcess writeProcess(long size) {
		if (size > LARGE_DATA_WRITE_THRESHOLD) {
			/*
			 * If there's more than a (tunable) threshold of data to move, we
			 * limit the number of messages in flight when doing uploads so that
			 * we don't overload SCAMP. Overloading SCAMP *really* slows things
			 * down!
			 */
			return new WriteMemoryProcess(scpSelector,
					LARGE_WRITE_PARALLEL_MESSAGE_COUNT, this);
		}
		return new WriteMemoryProcess(scpSelector, this);
	}

	@Override
	@ParallelSafe
	public void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, ProcessException, InterruptedException {
		writeProcess(numBytes).writeMemory(core, baseAddress, dataStream,
				numBytes);
	}

	@Override
	@ParallelSafe
	public void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			File dataFile)
			throws IOException, ProcessException, InterruptedException {
		writeProcess(dataFile.length()).writeMemory(core, baseAddress,
				dataFile);
	}

	@Override
	@ParallelSafe
	public void writeMemory(HasCoreLocation core, MemoryLocation baseAddress,
			ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		writeProcess(data.remaining()).writeMemory(core, baseAddress, data);
	}

	@Override
	@ParallelUnsafe
	public void writeNeighbourMemory(HasCoreLocation core, Direction link,
			MemoryLocation baseAddress, InputStream dataStream, int numBytes)
			throws IOException, ProcessException, InterruptedException {
		writeProcess(numBytes).writeLink(core, link, baseAddress, dataStream,
				numBytes);
	}

	@Override
	@ParallelUnsafe
	public void writeNeighbourMemory(HasCoreLocation core, Direction link,
			MemoryLocation baseAddress, File dataFile)
			throws IOException, ProcessException, InterruptedException {
		writeProcess(dataFile.length()).writeLink(core, link, baseAddress,
				dataFile);
	}

	@Override
	@ParallelUnsafe
	public void writeNeighbourMemory(HasCoreLocation core, Direction link,
			MemoryLocation baseAddress, ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		writeProcess(data.remaining()).writeLink(core, link, baseAddress, data);
	}

	@Override
	@ParallelUnsafe
	public void writeMemoryFlood(MemoryLocation baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, ProcessException, InterruptedException {
		var process = new WriteMemoryFloodProcess(scpSelector, this);
		// Ensure only one flood fill occurs at any one time
		synchronized (floodWriteLock) {
			// Start the flood fill
			process.writeMemory(getNextNearestNeighbourID(), baseAddress,
					dataStream, numBytes);
		}
	}

	@Override
	@ParallelUnsafe
	public void writeMemoryFlood(MemoryLocation baseAddress, File dataFile)
			throws IOException, ProcessException, InterruptedException {
		var process = new WriteMemoryFloodProcess(scpSelector, this);
		// Ensure only one flood fill occurs at any one time
		synchronized (floodWriteLock) {
			// Start the flood fill
			process.writeMemory(getNextNearestNeighbourID(), baseAddress,
					dataFile);
		}
	}

	@Override
	@ParallelUnsafe
	public void writeMemoryFlood(MemoryLocation baseAddress, ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		var process = new WriteMemoryFloodProcess(scpSelector, this);
		// Ensure only one flood fill occurs at any one time
		synchronized (floodWriteLock) {
			// Start the flood fill
			process.writeMemory(getNextNearestNeighbourID(), baseAddress, data);
		}
	}

	@Override
	@CheckReturnValue
	@ParallelSafe
	public ByteBuffer readMemory(HasCoreLocation core,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException, InterruptedException {
		return new ReadMemoryProcess(scpSelector, this).readMemory(core,
				baseAddress, length);
	}

	@Override
	@ParallelSafe
	public void readRegion(BufferManagerStorage.Region region,
			BufferManagerStorage storage) throws IOException, ProcessException,
			StorageException, InterruptedException {
		new ReadMemoryProcess(scpSelector, this).readMemory(region, storage);
	}

	@Override
	@CheckReturnValue
	@ParallelUnsafe
	public ByteBuffer readNeighbourMemory(HasCoreLocation core, Direction link,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException, InterruptedException {
		return new ReadMemoryProcess(scpSelector, this).readLink(core, link,
				baseAddress, length);
	}

	@Override
	@ParallelUnsafe
	public void stopApplication(AppID appID)
			throws IOException, ProcessException, InterruptedException {
		if (machineOff) {
			log.warn("You are calling a app stop on a turned off machine. "
					+ "Please fix and try again");
			return;
		}
		call(new ApplicationStop(appID));
	}

	@CheckReturnValue
	private boolean inErrorStates(AppID appID, Set<CPUState> errorStates)
			throws IOException, ProcessException, InterruptedException {
		for (var state : errorStates) {
			if (getCoreStateCount(appID, state) > 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	@ParallelSafeWithCare
	public void waitForCoresToBeInState(CoreSubsets allCoreSubsets, AppID appID,
			Set<CPUState> cpuStates, Integer timeout, int timeBetweenPolls,
			Set<CPUState> errorStates, int countBetweenFullChecks)
			throws IOException, InterruptedException, SpinnmanException {
		// check that the right number of processors are in the states
		int processorsReady = 0;
		long timeoutTime =
				currentTimeMillis() + (timeout == null ? 0 : timeout);
		int tries = 0;
		while (processorsReady < allCoreSubsets.size()
				&& (timeout == null || currentTimeMillis() < timeoutTime)) {
			// Get the number of processors in the ready states
			processorsReady = 0;
			for (var state : cpuStates) {
				processorsReady += getCoreStateCount(appID, state);
			}

			// If the count is too small, check for error states
			if (processorsReady < allCoreSubsets.size()) {
				if (inErrorStates(appID, errorStates)) {
					// Small chance that inErrorStates() is wrong
					var errorCores =
							getCoresInState(allCoreSubsets, errorStates);
					if (!errorCores.isEmpty()) {
						throw new CoresNotInStateException(timeout, cpuStates,
								errorCores);
					}
				}

				/*
				 * If we haven't seen an error, increase the tries, and do a
				 * full check if required
				 */
				if (++tries >= countBetweenFullChecks) {
					var coresInState =
							getCoresInState(allCoreSubsets, cpuStates);
					processorsReady = coresInState.size();
					tries = 0;
				}

				// If we're still not in the correct state, wait a bit
				if (processorsReady < allCoreSubsets.size()) {
					sleep(timeBetweenPolls);
				}
			}
		}

		// If we haven't reached the final state, do a final full check
		if (processorsReady < allCoreSubsets.size()) {
			var coresInState = getCoresInState(allCoreSubsets, cpuStates);

			/*
			 * If we are sure we haven't reached the final state, report a
			 * timeout error
			 */
			if (coresInState.size() != allCoreSubsets.size()) {
				throw new SocketTimeoutException(
						format("waiting for cores %s to reach one of %s",
								allCoreSubsets, cpuStates));
			}
		}
	}

	@Override
	@ParallelUnsafe
	public void sendSignal(AppID appID, Signal signal)
			throws IOException, ProcessException, InterruptedException {
		call(new SendSignal(appID, signal));
	}

	@Override
	@ParallelSafe
	public void setLEDs(HasCoreLocation core, Map<Integer, LEDAction> ledStates)
			throws IOException, ProcessException, InterruptedException {
		call(new SetLED(core, ledStates));
	}

	@Override
	@ParallelSafe
	public SCPConnection locateSpinnakerConnection(InetAddress boardAddress) {
		return udpScpConnections.get(boardAddress);
	}

	@Override
	@ParallelSafeWithCare
	public void setIPTag(IPTag tag)
			throws IOException, ProcessException, InterruptedException {
		// Check that the tag has a port assigned
		if (tag.getPort() == null) {
			throw new IllegalArgumentException(
					"The tag port must have been set");
		}

		/*
		 * Get the connections. If the tag specifies a connection, use that,
		 * otherwise apply the tag to all connections
		 */
		var connections = getConnectionList(tag.getBoardAddress());
		if (connections == null || connections.isEmpty()) {
			throw new IllegalArgumentException(
					"The given board address is not recognised");
		}

		var process = simpleProcess();
		for (var connection : connections) {
			// Convert the host string
			var host = tag.getIPAddress();
			if (host == null || host.isAnyLocalAddress()
					|| host.isLoopbackAddress()) {
				host = connection.getLocalIPAddress();
			}
			process.call(new IPTagSet(connection.getChip(),
					host.getAddress(), tag.getPort(), tag.getTag(),
					tag.isStripSDP(), false));
		}
	}

	@Override
	@ParallelSafeWithCare
	public void setIPTag(IPTag tag, SDPConnection connection)
			throws IOException, ProcessException, InterruptedException {
		/*
		 * Check that the connection is actually pointing to somewhere we know.
		 */
		var connections = getConnectionList(connection.getRemoteIPAddress());
		if (connections == null || connections.isEmpty()) {
			throw new IllegalArgumentException(
					"The given board address is not recognised");
		}

		var process = simpleProcess(connection);
		process.call(new IPTagSet(connection.getChip(), null, 0,
					tag.getTag(), tag.isStripSDP(), true));
	}

	@Override
	@ParallelSafeWithCare
	public void setReverseIPTag(ReverseIPTag tag)
			throws IOException, ProcessException, InterruptedException {
		if (requireNonNull(tag).getPort() == SCP_SCAMP_PORT
				|| tag.getPort() == UDP_BOOT_CONNECTION_DEFAULT_PORT) {
			throw new IllegalArgumentException(format(
					"The port number for the reverse IP tag conflicts with"
							+ " the SpiNNaker system ports (%d and %d)",
					SCP_SCAMP_PORT, UDP_BOOT_CONNECTION_DEFAULT_PORT));
		}

		/*
		 * Get the connections. If the tag specifies a connection, use that,
		 * otherwise apply the tag to all connections
		 */
		var connections = getConnectionList(tag.getBoardAddress());
		if (connections == null || connections.isEmpty()) {
			throw new IllegalArgumentException(
					"The given board address is not recognised");
		}

		var process = simpleProcess();
		for (var connection : connections) {
			process.call(new ReverseIPTagSet(connection.getChip(),
					tag.getDestination(), tag.getPort(), tag.getTag(),
					tag.getPort()));
		}
	}

	@Override
	@ParallelSafeWithCare
	public void clearIPTag(int tag, InetAddress boardAddress)
			throws IOException, ProcessException, InterruptedException {
		var process = simpleProcess();
		for (var conn : getConnectionList(boardAddress)) {
			process.call(new IPTagClear(conn.getChip(), tag));
		}
	}

	@Override
	@CheckReturnValue
	@ParallelSafeWithCare
	public List<Tag> getTags(SCPConnection connection)
			throws IOException, ProcessException, InterruptedException {
		var allTags = new ArrayList<Tag>();
		var process = new GetTagsProcess(scpSelector, this);
		for (var conn : getConnectionList(connection)) {
			allTags.addAll(process.getTags(conn));
		}
		return allTags;
	}

	@Override
	@CheckReturnValue
	@ParallelSafeWithCare
	public Map<Tag, Integer> getTagUsage(SCPConnection connection)
			throws IOException, ProcessException, InterruptedException {
		var allUsage = new HashMap<Tag, Integer>();
		var process = new GetTagsProcess(scpSelector, this);
		for (var conn : getConnectionList(connection)) {
			allUsage.putAll(process.getTagUsage(conn));
		}
		return allUsage;
	}

	@Override
	@CheckReturnValue
	@ParallelSafe
	public MemoryLocation mallocSDRAM(HasChipLocation chip, int size,
			AppID appID, int tag)
			throws IOException, ProcessException, InterruptedException {
		return get(new SDRAMAlloc(chip, appID, size, tag));
	}

	@Override
	@ParallelSafe
	public void freeSDRAM(HasChipLocation chip, MemoryLocation baseAddress)
			throws IOException, ProcessException, InterruptedException {
		call(new SDRAMDeAlloc(chip, baseAddress));
	}

	@Override
	@ParallelSafe
	public int freeSDRAM(HasChipLocation chip, AppID appID)
			throws IOException, ProcessException, InterruptedException {
		return get(new SDRAMDeAlloc(chip, appID));
	}

	@Override
	@ParallelSafe
	public void loadMulticastRoutes(HasChipLocation chip,
			Collection<MulticastRoutingEntry> routes, AppID appID)
			throws IOException, ProcessException, InterruptedException {
		new MulticastRoutesControlProcess(scpSelector, this).setRoutes(chip,
				routes, appID);
	}

	@Override
	@ParallelSafe
	public void loadFixedRoute(HasChipLocation chip, RoutingEntry fixedRoute,
			AppID appID)
			throws IOException, ProcessException, InterruptedException {
		new FixedRouteControlProcess(scpSelector, this).loadFixedRoute(chip,
				fixedRoute, appID);
	}

	@Override
	@CheckReturnValue
	@ParallelSafe
	public RoutingEntry readFixedRoute(HasChipLocation chip, AppID appID)
			throws IOException, ProcessException, InterruptedException {
		return new FixedRouteControlProcess(scpSelector, this)
				.readFixedRoute(chip, appID);
	}

	@Override
	@CheckReturnValue
	@ParallelSafe
	public List<MulticastRoutingEntry> getMulticastRoutes(HasChipLocation chip,
			AppID appID)
			throws IOException, ProcessException, InterruptedException {
		var address = (MemoryLocation) getSystemVariable(chip,
				router_table_copy_address);
		return new MulticastRoutesControlProcess(scpSelector, this)
				.getRoutes(chip, address, appID);
	}

	@Override
	@ParallelSafe
	public void clearMulticastRoutes(HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException {
		call(new RouterClear(chip));
	}

	@Override
	@CheckReturnValue
	@ParallelSafe
	public RouterDiagnostics getRouterDiagnostics(HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException {
		return new RouterControlProcess(scpSelector, this)
				.getRouterDiagnostics(chip);
	}

	@Override
	@ParallelSafe
	public void setRouterDiagnosticFilter(HasChipLocation chip, int position,
			DiagnosticFilter diagnosticFilter)
			throws IOException, ProcessException, InterruptedException {
		if (position < 0 || position > NO_ROUTER_DIAGNOSTIC_FILTERS) {
			throw new IllegalArgumentException(
					"router filter positions must be between 0 and "
							+ NO_ROUTER_DIAGNOSTIC_FILTERS);
		}
		if (position <= ROUTER_DEFAULT_FILTERS_MAX_POSITION) {
			log.warn("You are planning to change a filter which is set by "
					+ "default. By doing this, other runs occurring on this "
					+ "machine will be forced to use this new configuration "
					+ "until the machine is reset. Please also note that "
					+ "these changes will make the the reports from ybug not "
					+ "correct. This has been executed and is trusted that "
					+ "the end user knows what they are doing.");
		}

		var address =
				ROUTER_FILTERS.add(position * ROUTER_DIAGNOSTIC_FILTER_SIZE);
		writeMemory(chip, address, diagnosticFilter.getFilterWord());
	}

	@Override
	@ParallelSafe
	public DiagnosticFilter getRouterDiagnosticFilter(HasChipLocation chip,
			int position)
			throws IOException, ProcessException, InterruptedException {
		if (position < 0 || position > NO_ROUTER_DIAGNOSTIC_FILTERS) {
			throw new IllegalArgumentException(
					"router filter positions must be between 0 and "
							+ NO_ROUTER_DIAGNOSTIC_FILTERS);
		}
		var address =
				ROUTER_FILTERS.add(position * ROUTER_DIAGNOSTIC_FILTER_SIZE);
		var response = get(new ReadMemory(chip, address, WORD_SIZE));
		return new DiagnosticFilter(response.getInt());
	}

	@Override
	@ParallelSafe
	public void clearRouterDiagnosticCounters(HasChipLocation chip,
			boolean enable, Iterable<Integer> counterIDs)
			throws IOException, ProcessException, InterruptedException {
		int clearData = 0;
		for (int counterID : requireNonNull(counterIDs)) {
			if (counterID < 0 || counterID >= NUM_ROUTER_DIAGNOSTIC_COUNTERS) {
				throw new IllegalArgumentException(
						"Diagnostic counter IDs must be between 0 and 15");
			}
			clearData |= 1 << counterID;
		}
		if (enable) {
			for (int counterID : counterIDs) {
				clearData |= 1 << (counterID + ENABLE_SHIFT);
			}
		}
		writeMemory(chip, ROUTER_DIAGNOSTIC_COUNTER, clearData);
	}

	@Override
	@ParallelSafe
	public void clearReinjectionQueues(HasCoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this)
				.clearQueue(monitorCore.asCoreLocation());
	}

	@Override
	@ParallelSafe
	public void clearReinjectionQueues(CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this).clearQueue(monitorCores);
	}

	@Override
	@ParallelSafe
	public ReinjectionStatus getReinjectionStatus(HasCoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException {
		return new RouterControlProcess(scpSelector, this)
				.getReinjectionStatus(monitorCore.asCoreLocation());
	}

	@Override
	@ParallelSafe
	public Map<CoreLocation, ReinjectionStatus> getReinjectionStatus(
			CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException {
		return new RouterControlProcess(scpSelector, this)
				.getReinjectionStatus(monitorCores);
	}

	@Override
	@ParallelSafe
	public void resetReinjectionCounters(HasCoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this)
				.resetCounters(monitorCore.asCoreLocation());
	}

	@Override
	@ParallelSafeWithCare
	public void resetReinjectionCounters(CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this).resetCounters(monitorCores);
	}

	@Override
	@ParallelSafe
	public void setReinjectionTypes(HasCoreLocation monitorCore,
			boolean multicast, boolean pointToPoint, boolean fixedRoute,
			boolean nearestNeighbour)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this).setPacketTypes(
				monitorCore.asCoreLocation(), multicast, pointToPoint,
				fixedRoute, nearestNeighbour);
	}

	@Override
	@ParallelSafeWithCare
	public void setReinjectionTypes(CoreSubsets monitorCores, boolean multicast,
			boolean pointToPoint, boolean fixedRoute, boolean nearestNeighbour)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this).setPacketTypes(monitorCores,
				multicast, pointToPoint, fixedRoute, nearestNeighbour);
	}

	@Override
	@ParallelSafe
	public void setReinjectionEmergencyTimeout(HasCoreLocation monitorCore,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this).setEmergencyTimeout(
				monitorCore.asCoreLocation(), timeoutMantissa, timeoutExponent);
	}

	@Override
	@ParallelSafeWithCare
	public void setReinjectionEmergencyTimeout(CoreSubsets monitorCores,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this).setEmergencyTimeout(
				monitorCores, timeoutMantissa, timeoutExponent);
	}

	@Override
	@ParallelSafe
	public void setReinjectionTimeout(HasCoreLocation monitorCore,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this).setTimeout(
				monitorCore.asCoreLocation(), timeoutMantissa, timeoutExponent);
	}

	@Override
	@ParallelSafeWithCare
	public void setReinjectionTimeout(CoreSubsets monitorCores,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this).setTimeout(monitorCores,
				timeoutMantissa, timeoutExponent);
	}

	@Override
	@CheckReturnValue
	@ParallelSafe
	public List<HeapElement> getHeap(HasChipLocation chip,
			SystemVariableDefinition heap)
			throws IOException, ProcessException, InterruptedException {
		return new GetHeapProcess(scpSelector, this).getBlocks(chip, heap);
	}

	@Override
	@ParallelSafe
	public void fillMemory(HasChipLocation chip, MemoryLocation baseAddress,
			int repeatValue, int size, FillDataType dataType)
			throws ProcessException, IOException, InterruptedException {
		if (repeatValue < 1) {
			throw new IllegalArgumentException("the repeat must be at least 1");
		}
		new FillProcess(scpSelector, this).fillMemory(chip, baseAddress,
				repeatValue, size, dataType);
	}

	@Override
	@ParallelSafeWithCare
	public void saveApplicationRouterTables(CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this)
				.saveApplicationRouterTable(monitorCores);
	}

	@Override
	@ParallelSafeWithCare
	public void loadApplicationRouterTables(CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this)
				.loadApplicationRouterTable(monitorCores);
	}

	@Override
	@ParallelSafeWithCare
	public void loadSystemRouterTables(CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException {
		new RouterControlProcess(scpSelector, this)
				.loadSystemRouterTable(monitorCores);
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
			close(true, false);
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
	public void close(boolean closeOriginalConnections, boolean powerOffMachine)
			throws IOException, InterruptedException {
		if (powerOffMachine && !bmpConnections.isEmpty()) {
			try {
				powerOffMachine();
			} catch (ProcessException e) {
				log.warn("failed to power off machine", e);
			}
		}

		super.close();

		for (var connection : allConnections) {
			if (closeOriginalConnections
					|| !originalConnections.contains(connection)) {
				connection.close();
			}
		}

		log.info("total retries used: {}", retryCount);
	}

	/**
	 * @return The connection selectors used for BMP connections.
	 */
	public Map<BMPCoords,
			ConnectionSelector<BMPConnection>> getBMPConnection() {
		return bmpSelectors;
	}

	/**
	 * A simple description of a connnection to create.
	 */
	public static final class ConnectionDescriptor {
		/** What host to talk to. */
		private InetAddress hostname;

		/** What port to talk to, or {@code null} for default. */
		private Integer portNumber;

		/** What chip to talk to. */
		private ChipLocation chip;

		/**
		 * Create a connection descriptor.
		 *
		 * @param hostname
		 *            The host to talk to. The default UDP port will be used.
		 * @param chip
		 *            The chip to talk to.
		 */
		public ConnectionDescriptor(InetAddress hostname,
				HasChipLocation chip) {
			this.hostname = requireNonNull(hostname);
			this.chip = chip.asChipLocation();
			this.portNumber = null;
		}

		/**
		 * Create a connection descriptor.
		 *
		 * @param host
		 *            The host to talk to.
		 * @param port
		 *            The UDP port to talk to.
		 * @param chip
		 *            The chip to talk to.
		 */
		public ConnectionDescriptor(InetAddress host, int port,
				HasChipLocation chip) {
			this.hostname = requireNonNull(host);
			this.chip = chip.asChipLocation();
			this.portNumber = port;
		}
	}

	@Override
	protected void addConnection(Connection connection) {
		this.allConnections.add(connection);
	}

	@Override
	public void bind(BMPCoords bmp) {
		boundBMP = bmp;
	}

	@Override
	public BMPCoords getBoundBMP() {
		return boundBMP;
	}
}
