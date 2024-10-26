package org.mcisb.util.io;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

/**
 *
 * @author Neil Swainston
 */
public class FileUtils {

    /**
	 * 
	 */
    public static final String EXTENSION_SEPARATOR = ".";

    /**
	 *
	 * @param filename
	 * @return File
	 */
    public static File getResource(final String filename) {
        final String SPACE = " ";
        final String SPACE_ENCODED = "%20";
        return new File(ClassLoader.getSystemResource(filename).getFile().replace(SPACE_ENCODED, SPACE));
    }

    /**
	 *
	 * @param file
	 * @return String
	 */
    public static String getExtension(final File file) {
        return file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(EXTENSION_SEPARATOR) + EXTENSION_SEPARATOR.length());
    }

    /**
	 *
	 * @param file
	 * @return String
	 */
    public static String getFilename(final File file) {
        return file.getName().substring(0, file.getName().lastIndexOf(EXTENSION_SEPARATOR));
    }

    /**
	 * Wrapper of File.createTempFile.
	 * 
	 * At first glance this seems unnecessary, but there is evidence that Tomcat occasionally deletes its temp directory.
	 * 
	 * Therefore, the directory must be re-created.
	 * 
	 * @param prefix
	 * @param suffix
	 * @return File
	 * @throws IOException
	 */
    public static File createTempFile(final String prefix, final String suffix) throws IOException {
        final File temp = new File(System.getProperty("java.io.tmpdir"));
        if (!temp.mkdir()) {
            throw new IOException();
        }
        return File.createTempFile(prefix, suffix, temp);
    }

    /**
	 *
	 * @param path
	 * @return String
	 */
    public String normalisePath(final String path) {
        final char BACK_SLASH = '\\';
        final char FORWARD_SLASH = '/';
        final String SPACE = " ";
        final String SPACE_ESCAPE = "%20";
        return path.replace(BACK_SLASH, FORWARD_SLASH).replace(SPACE, SPACE_ESCAPE);
    }

    /**
	 *
	 * @param filepaths
	 * @return List
	 */
    public List<File> getFiles(List<String> filepaths) {
        final List<File> files = new ArrayList<File>();
        for (Iterator<String> iterator = filepaths.iterator(); iterator.hasNext(); ) {
            files.add(new File(iterator.next()));
        }
        return files;
    }

    /**
	 *
	 * @param filename
	 * @return String
	 */
    public String stripExtension(String filename) {
        final String SEPARATOR = ".";
        final StringTokenizer tokenizer = new StringTokenizer(filename, SEPARATOR);
        return tokenizer.nextToken();
    }

    /**
	 *
	 * @param file
	 * @param bytes
	 * @throws IOException
	 */
    public void write(final File file, final byte[] bytes) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new ByteArrayInputStream(bytes);
            os = new FileOutputStream(file);
            new StreamReader(is, os).read();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        }
    }

    /**
	 *
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
    public void fileCopy(File src, File dest) throws IOException {
        if (!dest.exists()) {
            final File parent = new File(dest.getParent());
            if (!parent.exists() && !parent.mkdirs()) {
                throw new IOException();
            }
            if (!dest.createNewFile()) {
            }
        }
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(src);
            os = new FileOutputStream(dest);
            final FileChannel srcChannel = is.getChannel();
            final FileChannel dstChannel = os.getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        }
    }

    /**
	 *
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
    public void fileCopy(String src, String dest) throws IOException {
        fileCopy(new File(src), new File(dest));
    }

    /**
	 *
	 * @param file
	 * @throws IOException
	 */
    public void zip(final File file) throws IOException {
        final String ZIP_EXTENSION = ".zip";
        InputStream is = null;
        ZipOutputStream os = null;
        try {
            os = new ZipOutputStream(new FileOutputStream(file.getAbsolutePath() + ZIP_EXTENSION));
            is = new FileInputStream(file);
            os.putNextEntry(new ZipEntry(file.getName()));
            new StreamReader(is, os).read();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        }
    }

    /**
	 *
	 * @param fileName
	 * @return String
	 */
    public String convertCharacters(String fileName) {
        return fileName.replace(':', ' ').replace('|', ' ').replace('*', ' ').replace('?', ' ');
    }

    /**
	 *
	 * @param parentDir
	 * @param name
	 * @return File
	 * @throws IOException
	 */
    public File mkdir(File parentDir, String name) throws IOException {
        final File directory = new File(parentDir, name);
        if (!directory.mkdir()) {
            throw new IOException();
        }
        return directory;
    }

    /**
	 *
	 * @param url
	 * @return String
	 * @throws IOException
	 */
    public byte[] read(URL url) throws IOException {
        return read(url.openStream());
    }

    /**
	 * 
	 * @param is
	 * @return String
	 * @throws IOException
	 */
    public byte[] read(InputStream is) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        new StreamReader(is, os).read();
        return os.toByteArray();
    }

    /**
	 *
	 * @param is
	 * @param os
	 * @param regex
	 * @throws IOException
	 */
    public void stripLine(final InputStream is, final OutputStream os, final String regex) throws IOException {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        String line = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
            writer = new BufferedWriter(new OutputStreamWriter(os, Charset.defaultCharset()));
            while ((line = reader.readLine()) != null) {
                if (line.matches(regex)) {
                    continue;
                }
                writer.write(line);
                writer.newLine();
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
	 *
	 *
	 * @param file
	 * @return Map
	 * @throws IOException
	 */
    public Map<String, List<String>> generateMap(final File file) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return generateMap(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
	 *
	 *
	 * @param is
	 * @return Map
	 * @throws IOException
	 */
    public Map<String, List<String>> generateMap(final InputStream is) throws IOException {
        final String WHITE_SPACE = "\\s";
        final Map<String, List<String>> map = new HashMap<String, List<String>>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
        String line = null;
        while ((line = reader.readLine()) != null) {
            final String[] tokens = line.split(WHITE_SPACE);
            final String[] copy = new String[tokens.length - 1];
            System.arraycopy(tokens, 1, copy, 0, copy.length);
            map.put(tokens[0], Arrays.asList(copy));
        }
        return map;
    }

    /**
	 *
	 * @param topDirectory
	 * @param outputDirectory
	 * @param extension
	 * @throws IOException
	 */
    public void concatFiles(final File topDirectory, final File outputDirectory, final String extension) throws IOException {
        if (topDirectory.isDirectory()) {
            final File[] childFiles = topDirectory.listFiles();
            for (int i = 0; i < childFiles.length; i++) {
                if (childFiles[i].isDirectory()) {
                    concatFiles(childFiles[i], new File(outputDirectory, childFiles[i].getName() + extension));
                }
            }
        }
    }

    /**
	 * 
	 *
	 * @param directory
	 * @param out
	 * @throws IOException
	 */
    public void concatFiles(final File directory, final File out) throws IOException {
        final String LINE_SEPARATOR = System.getProperty("line.separator");
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(out, true));
            final File[] in = directory.listFiles();
            for (int i = 0; i < in.length; i++) {
                InputStream is = null;
                try {
                    is = new FileInputStream(in[i]);
                    new StreamReader(is, os).read();
                    os.write(LINE_SEPARATOR.getBytes(Charset.defaultCharset()));
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    /**
	 * 
	 * @param root
	 * @param name
	 * @return File
	 */
    public File find(final File root, final String name) {
        if (root.getName().equals(name)) {
            return root;
        }
        if (root.isDirectory()) {
            for (final File child : root.listFiles()) {
                final File result = find(child, name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
	 * 
	 * @param file
	 * @param nameRegExp
	 * @return Collection<File>
	 */
    public Collection<File> findByRegExp(final File file, final String nameRegExp) {
        final Collection<File> files = new ArrayList<File>();
        findByRegExp(file, nameRegExp, files);
        return files;
    }

    /**
	 * 
	 * @param directory
	 * @param nameRegExp
	 * @param files
	 */
    private void findByRegExp(final File file, final String nameRegExp, final Collection<File> files) {
        if (file.getName().matches(nameRegExp)) {
            files.add(file);
        }
        if (file.isDirectory()) {
            for (final File child : file.listFiles()) {
                findByRegExp(child, nameRegExp, files);
            }
        }
    }
}
