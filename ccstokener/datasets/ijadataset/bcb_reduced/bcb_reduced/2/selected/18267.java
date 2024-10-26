package newgen.presentation.component;

import newgen.presentation.StaticValues;

/**
 *
 * @author  Administrator
 */
public class ServletConnector {

    /** Creates a new instance of ServletConnector */
    private ServletConnector() {
    }

    public static ServletConnector getInstance() {
        if (sc == null) sc = new ServletConnector();
        return sc;
    }

    public String sendRequest(java.lang.String servletName, java.lang.String request) {
        String reqxml = "";
        org.jdom.Document retdoc = null;
        String myurl = java.util.prefs.Preferences.systemRoot().get("serverurl", "");
        String myport = "";
        myport = java.util.prefs.Preferences.systemRoot().get("portno", "8080");
        if (myport == null || myport.trim().equals("")) {
            myport = "80";
        }
        if (this.serverURL == null) {
            try {
                java.net.URL codebase = newgen.presentation.NewGenMain.getAppletInstance().getCodeBase();
                if (codebase != null) serverURL = codebase.getHost(); else serverURL = "localhost";
            } catch (Exception exp) {
                exp.printStackTrace();
                serverURL = "localhost";
            }
            newgen.presentation.component.IPAddressPortNoDialog ipdig = new newgen.presentation.component.IPAddressPortNoDialog(myurl, myport);
            ipdig.show();
            serverURL = myurl = ipdig.getIPAddress();
            myport = ipdig.getPortNo();
            java.util.prefs.Preferences.systemRoot().put("serverurl", serverURL);
            java.util.prefs.Preferences.systemRoot().put("portno", myport);
            System.out.println(serverURL);
        }
        try {
            System.out.println("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URL url = new java.net.URL("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URLConnection urlconn = (java.net.URLConnection) url.openConnection();
            urlconn.setDoOutput(true);
            urlconn.setRequestProperty("Content-type", "text/xml; charset=UTF-8");
            java.io.OutputStream os = urlconn.getOutputStream();
            String req1xml = request;
            java.util.zip.CheckedOutputStream cos = new java.util.zip.CheckedOutputStream(os, new java.util.zip.Adler32());
            java.util.zip.GZIPOutputStream gop = new java.util.zip.GZIPOutputStream(cos);
            java.io.OutputStreamWriter dos = new java.io.OutputStreamWriter(gop, "UTF-8");
            System.out.println("#########***********$$$$$$$$##########" + req1xml);
            dos.write(req1xml);
            dos.flush();
            dos.close();
            System.out.println("url conn: " + urlconn.getContentEncoding() + "  " + urlconn.getContentType());
            java.io.InputStream ios = urlconn.getInputStream();
            java.util.zip.CheckedInputStream cis = new java.util.zip.CheckedInputStream(ios, new java.util.zip.Adler32());
            java.util.zip.GZIPInputStream gip = new java.util.zip.GZIPInputStream(cis);
            java.io.InputStreamReader br = new java.io.InputStreamReader(gip, "UTF-8");
            retdoc = (new org.jdom.input.SAXBuilder()).build(br);
        } catch (java.net.ConnectException conexp) {
            javax.swing.JOptionPane.showMessageDialog(null, newgen.presentation.NewGenMain.getAppletInstance().getMyResource().getString("ConnectExceptionMessage"), "Critical error", javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (Exception exp) {
            exp.printStackTrace(System.out);
            TroubleShootConnectivity troubleShoot = new TroubleShootConnectivity();
        }
        System.out.println(reqxml);
        return (new org.jdom.output.XMLOutputter()).outputString(retdoc);
    }

    public Object sendObjectRequest(java.lang.String servletName, java.lang.Object request) {
        Object reqxml = null;
        org.jdom.Document retdoc = null;
        String myurl = java.util.prefs.Preferences.systemRoot().get("serverurl", "");
        String myport = java.util.prefs.Preferences.systemRoot().get("portno", "8080");
        if (myport == null || myport.trim().equals("")) {
            myport = "80";
        }
        if (this.serverURL == null) {
            try {
                java.net.URL codebase = newgen.presentation.NewGenMain.getAppletInstance().getCodeBase();
                if (codebase != null) serverURL = codebase.getHost(); else serverURL = "localhost";
            } catch (Exception exp) {
                exp.printStackTrace();
                serverURL = "localhost";
            }
            newgen.presentation.component.IPAddressPortNoDialog ipdig = new newgen.presentation.component.IPAddressPortNoDialog(myurl, myport);
            ipdig.show();
            serverURL = myurl = ipdig.getIPAddress();
            myport = ipdig.getPortNo();
            java.util.prefs.Preferences.systemRoot().put("serverurl", serverURL);
            java.util.prefs.Preferences.systemRoot().put("portno", myport);
            System.out.println(serverURL);
        }
        try {
            java.net.URL url = new java.net.URL("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URLConnection urlconn = (java.net.URLConnection) url.openConnection();
            urlconn.setDoOutput(true);
            java.io.OutputStream os = urlconn.getOutputStream();
            java.util.zip.CheckedOutputStream cos = new java.util.zip.CheckedOutputStream(os, new java.util.zip.Adler32());
            java.util.zip.GZIPOutputStream gop = new java.util.zip.GZIPOutputStream(cos);
            java.io.ObjectOutputStream dos = new java.io.ObjectOutputStream(gop);
            dos.writeObject(request);
            dos.flush();
            dos.close();
            java.io.InputStream ios = urlconn.getInputStream();
            java.util.zip.CheckedInputStream cis = new java.util.zip.CheckedInputStream(ios, new java.util.zip.Adler32());
            java.util.zip.GZIPInputStream gip = new java.util.zip.GZIPInputStream(cis);
            java.io.ObjectInputStream br = new java.io.ObjectInputStream(gip);
            reqxml = br.readObject();
        } catch (Exception exp) {
            exp.printStackTrace(System.out);
            System.out.println("Exception in Servlet Connector: " + exp);
        }
        return reqxml;
    }

    public Object sendObjectRequestToSpecifiedServer(java.lang.String serverName, java.lang.String servletName, java.lang.Object request) {
        Object reqxml = null;
        org.jdom.Document retdoc = null;
        String myurl = java.util.prefs.Preferences.systemRoot().get("serverurl", "");
        String myport = java.util.prefs.Preferences.systemRoot().get("portno", "8080");
        if (myport == null || myport.trim().equals("")) {
            myport = "80";
        }
        if (this.serverURL == null) {
            try {
                java.net.URL codebase = newgen.presentation.NewGenMain.getAppletInstance().getCodeBase();
                if (codebase != null) serverURL = codebase.getHost(); else serverURL = "localhost";
            } catch (Exception exp) {
                exp.printStackTrace();
                serverURL = "localhost";
            }
            newgen.presentation.component.IPAddressPortNoDialog ipdig = new newgen.presentation.component.IPAddressPortNoDialog(myurl, myport);
            ipdig.show();
            serverURL = myurl = ipdig.getIPAddress();
            myport = ipdig.getPortNo();
            java.util.prefs.Preferences.systemRoot().put("serverurl", serverURL);
            java.util.prefs.Preferences.systemRoot().put("portno", myport);
            System.out.println(serverURL);
        }
        try {
            java.net.URL url = new java.net.URL("http://" + serverName + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URLConnection urlconn = (java.net.URLConnection) url.openConnection();
            urlconn.setDoOutput(true);
            java.io.OutputStream os = urlconn.getOutputStream();
            java.util.zip.CheckedOutputStream cos = new java.util.zip.CheckedOutputStream(os, new java.util.zip.Adler32());
            java.util.zip.GZIPOutputStream gop = new java.util.zip.GZIPOutputStream(cos);
            java.io.ObjectOutputStream dos = new java.io.ObjectOutputStream(gop);
            dos.writeObject(request);
            dos.flush();
            dos.close();
            java.io.InputStream ios = urlconn.getInputStream();
            java.util.zip.CheckedInputStream cis = new java.util.zip.CheckedInputStream(ios, new java.util.zip.Adler32());
            java.util.zip.GZIPInputStream gip = new java.util.zip.GZIPInputStream(cis);
            java.io.ObjectInputStream br = new java.io.ObjectInputStream(gip);
            reqxml = br.readObject();
        } catch (Exception exp) {
            exp.printStackTrace(System.out);
            System.out.println("Exception in Servlet Connector: " + exp);
        }
        return reqxml;
    }

    public Object sendRequestObjectResponse(java.lang.String servletName, java.lang.String request) {
        String osRoot = OSRoot.getRoot();
        String fname = "";
        Object retobj = null;
        String myurl = java.util.prefs.Preferences.systemRoot().get("serverurl", "");
        String myport = java.util.prefs.Preferences.systemRoot().get("portno", "8080");
        if (myport == null || myport.trim().equals("")) {
            myport = "80";
        }
        if (this.serverURL == null) {
            try {
                java.net.URL codebase = newgen.presentation.NewGenMain.getAppletInstance().getCodeBase();
                if (codebase != null) serverURL = codebase.getHost(); else serverURL = "localhost";
            } catch (Exception exp) {
                exp.printStackTrace();
                serverURL = "localhost";
            }
            newgen.presentation.component.IPAddressPortNoDialog ipdig = new newgen.presentation.component.IPAddressPortNoDialog(myurl, myport);
            ipdig.show();
            serverURL = myurl = ipdig.getIPAddress();
            myport = ipdig.getPortNo();
            java.util.prefs.Preferences.systemRoot().put("serverurl", serverURL);
            java.util.prefs.Preferences.systemRoot().put("portno", myport);
            System.out.println(serverURL);
        }
        try {
            System.out.println("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URL url = new java.net.URL("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URLConnection urlconn = (java.net.URLConnection) url.openConnection();
            urlconn.setDoOutput(true);
            urlconn.setRequestProperty("Content-type", "text/xml; charset=UTF-8");
            java.io.OutputStream os = urlconn.getOutputStream();
            String req1xml = request;
            java.util.zip.CheckedOutputStream cos = new java.util.zip.CheckedOutputStream(os, new java.util.zip.Adler32());
            java.util.zip.GZIPOutputStream gop = new java.util.zip.GZIPOutputStream(cos);
            java.io.OutputStreamWriter dos = new java.io.OutputStreamWriter(gop, "UTF-8");
            System.out.println(req1xml);
            try {
                java.io.FileOutputStream pw = new java.io.FileOutputStream("log.txt");
                pw.write(req1xml.getBytes());
                pw.flush();
                pw.close();
            } catch (Exception exp) {
                exp.printStackTrace();
            }
            dos.write(req1xml);
            dos.flush();
            dos.close();
            System.out.println("url conn: " + urlconn.getContentEncoding() + "  " + urlconn.getContentType());
            java.io.InputStream ios = urlconn.getInputStream();
            java.io.File f1 = new java.io.File(osRoot + "/localattachments/Reports");
            if (!f1.exists()) f1.mkdirs();
            java.io.File file = null;
            if (urlconn.getContentType() != null && urlconn.getContentType().trim().equals("application/vnd.oasis.opendocument.text")) {
                file = new java.io.File(osRoot + "/localattachments/Reports/" + System.currentTimeMillis() + ".odt");
            } else if (urlconn.getContentType() != null && urlconn.getContentType().trim().equals("text/html")) {
                file = new java.io.File(osRoot + "/localattachments/Reports/" + System.currentTimeMillis() + ".html");
            } else {
                file = new java.io.File(osRoot + "/localattachments/Reports/" + System.currentTimeMillis() + ".xls");
            }
            file = new java.io.File(file.getAbsolutePath());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            int c;
            while ((c = ios.read()) != -1) fos.write(c);
            fos.close();
            ios.close();
            fname = file.getAbsolutePath();
            System.out.println(fname);
            newgen.presentation.component.Utility.getInstance().showBrowser("file://" + fname);
        } catch (Exception exp) {
            exp.printStackTrace(System.out);
            javax.swing.JOptionPane.showMessageDialog(null, "<html>Could not establish connection with the server, <br>Please verify server name/IP adress, <br>Also check if NewGenLib server is running</html>", "Critical error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        return fname;
    }

    public String sendRequestHTTPTunelling(java.lang.String servletName, java.lang.String request) {
        String reqxml = "";
        org.jdom.Document retdoc = null;
        String myurl = java.util.prefs.Preferences.systemRoot().get("serverurl", "");
        String myport = java.util.prefs.Preferences.systemRoot().get("portno", "8080");
        if (myport == null || myport.trim().equals("")) {
            myport = "80";
        }
        if (this.serverURL == null) {
            try {
                java.net.URL codebase = newgen.presentation.NewGenMain.getAppletInstance().getCodeBase();
                if (codebase != null) serverURL = codebase.getHost(); else serverURL = "localhost";
            } catch (Exception exp) {
                exp.printStackTrace();
                serverURL = "localhost";
            }
            newgen.presentation.component.IPAddressPortNoDialog ipdig = new newgen.presentation.component.IPAddressPortNoDialog(myurl, myport);
            ipdig.show();
            serverURL = myurl = ipdig.getIPAddress();
            myport = ipdig.getPortNo();
            java.util.prefs.Preferences.systemRoot().put("serverurl", serverURL);
            java.util.prefs.Preferences.systemRoot().put("portno", myport);
            System.out.println(serverURL);
        }
        try {
            System.out.println("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URL url = new java.net.URL("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URLConnection urlconn = (java.net.URLConnection) url.openConnection();
            urlconn.setDoOutput(true);
            urlconn.setRequestProperty("Content-type", "text/xml; charset=UTF-8");
            java.io.OutputStream os = urlconn.getOutputStream();
            String req1xml = request;
            java.util.zip.CheckedOutputStream cos = new java.util.zip.CheckedOutputStream(os, new java.util.zip.Adler32());
            java.util.zip.GZIPOutputStream gop = new java.util.zip.GZIPOutputStream(cos);
            java.io.OutputStreamWriter dos = new java.io.OutputStreamWriter(gop, "UTF-8");
            System.out.println(req1xml);
            try {
                java.io.FileOutputStream pw = new java.io.FileOutputStream("log.txt");
                pw.write(req1xml.getBytes());
                pw.flush();
                pw.close();
            } catch (Exception exp) {
                exp.printStackTrace();
            }
            dos.write(req1xml);
            dos.flush();
            dos.close();
            System.out.println("url conn: " + urlconn.getContentEncoding() + "  " + urlconn.getContentType());
            java.io.InputStream ios = urlconn.getInputStream();
            java.util.zip.CheckedInputStream cis = new java.util.zip.CheckedInputStream(ios, new java.util.zip.Adler32());
            java.util.zip.GZIPInputStream gip = new java.util.zip.GZIPInputStream(cis);
            java.io.InputStreamReader br = new java.io.InputStreamReader(gip, "UTF-8");
            retdoc = (new org.jdom.input.SAXBuilder()).build(br);
            try {
                java.io.FileOutputStream pw = new java.io.FileOutputStream("log3.txt");
                pw.write(reqxml.getBytes());
                pw.flush();
                pw.close();
            } catch (Exception exp) {
                exp.printStackTrace();
            }
        } catch (Exception exp) {
            exp.printStackTrace(System.out);
            javax.swing.JOptionPane.showMessageDialog(null, "<html>Could not establish connection with the server, <br>Please verify server name/IP adress, <br>Also check if NewGenLib server is running</html>", "Critical error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        System.out.println(reqxml);
        return (new org.jdom.output.XMLOutputter()).outputString(retdoc);
    }

    public String sendRequestAndGetNormalStringOutPut(java.lang.String servletName, java.lang.String request) {
        String myurl = java.util.prefs.Preferences.systemRoot().get("serverurl", "");
        String myport = java.util.prefs.Preferences.systemRoot().get("portno", "8080");
        if (myport == null || myport.trim().equals("")) {
            myport = "80";
        }
        if (this.serverURL == null) {
            try {
                java.net.URL codebase = newgen.presentation.NewGenMain.getAppletInstance().getCodeBase();
                if (codebase != null) serverURL = codebase.getHost(); else serverURL = "localhost";
            } catch (Exception exp) {
                exp.printStackTrace();
                serverURL = "localhost";
            }
            newgen.presentation.component.IPAddressPortNoDialog ipdig = new newgen.presentation.component.IPAddressPortNoDialog(myurl, myport);
            ipdig.show();
            serverURL = myurl = ipdig.getIPAddress();
            myport = ipdig.getPortNo();
            java.util.prefs.Preferences.systemRoot().put("serverurl", serverURL);
            java.util.prefs.Preferences.systemRoot().put("portno", myport);
            System.out.println(serverURL);
        }
        String response = "";
        try {
            System.out.println("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URL url = new java.net.URL("http://" + serverURL + ":" + myport + "/newgenlibctxt/" + servletName);
            java.net.URLConnection urlconn = (java.net.URLConnection) url.openConnection();
            urlconn.setDoOutput(true);
            urlconn.setRequestProperty("Content-type", "text/xml; charset=UTF-8");
            java.io.OutputStream os = urlconn.getOutputStream();
            String req1xml = request;
            java.util.zip.CheckedOutputStream cos = new java.util.zip.CheckedOutputStream(os, new java.util.zip.Adler32());
            java.util.zip.GZIPOutputStream gop = new java.util.zip.GZIPOutputStream(cos);
            java.io.OutputStreamWriter dos = new java.io.OutputStreamWriter(gop, "UTF-8");
            System.out.println(req1xml);
            dos.write(req1xml);
            dos.flush();
            dos.close();
            System.out.println("url conn: " + urlconn.getContentEncoding() + "  " + urlconn.getContentType());
            java.io.InputStream ios = urlconn.getInputStream();
            java.util.zip.CheckedInputStream cis = new java.util.zip.CheckedInputStream(ios, new java.util.zip.Adler32());
            java.util.zip.GZIPInputStream gip = new java.util.zip.GZIPInputStream(cis);
            java.io.InputStreamReader br = new java.io.InputStreamReader(gip, "UTF-8");
            int n = -1;
            while ((n = br.read()) != -1) response += (char) n;
        } catch (java.net.ConnectException conexp) {
            javax.swing.JOptionPane.showMessageDialog(null, "<html>Could not establish connection with the NewGenLib server, " + "<br>These might be the possible reasons." + "<br><li>Check the network connectivity between this machine and the server." + "<br><li>Check whether NewGenLib server is running on the server machine." + "<br><li>NewGenLib server might not have initialized properly. In this case" + "<br>go to server machine, open NewGenLibDesktop Application," + "<br> utility ->Send log to NewGenLib Customer Support</html>", "Critical error", javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (Exception exp) {
            exp.printStackTrace(System.out);
            TroubleShootConnectivity troubleShoot = new TroubleShootConnectivity();
        }
        return response;
    }

    private static ServletConnector sc = null;

    private static String serverURL = null;
}
