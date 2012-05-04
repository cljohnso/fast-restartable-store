/*
 * All content copyright (c) 2012 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs;

import com.terracottatech.frs.action.ActionCodec;
import com.terracottatech.frs.action.ActionCodecImpl;
import com.terracottatech.frs.action.ActionManager;
import com.terracottatech.frs.action.ActionManagerImpl;
import com.terracottatech.frs.compaction.CompactionActions;
import com.terracottatech.frs.io.IOManager;
import com.terracottatech.frs.io.nio.NIOManager;
import com.terracottatech.frs.log.LogManager;
import com.terracottatech.frs.log.MasterLogRecordFactory;
import com.terracottatech.frs.log.StagingLogManager;
import com.terracottatech.frs.object.ObjectManager;
import com.terracottatech.frs.transaction.TransactionActions;
import com.terracottatech.frs.transaction.TransactionManager;
import com.terracottatech.frs.transaction.TransactionManagerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author tim
 */
public abstract class RestartStoreFactory {
  private RestartStoreFactory() {
  }

  private static ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> createCodec(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager) {
    ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> codec =
            new ActionCodecImpl<ByteBuffer, ByteBuffer, ByteBuffer>(objectManager);
    MapActions.registerActions(0, codec);
    TransactionActions.registerActions(1, codec);
    CompactionActions.registerActions(2, codec);
    return codec;
  }

  public static RestartStore<ByteBuffer, ByteBuffer, ByteBuffer> createStore(
          ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager, File dbHome,
          boolean synchronousWrites, long fileSize) throws
          IOException {
    IOManager ioManager = new NIOManager(dbHome.getAbsolutePath(), fileSize);
    LogManager logManager = new StagingLogManager(ioManager);
    ActionManager actionManager = new ActionManagerImpl(logManager, objectManager,
                                                        createCodec(objectManager) ,
                                                        new MasterLogRecordFactory());
    TransactionManager transactionManager = new TransactionManagerImpl(actionManager, synchronousWrites);
    return new RestartStoreImpl(objectManager, transactionManager, logManager, actionManager);
  }
}