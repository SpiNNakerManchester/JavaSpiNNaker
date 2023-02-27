/*
 * Copyright (c) 2021 The University of Manchester
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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.messages.model.FPGALinkRegisters.STOP;
import static uk.ac.manchester.spinnaker.messages.model.FPGAMainRegisters.FLAG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties.TxrxProperties;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.Machine;
import uk.ac.manchester.spinnaker.alloc.bmp.FirmwareLoader.FirmwareLoaderException;
import uk.ac.manchester.spinnaker.alloc.model.Prototype;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.messages.model.FPGA;
import uk.ac.manchester.spinnaker.transceiver.BMPTransceiverInterface;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Implementation of controller for one BMP of one SpiNNaker 1 based system.
 * This is a short-lived component, which is why it isn't merged with the
 * longer-lived {@link TransceiverFactory}.
 */
@Component
@Prototype
class SpiNNaker1 implements SpiNNakerControl {
	// ----------------------------------------------------------------
	// CORE BMP ACCESS FUNCTIONS

	private static final Logger log = getLogger(SpiNNaker1.class);

	private static final int FPGA_FLAG_ID_MASK = 0x3;

	private static final int BMP_VERSION_MIN = 2;

	/**
	 * We <em>always</em> pretend to talk to the root BMP of a machine (actually
	 * the root of a frame), and never directly to any others. The BMPs within a
	 * frame use a CAN bus and I<sup>2</sup>C to communicate with each other on
	 * our behalf.
	 */
	private static final BMPCoords ROOT_BMP = new BMPCoords(0, 0);

	@Autowired
	private TxrxProperties props;

	@Autowired
	private TransceiverFactoryAPI<?> txrxFactory;

	/**
	 * Factory for {@link FirmwareLoader}. Do not call directly; use
	 * {@link #loadFirmware(List)} instead.
	 */
	@Autowired
	private ObjectProvider<FirmwareLoader> firmwareLoaderFactory;

	/** The BMP coordinates to bind into the transceiver. */
	private final BMPCoords bmp;

	private final Machine machine;

	/**
	 * The transceiver for talking to the machine.
	 */
	private BMPTransceiverInterface txrx;

	private Map<Integer, BMPBoard> idToBoard;

	/**
	 * Load the FPGA firmware onto a board.
	 *
	 * @param boards
	 *            Which boards are we planning to load the firmware on?
	 * @throws InterruptedException
	 *             If interrupted while sleeping
	 * @throws ProcessException
	 *             If a BMP rejects a message
	 * @throws IOException
	 *             If the network fails or the packaged bitfiles are unreadable
	 * @throws FirmwareLoaderException
	 *             If something goes wrong.
	 */
	@UsedInJavadocOnly(FirmwareLoaderException.class)
	private void loadFirmware(List<BMPBoard> boards)
			throws ProcessException, InterruptedException, IOException {
		int count = 0;
		for (var board : boards) {
			firmwareLoaderFactory.getObject(txrx, board)
					.bitLoad(++count == boards.size());
		}
	}

	/**
	 * The factory. Forces the constructor to conform to the API.
	 * <p>
	 * Do not use this directly (unless you're Spring Boot itself).
	 */
	static final Factory FACTORY = SpiNNaker1::new;

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
	void initTransceiver()
			throws IOException, SpinnmanException, InterruptedException {
		txrx = txrxFactory.getTransciever(machine, bmp);
		txrx.bind(ROOT_BMP);
	}

	@Override
	public void setIdToBoardMap(Map<Integer, BMPBoard> idToBoard) {
		this.idToBoard = idToBoard;
	}

	private List<BMPBoard> remap(List<Integer> boardIds) {
		return boardIds.stream().map(idToBoard::get).collect(toList());
	}

	/** Notes that a board probably needs its FPGA definitions reloading. */
	private static class FPGAReloadRequired extends Exception {
		private static final long serialVersionUID = 1L;

		final BMPBoard board;

		FPGAReloadRequired(BMPBoard board) {
			this.board = board;
		}
	}

