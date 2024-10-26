package neissmodel.portlet;

import java.io.BufferedReader;
import java.io.File;
import javax.portlet.GenericPortlet;
import javax.portlet.ActionRequest;
import javax.portlet.RenderRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderResponse;
import javax.portlet.PortletException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.xml.namespace.QName;
import neissmodel.webservice.ModelServiceService;
import neissmodel.xml.NeissMapperXML;
import neissmodel.xml.NeissModelXML;
import org.jdom.JDOMException;

/**
 * MapperPortlet Portlet Class
 */
public class MapperPortlet extends GenericPortlet {

    private static String WSDL_URL = "http://geo-s12.leeds.ac.uk:8080/NeissModel_WS_V2/ModelService?wsdl";

    private static String UserName = "Nick";

    private static final String USER_NAME = "UserName";

    private static final String CREATE_MAP = "createMap";

    private static final String INITIALISE = "initialise";

    private static final String ERROR_MESSAGES = "ErrorMessages";

    private static final String SUCCESS_MESSAGES = "SuccessMessages";

    /** The column that contains the data to be mapped */
    private static String DataValues;

    private static final String DATA_VALUES = "DataValues";

    /** A URL of the csv file that contains the required data */
    private static String CsvURL;

    private static final String CSV_URL = "CsvURL";

    /** String constant for the name of the list of parameters that are passed to the JSP */
    private static final String MAPPING_PARAMS = "MappingParams";

    /** String constant for the name of the variable that holds the final map URL */
    private static final String MAP_URL = "MapURL";

    private static String MapURL = "";

    @Override
    public void processAction(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        PortletSession thePortletSession = request.getPortletSession(true);
        thePortletSession.setAttribute(ERROR_MESSAGES, null);
        thePortletSession.setAttribute(SUCCESS_MESSAGES, null);
        MapURL = "";
        String generateMap = request.getParameter(CREATE_MAP);
        String initialise = request.getParameter(INITIALISE);
        if (generateMap != null) {
            response.setRenderParameter(CREATE_MAP, generateMap);
            DataValues = request.getParameter(DATA_VALUES);
            CsvURL = request.getParameter(CSV_URL);
            if (DataValues == null || DataValues.length() == 0 || CsvURL == null || CsvURL.length() == 0) {
                thePortletSession.setAttribute(ERROR_MESSAGES, "Internal error, can't find data values or " + "CSV url. Data values: '" + (DataValues == null ? "null" : DataValues) + "'. CSV url: '" + (CsvURL == null ? "null" : CsvURL) + "'.");
                return;
            }
            System.out.println("Mapper.ProcessAction() detects that GenerateMap has been pressed. Have " + "the following values:" + "\n\t dataValues " + DataValues + "\n\t csvurl: " + CsvURL);
            neissmodel.webservice.ModelServiceService service = this.getService();
            neissmodel.webservice.ModelService port = service.getModelServicePort();
            MapURL = port.runMapperFromURL(CsvURL, DataValues);
            thePortletSession.setAttribute(SUCCESS_MESSAGES, "Found this url to a map: <a href='" + MapURL + "'>URL</a>");
        } else if (initialise != null) {
            System.out.println("MapperPortlet.ProcessAction() detects that initialise has been pressed");
            UserName = request.getParameter(USER_NAME).trim();
            if (UserName == null || UserName.length() == 0) {
                thePortletSession.setAttribute(ERROR_MESSAGES, "Please enter a username");
                return;
            }
            System.out.println("MapperPortlet.ProcessAction() has username " + UserName);
            neissmodel.webservice.ModelServiceService service = this.getService();
            neissmodel.webservice.ModelService port = service.getModelServicePort();
            String resString = port.getAllResults(UserName);
            if (resString.length() == 0) {
                thePortletSession.setAttribute(ERROR_MESSAGES, "I can't find any results for " + "user '" + UserName + "', are you sure the user has generated mappable data?");
                return;
            }
            NeissModelXML resXML = null;
            try {
                resXML = new NeissModelXML(resString);
            } catch (JDOMException ex) {
                thePortletSession.setAttribute(ERROR_MESSAGES, "Error trying to translate the results " + "into an xml document. The XML string is '" + resString + "'.");
                return;
            }
            StringBuilder ers = new StringBuilder();
            String xmlParameters = MapperCopy.getMapperParameters(resXML, ers);
            if (xmlParameters == null) {
                thePortletSession.setAttribute(ERROR_MESSAGES, "Error trying to parse the results " + "URLs and create mappable parameters from them: " + ers.toString());
                return;
            }
            System.out.println("DEBUG: Have this output from MapperCopy.getMapperParameters(): " + xmlParameters);
            NeissMapperXML xmlDoc = null;
            try {
                xmlDoc = new NeissMapperXML(xmlParameters);
            } catch (JDOMException ex) {
                thePortletSession.setAttribute(ERROR_MESSAGES, "Error trying to translate the results " + "into an xml document. The XML string is '" + resString + "'.");
                return;
            }
            ArrayList<String> params = xmlDoc.getAllDataFileNames();
            ArrayList<Object> mappingParams = new ArrayList<Object>();
            if (params != null) {
                System.out.println("The available mapping parameters (from the webservice) are: ");
                for (String name : params) {
                    ArrayList<Object> l = xmlDoc.getDataFile(name);
                    mappingParams.add(l);
                    System.out.println("\t" + l.toString());
                }
            } else {
                System.out.println("No mappable data exists on the server (no parameters returned)");
            }
            thePortletSession.setAttribute(MAPPING_PARAMS, mappingParams);
        }
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        response.setContentType("text/html");
        PortletSession thePortletSession = request.getPortletSession(true);
        request.setAttribute(DATA_VALUES, DataValues);
        request.setAttribute(CSV_URL, CsvURL);
        request.setAttribute(USER_NAME, UserName);
        request.setAttribute(MAP_URL, MapURL);
        request.setAttribute(ERROR_MESSAGES, thePortletSession.getAttribute(ERROR_MESSAGES));
        request.setAttribute(SUCCESS_MESSAGES, thePortletSession.getAttribute(SUCCESS_MESSAGES));
        request.setAttribute(MAPPING_PARAMS, thePortletSession.getAttribute(MAPPING_PARAMS));
        PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/MapperPortlet_view.jsp");
        dispatcher.include(request, response);
    }

