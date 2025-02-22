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
public class CopyingChunk extends AbstractChunk {

   private final ByteBuffer[] list;
    
   public CopyingChunk(Chunk src) {
       ByteBuffer[] copy = src.getBuffers();
       list = new ByteBuffer[copy.length];
       int x=0;
       for ( ByteBuffer c : copy ) {
           list[x++] = (ByteBuffer)ByteBuffer.allocate(c.remaining()).put(c).flip();
       }
   }

    @Override
    public ByteBuffer[] getBuffers() {
        return list;
    } 
}
