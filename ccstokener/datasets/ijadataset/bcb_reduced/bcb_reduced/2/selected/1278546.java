package view;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

public class FindAndDownloadCdImage extends Thread {

    final String SEARCH_SITE = "http://www.coverhunt.com/search/";

    String searchPhrase;

    String localFileName;

    public FindAndDownloadCdImage(String searchPhrase, String localFileName) {
        this.searchPhrase = searchPhrase;
        this.localFileName = localFileName;
    }

    public void run() {
        downloadImage(getImageUrl(SEARCH_SITE + searchPhrase), localFileName);
    }

    private static void downloadImage(URL url, String localFileName) {
        FileOutputStream fos = null;
        URL fileUrl = null;
        Bundle bundle = Activator.getDefault().getBundle();
        Path path = new Path("album covers/empty_disk.jpg");
        URL localUrl = FileLocator.find(bundle, path, Collections.EMPTY_MAP);
        try {
            fileUrl = FileLocator.toFileURL(localUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            InputStream is = url.openStream();
            String actualPath = fileUrl.getPath();
            actualPath = actualPath.substring(1);
            actualPath = actualPath.substring(0, actualPath.lastIndexOf("/"));
            fos = new FileOutputStream(actualPath + "/" + localFileName);
            int oneChar, count = 0;
            while ((oneChar = is.read()) != -1) {
                fos.write(oneChar);
                count++;
            }
            is.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static URL getImageUrl(String urlToSearchString) {
        String expr = "<img .*?src=[\"']?([^'\">]+)[\"']?.*?>";
        String image = null;
        URL imageUrl = null;
        try {
            Pattern patt = Pattern.compile(expr, Pattern.DOTALL | Pattern.UNIX_LINES);
            Matcher m = patt.matcher(getURLContent(urlToSearchString));
            if (m.find() && m.find()) {
                image = m.group(1);
                imageUrl = new URL(image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageUrl;
    }

    public static String getURLContent(String urlToSearchString) throws IOException {
        URL url = new URL(urlToSearchString);
        URLConnection conn = url.openConnection();
        String encoding = conn.getContentEncoding();
        if (encoding == null) encoding = "ISO-8859-1";
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), encoding));
        StringBuilder sb = new StringBuilder(16384);
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }
}
