package com.efsol.util;

import java.io.*;
import java.net.*;

public class BasicTransformer implements com.efsol.util.Transformer {

    public void transform(InputStream in, OutputStream out) throws IOException {
        FileUtils.copyStream(in, out, true);
        out.flush();
        out.close();
    }

    public void transform(URL url, OutputStream out) throws IOException {
        InputStream in = url.openStream();
        transform(in, out);
        in.close();
    }

    public void transform(String string, OutputStream out) throws IOException {
        InputStream in = new ByteArrayInputStream(string.getBytes());
        transform(in, out);
        in.close();
    }
}
