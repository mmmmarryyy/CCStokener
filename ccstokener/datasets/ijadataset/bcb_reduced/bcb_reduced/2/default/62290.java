import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;
import java.util.regex.*;

/**
	 * Download manager class
	 * @author mjsabby
	 */
class gRapidDownloadManager extends Thread {

    /**
		 * Lock object
		 */
    static Object lock;

    /**
		 * Premium cookie
		 */
    static String cookie;

    /**
		 * File list
		 */
    Stack<String> fileList;

    /**
		 * gRapidDownloadManager thread
		 */
    static Thread parentThread;

    /**
		 * Counter for wget log files
		 */
    static int counter;

    /**
		 * Maximum allowed simultaneous downloads
		 */
    static int maxSimultaneousDownloads;

    /**
		 * Current simultaneous downloads running
		 */
    static int curSimultaneousDownloads;

    /**
		 * Is the parent thread sleeping?
		 */
    static boolean parentThreadSleeping;

    /**
		 * Constructor
		 * @param f List of files to download
		 * @param c Cookie
		 * @param m Maximum parallel downloads to queue
		 */
    gRapidDownloadManager(Stack<String> f, String c, int m) {
        lock = new Object();
        cookie = c;
        fileList = f;
        maxSimultaneousDownloads = m;
        curSimultaneousDownloads = 0;
    }

    /**
		 * The download manager
		 */
    public void run() {
        parentThread = Thread.currentThread();
        while (true) {
            synchronized (lock) {
                while (fileList.size() > 0 && curSimultaneousDownloads < maxSimultaneousDownloads) {
                    curSimultaneousDownloads++;
                    (new gRapidDownload(fileList.pop(), cookie)).start();
                }
            }
            if (fileList.size() < 1) return;
            try {
                synchronized (lock) {
                    parentThreadSleeping = true;
                }
                Thread.sleep(999999999);
            } catch (InterruptedException e) {
                synchronized (lock) {
                    parentThreadSleeping = false;
                }
            }
        }
    }

    /**
		 * Individual download thread
		 * @author mjsabby
		 */
    private class gRapidDownload extends Thread {

        /**
			 * Url to download
			 */
        private String url;

        /**
			 * Premium cookie
			 */
        private String cookie;

        /**
			 * Constructor
			 * @param u Url
			 * @param c Cookie
			 */
        gRapidDownload(String u, String c) {
            url = u;
            cookie = c;
        }

        /**
			 * The actual wget download thread
			 */
        public void run() {
            try {
                Runtime rt = Runtime.getRuntime();
                Process proc = rt.exec("wget -c --directory-prefix=Downloads --no-cookies --header \"Cookie: " + cookie + "\" --append-output=wget-log.txt " + url);
                InputStream in = proc.getInputStream();
                proc.waitFor();
                in.close();
            } catch (Exception e) {
                System.out.print(e.getLocalizedMessage());
            } finally {
                synchronized (lock) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                    curSimultaneousDownloads--;
                    if (parentThreadSleeping) parentThread.interrupt();
                }
            }
        }
    }
}

/**
	 * gRapid class
	 * @author mjsabby
	 */
public class gRapid {

    /**
		 * Fetch the actual url and write to outfile
		 * @param urltxt URL
		 * @param cookie Cookie
		 * @return
		 */
    private static String fetch(String urltxt, String cookie) {
        try {
            URL url = new URL(urltxt);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream source = url.openStream();
            String data = new Scanner(source).useDelimiter("\\A").next();
            Pattern p = Pattern.compile("form action=\"(.*)\" method=\"post\"");
            Matcher m = p.matcher(data);
            if (!m.find()) return "";
            urltxt = m.group(1);
            url = new URL(urltxt);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Cookie", cookie);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write("dl.start=PREMIUM");
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            while ((data = in.readLine()) != null) sb.append(data + System.getProperty("line.separator"));
            data = urltxt.substring(urltxt.lastIndexOf("/") + 1);
            p = Pattern.compile("<tr><td><a href=\"(.*?)" + data);
            m = p.matcher(sb.toString());
            data = (m.find()) ? (m.group(1) + data + System.getProperty("line.separator")) : "";
            return data;
        } catch (Exception e) {
            return "";
        }
    }

    /**
		 * Returns cookie contents once logged into RapidShare
		 * @param username
		 * @param password
		 * @return cookie contents
		 */
    private static String saveCookie(String username, String password) {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            URL url = new URL("https://ssl.rapidshare.com/cgi-bin/premiumzone.cgi");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write("login=" + username + "&password=" + password);
            out.flush();
            String cookie = conn.getHeaderField("Set-Cookie");
            cookie = cookie.substring(0, cookie.indexOf(";"));
            out.close();
            conn.disconnect();
            return cookie;
        } catch (Exception e) {
            return "";
        }
    }

    /**
		 * Main method
		 * @param argv
		 * @throws Exception
		 */
    public static void main(String argv[]) throws Exception {
        if (argv.length != 4) {
            System.out.println("Usage: java -jar gRapid.jar username password input_file simultaneous_downloads");
            System.exit(0);
        }
        String cookie = saveCookie(argv[0], argv[1]);
        if (cookie.length() < 6) {
            System.out.println("Authentication failure");
            System.exit(0);
        }
        Scanner in = new Scanner(new File(argv[2]));
        BufferedWriter out = new BufferedWriter(new FileWriter("out.txt"));
        Stack<String> files = new Stack<String>();
        Stack<String> nList = new Stack<String>();
        while (in.hasNextLine()) {
            files.push(in.nextLine().trim());
            nList.push(fetch(files.peek(), cookie));
            out.write(nList.peek());
            out.flush();
        }
        in.close();
        out.close();
        new gRapidDownloadManager(nList, cookie, Integer.parseInt(argv[3])).start();
    }
}
