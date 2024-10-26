package tudresden.ocl.test;

import java.io.*;
import java.net.*;

public final class DiffSource {

    private final BufferedReader bufferedReader;

    private final String name;

    public DiffSource(final BufferedReader bufferedReader, final String name) throws IOException {
        this.bufferedReader = bufferedReader;
        this.name = name;
    }

    public final BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    public final String getName() {
        return name;
    }

    public void close() throws IOException {
        bufferedReader.close();
    }

    public DiffSource(final Reader reader, final String name) throws IOException {
        this(new BufferedReader(reader), name);
    }

    public DiffSource(final InputStream stream, final String name) throws IOException {
        this(new InputStreamReader(stream), name);
    }

    public DiffSource(final File file) throws IOException {
        this(new FileInputStream(file), file.getAbsolutePath());
    }

    public DiffSource(final URL url) throws IOException {
        this(url.openStream(), url.toString());
    }
}
