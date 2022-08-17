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
package uk.ac.manchester.spinnaker.transceiver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * A transceiver where every operation fails with
 * {@link UnsupportedOperationException}. A suitable base for partial
 * implementations.
 *
 * @author Donal Fellows
 */
public abstract class UnimplementedBMPTransceiver
		implements BMPTransceiverInterface {
	private BMPCoords boundBMP = new BMPCoords(0, 0);

	protected UnimplementedBMPTransceiver() {
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
	@Deprecated
	public MemoryLocation eraseBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int size)
			throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void chunkBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation address) throws IOException, ProcessException {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void copyBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int size)
			throws IOException, ProcessException {
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
}
