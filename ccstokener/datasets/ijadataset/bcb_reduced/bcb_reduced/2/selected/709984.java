package com.google.gwt.dev.shell;

import com.google.gwt.dev.util.Util;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Provides various strategies for emma integration based on runtime detection.
 */
abstract class EmmaStrategy {

    private static class NoEmmaStrategy extends EmmaStrategy {

        public byte[] getEmmaClassBytes(byte[] classBytes, String slashedName, long unitLastModified) {
            return classBytes;
        }
    }

    private static class PreinstrumentedEmmaStrategy extends EmmaStrategy {

        public byte[] getEmmaClassBytes(byte[] classBytes, String slashedName, long unitLastModified) {
            URL url = Thread.currentThread().getContextClassLoader().getResource(slashedName + ".class");
            if (url != null) {
                try {
                    URLConnection conn = url.openConnection();
                    if (conn.getLastModified() >= unitLastModified) {
                        byte[] result = Util.readURLConnectionAsBytes(conn);
                        if (result != null) {
                            return result;
                        }
                    }
                } catch (IOException ignored) {
                }
            }
            return classBytes;
        }
    }

    /**
   * Classname for Emma's RT, to enable bridging.
   */
    public static final String EMMA_RT_CLASSNAME = "com.vladium.emma.rt.RT";

    /**
   * Gets the emma classloading strategy.
   */
    public static EmmaStrategy get(boolean emmaIsAvailable) {
        if (!emmaIsAvailable) {
            return new NoEmmaStrategy();
        } else {
            return new PreinstrumentedEmmaStrategy();
        }
    }

    public abstract byte[] getEmmaClassBytes(byte[] classBytes, String slashedName, long unitLastModified);
}
