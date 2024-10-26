package org.eclipse.datatools.enablement.oda.xml.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * This class is used to manager the XML data source. If the XML data source is 
 * passed as InputStream or URL using protocol other than File, then we will make
 * a local cache for it to accelerate the processing.
 */
public class XMLDataInputStreamCreator {

    private static final String TEMPFILENAME = "tempXMLData";

    private static HashMap inputStreamCache = new HashMap();

    private URL url = null;

    /**
	 * Return an XMLDataInputStreamCreator instance according to the given key.
	 * 
	 * @param key
	 * @return
	 * @throws OdaException
	 */
    public static XMLDataInputStreamCreator getCreator(String key) throws OdaException {
        return new XMLDataInputStreamCreator(key);
    }

    /**
	 * Return an XMLDataInputStreamCreator instance according to the given key.
	 * 
	 * @param key
	 * @return
	 * @throws OdaException
	 */
    public static synchronized XMLDataInputStreamCreator getCreator(InputStream key) throws OdaException {
        if (inputStreamCache.get(key) == null) {
            XMLDataInputStreamCreator creator = new XMLDataInputStreamCreator(key);
            inputStreamCache.put(key, creator);
            return creator;
        } else {
            return (XMLDataInputStreamCreator) inputStreamCache.get(key);
        }
    }

    /**
	 * 
	 * @param is
	 * @throws OdaException
	 */
    protected XMLDataInputStreamCreator(InputStream is) throws OdaException {
        createTemporaryFile(is);
    }

    /**
	 * @param is
	 * @throws OdaException
	 */
    private void createTemporaryFile(InputStream is) throws OdaException {
        BufferedInputStream bis = new BufferedInputStream(is);
        try {
            File file = createFileWithUniqueName();
            FileOutputStream fos = new FileOutputStream(file);
            int abyte;
            while ((abyte = bis.read()) != -1) {
                fos.write(abyte);
            }
            fos.close();
            bis.close();
            url = file.toURL();
        } catch (IOException e) {
            throw new OdaException(e.getLocalizedMessage());
        }
    }

    /**
	 * @return
	 * @throws IOException 
	 */
    private File createFileWithUniqueName() throws IOException {
        File file = File.createTempFile(TEMPFILENAME, null);
        file.deleteOnExit();
        return file;
    }

    /**
	 * 
	 * @param xmlFile
	 * @throws OdaException
	 */
    protected XMLDataInputStreamCreator(String xmlFile) throws OdaException {
        URL url = null;
        try {
            File f = new File(xmlFile);
            if (f.exists()) url = f.toURL();
            this.url = url;
        } catch (IOException e) {
        }
        try {
            if (url == null) {
                url = new URL(xmlFile);
                this.createTemporaryFile(url.openStream());
            }
        } catch (MalformedURLException e) {
            throw new OdaException(e.getLocalizedMessage());
        } catch (IOException e) {
            throw new OdaException(e.getLocalizedMessage());
        }
    }

    /**
	 * 
	 * @return
	 * @throws OdaException
	 */
    public XMLDataInputStream createXMLDataInputStream() throws OdaException {
        return new XMLDataInputStream(this.url);
    }
}
