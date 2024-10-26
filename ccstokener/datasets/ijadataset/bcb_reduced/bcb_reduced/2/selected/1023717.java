package tcl.lang;

import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * The Tcl interpreter class.
 */
public class Interp {

    Hashtable reflectIDTable = new Hashtable();

    Hashtable reflectObjTable = new Hashtable();

    long reflectObjCount = 0;

    private Thread primaryThread;

    private static final int MAX_ERR_LENGTH = 200;

    static final String TCL_VERSION = "8.0";

    static final String TCL_PATCH_LEVEL = "8.0";

    protected int cmdCount;

    Hashtable interpChanTable;

    private Notifier notifier;

    Hashtable assocData;

    private File workingDir;

    CallFrame frame;

    CallFrame varFrame;

    NamespaceCmd.Namespace globalNs;

    String scriptFile;

    int nestLevel;

    int maxNestingDepth;

    int evalFlags;

    int flags;

    int termOffset;

    Expression expr;

    int noEval;

    boolean randSeedInit;

    long randSeed;

    String errorInfo;

    String errorCode;

    protected int returnCode;

    protected boolean deleted;

    protected boolean errInProgress;

    protected boolean errAlreadyLogged;

    protected boolean errCodeSet;

    int errorLine;

    private TclObject m_result;

    private TclObject m_nullResult;

    Hashtable packageTable;

    String packageUnknown;

    TclObject[][][] parserObjv;

    int[] parserObjvUsed;

    TclToken[] parserTokens;

    int parserTokensUsed;

    Hashtable[] importTable = { new Hashtable(), new Hashtable() };

    public Interp() {
        errorLine = 0;
        m_nullResult = TclString.newInstance("");
        m_nullResult.preserve();
        m_result = m_nullResult;
        expr = new Expression();
        nestLevel = 0;
        maxNestingDepth = 1000;
        frame = null;
        varFrame = null;
        returnCode = TCL.OK;
        errorInfo = null;
        errorCode = null;
        packageTable = new Hashtable();
        packageUnknown = null;
        cmdCount = 0;
        termOffset = 0;
        evalFlags = 0;
        scriptFile = null;
        flags = 0;
        assocData = null;
        globalNs = null;
        globalNs = NamespaceCmd.createNamespace(this, null, null);
        if (globalNs == null) {
            throw new TclRuntimeError("Interp(): can't create global namespace");
        }
        workingDir = new File(Util.tryGetSystemProperty("user.dir", "."));
        noEval = 0;
        primaryThread = Thread.currentThread();
        notifier = Notifier.getNotifierForThread(primaryThread);
        notifier.preserve();
        randSeedInit = false;
        deleted = false;
        errInProgress = false;
        errAlreadyLogged = false;
        errCodeSet = false;
        dbg = initDebugInfo();
        Parser.init(this);
        TclParse.init(this);
        interpChanTable = TclIO.getInterpChanTable(this);
        Util.setupPrecisionTrace(this);
        createCommands();
        try {
            setVar("tcl_platform", "platform", "java", TCL.GLOBAL_ONLY);
            setVar("tcl_platform", "byteOrder", "bigEndian", TCL.GLOBAL_ONLY);
            setVar("tcl_platform", "os", Util.tryGetSystemProperty("os.name", "?"), TCL.GLOBAL_ONLY);
            setVar("tcl_platform", "osVersion", Util.tryGetSystemProperty("os.version", "?"), TCL.GLOBAL_ONLY);
            setVar("tcl_platform", "machine", Util.tryGetSystemProperty("os.arch", "?"), TCL.GLOBAL_ONLY);
            setVar("tcl_version", TCL_VERSION, TCL.GLOBAL_ONLY);
            setVar("tcl_patchLevel", TCL_PATCH_LEVEL, TCL.GLOBAL_ONLY);
            setVar("tcl_library", "resource:/tcl/lang/library", TCL.GLOBAL_ONLY);
            if (Util.isWindows()) {
                setVar("tcl_platform", "host_platform", "windows", TCL.GLOBAL_ONLY);
            } else if (Util.isMac()) {
                setVar("tcl_platform", "host_platform", "macintosh", TCL.GLOBAL_ONLY);
            } else {
                setVar("tcl_platform", "host_platform", "unix", TCL.GLOBAL_ONLY);
            }
            Env.initialize(this);
            pkgProvide("Tcl", TCL_VERSION);
            evalResource("/tcl/lang/library/init.tcl");
        } catch (TclException e) {
            System.out.println(getResult());
            e.printStackTrace();
            throw new TclRuntimeError("unexpected TclException: " + e);
        }
    }

