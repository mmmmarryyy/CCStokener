package com.yerihyo.yeritools.net;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import com.yerihyo.yeritools.ErrorToolkit;
import com.yerihyo.yeritools.io.FileToolkit;
import com.yerihyo.yeritools.text.StringToolkit;

public class HttpToolkit {

    public static Pattern TAG_PATTERN = Pattern.compile("</?[a-z!][^>]*>");

    public static InputStream createHTMLInputStream(URL url) throws MalformedURLException, IOException {
        return createHTMLInputStream(url, null);
    }

    public static InputStream createHTMLInputStream(URL url, String[][] argumentArray) throws MalformedURLException, IOException {
        return createHTMLInputStream(url.toExternalForm(), argumentArray);
    }

    public static InputStream createHTMLInputStream(String urlString, String[][] argumentArray) throws MalformedURLException, IOException {
        if (argumentArray != null) {
            boolean urlHasArguments = (urlString.indexOf("?") > 0);
            for (String[] argument : argumentArray) {
                if (urlHasArguments) {
                    urlString += "&";
                } else {
                    urlString += "?";
                    urlHasArguments = true;
                }
                urlString += argument[0] + "=" + argument[1];
            }
        }
        return (new URL(urlString)).openConnection().getInputStream();
    }

    public static Reader createHTMLReader(URL url) throws MalformedURLException, IOException {
        return new InputStreamReader(createHTMLInputStream(url));
    }

    public static Reader createHTMLReader(URL url, Charset charset) throws MalformedURLException, IOException {
        return new InputStreamReader(createHTMLInputStream(url), charset);
    }

    public static Reader createHTMLReader(URL url, String[][] argumentArray) throws MalformedURLException, IOException {
        return new InputStreamReader(createHTMLInputStream(url, argumentArray));
    }

    public static void copyHTMLFile(URL url, File file) throws FileNotFoundException, MalformedURLException, IOException {
        BufferedReader reader = null;
        PrintWriter writer = null;
        String oneline = null;
        try {
            reader = new BufferedReader(new InputStreamReader(createHTMLInputStream(url)));
            writer = new PrintWriter(file);
            while ((oneline = reader.readLine()) != null) {
                writer.println(oneline);
            }
        } finally {
            reader.close();
            writer.close();
        }
    }

    public static StringBuffer getHTMLString(URL url) throws MalformedURLException, IOException {
        Reader reader = null;
        StringBuffer returnValue = new StringBuffer();
        char[] buffer = new char[1024];
        int length = 0;
        try {
            reader = new InputStreamReader(createHTMLInputStream(url));
            while ((length = reader.read(buffer)) > 0) {
                returnValue.append(buffer, 0, length);
            }
        } finally {
            reader.close();
        }
        return returnValue;
    }

    public static void parse(URL url, HTMLEditorKit.ParserCallback pcb) throws MalformedURLException, IOException {
        Reader reader = HttpToolkit.createHTMLReader(url);
        ParserDelegator delegator = new ParserDelegator();
        delegator.parse(reader, pcb, true);
        reader.close();
    }

    public static void main(String[] args) throws IOException {
        test1();
    }

    public static int getTagEnd(HTML.Tag tag, int startPos, boolean isStartTag) {
        String tagString = tag.toString();
        int tagLength = tagString.length();
        int bracketLength = 2;
        if (!isStartTag) {
            bracketLength = 3;
        }
        return startPos + tagLength + bracketLength;
    }

    public static class LinkNameExtractor extends HTMLEditorKit.ParserCallback {

        private int linkNameStartPosition = -1;

        private StringBuffer stringBuffer = new StringBuffer();

        private List<String> linkNameList = new ArrayList<String>();

        private boolean isInATag() {
            return linkNameStartPosition != -1;
        }

        private void reset() {
            linkNameStartPosition = -1;
            stringBuffer = new StringBuffer();
        }

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if (t == HTML.Tag.A) {
                linkNameStartPosition = getTagEnd(t, pos, true) + 1;
            }
        }

