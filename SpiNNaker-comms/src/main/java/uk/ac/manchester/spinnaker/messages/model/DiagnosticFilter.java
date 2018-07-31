package uk.ac.manchester.spinnaker.messages.model;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A router diagnostic counter filter, which counts packets passing through the
 * router with certain properties. The counter will be incremented so long as
 * the packet matches one of the values in <i>each</i> field, i.e., one of each
 * of the destinations, sources, payload statuses, default routing statuses,
 * emergency routing statuses, and packet types.
 */
public class DiagnosticFilter {
	private static final int PACKET_TYPE_OFFSET = 0;
	private static final int EMERGENCY_ROUTE_OFFSET = 4;
	private static final int EMERGENCY_ROUTE_MODE_OFFSET = 8;
	private static final int DEFAULT_ROUTE_OFFSET = 10;
	private static final int PAYLOAD_OFFSET = 12;
	private static final int SOURCE_OFFSET = 14;
	private static final int DESTINATION_OFFSET = 16;
	private static final int ENABLE_INTERRUPT_OFFSET = 30;

	private final boolean enableInterrupt;
	private final boolean emergencyMode;
	private final Collection<Destination> destinations;
	private final Collection<Source> sources;
	private final Collection<PayloadStatus> payloads;
	private final Collection<DefaultRoutingStatus> defaultStatuses;
	private final Collection<EmergencyRoutingStatus> emergencyStatuses;
	private final Collection<PacketType> packetTypes;

	/**
	 * @param enableInterruptOnCounterEvent
	 *            Indicates whether an interrupt should be raised when this rule
	 *            matches
	 * @param matchmergencyRoutingStatusToIncomingPacket
	 *            Indicates whether the emergency routing statuses should be
	 *            matched against packets arriving at this router (if True), or
	 *            if they should be matched against packets leaving this router
	 *            (if False)
	 * @param destinations
	 *            Increment the counter if one or more of the given destinations
	 *            match
	 * @param sources
	 *            Increment the counter if one or more of the given sources
	 *            match (or empty list to match all)
	 * @param payloadStatuses
	 *            Increment the counter if one or more of the given payload
	 *            statuses match (or empty list to match all)
	 * @param defaultRoutingStatuses
	 *            Increment the counter if one or more of the given default
	 *            routing statuses match (or empty list to match all)
	 * @param emergencyRoutingStatuses
	 *            Increment the counter if one or more of the given emergency
	 *            routing statuses match (or empty list to match all)
	 * @param packetTypes
	 *            Increment the counter if one or more of the given packet types
	 *            match (or empty list to match all)
	 */
	public DiagnosticFilter(boolean enableInterruptOnCounterEvent,
			boolean matchmergencyRoutingStatusToIncomingPacket,
			Collection<Destination> destinations, Collection<Source> sources,
			Collection<PayloadStatus> payloadStatuses,
			Collection<DefaultRoutingStatus> defaultRoutingStatuses,
			Collection<EmergencyRoutingStatus> emergencyRoutingStatuses,
			Collection<PacketType> packetTypes) {
		this.enableInterrupt = enableInterruptOnCounterEvent;
		this.emergencyMode = matchmergencyRoutingStatusToIncomingPacket;
		this.destinations = convert(destinations, Destination.values());
		this.sources = convert(sources, Source.values());
		this.payloads = convert(payloadStatuses, PayloadStatus.values());
		this.defaultStatuses = convert(defaultRoutingStatuses,
				DefaultRoutingStatus.values());
		this.emergencyStatuses = convert(emergencyRoutingStatuses,
				EmergencyRoutingStatus.values());
		this.packetTypes = convert(packetTypes, PacketType.values());
	}

	private static <T> Collection<T> convert(Collection<T> collection,
			T[] defaults) {
		if (collection == null || collection.isEmpty()) {
			return unmodifiableList(Arrays.asList(defaults));
		}
		return unmodifiableCollection(collection);
	}

