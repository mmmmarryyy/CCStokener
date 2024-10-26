package fr.x9c.cadmium.kernel;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import fr.x9c.cadmium.util.RandomAccessInputStream;

/**
 * Main class of Cadmium interpreter.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.3
 * @since 1.0
 */
public final class Interpreter {

    /** Exception message, when a class cannot be loaded. */
    private static final String UNABLE_TO_LOAD_CLASS = "unable to load class '%s'";

    /** Map from primitive name to primitive implementation (as method). */
    private static Map<String, Method> BUILTIN_PRIMITIVES;

    /** Interpreter context. */
    private final Context context;

    /**
     * Constructs an interpreter from stream and parameters. <br/>
     * Bytecode is loaded into memory from given stream.
     * @param bcp bytecode parameters - should not be <tt>null</tt>
     * @param dir interpreter directory - should not be <tt>null</tt>
     * @param source stream to read bytecode from - should not be <tt>null</tt>
     *               <br/>not closed after code has been loaded
     * @param customs additional custom types
     * @throws IOException if bytecode cannot be loaded
     * @throws Fatal.Exception if an unsupported element is encountered
     * @throws CadmiumException if MD5 provider is missing
     * @throws CadmiumException if a primitive is missing
     */
    public Interpreter(final ByteCodeParameters bcp, final File dir, final RandomAccessInputStream source, final Custom.Operations... customs) throws IOException, CadmiumException, Fatal.Exception {
        assert bcp != null : "null bcp";
        assert dir != null : "null dir";
        assert source != null : "null source";
        assert customs != null : "null customs";
        this.context = new Context(bcp, false, dir);
        Debugger.init(this.context);
        for (Custom.Operations ops : customs) {
            this.context.registerCustom(ops);
        }
        final FileLoader loader;
        try {
            loader = new FileLoader(this.context, source);
        } catch (final NoSuchAlgorithmException nsae) {
            throw new CadmiumException("No MD5 provider", nsae);
        }
        buildBuiltinPrimitivesMap();
        final List<String> primitives = loader.getPrimitives();
        final int szPrim = primitives.size();
        final String[] primitiveNames = new String[szPrim];
        primitives.toArray(primitiveNames);
        final Method[] primitiveTable = new Method[szPrim];
        final Class[] primitivesImpl = mapClasses(bcp.getProviders());
        for (int i = 0; i < szPrim; i++) {
            final String prim = primitiveNames[i];
            if (Interpreter.BUILTIN_PRIMITIVES.containsKey(prim)) {
                primitiveTable[i] = Interpreter.BUILTIN_PRIMITIVES.get(prim);
            } else {
                primitiveTable[i] = Primitives.lookupPrimitive(prim, primitivesImpl);
            }
        }
        Dispatcher dispatcher = bcp.isDispatcherCompiled() ? DispatcherCompiler.make(primitiveNames, primitiveTable) : new ReflectionDispatcher(primitiveNames, primitiveTable);
        if (dispatcher == null) {
            bcp.getStandardError().println("error in dispatcher compilation -- using reflection dispatcher");
            dispatcher = new ReflectionDispatcher(primitiveNames, primitiveTable);
        }
        this.context.setDispatcher(dispatcher);
    }

    /**
     * Returns the interpreter context.
     * @return the interpreter context
     */
    public Context getContext() {
        return this.context;
    }

    /**
     * Actually executes the bytecode, from code start. <br/> <br/>
     * If an exception is throws by the interpreted program, the exception
     * is writen on the standard error of the interpreter and also returned
     * as the cause of a {@link fr.x9c.cadmium.kernel.CadmiumException}.
     * Such a cause is either a {@link fr.x9c.cadmium.kernel.Fail.Exception}
     * or a {@link fr.x9c.cadmium.kernel.Fatal.Exception} instance; otherwise,
     * the exception indicates an internal error (read bug) of Cadmium.<br/><br/>
     * The functions registered by <i>Pervasives.at_exit</i> are run. <br/>
     * Signals handlers are removed after execution.
     * @throws CadmiumException if an internal error occurs
     * @throws CadmiumException if an exception is thrown by the
     *                          interpreted code
     */
    public Value execute() throws CadmiumException {
        return runMain(null, null);
    }

    /**
     * Actually executes a function from the bytecode. This method is hence
     * intended to be used after callback registration by bytecode. <br/> <br/>
     * If an exception is throws by the interpreted program, the exception
     * is writen on the standard error of the interpreter and also returned
     * as the cause of an {@link fr.x9c.cadmium.kernel.CadmiumException}.
     * Such a cause is either a {@link fr.x9c.cadmium.kernel.Fail.Exception}
     * or a {@link fr.x9c.cadmium.kernel.Fatal.Exception} instance. <br/> <br/>
     * The functions registered by <i>Pervasives.at_exit</i> are run. <br/>
     * Signals handlers are removed after execution.
     * @param function function to be executed - should not be <tt>null</tt>
     * @param params parameters to function - should not be <tt>null</tt> <br/>
     *               it is the responsibility of the programmer to supply the
     *               correct number and types of parameters
     * @return the value returned by function application to the parameters
     * @throws CadmiumException if an internal error occurs
     * @throws CadmiumException if the function has not been registered
     * @throws CadmiumException if an exception is thrown by the interpreted code
     */
    public Value execute(final String function, final Value... params) throws CadmiumException {
        assert function != null : "null function";
        assert params != null : "null params";
        assert params.length + 4 <= 256 : "params is too long";
        final Value closure = this.context.getCallback(function);
        if (closure == null) {
            throw new CadmiumException("unknown callback");
        }
        return runMain(closure, null, params);
    }

