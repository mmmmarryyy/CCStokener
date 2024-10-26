package utility;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class CRemoteXML {

    private static String charsetName = "UTF-8";

    public Document extractData(String p_url) {
        for (int i = 0; i < 9; i++) {
            CExtractHelper l_extractResult = getData(p_url);
            if (l_extractResult.m_generalFailure) {
                return null;
            }
            if (l_extractResult.m_timeoutFailure != true && l_extractResult.m_document != null) {
                return l_extractResult.m_document;
            }
        }
        return null;
    }

    private CExtractHelper getData(String p_url) {
        CExtractHelper l_extractHelper = new CExtractHelper();
        URL l_url;
        HttpURLConnection l_connection;
        try {
            System.out.println("Getting [" + p_url + "]");
            l_url = new URL(p_url);
            try {
                URLConnection l_uconn = l_url.openConnection();
                l_connection = (HttpURLConnection) l_uconn;
                l_connection.setConnectTimeout(2000);
                l_connection.setReadTimeout(2000);
                l_connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.1) Gecko/20061204 Firefox/2.0.0.1");
                l_connection.connect();
                int l_responseCode = l_connection.getResponseCode();
                String response = l_connection.getResponseMessage();
                System.out.println("HTTP/1.x " + l_responseCode + " " + response);
                for (int j = 1; ; j++) {
                    String l_header = l_connection.getHeaderField(j);
                    String l_key = l_connection.getHeaderFieldKey(j);
                    if (l_header == null || l_key == null) {
                        break;
                    }
                }
                InputStream l_inputStream = new BufferedInputStream(l_connection.getInputStream());
                CRemoteXML l_parser = new CRemoteXML();
                try {
                    Document l_document = l_parser.parse(l_inputStream);
                    PrintWriter l_writerOut = new PrintWriter(new OutputStreamWriter(System.out, charsetName), true);
                    OutputFormat l_format = OutputFormat.createPrettyPrint();
                    XMLWriter l_xmlWriter = new XMLWriter(l_writerOut, l_format);
                    l_xmlWriter.write(l_document);
                    l_xmlWriter.flush();
                    l_connection.disconnect();
                    l_extractHelper.m_document = l_document;
                    return l_extractHelper;
                } catch (DocumentException e) {
                    e.printStackTrace();
                    l_connection.disconnect();
                    System.out.println("XML parsing issue");
                    l_extractHelper.m_generalFailure = true;
                }
            } catch (SocketTimeoutException e) {
                l_extractHelper.m_timeoutFailure = true;
                System.out.println("Timed out");
            } catch (IOException e) {
                e.printStackTrace();
                l_extractHelper.m_generalFailure = true;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            l_extractHelper.m_generalFailure = true;
        }
        return l_extractHelper;
    }

    private Document parse(InputStream s) throws DocumentException {
        SAXReader l_reader = new SAXReader();
        Document l_document = l_reader.read(s);
        return l_document;
    }

    class CExtractHelper {

        public Document m_document;

        public boolean m_timeoutFailure;

        public boolean m_generalFailure;

        CExtractHelper() {
            m_document = null;
            m_timeoutFailure = false;
            m_generalFailure = false;
        }
    }
}
