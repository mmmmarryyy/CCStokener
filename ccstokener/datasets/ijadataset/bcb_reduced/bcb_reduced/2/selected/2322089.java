package org.evertree.breakfast.component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.evertree.breakfast.Component;
import org.evertree.breakfast.Parameter;
import org.evertree.breakfast.Provider;
import org.evertree.breakfast.UnsetParameterException;

public class URLSource extends Component implements Provider {

    protected Parameter url = new Parameter("url", URL.class);

    protected String content;

    public void setUrl(Object url) {
        this.url.setValue(url);
    }

    @Override
    public void execute() throws Exception {
        if (url.isNull()) {
            throw new UnsetParameterException(url.getName());
        }
        ByteArrayOutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            out = new ByteArrayOutputStream();
            conn = openConnection();
            handleConnection(conn);
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
        this.content = out.toString();
    }

    protected void handleConnection(URLConnection conn) {
    }

    @Override
    public Object provide() {
        return content;
    }

    protected URLConnection openConnection() throws Exception {
        return ((URL) url.getValue()).openConnection();
    }
}
