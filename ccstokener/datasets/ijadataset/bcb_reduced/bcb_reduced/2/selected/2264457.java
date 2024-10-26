package uk.ac.sanger.cgp.dbcon.util.resources;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.IOUtils;
import uk.ac.sanger.cgp.dbcon.exceptions.DbConException;
import uk.ac.sanger.cgp.dbcon.util.InputOutputUtils;

/**
 * Implementation of the resource pattern which streams input from valid
 * URLs and will also clean up error streams if an exception is
 * detected. This is very important in the Sun implementation of the URL
 * classes since the Java VM is not fast enough to clear the backlog itself
 * and this will cause a decrease in the number of available sockets on the 
 * machine (normally these are idle sockets as well)
 * 
 * @author ayates
 */
public class UrlResource extends AbstractResource {

    public UrlResource(String location) {
        super(location);
    }

    protected InputStream getActualInputStream() {
        InputStream is = null;
        URLConnection connection = null;
        try {
            URL url = URI.create(getLocation()).toURL();
            connection = url.openConnection();
            connection.connect();
            InputStream bytesInput = connection.getInputStream();
            byte[] bytes = IOUtils.toByteArray(bytesInput);
            is = new ByteArrayInputStream(bytes);
        } catch (MalformedURLException e) {
            cleanUpHttpURLConnection(connection);
            throw new DbConException("Detected a malformed URL; aborting", e);
        } catch (FileNotFoundException e) {
            cleanUpHttpURLConnection(connection);
            throw new DbConException("Could not find the specified file at the given URL", e);
        } catch (IOException e) {
            cleanUpHttpURLConnection(connection);
            throw new DbConException("Detected IO problems whilst reading URL's content", e);
        }
        return is;
    }

    /**
   * Used to provide a clean up procedure when dealing with URLs
   */
    private void cleanUpHttpURLConnection(URLConnection connection) throws DbConException {
        if (connection == null) {
            getLog().fatal("Input HttpURLConnection was null ... will not perform cleanup");
        } else if (HttpURLConnection.class.isAssignableFrom(connection.getClass())) {
            InputStream errorStream = null;
            try {
                HttpURLConnection httpUrlConnection = (HttpURLConnection) connection;
                errorStream = httpUrlConnection.getErrorStream();
                if (errorStream != null) {
                    byte[] errorOutput = IOUtils.toByteArray(errorStream);
                    if (getLog().isDebugEnabled()) {
                        String lengthOfError = (errorOutput == null) ? "NULL STREAM" : Integer.toString(errorOutput.length);
                        getLog().debug("Length of error stream (in bytes): " + lengthOfError);
                    }
                }
                int responseCode = httpUrlConnection.getResponseCode();
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Detected response code: " + responseCode);
                }
            } catch (IOException e) {
                throw new DbConException("Detected IOException when retriving errorStream", e);
            } finally {
                InputOutputUtils.closeQuietly(errorStream);
            }
        } else {
            getLog().debug("Input URLConnection was not a HttpURLConnection; " + "will not perform cleanup");
        }
    }
}
