package com.netx.ut.lib.java;

import java.net.*;
import java.security.*;
import javax.net.ssl.*;
import java.util.*;
import java.io.*;
import com.netx.basic.R1.io.ExtendedInputStream;
import com.netx.basic.R1.io.ExtendedOutputStream;
import com.netx.basic.R1.io.ExtendedReader;
import com.netx.basic.R1.io.Streams;
import com.netx.generics.R1.util.UnitTester;

public class NTNet extends UnitTester {

    public static void main(String[] args) throws Throwable {
        NTNet nt = new NTNet();
        nt.getHttpURL();
        System.out.println("done.");
    }

    public void getLocalhostInfo() throws Exception {
        InetAddress localhost = InetAddress.getLocalHost();
        println("getCanonicalHostName: " + localhost.getCanonicalHostName());
        println("getHostAddress: " + localhost.getHostAddress());
        println("getHostName: " + localhost.getHostName());
    }

    public void testURLAndURI() throws Throwable {
        final String path = "http://localhost:80/servlet-tests/test?method=testPaths#123";
        URI uri = new URI(path);
        URL url = new URL(path);
        println("URI: " + uri);
        println("URL: " + url);
        println("File.getName(): " + new File("C:/Windows/..").getName());
        url = new URL("http://www.google.com/../abc");
        println("File.getCanocicalPath(): " + new File(url.getFile()).getCanonicalPath());
        url = new URL("file://C:\\Windows");
        showObjectProperty(url, "getProtocol");
        showObjectProperty(url, "getPath");
        showObjectProperty(url, "getFile");
        showObjectProperty(url, "getHost");
        showObjectProperty(url, "getQuery");
        showObjectProperty(url, "getRef");
        showObjectProperty(url, "getUserInfo");
    }

    public void getHttpURL() throws Exception {
        boolean display = true;
        boolean allHeaders = false;
        String url = null;
        url = "http://localhost/cubigraf2";
        url = "http://www.accenture.com/NR/rdonlyres/971C4EEE-24E2-4BAA-8C7B-D5A5133D5968/0/en_sprout.jpg";
        url = "http://www.uni.pt/img/home-direito.gif";
        url = "http://www.google.com";
        URLConnection uc = new URL(url).openConnection();
        println("HEADERS:");
        if (allHeaders) {
            Iterator<Map.Entry<String, List<String>>> itHeaders = uc.getHeaderFields().entrySet().iterator();
            while (itHeaders.hasNext()) {
                Map.Entry<String, List<String>> e = itHeaders.next();
                Iterator<?> itValues = e.getValue().iterator();
                while (itValues.hasNext()) {
                    println(e.getKey() + ": " + itValues.next());
                }
            }
        } else {
            showObjectProperty(uc, "getContentEncoding");
            showObjectProperty(uc, "getContentLength");
            showObjectProperty(uc, "getContentType");
            showObjectProperty(uc, "getDate", FORMAT.TIMESTAMP);
            showObjectProperty(uc, "getExpiration", FORMAT.TIMESTAMP);
            showObjectProperty(uc, "getLastModified", FORMAT.TIMESTAMP);
        }
        ExtendedInputStream in = new ExtendedInputStream(uc.getInputStream(), url.toString());
        if (display) {
            println("BODY:");
            ExtendedReader reader = new ExtendedReader(in);
            for (String s = reader.readLine(); s != null; s = reader.readLine()) {
                println(s);
            }
        } else {
            println("(BODY saved to a file)");
            String contentType = uc.getContentType();
            StringBuilder filename = new StringBuilder("C:\\Documents and Settings\\Carlos_da_S_Pereira\\Desktop\\JAVA_NET_TESTS");
            filename.append(".");
            filename.append(contentType.substring(contentType.indexOf("/") + 1));
            File file = new File(filename.toString());
            ExtendedOutputStream out = new ExtendedOutputStream(new FileOutputStream(file), file.getAbsolutePath());
            Streams.copy(in, out);
            out.close();
        }
        in.close();
    }

