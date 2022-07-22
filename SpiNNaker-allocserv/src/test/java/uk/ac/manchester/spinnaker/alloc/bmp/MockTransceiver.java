/*
 * Copyright (c) 2021-2022 The University of Manchester
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

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.UnimplementedBMPTransceiver;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * BMP transceiver mock just for test purposes.
 */
public final class MockTransceiver extends UnimplementedBMPTransceiver {
	private static final Logger log = getLogger(MockTransceiver.class);

	/**
	 * Install this mock transceiver as the thing to be manufactured by the
	 * given transceiver factory.
	 *
	 * @param txrxFactory
	 *            The transceiver factory to install into.
	 */
	@SuppressWarnings("deprecation")
	public static void installIntoFactory(TransceiverFactory txrxFactory) {
		txrxFactory.getTestAPI().setFactory(MockTransceiver::new);
	}

	/** Not a real serial number at all! Just for testing purposes. */
	private static final String SERIAL_NUMBER = "gorp";

	private static final int VERSION_INFO_SIZE = 32;

	/** Dummy version code. */
	static final short VERSION = 0x202;

	/** Initial dummy blacklist data. */
	static final String BLACKLIST = "chip 5 5 core 5";

	private static String blacklistData = BLACKLIST;

	private static VersionInfo version =
			new VersionInfo(syntheticVersionData(VERSION), true);

	private Map<Integer, Boolean> status;

	private final ValueHolder<Blacklist> setBlacklist;

	private MockTransceiver(String machineName, BMPConnectionData data,
			ValueHolder<Blacklist> setBlacklist) {
		log.info("constructed dummy transceiver for {} ({} : {})", machineName,
				data.ipAddress, data.boards);
		status = new HashMap<>();
		this.setBlacklist = setBlacklist;
	}

	public static void setVersion(short versionCode) {
		version = new VersionInfo(syntheticVersionData(versionCode), true);
	}

	public static void setBlacklist(String blacklist) {
		blacklistData = blacklist;
	}

	/**
	 * @return The bytes of a response, correct in the places which Spalloc
	 *         checks, and arbitrary (zero) elsewhere.
	 */
	private static ByteBuffer syntheticVersionData(short versionCode) {
		var b = allocate(VERSION_INFO_SIZE).order(LITTLE_ENDIAN);
		b.putInt(0);
		b.putInt(0);
		b.putInt(0);
		b.putInt(0);
		b.putShort((short) 0);
		b.putShort(versionCode);
		b.putInt(0);
		b.put("abc/def".getBytes(UTF_8));
		return b.flip();
	}

	public Map<Integer, Boolean> getStatus() {
		return unmodifiableMap(status);
	}

	@Override
	public void power(PowerCommand powerCommand, BMPCoords bmp,
			Collection<BMPBoard> boards) {
		log.info("power({},{},{})", powerCommand, bmp, boards);
		for (var b : boards) {
			status.put(b.board, powerCommand == POWER_ON);
		}
	}

	@Override
	public int readFPGARegister(FPGA fpga, MemoryLocation register,
			BMPCoords bmp, BMPBoard board) {
		log.info("readFPGARegister({},{},{},{})", fpga, register, bmp, board);
		return fpga.value;
	}

	@Override
	public void writeFPGARegister(FPGA fpga, MemoryLocation register, int value,
			BMPCoords bmp, BMPBoard board) {
		log.info("writeFPGARegister({},{},{},{},{})", fpga, register, value,
				bmp, board);
	}

	@Override
	public VersionInfo readBMPVersion(BMPCoords bmp, BMPBoard board) {
		return version;
	}

	@Override
	public String readBoardSerialNumber(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		log.info("readBoardSerialNumber({},{})", bmp, board);
		return SERIAL_NUMBER;
	}

	public ByteBuffer readSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length) {
		log.info("readSerialFlash({},{},{},{})", bmp, board, baseAddress,
				length);
		// Pad to length
		var b = ByteBuffer.allocate(length).order(LITTLE_ENDIAN);
		if (baseAddress.address == SERIAL_FLASH_BLACKLIST_OFFSET) {
			b.put(new Blacklist(blacklistData).getRawData());
		}
		return b.limit(length).position(length).flip();
	}

	@Override
	public ByteBuffer readBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException {
		log.info("readBMPMemory({},{},{},{})", bmp, board, baseAddress,
				length);
		var b = ByteBuffer.allocate(length).order(LITTLE_ENDIAN);
		return b.limit(length).position(length).flip();
	}

	private ByteBuffer chunkedData;

	private ByteBuffer written;

	private static final int SPACE = 0x10000;

	private static final MemoryLocation BUF_PLACE =
			new MemoryLocation(0x12345678);

	@Override
	public MemoryLocation eraseBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int size) {
		log.info("eraseBMPFlash({},{},{},{})", bmp, board, baseAddress, size);
		chunkedData = ByteBuffer.allocate(SPACE).order(LITTLE_ENDIAN);
		return BUF_PLACE;
	}

	public void writeBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data) {
		log.info("writeBMPMemory({},{},{}:{})", bmp, board, baseAddress,
				data.remaining());
		written = data.duplicate();
	}

	public void chunkBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation address) {
		log.info("chunkBMPFlash({},{},{})", bmp, board, address);
		chunkedData.put(written);
	}

	private static final int BMP_FLASH_BLACKLIST_OFFSET = 0xe00;

	public void copyBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int size) {
		log.info("copyBMPFlash({},{},{},{})", bmp, board, baseAddress, size);
		var inFlash = chunkedData.duplicate();
		inFlash.flip().position(BMP_FLASH_BLACKLIST_OFFSET);
		synchronized (setBlacklist) {
			setBlacklist.setValue(new Blacklist(inFlash.slice()));
		}
	}

	private static final int SERIAL_FLASH_BLACKLIST_OFFSET = 0x100;

	public void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data) {
		log.info("writeSerialFlash({},{},{}:{})", bmp, board, baseAddress,
				data.remaining());
		var b = data.duplicate().order(LITTLE_ENDIAN);
		b.position(SERIAL_FLASH_BLACKLIST_OFFSET);
		var bl = new Blacklist(b);
		synchronized (setBlacklist) {
			if (!bl.equals(setBlacklist.getValue())) {
				throw new IllegalStateException("blacklist in serial flash ("
						+ bl + ") is different to blacklist in BMP flash ("
						+ setBlacklist.getValue() + ")");
			}
		}
	}
}