	/**
	 * Check whether an FPGA has come up in a good state.
	 *
	 * @param board
	 *            Which board is the FPGA on?
	 * @param fpga
	 *            Which FPGA (0, 1, or 2) is being tested?
	 * @return True if the FPGA is in a correct state, false otherwise.
	 * @throws FPGAReloadRequired
	 *             If the FPGA is in such a bad state that the FPGA definitions
	 *             for the board need to be reloaded.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private boolean isGoodFPGA(BMPBoard board, FPGA fpga)
			throws FPGAReloadRequired, InterruptedException {
		int flag;
		try {
			flag = txrx.readFPGARegister(fpga, FLAG, board);
		} catch (ProcessException | IOException ignored) {
			// An exception means the FPGA is a problem
			return false;
		}
		// FPGA ID is bottom two bits of FLAG register
		int fpgaId = flag & FPGA_FLAG_ID_MASK;
		boolean ok = fpgaId == fpga.value;
		if (!ok) {
			log.warn("{} on board {} of {} has incorrect FPGA ID flag {}", fpga,
					board, machine.getName(), fpgaId);
			if (fpgaId == FPGA.FPGA_ALL.value) {
				throw new FPGAReloadRequired(board);
			}
		}
		return ok;
	}

	/**
	 * Is a board new enough to be able to manage FPGAs?
	 *
	 * @param board
	 *            The board number.
	 * @return True if the board can manage FPGAs.
	 * @throws ProcessException
	 *             If a BMP rejects a message.
	 * @throws IOException
	 *             If network I/O fails.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	private boolean canBoardManageFPGAs(BMPBoard board)
			throws ProcessException, IOException, InterruptedException {
		var vi = txrx.readBMPVersion(board);
		return vi.versionNumber.majorVersion >= BMP_VERSION_MIN;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Technically, switching a link off just switches off <em>sending</em> on
	 * that link. We assume that the other end of the link also behaves.
	 */
	@Override
	public void setLinkOff(Link link)
			throws ProcessException, IOException, InterruptedException {
		var board = requireNonNull(idToBoard.get(link.getBoard()));
		var d = link.getLink();
		// skip FPGA link configuration if old BMP version
		if (!canBoardManageFPGAs(board)) {
			return;
		}
		txrx.writeFPGARegister(d.fpga, d.bank, STOP, 1, board);
	}

	/**
	 * A board is good if all its FPGAs are good.
	 *
	 * @param board
	 *            The board ID
	 * @return Whether the board's FPGAs all came up correctly.
	 * @throws FPGAReloadRequired
	 *             If an FPGA is in such a bad state that the FPGA definitions
	 *             for the board need to be reloaded.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 * @see #isGoodFPGA(Integer, FPGA)
	 */
	private boolean hasGoodFPGAs(BMPBoard board)
			throws FPGAReloadRequired, InterruptedException {
		for (var fpga : FPGA.values()) {
			if (fpga.isSingleFPGA() && !isGoodFPGA(board, fpga)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void powerOnAndCheck(List<Integer> boards)
			throws ProcessException, InterruptedException, IOException {
		log.debug("Power on and check boards {} for BMP {}", boards, bmp);
		var boardsToPower = remap(boards);
		boolean reloadDone = false; // so we only do firmware loading once
		for (int attempt = 1; attempt <= props.getFpgaAttempts(); attempt++) {
			if (attempt > 1) {
				log.warn("rebooting {} boards in allocation to "
						+ "get stability", boardsToPower.size());
			}
			txrx.powerOn(boardsToPower);

			/*
			 * Check whether all the FPGAs on each board have come up correctly.
			 * If not, we'll need to try booting that board again. The boards
			 * that have booted correctly need no further action.
			 */

			var retryBoards = new ArrayList<BMPBoard>();
			var reloadBoards = new ArrayList<BMPBoard>();
			for (var board : boardsToPower) {
				// Skip board if old BMP version
				if (!canBoardManageFPGAs(board)) {
					continue;
				}
				try {
					if (!hasGoodFPGAs(board)) {
						retryBoards.add(board);
					}
				} catch (FPGAReloadRequired e) {
					reloadBoards.add(e.board);
				}
			}
			if (retryBoards.isEmpty() && reloadBoards.isEmpty()) {
				// Success!
				log.debug("Finished power on and check boards {} for {}",
						boards, bmp);
				return;
			}
			// We don't try reloading the first time
			if (props.isFpgaReload() && attempt > 1
					&& attempt < props.getFpgaAttempts()
					&& !reloadBoards.isEmpty() && !reloadDone) {
				log.warn("reloading FPGA firmware on {} boards",
						retryBoards.size());
				loadFirmware(reloadBoards);
				reloadDone = true;
				// Need a full retry after that!
				boardsToPower = remap(boards);
				continue;
			}
			retryBoards.addAll(reloadBoards); // Might not be empty
			boardsToPower = retryBoards;
		}
		throw new IOException("Could not get correct FPGA ID for "
				+ boardsToPower.size() + " boards after "
				+ props.getFpgaAttempts() + " tries");
	}

	@Override
	public void powerOff(List<Integer> boards)
			throws ProcessException, InterruptedException, IOException {
		txrx.powerOff(remap(boards));
	}

	@Override
	public String readSerial(BMPBoard board)
			throws ProcessException, IOException, InterruptedException {
		return txrx.readBoardSerialNumber(board);
	}

	@Override
	public Blacklist readBlacklist(BMPBoard board)
			throws ProcessException, IOException, InterruptedException {
		return txrx.readBlacklist(board);
	}

	@Override
	public void writeBlacklist(BMPBoard board, Blacklist blacklist)
			throws ProcessException, InterruptedException, IOException {
		txrx.writeBlacklist(board, blacklist);
	}

	@Override
	public ADCInfo readTemp(BMPBoard board)
			throws ProcessException, IOException, InterruptedException {
		return txrx.readADCData(board);
	}
}
