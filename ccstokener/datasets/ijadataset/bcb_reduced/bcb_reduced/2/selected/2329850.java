package com.scythebill.birdlist.ui.guice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import com.google.common.base.Throwables;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import com.scythebill.birdlist.model.io.ProgressInputStream;
import com.scythebill.birdlist.model.taxa.Taxonomy;
import com.scythebill.birdlist.model.xml.XmlTaxonImport;
import com.scythebill.birdlist.ui.util.Progress;

/**
 * Loader class for importing a taxonomy file and identifying its progress.
 */
public class TaxonomyLoader implements Callable<Taxonomy>, Progress {

    private final long size;

    private volatile ProgressInputStream progressStream;

    private final InputSupplier<? extends InputStream> streamProvider;

    public TaxonomyLoader(URL url) {
        this(Resources.newInputStreamSupplier(url), getLength(url));
    }

    public TaxonomyLoader(InputSupplier<? extends InputStream> streamProvider, long size) {
        this.streamProvider = streamProvider;
        this.size = size;
    }

    private static long getLength(URL url) {
        URLConnection openConnection = null;
        try {
            openConnection = url.openConnection();
            return openConnection.getContentLength();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Taxonomy call() throws Exception {
        progressStream = new ProgressInputStream(streamProvider.getInput());
        BufferedReader reader = new BufferedReader(new InputStreamReader(progressStream, Charset.forName("UTF-8")));
        try {
            return (new XmlTaxonImport()).importTaxa(reader);
        } finally {
            reader.close();
        }
    }

    @Override
    public long current() {
        return progressStream.getCurrentPosition();
    }

    @Override
    public long max() {
        return size;
    }
}
