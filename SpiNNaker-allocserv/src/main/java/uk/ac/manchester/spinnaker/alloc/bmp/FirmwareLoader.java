/*
 * Copyright (c) 2022 The University of Manchester
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

import static java.lang.Integer.parseUnsignedInt;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.CRC32;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.model.Prototype;
import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;

/**
 * Handles loading of firmware into a BMP or an FPGA.
 *
 * @author Donal Fellows
 */
@Component
@Prototype
public class FirmwareLoader {
	private static final int FDS_LENGTH = 4096;

	private static final int CRC_OFFSET = FDS_LENGTH - WORD_SIZE;

	private static final int CRC_BUFFER_LENGTH = 8192;

	private static final long CRC_MASK = 0xffffffffL;

	private static final int FLASH_DATA_ADDRESS = 0x1000;

	private static final int BITFILE_BASE = 0x200000;

	private static final int BITFILE_MAX_SIZE = 0x180000;

	private static final int REGISTER_DATA_SECTOR_LOCATION = 6 * 128;

	private static final int BITFILE_DATA_SECTOR_LOCATION = 128;

	private static final double SMALL_SLEEP = 0.25;

	private static final double BIG_SLEEP = 12.0;

	private final BMPCoords bmp;

	private final BMPBoard board;

	private final BMPTransceiverInterface txrx;

	@Value("classpath:bitfiles/manifest.properties")
	private Resource manifestLocation;

	/**
	 * @param txrx
	 *            How to talk to BMPs.
	 * @param bmp
	 *            Which BMP to talk to. (This might just be for message-routing
	 *            purposes.)
	 * @param board
	 *            Which board's BMP are we really working with.
	 */
	public FirmwareLoader(BMPTransceiverInterface txrx, BMPCoords bmp,
			BMPBoard board) {
		this.txrx = txrx;
		this.bmp = bmp;
		this.board = board;
	}

	private List<String> bitfileNames;

	private Map<String, Resource> bitFiles = new HashMap<>();

	private Map<String, Integer> modTimes = new HashMap<>();

	@PostConstruct
	void loadManifest() throws IOException {
		Properties props = new Properties();
		try (InputStream is = manifestLocation.getInputStream()) {
			props.load(is);
		}
		bitfileNames = stream(props.getProperty("bitfiles").split(","))
				.map(String::trim).collect(toList());
		for (String f : bitfileNames) {
			modTimes.put(f, parseUnsignedInt(props.getProperty(f)));
			bitFiles.put(f, manifestLocation.createRelative(f));
		}
	}

	public static class UpdateFailedException extends RuntimeException {
		private static final long serialVersionUID = -2711856077373862547L;

		/** The data read back from the BMP. */
		public final ByteBuffer data;

		UpdateFailedException(ByteBuffer data) {
			super("failed to update flash data correctly!");
			this.data = data.asReadOnlyBuffer();
			this.data.order(ByteOrder.LITTLE_ENDIAN);
		}
	}

	public static class CRCFailedException extends RuntimeException {
		private static final long serialVersionUID = 6083516080055422661L;

		/** The CRC calculated by the BMP. */
		public final int crc;

		CRCFailedException(int crc) {
			super(format("CRC by BMP failed to match: 0x%08x", crc));
			this.crc = crc;
		}
	}

	public static class TooLargeException extends RuntimeException {
		private static final long serialVersionUID = 2300521304133953589L;

		TooLargeException(long size) {
			super(format("Bit file is too large for BMP buffer: 0x%08x", size));
		}
	}

	private static class FlashDataSector {
		private static final int DATA_SECTOR_LENGTH = 128;

		private static final byte BITFILE_BYTE = 3;

		private static final byte REGISTER_BYTE = 4;

		private static final int PADDING_WORDS = 7;

		private static final int BITFILE_NAME_MAX_LENGTH = 96;

		private static final int BITFILE_MAGIC_FLAG = 0x8000;

		private static final int PAD = 0xffffffff;

		final ByteBuffer buf;

