package org.eclipse.core.internal.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

public class FindSupport {

    private static String[] NL_JAR_VARIANTS = buildNLVariants(InternalPlatform.getDefault().getNL());

    private static String[] buildNLVariants(String nl) {
        ArrayList result = new ArrayList();
        IPath base = new Path("nl");
        IPath path = new Path(nl.replace('_', '/'));
        while (path.segmentCount() > 0) {
            result.add(base.append(path).toString());
            if (path.segmentCount() > 1) result.add(base.append(path.toString().replace('/', '_')).toString());
            path = path.removeLastSegments(1);
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    /**
	 * See doc on @link Platform#find(Bundle, IPath) Platform#find(Bundle, IPath) 
	 */
    public static URL find(Bundle bundle, IPath path) {
        return find(bundle, path, null);
    }

    /**
	 * See doc on @link Platform#find(Bundle, IPath, Map) Platform#find(Bundle, IPath, Map) 
	 */
    public static URL find(Bundle b, IPath path, Map override) {
        if (path == null) return null;
        URL result = null;
        if (path.isEmpty() || path.isRoot()) {
            result = findInPlugin(b, Path.EMPTY);
            if (result == null) result = findInFragments(b, Path.EMPTY);
            return result;
        }
        String first = path.segment(0);
        if (first.charAt(0) != '$') {
            result = findInPlugin(b, path);
            if (result == null) result = findInFragments(b, path);
            return result;
        }
        IPath rest = path.removeFirstSegments(1);
        if (first.equalsIgnoreCase("$nl$")) return findNL(b, rest, override);
        if (first.equalsIgnoreCase("$os$")) return findOS(b, rest, override);
        if (first.equalsIgnoreCase("$ws$")) return findWS(b, rest, override);
        if (first.equalsIgnoreCase("$files$")) return null;
        return null;
    }

    private static URL findOS(Bundle b, IPath path, Map override) {
        String os = null;
        if (override != null) try {
            os = (String) override.get("$os$");
        } catch (ClassCastException e) {
        }
        if (os == null) os = InternalPlatform.getDefault().getOS();
        if (os.length() == 0) return null;
        String osArch = null;
        if (override != null) try {
            osArch = (String) override.get("$arch$");
        } catch (ClassCastException e) {
        }
        if (osArch == null) osArch = InternalPlatform.getDefault().getOSArch();
        if (osArch.length() == 0) return null;
        URL result = null;
        IPath base = new Path("os").append(os).append(osArch);
        while (base.segmentCount() != 1) {
            IPath filePath = base.append(path);
            result = findInPlugin(b, filePath);
            if (result != null) return result;
            result = findInFragments(b, filePath);
            if (result != null) return result;
            base = base.removeLastSegments(1);
        }
        result = findInPlugin(b, path);
        if (result != null) return result;
        return findInFragments(b, path);
    }

    private static URL findWS(Bundle b, IPath path, Map override) {
        String ws = null;
        if (override != null) try {
            ws = (String) override.get("$ws$");
        } catch (ClassCastException e) {
        }
        if (ws == null) ws = InternalPlatform.getDefault().getWS();
        IPath filePath = new Path("ws").append(ws).append(path);
        URL result = findInPlugin(b, filePath);
        if (result != null) return result;
        result = findInFragments(b, filePath);
        if (result != null) return result;
        result = findInPlugin(b, path);
        if (result != null) return result;
        return findInFragments(b, path);
    }

    private static URL findNL(Bundle b, IPath path, Map override) {
        String nl = null;
        String[] nlVariants = null;
        if (override != null) try {
            nl = (String) override.get("$nl$");
        } catch (ClassCastException e) {
        }
        nlVariants = nl == null ? NL_JAR_VARIANTS : buildNLVariants(nl);
        if (nl != null && nl.length() == 0) return null;
        URL result = null;
        for (int i = 0; i < nlVariants.length; i++) {
            IPath filePath = new Path(nlVariants[i]).append(path);
            result = findInPlugin(b, filePath);
            if (result != null) return result;
            result = findInFragments(b, filePath);
            if (result != null) return result;
        }
        result = findInPlugin(b, path);
        if (result != null) return result;
        return findInFragments(b, path);
    }

    private static URL findInPlugin(Bundle b, IPath filePath) {
        return b.getEntry(filePath.toString());
    }

    private static URL findInFragments(Bundle b, IPath filePath) {
        Bundle[] fragments = InternalPlatform.getDefault().getFragments(b);
        if (fragments == null) return null;
        URL fileURL = null;
        int i = 0;
        while (i < fragments.length && fileURL == null) {
            fileURL = fragments[i].getEntry(filePath.toString());
            i++;
        }
        return fileURL;
    }

    /**
	 * See doc on @link Platform#openStream(Bundle, IPath, boolean) Platform#Platform#openStream(Bundle, IPath, boolean) 
	 */
    public static final InputStream openStream(Bundle bundle, IPath file, boolean localized) throws IOException {
        URL url = null;
        if (!localized) {
            url = findInPlugin(bundle, file);
            if (url == null) url = findInFragments(bundle, file);
        } else {
            url = FindSupport.find(bundle, file);
        }
        if (url != null) return url.openStream();
        throw new IOException("Cannot find " + file.toString());
    }
}
