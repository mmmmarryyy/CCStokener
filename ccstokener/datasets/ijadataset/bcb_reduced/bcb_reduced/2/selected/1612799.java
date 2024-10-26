package org.apache.tapestry.contrib.script;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.MetaClass;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tapestry.ApplicationRuntimeException;
import org.apache.tapestry.IComponent;
import org.apache.tapestry.ILocation;
import org.apache.tapestry.IResourceLocation;
import org.apache.tapestry.Location;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.ProcessingUnit;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;

/**
 * A script resolver for the Groovy scripting language.
 * 
 * @author Richard Hensley
 * @author Michael Henderson
 */
public class ScriptResolver implements IScriptResolver {

    private static final Log LOG = LogFactory.getLog(ScriptResolver.class);

    static {
        MetaClass.setUseReflection(true);
    }

    private Map deployedScripts;

    private GroovyClassLoader groovyLoader;

    private ClassLoader parent;

    private Map loadedScripts;

    /**
     * Construct the groovy script resolver, with a parent class loader.
     * @param parent
     */
    public ScriptResolver(ClassLoader parent) {
        super();
        this.parent = parent;
        this.groovyLoader = new GroovyClassLoader(this.parent);
        this.loadedScripts = new HashMap();
        this.deployedScripts = new HashMap();
    }

    /**
     * Convert the groovy compilation failed exception into a line
     * precise Tapestry exception.
     * @param ex
     * @param component
     * @param resourceLocation
     * @return
     */
    private ApplicationRuntimeException convertToApplicationRuntimeException(CompilationFailedException ex, IComponent component, IResourceLocation resourceLocation) {
        Object[] info = inspectCompilation(ex.getUnit());
        int line = ((Number) info[0]).intValue();
        int column = ((Number) info[1]).intValue();
        String message = (String) info[2];
        Location location = new Location(resourceLocation, line, column);
        return new ApplicationRuntimeException(message, component, location, ex);
    }

    /**
     * Attempt to find the compiled script in the parent class loader.
     * 
     * @param script
     * @return
     */
    private Class findDeployedScript(IScriptReference script) {
        Class result = script.getDeployedClass();
        if (result != null) {
            return result;
        }
        if (this.deployedScripts.containsKey(script.getScriptClassName()) == true) {
            result = (Class) this.deployedScripts.get(script.getScriptClassName());
            return result;
        }
        try {
            result = this.parent.loadClass(script.getScriptClassName());
            if (LOG.isDebugEnabled()) {
                LOG.debug("using deployed script " + script.getScriptClassName());
            }
        } catch (ClassNotFoundException e) {
            result = null;
        }
        script.setDeployedClass(result);
        this.deployedScripts.put(script.getScriptClassName(), result);
        return result;
    }

    /**
     * @see org.apache.tapestry.contrib.script.IScriptResolver#findScript(java.lang.String, org.apache.tapestry.IComponent, org.apache.tapestry.ILocation)
     */
    public IScriptReference findScript(String name, IComponent component, ILocation componentLocation) {
        IScriptReference result = findScript(name, component, componentLocation, null);
        URL scriptUrl = result.getResourceLocation().getResourceURL();
        if (scriptUrl != null) {
            return result;
        } else {
            String msg = Messages.format("ScriptResolver.script-not-found", result.getResourceLocation().getPath());
            throw new ApplicationRuntimeException(msg, component, componentLocation, null);
        }
    }

    /**
     * @see org.apache.tapestry.contrib.script.IScriptResolver#findScript(java.lang.String, org.apache.tapestry.IComponent, org.apache.tapestry.ILocation, java.lang.Class)
     */
    public IScriptReference findScript(String name, IComponent component, ILocation componentLocation, Class defaultClass) {
        if (name == null) {
            String componentname = component.getSpecification().getSpecificationLocation().getName();
            name = componentname.substring(0, componentname.indexOf('.'));
        }
        IResourceLocation scriptResourceLocation = component.getSpecification().getSpecificationLocation().getRelativeLocation(name + ".groovy");
        return new ScriptReference(scriptResourceLocation, component, componentLocation, this, defaultClass);
    }

