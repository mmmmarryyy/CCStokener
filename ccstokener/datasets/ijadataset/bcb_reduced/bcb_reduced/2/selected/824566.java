package edu.whitman.halfway.util;

import java.util.*;
import org.apache.log4j.*;
import java.io.*;
import java.net.URL;
import org.apache.oro.text.perl.Perl5Util;
import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix2D;

public final class FileUtil {

    protected static FileFilter imageFileFilter = null;

    protected static FileFilter imageDirFileFilter = null;

    protected static FileFilter directoryFilter = null;

    protected static FileFilter regularFileFilter = null;

    public static final String fileSep = System.getProperty("file.separator");

    private static Logger log = Logger.getLogger(FileUtil.class.getName());

    private static Perl5Util perl5Util = new Perl5Util();

    public static String insertBeforeExtension(String filename, String addition) {
        int lastDot = filename.lastIndexOf('.');
        return filename.substring(0, lastDot) + addition + filename.substring(lastDot, filename.length());
    }

    /** Does not include the dot (.) */
    public static String getExtension(File file) {
        String f = file.getPath();
        int lastDot = f.lastIndexOf('.');
        int lastSep = f.lastIndexOf(fileSep);
        if (lastSep > lastDot) return null;
        return f.substring(lastDot + 1);
    }

    public static File makeNumFilename(String prefix, String suffix, File dir) {
        return makeNumFilename(prefix, suffix, dir, false);
    }

    public static File makeNumFilename(String prefix, String suffix, File dir, boolean tryNoNum) {
        if (tryNoNum) {
            File f = new File(dir, prefix + suffix);
            if (!f.exists()) return f;
        }
        File f = new File(dir, prefix + "1" + suffix);
        int i = 2;
        while (f.exists()) {
            f = new File(dir, prefix + i + suffix);
            i++;
        }
        return f;
    }

    public static File makeNumFilename(String prefix, File dir) {
        return makeNumFilename(prefix + ".", "", dir);
    }

    /** Makes a filename starting with prefix that has the date as a
     * postfix, and a number to ensure it is not the name of any file
     * already in dir. */
    public static String makeDateFilename(String prefix, String ext, File dir) {
        int counter = 0;
        while (true) {
            String fileName = prefix + "_" + StringUtil.timeForFile() + ((counter == 0) ? "" : counter + "") + ext;
            File f = new File(dir, fileName);
            if (!f.exists()) return fileName;
            counter++;
        }
    }

    /** Returns a new file, or null.  First, checks to see if fileName
     * is file relative to parentDir.  If so, returns that file.
     * Otherwise, checks if fileName is an absolute path to an
     * existing file.  If so, returns new File(fileName).  Otherwise,
     * returns null.

     XXX I've had some weird problems with this.  Currently works for
     relative files, not sure about actual absolute files.
     
    */
    public static File getValidFile(File parentDir, String fileName) {
        File oldCWD = getCWD();
        if (log.isDebugEnabled()) log.debug("Current cwd is " + oldCWD);
        setCWD(parentDir);
        if (log.isDebugEnabled()) log.debug("Now it is " + getCWD());
        File f = new File(fileName);
        File fAbs = f.getAbsoluteFile();
        File rval = null;
        if (log.isDebugEnabled()) log.debug("Get absoluteFile() = " + fAbs);
        if (!f.exists() && fAbs.exists()) {
        }
        if (fAbs.exists()) {
            if (log.isDebugEnabled()) log.debug("File " + fileName + " exists as " + fAbs);
            rval = fAbs;
        } else {
            if (log.isDebugEnabled()) log.debug("File " + f + " does not exist.");
        }
        setCWD(oldCWD);
        return rval;
    }

    public static File createTempDir(String prefix, String suffix) {
        File tempDirectory;
        try {
            do {
                tempDirectory = File.createTempFile(prefix, suffix);
                tempDirectory.delete();
            } while (!tempDirectory.mkdir());
        } catch (IOException ioe) {
            log.error("IOException: " + ioe, ioe);
            return null;
        }
        return tempDirectory;
    }

