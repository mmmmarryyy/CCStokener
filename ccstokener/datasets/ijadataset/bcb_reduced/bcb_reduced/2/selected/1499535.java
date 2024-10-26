package marla.ide.resource;

import marla.ide.gui.Domain;
import marla.ide.gui.MainFrame;
import marla.ide.gui.WorkspacePanel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import marla.ide.latex.LatexExporter;
import marla.ide.operation.OperationXMLException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import marla.ide.problem.MarlaException;
import marla.ide.operation.OperationXML;
import marla.ide.problem.InternalMarlaException;
import marla.ide.problem.Problem;
import marla.ide.r.RProcessor;
import marla.ide.r.RProcessor.RecordMode;
import marla.ide.r.RProcessorException;

/**
 * Configures most aspects of maRla by calling out to the appropriate settings
 * methods in other classes
 * @author Ryan Morehart
 */
public class Configuration {

    /**
	 * Configuration elements which are required for maRla to run.
	 */
    public static ConfigType[] requiredConfig = new ConfigType[] { ConfigType.R, ConfigType.PrimaryOpsXML };

    /**
	 * Current instance oconfigXML.getChild("r")f Configuration
	 */
    private static Configuration instance = null;

    /**
	 * Controls whether detailed messages about where things are configured from
	 * are displayed
	 */
    private boolean detailedConfigStatus = true;

    /**
	 * Cache of wiki settings page
	 */
    private static String pageCache = null;

    /**
	 * Saves where the last load() tried to load its configuration from.
	 * Useful for saving back to the same location
	 */
    private final String configPath;

    /**
	 * Parsed XML from config file
	 */
    private Element configXML = null;

    /**
	 * Possible elements that can be configured through this class
	 */
    public enum ConfigType {

        DebugMode, FirstRun, BrowseLocation, WindowX, WindowY, WindowHeight, WindowWidth, PdfTex, R, PrimaryOpsXML, UserOpsXML, TexTemplate, UserName, ClassShort, ClassLong, MinLineWidth, LineSpacing, SendErrorReports, ReportWithProblem, ErrorServer
    }

    ;

    /**
	 * Creates new instance of the configuration pointed to the given configuration file
	 */
    private Configuration(String configPath) {
        this.configPath = configPath;
    }

