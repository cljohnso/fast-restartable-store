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
package com.terracottatech.frs;

import com.terracottatech.frs.config.FrsProperty;
import com.terracottatech.frs.io.nio.NIOConstants;
import com.terracottatech.frs.object.RegisterableObjectManager;
import com.terracottatech.frs.object.SimpleRestartableMap;
import com.terracottatech.frs.util.JUnitTestFolder;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Formatter;
import java.util.Properties;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author tim
 */
public abstract class OfflineCompactorTest {
  @Rule
  public JUnitTestFolder temporaryFolder = new JUnitTestFolder();

  public abstract Properties configure(Properties props);
  
  @Test
  public void testExistingOutputDirectory() throws Exception {
    File testFolder = temporaryFolder.newFolder();

    File uncompacted = new File(testFolder, "uncompacted");
    File compacted = new File(testFolder, "compacted");

    assertThat(uncompacted.mkdirs(), is(true));
    assertThat(compacted.mkdirs(), is(true));

    try {
      OfflineCompactor.main(
              new String[]{uncompacted.getAbsolutePath(), compacted.getAbsolutePath()});
      Assert.fail("Should have failed since output directory exists.");
    } catch (IOException e) {
      // expected
    }
  }

  @Test
  public void testMissingInputDirectory() throws Exception {
    File testFolder = temporaryFolder.newFolder();

    File uncompacted = new File(testFolder, "uncompacted");
    File compacted = new File(testFolder, "compacted");

    try {
      OfflineCompactor.main(
              new String[]{uncompacted.getAbsolutePath(), compacted.getAbsolutePath()});
      Assert.fail("Should have failed since source directory is missing.");
    } catch (IOException e) {
      // expected
    }
  }

  @Test
  public void testBasicCompaction() throws Exception {
    File testFolder = temporaryFolder.newFolder();

    File uncompacted = new File(testFolder, "uncompacted");
    File compacted = new File(testFolder, "compacted");

    Properties properties = configure(new Properties());
    properties.setProperty(FrsProperty.COMPACTOR_POLICY.shortName(),
                           "NoCompactionPolicy");
    properties.setProperty(FrsProperty.COMPACTOR_RUN_INTERVAL.shortName(), Integer.toString(Integer.MAX_VALUE));
    properties.setProperty(FrsProperty.COMPACTOR_START_THRESHOLD.shortName(), Integer.toString(Integer.MAX_VALUE));

    {
      assertThat(uncompacted.mkdirs(), is(true));
      RegisterableObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager =
              new RegisterableObjectManager<ByteBuffer, ByteBuffer, ByteBuffer>();
      RestartStore<ByteBuffer, ByteBuffer, ByteBuffer> uncompactedStore =
              RestartStoreFactory.createStore(
                      objectManager,
                      uncompacted, properties);
      SimpleRestartableMap map =
              new SimpleRestartableMap(0, uncompactedStore,
                                       false);
      objectManager.registerObject(map);

      uncompactedStore.startup().get();

      Random r = new Random();

      for (int i = 0; i < 100; i++) {
        for (int j = 0; j < 100; j++) {
          map.put(Integer.toString(j), Integer.toString(r.nextInt()));
        }
      }

      uncompactedStore.shutdown();
      assertThat(objectManager.size(), is(100L));
    }

    OfflineCompactor.main(new String[] { uncompacted.getAbsolutePath(), compacted.getAbsolutePath() });

    {
      RegisterableObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager =
              spy(new RegisterableObjectManager<ByteBuffer, ByteBuffer, ByteBuffer>());
      RestartStore<ByteBuffer, ByteBuffer, ByteBuffer> compactedStore =
              RestartStoreFactory.createStore(objectManager, compacted, properties);

      SimpleRestartableMap map =
              new SimpleRestartableMap(0, compactedStore,
                                       false);
      objectManager.registerObject(map);

      compactedStore.startup().get();

      RegisterableObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> uncompactedObjectManager =
              new RegisterableObjectManager<ByteBuffer, ByteBuffer, ByteBuffer>();
      RestartStore<ByteBuffer, ByteBuffer, ByteBuffer> uncompactedStore =
              RestartStoreFactory.createStore(
                      uncompactedObjectManager,
                      uncompacted, properties);
      SimpleRestartableMap uncompactedMap =
              new SimpleRestartableMap(0, uncompactedStore,
                                       false);
      uncompactedObjectManager.registerObject(uncompactedMap);

      uncompactedStore.startup().get();

      for (int i = 0; i < 100; i++) {
        assertThat(map.get(Integer.toString(i)), is(uncompactedMap.get(Integer.toString(i))));
      }

      compactedStore.shutdown();
      uncompactedStore.shutdown();

      assertThat(objectManager.size(), is(100L));
      verify(objectManager, times(100)).replayPut(any(ByteBuffer.class),
                                                  any(ByteBuffer.class), any(ByteBuffer.class),
                                                  anyLong());
        StringBuilder fn = new StringBuilder();
        Formatter pfn = new Formatter(fn);
  
        assertTrue("make sure first file exists", new File(compacted,pfn.format(NIOConstants.SEGMENT_NAME_FORMAT, 0).toString()).exists());
    }

    assertThat(FileUtils.sizeOfDirectory(compacted), lessThan(
            FileUtils.sizeOfDirectory(uncompacted)));
  }
}
