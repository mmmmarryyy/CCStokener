package com.unboundid.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import static com.unboundid.util.UtilityMessages.*;

/**
 * This class defines an HTTP value pattern component, which may be used provide
 * string values read from a specified remote file accessed via HTTP.
 */
final class HTTPValuePatternComponent extends ValuePatternComponent {

    /**
   * The serial version UID for this serializable class.
   */
    private static final long serialVersionUID = 8879412445617836376L;

    private final String[] lines;

    private final Random seedRandom;

    private final ThreadLocal<Random> random;

    /**
   * Creates a new HTTP value pattern component with the provided information.
   *
   * @param  url   The HTTP URL to the file from which to read the data.
   * @param  seed  The value that will be used to seed the initial random number
   *               generator.
   *
   * @throws  IOException  If a problem occurs while reading data from the
   *                       specified HTTP URL.
   */
    HTTPValuePatternComponent(final String url, final long seed) throws IOException {
        seedRandom = new Random(seed);
        random = new ThreadLocal<Random>();
        final ArrayList<String> lineList = new ArrayList<String>(100);
        final URL parsedURL = new URL(url);
        final HttpURLConnection urlConnection = (HttpURLConnection) parsedURL.openConnection();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        try {
            while (true) {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                lineList.add(line);
            }
        } finally {
            reader.close();
        }
        if (lineList.isEmpty()) {
            throw new IOException(ERR_VALUE_PATTERN_COMPONENT_EMPTY_FILE.get());
        }
        lines = new String[lineList.size()];
        lineList.toArray(lines);
    }

    /**
   * {@inheritDoc}
   */
    @Override()
    void append(final StringBuilder buffer) {
        Random r = random.get();
        if (r == null) {
            r = new Random(seedRandom.nextLong());
            random.set(r);
        }
        buffer.append(lines[r.nextInt(lines.length)]);
    }

    /**
   * {@inheritDoc}
   */
    @Override()
    boolean supportsBackReference() {
        return true;
    }
}
