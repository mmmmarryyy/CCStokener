package com.efsol.wayne;

import java.io.*;
import java.net.*;
import com.caucho.transform.*;
import com.caucho.xsl.*;
import com.efsol.util.FileUtils;

public class Wayne {

    private String root = null;

    private URL rootURL = null;

    private StylesheetFactory factory = new StyleScript();

    public Wayne(String rootpath) {
        try {
            root = rootpath + System.getProperty("file.separator");
            rootURL = new URL("file:" + rootpath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void transform(String style, InputStream in, OutputStream out) throws IOException {
        if (style != null) {
            try {
                Stylesheet sheet = factory.newStylesheet(style);
                StreamTransformer transformer = sheet.newStreamTransformer();
                transformer.transform(in, out);
            } catch (Exception e) {
                e.printStackTrace(new PrintStream(out));
            }
            in.close();
        } else {
            FileUtils.copyStream(in, out, true);
        }
        out.flush();
        out.close();
    }

    public void transform(String style, String spec, OutputStream out) throws IOException {
        URL url = new URL(rootURL, spec);
        InputStream in = new PatchXMLSymbolsStream(new StripDoctypeStream(url.openStream()));
        transform(style, in, out);
        in.close();
    }
}
