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
import com.terracottatech.frs.object.RegisterableObjectManager;
import com.terracottatech.frs.object.SimpleRestartableMap;
import com.terracottatech.frs.util.JUnitTestFolder;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * @author tim
 */
public class TryForceCompaction {
  @Rule
  public JUnitTestFolder testFolder = new JUnitTestFolder();

  @Test
  public void testForcingCompaction() throws Exception {
    File frsFolder = testFolder.newFolder();

    Properties properties = new Properties();
    properties.setProperty(FrsProperty.IO_NIO_SEGMENT_SIZE.shortName(), "102400");
    properties.setProperty(FrsProperty.COMPACTOR_START_THRESHOLD.shortName(), "1000000000");

    RegisterableObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager =
            new RegisterableObjectManager<ByteBuffer, ByteBuffer, ByteBuffer>();

    RestartStore<ByteBuffer, ByteBuffer, ByteBuffer> restartStore =
            RestartStoreFactory.createStore(objectManager, frsFolder, properties);

    SimpleRestartableMap map1 =
            new SimpleRestartableMap(0, restartStore, false);
    objectManager.registerObject(map1);

    SimpleRestartableMap map2 =
            new SimpleRestartableMap(1, restartStore, false);
    objectManager.registerObject(map2);

    restartStore.startup().get();

    String data = new String(new byte[100]);

    for (int i = 0; i < 10000; i++) {
      map1.put(Integer.toString(i), data);
    }

    Thread.sleep(10 * 1000);

    System.out.println("Number of files before " + frsFolder.list().length);

    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10000; j++) {
        map1.put(Integer.toString(j), data);
      }
    }

    for (int i = 0; i < 10; i++) {
      System.out.println("Attempting to force a compaction");
      for (int j = 0; j < 1000; j++) {
        map2.put(Integer.toString(j), data);
      }
      map2.clear();

      Thread.sleep(10 * 1000);

      System.out.println("Number of files after " + frsFolder.list().length);
    }

    map1.clear();
    Thread.sleep(10 * 1000);
    restartStore.shutdown();
  }
}
