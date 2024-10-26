package gate.util.persistence;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.XStream12FieldKeySorter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.*;
import java.io.*;
import java.net.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import javax.xml.stream.*;
import org.apache.log4j.Logger;
import gate.*;
import gate.creole.*;
import gate.event.ProgressListener;
import gate.event.StatusListener;
import gate.persist.GateAwareObjectInputStream;
import gate.persist.PersistenceException;
import gate.util.*;

/**
 * This class provides utility methods for saving resources through
 * serialisation via static methods.
 *
 * It now supports both native and xml serialization.
 */
public class PersistenceManager {

    private static final boolean DEBUG = false;

    /**
   * A reference to an object; it uses the identity hashcode and the
   * equals defined by object identity. These values will be used as
   * keys in the {link #existingPersistentReplacements} map.
   */
    protected static class ObjectHolder {

        ObjectHolder(Object target) {
            this.target = target;
        }

        public int hashCode() {
            return System.identityHashCode(target);
        }

        public boolean equals(Object obj) {
            if (obj instanceof ObjectHolder) return ((ObjectHolder) obj).target == this.target; else return false;
        }

        public Object getTarget() {
            return target;
        }

        private Object target;
    }

    /**
   * This class is used as a marker for types that should NOT be
   * serialised when saving the state of a gate object. Registering this
   * type as the persistent equivalent for a specific class (via
   * {@link PersistenceManager#registerPersistentEquivalent(Class , Class)})
   * effectively stops all values of the specified type from being
   * serialised.
   *
   * Maps that contain values that should not be serialised will have
   * that entry removed. In any other places where such values occur
   * they will be replaced by null after deserialisation.
   */
    public static class SlashDevSlashNull implements Persistence {

        /**
     * Does nothing
     */
        public void extractDataFromSource(Object source) throws PersistenceException {
        }

        /**
     * Returns null
     */
        public Object createObject() throws PersistenceException, ResourceInstantiationException {
            return null;
        }

        static final long serialVersionUID = -8665414981783519937L;
    }

    /**
   * URLs get upset when serialised and deserialised so we need to
   * convert them to strings for storage. In the case of
   * &quot;file:&quot; URLs the relative path to the persistence file
   * will actually be stored, except when the URL refers to a resource
   * within the current GATE home directory in which case the relative path
   * to the GATE home directory will be stored. If the property 
   * gate.user.resourceshom is set to a directory path and the URL refers 
   * to a resource inside this directory, the relative path to this directory
   * will be stored. If resources are stored relative to gate home or
   * resources home, a warning will also be logged.
   */
    public static class URLHolder implements Persistence {

