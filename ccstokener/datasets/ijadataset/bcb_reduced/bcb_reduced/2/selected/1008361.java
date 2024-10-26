package net.sf.ashkay.strategies;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import net.sf.ashkay.CacheEntry;
import net.sf.ashkay.CachingStrategy;

/**
 * LastModifiedCachingStrategy attempts to locate a timestamped resource and check the last time that resource was modified.
 * This is a tricky thing to do, and thusly, at the current time, LastModifiedCachingStrategy only works with Files and URLs.
 * Since we can't guarantee the entry value will remain around, LastModifiedCachingStrategy only works with the entry key as
 * well. So, LastModifiedCachingStrategy will work if you are looking up an object from the cache based on a file name or url
 * key. It will work with keys that are of type java.io.File, java.net.URL and java.lang.String. If the key is a string,
 * LastModifiedCachingStrategy will try to turn it first into a File and if that fails, a URL. Obviously, for URLs, your
 * java.net stuff needs to be working (ie. the network is connected).
 */
public class LastModifiedCachingStrategy implements CachingStrategy {

    private static final String PROPERTY_MODIFIED_TIME = LastModifiedCachingStrategy.class.getName() + ".fileModified";

    /**
	 * Prepares the cache entry for caching with this strategy.<br>
	 * <b>NOTE:</b> Be Careful: a caching strategy <em>is</em> allowed to return
	 * a different CacheEntry, so make sure that you store the results of this
	 * method and don't assume the symantics of pass by reference.
	 */
    public CacheEntry prepare(CacheEntry entry) {
        long modifiedTime = getLastModifiedTime(entry);
        entry.addProperty(PROPERTY_MODIFIED_TIME, new Long(modifiedTime));
        return entry;
    }

    /**
	 * Validates this cache entry for this caching strategy.
	 */
    public boolean validate(CacheEntry entry) {
        boolean val = true;
        Long entry_modified = (Long) entry.getProperty(PROPERTY_MODIFIED_TIME);
        long file_modified = 0;
        file_modified = getLastModifiedTime(entry);
        if (entry_modified != null) {
            if (file_modified != entry_modified.longValue()) {
                val = false;
            }
        }
        return val;
    }

    private long getLastModifiedTime(CacheEntry entry) {
        long val = 0;
        if (entry.getEntryKey() instanceof File) {
            val = getLastModifiedTime((File) entry.getEntryKey());
        } else if (entry.getEntryKey() instanceof URL) {
            URL url = (URL) entry.getEntryKey();
            val = getLastModifiedTime(url);
        } else if (entry.getEntryKey() instanceof String) {
            String fileName = (String) entry.getEntryKey();
            val = getLastModifiedTime(fileName);
        }
        return val;
    }

    private long getLastModifiedTime(String fileName) {
        File aFile = new File(fileName);
        if (aFile.exists()) {
            return getLastModifiedTime(aFile);
        } else {
            try {
                URL aUrl = new URL(fileName);
                return getLastModifiedTime(aUrl);
            } catch (MalformedURLException e) {
            }
        }
        return 0;
    }

    private long getLastModifiedTime(URL url) {
        try {
            return url.openConnection().getLastModified();
        } catch (IOException e) {
            return 0;
        }
    }

    private long getLastModifiedTime(File aFile) {
        return aFile.lastModified();
    }
}
