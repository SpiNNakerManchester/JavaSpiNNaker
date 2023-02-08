/*
 * Copyright (c) 2021 The University of Manchester
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * A transceiver where every operation (except the few mandatory ones) fails
 * with {@link UnsupportedOperationException}. A suitable base for partial
 * implementations.
 *
 * @author Donal Fellows
 */
public abstract class UnimplementedBMPTransceiver
		implements BMPTransceiverInterface {
	private BMPCoords boundBMP = new BMPCoords(0, 0);

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
	public void power(PowerCommand powerCommand, BMPCoords bmp,
			Collection<BMPBoard> boards)
			throws InterruptedException, IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLED(Collection<Integer> leds, LEDAction action,
			BMPCoords bmp, Collection<BMPBoard> board)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int readFPGARegister(FPGA fpga, MemoryLocation register,
			BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeFPGARegister(FPGA fpga, MemoryLocation register, int value,
			BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ADCInfo readADCData(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public VersionInfo readBMPVersion(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getResetStatus(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resetFPGA(BMPCoords bmp, BMPBoard board,
			FPGAResetType resetType) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ByteBuffer readBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, File file)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ByteBuffer readSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int readSerialFlashCRC(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, File file)
			throws ProcessException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data)
			throws ProcessException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeFlash(@Valid BMPCoords bmp, @Valid BMPBoard board,
			@NotNull MemoryLocation baseAddress, @NotNull ByteBuffer data,
			boolean update)
			throws ProcessException, IOException, InterruptedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public MemoryLocation getSerialFlashBuffer(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int size, InputStream stream)
			throws ProcessException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public MappableIterable<BMPBoard> availableBoards(BMPCoords bmp)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String readBoardSerialNumber(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
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
	public void close() throws IOException {
	}
}
