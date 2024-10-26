package org.twdata.kokua.script.flow.javascript;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.*;
import java.util.*;
import java.net.URL;
import org.twdata.kokua.script.flow.Interpreter;
import org.twdata.kokua.ResourceManager;
import org.springframework.beans.factory.*;
import org.twdata.kokua.script.flow.*;
import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.ToolErrorReporter;
import org.apache.log4j.Logger;

/**
 * Interface with the JavaScript interpreter.
 *
 * @author <a href="mailto:ovidiu@apache.org">Ovidiu Predescu</a>
 * @author <a href="mailto:crafterm@apache.org">Marcus Crafter</a>
 * @since March 25, 2002
 * @version CVS $Id: JavaScriptInterpreter.java,v 1.11 2004/12/02 06:18:22 mrdon Exp $
 */
public class JavaScriptInterpreter implements BeanFactoryAware {

    /**
     * LAST_EXEC_TIME
     * A long value is stored under this key in each top level JavaScript
     * thread scope object. When you enter a context any scripts whose
     * modification time is later than this value will be recompiled and reexecuted,
     * and this value will be updated to the current time.
     */
    private static final String LAST_EXEC_TIME = "__PRIVATE_LAST_EXEC_TIME__";

    static int OPTIMIZATION_LEVEL = -2;

    private static final Logger log = Logger.getLogger(JavaScriptInterpreter.class);

    /**
     * Shared global scope for scripts and other immutable objects
     */
    JSGlobal scope;

    private BeanFactory factory;

    private ResourceManager resourceMgr;

    private ContinuationsManager continuationMgr;

    private String globalScript;

    private static final String GLOBAL_SCRIPT = "__global__";

    private Map scripts = Collections.synchronizedMap(new HashMap());

    private Map activeScripts = Collections.synchronizedMap(new HashMap());

    JSErrorReporter errorReporter;

    boolean enableDebugger = false;

    /**
     * JavaScript debugger: there's only one of these: it can debug multiple
     * threads executing JS code.
     */
    static org.mozilla.javascript.tools.debugger.Main debugger;

    static synchronized org.mozilla.javascript.tools.debugger.Main getDebugger() {
        if (debugger == null) {
            final org.mozilla.javascript.tools.debugger.Main db = new org.mozilla.javascript.tools.debugger.Main("Kokua Flow Debugger");
            db.pack();
            java.awt.Dimension size = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            size.width *= 0.75;
            size.height *= 0.75;
            db.setSize(size);
            db.setExitAction(new Runnable() {

                public void run() {
                    db.setVisible(false);
                }
            });
            db.setOptimizationLevel(OPTIMIZATION_LEVEL);
            db.setVisible(true);
            debugger = db;
            Context.addContextListener(debugger);
        }
        return debugger;
    }

    public void setErrorReporter(JSErrorReporter er) {
        this.errorReporter = er;
    }

    public void setEnableDebugger(boolean debug) {
        enableDebugger = debug;
        if (enableDebugger) {
            if (log.isDebugEnabled()) {
                log.debug("Flow debugger enabled, creating");
            }
            getDebugger().doBreak();
        }
    }

    public boolean getEnableDebugger() {
        return enableDebugger;
    }

    public void setBeanFactory(BeanFactory factory) {
        this.factory = factory;
    }

    public void setContinuationsManager(ContinuationsManager cm) {
        continuationMgr = cm;
    }

    public void setResourceManager(ResourceManager rm) {
        this.resourceMgr = rm;
    }

    public void setGlobalScript(String globalScript) {
        this.globalScript = globalScript;
    }

    public void init() {
        Context context = Context.enter();
        context.setOptimizationLevel(OPTIMIZATION_LEVEL);
        context.setCompileFunctionsWithDynamicScope(true);
        context.setGeneratingDebug(true);
        try {
            scope = new JSGlobal(context);
            ScriptableObject.defineClass(scope, JSLog.class);
            ScriptableObject.defineClass(scope, JSKokua.class);
            ScriptableObject.defineClass(scope, JSDialog.class);
            ScriptableObject.defineClass(scope, JSContinuation.class);
            String[] names = { "print", "sleep" };
            try {
                scope.defineFunctionProperties(names, JSGlobal.class, ScriptableObject.DONTENUM);
            } catch (PropertyException e) {
                throw new Error(e.getMessage());
            }
            Object args[] = {};
            Scriptable slog = context.toObject(log, scope);
            scope.put("log", scope, slog);
            errorReporter.setLogger(log);
        } catch (Exception e) {
            log.error(e, e);
        } finally {
            Context.exit();
        }
    }

