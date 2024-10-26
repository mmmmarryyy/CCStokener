package ar.com.larreta.comunes;

import java.util.Iterator;
import java.util.Properties;
import org.apache.log4j.Logger;
import ar.com.larreta.excepciones.Excepcion;

public class Propiedades extends Properties {

    protected static Logger logger = Logger.getLogger(Properties.class);

    private java.net.URL url;

    public Propiedades(String nombreCompletoArchivo) {
        try {
            url = getClass().getClassLoader().getResource(nombreCompletoArchivo);
            load(url.openStream());
        } catch (Exception e) {
            logger.error(Excepcion.getStackTrace(e));
        }
    }

    public void propiedadesComoParametroSistema() {
        Iterator it = this.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            System.setProperty(key, this.getProperty(key));
        }
    }

    public String getPath() {
        if (url != null) {
            return url.getPath().replaceAll(Constantes.PORCENTAJE20, Constantes.ESPACIO);
        }
        return "";
    }
}
