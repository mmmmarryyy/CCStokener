package org.aspencloud.simple9.server.response;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import org.aspencloud.simple9.server.enums.ContentType;
import org.aspencloud.simple9.server.enums.StatusCode;

public class ResourceResponse extends ServerResponse {

    private URL url;

    public ResourceResponse(String resource) {
        int ix = resource.lastIndexOf('.');
        if (ix > 0 && ix < resource.length() - 1) {
            setContentType(ContentType.get(resource.substring(ix + 1), ContentType.HTML));
        } else {
            setContentType(ContentType.HTML);
        }
        switch(getContentType().getPrimaryType()) {
            case CSS:
                resource = "/styles" + resource;
                break;
            case IMG:
                resource = "/images" + resource;
                break;
            case JS:
                resource = "/scripts" + resource;
                break;
        }
        url = getClass().getResource(resource);
        if (url != null) {
            setStatus(StatusCode.OK);
        } else {
            setStatus(StatusCode.NOT_FOUND);
            setContentType(ContentType.HTML);
        }
    }

    public void send(PrintStream out) {
        out.println(getStatusHeader());
        for (String header : getHeaders()) {
            out.println(header);
        }
        out.println();
        if (url != null) {
            int n;
            byte[] buf = new byte[2048];
            BufferedInputStream bis = null;
            try {
                bis = new BufferedInputStream(url.openStream());
                while ((n = bis.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }
}
