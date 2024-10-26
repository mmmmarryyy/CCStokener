package org.armedbear.lisp;

import static org.armedbear.lisp.Nil.NIL;
import static org.armedbear.lisp.Lisp.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public final class Load extends LispFile {

    public static final LispObject load(String filename) throws ConditionThrowable {
        final LispThread thread = LispThread.currentThread();
        return load(new Pathname(filename), filename, SymbolConstants.LOAD_VERBOSE.symbolValue(thread) != NIL, SymbolConstants.LOAD_PRINT.symbolValue(thread) != NIL, true);
    }

    private static final File findLoadableFile(final String filename, final String dir) {
        File file = new File(dir, filename);
        if (!file.isFile()) {
            String extension = getExtension(filename);
            if (extension == null) {
                File lispFile = IkvmSite.ikvmFileSafe(new File(dir, filename.concat(".lisp")));
                File abclFile = IkvmSite.ikvmFileSafe(new File(dir, filename.concat(".abcl")));
                if (lispFile.isFile() && abclFile.isFile()) {
                    if (abclFile.lastModified() > lispFile.lastModified()) {
                        return abclFile;
                    } else {
                        return lispFile;
                    }
                } else if (abclFile.isFile()) {
                    return abclFile;
                } else if (lispFile.isFile()) {
                    return lispFile;
                }
            }
        } else return file;
        return null;
    }

    public static final LispObject load(Pathname pathname, String filename, boolean verbose, boolean print, boolean ifDoesNotExist) throws ConditionThrowable {
        return load(pathname, filename, verbose, print, ifDoesNotExist, false);
    }

    public static final LispObject load(Pathname pathname, String filename, boolean verbose, boolean print, boolean ifDoesNotExist, boolean returnLastResult) throws ConditionThrowable {
        String dir = null;
        if (!Utilities.isFilenameAbsolute(filename)) {
            dir = coerceToPathname(SymbolConstants.DEFAULT_PATHNAME_DEFAULTS.symbolValue()).getNamestring();
        }
        String zipFileName = null;
        String zipEntryName = null;
        if (filename.startsWith("jar:file:")) {
            String s = new String(filename);
            s = s.substring(9);
            int index = s.lastIndexOf('!');
            if (index >= 0) {
                zipFileName = s.substring(0, index);
                zipEntryName = s.substring(index + 1);
                if (zipEntryName.length() > 0 && zipEntryName.charAt(0) == '/') zipEntryName = zipEntryName.substring(1);
                if (Utilities.isPlatformWindows) {
                    if (zipFileName.length() > 0 && zipFileName.charAt(0) == '/') zipFileName = zipFileName.substring(1);
                }
            }
        }
        File file = findLoadableFile(filename, dir);
        if (null == file && null == zipFileName) {
            if (ifDoesNotExist) return error(new FileError("File not found: " + filename, pathname)); else return NIL;
        }
        if (checkZipFile(file)) {
            if (".abcl".equals(getExtension(file.getPath()))) {
                filename = file.getPath();
            }
            zipFileName = file.getPath();
            zipEntryName = file.getName();
        }
        String truename = filename;
        ZipFile zipfile = null;
        boolean packedFASL = false;
        InputStream in = null;
        if (zipFileName != null) {
            try {
                zipfile = ZipCache.getZip(zipFileName);
            } catch (Throwable t) {
            }
            ZipEntry entry = zipfile.getEntry(zipEntryName);
            if (null == entry) {
                int index = zipEntryName.lastIndexOf('.');
                if (-1 == index) index = zipEntryName.length();
                zipEntryName = zipEntryName.substring(0, index).concat("._");
                entry = zipfile.getEntry(zipEntryName);
            }
            if (null == entry) {
                int index = zipEntryName.lastIndexOf('.');
                if (index == -1) index = zipEntryName.length();
                zipEntryName = zipEntryName.substring(0, index).concat(".abcl");
                entry = zipfile.getEntry(zipEntryName);
                if (entry != null) packedFASL = true;
            }
            if (null == entry) {
                int i = zipEntryName.lastIndexOf('.');
                if (i == -1) {
                    i = zipEntryName.length();
                }
                zipEntryName = zipEntryName.substring(0, i).concat(".lisp");
                entry = zipfile.getEntry(zipEntryName);
                if (entry == null) {
                    return error(new LispError("Failed to find " + zipEntryName + " in " + zipFileName + "."));
                }
            }
            if (null == entry) {
                return error(new FileError("Can't find zip file entry " + zipEntryName, pathname));
            }
            if (".abcl".equals(getExtension(zipEntryName))) {
                packedFASL = true;
            }
            if (packedFASL) {
                int i = zipEntryName.lastIndexOf('.');
                String subZipEntryName = zipEntryName.substring(0, i).concat("._");
                in = Utilities.getZippedZipEntryAsInputStream(zipfile, zipEntryName, subZipEntryName);
            } else {
                try {
                    in = zipfile.getInputStream(entry);
                } catch (IOException e) {
                    return error(new LispError(e.getMessage()));
                }
            }
        } else {
            try {
                in = new FileInputStream(file);
                truename = file.getCanonicalPath();
            } catch (FileNotFoundException e) {
                if (ifDoesNotExist) return error(new FileError("File not found: " + filename, pathname)); else return NIL;
            } catch (IOException e) {
                return error(new LispError(e.getMessage()));
            }
        }
        try {
            return loadFileFromStream(null, truename, new Stream(in, SymbolConstants.CHARACTER), verbose, print, false, returnLastResult);
        } catch (FaslVersionMismatch e) {
            FastStringBuffer sb = new FastStringBuffer("Incorrect fasl version: ");
            sb.append(truename);
            return error(new SimpleError(sb.toString()));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    return error(new LispError(e.getMessage()));
                }
            }
            if (zipfile != null) {
                try {
                    ZipCache.removeZip(zipfile.getName());
                } catch (IOException e) {
                    return error(new LispError(e.getMessage()));
                }
            }
        }
    }

    public static final LispObject loadSystemFile(String filename) throws ConditionThrowable {
        final LispThread thread = LispThread.currentThread();
        return loadSystemFile(filename, SymbolConstants.LOAD_VERBOSE.symbolValue(thread) != NIL, SymbolConstants.LOAD_PRINT.symbolValue(thread) != NIL, false);
    }

    public static final LispObject loadSystemFile(String filename, boolean auto) throws ConditionThrowable {
        LispThread thread = LispThread.currentThread();
        if (auto) {
            SpecialBinding lastSpecialBinding = thread.lastSpecialBinding;
            thread.bindSpecial(SymbolConstants.CURRENT_READTABLE, STANDARD_READTABLE.symbolValue(thread));
            thread.bindSpecial(SymbolConstants._PACKAGE_, PACKAGE_CL_USER);
            try {
                return loadSystemFile(filename, _AUTOLOAD_VERBOSE_.symbolValue(thread) != NIL, SymbolConstants.LOAD_PRINT.symbolValue(thread) != NIL, auto);
            } finally {
                thread.lastSpecialBinding = lastSpecialBinding;
            }
        } else {
            return loadSystemFile(filename, SymbolConstants.LOAD_VERBOSE.symbolValue(thread) != NIL, SymbolConstants.LOAD_PRINT.symbolValue(thread) != NIL, auto);
        }
    }

    public static final LispObject loadSystemFile(final String filename, boolean verbose, boolean print, boolean auto) throws ConditionThrowable {
        final int ARRAY_SIZE = 2;
        String[] candidates = new String[ARRAY_SIZE];
        final String extension = getExtension(filename);
        if (extension == null) {
            candidates[0] = filename + '.' + COMPILE_FILE_TYPE;
            candidates[1] = filename.concat(".lisp");
        } else if (extension.equals(".abcl")) {
            candidates[0] = filename;
            candidates[1] = filename.substring(0, filename.length() - 5).concat(".lisp");
        } else candidates[0] = filename;
        InputStream in = null;
        Pathname pathname = null;
        String truename = null;
        for (int i = 0; i < ARRAY_SIZE; i++) {
            String s = candidates[i];
            if (s == null) break;
            ZipFile zipfile = null;
            final String dir = Site.getLispHome();
            try {
                if (dir != null) {
                    File file = IkvmSite.ikvmFileSafe(new File(dir, s));
                    if (file.isFile()) {
                        String ext = getExtension(s);
                        if (ext.equalsIgnoreCase(".abcl")) {
                            try {
                                zipfile = ZipCache.getZip(file.getPath());
                                String name = file.getName();
                                int index = name.lastIndexOf('.');
                                Debug.assertTrue(index >= 0);
                                name = name.substring(0, index).concat("._");
                                ZipEntry entry = zipfile.getEntry(name);
                                if (entry != null) {
                                    in = zipfile.getInputStream(entry);
                                    truename = file.getCanonicalPath();
                                }
                            } catch (ZipException e) {
                            } catch (Throwable t) {
                                Debug.trace(t);
                                in = null;
                            }
                        }
                        if (in == null) {
                            try {
                                in = new FileInputStream(file);
                                truename = file.getCanonicalPath();
                            } catch (IOException e) {
                                in = null;
                            }
                        }
                    }
                } else {
                    URL url = Lisp.class.getResource(s);
                    if (url != null) {
                        try {
                            in = url.openStream();
                            String proto = url.getProtocol();
                            if ("jar".equals(proto) || "ikvmres".equals(proto)) pathname = new Pathname(url);
                            truename = getPath(url);
                        } catch (IOException e) {
                            in = null;
                        }
                    }
                }
                if (in != null) {
                    final LispThread thread = LispThread.currentThread();
                    final SpecialBinding lastSpecialBinding = thread.lastSpecialBinding;
                    thread.bindSpecial(_WARN_ON_REDEFINITION_, NIL);
                    try {
                        return loadFileFromStream(pathname, truename, new Stream(in, SymbolConstants.CHARACTER), verbose, print, auto);
                    } catch (FaslVersionMismatch e) {
                        FastStringBuffer sb = new FastStringBuffer("; Incorrect fasl version: ");
                        sb.append(truename);
                        System.err.println(sb.toString());
                    } finally {
                        thread.lastSpecialBinding = lastSpecialBinding;
                        try {
                            in.close();
                        } catch (IOException e) {
                            return error(new LispError(e.getMessage()));
                        }
                    }
                }
            } finally {
                if (zipfile != null) {
                    try {
                        ZipCache.removeZip(zipfile.getName());
                    } catch (IOException e) {
                        return error(new LispError(e.getMessage()));
                    }
                }
            }
        }
        return error(new LispError("File not found: " + filename));
    }

    static final Symbol _FASL_VERSION_ = exportConstant("*FASL-VERSION*", PACKAGE_SYS, Fixnum.makeFixnum(32));

    /**
     * This variable gets bound to a package with no name in which the
     * reader can intern its uninterned symbols.
     *
     */
    public static final Symbol _FASL_ANONYMOUS_PACKAGE_ = internSpecial("*FASL-ANONYMOUS-PACKAGE*", PACKAGE_SYS, NIL);

    private static final Primitive INIT_FASL = new Primitive("init-fasl", PACKAGE_SYS, true, "&key version") {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            if (first == Keyword.VERSION) {
                if (second.eql(_FASL_VERSION_.getSymbolValue())) {
                    final LispThread thread = LispThread.currentThread();
                    thread.bindSpecial(_FASL_ANONYMOUS_PACKAGE_, NIL);
                    thread.bindSpecial(_SOURCE_, NIL);
                    return faslLoadStream(thread);
                }
            }
            throw new FaslVersionMismatch(second);
        }
    };

    private static final LispObject loadFileFromStream(LispObject pathname, String truename, Stream in, boolean verbose, boolean print, boolean auto) throws ConditionThrowable {
        return loadFileFromStream(pathname, truename, in, verbose, print, auto, false);
    }

    private static final LispObject loadFileFromStream(LispObject pathname, String truename, Stream in, boolean verbose, boolean print, boolean auto, boolean returnLastResult) throws ConditionThrowable {
        if (false && IkvmSite.isIKVMDll()) {
            String pstring = "" + pathname;
            try {
                pstring = pathname == null ? "NULL" : pathname.writeToString();
            } catch (Exception e) {
            }
            IkvmSite.printDebug("loadFileFromStream=" + pstring + " truename=" + truename + " in" + in);
        }
        long start = System.currentTimeMillis();
        final LispThread thread = LispThread.currentThread();
        final SpecialBinding lastSpecialBinding = thread.lastSpecialBinding;
        thread.bindSpecialToCurrentValue(SymbolConstants.CURRENT_READTABLE);
        thread.bindSpecialToCurrentValue(SymbolConstants._PACKAGE_);
        int loadDepth = _LOAD_DEPTH_.symbolValue(thread).intValue();
        thread.bindSpecial(_LOAD_DEPTH_, Fixnum.makeFixnum(++loadDepth));
        thread.bindSpecialToCurrentValue(_SPEED_);
        thread.bindSpecialToCurrentValue(_SPACE_);
        thread.bindSpecialToCurrentValue(_SAFETY_);
        thread.bindSpecialToCurrentValue(_DEBUG_);
        thread.bindSpecialToCurrentValue(_EXPLAIN_);
        final String prefix = getLoadVerbosePrefix(loadDepth);
        try {
            if (pathname == null && truename != null) pathname = Pathname.parseNamestring(truename);
            thread.bindSpecial(SymbolConstants.LOAD_PATHNAME, pathname != null ? pathname : NIL);
            thread.bindSpecial(SymbolConstants.LOAD_TRUENAME, pathname != null ? pathname : NIL);
            thread.bindSpecial(_SOURCE_, pathname != null ? pathname : NIL);
            if (verbose) {
                Stream out = getStandardOutput();
                out.freshLine();
                out._writeString(prefix);
                out._writeString(auto ? " Autoloading " : " Loading ");
                out._writeString(truename != null ? truename : "stream");
                out._writeLine(" ...");
                out._finishOutput();
                LispObject result = loadStream(in, print, thread, returnLastResult);
                long elapsed = System.currentTimeMillis() - start;
                out.freshLine();
                out._writeString(prefix);
                out._writeString(auto ? " Autoloaded " : " Loaded ");
                out._writeString(truename != null ? truename : "stream");
                out._writeString(" (");
                out._writeString(String.valueOf(((float) elapsed) / 1000));
                out._writeLine(" seconds)");
                out._finishOutput();
                return result;
            } else return loadStream(in, print, thread, returnLastResult);
        } finally {
            thread.lastSpecialBinding = lastSpecialBinding;
        }
    }

    public static String getLoadVerbosePrefix(int loadDepth) {
        FastStringBuffer sb = new FastStringBuffer(";");
        for (int i = loadDepth - 1; i-- > 0; ) sb.append(' ');
        return sb.toString();
    }

    private static final LispObject loadStream(Stream in, boolean print, LispThread thread) throws ConditionThrowable {
        return loadStream(in, print, thread, false);
    }

    private static final LispObject loadStream(Stream in, boolean print, LispThread thread, boolean returnLastResult) throws ConditionThrowable {
        SpecialBinding lastSpecialBinding = thread.lastSpecialBinding;
        thread.bindSpecial(_LOAD_STREAM_, in);
        SpecialBinding sourcePositionBinding = new SpecialBinding(_SOURCE_POSITION_, Fixnum.ZERO, thread.lastSpecialBinding);
        thread.lastSpecialBinding = sourcePositionBinding;
        try {
            final Environment env = new Environment();
            LispObject result = NIL;
            while (true) {
                sourcePositionBinding.value = Fixnum.makeFixnum(in.getOffset());
                LispObject obj = in.read(false, EOF, false, thread);
                if (obj == EOF) break;
                result = eval(obj, env, thread);
                if (print) {
                    Stream out = checkCharacterOutputStream(SymbolConstants.STANDARD_OUTPUT.symbolValue(thread));
                    out._writeLine(result.writeToString());
                    out._finishOutput();
                }
            }
            if (returnLastResult) {
                return result;
            } else {
                return T;
            }
        } finally {
            thread.lastSpecialBinding = lastSpecialBinding;
        }
    }

    static final LispObject faslLoadStream(LispThread thread) throws ConditionThrowable {
        Stream in = (Stream) _LOAD_STREAM_.symbolValue(thread);
        final Environment env = new Environment();
        final SpecialBinding lastSpecialBinding = thread.lastSpecialBinding;
        LispObject result = NIL;
        try {
            thread.bindSpecial(_FASL_ANONYMOUS_PACKAGE_, new LispPackage());
            while (true) {
                LispObject obj = in.faslRead(false, EOF, true, thread);
                if (obj == EOF) break;
                result = eval(obj, env, thread);
            }
        } finally {
            thread.lastSpecialBinding = lastSpecialBinding;
        }
        return result;
    }

    private static final String getExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0) return null;
        if (index < filename.lastIndexOf(File.separatorChar)) return null;
        return filename.substring(index);
    }

    private static final String getPath(URL url) {
        if (url != null) {
            String path;
            try {
                path = URLDecoder.decode(url.getPath(), "UTF-8");
            } catch (java.io.UnsupportedEncodingException uee) {
                path = null;
            }
            if (path != null) {
                if (Utilities.isPlatformWindows) {
                    if (path.length() > 0 && path.charAt(0) == '/') path = path.substring(1);
                }
                return path;
            }
        }
        return null;
    }

    private static final boolean checkZipFile(File file) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] bytes = new byte[4];
            int bytesRead = in.read(bytes);
            return (bytesRead == 4 && bytes[0] == 0x50 && bytes[1] == 0x4b && bytes[2] == 0x03 && bytes[3] == 0x04);
        } catch (Throwable t) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    private static final Primitive _LOAD = new Primitive("%load", PACKAGE_SYS, false, "filespec verbose print if-does-not-exist") {

        @Override
        public LispObject execute(LispObject filespec, LispObject verbose, LispObject print, LispObject ifDoesNotExist) throws ConditionThrowable {
            return load(filespec, verbose, print, ifDoesNotExist, NIL);
        }
    };

    private static final Primitive _LOAD_RETURNING_LAST_RESULT = new Primitive("%load-returning-last-result", PACKAGE_SYS, false, "filespec verbose print if-does-not-exist") {

        @Override
        public LispObject execute(LispObject filespec, LispObject verbose, LispObject print, LispObject ifDoesNotExist) throws ConditionThrowable {
            return load(filespec, verbose, print, ifDoesNotExist, T);
        }
    };

    static final LispObject load(LispObject filespec, LispObject verbose, LispObject print, LispObject ifDoesNotExist, LispObject returnLastResult) throws ConditionThrowable {
        if (filespec instanceof Stream) {
            if (((Stream) filespec).isOpen()) {
                LispObject pathname;
                if (filespec instanceof FileStream) pathname = ((FileStream) filespec).getPathname(); else pathname = NIL;
                String truename;
                if (pathname instanceof Pathname) truename = ((Pathname) pathname).getNamestring(); else truename = null;
                return loadFileFromStream(pathname, truename, (Stream) filespec, verbose != NIL, print != NIL, false, returnLastResult != NIL);
            }
        }
        Pathname pathname = coerceToPathname(filespec);
        if (pathname instanceof LogicalPathname) pathname = LogicalPathname.translateLogicalPathname((LogicalPathname) pathname);
        return load(pathname, pathname.getNamestring(), verbose != NIL, print != NIL, ifDoesNotExist != NIL, returnLastResult != NIL);
    }

    private static final Primitive LOAD_SYSTEM_FILE = new Primitive("load-system-file", PACKAGE_SYS, true) {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            final LispThread thread = LispThread.currentThread();
            return loadSystemFile(arg.getStringValue(), SymbolConstants.LOAD_VERBOSE.symbolValue(thread) != NIL, SymbolConstants.LOAD_PRINT.symbolValue(thread) != NIL, false);
        }
    };

    private static class FaslVersionMismatch extends Error {

        private final LispObject version;

        public FaslVersionMismatch(LispObject version) {
            this.version = version;
        }

        public LispObject getVersion() {
            return version;
        }
    }
}