    public void dispose() {
        if (deleted) {
            return;
        }
        deleted = true;
        if (nestLevel > 0) {
            throw new TclRuntimeError("dispose() called with active evals");
        }
        if (notifier != null) {
            notifier.release();
            notifier = null;
        }
        NamespaceCmd.teardownNamespace(globalNs);
        TclObject errorInfoObj = null, errorCodeObj = null;
        try {
            errorInfoObj = getVar("errorInfo", null, TCL.GLOBAL_ONLY);
        } catch (TclException e) {
        }
        if (errorInfoObj != null) {
            errorInfoObj.preserve();
        }
        try {
            errorCodeObj = getVar("errorCode", null, TCL.GLOBAL_ONLY);
        } catch (TclException e) {
        }
        if (errorCodeObj != null) {
            errorCodeObj.preserve();
        }
        frame = null;
        varFrame = null;
        try {
            if (errorInfoObj != null) {
                setVar("errorInfo", null, errorInfoObj, TCL.GLOBAL_ONLY);
                errorInfoObj.release();
            }
            if (errorCodeObj != null) {
                setVar("errorCode", null, errorCodeObj, TCL.GLOBAL_ONLY);
                errorCodeObj.release();
            }
        } catch (TclException e) {
        }
        expr = null;
        while (assocData != null) {
            Hashtable table = assocData;
            assocData = null;
            for (Enumeration e = table.keys(); e.hasMoreElements(); ) {
                Object key = e.nextElement();
                AssocData data = (AssocData) table.get(key);
                data.disposeAssocData(this);
                table.remove(key);
            }
        }
        NamespaceCmd.deleteNamespace(globalNs);
        globalNs = null;
        frame = null;
        varFrame = null;
        resetResult();
    }

    protected void finalize() {
        dispose();
    }

