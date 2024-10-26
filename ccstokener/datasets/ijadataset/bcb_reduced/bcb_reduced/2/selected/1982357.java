package br.net.woodstock.rockframework.core.test.io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import junit.framework.TestCase;
import br.net.woodstock.rockframework.utils.IOUtils;

public class IOTest extends TestCase {

    private static final String URL = "http://localhost:8080/cache-test/video.mp4";

    private static final String FILE = "C:/Temp/66_anos_tse.mp4";

    public void xtestURL1() throws Exception {
        URL url = new URL(IOTest.URL);
        InputStream inputStream = url.openStream();
        OutputStream outputStream = new FileOutputStream("C:/Temp/testURL1.mp4");
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
    }

    public void xtestURL2() throws Exception {
        URL url = new URL(IOTest.URL);
        InputStream inputStream = url.openStream();
        OutputStream outputStream = new FileOutputStream("C:/Temp/testURL2.mp4");
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
    }

    public void xtestFile1() throws Exception {
        InputStream inputStream = new FileInputStream(IOTest.FILE);
        OutputStream outputStream = new FileOutputStream("C:/Temp/testFile1.mp4");
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
    }

    public void xtestFile2() throws Exception {
        InputStream inputStream = new FileInputStream(IOTest.FILE);
        OutputStream outputStream = new FileOutputStream("C:/Temp/testFile2.mp4");
        IOUtils.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
    }

    public void test3() throws Exception {
        FileInputStream fileInputStream = new FileInputStream("/tmp/carimbo.p7s");
        FileOutputStream fileOutputStream1 = new FileOutputStream("/tmp/carimbo2.p7s");
        FileOutputStream fileOutputStream2 = new FileOutputStream("/tmp/carimbo3.p7s");
        byte[] bytes = IOUtils.toByteArray(fileInputStream);
        byte[] newBytes = new byte[bytes.length - 1];
        fileOutputStream1.write(newBytes);
        fileOutputStream2.write(newBytes);
        System.arraycopy(bytes, 0, newBytes, 0, newBytes.length);
        System.arraycopy(bytes, 1, newBytes, 0, newBytes.length);
    }
}
