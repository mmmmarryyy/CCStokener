package org.fulworx.core.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse each line in the configuration file to determine the start date, end date, and refresh time for
 * caching the given resource.
 * <p/>
 * File format:
 * [2005-12-01 12:00:00|2000|-1] read|/somePropConfigCachedResource/1/2
 * [2005-12-01 12:00:00|2000|40000] read|/somePropConfigCachedResource/3/4
 * <p/>
 * This will create 2 refreshing entries starting on Dec 1, 2005 for reading the resources /somePropConfigCachedResource/1/2
 * and /somePropConfigCachedResource/3/4.  Both refresh every 2000 milliseconds, and the second resource will stop refreshing
 * at the start time + 40000 milliseconds.  If the start time is in the past, the current time is used.
 *
 * @author teastlack
 * @date Oct 25, 2007
 */
public class ConfigFile {

    private static final Log LOG = LogFactory.getLog(ConfigFile.class);

    private List<ConfigFileEntry> entries;

    private String filename;

    private SimpleDateFormat dateFormat;

    public ConfigFile(String filename, SimpleDateFormat dateFormat) {
        this.filename = filename;
        this.dateFormat = dateFormat;
    }

    /**
     * Default the date format within the file to yyyy-MM-dd HH:mm:ss
     *
     * @param propertyFileName classpath URI to filename
     */
    public ConfigFile(String propertyFileName) {
        this(propertyFileName, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    public List<ConfigFileEntry> getEntries() {
        if (entries == null) {
            entries = createEntries(this.filename);
        }
        return entries;
    }

    private List<ConfigFileEntry> createEntries(String filename) {
        List<ConfigFileEntry> data = new ArrayList<ConfigFileEntry>();
        try {
            InputStream stream = null;
            URL url = getClass().getClassLoader().getResource(filename);
            if (url != null) {
                stream = url.openStream();
            }
            if (stream == null) {
                File file = new File(filename);
                if (file.exists()) {
                    stream = new FileInputStream(filename);
                } else {
                    LOG.warn("Unable to find file at " + file.getAbsolutePath());
                }
            }
            if (stream != null) {
                readStream(stream, data);
            } else {
                LOG.error("No file found:" + filename);
            }
        } catch (IOException e) {
            LOG.error("Unable to load properties file " + new File(filename).getAbsolutePath(), e);
        }
        return data;
    }

    private void readStream(InputStream stream, List<ConfigFileEntry> data) throws IOException {
        InputStreamReader input = new InputStreamReader(stream);
        BufferedReader bufRead = new BufferedReader(input);
        String line;
        int count = 1;
        while ((line = bufRead.readLine()) != null) {
            LOG.debug("Loading line " + count + ": " + line);
            if (line.length() > 0) {
                data.add(new ConfigFileEntry(line, dateFormat));
            }
            count++;
        }
        bufRead.close();
    }
}
