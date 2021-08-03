/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.connections.SDPConnection;
import uk.ac.manchester.spinnaker.connections.model.Connection;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.machine.MulticastRoutingEntry;
import uk.ac.manchester.spinnaker.machine.RoutingEntry;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.ReverseIPTag;
import uk.ac.manchester.spinnaker.machine.tags.Tag;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.model.CPUInfo;
import uk.ac.manchester.spinnaker.messages.model.CPUState;
import uk.ac.manchester.spinnaker.messages.model.DiagnosticFilter;
import uk.ac.manchester.spinnaker.messages.model.HeapElement;
import uk.ac.manchester.spinnaker.messages.model.IOBuffer;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.ReinjectionStatus;
import uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics;
import uk.ac.manchester.spinnaker.messages.model.Signal;
import uk.ac.manchester.spinnaker.messages.model.SystemVariableDefinition;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;
import uk.ac.manchester.spinnaker.messages.sdp.SDPMessage;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.storage.BufferManagerStorage.Region;
import uk.ac.manchester.spinnaker.transceiver.FillDataType;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.TransceiverInterface;

/**
 * As much API as we need for the server to mock, and no more. Most transceiver
 * methods don't deal with BMPs, so we just mark them as unsupported and
 * deprecated.
 *
 * @author Donal Fellows
 */
