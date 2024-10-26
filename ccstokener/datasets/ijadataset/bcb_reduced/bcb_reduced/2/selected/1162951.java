package org.j2eespider.ide.jobs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.j2eespider.SpiderPlugin;
import org.j2eespider.ide.data.domain.RepositoryTemplateInfo;
import org.j2eespider.ide.data.rules.XmlManager;
import org.j2eespider.ide.data.rules.XmlManager.XML_TYPE;
import org.j2eespider.util.PropertiesUtil;

public class DownloadTemplateJob extends Job {

    Logger logger = Logger.getLogger(getClass());

    private List<String> pathZips, urlsTemplateInfo;

    private String pathDest;

    private int downloadSize;

    private Map<String, RepositoryTemplateInfo> mapTemplateInfo;

    public DownloadTemplateJob(String name, List<String> urlsTemplateInfo, List<String> pathZips, String pathDest, int downloadSize, Map<String, RepositoryTemplateInfo> mapTemplateInfo) {
        super(name);
        this.urlsTemplateInfo = urlsTemplateInfo;
        this.pathZips = pathZips;
        this.pathDest = pathDest;
        this.downloadSize = downloadSize;
        this.mapTemplateInfo = mapTemplateInfo;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        new File(pathDest).mkdirs();
        final int BUFFER = 2048;
        try {
            monitor.beginTask(PropertiesUtil.getMessage("dialog.progressDownload.description", "spider"), downloadSize);
            for (String pathZip : pathZips) {
                BufferedInputStream buffInputStream = null;
                try {
                    URL url = new URL(pathZip);
                    InputStream is = url.openStream();
                    buffInputStream = new BufferedInputStream(is);
                } catch (MalformedURLException e1) {
                    FileInputStream fis = new FileInputStream(pathZip);
                    buffInputStream = new BufferedInputStream(fis);
                }
                String pathTemplateList1 = null;
                String pathTemplateList2 = null;
                BufferedOutputStream dest = null;
                ZipInputStream zis = new ZipInputStream(buffInputStream);
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    monitor.subTask(PropertiesUtil.getMessage("dialog.progressDownload.status", new String[] { entry.getName() }, "spider"));
                    int count;
                    byte data[] = new byte[BUFFER];
                    String pathFile = pathDest + File.separator + entry.getName();
                    File file = new File(pathFile);
                    if (entry.isDirectory()) {
                        file.mkdir();
                        continue;
                    }
                    FileOutputStream fos = new FileOutputStream(pathFile);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                        monitor.worked(count);
                    }
                    dest.flush();
                    dest.close();
                }
                zis.close();
            }
            List<RepositoryTemplateInfo> templateList = new ArrayList<RepositoryTemplateInfo>();
            String pathFileTemplateList = pathDest + File.separator + "templateList.xml";
            for (String urlTemplateInfo : urlsTemplateInfo) {
                RepositoryTemplateInfo templateInfo = mapTemplateInfo.get(urlTemplateInfo);
                templateList.add(templateInfo);
            }
            File fileTemplateList = new File(pathFileTemplateList);
            if (fileTemplateList.exists()) {
                List<RepositoryTemplateInfo> oldTemplateList = (List<RepositoryTemplateInfo>) XmlManager.loadXml(pathFileTemplateList, XML_TYPE.TEMPLATE_LIST);
                old: for (RepositoryTemplateInfo oldTemplateInfo : oldTemplateList) {
                    for (RepositoryTemplateInfo newTemplateInfo : templateList) {
                        if (oldTemplateInfo.getFolder().equals(newTemplateInfo.getFolder())) {
                            continue old;
                        }
                    }
                    templateList.add(oldTemplateInfo);
                }
            }
            XmlManager.writeXml(templateList, pathFileTemplateList, XML_TYPE.TEMPLATE_LIST);
        } catch (Exception e) {
            logger.error(e);
            return new Status(Status.ERROR, SpiderPlugin.ID, e.getMessage());
        } finally {
            monitor.done();
        }
        return Status.OK_STATUS;
    }

    public List<String> getPathZips() {
        return pathZips;
    }

    public String getPathDest() {
        return pathDest;
    }
}
