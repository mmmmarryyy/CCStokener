package gov.lanl.adore.helper;

import gov.lanl.identifier.sru.SRUDC;
import gov.lanl.identifier.sru.SRUException;
import gov.lanl.identifier.sru.SRUSearchRetrieveResponse;
import gov.lanl.locator.IdLocation;
import gov.lanl.locator.IdLocatorException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simplifies OpenURL requests to adore-xmltape-resolver
 * @author rchute
 */
public class XMLTapeResolverProxy {

    private static String SVCID_ADORE1 = "info:lanl-repo/svc/getDIDL";

    private static String SVCID_ADORE2 = "info:lanl-repo/svc/locate.sru";

    private URL defaultBaseurl;

    /** 
     * Default constructor; requires explicit setDefaultBaseurl() call
     */
    public XMLTapeResolverProxy() {
    }

    /** 
     * Constructor used to initially define the service url
     * @param baseurl
     *        Base URL of the adore-xmltape-resolver service <br>
     *        (i.e. http://localhost:8080/adore-xmltape-resolver)
     *        
     */
    public XMLTapeResolverProxy(URL baseurl) {
        if (baseurl == null) throw new NullPointerException("empty baseurl");
        this.defaultBaseurl = baseurl;
    }

    /**
     * Gets a Resource object given a resourceId and the collectionId
     * in which it resides
     * @param collectionId
     *        the repositoryId of resourceId 
     *        (e.g. info:lanl-repo/xmltape/demoTape)
     * @param resourceId
     *        the unique identifier of the datastream
     *        (e.g. info:lanl-repo/i/d5bb085a-e342-489e-a416-c46f1a21dc53)
     * @return
     *        A Resource object containing bitstream and content-type 
     * @throws Exception
     */
    public Resource get(String collectionId, String resourceId) throws Exception {
        if (defaultBaseurl == null) throw new NullPointerException("empty defaultBaseurl");
        URL baseUrl;
        try {
            baseUrl = new URL(defaultBaseurl.toString() + "/" + collectionId.substring(collectionId.lastIndexOf("/") + 1) + "/openurl-aDORe1");
        } catch (MalformedURLException e) {
            throw new Exception(e);
        }
        return get(baseUrl, resourceId);
    }

    /**
     * Gets a Resource object given a resourceId and service url 
     * @param serviceUrl
     *        Service URL for repo in which resource resides <br>
     *        (e.g. http://localhost:8080/adore-xmltape-resolver/demoTape/openurl-aDORe1)
     * @param resourceId
     *        the unique identifier of the datastream
     *        (e.g. info:lanl-repo/i/d5bb085a-e342-489e-a416-c46f1a21dc53)
     * @return
     *        A Resource object containing bitstream and content-type 
     * @throws Exception
     */
    public Resource get(URL serviceUrl, String resourceId) throws Exception {
        Resource resource = new Resource();
        String openurl = serviceUrl.toString() + "?url_ver=Z39.88-2004" + "&rft_id=" + URLEncoder.encode(resourceId, "UTF-8") + "&svc_id=" + SVCID_ADORE1;
        URL url;
        try {
            url = new URL(openurl);
            HttpURLConnection huc = (HttpURLConnection) (url.openConnection());
            int code = huc.getResponseCode();
            if (code == 200) {
                InputStream is = huc.getInputStream();
                resource.setBytes(StreamUtil.getByteArray(is));
                resource.setContentType(huc.getContentType());
            } else throw new Exception("Cannot get " + url.toString());
        } catch (MalformedURLException e) {
            throw new Exception("A MalformedURLException occurred for " + openurl);
        } catch (IOException e) {
            throw new Exception("An IOException occurred attempting to connect to " + openurl);
        }
        return resource;
    }

    /**
	 * Gets locate.sru response as a list
	 * 
	 * @param serviceUrl
	 *            OpenURL baseurl
	 * @param identifier
	 * @return list of Idlocations
	 * @throws Exception
	 */
    public List<IdLocation> getLocations(URL serviceUrl, String identifier) throws Exception {
        String openurl = serviceUrl.toString() + "?url_ver=Z39.88-2004&rft_id=" + identifier + "&svc_id=" + SVCID_ADORE2;
        URL url;
        SRUSearchRetrieveResponse sru;
        try {
            url = new URL(openurl);
            HttpURLConnection huc = (HttpURLConnection) (url.openConnection());
            int code = huc.getResponseCode();
            if (code == 200) {
                sru = SRUSearchRetrieveResponse.read(huc.getInputStream());
            } else throw new IdLocatorException("cannot get " + url.toString());
        } catch (MalformedURLException e) {
            throw new IdLocatorException("A MalformedURLException occurred for " + openurl);
        } catch (IOException e) {
            throw new IdLocatorException("An IOException occurred attempting to connect to " + openurl);
        } catch (SRUException e) {
            throw new IdLocatorException("An SRUException occurred attempting to parse the response");
        }
        ArrayList<IdLocation> ids = new ArrayList<IdLocation>();
        for (SRUDC dc : sru.getRecords()) {
            IdLocation id = new IdLocation();
            id.setId(dc.getKeys(SRUDC.Key.IDENTIFIER).firstElement());
            id.setRepo(dc.getKeys(SRUDC.Key.SOURCE).firstElement());
            if (dc.getKeys(SRUDC.Key.DATE).size() > 0) id.setDate(dc.getKeys(SRUDC.Key.DATE).firstElement());
            ids.add(id);
        }
        Collections.sort(ids);
        return ids;
    }

    /**
     * Gets the base XMLtape Resolver Service URL
     * @return
     *        Service BaseUrl
     */
    public URL getDefaultBaseurl() {
        return defaultBaseurl;
    }

    /**
     * Sets the base XMLtape Resolver Service URL
     * @param baseurl
     *        Service URL for repo in which resource resides <br>
     *        (e.g. http://localhost:8080/adore-xmltappe-resolver/demoTape/openurl-aDORe1)
     */
    public void setDefaultBaseurl(URL baseurl) {
        this.defaultBaseurl = baseurl;
    }

    /**
     * Main Method - Parses command line args<br>
     * 
     * Expects the following args:<br>
     *   [baseUrl]<br>
     *        base XMLtape Resolver Service URL<br>
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
        XMLTapeResolverProxy resolver;
        try {
            long s = System.currentTimeMillis();
            resolver = new XMLTapeResolverProxy(new URL(args[0]));
            Resource resource = resolver.get(args[1], args[2]);
            System.out.println("Time to get resource: " + (System.currentTimeMillis() - s));
            System.out.println("Content-type: " + resource.getContentType());
            System.out.println("Writing file to: " + args[3]);
            FileOutputStream fos = new FileOutputStream(new File(args[3]));
            fos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
            fos.write(resource.getBytes());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
