package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Byte.toUnsignedInt;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import uk.ac.manchester.spinnaker.machine.CPUState;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/** Represents information about the state of a CPU. */
public class CPUInfo implements HasCoreLocation {
	private final int x;
	private final int y;
	private final int p;

	private final int[] registers;
	private final int processor_state_register;
	private final int stack_pointer;
	private final int link_register;
	private final RunTimeError run_time_error;
	private final byte physical_cpu_id;
	private final CPUState state;
	private final byte application_id;
	private final int application_mailbox_data_address;
	private final int monitor_mailbox_data_address;
	private final MailboxCommand application_mailbox_command;
	private final MailboxCommand monitor_mailbox_command;
	private final short software_error_count;
	private final int software_source_filename_address;
	private final int software_source_line_number;
	private final int time;
	private final String application_name;
	private final int iobuf_address;
	private final int software_version;
	private final int[] user;

	private static final Charset ASCII = Charset.forName("ascii");
	private static final int NUM_REGISTERS = 8;
	private static final int APP_NAME_WIDTH = 16;
	private static final int SKIP_BYTES = 16;
	private static final int NUM_USER_VALUES = 4;

	private static int[] getInts(ByteBuffer buffer, int fieldLength) {
		int[] data = new int[fieldLength];
		buffer.asIntBuffer().get(data);
		buffer.position(buffer.position() + fieldLength * 4);
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
		// TODO should we hold the original location object instead?
		x = location.getX();
		y = location.getY();
		p = location.getP();
		registers = getInts(buffer, NUM_REGISTERS);
		processor_state_register = buffer.getInt();
		stack_pointer = buffer.getInt();
		link_register = buffer.getInt();
		run_time_error = RunTimeError.get(buffer.get());
		physical_cpu_id = buffer.get();
		state = CPUState.get(buffer.get());
		application_id = buffer.get();
		application_mailbox_data_address = buffer.getInt();
		monitor_mailbox_data_address = buffer.getInt();
		application_mailbox_command = MailboxCommand.get(buffer.get());
		monitor_mailbox_command = MailboxCommand.get(buffer.get());
		software_error_count = buffer.getShort();
		software_source_filename_address = buffer.getInt();
		software_source_line_number = buffer.getInt();
		time = buffer.getInt();
		application_name = getStr(buffer, APP_NAME_WIDTH);
		iobuf_address = buffer.getInt();
		software_version = buffer.getInt();
		buffer.position(buffer.position() + SKIP_BYTES);
		user = getInts(buffer, NUM_USER_VALUES);
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public int getP() {
		return p;
	}

	/** The current register values (r0&hellip;r7). */
	public int[] getRegisters() {
		return registers;
	}

	/** The value in the processor state register (PSR). */
	public int getProcessorStateRegister() {
		return processor_state_register;
	}

	/** The current stack pointer value (SP). */
	public int getStackPointer() {
		return stack_pointer;
	}

	/** The current link register value (LR). */
	public int getLinkRegister() {
		return link_register;
	}

	/** The reason for a run time error. */
	public RunTimeError getRunTimeError() {
		return run_time_error;
	}

	/** The physical ID of this processor. */
	public byte getPhysicalCPUID() {
		return physical_cpu_id;
	}

	/** The current state of the core. */
	public CPUState getState() {
		return state;
	}

	/** The ID of the application running on the core. */
	public int getApplicationID() {
		return toUnsignedInt(application_id);
	}

	/** The address of the data in SDRAM for the application mailbox. */
	public int getApplicationMailboxDataAddress() {
		return application_mailbox_data_address;
	}

	/** The address of the data in SDRAM of the monitor mailbox. */
	public int getMonitorMailboxDataAddress() {
		return monitor_mailbox_data_address;
	}

	/**
	 * The command currently in the mailbox being sent from the monitor
	 * processor to the application.
	 */
	public MailboxCommand getApplicationMailboxCommand() {
		return application_mailbox_command;
	}

	/**
	 * The command currently in the mailbox being sent from the application to
	 * the monitor processor.
	 */
	public MailboxCommand getMonitorMailboxCommand() {
		return monitor_mailbox_command;
	}

	/** The number of software errors counted. */
	public short getSoftwareErrorCount() {
		return software_error_count;
	}

	/** The address of the filename of the software source. */
	public int getSoftwareSourceFilenameAddress() {
		return software_source_filename_address;
	}

	/** The line number of the software source. */
	public int getSoftwareSourceLineNumber() {
		return software_source_line_number;
	}

	/**
	 * The time at which the application started, in seconds since 00:00:00 GMT
	 * on 1 January 1970.
	 */
	public int getTime() {
		return time;
	}

	/** The name of the application running on the core. */
	public String getApplicationName() {
		return application_name;
	}

	/** The address of the IOBUF buffer in SDRAM. */
	public int getIobufAddress() {
		return iobuf_address;
	}

	/** The software version. */
	public int getSoftwareVersion() {
		return software_version;
	}

	/** The current user values (user<sub>0</sub>&hellip;user<sub>3</sub>). */
	public int[] getUser() {
		return user;
	}
}
