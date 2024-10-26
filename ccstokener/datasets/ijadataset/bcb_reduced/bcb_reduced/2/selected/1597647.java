package se.kth.cid.rdf;

import se.kth.cid.rdf.*;
import org.w3c.rdf.util.*;
import org.w3c.rdf.model.*;
import org.w3c.rdf.syntax.*;
import org.xml.sax.InputSource;
import org.w3c.rdf.implementation.syntax.sirpac.SiRPAC;
import se.kth.cid.component.*;
import se.kth.cid.component.cache.ComponentCache;
import se.kth.cid.component.local.*;
import se.kth.cid.rdf.metadata.*;
import netscape.security.PrivilegeManager;
import se.kth.cid.util.*;
import se.kth.cid.identity.*;
import se.kth.cid.neuron.*;
import se.kth.cid.neuron.local.*;
import se.kth.cid.conceptmap.*;
import se.kth.cid.conceptmap.local.*;
import org.w3c.rdf.vocabulary.rdf_syntax_19990222.RDF;
import org.w3c.rdf.vocabulary.rdf_schema_200001.RDFS;
import java.io.*;
import java.util.*;

/** This is the FormatHandler that are used with RDF models (text/rdf).
 *
 *  @author Matthias Palmer
 *  @version $Revision: 395 $
 */
public class RDFFormatHandler implements FormatHandler {

    /** The "text/RDF" MIME type.
   */
    public static final MIMEType RDF = new MIMEType("text/rdf", true);

    /** Whether we should bother using the Netscape privilege manager.
   */
    public static boolean usePrivMan = false;

    /** Factory for parsers, models, nodes and serializers.
   */
    ConzillaRDFFactoryImpl factory;

    /** A formathandler can't be created without a cache, @see #RDFFormatHandler(ComponentCache).
   */
    private RDFFormatHandler() {
    }

    /** Constructs an XmlFormatLoader.
   * 
   *  The ComponentCache is needed for the @see #StaticModel that are created from
   *  the @see #ConzillaRDFFactoryImpl.
   */
    public RDFFormatHandler(ComponentCache cache) {
        factory = new ConzillaRDFFactoryImpl(cache);
    }

    public MIMEType getMIMEType() {
        return RDF;
    }

    public boolean canHandleURI(URI uri) {
        return true;
    }

    public Component loadComponent(Container container, URI origuri) throws ComponentException {
        return container.getComponent(origuri);
    }

    public Component loadComponent(URI uri, URI origuri) throws ComponentException {
        if (usePrivMan) PrivilegeManager.enablePrivilege("UniversalConnect");
        return getComponent(origuri, null);
    }

    /** Returns a Component (i.e. a ResourceComponent, a ReifiedResourceComponent or
     *  a RDFConceptMap) wrapping a RDF-resource. The RDF-resource doesn't need to be
     *  mentioned in any tripples for it to be fetched.
     *
     *  <P>When a component is asked for for the first time it is created on the basis of existing type
     *  information, type information added later won't affect (in runtime) wich wrapping Component used. 
     *  This means that an ResourceComponent won't automatically be upgraded to a conceptmap if 
     *  such type information is created later. However this might be circumvented via the reload 
     *  functionality.</P>
     *  Type 'Statement', a ReifiedResourceComponent is created.<br>
     *  Type 'ConceptMap', a RDFConceptMap is created.<br>
     *  Otherwise a ResourceComponent is created.
     *
     *  @param uri the uri of the component to get.
     *  @param container the default model where triples should be added on this resource.
     *  @return a component, never null but may be empty 
     *         (i.e. no tripple in any model currently loaded mentions it). 
     */
    public Component getComponent(URI uri, Container container) throws ComponentException {
        ResourceComponent comp = factory.getTotalModel().findComponent(uri);
        if (comp != null) return comp;
        try {
            Resource re;
            re = factory.getNodeFactory().createResource(uri.toString());
            ReifiedResourceComponent rrc = factory.getTotalModel().getReifiedResourceComponent(re);
            if (rrc != null) return rrc;
            if (container instanceof ConzillaRDFModel) return new ResourceComponent(factory.getTotalModel(), (ConzillaRDFModel) container, re, uri); else return new ResourceComponent(factory.getTotalModel(), null, re, uri);
        } catch (ModelException me) {
            throw new ComponentException("Failed creating component from resource with uri=" + uri + " Reason:" + me.getMessage());
        }
    }

    public Component loadModel(URI uri, URI origuri) throws ComponentException {
        if (usePrivMan) PrivilegeManager.enablePrivilege("UniversalConnect");
        ConzillaRDFModel model = factory.createModel(origuri, uri);
        RDFParser parser = new com.hp.hpl.jena.rdf.arp.StanfordImpl();
        java.net.URL url = null;
        try {
            url = uri.getJavaURL();
        } catch (java.net.MalformedURLException e) {
            throw new ComponentException("Invalid URL " + uri + " for component " + origuri + ":\n " + e.getMessage());
        }
        try {
            InputSource source = new InputSource(url.openStream());
            source.setSystemId(origuri.toString());
            parser.parse(source, new ModelConsumer(model));
            factory.getTotalModel().addModel(model);
        } catch (org.xml.sax.SAXException se) {
            se.getException().printStackTrace();
            throw new ComponentException("Format error loading URL " + url + " for component " + origuri + ":\n " + se.getMessage());
        } catch (java.io.IOException se) {
            throw new ComponentException("IO error loading URL " + url + " for component " + origuri + ":\n " + se.getMessage());
        } catch (org.w3c.rdf.model.ModelException se) {
            throw new ComponentException("Model error loading URL " + url + " for component " + origuri + ":\n " + se.getMessage());
        }
        return model;
    }

    public boolean isSavable(URI uri) {
        return false;
    }

    public void checkCreateComponent(URI uri) throws ComponentException {
        throw new ComponentException("Not implemented on RDF models yet");
    }

    public Component createComponent(URI uri, URI realURI, String type, Object extras) throws ComponentException {
        throw new ComponentException("Creation of components in RDF models not implemented yet.");
    }

    public void saveComponent(URI uri, Component comp) throws ComponentException {
        throw new ComponentException("Saving of RDF models not implemented yet.");
    }
}
