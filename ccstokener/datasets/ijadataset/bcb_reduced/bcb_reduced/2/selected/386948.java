package org.jives.jivescript;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import lombok.Delegate;
import lombok.Getter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jives.actions.PortalAction;
import org.jives.core.Jives;
import org.jives.core.JivesActiveNode;
import org.jives.core.JivesScene;
import org.jives.events.JivesEvent;
import org.jives.exceptions.JiveScriptException;
import org.jives.implementors.NetworkImplementorIntf;
import org.jives.utils.Dependencies;
import org.jives.utils.Log;
import com.sun.script.javascript.RhinoScriptEngine;

/**
 * Decorator for ECMAscript engine that implements the JiveScript features
 * 
 * @author adriano
 */
public class JiveScriptEngine extends JiveScriptEntityFactory implements ScriptEngine {

    /**
	 * The underlying ECMAscript engine needed to run jivescript
	 */
    @Delegate
    private RhinoScriptEngine engine;

    /**
	 * A list of dependencies
	 */
    protected ArrayList<String> imports;

    /**
	 * Flag set every time a new import is performed so that the evaluator
	 * context can be aligned
	 */
    private boolean scriptContextChanged;

    /**
	 * Human readable script name
	 */
    @Getter
    protected static String FILENAME;

    /**
	 * Script checksum
	 */
    @Getter
    protected static String MD5;

    /**
	 * The allowed script version
	 */
    @Getter
    protected static String JIVESCRIPT_VERSION = "0.2";

    /**
	 * The JiveScript
	 */
    protected String script = "";

    /**
	 * A flag to signal that a JiveScript is being loaded
	 */
    @Getter
    private boolean loading;

    /**
	 * Default constructor
	 * 
	 * @throws ScriptException
	 *             if resolution of the ECMAscript engine dependency fails
	 */
    public JiveScriptEngine() throws ScriptException {
        ScriptEngineManager mgr = new ScriptEngineManager();
        engine = (RhinoScriptEngine) mgr.getEngineByName("ECMAScript");
        if (engine == null) {
            throw new ScriptException("ECMAscript is not available to this JVM.");
        }
        imports = new ArrayList<String>();
        allowScripting = true;
        FILENAME = "script-" + Jives.generateId();
        MD5 = "";
        initEvaluator(true);
    }

    /**
	 * Executed automatically prior to eval(), setup the evaluation whenever a
	 * change in the script engine context has occurred
	 * 
	 * @param changed
	 *            A flag to indicate whether the context is changed or not
	 * @throws JiveScriptException
	 *             if something goes wrong in the initialization
	 */
    protected void initEvaluator(boolean changed) throws JiveScriptException {
        if (changed) {
            try {
                String[] defaultImports = { "org.jives.core", "org.jives.actions", "org.jives.dialogues", "org.jives.events", "org.jives.exceptions" };
                if (imports.size() > 0) {
                    defaultImports = (String[]) ArrayUtils.addAll(imports.toArray(new String[imports.size()]), defaultImports);
                }
                engine.eval("var evaluator = JavaImporter(" + StringUtils.join(defaultImports, ",") + ");");
                int scope = ScriptContext.ENGINE_SCOPE;
                Bindings bindings = engine.getBindings(scope);
                Method[] methods = getClass().getMethods();
                bindings.put("engine", this);
                for (Method method : methods) {
                    if (method.isAnnotationPresent(JiveScriptCommand.class)) {
                        Object fn = engine.eval("	  (function() {" + "      var context = engine;" + "      var method = engine['" + method.getName() + "'];" + "      return function() {" + "         return method.apply(context, arguments);" + "      };" + "    })();");
                        bindings.put(method.getName(), fn);
                    }
                }
                put("Jives", new Jives());
                bindings.remove("engine");
                bindings.remove("fn");
                bindings.remove("context");
            } catch (ScriptException e) {
                throw new JiveScriptException(Log.LOG_ERROR, e);
            }
            scriptContextChanged = false;
        }
    }

    /**
	 * Check if a script complies to JiveScript rules, called before evaluating
	 * it
	 * 
	 * @param script
	 *            The script to validate
	 * @throws JiveScriptException
	 *             if the script is not valid
	 */
    protected void parse(String script) throws JiveScriptException {
        if (script.indexOf("importPackage(") >= 0 || script.indexOf("importClass(") >= 0) {
            throw new JiveScriptException(Log.LOG_ERROR, "Usage of imports is forbidden in JiveScript. Call __uses instead");
        }
    }

