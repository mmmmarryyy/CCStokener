package com.iv.flash.js;

import com.iv.flash.util.Log;
import com.iv.flash.util.Resource;
import org.mozilla.javascript.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Norris Boyd
 */
public class JSHelper {

    private static Context init(String args[], PrintStream out, com.iv.flash.context.Context genContext) {
        Context cx = Context.enter();
        global = getGlobal(genContext);
        global.setOut(out);
        errorReporter = new IVErrorReporter(false);
        cx.setErrorReporter(errorReporter);
        cx.setLanguageVersion(Context.VERSION_1_5);
        cx.setOptimizationLevel(-1);
        if (args == null) args = new String[0];
        Scriptable argsObj = cx.newArray(global, args);
        global.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);
        return cx;
    }

    /**
     *  Execute the given arguments
     */
    public static int execFile(String fileName, String args[], PrintStream out) {
        Context cx = init(args, out, null);
        processFile(cx, global, fileName);
        cx.exit();
        return exitCode;
    }

    /**
     *  Execute the given arguments
     */
    public static int execFile(String fileName, String args[], PrintStream out, com.iv.flash.context.Context genContext) {
        Context cx = init(args, out, genContext);
        processFile(cx, global, fileName);
        cx.exit();
        return exitCode;
    }

    /**
     *  Execute the given arguments
     */
    public static int execString(String js_text, String args[], PrintStream out, com.iv.flash.context.Context genContext) {
        Context cx = init(args, out, genContext);
        StringReader in = new StringReader(js_text);
        String source = js_text.length() > 20 ? js_text.substring(0, 20) + "..." : js_text;
        evaluateReader(cx, global, in, source, 1);
        cx.exit();
        return exitCode;
    }

    public static Global getGlobal() {
        return getGlobal(null);
    }

    public static Global getGlobal(com.iv.flash.context.Context genContext) {
        if (global == null) {
            try {
                global = new Global(Context.enter(), genContext);
            } finally {
                Context.exit();
            }
        }
        if (global != null) {
            global.genContext = genContext;
        }
        return global;
    }

    public static void processFile(Context cx, Scriptable scope, String filename) {
        Reader in;
        try {
            URL url = new URL(filename);
            InputStream is = url.openStream();
            in = new BufferedReader(new InputStreamReader(is));
        } catch (MalformedURLException mfex) {
            in = null;
        } catch (IOException ioex) {
            Context.reportError(IVErrorReporter.getMessage("msg.couldnt.open.url", filename, ioex.toString()));
            exitCode = EXITCODE_FILE_NOT_FOUND;
            return;
        }
        if (in == null) {
            try {
                in = new FileReader(filename);
                filename = new java.io.File(filename).getCanonicalPath();
            } catch (FileNotFoundException ex) {
                Context.reportError(IVErrorReporter.getMessage("msg.couldnt.open", filename));
                exitCode = EXITCODE_FILE_NOT_FOUND;
                return;
            } catch (IOException ioe) {
                Log.logRB(Resource.JSERROR, ioe);
            }
        }
        evaluateReader(cx, scope, in, filename, 1);
    }

    public static Object evaluateReader(Context cx, Scriptable scope, Reader in, String sourceName, int lineno) {
        Object result = cx.getUndefinedValue();
        try {
            result = cx.evaluateReader(scope, in, sourceName, lineno, null);
        } catch (WrappedException we) {
            Log.logRB(Resource.JSERROR, we);
        } catch (EcmaError ee) {
            String msg = IVErrorReporter.getMessage("msg.uncaughtJSException", ee.toString());
            exitCode = EXITCODE_RUNTIME_ERROR;
            if (ee.getSourceName() != null) {
                Context.reportError(msg, ee.getSourceName(), ee.getLineNumber(), ee.getLineSource(), ee.getColumnNumber());
            } else {
                Context.reportError(msg);
            }
        } catch (EvaluatorException ee) {
            exitCode = EXITCODE_RUNTIME_ERROR;
        } catch (JavaScriptException jse) {
            Object value = jse.getValue();
            if (value instanceof ThreadDeath) throw (ThreadDeath) value;
            exitCode = EXITCODE_RUNTIME_ERROR;
            Context.reportError(IVErrorReporter.getMessage("msg.uncaughtJSException", jse.getMessage()));
        } catch (IOException ioe) {
            Log.logRB(Resource.JSERROR, ioe);
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                Log.logRB(Resource.JSERROR, ioe);
            }
        }
        return result;
    }

    public static ScriptableObject getScope() {
        return global;
    }

    public static InputStream getIn() {
        return Global.getInstance(getGlobal()).getIn();
    }

    public static void setIn(InputStream in) {
        Global.getInstance(getGlobal()).setIn(in);
    }

    public static PrintStream getOut() {
        return Global.getInstance(getGlobal()).getOut();
    }

    public static void setOut(PrintStream out) {
        Global.getInstance(getGlobal()).setOut(out);
    }

    public static PrintStream getErr() {
        return Global.getInstance(getGlobal()).getErr();
    }

    public static void setErr(PrintStream err) {
        Global.getInstance(getGlobal()).setErr(err);
    }

    protected static IVErrorReporter errorReporter;

    protected static Global global;

    protected static int exitCode = 0;

    private static final int EXITCODE_RUNTIME_ERROR = 3;

    private static final int EXITCODE_FILE_NOT_FOUND = 4;
}
