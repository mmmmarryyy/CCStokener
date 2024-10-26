package com.martiansoftware.nailgun.components.builtins.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.codehaus.groovy.control.CompilerConfiguration;

/**
 * <p>Title:GroovyScriptManager</p>
 * <p>Description: Manager class for individual Groovy scripts.</p>
 * @author Nicholas Whitehead (nwhitehead at heliosdev dot org)
 *
 */
public class GroovyScriptManager {

    /** The groovy binding to pass context and state between the script and the renderer */
    protected Binding binding = null;

    /** The groovy shell to compile the script */
    protected GroovyShell groovyShell = null;

    /** The groovy script that executes the rendering and returns a byte array */
    protected Script script = null;

    /** The URL for the script source code */
    protected URL sourceUrl = null;

    /** The last time the script was compiled from source */
    protected long lastCompileTime = 0L;

    /** The groovy compilation properties */
    protected Properties groovyProperties = null;

    /** Additional properties to be passed into the script's binding */
    protected Properties scriptProperties = null;

    /** The compiler options */
    protected CompilerConfiguration cc = null;

    /** The prepared groovy source */
    protected GroovyCodeSource groovySourceCode = null;

    /** Additional static properties passed to the script context */
    protected Map args = null;

    /** The console output stream */
    protected PrintStream out = null;

    /**
		 * Creates a new GroovyScriptManager
		 * @param sourceUrl The URL of the groovy source file.
		 * @param groovyProperties The compiler properties.
		 * @param scriptProperties The properties containing arguments to pass to the script.
		 * @param out The console print stream.
		 * @throws IOException 
		 */
    public GroovyScriptManager(URL sourceUrl, Properties groovyProperties, Properties scriptProperties, Map args, PrintStream out) throws IOException {
        this.sourceUrl = sourceUrl;
        this.groovyProperties = groovyProperties;
        this.scriptProperties = scriptProperties;
        this.args = args;
        this.out = out;
        init();
    }

    /**
		 * Initializes the compiler configuration, compiles the script and passes the parameter properties to the binding.
		 * @throws IOException
		 */
    protected void init() throws IOException {
        cc = new CompilerConfiguration();
        binding = new Binding();
        Boolean debug = null;
        Boolean recompile = null;
        String scriptBaseClass = null;
        String encoding = null;
        Integer tolerance = null;
        Boolean verbose = null;
        Integer warning = null;
        try {
            debug = Boolean.valueOf(groovyProperties.get("debug").toString());
            if (debug != null) cc.setDebug(debug.booleanValue());
        } catch (Exception e) {
        }
        try {
            recompile = Boolean.valueOf(groovyProperties.get("recomp").toString());
            if (recompile != null) cc.setRecompileGroovySource(recompile.booleanValue()); else recompile = new Boolean(false);
        } catch (Exception e) {
            recompile = new Boolean(false);
        }
        try {
            scriptBaseClass = groovyProperties.get("scriptbaseclass").toString();
            if (scriptBaseClass != null && (!"".equalsIgnoreCase(scriptBaseClass))) cc.setScriptBaseClass(scriptBaseClass);
        } catch (Exception e) {
        }
        try {
            encoding = groovyProperties.get("encoding").toString();
            if (encoding != null && (!"".equalsIgnoreCase(encoding))) cc.setSourceEncoding(encoding);
        } catch (Exception e) {
        }
        try {
            tolerance = new Integer(Integer.parseInt(groovyProperties.get("tolerance").toString()));
            if (tolerance != null) cc.setTolerance(tolerance.intValue());
        } catch (Exception e) {
        }
        try {
            verbose = Boolean.valueOf(groovyProperties.get("verbose").toString());
            if (verbose != null) cc.setVerbose(verbose.booleanValue());
        } catch (Exception e) {
        }
        try {
            warning = new Integer(Integer.parseInt(groovyProperties.get("warning").toString()));
            if (warning != null) cc.setWarningLevel(warning.intValue());
        } catch (Exception e) {
        }
        long start = System.currentTimeMillis();
        for (Iterator propIterator = scriptProperties.keySet().iterator(); propIterator.hasNext(); ) {
            String key = propIterator.next().toString();
            Object value = scriptProperties.get(key);
            binding.setProperty(key, value);
        }
        for (Iterator argIterator = args.keySet().iterator(); argIterator.hasNext(); ) {
            String key = argIterator.next().toString();
            Object value = args.get(key);
            binding.setProperty(key, value);
        }
        binding.setProperty("binding", binding);
        binding.setProperty("stateMap", new HashMap());
        groovyShell = new GroovyShell(binding, cc);
        groovySourceCode = new GroovyCodeSource(sourceUrl);
        groovySourceCode.setCachable(false);
        script = groovyShell.parse(groovySourceCode);
        lastCompileTime = System.currentTimeMillis();
        long elapsed = lastCompileTime - start;
        out.println("Script Compiled From [" + sourceUrl.toString() + "] In " + elapsed + " ms.");
    }

