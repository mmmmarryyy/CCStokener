package org.eclipse.core.runtime.internal.stats;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * BundleStats is used to represent information about loaded bundle. A
 * bundlestats instance represents only one bundle.
 */
public class ResourceBundleStats {

    private String pluginId;

    private String fileName;

    private int keyCount = 0;

    private int keySize = 0;

    private int valueSize = 0;

    private long hashSize = 0;

    private long fileSize = 0;

    private static int sizeOf(String value) {
        return 44 + (2 * value.length());
    }

    private static int sizeOf(Properties value) {
        return (int) Math.round(44 + (16 + (value.size() * 1.25 * 4)) + (24 * value.size()));
    }

    public ResourceBundleStats(String pluginId, String fileName, URL input) {
        this.pluginId = pluginId;
        this.fileName = fileName;
        initialize(input);
    }

    public ResourceBundleStats(String pluginId, String fileName, ResourceBundle bundle) {
        this.pluginId = pluginId;
        this.fileName = fileName;
        initialize(bundle);
    }

    /**
	 * Compute the size of bundle
	 */
    private void initialize(ResourceBundle bundle) {
        for (Enumeration keys = bundle.getKeys(); keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            keySize += sizeOf(key);
            valueSize += sizeOf(bundle.getString(key));
            keyCount++;
        }
    }

    /**
	 * Compute the size of stream which represents a property file
	 */
    private void initialize(URL url) {
        InputStream stream = null;
        Properties props = new Properties();
        try {
            try {
                stream = url.openStream();
                fileSize = stream.available();
                props.load(stream);
                for (Iterator iter = props.keySet().iterator(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    keySize += sizeOf(key);
                    valueSize += sizeOf(props.getProperty(key));
                    keyCount++;
                }
                hashSize = sizeOf(props);
            } finally {
                if (stream != null) stream.close();
            }
        } catch (IOException e) {
        }
    }

    public long getHashSize() {
        return hashSize;
    }

    public int getKeyCount() {
        return keyCount;
    }

    public String getPluginId() {
        return pluginId;
    }

    public int getKeySize() {
        return keySize;
    }

    public int getValueSize() {
        return valueSize;
    }

    public long getTotalSize() {
        return keySize + valueSize + hashSize;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }
}
