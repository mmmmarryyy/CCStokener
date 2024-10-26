package org.illico.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.illico.common.lang.Exception;
import org.illico.common.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ManifestUtils {

    private static Map<String, String> mainPackageVersions = null;

    private static final Logger logger = LoggerFactory.getLogger(ManifestUtils.class);

    public static final String DEFAULT_PATH = "META-INF/MANIFEST.MF";

    public static final String MAIN_PACKAGE_ATTRIBUTE_NAME = "Main-Package";

    private ManifestUtils() {
    }

    private static synchronized Map<String, String> getMainPackageVersions() {
        if (mainPackageVersions == null) {
            mainPackageVersions = new HashMap<String, String>();
            for (Manifest mf : getManifests()) {
                String mainPackage = mf.getMainAttributes().getValue(MAIN_PACKAGE_ATTRIBUTE_NAME);
                String version = mf.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                if (!StringUtils.isEmpty(mainPackage) && !StringUtils.isEmpty(version)) {
                    logger.debug("{} = {}", mainPackage, version);
                    mainPackageVersions.put(mainPackage, version);
                }
            }
        }
        return mainPackageVersions;
    }

    public static Collection<Manifest> getManifests(String path) {
        Collection<Manifest> result = new HashSet<Manifest>();
        try {
            List<URL> urls = Collections.list(PropertiesUtils.class.getClassLoader().getResources(path));
            for (URL url : urls) {
                InputStream is = url.openStream();
                try {
                    Manifest mf = new Manifest(is);
                    result.add(mf);
                } finally {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new Exception(e);
        }
        return result;
    }

    public static Collection<Manifest> getManifests() {
        return getManifests(DEFAULT_PATH);
    }

    public static String getVersion(String mainPackage) {
        String result = getMainPackageVersions().get(mainPackage);
        if (result == null) {
            int lastDotIndex = mainPackage.lastIndexOf('.');
            if (lastDotIndex > -1) {
                result = getVersion(mainPackage.substring(0, lastDotIndex));
            }
        }
        return result;
    }
}
