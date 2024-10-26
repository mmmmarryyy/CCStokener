package com.itextpdf.tool.xml.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.itextpdf.text.log.Logger;
import com.itextpdf.text.log.LoggerFactory;
import com.itextpdf.text.log.SysoLogger;
import com.itextpdf.tool.xml.exceptions.RuntimeWorkerException;

/**
 * @author redlab_b
 *
 */
public class FileRetrieveTest {

    static {
        LoggerFactory.getInstance().setLogger(new SysoLogger(3));
    }

    private static final Logger LOG = LoggerFactory.getLogger(FileRetrieveTest.class);

    private FileRetrieveImpl retriever;

    private File expected;

    private File actual;

    @Before
    public void setup() {
        retriever = new FileRetrieveImpl();
        actual = new File("./target/test-classes/css/actual.css");
        expected = new File("./target/test-classes/css/test.css");
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(actual);
    }

    @Test
    public void retrieveURL() throws IOException {
        boolean execute = true;
        try {
            URL url = new URL("http://www.redlab.be/test/test.css");
            url.openConnection().connect();
        } catch (SocketTimeoutException e) {
            LOG.info("Skipping retrieve from URL test as we cannot open the url itself. Maybe no internet connection. Marking test as Success");
            execute = false;
        } catch (IOException e) {
            LOG.info("Skipping retrieve from URL test as we cannot open the url itself. Maybe no internet connection. Marking test as Success");
            execute = false;
        }
        if (execute) {
            final FileOutputStream out = new FileOutputStream(actual);
            retriever.processFromHref("http://www.redlab.be/test/test.css", new ReadingProcessor() {

                public void process(final int inbit) {
                    try {
                        out.write((char) inbit);
                    } catch (IOException e) {
                        throw new RuntimeWorkerException(e);
                    }
                }
            });
            out.close();
        }
    }

    @Test
    public void retrieveStreamFromFile() throws MalformedURLException, IOException {
        final FileOutputStream out = new FileOutputStream(actual);
        InputStream css = FileRetrieveTest.class.getResourceAsStream("/css/test.css");
        retriever.processFromStream(css, new ReadingProcessor() {

            public void process(final int inbit) {
                try {
                    out.write((char) inbit);
                } catch (IOException e) {
                    throw new RuntimeWorkerException(e);
                }
            }
        });
        css.close();
        out.close();
        Assert.assertTrue(FileUtils.contentEquals(expected, actual));
    }

    @Test
    public void retrieveFile() throws MalformedURLException, IOException {
        final FileOutputStream out = new FileOutputStream(actual);
        retriever.addRootDir(new File("./target/test-classes"));
        retriever.processFromHref("/css/test.css", new ReadingProcessor() {

            public void process(final int inbit) {
                try {
                    out.write((char) inbit);
                } catch (IOException e) {
                    throw new RuntimeWorkerException(e);
                }
            }
        });
        out.close();
        Assert.assertTrue(FileUtils.contentEquals(expected, actual));
    }
}
