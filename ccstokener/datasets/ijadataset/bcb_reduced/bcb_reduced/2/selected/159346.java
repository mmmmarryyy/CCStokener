package uk.ac.osswatch.simal.importData;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import uk.ac.osswatch.simal.SimalRepositoryFactory;
import uk.ac.osswatch.simal.model.Foaf;
import uk.ac.osswatch.simal.rdf.Doap;
import uk.ac.osswatch.simal.rdf.DuplicateURIException;
import uk.ac.osswatch.simal.rdf.SimalException;
import uk.ac.osswatch.simal.rdf.SimalRepositoryException;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class Pims {

    private static final String VALID_PROJECT_CONTACTS_FILE_ID = "Fullname";

    private static final String VALID_PROJECTS_FILE_ID = "projects_name";

    private static final String VALID_PROGRAMMES_FILE_ID = "programmes_name";

    private static final Logger logger = LoggerFactory.getLogger(Pims.class);

    public static final String PIMS_PROJECT_URI = "http://www.jisc.ac.uk/project/pims";

    private static final int MAX_IMPORT_ERRORS = 10;

    private static final String NEW_LINE = System.getProperty("line.separator");

    private Pims() {
    }

    /**
	 * Import institutions from an export PIMS spreadsheet.
	 * 
	 * @param url
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DuplicateURIException
	 * @throws SimalException 
	 */
    public static void importInstitutions(URL url) throws FileNotFoundException, IOException, DuplicateURIException, SimalException {
        HSSFWorkbook wb = new HSSFWorkbook(url.openStream());
        HSSFSheet sheet = wb.getSheetAt(0);
        HSSFRow row = sheet.getRow(0);
        String title = getNullSafeStringValue(row, 1);
        if (!title.equals("name")) {
            throw new SimalException(url + " is not a valid PIMS project export file");
        }
        int lastRow = sheet.getLastRowNum();
        for (int i = 1; i <= lastRow; i++) {
            Document doc;
            Element foaf;
            try {
                doc = createRdfDocument();
                foaf = doc.createElementNS(Foaf.getURI(), "Organization");
            } catch (ParserConfigurationException e1) {
                throw new SimalException("Unable to create XML document for import");
            }
            row = sheet.getRow(i);
            int id = getNullSafeIntValue(row, 0);
            foaf.setAttributeNS(RDF.getURI(), "about", getOrganisationURI(id));
            String value = getNullSafeStringValue(row, 1);
            Element elem = doc.createElementNS(Foaf.getURI(), "name");
            elem.setTextContent(value);
            foaf.appendChild(elem);
            int projectId = getNullSafeIntValue(row, 2);
            elem = doc.createElementNS(Foaf.getURI(), "currentProject");
            elem.setAttributeNS(RDF.getURI(), "resource", getProjectURI(projectId));
            foaf.appendChild(elem);
            doc.getDocumentElement().appendChild(foaf);
            serialise(doc);
            SimalRepositoryFactory.getInstance().addRDFXML(doc);
        }
    }

    private static String getOrganisationURI(int institutionId) {
        return "http://jisc.ac.uk/institution#" + institutionId;
    }

    /**
	 * Import projects from an exported PIMS spreadheet.
	 * 
	 * @param url
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DuplicateURIException
	 * @throws SimalException 
	 */
    public static void importProjects(URL url) throws FileNotFoundException, IOException, DuplicateURIException, SimalException {
        HSSFWorkbook wb = new HSSFWorkbook(url.openStream());
        HSSFSheet sheet = wb.getSheetAt(0);
        HSSFRow row = sheet.getRow(0);
        String title = getNullSafeStringValue(row, 2);
        if (!title.equals(VALID_PROJECTS_FILE_ID)) {
            throw new SimalException(url + " is not a valid PIMS project export file");
        }
        int errorsOccurred = 0;
        StringBuffer errorReports = new StringBuffer();
        int lastRow = sheet.getLastRowNum();
        for (int i = 1; i <= lastRow; i++) {
            Document doc;
            Element doap;
            try {
                doc = createRdfDocument();
                doap = doc.createElementNS(Doap.getURI(), "Project");
            } catch (ParserConfigurationException e1) {
                throw new SimalException("Unable to create XML document for import");
            }
            row = sheet.getRow(i);
            int id = getNullSafeIntValue(row, 0);
            doap.setAttributeNS(RDF.getURI(), "about", getProjectURI(id));
            String name = getNullSafeStringValue(row, 2);
            Element elem = doc.createElementNS(Doap.getURI(), "name");
            elem.setTextContent(name);
            doap.appendChild(elem);
            String value = getNullSafeStringValue(row, 4);
            elem = doc.createElementNS(Doap.getURI(), "description");
            elem.setTextContent(value);
            doap.appendChild(elem);
            value = getNullSafeStringValue(row, 6);
            if (value.length() != 0 && !value.equals("tbc")) {
                elem = doc.createElementNS(Doap.getURI(), "homepage");
                elem.setAttributeNS(RDF.getURI(), "resource", value);
                elem.setAttributeNS(RDFS.getURI(), "label", "Homepage");
                doap.appendChild(elem);
            }
            value = getCategoryURI(getNullSafeIntValue(row, 1));
            elem = doc.createElementNS(Doap.getURI(), "category");
            elem.setAttributeNS(RDF.getURI(), "resource", value);
            doap.appendChild(elem);
            doc.getDocumentElement().appendChild(doap);
            try {
                SimalRepositoryFactory.getProjectService().createProject(doc);
            } catch (SimalRepositoryException e) {
                errorReports.append("Error when importing project named '" + name + "': ");
                errorReports.append(e.getMessage());
                errorReports.append(NEW_LINE);
                errorsOccurred++;
                if (errorsOccurred > MAX_IMPORT_ERRORS) {
                    throw new SimalException("Too many errors (" + MAX_IMPORT_ERRORS + ")" + NEW_LINE + errorReports.toString());
                }
            }
        }
        if (errorsOccurred > 0) {
            throw new SimalException("Import resulted in " + errorsOccurred + " errors:" + NEW_LINE + errorReports.toString());
        }
    }

    /**
	 * Serialise an XML document, used for debugging.
	 * 
	 * @param domImpl
	 * @param document
	 */
    private static void serialise(Document document) {
        DOMImplementationLS ls = (DOMImplementationLS) document.getImplementation();
        LSSerializer lss = ls.createLSSerializer();
        LSOutput lso = ls.createLSOutput();
        lso.setByteStream(System.out);
        lss.write(document, lso);
    }

    private static Document createRdfDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        DOMImplementation domImpl = db.getDOMImplementation();
        Document document = domImpl.createDocument(RDF.getURI(), "RDF", null);
        return document;
    }

    /**
	 * Import programmes from an exported PIMS spreadsheet. Themes are known as categories in 
	 * the Simal application
	 * 
	 * @param url
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DuplicateURIException
	 * @throws SimalException 
	 */
    public static void importProgrammes(URL url) throws FileNotFoundException, IOException, DuplicateURIException, SimalException {
        HSSFWorkbook wb = new HSSFWorkbook(url.openStream());
        HSSFSheet sheet = wb.getSheetAt(0);
        HSSFRow row = sheet.getRow(0);
        String title = getNullSafeStringValue(row, 1);
        if (!title.equals(VALID_PROGRAMMES_FILE_ID)) {
            throw new SimalException(url + " is not a valid PIMS programme export file");
        }
        int lastRow = sheet.getLastRowNum();
        for (int i = 1; i <= lastRow; i++) {
            Document doc;
            Element cat;
            try {
                doc = createRdfDocument();
                cat = doc.createElementNS(Doap.getURI(), "category");
            } catch (ParserConfigurationException e1) {
                throw new SimalException("Unable to create XML document for import");
            }
            row = sheet.getRow(i);
            int id = getNullSafeIntValue(row, 0);
            cat.setAttributeNS(RDF.getURI(), "about", getCategoryURI(id));
            String value = getNullSafeStringValue(row, 1);
            cat.setAttributeNS(RDFS.getURI(), "label", value);
            doc.getDocumentElement().appendChild(cat);
            serialise(doc);
            SimalRepositoryFactory.getInstance().addRDFXML(doc);
        }
    }

    /**
	 * Import contacts relating to each project as exported by PIMS.
	 * 
	 * @param url
	 * @throws IOException 
	 * @throws SimalException 
	 */
    public static void importProjectContacts(URL url) throws IOException, SimalException {
        HSSFWorkbook wb = new HSSFWorkbook(url.openStream());
        HSSFSheet sheet = wb.getSheetAt(0);
        HSSFRow row = sheet.getRow(0);
        String title = getNullSafeStringValue(row, 2);
        if (!title.equals(VALID_PROJECT_CONTACTS_FILE_ID)) {
            throw new SimalException(url + " is not a valid PIMS project contact export file");
        }
        StringBuffer errorReports = new StringBuffer();
        int errorsOccurred = 0;
        int lastRow = sheet.getLastRowNum();
        for (int i = 1; i <= lastRow; i++) {
            Document doc;
            Element project;
            try {
                doc = createRdfDocument();
                project = doc.createElementNS(Doap.getURI(), "Project");
            } catch (ParserConfigurationException e1) {
                throw new SimalException("Unable to create XML document for import");
            }
            row = sheet.getRow(i);
            int projectId = getNullSafeIntValue(row, 1);
            project.setAttributeNS(RDF.getURI(), "about", getProjectURI(projectId));
            Element person = doc.createElementNS(Foaf.getURI(), "Person");
            int id = getNullSafeIntValue(row, 0);
            person.setAttributeNS(RDF.getURI(), "about", getPersonURI(id));
            String name = getNullSafeStringValue(row, 2);
            Element elem = doc.createElementNS(Foaf.getURI(), "name");
            elem.setTextContent(name);
            person.appendChild(elem);
            String email = getNullSafeStringValue(row, 6);
            if (email.contains("@")) {
                if (!email.startsWith("mailto:")) {
                    email = "mailto:" + email;
                }
                elem = doc.createElementNS(Foaf.getURI(), "mbox");
                elem.setAttributeNS(RDF.getURI(), "resource", email);
                person.appendChild(elem);
            } else {
                logger.info("Contact in PIMS import has a strange looking email: " + email);
            }
            String role = getNullSafeStringValue(row, 3);
            if (role.equals("Programme Stream Manager") || role.equals("Programme Strand Manager") || role.equals("Programme Manager")) {
                elem = doc.createElementNS(Doap.getURI(), "helper");
                elem.appendChild(person);
            } else if (role.equals("Project Director") || role.equals("Project Manager")) {
                elem = doc.createElementNS(Doap.getURI(), "maintainer");
                elem.appendChild(person);
            } else if (role.equals("Project Team Member")) {
                elem = doc.createElementNS(Doap.getURI(), "developer");
                elem.appendChild(person);
            } else {
                logger.warn("Got a person (" + name + ") with an unkown role, adding as helper: " + role);
            }
            project.appendChild(elem);
            doc.getDocumentElement().appendChild(project);
            serialise(doc);
            try {
                SimalRepositoryFactory.getProjectService().createProject(doc);
            } catch (SimalRepositoryException e) {
                errorReports.append("Error when importing person named '" + name + "': ");
                errorReports.append(e.getMessage());
                errorReports.append(NEW_LINE);
                errorsOccurred++;
                if (errorsOccurred > MAX_IMPORT_ERRORS) {
                    throw new SimalException("Too many errors (" + MAX_IMPORT_ERRORS + ")" + NEW_LINE + errorReports.toString());
                }
            }
        }
        if (errorsOccurred > 0) {
            throw new SimalException("Import resulted in " + errorsOccurred + " errors:" + NEW_LINE + errorReports.toString());
        }
    }

    /**
         * Get the int value of the specified cell index in the row
         * @param row
         * @param cellIndex
         * @return int value of the specified cell, will be 0 in case of a null cell
         */
    private static int getNullSafeIntValue(HSSFRow row, int cellIndex) {
        return ((Double) row.getCell(cellIndex, HSSFRow.CREATE_NULL_AS_BLANK).getNumericCellValue()).intValue();
    }

    /**
         * Get a String value from the specified cell index in the row.
         * @param row
         * @param cellIndex
         * @return String value of the specified cell, possibly empty in case of null cell
         */
    private static String getNullSafeStringValue(HSSFRow row, int cellIndex) {
        return row.getCell(cellIndex, HSSFRow.CREATE_NULL_AS_BLANK).getRichStringCellValue().getString();
    }

    /**
	 * Get a URI for the programme ID provided.
	 * @param id
	 * @return
	 */
    private static String getCategoryURI(int id) {
        return "http://jisc.ac.uk/programme#" + id;
    }

    /**
	 * Get a URI for the person ID provided.
	 * @param id
	 * @return
	 */
    private static String getPersonURI(int id) {
        logger.debug("Creating a person with id; " + id);
        return "http://jisc.ac.uk/person#" + id;
    }

    /**
	 * Get a URI for the project ID provided.
	 * @param id
	 * @return
	 */
    private static String getProjectURI(int id) {
        return "http://jisc.ac.uk/project#" + id;
    }
}