// TODO split the transceiver interface
abstract class BMPOnlyTransceiverBase implements TransceiverInterface {
	@Override
	@Deprecated
	public final ConnectionSelector<SCPConnection>
			getScampConnectionSelector() {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void sendSCPMessage(SCPRequest<?> message,
			SCPConnection connection) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void sendSDPMessage(SDPMessage message,
			SDPConnection connection) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final MachineDimensions getMachineDimensions()
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final Machine getMachineDetails()
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final boolean isConnected(Connection connection) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final VersionInfo getScampVersion(HasChipLocation chip,
			ConnectionSelector<SCPConnection> connectionSelector)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void
			bootBoard(Map<SystemVariableDefinition, Object> extraBootValues)
					throws InterruptedException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final VersionInfo ensureBoardIsReady(int numRetries,
			Map<SystemVariableDefinition, Object> extraBootValues)
			throws IOException, ProcessException, InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final Iterable<CPUInfo> getCPUInformation(CoreSubsets coreSubsets)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final Iterable<IOBuffer> getIobuf(CoreSubsets coreSubsets)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void clearIobuf(CoreSubsets coreSubsets)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setWatchDogTimeoutOnChip(HasChipLocation chip,
			int watchdog) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void enableWatchDogTimerOnChip(HasChipLocation chip,
			boolean watchdog) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final int getCoreStateCount(AppID appID, CPUState state)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void execute(HasChipLocation chip,
			Collection<Integer> processors, InputStream executable,
			int numBytes, AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void execute(HasChipLocation chip,
			Collection<Integer> processors, File executable, AppID appID,
			boolean wait)
			throws IOException, ProcessException, InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void execute(HasChipLocation chip,
			Collection<Integer> processors, ByteBuffer executable, AppID appID,
			boolean wait)
			throws IOException, ProcessException, InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void executeFlood(CoreSubsets coreSubsets,
			InputStream executable, int numBytes, AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void executeFlood(CoreSubsets coreSubsets, File executable,
			AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void executeFlood(CoreSubsets coreSubsets,
			ByteBuffer executable, AppID appID, boolean wait)
			throws IOException, ProcessException, InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void updateRuntime(Integer runTimesteps,
			CoreSubsets coreSubsets) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void updateProvenanceAndExit(CoreSubsets coreSubsets)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void powerOnMachine()
			throws InterruptedException, IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void powerOffMachine()
			throws InterruptedException, IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public abstract void power(PowerCommand powerCommand, BMPCoords bmp,
			Collection<Integer> boards)
			throws InterruptedException, IOException, ProcessException;

	@Override
	public void setLED(Collection<Integer> leds, LEDAction action,
			BMPCoords bmp, Collection<Integer> board)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public abstract int readFPGARegister(int fpgaNumber, int register,
			BMPCoords bmp, int board) throws IOException, ProcessException;

	@Override
	public abstract void writeFPGARegister(int fpgaNumber, int register,
			int value, BMPCoords bmp, int board)
			throws IOException, ProcessException;

	@Override
	public ADCInfo readADCData(BMPCoords bmp, int board)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public abstract VersionInfo readBMPVersion(BMPCoords bmp, int board)
			throws IOException, ProcessException;

	@Override
	@Deprecated
	public final void writeMemory(HasCoreLocation core, int baseAddress,
			InputStream dataStream, int numBytes)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void writeMemory(HasCoreLocation core, int baseAddress,
			File dataFile) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void writeMemory(HasCoreLocation core, int baseAddress,
			ByteBuffer data) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void writeNeighbourMemory(HasCoreLocation core, Direction link,
			int baseAddress, InputStream dataStream, int numBytes)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void writeNeighbourMemory(HasCoreLocation core, Direction link,
			int baseAddress, File dataFile)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void writeNeighbourMemory(HasCoreLocation core, Direction link,
			int baseAddress, ByteBuffer data)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void writeMemoryFlood(int baseAddress, InputStream dataStream,
			int numBytes) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void writeMemoryFlood(int baseAddress, File dataFile)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void writeMemoryFlood(int baseAddress, ByteBuffer data)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final ByteBuffer readMemory(HasCoreLocation core, int baseAddress,
			int length) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void readRegion(Region region, BufferManagerStorage storage)
			throws IOException, ProcessException, StorageException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final ByteBuffer readNeighbourMemory(HasCoreLocation core,
			Direction link, int baseAddress, int length)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void stopApplication(AppID appID)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void waitForCoresToBeInState(CoreSubsets allCoreSubsets,
			AppID appID, Set<CPUState> cpuStates, Integer timeout,
			int timeBetweenPolls, Set<CPUState> errorStates,
			int countsBetweenFullCheck)
			throws IOException, InterruptedException, SpinnmanException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void sendSignal(AppID appID, Signal signal)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setLEDs(HasCoreLocation core,
			Map<Integer, LEDAction> ledStates)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final SCPConnection
			locateSpinnakerConnection(InetAddress boardAddress) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setIPTag(IPTag tag) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setIPTag(IPTag tag, SDPConnection connection)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setReverseIPTag(ReverseIPTag tag)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void clearIPTag(int tag, InetAddress boardAddress)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final List<Tag> getTags(SCPConnection connection)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final Map<Tag, Integer> getTagUsage(SCPConnection connection)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final int mallocSDRAM(HasChipLocation chip, int size, AppID appID,
			int tag) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void freeSDRAM(HasChipLocation chip, int baseAddress)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final int freeSDRAM(HasChipLocation chip, AppID appID)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void loadMulticastRoutes(HasChipLocation chip,
			Collection<MulticastRoutingEntry> routes, AppID appID)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void loadFixedRoute(HasChipLocation chip,
			RoutingEntry fixedRoute, AppID appID)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final RoutingEntry readFixedRoute(HasChipLocation chip, AppID appID)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final List<MulticastRoutingEntry>
			getMulticastRoutes(HasChipLocation chip, AppID appID)
					throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void clearMulticastRoutes(HasChipLocation chip)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final RouterDiagnostics getRouterDiagnostics(HasChipLocation chip)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void setRouterDiagnosticFilter(HasChipLocation chip, int position,
			DiagnosticFilter diagnosticFilter)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final DiagnosticFilter
			getRouterDiagnosticFilter(HasChipLocation chip, int position)
					throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void clearRouterDiagnosticCounters(HasChipLocation chip,
			boolean enable, Iterable<Integer> counterIDs)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final List<HeapElement> getHeap(HasChipLocation chip,
			SystemVariableDefinition heap)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void fillMemory(HasChipLocation chip, int baseAddress,
			int repeatValue, int size, FillDataType dataType)
			throws ProcessException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void clearReinjectionQueues(HasCoreLocation monitorCore)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void clearReinjectionQueues(CoreSubsets monitorCores)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final ReinjectionStatus getReinjectionStatus(
			HasCoreLocation monitorCore) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final Map<CoreLocation, ReinjectionStatus> getReinjectionStatus(
			CoreSubsets monitorCores) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void resetReinjectionCounters(HasCoreLocation monitorCore)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void resetReinjectionCounters(CoreSubsets monitorCores)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setReinjectionTypes(HasCoreLocation monitorCore,
			boolean multicast, boolean pointToPoint, boolean fixedRoute,
			boolean nearestNeighbour) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setReinjectionTypes(CoreSubsets monitorCores,
			boolean multicast, boolean pointToPoint, boolean fixedRoute,
			boolean nearestNeighbour) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setReinjectionEmergencyTimeout(
			HasCoreLocation monitorCore, int timeoutMantissa,
			int timeoutExponent) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setReinjectionEmergencyTimeout(CoreSubsets monitorCores,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setReinjectionTimeout(HasCoreLocation monitorCore,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void setReinjectionTimeout(CoreSubsets monitorCores,
			int timeoutMantissa, int timeoutExponent)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void saveApplicationRouterTables(CoreSubsets monitorCores)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void loadApplicationRouterTables(CoreSubsets monitorCores)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public final void loadSystemRouterTables(CoreSubsets monitorCores)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}
}