        public void handleEndTag(HTML.Tag t, int pos) {
            if (t == HTML.Tag.A) {
                if (!isInATag()) {
                    return;
                }
                linkNameList.add(stringBuffer.toString());
                reset();
            }
        }

        public void handleText(char[] data, int pos) {
            if (!isInATag()) {
                return;
            }
            stringBuffer.append(data);
        }

        public List<String> getLinkNameList() {
            return linkNameList;
        }
    }

    public static class LinkAddressExtractor extends HTMLEditorKit.ParserCallback {

        private List<String> linkAddressList = new ArrayList<String>();

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if (t == HTML.Tag.A) {
                Object o = a.getAttribute(HTML.Attribute.HREF);
                if (o == null) {
                    return;
                }
                String linkAddress = (String) o;
                linkAddressList.add(linkAddress);
            }
        }

        public List<String> getLinkAddressList() {
            return linkAddressList;
        }
    }

    public static URL makeAbsoluteURL(URL baseURL, String urlString) throws MalformedURLException {
        if (urlString == null) {
            return null;
        }
        URL returnURL = null;
        if (urlString.contains("://")) {
            returnURL = new URL(urlString);
        } else {
            returnURL = new URL(baseURL, urlString);
        }
        return returnURL;
    }

    public static List<URL> makeAbsoluteURLList(URL baseURL, List<? extends String> urlStringList) {
        List<URL> urlList = new ArrayList<URL>();
        for (String urlString : urlStringList) {
            URL tmpURL = null;
            try {
                tmpURL = makeAbsoluteURL(baseURL, urlString);
            } catch (MalformedURLException ex) {
                continue;
            }
            urlList.add(tmpURL);
        }
        return urlList;
    }

    public static void downloadFile(URL fileURL, File localFile, PrintStream oStream) {
        if (fileURL == null) {
            if (oStream != null) {
                oStream.println("While downloading.. downloading URL is null.");
            }
            return;
        }
        if (localFile == null) {
            if (oStream != null) {
                oStream.append("While downloading '");
                oStream.append(fileURL.toExternalForm());
                oStream.append("' local filename is null");
                oStream.append(StringToolkit.newLine());
                oStream.flush();
            }
            return;
        }
        if (oStream != null) {
            oStream.append("Downloding '");
            oStream.append(fileURL.toExternalForm());
            oStream.append("' -> '");
            oStream.append(localFile.getAbsolutePath());
            oStream.append("'");
            oStream.append(StringToolkit.newLine());
            oStream.flush();
        }
        if (!localFile.exists()) {
            boolean result = false;
            try {
                result = localFile.createNewFile();
            } catch (IOException ex) {
                ErrorToolkit.printShortenedStackTrace(ex, oStream);
                return;
            }
            if (!result) {
                if (oStream != null) {
                    oStream.append("Failed creating file '");
                    oStream.append(localFile.getAbsolutePath());
                    oStream.append("'");
                    oStream.append(StringToolkit.newLine());
                    oStream.flush();
                }
                return;
            }
        }
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(localFile));
            conn = fileURL.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    public static void downloadFileTo(URL fileURL, File baseFolder, PrintStream oStream) throws MalformedURLException {
        if (fileURL == null) {
            return;
        }
        String filepath = fileURL.getFile();
        String filename = FileToolkit.getFilename(filepath);
        if (baseFolder == null) {
            baseFolder = new File(".");
        }
        File localFile = new File(baseFolder, filename);
        downloadFile(fileURL, localFile, oStream);
    }

    public static void downloadFileTo(List<? extends URL> fileURLList, File baseFolder, PrintStream oStream) {
        for (URL fileURL : fileURLList) {
            try {
                downloadFileTo(fileURL, baseFolder, oStream);
            } catch (MalformedURLException e) {
                ErrorToolkit.printShortenedStackTrace(e);
            }
        }
    }

    @SuppressWarnings("unused")
    private static void test1() throws FileNotFoundException, MalformedURLException, IOException {
        try {
            copyHTMLFile(new URL("http://www.cmu.edu/icc/calendar/index.shtml"), new File("data/OIESchedule.html"));
        } finally {
        }
    }
}
