/*
 * Copyright (c) 2018-2021 The University of Manchester
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
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Collections.singleton;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer.FLASH_CHUNK_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;

import uk.ac.manchester.spinnaker.messages.bmp.BMPBoard;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.bmp.WriteFlashBuffer;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.FPGALinkRegisters;
import uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters;
import uk.ac.manchester.spinnaker.messages.model.FPGARecevingLinkCounters;
import uk.ac.manchester.spinnaker.messages.model.FPGASendingLinkCounters;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;

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
	 * Power on a set of boards in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param frame
	 *            the ID of the frame in the cabinet containing the boards, or 0
	 *            if the boards are not in a frame
	 * @param boards
	 *            The board or boards to power on
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	default void powerOn(int frame, Collection<BMPBoard> boards)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_ON, new BMPCoords(0, frame), boards);
	}

	/**
	 * Power on a set of boards in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param cabinet
	 *            the ID of the cabinet containing the frame, or 0 if the frame
	 *            is not in a cabinet
	 * @param frame
	 *            the ID of the frame in the cabinet containing the boards, or 0
	 *            if the boards are not in a frame
	 * @param boards
	 *            The board or boards to power on
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	default void powerOn(int cabinet, int frame, Collection<BMPBoard> boards)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_ON, new BMPCoords(cabinet, frame), boards);
	}

	/**
	 * Power on a board in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param board
	 *            The board to power off (in cabinet 0, frame 0)
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
		power(POWER_ON, new BMPCoords(0, 0), singleton(board));
	}

	/**
	 * Power on a board in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board, or 0
	 *            if the board is not in a frame
	 * @param board
	 *            The board to power on
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void powerOn(BMPBoard board, int frame)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_ON, new BMPCoords(0, frame), singleton(board));
	}

	/**
	 * Power on a board in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param cabinet
	 *            the ID of the cabinet containing the frame, or 0 if the frame
	 *            is not in a cabinet
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board, or 0
	 *            if the board is not in a frame
	 * @param board
	 *            The board to power on
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void powerOn(int cabinet, int frame, BMPBoard board)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_ON, new BMPCoords(cabinet, frame), singleton(board));
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
	 * Power off a set of boards in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board(s), or
	 *            0 if the board is not in a frame
	 * @param boards
	 *            The board or boards to power off
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	default void powerOff(int frame, Collection<BMPBoard> boards)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_OFF, new BMPCoords(0, frame), boards);
	}

	/**
	 * Power off a set of boards in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param cabinet
	 *            the ID of the cabinet containing the frame, or 0 if the frame
	 *            is not in a cabinet
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board(s), or
	 *            0 if the board is not in a frame
	 * @param boards
	 *            The board or boards to power off
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelUnsafe
	default void powerOff(int cabinet, int frame, Collection<BMPBoard> boards)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_OFF, new BMPCoords(cabinet, frame), boards);
	}

	/**
	 * Power off a board in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param board
	 *            The board to power off (in cabinet 0, frame 0)
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
		power(POWER_OFF, new BMPCoords(0, 0), singleton(board));
	}

	/**
	 * Power off a board in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board, or 0
	 *            if the board is not in a frame
	 * @param board
	 *            The board to power off
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void powerOff(int frame, BMPBoard board)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_OFF, new BMPCoords(0, frame), singleton(board));
	}

	/**
	 * Power off a board in the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param cabinet
	 *            the ID of the cabinet containing the frame, or 0 if the frame
	 *            is not in a cabinet
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board, or 0
	 *            if the board is not in a frame
	 * @param board
	 *            The board to power off
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting.
	 */
	@ParallelSafeWithCare
	default void powerOff(int cabinet, int frame, BMPBoard board)
			throws InterruptedException, IOException, ProcessException {
		power(POWER_OFF, new BMPCoords(cabinet, frame), singleton(board));
	}

	/**
	 * Send a power request to the machine.
	 * <p>
	 * <strong>WARNING!</strong> This operation is <em>unsafe</em> in a
	 * multi-threaded context.
	 *
	 * @param powerCommand
	 *            The power command to send
	 * @param cabinet
	 *            the ID of the cabinet containing the frame, or 0 if the frame
	 *            is not in a cabinet
	 * @param frame
	 *            the ID of the frame in the cabinet containing the board(s), or
	 *            0 if the board is not in a frame
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
	default void power(PowerCommand powerCommand, int cabinet, int frame,
			Collection<BMPBoard> boards)
			throws InterruptedException, IOException, ProcessException {
		power(powerCommand, new BMPCoords(cabinet, frame), boards);
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
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param boards
	 *            Specifies the board to control the LEDs of. The command will
	 *            actually be sent to the first board in the collection.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void setLED(Collection<Integer> leds, LEDAction action, int cabinet,
			int frame, Collection<BMPBoard> boards)
			throws IOException, ProcessException {
		setLED(leds, action, new BMPCoords(cabinet, frame), boards);
	}

	/**
	 * Set the LED state of a board in the machine.
	 *
	 * @param leds
	 *            Collection of LED numbers to set the state of (0-7)
	 * @param action
	 *            State to set the LED to, either on, off or toggle
	 * @param bmp
	 *            the coordinates of the BMP this is targeting
	 * @param board
	 *            Specifies the board to control the LEDs of. The command will
	 *            actually be sent to the first board in the collection.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	void setLED(Collection<Integer> leds, LEDAction action, BMPCoords bmp,
			Collection<BMPBoard> board) throws IOException, ProcessException;

	/**
	 * Set the LED state of a board in the machine.
	 *
	 * @param leds
	 *            Collection of LED numbers to set the state of (0-7)
	 * @param action
	 *            State to set the LED to, either on, off or toggle
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            Specifies the board to control the LEDs of.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void setLED(Collection<Integer> leds, LEDAction action, int cabinet,
			int frame, BMPBoard board) throws IOException, ProcessException {
		setLED(leds, action, new BMPCoords(cabinet, frame), singleton(board));
	}

	/**
	 * Set the LED state of a board in the machine.
	 *
	 * @param led
	 *            Number of the LED to set the state of (0-7)
	 * @param action
	 *            State to set the LED to, either on, off or toggle
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param boards
	 *            Specifies the board to control the LEDs of. The command will
	 *            actually be sent to the first board in the collection.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void setLED(int led, LEDAction action, int cabinet, int frame,
			Collection<BMPBoard> boards) throws IOException, ProcessException {
		setLED(singleton(led), action, new BMPCoords(cabinet, frame), boards);
	}

	/**
	 * Set the LED state of a board in the machine.
	 *
	 * @param led
	 *            Number of the LED to set the state of (0-7)
	 * @param action
	 *            State to set the LED to, either on, off or toggle
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            Specifies the board to control the LEDs of.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void setLED(int led, LEDAction action, int cabinet, int frame,
			BMPBoard board) throws IOException, ProcessException {
		setLED(singleton(led), action, new BMPCoords(cabinet, frame),
				singleton(board));
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register to read.
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGARegister(int fpgaNumber, FPGAMainRegisters register,
			int cabinet, int frame, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpgaNumber, register,
				new BMPCoords(cabinet, frame), board);
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
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
	default int readFPGARegister(int fpgaNumber, FPGAMainRegisters register,
			BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpgaNumber, register.getAddress(), bmp, board);
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param registerBank
	 *            Which bank of link registers to read from.
	 * @param register
	 *            Register to read.
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGARegister(int fpgaNumber, int registerBank,
			FPGALinkRegisters register, int cabinet, int frame, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpgaNumber, registerBank, register,
				new BMPCoords(cabinet, frame), board);
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
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
	default int readFPGARegister(int fpgaNumber, int registerBank,
			FPGALinkRegisters register, BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpgaNumber, register.address(registerBank), bmp,
				board);
	}

	/**
	 * Read a link counter on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param linkNumber
	 *            Which bank of link counters to read from.
	 * @param counter
	 *            Counter to read.
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGALinkCounter(int fpgaNumber, int linkNumber,
			FPGARecevingLinkCounters counter, int cabinet, int frame,
			BMPBoard board) throws IOException, ProcessException {
		return readFPGALinkCounter(fpgaNumber, linkNumber, counter,
				new BMPCoords(cabinet, frame), board);
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
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
	default int readFPGALinkCounter(int fpgaNumber, int linkNumber,
			FPGARecevingLinkCounters counter, BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpgaNumber, counter.address(linkNumber), bmp,
				board);
	}

	/**
	 * Read a link counter on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param linkNumber
	 *            Which bank of link counters to read from.
	 * @param counter
	 *            Counter to read.
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGALinkCounter(int fpgaNumber, int linkNumber,
			FPGASendingLinkCounters counter, int cabinet, int frame,
			BMPBoard board) throws IOException, ProcessException {
		return readFPGALinkCounter(fpgaNumber, linkNumber, counter,
				new BMPCoords(cabinet, frame), board);
	}

	/**
	 * Read a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
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
	default int readFPGALinkCounter(int fpgaNumber, int linkNumber,
			FPGASendingLinkCounters counter, BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		return readFPGARegister(fpgaNumber, counter.address(linkNumber), bmp,
				board);
	}

	/**
	 * Read a register on a FPGA of a board. The meaning of the register's
	 * contents will depend on the FPGA's configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the FPGA register from
	 * @return the register data
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default int readFPGARegister(int fpgaNumber, int register, int cabinet,
			int frame, BMPBoard board) throws IOException, ProcessException {
		return readFPGARegister(fpgaNumber, register,
				new BMPCoords(cabinet, frame), board);
	}

	/**
	 * Read a register on a FPGA of a board. The meaning of the register's
	 * contents will depend on the FPGA's configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
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
	int readFPGARegister(int fpgaNumber, int register, BMPCoords bmp,
			BMPBoard board) throws IOException, ProcessException;

	/**
	 * Write a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register to write.
	 * @param value
	 *            the value to write into the FPGA register
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to write the FPGA register to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void writeFPGARegister(int fpgaNumber, FPGAMainRegisters register,
			int value, int cabinet, int frame, BMPBoard board)
			throws IOException, ProcessException {
		writeFPGARegister(fpgaNumber, register, value,
				new BMPCoords(cabinet, frame), board);
	}

	/**
	 * Write a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
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
	default void writeFPGARegister(int fpgaNumber, FPGAMainRegisters register,
			int value, BMPCoords bmp, BMPBoard board)
			throws IOException, ProcessException {
		writeFPGARegister(fpgaNumber, register.getAddress(), value, bmp, board);
	}

	/**
	 * Write a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param registerBank
	 *            Which bank of link registers to read from.
	 * @param register
	 *            Register to write.
	 * @param value
	 *            the value to write into the FPGA register
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to write the FPGA register to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void writeFPGARegister(int fpgaNumber, int registerBank,
			FPGALinkRegisters register, int value, int cabinet, int frame,
			BMPBoard board) throws IOException, ProcessException {
		writeFPGARegister(fpgaNumber, registerBank, register, value,
				new BMPCoords(cabinet, frame), board);
	}

	/**
	 * Write a register on a FPGA of a board, assuming the standard FPGA
	 * configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
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
	default void writeFPGARegister(int fpgaNumber, int registerBank,
			FPGALinkRegisters register, int value, BMPCoords bmp,
			BMPBoard board) throws IOException, ProcessException {
		writeFPGARegister(fpgaNumber, register.address(registerBank), value,
				bmp, board);
	}

	/**
	 * Write a register on a FPGA of a board. The meaning of setting the
	 * register's contents will depend on the FPGA's configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
	 * @param register
	 *            Register address to read to (will be rounded down to the
	 *            nearest 32-bit word boundary).
	 * @param value
	 *            the value to write into the FPGA register
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to write the FPGA register to
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default void writeFPGARegister(int fpgaNumber, int register, int value,
			int cabinet, int frame, BMPBoard board)
			throws IOException, ProcessException {
		writeFPGARegister(fpgaNumber, register, value,
				new BMPCoords(cabinet, frame), board);
	}

	/**
	 * Write a register on a FPGA of a board. The meaning of setting the
	 * register's contents will depend on the FPGA's configuration.
	 *
	 * @param fpgaNumber
	 *            FPGA number (0, 1 or 2) to communicate with.
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
	void writeFPGARegister(int fpgaNumber, int register, int value,
			BMPCoords bmp, BMPBoard board) throws IOException, ProcessException;

	/**
	 * Read the ADC data.
	 *
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the ADC data from
	 * @return the FPGA's ADC data object
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default ADCInfo readADCData(int cabinet, int frame, BMPBoard board)
			throws IOException, ProcessException {
		return readADCData(new BMPCoords(cabinet, frame), board);
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
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
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
	default VersionInfo readBMPVersion(int cabinet, int frame,
			Iterable<BMPBoard> boards) throws IOException, ProcessException {
		return readBMPVersion(new BMPCoords(cabinet, frame),
				boards.iterator().next());
	}

	/**
	 * Read the BMP version.
	 *
	 * @param cabinet
	 *            the cabinet this is targeting
	 * @param frame
	 *            the frame this is targeting
	 * @param board
	 *            which board to request the data from
	 * @return the parsed SVER from the BMP
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@ParallelSafe
	default VersionInfo readBMPVersion(int cabinet, int frame, BMPBoard board)
			throws IOException, ProcessException {
		return readBMPVersion(new BMPCoords(cabinet, frame), board);
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
	int getSerialFlashBuffer(BMPCoords bmp, BMPBoard board)
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
	ByteBuffer readBMPMemory(BMPCoords bmp, BMPBoard board, int baseAddress,
			int length) throws IOException, ProcessException;

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
	default int readBMPMemoryWord(BMPCoords bmp, BMPBoard board, int address)
			throws IOException, ProcessException {
		ByteBuffer b = readBMPMemory(bmp, board, address, WORD_SIZE);
		return b.getInt(0);
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
	void writeBMPMemory(BMPCoords bmp, BMPBoard board, int baseAddress,
			ByteBuffer data) throws IOException, ProcessException;

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
	default void writeBMPMemory(BMPCoords bmp, BMPBoard board, int baseAddress,
			int dataWord) throws IOException, ProcessException {
		ByteBuffer data = ByteBuffer.allocate(WORD_SIZE);
		data.order(LITTLE_ENDIAN);
		data.putInt(dataWord);
		data.flip();
		writeBMPMemory(bmp, board, baseAddress, data);
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
	void writeBMPMemory(BMPCoords bmp, BMPBoard board, int baseAddress,
			File file) throws IOException, ProcessException;

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
	ByteBuffer readSerialFlash(BMPCoords bmp, BMPBoard board, int baseAddress,
			int length) throws IOException, ProcessException;

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
	int readSerialFlashCRC(BMPCoords bmp, BMPBoard board, int baseAddress,
			int length) throws IOException, ProcessException;

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
	void writeSerialFlash(BMPCoords bmp, BMPBoard board, int baseAddress,
			File file) throws ProcessException, IOException;

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
	void writeSerialFlash(BMPCoords bmp, BMPBoard board, int baseAddress,
			int size, InputStream stream) throws ProcessException, IOException;

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
	void writeSerialFlash(BMPCoords bmp, BMPBoard board, int baseAddress,
			ByteBuffer data) throws ProcessException, IOException;

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
	 * @see #writeFlash(BMPCoords,BMPBoard,int,ByteBuffer,boolean)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@Deprecated
	int eraseBMPFlash(BMPCoords bmp, BMPBoard board, int baseAddress, int size)
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
	 * @see #writeFlash(BMPCoords,BMPBoard,int,ByteBuffer,boolean)
	 * @see #eraseBMPFlash(BMPCoords,BMPBoard,int,int)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@Deprecated
	void chunkBMPFlash(BMPCoords bmp, BMPBoard board, int address)
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
	 * @see #writeFlash(BMPCoords,BMPBoard,int,ByteBuffer,boolean)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	@Deprecated
	void copyBMPFlash(BMPCoords bmp, BMPBoard board, int baseAddress, int size)
			throws IOException, ProcessException;

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
	 * @see #writeFlash(BMPCoords,BMPBoard,int,ByteBuffer,boolean)
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 */
	void writeBMPFlash(BMPCoords bmp, BMPBoard board, int baseAddress)
			throws IOException, ProcessException;

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
	default void writeFlash(BMPCoords bmp, BMPBoard board, int baseAddress,
			ByteBuffer data, boolean update)
			throws ProcessException, IOException {
		int size = data.remaining();
		int bufferBase = eraseBMPFlash(bmp, board, baseAddress, size);
		int offset = 0;

		while (true) {
			ByteBuffer buf = data.asReadOnlyBuffer();
			buf.position(offset)
					.limit(min(offset + FLASH_CHUNK_SIZE, buf.capacity()));
			int length = buf.remaining();
			if (length == 0) {
				break;
			}

			writeBMPMemory(bmp, board, bufferBase, buf);
			chunkBMPFlash(bmp, board, baseAddress);
			if (length < FLASH_CHUNK_SIZE) {
				break;
			}
			baseAddress += FLASH_CHUNK_SIZE;
			offset += FLASH_CHUNK_SIZE;
		}
		if (update) {
			copyBMPFlash(bmp, board, baseAddress, size);
		}
	}
}
