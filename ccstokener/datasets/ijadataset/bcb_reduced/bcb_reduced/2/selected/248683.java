package com.erinors.tapestry.tapdoc.service;

import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.io.IOUtils;

/**
 * @author Norbert Sándor
 */
public class DocResolverImpl implements DocResolver {

    public DocResolverImpl(TapdocContext context, FileNameGenerator fileNameGenerator) {
        this.context = context;
        this.fileNameGenerator = fileNameGenerator;
    }

    private final TapdocContext context;

    private final FileNameGenerator fileNameGenerator;

    private boolean initialized;

    private Map<String, String> javadocByPackage = new HashMap<String, String>();

    private void initialize() {
        if (!initialized) {
            if (context.getJavadocLinks() != null) {
                for (String url : context.getJavadocLinks()) {
                    if (!url.endsWith("/")) {
                        url += "/";
                    }
                    StringWriter writer = new StringWriter();
                    try {
                        IOUtils.copy(new URL(url + "package-list").openStream(), writer);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    StringTokenizer tokenizer2 = new StringTokenizer(writer.toString());
                    while (tokenizer2.hasMoreTokens()) {
                        javadocByPackage.put(tokenizer2.nextToken(), url);
                    }
                }
            }
            initialized = true;
        }
    }

    public String getJavadocUrl(String type, String member) {
        initialize();
        String javadocUrl = null;
        if (type != null && type.length() > 0) {
            if (type.endsWith("[]")) {
                type = type.substring(0, type.length() - 2);
            }
            String packageName = type.indexOf('.') != -1 ? type.substring(0, type.lastIndexOf('.')) : "";
            if (javadocByPackage.containsKey(packageName)) {
                javadocUrl = javadocByPackage.get(packageName) + type.replace('.', '/') + ".html";
            }
            if (javadocUrl != null && member != null && member.length() > 0) {
                javadocUrl += "#" + member;
            }
        }
        return javadocUrl;
    }

    public String getLibraryUrl(String libraryLocation) {
        initialize();
        if (context.getLibraryLocations().contains(libraryLocation)) {
            return fileNameGenerator.getLibraryDirectory(libraryLocation);
        } else {
            return null;
        }
    }

    public String getComponentUrl(String libraryLocation, String componentName) {
        initialize();
        if (context.getLibraryLocations().contains(libraryLocation)) {
            return fileNameGenerator.getComponentIndexFile(libraryLocation, componentName, false);
        } else {
            return null;
        }
    }
}
