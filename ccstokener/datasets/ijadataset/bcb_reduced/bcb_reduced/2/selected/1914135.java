package page;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.illico.common.IllicoUtils;
import org.illico.common.component.Published;
import org.illico.common.display.DisplayNotifiable;
import org.illico.common.lang.Exception;
import org.illico.common.lang.MapUtils;
import org.illico.common.util.ManifestUtils;
import org.illico.common.util.PropertiesUtils;
import org.illico.web.common.Context;

@Published
public class TestManifest implements DisplayNotifiable {

    public void notifyDisplay() {
        try {
            List<URL> urls = Collections.list(PropertiesUtils.class.getClassLoader().getResources("META-INF/MANIFEST.MF"));
            for (URL url : urls) {
                InputStream is = url.openStream();
                try {
                    Manifest mf = new Manifest(is);
                    if ("Illico".equals(mf.getMainAttributes().get(Attributes.Name.IMPLEMENTATION_TITLE))) {
                        MapUtils.append(Context.getResponse().getWriter(), "\n", "=", mf.getMainAttributes());
                    }
                } finally {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new Exception(e);
        }
        System.out.println(ManifestUtils.getVersion("org.illico.common.web"));
    }
}
