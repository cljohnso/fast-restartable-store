/*
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.terracottatech.frs;

import com.terracottatech.frs.action.Action;
import com.terracottatech.frs.action.ActionCodec;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author mscott
 */
public class DisposableAction implements Action, Disposable {
    
    private final Action delegate;
    private final Disposable disposable;

    public DisposableAction(Action a, Disposable dispose) {
        delegate = a;
        disposable = dispose;
    }

    @Override
    public void dispose() {
        disposable.dispose();
    }

    @Override
    public void close() throws IOException {
        disposable.dispose();
    }

    @Override
    public void record(long lsn) {
        delegate.record(lsn);
    }

    @Override
    public void replay(long lsn) {
        delegate.replay(lsn);
    }

    @Override
    public ByteBuffer[] getPayload(ActionCodec codec) {
        return delegate.getPayload(codec);
    }
    
    protected Action getAction() {
        return delegate;
    }
    
}
