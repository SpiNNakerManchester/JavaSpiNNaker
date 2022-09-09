/*
 * Copyright (c) 2018-2022 The University of Manchester
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

import static java.lang.Math.min;
import static java.lang.Thread.interrupted;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static uk.ac.manchester.spinnaker.machine.MemoryLocation.NULL;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer.FLASH_CHUNK_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;
import static uk.ac.manchester.spinnaker.transceiver.BMPConstants.BLACKLIST_BLANK;
import static uk.ac.manchester.spinnaker.transceiver.BMPConstants.BMP_BOOT_BLACKLIST_OFFSET;
import static uk.ac.manchester.spinnaker.transceiver.BMPConstants.BMP_BOOT_CRC_OFFSET;
import static uk.ac.manchester.spinnaker.transceiver.BMPConstants.BMP_BOOT_SECTOR_ADDR;
import static uk.ac.manchester.spinnaker.transceiver.BMPConstants.BMP_BOOT_SECTOR_SIZE;
import static uk.ac.manchester.spinnaker.transceiver.BMPConstants.SF_BL_ADDR;
import static uk.ac.manchester.spinnaker.transceiver.BMPConstants.SF_BL_LEN;
import static uk.ac.manchester.spinnaker.transceiver.Utils.crc;
import static uk.ac.manchester.spinnaker.transceiver.Utils.fill;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Set;

import uk.ac.manchester.spinnaker.machine.MemoryLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.messages.model.FPGALinkRegisters;
import uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters;
import uk.ac.manchester.spinnaker.messages.model.FPGARecevingLinkCounters;
import uk.ac.manchester.spinnaker.messages.model.FPGASendingLinkCounters;
import uk.ac.manchester.spinnaker.messages.model.FirmwareDescriptor;
import uk.ac.manchester.spinnaker.messages.model.FirmwareDescriptors;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.utils.MappableIterable;

/**
 * The interface supported by the {@link Transceiver} for talking to a BMP.
 * Emulates a lot of default handling and variant-type handling by Python.
 * <p>
 * Note that operations on a particular BMP (i.e., with the same
 * {@link BMPCoords}) are <strong>always</strong> thread-unsafe.
 *
 * @author Donal Fellows
 */
public interface BMPTransceiverInterface {
	/**
	 * Set the default BMP coordinates, at least for cabinet and frame.
	 *
	 * @param bmp
	 *            The new default coordinates.
	 */
	void bind(BMPCoords bmp);

	/**
	 * @return The currently bound BMP coordinates. Defaults to 0,0 if not set
	 *         by {@link #bind(BMPCoords)}.
	 */
	BMPCoords getBoundBMP();

