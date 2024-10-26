package org.mobicents.servlet.sip.alerting;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class EricssonSmsAlertServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(EricssonSmsAlertServlet.class);

    private static final String RESPONSE_BEGIN = "<HTML><BODY>";

    private static final String RESPONSE_END = "</BODY></HTML>";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        logger.info("the SmsAlertServlet has been started");
    }

    /**
	 * Handle the HTTP POST method on which alert can be sent so that the app
	 * sends an sms based on that
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String key = getServletContext().getInitParameter("key");
        String alertId = request.getParameter("alertId");
        String tel = request.getParameter("tel");
        String alertText = request.getParameter("alertText");
        if (alertText == null || alertText.length() < 1) {
            byte[] content = new byte[request.getContentLength()];
            request.getInputStream().read(content, 0, request.getContentLength());
            alertText = new String(content);
        }
        alertText = "Alert Id : " + alertId + ",  Alert Details :" + alertText;
        StringBuffer responseBuffer = new StringBuffer();
        String sData;
        try {
            sData = ("key=" + key);
            sData += ("&to=" + tel);
            sData += ("&message=" + alertText);
            URI uri = new URI("https", null, "sms.labs.ericsson.net", -1, "/send", sData, null);
            URL url = uri.toURL();
            if (logger.isInfoEnabled()) {
                logger.info("Got an alert : \n alertID : " + alertId + " \n tel : " + tel + " \n text : " + alertText);
                logger.info("Ericsson URL : \n URL : " + url);
            }
            InputStream in = url.openConnection().getInputStream();
            byte[] buffer = new byte[10000];
            int len = in.read(buffer);
            for (int q = 0; q < len; q++) responseBuffer.append((char) buffer[q]);
            in.close();
        } catch (Exception e) {
            logger.error("Exception caught sending SMS", e);
            responseBuffer.append(e.getMessage());
        }
        sendHttpResponse(response, RESPONSE_BEGIN + "response : Number of SMS Sent " + responseBuffer.toString() + RESPONSE_END);
    }

    /**
	 * @param response
	 * @throws IOException
	 */
    private void sendHttpResponse(HttpServletResponse response, String body) throws IOException {
        PrintWriter out;
        response.setContentType("text/html");
        out = response.getWriter();
        out.println(body);
        out.close();
    }
}
