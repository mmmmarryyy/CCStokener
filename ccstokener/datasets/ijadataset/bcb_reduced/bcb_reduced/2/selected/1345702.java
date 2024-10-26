package org.specrunner.source.resource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.specrunner.source.ISource;
import org.specrunner.source.resource.EType;
import org.specrunner.source.resource.IResource;
import org.specrunner.source.resource.Position;
import org.specrunner.source.resource.ResourceException;
import org.specrunner.util.UtilLog;
import org.specrunner.util.UtilResources;

/**
 * Default resource to be written in header part.
 * 
 * @author Thiago Santos
 * 
 */
public abstract class AbstractResourceHeader implements IResource {

    protected static final int BUFFER_SIZE = 1024;

    private ISource parent;

    private String path;

    private EType type;

    private Position position;

    public AbstractResourceHeader(ISource parent, String path, EType ref, Position position) {
        this.parent = parent;
        this.path = path;
        this.type = ref;
        this.position = position;
    }

    @Override
    public ISource getParent() {
        return parent;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public EType getType() {
        return type;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    protected List<URL> getURLs() throws ResourceException {
        List<URL> files;
        try {
            files = UtilResources.getFileList(path);
            Collections.reverse(files);
        } catch (IOException e) {
            throw new ResourceException(e);
        }
        return files;
    }

    protected InputStream[] getInputStreams(List<URL> files) throws ResourceException {
        InputStream[] result = new InputStream[files.size()];
        int i = 0;
        try {
            for (URL url : files) {
                result[i++] = url.openStream();
            }
        } catch (IOException e) {
            for (int j = 0; j < i; j++) {
                try {
                    if (UtilLog.LOG.isDebugEnabled()) {
                        UtilLog.LOG.debug("Closing " + files.get(j));
                    }
                    result[j].close();
                } catch (IOException e1) {
                    if (UtilLog.LOG.isDebugEnabled()) {
                        UtilLog.LOG.debug(e1.getMessage(), e1);
                    }
                }
            }
            if (UtilLog.LOG.isDebugEnabled()) {
                UtilLog.LOG.debug(e.getMessage(), e);
            }
            throw new ResourceException(e);
        }
        return result;
    }

    protected void writeTo(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int size = 0;
        while ((size = in.read(buffer)) > 0) {
            out.write(buffer, 0, size);
        }
        out.flush();
    }

    protected void writeAllTo(List<URL> files, OutputStream out) throws ResourceException {
        InputStream[] ins = null;
        int i = 0;
        try {
            ins = getInputStreams(files);
            for (InputStream in : ins) {
                writeTo(in, out);
                in.close();
                i++;
            }
        } catch (IOException e) {
            if (ins != null) {
                for (int j = 0; j < i; j++) {
                    try {
                        if (UtilLog.LOG.isDebugEnabled()) {
                            UtilLog.LOG.debug("Closing " + ins[j]);
                        }
                        ins[j].close();
                    } catch (IOException e1) {
                        if (UtilLog.LOG.isDebugEnabled()) {
                            UtilLog.LOG.debug(e1.getMessage(), e1);
                        }
                    }
                }
            }
            if (UtilLog.LOG.isDebugEnabled()) {
                UtilLog.LOG.debug(e.getMessage(), e);
            }
        }
    }

    @Override
    public String toString() {
        return "AbstractResourceHeader [path=" + path + ",type=" + type + ", position=" + position + "]";
    }
}