	/**
	 * List which boards are actually available to be manipulated by a
	 * particular BMP.
	 *
	 * @param bmp
	 *            Which BMP are we asking?
	 * @return Ordered list of board identifiers.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	MappableIterable<BMPBoard> availableBoards(BMPCoords bmp)
			throws IOException, ProcessException;

	/**
	 * List which boards are actually available to be manipulated by the default
	 * BMP.
	 *
	 * @return Ordered list of board identifiers.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default MappableIterable<BMPBoard> availableBoards()
			throws IOException, ProcessException {
		return availableBoards(getBoundBMP());
	}

	/**
	 * Power on the whole machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	void powerOnMachine()
			throws InterruptedException, IOException, ProcessException;

	/**
	 * Power on some boards in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param boards
	 *            The boards to power on (managed by default BMP)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void powerOn(Collection<BMPBoard> boards)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_ON, getBoundBMP(), boards);
	}

	/**
	 * Power on a board in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param board
	 *            The board to power on (managed by default BMP)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void powerOn(BMPBoard board)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_ON, getBoundBMP(), Set.of(board));
	}

	/**
	 * Power off the whole machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	void powerOffMachine()
			throws InterruptedException, IOException, ProcessException;

	/**
	 * Power off some boards in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param boards
	 *            The boards to power off (managed by default BMP)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void powerOff(Collection<BMPBoard> boards)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_OFF, getBoundBMP(), boards);
	}

	/**
	 * Power off a board in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param board
	 *            The board to power off (in the bound BMP)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void powerOff(BMPBoard board)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_OFF, getBoundBMP(), Set.of(board));
	}

	/**
	 * Send a power request to the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param powerCommand
	 *            The power command to send
	 * @param bmp
	 *            the coordinates of the BMP; components are zero for a board
	 *            not in a frame of a cabinet
	 * @param boards
	 *            The boards to send the command to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	void power(PowerCommand powerCommand, BMPCoords bmp,
			Collection<BMPBoard> boards)
			throws InterruptedException, IOException, ProcessException;

	/**
	 * Set the LED state of a board in the machine.
	 *
	 * @param leds
	 *            Collection of LED numbers to set the state of (0-7)
	 * @param action
	 *            State to set the LED to, either on, off or toggle
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param boards
	 *            Specifies the board to control the LEDs of. The command will
	 *            actually be sent to the first board in the collection.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	void setLED(Collection<Integer> leds, LEDAction action, BMPCoords bmp,
			Collection<BMPBoard> boards) throws IOException, ProcessException;

	/**
	 * Set the LED state of a board in the machine.
	 *
	 * @param leds
	 *            Collection of LED numbers to set the state of (0-7)
	 * @param action
	 *            State to set the LED to, either on, off or toggle
	 * @param board
	 *            Specifies the board to control the LEDs of.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void setLED(Collection<Integer> leds, LEDAction action,
			BMPBoard board) throws IOException, ProcessException {
		setLED(leds, action, getBoundBMP(), Set.of(board));
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register to read.
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGARegister(FPGA fpga, FPGAMainRegisters register,
			BMPBoard board) throws IOException, ProcessException {
		return readFPGARegister(fpga, register, getBoundBMP(), board);
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register to read.
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGARegister(FPGA fpga, FPGAMainRegisters register,
			BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpga, register.getAddress(), bmp, board);
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param registerBank
	 *            Which bank of link registers to read from.
	 * @param register
	 *            Register to read.
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGARegister(FPGA fpga, int registerBank,
			FPGALinkRegisters register, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpga, registerBank, register, getBoundBMP(),
				board);
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param registerBank
	 *            Which bank of link registers to read from.
	 * @param register
	 *            Register to read.
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGARegister(FPGA fpga, int registerBank,
			FPGALinkRegisters register, BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpga, register.address(registerBank), bmp,
				board);
	}

	/**
	 * Read a link counter on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param linkNumber
	 *            Which bank of link counters to read from.
	 * @param counter
	 *            Counter to read.
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGALinkCounter(FPGA fpga, int linkNumber,
			FPGARecevingLinkCounters counter, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGALinkCounter(fpga, linkNumber, counter, getBoundBMP(),
				board);
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param linkNumber
	 *            Which bank of link counters to read from.
	 * @param counter
	 *            Counter to read.
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGALinkCounter(FPGA fpga, int linkNumber,
			FPGARecevingLinkCounters counter, BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpga, counter.address(linkNumber), bmp, board);
	}

	/**
	 * Read a link counter on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param linkNumber
	 *            Which bank of link counters to read from.
	 * @param counter
	 *            Counter to read.
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGALinkCounter(FPGA fpga, int linkNumber,
			FPGASendingLinkCounters counter, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGALinkCounter(fpga, linkNumber, counter, getBoundBMP(),
				board);
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param linkNumber
	 *            Which bank of link counters to read from.
	 * @param counter
	 *            Counter to read.
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGALinkCounter(FPGA fpga, int linkNumber,
			FPGASendingLinkCounters counter, BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpga, counter.address(linkNumber), bmp, board);
	}

	/**
	 * Read a register on a FPGA of a board. The meaning of the register's
	 * contents will depend on the FPGA's configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGARegister(FPGA fpga, MemoryLocation register,
			BMPBoard board) throws IOException, ProcessException {
		return readFPGARegister(fpga, register, getBoundBMP(), board);
	}

	/**
	 * Read a register on a FPGA of a board. The meaning of the register's
	 * contents will depend on the FPGA's configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	int readFPGARegister(FPGA fpga, MemoryLocation register, BMPCoords bmp,
			BMPBoard board) throws IOException, ProcessException;

	/**
	 * Write a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register to write.
	 * @param value
	 *            the value to write into the FPGA register
	 * @param board
	 *            which board to write the FPGA register to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void writeFPGARegister(FPGA fpga, FPGAMainRegisters register,
			int value, BMPBoard board) throws IOException, ProcessException {
		writeFPGARegister(fpga, register, value, getBoundBMP(), board);
	}

	/**
	 * Write a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register to write.
	 * @param value
	 *            the value to write into the FPGA register
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to write the FPGA register to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void writeFPGARegister(FPGA fpga, FPGAMainRegisters register,
			int value, BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		writeFPGARegister(fpga, register.getAddress(), value, bmp, board);
	}

	/**
	 * Write a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param registerBank
	 *            Which bank of link registers to read from.
	 * @param register
	 *            Register to write.
	 * @param value
	 *            the value to write into the FPGA register
	 * @param board
	 *            which board to write the FPGA register to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void writeFPGARegister(FPGA fpga, int registerBank,
			FPGALinkRegisters register, int value, BMPBoard board)
			throws IOException, ProcessException {
		writeFPGARegister(fpga, registerBank, register, value, getBoundBMP(),
				board);
	}

	/**
	 * Write a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param registerBank
	 *            Which bank of link registers to read from.
	 * @param register
	 *            Register to write.
	 * @param value
	 *            the value to write into the FPGA register
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to write the FPGA register to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void writeFPGARegister(FPGA fpga, int registerBank,
			FPGALinkRegisters register, int value, BMPCoords bmp,
			BMPBoard board) throws IOException, ProcessException {
		writeFPGARegister(fpga, register.address(registerBank), value, bmp,
				board);
	}

	/**
	 * Write a register on a FPGA of a board. The meaning of setting the
	 * register's contents will depend on the FPGA's configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param value
	 *            the value to write into the FPGA register
	 * @param board
	 *            which board to write the FPGA register to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void writeFPGARegister(FPGA fpga, MemoryLocation register,
			int value, BMPBoard board) throws IOException, ProcessException {
		writeFPGARegister(fpga, register, value, getBoundBMP(), board);
	}

	/**
	 * Write a register on a FPGA of a board. The meaning of setting the
	 * register's contents will depend on the FPGA's configuration.
	 *
	 * @param fpga
	 *            FPGA (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param value
	 *            the value to write into the FPGA register
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to write the FPGA register to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	void writeFPGARegister(FPGA fpga, MemoryLocation register, int value,
			BMPCoords bmp, BMPBoard board) throws IOException, ProcessException;

	/**
	 * Read the ADC data.
	 *
	 * @param board
	 *            which board to request the ADC data from
	 * @return the FPGA's ADC data object
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default ADCInfo readADCData(BMPBoard board)
			throws IOException, ProcessException {
		return readADCData(getBoundBMP(), board);
	}

	/**
	 * Read the ADC data.
	 *
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to request the ADC data from
	 * @return the FPGA's ADC data object
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	ADCInfo readADCData(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException;

	/**
	 * Read the BMP version.
	 *
	 * @param boards
	 *            which board to request the data from; the first board in the
	 *            collection will be queried
	 * @return the parsed SVER from the BMP
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelUnsafe
	default VersionInfo readBMPVersion(
			Iterable<BMPBoard> boards) throws IOException, ProcessException {
		return readBMPVersion(getBoundBMP(),
				boards.iterator().next());
	}

	/**
	 * Read the BMP version.
	 *
	 * @param board
	 *            which board to request the data from
	 * @return the parsed SVER from the BMP
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default VersionInfo readBMPVersion(BMPBoard board)
			throws IOException, ProcessException {
		return readBMPVersion(getBoundBMP(), board);
	}

	/**
	 * Read the BMP version.
	 *
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param boards
	 *            which board to request the data from; the first board in the
	 *            collection will be queried
	 * @return the parsed SVER from the BMP
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelUnsafe
	default VersionInfo readBMPVersion(BMPCoords bmp, Iterable<BMPBoard> boards)
			throws IOException, ProcessException {
		return readBMPVersion(bmp, boards.iterator().next());
	}

	/**
	 * Read the BMP version.
	 *
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to request the data from
	 * @return the parsed SVER from the BMP
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	VersionInfo readBMPVersion(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException;

	/**
	 * Read the BMP firmware descriptor.
	 *
	 * @param board
	 *            which board to request the descriptor from
	 * @param type
	 *            Which firmware descriptor to read
	 * @return the firmware descriptor
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default FirmwareDescriptor readBMPFirmwareDescriptor(
			BMPBoard board, FirmwareDescriptors type)
			throws IOException, ProcessException {
		return readBMPFirmwareDescriptor(getBoundBMP(), board, type);
	}

	/**
	 * Read the BMP firmware descriptor.
	 *
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            which board to request the descriptor from
	 * @param type
	 *            Which firmware descriptor to read
	 * @return the firmware descriptor
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default FirmwareDescriptor readBMPFirmwareDescriptor(BMPCoords bmp,
			BMPBoard board, FirmwareDescriptors type)
			throws IOException, ProcessException {
		return new FirmwareDescriptor(type,
				readBMPMemory(bmp, board, type.address, type.blockSize));
	}

	/**
	 * Get the FPGA reset status.
	 *
	 * @param board
	 *            Which board are the FPGAs on?
	 * @return What the state of the reset line is.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default boolean getResetStatus(BMPBoard board)
			throws IOException, ProcessException {
		return getResetStatus(getBoundBMP(), board);
	}

	/**
	 * Get the FPGA reset status.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board are the FPGAs on?
	 * @return What the state of the reset line is.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	boolean getResetStatus(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException;

	/**
	 * Get the address of the serial flash buffer.
	 *
	 * @param board
	 *            Which BMP's buffer do we want the address of?
	 * @return The adress of the serial flash buffer.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default MemoryLocation getSerialFlashBuffer(BMPBoard board)
			throws IOException, ProcessException {
		return getSerialFlashBuffer(getBoundBMP(), board);
	}

	/**
	 * Get the address of the serial flash buffer.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which BMP's buffer do we want the address of?
	 * @return The adress of the serial flash buffer.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	MemoryLocation getSerialFlashBuffer(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException;

	/** The type of reset to perform. */
	enum FPGAResetType {
		// NB: The order of these values is important
		/** Reset by taking the reset line low. */
		LOW,
		/** Reset by taking the reset line high. */
		HIGH,
		/** Reset by sending a pulse on the reset line. */
		PULSE
	}

