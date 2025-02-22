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
package com.terracottatech.frs.log;

import com.terracottatech.frs.io.AbstractChunk;
import com.terracottatech.frs.io.BufferSource;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 *
 * @author mscott
 */
public class BufferListWrapper extends AbstractChunk implements Closeable {
    
    private final ByteBuffer[]  converted;
    private final BufferSource  source;
    
    public BufferListWrapper(List<ByteBuffer> base) {
        converted = base.toArray(new ByteBuffer[base.size()]);
        this.source = null;
    }    
    
    public BufferListWrapper(List<ByteBuffer> base, BufferSource source) {
        converted = base.toArray(new ByteBuffer[base.size()]);
        this.source = source;
    }

    @Override
    public ByteBuffer[] getBuffers() {
        return converted;
    }

    @Override
    public void close() throws IOException {
      if ( source != null ) {
        for ( ByteBuffer bb : converted ) {
          source.returnBuffer(bb);
        }
      }
    }
}
