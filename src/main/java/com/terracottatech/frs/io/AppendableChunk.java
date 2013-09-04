/*
 * All content copyright (c) 2013 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs.io;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author mscott
 */
public class AppendableChunk extends AbstractChunk {
    private ByteBuffer[] buffers;

    public AppendableChunk(List<ByteBuffer>  base) {
        this.buffers = base.toArray(new ByteBuffer[base.size()]);
    }
 
    public AppendableChunk(ByteBuffer[]  base) {
        this.buffers = base;
    }   
    
    public AppendableChunk copy() {
        ByteBuffer[] cb = Arrays.copyOf(buffers, buffers.length);
        for ( int x=0;x<cb.length;x++ ) {
            cb[x] = (ByteBuffer)cb[x].duplicate().clear();
        }
        return new AppendableChunk(cb);
    }
    
    public void append(ByteBuffer add) {
        ByteBuffer[] list = Arrays.copyOf(buffers, buffers.length + 1);
        list[buffers.length] = add;
        buffers = list;
    }

    @Override
    public ByteBuffer[] getBuffers() {
        return buffers;
    }
    
    public void truncate(long position) {
        long len = 0;
        for (int x=0;x<buffers.length;x++) {
            len += buffers[x].limit();
            if ( buffers[x].capacity() != buffers[x].limit()) {
                throw new AssertionError("bad truncation");
            }
            if ( len > position ) {
                buffers[x].position(buffers[x].limit() - (int)(len-position));
                buffers[x].flip();
                buffers[x] = buffers[x].slice();
                buffers[x].position(buffers[x].limit());
                if ( x+1 != buffers.length ) {
                    buffers = Arrays.copyOf(buffers, x+1);
                }
                if ( this.hasRemaining() ) {
                    throw new AssertionError("bad truncation");
                }
                return;
            } else {
                buffers[x].position(buffers[x].limit());
            }
        }
    }
    
    public void destroy() {
        buffers = new ByteBuffer[0];
    }
    
    public void mark() {
        for ( ByteBuffer bb : buffers ) {
            if ( bb.hasRemaining() ) {
                bb.mark();
                return;
            }
        }
    }
    
    public void reset() {
        for ( ByteBuffer bb : buffers ) {
            if ( bb.hasRemaining() ) {
                bb.reset();
                return;
            }
        }
    }
    
}