    /**
     * Actually executes a function from the bytecode. This method is hence
     * intended to be used after callback registration by bytecode. <br/> <br/>
     * If an exception is throws by the interpreted program, the exception
     * is writen on the standard error of the interpreter and also returned
     * as the cause of an {@link fr.x9c.cadmium.kernel.CadmiumException}.
     * Such a cause is either a {@link fr.x9c.cadmium.kernel.Fail.Exception}
     * or a {@link fr.x9c.cadmium.kernel.Fatal.Exception} instance. <br/> <br/>
     * The functions registered by <i>Pervasives.at_exit</i> are run. <br/>
     * Signals handlers are removed after execution.
     * @param function function to be executed - should not be <tt>null</tt>
     * @param bindings scripting bindings - should not be <tt>null</tt>
     * @param params parameters to function - should not be <tt>null</tt> <br/>
     *               it is the responsibility of the programmer to supply the
     *               correct number and types of parameters
     * @return the value returned by function application to the parameters
     * @throws CadmiumException if an internal error occurs
     * @throws CadmiumException if the function has not been registered
     * @throws CadmiumException if an exception is thrown by the interpreted code
     */
    public Value executeWithBindings(final String function, final Map<String, Value> bindings, final Value... params) throws CadmiumException {
        assert function != null : "null function";
        assert bindings != null : "null bindings";
        assert params != null : "null params";
        assert params.length + 4 <= 256 : "params is too long";
        final Value closure = this.context.getCallback(function);
        if (closure == null) {
            throw new CadmiumException("unknown callback");
        }
        return runMain(closure, bindings, params);
    }

    /**
     * Returns the builtin primitives map, building it if necessary.
     * @return the builtin primitives map
     * @throws CadmiumException if the map cannot be built
     */
    static Map<String, Method> getBuiltinPrimitivesMap() throws CadmiumException {
        buildBuiltinPrimitivesMap();
        return BUILTIN_PRIMITIVES;
    }

