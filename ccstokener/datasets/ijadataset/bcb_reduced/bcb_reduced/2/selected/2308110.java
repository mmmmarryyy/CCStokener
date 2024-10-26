package net.sf.csutils.core.model.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.csutils.core.model.ROMetaModel;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

public class ROMetaModelReaderTest extends AbstractMetaModelTestCase {

    @Test
    public void testReadInputStream() throws Exception {
        final URL url = getClass().getResource("registryObjectModel.xml");
        Assert.assertNotNull(url);
        final ROMetaModel model = new ROMetaModelReader().read(url.openStream());
        validateModel(model);
    }

    private void copy(InputStream pIn, OutputStream pOut) throws IOException {
        InputStream in = pIn;
        OutputStream out = pOut;
        try {
            final byte[] buffer = new byte[8192];
            for (; ; ) {
                int res = in.read(buffer);
                if (res == -1) {
                    in.close();
                    in = null;
                    out.close();
                    out = null;
                    return;
                }
                if (res > 0) {
                    out.write(buffer, 0, res);
                }
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable t) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    @Test
    public void testReadFile() throws Exception {
        final URL url = getClass().getResource("registryObjectModel.xml");
        Assert.assertNotNull(url);
        File file = File.createTempFile("ROMetaModelReaderTest", ".xml");
        file.deleteOnExit();
        try {
            copy(url.openStream(), new FileOutputStream(file));
            final ROMetaModel model = new ROMetaModelReader().read(file);
            validateModel(model);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testReadInputSource() throws Exception {
        final URL url = getClass().getResource("registryObjectModel.xml");
        Assert.assertNotNull(url);
        final InputSource isource = new InputSource(url.openStream());
        isource.setSystemId(url.toExternalForm());
        final ROMetaModel model = new ROMetaModelReader().read(isource);
        validateModel(model);
    }

    @Test
    public void testReadReader() throws Exception {
        final URL url = getClass().getResource("registryObjectModel.xml");
        Assert.assertNotNull(url);
        final Transformer t = TransformerFactory.newInstance().newTransformer();
        final StringWriter sw = new StringWriter();
        t.transform(new StreamSource(url.openStream()), new StreamResult(sw));
        final ROMetaModel model = new ROMetaModelReader().read(new StringReader(sw.toString()));
        validateModel(model);
    }

    @Test
    public void testReadNode() throws Exception {
        final URL url = getClass().getResource("registryObjectModel.xml");
        Assert.assertNotNull(url);
        final DOMResult domResult = new DOMResult();
        final Transformer t = TransformerFactory.newInstance().newTransformer();
        t.transform(new StreamSource(url.openStream()), domResult);
        final ROMetaModel model = new ROMetaModelReader().read(domResult.getNode());
        validateModel(model);
    }

    @Test
    public void testReadSource() throws Exception {
        final URL url = getClass().getResource("registryObjectModel.xml");
        final ROMetaModel model = new ROMetaModelReader().read(new StreamSource(url.openStream()));
        validateModel(model);
    }

    @Test
    public void testReadURL() throws Exception {
        final URL url = getClass().getResource("registryObjectModel.xml");
        Assert.assertNotNull(url);
        final ROMetaModel model = new ROMetaModelReader().read(url);
        validateModel(model);
    }

    @Test
    public void testReadXMLEventReader() throws Exception {
        final URL url = getClass().getResource("registryObjectModel.xml");
        Assert.assertNotNull(url);
        XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(url.openStream());
        final ROMetaModel model = new ROMetaModelReader().read(reader);
        validateModel(model);
    }

    @Test
    public void testReadXMLStreamReader() throws Exception {
        final URL url = getClass().getResource("registryObjectModel.xml");
        Assert.assertNotNull(url);
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(url.openStream());
        final ROMetaModel model = new ROMetaModelReader().read(reader);
        validateModel(model);
    }
}
