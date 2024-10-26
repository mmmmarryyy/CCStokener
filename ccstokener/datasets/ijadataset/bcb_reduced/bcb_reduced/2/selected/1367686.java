package net.sf.maven.plugins.sf_download_wagon;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Properties;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author SchubertT006
 * 
 * @plexus.component role="org.apache.maven.wagon.Wagon"
 *                   role-hint="http-sourceforge"
 *                   instantiation-strategy="per-lookup"
 *                   
 */
public class SourceforgeDownloadWagon extends LightweightHttpWagon {

    /**
	 * @plexus.configuration  default-value="0.5-SNAPSHOT"
	 *  
	 */
    protected String buildVersion;

    private static Logger logger = LoggerFactory.getLogger(SourceforgeDownloadWagon.class);

    private Properties httpHeaders;

    private void addHeaders(URLConnection urlConnection) {
        if (httpHeaders != null) {
            for (Iterator<Object> i = httpHeaders.keySet().iterator(); i.hasNext(); ) {
                String header = (String) i.next();
                urlConnection.setRequestProperty(header, httpHeaders.getProperty(header));
            }
        }
    }

    private String buildUrl(String path) {
        final String repoUrl = getRepository().getUrl();
        path = path.replace(' ', '+');
        if (repoUrl.charAt(repoUrl.length() - 1) != '/') {
            return repoUrl + '/' + path;
        }
        return repoUrl + path;
    }

    protected boolean checkResource(final String resourceName) {
        boolean isValid = true;
        HttpURLConnection.setFollowRedirects(false);
        try {
            final URL url = new URL(buildUrl(new Resource(resourceName).getName()));
            logger.debug("check url: " + url.toString());
            final HttpURLConnection headConnection = (HttpURLConnection) url.openConnection();
            addHeaders(headConnection);
            headConnection.setRequestMethod("HEAD");
            headConnection.setDoOutput(true);
            int statusCode = headConnection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_MOVED_PERM) {
                isValid = false;
                logger.debug("responseCode: " + statusCode);
            } else {
                logger.debug("responseCode: " + statusCode);
            }
        } catch (MalformedURLException e) {
            logger.error(e.toString());
            isValid = false;
        } catch (ProtocolException e) {
            logger.error(e.toString());
            isValid = false;
        } catch (IOException e) {
            logger.error(e.toString());
            isValid = false;
        }
        HttpURLConnection.setFollowRedirects(true);
        return isValid;
    }

    @Override
    public void get(final String resourceNameOrg, final File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        final String resourceName = resourceNameOrg + "/download";
        logger.info("Version: " + this.buildVersion + "\nGET: " + resourceName + "\nTO: " + destination.getAbsolutePath());
        final java.util.logging.Logger sunLogger = java.util.logging.Logger.getLogger(SourceforgeDownloadWagon.class.getName());
        sunLogger.info("Version: " + this.buildVersion + "\nGET: " + resourceName + "\nTO: " + destination.getAbsolutePath());
        if (this.checkResource(resourceName)) {
            super.get(resourceName, destination);
        } else {
            logger.info("Could not be retrieve: " + resourceName);
            cleanupGetTransfer(new Resource(resourceNameOrg));
        }
    }

    @Override
    public boolean getIfNewer(final String resourceName, final File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        logger.debug("excute getIfNewer(" + resourceName + ", " + destination.getAbsolutePath() + ")");
        return super.getIfNewer(resourceName, destination, timestamp);
    }

    @Override
    public void closeConnection() throws ConnectionException {
        logger.debug("execute closeConnection()");
        super.closeConnection();
    }

    @Override
    public void fillInputData(InputData arg0) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        logger.debug("execute fillInputData(arg0)");
        super.fillInputData(arg0);
    }

    @Override
    public void fillOutputData(OutputData arg0) throws TransferFailedException {
        logger.debug("exceute fillOutputData(arg0)");
        super.fillOutputData(arg0);
    }

    @Override
    public void openConnection() throws ConnectionException, AuthenticationException {
        logger.debug("execute openConnection()");
        super.openConnection();
    }

    @Override
    public void put(File arg0, String arg1) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        logger.debug("execute put(file,string)");
        super.put(arg0, arg1);
    }
}
