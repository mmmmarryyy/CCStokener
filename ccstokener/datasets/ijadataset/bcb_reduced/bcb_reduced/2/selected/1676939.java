package gov.lanl.ockham.client.app;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import org.apache.log4j.*;
import gov.lanl.ockham.iesrdata.IESRCollection;
import gov.lanl.ockham.iesrdata.IESRService;
import gov.lanl.registryclient.parser.SerializationException;

/**
 * Ockham Registry abstraction providing get, post, list, delete interfaces
 * <p>
 * A java client may interact with the registry through this class
 * 
 */
public class Registry {

    URL baseurl;

    static Logger logger = Logger.getLogger(Registry.class);

    public static final int BUFFER_SIZE = 16 * 1024;

    /**
	 * Creates new Registry instance for the provided baseUrl
	 * @param baseurl
	 *            baseurl of repository PutRecord interface, 
     *            (e.g. http://localhost:8080/adore-service-registry/PutRecordHandler)
	 */
    public Registry(URL baseurl) {
        if (baseurl == null) throw new NullPointerException("empty baseurl");
        this.baseurl = baseurl;
    }

    public static void main(String[] args) {
        String url = args[0];
        String dir = args[1];
        try {
            Registry reg = new Registry(new URL(url));
            ArrayList<File> files = getFileList(dir);
            for (File file : files) {
                String f = getContents(file);
                if (f != null && reg.doPost(f)) logger.info("Registration Successful: " + file.getAbsolutePath()); else logger.info("Registration Failed: " + file.getAbsolutePath());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Gets the IESRCollection information for the provided Ockham identifier
	 * 
	 * @param identifier
     *         the ockham identifier for the collection
	 * @return
     *         an IESRCollection instance
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws IESRSerializationException
	 */
    public IESRCollection getCollection(String identifier) throws MalformedURLException, IOException, SerializationException {
        String str = doGet(identifier);
        IESRCollection coll = new IESRCollection();
        coll.read(new ByteArrayInputStream(str.getBytes()));
        return coll;
    }

    /**
	 * Gets Ockham registry information for the provided identifier
	 * 
	 * @param identifier
     *         an OCKHAM identifier
	 * @return
     *         XML formatted string
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws IESRSerializationException
	 */
    public String get(String identifier) throws MalformedURLException, IOException, SerializationException {
        return doGet(identifier);
    }

    /**
     * Gets the IESRService information for the provided Ockham identifier
     * 
     * @param identifier
     *         the ockham identifier for the service
     * @return
     *         an IESRCollection instance
     * @throws MalformedURLException
     * @throws IOException
     * @throws IESRSerializationException
     */
    public IESRService getService(String identifier) throws MalformedURLException, IOException, SerializationException {
        String str = doGet(identifier);
        IESRService srv = new IESRService();
        srv.read(new ByteArrayInputStream(str.getBytes()));
        return srv;
    }

    /**
	 * Posts an IESRCollection instance to the Ockham Registry
	 * 
	 * @param coll
     *         the collection instance to be registered
	 * @return
     *         true for success
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws IESRSerializationException
	 */
    public boolean put(IESRCollection coll) throws MalformedURLException, IOException, SerializationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        coll.write(out);
        out.close();
        return doPost(out.toString());
    }

    /**
     * Posts an IESRService instance to the Ockham Registry
     * 
     * @param service
     *         the service instance to be registered
     * @return
     *         true for success
     * @throws MalformedURLException
     * @throws IOException
     * @throws IESRSerializationException
     */
    public boolean put(IESRService service) throws MalformedURLException, IOException, SerializationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(out);
        out.close();
        return doPost(out.toString());
    }

    /**
     * Deletes a record from the Ockham Registry
     * 
     * @param identifier
     *         the ockham identifier of the record to be deleted
     * @return
     *         true for success
     * @throws MalformedURLException
     */
    public boolean delete(String identifier) throws MalformedURLException, IOException, SerializationException {
        return doDelete(identifier);
    }

    /**
	 * Simple HTTP Request for an identifer
	 * 
	 * @param identifier
     *         id of record to be obtained
	 * @return
     *        xml string of record
	 * @throws IOException
	 * @throws MalformedURLException
	 */
    private String doGet(String identifier) throws IOException, MalformedURLException {
        URL url = new URL(baseurl.toString() + "/" + identifier);
        logger.debug("get " + url.toString());
        HttpURLConnection huc = (HttpURLConnection) (url.openConnection());
        BufferedReader reader = new BufferedReader(new InputStreamReader(huc.getInputStream()));
        StringWriter writer = new StringWriter();
        char[] buffer = new char[BUFFER_SIZE];
        int count = 0;
        while ((count = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, count);
        }
        writer.close();
        reader.close();
        int code = huc.getResponseCode();
        logger.debug(" get result" + code);
        if (code == 200) {
            return writer.toString();
        } else throw new IOException("cannot get " + url.toString());
    }

    /**
	 * HTTP Post Request, used to register new collection/service objects
	 * 
	 * @param content
     *         collection/service record as xml string
	 * @return
     *         true for success
	 * @throws IOException
	 */
    private boolean doPost(String content) throws IOException {
        logger.debug("Service Registry PutRecordHandler: " + baseurl.toString());
        logger.debug("**** Service Registry PutRecord Request ****\n " + content);
        HttpURLConnection huc = (HttpURLConnection) (baseurl.openConnection());
        huc.setRequestMethod("POST");
        huc.setDoOutput(true);
        ByteArrayInputStream in = new ByteArrayInputStream(content.getBytes());
        OutputStream out = huc.getOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedInputStream bis = new BufferedInputStream(in);
        int count = 0;
        while ((count = bis.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        out.close();
        int code = huc.getResponseCode();
        logger.debug("Service Registry Response Code: " + code);
        if (code == 200) {
            return true;
        } else return false;
    }

    /**
     * HTTP Request to delete a collection/service record
     * 
     * @param identifier
     *         id of record to be deleted
     * @return
     *         true for success
     * @throws IOException
     * @throws MalformedURLException
     */
    private boolean doDelete(String identifier) throws IOException, MalformedURLException {
        URL url = new URL(baseurl.toString() + "/" + identifier);
        HttpURLConnection huc = (HttpURLConnection) (url.openConnection());
        huc.setRequestMethod("DELETE");
        huc.connect();
        if (huc.getResponseCode() == 200) {
            return true;
        } else return false;
    }

    /**
     * Gets a ArrayList of File objects provided a dir or file path.
     * @param filePath
     *        Absolute path to file or directory
     * @param fileFilter
     *        Filter dir content by extention; allows "null"
     * @param recursive
     *        Recursively search for files
     * @return
     *        ArrayList of File objects matching specified criteria.
     */
    private static ArrayList<File> getFileList(String filePath) {
        ArrayList<File> files = new ArrayList<File>();
        File file = new File(filePath);
        if (file.exists() && file.isDirectory()) {
            File[] fa = file.listFiles();
            for (int i = 0; i < fa.length; i++) {
                if (fa[i].isFile()) files.add(fa[i]);
            }
        } else if (file.exists() && file.isFile()) {
            files.add(file);
        }
        return files;
    }

    private static String getContents(File src) {
        InputStream in = null;
        StringBuffer sb = new StringBuffer();
        byte[] buf = null;
        int bufLen = 20000 * 1024;
        try {
            in = new BufferedInputStream(new FileInputStream(src));
            buf = new byte[bufLen];
            byte[] tmp = null;
            int len = 0;
            while ((len = in.read(buf, 0, bufLen)) != -1) {
                tmp = new byte[len];
                System.arraycopy(buf, 0, tmp, 0, len);
                sb.append(new String(tmp));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception e) {
            }
        }
        return sb.toString();
    }
}
