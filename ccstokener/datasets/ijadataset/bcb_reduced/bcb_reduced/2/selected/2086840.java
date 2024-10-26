package com.valotas.taglibs.scripting.groovy;

import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletContext;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;

/**
 * An simple implementation of the ResourceConnector interface.
 * It just looks for the script inside the /WEB-INF/scriptags/ 
 * 
 * @author valotas@gmail.com
 * @version , 09 Jan 2009
 *
 */
public class SimpleResourceConnector implements ResourceConnector {

    private final ServletContext context;

    /**
	 * We are working on a ServletContainer and we need the
	 * {@link ServletContext} of the web application in order
	 * to search in it for the groovy scripts.
	 * 
	 * @param context
	 */
    public SimpleResourceConnector(ServletContext context) {
        this.context = context;
    }

    /**
	 * 	The main implementation of our class.
	 * 	see {@link groovy.util.ResourceConnector} for more information.
	 */
    public URLConnection getResourceConnection(String name) throws ResourceException {
        if (context == null) throw new ResourceException("There is no ServletContext to get the requested resource");
        URL url = null;
        try {
            url = context.getResource("/WEB-INF/scriptags/" + name);
            return url.openConnection();
        } catch (Exception e) {
            throw new ResourceException(String.format("Resource '%s' could not be found (url: %s)", name, url), e);
        }
    }
}
