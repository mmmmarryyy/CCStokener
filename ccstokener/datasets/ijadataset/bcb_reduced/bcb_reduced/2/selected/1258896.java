package vi.log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.net.URL;
import java.net.URLConnection;

/**
 Class with a utility method to get a log stream from a URL.

 @author <a href="mailto:ivaradi@freemail.c3.hu">Istv�n V�radi</a>
 */
public class LogStream {

    /**
         Get a log stream from the specified URL.
         <p>
         If the protocol is <code>file</code> a file will be opened
         for writing. If the protocol is <code>logserv</code>, a connection
         to a log server will be opened. Otherwise an <code>URLConnection</code>
         is retrieved from the <code>URL</code> object and opened for
         writing if possible.

         @param url     the URL to use

         @return an output stream which can be used to write into the log

         @exception FileNotFoundException if the protocol is <code>file</code>,
         and the file specified cannot be opened for writing
         @exception UnknownHostException if the protocol is <code>logserv</code>,
         and the connection could not be established with the specified host
         @exception UnknownServiceException if the given URL does not support
         writing
         @exception IOException on other miscellaneous error conditions
         */
    public static OutputStream openLog(URL url) throws FileNotFoundException, UnknownHostException, UnknownServiceException, IOException {
        URLConnection urlc = url.openConnection();
        urlc.setDoOutput(true);
        return urlc.getOutputStream();
    }

    /**
         Open a log stream writer.

         @param url     the URL to use

         @return an output writer stream which can be used to write into the log
         */
    public static Writer openWriter(URL url) throws FileNotFoundException, UnknownHostException, UnknownServiceException, IOException {
        return new OutputStreamWriter(openLog(url));
    }

    /**
         Open a log writer.

         @param url     the URL to use

         @return an output writer stream which can be used to write into the log
         */
    public static Writer openWriter(URL url, String msgtempl) throws FileNotFoundException, UnknownHostException, UnknownServiceException, IOException {
        return new LogWriter(openWriter(url), msgtempl);
    }
}
