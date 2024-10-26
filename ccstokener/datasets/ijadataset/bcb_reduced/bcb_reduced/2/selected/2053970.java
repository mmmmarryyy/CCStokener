package net.sourceforge.processdash.i18n;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.sourceforge.processdash.templates.TemplateLoader;

/** A special classloader for use with java.util.ResourceBundle.
 * This loads ".properties" files from the TemplateLoader search
 * path, allowing dashboard add-on files to contribute localization
 * information.  In addition, if this classloader finds more than
 * one matching ".properties" file in the TemplateLoader search
 * path, it will merge their contents.
 */
public class MergingTemplateClassLoader extends SafeTemplateClassLoader {

    private Map cache = Collections.synchronizedMap(new HashMap());

    private boolean reverseOrder;

    private File tempDir;

    public MergingTemplateClassLoader() {
        try {
            String s = "a=1\na=2\n";
            byte[] data = s.getBytes("ISO-8859-1");
            Properties p = new Properties();
            p.load(new ByteArrayInputStream(data));
            reverseOrder = ("2".equals(p.get("a")));
            File tempFile = File.createTempFile("res", ".tmp");
            tempDir = tempFile.getParentFile();
            tempFile.delete();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    protected URL findResourceImpl(String mappedName) {
        try {
            if (cache.containsKey(mappedName)) return (URL) cache.get(mappedName); else {
                URL result = lookupTemplateResource(mappedName);
                cache.put(mappedName, result);
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private URL lookupTemplateResource(String mappedName) {
        URL[] result = TemplateLoader.resolveURLs(mappedName);
        if (result.length == 0) return null; else if (result.length == 1) return result[0]; else try {
            return concatenateResources(mappedName, result);
        } catch (IOException ioe) {
            return result[0];
        }
    }

    private URL concatenateResources(String mappedName, URL[] itemsToMerge) throws IOException {
        String name = "temp-" + mappedName.replace('/', ',');
        File f = new File(tempDir, name);
        f.deleteOnExit();
        FileOutputStream out = new FileOutputStream(f);
        if (reverseOrder) {
            for (int i = itemsToMerge.length; i-- > 0; ) copyData(itemsToMerge[i], out);
        } else {
            for (int i = 0; i < itemsToMerge.length; i++) copyData(itemsToMerge[i], out);
        }
        out.close();
        return f.toURL();
    }

    private void copyData(URL url, FileOutputStream out) throws IOException {
        InputStream in = url.openConnection().getInputStream();
        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = in.read(buf)) != -1) out.write(buf, 0, bytesRead);
        out.write('\n');
        in.close();
    }
}
