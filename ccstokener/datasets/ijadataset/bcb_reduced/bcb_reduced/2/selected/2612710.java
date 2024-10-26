package gate.util.persistence;

import gate.Gate;
import gate.persist.PersistenceException;
import gate.util.Err;
import gate.util.GateException;
import gleam.gateservice.definition.tool.ApplicationInfo;
import gleam.gateservice.definition.tool.PRInfo;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.StaxReader;

/**
 * Parser to load a gapp file and extract the necessary information out
 * of it for the service definition builder tool. This class is in
 * package gate.util.persistence because it needs access to
 * package-private data in the various Persistence classes used by the
 * PersistenceManager.
 */
public class ServiceDefinitionToolGappParser {

    /**
   * This method loads the persistent representation of an object from
   * an XML GAPP file. It is largely a copy of
   * {@link PersistenceManager#loadObjectFromUrl} but does not make the
   * final step from persistent to transient representation of the
   * resulting object.
   * 
   * @param url
   * @return
   * @throws PersistenceException
   */
    static Object loadPersistentRepresentationFromFile(URL url) throws PersistenceException {
        PersistenceManager.persistenceURL.get().addFirst(url);
        ObjectInputStream ois = null;
        HierarchicalStreamReader reader = null;
        XStream xstream = null;
        try {
            Reader inputReader = new java.io.InputStreamReader(url.openStream());
            try {
                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                XMLStreamReader xsr = inputFactory.createXMLStreamReader(url.toExternalForm(), inputReader);
                reader = new StaxReader(new QNameMap(), xsr);
            } catch (XMLStreamException xse) {
                throw new PersistenceException("Error creating reader", xse);
            }
            xstream = new XStream(new StaxDriver());
            xstream.setClassLoader(Gate.getClassLoader());
            ois = xstream.createObjectInputStream(reader);
            Object res = null;
            Iterator urlIter = ((Collection) PersistenceManager.getTransientRepresentation(ois.readObject())).iterator();
            while (urlIter.hasNext()) {
                URL anUrl = (URL) urlIter.next();
                try {
                    Gate.getCreoleRegister().registerDirectories(anUrl);
                } catch (GateException ge) {
                    Err.prln("Could not reload creole directory " + anUrl.toExternalForm());
                }
            }
            res = ois.readObject();
            ois.close();
            return res;
        } catch (PersistenceException pe) {
            throw pe;
        } catch (Exception e) {
            throw new PersistenceException("Error loading GAPP file", e);
        } finally {
            PersistenceManager.persistenceURL.get().removeFirst();
            if (PersistenceManager.persistenceURL.get().isEmpty()) {
                PersistenceManager.persistenceURL.remove();
            }
        }
    }

    /**
   * Load information about the PRs in an application from the given
   * GAPP file. We do this using XStream in the same way as the
   * persistence mechanism from GATE, rather than trying to parse the
   * XML ourselves, as XStream does complicated things with the XML in
   * the case of a conditional controller, with all sorts of XPath cross
   * references which I don't want to have to resolve manually...
   * 
   * @param url
   * @return
   * @throws PersistenceException
   */
    public static ApplicationInfo parseGapp(URL url) throws PersistenceException {
        Object res = loadPersistentRepresentationFromFile(url);
        if (!(res instanceof ControllerPersistence)) {
            throw new PersistenceException("GAPP file " + url + " does not contain a saved GATE application");
        }
        ControllerPersistence cp = (ControllerPersistence) res;
        if (!(cp.prList instanceof CollectionPersistence)) {
            throw new PersistenceException("Malformed GAPP file " + url + ": Controller definition does not contain a PR list");
        }
        CollectionPersistence prListPersistence = (CollectionPersistence) cp.prList;
        List<PRInfo> prInfos = new ArrayList<PRInfo>(prListPersistence.localList.size());
        Set<String> inputAnnotationSetNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        Set<String> outputAnnotationSetNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (Object prPersistenceObj : prListPersistence.localList) {
            if (!(prPersistenceObj instanceof PRPersistence)) {
                throw new PersistenceException("Malformed GAPP file " + url + ": PR list contains an object which is not a " + "PRPersistence");
            }
            PRPersistence prPersistence = (PRPersistence) prPersistenceObj;
            PRInfo prInfo = new PRInfo();
            prInfo.setName(prPersistence.resourceName);
            prInfo.setClassName(prPersistence.resourceType);
            if (prPersistence.runtimeParams instanceof MapPersistence) {
                Map params = ((MapPersistence) prPersistence.runtimeParams).localMap;
                if (params != null) {
                    if (params.get("annotationSetName") != null) {
                        inputAnnotationSetNames.add((String) params.get("annotationSetName"));
                        outputAnnotationSetNames.add((String) params.get("annotationSetName"));
                    }
                    if (params.get("inputASName") != null) {
                        inputAnnotationSetNames.add((String) params.get("inputASName"));
                    }
                    if (params.get("outputASName") != null) {
                        outputAnnotationSetNames.add((String) params.get("outputASName"));
                    }
                }
            }
            prInfos.add(prInfo);
        }
        return new ApplicationInfo(prInfos, inputAnnotationSetNames, outputAnnotationSetNames);
    }
}
