package uk.ac.osswatch.simal.wicket.doap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.wicket.PageParameters;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.ajax.markup.html.form.upload.UploadProgressBar;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;
import uk.ac.osswatch.simal.SimalProperties;
import uk.ac.osswatch.simal.SimalRepositoryFactory;
import uk.ac.osswatch.simal.model.IProject;
import uk.ac.osswatch.simal.rdf.SimalException;
import uk.ac.osswatch.simal.rdf.io.RDFXMLUtils;
import uk.ac.osswatch.simal.wicket.BasePage;
import uk.ac.osswatch.simal.wicket.ErrorReportPage;
import uk.ac.osswatch.simal.wicket.UserReportableException;

/**
 * A form for importing and creating DOAP files.
 */
public class DoapFormPage extends BasePage {

    private static final long serialVersionUID = -7082891387390604176L;

    private static final Logger logger = LoggerFactory.getLogger(DoapFormPage.class);

    private static final String FILE_NAME_FORMAT = "%s_%s.xml";

    private static DoapFormInputModel inputModel = new DoapFormInputModel();

    private FeedbackPanel feedback;

    public DoapFormPage() {
        feedback = new FeedbackPanel("feedback");
        add(feedback);
        AddByURLForm<DoapFormInputModel> addByURLForm = new AddByURLForm<DoapFormInputModel>("addByURLForm", new CompoundPropertyModel<DoapFormInputModel>(inputModel));
        TextField<URL> urlField = new TextField<URL>("sourceURL", URL.class);
        addByURLForm.add(urlField);
        String[] defaultValue = { "" };
        urlField.setModelValue(defaultValue);
        add(addByURLForm);
        add(new AddBySourceForgeURLForm<DoapFormInputModel>("addBySourceForgeIdForm", new CompoundPropertyModel<DoapFormInputModel>(inputModel)));
        final FileUploadForm ajaxSimpleUploadForm = new FileUploadForm("uploadForm");
        ajaxSimpleUploadForm.add(new UploadProgressBar("uploadProgress", ajaxSimpleUploadForm));
        add(ajaxSimpleUploadForm);
        add(new AddByRawRDFForm("rawRDFForm"));
    }

    /**
   * Form for a project's DOAP description that can been entered in a text area 
   * and is processed as a project if it's valid RDF/XML containing DOAP.  
   */
    private class AddByRawRDFForm extends Form<DoapFormInputModel> {

        private static final long serialVersionUID = 5436861979864365527L;

        private TextArea<String> rdfField;

        public AddByRawRDFForm(String id) {
            super(id, new CompoundPropertyModel<DoapFormInputModel>(inputModel));
            add(rdfField = new TextArea<String>("rawRDF"));
            String[] defaultValue = { "" };
            rdfField.setModelValue(defaultValue);
        }

        @Override
        protected void onSubmit() {
            super.onSubmit();
            String rdf = StringEscapeUtils.unescapeXml(rdfField.getValue());
            processSubmittedDoap(rdf);
        }
    }

    /**
   * Form for uploading a DOAP description as a file.
   */
    private class FileUploadForm extends Form<FileUploadField> {

        private static final long serialVersionUID = -6275625011225339551L;

        private FileUploadField fileUploadField;

        public FileUploadForm(String name) {
            super(name);
            setMultiPart(true);
            add(fileUploadField = new FileUploadField("fileInput"));
            setMaxSize(Bytes.kilobytes(100));
        }

        /**
     * @see org.apache.wicket.markup.html.form.Form#onSubmit()
     */
        protected void onSubmit() {
            super.onSubmit();
            if (!this.hasError()) {
                final FileUpload upload = fileUploadField.getFileUpload();
                if (upload != null) {
                    try {
                        StringWriter xmlSourceWriter = new StringWriter();
                        IOUtils.copy(upload.getInputStream(), xmlSourceWriter);
                        processSubmittedDoap(xmlSourceWriter.toString());
                    } catch (IOException e) {
                        setResponsePage(new ErrorReportPage(new UserReportableException("Unable to add doap using RDF supplied", DoapFormPage.class, e)));
                    }
                }
            }
        }
    }

    /**
   * For for submitting a project to Simal by specifying a URL to 
   * a DOAP specification.  
   * @param <T>
   */
    private class AddByURLForm<T extends DoapFormInputModel> extends Form<DoapFormInputModel> {

        private static final long serialVersionUID = 4350446873545711199L;

        public AddByURLForm(String name, IModel<DoapFormInputModel> model) {
            super(name, model);
        }

        protected void processAddByURLSubmit(URL url, String invalidUrlMsg) {
            if (!this.hasError()) {
                try {
                    StringWriter xmlSourceWriter = new StringWriter();
                    IOUtils.copy(url.openStream(), xmlSourceWriter);
                    processSubmittedDoap(xmlSourceWriter.toString());
                } catch (FileNotFoundException e) {
                    Session.get().error(invalidUrlMsg);
                    logger.warn("Error processing URL: " + invalidUrlMsg);
                } catch (IOException e) {
                    setResponsePage(new ErrorReportPage(new UserReportableException("Unable to add doap using RDF supplied", DoapFormPage.class, e)));
                    logger.warn("Error processing URL: " + url + "; " + e.getMessage(), e);
                }
            }
        }

