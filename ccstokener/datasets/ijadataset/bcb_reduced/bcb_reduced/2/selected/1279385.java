package com.doshiland.fx4web.jsf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A phase-listener that executes a configurable action corresponding to the
 * given view before rendering it. It picks up the action method binding
 * expression from the "view-init.properties" file where key is the view-id and
 * the values is the method-binding expression. Just before rendering a view,
 * it's initializing method-binding expression is invoked. This allows
 * initializing the beans before showing a page. For example, you could use it
 * to load all the data from the database that the page migtht show. The methods
 * in the method binding expression should take no parameters and return void.
 * 
 * This listener must be included as a listener in JSF lifecycle.
 * 
 * See the "view-init.properties" file to learn how to write the configuration for this.
 * 
 * @author <a href="mailto:jitesh@doshiland.com">Jitesh Doshi</a>
 */
public class ViewInitListener implements PhaseListener {

    private static final Log log = LogFactory.getLog(ViewInitListener.class);

    private static final String VIEW_INIT_CONFIG = "/view-init.properties";

    private Properties config;

    public ViewInitListener() throws IOException {
        URL url = this.getClass().getResource(VIEW_INIT_CONFIG);
        log.debug("Loading configuration from: " + url);
        config = new Properties();
        InputStream in = url.openStream();
        config.load(in);
        in.close();
    }

    public void afterPhase(PhaseEvent event) {
    }

    public void beforePhase(PhaseEvent event) {
        FacesContext facesContext = event.getFacesContext();
        UIViewRoot root = facesContext.getViewRoot();
        String viewId = root.getViewId();
        log.debug("Before render view: " + viewId);
        String actions = (String) config.get(viewId);
        if (actions != null && actions.length() != 0) {
            StringTokenizer tokenizer = new StringTokenizer(actions);
            while (tokenizer.hasMoreTokens()) {
                String action = tokenizer.nextToken();
                log.debug("Finding view-init action: " + action);
                MethodBinding binding = facesContext.getApplication().createMethodBinding(action, new Class[] {});
                log.debug("Invoking view-init action: " + action);
                binding.invoke(facesContext, new Object[] {});
            }
        }
    }

    public PhaseId getPhaseId() {
        return PhaseId.RENDER_RESPONSE;
    }
}
