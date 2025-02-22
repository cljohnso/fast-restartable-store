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

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Wrap a file in a chunk for easy access.
 *
 * @author mscott
 */
public class FileBuffer extends AbstractChunk implements Closeable {

    protected final FileChannel channel;
    protected final BufferSource source;
    protected final ByteBuffer base;
    protected ByteBuffer[] ref;
    private int mark = 0;
    private long total = 0;
    private long offset = 0;

    public FileBuffer(FileChannel channel, ByteBuffer src) throws IOException {
        if ( src.position() != 0 ) {
            throw new AssertionError();
        }
        if ( src.capacity() < 512 ) {
            
        }
        this.channel = channel;
        this.base = src;
        this.source = null;
        this.ref = new ByteBuffer[]{base.duplicate()};
        this.offset = 0;
    }

    public FileBuffer(FileChannel channel, BufferSource src, int size) throws IOException {
        this.channel = channel;
        this.source = src;
        this.base = src.getBuffer(size);
        this.ref = new ByteBuffer[]{base.duplicate()};
        this.offset = 0;
    }    

    public long getTotal() {
        return total;
    }

    @Override
    public ByteBuffer[] getBuffers() {
        return ref;
    }

    @Override
    public long position() {
        return offset + super.position();
    }

    public long offset() {
        return offset;
    }

    public long size() throws IOException {
        return channel.size();
    }

    public int capacity() {
        return base.limit();
    }

    public void bufferMove(int src, int dest, int length) {
        ByteBuffer from = (ByteBuffer) base.duplicate().position(src).limit(src + length);
        ByteBuffer to = (ByteBuffer) base.duplicate().position(dest).limit(dest + length);
        to.put(from);
    }

    public FileBuffer position(long pos) throws IOException {
        if (pos < 0) {
            channel.position(channel.size() + pos);
        } else {
            channel.position(pos);
        }
        offset = channel.position() - super.position();
        return this;
    }

    public FileBuffer partition(int... pos) {
        ByteBuffer target = base.duplicate();

        ArrayList<ByteBuffer> sections = new ArrayList<ByteBuffer>();
        for (int p : pos) {
            if (p > target.limit()) {
                throw new BufferUnderflowException();
            } else {
                target.limit(p + target.position());
                sections.add(target.slice());
                target.clear().position(target.position() + p);
            }
        }
        sections.add(target.slice());

        ref = sections.toArray(new ByteBuffer[sections.size()]);
        mark = 0; //  reset the mark count so we start reading from the start again
        return this;
    }

    @Override
    public void clear() {
        ref = new ByteBuffer[]{base.duplicate()};
        mark = 0;
    }

    public long read(int count) throws IOException {
        long lt = 0;
        if ( mark == 0 ) offset = channel.position();
        for (int x = mark; x < mark + count; x++) {
            if (ref[x].isReadOnly()) {
                ref[x] = ref[x].duplicate();
                ref[x].limit(ref[x].position());
            } else {
                assert (ref[x].position() == 0);
            }
        }
        while (ref[mark + count - 1].hasRemaining()) {
            long read = channel.read(ref, mark, count);
            if (read < 0) {
                throw new EOFException(lt + " " + read + " " + mark + " " + count);
            }
            lt += read;
        }
        for (int x = mark; x < mark + count; x++) {
            ref[x].flip();
        }
        mark += count;
        total += lt;
        return lt;
    }
    
    public long truncate() {
        long lt = 0;
        for (int x = mark; x < ref.length; x++) {
            lt += ref[x].limit(ref[x].position()).position();
        }
        return lt;
    }

    public long writeFully(ByteBuffer buffer) throws IOException {
        long lt = 0;
        while (buffer.hasRemaining()) {
            lt += channel.write(buffer);
        }
        offset = channel.position();
        return lt;
    }    

    private ByteBuffer coalesce(ByteBuffer scratch, ByteBuffer[] list, int start, int count) {
//  use the remaining buffer space as scratch space for small buffer aggregation and
//  making sure the memory is direct memory
        if (count == 1 && (scratch.isDirect() == list[start].isDirect() || list[start].remaining() > scratch.remaining())) {
            // waste of time
            return list[start];
        }
        scratch.clear();
        for (int x = start; x < start + count; x++) {
            if (list[x].remaining() <= scratch.remaining()) {
                scratch.put(list[x]);
            } else {
                if ( count == 1 ) return list[x];
                //  no more scratch space, write the remaining the hard way
                throw new AssertionError("no space");
            }
        }
        //  write out the private copy to disk
        scratch.flip();

        return scratch;
    }

