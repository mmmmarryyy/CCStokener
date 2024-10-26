package vehikel.target.asuro;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.osgi.framework.Bundle;
import vehikel.ITemplateProvider;

public class TemplateProvider implements ITemplateProvider {

    private static final String BUNDEL_TEMPLATES = "/templates";

    List<String> templateFolderList = new ArrayList<String>();

    List<String> templateList = new ArrayList<String>();

    public List<String> findBundelTemplates() {
        serachTemplateFolders();
        return templateList;
    }

    public List<String> findBundelTemplateFolders() {
        serachTemplateFolders();
        return templateFolderList;
    }

    private void serachTemplateFolders() {
        Bundle bundle = Activator.getBundleContext().getBundle();
        Enumeration<URL> entries = bundle.findEntries(BUNDEL_TEMPLATES, null, true);
        while (entries.hasMoreElements()) {
            URL url = entries.nextElement();
            String path = url.getPath();
            if (path.endsWith("/")) {
                templateFolderList.add(path.substring(BUNDEL_TEMPLATES.length()));
            } else {
                templateList.add(path.substring(BUNDEL_TEMPLATES.length()));
            }
        }
    }

    public InputStream getBundelTemplateStream(String path) {
        Bundle bundle = Activator.getBundleContext().getBundle();
        String absolutePath = BUNDEL_TEMPLATES + path;
        java.net.URL url = bundle.getEntry(absolutePath);
        InputStream in = null;
        try {
            in = url.openStream();
        } catch (IOException ex) {
            System.err.println(ex);
            ex.printStackTrace();
        }
        return in;
    }
}