    /**
		 * Executes the script.
		 * @return The return value from the script.
		 */
    public Object execute() {
        checkForRecompile();
        return script.run();
    }

    /**
		 * Invokes a method in the script.
		 * @param methodName The method name to invoke.
		 * @param methodArguments The arguments to the method.
		 * @return The return value of the invoked method.
		 */
    public Object invokeMethod(String methodName, Object methodArguments) {
        checkForRecompile();
        for (Iterator propIterator = scriptProperties.keySet().iterator(); propIterator.hasNext(); ) {
            String key = propIterator.next().toString();
            Object value = scriptProperties.get(key);
            binding.setProperty(key, value);
        }
        return script.invokeMethod(methodName, methodArguments);
    }

    /**
		 * Checks the URL of the groovy source against the last compile time.
		 * If the source has been modified, it is recompiled.
		 */
    public void checkForRecompile() {
        if (isModified(sourceUrl, lastCompileTime)) {
            try {
                long start = System.currentTimeMillis();
                groovySourceCode = new GroovyCodeSource(sourceUrl);
                groovySourceCode.setCachable(false);
                script = groovyShell.parse(groovySourceCode);
                lastCompileTime = System.currentTimeMillis();
                long elapsed = lastCompileTime - start;
                out.println("Script Compiled From [" + sourceUrl.toString() + "] In " + elapsed + " ms.");
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to recompile script [" + sourceUrl + "]", ioe);
            }
        }
    }

    /**
		 * Tests to see if the source code at the URL has been modified since the last init.
		 * @param testUrl The URL to test the last modified date on.
		 * @param lastModified The known last modified date.
		 * @return true if the URL modified date is greater than the passed known modified date, and false if it is not. 
		 * @throws IOException 
		 */
    public static boolean isModified(URL testUrl, long lastModified) {
        if (lastModified == 0) return false;
        URLConnection urlConn = null;
        try {
            urlConn = testUrl.openConnection();
            long lastMod = urlConn.getLastModified();
            urlConn.getInputStream().close();
            return lastMod > lastModified;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                urlConn.getInputStream().close();
            } catch (Throwable t) {
            }
        }
    }

    /**
		 * @return the sourceUrl
		 */
    public URL getSourceUrl() {
        return sourceUrl;
    }

    /**
		 * @param sourceUrl the sourceUrl to set
		 */
    public void setSourceUrl(URL sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    /**
		 * @return the binding
		 */
    public Binding getBinding() {
        return binding;
    }

    /**
		 * @return the groovyProperties
		 */
    public Properties getGroovyProperties() {
        return groovyProperties;
    }

    /**
		 * @return the groovyShell
		 */
    public GroovyShell getGroovyShell() {
        return groovyShell;
    }

    /**
		 * @return the lastCompileTime
		 */
    public long getLastCompileTime() {
        return lastCompileTime;
    }

    /**
		 * @return the script
		 */
    public Script getScript() {
        return script;
    }

    /**
		 * @return the scriptProperties
		 */
    public Properties getScriptProperties() {
        return scriptProperties;
    }

    /**
		 * @param cc the cc to set
		 */
    public void setCc(CompilerConfiguration cc) {
        this.cc = cc;
    }
}
