package neissmodel;

import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.util.SardineException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import neissmodel.data_access.DataSource;
import neissmodel.xml.NeissModelXML;

/**
 *
 * @author Nick Malleson
 */
public abstract class NeissModel {

    protected String name;

    protected String description;

    protected List<NeissModelParameter<?>> parameters;

    private static Properties properties;

    /** An XML document to store the results of the model (and messages) */
    protected NeissModelXML xmlDoc;

    /** The datasources which contain the input data for this model. There can
    be many different data sources.*/
    private List<DataSource> inputDataSources;

    private static Logger log;

    /** A regular expression that matches the naming convention used for
     * synthetic population files (e.g. 00DAXP01.TXT') */
    private static final String POPFILE_REGEX = "(....)XP\\d\\d\\.TXT";

    static {
        log = NeissModelLogger.getLogger(NeissModel.class.getName());
        NeissModel.getProperties();
    }

    public NeissModel() {
        this.inputDataSources = new ArrayList<DataSource>();
        this.xmlDoc = new NeissModelXML();
        this.init();
        this.setupInputDataSources();
        this.populateParameterList();
    }

    /**
     * Return the model paramters as a String. See the NeissModelParameter class
     * for information about the structure of the string. Basically converts the
     * list of model parameters into a String a returns it.
     *
     * @see NeissModelParameter
     * @return The paramters required to run this model represented as a
     * single string.
     */
    public String getModelParameters() {
        populateParameterList();
        String parameterListString = NeissModelParameter.createParameterList(this.parameters);
        return parameterListString;
    }

    /**
     * Create the required parameters for this model and add them to the
     * 'parameters' list.
     */
    protected abstract void populateParameterList();

    /**
     * Create and initialise the <code>DataSource</code>s that contain the
     * required input model data.
     */
    protected abstract void setupInputDataSources();

    /**
     * Initialise any required properties.
     */
    protected abstract void init();

    /**
     * Get the name of this model.
     * @return the name of the model
     */
    public String getModelName() {
        return this.name;
    }

