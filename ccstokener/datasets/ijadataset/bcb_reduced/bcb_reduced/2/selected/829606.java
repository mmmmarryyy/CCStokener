package com.zwarg.thread;

import java.io.*;
import java.net.*;

/**
 * A loader that attempts to open an input stream in a separate thread, enabling
 * timeout and status checking.
 *
 * @author David Zwarg
 */
public class InputStreamLoader implements Runnable {

    /**
   * A flag to determine if the stream URL is properly formatted. This flag is
   * set to false if, when opening the URL, a MalformedURLException is thrown.
   */
    private boolean bolStreamFormat = false;

    /**
   * A flag to indicate that the stream has been loaded and is available for
   * reading.
   */
    private boolean bolStreamLoaded = false;

    /**
   * The URL to load in the background.
   */
    private URL urlLoadMe = null;

    /**
   * The input stream that read the url #urlLoadMe.
   */
    private InputStream insLoadMe = null;

    /**
   * Create a background threaded URL loader.
   */
    public InputStreamLoader() {
    }

    /**
   * Create a background threaded URL loader from a named url.
   *
   * @param anUrl The string representation of the URL to be loaded.
   */
    public InputStreamLoader(String anUrl) {
        setUrl(anUrl);
    }

    /**
   * Begin retrieving the named URL.
   */
    public void fetch() {
        new Thread(this).start();
    }

    /**
   * Perform the fetching of the URL into the input stream.
   */
    public void run() {
        if (this.bolStreamFormat) {
            try {
                this.insLoadMe = this.urlLoadMe.openStream();
                this.bolStreamLoaded = true;
            } catch (IOException ioe) {
                this.bolStreamLoaded = false;
            }
        }
    }

    /**
   * Determine if the URL has been formatted properly.
   *
   * @return  <CODE>true</CODE> if the URL has been properly formatted.
   */
    public boolean isUrlFormatValid() {
        return new Boolean(this.bolStreamFormat).booleanValue();
    }

    /**
   * Determine if the stream has been loaded into the InputStream properly.
   *
   * @return  <CODE>true</CODE> if the stream has been loaded into the InputStream.
   */
    public boolean isStreamLoaded() {
        return new Boolean(this.bolStreamLoaded).booleanValue();
    }

    /**
   * Get the input stream associated with the given URL. This method will return
   * a valid InputStream only if the URL has proper formatting, and is loaded.
   *
   * @return  The InputStream for the named URL, or <CODE>null</CODE> if there is no stream
   * available.
   */
    public InputStream getInputStream() {
        return this.insLoadMe;
    }

    /**
   * Change the URL that should be fetched by this background loader.
   *
   * @param anUrl A string representation of a valid URL.
   */
    public void setUrl(String anUrl) {
        this.bolStreamFormat = false;
        this.bolStreamLoaded = false;
        try {
            this.urlLoadMe = new URL(anUrl);
            this.bolStreamFormat = true;
        } catch (MalformedURLException mue) {
            this.bolStreamFormat = false;
        }
    }
}
