package net.unicon.ipac;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.net.ssl.*;

public class IPACServletDriver {

    public static URL getURL(String urlLocation) {
        URL url = null;
        try {
            url = new URL(urlLocation);
        } catch (MalformedURLException mfe) {
            System.out.println("UNABLE TO connection to url");
            mfe.printStackTrace();
        }
        return url;
    }

    public static String retrieveFile(String location) throws IOException {
        String blah = null;
        try {
            URI uri = new URI(location);
            File file = new File(uri);
        } catch (URISyntaxException use) {
            use.printStackTrace();
        }
        return blah;
    }

    public static String securePost(String xml, String urlLocation) throws IOException {
        URL url = getURL(urlLocation);
        if (url == null) return null;
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoOutput(true);
        OutputStream outStream = conn.getOutputStream();
        byte[] bites = (new String(xml)).getBytes();
        outStream.write(bites);
        outStream.flush();
        outStream.close();
        InputStream inStream = conn.getInputStream();
        String responseMsg = __getHttpResponse(inStream);
        inStream.close();
        conn.disconnect();
        return responseMsg;
    }

    public static String post(String xml, String urlLocation) throws IOException {
        URL url = getURL(urlLocation);
        if (url == null) return null;
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        OutputStream outStream = conn.getOutputStream();
        byte[] bites = (new String(xml)).getBytes();
        outStream.write(bites);
        outStream.flush();
        outStream.close();
        InputStream inStream = conn.getInputStream();
        String responseMsg = __getHttpResponse(inStream);
        inStream.close();
        conn.disconnect();
        return responseMsg;
    }

    private static String __getHttpResponse(InputStream inStream) throws IOException {
        StringBuffer strBuff = new StringBuffer();
        BufferedReader input = new BufferedReader(new InputStreamReader(inStream));
        String data = null;
        while ((data = input.readLine()) != null) {
            strBuff.append(data);
        }
        return strBuff.toString();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("IPAC Servlet Driver");
        if (args.length <= 0) {
            System.out.println("Please provide the web servlet");
            System.out.println("\ti.e. http://localhost:8080/ipac");
            return;
        }
        String httpServer = args[0];
        String responseMsg = null;
        System.out.println("SERVER : " + httpServer);
        if (httpServer.startsWith("https://")) responseMsg = securePost(data(), httpServer); else if (httpServer.startsWith("http://")) responseMsg = post(data(), httpServer); else System.out.println("Server location need to started with http or https");
        System.out.println(responseMsg);
        System.exit(1);
    }

    public static String data() {
        String data = "<?xml version = \"1.0\" encoding = \"UTF-8\"?><!DOCTYPE enterprise SYSTEM \"http://eng1497.unicon.net:8080/uPortal2.1.2/ims_epv1p1.dtd\"><enterprise><properties>        <datasource>Dunelm Services Limited</datasource>        <target>Telecommunications LMS</target>        <type>DATABASE UPDATE</type>        <datetime>2001-08-08</datetime>    </properties>    <person recstatus = \"1\">        <comments>Add a new Person record.</comments>        <sourcedid>            <source>Dunelm Services Limited</source>            <id>CK1</id>        </sourcedid>        <name>            <fn>Clark Kent</fn>            <sort>Kent, C</sort>            <nickname>Superman</nickname>        </name>        <demographics>            <gender>2</gender>        </demographics>        <adr>            <extadd>The Daily Planet</extadd>            <locality>Metropolis</locality>            <country>USA</country>        </adr>    </person>    <person recstatus = \"2\">        <comments>Update a previously created record.</comments>        <sourcedid>            <source>Dunelm Services Limited</source>            <id>CS1</id>        </sourcedid>        <name>            <fn>Colin Smythe</fn>            <sort>Smythe, C</sort>            <nickname>Colin</nickname>            <n>                <family>Smythe</family>                <given>Colin</given>                <other>Manfred</other>                <other>Wingarde</other>                <prefix>Dr.</prefix>                <suffix>C.Eng</suffix>                <partname partnametype = \"Initials\">C.M.W.</partname>            </n>        </name>        <demographics>            <gender>2</gender>            <bday>1958-02-18</bday>            <disability>None.</disability>        </demographics>        <email>colin@dunelm.com</email>        <url>http://www.dunelm.com</url>        <tel teltype = \"Mobile\">4477932335019</tel>        <adr>            <extadd>Dunelm Services Limited</extadd>            <street>34 Acorn Drive</street>            <street>Stannington</street>            <locality> Sheffield</locality>            <region>S.Yorks</region>            <pcode>S7 6WA</pcode>            <country>UK</country>        </adr>        <photo imgtype = \"gif\">            <extref>http://www.dunelm.com/staff/colin2.gif</extref>        </photo>        <institutionrole primaryrole = \"No\" institutionroletype = \"Alumni\"/>        <datasource>dunelm:colinsmythe:1</datasource>    </person>    <person recstatus = \"3\">        <comments>Delete this record.</comments>        <sourcedid>            <source>Dunelm Services Limited</source>            <id>LL1</id>        </sourcedid>        <name>            <fn>Lois Lane</fn>            <sort>Lane, L</sort>        </name>    </person></enterprise>";
        return data;
    }
}
