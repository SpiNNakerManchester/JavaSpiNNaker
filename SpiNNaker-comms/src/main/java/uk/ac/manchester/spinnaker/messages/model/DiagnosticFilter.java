/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.model;

import static java.util.stream.Collectors.toUnmodifiableList;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

import java.util.Collection;
import java.util.List;
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
	 * @param matchEmergencyRoutingStatusToIncomingPacket
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
			boolean matchEmergencyRoutingStatusToIncomingPacket,
			Collection<Destination> destinations, Collection<Source> sources,
			Collection<PayloadStatus> payloadStatuses,
			Collection<DefaultRoutingStatus> defaultRoutingStatuses,
			Collection<EmergencyRoutingStatus> emergencyRoutingStatuses,
			Collection<PacketType> packetTypes) {
		this.enableInterrupt = enableInterruptOnCounterEvent;
		this.emergencyMode = matchEmergencyRoutingStatusToIncomingPacket;
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
			return List.of(defaults);
		}
		return List.copyOf(collection);
	}

	/**
	 * @param encodedValue
	 *            The word of data that would be written to the router to set up
	 *            the filter.
	 */
	public DiagnosticFilter(int encodedValue) {
		enableInterrupt = bitSet(encodedValue, ENABLE_INTERRUPT_OFFSET);
		emergencyMode = bitSet(encodedValue, EMERGENCY_ROUTE_MODE_OFFSET);
		destinations = Stream.of(Destination.values())
				.filter(d -> d.bitSet(encodedValue))
				.collect(toUnmodifiableList());
		sources = Stream.of(Source.values()).filter(s -> s.bitSet(encodedValue))
				.collect(toUnmodifiableList());
		payloads = Stream.of(PayloadStatus.values())
				.filter(p -> p.bitSet(encodedValue))
				.collect(toUnmodifiableList());
		defaultStatuses = Stream.of(DefaultRoutingStatus.values())
				.filter(dr -> dr.bitSet(encodedValue))
				.collect(toUnmodifiableList());
		emergencyStatuses = Stream.of(EmergencyRoutingStatus.values())
				.filter(er -> er.bitSet(encodedValue))
				.collect(toUnmodifiableList());
		packetTypes = Stream.of(PacketType.values())
				.filter(pt -> pt.bitSet(encodedValue))
				.collect(toUnmodifiableList());
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
		for (var val : destinations) {
			data |= val.bit;
		}
		for (var val : sources) {
			data |= val.bit;
		}
		for (var val : payloads) {
			data |= val.bit;
		}
		for (var val : defaultStatuses) {
			data |= val.bit;
		}
		for (var val : emergencyStatuses) {
			data |= val.bit;
		}
		for (var val : packetTypes) {
			data |= val.bit;
		}
		return data;
	}

	/** How to get the bit index of an enum value. */
	public interface GetBitIndex {
		/**
		 * Get the bit index of this value.
		 *
		 * @return The encoded value's bit index.
		 */
		int bit();

		/**
		 * Is the bit for this enum value set in the given word?
		 *
		 * @param value
		 *            The word to examine.
		 * @return True if the bit is set, false if it isn't.
		 */
		default boolean bitSet(int value) {
			return ((value >> bit()) & 0x1) == 1;
		}
	}

	/**
	 * Default routing flags for the diagnostic filters. Note that only one has
	 * to match for the counter to be incremented.
	 */
	public enum DefaultRoutingStatus implements GetBitIndex {
		/** Packet is to be default routed. */
		DEFAULT_ROUTED(0),
		/** Packet is not to be default routed. */
		NON_DEFAULT_ROUTED(1);

		/** The encoded value's bit index. */
		public final int bit;

		private final int value;

		private static final Map<Integer, DefaultRoutingStatus> MAP =
				makeEnumBackingMap(values(), v -> v.value);

		DefaultRoutingStatus(int value) {
			this.bit = value + DEFAULT_ROUTE_OFFSET;
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value.
		 * @return The decoded value, or {@code null} if the decoding failed.
		 */
		static DefaultRoutingStatus get(int value) {
			return MAP.get(value);
		}

		@Override
		public int bit() {
			return bit;
		}
	}

	/**
	 * Destination flags for the diagnostic filters. Note that only one has to
	 * match for the counter to be incremented.
	 */
	public enum Destination implements GetBitIndex {
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

		private static final Map<Integer, Destination> MAP =
				makeEnumBackingMap(values(), v -> v.value);

		Destination(int value) {
			this.bit = value + DESTINATION_OFFSET;
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value, without the shift.
		 * @return The decoded value, or {@code null} if the decoding failed.
		 */
		static Destination get(int value) {
			return MAP.get(value);
		}

		@Override
		public int bit() {
			return bit;
		}
	}

	/**
	 * Emergency routing status flags for the diagnostic filters. Note that only
	 * one has to match for the counter to be incremented.
	 */
	public enum EmergencyRoutingStatus implements GetBitIndex {
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

		private static final Map<Integer, EmergencyRoutingStatus> MAP =
				makeEnumBackingMap(values(), v -> v.value);

		EmergencyRoutingStatus(int value) {
			this.bit = value + EMERGENCY_ROUTE_OFFSET;
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value, without the shift.
		 * @return The decoded value, or {@code null} if the decoding failed.
		 */
		static EmergencyRoutingStatus get(int value) {
			return MAP.get(value);
		}

		@Override
		public int bit() {
			return bit;
		}
	}

	/**
	 * Packet type flags for the diagnostic filters. Note that only one has to
	 * match for the counter to be incremented.
	 */
	public enum PacketType implements GetBitIndex {
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

		private static final Map<Integer, PacketType> MAP =
				makeEnumBackingMap(values(), v -> v.value);

		PacketType(int value) {
			this.bit = value + PACKET_TYPE_OFFSET;
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value, without the shift.
		 * @return The decoded value, or {@code null} if the decoding failed.
		 */
		static PacketType get(int value) {
			return MAP.get(value);
		}

		@Override
		public int bit() {
			return bit;
		}
	}

	/**
	 * Payload flags for the diagnostic filters. Note that only one has to match
	 * for the counter to be incremented.
	 */
	public enum PayloadStatus implements GetBitIndex {
		/** Packet has a payload. */
		WITH_PAYLOAD(0),
		/** Packet doesn't have a payload. */
		WITHOUT_PAYLOAD(1);

		/** The encoded value's bit index. */
		public final int bit;

		private final int value;

		private static final Map<Integer, PayloadStatus> MAP =
				makeEnumBackingMap(values(), v -> v.value);

		PayloadStatus(int value) {
			this.bit = value + PAYLOAD_OFFSET;
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value, without the shift.
		 * @return The decoded value, or {@code null} if the decoding failed.
		 */
		static PayloadStatus get(int value) {
			return MAP.get(value);
		}

		@Override
		public int bit() {
			return bit;
		}
	}

	/**
	 * Source flags for the diagnostic filters. Note that only one has to match
	 * for the counter to be incremented.
	 */
	public enum Source implements GetBitIndex {
		/** Source is a local core. */
		LOCAL(0),
		/** Source is not a local core. */
		NON_LOCAL(1);

		/** The encoded value's bit index. */
		public final int bit;

		private final int value;

		private static final Map<Integer, Source> MAP =
				makeEnumBackingMap(values(), v -> v.value);

		Source(int value) {
			this.bit = value + SOURCE_OFFSET;
			this.value = value;
		}

		/**
		 * @param value
		 *            The encoded value, without the shift.
		 * @return The decoded value, or {@code null} if the decoding failed.
		 */
		static Source get(int value) {
			return MAP.get(value);
		}

		@Override
		public int bit() {
			return bit;
		}
	}
}
