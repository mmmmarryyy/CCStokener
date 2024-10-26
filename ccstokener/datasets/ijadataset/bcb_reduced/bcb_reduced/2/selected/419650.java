package com.gorillalogic.config;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URLConnection;
import java.util.Date;
import java.util.jar.*;
import org.apache.log4j.Logger;

public class TB {

    private static Logger logger = Logger.getLogger(TB.class);

    private static final String LICENSE_MARKER_FILE = "lm";

    private static final String LICENSE_MARKER_DIR = "resource_defaults";

    private static final String LICENSE_MARKER_PATH = "bin/gorilla/WEB-INF/classes/" + LICENSE_MARKER_DIR + LICENSE_MARKER_FILE;

    private static final long LICENSE_DAYS = 30;

    private static final long MS_IN_ONE_DAY = 1000L * 60L * 60L * 24L;

    private static final long LICENSE_MS = LICENSE_DAYS * MS_IN_ONE_DAY;

    /** Creates a new instance of TB */
    private TB() {
    }

    public static boolean tb() throws ConfigurationException {
        InputStream lmIn = Resources.getResourceAsStream(LICENSE_MARKER_FILE);
        if (lmIn != null) {
            LineNumberReader in = null;
            try {
                in = new LineNumberReader(new InputStreamReader(lmIn));
                String line = in.readLine();
                if (line != null) {
                    if (isValidKey(line.trim())) {
                        return true;
                    } else {
                        long tagDate = getTagDate();
                        long curDate = new Date().getTime();
                        if (curDate > tagDate) {
                            long dateDiff = curDate - tagDate;
                            if (dateDiff < LICENSE_MS) {
                                long remaining = (LICENSE_MS - dateDiff) / MS_IN_ONE_DAY + 1;
                                Bootstrap.logger.info("trial license, " + remaining + " days remaining");
                                return true;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new ConfigurationException("cannot read license information");
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new ConfigurationException("error closing license file");
                    }
                }
            }
        } else {
            throw new ConfigurationException("cannot find license file, contact Gorilla Logic tech support");
        }
        return false;
    }

    private static long getTagDate() throws ConfigurationException {
        long tagDate = 0;
        boolean found = false;
        java.net.URL url = Resources.getResource(LICENSE_MARKER_FILE);
        if (url != null) {
            try {
                URLConnection conn;
                conn = url.openConnection();
                if (conn instanceof JarURLConnection) {
                    JarURLConnection juc = (JarURLConnection) conn;
                    JarEntry tagEntry = juc.getJarEntry();
                    if (tagEntry != null) {
                        found = true;
                        long entryDate = tagEntry.getTime();
                        if (entryDate != -1) {
                            found = true;
                            tagDate = entryDate;
                        } else {
                            throw new TBException("cannot read license information from jar file");
                        }
                    }
                } else if (conn.getClass().getName().indexOf("eclipse") != -1) {
                    return new Date().getTime() - 1000000L;
                } else {
                    String fileName = url.getFile();
                    fileName = fileName.replaceAll("%20", " ");
                    File tagFile = new File(fileName);
                    if (tagFile.exists()) {
                        found = true;
                        tagDate = tagFile.lastModified();
                    } else {
                        throw new TBException("cannot read license information from file");
                    }
                }
            } catch (IOException e) {
                logger.debug(e);
            }
        }
        if (!found) {
            throw new TBException("cannot find license information");
        }
        return tagDate;
    }

    private static final String LICENSE_FULL = "ullyfayicensedlayopycay";

    private static boolean isValidKey(String key) {
        boolean rez = false;
        if (key != null) {
            rez = key.equals(LICENSE_FULL);
        }
        return rez;
    }
}
