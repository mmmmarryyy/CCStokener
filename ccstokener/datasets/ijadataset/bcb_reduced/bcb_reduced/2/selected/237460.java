package eu.annocultor.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * Various utilities.
 * 
 * @author Borys Omelayenko
 * 
 */
public class Utils {

    public static String getLocalOrGlobalEnvironmentVariable(String parameter) throws RuntimeException {
        return (System.getProperty(parameter) == null) ? System.getenv(parameter) : System.getProperty(parameter);
    }

    /**
	 * Passing parameters via env variable, versus command line,
	 * works around the fact that maven exec plugin launches a shell
	 * that enforces its own interpretation of wildcards.
	 * 
	 * E.g. on Win a parameter input_files/*.xml gets replaced
	 * with input_files/first-file-name.xml where first-file-name is the
	 * name of the first file in input_files.
	 * 
	 */
    public static String[] getCommandLineFromANNOCULTOR_ARGS(String... args) {
        List<String> resArgs = new ArrayList<String>();
        resArgs.addAll(Arrays.asList(args));
        String envArgs = System.getenv("ANNOCULTOR_ARGS");
        if (envArgs != null) resArgs.addAll(Arrays.asList(envArgs.split(" ")));
        return resArgs.toArray(new String[] {});
    }

    public static List<File> expandFileTemplateFrom(File dir, String... patterns) throws IOException {
        List<File> files = new ArrayList<File>();
        for (String pattern : patterns) {
            File fdir = new File(new File(dir, FilenameUtils.getFullPathNoEndSeparator(pattern)).getCanonicalPath());
            if (!fdir.isDirectory()) throw new IOException("Error: " + fdir.getCanonicalPath() + ", expanded from directory " + dir.getCanonicalPath() + " and pattern " + pattern + " does not denote a directory");
            if (!fdir.canRead()) throw new IOException("Error: " + fdir.getCanonicalPath() + " is not readable");
            FileFilter fileFilter = new WildcardFileFilter(FilenameUtils.getName(pattern));
            File[] list = fdir.listFiles(fileFilter);
            if (list == null) throw new IOException("Error: " + fdir.getCanonicalPath() + " does not denote a directory or something else is wrong");
            if (list.length == 0) throw new IOException("Error: no files found, template " + pattern + " from dir " + dir.getCanonicalPath() + " where we recognised " + fdir.getCanonicalPath() + " as path and " + fileFilter + " as file mask");
            for (File file : list) {
                if (!file.exists()) {
                    throw new FileNotFoundException("File not found: " + file + " resolved to " + file.getCanonicalPath());
                }
            }
            files.addAll(Arrays.asList(list));
        }
        return files;
    }

    public static String show(Collection list, String separator) {
        String result = "";
        for (Object object : list) {
            if (result.length() > 0) result += separator;
            result += object.toString();
        }
        return result;
    }

    /**
	 * Returned by compareFiles.
	 * 
	 */
    public static class DiffInFiles {

        @Override
        public String toString() {
            return "Line" + line + ", " + strOne + " * " + strTwo;
        }

        public int line;

        public String strOne;

        public String strTwo;

        public DiffInFiles(int line, String strOne, String strTwo) {
            super();
            this.line = line;
            this.strOne = strOne;
            this.strTwo = strTwo;
        }
    }

    /**
	 * Compares two text files.
	 * 
	 * @param file1
	 * @param file2
	 * @return lines where they differ
	 */
    public static List<DiffInFiles> compareFiles(File file1, File file2, int maxDiffLinesToReport) throws IOException {
        List<DiffInFiles> result = new ArrayList<DiffInFiles>();
        BufferedReader b1 = new BufferedReader(new FileReader(file1), 64000);
        BufferedReader b2 = new BufferedReader(new FileReader(file2), 64000);
        int lineNumber = 0;
        while (maxDiffLinesToReport == -1 || result.size() < maxDiffLinesToReport) {
            String s1 = b1.readLine();
            String s2 = b2.readLine();
            if (s1 == null && s2 == null) break;
            if (s1 == null) {
                result.add(new DiffInFiles(lineNumber, s1, s2));
            } else {
                if (s2 == null) {
                    result.add(new DiffInFiles(lineNumber, s1, s2));
                } else {
                    if (!s1.equals(s2)) result.add(new DiffInFiles(lineNumber, s1, s2));
                }
            }
            lineNumber++;
        }
        b1.close();
        b2.close();
        return result;
    }

