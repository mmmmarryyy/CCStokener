package gnu.protocol.zip;

import java.io.*;
import java.net.*;

public class Handler extends URLStreamHandler {

    protected URLConnection openConnection(URL url) throws IOException {
        return new ZipConnection(url);
    }
}
