package neissmodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import neissmodel.xml.NeissMapperXML;
import neissmodel.xml.NeissModelXML;
import sun.util.logging.resources.logging_it;

/**
 * Simple class that can be used to generate a URL that will create a map from
 * some aggregated data (generated using, for example, the BHPSLinker).
 *
 * @author Nick Malleson
 * @see BHPSLinker
 */
public class Mapper extends NeissModel {

    /** The tomcat install directory (will contain aggregated data) */
    private static String TOMCAT_DIR;

    /** The URL that published csv data can be found at */
    private static String TOMCAT_URL;

    /** The geometry to map, e.g. 'OA' */
    private NeissModelParameter<String> geometry;

    /** The data column that contains the area codes */
    private NeissModelParameter<String> areaKey;

    /** The column that contains the data to be mapped */
    private NeissModelParameter<String> dataValues;

    /** A URL of the csv file that contains the required data */
    private NeissModelParameter<String> csvURL;

    public Mapper() {
        super();
        this.init();
    }

    @Override
    protected void populateParameterList() {
        if (this.parameters == null) {
            this.parameters = new ArrayList<NeissModelParameter<?>>();
            List<String[]> files = this.findAvailableDataFiles();
            if (files.size() > 0) {
                List<String> urls = new ArrayList<String>(files.get(0).length);
                List<String> geometrys = new ArrayList<String>(files.get(0).length);
                List<String> geomKeys = new ArrayList<String>(files.get(0).length);
                for (String[] s : files) {
                    urls.add(s[1]);
                    geometrys.add(s[2]);
                    geomKeys.add(s[3]);
                }
                this.csvURL = new NeissModelParameter<String>("Data URL", urls);
                this.geometry = new NeissModelParameter<String>("Geometry", geometrys);
                this.areaKey = new NeissModelParameter<String>("Area codes", geomKeys);
            } else {
                this.csvURL = new NeissModelParameter<String>("Data URL", "NO AVAIL FILES");
                this.geometry = new NeissModelParameter<String>("Geometry", "NO AVAIL FILES");
                this.areaKey = new NeissModelParameter<String>("Area codes", "NO AVAIL FILES");
                this.addToMessages("Mapper could not find any available data files (not an error).\n", false);
            }
            this.dataValues = new NeissModelParameter<String>("Data column", "_");
            this.parameters.add(this.geometry);
            this.parameters.add(this.areaKey);
            this.parameters.add(this.dataValues);
            this.parameters.add(this.csvURL);
        }
    }

    /**
    * This doesn't do anything as the BHPSLinker works directly with flat
    * files created by the PRM, doesn't need access to any external data.
    */
    @Override
    protected void setupInputDataSources() {
        return;
    }

    @Override
    protected void init() {
        Mapper.TOMCAT_DIR = NeissModel.getProperty("NeissModel_Tomcat_Dir", true);
        Mapper.TOMCAT_URL = NeissModel.getProperty("NeissModel_Tomcat_URL", true);
    }

    @Override
    public boolean runModel(List<NeissModelParameter<?>> parameters) {
        this.addToMessages("Mapper.runModel() starting\n", false);
        this.populateParameterList();
        this.addToMessages(this.setParameterValues(parameters) + "\n", false);
        boolean error = false;
        for (NeissModelParameter<?> p : parameters) {
            if (p == null || p.getValue().equals("")) {
                this.addToMessages("Error: parameter" + p.getName() + " is the empty string.\n", true);
                error = true;
            }
        }
        if (error) {
            this.xmlDoc.setSuccessFailure(false);
            return false;
        }
        this.runModel();
        return true;
    }

    private void runModel() {
        String urlStr = "http://www.maptube.org/mapcsv.aspx?" + "g=" + this.geometry.getValue().toString().trim() + "&ak=" + this.areaKey.getValue().toString().trim() + "&dc=" + this.dataValues.getValue().toString().trim() + "&csv=" + this.csvURL.getValue().toString().trim();
        this.addToMessages("Mapper has constructed url: " + urlStr + "\n", false);
        this.xmlDoc.addResultsData(urlStr, "URL of the generated map");
        this.xmlDoc.addImageData(urlStr, "URL of the generated map");
        this.xmlDoc.setSuccessFailure(true);
    }

