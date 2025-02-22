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

import com.terracottatech.frs.SnapshotRequest;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class AtomicCommitListTest {
  private CommitList commitList;

  @Before
  public void setUp() throws Exception {
    commitList = new AtomicCommitList(10, 10, 2000);
  }

  @Test
  public void testBasicAppend() throws Exception {
    LogRecord record0 = record(10);
    assertThat(commitList.append(record0,false), is(true));
    // Test re-append
    assertThat(commitList.append(record0,false), is(false));

    // Test outside of range
    assertThat(commitList.append(record(21),false), is(false));

    commitList.close(10);
    for (LogRecord record : commitList) {
      assertThat(record, is(record0));
    }
  }
  
  @Test
  public void testLongSnapshot() throws Exception {
    CommitList current = commitList;
    long lsn = 10;
    while ( current == commitList ) {
      current = append(record(lsn++));
    }
    current = append(snapshot(lsn++));

    assertThat("make sure commitlist is closed", current.getEndLsn() == lsn-1);
  }  
  
  @Test
  public void testOneElementSync() throws Exception {
      final long time = System.currentTimeMillis();
      new Thread() {
            @Override
          public void run() {
            try {
                Thread.sleep(1);
            } catch ( InterruptedException ie ) {
                
            }
            commitList.append(record(10), true);
          }
      }.start();
    commitList.waitForContiguous();
    commitList.written();
    commitList.getWriteFuture().get();
    assertThat(System.currentTimeMillis()-time,lessThan(2000l));
  }

  @Test
  public void testBasicClose() throws Exception {
    LogRecord record0 = record(10);
    assertThat(commitList.append(record0,false), is(true));
    LogRecord record1 = record(11);
    assertThat(commitList.append(record1,false), is(true));

    assertThat(commitList.close(10), is(true));
    assertThat(commitList.isSyncRequested(), is(false));
    for (LogRecord record : commitList) {
      assertThat(record, is(record0));
    }
    
    LogRecord record2 = record(12);
    assertThat(commitList.append(record2,true), is(false));
    assertThat(commitList.isSyncRequested(), is(false));
   
    commitList.next().close(11);
    for (LogRecord record : commitList.next()) {
      assertThat(record, is(record1));
    }
  }

  @Test
  public void testWaitForContiguous() throws Exception {
    assertThat(commitList.append(record(15),false), is(true));
    assertThat(commitList.close(15), is(true));

    final AtomicReference<Exception> error = new AtomicReference<Exception>();
    final AtomicBoolean waitComplete = new AtomicBoolean(false);
    Thread waiter = new Thread() {
      @Override
      public void run() {
        try {
          commitList.waitForContiguous();
          waitComplete.set(true);
        } catch (Exception e) {
          error.set(e);
        }
      }
    };
    waiter.start();
    waiter.join(5 * 1000); // wait for the waiter to start waiting.
    assertThat(waitComplete.get(), is(false));

    for (int i = 16; i < 21; i++) {
      // Try appending a few records that land in the next link
      assertThat(commitList.append(record(i),false), is(false));
      assertThat(commitList.next().append(record(i),false), is(true));
    }

    waiter.join(5 * 1000); // Should still not be done waiting.
    assertThat(waitComplete.get(), is(false));

    for (int i = 10; i < 15; i++) {
      assertThat(commitList.append(record(i),false), is(true));
    }

    waiter.join(5 * 1000);
    assertThat(waitComplete.get(), is(true));
  }

  @Test
  public void testMultiThreadedAppendAndClose() throws Exception {
    ExecutorService executorService = Executors.newFixedThreadPool(20);
    Random r = new Random();
    final AtomicLong lsn = new AtomicLong(10);
    final List<Throwable> errors =
            Collections.synchronizedList(new ArrayList<Throwable>());
    for (int i = 0; i < 1000; i++) {
      if (r.nextInt(100) < 25) {
        executorService.submit(new Runnable() {
          @Override
          public void run() {
            try {
              append(record(lsn.incrementAndGet()));
            } catch (Throwable t) {
              errors.add(t);
            }
          }
        });
      } else {
        executorService.submit(new Runnable() {
          @Override
          public void run() {
            try {
              close(lsn.get());
            } catch (Throwable t) {
              errors.add(t);
            }
          }
        });
      }
    }
    assertThat(errors,Matchers.<Throwable>empty());
  }
  
  @Test
  public void testThrowingException() throws Exception {
      new Thread() {
          public void run() {
              commitList.exceptionThrown(new IOException());
          }
      }.start();
      
      try {
          commitList.getWriteFuture().get();
          fail();
      } catch ( ExecutionException ex ) {
          System.out.println("caught exception");
      } catch ( InterruptedException ie ) {
          
      }
  }
  
  private LogRecord snapshot(long lsn) {
    LogRecord record = mock(SnapshotRecord.class);
    when(record.getLsn()).thenReturn(lsn);
    return record;
  }  
  
  
  private LogRecord record(long lsn) {
    LogRecord record = mock(LogRecord.class);
    when(record.getLsn()).thenReturn(lsn);
    return record;
  }

  private CommitList append(LogRecord record) {
    CommitList l = commitList;
    while (!l.append(record,false)) {
      l = l.next();
    }
    return l;
  }

  private void close(long lsn) {
    CommitList l = commitList;
    while (!l.close(lsn)) {
      l = l.next();
    }
  }
  
  private static abstract class SnapshotRecord implements LogRecord, SnapshotRequest {
    
  }
}
