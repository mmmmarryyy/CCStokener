package org.vizzini.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides convenience methods for working with files.
 *
 * @author   Jeffrey M. Thompson
 * @version  v0.4
 * @since    v0.3
 */
public class FileUtilities {

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(FileUtilities.class.getName());

    /** Line separator. */
    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * @param   protocol         Protocol (e.g. http or file).
     * @param   host             Host.
     * @param   file             File.
     * @param   includePrefixes  List of included prefixes (ignored if null).
     * @param   includeSuffixes  List of included suffixes (ignored if null).
     * @param   excludePrefixes  List of excluded prefixes (ignored if null).
     * @param   excludeSuffixes  List of excluded suffixes (ignored if null).
     *
     * @return  all directories using the given parameters.
     *
     * @throws  MalformedURLException  if a URL is malformed.
     *
     * @since   v0.3
     */
    public URL[] listDirectories(String protocol, String host, String file, List<String> includePrefixes, List<String> includeSuffixes, List<String> excludePrefixes, List<String> excludeSuffixes) throws MalformedURLException {
        boolean isRecursive = false;
        return listDirectories(protocol, host, file, includePrefixes, includeSuffixes, excludePrefixes, excludeSuffixes, isRecursive);
    }

    /**
     * @param   protocol         Protocol (e.g. http or file).
     * @param   host             Host.
     * @param   file             File.
     * @param   includePrefixes  List of included prefixes (ignored if null).
     * @param   includeSuffixes  List of included suffixes (ignored if null).
     * @param   excludePrefixes  List of excluded prefixes (ignored if null).
     * @param   excludeSuffixes  List of excluded suffixes (ignored if null).
     * @param   isRecursive      Flag indicating whether to recursively include
     *                           sub directories.
     *
     * @return  all directories using the given parameters.
     *
     * @throws  MalformedURLException  if a URL is malformed.
     *
     * @since   v0.3
     */
    public URL[] listDirectories(String protocol, String host, String file, List<String> includePrefixes, List<String> includeSuffixes, List<String> excludePrefixes, List<String> excludeSuffixes, boolean isRecursive) throws MalformedURLException {
        boolean isDirectoriesOnly = true;
        return listFiles(protocol, host, file, includePrefixes, includeSuffixes, excludePrefixes, excludeSuffixes, isRecursive, isDirectoriesOnly);
    }

