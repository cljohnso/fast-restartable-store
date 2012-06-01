/*
 * All content copyright (c) 2012 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs.io;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.TreeMap;

/**
 *
 * @author mscott
 */
public class CachingBufferSource implements BufferSource {
    private long   totalSize;
    private final TreeMap<Integer,ByteBuffer> freeList = new TreeMap<Integer,ByteBuffer>( new Comparator<Integer>() {
        @Override
        public int compare(Integer t, Integer t1) {
     // make sure nothing ever equals so everything fits in the set
            return t.intValue() - t1.intValue();
        }
    });
    
    public CachingBufferSource() {
    }
    

    @Override
    public ByteBuffer getBuffer(int size) {
        if (freeList.isEmpty()) {
            return null;
        }
        Integer get = freeList.ceilingKey(size);
        if ( get == null ) {
            get = freeList.floorKey(size);
        }
//  don't need to check for null again, already check that the map is not empty
        ByteBuffer buffer = freeList.remove(get);
        totalSize -= buffer.capacity();

        if ( buffer.capacity() < size ) {
            findSlot(buffer);
            return null;
        }
        
        assert(frameIsZeroed(buffer));
        
        buffer.clear();
        buffer.limit(size);
        if ( buffer.capacity() > size * 2 ) {
            buffer.clear();
            buffer.limit(size);
            ByteBuffer slice = buffer.slice();
            buffer.limit(buffer.capacity()).position(size+1);
            findSlot(buffer.slice());
            buffer = slice;
        } else {
            buffer.limit(size);
        }
        assert(buffer == null || buffer.remaining() == size);
        return buffer;
    }
    
    public long getSize() {
        return totalSize;
    }

    @Override
    public void reclaim() {
        totalSize = 0;
        freeList.clear();
    }

    @Override
    public void returnBuffer(ByteBuffer buffer) {
        assert(zeroFrame(buffer));
        findSlot(buffer);
    }
    
    private void findSlot(ByteBuffer buffer) {
        buffer.clear();
        totalSize += buffer.capacity();
        assert(checkReturn(buffer));
        while ( buffer != null ) {
            if ( buffer.limit() == 0 ) {
                totalSize -= buffer.capacity();
                return;
            }
            buffer = freeList.put(buffer.limit(),buffer);
            if ( buffer != null ) buffer.limit(buffer.limit()-1);
        }
    }
    
    private boolean zeroFrame(ByteBuffer buffer) {
        buffer.clear();
        for ( int x=0;x<buffer.capacity();x++) {
            buffer.put(x,(byte)0);
        }
        return true;
    }
    
    private boolean frameIsZeroed(ByteBuffer buffer) {
        if ( buffer == null ) return true;
        buffer.clear();
        for ( int x=0;x<buffer.capacity();x++) {
            if ( buffer.get(x) != 0 ) {
                return false;
            }
        }
        return true;
    }
        
    private boolean checkReturn(ByteBuffer buffer) {
        if ( buffer == null ) return true;
        for ( ByteBuffer b : freeList.values() ) {
            if ( b == buffer ) {
                return false;
            }
        }
        return true;
    }
}