    /**
     * Returns a new Scriptable object to be used as the global scope
     * when running the JavaScript scripts in the context of a request.
     *
     * <p>If you want to maintain the state of global variables across
     * multiple invocations of <code>&lt;map:call
     * function="..."&gt;</code>, you need to invoke from the JavaScript
     * script <code>kokua.createSession()</code>. This will place the
     * newly create Scriptable object in the user's session, where it
     * will be retrieved from at the next invocation of {@link #callFunction}.</p>
     *
     * @param environment an <code>Environment</code> value
     * @return a <code>Scriptable</code> value
     * @exception Exception if an error occurs
     */
    public Scriptable enterContext(Scriptable thrScope) throws Exception {
        Context context = Context.enter();
        context.setOptimizationLevel(OPTIMIZATION_LEVEL);
        context.setGeneratingDebug(true);
        context.setCompileFunctionsWithDynamicScope(true);
        context.setErrorReporter(errorReporter);
        JSKokua kokua;
        boolean newScope = false;
        long lastExecTime = 0;
        if (thrScope == null) {
            newScope = true;
            thrScope = context.newObject(scope);
            thrScope.setPrototype(scope);
            thrScope.setParentScope(null);
            Object args[] = {};
            kokua = (JSKokua) context.newObject(thrScope, "Kokua", args);
            kokua.setInterpreter(this);
            kokua.setParentScope(thrScope);
            kokua.setBeanFactory(factory);
            thrScope.put("kokua", thrScope, kokua);
            ((ScriptableObject) thrScope).defineProperty(LAST_EXEC_TIME, new Long(0), ScriptableObject.DONTENUM | ScriptableObject.PERMANENT);
            Script script = (Script) scripts.get(GLOBAL_SCRIPT);
            if (script == null) {
                synchronized (scripts) {
                    Reader reader = new InputStreamReader(resourceMgr.getResource(globalScript));
                    script = compileScript(context, thrScope, reader, GLOBAL_SCRIPT);
                    scripts.put(GLOBAL_SCRIPT, script);
                }
            }
            exec(script, context, thrScope);
        } else {
        }
        return thrScope;
    }

    /**
     * Remove the Kokua object from the JavaScript thread scope so it
     * can be garbage collected, together with all the objects it
     * contains.
     */
    public void exitContext(Scriptable thrScope) {
        Context.exit();
    }

    public Script compileScript(String id, String code) throws Exception {
        Scriptable sc = enterContext(null);
        Context context = Context.getCurrentContext();
        Reader reader = new BufferedReader(new StringReader(code));
        Script compiledScript = context.compileReader(sc, reader, id, 1, null);
        return compiledScript;
    }

    protected Script compileScript(Context cx, Scriptable scope, Reader src, String id) throws Exception {
        try {
            Reader reader = new BufferedReader(src);
            Script compiledScript = cx.compileReader(scope, reader, id, 1, null);
            return compiledScript;
        } finally {
            src.close();
        }
    }

    public void exec(String id, boolean ownThread) throws Exception {
        exec(id, null, ownThread);
    }

    public void exec(URL url, boolean ownThread) throws Exception {
        String id = url.toString();
        Reader reader = new InputStreamReader(url.openStream());
        synchronized (scripts) {
            Scriptable sc = enterContext(null);
            Context context = Context.getCurrentContext();
            Script compiledScript = compileScript(context, sc, reader, id);
            scripts.put(id, compiledScript);
        }
        exec(id, ownThread);
    }

    public void exec(String id, Scriptable scope, boolean ownThread) throws Exception {
        Script script = (Script) scripts.get(id);
        exec(script, scope, ownThread);
    }

    /**
     * Calls a JavaScript function, passing <code>params</code> as its
     * arguments. In addition to this, it makes available the parameters
     * through the <code>kokua.parameters</code> JavaScript array
     * (indexed by the parameter names).
     *
     * @param funName a <code>String</code> value
     * @param params a <code>List</code> value
     * @param environment an <code>Environment</code> value
     * @exception Exception if an error occurs
     */
    public void exec(final Script script, final Scriptable scope, boolean ownThread) throws Exception {
        if (ownThread) {
            Thread t = new Thread() {

                public void run() {
                    Scriptable thrScope = null;
                    try {
                        thrScope = enterContext(scope);
                        Scriptable kokua = (JSKokua) thrScope.get("kokua", thrScope);
                        final Context context = Context.getCurrentContext();
                        if (enableDebugger) {
                            if (!getDebugger().isVisible()) {
                                getDebugger().setVisible(true);
                            }
                        }
                        exec(script, context, thrScope);
                    } catch (Exception ex) {
                        log.error(ex, ex);
                    } finally {
                        activeScripts.remove(script);
                        Context.exit();
                    }
                }
            };
            t.start();
            activeScripts.put(script, t);
        } else {
            Scriptable thrScope = null;
            try {
                thrScope = enterContext(scope);
                final Context context = Context.getCurrentContext();
                if (enableDebugger) {
                    if (!getDebugger().isVisible()) {
                        getDebugger().setVisible(true);
                    }
                }
                exec(script, context, thrScope);
            } finally {
                exitContext(thrScope);
            }
        }
    }

    public void stop(URL url) {
        stop(url.toString());
    }

    public void stopAll() {
        synchronized (activeScripts) {
            for (Iterator i = activeScripts.values().iterator(); i.hasNext(); ) {
                ((JSKokua) i.next()).stopRequested();
            }
        }
    }

