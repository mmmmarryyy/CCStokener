package org.eclipse.help.internal.search;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.util.ResourceLocator;
import org.osgi.framework.Bundle;

public class PluginIndex {

    private static final String COMPLETE_FILENAME = "indexed_complete";

    private String pluginId;

    /**
	 * index path as defined in plugin.xml, e.g. "index"
	 */
    private String path;

    private SearchIndex targetIndex;

    /**
	 * path prefixes where index is found e.g. "", "nl/en/US", "ws/gtk"
	 */
    private List indexIDs;

    /**
	 * resolved directory paths (Strings) corresponding to indexes at given
	 * prefixes, e.g. //d/eclipse/...../os/linux/index,
	 */
    private List resolvedPaths;

    public PluginIndex(String pluginId, String path, SearchIndex targetIndex) {
        super();
        this.pluginId = pluginId;
        this.path = path;
        this.targetIndex = targetIndex;
    }

    private void resolve() {
        if (indexIDs != null) {
            return;
        }
        indexIDs = new ArrayList();
        resolvedPaths = new ArrayList();
        Bundle bundle = Platform.getBundle(pluginId);
        if (bundle == null) {
            return;
        }
        boolean found = false;
        ArrayList availablePrefixes = ResourceLocator.getPathPrefix(targetIndex.getLocale());
        for (int i = 0; i < availablePrefixes.size(); i++) {
            String prefix = (String) availablePrefixes.get(i);
            IPath prefixedPath = new Path(prefix + path);
            URL url = FileLocator.find(bundle, prefixedPath, null);
            if (url == null) {
                continue;
            }
            found = true;
            if (!isCompatible(bundle, prefixedPath)) {
                continue;
            }
            URL resolved;
            try {
                resolved = FileLocator.resolve(url);
            } catch (IOException ioe) {
                HelpBasePlugin.logError("Help index directory at " + prefixedPath + " for plugin " + bundle.getSymbolicName() + " cannot be resolved.", ioe);
                continue;
            }
            if ("file".equals(resolved.getProtocol())) {
                indexIDs.add(getIndexId(prefix));
                resolvedPaths.add(resolved.getFile());
                if (isComplete(bundle, prefixedPath)) {
                    break;
                }
            } else {
                try {
                    URL localURL = FileLocator.toFileURL(url);
                    if ("file".equals(localURL.getProtocol())) {
                        indexIDs.add(getIndexId(prefix));
                        resolvedPaths.add(localURL.getFile());
                        if (isComplete(bundle, prefixedPath)) {
                            break;
                        }
                    }
                } catch (IOException ioe) {
                    HelpBasePlugin.logError("Help index directory at " + prefixedPath + " for plugin " + bundle.getSymbolicName() + " cannot be resolved.", ioe);
                    continue;
                }
            }
        }
        if (!found) {
            HelpBasePlugin.logError("Help index declared, but missing for plugin " + getPluginId() + ".", null);
        }
    }

    private boolean isCompatible(Bundle bundle, IPath prefixedPath) {
        URL url = FileLocator.find(bundle, prefixedPath.append(SearchIndex.DEPENDENCIES_VERSION_FILENAME), null);
        if (url == null) {
            HelpBasePlugin.logError(prefixedPath.append(SearchIndex.DEPENDENCIES_VERSION_FILENAME) + " file missing from help index \"" + path + "\" of plugin " + getPluginId(), null);
            return false;
        }
        InputStream in = null;
        try {
            in = url.openStream();
            Properties prop = new Properties();
            prop.load(in);
            String lucene = prop.getProperty(SearchIndex.DEPENDENCIES_KEY_LUCENE);
            String analyzer = prop.getProperty(SearchIndex.DEPENDENCIES_KEY_ANALYZER);
            if (!targetIndex.isLuceneCompatible(lucene) || !targetIndex.isAnalyzerCompatible(analyzer)) {
                return false;
            }
        } catch (MalformedURLException mue) {
            return false;
        } catch (IOException ioe) {
            HelpBasePlugin.logError("IOException accessing prebuilt index.", ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return true;
    }

    private boolean isComplete(Bundle bundle, IPath prefixedPath) {
        URL url = FileLocator.find(bundle, prefixedPath.append(COMPLETE_FILENAME), null);
        return url != null;
    }

    /**
	 * Creates id of prebuilt index
	 * 
	 * @param prefix
	 *            index directory prefix, e.g. "", "ws/gtk"
	 * @return indexId string, e.g. "/", "/ws/gtk"
	 */
    private String getIndexId(String prefix) {
        if (prefix.length() == 0) {
            return "/";
        }
        return "/" + prefix.substring(0, prefix.length() - 1);
    }

    public boolean equals(Object obj) {
        return pluginId.equals(obj);
    }

    public int hashCode() {
        return pluginId.hashCode();
    }

    public String toString() {
        StringBuffer ret = new StringBuffer(pluginId);
        ret.append(":");
        ret.append(path);
        ret.append("=");
        if (indexIDs == null) {
            ret.append("unresolved");
        } else {
            for (int i = 0; i < indexIDs.size(); i++) {
                ret.append(indexIDs.get(i));
                ret.append("@");
                ret.append(resolvedPaths.get(i));
            }
        }
        return ret.toString();
    }

    public List getIDs() {
        resolve();
        return indexIDs;
    }

    /**
	 * @return list of paths (string) to an index directory. Paths are ordered
	 *         from
	 */
    public List getPaths() {
        resolve();
        return resolvedPaths;
    }

    public String getPluginId() {
        return pluginId;
    }
}