		FlashDataSector() {
			buf = ByteBuffer.allocate(DATA_SECTOR_LENGTH);
			buf.order(ByteOrder.LITTLE_ENDIAN);
		}

		static FlashDataSector registers(int numItems, List<Integer> data) {
			FlashDataSector fds = new FlashDataSector();
			fds.registersHeader(numItems);
			fds.registersPayload(data);
			fds.buf.flip();
			return fds;
		}

		static FlashDataSector bitfile(String name, int mtime, int crc,
				FPGA chip, int timestamp, int baseAddress, int length) {
			FlashDataSector fds = new FlashDataSector();
			fds.bitfileHeader(name, mtime, crc, chip, timestamp, baseAddress,
					length);
			fds.buf.flip();
			return fds;
		}

		private void pad(int targetLength, int value) {
			while (buf.position() < targetLength) {
				buf.putInt(value);
			}
		}

		private void registersHeader(int numItems) {
			buf.put(REGISTER_BYTE);

			buf.put((byte) numItems);

			buf.putShort((short) 0);
			for (int i = 0; i < PADDING_WORDS; i++) {
				buf.putInt(0);
			}
		}

		private void registersPayload(List<Integer> data) {
			for (int item : data) {
				buf.putInt(item);
			}
			pad(buf.capacity(), PAD);
		}

		private void bitfileHeader(String name, int mtime, int crc, FPGA chip,
				int timestamp, int baseAddress, int length) {
			byte[] namedata = name.getBytes(StandardCharsets.UTF_8);
			int namesize = min(namedata.length, BITFILE_NAME_MAX_LENGTH);

			buf.put(BITFILE_BYTE);

			buf.put((byte) namesize);
			buf.putShort((short) (BITFILE_MAGIC_FLAG + chip.bits));
			buf.putInt(timestamp);
			buf.putInt(crc);
			buf.putInt(baseAddress);
			buf.putInt(length);
			buf.putInt(mtime);

			buf.putInt(PAD);
			buf.putInt(PAD);

			buf.put(namedata, 0, namesize);
			while (buf.position() < buf.capacity()) {
				buf.put((byte) 0);
			}
		}
	}

	private enum FPGA {
		first("0", 0, 1), second("1", 1, 2), third("2", 2, 4), all("0-2", 3, 7);

		final String name;

		final int value;

		final int bits;

