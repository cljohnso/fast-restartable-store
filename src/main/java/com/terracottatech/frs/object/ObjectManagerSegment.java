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
package com.terracottatech.frs.object;

/**
 *
 * @author Chris Dennis
 */
public interface ObjectManagerSegment<I, K, V> {

  ObjectManagerEntry<I, K, V> acquireCompactionEntry(long ceilingLsn);

  void updateLsn(int hash, ObjectManagerEntry<I, K, V> entry, long newLsn);

  void releaseCompactionEntry(ObjectManagerEntry<I, K, V> entry);
  
  Long getLowestLsn();
  
  Long getLsn(int hash, K key);
  
  void put(int hash, K key, V value, long lsn);
  
  void replayPut(int hash, K key, V value, long lsn);
  
  void remove(int hash, K key);

  long size();

  long sizeInBytes();
}
