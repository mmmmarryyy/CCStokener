package org.eiichiro.jazzmaster.examples.petstore.ui.renderkit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author edburns
 */
public class AjaxPhaseListener implements PhaseListener {

    private static final String SCRIPT_VIEW_ID = "ajax-script";

    private static final String SCRIPT_RESOURCE_NAME = "/META-INF/ajax.js";

    public AjaxPhaseListener() {
    }

    public static ClassLoader getCurrentLoader(Object fallbackClass) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = fallbackClass.getClass().getClassLoader();
        }
        return loader;
    }

    public void afterPhase(PhaseEvent event) {
        if (-1 != event.getFacesContext().getViewRoot().getViewId().indexOf(SCRIPT_VIEW_ID) && PhaseId.RESTORE_VIEW == event.getPhaseId()) {
            renderScript(event);
        } else {
            String abortPhaseParam = (String) event.getFacesContext().getExternalContext().getRequestParameterMap().get("bpcatalog.abortPhase");
            int phaseOrdinal = -1;
            if (null != abortPhaseParam) {
                try {
                    phaseOrdinal = Integer.valueOf(abortPhaseParam).intValue();
                } catch (NumberFormatException e) {
                }
                if (phaseOrdinal == event.getPhaseId().getOrdinal()) {
                    event.getFacesContext().responseComplete();
                }
            }
        }
    }

    public void beforePhase(PhaseEvent event) {
    }

    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    private void renderScript(PhaseEvent event) {
        URL url = AjaxPhaseListener.class.getResource(SCRIPT_RESOURCE_NAME);
        URLConnection conn = null;
        InputStream stream = null;
        BufferedReader bufReader = null;
        HttpServletResponse response = (HttpServletResponse) event.getFacesContext().getExternalContext().getResponse();
        OutputStreamWriter outWriter = null;
        String curLine = null;
        try {
            outWriter = new OutputStreamWriter(response.getOutputStream(), response.getCharacterEncoding());
            conn = url.openConnection();
            conn.setUseCaches(false);
            stream = conn.getInputStream();
            bufReader = new BufferedReader(new InputStreamReader(stream));
            response.setContentType("text/javascript");
            response.setStatus(200);
            while (null != (curLine = bufReader.readLine())) {
                outWriter.write(curLine + "\n");
            }
            outWriter.flush();
            outWriter.close();
            event.getFacesContext().responseComplete();
        } catch (Exception e) {
            String message = null;
            message = "Can't load script file:" + url.toExternalForm();
        }
    }
}