    public void getAuthenticatedHttpURL() throws Exception {
        String url = null;
        url = "https://eme.mail.accenture.com";
        String username = "carlos.pereira";
        String password = "mypassword";
        InetSocketAddress proxyAddress = new InetSocketAddress(InetAddress.getByName("excelerator.inet"), 3128);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
        URLConnection uc = new URL(url).openConnection(proxy);
        uc.setUseCaches(false);
        if (username != null) {
            Authenticator.setDefault(new BasicAuthenticator(username, password));
        }
        uc.setRequestProperty("pragma", "no-cache");
        uc.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; (R1 1.5); .NET CLR 1.0.3705; .NET CLR 1.1.4322)");
        println("HEADERS:");
        showObjectProperty(uc, "getContent");
        showObjectProperty(uc, "getContentEncoding");
        showObjectProperty(uc, "getContentLength");
        showObjectProperty(uc, "getContentType");
        showObjectProperty(uc, "getDate", FORMAT.TIMESTAMP);
        showObjectProperty(uc, "getExpiration", FORMAT.TIMESTAMP);
        showObjectProperty(uc, "getLastModified", FORMAT.TIMESTAMP);
    }

    public void getHttpsURL() throws Exception {
        String url = null;
        url = "https://eme.mail.accenture.com/exchange/carlos.pereira";
        String username = "carlos.pereira";
        String password = "mypassword";
        Authenticator.setDefault(new BasicAuthenticator(username, password));
        InetSocketAddress proxyAddress = new InetSocketAddress(InetAddress.getByName("excelerator.inet"), 3128);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
        HttpURLConnection uc = (HttpURLConnection) new URL(url).openConnection(proxy);
        uc.setUseCaches(false);
        uc.setDoInput(true);
        uc.setDoOutput(true);
        uc.setRequestMethod("GET");
        uc.setRequestProperty("pragma", "no-cache");
        uc.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; (R1 1.5); .NET CLR 1.0.3705; .NET CLR 1.1.4322)");
        println("HEADERS:");
        Iterator<Map.Entry<String, List<String>>> itProps = uc.getRequestProperties().entrySet().iterator();
        while (itProps.hasNext()) {
            Map.Entry<String, List<String>> e = itProps.next();
            Iterator<?> itValues = e.getValue().iterator();
            while (itValues.hasNext()) {
                println(e.getKey() + ": " + itValues.next());
            }
        }
        uc.connect();
        println(uc.getHeaderField("WWW-Authenticate"));
        println(uc.getResponseMessage());
    }

    public void getURLBySocket() throws Exception {
        String host = "www.verisign.com";
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) factory.createSocket(host, 443);
        Writer out = new OutputStreamWriter(socket.getOutputStream());
        out.write("GET http://" + host + "/ HTTP/1.1\r\n");
        out.write("\r\n");
        out.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        int c;
        while ((c = in.read()) != -1) {
            System.out.write(c);
        }
        out.close();
        in.close();
        socket.close();
    }

    private class BasicAuthenticator extends Authenticator {

        private final PasswordAuthentication _pa;

        private final boolean _printProperties = false;

        private int counter = 0;

        public BasicAuthenticator(String username, String password) {
            _pa = new PasswordAuthentication(username, password.toCharArray());
        }

        public PasswordAuthentication getPasswordAuthentication() {
            counter++;
            if (counter > 3) {
                return null;
            } else {
                println("password requested.");
                if (_printProperties) {
                    println("getRequestingHost: " + getRequestingHost());
                    println("getRequestingPort: " + getRequestingPort());
                    println("getRequestingPrompt: " + getRequestingPrompt());
                    println("getRequestingProtocol: " + getRequestingProtocol());
                    println("getRequestingScheme: " + getRequestingScheme());
                    println("getRequestingSite: " + getRequestingSite());
                    println("getRequestingURL: " + getRequestingURL());
                    println("getRequestorType: " + getRequestorType());
                }
                return _pa;
            }
        }
    }
}