    protected void createCommands() {
        Extension.loadOnDemand(this, "after", "tcl.lang.AfterCmd");
        Extension.loadOnDemand(this, "append", "tcl.lang.AppendCmd");
        Extension.loadOnDemand(this, "array", "tcl.lang.ArrayCmd");
        Extension.loadOnDemand(this, "break", "tcl.lang.BreakCmd");
        Extension.loadOnDemand(this, "case", "tcl.lang.CaseCmd");
        Extension.loadOnDemand(this, "catch", "tcl.lang.CatchCmd");
        Extension.loadOnDemand(this, "cd", "tcl.lang.CdCmd");
        Extension.loadOnDemand(this, "clock", "tcl.lang.ClockCmd");
        Extension.loadOnDemand(this, "close", "tcl.lang.CloseCmd");
        Extension.loadOnDemand(this, "continue", "tcl.lang.ContinueCmd");
        Extension.loadOnDemand(this, "concat", "tcl.lang.ConcatCmd");
        Extension.loadOnDemand(this, "eof", "tcl.lang.EofCmd");
        Extension.loadOnDemand(this, "eval", "tcl.lang.EvalCmd");
        Extension.loadOnDemand(this, "error", "tcl.lang.ErrorCmd");
        if (!Util.isMac()) {
            Extension.loadOnDemand(this, "exec", "tcl.lang.ExecCmd");
        }
        Extension.loadOnDemand(this, "exit", "tcl.lang.ExitCmd");
        Extension.loadOnDemand(this, "expr", "tcl.lang.ExprCmd");
        Extension.loadOnDemand(this, "fblocked", "tcl.lang.FblockedCmd");
        Extension.loadOnDemand(this, "fconfigure", "tcl.lang.FconfigureCmd");
        Extension.loadOnDemand(this, "file", "tcl.lang.FileCmd");
        Extension.loadOnDemand(this, "flush", "tcl.lang.FlushCmd");
        Extension.loadOnDemand(this, "for", "tcl.lang.ForCmd");
        Extension.loadOnDemand(this, "foreach", "tcl.lang.ForeachCmd");
        Extension.loadOnDemand(this, "format", "tcl.lang.FormatCmd");
        Extension.loadOnDemand(this, "gets", "tcl.lang.GetsCmd");
        Extension.loadOnDemand(this, "global", "tcl.lang.GlobalCmd");
        Extension.loadOnDemand(this, "glob", "tcl.lang.GlobCmd");
        Extension.loadOnDemand(this, "if", "tcl.lang.IfCmd");
        Extension.loadOnDemand(this, "incr", "tcl.lang.IncrCmd");
        Extension.loadOnDemand(this, "info", "tcl.lang.InfoCmd");
        Extension.loadOnDemand(this, "list", "tcl.lang.ListCmd");
        Extension.loadOnDemand(this, "join", "tcl.lang.JoinCmd");
        Extension.loadOnDemand(this, "lappend", "tcl.lang.LappendCmd");
        Extension.loadOnDemand(this, "lindex", "tcl.lang.LindexCmd");
        Extension.loadOnDemand(this, "linsert", "tcl.lang.LinsertCmd");
        Extension.loadOnDemand(this, "llength", "tcl.lang.LlengthCmd");
        Extension.loadOnDemand(this, "lrange", "tcl.lang.LrangeCmd");
        Extension.loadOnDemand(this, "lreplace", "tcl.lang.LreplaceCmd");
        Extension.loadOnDemand(this, "lsearch", "tcl.lang.LsearchCmd");
        Extension.loadOnDemand(this, "lsort", "tcl.lang.LsortCmd");
        Extension.loadOnDemand(this, "namespace", "tcl.lang.NamespaceCmd");
        Extension.loadOnDemand(this, "open", "tcl.lang.OpenCmd");
        Extension.loadOnDemand(this, "package", "tcl.lang.PackageCmd");
        Extension.loadOnDemand(this, "proc", "tcl.lang.ProcCmd");
        Extension.loadOnDemand(this, "puts", "tcl.lang.PutsCmd");
        Extension.loadOnDemand(this, "pwd", "tcl.lang.PwdCmd");
        Extension.loadOnDemand(this, "read", "tcl.lang.ReadCmd");
        Extension.loadOnDemand(this, "regsub", "tcl.lang.RegsubCmd");
        Extension.loadOnDemand(this, "rename", "tcl.lang.RenameCmd");
        Extension.loadOnDemand(this, "return", "tcl.lang.ReturnCmd");
        Extension.loadOnDemand(this, "scan", "tcl.lang.ScanCmd");
        Extension.loadOnDemand(this, "seek", "tcl.lang.SeekCmd");
        Extension.loadOnDemand(this, "set", "tcl.lang.SetCmd");
        Extension.loadOnDemand(this, "socket", "tcl.lang.SocketCmd");
        Extension.loadOnDemand(this, "source", "tcl.lang.SourceCmd");
        Extension.loadOnDemand(this, "split", "tcl.lang.SplitCmd");
        Extension.loadOnDemand(this, "string", "tcl.lang.StringCmd");
        Extension.loadOnDemand(this, "subst", "tcl.lang.SubstCmd");
        Extension.loadOnDemand(this, "switch", "tcl.lang.SwitchCmd");
        Extension.loadOnDemand(this, "tell", "tcl.lang.TellCmd");
        Extension.loadOnDemand(this, "time", "tcl.lang.TimeCmd");
        Extension.loadOnDemand(this, "trace", "tcl.lang.TraceCmd");
        Extension.loadOnDemand(this, "unset", "tcl.lang.UnsetCmd");
        Extension.loadOnDemand(this, "update", "tcl.lang.UpdateCmd");
        Extension.loadOnDemand(this, "uplevel", "tcl.lang.UplevelCmd");
        Extension.loadOnDemand(this, "upvar", "tcl.lang.UpvarCmd");
        Extension.loadOnDemand(this, "variable", "tcl.lang.VariableCmd");
        Extension.loadOnDemand(this, "vwait", "tcl.lang.VwaitCmd");
        Extension.loadOnDemand(this, "while", "tcl.lang.WhileCmd");
        RegexpCmd.init(this);
        Extension.loadOnDemand(this, "jaclloadjava", "tcl.lang.JaclLoadJavaCmd");
        try {
            eval("package ifneeded java 1.2.6 jaclloadjava");
        } catch (TclException e) {
            System.out.println(getResult());
            e.printStackTrace();
            throw new TclRuntimeError("unexpected TclException: " + e);
        }
    }

