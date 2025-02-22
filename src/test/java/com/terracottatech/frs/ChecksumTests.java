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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import org.junit.*;

/**
 *
 * @author mscott
 */
public class ChecksumTests {
    
    StringBuilder buf = new StringBuilder();
    String format = "%s time: %.5f\n";
    Formatter f = new Formatter(buf);
    byte[] meg = new byte[1 * 1024 * 1024];
    
    public ChecksumTests() {

    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void seed() {
        for (int x=0;x<meg.length;x++) {
            meg[x] = (byte)(0xff & (int)(Math.random() * 128));
        }
    }
    
    @Test @Ignore
    public void testMD5() {
        long n = System.nanoTime();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            for (int x=0;x<100;x++) md5.update(meg);
            md5.digest();
            f.format(format,"MD5",(System.nanoTime()-n)/1e9);
        } catch ( NoSuchAlgorithmException no ) {
            
        }
    }
    
    @Test @Ignore
    public void testSHA1() {
        long n = System.nanoTime();
        try {
            MessageDigest md5 = MessageDigest.getInstance("SHA1");
            for (int x=0;x<100;x++) md5.update(meg);
            md5.digest();
            f.format(format,"SHA1",(System.nanoTime()-n)/1e9);
        } catch ( NoSuchAlgorithmException no ) {
            
        }
    }
    
    @Test @Ignore
    public void testAdler32() {
        long n = System.nanoTime();
        Adler32 tr = new Adler32();
        for (int x=0;x<100;x++) tr.update(meg);
        tr.getValue();
        f.format(format,"Adler32",(System.nanoTime()-n)/1e9);
    }
    
    @Test @Ignore
    public void testCRC32() {
        long n = System.nanoTime();
        CRC32 tr = new CRC32();
        for (int x=0;x<100;x++) tr.update(meg);
        tr.getValue();
        f.format(format,"CRC32",(System.nanoTime()-n)/1e9);
    }
    
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
        System.out.println(buf.toString());
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
