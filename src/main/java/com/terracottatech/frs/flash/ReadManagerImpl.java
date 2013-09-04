/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracottatech.frs.flash;

import com.terracottatech.frs.io.Chunk;
import com.terracottatech.frs.io.IOManager;
import com.terracottatech.frs.log.FormatException;
import com.terracottatech.frs.log.LogRecord;
import com.terracottatech.frs.log.LogRegionPacker;
import com.terracottatech.frs.log.Signature;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author mscott
 */
public class ReadManagerImpl implements ReadManager {
  
  private final IOManager ioManager;
  private Cache cached = new Cache();
  
  public ReadManagerImpl(IOManager io) {
    this.ioManager = io;
  }
  
  public synchronized LogRecord checkCache(long lsn) {
      LogRecord lr = cached.remove(lsn);
      if ( lr != null ) {
          cached.put(lsn,lr);
      }
      return lr;
  }
  
  public synchronized LogRecord cacheRecords(long lsn, List<LogRecord> records) {
      LogRecord target = null;
      for ( LogRecord lr : records ) {
          cached.put(lr.getLsn(), lr);
          if ( lr.getLsn() == lsn ) {
              target = lr;
          }
      }
      return target;
  }

  @Override
  public LogRecord get(long marker) throws IOException {
    try {
        Chunk c = ioManager.scan(marker);
// maybe try and cache this
        return LogRegionPacker.extract(Signature.NONE, c, marker);
    } catch ( FormatException form ) {
        throw new IOException(form);
    } 
  }
  
  static class Cache extends LinkedHashMap<Long, LogRecord> {
        boolean over = false;

        @Override
        public LogRecord remove(Object key) {
            over = false;
            return super.remove(key); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected boolean removeEldestEntry(Entry<Long, LogRecord> eldest) {
            if ( over == false && this.size() > 1000 ) {
                over = true;
            }
            return over;
        }
      
  }
  
}