    /**
	 * Loads a file into a string.
	 * 
	 * @param fileName
	 *          file name
	 * @param EOL
	 *          End-of-line string to put into the resulting string
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static String loadFileToString(String fileName, String EOL) throws FileNotFoundException, IOException {
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String result = "";
        String str;
        while ((str = in.readLine()) != null) {
            result += str + EOL;
        }
        in.close();
        return result;
    }

    public static void saveStringToFile(String string, String fileName) throws FileNotFoundException, IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
        out.append(string);
        out.close();
    }

    /**
	 * Loads a web page from an URL to a string.
	 * 
	 * @param url
	 *          address of the page
	 * @param EOL
	 *          End-of-line string to put into the resulting string
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static String loadURLToString(String url, String EOL) throws FileNotFoundException, IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader((new URL(url)).openStream()));
        String result = "";
        String str;
        while ((str = in.readLine()) != null) {
            result += str + EOL;
        }
        in.close();
        return result;
    }

    private static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("File is too large");
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    public static byte[] readResourceFile(File src) throws IOException {
        if (src == null) throw new NullPointerException("Null src");
        String srcPath;
        try {
            srcPath = src.getCanonicalPath();
        } catch (Exception e) {
            throw new IOException(e.getMessage() + " on " + src.getName());
        }
        if (srcPath.contains("!")) {
            if (srcPath.indexOf("file:") != srcPath.lastIndexOf("file:")) throw new IOException("Source " + srcPath + " should contain only one substring 'file:'");
            srcPath = srcPath.substring(srcPath.indexOf("file:") + "file:".length());
            String[] srcPaths = srcPath.split("!");
            if (srcPaths.length != 2) throw new IOException("Source " + srcPath + " should contain no '!' for plain files or just '!' for jar files.");
            if (srcPaths[1].startsWith("/") || srcPaths[1].startsWith("\\")) srcPaths[1] = srcPaths[1].substring(1);
            JarResources jr = new JarResources(srcPaths[0]);
            byte[] buf = jr.getResource(srcPaths[1]);
            if (buf == null) throw new NullPointerException("Null resource: " + srcPaths[1] + " from " + srcPaths[0]);
            return buf;
        } else {
            return getBytesFromFile(src);
        }
    }

    public static String readResourceFileAsString(String resource) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(readResourceFile(new File(resource)))));
        String line;
        String result = "";
        while ((line = br.readLine()) != null) {
            result += line + "\n";
        }
        br.close();
        return result;
    }

    private static InputStream readResourceFromPackage(Class theClass, String resource) throws IOException {
        if (theClass.getResource(resource) == null) throw new NullPointerException("Failed to find resource for class " + theClass.getName() + " resource " + resource);
        String fileName = theClass.getResource(resource).getFile();
        if (fileName == null) throw new NullPointerException("Failed to generate file for resource for class " + theClass.getName() + " resource " + resource);
        byte[] file = readResourceFile(new File(fileName));
        return new ByteArrayInputStream(file);
    }

    public static String readResourceFileFromSamePackageAsString(Class theClass, String resource) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(readResourceFromPackage(theClass, resource)));
        String line;
        String result = "";
        while ((line = br.readLine()) != null) {
            result += line + "\n";
        }
        br.close();
        return result;
    }

    public static List<String> readResourceFileFromSamePackageAsList(Class theClass, String resource) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(readResourceFromPackage(theClass, resource)));
        String line;
        List<String> result = new ArrayList<String>();
        while ((line = br.readLine()) != null) {
            result.add(line);
        }
        br.close();
        return result;
    }

    /**
	 * A dirty Dutch plural -> singular converter.
	 */
    public static String getSingular(String plural) {
        if (plural == null) return null;
        String singular = plural;
        if (plural.endsWith("en")) singular = plural.substring(0, plural.length() - 2); else if (plural.endsWith("'s")) singular = plural.substring(0, plural.length() - 2); else if (plural.endsWith("s")) singular = plural.substring(0, plural.length() - 1);
        if (singular.matches("(.*)(ss|tt|qq|ww|rr|pp|dd|ff|gg|kk|ll|zz|xx|cc|vv|bb|nn|mm)$")) singular = singular.substring(0, singular.length() - 1);
        if (singular.endsWith("v")) singular = singular.substring(0, singular.length() - 1) + "f";
        if (singular.endsWith("z")) singular = singular.substring(0, singular.length() - 1) + "s";
        return singular;
    }