	/**
	 * Reset the FPGAs on a board.
	 *
	 * @param board
	 *            Which board are the FPGAs on?
	 * @param resetType
	 *            What kind of reset to perform.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default void resetFPGA(BMPBoard board, FPGAResetType resetType)
			throws IOException, ProcessException {
		resetFPGA(getBoundBMP(), board, resetType);
	}

	/**
	 * Reset the FPGAs on a board.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board are the FPGAs on?
	 * @param resetType
	 *            What kind of reset to perform.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	void resetFPGA(BMPCoords bmp, BMPBoard board, FPGAResetType resetType)
			throws IOException, ProcessException;

	/**
	 * Read BMP memory.
	 *
	 * @param board
	 *            Which board's BMP are we reading?
	 * @param baseAddress
	 *            The address in the BMP's memory where the region of memory to
	 *            be read starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data read, positioned at the start of
	 *         the data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default ByteBuffer readBMPMemory(BMPBoard board, MemoryLocation baseAddress,
			int length) throws IOException, ProcessException {
		return readBMPMemory(getBoundBMP(), board, baseAddress, length);
	}

	/**
	 * Read BMP memory.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we reading?
	 * @param baseAddress
	 *            The address in the BMP's memory where the region of memory to
	 *            be read starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data read, positioned at the start of
	 *         the data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	ByteBuffer readBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException;

	/**
	 * Read BMP memory.
	 *
	 * @param board
	 *            Which board's BMP are we reading?
	 * @param address
	 *            The address in the BMP's memory where the region of memory to
	 *            be read starts
	 * @return The little-endian word at the given address.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default int readBMPMemoryWord(BMPBoard board, MemoryLocation address)
			throws IOException, ProcessException {
		return readBMPMemoryWord(getBoundBMP(), board, address);
	}

	/**
	 * Read BMP memory.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we reading?
	 * @param address
	 *            The address in the BMP's memory where the region of memory to
	 *            be read starts
	 * @return The little-endian word at the given address.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default int readBMPMemoryWord(BMPCoords bmp, BMPBoard board,
			MemoryLocation address) throws IOException, ProcessException {
		var b = readBMPMemory(bmp, board, address, WORD_SIZE);
		return b.getInt(0);
	}

	/**
	 * Write BMP memory.
	 *
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's memory where the region of memory to
	 *            be written starts
	 * @param data
	 *            The data to be written, extending from the <i>position</i> to
	 *            the <i>limit</i>. The contents of this buffer will be
	 *            unchanged.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default void writeBMPMemory(BMPBoard board, MemoryLocation baseAddress,
			ByteBuffer data) throws IOException, ProcessException {
		writeBMPMemory(getBoundBMP(), board, baseAddress, data);
	}

	/**
	 * Write BMP memory.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's memory where the region of memory to
	 *            be written starts
	 * @param data
	 *            The data to be written, extending from the <i>position</i> to
	 *            the <i>limit</i>. The contents of this buffer will be
	 *            unchanged.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void writeBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data)
			throws IOException, ProcessException;

	/**
	 * Write BMP memory.
	 *
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's memory where the region of memory to
	 *            be written starts
	 * @param dataWord
	 *            The data to be written as a 4-byte little-endian value.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default void writeBMPMemory(BMPBoard board, MemoryLocation baseAddress,
			int dataWord) throws IOException, ProcessException {
		writeBMPMemory(getBoundBMP(), board, baseAddress, dataWord);
	}

	/**
	 * Write BMP memory.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's memory where the region of memory to
	 *            be written starts
	 * @param dataWord
	 *            The data to be written as a 4-byte little-endian value.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default void writeBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int dataWord)
			throws IOException, ProcessException {
		var data = allocate(WORD_SIZE).order(LITTLE_ENDIAN);
		data.putInt(dataWord);
		data.flip();
		writeBMPMemory(bmp, board, baseAddress, data);
	}

	/**
	 * Write BMP memory from a file.
	 *
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's memory where the region of memory to
	 *            be written starts
	 * @param file
	 *            The file containing the data to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking or file I/O.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default void writeBMPMemory(BMPBoard board, MemoryLocation baseAddress,
			File file) throws IOException, ProcessException {
		writeBMPMemory(getBoundBMP(), board, baseAddress, file);
	}

	/**
	 * Write BMP memory from a file.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's memory where the region of memory to
	 *            be written starts
	 * @param file
	 *            The file containing the data to be written.
	 * @throws IOException
	 *             If anything goes wrong with networking or file I/O.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void writeBMPMemory(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, File file)
			throws IOException, ProcessException;

	/**
	 * Read BMP serial flash memory.
	 *
	 * @param board
	 *            Which board's BMP are we reading?
	 * @param baseAddress
	 *            The address in the BMP's serial flash where the region of
	 *            memory to be read starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data read, positioned at the start of
	 *         the data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default ByteBuffer readSerialFlash(BMPBoard board,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException {
		return readSerialFlash(getBoundBMP(), board, baseAddress, length);
	}

	/**
	 * Read BMP serial flash memory.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we reading?
	 * @param baseAddress
	 *            The address in the BMP's serial flash where the region of
	 *            memory to be read starts
	 * @param length
	 *            The length of the data to be read in bytes
	 * @return A little-endian buffer of data read, positioned at the start of
	 *         the data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	ByteBuffer readSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException;

	/**
	 * Read the BMP serial number from a board.
	 *
	 * @param bmp
	 *            Which BMP are we sending messages to directly?
	 * @param board
	 *            Which board's BMP (of those managed by the BMP we send the
	 *            message to) are we getting the serial number from?
	 * @return The LPC1768 serial number.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	String readBoardSerialNumber(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException;

	/**
	 * Read the BMP serial number from a board.
	 *
	 * @param board
	 *            Which board's BMP are we reading the serial number of? Must
	 *            be one controlled by the current bound BMP.
	 * @return The LPC1768 serial number.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default String readBoardSerialNumber(BMPBoard board)
			throws ProcessException, IOException {
		return readBoardSerialNumber(getBoundBMP(), board);
	}

	/**
	 * Read the blacklist from a board.
	 *
	 * @param bmp
	 *            Which BMP are we sending messages to directly?
	 * @param board
	 *            Which board's blacklist are we reading?
	 * @return The contents of the blacklist.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default Blacklist readBlacklist(BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		return new Blacklist(
				readSerialFlash(bmp, board, SF_BL_ADDR, SF_BL_LEN));
	}

	/**
	 * Read the blacklist from a board.
	 *
	 * @param board
	 *            Which board's blacklist are we reading? Must
	 *            be one controlled by the current bound BMP.
	 * @return The contents of the blacklist.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default Blacklist readBlacklist(BMPBoard board)
			throws IOException, ProcessException {
		return readBlacklist(getBoundBMP(), board);
	}

	/**
	 * Write a blacklist to a board. Note that this is a non-transactional
	 * operation!
	 *
	 * @param board
	 *            Which board's BMP to write to.
	 * @param blacklist
	 *            The blacklist to write.
	 * @throws ProcessException
	 *             If a BMP rejects a message.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws InterruptedException
	 *             If interrupted. Interruption can happen <em>prior</em> to
	 *             commencing the actual writes.
	 */
	default void writeBlacklist(BMPBoard board, Blacklist blacklist)
			throws ProcessException, IOException, InterruptedException {
		writeBlacklist(getBoundBMP(), board, blacklist);
	}