    /**
	 * (Command) Directive: set the flag to allow/disallow scripting now on As
	 * "scripting" we considered any of the following operations:
	 * <ul>
	 * <li>Instance or make() any Jives objects</li>
	 * <li>Execute multi-line commands</li>
	 * <li>Execute variable assignments</li>
	 * </ul>
	 * 
	 * @param allowed
	 *            True to allow scripting, false otherwise. Once set to false,
	 *            only reset() can restore it to true.
	 * 
	 * @throws JiveScriptException
	 *             If scripting is disallowed
	 */
    @JiveScriptCommand(help = "Set the flag to allow scripting or not")
    public void __scripting(boolean allowed) throws JiveScriptException {
        internalCheckScripting();
        allowScripting = allowed;
        if (!allowScripting) {
            echo("Scripting is no longer allowed");
        }
    }

    /**
	 * (Command) Directive: set the script user-readable name
	 * @throws JiveScriptException If scripting is disallowed
	 */
    @JiveScriptCommand(help = "Set the script user-readable name")
    public void __name(String scriptName) throws JiveScriptException {
        internalCheckScripting();
        JiveScriptEngine.FILENAME = scriptName.replaceAll(" ", "");
        name();
    }

    @JiveScriptCommand(help = "Declare and import a dependency")
    public void __uses(Object dependency) throws JiveScriptException {
        internalCheckScripting();
        try {
            Pattern pattern = Pattern.compile("(\\w+(\\.\\w+)+)");
            Matcher matcher = pattern.matcher(dependency.toString());
            if (!matcher.find()) {
                throw new JiveScriptException(Log.LOG_ERROR, "Unresolved path " + dependency.toString() + ". Please " + "specify a fully qualified path which is not the default package");
            }
            String dependencyName = matcher.group(0);
            String[] available;
            try {
                available = new String[] { Class.forName(dependencyName).getName() };
            } catch (ClassNotFoundException e) {
                available = Dependencies.getClassesInPackage(dependencyName, false);
            }
            for (String element : available) {
                Log.debug(JiveScriptEngine.class, "Uses " + element);
                imports.add(element);
            }
            if (available.length == 0) {
                throw new JiveScriptException(Log.LOG_FATAL, "Unresolved dependency " + dependencyName);
            }
            scriptContextChanged = true;
        } catch (Exception e) {
            echo(e);
        }
    }

    @JiveScriptCommand(help = "Print the textual representation of an object")
    public void echo(Object o) {
        echo(o, false);
    }

    @JiveScriptCommand(help = "Print the textual representation of an object, with(out) line break")
    public void echo(Object o, boolean keepLine) {
        try {
            Writer w = engine.getContext().getWriter();
            if (o == null) {
                o = "null";
            }
            w.write(o.toString());
            if (!keepLine) {
                w.write(Log.LINE_SEPARATOR);
            }
            w.flush();
        } catch (IOException e) {
            Log.error(JiveScriptEngine.class, "Cannot echo to output writer");
        }
    }

    @JiveScriptCommand(help = "Print the list of objects in the registry")
    public void entities() {
        Set<Entry<String, Object>> registry = Jives.listRegistry();
        echo("Content of Jives registry:");
        if (registry.size() == 0) {
            echo("  No object found.");
        } else {
            Formatter f;
            for (Entry<String, Object> entry : registry) {
                Class<?> clazz = entry.getValue().getClass();
                String description = clazz.getName();
                if (clazz.isAnonymousClass()) {
                    if (entry.getValue() instanceof JivesEvent) {
                        description = JivesEvent.class.getSimpleName();
                    }
                } else if (description.indexOf("org.jives") < 0) {
                    Type[] interfaces = clazz.getGenericInterfaces();
                    if (interfaces.length > 0 && interfaces[0].toString().indexOf("org.jives") >= 0) {
                        description = interfaces[0].toString().replace("interface ", "");
                    }
                }
                if (description.contains(".")) {
                    description = description.substring(description.lastIndexOf(".") + 1);
                }
                f = new Formatter();
                f = f.format("  %-24s %-24s |", entry.getKey(), description);
                echo(f.toString(), true);
                entitiesRenderHook(entry.getValue());
            }
        }
    }

    /**
	 * Used by a potential inheritor to control the way entities()
	 * renders the object. Default is showing their toString() 
	 * representation
	 * 
	 * @param entity The entity to render
	 */
    protected void entitiesRenderHook(Object entity) {
        echo(entity.toString());
    }

    @Override
    public Object eval(Reader reader) throws JiveScriptException {
        return eval(reader, engine.getContext());
    }

