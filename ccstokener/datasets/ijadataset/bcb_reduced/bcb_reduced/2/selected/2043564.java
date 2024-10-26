package org.argouml.kernel;

import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Category;
import org.argouml.BOTL.BOTLXMLFileHandler;
import org.argouml.BOTL.FigBotlElement;
import org.argouml.BOTL.MetaModelBuilder;
import org.argouml.BOTL.ModelBuilderException;
import org.argouml.BOTL.RuleSetBuilder;
import org.argouml.BOTL.RuleSetVerificator;
import org.argouml.BOTLDemo.BOTLDemo;
import org.argouml.application.api.Argo;
import org.argouml.cognitive.Designer;
import org.argouml.cognitive.ToDoList;
import org.argouml.cognitive.checklist.ChecklistStatus;
import org.argouml.cognitive.critics.Agency;
import org.argouml.cognitive.critics.Critic;
import org.argouml.cognitive.critics.ui.CriticBrowserDialog;
import org.argouml.cognitive.ui.DesignIssuesDialog;
import org.argouml.cognitive.ui.GoalsDialog;
import org.argouml.cognitive.ui.TabToDo;
import org.argouml.cognitive.ui.ToDoPane;
import org.argouml.model.uml.UmlFactory;
import org.argouml.model.uml.UmlHelper;
import org.argouml.model.uml.modelmanagement.ModelManagementHelper;
import org.argouml.ui.ArgoDiagram;
import org.argouml.ui.FindDialog;
import org.argouml.ui.NavigatorConfigDialog;
import org.argouml.ui.NavigatorPane;
import org.argouml.ui.ProjectBrowser;
import org.argouml.ui.TabResults;
import org.argouml.ui.UsageStatistic;
import org.argouml.uml.ProfileJava;
import org.argouml.uml.ProjectMemberModel;
import org.argouml.uml.UMLChangeRegistry;
import org.argouml.uml.diagram.ProjectMemberDiagram;
import org.argouml.uml.diagram.botl_model.ui.UMLBOTLDestinationDiagram;
import org.argouml.uml.diagram.botl_model.ui.UMLBOTLSourceDiagram;
import org.argouml.uml.diagram.botl_obj_dest.ui.UMLBOTLObjectDestinationDiagram;
import org.argouml.uml.diagram.botl_obj_src.ui.UMLBOTLObjectSourceDiagram;
import org.argouml.uml.diagram.botl_rule.ui.FigBOTLArrow;
import org.argouml.uml.diagram.botl_rule.ui.UMLBOTLRuleDiagram;
import org.argouml.uml.diagram.static_structure.ui.UMLClassDiagram;
import org.argouml.uml.diagram.ui.ModeCreateEdgeAndNode;
import org.argouml.uml.diagram.ui.SelectionWButtons;
import org.argouml.uml.diagram.ui.UMLDiagram;
import org.argouml.uml.diagram.use_case.ui.UMLUseCaseDiagram;
import org.argouml.uml.generator.GenerationPreferences;
import org.argouml.util.ChangeRegistry;
import org.argouml.util.SubInputStream;
import org.argouml.util.Trash;
import org.argouml.xml.argo.ArgoParser;
import org.argouml.xml.pgml.PGMLParser;
import org.argouml.xml.xmi.XMIParser;
import org.argouml.xml.xmi.XMIReader;
import org.tigris.gef.base.Diagram;
import org.tigris.gef.ocl.OCLExpander;
import org.tigris.gef.ocl.TemplateReader;
import org.tigris.gef.presentation.Fig;
import org.tigris.gef.util.Dbg;
import org.tigris.gef.util.Util;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.novosoft.uml.MBase;
import ru.novosoft.uml.foundation.core.MClassifier;
import ru.novosoft.uml.foundation.core.MComment;
import ru.novosoft.uml.foundation.core.MModelElement;
import ru.novosoft.uml.foundation.core.MNamespace;
import ru.novosoft.uml.model_management.MModel;
import de.tum.in.botl.ruleSet.RuleSetFactory;
import de.tum.in.botl.ruleSet.RuleSet;
import de.tum.in.botl.util.Config;

/** A datastructure that represents the designer's current project.  A
 *  Project consists of diagrams and UML models. */
public class Project implements java.io.Serializable {

    public static final String BOTL_FILE_EXT = ".botl";

    public static final String SEPARATOR = "/";

    public static final String COMPRESSED_FILE_EXT = ".zargo";

    public static final String UNCOMPRESSED_FILE_EXT = ".argo";

    public static final String PROJECT_FILE_EXT = ".argo";

    public static final String TEMPLATES = "/org/argouml/templates/";

    public static String ARGO_TEE = "/org/argouml/xml/dtd/argo.tee";

    public static final String UNTITLED_FILE = "Untitled";

    protected static OCLExpander expander = null;

    protected static boolean botlProject = false;

    private static Config botlConfig = Config.getInstance();

    private URL _url = null;

