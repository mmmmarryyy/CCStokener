package org.apache.solr.common.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.util.ContentStreamBase;
import junit.framework.TestCase;

/**
 */
public class ContentStreamTest extends TestCase {

    public void testStringStream() throws IOException {
        String input = "aads ghaskdgasgldj asl sadg ajdsg &jag # @ hjsakg hsakdg hjkas s";
        ContentStreamBase stream = new ContentStreamBase.StringStream(input);
        assertEquals(input.length(), stream.getSize().intValue());
        assertEquals(input, IOUtils.toString(stream.getStream()));
        assertEquals(input, IOUtils.toString(stream.getReader()));
    }

    public void testFileStream() throws IOException {
        File file = new File("README");
        assertTrue(file.exists());
        ContentStreamBase stream = new ContentStreamBase.FileStream(file);
        assertEquals(file.length(), stream.getSize().intValue());
        assertTrue(IOUtils.contentEquals(new FileInputStream(file), stream.getStream()));
        assertTrue(IOUtils.contentEquals(new FileReader(file), stream.getReader()));
    }

    public void testURLStream() throws IOException {
        String content = null;
        URL url = new URL("http://svn.apache.org/repos/asf/lucene/solr/trunk/");
        InputStream in = url.openStream();
        try {
            content = IOUtils.toString(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
        assertTrue(content.length() > 10);
        ContentStreamBase stream = new ContentStreamBase.URLStream(url);
        assertEquals(content.length(), stream.getSize().intValue());
        in = stream.getStream();
        try {
            assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(content.getBytes()), in));
        } finally {
            IOUtils.closeQuietly(in);
        }
        stream = new ContentStreamBase.URLStream(url);
        assertTrue(IOUtils.contentEquals(new StringReader(content), stream.getReader()));
    }
}
