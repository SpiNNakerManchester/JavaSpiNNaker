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

	private final boolean enable_interrupt;
	private final boolean emergency_mode;
	private final Collection<Destination> destinations;
	private final Collection<Source> sources;
	private final Collection<PayloadStatus> payloads;
	private final Collection<DefaultRoutingStatus> default_statuses;
	private final Collection<EmergencyRoutingStatus> emergency_statuses;
	private final Collection<PacketType> packet_types;

	/**
	 * @param enable_interrupt_on_counter_event
	 *            Indicates whether an interrupt should be raised when this rule
	 *            matches
	 * @param match_emergency_routing_status_to_incoming_packet
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
	 * @param payloads
	 *            Increment the counter if one or more of the given payload
	 *            statuses match (or empty list to match all)
	 * @param default_statuses
	 *            Increment the counter if one or more of the given default
	 *            routing statuses match (or empty list to match all)
	 * @param emergency_statuses
	 *            Increment the counter if one or more of the given emergency
	 *            routing statuses match (or empty list to match all)
	 * @param packet_types
	 *            Increment the counter if one or more of the given packet types
	 *            match (or empty list to match all)
	 */
	public DiagnosticFilter(boolean enable_interrupt_on_counter_event,
			boolean match_emergency_routing_status_to_incoming_packet,
			Collection<Destination> destinations, Collection<Source> sources,
			Collection<PayloadStatus> payload_statuses,
			Collection<DefaultRoutingStatus> default_routing_statuses,
			Collection<EmergencyRoutingStatus> emergency_routing_statuses,
			Collection<PacketType> packet_types) {
		this.enable_interrupt = enable_interrupt_on_counter_event;
		this.emergency_mode = match_emergency_routing_status_to_incoming_packet;
		this.destinations = convert(destinations, Destination.values());
		this.sources = convert(sources, Source.values());
		this.payloads = convert(payload_statuses, PayloadStatus.values());
		this.default_statuses = convert(default_routing_statuses,
				DefaultRoutingStatus.values());
		this.emergency_statuses = convert(emergency_routing_statuses,
				EmergencyRoutingStatus.values());
		this.packet_types = convert(packet_types, PacketType.values());
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
		enable_interrupt = bitSet(encodedValue, ENABLE_INTERRUPT_OFFSET);
		emergency_mode = bitSet(encodedValue, EMERGENCY_ROUTE_MODE_OFFSET);
		destinations = unmodifiableCollection(Stream.of(Destination.values())
				.filter(d -> bitSet(encodedValue, DESTINATION_OFFSET + d.value))
				.collect(Collectors.toList()));
		sources = unmodifiableCollection(Stream.of(Source.values())
				.filter(s -> bitSet(encodedValue, SOURCE_OFFSET + s.value))
				.collect(Collectors.toList()));
		payloads = unmodifiableCollection(Stream.of(PayloadStatus.values())
				.filter(p -> bitSet(encodedValue, PAYLOAD_OFFSET + p.value))
				.collect(Collectors.toList()));
		default_statuses = unmodifiableCollection(
				Stream.of(DefaultRoutingStatus.values())
						.filter(dr -> bitSet(encodedValue,
								DEFAULT_ROUTE_OFFSET + dr.value))
						.collect(Collectors.toList()));
		emergency_statuses = unmodifiableCollection(
				Stream.of(EmergencyRoutingStatus.values())
						.filter(er -> bitSet(encodedValue,
								EMERGENCY_ROUTE_OFFSET + er.value))
						.collect(Collectors.toList()));
		packet_types = unmodifiableCollection(Stream.of(PacketType.values())
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
		return enable_interrupt;
	}

	/**
	 * Whether the emergency routing statuses should be matched against packets
	 * arriving at this router (if True), or if they should be matched against
	 * packets leaving this router (if False).
	 */
	public boolean getMatchEmergencyRoutingStatusToIncomingPacket() {
		return emergency_mode;
	}

	/**
	 * The set of destinations to match
	 */
	public Collection<Destination> getDestinations() {
		return destinations;
	}

	/**
	 * The set of sources to match
	 */
	public Collection<Source> getSources() {
		return sources;
	}

	/**
	 * The set of payload statuses to match
	 */
	public Collection<PayloadStatus> getPayloadStatuses() {
		return payloads;
	}

	/**
	 * The set of default routing statuses to match
	 */
	public Collection<DefaultRoutingStatus> getDefaultRoutingStatuses() {
		return default_statuses;
	}

	/**
	 * The set of emergency routing statuses to match
	 */
	public Collection<EmergencyRoutingStatus> getEmergencyRoutingStatuses() {
		return emergency_statuses;
	}

	/**
	 * The set of packet types to match
	 */
	public Collection<PacketType> getPacketTypes() {
		return packet_types;
	}

	/**
	 * A word of data that can be written to the router to set up the filter
	 */
	public int getFilterWord() {
		int data = (enable_interrupt ? 1 << ENABLE_INTERRUPT_OFFSET : 0);
		if (!emergency_mode) {
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
		for (DefaultRoutingStatus val : default_statuses) {
			data |= val.value + DEFAULT_ROUTE_OFFSET;
		}
		for (EmergencyRoutingStatus val : emergency_statuses) {
			data |= val.value + EMERGENCY_ROUTE_OFFSET;
		}
		for (PacketType val : packet_types) {
			data |= val.value + PACKET_TYPE_OFFSET;
		}
		return data;
	}

	/**
	 * Default routing flags for the diagnostic filters. Note that only one has
	 * to match for the counter to be incremented
	 */
	public enum DefaultRoutingStatus {
		/** Packet is to be default routed */
		DEFAULT_ROUTED(0),
		/** Packet is not to be default routed */
		NON_DEFAULT_ROUTED(1);
		public final int value;

		private DefaultRoutingStatus(int value) {
			this.value = value;
		}

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
	 * match for the counter to be incremented
	 */
	public enum Destination {
		/** Destination is to dump the packet */
		DUMP(0),
		/** Destination is a local core (but not the monitor core) */
		LOCAL(1),
		/** Destination is the local monitor core */
		LOCAL_MONITOR(2),
		/** Destination is link 0 */
		LINK_0(3),
		/** Destination is link 1 */
		LINK_1(4),
		/** Destination is link 2 */
		LINK_2(5),
		/** Destination is link 3 */
		LINK_3(6),
		/** Destination is link 4 */
		LINK_4(7),
		/** Destination is link 5 */
		LINK_5(8);
		public final int value;

		private Destination(int value) {
			this.value = value;
		}

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
		public final int value;

		private EmergencyRoutingStatus(int value) {
			this.value = value;
		}

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
		/** Packet is multicast */
		MULTICAST(0),
		/** Packet is point-to-point */
		POINT_TO_POINT(1),
		/** Packet is nearest-neighbour */
		NEAREST_NEIGHBOUR(2),
		/** Packet is fixed-route */
		FIXED_ROUTE(3);
		public final int value;

		private PacketType(int value) {
			this.value = value;
		}

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
		/** Packet has a payload */
		WITH_PAYLOAD(0),
		/** Packet doesn't have a payload */
		WITHOUT_PAYLOAD(1);
		public final int value;

		private PayloadStatus(int value) {
			this.value = value;
		}

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
		/** Source is a local core */
		LOCAL(0),
		/** Source is not a local core */
		NON_LOCAL(1);
		public final int value;

		private Source(int value) {
			this.value = value;
		}

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