        /**
     * Populates this Persistence with the data that needs to be stored
     * from the original source object.
     */
        public void extractDataFromSource(Object source) throws PersistenceException {
            final Logger logger = Logger.getLogger(URLHolder.class);
            try {
                URL url = (URL) source;
                if (url.getProtocol().equals("file")) {
                    try {
                        String pathMarker = relativePathMarker;
                        File gateHomePath = getGateHomePath();
                        URL urlPersistenceFilePath = getCanonicalFileIfPossible(currentPersistenceFile()).toURI().toURL();
                        File urlPath = getCanonicalFileIfPossible(Files.fileFromURL(url));
                        url = urlPath.toURI().toURL();
                        if (currentUseGateHome() || currentWarnAboutGateHome()) {
                            String persistenceFilePathName = urlPersistenceFilePath.getPath();
                            String gateHomePathName = gateHomePath.getPath();
                            String urlPathName = urlPath.getPath();
                            String resourceshomePathName = null;
                            if (getResourceshomePath() != null) {
                                resourceshomePathName = getResourceshomePath().getPath();
                            }
                            if (!persistenceFilePathName.startsWith(gateHomePathName) && urlPathName.startsWith(gateHomePathName)) {
                                if (currentWarnAboutGateHome()) {
                                    if (!currentHaveWarnedAboutGateHome().getValue()) {
                                        logger.warn("\nYour application is using some of the resources/plugins " + "distributed with GATE, and may not work as expected " + "with different versions of GATE. You should consider " + "making private local copies of the plug-ins, and " + "distributing those with your application.");
                                        currentHaveWarnedAboutGateHome().setValue(true);
                                    }
                                    logger.warn("GATE resource referenced: " + url);
                                }
                                if (currentUseGateHome()) {
                                    pathMarker = gatehomePathMarker;
                                }
                            } else if (resourceshomePathName != null && !persistenceFilePathName.startsWith(resourceshomePathName) && urlPathName.startsWith(resourceshomePathName)) {
                                if (currentWarnAboutGateHome()) {
                                    if (!currentHaveWarnedAboutResourceshome().getValue()) {
                                        logger.warn("\nYour application is using resources from your project " + "path at " + getResourceshomePath() + ". Restoring the application " + "will only work if the same project path is set.");
                                        currentHaveWarnedAboutResourceshome().setValue(true);
                                    }
                                    logger.warn("Resource referenced: " + url);
                                }
                                if (currentUseGateHome()) {
                                    pathMarker = resourceshomePathMarker;
                                }
                            }
                        }
                        if (pathMarker.equals(relativePathMarker)) {
                            urlString = pathMarker + getRelativePath(urlPersistenceFilePath, url);
                        } else if (pathMarker.equals(gatehomePathMarker)) {
                            urlString = pathMarker + getRelativePath(gateHomePath.toURI().toURL(), url);
                        } else if (pathMarker.equals(resourceshomePathMarker)) {
                            urlString = pathMarker + getRelativePath(getResourceshomePath().toURI().toURL(), url);
                        } else {
                            throw new GateRuntimeException("Unexpected error when persisting URL " + url);
                        }
                    } catch (MalformedURLException mue) {
                        urlString = ((URL) source).toExternalForm();
                    }
                } else {
                    urlString = ((URL) source).toExternalForm();
                }
            } catch (ClassCastException cce) {
                throw new PersistenceException(cce);
            }
        }

        /**
     * Creates a new object from the data contained. This new object is
     * supposed to be a copy for the original object used as source for
     * data extraction.
     */
        public Object createObject() throws PersistenceException {
            try {
                if (urlString.startsWith(relativePathMarker)) {
                    URL context = currentPersistenceURL();
                    return new URL(context, urlString.substring(relativePathMarker.length()));
                } else if (urlString.startsWith(gatehomePathMarker)) {
                    URL gatehome = getCanonicalFileIfPossible(getGateHomePath()).toURI().toURL();
                    return new URL(gatehome, urlString.substring(gatehomePathMarker.length()));
                } else if (urlString.startsWith(gatepluginsPathMarker)) {
                    URL gateplugins = Gate.getPluginsHome().toURI().toURL();
                    return new URL(gateplugins, urlString.substring(gatepluginsPathMarker.length()));
                } else if (urlString.startsWith(resourceshomePathMarker)) {
                    if (getResourceshomePath() == null) {
                        throw new GateRuntimeException("Cannot restore URL " + urlString + "property " + resourceshomePropertyName + " is not set");
                    }
                    URL resourceshomeurl = getResourceshomePath().toURI().toURL();
                    return new URL(resourceshomeurl, urlString.substring(resourceshomePathMarker.length()));
                } else if (urlString.startsWith(syspropMarker)) {
                    String urlRestString = urlString.substring(syspropMarker.length());
                    int dollarindex = urlRestString.indexOf("$");
                    if (dollarindex > 0) {
                        String syspropname = urlRestString.substring(0, dollarindex);
                        String propvalue = System.getProperty(syspropname);
                        if (propvalue == null) {
                            throw new PersistenceException("Property '" + syspropname + "' is null in " + urlString);
                        }
                        URL propuri = (new File(propvalue)).toURI().toURL();
                        if (dollarindex == urlRestString.length()) {
                            return propuri;
                        } else {
                            return new URL(propuri, urlRestString.substring(dollarindex + 1));
                        }
                    } else if (dollarindex == 0) {
                        throw new PersistenceException("No property name after '" + syspropMarker + "' in " + urlString);
                    } else {
                        throw new PersistenceException("No ending $ after '" + syspropMarker + "' in " + urlString);
                    }
                } else {
                    return new URL(urlString);
                }
            } catch (MalformedURLException mue) {
                throw new PersistenceException(mue);
            }
        }

