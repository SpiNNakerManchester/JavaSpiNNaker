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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.alloc.bmp.DataSectorTypes.BITFILE;
import static uk.ac.manchester.spinnaker.alloc.bmp.DataSectorTypes.REGISTER;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.CRC32;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.model.Prototype;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.FPGALinkRegisters;
import uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
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

	private static final MemoryLocation FLASH_DATA_ADDRESS =
			new MemoryLocation(0x1000);

	private static final MemoryLocation BITFILE_BASE =
			new MemoryLocation(0x200000);

	private static final int BITFILE_MAX_SIZE = 0x180000;

	private static final int DATA_SECTOR_CHUNK_SIZE = 128;

	private static final int NUM_DATA_SECTORS = 16;

	private static final int FPGA_ID_MASK = 0b00000011;

	private static final int CHIP_MASK = 0b111;

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

	private static final int DATA_SECTOR_LENGTH = 128;

	private static final int DATA_SECTOR_HEADER_WORDS = 8;

	private static final int DATA_SECTOR_HEADER_BYTES =
			DATA_SECTOR_HEADER_WORDS * WORD_SIZE;

	private static final int BITFILE_NAME_MAX_LENGTH = 96;

	private static final int BITFILE_ENABLED_FLAG = 0x8000;

	private static final int PAD = 0xffffffff;

	/** Used to clear all bits in a register. */
	private static final int CLEAR = 0;

	/** Used to set all bits in a register. */
	private static final int SET = 0xffffffff;

	private static final String[] CHIP_LABELS = {
		"", "0", "1", "10", "2", "20", "21", "210"
	};

	private static final String[] SLOT_LABELS = {
		"", "", "S0", "S1", "S2", "S3", "", "", "", "", "", "", "", "", "", ""
	};

	private static final double SMALL_SLEEP = 0.25;

	private static final double BIG_SLEEP = 12.0;

	private final BMPBoard board;

	private final BMPTransceiverInterface txrx;

	@Autowired
	private FirmwareDefinition firmware;

	/**
	 * @param txrx
	 *            How to talk to BMPs. The specific BMP to talk to must have
	 *            been bound.
	 * @param board
	 *            Which board's BMP are we really working with.
	 */
	public FirmwareLoader(BMPTransceiverInterface txrx, BMPBoard board) {
		this.txrx = txrx;
		this.board = board;
	}

	/** Base class of exceptions thrown by the firmware loader. */
	public abstract static class FirmwareLoaderException
			extends RuntimeException {
		private static final long serialVersionUID = -7057612243855126410L;

		FirmwareLoaderException(String msg) {
			super(msg);
		}
	}

	/** An update of the firmware on a BMP failed. */
	public static class UpdateFailedException extends FirmwareLoaderException {
		private static final long serialVersionUID = 7925582707336953554L;

		/** The data read back from the BMP. */
		public final ByteBuffer data;

		UpdateFailedException(ByteBuffer data) {
			super("failed to update flash data correctly!");
			this.data = data.asReadOnlyBuffer();
			this.data.order(LITTLE_ENDIAN);
		}
	}

	/** A CRC check failed. */
	public static class CRCFailedException extends FirmwareLoaderException {
		private static final long serialVersionUID = -4111893327837084643L;

		/** The CRC calculated by the BMP. */
		public final int crc;

		CRCFailedException(int crc) {
			super(format("CRC by BMP failed to match: 0x%08x", crc));
			this.crc = crc;
		}
	}

	/** A data chunk was too large for the firmware loader to handle. */
	public static class TooLargeException extends FirmwareLoaderException {
		private static final long serialVersionUID = -9025065456329109710L;

		TooLargeException(long size) {
			super(format("Bit file is too large for BMP buffer: 0x%08x", size));
		}
	}

	private static final int SUB_WORD_MASK = 3;

	private static boolean notAligned(int offset) {
		return (offset & SUB_WORD_MASK) != 0;
	}

	private static class FlashDataSector {
		final ByteBuffer buf;

		FlashDataSector() {
			buf = ByteBuffer.allocate(DATA_SECTOR_LENGTH);
			buf.order(LITTLE_ENDIAN);
		}

		static FlashDataSector registers(int numItems, List<Integer> data) {
			FlashDataSector fds = new FlashDataSector();
			fds.registersHeader(numItems);
			fds.registersPayload(data);
			fds.buf.flip();
			return fds;
		}

		static FlashDataSector bitfile(String name, int mtime, int crc,
				FPGA chip, int timestamp, MemoryLocation baseAddress,
				int length) {
			FlashDataSector fds = new FlashDataSector();
			fds.bitfileHeader(mtime, crc, chip, timestamp, baseAddress, length);
			fds.bitfileName(name);
			fds.buf.flip();
			return fds;
		}

		private void pad(int targetLength, int value) {
			while (buf.position() < targetLength
					&& notAligned(buf.position())) {
				buf.put((byte) value);
			}
			while (buf.position() < targetLength) {
				buf.putInt(value);
			}
		}

		private void registersHeader(int numItems) {
			buf.put(REGISTER.value);
			buf.put((byte) numItems);
			pad(DATA_SECTOR_HEADER_BYTES, 0);
		}

		private void registersPayload(List<Integer> data) {
			for (int item : data) {
				buf.putInt(item);
			}
			pad(buf.capacity(), PAD);
		}

		private void bitfileHeader(int mtime, int crc, FPGA chip, int timestamp,
				MemoryLocation baseAddress, int length) {
			buf.put(BITFILE.value);

			buf.put((byte) 0);
			buf.putShort((short) (BITFILE_ENABLED_FLAG + chip.bits));
			buf.putInt(timestamp);
			buf.putInt(crc);
			buf.putInt(baseAddress.address);
			buf.putInt(length);
			buf.putInt(mtime);

			buf.putInt(PAD);
			buf.putInt(PAD);
		}

		private void bitfileName(String name) {
			byte[] namedata = name.getBytes(StandardCharsets.UTF_8);
			int namesize = min(namedata.length, BITFILE_NAME_MAX_LENGTH);
			buf.put(1, (byte) namesize);
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

		private final MemoryLocation address;

		private final int value;

		/**
		 * @param fpga
		 *            Which FPGA's registers to set
		 * @param register
		 *            Which register is this
		 * @param value
		 *            The value to set
		 */
		public RegisterSet(FPGA fpga, FPGAMainRegisters register, int value) {
			this.fpga = fpga;
			this.address = register.getAddress();
			this.value = value;
		}

		/**
		 * @param fpga
		 *            Which FPGA's registers to set
		 * @param register
		 *            Which register is this
		 * @param bank
		 *            In which register bank (i.e., for which link)
		 * @param value
		 *            The value to set
		 */
		public RegisterSet(FPGA fpga, FPGALinkRegisters register, int bank,
				int value) {
			this.fpga = fpga;
			this.address = register.address(bank);
			this.value = value;
		}
	}

	private static void putBuffer(ByteBuffer target, ByteBuffer source,
			int offset) {
		// Dupe so we can freely manipulate the position
		ByteBuffer slice = target.duplicate();
		slice.position(offset);
		slice.put(source);
	}

	/**
	 * Make a slice of a byte buffer without modifying the original buffer.
	 *
	 * @param src
	 *            The originating buffer.
	 * @param from
	 *            The offset into the originating buffer where the slice starts.
	 * @param len
	 *            The length of the slice.
	 * @return The little-endian slice. This will be read-only if and only if
	 *         the original buffer is read-only.
	 */
	private static ByteBuffer slice(ByteBuffer src, int from, int len) {
		ByteBuffer s = src.duplicate();
		s.position(from);
		s.limit(from + len);
		return s.slice().order(LITTLE_ENDIAN);
	}

	private ByteBuffer readFlashData() throws ProcessException, IOException {
		return txrx.readBMPMemory(board, FLASH_DATA_ADDRESS, FLASH_DATA_LENGTH);
	}

	private ByteBuffer readFlashDataHead()
			throws ProcessException, IOException {
		return txrx.readBMPMemory(board, FLASH_DATA_ADDRESS,
				FLASH_DATA_LENGTH / 2);
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
		data.putInt(CRC_OFFSET, ~crc(data, 0, CRC_OFFSET));
		data.position(0);
		MemoryLocation fb = txrx.getSerialFlashBuffer(board);
		txrx.writeBMPMemory(board, fb, data);
		txrx.writeBMPFlash(board, FLASH_DATA_ADDRESS);
		ByteBuffer newData = readFlashData();
		if (!data.equals(newData)) {
			throw new UpdateFailedException(newData);
		}
	}

	private void logBMPVersion() throws ProcessException, IOException {
		VersionInfo info = txrx.readBMPVersion(board);
		// TODO validate which field is which; some of these seem... unlikely
		log.info("BMP INFO:       {}",
				format("%s %s at %s:%s (built %s) [C=%s, F=%s, B=%s]",
						info.name, info.versionNumber, info.hardware,
						info.core.getP(), Instant.ofEpochSecond(info.buildDate),
						info.physicalCPUID, info.core.getY(),
						info.core.getX()));
	}

	/**
	 * The read part of {@code cmd_xreg} and {@code cmd_xboot} from
	 * {@code bmpc}. This merges the two because there's little point in keeping
	 * them separate for our use case and that avoids doing some network
	 * traffic.
	 */
	private void listFPGABootChunks() throws ProcessException, IOException {
		ByteBuffer data = readFlashDataHead();
		for (int i = 0; i < NUM_DATA_SECTORS; i++) {
			ByteBuffer chunk = slice(data, DATA_SECTOR_CHUNK_SIZE * i,
					DATA_SECTOR_CHUNK_SIZE);
			byte type = chunk.get();
			switch (DataSectorTypes.get(type)) {
			case REGISTER:
				logRegisterSets(chunk, i);
				break;
			case BITFILE:
				logFPGABootBitfile(chunk, i);
				break;
			default:
				// Ignore the chunk
				break;
			}
		}
	}

	/**
	 * Describe the register set control commands in the current chunk of boot
	 * header.
	 *
	 * @param chunk
	 *            The chunk, positioned immediately after the type byte.
	 * @param i
	 *            The index of the chunk
	 */
	private void logRegisterSets(ByteBuffer chunk, int i) {
		int size = chunk.get();
		// Position after the header
		chunk.position(DATA_SECTOR_HEADER_BYTES);
		for (int j = 0; j < size; j++) {
			int addr = chunk.getInt();
			int value = chunk.getInt();
			log.info("FPGA REGISTERS: {}",
					format("%3s %08x %08x", FPGA.values()[addr & FPGA_ID_MASK],
							addr & ~FPGA_ID_MASK, value));
		}
	}

	/**
	 * Describe the installed bitfile used to boot an FPGA, using the
	 * information in the current chunk of boot header.
	 *
	 * @param data
	 *            The chunk, positioned immediately after the type byte.
	 * @param i
	 *            The index of the chunk
	 */
	private void logFPGABootBitfile(ByteBuffer data, int i) {
		int size = data.get();
		int flags = data.getShort();
		int time = data.getInt();
		int crc = data.getInt();
		int base = data.getInt();
		int length = data.getInt();
		int mtime = data.getInt();
		byte[] filenameBytes = new byte[size];
		data.position(DATA_SECTOR_HEADER_BYTES);
		data.get(filenameBytes, 0, size);

		String state =
				(flags & BITFILE_ENABLED_FLAG) > 0 ? "ENABLED " : "DISABLED";
		log.info("FPGA BOOT:      {}", format(
				"%3s  %s  Chips %-3s, Base 0x%06x, Length %8d, CRC 0x%08x",
				SLOT_LABELS[i], state, CHIP_LABELS[flags & CHIP_MASK], base,
				length, crc));
		log.info("FPGA BOOT:           File      {}",
				new String(filenameBytes, 0, size, UTF_8).trim());
		log.info("FPGA BOOT:           Written   {}",
				Instant.ofEpochSecond(time));
		log.info("FPGA BOOT:           ModTime   {}",
				Instant.ofEpochSecond(mtime));
	}

	/**
	 * The write part of {@code cmd_xreg} from {@code bmpc}.
	 *
	 * @param settings
	 *            The registers to set.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws UpdateFailedException
	 *             If the flash data sector read back does not match what we
	 *             wanted to write.
	 */
	public void setupRegisters(RegisterSet... settings)
			throws ProcessException, IOException {
		List<Integer> data = new ArrayList<>();
		for (RegisterSet r : settings) {
			data.add(r.address.address | r.fpga.value);
			data.add(r.value);
		}
		FlashDataSector sector =
				FlashDataSector.registers(settings.length, data);

		ByteBuffer flashData = readFlashData();
		putBuffer(flashData, sector.buf, REGISTER_DATA_SECTOR_LOCATION);
		updateFlashData(flashData);
	}

	/**
	 * Set a bitfile to be loaded.
	 *
	 * @param handle
	 *            The bitfile handle, matching one of the resources known to
	 *            this bean.
	 * @param slot
	 *            Which slot to install the bitfile in.
	 * @param chip
	 *            Which chip or chips are to load the bitfile.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws TooLargeException
	 *             If the bitfile is too large for the BMP's buffer.
	 * @throws CRCFailedException
	 *             If the written bitfile fails its CRC check.
	 * @throws UpdateFailedException
	 *             If the flash data sector read back does not match what we
	 *             wanted to write.
	 */
	public void setupBitfile(String handle, int slot, FPGA chip)
			throws IOException, ProcessException {
		Resource resource = firmware.resource(handle);
		int mtime = firmware.mtime(handle);
		String name = resource.getFilename();
		int size = (int) resource.contentLength();
		int crc = crc(resource);

		if (size > BITFILE_MAX_SIZE) {
			throw new TooLargeException(size);
		}

		MemoryLocation base = BITFILE_BASE.add(slot * BITFILE_MAX_SIZE);
		try (InputStream s =
				new BufferedInputStream(resource.getInputStream())) {
			txrx.writeSerialFlash(board, base, size, s);
		}
		int otherCRC = txrx.readSerialFlashCRC(board, base, size);
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
	 * @param postDelay
	 *            Whether to do the long delay after the FPGA boot/check
	 * @throws InterruptedException
	 *             If interrupted while sleeping
	 * @throws ProcessException
	 *             If a BMP rejects a message
	 * @throws IOException
	 *             If the network fails or the packaged bitfiles are unreadable
	 * @throws FirmwareLoaderException
	 *             If something goes wrong.
	 */
	public void bitLoad(boolean postDelay)
			throws InterruptedException, ProcessException, IOException {
		// Bleah
		int idx = 0;

		setupRegisters(new RegisterSet(FPGA_ALL, SCRM, CLEAR),
				new RegisterSet(FPGA_ALL, SLEN, SET),
				new RegisterSet(FPGA_ALL, LEDO, CLEAR));
		sleep(SMALL_SLEEP);

		String nameDef = firmware.bitfileNames.get(idx);
		setupBitfile(nameDef, 0, FPGA_ALL);
		idx++;

		sleep(SMALL_SLEEP);

		setupBitfile(firmware.bitfileNames.get(idx), idx, FPGA_E_S);
		idx++;
		setupBitfile(firmware.bitfileNames.get(idx), idx, FPGA_SW_W);
		idx++;
		setupBitfile(firmware.bitfileNames.get(idx), idx, FPGA_N_NE);

		// TODO these read the configuration... but are they necessary?
		listFPGABootChunks();
		logBMPVersion();

		if (postDelay) {
			log.info("beginning post-load delay");
			sleep(BIG_SLEEP);
			log.info("completed post-load delay");
		}
	}
}

enum DataSectorTypes {
	/** Chunk describes a bitfile to load. */
	BITFILE(3),
	/** Chunk describes some registers to set. */
	REGISTER(4),
	/** Chunk is not recognised. */
	@Deprecated
	UNKNOWN(-1);

	/** The value of the chunk's type code. */
	final byte value;

	DataSectorTypes(int value) {
		this.value = (byte) value;
	}

	static DataSectorTypes get(byte val) {
		if (val == BITFILE.value) {
			return BITFILE;
		} else if (val == REGISTER.value) {
			return REGISTER;
		} else {
			return UNKNOWN;
		}
	}
}

@Component
class FirmwareDefinition {
	private static final Logger log = getLogger(FirmwareDefinition.class);

	@Value("classpath:bitfiles/manifest.properties")
	private Resource manifestLocation;

	/** The <em>ordered</em> list of firmware filenames from the manifest. */
	List<String> bitfileNames;

	/** Where to load each of the bitfiles from. */
	private Map<String, Resource> bitFiles = new HashMap<>();

	/**
	 * What the intended modification time of each of the bitfiles is. From the
	 * manifest, because actual file modification times are broken.
	 */
	private Map<String, Integer> modTimes = new HashMap<>();

	@PostConstruct
	private void loadManifest() throws IOException {
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
				log.info("loaded firmware definition: {}", f);
			} catch (IOException e) {
				FileNotFoundException fnf = new FileNotFoundException(
						"failed to open bitfile resource: " + r);
				fnf.initCause(e);
				throw fnf;
			}
		}
	}

	Resource resource(String handle) {
		return bitFiles.get(handle);
	}

	int mtime(String handle) {
		return modTimes.get(handle);
	}
}
