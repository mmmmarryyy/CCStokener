package net.disy.legato.net.protocol.classpath.test;

import java.io.InputStream;
import java.net.URL;
import junit.framework.Assert;
import net.disy.legato.net.protocol.classpath.Handler;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ClassPathProtocolTest {

    @Test
    public void test() throws Exception {
        Handler.register();
        final URL url = new URL("classpath://none/" + getClass().getName().replace('.', '/') + ".class");
        InputStream is = null;
        try {
            is = url.openConnection().getInputStream();
            Assert.assertNotNull(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}
