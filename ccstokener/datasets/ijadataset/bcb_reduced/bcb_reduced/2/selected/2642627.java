package info.monitorenter.cpdetector.io;

import info.monitorenter.util.FileUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test case for
 * <code>{@link info.monitorenter.cpdetector.io.CodepageDetectorProxy}</code>.
 * <p>
 * 
 * @author <a href="mailto:Achim.Westermann@gmx.de">Achim Westermann</a>
 * 
 * 
 * @version $Revision: 1.3 $
 */
public class CodePageDetectorProxyTest {

    /** The detector to use. */
    private CodepageDetectorProxy m_detector;

    /**
   * @see junit.framework.TestCase#setUp()
   */
    @Before
    public void setUp() throws Exception {
        this.m_detector = CodepageDetectorProxy.getInstance();
        this.m_detector.add(new ParsingDetector(true));
        this.m_detector.add(JChardetFacade.getInstance());
        this.m_detector.add(ASCIIDetector.getInstance());
    }

    /**
   * @see junit.framework.TestCase#tearDown()
   */
    @After
    public void tearDown() throws Exception {
        this.m_detector = null;
    }

    /**
   * Tests
   * <code>{@link CodepageDetectorProxy#detectCodepage(InputStream, int)}</code>
   * .
   * <p>
   * 
   * @throws IOException
   *           if something goes wrong.
   */
    @Test
    public void testDetectCodePageInputStream() throws IOException {
        InputStream in = null;
        try {
            Assert.assertNotNull(this.m_detector);
            File f = new File("testdocuments/stress/illegalHtmlTag/1111.htm");
            Assert.assertTrue("Test file " + f.getAbsolutePath() + " does not exist. ", f.exists());
            in = new BufferedInputStream(new FileInputStream(f));
            byte[] barr = new byte[50];
            in.mark(50);
            in.read(barr);
            in.reset();
            String originalStart = new String(barr);
            in.mark(100);
            Charset result = this.m_detector.detectCodepage(in, (int) f.length());
            System.out.println("Result: " + result);
            Assert.assertEquals(Charset.forName("utf-8"), result);
            in.reset();
            in.read(barr);
            String afterStart = new String(barr);
            Assert.assertEquals("Modification or stream position error.", originalStart, afterStart);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
   * Tests <code>{@link CodepageDetectorProxy#detectCodepage(URL)}</code>.
   * <p>
   * 
   * @throws IOException
   *           if sth. goes wrong.
   */
    @Test
    public void testDetectCodePageUrl() throws IOException {
        Assert.assertNotNull(this.m_detector);
        File f = new File("testdocuments/xml.ascc.net/test/wf/big5/text_xml/zh-big5-0.xml");
        Assert.assertTrue("Test file " + f.getAbsolutePath() + " does not exist. ", f.exists());
        URL url = f.toURL();
        Charset result = this.m_detector.detectCodepage(url);
        System.out.println("Result: " + result);
        Assert.assertEquals(Charset.forName("Big5"), result);
        byte[] barr = FileUtil.readRAM(f);
        boolean deleted = f.delete();
        Assert.assertTrue("Cannot delete " + f.getAbsolutePath() + " (has a lock?)", deleted);
        f.createNewFile();
        OutputStream out = new FileOutputStream(f);
        out.write(barr);
        out.flush();
        out.close();
        Assert.assertTrue("File " + f.getAbsolutePath() + " seems to be locked (open InputStream) after detection.", f.canWrite());
    }

    /**
   * Tests {@link CodepageDetectorProxy#detectCodepage(InputStream, int)} by
   * setting a read limit lower than file size with a file whose code page
   * cannot be detected (which ensures that all {@link ICodepageDetector}
   * instances will run over the file).
   * <p>
   * 
   * @throws IllegalArgumentException
   *           if something goes wrong.
   * 
   * @throws IOException
   *           if something related to I/O goes wrong.
   */
    @Test
    public void testMark() throws IllegalArgumentException, IOException {
        Assert.assertNotNull(this.m_detector);
        File f = new File("testdocuments/voiddocument/Voiderror.htm");
        Assert.assertTrue("Test file " + f.getAbsolutePath() + " does not exist. ", f.exists());
        URL url = f.toURL();
        this.m_detector.detectCodepage(url.openStream(), 200);
    }
}