    @Override
    public Object eval(Reader reader, Bindings n) throws JiveScriptException {
        try {
            initEvaluator(scriptContextChanged);
            String line = "";
            BufferedReader in = new BufferedReader(reader);
            do {
                script += line;
                line = in.readLine();
            } while (line != "");
            return eval(script, n);
        } catch (Exception e) {
            throw new JiveScriptException(Log.LOG_ERROR, e);
        }
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws JiveScriptException {
        try {
            initEvaluator(scriptContextChanged);
            String script = "";
            String line = "";
            BufferedReader in = new BufferedReader(reader);
            do {
                script += line;
                line = in.readLine();
            } while (line != "");
            return eval(script, context);
        } catch (Exception e) {
            throw new JiveScriptException(Log.LOG_ERROR, e);
        }
    }

    @Override
    @JiveScriptCommand(help = "Evaluate a script")
    public Object eval(String script) throws JiveScriptException {
        try {
            return secureEval(engine, script);
        } catch (ScriptException e) {
            throw new JiveScriptException(Log.LOG_ERROR, e);
        }
    }

    @Override
    public Object eval(String script, Bindings n) throws JiveScriptException {
        try {
            initEvaluator(scriptContextChanged);
            parse(script);
            script = "with (evaluator) { " + script + "}";
            return engine.eval(script, n);
        } catch (ScriptException e) {
            throw new JiveScriptException(Log.LOG_ERROR, e);
        }
    }

    @Override
    public Object eval(String script, ScriptContext context) throws JiveScriptException {
        try {
            initEvaluator(scriptContextChanged);
            parse(script);
            script = "with (evaluator) { " + script + "}";
            return engine.eval(script, context);
        } catch (ScriptException e) {
            throw new JiveScriptException(Log.LOG_ERROR, e);
        }
    }

    /**
	 * Set JiveScriptEngine to run on top of another ECMAscript engine
	 * 
	 * @param engine
	 * @throws ScriptException
	 *             if suggested engine is not an ECMAscript engine
	 */
    public void setInternalEngine(ScriptEngine engine) throws ScriptException {
        if (engine instanceof RhinoScriptEngine) {
            this.engine = (RhinoScriptEngine) engine;
            initEvaluator(true);
        } else {
            throw new ScriptException("Suggested engine is not an ECMAscript engine");
        }
    }

    /**
	 * @return The underlying ECMAscript engine needed to run JiveScript
	 */
    public RhinoScriptEngine getInternalEngine() {
        return engine;
    }

    /**
	 * (Command) Display the list of available commands
	 */
    @JiveScriptCommand(help = "Display this help")
    public void help() {
        Method[] methods = JiveScriptEngine.class.getMethods();
        Formatter f = new Formatter();
        for (Method method : methods) {
            if (method.isAnnotationPresent(JiveScriptCommand.class)) {
                JiveScriptCommand ann = method.getAnnotation(JiveScriptCommand.class);
                f = f.format("  %-24s %-40s %n", method.getName() + "()", ann.help());
            }
        }
        echo(f.toString());
    }

    /**
	 * (Command) Display the script name
	 */
    @JiveScriptCommand(help = "Display the script name")
    public void name() {
        echo("This script name is '" + JiveScriptEngine.FILENAME + "'");
    }

    /**
	 * Load an external JiveScript file
	 * 
	 * @param path
	 *            The path to the local resource
	 * @throws IOException
	 *            If any error occurs reading the file
	 * @throws JiveScriptException
	 *            If any error is encountered evaluating the file
	 */
    @JiveScriptCommand(help = "Load and executes a JiveScript file")
    public void load(String path) throws JiveScriptException, IOException {
        loading = true;
        JivesScene.setActiveScene(null);
        boolean allowScripting = JiveScriptEngine.allowScripting;
        JiveScriptEngine.allowScripting = true;
        JiveScriptEngine.FILENAME = null;
        URL url = new URL(path);
        InputStream fis = url.openStream();
        if (fis == null) {
            throw new IOException("Unable to open file at path " + path);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        script = "";
        String line;
        while ((line = br.readLine()) != null) {
            script = script.concat(line) + Log.LINE_SEPARATOR;
        }
        eval(script);
        if (JiveScriptEngine.FILENAME == null) {
            String filename = path.substring(path.lastIndexOf(File.separatorChar) + 1);
            JiveScriptEngine.FILENAME = filename;
        }
        JiveScriptEngine.MD5 = DigestUtils.md5Hex(script.toString().getBytes());
        NetworkImplementorIntf networkImplementor = Jives.getNetwork();
        Object[] config = (Object[]) networkImplementor.getConnectionState(NetworkImplementorIntf.CONNECTIONSTATE_ALL);
        boolean internet = (Boolean) config[NetworkImplementorIntf.CONNECTIONSTATE_INTERNET];
        if (internet) {
            echo("Starting network on the internet");
        } else {
            boolean ipv6 = (Boolean) config[NetworkImplementorIntf.CONNECTIONSTATE_IPV6];
            String connection = (String) config[NetworkImplementorIntf.CONNECTIONSTATE_RENDEZVOUS_IPV4] + ":" + (Integer) config[NetworkImplementorIntf.CONNECTIONSTATE_RENDEZVOUS_IPV4_PORT];
            if (ipv6) {
                connection = (String) networkImplementor.getConnectionState(NetworkImplementorIntf.CONNECTIONSTATE_RENDEZVOUS_IPV6) + ":" + (Integer) networkImplementor.getConnectionState(NetworkImplementorIntf.CONNECTIONSTATE_RENDEZVOUS_IPV6_PORT);
            }
            echo("Starting network on " + connection);
        }
        networkImplementor.startNetwork(JiveScriptEngine.FILENAME, JiveScriptEngine.MD5);
        fis.close();
        if (JiveScriptEngine.allowScripting && !allowScripting) {
            JiveScriptEngine.allowScripting = allowScripting;
        }
        loading = false;
    }

    /**
	 * (Command) Prints the memory statistics to standard buffer
	 */
    @JiveScriptCommand(help = "Print available and allocated memory")
    public void memstats() {
        long maxMemory = Runtime.getRuntime().maxMemory() >> 20;
        long freeMemory = Runtime.getRuntime().freeMemory() >> 20;
        echo(maxMemory + " MB memory\t" + freeMemory + " MB free");
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        engine.setBindings(bindings, scope);
        scriptContextChanged = true;
    }

    @Override
    public void setContext(ScriptContext context) {
        engine.setContext(context);
        scriptContextChanged = true;
    }

    @JiveScriptCommand(help = "Display JiveScript version")
    public void version() {
        echo(JiveScriptEngine.JIVESCRIPT_VERSION);
    }

    /**
	 * (Command) Convenience method to return an array of the objects passed as 
	 * parameters from JiveScript
	 * 
	 * @param params (vararg) The list of Java objects to implode into an array
	 */
    @JiveScriptCommand(help = "Return an array of the objects passed as parameters")
    public Object implode(Object... params) {
        ArrayList<Object> list = new ArrayList<Object>();
        for (Object param : params) {
            list.add(param);
        }
        return list.toArray(new Object[params.length]);
    }

    /**
	 * Revert the system to the initial state, cleaning up memory
	 * @throws JiveScriptException
	 */
    @JiveScriptCommand(help = "Revert the system to the initial state, cleaning up memory")
    public void reset() throws JiveScriptException {
        FILENAME = "script-" + Jives.generateId();
        allowScripting = true;
        JivesScene currentScene = JivesScene.getActiveScene();
        PortalAction.sendExitMessage(currentScene.getId());
        if (currentScene != null) {
            currentScene.destroy();
        }
        getBindings(ScriptContext.ENGINE_SCOPE).clear();
        Jives.flushRegisters();
        try {
            if (Jives.getNetwork() != null) {
                Log.info(this, "Stopping JXTA");
                Jives.getNetwork().stopNetwork();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        ;
        JivesScene.setActiveScene(null);
        Jives.put(JivesScene.getMyself());
        Jives.put(JivesScene.getActiveScene());
        initEvaluator(true);
        Jives.getEngine().reset();
        if (!isLoading()) {
            __name(FILENAME);
        }
    }

    /**
	 * @return The representation of the user in the scene
	 */
    @JiveScriptCommand(help = "Reference to your alter ego")
    public JivesActiveNode me() {
        return JivesScene.getMyself();
    }

    /**
	 * Save a snapshot of the state of the currently executed script 
	 * using the specified name
	 * @param name The name of the script to save
	 * @throws IOException If there was an error writing the state to disk
	 */
    @JiveScriptCommand(help = "Save state snapshot")
    public void saveState(String name) throws IOException {
        Jives.saveSnapshot(name);
    }

    /**
	 * Load a snapshot of the state of the currently executed script 
	 * using the specified name
	 * @param name The name of the script to load
	 * @throws IOException If there was an error reading the state from disk
	 * @throws JiveScriptException If the snapshot script doesn't match the script
	 * currently being executed
	 */
    @JiveScriptCommand(help = "Load state snapshot")
    public void loadState(String name) throws IOException, JiveScriptException {
        Jives.loadSnapshot(name);
    }
}