    /**
    * Find out which data sources are available and which parameters are contained in each,
    * returning an xml-formatted list of all possible parameters.
    *
    * @return True if the operation was successful, false otherwise
    * @see NeissMapperXML The generated xml or a string saying what went wrong (if the
    * operation wasn't successful).
    */
    @Deprecated
    public boolean getMapperParameters(StringBuilder theXML) {
        List<String[]> files = this.findAvailableDataFiles();
        NeissMapperXML xml = new NeissMapperXML();
        for (String[] f : files) {
            String filename = f[0];
            String url = f[1];
            String theGeom = f[2];
            String areaCode = f[3];
            ArrayList<String> mappableColumns = new ArrayList<String>();
            for (int i = 4; i < f.length; i++) {
                mappableColumns.add(f[i]);
            }
            xml.addDataFile(url, url, theGeom, areaCode, mappableColumns);
        }
        theXML.append(xml.toString());
        return true;
    }

    /**
    * Returns an xml-formatted list of all possible parameters in the intput.
    *
    * @param inputReader The input to parse
    * @param theXML The generated xml or a string saying what went wrong (if the
    * operation wasn't successful).
    * @return True if the operation was successful, false otherwise
    */
    public static boolean getMapperParameters(InputStream inputReader, StringBuilder theXML) {
        String[] theFile = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputReader));
            theFile = analyseGeographicFile(br, null);
        } catch (IOException ex) {
            theXML.append("Caught an IOException reading a n input source: " + ex.getMessage() + "\n");
            return false;
        }
        if (theFile == null) {
            theXML.append("Could not parse the input.");
            return false;
        }
        NeissMapperXML xml = new NeissMapperXML();
        String filename = theFile[0];
        String url = theFile[1];
        String theGeom = theFile[2];
        String areaCode = theFile[3];
        ArrayList<String> mappableColumns = new ArrayList<String>();
        for (int i = 4; i < theFile.length; i++) {
            mappableColumns.add(theFile[i]);
        }
        xml.addDataFile(url, url, theGeom, areaCode, mappableColumns);
        theXML.append(xml.toString());
        return true;
    }

    /**
    * Find available data files and return their absolute file paths, urls and
    * some other information. Only works for files that contains ward or output
    * area at the moment.
    * @return A list containing the data file names as well as some other information.
    * The list contains these elements:
    * <UL>
    * <LI>0 - the name of the data file</LI>
    * <LI>1 - a url to the data file</LI>
    * <LI>2 - the gemetry of the data in the file</LI>
    * <LI>3 - the name of the column that contains the area code in it</LI>
    * <LI>4+ - the names of the other fields that can be aggregated</LI>
    * </UL>
    */
    @Deprecated
    private List<String[]> findAvailableDataFiles() {
        File serverDir = new File(Mapper.TOMCAT_DIR);
        if (!serverDir.exists() || !serverDir.isDirectory()) {
            this.addToMessages("Mapper.populateParameterList error: the server " + "directory doesn't exist or isn't a directory: " + serverDir.getAbsolutePath().toString() + "\n", true);
            return null;
        }
        File currentFile = null;
        List<String[]> files = new ArrayList<String[]>();
        files: for (File f : serverDir.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        })) {
            try {
                currentFile = f;
                BufferedReader br = new BufferedReader(new FileReader(f));
                String[] theFile = analyseGeographicFile(br, f);
                if (theFile != null) {
                    files.add(theFile);
                }
            } catch (FileNotFoundException ex) {
                this.addToMessages("Warning: Mapper.findAvailableDataFiles() caught a " + "FileNotFoundException in the server directory '" + serverDir.getAbsolutePath() + "': " + ex.getMessage() + "\n", true);
            } catch (IOException ex) {
                this.addToMessages("Warning: Mapper.findAvailableDataFiles() caught an " + "IOException reading a file in the server directory '" + serverDir.getAbsolutePath() + "': " + ex.getMessage() + "\n", true);
            } catch (NullPointerException ex) {
                this.addToMessages("Warning: Mapper.findAvailableFiles() caught a " + "NullPointerException reading file " + currentFile.getAbsolutePath() + " which means there are probably less than <2 lines in the file. Exception " + "message : " + ex.getMessage() + "\n", true);
            }
        }
        return files;
    }

    /**
    * Analyse a file to get information like geometry type, mappable
    * columns etc.
    * @param br A buffered reader reading the file. NOTE: the reader will be
    * closed once the first two lines have been read.
    * @param f (Optional) the file itself, not required if the BufferedReader is
    * reading a file that isn't locally stored. This can be used to return the
    * absolute file path and url if applicable.
    * @return File attributes as a String array:
    * <OL>
    * <LI>File path (if local) or null</LI>
    * <LI>File path on tomcat server (if local) or null</LI>
    * <LI>Geometry type (e.g. 'OA'). This will be a {@link GeographicAreaAttributes}
    * name.</LI>
    * <LI>Geometry column</LI>
    * <LI>(multiple) mappable columns (start at index 4 and continue until
    * length-1)</LI>
    * </OL>
    * Or return null if no geographic data column (i.e. a column with OA codes)
    * could be found.
    *
    * @see GeographicAreaAttributes 
    * @throws IOException
    */
    public static String[] analyseGeographicFile(BufferedReader br, File f) throws IOException {
        String[] line1 = br.readLine().trim().split("\\s*?,\\s*?");
        String[] line2 = br.readLine().trim().split("\\s*?,\\s*?");
        br.close();
        Pattern region = GeographicAreaAttributes.ONSDistricts.getPattern();
        Pattern ward = GeographicAreaAttributes.CASWards.getPattern();
        Pattern oa = GeographicAreaAttributes.OA.getPattern();
        int geomColumn = -1;
        String geometryType = null;
        List<String> mappableColumns = new ArrayList<String>();
        int regCol = -1, wardCol = -1, oaCol = -1;
        for (int i = 0; i < line1.length; i++) {
            if (region.matcher(line2[i]).matches()) {
                geometryType = GeographicAreaAttributes.ONSDistricts.name();
                regCol = i;
            } else if (ward.matcher(line2[i]).matches()) {
                geometryType = GeographicAreaAttributes.CASWards.name();
                wardCol = i;
            } else if (oa.matcher(line2[i]).matches()) {
                geometryType = GeographicAreaAttributes.OA.name();
                oaCol = i;
            }
        }
        if (oaCol != -1) {
            geomColumn = oaCol;
        } else if (wardCol != -1) {
            geomColumn = wardCol;
        } else if (regCol != -1) {
            geomColumn = regCol;
        } else {
            return null;
        }
        for (int i = 0; i < line1.length; i++) {
            if (i != geomColumn) {
                mappableColumns.add(line1[i].trim());
            }
        }
        String[] theFile = new String[4 + mappableColumns.size()];
        theFile[0] = f == null ? null : f.getAbsolutePath();
        theFile[1] = f == null ? null : Mapper.TOMCAT_URL + f.getName();
        theFile[2] = geometryType;
        theFile[3] = line1[geomColumn].trim();
        for (int i = 0; i < mappableColumns.size(); i++) {
            theFile[i + 4] = mappableColumns.get(i);
        }
        return theFile;
    }

    /**
    * Generate a maptube URL from the given data and field.
    * @param dataURL
    * @param fieldToMap
    * @param errors Optional object will be populated with error messages
    * @return The URL String or null if there was an error.
    */
    public static String generateMapFromURL(String dataURL, String fieldToMap, StringBuilder errors) {
        if (errors == null) {
            errors = new StringBuilder();
        }
        try {
            URL url = new URL(dataURL);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String[] fileInfo = analyseGeographicFile(br, null);
            if (fileInfo == null) {
                throw new Exception("generateMapfromURL: No geographic data column (e.g. " + "a column with OA codes) could be found in the data");
            }
            if (fileInfo.length < 5) {
                throw new Exception("analyseFile() has not returned enough information " + "(array should have at least 5 elements, but only has " + fileInfo.length + ". The array looks like: " + Arrays.toString(fileInfo));
            }
            boolean foundCol = false;
            for (int i = 4; i < fileInfo.length; i++) {
                if (fileInfo[i].equals(fieldToMap)) {
                    foundCol = true;
                }
            }
            if (!foundCol) {
                throw new Exception("The field to map (" + fieldToMap + ") does not appear in " + "the data: " + Arrays.toString(fileInfo));
            }
            String mapTitle = ("Map of " + fieldToMap + " from " + url.getPath()).replace(' ', '+');
            return "http://www.maptube.org/mapcsv.aspx?" + "g=" + fileInfo[2] + "&ti=" + mapTitle + "&ak=" + fileInfo[3] + "&dc=" + fieldToMap + "&csv=" + dataURL;
        } catch (MalformedURLException ex) {
            errors.append("MalformedURLException from input url: '" + dataURL + "': " + ex.getMessage() + "\n");
        } catch (IOException ex) {
            errors.append("IOException reading the data: " + ex.getMessage() + "\n");
        } catch (Exception ex) {
            errors.append("Caught an exception: " + ex.toString() + ". Exception message is: " + ex.getMessage() + ". Stack trace is:\n");
            for (StackTraceElement s : ex.getStackTrace()) {
                errors.append("\t" + s.toString() + "\n");
            }
        }
        return null;
    }

    /**
    * Function that takes an XML document containing results (desciptions and urls to files)
    * and looks at the riles by downloading a few lines to determine if they are mappable.
    * If they are then it saves the adds the parameters (geometry, mappable fields etc) and
    * returns them all in a single <code>NeissMapperXML</code> document.
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
                theFile = Mapper.analyseGeographicFile(br, null);
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
}
