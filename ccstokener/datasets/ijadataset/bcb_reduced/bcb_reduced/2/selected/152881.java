package org.gocha.files;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author gocha
 */
public class ZipFileSystem implements FileSystem {

    private interface FSObject {

        boolean isDirectory();

        long size();

        String name();

        FSObject parent();

        void parent(FSObject object);

        FSObject[] children();

        InputStream openRead() throws IOException;
    }

    private abstract class AbstractFSObject implements FSObject {

        public String stringPath(String join) {
            if (join == null) {
                throw new IllegalArgumentException("join == null");
            }
            String path = "";
            FSObject fso = this;
            while (true) {
                path = join + fso.name() + path;
                FSObject parent = fso.parent();
                if (parent == null) break;
                fso = parent;
                boolean isRoot = fso.parent() == null;
                if (isRoot) break;
            }
            return path;
        }
    }

    private abstract class AbstractFileObject extends AbstractFSObject implements FSObject {

        protected String name;

        protected long size;

        public AbstractFileObject(String name, long size) {
            if (name == null) {
                throw new IllegalArgumentException("name == null");
            }
            this.name = name;
            this.size = size;
        }

        public boolean isDirectory() {
            return false;
        }

        public long size() {
            return size;
        }

        public String name() {
            return name;
        }

        protected FSObject parent = null;

        public FSObject parent() {
            return parent;
        }

        public void parent(FSObject object) {
            this.parent = object;
        }

        private FSObject[] empty = new FSObject[] {};

        public FSObject[] children() {
            return empty;
        }
    }

    private class DirectoryObject extends AbstractFSObject implements FSObject {

        public DirectoryObject(String name) {
            if (name == null) {
                throw new IllegalArgumentException("name == null");
            }
            this.name = name;
        }

        public boolean isDirectory() {
            return true;
        }

        public long size() {
            return 0;
        }

        private String name = null;

        public String name() {
            return name;
        }

        private FSObject parent = null;

        public FSObject parent() {
            return parent;
        }

        public void parent(FSObject p) {
            parent = p;
        }

        private FSObject[] children = new FSObject[] {};

        public FSObject[] children() {
            return children;
        }

        public InputStream openRead() throws IOException {
            throw new IOException("Not supported yet.");
        }

        public void addChild(FSObject child) {
            if (child == null) return;
            child.parent(this);
            String cname = child.name();
            if (cname == null) return;
            for (int i = 0; i < children.length; i++) {
                if (cname.equals(children[i].name())) {
                    children[i] = child;
                    return;
                }
            }
            FSObject[] newChildren = Arrays.<FSObject>copyOf(children, children.length + 1);
            newChildren[newChildren.length - 1] = child;
            children = newChildren;
        }

        public void removeChild(FSObject child) {
            if (child == null) return;
            FSObject[] newChildren = new FSObject[] {};
            for (FSObject fso : children) {
                if (child != fso) {
                    newChildren = Arrays.<FSObject>copyOf(newChildren, newChildren.length + 1);
                    newChildren[newChildren.length - 1] = fso;
                }
            }
            children = newChildren;
        }

        public FSObject getChild(String name) {
            if (name == null) {
                throw new IllegalArgumentException("name == null");
            }
            for (FSObject fso : children) {
                if (fso == null) continue;
                if (name.equals(fso.name())) return fso;
            }
            return null;
        }

        public FSObject getChild(String[] path) {
            if (path == null) {
                throw new IllegalArgumentException("path == null");
            }
            if (path.length < 1) return null;
            FSObject child = getChild(path[0]);
            String[] childPath = shiftLeft(path);
            if (childPath.length < 1) return child;
            if (child instanceof DirectoryObject) {
                return ((DirectoryObject) child).getChild(childPath);
            }
            return null;
        }

        public String[] shiftLeft(String[] path) {
            if (path == null) {
                throw new IllegalArgumentException("path == null");
            }
            if (path.length > 1) return Arrays.<String>copyOfRange(path, 1, path.length);
            if (path.length == 1) return new String[] {};
            return path;
        }

