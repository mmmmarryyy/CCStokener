package gleam.annotatorgui;

import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.GateConstants;
import gate.LanguageResource;
import gate.creole.ResourceInstantiationException;
import gate.creole.ontology.Ontology;
import gate.gui.docview.AnnotationSetsView;
import gate.util.GateException;
import gleam.annotatorgui.actions.ActionConnectToExecutive;
import gleam.annotatorgui.actions.ActionConnectToDocservice;
import gleam.annotatorgui.actions.ActionShowExecutiveConnectDialog;
import gleam.annotatorgui.actions.ActionShowDocserviceConnectDialog;
import gleam.annotatorgui.gui.LogFrame;
import gleam.annotatorgui.gui.MainFrame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

/**
 * <p>
 * Main class to start client application.
 * </p>
 * 
 * <ul>
 * Annotator GUI startup parameters:
 * <li>Parameter name: <b>mode</b> Value: "pool" or "direct". Defines a mode
 * for Annotator GUI. (mandatory)</li>
 * 
 * <li>Parameter name: <b>sitecfg</b> Value: URL string. Defines a location of
 * GATE's site configuration file. (mandatory)</li>
 * 
 * <li>Parameter name: <b>autoconnect</b> Value: "true" or "false".</li>
 * 
 * <li>Parameter name: <b>load-plugins</b> Value: comma separated list of
 * plugin names to be loaded</li>
 * 
 * <li>Parameter name: <b>load-ann-schemas</b> Value: comma separated list of
 * URLs to load annotation schemas</li>
 * 
 * <li>Parameter name: <b>ontology-url</b> Value: URL string. Defines location
 * of ontology to be used.</li>
 * 
 * <li>Parameter name: <b>repository-name</b> Value: repository name.</li>
 * 
 * <li>Parameter name: <b>ontology-type</b> Value: string. Defines the type of
 * ontology to be used. Values defined by
 * {@link gleam.annotatorgui.Constants#ONTOLOGY_TYPE_RDFXML}
 * {@link gleam.annotatorgui.Constants#ONTOLOGY_TYPE_TURTLE}.
 * {@link gleam.annotatorgui.Constants#ONTOLOGY_TYPE_NTRIPPLES}. If ontology
 * type undefined, Annotator GUI will try to guess it from ontology URL. If it's
 * immpossible to guess, an ontology assumed to be RDFXML.</li>
 * 
 * <li>Parameter name: <b>debug</b> Value: "true" or "false"</li>
 * 
 * <li>Parameter name: <b>enable-oe</b> Value: "true" or "false"</li>
 * <li>Parameter name: <b>enable-application-log</b> Value: "true" or "false"</li>
 * 
 * Parameters only for "DIRECT" mode:<br>
 * <li>Parameter name: <b>docservice-url</b> Value: URL string. Defines
 * location of document service.</li>
 * 
 * <li>Parameter name: <b>doc-id</b> Value: persistent ID of the document in
 * document service.</li>
 * 
 * <li>Parameter name: <b>annotationset-name</b> Value: name of annotation set
 * to load. If defined, only annotation set with given name will be loaded. Use ""
 * (empty string) to define default annotation set. If parameter undefined, all
 * document annotation sets will be loaded.</li> * Parameters only for "POOL"
 * mode:<br>
 * <li>Parameter name: <b>executiveservice-url</b> Value: URI string. Defines
 * location of Executive Callback web service.</li>
 * 
 * <li>Parameter name: <b>user</b> Value: user name</li>
 * 
 * <li>Parameter name: <b>password</b> Value: user password</li>
 * 
 * <li>Parameter name: <b>can-cancel</b> Value: if 'true' user can cancel
 * task.</li>
 * </ul>
 * 
 * @author Andrey Shafirin
 */
public class AnnotatorGUI implements Constants {

    /** Debug flag */
    private static boolean DEBUG = true;

    private static Connection connection;

    private static List<AnnotatorGUIListener> listeners = new ArrayList<AnnotatorGUIListener>();

    private static Properties properties = new Properties();

    private static XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    private static XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

