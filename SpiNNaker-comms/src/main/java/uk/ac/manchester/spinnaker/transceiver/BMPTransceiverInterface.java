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

import static java.util.Collections.singleton;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;

import java.io.IOException;
import java.util.Collection;

import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.LEDAction;
import uk.ac.manchester.spinnaker.messages.model.PowerCommand;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;

/**
 * The interface supported by the {@link Transceiver} for talking to a BMP.
 * Emulates a lot of default handling and variant-type handling by Python.
 * <p>
 * Note that operations on a BMP are <strong>always</strong> thread-unsafe.
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
	default void powerOn(int frame, Collection<Integer> boards)
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
	default void powerOn(int cabinet, int frame, Collection<Integer> boards)
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
	default void powerOn(int board)
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
	default void powerOn(int board, int frame)
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
	default void powerOn(int cabinet, int frame, int board)
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
	default void powerOff(int frame, Collection<Integer> boards)
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
	default void powerOff(int cabinet, int frame, Collection<Integer> boards)
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
	default void powerOff(int board)
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
	default void powerOff(int frame, int board)
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
	default void powerOff(int cabinet, int frame, int board)
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
			Collection<Integer> boards)
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
			Collection<Integer> boards)
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
			int frame, Collection<Integer> boards)
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
			Collection<Integer> board) throws IOException, ProcessException;

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
			int frame, int board) throws IOException, ProcessException {
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
			Collection<Integer> boards) throws IOException, ProcessException {
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
			int board) throws IOException, ProcessException {
		setLED(singleton(led), action, new BMPCoords(cabinet, frame),
				singleton(board));
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
			int frame, int board) throws IOException, ProcessException {
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
	int readFPGARegister(int fpgaNumber, int register, BMPCoords bmp, int board)
			throws IOException, ProcessException;

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
			int cabinet, int frame, int board)
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
			BMPCoords bmp, int board) throws IOException, ProcessException;

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
	default ADCInfo readADCData(int cabinet, int frame, int board)
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
	ADCInfo readADCData(BMPCoords bmp, int board)
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
			Iterable<Integer> boards) throws IOException, ProcessException {
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
	default VersionInfo readBMPVersion(int cabinet, int frame, int board)
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
	default VersionInfo readBMPVersion(BMPCoords bmp, Iterable<Integer> boards)
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
	@ParallelSafe
	VersionInfo readBMPVersion(BMPCoords bmp, int board)
			throws IOException, ProcessException;
}