        public String[] parentOf(String[] path) {
            if (path == null) {
                throw new IllegalArgumentException("path == null");
            }
            if (path.length > 1) return Arrays.<String>copyOf(path, path.length - 1);
            if (path.length == 1) return new String[] {};
            return path;
        }

        public void mkdir(String[] path) {
            if (path == null) {
                throw new IllegalArgumentException("path == null");
            }
            if (path.length < 1) return;
            FSObject fso = getChild(path[0]);
            DirectoryObject dir = null;
            if (fso == null) {
                dir = new DirectoryObject(path[0]);
                addChild(dir);
            } else {
                if (fso instanceof DirectoryObject) {
                    dir = (DirectoryObject) fso;
                } else {
                    dir = new DirectoryObject(path[0]);
                    removeChild(fso);
                    addChild(dir);
                }
            }
            String[] newPath = shiftLeft(path);
            dir.mkdir(newPath);
        }
    }

    private interface ZipDataSource {

        public InputStream zipData() throws IOException;
    }

    private ZipDataSource zipfile = null;

    public ZipFileSystem(File zipFile) {
        if (zipFile == null) {
            throw new IllegalArgumentException("zipFile == null");
        }
        final File fZip = zipFile;
        zipfile = new ZipDataSource() {

            public InputStream zipData() throws IOException {
                return new FileInputStream(fZip);
            }
        };
        refreshZip();
    }

    public ZipFileSystem(InputStream zipFile) {
        if (zipFile == null) {
            throw new IllegalArgumentException("zipFile == null");
        }
        try {
            final byte[] zipdata = FileUtil.readAllData(zipFile);
            if (zipdata != null) {
                zipfile = new ZipDataSource() {

                    public InputStream zipData() throws IOException {
                        return new ByteArrayInputStream(zipdata);
                    }
                };
            } else {
                zipfile = new ZipDataSource() {

                    public InputStream zipData() throws IOException {
                        throw new FileNotFoundException();
                    }
                };
            }
        } catch (IOException ex) {
            zipfile = new ZipDataSource() {

                public InputStream zipData() throws IOException {
                    throw new FileNotFoundException();
                }
            };
        }
        refreshZip();
    }

    public ZipFileSystem(URL zipFile) {
        if (zipFile == null) {
            throw new IllegalArgumentException("zipFile == null");
        }
        final URL furl = zipFile;
        zipfile = new ZipDataSource() {

            public InputStream zipData() throws IOException {
                return furl.openStream();
            }
        };
        refreshZip();
    }

    public ZipFileSystem(FileSystem fs, String file) {
        if (fs == null) {
            throw new IllegalArgumentException("fs == null");
        }
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        final FileSystem ffs = fs;
        final String ffile = file;
        zipfile = new ZipDataSource() {

            public InputStream zipData() throws IOException {
                return ffs.getFile(ffile);
            }
        };
        refreshZip();
    }

    private String rootName = "";

    private DirectoryObject root = new DirectoryObject(rootName);

    private FileNames fnames = DefaultFileNames.instance();

    public void refreshZip() {
        root = new DirectoryObject(rootName);
        if (zipfile == null) return;
        try {
            InputStream inStrm = zipfile.zipData();
            ZipInputStream zipInStrm = new ZipInputStream(inStrm);
            while (true) {
                ZipEntry ze = zipInStrm.getNextEntry();
                if (ze == null) break;
                boolean isDir = ze.isDirectory();
                String name = ze.getName();
                long size = isDir ? 0 : ze.getSize();
                long time = ze.getTime();
                zipInStrm.closeEntry();
                String spath = name.replace("\\", "/");
                String[] path = fnames.splitPath(spath);
                if (path == null || path.length < 1) continue;
                if (isDir) {
                    root.mkdir(path);
                } else {
                    final String fileName = path[path.length - 1];
                    final String filePath = name;
                    FSObject file = new AbstractFileObject(fileName, size) {

                        public InputStream openRead() throws IOException {
                            return openZipEntry(filePath);
                        }
                    };
                    if (path.length == 1) {
                        root.addChild(file);
                    } else {
                        String[] dirPath = root.parentOf(path);
                        root.mkdir(dirPath);
                        FSObject _dir = root.getChild(dirPath);
                        DirectoryObject dir = _dir instanceof DirectoryObject ? (DirectoryObject) _dir : null;
                        if (dir != null) {
                            dir.addChild(file);
                        }
                    }
                }
            }
            inStrm.close();
        } catch (IOException ex) {
            FileUtil.fireException(ex);
            return;
        }
    }

