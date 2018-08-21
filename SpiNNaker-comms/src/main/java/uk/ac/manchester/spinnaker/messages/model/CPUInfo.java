package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.messages.model.CPUState.RUN_TIME_EXCEPTION;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.ARMRegisters.r0;
import static uk.ac.manchester.spinnaker.messages.model.ARMRegisters.r1;
import static uk.ac.manchester.spinnaker.messages.model.ARMRegisters.r2;
import static uk.ac.manchester.spinnaker.messages.model.ARMRegisters.r3;
import static uk.ac.manchester.spinnaker.messages.model.ARMRegisters.r4;
import static uk.ac.manchester.spinnaker.messages.model.ARMRegisters.r5;
import static uk.ac.manchester.spinnaker.messages.model.ARMRegisters.r6;
import static uk.ac.manchester.spinnaker.messages.model.ARMRegisters.r7;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/** Represents information about the state of a CPU. */
public class CPUInfo implements HasCoreLocation {
	private final CoreLocation core;

	private final int[] registers;
	private final int processorStateRegister;
	private final int stackPointer;
	private final int linkRegister;
	private final RunTimeError runTimeError;
	private final byte physicalCPUID;
	private final CPUState state;
	private final int applicationID;
	private final int applicationMailboxDataAddress;
	private final int monitorMailboxDataAddress;
	private final MailboxCommand applicationMailboxCommand;
	private final MailboxCommand monitorMailboxCommand;
	private final short softwareErrorCount;
	private final int softwareSourceFilenameAddress;
	private final int softwareSourceLineNumber;
	private final int time;
	private final String applicationName;
	private final int iobufAddress;
	private final int softwareVersion;
	private final int[] user;

	private static final Charset ASCII = Charset.forName("ascii");
	private static final int NUM_REGISTERS = 8;
	private static final int APP_NAME_WIDTH = 16;
	private static final int SKIP_BYTES = 16;
	private static final int NUM_USER_VALUES = 4;

	private static int[] getInts(ByteBuffer buffer, int fieldLength) {
		int[] data = new int[fieldLength];
		buffer.asIntBuffer().get(data);
		buffer.position(buffer.position() + fieldLength * WORD_SIZE);
		return data;
	}

	private static String getStr(ByteBuffer buffer, int fieldLength) {
		byte[] data = new byte[fieldLength];
		buffer.get(data);
		int len = 0;
		// Trim from first trailing NUL
		for (byte b : data) {
			if (b == 0) {
				break;
			}
			len++;
		}
		return new String(data, 0, len, ASCII);
	}

	/**
	 * @param location
	 *            Which core was queried for the information.
	 * @param buffer
	 *            The data received from SDRAM on the board, in a little-endian
	 *            buffer.
	 */
	public CPUInfo(HasCoreLocation location, ByteBuffer buffer) {
		core = location.asCoreLocation();
		registers = getInts(buffer, NUM_REGISTERS);
		processorStateRegister = buffer.getInt();
		stackPointer = buffer.getInt();
		linkRegister = buffer.getInt();
		runTimeError = RunTimeError.get(toUnsignedInt(buffer.get()));
		physicalCPUID = buffer.get();
		state = CPUState.get(buffer.get());
		applicationID = toUnsignedInt(buffer.get());
		applicationMailboxDataAddress = buffer.getInt();
		monitorMailboxDataAddress = buffer.getInt();
		applicationMailboxCommand = MailboxCommand.get(buffer.get());
		monitorMailboxCommand = MailboxCommand.get(buffer.get());
		softwareErrorCount = buffer.getShort();
		softwareSourceFilenameAddress = buffer.getInt();
		softwareSourceLineNumber = buffer.getInt();
		time = buffer.getInt();
		applicationName = getStr(buffer, APP_NAME_WIDTH);
		iobufAddress = buffer.getInt();
		softwareVersion = buffer.getInt();
		buffer.position(buffer.position() + SKIP_BYTES);
		user = getInts(buffer, NUM_USER_VALUES);
	}