        public File getGateHomePath() {
            if (gatehomePath != null) {
                return gatehomePath;
            } else {
                gatehomePath = getCanonicalFileIfPossible(Gate.getGateHome());
                return gatehomePath;
            }
        }

        public File getResourceshomePath() {
            if (haveResourceshomePath == null) {
                String resourceshomeString = (String) System.getProperty(resourceshomePropertyName);
                if (resourceshomeString == null) {
                    haveResourceshomePath = false;
                    return null;
                }
                resourceshomePath = new File(resourceshomeString);
                resourceshomePath = getCanonicalFileIfPossible(resourceshomePath);
                haveResourceshomePath = true;
                System.out.println("Found project home path " + resourceshomePath);
                return resourceshomePath;
            } else if (haveResourceshomePath) {
                return resourceshomePath;
            } else {
                return null;
            }
        }

        public File getCanonicalFileIfPossible(File file) {
            File tmp = file;
            try {
                tmp = tmp.getCanonicalFile();
            } catch (IOException ex) {
            }
            return tmp;
        }

        String urlString;

        /**
     * This string will be used to start the serialisation of URL that
     * represent relative paths.
     */
        private static final String relativePathMarker = "$relpath$";

        private static final String gatehomePathMarker = "$gatehome$";

        private static final String gatepluginsPathMarker = "$gateplugins$";

        private static final String syspropMarker = "$sysprop:";

        private static final String resourceshomePathMarker = "$resourceshome$";

        private static final String resourceshomePropertyName = "gate.user.resourceshome";

        private static File resourceshomePath = null;

        private static Boolean haveResourceshomePath = null;

        private static File gatehomePath = null;

        static final long serialVersionUID = 7943459208429026229L;
    }

    public static class ClassComparator implements Comparator {

        /**
     * Compares two {@link Class} values in terms of specificity; the
     * more specific class is said to be &quot;smaller&quot; than the
     * more generic one hence the {@link Object} class is the
     * &quot;largest&quot; possible class. When two classes are not
     * comparable (i.e. not assignable from each other) in either
     * direction a NotComparableException will be thrown. both input
     * objects should be Class values otherwise a
     * {@link ClassCastException} will be thrown.
     *
     */
        public int compare(Object o1, Object o2) {
            Class c1 = (Class) o1;
            Class c2 = (Class) o2;
            if (c1.equals(c2)) return 0;
            if (c1.isAssignableFrom(c2)) return 1;
            if (c2.isAssignableFrom(c1)) return -1;
            throw new NotComparableException();
        }
    }

    /**
   * Thrown by a comparator when the values provided for comparison are
   * not comparable.
   */
    public static class NotComparableException extends RuntimeException {

        public NotComparableException(String message) {
            super(message);
        }

        public NotComparableException() {
        }
    }

