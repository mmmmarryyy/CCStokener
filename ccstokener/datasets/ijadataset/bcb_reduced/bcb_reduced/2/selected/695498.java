package org.nees.central;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.nees.data.Central;
import org.nees.data.DataFile;
import org.nees.data.Experiment;
import org.nees.data.Project;
import org.nees.data.Repetition;
import org.nees.data.Trial;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * A client for the NEEScentral web services.
 * 
 * @author Jason P. Hanley
 */
public class CentralClient {

    /** the XML namespace for the NEEScentral schema */
    private static final String namespace = "http://central.nees.org/api";

    /** the package where the domain classes are */
    private static final String domainObjectsPackage = "org.nees.data";

    /** the base URL for the web services */
    private String baseURL = "https://central.nees.org";

    /** the GridAuth session */
    private final String gaSession;

    /** the XML unmarshaller */
    private Unmarshaller unmarshaller;

    /** the XML reader */
    private XMLReader xmlReader;

    /**
   * Creates a client for NEEScentral.
   */
    public CentralClient() throws CentralException {
        this(null);
    }

    /**
   * Creates a client for NEEScentral with the GridAuth session.
   * 
   * @param gaSession          the GridAuth session
   * @throws CentralException  if there is an error creating the client
   */
    public CentralClient(String gaSession) throws CentralException {
        super();
        this.gaSession = gaSession;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(domainObjectsPackage);
            unmarshaller = jaxbContext.createUnmarshaller();
            UnmarshallerHandler unmarshallerHandler = unmarshaller.getUnmarshallerHandler();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);
            SAXParser parser = factory.newSAXParser();
            XMLReader rawXmlReader = parser.getXMLReader();
            xmlReader = new RemoveNamespaceFilter(rawXmlReader, namespace);
            xmlReader.setContentHandler(unmarshallerHandler);
        } catch (Exception e) {
            throw new CentralException(e);
        }
    }

    /**
   * Sets the hostname to use when calling the web services.
   * 
   * @param hostname  the hostname for the NEEScentral instance
   */
    public void setHostname(String hostname) {
        baseURL = "https://" + hostname;
    }

    /**
   * Calls a REST web service and return the response as a Central domain
   * object.
   * 
   * @param path               the path for the web service
   * @return                   the response
   * @throws CentralException  if there is an error calling the web service
   */
    private synchronized Central callREST(String path) throws CentralException {
        Central central = null;
        String urlString = baseURL + "/REST/" + path;
        if (gaSession != null) {
            urlString += "?GAsession=" + gaSession;
        }
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
        }
        SAXSource source;
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            if (connection.getResponseCode() == 401) {
                throw new CentralException("You are not authorized to view this resource");
            }
            source = new SAXSource(xmlReader, new InputSource(connection.getInputStream()));
        } catch (IOException e) {
            throw new CentralException("Error connecting to NEEScentral", e);
        }
        JAXBElement centralElement;
        try {
            centralElement = (JAXBElement) unmarshaller.unmarshal(source);
        } catch (JAXBException e) {
            throw new CentralException("NEEScentral returned an invalid response", e);
        }
        central = (Central) centralElement.getValue();
        return central;
    }

    /**
   * Gets all the projects the user has access to in NEEScentral.
   * 
   * @return                   the projects in NEEScentral
   * @throws CentralException  if there is an error getting the list of projects
   */
    public List<Project> getProjects() throws CentralException {
        Central central = callREST("Project");
        List<Project> projects = central.getProject();
        for (Project project : projects) {
            project.setName("NEES-" + project.getId());
        }
        return projects;
    }

    /**
   * Gets the project from NEEScentral.
   * 
   * @param projectId          the id of the project
   * @return                   the project, or null if it was not found
   * @throws CentralException  if there is an error getting the project
   */
    public Project getProject(int projectId) throws CentralException {
        Central central = callREST("Project/" + projectId);
        List<Project> projects = central.getProject();
        Project project;
        if (projects.size() == 1) {
            project = projects.get(0);
            project.setId(projectId);
            List<Experiment> experiments = project.getExperiment();
            for (int i = 0; i < experiments.size(); i++) {
                Experiment experiment = experiments.get(i);
                int experimentId = getIdFromLink(experiment.getLink());
                if (experimentId != -1) {
                    experiment.setId(experimentId);
                }
                experiment.setName("Experiment-" + (i + 1));
            }
            fixDataFiles(project.getDataFile(), "NEES-");
        } else {
            project = null;
        }
        return project;
    }

    /**
   * Gets the list of experiments for the project.
   * 
   * @param projectId          the project id
   * @return                   a list of experiments for the project
   * @throws CentralException  if there is an error getting the list of
   *                           experiments
   */
    public List<Experiment> getExperiments(int projectId) throws CentralException {
        Central central = callREST("Project/" + projectId + "/Experiment");
        return central.getExperiment();
    }

    /**
   * Gets the experiment from NEEScentral.
   * 
   * @param projectId          the project id for the experiment
   * @param experimentId       the experiment id
   * @return                   the experiment, or null if it is not found
   * @throws CentralException  if there is an error getting the experiment
   */
    public Experiment getExperiment(int projectId, int experimentId) throws CentralException {
        Central central = callREST("Project/" + projectId + "/Experiment/" + experimentId);
        List<Experiment> experiments = central.getExperiment();
        Experiment experiment;
        if (experiments.size() == 1) {
            experiment = experiments.get(0);
            experiment.setId(experimentId);
            List<Trial> trials = experiment.getTrial();
            for (int i = 0; i < trials.size(); i++) {
                Trial trial = trials.get(i);
                int trialId = getIdFromLink(trial.getLink());
                if (trialId != -1) {
                    trial.setId(trialId);
                }
                trial.setName(new JAXBElement<String>(new QName("uri", "local"), String.class, "Trial-" + (i + 1)));
            }
            fixDataFiles(experiment.getDataFile(), new String[] { "Experiment-", "Simulation-" });
        } else {
            experiment = null;
        }
        return experiment;
    }

    /**
   * Gets the list of trials for the experiment.
   * 
   * @param projectId          the project id for the trials
   * @param experimentId       the experiment id for the trials
   * @return                   a list of trials for the experiment
   * @throws CentralException  if there is an error getting the list of trials
   */
    public List<Trial> getTrials(int projectId, int experimentId) throws CentralException {
        Central central = callREST("Project/" + projectId + "/Experiment/" + experimentId + "/Trial");
        return central.getTrial();
    }

    /**
   * Gets the trial from NEEScentral.
   * 
   * @param projectId          the project id for the trial
   * @param experimentId       the experiment id for the trial
   * @param trialId            the trial id
   * @return                   the trial, or null if it is not found
   * @throws CentralException  if there is an error getting the trial
   */
    public Trial getTrial(int projectId, int experimentId, int trialId) throws CentralException {
        Central central = callREST("Project/" + projectId + "/Experiment/" + experimentId + "/Trial/" + trialId);
        List<Trial> trials = central.getTrial();
        Trial trial;
        if (trials.size() == 1) {
            trial = trials.get(0);
            trial.setId(trialId);
            List<Repetition> repetitions = trial.getRepetition();
            for (int i = 0; i < repetitions.size(); i++) {
                Repetition repetition = repetitions.get(i);
                int repetitionId = getIdFromLink(repetition.getLink());
                if (repetitionId != -1) {
                    repetition.setId(repetitionId);
                }
                repetition.setName(new JAXBElement<String>(new QName("uri", "local"), String.class, "Rep-" + (i + 1)));
            }
            fixDataFiles(trial.getDataFile(), new String[] { "Trial-", "Run-" });
        } else {
            trial = null;
        }
        return trial;
    }

    /**
   * Gets a list of repetitions for the trial.
   * 
   * @param projectId          the project id for the repetitions
   * @param experimentId       the experiment id for the repetitions
   * @param trialId            the trial id for the repetitions
   * @return                   a list of repetitions for the trial
   * @throws CentralException  if there is an error getting the list of
   *                           repetitions
   */
    public List<Repetition> getRepetitions(int projectId, int experimentId, int trialId) throws CentralException {
        Central central = callREST("Project/" + projectId + "/Experiment/" + experimentId + "/Trial/" + trialId + "/Repetition");
        return central.getRepetition();
    }

    /**
   * Gets the repetition from NEEScentral.
   * 
   * @param projectId          the project id for the repetition
   * @param experimentId       the experiment id for the repetition
   * @param trialId            the trial id for the repetition
   * @param repetitionId       the repetition id
   * @return                   the repetition, or null if it is not found
   * @throws CentralException  if there is an error getting the repetition
   */
    public Repetition getRepetition(int projectId, int experimentId, int trialId, int repetitionId) throws CentralException {
        Central central = callREST("Project/" + projectId + "/Experiment/" + experimentId + "/Trial/" + trialId + "/Repetition/" + repetitionId);
        List<Repetition> repetitions = central.getRepetition();
        Repetition repetition;
        if (repetitions.size() == 1) {
            repetition = repetitions.get(0);
            repetition.setId(repetitionId);
            fixDataFiles(repetition.getDataFile(), "Rep-");
        } else {
            repetition = null;
        }
        return repetition;
    }

    /**
   * Gets the data file for the path.
   * 
   * @param path               the path of the data file
   * @return                   the data file, or null if it is not found
   * @throws CentralException  if there is an error getting the data file
   */
    public DataFile getDataFile(String path) throws CentralException {
        path = stripDataFilePath(path);
        Central central = callREST("File/" + path);
        List<DataFile> dataFiles = central.getDataFile();
        DataFile dataFile;
        if (dataFiles.size() == 1) {
            dataFile = dataFiles.get(0);
            if (!dataFile.isIsDirectory() && dataFile.getDataFile().size() > 0) {
                dataFile.setIsDirectory(true);
            }
            if (dataFile.getContentLink().startsWith("/File")) {
                dataFile.setContentLink("/REST" + dataFile.getContentLink());
            }
            fixDataFiles(dataFile.getDataFile());
        } else {
            dataFile = null;
        }
        return dataFile;
    }

    /**
   * Get the URL for a data file.
   * 
   * @param dataFile  the data file
   * @return          the URL for the data file
   */
    public URL getDataFileURL(DataFile dataFile) {
        String urlString = baseURL + dataFile.getContentLink();
        if (gaSession != null) {
            urlString += "?GAsession=" + gaSession;
        }
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
        }
        return url;
    }

    /**
   * Returns a relative path based on the given path. This removes various
   * roots from the path that may make it an absolute path. 
   *  
   * @param path  the path for the data file
   * @return      the relative path for the data file
   */
    private static String stripDataFilePath(String path) {
        if (path.startsWith("/REST/File/")) {
            path = path.substring(11);
        } else if (path.startsWith("/File/")) {
            path = path.substring(6);
        } else if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    /**
   * "Fixes" the data files by populating various fields based on educated
   * guesses. This is also removes any duplicate data file from the list.
   * 
   * @param dataFiles  the data file to fix
   */
    private static void fixDataFiles(List<DataFile> dataFiles) {
        fixDataFiles(dataFiles, (String[]) null);
    }

    /**
   * "Fixes" the data files by populating various fields based on educated
   * guesses. This is also removes any duplicate data file from the list.
   * 
   * Any data file who's name begins with the start filter will be removed from
   * the list. 
   * 
   * @param dataFiles    the data file to fix
   * @param startFilter  a filter for the data files
   */
    private static void fixDataFiles(List<DataFile> dataFiles, String startFilter) {
        String[] startFilters = new String[] { startFilter };
        fixDataFiles(dataFiles, startFilters);
    }

    /**
   * "Fixes" the data files by populating various fields based on educated
   * guesses. This is also removes any duplicate data file from the list.
   * 
   * Any data file who's name begins with the start filter will be removed from
   * the list. 
   * 
   * @param dataFiles     the data file to fix
   * @param startFilters  an array of filters for the data files
   */
    private static void fixDataFiles(List<DataFile> dataFiles, String[] startFilters) {
        for (DataFile dataFile : dataFiles) {
            String link = dataFile.getLink();
            if (!link.startsWith("/REST")) {
                link = "/REST" + link;
                dataFile.setLink(link);
            }
            dataFile.setContentLink(link + "/content");
            String fullPath = link.substring(11);
            int lastSlashIndex = fullPath.lastIndexOf('/');
            String name, path;
            if (lastSlashIndex != -1) {
                name = fullPath.substring(lastSlashIndex + 1);
                path = fullPath.substring(0, lastSlashIndex);
            } else {
                name = fullPath;
                path = ".";
            }
            dataFile.setName(name);
            dataFile.setPath(path);
            if (!name.contains(".")) {
                dataFile.setIsDirectory(true);
            }
        }
        for (int i = dataFiles.size() - 1; i >= 0; i--) {
            DataFile dataFile = dataFiles.get(i);
            if (hasDuplicate(dataFiles, dataFile)) {
                dataFiles.remove(i);
            } else if (startFilters != null) {
                for (String startFilter : startFilters) {
                    if (dataFile.getName().startsWith(startFilter)) {
                        dataFiles.remove(i);
                        break;
                    }
                }
            }
        }
    }

    /**
   * Parses the id from the end of a link. The link will be in the format: 
   * 
   * /REST/LEVEL1/ID/LEVEL2/ID
   * 
   * where the id for the lowest level (closest to the end) will be returned.
   * There can be any number of levels.
   * 
   * @param link  the link to parse
   * @return      the id embedded in the link, or -1 if one wasn't found
   */
    private static int getIdFromLink(String link) {
        int slashIndex = link.lastIndexOf('/');
        if (slashIndex == -1) {
            return -1;
        }
        if (slashIndex + 1 == link.length()) {
            return getIdFromLink(link.substring(0, link.length() - 1));
        }
        String idString = link.substring(slashIndex + 1);
        int id;
        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            id = -1;
        }
        return id;
    }

    /**
   * Sees if the given data file has more than one occurance in the list of data
   * files.
   * 
   * @param dataFiles  a list of data files
   * @param dataFile   the data file to check for duplicates
   * @return           true if the given data file occurs more than once in the
   *                   list of data files, otherwise false
   */
    private static boolean hasDuplicate(List<DataFile> dataFiles, DataFile dataFile) {
        int count = 0;
        for (DataFile df : dataFiles) {
            if (dataFile.getPath().equals(df.getPath()) && dataFile.getName().equals(df.getName())) {
                count++;
                if (count > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
   *  A filter to remove a namespace from XML elements. 
   */
    class RemoveNamespaceFilter extends XMLFilterImpl {

        /** the namespace to filter */
        private final String namespace;

        /**
     * Creates the namespace filter.
     * 
     * @param xmlReader  the XML reader
     * @param namespace  the namespace to filter
     */
        public RemoveNamespaceFilter(XMLReader xmlReader, String namespace) {
            super(xmlReader);
            this.namespace = namespace;
        }

        /**
     * This will set the namespace of the element to an empty string if it is
     * null or equals the filtered namespace.
     */
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (uri == null || uri.equals(namespace)) {
                uri = "";
            }
            super.startElement(uri, localName, qName, attributes);
        }
    }
}
