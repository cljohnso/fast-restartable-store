/*
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.terracottatech.frs.io;

import java.nio.ByteBuffer;

/**
 *
 * @author mscott
 */
public class CascadingBufferSource implements BufferSource {
    private final BufferSource base;
    
    public CascadingBufferSource(BufferSource base) {
        this.base = base;
    }

    @Override
    public ByteBuffer getBuffer(int size) {
        ByteBuffer buffer = base.getBuffer(size);
        if ( buffer == null ) {
            buffer = ByteBuffer.allocate(size);
        }
        return buffer;
    }

    @Override
    public void returnBuffer(ByteBuffer buffer) {
        base.returnBuffer(buffer);
    }

    @Override
    public void reclaim() {
        base.reclaim();
    }    
}