    /**
   * Recursively traverses the provided object and replaces it and all
   * its contents with the appropriate persistent equivalent classes.
   *
   * @param target the object to be analysed and translated into a
   *          persistent equivalent.
   * @return the persistent equivalent value for the provided target
   */
    public static Serializable getPersistentRepresentation(Object target) throws PersistenceException {
        if (target == null) return null;
        Persistence res = (Persistence) existingPersistentReplacements.get().getFirst().get(new ObjectHolder(target));
        if (res != null) return res;
        Class type = target.getClass();
        Class newType = getMostSpecificPersistentType(type);
        if (newType == null) {
            if (target instanceof Serializable) return (Serializable) target; else throw new PersistenceException("Could not find a serialisable replacement for " + type);
        }
        try {
            res = (Persistence) newType.newInstance();
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
        if (target instanceof NameBearer) {
            StatusListener sListener = (StatusListener) Gate.getListeners().get("gate.event.StatusListener");
            if (sListener != null) {
                sListener.statusChanged("Storing " + ((NameBearer) target).getName());
            }
        }
        res.extractDataFromSource(target);
        existingPersistentReplacements.get().getFirst().put(new ObjectHolder(target), res);
        return res;
    }

    public static Object getTransientRepresentation(Object target) throws PersistenceException, ResourceInstantiationException {
        if (target == null || target instanceof SlashDevSlashNull) return null;
        if (target instanceof Persistence) {
            Object resultKey = new ObjectHolder(target);
            Object result = existingTransientValues.get().getFirst().get(resultKey);
            if (result != null) return result;
            result = ((Persistence) target).createObject();
            existingTransientValues.get().getFirst().put(resultKey, result);
            return result;
        } else return target;
    }

    /**
   * Finds the most specific persistent replacement type for a given
   * class. Look for a type that has a registered persistent equivalent
   * starting from the provided class continuing with its superclass and
   * implemented interfaces and their superclasses and implemented
   * interfaces and so on until a type is found. Classes are considered
   * to be more specific than interfaces and in situations of ambiguity
   * the most specific types are considered to be the ones that don't
   * belong to either java or GATE followed by the ones that belong to
   * GATE and followed by the ones that belong to java.
   *
   * E.g. if there are registered persistent types for
   * {@link gate.Resource} and for {@link gate.LanguageResource} than
   * such a request for a {@link gate.Document} will yield the
   * registered type for {@link gate.LanguageResource}.
   */
    protected static Class getMostSpecificPersistentType(Class type) {
        List expansionSet = new ArrayList();
        expansionSet.add(type);
        List userInterfaces = new ArrayList();
        List gateInterfaces = new ArrayList();
        List javaInterfaces = new ArrayList();
        while (!expansionSet.isEmpty()) {
            Iterator typesIter = expansionSet.iterator();
            while (typesIter.hasNext()) {
                Class result = (Class) persistentReplacementTypes.get(typesIter.next());
                if (result != null) {
                    return result;
                }
            }
            if (type != null) type = type.getSuperclass();
            userInterfaces.clear();
            gateInterfaces.clear();
            javaInterfaces.clear();
            typesIter = expansionSet.iterator();
            while (typesIter.hasNext()) {
                Class aType = (Class) typesIter.next();
                Class[] interfaces = aType.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    Class anIterf = interfaces[i];
                    String interfType = anIterf.getName();
                    if (interfType.startsWith("java")) {
                        javaInterfaces.add(anIterf);
                    } else if (interfType.startsWith("gate")) {
                        gateInterfaces.add(anIterf);
                    } else userInterfaces.add(anIterf);
                }
            }
            expansionSet.clear();
            if (type != null) expansionSet.add(type);
            expansionSet.addAll(userInterfaces);
            expansionSet.addAll(gateInterfaces);
            expansionSet.addAll(javaInterfaces);
        }
        return null;
    }