    @Override
    public void doEdit(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        response.setContentType("text/html");
        PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/MapperPortlet_edit.jsp");
        dispatcher.include(request, response);
    }

    @Override
    public void doHelp(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        response.setContentType("text/html");
        PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/jsp/MapperPortlet_help.jsp");
        dispatcher.include(request, response);
    }

    private ModelServiceService getService() throws MalformedURLException {
        ModelServiceService service = null;
        System.out.println("Attempting to connect to web service: " + (WSDL_URL == null ? " default " : WSDL_URL));
        if (WSDL_URL == null) {
            service = new neissmodel.webservice.ModelServiceService();
        } else {
            service = new neissmodel.webservice.ModelServiceService(new URL(WSDL_URL), new QName("http://webservice.neissmodel/", "ModelServiceService"));
        }
        System.out.println("Portlet connected to web service at " + service.getWSDLDocumentLocation());
        return service;
    }
}

/**
 * This class is just used to provide certain function(s) from the neissmodel.Mapper
 * class. I've done that this way (instead of using the Mapper class directly)
 * so that the whole NiessModel project (and all required libraries) don't need
 * to be imported here.
 * @author Nick
 */
abstract class MapperCopy {

    /**
    * NOTE: THIS FUNCTION IS COPIED DIRECTLY FROM neissmodel.Mapper so that the
    * NeissModel project doesn't need to be imported
    *
    * <P>
    * Function that takes an XML document containing results (desciptions and urls to files)
    * and looks at the riles by downloading a few lines to determine if they are mappable.
    * If they are then it saves the adds the parameters (geometry, mappable fields etc) and
    * returns them all in a single <code>NeissMapperXML</code> document.</P>
    *
    * <p>Note that the Mapper class has similar function that are now depricated (worked
    * when all data was guaranteed to be stored on the server hosting the NeissModel web service).
    * Now the data could be stored anywhere.
    * </p>
    * @param resXML The <code>NeissModelXML</code> document that contains results
    * @ param errors Any errors can be added to this optional parameter
    * @return A string representing a <code>NeissMapperXML</code> document. It is possible
    * that this can contain no mapper parameters (if no results were in the input
    * xml document). Null is only returned if there was an error (i.e. a given result url
    * could not be converted into a <code>URL</code> object).
    * @see NeissMapperXML
    * @see NeissModelXML
    * @see Mapper
    */
    public static String getMapperParameters(NeissModelXML resXML, StringBuilder errors) {
        if (errors == null) {
            errors = new StringBuilder();
        }
        String[] theFile = null;
        NeissMapperXML mapXML = new NeissMapperXML();
        Map<String, String> resMap = resXML.getResultsData();
        if (resMap.size() == 0) {
            System.err.println("MapperPortlet.getMapperParameters() warning: there are no results " + "in the XML, this function shoudn't have been called.");
            return mapXML.toString();
        }
        for (String desc : resMap.keySet()) {
            String urlStr = resMap.get(desc);
            try {
                URL url = new URL(urlStr);
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                theFile = MapperCopy.analyseFile(br, null);
                if (theFile == null) {
                    errors.append("Warning. Could not parse the input for " + url.toString() + "\n");
                } else {
                    String filename = theFile[0];
                    String theGeom = theFile[2];
                    String areaCode = theFile[3];
                    ArrayList<String> mappableColumns = new ArrayList<String>();
                    for (int i = 4; i < theFile.length; i++) {
                        mappableColumns.add(theFile[i]);
                    }
                    mapXML.addDataFile(urlStr, filename, theGeom, areaCode, mappableColumns);
                }
            } catch (MalformedURLException ex) {
                errors.append("MalformedURLException trying to create a URL object from the " + "results url '" + urlStr + "' (description: '" + desc + "')");
                return null;
            } catch (IOException ex) {
                errors.append("IOException (" + ex.toString() + ") trying to create a URL object from the " + "results url '" + urlStr + "' (description: '" + desc + "'). ");
                return null;
            }
        }
        return mapXML.toString();
    }

