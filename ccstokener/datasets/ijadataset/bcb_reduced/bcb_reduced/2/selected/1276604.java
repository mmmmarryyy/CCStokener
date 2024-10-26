package common.devbot.generator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;
import common.devbot.description.AppDescription;
import common.devbot.description.EntityDescription;
import common.devbot.description.PackageDescription;
import common.devbot.util.csv.CsvUtils;
import common.devbot.util.exception.TechnicalException;
import common.devbot.util.file.FileUtils;
import common.devbot.util.file.StreamUtils;
import common.devbot.util.string.StringUtils;

public class JspCodeGenerator {

    private final String CONTEXTPATH = "/generator";

    private final int PORT = 8080;

    private final Server server = new Server(PORT);

    private final CustomHandler handler;

    public JspCodeGenerator() {
        final String WEBAPPDIR = ".";
        final URL warUrl = this.getClass().getClassLoader().getResource(WEBAPPDIR);
        System.out.println("info" + warUrl);
        final String warUrlString = warUrl.toExternalForm();
        handler = new CustomHandler(warUrlString, CONTEXTPATH);
        server.setHandler(handler);
    }

    private class CustomHandler extends WebAppContext {

        private Map<String, Object> contextObjects;

        public CustomHandler(String warUrl, String contextPath) {
            super(warUrl, contextPath);
        }

        @Override
        public void handle(String arg0, HttpServletRequest req, HttpServletResponse resp, int arg3) throws IOException, ServletException {
            if (contextObjects != null) {
                for (String key : contextObjects.keySet()) {
                    req.setAttribute(key, contextObjects.get(key));
                }
            }
            super.handle(arg0, req, resp, arg3);
        }

        public void updateContextObjects(Map<String, Object> obj) {
            contextObjects = obj;
        }
    }

    public int generateApp(AppDescription model, String appTemplate, String targetPath) {
        int generated = 0;
        try {
            startServer();
            String res = generateSource(model, appTemplate);
            List<String[]> liste = CsvUtils.parseCSV(res);
            System.out.println("TARGET=" + targetPath);
            for (String[] ligne : liste) {
                if (ligne.length > 1) {
                    String genFilePath = targetPath + "/" + ligne[2];
                    PackageDescription pDesc = model.getPackage(ligne[3]);
                    Object itemModel = pDesc;
                    if ("package".equals(ligne[0])) {
                        System.out.println("PKG SIZE=" + pDesc.getName() + "(" + pDesc.getEntities().size() + ")");
                    } else if ("entity".equals(ligne[0])) {
                        EntityDescription eDesc = pDesc.getEntity(ligne[4]);
                        System.out.println("ENT SIZE=" + eDesc.getEntityName() + "(" + eDesc.getFieldList().size() + ")");
                        itemModel = eDesc;
                    }
                    String source = generateSource(itemModel, "/" + ligne[1]);
                    int essais = 1;
                    while (essais < 5 && StringUtils.isNullOrEmpty(source)) {
                        System.out.println("retour vide(" + essais + "): on retente dans 2s");
                        essais++;
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        source = generateSource(itemModel, "/" + ligne[1]);
                    }
                    if (StringUtils.isNullOrEmpty(source)) {
                        System.err.println("Problème de génération de fichier (fichier vide) :" + genFilePath);
                    }
                    FileUtils.createTextFile(genFilePath, source);
                    System.out.println("Generated (" + source.length() + ") :" + genFilePath);
                    generated++;
                }
            }
        } finally {
            stopServer();
        }
        return generated;
    }

    public void generateFile(Object model, String templateUrl, File targetPath) {
        try {
            startServer();
            String content = generateSource(model, templateUrl);
            System.out.println("resultat:" + content);
        } finally {
            stopServer();
        }
    }

    private String generateSource(Object model, String templateUrl) {
        Map<String, Object> objs = new HashMap<String, Object>();
        objs.put("CONTEXT_OBJECT", model);
        handler.updateContextObjects(objs);
        return getContents(server, CONTEXTPATH + "/" + templateUrl);
    }

    private String getContents(Server server, String uri) throws TechnicalException {
        try {
            URL url = new URL("http://localhost:" + PORT + uri);
            return StreamUtils.getStreamContent(url.openStream());
        } catch (Exception e) {
            e.printStackTrace();
            throw new TechnicalException(e);
        }
    }

    private void startServer() {
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopServer() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
