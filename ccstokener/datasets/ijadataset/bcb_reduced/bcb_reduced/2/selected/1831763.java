package pl.otros.logview.io;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs.*;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.otros.logview.LogData;
import pl.otros.logview.importer.LogImporter;
import pl.otros.logview.importer.UtilLoggingXmlLogImporter;
import pl.otros.logview.parser.ParsingContext;
import pl.otros.logview.reader.ProxyLogDataCollector;
import java.io.*;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtilsTest {

    private static final String HTTP_GZIPPED = "http://otroslogviewer.googlecode.com/svn/trunk/OtrosLogViewer-app/src/test/resources/hierarchy/hierarchy.log.gz";

    private static final String HTTP_NOT_GZIPPED = "http://otroslogviewer.googlecode.com/svn/trunk/OtrosLogViewer-app/src/test/resources/hierarchy/hierarchy.log";

    public static FileSystemManager fsManager;

    @BeforeClass
    public static void setUp() throws FileSystemException {
        fsManager = VFS.getManager();
    }

    @Test
    public void testEmptyFile() throws IOException {
        FileObject resolveFile = resolveFileObject("/empty.log");
        assertEquals(0, resolveFile.getContent().getSize());
        boolean checkIfIsGzipped = Utils.checkIfIsGzipped(resolveFile);
        assertFalse(checkIfIsGzipped);
    }

    @Test
    public void testEmptyGzipedFile() throws IOException {
        FileObject resolveFile = resolveFileObject("/empty.log.gz");
        assertEquals(26, resolveFile.getContent().getSize());
        boolean checkIfIsGzipped = Utils.checkIfIsGzipped(resolveFile);
        assertTrue(checkIfIsGzipped);
    }

    @Test
    public void testGzipedFile() throws IOException {
        FileObject resolveFile = resolveFileObject("/jul_log.txt.gz");
        boolean checkIfIsGzipped = Utils.checkIfIsGzipped(resolveFile);
        assertTrue(resolveFile.getName() + " should be compressed", checkIfIsGzipped);
    }

    @Test
    public void testNotGzipedFile() throws IOException {
        FileObject resolveFile = resolveFileObject("/jul_log.txt");
        boolean checkIfIsGzipped = Utils.checkIfIsGzipped(resolveFile);
        assertFalse(resolveFile.getName() + " should be not compressed", checkIfIsGzipped);
    }

    @Test
    public void testSmallGzipedFile() throws IOException {
        FileObject resolveFile = resolveFileObject("/smallFile.txt.gz");
        boolean checkIfIsGzipped = Utils.checkIfIsGzipped(resolveFile);
        assertTrue(resolveFile.getName() + " should be compressed", checkIfIsGzipped);
    }

    private FileObject resolveFileObject(String resources) throws FileSystemException {
        URL resource = this.getClass().getResource(resources);
        FileObject resolveFile = fsManager.resolveFile(resource.toExternalForm());
        return resolveFile;
    }

    @Test
    public void testLoadHttpNotGzipped() throws Exception {
        String url = HTTP_NOT_GZIPPED;
        LoadingInfo loadingInfo = Utils.openFileObject(fsManager.resolveFile(url));
        InputStream contentInputStream = loadingInfo.getContentInputStream();
        byte[] actual = IOUtils.toByteArray(contentInputStream);
        byte[] expected = IOUtils.toByteArray(fsManager.resolveFile(url).getContent().getInputStream());
        assertEquals(expected.length, actual.length);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testLoadHttpNotGzippedBufferedReader() throws Exception {
        String url = HTTP_NOT_GZIPPED;
        LoadingInfo loadingInfo = Utils.openFileObject(fsManager.resolveFile(url));
        InputStream contentInputStream = loadingInfo.getContentInputStream();
        LineNumberReader bin = new LineNumberReader(new InputStreamReader(contentInputStream));
        int lines = 0;
        while (bin.readLine() != null) {
            lines++;
        }
        assertEquals(2600, lines);
    }

    @Test
    public void testLoadHttpGzipped() throws Exception {
        String url = HTTP_GZIPPED;
        LoadingInfo loadingInfo = Utils.openFileObject(fsManager.resolveFile(url));
        InputStream contentInputStream = loadingInfo.getContentInputStream();
        byte[] actual = IOUtils.toByteArray(contentInputStream);
        byte[] expected = IOUtils.toByteArray(new GZIPInputStream(new URL(url).openStream()));
        assertEquals(expected.length, actual.length);
    }

    @Test
    public void testLoadLocalNotGzipped() throws Exception {
        FileObject fileObject = resolveFileObject("/hierarchy/hierarchy.log");
        LoadingInfo loadingInfo = Utils.openFileObject(fileObject);
        InputStream contentInputStream = loadingInfo.getContentInputStream();
        byte[] actual = IOUtils.toByteArray(contentInputStream);
        byte[] expected = IOUtils.toByteArray(fileObject.getContent().getInputStream());
        assertFalse(loadingInfo.isGziped());
        assertEquals(expected.length, actual.length);
    }

    @Test
    public void testLoadLocalGzipped() throws Exception {
        FileObject fileObject = resolveFileObject("/hierarchy/hierarchy.log.gz");
        LoadingInfo loadingInfo = Utils.openFileObject(fileObject);
        InputStream contentInputStream = loadingInfo.getContentInputStream();
        byte[] actual = IOUtils.toByteArray(contentInputStream);
        byte[] expected = IOUtils.toByteArray(new GZIPInputStream(fileObject.getContent().getInputStream()));
        assertTrue(loadingInfo.isGziped());
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testSequeceRead() throws Exception {
        String url = HTTP_NOT_GZIPPED;
        FileObject resolveFile = fsManager.resolveFile(url);
        InputStream httpInputStream = resolveFile.getContent().getInputStream();
        byte[] buff = Utils.loadProbe(httpInputStream, 10000);
        ByteArrayInputStream bin = new ByteArrayInputStream(buff);
        SequenceInputStream sequenceInputStream = new SequenceInputStream(bin, httpInputStream);
        byte[] byteArray = IOUtils.toByteArray(new ObservableInputStreamImpl(sequenceInputStream));
        LoadingInfo loadingInfo = Utils.openFileObject(fsManager.resolveFile(url), false);
        byte[] byteArrayUtils = IOUtils.toByteArray(loadingInfo.getContentInputStream());
        assertEquals(byteArrayUtils.length, byteArray.length);
    }

    @Test
    public void testSequeceReadGzipped() throws Exception {
        String url = HTTP_GZIPPED;
        FileObject resolveFile = fsManager.resolveFile(url);
        InputStream httpInputStream = resolveFile.getContent().getInputStream();
        byte[] buff = Utils.loadProbe(httpInputStream, 10000);
        ByteArrayInputStream bin = new ByteArrayInputStream(buff);
        SequenceInputStream sequenceInputStream = new SequenceInputStream(bin, httpInputStream);
        byte[] byteArray = IOUtils.toByteArray(new GZIPInputStream(new ObservableInputStreamImpl(sequenceInputStream)));
        LoadingInfo loadingInfo = Utils.openFileObject(fsManager.resolveFile(url), false);
        byte[] byteArrayUtils = IOUtils.toByteArray(loadingInfo.getContentInputStream());
        assertEquals(byteArrayUtils.length, byteArray.length);
    }

    @Test
    public void testLoadingLog() throws Exception {
        LoadingInfo loadingInfo = Utils.openFileObject(fsManager.resolveFile(HTTP_GZIPPED), false);
        LogImporter importer = new UtilLoggingXmlLogImporter();
        importer.init(new Properties());
        ParsingContext parsingContext = new ParsingContext("");
        importer.initParsingContext(parsingContext);
        ProxyLogDataCollector proxyLogDataCollector = new ProxyLogDataCollector();
        importer.importLogs(loadingInfo.getContentInputStream(), proxyLogDataCollector, parsingContext);
        LogData[] logData = proxyLogDataCollector.getLogData();
        assertEquals(236, logData.length);
    }

    @Test
    public void getFileObjectShortNameIp() throws Exception {
        String scheme = "sftp";
        String url = "sftp://10.0.22.3/logs/out.log";
        String baseName = "out.log";
        String output = "sftp://10.0.22.3/out.log";
        testGetObjectShortName(scheme, url, baseName, output);
    }

    @Test
    public void getFileObjectShortNameLongHost() throws Exception {
        String scheme = "sftp";
        String url = "sftp://machine.a.b.com/logs/out.log";
        String baseName = "out.log";
        String output = "sftp://machine/out.log";
        testGetObjectShortName(scheme, url, baseName, output);
    }

    @Test
    public void getFileObjectShortNameShortHost() throws Exception {
        String scheme = "sftp";
        String url = "sftp://machine/logs/out.log";
        String baseName = "out.log";
        String output = "sftp://machine/out.log";
        testGetObjectShortName(scheme, url, baseName, output);
    }

    @Test
    public void getFileObjectShortNameLocalFile() throws Exception {
        String scheme = "file";
        String url = "file://opt/logs/out.log";
        String baseName = "out.log";
        String output = "file://out.log";
        testGetObjectShortName(scheme, url, baseName, output);
    }

    private void testGetObjectShortName(String scheme, String url, String baseName, String output) {
        FileObject fileObjectMock = mock(FileObject.class);
        FileName fileNameMock = mock(FileName.class);
        when(fileObjectMock.getName()).thenReturn(fileNameMock);
        when(fileNameMock.getScheme()).thenReturn(scheme);
        when(fileNameMock.getURI()).thenReturn(url);
        when(fileNameMock.getBaseName()).thenReturn(baseName);
        String fileObjectShortName = Utils.getFileObjectShortName(fileObjectMock);
        assertEquals(output, fileObjectShortName);
    }

    @Test
    public void testLoadProbeEmpty() throws IOException {
        byte[] loadProbe = Utils.loadProbe(new ByteArrayInputStream(new byte[0]), 1024);
        assertEquals(0, loadProbe.length);
    }
}
