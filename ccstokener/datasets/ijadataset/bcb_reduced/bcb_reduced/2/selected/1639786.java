package org.go.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import org.go.expcetion.GoException;
import org.go.spi.ClassLoadHelper;
import org.slf4j.Logger;

/**
 * 
 * @author hejie
 *
 */
public class JobFile {

    private static Logger LOG = org.slf4j.LoggerFactory.getLogger(JobFile.class);

    private ClassLoadHelper classLoadHelper = null;

    private String fileBasename;

    private boolean fileFound;

    private String fileName;

    private String filePath;

    public JobFile(String fileName, ClassLoadHelper aClassLoadHelper) throws GoException {
        this.setFileName(fileName);
        initialize(aClassLoadHelper);
    }

    private void initialize(ClassLoadHelper aClassLoadHelper) throws GoException {
        InputStream f = null;
        try {
            this.classLoadHelper = aClassLoadHelper;
            String furl = null;
            File file = new File(getFileName());
            if (!file.exists()) {
                URL url = classLoadHelper.getResource(getFileName());
                if (url != null) {
                    try {
                        furl = URLDecoder.decode(url.getPath(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        furl = url.getPath();
                    }
                    file = new File(furl);
                    try {
                        f = url.openStream();
                    } catch (IOException ignor) {
                    }
                }
            } else {
                try {
                    f = new java.io.FileInputStream(file);
                } catch (FileNotFoundException e) {
                }
            }
            if (f == null) {
                LOG.warn("File named ' {} ' does not exist.", getFileName());
            } else {
                fileFound = true;
                filePath = (furl != null) ? furl : file.getAbsolutePath();
                fileBasename = file.getName();
            }
        } finally {
            try {
                if (f != null) {
                    f.close();
                }
            } catch (IOException ioe) {
                LOG.warn("Error closing jobs file " + getFileName(), ioe);
            }
        }
    }

    public String getFileBasename() {
        return fileBasename;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isFileFound() {
        return fileFound;
    }

    public void setFileBasename(String fileBasename) {
        this.fileBasename = fileBasename;
    }

    public void setFileFound(boolean fileFound) {
        this.fileFound = fileFound;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