    /**
   * Calculates the relative path for a file: URL starting from a given
   * context which is also a file: URL.
   *
   * @param context the URL to be used as context.
   * @param target the URL for which the relative path is computed.
   * @return a String value representing the relative path. Constructing
   *         a URL from the context URL and the relative path should
   *         result in the target URL.
   */
    public static String getRelativePath(URL context, URL target) {
        if (context.getProtocol().equals("file") && target.getProtocol().equals("file")) {
            File contextFile = Files.fileFromURL(context);
            File targetFile = Files.fileFromURL(target);
            if (context.toExternalForm().endsWith("/")) {
                contextFile = new File(contextFile, "__dummy__");
            }
            List targetPathComponents = new ArrayList();
            File aFile = targetFile.getParentFile();
            while (aFile != null) {
                targetPathComponents.add(0, aFile);
                aFile = aFile.getParentFile();
            }
            List contextPathComponents = new ArrayList();
            aFile = contextFile.getParentFile();
            while (aFile != null) {
                contextPathComponents.add(0, aFile);
                aFile = aFile.getParentFile();
            }
            int commonPathElements = 0;
            while (commonPathElements < targetPathComponents.size() && commonPathElements < contextPathComponents.size() && targetPathComponents.get(commonPathElements).equals(contextPathComponents.get(commonPathElements))) commonPathElements++;
            String relativePath = "";
            for (int i = commonPathElements; i < contextPathComponents.size(); i++) {
                if (relativePath.length() == 0) relativePath += ".."; else relativePath += "/..";
            }
            for (int i = commonPathElements; i < targetPathComponents.size(); i++) {
                String aDirName = ((File) targetPathComponents.get(i)).getName();
                if (aDirName.length() == 0) {
                    aDirName = ((File) targetPathComponents.get(i)).getAbsolutePath();
                    if (aDirName.endsWith(File.separator)) {
                        aDirName = aDirName.substring(0, aDirName.length() - File.separator.length());
                    }
                }
                if (relativePath.length() == 0) {
                    relativePath += aDirName;
                } else {
                    relativePath += "/" + aDirName;
                }
            }
            if (relativePath.length() == 0) {
                relativePath += targetFile.getName();
            } else {
                relativePath += "/" + targetFile.getName();
            }
            if (target.toExternalForm().endsWith("/")) {
                relativePath += "/";
            }
            try {
                URI relativeURI = new URI(null, null, relativePath, null, null);
                return relativeURI.getRawPath();
            } catch (URISyntaxException use) {
                throw new GateRuntimeException("Failed to generate relative path " + "between context: " + context + " and target: " + target, use);
            }
        } else {
            throw new GateRuntimeException("Both the target and the context URLs " + "need to be \"file:\" URLs!");
        }
    }

    public static void saveObjectToFile(Object obj, File file) throws PersistenceException, IOException {
        saveObjectToFile(obj, file, false, false);
    }

    public static void saveObjectToFile(Object obj, File file, boolean usegatehome, boolean warnaboutgatehome) throws PersistenceException, IOException {
        ProgressListener pListener = (ProgressListener) Gate.getListeners().get("gate.event.ProgressListener");
        StatusListener sListener = (gate.event.StatusListener) Gate.getListeners().get("gate.event.StatusListener");
        long startTime = System.currentTimeMillis();
        if (pListener != null) pListener.progressChanged(0);
        ObjectOutputStream oos = null;
        com.thoughtworks.xstream.XStream xstream = null;
        HierarchicalStreamWriter writer = null;
        warnAboutGateHome.get().addFirst(warnaboutgatehome);
        useGateHome.get().addFirst(usegatehome);
        startPersistingTo(file);
        try {
            if (Gate.getUseXMLSerialization()) {
                xstream = new XStream(new Sun14ReflectionProvider(new FieldDictionary(new XStream12FieldKeySorter())), new StaxDriver(new XStream11NameCoder())) {

                    protected boolean useXStream11XmlFriendlyMapper() {
                        return true;
                    }
                };
                FileWriter fileWriter = new FileWriter(file);
                writer = new PrettyPrintWriter(fileWriter, new XmlFriendlyNameCoder("-", "_"));
            } else {
                oos = new ObjectOutputStream(new FileOutputStream(file));
            }
            List urlList = new ArrayList(Gate.getCreoleRegister().getDirectories());
            Object persistentList = getPersistentRepresentation(urlList);
            Object persistentObject = getPersistentRepresentation(obj);
            if (Gate.getUseXMLSerialization()) {
                GateApplication gateApplication = new GateApplication();
                gateApplication.urlList = persistentList;
                gateApplication.application = persistentObject;
                xstream.marshal(gateApplication, writer);
            } else {
                oos.writeObject(persistentList);
                oos.writeObject(persistentObject);
            }
        } finally {
            finishedPersisting();
            if (oos != null) {
                oos.flush();
                oos.close();
            }
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            long endTime = System.currentTimeMillis();
            if (sListener != null) sListener.statusChanged("Storing completed in " + NumberFormat.getInstance().format((double) (endTime - startTime) / 1000) + " seconds");
            if (pListener != null) pListener.processFinished();
        }
    }

