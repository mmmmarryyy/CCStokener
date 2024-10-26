package net.sf.gridarta.maincontrol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.MissingResourceException;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import net.sf.gridarta.gui.filter.FilterControl;
import net.sf.gridarta.gui.map.renderer.RendererFactory;
import net.sf.gridarta.gui.scripts.DefaultScriptArchEditor;
import net.sf.gridarta.gui.scripts.ScriptArchDataUtils;
import net.sf.gridarta.gui.scripts.ScriptArchEditor;
import net.sf.gridarta.gui.scripts.ScriptedEventEditor;
import net.sf.gridarta.model.anim.AnimationObjects;
import net.sf.gridarta.model.archetype.Archetype;
import net.sf.gridarta.model.archetypechooser.ArchetypeChooserModel;
import net.sf.gridarta.model.archetypeset.ArchetypeSet;
import net.sf.gridarta.model.archetypetype.ArchetypeAttributeFactory;
import net.sf.gridarta.model.archetypetype.ArchetypeAttributeParser;
import net.sf.gridarta.model.archetypetype.ArchetypeTypeList;
import net.sf.gridarta.model.archetypetype.ArchetypeTypeParser;
import net.sf.gridarta.model.archetypetype.ArchetypeTypeSet;
import net.sf.gridarta.model.archetypetype.ArchetypeTypeSetParser;
import net.sf.gridarta.model.archetypetype.DefaultArchetypeAttributeFactory;
import net.sf.gridarta.model.autojoin.AutojoinLists;
import net.sf.gridarta.model.autojoin.AutojoinListsParser;
import net.sf.gridarta.model.configsource.ConfigSource;
import net.sf.gridarta.model.configsource.ConfigSourceFactory;
import net.sf.gridarta.model.direction.Direction;
import net.sf.gridarta.model.errorview.ErrorView;
import net.sf.gridarta.model.errorview.ErrorViewCategory;
import net.sf.gridarta.model.errorview.ErrorViewCollector;
import net.sf.gridarta.model.errorview.ErrorViewCollectorErrorHandler;
import net.sf.gridarta.model.face.FaceObjectProviders;
import net.sf.gridarta.model.face.FaceObjects;
import net.sf.gridarta.model.gameobject.GameObject;
import net.sf.gridarta.model.gameobject.GameObjectFactory;
import net.sf.gridarta.model.io.CacheFiles;
import net.sf.gridarta.model.io.DefaultMapReaderFactory;
import net.sf.gridarta.model.io.MapWriter;
import net.sf.gridarta.model.io.PathManager;
import net.sf.gridarta.model.maparchobject.MapArchObject;
import net.sf.gridarta.model.maparchobject.MapArchObjectFactory;
import net.sf.gridarta.model.mapmanager.AbstractMapManager;
import net.sf.gridarta.model.mapmanager.MapManager;
import net.sf.gridarta.model.mapmodel.InsertionMode;
import net.sf.gridarta.model.mapmodel.MapModelFactory;
import net.sf.gridarta.model.mapviewsettings.MapViewSettings;
import net.sf.gridarta.model.match.GameObjectMatcher;
import net.sf.gridarta.model.match.GameObjectMatchers;
import net.sf.gridarta.model.match.GameObjectMatchersParser;
import net.sf.gridarta.model.resource.AbstractResources;
import net.sf.gridarta.model.scripts.ScriptArchData;
import net.sf.gridarta.model.scripts.ScriptArchUtils;
import net.sf.gridarta.model.scripts.ScriptedEventFactory;
import net.sf.gridarta.model.settings.GlobalSettings;
import net.sf.gridarta.model.spells.ArchetypeSetSpellLoader;
import net.sf.gridarta.model.spells.GameObjectSpell;
import net.sf.gridarta.model.spells.NumberSpell;
import net.sf.gridarta.model.spells.Spells;
import net.sf.gridarta.model.spells.XMLSpellLoader;
import net.sf.gridarta.model.treasurelist.TreasureListsParser;
import net.sf.gridarta.model.treasurelist.TreasureLoader;
import net.sf.gridarta.model.treasurelist.TreasureTree;
import net.sf.gridarta.model.treasurelist.TreasureTreeNode;
import net.sf.gridarta.model.validation.DelegatingMapValidator;
import net.sf.gridarta.model.validation.NoSuchValidatorException;
import net.sf.gridarta.model.validation.ValidatorPreferences;
import net.sf.gridarta.model.validation.checks.ArchetypeTypeChecks;
import net.sf.gridarta.model.validation.checks.AttributeRangeChecker;
import net.sf.gridarta.model.validation.checks.EnvironmentChecker;
import net.sf.gridarta.model.validation.checks.PaidItemShopSquareChecker;
import net.sf.gridarta.model.validation.checks.ShopSquareChecker;
import net.sf.gridarta.model.validation.checks.ValidatorFactory;
import net.sf.gridarta.plugin.PluginExecutor;
import net.sf.gridarta.plugin.PluginModel;
import net.sf.gridarta.plugin.PluginModelLoader;
import net.sf.gridarta.plugin.PluginModelParser;
import net.sf.gridarta.plugin.PluginParameters;
import net.sf.gridarta.plugin.parameter.PluginParameterFactory;
import net.sf.gridarta.utils.CommonConstants;
import net.sf.gridarta.utils.GuiFileFilters;
import net.sf.gridarta.utils.IOUtils;
import net.sf.gridarta.utils.StringUtils;
import net.sf.gridarta.utils.SystemIcons;
import net.sf.gridarta.utils.XmlHelper;
import net.sf.japi.swing.action.ActionBuilder;
import net.sf.japi.swing.action.ActionBuilderFactory;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * MainControl is a central class that's used for access on global data
 * structures / collections and global functions.
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 */
public class DefaultMainControl<G extends GameObject<G, A, R>, A extends MapArchObject<A>, R extends Archetype<G, A, R>> {

