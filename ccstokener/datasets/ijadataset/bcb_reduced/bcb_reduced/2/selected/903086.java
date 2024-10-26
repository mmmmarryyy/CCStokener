package net.woodstock.rockapi.jsp.test;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import junit.framework.TestCase;

public class TestImage extends TestCase {

    public void test() throws Exception {
        String s = "file:///C:/Documents%20and%20Settings/lourival.junior/workspace/funttel/web/images/close.png";
        URL url = new URL(s);
        InputStream input = url.openStream();
        OutputStream output = new FileOutputStream("D:/close-new.png");
        Writer writer = new FileWriter("D:/close-new-2.png");
        byte[] b = new byte[input.available()];
        input.read(b);
        output.write(b);
        writer.write(new String(b));
        input.close();
        output.flush();
        output.close();
        writer.close();
    }
}
