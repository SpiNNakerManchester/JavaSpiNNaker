/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.transceiver;

import static java.lang.Math.random;
import static java.lang.Thread.sleep;
import static java.net.InetAddress.getLocalHost;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.abort;
import static org.slf4j.LoggerFactory.getLogger;
import static testconfig.Utils.printEnumCollection;
import static testconfig.Utils.printWordAsBinary;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.FINISHED;
import static uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter.Destination.LINK_0;
import static uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter.Destination.LINK_1;
import static uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter.Destination.LINK_2;
import static uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter.Destination.LINK_5;
import static uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter.PacketType.POINT_TO_POINT;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.EXT_PP;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.LOC_PP;
import static uk.ac.manchester.spinnaker.messages.model.Signal.STOP;
import static uk.ac.manchester.spinnaker.transceiver.CommonMemoryLocations.BUFFERED_SDRAM_START;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;

import net.jcip.annotations.NotThreadSafe;
import testconfig.BoardTestConfiguration;
import testconfig.Utils.Field;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister;
import uk.ac.manchester.spinnaker.messages.model.Signal;
import uk.ac.manchester.spinnaker.messages.scp.ReadMemory;
import uk.ac.manchester.spinnaker.spalloc.SpallocJob;
import uk.ac.manchester.spinnaker.spalloc.exceptions.JobDestroyedException;

/**
 * Communications integration test.
 *
 * @author Andrew Rowley
 * @author Donal Fellows
 */
@org.junit.jupiter.api.Tag("integration")
@NotThreadSafe
public class TransceiverITCase {
	private static final Logger log = getLogger(TransceiverITCase.class);

	// TODO Stop printing to System.out
	private static BoardTestConfiguration boardConfig;

	private static SpallocJob job;

	private static Set<ChipLocation> downChips;

	private static CoreSubsets coreSubsets;

	private static Map<ChipLocation, Set<Integer>> downCores;

	@BeforeAll
	@Timeout(60) // Two minutes is enough
	@SuppressWarnings("MustBeClosed")
	static void setUpBeforeClass() throws Exception {
		boardConfig = new BoardTestConfiguration();
		try {
			job = boardConfig.setUpSpallocedBoard();
		} catch (JobDestroyedException e) {
			abort("could not set up job: " + e.getMessage());
		}
		coreSubsets = new CoreSubsets();
		coreSubsets.addCores(0, 0, range(1, 11).boxed().collect(toSet()));
		coreSubsets.addCores(1, 1, range(1, 11).boxed().collect(toSet()));

		downCores = Map.of(new ChipLocation(0, 0), Set.of(5));

		downChips = Set.of(new ChipLocation(0, 1));
	}

	@AfterAll
	static void tearDownAtTheEnd() throws Exception {
		if (job != null) {
			job.destroy();
			job.close();
		}
	}

	private static final Field[] FILTER_FIELDS = new Field[] {
		new Field(31), new Field(30), new Field(29), new Field(28, 25),
		new Field(24, 16), new Field(15, 14), new Field(13, 12),
		new Field(11, 10), new Field(9), new Field(8), new Field(7, 4),
		new Field(3, 0)
	};

	void printFilter(DiagnosticFilter filter) {
		printWordAsBinary("Filter word:", filter.getFilterWord(),
				FILTER_FIELDS);
		System.out.println("Enable Interrupt: "
				+ filter.getEnableInterruptOnCounterEvent());
		System.out.println("Emergency Routing Status on Incoming: "
				+ filter.getMatchEmergencyRoutingStatusToIncomingPacket());
		printEnumCollection("Destinations:", filter.getDestinations());
		printEnumCollection("Sources:", filter.getSources());
		printEnumCollection("Payloads:", filter.getPayloadStatuses());
		printEnumCollection("Default Routing:",
				filter.getDefaultRoutingStatuses());
		printEnumCollection("Emergency Routing:",
				filter.getEmergencyRoutingStatuses());
		printEnumCollection("Packet Types:", filter.getPacketTypes());
	}

	private static class Section implements AutoCloseable {
		Section(String msg) {
			var underline = new StringBuilder(msg.length());
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
		try (var s = new Section(title)) {
			call.call();
		}
	}

	private static final ChipLocation SCAMP = ZERO_ZERO;

	/** Where we like to read and write when testing. */
	private static final MemoryLocation MEM = BUFFERED_SDRAM_START;

	private void boardReady(Transceiver txrx) throws Exception {
		var versionInfo = txrx.ensureBoardIsReady();
		System.out.println(versionInfo);
	}

	private void findConnections(Transceiver txrx) throws Exception {
		var connections = txrx.discoverScampConnections();
		System.out.println(connections);
	}

	private void retrieveDetails(Transceiver txrx) throws Exception {
		var machine = txrx.getMachineDetails();
		System.out.println(machine);
		System.out.println(machine.bootChip());
	}

	private void readWrite(Transceiver txrx) throws Exception {
		var writeData = allocate(1000);
		while (writeData.hasRemaining()) {
			writeData.put((byte) (random() * 256));
		}
		writeData.flip();
		txrx.writeMemory(SCAMP, MEM, writeData);
		var readData = txrx.readMemory(SCAMP, MEM, 1000);
		assertEquals(writeData, readData);
	}

