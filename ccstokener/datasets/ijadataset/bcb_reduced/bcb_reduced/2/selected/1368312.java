package org.s3b.search.indexing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.jeromedl.beans.RequirementType;
import org.jeromedl.beans.ToolsHelper;
import org.jeromedl.description.query.PropertiesMapper;
import org.jeromedl.stringdict.MimeTypes;
import org.jeromedl.stringdict.Statics;

/**
 * Indexes textual content of a remote resource  
 * 
 * @author    Sebastian Ryszard Kruk, Sławek Grzonkowski,
 */
public class URIIndexer implements Indexer {

    /**
	 * list of required resources
	 */
    private Map<String, RequirementType> requirements = new HashMap<String, RequirementType>();

    /**
	 * 
	 */
    private URIIndexer() {
        synchronized (URIIndexer.class) {
            if (requirements == null) {
                requirements = new HashMap<String, RequirementType>();
                requirements.put("java.net.URI", RequirementType.RT_JAVA);
            }
        }
    }

    public void index(String contentURI, String resourceURI, Document documentIndex) {
        String ctype = getContentType(contentURI);
        if (ctype != null && ctype.startsWith(MimeTypes.APPLICATION_PDF)) {
            try {
                String fn = File.createTempFile(TEST, null, null).getPath();
                if (downloadFile(contentURI, fn)) PdfIndexer.getInstance().index(fn, resourceURI, documentIndex);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (ctype != null && ctype.startsWith(MimeTypes.APPLICATION_X_SHOCKWAVE_FLASH)) {
            try {
                String fn = File.createTempFile(TEST, null, null).getPath();
                if (downloadFile(contentURI, fn)) SwfIndexer.getInstance().index(fn, resourceURI, documentIndex);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (ctype != null && ctype.startsWith(MimeTypes.TEXT_HTML)) {
            try {
                String fn = File.createTempFile(TEST, null, null).getPath();
                if (downloadFile(contentURI, fn)) HtmlIndexer.getInstance().index(fn, resourceURI, documentIndex);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (ctype != null && ctype.startsWith(MimeTypes.TEXT_PLAIN)) {
            try {
                String fn = File.createTempFile(TEST, null, null).getPath();
                if (downloadFile(contentURI, fn)) {
                    documentIndex.add(Field.Text(PropertiesMapper.S_CONTENT, new FileReader(fn)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (contentURI != null) documentIndex.add(Field.Text(PropertiesMapper.S_CONTENT, contentURI));
    }

    public boolean canOperate() {
        return ToolsHelper.checkConditions(requirements);
    }

    public Map<String, RequirementType> getRequirements() {
        return requirements;
    }

    /**
	 * @return instance of the structure indexer
	 */
    public static URIIndexer getInstance() {
        return new URIIndexer();
    }

    public static String getContentType(String urladr) {
        String out = Statics.S_NULL;
        try {
            URL url = new URL(urladr);
            URLConnection urlconnection = url.openConnection();
            out = urlconnection.getContentType();
        } catch (Exception e) {
        }
        return out;
    }

    public static boolean downloadFile(String srcUri, String srcDest) {
        try {
            URL url = new URL(srcUri);
            InputStream is = url.openStream();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(srcDest));
            byte[] buff = new byte[10000];
            int b;
            while ((b = is.read(buff)) > 0) bos.write(buff, 0, b);
            is.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * test
	 */
    protected static final String TEST = "test";
}
