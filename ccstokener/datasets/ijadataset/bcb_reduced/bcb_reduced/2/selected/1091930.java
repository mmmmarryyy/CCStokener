package sanity;

import compatible.PrintProperties;
import junit.framework.Assert;
import org.junit.Test;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * @author Petr Kozelka
 */
public class PrintPropertiesTest {

    @Test
    public void justTry() throws IOException {
        PrintProperties.main(new String[] {});
    }

    /**
     * Checks that {@link PrintProperties} class is really compiled for target jdk version "1.2" which corresponds to class version 46.
     * Credits: http://www.rgagnon.com/javadetails/java-0544.html
     * @throws IOException -
     */
    @Test
    public void verifyJavaClassVersion() throws IOException {
        final Class<PrintProperties> cls = PrintProperties.class;
        final URL url = cls.getResource(cls.getSimpleName() + ".class");
        final DataInputStream in = new DataInputStream(url.openStream());
        try {
            final int magic = in.readInt();
            Assert.assertEquals(cls + " is invalid - magic is not 0xCAFEBABE", Integer.toHexString(0xcafebabe), Integer.toHexString(magic));
            final int minor = in.readUnsignedShort();
            final int major = in.readUnsignedShort();
            System.out.println("java.class.version of " + cls + ": " + major + "." + minor);
            Assert.assertEquals("Bad minor version", 0, minor);
            Assert.assertEquals("Bad major version", 46, major);
        } finally {
            in.close();
        }
    }
}
