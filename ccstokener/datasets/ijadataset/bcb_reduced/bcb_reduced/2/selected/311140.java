package oxygen.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import oxygen.util.StringUtils;
import sun.misc.BASE64Encoder;

/**
 * @author  Ugorji Dick-Nwoke 
 * @version 1.0, Mar 3, 2001
 */
public class Try {

    static void methods(String[] args) throws Exception {
        Method[] methods = Try.class.getMethods();
        for (int i = 1; i < methods.length; i++) {
            System.out.println(methods[i].getName());
        }
    }

    static void encode(String[] args) throws Exception {
        String argToEncode = "a=b&c=d";
        StringBuffer s = new StringBuffer();
        String enc = URLEncoder.encode(argToEncode);
        String dec = URLDecoder.decode(enc);
        boolean eq = (dec.equals(argToEncode));
        s.append("\nArgument is: ").append(argToEncode).append("\nEncoded, it is: ").append(enc).append("\nDecoded, it is: ").append(dec).append("\nConcluding, is arg equal to decoded: ").append(eq).append("\n");
        System.out.println(s.toString());
    }

    static void calendar(String[] args) throws Exception {
        Calendar calendar = Calendar.getInstance();
        Date trialTime = new Date();
        calendar.setTime(trialTime);
        System.out.println("ERA: " + calendar.get(Calendar.ERA));
        System.out.println("YEAR: " + calendar.get(Calendar.YEAR));
        System.out.println("MONTH: " + calendar.get(Calendar.MONTH));
        System.out.println("WEEK_OF_YEAR: " + calendar.get(Calendar.WEEK_OF_YEAR));
        System.out.println("WEEK_OF_MONTH: " + calendar.get(Calendar.WEEK_OF_MONTH));
        System.out.println("DATE: " + calendar.get(Calendar.DATE));
        System.out.println("DAY_OF_MONTH: " + calendar.get(Calendar.DAY_OF_MONTH));
        System.out.println("DAY_OF_YEAR: " + calendar.get(Calendar.DAY_OF_YEAR));
        System.out.println("DAY_OF_WEEK: " + calendar.get(Calendar.DAY_OF_WEEK));
        System.out.println("DAY_OF_WEEK_IN_MONTH: " + calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH));
        System.out.println("AM_PM: " + calendar.get(Calendar.AM_PM));
        System.out.println("HOUR: " + calendar.get(Calendar.HOUR));
        System.out.println("HOUR_OF_DAY: " + calendar.get(Calendar.HOUR_OF_DAY));
        System.out.println("MINUTE: " + calendar.get(Calendar.MINUTE));
        System.out.println("SECOND: " + calendar.get(Calendar.SECOND));
        System.out.println("MILLISECOND: " + calendar.get(Calendar.MILLISECOND));
        System.out.println("ZONE_OFFSET: " + (calendar.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000)));
        System.out.println("DST_OFFSET: " + (calendar.get(Calendar.DST_OFFSET) / (60 * 60 * 1000)));
        System.out.println("Current Time, with hour reset to 3");
        calendar.clear(Calendar.HOUR_OF_DAY);
        calendar.set(Calendar.HOUR, 3);
        System.out.println("ERA: " + calendar.get(Calendar.ERA));
        System.out.println("YEAR: " + calendar.get(Calendar.YEAR));
        System.out.println("MONTH: " + calendar.get(Calendar.MONTH));
        System.out.println("WEEK_OF_YEAR: " + calendar.get(Calendar.WEEK_OF_YEAR));
        System.out.println("WEEK_OF_MONTH: " + calendar.get(Calendar.WEEK_OF_MONTH));
        System.out.println("DATE: " + calendar.get(Calendar.DATE));
        System.out.println("DAY_OF_MONTH: " + calendar.get(Calendar.DAY_OF_MONTH));
        System.out.println("DAY_OF_YEAR: " + calendar.get(Calendar.DAY_OF_YEAR));
        System.out.println("DAY_OF_WEEK: " + calendar.get(Calendar.DAY_OF_WEEK));
        System.out.println("DAY_OF_WEEK_IN_MONTH: " + calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH));
        System.out.println("AM_PM: " + calendar.get(Calendar.AM_PM));
        System.out.println("HOUR: " + calendar.get(Calendar.HOUR));
        System.out.println("HOUR_OF_DAY: " + calendar.get(Calendar.HOUR_OF_DAY));
        System.out.println("MINUTE: " + calendar.get(Calendar.MINUTE));
        System.out.println("SECOND: " + calendar.get(Calendar.SECOND));
        System.out.println("MILLISECOND: " + calendar.get(Calendar.MILLISECOND));
        System.out.println("ZONE_OFFSET: " + (calendar.get(Calendar.ZONE_OFFSET) / (60 * 60 * 1000)));
        System.out.println("DST_OFFSET: " + (calendar.get(Calendar.DST_OFFSET) / (60 * 60 * 1000)));
    }

    static void urlparams(String[] args) throws Exception {
        java.net.URL url = new URL("http://lyx.cbs:9877/dir1/dir2/ac.jsp?a=b&c=d");
        System.out.println("getFile : " + url.getFile());
        System.out.println("getHost : " + url.getHost());
        System.out.println("getPath : " + url.getPath());
        System.out.println("getPort : " + url.getPort());
        System.out.println("getProtocol : " + url.getProtocol());
        System.out.println("getQuery : " + url.getQuery());
        System.out.println("getRef : " + url.getRef());
        System.out.println("getUserInfo : " + url.getUserInfo());
    }

    static void socket2url(String[] args) throws Exception {
        String urlstr = "http://www.yahoo.com:80/";
        URL url = new URL(urlstr);
        String host = url.getHost();
        int port = url.getPort();
        String file = url.getFile();
        Socket s = new Socket(host, port);
        InputStream is = s.getInputStream();
        OutputStream os = s.getOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
        pw.println("GET " + file + " HTTP/1.1");
        pw.flush();
        int len = -1;
        byte[] ca = new byte[1024];
        StringBuffer contents = new StringBuffer();
        System.out.println("--- OUTPUT DIRECT ---");
        while ((len = is.read(ca, 0, 1024)) != -1) {
            System.out.write(ca, 0, len);
            System.out.flush();
        }
        System.out.println("--- OUTPUT DIRECT ---");
        pw.close();
        is.close();
        System.out.println("--- OUTPUT STRINGBUFFER ---");
        System.out.println(contents.toString());
        System.out.println("--- OUTPUT STRINGBUFFER ---");
    }

    static void socket(String[] args) throws Exception {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        Socket s = new Socket(host, port);
        System.out.println("Socket connection made to " + host + " at " + port);
        System.out.println(s);
        s.close();
    }

    static void responsecode(String[] args) throws Exception {
        String urlstr = "http://lynwood:8801/wlservlet-functests/putSessionData";
        URL url = new URL(urlstr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.connect();
        int responseCode = con.getResponseCode();
        System.out.println("responseCode: " + responseCode);
        String key = null;
        String value = null;
        int i = 1;
        while ((key = con.getHeaderFieldKey(i)) != null) {
            value = con.getHeaderField(i);
            System.out.println("PAIR: " + key + " -> " + value + " [NL]");
            i++;
        }
        responseCode = con.getResponseCode();
        System.out.println("responseCode: " + responseCode);
        InputStreamReader isr = new InputStreamReader(con.getInputStream());
        int len = -1;
        char[] ca = new char[1024];
        StringBuffer contents = new StringBuffer();
        while ((len = isr.read(ca, 0, 1024)) != -1) {
            contents.append(ca, 0, len);
        }
        String result = contents.toString().trim();
        System.out.println("--- OUTPUT STRINGBUFFER ---");
        System.out.println("--- OUTPUT STRINGBUFFER ---");
    }

    static void xml1(String[] args) throws Exception {
        System.setProperty("javax.xml.parsers.SAXParserFactory", "weblogic.xml.jaxp.RegistrySAXParserFactory");
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "weblogic.xml.jaxp.RegistryDocumentBuilderFactory");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        System.out.println("+");
        DocumentBuilder builder = dbf.newDocumentBuilder();
        File f = new File(args[0]);
        Document doc = builder.parse(f);
        Node docElement = doc.getDocumentElement();
        listNodes(docElement);
        System.out.println("About to write out docElement to a printWriter");
        PrintWriter writer = new PrintWriter(new FileWriter(args[0] + ".out.xml"));
        writer.println(docElement.toString());
        writer.flush();
        writer.close();
    }

    static void xml2(String[] args) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        File f = new File(args[0]);
        Document doc = builder.parse(f);
        System.out.println("About to write out docElement to a printWriter");
        PrintWriter writer = new PrintWriter(new FileWriter(args[0] + ".out.xml"));
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        writer.flush();
        writer.close();
    }

    static void listNodes(Node node) throws Exception {
        System.out.println("Node Name: " + node.getNodeName());
        System.out.println("Node Value: '" + node.getNodeValue() + "'");
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            listNodes(n);
        }
    }

    static void listargs(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            System.out.println("Args[" + i + "] = '" + args[i] + "'");
        }
    }

    static void seeFileName(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            try {
                File f = new File(args[i]);
                System.out.println("file name for " + args[i] + " = " + f.getName());
                System.out.println("file path for " + args[i] + " = " + f.getPath());
                f = f.getAbsoluteFile();
                System.out.println("abs file name for " + args[i] + " = " + f.getName());
                System.out.println("abs file path for " + args[i] + " = " + f.getPath());
                f = f.getCanonicalFile();
                System.out.println("can file name for " + args[i] + " = " + f.getName());
                System.out.println("can file path for " + args[i] + " = " + f.getPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void base64Encode(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            String encstr = new BASE64Encoder().encode(args[i].getBytes());
            StringBuffer buf = new StringBuffer().append("'").append(args[i]).append("' => '").append(encstr).append("'");
            System.out.println(buf.toString());
        }
    }

    static void dos2unix(String[] args) throws Exception {
        PrintWriter pw = new PrintWriter(new FileWriter(args[1]));
        File f = new File(args[0]);
        char[] fc = new char[1024];
        FileReader fr = new FileReader(f);
        int num = -1;
        while ((num = fr.read(fc)) != -1) {
            String s = new String(fc, 0, num);
            String s2 = StringUtils.replaceInString(s, "\r", "");
            pw.print(s2);
            pw.flush();
        }
        fr.close();
        pw.close();
    }

    static void dos2unixOld(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        PrintWriter pw = new PrintWriter(new FileWriter(args[1]));
        String lineRead = null;
        while ((lineRead = br.readLine()) != null) {
            lineRead = StringUtils.replaceInString(lineRead, "\r", "");
            pw.print(lineRead);
            pw.print("\n");
        }
        pw.flush();
        br.close();
        pw.close();
    }

    static void concatnull(String[] args) throws Exception {
        String s1 = "1. concat null = [" + null + "]";
        StringBuffer sb2 = new StringBuffer().append("2. append null = [").append((String) null).append("]");
        System.out.println(s1);
        System.out.println(sb2.toString());
    }
}
