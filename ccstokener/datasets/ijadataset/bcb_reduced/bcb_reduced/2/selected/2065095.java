package org.springframework.richclient.image;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Tests the "image:" URL protocol handler.
 * 
 * @author oliverh
 */
public class HandlerTest extends TestCase {

    private static boolean imageHasNotBeenInstalledInThisJVM = true;

    /**
     * NOTE: This must be one big test method because of the static dependency
     * introduced by the strange way Java requires custom URL handlers to be
     * registered.
     */
    public void testHandler() throws MalformedURLException, IOException {
        assertTrue("This test can only be run once in a single JVM", imageHasNotBeenInstalledInThisJVM);
        URL url;
        Handler.installImageUrlHandler((ImageSource) new ClassPathXmlApplicationContext("org/springframework/richclient/image/application-context.xml").getBean("imageSource"));
        try {
            url = new URL("image:test");
            imageHasNotBeenInstalledInThisJVM = false;
        } catch (MalformedURLException e) {
            fail("protocol was not installed");
        }
        url = new URL("image:image.that.does.not.exist");
        try {
            url.openConnection();
            fail();
        } catch (NoSuchImageResourceException e) {
        }
        url = new URL("image:test.image.key");
        url.openConnection();
    }
}
