package api.server.editWSDL;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Benedikt Ley
 * 
 */
public class WSDLParser {

    public static void addDocumentation(String wsdlURL, String[][] doc) throws Exception {
        String template = "";
        for (int i = 0; i < doc.length; i++) {
            template += "\t<xsl:template match=\"" + doc[i][0] + "\">\n" + "\t\t<xsl:copy>\n" + "\t\t\t<xsl:apply-templates select=\"@*|node()\"/>\n" + "\t\t<documentation>" + doc[i][1] + "</documentation>\n" + "\t\t</xsl:copy>\n" + "\t</xsl:template>\n\n";
        }
        String xsltOut = "<xsl:stylesheet version=\"1.0\"\n" + "\txmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" + "\txmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\"\n" + "\txmlns=\"http://schemas.xmlsoap.org/wsdl/\">\n\n" + "\t<xsl:output method=\"xml\" />\n\n" + template + "\t<xsl:template match=\"/|@*|node()\">\n" + "\t\t<xsl:copy>\n" + "\t\t\t<xsl:apply-templates select=\"@*|node()\"/>\n" + "\t\t</xsl:copy>\n" + "\t</xsl:template>\n" + "</xsl:stylesheet>";
        String[] pathArray = getPath(wsdlURL);
        File directory = new File(pathArray[0]);
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }
        File outFile = new File(pathArray[0] + pathArray[1]);
        FileWriter out = new FileWriter(outFile);
        out.write(xsltOut);
        out.close();
    }

    public static String[] getPath(String wsdlURL) {
        String tempPathString = wsdlURL.replaceAll(".*\\b://www.\\b", "");
        tempPathString = tempPathString.replaceAll(".*\\b://\\b", "");
        String path = "tmp/xslt_transform/";
        String[] helpArray = tempPathString.split("/");
        String[] helpArray2 = helpArray[0].split("\\.");
        for (int i = helpArray2.length - 1; i >= 0; i--) {
            if (helpArray2[i].length() > 0) path += helpArray2[i] + "/";
        }
        for (int i = 1; i < helpArray.length - 1; i++) {
            if (helpArray[i].length() > 0) path += helpArray[i] + "/";
        }
        String[] pathArray = new String[2];
        pathArray[0] = path;
        pathArray[1] = helpArray[helpArray.length - 1];
        return pathArray;
    }

    public static String[] getWSDLNamespaces(String wsdlURL) {
        String wsdl = getOriginalWSDL(wsdlURL);
        String[] splitWSDL = wsdl.split("xmlns:");
        int numberOfNamespaces = 0;
        String[] tempArray = new String[splitWSDL.length - 1];
        for (int i = 1; i < splitWSDL.length; i++) {
            int startIndex = splitWSDL[i].indexOf("\"");
            int endIndex = splitWSDL[i].indexOf("\"", startIndex + 1) + 1;
            String namespace = "xmlns:" + splitWSDL[i].substring(0, endIndex);
            for (int j = 0; j < i; j++) {
                if (namespace.equals(tempArray[j])) {
                    namespace = null;
                    break;
                }
            }
            if (namespace != null) numberOfNamespaces++;
            tempArray[i - 1] = namespace;
        }
        String[] namespaces = new String[numberOfNamespaces];
        for (int i = 0; i < numberOfNamespaces; i++) {
            namespaces[i] = tempArray[i];
        }
        return namespaces;
    }

    public static String getTransformedWSDL(String wsdlURL) throws Exception {
        String[] pathArray = getPath(wsdlURL);
        String xsltFile = pathArray[0] + pathArray[1];
        String result = XSLTParser.transformXML(wsdlURL, xsltFile);
        return result;
    }

    public static String getOriginalWSDL(String wsdlURL) {
        try {
            URL url = new URL(wsdlURL);
            java.net.HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream input = conn.getInputStream();
            final int BUFFERSIZE = 4096;
            byte[] buffer = new byte[BUFFERSIZE];
            OutputStream output = new ByteArrayOutputStream();
            while (true) {
                int read = input.read(buffer);
                if (read == -1) {
                    break;
                }
                output.write(buffer, 0, read);
            }
            output.close();
            input.close();
            conn.disconnect();
            String s = output.toString();
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            String source = "c:\\test.wsdl";
            String[][] docu = new String[2][2];
            docu[0][0] = "//wsdl:message[@name='doGoogleSearch']/wsdl:part[@name='key']";
            docu[0][1] = "Das ist eine Testdokumentation";
            docu[1][0] = "//wsdl:portType/wsdl:operation[@name='doGoogleSearch']";
            docu[1][1] = "Das ist eine Testdokumentation";
            addDocumentation(source, docu);
            System.out.println(getTransformedWSDL(source));
        } catch (Exception e) {
            System.out.print(e.getClass());
            System.out.print(e.getMessage());
        }
    }
}