    /**
	 * Gets the current instance of the Configuration. Creates a new one if needed
	 * @return Working instance of Configuration class
	 */
    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration(locateConfig());
        }
        return instance;
    }

    /**
	 * Loads configuration from XML file. No attempt is made to search
	 * for missing config elements.
	 * @return true if the configuration file was loaded successfully
	 */
    private boolean reloadXML() {
        try {
            System.out.println("Using config file at '" + configPath + "'");
            SAXBuilder parser = new SAXBuilder();
            Document doc = parser.build(configPath);
            configXML = doc.getRootElement();
            return true;
        } catch (JDOMException ex) {
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
	 * Loads configuration from all possible sources. Processing order is:
	 *		Command Line->XML file->Search->Defaults
	 * Items fulfilled at higher levels are skipped later
	 * @param args Command line options to be parsed
	 * @return List of the config types that failed to be configured
	 */
    public List<ConfigType> configureAll(String[] args) {
        ConfigType[] vals = ConfigType.values();
        int currProgress = 10;
        int max = 80;
        int valCount = vals.length;
        int incr = max;
        if (valCount > 0) incr = (int) Math.floor((max - currProgress) / valCount); else incr = max;
        List<ConfigType> unconfigured = new ArrayList<ConfigType>();
        for (ConfigType c : vals) {
            if (!configureFromBest(c, args)) {
                unconfigured.add(c);
            }
            currProgress += incr;
            Domain.setProgressString(currProgress + "%");
            Domain.setProgressValue(currProgress);
        }
        if (unconfigured.isEmpty()) {
            Domain.setProgressStatus("Configuration complete");
        } else {
            Domain.setProgressStatus(unconfigured.size() + " unconfigured");
        }
        if (detailedConfigStatus) {
            System.out.println("Configuration:");
            for (ConfigType c : ConfigType.values()) {
                System.out.print("\t" + c + ": ");
                try {
                    System.out.println(get(c));
                } catch (MarlaException ex) {
                    System.out.println("unset (" + ex.getMessage() + ")");
                }
            }
        }
        List<ConfigType> requiredAndMissing = new ArrayList<ConfigType>();
        for (ConfigType c : unconfigured) {
            for (ConfigType r : requiredConfig) {
                if (r == c) requiredAndMissing.add(c);
            }
        }
        return requiredAndMissing;
    }

    /**
	 * Gets the given configuration item's value
	 * @param setting Setting to adjust
	 * @return Currently set value for configuration item, null if there was none
	 */
    public Object get(ConfigType setting) {
        switch(setting) {
            case PdfTex:
                return LatexExporter.getPdfTexPath();
            case PrimaryOpsXML:
                return OperationXML.getPrimaryXMLPath();
            case UserOpsXML:
                List<String> paths = OperationXML.getUserXMLPaths();
                if (paths != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < paths.size(); i++) {
                        sb.append(paths.get(i));
                        if (i != paths.size() - 1) sb.append('|');
                    }
                    return sb.toString();
                } else return null;
            case R:
                return RProcessor.getRLocation();
            case DebugMode:
                return Domain.isDebugMode();
            case FirstRun:
                return Domain.isFirstRun();
            case BrowseLocation:
                return Domain.lastBrowseLocation();
            case TexTemplate:
                return LatexExporter.getDefaultTemplate();
            case ClassLong:
                return Problem.getDefaultLongCourseName();
            case ClassShort:
                return Problem.getDefaultShortCourseName();
            case UserName:
                return Problem.getDefaultPersonName();
            case LineSpacing:
                return WorkspacePanel.getLineSpacing();
            case MinLineWidth:
                return WorkspacePanel.getMinLineWidth();
            case SendErrorReports:
                return Domain.getSendReport();
            case ReportWithProblem:
                return Domain.getReportIncludesProblem();
            case ErrorServer:
                return Domain.getErrorServer();
            case WindowX:
                if (Domain.getInstance() != null) return Domain.getInstance().getMainFrame().getX(); else return null;
            case WindowY:
                if (Domain.getInstance() != null) return Domain.getInstance().getMainFrame().getY(); else return null;
            case WindowHeight:
                if (Domain.getInstance() != null) return Domain.getInstance().getMainFrame().getHeight(); else return null;
            case WindowWidth:
                if (Domain.getInstance() != null) return Domain.getInstance().getMainFrame().getWidth(); else return null;
            default:
                throw new InternalMarlaException("Unhandled configuration exception type in get");
        }
    }

    /**
	 * Sets the given configuration item to the given value
	 * @param setting Setting to adjust
	 * @param val Value to assign to setting
	 * @return Previously set value for configuration item, null if there was none
	 */
    public Object set(ConfigType setting, Object val) {
        Object previous = null;
        MainFrame frame = null;
        int x;
        int y;
        int height;
        int width;
        switch(setting) {
            case PdfTex:
                previous = LatexExporter.setPdfTexPath(val.toString());
                break;
            case PrimaryOpsXML:
                OperationXML.clearXMLOps();
                previous = OperationXML.setPrimaryXMLPath(val.toString());
                break;
            case UserOpsXML:
                previous = get(setting);
                OperationXML.clearXMLOps();
                OperationXML.setUserXMLPaths(Arrays.asList(val.toString().split("\\|")));
                break;
            case R:
                previous = RProcessor.setRLocation(val.toString());
                break;
            case DebugMode:
                Boolean mode = true;
                if (val instanceof Boolean) mode = (Boolean) val; else mode = Boolean.valueOf(val.toString());
                previous = Domain.isDebugMode(mode);
                detailedConfigStatus = mode;
                if (mode) RProcessor.setDebugMode(RecordMode.FULL); else RProcessor.setDebugMode(RecordMode.DISABLED);
                break;
            case FirstRun:
                Boolean first = true;
                if (val instanceof Boolean) first = (Boolean) val; else first = Boolean.valueOf(val.toString());
                previous = Domain.isFirstRun(first);
                break;
            case BrowseLocation:
                previous = Domain.lastBrowseLocation(val.toString());
                break;
            case TexTemplate:
                previous = LatexExporter.setDefaultTemplate(val.toString());
                break;
            case ClassLong:
                previous = Problem.setDefaultLongCourseName(val.toString());
                break;
            case ClassShort:
                previous = Problem.setDefaultShortCourseName(val.toString());
                break;
            case UserName:
                previous = Problem.setDefaultPersonName(val.toString());
                break;
            case LineSpacing:
                if (val instanceof Integer) previous = WorkspacePanel.setLineSpacing((Integer) val); else previous = WorkspacePanel.setLineSpacing(Integer.parseInt(val.toString()));
                break;
            case MinLineWidth:
                if (val instanceof Integer) previous = WorkspacePanel.setMinLineWidth((Integer) val); else previous = WorkspacePanel.setMinLineWidth(Integer.parseInt(val.toString()));
                break;
            case ErrorServer:
                previous = Domain.setErrorServer(val.toString());
                break;
            case SendErrorReports:
                if (val instanceof Boolean) previous = Domain.setSendReport((Boolean) val); else previous = Domain.setSendReport(Boolean.parseBoolean(val.toString().toLowerCase()));
                break;
            case ReportWithProblem:
                if (val instanceof Boolean) previous = Domain.setReportIncludesProblem((Boolean) val); else previous = Domain.setReportIncludesProblem(Boolean.parseBoolean(val.toString().toLowerCase()));
                break;
            case WindowX:
                try {
                    frame = Domain.getInstance().getMainFrame();
                    y = frame.getY();
                    if (val instanceof Integer) x = (Integer) val; else x = Integer.parseInt(val.toString());
                    frame.setLocation(x, y);
                    MainFrame.progressFrame.setLocationRelativeTo(frame);
                } catch (NullPointerException ex) {
                    throw new ConfigurationException("No window currently available to set", setting);
                }
                break;
            case WindowY:
                try {
                    frame = Domain.getInstance().getMainFrame();
                    x = frame.getX();
                    if (val instanceof Integer) y = (Integer) val; else y = Integer.parseInt(val.toString());
                    frame.setLocation(x, y);
                    MainFrame.progressFrame.setLocationRelativeTo(frame);
                } catch (NullPointerException ex) {
                    throw new ConfigurationException("No window currently available to set", setting);
                }
                break;
            case WindowHeight:
                try {
                    frame = Domain.getInstance().getMainFrame();
                    x = frame.getX();
                    y = frame.getY();
                    width = frame.getWidth();
                    if (val instanceof Integer) height = (Integer) val; else height = Integer.parseInt(val.toString());
                    frame.setBounds(x, y, width, height);
                    MainFrame.progressFrame.setLocationRelativeTo(frame);
                } catch (NullPointerException ex) {
                    throw new ConfigurationException("No window currently available to set", setting);
                }
                break;
            case WindowWidth:
                try {
                    frame = Domain.getInstance().getMainFrame();
                    x = frame.getX();
                    y = frame.getY();
                    height = frame.getHeight();
                    if (val instanceof Integer) width = (Integer) val; else width = Integer.parseInt(val.toString());
                    frame.setBounds(x, y, width, height);
                    MainFrame.progressFrame.setLocationRelativeTo(frame);
                } catch (NullPointerException ex) {
                    throw new ConfigurationException("No window currently available to set", setting);
                }
                break;
            default:
                throw new InternalMarlaException("Unhandled configuration exception type in name");
        }
        return previous;
    }

    /**
	 * Runs down the chain of possibilities, configuring the given setting
	 * from the "best" source (command line->xml->search->default)
	 * @param setting Setting to configure
	 * @param args The command line. If null, will be skipped entirely
	 * @return true if setting was configured, false otherwise
	 */
    public boolean configureFromBest(ConfigType setting, String[] args) {
        String dispStr = getName(setting);
        Domain.setProgressStatus("Configuring " + dispStr + " from command line...");
        if (args != null && configureFromCmdLine(setting, args)) return true;
        Domain.setProgressStatus("Configuring " + dispStr + " from XML...");
        if (configPath != null && configXML == null) reloadXML();
        if (configXML != null && configureFromXML(setting)) return true;
        Domain.setProgressStatus("Searching for " + dispStr + "...");
        if (configureFromSearch(setting)) return true;
        Domain.setProgressStatus("Using default for " + dispStr + "...");
        if (configureFromDefault(setting)) return true;
        return false;
    }

    /**
	 * Sets configuration parameters based loaded configuration file
	 * @param setting Setting to configure
	 * @return true if successfully configured, false otherwise
	 */
    public boolean configureFromXML(ConfigType setting) {
        boolean success = false;
        try {
            String val = configXML.getChildText(setting.toString());
            if (val != null) {
                set(setting, val);
                success = true;
            } else success = false;
        } catch (MarlaException ex) {
            success = false;
        }
        if (detailedConfigStatus && success) System.out.println("Configured " + setting + " from config file");
        return success;
    }

    /**
	 * Sets configuration parameters based on command line
	 * @param setting Setting to configure based on command line
	 * @param args Command line parameters, as given to main() by the VM
	 * @return true if successfully configured, false otherwise
	 */
    public boolean configureFromCmdLine(ConfigType setting, String[] args) {
        String setName = "--" + setting.toString();
        for (String arg : args) {
            if (arg.startsWith(setName)) {
                try {
                    set(setting, arg.substring(arg.indexOf('=') + 1));
                    if (detailedConfigStatus) System.out.println("Configured " + setting + " from command line");
                    return true;
                } catch (MarlaException ex) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
	 * Attempts to search for the given setting's file (if applicable)
	 * @param setting Configuration setting to look for
	 * @return true if the item is configured successfully, false otherwise
	 */
    public boolean configureFromSearch(ConfigType setting) {
        boolean success = false;
        switch(setting) {
            case PdfTex:
                success = findAndSetPdfTex();
                break;
            case PrimaryOpsXML:
                success = findAndSetOpsXML();
                break;
            case R:
                success = findAndSetR();
                break;
            case TexTemplate:
                success = findAndSetLatexTemplate();
                break;
            case ErrorServer:
                success = retreiveAndSetErrorServer();
                break;
            default:
                success = false;
        }
        if (detailedConfigStatus && success) System.out.println("Configured " + setting + " from search");
        return success;
    }

    /**
	 * Sets configuration elements from those that have logical defaults
	 * @param setting Configuration setting to find a default for
	 * @return true if the item is configured successfully, false otherwise
	 */
    public boolean configureFromDefault(ConfigType setting) {
        boolean success = false;
        try {
            switch(setting) {
                case DebugMode:
                    set(setting, false);
                    success = true;
                    break;
                case FirstRun:
                    set(setting, true);
                    success = true;
                    break;
                case MinLineWidth:
                    set(setting, 2);
                    success = true;
                    break;
                case LineSpacing:
                    set(setting, 4);
                    success = true;
                    break;
                case SendErrorReports:
                case ReportWithProblem:
                    set(setting, true);
                    success = true;
                    break;
                case ErrorServer:
                    set(setting, "http://www.moreharts.com/marla/report.php");
                    success = true;
                    break;
                case ClassLong:
                case ClassShort:
                case UserName:
                    set(setting, "");
                    success = true;
                    break;
                case UserOpsXML:
                    set(setting, "");
                    success = true;
                    break;
                case BrowseLocation:
                    set(setting, System.getProperty("user.home"));
                    success = true;
                    break;
                case WindowHeight:
                case WindowWidth:
                case WindowX:
                case WindowY:
                    success = true;
                    break;
                default:
                    success = false;
            }
        } catch (MarlaException ex) {
            success = false;
        }
        if (detailedConfigStatus && success) System.out.println("Configured " + setting + " with default");
        return success;
    }

    /**
	 * Locates maRla configuration file and returns the path to use. The path
	 * "found" may actually not exist, it will return the default if none exists
	 * currently
	 * @return Path to configuration file, which may or may not exist
	 */
    private static String locateConfig() {
        File[] configPaths = new File[] { new File(System.getProperty("user.home") + "/" + ".marla/config.xml"), new File("config.xml") };
        for (int i = 0; i < configPaths.length; i++) {
            if (configPaths[i].exists()) return configPaths[i].getPath();
        }
        return configPaths[0].getPath();
    }

    /**
	 * Saves current maRla configuration to the location we loaded from
	 */
    public void save() {
        try {
            System.out.println("Writing configuration to '" + configPath + "'");
            FileUtils.forceMkdir(new File(configPath).getAbsoluteFile().getParentFile());
            String conf = getConfigXML();
            BufferedWriter os = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configPath)));
            os.write(conf, 0, conf.length());
            os.close();
        } catch (IOException ex) {
            throw new MarlaException("Problem occured writing to configuration file", ex);
        }
    }

    /**
	 * Retrieves all current settings and saves them to an XML string
	 * @return XML for the current configuration
	 */
    public String getConfigXML() {
        Element rootEl = new Element("marla");
        Document doc = new Document(rootEl);
        for (ConfigType c : ConfigType.values()) {
            try {
                Element el = new Element(c.toString());
                el.addContent(get(c).toString());
                rootEl.addContent(el);
            } catch (NullPointerException ex) {
            }
        }
        try {
            StringWriter os = new StringWriter();
            Format formatter = Format.getPrettyFormat();
            XMLOutputter xml = new XMLOutputter(formatter);
            xml.output(doc, os);
            return os.toString();
        } catch (IOException ex) {
            throw new MarlaException("Problem occured writing to configuration file", ex);
        }
    }

    /**
	 * Checks if the given executable is on the system's PATH
	 * @param exeName executable to locate. On Windows this must include the extension
	 * @return true if the executable is on the path, false otherwise
	 */
    public static boolean isOnPath(String exeName) {
        System.out.println("Looking for '" + exeName + "' on PATH");
        String[] pathDirs = System.getenv("PATH").split(";|:");
        for (String dirPath : pathDirs) {
            File exeNoExt = new File(dirPath + "/" + exeName);
            File exeWithExt = new File(dirPath + "/" + exeName + ".exe");
            if (exeNoExt.exists() || exeWithExt.exists()) return true;
        }
        return false;
    }

    /**
	 * Looks for R in typical binary locations on Windows, Linux, and OSX. Once
	 * located, it sets the correct values
	 * @return true if R is found, false otherwise
	 */
    private static boolean findAndSetR() {
        List<String> possibilities = findFile("R(\\.exe)?", "R.*|bin|usr|local|lib|Program Files.*|x64|i386|Library|Frameworks", null);
        for (String exe : possibilities) {
            try {
                System.out.println("Checking '" + exe + "'");
                RProcessor.setRLocation(exe);
                RProcessor.getInstance();
                return true;
            } catch (RProcessorException ex) {
            } catch (ConfigurationException ex) {
            }
        }
        return false;
    }

    /**
	 * Looks for primary operation XML file in typical install locations
	 * on Windows, Linux, and OSX, as well as close to the run directory. Once
	 * located, it sets the correct values
	 * @return true if ops XML is found, false otherwise
	 */
    private static boolean findAndSetOpsXML() {
        List<String> possibilities = findFile("ops\\.xml", "config|xml|test|etc|ma[rR]la|Program Files.*", null);
        for (String path : possibilities) {
            try {
                System.out.println("Checking '" + path + "'");
                OperationXML.clearXMLOps();
                OperationXML.setPrimaryXMLPath(path);
                OperationXML.loadXML();
                return true;
            } catch (OperationXMLException ex) {
            } catch (ConfigurationException ex) {
            }
        }
        return false;
    }

    /**
	 * Looks for Latex template in typical install locations on Windows,
	 * Linux, and OSX, as well as close to the working directory. Once
	 * located, it sets the correct values
	 * @return true if template is found, false otherwise
	 */
    private static boolean findAndSetLatexTemplate() {
        List<String> possibilities = findFile("export_template\\.xml", "config|xml|test|etc|ma[rR]la|Program Files.*", null);
        for (String path : possibilities) {
            try {
                System.out.println("Checking '" + path + "'");
                LatexExporter.setDefaultTemplate(path);
                return true;
            } catch (ConfigurationException ex) {
            }
        }
        return false;
    }

    /**
	 * Looks for pdfTeX in typical binary locations on Windows, Linux, and OSX. Once
	 * located, it sets the correct values
	 * @return true if pdfTeX is found, false otherwise
	 */
    private static boolean findAndSetPdfTex() {
        List<String> possibilities = findFile("pdf(la)?tex(\\.exe)?", "bin|usr|Program Files.*|.*[Tt][Ee][Xx].*|local|20[0-9]{2}|.*darwin|Contents|Resources|Portable.*", null);
        for (String exe : possibilities) {
            try {
                System.out.println("Checking '" + exe + "'");
                LatexExporter.setPdfTexPath(exe);
                return true;
            } catch (ConfigurationException ex) {
            }
        }
        return false;
    }

    /**
	 * Returns a path to the given executable or null if none is found
	 * @param fileName Name of the executable to find. Should not include the .exe portion
	 *	(although that will still function)
	 * @param dirSearch Regular expression of the directories to search. Any directory that
	 *	matches this pattern will be recursed into
	 * @param additional List of directories to manually add to the search
	 * @return Path to executable, as usable by createProcess(). Null if not found
	 */
    public static List<String> findFile(String fileName, String dirSearch, List<File> additional) {
        System.out.println("Looking for '" + fileName + "', please wait");
        Pattern namePatt = Pattern.compile(fileName);
        Pattern dirPatt = Pattern.compile(dirSearch);
        List<File> checkPaths = new ArrayList<File>();
        if (additional != null) checkPaths.addAll(additional);
        @SuppressWarnings("unchecked") Collection<File> currDirSearchRes = FileUtils.listFiles(new File("."), new RegexFileFilter(namePatt), new RegexFileFilter(dirPatt));
        checkPaths.addAll(currDirSearchRes);
        File[] roots = File.listRoots();
        for (int i = 0; i < roots.length; i++) {
            if (!roots[i].isDirectory()) continue;
            @SuppressWarnings("unchecked") Collection<File> driveSearchRes = FileUtils.listFiles(roots[i], new RegexFileFilter(namePatt), new RegexFileFilter(dirPatt));
            checkPaths.addAll(driveSearchRes);
        }
        List<String> files = new ArrayList<String>(checkPaths.size());
        System.out.println(checkPaths.size() + " possibilities found for " + fileName + ": ");
        for (File f : checkPaths) {
            System.out.println("\t" + f.getPath());
            files.add(f.getAbsolutePath());
        }
        return files;
    }

    /**
	 * Fetches the wiki setting page and searches it for the given tag
	 * @param tag Tag to search for on wiki page
	 * @return String found between the tag markers
	 */
    public static String fetchSettingFromServer(String tag) {
        try {
            if (pageCache == null) {
                URL url = new URL("http://code.google.com/p/marla/wiki/CurrentSettings");
                URLConnection conn = url.openConnection();
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder page = new StringBuilder();
                String line = null;
                while ((line = rd.readLine()) != null) page.append(line);
                rd.close();
                pageCache = page.toString();
            }
            Pattern server = Pattern.compile("\\|" + tag + "\\|(.+)\\|" + tag + "\\|");
            Matcher m = server.matcher(pageCache.toString());
            if (!m.find()) return null;
            return m.group(1);
        } catch (IOException ex) {
            return null;
        }
    }

    /**
	 * Gets the current error server report URL from the settings page. 
	 * @return true if server is found, false otherwise
	 */
    private static boolean retreiveAndSetErrorServer() {
        try {
            String server = fetchSettingFromServer("SERVER");
            if (server != null) {
                Domain.setErrorServer(server);
                return true;
            } else return false;
        } catch (ConfigurationException ex) {
            return false;
        }
    }

    /**
	 * Gets a user-friendly name for the given setting
	 * @param setting Setting to find a name for
	 * @return User-friendly name
	 */
    public static String getName(ConfigType setting) {
        switch(setting) {
            case PdfTex:
                return "pdfTeX path";
            case PrimaryOpsXML:
                return "Operation XML path";
            case R:
                return "R path";
            case TexTemplate:
                return "LaTeX export template path";
            default:
                return setting.toString();
        }
    }

    /**
	 * Convenience method to do the most common configuration stuff. Intended
	 * only for use when nothing will need manual configuration (pre-setup computer).
	 * Assumes no command line parameters
	 * @return true if all items are configured, false otherwise
	 */
    public static boolean load() {
        return Configuration.getInstance().configureAll(new String[] {}).isEmpty();
    }

    /**
	 * Loads given command line configuration and saves to
	 * file. Useful for creating new configuration
	 */
    public static void main(String[] args) {
        Configuration conf = Configuration.getInstance();
        for (ConfigType c : ConfigType.values()) {
            if (conf.configureFromCmdLine(c, args)) System.out.println("Configured " + c);
        }
        conf.save();
    }
}
