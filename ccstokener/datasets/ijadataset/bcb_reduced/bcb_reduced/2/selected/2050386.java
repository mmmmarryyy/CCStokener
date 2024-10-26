package com.wantmeet.castloader;

import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;
import java.net.URLConnection;
import java.io.BufferedInputStream;
import com.wantmeet.castloader.FileDownLoadThread;

public class CastDownLoader {

    private static final Log log = LogFactory.getLog(CastDownLoader.class);

    private static Document configDoc = null;

    private static Document baseDoc = null;

    private static final String CONFFILE = "config.xml";

    private static final String BASEDOCFILE = "basedoc.xml";

    private void readPropertyXml() throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(CONFFILE), "UTF-8"));
        SAXReader reader = new SAXReader();
        configDoc = reader.read(br);
        br.close();
    }

    public void readBaseXml() throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(BASEDOCFILE), "UTF-8"));
        SAXReader reader = new SAXReader();
        baseDoc = reader.read(br);
        br.close();
    }

    public void doExec() throws Exception {
        List<Node> nodeList = configDoc.selectNodes("//downloader/podcast");
        for (int ii = 0; ii < nodeList.size(); ii++) {
            List<String> strList = new ArrayList();
            Node eachNode = nodeList.get(ii);
            String sourceUrl = eachNode.valueOf("./source-url");
            String targetUrl = eachNode.valueOf("./target-url");
            String downFolder = eachNode.valueOf("./save-path");
            String howMany = eachNode.valueOf("./number");
            String maintain = eachNode.valueOf("./maintain-files");
            String storeOnly = eachNode.valueOf("./store-only");
            strList.add(this.extractFileName(targetUrl));
            String targetFileUrl = this.extractUrlName(targetUrl);
            Document sourceDoc = this.readExternalXml(sourceUrl);
            int iHowMany = Integer.parseInt(howMany);
            List<Node> srcNodeList = sourceDoc.selectNodes("//rss/channel/item");
            String targetPath = null;
            for (int jj = 0; jj < srcNodeList.size(); jj++) {
                if (jj < iHowMany) {
                    Node itemNode = srcNodeList.get(jj);
                    Node sNode = itemNode.selectSingleNode("./enclosure/@url");
                    String downUrl = sNode.valueOf(".");
                    log.debug("downUrl=[" + downUrl + "]");
                    String targetFileName = this.extractFileName(downUrl);
                    String downTargetPath = downFolder + targetFileName;
                    log.debug("downTargetPath=[" + downTargetPath + "]");
                    Thread t = new Thread(new FileDownLoadThread(downUrl, downTargetPath));
                    t.start();
                    strList.add(targetFileName);
                    targetPath = this.extractUrlName(targetUrl) + "/" + this.extractFileName(downUrl);
                    sNode.setText(targetPath);
                    log.debug("sN=[" + sNode.asXML() + "]");
                } else {
                    Node itemNode = srcNodeList.get(jj);
                    itemNode.detach();
                }
            }
            if (!storeOnly.equals("yes")) {
                XMLWriter writer = null;
                try {
                    writer = new XMLWriter(new FileOutputStream(downFolder + this.extractFileName(targetUrl)));
                    writer.write(sourceDoc);
                } catch (Exception e) {
                    log.error("error in feed xml writing.");
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
            for (int kk = 0; kk < strList.size(); kk++) {
                log.debug("strList=[" + strList.get(kk) + "]");
            }
            if (maintain.equals("no")) {
                this.delCurrentDirFilesExcept(downFolder, strList);
            }
        }
    }

    public void delCurrentDirFilesExcept(String path, List<String> strList) {
        File f = new File(path);
        if (f.isDirectory()) {
            File[] fileList = f.listFiles();
            for (int ii = 0; ii < fileList.length; ii++) {
                String fName = fileList[ii].getName();
                boolean fileExist = false;
                for (int jj = 0; jj < strList.size(); jj++) {
                    String strFile = (String) strList.get(jj);
                    if (strFile.equals(fName)) {
                        fileExist = true;
                    }
                }
                if (!fileExist) {
                    fileList[ii].delete();
                }
            }
        }
    }

    private String extractFileName(String str) {
        String rtnStr = str.substring(str.lastIndexOf("/") + 1);
        return rtnStr;
    }

    private String extractUrlName(String str) {
        String rtnStr = str.substring(0, str.lastIndexOf("/"));
        return rtnStr;
    }

    public void seeURLConnection() throws Exception {
        URL url = new URL("http://wantmeet.iptime.org");
        URLConnection uc = url.openConnection();
        BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String s = null;
        StringBuffer sb = new StringBuffer();
        while ((s = br.readLine()) != null) {
            sb.append(s);
        }
        br.close();
        log.debug("sb=[" + sb.toString() + "]");
    }

    public void url2SaveAsFile(String urlStr, String saveAsFileName) throws Exception {
        URL url = new URL(urlStr);
        URLConnection uc = url.openConnection();
        File f = new File(saveAsFileName);
        if (!f.exists()) {
            FileOutputStream fos = new FileOutputStream(f);
            BufferedInputStream bis = new BufferedInputStream(uc.getInputStream());
            byte[] buffer = new byte[4096];
            int readCount = 0;
            while ((readCount = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, readCount);
            }
            fos.flush();
            fos.close();
            bis.close();
        }
    }

    private Document readExternalXml(String urlStr) throws Exception {
        SAXReader reader = new SAXReader();
        URL url = new URL(urlStr);
        Document extFeedDoc = reader.read(url);
        return extFeedDoc;
    }

    public static void main(String[] args) throws Exception {
        CastDownLoader cdl = new CastDownLoader();
        cdl.readPropertyXml();
        cdl.doExec();
        log.info("Complete!");
    }
}
