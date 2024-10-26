package exogwt.server.jcr.url;

import exogwt.server.AbstractServiceTest;
import javax.jcr.Node;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.net.URL;

public class JcrUrlConnectionTest extends AbstractServiceTest {

    public void setUp() throws Exception {
        Node root = getSession().getRootNode();
        Node file = root.addNode("file1", "nt:file");
        Node contentNode = file.addNode("jcr:content", "nt:resource");
        contentNode.setProperty("jcr:data", new ByteArrayInputStream("this is the content".getBytes()));
        contentNode.setProperty("jcr:mimeType", "text/html");
        contentNode.setProperty("jcr:lastModified", getSession().getValueFactory().createValue(Calendar.getInstance()));
        getSession().save();
    }

    public void testJcrURL() throws Exception {
        URL url = new URL(null, "jcr://exo:exo@ws/file1", new JcrURLStreamHandler(getRepositoryService()));
        assertNotNull("url not null", url);
        InputStream inputStream = url.openStream();
        assertNotNull("inputStream not null", inputStream);
        byte[] buf = new byte[inputStream.available()];
        inputStream.read(buf);
        String content = new String(buf);
        assertEquals("content", "this is the content", content);
        inputStream.close();
    }
}
