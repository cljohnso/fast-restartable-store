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
package com.terracottatech.frs.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author mscott
 */
public class TimebombFilter extends BufferFilter {
    
    private final long start = System.currentTimeMillis();
    private final long explode;
    private boolean exploded = false;
    
    public TimebombFilter(long time, TimeUnit units) {
        explode = start + units.toMillis(time);
    }

    @Override
    public boolean filter(ByteBuffer buffer) throws IOException {
        final Thread hit = Thread.currentThread();
        if ( System.currentTimeMillis() > explode && !exploded) {
            exploded = true;
            new Thread() {
                public void run() {
                    hit.interrupt();
                }
            }.start();
            return true;
        }
        return true;
    }
    
}
