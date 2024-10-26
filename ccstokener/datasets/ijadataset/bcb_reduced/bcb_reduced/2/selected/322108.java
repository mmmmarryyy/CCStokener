package org.netbeans.module.flexbean.modules.project.properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.module.flexbean.modules.project.FlexProjectGenerator;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.GeneratedFilesHelper;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author arnaud
 */
public final class FlexProjectPropertiesSupport {

    private static final String PROP_VERSION = "version";

    private static final String CURRENT_VERSION = "1.0-dev-20081029";

    public static EditableProperties load(final Project project) throws IOException {
        AntProjectHelper antProjectHelper = project.getLookup().lookup(AntProjectHelper.class);
        EditableProperties antProperties = antProjectHelper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        final boolean isUpdated = _update(antProperties);
        if (isUpdated || !_isBuildXmlExists(project, antProperties)) {
            _updateBuildXml(project);
            save(project, antProperties);
        }
        return antProperties;
    }

    public static void save(final Project project, final EditableProperties props) {
        final AntProjectHelper antProjectHelper = project.getLookup().lookup(AntProjectHelper.class);
        ProjectManager.mutex().writeAccess(new Runnable() {

            public void run() {
                antProjectHelper.putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, props);
            }
        });
    }

    public static Properties getPropertiesStartWith(EditableProperties props, String keyToFind) {
        final Properties propsFind = new Properties();
        final int length = keyToFind.length();
        final Set<String> keys = props.keySet();
        for (String key : keys) {
            if (key.startsWith(keyToFind)) {
                final String prop = key.substring(length);
                final String val = props.getProperty(key);
                propsFind.setProperty(prop, val);
            }
        }
        return propsFind;
    }

    public static void removeArrayProperties(EditableProperties props, String key) {
        final Iterator<Properties> arrayProps = getArrayProperties(props, key);
        while (arrayProps.hasNext()) {
            final Properties prop = arrayProps.next();
            final Set<Object> names = prop.keySet();
            for (Object name : names) {
                final String keyToDelete = key + (String) name;
                props.remove(keyToDelete);
            }
        }
    }

    public static void setArrayProperties(final EditableProperties props, String propertyName, Iterator<Properties> values) {
        int index = 0;
        while (values.hasNext()) {
            final Properties subValues = values.next();
            setArrayProperties(props, propertyName, index, subValues);
            ++index;
        }
    }

    private static void setArrayProperties(final EditableProperties props, String propertyName, int index, Properties values) {
        final Enumeration<String> subPropNames = (Enumeration<String>) values.propertyNames();
        while (subPropNames.hasMoreElements()) {
            final String subPropName = subPropNames.nextElement();
            final String key = propertyName + "[" + index + "]." + subPropName;
            props.setProperty(key, values.getProperty(subPropName));
        }
    }

    /**
     * Get array property : propertyName[index].subProp=value;... <br/>
     * 
     * 0&lt;= index &lt; n <br/>
     * Sample : <br/>
     * 
     * dependency[0].value=toto <br/>
     * dependency[0].nb=1 <br/>
     * dependency[1].value=titi <br/>
     * dependency[1].index=3 <br/>
     * 
     * Iterator&lt;Properties&gt;.length = 2 : { {(value,toto),(nb,1)}, {(value,titi),(index,3)}}
     * 
     * @param props
     * @param key
     * @return
     */
    public static Iterator<Properties> getArrayProperties(final EditableProperties props, String key) {
        final String keyToFind = key;
        return new Iterator<Properties>() {

            private int ix = -1;

            public boolean hasNext() {
                return props.containsKey(keyToFind + "[" + (ix + 1) + "]");
            }

            public Properties next() {
                if (!hasNext()) {
                    throw new IndexOutOfBoundsException();
                }
                ix++;
                final String indexedKey = keyToFind + "[" + ix + "]";
                return getPropertiesStartWith(props, indexedKey);
            }

            public void remove() {
            }
        };
    }

    public static EditableProperties create(final AntProjectHelper antProjectHelper) {
        final EditableProperties antProperties = antProjectHelper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        antProperties.setProperty(PROP_VERSION, CURRENT_VERSION);
        return antProperties;
    }

    private static String _getVersion(EditableProperties properties) {
        final String versionOf = properties.getProperty(PROP_VERSION);
        return versionOf;
    }

    private static boolean _update(EditableProperties antProperties) {
        boolean isUpdated = false;
        final String version = _getVersion(antProperties);
        if (version == null || !CURRENT_VERSION.equals(version)) {
            antProperties.setProperty(PROP_VERSION, CURRENT_VERSION);
            isUpdated = true;
        }
        return isUpdated;
    }

    private static boolean _isBuildXmlExists(Project project, EditableProperties antProperties) {
        final FileObject projectFileFolder = project.getProjectDirectory();
        final FileObject fo = projectFileFolder.getFileObject(GeneratedFilesHelper.BUILD_XML_PATH);
        return fo != null;
    }

    private static void _updateBuildXml(final Project project) throws IOException {
        final FileObject projectFileFolder = project.getProjectDirectory();
        projectFileFolder.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {

            public void run() throws IOException {
                FileObject dest = projectFileFolder.getFileObject(GeneratedFilesHelper.BUILD_XML_PATH);
                if (dest != null) {
                    dest.delete();
                }
                dest = projectFileFolder.createData(GeneratedFilesHelper.BUILD_XML_PATH);
                final URL url = FlexProjectGenerator.class.getResource("resources/build-impl.xml");
                InputStream in = url.openStream();
                OutputStream out = dest.getOutputStream();
                FileUtil.copy(in, out);
                in.close();
                out.close();
                ProjectManager.getDefault().saveProject(project);
            }
        });
    }
}