    /**
     * The Logger for printing log messages.
     */
    private static final Category log = Logger.getLogger(DefaultMainControl.class);

    @NotNull
    private final TreasureTree treasureTree;

    @NotNull
    private final ScriptArchEditor<G, A, R> scriptArchEditor;

    @NotNull
    private final ScriptArchData<G, A, R> scriptArchData;

    @NotNull
    private final ScriptArchDataUtils<G, A, R> scriptArchDataUtils;

    @NotNull
    private final ScriptArchUtils scriptArchUtils;

    /**
     * Creates a new instance.
     * @param scriptFileFilter the file filter for script files
     * @param scriptExtension the file extension for script files
     * @param scriptName the display name for script files
     * @param spellType the object id for spell objects or <code>0</code> to not
     * collect game objects spells
     * @param spellFile the spell file name to load or <code>null</code> to not
     * load numbered spells
     * @param scriptsDir the plugin scripts directory
     * @param errorView the error view to add errors to
     * @param editorFactory the editor factory to use
     * @param forceReadFromFiles if set, read resources from individual files
     * ignoring the user's settings
     * @param globalSettings the global settings to use
     * @param configSourceFactory the config source factory to use
     * @param pathManager the path manager to use
     * @param gameObjectMatchers the game object matchers to use
     * @param gameObjectFactory the game object factory to use
     * @param archetypeTypeSet the archetype type set to use
     * @param archetypeSet the archetype set to use
     * @param archetypeChooserModel the archetype chooser model to use
     * @param autojoinLists the autojoin lists to use
     * @param mapManager the map manager
     * @param pluginModel the script model
     * @param validators the map validators
     * @param scriptedEventEditor the scripted event editor
     * @param resources the resources
     * @param numberSpells the number spells to use
     * @param gameObjectSpells the game object spells to use
     * @param pluginParameterFactory the plugin parameter factory to use
     * @param validatorPreferences the validator preferences to use
     * @param mapWriter the map writer for writing temporary map files
     */
    public DefaultMainControl(@NotNull final FileFilter scriptFileFilter, @NotNull final String scriptExtension, @NotNull final String scriptName, final int spellType, @Nullable final String spellFile, @NotNull final String scriptsDir, final ErrorView errorView, @NotNull final EditorFactory<G, A, R> editorFactory, final boolean forceReadFromFiles, @NotNull final GlobalSettings globalSettings, @NotNull final ConfigSourceFactory configSourceFactory, @NotNull final PathManager pathManager, @NotNull final GameObjectMatchers gameObjectMatchers, @NotNull final GameObjectFactory<G, A, R> gameObjectFactory, @NotNull final ArchetypeTypeSet archetypeTypeSet, @NotNull final ArchetypeSet<G, A, R> archetypeSet, @NotNull final ArchetypeChooserModel<G, A, R> archetypeChooserModel, @NotNull final AutojoinLists<G, A, R> autojoinLists, @NotNull final AbstractMapManager<G, A, R> mapManager, @NotNull final PluginModel<G, A, R> pluginModel, @NotNull final DelegatingMapValidator<G, A, R> validators, @NotNull final ScriptedEventEditor<G, A, R> scriptedEventEditor, @NotNull final AbstractResources<G, A, R> resources, @NotNull final Spells<NumberSpell> numberSpells, @NotNull final Spells<GameObjectSpell<G, A, R>> gameObjectSpells, @NotNull final PluginParameterFactory<G, A, R> pluginParameterFactory, @NotNull final ValidatorPreferences validatorPreferences, @NotNull final MapWriter<G, A, R> mapWriter) {
        final XmlHelper xmlHelper;
        try {
            xmlHelper = new XmlHelper();
        } catch (final ParserConfigurationException ex) {
            log.fatal("Cannot create XML parser: " + ex.getMessage());
            throw new MissingResourceException("Cannot create XML parser: " + ex.getMessage(), null, null);
        }
        final AttributeRangeChecker<G, A, R> attributeRangeChecker = new AttributeRangeChecker<G, A, R>(validatorPreferences);
        final EnvironmentChecker<G, A, R> environmentChecker = new EnvironmentChecker<G, A, R>(validatorPreferences);
        final DocumentBuilder documentBuilder = xmlHelper.getDocumentBuilder();
        try {
            final URL url = IOUtils.getResource(globalSettings.getConfigurationDirectory(), "GameObjectMatchers.xml");
            final ErrorViewCollector gameObjectMatchersErrorViewCollector = new ErrorViewCollector(errorView, url);
            try {
                documentBuilder.setErrorHandler(new ErrorViewCollectorErrorHandler(gameObjectMatchersErrorViewCollector, ErrorViewCategory.GAMEOBJECTMATCHERS_FILE_INVALID));
                try {
                    final GameObjectMatchersParser gameObjectMatchersParser = new GameObjectMatchersParser(documentBuilder, xmlHelper.getXPath());
                    gameObjectMatchersParser.readGameObjectMatchers(url, gameObjectMatchers, gameObjectMatchersErrorViewCollector);
                } finally {
                    documentBuilder.setErrorHandler(null);
                }
            } catch (final IOException ex) {
                gameObjectMatchersErrorViewCollector.addWarning(ErrorViewCategory.GAMEOBJECTMATCHERS_FILE_INVALID, ex.getMessage());
            }
            final ValidatorFactory<G, A, R> validatorFactory = new ValidatorFactory<G, A, R>(validatorPreferences, gameObjectMatchers, globalSettings, mapWriter);
            loadValidators(validators, validatorFactory, errorView);
            editorFactory.initMapValidators(validators, gameObjectMatchersErrorViewCollector, globalSettings, gameObjectMatchers, attributeRangeChecker, validatorPreferences);
            validators.addValidator(attributeRangeChecker);
            validators.addValidator(environmentChecker);
        } catch (final FileNotFoundException ex) {
            errorView.addWarning(ErrorViewCategory.GAMEOBJECTMATCHERS_FILE_INVALID, "GameObjectMatchers.xml: " + ex.getMessage());
        }
        final GameObjectMatcher shopSquareMatcher = gameObjectMatchers.getMatcher("system_shop_square", "shop_square");
        if (shopSquareMatcher != null) {
            final GameObjectMatcher noSpellsMatcher = gameObjectMatchers.getMatcher("system_no_spells", "no_spells");
            if (noSpellsMatcher != null) {
                final GameObjectMatcher blockedMatcher = gameObjectMatchers.getMatcher("system_blocked", "blocked");
                validators.addValidator(new ShopSquareChecker<G, A, R>(validatorPreferences, shopSquareMatcher, noSpellsMatcher, blockedMatcher));
            }
            final GameObjectMatcher paidItemMatcher = gameObjectMatchers.getMatcher("system_paid_item");
            if (paidItemMatcher != null) {
                validators.addValidator(new PaidItemShopSquareChecker<G, A, R>(validatorPreferences, shopSquareMatcher, paidItemMatcher));
            }
        }
        Map<String, TreasureTreeNode> specialTreasureLists;
        try {
            final URL url = IOUtils.getResource(globalSettings.getConfigurationDirectory(), "TreasureLists.xml");
            final ErrorViewCollector treasureListsErrorViewCollector = new ErrorViewCollector(errorView, url);
            try {
                final InputStream inputStream = url.openStream();
                try {
                    documentBuilder.setErrorHandler(new ErrorViewCollectorErrorHandler(treasureListsErrorViewCollector, ErrorViewCategory.TREASURES_FILE_INVALID));
                    try {
                        final Document specialTreasureListsDocument = documentBuilder.parse(new InputSource(inputStream));
                        specialTreasureLists = TreasureListsParser.parseTreasureLists(specialTreasureListsDocument);
                    } finally {
                        documentBuilder.setErrorHandler(null);
                    }
                } finally {
                    inputStream.close();
                }
            } catch (final IOException ex) {
                treasureListsErrorViewCollector.addWarning(ErrorViewCategory.TREASURES_FILE_INVALID, ex.getMessage());
                specialTreasureLists = Collections.emptyMap();
            } catch (final SAXException ex) {
                treasureListsErrorViewCollector.addWarning(ErrorViewCategory.TREASURES_FILE_INVALID, ex.getMessage());
                specialTreasureLists = Collections.emptyMap();
            }
        } catch (final FileNotFoundException ex) {
            errorView.addWarning(ErrorViewCategory.TREASURES_FILE_INVALID, "TreasureLists.xml: " + ex.getMessage());
            specialTreasureLists = Collections.emptyMap();
        }
        final ConfigSource configSource = forceReadFromFiles ? configSourceFactory.getFilesConfigSource() : configSourceFactory.getConfigSource(globalSettings.getConfigSourceName());
        treasureTree = TreasureLoader.parseTreasures(errorView, specialTreasureLists, configSource, globalSettings);
        final ArchetypeAttributeFactory archetypeAttributeFactory = new DefaultArchetypeAttributeFactory();
        final ArchetypeAttributeParser archetypeAttributeParser = new ArchetypeAttributeParser(archetypeAttributeFactory);
        final ArchetypeTypeParser archetypeTypeParser = new ArchetypeTypeParser(archetypeAttributeParser);
        ArchetypeTypeList eventTypeSet = null;
        try {
            final URL url = IOUtils.getResource(globalSettings.getConfigurationDirectory(), CommonConstants.TYPEDEF_FILE);
            final ErrorViewCollector typesErrorViewCollector = new ErrorViewCollector(errorView, url);
            documentBuilder.setErrorHandler(new ErrorViewCollectorErrorHandler(typesErrorViewCollector, ErrorViewCategory.GAMEOBJECTMATCHERS_FILE_INVALID));
            try {
                final ArchetypeTypeSetParser archetypeTypeSetParser = new ArchetypeTypeSetParser(documentBuilder, archetypeTypeSet, archetypeTypeParser);
                archetypeTypeSetParser.loadTypesFromXML(typesErrorViewCollector, new InputSource(url.toString()));
            } finally {
                documentBuilder.setErrorHandler(null);
            }
            final ArchetypeTypeList eventTypeSetTmp = archetypeTypeSet.getList("event");
            if (eventTypeSetTmp == null) {
                typesErrorViewCollector.addWarning(ErrorViewCategory.TYPES_ENTRY_INVALID, "list 'list_event' does not exist");
            } else {
                eventTypeSet = eventTypeSetTmp;
            }
        } catch (final FileNotFoundException ex) {
            errorView.addWarning(ErrorViewCategory.TYPES_FILE_INVALID, CommonConstants.TYPEDEF_FILE + ": " + ex.getMessage());
        }
        if (eventTypeSet == null) {
            eventTypeSet = new ArchetypeTypeList();
        }
        scriptArchUtils = editorFactory.newScriptArchUtils(eventTypeSet);
        final ScriptedEventFactory<G, A, R> scriptedEventFactory = editorFactory.newScriptedEventFactory(scriptArchUtils, gameObjectFactory, scriptedEventEditor, archetypeSet);
        scriptArchEditor = new DefaultScriptArchEditor<G, A, R>(scriptedEventFactory, scriptExtension, scriptName, scriptArchUtils, scriptFileFilter, globalSettings, mapManager, pathManager);
        scriptedEventEditor.setScriptArchEditor(scriptArchEditor);
        scriptArchData = editorFactory.newScriptArchData();
        scriptArchDataUtils = editorFactory.newScriptArchDataUtils(scriptArchUtils, scriptedEventFactory, scriptedEventEditor);
        final long timeStart = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
            log.info("Start to load archetypes...");
        }
        configSource.read(globalSettings, resources, errorView);
        for (final R archetype : archetypeSet.getArchetypes()) {
            final CharSequence editorFolder = archetype.getEditorFolder();
            if (editorFolder != null && !editorFolder.equals(GameObject.EDITOR_FOLDER_INTERN)) {
                final String[] tmp = StringUtils.PATTERN_SLASH.split(editorFolder, 2);
                if (tmp.length == 2) {
                    final String panelName = tmp[0];
                    final String folderName = tmp[1];
                    archetypeChooserModel.addArchetype(panelName, folderName, archetype);
                }
            }
        }
        if (log.isInfoEnabled()) {
            log.info("Archetype loading took " + (double) (System.currentTimeMillis() - timeStart) / 1000.0 + " seconds.");
        }
        if (spellType != 0) {
            new ArchetypeSetSpellLoader<G, A, R>(gameObjectFactory).load(archetypeSet, spellType, gameObjectSpells);
            gameObjectSpells.sort();
        }
        if (spellFile != null) {
            try {
                final URL url = IOUtils.getResource(globalSettings.getConfigurationDirectory(), spellFile);
                final ErrorViewCollector errorViewCollector = new ErrorViewCollector(errorView, url);
                documentBuilder.setErrorHandler(new ErrorViewCollectorErrorHandler(errorViewCollector, ErrorViewCategory.SPELLS_FILE_INVALID));
                try {
                    XMLSpellLoader.load(errorViewCollector, url, xmlHelper.getDocumentBuilder(), numberSpells);
                } finally {
                    documentBuilder.setErrorHandler(null);
                }
            } catch (final FileNotFoundException ex) {
                errorView.addWarning(ErrorViewCategory.SPELLS_FILE_INVALID, spellFile + ": " + ex.getMessage());
            }
            numberSpells.sort();
        }
        final File scriptsFile = new File(globalSettings.getMapsDirectory(), scriptsDir);
        final PluginModelParser<G, A, R> pluginModelParser = new PluginModelParser<G, A, R>(pluginParameterFactory);
        new PluginModelLoader<G, A, R>(pluginModelParser).loadPlugins(errorView, scriptsFile, pluginModel);
        new AutojoinListsParser<G, A, R>(errorView, archetypeSet, autojoinLists).loadList(globalSettings.getConfigurationDirectory());
        ArchetypeTypeChecks.addChecks(archetypeTypeSet, attributeRangeChecker, environmentChecker);
    }

    /**
     * Creates map validators from preferences.
     * @param validators the validators to add the newly created validators to
     * @param validatorFactory the validator factory for creating new
     * validators
     * @param errorView the error view to add error messages to
     */
    private void loadValidators(@NotNull final DelegatingMapValidator<G, A, R> validators, @NotNull final ValidatorFactory<G, A, R> validatorFactory, @NotNull final ErrorView errorView) {
        final ActionBuilder actionBuilder = ActionBuilderFactory.getInstance().getActionBuilder("net.sf.gridarta");
        int id = 0;
        while (true) {
            final String spec = actionBuilder.getString("validator." + id);
            if (spec == null) {
                break;
            }
            try {
                validators.addValidator(validatorFactory.newValidator(spec));
            } catch (final NoSuchValidatorException ex) {
                errorView.addWarning(ErrorViewCategory.MAP_VALIDATOR_ENTRY_INVALID, id, ex.getMessage());
            }
            id++;
        }
        if (log.isInfoEnabled()) {
            log.info("Loaded " + id + " map validators.");
        }
    }

    /**
     * Creates a new {@link GUIMainControl} instance.
     * @param scriptFileFilter the file filter for script files
     * @param scriptExtension the file extension for script files
     * @param createDirectionPane whether the direction panel should be created
     * @param mapManager the map manager
     * @param pickmapManager the pickmap manager
     * @param archetypeSet the archetype set
     * @param mapModelFactory the map model factory
     * @param compassIcon the icon to display in the selected square view;
     * <code>null</code> to not show an icon
     * @param gridartaJarFilename the filename of the editor's .jar file
     * @param lockedItemsTypeNumbers the type numbers of game objects being
     * locked items
     * @param autoValidatorDefault whether the auto validator is enabled by
     * default
     * @param spellFile the spell file to load; <code>null</code> to not load a
     * spell file
     * @param editorFactory the editor factory to use
     * @param errorView the error view to add errors to
     * @param cacheFiles the cache files for icon and preview images
     * @param configSourceFactory the config source factory to use
     * @param rendererFactory the renderer factory
     * @param filterControl the filter control to use
     * @param pluginExecutor the script executor to use
     * @param pluginParameters the script parameters to use
     * @param faceObjects the face objects
     * @param globalSettings the global settings
     * @param mapViewSettings the map view settings
     * @param faceObjectProviders the face object providers
     * @param pathManager the path manager
     * @param topmostInsertionMode the "topmost" insertion mode
     * @param gameObjectFactory the game object factory
     * @param systemIcons the system icons for creating icons
     * @param undefinedSpellIndex the index for no spell
     * @param archetypeTypeSet the archetype type set
     * @param mapArchObjectFactory the map arch object factory to use
     * @param mapReaderFactory the map reader factory to use
     * @param validators the map validators
     * @param gameObjectMatchers the game object matchers
     * @param scriptsDir the plugin scripts directory
     * @param pluginModel the script model
     * @param animationObjects the animation objects
     * @param archetypeChooserModel the archetype chooser model
     * @param allowRandomMapParameters whether exit paths may point to random
     * map parameters
     * @param scriptedEventEditor the scripted event editor
     * @param directionMap maps relative direction to map window direction
     * @param resources the resources
     * @param gameObjectSpells the game object spells to use
     * @param numberSpells the number spells to use
     * @param pluginParameterFactory the plugin parameter factory to use
     * @return the new instance
     */
    public GUIMainControl<G, A, R> createGUIMainControl(@NotNull final FileFilter scriptFileFilter, @NotNull final String scriptExtension, final boolean createDirectionPane, @NotNull final AbstractMapManager<G, A, R> mapManager, @NotNull final MapManager<G, A, R> pickmapManager, @NotNull final ArchetypeSet<G, A, R> archetypeSet, @NotNull final MapModelFactory<G, A, R> mapModelFactory, @Nullable final ImageIcon compassIcon, @NotNull final String gridartaJarFilename, @NotNull final int[] lockedItemsTypeNumbers, final boolean autoValidatorDefault, @Nullable final String spellFile, @NotNull final EditorFactory<G, A, R> editorFactory, @NotNull final ErrorView errorView, @NotNull final CacheFiles cacheFiles, @NotNull final ConfigSourceFactory configSourceFactory, @NotNull final RendererFactory<G, A, R> rendererFactory, @NotNull final FilterControl<G, A, R> filterControl, @NotNull final PluginExecutor<G, A, R> pluginExecutor, @NotNull final PluginParameters pluginParameters, @NotNull final FaceObjects faceObjects, @NotNull final GlobalSettings globalSettings, @NotNull final MapViewSettings mapViewSettings, @NotNull final FaceObjectProviders faceObjectProviders, @NotNull final PathManager pathManager, @NotNull final InsertionMode<G, A, R> topmostInsertionMode, @NotNull final GameObjectFactory<G, A, R> gameObjectFactory, @NotNull final SystemIcons systemIcons, final int undefinedSpellIndex, @NotNull final ArchetypeTypeSet archetypeTypeSet, @NotNull final MapArchObjectFactory<A> mapArchObjectFactory, @NotNull final DefaultMapReaderFactory<G, A, R> mapReaderFactory, @NotNull final DelegatingMapValidator<G, A, R> validators, @NotNull final GameObjectMatchers gameObjectMatchers, @NotNull final String scriptsDir, @NotNull final PluginModel<G, A, R> pluginModel, @NotNull final AnimationObjects animationObjects, @NotNull final ArchetypeChooserModel<G, A, R> archetypeChooserModel, final boolean allowRandomMapParameters, @NotNull final ScriptedEventEditor<G, A, R> scriptedEventEditor, @NotNull final Direction[] directionMap, @NotNull final AbstractResources<G, A, R> resources, @NotNull final Spells<GameObjectSpell<G, A, R>> gameObjectSpells, @NotNull final Spells<NumberSpell> numberSpells, @NotNull final PluginParameterFactory<G, A, R> pluginParameterFactory) {
        return new GUIMainControl<G, A, R>(createDirectionPane, mapManager, pickmapManager, archetypeSet, faceObjects, globalSettings, mapViewSettings, mapModelFactory, mapReaderFactory, mapArchObjectFactory, treasureTree, archetypeTypeSet, compassIcon, gridartaJarFilename, GuiFileFilters.mapFileFilter, scriptFileFilter, scriptExtension, validators, resources, gameObjectMatchers, errorView, lockedItemsTypeNumbers, scriptsDir, pluginModel, archetypeChooserModel, animationObjects, scriptArchEditor, scriptedEventEditor, scriptArchData, scriptArchDataUtils, scriptArchUtils, autoValidatorDefault, spellFile, allowRandomMapParameters, directionMap, editorFactory, faceObjectProviders, pluginParameterFactory, gameObjectFactory, pathManager, cacheFiles, gameObjectSpells, numberSpells, undefinedSpellIndex, systemIcons, configSourceFactory, topmostInsertionMode, rendererFactory, filterControl, pluginExecutor, pluginParameters);
    }
}