    public void setAssocData(String name, AssocData data) {
        if (assocData == null) {
            assocData = new Hashtable();
        }
        assocData.put(name, data);
    }

    public void deleteAssocData(String name) {
        if (assocData == null) {
            return;
        }
        assocData.remove(name);
    }

    public AssocData getAssocData(String name) {
        if (assocData == null) {
            return null;
        } else {
            return (AssocData) assocData.get(name);
        }
    }

    public void backgroundError() {
        BgErrorMgr mgr = (BgErrorMgr) getAssocData("tclBgError");
        if (mgr == null) {
            mgr = new BgErrorMgr(this);
            setAssocData("tclBgError", mgr);
        }
        mgr.addBgError();
    }

    final TclObject setVar(TclObject nameObj, TclObject value, int flags) throws TclException {
        return Var.setVar(this, nameObj, value, (flags | TCL.LEAVE_ERR_MSG));
    }

    public final TclObject setVar(String name, TclObject value, int flags) throws TclException {
        return Var.setVar(this, name, value, (flags | TCL.LEAVE_ERR_MSG));
    }

    public final TclObject setVar(String name1, String name2, TclObject value, int flags) throws TclException {
        return Var.setVar(this, name1, name2, value, (flags | TCL.LEAVE_ERR_MSG));
    }

    final void setVar(String name, String strValue, int flags) throws TclException {
        Var.setVar(this, name, TclString.newInstance(strValue), (flags | TCL.LEAVE_ERR_MSG));
    }

    final void setVar(String name1, String name2, String strValue, int flags) throws TclException {
        Var.setVar(this, name1, name2, TclString.newInstance(strValue), (flags | TCL.LEAVE_ERR_MSG));
    }

    final TclObject getVar(TclObject nameObj, int flags) throws TclException {
        return Var.getVar(this, nameObj, (flags | TCL.LEAVE_ERR_MSG));
    }

    public final TclObject getVar(String name, int flags) throws TclException {
        return Var.getVar(this, name, (flags | TCL.LEAVE_ERR_MSG));
    }

    public final TclObject getVar(String name1, String name2, int flags) throws TclException {
        return Var.getVar(this, name1, name2, (flags | TCL.LEAVE_ERR_MSG));
    }

    final void unsetVar(TclObject nameObj, int flags) throws TclException {
        Var.unsetVar(this, nameObj, (flags | TCL.LEAVE_ERR_MSG));
    }

    public final void unsetVar(String name, int flags) throws TclException {
        Var.unsetVar(this, name, (flags | TCL.LEAVE_ERR_MSG));
    }

    public final void unsetVar(String name1, String name2, int flags) throws TclException {
        Var.unsetVar(this, name1, name2, (flags | TCL.LEAVE_ERR_MSG));
    }

    void traceVar(TclObject nameObj, VarTrace trace, int flags) throws TclException {
        Var.traceVar(this, nameObj, flags, trace);
    }

    public void traceVar(String name, VarTrace trace, int flags) throws TclException {
        Var.traceVar(this, name, flags, trace);
    }

    public void traceVar(String part1, String part2, VarTrace trace, int flags) throws TclException {
        Var.traceVar(this, part1, part2, flags, trace);
    }

    void untraceVar(TclObject nameObj, VarTrace trace, int flags) {
        Var.untraceVar(this, nameObj, flags, trace);
    }

