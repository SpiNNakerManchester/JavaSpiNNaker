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
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.FPGA.FPGA_ALL;
import static uk.ac.manchester.spinnaker.messages.model.FPGA.FPGA_E_S;
import static uk.ac.manchester.spinnaker.messages.model.FPGA.FPGA_N_NE;
import static uk.ac.manchester.spinnaker.messages.model.FPGA.FPGA_SW_W;
import static uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters.LEDO;
import static uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters.SCRM;
import static uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters.SLEN;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
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

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.model.Prototype;
import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.FPGALinkRegisters;
import uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters;
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
	private static final Logger log = getLogger(FirmwareLoader.class);

	private static final int FLASH_DATA_LENGTH = 4096;

	private static final int CRC_OFFSET = FLASH_DATA_LENGTH - WORD_SIZE;

	private static final int CRC_BUFFER_LENGTH = 8192;

	private static final long CRC_MASK = 0xffffffffL;

	private static final int FLASH_DATA_ADDRESS = 0x1000;

	private static final int BITFILE_BASE = 0x200000;

	private static final int BITFILE_MAX_SIZE = 0x180000;

	private static final int DATA_SECTOR_CHUNK_SIZE = 128;

	/**
	 * Location of FPGA register control instructions within the flash data
	 * sector.
	 */
	private static final int REGISTER_DATA_SECTOR_LOCATION =
			6 * DATA_SECTOR_CHUNK_SIZE;

	/**
	 * Location of bitfile control instructions within the flash data sector.
	 */
	private static final int BITFILE_DATA_SECTOR_LOCATION =
			DATA_SECTOR_CHUNK_SIZE;

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
			Resource r = manifestLocation.createRelative(f);
			try (InputStream dummy = r.getInputStream()) {
				// We do this to check that the bit file is readable at all
				bitFiles.put(f, r);
			} catch (IOException e) {
				FileNotFoundException fnf = new FileNotFoundException(
						"failed to open bitfile resource: " + r);
				fnf.initCause(e);
				throw fnf;
			}
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

		private static final int DATA_SECTOR_HEADER_WORDS = 8;

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

	/**
	 * Instructions to set a register on one or more FPGAs. This is done not
	 * just immediately, but also during BMP boot.
	 *
	 * @author Donal Fellows
	 */
	public static class RegisterSet {
		private final FPGA fpga;

		private final int address;

		private final int value;

		public RegisterSet(FPGA fpga, FPGAMainRegisters register, int value) {
			this.fpga = fpga;
			this.address = register.getAddress();
			this.value = value;
		}

		public RegisterSet(FPGA fpga, FPGALinkRegisters register, int bank,
				int value) {
			this.fpga = fpga;
			this.address = register.address(bank);
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
		return txrx.readBMPMemory(bmp, board, FLASH_DATA_ADDRESS,
				FLASH_DATA_LENGTH);
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

	private static final int NUM_DATA_SECTORS = 16;

	private static final int FPGA_ID_MASK = 0b00000011;

	/** The read part of {@code cmd_xreg} from {@code bmpc}. */
	private void listFPGARegisterSets() throws ProcessException, IOException {
		ByteBuffer data = readFlashData();
		for (int i = 0; i < NUM_DATA_SECTORS; i++) {
			int chunkBase = DATA_SECTOR_CHUNK_SIZE * i;
			data.position(chunkBase);
			int type = data.get();
			if (type != FlashDataSector.REGISTER_BYTE) {
				continue;
			}
			int size = data.get();
			// Position after the header
			data.position(chunkBase
					+ FlashDataSector.DATA_SECTOR_HEADER_WORDS * WORD_SIZE);
			for (int j = 0; j < size; j++) {
				int addr = data.getInt();
				int value = data.getInt();
				log.info("FPGA REGISTERS: {}",
						format("%3s %08x %08x",
								FPGA.values()[addr & FPGA_ID_MASK],
								addr & ~FPGA_ID_MASK, value));
			}
		}
	}

	private void sver() {
		// TODO do we keep this?
	}

	private void xboot() {
		// TODO do we keep this?
	}

	/**
	 * The write part of {@code cmd_xreg} from {@code bmpc}.
	 *
	 * @param settings
	 *            The registers to set.
	 */
	private void setupRegisters(RegisterSet... settings)
			throws ProcessException, IOException {
		List<Integer> data = new ArrayList<>();
		for (RegisterSet r : settings) {
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

	private static final int CLEAR = 0;

	private static final int SET = 0xffffffff;

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

		setupRegisters(new RegisterSet(FPGA_ALL, SCRM, CLEAR),
				new RegisterSet(FPGA_ALL, SLEN, SET),
				new RegisterSet(FPGA_ALL, LEDO, CLEAR));
		sleep(SMALL_SLEEP);

		String nameDef = bitfileNames.get(idx);
		setupBitfile(nameDef, 0, FPGA_ALL);
		idx++;

		sleep(SMALL_SLEEP);

		setupBitfile(bitfileNames.get(idx), idx, FPGA_E_S);
		idx++;
		setupBitfile(bitfileNames.get(idx), idx, FPGA_SW_W);
		idx++;
		setupBitfile(bitfileNames.get(idx), idx, FPGA_N_NE);

		// TODO these would read the configuration...
		xboot();
		listFPGARegisterSets();
		sver();

		// TODO is this a necessary delay?
		sleep(BIG_SLEEP);
	}
}