    /**
   * Set up the thread-local state for a new persistence run.
   */
    private static void startPersistingTo(File file) {
        haveWarnedAboutGateHome.get().addFirst(new BooleanFlag(false));
        haveWarnedAboutResourceshome.get().addFirst(new BooleanFlag(false));
        persistenceFile.get().addFirst(file);
        existingPersistentReplacements.get().addFirst(new HashMap());
    }

    /**
   * Get the file currently being saved by this thread.
   */
    private static File currentPersistenceFile() {
        return persistenceFile.get().getFirst();
    }

    private static Boolean currentWarnAboutGateHome() {
        return warnAboutGateHome.get().getFirst();
    }

    private static Boolean currentUseGateHome() {
        return useGateHome.get().getFirst();
    }

    private static BooleanFlag currentHaveWarnedAboutGateHome() {
        return haveWarnedAboutGateHome.get().getFirst();
    }

    private static BooleanFlag currentHaveWarnedAboutResourceshome() {
        return haveWarnedAboutResourceshome.get().getFirst();
    }

    /**
   * Clean up the thread-local state for the current persistence run.
   */
    private static void finishedPersisting() {
        persistenceFile.get().removeFirst();
        if (persistenceFile.get().isEmpty()) {
            persistenceFile.remove();
        }
        existingPersistentReplacements.get().removeFirst();
        if (existingPersistentReplacements.get().isEmpty()) {
            existingPersistentReplacements.remove();
        }
    }

    public static Object loadObjectFromFile(File file) throws PersistenceException, IOException, ResourceInstantiationException {
        return loadObjectFromUrl(file.toURI().toURL());
    }

    public static Object loadObjectFromUrl(URL url) throws PersistenceException, IOException, ResourceInstantiationException {
        ProgressListener pListener = (ProgressListener) Gate.getListeners().get("gate.event.ProgressListener");
        StatusListener sListener = (gate.event.StatusListener) Gate.getListeners().get("gate.event.StatusListener");
        if (pListener != null) pListener.progressChanged(0);
        startLoadingFrom(url);
        InputStream rawStream = null;
        try {
            long startTime = System.currentTimeMillis();
            boolean xmlStream = isXmlApplicationFile(url);
            ObjectInputStream ois = null;
            HierarchicalStreamReader reader = null;
            XStream xstream = null;
            if (xmlStream) {
                Reader inputReader = new InputStreamReader(rawStream = url.openStream());
                try {
                    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                    XMLStreamReader xsr = inputFactory.createXMLStreamReader(url.toExternalForm(), inputReader);
                    reader = new StaxReader(new QNameMap(), xsr);
                } catch (XMLStreamException xse) {
                    inputReader.close();
                    throw new PersistenceException("Error creating reader", xse);
                }
                xstream = new XStream(new StaxDriver(new XStream11NameCoder())) {

                    protected boolean useXStream11XmlFriendlyMapper() {
                        return true;
                    }
                };
                xstream.setClassLoader(Gate.getClassLoader());
                ois = xstream.createObjectInputStream(reader);
            } else {
                ois = new GateAwareObjectInputStream(url.openStream());
            }
            Object res = null;
            try {
                Iterator urlIter = ((Collection) getTransientRepresentation(ois.readObject())).iterator();
                while (urlIter.hasNext()) {
                    URL anUrl = (URL) urlIter.next();
                    try {
                        Gate.getCreoleRegister().registerDirectories(anUrl);
                    } catch (GateException ge) {
                        Err.prln("Could not reload creole directory " + anUrl.toExternalForm());
                        ge.printStackTrace(Err.getPrintWriter());
                    }
                }
                res = ois.readObject();
                clearCurrentTransients();
                res = getTransientRepresentation(res);
                long endTime = System.currentTimeMillis();
                if (sListener != null) sListener.statusChanged("Loading completed in " + NumberFormat.getInstance().format((double) (endTime - startTime) / 1000) + " seconds");
                return res;
            } catch (ResourceInstantiationException rie) {
                if (sListener != null) sListener.statusChanged("Failure during instantiation of resources.");
                throw rie;
            } catch (PersistenceException pe) {
                if (sListener != null) sListener.statusChanged("Failure during persistence operations.");
                throw pe;
            } catch (Exception ex) {
                if (sListener != null) sListener.statusChanged("Loading failed!");
                throw new PersistenceException(ex);
            } finally {
                if (ois != null) ois.close();
                if (reader != null) reader.close();
            }
        } finally {
            if (rawStream != null) rawStream.close();
            finishedLoading();
            if (pListener != null) pListener.processFinished();
        }
    }

