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

import com.terracottatech.frs.action.Action;
import com.terracottatech.frs.action.ActionCodec;
import com.terracottatech.frs.action.ActionFactory;
import com.terracottatech.frs.object.ObjectManager;
import com.terracottatech.frs.util.ByteBufferUtils;

import java.nio.ByteBuffer;

/**
 * @author tim
 */
class TransactionCommitAction implements TransactionAction {
  public static final ActionFactory<ByteBuffer, ByteBuffer, ByteBuffer> FACTORY =
          new ActionFactory<ByteBuffer, ByteBuffer, ByteBuffer>() {
            @Override
            public Action create(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager,
                                 ActionCodec codec, ByteBuffer[] buffers) {
              TransactionHandle handle = TransactionHandleImpl.withByteBuffers(buffers);
              boolean emptyTransaction = ByteBufferUtils.get(buffers) == 1;
              return new TransactionCommitAction(handle, emptyTransaction);
            }
          };

  private final TransactionHandle handle;
  private final boolean emptyTransaction;

  TransactionCommitAction(TransactionHandle handle, boolean emptyTransaction) {
    this.handle = handle;
    this.emptyTransaction = emptyTransaction;
  }

  @Override
  public TransactionHandle getHandle() {
    return handle;
  }

  @Override
  public boolean isCommit() {
    return true;
  }

  @Override
  public boolean isBegin() {
    return emptyTransaction;
  }

  @Override
  public void record(long lsn) {

  }

  @Override
  public void replay(long lsn) {
  }

  @Override
  public ByteBuffer[] getPayload(ActionCodec codec) {
    ByteBuffer handleBuffer = handle.toByteBuffer();
    ByteBuffer header = ByteBuffer.allocate(1);
    if (emptyTransaction) {
      header.put((byte) 1);
    } else {
      header.put((byte) 0);
    }
    header.flip();
    return new ByteBuffer[]{handleBuffer, header};
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TransactionCommitAction that = (TransactionCommitAction) o;

    return handle.equals(that.getHandle()) && emptyTransaction == that.emptyTransaction;
  }

  @Override
  public int hashCode() {
    return handle != null ? handle.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "TransactionCommitAction{" +
            "handle=" + handle +
            '}';
  }
}
