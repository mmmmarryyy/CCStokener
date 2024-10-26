package astcentric.structure.basic;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import sun.net.www.protocol.file.FileURLConnection;
import astcentric.structure.filesystem.RealFile;
import astcentric.structure.filesystem.VirtualFile;
import astcentric.structure.filesystem.ZipEntryFilter;
import astcentric.structure.filesystem.ZipFilesystem;

/**
 * AST path with path elements for searching for {@link AST ASTs} by the
 * {@link DefaultASTLoader}.
 */
public class ASTPath {

    private final List<VirtualFile> _pathElements;

    /**
   * Creates an ASTPath based on the class path of the ClassLoader of this
   * class. Only folders and jar files with a <code>META-INF</code> folder are
   * taken into account.
   * <p>
   * Note, that the class path of the ClassLoader can be different from
   * <code>System.getProperty("java.class.path")</code>.
   */
    public static ASTPath createFromClassLoaderClasspath() {
        ClassLoader classLoader = ASTPath.class.getClassLoader();
        LinkedHashSet<String> elements = new LinkedHashSet<String>();
        try {
            addElementsTo(elements, "", classLoader);
            addElementsTo(elements, "META-INF", classLoader);
            return new ASTPath(elements.toArray(new String[elements.size()]));
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create AST path from class loader path.", e);
        }
    }

    private static void addElementsTo(LinkedHashSet<String> elements, String folder, ClassLoader classLoader) throws IOException {
        Enumeration<URL> resources = classLoader.getResources(folder);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            URLConnection connection = url.openConnection();
            if (connection instanceof JarURLConnection) {
                JarURLConnection jarc = (JarURLConnection) connection;
                ZipFile zipFile = jarc.getJarFile();
                elements.add(zipFile.getName());
            } else if (connection instanceof FileURLConnection) {
                elements.add(url.getFile());
            }
        }
    }

    /**
   * Creates an instance with the specified directory as the only path element.
   * 
   * @param baseDir A non-<code>null</code> argument.
   */
    public ASTPath(VirtualFile baseDir) {
        this(new VirtualFile[] { baseDir });
    }

    /**
   * Creates an instance with the specified files as path element.
   * 
   * @throws IllegalArgumentException if an array element is <code>null</code>.
   */
    public ASTPath(VirtualFile[] files) {
        _pathElements = new ArrayList<VirtualFile>();
        for (int i = 0; i < files.length; i++) {
            VirtualFile file = files[i];
            if (file == null) {
                throw new IllegalArgumentException("File for index " + i + " is undefined.");
            }
            _pathElements.add(file);
        }
    }

    /**
   * Creates an instance based the system property <code>ast.path</code>.
   */
    public ASTPath() {
        this(System.getProperty("ast.path", ""), File.pathSeparator);
    }

    /**
   * Creates an instance from the specified path elements.
   */
    public ASTPath(final String[] pathElements) {
        this(new Enumeration() {

            private int _index;

            public Object nextElement() {
                return hasMoreElements() ? pathElements[_index++] : null;
            }

            public boolean hasMoreElements() {
                return _index < pathElements.length;
            }
        });
    }

    /**
   * Creates an instance based on the specified path. The path elements are
   * separated by <code>java.io.File.pathSeparator</code>.
   * 
   * @param astPath
   *          Paths used to search for ASTs. Has to be non-empty string.
   */
    public ASTPath(String astPath) {
        this(astPath, File.pathSeparator);
    }

    private ASTPath(String astPath, String pathSeparator) {
        this(new StringTokenizer(astPath, pathSeparator));
    }

    private ASTPath(Enumeration elements) {
        List<VirtualFile> pathElements = new ArrayList<VirtualFile>();
        while (elements.hasMoreElements()) {
            File file = new File(elements.nextElement().toString());
            if (file.exists()) {
                if (file.isDirectory()) {
                    pathElements.add(new RealFile(file));
                } else if (isZipFile(file)) {
                    try {
                        ZipEntryFilter filter = new ZipEntryFilter() {

                            public boolean accept(ZipEntry entry) {
                                return entry.getName().endsWith(".ast");
                            }
                        };
                        ZipFilesystem filesystem = new ZipFilesystem(file, filter);
                        pathElements.add(filesystem.getRootDirectory());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        _pathElements = Collections.unmodifiableList(pathElements);
        if (_pathElements.size() == 0) {
            throw new IllegalArgumentException("No file in the astpath exists.");
        }
    }

    private boolean isZipFile(File file) {
        String name = file.getName();
        return name.endsWith(".jar") || name.endsWith(".zip");
    }

    List<VirtualFile> getPathElements() {
        return _pathElements;
    }
}
