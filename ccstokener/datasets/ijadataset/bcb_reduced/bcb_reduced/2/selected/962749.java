package ramon.ext.viewers;

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import ramon.Atributos;
import ramon.BodyViewer;

/**
 * Viewer particular para la generación de pantallas estáticas. Ignora los
 * modelos
 */
public class StaticViewer extends BodyViewer {

    public void show(HttpServletRequest request, HttpServletResponse response, String pantalla, Atributos modelos) {
        URL url = getRecurso(pantalla);
        try {
            IOUtils.copy(url.openStream(), response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
