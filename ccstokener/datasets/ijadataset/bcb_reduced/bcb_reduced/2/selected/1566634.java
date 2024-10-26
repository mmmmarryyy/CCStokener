package jeeves.services.http;

import java.util.*;
import org.jdom.*;
import jeeves.constants.*;
import jeeves.interfaces.*;
import jeeves.server.*;
import jeeves.server.context.ServiceContext;
import jeeves.utils.*;
import java.net.*;
import java.io.*;

/** Returns a specific record given its id
  */
public class Get implements Service {

    public static final String URL_PARAM_NAME = "url";

    private static final int BUFSIZE = 1024;

    private String configUrl;

    public void init(String appPath, ServiceConfig params) throws Exception {
        configUrl = params.getValue(URL_PARAM_NAME);
    }

    public Element exec(Element params, ServiceContext context) throws Exception {
        String sUrl = params.getChildText(URL_PARAM_NAME);
        if (sUrl == null) sUrl = configUrl;
        if (sUrl == null) throw new IllegalArgumentException("The '" + URL_PARAM_NAME + "' configuration parameter is missing");
        boolean first = new URL(sUrl).getQuery() == null;
        StringBuffer sb = new StringBuffer(sUrl);
        for (Iterator iter = params.getChildren().iterator(); iter.hasNext(); ) {
            Element child = (Element) iter.next();
            if (child.getName().equals(URL_PARAM_NAME)) continue;
            if (first) {
                first = false;
                sb.append("?");
            } else sb.append("&");
            sb.append(child.getName()).append("=").append(URLEncoder.encode(child.getText(), "UTF-8"));
        }
        URL url = new URL(sb.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        InputStream input = conn.getInputStream();
        return Xml.loadStream(input);
    }
}