	/**
	 * Write a blacklist to a board. Note that this is a non-transactional
	 * operation!
	 *
	 * @param bmp
	 *            The BMP to send communications via.
	 * @param board
	 *            Which board's BMP to write to.
	 * @param blacklist
	 *            The blacklist to write.
	 * @throws ProcessException
	 *             If a BMP rejects a message.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws InterruptedException
	 *             If interrupted. Interruption can happen <em>prior</em> to
	 *             commencing the actual writes.
	 */
	default void writeBlacklist(BMPCoords bmp, BMPBoard board,
			Blacklist blacklist)
			throws ProcessException, IOException, InterruptedException {
		// Clear the interrupt status
		interrupted();

		// Prepare the boot data
		var data = allocate(BMP_BOOT_SECTOR_SIZE);
		data.order(LITTLE_ENDIAN);
		data.put(readBMPMemory(bmp, board, BMP_BOOT_SECTOR_ADDR,
				BMP_BOOT_SECTOR_SIZE));
		fill(data, BMP_BOOT_BLACKLIST_OFFSET, SF_BL_LEN, BLACKLIST_BLANK);
		data.position(BMP_BOOT_BLACKLIST_OFFSET);
		data.put(blacklist.getRawData());
		data.putInt(BMP_BOOT_CRC_OFFSET,
				crc(data, 0, BMP_BOOT_BLACKLIST_OFFSET));

		if (interrupted()) {
			throw new InterruptedException(
					"interrupted while reading boot data");
		}

		// Prepare the serial flash update; must read part of the data first
		var sfData = new byte[SF_BL_ADDR.address + SF_BL_LEN];
		readSerialFlash(bmp, board, NULL, SF_BL_ADDR.address).get(sfData, 0,
				SF_BL_ADDR.address);
		data.position(BMP_BOOT_BLACKLIST_OFFSET);
		data.get(sfData, SF_BL_ADDR.address, SF_BL_LEN);

		data.position(0); // Prep for write

		if (interrupted()) {
			throw new InterruptedException(
					"interrupted while reading serial flash");
		}

		// Do the actual writes here; any failure before here is unimportant
		writeFlash(bmp, board, BMP_BOOT_SECTOR_ADDR, data, true);
		writeSerialFlash(bmp, board, NULL, ByteBuffer.wrap(sfData));
	}

