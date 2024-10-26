package oxygen.tool;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Precompiles all the jsp and tell us which are broken
 * <xmp>To see Usage Information: java weblogic.qa.taskmgr.TestJSPFiles</xmp> 
 * fName is the list of files with the paths of JSP files to precompile
 * host and port are the server host and port respectively
 * 
 * Currently, this is only geared towards FORM-based authenticated sites
 *
 * @author  Ugorji Dick-Nwoke ugorji.dick-nwoke@bea.com
 * @version 1.0, August 3, 2001
 */
public class TestJSPFiles {

    private static String HELP_MESSAGE = null;

    public boolean DEBUG = false;

    public List jspFiles;

    public String host;

    public int port;

    public String username;

    public String password;

    public String cookieString;

    public String formAuthTarget = "/j_security_check";

    static {
        String lsep = System.getProperty("line.separator");
        HELP_MESSAGE = "Usage: " + lsep + "java [-Dhost=] [-Dport=] [-Dusername=] [-Dpassword=] [-Dformauthtarget=] TestJSPFiles <fName> " + lsep + "Defaults:" + lsep + "  -Dhost=localhost" + lsep + "  -Dport=80" + lsep + "  -Dusername=" + lsep + "  -Dpassword=" + lsep + "  -Dformauthtarget=/j_security_check" + lsep + "  File should be of the form below:" + lsep + "#---------------------------------------------" + lsep + "/console/index.jsp" + lsep + "/console/standards/home.jsp" + lsep + "#---------------------------------------------" + lsep + "";
    }

    public String toString() {
        String s = host + ":" + port + " [user/pass=" + username + "/" + password + "]";
        return s;
    }

    public void run() throws Exception {
        cookieString = getCookieString();
        if (DEBUG) System.err.println(cookieString);
        List badFiles = new ArrayList();
        List goodFiles = new ArrayList();
        int numGood = 0;
        int numBad = 0;
        int sz = jspFiles.size();
        for (int i = 0; i < sz; i++) {
            String file = null;
            try {
                file = (String) jspFiles.get(i);
                int respCode = getResponseCode(file);
                if (respCode == 200) {
                    System.out.println("Good: " + file + " --- Got RespCode " + respCode);
                    goodFiles.add(file);
                    numGood++;
                } else {
                    System.out.println("Error: " + file + " --- Got RespCode " + respCode);
                    badFiles.add(file);
                    numBad++;
                }
            } catch (Exception exc) {
                System.out.println("Error: " + file + " --- Got Exception " + exc);
                badFiles.add(file);
                numBad++;
            }
            if (DEBUG) {
                System.err.println("=============================================");
            }
        }
        System.out.println("Good files: " + numGood);
        System.out.println("Bad files: " + numBad);
        System.out.println("---------------------------------------");
        for (int i = 0; i < numBad; i++) {
            System.out.println(badFiles.get(i));
        }
    }

    public int getResponseCode(String file) throws Exception {
        int respCode = -1;
        Socket s = new Socket(host, port);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
        String target = file + "?jsp_precompile=true";
        out.print("GET " + target + " HTTP/1.0" + "\r\n");
        if (cookieString != null) out.print("Cookie: " + cookieString + "\r\n");
        out.print("Connection: close\r\n");
        out.print("Host: " + host + "\r\n");
        out.print("\r\n");
        out.flush();
        BufferedReader buffreader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String line = buffreader.readLine();
        if (line != null) {
            StringTokenizer st = new StringTokenizer(line.trim());
            st.nextToken();
            respCode = Integer.parseInt(st.nextToken());
        }
        while ((line = buffreader.readLine()) != null) {
            if (line.trim().length() == 0) break;
        }
        StringBuffer buf = new StringBuffer();
        while ((line = buffreader.readLine()) != null) {
            buf.append(line).append("\n");
        }
        if (DEBUG) {
            System.err.println(target);
            System.err.println(respCode);
            System.err.println(buf.toString());
        }
        buffreader.close();
        out.close();
        s.close();
        return respCode;
    }

    public String getCookieString() throws Exception {
        if (username == null || password == null) return null;
        String cookieLine = null;
        String c = null;
        Socket s = new Socket(host, port);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
        String target = formAuthTarget;
        out.print("POST " + target + " HTTP/1.0" + "\r\n");
        out.print("Connection: close\r\n");
        out.print("Host: " + host + "\r\n");
        out.print("Content-type: application/x-www-form-urlencoded\r\n");
        String postString = "j_username=" + username + "&j_password=" + password;
        out.print("Content-length: " + postString.length() + "\r\n");
        out.print("\r\n");
        out.print(postString + "\r\n");
        out.print("\r\n");
        out.flush();
        BufferedReader buffreader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String line = null;
        while ((line = buffreader.readLine()) != null) {
            if (DEBUG) System.err.println(line);
            if (line.trim().length() == 0) break;
            if (line.toLowerCase().startsWith("set-cookie:")) {
                cookieLine = line.substring("Set-Cookie:".length()).trim();
                int semicolon = cookieLine.indexOf(";");
                if (semicolon != -1) {
                    cookieLine = cookieLine.substring(0, semicolon).trim();
                }
                break;
            }
        }
        buffreader.close();
        out.close();
        s.close();
        return cookieLine;
    }

    public int getResponseCode0(String file) throws Exception {
        HttpURLConnection urlconn = null;
        try {
            String target = file + "?jsp_precompile=true";
            URL url = new URL("http", host, port, target);
            urlconn = (HttpURLConnection) url.openConnection();
            urlconn.connect();
            int respCode = urlconn.getResponseCode();
            return respCode;
        } finally {
            try {
                if (urlconn != null) urlconn.disconnect();
            } catch (Exception exc2) {
            }
        }
    }

    /** <xmp>Usage: java weblogic.qa.taskmgr.TestJSPFiles <fName> <host> <port> </xmp> */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println(HELP_MESSAGE);
            System.exit(0);
        }
        String fName = "jsp-pages.txt";
        String tHost = "localhost";
        int tPort = 80;
        String tUser = null;
        String tPass = null;
        String tFormAuthTarget = "/j_security_check";
        boolean tDebug = false;
        String tmpstr = null;
        if ((tmpstr = System.getProperty("host")) != null) tHost = tmpstr;
        if ((tmpstr = System.getProperty("port")) != null) tPort = Integer.parseInt(tmpstr);
        if ((tmpstr = System.getProperty("username")) != null) tUser = tmpstr;
        if ((tmpstr = System.getProperty("password")) != null) tPass = tmpstr;
        if ("true".equals(System.getProperty("debug"))) tDebug = true;
        if ((tmpstr = System.getProperty("formauthtarget")) != null) tFormAuthTarget = tmpstr;
        if (args.length > 0) fName = args[0];
        if (args.length > 1) tHost = args[1];
        if (args.length > 2) tPort = Integer.parseInt(args[2]);
        if (args.length > 3) tUser = args[3];
        if (args.length > 4) tPass = args[4];
        FileReader fr = new FileReader(fName);
        BufferedReader br = new BufferedReader(fr);
        List files = new ArrayList();
        String line = null;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            files.add(line);
        }
        fr.close();
        br.close();
        TestJSPFiles test = new TestJSPFiles();
        test.host = tHost;
        test.port = tPort;
        test.jspFiles = files;
        test.username = tUser;
        test.password = tPass;
        test.formAuthTarget = tFormAuthTarget;
        test.DEBUG = tDebug;
        System.out.println("file: " + fName);
        System.out.println("TestJSPFiles: " + test);
        test.run();
    }
}