    /**
     * Describe the model in a few sentences.
     * @return a description of the model.
     */
    public String getModelDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        if (this.name == null) {
            return "Model '" + this.getClass().getSimpleName() + "'";
        } else {
            return "NeissModel '" + this.name + "'";
        }
    }

    /**
     * Run the model with the given inParameters. This must be overidden by
     * subclasses which supply the actual means of running a model.
     *
     * @param inParameters A list of inParameters which configure the model run.
     * @return True if the model was successful, false otherwise.
     */
    public abstract boolean runModel(List<NeissModelParameter<?>> parameters);

    /**
     * Convenience function that can be used by subclasses of
     * <code>NeissModel</code> to set the parameter values. Takes a list of
     * <code>NeissModelParameter</code>s and looks through this model's internal
     * list of parameters for matches, setting the internal values if a parameter
     * with a matching name is found.
     *
     * @param inParameters The paramters to use in configuring this model.
     * @return A string with any output infoMessages (whether successful or
     * unsuccessful)
     */
    protected String setParameterValues(List<NeissModelParameter<?>> inParameters) {
        String out = "";
        if (inParameters == null || inParameters.size() == 0) {
            out += ("Error, the list of parameters sent to the BirkinDynamicModel " + "is " + (inParameters == null ? "null" : "size zero") + ". Not continuing.");
            return out;
        }
        for (NeissModelParameter p : inParameters) {
            out += ("Received parameter '" + p.getName() + "'");
            boolean foundMatchingParam = false;
            for (NeissModelParameter q : this.parameters) {
                if (p.equals(q)) {
                    foundMatchingParam = true;
                    q.setValue(p.getValue());
                    out += (" which matches a model parameter. Setting value to: " + q.getValue().toString() + "\n");
                }
            }
            if (!foundMatchingParam) {
                out += (" but could not find a matching model parameter.\n");
            }
        }
        return out;
    }

    /**
     * Check that the given parameters match model parameters. Also check that all
     * model parameters have been accounted for. This can be useful to do before trying
     * to run a model using the given parameters.
     * @param inParams A list of parameters to check.
     * @param messages Optional messages to be passed back re. success or failure.
     * @return True if the parameters are correct, false otherwise.
     */
    public boolean checkParameters(List<NeissModelParameter<?>> inParams, StringBuilder messages) {
        if (messages == null) {
            messages = new StringBuilder();
        }
        if (this.parameters == null) {
            this.populateParameterList();
        }
        if (inParams == null || inParams.size() == 0) {
            messages.append("The list of parameters is " + (inParams == null ? "null" : "size zero") + "\n");
            return false;
        }
        List<NeissModelParameter<?>> modelParams = new ArrayList<NeissModelParameter<?>>();
        for (NeissModelParameter<?> p : this.parameters) {
            modelParams.add(p);
        }
        boolean fail = false;
        for (NeissModelParameter p : inParams) {
            messages.append("Received parameter '" + p.getName() + "'");
            int matchingParam = -1;
            for (int i = 0; i < modelParams.size(); i++) {
                NeissModelParameter q = modelParams.get(i);
                if (p.equals(q)) {
                    matchingParam = i;
                    messages.append(" which matches a model parameter. Setting value to: " + q.getValue().toString() + "\n");
                }
            }
            if (matchingParam != -1) {
                modelParams.remove(matchingParam);
            } else {
                messages.append(" but could not find a matching model parameter.\n");
                fail = true;
            }
        }
        if (fail) {
            return false;
        }
        if (modelParams.size() > 0) {
            messages.append("There are some model parameters that were not accounted for:\n");
            for (NeissModelParameter<?> p : modelParams) {
                messages.append("\t" + p.toString() + "\n");
            }
            return false;
        }
        return true;
    }

    /**
     * Set up the properties file. Look for it in a few expected directories (on
     * Nick's laptop or on the Leeds server), throwing a new RuntimeException
     * if a properties file could not be found.
     */
    private static void getProperties() throws RuntimeException {
        boolean nullLog = false;
        ConsoleHandler ch = new ConsoleHandler();
        if (log == null) {
            System.out.println("NeissModel.getProperties() called and the log is null, " + "writing to console temporarily.");
            log = Logger.getLogger(NeissModel.class.getName());
            log.addHandler(ch);
            nullLog = true;
        }
        try {
            if (NeissModel.properties == null) {
                File propFile = null;
                propFile = new File("./neiss_model.properties");
                if (propFile.exists()) {
                    log.log(Level.INFO, "Found properties file in current directory (" + propFile.getAbsolutePath() + "), reading properties.");
                    System.out.println("Found properties file in current directory (" + propFile.getAbsolutePath() + "), reading properties.");
                    setProperties(propFile);
                    return;
                }
                propFile = new File("/Users/nick/neiss/netbeans_workspace/NeissModel/neiss_model.properties");
                if (propFile.exists()) {
                    log.log(Level.INFO, "Found properties file on Nick's laptop (" + propFile.getAbsolutePath() + "), reading properties.");
                    setProperties(propFile);
                    return;
                }
                propFile = new File("/home/geonsm/neiss/NeissModel/neiss_model.properties");
                if (propFile.exists()) {
                    log.log(Level.INFO, "Found properties file on geo-s12 (" + propFile.getAbsolutePath() + "), reading properties.");
                    setProperties(propFile);
                    return;
                }
                propFile = new File("/home/neiss/model_data/neiss_model.properties");
                if (propFile.exists()) {
                    log.log(Level.INFO, "Found properties file on ngs.leeds.ac.uk (" + propFile.getAbsolutePath() + "), reading properties.");
                    setProperties(propFile);
                    return;
                }
                propFile = new File("/home/nick/NeissModel/neiss_model.properties");
                if (propFile.exists()) {
                    log.log(Level.INFO, "Found properties file on arc1 (" + propFile.getAbsolutePath() + "), reading properties.");
                    setProperties(propFile);
                    return;
                }
                throw new RuntimeException("Could not find a properties file anywhere.");
            }
        } finally {
            if (nullLog) {
                log.removeHandler(ch);
                log = null;
            }
        }
    }

    private static void setProperties(File propFile) {
        NeissModel.properties = new Properties();
        FileInputStream in;
        try {
            in = new FileInputStream(propFile.getAbsolutePath());
            NeissModel.properties.load(in);
            in.close();
            for (Enumeration e = properties.propertyNames(); e.hasMoreElements(); ) {
                String k = (String) e.nextElement();
                String newVal = System.getProperty(k);
                if (newVal != null) {
                    log.log(Level.FINER, "Found a system property '" + k + "->" + newVal + "' which matches a NeissModel property '" + k + "->" + properties.getProperty(k) + "', replacing the non-system one.");
                    properties.setProperty(k, newVal);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not load properties (IOException) from file: " + propFile.getAbsolutePath() + ". Reason: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get the value of a property in the properties file.
     * Can also be used to check a model property was loaded OK. If the input is
     * empty or null or if there is no property with a matching name, throw
     * a RuntimeException.
     *
     * @param input
     * @param throwError If true, an error will be thrown if the property is
     * not found. If false, null will just be returned.
     * @return A value for the property with the given name.
     */
    public static String getProperty(String input, boolean throwError) {
        if (input == null || input.equals("")) {
            throw new RuntimeException("NeissModel.getProperty() error, input " + "parameter (" + input + ") is " + (input == null ? "null" : "empty"));
        } else {
            if (NeissModel.properties == null) {
                getProperties();
            }
            String val = NeissModel.properties.getProperty(input);
            if (val == null || val.equals("")) {
                if (throwError) {
                    throw new RuntimeException("NeissModel.checkProperty() error, the required " + "property (" + input + ") is " + (input == null ? "null" : "empty"));
                } else {
                    return null;
                }
            }
            return val;
        }
    }

    /**
     * Set a single property. This is useful for hard-coded scripts.
     * Usually properties should be set by adding them to
     * the <code>neissmodel.properties</code> file or passing them as a
     * command-line parameter. E.g.:
     * <pre>{@code
     * java -DmyParameterParameter=aValue -jar NeissModel
     * }
     * </pre>
     *
     * @param key The parameter. Cannot be null or empty.
     * @param value The value which will be mapped to the parameter..
     * @throws RuntimeException if the key is null or the empty string.
     */
    public static void setProperty(String key, String value) {
        if (key == null || key.equals("")) {
            throw new RuntimeException("NeissModel.setProperty() error, the " + "key (" + key + ") is " + (key == null ? "null" : "empty"));
        }
        if (NeissModel.properties == null) {
            getProperties();
        }
        NeissModel.properties.put(key, value);
    }

    /**
     * Can be used to create the first properties file (easier than writing it by
     * hand. Once a properties file has been created, this shouldn't need to be
     * used again! Any new properties that are needed at a later date can be added
     * tp the file by hand
     */
    private void createInitialPropertiesFile(File propFile) {
        Properties initProps = new Properties();
        initProps.setProperty("BirkinPRM.PRM_ROOT_DIR", "/Users/nick/Documents/Dropbox/neiss/netbeans_workspace/NeissModel/prm");
        initProps.setProperty("BirkinDynamicModel.DYNAMIC_MODEL_ROOT_DIR", "/Users/nick/Documents/Dropbox/neiss/netbeans_workspace/NeissModel/dynamic_model");
        initProps.setProperty("BirkinPRM.DISTRICT_COLUMN", "DISTRICT");
        initProps.setProperty("BirkinPRM.MODEL_INPUT_FILE_NAME", "XXXXDATA.csv");
        initProps.setProperty("BirkinPRM.OUTPUT_AREA_COLUMN", "OA");
        initProps.setProperty("BirkinPRM.CENSUS_DATA_TABLE_NAME", "census2001");
        initProps.setProperty("BirkinDynamicModel.CENSUS_DATA_TABLE_NAME", "census2001");
        initProps.setProperty("BirkinDynamicModel.MODEL_INPUT_FILE_NAME", "ZZZZXPYY.csv");
        initProps.setProperty("BirkinDynamicModel.FERTILITY_INPUT_FILE_NAME", "FEXXXXYY.csv");
        initProps.setProperty("BirkinDynamicModel.MORTALITY_INPUT_FILE_NAME", "MOXXXXYY.csv");
        initProps.setProperty("BirkinDynamicModel.DISTRICT_COLUMN", "District");
        initProps.setProperty("BirkinDynamicModel.WARD_COLUMN", "Ward");
        initProps.setProperty("BirkinDynamicModel.MORTALITY_MALES_TABLE_NAME", "mort_males");
        initProps.setProperty("BirkinDynamicModel.MORTALITY_FEMALES_TABLE_NAME", "mort_females");
        initProps.setProperty("BirkinDynamicModel.FERTILITY_SINGLE_TABLE_NAME", "fert_single");
        initProps.setProperty("BirkinDynamicModel.FERTILITY_MARRIED_TABLE_NAME", "fert_married");
        initProps.setProperty("BirkinDynamicModel.DYNAMIC_MODEL_ROOT_DIR", "/Users/nick/Documents/Dropbox/neiss/netbeans_workspace/NeissModel/dynamic_model");
        try {
            FileOutputStream out = new FileOutputStream(propFile.getAbsolutePath());
            NeissModel.properties.clear();
            Collection<String> props = new LinkedList<String>();
            Enumeration names = initProps.propertyNames();
            while (names.hasMoreElements()) {
                props.add((String) names.nextElement());
            }
            for (String prop : props) {
                NeissModel.properties.setProperty(prop, initProps.getProperty(prop));
            }
            NeissModel.properties.store(out, "---Properties for the NeissModels---");
            out.close();
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Could not load properties (FileNotFound) from file: " + propFile.getAbsolutePath() + ". Reason: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new RuntimeException("Could not load properties (IOException) from file: " + propFile.getAbsolutePath() + ". Reason: " + ex.getMessage(), ex);
        }
    }

    /**
     * Get the messages and data produced by this model as an XML string.
     * @return
     */
    public String getMessages() {
        return this.xmlDoc.toString();
    }

    /**
     * Store the results (messages, results data, parameters etc - basically
     * everything in this model's <code>NiessModelXML</code> doc.
     *
     * @param f The file to store the results in
     * @throws IOException if there was a problem writing to the file.
     */
    public void storeResults(File f) throws IOException {
        log.log(Level.FINER, "Writing model results xml to file " + f.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        bw.write(this.xmlDoc.toString());
        bw.close();
        return;
    }

    public void addToMessages(String str, boolean error) {
        if (error) {
            this.xmlDoc.addErrorMessage(str);
            log.log(Level.SEVERE, str);
        } else {
            this.xmlDoc.addInfoMessage(str);
            log.log(Level.INFO, str);
        }
    }

    public List<DataSource> getDataSources() {
        return this.inputDataSources;
    }

    public void addDataSource(DataSource d) {
        this.inputDataSources.add(d);
    }

    public void clearDataSources() {
        this.inputDataSources.clear();
    }

    @Override
    public void finalize() {
        try {
            for (DataSource s : this.inputDataSources) {
                s.close();
            }
        } catch (Exception ex) {
            this.addToMessages("Exception closing data sources: " + ex.getMessage() + "\n", true);
        }
    }

    /**
     * Make 1-, 2-, 3- or 4-digit years into two digit strings.
     *
     * <ul>
     * <li>2 -> '02'</li>
     * <li>13 -> '13'</li>
     * <li>102 -> '02'</li>
     * <li>2032 -> '2032'</li>
     * </ul>
     */
    public static String twoDigitYear(int value) {
        assert value >= 0 : "NeissModel.twoDigitYear() doesn't know what to do with " + "a value of less tha zero: " + value;
        if (value < 0) {
            log.log(Level.WARNING, "NeissModel.twoDigitYear() doesn't know what to do with " + "a value of less tha zero: " + value, new Exception());
            return "";
        } else if (value < 10) {
            return "0" + value;
        } else if (value < 100) {
            return "" + value;
        } else if (value < 1000) {
            return ("" + value).substring(1);
        } else if (value >= 1000) {
            return ("" + value).substring(2);
        } else {
            log.log(Level.WARNING, "NeissModel.twoDigitYear() got an unexpected value: " + value, new Exception());
            return "";
        }
    }

    /**
     * Convenience method to write a text file from data in a data source. Used by DynamicModel and TranSim.
     *
     * @param inDataSources A list of available data sources that might hold the required data
     * @param outFileName The name of the output file to write
     * @param tableName The name of the dable in the data sources which holds the required data
     * @param regionName The names of the regions to interrogate. Passing a single region is fine.
     * @param databaseColumns The columns that hold the required data (in the order that the
     * columns should appear in the text file)
     * @param searchColumns Column(s) used to restrict the search (i.e. a district column)
     * @param orderColumns Column(s) used to order the data
     * @param separator The seperator for the columns
     * @param df An optional decimal format for decimal numbers
     * @param out An optional string builder for succes/failure messages
     * @param headerString An optional string that is appended to the beginning of the output file.
     * @return True if the operation was successful, false otherwise.
     */
    protected static boolean createTextFile(List<DataSource> inDataSources, String outFileName, String tableName, List<String> regionNames, String[] databaseColumns, String[] searchColumns, String[] orderColumns, String separator, DecimalFormat df, StringBuilder out, String headerString) {
        if (out == null) {
            out = new StringBuilder();
        }
        out.append("Looking for data source for table " + tableName);
        List<List<Object>> results = null;
        StringBuilder queryRes = new StringBuilder();
        for (DataSource ds : inDataSources) {
            results = ds.getValues(tableName, databaseColumns, searchColumns, new Object[] { regionNames }, orderColumns, DataSource.SEARCH_TYPE.EXACT, queryRes);
            if ((results == null) || (results.size() == 0) || (results.get(0).size() == 0)) {
                out.append("\tData source '" + ds.toString() + "' not correct: " + queryRes.toString() + "\n");
            } else {
                out.append("\tFound data source: " + ds.toString() + "\n");
                break;
            }
        }
        if (results == null || results.size() == 0 || results.get(0).size() == 0) {
            out.append("Error: no data found for regions: " + regionNames.toString() + ". The outpuy from the database is:" + queryRes.toString());
            return false;
        }
        String toWrite = "";
        if (headerString != null) {
            toWrite += headerString;
        }
        for (int row = 0; row < results.get(0).size(); row++) {
            for (int col = 0; col < results.size(); col++) {
                try {
                    Double d = (Double) results.get(col).get(row);
                    if (d < 1 && df != null) {
                        toWrite += (df.format(d) + separator);
                    } else {
                        toWrite += (d + separator);
                    }
                } catch (ClassCastException ex) {
                    toWrite += (results.get(col).get(row) + separator);
                }
            }
            toWrite = toWrite.substring(0, toWrite.length() - (separator.length())) + "\n";
        }
        File outFile = new File(outFileName);
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(outFile));
            w.write(toWrite);
            w.close();
        } catch (IOException ex) {
            out.append("Error writing emigration file: " + outFile.getAbsolutePath());
        }
        return true;
    }

    /**
     * See how many available population files there are by looking through
     * the files stored in the given root directory.
     *
     * @return A list of available regions.
     * @param rootDirString
     * @param fullFilenames Return the full filenames (true) or just the region names (false)
     */
    public static List<String> findPopulationDataFiles(String rootDirString, boolean fullFilenames) {
        List<String> rgns = new ArrayList<String>();
        File rootDir = new File(rootDirString);
        if (rootDir == null || !rootDir.exists()) {
            return null;
        }
        Pattern p = Pattern.compile(POPFILE_REGEX);
        Matcher m = null;
        for (String file : rootDir.list()) {
            m = p.matcher(file);
            if (m.matches()) {
                if (fullFilenames) {
                    rgns.add(file);
                } else {
                    rgns.add(m.group(1));
                }
            } else {
            }
        }
        return rgns;
    }

    /**
     * Allows saving (copying) results to another location (i.e. not the machine
     * that was used to run the model and, on which, the results are currently located).
     * At the moment this is used to copy results to a WebDav server so that they
     * are publically available.
     *
     * @param userid An optional userid. If this is null or empty then the results
     * will be stored in the root directory on the web server, otherwise they
     * will be stored in a <code>userid</code> sub directory.
     * @param xml The XML document which is being used to store information about the
     * model run. The location of the file on the new location will be added to this
     * document.
     * @param resultsFile The file of results to be saved.
     * @param desc A description of the results
     * @return True if successful, false otherwise
     */
    public static boolean saveResultsExternally(String userid, NeissModelXML xml, File resultsFile, String desc) {
        Sardine sardine = null;
        InputStream fis = null;
        String url = null;
        String rootURL = NeissModel.getProperty(PCONST.WEBDAV_URL, true);
        log.log(Level.FINER, "Will attempt to save data on server: " + rootURL);
        try {
            String[] userPass = getWebDavUserPassword();
            if (userPass == null) {
                String msg = "Error getting the username and password for the WebDav server, " + "see previous error messages";
                log.log(Level.SEVERE, msg);
                throw new IOException(msg);
            }
            log.log(Level.FINER, "saveResults() has read username and password for webdav server");
            sardine = SardineFactory.begin(userPass[0], userPass[1]);
            fis = new FileInputStream(resultsFile);
            if (userid == null || userid.length() == 0) {
                url = rootURL + resultsFile.getName();
            } else {
                String dirUrl = rootURL + userid + "/";
                log.log(Level.FINER, "Saving file in user's directoy (" + dirUrl + ")");
                url = dirUrl + resultsFile.getName();
                try {
                    sardine.createDirectory(dirUrl);
                } catch (SardineException ex) {
                    log.log(Level.FINER, "User directory " + dirUrl + " already exists");
                }
            }
            log.log(Level.FINER, "saveResults() will now upload the file " + resultsFile.getAbsolutePath() + " to " + url);
            sardine.put(url, fis);
            fis.close();
            xml.addResultsData(url, desc);
        } catch (FileNotFoundException ex) {
            log.log(Level.SEVERE, "Could not copy results to the external machine (web server). " + "Message is " + ex.getMessage() == null ? "<null>" : ex.getMessage(), ex);
            return false;
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Could not copy results to the external machine (web server). " + "Message is " + ex.getMessage() == null ? "<null>" : ex.getMessage(), ex);
            return false;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Could not copy results to the external machine (web server). " + "Message is " + ex.getMessage() == null ? "<null>" : ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    /**
     * Convenience method to get the username and password for the webdav server, these
     * are stored separately from other project files for security.
     * @return An array of strings with username as the first item and password as the second.
     */
    private static String[] getWebDavUserPassword() throws FileNotFoundException, IOException {
        File f = new File(NeissModel.getProperty(PCONST.WEBDAV_PASSWORD_FILE, true));
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;
        String[] userPass = new String[2];
        int linecount = 0;
        while ((line = br.readLine()) != null) {
            if (!line.startsWith("#")) {
                userPass[linecount] = line.trim();
                if (linecount > 2) {
                    String msg = "NeissModel.getWebDavUserPassword(): unexpected line in username/password " + "file (have already read the username and password, the file should have terminated";
                    log.log(Level.SEVERE, msg, new Exception());
                    return null;
                }
                linecount++;
            }
        }
        return userPass;
    }

    /**
     * Convenience method to create a URL connection. This is useful because
     * sometimes a web proxy is required (e.g. on Arc1) and in these cases
     * a 'WebProxy' parameter can be set using the neissmodel.properties file.
     *
     * @param urlStr The URL to connect to.
     * @param messages
     * @return The connection object.
     * @throws MalformedURLException
     * @throws IOException
     */
    public static HttpURLConnection createHTTPConnection(String urlStr, StringBuilder messages) throws MalformedURLException, IOException {
        if (messages == null) {
            messages = new StringBuilder();
        }
        URL url = new URL(urlStr);
        HttpURLConnection uc = null;
        String newProxy = NeissModel.getProperty(PCONST.WEB_PROXY, false);
        String[] split;
        if (newProxy != null && newProxy.length() > 0) {
            split = newProxy.split(",");
            try {
                String proxyURL = split[0];
                int proxyPort = Integer.parseInt(split[1]);
                Proxy prxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyURL, proxyPort));
                messages.append("Setting http proxy (host,port) to: " + proxyURL + "," + proxyPort + "\n");
                uc = (HttpURLConnection) url.openConnection(prxy);
            } catch (NumberFormatException ex) {
                messages.append("PRMWithData.lookForExternalCSV Could not parse the proxy " + "port '" + split[1] + "'.\n");
                return null;
            } catch (ArrayIndexOutOfBoundsException ex) {
                messages.append("PRMWithData.lookForExternalCSV error: the proxy specified (" + newProxy + ") in neissmodel.properties must have at least two elements when split on a " + "comma (the url and port), not " + split.length + "\n");
                return null;
            }
        } else {
            uc = (HttpURLConnection) url.openConnection();
        }
        return uc;
    }

    /**
     * Convenience method gets the current date and time, formatted as follows:
     * <code>YYYY-MM-DD-HH:mm:ss:ms</code>
     */
    public static String currentTime() {
        return NeissModel.currentTime(null);
    }

    /**
     * Convenience method gets the current date and time, formatted as follows:
     * <code>YYYY-MM-DD-HH:mm:ss:ms</code>
     * @param colon Optionally set the character to use instead of a colon
     * (useful if the time is used in a URL). If this is null then a colon is used.
     */
    public static String currentTime(String colon) {
        if (colon == null) {
            colon = ":";
        }
        Calendar now = new GregorianCalendar();
        return now.get(Calendar.YEAR) + "-" + twoDigits(now.get(Calendar.MONTH) + 1) + "-" + twoDigits(now.get(Calendar.DAY_OF_MONTH)) + "-" + twoDigits(now.get(Calendar.HOUR_OF_DAY)) + colon + twoDigits(now.get(Calendar.MINUTE)) + colon + twoDigits(now.get(Calendar.SECOND)) + colon + twoDigits(now.get(Calendar.MILLISECOND));
    }

    private static String twoDigits(int i) {
        if (i < 10) {
            return "0" + i;
        } else {
            return i + "";
        }
    }
}

/** Simple class to represent a file created on a server somewhere that a
 * <code>NeissModel</code> creates and a person who ran the model can download.
 *
 * @author Nick Malleson
 */
class ResultFile {

    /**
     * The name of the file. Does not need to bare any relation to the url.
     */
    public String name = "";

    /**
     * A description of the file.
     */
    public String description = "";

    /**
     * The URL where the file is stored.
     */
    URL url = null;
}
