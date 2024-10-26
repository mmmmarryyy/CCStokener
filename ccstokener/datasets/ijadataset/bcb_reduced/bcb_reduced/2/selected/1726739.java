package com.razie.pub.comms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import razie.base.AttrAccess;
import com.razie.pub.base.data.ByteArray;
import com.razie.pub.base.log.Log;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

/**
 * communications utils
 * 
 * TODO detailed docs
 * 
 * @author razvanc99
 * 
 */
public class Comms {

    /**
     * Stream the response of a URL.
     * 
     * @param url can be local or remote
     * @return a string containing the text read from the URL. can be the result of a servlet, a web
     *         page or the contents of a local file. It's null if i couldn't read the file.
     */
    public static InputStream xpoststreamUrl2(String url, AttrAccess httpArgs, String content) {
        try {
            InputStream in = null;
            URLConnection uc = (new URL(url)).openConnection();
            uc.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(uc.getOutputStream());
            wr.write(content);
            wr.flush();
            logger.trace(3, "hdr: ", uc.getHeaderFields());
            String resCode = uc.getHeaderField(0);
            in = uc.getInputStream();
            if (!resCode.endsWith("200 OK")) {
                String msg = "Could not fetch data from url " + url + ", resCode=" + resCode;
                logger.trace(3, msg);
                RuntimeException rte = new RuntimeException(msg);
                if (uc.getContentType().endsWith("xml")) {
                    DOMParser parser = new DOMParser();
                    try {
                        parser.parse(new InputSource(in));
                    } catch (SAXException e) {
                        RuntimeException iex = new RuntimeException("Error while processing document at " + url);
                        iex.initCause(e);
                        throw iex;
                    }
                }
                throw rte;
            }
            return in;
        } catch (MalformedURLException e) {
            RuntimeException iex = new IllegalArgumentException();
            iex.initCause(e);
            throw iex;
        } catch (IOException e1) {
            throw new RuntimeException("Connection exception for url=" + url, e1);
        }
    }

    /**
     * Stream the response of a URL.
     * 
     * @param url can be local or remote
     * @return a string containing the text read from the URL. can be the result of a servlet, a web
     *         page or the contents of a local file. It's null if i couldn't read the file.
     */
    public static InputStream streamUrl(String url, AttrAccess... httpArgs) {
        try {
            InputStream in = null;
            if (url.startsWith("file:")) {
                in = (new URL(url)).openStream();
            } else if (url.startsWith("http:")) {
                URLConnection uc = (new URL(url)).openConnection();
                if (httpArgs.length > 0 && httpArgs[0] != null) {
                    for (String a : httpArgs[0].getPopulatedAttr()) uc.setRequestProperty(a, httpArgs[0].sa(a));
                }
                logger.trace(3, "hdr: ", uc.getHeaderFields());
                String resCode = uc.getHeaderField(0);
                in = uc.getInputStream();
                if (!resCode.endsWith("200 OK")) {
                    String msg = "Could not fetch data from url " + url + ", resCode=" + resCode;
                    logger.trace(3, msg);
                    CommRtException rte = new CommRtException(msg);
                    if (uc.getContentType().endsWith("xml")) {
                        DOMParser parser = new DOMParser();
                        try {
                            parser.parse(new InputSource(in));
                        } catch (SAXException e) {
                            RuntimeException iex = new CommRtException("Error while processing document at " + url);
                            iex.initCause(e);
                            throw iex;
                        }
                    }
                    throw rte;
                }
            } else {
                File file = new File(url);
                in = file.toURL().openStream();
            }
            return in;
        } catch (MalformedURLException e) {
            RuntimeException iex = new IllegalArgumentException();
            iex.initCause(e);
            throw iex;
        } catch (IOException e1) {
            CommRtException rte = new CommRtException("Connection exception for url=" + url);
            rte.initCause(e1);
            throw rte;
        }
    }

    /**
     * read the given stream into a String and return the string. It will read and concatenate
     * chunks of 100 bytes.
     * 
     * @param fis an input stream
     * @return a string containing the text read from the stream. It's null if i couldn't read the
     *         file.
     */
    public static String readStream(InputStream fis) {
        try {
            byte[] buff = new byte[ByteArray.BUFF_QUOTA];
            int n = 0;
            ByteArray xml = new ByteArray();
            while ((n = fis.read(buff, 0, ByteArray.BUFF_QUOTA)) > 0) {
                xml.append(buff, n);
            }
            return xml.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot read from input stream ...", e);
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * read the given stream into a String and return the string. It will read and concatenate
     * chunks of 100 bytes.
     * 
     * @param fis an input stream
     * @return a string containing the text read from the stream. It's null if i couldn't read the
     *         file.
     */
    public static String readStreamNoClose(InputStream fis) {
        try {
            byte[] buff = new byte[ByteArray.BUFF_QUOTA];
            int n = 0;
            ByteArray xml = new ByteArray();
            while ((n = fis.read(buff, 0, ByteArray.BUFF_QUOTA)) > 0) {
                xml.append(buff, n);
            }
            return xml.toString();
        } catch (Exception e) {
            throw new RuntimeException("Cannot read from input stream ...", e);
        } finally {
        }
    }

    /**
     * Serialize to string the response of a URL.
     * 
     * @param url can be local or remote
     * @return a string containing the text read from the URL. can be the result of a servlet, a web
     *         page or the contents of a local file. It's null if i couldn't read the file.
     */
    public static String readUrl(String url, AttrAccess... httpArgs) {
        InputStream s = streamUrl(url, httpArgs);
        if (s == null) {
            return null;
        }
        return readStream(s);
    }

    /** copy a stream using a simple SED like filter */
    public static void copyStreamSED(InputStream is, OutputStream fos, List<SedFilter> filters) {
        try {
            String line;
            fos.write(HttpHelper.httpHeader(HttpHelper.OK).getBytes());
            BufferedReader input = new BufferedReader(new InputStreamReader(is));
            while ((line = input.readLine()) != null) {
                for (SedFilter filter : filters) {
                    line = filter.filter(line);
                }
                fos.write(line.getBytes());
                fos.write('\n');
            }
            fos.flush();
            fos.close();
            input.close();
            is.close();
        } catch (IOException e1) {
            throw new CommRtException("Copystream failed: ", e1);
        }
    }

    /** is this the localhost? x can be either hostname or IP, ipv4, ipv6 etc
     * 
     *  NOTE this is part of authorization chain
     */
    public static boolean isLocalhost(String x) {
        if ("127.0.0.1".equals(x) || "0:0:0:0:0:0:0:1".equals(x) || "localhost".equals(x)) return true;
        return false;
    }

    static Log logger = Log.create(Comms.class.getName());
}
