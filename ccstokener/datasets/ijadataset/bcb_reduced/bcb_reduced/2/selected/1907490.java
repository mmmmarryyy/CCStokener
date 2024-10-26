package org.emergent.antbite.savant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.emergent.antbite.savant.log.Log;

/**
 * <p>
 * This class is the base class of Internet download processors.
 * </p>
 *
 * @author  Brian Pontarelli
 */
public abstract class InternetProcess implements Process {

    private String defaultDomain;

    private File mapping;

    private boolean failonmd5;

    public String getDefaultdomain() {
        return defaultDomain;
    }

    public void setDefaultdomain(String defaultDomain) {
        this.defaultDomain = defaultDomain;
    }

    public String getUrl() {
        return defaultDomain;
    }

    public void setUrl(String defaultDomain) {
        this.defaultDomain = defaultDomain;
    }

    public File getMapping() {
        return mapping;
    }

    public void setMapping(File mapping) {
        this.mapping = mapping;
    }

    public boolean isFailonmd5() {
        return failonmd5;
    }

    public void setFailonmd5(boolean failonmd5) {
        this.failonmd5 = failonmd5;
    }

    /**
     * Returns a printable name of this process. Used in error messaging.
     *
     * @return  The name
     */
    public abstract String getProcessName();

    /**
     * Validates the process. Processes must have either a default domain or a
     * mapping file.
     *
     * @throws  SavantException If the process is invalid
     */
    public void validate() throws SavantException {
        if (defaultDomain == null && mapping == null) {
            throw new SavantException("The " + getProcessName() + " requires " + "either a default domain or a mapping file");
        }
    }