    public void untraceVar(String name, VarTrace trace, int flags) {
        Var.untraceVar(this, name, flags, trace);
    }

    public void untraceVar(String part1, String part2, VarTrace trace, int flags) {
        Var.untraceVar(this, part1, part2, flags, trace);
    }

    public void createCommand(String cmdName, Command cmdImpl) {
        ImportRef oldRef = null;
        NamespaceCmd.Namespace ns;
        WrappedCommand cmd, refCmd;
        String tail;
        ImportedCmdData data;
        if (deleted) {
            return;
        }
        if (cmdName.indexOf("::") != -1) {
            NamespaceCmd.Namespace[] nsArr = new NamespaceCmd.Namespace[1];
            NamespaceCmd.Namespace[] dummyArr = new NamespaceCmd.Namespace[1];
            String[] tailArr = new String[1];
            NamespaceCmd.getNamespaceForQualName(this, cmdName, null, NamespaceCmd.CREATE_NS_IF_UNKNOWN, nsArr, dummyArr, dummyArr, tailArr);
            ns = nsArr[0];
            tail = tailArr[0];
            if ((ns == null) || (tail == null)) {
                return;
            }
        } else {
            ns = globalNs;
            tail = cmdName;
        }
        cmd = (WrappedCommand) ns.cmdTable.get(tail);
        if (cmd != null) {
            oldRef = cmd.importRef;
            cmd.importRef = null;
            deleteCommandFromToken(cmd);
            cmd = (WrappedCommand) ns.cmdTable.get(tail);
            if (cmd != null) {
                cmd.table.remove(cmd.hashKey);
            }
        }
        cmd = new WrappedCommand();
        ns.cmdTable.put(tail, cmd);
        cmd.table = ns.cmdTable;
        cmd.hashKey = tail;
        cmd.ns = ns;
        cmd.cmd = cmdImpl;
        cmd.deleted = false;
        if (oldRef != null) {
            cmd.importRef = oldRef;
            while (oldRef != null) {
                refCmd = oldRef.importedCmd;
                data = (ImportedCmdData) refCmd.cmd;
                data.realCmd = cmd;
                oldRef = oldRef.next;
            }
        }
        return;
    }

    String getCommandFullName(WrappedCommand cmd) {
        Interp interp = this;
        StringBuffer name = new StringBuffer();
        if (cmd != null) {
            if (cmd.ns != null) {
                name.append(cmd.ns.fullName);
                if (cmd.ns != interp.globalNs) {
                    name.append("::");
                }
            }
            if (cmd.table != null) {
                name.append(cmd.hashKey);
            }
        }
        return name.toString();
    }

    public int deleteCommand(String cmdName) {
        WrappedCommand cmd;
        try {
            cmd = NamespaceCmd.findCommand(this, cmdName, null, 0);
        } catch (TclException e) {
            throw new TclRuntimeError("unexpected TclException: " + e);
        }
        if (cmd == null) {
            return -1;
        }
        return deleteCommandFromToken(cmd);
    }

    protected int deleteCommandFromToken(WrappedCommand cmd) {
        if (cmd == null) {
            return -1;
        }
        ImportRef ref, nextRef;
        WrappedCommand importCmd;
        if (cmd.deleted) {
            cmd.table.remove(cmd.hashKey);
            cmd.table = null;
            cmd.hashKey = null;
            return 0;
        }
        cmd.deleted = true;
        if (cmd.cmd instanceof CommandWithDispose) {
            ((CommandWithDispose) cmd.cmd).disposeCmd();
        }
        for (ref = cmd.importRef; ref != null; ref = nextRef) {
            nextRef = ref.next;
            importCmd = ref.importedCmd;
            deleteCommandFromToken(importCmd);
        }
        if (cmd.table != null) {
            cmd.table.remove(cmd.hashKey);
            cmd.table = null;
            cmd.hashKey = null;
        }
        cmd.cmd = null;
        return 0;
    }

