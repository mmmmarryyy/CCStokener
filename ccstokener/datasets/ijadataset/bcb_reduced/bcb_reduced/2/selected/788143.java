package org.colombbus.tangara.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import org.colombbus.tangara.util.ExternalJarClassLoader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("nls")
public class ExternalJarClassLoaderTest {

    private static final String RESOURCE_KEY = "HelloWorld.class";

    private static final String CLASSNAME_GAME_2 = "org.colombbus.tangara.extension.game.english.Dice";

    private static final String CLASSNAME_GAME_1 = "org.colombbus.tangara.extension.game.francais.De";

    private static final File GAME_EXTENSION_JAR = new File("test/data/extension/org.colombbus.tangara.extension.game-1.0/org.colombbus.tangara.extension.game-1.0.jar");

    private static final File BASE_EXTENSION_JAR = new File("test/data/extension/org.colombbus.tangara.extension.base-1.0.1/org.colombbus.tangara.extension.base-1.0.1.jar");

    private static final String CLASSNAME_BASE_2 = "org.colombbus.tangara.extension.base.english.HelloWorld";

    private static final String CLASSNAME_BASE_1 = "org.colombbus.tangara.extension.base.francais.BonjourLeMonde";

    private static final String RESOURCE_PATH = "org/colombbus/tangara/extension/base/localize_en.properties";

    private static final String MISSING_RESOURCE_PATH = "org/colombbus/extern/objects/MissingResource.properties";

    private static final File NON_EXIST_JAR_FILE = new File("src/test/data/extern-objects/nonexist.jar");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testExternalJarClassLoader() {
        new ExternalJarClassLoader();
    }

    @Test
    public void testExternalJarClassLoaderWithArg() {
        ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
        new ExternalJarClassLoader(currentCl);
    }

    @Test
    public void testRegisterJar() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNullJar() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNonExistJar() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(NON_EXIST_JAR_FILE);
    }

    @Test
    public void testUnregisterJar() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
        cl.unregisterJar(BASE_EXTENSION_JAR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnregisterNullJar() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.unregisterJar(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnregisterNonRegisteredJar() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.unregisterJar(BASE_EXTENSION_JAR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnregisterNonExistJar() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.unregisterJar(NON_EXIST_JAR_FILE);
    }

    @Test
    public void testIsJarRegistered() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        assertFalse(cl.isJarRegistered(BASE_EXTENSION_JAR));
        cl.registerJar(BASE_EXTENSION_JAR);
        assertTrue(cl.isJarRegistered(BASE_EXTENSION_JAR));
        cl.unregisterJar(BASE_EXTENSION_JAR);
        assertFalse(cl.isJarRegistered(BASE_EXTENSION_JAR));
    }

    @Test
    public void testCloseJars() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
        Class<?> loadedClass = cl.loadClass(CLASSNAME_BASE_1);
        assertNotNull(loadedClass);
        cl.closeJars();
    }

    @Test
    public void testCloseJarsNoJars() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.closeJars();
    }

    @Test
    public void testCloseJarsNoOpenedJars() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
        cl.closeJars();
    }

    @Test()
    public void testFindClass() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
        Class<?> class1 = cl.findClass(CLASSNAME_BASE_1);
        assertNotNull(class1);
        Class<?> class2 = cl.findClass(CLASSNAME_BASE_2);
        assertNotNull(class2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindNullClass() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.findClass(null);
    }

    @Test(expected = ClassNotFoundException.class)
    public void testFindUnknownClass() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.findClass("this.is.an.UnknownClass");
    }

    @Test
    public void testFindResource() throws IllegalArgumentException, IOException {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
        URL url = cl.findResource(RESOURCE_PATH);
        assertNotNull(url);
        Properties urlContent = new Properties();
        urlContent.load(url.openStream());
        assertTrue(urlContent.containsKey(RESOURCE_KEY));
    }

    @Test
    public void testFindNullResource() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
        URL url = cl.findResource(null);
        assertNull(url);
    }

    @Test
    public void testFindMissingResourceResource() throws IllegalArgumentException, IOException {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
        URL url = cl.findResource(MISSING_RESOURCE_PATH);
        assertNull(url);
    }

    @Test
    public void testFindNullResources() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
        Enumeration<URL> url = cl.findResources(null);
        assertNull(url);
    }

    @Test
    public void testFindResources() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
        Enumeration<URL> urlEnum = cl.findResources(RESOURCE_PATH);
        assertNotNull(urlEnum);
        assertTrue(urlEnum.hasMoreElements());
        URL firstUrl = urlEnum.nextElement();
        assertNotNull(firstUrl);
        assertFalse(urlEnum.hasMoreElements());
    }

    @Test
    public void testURL() throws Exception {
        printResourceUrl("log4j.properties");
        printResourceUrl("org/colombbus/tangara/engine/script/resources/messages.properties");
        printResourceUrl("org/colombbus/tangara/util/ExternalJarClassLoaderTest.class");
        printResourceUrl("groovy/ui/icons/disk.png");
        printResourceUrl("groovy/ui/Console.class");
        System.out.println("-->" + BASE_EXTENSION_JAR.toURI().toURL());
    }

    private void printResourceUrl(String resourceName) {
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource(resourceName);
        assertNotNull(url);
        System.out.println("Resource '" + resourceName + "' has url '" + url.toExternalForm());
    }

    @Test
    public void testMultiJars() throws Exception {
        ExternalJarClassLoader cl = new ExternalJarClassLoader();
        cl.registerJar(BASE_EXTENSION_JAR);
        cl.registerJar(GAME_EXTENSION_JAR);
        assertNotNull(cl.findClass(CLASSNAME_BASE_1));
        assertNotNull(cl.findClass(CLASSNAME_BASE_2));
        assertNotNull(cl.findClass(CLASSNAME_GAME_1));
        assertNotNull(cl.findClass(CLASSNAME_GAME_2));
    }
}