	private void floodWrite(Transceiver txrx) throws Exception {
		txrx.writeMemoryFlood(MEM, 0x04050607);
		var readData = txrx.readMemory(new ChipLocation(1, 1), MEM, 4);
		System.out.printf("%x\n", readData.getInt());
	}

	private void execFlood(Transceiver txrx, AppID appID) throws Exception {
		txrx.executeFlood(coreSubsets, new File("hello.aplx"), appID);
		int count = 0;
		while (count < 20) {
			count = txrx.getCoreStateCount(appID, CPUState.SYNC0);
			System.out.printf("Cores in state SYNC0=%d\n", count);
			sleep(100);
		}
	}

	private static List<CPUInfo> getCPUInfo(Transceiver txrx, CoreSubsets cores)
			throws Exception {
		var cpuInfos = new ArrayList<CPUInfo>();
		txrx.getCPUInformation(cores).forEach(cpuInfos::add);
		sort(cpuInfos,
			(o1, o2) -> o1.asCoreLocation().compareTo(o2.asCoreLocation()));
		return cpuInfos;
	}

	private void cpuInfo(Transceiver txrx) throws Exception {
		var cpuInfos = getCPUInfo(txrx, coreSubsets);
		System.out.printf("%d CPUs\n", cpuInfos.size());
		for (var cpuInfo : cpuInfos) {
			System.out.println(cpuInfo.getStatusDescription());
		}
	}

	private void sync(Transceiver txrx, AppID appID) throws Exception {
		txrx.sendSignal(appID, Signal.SYNC0);
		int count = 0;
		while (count < 20) {
			count = txrx.getCoreStateCount(appID, FINISHED);
			System.out.printf("Cores in state FINISHED=%d\n", count);
			sleep(100);
		}
	}

	private void iobufs(Transceiver txrx) throws Exception {
		for (var iobuf : txrx.getIobuf(coreSubsets)) {
			System.out.println(iobuf.getContentsString());
		}
	}

	private void stop(Transceiver txrx, AppID appID) throws Exception {
		txrx.sendSignal(appID, STOP);
		sleep(500);
		var cpuInfos = getCPUInfo(txrx, coreSubsets);
		System.out.printf("%d CPUs\n", cpuInfos.size());
		for (var cpuInfo : cpuInfos) {
			System.out.println(cpuInfo);
		}
	}

	private void iptags(Transceiver txrx) throws Exception {
		var localhost = getLocalHost();

		txrx.setIPTag(new IPTag(null, SCAMP, 1, localhost, 50000));
		txrx.setIPTag(new IPTag(null, SCAMP, 2, localhost, 60000, true));
		txrx.setReverseIPTag(
				new ReverseIPTag(null, 3, 40000, new CoreLocation(0, 1, 2)));
		for (var tag : txrx.getTags()) {
			System.out.println(tag);
		}

		txrx.clearIPTag(1);
		txrx.clearIPTag(2);
		txrx.clearIPTag(3);
		for (var tag : txrx.getTags()) {
			System.out.println(tag);
		}
	}

	private void routes(Transceiver txrx, AppID appID) throws Exception {
		var routes = List.of(new MulticastRoutingEntry(0x10000000, 0xFFFF7000,
				List.of(1, 2, 3, 4, 5), EnumSet.of(EAST, NORTHEAST, NORTH),
				false));
		txrx.loadMulticastRoutes(SCAMP, routes, appID);

		routes = txrx.getMulticastRoutes(SCAMP, appID);
		for (var route : routes) {
			System.out.printf("Key=%x, Mask=%x, processors=%s, links=%s\n",
					route.getKey(), route.getMask(), route.getProcessorIDs(),
					route.getLinkIDs());
		}

		txrx.clearMulticastRoutes(SCAMP);
		routes = txrx.getMulticastRoutes(SCAMP);
		for (var route : routes) {
			System.out.printf("Key=%x, Mask=%x, processors=%s, links=%s\n",
					route.getKey(), route.getMask(), route.getProcessorIDs(),
					route.getLinkIDs());
		}
	}

