package joj.core;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import joj.web.ContainerHookedObjectsTestBase;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Jason Miller (heinousjay@gmail.com)
 *
 */
@RunWith(JMock.class)
public class ModulesTest extends ContainerHookedObjectsTestBase {

    private static final String WEB_INF_LIB_PATH = "/WEB-INF/lib/";

    private static final String ANOTHER_THING_JAR = WEB_INF_LIB_PATH + "another-thing.jar";

    private static final String SOMETHING_ELSE_JAR = WEB_INF_LIB_PATH + "something-else.jar";

    private static final String SOMETHING_JAR = WEB_INF_LIB_PATH + "something.jar";

    private static final String NOT_A_JAR = WEB_INF_LIB_PATH + "nothing";

    @Before
    public void before() {
        super.setUp();
    }

    @Test
    public void testCheckIsConfigClass() throws Exception {
        final URL configURL = getClass().getResource("/config/Config.class");
        final URL notConfigURL = getClass().getResource("/config/NotConfig.class");
        assertThat(new ModulesImpl(servletContext, null, new LoggerProvider()).isValidConfigClass(configURL), is(true));
        assertThat(new ModulesImpl(servletContext, null, new LoggerProvider()).isValidConfigClass(notConfigURL), is(false));
    }

    @Test
    public void testFindClassesInConfig() throws Exception {
        final URL configURL = getClass().getResource("/config/Config.class");
        final String basePath = "/WEB-INF/classes/";
        final String path1 = basePath + "org/";
        final String path2 = path1 + "something/";
        final String path3_1 = path2 + "crap/";
        final String path3_2 = path2 + "morecrap/";
        final String path3_3 = path2 + "Config.class";
        final String path4_1 = path3_1 + "Nothing.class";
        final String path4_2 = path3_2 + "AlsoNothing.class";
        final Set<String> one = new HashSet<String>();
        one.add(path1);
        final Set<String> two = new HashSet<String>();
        two.add(path2);
        final Set<String> three = new HashSet<String>();
        three.add(path3_1);
        three.add(path3_2);
        three.add(path3_3);
        final Set<String> four = new HashSet<String>();
        four.add(path4_1);
        final Set<String> five = new HashSet<String>();
        five.add(path4_2);
        context.checking(new Expectations() {

            {
                one(servletContext).getResourcePaths(basePath);
                will(returnValue(one));
                one(servletContext).getResourcePaths(path1);
                will(returnValue(two));
                one(servletContext).getResourcePaths(path2);
                will(returnValue(three));
                one(servletContext).getResource(path3_3);
                will(returnValue(configURL));
            }
        });
        final URL output = new ModulesImpl(servletContext, null, new LoggerProvider()).findConfigInClassesDirectory();
        assertThat(output, is(configURL));
    }

    @Test
    public void testFindStandardModules() throws Exception {
        final ModulesImpl modules = new ModulesImpl(servletContext, null, new LoggerProvider());
        modules.addStandardModules();
        assertThat(modules.getModuleCount(), is(1));
    }

    @Test
    public void testGetJarInformation() throws Exception {
        final URL url1 = getClass().getResource("/fakejars/something");
        final URL url2 = getClass().getResource("/fakejars/something-else");
        final URL url3 = getClass().getResource("/fakejars/another-thing");
        final Map<String, Date> paths = new HashMap<String, Date>();
        paths.put(SOMETHING_JAR, new Date(url1.openConnection().getLastModified()));
        paths.put(SOMETHING_ELSE_JAR, new Date(url2.openConnection().getLastModified()));
        paths.put(ANOTHER_THING_JAR, new Date(url3.openConnection().getLastModified()));
        paths.put(NOT_A_JAR, null);
        context.checking(new Expectations() {

            {
                one(servletContext).getResourcePaths(WEB_INF_LIB_PATH);
                will(returnValue(paths.keySet()));
                one(servletContext).getResource(SOMETHING_JAR);
                will(returnValue(url1));
                one(servletContext).getResource(SOMETHING_ELSE_JAR);
                will(returnValue(url2));
                one(servletContext).getResource(ANOTHER_THING_JAR);
                will(returnValue(url3));
            }
        });
        final Map<URL, Date> output = new ModulesImpl(servletContext, null, new LoggerProvider()).getJarInformation();
        assertThat(output.size(), is(3));
        for (final URL url : output.keySet()) {
            final String jarName = url.toString();
            final String key = WEB_INF_LIB_PATH + jarName.substring(jarName.lastIndexOf("/") + 1) + ".jar";
            assertThat(output.get(url), is(paths.get(key)));
        }
    }
}
