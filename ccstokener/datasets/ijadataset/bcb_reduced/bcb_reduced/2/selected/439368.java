package nmazurs.sample_gae.server.word_connections;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author Normunds Mazurs (MAZE)
 * 
 */
public class PageGetter {

    protected final String url;

    protected final int bufferSize;

    protected final int maxSize;

    protected PageGetter(final String url, final int bufferSize, final int maxSize) {
        assert bufferSize > 0;
        assert maxSize > 0;
        this.url = url;
        this.bufferSize = bufferSize;
        this.maxSize = maxSize;
    }

    public static PageGetter create(final String url, final int bufferSize, final int maxSize) {
        return new PageGetter(url, bufferSize, maxSize);
    }

    public static PageGetter create(final String url) {
        return create(url, 1024 * 2, 1024 * 50);
    }

    protected long copyLarge(final Reader input, final Writer output) throws IOException {
        final char[] buffer = new char[bufferSize];
        long count = 0;
        int n;
        while (count < maxSize && (n = input.read(buffer)) >= 0) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    protected int copy(final Reader input, final Writer output) throws IOException {
        final long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    protected void copy(final InputStream input, final Writer output) throws IOException {
        final InputStreamReader in = new InputStreamReader(input);
        copy(in, output);
    }

    protected String toString(final InputStream input) throws IOException {
        final StringWriter sw = new StringWriter();
        copy(input, sw);
        return sw.toString();
    }

    public static class Result {

        protected final String output;

        protected final String mimeType;

        public Result(final String output, final String mimeType) {
            this.output = output;
            this.mimeType = mimeType;
        }

        public String getOutput() {
            return output;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    public Result getPage() {
        final URLConnection openConnection;
        final InputStream in;
        try {
            openConnection = new URL(url).openConnection();
            in = openConnection.getInputStream();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            return new Result(IOUtils.toString(in), openConnection.getContentType());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static void main(String[] args) {
        for (final String s : StringUtils.split("a ;b: v", ";: ")) {
            System.out.println(s);
        }
    }
}
