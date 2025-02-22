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
package com.terracottatech.frs.transaction;

import com.terracottatech.frs.Disposable;
import com.terracottatech.frs.DisposableLifecycle;
import com.terracottatech.frs.GettableAction;
import com.terracottatech.frs.action.Action;
import com.terracottatech.frs.action.ActionCodec;
import com.terracottatech.frs.action.ActionFactory;
import com.terracottatech.frs.action.InvalidatingAction;
import com.terracottatech.frs.object.ObjectManager;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;

import static com.terracottatech.frs.util.ByteBufferUtils.concatenate;
import static com.terracottatech.frs.util.ByteBufferUtils.get;
import java.io.Closeable;
import java.io.IOException;

/**
 * @author tim
 */
class TransactionalAction implements TransactionAction, GettableAction {
  public static final ActionFactory<ByteBuffer, ByteBuffer, ByteBuffer> FACTORY =
          new ActionFactory<ByteBuffer, ByteBuffer, ByteBuffer>() {
            @Override
            public Action create(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager,
                                 ActionCodec codec, ByteBuffer[] buffers) {
              return new TransactionalAction(
                      TransactionHandleImpl.withByteBuffers(buffers), get(buffers), codec.decode(buffers));
            }
          };
  
  private static final byte COMMIT_BIT = 0x01;
  private static final byte BEGIN_BIT = 0x02;

  private final TransactionHandle handle;
  private final Action action;
  private final byte mode;
  private final TransactionLSNCallback callback;

  TransactionalAction(TransactionHandle handle, byte mode, Action action) {
    this.handle = handle;
    this.action = action;
    this.mode = mode;
    this.callback = null;
  }
  
  TransactionalAction(TransactionHandle handle, boolean begin, boolean commit, Action action, TransactionLSNCallback callback) {
    this.handle = handle;
    this.action = action;
    byte tempMode = 0;
    if (commit) {
      tempMode |= COMMIT_BIT;
    }
    if (begin) {
      tempMode |= BEGIN_BIT;
    }
    mode = tempMode;
    this.callback = callback;
  }

  @Override
  public boolean isCommit() {
    return (mode & COMMIT_BIT) != 0;
  }

  @Override
  public boolean isBegin() {
    return (mode & BEGIN_BIT) != 0;
  }

  public TransactionHandle getHandle() {
    return handle;
  }

  Action getAction() {
    return action;
  }

  @Override
  public ByteBuffer getIdentifier() {
    if ( action instanceof GettableAction ) {
      return ((GettableAction)action).getIdentifier();
    }
    return null;
  }

  @Override
  public ByteBuffer getKey() {
    if ( action instanceof GettableAction ) {
      return ((GettableAction)action).getKey();
    }
    return null;
  }

  @Override
  public ByteBuffer getValue() {
    if ( action instanceof GettableAction ) {
      return ((GettableAction)action).getValue();
    }
    return null;
  }
  

  @Override
  public long getLsn() {
    if ( action instanceof GettableAction ) {
      return ((GettableAction)action).getLsn();
    }
    return 0;
  }
    

  @Override
  public Set<Long> getInvalidatedLsns() {
    if (action instanceof InvalidatingAction) {
      return ((InvalidatingAction) action).getInvalidatedLsns();
    } else {
      return Collections.emptySet();
    }
  }
  
  @Override
  public void setDisposable(Closeable c) {
    if ( action instanceof DisposableLifecycle ) {
      ((DisposableLifecycle)action).setDisposable(c);
    } else {
      try {
        c.close();
      } catch ( IOException ioe ) {
        throw new RuntimeException(ioe);
      }
    }
  }

  @Override
  public void dispose() {
    if ( action instanceof Disposable ) {
      ((Disposable)action).dispose();
    }
  }

  @Override
  public void close() throws IOException {
    if ( action instanceof Closeable ) {
      ((Closeable)action).close();
    }
  }

  @Override
  public void record(long lsn) {
    assert callback != null;
    action.record(lsn);
    callback.setLsn(lsn);
  }

  @Override
  public void replay(long lsn) {
    action.replay(lsn);
  }

  @Override
  public int replayConcurrency() {
    return action.replayConcurrency();
  }

  @Override
  public ByteBuffer[] getPayload(ActionCodec codec) {
    ByteBuffer handleBuffer = handle.toByteBuffer();
    ByteBuffer header = ByteBuffer.allocate(handleBuffer.capacity() + 1);
    header.put(handleBuffer).put(mode).flip();
    return concatenate(header, codec.encode(action));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TransactionalAction that = (TransactionalAction) o;

    return handle.equals(that.handle) && action.equals(that.action) && mode == that.mode;
  }

  @Override
  public int hashCode() {
    int result = handle != null ? handle.hashCode() : 0;
    result = 31 * result + (action != null ? action.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TransactionalAction{" +
            "handle=" + handle +
            ", action=" + action +
            ", mode=" + mode +
            '}';
  }
}
