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

import static java.lang.Thread.sleep;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_USER_0_START_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_USER_1_START_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.CPU_USER_2_START_ADDRESS;
import static uk.ac.manchester.spinnaker.messages.Constants.NO_ROUTER_DIAGNOSTIC_FILTERS;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.AppID.DEFAULT;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.READY;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.RUN_TIME_EXCEPTION;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.WATCHDOG;
import static uk.ac.manchester.spinnaker.messages.model.Signal.START;
import static uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition.sdram_heap_address;
import static uk.ac.manchester.spinnaker.messages.scp.SCPRequest.BOOT_CHIP;
import static uk.ac.manchester.spinnaker.transceiver.FillDataType.WORD;
import static uk.ac.manchester.spinnaker.transceiver.Utils.getVcpuAddress;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.MustBeClosed;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SDPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.machine.Processor;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.machine.ValidP;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.machine.tags.TagID;
import uk.ac.manchester.spinnaker.messages.Constants;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter;
import uk.ac.manchester.spinnaker.messages.model.ExecutableTargets;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.IOBuffer;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.model.RouterTimeout;
import uk.ac.manchester.spinnaker.messages.model.Signal;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.utils.MappableIterable;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The interface supported by the {@link Transceiver}. Emulates a lot of default
 * handling and variant-type handling by Python.
 * <p>
 * Note that operations on a BMP are <strong>always</strong> thread-unsafe.
 *
 * @author Donal Fellows
 */
public interface TransceiverInterface extends BMPTransceiverInterface {
	/**
	 * Delay between starting a program on a core and checking to see if the
	 * core is ready for operational use. In milliseconds.
	 */
	int LAUNCH_DELAY = 500;

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
	Set<CPUState> DEFAULT_ERROR_STATES = Set.of(RUN_TIME_EXCEPTION, WATCHDOG);

	/**
	 * What proportion of checks are to be expensive full checks.
	 */
	int DEFAULT_CHECK_INTERVAL = 100;

	/** How many times to try booting a board. */
	int BOARD_BOOT_RETRIES = 5;

	/**
	 * @return The connection selector to use for SCP messages.
	 */
	@ParallelSafe
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
	@ParallelSafe
	void sendSCPMessage(@NotNull SCPRequest<?> message,
			@NotNull SCPConnection connection) throws IOException;

