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
	/** The "mon" part of the control register */
	public final int mon;
	/** The "wait_1" part of the control register */
	public final int wait_1;
	/** The "wait_2" part of the control register */
	public final int wait_2;
	/** The error status */
	public final int errorStatus;
	/**
	 * The values in all of the registers. Can be used to directly access the
	 * registers if they have been programmed to give different values
	 */
	public final int[] registerValues;

	public RouterDiagnostics(int controlRegister, int errorStatus,
			int[] registerValues) {
		if (registerValues.length != 16) {
			throw new IllegalArgumentException(
					"must be exactly 16 router register values");
		}
		this.mon = (controlRegister >> 8) & 0x1F;// FIXME overlap with wait_2?
		this.wait_1 = (controlRegister >> 16) & 0xFF;
		this.wait_2 = (controlRegister >> 8) & 0xFF;// FIXME overlap with mon?
		this.errorStatus = errorStatus;
		this.registerValues = registerValues;
	}

	private int register(RouterRegister r) {
		return registerValues[r.ordinal()];
	}

	/** The number of multicast packets received from local cores. */
	public int getNumLocalMulticastPackets() {
		return register(LOC_MC);
	}

	/** The number of multicast packets received from external links. */
	public int getNumExternalMulticastPackets() {
		return register(EXT_MC);
	}

	/** The number of multicast packets received that were dropped. */
	public int getNumDroppedMulticastPackets() {
		return register(DUMP_MC);
	}

	/** The number of peer-to-peer packets received from local cores. */
	public int getNumLocalPeerToPeerPackets() {
		return register(LOC_PP);
	}

	/** The number of peer-to-peer packets received from external links. */
	public int getNumExternalPeerToPeerPackets() {
		return register(EXT_PP);
	}

	/** The number of peer-to-peer packets received that were dropped. */
	public int getNumDroppedPeerToPeerPackets() {
		return register(DUMP_PP);
	}

	/** The number of nearest-neighbour packets received from local cores. */
	public int getNumLocalNearestNeighbourPackets() {
		return register(LOC_NN);
	}

	/** The number of nearest-neighbour packets received from external links. */
	public int getNumExternalNearestNeighbourPackets() {
		return register(EXT_NN);
	}

	/** The number of nearest-neighbour packets received that were dropped. */
	public int getNumDroppedNearestNeighbourPackets() {
		return register(DUMP_NN);
	}

	/** The number of fixed-route packets received from local cores. */
	public int getNumLocalFixedRoutePackets() {
		return register(LOC_FR);
	}

	/** The number of fixed-route packets received from external links. */
	public int getNum_external_fixed_route_packets() {
		return register(EXT_FR);
	}

	/** The number of fixed-route packets received that were dropped. */
	public int getNum_dropped_fixed_route_packets() {
		return register(DUMP_FR);
	}

	/** The data gained from the user 0 router diagnostic filter. */
	public int getUser0() {
		return register(USER_0);
	}

	/** The data gained from the user 1 router diagnostic filter. */
	public int getUser1() {
		return register(USER_1);
	}

	/** The data gained from the user 2 router diagnostic filter. */
	public int getUser2() {
		return register(USER_2);
	}

	/** The data gained from the user 3 router diagnostic filter. */
	public int getUser3() {
		return register(USER_3);
	}

	/**
	 * The values in the user control registers.
	 *
	 * @return An array of 4 values
	 */
	public int[] getUserRegisters() {
		int[] ur = new int[4];
		arraycopy(registerValues, USER_0.ordinal(), ur, 0, ur.length);
		return ur;
	}

	public enum RouterRegister {
		/** Local Multicast Counter */
		LOC_MC,
		/** External Multicast Counter */
		EXT_MC,
		/** Local Peer-to-Peer Counter */
		LOC_PP,
		/** External Peer-to-Peer Counter */
		EXT_PP,
		/** Local Nearest Neighbour Counter */
		LOC_NN,
		/** External Nearest Neighbour Counter */
		EXT_NN,
		/** Local Fixed Route Counter */
		LOC_FR,
		/** External Fixed Route Counter */
		EXT_FR,
		/** Dumped Multicast Counter */
		DUMP_MC,
		/** Dumped Peer-to-Peer Counter */
		DUMP_PP,
		/** Dumped Nearest Neighbour Counter */
		DUMP_NN,
		/** Dumped Fixed Route Counter */
		DUMP_FR,
		/** Diagnostic Filter 0 Counter */
		USER_0,
		/** Diagnostic Filter 1 Counter */
		USER_1,
		/** Diagnostic Filter 2 Counter */
		USER_2,
		/** Diagnostic Filter 3 Counter */
		USER_3
	}
}