    private InputStream openZipEntry(String __name) throws IOException {
        if (zipfile == null) throw new IOException("ZIP data not avaliable");
        try {
            final InputStream inStrm = zipfile.zipData();
            final ZipInputStream zipInStrm = new ZipInputStream(inStrm);
            while (true) {
                ZipEntry ze = zipInStrm.getNextEntry();
                if (ze == null) break;
                String name = ze.getName();
                if (name.equals(__name)) {
                    InputStream res = new InputStream() {

                        @Override
                        public int read() throws IOException {
                            return zipInStrm.read();
                        }

                        @Override
                        public int read(byte[] b) throws IOException {
                            return zipInStrm.read(b);
                        }

                        @Override
                        public int read(byte[] b, int off, int len) throws IOException {
                            return zipInStrm.read(b, off, len);
                        }

                        @Override
                        public void close() throws IOException {
                            zipInStrm.closeEntry();
                            inStrm.close();
                        }
                    };
                    return res;
                }
                zipInStrm.closeEntry();
            }
            inStrm.close();
        } catch (IOException ex) {
            FileUtil.fireException(ex);
        }
        throw new FileNotFoundException(__name);
    }

    public InputStream getFile(String file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        String[] path = fnames.splitPath(file);
        if (path == null) throw new IOException("Invalid file path:" + file);
        FSObject obj = root.getChild(path);
        if (obj == null) throw new IOException("File not dound:" + file);
        return obj.openRead();
    }

    public OutputStream putFile(String file) throws IOException {
        throw new IOException("Not supported yet.");
    }

    public String[] list(String path) throws IOException {
        if (path == null) {
            return new String[] {};
        }
        DirectoryObject dir = null;
        if (fnames.root().equals(path)) {
            dir = root;
        } else {
            String[] apath = fnames.splitPath(path);
            if (apath == null) {
                return new String[] {};
            }
            FSObject fso = root.getChild(apath);
            if (fso instanceof DirectoryObject) {
                dir = (DirectoryObject) fso;
            }
        }
        if (dir == null) return new String[] {};
        FSObject[] children = dir.children();
        if (children == null) return new String[] {};
        String[] result = new String[children.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = children[i] instanceof AbstractFSObject ? ((AbstractFSObject) children[i]).stringPath(fnames.delimeter()) : children[i].name();
        }
        return result;
    }

    public long sizeOf(String file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        String[] path = fnames.splitPath(file);
        if (path == null) throw new IOException("Invalid file path:" + file);
        FSObject obj = root.getChild(path);
        if (obj == null) throw new IOException("File not dound:" + file);
        return obj.size();
    }

    public boolean ignoreCase() {
        return false;
    }

    public boolean exists(String file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        String[] path = fnames.splitPath(file);
        if (path == null) return false;
        FSObject obj = root.getChild(path);
        if (obj == null) return false;
        return true;
    }

    public boolean isFile(String file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        String[] path = fnames.splitPath(file);
        if (path == null) return false;
        FSObject obj = root.getChild(path);
        if (obj == null) return false;
        return obj instanceof AbstractFileObject;
    }

    public boolean isDirectory(String file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        String[] path = fnames.splitPath(file);
        if (path == null) return false;
        FSObject obj = root.getChild(path);
        if (obj == null) return false;
        return obj instanceof DirectoryObject;
    }

    public boolean delete(String path) throws IOException {
        throw new IOException("Not supported yet.");
    }

    public boolean rename(String srcPath, String destPath) throws IOException {
        throw new IOException("Not supported yet.");
    }

    public boolean mkdir(String path) throws IOException {
        throw new IOException("Not supported yet.");
    }
}
