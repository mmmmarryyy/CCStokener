package atv;

import java.io.*;
import java.net.*;

public class ServerConnexion {

    private URL url;

    private URLConnection c;

    public ServerConnexion(String adr) throws Exception {
        url = new URL(adr);
        c = url.openConnection();
    }

    public InputStream getInputStream() throws Exception {
        return (c.getInputStream());
    }

    public String getOutCGI() throws Exception {
        String str = new String();
        StringBuffer sb = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
        while ((str = in.readLine()) != null) sb.append(str).append('\n');
        in.close();
        str = sb.toString();
        return (str);
    }

    public void sendFileInCGI(String fileName) throws Exception {
        c.setDoOutput(true);
        String str = new String();
        PrintWriter out = new PrintWriter(c.getOutputStream());
        BufferedReader f = new BufferedReader(new FileReader(fileName));
        while ((str = f.readLine()) != null) {
            out.println(str);
        }
        out.close();
    }

    public void sendStringInCGI(String str) throws Exception {
        c.setDoOutput(true);
        PrintWriter out = new PrintWriter(c.getOutputStream());
        out.println(str);
        out.close();
    }
}
