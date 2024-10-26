package info.olteanu.utils.retrieve;

import info.olteanu.utils.retrieve.*;
import java.io.*;
import java.net.*;
import java.util.zip.*;

public class RetrievePage {

    public int timeOut = -1;

    public static RetrievePage makeWithCookie(String cookie, boolean autoRef) {
        String req[][] = new String[1][2];
        req[0][0] = "Cookie";
        req[0][1] = cookie;
        return new RetrievePage(req, autoRef);
    }

    private static final String[][] pIE = { { "Accept", "image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, application/x-shockwave-flash, */*" }, { "Accept-Language", "en-us" }, { "Accept-Encoding", "gzip, deflate" }, { "User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)" } };

    public static RetrievePage makeAsIE(boolean autoRef) {
        return new RetrievePage(pIE, autoRef);
    }

    public static RetrievePage makeAsIE(String host, boolean autoRef) {
        String[][] pIE2 = new String[pIE.length + 1][2];
        System.arraycopy(pIE, 0, pIE2, 0, pIE.length);
        pIE2[pIE.length][0] = "Host";
        pIE2[pIE.length][1] = host;
        return new RetrievePage(pIE2, autoRef);
    }

    public RetrievePage() {
        props = null;
        autoRef = false;
    }

    public RetrievePage(String props[][]) {
        this.props = props;
        autoRef = false;
    }

    public RetrievePage(boolean autoRef) {
        props = null;
        this.autoRef = autoRef;
    }

    public RetrievePage(String props[][], boolean autoRef) {
        this.props = props;
        this.autoRef = autoRef;
    }

    private final String props[][];

    private final boolean autoRef;

    private void prepareUC(URLConnection uc, String urlToRetrieve) {
        if (autoRef) uc.setRequestProperty("Referer", urlToRetrieve);
        if (props != null) for (String[] prop : props) uc.setRequestProperty(prop[0], prop[1]);
    }

    private InputStream prepareInputStream(String urlToRetrieve) throws IOException {
        URL url = new URL(urlToRetrieve);
        URLConnection uc = url.openConnection();
        if (timeOut > 0) {
            uc.setConnectTimeout(timeOut);
            uc.setReadTimeout(timeOut);
        }
        prepareUC(uc, urlToRetrieve);
        InputStream is = uc.getInputStream();
        if ("gzip".equals(uc.getContentEncoding())) is = new GZIPInputStream(is);
        return is;
    }

    public void retrieveToStream(String urlToRetrieve, OutputStream stream) throws MalformedURLException, IOException {
        InputStream is = prepareInputStream(urlToRetrieve);
        byte[] buffer = new byte[16384];
        while (true) {
            int nBytes = is.read(buffer);
            if (nBytes == -1) break;
            stream.write(buffer, 0, nBytes);
        }
        is.close();
        stream.flush();
    }

    public String retrieve(String urlToRetrieve) throws MalformedURLException, IOException {
        InputStream is = prepareInputStream(urlToRetrieve);
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder output = new StringBuilder(16384);
        String str;
        boolean first = true;
        while ((str = in.readLine()) != null) {
            if (!first) output.append("\n");
            first = false;
            output.append(str);
        }
        in.close();
        return output.toString();
    }

    public void retrieveBinaryToFile(String urlToRetrieve, String fileDest) throws MalformedURLException, IOException {
        InputStream is = prepareInputStream(urlToRetrieve);
        OutputStream os = new FileOutputStream(fileDest);
        int nread;
        final int LEN = 65536;
        byte buff[] = new byte[LEN];
        while ((nread = is.read(buff, 0, LEN)) != -1) {
            if (nread > 0) os.write(buff, 0, nread);
        }
        is.close();
        os.close();
    }

    public void forceRetrieveBinaryToFile(String urlToRetrieve, String fileDest) throws MalformedURLException, IOException {
        for (int i = 1; i <= 10; i++) try {
            retrieveBinaryToFile(urlToRetrieve, fileDest);
            return;
        } catch (IOException e) {
            System.out.println("Exception " + e);
        }
        retrieveBinaryToFile(urlToRetrieve, fileDest);
    }

    public String forceRetrieve(String urlToRetrieve) throws MalformedURLException, IOException {
        for (int i = 1; i <= 10; i++) try {
            return retrieve(urlToRetrieve);
        } catch (IOException e) {
            System.out.println("Exception " + e);
        }
        return retrieve(urlToRetrieve);
    }

    public static String retrieveQ(String urlToRetrieve) throws MalformedURLException, IOException {
        URL url = new URL(urlToRetrieve);
        URLConnection uc = url.openConnection();
        InputStream is = uc.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder output = new StringBuilder(16384);
        String str;
        boolean first = true;
        while ((str = in.readLine()) != null) {
            if (!first) output.append("\n");
            first = false;
            output.append(str);
        }
        in.close();
        System.err.println(((HttpURLConnection) uc).getResponseMessage());
        return output.toString();
    }
}
