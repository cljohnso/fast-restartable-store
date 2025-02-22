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
package com.terracottatech.frs.mock.transaction;

import com.terracottatech.frs.action.Action;
import com.terracottatech.frs.action.ActionManager;
import com.terracottatech.frs.transaction.TransactionHandle;
import com.terracottatech.frs.transaction.TransactionManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

/**
 *
 * @author cdennis
 */
public class MockTransactionManager implements TransactionManager {

  private final AtomicLong txnId = new AtomicLong();
  
  private final ActionManager rcdManager;
  
  private final Map<TransactionHandle, Collection<Lock>> heldLocks = new ConcurrentHashMap<TransactionHandle, Collection<Lock>>();
  
  public MockTransactionManager(ActionManager rcdManager) {
    this.rcdManager = rcdManager;
  }

  @Override
  public TransactionHandle begin() {
    long id = txnId.getAndIncrement();
    rcdManager.happened(new MockTransactionBeginAction(id));
    TransactionHandle handle = new MockTransactionHandle(id);
    heldLocks.put(handle, new ArrayList<Lock>());
    return handle;
  }

  @Override
  public void commit(TransactionHandle handle, boolean synchronous) {
    Future<Void> f = rcdManager.happened(new MockTransactionCommitAction(getIdAndValidateHandle(handle)));
    try {
      f.get();
    } catch (InterruptedException ex) {
      throw new AssertionError(ex);
    } catch (ExecutionException ex) {
      throw new AssertionError(ex);
    } finally {
      for (Lock l : heldLocks.remove(handle)) {
        l.unlock();
      }
    }
  }

  @Override
  public void happened(TransactionHandle handle, Action action) {
    rcdManager.happened(new MockTransactionalAction(getIdAndValidateHandle(handle), action));
  }

  private long getIdAndValidateHandle(TransactionHandle handle) {
    if (handle instanceof MockTransactionHandle) {
      MockTransactionHandle mth = (MockTransactionHandle) handle;
      return mth.getId(this);
    } else {
      throw new IllegalArgumentException("Not one of our handles");
    }
  }
  
  private class MockTransactionHandle implements TransactionHandle {

    private final long id;
    
    public MockTransactionHandle(long id) {
      this.id = id;
    }

    @Override
    public ByteBuffer toByteBuffer() {
      return null;
    }

    private long getId(MockTransactionManager asker) {
      if (MockTransactionManager.this != asker) {
        throw new IllegalArgumentException("Not my owner");
      } else {
        return id;
      }
    }
  }

  @Override
  public long getLowestOpenTransactionLsn() {
    return Long.MAX_VALUE;
  }
}
