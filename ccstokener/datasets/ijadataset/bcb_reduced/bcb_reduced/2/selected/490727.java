package com.cirnoworks.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.w3c.dom.Element;
import com.cirnoworks.fisce.intf.FiScEVM;
import com.cirnoworks.fisce.intf.IToolkit;
import com.cirnoworks.fisce.intf.VMCriticalException;

/**
 * @author Cloudee
 * 
 */
public class ResourceFetcherToolkit implements IToolkit {

    private final ResourceFetcher resourceFetcher;

    /**
	 * @param resourceFetcher
	 */
    public ResourceFetcherToolkit(ResourceFetcher resourceFetcher) {
        super();
        this.resourceFetcher = resourceFetcher;
    }

    @Override
    public void setContext(FiScEVM context) {
    }

    @Override
    public void setupContext() {
    }

    @Override
    public InputStream getResourceByClassName(String className) {
        URL url = resourceFetcher.getResource("/fisce_scripts/" + className + ".class");
        if (url == null) {
            return null;
        } else {
            try {
                return url.openStream();
            } catch (IOException e) {
                return null;
            }
        }
    }

    @Override
    public void saveData(Element data) throws VMCriticalException {
    }

    @Override
    public void loadData(Element data) throws VMCriticalException {
    }
}