	/**
	 * @param encodedValue
	 *            The word of data that would be written to the router to set up
	 *            the filter.
	 */
	public DiagnosticFilter(int encodedValue) {
		enableInterrupt = bitSet(encodedValue, ENABLE_INTERRUPT_OFFSET);
		emergencyMode = bitSet(encodedValue, EMERGENCY_ROUTE_MODE_OFFSET);
		destinations = unmodifiableCollection(Stream.of(Destination.values())
				.filter(d -> bitSet(encodedValue, DESTINATION_OFFSET + d.value))
				.collect(Collectors.toList()));
		sources = unmodifiableCollection(Stream.of(Source.values())
				.filter(s -> bitSet(encodedValue, SOURCE_OFFSET + s.value))
				.collect(Collectors.toList()));
		payloads = unmodifiableCollection(Stream.of(PayloadStatus.values())
				.filter(p -> bitSet(encodedValue, PAYLOAD_OFFSET + p.value))
				.collect(Collectors.toList()));
		defaultStatuses = unmodifiableCollection(
				Stream.of(DefaultRoutingStatus.values())
						.filter(dr -> bitSet(encodedValue,
								DEFAULT_ROUTE_OFFSET + dr.value))
						.collect(Collectors.toList()));
		emergencyStatuses = unmodifiableCollection(
				Stream.of(EmergencyRoutingStatus.values())
						.filter(er -> bitSet(encodedValue,
								EMERGENCY_ROUTE_OFFSET + er.value))
						.collect(Collectors.toList()));
		packetTypes = unmodifiableCollection(Stream.of(PacketType.values())
				.filter(pt -> bitSet(encodedValue,
						PACKET_TYPE_OFFSET + pt.value))
				.collect(Collectors.toList()));
	}

	private static boolean bitSet(int value, int bitIndex) {
		return ((value >> bitIndex) & 0x1) == 1;
	}

	/**
	 * Whether an interrupt should be raised when this rule matches.
	 */
	public boolean getEnableInterruptOnCounterEvent() {
		return enableInterrupt;
	}

	/**
	 * Whether the emergency routing statuses should be matched against packets
	 * arriving at this router (if True), or if they should be matched against
	 * packets leaving this router (if False).
	 */
	public boolean getMatchEmergencyRoutingStatusToIncomingPacket() {
		return emergencyMode;
	}

	/**
	 * @return The set of destinations to match.
	 */
	public Collection<Destination> getDestinations() {
		return destinations;
	}

	/**
	 * @return The set of sources to match.
	 */
	public Collection<Source> getSources() {
		return sources;
	}

	/**
	 * @return The set of payload statuses to match.
	 */
	public Collection<PayloadStatus> getPayloadStatuses() {
		return payloads;
	}

	/**
	 * @return The set of default routing statuses to match.
	 */
	public Collection<DefaultRoutingStatus> getDefaultRoutingStatuses() {
		return defaultStatuses;
	}

	/**
	 * @return The set of emergency routing statuses to match.
	 */
	public Collection<EmergencyRoutingStatus> getEmergencyRoutingStatuses() {
		return emergencyStatuses;
	}

	/**
	 * @return The set of packet types to match.
	 */
	public Collection<PacketType> getPacketTypes() {
		return packetTypes;
	}

	/**
	 * @return A word of data that can be written to the router to set up the
	 *         filter.
	 */
	public int getFilterWord() {
		int data = (enableInterrupt ? 1 << ENABLE_INTERRUPT_OFFSET : 0);
		if (!emergencyMode) {
			data |= 1 << EMERGENCY_ROUTE_MODE_OFFSET;
		}
		for (Destination val : destinations) {
			data |= val.value + DESTINATION_OFFSET;
		}
		for (Source val : sources) {
			data |= val.value + SOURCE_OFFSET;
		}
		for (PayloadStatus val : payloads) {
			data |= val.value + PAYLOAD_OFFSET;
		}
		for (DefaultRoutingStatus val : defaultStatuses) {
			data |= val.value + DEFAULT_ROUTE_OFFSET;
		}
		for (EmergencyRoutingStatus val : emergencyStatuses) {
			data |= val.value + EMERGENCY_ROUTE_OFFSET;
		}
		for (PacketType val : packetTypes) {
			data |= val.value + PACKET_TYPE_OFFSET;
		}
		return data;
	}

	/**
	 * Default routing flags for the diagnostic filters. Note that only one has
	 * to match for the counter to be incremented.
	 */
	public enum DefaultRoutingStatus {
		/** Packet is to be default routed. */
		DEFAULT_ROUTED(0),
		/** Packet is not to be default routed. */
		NON_DEFAULT_ROUTED(1);

		/** The encoded value. */
		public final int value;
		DefaultRoutingStatus(int value) {
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static DefaultRoutingStatus get(int value) {
			switch (value) {
			case 0:
				return DEFAULT_ROUTED;
			case 1:
				return NON_DEFAULT_ROUTED;
			default:
				return null;
			}
		}
	}

