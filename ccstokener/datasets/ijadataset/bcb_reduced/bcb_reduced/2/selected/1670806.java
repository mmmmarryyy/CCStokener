package org.icenigrid.gridsam.core.plugin.connector.common;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.GroovyCategorySupport;
import org.icenigrid.gridsam.core.JobState;
import org.icenigrid.gridsam.core.SubmissionException;
import org.icenigrid.gridsam.core.plugin.DRMConnector;
import org.icenigrid.gridsam.core.plugin.JobContext;

/**
 * A DRMConnector that uses a groovy script to perform submission
 */
public class GroovyDRMConnector implements DRMConnector {

    /**
     * serial version ID
     */
    private static final long serialVersionUID = 5174783883192849481L;

    /**
     * script location URI string
     */
    private String oScriptLocation;

    private String oMpiLaunchScript;

    /**
     * logger
     */
    private static Log sLog = LogFactory.getLog(GroovyDRMConnector.class);

    /**
     * properties
     */
    private Properties oProps;

    /**
     * static engine
     */
    private static transient GroovyScriptEngine sEngine;

    /**
     * 
     * @return MPI launch script path
     */
    public String getMpiLaunchScript() {
        return this.oMpiLaunchScript;
    }

    /**
     * 
     * @param pCondorMPILaunchScript 
     */
    public void setMpiLaunchScript(String pMpiLaunchScript) {
        this.oMpiLaunchScript = pMpiLaunchScript;
    }

    /**
     * get the shared GroovyScriptEngine
     * 
     * @return GroovyScriptEngine the engine
     */
    public static final synchronized GroovyScriptEngine getGroovyScriptEngine() {
        if (sEngine == null) {
            sEngine = new GroovyScriptEngine(new ResourceConnector() {

                public URLConnection getResourceConnection(String s) throws ResourceException {
                    try {
                        ClassLoader xClsLoader = GroovyDRMConnector.class.getClassLoader();
                        URL url = xClsLoader.getResource(s);
                        if (url == null) {
                            url = xClsLoader.getResource("/WEB-INF/groovy/" + s);
                            if (url == null) {
                                throw new ResourceException("Resource " + s + " not found");
                            }
                        }
                        return url.openConnection();
                    } catch (IOException ioe) {
                        throw new ResourceException("Problem reading resource " + s + ": " + ioe.getMessage());
                    }
                }
            });
        }
        return sEngine;
    }

    /**
     * Constructor allowing a set of properties to be passed to the groovy
     * script as implicit variables
     * 
     * @param pProps
     *            properties
     */
    public GroovyDRMConnector(List pProps) {
        super();
        Properties xProps = new Properties();
        List xSortedList = new ArrayList(pProps);
        Collections.sort(xSortedList);
        sLog.debug("Properties preference order: " + xSortedList);
        for (int i = 0; i < xSortedList.size(); i++) {
            xProps.putAll((InlineProperties) xSortedList.get(i));
        }
        sLog.debug("Consolidated scheduler properties: " + xProps);
        setProperties(xProps);
    }

    /**
     * Default constructor
     */
    public GroovyDRMConnector() {
        this(new ArrayList());
    }

    /**
     * set script location
     * 
     * @param pScript
     *            the script location URI
     */
    public void setScriptResource(String pScript) {
        oScriptLocation = pScript;
    }

    /**
     * get script location
     * 
     * @return String the script location
     */
    public String getScriptResource() {
        return oScriptLocation;
    }

    /**
     * get properties
     * 
     * @return Properties
     */
    public Properties getProperties() {
        return oProps;
    }

    /**
     * set properties
     * 
     * @param pProps
     *            props
     */
    public void setProperties(Properties pProps) {
        oProps = pProps == null ? new Properties() : pProps;
    }

    /**
     * execute a job through this DRMConnector
     * 
     * @param pContext
     *            the context in which this DRMConnector executes
     */
    public void execute(JobContext pContext) {
        try {
            if (getScriptResource() == null) {
                throw new SubmissionException("Configuration Error: 'scriptLocation' property is not set");
            }
            try {
                final Binding fBinding = new Binding();
                fBinding.setProperty("job", pContext);
                fBinding.setProperty("jsdl", pContext.getJobInstance().getJobDefinition());
                fBinding.setProperty("mpiLaunchScript", this.getMpiLaunchScript());
                for (Iterator xEntries = getProperties().entrySet().iterator(); xEntries.hasNext(); ) {
                    Map.Entry xEntry = (Map.Entry) xEntries.next();
                    fBinding.setProperty(xEntry.getKey().toString(), xEntry.getValue());
                }
                Closure xClosure = new Closure(getGroovyScriptEngine()) {

                    public Object call() {
                        try {
                            return ((GroovyScriptEngine) getDelegate()).run(getScriptResource(), fBinding);
                        } catch (ResourceException e) {
                            throw new RuntimeException(e);
                        } catch (ScriptException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                pContext.getLog().info("executing groovy script " + getScriptResource());
                GroovyCategorySupport.use(XmlObjectCategory.class, xClosure);
                pContext.getLog().info("executed groovy script " + getScriptResource());
            } catch (Exception xEx) {
                throw new SubmissionException(xEx.getMessage(), xEx);
            }
        } catch (Exception xEx) {
            pContext.getLog().debug("script failed: " + xEx.getMessage(), xEx);
            pContext.getLog().warn("script failed: " + xEx.getMessage());
            pContext.getJobInstance().advanceJobState(JobState.FAILED, xEx.getMessage());
        }
    }
}
