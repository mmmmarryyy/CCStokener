package gov.lanl.adore.helper;

import gov.lanl.util.StreamUtil;
import gov.lanl.util.resource.Resource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.log4j.Logger;

/**
 * Simplifies OpenURL requests to adore-arcfile-resolver
 * @author rchute
 */
public class ArcResolverProxy {

    static Logger log = Logger.getLogger(ArcResolverProxy.class.getName());

    private static String SVCID_ADORE4 = "info:lanl-repo/svc/getDatastream";

    private URL defaultBaseurl;

    /** 
     * Default constructor; requires explicit setDefaultBaseurl() call
     */
    public ArcResolverProxy() {
    }

    /** 
     * Constructor used to initially define the service url
     * @param baseurl
     *        Base URL of the adore-arcfile-resolver service <br>
     *        (i.e. http://localhost:8080/adore-arcfile-resolver)
     *        
     */
    public ArcResolverProxy(URL baseurl) {
        if (baseurl == null) throw new NullPointerException("empty baseurl");
        this.defaultBaseurl = baseurl;
    }

    /**
     * Gets a Resource object given a resourceId and the collectionId
     * in which it resides
     * @param collectionId
     *        the repositoryId of resourceId 
     *        (e.g. info:lanl-repo/arc/560bbd27-9558-4742-bac4-df21cba5c4cf)
     * @param resourceId
     *        the unique identifier of the datastream
     *        (e.g. info:lanl-repo/ds/d5bb085a-e342-489e-a416-c46f1a21dc53)
     * @return
     *        A Resource object containing bitstream and content-type 
     * @throws Exception
     */
    public Resource get(String collectionId, String resourceId) throws Exception {
        if (defaultBaseurl == null) throw new NullPointerException("empty defaultBaseurl");
        URL baseUrl;
        try {
            baseUrl = new URL(defaultBaseurl.toString() + "/" + collectionId.substring(collectionId.lastIndexOf("/") + 1) + "/openurl-aDORe4");
            log.debug("Service URL: " + baseUrl);
        } catch (MalformedURLException e) {
            throw new Exception(e);
        }
        return get(baseUrl, resourceId);
    }

    /**
     * Gets a Resource object given a resourceId and service url 
     * @param serviceUrl
     *        Service URL for repo in which resource resides <br>
     *        (e.g. http://localhost:8080/adore-arcfile-resolver/d5bb085a-e342-489e-a416-c46f1a21dc53/openurl-aDORe4)
     * @param resourceId
     *        the unique identifier of the datastream
     *        (e.g. info:lanl-repo/ds/d5bb085a-e342-489e-a416-c46f1a21dc53)
     * @return
     *        A Resource object containing bitstream and content-type 
     * @throws Exception
     */
    public Resource get(URL serviceUrl, String resourceId) throws Exception {
        Resource resource = new Resource();
        String openurl = serviceUrl.toString() + "?url_ver=Z39.88-2004" + "&rft_id=" + URLEncoder.encode(resourceId, "UTF-8") + "&svc_id=" + SVCID_ADORE4;
        log.debug("OpenURL Request: " + openurl);
        URL url;
        try {
            url = new URL(openurl);
            HttpURLConnection huc = (HttpURLConnection) (url.openConnection());
            int code = huc.getResponseCode();
            if (code == 200) {
                InputStream is = huc.getInputStream();
                resource.setBytes(StreamUtil.getByteArray(is));
                resource.setContentType(huc.getContentType());
            } else {
                log.error("An error of type " + code + " occurred for " + url.toString());
                throw new Exception("Cannot get " + url.toString());
            }
        } catch (MalformedURLException e) {
            throw new Exception("A MalformedURLException occurred for " + openurl);
        } catch (IOException e) {
            throw new Exception("An IOException occurred attempting to connect to " + openurl);
        }
        return resource;
    }

    /**
     * Gets the base ARCfile Resolver Service URL
     * @return
     *        Service BaseUrl
     */
    public URL getDefaultBaseurl() {
        return defaultBaseurl;
    }

    /**
     * Sets the base ARCfile Resolver Service URL
     * @param baseurl
     *        Service URL for repo in which resource resides <br>
     *        (e.g. http://localhost:8080/adore-arcfile-resolver/d5bb085a-e342-489e-a416-c46f1a21dc53/openurl-aDORe4)
     */
    public void setDefaultBaseurl(URL baseurl) {
        this.defaultBaseurl = baseurl;
    }

    /**
     * Main Method - Parses command line args<br>
     * 
     * Expects the following args:<br>
     *   [baseUrl]<br>
     *        base ARCfile Resolver Service URL<br>
     *   [collectionId]<br>
     *         the repositoryId of resourceId<br>
     *   [resourceId]<br>
     *        the unique identifier of the datastream<br>
     *   [outputFile]<br>
     *        path to output file
     * @param args
     *        String Array containing processing configurations
     */
    public static void main(String[] args) {
        ArcResolverProxy resolver;
        try {
            if (args.length != 4) {
                System.out.println("Usage: gov.lanl.adore.helper.ArcResolverProxy [baseUrl] [collectionId] [resourceId] [outputFile]");
                System.exit(0);
            }
            long s = System.currentTimeMillis();
            resolver = new ArcResolverProxy(new URL(args[0]));
            Resource resource = resolver.get(args[1], args[2]);
            System.out.println("Time to get resource: " + (System.currentTimeMillis() - s));
            System.out.println("Content-type: " + resource.getContentType());
            System.out.println("Writing file to: " + args[3]);
            FileOutputStream fos = new FileOutputStream(new File(args[3]));
            fos.write(resource.getBytes());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
