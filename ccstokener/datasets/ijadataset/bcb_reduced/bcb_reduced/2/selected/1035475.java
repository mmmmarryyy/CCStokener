package es.gva.cit.catalog.protocols;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import es.gva.cit.catalog.metadataxml.XMLTree;
import es.gva.cit.catalog.utils.Strings;

/**
 * This class implement the HTTP Post protocol.
 * 
 * 
 * @author Jorge Piera Llodra (piera_jor@gva.es)
 */
public class HTTPPostProtocol implements IProtocols {

    /**
 * @return 
 * @param url 
 * @param message 
 * @param firstRecord 
 */
    public Collection doQuery(URL url, Object message, int firstRecord) {
        String body = (String) message;
        ByteArrayInputStream output = null;
        try {
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestProperty("SOAPAction", "post");
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            OutputStreamWriter w = new OutputStreamWriter(c.getOutputStream(), "UTF-8");
            w.write(body);
            w.flush();
            InputStream is = c.getInputStream();
            byte[] buf = new byte[1024];
            int len;
            String str = "";
            while ((len = is.read(buf)) > 0) {
                str = str + new String(buf, 0, len);
            }
            str = Strings.replace(str, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            System.out.println(str);
            output = new ByteArrayInputStream(str.getBytes());
        } catch (IOException e) {
            return null;
        }
        Collection col = new java.util.ArrayList();
        col.add(XMLTree.xmlToTree(output));
        return col;
    }

    public void doQuery(URL url, Object message, int firstRecord, String fileName) {
        String body = (String) message;
        FileOutputStream output = null;
        try {
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestProperty("SOAPAction", "post");
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            OutputStreamWriter w = new OutputStreamWriter(c.getOutputStream(), "UTF-8");
            w.write(body);
            w.flush();
            InputStream is = c.getInputStream();
            byte[] buf = new byte[1024];
            int len;
            String str = "";
            while ((len = is.read(buf)) > 0) {
                str = str + new String(buf, 0, len);
            }
            System.out.println(str);
            output = new FileOutputStream(new File(fileName));
            output.write(str.getBytes());
            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
