package net.sf.jpim.util;

import java.io.DataInputStream;
import java.net.URL;
import java.rmi.server.UID;
import java.util.Random;

/**
 * Utility class exposing a method that will return
 * a unique identifier.
 *
 * @author Dieter Wimberger
 * @version 0.9.6 19/11/2002
 */
public class UIDGenerator {

    private static Random m_Random;

    private static int m_ReseedCounter = 0;

    static {
        m_Random = new Random();
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
        m_Random.nextBytes(buffer);
        u = u + new String(buffer);
        if (m_ReseedCounter == RANDOM_RESEED) {
            seedRandom();
            m_ReseedCounter = 0;
        } else {
            m_ReseedCounter++;
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
        try {
            URL url = new URL(HOTBITS_URL);
            DataInputStream din = new DataInputStream(url.openStream());
            m_Random.setSeed(din.readLong());
            din.close();
        } catch (Exception ex) {
            m_Random.setSeed(System.currentTimeMillis());
        }
    }

    public static final void main(String[] args) {
        int i = 0;
        long start = System.currentTimeMillis();
        while (i < 1000) {
            i++;
        }
        long stop = System.currentTimeMillis();
    }

    public static final int RANDOM_PADDING = 256;

    public static final int RANDOM_SEED_LENGTH = 6;

    public static final int RANDOM_RESEED = 1000;

    public static final String HOTBITS_URL = "http://www.fourmilab.ch/cgi-bin/uncgi/Hotbits?nbytes=" + RANDOM_SEED_LENGTH + "&fmt=bin";
}
