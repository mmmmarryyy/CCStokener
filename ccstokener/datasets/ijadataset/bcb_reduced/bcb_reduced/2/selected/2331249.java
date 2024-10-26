package net.sourceforge.retriever.collector.resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.retriever.collector.handler.Document;
import sun.net.www.protocol.file.FileURLConnection;

class FileResource extends Resource {

    FileResource(final URL url) {
        super(url);
    }

    @Override
    public Document getData() throws Exception {
        URLConnection urlConnection = null;
        try {
            urlConnection = super.getURL().openConnection();
            urlConnection.connect();
            return (Document) urlConnection.getContent();
        } finally {
            if (urlConnection != null) ((FileURLConnection) urlConnection).close();
        }
    }

    @Override
    public Iterator<Resource> childrenResources() {
        final List<Resource> resources = new ArrayList<Resource>();
        try {
            final File resourceAsFileObject = new File(this.getURL().getPath().replaceFirst("file:", ""));
            if (resourceAsFileObject.isDirectory()) {
                final File[] innerFiles = resourceAsFileObject.listFiles();
                for (File innerFile : innerFiles) {
                    resources.add(new FileResource(new URL("file:" + innerFile.getAbsolutePath())));
                }
            }
        } catch (final IOException e) {
        }
        return resources.iterator();
    }

    @Override
    public boolean canCollect() {
        final File resource = new File(super.getURL().getPath().replaceFirst("file:", ""));
        if (resource.exists() && resource.isFile()) {
            return true;
        } else {
            return false;
        }
    }
}
