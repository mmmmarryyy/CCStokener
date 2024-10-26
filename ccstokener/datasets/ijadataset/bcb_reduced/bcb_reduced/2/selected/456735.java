package depth;

import static de.tu.depth.fragments.Category.SubType.CHAPTERS;
import static de.tu.depth.fragments.Category.SubType.CODE_FILES;
import static de.tu.depth.fragments.Category.SubType.CODE_SNIPPETS;
import static de.tu.depth.fragments.Category.SubType.IMAGES;
import static de.tu.depth.fragments.Category.SubType.TUTORIALS;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import de.tu.depth.fragments.Category;
import de.tu.depth.fragments.Chapter;
import de.tu.depth.fragments.CodeFile;
import de.tu.depth.fragments.CodeSnippet;
import de.tu.depth.fragments.EmbeddableFragment;
import de.tu.depth.fragments.Folder;
import de.tu.depth.fragments.Fragment;
import de.tu.depth.fragments.FragmentFilter;
import de.tu.depth.fragments.HierarchyFragment;
import de.tu.depth.fragments.IFragmentFilter;
import de.tu.depth.fragments.Image;
import de.tu.depth.fragments.Project;
import de.tu.depth.fragments.Tutorial;
import de.tu.depth.fragments.Category.SubType;
import depth.event.XfsrOptionEvent;
import depth.event.XfsrOptionListener;
import depth.exceptions.DepthCrossProjectRelationException;
import depth.exceptions.DepthFragmentAlreadyExistsException;
import depth.exceptions.DepthIllegalFragmentTreeException;
import depth.exceptions.DepthIllegalRevisionException;
import depth.exceptions.DepthMultipleParserException;
import depth.exceptions.DepthParseException;
import depth.formatting.AbstractFormattingTemplate;
import depth.formatting.Format;
import depth.formatting.FormatChangedEvent;
import depth.formatting.FormattingConfiguration;
import depth.formatting.IFormatChangedListener;
import depth.formatting.Ident;
import depth.formatting.NodeInformation;
import depth.formatting.Range;
import depth.formatting.Templates;
import depth.formatting.Format.DisplayType;
import depth.formatting.selected.SelectedInformation;
import depth.parser.registry.IParser;
import depth.parser.registry.ParserConfig;
import depth.parser.registry.ParserRegistry;
import depth.processor.CategoryProcessor;
import depth.processor.ChapterProcessor;
import depth.processor.CodeFileProcessor;
import depth.processor.CodeSnippetProcessor;
import depth.processor.FolderProcessor;
import depth.processor.FragmentProcessor;
import depth.processor.ImageProcessor;
import depth.processor.ProjectProcessor;
import depth.processor.TutorialChapterProcessor;
import depth.processor.TutorialProcessor;
import depth.query.Query;
import depth.query.Queryer;
import depth.query.XfsrFormatManager;
import depth.repository.XfsrRevisionManager;
import depth.repository.fragment.XfsrFragmentManager;
import depth.repository.fragment.rules.RuleChecker;
import depth.repository.fragment.rules.TuPrologRuleChecker;
import depth.repository.options.XfsrOptionManager;
import depth.repository.query.XfsrQueryManager;
import depth.repository.reference.CodeSnippetRef;
import depth.repository.reference.ImageRef;
import depth.repository.reference.Reference;
import depth.repository.reference.XfsrReferenceManager;
import depth.template.registry.TemplateConfig.TemplateType;
import depth.tree.Token;
import depth.tree.TokenNode;
import depth.tree.TreeNode;
import depth.xmlconfig.XmlToFormatContentConverter;
import depth.xmlconfig.XmlToFragmentContentConverter;
import depth.xmlconfig.XmlToReferenceContentConverter;
import depth.xmlconfig.XmlToRevisionContentConverter;

/**
 * @author Andreas Bartho
 */
public abstract class XmlFileSystemRepository extends Observable implements IRepository {

    private static final Logger logger = Logger.getLogger(XmlFileSystemRepository.class);

    public static final String CHAPTER_FILE_EXTENSION = "depth";

    private IResourceHandler resourceHandler;

    private XfsrFragmentManager fragmentManager;

    private XfsrOptionManager repositoryOptions;

    private XfsrReferenceManager referenceManager;

    private XfsrQueryManager queryManager;

    private XfsrFormatManager formatManager;

    private XfsrRevisionManager revisionManager;

    private XfsrOptionListener optionListener;

    private RuleChecker ruleChecker;

    private Map<UUID, List<Closeable>> streamMap = new HashMap<UUID, List<Closeable>>();

    /**
	 * Returns the path in the file system in which the repository is stored.
	 * The path is read from the RepositoryOptions.
	 * 
	 * @return the path of the repository
	 */
    private IPath getRepositoryPath() {
        String repPathString = getXfsrOptions().getRepositoryPath();
        logger.debug(repPathString);
        return new Path(repPathString);
    }

    /**
	 * Returns the RepositoryOptions which contain metadata about the
	 * repository. The RepositoryOptions of the XmlFileSystemRepository contain
	 * the path of the repository and whether multiple identical CodeSnippets
	 * shall be saved.
	 */
    public IRepositoryOptions getRepositoryOptions() {
        if (repositoryOptions == null) {
            repositoryOptions = createRepositoryOptions();
            if (optionListener == null) {
                optionListener = new OptionListener();
            }
            repositoryOptions.addXfsrOptionListener(optionListener);
        }
        return repositoryOptions;
    }

    protected void setup() {
        addObserver(new RepositoryObserver());
        fragmentManager = new XfsrFragmentManager(this);
        referenceManager = new XfsrReferenceManager(this);
        queryManager = new XfsrQueryManager(this);
        formatManager = new XfsrFormatManager(this);
        formatManager.addFormatChangedListener(new FormatListener());
        ruleChecker = new TuPrologRuleChecker(this);
        revisionManager = new XfsrRevisionManager(this);
        loadDefaultFormats();
    }

