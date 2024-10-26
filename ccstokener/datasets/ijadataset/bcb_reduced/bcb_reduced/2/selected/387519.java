package net.wimpi.pim.util;

import java.io.DataInputStream;
import java.net.URL;
import java.rmi.server.UID;
import java.security.SecureRandom;

/**
 * Utility class exposing a method that will return
 * a unique identifier.
 *
 * @author Dieter Wimberger
 * @version @version@ (@date@)
 */
public class UIDGenerator {

    private static SecureRandom c_Random;

    private static SecureRandom c_SeedRandom;

    private static int c_ReseedCounter = 0;

    private static boolean c_SeedWithHotBits = false;

    static {
        c_Random = new SecureRandom();
        c_SeedRandom = new SecureRandom();
        c_SeedWithHotBits = new Boolean(System.getProperty("jpim.uidgen.hotbits", "false")).booleanValue();
        seedRandom();
    }

    /**
   * Returns a UID (unique identifier) as <tt>String</tt>.
   * The identifier represents the MD5 hashed combination
   * of a <tt>java.rmi.server.UID</tt> instance, a random padding of
   * <tt>RANDOM_PADDING</tt> length, it's identity hashcode and
   * <tt>System.currentTimeMillis()</tt>.
   *
   * @return the UID as <tt>String</tt>.
   */
    public static final synchronized String getUID() {
        byte[] buffer = new byte[RANDOM_PADDING];
        String u = new UID().toString();
        int i = System.identityHashCode(u);
        long d = System.currentTimeMillis();
        c_Random.nextBytes(buffer);
        u = u + new String(buffer);
        if (c_ReseedCounter == RANDOM_RESEED) {
            seedRandom();
            c_ReseedCounter = 0;
        } else {
            c_ReseedCounter++;
        }
        return MD5.hash(u + i + d);
    }

    /**
   * If the <tt>HotBits</tt> Server is available, <tt>Random</tt>
   * will be seeded with a real random long.
   * <p>
   * <a href="http://www.fourmilab.ch/hotbits/">HotBits</a> is located
   * at Fermilab, Switzerland.
   *
   */
    public static final void seedRandom() {
        if (c_SeedWithHotBits) {
            try {
                URL url = new URL(HOTBITS_URL);
                DataInputStream din = new DataInputStream(url.openStream());
                c_Random.setSeed(din.readLong());
                din.close();
            } catch (Exception ex) {
                c_Random.setSeed(c_SeedRandom.getSeed(8));
            }
        } else {
            c_Random.setSeed(c_SeedRandom.getSeed(8));
        }
    }

    public static final void main(String[] args) {
        int i = 0;
        System.out.println("Seed with Hotbits = " + c_SeedWithHotBits);
        long start = System.currentTimeMillis();
        while (i < 1000) {
            System.out.println(getUID());
            i++;
        }
        long stop = System.currentTimeMillis();
        System.out.println("Time =" + (stop - start) + "[ms]");
    }

    public static final int RANDOM_PADDING = 256;

    public static final int RANDOM_SEED_LENGTH = 6;

    public static final int RANDOM_RESEED = 1000;

    public static final String HOTBITS_URL = "http://www.fourmilab.ch/cgi-bin/uncgi/Hotbits?nbytes=" + RANDOM_SEED_LENGTH + "&fmt=bin";
}