	private void diagnostics(Transceiver txrx) throws Exception {
		// Set Router Diagnostic Filter
		var destinations = List.of(LINK_0, LINK_1, LINK_2, LINK_5);
		for (int i = 0; i < destinations.size(); i++) {
			var filter = new DiagnosticFilter(false, true,
					List.of(destinations.get(i)), null, null, List.of(),
					List.of(), List.of(POINT_TO_POINT));
			txrx.setRouterDiagnosticFilter(SCAMP, i + 12, filter);
		}

		// Clear Router Diagnostics
		txrx.clearRouterDiagnosticCounters(SCAMP,
				List.of(LOC_PP.ordinal(), EXT_PP.ordinal()));
		var diagnostics = txrx.getRouterDiagnostics(SCAMP);
		for (var register : RouterRegister.values()) {
			System.out.printf("%s: %x\n", register.name(),
					diagnostics.registerValues[register.ordinal()]);
		}

		// Send read requests
		// TODO use one-way requests instead
		txrx.sendSCPMessage(
				new ReadMemory(new ChipLocation(1, 0), MEM, WORD_SIZE), null);
		txrx.sendSCPMessage(
				new ReadMemory(new ChipLocation(1, 1), MEM, WORD_SIZE), null);
		txrx.sendSCPMessage(
				new ReadMemory(new ChipLocation(1, 1), MEM, WORD_SIZE), null);
		txrx.sendSCPMessage(
				new ReadMemory(new ChipLocation(0, 1), MEM, WORD_SIZE), null);
		txrx.sendSCPMessage(
				new ReadMemory(new ChipLocation(0, 1), MEM, WORD_SIZE), null);
		txrx.sendSCPMessage(
				new ReadMemory(new ChipLocation(0, 1), MEM, WORD_SIZE), null);

		// Get Router Diagnostics
		diagnostics = txrx.getRouterDiagnostics(SCAMP);
		for (var register : RouterRegister.values()) {
			System.out.printf("%s: %x\n", register.name(),
					diagnostics.registerValues[register.ordinal()]);
		}

		// Get Router Diagnostic Filters
		for (int i = 0; i < 16; i++) {
			System.out.println("Filter " + i + ":");
			printFilter(txrx.getRouterDiagnosticFilter(SCAMP, i));
			System.out.println();
		}
	}

	private void heap(Transceiver txrx) throws Exception {
		for (var heapElement : txrx.getHeap(SCAMP)) {
			System.out.println(heapElement);
		}
	}

	@Test
	@Timeout(120) // Two minutes is enough
	@Disabled("not working right")
	public void testTransceiver() throws Exception {
		try (var txrx = new Transceiver(boardConfig.remotehost,
				boardConfig.boardVersion, boardConfig.bmpNames, null, downChips,
				downCores, null, boardConfig.autoDetectBMP, null, null, null)) {

			section("Version Information", () -> boardReady(txrx));

			var appID = txrx.getAppIdTracker().allocateNewID();

			section("Discovering other connections to the machine",
				() -> findConnections(txrx));
			section("Machine Details", () -> retrieveDetails(txrx));
			section("Memory Write and Read", () -> readWrite(txrx));
			section("Flood Memory Write", () -> floodWrite(txrx));
			section("Execute Flood", () -> execFlood(txrx, appID));
			section("CPU Information", () -> cpuInfo(txrx));
			section("Send SYNC0", () -> sync(txrx, appID));
			section("Get IOBufs", () -> iobufs(txrx));
			section("Stop Application", () -> stop(txrx, appID));
			section("Create and Clear IP Tags", () -> iptags(txrx));
			section("Load and Clear Routes", () -> routes(txrx, appID));
			section("Router Diagnostic Filter Testing",
				() -> diagnostics(txrx));

			/*
			 * 8-byte numbers have to be converted into bytebuffers to be
			 * written
			 */
			long longVal = 123456789123456789L;
			int intVal = 123456789;
			var longData = allocate(8).order(LITTLE_ENDIAN);
			longData.putLong(longVal).flip();

			section("Test reading/writing blobs", () -> {
				txrx.writeMemory(SCAMP, MEM, longData);
				assertEquals(longVal, txrx.readMemory(SCAMP, MEM, 8).getLong());
				txrx.writeMemory(SCAMP, MEM, intVal);
				assertEquals(intVal,
						txrx.readMemory(SCAMP, MEM, WORD_SIZE).getInt());
			});

			section("Test reading/writing blobs to neighbours", () -> {
				txrx.writeNeighbourMemory(SCAMP, EAST, MEM, longData);
				assertEquals(longVal, txrx
						.readNeighbourMemory(SCAMP, EAST, MEM, 8).getLong());
				txrx.writeNeighbourMemory(SCAMP, EAST, MEM, intVal);
				assertEquals(intVal,
						txrx.readNeighbourMemory(SCAMP, EAST, MEM, WORD_SIZE)
								.getInt());
			});

			var neighbour = new ChipLocation(1, 1);

			section("Test writing blobs by flood", () -> {
				txrx.writeMemoryFlood(MEM, longData);
				assertEquals(longVal, txrx.readMemory(SCAMP, MEM, 8).getLong());
				assertEquals(longVal,
						txrx.readMemory(neighbour, MEM, 8).getLong());

				txrx.writeMemoryFlood(MEM, intVal);
				assertEquals(intVal,
						txrx.readMemory(SCAMP, MEM, WORD_SIZE).getInt());
				assertEquals(intVal,
						txrx.readMemory(neighbour, MEM, WORD_SIZE).getInt());
			});

			section("Get Heap", () -> heap(txrx));
		}
	}

	public static void main(String... args) throws Exception {
		try {
			setUpBeforeClass();
		} catch (TestAbortedException e) {
			log.error("precondition violated", e);
			System.exit(1);
		}
		try {
			var test = new TransceiverITCase();
			test.testTransceiver();
		} finally {
			tearDownAtTheEnd();
		}
	}
}