    protected void renameCommand(String oldName, String newName) throws TclException {
        Interp interp = this;
        String newTail;
        NamespaceCmd.Namespace cmdNs, newNs;
        WrappedCommand cmd;
        Hashtable table, oldTable;
        String hashKey, oldHashKey;
        cmd = NamespaceCmd.findCommand(interp, oldName, null, 0);
        if (cmd == null) {
            throw new TclException(interp, "can't " + (((newName == null) || (newName.length() == 0)) ? "delete" : "rename") + " \"" + oldName + "\": command doesn't exist");
        }
        cmdNs = cmd.ns;
        if ((newName == null) || (newName.length() == 0)) {
            deleteCommandFromToken(cmd);
            return;
        }
        NamespaceCmd.Namespace[] newNsArr = new NamespaceCmd.Namespace[1];
        NamespaceCmd.Namespace[] dummyArr = new NamespaceCmd.Namespace[1];
        String[] newTailArr = new String[1];
        NamespaceCmd.getNamespaceForQualName(interp, newName, null, NamespaceCmd.CREATE_NS_IF_UNKNOWN, newNsArr, dummyArr, dummyArr, newTailArr);
        newNs = newNsArr[0];
        newTail = newTailArr[0];
        if ((newNs == null) || (newTail == null)) {
            throw new TclException(interp, "can't rename to \"" + newName + "\": bad command name");
        }
        if (newNs.cmdTable.get(newTail) != null) {
            throw new TclException(interp, "can't rename to \"" + newName + "\": command already exists");
        }
        oldTable = cmd.table;
        oldHashKey = cmd.hashKey;
        newNs.cmdTable.put(newTail, cmd);
        cmd.table = newNs.cmdTable;
        cmd.hashKey = newTail;
        cmd.ns = newNs;
        if (cmd.cmd instanceof Procedure) {
            Procedure p = (Procedure) cmd.cmd;
            p.ns = cmd.ns;
        }
        oldTable.remove(oldHashKey);
        return;
    }

    public Command getCommand(String cmdName) {
        WrappedCommand cmd;
        try {
            cmd = NamespaceCmd.findCommand(this, cmdName, null, 0);
        } catch (TclException e) {
            throw new TclRuntimeError("unexpected TclException: " + e);
        }
        return ((cmd == null) ? null : cmd.cmd);
    }

    public static boolean commandComplete(String string) {
        return Parser.commandComplete(string, string.length());
    }

    public final TclObject getResult() {
        return m_result;
    }

    public final void setResult(TclObject r) {
        if (r == null) {
            throw new NullPointerException("Interp.setResult() called with null TclObject argument.");
        }
        if (m_result != m_nullResult) {
            m_result.release();
        }
        m_result = r;
        m_result.preserve();
    }

    public final void setResult(String r) {
        if (r == null) {
            resetResult();
        } else {
            setResult(TclString.newInstance(r));
        }
    }

    public final void setResult(int r) {
        setResult(TclInteger.newInstance(r));
    }

    public final void setResult(double r) {
        setResult(TclDouble.newInstance(r));
    }

    public final void setResult(boolean r) {
        setResult(TclBoolean.newInstance(r));
    }

    public final void resetResult() {
        if (m_result != m_nullResult) {
            m_result.release();
            m_result = m_nullResult;
        }
        errAlreadyLogged = false;
        errInProgress = false;
        errCodeSet = false;
    }

    void appendElement(String string) throws TclException {
        TclObject result;
        result = getResult();
        result.preserve();
        result = result.takeExclusive();
        TclList.append(this, result, TclString.newInstance(string));
        setResult(result.toString());
        result.release();
    }

    public void eval(String string, int flags) throws TclException {
        CharPointer script = new CharPointer(string);
        Parser.eval2(this, script.array, script.index, script.length(), flags);
    }

    public void eval(String script) throws TclException {
        eval(script, 0);
    }

    public void eval(TclObject tobj, int flags) throws TclException {
        eval(tobj.toString(), flags);
    }

