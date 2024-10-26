package uk.ac.ebi.intact.plugins.dbtest;

import uk.ac.ebi.intact.plugin.IntactAbstractMojo;
import uk.ac.ebi.intact.plugin.IntactHibernateMojo;
import uk.ac.ebi.intact.plugins.hibernateconfig.HibernateConfigCreatorMojo;
import uk.ac.ebi.intact.plugins.dbtest.xmlimport.Imports;
import uk.ac.ebi.intact.plugins.dbtest.xmlimport.XmlFileset;
import uk.ac.ebi.intact.util.Utilities;
import uk.ac.ebi.intact.util.protein.UpdateProteins;
import uk.ac.ebi.intact.util.protein.UpdateProteinsI;
import uk.ac.ebi.intact.util.protein.BioSourceFactory;
import uk.ac.ebi.intact.business.IntactException;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.checker.ControlledVocabularyRepository;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.checker.EntrySetChecker;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.util.report.MessageHolder;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.parser.EntrySetParser;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.persister.EntrySetPersister;
import uk.ac.ebi.intact.application.dataConversion.psiUpload.model.EntrySetTag;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.net.URL;

/**
 * Import a psi xml 1 into the database
 * @goal import-psi1
 */
public class PsiXml1ImportMojo extends IntactHibernateMojo {

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
     * @required
     */
    private Imports imports;

    protected void executeIntactMojo() throws MojoExecutionException, MojoFailureException, IOException {
        for (XmlFileset fileset : imports.getXmlFilesets()) {
            if (fileset.getVersion() == null) {
                throw new MojoExecutionException("All xmlFilesets must contain a <version> element");
            }
            if (fileset.getVersion().equals("1")) {
                for (String url : fileset.getUrls()) {
                    getLog().debug("To import: " + url);
                    try {
                        importUrl(url);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new MojoExecutionException("Could import file: " + url, e);
                    }
                }
            }
        }
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
