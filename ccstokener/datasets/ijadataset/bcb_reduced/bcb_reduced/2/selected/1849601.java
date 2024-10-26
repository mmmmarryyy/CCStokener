package net.sf.gridarta.model.face;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.gridarta.model.errorview.ErrorView;
import net.sf.gridarta.model.errorview.ErrorViewCategory;
import net.sf.gridarta.model.errorview.ErrorViewCollector;
import net.sf.gridarta.utils.ActionBuilderUtils;
import net.sf.gridarta.utils.ArrayUtils;
import net.sf.gridarta.utils.IOUtils;
import net.sf.japi.swing.action.ActionBuilder;
import net.sf.japi.swing.action.ActionBuilderFactory;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base implementation of {@link FaceObjects}.
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 */
public class DefaultFaceObjects extends AbstractFaceObjects {

    /**
     * The serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The chunk size in bytes when reading file contents.
     */
    private static final int CHUNK_SIZE = 4096;

    /**
     * The Logger for printing log messages.
     */
    private static final Category log = Logger.getLogger(DefaultFaceObjects.class);

    /**
     * Action Builder.
     */
    private static final ActionBuilder ACTION_BUILDER = ActionBuilderFactory.getInstance().getActionBuilder("net.sf.gridarta");

    /**
     * Whether the face file contains face numbers.
     */
    private final boolean includeFaceNumbers;

    /**
     * Creates a new instance.
     * @param includeFaceNumbers whether the face file contains face numbers
     */
    public DefaultFaceObjects(final boolean includeFaceNumbers) {
        super(ActionBuilderUtils.getString(ACTION_BUILDER, "nameOfFaceObject"));
        this.includeFaceNumbers = includeFaceNumbers;
    }

    @NotNull
    @Override
    public FaceProvider loadFacesCollection(@NotNull final ErrorView errorView, @NotNull final File collectedDirectory) {
        final String faceFile = ActionBuilderUtils.getString(ACTION_BUILDER, "configSource.image.name");
        final String treeFile = ActionBuilderUtils.getString(ACTION_BUILDER, "configSource.facetree.name");
        final File tmpFaceFile;
        try {
            tmpFaceFile = IOUtils.getFile(collectedDirectory, faceFile);
        } catch (final IOException ex) {
            errorView.addWarning(ErrorViewCategory.FACES_FILE_INVALID, new File(collectedDirectory, faceFile) + ": " + ex.getMessage());
            return new EmptyFaceProvider();
        }
        final CollectedFaceProvider collectedFaceProvider;
        int faces = 0;
        final ErrorViewCollector faceFileErrorViewCollector = new ErrorViewCollector(errorView, tmpFaceFile);
        try {
            collectedFaceProvider = new CollectedFaceProvider(tmpFaceFile);
            final byte[] data = getFileContents(tmpFaceFile);
            final ErrorViewCollector treeFileErrorViewCollector = new ErrorViewCollector(errorView, new File(collectedDirectory, treeFile));
            BufferedReader treeIn = null;
            try {
                final URL url = IOUtils.getResource(collectedDirectory, treeFile);
                final InputStream inputStream2 = url.openStream();
                final Reader reader = new InputStreamReader(inputStream2, IOUtils.MAP_ENCODING);
                treeIn = new BufferedReader(reader);
            } catch (final FileNotFoundException ignored) {
                treeFileErrorViewCollector.addWarning(ErrorViewCategory.FACES_FILE_INVALID);
            }
            final byte[] tag = "IMAGE ".getBytes();
            final StringBuilder faceB = new StringBuilder();
            try {
                final Pattern pattern = Pattern.compile(ActionBuilderUtils.getString(ACTION_BUILDER, "configSource.facetree.input"));
                int offset = 0;
                while (offset < data.length) {
                    if (!ArrayUtils.contains(data, offset, tag)) {
                        throw new IOException("expecting 'IMAGE' at position " + offset);
                    }
                    offset += 6;
                    if (includeFaceNumbers) {
                        while (data[offset++] != 0x20) {
                        }
                    }
                    int size = 0;
                    while (true) {
                        final char c = (char) data[offset++];
                        if (c == ' ') {
                            break;
                        }
                        if (c < '0' || c > '9') {
                            throw new IOException("syntax error at position " + offset + ": not a digit");
                        }
                        size *= 10;
                        size += (int) c - (int) '0';
                    }
                    faceB.setLength(0);
                    while (true) {
                        final char c = (char) data[offset++];
                        if (c == '\n') {
                            break;
                        }
                        if (c == '/') {
                            faceB.setLength(0);
                        } else {
                            faceB.append(c);
                        }
                    }
                    final String faceName = faceB.toString().intern();
                    if (offset + size > data.length) {
                        throw new IOException("truncated at position " + offset);
                    }
                    if (treeIn != null) {
                        final String originalFilename = treeIn.readLine();
                        if (originalFilename == null) {
                            log.warn(ACTION_BUILDER.format("logFaceObjectWithoutOriginalName", faceName));
                        } else {
                            final Matcher matcher = pattern.matcher(originalFilename);
                            if (matcher.matches()) {
                                try {
                                    addFaceObject(faceName, matcher.group(1), offset, size);
                                } catch (final DuplicateFaceException ex) {
                                    faceFileErrorViewCollector.addWarning(ErrorViewCategory.FACES_ENTRY_INVALID, "duplicate face: " + ex.getDuplicate().getFaceName());
                                } catch (final IllegalFaceException ex) {
                                    faceFileErrorViewCollector.addWarning(ErrorViewCategory.FACES_ENTRY_INVALID, "invalid face: " + ex.getFaceObject().getFaceName());
                                }
                            } else {
                                treeFileErrorViewCollector.addWarning(ErrorViewCategory.FACES_ENTRY_INVALID, "syntax error: " + originalFilename);
                            }
                        }
                    }
                    collectedFaceProvider.addInfo(faceName, offset, size);
                    faces++;
                    offset += size;
                }
            } finally {
                if (treeIn != null) {
                    treeIn.close();
                }
            }
        } catch (final IOException ex) {
            faceFileErrorViewCollector.addWarning(ErrorViewCategory.FACES_FILE_INVALID, ex.getMessage());
            return new EmptyFaceProvider();
        }
        if (log.isInfoEnabled()) {
            log.info("Loaded " + faces + " faces from '" + tmpFaceFile + "'.");
        }
        return collectedFaceProvider;
    }

    /**
     * Returns the contents of a {@link File} as a <code>byte</code> array.
     * @param file the file to read
     * @return the file contents
     * @throws IOException if the file cannot be read
     */
    private static byte[] getFileContents(@NotNull final File file) throws IOException {
        final ByteArrayOutputStream bufOut = new ByteArrayOutputStream((int) file.length());
        final InputStream inputStream = new FileInputStream(file);
        try {
            final byte[] buf = new byte[CHUNK_SIZE];
            while (true) {
                final int len = inputStream.read(buf);
                if (len == -1) {
                    break;
                }
                bufOut.write(buf, 0, len);
            }
        } finally {
            inputStream.close();
        }
        return bufOut.toByteArray();
    }

    /**
     * Returns whether the images file contains face numbers.
     * @return whether the images file contains face numbers
     */
    @Override
    public boolean isIncludeFaceNumbers() {
        return includeFaceNumbers;
    }
}