		FPGA(String name, int value, int bits) {
			this.name = name;
			this.value = value;
			this.bits = bits;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static class RegSet {
		final FPGA fpga;

		final int address;

		final int value;

		RegSet(FPGA fpga, int address, int value) {
			this.fpga = fpga;
			this.address = address;
			this.value = value;
		}
	}

	private static void putBuffer(ByteBuffer target, ByteBuffer source,
			int offset) {
		ByteBuffer slice = target.slice();
		slice.position(offset);
		slice.put(source.array(), 0, source.remaining());
	}

	private static ByteBuffer slice(ByteBuffer src, int from, int len) {
		ByteBuffer s = src.slice();
		s.order(LITTLE_ENDIAN);
		s.position(from);
		s.limit(from + len);
		return s;
	}

	private ByteBuffer readFlashData() throws ProcessException, IOException {
		return txrx.readBMPMemory(bmp, board, FLASH_DATA_ADDRESS, FDS_LENGTH);
	}

	private static int crc(ByteBuffer buffer, int from, int len) {
		CRC32 crc = new CRC32();
		crc.update(slice(buffer, from, len));
		return (int) (crc.getValue() & CRC_MASK);
	}

	private static int crc(Resource r) throws IOException {
		try (InputStream s = new BufferedInputStream(r.getInputStream())) {
			return crc(s);
		}
	}

	private static int crc(InputStream s) throws IOException {
		CRC32 crc = new CRC32();
		byte[] buffer = new byte[CRC_BUFFER_LENGTH];
		while (true) {
			int len = s.read(buffer);
			if (len < 1) {
				break;
			}
			crc.update(buffer, 0, len);
		}
		return (int) (crc.getValue() & CRC_MASK);
	}

	private void updateFlashData(ByteBuffer data)
			throws ProcessException, IOException {
		data.putInt(CRC_OFFSET, (int) ~crc(data, 0, CRC_OFFSET));
		data.position(0);
		int fb = txrx.getSerialFlashBuffer(bmp, board);
		txrx.writeBMPMemory(bmp, board, fb, data);
		txrx.writeBMPFlash(bmp, board, FLASH_DATA_ADDRESS);
		ByteBuffer newData = readFlashData();
		if (!data.equals(newData)) {
			throw new UpdateFailedException(newData);
		}
	}

	private void xreg() {
		// TODO do we keep this?
	}

	private void sver() {
		// TODO do we keep this?
	}

	private void xboot() {
		// TODO do we keep this?
	}

	private void setupRegisters(RegSet... settings)
			throws ProcessException, IOException {
		List<Integer> data = new ArrayList<>();
		for (RegSet r : settings) {
			data.add(r.address | r.fpga.value);
			data.add(r.value);
		}
		FlashDataSector sector =
				FlashDataSector.registers(settings.length, data);

		ByteBuffer flashData = readFlashData();
		putBuffer(flashData, sector.buf, REGISTER_DATA_SECTOR_LOCATION);
		updateFlashData(flashData);
	}

	private void setupBitfile(String handle, int slot, FPGA chip)
			throws IOException, ProcessException {
		Resource resource = bitFiles.get(handle);
		int mtime = modTimes.get(handle);
		String name = resource.getFilename();
		int size = (int) resource.contentLength();
		int crc = crc(resource);

		if (size > BITFILE_MAX_SIZE) {
			throw new TooLargeException(size);
		}

		int base = BITFILE_BASE + slot * BITFILE_MAX_SIZE;
		// TODO progress bar? Not in server mode
		try (InputStream s =
				new BufferedInputStream(resource.getInputStream())) {
			txrx.writeSerialFlash(bmp, board, base, size, s);
		}
		int otherCRC = txrx.readSerialFlashCRC(bmp, board, base, size);
		if (otherCRC != crc) {
			throw new CRCFailedException(otherCRC);
		}

		int timestamp = (int) (currentTimeMillis() / MSEC_PER_SEC);

		FlashDataSector sector = FlashDataSector.bitfile(name, mtime, crc, chip,
				timestamp, base, size);

		ByteBuffer flashData = readFlashData();
		putBuffer(flashData, sector.buf, BITFILE_DATA_SECTOR_LOCATION);
		updateFlashData(flashData);
	}

	private void sleep(double secs) throws InterruptedException {
		Thread.sleep((long) (secs * MSEC_PER_SEC));
	}

	/**
	 * Load the FPGA definitions.
	 *
	 * @throws InterruptedException
	 *             If interrrupted while sleeping
	 * @throws ProcessException
	 *             If a BMP rejects a message
	 * @throws IOException
	 *             If the network fails or the packaged bitfiles are unreadable
	 */
	@SuppressWarnings("checkstyle:magicnumber")
	public void bitLoad()
			throws InterruptedException, ProcessException, IOException {
		// Bleah
		int idx = 0;
		String nameDef = bitfileNames.get(idx++);
		String fpga0 = bitfileNames.get(idx++);
		String fpga1 = bitfileNames.get(idx++);
		String fpga2 = bitfileNames.get(idx++);
		setupRegisters(new RegSet(FPGA.all, 0x40010, 0),
				new RegSet(FPGA.all, 0x40014, 0xffffffff),
				new RegSet(FPGA.all, 0x40018, 0));
		sleep(SMALL_SLEEP);
		setupBitfile(nameDef, 0, FPGA.all);
		sleep(SMALL_SLEEP);
		setupBitfile(fpga0, 1, FPGA.first);
		setupBitfile(fpga1, 2, FPGA.second);
		setupBitfile(fpga2, 3, FPGA.third);
		xboot();
		xreg();
		sver();
		sleep(BIG_SLEEP);
	}
}