    /**
   * Set up the thread-local state for the current loading run.
   */
    private static void startLoadingFrom(URL url) {
        persistenceURL.get().addFirst(url);
        existingTransientValues.get().addFirst(new HashMap());
    }

    /**
   * Clear the current list of transient replacements without
   * popping them off the stack.
   */
    private static void clearCurrentTransients() {
        existingTransientValues.get().getFirst().clear();
    }

    /**
   * Get the URL currently being loaded by this thread.
   */
    private static URL currentPersistenceURL() {
        return persistenceURL.get().getFirst();
    }

    /**
   * Clean up the thread-local state at the end of a loading run.
   */
    private static void finishedLoading() {
        persistenceURL.get().removeFirst();
        if (persistenceURL.get().isEmpty()) {
            persistenceURL.remove();
        }
        existingTransientValues.get().removeFirst();
        if (existingTransientValues.get().isEmpty()) {
            existingTransientValues.remove();
        }
    }

    /**
   * Determine whether the URL contains a GATE application serialized
   * using XML.
   *
   * @param url The URL to check.
   * @return true if the URL refers to an xml serialized application,
   *         false otherwise.
   */
    private static boolean isXmlApplicationFile(URL url) throws java.io.IOException {
        if (DEBUG) {
            System.out.println("Checking whether file is xml");
        }
        String firstLine;
        BufferedReader fileReader = null;
        try {
            fileReader = new BomStrippingInputStreamReader(url.openStream());
            firstLine = fileReader.readLine();
        } finally {
            if (fileReader != null) fileReader.close();
        }
        if (firstLine == null) {
            return false;
        }
        for (String startOfXml : STARTOFXMLAPPLICATIONFILES) {
            if (firstLine.length() >= startOfXml.length() && firstLine.substring(0, startOfXml.length()).equals(startOfXml)) {
                if (DEBUG) {
                    System.out.println("isXMLApplicationFile = true");
                }
                return true;
            }
        }
        if (DEBUG) {
            System.out.println("isXMLApplicationFile = false");
        }
        return false;
    }

    private static final String[] STARTOFXMLAPPLICATIONFILES = { "<gate.util.persistence.GateApplication>", "<?xml", "<!DOCTYPE" };

    /**
   * @deprecated Use {@link #registerPersistentEquivalent(Class,Class)} instead
   */
    public static Class registerPersitentEquivalent(Class transientType, Class persistentType) throws PersistenceException {
        return registerPersistentEquivalent(transientType, persistentType);
    }

    /**
   * Sets the persistent equivalent type to be used to (re)store a given
   * type of transient objects.
   *
   * @param transientType the type that will be replaced during
   *          serialisation operations
   * @param persistentType the type used to replace objects of transient
   *          type when serialising; this type needs to extend
   *          {@link Persistence}.
   * @return the persitent type that was used before this mapping if
   *         such existed.
   */
    public static Class registerPersistentEquivalent(Class transientType, Class persistentType) throws PersistenceException {
        if (!Persistence.class.isAssignableFrom(persistentType)) {
            throw new PersistenceException("Persistent equivalent types have to implement " + Persistence.class.getName() + "!\n" + persistentType.getName() + " does not!");
        }
        return (Class) persistentReplacementTypes.put(transientType, persistentType);
    }

    /**
   * A dictionary mapping from java type (Class) to the type (Class)
   * that can be used to store persistent data for the input type.
   */
    private static Map persistentReplacementTypes;

