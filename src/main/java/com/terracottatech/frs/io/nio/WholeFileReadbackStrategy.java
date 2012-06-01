/*
 * All content copyright (c) 2012 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs.io.nio;

import com.terracottatech.frs.io.Chunk;
import com.terracottatech.frs.io.Direction;
import com.terracottatech.frs.io.FileBuffer;
import com.terracottatech.frs.io.WrappingChunk;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 *
 * @author mscott
 */
class WholeFileReadbackStrategy extends AbstractReadbackStrategy {
    
    private final FileBuffer                      buffer;
    private ListIterator<Chunk>   chunks;
    private Direction             queueDirection;
    
    
    public WholeFileReadbackStrategy(FileBuffer buffer) throws IOException {
        super();
        this.buffer = buffer;
        queue(Direction.REVERSE);
    }
    
    @Override
    public Iterator<Chunk> iterator() {
        return chunks;
    }
    
    @Override
    public boolean hasMore(Direction dir) throws IOException {
        if ( dir == queueDirection && chunks.hasNext() ) return true;
        if ( dir != queueDirection && chunks.hasPrevious() ) return true;
        return false;
    }
    
 
    @Override
    public Chunk iterate(Direction dir) throws IOException {
        if ( dir == queueDirection && chunks.hasNext() ) return chunks.next();
        if ( dir != queueDirection && chunks.hasPrevious() ) return chunks.previous();
        return null;
    }   
    
    private ByteBuffer prepare() throws IOException {
        ByteBuffer copy = ByteBuffer.allocate((int)(buffer.size() - NIOSegmentImpl.FILE_HEADER_SIZE));
        buffer.partition((int)buffer.size() - NIOSegmentImpl.FILE_HEADER_SIZE);
        buffer.read(1);
        copy.put(buffer.getBuffers()[0]);
        copy.flip();
        return copy;
    }

    private void queue(Direction dir) throws IOException {  
        Chunk copy = new WrappingChunk(prepare());
        
        List<Chunk> list = new ArrayList<Chunk>();
        ByteBuffer[] chunk = readChunk(copy);

        while (chunk != null) {
            list.add(new WrappingChunk(chunk));
            chunk = readChunk(copy);
        }
        
        if ( dir == Direction.REVERSE ) Collections.reverse(list); 
        
        this.chunks = list.listIterator();
        this.queueDirection = dir;
    }
    
    
}