    private long getLastModified(URL url) {
        long result = 0;
        if (url != null) {
            try {
                URLConnection conn = url.openConnection();
                conn.connect();
                result = conn.getLastModified();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Inspect the compliation unit for errors.
     * @param unit
     * @return
     */
    private Object[] inspectCompilation(ProcessingUnit unit) {
        Object[] result = inspectUnit(unit);
        if (result == null && unit instanceof CompilationUnit) {
            Iterator iterator = ((CompilationUnit) unit).iterator();
            while (iterator.hasNext()) {
                result = inspectUnit((ProcessingUnit) iterator.next());
                if (result != null) {
                    return result;
                }
            }
        }
        if (result == null) {
            result = new Object[3];
            result[0] = new Integer(0);
            result[1] = new Integer(0);
            result[2] = "";
        }
        return result;
    }

    /**
     * Inspect the messages associated with a unit. Return the first
     * occurrance found.
     * @param unit
     * @param list
     * @return
     */
    private Object[] inspectMessages(ProcessingUnit unit, List list) {
        if (list == null || list.size() == 0) {
            return null;
        }
        Message msg = (Message) list.get(0);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        msg.write(pw, unit);
        StringBuffer sb = sw.getBuffer();
        while (sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        Object[] result = new Object[3];
        result[0] = new Integer(0);
        result[1] = new Integer(0);
        result[2] = sb.toString();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Message " + msg.getClass().getName() + ": " + sb.toString());
        }
        if (msg instanceof SyntaxErrorMessage) {
            SyntaxErrorMessage semsg = (SyntaxErrorMessage) msg;
            result[0] = new Integer(semsg.getCause().getLine());
            result[1] = new Integer(semsg.getCause().getStartColumn());
        }
        return result;
    }

    /**
     * Inspect the error and warnings from a unit, return the
     * first.
     * @param unit
     * @return
     */
    private Object[] inspectUnit(ProcessingUnit unit) {
        Object[] result = inspectMessages(unit, unit.getErrors());
        if (result == null) {
            result = inspectMessages(unit, unit.getWarnings());
        }
        return result;
    }

    /**
     * Resolve the script to a Class.
     * @param script
     * @return
     */
    public synchronized Class resolveScript(IScriptReference script) {
        Class result = findDeployedScript(script);
        if (result != null) {
            return result;
        }
        URL url = script.getResourceLocation().getResourceURL();
        if (url == null) {
            return null;
        }
        String urlString = url.toString();
        Long modifiedInExternalStorage = new Long(getLastModified(url));
        Long lastModified = (Long) loadedScripts.get(urlString);
        if (lastModified != null && lastModified.longValue() < modifiedInExternalStorage.longValue()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating new groovy loader because " + url + " changed");
            }
            this.groovyLoader = new GroovyClassLoader(this.parent);
            this.loadedScripts = new HashMap();
        }
        try {
            InputStream is = url.openStream();
            try {
                BufferedInputStream bis = new BufferedInputStream(is);
                result = this.groovyLoader.parseClass(bis, script.getResourceLocation().getName());
                if (LOG.isDebugEnabled() && this.loadedScripts.containsKey(urlString) == false) {
                    LOG.debug("New script loaded" + script);
                    LOG.debug("name=" + result.getName());
                    LOG.debug("scriptClass=" + script.getScriptClassName());
                }
                if (script.getScriptClassName().equals(result.getName()) == false) {
                    ILocation loc = new Location(script.getResourceLocation(), 0, 0);
                    throw new ApplicationRuntimeException(Messages.format("ScriptResolver.bad-classname", script.getScriptClassName(), result.getName()), script.getComponent(), loc, null);
                }
                loadedScripts.put(urlString, modifiedInExternalStorage);
                return result;
            } finally {
                is.close();
            }
        } catch (ApplicationRuntimeException are) {
            throw are;
        } catch (CompilationFailedException ex) {
            throw convertToApplicationRuntimeException(ex, script.getComponent(), script.getResourceLocation());
        } catch (GroovyRuntimeException ex) {
            ASTNode node = ex.getNode();
            Location location = new Location(script.getResourceLocation(), node.getLineNumber(), node.getColumnNumber());
            throw new ApplicationRuntimeException(ex.getMessage(), script.getComponent(), location, ex);
        } catch (Throwable t) {
            throw new ApplicationRuntimeException(Messages.format("ScriptResolver.script-not-loaded", script.getResourceLocation().getName(), t.getMessage()), t);
        }
    }
}