    public void evalFile(String s) throws TclException {
        String fileContent;
        fileContent = readScriptFromFile(s);
        if (fileContent == null) {
            throw new TclException(this, "couldn't read file \"" + s + "\"");
        }
        String oldScript = scriptFile;
        scriptFile = s;
        try {
            pushDebugStack(s, 1);
            eval(fileContent, 0);
        } catch (TclException e) {
            if (e.getCompletionCode() == TCL.ERROR) {
                addErrorInfo("\n    (file \"" + s + "\" line " + errorLine + ")");
            }
            throw e;
        } finally {
            scriptFile = oldScript;
            popDebugStack();
        }
    }

    void evalURL(URL context, String s) throws TclException {
        String fileContent;
        fileContent = readScriptFromURL(context, s);
        if (fileContent == null) {
            throw new TclException(this, "cannot read URL \"" + s + "\"");
        }
        String oldScript = scriptFile;
        scriptFile = s;
        try {
            eval(fileContent, 0);
        } finally {
            scriptFile = oldScript;
        }
    }

    private String readScriptFromFile(String s) {
        File sourceFile;
        FileInputStream fs;
        try {
            sourceFile = FileUtil.getNewFileObj(this, s);
            fs = new FileInputStream(sourceFile);
        } catch (TclException e) {
            resetResult();
            return null;
        } catch (FileNotFoundException e) {
            return null;
        } catch (SecurityException sec_e) {
            return null;
        }
        try {
            byte charArray[] = new byte[fs.available()];
            fs.read(charArray);
            return new String(charArray);
        } catch (IOException e) {
            return null;
        } finally {
            closeInputStream(fs);
        }
    }

