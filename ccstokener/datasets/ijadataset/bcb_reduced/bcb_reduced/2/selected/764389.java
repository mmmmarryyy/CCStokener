package vi.misc;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

/**
 A source of properties that works by reading them from a resource.

 @author <a href="mailto:ivaradi@freemail.c3.hu">Istv�n V�radi</a>
 */
public class ResourcePropertySource extends DictPropSource {

    /**
         The classloader to use. If <code>null</code> the system class
         loader is used.
         */
    private ClassLoader cloader;

    /**
         The locale to use, or <code>null</code> if no locale should be
         used
         */
    private Locale locale;

    /**
         The resource name.
         */
    private String resname;

    /**
         Construct a property source.

         @param cloader the classloader to use
         @param locale  the locale to use
         @param resname the resouce name

         @exception IOException if some I/O error occurs
         */
    public ResourcePropertySource(ClassLoader cloader, Locale locale, String resname) throws IOException {
        this.cloader = cloader;
        this.locale = locale;
        this.resname = resname;
        refresh();
    }

    /**
         Find the properties file in the classloader given.

         @param cl      the class loader to use

         @return the properties found or <code>null</code>.

         @exception IOException if some I/O error occurs
         */
    private Properties findProperties(ClassLoader cl) throws IOException {
        URL url = null;
        if (locale != null) {
            url = cl.getResource(resname + "_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant() + ".properties");
            if (url == null) {
                url = cl.getResource(resname + "_" + locale.getLanguage() + "_" + locale.getCountry() + ".properties");
            }
            if (url == null) {
                url = cl.getResource(resname + "_" + locale.getLanguage() + ".properties");
            }
        }
        if (url == null) {
            url = cl.getResource(resname + ".properties");
        }
        if (url == null) return null;
        Properties pr = new Properties();
        InputStream is = url.openStream();
        pr.load(is);
        is.close();
        return pr;
    }

    /**
         Refresh the properties handled by the source.

         @exception IOException if some I/O error occurs
         */
    public void refresh() throws IOException {
        Properties pr = null;
        if (cloader != null) pr = findProperties(cloader);
        if (pr == null) pr = findProperties(getClass().getClassLoader());
        if (pr == null) throw new FileNotFoundException("Resource '" + resname + "' not found"); else props = pr;
    }

    /**
         Create a new property source which uses the resource with the
         given name, and the locale and classloader as this one

         @param param   the name of the resource
         */
    public PropertySource create(String param) throws IOException {
        return new ResourcePropertySource(cloader, locale, param);
    }
}
