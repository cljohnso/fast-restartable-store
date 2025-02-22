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

import com.terracottatech.frs.config.FrsProperty;
import com.terracottatech.frs.io.Chunk;
import com.terracottatech.frs.io.CopyingChunk;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.terracottatech.frs.config.FrsProperty.FORCE_LOG_REGION_FORMAT;
import static org.junit.Assert.*;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 *
 * @author mscott
 */
public class LogRegionPackerTest {
  
  public LogRegionPackerTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  /**
   * Test of pack method, of class LogRegionPacker.
   */
  @Test
  public void testPack() throws Exception {
    System.out.println("pack");
    final ArrayList<LogRecord> list = new ArrayList<LogRecord>();
    LogRecord mock = Mockito.mock(LogRecord.class);
    when(mock.getPayload()).thenReturn(new ByteBuffer[] {ByteBuffer.allocate(512)});
    when(mock.getLsn()).thenReturn(999L);
    list.add(mock);
    Iterable<LogRecord> payload = new Iterable<LogRecord>() {

      @Override
      public Iterator<LogRecord> iterator() {
        return list.iterator();
      }
    };
    LogRegionPacker instance = new LogRegionPacker(Signature.ADLER32, (String) FORCE_LOG_REGION_FORMAT.defaultValue());
    Chunk result = instance.pack(payload);
    verifyChunkHeader(result);
    verifyHints(result,0);
    verify(mock).getPayload();
    verify(mock).getLsn();
  }
  
  /**
   * Test of pack method, of class LogRegionPacker.
   */
  @Test
  public void testLargePack() throws Exception {
    System.out.println("large pack");
    final ArrayList<LogRecord> list = new ArrayList<LogRecord>();
    LogRecord mock = Mockito.mock(LogRecord.class);
    when(mock.getPayload()).thenReturn(new ByteBuffer[] {ByteBuffer.allocate(512)});
    when(mock.getLsn()).thenReturn(999L);
    for (int x=0;x<8192;x++) {
      list.add(mock);
    }
    Iterable<LogRecord> payload = new Iterable<LogRecord>() {

      @Override
      public Iterator<LogRecord> iterator() {
        return list.iterator();
      }
    };
    LogRegionPacker instance = new LogRegionPacker(Signature.ADLER32, (String) FrsProperty.FORCE_LOG_REGION_FORMAT.defaultValue());
    Chunk result = instance.pack(payload);
    verifyChunkHeader(result);
    verifyHints(result,255);
    verify(mock, times(8192)).getPayload();
    verify(mock, times(8192)).getLsn();
  }  
  
  private boolean verifyChunkHeader(Chunk c) {
    assertEquals(c.getShort(), LogRegionPacker.REGION_VERSION);
    assertEquals(c.getLong(), c.getLong());
    c.get(); //  region format
    c.get(); //  region format
    return true;
  }
  
    
  private boolean verifyRecordHeader(Chunk c) {
    c.getShort(); // record header
    c.getLong(); // lsn
    c.getLong();  // length 
    return true;
  }
  
    
  private boolean verifyHints(Chunk c, int expectedSize) {
    int size = c.getShort();
    assertEquals(expectedSize, size);
    for (int x=0;x<size;x++) {
      System.out.println(c.getLong());
    }
    return true;
  }

  /**
   * Test of extract method, of class LogRegionPacker.
   */
  @Test
  public void testExtract() throws Exception {
    System.out.println("extract");

    final ArrayList<LogRecord> list = new ArrayList<LogRecord>();
    final AtomicLong lsn = new AtomicLong(999);

    LogRecord mock = Mockito.mock(LogRecord.class);
    when(mock.getPayload()).then(new Answer<ByteBuffer[]> () {

      @Override
      public ByteBuffer[] answer(InvocationOnMock invocation) throws Throwable {
        return new ByteBuffer[] {ByteBuffer.allocate(512)};
      }

    });

    when(mock.getLsn()).then(new Answer<Long> () {

      @Override
      public Long answer(InvocationOnMock invocation) throws Throwable {
        return lsn.incrementAndGet();
      }

    });
      
    for (int x=0;x<1024;x++) {
      list.add(mock);
    }
      
    Iterable<LogRecord> payload = new Iterable<LogRecord>() {

      @Override
      public Iterator<LogRecord> iterator() {
        return list.iterator();
      }
    };
    LogRegionPacker instance = new LogRegionPacker(Signature.ADLER32, (String) FORCE_LOG_REGION_FORMAT.defaultValue());
    Chunk result = instance.pack(payload);
    Chunk check = new CopyingChunk(result);
    verifyChunkHeader(check);
    verify(mock,times(1024)).getPayload();
    verify(mock,times(1024)).getLsn();
    
    for (int x=0;x<1024;x++) {
      System.out.println("ITERATION: " + x);
      check.clear();
      LogRecord lr = LogRegionPacker.extract(Signature.NONE, (String) FORCE_LOG_REGION_FORMAT.defaultValue(), check,1000 + x);
      assertNotNull(lr);
    }
  }
}
