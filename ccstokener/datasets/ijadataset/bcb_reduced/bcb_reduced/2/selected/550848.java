package sk.tonyb.library.web.downloader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.Logger;
import sk.tonyb.library.codebook.Encoding;

/** 
 * Class, which contains method, which download web pages. <br>
 * All methods of this class are synchronized. <br>
 * 
 * @author Anton Balucha
 * @since 19.05.2010
 * @last_modified 01.10.2011
 */
public class PageDownloader {

    /** Logger. */
    private static Logger logger = Logger.getLogger(PageDownloader.class);

    /** 
	 * Method, which returns content of page from inserted pageUrl. <br>
	 * 
	 * @author Anton Balucha
	 * @since 19.05.2010
	 * @last_modified 01.10.2011
	 * 
	 * @param pageUrl - URL of page. <br>
	 * @return 
	 * page - content of page. <br>
	 * null - if error occurs. <br>
	 * 
	 */
    public static synchronized String getPageContent(String pageUrl) {
        URL url = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        String line = null;
        StringBuilder page = null;
        if (pageUrl == null || pageUrl.trim().length() == 0) {
            return null;
        } else {
            try {
                url = new URL(pageUrl);
                inputStreamReader = new InputStreamReader(url.openStream());
                bufferedReader = new BufferedReader(inputStreamReader);
                page = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    page.append(line);
                    page.append("\n");
                }
            } catch (IOException e) {
                logger.error("IOException", e);
            } catch (Exception e) {
                logger.error("Exception", e);
            } finally {
                try {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                } catch (IOException e) {
                    logger.error("IOException", e);
                } catch (Exception e) {
                    logger.error("Exception", e);
                }
            }
        }
        if (page == null) {
            return null;
        } else {
            return page.toString();
        }
    }

    /** 
	 * Method, which returns content of web page. <br>
	 * 
	 * @author Anton Balucha
	 * @since 19.05.2010
	 * 
	 * @param prefix - prefix <code>http://</code> <br>
	 * @param host - host name <code>www.google.com</code> <br>
	 * @param parameter - other parameters <code>/someParameters</code> <br>
	 * @return 
	 * page - page as String. <br>
	 * null - if some error occurs. <br>
	 * 
	 */
    public static synchronized String getPageContent(String prefix, String host, String parameter) {
        URL url = null;
        URLConnection urlConnection = null;
        HttpURLConnection httpUrlConnection = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        String line = null;
        StringBuilder page = null;
        String finalPage = null;
        try {
            url = new URL(prefix + host + parameter);
            urlConnection = url.openConnection();
            httpUrlConnection = (HttpURLConnection) urlConnection;
            httpUrlConnection.setRequestProperty("User-Agent", "Opera/9.80 (Windows NT 5.1; U; en-GB) Presto/2.2.15 Version/10.00");
            httpUrlConnection.setRequestProperty("Accept", "text/html, application/xml;q=0.9, application/xhtml+xml, image/png, image/jpeg, image/gif, image/x-xbitmap, */*;q=0.1");
            httpUrlConnection.setRequestProperty("Accept-Language", "sk-SK,sk;q=0.9,en;q=0.8");
            httpUrlConnection.setRequestProperty("Accept-Charset", "iso-8859-1, utf-8, utf-16, *;q=0.1");
            httpUrlConnection.setRequestProperty("Referer", prefix + host + parameter);
            httpUrlConnection.setRequestProperty("Connection", "Keep-Alive, TE");
            httpUrlConnection.setRequestProperty("TE", "deflate, gzip, chunked, identity, trailers");
            httpUrlConnection.setReadTimeout(5 * 1000);
            httpUrlConnection.connect();
            inputStreamReader = new InputStreamReader(httpUrlConnection.getInputStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            page = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                page.append(line);
                page.append("\n");
            }
        } catch (MalformedURLException e) {
            logger.error("MalformedURLException", e);
        } catch (ProtocolException e) {
            logger.error("ProtocolException", e);
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException", e);
        } catch (IOException e) {
            logger.error("IOException", e);
        } catch (Exception e) {
            logger.error("Exception", e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                logger.error("IOException", e);
            } catch (Exception e) {
                logger.error("Exception", e);
            }
        }
        if (page == null) {
            return null;
        } else {
            try {
                finalPage = new String(page.toString().getBytes(), Encoding.UTF_8.getValue());
            } catch (UnsupportedEncodingException e) {
                logger.error("UnsupportedEncodingException", e);
                finalPage = null;
            } catch (Exception e) {
                logger.error("Exception", e);
                finalPage = null;
            }
            return finalPage;
        }
    }
}
