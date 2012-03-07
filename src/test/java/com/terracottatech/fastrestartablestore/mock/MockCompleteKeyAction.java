/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.terracottatech.fastrestartablestore.mock;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.terracottatech.fastrestartablestore.CompleteKey;
import com.terracottatech.fastrestartablestore.TransactionLockProvider;
import com.terracottatech.fastrestartablestore.messages.Action;

/**
 *
 * @author cdennis
 */
public abstract class MockCompleteKeyAction<I, K> implements Action, Serializable {
  private final I id;
  private final K key;
  
  public MockCompleteKeyAction() {
    // Get rid of this
    id = null;
    key = null;
  }
  
  public MockCompleteKeyAction(CompleteKey<I, K> completeKey) {
    this(completeKey.getId(), completeKey.getKey());
  }
  
  public MockCompleteKeyAction(I id, K key) {
    this.id = id;
    this.key = key;
  }
  
  protected I getId() {
    return id;
  }

  protected K getKey() {
    return key;
  }

  protected final ReadWriteLock getLock(TransactionLockProvider locks) {
    return locks.getLockForKey(getId(), getKey());
  }
  
  @Override
  public Collection<Lock> lock(TransactionLockProvider locks) {
    Lock lock = getLock(locks).writeLock();
    lock.lock();
    return Collections.singleton(lock);
  }
}
