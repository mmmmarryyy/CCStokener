package com.triplea.rolap.api;

import com.triplea.rolap.server.Reply;
import com.triplea.rolap.server.Request;
import com.triplea.rolap.server.StringContent;
import com.triplea.rolap.plugins.IRequest;
import com.triplea.rolap.plugins.IReply;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * @author kiselev
 *         When: 21.09.2007 8:55:34
 */
public class RedirectHandler extends BaseHandler {

    protected static Logger _logger = Logger.getLogger(RedirectHandler.class.getName());

    public IReply handleRequest(IRequest request) {
        try {
            String path = request.uri().getPath();
            String query = request.uri().getQuery();
            URL url = new URL("http", "loader", 7777, path + "?" + query);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");
            Object ob = httpConnection.getContent();
            if (httpConnection.getResponseCode() == 200) {
                InputStream is = httpConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(is);
                BufferedReader stringReader = new BufferedReader(reader);
                StringBuffer out = new StringBuffer(4096);
                while (stringReader.ready()) {
                    out.append(stringReader.readLine()).append(CRLF);
                }
                Map<String, List<String>> headers = httpConnection.getHeaderFields();
                _logger.debug("-------------------filtered header start");
                String additionalHeader = null;
                for (String key : headers.keySet()) {
                    if (key != null && key.startsWith("X-PALO")) {
                        _logger.debug(key);
                        _logger.debug(" ");
                        List values = headers.get(key);
                        for (Object value : values) {
                            additionalHeader = key + " " + value;
                            _logger.debug(value);
                        }
                    }
                }
                _logger.debug("-------------------filtered header finish");
                return new Reply(Reply.OK, new StringContent(out), additionalHeader);
            } else {
                _logger.debug("Response Message = " + httpConnection.getResponseCode());
                try {
                    InputStream is = httpConnection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(is);
                    BufferedReader stringReader = new BufferedReader(reader);
                    StringBuffer out = new StringBuffer(4096);
                    while (stringReader.ready()) {
                        _logger.debug(stringReader.readLine());
                    }
                } catch (Throwable t) {
                }
                return BaseHandler.REPLY_SESSION_BAD;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return BaseHandler.REPLY_SESSION_BAD;
        }
    }
}
