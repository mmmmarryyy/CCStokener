package org.tolven.url.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.tolven.app.MenuLocal;
import org.tolven.app.entity.MSResource;
import org.tolven.core.TolvenRequest;
import org.tolven.logging.TolvenLogger;

public class Handler extends URLStreamHandler {

    private MenuLocal menuBean;

    public Handler() {
        super();
        TolvenLogger.info("RestHandler", Handler.class);
        try {
            InitialContext ctx = new InitialContext();
            menuBean = (MenuLocal) ctx.lookup("java:global/tolven/tolvenEJB/MenuBean!org.tolven.app.MenuLocal");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        if (url.getQuery() == null) throw new IllegalStateException("Missing TemplateAccount number in rest URL " + url);
        MSResource msResource = null;
        try {
            long templateAccountId = Long.parseLong(url.getQuery());
            msResource = menuBean.findMSResource(templateAccountId, url.getPath());
        } catch (Exception e) {
            throw new IllegalStateException("Resource not found in database: " + url, e);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(msResource.getValue());
        return new RestConnection(url, bais);
    }
}
