/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

import static java.nio.ByteBuffer.allocate;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.MemoryLocation.NULL;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.IDLE;
import static uk.ac.manchester.spinnaker.messages.model.FPGA.FPGA_E_S;
import static uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime.TIMEOUT_10_ms;
import static uk.ac.manchester.spinnaker.messages.model.LEDAction.TOGGLE;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.Signal.CONTINUE;
import static uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface.FPGAResetType.PULSE;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.BMPReadMemory;
import uk.ac.manchester.spinnaker.messages.bmp.BMPSetLED;
import uk.ac.manchester.spinnaker.messages.bmp.BMPWriteMemory;
import uk.ac.manchester.spinnaker.messages.bmp.EraseFlash;
import uk.ac.manchester.spinnaker.messages.bmp.GetBMPVersion;
import uk.ac.manchester.spinnaker.messages.bmp.GetFPGAResetStatus;
import uk.ac.manchester.spinnaker.messages.bmp.InitFPGA;
import uk.ac.manchester.spinnaker.messages.bmp.ReadADC;
import uk.ac.manchester.spinnaker.messages.bmp.ReadCANStatus;
import uk.ac.manchester.spinnaker.messages.bmp.ReadFPGARegister;
import uk.ac.manchester.spinnaker.messages.bmp.ReadIPAddress;
import uk.ac.manchester.spinnaker.messages.bmp.ReadSerialFlash;
import uk.ac.manchester.spinnaker.messages.bmp.ReadSerialFlashCRC;
import uk.ac.manchester.spinnaker.messages.bmp.ReadSerialVector;
import uk.ac.manchester.spinnaker.messages.bmp.ResetFPGA;
import uk.ac.manchester.spinnaker.messages.bmp.SetPower;
import uk.ac.manchester.spinnaker.messages.bmp.UpdateFlash;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFPGAData;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFPGARegister;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer;
import uk.ac.manchester.spinnaker.messages.bmp.WriteSerialFlash;
import uk.ac.manchester.spinnaker.messages.model.AppID;

class GeneralMessageTest {
	private static final AppID APP = new AppID(1);

	private static final HasCoreLocation ZERO_ZERO_ZERO =
			ZERO_ZERO.getScampCore();

	private static final BMPBoard BOARD = new BMPBoard(0);

	private Set<Integer> set = new HashSet<>();

	private int length(SCPRequest<?> r) {
		// Serialise and get message length
		ByteBuffer b = allocate(256);
		r.sdpHeader.setSource(ZERO_ZERO_ZERO);
		r.sdpHeader.setSourcePort(0);
		r.sdpHeader.setDestination(ZERO_ZERO_ZERO);
		r.sdpHeader.setDestinationPort(0);
		r.scpRequestHeader.issueSequenceNumber(set);
		r.addToBuffer(b);
		return b.position();
	}

	@Nested
	class Scp {
		@Test
		void applicationRun() {
			assertEquals(24,
					length(new ApplicationRun(APP, ZERO_ZERO, asList(1))));
		}

		@Test
		void applicationStop() {
			assertEquals(24, length(new ApplicationStop(APP)));
		}

		@Test
		void clearIobuf() {
			assertEquals(24, length(new ClearIOBUF(ZERO_ZERO_ZERO)));
		}

		@Test
		void clearReinjectionQueue() {
			assertEquals(24, length(new ClearReinjectionQueue(ZERO_ZERO_ZERO)));
		}

		@Test
		void countState() {
			assertEquals(24, length(new CountState(APP, IDLE)));
		}

		@Test
		void fillRequest() {
			assertEquals(24, length(new FillRequest(ZERO_ZERO, NULL, 0, 0)));
		}

		@Test
		void fixedRouteInitialise() {
			assertEquals(24,
					length(new FixedRouteInitialise(ZERO_ZERO, 0, APP)));
		}

		@Test
		void fixedRouteRead() {
			assertEquals(24, length(new FixedRouteRead(ZERO_ZERO, APP)));
		}

		@Test
		void floodFillData() {
			ByteBuffer b = allocate(128);
			assertEquals(152, length(new FloodFillData((byte) 0, 0, NULL, b)));
		}

