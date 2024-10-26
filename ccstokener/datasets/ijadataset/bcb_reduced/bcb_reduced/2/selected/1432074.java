package uk.ac.ebi.intact.plugins.dbtest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.hibernate.SessionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.checker.ControlledVocabularyRepository;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.checker.EntrySetChecker;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.model.EntrySetTag;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.parser.EntrySetParser;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.persister.EntrySetPersister;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.util.report.MessageHolder;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.context.IntactEnvironment;
import uk.ac.ebi.intact.plugin.IntactHibernateMojo;
import uk.ac.ebi.intact.plugins.dbtest.xmlimport.Imports;
import uk.ac.ebi.intact.plugins.dbtest.xmlimport.XmlFileset;
import uk.ac.ebi.intact.util.protein.BioSourceFactory;
import uk.ac.ebi.intact.util.protein.UpdateProteins;
import uk.ac.ebi.intact.util.protein.UpdateProteinsI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Import a psi xml into the database
 * @goal import-psi
 *
 * @phase generate-test-resources
 */
public class PsiXmlImportMojo extends IntactHibernateMojo {

    private static String PSI1_DIR = "src/test/psi1";

    private static String PSI2_5_DIR = "src/test/psi25";

    private static String VERSION_1 = "1";

    private static String VERSION_2_5 = "2.5";

    /**
     * Project instance
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Files to import
     *
     * @parameter
     */
    private Imports imports;

    protected void initializeHibernate() throws MojoExecutionException {
        System.setProperty(IntactEnvironment.INSTITUTION_LABEL.getFqn(), "myInstitution");
        System.setProperty(IntactEnvironment.AC_PREFIX_PARAM_NAME.getFqn(), "TEST");
        System.setProperty(IntactEnvironment.READ_ONLY_APP.getFqn(), Boolean.FALSE.toString());
        super.initializeHibernate();
    }

    protected void executeIntactMojo() throws MojoExecutionException, MojoFailureException, IOException {
        if (imports == null) {
            imports = defaultImports();
        } else {
            imports.getXmlFilesets().addAll(defaultImports().getXmlFilesets());
        }
        for (XmlFileset fileset : imports.getXmlFilesets()) {
            if (fileset.getVersion() == null) {
                throw new MojoExecutionException("All xmlFilesets must contain a <version> element");
            }
            if (fileset.getVersion().equals(VERSION_1)) {
                for (String url : fileset.getUrls()) {
                    getLog().debug("Importing file: " + url);
                    try {
                        importUrl(url);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new MojoExecutionException("Could'nt import file: " + url, e);
                    }
                }
            } else {
                throw new MojoExecutionException("Import for version " + fileset.getVersion() + " not implemented");
            }
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
        SessionFactory sf = (SessionFactory) IntactContext.getCurrentInstance().getConfig().getDefaultDataConfig().getSessionFactory();
        sf.close();
    }

    private Imports defaultImports() throws IOException {
        Imports imports = new Imports();
        File baseDir;
        if (getProject() == null) {
            baseDir = new File(".");
        } else {
            baseDir = getProject().getBasedir();
        }
        File psi1Dir = new File(baseDir, PSI1_DIR);
        File psi25Dir = new File(baseDir, PSI2_5_DIR);
        if (psi1Dir.exists()) {
            imports.getXmlFilesets().add(filesetFromDir(psi1Dir, VERSION_1));
        }
        if (psi25Dir.exists()) {
            imports.getXmlFilesets().add(filesetFromDir(psi25Dir, VERSION_2_5));
        }
        return imports;
    }

    private XmlFileset filesetFromDir(File dir, String version) throws IOException {
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IOException("File does not exist or it is not a directory: " + dir);
        }
        XmlFileset fileset = new XmlFileset();
        File[] filesInDir = dir.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".xml");
            }
        });
        for (File fileInDir : filesInDir) {
            fileset.getUrls().add(fileInDir.toURL().toString());
        }
        fileset.setVersion(version);
        return fileset;
    }

    private void importUrl(String str) throws Exception {
        URL url = new URL(str);
        InputStream xmlStream = url.openStream();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        MessageHolder messages = MessageHolder.getInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlStream);
        Element rootElement = document.getDocumentElement();
        EntrySetParser entrySetParser = new EntrySetParser();
        EntrySetTag entrySet = entrySetParser.process(rootElement);
        UpdateProteinsI proteinFactory = new UpdateProteins();
        BioSourceFactory bioSourceFactory = new BioSourceFactory();
        ControlledVocabularyRepository.check();
        EntrySetChecker.check(entrySet, proteinFactory, bioSourceFactory);
        if (messages.checkerMessageExists()) {
            MessageHolder.getInstance().printCheckerReport(System.err);
        } else {
            EntrySetPersister.persist(entrySet);
            if (messages.checkerMessageExists()) {
                MessageHolder.getInstance().printPersisterReport(System.err);
            } else {
                System.out.println("The data have been successfully saved in your Intact node.");
            }
        }
    }

    public MavenProject getProject() {
        return project;
    }

    public Imports getImports() {
        return imports;
    }

    public void setImports(Imports imports) {
        this.imports = imports;
    }
}
