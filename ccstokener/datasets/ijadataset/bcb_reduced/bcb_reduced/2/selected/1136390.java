package astcentric;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;

/**
 * Helper class which makes build and environment information available.
 *
 */
public class BuildInfo {

    private static final String IMPLEMENTATION_VERSION = Attributes.Name.IMPLEMENTATION_VERSION.toString();

    private static final String IMPLMENTATION_TITLE = Attributes.Name.IMPLEMENTATION_TITLE.toString();

    /**
   * Name and version of a library.
   */
    public static final class LibraryInfo {

        private final String _name;

        private final String _version;

        private LibraryInfo(String name, String version) {
            _name = name;
            _version = version;
        }

        public String getVersion() {
            return _version;
        }

        public String getName() {
            return _name;
        }

        @Override
        public String toString() {
            return _name + " " + _version;
        }
    }

    /**
   * The one and only one instance of this class.
   */
    public static final BuildInfo BUILD_INFO = new BuildInfo();

    private final List<LibraryInfo> _libraryInfos;

    private final String _os = System.getProperty("os.name") + " " + System.getProperty("os.version");

    private final String _javaVM = System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version");

    private BuildInfo() {
        String javaHome = System.getProperty("java.home");
        List<LibraryInfo> infos = new ArrayList<LibraryInfo>();
        Class<? extends BuildInfo> clazz = getClass();
        ClassLoader classLoader = clazz.getClassLoader();
        try {
            Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                LibraryInfo info = readManifest(url.openStream());
                if (info != null && url.getFile().indexOf(javaHome) < 0) {
                    infos.add(info);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read BUILD file.", e);
        }
        _libraryInfos = Collections.unmodifiableList(infos);
    }

    private LibraryInfo readManifest(InputStream inputStream) {
        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            String title = properties.getProperty(IMPLMENTATION_TITLE);
            if (title == null || title.length() == 0) {
                return null;
            }
            String version = properties.getProperty(IMPLEMENTATION_VERSION);
            if (version == null || version.length() == 0) {
                return null;
            }
            return new LibraryInfo(title, version);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read MANIFEST file.", e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException("Couldn't close input stream.", e);
            }
        }
    }

    /**
   * Returns the unmodifiable list of all libraries with a MANIFEST file
   * defining Implementation-Title and Implementation-Version. 
   * The order is determined by the order the MANIFEST files 
   * appear in the classpath. 
   */
    public List<LibraryInfo> getLibraryInfos() {
        return _libraryInfos;
    }

    /**
   * Returns the version of the used Java VM.
   */
    public String getJavaVM() {
        return _javaVM;
    }

    /**
   * Returns name and version of the used operation system.
   */
    public String getOS() {
        return _os;
    }

    /**
   * Returns a multiline string with all build and environment informations.
   */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Software Version");
        int size = _libraryInfos.size();
        if (size == 0) {
            buffer.append(": UNKNOWN\n");
        } else if (size == 1) {
            String version = _libraryInfos.get(0).getVersion();
            buffer.append(": ").append(version).append('\n');
        } else {
            buffer.append("s:\n");
            for (LibraryInfo entry : _libraryInfos) {
                buffer.append("   ").append(entry).append('\n');
            }
        }
        buffer.append("Java VM: ").append(getJavaVM()).append('\n');
        buffer.append("OS: ").append(getOS());
        return new String(buffer);
    }

    /**
   * Prints build and environment information onto the console.
   */
    public static void main(String[] args) {
        System.out.println(BUILD_INFO);
    }
}