	/**
	 * Sends an SDP message using one of the connections.
	 *
	 * @param message
	 *            The message to send
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 */
	@ParallelSafe
	default void sendSDPMessage(@NotNull SDPMessage message)
			throws IOException {
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
	@ParallelSafe
	void sendSDPMessage(@NotNull SDPMessage message, SDPConnection connection)
			throws IOException;

	/**
	 * Get the maximum chip x-coordinate and maximum chip y-coordinate of the
	 * chips in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context unless previously called from a parallel-safe
	 * situation.
	 *
	 * @return The dimensions of the machine
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	@CheckReturnValue
	MachineDimensions getMachineDimensions()
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Get the details of the machine made up of chips on a board and how they
	 * are connected to each other.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context unless previously called from a parallel-safe
	 * situation.
	 *
	 * @return A machine description
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	@CheckReturnValue
	Machine getMachineDetails()
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Determines if the board can be contacted.
	 *
	 * @return True if the board can be contacted, False otherwise
	 */
	@ParallelSafe
	default boolean isConnected() {
		return isConnected(null);
	}

	/**
	 * Determines if the board can be contacted.
	 *
	 * @param connection
	 *            The connection which is to be tested. If {@code null}, all
	 *            connections will be tested, and the board will be considered
	 *            to be connected if any one connection works.
	 * @return True if the board can be contacted, False otherwise
	 */
	@ParallelSafe
	boolean isConnected(Connection connection);

	/**
	 * Get the version of SCAMP which is running on the board.
	 *
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default VersionInfo getScampVersion()
			throws IOException, ProcessException, InterruptedException {
		return getScampVersion(BOOT_CHIP, getScampConnectionSelector());
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default VersionInfo getScampVersion(
			@NotNull ConnectionSelector<SCPConnection> connectionSelector)
			throws IOException, ProcessException, InterruptedException {
		return getScampVersion(BOOT_CHIP, connectionSelector);
	}

	/**
	 * Get the version of SCAMP which is running on the board.
	 *
	 * @param chip
	 *            the coordinates of the chip to query for SCAMP version
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default VersionInfo getScampVersion(@Valid HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	VersionInfo getScampVersion(@Valid HasChipLocation chip,
			@NotNull ConnectionSelector<SCPConnection> connectionSelector)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Attempt to boot the board. No check is performed to see if the board is
	 * already booted.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	default void bootBoard() throws InterruptedException, IOException {
		bootBoard(null);
	}

	/**
	 * Attempt to boot the board. No check is performed to see if the board is
	 * already booted.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param extraBootValues
	 *            extra values to set during boot
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	void bootBoard(Map<SystemVariableDefinition, Object> extraBootValues)
			throws InterruptedException, IOException;

	/**
	 * Ensure that the board is ready to interact with this version of the
	 * transceiver. Boots the board if not already booted and verifies that the
	 * version of SCAMP running is compatible with this transceiver.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	@CheckReturnValue
	default VersionInfo ensureBoardIsReady()
			throws IOException, ProcessException, InterruptedException {
		return ensureBoardIsReady(BOARD_BOOT_RETRIES, null);
	}

	/**
	 * Ensure that the board is ready to interact with this version of the
	 * transceiver. Boots the board if not already booted and verifies that the
	 * version of SCAMP running is compatible with this transceiver.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param extraBootValues
	 *            Any additional values to set during boot
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	@CheckReturnValue
	default VersionInfo ensureBoardIsReady(
			Map<SystemVariableDefinition, Object> extraBootValues)
			throws IOException, ProcessException, InterruptedException {
		return ensureBoardIsReady(BOARD_BOOT_RETRIES, extraBootValues);
	}

	/**
	 * Ensure that the board is ready to interact with this version of the
	 * transceiver. Boots the board if not already booted and verifies that the
	 * version of SCAMP running is compatible with this transceiver.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param numRetries
	 *            The number of times to retry booting
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	default VersionInfo ensureBoardIsReady(@PositiveOrZero int numRetries)
			throws IOException, ProcessException, InterruptedException {
		return ensureBoardIsReady(numRetries, null);
	}

	/**
	 * Ensure that the board is ready to interact with this version of the
	 * transceiver. Boots the board if not already booted and verifies that the
	 * version of SCAMP running is compatible with this transceiver.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param numRetries
	 *            The number of times to retry booting
	 * @param extraBootValues
	 *            Any additional values to set during boot
	 * @return The version identifier
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	VersionInfo ensureBoardIsReady(@PositiveOrZero int numRetries,
			Map<SystemVariableDefinition, Object> extraBootValues)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Get information about the processors on the board.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @return An iterable of the CPU information for all cores.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	@CheckReturnValue
	default MappableIterable<CPUInfo> getCPUInformation()
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default CPUInfo getCPUInformation(@Valid HasCoreLocation core)
			throws IOException, ProcessException, InterruptedException {
		return getCPUInformation(new CoreSubsets(core)).first().orElseThrow();
	}

	/**
	 * Get information about some processors on the board.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context unless the {@code coreSubsets} only contains cores
	 * on a single board.
	 *
	 * @param coreSubsets
	 *            A set of chips and cores from which to get the information. If
	 *            {@code null}, the information from all of the cores on all of
	 *            the chips on the board are obtained.
	 * @return An iterable of the CPU information for the selected cores, or all
	 *         cores if {@code coreSubsets} is {@code null}.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	@CheckReturnValue
	MappableIterable<CPUInfo> getCPUInformation(@Valid CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Get the address of user<sub>0</sub> for a given processor on the board.
	 * <i>This does not read from the processor.</i>
	 *
	 * @param core
	 *            the coordinates of the core to get the user<sub>0</sub>
	 *            address for
	 * @return The address for user<sub>0</sub> register for this processor
	 */
	@ParallelSafe
	default MemoryLocation getUser0RegisterAddress(
			@Valid HasCoreLocation core) {
		return getVcpuAddress(core).add(CPU_USER_0_START_ADDRESS);
	}

	/**
	 * Get the address of user<sub>0</sub> for a given processor on the board.
	 * <i>This does not read from the processor.</i>
	 *
	 * @param p
	 *            the processor ID to get the user<sub>0</sub> address for
	 * @return The address for user<sub>0</sub> register for this processor
	 */
	@ParallelSafe
	default MemoryLocation getUser0RegisterAddress(@ValidP int p) {
		return getVcpuAddress(p).add(CPU_USER_0_START_ADDRESS);
	}

	/**
	 * Get the address of user<sub>0</sub> for a given processor on the board.
	 * <i>This does not read from the processor.</i>
	 *
	 * @param processor
	 *            the processor to get the user<sub>0</sub> address for
	 * @return The address for user<sub>0</sub> register for this processor
	 */
	@ParallelSafe
	default MemoryLocation getUser0RegisterAddress(@Valid Processor processor) {
		return getVcpuAddress(processor.processorId)
				.add(CPU_USER_0_START_ADDRESS);
	}

	/**
	 * Get the address of user<sub>1</sub> for a given processor on the board.
	 * <i>This does not read from the processor.</i>
	 *
	 * @param core
	 *            the coordinates of the core to get the user<sub>1</sub>
	 *            address for
	 * @return The address for user<sub>1</sub> register for this processor
	 */
	@ParallelSafe
	default MemoryLocation getUser1RegisterAddress(
			@Valid HasCoreLocation core) {
		return getVcpuAddress(core).add(CPU_USER_1_START_ADDRESS);
	}

	/**
	 * Get the address of user<sub>1</sub> for a given processor on the board.
	 * <i>This does not read from the processor.</i>
	 *
	 * @param p
	 *            the processor ID to get the user<sub>1</sub> address for
	 * @return The address for user<sub>1</sub> register for this processor;
	 *         this will be in System RAM and be accessible from any core of the
	 *         chip.
	 */
	@ParallelSafe
	default MemoryLocation getUser1RegisterAddress(@ValidP int p) {
		return getVcpuAddress(p).add(CPU_USER_1_START_ADDRESS);
	}

	/**
	 * Get the address of user<sub>1</sub> for a given processor on the board.
	 * <i>This does not read from the processor.</i>
	 *
	 * @param processor
	 *            the processor to get the user<sub>1</sub> address for
	 * @return The address for user<sub>1</sub> register for this processor;
	 *         this will be in System RAM and be accessible from any core of the
	 *         chip.
	 */
	@ParallelSafe
	default MemoryLocation getUser1RegisterAddress(@Valid Processor processor) {
		return getVcpuAddress(processor.processorId)
				.add(CPU_USER_1_START_ADDRESS);
	}

	/**
	 * Get the address of user<sub>2</sub> for a given processor on the board.
	 * <i>This does not read from the processor.</i>
	 *
	 * @param core
	 *            the coordinates of the core to get the user<sub>2</sub>
	 *            address for
	 * @return The address for user<sub>2</sub> register for this processor;
	 *         this will be in System RAM and be accessible from any core of the
	 *         chip.
	 */
	@ParallelSafe
	default MemoryLocation getUser2RegisterAddress(
			@Valid HasCoreLocation core) {
		return getVcpuAddress(core).add(CPU_USER_2_START_ADDRESS);
	}

	/**
	 * Get the address of user<sub>2</sub> for a given processor on the board.
	 * <i>This does not read from the processor.</i>
	 *
	 * @param p
	 *            the processor ID to get the user<sub>2</sub> address for
	 * @return The address for user<sub>0</sub> register for this processor
	 */
	@ParallelSafe
	default MemoryLocation getUser2RegisterAddress(@ValidP int p) {
		return getVcpuAddress(p).add(CPU_USER_2_START_ADDRESS);
	}

	/**
	 * Get the address of user<sub>2</sub> for a given processor on the board.
	 * <i>This does not read from the processor.</i>
	 *
	 * @param processor
	 *            the processor to get the user<sub>2</sub> address for
	 * @return The address for user<sub>0</sub> register for this processor
	 */
	@ParallelSafe
	default MemoryLocation getUser2RegisterAddress(@Valid Processor processor) {
		return getVcpuAddress(processor.processorId)
				.add(CPU_USER_2_START_ADDRESS);
	}

	/**
	 * Get the contents of the IOBUF buffer for all processors.
	 *
	 * @return An iterable of the buffers, order undetermined.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	@CheckReturnValue
	default MappableIterable<IOBuffer> getIobuf()
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default IOBuffer getIobuf(@Valid HasCoreLocation core)
			throws IOException, ProcessException, InterruptedException {
		return getIobuf(new CoreSubsets(core)).first().orElseThrow();
	}

	/**
	 * Get the contents of the IOBUF buffer for a collection of processors.
	 *
	 * @param coreSubsets
	 *            A set of chips and cores from which to get the buffers.
	 * @return An iterable of the buffers, which may not be in the order of
	 *         {@code coreSubsets}
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	@CheckReturnValue
	MappableIterable<IOBuffer> getIobuf(@Valid CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Clear the contents of the IOBUF buffer for all processors.
	 *
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	default void clearIobuf()
			throws IOException, ProcessException, InterruptedException {
		clearIobuf((CoreSubsets) null);
	}

	/**
	 * Clear the contents of the IOBUF buffer for a given core.
	 *
	 * @param core
	 *            The coordinates of the processor to clear the IOBUF on.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void clearIobuf(@Valid HasCoreLocation core)
			throws IOException, ProcessException, InterruptedException {
		clearIobuf(new CoreSubsets(core));
	}

	/**
	 * Clear the contents of the IOBUF buffer for a collection of processors.
	 *
	 * @param coreSubsets
	 *            A set of chips and cores on which to clear the buffers.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void clearIobuf(@Valid CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set the value of the watch dog timer on a specific chip.
	 *
	 * @param chip
	 *            coordinates of the chip to write new watchdog parameter to
	 * @param watchdog
	 *            value to set the timer count to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void setWatchDogTimeoutOnChip(@Valid HasChipLocation chip, int watchdog)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Enable or disable the watch dog timer on a specific chip.
	 *
	 * @param chip
	 *            coordinates of the chip to write new watchdog parameter to
	 * @param watchdog
	 *            whether to enable (True) or disable (False) the watchdog timer
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void enableWatchDogTimerOnChip(@Valid HasChipLocation chip,
			boolean watchdog)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set the value of the watch dog timer.
	 *
	 * @param watchdog
	 *            value to set the timer count to.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	default void setWatchDogTimeout(int watchdog)
			throws IOException, ProcessException, InterruptedException {
		for (var chip : getMachineDetails().chipCoordinates()) {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	default void enableWatchDogTimer(boolean watchdog)
			throws IOException, ProcessException, InterruptedException {
		for (var chip : getMachineDetails().chipCoordinates()) {
			enableWatchDogTimerOnChip(chip, watchdog);
		}
	}

	/**
	 * Get a count of the number of cores which have a given state.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param appID
	 *            The ID of the application from which to get the count.
	 * @param state
	 *            The state count to get
	 * @return A count of the cores with the given status
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	@CheckReturnValue
	int getCoreStateCount(@NotNull AppID appID, @NotNull CPUState state)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	default void execute(@Valid HasCoreLocation core,
			@NotNull InputStream executable, @Positive int numBytes,
			@NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException {
		execute(core, Set.of(core.getP()), executable, numBytes, appID);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	default void execute(@Valid HasChipLocation chip,
			Collection<@ValidP Integer> processors,
			@NotNull InputStream executable, @Positive int numBytes,
			@NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	default void execute(@Valid HasCoreLocation core,
			@NotNull InputStream executable, @Positive int numBytes,
			@NotNull AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		execute(core, Set.of(core.getP()), executable, numBytes, appID,
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	void execute(@Valid HasChipLocation chip,
			Collection<@ValidP Integer> processors,
			@NotNull InputStream executable, @Positive int numBytes,
			@NotNull AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	default void execute(@Valid HasCoreLocation core, @NotNull File executable,
			@NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException {
		execute(core, Set.of(core.getP()), executable, appID, false);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	default void execute(@Valid HasChipLocation chip,
			Collection<@ValidP Integer> processors, @NotNull File executable,
			@NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	default void execute(@Valid HasCoreLocation core, @NotNull File executable,
			@NotNull AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		execute(core, Set.of(core.getP()), executable, appID, wait);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	void execute(@Valid HasChipLocation chip,
			Collection<@ValidP Integer> processors, @NotNull File executable,
			@NotNull AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	default void execute(@Valid HasCoreLocation core,
			@NotNull ByteBuffer executable, @NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException {
		execute(core, Set.of(core.getP()), executable, appID, false);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	default void execute(@Valid HasChipLocation chip,
			Collection<@ValidP Integer> processors,
			@NotNull ByteBuffer executable, @NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	default void execute(@Valid HasCoreLocation core,
			@NotNull ByteBuffer executable, @NotNull AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		execute(core, Set.of(core.getP()), executable, appID, wait);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafe
	void execute(HasChipLocation chip, Collection<Integer> processors,
			ByteBuffer executable, AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void executeFlood(@Valid CoreSubsets coreSubsets,
			@NotNull InputStream executable, @Positive int numBytes,
			@NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException {
		executeFlood(coreSubsets, executable, numBytes, appID, false);
	}

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	void executeFlood(@Valid CoreSubsets coreSubsets,
			@NotNull InputStream executable, @Positive int numBytes,
			@NotNull AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void executeFlood(@Valid CoreSubsets coreSubsets,
			@NotNull File executable, @NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException {
		executeFlood(coreSubsets, executable, appID, false);
	}

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	void executeFlood(@Valid CoreSubsets coreSubsets, @NotNull File executable,
			@NotNull AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void executeFlood(@Valid CoreSubsets coreSubsets,
			@NotNull ByteBuffer executable, @NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException {
		executeFlood(coreSubsets, executable, appID, false);
	}

	/**
	 * Start an executable running on multiple places on the board. This will be
	 * optimised based on the selected cores, but it may still require a number
	 * of communications with the board to execute.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	void executeFlood(@Valid CoreSubsets coreSubsets,
			@NotNull ByteBuffer executable, @NotNull AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Execute a set of binaries that make up a complete application on
	 * specified cores, wait for them to be ready and then start all of the
	 * binaries. Note this will get the binaries into {@code c_main()} but will
	 * not signal the barrier.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param executableTargets
	 *            The binaries to be executed and the cores to execute them on
	 * @param appID
	 *            The application ID to give this application
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 * @throws SpinnmanException
	 *             If some cores enter an unexpected state.
	 */
	@ParallelUnsafe
	default void executeApplication(@Valid ExecutableTargets executableTargets,
			@NotNull AppID appID) throws IOException, ProcessException,
			InterruptedException, SpinnmanException {
		// Execute each of the binaries and get them in to a "wait" state
		for (var binary : executableTargets.getBinaries()) {
			executeFlood(executableTargets.getCoresForBinary(binary),
					new File(binary), appID, true);
		}

		// Sleep to allow cores to get going
		sleep(LAUNCH_DELAY);

		// Check that the binaries have reached a wait state
		int count = getCoreStateCount(appID, READY);
		if (count < executableTargets.getTotalProcessors()) {
			var coresNotReady = getCoresNotInState(
					executableTargets.getAllCoreSubsets(), READY);
			if (!coresNotReady.isEmpty()) {
				try (var f = new Formatter()) {
					f.format("Only %d of %d cores reached ready state:", count,
							executableTargets.getTotalProcessors());
					for (var info : coresNotReady.values()) {
						f.format("\n%s", info.getStatusDescription());
					}
					throw new SpinnmanException(f.toString());
				}
			}
		}

		// Send a signal telling the application to start
		sendSignal(appID, START);
	}

	/**
	 * Set the running time information for all processors.
	 *
	 * @param runTimesteps
	 *            How many machine timesteps will the run last. {@code null} is
	 *            used to indicate an infinite (unbounded until explicitly
	 *            stopped) run.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	default void updateRuntime(@PositiveOrZero Integer runTimesteps)
			throws IOException, ProcessException, InterruptedException {
		updateRuntime(runTimesteps, (CoreSubsets) null);
	}

	/**
	 * Set the running time information for a given core.
	 *
	 * @param runTimesteps
	 *            How many machine timesteps will the run last. {@code null} is
	 *            used to indicate an infinite (unbounded until explicitly
	 *            stopped) run.
	 * @param core
	 *            The coordinates of the processor to clear the IOBUF on.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void updateRuntime(@PositiveOrZero Integer runTimesteps,
			@Valid HasCoreLocation core)
			throws IOException, ProcessException, InterruptedException {
		updateRuntime(runTimesteps, new CoreSubsets(core));
	}

	/**
	 * Set the running time information for a collection of processors.
	 *
	 * @param runTimesteps
	 *            How many machine timesteps will the run last. {@code null} is
	 *            used to indicate an infinite (unbounded until explicitly
	 *            stopped) run.
	 * @param coreSubsets
	 *            A set of chips and cores on which to set the running time.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void updateRuntime(@PositiveOrZero Integer runTimesteps,
			@Valid CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Tell all running application cores to write their provenance data to a
	 * location where it can be read back, and then to go into the
	 * {@link CPUState#FINISHED FINISHED} state.
	 *
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	default void updateProvenanceAndExit()
			throws IOException, ProcessException, InterruptedException {
		updateProvenanceAndExit((CoreSubsets) null);
	}

	/**
	 * Tell a running application core to write its provenance data to a
	 * location where it can be read back, and then to go into the
	 * {@link CPUState#FINISHED FINISHED} state.
	 *
	 * @param core
	 *            The core to tell to finish.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void updateProvenanceAndExit(@Valid HasCoreLocation core)
			throws IOException, ProcessException, InterruptedException {
		updateProvenanceAndExit(new CoreSubsets(core));
	}

	/**
	 * Tell some running application cores to write their provenance data to a
	 * location where it can be read back, and then to go into their
	 * {@link CPUState#FINISHED FINISHED} state.
	 *
	 * @param coreSubsets
	 *            A set of cores tell to finish.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void updateProvenanceAndExit(@Valid CoreSubsets coreSubsets)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void writeMemory(@Valid HasChipLocation chip,
			@NotNull MemoryLocation baseAddress,
			@NotNull InputStream dataStream, @Positive int numBytes)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void writeMemory(@Valid HasCoreLocation core,
			@NotNull MemoryLocation baseAddress,
			@NotNull InputStream dataStream, @Positive int numBytes)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void writeMemory(@Valid HasChipLocation chip,
			@NotNull MemoryLocation baseAddress, @NotNull File dataFile)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void writeMemory(@Valid HasCoreLocation core,
			@NotNull MemoryLocation baseAddress, @NotNull File dataFile)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void writeMemory(@Valid HasChipLocation chip,
			@NotNull MemoryLocation baseAddress, int dataWord)
			throws IOException, ProcessException, InterruptedException {
		writeMemory(chip.getScampCore(), baseAddress, dataWord);
	}

	/**
	 * Write to the SDRAM (or System RAM) on the board.
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void writeMemory(@Valid HasCoreLocation core,
			@NotNull MemoryLocation baseAddress, int dataWord)
			throws IOException, ProcessException, InterruptedException {
		var b = allocate(WORD_SIZE).order(LITTLE_ENDIAN);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void writeMemory(@Valid HasChipLocation chip,
			@NotNull MemoryLocation baseAddress, @NotEmpty byte[] data)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void writeMemory(@Valid HasCoreLocation core,
			@NotNull MemoryLocation baseAddress, @NotEmpty byte[] data)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void writeMemory(@Valid HasChipLocation chip,
			@NotNull MemoryLocation baseAddress, @NotNull ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		writeMemory(chip.getScampCore(), baseAddress, data);
	}

	/**
	 * Write to the SDRAM (or System RAM) on the board.
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void writeMemory(@Valid HasCoreLocation core,
			@NotNull MemoryLocation baseAddress, @NotNull ByteBuffer data)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Write to the user<sub>0</sub> register of a core.
	 *
	 * @param core
	 *            The coordinates of the core whose register is to be
	 *            written to
	 * @param value
	 *            The word of data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	default void writeUser0(@Valid HasCoreLocation core, int value)
			throws ProcessException, IOException, InterruptedException {
		writeMemory(core.getScampCore(), getUser0RegisterAddress(core), value);
	}

	/**
	 * Write to the user<sub>0</sub> register of a core.
	 *
	 * @param core
	 *            The coordinates of the core whose register is to be
	 *            written to
	 * @param pointer
	 *            The pointer/address that is to be written to the register.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	default void writeUser0(@Valid HasCoreLocation core,
			@NotNull MemoryLocation pointer)
			throws ProcessException, IOException, InterruptedException {
		writeMemory(core.getScampCore(), getUser0RegisterAddress(core),
				pointer.address);
	}

	/**
	 * Write to the user<sub>1</sub> register of a core.
	 *
	 * @param core
	 *            The coordinates of the core whose register is to be
	 *            written to
	 * @param value
	 *            The word of data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	default void writeUser1(@Valid HasCoreLocation core, int value)
			throws ProcessException, IOException, InterruptedException {
		writeMemory(core.getScampCore(), getUser1RegisterAddress(core), value);
	}

	/**
	 * Write to the user<sub>1</sub> register of a core.
	 *
	 * @param core
	 *            The coordinates of the core whose register is to be written to
	 * @param pointer
	 *            The pointer/address that is to be written to the register.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	default void writeUser1(@Valid HasCoreLocation core,
			@NotNull MemoryLocation pointer)
			throws ProcessException, IOException, InterruptedException {
		writeMemory(core.getScampCore(), getUser1RegisterAddress(core),
				pointer.address);
	}

	/**
	 * Write to the user<sub>2</sub> register of a core.
	 *
	 * @param core
	 *            The coordinates of the core whose register is to be
	 *            written to
	 * @param value
	 *            The word of data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	default void writeUser2(@Valid HasCoreLocation core, int value)
			throws ProcessException, IOException, InterruptedException {
		writeMemory(core.getScampCore(), getUser2RegisterAddress(core), value);
	}

	/**
	 * Write to the user<sub>2</sub> register of a core.
	 *
	 * @param core
	 *            The coordinates of the core whose register is to be written to
	 * @param pointer
	 *            The pointer/address that is to be written to the register.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	default void writeUser2(@Valid HasCoreLocation core,
			@NotNull MemoryLocation pointer)
			throws ProcessException, IOException, InterruptedException {
		writeMemory(core.getScampCore(), getUser2RegisterAddress(core),
				pointer.address);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_WRITE SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link direction to send the request along
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void writeNeighbourMemory(@Valid HasChipLocation chip,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			@NotNull InputStream dataStream, @Positive int numBytes)
			throws IOException, ProcessException, InterruptedException {
		writeNeighbourMemory(chip.getScampCore(), link, baseAddress, dataStream,
				numBytes);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_WRITE SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link direction to send the request along
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void writeNeighbourMemory(@Valid HasCoreLocation core,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			@NotNull InputStream dataStream, @Positive int numBytes)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Write to the memory of a neighbouring chip using a LINK_WRITE SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link direction to send the request along
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The file holding the data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void writeNeighbourMemory(@Valid HasChipLocation chip,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			@NotNull File dataFile)
			throws IOException, ProcessException, InterruptedException {
		writeNeighbourMemory(chip.getScampCore(), link, baseAddress, dataFile);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_WRITE SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link direction to send the request along
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The name of the file holding the data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void writeNeighbourMemory(@Valid HasCoreLocation core,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			@NotNull File dataFile)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Write to the memory of a neighbouring chip using a LINK_WRITE SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link direction to send the request along
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void writeNeighbourMemory(@Valid HasChipLocation chip,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			int dataWord)
			throws IOException, ProcessException, InterruptedException {
		writeNeighbourMemory(chip.getScampCore(), link, baseAddress, dataWord);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_WRITE SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link direction to send the request along
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void writeNeighbourMemory(@Valid HasCoreLocation core,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			int dataWord)
			throws IOException, ProcessException, InterruptedException {
		var b = allocate(WORD_SIZE).order(LITTLE_ENDIAN);
		b.putInt(dataWord).flip();
		writeNeighbourMemory(core, link, baseAddress, b);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_WRITE SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link direction to send the request along
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void writeNeighbourMemory(@Valid HasChipLocation chip,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			@NotEmpty byte[] data)
			throws IOException, ProcessException, InterruptedException {
		writeNeighbourMemory(chip.getScampCore(), link, baseAddress, data);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_WRITE SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link direction to send the request along
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void writeNeighbourMemory(@Valid HasCoreLocation core,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			@NotEmpty byte[] data)
			throws IOException, ProcessException, InterruptedException {
		writeNeighbourMemory(core, link, baseAddress, wrap(data));
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_WRITE SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be written
	 *            to
	 * @param link
	 *            The link direction to send the request along
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. The data should be from the
	 *            <i>position</i> to the <i>limit</i>.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void writeNeighbourMemory(@Valid HasChipLocation chip,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			@NotNull ByteBuffer data)
			throws IOException, ProcessException, InterruptedException {
		writeNeighbourMemory(chip.getScampCore(), link, baseAddress, data);
	}

	/**
	 * Write to the memory of a neighbouring chip using a LINK_WRITE SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param core
	 *            The coordinates of the core whose neighbour is to be written
	 *            to; the CPU to use is typically 0 (or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link direction to send the request along
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. The data should be from the
	 *            <i>position</i> to the <i>limit</i>.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void writeNeighbourMemory(@Valid HasCoreLocation core,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			@NotNull ByteBuffer data)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Write to the SDRAM of all chips.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context. It has interlocking, but you should not rely on
	 * it.
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	void writeMemoryFlood(@NotNull MemoryLocation baseAddress,
			@NotNull InputStream dataStream, @Positive int numBytes)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Write to the SDRAM of all chips.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context. It has interlocking, but you should not rely on
	 * it.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataFile
	 *            The name of the file holding the data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking or with reading from
	 *             the file.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	void writeMemoryFlood(@NotNull MemoryLocation baseAddress,
			@NotNull File dataFile)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Write to the SDRAM of all chips.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context. It has interlocking, but you should not rely on
	 * it.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param dataWord
	 *            The word that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	default void writeMemoryFlood(@NotNull MemoryLocation baseAddress,
			int dataWord)
			throws IOException, ProcessException, InterruptedException {
		var b = allocate(WORD_SIZE).order(LITTLE_ENDIAN);
		b.putInt(dataWord).flip();
		writeMemoryFlood(baseAddress, b);
	}

	/**
	 * Write to the SDRAM of all chips.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context. It has interlocking, but you should not rely on
	 * it.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	default void writeMemoryFlood(@NotNull MemoryLocation baseAddress,
			@NotEmpty byte[] data)
			throws IOException, ProcessException, InterruptedException {
		writeMemoryFlood(baseAddress, wrap(data));
	}

	/**
	 * Write to the SDRAM of all chips.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context. It has interlocking, but you should not rely on
	 * it.
	 *
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory is to be
	 *            written
	 * @param data
	 *            The data that is to be written. The data should be from the
	 *            <i>position</i> to the <i>limit</i>.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	void writeMemoryFlood(@NotNull MemoryLocation baseAddress,
			@NotNull ByteBuffer data)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default ByteBuffer readMemory(@Valid HasChipLocation chip,
			@NotNull MemoryLocation baseAddress, @Positive int length)
			throws IOException, ProcessException, InterruptedException {
		return readMemory(chip.getScampCore(), baseAddress, length);
	}

	/**
	 * Read some areas of SDRAM (or System RAM) from the board.
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	ByteBuffer readMemory(@Valid HasCoreLocation core,
			@NotNull MemoryLocation baseAddress, @Positive int length)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Read the contents of an allocated block on the heap from the board. The
	 * SDRAM heap can be read from any core of that chip; the DTCM heap can only
	 * be read from one particular core.
	 *
	 * @param core
	 *            The coordinates of the core where the memory is to be read
	 *            from
	 * @param element
	 *            The heap element to read the contents of
	 * @return A little-endian buffer of data read, positioned at the start of
	 *         the data, or {@code null} if the heap element is free.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default ByteBuffer readMemory(@Valid HasCoreLocation core,
			@NotNull HeapElement element)
			throws IOException, ProcessException, InterruptedException {
		if (element.isFree) {
			return null;
		}
		return readMemory(core, element.getDataAddress(), element.size);
	}

	/**
	 * Read an area associated with a <em>recording region</em> from SDRAM from
	 * a core of a chip on the board.
	 *
	 * @param region
	 *            The recording region that is being read. Describes which core
	 *            produced the data, what <em>DSE index</em> the data came from,
	 *            and where in memory to actually read.
	 * @param storage
	 *            The database to write to.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws StorageException
	 *             If anything goes wrong with access to the database.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void readRegion(@Valid BufferManagerStorage.Region region,
			@NotNull BufferManagerStorage storage) throws IOException,
			ProcessException, StorageException, InterruptedException;

	/**
	 * Read the user<sub>0</sub> register of a core.
	 *
	 * @param core
	 *            The coordinates of the core to read the register of
	 * @return The contents of the register
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@CheckReturnValue
	default int readUser0(@Valid HasCoreLocation core)
			throws ProcessException, IOException, InterruptedException {
		var user0 = getUser0RegisterAddress(core);
		return readMemory(core.getScampCore(), user0, WORD_SIZE).getInt();
	}

	/**
	 * Read the user<sub>1</sub> register of a core.
	 *
	 * @param core
	 *            The coordinates of the core to read the register of
	 * @return The contents of the register
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@CheckReturnValue
	default int readUser1(@Valid HasCoreLocation core)
			throws ProcessException, IOException, InterruptedException {
		var user1 = getUser1RegisterAddress(core);
		return readMemory(core.getScampCore(), user1, WORD_SIZE).getInt();
	}

	/**
	 * Read the user<sub>2</sub> register of a core.
	 *
	 * @param core
	 *            The coordinates of the core to read the register of
	 * @return The contents of the register
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@CheckReturnValue
	default int readUser2(@Valid HasCoreLocation core)
			throws ProcessException, IOException, InterruptedException {
		var user2 = getUser2RegisterAddress(core);
		return readMemory(core.getScampCore(), user2, WORD_SIZE).getInt();
	}

	/**
	 * Read some areas of memory on a neighbouring chip using a LINK_READ SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param chip
	 *            The coordinates of the chip whose neighbour is to be read from
	 * @param link
	 *            The link direction to send the request along
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory to be read
	 *            starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data that has been read, positioned at
	 *         the start of the data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default ByteBuffer readNeighbourMemory(@Valid HasChipLocation chip,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			@Positive int length)
			throws IOException, ProcessException, InterruptedException {
		return readNeighbourMemory(chip.getScampCore(), link, baseAddress,
				length);
	}

	/**
	 * Read some areas of memory on a neighbouring chip using a LINK_READ SCP
	 * command. If sent to a BMP, this command can be used to communicate with
	 * the FPGAs' debug registers; in that case, the link must be the direction
	 * with the same ID as the ID of the FPGA to communicate with.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the link leaves the current board.
	 *
	 * @param core
	 *            The coordinates of the chip whose neighbour is to be read
	 *            from, plus the CPU to use (typically 0, or if a BMP, the slot
	 *            number)
	 * @param link
	 *            The link direction to send the request along
	 * @param baseAddress
	 *            The address in SDRAM where the region of memory to be read
	 *            starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data that has been read, positioned at
	 *         the start of the data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	ByteBuffer readNeighbourMemory(@Valid HasCoreLocation core,
			@NotNull Direction link, @NotNull MemoryLocation baseAddress,
			@Positive int length)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Sends a stop request for an application ID.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param appID
	 *            The ID of the application to send to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	void stopApplication(@NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Waits for the specified cores running the given application to be in some
	 * target state or states. Handles failures.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the cores are over more than a single board.
	 *
	 * @param coreSubsets
	 *            the cores to check are in a given sync state
	 * @param appID
	 *            the application ID that being used by the simulation
	 * @param cpuStates
	 *            The expected states once the applications are ready; success
	 *            is when each application is in one of these states
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 * @throws SpinnmanException
	 *             If some cores enter an error state or SpiNNaker rejects a
	 *             message.
	 */
	@ParallelSafeWithCare
	default void waitForCoresToBeInState(@Valid CoreSubsets coreSubsets,
			@NotNull AppID appID, Set<@NotNull CPUState> cpuStates)
			throws IOException, InterruptedException, SpinnmanException {
		waitForCoresToBeInState(coreSubsets, appID, cpuStates, TIMEOUT_DISABLED,
				DEFAULT_POLL_INTERVAL, DEFAULT_ERROR_STATES,
				DEFAULT_CHECK_INTERVAL);
	}

	/**
	 * Waits for the specified cores running the given application to be in some
	 * target state or states. Handles failures.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context if the cores are over more than a single board.
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
	 *            reach one of the states, or {@code null} to wait for an
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
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 * @throws SpinnmanException
	 *             If some cores enter an error state or SpiNNaker rejects a
	 *             message.
	 */
	@ParallelSafeWithCare
	void waitForCoresToBeInState(@Valid CoreSubsets allCoreSubsets,
			@NotNull AppID appID, Set<@NotNull CPUState> cpuStates,
			@PositiveOrZero Integer timeout, @Positive int timeBetweenPolls,
			Set<@NotNull CPUState> errorStates,
			@Positive int countsBetweenFullCheck)
			throws IOException, InterruptedException, SpinnmanException;

	/**
	 * Get all cores that are in a given state.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context unless the {@code allCoreSubsets} only contains
	 * cores on a single board.
	 *
	 * @param allCoreSubsets
	 *            The cores to filter
	 * @param state
	 *            The states to filter on
	 * @return Core subsets object containing cores in the given state
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	@CheckReturnValue
	default CoreSubsets getCoresInState(@Valid CoreSubsets allCoreSubsets,
			@NotNull CPUState state)
			throws IOException, ProcessException, InterruptedException {
		return getCoresInState(allCoreSubsets, Set.of(state));
	}

	/**
	 * Get all cores that are in a given set of states.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context unless the {@code allCoreSubsets} only contains
	 * cores on a single board.
	 *
	 * @param allCoreSubsets
	 *            The cores to filter
	 * @param states
	 *            The states to filter on
	 * @return Core subsets object containing cores in the given states
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	@CheckReturnValue
	default CoreSubsets getCoresInState(@Valid CoreSubsets allCoreSubsets,
			Set<@NotNull CPUState> states)
			throws IOException, ProcessException, InterruptedException {
		return new CoreSubsets(getCPUInformation(allCoreSubsets)
				.filter(info -> states.contains(info.getState()))
				.map(CPUInfo::asCoreLocation));
	}

	/**
	 * Get all cores that are not in a given state.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context unless the {@code allCoreSubsets} only contains
	 * cores on a single board.
	 *
	 * @param allCoreSubsets
	 *            The cores to filter
	 * @param state
	 *            The state to filter on
	 * @return Core subsets object containing cores not in the given state
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	@CheckReturnValue
	default Map<CoreLocation, CPUInfo> getCoresNotInState(
			@Valid CoreSubsets allCoreSubsets, @NotNull CPUState state)
			throws IOException, ProcessException, InterruptedException {
		return getCoresNotInState(allCoreSubsets, Set.of(state));
	}

	/**
	 * Get all cores that are not in a given set of states.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context unless the {@code allCoreSubsets} only contains
	 * cores on a single board.
	 *
	 * @param allCoreSubsets
	 *            The cores to filter
	 * @param states
	 *            The states to filter on
	 * @return Core subsets object containing cores not in the given states
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	@CheckReturnValue
	default Map<CoreLocation, CPUInfo> getCoresNotInState(
			@Valid CoreSubsets allCoreSubsets, Set<@NotNull CPUState> states)
			throws IOException, ProcessException, InterruptedException {
		return getCPUInformation(allCoreSubsets)
				.filter(info -> !states.contains(info.getState()))
				.toMap(TreeMap::new, CPUInfo::asCoreLocation, identity());
	}

	/**
	 * Send a signal to an application.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param appID
	 *            The ID of the application to send to
	 * @param signal
	 *            The signal to send
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	void sendSignal(@NotNull AppID appID, @NotNull Signal signal)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void setLEDs(@Valid HasChipLocation chip,
			Map<Integer, LEDAction> ledStates)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void setLEDs(@Valid HasCoreLocation core, Map<Integer, LEDAction> ledStates)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Find a connection that matches the given board IP address.
	 *
	 * @param boardAddress
	 *            The IP address of the Ethernet connection on the board
	 * @return A connection for the given IP address, or {@code null} if no such
	 *         connection exists
	 */
	@ParallelSafe
	SCPConnection locateSpinnakerConnection(@NotNull InetAddress boardAddress);

	/**
	 * Set up an IP tag.
	 *
	 * @param tag
	 *            The tag to set up; note its board address can be {@code null},
	 *            in which case, the tag will be assigned to all boards.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void setIPTag(@Valid IPTag tag)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set up an IP tag to deliver messages to a particular connection.
	 *
	 * @param tag
	 *            The tag to set up.
	 * @param connection
	 *            The connection to deliver messages to, which must already be
	 *            set up to talk to the correct board.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void setIPTag(@Valid IPTag tag, @NotNull SDPConnection connection)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set up a reverse IP tag.
	 *
	 * @param tag
	 *            The reverse tag to set up; note its board address can be
	 *            {@code null}, in which case, the tag will be assigned to all
	 *            boards
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void setReverseIPTag(@Valid ReverseIPTag tag)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void clearIPTag(@Valid Tag tag)
			throws IOException, ProcessException, InterruptedException {
		clearIPTag(tag.getTag(), tag.getBoardAddress());
	}

	/**
	 * Clear the setting of an IP tag.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param tag
	 *            The tag ID
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	default void clearIPTag(@TagID int tag)
			throws IOException, ProcessException, InterruptedException {
		clearIPTag(tag, null);
	}

	/**
	 * Clear the setting of an IP tag.
	 *
	 * @param tag
	 *            The tag ID
	 * @param boardAddress
	 *            Board address where the tag should be cleared. If
	 *            {@code null}, all SCPSender connections will send the message
	 *            to clear the tag
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void clearIPTag(@TagID int tag, InetAddress boardAddress)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Get the current set of tags that have been set on the board using all
	 * SCPSender connections.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @return An iterable of tags
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	@CheckReturnValue
	default List<Tag> getTags()
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	List<Tag> getTags(SCPConnection connection)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Get the number of times each tag has had a message sent via it using all
	 * SCPSender connections.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @return A map from the tags to their usage.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelUnsafe
	@CheckReturnValue
	default Map<Tag, Integer> getTagUsage()
			throws IOException, ProcessException, InterruptedException {
		return getTagUsage(null);
	}

	/**
	 * Get the number of times each tag has had a message sent via it for a
	 * connection.
	 *
	 * @param connection
	 *            Connection from which the tag usage should be retrieved.
	 * @return A map from the tags to their usage.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	Map<Tag, Integer> getTagUsage(@NotNull SCPConnection connection)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default MemoryLocation mallocSDRAM(@Valid HasChipLocation chip,
			@Positive int size)
			throws IOException, ProcessException, InterruptedException {
		return mallocSDRAM(chip, size, DEFAULT, 0);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default MemoryLocation mallocSDRAM(@Valid HasChipLocation chip,
			@Positive int size, @NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	MemoryLocation mallocSDRAM(@Valid HasChipLocation chip, @Positive int size,
			@NotNull AppID appID, int tag)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Free allocated SDRAM.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to free memory
	 * @param baseAddress
	 *            The base address of the allocated memory
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void freeSDRAM(@Valid HasChipLocation chip,
			@NotNull MemoryLocation baseAddress)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Free all SDRAM allocated to a given application ID.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to free memory
	 * @param appID
	 *            The app ID of the owner of the allocated memory
	 * @return The number of blocks freed
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	int freeSDRAM(@Valid HasChipLocation chip, @NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void loadMulticastRoutes(@Valid HasChipLocation chip,
			Collection<@NotNull MulticastRoutingEntry> routes)
			throws IOException, ProcessException, InterruptedException {
		loadMulticastRoutes(chip, routes, DEFAULT);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void loadMulticastRoutes(@Valid HasChipLocation chip,
			Collection<@NotNull MulticastRoutingEntry> routes,
			@NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Loads a fixed route routing table entry onto a chip's router.
	 *
	 * @param chip
	 *            The coordinates of the chip onto which to load the route
	 * @param fixedRoute
	 *            the route for the fixed route entry on this chip
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void loadFixedRoute(@Valid HasChipLocation chip,
			@Valid RoutingEntry fixedRoute)
			throws IOException, ProcessException, InterruptedException {
		loadFixedRoute(chip, fixedRoute, DEFAULT);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void loadFixedRoute(@Valid HasChipLocation chip,
			@Valid RoutingEntry fixedRoute, @NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Reads a fixed route routing table entry from a chip's router.
	 *
	 * @param chip
	 *            The coordinate of the chip from which to read the route.
	 * @return the route as a fixed route entry
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default RoutingEntry readFixedRoute(@Valid HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException {
		return readFixedRoute(chip, DEFAULT);
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	RoutingEntry readFixedRoute(@Valid HasChipLocation chip,
			@NotNull AppID appID)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Get the current multicast routes set up on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip from which to get the routes
	 * @return An iterable of multicast routes
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default List<MulticastRoutingEntry> getMulticastRoutes(
			@Valid HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException {
		return getMulticastRoutes(chip, null);
	}

	/**
	 * Get the current multicast routes set up on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip from which to get the routes
	 * @param appID
	 *            The ID of the application to filter the routes for.
	 *            {@code null} means "don't filter".
	 * @return An iterable of multicast routes
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	List<MulticastRoutingEntry> getMulticastRoutes(@Valid HasChipLocation chip,
			AppID appID)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Remove all the multicast routes on a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip on which to clear the routes
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void clearMulticastRoutes(@Valid HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Get router diagnostic information from a chip.
	 *
	 * @param chip
	 *            The coordinates of the chip from which to get the information
	 * @return The router diagnostic information
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	RouterDiagnostics getRouterDiagnostics(@Valid HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void setRouterDiagnosticFilter(@Valid HasChipLocation chip, int position,
			@NotNull DiagnosticFilter diagnosticFilter)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	DiagnosticFilter getRouterDiagnosticFilter(@Valid HasChipLocation chip,
			@PositiveOrZero int position)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Clear router diagnostic information on a chip. Resets and enables all
	 * diagnostic counters.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void clearRouterDiagnosticCounters(@Valid HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void clearRouterDiagnosticCounters(@Valid HasChipLocation chip,
			boolean enable)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void clearRouterDiagnosticCounters(@Valid HasChipLocation chip,
			Iterable<@NotNull Integer> counterIDs)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void clearRouterDiagnosticCounters(@Valid HasChipLocation chip,
			boolean enable, Iterable<@NotNull Integer> counterIDs)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Get the contents of the SDRAM heap on a given chip.
	 *
	 * @param chip
	 *            The coordinates of the chip
	 * @return the list of chunks in the heap
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	default List<HeapElement> getHeap(@Valid HasChipLocation chip)
			throws IOException, ProcessException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	List<HeapElement> getHeap(@Valid HasChipLocation chip,
			@NotNull SystemVariableDefinition heap)
			throws IOException, ProcessException, InterruptedException;

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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void fillMemory(@Valid HasChipLocation chip,
			@NotNull MemoryLocation baseAddress, int repeatValue,
			@Positive int size)
			throws ProcessException, IOException, InterruptedException {
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
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void fillMemory(@Valid HasChipLocation chip,
			@NotNull MemoryLocation baseAddress, int repeatValue,
			@Positive int size, @NotNull FillDataType dataType)
			throws ProcessException, IOException, InterruptedException;

	/**
	 * Clear the packet reinjection queues in a monitor process.
	 *
	 * @param monitorCore
	 *            The coordinates of the monitor core.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void clearReinjectionQueues(@Valid HasCoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Clear the packet reinjection queues in some monitor processes.
	 *
	 * @param monitorCores
	 *            The coordinates of the monitor cores.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void clearReinjectionQueues(@Valid CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Get the packet reinjection status of a monitor process.
	 *
	 * @param monitorCore
	 *            The coordinates of the monitor core.
	 * @return The reinjection status.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	ReinjectionStatus getReinjectionStatus(@Valid HasCoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Get the packet reinjection status of some monitor processes.
	 *
	 * @param monitorCores
	 *            The coordinates of the monitor cores.
	 * @return The reinjection statuses of the cores.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	@CheckReturnValue
	Map<CoreLocation, ReinjectionStatus> getReinjectionStatus(
			@Valid CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Reset the packet reinjection counters of a monitor process.
	 *
	 * @param monitorCore
	 *            The coordinates of the monitor core.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void resetReinjectionCounters(@Valid HasCoreLocation monitorCore)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Reset the packet reinjection counters of some monitor processes.
	 *
	 * @param monitorCores
	 *            The coordinates of the monitor cores.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void resetReinjectionCounters(@Valid CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set whether packets (of all types) are to be reinjected.
	 *
	 * @param monitorCore
	 *            The coordinates of the monitor core.
	 * @param reinject
	 *            True if all packets are to be reinjected.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void setReinjection(@Valid HasCoreLocation monitorCore,
			boolean reinject)
			throws IOException, ProcessException, InterruptedException {
		setReinjectionTypes(monitorCore, reinject, reinject, reinject,
				reinject);
	}

	/**
	 * Set whether packets (of all types) are to be reinjected.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores.
	 * @param reinject
	 *            True if all packets are to be reinjected.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void setReinjection(@Valid CoreSubsets monitorCores,
			boolean reinject)
			throws IOException, ProcessException, InterruptedException {
		setReinjectionTypes(monitorCores, reinject, reinject, reinject,
				reinject);
	}

	/**
	 * Restore whether packets are to be reinjected to a previously saved state.
	 *
	 * @param monitorCore
	 *            The coordinates of the monitor core.
	 * @param status
	 *            The saved reinjection status to restore.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void setReinjection(@Valid HasCoreLocation monitorCore,
			@NotNull ReinjectionStatus status)
			throws IOException, ProcessException, InterruptedException {
		setReinjectionTypes(monitorCore, status.isReinjectingMulticast(),
				status.isReinjectingPointToPoint(),
				status.isReinjectingFixedRoute(),
				status.isReinjectingNearestNeighbour());
	}

	/**
	 * Restore whether packets are to be reinjected to a previously saved state.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores.
	 * @param status
	 *            The saved reinjection status to restore.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void setReinjection(@Valid CoreSubsets monitorCores,
			@NotNull ReinjectionStatus status)
			throws IOException, ProcessException, InterruptedException {
		setReinjectionTypes(monitorCores, status.isReinjectingMulticast(),
				status.isReinjectingPointToPoint(),
				status.isReinjectingFixedRoute(),
				status.isReinjectingNearestNeighbour());
	}

	/**
	 * Set what types of packets are to be reinjected.
	 *
	 * @param monitorCore
	 *            The coordinates of the monitor core.
	 * @param multicast
	 *            True if multicast packets are to be reinjected.
	 * @param pointToPoint
	 *            True if point-to-point packets are to be reinjected.
	 * @param fixedRoute
	 *            True if fixed-route packets are to be reinjected.
	 * @param nearestNeighbour
	 *            True if nearest-neighbour packets are to be reinjected.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void setReinjectionTypes(@Valid HasCoreLocation monitorCore,
			boolean multicast, boolean pointToPoint, boolean fixedRoute,
			boolean nearestNeighbour)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set what types of packets are to be reinjected.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores.
	 * @param multicast
	 *            True if multicast packets are to be reinjected.
	 * @param pointToPoint
	 *            True if point-to-point packets are to be reinjected.
	 * @param fixedRoute
	 *            True if fixed-route packets are to be reinjected.
	 * @param nearestNeighbour
	 *            True if nearest-neighbour packets are to be reinjected.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void setReinjectionTypes(@Valid CoreSubsets monitorCores, boolean multicast,
			boolean pointToPoint, boolean fixedRoute, boolean nearestNeighbour)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set the emergency packet reinjection timeout.
	 *
	 * @param monitorCore
	 *            The coordinates of the monitor core.
	 * @param timeoutMantissa
	 *            The mantissa of the timeout value, between 0 and 15.
	 * @param timeoutExponent
	 *            The exponent of the timeout value, between 0 and 15.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void setReinjectionEmergencyTimeout(@Valid HasCoreLocation monitorCore,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set the emergency packet reinjection timeout.
	 *
	 * @param monitorCore
	 *            The coordinates of the monitor core.
	 * @param timeout
	 *            The timeout value.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void setReinjectionEmergencyTimeout(
			@Valid HasCoreLocation monitorCore, @NotNull RouterTimeout timeout)
			throws IOException, ProcessException, InterruptedException {
		setReinjectionEmergencyTimeout(monitorCore, timeout.mantissa,
				timeout.exponent);
	}

	/**
	 * Set the emergency packet reinjection timeout.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores.
	 * @param timeoutMantissa
	 *            The mantissa of the timeout value, between 0 and 15.
	 * @param timeoutExponent
	 *            The exponent of the timeout value, between 0 and 15.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void setReinjectionEmergencyTimeout(@Valid CoreSubsets monitorCores,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set the emergency packet reinjection timeout.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores.
	 * @param timeout
	 *            The timeout value.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void setReinjectionEmergencyTimeout(@Valid CoreSubsets monitorCores,
			@NotNull RouterTimeout timeout)
			throws IOException, ProcessException, InterruptedException {
		setReinjectionEmergencyTimeout(monitorCores, timeout.mantissa,
				timeout.exponent);
	}

	/**
	 * Set the emergency packet reinjection timeout.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores.
	 * @param status
	 *            The saved core status.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void setReinjectionEmergencyTimeout(@Valid CoreSubsets monitorCores,
			@NotNull ReinjectionStatus status)
			throws IOException, ProcessException, InterruptedException {
		setReinjectionEmergencyTimeout(monitorCores,
				status.getEmergencyTimeout());
	}

	/**
	 * Set the packet reinjection timeout.
	 *
	 * @param monitorCore
	 *            The coordinates of the monitor core.
	 * @param timeoutMantissa
	 *            The mantissa of the timeout value, between 0 and 15.
	 * @param timeoutExponent
	 *            The exponent of the timeout value, between 0 and 15.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	void setReinjectionTimeout(@Valid HasCoreLocation monitorCore,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set the packet reinjection timeout.
	 *
	 * @param monitorCore
	 *            The coordinates of the monitor core.
	 * @param timeout
	 *            The timeout value.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafe
	default void setReinjectionTimeout(@Valid HasCoreLocation monitorCore,
			@NotNull RouterTimeout timeout)
			throws IOException, ProcessException, InterruptedException {
		setReinjectionTimeout(monitorCore, timeout.mantissa, timeout.exponent);
	}

	/**
	 * Set the packet reinjection timeout.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores.
	 * @param timeoutMantissa
	 *            The mantissa of the timeout value, between 0 and 15.
	 * @param timeoutExponent
	 *            The exponent of the timeout value, between 0 and 15.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void setReinjectionTimeout(@Valid CoreSubsets monitorCores,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Set the packet reinjection timeout.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores.
	 * @param timeout
	 *            The timeout value.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void setReinjectionTimeout(@Valid CoreSubsets monitorCores,
			@NotNull RouterTimeout timeout)
			throws IOException, ProcessException, InterruptedException {
		setReinjectionTimeout(monitorCores, timeout.mantissa, timeout.exponent);
	}

	/**
	 * Set the packet reinjection timeout.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores.
	 * @param status
	 *            The saved core status.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	default void setReinjectionTimeout(@Valid CoreSubsets monitorCores,
			@NotNull ReinjectionStatus status)
			throws IOException, ProcessException, InterruptedException {
		setReinjectionTimeout(monitorCores, status.getTimeout());
	}

	/**
	 * Save the application's multicast router tables.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores; the routers on those
	 *            chips will have their (current) multicast router tables saved.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void saveApplicationRouterTables(@Valid CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Load the (previously saved) application's multicast router tables. The
	 * router tables <em>must</em> have been previously
	 * {@linkplain #saveApplicationRouterTables(CoreSubsets) saved}.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores; the routers on those
	 *            chips will have their multicast router tables loaded.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void loadApplicationRouterTables(@Valid CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Load the (previously configured) system multicast router tables. The
	 * application's router tables <em>must</em> have been previously
	 * {@linkplain #saveApplicationRouterTables(CoreSubsets) saved}.
	 *
	 * @param monitorCores
	 *            The coordinates of some monitor cores; the routers on those
	 *            chips will have their multicast router tables loaded.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	@ParallelSafeWithCare
	void loadSystemRouterTables(@Valid CoreSubsets monitorCores)
			throws IOException, ProcessException, InterruptedException;

	/**
	 * Create a connection to a particular instance of SCAMP. Note that this
	 * connection is a new connection; it is not a previously existing
	 * connection.
	 *
	 * @param chip
	 *            The location of the chip on the board with this remoteHost
	 * @param addr
	 *            The IP address of the SpiNNaker board to send messages to.
	 * @return The SCP connection to use. It is up to the caller to arrange for
	 *         this to be closed at the right time.
	 * @throws IOException
	 *             If anything goes wrong with socket setup.
	 */
	@ParallelSafe
	@MustBeClosed
	@UsedInJavadocOnly(Constants.class)
	SCPConnection createScpConnection(ChipLocation chip, InetAddress addr)
			throws IOException;
}
