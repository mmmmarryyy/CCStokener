package jam4j;

import jam4j.util.AsyncMultiPipe;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import org.testng.Assert;
import org.testng.Reporter;

final class Sandbox {

    private final File dir;

    Sandbox() throws IOException {
        dir = File.createTempFile("test-sandbox", "");
        if (!dir.delete()) throw new IOException("Couldn't delete temp file " + dir);
        if (!dir.mkdir()) throw new IOException("Couldn't create temp dir " + dir);
        dir.deleteOnExit();
    }

    void add(InputStream stream, String relPath) throws IOException {
        final BufferedInputStream in = makeBuffered(stream);
        final File dest = new File(dir, relPath);
        if (dest.getParent() != null) dest.getParentFile().mkdirs();
        final FileOutputStream out = new FileOutputStream(dest);
        try {
            while (true) {
                final int b;
                if ((b = in.read()) == -1) break;
                out.write(b);
            }
        } finally {
            out.close();
        }
        dest.deleteOnExit();
    }

    void add(URL url) throws IOException {
        add(url, new File(url.getPath()).getName());
    }

    void add(URL url, String relPath) throws IOException {
        add(url.openStream(), relPath);
    }

    void add(String resource, String relPath) throws IOException {
        final URL url;
        if ((url = Sandbox.class.getResource(resource)) == null) throw new FileNotFoundException("Cannot find resource: " + resource);
        add(url, relPath);
    }

    void add(String resource) throws IOException {
        add(resource, new File(resource).getName());
    }

    private static BufferedInputStream makeBuffered(InputStream stream) {
        return stream instanceof BufferedInputStream ? (BufferedInputStream) stream : new BufferedInputStream(stream);
    }

    byte[][] runJam(File jam) throws IOException {
        final Process jamProcess = new ProcessBuilder().command(jam.getAbsolutePath(), "-d0").directory(dir).start();
        jamProcess.getOutputStream().close();
        return AsyncMultiPipe.capture(jamProcess);
    }

    byte[][] runJam4J() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(), err = new ByteArrayOutputStream();
        final PrintStream testOut = new PrintStream(out), testErr = new PrintStream(err);
        Reporter.log("Running Jam4J in " + dir + " ...", true);
        try {
            new Jam4J().outputStream(testOut).errorStream(testErr).workingDirectory(dir).display(0, false).run();
        } catch (Exiting e) {
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            Assert.fail("Failed to run Jam4J", e);
        }
        return new byte[][] { out.toByteArray(), err.toByteArray() };
    }
}
