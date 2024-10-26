package com.nexirius.tools.dirsync;

import com.nexirius.util.XFile;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;

public class DefaultDirSyncManager implements DirSyncManager {

    private boolean interrupted = false;

    public void removeDirectory(XFile targetDir) {
        targetDir.delete();
    }

    public void createDirectory(XFile targetDir) {
        targetDir.mkdirs();
        targetDir.mkdir();
    }

    public void removeFile(XFile targetFile) {
        targetFile.delete();
    }

    public void createFile(URL url, XFile targetFile) throws IOException {
        InputStream in = url.openStream();
        targetFile.createFrom(in);
        in.close();
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}
