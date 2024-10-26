package org.formaria.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.formaria.task.LaunderThrowable;
import org.formaria.debug.DebugLogger;
import org.formaria.aria.Project;
import org.formaria.aria.data.BaseModel;
import org.formaria.aria.data.DataModel;
import org.formaria.aria.helper.SwingWorker;

/**
 * A dynamic loader of resources from a URL.
 * <p> Copyright (c) Formaria Ltd., 2008, This software is licensed under
 * the GNU Public License (GPL), please see license.txt for more details. If
 * you make commercial use of this software you must purchase a commercial
 * license from Formaria.</p>
 */
public class ResourceLoader {

    public static final int DOWNLOAD_FILENAME = 0;

    public static final int DOWNLOAD_ID = 1;

    public static final int DOCUMENT_FILESIZE = 2;

    public static final int DOWNLOAD_FILEDATE = 3;

    public static final int DOWNLOAD_PROGRESS = 4;

    private Project currentProject;

    private DataModel rootModel;

    private BaseModel downloadList;

    private String serverURL;

    private File targetFolder;

    private SimpleDateFormat sdf;

    private final ExecutorService threadPool;

    private ResourceClassLoader urlClassLoader;

    /** Creates a new instance of DocumentManager */
    protected ResourceLoader(Project project) {
        currentProject = project;
        rootModel = currentProject.getModel();
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        threadPool = Executors.newFixedThreadPool(3);
        urlClassLoader = new ResourceClassLoader(new URL[0], getClass().getClassLoader());
        project.addCustomClassLoader(urlClassLoader);
    }

    public static ResourceLoader getInstance(Project project) {
        ResourceLoader resourceLoader = (ResourceLoader) project.getObject("ResourceLoader");
        if (resourceLoader == null) {
            resourceLoader = new ResourceLoader(project);
            project.setObject("ResourceLoader", resourceLoader);
        }
        return resourceLoader;
    }

    public ClassLoader getClassloader() {
        return urlClassLoader;
    }

    /**
   * Start the download service
   */
    public void startCacheCheck(BaseModel fileList, File outputFolder) {
        downloadList = fileList;
        if (outputFolder != null) setTargetFolder(outputFolder);
        SwingWorker worker = new SwingWorker() {

            public Object construct() {
                try {
                    checkCache();
                } finally {
                }
                return null;
            }

            public void finished() {
            }
        };
        worker.start();
    }

    /**
   * Start the download service
   */
    public void startDownload(BaseModel fileList, final String downloadServer, File outputFolder, final ResourceLoaderStatus status) {
        downloadList = fileList;
        setServerURL(downloadServer);
        if (outputFolder != null) setTargetFolder(outputFolder);
        SwingWorker worker = new SwingWorker() {

            public Object construct() {
                try {
                    doDownload(status);
                } finally {
                }
                return null;
            }

            public void finished() {
                status.setProgress(100);
            }
        };
        worker.start();
    }

