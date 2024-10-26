package uk.ac.ebi.intact.bridges.ontologies.iterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import uk.ac.ebi.intact.bridges.ontologies.OntologyDocument;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

/**
 * Ontology iterator that gets the documents from lines;
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: LineOntologyIterator.java 13161 2009-05-14 22:28:33Z baranda $
 */
public abstract class LineOntologyIterator implements OntologyIterator {

    private LineIterator lineIterator;

    private String currentLine;

    public LineOntologyIterator(URL url) throws IOException {
        this(url.openStream());
    }

    public LineOntologyIterator(InputStream is) {
        this(new InputStreamReader(is));
    }

    public LineOntologyIterator(Reader reader) {
        lineIterator = IOUtils.lineIterator(reader);
    }

    public boolean hasNext() {
        while (lineIterator.hasNext()) {
            currentLine = (String) lineIterator.next();
            if (!skipLine(currentLine)) {
                return true;
            }
        }
        return false;
    }

    public OntologyDocument next() {
        try {
            return processLine(currentLine);
        } catch (Throwable e) {
            throw new RuntimeException("Problem processing line: " + currentLine, e);
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("Cannot be removed");
    }

    protected abstract OntologyDocument processLine(String line);

    public boolean skipLine(String line) {
        line = line.trim();
        if (line.length() == 0) {
            return true;
        }
        return false;
    }
}