    /**
     * Using the URL spec given, this method connects to the URL, reads the file
     * from the URL and stores the file in the local cache store. The artifact is
     * used to determine the local cache store directory and file name.
     *
     * @param   builder The URLBuilder to use to generate URLs
     * @param   artifact The artifact being fetched and stored
     * @param   localCache The local cache to store the artifact in
     * @return  The File of the artifact in the local cache
     */
    protected File findFile(URLBuilder builder, Artifact artifact, LocalCacheStore localCache) throws SavantException {
        URL artifactURL = builder.buildURL(defaultDomain, mapping, artifact);
        if (artifactURL == null) {
            throw new SavantException("Unable to build URL for artifact [" + artifact.toString() + "]");
        }
        URLConnection uc = openConnection(artifactURL);
        if (uc == null) {
            return null;
        }
        InputStream is = null;
        try {
            if (!isValid(uc)) {
                return null;
            }
            byte[] md5 = getMD5Hash(builder, artifact);
            is = uc.getInputStream();
            return localCache.store(artifact, is, md5, failonmd5);
        } catch (IOException ioe) {
            Log.log("Unable to download artifact [" + artifact + "]", Log.ERROR);
            throw new SavantException(ioe);
        } catch (SavantException se) {
            Log.log(se.getMessage(), Log.ERROR);
            throw new SavantException(se);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    throw new SavantException(ioe);
                }
            }
        }
    }

    /**
     * Builds the MD5 hash using the URLBuilder to build a URL to the MD5 file for
     * the given artifact.
     *
     * @param   builder The URLBuilder to call the
     *          {@link URLBuilder#buildMD5URL(String, File, Artifact)} method on.
     * @param   artifact The artifact to build the hash for
     * @return  The hash or null if it doesn't exist
     * @throws  SavantException If the hash could not be read
     */
    protected byte[] getMD5Hash(URLBuilder builder, Artifact artifact) throws SavantException {
        byte[] bytes = null;
        URL url = builder.buildMD5URL(defaultDomain, mapping, artifact);
        if (url != null) {
            URLConnection uc = openConnection(url);
            if (isValid(uc)) {
                StringBuffer buf = new StringBuffer();
                try {
                    InputStream is = uc.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    char[] c = new char[1024];
                    int count;
                    while ((count = br.read(c, 0, 1024)) != -1) {
                        for (int i = 0; i < count; i++) {
                            if (Character.isWhitespace(c[i])) {
                                continue;
                            }
                            buf.append(c[i]);
                        }
                    }
                    br.close();
                    isr.close();
                    is.close();
                    if (buf.length() > 0) {
                        bytes = StringTools.fromHex(buf.toString());
                    }
                } catch (IOException ioe) {
                    Log.log("Unable to download MD5 (skipping) for artifact [" + artifact + "]", Log.DEBUG);
                } catch (IllegalArgumentException iae) {
                    Log.log("Unable to download MD5 (skipping) for artifact [" + artifact + "]", Log.DEBUG);
                }
            }
        }
        return bytes;
    }

    /**
     * Updates the given artifact with its dependencies by parsing the XML
     * dependency file (if one exists).
     *
     * @param   builder The URLBuilder that will build the URL to the XML file
     * @param   artifact The artifact to update
     * @param   localCache The local cache store to cache the dependency file if
     *          found.
     * @throws  SavantException If building, parsing or content failed or was
     *          invalid
     */
    protected boolean resolveArtifactDependencies(URLBuilder builder, Artifact artifact, LocalCacheStore localCache) throws SavantException {
        URL url = builder.buildDepsURL(defaultDomain, mapping, artifact);
        if (url != null) {
            URLConnection uc = openConnection(url);
            if (isValid(uc)) {
                InputStream is = null;
                File tmp = null;
                try {
                    is = uc.getInputStream();
                    tmp = File.createTempFile("savant", "deps");
                    FileTools.output(is, tmp);
                    ArtifactTools.resolveArtifactDependencies(artifact, tmp);
                    localCache.storeDeps(artifact, tmp);
                    return true;
                } catch (IOException e) {
                    Log.log("Unable to locate artifact dependency XML for artifact [" + artifact + "]", Log.DEBUG);
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (tmp != null) {
                            tmp.delete();
                        }
                    } catch (IOException ioe) {
                        throw new SavantException(ioe);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Opens a HTTP connection to the given URL. If the URL is invalid, a
     * BuildException is thrown. If the URL can't be opened, this does not error
     * out, but just returns null.
     *
     * @param   url The URL to open
     * @return  The connection if if can be opened, null if it can't
     */
    protected URLConnection openConnection(URL url) throws SavantException {
        Log.log("Opening connection to [" + url + "]", Log.DEBUG);
        URLConnection uc = null;
        try {
            uc = url.openConnection();
            uc.connect();
        } catch (MalformedURLException mue) {
            throw new SavantException(mue);
        } catch (IOException ioe) {
            Log.log("Unable to open connection to [" + url + "] because [" + ioe.getMessage() + "]", Log.INFO);
        }
        return uc;
    }

    /**
     * Verifies that the URL connection is valid by fetching the HTTP header
     * response code (by first casting to an HttpURLConnection) and ensuring
     * that it is either accepted or ok. If the URLConnection is not an HTTP
     * connection, this method returns true.
     *
     * @param   uc The URL connection validate
     * @return  True if valid, false otherwise
     * @throws  SavantException If the response code could not be fetched
     */
    protected boolean isValid(URLConnection uc) throws SavantException {
        boolean valid = true;
        if (uc instanceof HttpURLConnection) {
            try {
                int code = ((HttpURLConnection) uc).getResponseCode();
                Log.log("Got code from URL of [" + code + "]", Log.DEBUG);
                valid = (code == HttpURLConnection.HTTP_ACCEPTED || code == HttpURLConnection.HTTP_OK);
            } catch (IOException ioe) {
                Log.log("Unable to verify the status of HTTP connection to the URL [" + uc.getURL() + "]", Log.ERROR);
                throw new SavantException(ioe);
            }
        }
        return valid;
    }
}
