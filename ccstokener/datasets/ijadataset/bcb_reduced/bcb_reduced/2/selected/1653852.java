package com.nexirius.tools.dirsync;

import com.nexirius.framework.datamodel.*;
import com.nexirius.util.CopyPairs;
import com.nexirius.util.TextToken;
import com.nexirius.util.XFile;
import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.Stack;

public class DirectoryInfoModel extends FileInfoModel {

    protected LongModel files;

    protected FileInfoArrayModel fileList;

    protected DirectoryInfoArrayModel dirList;

    private String url = null;

    public static final String DIR_INFO_FIENAME = "dirInfo.txt";

    public static final String FIELD_files = "files";

    public DirectoryInfoModel(URL urlValue) {
        super("DirectoryInfo", urlValue.toString());
        url = urlValue.toString();
        init();
    }

    public DirectoryInfoModel(String directory, String dirFilter, String filter) {
        this(new DirectoryInfo(directory), new FilterModel(dirFilter, ""), new FilterModel(filter, ""));
    }

    public DirectoryInfoModel(DirectoryInfo dirInfo, FilterModel dirFilter, FilterModel filter) {
        this(dirInfo.getRootDirectory());
        readDirectoryTree(dirInfo, dirFilter, filter);
    }

    public void readDirectoryTree(DirectoryInfo dirInfo, FilterModel dirFilter, FilterModel filter) {
        while (true) {
            DirectoryInfo actDir = dirInfo.nextDirectory(dirFilter, filter);
            if (actDir == null) {
                break;
            }
            String fullName = actDir.getFullName();
            if (fullName == null) {
                init(actDir);
            } else {
                Stack stack = new Stack();
                while (actDir.getParentDir() != null) {
                    stack.push(actDir);
                    actDir = actDir.getParentDir();
                }
                DirectoryInfoModel child = this;
                while (!stack.empty()) {
                    actDir = (DirectoryInfo) stack.pop();
                    child = child.addDirectory(actDir);
                }
            }
        }
    }

    public DirectoryInfoModel() {
        this((String) null);
    }

    public DirectoryInfoModel(String name) {
        super("DirectoryInfo", name);
        init();
    }

    private void init() {
        files = new LongModel(0, FIELD_files);
        append(files);
        fileList = new FileInfoArrayModel();
        append(fileList);
    }

    public void init(DirectoryInfo info) {
        if (!info.isDone()) {
            return;
        }
        name.setText(info.getName());
        files.setLong(info.getFiles());
        size.setLong(info.getSize());
        hash.setLong(info.getHash());
        Iterator iter = info.getFileList().iterator();
        while (iter.hasNext()) {
            FileInfo fileInfo = (FileInfo) iter.next();
            fileList.append(new FileInfoModel(fileInfo));
        }
    }