    /**
    * NOTE: THIS FUNCTION IS COPIED DIRECTLY FROM neissmodel.Mapper so that the
    * NeissModel project doesn't need to be imported
    *
    * <P>Analyse a file on the server to get information like geometry type, mappable
    * columns etc.</P>
    * 
    * @param br A buffered reader reading the file.
    * @param f (Optional) the file itself, not required if the BufferedReader is
    * reading a file that isn't locally stored.
    * @return File attributes as a String array:
    * <OL>
    * <LI>File path (if local) or null</LI>
    * <LI>File path on tomcat server (if local) or null</LI>
    * <LI>Geometry type (e.g. 'OA')</LI>
    * <LI>Geometry column</LI>
    * <LI> ... (multiple) mappable columns ... </LI>
    * </OL>
    * Or return null if no geographic data column (i.e. a column with OA codes)
    * could be found.
    * @throws IOException
    */
    private static String[] analyseFile(BufferedReader br, File f) throws IOException {
        String[] line1 = br.readLine().trim().split("\\s*?,\\s*?");
        String[] line2 = br.readLine().trim().split("\\s*?,\\s*?");
        br.close();
        Pattern region = Pattern.compile("\\d\\d[A-Z][A-Z]");
        Pattern ward = Pattern.compile("\\d\\d[A-Z][A-Z][A-Z][A-Z]");
        Pattern oa = Pattern.compile("\\d\\d[A-Z][A-Z][A-Z][A-Z]\\d\\d\\d\\d");
        int geomColumn = -1;
        String geometryType = null;
        List<String> mappableColumns = new ArrayList<String>();
        line: for (int i = 0; i < line1.length; i++) {
            if (region.matcher(line2[i]).matches()) {
                geometryType = "ONSDistricts";
                geomColumn = i;
                break line;
            } else if (ward.matcher(line2[i]).matches()) {
                geometryType = "CASWards";
                geomColumn = i;
                break line;
            } else if (oa.matcher(line2[i]).matches()) {
                geometryType = "OA";
                geomColumn = i;
                break line;
            }
        }
        if (geomColumn == -1) {
            return null;
        } else {
            for (int i = 0; i < line1.length; i++) {
                if (i != geomColumn) {
                    mappableColumns.add(line1[i].trim());
                }
            }
            String[] theFile = new String[4 + mappableColumns.size()];
            theFile[0] = f == null ? null : f.getAbsolutePath();
            theFile[2] = geometryType;
            theFile[3] = line1[geomColumn].trim();
            for (int i = 0; i < mappableColumns.size(); i++) {
                theFile[i + 4] = mappableColumns.get(i);
            }
            return theFile;
        }
    }
}
