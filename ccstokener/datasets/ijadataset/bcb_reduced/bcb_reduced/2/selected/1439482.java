package com.ideo.jso;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import com.ideo.jso.util.URLConf;

@Ignore
public class Test {

    /**
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        System.out.println(getRealPathForJarFile("com/ideo/jso/junit/minimize/jso1.properties"));
    }

    protected static String getFileContentAsString(URL url, String encoding) throws IOException {
        InputStream input = null;
        StringWriter sw = new StringWriter();
        try {
            System.out.println("Free mem :" + Runtime.getRuntime().freeMemory());
            input = url.openStream();
            IOUtils.copy(input, sw, encoding);
            System.out.println("Free mem :" + Runtime.getRuntime().freeMemory());
        } finally {
            if (input != null) {
                input.close();
                System.gc();
                input = null;
                System.out.println("Free mem :" + Runtime.getRuntime().freeMemory());
            }
        }
        return sw.toString();
    }

    protected static String getRealPathForJarFile(String classPath) {
        URL urlProperties = URLConf.getClassPathUrlResource(classPath);
        if (urlProperties != null) {
            String urlExternForm = urlProperties.toExternalForm();
            if (urlExternForm != null && urlExternForm.startsWith("file:/")) {
                return urlExternForm.substring("file:/".length());
            }
        }
        return classPath;
    }
}
