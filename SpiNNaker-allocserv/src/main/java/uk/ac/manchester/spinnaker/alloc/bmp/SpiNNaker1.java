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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.model.FPGALinkRegisters.STOP;
import static uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters.FLAG;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_OFF;
import static uk.ac.manchester.spinnaker.messages.model.PowerCommand.POWER_ON;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties.TxrxProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.alloc.model.FpgaIdentifiers;
import uk.ac.manchester.spinnaker.messages.bmp.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.VersionInfo;
import uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;

/**
 * Implementation of controller for one BMP of one SpiNNaker 1 based system.
 * This is a short-lived component, which is why it isn't merged with the
 * longer-lived {@link TransceiverFactory}.
 */
@Component
@Scope("prototype")
class SpiNNaker1 implements SpiNNakerControl {
	// ----------------------------------------------------------------
	// CORE BMP ACCESS FUNCTIONS

	private static final Logger log = getLogger(SpiNNaker1.class);

	private static final int FPGA_FLAG_ID_MASK = 0x3;

	private static final int BMP_VERSION_MIN = 2;

	/**
	 * We <em>always</em> pretend to talk to the root BMP of a machine (actually
	 * the root of a frame), and never directly to any others. The BMPs use a
	 * CAN bus and I<sup>2</sup>C to communicate with each other on our behalf.
	 */
	private static final BMPCoords ROOT_BMP = new BMPCoords(0, 0);

	@Autowired
	private TxrxProperties props;

	@Autowired
	private TransceiverFactoryAPI<?> txrxFactory;

	private final BMPCoords bmp;

	private final Machine machine;

	/**
	 * The transceiver for talking to the machine.
	 */
	private BMPTransceiverInterface txrx;

	private Map<Integer, Integer> idToBoard;

	/**
	 * @param machine
	 *            The machine hosting the boards and FPGAs.
	 * @param bmp
	 *            Which BMP on the machine are we really talking to.
	 */
	SpiNNaker1(Machine machine, BMPCoords bmp) {
		this.machine = machine;
		this.bmp = bmp;
	}

	@PostConstruct
	void initTransceiver() throws IOException, SpinnmanException {
		this.txrx = txrxFactory.getTransciever(machine, bmp);
	}

	@Override
	public void setIdToBoardMap(Map<Integer, Integer> idToBoard) {
		this.idToBoard = idToBoard;
	}

	private List<Integer> remap(List<Integer> boardIds) {
		List<Integer> boardNums = new ArrayList<>(boardIds.size());
		for (Integer id : boardIds) {
			boardNums.add(requireNonNull(idToBoard.get(id)));
		}
		return boardNums;
	}

	/**
	 * Check whether an FPGA has come up in a good state.
	 *
	 * @param board
	 *            Which board is the FPGA on?
	 * @param fpga
	 *            Which FPGA (0, 1, or 2) is being tested?
	 * @return True if the FPGA is in a correct state, false otherwise.
	 * @throws ProcessException
	 *             If a BMP rejects a message.
	 * @throws IOException
	 *             If network I/O fails.
	 */
	private boolean isGoodFPGA(Integer board, FpgaIdentifiers fpga)
			throws ProcessException, IOException {
		int flag = txrx.readFPGARegister(fpga.ordinal(), FLAG, ROOT_BMP, board);
		// FPGA ID is bottom two bits of FLAG register
		int fpgaId = flag & FPGA_FLAG_ID_MASK;
		boolean ok = fpgaId == fpga.ordinal();
		if (!ok) {
			log.warn("{} on board {} of {} has incorrect FPGA ID flag {}", fpga,
					board, machine.getName(), fpgaId);
		}
		return ok;
	}

	/**
	 * Is a board new enough to be able to manage FPGAs?
	 *
	 * @param txrx
	 *            Transceiver for talking to a BMP in a machine.
	 * @param board
	 *            The board number.
	 * @return True if the board can manage FPGAs.
	 * @throws ProcessException
	 *             If a BMP rejects a message.
	 * @throws IOException
	 *             If network I/O fails.
	 */
	private boolean canBoardManageFPGAs(Integer board)
			throws ProcessException, IOException {
		VersionInfo vi = txrx.readBMPVersion(ROOT_BMP, board);
		return vi.versionNumber.majorVersion >= BMP_VERSION_MIN;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Technically, switching a link off just switches off <em>sending</em> on
	 * that link. We assume that the other end of the link also behaves.
	 */
	@Override
	public void setLinkOff(Link link) throws ProcessException, IOException {
		int board = requireNonNull(idToBoard.get(link.getBoard()));
		Direction d = link.getLink();
		// skip FPGA link configuration if old BMP version
		if (!canBoardManageFPGAs(board)) {
			return;
		}
		txrx.writeFPGARegister(d.fpga.ordinal(), d.bank, STOP, 1, ROOT_BMP,
				board);
	}

	@Override
	public void powerOnAndCheck(List<Integer> boards)
			throws ProcessException, InterruptedException, IOException {
		List<Integer> boardsToPower = remap(boards);
		for (int attempt = 1; attempt <= props.getFpgaAttempts(); attempt++) {
			if (attempt > 1) {
				log.warn("rebooting {} boards in allocation to "
						+ "get stability", boardsToPower.size());
			}
			txrx.power(POWER_ON, ROOT_BMP, boardsToPower);

			/*
			 * Check whether all the FPGAs on each board have come up correctly.
			 * If not, we'll need to try booting that board again. The boards
			 * that have booted correctly need no further action.
			 */

			List<Integer> retryBoards = new ArrayList<>();
			for (Integer board : boardsToPower) {
				// Skip board if old BMP version
				if (!canBoardManageFPGAs(board)) {
					continue;
				}

				for (FpgaIdentifiers fpga : FpgaIdentifiers.values()) {
					if (!isGoodFPGA(board, fpga)) {
						retryBoards.add(board);
						/*
						 * Stop the INNERMOST loop; we know this board needs
						 * retrying so there's no point in continuing to look at
						 * the FPGAs it has.
						 */
						break;
					}
				}
			}
			if (retryBoards.isEmpty()) {
				// Success!
				return;
			}
			boardsToPower = retryBoards;
		}
		throw new IOException("Could not get correct FPGA ID for "
				+ boardsToPower.size() + " boards after "
				+ props.getFpgaAttempts() + " tries");
	}

	@Override
	public void powerOff(List<Integer> boards)
			throws ProcessException, InterruptedException, IOException {
		txrx.power(POWER_OFF, ROOT_BMP, remap(boards));
	}
}
