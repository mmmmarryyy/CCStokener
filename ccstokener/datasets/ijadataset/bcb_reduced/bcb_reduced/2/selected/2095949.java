package org.klco.openkeyvalfs.model.interaction;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.klco.openkeyvalfs.Utilities;
import org.klco.openkeyvalfs.model.OpenKeyValObject;
import org.klco.openkeyvalfs.model.OpenKeyValConfig;

/**
 * Interacts with the OpenKeyVal.org website to store and retrieve data.
 *
 * @author daniel.klco
 * @version 20101105
 */
public class OpenKeyValInteraction {

    /** log4j logger */
    private static Logger log = Logger.getLogger(OpenKeyValInteraction.class);

    /** the key for looking up the url of OpenKeyVal.org (can be replaced for testing purposes) */
    public static final String OPEN_KEY_VAL_URL_KEY = "OPEN_KEY_VAL_URL";

    /** the base url for OpenKeyVal.org */
    protected static final String BASE_URL = Utilities.getApplicationProperty(OPEN_KEY_VAL_URL_KEY);

    /** the maximum size for an OpenKeyVal.org data chunk */
    public static final int VALUE_SIZE = 65536;

    /**
     * a cache of interaction objects, used to easily retrieve the interaction
     * object associated with a config
     */
    protected static Map<OpenKeyValConfig, OpenKeyValInteraction> cache = new HashMap<OpenKeyValConfig, OpenKeyValInteraction>();

    /** the configuration object for this interaction object */
    protected OpenKeyValConfig config;

    /** the current file count for the particular OpenKeyVal datastore */
    protected int fileCount;

    /**
     * Default constructor, nothing to see here folks, move along.
     */
    protected OpenKeyValInteraction() {
    }

    /**
     * Non-default constructor, nothing to see here folks, move along.
     */
    protected OpenKeyValInteraction(OpenKeyValConfig core) {
        this.config = core;
    }

    /**
     * Retrieve the interaction associated with the specified configuration.  If
     * the interaction is new it will create a single root folder.
     *
     * @param config - the configuration with which to retrieve the interaction
     * @return the interaction
     * @throws IOException
     */
    public static OpenKeyValInteraction getInteraction(OpenKeyValConfig config) throws IOException {
        log.info("Getting interaction for " + config);
        if (cache.containsKey(config)) {
            return cache.get(config);
        }
        OpenKeyValInteraction intr = new OpenKeyValInteraction(config);
        String count = intr.get(0, -3);
        if (count == null) {
            log.info("Data not previously saved under root :" + config.getBaseKey() + " creating.");
            intr.save(0, -3, "1");
            intr.save(0, -1, "F:0|/|" + System.currentTimeMillis() + "|1|0");
            intr.save(0, 0, "");
            intr.fileCount = 1;
            log.info("Base data created successfully.");
        } else {
            log.debug("Data retrieved");
            intr.fileCount = Integer.parseInt(count);
        }
        cache.put(config, intr);
        log.debug("Successfully retrieved interaction");
        return intr;
    }

    /**
     * Create a new id, will be unique.
     * 
     * @return
     * @throws IOException
     */
    public int createId() throws IOException {
        log.debug("Creating new object id");
        save(0, -3, String.valueOf(fileCount + 1));
        return fileCount++;
    }

    /**
     * Delete the specified object from the OpenKeyVal.org datastore
     *
     * @param obj the object to delete
     * @param deleteMetadata if this is true it will delete the metadata and not just the contents
     * @throws IOException
     */
    public void delete(OpenKeyValObject obj, boolean deleteMetadata) throws IOException {
        log.info("Deleting object " + obj.getId());
        for (int i = 0; i < obj.getValueCount(); i++) {
            if (log.isTraceEnabled()) {
                log.trace("Deleting content from chunk " + i);
            }
            String key = config.getBaseKey() + "_" + String.valueOf(obj.getId()) + "_" + String.valueOf(i);
            set(key, "");
        }
        if (deleteMetadata) {
            log.debug("Deleting metadata");
            set(config.getBaseKey() + "_" + String.valueOf(obj.getId()) + "_" + String.valueOf(-1), "");
            set(config.getBaseKey() + "_" + String.valueOf(obj.getId()) + "_" + String.valueOf(-2), "");
            set(config.getBaseKey() + "_" + String.valueOf(obj.getId()) + "_" + String.valueOf(-3), "");
        }
        log.debug("Object deleted succeessfully");
    }

