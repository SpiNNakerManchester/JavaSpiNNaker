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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

	private static final List<Integer> PROCS = asList(1);

	private Set<Integer> set = new HashSet<>();

	/**
	 * Serialise a request and get its length.
	 *
	 * @param req
	 *            the request
	 * @return the length of the request
	 */
	private int length(SCPRequest<?> req) {
		// Serialise and get message length
		var b = allocate(280);
		req.sdpHeader.setSource(ZERO_ZERO_ZERO);
		req.sdpHeader.setSourcePort(0);
		req.sdpHeader.setDestination(ZERO_ZERO_ZERO);
		req.sdpHeader.setDestinationPort(0);
		req.scpRequestHeader.issueSequenceNumber(set);
		req.addToBuffer(b);
		return b.position();
	}

	private static final int NO_PAYLOAD = 24;

	@Nested
	class Scp {
		@Test
		void applicationRun() {
			assertEquals(NO_PAYLOAD,
					length(new ApplicationRun(APP, ZERO_ZERO, PROCS)));
			assertEquals(NO_PAYLOAD,
					length(new ApplicationRun(APP, ZERO_ZERO, PROCS, true)));
		}

		@Test
		void applicationStop() {
			assertEquals(NO_PAYLOAD, length(new ApplicationStop(APP)));
		}

		@Test
		void clearIobuf() {
			assertEquals(NO_PAYLOAD, length(new ClearIOBUF(ZERO_ZERO_ZERO)));
		}

		@Test
		void countState() {
			assertEquals(NO_PAYLOAD, length(new CountState(APP, IDLE)));
		}

		@Test
		void getChipInfo() {
			assertEquals(NO_PAYLOAD, length(new GetChipInfo(ZERO_ZERO)));
			assertEquals(NO_PAYLOAD, length(new GetChipInfo(ZERO_ZERO, false)));
		}

		@Test
		void getVersion() {
			assertEquals(NO_PAYLOAD, length(new GetVersion(ZERO_ZERO_ZERO)));
		}

		@Test
		void sendSignal() {
			assertEquals(NO_PAYLOAD, length(new SendSignal(APP, CONTINUE)));
		}

		@Test
		void setLed() {
			assertEquals(NO_PAYLOAD,
					length(new SetLED(ZERO_ZERO_ZERO, Map.of())));
		}

		@Test
		void updateProvenanceAndExit() {
			assertEquals(NO_PAYLOAD,
					length(new UpdateProvenanceAndExit(ZERO_ZERO_ZERO)));
		}

		@Test
		void updateRuntime() {
			assertEquals(NO_PAYLOAD,
					length(new UpdateRuntime(ZERO_ZERO_ZERO, 1, false)));
		}

		@Nested
		class IPTags {
			@Test
			void ipTagClear() {
				assertEquals(NO_PAYLOAD,
						length(new IPTagClear(ZERO_ZERO_ZERO, 0)));
			}

			@Test
			void ipTagGet() {
				assertEquals(NO_PAYLOAD,
						length(new IPTagGet(ZERO_ZERO_ZERO, 0)));
			}

			@Test
			void ipTagGetInfo() {
				assertEquals(NO_PAYLOAD,
						length(new IPTagGetInfo(ZERO_ZERO_ZERO)));
			}

			@Test
			void ipTagSet() {
				assertEquals(NO_PAYLOAD, length(
						new IPTagSet(ZERO_ZERO_ZERO, null, 0, 0, true, true)));
			}

			@Test
			void ipTagSetTto() {
				assertEquals(NO_PAYLOAD,
						length(new IPTagSetTTO(ZERO_ZERO_ZERO, TIMEOUT_10_ms)));
			}

			@Test
			void reverseIpTagSet() {
				assertEquals(NO_PAYLOAD, length(new ReverseIPTagSet(ZERO_ZERO,
						ZERO_ZERO_ZERO, 0, 0, 0)));
			}
		}

		@Nested
		class Routers {
			@Test
			void clearReinjectionQueue() {
				assertEquals(NO_PAYLOAD,
						length(new ClearReinjectionQueue(ZERO_ZERO_ZERO)));
			}

			@Test
			void fixedRouteInitialise() {
				assertEquals(NO_PAYLOAD,
						length(new FixedRouteInitialise(ZERO_ZERO, 0, APP)));
			}

			@Test
			void fixedRouteRead() {
				assertEquals(NO_PAYLOAD,
						length(new FixedRouteRead(ZERO_ZERO, APP)));
			}

			@Test
			void getReinjectionStatus() {
				assertEquals(NO_PAYLOAD,
						length(new GetReinjectionStatus(ZERO_ZERO_ZERO)));
			}

			@Test
			void resetReinjectionCounters() {
				assertEquals(NO_PAYLOAD,
						length(new ResetReinjectionCounters(ZERO_ZERO_ZERO)));
			}

			@Test
			void routerAlloc() {
				assertEquals(NO_PAYLOAD,
						length(new RouterAlloc(ZERO_ZERO, APP, 4)));
			}

			@Test
			void routerClear() {
				assertEquals(NO_PAYLOAD, length(new RouterClear(ZERO_ZERO)));
			}

			@Test
			void routerInit() {
				assertEquals(NO_PAYLOAD,
						length(new RouterInit(ZERO_ZERO, 4, NULL, 4, APP)));
			}

			@Test
			void routerTableLoadApplicationRoutes() {
				assertEquals(NO_PAYLOAD, length(
						new RouterTableLoadApplicationRoutes(ZERO_ZERO_ZERO)));
			}

			@Test
			void routerTableLoadSystemRoutes() {
				assertEquals(NO_PAYLOAD, length(
						new RouterTableLoadSystemRoutes(ZERO_ZERO_ZERO)));
			}

			@Test
			void routerTableSaveApplicationRoutes() {
				assertEquals(NO_PAYLOAD, length(
						new RouterTableSaveApplicationRoutes(ZERO_ZERO_ZERO)));
			}

			@Test
			void setReinjectionPacketTypes() {
				assertEquals(25,
						length(new SetReinjectionPacketTypes(ZERO_ZERO_ZERO,
								true, true, true, true)));
			}

			@Test
			void setRouterEmergencyTimeout() {
				assertEquals(NO_PAYLOAD, length(
						new SetRouterEmergencyTimeout(ZERO_ZERO_ZERO, 1, 1)));
			}

			@Test
			void setRouterTimeout() {
				assertEquals(NO_PAYLOAD,
						length(new SetRouterTimeout(ZERO_ZERO_ZERO, 1, 1)));
			}
		}

		@Nested
		class Memory {
			@Test
			void fillRequest() {
				assertEquals(NO_PAYLOAD,
						length(new FillRequest(ZERO_ZERO, NULL, 0, 0)));
			}

			@Test
			void floodFillData() {
				var b = allocate(128);
				assertEquals(152,
						length(new FloodFillData((byte) 0, 0, NULL, b)));
				var bs = new byte[64];
				assertEquals(88,
						length(new FloodFillData((byte) 0, 0, NULL, bs)));
				assertEquals(56, length(
						new FloodFillData((byte) 0, 0, NULL, bs, 16, 32)));
			}

			@Test
			void floodFillEnd() {
				assertEquals(NO_PAYLOAD, length(new FloodFillEnd((byte) 0)));
				assertEquals(NO_PAYLOAD,
						length(new FloodFillEnd((byte) 0, APP, PROCS)));
				assertEquals(NO_PAYLOAD,
						length(new FloodFillEnd((byte) 0, APP, PROCS, false)));
			}

			@Test
			void floodFillStart() {
				assertEquals(NO_PAYLOAD,
						length(new FloodFillStart((byte) 0, 0)));
				assertEquals(NO_PAYLOAD,
						length(new FloodFillStart((byte) 0, 0, ZERO_ZERO)));
			}

			@Test
			void readLink() {
				assertEquals(NO_PAYLOAD,
						length(new ReadLink(ZERO_ZERO, EAST, NULL, 4)));
				assertEquals(NO_PAYLOAD,
						length(new ReadLink(ZERO_ZERO_ZERO, EAST, NULL, 4)));
			}

			@Test
			void readMemory() {
				assertEquals(NO_PAYLOAD,
						length(new ReadMemory(ZERO_ZERO, NULL, 4)));
				assertEquals(NO_PAYLOAD,
						length(new ReadMemory(ZERO_ZERO_ZERO, NULL, 4)));
			}

			@Test
			void sdramAlloc() {
				assertEquals(NO_PAYLOAD,
						length(new SDRAMAlloc(ZERO_ZERO, APP, 4)));
				assertEquals(NO_PAYLOAD,
						length(new SDRAMAlloc(ZERO_ZERO, APP, 4, 4)));
			}

			@Test
			void sdramDealloc() {
				assertEquals(NO_PAYLOAD,
						length(new SDRAMDeAlloc(ZERO_ZERO, APP)));
				assertEquals(NO_PAYLOAD,
						length(new SDRAMDeAlloc(ZERO_ZERO, NULL)));
			}

			@Test
			void writeLink() {
				var b = allocate(128);
				assertEquals(152,
						length(new WriteLink(ZERO_ZERO_ZERO, EAST, NULL, b)));
			}

			@Test
			void writeMemory() {
				var b = allocate(128);
				assertEquals(152, length(new WriteMemory(ZERO_ZERO, NULL, b)));
				assertEquals(152,
						length(new WriteMemory(ZERO_ZERO_ZERO, NULL, b)));
			}
		}
	}

	@Nested
	class Bmp {
		@Test
		void readMemory() {
			assertEquals(NO_PAYLOAD, length(new BMPReadMemory(BOARD, NULL, 4)));
		}

		@Test
		void setLed() {
			assertEquals(NO_PAYLOAD,
					length(new BMPSetLED(asList(0), TOGGLE, asList(BOARD))));
		}

		@Test
		void writeMemory() {
			var b = allocate(128);
			assertEquals(152, length(new BMPWriteMemory(BOARD, NULL, b)));
		}

		@Test
		void setPower() {
			assertEquals(NO_PAYLOAD,
					length(new SetPower(POWER_OFF, asList(BOARD), 0.0)));
		}

		@Nested
		class Info {
			@Test
			void getVersion() {
				assertEquals(NO_PAYLOAD, length(new GetBMPVersion(BOARD)));
			}

			@Test
			void readAdc() {
				assertEquals(NO_PAYLOAD, length(new ReadADC(BOARD)));
			}

			@Test
			void readCanStatus() {
				assertEquals(NO_PAYLOAD, length(new ReadCANStatus()));
			}

			@Test
			void readIpAddress() {
				assertEquals(NO_PAYLOAD, length(new ReadIPAddress(BOARD)));
			}

			@Test
			void readSerialVector() {
				assertEquals(NO_PAYLOAD, length(new ReadSerialVector(BOARD)));
			}
		}

		@Nested
		class Fpgas {
			@Test
			void getFpgaResetStatus() {
				assertEquals(NO_PAYLOAD, length(new GetFPGAResetStatus(BOARD)));
			}

			@Test
			void initFpga() {
				assertEquals(NO_PAYLOAD, length(new InitFPGA(BOARD, 0)));
			}

			@Test
			void readFpgaRegister() {
				assertEquals(NO_PAYLOAD,
						length(new ReadFPGARegister(FPGA_E_S, NULL, BOARD)));
			}

			@Test
			void resetFpga() {
				assertEquals(NO_PAYLOAD, length(new ResetFPGA(BOARD, PULSE)));
			}

			@Test
			void writeFpgaData() {
				var b = allocate(128);
				assertEquals(152, length(new WriteFPGAData(BOARD, b)));
				var bs = new byte[64];
				assertEquals(88, length(new WriteFPGAData(BOARD, bs)));
			}

			@Test
			void writeFpgaRegister() {
				assertEquals(28, length(
						new WriteFPGARegister(FPGA_E_S, NULL, 4, BOARD)));
			}
		}

		@Nested
		class Flash {
			@Test
			void eraseFlash() {
				assertEquals(NO_PAYLOAD,
						length(new EraseFlash(BOARD, NULL, 4)));
			}

			@Test
			void readSerialFlash() {
				assertEquals(NO_PAYLOAD,
						length(new ReadSerialFlash(BOARD, NULL, 4)));
			}

			@Test
			void readSerialFlashCrc() {
				assertEquals(NO_PAYLOAD,
						length(new ReadSerialFlashCRC(BOARD, NULL, 4)));
			}

			@Test
			void updateFlash() {
				assertEquals(NO_PAYLOAD,
						length(new UpdateFlash(BOARD, NULL, 4)));
			}

			@Test
			void writeFlashBuffer() {
				assertEquals(NO_PAYLOAD,
						length(new WriteFlashBuffer(BOARD, NULL, false)));
			}

			@Test
			void writeSerialFlash() {
				var b = allocate(128);
				assertEquals(152, length(new WriteSerialFlash(BOARD, NULL, b)));
			}
		}
	}
}
