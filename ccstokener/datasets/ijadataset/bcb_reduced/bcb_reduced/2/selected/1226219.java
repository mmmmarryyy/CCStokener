package com.gorillalogic.gython;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import com.gorillalogic.accounts.GXESession;
import com.gorillalogic.config.Bootstrap;
import com.gorillalogic.config.Preferences;
import com.gorillalogic.dal.AccessException;
import com.gorillalogic.dal.OperationException;
import com.gorillalogic.dal.Table;
import com.gorillalogic.dal.Txn;
import com.gorillalogic.dal.Type;
import com.gorillalogic.dal.Universe;
import com.gorillalogic.dal.common.CommonScope;
import com.gorillalogic.dal.common.MethodBlock;
import com.gorillalogic.glob.GLBase;
import com.gorillalogic.glob.impl.GLBaseImpl;
import com.gorillalogic.glob.impl.GLObjectImpl;
import com.gorillalogic.glob.impl.GLObjectListImpl;
import com.gorillalogic.gosh.Gosh;
import com.gorillalogic.gosh.GoshOptions;
import com.gorillalogic.gosh.Script;
import com.gorillalogic.gosh.commands.CommandMap;
import com.gorillalogic.util.ExceptionLogger;
import com.gorillalogic.util.IOUtil;

/**
 * 
 * @author Stu
 */
public class Gython {

    static Logger logger = Logger.getLogger(Gython.class);

    private PythonInterpreter _jython;

    private Gosh _gosh;

    public Gython() throws GythonException {
        try {
            init(Universe.factory.defaultDataWorld().getRootPkg());
        } catch (OperationException e) {
            throw new GythonException(logger, "Error getting root package for default data world", e);
        }
    }

    public Gython(Gosh gosh) throws GythonException {
        _gosh = gosh;
        init(gosh.currentScope().getSelf());
    }

    public Gython(Table context) throws GythonException {
        init(context);
    }

    public Gython(GLBase context) throws GythonException {
        init(((GLBaseImpl) context).getTable());
    }

    /** Creates a new instance of Gython */
    public void init(Table context) throws GythonException {
        init();
        _jython = new PythonInterpreter();
        _jython.exec("import sys");
        _jython.exec("from keyword import *\n");
        _jython.exec("from java.io import *");
        _jython.exec("from com.gorillalogic.glob import *");
        _jython.set("gython", this);
        try {
            if (_gosh == null) {
                _gosh = Gosh.factory.makePassThruShell(context);
            } else {
                _gosh.pushScope(context, false);
            }
        } catch (AccessException e) {
            throw new GythonException(logger, "Error getting root package for default data world", e);
        }
        _jython.set(Type.GLPREFIX + "gosh", _gosh);
    }

    /**
     * Sets assigns value to name in underlying Jython namespace
     * 
     * @param name -
     *            Variable name
     * @param value -
     *            Value
     */
    public void set(String name, Object value) {
        _jython.set(name, value);
    }

