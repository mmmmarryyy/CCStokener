package info.jonclark.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/**
 * Utilities for getting data from the web
 */
public class NetUtils {

    public static String getStreamAsString(InputStream stream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = in.readLine()) != null) builder.append(line + "\n");
        return builder.toString();
    }

    public static void saveStreamToFile(InputStream stream, File file) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        byte[] b = new byte[1024];
        int n;
        while ((n = stream.read(b)) > 0) out.write(b, 0, n);
        out.close();
    }

    /**
	 * Get an input stream for the data at a given URL
	 * 
	 * @param urlToRetrieve
	 *            The URL from which we will get data
	 * @return An input stream with the data at <code>urlToRetrieve</code>
	 * @throws IOException
	 */
    public static InputStream getUrlStream(String urlToRetrieve) throws IOException {
        URL url = new URL(urlToRetrieve);
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        String encoding = conn.getContentEncoding();
        if (encoding != null) {
            if (encoding.equals("gzip")) in = new GZIPInputStream(in);
        }
        return in;
    }

    public static InetSocketAddress[] toInetSocketAddressArray(String[] str, String requiredProtocol) {
        InetSocketAddress[] n = new InetSocketAddress[str.length];
        for (int i = 0; i < str.length; i++) {
            n[i] = toInetSocketAddress(str[i], requiredProtocol);
        }
        return n;
    }

    public static InetSocketAddress toInetSocketAddress(String str, String requiredProtocol) {
        String actualProtocol = StringUtils.substringBefore(str, "://");
        if (!actualProtocol.equals(requiredProtocol)) {
            throw new RuntimeException("Incorrect protocol: " + actualProtocol + "; Expected: " + requiredProtocol);
        }
        String remainder = StringUtils.substringAfter(str, "://");
        String[] hostPort = StringUtils.tokenize(remainder, ":", 2);
        int nPort = Integer.parseInt(hostPort[1]);
        return new InetSocketAddress(hostPort[0], nPort);
    }

    public static String formatAddress(InetSocketAddress address, String protocol) {
        return protocol + "://" + address.getHostName() + ":" + address.getPort();
    }

    public static String formatAddressArray(InetSocketAddress[] addresses, String protocol) {
        if (addresses.length > 0) {
            final StringBuilder builder = new StringBuilder();
            for (final InetSocketAddress address : addresses) builder.append(formatAddress(address, protocol) + ", ");
            return StringUtils.cutCharsFromEnd(builder.toString(), 2);
        } else {
            return "";
        }
    }

    /**
	 * Main method, for demo purposes
	 * 
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        InputStream stream = NetUtils.getUrlStream("http://www.gutenberg.org/feeds/catalog.rdf.zip");
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println(line);
        }
    }
}
