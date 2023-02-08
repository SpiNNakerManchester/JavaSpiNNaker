/*
 * Copyright (c) 2021-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.io.IOUtils.readFully;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;
// TODO use ByteBuffer.slice(int,int) from Java 14 onwards
import static uk.ac.manchester.spinnaker.utils.ByteBufferUtils.slice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

import org.slf4j.Logger;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
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

	private static MockTransceiver current;

	public static MockTransceiver getCurrentMock() {
		return current;
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

	@GuardedBy("itself")
	private final ValueHolder<Blacklist> setBlacklist;

	private MockTransceiver(String machineName, BMPConnectionData data,
			ValueHolder<Blacklist> setBlacklist) {
		log.info("constructed dummy transceiver for {} ({} : {})", machineName,
				data.ipAddress, data.boards);
		status = new HashMap<>();
		this.setBlacklist = setBlacklist;
		current = this;
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
		byte zero = 0;
		var b = allocate(VERSION_INFO_SIZE).order(LITTLE_ENDIAN);
		b.put(zero);
		b.put(zero);
		b.put(zero);
		b.put(zero);
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

	/**
	 * A place where you can set a queue of wonky results from the read of the
	 * FPGA registers.
	 */
	@SuppressWarnings("checkstyle:visibilitymodifier")
	public static ArrayDeque<FPGA> fpgaResults = new ArrayDeque<>();

	@Override
	public int readFPGARegister(FPGA fpga, MemoryLocation register,
			BMPCoords bmp, BMPBoard board) {
		log.info("readFPGARegister({},{},{},{})", fpga, register, bmp, board);
		var r = fpgaResults.pollFirst();
		if (r != null) {
			return r.value;
		}
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
		log.info("readBMPVersion({},{})", bmp, board);
		return version;
	}

	@Override
	public String readBoardSerialNumber(BMPCoords bmp, BMPBoard board) {
		log.info("readBoardSerialNumber({},{})", bmp, board);
		return SERIAL_NUMBER;
	}

	private static final int MEM_SIZE = 8 * 1024 * 1024;

	private static ByteBuffer allocateMemory() {
		var buf = allocate(MEM_SIZE).order(LITTLE_ENDIAN);
		buf.position(0).limit(MEM_SIZE);
		return buf;
	}

	private ByteBuffer memory = allocateMemory();

	private ByteBuffer flash = allocateMemory();

	@Override
	public ByteBuffer readSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length) {
		log.info("readSerialFlash({},{},{},{})", bmp, board, baseAddress,
				length);
		// Pad to length
		var b = slice(flash, baseAddress.address, length);
		if (baseAddress.address == SERIAL_FLASH_BLACKLIST_OFFSET) {
			b.put(new Blacklist(blacklistData).getRawData());
			b.position(0);
		}
		return b;
	}

	@Override
	public ByteBuffer readBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length) {
		log.info("readBMPMemory({},{},{},{})", bmp, board, baseAddress, length);
		return slice(memory, baseAddress.address, length);
	}

	@Override
	public MemoryLocation getSerialFlashBuffer(BMPCoords bmp, BMPBoard board) {
		log.info("getSerialFlashBuffer({},{})", bmp, board);
		return MemoryLocation.NULL;
	}

	@Override
	public void writeBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data) {
		log.info("writeBMPMemory({},{},{}:{})", bmp, board, baseAddress,
				data.remaining());
		slice(memory, baseAddress.address, data.remaining())
				.put(data.duplicate());
	}

	@Override
	public void writeFlash(@Valid BMPCoords bmp, @Valid BMPBoard board,
			@NotNull MemoryLocation baseAddress, @NotNull ByteBuffer data,
			boolean update) {
		log.info("writeFlash({},{},{},{})", bmp, board, baseAddress,
				data.remaining());
		var blData = data.duplicate().position(BMP_FLASH_BLACKLIST_OFFSET);
		synchronized (setBlacklist) {
			setBlacklist.setValue(new Blacklist(blData));
		}
	}

	private static final int BMP_FLASH_BLACKLIST_OFFSET = 0xe00;

	private static final int SERIAL_FLASH_BLACKLIST_OFFSET = 0x100;

	@Override
	public void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data) {
		log.info("writeSerialFlash({},{},{}:{})", bmp, board, baseAddress,
				data.remaining());
		var b = slice(flash, baseAddress.address, data.remaining()).put(data);
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

	@Override
	public void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int size, InputStream stream)
			throws IOException {
		log.info("writeSerialFlash({},{},{},{})", bmp, board, baseAddress,
				size);
		slice(flash, baseAddress.address, size).put(readFully(stream, size));
	}

	@Override
	public void writeBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress) {
		log.info("writeBMPFlash({},{},{})", bmp, board, baseAddress);
	}

	private static final long CRC_MASK = 0xffffffffL;

	@Override
	public int readSerialFlashCRC(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length) {
		log.info("readSerialFlashCRC({},{},{},{})", bmp, board, baseAddress,
				length);
		var crc = new CRC32();
		crc.update(slice(flash, baseAddress.address, length));
		return (int) (crc.getValue() & CRC_MASK);
	}
}
