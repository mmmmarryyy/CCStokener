package org.eaasyst.eaa.apps;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.util.LabelValueBean;
import org.eaasyst.eaa.Constants;
import org.eaasyst.eaa.syst.EaasyStreet;
import org.eaasyst.eaa.syst.data.transients.Event;
import org.eaasyst.eaa.utils.StringUtils;

/**
 * <p>This abstract class is the base class for all EaasyStreet
 * "dumpUrl" applications. A dumpUrl application is an application
 * that dumps the raw contents of another URL onto the screen.</p>
 *
 * @version 2.0
 * @author Jeff Chilton
 */
public abstract class DumpUrlApplicationBase extends ApplicationBase {

    private static final String METHOD_IN = Constants.METHOD_IN + "DumpUrlApplicationBase(";

    private static final String METHOD_OUT = Constants.METHOD_OUT + "DumpUrlApplicationBase(";

    private static final String OUTPUT_METHOD = ").prepareOutput()";

    private String sourceUrl = null;

    private String startDelimiter = null;

    private String endDelimiter = null;

    private List replacementValues = null;

    /**
	 * Constructs a new DumpUrlApplicationBase.
	 *
	 * @since	Eaasy Street 2.0
	 */
    public DumpUrlApplicationBase() {
        super();
        setViewComponent("open.jsp");
    }

    /**
	 * Called by the <code>Controller</code> to obtain unformatted
	 * application results.
	 *
	 * @param	req	the <code>HttpServletRequest</code> object
	 * @since	Eaasy Street 2.0
	 */
    public void prepareOutput(HttpServletRequest req) {
        EaasyStreet.logTrace(METHOD_IN + className + OUTPUT_METHOD);
        super.prepareOutput(req);
        String content = Constants.EMPTY_STRING;
        String rawContent = null;
        List parts = null;
        try {
            URL url = new URL(sourceUrl);
            BufferedReader input = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            StringBuffer buffer = new StringBuffer();
            while ((line = input.readLine()) != null) {
                buffer.append(line);
                buffer.append(Constants.LF);
            }
            rawContent = buffer.toString();
        } catch (FileNotFoundException nf) {
            req.setAttribute(Constants.RAK_SYSTEM_ACTION, Constants.SYSTEM_ACTION_BACK);
            EaasyStreet.handleSafeEvent(req, new Event(Constants.EAA0012I, new String[] { "URL", nf.getMessage(), nf.toString() }));
        } catch (Exception e) {
            req.setAttribute(Constants.RAK_SYSTEM_ACTION, Constants.SYSTEM_ACTION_BACK);
            EaasyStreet.handleSafeEvent(req, new Event(Constants.EAA0012I, new String[] { "URL", e.getMessage(), e.toString() }));
        }
        if (rawContent != null) {
            if (startDelimiter != null) {
                parts = StringUtils.split(rawContent, startDelimiter);
                if (parts != null && parts.size() > 1) {
                    rawContent = (String) parts.get(1);
                    if (parts.size() > 2) {
                        for (int x = 2; x < parts.size(); x++) {
                            rawContent += startDelimiter;
                            rawContent += parts.get(x);
                        }
                    }
                } else {
                    rawContent = null;
                }
            }
        }
        if (rawContent != null) {
            if (endDelimiter != null) {
                parts = StringUtils.split(rawContent, endDelimiter);
                if (parts != null && parts.size() > 0) {
                    rawContent = (String) parts.get(0);
                } else {
                    rawContent = null;
                }
            }
        }
        if (rawContent != null) {
            if (replacementValues != null && !replacementValues.isEmpty()) {
                for (int x = 0; x < replacementValues.size(); x++) {
                    LabelValueBean bean = (LabelValueBean) replacementValues.get(x);
                    rawContent = StringUtils.replace(rawContent, bean.getLabel(), bean.getValue());
                }
            }
        }
        if (rawContent != null) {
            content = rawContent;
        }
        req.setAttribute(getFormName(), content);
        EaasyStreet.logTrace(METHOD_OUT + className + OUTPUT_METHOD);
    }

    /**
	 * Returns the endDelimiter.
	 * @return String
	 */
    public String getEndDelimiter() {
        return endDelimiter;
    }

    /**
	 * Returns the replacementValues.
	 * @return List
	 */
    public List getReplacementValues() {
        return replacementValues;
    }

    /**
	 * Returns the sourceUrl.
	 * @return String
	 */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
	 * Returns the startDelimiter.
	 * @return String
	 */
    public String getStartDelimiter() {
        return startDelimiter;
    }

    /**
	 * Sets the endDelimiter.
	 * @param endDelimiter The endDelimiter to set
	 */
    public void setEndDelimiter(String endDelimiter) {
        this.endDelimiter = endDelimiter;
    }

    /**
	 * Sets the replacementValues.
	 * @param replacementValues The replacementValues to set
	 */
    public void setReplacementValues(List replacementValues) {
        this.replacementValues = replacementValues;
    }

    /**
	 * Sets the sourceUrl.
	 * @param sourceUrl The sourceUrl to set
	 */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    /**
	 * Sets the startDelimiter.
	 * @param startDelimiter The startDelimiter to set
	 */
    public void setStartDelimiter(String startDelimiter) {
        this.startDelimiter = startDelimiter;
    }
}