    private long coalescingWrite(int usage, int count) throws IOException {
        int smStart = -1;
        long lt = 0;
//  use the remaining buffer space as scratch space for small buffer aggregation and
//  making sure the memory is direct memory
        try {
            ByteBuffer memcpy = ((ByteBuffer) base.position(usage)).slice();
            int currentRun = 0;
            for (int x = mark; x < mark + count; x++) {
                if (ref[x].isDirect() && ref[x].remaining() > 512) {
                    if (smStart >= 0) {
                        //  write out smalls first
                        lt += writeFully(coalesce(memcpy, ref, smStart, x - smStart));
                        smStart = -1;
                        currentRun = 0;
                    }
                    //  write out big directs
                    lt += writeFully(coalesce(memcpy, ref, x, 1));
                } else if (currentRun + ref[x].remaining() > memcpy.capacity()) {
                    // buffer is full
                    if ( smStart < 0 ) {
                        lt += writeFully(coalesce(memcpy, ref, x, 1));
                    } else {
                        lt += writeFully(coalesce(memcpy, ref, smStart, x - smStart));
                        smStart = x;
                        currentRun = ref[x].remaining();
                    }
                } else {
                    if (smStart < 0) {
                        smStart = x;
                    }
                    currentRun += ref[x].remaining();
                }
            }

            if (smStart >= 0) {
//  finish the writes
                lt += writeFully(coalesce(memcpy, ref, smStart, (mark + count) - smStart));
            }
        } finally {
            base.position(0);
        }
        return lt;
    }
    //  need to do buffering b/c NIO des not handle small byte buffers quickly.   

    public long write(int count) throws IOException {
        long lt = 0;
        int usage = 0;
        boolean direct = true;
//  see how much private buffer is being used by memory writing and flip those buffers
        for (int x = mark; x < mark + count; x++) {
            if (!ref[x].isReadOnly()) {
                usage += ref[x].position();
                if (!ref[x].isDirect()) {
                    direct = false;
                }
                ref[x].flip();
            } else {
//  readonly buffers are inserted into the buffer chain but are not part of 
//  this file buffer's memory space
            }
        }

        if (count > 5 || !direct) {
            lt += coalescingWrite(usage, count);
        } else {
            for(int x=mark;x<count;x++) {
                lt += writeFully(ref[x]);
            }
        }

        mark += count;
        total += lt;
        return lt;
    }

    @Override
    public Chunk getChunk(long length) {
      if ( this.remaining() < length ) {
        return super.getChunk(length);
      } else {
        try {
          return new CloseableChunk(this.position(), length);
        } catch ( IOException ioe ) {
          throw new RuntimeException(ioe);
        }
      }
    }

    public void insert(ByteBuffer[] bufs, int loc, boolean writable) throws IOException {
        int len = ref.length;
        ref = Arrays.copyOf(ref, ref.length + bufs.length);
        System.arraycopy(ref, loc, ref, loc + bufs.length, len - loc);
        for (int x = 0; x < bufs.length; x++) {
            ref[loc + x] = ( writable ) ? bufs[x] : bufs[x].asReadOnlyBuffer();
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
        if ( source != null ) {
            source.returnBuffer(base);
        }
        ref = null;
    }
    
    public boolean isOpen() {
        return channel.isOpen();
    }
    
    public void sync(boolean meta) throws IOException {
        channel.force(meta);
    }

    @Override
    public String toString() {
        return "FileBuffer{" + "channel=" + channel.toString() + '}';
    }
    
    public FileChannel getFileChannel() {
        return channel;
    }

    private class CloseableChunk extends AbstractChunk implements Closeable {
      
      private final ByteBuffer[] data;

      public CloseableChunk(long start, long length) throws IOException {
        if ( source == null ) {
          throw new IOException("no buffer space");
        }
        if ( length > Integer.MAX_VALUE ) {
          throw new IOException("buffer overflow");
        }
        data = new ByteBuffer[] {source.getBuffer((int)length)};
        if ( data[0] == null ) {
          throw new IOException("no buffer space");
        }
        channel.position(start);
        while ( data[0].hasRemaining() ) {
          if ( channel.read(data) < 0 ) {
            throw new EOFException();
          }
        }
      }

      @Override
      public ByteBuffer[] getBuffers() {
        return data;
      }

      @Override
      public void close() throws IOException {
        source.returnBuffer(data[0]);
      }      
      
    }
    
}
