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
package com.terracottatech.frs.io.nio;

import com.terracottatech.frs.Constants;
import com.terracottatech.frs.io.Chunk;
import com.terracottatech.frs.io.IOManager;
import com.terracottatech.frs.io.WrappingChunk;
import com.terracottatech.frs.util.JUnitTestFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import org.junit.*;

/**
 *
 * @author mscott
 */
public class NIOSpeedTest {
  private static final long MAX_SEGMENT_SIZE = 10 * 1024 * 1024;
  NIOStreamImpl stream;
    
  @Rule
  public JUnitTestFolder folder = new JUnitTestFolder();

  @Before
  public void setUp() throws Exception {
    stream = new NIOStreamImpl(folder.getRoot(), MAX_SEGMENT_SIZE);
        stream.setMinimumMarker(Constants.FIRST_LSN);
  }
  
  public NIOSpeedTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    
    @Test @Ignore
    public void nullSpeed() throws IOException {
        File nf = new File("/dev/null");
        FileChannel fc = new FileOutputStream(nf).getChannel();
        ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);
        long w = 0;
        long n = System.nanoTime();
        while ( w < 128L * 1024 * 1024 ) {
            buf.position(0);
            w += fc.write(buf);
            try {
              fc.force(false);
            } catch (IOException e) {
              System.out.format("fsync on /dev/null threw %s [was this run on linux?]\n", e.toString());
            }
        }
        long r = w;
        n = System.nanoTime() - n;
        System.out.format("write null %d MB in %.6f sec %.2f MB/s\n",w/(1024*1024),n/1e9,(w/(1024*1024))/(n/1e9));
        while ( r > 0 ) {
            buf.position(0);
            r -= fc.read(buf);
        }
        n = System.nanoTime() - n;
        System.out.format("read null %d MB in %.6f sec %.2f MB/s\n",w/(1024*1024),n/1e9,(w/(1024*1024))/(n/1e9));
     }    
    
    @Test @Ignore
    public void rawSpeed() throws IOException {
        File nf = folder.newFile();
        FileChannel fc = new FileOutputStream(nf).getChannel();
        ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);
        long w = 0;
        long n = System.nanoTime();
        while ( w < 128L * 1024 * 1024 ) {
            buf.position(0);
            w += fc.write(buf);
//            fc.force(false);
        }
        fc.close();
        long r = w;
        n = System.nanoTime() - n;
        System.out.format("write raw %d MB in %.6f sec %.2f MB/s\n",w/(1024*1024),n/1e9,(w/(1024*1024))/(n/1e9));
     }
    
    @Test @Ignore
    public void speedTest() throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(512 * 1024);
        Chunk c = new WrappingChunk(buf);
        long w = 0;
        long n = System.nanoTime();
        long marker = Constants.FIRST_LSN;
        while ( w < 128L * 1024 * 1024 ) {
            buf.position(0);
            w += stream.append(c,marker+=100);
//            stream.sync();
        }
        stream.seek(IOManager.Seek.BEGINNING.getValue());
        long r = w;
        n = System.nanoTime() - n;
        System.out.format("write frs %d MB in %.6f sec %.2f MB/s\n",w/(1024*1024),n/1e9,(w/(1024*1024))/(n/1e9));
    }
    
     
    @Test @Ignore
    public void speedTiny() throws IOException {
        File nf = folder.newFile();
        FileChannel fc = new FileOutputStream(nf).getChannel();
        ArrayList<ByteBuffer> fd = new ArrayList<ByteBuffer>();
        for(int x=0;x<1024;x++) {
            fd.add(ByteBuffer.allocateDirect(32));
        }
        
        Chunk c = new WrappingChunk(fd.toArray(new ByteBuffer[fd.size()]));
        
        long w = 0;
        long n = System.nanoTime();
        while ( w < 128L * 1024 * 1024 ) {
            while ( c.hasRemaining() ) {
                w += fc.write(c.getBuffers());
            }
            c.flip();
        }
        fc.position(0);
        long r = w;
        n = System.nanoTime() - n;
        System.out.format("write tiny %d MB in %.6f sec %.2f MB/s\n",w/(1024*1024),n/1e9,(w/(1024*1024))/(n/1e9));
    }   
    
    @After
    public void tearDown() {
    }

}
