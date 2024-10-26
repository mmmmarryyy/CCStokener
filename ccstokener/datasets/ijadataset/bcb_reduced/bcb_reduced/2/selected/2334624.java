package com.g2inc.scap.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import com.g2inc.scap.library.domain.SCAPDocumentFactory;
import com.g2inc.scap.library.domain.SCAPDocumentTypeEnum;
import com.g2inc.scap.library.domain.cpe.CPEDictionaryDocument;
import com.g2inc.scap.library.domain.cpe.CPEItem;
import com.g2inc.scap.library.domain.cpe.CPEItemCheck;
import com.g2inc.scap.library.domain.cpe.CPEItemCheckSystemType;
import com.g2inc.scap.library.domain.oval.MergeStats;
import com.g2inc.scap.library.domain.oval.OvalDefinitionsDocument;

public class CreateOfficalCPEDictionaryOvalTask {

    private static final String CONTENT_DIR = "data_feeds";

    private static final String OVAL_FILE = "official-cpe-oval.xml";

    private static final Logger LOG = Logger.getLogger(CreateOfficalCPEDictionaryOvalTask.class);

    /**
	 * Attempt to grab the latest offical CPE dictionary file and store it locally
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        CreateOfficalCPEDictionaryOvalTask task = new CreateOfficalCPEDictionaryOvalTask();
        task.work(args);
    }

    private void work(String[] args) throws Exception {
        String contentDir = CONTENT_DIR;
        File contentDirHandle = new File(contentDir);
        FilenameFilter ffilter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                String lcFn = name.toLowerCase();
                if (lcFn.startsWith("official-cpe-dictionary") && lcFn.endsWith(".xml")) {
                    return true;
                }
                return false;
            }
        };
        File[] dictFiles = contentDirHandle.listFiles(ffilter);
        if (dictFiles == null || dictFiles.length == 0) {
            throw new RuntimeException("official cpe dictionary file not found!");
        }
        String dictFilename = dictFiles[0].getAbsolutePath();
        String ovalFilename = dictFiles[0].getAbsoluteFile().getParentFile().getAbsolutePath() + File.separator + OVAL_FILE;
        File dictFile = new File(dictFilename);
        File ovalFile = new File(ovalFilename);
        boolean dictChanged = false;
        CPEDictionaryDocument cpeDict = (CPEDictionaryDocument) SCAPDocumentFactory.loadDocument(dictFile);
        if (cpeDict == null) {
            throw new IllegalStateException("CPEDictionary could not be loaded!");
        }
        OvalDefinitionsDocument cpeOval = (OvalDefinitionsDocument) SCAPDocumentFactory.createNewDocument(SCAPDocumentTypeEnum.OVAL_59);
        cpeOval.setFilename(ovalFile.getAbsolutePath());
        int itemsProcessed = 0;
        int itemsWithChecks = 0;
        List<CPEItem> items = cpeDict.getItems();
        MergeStats stats = new MergeStats();
        for (int x = 0; x < items.size(); x++) {
            CPEItem item = items.get(x);
            itemsProcessed++;
            List<CPEItemCheck> checks = item.getChecks();
            if (checks == null) {
                continue;
            }
            for (int y = 0; y < checks.size(); y++) {
                CPEItemCheck check = checks.get(y);
                if (check.getSystem() != null && check.getSystem().equals(CPEItemCheckSystemType.OVAL5)) {
                    String href = check.getHref();
                    if (href.indexOf(":") == -1) {
                        continue;
                    }
                    System.out.println("Downloading content for cpe item " + item.getName() + "(" + itemsProcessed + ")");
                    dictChanged = true;
                    itemsWithChecks++;
                    URL url = new URL(href);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    LOG.debug("loading oval definitions document from URL " + href);
                    OvalDefinitionsDocument odd = (OvalDefinitionsDocument) SCAPDocumentFactory.loadDocument(is);
                    LOG.debug("oval definitions document loaded");
                    if (odd != null) {
                        cpeOval.merge(odd, stats);
                    }
                    odd = null;
                    check.setHref(ovalFile.getName());
                }
            }
        }
        if (dictChanged) {
            System.out.println("Items processed = " + itemsProcessed);
            System.out.println("Items with checks = " + itemsWithChecks);
            cpeDict.save();
            cpeOval.save();
        } else {
            System.out.println("Files have already been processed, no changes needed.");
        }
    }
}
