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
package com.terracottatech.frs.mock.recovery;

import com.terracottatech.frs.recovery.Filter;
import com.terracottatech.frs.action.Action;

/**
 *
 * @author cdennis
 */
class MockReplayFilter implements Filter<Action> {

  @Override
  public boolean filter(Action element, long lsn, boolean filtered) {
    if (filtered) {
      return false;
    } else {
      element.replay(lsn);
      return true;
    }
  }
  
}