		@Test
		void floodFillEnd() {
			assertEquals(24, length(new FloodFillEnd((byte) 0)));
		}

		@Test
		void floodFillStart() {
			assertEquals(24, length(new FloodFillStart((byte) 0, 0)));
		}

		@Test
		void getChipInfo() {
			assertEquals(24, length(new GetChipInfo(ZERO_ZERO)));
		}

		@Test
		void getReinjectionStatus() {
			assertEquals(24, length(new GetReinjectionStatus(ZERO_ZERO_ZERO)));
		}

		@Test
		void getVersion() {
			assertEquals(24, length(new GetVersion(ZERO_ZERO_ZERO)));
		}

		@Test
		void ipTagClear() {
			assertEquals(24, length(new IPTagClear(ZERO_ZERO_ZERO, 0)));
		}

		@Test
		void ipTagGet() {
			assertEquals(24, length(new IPTagGet(ZERO_ZERO_ZERO, 0)));
		}

		@Test
		void ipTagGetInfo() {
			assertEquals(24, length(new IPTagGetInfo(ZERO_ZERO_ZERO)));
		}

		@Test
		void ipTagSet() {
			assertEquals(24, length(
					new IPTagSet(ZERO_ZERO_ZERO, null, 0, 0, true, true)));
		}

		@Test
		void ipTagSetTto() {
			assertEquals(24,
					length(new IPTagSetTTO(ZERO_ZERO_ZERO, TIMEOUT_10_ms)));
		}

		@Test
		void readLink() {
			assertEquals(24, length(new ReadLink(ZERO_ZERO, EAST, NULL, 4)));
		}

		@Test
		void readMemory() {
			assertEquals(24, length(new ReadMemory(ZERO_ZERO, NULL, 4)));
		}

		@Test
		void resetReinjectionCounters() {
			assertEquals(24,
					length(new ResetReinjectionCounters(ZERO_ZERO_ZERO)));
		}

		@Test
		void reverseIpTagSet() {
			assertEquals(24, length(
					new ReverseIPTagSet(ZERO_ZERO, ZERO_ZERO_ZERO, 0, 0, 0)));
		}

		@Test
		void routerAlloc() {
			assertEquals(24, length(new RouterAlloc(ZERO_ZERO, APP, 4)));
		}

		@Test
		void routerClear() {
			assertEquals(24, length(new RouterClear(ZERO_ZERO)));
		}

		@Test
		void routerInit() {
			assertEquals(24,
					length(new RouterInit(ZERO_ZERO, 4, NULL, 4, APP)));
		}

		@Test
		void routerTableLoadApplicationRoutes() {
			assertEquals(24, length(
					new RouterTableLoadApplicationRoutes(ZERO_ZERO_ZERO)));
		}

		@Test
		void routerTableLoadSystemRoutes() {
			assertEquals(24,
					length(new RouterTableLoadSystemRoutes(ZERO_ZERO_ZERO)));
		}

		@Test
		void routerTableSaveApplicationRoutes() {
			assertEquals(24, length(
					new RouterTableSaveApplicationRoutes(ZERO_ZERO_ZERO)));
		}

		@Test
		void sdramAlloc() {
			assertEquals(24, length(new SDRAMAlloc(ZERO_ZERO, APP, 4)));
		}

		@Test
		void sdramDealloc() {
			assertEquals(24, length(new SDRAMDeAlloc(ZERO_ZERO, APP)));
			assertEquals(24, length(new SDRAMDeAlloc(ZERO_ZERO, NULL)));
		}

		@Test
		void sendSignal() {
			assertEquals(24, length(new SendSignal(APP, CONTINUE)));
		}

		@Test
		void setLed() {
			assertEquals(24,
					length(new SetLED(ZERO_ZERO_ZERO, new HashMap<>())));
		}

		@Test
		void setReinjectionPacketTypes() {
			assertEquals(25,
					length(new SetReinjectionPacketTypes(ZERO_ZERO_ZERO, true,
							true, true, true)));
		}