    /**
     * Retrieve the configuration for this interaction.
     *
     * @return
     */
    public OpenKeyValConfig getConfig() {
        return this.config;
    }

    /**
     * Retrieve the data from OpenKeyVal.org for the specified id and chunk.
     *
     * @param id the id to retrieve
     * @param chunk the chunk to retrieve
     * @return the data from the specified id and chunk
     * @throws IOException
     */
    public String get(int id, int chunk) throws IOException {
        log.debug("Retrieving value for " + id + " from chunk " + chunk);
        String key = config.getBaseKey() + "_" + String.valueOf(id) + "_" + String.valueOf(chunk);
        return get(key);
    }

    /**
     * Retrieve the data from OpenKeyVal.org for the specified key.
     *
     * @param key the key to retrieve
     * @return the data from the specified id and chunk
     * @throws IOException
     */
    protected String get(String key) throws IOException {
        URL myUrl = new URL(BASE_URL + key);
        log.debug("Opening connection to: " + myUrl);
        HttpURLConnection urlConn = (HttpURLConnection) myUrl.openConnection();
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        if (urlConn.getResponseCode() != 200) {
            log.warn("Recieved error response of " + urlConn.getResponseCode() + " " + urlConn.getRequestMethod());
            return null;
        }
        log.debug("Response successfully recieved");
        DataInputStream input = new DataInputStream(urlConn.getInputStream());
        byte[] data = new byte[VALUE_SIZE];
        int cnt = 0;
        int cur = input.read();
        while (cur != -1) {
            data[cnt] = Byte.valueOf((byte) cur);
            cur = input.read();
            cnt++;
        }
        data = Utilities.shrink(data, cnt);
        log.debug("Recieved response of size: " + data.length);
        return URLDecoder.decode(new String(data, "UTF-8"), "UTF-8");
    }

    /**
     * Called to save the particular chunk of data to the OpenKeyVal.org datastore.
     *
     * @param id the id of the object to save to
     * @param chunk the id of the chunk to save to
     * @param contents the contents to save
     * @throws IOException
     */
    public void save(int id, int chunk, String contents) throws IOException {
        set(config.getBaseKey() + "_" + String.valueOf(id) + "_" + String.valueOf(chunk), contents);
    }

    /**
     * Set the specified value to the specified key in the OpenKeyVal.org.  The value will be
     * URLEncoded before being saved.
     *
     * @param key the key to save to
     * @param value the value to save
     * @throws MalformedURLException
     * @throws IOException
     */
    protected void set(String key, String value) throws MalformedURLException, IOException {
        if (log.isTraceEnabled()) {
            log.trace("Saving " + key + ": " + value);
        }
        URL myUrl = new URL(BASE_URL);
        HttpURLConnection urlConn = (HttpURLConnection) myUrl.openConnection();
        log.debug("Connection opened");
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
        byte[] bValue = URLEncoder.encode(value, "UTF-8").getBytes("UTF-8");
        log.debug("Sending request of size " + bValue.length);
        printout.write((key + "=" + URLEncoder.encode(value, "UTF-8")).getBytes("UTF-8"));
        printout.flush();
        printout.close();
        DataInputStream input = new DataInputStream(urlConn.getInputStream());
        log.debug("Reply recieved with code: " + urlConn.getResponseCode());
        if (urlConn.getResponseCode() != 200) {
            log.warn("Recieved error response of " + urlConn.getResponseCode() + " " + urlConn.getRequestMethod());
            throw new IOException("Invalid response from save " + urlConn.getResponseCode());
        }
        input.close();
        if (log.isTraceEnabled()) {
            log.trace("Save successful");
        }
    }
}
