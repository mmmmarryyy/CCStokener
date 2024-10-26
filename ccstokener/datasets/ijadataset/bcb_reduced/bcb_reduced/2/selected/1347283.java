package net.sf.gridarta.model.smoothface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.regex.Pattern;
import net.sf.gridarta.model.errorview.ErrorView;
import net.sf.gridarta.model.errorview.ErrorViewCategory;
import net.sf.gridarta.model.errorview.ErrorViewCollector;
import net.sf.gridarta.utils.IOUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Loader for smooth files.
 * @author tchize
 * @author Andreas Kirschbaum
 */
public class SmoothFacesLoader {

    /**
     * The Logger for printing log messages.
     */
    private static final Category log = Logger.getLogger(SmoothFacesLoader.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private SmoothFacesLoader() {
    }

    /**
     * Loads a smooth file.
     * @param url the URL to read from
     * @param smoothFaces the smooth faces to update
     * @param errorView the error view for reporting errors
     */
    public static void load(@NotNull final URL url, @NotNull final SmoothFaces smoothFaces, @NotNull final ErrorView errorView) {
        final ErrorViewCollector errorViewCollector = new ErrorViewCollector(errorView, url);
        try {
            final InputStream inputStream = url.openStream();
            try {
                final Reader reader = new InputStreamReader(inputStream, IOUtils.MAP_ENCODING);
                try {
                    final Reader bufferedReader = new BufferedReader(reader);
                    try {
                        load(url.toString(), bufferedReader, smoothFaces, errorViewCollector);
                    } finally {
                        bufferedReader.close();
                    }
                } finally {
                    reader.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (final IOException ex) {
            errorViewCollector.addWarning(ErrorViewCategory.SMOOTH_FILE_INVALID, ex.getMessage());
        }
    }

    /**
     * Loads a smooth file.
     * @param readerName the (file) name of the reader
     * @param reader the reader to read from
     * @param smoothFaces the smooth faces to update
     * @param errorViewCollector the error view collector for reporting errors
     * @throws IOException if loadings fails
     */
    private static void load(@NotNull final String readerName, @NotNull final Reader reader, @NotNull final SmoothFaces smoothFaces, @NotNull final ErrorViewCollector errorViewCollector) throws IOException {
        int smoothEntries = 0;
        final BufferedReader bufferedReader = new BufferedReader(reader);
        try {
            final Pattern pattern = Pattern.compile(" ");
            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }
                final String[] elements = pattern.split(line);
                if (elements.length != 2) {
                    errorViewCollector.addWarning(ErrorViewCategory.SMOOTH_FILE_INVALID, "syntax error in line " + line);
                    return;
                }
                final SmoothFace smoothFace = new SmoothFace(elements[0], elements[1]);
                try {
                    smoothFaces.add(smoothFace);
                    smoothEntries++;
                } catch (final DuplicateSmoothFaceException ex) {
                    errorViewCollector.addWarning(ErrorViewCategory.SMOOTH_FILE_INVALID, "inconsistent smooth face '" + ex.getMessage() + "' maps to '" + ex.getNewValue() + "' and '" + ex.getOldValue() + "'");
                }
            }
        } finally {
            bufferedReader.close();
        }
        log.info("Loaded " + smoothEntries + " smooth rules from '" + readerName + "'.");
    }
}
