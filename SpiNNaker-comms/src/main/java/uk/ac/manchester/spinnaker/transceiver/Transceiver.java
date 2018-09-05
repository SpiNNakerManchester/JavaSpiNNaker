package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.net.InetAddress.getByAddress;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.SpiNNakerTriadGeometry.getSpinn5Geometry;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_POST_POWER_ON_SLEEP_TIME;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_POWER_ON_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_TIMEOUT;
import static uk.ac.manchester.spinnaker.messages.Constants.MS_PER_S;
import static uk.ac.manchester.spinnaker.messages.Constants.NO_ROUTER_DIAGNOSTIC_FILTERS;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_DEFAULT_FILTERS_MAX_POSITION;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_DIAGNOSTIC_FILTER_SIZE;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_FILTER_CONTROLS_OFFSET;
import static uk.ac.manchester.spinnaker.messages.Constants.ROUTER_REGISTER_BASE_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;
import static uk.ac.manchester.spinnaker.messages.Constants.SYSTEM_VARIABLE_BASE_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.UDP_BOOT_CONNECTION_DEFAULT_PORT;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime.TIMEOUT_2560_ms;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.ethernet_ip_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.router_table_copy_address;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.software_watchdog_count;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.y_size;
import static uk.ac.manchester.spinnaker.transceiver.Utils.workOutBMPFromMachineDetails;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import static java.util.Collections.emptyMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.connections.BMPConnection;
import uk.ac.manchester.spinnaker.connections.BootConnection;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SDPConnection;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.connections.model.BootReceiver;
import uk.ac.manchester.spinnaker.connections.model.BootSender;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.connections.model.MulticastSender;
import uk.ac.manchester.spinnaker.connections.model.SCPReceiver;
import uk.ac.manchester.spinnaker.connections.model.SCPSender;
import uk.ac.manchester.spinnaker.connections.model.SDPSender;
import uk.ac.manchester.spinnaker.connections.selectors.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.selectors.MachineAware;
import uk.ac.manchester.spinnaker.connections.selectors.MostDirectConnectionSelector;
import uk.ac.manchester.spinnaker.connections.selectors.RoundRobinConnectionSelector;
import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.NUM_ROUTER_DIAGNOSTIC_COUNTERS;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.machine.Processor;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.bmp.BMPSetLED;
import uk.ac.manchester.spinnaker.messages.bmp.GetBMPVersion;
import uk.ac.manchester.spinnaker.messages.bmp.ReadADC;
import uk.ac.manchester.spinnaker.messages.bmp.ReadFPGARegister;
import uk.ac.manchester.spinnaker.messages.bmp.SetPower;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFPGARegister;
import uk.ac.manchester.spinnaker.messages.boot.BootMessage;
import uk.ac.manchester.spinnaker.messages.boot.BootMessages;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.ChipSummaryInfo;
import uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.IOBuffer;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.model.Signal;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.ApplicationRun;
import uk.ac.manchester.spinnaker.messages.scp.ApplicationStop;
import uk.ac.manchester.spinnaker.messages.scp.CountState;
import uk.ac.manchester.spinnaker.messages.scp.GetChipInfo;
import uk.ac.manchester.spinnaker.messages.scp.IPTagClear;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSet;
import uk.ac.manchester.spinnaker.messages.scp.IPTagSetTTO;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory.Response;
import uk.ac.manchester.spinnaker.messages.scp.ReverseIPTagSet;
import uk.ac.manchester.spinnaker.messages.scp.RouterClear;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.scp.SendSignal;
import uk.ac.manchester.spinnaker.messages.scp.SetLED;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.processes.ApplicationRunProcess;
import uk.ac.manchester.spinnaker.processes.DeallocSDRAMProcess;
import uk.ac.manchester.spinnaker.processes.FillProcess;
import uk.ac.manchester.spinnaker.processes.FillProcess.DataType;
import uk.ac.manchester.spinnaker.processes.GetCPUInfoProcess;
import uk.ac.manchester.spinnaker.processes.GetHeapProcess;
import uk.ac.manchester.spinnaker.processes.GetMachineProcess;
import uk.ac.manchester.spinnaker.processes.GetMulticastRoutesProcess;
import uk.ac.manchester.spinnaker.processes.GetTagsProcess;
import uk.ac.manchester.spinnaker.processes.GetVersionProcess;
import uk.ac.manchester.spinnaker.processes.LoadFixedRouteEntryProcess;
import uk.ac.manchester.spinnaker.processes.LoadMulticastRoutesProcess;
import uk.ac.manchester.spinnaker.processes.MallocSDRAMProcess;
import uk.ac.manchester.spinnaker.processes.Process.Exception;
import uk.ac.manchester.spinnaker.processes.ReadFixedRouteEntryProcess;
import uk.ac.manchester.spinnaker.processes.ReadIOBufProcess;
import uk.ac.manchester.spinnaker.processes.ReadMemoryProcess;
import uk.ac.manchester.spinnaker.processes.ReadRouterDiagnosticsProcess;
import uk.ac.manchester.spinnaker.processes.SendSingleBMPCommandProcess;
import uk.ac.manchester.spinnaker.processes.SendSingleSCPCommandProcess;
import uk.ac.manchester.spinnaker.processes.WriteMemoryFloodProcess;
import uk.ac.manchester.spinnaker.processes.WriteMemoryProcess;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

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
 */