    /**
   * Iterate the downloada list and download all the files
   */
    private void checkCache() {
        CompletionService<BaseModel> completionService = new ExecutorCompletionService<BaseModel>(threadPool);
        int numEntries = downloadList.getNumChildren();
        for (int j = 0; j < numEntries; j++) {
            final BaseModel documentModel = ((BaseModel) downloadList.get(j));
            completionService.submit(new Callable<BaseModel>() {

                public BaseModel call() {
                    checkFile(documentModel);
                    return documentModel;
                }
            });
        }
        try {
            for (int i = 0; i < numEntries; i++) {
                Future<BaseModel> f = completionService.take();
                BaseModel documentModel = f.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw LaunderThrowable.launderThrowable(e.getCause());
        }
    }

    /**
   * Iterate the downloada list and download all the files
   */
    private void doDownload(final ResourceLoaderStatus status) {
        CompletionService<BaseModel> completionService = new ExecutorCompletionService<BaseModel>(threadPool);
        int numEntries = downloadList.getNumChildren();
        for (int j = 0; j < numEntries; j++) {
            final BaseModel documentModel = ((BaseModel) downloadList.get(j));
            completionService.submit(new Callable<BaseModel>() {

                public BaseModel call() {
                    downloadFile(status, documentModel);
                    return documentModel;
                }
            });
        }
        try {
            for (int i = 0; i < numEntries; i++) {
                Future<BaseModel> f = completionService.take();
                BaseModel documentModel = f.get();
                status.setProgress(i * 100 / numEntries);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw LaunderThrowable.launderThrowable(e.getCause());
        }
    }

    /**
   * Download an individual file
   */
    private void downloadFile(final ResourceLoaderStatus status, BaseModel documentModel) {
        String fileName = (String) documentModel.getAttribValue(DOWNLOAD_FILENAME);
        String fileDate = (String) documentModel.getAttribValue(DOWNLOAD_FILEDATE);
        long lastModified = Long.MAX_VALUE;
        if ((fileDate != null) && (fileDate.trim().length() > 0)) {
            try {
                Date date = sdf.parse(fileDate.trim());
                lastModified = date.getTime();
            } catch (Exception ex) {
                System.out.println(fileDate);
                ex.printStackTrace();
            }
        }
        String srcFileName = getSourceFileName(fileName);
        File targetFile = getTargetFile(fileName, true);
        int fileSize = 0;
        try {
            fileSize = Integer.parseInt((String) documentModel.getAttribValue(DOCUMENT_FILESIZE));
        } catch (Exception ex) {
        }
        int bytesSofar = 0;
        try {
            if (!targetFile.exists() || (targetFile.lastModified() < lastModified)) {
                DebugLogger.trace("Downloading file: " + srcFileName);
                URL url = new URL(srcFileName);
                InputStream in = url.openStream();
                OutputStream out = new FileOutputStream(targetFile);
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                    bytesSofar += len;
                    documentModel.setAttribValue(DOWNLOAD_PROGRESS, Integer.toString(bytesSofar * 100 / fileSize));
                    status.updateFileProgress();
                }
                out.flush();
                in.close();
                out.close();
            } else documentModel.setAttribValue(DOWNLOAD_PROGRESS, "100");
            urlClassLoader.addURL(targetFile.toURL());
        } catch (Exception e) {
            if (bytesSofar == 0) documentModel.setAttribValue(DOWNLOAD_PROGRESS, "-10000"); else documentModel.setAttribValue(DOWNLOAD_PROGRESS, Integer.toString(-bytesSofar * 100 / fileSize));
            status.updateFileProgress();
            e.printStackTrace();
        }
    }

    /**
   * Check the cache for an individual file
   */
    private void checkFile(BaseModel documentModel) {
        String fileName = (String) documentModel.getAttribValue(DOWNLOAD_FILENAME);
        String fileDate = (String) documentModel.getAttribValue(DOWNLOAD_FILEDATE);
        long lastModified = Long.MAX_VALUE;
        if ((fileDate != null) && (fileDate.length() > 0)) {
            try {
                Date date = sdf.parse(fileDate.trim());
                lastModified = date.getTime();
            } catch (Exception ex) {
                System.out.println(fileDate);
                ex.printStackTrace();
            }
        }
        File targetFile = getTargetFile(fileName, true);
        int fileSize = 0;
        try {
            fileSize = Integer.parseInt((String) documentModel.getAttribValue(DOCUMENT_FILESIZE));
        } catch (Exception ex) {
        }
        int bytesSofar = 0;
        try {
            if (!targetFile.exists() || (targetFile.lastModified() < lastModified)) {
            } else documentModel.setAttribValue(DOWNLOAD_PROGRESS, "100");
            if (targetFile.exists()) urlClassLoader.addURL(targetFile.toURL());
        } catch (Exception e) {
            if (bytesSofar == 0) documentModel.setAttribValue(DOWNLOAD_PROGRESS, "-10000"); else documentModel.setAttribValue(DOWNLOAD_PROGRESS, Integer.toString(-bytesSofar * 100 / fileSize));
            e.printStackTrace();
        }
    }

    public void stopDownload() {
    }

    public void pauseDownload() {
    }

    /**
   * Get the complete server URL of the file
   */
    public String getSourceFileName(String fileName) {
        String srcFileName = "";
        if (serverURL != null) srcFileName += serverURL; else srcFileName += "<server>";
        if (!(srcFileName.endsWith("/") || srcFileName.endsWith("\\"))) srcFileName += "/";
        srcFileName += fileName;
        return srcFileName;
    }

    /**
   * Get the complete target filename
   */
    public File getTargetFile(String fileName, boolean makeDirs) {
        File targetFile = new File(targetFolder, fileName);
        if (makeDirs && !targetFile.exists()) targetFile.getParentFile().mkdirs();
        return targetFile;
    }

    /**
   * Fixup the server URL
   */
    public void setServerURL(String url) {
        serverURL = url.trim();
        if (!serverURL.endsWith("/")) serverURL += "/";
    }

    /**
   * Fixup the target folder
   */
    public void setTargetFolder(File folder) {
        targetFolder = folder;
    }

    class ResourceClassLoader extends URLClassLoader {

        public ResourceClassLoader(URL[] urls) {
            super(urls);
        }

        public ResourceClassLoader(URL[] urls, ClassLoader cl) {
            super(urls, cl);
        }

        public void addURL(URL url) {
            super.addURL(url);
        }
    }
}
