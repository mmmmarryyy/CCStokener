package org.opencarto.io;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

/**
 * 
 * This data loader load data stored somewhere on a static file to be loaded only once.
 * 
 * @author julien Gaffuri
 *
 */
public abstract class FileLoader extends OneTimeDataLoader {

    private static Logger logger = Logger.getLogger(FileLoader.class.getName());

    public FileLoader(DataSource ds) {
        super(ds);
    }

    protected synchronized InputStream getInputStream() {
        InputStream ips = null;
        try {
            URL url = new URL(this.getDataSource().getUrl());
            if ("file".equals(url.getProtocol())) ips = new FileInputStream(url.getFile()); else if ("http".equals(url.getProtocol())) ips = url.openStream(); else logger.warning("Impossible to load file from " + url + ". Unsupported protocol " + url.getProtocol());
        } catch (Exception e) {
            logger.warning("Impossible to read from: " + this.getDataSource().getUrl());
            e.printStackTrace();
        }
        return ips;
    }
}