    public DirectoryInfoModel getDirectory(String name) {
        if (dirList == null) {
            return null;
        }
        DataModelEnumeration e = dirList.getEnumeration();
        while (e.hasMore()) {
            DirectoryInfoModel m = (DirectoryInfoModel) e.next();
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    private void resolveIfNeeded() throws Exception {
        if (url != null) {
            readDirectoryFrom(url);
            url = null;
        }
    }

    /**
     * read whole tree and set all nodes to create
     */
    private void createAll() throws Exception {
        if (url != null) {
            readDirectoryFrom(url);
            setCreate();
            url = null;
            DataModelEnumeration e;
            if (dirList != null) {
                e = dirList.getEnumeration();
                while (e.hasMore()) {
                    DirectoryInfoModel dir = (DirectoryInfoModel) e.next();
                    dir.createAll();
                }
            }
            e = fileList.getEnumeration();
            while (e.hasMore()) {
                FileInfoModel file = (FileInfoModel) e.next();
                file.setCreate();
            }
        }
    }

    public DirectoryInfoModel addDirectory(DirectoryInfo dir) {
        createDirListIfNeeded();
        DirectoryInfoModel child = getDirectory(dir.getName());
        if (child == null) {
            child = new DirectoryInfoModel(dir.getName());
            child.init(dir);
            dirList.append(child);
        } else {
            child.init(dir);
        }
        return child;
    }

    private void createDirListIfNeeded() {
        if (dirList == null) {
            dirList = new DirectoryInfoArrayModel();
            append(dirList);
        }
    }

    public boolean compareTo(DirectoryInfoModel source) throws Exception {
        source.resolveIfNeeded();
        if (getHash() == source.getHash()) {
            return true;
        }
        setChange();
        DataModelEnumeration e;
        if (dirList != null) {
            e = dirList.getEnumeration();
            while (e.hasMore()) {
                DirectoryInfoModel dir = (DirectoryInfoModel) e.next();
                DirectoryInfoModel sourceDir = source.getDirectory(dir.getName());
                if (sourceDir != null) {
                    sourceDir.resolveIfNeeded();
                    if (!dir.compareTo(sourceDir)) {
                        dir.setChange();
                    }
                } else {
                    dir.setRemove();
                }
            }
        }
        ArrayModel sourceDirList = source.getDirList();
        if (sourceDirList != null) {
            e = sourceDirList.getEnumeration();
            while (e.hasMore()) {
                DirectoryInfoModel sourceDir = (DirectoryInfoModel) e.next();
                DirectoryInfoModel dir = getDirectory(sourceDir.getName());
                if (dir == null) {
                    dir = (DirectoryInfoModel) sourceDir.duplicate(null, null);
                    dir.createAll();
                    createDirListIfNeeded();
                    dirList.append(dir);
                }
            }
        }
        e = fileList.getEnumeration();
        while (e.hasMore()) {
            FileInfoModel file = (FileInfoModel) e.next();
            FileInfoModel sourceFile = source.getFile(file.getName());
            if (sourceFile != null) {
                if (!file.sameVersion(sourceFile)) {
                    file.setChange();
                }
            } else {
                file.setRemove();
            }
        }
        e = source.getFileList().getEnumeration();
        while (e.hasMore()) {
            FileInfoModel sourceFile = (FileInfoModel) e.next();
            FileInfoModel file = getFile(sourceFile.getName());
            if (file == null) {
                file = (FileInfoModel) sourceFile.duplicate(null, null);
                file.setCreate();
                fileList.append(file);
            }
        }
        return false;
    }

    public DataModel duplicate(DataModel instance, CopyPairs copyPairs) {
        DirectoryInfoModel ret = (DirectoryInfoModel) super.duplicate(instance, copyPairs);
        ret.setUrl(url);
        return ret;
    }

    private ArrayModel getFileList() {
        return fileList;
    }

    private ArrayModel getDirList() {
        return dirList;
    }

    public FileInfoModel getFile(String name) {
        DataModelEnumeration e = fileList.getEnumeration();
        while (e.hasMore()) {
            FileInfoModel file = (FileInfoModel) e.next();
            if (file.getName().equals(name)) {
                return file;
            }
        }
        return null;
    }

    public boolean hasDirectory(String name) {
        DataModelEnumeration e = dirList.getEnumeration();
        while (e.hasMore()) {
            DirectoryInfoModel dir = (DirectoryInfoModel) e.next();
            if (dir.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private long getHash() {
        return hash.getLong();
    }

    public void sync(String sourceRootDirectoryUrl, String targetRootDirectory, DirSyncManager dirSyncManager) throws Exception {
        if (dirSyncManager.isInterrupted()) {
            return;
        }
        if (!sourceRootDirectoryUrl.endsWith("/")) {
            sourceRootDirectoryUrl = sourceRootDirectoryUrl + '/';
        }
        String dirUrl = sourceRootDirectoryUrl + getDirectoryName('/', false);
        XFile targetDir = new XFile(targetRootDirectory + File.separatorChar + getDirectoryName(File.separatorChar, false));
        if (isCreate()) {
            dirSyncManager.createDirectory(targetDir);
        } else if (isRemove()) {
            dirSyncManager.removeDirectory(targetDir);
            return;
        } else if (!targetDir.exists()) {
            dirSyncManager.createDirectory(targetDir);
        }
        DataModelEnumeration e = getFileList().getEnumeration();
        if (!targetDir.isDirectory()) {
            throw new Exception(targetDir.getPath() + " is not a target directory.");
        }
        while (e.hasMore()) {
            if (dirSyncManager.isInterrupted()) {
                return;
            }
            FileInfoModel file = (FileInfoModel) e.next();
            file.sync(dirUrl, targetDir.getPath(), dirSyncManager);
        }
        if (dirList == null) {
            return;
        }
        e = dirList.getEnumeration();
        while (e.hasMore()) {
            if (dirSyncManager.isInterrupted()) {
                return;
            }
            DirectoryInfoModel subdir = (DirectoryInfoModel) e.next();
            subdir.sync(sourceRootDirectoryUrl, targetRootDirectory, dirSyncManager);
        }
    }

    private String getDirectoryName(char separatorChar, boolean absolute) {
        StringBuffer ret = new StringBuffer();
        getDirectoryName(separatorChar, ret, absolute);
        return ret.toString();
    }

    private void getDirectoryName(char separatorChar, StringBuffer buffer, boolean absolute) {
        DirectoryInfoModel parent = getParentDirectoryInfoModel();
        if (parent == null && !absolute) {
            return;
        }
        buffer.insert(0, name.getText() + separatorChar);
        if (parent != null) {
            parent.getDirectoryName(separatorChar, buffer, absolute);
        }
    }

    public String getPath() {
        return getDirectoryName('/', true);
    }

    public void writeInfoToDirectory(String dirName) throws Exception {
        XFile dir = new XFile(dirName);
        dir.mkdirs();
        dir.mkdir();
        XFile dirInfo = new XFile(dirName, DIR_INFO_FIENAME);
        if (dirList != null) {
            dirList.setTransient(true);
        }
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dirInfo));
        writeDataTo(out);
        if (dirList != null) {
            DataModelEnumeration e = dirList.getEnumeration();
            while (e.hasMore()) {
                DirectoryInfoModel subdir = (DirectoryInfoModel) e.next();
                TextToken t = new TextToken(subdir.getName(), TextToken.STRING);
                t.writeTo(out);
                out.write('\n');
            }
        }
        out.close();
        if (dirList != null) {
            dirList.setTransient(false);
        }
        if (dirList != null) {
            DataModelEnumeration e = dirList.getEnumeration();
            while (e.hasMore()) {
                DirectoryInfoModel subdir = (DirectoryInfoModel) e.next();
                subdir.writeInfoToDirectory(dirName + File.separator + subdir.getName());
            }
        }
    }

    public void readDirectoryFrom(String urlString) throws Exception {
        URL url = new URL(urlString + DIR_INFO_FIENAME);
        PushbackInputStream in = new PushbackInputStream(new BufferedInputStream(url.openStream()));
        readDataFrom(in);
        TextToken t = TextToken.nextToken(in);
        while (t != null && t.isString()) {
            DirectoryInfoModel dir = addDirectory(new DirectoryInfo(t.getString()));
            dir.setUrl(urlString + t.getString() + '/');
            t = TextToken.nextToken(in);
        }
        in.close();
    }

    private void setUrl(String urlString) {
        url = urlString;
    }

    public long getFiles() {
        return files.getLong();
    }

    public static void main(String argv[]) throws Exception {
        XFile targetDirectory = new XFile("C:\\temp\\foo2");
        XFile sourceDir = new XFile("C:\\temp\\Zahlungen");
        XFile infoDir = new XFile("C:\\temp\\infoDir");
        infoDir.delete();
        DirectoryInfoModel sourceModel = new DirectoryInfoModel(sourceDir.getPath(), "+\"*\"-\"*\"", "+\"*\"");
        sourceModel.writeInfoToDirectory(infoDir.getPath());
        DirectoryInfoModel infoModel = new DirectoryInfoModel(infoDir.toURL());
        DirectoryInfoModel targetModel = new DirectoryInfoModel(targetDirectory.getPath(), "+\"*\"", "+\"*\"");
        targetModel.compareTo(infoModel);
        try {
            targetModel.sync(sourceDir.toURL().toString(), targetDirectory.getPath(), new DefaultDirSyncManager());
        } catch (Throwable t) {
        }
    }
}
