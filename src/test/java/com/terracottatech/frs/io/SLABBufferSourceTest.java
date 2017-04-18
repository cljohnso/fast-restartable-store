/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */

package com.terracottatech.frs.io;

import java.nio.ByteBuffer;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.number.OrderingComparison.*;


/**
 *
 * @author mscott
 */
public class SLABBufferSourceTest extends BufferSourceTest {

  private static final int SLABSIZE = 8*1024*1024;
  SLABBufferSource src = new SLABBufferSource(SLABSIZE, 1);

  @After
  public void tearDown() throws Exception {
  }

  @Override
  public BufferSource getBufferSource() {
    return src;
  }

  /**
   * Test of getSlabSize method, of class SLABBufferSource.
   */
  @Test
  public void testGetSlabSize() {
    System.out.println("getSlabSize");
    SLABBufferSource instance = (SLABBufferSource)getBufferSource();
    int result = instance.getSlabSize();
    assertEquals(SLABSIZE, result);
  }

  /**
   * Test of getSize method, of class SLABBufferSource.
   */
  @Test
  public void testGetSize() {
    System.out.println("getSize");
    SLABBufferSource instance = (SLABBufferSource)getBufferSource();
    int expResult = 0;
    int result = instance.getSize();
    assertEquals(expResult, result);
    int size = 1023;
    ByteBuffer buf = instance.getBuffer(size);
    assertEquals(SLABSIZE, instance.getSize());
  }

  /**
   * Test of verify method, of class SLABBufferSource.
   */
  @Test
  public void testVerify() {
    System.out.println("verify");
    SLABBufferSource instance = new SLABBufferSource();
    int expResult = 0;
    int result = instance.verify();
    assertEquals(expResult, result);
    
    int size = 1023;
    ByteBuffer buf = instance.getBuffer(size);
    assertEquals(1, instance.verify());
  }

  /**
   * Test of getBuffer method, of class SLABBufferSource.
   */
  @Test
  @Override
  public void testGetBuffer() {
    super.testGetBuffer();
    System.out.println("getBuffer");
    int size = 1023;
    SLABBufferSource instance = new SLABBufferSource();
    ByteBuffer result = instance.getBuffer(size);
    assertThat(result.capacity(), greaterThanOrEqualTo(size));
    assertThat(result.remaining(), greaterThanOrEqualTo(size));
  }

  /**
   * Test of getBuffer method, of class SLABBufferSource.
   */
  @Test
  public void testGetBufferWithSmallLeftOverPerSlab() {
    System.out.println("getBufferLoop");
    int size = 7153746;
    SLABBufferSource instance = new SLABBufferSource();
    for (int i = 0; i < 128; i++) {
      ByteBuffer result = instance.getBuffer(size);
      assertThat(result.capacity(), greaterThanOrEqualTo(size));
      assertThat(result.remaining(), greaterThanOrEqualTo(size));
    }
    // TDB-421 this should return null instead of looping
    ByteBuffer result = instance.getBuffer(size);
    assertNull(result);
  }


  /**
   * Test of returnBuffer method, of class SLABBufferSource.
   */
  @Test
  @Override
  public void testReturnBuffer() {
    super.testReturnBuffer();
    System.out.println("returnBuffer");
    SLABBufferSource instance = new SLABBufferSource();
    int expResult = 0;
    int result = instance.verify();
    assertEquals(expResult, result);
    
    int size = 1023;
    ByteBuffer buf = instance.getBuffer(size);
    assertEquals(1, instance.verify());
    instance.returnBuffer(buf);
    assertEquals(0, instance.verify());
  }

  /**
   * Test of reclaim method, of class SLABBufferSource.
   */
  @Test
  @Override
  public void testReclaim() {
    super.testReclaim();
    System.out.println("reclaim");
    SLABBufferSource instance = new SLABBufferSource();
    int expResult = 0;
    int result = instance.verify();
    assertEquals(expResult, result);
    
    int size = 1023;
    ByteBuffer buf = instance.getBuffer(size);
    assertEquals(1, instance.verify());
    instance.reclaim();
    assertEquals(0, instance.verify());
  }
  
}
