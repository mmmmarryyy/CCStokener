package uk.ac.shef.wit.runes.artisan;

import com.hp.hpl.jena.mem.ModelMem;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import uk.ac.shef.wit.runes.Runes;
import uk.ac.shef.wit.runes.exceptions.RunesExceptionCannotHandle;
import uk.ac.shef.wit.runes.exceptions.RunesExceptionNoSuchContent;
import uk.ac.shef.wit.runes.exceptions.RunesExceptionNoSuchStructure;
import uk.ac.shef.wit.runes.exceptions.RunesExceptionRuneExecution;
import uk.ac.shef.wit.runes.rune.Rune;
import uk.ac.shef.wit.runes.rune.RuneShaper;
import uk.ac.shef.wit.runes.runestone.Runestone;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * An {@link Artisan} decorator that allows specifying the required output data in the form of an RDF ontology.
 * @version $Id: ArtisanModeled.java 359 2008-05-08 23:25:20Z jiria $
 * @author <a href="mailto:j.iria@dcs.shef.ac.uk">Jos&eacute; Iria</a>
 */
public class ArtisanModeled implements Artisan {

    private static final Logger log = Logger.getLogger(ArtisanModeled.class.getName());

    private URL _modelURL;

    private Artisan _artisan;

    /**
    * Creates a new {@link ArtisanModeled} instance, using {@link uk.ac.shef.wit.runes.artisan.ArtisanDefault} as the underlying {@link Artisan}.
    * @param modelURL the url to the RDF model to use.
    */
    public ArtisanModeled(final URL modelURL) {
        this(modelURL, new ArtisanDefault());
    }

    /**
    * Creates a new {@link ArtisanModeled} instance.
    * @param modelURL the url to the RDF model to use.
    * @param artisan the underlying {@link uk.ac.shef.wit.runes.artisan.Artisan} to use.
    */
    public ArtisanModeled(final URL modelURL, final Artisan artisan) {
        _modelURL = modelURL;
        _artisan = artisan;
    }

    public Runestone carve(final Runestone stone, final Rune... runes) throws RunesExceptionCannotHandle, RunesExceptionNoSuchContent, RunesExceptionNoSuchStructure, RunesExceptionRuneExecution {
        final Rune[] augmented = new Rune[runes.length + 1];
        try {
            augmented[0] = new RuneShaper(processRDFModel(_modelURL));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.arraycopy(runes, 0, augmented, 1, runes.length);
        return _artisan.carve(stone, augmented);
    }

    private String[] processRDFModel(final URL url) throws IOException {
        final Set<String> types = new HashSet<String>();
        final Model model = new ModelMem().read(url.openStream(), "");
        final Map<String, String> labels = new HashMap<String, String>();
        addLabels(model, RDFS.Class, labels);
        addLabels(model, RDF.Property, labels);
        for (ResIterator rit = model.listSubjectsWithProperty(RDF.type, RDFS.Class); rit.hasNext(); ) types.add(getLabel(labels, rit.nextResource().getURI()));
        for (ResIterator rit = model.listSubjectsWithProperty(RDF.type, RDF.Property); rit.hasNext(); ) {
            final Resource property = rit.nextResource();
            final String relation = getLabel(labels, property.getURI());
            types.add(relation);
            String domain = null;
            for (StmtIterator sit = property.listProperties(RDFS.domain); sit.hasNext(); ) domain = sit.nextStatement().getResource().getURI();
            String range = null;
            for (StmtIterator sit = property.listProperties(RDFS.range); sit.hasNext(); ) range = sit.nextStatement().getResource().getURI();
            if (!(domain == null || relation == null || range == null)) Runes.registerAlias(relation, getLabel(labels, domain) + '|' + getLabel(labels, range) + '|' + relation);
        }
        return types.toArray(new String[types.size()]);
    }

    private void addLabels(final Model model, final Resource type, final Map<String, String> labels) {
        for (ResIterator rit = model.listSubjectsWithProperty(RDF.type, type); rit.hasNext(); ) {
            final Resource resource = rit.nextResource();
            String label = resource.getLocalName();
            for (StmtIterator sit = resource.listProperties(RDFS.label); sit.hasNext(); ) label = sit.nextStatement().getObject().toString();
            labels.put(resource.getURI(), label);
        }
    }

    private String getLabel(final Map<String, String> labels, final String type) {
        final String label = labels.get(type);
        return label == null ? type : label;
    }
}