	/**
	 * Destination flags for the diagnostic filters. Note that only one has to
	 * match for the counter to be incremented.
	 */
	public enum Destination {
		/** Destination is to dump the packet. */
		DUMP(0),
		/** Destination is a local core (but not the monitor core). */
		LOCAL(1),
		/** Destination is the local monitor core. */
		LOCAL_MONITOR(2),
		/** Destination is link 0. */
		LINK_0(3),
		/** Destination is link 1. */
		LINK_1(4),
		/** Destination is link 2. */
		LINK_2(5),
		/** Destination is link 3. */
		LINK_3(6),
		/** Destination is link 4. */
		LINK_4(7),
		/** Destination is link 5. */
		LINK_5(8);

		/** The encoded value. */
		public final int value;
		Destination(int value) {
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static Destination get(int value) {
			switch (value) {
			case 0:
				return DUMP;
			case 1:
				return LOCAL;
			case 2:
				return LOCAL_MONITOR;
			case 3:
				return LINK_0;
			case 4:
				return LINK_1;
			case 5:
				return LINK_2;
			case 6:
				return LINK_3;
			case 7:
				return LINK_4;
			case 8:
				return LINK_5;
			default:
				return null;
			}
		}
	}

	/**
	 * Emergency routing status flags for the diagnostic filters. Note that only
	 * one has to match for the counter to be incremented.
	 */
	public enum EmergencyRoutingStatus {
		/** Packet is not emergency routed. */
		NORMAL(0),
		/**
		 * Packet is in first hop of emergency route; packet should also have
		 * been sent here by normal routing.
		 */
		FIRST_STAGE_COMBINED(1),
		/**
		 * Packet is in first hop of emergency route; packet wouldn't have
		 * reached this router without emergency routing.
		 */
		FIRST_STAGE(2),
		/**
		 * Packet is in last hop of emergency route and should now return to
		 * normal routing.
		 */
		SECOND_STAGE(3);

		/** The encoded value. */
		public final int value;
		EmergencyRoutingStatus(int value) {
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static EmergencyRoutingStatus get(int value) {
			switch (value) {
			case 0:
				return NORMAL;
			case 1:
				return FIRST_STAGE_COMBINED;
			case 2:
				return FIRST_STAGE;
			case 3:
				return SECOND_STAGE;
			default:
				return null;
			}
		}
	}

	/**
	 * Packet type flags for the diagnostic filters. Note that only one has to
	 * match for the counter to be incremented.
	 */
	public enum PacketType {
		/** Packet is multicast. */
		MULTICAST(0),
		/** Packet is point-to-point. */
		POINT_TO_POINT(1),
		/** Packet is nearest-neighbour. */
		NEAREST_NEIGHBOUR(2),
		/** Packet is fixed-route. */
		FIXED_ROUTE(3);

		/** The encoded value. */
		public final int value;
		PacketType(int value) {
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static PacketType get(int value) {
			switch (value) {
			case 0:
				return MULTICAST;
			case 1:
				return POINT_TO_POINT;
			case 2:
				return NEAREST_NEIGHBOUR;
			case 3:
				return FIXED_ROUTE;
			default:
				return null;
			}
		}
	}

	/**
	 * Payload flags for the diagnostic filters. Note that only one has to match
	 * for the counter to be incremented.
	 */
	public enum PayloadStatus {
		/** Packet has a payload. */
		WITH_PAYLOAD(0),
		/** Packet doesn't have a payload. */
		WITHOUT_PAYLOAD(1);

		/** The encoded value. */
		public final int value;
		PayloadStatus(int value) {
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static PayloadStatus get(int value) {
			switch (value) {
			case 0:
				return WITH_PAYLOAD;
			case 1:
				return WITHOUT_PAYLOAD;
			default:
				return null;
			}
		}
	}

	/**
	 * Source flags for the diagnostic filters. Note that only one has to match
	 * for the counter to be incremented.
	 */
	public enum Source {
		/** Source is a local core. */
		LOCAL(0),
		/** Source is not a local core. */
		NON_LOCAL(1);

		/** The encoded value. */
		public final int value;
		Source(int value) {
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static Source get(int value) {
			switch (value) {
			case 0:
				return LOCAL;
			case 1:
				return NON_LOCAL;
			default:
				return null;
			}
		}
	}
}