		@Test
		void setRouterEmergencyTimeout() {
			assertEquals(24, length(
					new SetRouterEmergencyTimeout(ZERO_ZERO_ZERO, 1, 1)));
		}

		@Test
		void setRouterTimeout() {
			assertEquals(24,
					length(new SetRouterTimeout(ZERO_ZERO_ZERO, 1, 1)));
		}

		@Test
		void updateProvenanceAndExit() {
			assertEquals(24,
					length(new UpdateProvenanceAndExit(ZERO_ZERO_ZERO)));
		}

		@Test
		void udpateRuntime() {
			assertEquals(24,
					length(new UpdateRuntime(ZERO_ZERO_ZERO, 1, false)));
		}

		@Test
		void writeLink() {
			ByteBuffer b = allocate(128);
			assertEquals(152,
					length(new WriteLink(ZERO_ZERO_ZERO, EAST, NULL, b)));
		}

		@Test
		void writeMemory() {
			ByteBuffer b = allocate(128);
			assertEquals(152, length(new WriteMemory(ZERO_ZERO_ZERO, NULL, b)));
		}
	}

	@Nested
	class Bmp {
		@Test
		void readMemory() {
			assertEquals(24, length(new BMPReadMemory(BOARD, NULL, 4)));
		}

		@Test
		void setLed() {
			assertEquals(24,
					length(new BMPSetLED(asList(), TOGGLE, asList(BOARD))));
		}

		@Test
		void writeMemory() {
			ByteBuffer b = allocate(128);
			assertEquals(152, length(new BMPWriteMemory(BOARD, NULL, b)));
		}

		@Test
		void eraseFlash() {
			assertEquals(24, length(new EraseFlash(BOARD, NULL, 4)));
		}

		@Test
		void getVersion() {
			assertEquals(24, length(new GetBMPVersion(BOARD)));
		}

		@Test
		void getFpgaResetStatus() {
			assertEquals(24, length(new GetFPGAResetStatus(BOARD)));
		}

		@Test
		void initFpga() {
			assertEquals(24, length(new InitFPGA(BOARD, 0)));
		}

		@Test
		void readAdc() {
			assertEquals(24, length(new ReadADC(BOARD)));
		}

		@Test
		void readCanStatus() {
			assertEquals(24, length(new ReadCANStatus()));
		}

		@Test
		void readFpgaRegister() {
			assertEquals(24,
					length(new ReadFPGARegister(FPGA_E_S, NULL, BOARD)));
		}

		@Test
		void readIpAddress() {
			assertEquals(24, length(new ReadIPAddress(BOARD)));
		}

		@Test
		void readSerialFlash() {
			assertEquals(24, length(new ReadSerialFlash(BOARD, NULL, 4)));
		}

		@Test
		void readSerialFlashCrc() {
			assertEquals(24, length(new ReadSerialFlashCRC(BOARD, NULL, 4)));
		}

		@Test
		void readSerialVector() {
			assertEquals(24, length(new ReadSerialVector(BOARD)));
		}

		@Test
		void resetFpga() {
			assertEquals(24, length(new ResetFPGA(BOARD, PULSE)));
		}

		@Test
		void setPower() {
			assertEquals(24,
					length(new SetPower(POWER_OFF, asList(BOARD), 0.0)));
		}

		@Test
		void updateFlash() {
			assertEquals(24, length(new UpdateFlash(BOARD, NULL, 4)));
		}

		@Test
		void writeFlashBuffer() {
			assertEquals(24, length(new WriteFlashBuffer(BOARD, NULL, false)));
		}

		@Test
		void writeFpgaData() {
			ByteBuffer b = allocate(128);
			assertEquals(152, length(new WriteFPGAData(BOARD, b)));
		}

		@Test
		void writeFpgaRegister() {
			assertEquals(28,
					length(new WriteFPGARegister(FPGA_E_S, NULL, 4, BOARD)));
		}

		@Test
		void writeSerialFlash() {
			ByteBuffer b = allocate(128);
			assertEquals(152, length(new WriteSerialFlash(BOARD, NULL, b)));
		}
	}
}
