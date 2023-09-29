/*
 * Copyright (c) 2022 The University of Manchester
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
/**
 * Classes relating to boards in a SpiNNaker machine. Note that boards
 * themselves mostly not modelled, but their coordinates schemes are because
 * they are used in many places.
 * <p>
 * <em>Physically,</em> cabinets contain a collection of frames, and frames
 * contain a collection of boards. There is one controller BMP per frame; only
 * that BMP may talk to the other BMPs in the frame. When boards are not put in
 * a frame, their BMPs may be directly addressed (though in that situation there
 * are not usually more than three boards connected). {@linkplain PhysicalCoords
 * Physical coordinates} are typically only useful for when dealing with the
 * hardware itself; however, {@linkplain BMPLocation BMP coordinates} are
 * closely related.
 * <p>
 * In large machines, boards are <em>logically</em> grouped into
 * {@linkplain TriadCoords triads} for allocation control purposes; most
 * allocations are done in terms of triads (except for single-board allocations)
 * in order to keep the allocation algorithms reasonably tractable. The boards
 * of a triad <em>may not necessarily</em> be all on the same frame, but they
 * will be closely connected to one another.
 * <p>
 * The actual connectivity setup is done by
 * <a href="https://github.com/SpiNNakerManchester/SpiNNer">SpiNNer</a>. See
 * that project for details.
 */
package uk.ac.manchester.spinnaker.machine.board;
