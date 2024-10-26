package org.javacraft.qa.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author jan
 */
public class SimpleLearningTest {

    @Test
    public void testTrafficLength2() throws Exception {
        String url = "http://trafficinfoservice.be-mobile.be/ContentService.asmx";
        String charset = "UTF-8";
        File requestFile = new File(this.getClass().getResource("/TrafficLengthReportPast24H.vm").toURI());
        final String request = FileUtils.readFileToString(requestFile);
        URLConnection urlConnection = new URL(url).openConnection();
        urlConnection.setUseCaches(false);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("accept-charset", charset);
        urlConnection.setRequestProperty("content-type", "text/xml; charset=utf-8");
        urlConnection.setRequestProperty("Content-Length", "" + request.length());
        urlConnection.setRequestProperty("SOAPAction", "http://www.be-mobile.be/TrafficLengthReportPast24H");
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(urlConnection.getOutputStream(), charset);
            writer.write(request);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException logOrIgnore) {
                }
            }
        }
        InputStream result = urlConnection.getInputStream();
        System.out.println(IOUtils.readLines(result));
    }
}
