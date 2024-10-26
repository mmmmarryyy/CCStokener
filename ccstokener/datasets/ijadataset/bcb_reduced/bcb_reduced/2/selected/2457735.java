package com.goodcodeisbeautiful.archtea.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import com.goodcodeisbeautiful.archtea.config.DefaultConfigManager;
import com.goodcodeisbeautiful.test.util.CommonTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TomcatServerServiceTest extends CommonTestCase {

    private static final String ROOT_FOLDER = "webapps" + File.separator + "ROOT";

    private static final String CONF_FOLDER = "conf";

    private static final String WEB_INF_FOLDER = ROOT_FOLDER + File.separator + "WEB-INF";

    public static Test suite() {
        return new TestSuite(TomcatServerServiceTest.class);
    }

    @Override
    protected List getSetupFilenames() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("archtea-main-config.xml");
        list.add("tomcat-config.xml");
        list.add("index.html");
        list.add("web.xml");
        list.add("jk2.properties");
        list.add("tomcat-users.xml");
        return list;
    }

    TomcatServerService _service;

    protected void setUp() throws Exception {
        super.setUp();
        new File(getWorkDir() + File.separator + ROOT_FOLDER).mkdirs();
        new File(getWorkDir() + File.separator + CONF_FOLDER).mkdirs();
        new File(getWorkDir() + File.separator + WEB_INF_FOLDER).mkdirs();
        copyTo("index.html", ROOT_FOLDER + File.separator + "index.html");
        copyTo("web.xml", CONF_FOLDER + File.separator + "web.xml");
        copyTo("jk2.properties", CONF_FOLDER + File.separator + "jk2.properties");
        copyTo("tomcat-users.xml", CONF_FOLDER + File.separator + "tomcat-users.xml");
        InputStream testIn = new FileInputStream(getWorkDir() + File.separator + "archtea-main-config.xml");
        byte[] buff = new byte[512];
        int len = testIn.read(buff, 0, buff.length);
        while (len != -1) {
            System.out.println(new String(buff, 0, len));
            len = testIn.read(buff, 0, buff.length);
        }
        DefaultConfigManager.getInstance().setMainConfigPath(getWorkDir() + File.separator + "archtea-main-config.xml");
        _service = new TomcatServerService(getWorkDir());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        _service.shutdown();
        super.cleanWorkFiles();
    }

    public void testTomcatServerService() throws Exception {
        assertNotNull(_service);
    }

    public void testStartup() throws Exception {
        _service.startup();
        System.out.print("Waiting for startup ...");
        Thread.sleep(10 * 1000L);
        System.out.println("Done.");
        URL url = new URL("http://localhost:7788/index.html");
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(10 * 1000);
        InputStream in = null;
        try {
            StringBuffer strBuff = new StringBuffer();
            byte[] buff = new byte[1024];
            in = conn.getInputStream();
            int len = in.read(buff, 0, buff.length);
            while (len != -1) {
                strBuff.append(new String(buff, 0, len));
                len = in.read(buff, 0, buff.length);
            }
            assertNotNull("Check contents.", strBuff.toString().contains("test root document"));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void testInit() throws Exception {
        _service.init(null);
    }

    public void testRestart() throws Exception {
        _service.restart();
    }
}
