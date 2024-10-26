package org.integration.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Read the SOAP envelop fiel passed as second parameter, pass it to SOAP
 * endpoint passed as the first paramenter, and print out the SOAP envelope as a
 * response.
 * 
 * @author khurshid
 */
public final class SOAPClient4XG {

    /**
     * private constructor.
     */
    private SOAPClient4XG() {
    }

    /**
     * Main.
     * 
     * @param args
     *            string[]
     * @throws Exception
     *             exception
     */
    public static void main(final String[] args) throws Exception {
        String soapURL = "http://localhost:8080/services/BusinessManager_QueryProductCatalog?wsdl";
        String xmlFile2Send = "properties/soap.xml";
        String soapAction = "";
        if (args.length > 2) {
            soapAction = args[2];
        }
        URL url = new URL(soapURL);
        URLConnection connection = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection) connection;
        FileInputStream fin = new FileInputStream(xmlFile2Send);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        copy(fin, bout);
        fin.close();
        byte[] b = bout.toByteArray();
        httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
        httpConn.setRequestProperty("Content-Type", "application/soap+xml; charset=UTF-8");
        httpConn.setRequestProperty("SOAPAction", soapAction);
        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        OutputStream out = httpConn.getOutputStream();
        out.write(b);
        out.close();
        InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
        BufferedReader in = new BufferedReader(isr);
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder db;
        db = factory.newDocumentBuilder();
        org.xml.sax.InputSource inStream = new org.xml.sax.InputSource();
        inStream.setCharacterStream(new java.io.StringReader(response.toString()));
        System.out.println(response.toString());
        org.w3c.dom.Document doc = db.parse(inStream);
        doc.getDocumentElement().normalize();
        org.w3c.dom.NodeList uuidList = doc.getElementsByTagName("uuid");
        for (int k = 0; k < uuidList.getLength(); k++) {
            org.w3c.dom.Node key = uuidList.item(k);
            if (key.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                org.w3c.dom.Element valueElement = (org.w3c.dom.Element) key;
                org.w3c.dom.NodeList fstNmElmntLst = valueElement.getElementsByTagName("value");
                org.w3c.dom.Element fstNmElmnt = (org.w3c.dom.Element) fstNmElmntLst.item(0);
                org.w3c.dom.NodeList fstNm = fstNmElmnt.getChildNodes();
                System.out.println("SLATemplateID : " + ((org.w3c.dom.Node) fstNm.item(0)).getNodeValue());
            }
        }
    }

    /**
     * copy method.
     * 
     * @param in
     *            streamInput
     * @param out
     *            streamOutput
     * @throws IOException
     *             exception
     */
    public static void copy(final InputStream in, final OutputStream out) throws IOException {
        synchronized (in) {
            synchronized (out) {
                byte[] buffer = new byte[256];
                while (true) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