    private String readScriptFromURL(URL context, String s) {
        Object content = null;
        URL url;
        try {
            url = new URL(context, s);
        } catch (MalformedURLException e) {
            return null;
        }
        try {
            content = url.getContent();
        } catch (UnknownServiceException e) {
            Class jar_class;
            try {
                jar_class = Class.forName("java.net.JarURLConnection");
            } catch (Exception e2) {
                return null;
            }
            Object jar;
            try {
                jar = url.openConnection();
            } catch (IOException e2) {
                return null;
            }
            if (jar == null) {
                return null;
            }
            try {
                Method m = jar_class.getMethod("openConnection", ((java.lang.Class[]) null));
                content = m.invoke(jar, ((java.lang.Object[]) null));
            } catch (Exception e2) {
                return null;
            }
        } catch (IOException e) {
            return null;
        } catch (SecurityException e) {
            return null;
        }
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof InputStream) {
            InputStream fs = (InputStream) content;
            try {
                byte charArray[] = new byte[fs.available()];
                fs.read(charArray);
                return new String(charArray);
            } catch (IOException e2) {
                return null;
            } finally {
                closeInputStream(fs);
            }
        } else {
            return null;
        }
    }

    private void closeInputStream(InputStream fs) {
        try {
            fs.close();
        } catch (IOException e) {
            ;
        }
    }

    void evalResource(String resName) throws TclException {
        InputStream stream = null;
        try {
            stream = Interp.class.getResourceAsStream(resName);
        } catch (SecurityException e2) {
            System.err.println("evalResource: Ignoring SecurityException, " + "it is likely we are running in an applet: " + "cannot read resource \"" + resName + "\"" + e2);
            return;
        }
        if (stream == null) {
            throw new TclException(this, "cannot read resource \"" + resName + "\"");
        }
        try {
            if (System.getProperty("java.version").startsWith("1.2") && stream.getClass().getName().equals("java.util.zip.ZipFile$1")) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];
                int numRead;
                while ((numRead = stream.read(buffer, 0, buffer.length)) != -1) {
                    baos.write(buffer, 0, numRead);
                }
                eval(new String(baos.toByteArray()), 0);
            } else {
                int num = stream.available();
                byte[] byteArray = new byte[num];
                int offset = 0;
                while (num > 0) {
                    int readLen = stream.read(byteArray, offset, num);
                    offset += readLen;
                    num -= readLen;
                }
                eval(new String(byteArray), 0);
            }
        } catch (IOException e) {
            return;
        } finally {
            closeInputStream(stream);
        }
    }

    static BackSlashResult backslash(String s, int i, int len) {
        CharPointer script = new CharPointer(s.substring(0, len));
        script.index = i;
        return Parser.backslash(script.array, script.index);
    }

    public void setErrorCode(TclObject code) {
        try {
            setVar("errorCode", null, code, TCL.GLOBAL_ONLY);
            errCodeSet = true;
        } catch (TclException excp) {
        }
    }

    public void addErrorInfo(String message) {
        if (!errInProgress) {
            errInProgress = true;
            try {
                setVar("errorInfo", null, getResult().toString(), TCL.GLOBAL_ONLY);
            } catch (TclException e1) {
            }
            if (!errCodeSet) {
                try {
                    setVar("errorCode", null, "NONE", TCL.GLOBAL_ONLY);
                } catch (TclException e1) {
                }
            }
        }
        try {
            setVar("errorInfo", null, message, TCL.APPEND_VALUE | TCL.GLOBAL_ONLY);
        } catch (TclException e1) {
        }
    }

    protected int updateReturnInfo() {
        int code;
        code = returnCode;
        returnCode = TCL.OK;
        if (code == TCL.ERROR) {
            try {
                setVar("errorCode", null, (errorCode != null) ? errorCode : "NONE", TCL.GLOBAL_ONLY);
            } catch (TclException e) {
            }
            errCodeSet = true;
            if (errorInfo != null) {
                try {
                    setVar("errorInfo", null, errorInfo, TCL.GLOBAL_ONLY);
                } catch (TclException e) {
                }
                errInProgress = true;
            }
        }
        return code;
    }

    protected CallFrame newCallFrame(Procedure proc, TclObject[] objv) throws TclException {
        return new CallFrame(this, proc, objv);
    }

    protected CallFrame newCallFrame() {
        return new CallFrame(this);
    }

    File getWorkingDir() {
        if (workingDir == null) {
            try {
                String dirName = getVar("env", "HOME", 0).toString();
                workingDir = FileUtil.getNewFileObj(this, dirName);
            } catch (TclException e) {
                resetResult();
            }
            workingDir = new File(Util.tryGetSystemProperty("user.home", "."));
        }
        return workingDir;
    }

    void setWorkingDir(String dirName) throws TclException {
        File dirObj = FileUtil.getNewFileObj(this, dirName);
        try {
            dirObj = new File(dirObj.getCanonicalPath());
        } catch (IOException e) {
        }
        if (dirObj.isDirectory()) {
            workingDir = dirObj;
        } else {
            throw new TclException(this, "couldn't change working directory to \"" + dirObj.getName() + "\": no such file or directory");
        }
    }

    public Notifier getNotifier() {
        return notifier;
    }

    public final void pkgProvide(String name, String version) throws TclException {
        PackageCmd.pkgProvide(this, name, version);
    }

    public final String pkgRequire(String pkgname, String version, boolean exact) throws TclException {
        return PackageCmd.pkgRequire(this, pkgname, version, exact);
    }

    protected DebugInfo dbg;

    /**
 * Initialize the debugging information.
 * @return a DebugInfo object used by Interp in non-debugging mode.
 */
    protected DebugInfo initDebugInfo() {
        return new DebugInfo(null, 1);
    }

    /**
 * Add more more level at the top of the debug stack.
 *
 * @param fileName the filename for the new stack level
 * @param lineNumber the line number at which the execution of the
 *	   new stack level begins.
 */
    void pushDebugStack(String fileName, int lineNumber) {
    }

    /**
 * Remove the top-most level of the debug stack.
 */
    void popDebugStack() throws TclRuntimeError {
    }

    /**
 * Returns the name of the script file currently under execution.
 *
 * @return the name of the script file currently under execution.
 */
    String getScriptFile() {
        return dbg.fileName;
    }

    /**
 * Returns the line number where the given command argument begins. E.g, if
 * the following command is at line 10:
 *
 *	foo {a
 *      b } c
 *
 * getArgLine(0) = 10
 * getArgLine(1) = 10
 * getArgLine(2) = 11
 *
 * @param index specifies an argument.
 * @return the line number of the given argument.
 */
    int getArgLineNumber(int index) {
        return 0;
    }
}