    public String execFile(String filename) throws GythonException {
        Reader reader;
        try {
            reader = new FileReader(new File(filename));
        } catch (FileNotFoundException e) {
            String msg = filename + " not found ";
            GythonException ex = new GythonException(msg, e);
            logger.error(msg, ex);
            throw ex;
        }
        String s = "";
        try {
            s = exec(reader);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new GythonException("Error closing " + filename);
            }
        }
        return s;
    }

    public String exec(String script) throws GythonException {
        return exec(new StringReader(script));
    }

    public String exec(InputStream in) throws GythonException {
        return exec(new InputStreamReader(in));
    }

    public String exec(Reader script) throws GythonException {
        StringWriter output = new StringWriter();
        run(script, new PrintWriter(output));
        return output.toString();
    }

    public void run(Script script, PrintWriter output) throws GythonException {
        Reader rdr = null;
        try {
            rdr = script.openAsReader();
        } catch (Script.InvalidException e) {
            throw new GythonException("error opening file for gython script: " + e.getMessage(), e);
        }
        try {
            run(rdr, output);
        } finally {
            try {
                rdr.close();
            } catch (IOException e) {
                throw new GythonException("exception caught closing Rdr " + "from script " + script.getFilePath());
            }
        }
    }

    public void run(Reader rdr, PrintWriter output) throws GythonException {
        PrintWriter prev = GXESession.factory.currentSession().getWriter();
        GXESession.factory.currentSession().setWriter(output);
        try {
            run(rdr);
        } finally {
            _jython.exec("print");
            GXESession.factory.currentSession().setWriter(prev);
        }
    }

    public void run(Reader script) throws GythonException {
        long start = System.currentTimeMillis();
        String line;
        int outChars = 0;
        GythonExecutable block;
        StringBuffer buf = new StringBuffer();
        int c;
        if (!script.markSupported()) {
            try {
                while ((c = script.read()) != -1) {
                    buf.append((char) c);
                }
            } catch (IOException e1) {
                throw new GythonException(logger, "Error reading script", e1);
            }
            script = new StringReader(buf.toString());
        }
        try {
            block = (GythonExecutable) GythonExecutableFactory.evaluateBody(script, "anonymous");
        } catch (AccessException e) {
            throw (GythonException) e.getCause();
        }
        try {
            if (block != null) {
                block.compute((CommonScope) _gosh.currentScope(), this);
            } else {
                script.reset();
                _gosh.run(script, GXESession.factory.currentSession().getWriter());
            }
        } catch (AccessException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(ExceptionLogger.getMessage(e));
            }
            throw new GythonException(ExceptionLogger.getMessage(e), e);
        } catch (Exception e) {
            throw new GythonException(logger, "Error executing gython script", e);
        } finally {
            _jython.exec("print");
        }
        long stop = System.currentTimeMillis();
    }

    public Vector lineVector(String script) {
        LineNumberReader reader = new LineNumberReader(new StringReader(script));
        String line;
        Vector results = new Vector();
        try {
            int i = 0;
            while ((line = reader.readLine()) != null) {
                results.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static String lineNumber(String script) {
        LineNumberReader reader = new LineNumberReader(new StringReader(script));
        String line;
        StringBuffer results = new StringBuffer();
        StringBuffer lineNum;
        String num;
        int i = 0;
        try {
            while ((line = reader.readLine()) != null) {
                lineNum = new StringBuffer("     : ");
                num = new Integer(i++).toString();
                lineNum.replace(5 - num.length(), 5, num);
                results.append(lineNum.toString() + line + '\n');
            }
            return results.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results.toString();
    }

    private void setGoshContext(Table t) throws GythonException {
        try {
            _gosh = Gosh.factory.makePassThruShell(t);
        } catch (AccessException e) {
            String msg = "Error creating new Gosh shell";
            GythonException ex = new GythonException(msg, e);
            logger.error(msg, e);
            throw ex;
        }
    }

    /**
     * to allow a Gython instance to be used to feed a gosh instance
     * line-by-line
     */
    public Gosh getGosh() {
        return _gosh;
    }

    /**
     * get the current scope/context as a GLBase if there is just one row, this
     * will be GLObject otherwise it will be a GLObjectList
     */
    public GLBase getContext() {
        GLBase g = null;
        Table t = getGosh().currentScope().getData();
        if (t != null) {
            if (t.rowCount() == 1) {
                try {
                    g = new GLObjectImpl(t.asRow());
                } catch (AccessException e) {
                    throw new com.gorillalogic.dal.InternalException("cannot create GLObject from one-row table: " + t.path());
                }
            } else {
                g = new GLObjectListImpl(t);
            }
        }
        return g;
    }

    /**
     * set the current context to the supplied glob
     */
    public void setContext(GLBase g) throws GythonException {
        setGoshContext(((GLBaseImpl) g).getTable());
    }

    public boolean isPureGosh(String source) {
        int newLineNdx = source.indexOf('\n');
        if (newLineNdx == -1) {
            return isPureGoshLine(source);
        } else {
            LineNumberReader lines = new LineNumberReader(new StringReader(source));
            String line;
            try {
                while ((line = lines.readLine()) != null) {
                    if (!isPureGoshLine(line)) {
                        return false;
                    }
                }
                return true;
            } catch (IOException e) {
                throw new RuntimeException("Error reading source from string", e);
            }
        }
    }

    public boolean isPureGoshLine(String line) {
        if (line.length() == 0) {
            return true;
        }
        if (line.indexOf("${") > -1) {
            return false;
        }
        boolean isGoshLine = isGoshLine(line);
        if (!isGoshLine) {
            if (line.trim().startsWith("return")) {
                return true;
            }
            return false;
        }
        return true;
    }

    public boolean isGoshLine(String line) {
        if (line.trim().startsWith("begin(") && line.length() > 6) {
            line = "begin (" + line.substring(6);
        }
        String keyword = GySource.firstWord(line);
        if (keyword.length() > 1 && keyword.startsWith("--")) return true;
        return (CommandMap.isCommand(keyword) && _jython.eval("iskeyword(\"" + keyword + "\")").toString().equals("0"));
    }

    public String substitute(String s) throws GythonException {
        int index = s.indexOf("${");
        if (index == -1) {
            return "\'" + s + "\'";
        }
        int pos = 0;
        int i;
        StringBuffer result = new StringBuffer();
        while (pos < s.length()) {
            result.append("\'" + s.substring(pos, index).replaceAll("\\'", "\\\\'") + "\'");
            int endIndex;
            i = 0;
            do {
                endIndex = s.indexOf("}", index + 2 + i++);
                if (endIndex == -1) {
                    break;
                }
            } while (s.charAt(endIndex - 1) == '\\');
            if (endIndex == -1) {
                String msg = "Closing } not found in " + s;
                GythonException e = new GythonException(msg);
                logger.error(msg, e);
                throw e;
            }
            String expr = s.substring(index + 2, endIndex);
            result.append(" + str(" + expr + ")");
            pos = endIndex + 1;
            index = s.indexOf("${", pos);
            if (index == -1) {
                break;
            } else {
                result.append(" + ");
            }
        }
        if (pos < s.length()) {
            result.append("+" + "\'" + s.substring(pos) + "\'");
        }
        return result.toString();
    }

    public void outputPrompt() {
        PrintWriter output = GXESession.factory.currentSession().getWriter();
        output.print("<b><em>");
        _gosh.formatPrompt(output);
        output.print("</em></b>");
    }

    public String getPrompt() {
        StringWriter s = new StringWriter();
        _gosh.formatPrompt(new PrintWriter(s));
        return s.toString();
    }

    public PythonInterpreter getJython() {
        return _jython;
    }

    /**
     * @throws GythonException
     * 
     */
    public void check() throws GythonException {
        try {
            Txn.mgr.check();
        } catch (AccessException e) {
            throw new GythonException(e.getMessage(), e);
        }
    }

    public static void init() throws GythonException {
        _init();
    }

    private static boolean _isInitialized = false;

    private static synchronized void _init() throws GythonException {
        if (_isInitialized) return;
        _isInitialized = true;
        logger.debug("Initializing Jython");
        MethodBlock.registry.register(new GythonMethodBlockFactory());
        GythonScriptHandler gyHandler = new GythonScriptHandler();
        Script.factory.registerExtension("gy", gyHandler);
        Script.factory.registerExtension("jy", gyHandler);
        Script.factory.registerExtension("py", new GythonScriptHandler());
        Properties props = new Properties();
        PythonInterpreter.initialize(System.getProperties(), props, new String[0]);
        File pHome = initPythonHome();
        PythonInterpreter jython = new PythonInterpreter();
        jython.exec("import sys");
        if (logger.isEnabledFor(Priority.DEBUG)) {
            jython.exec("print 'Gython.init() - sys.path=', sys.path");
        }
        jython.exec("import gorillautils");
        jython.exec("sys.stdout = gorillautils.GXEOutput()");
        jython.exec("sys.stderr = gorillautils.GXEOutput()");
        PySystemState.add_package("com.gorillalogic.gosh");
        logger.info("Gython bootup: " + " python.home=" + pHome.getPath());
    }

    private static File initPythonHome() throws GythonException {
        String pHome = forcePySrcDir().getPath();
        setPythonHome(pHome);
        setGorillaPySrcDir(pHome);
        if (runningAsWebapp()) {
            String bootDir = initGythonBootDir().getPath();
            PySystemState.add_extdir(bootDir + "/../lib");
            PySystemState.add_classdir(bootDir + "/../classes");
        }
        return new File(pHome);
    }

    private static File forcePySrcDir() throws GythonException {
        File pySrcDir = null;
        String pHome = System.getProperty("python.home");
        if (pHome != null && pHome.length() > 0) {
            pySrcDir = new File(pHome);
            if (hasPySrc(pySrcDir) || tryExpandPySrc(pySrcDir)) {
                return pySrcDir;
            }
        }
        pySrcDir = new File(initGythonBootDir().getPath() + "/" + Gython.class.getPackage().getName().replaceAll("[.]", "/") + "/jython");
        if (hasPySrc(pySrcDir)) {
            return pySrcDir;
        }
        pySrcDir = new File(initGythonBootDir().getPath() + "/jython");
        if (hasPySrc(pySrcDir) || tryExpandPySrc(pySrcDir)) {
            return pySrcDir;
        }
        pySrcDir = new File(Preferences.getGorillaHome() + "/jython");
        if (hasPySrc(pySrcDir) || tryExpandPySrc(pySrcDir)) {
            return pySrcDir;
        }
        pySrcDir = new File(System.getProperty("user.dir") + "/.jython");
        if (hasPySrc(pySrcDir) || tryExpandPySrc(pySrcDir)) {
            return pySrcDir;
        }
        pySrcDir = new File(System.getProperty("user.home") + "/.jython");
        if (hasPySrc(pySrcDir) || tryExpandPySrc(pySrcDir)) {
            return pySrcDir;
        }
        pySrcDir = new File(System.getProperty("java.io.tmpdir") + "/.jython");
        if (hasPySrc(pySrcDir) || tryExpandPySrc(pySrcDir)) {
            return pySrcDir;
        }
        return pySrcDir;
    }

    private static boolean hasPySrc(File f) {
        if (f.exists() && f.isDirectory()) {
            File f2 = new File(f.getPath() + "/Lib/keyword.py");
            if (f2.exists()) {
                File f3 = new File(f.getPath() + "/gorillautils.py");
                if (f3.exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean tryExpandPySrc(File f) throws GythonException {
        if (f.exists()) {
            if (!f.isDirectory() || !f.canWrite()) {
                return false;
            }
        } else {
            boolean dirOK = f.mkdirs();
        }
        if (f.exists() && f.isDirectory() && f.canWrite()) {
            java.net.URL url = Gython.class.getResource("jython");
            if (url == null) {
                throw new GythonException("cannot find python source code resources relative to Gython jar file");
            }
            java.net.URLConnection conn;
            try {
                conn = url.openConnection();
            } catch (IOException e) {
                String msg = "Error opening connection to " + url.toString();
                logger.error(msg, e);
                throw new GythonException("Error copying " + url.toString(), e);
            }
            if (conn == null) {
                throw new GythonException("cannot find python source code resources relative to Gython jar file");
            }
            if (conn instanceof java.net.JarURLConnection) {
                logger.debug("Expanding python source from jar file");
                try {
                    IOUtil.expandJar((java.net.JarURLConnection) conn, f);
                    return true;
                } catch (Exception e) {
                    throw new GythonException("Error expanding python source" + " from jar file at " + conn.getURL().toString() + ": " + e.getMessage());
                }
            } else {
                try {
                    IOUtil.copyDir(new File(url.getFile()), f);
                    return true;
                } catch (Exception e) {
                    throw new GythonException("Error expanding python source" + " from jar file at " + conn.getURL().toString() + ": " + e.getMessage());
                }
            }
        }
        return false;
    }

    private static void setGorillaPySrcDir(String dirName) {
        File pySrcDir = null;
        pySrcDir = new File(dirName);
        logger.debug("Gorilla python modules: " + pySrcDir.getPath());
        Py.getSystemState().path.append(new PyString(pySrcDir.getPath()));
    }

    private static void setPythonHome(String dirName) {
        File pythonHome;
        pythonHome = new File(dirName);
        logger.debug("Python home: " + pythonHome.getPath());
        Py.getSystemState().path.append(new PyString(pythonHome.getPath() + "/Lib"));
    }

    private static File initGythonBootDir() throws GythonException {
        String bootDir = System.getProperty("gorilla.config.gython.bootDir");
        if (bootDir != null) {
            return new File(bootDir);
        }
        File bootDirFile = null;
        try {
            bootDirFile = IOUtil.getClasspathDir(Gython.class);
        } catch (com.gorillalogic.core.GorillaException e) {
            throw new GythonException("could not bootstrap root of Gython classpath.");
        }
        return bootDirFile;
    }

    private static boolean runningAsWebapp() {
        return (new File(Preferences.getBootDir("."), "../../WEB-INF").exists());
    }

    private static class GythonScriptHandler implements Script.Handler {

        public void run(Script script, PrintWriter out, Table context, GoshOptions options) throws AccessException {
            Gython g = new Gython(context);
            g.run(script, out);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: Gython gython-script|deploy-file");
            return;
        }
        File file = new File(args[0]);
        if (!file.exists()) {
            if (args[0].endsWith(".gy")) {
                System.out.println("File " + args[0] + " not found");
                return;
            }
            file = new File(args[0] + ".gy");
            if (!file.exists()) {
                System.out.println("File " + args[0] + " not found");
                return;
            }
        }
        Bootstrap.init();
        logger.debug("Bootstrapping complete");
        Gython gython = new Gython();
        try {
            gython.run(new FileReader(file), new PrintWriter(System.out));
        } catch (GythonRunException e) {
            ((GythonRunException) e).printException();
            System.out.println(lineNumber(e.getSource()));
            System.out.println(lineNumber(e.getPySource()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