	@Override
	public int getX() {
		return core.getX();
	}

	@Override
	public int getY() {
		return core.getY();
	}

	@Override
	public int getP() {
		return core.getP();
	}

	/** @return The current register values (r0&hellip;r7). */
	public int[] getRegisters() {
		return registers;
	}

	/** @return The value in the processor state register (PSR). */
	public int getProcessorStateRegister() {
		return processorStateRegister;
	}

	/** @return The current stack pointer value (SP). */
	public int getStackPointer() {
		return stackPointer;
	}

	/** @return The current link register value (LR). */
	public int getLinkRegister() {
		return linkRegister;
	}

	/** @return The reason for a run time error. */
	public RunTimeError getRunTimeError() {
		return runTimeError;
	}

	/** @return The physical ID of this processor. */
	public byte getPhysicalCPUID() {
		return physicalCPUID;
	}

	/** @return The current state of the core. */
	public CPUState getState() {
		return state;
	}

	/** @return The ID of the application running on the core. */
	public int getApplicationID() {
		return applicationID;
	}

	/** @return The address of the data in SDRAM for the application mailbox. */
	public int getApplicationMailboxDataAddress() {
		return applicationMailboxDataAddress;
	}

	/** @return The address of the data in SDRAM of the monitor mailbox. */
	public int getMonitorMailboxDataAddress() {
		return monitorMailboxDataAddress;
	}

	/**
	 * @return The command currently in the mailbox being sent from the monitor
	 *         processor to the application.
	 */
	public MailboxCommand getApplicationMailboxCommand() {
		return applicationMailboxCommand;
	}

	/**
	 * @return The command currently in the mailbox being sent from the
	 *         application to the monitor processor.
	 */
	public MailboxCommand getMonitorMailboxCommand() {
		return monitorMailboxCommand;
	}

	/** @return The number of software errors counted. */
	public short getSoftwareErrorCount() {
		return softwareErrorCount;
	}

	/** @return The address of the filename of the software source. */
	public int getSoftwareSourceFilenameAddress() {
		return softwareSourceFilenameAddress;
	}

	/** @return The line number of the software source. */
	public int getSoftwareSourceLineNumber() {
		return softwareSourceLineNumber;
	}

	/**
	 * @return The time at which the application started, in seconds since
	 *         00:00:00 GMT on 1 January 1970.
	 */
	public int getTime() {
		return time;
	}

	/** @return The name of the application running on the core. */
	public String getApplicationName() {
		return applicationName;
	}

	/** @return The address of the IOBUF buffer in SDRAM. */
	public int getIobufAddress() {
		return iobufAddress;
	}

	/** @return The software version. */
	public int getSoftwareVersion() {
		return softwareVersion;
	}

	/**
	 * @return The current user values
	 *         (user<sub>0</sub>&hellip;user<sub>3</sub>).
	 */
	public int[] getUser() {
		return user;
	}

	/**
	 * @return A description of the state.
	 */
	public String getStatusDescription() {
		if (state != RUN_TIME_EXCEPTION) {
			return format("    %d:%d:%d in state %s", core.getX(), core.getY(),
					core.getP(), state);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(format("    %d:%d:%d in state %s:%s\n", core.getX(),
				core.getY(), core.getP(), state, runTimeError));
		sb.append(format("        r0=%08x, r1=%08x, r2=%08x, r3=%08x\n",
				r0.get(registers), r1.get(registers), r2.get(registers),
				r3.get(registers)));
		sb.append(format("        r4=%08x, r5=%08x, r6=%08x, r7=%08x\n",
				r4.get(registers), r5.get(registers), r6.get(registers),
				r7.get(registers)));
		sb.append(format("        PSR=%08x, SP=%08x, LR=%08x",
				processorStateRegister, stackPointer, linkRegister));
		return sb.toString();
	}
}
