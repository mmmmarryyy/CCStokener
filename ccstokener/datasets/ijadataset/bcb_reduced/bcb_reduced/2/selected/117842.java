package org.openthinclient.common.model.schema.provider;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.openthinclient.common.model.schema.Schema;

/**
 * @author levigo
 */
public class HTTPSchemaProvider extends AbstractSchemaProvider {

    private static final Logger logger = Logger.getLogger(HTTPSchemaProvider.class);

    private final URL baseURL;

    /**
	 * @throws MalformedURLException
	 * 
	 */
    public HTTPSchemaProvider(String hostname) throws MalformedURLException {
        baseURL = new URL("http", hostname, 8080, "/openthinclient/files/" + SCHEMA_PATH + "/");
        if (logger.isDebugEnabled()) logger.debug("Using schema base url: " + baseURL);
    }

    public boolean checkAccess() {
        try {
            URLConnection openConnection = baseURL.openConnection();
            String contentType = openConnection.getContentType();
            return contentType.startsWith("text/plain;");
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * @param profileTypeName
	 * @return
	 * @throws SchemaLoadingException
	 */
    @Override
    protected List<Schema> loadDefaultSchema(String profileTypeName) throws SchemaLoadingException {
        List<Schema> schemas = new ArrayList<Schema>();
        try {
            loadFromURL(schemas, new URL(baseURL, profileTypeName + ".xml"));
        } catch (Throwable e) {
            throw new SchemaLoadingException("Could not fetch schema from file service", e);
        }
        return schemas;
    }

    /**
	 * @param schemas
	 * @param url
	 * @throws IOException
	 * @throws SchemaLoadingException
	 */
    private void loadFromURL(List<Schema> schemas, URL url) throws IOException, SchemaLoadingException {
        if (logger.isDebugEnabled()) logger.debug("Trying to load schema from " + url);
        URLConnection con = url.openConnection();
        if (con.getContentType().startsWith("application/octet-stream")) schemas.add(loadSchema(con.getInputStream()));
    }

    /**
	 * @param profileTypeName
	 * @return
	 * @throws SchemaLoadingException
	 */
    @Override
    protected List<Schema> loadAllSchemas(String profileTypeName) throws SchemaLoadingException {
        List<Schema> schemas = new ArrayList<Schema>();
        try {
            URL dirURL = new URL(baseURL, profileTypeName + "/");
            if (logger.isDebugEnabled()) logger.debug("Trying to load all schemas for " + profileTypeName + " from " + dirURL);
            URLConnection con = dirURL.openConnection();
            InputStream inputStream = con.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "ISO-8859-1"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("F")) {
                    String filename = line.substring(2);
                    if (filename.endsWith(".xml")) loadFromURL(schemas, new URL(dirURL, filename));
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            if (logger.isDebugEnabled()) logger.debug("No schemas found for " + profileTypeName);
        } catch (Throwable e) {
            logger.error("Could not fetch schema from file service", e);
            throw new SchemaLoadingException("Could not fetch schema from file service", e);
        }
        return schemas;
    }

    @Override
    public void reload() {
        super.reload();
        try {
            loadAllSchemas("application");
        } catch (SchemaLoadingException e) {
            e.printStackTrace();
        }
    }
}
