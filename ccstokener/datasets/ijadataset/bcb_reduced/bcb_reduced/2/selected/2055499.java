package eu.annocultor.tagger.vocabularies.loaders;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.input.CloseShieldInputStream;
import eu.annocultor.common.Utils;

/**
 * A vocabulary iterator.
 * 
 * @see VocabularyLoader
 * 
 * @author Borys Omelayenko
 * 
 */
public class VocabularyLocationIterator implements Iterator<VocabularyLocation> {

    String[] patterns;

    Queue<URL> urls = new LinkedList<URL>();

    Queue<File> files = new LinkedList<File>();

    ZipInputStream in;

    ZipEntry nextZipEntry;

    public VocabularyLocationIterator(String base, String... patterns) {
        this.patterns = patterns;
        init(base);
    }

    void init(String base) {
        try {
            if (base.endsWith(".zip")) {
                in = new ZipInputStream(new FileInputStream(base));
                findNextZipEntry();
            } else {
                if (base.startsWith("http://") && patterns.length == 0) {
                    urls.add(new URL(base));
                } else {
                    for (String pattern : patterns) {
                        for (File file : Utils.expandFileTemplateFrom(new File(base), pattern)) {
                            files.add(file);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error on init", e);
        }
    }

    void findNextZipEntry() throws IOException {
        while ((nextZipEntry = in.getNextEntry()) != null) {
            if (accept(nextZipEntry.getName())) {
                return;
            }
        }
        in.close();
    }

    @Override
    public boolean hasNext() {
        return !urls.isEmpty() || !files.isEmpty() || nextZipEntry != null;
    }

    @Override
    public VocabularyLocation next() {
        try {
            if (!urls.isEmpty()) {
                final URL url = urls.poll();
                return new VocabularyLocation(url.toExternalForm(), VocabularyFormat.RDFXML, 0, url.openStream());
            }
            if (!files.isEmpty()) {
                File file = files.poll();
                return new VocabularyLocation(file.getCanonicalPath(), file.getName().endsWith(".ntriples") ? VocabularyFormat.NTRIPLES : VocabularyFormat.RDFXML, file.lastModified(), new FileInputStream(file));
            }
            if (nextZipEntry != null) {
                String zipEntryAsString = IOUtils.toString(new CloseShieldInputStream(in), "UTF-8");
                VocabularyLocation location = new VocabularyLocation(nextZipEntry.getName(), nextZipEntry.getName().endsWith(".rdf") ? VocabularyFormat.RDFXML : null, nextZipEntry.getTime(), IOUtils.toInputStream(zipEntryAsString, "UTF-8"));
                findNextZipEntry();
                return location;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new NoSuchElementException();
    }

    public boolean accept(String name) {
        for (String pattern : patterns) {
            FileFilter fileFilter = new WildcardFileFilter(FilenameUtils.getName(pattern));
            if (fileFilter.accept(new File(name))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void remove() {
        throw new RuntimeException("Not implemented");
    }
}
