package net.sf.gridarta.model.autojoin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import net.sf.gridarta.model.archetype.Archetype;
import net.sf.gridarta.model.archetype.UndefinedArchetypeException;
import net.sf.gridarta.model.archetypeset.ArchetypeSet;
import net.sf.gridarta.model.errorview.ErrorView;
import net.sf.gridarta.model.errorview.ErrorViewCategory;
import net.sf.gridarta.model.gameobject.GameObject;
import net.sf.gridarta.model.maparchobject.MapArchObject;
import net.sf.gridarta.utils.IOUtils;
import net.sf.gridarta.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Loader for {@link AutojoinLists} instances from files.
 * @author Andreas Kirschbaum
 */
public class AutojoinListsParser<G extends GameObject<G, A, R>, A extends MapArchObject<A>, R extends Archetype<G, A, R>> {

    /**
     * The file defining autojoin lists.
     */
    private static final String FILENAME = "autojoin.txt";

    /**
     * The {@link ErrorView} for reporting errors.
     */
    @NotNull
    private final ErrorView errorView;

    /**
     * The {@link ArchetypeSet} for looking up archetypes.
     */
    @NotNull
    private final ArchetypeSet<G, A, R> archetypeSet;

    /**
     * The {@link AutojoinLists} instance to update.
     */
    @NotNull
    private final AutojoinLists<G, A, R> autojoinLists;

    /**
     * Creates a new instance.
     * @param errorView the error view for reporting errors
     * @param archetypeSet the archetype set for looking up archetypes
     * @param autojoinLists the autojoin lists instance to update
     */
    public AutojoinListsParser(@NotNull final ErrorView errorView, final ArchetypeSet<G, A, R> archetypeSet, @NotNull final AutojoinLists<G, A, R> autojoinLists) {
        this.errorView = errorView;
        this.archetypeSet = archetypeSet;
        this.autojoinLists = autojoinLists;
    }

    /**
     * Loads all the autojoin lists from the data file.
     * @param baseDir the base directory to load autojoin info from
     */
    public void loadList(@NotNull final File baseDir) {
        try {
            final URL url = IOUtils.getResource(baseDir, FILENAME);
            try {
                final InputStream inputStream = url.openStream();
                try {
                    final Reader reader = new InputStreamReader(inputStream, IOUtils.MAP_ENCODING);
                    try {
                        final BufferedReader stream = new BufferedReader(reader);
                        try {
                            loadList(url, stream);
                        } finally {
                            stream.close();
                        }
                    } finally {
                        reader.close();
                    }
                } finally {
                    inputStream.close();
                }
            } catch (final IOException ex) {
                errorView.addWarning(ErrorViewCategory.AUTOJOIN_FILE_INVALID, url + ": " + ex.getMessage());
            }
        } catch (final IOException ex) {
            errorView.addWarning(ErrorViewCategory.AUTOJOIN_FILE_INVALID, FILENAME + ": " + ex.getMessage());
        }
    }

    /**
     * Loads all the autojoin lists from the data file.
     * @param url the source location for error messages
     * @param bufferedReader the reader to read from
     * @throws IOException if an I/O error occurs
     */
    private void loadList(@NotNull final URL url, @NotNull final BufferedReader bufferedReader) throws IOException {
        boolean sectionFlag = false;
        final List<List<R>> archetypes = new ArrayList<List<R>>(AutojoinList.SIZE);
        final StringBuilder undefinedArchetypes = new StringBuilder();
        boolean skipList = true;
        while (true) {
            final String line2 = bufferedReader.readLine();
            if (line2 == null) {
                break;
            }
            if (!line2.startsWith("#") && line2.length() > 0) {
                final String line = line2.trim();
                if (sectionFlag) {
                    if (line.equals("end")) {
                        if (undefinedArchetypes.length() > 0) {
                            errorView.addWarning(ErrorViewCategory.AUTOJOIN_ENTRY_INVALID, url + ": Autojoin list references undefined archetypes:" + undefinedArchetypes);
                        }
                        if (!skipList) {
                            try {
                                final AutojoinList<G, A, R> autojoinList = new AutojoinList<G, A, R>(archetypes);
                                autojoinLists.addAutojoinList(autojoinList);
                            } catch (final IllegalAutojoinListException ex) {
                                errorView.addWarning(ErrorViewCategory.AUTOJOIN_ENTRY_INVALID, url + ": " + ex.getMessage());
                            }
                        }
                        sectionFlag = false;
                    } else {
                        final List<R> tmp = new ArrayList<R>();
                        for (final String archetypeName : StringUtils.PATTERN_WHITESPACE.split(line)) {
                            try {
                                final R archetype = archetypeSet.getArchetype(archetypeName);
                                if (archetype.isMulti()) {
                                    errorView.addWarning(ErrorViewCategory.AUTOJOIN_ENTRY_INVALID, url + ": list contains multi-part game object: archetype '" + line + "'");
                                    skipList = true;
                                } else {
                                    tmp.add(archetype);
                                }
                            } catch (final UndefinedArchetypeException ex) {
                                undefinedArchetypes.append(' ').append(ex.getMessage());
                                skipList = true;
                            }
                        }
                        archetypes.add(tmp);
                    }
                } else {
                    if (line.equals("start")) {
                        sectionFlag = true;
                        archetypes.clear();
                        skipList = false;
                        undefinedArchetypes.setLength(0);
                    }
                }
            }
        }
    }
}
