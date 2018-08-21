package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.System.arraycopy;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.DUMP_FR;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.DUMP_MC;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.DUMP_NN;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.DUMP_PP;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.EXT_FR;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.EXT_MC;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.EXT_NN;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.EXT_PP;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.LOC_FR;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.LOC_MC;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.LOC_NN;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.LOC_PP;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.USER_0;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.USER_1;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.USER_2;
import static uk.ac.manchester.spinnaker.messages.model.RouterDiagnostics.RouterRegister.USER_3;

/**
 * Represents a set of diagnostic information available from a chip router.
 */
public class RouterDiagnostics {
	/** The "mon" part of the control register. */
	public final int mon;
	/** The "wait_1" part of the control register. */
	public final int wait1;
	/** The "wait_2" part of the control register. */
	public final int wait2;
	/** The error status. */
	public final int errorStatus;
	/**
	 * The values in all of the registers. Can be used to directly access the
	 * registers if they have been programmed to give different values.
	 */
	public final int[] registerValues;

	private static final int NUM_REGISTERS = 16;
	private static final int NUM_USER_CONTROL_REGISTERS = 4;
	private static final int MON_MASK = 0x1F;
	private static final int BYTE_MASK = 0xFF;
	private static final int MON_SHIFT = 8;
	private static final int WAIT1_SHIFT = 16;
	private static final int WAIT2_SHIFT = 24;

	/**
	 * Parse the router diagnostics.
	 *
	 * @param controlRegister
	 *            The control register value.
	 * @param errorStatus
	 *            The error status value.
	 * @param registerValues
	 *            The register values. (Should have {@value #NUM_REGISTERS}
	 *            elements.)
	 */
	public RouterDiagnostics(int controlRegister, int errorStatus,
			int[] registerValues) {
		if (registerValues.length != NUM_REGISTERS) {
			throw new IllegalArgumentException(
					"must be exactly 16 router register values");
		}
		this.mon = (controlRegister >> MON_SHIFT) & MON_MASK;
		this.wait1 = (controlRegister >> WAIT1_SHIFT) & BYTE_MASK;
		this.wait2 = (controlRegister >> WAIT2_SHIFT) & BYTE_MASK;
		this.errorStatus = errorStatus;
		this.registerValues = registerValues;
	}

	private int register(RouterRegister r) {
		return registerValues[r.ordinal()];
	}

	/** @return The number of multicast packets received from local cores. */
	public int getNumLocalMulticastPackets() {
		return register(LOC_MC);
	}

	/** @return The number of multicast packets received from external links. */
	public int getNumExternalMulticastPackets() {
		return register(EXT_MC);
	}

	/** @return The number of multicast packets received that were dropped. */
	public int getNumDroppedMulticastPackets() {
		return register(DUMP_MC);
	}

	/** @return The number of peer-to-peer packets received from local cores. */
	public int getNumLocalPeerToPeerPackets() {
		return register(LOC_PP);
	}

	/**
	 * @return The number of peer-to-peer packets received from external links.
	 */
	public int getNumExternalPeerToPeerPackets() {
		return register(EXT_PP);
	}

	/**
	 * @return The number of peer-to-peer packets received that were dropped.
	 */
	public int getNumDroppedPeerToPeerPackets() {
		return register(DUMP_PP);
	}

	/**
	 * @return The number of nearest-neighbour packets received from local
	 *         cores.
	 */
	public int getNumLocalNearestNeighbourPackets() {
		return register(LOC_NN);
	}

	/**
	 * @return The number of nearest-neighbour packets received from external
	 *         links.
	 */
	public int getNumExternalNearestNeighbourPackets() {
		return register(EXT_NN);
	}

	/**
	 * @return The number of nearest-neighbour packets received that were
	 *         dropped.
	 */
	public int getNumDroppedNearestNeighbourPackets() {
		return register(DUMP_NN);
	}

	/** @return The number of fixed-route packets received from local cores. */
	public int getNumLocalFixedRoutePackets() {
		return register(LOC_FR);
	}

	/**
	 * @return The number of fixed-route packets received from external links.
	 */
	public int getNumExternalFixedRoutePackets() {
		return register(EXT_FR);
	}

	/** @return The number of fixed-route packets received that were dropped. */
	public int getNumDroppedFixedRoutePackets() {
		return register(DUMP_FR);
	}

	/** @return The data gained from the user 0 router diagnostic filter. */
	public int getUser0() {
		return register(USER_0);
	}

	/** @return The data gained from the user 1 router diagnostic filter. */
	public int getUser1() {
		return register(USER_1);
	}

	/** @return The data gained from the user 2 router diagnostic filter. */
	public int getUser2() {
		return register(USER_2);
	}

	/** @return The data gained from the user 3 router diagnostic filter. */
	public int getUser3() {
		return register(USER_3);
	}

	/**
	 * The values in the user control registers.
	 *
	 * @return An array of 4 values
	 */
	public int[] getUserRegisters() {
		int[] ur = new int[NUM_USER_CONTROL_REGISTERS];
		arraycopy(registerValues, USER_0.ordinal(), ur, 0, ur.length);
		return ur;
	}

	/**
	 * Description of router registers.
	 */
	public enum RouterRegister {
		/** Local Multicast Counter. */
		LOC_MC,
		/** External Multicast Counter. */
		EXT_MC,
		/** Local Peer-to-Peer Counter. */
		LOC_PP,
		/** External Peer-to-Peer Counter. */
		EXT_PP,
		/** Local Nearest Neighbour Counter. */
		LOC_NN,
		/** External Nearest Neighbour Counter. */
		EXT_NN,
		/** Local Fixed Route Counter. */
		LOC_FR,
		/** External Fixed Route Counter. */
		EXT_FR,
		/** Dumped Multicast Counter. */
		DUMP_MC,
		/** Dumped Peer-to-Peer Counter. */
		DUMP_PP,
		/** Dumped Nearest Neighbour Counter. */
		DUMP_NN,
		/** Dumped Fixed Route Counter. */
		DUMP_FR,
		/** Diagnostic Filter 0 Counter. */
		USER_0,
		/** Diagnostic Filter 1 Counter. */
		USER_1,
		/** Diagnostic Filter 2 Counter. */
		USER_2,
		/** Diagnostic Filter 3 Counter. */
		USER_3
	}
}