    public static boolean writeToFile(File f, double[] data) {
        try {
            Writer out = new FileWriter(f);
            for (int i = 0; i < data.length; i++) {
                out.write(String.format("%.15g%n", data[i]));
            }
            out.close();
        } catch (IOException e) {
            log.warn("Error writing array to file " + f, e);
            return false;
        }
        return true;
    }

    public static boolean writeObjectToFile(File f, Serializable object) {
        try {
            FileOutputStream ostream = new FileOutputStream(f);
            ObjectOutputStream p = new ObjectOutputStream(new BufferedOutputStream(ostream));
            p.writeObject(object);
            p.flush();
            ostream.close();
        } catch (IOException e) {
            log.warn("Error writing object " + object + " to file " + f, e);
            return false;
        }
        return true;
    }

    public static boolean writeStringToFile(File f, String s) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(f));
            w.write(s);
            w.flush();
            w.close();
        } catch (IOException e) {
            log.warn("Error writing string \"" + s + "\" to file " + f, e);
            return false;
        }
        return true;
    }

    /** writes a string to a file in a format easily importable by matlab.*/
    public static boolean writeToFileForMatlab(File f, DoubleMatrix2D m) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(f));
            int nr = m.rows();
            int nc = m.columns();
            for (int r = 0; r < nr; r++) {
                for (int c = 0; c < nc; c++) {
                    w.write(m.getQuick(r, c) + " ");
                }
                w.write(StringUtil.newline);
            }
            w.flush();
            w.close();
        } catch (IOException e) {
            log.warn("Error writing matrix \"" + m.toStringShort() + "\" to file " + f, e);
            return false;
        }
        return true;
    }

    /** writes a string to a file in a format easily importable by matlab*/
    public static boolean writeToFileForMatlabSparse(File f, DoubleMatrix2D m) {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(f));
            int nr = m.rows();
            int nc = m.columns();
            for (int r = 0; r < nr; r++) {
                for (int c = 0; c < nc; c++) {
                    double v = m.getQuick(r, c);
                    if (v != 0 || (r == (nr - 1) && c == (nc - 1))) {
                        w.write((r + 1) + " " + (c + 1) + " " + v + StringUtil.newline);
                    }
                }
            }
            w.flush();
            w.close();
        } catch (IOException e) {
            log.warn("Error writing matrix \"" + m.toStringShort() + "\" to file " + f, e);
            return false;
        }
        return true;
    }

    public static Object readObjectFromFile(File f) {
        try {
            FileInputStream istream = new FileInputStream(f);
            ObjectInputStream p = new ObjectInputStream(istream);
            Object object = p.readObject();
            istream.close();
            return object;
        } catch (IOException e) {
            log.warn("Error reading object from file " + f, e);
            return null;
        } catch (ClassNotFoundException e) {
            log.error("Could not find class to load object from file " + f, e);
            return null;
        }
    }

    /** Calls FileFunction on all files in the directory that match
     * the given filter. */
    public static void dirWalk(File dir, FileFunction func, FileFilter filter) {
        File[] files = dir.listFiles(filter);
        for (int i = 0; i < files.length; i++) {
            if (log.isDebugEnabled()) log.debug("Processing file " + files[i]);
            func.exec(files[i]);
        }
    }

    /**
       This function visits all files in dir, and runs function func
       on them if and only if filter accepts the file.  It then
       recursively processes all subdirectories of dir.
    */
    public static void recursiveDirWalk(File dir, FileFunction func, FileFilter filter) {
        dirWalk(dir, func, filter);
        File[] dirs = dir.listFiles(new DirectoryFilter());
        for (int i = 0; i < dirs.length; i++) {
            if (log.isDebugEnabled()) log.debug("Processing dir " + dirs[i]);
            recursiveDirWalk(dirs[i], func, filter);
        }
    }

    /** Recursively deletes the given directory.  Be very carefull
     * using this! It seems to delete but not follow symlinks on my
     * system, but this behavior isn't guaranteed as the documentation
     * in File.java is not good enough.*/
    public static boolean recursiveDelete(File dir) {
        File[] files = dir.listFiles();
        boolean success = true;
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    if (!files[i].delete()) success = false;
                } else {
                    if (!files[i].delete()) {
                        if (!recursiveDelete(files[i])) success = false;
                        if (!files[i].delete()) success = false;
                    }
                }
            }
            return success;
        }
        log.warn("No files in " + dir);
        return false;
    }

    public static File getCWD() {
        return new File(System.getProperty("user.dir"));
    }

    public static void setCWD(File cwd) {
        System.setProperty("user.dir", cwd.getPath());
    }

    /** Tries to copy the source file to the target file.  The target
     * cannot exist, and the source must exist.  Returns false if for
     * any reason copy fails. */
    public static boolean copy(File source, File target) {
        return copy(source, target, false);
    }

    /** Tries to copy the source file to the target file.  The target
     * cannot exist, and the source must exist.  Returns false if for
     * any reason copy fails. */
    public static boolean copy(File source, File target, boolean owrite) {
        if (!source.exists()) {
            log.error("Invalid input to copy: source " + source + "doesn't exist");
            return false;
        } else if (!source.isFile()) {
            log.error("Invalid input to copy: source " + source + "isn't a file.");
            return false;
        } else if (target.exists() && !owrite) {
            log.error("Invalid input to copy: target " + target + " exists.");
            return false;
        }
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target));
            byte buffer[] = new byte[1024];
            int read = -1;
            while ((read = in.read(buffer, 0, 1024)) != -1) out.write(buffer, 0, read);
            out.flush();
            out.close();
            in.close();
            return true;
        } catch (IOException e) {
            log.error("Copy failed: ", e);
            return false;
        }
    }

    /**
       equivalent to readFromBufferedReader(new BufferedReader(new FileReader(f)));
    */
    public static StringBuffer readFromFile(File f) {
        try {
            return readFromBufferedReader(new BufferedReader(new FileReader(f)));
        } catch (IOException e) {
            log.error("readFromURL: IOException opening file " + f, e);
            return null;
        }
    }

    /** Reads the contents of the given BufferedReader if possible and returns
        contents as a string, otherwise returnining null and logging
        errors. */
    public static StringBuffer readFromBufferedReader(BufferedReader reader) {
        try {
            String line;
            StringBuffer contents = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                contents.append(line);
                contents.append(StringUtil.newline);
            }
            return contents;
        } catch (IOException e) {
            log.error("readFromBufferedReader: IOException reading from " + reader, e);
            return null;
        }
    }

    /** Reads the contents of the given BufferedReader by making each
        line an entry in the returned array of Strings, otherwise
        returnining null and logging errors.  Skips lines of only
        whitespace.  Warning: this is slow and memory intensize, only
        for small files.  */
    public static String[] readToArray(BufferedReader reader) {
        try {
            String line;
            ArrayList<String> lineList = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                if ((line.length() != 0) && (line.trim().length() != 0)) {
                    lineList.add(line);
                }
            }
            return (String[]) lineList.toArray(new String[lineList.size()]);
        } catch (IOException e) {
            log.error("readToArray: IOException reading from " + reader, e);
            return null;
        }
    }

    /**
       equivalent to readToArray(new BufferedReader(new FileReader(f))
    */
    public static String[] readToArray(File f) {
        try {
            return readToArray(new BufferedReader(new FileReader(f)));
        } catch (IOException e) {
            log.error("readToArray: IOException opening file " + f, e);
            return null;
        }
    }

    public static DoubleArrayList readToDoubleArray(File f) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            DoubleArrayList list = new DoubleArrayList();
            while ((line = reader.readLine()) != null) {
                list.add(Double.parseDouble(line));
            }
            return list;
        } catch (IOException e) {
            log.error("readDoubles: IOException reading from " + f, e);
            return null;
        } catch (NumberFormatException e) {
            log.error("readToArray: NumberFormatException reading from " + f, e);
            return null;
        }
    }

    /** this doesn't really belong here, but oh well. */
    public static StringBuffer readFromURL(URL url) {
        if (url == null) {
            log.error("Called readFromURL with null url, returning null.");
            return null;
        }
        try {
            return readFromBufferedReader(new BufferedReader(new InputStreamReader(url.openStream())));
        } catch (IOException e) {
            log.error("readFromURL: IOException opening URL " + url, e);
            return null;
        }
    }

    /** Gets a relative path to target for a file sitting in crntDir.
    May return null if a getCanonicalPath fails internally or if
    there is some other error.*/
    public static String makeRelativePath(File crntDir, File target) {
        return makeRelativePath(crntDir, target, File.separator);
    }

    /** Gets a relative path to target for a file sitting in crntDir.
      May return null if a getCanonicalPath fails internally or if
      there is some other error.*/
    public static String makeRelativePath(File crntDir, File target, String sep) {
        if (log.isDebugEnabled()) log.debug("makeRealtivePath" + StringUtil.newline + "crntDir=" + crntDir + StringUtil.newline + "target=" + target + StringUtil.newline);
        File absCrnt = null;
        File absTarg = null;
        try {
            absCrnt = crntDir.getCanonicalFile();
            absTarg = target.getCanonicalFile();
        } catch (IOException e) {
            log.error("Couldn't get canonical file", e);
            return null;
        }
        if (log.isDebugEnabled()) log.debug("makeRealtivePath" + StringUtil.newline + "abs crntDir=" + absCrnt + StringUtil.newline + "abs target=" + absTarg + StringUtil.newline);
        List clist = splitFile(absCrnt);
        List tlist = splitFile(absTarg);
        Iterator crnt = clist.iterator();
        Iterator targ = tlist.iterator();
        StringBuffer relPath = new StringBuffer();
        while (crnt.hasNext() && targ.hasNext()) {
            String c = (String) crnt.next();
            String t = (String) targ.next();
            if (c.equals(t)) {
                if (log.isDebugEnabled()) log.debug("Removing common dir: " + c);
                crnt.remove();
                targ.remove();
            } else {
                break;
            }
        }
        crnt = clist.iterator();
        targ = tlist.iterator();
        while (crnt.hasNext()) {
            String s = (String) crnt.next();
            if (log.isDebugEnabled()) log.debug("Adding a .. for " + s);
            relPath.append("..");
            relPath.append(sep);
        }
        while (targ.hasNext()) {
            String s = (String) targ.next();
            if (log.isDebugEnabled()) log.debug("Adding path " + s);
            relPath.append(s);
            if (targ.hasNext()) {
                relPath.append(sep);
            }
        }
        if (log.isDebugEnabled()) log.debug("Generated path = " + relPath);
        return relPath.toString();
    }

    /** Determines if the files are equal by using equal on the two
        files.  Comparing two null files returns true.*/
    public static boolean isEqual(File one, File two) {
        if ((one == null) && (two == null)) return true;
        if ((one == null) || (two == null)) return false;
        return one.equals(two);
    }

    /** Determines if the files are equal by comparing canonical
        files.  Returns deflt if an error occurs on
        getCanonicalFile.  Comparing two null files returns true.*/
    public static boolean isEqual(File one, File two, boolean deflt) {
        try {
            if (one != null && two != null) {
                one = one.getCanonicalFile();
                two = two.getCanonicalFile();
                return one.equals(two);
            }
            return (one == null && two == null);
        } catch (IOException e) {
            log.warn("Couldn't do canonical file comparison. returning default.");
            return deflt;
        }
    }

    /** Splits the file on the filename seperator, returning a List of
        the corresponding names.  That is, "/usr/local/bin/" becomes
        ["usr", "local", "bin"].  There will be no empty or null
        strings in the output.  

        <p>XXX Java's get absolute path does not appear to remove
        "."'s from a path, ie, a File foo = "./output/", which is
        really, "/home/mcmahan/output/", gets an absolute path of
        "/home/mcmahan/./output/", which may work, but doesn't do what
        we want here.  Using get Canonical Path first seems to be a
        solution.
    */
    public static List splitFile(String f) {
        List results = new LinkedList();
        String sep = File.separator;
        if (sep.equals("\\")) sep = "\\\\";
        String pattern = "m#" + sep + "#";
        perl5Util.split(results, pattern, f);
        Iterator iter = results.iterator();
        while (iter.hasNext()) {
            String s = (String) iter.next();
            if ((s == null) || (s.equals(""))) {
                iter.remove();
            }
        }
        return results;
    }

    public static String[] splitFileToArray(String f) {
        List list = splitFile(f);
        return (String[]) list.toArray(new String[0]);
    }

    /**
       Equivalent to splitFile(f.getPath());
       See comments for splitFile(String f)
    */
    public static List splitFile(File f) {
        return splitFile(f.getPath());
    }

    public static FileFilter getDirectoryFilter() {
        if (directoryFilter == null) {
            directoryFilter = new DirectoryFilter();
        }
        return directoryFilter;
    }

    /** extinsion is a array of acceptable file extinsions, with the
     * dot.  For example, for jpeg files you could use
     * ".{jpg. .jpeg}".  Case insensitive.  Does not check if the file
     * actually exists.*/
    public static FileFilter getExtensionFilter(String[] extension) {
        return new ExtensionFilter(extension);
    }

    /** extinsion is an extinsion with the dot.  For example, for text
     * files you could use ".txt".  Case insensitive.  Does not check
     * if the file actually exists.*/
    public static FileFilter getExtensionFilter(String extension) {
        String[] ext = new String[1];
        ext[0] = extension;
        return new ExtensionFilter(ext);
    }

    static class ExtensionFilter implements FileFilter {

        String[] ext;

        public ExtensionFilter(String[] rawExt) {
            ext = new String[rawExt.length];
            for (int i = 0; i < rawExt.length; i++) {
                ext[i] = rawExt[i].toLowerCase();
            }
        }

        public boolean accept(File file) {
            boolean valid = false;
            String name = file.getName();
            if (name == null || name.equals("")) {
                valid = false;
            } else {
                for (int i = 0; i < ext.length; i++) {
                    if (name.toLowerCase().endsWith(ext[i])) {
                        valid = true;
                    }
                }
            }
            return valid;
        }
    }

    public static FileFilter getImageFileFilter() {
        if (imageFileFilter == null) {
            imageFileFilter = new ValidImageFileFilter();
        }
        return imageFileFilter;
    }

    public static FileFilter getImageDirFileFilter() {
        if (imageDirFileFilter == null) {
            imageDirFileFilter = new ValidImageDirFileFilter();
        }
        return imageDirFileFilter;
    }

    public static FileFilter getRegularFileFilter() {
        if (regularFileFilter == null) {
            regularFileFilter = new RegularFileFilter();
        }
        return regularFileFilter;
    }

    private static class DirectoryFilter implements FileFilter {

        public boolean accept(File file) {
            return file.isDirectory();
        }
    }

    private static class RegularFileFilter implements FileFilter {

        public boolean accept(File file) {
            boolean isFile = file.isFile();
            if (isFile) {
                if (log.isDebugEnabled()) log.debug("Accepting " + file);
            } else {
                if (log.isDebugEnabled()) log.debug("Rejecting " + file);
            }
            return isFile;
        }
    }

    /** Accept only files that end in a valid image file format and
        exist. */
    private static class ValidImageFileFilter implements FileFilter {

        String[] validExt = { ".jpg", ".jpeg", ".gif", ".png", ".tif", ".tiff" };

        public boolean accept(File file) {
            boolean valid = false;
            String name = file.getName();
            if (!file.exists() || !file.isFile()) {
                valid = false;
            } else if (name == null || name.equals("")) {
                valid = false;
            } else {
                for (int i = 0; i < validExt.length; i++) {
                    if (name.toLowerCase().endsWith(validExt[i])) {
                        valid = true;
                    }
                }
            }
            return valid;
        }
    }

    /** Accept only files that end in a valid image file format and exist or are
        directories. */
    private static class ValidImageDirFileFilter extends ValidImageFileFilter {

        public boolean accept(File file) {
            if (file.isDirectory()) return true;
            return super.accept(file);
        }
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
    }
}
