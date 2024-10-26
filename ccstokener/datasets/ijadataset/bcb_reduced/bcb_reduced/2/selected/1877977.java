package net.sf.gridarta.model.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.sf.gridarta.model.anim.AnimationObjects;
import net.sf.gridarta.model.archetype.Archetype;
import net.sf.gridarta.model.archetypeset.ArchetypeSet;
import net.sf.gridarta.model.errorview.ErrorView;
import net.sf.gridarta.model.errorview.ErrorViewCategory;
import net.sf.gridarta.model.errorview.ErrorViewCollector;
import net.sf.gridarta.model.face.FaceObjects;
import net.sf.gridarta.model.face.FaceProvider;
import net.sf.gridarta.model.gameobject.GameObject;
import net.sf.gridarta.model.io.AbstractArchetypeParser;
import net.sf.gridarta.model.maparchobject.MapArchObject;
import net.sf.gridarta.utils.IOUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for {@link AbstractResourcesReader
 * AbstractResourcesReaders} that read from collected files.
 * @author Andreas Kirschbaum
 */
public abstract class AbstractCollectedResourcesReader<G extends GameObject<G, A, R>, A extends MapArchObject<A>, R extends Archetype<G, A, R>> extends AbstractResourcesReader<G> {

    /**
     * The logger for printing log messages.
     */
    private static final Category log = Logger.getLogger(AbstractCollectedResourcesReader.class);

    /**
     * The collected directory.
     */
    @NotNull
    private final File collectedDirectory;

    /**
     * The {@link ArchetypeSet} to update.
     */
    @NotNull
    private final ArchetypeSet<G, A, R> archetypeSet;

    /**
     * The {@link AbstractArchetypeParser} to use.
     */
    @NotNull
    private final AbstractArchetypeParser<G, A, R, ?> archetypeParser;

    /**
     * The {@link FaceObjects} instance.
     */
    @NotNull
    private final FaceObjects faceObjects;

    /**
     * The name of the animation tree information file.
     */
    @NotNull
    private final String animTreeFile;

    /**
     * The name of the collected archetypes file.
     */
    @NotNull
    private final String archetypesFile;

    /**
     * Creates a new instance.
     * @param collectedDirectory the collected directory
     * @param imageSet the active image set or <code>null</code>
     * @param archetypeSet the archetype set to update
     * @param archetypeParser the archetype parser to use
     * @param animationObjects the animation objects instance
     * @param faceObjects the face objects instance
     * @param animTreeFile the name of the animation tree information file
     * @param archetypesFile the name of the collected archetypes file
     */
    protected AbstractCollectedResourcesReader(@NotNull final File collectedDirectory, @Nullable final String imageSet, @NotNull final ArchetypeSet<G, A, R> archetypeSet, @NotNull final AbstractArchetypeParser<G, A, R, ?> archetypeParser, @NotNull final AnimationObjects animationObjects, @NotNull final FaceObjects faceObjects, @NotNull final String animTreeFile, @NotNull final String archetypesFile) {
        super(collectedDirectory, imageSet, animationObjects, faceObjects);
        this.collectedDirectory = collectedDirectory;
        this.archetypeSet = archetypeSet;
        this.archetypeParser = archetypeParser;
        this.faceObjects = faceObjects;
        this.animTreeFile = animTreeFile;
        this.archetypesFile = archetypesFile;
    }

    /**
     * Loads all animations.
     * @param errorView the error view for reporting problems
     */
    protected void loadAnimations(@NotNull final ErrorView errorView) {
        Map<String, String> animations = null;
        try {
            final URL url = IOUtils.getResource(collectedDirectory, animTreeFile);
            try {
                animations = loadAnimTree(url);
            } catch (final IOException ex) {
                errorView.addWarning(ErrorViewCategory.ANIMTREE_FILE_INVALID, url + ": " + ex.getMessage());
            }
        } catch (final IOException ex) {
            errorView.addWarning(ErrorViewCategory.ANIMTREE_FILE_INVALID, animTreeFile + ": " + ex.getMessage());
        }
        loadAnimationsFromCollect(errorView, animations == null ? Collections.<String, String>emptyMap() : animations);
    }

    /**
     * Loads all archetypes.
     * @param errorView the error view for reporting problems
     * @param invObjects all read archetypes are added to this list
     */
    protected void loadArchetypes(@NotNull final ErrorView errorView, @NotNull final List<G> invObjects) {
        archetypeSet.setLoadedFromArchive(true);
        try {
            final int archetypeCount = archetypeSet.getArchetypeCount();
            final URL url = IOUtils.getResource(collectedDirectory, archetypesFile);
            try {
                final InputStream inputStream = url.openStream();
                try {
                    final Reader reader = new InputStreamReader(inputStream, IOUtils.MAP_ENCODING);
                    try {
                        final BufferedReader bufferedReader = new BufferedReader(reader);
                        try {
                            archetypeParser.parseArchetypeFromStream(bufferedReader, null, null, null, "default", "default", "", invObjects, new ErrorViewCollector(errorView, url));
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
                errorView.addWarning(ErrorViewCategory.ARCHETYPES_FILE_INVALID, url + ": " + ex.getMessage());
            }
            if (log.isInfoEnabled()) {
                log.info("Loaded " + (archetypeSet.getArchetypeCount() - archetypeCount) + " archetypes from '" + url + "'.");
            }
        } catch (final IOException ex) {
            errorView.addWarning(ErrorViewCategory.ARCHETYPES_FILE_INVALID, archetypesFile + ": " + ex.getMessage());
        }
    }

    /**
     * Loads all faces.
     * @param errorView the error view for reporting problems
     * @return the face provider for accessing the read faces
     */
    @NotNull
    protected FaceProvider loadFacesCollection(@NotNull final ErrorView errorView) {
        return faceObjects.loadFacesCollection(errorView, collectedDirectory);
    }
}