    public void stop(String id) {
        Script script = (Script) scripts.get(id);
        Thread t = (Thread) activeScripts.get(script);
        log.warn("script:" + script + " thread:" + t);
        if (t != null) {
            log.debug("Interrupting script");
            t.interrupt();
        }
    }

    public Object eval(String name, String code, Scriptable scope) throws Exception {
        Scriptable thrScope = null;
        Object result = null;
        try {
            thrScope = enterContext(scope);
            final Context context = Context.getCurrentContext();
            if (enableDebugger) {
                if (!getDebugger().isVisible()) {
                    getDebugger().setVisible(true);
                }
            }
            try {
                result = context.evaluateString(thrScope, code, name, 1, null);
            } catch (JavaScriptException ex) {
                EvaluatorException ee = Context.reportRuntimeError(ToolErrorReporter.getMessage("msg.uncaughtJSException", ex.getMessage()));
                Throwable unwrapped = unwrap(ex);
                throw new RuntimeException(ee.getMessage(), unwrapped);
            } catch (EcmaError ee) {
                String msg = ToolErrorReporter.getMessage("msg.uncaughtJSException", ee.toString());
                if (ee.getSourceName() != null) {
                    Context.reportRuntimeError(msg, ee.getSourceName(), ee.getLineNumber(), ee.getLineSource(), ee.getColumnNumber());
                } else {
                    Context.reportRuntimeError(msg);
                }
                throw new RuntimeException(ee.getMessage(), ee);
            }
        } finally {
            exitContext(thrScope);
        }
        return result;
    }

    public void exec(Script script, Context ctx, Scriptable thrScope) {
        try {
            script.exec(ctx, thrScope);
        } catch (JavaScriptException ex) {
            EvaluatorException ee = Context.reportRuntimeError(ToolErrorReporter.getMessage("msg.uncaughtJSException", ex.getMessage()));
            Throwable unwrapped = unwrap(ex);
            throw new RuntimeException(ee.getMessage(), unwrapped);
        } catch (EcmaError ee) {
            String msg = ToolErrorReporter.getMessage("msg.uncaughtJSException", ee.toString());
            if (ee.getSourceName() != null) {
                Context.reportRuntimeError(msg, ee.getSourceName(), ee.getLineNumber(), ee.getLineSource(), ee.getColumnNumber());
            } else {
                Context.reportRuntimeError(msg);
            }
            throw new RuntimeException(ee.getMessage(), ee);
        }
    }

    public void handleContinuation(String id, List params) throws Exception {
        Continuation wk = continuationMgr.lookupContinuation(id);
        if (wk == null) {
            throw new InvalidContinuationException("The continuation ID " + id + " is invalid.");
        }
        Context context = Context.enter();
        context.setOptimizationLevel(OPTIMIZATION_LEVEL);
        context.setGeneratingDebug(true);
        context.setCompileFunctionsWithDynamicScope(true);
        JSContinuation jswk = (JSContinuation) wk.getUserObject();
        JSKokua kokua = jswk.getJSKokua();
        final Scriptable kScope = kokua.getParentScope();
        if (enableDebugger) {
            getDebugger().setVisible(true);
        }
        Object handleContFunction = kScope.get("handleContinuation", kScope);
        if (handleContFunction == Scriptable.NOT_FOUND) {
            throw new RuntimeException("Cannot find 'handleContinuation' " + "(system.js not loaded?)");
        }
        Object args[] = { jswk };
        int size = (params != null ? params.size() : 0);
        NativeArray parameters = new NativeArray(size);
        if (size != 0) {
            for (int i = 0; i < size; i++) {
                Interpreter.Argument arg = (Interpreter.Argument) params.get(i);
                parameters.put(arg.name, parameters, arg.value);
            }
        }
        try {
            ((Function) handleContFunction).call(context, kScope, kScope, args);
        } catch (JavaScriptException ex) {
            EvaluatorException ee = Context.reportRuntimeError(ToolErrorReporter.getMessage("msg.uncaughtJSException", ex.getMessage()));
            Throwable unwrapped = unwrap(ex);
            throw new RuntimeException(ee.getMessage(), unwrapped);
        } catch (EcmaError ee) {
            String msg = ToolErrorReporter.getMessage("msg.uncaughtJSException", ee.toString());
            if (ee.getSourceName() != null) {
                Context.reportRuntimeError(msg, ee.getSourceName(), ee.getLineNumber(), ee.getLineSource(), ee.getColumnNumber());
            } else {
                Context.reportRuntimeError(msg);
            }
            throw new RuntimeException(ee.getMessage(), ee);
        } finally {
            Context.exit();
        }
    }

    private Throwable unwrap(JavaScriptException e) {
        Object value = e.getValue();
        while (value instanceof Wrapper) {
            value = ((Wrapper) value).unwrap();
        }
        if (value instanceof Throwable) {
            return (Throwable) value;
        }
        return e;
    }
}