    /**
	 * Loads and registers all default formats that are defined via the
	 * DepthRepositoryPlugIn.formats extension point.
	 */
    private void loadDefaultFormats() {
        String formatSchemaLocation = resourceHandler.getFileLocation("resources/formats.xsd");
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("DepthRepositoryPlugIn.formats");
        for (IConfigurationElement defaultFormat : point.getConfigurationElements()) {
            String parserID = defaultFormat.getAttribute("parserID");
            String filePath = defaultFormat.getAttribute("xmlFile");
            String plugin = defaultFormat.getContributor().getName();
            XmlToFormatContentConverter fConvDefault = new XmlToFormatContentConverter(this, formatSchemaLocation, parserID);
            try {
                URL url = EclipsePluginResourceHandler.getFileLocation(plugin, filePath);
                fConvDefault.convert(url.openStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private XfsrOptionManager getXfsrOptions() {
        return (XfsrOptionManager) getRepositoryOptions();
    }

    public boolean canContain(Fragment parent, Fragment child) {
        if (parent == null) {
            throw new IllegalArgumentException("parent must not be null");
        }
        if (child == null) {
            throw new IllegalArgumentException("child must not be null");
        }
        if (!fragmentExists(parent)) {
            throw new IllegalArgumentException("unregistered Fragment: " + parent);
        }
        if (!fragmentExists(child)) {
            Set<UUID> uuidSet = new HashSet<UUID>();
            List<Fragment> fragmentList = new LinkedList<Fragment>();
            serializeFragments(child, fragmentList);
            for (Fragment f : fragmentList) {
                UUID uuid = f.getUUID();
                if (uuidSet.contains(uuid)) {
                    return false;
                } else {
                    uuidSet.add(uuid);
                }
                if (getFragment(uuid) != null) {
                    return false;
                }
            }
        }
        return ruleChecker.canContain(parent, child);
    }

    private void serializeFragments(Fragment root, List<Fragment> list) {
        list.add(root);
        for (Fragment child : root.getChildren()) {
            serializeFragments(child, list);
        }
    }

    public boolean canContain(Fragment parent, String childType) {
        if (parent == null) {
            throw new IllegalArgumentException("parent must not be null");
        }
        if (childType == null) {
            throw new IllegalArgumentException("childType must not be null");
        }
        if (!fragmentExists(parent)) {
            throw new IllegalArgumentException("unregistered Fragment: " + parent);
        }
        return ruleChecker.canContain(parent, childType);
    }

    public boolean nameContained(Fragment parent, String childName) {
        return ruleChecker.nameContained(parent, childName);
    }

    public boolean canContainName(Fragment parent, String childName) {
        return ruleChecker.canContainName(parent, childName);
    }

    /**
	 * Creates the RepositoryOptions. This method is invoked automatically the
	 * first time the RepositoryOptions are needed (this is during the startup
	 * of the repository). It is tried to read the repository location from the
	 * depth.properties configuration file. If that fails for some reason the
	 * current working directory is taken as repository path. A warning is
	 * logged, however.
	 * 
	 * @return the RepositoryOptions
	 */
    protected abstract XfsrOptionManager createRepositoryOptions();

    public XfsrFragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public XfsrReferenceManager getReferenceManager() {
        return referenceManager;
    }

    public XfsrFormatManager getFormatManager() {
        return formatManager;
    }

    protected void setResourceHandler(IResourceHandler resourceHandler) {
        this.resourceHandler = resourceHandler;
    }

    protected IResourceHandler getResourceHandler() {
        return resourceHandler;
    }

    public String getResourceLocation(String resourceName) {
        return resourceHandler.getFileLocation(resourceName);
    }

    public Project getProjectOf(Fragment fragment) {
        if (fragment == null) {
            throw new IllegalArgumentException("fragment must not be null");
        }
        if (!fragmentExists(fragment)) {
            throw new IllegalArgumentException("unregistered fragment: " + fragment);
        }
        return fragmentManager.getProjectOf(fragment);
    }

    public boolean isTutorialChapter(Fragment fragment) {
        if (fragment == null) {
            throw new IllegalArgumentException("fragment must not be null");
        }
        if (fragment instanceof Chapter) {
            Chapter c = (Chapter) fragment;
            if (c.getParent() instanceof Tutorial) {
                Tutorial t = (Tutorial) c.getParent();
                return fragmentExists(t);
            }
        }
        return false;
    }

    public void setChapterPosition(Chapter chapter, int position) {
        if (chapter == null) {
            throw new IllegalArgumentException("chapter must not be null");
        }
        if (!fragmentExists(chapter)) {
            throw new IllegalArgumentException("chapter is not in repository");
        }
        if (!isTutorialChapter(chapter)) {
            throw new IllegalArgumentException("chapter must be a tutorial chapter");
        }
        if (position < 0) {
            throw new IllegalArgumentException("positon must be >= 0, but was " + position);
        }
        Tutorial tut = (Tutorial) chapter.getParent();
        int nrTutChildren = tut.getChildren().size();
        if (position >= nrTutChildren) {
            throw new IllegalArgumentException("positon (" + position + " must be < the number of the tutorial's chapters (" + nrTutChildren + ")");
        }
        tut.setChildPosition(chapter, position);
        contentChanged();
    }

    public boolean hasProjects() {
        return getProjects().size() != 0;
    }

    public void move(Fragment toMove, HierarchyFragment target) throws DepthFragmentAlreadyExistsException, DepthIllegalFragmentTreeException, DepthCrossProjectRelationException {
        if (toMove.getParent() == target) {
            return;
        }
        if (getProjectOf(toMove) != getProjectOf(target)) {
            throw new DepthCrossProjectRelationException(toMove, target);
        }
        if (!canContainName(target, toMove.getName())) {
            throw new DepthFragmentAlreadyExistsException(getProjectOf(target), target, toMove.getTypeString(), toMove.getName());
        }
        if (!canContain(target, toMove)) {
            throw new DepthIllegalFragmentTreeException(target, toMove);
        }
        closeStreamsRecursively(toMove);
        fragmentManager.move(toMove, target);
        contentChanged();
    }

    public void rename(Fragment fragment, String name) throws DepthFragmentAlreadyExistsException {
        if (fragment.getName().equals(name)) {
            return;
        }
        if (fragment.getName().equalsIgnoreCase(name)) {
            fragmentManager.rename(fragment, name);
        } else {
            if (fragment instanceof Project) {
                if (projectExists(name)) {
                    throw new DepthFragmentAlreadyExistsException("project", name);
                }
            } else if (!canContainName(fragment.getParent(), name)) {
                throw new DepthFragmentAlreadyExistsException(getProjectOf(fragment), fragment.getTypeString(), name);
            }
            closeStreams(fragment);
            fragmentManager.rename(fragment, name);
        }
        contentChanged(fragment);
    }

    public Set<Fragment> getDependentFragments(Fragment fragment) {
        if (fragment == null) {
            throw new IllegalArgumentException("fragment must not be null");
        }
        if (!fragmentExists(fragment)) {
            throw new IllegalArgumentException("unregistered Fragment: " + fragment);
        }
        return fragmentManager.getDependentFragments(fragment);
    }

    public boolean projectExists(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        return ruleChecker.projectExists(name);
    }

    public boolean chapterExists(Project project, String name) {
        if (project == null) {
            throw new IllegalArgumentException("project must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!fragmentExists(project)) {
            return false;
        }
        Category catCh = project.getCategory(Category.SubType.CHAPTERS);
        return nameContained(catCh, name);
    }

    public boolean tutorialExists(Project project, String name) {
        if (project == null) {
            throw new IllegalArgumentException("project must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!fragmentExists(project)) {
            return false;
        }
        Category catTut = project.getCategory(Category.SubType.TUTORIALS);
        return nameContained(catTut, name);
    }

    public boolean tutorialChapterExists(Tutorial tutorial, String name) {
        if (tutorial == null) {
            throw new IllegalArgumentException("tutorial must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!fragmentExists(tutorial)) {
            return false;
        }
        return nameContained(tutorial, name);
    }

    public boolean codeFileExists(HierarchyFragment hierarchyFragment, String name) {
        if (hierarchyFragment == null) {
            throw new IllegalArgumentException("hierarchyFragment must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!fragmentExists(hierarchyFragment)) {
            return false;
        }
        return nameContained(hierarchyFragment, name);
    }

    public boolean folderExists(HierarchyFragment hierarchyFragment, String name) {
        if (hierarchyFragment == null) {
            throw new IllegalArgumentException("hierarchyFragment must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!fragmentExists(hierarchyFragment)) {
            return false;
        }
        return nameContained(hierarchyFragment, name);
    }

    public boolean codeSnippetExists(Project project, String name) {
        if (project == null) {
            throw new IllegalArgumentException("project must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!fragmentExists(project)) {
            return false;
        }
        Category catCs = project.getCategory(Category.SubType.CODE_SNIPPETS);
        return nameContained(catCs, name);
    }

    public boolean imageExists(HierarchyFragment hierarchyFragment, String name) {
        if (hierarchyFragment == null) {
            throw new IllegalArgumentException("hierarchyFragment must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!fragmentExists(hierarchyFragment)) {
            return false;
        }
        return nameContained(hierarchyFragment, name);
    }

    public boolean fragmentExists(Fragment fragment) {
        if (fragment == null) {
            throw new IllegalArgumentException("fragment must not be null");
        }
        return fragmentManager.fragmentExists(fragment);
    }

    public Project createNewProject(String name) throws DepthFragmentAlreadyExistsException {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (projectExists(name)) {
            throw new DepthFragmentAlreadyExistsException("project", name);
        }
        Project project = new Project(name);
        ProjectProcessor processor = new ProjectProcessor(project, this);
        processor.register();
        processor.createFileRepresentation(null);
        createNewCategory(project, CHAPTERS);
        createNewCategory(project, CODE_FILES);
        createNewCategory(project, CODE_SNIPPETS);
        createNewCategory(project, IMAGES);
        createNewCategory(project, TUTORIALS);
        contentChanged();
        return project;
    }

    private Category createNewCategory(Project project, Category.SubType subType) {
        Category category = new Category(project, subType);
        CategoryProcessor processor = new CategoryProcessor(category, this);
        processor.register();
        processor.createFileRepresentation(null);
        return category;
    }

    public Chapter createNewChapter(Project project, String name) throws DepthFragmentAlreadyExistsException {
        Chapter chapter = importChapter(project, resourceHandler.getFileLocation("templates/chapter_template.depth"), name);
        return chapter;
    }

    public Folder createNewFolder(HierarchyFragment parent, String name) throws DepthFragmentAlreadyExistsException, DepthIllegalFragmentTreeException {
        if (parent == null) {
            throw new IllegalArgumentException("parent must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!fragmentExists(parent)) {
            throw new IllegalArgumentException("unregistered fragment: " + parent);
        }
        if (folderExists(parent, name)) {
            Project project = getProjectOf(parent);
            throw new DepthFragmentAlreadyExistsException(project, parent, "code file or folder", name);
        }
        if (!canContain(parent, "folder")) {
            throw new DepthIllegalFragmentTreeException(parent, name, "folder");
        }
        Folder folder = new Folder(parent, name);
        FolderProcessor processor = new FolderProcessor(folder, this);
        processor.register();
        processor.createFileRepresentation(null);
        contentChanged();
        return folder;
    }

    public Chapter importChapter(Project project, String filename, String name) throws DepthFragmentAlreadyExistsException {
        if (project == null) {
            throw new IllegalArgumentException("project must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (filename == null) {
            throw new IllegalArgumentException("filename must not be null");
        }
        if (!fragmentExists(project)) {
            throw new IllegalArgumentException("unregistered Project: " + project);
        }
        if (chapterExists(project, name)) {
            throw new DepthFragmentAlreadyExistsException(project, "chapter", name);
        }
        Category category = project.getCategory(CHAPTERS);
        Chapter chapter = new Chapter(category, name);
        ChapterProcessor processor = new ChapterProcessor(chapter, this);
        processor.register();
        processor.createFileRepresentation(filename);
        contentChanged();
        return chapter;
    }

    public Tutorial createNewTutorial(Project project, String name) throws DepthFragmentAlreadyExistsException {
        if (project == null) {
            throw new IllegalArgumentException("project must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!fragmentExists(project)) {
            throw new IllegalArgumentException("unregistered Project: " + project);
        }
        if (tutorialExists(project, name)) {
            throw new DepthFragmentAlreadyExistsException(project, "tutorial", name);
        }
        Category category = project.getCategory(TUTORIALS);
        Tutorial tutorial = new Tutorial(category, name);
        TutorialProcessor processor = new TutorialProcessor(tutorial, this);
        processor.register();
        processor.createFileRepresentation(null);
        contentChanged();
        return tutorial;
    }

    /**
	 * Note that multiple chapters with the same name can exist for a tutorial
	 */
    public Chapter addChapterToTutorial(Tutorial tutorial, Chapter chapter) throws DepthFragmentAlreadyExistsException, DepthCrossProjectRelationException {
        if (tutorial == null) {
            throw new IllegalArgumentException("tutorial must not be null");
        }
        if (chapter == null) {
            throw new IllegalArgumentException("tutorial must not be null");
        }
        if (!fragmentExists(tutorial)) {
            throw new IllegalArgumentException("unregistered Tutorial: " + tutorial);
        }
        if (!fragmentExists(chapter)) {
            throw new IllegalArgumentException("unregistered Chapter: " + chapter);
        }
        if (getProjectOf(tutorial) != (getProjectOf(chapter))) {
            throw new DepthCrossProjectRelationException(tutorial, chapter);
        }
        String chapterName = chapter.getName();
        Project project = getProjectOf(tutorial);
        if (tutorialChapterExists(tutorial, chapterName)) {
            throw new DepthFragmentAlreadyExistsException(project, tutorial, chapterName);
        }
        if (isTutorialChapter(chapter)) {
            throw new IllegalArgumentException("chapter must not be a Tutorial Chapter");
        }
        UUID uuidClone = UUID.fromString(chapter.getUUID().toString());
        Chapter chapterClone = new Chapter(tutorial, chapter.getName(), uuidClone);
        TutorialChapterProcessor processor = new TutorialChapterProcessor(chapterClone, this);
        processor.register();
        processor.createFileRepresentation(null);
        contentChanged();
        return chapterClone;
    }

    protected void loadProjects() {
        String projectSchemaLocation = resourceHandler.getFileLocation("resources/projects.xsd");
        String formatSchemaLocation = resourceHandler.getFileLocation("resources/formats.xsd");
        String revisionSchemaLocation = resourceHandler.getFileLocation("resources/revisions.xsd");
        File projectsXml = getRepositoryPath().append("projects.xml").toFile();
        if (projectsXml.exists()) {
            XmlToFragmentContentConverter fragConverter = new XmlToFragmentContentConverter(this, projectSchemaLocation);
            fragConverter.convert(projectsXml);
            XmlToReferenceContentConverter refConverter = new XmlToReferenceContentConverter(this, projectSchemaLocation);
            refConverter.convert(projectsXml);
        }
        File formatsXml = getRepositoryPath().append("formats.xml").toFile();
        if (formatsXml.exists()) {
            XmlToFormatContentConverter fConv = new XmlToFormatContentConverter(this, formatSchemaLocation);
            fConv.convert(formatsXml);
        }
        File revisionsXml = getRepositoryPath().append("revisions.xml").toFile();
        if (revisionsXml.exists()) {
            XmlToRevisionContentConverter revConverter = new XmlToRevisionContentConverter(this, revisionSchemaLocation);
            revConverter.convert(revisionsXml);
        }
    }

    protected void unloadProjects() {
        for (Project project : getProjects()) {
            closeStreamsRecursively(project);
        }
        fragmentManager.unloadProjects();
        referenceManager.unloadProjects();
        formatManager.unloadProjects();
        revisionManager.unloadProjects();
    }

    public CodeFile importCodeFile(HierarchyFragment parent, String filename, String name) throws DepthFragmentAlreadyExistsException, DepthParseException, DepthIllegalFragmentTreeException, DepthMultipleParserException {
        if (parent instanceof Project) {
            parent = ((Project) parent).getCategory(SubType.CODE_FILES);
        }
        int latestRevision = getLatestRevision(getProjectOf(parent));
        CodeFile cf = null;
        try {
            cf = importCodeFile(parent, filename, name, latestRevision + 1, null);
        } catch (DepthIllegalRevisionException e) {
            assert false : "updated revision has not been accepted";
        }
        return cf;
    }

    public CodeFile importCodeFile(HierarchyFragment parent, String filename, String name, String parserID) throws DepthFragmentAlreadyExistsException, DepthParseException, DepthIllegalFragmentTreeException, DepthMultipleParserException {
        int latestRevision = getLatestRevision(getProjectOf(parent));
        CodeFile cf = null;
        try {
            cf = importCodeFile(parent, filename, name, latestRevision + 1, parserID);
        } catch (DepthIllegalRevisionException e) {
            assert false : "updated revision has not been accepted";
        }
        return cf;
    }

    public CodeFile importCodeFile(HierarchyFragment parent, String filename, String name, int revision, String parserID) throws DepthFragmentAlreadyExistsException, DepthParseException, DepthIllegalFragmentTreeException, DepthMultipleParserException, DepthIllegalRevisionException {
        Project project = getProjectOf(parent);
        if (revision <= 0) {
            throw new DepthIllegalRevisionException(revision);
        }
        if (getLatestRevision(project) > revision) {
            throw new DepthIllegalRevisionException(getLatestRevision(project), revision);
        }
        if (codeFileExists(parent, name)) {
            throw new DepthFragmentAlreadyExistsException(project, parent, "code file or folder", name);
        }
        ParserRegistry registry = ParserRegistry.getInstance();
        ParserConfig pc = null;
        if (parserID != null) {
            pc = registry.getParserFromID(parserID);
        }
        if (pc == null) {
            List<ParserConfig> pcList = registry.getParser(filename);
            if (pcList.size() > 1) {
                throw new DepthMultipleParserException(name, pcList);
            } else if (pcList.size() == 1) {
                pc = pcList.get(0);
            }
        }
        if (pc == null) throw new DepthParseException("Cannot find a parser for this file type.");
        CodeFile codeFile = new CodeFile(parent, name, pc.getId());
        if (!canContain(parent, codeFile.getTypeString())) {
            throw new DepthIllegalFragmentTreeException(parent, codeFile);
        }
        revisionManager.addRevisionIndex(project, codeFile, revision);
        CodeFileProcessor processor = new CodeFileProcessor(codeFile, this);
        processor.register();
        processor.createFileRepresentation(filename);
        try {
            IParser parser = pc.getParser();
            Document doc = parser.getXmlAst(codeFile);
            processor.setCachedDoc(doc, revision);
        } catch (DepthParseException dpe) {
            removeFragment(codeFile);
            throw dpe;
        }
        contentChanged();
        return codeFile;
    }

    public Image importImage(Project project, String filename, String name) throws DepthFragmentAlreadyExistsException, DepthIllegalFragmentTreeException {
        Category category = project.getCategory(IMAGES);
        return importImage(category, filename, name);
    }

    public Image importImage(HierarchyFragment parent, String filename, String name) throws DepthFragmentAlreadyExistsException, DepthIllegalFragmentTreeException {
        Project project = getProjectOf(parent);
        if (imageExists(parent, name)) {
            throw new DepthFragmentAlreadyExistsException(project, "image", name);
        }
        Image image = new Image(parent, name, Image.SubType.JPEG);
        if (!canContain(parent, image.getTypeString())) {
            throw new DepthIllegalFragmentTreeException(parent, image);
        }
        ImageProcessor processor = new ImageProcessor(image, this);
        processor.register();
        processor.createFileRepresentation(filename);
        contentChanged();
        return image;
    }

    public boolean belongToSameProject(Fragment f1, Fragment f2) {
        return getProjectOf(f1) == getProjectOf(f2);
    }

    private void contentChanged() {
        contentChanged(null);
    }

    private void contentChanged(Object o) {
        setChanged();
        notifyObservers(o);
    }

    private void saveConfigDocuments() {
        saveFragmentConfig();
        saveFormatConfig();
        saveRevisionConfig();
    }

    private void saveFragmentConfig() {
        Document projectDoc = Util.makeDocument();
        Element eProjects = projectDoc.createElement("projects");
        projectDoc.appendChild(eProjects);
        int i = 0;
        for (Project project : getProjects()) {
            fragmentManager.appendConfigurationAsXml(project, eProjects);
            Element eLastProject = (Element) eProjects.getElementsByTagName("project").item(i);
            referenceManager.appendConfigurationAsXml(project, eLastProject);
            i++;
        }
        Util.savePrettyXml(projectDoc, getRepositoryPath().append("projects.xml").toOSString());
    }

    private void saveFormatConfig() {
        Document formatDoc = formatManager.createConfigurationAsXml();
        Util.savePrettyXml(formatDoc, getRepositoryPath().append("formats.xml").toOSString());
    }

    private void saveRevisionConfig() {
        Document revisionDoc = Util.makeDocument();
        Element eRevisions = revisionDoc.createElement("revisions");
        revisionDoc.appendChild(eRevisions);
        int i = 0;
        for (Project project : getProjects()) {
            for (Fragment fragment : getFragments(project, FragmentFilter.CODEFILE_FILTER)) {
                Element eCodeFile = revisionDoc.createElement("codefile");
                eCodeFile.setAttribute("uuid", fragment.getUUID().toString());
                eRevisions.appendChild(eCodeFile);
                for (Integer rev : revisionManager.getRevisionIndexes((CodeFile) fragment)) {
                    Element eRev = revisionDoc.createElement("rev");
                    eRev.setAttribute("nr", rev.toString());
                    eCodeFile.appendChild(eRev);
                }
            }
        }
        Util.savePrettyXml(revisionDoc, getRepositoryPath().append("revisions.xml").toOSString());
    }

    /**
	 * 
	 */
    public Document getCodeFileDocument(CodeFile codeFile, Collection<CodeSnippetRef> codeSnippetRefs, Ident... idents) {
        return getCodeFileDocument(codeFile, getRevision(codeFile), codeSnippetRefs, idents);
    }

    public Document getCodeFileDocument(CodeFile codeFile, int revision, Collection<CodeSnippetRef> codeSnippetRefs, Ident... idents) {
        if (codeSnippetRefs == null) {
            throw new IllegalArgumentException("codeSnippetRefs must not be null");
        }
        Document codeFileDoc = getCodeFileAst(codeFile);
        for (CodeSnippetRef ref : codeSnippetRefs) {
            executeQuery(ref, codeFileDoc);
        }
        ParserConfig pc = ParserRegistry.getInstance().getParser(codeFile);
        FormattingConfiguration fc = new FormattingConfiguration(pc);
        if (idents.length > 0) {
            for (Ident ident : idents) {
                fc.addTemplate(ident);
            }
        } else {
            List<AbstractFormattingTemplate> templates = pc.getTemplates(TemplateType.CODE_FILE);
            for (AbstractFormattingTemplate template : templates) {
                fc.addTemplate(template);
            }
        }
        TreeNode root = fc.convertToNodes(codeFileDoc.getDocumentElement());
        Document doc = fc.makeXml(root);
        return doc;
    }

    public CodeFile getParentCodeFile(CodeSnippet codeSnippet) {
        if (codeSnippet == null) {
            throw new IllegalArgumentException("CodeSnippet must not be null");
        }
        if (!fragmentExists(codeSnippet)) {
            throw new IllegalArgumentException("unregistered CodeSnippet: " + codeSnippet);
        }
        return fragmentManager.getParentCodeFile(codeSnippet);
    }

    public Element getCodeSnippetAst(CodeSnippet codeSnippet) {
        return getCodeSnippetAst(codeSnippet, getRevision(getParentCodeFile(codeSnippet)));
    }

    public Element getCodeSnippetAst(CodeSnippet codeSnippet, int revision) {
        Document queriedDoc = executeQuery(codeSnippet, revision, null);
        List<Node> nodes = new ArrayList<Node>();
        getSelectedNodes(queriedDoc.getDocumentElement(), nodes);
        Element root = queriedDoc.createElement("root");
        for (Node node : nodes) {
            root.appendChild(node);
        }
        return root;
    }

    public Node getCodeSnippetNode(CodeSnippet codeSnippet, DisplayType dType, Format format, Ident... idents) {
        return getCodeSnippetNode(codeSnippet, getRevision(getParentCodeFile(codeSnippet)), dType, format, idents);
    }

    public Node getCodeSnippetNode(CodeSnippet codeSnippet, int revision, DisplayType dType, Format format, Ident... idents) {
        if (codeSnippet == null) {
            throw new IllegalArgumentException("codeSnippet must not be null");
        }
        if (!fragmentExists(codeSnippet)) {
            throw new IllegalArgumentException("unregistered Fragment: " + codeSnippet);
        }
        if (format == null) {
            format = new Format("unchanged");
        }
        CodeFile codeFile = getParentCodeFile(codeSnippet);
        Document queriedDoc = executeQuery(codeSnippet, revision, format);
        ParserConfig pc = ParserRegistry.getInstance().getParser(codeFile);
        FormattingConfiguration fc = new FormattingConfiguration(pc);
        if (idents.length > 0) {
            boolean selectedFound = false;
            for (int i = 0; i < idents.length; i++) {
                if (!selectedFound) {
                    selectedFound = (idents[i].equals(Templates.SELECTED));
                }
            }
            if (!selectedFound) {
                fc.addTemplate(Templates.SELECTED);
            }
            for (Ident ident : idents) {
                fc.addTemplate(ident);
            }
        } else {
            List<AbstractFormattingTemplate> templates = pc.getTemplates(TemplateType.CODE_SNIPPET);
            boolean selectedFound = false;
            for (AbstractFormattingTemplate template : templates) {
                if (template.getIdent().equals(Templates.SELECTED)) {
                    selectedFound = true;
                    break;
                }
            }
            if (!selectedFound) {
                fc.addTemplate(Templates.SELECTED);
            }
            for (AbstractFormattingTemplate templ : templates) {
                fc.addTemplate(templ);
            }
        }
        TreeNode root = fc.convertToNodes(queriedDoc.getDocumentElement());
        TreeNode node = queryManager.cutAndFormat(root);
        Document doc = fc.makeXml(node);
        if (dType != null && dType.equals(DisplayType.INLINE)) {
            normalize(doc);
        }
        return doc.getDocumentElement();
    }

    public Query getQuery(CodeSnippet snippet) {
        IPath queryPath = fragmentManager.getQueryPath(snippet);
        return Query.readQueryFromFile(queryPath.toOSString());
    }

    public CodeSnippet updateQuery(CodeSnippet snippet, Query query) {
        CodeSnippet cs = null;
        cs = createCodeSnippet(getParentCodeFile(snippet), snippet.getName(), query);
        return cs;
    }

    public void updateCodeFile(CodeFile codeFile) {
        contentChanged(codeFile);
    }

    /**
	 * Normalizes an xml document which in this case means to remove all line
	 * breaks and skip whitespaces. This is just a workaround and should be
	 * replaced by a more sophisticated pretty printing.
	 * 
	 * @param doc
	 */
    private void normalize(Document doc) {
        DocumentTraversal traversable = (DocumentTraversal) doc;
        NodeIterator iterator = traversable.createNodeIterator((Node) doc, NodeFilter.SHOW_TEXT, null, false);
        Node n;
        while ((n = iterator.nextNode()) != null) {
            Text text = (Text) n;
            String data = text.getData();
            data = data.replaceAll("[\t\n\r\f]+", "");
            data = data.replaceAll("\\s{2,}", " ");
            text.setData(data);
        }
    }

    /**
	 * Helper method that populates an empty node list with all selected nodes
	 * that can be found in the AST document.
	 * 
	 * @param root
	 *            element
	 * @param result
	 *            (a list of Nodes)
	 */
    private void getSelectedNodes(Node rootNode, List<Node> result) {
        if (rootNode instanceof Element) {
            Element e = (Element) rootNode;
            if (!e.getAttribute("selected").equals("")) {
                result.add(e);
            } else {
                NodeList list = e.getChildNodes();
                for (int i = 0; i < list.getLength(); i++) {
                    getSelectedNodes(list.item(i), result);
                }
            }
        }
    }

    /**
	 * Prints a given tree node to the console using the spacer. (This is just
	 * for debugging)
	 * 
	 * @param node
	 * @param spacer
	 */
    public static void printTreeNode(TreeNode node, String spacer) {
        StringBuffer buff = new StringBuffer();
        buff.append("(");
        for (NodeInformation info : node.getInformation()) {
            buff.append(info.toString() + ",");
        }
        buff.append(")");
        System.out.println(spacer + node.toString() + buff.toString());
        spacer += spacer.substring(spacer.length() - 1);
        for (TreeNode n : node.getChildren()) {
            printTreeNode(n, spacer);
        }
    }

    public static void printTokenList(List<TokenNode> token) {
        System.out.println("####");
        for (TokenNode node : token) {
            Token t = node.getToken();
            System.out.println(t.getText() + "  " + t.getLine() + " " + t.getCol() + " " + t.getEndLine() + " " + t.getEndCol());
        }
        System.out.println("####");
    }

    public static void printRangeList(List<Range> range) {
        for (Range range2 : range) {
            System.out.println(range2.toString());
        }
    }

    /**
	 * Prints all selected tree nodes to the console using the spacer. (This is
	 * just for debugging)
	 * 
	 * @param node
	 * @param spacer
	 */
    public static void printSelectedTreeNodes(TreeNode node, String spacer) {
        boolean selected = false;
        for (NodeInformation info : node.getInformation()) {
            if (info instanceof SelectedInformation) {
                selected = true;
            }
        }
        if (selected) {
            printTreeNode(node, spacer);
            spacer += spacer.substring(spacer.length() - 1);
        }
        for (TreeNode n : node.getChildren()) {
            printSelectedTreeNodes(n, spacer);
        }
    }

    private void saveStream(Fragment fragment, Closeable stream) {
        UUID uuid = fragment.getUUID();
        List<Closeable> list = streamMap.get(uuid);
        if (list == null) {
            list = new LinkedList<Closeable>();
            streamMap.put(uuid, list);
        }
        list.add(stream);
    }

    private void closeStreams(Fragment fragment) {
        UUID uuid = fragment.getUUID();
        List<Closeable> list = streamMap.get(uuid);
        if (list != null) {
            for (Closeable c : list) {
                try {
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        streamMap.remove(uuid);
    }

    private void closeStreamsRecursively(Fragment root) {
        for (Fragment child : root.getChildren()) {
            closeStreamsRecursively(child);
        }
        closeStreams(root);
    }

    public InputStream getInputStream(Fragment fragment) {
        if (fragment instanceof CodeSnippet) {
            long start = System.currentTimeMillis();
            InputStream is = getCodeSnippetInputStream((CodeSnippet) fragment);
            long end = System.currentTimeMillis();
            logger.debug("codesnippetstream: " + (end - start) + "ms");
            BufferedInputStream bi = new BufferedInputStream(is);
            saveStream(fragment, bi);
            return bi;
        }
        IPath fragmentPath = fragmentManager.getPathOf(fragment);
        return getInputStream(fragment, fragmentPath);
    }

    public InputStream getInputStream(CodeFile codeFile, int revision) {
        IPath path = fragmentManager.getPathOf(codeFile, revision);
        return getInputStream(codeFile, path);
    }

    private InputStream getInputStream(Fragment fragment, IPath path) {
        if (path != null && path.toFile().exists() && path.toFile().isFile()) {
            try {
                BufferedInputStream bi = new BufferedInputStream(new FileInputStream(path.toFile()));
                saveStream(fragment, bi);
                return bi;
            } catch (FileNotFoundException fnfe) {
                logger.error("Could not find a file (" + path.toOSString() + ") to create an InputStream for," + " which is strange, as I just checked for its existence!");
                fnfe.printStackTrace();
            }
        }
        return null;
    }

    private InputStream getCodeSnippetInputStream(CodeSnippet cs) {
        Node node = getCodeSnippetNode(cs, DisplayType.BLOCK, getFormat(cs, "all"));
        String string = node.getTextContent();
        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(string.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        saveStream(cs, inputStream);
        return inputStream;
    }

    public OutputStream getOutputStream(Fragment fragment) {
        IPath fragmentPath = fragmentManager.getPathOf(fragment);
        if (fragmentPath != null && fragmentPath.toFile().exists() && fragmentPath.toFile().isFile()) {
            try {
                FileOutputStream fos = new FileOutputStream(fragmentPath.toFile());
                saveStream(fragment, fos);
                return fos;
            } catch (FileNotFoundException fnfe) {
                logger.error("Could not find a file (" + fragmentPath.toOSString() + ") to create an OutputStream for," + " which is strange, as I just checked for its existence!");
                fnfe.printStackTrace();
            }
        }
        return null;
    }

    public CodeSnippet createCodeSnippet(CodeFile parentCodeFile, String name, Query query) {
        Project project = getProjectOf(parentCodeFile);
        if (!getXfsrOptions().getStoreIdenticalCodeSnippets()) {
            if (fragmentManager.queryExists(project, query)) {
                return fragmentManager.getCodeSnippetsForQuery(project, query).get(0);
            }
        }
        CodeSnippet cs = new CodeSnippet(project.getCategory(CODE_SNIPPETS), name);
        CodeSnippetProcessor processor = new CodeSnippetProcessor(cs, parentCodeFile, query, this);
        processor.createFileRepresentation((String) null);
        processor.register();
        contentChanged();
        return cs;
    }

    public Fragment getFragment(UUID uuid) {
        return fragmentManager.getFragment(uuid);
    }

    public Collection<Fragment> getFragments(Project project, IFragmentFilter filter) {
        return fragmentManager.getFragments(project, filter);
    }

    /**
	 * Removes a fragment from the repository. If the removed fragment has
	 * children itself, those children are first removed.
	 */
    public List<Fragment> removeFragment(Fragment fragment) {
        List<Fragment> errorList = new LinkedList<Fragment>();
        if (fragmentExists(fragment)) {
            doRemoveFragment(fragment, errorList);
            contentChanged(fragment);
        }
        logger.debug("Fragments not deleted: " + errorList);
        return errorList;
    }

    /**
	 * Outsourced code for deleting a fragment recursively. This could not be
	 * done in the removeFragment method itself because then after every
	 * recursive deletion the listeners would have been notified. This should,
	 * however, only happen once, after all fragments have been deleted.
	 * 
	 */
    private void doRemoveFragment(Fragment fragment, List<Fragment> errorList) {
        List<Fragment> childListCopy = new LinkedList<Fragment>(fragment.getChildren());
        for (Fragment f : childListCopy) {
            doRemoveFragment(f, errorList);
        }
        Set<Fragment> dependentSet = new HashSet<Fragment>(fragmentManager.getDependentFragments(fragment));
        if (!dependentSet.isEmpty()) {
            for (Fragment f : dependentSet) {
                doRemoveFragment(f, errorList);
            }
        }
        closeStreams(fragment);
        doRemoveReferences(fragment);
        FragmentProcessor fp = fragmentManager.getFragmentProcessor(fragment);
        boolean deleted = fp.deleteFileRepresentation();
        if (!deleted && fragmentManager.getPathOf(fragment).toFile().exists()) {
            logger.warn("File representation of " + fragment + " has not been deleted");
            errorList.add(fragment);
        } else {
            fp.unregister();
            if (fragment.getParent() != null) {
                fragment.getParent().removeChild(fragment);
            }
        }
    }

    private void doRemoveReferences(Fragment fragment) {
        if (fragment instanceof Chapter) {
            Chapter chapter = (Chapter) fragment;
            if (!isTutorialChapter(chapter)) {
                for (Reference ref : getReferences(chapter)) {
                    removeReference(ref);
                    referenceManager.commit(ref);
                }
            }
        } else if (fragment instanceof EmbeddableFragment) {
            for (Reference ref : getReferences((EmbeddableFragment) fragment)) {
                removeReference(ref);
                referenceManager.commit(ref);
            }
        }
    }

    public Document executeQuery(CodeSnippetRef ref, Document codeFileDoc) {
        IPath queryPath = fragmentManager.getQueryPath(ref.getCodeSnippet());
        Query query = Query.readQueryFromFile(queryPath.toOSString());
        Queryer queryer = new Queryer(codeFileDoc, query, ref.getRefId());
        queryer.query();
        return codeFileDoc;
    }

    public Document executeQuery(CodeSnippet codeSnippet, Format format) {
        return executeQuery(codeSnippet, getRevision(getParentCodeFile(codeSnippet)), format);
    }

    public Document executeQuery(CodeSnippet codeSnippet, int revision, Format format) {
        CodeFile codeFile = getParentCodeFile(codeSnippet);
        Document codeFileDoc = getCodeFileAst(codeFile, revision);
        IPath queryPath = fragmentManager.getQueryPath(codeSnippet);
        Query query = Query.readQueryFromFile(queryPath.toOSString());
        queryManager.query(codeFileDoc, codeSnippet.getUUID(), query, format, null);
        return codeFileDoc;
    }

    public Document getCodeFileAst(CodeFile codeFile, int revision) {
        CodeFileProcessor cfp = (CodeFileProcessor) fragmentManager.getFragmentProcessor(codeFile);
        int rev = getLatestApplicableRevision(codeFile, revision);
        Document doc = cfp.getCachedDoc(rev);
        if (doc == null) {
            ParserConfig pc = ParserRegistry.getInstance().getParser(codeFile);
            if (pc != null) {
                IParser parser = pc.getParser();
                try {
                    doc = parser.getXmlAst(codeFile);
                } catch (DepthParseException dpe) {
                    dpe.printStackTrace();
                }
                cfp.setCachedDoc(doc, rev);
            }
        }
        doc.setUserData(DocumentUserData.FRAGMENT_ID, codeFile.getUUID(), null);
        return doc;
    }

    public Document getCodeFileAst(CodeFile codeFile) {
        return getCodeFileAst(codeFile, getRevision(codeFile));
    }

    private int getLatestApplicableRevision(CodeFile codeFile, int revision) {
        int rev = revisionManager.getRevisionIndexes(codeFile).get(0);
        for (Integer itg : revisionManager.getRevisionIndexes(codeFile)) {
            if (itg <= revision) {
                rev = itg;
            } else {
                break;
            }
        }
        return rev;
    }

    public String getRepositoryState() {
        StringBuilder sb = new StringBuilder();
        for (Project project : fragmentManager.getProjects()) {
            sb.append("Project: ").append(project.getName()).append("\n");
            for (Fragment fragment : project.getChildren()) {
                addChildOutput("", fragment, sb);
            }
        }
        return sb.toString();
    }

    private void addChildOutput(String spaces, Fragment fragment, StringBuilder sb) {
        sb.append(spaces).append("    ").append(fragment.getName()).append("     ").append(fragmentManager.getPathOf(fragment)).append("\n");
        String newSpaces = spaces + "    ";
        for (Fragment f : fragment.getChildren()) {
            addChildOutput(newSpaces, f, sb);
        }
    }

    public List<Project> getProjects() {
        return fragmentManager.getProjects();
    }

    public CodeSnippetRef addCodeSnippetToChapter(Chapter chapter, CodeSnippet codeSnippet) throws DepthCrossProjectRelationException {
        CodeSnippetRef ref = referenceManager.addReference(chapter, codeSnippet);
        contentChanged();
        return ref;
    }

    public ImageRef addImageToChapter(Chapter chapter, Image image) throws DepthCrossProjectRelationException {
        ImageRef ref = referenceManager.addReference(chapter, image);
        contentChanged();
        return ref;
    }

    public void commitChapterChanges(Chapter chapter) {
        referenceManager.commit(chapter);
        contentChanged(chapter);
    }

    public void rollbackChapterChanges(Chapter chapter) {
        referenceManager.rollback(chapter);
        contentChanged(chapter);
    }

    public Reference getReference(UUID refId) {
        return referenceManager.getReference(refId);
    }

    public List<? extends Reference> getReferences(Chapter chapter) {
        return referenceManager.getReferences(chapter, true);
    }

    public List<? extends Reference> getReferences(EmbeddableFragment fragment) {
        return referenceManager.getReferences(fragment, true);
    }

    public void removeReference(Reference ref) {
        referenceManager.removeReference(ref);
        contentChanged();
    }

    public List<CodeSnippetRef> getCodeSnippetReferences(Chapter chapter) {
        return referenceManager.getCodeSnippetReferences(chapter, true);
    }

    public List<ImageRef> getImageReferences(Chapter chapter) {
        return referenceManager.getImageReferences(chapter, true);
    }

    public List<CodeSnippetRef> getCodeSnippetReferences(CodeFile codeFile) {
        return referenceManager.getCodeSnippetReferences(codeFile, true);
    }

    public List<CodeSnippetRef> getCodeSnippetReferences(CodeSnippet codeSnippet) {
        return referenceManager.getCodeSnippetReferences(codeSnippet, true);
    }

    public List<ImageRef> getImageReferences(Image image) {
        return referenceManager.getImageReferences(image, true);
    }

    public boolean formatExists(CodeSnippet codeSnippet, Format format) {
        return formatManager.formatExists(codeSnippet, format);
    }

    public void registerFormat(CodeSnippet codeSnippet, Format format) {
        formatManager.registerFormat(codeSnippet, format);
    }

    public void unregisterFormat(CodeSnippet codeSnippet, Format format) {
        formatManager.unregisterFormat(codeSnippet, format);
    }

    public void updateFormat(CodeSnippet snippet, Format oldFormat, Format newFormat) {
        formatManager.updateFormat(snippet, oldFormat, newFormat);
    }

    public Format getFormat(CodeSnippet codeSnippet, String formatName) {
        return formatManager.getFormat(codeSnippet, formatName);
    }

    public List<Format> getValidFormats(CodeSnippet codeSnippet) {
        return formatManager.getValidFormats(codeSnippet);
    }

    public List<Format> getValidFormats(CodeSnippet codeSnippet, DisplayType display) {
        return formatManager.getValidFormats(codeSnippet, display);
    }

    public boolean isDefaultFormat(CodeSnippet codeSnippet, String formatName) {
        return formatManager.isDefaultFormat(codeSnippet, formatName);
    }

    public void addFormatChangedListener(IFormatChangedListener listener) {
        formatManager.addFormatChangedListener(listener);
    }

    public void removeFormatChangedListener(IFormatChangedListener listener) {
        formatManager.removeFormatChangedListener(listener);
    }

    public String toString() {
        return getRepositoryState();
    }

    private class RepositoryObserver implements Observer {

        public void update(Observable o, Object arg) {
            XmlFileSystemRepository.this.saveConfigDocuments();
        }
    }

    private class OptionListener implements XfsrOptionListener {

        public void pathChanged(XfsrOptionEvent e) {
            unloadProjects();
            try {
                loadProjects();
                contentChanged();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void storeIdenticalChanged(XfsrOptionEvent e) {
        }
    }

    private class FormatListener implements IFormatChangedListener {

        public void formatChanged(FormatChangedEvent e) {
            saveConfigDocuments();
        }
    }

    public int getLatestRevision(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("project must not be null");
        }
        if (!fragmentExists(project)) {
            throw new IllegalArgumentException("unregistered Fragment: " + project);
        }
        return revisionManager.getLatestRevision(project);
    }

    public int getRevision(CodeFile codeFile) {
        if (codeFile == null) {
            throw new IllegalArgumentException("codeFile must not be null");
        }
        if (!fragmentExists(codeFile)) {
            throw new IllegalArgumentException("unregistered Fragment: " + codeFile);
        }
        List<Integer> revList = revisionManager.getRevisionIndexes(codeFile);
        assert revList != null && revList.size() > 0 : "A registered CodeFile must have at least 1 revision";
        return revList.get(revList.size() - 1);
    }

    public TreeNode getTreeNodeAst(CodeFile codeFile) {
        Document doc = getCodeFileAst(codeFile);
        ParserConfig pc = ParserRegistry.getInstance().getParser(codeFile);
        FormattingConfiguration fc = new FormattingConfiguration(pc);
        fc.addTemplate(Templates.SELECTED);
        fc.addTemplate(Templates.TOKENTYPE);
        fc.addTemplate(Templates.ASTNAME);
        TreeNode root = fc.convertToNodes(doc.getDocumentElement());
        return root;
    }

    public void applyQuery(Document codeFileDoc, CodeSnippetRef ref) {
        IPath queryPath = fragmentManager.getQueryPath(ref.getCodeSnippet());
        Query query = Query.readQueryFromFile(queryPath.toOSString());
        queryManager.query(codeFileDoc, ref.getCodeSnippetId(), query, null, ref.getRefId());
    }

    public XfsrRevisionManager getRevisionManager() {
        return revisionManager;
    }

    /**
	 * 
	 * @param codeSnippet
	 * @param revision
	 * @param format can be null
	 * @return
	 */
    public String getCodeSnippetText(CodeSnippet codeSnippet, int revision, Format format) {
        if (codeSnippet == null) {
            throw new IllegalArgumentException("codeSnippet must not be null");
        }
        if (!fragmentExists(codeSnippet)) {
            throw new IllegalArgumentException("unregistered Fragment: " + codeSnippet);
        }
        CodeFile codeFile = getParentCodeFile(codeSnippet);
        int rev = getLatestApplicableRevision(codeFile, revision);
        Node csNode = getCodeSnippetNode(codeSnippet, rev, DisplayType.BLOCK, format);
        return csNode.getTextContent();
    }

    public CodeFile updateCodeFile(CodeFile codeFile, String filename, boolean rename) throws DepthFragmentAlreadyExistsException, DepthParseException, DepthIllegalFragmentTreeException, DepthMultipleParserException, DepthIllegalRevisionException {
        return updateCodeFile(codeFile, getLatestRevision(getProjectOf(codeFile)) + 1, filename, rename);
    }

    public CodeFile updateCodeFile(CodeFile codeFile, int revision, String filename, boolean rename) throws DepthFragmentAlreadyExistsException, DepthParseException, DepthIllegalFragmentTreeException, DepthMultipleParserException, DepthIllegalRevisionException {
        Project project = getProjectOf(codeFile);
        ParserConfig pc = ParserRegistry.getInstance().getParser(codeFile);
        IParser parser = pc.getParser();
        revisionManager.addRevisionIndex(project, codeFile, revision);
        CodeFileProcessor processor = (CodeFileProcessor) fragmentManager.getFragmentProcessor(codeFile);
        processor.createFileRepresentation(filename);
        try {
            Document doc = parser.getXmlAst(codeFile);
            processor.setCachedDoc(doc, revision);
        } catch (DepthParseException dpe) {
            closeStreams(codeFile);
            processor.deleteFileRepresentation(revision);
            revisionManager.removeRevisionIndex(project, codeFile, revision);
            throw dpe;
        }
        if (rename) {
            String name = new File(filename).getName();
            codeFile.setName(name);
        }
        markChangedCodeSnippetRefs(codeFile);
        contentChanged(codeFile);
        return codeFile;
    }

    private void markChangedCodeSnippetRefs(CodeFile codeFile) {
        Map<CodeSnippet, Boolean> checked = new HashMap<CodeSnippet, Boolean>();
        List<Integer> revisions = revisionManager.getRevisionIndexes(codeFile);
        int size = revisions.size();
        int rev1 = revisions.get(size - 2).intValue();
        int rev2 = revisions.get(size - 1).intValue();
        List<CodeSnippetRef> refs = getCodeSnippetReferences(codeFile);
        for (CodeSnippetRef codeSnippetRef : refs) {
            CodeSnippet snippet = codeSnippetRef.getCodeSnippet();
            if (!checked.containsKey(snippet)) {
                String textRev1 = getCodeSnippetText(snippet, rev1, null);
                String textRev2 = getCodeSnippetText(snippet, rev2, null);
                boolean changed = !textRev1.equals(textRev2);
                checked.put(snippet, changed);
            }
            if (checked.get(snippet)) {
                codeSnippetRef.setChangedCodeSnippet(checked.get(snippet));
            }
        }
    }

    /**
	 * This is going to change in the next version.
	 * In order to include Formats in the comparison, one
	 * must not search for CodeSnippets (they have possibly
	 * multiple Formats), but CodeSnippet occurrences in the
	 * documentation.
	 * 
	 * Finds all CodeSnippets that have changed between two revisions.
	 */
    public List<CodeSnippet> getChangedCodeSnippets(Project project, int rev1, int rev2) {
        List<CodeSnippet> changedSnippets = new LinkedList<CodeSnippet>();
        for (Fragment fragment : getFragments(project, FragmentFilter.CODESNIPPET_FILTER)) {
            CodeSnippet cs = (CodeSnippet) fragment;
            String textRev1 = getCodeSnippetText(cs, rev1, null);
            String textRev2 = getCodeSnippetText(cs, rev2, null);
            if (!textRev1.equals(textRev2)) {
                changedSnippets.add(cs);
            }
        }
        return changedSnippets;
    }

    public List<CodeSnippetRef> getChangedCodeSnippetReferences() {
        List<CodeSnippetRef> changedReferences = new LinkedList<CodeSnippetRef>();
        for (Project p : getProjects()) {
            for (Fragment fragment : getFragments(p, FragmentFilter.CODESNIPPET_FILTER)) {
                CodeSnippet snippet = (CodeSnippet) fragment;
                for (CodeSnippetRef ref : getCodeSnippetReferences(snippet)) {
                    if (ref.getChangedCodeSnippet()) {
                        changedReferences.add(ref);
                    }
                }
            }
        }
        return changedReferences;
    }
}