    /**
   * Stores the persistent replacements created during a transaction in
   * order to avoid creating two different persistent copies for the
   * same object. The keys used are {@link ObjectHolder}s that contain
   * the transient values being converted to persistent equivalents.
   */
    private static ThreadLocal<LinkedList<Map>> existingPersistentReplacements;

    /**
   * Stores the transient values obtained from persistent replacements
   * during a transaction in order to avoid creating two different
   * transient copies for the same persistent replacement. The keys used
   * are {@link ObjectHolder}s that hold persistent equivalents. The
   * values are the transient values created by the persisten
   * equivalents.
   */
    private static ThreadLocal<LinkedList<Map>> existingTransientValues;

    private static ClassComparator classComparator = new ClassComparator();

    /**
   * The file currently used to write the persisten representation. Will
   * only have a non-null value during storing operations.
   */
    static ThreadLocal<LinkedList<File>> persistenceFile;

    /**
   * The URL currently used to read the persistent representation when
   * reading from a URL. Will only be non-null during restoring
   * operations.
   */
    static ThreadLocal<LinkedList<URL>> persistenceURL;

    private static final class BooleanFlag {

        BooleanFlag(boolean initial) {
            flag = initial;
        }

        private boolean flag;

        public void setValue(boolean value) {
            flag = value;
        }

        public boolean getValue() {
            return flag;
        }
    }

    private static ThreadLocal<LinkedList<Boolean>> useGateHome;

    private static ThreadLocal<LinkedList<Boolean>> warnAboutGateHome;

    private static ThreadLocal<LinkedList<BooleanFlag>> haveWarnedAboutGateHome;

    private static ThreadLocal<LinkedList<BooleanFlag>> haveWarnedAboutResourceshome;

    static {
        persistentReplacementTypes = new HashMap();
        try {
            registerPersistentEquivalent(VisualResource.class, SlashDevSlashNull.class);
            registerPersistentEquivalent(URL.class, URLHolder.class);
            registerPersistentEquivalent(Map.class, MapPersistence.class);
            registerPersistentEquivalent(Collection.class, CollectionPersistence.class);
            registerPersistentEquivalent(ProcessingResource.class, PRPersistence.class);
            registerPersistentEquivalent(DataStore.class, DSPersistence.class);
            registerPersistentEquivalent(LanguageResource.class, LRPersistence.class);
            registerPersistentEquivalent(Corpus.class, CorpusPersistence.class);
            registerPersistentEquivalent(Controller.class, ControllerPersistence.class);
            registerPersistentEquivalent(ConditionalController.class, ConditionalControllerPersistence.class);
            registerPersistentEquivalent(ConditionalSerialAnalyserController.class, ConditionalSerialAnalyserControllerPersistence.class);
            registerPersistentEquivalent(LanguageAnalyser.class, LanguageAnalyserPersistence.class);
            registerPersistentEquivalent(SerialAnalyserController.class, SerialAnalyserControllerPersistence.class);
            registerPersistentEquivalent(gate.creole.AnalyserRunningStrategy.class, AnalyserRunningStrategyPersistence.class);
            registerPersistentEquivalent(gate.creole.RunningStrategy.UnconditionalRunningStrategy.class, UnconditionalRunningStrategyPersistence.class);
        } catch (PersistenceException pe) {
            pe.printStackTrace();
        }
        /**
     * Thread-local stack.
     */
        class ThreadLocalStack<T> extends ThreadLocal<LinkedList<T>> {

            @Override
            protected LinkedList<T> initialValue() {
                return new LinkedList<T>();
            }
        }
        existingPersistentReplacements = new ThreadLocalStack<Map>();
        existingTransientValues = new ThreadLocalStack<Map>();
        persistenceFile = new ThreadLocalStack<File>();
        persistenceURL = new ThreadLocalStack<URL>();
        useGateHome = new ThreadLocalStack<Boolean>();
        warnAboutGateHome = new ThreadLocalStack<Boolean>();
        haveWarnedAboutGateHome = new ThreadLocalStack<BooleanFlag>();
        haveWarnedAboutResourceshome = new ThreadLocalStack<BooleanFlag>();
    }
}
