package jtiger.core.web.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import jtiger.core.test.SpringContextTestCase;
import jtiger.modules.secur.user.model.User;
import jtiger.modules.upload.FileSystemUploadService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = { "classpath*:META-INF/spring/applicationContext*.xml" })
public class FileSystemUploadServiceTest extends SpringContextTestCase {

    @Autowired
    private FileSystemUploadService fsus;

    @Autowired
    @Value("classpath:META-INF/spring/applicationContext.xml")
    private Resource res;

    private String name;

    private String id = "4028815b291ad91d01291ad91d010000";

    @Test
    public void buildUserPath() {
        User u = new User();
        u.setId(id);
        fsus.buildUserPath(u);
        fsus.buildUserPath(null);
    }

    @Test
    public void write() throws FileNotFoundException, IOException {
        User u = new User();
        u.setId(id);
        name = fsus.write(new FileInputStream(res.getFile()), u, "applicationContext.xml", false);
        logger.debug(name);
    }

    @Test
    public void read() throws IOException {
        write();
        User u = new User();
        u.setId(id);
        fsus.read(name, u, new FileOutputStream(new File("E:\\temp\\" + name)));
    }

    @Test
    public void cleanTest() {
        fsus.buildUserPath(null);
        fsus.cleanTest();
    }

    @Test
    public void writeHead() throws Exception {
        User u = new User();
        u.setId(id);
        URL url = new URL("http://www.youxiy.com/uploads/allimg/081107/2159580.jpg");
        URLConnection conn = url.openConnection();
        conn.connect();
        fsus.writeFace(conn.getInputStream(), u);
    }
}
