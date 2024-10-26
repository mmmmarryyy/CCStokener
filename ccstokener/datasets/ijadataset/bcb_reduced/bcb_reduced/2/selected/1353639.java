package rachel.url;

import java.net.*;
import java.io.*;

public class ClassUrlStreamHandler extends URLStreamHandler {

    protected URLConnection openConnection(URL url) throws IOException {
        return new ClassUrlConnection(url);
    }
}
