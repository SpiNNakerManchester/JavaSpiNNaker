/*
 * Copyright (c) 2021 The University of Manchester
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
 * The BMP control subsystem. This (mainly {@link BMPController} is responsible
 * for booting boards and obtaining system information about them (such as the
 * serial version number). It is the <em>only</em> part of the server which
 * talks to any BMP; it takes great care to ensure that only one task is ever
 * given to one BMP at a time.
 */
package uk.ac.manchester.spinnaker.alloc.bmp;
