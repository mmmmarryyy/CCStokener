package org.eclipse.update.internal.core.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.internal.core.IStatusCodes;

public class OtherResponse extends AbstractResponse {

    protected URL url;

    protected InputStream in;

    protected long lastModified;

    protected OtherResponse(URL url) {
        this.url = url;
    }

    public InputStream getInputStream() throws IOException {
        if (in == null && url != null) {
            if (connection == null) connection = url.openConnection();
            in = connection.getInputStream();
            this.lastModified = connection.getLastModified();
        }
        return in;
    }

    public void close() {
        if (null != in) {
            try {
                in.close();
            } catch (IOException e) {
            }
            in = null;
        }
    }

    /**
	 * @see IResponse#getInputStream(IProgressMonitor)
	 */
    public InputStream getInputStream(IProgressMonitor monitor) throws IOException, CoreException {
        if (in == null && url != null) {
            if (connection == null) connection = url.openConnection();
            if (monitor != null) {
                this.in = openStreamWithCancel(connection, monitor);
            } else {
                this.in = connection.getInputStream();
            }
            if (in != null) {
                this.lastModified = connection.getLastModified();
            }
        }
        return in;
    }

    public long getContentLength() {
        if (connection != null) return connection.getContentLength();
        return 0;
    }

    public int getStatusCode() {
        return IStatusCodes.HTTP_OK;
    }

    public String getStatusMessage() {
        return "";
    }

    public long getLastModified() {
        if (lastModified == 0 && connection != null) {
            lastModified = connection.getLastModified();
        }
        return lastModified;
    }
}
