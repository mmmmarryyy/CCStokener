package co.edu.unal.ungrid.services.client.applet.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.List;

public class TestImagesHelper implements Runnable {

    private TestImagesHelper(final String sCodeBase, final List<String> lst, final String sTargetDir) {
        m_sCodeBase = sCodeBase;
        m_lst = lst;
        m_sTargetDir = sTargetDir;
    }

    public static void loadTestImages(final String sCodeBase, final List<String> lst, final String sTargetDir) {
        new Thread(new TestImagesHelper(sCodeBase, lst, sTargetDir)).start();
    }

    private void downloadImage(final String sImgName) {
        if (sImgName != null && sImgName.length() > 0) {
            File f = new File(m_sTargetDir);
            if (f.canWrite()) {
                f = new File(f, sImgName);
                if (!f.exists()) {
                    try {
                        URL url = new URL(m_sCodeBase + '/' + sImgName);
                        BufferedInputStream is = new BufferedInputStream(url.openStream());
                        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                        int c;
                        while ((c = is.read()) != -1) {
                            os.write(c);
                        }
                        is.close();
                        os.close();
                    } catch (Exception exc) {
                        System.out.println("TestImagesHelper::downloadImage(): " + exc);
                    }
                }
            }
        }
    }

    public void run() {
        if (m_sCodeBase != null && m_sCodeBase.length() > 0 && m_lst != null && m_lst.size() > 0 && m_sTargetDir != null && m_sTargetDir.length() > 0) {
            for (String imgName : m_lst) {
                downloadImage(imgName);
            }
        }
    }

    private String m_sCodeBase;

    private List<String> m_lst;

    private String m_sTargetDir;
}
