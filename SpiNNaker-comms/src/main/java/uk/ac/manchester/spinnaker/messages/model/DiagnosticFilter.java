package uk.ac.manchester.spinnaker.messages.model;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
	@SuppressWarnings("checkstyle:ParameterNumber")
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
		this.defaultStatuses =
				convert(defaultRoutingStatuses, DefaultRoutingStatus.values());
		this.emergencyStatuses = convert(emergencyRoutingStatuses,
				EmergencyRoutingStatus.values());
		this.packetTypes = convert(packetTypes, PacketType.values());
	}

	private static <T> Collection<T> convert(Collection<T> collection,
			T[] defaults) {
		if (collection == null || collection.isEmpty()) {
			return unmodifiableList(asList(defaults));
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
				.filter(d -> bitSet(encodedValue, d.bit)).collect(toList()));
		sources = unmodifiableCollection(Stream.of(Source.values())
				.filter(s -> bitSet(encodedValue, s.bit)).collect(toList()));
		payloads = unmodifiableCollection(Stream.of(PayloadStatus.values())
				.filter(p -> bitSet(encodedValue, p.bit)).collect(toList()));
		defaultStatuses = unmodifiableCollection(Stream
				.of(DefaultRoutingStatus.values())
				.filter(dr -> bitSet(encodedValue, dr.bit)).collect(toList()));
		emergencyStatuses = unmodifiableCollection(Stream
				.of(EmergencyRoutingStatus.values())
				.filter(er -> bitSet(encodedValue, er.bit)).collect(toList()));
		packetTypes = unmodifiableCollection(Stream.of(PacketType.values())
				.filter(pt -> bitSet(encodedValue, pt.bit)).collect(toList()));
	}

	private static boolean bitSet(int value, int bitIndex) {
		return ((value >> bitIndex) & 0x1) == 1;
	}

	/**
	 * @return Whether an interrupt should be raised when this rule matches.
	 */
	public boolean getEnableInterruptOnCounterEvent() {
		return enableInterrupt;
	}

	/**
	 * @return Whether the emergency routing statuses should be matched against
	 *         packets arriving at this router (if True), or if they should be
	 *         matched against packets leaving this router (if False).
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
			data |= val.bit;
		}
		for (Source val : sources) {
			data |= val.bit;
		}
		for (PayloadStatus val : payloads) {
			data |= val.bit;
		}
		for (DefaultRoutingStatus val : defaultStatuses) {
			data |= val.bit;
		}
		for (EmergencyRoutingStatus val : emergencyStatuses) {
			data |= val.bit;
		}
		for (PacketType val : packetTypes) {
			data |= val.bit;
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

		/** The encoded value's bit index. */
		public final int bit;
		private final int value;
		private static final Map<Integer, DefaultRoutingStatus> MAP;

		DefaultRoutingStatus(int value) {
			this.bit = value + DEFAULT_ROUTE_OFFSET;
			this.value = value;
		}

		static {
			MAP = new HashMap<>();
			for (DefaultRoutingStatus d : values()) {
				MAP.put(d.value, d);
			}
		}

		/**
		 * @param value
		 *            The encoded value.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static DefaultRoutingStatus get(int value) {
			return MAP.get(value);
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

		/** The encoded value's bit index. */
		public final int bit;
		private final int value;
		private static final Map<Integer, Destination> MAP;

		Destination(int value) {
			this.bit = value + DESTINATION_OFFSET;
			this.value = value;
		}

		static {
			MAP = new HashMap<>();
			for (Destination d : values()) {
				MAP.put(d.value, d);
			}
		}

		/**
		 * @param value
		 *            The encoded value, without the shift.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static Destination get(int value) {
			return MAP.get(value);
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

		/** The encoded value's bit index. */
		public final int bit;
		private final int value;
		private static final Map<Integer, EmergencyRoutingStatus> MAP;

		EmergencyRoutingStatus(int value) {
			this.bit = value + EMERGENCY_ROUTE_OFFSET;
			this.value = value;
		}

		static {
			MAP = new HashMap<>();
			for (EmergencyRoutingStatus e : values()) {
				MAP.put(e.value, e);
			}
		}

		/**
		 * @param value
		 *            The encoded value, without the shift.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static EmergencyRoutingStatus get(int value) {
			return MAP.get(value);
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

		/** The encoded value's bit index. */
		public final int bit;
		private final int value;
		private static final Map<Integer, PacketType> MAP;

		PacketType(int value) {
			this.bit = value + PACKET_TYPE_OFFSET;
			this.value = value;
		}

		static {
			MAP = new HashMap<>();
			for (PacketType p : values()) {
				MAP.put(p.value, p);
			}
		}

		/**
		 * @param value
		 *            The encoded value, without the shift.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static PacketType get(int value) {
			return MAP.get(value);
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

		/** The encoded value's bit index. */
		public final int bit;
		private final int value;
		private static final Map<Integer, PayloadStatus> MAP;

		PayloadStatus(int value) {
			this.bit = value + PAYLOAD_OFFSET;
			this.value = value;
		}

		static {
			MAP = new HashMap<>();
			for (PayloadStatus p : values()) {
				MAP.put(p.value, p);
			}
		}

		/**
		 * @param value
		 *            The encoded value, without the shift.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static PayloadStatus get(int value) {
			return MAP.get(value);
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

		/** The encoded value's bit index. */
		public final int bit;
		private final int value;
		private static final Map<Integer, Source> MAP;

		Source(int value) {
			this.bit = value + SOURCE_OFFSET;
			this.value = value;
		}

		static {
			MAP = new HashMap<>();
			for (Source s : values()) {
				MAP.put(s.value, s);
			}
		}

		/**
		 * @param value
		 *            The encoded value, without the shift.
		 * @return The decoded value, or <tt>null</tt> if the decoding failed.
		 */
		static Source get(int value) {
			return MAP.get(value);
		}
	}
}
