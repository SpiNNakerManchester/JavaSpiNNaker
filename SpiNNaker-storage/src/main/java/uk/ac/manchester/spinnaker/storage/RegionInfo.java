/*
 * Copyright (c) 2032 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

import java.nio.ByteBuffer;
import uk.ac.manchester.spinnaker.machine.MemoryLocation;

/**
 *
 * @author Christian
 */
public class RegionInfo {
    public final MemoryLocation pointer;

    public final ByteBuffer  content;

    public RegionInfo(ByteBuffer content, MemoryLocation pointer){
        this. content =  content;
        this.pointer = pointer;
    }

}    
