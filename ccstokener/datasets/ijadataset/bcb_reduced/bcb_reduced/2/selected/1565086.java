package org.eclipse.update.internal.core.connection;

import java.io.*;
import java.net.*;
import org.eclipse.core.runtime.*;
import org.eclipse.update.internal.core.IStatusCodes;

public class FileResponse implements IResponse {

    protected URL url;

    protected long lastModified;

    protected FileResponse(URL url) {
        this.url = url;
    }

    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

    public InputStream getInputStream(IProgressMonitor monitor) throws IOException, CoreException {
        return getInputStream();
    }

    public long getContentLength() {
        return 0;
    }

    public int getStatusCode() {
        return IStatusCodes.HTTP_OK;
    }

    public void close() {
    }

    public String getStatusMessage() {
        return "";
    }

    public long getLastModified() {
        if (lastModified == 0) {
            File f = new File(url.getFile());
            if (f.isDirectory()) f = new File(f, "site.xml");
            lastModified = f.lastModified();
        }
        return lastModified;
    }
}