        @Override
        protected void onSubmit() {
            String invalidUrlMsg = "Invalid URL : " + inputModel.getSourceURL();
            processAddByURLSubmit(inputModel.getSourceURL(), invalidUrlMsg);
        }
    }

    /**
   * Add a project to Simal based on the Unix name of the project. The form
   * will fetch the DOAP from SourceForge based on the name entered by the user. 
   * @param <T>
   */
    private class AddBySourceForgeURLForm<T extends DoapFormInputModel> extends AddByURLForm<DoapFormInputModel> {

        private static final long serialVersionUID = 5717813288953568375L;

        private static final String SF_URL_FORMAT = "http://sourceforge.net/api/project/name/%s/doap";

        /**
     * @param name
     */
        public AddBySourceForgeURLForm(String name, IModel<DoapFormInputModel> model) {
            super(name, model);
            RequiredTextField<String> stringTextField = new RequiredTextField<String>("sourceForgeId");
            stringTextField.setLabel(new Model<String>());
            add(stringTextField);
        }

        @Override
        protected void onSubmit() {
            try {
                URL url = new URL(String.format(SF_URL_FORMAT, inputModel.getSourceForgeId()));
                String invalidUrlMsg = "Could not get DOAP from SourceForge for the project named " + inputModel.getSourceForgeId();
                processAddByURLSubmit(url, invalidUrlMsg);
            } catch (MalformedURLException e) {
                logger.error("Unexpected malformed URL: " + e.getMessage(), e);
                setResponsePage(new ErrorReportPage(new UserReportableException("Unable to add doap using RDF supplied", DoapFormPage.class, e)));
            }
        }
    }

    /**
   * Generic method of processing a DOAP specification as a String. This 
   * will store a backup of the submitted DOAP and create a new project.
   * 
   * @param doap
   * @throws SimalException
   */
    private void processSubmittedDoap(String doap) {
        try {
            backupDoapFile(doap);
            IProject newProject = SimalRepositoryFactory.getProjectService().createProject(RDFXMLUtils.convertXmlStringToDom(doap));
            if (newProject != null) {
                PageParameters newProjectPageParams = new PageParameters();
                newProjectPageParams.add("simalID", newProject.getSimalID());
                setResponsePage(ProjectDetailPage.class, newProjectPageParams);
            } else {
                throw new SimalException("Failed to created new project based on submitted DOAP.");
            }
        } catch (SimalException e) {
            if (e.getCause().getClass() == IOException.class || e.getCause().getClass() == SAXParseException.class) {
                logger.warn("No valid doap found, msg: " + e.getMessage() + "; doap: " + doap, e);
                Session.get().error("No valid DOAP file found at the provided URL.");
            } else {
                logger.warn("Unable to create a project from the following doap: " + doap);
                setResponsePage(new ErrorReportPage(new UserReportableException("Unable to add doap using RDF supplied", DoapFormPage.class, e)));
            }
        }
    }

    /**
   * Backup the doap string on the local file system.
   * @param doap
   * @throws SimalException If the backup failed.
   */
    private void backupDoapFile(String doap) throws SimalException {
        String backupPath = SimalProperties.getProperty(SimalProperties.PROPERTY_RDF_BACKUP_DIR);
        String backupFileName = generateFileName(doap);
        File backupFile = new File(backupPath, backupFileName);
        try {
            FileUtils.writeStringToFile(backupFile, doap, "UTF-8");
            logger.debug("Written file to " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            String msg = "Could not write file to " + backupFileName + "; " + e.getMessage();
            logger.warn(msg);
            throw new SimalException(msg, e);
        }
    }

    /**
   * Generate a unique file name based on timestamp information and
   * information in the xml. 
   * @param doapXml
   * @return
   */
    private String generateFileName(String doapXml) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        List<String> projectTags = new ArrayList<String>();
        projectTags.add("shortname");
        projectTags.add("doap:shortname");
        projectTags.add("name");
        projectTags.add("doap:name");
        String projectName = findXmlContentByTagName(doapXml, projectTags);
        return String.format(FILE_NAME_FORMAT, timestamp, projectName);
    }

    /**
   * Iterates of the list tagNames for a tag that is present in the xml. 
   * If a match is found the content of this tag will be returned, 
   * otherwise null will be returned.
   * @param xml
   * @param tagNames
   * @return
   */
    private static String findXmlContentByTagName(String xml, List<String> tagNames) {
        String xmlContent = null;
        for (String tagName : tagNames) {
            String startName = "<" + tagName + ">";
            String endName = "</" + tagName + ">";
            if (xml != null) {
                int startNameIndex = xml.indexOf(startName);
                int endNameIndex = xml.indexOf(endName);
                if (startNameIndex != -1 && endNameIndex != -1) {
                    xmlContent = xml.substring(startNameIndex + startName.length(), endNameIndex);
                    xmlContent = xmlContent.replaceAll(" ", "");
                    break;
                }
            }
        }
        return xmlContent;
    }
}