    /**
     * Runs the main thread of an interpreted program.
     * @param closure closure to be run (<tt>null</tt> to run from code start)
     * @param bindings scripting bindings
     * @param params parameters to closure
     *               (ignored if closure is <tt>null</tt>)
     * @throws CadmiumException if an internal error occurs
     * @throws CadmiumException if an exception is thrown by the interpreted code
     */
    private Value runMain(final Value closure, final Map<String, Value> bindings, final Value... params) throws CadmiumException {
        final ByteCodeRunner runner = new ByteCodeRunner(this, null, true);
        runner.setBindings(bindings);
        this.context.setMainCodeRunner(runner);
        runner.setup(closure, params);
        final CadmiumThread thread = new CadmiumThread(this.context.getThreadGroup(), runner);
        this.context.setMainThread(thread);
        thread.start();
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (final InterruptedException ie) {
                Signals.unregisterContext(this.context);
                this.context.clearSignals();
                try {
                    final int exitCode = FalseExit.createFromContext(this.context).getExitCode();
                    return Value.createFromLong(exitCode);
                } catch (final Fail.Exception fe) {
                }
            }
        }
        Signals.unregisterContext(this.context);
        this.context.clearSignals();
        final Throwable exn = runner.getException();
        if (exn == null) {
            try {
                Debugger.handleEvent(runner, Debugger.EventKind.PROGRAM_EXIT);
            } catch (final FalseExit fe) {
                throw new CadmiumException("error during debugger event handling", fe);
            } catch (final Fail.Exception fe) {
                throw new CadmiumException("error during debugger event handling", fe);
            } catch (final Fatal.Exception fe) {
                throw new CadmiumException("error during debugger event handling", fe);
            }
            return runner.getResult();
        } else {
            try {
                Debugger.handleEvent(runner, Debugger.EventKind.UNCAUGHT_EXC);
            } catch (final FalseExit fe) {
                throw new CadmiumException("error during debugger event handling", fe);
            } catch (final Fail.Exception fe) {
                throw new CadmiumException("error during debugger event handling", fe);
            } catch (final Fatal.Exception fe) {
                throw new CadmiumException("error during debugger event handling", fe);
            }
            if (closure != null) {
                throw new CadmiumException("callback exception", exn);
            }
            final Channel ch = this.context.getChannel(Channel.STDERR);
            final PrintStream err;
            final ByteArrayOutputStream altErr;
            if ((ch != null) && (ch.asOutputStream() != null)) {
                altErr = null;
                err = new PrintStream(ch.asOutputStream(), true);
            } else {
                altErr = new ByteArrayOutputStream();
                err = new PrintStream(altErr, true);
            }
            final boolean backtrace = this.context.isBacktraceActive();
            this.context.setBacktraceActive(false);
            final Value atExit = this.context.getCallback("Pervasives.do_at_exit");
            if (atExit != null) {
                try {
                    runner.callback(atExit, Value.UNIT);
                } catch (final Exception e) {
                }
            }
            this.context.setBacktraceActive(backtrace);
            if (exn instanceof Fail.Exception) {
                final String msg = Misc.convertException(((Fail.Exception) exn).asValue(this.context.getGlobalData()));
                err.println("Fatal error: exception " + msg);
                if (this.context.isBacktraceActive() && !this.context.isDebuggerInUse()) {
                    runner.printExceptionBacktrace(err);
                }
                err.close();
                throw new CadmiumException(altErr != null ? altErr.toString() : "program exception", exn);
            } else if (exn instanceof Fatal.Exception) {
                err.println(((Fatal.Exception) exn).getMessage());
                err.close();
                throw new CadmiumException(altErr != null ? altErr.toString() : "fatal error", exn);
            } else {
                err.println(exn.toString());
                err.close();
                throw new CadmiumException(altErr != null ? altErr.toString() : "internal error", exn);
            }
        }
    }

    /**
     * Builds the map of builtin primitives, if needed
     * (done once for all <tt>Interpreter</tt> instances). <br/>
     * There is only one (static) map for builtin primitives, shared by
     * all interpreters, hence the map is built by the first interpreter
     * being instantiated.
     * @throws CadmiumException if a class of the standard distribution
     *                              cannot be loaded
     */
    private static void buildBuiltinPrimitivesMap() throws CadmiumException {
        synchronized (Interpreter.class) {
            if (Interpreter.BUILTIN_PRIMITIVES == null) {
                Interpreter.BUILTIN_PRIMITIVES = new HashMap<String, Method>();
                for (String[] lib : Libraries.ALL_LIBRARIES) {
                    for (String cls : lib) {
                        try {
                            Primitives.loadAllPrimitives(Interpreter.BUILTIN_PRIMITIVES, Class.forName(cls));
                        } catch (final Throwable t) {
                            final String msg = String.format(Interpreter.UNABLE_TO_LOAD_CLASS, cls);
                            throw new CadmiumException(msg, t);
                        }
                    }
                }
            }
        }
    }

    /**
     * Maps an array of class names into an array of actual class objects,
     * also adding classes located using SPI.
     * @param classNames class names to map - should not be <tt>null</tt>
     * @return an array of class object corresponding to the passed class names,
     *         with classes located using SPI
     * @throws CadmiumException if any of the classes cannot be loaded
     */
    private static Class[] mapClasses(final String[] classNames) throws CadmiumException {
        assert classNames != null : "null classNames";
        final Class[] spi = getSPIClasses();
        final int len = classNames.length;
        final Class[] res = new Class[len + spi.length];
        for (int i = 0; i < len; i++) {
            try {
                res[i] = Class.forName(classNames[i]);
            } catch (final Throwable t) {
                final String msg = String.format(Interpreter.UNABLE_TO_LOAD_CLASS, classNames[i]);
                throw new CadmiumException(msg, t);
            }
        }
        System.arraycopy(spi, 0, res, len, spi.length);
        return res;
    }

    /**
     * Constructs an array containing all the primitive providers using SPI.
     * @return an array containing all the primitive providers using SPI
     */
    private static Class[] getSPIClasses() {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            final List<Class> classes = new LinkedList<Class>();
            final Enumeration<URL> e = cl.getResources("META-INF/services/fr.x9c.cadmium.PrimitiveProvider");
            while (e.hasMoreElements()) {
                final URL url = e.nextElement();
                for (String s : readClassNames(url)) {
                    try {
                        classes.add(Class.forName(s));
                    } catch (final ClassNotFoundException cnfe) {
                    }
                }
            }
            final Class[] res = new Class[classes.size()];
            return classes.toArray(res);
        } catch (final IOException ioe) {
            return new Class[0];
        }
    }

    /**
     * Reads the list of classes from a (SPI) URL.
     * @param url URL to read classes from - should not be <tt>null</tt>
     * @return the list of classes read from the passed URL
     */
    private static List<String> readClassNames(final URL url) {
        assert url != null : "null url";
        final List<String> res = new LinkedList<String>();
        try {
            final InputStreamReader isr = new InputStreamReader(url.openStream());
            final BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();
            while (line != null) {
                final int idx = line.indexOf('#');
                if (idx != -1) {
                    line = line.substring(0, idx);
                }
                line = line.trim();
                if (line.length() != 0) {
                    res.add(line);
                }
                line = br.readLine();
            }
            br.close();
            isr.close();
        } catch (final IOException ioe) {
        }
        return res;
    }
}
