package com.google.code.javastorage.dropio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.google.code.javastorage.AbstractStorageFile;
import com.google.code.javastorage.StorageFile;
import com.google.code.javastorage.dropio.commands.GetAssets;
import com.google.code.javastorage.dropio.commands.GetContent;

/**
 * 
 * @author thomas.scheuchzer@gmail.com
 * 
 */
public class DropIoFile extends AbstractStorageFile {

    private static final long serialVersionUID = 1L;

    private DropIoSession session;

    private DropIoFile parent;

    private URL url;

    public DropIoFile(DropIoSession session) {
        super("");
        this.session = session;
    }

    public DropIoFile(String pathname) {
        super(pathname);
    }

    public DropIoFile(DropIoFile parent, String child) {
        this((StorageFile) parent, child);
        this.parent = parent;
        this.session = parent.session;
    }

    public DropIoFile(StorageFile parent, String child) {
        super(parent, child);
    }

    @Override
    public String[] list() {
        List<String> files = new ArrayList<String>();
        new GetAssets(session).list(this, files);
        return files.toArray(new String[0]);
    }

    @Override
    public DropIoFile[] listFiles() {
        List<DropIoFile> files = new ArrayList<DropIoFile>();
        new GetAssets(session).listFiles(this, files);
        return files.toArray(new DropIoFile[0]);
    }

    @Override
    public String getAbsolutePath() {
        return "http://drop.io" + super.getAbsolutePath();
    }

    @Override
    public File getParentFile() {
        return parent;
    }

    @Override
    public String getParent() {
        return parent == null ? null : parent.getParent();
    }

    @Override
    public InputStream openStream() throws IOException {
        return new GetContent().openStream(url);
    }

    public void setURL(URL url) {
        this.url = url;
    }
}
