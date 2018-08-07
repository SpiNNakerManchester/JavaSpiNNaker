package uk.ac.manchester.spinnaker.transceiver;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import testconfig.BoardTestConfiguration;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter;
import uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter.Destination;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.IOBuffer;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister;
import uk.ac.manchester.spinnaker.messages.model.Signal;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;

/**
 * Communications integration test.
 *
 * @author Andrew Rowley
 * @author Donal Fellows
 */
class TransceiverITCase {
	static BoardTestConfiguration board_config;

	static int n_cores = 20;
	static List<ChipLocation> down_chips;
	static CoreSubsets core_subsets;
	static Map<ChipLocation, Collection<Integer>> down_cores;

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
		board_config = new BoardTestConfiguration();
		board_config.set_up_remote_board();
		core_subsets = new CoreSubsets();
		core_subsets.addCores(0, 0, range(1, 11).boxed().collect(toSet()));
		core_subsets.addCores(1, 1, range(1, 11).boxed().collect(toSet()));

		down_cores = new HashMap<>();
		down_cores.put(new ChipLocation(0, 0), Arrays.asList(5));

		down_chips = new ArrayList<>();
		down_chips.add(new ChipLocation(0, 1));
	}

	void printEnums(String name, Collection<? extends Enum<?>> enum_list) {
		StringBuilder sb = new StringBuilder(name).append(" ");
		for (Enum<?> enum_value : enum_list) {
			sb.append(enum_value.name()).append("; ");
		}
		System.out.println(sb.toString());
	}

	void printWordAsBinary(String name, int word, int[][] fields) {
		int start = 0;
		int end = 32;

		Set<Integer> start_fields = new HashSet<>();
		Set<Integer> end_fields = new HashSet<>();
		for (int[] field : fields) {
			start_fields.add(field[0]);
			end_fields.add(field[1]);
		}
		for (Integer v : start_fields) {
			end_fields.remove(v - 1);
		}

		StringBuilder prefix = new StringBuilder(" ");
		name.chars().forEachOrdered(c -> prefix.append(" "));
		StringBuilder header1 = new StringBuilder(prefix);
		StringBuilder header2 = new StringBuilder(prefix);
		StringBuilder header3 = new StringBuilder(prefix);
		StringBuilder mainline = new StringBuilder(name).append(" ");
		for (int i = start; i < end; i++) {
			if (start_fields.contains(i)) {
				header1.append("|");
				header2.append("|");
				header3.append("|");
				mainline.append("|");
			}
			header1.append(i % 10 == 0 ? i / 10 : " ");
			header2.append(i % 10);
			header3.append("=");
			mainline.append((word >>> i) & 0x1);
			if (end_fields.contains(i)) {
				header1.append("|");
				header2.append("|");
				header3.append("|");
				mainline.append("|");
			}
		}
		;
		System.out.println(header1);
		System.out.println(header2);
		System.out.println(header3);
		System.out.println(mainline);
	}

	private static final int[][] FILTER_FIELDS = new int[][] {
			{
					31, 31
			}, {
					30, 30
			}, {
					29, 29
			}, {
					28, 25
			}, {
					24, 16
			}, {
					15, 14
			}, {
					13, 12
			}, {
					11, 10
			}, {
					9, 9
			}, {
					8, 8
			}, {
					7, 4
			}, {
					3, 0
			}
	};

	void printFilter(DiagnosticFilter filter) {
		printWordAsBinary("Filter word:", filter.getFilterWord(),
				FILTER_FIELDS);
		System.out.println("Enable Interrupt: "
				+ filter.getEnableInterruptOnCounterEvent());
		System.out.println("Emergency Routing Status on Incoming: "
				+ filter.getMatchEmergencyRoutingStatusToIncomingPacket());
		printEnums("Destinations:", filter.getDestinations());
		printEnums("Sources:", filter.getSources());
		printEnums("Payloads:", filter.getPayloadStatuses());
		printEnums("Default Routing:", filter.getDefaultRoutingStatuses());
		printEnums("Emergency Routing:", filter.getEmergencyRoutingStatuses());
		printEnums("Packet Types:", filter.getPacketTypes());
	}

	private static class Section implements AutoCloseable {
		Section(String msg) {
			StringBuilder underline = new StringBuilder(msg.length());
			msg.chars().forEachOrdered(c -> underline.append("="));

			System.out.println(msg);
			System.out.println(underline);
		}

		@Override
		public void close() {
			System.out.println();
		}

	}

	private interface Call {
		void call() throws Exception;
	}

	private void section(String title, Call call) throws Exception {
		try (Section s = new Section(title)) {
			call.call();
		}
	}

	private static final ChipLocation SCAMP = new ChipLocation(0, 0);
	/** Where we like to read and write when testing. */
	private static final int MEM = 0x70000000;

	private void boardReady(Transceiver transceiver) throws Exception {
		VersionInfo version_info = transceiver.ensureBoardIsReady();
		System.out.println(version_info);
	}

	private void findConnections(Transceiver transceiver) throws Exception {
		List<SCPConnection> connections =
				transceiver.discoverScampConnections();
		System.out.println(connections);
	}

	private void retrieveDetails(Transceiver transceiver) throws Exception {
		Machine machine = transceiver.getMachineDetails();
		System.out.println(machine);
		System.out.println(machine.bootChip());
	}

	private void readWrite(Transceiver transceiver) throws Exception {
		ByteBuffer write_data = ByteBuffer.allocate(1000);
		while (write_data.hasRemaining()) {
			write_data.put((byte) (Math.random() * 256));
		}
		write_data.flip();
		transceiver.writeMemory(SCAMP, MEM, write_data);
		ByteBuffer read_data = transceiver.readMemory(SCAMP, MEM, 1000);
		assertArrayEquals(write_data.array(), read_data.array());
	}

	private void floodWrite(Transceiver transceiver) throws Exception {
		transceiver.writeMemoryFlood(MEM, 0x04050607);
		ByteBuffer read_data =
				transceiver.readMemory(new ChipLocation(1, 1), MEM, 4);
		System.out.printf("%x\n", read_data.getInt());
	}

	private void execFlood(Transceiver transceiver, int app_id)
			throws Exception {
		transceiver.executeFlood(core_subsets, new File("hello.aplx"), app_id);
		int count = 0;
		while (count < 20) {
			count = transceiver.getCoreStateCount(app_id, CPUState.SYNC0);
			System.out.printf("Cores in state SYNC0=%d\n", count);
			Thread.sleep(100);
		}
	}

	private void cpuInfo(Transceiver transceiver) throws Exception {
		List<CPUInfo> cpu_infos = new ArrayList<>();
		transceiver.getCPUInformation(core_subsets).forEach(cpu_infos::add);
		Collections.sort(cpu_infos,
				(o1, o2) -> o1.asCoreLocation().compareTo(o2.asCoreLocation()));
		System.out.printf("%d CPUs\n", cpu_infos.size());
		for (CPUInfo cpu_info : cpu_infos) {
			System.out.println(cpu_info);
		}
	}

	private void sync(Transceiver transceiver, int app_id) throws Exception {
		transceiver.sendSignal(app_id, Signal.SYNC0);
		int count = 0;
		while (count < 20) {
			count = transceiver.getCoreStateCount(app_id, CPUState.FINISHED);
			System.out.printf("Cores in state FINISHED=%d\n", count);
			Thread.sleep(100);
		}
	}

	private void iobufs(Transceiver transceiver) throws Exception {
		for (IOBuffer iobuf : transceiver.getIobuf(core_subsets)) {
			System.out.println(iobuf);
		}
	}

	private void stop(Transceiver transceiver, int app_id) throws Exception {
		transceiver.sendSignal(app_id, Signal.STOP);
		Thread.sleep(500);
		List<CPUInfo> cpu_infos = new ArrayList<>();
		transceiver.getCPUInformation(core_subsets).forEach(cpu_infos::add);
		Collections.sort(cpu_infos,
				(o1, o2) -> o1.asCoreLocation().compareTo(o2.asCoreLocation()));
		System.out.printf("%d CPUs\n", cpu_infos.size());
		for (CPUInfo cpu_info : cpu_infos) {
			System.out.println(cpu_info);
		}
	}

	private void iptags(Transceiver transceiver) throws Exception {
		InetAddress localhost = InetAddress.getLocalHost();

		transceiver.setIPTag(new IPTag(null, SCAMP, 1, localhost, 50000));
		transceiver.setIPTag(new IPTag(null, SCAMP, 2, localhost, 60000, true));
		transceiver.setReverseIPTag(
				new ReverseIPTag(null, 3, 40000, new CoreLocation(0, 1, 2)));
		for (Tag tag : transceiver.getTags()) {
			System.out.println(tag);
		}

		transceiver.clearIPTag(1);
		transceiver.clearIPTag(2);
		transceiver.clearIPTag(3);
		for (Tag tag : transceiver.getTags()) {
			System.out.println(tag);
		}
	}

	private void routes(Transceiver transceiver, int app_id) throws Exception {
		List<MulticastRoutingEntry> routes;

		routes = Collections.singletonList(new MulticastRoutingEntry(0x10000000,
				0xFFFF7000, Arrays.asList(1, 2, 3, 4, 5),
				Arrays.asList(0, 1, 2), false));
		transceiver.loadMulticastRoutes(new ChipLocation(0, 0), routes, app_id);

		routes = transceiver.getMulticastRoutes(new ChipLocation(0, 0), app_id);
		for (MulticastRoutingEntry route : routes) {
			System.out.printf("Key=%x, Mask=%x, processors=%s, links=%s\n",
					route.getKey(), route.getMask(),
					Arrays.toString(route.getProcessorIDs()),
					Arrays.toString(route.getLinkIDs()));
		}

		transceiver.clearMulticastRoutes(new ChipLocation(0, 0));
		routes = transceiver.getMulticastRoutes(new ChipLocation(0, 0));
		for (MulticastRoutingEntry route : routes) {
			System.out.printf("Key=%x, Mask=%x, processors=%s, links=%s\n",
					route.getKey(), route.getMask(),
					Arrays.toString(route.getProcessorIDs()),
					Arrays.toString(route.getLinkIDs()));
		}
	}

	private void diagnostics(Transceiver transceiver) throws Exception {
		// Set Router Diagnostic Filter
		List<Destination> destinations =
				asList(DiagnosticFilter.Destination.LINK_0,
						DiagnosticFilter.Destination.LINK_1,
						DiagnosticFilter.Destination.LINK_2,
						DiagnosticFilter.Destination.LINK_5);
		for (int i = 0; i < destinations.size(); i++) {
			DiagnosticFilter current_filter = new DiagnosticFilter(false, true,
					singletonList(destinations.get(i)), null, null, emptyList(),
					emptyList(),
					singletonList(DiagnosticFilter.PacketType.POINT_TO_POINT));
			transceiver.setRouterDiagnosticFilter(SCAMP, i + 12,
					current_filter);
		}

		// Clear Router Diagnostics
		transceiver.clearRouterDiagnosticCounters(SCAMP,
				Arrays.asList(RouterDiagnostics.RouterRegister.LOC_PP.ordinal(),
						RouterDiagnostics.RouterRegister.EXT_PP.ordinal()));
		RouterDiagnostics diagnostics = transceiver.getRouterDiagnostics(SCAMP);
		for (RouterRegister register : RouterDiagnostics.RouterRegister
				.values()) {
			System.out.printf("%s: %x\n", register.name(),
					diagnostics.registerValues[register.ordinal()]);
		}

		// Send read requests
		transceiver.sendSCPMessage(
				new ReadMemory(new ChipLocation(1, 0), MEM, 4), null);
		transceiver.sendSCPMessage(
				new ReadMemory(new ChipLocation(1, 1), MEM, 4), null);
		transceiver.sendSCPMessage(
				new ReadMemory(new ChipLocation(1, 1), MEM, 4), null);
		transceiver.sendSCPMessage(
				new ReadMemory(new ChipLocation(0, 1), MEM, 4), null);
		transceiver.sendSCPMessage(
				new ReadMemory(new ChipLocation(0, 1), MEM, 4), null);
		transceiver.sendSCPMessage(
				new ReadMemory(new ChipLocation(0, 1), MEM, 4), null);

		// Get Router Diagnostics
		diagnostics = transceiver.getRouterDiagnostics(SCAMP);
		for (RouterRegister register : RouterDiagnostics.RouterRegister
				.values()) {
			System.out.printf("%s: %x\n", register.name(),
					diagnostics.registerValues[register.ordinal()]);
		}

		// Get Router Diagnostic Filters
		for (int i = 0; i < 16; i++) {
			System.out.println("Filter " + i + ":");
			printFilter(transceiver.getRouterDiagnosticFilter(SCAMP, i));
			System.out.println();
		}
	}

	private void heap(Transceiver transceiver) throws Exception {
		for (HeapElement heap_element : transceiver.getHeap(SCAMP)) {
			System.out.println(heap_element);
		}
	}

	@Test
	void testTransceiver() throws Exception {
		try (Transceiver transceiver = Transceiver.createTransceiver(
				board_config.remotehost, board_config.board_version,
				board_config.bmp_names, null, down_chips, down_cores, null,
				null, board_config.auto_detect_bmp, null, null, null)) {

			section("Version Information", () -> boardReady(transceiver));

			int app_id = transceiver.getAppIdTracker().allocateNewID();

			section("Discovering other connections to the machine",
					() -> findConnections(transceiver));

			section("Machine Details", () -> retrieveDetails(transceiver));

			section("Memory Write and Read", () -> readWrite(transceiver));

			section("Flood Memory Write", () -> floodWrite(transceiver));

			section("Execute Flood", () -> execFlood(transceiver, app_id));

			section("CPU Information", () -> cpuInfo(transceiver));

			section("Send SYNC0", () -> sync(transceiver, app_id));

			section("Get IOBufs", () -> iobufs(transceiver));

			section("Stop Application", () -> stop(transceiver, app_id));

			section("Create and Clear IP Tags", () -> iptags(transceiver));

			section("Load and Clear Routes", () -> routes(transceiver, app_id));

			section("Router Diagnostic Filter Testing",
					() -> diagnostics(transceiver));

			/*
			 * 8-byte numbers have to be converted into bytebuffers to be
			 * written
			 */
			long longVal = 123456789123456789L;
			int intVal = 123456789;
			ByteBuffer longData = ByteBuffer.allocate(8).order(LITTLE_ENDIAN);
			longData.putLong(longVal).flip();

			section("Test reading/writing blobs", () -> {
				transceiver.writeMemory(SCAMP, MEM, longData);
				assertEquals(longVal,
						transceiver.readMemory(SCAMP, MEM, 8).getLong());
				transceiver.writeMemory(SCAMP, MEM, intVal);
				assertEquals(intVal,
						transceiver.readMemory(SCAMP, MEM, 4).getInt());
			});

			section("Test reading/writing blobs to neighbours", () -> {
				transceiver.writeNeighbourMemory(SCAMP, 0, MEM, longData);
				assertEquals(longVal, transceiver
						.readNeighbourMemory(SCAMP, 0, MEM, 8).getLong());
				transceiver.writeNeighbourMemory(SCAMP, 0, MEM, intVal);
				assertEquals(intVal, transceiver
						.readNeighbourMemory(SCAMP, 0, MEM, 4).getInt());
			});

			ChipLocation neighbour = new ChipLocation(1, 1);

			section("Test writing blobs by flood", () -> {
				transceiver.writeMemoryFlood(MEM, longData);
				assertEquals(longVal,
						transceiver.readMemory(SCAMP, MEM, 8).getLong());
				assertEquals(longVal,
						transceiver.readMemory(neighbour, MEM, 8).getLong());

				transceiver.writeMemoryFlood(MEM, intVal);
				assertEquals(intVal,
						transceiver.readMemory(SCAMP, MEM, 4).getInt());
				assertEquals(intVal,
						transceiver.readMemory(neighbour, MEM, 4).getInt());
			});

			section("Get Heap", () -> heap(transceiver));
		}
	}

}