    protected ChangeRegistry _saveRegistry;

    public String _authorname = "";

    public String _description = "";

    public String _version = "";

    public Vector _searchpath = new Vector();

    public Vector _members = new Vector();

    public String _historyFile = "";

    public Vector _models = new Vector();

    public Vector _diagrams = new Vector();

    protected MModel _defaultModel = null;

    public boolean _needsSave = false;

    public MNamespace _curModel = null;

    public Hashtable _definedTypes = new Hashtable(80);

    public HashMap _UUIDRefs = null;

    public GenerationPreferences _cgPrefs = new GenerationPreferences();

    public transient VetoableChangeSupport _vetoSupport = null;

    public boolean isBOTLProject() {
        return botlProject;
    }

    public void setBOTLProject(boolean bp) {
        botlProject = bp;
    }

    public Config getBotlConfig() {
        return this.botlConfig;
    }

    /**
 	 * Makes an empty BOTL Project and creates the stereotype Key AttributeImpl
 	 * @return
 	 */
    public static Project makeEmptyBOTLProject() {
        Argo.log.info("making empty BOTL project");
        botlProject = true;
        MModel m1 = UmlFactory.getFactory().getModelManagement().createModel();
        m1.setName("untitledBOTLModel");
        Project p = new Project(m1);
        p.addSearchPath("PROJECT_DIR");
        return p;
    }