@SuppressWarnings("checkstyle:ParameterNumber")
public class Transceiver extends UDPTransceiver
		implements TransceiverInterface {
	private static final int BIGGER_BOARD = 4;
	private static final Logger log = getLogger(Transceiver.class);
	/** The version of the board being connected to. */
	private int version;
	/** The discovered machine model. */
	Machine machine;
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
	private final DefaultMap<ChipLocation, Collection<Integer>> ignoreCores
                =  new DefaultMap<>(new HashSet<>());

	/**
	 * A set of links to ignore in the machine. Requests for a "machine" will
	 * have these links excluded, as if they never existed.
	 */
	private final DefaultMap<ChipLocation, Collection<Direction>>
                ignoreLinks = new DefaultMap<>(new HashSet<>());

	/**
	 * The maximum core ID in any discovered machine. Requests for a "machine"
	 * will only have core IDs up to and including this value.
	 */
	private final Integer maxCoreID;
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
	private BootSender bootSendConnection;
	/**
	 * A list of boot receive connections. These are used to listen for the
	 * pre-boot board identifiers.
	 */
	private final List<BootReceiver> bootReceiveConnections = new ArrayList<>();
	/**
	 * A list of all connections that can be used to send SCP messages.
	 * <p>
	 * Note that some of these might not be able to receive SCP; this could be
	 * useful if they are just using SCP to send a command that doesn't expect a
	 * response.
	 */
	private final List<SCPSender> scpSenderConnections = new ArrayList<>();
	/** A list of all connections that can be used to send SDP messages. */
	private final List<SDPSender> sdpSenderConnections = new ArrayList<>();
	/**
	 * A list of all connections that can be used to send Multicast messages.
	 */
	private final List<MulticastSender> multicastSenderConnections =
			new ArrayList<>();
	/**
	 * A map of IP address -> SCAMP connection. These are those that can be used
	 * for setting up IP Tags.
	 */
	private final Map<InetAddress, SCPConnection> udpScampConnections =
			new HashMap<>();
	/**
	 * A list of all connections that can be used to send and receive SCP
	 * messages for SCAMP interaction.
	 */
	private final List<SCPConnection> scampConnections = new ArrayList<>();
	/** The BMP connections. */
	private final List<BMPConnection> bmpConnections = new ArrayList<>();
	/** Connection selectors for the BMP processes. */
	private final Map<BMPCoords,
				ConnectionSelector<BMPConnection>> bmpSelectors =
			new HashMap<>();
	/** Connection selectors for the SCP processes. */
	private final ConnectionSelector<SCPConnection> scpSelector;
	/** The nearest neighbour start ID. */
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
			new DefaultMap<>(() -> new Semaphore(1));
	private final Object chipExecuteLockCondition = new Object();
	private int numChipExecuteLocks = 0;
	private boolean machineOff = false;

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
	 * @param numberOfBoards
	 *            a number of boards expected to be supported, or <tt>null</tt>,
	 *            which defaults to a single board
	 * @param ignoreChips
	 *            An optional set of chips to ignore in the machine. Requests
	 *            for a "machine" will have these chips excluded, as if they
	 *            never existed. The processors of the specified chips are
	 *            ignored.
	 * @param ignoreCores
	 *            An optional map of cores to ignore in the machine.
         *            Requests for a "machine" will have these cores excluded,
         *            as if they never existed.
	 * @param ignoredLinks
	 *            An optional set of links to ignore in the machine. Requests
	 *            for a "machine" will have these links excluded, as if they
	 *            never existed.
	 * @param maxCoreID
	 *            The maximum core ID in any discovered machine. Requests for a
	 *            "machine" will only have core IDs up to this value.
	 * @param version
	 *            the type of SpiNNaker board used within the SpiNNaker machine
	 *            being used. If a spinn-5 board, then the version will be 5,
	 *            spinn-3 would equal 3 and so on.
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
	 * @return The created transceiver
	 * @throws IOException
	 *             if networking fails
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable.
	 */
	public static Transceiver createTransceiver(InetAddress host,
			int version, Collection<BMPConnectionData> bmpConnectionData,
			Integer numberOfBoards, List<ChipLocation> ignoreChips,
			Map<ChipLocation, Collection<Integer>> ignoreCores,
            Map<ChipLocation, Collection<Direction>> ignoredLinks,
			Integer maxCoreID, boolean autodetectBMP,
			List<ConnectionDescriptor> scampConnections, Integer bootPortNumber,
			Integer maxSDRAMSize)
			throws IOException, SpinnmanException, Exception {
		if (host != null) {
			log.info("Creating transceiver for {}", host);
		}
		List<Connection> connections = new ArrayList<>();

		/*
		 * if no BMP has been supplied, but the board is a spinn4 or a spinn5
		 * machine, then an assumption can be made that the BMP is at -1 on the
		 * final value of the IP address
		 */
		if (version >= BIGGER_BOARD && autodetectBMP
				&& (bmpConnectionData == null || bmpConnectionData.isEmpty())) {
			bmpConnectionData = singletonList(
					workOutBMPFromMachineDetails(host, numberOfBoards));
		}

		// handle BMP connections
		if (bmpConnectionData != null) {
			List<InetAddress> bmpIPs = new ArrayList<>();
			for (BMPConnectionData connData : bmpConnectionData) {
				BMPConnection connection = new BMPConnection(connData);
				connections.add(connection);
				bmpIPs.add(connection.getRemoteIPAddress());
			}
			log.info("Transceiver using BMPs: {}", bmpIPs);
		}

		// handle the SpiNNaker connection
		if (scampConnections == null) {
			connections.add(new SCPConnection(host));
		}

		// handle the boot connection
		connections
				.add(new BootConnection(null, null, host, bootPortNumber));

		return new Transceiver(version, connections, ignoreChips, ignoreCores,
				ignoredLinks, maxCoreID, scampConnections, maxSDRAMSize);
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
	 *            the type of SpiNNaker board used within the SpiNNaker machine
	 *            being used. If a spinn-5 board, then the version will be 5,
	 *            spinn-3 would equal 3 and so on.
	 * @return The created transceiver
	 * @throws IOException
	 *             if networking fails
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable.
	 */
	public static Transceiver createTransceiver(InetAddress hostname,
			int version) throws IOException, SpinnmanException, Exception {
		return createTransceiver(hostname, version, null, 0, emptyList(),
				emptyMap(), emptyMap(), null, false, null, null, null);
	}

	/**
	 * Create a transceiver.
	 *
	 * @param version
	 *            The SpiNNaker board version number.
	 * @throws IOException
	 *             if networking fails
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable.
	 */
	public Transceiver(int version)
			throws IOException, SpinnmanException, Exception {
		this(version, null, null, null, null, null, null, null);
	}

	/**
	 * Create a transceiver.
	 *
	 * @param version
	 *            The SpiNNaker board version number.
	 * @param connections
	 *            The connections to use in the transceiver. Note that the
	 *            transceiver may make additional connections.
	 * @param ignoreChips
	 *            Blacklisted chips.
	 * @param ignoreCores
	 *            Blacklisted cores.
	 * @param ignoreLinks
	 *            Blacklisted links.
	 * @param maxCoreID
	 *            If not <tt>null</tt>, the maximum core ID to allow.
	 * @param scampConnections
	 *            Descriptions of SCP connections to create.
	 * @param maxSDRAMSize
	 *            If not <tt>null</tt>, the maximum SDRAM size to allow.
	 * @throws IOException
	 *             if networking fails
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 * @throws SpinnmanException
	 *             If a BMP is uncontactable.
	 */
	public Transceiver(int version, Collection<Connection> connections,
			Collection<ChipLocation> ignoreChips,
			Map<ChipLocation, Collection<Integer>> ignoreCores,
			Map<ChipLocation, Collection<Direction>> ignoreLinks,
            Integer maxCoreID,
			Collection<ConnectionDescriptor> scampConnections,
			Integer maxSDRAMSize)
			throws IOException, SpinnmanException, Exception {
		this.version = version;
		if (ignoreChips != null) {
			this.ignoreChips.addAll(ignoreChips);
		}
		if (ignoreCores != null) {
			this.ignoreCores.putAll(ignoreCores);
		}
		if (ignoreLinks != null) {
			this.ignoreLinks.putAll(ignoreLinks);
		}
		this.maxCoreID = maxCoreID;
		this.maxSDRAMSize = maxSDRAMSize;

		if (connections == null) {
			connections = emptyList();
		}
		originalConnections.addAll(connections);
		allConnections.addAll(connections);
		// if there has been SCAMP connections given, build them
		if (scampConnections != null) {
			for (ConnectionDescriptor desc : scampConnections) {
				connections.add(new SCPConnection(desc.chip, desc.hostname,
						desc.portNumber));
			}
		}
		scpSelector = identifyConnections(connections);
		checkBMPConnections();
	}

	private ConnectionSelector<SCPConnection> identifyConnections(
			Collection<Connection> connections) {
		for (Connection conn : connections) {
			// locate the only boot send conn
			if (conn instanceof BootSender) {
				if (bootSendConnection != null) {
					throw new IllegalArgumentException(
							"Only a single BootSender can be specified");
				}
				bootSendConnection = (BootSender) conn;
			}

			// locate any boot receiver connections
			if (conn instanceof BootReceiver) {
				bootReceiveConnections.add((BootReceiver) conn);
			}

			// Locate any connections listening on a UDP port
			if (conn instanceof UDPConnection) {
				registerConnection((UDPConnection<?>) conn);
			}

			/*
			 * Locate any connections that can send SCP (that are not BMP
			 * connections)
			 */
			if (conn instanceof SCPSender && !(conn instanceof BMPConnection)) {
				scpSenderConnections.add((SCPSender) conn);
			}

			// Locate any connections that can send SDP
			if (conn instanceof SDPSender) {
				sdpSenderConnections.add((SDPSender) conn);
			}

			// Locate any connections that can send Multicast
			if (conn instanceof MulticastSender) {
				multicastSenderConnections.add((MulticastSender) conn);
			}

			// Locate any connections that can send and receive SCP
			if (conn instanceof SCPSender && conn instanceof SCPReceiver) {
				// If it is a BMP connection, add it here
				if (conn instanceof BMPConnection) {
					BMPConnection bmpc = (BMPConnection) conn;
					bmpConnections.add(bmpc);
					bmpSelectors.put(new BMPCoords(bmpc.cabinet, bmpc.frame),
							new RoundRobinConnectionSelector<>(
									singletonList(bmpc)));
				} else if (conn instanceof SCPConnection) {
					SCPConnection scamp = (SCPConnection) conn;
					scampConnections.add(scamp);
					udpScampConnections.put(scamp.getRemoteIPAddress(), scamp);
				}
			}
		}

		// update the transceiver with the connection selectors.
		return new MostDirectConnectionSelector<SCPConnection>(machine,
				scampConnections);
	}

	/**
	 * Get the connections for talking to a board.
	 *
	 * @param connection
	 *            directly gives the connection to use. May be <tt>null</tt>
	 * @param boardAddress
	 *            the address of the board to talk to. May be <tt>null</tt>
	 * @return List of length 1 or 0 (the latter only if the search for the
	 *         given board address fails).
	 */
	private Collection<SCPConnection> getConnectionList(
			SCPConnection connection, InetAddress boardAddress) {
		if (connection != null) {
			return singletonList(connection);
		} else if (boardAddress == null) {
			return scampConnections;
		}
		connection = locateSpinnakerConnection(boardAddress);
		if (connection == null) {
			return emptyList();
		}
		return singletonList(connection);
	}

	private Object getSystemVariable(HasChipLocation chip,
			SystemVariableDefinition dataItem) throws IOException, Exception {
		ByteBuffer buffer =
				readMemory(chip, SYSTEM_VARIABLE_BASE_ADDRESS + dataItem.offset,
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
		default:
			// Unreachable
			return null;
		}
	}

	private ConnectionSelector<BMPConnection> bmpConnection(int cabinet,
			int frame) {
		BMPCoords key = new BMPCoords(cabinet, frame);
		if (!bmpSelectors.containsKey(key)) {
			throw new IllegalArgumentException(
					"Unknown combination of cabinet (" + cabinet
							+ ") and frame (" + frame + ")");
		}
		return bmpSelectors.get(key);
	}

	private static final int NNID_MAX = 0x7F;

	private byte getNextNearestNeighbourID() {
		synchronized (nearestNeighbourLock) {
			int next = (nearestNeighbourID + 1) & NNID_MAX;
			nearestNeighbourID = next;
			return (byte) next;
		}
	}

	private class ExecuteLock implements AutoCloseable {
		private final Semaphore lock;

		ExecuteLock(HasChipLocation chip) throws InterruptedException {
			ChipLocation key = chip.asChipLocation();
			synchronized (chipExecuteLockCondition) {
				lock = chipExecuteLocks.get(key);
			}
			lock.acquire();
			synchronized (chipExecuteLockCondition) {
				numChipExecuteLocks++;
			}
		}

		@Override
		public void close() {
			synchronized (chipExecuteLockCondition) {
				lock.release();
				numChipExecuteLocks--;
				chipExecuteLockCondition.notifyAll();
			}
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
	 * @param connections
	 *            the list of connections to locate a random one from
	 * @return a connection object
	 */
	private static <C> C getRandomConnection(List<C> connections) {
		if (connections == null || connections.isEmpty()) {
			return null;
		}
		int idx = ThreadLocalRandom.current().nextInt(0, connections.size());
		return connections.get(idx);
	}

	private static final int EXECUTABLE_ADDRESS = 0x67800000;
	private static final int DEFAULT_DESTINATION_COORDINATE = 255;
	private static final ChipLocation DEFAULT_DESTINATION = new ChipLocation(
			DEFAULT_DESTINATION_COORDINATE, DEFAULT_DESTINATION_COORDINATE);
	private static final String SCAMP_NAME = "SC&MP";
	private static final Version SCAMP_VERSION = new Version(3, 0, 1);
	private static final String BMP_NAME = "BC&MP";
	private static final Set<Integer> BMP_MAJOR_VERSIONS =
			unmodifiableSet(new HashSet<>(asList(1, 2)));
	private static final int INITIAL_FIND_SCAMP_RETRIES_COUNT = 3;
	private static final int STANDARD_RETRIES_NO = 3;

	/** Check that the BMP connections are actually connected to valid BMPs. */
	private void checkBMPConnections()
			throws IOException, SpinnmanException, Exception {
		/*
		 * Check that the UDP BMP conn is actually connected to a BMP via the
		 * SVER command
		 */
		for (BMPConnection conn : bmpConnections) {
			// try to send a BMP SVER to check if it responds as expected
			try {
				VersionInfo versionInfo =
						readBMPVersion(conn.boards, conn.cabinet, conn.frame);
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
			} catch (Exception e) {
				log.error("Failed to speak to BMP at {}",
						conn.getRemoteIPAddress(), e);
				throw e;
			}
		}
	}

	private static final int CONNECTION_CHECK_DELAY = 100;

	/**
	 * Check that the given connection to the given chip works.
	 *
	 * @param connectionSelector
	 *            the connection selector to use
	 * @param chip
	 *            the chip coordinates to try to talk to
	 * @param retries
	 *            how many attempts to do before giving up
	 * @return True if a valid response is received, False otherwise
	 */
	protected boolean checkConnection(
			ConnectionSelector<SCPConnection> connectionSelector, int retries,
			HasChipLocation chip) {
		for (int r = 0; r < retries; r++) {
			try {
				ChipSummaryInfo chipInfo =
						new SendSingleSCPCommandProcess(connectionSelector)
								.execute(new GetChipInfo(chip)).chipInfo;
				if (chipInfo.isEthernetAvailable) {
					return true;
				}
				sleep(CONNECTION_CHECK_DELAY);
			} catch (InterruptedException | SocketTimeoutException
					| Exception e) {
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
		if (connection == null) {
			connection =
					(SCPConnection) getRandomConnection(scpSenderConnections);
		}
		connection.sendSCPRequest(message);
	}

	@Override
	public void sendSDPMessage(SDPMessage message, SDPConnection connection)
			throws IOException {
		if (connection == null) {
			connection =
					(SDPConnection) getRandomConnection(sdpSenderConnections);
		}
		connection.sendSDPMessage(message);
	}

	/** Get the current machine status and store it. */
	void updateMachine() throws IOException, Exception {
		// Get the width and height of the machine
		getMachineDimensions();

		// Get the coordinates of the boot chip
		VersionInfo versionInfo = getScampVersion();

		// Get the details of all the chips
		machine = new GetMachineProcess(scpSelector, ignoreChips, ignoreCores,
				ignoreLinks, maxCoreID, maxSDRAMSize)
            .getMachineDetails(versionInfo.core, dimensions);

		// update the SCAMP selector with the machine
		if (scpSelector instanceof MachineAware) {
			((MachineAware) scpSelector).setMachine(machine);
		}

		/*
		 * update the SCAMP connections replacing any x and y with the default
		 * SCP request params with the boot chip coordinates
		 */
		for (SCPConnection sc : scampConnections) {
			if (sc.getChip().equals(DEFAULT_DESTINATION)) {
				sc.setChip(machine.boot);
			}
		}

		// Work out and add the SpiNNaker links and FPGA links
		machine.addSpinnakerLinks();
		machine.addFpgaLinks();

		// TODO: Actually get the existing APP_IDs in use
		appIDTracker = new AppIdTracker();

		log.info("Detected a machine on IP address {} which has {}",
				bootSendConnection.getRemoteIPAddress(),
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public List<SCPConnection> discoverScampConnections()
			throws IOException, Exception {
		/*
		 * Currently, this only finds other UDP connections given a connection
		 * that supports SCP - this is done via the machine
		 */
		if (scampConnections == null || scampConnections.isEmpty()) {
			return Collections.emptyList();
		}

		// Get the machine dimensions
		MachineDimensions dims = getMachineDimensions();

		// Find all the new connections via the machine Ethernet-connected chips
		List<SCPConnection> newConnections = new ArrayList<>();
		for (ChipLocation chip : getSpinn5Geometry()
				.getPotentialRootChips(dims)) {
			InetAddress ipAddress = getByAddress(
					(byte[]) getSystemVariable(chip, ethernet_ip_address));
			if (udpScampConnections.containsKey(ipAddress)) {
				continue;
			}
			SCPConnection conn = searchForProxies(chip);

			// if no data, no proxy
			if (conn == null) {
				conn = new SCPConnection(chip, ipAddress);
			} else {
				// proxy, needs an adjustment
				if (udpScampConnections
						.containsKey(conn.getRemoteIPAddress())) {
					udpScampConnections.remove(conn.getRemoteIPAddress());
				}
			}

			// check if it works
			if (checkConnection(new MostDirectConnectionSelector<>(null,
					singletonList(conn)), STANDARD_RETRIES_NO, chip)) {
				scpSenderConnections.add(conn);
				allConnections.add(conn);
				udpScampConnections.put(ipAddress, conn);
				scampConnections.add(conn);
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
	 * @return connection or <tt>null</tt> if there is no such connection
	 */
	private SCPConnection searchForProxies(HasChipLocation chip) {
		for (SCPConnection connection : scampConnections) {
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
			throws IOException, Exception {
		if (dimensions == null) {
			ByteBuffer data = readMemory(DEFAULT_DESTINATION,
					SYSTEM_VARIABLE_BASE_ADDRESS + y_size.offset, 2);
			int height = toUnsignedInt(data.get());
			int width = toUnsignedInt(data.get());
			dimensions = new MachineDimensions(width, height);
		}
		return dimensions;
	}

	@Override
	public Machine getMachineDetails() throws IOException, Exception {
		if (machine == null) {
			updateMachine();
		}
		return machine;
	}

	/**
	 * @return the application ID tracker for this transceiver.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	public AppIdTracker getAppIdTracker() throws IOException, Exception {
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
		return scampConnections.stream().anyMatch(this::connectedTest);
	}

	private boolean connectedTest(Connection c) {
		try {
			return c.isConnected();
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public VersionInfo getScampVersion(HasChipLocation chip,
			ConnectionSelector<SCPConnection> connectionSelector)
			throws IOException, Exception {
		if (connectionSelector == null) {
			connectionSelector = scpSelector;
		}
		return new GetVersionProcess(connectionSelector)
				.getVersion(chip.getScampCore());
	}

	private static final int POST_BOOT_DELAY = 2000;

	@Override
	public void bootBoard(Map<SystemVariableDefinition, Object> extraBootValues)
			throws InterruptedException, IOException {
		BootMessages bootMessages = new BootMessages(version, extraBootValues);
		Iterator<BootMessage> msgs = bootMessages.getMessages().iterator();
		while (msgs.hasNext()) {
			bootSendConnection.sendBootMessage(msgs.next());
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
	public static boolean isScampVersionCompabible(Version version) {
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
	 * The number of milliseconds after powering on the machine to wait before
	 * attempting to boot SCAMP on its chips. This is time to allow the code on
	 * each chip's ROM to figure out what the state of the hardware is enough
	 * for booting to be viable.
	 */
	private static final int POST_POWER_ON_DELAY = 2000;

	@Override
	public VersionInfo ensureBoardIsReady(int numRetries,
			Map<SystemVariableDefinition, Object> extraBootValues)
			throws IOException, Exception, InterruptedException {
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
				|| !isScampVersionCompabible(versionInfo.versionNumber)) {
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
		for (SCPConnection connection : scampConnections) {
			new SendSingleSCPCommandProcess(scpSelector).execute(
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
	 * @throws Exception
	 *             If SpiNNaker rejects a message.
	 */
	private VersionInfo findScampAndBoot(int numAttempts,
			Map<SystemVariableDefinition, Object> extraBootValues)
			throws InterruptedException, IOException, Exception {
		VersionInfo versionInfo = null;
		int triesLeft = numAttempts;
		while (versionInfo == null && triesLeft > 0) {
			try {
				versionInfo = getScampVersion();
				if (versionInfo.core.asChipLocation()
						.equals(DEFAULT_DESTINATION)) {
					versionInfo = null;
					sleep(CONNECTION_CHECK_DELAY);
				}
			} catch (Exception e) {
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
			if (versionInfo.core.asChipLocation().equals(DEFAULT_DESTINATION)) {
				versionInfo = null;
			}
		}
		if (versionInfo != null) {
			log.info("Found board with version {}", versionInfo);
		}
		return versionInfo;
	}

	@Override
	public Iterable<CPUInfo> getCPUInformation(CoreSubsets coreSubsets)
			throws IOException, Exception {
		// Get all the cores if the subsets are not given
		if (coreSubsets == null) {
			if (machine == null) {
				updateMachine();
			}
			coreSubsets = new CoreSubsets();
			for (Chip chip : machine.chips()) {
				for (Processor processor : chip.allProcessors()) {
					coreSubsets.addCore(new CoreLocation(chip.getX(),
							chip.getY(), processor.processorId));
				}
			}
		}

		return new GetCPUInfoProcess(scpSelector).getCPUInfo(coreSubsets);
	}

	@Override
	public Iterable<IOBuffer> getIobuf(CoreSubsets coreSubsets)
			throws IOException, Exception {
		// making the assumption that all chips have the same iobuf size.
		if (iobufSize == null) {
			iobufSize = (Integer) getSystemVariable(new ChipLocation(0, 0),
					SystemVariableDefinition.iobuf_size);
		}

		// read iobuf from machine
		return new ReadIOBufProcess(scpSelector).readIOBuf(iobufSize,
				coreSubsets);
	}

	@Override
	public void setWatchDogTimeoutOnChip(HasChipLocation chip, int watchdog)
			throws IOException, Exception {
		// build data holder
		ByteBuffer data = ByteBuffer.allocate(1);
		data.put((byte) watchdog).flip();

		// write data
		writeMemory(chip,
				SYSTEM_VARIABLE_BASE_ADDRESS + software_watchdog_count.offset,
				data);
	}

	@Override
	public void enableWatchDogTimerOnChip(HasChipLocation chip,
			boolean watchdog) throws IOException, Exception {
		// build data holder
		ByteBuffer data = ByteBuffer.allocate(1);
		data.put((byte) (watchdog
				? (Integer) software_watchdog_count.getDefault()
				: 0)).flip();

		// write data
		writeMemory(chip,
				SYSTEM_VARIABLE_BASE_ADDRESS + software_watchdog_count.offset,
				data);
	}

	@Override
	public int getCoreStateCount(int appID, CPUState state)
			throws IOException, Exception {
		return new SendSingleSCPCommandProcess(scpSelector)
				.execute(new CountState(appID, state)).count;
	}

	@Override
	public void execute(HasChipLocation chip, Collection<Integer> processors,
			InputStream executable, int numBytes, int appID, boolean wait)
			throws IOException, Exception, InterruptedException {
		// Lock against updates
		try (ExecuteLock lock = new ExecuteLock(chip)) {
			// Write the executable
			writeMemory(chip, EXECUTABLE_ADDRESS, executable, numBytes);

			// Request the start of the executable
			new SendSingleSCPCommandProcess(scpSelector)
					.execute(new ApplicationRun(appID, chip, processors, wait));
		}
	}

	@Override
	public final void execute(HasChipLocation chip,
			Collection<Integer> processors, File executable, int appID,
			boolean wait) throws IOException, Exception, InterruptedException {
		// Lock against updates
		try (ExecuteLock lock = new ExecuteLock(chip)) {
			// Write the executable
			writeMemory(chip, EXECUTABLE_ADDRESS, executable);

			// Request the start of the executable
			new SendSingleSCPCommandProcess(scpSelector)
					.execute(new ApplicationRun(appID, chip, processors, wait));
		}
	}

	@Override
	public void execute(HasChipLocation chip, Collection<Integer> processors,
			ByteBuffer executable, int appID, boolean wait)
			throws IOException, Exception, InterruptedException {
		// Lock against updates
		try (ExecuteLock lock = new ExecuteLock(chip)) {
			// Write the executable
			writeMemory(chip, EXECUTABLE_ADDRESS, executable);

			// Request the start of the executable
			new SendSingleSCPCommandProcess(scpSelector)
					.execute(new ApplicationRun(appID, chip, processors, wait));
		}
	}

	@Override
	public void executeFlood(CoreSubsets coreSubsets, InputStream executable,
			int numBytes, int appID, boolean wait)
			throws IOException, Exception, InterruptedException {
		// Lock against other executables
		synchronized (chipExecuteLockCondition) {
			while (numChipExecuteLocks > 0) {
				wait();
			}

			// Flood fill the system with the binary
			writeMemoryFlood(EXECUTABLE_ADDRESS, executable, numBytes);

			// Execute the binary on the cores on the chips where required
			new ApplicationRunProcess(scpSelector).run(appID, coreSubsets,
					wait);
		}
	}

	@Override
	public void executeFlood(CoreSubsets coreSubsets, File executable,
			int appID, boolean wait)
			throws IOException, Exception, InterruptedException {
		// Lock against other executables
		synchronized (chipExecuteLockCondition) {
			while (numChipExecuteLocks > 0) {
				wait();
			}

			// Flood fill the system with the binary
			writeMemoryFlood(EXECUTABLE_ADDRESS, executable);

			// Execute the binary on the cores on the chips where required
			new ApplicationRunProcess(scpSelector).run(appID, coreSubsets,
					wait);
		}
	}

	@Override
	public void executeFlood(CoreSubsets coreSubsets, ByteBuffer executable,
			int appID, boolean wait)
			throws IOException, Exception, InterruptedException {
		// Lock against other executables
		synchronized (chipExecuteLockCondition) {
			while (numChipExecuteLocks > 0) {
				wait();
			}

			// Flood fill the system with the binary
			writeMemoryFlood(EXECUTABLE_ADDRESS, executable);

			// Execute the binary on the cores on the chips where required
			ApplicationRunProcess process =
					new ApplicationRunProcess(scpSelector);
			process.run(appID, coreSubsets, wait);
		}
	}

	@Override
	public void powerOnMachine()
			throws InterruptedException, IOException, Exception {
		if (bmpConnections.isEmpty()) {
			log.warn("No BMP connections, so can't power on");
		}
		for (BMPConnection connection : bmpConnections) {
			power(POWER_ON, connection.boards, connection.cabinet,
					connection.frame);
		}
	}

	@Override
	public void powerOffMachine()
			throws InterruptedException, IOException, Exception {
		if (bmpConnections.isEmpty()) {
			log.warn("No BMP connections, so can't power off");
		}
		for (BMPConnection connection : bmpConnections) {
			power(POWER_OFF, connection.boards, connection.cabinet,
					connection.frame);
		}
	}

	@Override
	public void power(PowerCommand powerCommand, Collection<Integer> boards,
			int cabinet, int frame)
			throws InterruptedException, IOException, Exception {
		int timeout = (int) (MS_PER_S
				* (powerCommand == POWER_ON ? BMP_POWER_ON_TIMEOUT
						: BMP_TIMEOUT));
		new SendSingleBMPCommandProcess<SetPower.Response>(
				bmpConnection(cabinet, frame), timeout)
						.execute(new SetPower(powerCommand, boards, 0.0));
		machineOff = powerCommand == POWER_OFF;

		// Sleep for 5 seconds if the machine has just been powered on
		if (!machineOff) {
			sleep((int) (BMP_POST_POWER_ON_SLEEP_TIME * MS_PER_S));
		}
	}

	@Override
	public void setLED(Collection<Integer> leds, LEDAction action,
			Collection<Integer> board, int cabinet, int frame)
			throws IOException, Exception {
		new SendSingleBMPCommandProcess<BMPSetLED.Response>(
				bmpConnection(cabinet, frame))
						.execute(new BMPSetLED(leds, action, board));
	}

	@Override
	public int readFPGARegister(int fpgaNumber, int register, int cabinet,
			int frame, int board) throws IOException, Exception {
		return new SendSingleBMPCommandProcess<ReadFPGARegister.Response>(
				bmpConnection(cabinet, frame))
						.execute(new ReadFPGARegister(fpgaNumber, register,
								board)).fpgaRegister;
	}

	@Override
	public void writeFPGARegister(int fpgaNumber, int register, int value,
			int cabinet, int frame, int board) throws IOException, Exception {
		new SendSingleBMPCommandProcess<WriteFPGARegister.Response>(
				bmpConnection(cabinet, frame))
						.execute(new WriteFPGARegister(fpgaNumber, register,
								value, board));
	}

	@Override
	public ADCInfo readADCData(int board, int cabinet, int frame)
			throws IOException, Exception {
		return new SendSingleBMPCommandProcess<ReadADC.Response>(
				bmpConnection(cabinet, frame))
						.execute(new ReadADC(board)).adcInfo;
	}

	@Override
	public VersionInfo readBMPVersion(int board, int cabinet, int frame)
			throws IOException, Exception {
		return new SendSingleBMPCommandProcess<GetBMPVersion.Response>(
				bmpConnection(cabinet, frame))
						.execute(new GetBMPVersion(board)).versionInfo;
	}

	@Override
	public void writeMemory(HasCoreLocation core, int baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, Exception {
		new WriteMemoryProcess(scpSelector).writeMemory(core, baseAddress,
				dataStream, numBytes);
	}

	@Override
	public void writeMemory(HasCoreLocation core, int baseAddress,
			File dataFile) throws IOException, Exception {
		new WriteMemoryProcess(scpSelector).writeMemory(core, baseAddress,
				dataFile);
	}

	@Override
	public void writeMemory(HasCoreLocation core, int baseAddress,
			ByteBuffer data) throws IOException, Exception {
		new WriteMemoryProcess(scpSelector).writeMemory(core, baseAddress,
				data);
	}

	@Override
	public void writeNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, InputStream dataStream, int numBytes)
			throws IOException, Exception {
		new WriteMemoryProcess(scpSelector).writeLink(core, link, baseAddress,
				dataStream, numBytes);
	}

	@Override
	public void writeNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, File dataFile) throws IOException, Exception {
		new WriteMemoryProcess(scpSelector).writeLink(core, link, baseAddress,
				dataFile);
	}

	@Override
	public void writeNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, ByteBuffer data) throws IOException, Exception {
		new WriteMemoryProcess(scpSelector).writeLink(core, link, baseAddress,
				data);
	}

	@Override
	public void writeMemoryFlood(int baseAddress, InputStream dataStream,
			int numBytes) throws IOException, Exception {
		WriteMemoryFloodProcess process =
				new WriteMemoryFloodProcess(scpSelector);
		// Ensure only one flood fill occurs at any one time
		synchronized (floodWriteLock) {
			// Start the flood fill
			process.writeMemory(getNextNearestNeighbourID(), baseAddress,
					dataStream, numBytes);
		}
	}

	@Override
	public void writeMemoryFlood(int baseAddress, File dataFile)
			throws IOException, Exception {
		WriteMemoryFloodProcess process =
				new WriteMemoryFloodProcess(scpSelector);
		// Ensure only one flood fill occurs at any one time
		synchronized (floodWriteLock) {
			// Start the flood fill
			process.writeMemory(getNextNearestNeighbourID(), baseAddress,
					dataFile);
		}
	}

	@Override
	public void writeMemoryFlood(int baseAddress, ByteBuffer data)
			throws IOException, Exception {
		WriteMemoryFloodProcess process =
				new WriteMemoryFloodProcess(scpSelector);
		// Ensure only one flood fill occurs at any one time
		synchronized (floodWriteLock) {
			// Start the flood fill
			process.writeMemory(getNextNearestNeighbourID(), baseAddress, data);
		}
	}

	@Override
	public ByteBuffer readMemory(HasCoreLocation core, int baseAddress,
			int length) throws IOException, Exception {
		return new ReadMemoryProcess(scpSelector).readMemory(core, baseAddress,
				length);
	}

	@Override
	public ByteBuffer readNeighbourMemory(HasCoreLocation core, int link,
			int baseAddress, int length) throws IOException, Exception {
		return new ReadMemoryProcess(scpSelector).readLink(core, link,
				baseAddress, length);
	}

	@Override
	public void stopApplication(int appID) throws IOException, Exception {
		if (machineOff) {
			log.warn("You are calling a app stop on a turned off machine. "
					+ "Please fix and try again");
			return;
		}
		new SendSingleSCPCommandProcess(scpSelector)
				.execute(new ApplicationStop(appID));
	}

	@Override
	public void waitForCoresToBeInState(CoreSubsets allCoreSubsets, int appID,
			Set<CPUState> cpuStates, Integer timeout, int timeBetweenPolls,
			Set<CPUState> errorStates, int countBetweenFullChecks)
			throws IOException, Exception, InterruptedException,
			SpinnmanException {
		// check that the right number of processors are in the states
		int processorsReady = 0;
		long timeoutTime =
				currentTimeMillis() + (timeout == null ? 0 : timeout);
		int tries = 0;
		while (processorsReady < allCoreSubsets.size()
				&& (timeout == null || currentTimeMillis() < timeoutTime)) {
			// Get the number of processors in the ready states
			processorsReady = 0;
			for (CPUState state : cpuStates) {
				processorsReady += getCoreStateCount(appID, state);
			}

			// If the count is too small, check for error states
			if (processorsReady < allCoreSubsets.size()) {
				for (CPUState state : errorStates) {
					int errorCores = getCoreStateCount(appID, state);
					if (errorCores > 0) {
						throw new SpinnmanException(format(
								"%d cores have reached an error state %s",
								errorCores, state));
					}
				}

				/*
				 * If we haven't seen an error, increase the tries, and do a
				 * full check if required
				 */
				tries++;
				if (tries >= countBetweenFullChecks) {
					CoreSubsets coresInState =
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
			CoreSubsets coresInState =
					getCoresInState(allCoreSubsets, cpuStates);

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
	public void sendSignal(int appID, Signal signal)
			throws IOException, Exception {
		new SendSingleSCPCommandProcess(scpSelector)
				.execute(new SendSignal(appID, signal));
	}

	@Override
	public void setLEDs(HasCoreLocation core, Map<Integer, LEDAction> ledStates)
			throws IOException, Exception {
		new SendSingleSCPCommandProcess(scpSelector)
				.execute(new SetLED(core, ledStates));
	}

	@Override
	public SCPConnection locateSpinnakerConnection(InetAddress boardAddress) {
		return (SCPConnection) udpScampConnections.get(boardAddress);
	}

	@Override
	public void setIPTag(IPTag tag) throws IOException, Exception {
		// Check that the tag has a port assigned
		if (tag.getPort() == null) {
			throw new IllegalArgumentException(
					"The tag port must have been set");
		}

		/*
		 * Get the connections. If the tag specifies a connection, use that,
		 * otherwise apply the tag to all connections
		 */
		Collection<SCPConnection> connections =
				getConnectionList(null, tag.getBoardAddress());
		if (connections == null || connections.isEmpty()) {
			throw new IllegalArgumentException(
					"The given board address is not recognised");
		}

		for (SCPConnection connection : connections) {
			// Convert the host string
			InetAddress host = tag.getBoardAddress();
			if (host == null || host.isAnyLocalAddress()
					|| host.isLoopbackAddress()) {
				host = connection.getLocalIPAddress();
			}

			new SendSingleSCPCommandProcess(scpSelector).execute(
					new IPTagSet(connection.getChip(), host.getAddress(),
							tag.getPort(), tag.getTag(), tag.isStripSDP()));
		}
	}

	@Override
	public void setReverseIPTag(ReverseIPTag tag)
			throws IOException, Exception {
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
		Collection<SCPConnection> connections =
				getConnectionList(null, tag.getBoardAddress());
		if (connections == null || connections.isEmpty()) {
			throw new IllegalArgumentException(
					"The given board address is not recognised");
		}

		for (SCPConnection connection : connections) {
			new SendSingleSCPCommandProcess(scpSelector)
					.execute(new ReverseIPTagSet(connection.getChip(),
							tag.getDestination(), tag.getPort(), tag.getTag(),
							tag.getPort()));
		}
	}

	@Override
	public void clearIPTag(int tag, SCPConnection connection,
			InetAddress boardAddress) throws IOException, Exception {
		for (SCPConnection conn : getConnectionList(connection, boardAddress)) {
			new SendSingleSCPCommandProcess(scpSelector)
					.execute(new IPTagClear(conn.getChip(), tag));
		}
	}

	@Override
	public List<Tag> getTags(SCPConnection connection)
			throws IOException, Exception {
		List<Tag> allTags = new ArrayList<>();
		for (SCPConnection conn : getConnectionList(connection, null)) {
			allTags.addAll(new GetTagsProcess(scpSelector).getTags(conn));
		}
		return allTags;
	}

	@Override
	public int mallocSDRAM(HasChipLocation chip, int size, int appID, int tag)
			throws IOException, Exception {
		return new MallocSDRAMProcess(scpSelector).mallocSDRAM(chip, size,
				appID, tag);
	}

	@Override
	public void freeSDRAM(HasChipLocation chip, int baseAddress, int appID)
			throws IOException, Exception {
		new DeallocSDRAMProcess(scpSelector).deallocSDRAM(chip, appID,
				baseAddress);
	}

	@Override
	public int freeSDRAMByAppID(HasChipLocation chip, int appID)
			throws IOException, Exception {
		return new DeallocSDRAMProcess(scpSelector).deallocSDRAM(chip, appID);
	}

	@Override
	public void loadMulticastRoutes(HasChipLocation chip,
			Collection<MulticastRoutingEntry> routes, int appID)
			throws IOException, Exception {
		new LoadMulticastRoutesProcess(scpSelector).loadRoutes(chip, routes,
				appID);
	}

	@Override
	public void loadFixedRoute(HasChipLocation chip, RoutingEntry fixedRoute,
			int appID) throws IOException, Exception {
		new LoadFixedRouteEntryProcess(scpSelector).loadFixedRoute(chip,
				fixedRoute, appID);
	}

	@Override
	public RoutingEntry readFixedRoute(HasChipLocation chip, int appID)
			throws IOException, Exception {
		return new ReadFixedRouteEntryProcess(scpSelector).readFixedRoute(chip,
				appID);
	}

	@Override
	public List<MulticastRoutingEntry> getMulticastRoutes(HasChipLocation chip,
			Integer appID) throws IOException, Exception {
		int address = (int) getSystemVariable(chip, router_table_copy_address);
		return new GetMulticastRoutesProcess(scpSelector).getRoutes(chip,
				address, appID);
	}

	@Override
	public void clearMulticastRoutes(HasChipLocation chip)
			throws IOException, Exception {
		new SendSingleSCPCommandProcess(scpSelector)
				.execute(new RouterClear(chip));
	}

	@Override
	public RouterDiagnostics getRouterDiagnostics(HasChipLocation chip)
			throws IOException, Exception {
		return new ReadRouterDiagnosticsProcess(scpSelector)
				.getRouterDiagnostics(chip);
	}

	@Override
	public void setRouterDiagnosticFilter(HasChipLocation chip, int position,
			DiagnosticFilter diagnosticFilter) throws IOException, Exception {
		if (position < 0 || position > NO_ROUTER_DIAGNOSTIC_FILTERS) {
			throw new IllegalArgumentException(
					"router filter positions must be beween 0 and "
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

		int address =
				(ROUTER_REGISTER_BASE_ADDRESS + ROUTER_FILTER_CONTROLS_OFFSET
						+ position * ROUTER_DIAGNOSTIC_FILTER_SIZE);
		writeMemory(chip, address, diagnosticFilter.getFilterWord());
	}

	@Override
	public DiagnosticFilter getRouterDiagnosticFilter(HasChipLocation chip,
			int position) throws IOException, Exception {
		if (position < 0 || position > NO_ROUTER_DIAGNOSTIC_FILTERS) {
			throw new IllegalArgumentException(
					"router filter positions must be beween 0 and "
							+ NO_ROUTER_DIAGNOSTIC_FILTERS);
		}
		int address =
				ROUTER_REGISTER_BASE_ADDRESS + ROUTER_FILTER_CONTROLS_OFFSET
						+ position * ROUTER_DIAGNOSTIC_FILTER_SIZE;
		Response response = new SendSingleSCPCommandProcess(scpSelector)
				.execute(new ReadMemory(chip, address, WORD_SIZE));
		return new DiagnosticFilter(response.data.getInt());
	}

	private static final int ENABLE_SHIFT = 16;
	private static final int ROUTER_DIAGNOSTIC_COUNTER_ADDR = 0xf100002c;

	@Override
	public void clearRouterDiagnosticCounters(HasChipLocation chip,
			boolean enable, Iterable<Integer> counterIDs)
			throws IOException, Exception {
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
				clearData |= 1 << counterID + ENABLE_SHIFT;
			}
		}
		writeMemory(chip, ROUTER_DIAGNOSTIC_COUNTER_ADDR, clearData);
	}

	@Override
	public List<HeapElement> getHeap(HasChipLocation chip,
			SystemVariableDefinition heap) throws IOException, Exception {
		return new GetHeapProcess(scpSelector).getBlocks(chip, heap);
	}

	@Override
	public void fillMemory(HasChipLocation chip, int baseAddress,
			int repeatValue, int size, DataType dataType)
			throws Exception, IOException {
		new FillProcess(scpSelector).fillMemory(chip, baseAddress, repeatValue,
				size, dataType);
	}

	/** @return the number of boards currently configured. */
	public int getNumberOfBoardsLocated() {
		// NB if no BMPs are available, then there's still at least one board
		return max(1, bmpConnections.stream()
				.mapToInt(bmpConn -> bmpConn.boards.size()).sum());
	}

	/**
	 * Close the transceiver and any threads that are running.
	 *
	 * @throws java.lang.Exception
	 *             If anything goes wrong
	 */
	@Override
	public void close() throws java.lang.Exception {
		close(true, false);
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
	 * @throws java.lang.Exception
	 *             If anything goes wrong
	 */
	public void close(boolean closeOriginalConnections, boolean powerOffMachine)
			throws java.lang.Exception {
		if (powerOffMachine && bmpConnections != null
				&& !bmpConnections.isEmpty()) {
			powerOffMachine();
		}

		super.close();

		for (Connection connection : allConnections) {
			if (closeOriginalConnections
					|| !originalConnections.contains(connection)) {
				connection.close();
			}
		}
	}

	private static final InetAddress WILDCARD_ADDRESS;
	static {
		try {
			WILDCARD_ADDRESS = InetAddress.getByAddress(new byte[] {
					0, 0, 0, 0
			});
			if (!WILDCARD_ADDRESS.isAnyLocalAddress()) {
				throw new RuntimeException(
						"wildcard address is not wildcard address?");
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException("unexpected failure to initialise", e);
		}
	}

	/**
	 * @return The connection selectors used for BMP connections.
	 */
	public Map<BMPCoords, ConnectionSelector<BMPConnection>>
			getBMPConnection() {
		return bmpSelectors;
	}

	/**
	 * A simple description of a connnection to create.
	 */
	public static final class ConnectionDescriptor {
		/** What host to talk to. */
		private InetAddress hostname;
		/** What port to talk to, or <tt>null</tt> for default. */
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
		public ConnectionDescriptor(
                InetAddress hostname, HasChipLocation chip) {
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
			this.hostname = requireNonNull(hostname);
			this.chip = chip.asChipLocation();
			this.portNumber = port;
		}
	}

	/**
	 * A simple description of a BMP to talk to.
	 */
	static final class BMPCoords {
		private final int cabinet;
		private final int frame;

		BMPCoords(int cabinet, int frame) {
			this.cabinet = cabinet;
			this.frame = frame;
		}

		@Override
		public int hashCode() {
			return cabinet << 16 | frame;
		}

		@Override
		public boolean equals(Object o) {
			if (o != null && o instanceof BMPCoords) {
				BMPCoords b = (BMPCoords) o;
				return cabinet == b.cabinet && frame == b.frame;
			}
			return false;
		}
	}

	@Override
	protected void addConnection(Connection connection) {
		this.allConnections.add(connection);
	}
}
