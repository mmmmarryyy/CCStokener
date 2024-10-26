package com.google.api.chart;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.google.api.chart.encoding.DataEncoder;
import com.google.api.chart.encoding.JoinHelper;

public class URLHelper {

    public static String CHART_URL = "http://chart.apis.google.com/chart?";

    /**
	 * Forms an HTTP request and parses the response for a detection.
	 */
    protected static byte[] retrieveURL(final String url) throws Exception {
        try {
            final HttpURLConnection uc = (HttpURLConnection) new URL(url).openConnection();
            try {
                byte[] buf = new byte[1024];
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                InputStream in = uc.getInputStream();
                while (true) {
                    int rc = in.read(buf);
                    if (rc <= 0) break;
                    bout.write(buf, 0, rc);
                }
                return bout.toByteArray();
            } finally {
                uc.getInputStream().close();
                if (uc.getErrorStream() != null) {
                    uc.getErrorStream().close();
                }
            }
        } catch (Exception ex) {
            throw new Exception("Error retrieving data: " + ex.getMessage(), ex);
        }
    }

    public static String buildURL(ChartTypeMaker type, Dimension size, DataEncoder dataEncoder, List<DataSeriesMaker> dataSeries) {
        StringBuilder buf = new StringBuilder(CHART_URL);
        List<String> parameters = new ArrayList<String>();
        List<String> chmParameters = new ArrayList<String>();
        parameters.add("cht=" + type.toString());
        parameters.add("chs=" + size.width + "x" + size.height);
        parameters.add("chd=" + dataEncoder.encode(dataSeries));
        type.fillParameters(parameters, chmParameters);
        for (Iterator<String> it = parameters.iterator(); it.hasNext(); ) {
            buf.append(it.next());
            if (it.hasNext()) buf.append("&");
        }
        if (chmParameters.size() > 0) buf.append("&chm=" + JoinHelper.join(JoinHelper.array(chmParameters, String.class), "|"));
        return buf.toString();
    }
}