	/**
	 * Read the CRC32 checksum of BMP serial flash memory.
	 *
	 * @param board
	 *            Which board's BMP are we reading?
	 * @param baseAddress
	 *            The address in the BMP's serial flash where the region of
	 *            memory to be checksummed starts
	 * @param length
	 *            The length of the data to be checksummed in bytes
	 * @return The CRC32 checksum
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	default int readSerialFlashCRC(BMPBoard board, MemoryLocation baseAddress,
			int length) throws IOException, ProcessException {
		return readSerialFlashCRC(getBoundBMP(), board, baseAddress, length);
	}

	/**
	 * Read the CRC32 checksum of BMP serial flash memory.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we reading?
	 * @param baseAddress
	 *            The address in the BMP's serial flash where the region of
	 *            memory to be checksummed starts
	 * @param length
	 *            The length of the data to be checksummed in bytes
	 * @return The CRC32 checksum
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafeWithCare
	int readSerialFlashCRC(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int length)
			throws IOException, ProcessException;

	/**
	 * Write BMP serial flash memory from a file.
	 *
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's serial flash where the region of
	 *            memory to be written starts
	 * @param file
	 *            The file containing the data to be written
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default void writeSerialFlash(BMPBoard board, MemoryLocation baseAddress,
			File file) throws ProcessException, IOException {
		writeSerialFlash(getBoundBMP(), board, baseAddress, file);
	}

	/**
	 * Write BMP serial flash memory from a file.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's serial flash where the region of
	 *            memory to be written starts
	 * @param file
	 *            The file containing the data to be written
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, File file)
			throws ProcessException, IOException;

	/**
	 * Write BMP serial flash memory from a stream.
	 *
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's serial flash where the region of
	 *            memory to be written starts
	 * @param size
	 *            How many bytes to write from the stream
	 * @param stream
	 *            The file containing the data to be written
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default void writeSerialFlash(BMPBoard board, MemoryLocation baseAddress,
			int size, InputStream stream) throws ProcessException, IOException {
		writeSerialFlash(getBoundBMP(), board, baseAddress, size, stream);
	}

	/**
	 * Write BMP serial flash memory from a stream.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's serial flash where the region of
	 *            memory to be written starts
	 * @param size
	 *            How many bytes to write from the stream
	 * @param stream
	 *            The file containing the data to be written
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int size, InputStream stream)
			throws ProcessException, IOException;

	/**
	 * Write BMP serial flash memory.
	 *
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's serial flash where the region of
	 *            memory to be written starts
	 * @param data
	 *            The raw data to be written
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default void writeSerialFlash(BMPBoard board, MemoryLocation baseAddress,
			ByteBuffer data) throws ProcessException, IOException {
		writeSerialFlash(getBoundBMP(), board, baseAddress, data);
	}

	/**
	 * Write BMP serial flash memory.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing?
	 * @param baseAddress
	 *            The address in the BMP's serial flash where the region of
	 *            memory to be written starts
	 * @param data
	 *            The raw data to be written
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void writeSerialFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data)
			throws ProcessException, IOException;

	/**
	 * Prepare a transfer area for writing to the flash memory of a BMP.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing to?
	 * @param baseAddress
	 *            Where in flash will we write?
	 * @param size
	 *            How much data will we write.
	 * @return The location of the working buffer on the BMP
	 * @deprecated This operation should not be used directly.
	 * @see #writeFlash(BMPCoords,BMPBoard,MemoryLocation,ByteBuffer,boolean)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@Deprecated
	MemoryLocation eraseBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, int size)
			throws IOException, ProcessException;

	/**
	 * Move an uploaded chunk of data into the working buffer for writing to the
	 * flash memory of a BMP.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing to?
	 * @param address
	 *            Where in the working buffer will we copy to?
	 * @deprecated This operation should not be used directly.
	 * @see #writeFlash(BMPCoords,BMPBoard,MemoryLocation,ByteBuffer,boolean)
	 * @see #eraseBMPFlash(BMPCoords,BMPBoard,MemoryLocation,int)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@Deprecated
	void chunkBMPFlash(BMPCoords bmp, BMPBoard board, MemoryLocation address)
			throws IOException, ProcessException;

	/**
	 * Finalise the writing of the flash memory of a BMP.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing to?
	 * @param baseAddress
	 *            Where in flash will we write?
	 * @param size
	 *            How much data will we write.
	 * @deprecated This operation should not be used directly.
	 * @see #writeFlash(BMPCoords,BMPBoard,MemoryLocation,ByteBuffer,boolean)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@Deprecated
	void copyBMPFlash(BMPCoords bmp, BMPBoard board, MemoryLocation baseAddress,
			int size) throws IOException, ProcessException;

	/**
	 * Write a {@linkplain WriteFlashBuffer#FLASH_CHUNK_SIZE fixed size} chunk
	 * to flash memory of a BMP with erase. The data must have already been
	 * written to the flash buffer.
	 *
	 * @param board
	 *            Which board's BMP are we writing to?
	 * @param baseAddress
	 *            Where in flash will we write?
	 * @see #writeFlash(BMPCoords,BMPBoard,MemoryLocation,ByteBuffer,boolean)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default void writeBMPFlash(BMPBoard board, MemoryLocation baseAddress)
			throws IOException, ProcessException {
		writeBMPFlash(getBoundBMP(), board, baseAddress);
	}

	/**
	 * Write a {@linkplain WriteFlashBuffer#FLASH_CHUNK_SIZE fixed size} chunk
	 * to flash memory of a BMP with erase. The data must have already been
	 * written to the flash buffer.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing to?
	 * @param baseAddress
	 *            Where in flash will we write?
	 * @see #writeFlash(BMPCoords,BMPBoard,MemoryLocation,ByteBuffer,boolean)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void writeBMPFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress) throws IOException, ProcessException;

	/**
	 * Write a buffer to flash memory on the BMP. This is a composite operation.
	 *
	 * @param board
	 *            Which board's BMP are we writing to?
	 * @param baseAddress
	 *            Where in flash will we write?
	 * @param data
	 *            What data will we write?
	 * @param update
	 *            Whether to trigger an immediate update of flash.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default void writeFlash(BMPBoard board, MemoryLocation baseAddress,
			ByteBuffer data, boolean update)
			throws ProcessException, IOException {
		writeFlash(getBoundBMP(), board, baseAddress, data, update);
	}

	/**
	 * Write a buffer to flash memory on the BMP. This is a composite operation.
	 *
	 * @param bmp
	 *            Which BMP are we talking to?
	 * @param board
	 *            Which board's BMP are we writing to?
	 * @param baseAddress
	 *            Where in flash will we write?
	 * @param data
	 *            What data will we write?
	 * @param update
	 *            Whether to trigger an immediate update of flash.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	default void writeFlash(BMPCoords bmp, BMPBoard board,
			MemoryLocation baseAddress, ByteBuffer data, boolean update)
			throws ProcessException, IOException {
		int size = data.remaining();
		var workingBuffer = eraseBMPFlash(bmp, board, baseAddress, size);
		var targetAddr = baseAddress;
		int offset = 0;

		while (true) {
			var buf = data.asReadOnlyBuffer();
			buf.position(offset)
					.limit(min(offset + FLASH_CHUNK_SIZE, buf.capacity()));
			int length = buf.remaining();
			if (length == 0) {
				break;
			}

			writeBMPMemory(bmp, board, workingBuffer, buf);
			chunkBMPFlash(bmp, board, targetAddr);
			if (length < FLASH_CHUNK_SIZE) {
				break;
			}
			targetAddr = targetAddr.add(FLASH_CHUNK_SIZE);
			offset += FLASH_CHUNK_SIZE;
		}
		if (update) {
			copyBMPFlash(bmp, board, baseAddress, size);
		}
	}
}

interface BMPConstants {
	/** Location in serial flash of blacklist. */
	MemoryLocation SF_BL_ADDR = new MemoryLocation(0x100);

	/** Size of blacklist, in bytes. */
	int SF_BL_LEN = 256;

	/** Offset of blacklist in boot sector of flash. */
	int BMP_BOOT_BLACKLIST_OFFSET = 0xe00;

	/** Location of boot sector of flash. */
	MemoryLocation BMP_BOOT_SECTOR_ADDR = new MemoryLocation(0x1000);

	/** Size of boot sector of flash. */
	int BMP_BOOT_SECTOR_SIZE = 0x1000;

	/** Offset of CRC in boot sector of flash. */
	int BMP_BOOT_CRC_OFFSET = BMP_BOOT_SECTOR_SIZE - WORD_SIZE;

	/** Byte used to blank out the space used for blacklists. */
	byte BLACKLIST_BLANK = (byte) 255;
}