    /**
	 * Exports a BOTLProject to a XML File
	 * @param path
	 * @param name
	 * @return
	 */
    public boolean export2XML(String path, String name) {
        RuleSetBuilder b = new RuleSetBuilder(botlConfig.getMetamodelFactory(), botlConfig.getDefaultRuleSetFactory());
        BOTLXMLFileHandler f = new BOTLXMLFileHandler(botlConfig.getDefaultRuleSetFactory());
        RuleSet r = null;
        try {
            r = b.createRuleSetFromDiagrams();
        } catch (ModelBuilderException e) {
            ProjectBrowser pb = ProjectBrowser.TheInstance;
            String m = e.getMessage();
            if (m.length() > 150) {
                m = m.substring(0, 150) + "\n" + m.substring(150);
            }
            JOptionPane.showMessageDialog(pb, m + "!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            System.out.println("Started to save XML +Rule " + r);
            f.saveXML(r, path + name);
            System.out.println("XML Saved");
        } catch (Exception e) {
            ProjectBrowser pb = ProjectBrowser.TheInstance;
            String m = e.getMessage();
            if (m.length() > 150) {
                m = m.substring(0, 150) + "\n" + m.substring(150);
            }
            JOptionPane.showMessageDialog(pb, m + "!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        System.out.println("Export to " + path + name);
        return true;
    }

    /**
	 * Imports a BOTLProject from a XML File
	 * @param path
	 * @param name
	 * @return
	 */
    public boolean importFromXML(String path, String name) {
        RuleSetFactory ruleSetFactory = botlConfig.getDefaultRuleSetFactory();
        MetaModelBuilder b = new MetaModelBuilder(ruleSetFactory);
        BOTLXMLFileHandler f = new BOTLXMLFileHandler(botlConfig.getDefaultRuleSetFactory());
        try {
            RuleSet r = f.loadXML(path + "/" + name);
            b.buildAllDiagramsFromRuleSet(r);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ProjectBrowser.TheInstance, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
        System.out.println("Loading " + path + "/" + name);
        return true;
    }

    /**
	 * Imports BOTL Diagrams/Rules from an XML File to a BOTL-Project;
	 * @param path
	 * @param name
	 * @return
	 */
    public boolean importFromXML2(String path, String name) {
        RuleSetFactory ruleSetFactory = botlConfig.getDefaultRuleSetFactory();
        MetaModelBuilder b = new MetaModelBuilder(ruleSetFactory);
        BOTLXMLFileHandler f = new BOTLXMLFileHandler(ruleSetFactory);
        try {
            RuleSet r = f.loadXML(path + "/" + name);
            b.importRuleSetToProject(r);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ProjectBrowser.TheInstance, e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        System.out.println("Importing " + path + "/" + name);
        return true;
    }

    public static Category cat = Category.getInstance(org.argouml.kernel.Project.class);

    public Project(File file) throws MalformedURLException, IOException {
        this(Util.fileToURL(file));
    }

    public Project(URL url) {
        this();
        String urlString = url.toString();
        int lastDot = urlString.lastIndexOf(".");
        String suffix = "";
        if (lastDot >= 0) {
            suffix = urlString.substring(lastDot).toLowerCase();
        }
        if (suffix.equals(BOTL_FILE_EXT)) {
            _url = Util.fixURLExtension(url, BOTL_FILE_EXT);
        } else {
            _url = Util.fixURLExtension(url, COMPRESSED_FILE_EXT);
        }
        _saveRegistry = new UMLChangeRegistry();
    }

    public Project() {
        _saveRegistry = new UMLChangeRegistry();
        setDefaultModel(ProfileJava.loadProfileModel());
    }

    public Project(MModel model) {
        this();
        Argo.log.info("making empty project with model: " + model.getName());
        _saveRegistry = new UMLChangeRegistry();
        addSearchPath("PROJECT_DIR");
        if (!botlProject) {
            try {
                addMember(new UMLClassDiagram(model));
                addMember(new UMLUseCaseDiagram(model));
                addMember(model);
                setNeedsSave(false);
            } catch (PropertyVetoException pve) {
            }
        } else {
            try {
                addMember(new UMLBOTLSourceDiagram(model));
                addMember(new UMLBOTLDestinationDiagram(model));
                UMLBOTLRuleDiagram d = new UMLBOTLRuleDiagram(model);
                FigBOTLArrow fba = new FigBOTLArrow();
                MComment arrow = UmlFactory.getFactory().getCore().buildComment(d.getOwner());
                arrow.setName("Object for BOTL Arrow");
                fba.setOwner(arrow);
                d.add(fba);
                addMember(d);
                addMember(model);
                setNeedsSave(false);
            } catch (PropertyVetoException pve) {
            }
        }
        Runnable resetStatsLater = new ResetStatsLater();
        org.argouml.application.Main.addPostLoadAction(resetStatsLater);
        setCurrentNamespace(model);
    }

    /**   This method creates a project from the specified URL
     *
     *    Unlike the constructor which forces an .argo extension
     *    This method will attempt to load a raw XMI file
     * <P>
     * This method can fail in several different ways. Either by throwing
     * an exception or by having the ArgoParser.SINGLETON.getLastLoadStatus()
     * set to not true.
     * <P>
     * Needs-more-work: This method NEEDS a refactoring.
     */
    public static Project loadProject(URL url) throws IOException, Exception {
        Project p = null;
        String urlString = url.toString();
        int lastDot = urlString.lastIndexOf(".");
        String suffix = "";
        if (lastDot >= 0) {
            suffix = urlString.substring(lastDot).toLowerCase();
        }
        if (suffix.equals(".xmi")) {
            p = new Project();
            XMIParser.SINGLETON.readModels(p, url);
            MModel model = XMIParser.SINGLETON.getCurModel();
            UmlHelper.getHelper().addListenersToModel(model);
            p._UUIDRefs = XMIParser.SINGLETON.getUUIDRefs();
            try {
                p.addMember(model);
                p.setNeedsSave(false);
            } catch (PropertyVetoException pve) {
            }
            org.argouml.application.Main.addPostLoadAction(new ResetStatsLater());
        } else if ((suffix.equals(COMPRESSED_FILE_EXT)) || (suffix.equals(BOTL_FILE_EXT))) {
            try {
                ZipInputStream zis = new ZipInputStream(url.openStream());
                String name = zis.getNextEntry().getName();
                while (!name.endsWith(PROJECT_FILE_EXT)) {
                    name = zis.getNextEntry().getName();
                }
                System.out.println("ARGO NAME: " + name);
                ArgoParser.SINGLETON.setURL(url);
                ArgoParser.SINGLETON.readProject(zis, false);
                p = ArgoParser.SINGLETON.getProject();
                zis.close();
                if (suffix.equals(BOTL_FILE_EXT)) {
                    p.setBOTLProject(true);
                } else {
                    p.setBOTLProject(false);
                }
            } catch (Exception e) {
                cat.error("Oops, something went wrong in Project.loadProject ");
                cat.error(e);
                throw e;
            }
            try {
                p.loadZippedProjectMembers(url);
            } catch (IOException e) {
                cat.error("Project file corrupted");
                cat.error(e);
                throw e;
            }
            p.postLoad();
        } else {
            ArgoParser.SINGLETON.readProject(url);
            p = ArgoParser.SINGLETON.getProject();
            p.loadAllMembers();
            p.postLoad();
        }
        return p;
    }

    /**
     * Loads a model (XMI only) from a .zargo file. BE ADVISED this method has a side
     * effect. It sets _UUIDREFS to the model.
     * <p>
     * If there is a problem with the xmi file, an error is set in the
     * ArgoParser.SINGLETON.getLastLoadStatus() field. This needs to be
     * examined by the calling function.
     *
     * @param url The url with the .zargo file
     * @return MModel The model loaded
     * @throws IOException Thrown if the model or the .zargo file itself is corrupted in any way.
     */
    public MModel loadModelFromXMI(URL url) throws IOException {
        ZipInputStream zis = new ZipInputStream(url.openStream());
        String name = zis.getNextEntry().getName();
        while (!name.endsWith(".xmi")) {
            name = zis.getNextEntry().getName();
        }
        Argo.log.info("Loading Model from " + url);
        XMIReader xmiReader = null;
        try {
            xmiReader = new org.argouml.xml.xmi.XMIReader();
        } catch (SAXException se) {
        } catch (ParserConfigurationException pc) {
        }
        MModel mmodel = null;
        InputSource source = new InputSource(zis);
        source.setEncoding("UTF-8");
        try {
            mmodel = xmiReader.parse(new InputSource(zis));
        } catch (ClassCastException cc) {
            ArgoParser.SINGLETON.setLastLoadStatus(false);
            ArgoParser.SINGLETON.setLastLoadMessage("XMI file " + url.toString() + " could not be " + "parsed.");
        }
        if (xmiReader.getErrors()) {
            ArgoParser.SINGLETON.setLastLoadStatus(false);
            ArgoParser.SINGLETON.setLastLoadMessage("XMI file " + url.toString() + " could not be " + "parsed.");
        }
        UmlHelper.getHelper().addListenersToModel(mmodel);
        try {
            addMember(mmodel);
        } catch (PropertyVetoException pv) {
            throw new IOException("The model from XMI file" + url.toString() + "could not be added to the project.");
        }
        _UUIDRefs = new HashMap(xmiReader.getXMIUUIDToObjectMap());
        return mmodel;
    }

    /**
     * Loads all the members from a zipped input stream.
     *
     * @throws IOException if there is something wrong with the zipped archive
     *                     or with the model.
     * @throws PropertyVetoException if the adding of a diagram is vetoed.
     */
    public void loadZippedProjectMembers(URL url) throws IOException, PropertyVetoException {
        loadModelFromXMI(url);
        try {
            PGMLParser.SINGLETON.setOwnerRegistry(_UUIDRefs);
            ZipInputStream zis = new ZipInputStream(url.openStream());
            SubInputStream sub = new SubInputStream(zis);
            ZipEntry currentEntry = null;
            while ((currentEntry = sub.getNextEntry()) != null) {
                if (currentEntry.getName().endsWith(".pgml")) {
                    Argo.log.info("Now going to load " + currentEntry.getName() + " from ZipInputStream");
                    ArgoDiagram d = (ArgoDiagram) PGMLParser.SINGLETON.readDiagram(sub, false);
                    if (d == null) System.out.println("ERROR: Cannot load diagram " + currentEntry.getName()); else addMember(d);
                    Argo.log.info("Finished loading " + currentEntry.getName());
                }
            }
            zis.close();
        } catch (IOException e) {
            ArgoParser.SINGLETON.setLastLoadStatus(false);
            ArgoParser.SINGLETON.setLastLoadMessage(e.toString());
            System.out.println("Oops, something went wrong in Project.loadZippedProjectMembers() " + e);
            e.printStackTrace();
            throw e;
        }
    }

    public static Project makeEmptyProject() {
        Argo.log.info("making empty project");
        botlProject = false;
        MModel m1 = UmlFactory.getFactory().getModelManagement().createModel();
        m1.setName("untitledModel");
        Project p = new Project(m1);
        p.addSearchPath("PROJECT_DIR");
        return p;
    }

    /**
     * Added Eugenio's patches to load 0.8.1 projects.
     */
    public String getBaseName() {
        String n = getName();
        if (n.endsWith(COMPRESSED_FILE_EXT)) {
            return n.substring(0, n.length() - COMPRESSED_FILE_EXT.length());
        }
        if (n.endsWith(UNCOMPRESSED_FILE_EXT)) {
            return n.substring(0, n.length() - UNCOMPRESSED_FILE_EXT.length());
        }
        return n;
    }

    public String getName() {
        if (_url == null) return UNTITLED_FILE;
        String name = _url.getFile();
        int i = name.lastIndexOf('/');
        return name.substring(i + 1);
    }

    public void setName(String n) throws PropertyVetoException, MalformedURLException {
        String s = "";
        if (getURL() != null) s = getURL().toString();
        s = s.substring(0, s.lastIndexOf("/") + 1) + n;
        setURL(new URL(s));
    }

    public URL getURL() {
        return _url;
    }

    public void setURL(URL url) throws PropertyVetoException {
        if (url != null) {
            url = Util.fixURLExtension(url, COMPRESSED_FILE_EXT);
        }
        getVetoSupport().fireVetoableChange("url", _url, url);
        System.out.println("Setting project URL from \"" + _url + "\" to \"" + url + "\".");
        _url = url;
    }

    public void setFile(File file) throws PropertyVetoException {
        try {
            URL url = Util.fileToURL(file);
            getVetoSupport().fireVetoableChange("url", _url, url);
            System.out.println("Setting project file name from \"" + _url + "\" to \"" + url + "\".");
            _url = url;
        } catch (MalformedURLException murle) {
            System.out.println("problem in setFile:" + file);
            murle.printStackTrace();
        } catch (IOException ex) {
            System.out.println("problem in setFile:" + file);
            ex.printStackTrace();
        }
    }

    public Vector getSearchPath() {
        return _searchpath;
    }

    public void addSearchPath(String searchpath) {
        _searchpath.addElement(searchpath);
    }

    public URL findMemberURLInSearchPath(String name) {
        String u = "";
        if (getURL() != null) u = getURL().toString();
        u = u.substring(0, u.lastIndexOf("/") + 1);
        URL url = null;
        try {
            url = new URL(u + name);
        } catch (MalformedURLException murle) {
            System.out.println("MalformedURLException in findMemberURLInSearchPath:" + u + name);
            murle.printStackTrace();
        }
        return url;
    }

    public Vector getMembers() {
        return _members;
    }

    public void addMember(String name, String type) {
        URL memberURL = findMemberURLInSearchPath(name);
        if (memberURL == null) {
            System.out.println("null memberURL");
            return;
        } else System.out.println("memberURL = " + memberURL);
        ProjectMember pm = findMemberByName(name);
        if (pm != null) return;
        if ("pgml".equals(type)) pm = new ProjectMemberDiagram(name, this); else if ("xmi".equals(type)) pm = new ProjectMemberModel(name, this); else throw new RuntimeException("Unknown member type " + type);
        _members.addElement(pm);
    }

    public void addMember(ArgoDiagram d) throws PropertyVetoException {
        ProjectMember pm = new ProjectMemberDiagram(d, this);
        addDiagram(d);
        _members.addElement(pm);
    }

    public void addMember(MModel m) throws PropertyVetoException {
        Iterator iter = _members.iterator();
        Object currentMember = null;
        boolean memberFound = false;
        while (iter.hasNext()) {
            currentMember = iter.next();
            if (currentMember instanceof ProjectMemberModel) {
                MModel currentModel = ((ProjectMemberModel) currentMember).getModel();
                if (currentModel == m) {
                    memberFound = true;
                    break;
                }
            }
        }
        if (!memberFound) {
            if (!_models.contains(m)) {
                addModel(m);
            }
            ProjectMember pm = new ProjectMemberModel(m, this);
            _members.addElement(pm);
        }
    }

    public void addModel(MNamespace m) throws PropertyVetoException {
        getVetoSupport().fireVetoableChange("Models", _models, null);
        if (!_models.contains(m)) _models.addElement(m);
        setCurrentNamespace(m);
        setNeedsSave(true);
    }

    /**
     * Removes a project member diagram completely from the project.
     * @param d
     */
    protected void removeProjectMemberDiagram(ArgoDiagram d) {
        try {
            removeDiagram(d);
        } catch (PropertyVetoException ve) {
        }
        Iterator it = _members.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof ProjectMemberDiagram) {
                ProjectMemberDiagram pmd = (ProjectMemberDiagram) obj;
                if (pmd.getDiagram() == d) {
                    _members.removeElement(pmd);
                    break;
                }
            }
        }
    }

    public ProjectMember findMemberByName(String name) {
        System.out.println("findMemberByName called for \"" + name + "\".");
        for (int i = 0; i < _members.size(); i++) {
            ProjectMember pm = (ProjectMember) _members.elementAt(i);
            if (name.equals(pm.getPlainName())) return pm;
        }
        System.out.println("Member \"" + name + "\" not found.");
        return null;
    }

    public static Project load(URL url) throws IOException, org.xml.sax.SAXException {
        Dbg.log("org.argouml.kernel.Project", "Reading " + url);
        ArgoParser.SINGLETON.readProject(url);
        Project p = ArgoParser.SINGLETON.getProject();
        p.loadAllMembers();
        p.postLoad();
        Dbg.log("org.argouml.kernel.Project", "Done reading " + url);
        return p;
    }

    public void loadMembersOfType(String type) {
        if (type == null) return;
        java.util.Enumeration enu = getMembers().elements();
        try {
            while (enu.hasMoreElements()) {
                ProjectMember pm = (ProjectMember) enu.nextElement();
                if (type.equalsIgnoreCase(pm.getType())) pm.load();
            }
        } catch (IOException ignore) {
            System.out.println("IOException in makeEmptyProject");
        } catch (org.xml.sax.SAXException ignore) {
            System.out.println("SAXException in makeEmptyProject");
        }
    }

    public void loadAllMembers() {
        loadMembersOfType("xmi");
        loadMembersOfType("argo");
        loadMembersOfType("pgml");
        loadMembersOfType("text");
        loadMembersOfType("html");
    }

    /**
     * There are known issues with saving, particularly
     * losing the xmi at save time. see issue
     * http://argouml.tigris.org/issues/show_bug.cgi?id=410
     *
     * It is also being considered to save out individual
     * xmi's from individuals diagrams to make
     * it easier to modularize the output of Argo.
     */
    public void save(boolean overwrite, File file) throws IOException, Exception {
        if (expander == null) {
            java.util.Hashtable templates = TemplateReader.readFile(ARGO_TEE);
            expander = new OCLExpander(templates);
        }
        preSave();
        ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(file));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
        ZipEntry zipEntry = new ZipEntry(getBaseName() + UNCOMPRESSED_FILE_EXT);
        stream.putNextEntry(zipEntry);
        expander.expand(writer, this, "", "");
        writer.flush();
        stream.closeEntry();
        String path = file.getParent();
        Argo.log.info("Dir ==" + path);
        int size = _members.size();
        try {
            for (int i = 0; i < size; i++) {
                ProjectMember p = (ProjectMember) _members.elementAt(i);
                if (!(p.getType().equalsIgnoreCase("xmi"))) {
                    Argo.log.info("Saving member of type: " + ((ProjectMember) _members.elementAt(i)).getType());
                    stream.putNextEntry(new ZipEntry(p.getName()));
                    p.save(path, overwrite, writer);
                    writer.flush();
                    stream.closeEntry();
                }
            }
            for (int i = 0; i < size; i++) {
                ProjectMember p = (ProjectMember) _members.elementAt(i);
                if (p.getType().equalsIgnoreCase("xmi")) {
                    Argo.log.info("Saving member of type: " + ((ProjectMember) _members.elementAt(i)).getType());
                    stream.putNextEntry(new ZipEntry(p.getName()));
                    p.save(path, overwrite, writer);
                }
            }
        } catch (IOException e) {
            System.out.println("hat nicht geklappt: " + e);
            e.printStackTrace();
        }
        writer.close();
        postSave();
        try {
            setFile(file);
        } catch (PropertyVetoException ex) {
        }
    }

    public String getAuthorname() {
        return _authorname;
    }

    public void setAuthorname(String s) {
        _authorname = s;
    }

    public String getVersion() {
        return _version;
    }

    public void setVersion(String s) {
        _version = s;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String s) {
        _description = s;
    }

    public String getHistoryFile() {
        return _historyFile;
    }

    public void setHistoryFile(String s) {
        _historyFile = s;
    }

    public void setNeedsSave(boolean newValue) {
        _saveRegistry.setChangeFlag(newValue);
    }

    public boolean needsSave() {
        return _saveRegistry.hasChanged();
    }

    public Vector getModels() {
        return _models;
    }

    public MNamespace getModel() {
        if (_models.size() != 1) return null;
        return (MNamespace) _models.elementAt(0);
    }

    public Vector getDefinedTypesVector() {
        return new Vector(_definedTypes.values());
    }

    public Hashtable getDefinedTypes() {
        return _definedTypes;
    }

    public void setDefinedTypes(Hashtable h) {
        _definedTypes = h;
    }

    public void defineType(MClassifier cls) {
        String name = cls.getName();
        if (name == null) name = "anon";
        _definedTypes.put(name, cls);
    }

    public MClassifier findType(String s) {
        if (s != null) s = s.trim();
        if (s == null || s.length() == 0) return null;
        MClassifier cls = null;
        int numModels = _models.size();
        for (int i = 0; i < numModels; i++) {
            cls = findTypeInModel(s, (MNamespace) _models.elementAt(i));
            if (cls != null) return cls;
        }
        cls = findTypeInModel(s, _defaultModel);
        if (cls != null) return cls;
        cls = (MClassifier) _definedTypes.get(s);
        if (cls == null) {
            cls = UmlFactory.getFactory().getCore().buildClass();
            cls.setName(s);
        }
        if (cls.getNamespace() == null) cls.setNamespace(getCurrentNamespace());
        return cls;
    }

    /**
     * Finds all figs on the diagrams for some project member, including the 
     * figs containing the member (so for some operation, the containing figclass
     * is returned).
     * @param member The member we are looking for. This can be a NSUML object but also another object.
     * @return Collection The collection with the figs.
     */
    public Collection findFigsForMember(Object member) {
        Collection figs = new ArrayList();
        Iterator it = getDiagrams().iterator();
        while (it.hasNext()) {
            ArgoDiagram diagram = (ArgoDiagram) it.next();
            Fig fig = diagram.getContainingFig(member);
            if (fig != null) {
                figs.add(fig);
            }
        }
        return figs;
    }

    public MClassifier findTypeInModel(String s, MNamespace ns) {
        Collection allClassifiers = ModelManagementHelper.getHelper().getAllModelElementsOfKind(ns, MClassifier.class);
        Iterator it = allClassifiers.iterator();
        while (it.hasNext()) {
            MClassifier classifier = (MClassifier) it.next();
            if (classifier.getName() != null && classifier.getName().equals(s)) return classifier;
        }
        return null;
    }

    public void setCurrentNamespace(MNamespace m) {
        _curModel = m;
    }

    public MNamespace getCurrentNamespace() {
        return _curModel;
    }

    public Vector getDiagrams() {
        return _diagrams;
    }

    public void addDiagram(ArgoDiagram d) throws PropertyVetoException {
        getVetoSupport().fireVetoableChange("Diagrams", _diagrams, null);
        _diagrams.addElement(d);
        d.addChangeRegistryAsListener(_saveRegistry);
        setNeedsSave(true);
    }

    /**
     * Removes a diagram from the list with diagrams. 
     * Removes (hopefully) the event listeners for this diagram.
     * Does not remove the diagram from the project members. This should not be called
     * directly. Use moveToTrash if you want to remove a diagram.
     * @param d
     * @throws PropertyVetoException
     */
    protected void removeDiagram(ArgoDiagram d) throws PropertyVetoException {
        getVetoSupport().fireVetoableChange("Diagrams", _diagrams, null);
        _diagrams.removeElement(d);
        d.removeChangeRegistryAsListener(_saveRegistry);
        setNeedsSave(true);
    }

    public int getPresentationCountFor(MModelElement me) {
        int presentations = 0;
        int size = _diagrams.size();
        for (int i = 0; i < size; i++) {
            Diagram d = (Diagram) _diagrams.elementAt(i);
            presentations += d.getLayer().presentationCountFor(me);
        }
        return presentations;
    }

    public Object getInitialTarget() {
        if (_diagrams.size() > 0) return _diagrams.elementAt(0);
        if (_models.size() > 0) return _models.elementAt(0);
        return null;
    }

    public void setGenerationPrefs(GenerationPreferences cgp) {
        _cgPrefs = cgp;
    }

    public GenerationPreferences getGenerationPrefs() {
        return _cgPrefs;
    }

    public void addVetoableChangeListener(VetoableChangeListener l) {
        getVetoSupport().removeVetoableChangeListener(l);
        getVetoSupport().addVetoableChangeListener(l);
    }

    public void removeVetoableChangeListener(VetoableChangeListener l) {
        getVetoSupport().removeVetoableChangeListener(l);
    }

    public VetoableChangeSupport getVetoSupport() {
        if (_vetoSupport == null) _vetoSupport = new VetoableChangeSupport(this);
        return _vetoSupport;
    }

    public void preSave() {
        for (int i = 0; i < _diagrams.size(); i++) ((Diagram) _diagrams.elementAt(i)).preSave();
        if (this.isBOTLProject() && RuleSetVerificator.commentsExist()) RuleSetVerificator.clearComments();
        UMLBOTLObjectDestinationDiagram d = BOTLDemo.getDestinationModelDiagram();
        if (d != null) moveToTrash(d);
    }

    public void postSave() {
        for (int i = 0; i < _diagrams.size(); i++) ((Diagram) _diagrams.elementAt(i)).postSave();
        setNeedsSave(false);
    }

    public void postLoad() {
        for (int i = 0; i < _diagrams.size(); i++) ((Diagram) _diagrams.elementAt(i)).postLoad();
        setNeedsSave(false);
        _UUIDRefs = null;
    }

    public void moveToTrash(Object obj) {
        if (obj instanceof UMLBOTLDestinationDiagram) return;
        if (obj instanceof UMLBOTLObjectDestinationDiagram || obj instanceof UMLBOTLObjectSourceDiagram || obj instanceof UMLBOTLRuleDiagram) {
            UMLDiagram d = (UMLDiagram) obj;
            Enumeration elements = d.elements();
            while (elements.hasMoreElements()) {
                Object element = elements.nextElement();
                if (element instanceof FigBotlElement) {
                    moveToTrash(((Fig) element).getOwner());
                    ((Fig) element).delete();
                }
            }
        }
        if (Trash.SINGLETON.contains(obj)) return;
        trashInternal(obj);
    }

    protected void trashInternal(Object obj) {
        boolean needSave = false;
        if (obj instanceof MBase) {
            ProjectBrowser.TheInstance.getEditorPane().removePresentationFor(obj, getDiagrams());
            ((MBase) obj).remove();
            if (_members.contains(obj)) {
                _members.remove(obj);
            }
            if (_models.contains(obj)) {
                _models.remove(obj);
            }
            needSave = true;
        } else {
            if (obj instanceof ArgoDiagram) {
                removeProjectMemberDiagram((ArgoDiagram) obj);
                needSave = true;
            }
            if (obj instanceof Fig) {
                ((Fig) obj).dispose();
                needSave = true;
            }
        }
        setNeedsSave(needSave);
    }

    public void moveFromTrash(Object obj) {
        System.out.println("needs-more-work: not restoring " + obj);
    }

    public boolean isInTrash(Object dm) {
        return Trash.SINGLETON.contains(dm);
    }

    public static void resetStats() {
        ToDoPane._clicksInToDoPane = 0;
        ToDoPane._dblClicksInToDoPane = 0;
        ToDoList._longestToDoList = 0;
        Designer._longestAdd = 0;
        Designer._longestHot = 0;
        Critic._numCriticsFired = 0;
        ToDoList._numNotValid = 0;
        Agency._numCriticsApplied = 0;
        ToDoPane._toDoPerspectivesChanged = 0;
        NavigatorPane._navPerspectivesChanged = 0;
        NavigatorPane._clicksInNavPane = 0;
        FindDialog._numFinds = 0;
        TabResults._numJumpToRelated = 0;
        DesignIssuesDialog._numDecisionModel = 0;
        GoalsDialog._numGoalsModel = 0;
        CriticBrowserDialog._numCriticBrowser = 0;
        NavigatorConfigDialog._numNavConfig = 0;
        TabToDo._numHushes = 0;
        ChecklistStatus._numChecks = 0;
        SelectionWButtons.Num_Button_Clicks = 0;
        ModeCreateEdgeAndNode.Drags_To_New = 0;
        ModeCreateEdgeAndNode.Drags_To_Existing = 0;
    }

    public static void setStat(String n, int v) {
        System.out.println("setStat: " + n + " = " + v);
        if (n.equals("clicksInToDoPane")) ToDoPane._clicksInToDoPane = v; else if (n.equals("dblClicksInToDoPane")) ToDoPane._dblClicksInToDoPane = v; else if (n.equals("longestToDoList")) ToDoList._longestToDoList = v; else if (n.equals("longestAdd")) Designer._longestAdd = v; else if (n.equals("longestHot")) Designer._longestHot = v; else if (n.equals("numCriticsFired")) Critic._numCriticsFired = v; else if (n.equals("numNotValid")) ToDoList._numNotValid = v; else if (n.equals("numCriticsApplied")) Agency._numCriticsApplied = v; else if (n.equals("toDoPerspectivesChanged")) ToDoPane._toDoPerspectivesChanged = v; else if (n.equals("navPerspectivesChanged")) NavigatorPane._navPerspectivesChanged = v; else if (n.equals("clicksInNavPane")) NavigatorPane._clicksInNavPane = v; else if (n.equals("numFinds")) FindDialog._numFinds = v; else if (n.equals("numJumpToRelated")) TabResults._numJumpToRelated = v; else if (n.equals("numDecisionModel")) DesignIssuesDialog._numDecisionModel = v; else if (n.equals("numGoalsModel")) GoalsDialog._numGoalsModel = v; else if (n.equals("numCriticBrowser")) CriticBrowserDialog._numCriticBrowser = v; else if (n.equals("numNavConfig")) NavigatorConfigDialog._numNavConfig = v; else if (n.equals("numHushes")) TabToDo._numHushes = v; else if (n.equals("numChecks")) ChecklistStatus._numChecks = v; else if (n.equals("Num_Button_Clicks")) SelectionWButtons.Num_Button_Clicks = v; else if (n.equals("Drags_To_New")) ModeCreateEdgeAndNode.Drags_To_New = v; else if (n.equals("Drags_To_Existing")) ModeCreateEdgeAndNode.Drags_To_Existing = v; else {
            System.out.println("unknown UsageStatistic: " + n);
        }
    }

    public static Vector getStats() {
        Vector s = new Vector();
        addStat(s, "clicksInToDoPane", ToDoPane._clicksInToDoPane);
        addStat(s, "dblClicksInToDoPane", ToDoPane._dblClicksInToDoPane);
        addStat(s, "longestToDoList", ToDoList._longestToDoList);
        addStat(s, "longestAdd", Designer._longestAdd);
        addStat(s, "longestHot", Designer._longestHot);
        addStat(s, "numCriticsFired", Critic._numCriticsFired);
        addStat(s, "numNotValid", ToDoList._numNotValid);
        addStat(s, "numCriticsApplied", Agency._numCriticsApplied);
        addStat(s, "toDoPerspectivesChanged", ToDoPane._toDoPerspectivesChanged);
        addStat(s, "navPerspectivesChanged", NavigatorPane._navPerspectivesChanged);
        addStat(s, "clicksInNavPane", NavigatorPane._clicksInNavPane);
        addStat(s, "numFinds", FindDialog._numFinds);
        addStat(s, "numJumpToRelated", TabResults._numJumpToRelated);
        addStat(s, "numDecisionModel", DesignIssuesDialog._numDecisionModel);
        addStat(s, "numGoalsModel", GoalsDialog._numGoalsModel);
        addStat(s, "numCriticBrowser", CriticBrowserDialog._numCriticBrowser);
        addStat(s, "numNavConfig", NavigatorConfigDialog._numNavConfig);
        addStat(s, "numHushes", TabToDo._numHushes);
        addStat(s, "numChecks", ChecklistStatus._numChecks);
        addStat(s, "Num_Button_Clicks", SelectionWButtons.Num_Button_Clicks);
        addStat(s, "Drags_To_New", ModeCreateEdgeAndNode.Drags_To_New);
        addStat(s, "Drags_To_Existing", ModeCreateEdgeAndNode.Drags_To_Existing);
        return s;
    }

    public static void addStat(Vector stats, String name, int value) {
        stats.addElement(new UsageStatistic(name, value));
    }

    public void setDefaultModel(MModel defaultModel) {
        _defaultModel = defaultModel;
    }

    public MModel getDefaultModel() {
        return _defaultModel;
    }

    static final long serialVersionUID = 1399111233978692444L;
}

class ResetStatsLater implements Runnable {

    public void run() {
        Project.resetStats();
    }
}