    /**
     * @param   url  Parent directory of the files to list.
     *
     * @return  a collection of URLs representing files.
     *
     * @throws  IOException  if there is an I/O problem.
     *
     * @since   v0.3
     */
    public URL[] listFiles(URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        String urlStr = url.toString();
        if (!urlStr.endsWith("/")) {
            urlStr += "/";
        }
        Set<URL> files = new HashSet<URL>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader((InputStream) url.getContent()));
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                String filename = parseFilename(inputLine);
                if (filename != null) {
                    try {
                        files.add(new URL(urlStr + filename));
                    } catch (MalformedURLException e) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, e.getMessage(), e);
                        }
                    }
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return files.toArray(new URL[] {});
    }

    /**
     * @param   protocol  Protocol (e.g. http or file).
     * @param   host      Host.
     * @param   file      File.
     *
     * @return  all files using the given parameters.
     *
     * @throws  MalformedURLException  if a URL is malformed.
     *
     * @since   v0.3
     */
    public URL[] listFiles(String protocol, String host, String file) throws MalformedURLException {
        List<String> includePrefixes = null;
        List<String> includeSuffixes = null;
        List<String> excludePrefixes = null;
        List<String> excludeSuffixes = null;
        return listFiles(protocol, host, file, includePrefixes, includeSuffixes, excludePrefixes, excludeSuffixes);
    }

    /**
     * @param   protocol         Protocol (e.g. http or file).
     * @param   host             Host.
     * @param   file             File.
     * @param   includePrefixes  List of included prefixes (ignored if null).
     * @param   includeSuffixes  List of included suffixes (ignored if null).
     * @param   excludePrefixes  List of excluded prefixes (ignored if null).
     * @param   excludeSuffixes  List of excluded suffixes (ignored if null).
     *
     * @return  all files using the given parameters.
     *
     * @throws  MalformedURLException  if a URL is malformed.
     *
     * @since   v0.3
     */
    public URL[] listFiles(String protocol, String host, String file, List<String> includePrefixes, List<String> includeSuffixes, List<String> excludePrefixes, List<String> excludeSuffixes) throws MalformedURLException {
        boolean isRecursive = false;
        return listFiles(protocol, host, file, includePrefixes, includeSuffixes, excludePrefixes, excludeSuffixes, isRecursive);
    }

    /**
     * @param   protocol         Protocol (e.g. http or file).
     * @param   host             Host.
     * @param   file             File.
     * @param   includePrefixes  List of included prefixes (ignored if null).
     * @param   includeSuffixes  List of included suffixes (ignored if null).
     * @param   excludePrefixes  List of excluded prefixes (ignored if null).
     * @param   excludeSuffixes  List of excluded suffixes (ignored if null).
     * @param   isRecursive      Flag indicating whether to recursively include
     *                           sub directories.
     *
     * @return  all files using the given parameters.
     *
     * @throws  MalformedURLException  if a URL is malformed.
     *
     * @since   v0.3
     */
    public URL[] listFiles(String protocol, String host, String file, List<String> includePrefixes, List<String> includeSuffixes, List<String> excludePrefixes, List<String> excludeSuffixes, boolean isRecursive) throws MalformedURLException {
        boolean isDirectoriesOnly = false;
        return listFiles(protocol, host, file, includePrefixes, includeSuffixes, excludePrefixes, excludeSuffixes, isRecursive, isDirectoriesOnly);
    }

    /**
     * @param   protocol           Protocol (e.g. http or file).
     * @param   host               Host.
     * @param   file               File.
     * @param   includePrefixes    List of included prefixes (ignored if null).
     * @param   includeSuffixes    List of included suffixes (ignored if null).
     * @param   excludePrefixes    List of excluded prefixes (ignored if null).
     * @param   excludeSuffixes    List of excluded suffixes (ignored if null).
     * @param   isRecursive        Flag indicating whether to recursively
     *                             include sub directories.
     * @param   isDirectoriesOnly  Flag indicating whether to include
     *                             directories only.
     *
     * @return  all files using the given parameters.
     *
     * @throws  MalformedURLException  if a URL is malformed.
     *
     * @since   v0.3
     */
    public URL[] listFiles(String protocol, String host, String file, List<String> includePrefixes, List<String> includeSuffixes, List<String> excludePrefixes, List<String> excludeSuffixes, boolean isRecursive, boolean isDirectoriesOnly) throws MalformedURLException {
        URL baseUrl = new URL(protocol, host, file);
        Set<URL> set = new TreeSet<URL>(new URLComparator());
        set.add(baseUrl);
        try {
            URL[] files = listFiles(baseUrl);
            if ((files != null) && (files.length > 0)) {
                for (int i = 0; i < files.length; i++) {
                    URL url = files[i];
                    if (passes(url, includePrefixes, includeSuffixes, excludePrefixes, excludeSuffixes)) {
                        File currentFile = new File(url.getFile());
                        if (!isDirectoriesOnly || (isDirectoriesOnly && currentFile.isDirectory())) {
                            set.add(url);
                        }
                        if (isRecursive && currentFile.isDirectory()) {
                            URL[] recursiveFiles = listFiles(protocol, host, currentFile.getAbsolutePath(), includePrefixes, includeSuffixes, excludePrefixes, excludeSuffixes, isRecursive, isDirectoriesOnly);
                            for (int j = 0; j < recursiveFiles.length; j++) {
                                URL recursiveUrl = recursiveFiles[j];
                                set.add(recursiveUrl);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return set.toArray(new URL[set.size()]);
    }

    /**
     * @param   reader  Reader.
     *
     * @return  The contents of the file.
     *
     * @throws  IOException  if there is an I/O problem.
     *
     * @since   v0.3
     */
    public String readFile(Reader reader) throws IOException {
        BufferedReader bufferedReader = null;
        if (reader instanceof BufferedReader) {
            bufferedReader = (BufferedReader) reader;
        } else {
            bufferedReader = new BufferedReader(reader);
        }
        return read(bufferedReader);
    }

    /**
     * @param   inputStream  Input stream.
     *
     * @return  The contents of the file.
     *
     * @throws  IOException  if there is an I/O problem.
     *
     * @since   v0.3
     */
    public String readFile(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return read(reader);
    }

    /**
     * @param   sFilepath  Path to the file.
     *
     * @return  The contents of the file.
     *
     * @throws  FileNotFoundException  if a file is not found.
     * @throws  IOException            if there is an I/O problem.
     *
     * @since   v0.3
     */
    public String readFile(String sFilepath) throws FileNotFoundException, IOException {
        if (sFilepath == null) {
            throw new IllegalArgumentException("sFilepath == null");
        }
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(sFilepath);
            BufferedReader reader = new BufferedReader(fileReader);
            return read(reader);
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
        }
    }

    /**
     * @param   url  URL of the file.
     *
     * @return  The contents of the file.
     *
     * @throws  IOException  if there is an I/O problem.
     *
     * @since   v0.3
     */
    public String readFile(URL url) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        return read(reader);
    }

    /**
     * Write the given string to the given file.
     *
     * @param   file     File to which to write.
     * @param   content  Content to put into the file.
     *
     * @throws  IOException  if there is an I/O problem.
     *
     * @since   v0.3
     */
    public void writeFile(File file, String content) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        if (content == null) {
            throw new IllegalArgumentException("content == null");
        }
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(content);
            writer.flush();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * @param   list  List of prefixes.
     * @param   url   URL.
     *
     * @return  true if the given URL is prefixed by any item in the given list.
     *
     * @since   v0.3
     */
    protected boolean isPrefixedWith(URL url, List<String> list) {
        boolean answer = true;
        if (list != null) {
            String urlStr = url.getFile();
            int index = urlStr.lastIndexOf('/');
            urlStr = urlStr.substring(index + 1);
            answer = false;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                String prefix = list.get(i);
                if (urlStr.startsWith(prefix)) {
                    answer = true;
                    break;
                }
            }
        }
        return answer;
    }

    /**
     * @param   list  List of suffixes.
     * @param   url   URL.
     *
     * @return  true if the given URL is suffixed with any item in the given
     *          list.
     *
     * @since   v0.3
     */
    protected boolean isSuffixedWith(URL url, List<String> list) {
        boolean answer = true;
        if (list != null) {
            String urlStr = url.getFile();
            int index = urlStr.lastIndexOf('/');
            if (index >= 0) {
                urlStr = urlStr.substring(index + 1);
            }
            answer = false;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                String suffix = list.get(i);
                if (urlStr.endsWith(suffix)) {
                    answer = true;
                    break;
                }
            }
        }
        return answer;
    }

    /**
     * @param   url              URL.
     * @param   includePrefixes  List of included prefixes (ignored if null).
     * @param   includeSuffixes  List of included suffixes (ignored if null).
     * @param   excludePrefixes  List of excluded prefixes (ignored if null).
     * @param   excludeSuffixes  List of excluded suffixes (ignored if null).
     *
     * @return  true if the given URL passes the filter as specified by the
     *          other parameters.
     *
     * @since   v0.3
     */
    protected boolean passes(URL url, List<String> includePrefixes, List<String> includeSuffixes, List<String> excludePrefixes, List<String> excludeSuffixes) {
        return (isPrefixedWith(url, includePrefixes) && isSuffixedWith(url, includeSuffixes) && !((excludePrefixes != null) && isPrefixedWith(url, excludePrefixes)) && !((excludeSuffixes != null) && isSuffixedWith(url, excludeSuffixes)));
    }

    /**
     * @param   reader  Buffered reader.
     *
     * @return  the contents of the given reader.
     *
     * @throws  IOException  if there is an I/O problem.
     *
     * @since   v0.3
     */
    protected String read(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            sb.append(line);
            line = reader.readLine();
            if (line != null) {
                sb.append(LINE_SEPARATOR);
            }
        }
        return sb.toString();
    }

    /**
     * Parse out the filename from the given HTML input line. The given line is
     * assumed to be from a directory listing in HTML.
     *
     * @param   inputLine  Line to parse.
     *
     * @return  the filename.
     *
     * @since   v0.3
     */
    private static String parseFilename(String inputLine) {
        String answer = null;
        Matcher m = Pattern.compile("<a href=\"(.*)\">.*</a>", Pattern.CASE_INSENSITIVE).matcher(inputLine);
        if (m.find()) {
            String filename = m.group(1);
            if ((filename != null) && (filename.length() > 0)) {
                answer = filename;
            }
        }
        if (answer == null) {
            answer = inputLine;
        }
        return answer;
    }

    /**
     * Provides an implementation of <code>Comparator</code> for URLs.
     *
     * @author   Jeffrey M. Thompson
     * @version  v0.4
     * @since    v0.3
     */
    class URLComparator implements Comparator<URL> {

        /**
         * @param   url0  First URL.
         * @param   url1  Second URL.
         *
         * @return  -1, 0, 1 if url0 is less than, equal to, greater than url1.
         *
         * @see     java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(URL url0, URL url1) {
            int answer = -1;
            if (url0 == url1) {
                answer = 0;
            } else if ((url0 != null) && (url1 != null)) {
                String urlStr0 = url0.toExternalForm();
                String urlStr1 = url1.toExternalForm();
                answer = urlStr0.compareTo(urlStr1);
            }
            return answer;
        }
    }
}
