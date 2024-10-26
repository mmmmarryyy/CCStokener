package org.gamegineer.test.core;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestSuite;
import net.jcip.annotations.ThreadSafe;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * A JUnit test suite builder for OSGi bundles.
 * 
 * <p>
 * This class builds a JUnit test suite for all tests contained within an OSGi
 * bundle. It dynamically determines the bundle classpath on which to search for
 * tests. It properly handles both the case where the bundle is deployed in
 * source form (such as when run from an IDE) and when it is deployed in binary
 * form (such as when run from an installed product).
 * </p>
 */
@ThreadSafe
public final class BundleSuiteBuilder {

    /**
     * Initializes a new instance of the {@code BundleSuiteBuilder} class.
     */
    private BundleSuiteBuilder() {
    }

    private static Collection<String> getClasspathEntriesFromManifest(final Bundle bundle) {
        assert bundle != null;
        final String classpath = bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
        if (classpath == null) {
            return Collections.emptyList();
        }
        final List<String> classpathEntries = new ArrayList<String>();
        for (final String classpathEntry : classpath.split("\\s*,\\s*")) {
            classpathEntries.add(classpathEntry);
        }
        return Collections.unmodifiableList(classpathEntries);
    }

    private static Collection<String> getClasspathEntriesFromProject(final Bundle bundle) throws Exception {
        assert bundle != null;
        final URL url = bundle.getEntry(".classpath");
        if (url == null) {
            return Collections.emptyList();
        }
        final List<String> classpathEntries = new ArrayList<String>();
        final InputStream is = url.openStream();
        try {
            final InputSource inputSource = new InputSource(is);
            final XPath xpath = XPathFactory.newInstance().newXPath();
            final String expression = "/classpath/classpathentry[@kind='output']";
            final NodeList nodes = (NodeList) xpath.evaluate(expression, inputSource, XPathConstants.NODESET);
            for (int index = 0; index < nodes.getLength(); ++index) {
                final Element element = (Element) nodes.item(index);
                final String classpathEntry = element.getAttribute("path");
                if (classpathEntry != null) {
                    classpathEntries.add(classpathEntry);
                }
            }
        } finally {
            is.close();
        }
        return Collections.unmodifiableList(classpathEntries);
    }

    private static Collection<String> getTestClassNames(final Bundle bundle) throws Exception {
        assert bundle != null;
        final Collection<String> classNames = new ArrayList<String>();
        for (final String classpathEntry : getClasspathEntriesFromManifest(bundle)) {
            classNames.addAll(getTestClassNamesFromJar(bundle, classpathEntry));
        }
        if (!classNames.isEmpty()) {
            return classNames;
        }
        for (final String classpathEntry : getClasspathEntriesFromProject(bundle)) {
            classNames.addAll(getTestClassNamesFromPath(bundle, classpathEntry));
        }
        if (!classNames.isEmpty()) {
            return classNames;
        }
        return Collections.emptyList();
    }

    private static Collection<String> getTestClassNamesFromJar(final Bundle bundle, final String path) throws Exception {
        assert bundle != null;
        assert path != null;
        final URL url = bundle.getEntry(path);
        if (url == null) {
            return Collections.emptyList();
        }
        final JarInputStream is = new JarInputStream(url.openStream());
        final List<String> classNames = new ArrayList<String>();
        final Pattern CLASS_NAME_PATTERN = Pattern.compile("^(.*Test)\\.class$");
        try {
            JarEntry entry = null;
            while ((entry = is.getNextJarEntry()) != null) {
                final Matcher matcher = CLASS_NAME_PATTERN.matcher(entry.getName());
                if (matcher.matches()) {
                    classNames.add(matcher.group(1).replace('/', '.'));
                }
            }
        } finally {
            is.close();
        }
        return Collections.unmodifiableList(classNames);
    }

    private static Collection<String> getTestClassNamesFromPath(final Bundle bundle, final String path) {
        assert bundle != null;
        assert path != null;
        assert path.charAt(0) != '/';
        final List<String> classNames = new ArrayList<String>();
        final Pattern CLASS_NAME_PATTERN = Pattern.compile(String.format("^/%1$s/(.+)\\.class$", path));
        final String FILE_PATTERN = "*Test.class";
        for (final Enumeration<?> entries = bundle.findEntries(path, FILE_PATTERN, true); entries.hasMoreElements(); ) {
            final URL url = (URL) entries.nextElement();
            final Matcher matcher = CLASS_NAME_PATTERN.matcher(url.getPath());
            if (!matcher.matches()) {
                throw new RuntimeException("unexpected bundle entry URL format");
            }
            classNames.add(matcher.group(1).replace('/', '.'));
        }
        return Collections.unmodifiableList(classNames);
    }

    public static Test suite(final Bundle bundle) {
        if (bundle == null) {
            throw new NullPointerException("bundle");
        }
        try {
            return suite(bundle, (Bundle) null);
        } catch (final Exception e) {
            throw new RuntimeException("an error occurred while building the test suite", e);
        }
    }

    public static Test suite(final Bundle hostBundle, final String fragmentName) {
        if (hostBundle == null) {
            throw new NullPointerException("hostBundle");
        }
        if (fragmentName == null) {
            throw new NullPointerException("fragmentName");
        }
        final Bundle fragmentBundle = TestCases.getFragment(hostBundle, fragmentName);
        if (fragmentBundle == null) {
            throw new IllegalArgumentException(String.format("no such fragment '%1$s' in bundle '%2$s'", fragmentName, hostBundle.getSymbolicName()));
        }
        try {
            return suite(hostBundle, fragmentBundle);
        } catch (final Exception e) {
            throw new RuntimeException("an error occurred while building the test suite", e);
        }
    }

    private static Test suite(final Bundle hostBundle, final Bundle fragmentBundle) throws Exception {
        assert hostBundle != null;
        final Bundle targetBundle = (fragmentBundle != null) ? fragmentBundle : hostBundle;
        final TestSuite suite = new TestSuite(targetBundle.getSymbolicName());
        for (final String className : getTestClassNames(targetBundle)) {
            suite.addTest(new JUnit4TestAdapter(hostBundle.loadClass(className)));
        }
        return suite;
    }
}
