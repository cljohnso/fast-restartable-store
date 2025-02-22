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

import com.terracottatech.frs.action.ActionCodec;

import java.nio.ByteBuffer;

/**
 * @author tim
 */
public abstract class TransactionActions {
  public static void registerActions(int id, ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> codec) {
    codec.registerAction(id, 0, TransactionalAction.class, TransactionalAction.FACTORY);
    codec.registerAction(id, 1, TransactionCommitAction.class, TransactionCommitAction.FACTORY);
  }
}