    /**
	 * Starts the client application.
	 */
    public static void main(String[] args) {
        LogFrame.getInstance();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.trim().startsWith(DEBUG_PARAMETER_NAME + "=")) {
                properties.put(DEBUG_PARAMETER_NAME, arg.trim().substring(DEBUG_PARAMETER_NAME.length() + 1).trim());
                if (properties.getProperty(DEBUG_PARAMETER_NAME).toLowerCase().equals(DEBUG_TRUE)) {
                    DEBUG = true;
                }
            } else if (arg.trim().startsWith(MODE_PARAMETER_NAME + "=")) {
                properties.put(MODE_PARAMETER_NAME, arg.trim().substring(MODE_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(AUTOCONNECT_PARAMETER_NAME + "=")) {
                properties.put(AUTOCONNECT_PARAMETER_NAME, arg.trim().substring(AUTOCONNECT_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(SITE_CONFIG_URL_PARAMETER_NAME + "=")) {
                properties.put(SITE_CONFIG_URL_PARAMETER_NAME, arg.trim().substring(SITE_CONFIG_URL_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(LOAD_PLUGINS_PARAMETER_NAME + "=")) {
                properties.put(LOAD_PLUGINS_PARAMETER_NAME, arg.trim().substring(LOAD_PLUGINS_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(ONTOLOGY_URL_PARAMETER_NAME + "=")) {
                properties.put(ONTOLOGY_URL_PARAMETER_NAME, arg.trim().substring(ONTOLOGY_URL_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(REPOSITORY_PARAMETER_NAME + "=")) {
                properties.put(REPOSITORY_PARAMETER_NAME, arg.trim().substring(REPOSITORY_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(ONTOLOGY_TYPE_PARAMETER_NAME + "=")) {
                properties.put(ONTOLOGY_TYPE_PARAMETER_NAME, arg.trim().substring(ONTOLOGY_TYPE_PARAMETER_NAME.length() + 1).trim());
                if (!(properties.getProperty(ONTOLOGY_TYPE_PARAMETER_NAME).equals(ONTOLOGY_TYPE_RDFXML) || properties.getProperty(ONTOLOGY_TYPE_PARAMETER_NAME).equals(ONTOLOGY_TYPE_TURTLE) || properties.getProperty(ONTOLOGY_TYPE_PARAMETER_NAME).equals(ONTOLOGY_TYPE_NTRIPPLES))) System.out.println("WARNING! Unknown ontology type: '" + properties.getProperty(ONTOLOGY_TYPE_PARAMETER_NAME) + "' (Known types are: '" + ONTOLOGY_TYPE_RDFXML + "', '" + ONTOLOGY_TYPE_TURTLE + "', '" + ONTOLOGY_TYPE_NTRIPPLES + "')");
            } else if (arg.trim().startsWith(OWLIMSERVICE_URL_PARAMETER_NAME + "=")) {
                properties.put(OWLIMSERVICE_URL_PARAMETER_NAME, arg.trim().substring(OWLIMSERVICE_URL_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(DOCSERVICE_URL_PARAMETER_NAME + "=")) {
                properties.put(DOCSERVICE_URL_PARAMETER_NAME, arg.trim().substring(DOCSERVICE_URL_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(DOC_ID_PARAMETER_NAME + "=")) {
                properties.put(DOC_ID_PARAMETER_NAME, arg.trim().substring(DOC_ID_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(ANNSET_NAME_PARAMETER_NAME + "=")) {
                properties.put(ANNSET_NAME_PARAMETER_NAME, arg.trim().substring(ANNSET_NAME_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(EXECUTIVE_SERVICE_URL_PARAMETER_NAME + "=")) {
                properties.put(EXECUTIVE_SERVICE_URL_PARAMETER_NAME, arg.trim().substring(EXECUTIVE_SERVICE_URL_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(USER_ID_PARAMETER_NAME + "=")) {
                properties.put(USER_ID_PARAMETER_NAME, arg.trim().substring(USER_ID_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(USER_PASSWORD_PARAMETER_NAME + "=")) {
                properties.put(USER_PASSWORD_PARAMETER_NAME, arg.trim().substring(USER_PASSWORD_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(EXECUTIVE_PROXY_FACTORY_PARAMETER_NAME + "=")) {
                properties.put(EXECUTIVE_PROXY_FACTORY_PARAMETER_NAME, arg.trim().substring(EXECUTIVE_PROXY_FACTORY_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(LOAD_ANN_SCHEMAS_NAME + "=")) {
                properties.put(LOAD_ANN_SCHEMAS_NAME, arg.trim().substring(LOAD_ANN_SCHEMAS_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(SELECT_AS_PARAMETER_NAME + "=")) {
                properties.put(SELECT_AS_PARAMETER_NAME, arg.trim().substring(SELECT_AS_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(SELECT_ANN_TYPES_PARAMETER_NAME + "=")) {
                properties.put(SELECT_ANN_TYPES_PARAMETER_NAME, arg.trim().substring(SELECT_ANN_TYPES_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(ENABLE_ONTOLOGY_EDITOR_PARAMETER_NAME + "=")) {
                properties.put(ENABLE_ONTOLOGY_EDITOR_PARAMETER_NAME, arg.trim().substring(ENABLE_ONTOLOGY_EDITOR_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(CLASSES_TO_HIDE_PARAMETER_NAME + "=")) {
                properties.put(CLASSES_TO_HIDE_PARAMETER_NAME, arg.trim().substring(CLASSES_TO_HIDE_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(CLASSES_TO_SHOW_PARAMETER_NAME + "=")) {
                properties.put(CLASSES_TO_SHOW_PARAMETER_NAME, arg.trim().substring(CLASSES_TO_SHOW_PARAMETER_NAME.length() + 1).trim());
            } else if (arg.trim().startsWith(ENABLE_APPLICATION_LOG_PARAMETER_NAME + "=")) {
                properties.put(ENABLE_APPLICATION_LOG_PARAMETER_NAME, arg.trim().substring(ENABLE_APPLICATION_LOG_PARAMETER_NAME.length() + 1).trim());
            } else {
                System.out.println("WARNING! Unknown or undefined parameter: '" + arg.trim() + "'");
            }
        }
        System.out.println(startupParamsToString());
        if (properties.getProperty(MODE_PARAMETER_NAME) == null || (!(properties.getProperty(MODE_PARAMETER_NAME).toLowerCase().equals(POOL_MODE)) && !(properties.getProperty(MODE_PARAMETER_NAME).toLowerCase().equals(DIRECT_MODE)))) {
            String err = "Mandatory parameter '" + MODE_PARAMETER_NAME + "' must be defined and must have a value either '" + POOL_MODE + "' or '" + DIRECT_MODE + "'.\n\nApplication will exit.";
            System.out.println(err);
            JOptionPane.showMessageDialog(new JFrame(), err, "Error!", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
        if (properties.getProperty(SITE_CONFIG_URL_PARAMETER_NAME) == null || properties.getProperty(SITE_CONFIG_URL_PARAMETER_NAME).length() == 0) {
            String err = "Mandatory parameter '" + SITE_CONFIG_URL_PARAMETER_NAME + "' is missing.\n\nApplication will exit.";
            System.out.println(err);
            JOptionPane.showMessageDialog(new JFrame(), err, "Error!", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
        try {
            String context = System.getProperty(CONTEXT);
            if (context == null || "".equals(context)) {
                context = DEFAULT_CONTEXT;
            }
            String s = System.getProperty(GateConstants.GATE_HOME_PROPERTY_NAME);
            if (s == null || s.length() == 0) {
                File f = File.createTempFile("foo", "");
                String gateHome = f.getParent().toString() + context;
                f.delete();
                System.setProperty(GateConstants.GATE_HOME_PROPERTY_NAME, gateHome);
                f = new File(System.getProperty(GateConstants.GATE_HOME_PROPERTY_NAME));
                if (!f.exists()) {
                    f.mkdirs();
                }
            }
            s = System.getProperty(GateConstants.PLUGINS_HOME_PROPERTY_NAME);
            if (s == null || s.length() == 0) {
                System.setProperty(GateConstants.PLUGINS_HOME_PROPERTY_NAME, System.getProperty(GateConstants.GATE_HOME_PROPERTY_NAME) + "/plugins");
                File f = new File(System.getProperty(GateConstants.PLUGINS_HOME_PROPERTY_NAME));
                if (!f.exists()) {
                    f.mkdirs();
                }
            }
            s = System.getProperty(GateConstants.GATE_SITE_CONFIG_PROPERTY_NAME);
            if (s == null || s.length() == 0) {
                System.setProperty(GateConstants.GATE_SITE_CONFIG_PROPERTY_NAME, System.getProperty(GateConstants.GATE_HOME_PROPERTY_NAME) + "/gate.xml");
            }
            if (properties.getProperty(SITE_CONFIG_URL_PARAMETER_NAME) != null && properties.getProperty(SITE_CONFIG_URL_PARAMETER_NAME).length() > 0) {
                File f = new File(System.getProperty(GateConstants.GATE_SITE_CONFIG_PROPERTY_NAME));
                if (f.exists()) {
                    f.delete();
                }
                f.getParentFile().mkdirs();
                f.createNewFile();
                URL url = new URL(properties.getProperty(SITE_CONFIG_URL_PARAMETER_NAME));
                InputStream is = url.openStream();
                FileOutputStream fos = new FileOutputStream(f);
                int i = is.read();
                while (i != -1) {
                    fos.write(i);
                    i = is.read();
                }
                fos.close();
                is.close();
            }
            try {
                Gate.init();
                gate.Main.applyUserPreferences();
            } catch (Exception e) {
                e.printStackTrace();
            }
            s = BASE_PLUGIN_NAME + "," + properties.getProperty(LOAD_PLUGINS_PARAMETER_NAME);
            System.out.println("Loading plugins: " + s);
            loadPlugins(s, true);
            loadAnnotationSchemas(properties.getProperty(LOAD_ANN_SCHEMAS_NAME), true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        MainFrame.getInstance().setVisible(true);
        MainFrame.getInstance().pack();
        if (properties.getProperty(MODE_PARAMETER_NAME).toLowerCase().equals(DIRECT_MODE)) {
            if (properties.getProperty(AUTOCONNECT_PARAMETER_NAME, "").toLowerCase().equals(AUTOCONNECT_TRUE)) {
                if (properties.getProperty(DOC_ID_PARAMETER_NAME) == null || properties.getProperty(DOC_ID_PARAMETER_NAME).length() == 0) {
                    String err = "Can't autoconnect. A parameter '" + DOC_ID_PARAMETER_NAME + "' is missing.";
                    System.out.println(err);
                    JOptionPane.showMessageDialog(MainFrame.getInstance(), err, "Error!", JOptionPane.ERROR_MESSAGE);
                    ActionShowDocserviceConnectDialog.getInstance().actionPerformed(null);
                } else {
                    ActionConnectToDocservice.getInstance().actionPerformed(null);
                }
            } else {
                ActionShowDocserviceConnectDialog.getInstance().actionPerformed(null);
            }
        } else {
            if (properties.getProperty(AUTOCONNECT_PARAMETER_NAME, "").toLowerCase().equals(AUTOCONNECT_TRUE)) {
                if (properties.getProperty(USER_ID_PARAMETER_NAME) == null || properties.getProperty(USER_ID_PARAMETER_NAME).length() == 0) {
                    String err = "Can't autoconnect. A parameter '" + USER_ID_PARAMETER_NAME + "' is missing.";
                    System.out.println(err);
                    JOptionPane.showMessageDialog(MainFrame.getInstance(), err, "Error!", JOptionPane.ERROR_MESSAGE);
                    ActionShowExecutiveConnectDialog.getInstance().actionPerformed(null);
                } else {
                    ActionConnectToExecutive.getInstance().actionPerformed(null);
                }
            } else {
                ActionShowExecutiveConnectDialog.getInstance().actionPerformed(null);
            }
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    public static Properties getProperties() {
        return properties;
    }

    public static XMLInputFactory getXmlInputFactory() {
        return xmlInputFactory;
    }

    public static XMLOutputFactory getXmlOutputFactory() {
        return xmlOutputFactory;
    }

    /**
	 * Gets the instance of ontology
	 * 
	 * @param urlString
	 * @param ontologyType
	 * @param owlimServiceURL
	 * @param repositoryName
	 * @return
	 * @throws MalformedURLException
	 * @throws ResourceInstantiationException
	 */
    public static Ontology getOntology(String urlString, String ontologyType, String owlimServiceURL, String repositoryName) throws MalformedURLException, ResourceInstantiationException {
        URL ontologyUrl = null;
        if (urlString != null && urlString.trim().length() != 0) {
            ontologyUrl = new URL(urlString);
        } else {
            ontologyUrl = null;
        }
        String ontologyResourceClassName = null;
        FeatureMap fm = Factory.newFeatureMap();
        if (owlimServiceURL == null || owlimServiceURL.trim().length() == 0) {
            System.out.println("No owlimServiceURL specified, loading local ontology");
            if (ontologyUrl == null) {
                throw new ResourceInstantiationException("Ontology URL is required when not using ontology service");
            }
            ontologyResourceClassName = "gate.creole.ontology.owlim.OWLIMOntologyLR";
        } else {
            ontologyResourceClassName = "gate.creole.ontology.owlim.client.OWLIMServiceLR";
            if (repositoryName == null || repositoryName.trim().length() == 0) {
                throw new ResourceInstantiationException("Invalid RepositoryName");
            }
            fm.put("sesameRepositoryID", repositoryName);
            fm.put("owlimServiceURL", new URL(owlimServiceURL));
        }
        if (ontologyUrl != null) {
            if (ontologyType != null && ontologyType.length() > 0) {
                if (ontologyType.equalsIgnoreCase(ONTOLOGY_TYPE_RDFXML)) fm.put("rdfXmlUrl", ontologyUrl); else if (ontologyType.equalsIgnoreCase(ONTOLOGY_TYPE_TURTLE)) fm.put("turtleURL", ontologyUrl); else if (ontologyType.equalsIgnoreCase(ONTOLOGY_TYPE_NTRIPPLES)) fm.put("ntriplesURL", ontologyUrl); else fm.put("rdfXmlURL", ontologyUrl);
            } else {
                if (urlString.endsWith(".rdf") || urlString.endsWith(".rdfs") || urlString.endsWith(".xml")) fm.put("rdfXmlURL", ontologyUrl); else if (urlString.endsWith(".turtle")) fm.put("turtleURL", ontologyUrl); else if (urlString.endsWith(".nt")) fm.put("ntripplesURL", ontologyUrl); else fm.put("rdfXmlURL", ontologyUrl);
            }
        }
        String ontologyName = "Unknown Ontology";
        if (ontologyUrl != null) {
            String s = ontologyUrl.getPath();
            if (s != null && s.length() > 0) {
                ontologyName = s.substring(s.lastIndexOf("/") + 1);
            }
        }
        return (Ontology) Factory.createResource(ontologyResourceClassName, fm, Factory.newFeatureMap(), ontologyName);
    }

    public static void setConnection(Connection newConnection) throws AnnotatorGUIExeption {
        System.out.println("AGUI set connection " + newConnection.toString());
        if (AnnotatorGUI.getConnection() != null) {
            closeConnection();
        }
        AnnotatorGUI.connection = newConnection;
        System.out.println("AGUI status of connection " + getConnectionStatus());
        MainFrame.getInstance().updateAllStatuses();
        fireConnectionChanged();
    }

    /**
	 * Returns text representation for connection status of client application.
	 * 
	 * @return text representation of the application connection status
	 */
    public static String getConnectionStatus() {
        if (connection != null) {
            return connection.getConnectionStatus();
        } else {
            return "Not connected";
        }
    }

    /**
	 * Returns URL of the resource with a given name.*
	 * 
	 * @param resourceName
	 *            name
	 * @return URL of the resorce with a given name
	 */
    public static URL getResourceURL(String resourceName) {
        return AnnotatorGUI.class.getResource("resource/" + resourceName);
    }

    /**
	 * Returns path to the application resources.
	 * 
	 * @return path to the application resources
	 */
    public static String getResourcePath() {
        return AnnotatorGUI.class.getResource("resource").toString();
    }

    public static ImageIcon createIcon(String imageName) {
        URL imageURL = getResourceURL(imageName);
        if (imageURL == null) {
            System.err.println("Resource not found: " + imageName);
            return null;
        } else {
            return new ImageIcon(imageURL);
        }
    }

    /**
	 * Adds given listener to the list of listeners if this listener doesn't
	 * exists.
	 * 
	 * @param lsnr
	 *            listener to add
	 */
    public static boolean addAnnotatorGUIListener(AnnotatorGUIListener lsnr) {
        if (listeners.contains(lsnr)) return false;
        listeners.add(lsnr);
        return true;
    }

    /**
	 * Removes given listener to the list of listeners if this listener doesn't
	 * exists.
	 * 
	 * @param lsnr
	 *            listener to remove
	 */
    public static boolean removeAnnotatorGUIListener(AnnotatorGUIListener lsnr) {
        if (listeners.contains(lsnr)) {
            listeners.remove(lsnr);
            return true;
        }
        return false;
    }

    public static boolean closeConnection() throws AnnotatorGUIExeption {
        if (connection == null) return true;
        if (checkDocOnClose()) {
            connection.cleanup();
            connection = null;
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkDocOnClose() {
        if (AnnotatorGUI.getConnection() == null || AnnotatorGUI.getConnection().getDocument() == null || !AnnotatorGUI.getConnection().isDocumentAnnotationsModified()) return true; else {
            int i = JOptionPane.showConfirmDialog(MainFrame.getInstance(), "Document has been changed\nDo you want to save document?", "Document has been changed", JOptionPane.YES_NO_CANCEL_OPTION);
            switch(i) {
                case JOptionPane.YES_OPTION:
                    try {
                        AnnotatorGUI.getConnection().saveDocument();
                    } catch (AnnotatorGUIExeption ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(MainFrame.getInstance(), ex.getMessage() + ((ex.getCause() == null) ? "" : "\n\n" + ex.getCause().getMessage()), "Error!", JOptionPane.ERROR_MESSAGE);
                    }
                    return true;
                case JOptionPane.NO_OPTION:
                    try {
                        if (AnnotatorGUI.getConnection() instanceof ExecutiveConnection) {
                            ((ExecutiveConnection) AnnotatorGUI.getConnection()).getCurrentAnnotatorTask().releaseLock();
                        } else if (AnnotatorGUI.getConnection() instanceof DocserviceConnection) {
                            ((DocserviceConnection) AnnotatorGUI.getConnection()).cleanup();
                        } else {
                            JOptionPane.showMessageDialog(MainFrame.getInstance(), "Internal error occured while saving document. Unknown type of connection.", "Error!", JOptionPane.ERROR_MESSAGE);
                        }
                        return true;
                    } catch (AnnotatorGUIExeption ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(MainFrame.getInstance(), ex.getMessage() + ((ex.getCause() == null) ? "" : "\n\n" + ex.getCause().getMessage()), "Error!", JOptionPane.ERROR_MESSAGE);
                    }
                    return true;
                case JOptionPane.CANCEL_OPTION:
                    return false;
                default:
                    return true;
            }
        }
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    private static void fireConnectionChanged() {
        if (listeners.size() == 0) return;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                Object[] lsnrs = listeners.toArray();
                for (int i = 0; i < lsnrs.length; i++) {
                    AnnotatorGUIListener l = (AnnotatorGUIListener) lsnrs[i];
                    l.connectionChanged(connection);
                }
            }
        });
    }

    public static void highlightAnnotations(AnnotationSetsView asv) {
        String selectedAS = getProperties().getProperty(SELECT_AS_PARAMETER_NAME);
        System.out.println("DEBUG: AnnotatorGUI.highlightAnnotations(). selected AS '" + selectedAS + "'");
        if (selectedAS != null) {
            AnnotationSet as = null;
            if (selectedAS.length() == 0) {
                as = asv.getDocument().getAnnotations();
            } else {
                if (asv.getDocument().getAnnotationSetNames().contains(selectedAS)) as = asv.getDocument().getAnnotations(selectedAS);
            }
            if (as != null) {
                System.out.println("DEBUG: AnnotatorGUI.highlightAnnotations(). as found");
                String annTypesToSelect = getProperties().getProperty(SELECT_ANN_TYPES_PARAMETER_NAME);
                if (annTypesToSelect != null && annTypesToSelect.length() > 0) {
                    StringTokenizer stok = new StringTokenizer(annTypesToSelect, ",");
                    while (stok.hasMoreTokens()) {
                        String annType = stok.nextToken();
                        if (as.getAllTypes().contains(annType)) asv.setTypeSelected(as.getName(), annType, true);
                    }
                } else {
                    for (String annType : as.getAllTypes()) asv.setTypeSelected(as.getName(), annType, true);
                }
            }
        }
    }

    public static void loadAnnotationSchemas(String urls, boolean unloadOtherShemas) {
        if (urls != null && urls.length() > 0) {
            System.out.println("Loading annotation schemas: " + urls);
            StringTokenizer stok = new StringTokenizer(urls, ",");
            List<String> schemaLocations = new ArrayList<String>();
            while (stok.hasMoreTokens()) {
                String tok = stok.nextToken().trim();
                if (tok.length() > 0) {
                    schemaLocations.add(tok);
                }
            }
            List<LanguageResource> loadedSchemas = Gate.getCreoleRegister().getLrInstances("gate.creole.AnnotationSchema");
            if (loadedSchemas == null || loadedSchemas.size() == 0) {
                System.out.println("List<LanguageResource> loadedSchemas IS EMPTY ");
            }
            for (LanguageResource schema : loadedSchemas) {
                URL schemaURL = ((gate.creole.AnnotationSchema) schema).getXmlFileUrl();
                System.out.println("Found loaded schema: " + schemaURL);
                if (schemaLocations.contains(schemaURL.toString())) {
                    System.out.println("Annotation schema already loaded. URL: " + schemaURL.toString());
                } else {
                    if (unloadOtherShemas) {
                        System.out.println("Unloading annotation schema: " + schemaURL.toString());
                        Factory.deleteResource(schema);
                    }
                }
            }
            loadedSchemas = Gate.getCreoleRegister().getLrInstances("gate.creole.AnnotationSchema");
            List<String> loadedSchemaLocations = new ArrayList<String>();
            for (LanguageResource schema : loadedSchemas) {
                loadedSchemaLocations.add(((gate.creole.AnnotationSchema) schema).getXmlFileUrl().toString());
            }
            for (String schemaLoc : schemaLocations) {
                if (loadedSchemaLocations.contains(schemaLoc)) continue;
                System.out.println("Loading annotation schema: " + schemaLoc);
                try {
                    URL url = new URL(schemaLoc);
                    FeatureMap fm = Factory.newFeatureMap();
                    fm.put("xmlFileUrl", url);
                    Factory.createResource("gate.creole.AnnotationSchema", fm);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void loadPlugins(String plugins, boolean unloadOtherPlugins) {
        if (plugins == null || plugins.length() == 0) {
            System.out.println("No plugins specified");
            return;
        }
        StringTokenizer stok = new StringTokenizer(plugins, ",");
        Set<URL> pluginsSet = new HashSet<URL>();
        while (stok.hasMoreTokens()) {
            String tok = stok.nextToken().trim();
            if (tok.length() > 0) {
                URL url = AnnotatorGUI.class.getClassLoader().getResource(tok + "/creole.xml");
                if (url == null) {
                    System.out.println("Can't find plugin: " + tok);
                } else {
                    try {
                        url = new URL(url, ".");
                        pluginsSet.add(url);
                    } catch (MalformedURLException mue) {
                        mue.printStackTrace();
                    }
                }
            }
        }
        System.out.println("Loading plugins " + pluginsSet);
        if (unloadOtherPlugins) {
            Set<URL> loadedPlugins = new HashSet<URL>(Gate.getCreoleRegister().getDirectories());
            for (URL u : loadedPlugins) {
                if (!pluginsSet.contains(u)) {
                    Gate.getCreoleRegister().removeDirectory(u);
                }
            }
        }
        for (URL plugin : pluginsSet) {
            try {
                Gate.getCreoleRegister().registerDirectories(plugin);
            } catch (GateException ge) {
                System.err.println("Error loading plugin " + plugin);
                ge.printStackTrace();
            }
        }
    }

    private static String startupParamsToString() {
        StringBuffer sb = new StringBuffer("Annotator GUI startup parameters:\n");
        sb.append("------------------------------\n");
        for (Object propName : properties.keySet()) {
            sb.append(propName.toString() + "=" + properties.getProperty((String) propName) + "\n");
            System.out.println();
        }
        sb.append("------------------------------\n");
        return sb.toString();
    }
}