    /**
	 * Attempts to list all the classes in the specified package as determined by
	 * the context class loader
	 * 
	 * @param pckgname
	 *          the package name to search
	 * @return a list of classes that exist within that package
	 * @throws ClassNotFoundException
	 *           if something went wrong
	 */
    public static List<Class> getClassesForPackage(String pckgname) throws ClassNotFoundException, IOException {
        Set<String> list = new FindClasspath().getClassesForPackage(pckgname);
        List<Class> classes = new ArrayList<Class>();
        for (String fileName : list) {
            classes.add(Class.forName(pckgname.length() == 0 ? fileName : (pckgname + '.' + fileName)));
        }
        return classes;
    }

    private static Set<String> getClassNamesPackage(String pckgname) throws ClassNotFoundException, IOException {
        Queue<File> directories = new LinkedList<File>();
        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            String path = pckgname.replace('.', '/');
            Enumeration<URL> resources = cld.getResources(path);
            while (resources.hasMoreElements()) {
                directories.add(new File(URLDecoder.decode(resources.nextElement().getPath(), "UTF-8")));
            }
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(pckgname + " does not appear to be a valid package (Null pointer exception)");
        } catch (UnsupportedEncodingException encex) {
            throw new ClassNotFoundException(pckgname + " does not appear to be a valid package (Unsupported encoding)");
        } catch (IOException ioex) {
            throw new ClassNotFoundException("IOException was thrown when trying to get all resources for " + pckgname);
        }
        Set<String> classes = new HashSet<String>();
        while (!directories.isEmpty()) {
            File directory = directories.poll();
            if (directory.exists()) {
                File[] files = directory.listFiles();
                for (File file : files) {
                    if (file.getCanonicalPath().endsWith(".class")) {
                        String fileName = file.getPath().substring(directory.getPath().length() + 1);
                        pckgname = file.getPath().substring(file.getPath().indexOf(File.separator + "nl" + File.separator) + 1);
                        pckgname = pckgname.substring(0, pckgname.lastIndexOf(File.separator)).replaceAll("\\" + File.separator, ".");
                        classes.add(fileName.substring(0, fileName.length() - 6));
                    }
                    if (file.isDirectory()) {
                        directories.add(file);
                    }
                }
            } else {
                throw new ClassNotFoundException(pckgname + " (" + directory.getPath() + ") does not appear to be a valid package");
            }
        }
        return classes;
    }

    public static void copy(InputStream src, File dst) throws IOException {
        if (src == null) throw new NullPointerException("Source should not be NULL.");
        if (dst == null) throw new NullPointerException("Dest should not be NULL.");
        OutputStream out = new FileOutputStream(dst);
        while (src.available() != 0) {
            out.write(src.read());
        }
        out.close();
    }
}
